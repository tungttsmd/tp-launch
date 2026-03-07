@echo off
chcp 65001 >nul
setlocal

set "SVC_EXE=%~dp0WinAtSvc.exe"
set "SVC_NAME=WinAtSvc"

echo [1/3] Stop service (bo qua neu chua ton tai)...
"%SVC_EXE%" stop >nul 2>&1

echo [2/3] Uninstall service cu (bo qua neu chua ton tai)...
"%SVC_EXE%" uninstall >nul 2>&1

echo [3/3] Install va start service...
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
