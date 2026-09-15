@echo off
setlocal

set "MAVEN_PROJECTBASEDIR=%~dp0"
set "WRAPPER_PROPERTIES=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.properties"

if "%MAVEN_USER_HOME%"=="" (
  set "MAVEN_USER_HOME=%USERPROFILE%\.m2"
)

for /f "tokens=1,* delims==" %%A in ('findstr /r "^distributionUrl=" "%WRAPPER_PROPERTIES%"') do (
  set "DISTRIBUTION_URL=%%B"
)

set "MAVEN_VERSION=3.9.9"
set "DIST_DIR=%MAVEN_USER_HOME%\wrapper\dists\apache-maven-%MAVEN_VERSION%"
set "MAVEN_HOME=%DIST_DIR%\apache-maven-%MAVEN_VERSION%"
set "MAVEN_ZIP=%DIST_DIR%\apache-maven-%MAVEN_VERSION%-bin.zip"

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command ^
    "$ErrorActionPreference='Stop'; [Net.ServicePointManager]::SecurityProtocol=[Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri '%DISTRIBUTION_URL%' -OutFile '%MAVEN_ZIP%'; Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%DIST_DIR%' -Force"
  if errorlevel 1 exit /b 1
)

"%MAVEN_HOME%\bin\mvn.cmd" %*
endlocal
