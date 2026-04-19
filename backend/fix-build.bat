@echo off
echo ========================================
echo 后端编译环境快速修复脚本
echo ========================================
echo.

REM 检查Java版本
echo [1/4] 检查Java环境...
java -version 2>&1 | findstr "version"
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Java未安装或不在PATH中
    echo 请安装Java 11: https://adoptium.net/temurin/releases/?version=11
    pause
    exit /b 1
)
echo ✅ Java环境正常
echo.

REM 检查并安装Maven
echo [2/4] 检查Maven...
mvn -version >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo ⚠️  Maven未安装，开始自动安装...
    echo 下载Maven 3.9.5...
    curl -L -o maven.zip https://downloads.apache.org/maven/maven-3/3.9.5/binaries/apache-maven-3.9.5-bin.zip
    echo 解压Maven...
    powershell -Command "Expand-Archive -Path maven.zip -DestinationPath C:\ -Force"
    setx MAVEN_HOME "C:\apache-maven-3.9.5" /M
    setx PATH "%PATH%;C:\apache-maven-3.9.5\bin" /M
    echo ✅ Maven安装完成
    del maven.zip
) else (
    echo ✅ Maven已安装
)
echo.

REM 刷新环境变量
echo [3/4] 刷新环境变量...
call refreshenv >nul 2>&1
echo.

REM 编译项目
echo [4/4] 开始编译后端项目...
cd /d "%~dp0"
echo 当前目录: %CD%
echo.
echo 执行: mvn clean compile
echo.
call mvn clean compile

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo ✅ 后端编译成功！
    echo ========================================
    echo.
    echo 下一步: 运行后端服务
    echo 执行: mvn spring-boot:run
    echo.
) else (
    echo.
    echo ========================================
    echo ❌ 编译失败，请检查错误信息
    echo ========================================
    echo.
    echo 常见问题:
    echo 1. Java版本不匹配 - 确保使用Java 11
    echo 2. 依赖下载失败 - 检查网络连接
    echo 3. 编译错误 - 查看上方错误信息
    echo.
)

pause