<?xml version="1.0" encoding="ISO-8859-1"?>

<!-- Stylesheet zur Ausgabe der Struktur der von efa erstellten XML-Dokumente
     Nicolas Michael, 2002 -->

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="html" encoding="ISO-8859-1"/>


  <!-- Grundgerüst -->
  <xsl:template match="kilometerliste">
    <html>
    <head>
      <title>Struktur der XML-Ausgabe</title>
    </head>
    <body>
    <h1 align="center">Struktur der XML-Ausgabe</h1>
    <ul>
      <xsl:apply-templates/>
    </ul>
    </body>
    </html>
  </xsl:template>


  <!-- Elemente -->  
  <xsl:template match="*">
    <li><b><xsl:value-of select="name()"/></b>
    <xsl:apply-templates select="text()"/>
    <xsl:apply-templates select="attribute::*"/>
      <xsl:if test="*">
        <ul><xsl:apply-templates select="*"/></ul>
      </xsl:if>
    </li>
  </xsl:template>


  <!-- Text -->
  <xsl:template match="text()">
    <xsl:text> </xsl:text><i><font color="blue"><xsl:value-of select="."/></font></i>
  </xsl:template>


  <!-- Attribute -->    
  <xsl:template match="attribute::*">
    <xsl:text> </xsl:text><br/><xsl:value-of select="name()"/>=<i><font color="red"><xsl:value-of select="."/></font></i>
  </xsl:template>


</xsl:stylesheet>
