@echo off
setlocal ENABLEDELAYEDEXPANSION

echo ===============================
echo ACCESS MQTT TCP VIA CLOUDFLARE
echo ===============================
echo.

set HOSTNAME=mqtt.tungsmd.cloud
set LOCAL_PORT=1881

echo Target hostname: %HOSTNAME%
echo Local bind port : %LOCAL_PORT%
echo.

cloudflared access tcp ^
  --hostname %HOSTNAME% ^
  --url tcp://localhost:%LOCAL_PORT%

echo.
echo TCP access stopped
pause
