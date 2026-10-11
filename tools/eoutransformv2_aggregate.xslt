<?xml version ="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" version="4.0" encoding="UTF-8"/>


<!--
  refVersion:
  - Beta:      major.minor.patch#build   (z.B. 2.5.3#10)
  - Produktiv: major.minor.patch_update  (z.B. 2.4.0_00)
-->
<xsl:param name="refVersion" select="'2.4.0_00'"/>
<xsl:param name="langcode" select="'de'"/>

<!-- 
Transformation script for an eou.xml which has a section name parameter for each change item.
It is intended to be used in the new efaOnlineUpdate feature in efa.

It is optimized for generating a changelog which aggregates the changes over multiple versions,
beginning from $refVersion.

It iterates through all Version nodes in the eou.xml, sorts them by version number
and outputs a table with the latest version number, release date, latest minimum Java version, latest minimum efaCloud version, important notices 
and the changes grouped by section and type (new / bugfix / other).

if a section is defined in the eou.xml, it will be displayed in the order of the sections defined in the eou.xml.

-->

<!-- Constants for case-insensitive comparisons -->
<xsl:variable name="UPPER" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ'"/>
<xsl:variable name="LOWER" select="'abcdefghijklmnopqrstuvwxyzäöü'"/>

<xsl:variable name="NEWITEM_PREFIX">
    <xsl:choose>
        <xsl:when test="$langcode='de'">neu:</xsl:when>
        <xsl:otherwise>new:</xsl:otherwise>
    </xsl:choose>
</xsl:variable>

<xsl:variable name="AdditionsTitle">
    <xsl:choose>
        <xsl:when test="$langcode='de'">Neuerungen</xsl:when>
        <xsl:otherwise>New Features</xsl:otherwise>
    </xsl:choose>
</xsl:variable>
<xsl:variable name="BugfixTitle">
    <xsl:choose>
        <xsl:when test="$langcode='de'">Korrekturen</xsl:when>
        <xsl:otherwise>Bug Fixes</xsl:otherwise>
    </xsl:choose>
</xsl:variable>
<xsl:variable name="OtherTitle">
    <xsl:choose>
        <xsl:when test="$langcode='de'">Sonstige Änderungen</xsl:when>
        <xsl:otherwise>Other Changes</xsl:otherwise>
    </xsl:choose>
</xsl:variable>


<!-- Liefert die deutsche Übersetzung einer Section; Fallback ist der interne Section-Name -->
<xsl:template name="section-label">
    <xsl:param name="sectionName"/>
    <xsl:choose>
        <xsl:when test="/efaOnlineUpdate/Sections/Section[@name=$sectionName]/Translation[@lang=$langcode]">
            <xsl:value-of select="/efaOnlineUpdate/Sections/Section[@name=$sectionName]/Translation[@lang=$langcode][1]"/>
        </xsl:when>
        <xsl:otherwise>
            <xsl:value-of select="$sectionName"/>
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>

<!--
  Ranking:
  major*1e10 + minor*1e8 + patch*1e6 + update*1e3 + build
  - Beta: update=0
  - Produktiv: build=0
-->
<xsl:template name="version-rank">
    <xsl:param name="v"/>

    <xsl:variable name="main" select="substring-before(concat($v, '#'), '#')"/>
    <xsl:variable name="buildStr">
        <xsl:choose>
            <xsl:when test="contains($v, '#')">
                <xsl:value-of select="substring-after($v, '#')"/>
            </xsl:when>
            <xsl:otherwise>0</xsl:otherwise>
        </xsl:choose>
    </xsl:variable>

    <xsl:variable name="mainNoUpdate" select="substring-before(concat($main, '_'), '_')"/>
    <xsl:variable name="updateStr">
        <xsl:choose>
            <xsl:when test="contains($main, '_')">
                <xsl:value-of select="substring-after($main, '_')"/>
            </xsl:when>
            <xsl:otherwise>0</xsl:otherwise>
        </xsl:choose>
    </xsl:variable>

    <xsl:variable name="majorStr" select="substring-before($mainNoUpdate, '.')"/>
    <xsl:variable name="rest1" select="substring-after($mainNoUpdate, '.')"/>
    <xsl:variable name="minorStr" select="substring-before($rest1, '.')"/>
    <xsl:variable name="patchStr" select="substring-after($rest1, '.')"/>

    <xsl:variable name="major" select="number($majorStr)"/>
    <xsl:variable name="minor" select="number($minorStr)"/>
    <xsl:variable name="patch" select="number($patchStr)"/>
    <xsl:variable name="update" select="number($updateStr)"/>
    <xsl:variable name="build" select="number($buildStr)"/>

    <xsl:value-of select="$major * 10000000000 + $minor * 100000000 + $patch * 1000000 + $update * 1000 + $build"/>
