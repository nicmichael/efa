<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:xalan="http://xml.apache.org/xalan">
  
<xsl:output method="html" encoding="ISO-8859-1"/>

  <xsl:variable name="nameCol" select="5"/>

  <xsl:template match="kilometerliste">
    <html>
      <head>
        <title><xsl:value-of select="listenkopf/titel"/></title>
      </head>
      <body>
        <h1 align="center"><xsl:value-of select="listenkopf/titel"/></h1>
	<xsl:comment>EFA-START</xsl:comment> <!-- Markierung für "nur Tabelle ersetzen" -->
        <xsl:apply-templates select="listenkopf"/>
        <xsl:apply-templates select="wettZeitraumWarnung"/>
        <xsl:apply-templates select="wettBedingungen"/>
        <xsl:apply-templates select="gruppe"/>
        <xsl:apply-templates select="tabelle"/>
        <xsl:apply-templates select="zusatzTabelle"/>
	<xsl:apply-templates select="spezialTabelle"/>
	<xsl:comment>EFA-ENDE</xsl:comment> <!-- Markierung für "nur Tabelle ersetzen" -->
      </body>
    </html>
  </xsl:template>


  <xsl:template match="listenkopf">
    <table border="3" align="center">
    <tr><td>erstellt am:</td><td><xsl:value-of select="auswertungsDatum"/> von <a href="{auswertungsProgramm/@url}"><xsl:value-of select="auswertungsProgramm"/></a></td></tr>
    <tr><td>Art der Auswertung:</td><td><xsl:value-of select="auswertungsArt"/></td></tr>
    <tr><td>Zeitraum:</td><td><xsl:value-of select="auswertungsZeitraum"/></td></tr>
    <tr><td>Ausgewertete Einträge:</td><td><xsl:value-of select="ausgewertet/ausgewerteteEintraege"/></td></tr>
    <tr><td>ausgewertet für:</td><td><xsl:apply-templates select="ausgewertet/ausgewertetFuer"/></td></tr>
    <xsl:apply-templates select="ausgewertet/ausgewertetNurFuer"/>
    <xsl:apply-templates select="ausgewertet/ausgewertetWettNur"/>
    </table><br/>
  </xsl:template>

  <xsl:template match="ausgewertetFuer">
    <xsl:value-of select="."/><xsl:if test="position() != last()"><br/></xsl:if>
  </xsl:template>

  <xsl:template match="ausgewertetNurFuer">
    <tr><td>nur für <xsl:value-of select="@bezeichnung"/>:</td><td><xsl:value-of select="."/></td></tr>
  </xsl:template>

  <xsl:template match="ausgewertetWettNur">
    <tr><td>Ausgabe nur, wenn:</td><td><xsl:value-of select="."/></td></tr>
  </xsl:template>

  <xsl:template match="tabelle">
  

<!--
    <xsl:variable name="sorted">
      <xsl:for-each select="eintrag[not(@zusammenfassung)]">
        <xsl:sort select="name"/>
	<xsl:copy-of select="name"/>
       </xsl:for-each>
    </xsl:variable>
    <table bgcolor="#ddffdd" align="center">
    <tr><th colspan="{$nameCol}" align="center">alphabetische Namensliste</th></tr>
      <xsl:for-each select="xalan:nodeset($sorted)/name[position() mod $nameCol = 1]">       
 	<tr>
	  <xsl:for-each select=".|following-sibling::name[position() &lt; $nameCol]">
  	    <td><a href="#{generate-id(.)}"><xsl:value-of select="."/></a></td>
          </xsl:for-each>
	</tr>  
      </xsl:for-each>	
    </table>
