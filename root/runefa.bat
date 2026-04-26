@echo off

IF "%1" == "" GOTO USAGE
IF "%2" == "run" GOTO RUN
IF "%2" == "runcli" GOTO RUN

REM # ##########################################
REM # Test for Operating System                #
REM # ##########################################
IF "%OS%" == "Windows_NT" GOTO WINNT
GOTO WIN9X

:WIN9X
command.com /e:1024 /crunefa.bat %1 run %2 %3 %4 %5 %6 %7 %8
GOTO END

:WINNT
call runefa.bat %1 run %2 %3 %4 %5 %6 %7 %8
GOTO END


REM # ##########################################
REM # Show Usage                               #
REM # ##########################################
:USAGE
echo usage: runefa.bat mainclass [run] [arguments]
goto EXIT

REM # ##########################################
REM # Prepare to run Program                   #
REM # ##########################################
:RUN
REM Preparing to run ...


REM # ##########################################
REM # Classpath                                #
REM # ##########################################

REM Classpath: efa
SET CP=program/efa.jar;program/efahelp.jar;program

REM Classpath: Java Help
SET CP=%CP%;program/plugins/jh.jar

REM Classpath: FTP-Plugin
SET CP=%CP%;program/plugins/edtftpj.jar

REM Classpath: SFTP support for FTP Plugin
SET CP=%CP%;program/plugins/jsch-0.1.55.jar

REM Classpath: MAIL-Plugin
SET CP=%CP%;program/plugins/javax.mail.jar
SET CP=%CP%;program/plugins/activation.jar

REM Classpath: JSUNTIMES-Plugin
SET CP=%CP%;program/plugins/jsuntimes.jar

REM Classpath: FOP-Plugin
SET CP=%CP%;program/plugins/avalon-framework.jar
SET CP=%CP%;program/plugins/batik-all.jar
SET CP=%CP%;program/plugins/commons-io.jar
SET CP=%CP%;program/plugins/commons-logging.jar
SET CP=%CP%;program/plugins/fop.jar
SET CP=%CP%;program/plugins/xmlgraphics-commons.jar

REM Classpath: EFA Flat Laf
SET CP=%CP%;program/plugins/flatlaf-3.6.jar

REM Classpath: JSON
SET CP=%CP%;program/plugins/json-20250517.jar

REM Classpath: Weather-Plugin
SET CP=%CP%;program/plugins/commons-codec.jar
SET CP=%CP%;program/plugins/signpost-core.jar


REM # ##########################################
REM # JVM Settings                             #
REM # ##########################################

REM Java Heap
REM A higher Java Heaps helps to speed up efa on slower computers
REM As garbage collection needs to run at lower frequencies
SET EFA_JAVA_HEAP=192m
SET EFA_NEW_SIZE=32m
IF EXIST javaheap.bat CALL javaheap.bat

REM JVM Options
SET JVMOPTIONS=-Xmx%EFA_JAVA_HEAP% -XX:NewSize=%EFA_NEW_SIZE% -XX:MaxNewSize=%EFA_NEW_SIZE%


REM # ##########################################
REM # Run Program                              #
REM # ##########################################

REM Java Arguments
SET EFA_JAVA_ARGUMENTS=%JVMOPTIONS% -cp %CP% %1 -javaRestart %3 %4 %5 %6 %7 %8 %9

SET EFA_RUN_CLI=0
SET EFA_RUN_DEBUG=0
IF "%2" == "runcli" SET EFA_RUN_CLI=1
IF "%EFA_RUN_CLI%" == "1" GOTO STARTCLI
IF "%EFA_RUN_DEBUG%" == "1" GOTO STARTCLIDBG
IF "%OS%" == "Windows_NT" GOTO STARTNT
GOTO START9X

:STARTNT
REM Path for Windows 7 (64 Bit)
SET PATH=%PATH%;C:\Windows\SysWOW64
echo starting %1 (Windows NT) ...
start /b javaw %EFA_JAVA_ARGUMENTS%
GOTO END

:START9X
echo starting %1 (Windows 9x) ...
javaw %EFA_JAVA_ARGUMENTS%
GOTO END

:STARTCLIDBG
echo EFA_JAVA_ARGUMENTS=%EFA_JAVA_ARGUMENTS%
:STARTCLI
java %EFA_JAVA_ARGUMENTS%
goto EXIT

:END
IF "%EFA_RUN_CLI%" == "1" GOTO EXIT
IF "%EFA_RUN_DEBUG%" == "1" GOTO EXIT
@CLS
:EXIT
