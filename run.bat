@echo off
setlocal
cd /d "%~dp0"

if not defined JAVA_HOME (
    set "JAVA_HOME=C:\Users\dell\.p2\pool\plugins\org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_25.0.5.v20261030-0959\jre"
)
set "PATH=%JAVA_HOME%\bin;C:\Users\dell\maven\bin;%PATH%"

echo ========================================================
echo  Fixi - Lancement de l'application JavaFX
echo ========================================================
call mvn javafx:run
pause
