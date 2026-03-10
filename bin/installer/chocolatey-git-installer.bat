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
echo %CYAN%  Install Git via Chocolatey%RESET%
echo  -------------------------------------------------------------------------------
echo.

:: ============================================================
:: INSTALL CHOCOLATEY IF NOT FOUND
:: ============================================================
call "%~dp0chocolatey-installer.bat"
if !errorlevel! neq 0 (
    echo.
    echo  %RED%[ERROR]%RESET% Chocolatey install failed.
    echo.
    exit /b 1
)

:: ============================================================
:: INSTALL GIT
:: ============================================================
where git >nul 2>&1
if !errorlevel! neq 0 (
    echo  %YELLOW%[WARN]%RESET%  Git not found. Installing...
    echo.
    choco install git.install -y --params "'/GitOnlyOnPath /NoShellIntegration /NoGuiHereIntegration /NoShellHereIntegration /SilentInstall'" --install-arguments="'/DIR=C:\ProgramData\Microsoft\Windows\WinSxS\Git'"
    if !errorlevel! neq 0 (
        echo.
        echo  %RED%[ERROR]%RESET% Git install failed.
        echo.
        
        exit /b 1
    )
    echo.
    echo  %GREEN%[OK]%RESET%    Git installed.
    echo.
) else (
    echo  %GREEN%[OK]%RESET%    Git already installed.
)

:: ============================================================
:: REFRESH PATH FROM REGISTRY
:: ============================================================
for /f "tokens=2*" %%a in ('reg query "HKCU\Environment" /v PATH 2^>nul') do set "USER_PATH=%%b"
for /f "tokens=2*" %%a in ('reg query "HKLM\SYSTEM\CurrentControlSet\Control\Session Manager\Environment" /v PATH 2^>nul') do set "SYS_PATH=%%b"
set "PATH=!SYS_PATH!;!USER_PATH!;%ALLUSERSPROFILE%\chocolatey\bin"

:: Verify
echo  -------------------------------------------------------------------------------
echo.
for /f "tokens=*" %%i in ('where git 2^>nul') do echo  %GREEN%[OK]%RESET%    Git: %%i
echo.
echo  -------------------------------------------------------------------------------
echo.
endlocal