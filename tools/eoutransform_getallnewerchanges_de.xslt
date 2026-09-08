<?xml version ="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" version="4.0" encoding="UTF-8"/>

<!--
  refVersion:
  - Beta:      major.minor.patch#build   (z.B. 2.5.3#10)
  - Produktiv: major.minor.patch_update  (z.B. 2.4.0_00)
-->
<xsl:param name="refVersion" select="'2.4.0_00'"/>

<!-- Liefert die deutsche Übersetzung einer Section; Fallback ist der interne Section-Name -->
<xsl:template name="section-label-de">
    <xsl:param name="sectionName"/>
    <xsl:choose>
        <xsl:when test="/efaOnlineUpdate/Sections/Section[@name=$sectionName]/Translation[@lang='de']">
            <xsl:value-of select="/efaOnlineUpdate/Sections/Section[@name=$sectionName]/Translation[@lang='de'][1]"/>
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

<!-- Abschnittsausgabe (Neuerungen/Korrektionen/Sonstige) -->
<xsl:template name="output-aggregated-group">
    <xsl:param name="title"/>
    <xsl:param name="mode"/>
    <xsl:param name="refRank"/>

    <!-- Schnelltest: Gibt es überhaupt passende Einträge? -->
    <xsl:variable name="hasAny">
        <xsl:for-each select="/efaOnlineUpdate/Version">
            <xsl:variable name="vRank">
                <xsl:call-template name="version-rank">
                    <xsl:with-param name="v" select="VersionID"/>
                </xsl:call-template>
            </xsl:variable>
            <xsl:if test="number($vRank) &gt; number($refRank) and
                Changes[@lang='de']/ChangeItem[
                    ($mode='new' and starts-with(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'neu:'))
                    or
                    ($mode='bugfix' and starts-with(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'bugfix:'))
                    or
                    ($mode='other'
                        and not(starts-with(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'neu:'))
                        and not(starts-with(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'bugfix:'))
                    )
                ]">1</xsl:if>
        </xsl:for-each>
    </xsl:variable>

    <xsl:if test="contains($hasAny, '1')">
        <b><xsl:value-of select="$title"/></b>
        <ul>
            <!-- Reihenfolge exakt aus Sections -->
            <xsl:for-each select="/efaOnlineUpdate/Sections/Section">
                <xsl:variable name="sectionName" select="@name"/>

                <!-- Prüfen, ob diese Section in neueren Versionen Einträge hat -->
                <xsl:variable name="hasSection">
                    <xsl:for-each select="/efaOnlineUpdate/Version">
                        <xsl:variable name="vRank">
                            <xsl:call-template name="version-rank">
                                <xsl:with-param name="v" select="VersionID"/>
                            </xsl:call-template>
                        </xsl:variable>
                        <xsl:if test="number($vRank) &gt; number($refRank) and
                            Changes[@lang='de']/ChangeItem[@section=$sectionName][
                                ($mode='new' and starts-with(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'neu:'))
                                or
                                ($mode='bugfix' and starts-with(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'bugfix:'))
                                or
                                ($mode='other'
                                    and not(starts-with(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'neu:'))
                                    and not(starts-with(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'bugfix:'))
                                )
                            ]">1</xsl:if>
                    </xsl:for-each>
                </xsl:variable>

                <xsl:if test="contains($hasSection, '1')">
                    <li>
                        <b>
                            <xsl:call-template name="section-label-de">
                                <xsl:with-param name="sectionName" select="$sectionName"/>
                            </xsl:call-template>
                        </b>
                        <ul>
                            <!-- Alle Einträge für diese Section aus allen neueren Versionen -->
                            <xsl:for-each select="/efaOnlineUpdate/Version">
                                <xsl:variable name="vRank">
                                    <xsl:call-template name="version-rank">
                                        <xsl:with-param name="v" select="VersionID"/>
                                    </xsl:call-template>
                                </xsl:variable>
                                <xsl:if test="number($vRank) &gt; number($refRank)">
                                    <xsl:for-each select="Changes[@lang='de']/ChangeItem[@section=$sectionName][
                                        ($mode='new' and starts-with(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'neu:'))
                                        or
                                        ($mode='bugfix' and starts-with(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'bugfix:'))
                                        or
                                        ($mode='other'
                                            and not(starts-with(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'neu:'))
                                            and not(starts-with(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'bugfix:'))
                                        )
                                    ]">
                                        <li>
                                            <xsl:choose>
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
                        <br/>
                    </li>
                </xsl:if>
            </xsl:for-each>
        </ul>
    </xsl:if>
</xsl:template>

<xsl:template match="/">
    <xsl:variable name="refRank">
        <xsl:call-template name="version-rank">
            <xsl:with-param name="v" select="$refVersion"/>
        </xsl:call-template>
    </xsl:variable>

    <html>
    <body title="EFA Versionshistorie (kumuliert)">
    <h1><b>EFA Versionshistorie (kumuliert)</b></h1>
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
            Minimale Java Version: <xsl:value-of select="$newestVersion/MinimumJavaVersion"/><br/>
        </xsl:if>
        <xsl:if test="$newestVersion/MinimumEfaCloudVersion">
            Minimale efaCloud Version: <xsl:value-of select="$newestVersion/MinimumEfaCloudVersion"/><br/>
        </xsl:if>
        <br/>
    </xsl:if>

    <!-- Wichtige Hinweise nur aus neuester Version -->
    <xsl:if test="$newestVersion/ShowNotice[@lang='de']">
        <b><i><font color="#EE0000">Wichtige Hinweise:</font></i></b>
        <ul>
            <xsl:for-each select="$newestVersion/ShowNotice[@lang='de']">
                <li><xsl:value-of select="."/></li>
            </xsl:for-each>
        </ul>
    </xsl:if>


    <!-- Kumulierte Änderungen -->
    <xsl:call-template name="output-aggregated-group">
        <xsl:with-param name="title" select="'Neuerungen'"/>
        <xsl:with-param name="mode" select="'new'"/>
        <xsl:with-param name="refRank" select="$refRank"/>
    </xsl:call-template>

    <xsl:call-template name="output-aggregated-group">
        <xsl:with-param name="title" select="'Korrekturen'"/>
        <xsl:with-param name="mode" select="'bugfix'"/>
        <xsl:with-param name="refRank" select="$refRank"/>
    </xsl:call-template>

    <xsl:call-template name="output-aggregated-group">
        <xsl:with-param name="title" select="'Sonstige Änderungen'"/>
        <xsl:with-param name="mode" select="'other'"/>
        <xsl:with-param name="refRank" select="$refRank"/>
    </xsl:call-template>

    </body>
    </html>
</xsl:template>
</xsl:stylesheet>
