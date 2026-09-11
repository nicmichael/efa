<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" version="4.0"/>

<!-- Konstanten für case-insensitive Vergleiche -->
<xsl:variable name="UPPER" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ'"/>
<xsl:variable name="LOWER" select="'abcdefghijklmnopqrstuvwxyzäöü'"/>

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

<!-- Gibt eine einzelne Liste von ChangeItems aus -->
<xsl:template name="render-change-list">
    <xsl:param name="items"/>

    <ul>
        <xsl:for-each select="$items">
            <li>
                <xsl:choose>
                    <xsl:when test="contains(., ':')">
                        <xsl:value-of select="normalize-space(substring-after(., ':'))"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="normalize-space(.)"/>
                    </xsl:otherwise>
                </xsl:choose>
            </li>
        </xsl:for-each>
    </ul>
</xsl:template>

<!-- Gibt alle Änderungen einer Section aus, innerhalb der Section gruppiert nach
     Neuerungen / Korrekturen / Sonstige Änderungen -->
<xsl:template name="output-section-group">
    <xsl:param name="sectionName"/>

    <xsl:variable name="sectionChanges" select="Changes[@lang='de']/ChangeItem[@section = $sectionName]"/>

    <xsl:variable name="newChanges"
        select="$sectionChanges[starts-with(translate(normalize-space(.), $UPPER, $LOWER), 'neu:')]"/>

    <xsl:variable name="bugfixChanges"
        select="$sectionChanges[starts-with(translate(normalize-space(.), $UPPER, $LOWER), 'bugfix:')]"/>

    <xsl:variable name="otherChanges"
        select="$sectionChanges[
            not(starts-with(translate(normalize-space(.), $UPPER, $LOWER), 'neu:')) and
            not(starts-with(translate(normalize-space(.), $UPPER, $LOWER), 'bugfix:'))
        ]"/>

    <xsl:if test="$sectionChanges">
        <li>
            <b>
                <xsl:call-template name="section-label-de">
                    <xsl:with-param name="sectionName" select="$sectionName"/>
                </xsl:call-template>
            </b>

            <ul>
                <xsl:if test="$newChanges">
                    <li>
                        <b>Neuerungen</b>
                        <xsl:call-template name="render-change-list">
                            <xsl:with-param name="items" select="$newChanges"/>
                        </xsl:call-template>
                    </li>
                </xsl:if>

                <xsl:if test="$bugfixChanges">
                    <li>
                        <b>Korrekturen</b>
                        <xsl:call-template name="render-change-list">
                            <xsl:with-param name="items" select="$bugfixChanges"/>
                        </xsl:call-template>
                    </li>
                </xsl:if>

                <xsl:if test="$otherChanges">
                    <li>
                        <b>Sonstige Änderungen</b>
                        <xsl:call-template name="render-change-list">
                            <xsl:with-param name="items" select="$otherChanges"/>
                        </xsl:call-template>
                    </li>
                </xsl:if>
            </ul>

            <br/>
        </li>
    </xsl:if>
</xsl:template>

<xsl:template match="/">
    <html>
    <body title="EFA Versionshistorie">
    <h1><b>EFA Versionshistorie</b></h1>
    <table border="0">
    <xsl:for-each select="efaOnlineUpdate/Version">
        <!-- Nur Versionen ab 2.2.2 anzeigen -->
        <xsl:if test="not(VersionID[
            starts-with(translate(., $UPPER, $LOWER), '2.2.1') or
            starts-with(translate(., $UPPER, $LOWER), '2.1') or
            starts-with(translate(., $UPPER, $LOWER), '2.0') or
            starts-with(translate(., $UPPER, $LOWER), '1.')
        ])">

            <!-- Versionsblock -->
            <tr height="134">
                <td valign="top" align="center" width="128" background="line.png">
                    <table width="132" height="130" border="0">
                        <tr height="128">
                            <td background="circle.png" valign="middle" align="center" height="128" width="132">
                                <font color="yellow"><b><xsl:value-of select="VersionID"/></b><br/><br/></font>
                                <font color="yellow"><xsl:value-of select="ReleaseDate"/></font>
                            </td>
                        </tr>
                    </table>
                </td>
                <td valign="top">
                    <b><br/><br/>
                        <xsl:if test="MinimumJavaVersion">
                            Minimale Java Version: <xsl:value-of select="MinimumJavaVersion"/><br/>
                        </xsl:if>
                        <xsl:if test="MinimumEfaCloudVersion">
                            Minimale efaCloud Version: <xsl:value-of select="MinimumEfaCloudVersion"/><br/>
                        </xsl:if>
                    </b>

                    <!-- Wichtige Hinweise nur auf Deutsch -->
                    <xsl:if test="ShowNotice[@lang='de']">
                        <br/><b><i><font color="#EE0000">Wichtige Hinweise:</font></i></b>
                        <ul>
                            <xsl:for-each select="ShowNotice[@lang='de']">
                                <li><xsl:value-of select="."/></li>
                            </xsl:for-each>
                        </ul>
                    </xsl:if>

                    <!-- Änderungen nach Section, innerhalb der Section nach Typ gruppiert -->
                    <xsl:if test="Changes[@lang='de']/ChangeItem">
                        <ul>
                            <!-- Zuerst alle definierten Sections in der Reihenfolge aus eou.xml -->
                            <xsl:for-each select="/efaOnlineUpdate/Sections/Section">
                                <xsl:call-template name="output-section-group">
                                    <xsl:with-param name="sectionName" select="@name"/>
                                </xsl:call-template>
                            </xsl:for-each>

                            <!-- Danach Sections ausgeben, die in ChangeItems vorkommen,
                                 aber nicht im globalen Sections-Block definiert sind -->
                            <xsl:for-each select="Changes[@lang='de']/ChangeItem[not(@section = preceding-sibling::ChangeItem/@section)]">
                                <xsl:variable name="sectionName" select="@section"/>
                                <xsl:if test="not(/efaOnlineUpdate/Sections/Section[@name = $sectionName])">
                                    <xsl:call-template name="output-section-group">
                                        <xsl:with-param name="sectionName" select="$sectionName"/>
                                    </xsl:call-template>
                                </xsl:if>
                            </xsl:for-each>
                        </ul>
                    </xsl:if>

                </td>
            </tr>
        </xsl:if>
    </xsl:for-each>
    </table>
    </body>
    </html>
</xsl:template>
</xsl:stylesheet>
