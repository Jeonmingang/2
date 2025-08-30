@echo off
title Build SamSkyBridge (Windows)
cd /d %~dp0

set "JAR=.mvn\wrapper\maven-wrapper.jar"
if not exist "%JAR%" (
  echo [ERROR] %JAR% not found.
  echo ----
  echo 해결: .mvn\wrapper 폴더에 maven-wrapper.jar 파일을 넣어주세요.
  pause
  exit /b 1
)

REM Run Maven Wrapper Main directly via Java (no PowerShell/curl)
java -cp "%JAR%" org.apache.maven.wrapper.MavenWrapperMain -DskipTests clean package

echo.
echo === Done ===
echo Output: target\SamSkyBridge-0.2.0.jar
pause
