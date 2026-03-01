@echo off
setlocal

set APP_NAME=launch
set MAIN_CLASS=Launch
set SRC_DIR=.
set OUT_DIR=out
set TARGET_DIR=target
set DIST_DIR=dist
set JAR_FILE=%APP_NAME%.jar

echo [BUILD] Cleaning previous build...
if exist %OUT_DIR%    rmdir /s /q %OUT_DIR%
if exist %TARGET_DIR% rmdir /s /q %TARGET_DIR%
if exist %DIST_DIR%   rmdir /s /q %DIST_DIR%

echo [BUILD] Compiling Java sources...
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
    echo [ERROR] Compilation failed!
    pause
    exit /b 1
)

echo [BUILD] Packaging JAR...
mkdir %TARGET_DIR%
jar --create --file %TARGET_DIR%\%JAR_FILE% --main-class %MAIN_CLASS% -C %OUT_DIR% .

if errorlevel 1 (
    echo [ERROR] JAR packaging failed!
    pause
    exit /b 1
)

echo [BUILD] Running jpackage...
jpackage ^
  --type app-image ^
  --name %APP_NAME% ^
  --input %TARGET_DIR% ^
  --main-jar %JAR_FILE% ^
  --dest %DIST_DIR% ^
  --win-console

if errorlevel 1 (
    echo [ERROR] jpackage failed!
    pause
    exit /b 1
)

echo [BUILD] Copying launch.properties...
copy /y launch.properties %DIST_DIR%\%APP_NAME%\launch.properties

echo [BUILD] Cleaning temp files...
rmdir /s /q %OUT_DIR%
rmdir /s /q %TARGET_DIR%

echo.
echo [BUILD] Done! Output: %DIST_DIR%\%APP_NAME%\%APP_NAME%.exe
pause
