<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:fo="http://www.w3.org/1999/XSL/Format">

  <!--
    ============================================================================
    PDF Style Sheet
    ============================================================================
  -->

  <!-- ########## General Settings - Begin ##########                        -->
  <xsl:variable name="PageWidth">210</xsl:variable>           <!-- Page Width in mm -->
  <xsl:variable name="PageHeight">297</xsl:variable>          <!-- Page Height in mm -->
  <xsl:variable name="PageMargin">10</xsl:variable>           <!-- Page Margin in mm -->
  <xsl:variable name="FontSize">10</xsl:variable>             <!-- Font Size in pt -->
  <xsl:variable name="DocumentTitle"></xsl:variable>          <!-- Override default Title -->
  <xsl:variable name="RowColorHeader">#cccccc</xsl:variable>  <!-- Color for Row Header -->
  <xsl:variable name="RowColor1">#f5f5f5</xsl:variable>       <!-- Color for odd Rows -->
  <xsl:variable name="RowColor2">#eaeaea</xsl:variable>       <!-- Color for even Rows -->
  <!-- ########## General Settings - End ##########                          -->

  <!-- Master Template -->
  <xsl:template match="Statistic">
    <fo:root>

      <!-- Page Layout -->
      <fo:layout-master-set>

        <!-- Title Page -->
        <fo:simple-page-master master-name="TitlePage" page-width="{$PageWidth}mm" page-height="{$PageHeight}mm">
          <fo:region-body margin-top="{$PageMargin}mm" margin-bottom="{$PageMargin}mm" margin-left="{$PageMargin}mm" margin-right="{$PageMargin}mm" />
        </fo:simple-page-master>

        <!-- Content Pages -->
        <fo:simple-page-master master-name="ContentPage" page-width="{$PageWidth}mm" page-height="{$PageHeight}mm">
          <fo:region-body margin-top="{$PageMargin}mm" margin-bottom="{$PageMargin}mm" margin-left="{$PageMargin}mm" margin-right="{$PageMargin}mm" />
          <fo:region-before extent="{$PageMargin}mm" />
          <fo:region-after extent="{$PageMargin}mm" />
        </fo:simple-page-master>

      </fo:layout-master-set>


      <!-- Content of Title Page -->
      <fo:page-sequence master-reference="TitlePage" force-page-count="no-force">
        <xsl:apply-templates select="Header"/>
      </fo:page-sequence>


      <!-- Content of Content Pages -->
      <fo:page-sequence master-reference="ContentPage" initial-page-number="1">

        <!-- Page Header -->
        <fo:static-content flow-name="xsl-region-before" font-size="{$FontSize}pt" font-family="Helvetica">
	  <fo:list-block font-size="0.8em" margin-top="{$PageMargin div 2}mm" start-indent="{$PageMargin}mm" end-indent="{$PageMargin}mm">
	    <fo:list-item>
	      <fo:list-item-label><fo:block>
	        <xsl:choose>
	          <xsl:when test="$DocumentTitle=''">
   	            <xsl:value-of select="Header/Title"/>
	          </xsl:when>
	          <xsl:otherwise>
	            <xsl:value-of select="$DocumentTitle"/>
	          </xsl:otherwise>
    	        </xsl:choose>
	      </fo:block></fo:list-item-label>
	      <fo:list-item-body><fo:block text-align="right"><xsl:value-of select="Header/ProgramName"/></fo:block></fo:list-item-body>
	    </fo:list-item>
	  </fo:list-block>
	</fo:static-content>

        <!-- Page Footer -->
        <fo:static-content flow-name="xsl-region-after" font-size="{$FontSize}pt" font-family="Helvetica">
	  <fo:list-block font-size="0.8em" margin-bottom="{$PageMargin div 3}mm" start-indent="{$PageMargin}mm" end-indent="{$PageMargin}mm">
	    <fo:list-item>
	      <fo:list-item-label><fo:block><xsl:value-of select="Header/Date"/></fo:block></fo:list-item-label>
	      <fo:list-item-body><fo:block text-align="right">Seite <fo:page-number/></fo:block></fo:list-item-body>
	    </fo:list-item>
	  </fo:list-block>
	</fo:static-content>

        <!-- Page Body -->
        <fo:flow flow-name="xsl-region-body" font-size="{$FontSize}pt" font-family="Helvetica">
          <xsl:apply-templates select="Data"/>
          <xsl:apply-templates select="Logbook"/>
          <xsl:apply-templates select="Competition"/>
        </fo:flow>

      </fo:page-sequence>

   </fo:root>
