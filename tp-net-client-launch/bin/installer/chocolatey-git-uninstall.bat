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
echo %CYAN%  Uninstall Git via Chocolatey%RESET%
echo  -------------------------------------------------------------------------------
echo.

:: CHECK CHOCOLATEY
where choco >nul 2>&1
if !errorlevel! neq 0 (
    echo  %RED%[ERROR]%RESET% Chocolatey not found. Cannot uninstall.
    echo.
    pause
    exit /b 1
) else (
    echo  %GREEN%[OK]%RESET%    Chocolatey found.
)

echo  %YELLOW%[WARN]%RESET%  Removing Git...
echo.

:: Uninstall metapackage first, then git.install
choco uninstall git -y >nul 2>&1
choco uninstall git.install -y
if !errorlevel! neq 0 (
    echo.
    echo  %RED%[ERROR]%RESET% Uninstall failed.
    echo.
    pause
    exit /b 1
)

:: Clean leftover folders
if exist "C:\Program Files\Git" (
    rmdir /s /q "C:\Program Files\Git"
    echo  %GREEN%[OK]%RESET%    Removed: C:\Program Files\Git
)
if exist "C:\ProgramData\Microsoft\Windows\WinSxS\Git" (
    rmdir /s /q "C:\ProgramData\Microsoft\Windows\WinSxS\Git"
    echo  %GREEN%[OK]%RESET%    Removed: C:\ProgramData\Microsoft\Windows\WinSxS\Git
)

echo.
echo  %GREEN%[OK]%RESET%    Git uninstalled successfully.
echo.
echo  -------------------------------------------------------------------------------
echo.

endlocal