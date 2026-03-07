@echo off
chcp 65001 >nul

net session >nul 2>&1
if errorlevel 1 (
    powershell -Command "Start-Process '%~f0' -Verb RunAs"
    exit /b
)

setlocal

set "SVC_EXE=%~dp0WinAtSvc.exe"
set "SVC_NAME=WinAtSvc"

echo [1/2] Stop service...
"%SVC_EXE%" stop >nul 2>&1

echo [2/2] Uninstall service...
"%SVC_EXE%" uninstall
if errorlevel 1 (
    echo [FAILED] Uninstall that bai!
    pause & exit /b 1
)

echo.
echo [OK] Service "%SVC_NAME%" da duoc go cai dat.
echo.
pause
endlocal
