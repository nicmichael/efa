<?xml version='1.0' encoding='ISO-8859-1' ?>
<!DOCTYPE helpset
  PUBLIC "-//Sun Microsystems Inc.//DTD JavaHelp HelpSet Version 2.0//EN"
         "http://java.sun.com/products/javahelp/helpset_2_0.dtd">

<helpset version="2.0">

  <!-- title -->
  <title>efa - elektronisches Fahrtenbuch: Hilfe</title>

  <!-- maps -->
  <maps>
     <homeID>top</homeID>
     <mapref location="efaHelpMap_de.jhm"/>
  </maps>

  <!-- views -->
  <view>
    <name>TOC</name>
    <label>Inhalt</label>
    <type>javax.help.TOCView</type>
    <data>efaHelpTOC_de.xml</data>
  </view>

  <view>
    <name>Index</name>
    <label>Index</label>
    <type>javax.help.IndexView</type>
    <data>efaHelpIndex.xml</data>
  </view>

  <view>
    <name>Search</name>
    <label>Suche</label>
    <type>javax.help.SearchView</type>
    <data engine="com.sun.java.help.search.DefaultSearchEngine">
      JavaHelpSearch
    </data>
  </view>

  <presentation default="true" displayviewimages="false">
     <name>main</name>
     <size width="700" height="500" />
     <location x="50" y="50" />
     <title>efa - elektronisches Fahrtenbuch: Hilfe</title>
     <image>toplevelfolder</image>
     <toolbar>
        <helpaction>javax.help.BackAction</helpaction>
        <helpaction>javax.help.ForwardAction</helpaction>
        <helpaction>javax.help.SeparatorAction</helpaction>
        <helpaction>javax.help.HomeAction</helpaction>
        <helpaction>javax.help.ReloadAction</helpaction>
        <helpaction>javax.help.SeparatorAction</helpaction>
     </toolbar>
  </presentation>

</helpset>
