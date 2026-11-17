@echo off
setlocal
cd /d "%~dp0"

set "JFX_LIB=%USERPROFILE%\.m2\repository\org\openjfx"
set "MODULE_PATH=%JFX_LIB%\javafx-base\21.0.5\javafx-base-21.0.5-win.jar;%JFX_LIB%\javafx-controls\21.0.5\javafx-controls-21.0.5-win.jar;%JFX_LIB%\javafx-fxml\21.0.5\javafx-fxml-21.0.5-win.jar;%JFX_LIB%\javafx-graphics\21.0.5\javafx-graphics-21.0.5-win.jar"

set "CP=target\classes;%USERPROFILE%\.m2\repository\org\kordamp\ikonli\ikonli-core\12.3.1\ikonli-core-12.3.1.jar;%USERPROFILE%\.m2\repository\org\kordamp\ikonli\ikonli-javafx\12.3.1\ikonli-javafx-12.3.1.jar;%USERPROFILE%\.m2\repository\org\kordamp\ikonli\ikonli-feather-pack\12.3.1\ikonli-feather-pack-12.3.1.jar;%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\8.4.0\mysql-connector-j-8.4.0.jar;%USERPROFILE%\.m2\repository\org\postgresql\postgresql\42.7.3\postgresql-42.7.3.jar;%USERPROFILE%\.m2\repository\org\mindrot\jbcrypt\0.4\jbcrypt-0.4.jar"

if not exist target\.openjfx mkdir target\.openjfx

echo Lancement de Fixi (JavaFX)...
java "-Djavafx.cachedir=target/.openjfx" --module-path "%MODULE_PATH%" --add-modules javafx.controls,javafx.fxml -cp "%CP%" ma.fixi.App %*
