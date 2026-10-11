# The whole Android pipeline with the Rust toolchain's own mingw binutils on PATH - without it the
# host parts of the build (proc-macro crates, getrandom, windows-sys) fail on a missing dlltool.exe.
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

$mingw = "C:\Users\neo\KeyStan\.mingw\mingw64\bin"
$selfContained = "C:\Users\neo\.rustup\toolchains\1.98.0-x86_64-pc-windows-gnu\lib\rustlib\x86_64-pc-windows-gnu\bin\self-contained"
$env:PATH = "$mingw;$selfContained;$env:USERPROFILE\.cargo\bin;$env:PATH"
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
# Host tools (dlltool, gcc) cannot create temporary files in the system TEMP on this machine, so the
# whole build gets a scratch directory of its own inside the project.
$env:TMP = "$root\.tmp-build"
$env:TEMP = $env:TMP
New-Item -ItemType Directory -Force -Path $env:TMP | Out-Null
$env:ANDROID_NDK_HOME = "$env:ANDROID_HOME\ndk\28.2.13676358"
$cargo = "$env:USERPROFILE\.cargo\bin\cargo.exe"

RunBlock "native libraries" "$root\rust\rust-code" $cargo @(
    "ndk", "-t", "arm64-v8a", "-t", "armeabi-v7a", "-t", "x86_64", "--platform", "26",
    "-o", "..\build\generated\source\uniffi\jniLibs", "build", "--release", "-p", "keygo-bindings"
)

RunBlock "uniffi bindings" "$root\rust\rust-code" $cargo @(
    "run", "--release", "-p", "keygo-bindings", "--bin", "uniffi-bindgen", "--", "generate",
    "--library", "target\aarch64-linux-android\release\libkeygo_bindings.so",
    "--language", "kotlin", "--no-format",
    "--out-dir", "..\build\generated\source\uniffi\kotlin"
)
Step "bindings present: $(Test-Path "$root\rust\build\generated\source\uniffi\kotlin")"

RunBlock "gradle assemblePlayStoreDebug" $root "$root\gradlew.bat" @(
    ":app:assemblePlayStoreDebug", "-Prust.compile=false", "--console=plain", "--no-watch-fs"
)

$apk = Get-ChildItem "$root\app\build\outputs" -Recurse -Filter *.apk -ErrorAction SilentlyContinue
if (-not $apk) { Step "NO APK"; exit 1 }
$apk | ForEach-Object { Step ("apk: {0} ({1:N1} MB)" -f $_.FullName, ($_.Length / 1MB)) }

$adb = "$env:ANDROID_HOME\platform-tools\adb.exe"
Step "install on emulator-5556"
& $adb -s emulator-5556 install -r -t $apk[0].FullName 2>&1 | Tee-Object -FilePath $log -Append | Select-Object -Last 2 | Write-Host
& $adb -s emulator-5556 shell monkey -p de.davis.keygo -c android.intent.category.LAUNCHER 1 2>&1 | Select-Object -Last 1 | Write-Host
Start-Sleep -Seconds 10
& $adb -s emulator-5556 shell dumpsys activity activities 2>&1 | Select-String -Pattern 'topResumedActivity|mResumedActivity' | Select-Object -First 1 | Write-Host
Step "=== ready ==="
