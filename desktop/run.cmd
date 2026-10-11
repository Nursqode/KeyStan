@echo off
rem Runs KeyStan Desktop from source.
rem
rem The desktop build is its own Gradle build rooted at this directory, so it is invoked through the
rem repository's wrapper with an explicit project directory. The Android build is untouched by it.
setlocal
pushd "%~dp0"
call "..\gradlew.bat" -p . :app:run %*
set EXITCODE=%ERRORLEVEL%
popd
endlocal & exit /b %EXITCODE%
