#!/bin/bash
xsltproc  ./eoutransform_de.xslt ./src/eou/eou.xml >./src/changelog_de.html
xsltproc  ./eoutransform_en.xslt ./src/eou/eou.xml >./src/changelog_en.html