-->

    <!-- nach Namen spaltenweise sortierte Tabelle mit Links zu jeweiligem Namen in der eigentlichen Tabelle -->
    <xsl:if test="eintrag/name">
      <xsl:variable name="sorted">
        <xsl:for-each select="eintrag[not(@zusammenfassung)]/name">
          <xsl:sort select="."/>
	  <name href="{generate-id(.)}"><xsl:value-of select="."/></name>
        </xsl:for-each>
      </xsl:variable>
      <table bgcolor="#ddffdd" align="center">
        <xsl:variable name="zeilen" select="ceiling(count(xalan:nodeset($sorted)/name) div $nameCol)" />
        <tr><th colspan="{$nameCol}" align="center">alphabetische Namensliste</th></tr>
          <xsl:for-each select="xalan:nodeset($sorted)/name[position() &lt;= $zeilen]">       
 	    <tr>
	      <xsl:for-each select=".|following-sibling::name[position() mod $zeilen = 0]">
  	        <td><a href="#{@href}"><xsl:value-of select="."/></a></td>
              </xsl:for-each>
	    </tr>  
          </xsl:for-each>	
      </table><br/>
    </xsl:if>




    <table border="3" align="center">
      <xsl:apply-templates select="tabellenTitel"/>
      <xsl:apply-templates select="eintrag"/>
    </table><br/>
  </xsl:template>

  <xsl:template match="tabellenTitel">
      <tr>
      <xsl:apply-templates select="spaltenTitel"/>
      <xsl:if test="../eintrag[1]/fahrten and ../eintrag[1]/dauer">
        <th>Std/Fahrt</th>
      </xsl:if>
      </tr>
  </xsl:template>

  <xsl:template match="spaltenTitel">
        <xsl:choose>
          <xsl:when test="@colspan">
            <th colspan="{@colspan}"><xsl:value-of select="."/></th>
          </xsl:when>
          <xsl:otherwise>
            <th><xsl:value-of select="."/></th>
	  </xsl:otherwise>
        </xsl:choose>
  </xsl:template>

  <xsl:template match="eintrag">

    <!-- Berechnung der Hintergrundfarbe für Tabellenspalte -->
    <xsl:variable name="color" select="ceiling((position() * 256 div last()) - 1)"/>
    <xsl:variable name="b">
      <xsl:choose>
        <xsl:when test="($color mod 16) &lt; 10"><xsl:value-of select="$color mod 16"/></xsl:when>
	<xsl:otherwise><xsl:value-of select="translate($color mod 16 - 10,'012345','abcdef')"/></xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="a">
      <xsl:choose>
        <xsl:when test="floor($color div 16) &lt; 10"><xsl:value-of select="floor($color div 16)"/></xsl:when>
	<xsl:otherwise><xsl:value-of select="translate(floor($color div 16) - 10,'012345','abcdef')"/></xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <tr bgcolor="#{$a}{$b}ffff">

    <xsl:apply-templates select="nr"/>
    <xsl:apply-templates select="name"/>
    <xsl:apply-templates select="jahrgang"/>
    <xsl:apply-templates select="status"/>
    <xsl:apply-templates select="bezeichnung"/>
    <xsl:apply-templates select="km"/>
    <xsl:apply-templates select="rudkm"/>
    <xsl:apply-templates select="stmkm"/>
    <xsl:apply-templates select="fahrten"/>
    <xsl:apply-templates select="kmfahrt"/>
    <xsl:apply-templates select="dauer"/>
    <xsl:apply-templates select="kmh"/>
    <xsl:apply-templates select="anzversch"/>
    <xsl:apply-templates select="wafakm"/>
    <xsl:apply-templates select="zielfahrten"/>
    <xsl:apply-templates select="zusatzDRV"/>
    <xsl:apply-templates select="zusatzLRVBSommer"/>
    <xsl:apply-templates select="zusatzLRVBWinter"/>
    <xsl:apply-templates select="zusatzLRVBrbWanderWett"/>
    <xsl:apply-templates select="zusatzLRVBrbFahrtenWett"/>
    <xsl:apply-templates select="fahrtenbuch"/>
    <xsl:apply-templates select="wwListe"/>
    <xsl:if test="fahrten and dauer">
      <td align="right"><xsl:value-of select="format-number(dauer div fahrten, '0.0')"/></td>
    </xsl:if>
    </tr>
  </xsl:template>

  <xsl:template match="nr">
    <td><xsl:value-of select="."/></td>
  </xsl:template>

  <xsl:template match="name">
    <td><a name="{generate-id(.)}"><xsl:value-of select="."/></a></td>
  </xsl:template>

  <xsl:template match="jahrgang">
    <td><xsl:value-of select="."/></td>
  </xsl:template>

  <xsl:template match="status">
    <td><xsl:value-of select="."/></td>
  </xsl:template>

  <xsl:template match="bezeichnung">
    <td><xsl:value-of select="."/></td>
  </xsl:template>

  <xsl:template name="graFeld">
    <xsl:variable name="wert">
      <xsl:value-of select="."/>
    </xsl:variable>
    <xsl:variable name="breite">
      <xsl:choose>
        <xsl:when test="@breite >= 0">
	  <xsl:value-of select="@breite"/>
	</xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="@breite * -1"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:choose>
      <xsl:when test="@colspan">
        <xsl:choose>
          <xsl:when test="$wert = '+/- 0'">
	    <td colspan="2" align="center"><xsl:value-of select="."/></td>
          </xsl:when>
          <xsl:otherwise>
	    <td align="right">
              <xsl:choose>
                <xsl:when test="$wert &lt; 0">
      	          <xsl:value-of select="."/>
		  <xsl:if test="$breite != 0">
		    &#160;<img src="{@datei}" width="{$breite}" height="20" alt=""/>
		  </xsl:if>
                </xsl:when>
                <xsl:otherwise>
		  &#160;
                </xsl:otherwise>
              </xsl:choose>
	    </td>
	    <td align="left">
              <xsl:choose>
                <xsl:when test="not($wert &lt; 0)">
		  <xsl:if test="$breite != 0">
		    <img src="{@datei}" width="{$breite}" height="20" alt=""/>&#160;
		  </xsl:if>
      	          <xsl:value-of select="."/>
                </xsl:when>
                <xsl:otherwise>
		  &#160;
                </xsl:otherwise>
              </xsl:choose>
	    </td>	    
          </xsl:otherwise>
        </xsl:choose>
      </xsl:when>
      <xsl:otherwise>
        <xsl:choose>
          <xsl:when test="@datei">
            <td>
              <xsl:if test="$breite != 0">
		<img src="{@datei}" width="{$breite}" height="20" alt=""/>
              </xsl:if>
	    <xsl:value-of select="."/></td>
          </xsl:when>
          <xsl:otherwise>
            <td align="right"><xsl:value-of select="."/></td>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="km">
    <xsl:call-template name="graFeld"/>
  </xsl:template>

  <xsl:template match="rudkm">
    <xsl:call-template name="graFeld"/>
  </xsl:template>

  <xsl:template match="stmkm">
    <xsl:call-template name="graFeld"/>
  </xsl:template>

  <xsl:template match="fahrten">
    <xsl:call-template name="graFeld"/>
  </xsl:template>

  <xsl:template match="kmfahrt">
    <xsl:call-template name="graFeld"/>
  </xsl:template>

  <xsl:template match="dauer">
    <xsl:call-template name="graFeld"/>
  </xsl:template>

  <xsl:template match="kmh">
    <xsl:call-template name="graFeld"/>
  </xsl:template>

  <xsl:template match="anzversch">
    <td><xsl:value-of select="."/></td>
  </xsl:template>

  <xsl:template match="wafakm">
    <td><xsl:value-of select="."/></td>
  </xsl:template>

  <xsl:template match="zielfahrten">
    <td><xsl:value-of select="."/></td>
  </xsl:template>

  <xsl:template match="zusatzDRV">
    <td><xsl:value-of select="."/></td>
  </xsl:template>

  <xsl:template match="zusatzLRVBSommer">
    <td><xsl:value-of select="."/></td>
  </xsl:template>

  <xsl:template match="zusatzLRVBWinter">
    <td><xsl:value-of select="."/></td>
  </xsl:template>

  <xsl:template match="zusatzLRVBrbWanderWett">
    <td><xsl:value-of select="."/></td>
  </xsl:template>

  <xsl:template match="zusatzLRVBrbFahrtenWett">
    <td><xsl:value-of select="."/></td>
  </xsl:template>

  <xsl:template match="fahrtenbuch">
    <xsl:apply-templates select="fbFeld"/>
  </xsl:template>

  <xsl:template match="wwListe">
    <xsl:apply-templates select="wwFeld"/>
  </xsl:template>

  <xsl:template match="fbFeld">
    <td><xsl:value-of select="."/></td>
  </xsl:template>

  <xsl:template match="wwFeld">
    <xsl:choose>
      <xsl:when test="@selbst='true'">
        <td bgcolor="#ffdddd"><xsl:value-of select="."/></td>
      </xsl:when>
      <xsl:otherwise>
        <td><xsl:value-of select="."/></td>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <xsl:template match="wettZeitraumWarnung">
    <p align="center"><font color="red"><b><xsl:value-of select="."/></b></font></p>
  </xsl:template>

  <xsl:template match="wettBedingungen">
    <table align="center" bgcolor="#eeeeee"><tr><td>
    <xsl:apply-templates select="wettBedZeile"/>
    </td></tr></table>
  </xsl:template>

  <xsl:template match="wettBedZeile">
    <xsl:choose>
      <xsl:when test="@fett"><b><xsl:value-of select="."/></b><br/></xsl:when>
      <xsl:when test="@kursiv"><i><xsl:value-of select="."/></i><br/></xsl:when>
      <xsl:otherwise><xsl:value-of select="."/><br/></xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <!-- Gruppe -->
  <xsl:template match="gruppe">
    <xsl:apply-templates select="gruppenName"/>
    <xsl:apply-templates select="wettEintrag"/>
  </xsl:template>

  <xsl:template match="gruppenName">
    <p><b><xsl:value-of select="gruppenBez"/><xsl:text> </xsl:text><xsl:value-of select="gruppenJahrg"/><xsl:text> </xsl:text><xsl:value-of select="gruppenBed"/></b></p>
  </xsl:template>


  <!-- Eintrag bei Wettbewerbsauswertung -->
  <xsl:template match="wettEintrag">
    <table border="3">
    <xsl:choose>
      <xsl:when test="@erfuellt='true'">
        <tr><td bgcolor="#00ff00"><xsl:value-of select="wettName"/><xsl:apply-templates select="wettJahrgang"/>: <xsl:value-of select="wettKilometer"/> Km<xsl:apply-templates select="wettZusatz"/><xsl:apply-templates select="wettWarnung"/></td></tr>
      </xsl:when>
      <xsl:otherwise>
        <tr><td bgcolor="#ff0000"><xsl:value-of select="wettName"/><xsl:apply-templates select="wettJahrgang"/>: <xsl:value-of select="wettKilometer"/> Km<xsl:apply-templates select="wettZusatz"/><xsl:apply-templates select="wettWarnung"/></td></tr>
      </xsl:otherwise>
    </xsl:choose>
    <xsl:if test="wettDetail">
      <tr><td><table border="1"><xsl:apply-templates select="wettDetail"/></table></td></tr>
    </xsl:if>
    </table>
  </xsl:template>

  <xsl:template match="wettZusatz">
    <xsl:text>; </xsl:text><xsl:value-of select="."/>
  </xsl:template>

  <xsl:template match="wettWarnung">
    <xsl:text>; </xsl:text>
    <font color="yellow"><xsl:value-of select="."/></font>
  </xsl:template>

  <xsl:template match="wettJahrgang">
    <xsl:text> </xsl:text>(<xsl:value-of select="."/>)
  </xsl:template>

  <xsl:template match="wettDetail">
    <tr>
      <xsl:for-each select="wettDetailFeld">
        <td><xsl:value-of select="."/></td>
      </xsl:for-each>
    </tr>
  </xsl:template>



  <!-- Zusatztabelle -->
  <xsl:template match="zusatzTabelle">
    <table align="center" border="3">
      <xsl:for-each select="zusatzTabelleZeile">
        <tr>
          <xsl:for-each select="zusatzTabelleSpalte">
            <td>
              <xsl:value-of select="."/>
            </td>
          </xsl:for-each>
	</tr>
      </xsl:for-each>
    </table><br/>
  </xsl:template>

