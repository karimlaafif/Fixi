# Fixi - Script de lancement PowerShell
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

$env:JAVA_HOME = "C:\Users\dell\.p2\pool\plugins\org.eclipse.justj.openjdk.hotspot.jre.full.win32.x86_64_25.0.5.v20261030-0959\jre"
$env:Path = "$env:JAVA_HOME\bin;C:\Users\dell\maven\bin;" + $env:Path

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host " Fixi - Lancement de l'application JavaFX" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Cyan
mvn javafx:run
