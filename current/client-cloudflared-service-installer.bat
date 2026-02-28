@echo off

REM Đặt mã hóa thành UTF-8 để hỗ trợ tiếng Việt
chcp 65001

cls
echo.
echo ========= ^*^*^*^*^*^*^*^*^*^*^*^*^*^*^*^*^*^*^*^*^*^*^* Cài đặt Cloudflared.exe bằng winget ^*^*^*^*^*^*^*^*^*^*^*^*^*^*^*^*^*^*^*^*^*^*^*^*^ =========
echo.
echo [1/3] Kiểm tra tồn tại cloudflared.exe
echo ...
REM Kiểm tra xem cloudflared đã được cài đặt chưa
where cloudflared >nul 2>nul
if %errorlevel% equ 0 (
    echo [CHECK] cloudflared.exe đã tồn tại, tiến hành làm sạch, gỡ cài đặt... ^(dự kiến 2 - 3 phút^)
    winget uninstall Cloudflare.cloudflared
    if %errorlevel% neq 0 (
        echo [ERROR] Lỗi khi gỡ cài đặt cloudflared. Vui lòng kiểm tra lại.
        exit /b
    )
    echo [DONE] Đã gỡ cài đặt cloudflared thành công.
)
echo.
echo [2/3] Đang cài đặt cloudflared.exe (dự kiến 2 - 5 phút)
echo ...
REM Tiến hành cài đặt cloudflared
winget install --id Cloudflare.cloudflared

REM Kiểm tra xem cài đặt có thành công không
if %errorlevel% neq 0 (
    echo [ERROR] Cài đặt không thành công, vui lòng kiểm tra kết nối mạng hoặc thử lại.
    exit /b
)

REM Đề xuất mở lại cửa sổ CMD
echo.
echo [====== Cài đặt thành công! ======]
echo.
echo [INFO] Bạn cần mở CMD mới để PATH được cập nhật trên cmd đó.
echo [INFO] - Kiểm tra cài đặt thành công: cloudflared -v
echo [INFO] - Kiểm tra đường dẫn của file: where cloudflared
echo [INFO] - Tạo đường hầm https cho dịch vụ tại cổng ^<port^>: cloudflared tunne --url=^<host^>:^<port^>
echo [INFO] - Tạo đường hầm tcp cho dịch vụ tại cổng ^<port^>: cloudflared tunnel tcp --url=^<host^>:^<port^>

REM Kiểm tra lại cloudflared có trong PATH hay không sau khi cài đặt
echo.
echo [3/4] Đang kiểm tra lại cloudflared.exe...
echo ...
where cloudflared >nul 2>nul
if %errorlevel% equ 0 (
    echo [CHECK] cloudflared đã được cài đặt thành công
) else (
    echo [ERROR] cloudflared không được cài đặt đúng cách hoặc không có trong PATH. Bạn có thể thử mở lại cửa sổ CMD hoặc kiểm tra lại quá trình cài đặt.
)
echo.
echo [4/4] Cài đặt hoàn tất.

REM Thêm dòng thông báo trước khi pause
echo.
echo Nhấn phím bất kỳ để thoát...
pause
