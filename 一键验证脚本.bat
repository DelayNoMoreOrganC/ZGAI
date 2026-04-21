@echo off
echo ====================================
echo 案件管理按钮问题 - 一键验证脚本
echo ====================================
echo.

echo [步骤1] 检查后端服务...
netstat -ano | findstr ":8080" | findstr "LISTENING" >nul
if %errorlevel% == 0 (
    echo ✓ 后端服务运行中 (8080端口)
) else (
    echo ✗ 后端服务未运行！请先启动后端服务。
    pause
    exit /b 1
)

echo.
echo [步骤2] 检查前端服务...
netstat -ano | findstr ":3017" | findstr "LISTENING" >nul
if %errorlevel% == 0 (
    echo ✓ 前端服务运行中 (3017端口)
) else (
    echo ✗ 前端服务未运行！正在启动...
    cd /d D:\ZGAI\frontend
    start cmd /k "npm run dev"
    echo 等待前端服务启动...
    timeout /t 10 /nobreak >nul
)

echo.
echo [步骤3] 检查源代码修复...
findstr /C:"slot name=\"extra\"" D:\ZGAI\frontend\src\components\PageHeader.vue >nul
if %errorlevel% == 0 (
    echo ✓ PageHeader修复已确认
) else (
    echo ✗ PageHeader修复不存在！需要重新修复。
    pause
    exit /b 1
)

echo.
echo [步骤4] 打开浏览器（无痕模式）...
echo 请在无痕窗口中执行以下操作：
echo 1. 访问 http://localhost:3017
echo 2. 登录：admin / admin123
echo 3. 点击"案件管理" → "新建案件"
echo 4. 查看页面右上角是否有4个按钮
echo.

start chrome --incognito http://localhost:3017
REM 如果使用Edge，取消下面一行的注释
REM start msedge --inprivate http://localhost:3017

echo.
echo ====================================
echo 验证完成！
echo.
echo 如果看到4个按钮：
echo   ✓ 保存草稿
echo   ✓ 确认立案
echo   ✓ 提交案件
echo   ✓ 提交审批
echo 则问题已解决！
echo.
echo 如果仍然看不到按钮：
echo 1. 在浏览器中按 F12 打开开发者工具
echo 2. 切换到 Console 标签
echo 3. 查看是否有红色错误信息
echo 4. 截图并发送给开发团队
echo ====================================
pause
