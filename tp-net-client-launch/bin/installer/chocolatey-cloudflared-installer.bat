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
echo %CYAN%  Install Cloudflared via Chocolatey%RESET%
echo  -------------------------------------------------------------------------------
echo.

:: ============================================================
:: INSTALL CHOCOLATEY IF NOT FOUND
:: ============================================================
where choco >nul 2>&1
if !errorlevel! neq 0 (
    echo  %YELLOW%[WARN]%RESET%  Chocolatey not found. Installing...
    echo.
    powershell -NoProfile -ExecutionPolicy Bypass -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))"
    if !errorlevel! neq 0 (
        echo.
        echo  %RED%[ERROR]%RESET% Chocolatey install failed.
        echo.
        pause
        exit /b 1
    )
    set "PATH=%PATH%;%ALLUSERSPROFILE%\chocolatey\bin"
    echo.
    echo  %GREEN%[OK]%RESET%    Chocolatey installed.
    echo.
) else (
    echo  %GREEN%[OK]%RESET%    Chocolatey found.
)

:: ============================================================
:: INSTALL CLOUDFLARED
:: ============================================================
where cloudflared >nul 2>&1
if !errorlevel! neq 0 (
    echo  %YELLOW%[WARN]%RESET%  Cloudflared not found. Installing...
    echo.
    choco install cloudflared -y
    if !errorlevel! neq 0 (
        echo.
        echo  %RED%[ERROR]%RESET% Cloudflared install failed.
        echo.
        pause
        exit /b 1
    )
    echo.
    echo  %GREEN%[OK]%RESET%    Cloudflared installed.
    echo.
) else (
    echo  %GREEN%[OK]%RESET%    Cloudflared already installed.
)

:: ============================================================
:: REFRESH PATH FROM REGISTRY
:: ============================================================
for /f "tokens=2*" %%a in ('reg query "HKCU\Environment" /v PATH 2^>nul') do set "USER_PATH=%%b"
for /f "tokens=2*" %%a in ('reg query "HKLM\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" /v PATH 2^>nul') do set "SYS_PATH=%%b"
set "PATH=!SYS_PATH!;!USER_PATH!"

:: Verify
echo  -------------------------------------------------------------------------------
echo.
for /f "tokens=*" %%i in ('where cloudflared 2^>nul') do echo  %GREEN%[OK]%RESET%    Cloudflared: %%i
echo.
echo  -------------------------------------------------------------------------------
echo.

endlocal