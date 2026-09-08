<?xml version ="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" version="4.0"/>

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

<!-- Gibt einen Bereich (Neuerungen / Korrekturen) aus, gruppiert nach Section
     in der Reihenfolge aus /efaOnlineUpdate/Sections -->
<xsl:template name="output-change-group">
    <xsl:param name="title"/>
    <xsl:param name="prefix"/>

    <!-- Nur deutsche ChangeItems des gewünschten Typs -->
    <xsl:variable name="changes" select="Changes[@lang='de']/ChangeItem[
        starts-with(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'), $prefix)
    ]"/>

    <xsl:if test="$changes">
        <b><xsl:value-of select="$title"/></b>
        <ul>
            <!-- Sections exakt in der Reihenfolge aus eou.xml durchlaufen -->
            <xsl:for-each select="/efaOnlineUpdate/Sections/Section">
                <xsl:variable name="sectionName" select="@name"/>
                <xsl:variable name="sectionChanges" select="$changes[@section = $sectionName]"/>

                <!-- Section nur anzeigen, wenn es dafür Einträge gibt -->
                <xsl:if test="$sectionChanges">
                    <li>
                        <b>
                            <xsl:call-template name="section-label-de">
                                <xsl:with-param name="sectionName" select="$sectionName"/>
                            </xsl:call-template>
                        </b>
                        <ul>
                            <xsl:for-each select="$sectionChanges">
                                <li>
                                    <xsl:value-of select="substring-after(., ':')"/>
                                </li>
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
    <html>
    <body title="EFA Versionshistorie">
    <h1><b>EFA Versionshistorie</b></h1>
    <table border="0">
    <xsl:for-each select="efaOnlineUpdate/Version">
        <!-- Nur Versionen ab 2.2.2 anzeigen -->
        <xsl:if test="not(VersionID[(starts-with(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'2.2.1'))]
            or VersionID[(starts-with(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'2.1'))]
            or VersionID[(starts-with(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'2.0'))]
            or VersionID[(starts-with(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'1.'))])">

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

                    <!-- Neuerungen -->
                    <xsl:call-template name="output-change-group">
                        <xsl:with-param name="title" select="'Neuerungen'"/>
                        <xsl:with-param name="prefix" select="'neu:'"/>
                    </xsl:call-template>

                    <!-- Korrekturen -->
                    <xsl:call-template name="output-change-group">
                        <xsl:with-param name="title" select="'Korrekturen'"/>
                        <xsl:with-param name="prefix" select="'bugfix:'"/>
                    </xsl:call-template>

                    <!-- Sonstige Änderungen -->
                    <xsl:variable name="otherChanges" select="Changes[@lang='de']/ChangeItem[
                        not(starts-with(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'neu:')) and
                        not(starts-with(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'bugfix:'))
                    ]"/>

                    <xsl:if test="$otherChanges">
                        <b>Sonstige Änderungen</b>
                        <ul>
                            <!-- Sections exakt in der Reihenfolge aus eou.xml durchlaufen -->
                            <xsl:for-each select="/efaOnlineUpdate/Sections/Section">
                                <xsl:variable name="sectionName" select="@name"/>
                                <xsl:variable name="sectionChanges" select="$otherChanges[@section = $sectionName]"/>

                                <xsl:if test="$sectionChanges">
                                    <li>
                                        <b>
                                            <xsl:call-template name="section-label-de">
                                                <xsl:with-param name="sectionName" select="$sectionName"/>
                                            </xsl:call-template>
                                        </b>
                                        <ul>
                                            <xsl:for-each select="$sectionChanges">
                                                <li><xsl:value-of select="substring-after(., ':')"/></li>
                                            </xsl:for-each>
                                        </ul>
                                        <br/>
                                    </li>
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