</xsl:template>

<!-- Prüft, ob eine Section in neueren Versionen Einträge des gewünschten Typs hat -->
<xsl:template name="section-has-mode">
    <xsl:param name="sectionName"/>
    <xsl:param name="mode"/>
    <xsl:param name="refRank"/>

    <xsl:for-each select="/efaOnlineUpdate/Version">
        <xsl:variable name="vRank">
            <xsl:call-template name="version-rank">
                <xsl:with-param name="v" select="VersionID"/>
            </xsl:call-template>
        </xsl:variable>
        <xsl:if test="number($vRank) &gt; number($refRank) and
            Changes[@lang=$langcode]/ChangeItem[@section=$sectionName][
                ($mode='new' and starts-with(translate(normalize-space(.), $UPPER, $LOWER),$NEWITEM_PREFIX))
                or
                ($mode='bugfix' and starts-with(translate(normalize-space(.),$UPPER, $LOWER),'bugfix:'))
                or
                ($mode='other'
                    and not(starts-with(translate(normalize-space(.), $UPPER, $LOWER),$NEWITEM_PREFIX))
                    and not(starts-with(translate(normalize-space(.), $UPPER, $LOWER),'bugfix:'))
                )
            ]">1</xsl:if>
    </xsl:for-each>
</xsl:template>

<!-- Gibt für eine Section genau eine Untergruppe aus (Neuerungen/Korrekturen/Sonstiges), falls vorhanden -->
<xsl:template name="output-section-subgroup">
    <xsl:param name="sectionName"/>
    <xsl:param name="title"/>
    <xsl:param name="mode"/>
    <xsl:param name="refRank"/>

    <xsl:variable name="hasMode">
        <xsl:call-template name="section-has-mode">
            <xsl:with-param name="sectionName" select="$sectionName"/>
            <xsl:with-param name="mode" select="$mode"/>
            <xsl:with-param name="refRank" select="$refRank"/>
        </xsl:call-template>
    </xsl:variable>

    <xsl:if test="contains($hasMode, '1')">
        <br/><xsl:value-of select="$title"/>
        <ul>
            <!-- Alle passenden ChangeItems aus allen neueren Versionen, in Original-Reihenfolge -->
            <xsl:for-each select="/efaOnlineUpdate/Version">
                <xsl:variable name="vRank">
                    <xsl:call-template name="version-rank">
                        <xsl:with-param name="v" select="VersionID"/>
                    </xsl:call-template>
                </xsl:variable>
                <xsl:if test="number($vRank) &gt; number($refRank)">
                    <xsl:for-each select="Changes[@lang=$langcode]/ChangeItem[@section=$sectionName][
                        ($mode='new' and starts-with(translate(normalize-space(.),$UPPER, $LOWER),$NEWITEM_PREFIX))
                        or
                        ($mode='bugfix' and starts-with(translate(normalize-space(.),$UPPER, $LOWER),'bugfix:'))
                        or
                        ($mode='other'
                            and not(starts-with(translate(normalize-space(.),$UPPER, $LOWER),$NEWITEM_PREFIX))
                            and not(starts-with(translate(normalize-space(.),$UPPER, $LOWER),'bugfix:'))
                        )
                    ]">
                        <li>
                            <xsl:choose>
                                <!-- Prefix für Neu:/Bugfix: entfernen, Sonstiges unverändert lassen -->
                                <xsl:when test="$mode='other'">
                                    <xsl:value-of select="."/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="substring-after(., ':')"/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </li>
                    </xsl:for-each>
                </xsl:if>
            </xsl:for-each>
        </ul>
    </xsl:if>
