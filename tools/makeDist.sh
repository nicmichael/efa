#!/bin/bash
# makeDist.sh   V2.0 04.10.2025
# makeDist.sh   V2.1 09.11.2025 updated jars for json and flatlaf.
# ######################################
#
# Originally created by Nicolas Michael. Updated by Stefan Gebers.
#
# This script needs some libraries:
# - javahelp 2.0 
#   Outdated, cannot be obtained from official sources any more. But efa uses this help format for the initial help file, and it has not yet been removed.
#   should be placed in the same directory as makedist.sh  ./jh2.0/*
#
# - perl for creating International support keys from the efa_de.properties file
#   this is broken and has been commented out.
#
# - xsltproc to create changelog from eou.xml
#
# ######################################
# How to run
# makeDist.sh 2.4.1#19 2.4.1_19      (creates a distro for efa 2.4.1 beta 19, the beta numbers are behind a #)
# 
# makeDist.sh 2drv 2.4.1_19          (creates a distro for DRV-Deutscher Ruder Verband which includes additional source not included in the main distro)
#                                    It includes some programs which are only neccessary for the headquaters of DRV.
#
#
# ######################################
# 
# makedist.sh directory layout
#
# ./jh2.0     		javahelp 2.0 library
# ./src			the git repository with the source code, subdirectories by example
# ./src/.git		git common part
# ./src/META-INF
# ./src/cfg
# ./src/de
# ./src/eou
# ./src/fmt
# ./src/help
# ./src/plugins
# ./src/root
# ./src/tools
#
# automatically created:
# ./dist		directory where the output of the makedist.sh script is stored: a .tgz and a .zip file
# ./makedist		temporary folder with efa dist files, which get zipped in ./dist/*.tgz and ./dist/*.zip
# ./versions		any run of makedist.sh creates a new directory here, storing both the source from ./src AND the resulting zip file of the distribution
# ./winmedia		files where the (outdated) windows installer may take the dist files from
#
#

