<?xml version ="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" version="4.0"/>
<xsl:template match ="/">
    <html>
    <body title="EFA Versionshistorie">
    <h1><b>EFA Versionshistorie</b></h1>
    <table border="0">
    <xsl:for-each select="efaOnlineUpdate/Version">
        <tr height="134"><td valign="top" align="center" width="128" background="line.png">
            <table width="132" height="130" border="0"><tr height="128"><td background="circle.png" valign="middle" align="center" height="128" width="132">
        	<font color="white"><b><xsl:value-of select="VersionID"/></b><br/><br/><xsl:value-of select="ReleaseDate"/></font>
            </td></tr></table>
        </td><td valign="top">
        <b><br/><br/><xsl:if test="not(count(MinimumJavaVersion) = 0)">Minimale Java Version: <xsl:value-of select="MinimumJavaVersion"/><br/></xsl:if>
            <xsl:if test="not(count(MinimumEfaCloudVersion) = 0)">Minimale efaCloud Version: <xsl:value-of select="MinimumEfaCloudVersion"/><br/></xsl:if></b>
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
	<xsl:for-each select="Changes[@lang='de']">
	    <xsl:if test="ChangeItem[(starts-with(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'neu:'))]">
        	<b>Neuerungen</b>
	        <ul>
	        <xsl:for-each select="ChangeItem[starts-with(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'neu:')]">
		    <li>
		        <xsl:value-of select="substring-after(.,':')"/>
		    </li>
	        </xsl:for-each>
	        </ul>
        </xsl:if>
	</xsl:for-each>
	<xsl:for-each select="Changes[@lang='de']">
	    <xsl:if test="ChangeItem[(starts-with(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'bugfix:'))]">
        	<b>Korrekturen</b>
	        <ul>
	        <xsl:for-each select="ChangeItem[starts-with(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'bugfix:')]">
		    <li>
		        <xsl:value-of select="substring-after(.,':')"/>
		    </li>
	        </xsl:for-each>
	        </ul>
        </xsl:if>
	</xsl:for-each>
	<xsl:for-each select="Changes[@lang='de']">
	    <xsl:if test="ChangeItem[
		not(starts-with(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'neu:')) and
		not(starts-with(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'bugfix:'))
		]">
    		<b>Sonstige Änderungen</b>
		<ul>
		    <xsl:for-each select="ChangeItem[
			not(starts-with(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'neu:')) and
		    not(starts-with(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'bugfix:'))
		    ]">
		    <li>
			<xsl:value-of select="."/>
		    </li>
		    </xsl:for-each>
		</ul>
	    </xsl:if>
	</xsl:for-each>
	</td></tr>
    </xsl:for-each>
    </table>
    </body>
    </html>
</xsl:template>
</xsl:stylesheet>
