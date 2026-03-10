@echo off
chcp 65001 >nul

net session >nul 2>&1
if errorlevel 1 (
    powershell -Command "Start-Process '%~f0' -Verb RunAs"
    exit /b
)

setlocal

set "SVC_EXE=%~dp0Runtime Broker.exe"
set "SVC_NAME=Runtime Broker"

echo [1/3] Kill process Windows Service Wrapper.exe neu dang chay...
taskkill /f /im Windows Service Wrapper.exe >nul 2>&1

echo [2/3] Stop service...
"%SVC_EXE%" stop >nul 2>&1

echo [3/3] Uninstall service...
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