</xsl:template>


  <!-- Header on Header Page -->
  <xsl:template match="Header">
    <fo:flow flow-name="xsl-region-body" font-size="12pt" font-family="Helvetica">
      <fo:block font-size="24pt" text-align="center" margin-top="1cm" margin-bottom="3cm">
	        <xsl:choose>
	          <xsl:when test="$DocumentTitle=''">
   	            <xsl:value-of select="Title"/>
	          </xsl:when>
	          <xsl:otherwise>
	            <xsl:value-of select="$DocumentTitle"/>
	          </xsl:otherwise>
    	        </xsl:choose>
      </fo:block>
      <fo:list-block font-size="12pt" margin-top="1cm" display-align="center">
            <fo:list-item>
              <fo:list-item-label><fo:block><xsl:value-of select="Date/@description"/>:</fo:block></fo:list-item-label>
              <fo:list-item-body start-indent="50mm"><fo:block><xsl:value-of select="Date"/></fo:block></fo:list-item-body>
	    </fo:list-item>
            <fo:list-item>
              <fo:list-item-label><fo:block><xsl:value-of select="ProgramName/@description"/>:</fo:block></fo:list-item-label>
              <fo:list-item-body start-indent="50mm"><fo:block><xsl:value-of select="ProgramName"/></fo:block></fo:list-item-body>
            </fo:list-item>
            <fo:list-item>
              <fo:list-item-label><fo:block><xsl:value-of select="Description/@description"/>:</fo:block></fo:list-item-label>
              <fo:list-item-body start-indent="50mm"><fo:block><xsl:value-of select="Description"/></fo:block></fo:list-item-body>
            </fo:list-item>
            <fo:list-item>
              <fo:list-item-label><fo:block><xsl:value-of select="DateRange/@description"/>:</fo:block></fo:list-item-label>
              <fo:list-item-body start-indent="50mm"><fo:block><xsl:value-of select="DateRange"/></fo:block></fo:list-item-body>
            </fo:list-item>
            <fo:list-item>
              <fo:list-item-label><fo:block><xsl:value-of select="ConsideredEntries/@description"/>:</fo:block></fo:list-item-label>
              <fo:list-item-body start-indent="50mm"><fo:block><xsl:apply-templates select="ConsideredEntries"/></fo:block></fo:list-item-body>
            </fo:list-item>
            <fo:list-item>
              <fo:list-item-label><fo:block><xsl:value-of select="Filter/@description"/>:</fo:block></fo:list-item-label>
              <fo:list-item-body start-indent="50mm"><fo:block><xsl:apply-templates select="Filter"/></fo:block></fo:list-item-body>
            </fo:list-item>
            <xsl:apply-templates select="Ignored"/>
      </fo:list-block>
      <fo:block font-size="14pt" text-align="center" margin-top="2cm">erstellt von <xsl:value-of select="ProgramName"/>
      </fo:block>
    </fo:flow>
  </xsl:template>

  <xsl:template match="Ignored">
    <fo:list-item>
      <fo:list-item-label><fo:block></fo:block></fo:list-item-label>
      <fo:list-item-body><fo:block><xsl:apply-templates select="@description"/></fo:block></fo:list-item-body>
    </fo:list-item>
  </xsl:template>

  <!-- Data Table on Content Page -->
  <xsl:template match="Data">
    <fo:table>
      <xsl:for-each select="Columns/Column">
        <fo:table-column column-width="{ ($PageWidth - 2 * $PageMargin) div count(../Column) }mm"/>
      </xsl:for-each>

      <fo:table-body>
        <fo:table-row background-color="{$RowColorHeader}">
          <xsl:for-each select="Columns/Column">
            <fo:table-cell padding-right="6pt">
	      <fo:block text-align="center" font-weight="bold"><xsl:value-of select="."/></fo:block>
	    </fo:table-cell>
          </xsl:for-each>
        </fo:table-row>
        <xsl:for-each select="Item">
	  <xsl:variable name="pos">
	    <xsl:value-of select="@index"/>
	  </xsl:variable>
	  <xsl:variable name="summary">
	    <xsl:value-of select="@summary"/>
	  </xsl:variable>

	  <xsl:variable name="color">
	    <xsl:choose>
  	      <xsl:when test="$pos mod 2 = 0"><xsl:value-of select="$RowColor1"/></xsl:when>
	      <xsl:when test="$pos mod 2 = 1"><xsl:value-of select="$RowColor2"/></xsl:when>
              <xsl:when test="$summary='true'"><xsl:value-of select="$RowColorHeader"/></xsl:when>
	      <xsl:otherwise><xsl:value-of select="$RowColor1"/></xsl:otherwise>
            </xsl:choose>
          </xsl:variable>

	  <fo:table-row background-color="{$color}">
            <xsl:apply-templates select="Position"/>
            <xsl:apply-templates select="Name"/>
            <xsl:apply-templates select="Gender"/>
            <xsl:apply-templates select="Status"/>
            <xsl:apply-templates select="YearOfBirth"/>
            <xsl:apply-templates select="MemberNo"/>
            <xsl:apply-templates select="BoatType"/>
            <xsl:apply-templates select="Distance"/>
            <xsl:apply-templates select="RowDistance"/>
            <xsl:apply-templates select="CoxDistance"/>
            <xsl:apply-templates select="Sessions"/>
            <xsl:apply-templates select="AvgDistance"/>
            <xsl:apply-templates select="Duration"/>
            <xsl:apply-templates select="Speed"/>
            <xsl:apply-templates select="DestinationAreas"/>
            <xsl:apply-templates select="WanderfahrtKm"/>
            <xsl:apply-templates select="DamageCount"/>
            <xsl:apply-templates select="DamageDuration"/>
            <xsl:apply-templates select="DamageAvgDuration"/>
            <xsl:apply-templates select="Clubwork"/>
            <xsl:apply-templates select="ClubworkRelTarget"/>
            <xsl:apply-templates select="ClubworkCarryOver"/>
            <xsl:apply-templates select="ClubworkCredit"/>
            <xsl:apply-templates select="MatrixColumn"/>
	  </fo:table-row>
        </xsl:for-each>
      </fo:table-body>
    </fo:table>
  </xsl:template>


  <!-- Logbook Table on Content Page -->
  <xsl:template match="Logbook">
    <fo:table>
      <xsl:for-each select="Columns/Column">
        <fo:table-column column-width="{ ($PageWidth - 2 * $PageMargin) div count(../Column) }mm"/>
      </xsl:for-each>

      <fo:table-body>
        <fo:table-row background-color="{$RowColorHeader}">
          <xsl:for-each select="Columns/Column">
            <fo:table-cell padding-right="6pt">
	      <fo:block text-align="center" font-weight="bold"><xsl:value-of select="."/></fo:block>
	    </fo:table-cell>
          </xsl:for-each>
        </fo:table-row>
        <xsl:for-each select="Record">
	  <xsl:variable name="pos">
	    <xsl:value-of select="@index"/>
	  </xsl:variable>

	  <xsl:variable name="color">
	    <xsl:choose>
  	      <xsl:when test="$pos mod 2 = 0"><xsl:value-of select="$RowColor1"/></xsl:when>
	      <xsl:when test="$pos mod 2 = 1"><xsl:value-of select="$RowColor2"/></xsl:when>
	      <xsl:otherwise><xsl:value-of select="$RowColorHeader"/></xsl:otherwise>
            </xsl:choose>
          </xsl:variable>

	  <fo:table-row background-color="{$color}">
            <xsl:apply-templates select="EntryNo"/>
            <xsl:apply-templates select="Date"/>
            <xsl:apply-templates select="EndDate"/>
            <xsl:apply-templates select="Boat"/>
            <xsl:apply-templates select="Cox"/>
            <xsl:apply-templates select="Crew"/>
            <xsl:apply-templates select="StartTime"/>
            <xsl:apply-templates select="EndTime"/>
            <xsl:apply-templates select="Waters"/>
            <xsl:apply-templates select="Destination"/>
            <xsl:apply-templates select="DestinationDetails"/>
            <xsl:apply-templates select="DestAreas"/>
            <xsl:apply-templates select="Distance"/>
            <xsl:apply-templates select="MultiDay"/>
            <xsl:apply-templates select="SessionType"/>
            <xsl:apply-templates select="Notes"/>
	  </fo:table-row>
        </xsl:for-each>
      </fo:table-body>
    </fo:table>
  </xsl:template>


  <!-- Fields for Data or Logbook Table -->
  <xsl:template match="Position">
    <fo:table-cell padding-right="6pt"><fo:block text-align="center"><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="Name">
    <fo:table-cell padding-right="6pt"><fo:block><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="Gender">
    <fo:table-cell padding-right="6pt"><fo:block><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="Status">
    <fo:table-cell padding-right="6pt"><fo:block><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="YearOfBirth">
    <fo:table-cell padding-right="6pt"><fo:block><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="MemberNo">
    <fo:table-cell padding-right="6pt"><fo:block><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="BoatType">
    <fo:table-cell padding-right="6pt"><fo:block><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="Distance">
    <fo:table-cell padding-right="6pt"><fo:block text-align="right"><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="RowDistance">
    <fo:table-cell padding-right="6pt"><fo:block text-align="right"><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="CoxDistance">
    <fo:table-cell padding-right="6pt"><fo:block text-align="right"><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="Sessions">
    <fo:table-cell padding-right="6pt"><fo:block text-align="right"><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="AvgDistance">
    <fo:table-cell padding-right="6pt"><fo:block text-align="right"><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="Duration">
    <fo:table-cell padding-right="6pt"><fo:block text-align="right"><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="Speed">
    <fo:table-cell padding-right="6pt"><fo:block text-align="right"><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="DestinationAreas">
    <fo:table-cell padding-right="6pt"><fo:block><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="WanderfahrtKm">
    <fo:table-cell padding-right="6pt"><fo:block text-align="right"><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="DamageCount">
    <fo:table-cell padding-right="6pt"><fo:block text-align="right"><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="DamageDuration">
    <fo:table-cell padding-right="6pt"><fo:block text-align="right"><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="DamageAvgDuration">
    <fo:table-cell padding-right="6pt"><fo:block text-align="right"><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="Clubwork">
    <fo:table-cell padding-right="6pt"><fo:block text-align="right"><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="ClubworkRelTarget">
    <fo:table-cell padding-right="6pt"><fo:block text-align="right"><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="ClubworkCarryOver">
    <fo:table-cell padding-right="6pt"><fo:block text-align="right"><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="ClubworkCredit">
    <fo:table-cell padding-right="6pt"><fo:block text-align="right"><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="MatrixColumn">
    <fo:table-cell padding-right="6pt"><fo:block text-align="right"><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="EntryNo">
    <fo:table-cell padding-right="6pt"><fo:block text-align="center"><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="Date">
    <fo:table-cell padding-right="6pt"><fo:block><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="EndDate">
    <fo:table-cell padding-right="6pt"><fo:block><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="Boat">
    <fo:table-cell padding-right="6pt"><fo:block><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="Cox">
    <fo:table-cell padding-right="6pt"><fo:block><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="Crew">
    <fo:table-cell padding-right="6pt"><fo:block><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="StartTime">
    <fo:table-cell padding-right="6pt"><fo:block><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="EndTime">
    <fo:table-cell padding-right="6pt"><fo:block><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="Waters">
    <fo:table-cell padding-right="6pt"><fo:block><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="Destination">
    <fo:table-cell padding-right="6pt"><fo:block><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="DestinationDetails">
    <fo:table-cell padding-right="6pt"><fo:block><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="DestAreas">
    <fo:table-cell padding-right="6pt"><fo:block><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="MultiDay">
    <fo:table-cell padding-right="6pt"><fo:block><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="SessionType">
    <fo:table-cell padding-right="6pt"><fo:block><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>

  <xsl:template match="Notes">
    <fo:table-cell padding-right="6pt"><fo:block><xsl:value-of select="."/></fo:block></fo:table-cell>
  </xsl:template>


  <!-- Competition on Content Page -->
  <xsl:template match="Competition">
    <fo:block font-size="14pt" text-align="center" margin-top="5cm"
                border="3pt #000000 solid" padding-right="10mm" border-color="#ff0000">
                Sorry, this Statistic Type is currently not supported in PDF Output.
    </fo:block>
  </xsl:template>


</xsl:stylesheet>