</xsl:template>

<!--
  Hauptausgabe: Section-zentriert.
  Jede Section bekommt Untergruppen für Neuerungen, Korrekturen, Sonstiges.
-->
<xsl:template name="output-sections-with-subgroups">
    <xsl:param name="refRank"/>

    <b>Änderungen nach Bereichen</b>
    <ul>
        <!-- Reihenfolge exakt aus Sections -->
        <xsl:for-each select="/efaOnlineUpdate/Sections/Section">
            <xsl:variable name="sectionName" select="@name"/>

            <!-- Prüfen, ob die Section überhaupt relevante Einträge in irgendeiner Untergruppe hat -->
            <xsl:variable name="hasNew">
                <xsl:call-template name="section-has-mode">
                    <xsl:with-param name="sectionName" select="$sectionName"/>
                    <xsl:with-param name="mode" select="'new'"/>
                    <xsl:with-param name="refRank" select="$refRank"/>
                </xsl:call-template>
            </xsl:variable>
            <xsl:variable name="hasBugfix">
                <xsl:call-template name="section-has-mode">
                    <xsl:with-param name="sectionName" select="$sectionName"/>
                    <xsl:with-param name="mode" select="'bugfix'"/>
                    <xsl:with-param name="refRank" select="$refRank"/>
                </xsl:call-template>
            </xsl:variable>
            <xsl:variable name="hasOther">
                <xsl:call-template name="section-has-mode">
                    <xsl:with-param name="sectionName" select="$sectionName"/>
                    <xsl:with-param name="mode" select="'other'"/>
                    <xsl:with-param name="refRank" select="$refRank"/>
                </xsl:call-template>
            </xsl:variable>

            <xsl:if test="contains($hasNew, '1') or contains($hasBugfix, '1') or contains($hasOther, '1')">
                <li>
                    <b>
                        <xsl:call-template name="section-label">
                            <xsl:with-param name="sectionName" select="$sectionName"/>
                        </xsl:call-template>
                    </b>
                    <br/>

                    <!-- Untergruppen pro Section -->
                    <xsl:call-template name="output-section-subgroup">
                        <xsl:with-param name="sectionName" select="$sectionName"/>
                        <xsl:with-param name="title" select="$AdditionsTitle"/>
                        <xsl:with-param name="mode" select="'new'"/>
                        <xsl:with-param name="refRank" select="$refRank"/>
                    </xsl:call-template>

                    <xsl:call-template name="output-section-subgroup">
                        <xsl:with-param name="sectionName" select="$sectionName"/>
                        <xsl:with-param name="title" select="$BugfixTitle"/>
                        <xsl:with-param name="mode" select="'bugfix'"/>
                        <xsl:with-param name="refRank" select="$refRank"/>
                    </xsl:call-template>

                    <xsl:call-template name="output-section-subgroup">
                        <xsl:with-param name="sectionName" select="$sectionName"/>
                        <xsl:with-param name="title" select="$OtherTitle"/>
                        <xsl:with-param name="mode" select="'other'"/>
                        <xsl:with-param name="refRank" select="$refRank"/>
                    </xsl:call-template>

                    <br/>
                </li>
            </xsl:if>
        </xsl:for-each>
    </ul>
</xsl:template>