if [ $# -lt 1 ] ; then
  echo "usage: $0 <version> [versionid]"
  echo "       e.g. version: '200'"
  echo "       e.g. version: '2_beta'"
  echo "       e.g. version: '2drv'"
  echo "       e.g. versionid: '2.0.0_03'"
  exit 1
fi
VERSION=${1:?}
VERSIONID=$2

DRV=0
if [ "$VERSION" = "2drv" ] ; then
  DRV=1
fi

#MAIN Programs
JDK=/usr/lib/jvm/java-17-openjdk-amd64/
JHINDEXER=/home/efa/efadev/jh2.0/javahelp/bin/jhindexer
JAVA=$JDK/bin/java
JAVAC=$JDK/bin/javac
JAR=$JDK/bin/jar
TARGET=1.8

#EFA SOURCE AND OTHER RESOURCES
EFABASE=/home/efa/efadev
EFASRC=$EFABASE/src
EFADOC=$EFABASE/src/help/main
EFACFG=$EFABASE/src/cfg
EFAFMT=$EFABASE/src/fmt
EFAHELP=$EFABASE/src/help
EFAROOT=$EFABASE/src/root
PLUGINS=$EFABASE/src/plugins
EFAVERSIONS=$EFABASE/versions
MAKEDIST=$EFABASE/makedist
MAKEDIST_JAVA=$MAKEDIST/program
MAKEDIST_TOOLS=$MAKEDIST_JAVA/tools
MAKEDIST_PLUGINS=$MAKEDIST/program/plugins
MAKEDIST_HELP=$MAKEDIST_JAVA/help
MAKEDIST_DOC=$MAKEDIST/doc
MAKEDIST_CFG=$MAKEDIST/cfg
MAKEDIST_FMT=$MAKEDIST/fmt
DIST=$EFABASE/dist
SRCBACKUP=/home/efa/Backup

#WHERE TO PUT EFA2 AFTER BUILD FOR LATER WINDOWS SETUP
EFAWINDSETUP=$EFABASE/winmedia

CLASSPATH=$MAKEDIST_JAVA:$MAKEDIST_TOOLS
CLASSPATH=$CLASSPATH:$PLUGINS/ftp/edtftpj.jar:$CLASSPATH:$PLUGINS/ftp/jsch-0.1.55.jar:$PLUGINS/help/jh.jar:$PLUGINS/jsuntimes/jsuntimes.jar:$PLUGINS/mail/javax.mail.jar:$PLUGINS/mail/activation.jar:$PLUGINS/flatlaf/flatlaf-3.6.jar
CLASSPATH=$CLASSPATH:$PLUGINS/pdf/avalon-framework.jar:$PLUGINS/pdf/batik-all.jar:$PLUGINS/pdf/commons-io.jar:$PLUGINS/pdf/commons-logging.jar:$PLUGINS/pdf/fop.jar:$PLUGINS/pdf/xmlgraphics-commons.jar
CLASSPATH=$CLASSPATH:$PLUGINS/weather/commons-codec.jar:$PLUGINS/weather/signpost-core.jar
CLASSPATH=$CLASSPATH:$PLUGINS/json/json-20250517.jar

if [ -d ${MAKEDIST:?} ] ; then
  rm -fR ${MAKEDIST:?}
fi
mkdir -p ${MAKEDIST:?}

#OBTAIN SOURCE FROM LOCAL GIT

#BACKUP SOURCE FILES
echo "Backup of Source Files ..."
echo "------------------------------------------------------------------"
cd $EFASRC
zip -r $SRCBACKUP/efasrc_`date +%Y%m%d%H%M%S`.zip .

#COPY SOURCE FILES AND CREATE DIRECTORIES IF NECCESSARY
echo "Copying Source Files ..."
echo "------------------------------------------------------------------"
mkdir -p ${MAKEDIST_JAVA:?}
cd $EFASRC
# only include efaDRV java files and classes, if we are creating a dist for DRV
# (Deutscher Ruder Verband). This dist contains some extra tools only suitable and only provided for DRV.
if [ $DRV -eq 0 ] ; then
  find . -name '*.java' | grep -v '\.git' | grep -v '/drv/' > ${MAKEDIST:?}/filelist.tmp
else
  find . -name '*.java' | grep -v '\.git' > ${MAKEDIST:?}/filelist.tmp
fi
rm -f ${MAKEDIST:?}/dirlist.tmp
for f in `cat  ${MAKEDIST:?}/filelist.tmp`
do
  DIR=`dirname $f`
  if [ ! -d ${MAKEDIST_JAVA:?}/$DIR ] ; then
    echo "${MAKEDIST_JAVA:?}/$DIR" >> ${MAKEDIST:?}/dirlist.tmp
  fi
  mkdir -p ${MAKEDIST_JAVA:?}/$DIR
  cp $f ${MAKEDIST_JAVA:?}/$DIR
done


#COMPILE JAVA SOURCE
echo "Compiling Classes ..."
echo "------------------------------------------------------------------"
cd ${MAKEDIST_JAVA:?}
rm ~/compile.log
for f in `cat  ${MAKEDIST:?}/dirlist.tmp`
do
  $JAVAC -target $TARGET -source $TARGET -classpath $CLASSPATH $f/*.java >>~/compile.log 2>&1 || exit 1
  rm $f/*.java
done
rm -f ${MAKEDIST:?}/dirlist.tmp


echo "Copying other Resources ..."
echo "------------------------------------------------------------------"
cd $EFASRC
rm -f ${MAKEDIST:?}/filelist.tmp
find de -name '*.gif' | grep -v '\.git' >> ${MAKEDIST:?}/filelist.tmp
find de -name '*.png' | grep -v '\.git' >> ${MAKEDIST:?}/filelist.tmp
find de -name '*.txt' | grep -v '\.git' >> ${MAKEDIST:?}/filelist.tmp
find de -name '*.css' | grep -v '\.git' >> ${MAKEDIST:?}/filelist.tmp
find de -name '*.xml' | grep -v '\.git' >> ${MAKEDIST:?}/filelist.tmp
find de -name '*.xsl' | grep -v '\.git' >> ${MAKEDIST:?}/filelist.tmp
find doc | grep -v '\.git' | grep -v "onlinehilfe.txt" | grep -v "tour.txt" >> ${MAKEDIST:?}/filelist.tmp
find . -name '*.properties' | grep -v '\.git' >> ${MAKEDIST:?}/filelist.tmp
for f in `cat  ${MAKEDIST:?}/filelist.tmp`
do
  DIR=`dirname $f`
  mkdir -p ${MAKEDIST_JAVA:?}/$DIR
  cp $f ${MAKEDIST_JAVA:?}/$DIR
done

echo "Making JAR File ..."
echo "------------------------------------------------------------------"
cd ${MAKEDIST_JAVA:?}
find de -name *.java -delete
$JAR cfv efa.jar de || exit 1

echo "Updating efa_de.properties ..."
echo "------------------------------------------------------------------"

# the make_i18n_keys.sh does not work anymore due to need for outdated perl libraries, so it is not run any more
#cd ${EFASRC:?}
#./tools/make_i18n_keys.sh -ur ${MAKEDIST_JAVA:?}/efa_de.properties

echo "Making efa.sec File ..."
echo "------------------------------------------------------------------"
cd ${MAKEDIST_JAVA:?}
$JAVA -classpath $CLASSPATH tools.SecFileCreator ${MAKEDIST_JAVA:?} ${MAKEDIST_JAVA:?}/efa.jar || exit 1

echo "Preparing and Copying relevant Plugins ..."
echo "------------------------------------------------------------------"
mkdir -p ${MAKEDIST_PLUGINS:?}
cp ${PLUGINS:?}/mail/javax.mail.jar ${MAKEDIST_PLUGINS:?}/
cp ${PLUGINS:?}/mail/activation.jar ${MAKEDIST_PLUGINS:?}/
cp ${PLUGINS:?}/jsuntimes/jsuntimes.jar ${MAKEDIST_PLUGINS:?}/
cp ${PLUGINS:?}/help/jh.jar ${MAKEDIST_PLUGINS:?}/
cp ${PLUGINS:?}/ftp/edtftpj.jar ${MAKEDIST_PLUGINS:?}/
cp ${PLUGINS:?}/ftp/jsch-0.1.55.jar ${MAKEDIST_PLUGINS:?}/
cp ${PLUGINS:?}/weather/commons-codec.jar ${MAKEDIST_PLUGINS:?}/
cp ${PLUGINS:?}/weather/signpost-core.jar ${MAKEDIST_PLUGINS:?}/
cp ${PLUGINS:?}/flatlaf/flatlaf-*.jar ${MAKEDIST_PLUGINS:?}/
cp ${PLUGINS:?}/json/json*.jar ${MAKEDIST_PLUGINS:?}/

echo "Copying Config Files ..."
echo "------------------------------------------------------------------"
mkdir -p ${MAKEDIST_CFG:?}
cp ${EFACFG:?}/*.cfg ${MAKEDIST_CFG:?}

echo "Creating Documentation ..."
echo "------------------------------------------------------------------"
mkdir -p ${MAKEDIST_DOC:?}
cd ${MAKEDIST_DOC:?}
cp ${EFADOC:?}/*.gif ${MAKEDIST_DOC:?}
cp ${EFADOC:?}/*.png ${MAKEDIST_DOC:?}
cp ${EFADOC:?}/*.html ${MAKEDIST_DOC:?}

# Online help creation need javahelp 2.0 programs in ./jh2.0/...
# should be commented out if javahelp not available
echo "Creating Online Help ..."
echo "------------------------------------------------------------------"
mkdir -p ${MAKEDIST_HELP:?}
cd ${MAKEDIST_HELP:?}
cp -r ${EFAHELP:?}/* .

$JHINDEXER main gui
cd ${MAKEDIST_JAVA:?}
$JAR cfv efahelp.jar help || exit 1

#Addendum SGB: create changelog files
#creates /src/changelog_de.html and changelog_en.html
#requires xsltproc program.
echo "Creating Changelog files..."
echo "------------------------------------------------------------------"
echo ${EFABASE}
echo ${EFASRC}
echo ${MAKEDIST_DOC}

xsltproc  ${EFABASE}/eoutransform_de.xslt ${EFASRC}/eou/eou.xml >${MAKEDIST_DOC:?}/changelog_de.html
xsltproc  ${EFABASE}/eoutransform_en.xslt ${EFASRC}/eou/eou.xml >${MAKEDIST_DOC:?}/changelog_en.html
# Addendum SGB: eou changelog refers do some png images for showing the structure.
cp ${EFASRC}/eou/*.png ${MAKEDIST_DOC:?}

echo "DONE. Check"

#echo "Copying Formatting Files ..."
#mkdir -p ${MAKEDIST_FMT:?}
#cd $EFAFMT
#find . | grep -v '\.git' > ${MAKEDIST:?}/filelist.tmp
#for f in `cat  ${MAKEDIST:?}/filelist.tmp`
#do
#  DIR=`dirname $f`
#  mkdir -p ${MAKEDIST_FMT:?}/$DIR
#  cp $f ${MAKEDIST_FMT:?}/$DIR
#done

echo "Copying Root Files ..."
echo "------------------------------------------------------------------"
cp ${EFAROOT:?}/* ${MAKEDIST:?}
chmod +x ${MAKEDIST:?}/*.sh
if [ $DRV -eq 0 ] ; then
  rm ${MAKEDIST:?}/efaDRV.sh
  rm ${MAKEDIST:?}/efaDRV.bat
fi

echo "Converting DOS Files ..."
echo "------------------------------------------------------------------"
cd ${MAKEDIST:?}
find . -name '*.bat' | grep -v '\.git' > ${MAKEDIST:?}/filelist.tmp
#for f in `cat  ${MAKEDIST:?}/filelist.tmp`
#do
#  ORG=$f
#  BAK=$f.bak
#  mv ${ORG:?} ${BAK:?}
#  $JAVA -classpath $CLASSPATH tools.Unixlf2doslf ${BAK:?} ${ORG:?} || exit 1
#  rm -f ${BAK:?}
#done

echo "Removing Temporary Files ..."
echo "------------------------------------------------------------------"
rm -f ${MAKEDIST:?}/*.tmp
rm -f ${MAKEDIST_JAVA:?}/*.class
rm -rf ${MAKEDIST_JAVA:?}/de
rm -rf ${MAKEDIST_TOOLS:?}
rm -rf ${MAKEDIST_HELP:?}

echo "Touching all Files ..."
echo "------------------------------------------------------------------"
cd ${MAKEDIST:?}
find . -exec touch "{}" \;

echo "Creating Distribution Archives ..."
echo "------------------------------------------------------------------"
mkdir -p ${DIST:?}
cd ${MAKEDIST:?}
rm -f ${DIST:?}/efa${VERSION:?}.zip
rm -f ${DIST:?}/efa${VERSION:?}.tar
zip -r ${DIST:?}/efa${VERSION:?}.zip .
tar cfv ${DIST:?}/efa${VERSION:?}.tar .

if [ "$VERSIONID" != "" ] ; then
  mkdir ${EFAVERSIONS}/${VERSIONID}
  cp ${DIST:?}/efa${VERSION:?}.zip ${EFAVERSIONS}/${VERSIONID}/
  cp -r ${EFASRC:?} ${EFAVERSIONS}/${VERSIONID}/
  cp -r ${MAKEDIST:?}/* ${EFAWINDSETUP:?}
  echo "============================================="
  echo "Run After Windows Build:"
  echo "mv /media/exchange/efa_setup/efa*exe ${EFAVERSIONS}/${VERSIONID}/"
  echo "cp ${EFAVERSIONS}/${VERSIONID}/efa${VERSION}* /home/nick/html/nmichael/1und1/efa/download/"
  echo "============================================="
fi

echo "Done."
cd $EFABASE
ls -l dist
exit 0
