<?xml version ="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:output method="html" version="4.0"/>
<xsl:template match ="/">
    <html>
    <body title="EFA History of Versions">
    <h1><b>EFA History of Versions</b></h1>
    <table border="0">
    <xsl:for-each select="efaOnlineUpdate/Version">
        <tr height="134"><td valign="top" align="center" width="128" background="line.png">
            <table width="132" height="130" border="0"><tr height="128"><td background="circle.png" valign="middle" align="center" height="128" width="132">
        	<font color="white"><b><xsl:value-of select="VersionID"/></b><br/><br/><xsl:value-of select="ReleaseDate"/></font>
            </td></tr></table>
        </td><td valign="top">
        <b><br/><br/><xsl:if test="not(count(MinimumJavaVersion) = 0)">Minimum Java Version: <xsl:value-of select="MinimumJavaVersion"/><br/></xsl:if>
            <xsl:if test="not(count(MinimumEfaCloudVersion) = 0)">Minimum efaCloud Version: <xsl:value-of select="MinimumEfaCloudVersion"/><br/></xsl:if></b>
	<xsl:if test="not(count(ShowNotice) = 0)">
	<br/><b><i><font color="#EE0000">Important notice</font></i></b>
	<ul>
	    <xsl:for-each select="ShowNotice[@lang='en']">
	    <li>
		<xsl:value-of select="."/>
	    </li>
	    </xsl:for-each>
	</ul>
	</xsl:if>
	<xsl:for-each select="Changes[@lang='en']">
	    <xsl:if test="ChangeItem[(starts-with(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'new:'))]">
        	<b>New features</b>
	        <ul>
	        <xsl:for-each select="ChangeItem[starts-with(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'new:')]">
		    <li>
		        <xsl:value-of select="substring-after(.,':')"/>
		    </li>
	        </xsl:for-each>
	        </ul>
        </xsl:if>
	</xsl:for-each>
	<xsl:for-each select="Changes[@lang='en']">
	    <xsl:if test="ChangeItem[(starts-with(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'bugfix:'))]">
        	<b>Fixes</b>
	        <ul>
	        <xsl:for-each select="ChangeItem[starts-with(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'bugfix:')]">
		    <li>
		        <xsl:value-of select="substring-after(.,':')"/>
		    </li>
	        </xsl:for-each>
	        </ul>
        </xsl:if>
	</xsl:for-each>
	<xsl:for-each select="Changes[@lang='en']">
	    <xsl:if test="ChangeItem[
		not(starts-with(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'new:')) and
		not(starts-with(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'bugfix:'))
		]">
    		<b>Miscellaneous</b>
		<ul>
		    <xsl:for-each select="ChangeItem[
			not(starts-with(translate(.,'ABCDEFGHIJKLMNOPQRSTUVWXYZÄÖÜ','abcdefghijklmnopqrstuvwxyzäöü'),'new:')) and
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
