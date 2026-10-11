# KeyStan Desktop - Windows companion for the KeyGo Android password manager.
# Copyright (C) 2026 Nursqode
#
# Part of KeyStan (https://github.com/Nursqode/KeyStan), a fork of KeyGo
# (https://github.com/OffRange/KeyGo). Licensed under the GNU General Public License v3.0 or
# later, like the project it belongs to; see LICENSE.
#
# Builds the MSI installer without relying on WiX's ICE validation.
#
# `:app:packageMsi` is the normal way, and is what to try first. It fails in any session that cannot
# reach the Windows Installer service - a sandboxed build, a service account, some CI runners - with
#
#     error LGHT0217: Error executing ICE action 'ICE01' ... The Windows Installer Service could not
#     be accessed.
#     ... exited with 216 code
#
# ICE validation only *checks* the installer, so the fix is to let jpackage generate its WiX sources
# (which it does successfully) and then link them with light.exe directly, with the validations
# suppressed. The MSI that comes out is built from the same WiX sources jpackage would have linked.
#
# Usage, from the desktop/ directory, after `..\gradlew.bat -p . :app:createDistributable`:
#
#     powershell -ExecutionPolicy Bypass -File build-msi.ps1
#
# Requires WiX Toolset 3.x. If `$WixPath` does not contain it, the script unpacks the copy the
# Compose plugin already downloaded into its own cache, so there is nothing to install by hand.

param(
    [string]$WixPath = "$PSScriptRoot\app\build\compose\wix",
    [string]$JdkHome = $(if ($env:JAVA_HOME) { $env:JAVA_HOME } else { (Get-Command javac -ErrorAction SilentlyContinue).Source -replace '\\bin\\javac\.exe$', '' })
)

$ErrorActionPreference = "Stop"
$desktop = $PSScriptRoot

if (-not (Test-Path "$WixPath\light.exe")) {
    $cache = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { "$env:USERPROFILE\.gradle" }
    $zip = Get-ChildItem "$cache\compose-jb" -Filter "wix*.zip" -ErrorAction SilentlyContinue | Select-Object -First 1
    if (-not $zip) { throw "WiX not found at $WixPath and no wix*.zip in $cache\compose-jb - build an installer once, or pass -WixPath" }
    Write-Host "unpacking $($zip.Name) into $WixPath"
    New-Item -ItemType Directory -Force -Path $WixPath | Out-Null
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    [System.IO.Compression.ZipFile]::ExtractToDirectory($zip.FullName, $WixPath)
}
$appImage = "$desktop\app\build\compose\binaries\main\app\KeyStan"
$runtime = "$desktop\app\build\compose\tmp\main\runtime"
$input = "$desktop\app\build\compose\tmp\msi-input"
$out = "$desktop\dist\msi"
$temp = "$desktop\app\build\compose\tmp\msi-work"

if (-not (Test-Path "$appImage\KeyStan.exe")) { throw "no app image - run :app:createDistributable first" }
if (-not (Test-Path $runtime)) { throw "no runtime image - run :app:createDistributable first" }
if (-not (Test-Path "$WixPath\light.exe")) { throw "WiX not found at $WixPath - extract wix311.zip there or pass -WixPath" }

Remove-Item $input, $temp -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $input, $out | Out-Null
# The payload only: the app image's own launcher configuration would collide with the one jpackage
# writes into the MSI image.
Get-ChildItem "$appImage\app" -File |
    Where-Object { $_.Name -like "*.jar" -or $_.Name -eq "skiko-windows-x64.dll" -or $_.Name -eq "icudtl.dat" } |
    Copy-Item -Destination $input -Force
$mainJar = (Get-ChildItem $input -Filter "app-*.jar" | Select-Object -First 1).Name
Write-Host "payload: $((Get-ChildItem $input -File).Count) files, main jar $mainJar"

$env:PATH = "$WixPath;" + $env:PATH
$env:TMP = "$temp-scratch"
New-Item -ItemType Directory -Force -Path $env:TMP | Out-Null
# jpackage is expected to fail here (that is the whole point of this script), and it writes that
# failure to stderr; with the error preference at Stop, PowerShell would end the script on it.
$previousPreference = $ErrorActionPreference
$ErrorActionPreference = "Continue"
& "$JdkHome\bin\jpackage.exe" --type msi --name KeyStan --app-version 1.1.0 `
    --input $input --main-jar $mainJar --main-class de.davis.keygo.desktop.MainKt `
    --runtime-image $runtime --dest $out `
    --description "A local-first password manager, compatible with KeyGo on Android" `
    --vendor "Nursqode" --copyright "(c) 2026 Nursqode. GNU GPL v3.0 or later." `
    --win-upgrade-uuid "35ED3B02-6522-3DDB-A3D6-454F9055E051" `
    --win-menu --win-shortcut --win-per-user-install --win-dir-chooser --win-menu-group KeyStan `
    --java-options "-Dcompose.application.configure.swing.globals=true" `
    --java-options "-Dskiko.library.path=`$APPDIR" `
    --temp $temp 2>&1 | Select-Object -Last 2 | Write-Host
$ErrorActionPreference = $previousPreference

$wixobj = Get-ChildItem "$temp\wixobj" -Filter *.wixobj -ErrorAction SilentlyContinue
if (-not $wixobj) { throw "jpackage produced no wixobj files" }
Write-Host "wixobj: $($wixobj.Count)"

$msi = "$out\KeyStan-1.1.0.msi"
Remove-Item $msi -Force -ErrorAction SilentlyContinue
$sice = New-Object System.Collections.Generic.List[string]
foreach ($n in 1..199) {
    $sice.Add(("-sice:ICE{0:D3}" -f $n))
    $sice.Add(("-sice:ICE{0:D2}" -f $n))
}
$lightArgs = @(
    "-nologo", "-spdb", "-ext", "WixUtilExtension", "-out", $msi, "-ext", "WixUIExtension",
    "-b", "$temp\config", "-loc", "$temp\config\MsiInstallerStrings_en.wxl", "-cultures:en-us"
) + ($sice | Select-Object -Unique) + ($wixobj | ForEach-Object { $_.FullName })
& "$WixPath\light.exe" @lightArgs
if ($LASTEXITCODE -ne 0) { throw "light.exe failed with $LASTEXITCODE" }

Write-Host ("MSI: {0} ({1:N1} MB)" -f $msi, ((Get-Item $msi).Length / 1MB))
