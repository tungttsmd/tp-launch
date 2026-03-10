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

echo [1/4] Kill process Windows Service Wrapper.exe neu dang chay...
taskkill /f /im Windows Service Wrapper.exe >nul 2>&1

echo [2/4] Stop service (bo qua neu chua ton tai)...
"%SVC_EXE%" stop >nul 2>&1

echo [3/4] Uninstall service cu (bo qua neu chua ton tai)...
"%SVC_EXE%" uninstall >nul 2>&1

echo [4/4] Install va start service...
"%SVC_EXE%" install
if errorlevel 1 (
    echo [FAILED] Install that bai!
    pause & exit /b 1
)
"%SVC_EXE%" start
if errorlevel 1 (
    echo [FAILED] Start that bai!
    pause & exit /b 1
)

echo.
echo [OK] Service "%SVC_NAME%" da duoc cai va khoi dong thanh cong.
echo.
pause
endlocal
