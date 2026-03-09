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
echo %CYAN%  Uninstall Cloudflared via Chocolatey%RESET%
echo  -------------------------------------------------------------------------------
echo.

:: ============================================================
:: CHECK CHOCOLATEY
:: ============================================================
where choco >nul 2>&1
if !errorlevel! neq 0 (
    echo  %RED%[ERROR]%RESET% Chocolatey not found. Cannot uninstall.
    echo.
    pause
    exit /b 1
) else (
    echo  %GREEN%[OK]%RESET%    Chocolatey found.
)

:: ============================================================
:: UNINSTALL CLOUDFLARED
:: ============================================================
where cloudflared >nul 2>&1
if !errorlevel! neq 0 (
    echo  %CYAN%[INFO]%RESET%  Cloudflared is not installed.
    echo.
    pause
    exit /b 0
)

echo  %YELLOW%[WARN]%RESET%  Removing Cloudflared...
echo.
choco uninstall cloudflared -y
if !errorlevel! neq 0 (
    echo.
    echo  %RED%[ERROR]%RESET% Uninstall failed.
    echo.
    pause
    exit /b 1
)

echo.
echo  %GREEN%[OK]%RESET%    Cloudflared uninstalled successfully.
echo.
echo  -------------------------------------------------------------------------------
echo.

endlocal