<xsl:template match="/">
    <xsl:variable name="refRank">
        <xsl:call-template name="version-rank">
            <xsl:with-param name="v" select="$refVersion"/>
        </xsl:call-template>
    </xsl:variable>

    <html>
    <body title="EFA Changelog (cummulated)">
    <xsl:choose>
		<xsl:when test="$langcode='de'">
			<xsl:variable name="NEWITEM_PREFIX" select="'neu:'"/>
			<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
			<h1><b>EFA Versionshistorie (zusammengefasst)</b></h1>
		</xsl:when>
		<xsl:otherwise>
			<xsl:variable name="NEWITEM_PREFIX" select="'new:'"/>
			<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
			<h1><b>EFA Changelog (cummulated)</b></h1>
		</xsl:otherwise>
	</xsl:choose>
    <b>Referenzversion: <xsl:value-of select="$refVersion"/></b><br/><br/>

    <!-- Neueste Version ermitteln: höchster Rank > refRank (direkter DOM-Zugriff) -->
    <xsl:variable name="newestVersion" select="/efaOnlineUpdate/Version[
        number(
            (number(substring-before(substring-before(concat(VersionID, '#'), '#'), '.')) * 10000000000) +
            (number(substring-before(substring-after(substring-before(concat(VersionID, '#'), '#'), '.'), '.')) * 100000000) +
            (number(substring-after(substring-after(substring-before(concat(VersionID, '#'), '#'), '.'), '.')) * 1000000) +
            (number(substring-after(concat(substring-before(concat(VersionID, '#'), '#'), '_'), '_')) * 1000) +
            (number(substring-after(concat(VersionID, '#'), '#')))
        ) &gt; number($refRank)
    ][1]"/>

    <!-- MinimumJavaVersion und MinimumEfaCloudVersion aus neuester Version -->
    <xsl:if test="$newestVersion/MinimumJavaVersion or $newestVersion/MinimumEfaCloudVersion">
        <b>Anforderungen (neueste Version: <xsl:value-of select="$newestVersion/VersionID"/>):</b><br/>
        <xsl:if test="$newestVersion/MinimumJavaVersion">
			<xsl:choose> 
				<xsl:when test="$langcode='de'">                        
                	Minimale Java Version:
                </xsl:when>
                <xsl:otherwise>
                	Minimal Java Version:
                </xsl:otherwise>
            </xsl:choose>
           <xsl:value-of select="$newestVersion/MinimumJavaVersion"/><br/>
        </xsl:if>
        <xsl:if test="$newestVersion/MinimumEfaCloudVersion">
        	<xsl:choose> 
				<xsl:when test="$langcode='de'">                        
                	Minimale Java Version:
                </xsl:when>
                <xsl:otherwise>
                	Minimal Java Version:
                </xsl:otherwise>
            </xsl:choose>
            <xsl:value-of select="$newestVersion/MinimumEfaCloudVersion"/><br/>
        </xsl:if>
        <br/>
    </xsl:if>

    <!-- Wichtige Hinweise nur aus neuester Version -->
	<xsl:choose> 
		<xsl:when test="$langcode='de'">
		    <xsl:if test="$newestVersion/ShowNotice[@lang='de']">
		        <b><i><font color="#EE0000">Wichtige Hinweise:</font></i></b>
		        <ul>
		            <xsl:for-each select="$newestVersion/ShowNotice[@lang='de']">
		                <li><xsl:value-of select="."/></li>
		            </xsl:for-each>
		        </ul>
		    </xsl:if>
	    </xsl:when>
	    <xsl:otherwise>
	    	<xsl:if test="$newestVersion/ShowNotice[@lang='de']">
		        <b><i><font color="#EE0000">Important notice:</font></i></b>
		        <ul>
		            <xsl:for-each select="$newestVersion/ShowNotice[@lang='de']">
		                <li><xsl:value-of select="."/></li>
		            </xsl:for-each>
		        </ul>
		    </xsl:if>
		</xsl:otherwise>
    </xsl:choose>     

    <!-- Kumulierte Änderungen section-zentriert mit Untergruppen -->
    <xsl:call-template name="output-sections-with-subgroups">
        <xsl:with-param name="refRank" select="$refRank"/>
    </xsl:call-template>

    </body>
    </html>
</xsl:template>
</xsl:stylesheet>
