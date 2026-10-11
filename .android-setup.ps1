# Sets up the toolchain this repository needs to build the Android app, then builds it.
# Written for a machine with the Android SDK/NDK and JDK 17 but no Rust and no Visual Studio:
# the GNU host toolchain bundles its own linker, and the Android targets link with the NDK's clang.
$ErrorActionPreference = "Continue"
$root = "C:\Users\neo\KeyStan"
$log = "$root\.android-setup.log"
if (Test-Path $log) { Remove-Item $log -Force }
function Step($message) {
    $line = "[{0:HH:mm:ss}] {1}" -f (Get-Date), $message
    Write-Host $line
    Add-Content -Path $log -Value $line
}
function Run($exe, $arguments) {
    Step "run: $exe $($arguments -join ' ')"
    & $exe @arguments 2>&1 | Tee-Object -FilePath $log -Append | Select-Object -Last 3 | Write-Host
    Step "exit=$LASTEXITCODE"
}

$cargoBin = "$env:USERPROFILE\.cargo\bin"
$env:PATH = "$cargoBin;$env:PATH"

Step "=== 1. rustup ==="
if (-not (Test-Path "$cargoBin\rustup.exe")) {
    $installer = "$env:TEMP\rustup-init.exe"
    Step "downloading rustup-init.exe (gnu host)"
    Invoke-WebRequest -Uri "https://static.rust-lang.org/rustup/dist/x86_64-pc-windows-gnu/rustup-init.exe" `
        -OutFile $installer -UseBasicParsing
    Run $installer @("-y", "--default-host", "x86_64-pc-windows-gnu", "--default-toolchain", "stable",
                     "--profile", "minimal", "--no-modify-path")
} else { Step "rustup already present" }

Step "=== 2. the toolchain the repository pins ==="
Run "$cargoBin\rustup.exe" @("toolchain", "install", "1.98.0", "--profile", "minimal", "--no-self-update")
Run "$cargoBin\rustup.exe" @("target", "add", "--toolchain", "1.98.0",
    "aarch64-linux-android", "armv7-linux-androideabi", "x86_64-linux-android")

Step "=== 3. cargo-ndk ==="
Run "$cargoBin\cargo.exe" @("install", "cargo-ndk", "--locked")

Step "=== 4. native libraries for the three ABIs ==="
Push-Location "$root\rust\rust-code"
$env:ANDROID_NDK_HOME = "$env:ANDROID_HOME\ndk\28.2.13676358"
Run "$cargoBin\cargo.exe" @("ndk", "-t", "arm64-v8a", "-t", "armeabi-v7a", "-t", "x86_64", "--platform", "26",
    "-o", "..\build\generated\source\uniffi\jniLibs", "build", "--release", "-p", "keygo-bindings")

Step "=== 5. uniffi Kotlin bindings ==="
Run "$cargoBin\cargo.exe" @("run", "--release", "-p", "keygo-bindings", "--bin", "uniffi-bindgen", "--", "generate",
    "--library", "target\aarch64-linux-android\release\libkeygo_bindings.so",
    "--language", "kotlin", "--no-format",
    "--out-dir", "..\build\generated\source\uniffi\kotlin")
Pop-Location

Step "=== 6. the APK ==="
Push-Location $root
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
Run "$root\gradlew.bat" @(":app:assemblePlayStoreDebug", "-Prust.compile=false", "--console=plain", "--no-watch-fs")
Pop-Location

Step "=== done ==="
Get-ChildItem "$root\app\build\outputs\apk" -Recurse -Filter *.apk -ErrorAction SilentlyContinue |
    ForEach-Object { Step ("apk: {0} ({1:N1} MB)" -f $_.FullName, ($_.Length / 1MB)) }
