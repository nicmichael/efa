<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<!-- Outputs GitHub-Markdown for release notes -->
<xsl:output method="text" encoding="UTF-8"/>

<xsl:param name="refVersion" select="'2.4.0_00'"/>
<xsl:param name="langcode" select="'de'"/>

<xsl:variable name="NL" select="'&#10;'"/>
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

<!-- Section label (translated or internal name) -->
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

<!-- Version rank (unchanged from original) -->
<xsl:template name="version-rank">
    <xsl:param name="v"/>
    <xsl:variable name="main" select="substring-before(concat($v, '#'), '#')"/>
    <xsl:variable name="buildStr">
        <xsl:choose>
            <xsl:when test="contains($v, '#')">
                <xsl:value-of select="substring-after($v, '#')"/>
            </xsl:when>
            <xsl:otherwise>999</xsl:otherwise>
        </xsl:choose>
    </xsl:variable>
    <xsl:variable name="mainNoUpdate" select="substring-before(concat($main, '_'), '_')"/>
    <xsl:variable name="updateStr">
        <xsl:choose>
            <xsl:when test="string-length(substring-after($main, '_')) &gt; 0">
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

<!-- Position of newest version (unchanged from original) -->
<xsl:template name="find-newest">
    <xsl:param name="index" select="1"/>
    <xsl:param name="bestRank" select="0"/>
    <xsl:param name="bestPos" select="0"/>
    <xsl:variable name="cur" select="/efaOnlineUpdate/Version[$index]"/>
    <xsl:choose>
        <xsl:when test="not($cur)">
            <xsl:value-of select="$bestPos"/>
        </xsl:when>
        <xsl:otherwise>
            <xsl:variable name="curRank">
                <xsl:call-template name="version-rank">
                    <xsl:with-param name="v" select="$cur/VersionID"/>
                </xsl:call-template>
            </xsl:variable>
            <xsl:choose>
                <xsl:when test="number($curRank) &gt; $bestRank">
                    <xsl:call-template name="find-newest">
                        <xsl:with-param name="index" select="$index + 1"/>
                        <xsl:with-param name="bestRank" select="number($curRank)"/>
                        <xsl:with-param name="bestPos" select="$index"/>
                    </xsl:call-template>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="find-newest">
                        <xsl:with-param name="index" select="$index + 1"/>
                        <xsl:with-param name="bestRank" select="$bestRank"/>
                        <xsl:with-param name="bestPos" select="$bestPos"/>
                    </xsl:call-template>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>

<!-- Outputs "1" if the section has entries of the given mode in newer versions -->
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
                ($mode='new' and starts-with(translate(normalize-space(.), $UPPER, $LOWER), $NEWITEM_PREFIX))
                or
                ($mode='bugfix' and starts-with(translate(normalize-space(.), $UPPER, $LOWER), 'bugfix:'))
                or
                ($mode='other'
                    and not(starts-with(translate(normalize-space(.), $UPPER, $LOWER), $NEWITEM_PREFIX))
                    and not(starts-with(translate(normalize-space(.), $UPPER, $LOWER), 'bugfix:')))
            ]">1</xsl:if>
    </xsl:for-each>
</xsl:template>

<!-- Outputs a "#### Title" block with "- item" bullets for one mode -->
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
        <xsl:text>#### </xsl:text><xsl:value-of select="$title"/><xsl:value-of select="$NL"/><xsl:value-of select="$NL"/>
        <xsl:for-each select="/efaOnlineUpdate/Version">
            <xsl:variable name="vRank">
                <xsl:call-template name="version-rank">
                    <xsl:with-param name="v" select="VersionID"/>
                </xsl:call-template>
            </xsl:variable>
            <xsl:if test="number($vRank) &gt; number($refRank)">
                <xsl:for-each select="Changes[@lang=$langcode]/ChangeItem[@section=$sectionName][
                    ($mode='new' and starts-with(translate(normalize-space(.), $UPPER, $LOWER), $NEWITEM_PREFIX))
                    or
                    ($mode='bugfix' and starts-with(translate(normalize-space(.), $UPPER, $LOWER), 'bugfix:'))
                    or
                    ($mode='other'
                        and not(starts-with(translate(normalize-space(.), $UPPER, $LOWER), $NEWITEM_PREFIX))
                        and not(starts-with(translate(normalize-space(.), $UPPER, $LOWER), 'bugfix:')))
                ]">
                    <xsl:text>- </xsl:text>
                    <xsl:choose>
                        <xsl:when test="$mode='other'">
                            <xsl:value-of select="normalize-space(.)"/>
                        </xsl:when>
                        <xsl:otherwise>
                            <xsl:value-of select="normalize-space(substring-after(., ':'))"/>
                        </xsl:otherwise>
                    </xsl:choose>
                    <xsl:value-of select="$NL"/>
                </xsl:for-each>
            </xsl:if>
        </xsl:for-each>
        <xsl:value-of select="$NL"/>
    </xsl:if>
