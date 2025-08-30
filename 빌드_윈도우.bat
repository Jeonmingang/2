@echo off
title Build SamSkyBridge (Windows)
cd /d %~dp0

set "JAR=.mvn\wrapper\maven-wrapper.jar"
if not exist "%JAR%" (
  echo [ERROR] %JAR% not found.
  echo ----
  echo �ذ�: .mvn\wrapper ������ maven-wrapper.jar ������ �־��ּ���.
  pause
  exit /b 1
)

REM Run Maven Wrapper Main directly via Java (no PowerShell/curl)
java -cp "%JAR%" org.apache.maven.wrapper.MavenWrapperMain -DskipTests clean package

echo.
echo === Done ===
echo Output: target\SamSkyBridge-0.2.0.jar
pause
