# Finishes what .android-setup.ps1 could not: dlltool.exe lives inside the Rust toolchain but not on
# PATH, so the host link step of the bindgen build failed. Then it builds and installs the APK on the
# running emulator.
$ErrorActionPreference = "Continue"
$root = "C:\Users\neo\KeyStan"
$log = "$root\.android-setup.log"
function Step($message) {
    $line = "[{0:HH:mm:ss}] {1}" -f (Get-Date), $message
    Write-Host $line
    Add-Content -Path $log -Value $line
}
function RunBlock($label, $workdir, $exe, $arguments) {
    Step "--- $label"
    Push-Location $workdir
    & $exe @arguments 2>&1 | Tee-Object -FilePath $log -Append | Select-Object -Last 4 | Write-Host
    $code = $LASTEXITCODE
    Pop-Location
    Step "$label exit=$code"
    return $code
}

$cargo = "$env:USERPROFILE\.cargo\bin\cargo.exe"
$selfContained = "C:\Users\neo\.rustup\toolchains\1.98.0-x86_64-pc-windows-gnu\lib\rustlib\x86_64-pc-windows-gnu\bin\self-contained"
$env:PATH = "$selfContained;$env:USERPROFILE\.cargo\bin;$env:PATH"
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"

RunBlock "uniffi bindings" "$root\rust\rust-code" $cargo @(
    "run", "--release", "-p", "keygo-bindings", "--bin", "uniffi-bindgen", "--", "generate",
    "--library", "target\aarch64-linux-android\release\libkeygo_bindings.so",
    "--language", "kotlin", "--no-format",
    "--out-dir", "..\build\generated\source\uniffi\kotlin"
)
Step "bindings present: $(Test-Path "$root\rust\build\generated\source\uniffi\kotlin")"

# The previous Gradle run is still holding the build; let it finish so this one does not wait on a lock.
for ($i = 0; $i -lt 60; $i++) {
    $busy = Get-Process | Where-Object { $_.ProcessName -eq 'java' -and $_.StartTime -gt (Get-Date).AddMinutes(-30) } |
        Where-Object { $_.CPU -gt 0 }
    if (-not (Get-Process | Where-Object { $_.ProcessName -eq 'GradleWrapperMain' })) { break }
    Start-Sleep -Seconds 10
}

RunBlock "gradle assemblePlayStoreDebug" $root "$root\gradlew.bat" @(
    ":app:assemblePlayStoreDebug", "-Prust.compile=false", "--console=plain", "--no-watch-fs"
)

$apk = Get-ChildItem "$root\app\build\outputs" -Recurse -Filter *.apk -ErrorAction SilentlyContinue
if (-not $apk) { Step "NO APK"; exit 1 }
$apk | ForEach-Object { Step ("apk: {0} ({1:N1} MB)" -f $_.FullName, ($_.Length / 1MB)) }

$adb = "$env:ANDROID_HOME\platform-tools\adb.exe"
$target = "emulator-5556"
Step "installing on $target"
& $adb -s $target install -r -t $apk[0].FullName 2>&1 | Tee-Object -FilePath $log -Append | Select-Object -Last 3 | Write-Host
Step "launching"
& $adb -s $target shell monkey -p de.davis.keygo -c android.intent.category.LAUNCHER 1 2>&1 | Select-Object -Last 2 | Write-Host
Start-Sleep -Seconds 12
& $adb -s $target shell dumpsys activity activities 2>&1 | Select-String -Pattern 'mResumedActivity|topResumedActivity' | Select-Object -First 2 | Write-Host
Step "=== ready ==="
