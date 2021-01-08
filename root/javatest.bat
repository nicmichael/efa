@echo off

echo Damit efa gestartet werden kann, muss auf dem Rechner die Java
echo Laufzeitumgebung installiert sein.
echo.
echo In order to run efa you need to have the Java Runtime Environment
echo installed on your computer.
echo.

echo Folgende Java-Version ist installiert (mind. v1.6.0 benoetigt):
echo The following Java Version in installed (at least v1.6.0 required):
echo ============================= JAVA VERSION =============================
java -version
echo ========================================================================
echo.
echo Sollte in der vorangehenden Zeile "Befehl oder Dateiname nicht gefunden"
echo stehen, so ist auf dem Rechner KEIN Java installiert.
echo Falls kein Java oder eine Version aelter als v1.6.0 installiert ist,
echo installiere bitte Java wie in der efa-Dokumentation beschrieben.
echo.
echo In case the previous line shows a message "Command or filename not found"
echo this means that there is NO Java installed on your computer.
echo If you don't have Java installed, of if your version is older than v1.6.0,
echo please install Java as described in the efa documentation.

echo.
pause
@CLS