<!-- =================== Spezialtabelle =================== -->

  <xsl:template match="spezialTabelle">
    <xsl:apply-templates select="subTabelle"/>
  </xsl:template>

  <xsl:template match="subTabelle">
    <table border="3" align="center">
      <xsl:apply-templates select="zeile"/>
    </table>
    <br/>
  </xsl:template>

  <xsl:template match="zeile">
    <tr>
      <xsl:apply-templates select="spalte"/>
    </tr>
  </xsl:template>

  <xsl:template match="spalte">
    <xsl:variable name="_align">
      <xsl:choose>
        <xsl:when test="../@colspan='1'">left</xsl:when>
        <xsl:otherwise>center</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="_bgcolor">
      <xsl:choose>
        <xsl:when test="@color"><xsl:value-of select="@color"/></xsl:when>
        <xsl:otherwise>ffffff</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>

    <xsl:choose>
      <xsl:when test="@bold='true'">
        <td align="{$_align}" colspan="{../@colspan}" bgcolor="#{$_bgcolor}"><b><xsl:value-of select="."/></b></td>
      </xsl:when>
      <xsl:otherwise> 
        <td align="{$_align}" colspan="{../@colspan}" bgcolor="#{$_bgcolor}"><xsl:value-of select="."/></td>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


</xsl:stylesheet>
