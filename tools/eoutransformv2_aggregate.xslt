<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" version="4.0" encoding="UTF-8"/>

<!--
  refVersion formats:
  - Beta:      major.minor.patch#build     (e.g. 2.5.3#10)
  - Beta:      major.minor.patch_#build    (e.g. 2.5.3_#10, as used in eou.xml)
  - Productive: major.minor.patch_update   (e.g. 2.4.0_00, 2.5.3_17)
-->
<xsl:param name="refVersion" select="'2.4.0_00'"/>
<xsl:param name="langcode" select="'de'"/>

<!--
  Transformation script for an eou.xml where each ChangeItem has a section name.
  Used by the efaOnlineUpdate feature in efa.

  It aggregates the changes of all versions newer than $refVersion and outputs:
  - the newest version number, minimum Java / efaCloud versions and important notices
  - the changes grouped by section (in the order defined in <Sections>)
    and by type (new / bugfix / other).
-->

<!-- Constants for case-insensitive comparisons -->
<xsl:variable name="UPPER" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ'"/>
<xsl:variable name="LOWER" select="'abcdefghijklmnopqrstuvwxyzäöü'"/>

<!-- Prefix for new-feature entries depending on language -->
<xsl:variable name="NEWITEM_PREFIX">
    <xsl:choose>
        <xsl:when test="$langcode='de'">neu:</xsl:when>
        <xsl:otherwise>new:</xsl:otherwise>
    </xsl:choose>
</xsl:variable>

<!-- Subgroup titles depending on language -->
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


<!--
  Returns the translated section label for the current language.
  Falls back to the internal section name if no translation exists.
-->
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
  Computes a numeric rank for a version string.

  Rank = major*1e10 + minor*1e8 + patch*1e6 + update*1e3 + build

  - Beta (contains '#'): build is taken from the part after '#'.
                          Accepts both "2.5.3#10" and "2.5.3_#10".
  - Productive (no '#'): build is set to 999, so that a productive release
                          is always ranked higher than any beta build of the same
                          major.minor.patch version.
  - Update part (after '_') is only used if it is not empty.
-->
<xsl:template name="version-rank">
    <xsl:param name="v"/>

    <!-- Everything before '#' (e.g. "2.5.3_" for beta, "2.5.3_17" for productive) -->
    <xsl:variable name="main" select="substring-before(concat($v, '#'), '#')"/>

    <!-- Build number: part after '#' for beta, 999 for productive releases -->
    <xsl:variable name="buildStr">
        <xsl:choose>
            <xsl:when test="contains($v, '#')">
                <xsl:value-of select="substring-after($v, '#')"/>
            </xsl:when>
            <!-- Productive release: always higher than any beta build -->
            <xsl:otherwise>999</xsl:otherwise>
        </xsl:choose>
    </xsl:variable>

    <!-- "2.5.3" without the update part -->
    <xsl:variable name="mainNoUpdate" select="substring-before(concat($main, '_'), '_')"/>

    <!-- Update number: only used if non-empty (e.g. "17" for 2.5.3_17, not for 2.5.3_#16) -->
    <xsl:variable name="updateStr">
        <xsl:choose>
            <xsl:when test="string-length(substring-after($main, '_')) &gt; 0">
                <xsl:value-of select="substring-after($main, '_')"/>
            </xsl:when>
            <xsl:otherwise>0</xsl:otherwise>
        </xsl:choose>
    </xsl:variable>

    <!-- Split major.minor.patch -->
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

<!--
  Finds the position (1-based) of the Version element with the highest rank.
  Walks through the Version elements recursively by index.
  Returns 0 if there are no Version elements.
-->
<xsl:template name="find-newest">
    <xsl:param name="index" select="1"/>
    <xsl:param name="bestRank" select="0"/>
    <xsl:param name="bestPos" select="0"/>

    <xsl:variable name="cur" select="/efaOnlineUpdate/Version[$index]"/>

    <xsl:choose>
        <!-- No more Version elements: return the best position found so far -->
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
                <!-- Higher rank found: remember it and continue with the next index -->
                <xsl:when test="number($curRank) &gt; $bestRank">
                    <xsl:call-template name="find-newest">
                        <xsl:with-param name="index" select="$index + 1"/>
                        <xsl:with-param name="bestRank" select="number($curRank)"/>
                        <xsl:with-param name="bestPos" select="$index"/>
                    </xsl:call-template>
                </xsl:when>
                <!-- Not higher: keep the current best and continue -->
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

<!--
  Checks whether a section contains entries of the given mode (new / bugfix / other)
  in any version newer than $refRank. Outputs "1" if found, nothing otherwise.
-->
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
                    and not(starts-with(translate(normalize-space(.), $UPPER, $LOWER), 'bugfix:'))
                )
            ]">1</xsl:if>
    </xsl:for-each>
