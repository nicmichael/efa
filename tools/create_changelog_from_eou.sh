#!/bin/bash
xsltproc  ./eoutransform_de.xslt ./src/eou/eou.xml >./src/changelog_de_alt.html
xsltproc --stringparam langcode "de" ./eoutransformv2.xslt ./src/eou/eou.xml >./src/changelog_de.html
xsltproc --stringparam langcode "en" ./eoutransformv2.xslt ./src/eou/eou.xml >./src/changelog_en.html
xsltproc --stringparam langcode "de" --stringparam refVersion "2.5.0_00"  ./eoutransform_aggregate.xslt ./src/eou/eou.xml >./src/changelog_aggde.html
xsltproc --stringparam langcode "en" --stringparam refVersion "2.5.0_00"  ./eoutransform_aggregate.xslt ./src/eou/eou.xml >./src/changelog_aggen.html
#xsltproc ./eoutransform_en.xslt ./src/eou/eou.xml >./src/changelog_en.html
