@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

for /f "tokens=*" %%a in ('echo prompt $E^| cmd') do set "ESC=%%a"
set "CYAN=%ESC%[36m"
set "GREEN=%ESC%[32m"
set "YELLOW=%ESC%[33m"
set "RED=%ESC%[31m"
set "RESET=%ESC%[0m"

set "ROOT_DIR=%~dp0"
set "APP_NAME=launch"
set "MAIN_CLASS=Launch"
set "OUT_DIR=out"
set "TARGET_DIR=target"
set "DIST_DIR=dist"
set "JAR_FILE=%APP_NAME%.jar"
set "DEPLOY_DIR=%ROOT_DIR%tp-net-client-launch"

echo.
echo %CYAN%===============================================%RESET%
echo %CYAN%  BUILD: %APP_NAME%%RESET%
echo %CYAN%===============================================%RESET%
echo.

echo %CYAN%[INFO]%RESET% [1/6] Cleaning previous build...
if exist %OUT_DIR%    rmdir /s /q %OUT_DIR%
if exist %TARGET_DIR% rmdir /s /q %TARGET_DIR%
if exist %DIST_DIR%   rmdir /s /q %DIST_DIR%
echo %GREEN%[OK]%RESET% Cleaned.

echo.
echo %CYAN%[INFO]%RESET% [2/6] Compiling Java sources...
mkdir %OUT_DIR%
mkdir %OUT_DIR%\_src
copy /y launch.java %OUT_DIR%\_src\Launch.java > nul
javac -encoding UTF-8 -d %OUT_DIR% ^
  %OUT_DIR%\_src\Launch.java ^
  launch\App.java ^
  launch\app\config\Config.java ^
  launch\app\helpers\SimpleBuilder.java ^
  launch\app\helpers\SimpleDownloader.java ^
  launch\app\helpers\SimpleProcess.java ^
  launch\app\watchdog\CurrentKiller.java ^
  launch\app\watchdog\ManagedProcess.java ^
  launch\app\watchdog\Watchdog.java ^
  launch\app\update\Staging.java
if errorlevel 1 (
    echo %RED%[FAILED]%RESET% Compilation failed!
    pause & exit /b 1
)
echo %GREEN%[OK]%RESET% Compilation thanh cong.

echo.
echo %CYAN%[INFO]%RESET% [3/6] Packaging JAR...
mkdir %TARGET_DIR%
jar --create --file %TARGET_DIR%\%JAR_FILE% --main-class %MAIN_CLASS% -C %OUT_DIR% .
if errorlevel 1 (
    echo %RED%[FAILED]%RESET% JAR packaging failed!
    pause & exit /b 1
)
echo %GREEN%[OK]%RESET% JAR: %TARGET_DIR%\%JAR_FILE%

echo.
echo %CYAN%[INFO]%RESET% [4/6] jpackage app-image...
jpackage ^
  --type app-image ^
  --name %APP_NAME% ^
  --input %TARGET_DIR% ^
  --main-jar %JAR_FILE% ^
  --dest %DIST_DIR% ^
  --win-console
if errorlevel 1 (
    echo %RED%[FAILED]%RESET% jpackage failed!
    pause & exit /b 1
)
echo %GREEN%[OK]%RESET% App image: %DIST_DIR%\%APP_NAME%\

echo.
echo %CYAN%[INFO]%RESET% [5/6] Copy launch.properties...
copy /y launch.properties %DIST_DIR%\%APP_NAME%\launch.properties > nul
echo %GREEN%[OK]%RESET% launch.properties sao chep thanh cong.

echo.
echo %CYAN%[INFO]%RESET% [6/6] Deploy va don dep...
if exist "%DEPLOY_DIR%" rmdir /s /q "%DEPLOY_DIR%"
xcopy "%DIST_DIR%\%APP_NAME%" "%DEPLOY_DIR%\" /e /i /q
if errorlevel 1 (
    echo %RED%[FAILED]%RESET% Deploy that bai!
    pause & exit /b 1
)
echo %GREEN%[OK]%RESET% Deployed to: tp-net-client-launch\

rmdir /s /q %OUT_DIR%
rmdir /s /q %TARGET_DIR%
rmdir /s /q %DIST_DIR%
echo %GREEN%[OK]%RESET% Temp files cleaned.

echo.
echo %GREEN%===============================================%RESET%
echo %GREEN%  BUILD HOAN THANH!%RESET%
echo %GREEN%===============================================%RESET%
echo.
echo %CYAN%[INFO]%RESET% Output: tp-net-client-launch\%APP_NAME%.exe
echo.

pause
endlocal