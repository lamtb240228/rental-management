@echo off
setlocal

set "MAVEN_VERSION=3.9.11"
set "BASE_DIR=%~dp0"
set "WRAPPER_DIR=%BASE_DIR%.mvn\wrapper"
set "MAVEN_HOME=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%"
set "MAVEN_ZIP=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%-bin.zip"
set "MAVEN_URL=https://archive.apache.org/dist/maven/maven-3/%MAVEN_VERSION%/binaries/apache-maven-%MAVEN_VERSION%-bin.zip"

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    if not exist "%WRAPPER_DIR%" mkdir "%WRAPPER_DIR%"
    echo Maven %MAVEN_VERSION% was not found locally. Downloading...
    curl.exe -L --fail --output "%MAVEN_ZIP%" "%MAVEN_URL%"
    if errorlevel 1 exit /b %errorlevel%

    echo Extracting Maven...
    powershell -NoProfile -ExecutionPolicy Bypass -Command ^
        "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%WRAPPER_DIR%' -Force"
    if errorlevel 1 exit /b %errorlevel%
)

call "%MAVEN_HOME%\bin\mvn.cmd" %*
exit /b %errorlevel%
