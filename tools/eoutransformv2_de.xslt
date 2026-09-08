<?xml version ="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" version="4.0"/>

<xsl:key name="sections-by-name" match="efaOnlineUpdate/Sections/Section" use="@name"/>
<xsl:key name="changes-by-section-new" match="ChangeItem[starts-with(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'neu:')]" use="@section"/>
<xsl:key name="changes-by-section-bugfix" match="ChangeItem[starts-with(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'bugfix:')]" use="@section"/>

<xsl:template name="section-label-de">
    <xsl:param name="sectionName"/>
    <xsl:choose>
        <xsl:when test="key('sections-by-name', $sectionName)/Translation[@lang='de']">
            <xsl:value-of select="key('sections-by-name', $sectionName)/Translation[@lang='de'][1]"/>
        </xsl:when>
        <xsl:otherwise>
            <xsl:value-of select="$sectionName"/>
        </xsl:otherwise>
    </xsl:choose>
</xsl:template>

<xsl:template name="output-change-group">
    <xsl:param name="lang"/>
    <xsl:param name="title"/>
    <xsl:param name="prefix"/>
    <xsl:param name="keyName"/>

    <xsl:variable name="changes" select="Changes[@lang=$lang]/ChangeItem[
        starts-with(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'), $prefix)
    ]"/>

    <xsl:if test="$changes">
        <b><xsl:value-of select="$title"/></b>
        <xsl:for-each select="$changes[generate-id() = generate-id(key($keyName, @section)[1])]">
            <xsl:sort select="@section"/>
            <xsl:variable name="sectionName" select="@section"/>
            <xsl:variable name="sectionLabel">
                <xsl:call-template name="section-label-de">
                    <xsl:with-param name="sectionName" select="$sectionName"/>
                </xsl:call-template>
            </xsl:variable>

            <br/><b><xsl:value-of select="$sectionLabel"/></b>
            <ul>
                <xsl:for-each select="key($keyName, $sectionName)">
                    <li>
                        <xsl:value-of select="substring-after(., ':')"/>
                    </li>
                </xsl:for-each>
            </ul>
        </xsl:for-each>
    </xsl:if>
</xsl:template>

<xsl:template match ="/">
    <html>
    <body title="EFA Versionshistorie">
    <h1><b>EFA Versionshistorie</b></h1>
    <table border="0">
    <xsl:for-each select="efaOnlineUpdate/Version">
	<!--only create changelog for version 2.2.2 and above -->
	<xsl:if test="not(VersionID[(starts-with(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'2.2.1'))] 
	    or VersionID[(starts-with(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'2.1'))]
	    or VersionID[(starts-with(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'2.0'))]
	    or VersionID[(starts-with(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'1.'))])">
	<!-- create a circle with current version number in the left column -->
        <tr height="134"><td valign="top" align="center" width="128" background="line.png">
            <table width="132" height="130" border="0"><tr height="128"><td background="circle.png" valign="middle" align="center" height="128" width="132">
		<font color="yellow"><b><xsl:value-of select="VersionID"/></b><br/><br/></font>
		<font color="yellow"><xsl:value-of select="ReleaseDate"/></font>
            </td></tr></table>
        </td><td valign="top">
        <b><br/><br/>
            <xsl:if test="not(count(MinimumJavaVersion) = 0)">Minimale Java Version: <xsl:value-of select="MinimumJavaVersion"/><br/></xsl:if>
            <xsl:if test="not(count(MinimumEfaCloudVersion) = 0)">Minimale efaCloud Version: <xsl:value-of select="MinimumEfaCloudVersion"/><br/></xsl:if>
        </b>

	<xsl:if test="not(count(ShowNotice) = 0)">
	<br/><b><i><font color="#EE0000">Wichtige Hinweise:</font></i></b>
	<ul>
	    <xsl:for-each select="ShowNotice[@lang='de']">
	    <li>
		<xsl:value-of select="."/>
	    </li>
	    </xsl:for-each>
	</ul>
	</xsl:if>

        <xsl:call-template name="output-change-group">
            <xsl:with-param name="lang" select="'de'"/>
            <xsl:with-param name="title" select="'Neuerungen'"/>
            <xsl:with-param name="prefix" select="'neu:'"/>
            <xsl:with-param name="keyName" select="'changes-by-section-new'"/>
        </xsl:call-template>

        <xsl:call-template name="output-change-group">
            <xsl:with-param name="lang" select="'de'"/>
            <xsl:with-param name="title" select="'Korrekturen'"/>
            <xsl:with-param name="prefix" select="'bugfix:'"/>
            <xsl:with-param name="keyName" select="'changes-by-section-bugfix'"/>
        </xsl:call-template>

	<xsl:if test="Changes[@lang='de']/ChangeItem[
		not(starts-with(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'neu:')) and
		not(starts-with(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'bugfix:'))
	]">
	    <b>Sonstige Änderungen</b>
	    <ul>
	        <xsl:for-each select="Changes[@lang='de']/ChangeItem[
			not(starts-with(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'neu:')) and
			not(starts-with(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'bugfix:'))
		]">
	            <li><xsl:value-of select="."/></li>
	        </xsl:for-each>
	    </ul>
	</xsl:if>

	</td></tr>
    </xsl:if>
    </xsl:for-each>
    </table>
    </body>
    </html>
</xsl:template>
</xsl:stylesheet>
