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
set "chocolateyVersion=1.4.0"

:: Auto request admin
net session >nul 2>&1
if !errorlevel! neq 0 (
    echo  Requesting admin privileges...
    powershell -Command "Start-Process '%~f0' -Verb RunAs"
    exit /b
)

echo.
echo %CYAN%  Chocolatey Install%RESET%
echo  -------------------------------------------------------------------------------
echo.

:: Check if choco already installed
where choco >nul 2>&1
if !errorlevel! == 0 (
    for /f "tokens=*" %%i in ('choco -v') do set "CHOCO_VER=%%i"
    echo  %GREEN%[OK]%RESET%    Chocolatey already installed. Version: !CHOCO_VER!
    echo.
    
    exit /b 0
)
set "chocolateyVersion=1.4.0"
echo  %YELLOW%[WARN]%RESET%  Chocolatey not found. Installing...
echo.
powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))"
if !errorlevel! neq 0 (
    echo.
    echo  %RED%[ERROR]%RESET% Install failed.
    echo.
    
    exit /b 1
)

echo.
echo  %CYAN%[INFO]%RESET%  Updating PATH...
set "PATH=%PATH%;%ALLUSERSPROFILE%\chocolatey\bin"

choco -v >nul 2>&1
if !errorlevel! == 0 (
    for /f "tokens=*" %%i in ('choco -v') do set "CHOCO_VER=%%i"
    echo  %GREEN%[OK]%RESET%    Chocolatey installed successfully. Version: !CHOCO_VER!
) else (
    echo  %RED%[ERROR]%RESET% Chocolatey installed but not found in PATH.
)

echo.
echo  -------------------------------------------------------------------------------
echo.
endlocal