</xsl:template>

<!--
  Outputs one subgroup (new / bugfix / other) for a section, if it contains entries.
  Collects matching ChangeItems from all versions newer than $refRank, in original order.
-->
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
                            and not(starts-with(translate(normalize-space(.), $UPPER, $LOWER), 'bugfix:'))
                        )
                    ]">
                        <li>
                            <xsl:choose>
                                <!-- Remove the "neu:" / "bugfix:" prefix; keep "other" entries unchanged -->
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
  Main output: grouped by section.
  Each section gets subgroups for new features, bug fixes and other changes.
-->
<xsl:template name="output-sections-with-subgroups">
    <xsl:param name="refRank"/>

    <b>Änderungen nach Bereichen</b>
    <ul>
        <!-- Keep the order of the sections as defined in <Sections> -->
        <xsl:for-each select="/efaOnlineUpdate/Sections/Section">
            <xsl:variable name="sectionName" select="@name"/>

            <!-- Check whether this section has any relevant entries at all -->
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

                    <!-- Subgroups for this section -->
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

<!-- Root template: builds the complete HTML document -->
<xsl:template match="/">
    <!-- Rank of the reference version -->
    <xsl:variable name="refRank">
        <xsl:call-template name="version-rank">
            <xsl:with-param name="v" select="$refVersion"/>
        </xsl:call-template>
    </xsl:variable>

    <!-- Position of the newest version in the document (0 = none) -->
    <xsl:variable name="newestPos">
        <xsl:call-template name="find-newest"/>
    </xsl:variable>

    <!-- Only use the newest version if it is actually newer than the reference version -->
    <xsl:variable name="newestVersion" select="/efaOnlineUpdate/Version[number($newestPos) &gt; 0][number($newestPos)]"/>

    <html>
    <body title="EFA Changelog (cumulated)">
    <xsl:choose>
        <xsl:when test="$langcode='de'">
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
            <h1><b>EFA Versionshistorie (zusammengefasst)</b></h1>
        </xsl:when>
        <xsl:otherwise>
            <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
            <h1><b>EFA Changelog (cumulated)</b></h1>
        </xsl:otherwise>
    </xsl:choose>
    <b>Referenzversion: <xsl:value-of select="$refVersion"/></b><br/><br/>

    <!-- Requirements from the newest version -->
    <xsl:if test="$newestVersion/MinimumJavaVersion or $newestVersion/MinimumEfaCloudVersion">
        <b>Anforderungen (neueste Version: <xsl:value-of select="$newestVersion/VersionID"/>):</b><br/>

        <xsl:if test="$newestVersion/MinimumJavaVersion">
            <xsl:choose>
                <xsl:when test="$langcode='de'">Minimale Java Version: </xsl:when>
                <xsl:otherwise>Minimum Java version: </xsl:otherwise>
            </xsl:choose>
            <xsl:value-of select="$newestVersion/MinimumJavaVersion"/><br/>
        </xsl:if>

        <xsl:if test="$newestVersion/MinimumEfaCloudVersion">
            <xsl:choose>
                <xsl:when test="$langcode='de'">Minimale efaCloud Version: </xsl:when>
                <xsl:otherwise>Minimum efaCloud version: </xsl:otherwise>
            </xsl:choose>
            <xsl:value-of select="$newestVersion/MinimumEfaCloudVersion"/><br/>
        </xsl:if>
        <br/>
    </xsl:if>

    <!-- Important notices from the newest version only -->
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

    <!-- Cumulated changes grouped by section and type -->
    <xsl:call-template name="output-sections-with-subgroups">
        <xsl:with-param name="refRank" select="$refRank"/>
    </xsl:call-template>

    </body>
    </html>
</xsl:template>

</xsl:stylesheet>
