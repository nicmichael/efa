<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- Stylesheet für Ausruck (schwarz/weiß), Nicolas Michael, 2002 -->

<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output method="html" encoding="ISO-8859-1"/>



  <!-- Grundgerüst -->
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
	<xsl:if test="gruppe">
          <table width="100%">
            <xsl:apply-templates select="gruppe"/>
	  </table>
	</xsl:if>
        <xsl:apply-templates select="tabelle"/>
        <xsl:apply-templates select="zusatzTabelle"/>
	<xsl:apply-templates select="spezialTabelle"/>
	<xsl:comment>EFA-ENDE</xsl:comment> <!-- Markierung für "nur Tabelle ersetzen" -->
      </body>
    </html>
  </xsl:template>



  <!-- Tabelle mit allgemeinen Infos zur Auswertung -->
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



  <!-- Tabelle mit eigentlichen Einträgen -->
  <xsl:template match="tabelle">
    <table border="3" align="center">
      <xsl:apply-templates select="tabellenTitel"/>
      <xsl:apply-templates select="eintrag"/>
    </table><br/>
  </xsl:template>

  <xsl:template match="tabellenTitel">
    <tr>
      <xsl:for-each select="spaltenTitel">
        <xsl:choose>
          <xsl:when test="@colspan">
            <th colspan="{@colspan}"><xsl:value-of select="."/></th>
          </xsl:when>
          <xsl:otherwise>
            <th><xsl:value-of select="."/></th>
	  </xsl:otherwise>
        </xsl:choose>
      </xsl:for-each>
    </tr>
  </xsl:template>


  <!-- Ein Eintrag in der Tabelle -->
  <xsl:template match="eintrag">
    <tr>
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
    </tr>
  </xsl:template>

  <xsl:template match="nr">
    <td><xsl:value-of select="."/></td>
  </xsl:template>

  <xsl:template match="name">
    <td><xsl:value-of select="."/></td>
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
		  <xsl:if test="@breite != 0">
		    <xsl:apply-templates select="@datei"/>
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
		  <xsl:if test="@breite != 0">
		    <xsl:apply-templates select="@datei"/>
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
            <td><xsl:apply-templates select="@datei"/><xsl:value-of select="."/></td>
          </xsl:when>
          <xsl:otherwise>
            <td align="right"><xsl:value-of select="."/></td>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="@datei">
    <xsl:variable name="breite">
      <xsl:choose>
        <xsl:when test="../@breite >= 0">
	  <xsl:value-of select="../@breite"/>
	</xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="../@breite * -1"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
	
    <xsl:choose>
      <xsl:when test="contains(../@datei,'big.gif')">
        <xsl:if test="$breite != 0">
          <img src="graubig.gif" width="{$breite}" height="20" alt=""/>
        </xsl:if>
      </xsl:when>
      <xsl:otherwise>
        <xsl:if test="$breite != 0">
          <img src="grau.gif" width="{$breite}" height="20" alt=""/>
        </xsl:if>
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
    <xsl:for-each select="fbFeld">
      <td><xsl:value-of select="."/></td>
    </xsl:for-each>
  </xsl:template>


  <!-- Liste für Wer mit Wem und Wer Wohin -->
  <xsl:template match="wwListe">
    <xsl:for-each select="wwFeld">
      <xsl:choose>
        <xsl:when test="@selbst='true'">
          <td bgcolor="#ffdddd"><xsl:value-of select="."/></td>
        </xsl:when>
        <xsl:otherwise>
          <td><xsl:value-of select="."/></td>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:for-each>
  </xsl:template>



  <!-- Templates für Wettbewerbe -->

  <!-- Warnung bei ungültigem Zeitraum -->
  <xsl:template match="wettZeitraumWarnung">
    <p align="center"><font color="red"><b><xsl:value-of select="."/></b></font></p>
  </xsl:template>


  <!-- Wettbewerbsbedingungen -->
  <xsl:template match="wettBedingungen">
    <table align="center" bgcolor="#eeeeee">
      <tr><td>
        <xsl:for-each select="wettBedZeile">
          <xsl:choose>
            <xsl:when test="@fett"><b><xsl:value-of select="."/></b><br/></xsl:when>
            <xsl:when test="@kursiv"><i><xsl:value-of select="."/></i><br/></xsl:when>
            <xsl:otherwise><xsl:value-of select="."/><br/></xsl:otherwise>
          </xsl:choose>
        </xsl:for-each>
      </td></tr>
    </table><br/>
  </xsl:template>





  <!-- Gruppe -->
  <xsl:template match="gruppe">
    <xsl:apply-templates select="gruppenName"/>
    <xsl:apply-templates select="wettEintrag"/>
  </xsl:template>

  <xsl:template match="gruppenName">
    <tr><th colspan="2"> </th></tr>
    <tr><th align="left" colspan="2">
    <xsl:value-of select="gruppenBez"/><xsl:text> </xsl:text><xsl:value-of select="gruppenJahrg"/><xsl:text> </xsl:text>(<i>gefordert: <xsl:value-of select="gruppenBed"/></i>)
    </th></tr>
  </xsl:template>


  <!-- Eintrag bei Wettbewerbsauswertung -->
  <xsl:template match="wettEintrag">
    <tr>
    <td width="10%"> </td>
    <xsl:choose>
      <xsl:when test="wettDetail">
        <td><table border="1">
	  <tr><td colspan="6"><b><xsl:value-of select="wettName"/></b><xsl:apply-templates select="wettJahrgang"/>: <xsl:value-of select="wettKilometer"/> Km<xsl:apply-templates select="wettZusatz"/></td></tr>
	  <xsl:apply-templates select="wettDetail"/>
	</table></td>
      </xsl:when>
      <xsl:otherwise> <!-- keine Details, oder nicht erfüllt -->
        <xsl:choose>
          <xsl:when test="@erfuellt='true'">
            <td>erfüllt: <b><xsl:value-of select="wettName"/></b><xsl:apply-templates select="wettJahrgang"/>: <xsl:value-of select="wettKilometer"/> Km<xsl:apply-templates select="wettZusatz"/></td>
          </xsl:when>
          <xsl:otherwise> <!-- nicht erfüllt -->
            <td>
	      noch nicht erfüllt: <b><xsl:value-of select="wettName"/></b>
	      <xsl:apply-templates select="wettJahrgang"/>: 
	      <xsl:choose>
	        <xsl:when test="wettKilometer &lt; ../gruppenName/gruppenBed/@wert1"><i><xsl:value-of select="wettKilometer"/> Km</i></xsl:when>
	        <xsl:otherwise><xsl:value-of select="wettKilometer"/> Km</xsl:otherwise>	    
	      </xsl:choose>
	      <xsl:apply-templates select="wettZusatz"/>
	      <xsl:apply-templates select="wettWarnung"/>
	    </td>
          </xsl:otherwise>
        </xsl:choose>
      </xsl:otherwise>
    </xsl:choose>
    </tr>
  </xsl:template>

  <xsl:template match="wettZusatz">
    <xsl:text>; </xsl:text>
    <xsl:choose>
      <xsl:when test=". &lt; ../../gruppenName/gruppenBed/@wert2"><i><xsl:value-of select="."/></i></xsl:when>
      <xsl:otherwise><xsl:value-of select="."/></xsl:otherwise>	    
    </xsl:choose>
  </xsl:template>

  <xsl:template match="wettWarnung">
    <xsl:text>; </xsl:text>
    <xsl:value-of select="."/>
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
        <xsl:when test="@color='ffffff'">ffffff</xsl:when>
        <xsl:otherwise>cccccc</xsl:otherwise>
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
