@echo off
rem Builds a self-contained KeyStan folder: app\build\compose\binaries\main\app\KeyStan\KeyStan.exe
rem
rem Uses jpackage's app-image, which needs no installer toolchain; `packageMsi` would need WiX.
setlocal
pushd "%~dp0"
call "..\gradlew.bat" -p . :app:createDistributable %*
set EXITCODE=%ERRORLEVEL%
popd
if "%EXITCODE%"=="0" echo.
if "%EXITCODE%"=="0" echo Built: %~dp0app\build\compose\binaries\main\app\KeyStan\
endlocal & exit /b %EXITCODE%
