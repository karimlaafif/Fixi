$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
Set-Location $root

$jfx = "$env:USERPROFILE\.m2\repository\org\openjfx"
$mp = "$jfx\javafx-base\21.0.5\javafx-base-21.0.5-win.jar;$jfx\javafx-controls\21.0.5\javafx-controls-21.0.5-win.jar;$jfx\javafx-fxml\21.0.5\javafx-fxml-21.0.5-win.jar;$jfx\javafx-graphics\21.0.5\javafx-graphics-21.0.5-win.jar"

$m2 = "$env:USERPROFILE\.m2\repository"
$cp = "target\classes;$m2\org\kordamp\ikonli\ikonli-core\12.3.1\ikonli-core-12.3.1.jar;$m2\org\kordamp\ikonli\ikonli-javafx\12.3.1\ikonli-javafx-12.3.1.jar;$m2\org\kordamp\ikonli\ikonli-feather-pack\12.3.1\ikonli-feather-pack-12.3.1.jar;$m2\com\mysql\mysql-connector-j\8.4.0\mysql-connector-j-8.4.0.jar;$m2\org\postgresql\postgresql\42.7.3\postgresql-42.7.3.jar;$m2\org\mindrot\jbcrypt\0.4\jbcrypt-0.4.jar"

if (-not (Test-Path "target\.openjfx")) {
    New-Item -ItemType Directory -Path "target\.openjfx" | Out-Null
}

Write-Host "Lancement de Fixi (JavaFX)..." -ForegroundColor Green
java "-Djavafx.cachedir=target/.openjfx" --module-path $mp --add-modules javafx.controls,javafx.fxml -cp $cp ma.fixi.App $args
