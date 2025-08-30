@ECHO OFF
SETLOCAL ENABLEDELAYEDEXPANSION
SET "WRAPPER_DIR=%~dp0.mvn\wrapper"
SET "PROPS=%WRAPPER_DIR%\maven-wrapper.properties"
SET "JAR=%WRAPPER_DIR%\maven-wrapper.jar"

IF NOT EXIST "%JAR%" (
  IF NOT EXIST "%PROPS%" (
    ECHO [ERROR] %PROPS% not found.
    EXIT /B 1
  )
  SET "URL="
  FOR /F "usebackq tokens=1,* delims==" %%A IN ("%PROPS%") DO (
    IF /I "%%A"=="wrapperUrl" SET "URL=%%B"
  )
  IF NOT DEFINED URL (
    ECHO [ERROR] wrapperUrl not found in %PROPS%
    EXIT /B 1
  )
  REM Use PowerShell to download with explicit -Uri and env vars to avoid parsing issues
  SET "PScmd=$u=$env:URL; $j=$env:JAR; Invoke-WebRequest -Uri $u -OutFile $j"
  powershell -NoLogo -NoProfile -ExecutionPolicy Bypass -Command "%PScmd%"
  IF ERRORLEVEL 1 (
    ECHO [WARN] PowerShell download failed. Trying curl...
    WHERE curl >NUL 2>&1 && curl -L "!URL!" -o "%JAR%"
  )
  IF NOT EXIST "%JAR%" (
    ECHO [ERROR] Failed to download Maven Wrapper Jar.
    EXIT /B 1
  )
)

SET "JAVA_EXE=java"
"%JAVA_EXE%" -cp "%JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
