# Build a Windows .exe installer of the Calendar app with jpackage (bundles its own Java
# runtime). Needs a JDK 17+ on PATH and the WiX Toolset 3.x (jpackage uses it for .exe).
# Usage: packaging\build-installer.ps1 [-Version 1.0] [-OutputDir dist]
# The installer is unsigned, so SmartScreen may warn on first run.
param(
  [string]$Version = "",
  [string]$OutputDir = ""
)
$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$JavaDir = Join-Path $Root "archive\java"
if (-not $OutputDir) { $OutputDir = Join-Path $Root "dist" }
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
$OutputDir = (Resolve-Path $OutputDir).Path

$match = Select-String -Path (Join-Path $JavaDir "build.gradle") -Pattern "^version\s+'([^']+)'" | Select-Object -First 1
if (-not $match) { throw "could not read version from archive\java\build.gradle" }
$JarVersion = $match.Matches[0].Groups[1].Value
if (-not $Version) { $Version = $JarVersion }

Push-Location $JavaDir
try {
  & .\gradlew.bat jar --console=plain
  if ($LASTEXITCODE -ne 0) { throw "gradle jar failed" }
} finally { Pop-Location }

$Work = Join-Path ([System.IO.Path]::GetTempPath()) ("calendar-pkg-" + [guid]::NewGuid())
$InputDir = Join-Path $Work "input"
$Dest = Join-Path $Work "dest"
New-Item -ItemType Directory -Force -Path $InputDir, $Dest | Out-Null
Copy-Item (Join-Path $JavaDir "build\libs\calendar-$JarVersion.jar") $InputDir

try {
  & jpackage `
    --name Calendar `
    --app-version $Version `
    --vendor "Dhairya Bhatia" `
    --description "Multi-calendar scheduler with a GUI, a prompt and scripting" `
    --input $InputDir `
    --main-jar "calendar-$JarVersion.jar" `
    --main-class CalendarRunner `
    --add-modules "java.desktop,java.logging,java.prefs,java.base" `
    --icon (Join-Path $Root "packaging\windows\app.ico") `
    --type exe `
    --win-menu --win-shortcut --win-dir-chooser --win-per-user-install `
    --win-upgrade-uuid "EC0C6904-D5B7-4B4C-87D6-33E794DAE41D" `
    --dest $Dest
  if ($LASTEXITCODE -ne 0) { throw "jpackage failed" }

  $Final = Join-Path $OutputDir "Calendar-$Version-windows-x64.exe"
  Move-Item -Force (Join-Path $Dest "Calendar-$Version.exe") $Final
  Write-Host "built $Final"
} finally {
  Remove-Item -Recurse -Force $Work -ErrorAction SilentlyContinue
}
