#!/bin/sh
 
# ##########################################
# Check for updated JAR files and install  #
# ##########################################
check_online_update()
{
  for fnew in `find program -name '*.jar.new'`
  do
    echo "[`date +%Y-%m-%d_%H:%M:%S` $PROG] found updated JAR file: $fnew"
    forg=`echo "$fnew" | sed "s/.jar.new/.jar/"`
    echo "[`date +%Y-%m-%d_%H:%M:%S` $PROG] copying $fnew to $forg"
    cp ${fnew:?} ${forg:?}
    rm ${fnew:?}
  done
}

# ##########################################
# Main                                     #
# ##########################################

# change to efa directory
cd `dirname $0`
PROG=$0

# ##########################################
# Get Arguments                            #
# ##########################################
if [ $# -eq 0 ] ; then
  echo "usage: $PROG <mainclass> [arguments]"
  exit 1
fi
CLASSNAME=$1

# ##########################################
# Classpath                                #
# ##########################################

# efa
CP=program/efa.jar:program/efahelp.jar:program

# OnlineHelp Plugin
CP=$CP:program/plugins/jh.jar

# FTP Plugin
CP=$CP:program/plugins/edtftpj.jar

# SFTP support for FTP Plugin
CP=$CP:program/plugins/jsch-0.1.55.jar

# Mail Plugin
CP=$CP:program/plugins/javax.mail.jar
CP=$CP:program/plugins/activation.jar

# JSUNTIMES Plugin
CP=$CP:program/plugins/jsuntimes.jar

# PDF Plugin
CP=$CP:program/plugins/avalon-framework.jar
CP=$CP:program/plugins/batik-all.jar
CP=$CP:program/plugins/commons-io.jar
CP=$CP:program/plugins/commons-logging.jar
CP=$CP:program/plugins/fop.jar
CP=$CP:program/plugins/xmlgraphics-commons.jar

# EFA Flat Laf
CP=$CP:program/plugins/flatlaf-3.6.jar

# JSON
CP=$CP:program/plugins/json-20250517.jar

# Weather Plugin
CP=$CP:program/plugins/commons-codec.jar
CP=$CP:program/plugins/signpost-core.jar

# ##########################################
# JVM Settings                             #
# ##########################################

# Java Heap
# Include File expected in efa installation directory
if [ -f java.heap ] ; then
  . ./java.heap
fi
if [ "$EFA_JAVA_HEAP" = "" ] ; then
# A higher Java Heaps helps to speed up efa on slower computers
# As garbage collection needs to run at lower frequencies
  EFA_JAVA_HEAP=192m
fi
if [ "$EFA_NEW_SIZE" = "" ] ; then
  EFA_NEW_SIZE=32m
fi

# JVM-Optionen
JVMOPTIONS="-Xmx$EFA_JAVA_HEAP -XX:NewSize=$EFA_NEW_SIZE -XX:MaxNewSize=$EFA_NEW_SIZE"


# ##########################################
# Run Program                              #
# ##########################################

# Java Arguments
EFA_JAVA_ARGUMENTS="$JVMOPTIONS -cp $CP"

# Run Program
if [ $EFA_VERBOSE ] ; then
  echo "[`date +%Y-%m-%d_%H:%M:%S` $PROG] script running ..."
fi
RC=99
while [ $RC -ge 99 ]
do
  check_online_update
  if [ $EFA_VERBOSE ] ; then
    echo "[`date +%Y-%m-%d_%H:%M:%S` $PROG] starting $CLASSNAME ..."
  fi
  java $EFA_JAVA_ARGUMENTS "$@"
  RC=$?
  if [ $EFA_VERBOSE ] ; then
    echo "[`date +%Y-%m-%d_%H:%M:%S` $PROG] efa exit code: $RC"
  fi
done

if [ $EFA_VERBOSE ] ; then
  echo "[`date +%Y-%m-%d_%H:%M:%S` $PROG] script finished."
fi
exit $RC
