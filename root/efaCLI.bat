@echo off

REM # ##########################################
REM # Test for Operating System                #
REM # ##########################################
IF "%OS%" == "Windows_NT" GOTO WINNT
GOTO WIN9X

:WIN9X
command.com /e:1024 /crunefa.bat de.nmichael.efa.cli.Main runcli %1 %2 %3 %4 %5 %6 %7
GOTO EXIT

:WINNT
call runefa.bat de.nmichael.efa.cli.Main runcli %1 %2 %3 %4 %5 %6 %7
GOTO EXIT

:EXIT
