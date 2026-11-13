@echo off
setlocal
cd /d "%~dp0"
where java >nul 2>nul
if errorlevel 1 (
  echo Java 17 or later was not found. Install a JDK and add java.exe to PATH.
  pause
  exit /b 1
)
set "CMS_JAR=%~dp0cms-awt.jar"
if exist "%~dp0target\cms-awt.jar" set "CMS_JAR=%~dp0target\cms-awt.jar"
java -Dcms.config.path="%~dp0Application.config" -cp "%CMS_JAR%;%~dp0lib\*" com.cms.desktop.CmsDesktopApplication
if errorlevel 1 (
  echo CMS did not start. Check Application.config, the Java version, and logs\cms-application.log.
  pause
  exit /b 1
)
endlocal
