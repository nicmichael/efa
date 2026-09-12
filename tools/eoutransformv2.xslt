<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:param name="langcode" select="'de'"/>
<xsl:output method="html" version="4.0" encoding="UTF-8"/>
<!-- 
Transformation script for an eou.xml which has a section name parameter for each change item.
It is used for automatically creating a changelog_de.html / changelog_en.html file within the build process.
It has one mandatory parameter "langcode" which defaults to 'de'.

It is optimized for generating a changelog for each single efa version.
If you want to aggregate multiple versions into a single changelog, like collecting all changes for a major version,
you can use the eoutransform_aggregate.xslt instead. 

It iterates through all Version nodes in the eou.xml and outputs a table with 
the version number, release date, minimum Java version, minimum efaCloud version, important notices 
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

<!-- Gets the translation for a section (de or en); uses Section-Name as fallback if no translation is available-->
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

<!-- Gibt eine einzelne Liste von ChangeItems aus -->
<xsl:template name="render-change-list">
    <xsl:param name="items"/>
    <xsl:param name="bugfix"/>
    <ul>
        <xsl:for-each select="$items">
            <li>
				<xsl:if test="$bugfix='true'">BugFix: </xsl:if>
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

<!-- Gibt alle Aenderungen einer Section aus, innerhalb der Section gruppiert nach
     Neuerungen / Korrekturen / Sonstige Aenderungen -->
<xsl:template name="output-section-group">
    <xsl:param name="sectionName"/>
    <xsl:param name="versionNode"/>

    <xsl:if test="$versionNode/Changes[@lang=$langcode]/ChangeItem[@section = $sectionName]">
            <b>
                <xsl:call-template name="section-label">
                    <xsl:with-param name="sectionName" select="$sectionName"/>
                </xsl:call-template>
            </b>
                <xsl:if test="$versionNode/Changes[@lang=$langcode]/ChangeItem[
                    @section = $sectionName and
                    starts-with(translate(normalize-space(.), $UPPER, $LOWER), $NEWITEM_PREFIX)
                ]">    
                        <xsl:call-template name="render-change-list">
                            <xsl:with-param name="items"
                                select="$versionNode/Changes[@lang=$langcode]/ChangeItem[
                                    @section = $sectionName and
                                    starts-with(translate(normalize-space(.), $UPPER, $LOWER), $NEWITEM_PREFIX)
                                ]"/>
                            <xsl:with-param name="bugfix"
                            	select="'false'"
                            />
                        </xsl:call-template>
                </xsl:if>

                <xsl:if test="$versionNode/Changes[@lang=$langcode]/ChangeItem[
                    @section = $sectionName and
                    starts-with(translate(normalize-space(.), $UPPER, $LOWER), 'bugfix:')
                ]">
                        <!-- <xsl:text>&#160;&#160;&#160;&#160;</xsl:text>Korrekturen-->
                        <xsl:call-template name="render-change-list">
                            <xsl:with-param name="items"
                                select="$versionNode/Changes[@lang=$langcode]/ChangeItem[
                                    @section = $sectionName and
                                    starts-with(translate(normalize-space(.), $UPPER, $LOWER), 'bugfix:')
                                ]"/>
                            <xsl:with-param name="bugfix"
                            	select="'true'"
                            />
                        </xsl:call-template>
                </xsl:if>

                <xsl:if test="$versionNode/Changes[@lang=$langcode]/ChangeItem[
                    @section = $sectionName and
                    not(starts-with(translate(normalize-space(.), $UPPER, $LOWER), $NEWITEM_PREFIX)) and
                    not(starts-with(translate(normalize-space(.), $UPPER, $LOWER), 'bugfix:'))
                ]">
                        <xsl:call-template name="render-change-list">
                            <xsl:with-param name="items"
                                select="$versionNode/Changes[@lang=$langcode]/ChangeItem[
                                    @section = $sectionName and
                                    not(starts-with(translate(normalize-space(.), $UPPER, $LOWER), $NEWITEM_PREFIX)) and
                                    not(starts-with(translate(normalize-space(.), $UPPER, $LOWER), 'bugfix:'))
                                ]"/>
							<xsl:with-param name="bugfix"
                            	select="'false'"
                            />
                        </xsl:call-template>
                </xsl:if>
    </xsl:if>
