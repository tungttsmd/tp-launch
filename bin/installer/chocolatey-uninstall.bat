@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion
cls
for /f "tokens=*" %%a in ('echo prompt $E^| cmd') do set "ESC=%%a"
set "CYAN=%ESC%[36m"
set "GREEN=%ESC%[32m"
set "RED=%ESC%[31m"
set "YELLOW=%ESC%[33m"
set "RESET=%ESC%[0m"

:: Auto request admin
net session >nul 2>&1
if !errorlevel! neq 0 (
    echo  Requesting admin privileges...
    powershell -Command "Start-Process '%~f0' -Verb RunAs"
    exit /b
)

echo.
echo %CYAN%  Chocolatey Uninstall%RESET%
echo  -------------------------------------------------------------------------------
echo.

:: Check if choco installed
where choco >nul 2>&1
if !errorlevel! neq 0 (
    echo  %CYAN%[INFO]%RESET%  Chocolatey is not installed.
    echo.
    
    exit /b 0
)

echo  %YELLOW%[WARN]%RESET%  Removing Chocolatey...
echo.
powershell -NoProfile -ExecutionPolicy Bypass -Command "Remove-Item -Recurse -Force \"$env:ChocolateyInstall\""
if !errorlevel! neq 0 (
    echo.
    echo  %RED%[ERROR]%RESET% Uninstall failed.
    echo.
    
    exit /b 1
)

:: Clean up environment variable
powershell -NoProfile -ExecutionPolicy Bypass -Command "[System.Environment]::SetEnvironmentVariable('ChocolateyInstall', $null, 'Machine')"

echo.
echo  %GREEN%[OK]%RESET%    Chocolatey uninstalled successfully.
echo.
echo  -------------------------------------------------------------------------------
echo.

endlocal