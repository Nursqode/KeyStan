# Builds the release APK on this machine, without the CI queue.
#
# The native libraries for all three ABIs are already in rust/build/generated/source/uniffi/jniLibs,
# so this only has to regenerate the Kotlin bindings and run Gradle with the Rust half skipped.
$root = "C:\Users\neo\KeyStan"
$log = "$root\.local-build.log"
Remove-Item $log -Force -ErrorAction SilentlyContinue
function Note($text) { "[$(Get-Date -Format 'HH:mm:ss')] $text" | Tee-Object -FilePath $log -Append | Out-Host }

# A scratch directory of its own: dlltool and gcc cannot create temporary files in the system TEMP.
$env:TMP = "$root\.tmp-build"
$env:TEMP = $env:TMP
New-Item -ItemType Directory -Force -Path $env:TMP | Out-Null

# A fresh cargo home on purpose: the user-level registry cache holds a crate file this process
# cannot open (access denied), and cargo metadata dies on it.
$env:CARGO_HOME = "$root\.cargo-home"
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:PATH = "$root\.mingw\mingw64\bin;C:\Users\neo\.rustup\toolchains\1.98.0-x86_64-pc-windows-gnu\lib\rustlib\x86_64-pc-windows-gnu\bin\self-contained;$env:USERPROFILE\.cargo\bin;$env:JAVA_HOME\bin;$env:PATH"

# Signing: the same keystore that the CI secrets carry.
$creds = Get-Content "C:\Users\neo\KeyStan-signing\KEYSTORE-CREDENTIALS.txt" -Raw
$env:KEYSTORE_FILE = "C:\Users\neo\KeyStan-signing\keystan-release.jks"
$env:KEYSTORE_PASSWORD = ([regex]::Match($creds, 'storePassword: (\S+)')).Groups[1].Value
$env:KEY_ALIAS = ([regex]::Match($creds, 'alias: (\S+)')).Groups[1].Value
$env:KEY_PASSWORD = $env:KEYSTORE_PASSWORD

Note "--- bindings"
Push-Location "$root\rust\rust-code"
& "$root\rust\rust-code\target\release\uniffi-bindgen.exe" generate `
    --library "target\aarch64-linux-android\release\libkeygo_bindings.so" `
    --language kotlin --no-format `
    --out-dir "$root\rust\build\generated\source\uniffi\kotlin" 2>&1 |
    Tee-Object -FilePath $log -Append | Select-Object -Last 5 | Out-Host
$bindingsExit = $LASTEXITCODE
Pop-Location
Note "bindings exit=$bindingsExit"
$bindingCount = (Get-ChildItem "$root\rust\build\generated\source\uniffi\kotlin" -Recurse -File -ErrorAction SilentlyContinue | Measure-Object).Count
Note "bindings present: $bindingCount files"
if ($bindingCount -eq 0) { Note "STOP: no bindings"; exit 1 }

Note "--- gradle assemblePlayStoreRelease"
& "$root\gradlew.bat" -p $root :app:assemblePlayStoreRelease '-Prust.compile=false' --console=plain 2>&1 |
    Tee-Object -FilePath $log -Append | Select-Object -Last 8 | Out-Host
Note "gradle exit=$LASTEXITCODE"

$apk = Get-ChildItem "$root\app\build\outputs\apk" -Recurse -Filter *.apk -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $apk) { Note "STOP: no APK"; exit 1 }
Note "APK: $($apk.FullName) ($([math]::Round($apk.Length / 1MB, 1)) MB)"

Note "--- install into the emulator"
& "$env:ANDROID_HOME\platform-tools\adb.exe" -s emulator-5556 install -r -t $apk.FullName 2>&1 |
    Tee-Object -FilePath $log -Append | Select-Object -Last 3 | Out-Host
& "$env:ANDROID_HOME\platform-tools\adb.exe" -s emulator-5556 shell monkey -p de.nursqode.keystan -c android.intent.category.LAUNCHER 1 2>&1 |
    Select-Object -Last 1 | Out-Host
Start-Sleep -Seconds 8
& "$env:ANDROID_HOME\platform-tools\adb.exe" -s emulator-5556 shell dumpsys package de.nursqode.keystan 2>&1 |
    Select-String -Pattern 'versionName' | Select-Object -First 1 | ForEach-Object { Note $_.Line.Trim() }
Note "done"