</xsl:template>

<!-- Root -->
<xsl:template match="/">
    <xsl:variable name="refRank">
        <xsl:call-template name="version-rank">
            <xsl:with-param name="v" select="$refVersion"/>
        </xsl:call-template>
    </xsl:variable>
    <xsl:variable name="newestPos">
        <xsl:call-template name="find-newest"/>
    </xsl:variable>
    <xsl:variable name="newestVersion" select="/efaOnlineUpdate/Version[number($newestPos) &gt; 0][number($newestPos)]"/>

    <!-- Title -->
    <xsl:choose>
        <xsl:when test="$langcode='de'">## EFA Versionshistorie (zusammengefasst)</xsl:when>
        <xsl:otherwise>## EFA Changelog (cumulated)</xsl:otherwise>
    </xsl:choose>
    <xsl:value-of select="$NL"/><xsl:value-of select="$NL"/>

    <xsl:text>**Referenzversion:** `</xsl:text><xsl:value-of select="$refVersion"/><xsl:text>`</xsl:text>
    <xsl:value-of select="$NL"/><xsl:value-of select="$NL"/>

    <!-- Requirements from newest version -->
    <xsl:if test="$newestVersion/MinimumJavaVersion or $newestVersion/MinimumEfaCloudVersion">
        <xsl:choose>
            <xsl:when test="$langcode='de'">**Anforderungen** (neueste Version: `<xsl:value-of select="$newestVersion/VersionID"/>`):</xsl:when>
            <xsl:otherwise>**Requirements** (newest version: `<xsl:value-of select="$newestVersion/VersionID"/>`):</xsl:otherwise>
        </xsl:choose>
        <xsl:value-of select="$NL"/><xsl:value-of select="$NL"/>
        <xsl:if test="$newestVersion/MinimumJavaVersion">
            <xsl:text>- </xsl:text>
            <xsl:choose>
                <xsl:when test="$langcode='de'">Minimale Java Version: </xsl:when>
                <xsl:otherwise>Minimum Java version: </xsl:otherwise>
            </xsl:choose>
            <xsl:value-of select="$newestVersion/MinimumJavaVersion"/><xsl:value-of select="$NL"/>
        </xsl:if>
        <xsl:if test="$newestVersion/MinimumEfaCloudVersion">
            <xsl:text>- </xsl:text>
            <xsl:choose>
                <xsl:when test="$langcode='de'">Minimale efaCloud Version: </xsl:when>
                <xsl:otherwise>Minimum efaCloud version: </xsl:otherwise>
            </xsl:choose>
            <xsl:value-of select="$newestVersion/MinimumEfaCloudVersion"/><xsl:value-of select="$NL"/>
        </xsl:if>
        <xsl:value-of select="$NL"/>
    </xsl:if>

    <!-- Important notices (newest version only) -->
    <xsl:if test="$newestVersion/ShowNotice[@lang='de']">
        <xsl:choose>
            <xsl:when test="$langcode='de'">> ⚠️ **Wichtige Hinweise:**</xsl:when>
            <xsl:otherwise>> ⚠️ **Important notice:**</xsl:otherwise>
        </xsl:choose>
        <xsl:value-of select="$NL"/>
        <xsl:for-each select="$newestVersion/ShowNotice[@lang='de']">
            <xsl:text>> - </xsl:text><xsl:value-of select="normalize-space(.)"/><xsl:value-of select="$NL"/>
        </xsl:for-each>
        <xsl:value-of select="$NL"/>
    </xsl:if>

    <!-- Changes grouped by section and type -->
    <xsl:choose>
        <xsl:when test="$langcode='de'">### Änderungen nach Bereichen</xsl:when>
        <xsl:otherwise>### Changes by Area</xsl:otherwise>
    </xsl:choose>
    <xsl:value-of select="$NL"/><xsl:value-of select="$NL"/>

    <xsl:for-each select="/efaOnlineUpdate/Sections/Section">
        <xsl:variable name="sectionName" select="@name"/>
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
            <xsl:text>### </xsl:text>
            <xsl:call-template name="section-label">
                <xsl:with-param name="sectionName" select="$sectionName"/>
            </xsl:call-template>
            <xsl:value-of select="$NL"/><xsl:value-of select="$NL"/>

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
        </xsl:if>
    </xsl:for-each>
</xsl:template>

</xsl:stylesheet>