</xsl:template>

<xsl:template match="/">
    <html>
    <body title="EFA Changelog">
    <xsl:choose>
		<xsl:when test="$langcode='de'">
			<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
			<h1><b>EFA Versionshistorie</b></h1>
		</xsl:when>
		<xsl:otherwise>
			<meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
			<h1><b>EFA Changelog</b></h1>
		</xsl:otherwise>
	</xsl:choose>
    <table border="0">
    <xsl:for-each select="efaOnlineUpdate/Version">
        <!-- Aktuelle Version fuer stabile Kontextuebergabe merken -->
        <xsl:variable name="thisVersion" select="."/>

        <!-- Nur Versionen ab 2.2.2 anzeigen -->
        <xsl:if test="not(VersionID[
            starts-with(translate(., $UPPER, $LOWER), '2.2.1') or
            starts-with(translate(., $UPPER, $LOWER), '2.1') or
            starts-with(translate(., $UPPER, $LOWER), '2.0') or
            starts-with(translate(., $UPPER, $LOWER), '1.')
        ])">

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
							<xsl:choose> 
								<xsl:when test="$langcode='de'">                        
		                        	Minimale Java Version:
		                        </xsl:when>
		                        <xsl:otherwise>
		                        	Minimal Java Version:
		                        </xsl:otherwise>
		                    </xsl:choose>
			                <xsl:value-of select="MinimumJavaVersion"/><br/>	                    
    					</xsl:if>
                        <xsl:if test="MinimumEfaCloudVersion">
							<xsl:choose> 
								<xsl:when test="$langcode='de'">                        
		                        	Minimale efaCloud Version:
		                        </xsl:when>
		                        <xsl:otherwise>
		                        	Minimal efaCloud Version:
		                        </xsl:otherwise>
		                    </xsl:choose>                        
                        	<xsl:value-of select="MinimumEfaCloudVersion"/><br/>
                        </xsl:if>
                    </b>
					<xsl:choose> 
						<xsl:when test="$langcode='de'">
		                    <xsl:if test="ShowNotice[@lang='de']">
		                        <br/><b><i><font color="#EE0000">Wichtige Hinweise:</font></i></b>
		                        <ul>
		                            <xsl:for-each select="ShowNotice[@lang='de']">
		                                <li><xsl:value-of select="."/></li>
		                            </xsl:for-each>
		                        </ul>
		                    </xsl:if>
	                    </xsl:when>
	                    <xsl:otherwise>
	                    	<xsl:if test="ShowNotice[@lang='en']">
	                        <br/><b><i><font color="#EE0000">Important notice:</font></i></b>
	                        <ul>
	                            <xsl:for-each select="ShowNotice[@lang='en']">
	                                <li><xsl:value-of select="."/></li>
	                            </xsl:for-each>
	                        </ul>
		                    </xsl:if>
						</xsl:otherwise>
                    </xsl:choose>                 

                    <xsl:if test="Changes[@lang=$langcode]/ChangeItem">
                            <!-- Definierte Sections in der Reihenfolge aus eou.xml -->
                            <xsl:for-each select="/efaOnlineUpdate/Sections/Section">
                                <xsl:call-template name="output-section-group">
                                    <xsl:with-param name="sectionName" select="@name"/>
                                    <xsl:with-param name="versionNode" select="$thisVersion"/>
                                </xsl:call-template>
                            </xsl:for-each>

                            <!-- Nicht definierte, aber verwendete Sections -->
                            <xsl:for-each select="Changes[@lang=$langcode]/ChangeItem[not(@section = preceding-sibling::ChangeItem/@section)]">
                                <xsl:variable name="sectionName" select="@section"/>
                                <xsl:if test="not(/efaOnlineUpdate/Sections/Section[@name = $sectionName])">
                                    <xsl:call-template name="output-section-group">
                                        <xsl:with-param name="sectionName" select="$sectionName"/>
                                        <xsl:with-param name="versionNode" select="$thisVersion"/>
                                    </xsl:call-template>
                                </xsl:if>
                            </xsl:for-each>
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
