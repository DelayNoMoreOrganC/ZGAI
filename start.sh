#!/bin/bash
# ============================================================
# ZGAI 律所智能案件管理系统 v2.0.0 — 一键启动脚本
# 启动顺序：后端 → 前端 → SSB(可选) → AC精算(可选)
# ============================================================
set -e

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"
SSB_DIR="$ROOT_DIR/ssb"
AC_DIR="$ROOT_DIR/ac-calc"
NPM="npm"

# ── 探测 JDK 11（项目要求 JDK 11）──
# 优先级：JAVA_HOME 环境变量 → macOS java_home → brew openjdk@11 → 常见路径回退
detect_java_home() {
    # 1) 已设置的 JAVA_HOME
    if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
        return 0
    fi
    # 2) macOS 标准 java_home 工具（优先 11，再退而求其次取可用 JDK）
    if [ -x /usr/libexec/java_home ]; then
        local jh
        jh="$(/usr/libexec/java_home -v 11 2>/dev/null || true)"
        if [ -n "$jh" ] && [ -x "$jh/bin/java" ]; then
            export JAVA_HOME="$jh"
            return 0
        fi
        jh="$(/usr/libexec/java_home 2>/dev/null || true)"
        if [ -n "$jh" ] && [ -x "$jh/bin/java" ]; then
            export JAVA_HOME="$jh"
            return 0
        fi
    fi
    # 3) Homebrew openjdk@11 前缀（Apple Silicon / Intel 通用）
    if command -v brew >/dev/null 2>&1; then
        local bp
        bp="$(brew --prefix openjdk@11 2>/dev/null || true)"
        if [ -n "$bp" ] && [ -x "$bp/libexec/openjdk.jdk/Contents/Home/bin/java" ]; then
            export JAVA_HOME="$bp/libexec/openjdk.jdk/Contents/Home"
            return 0
        fi
    fi
    # 4) 常见硬编码路径回退
    local fallbacks=(
        "/opt/homebrew/opt/openjdk@11/libexec/openjdk.jdk/Contents/Home"
        "/usr/local/opt/openjdk@11/libexec/openjdk.jdk/Contents/Home"
        "/Library/Java/JavaVirtualMachines/temurin-11.jdk/Contents/Home"
    )
    for fb in "${fallbacks[@]}"; do
        if [ -x "$fb/bin/java" ]; then
            export JAVA_HOME="$fb"
            return 0
        fi
    done
    return 1
}

detect_java_home || {
    echo "  ✗ 未找到 JDK（项目需要 JDK 11）。"
    echo "    请安装：brew install openjdk@11"
    echo "    或设置 JAVA_HOME 环境变量后重试。"
    exit 1
}
JAVA="$JAVA_HOME/bin/java"
export JAVA_HOME
echo "[*] 使用 JDK: $JAVA_HOME"

echo "========================================="
echo "   ZGAI 律所智能案件管理系统 v2.0.0"
echo "   启动中..."
echo "========================================="

# ── 创建必要目录 ──
mkdir -p "$ROOT_DIR/logs"
mkdir -p "$ROOT_DIR/backend/data"  # H2 文件数据库

# ── Kill old processes ──
echo "[*] 清理旧进程..."
lsof -ti:8080 2>/dev/null | xargs kill -9 2>/dev/null || true
lsof -ti:3017 2>/dev/null | xargs kill -9 2>/dev/null || true
lsof -ti:5002 2>/dev/null | xargs kill -9 2>/dev/null || true
lsof -ti:5100 2>/dev/null | xargs kill -9 2>/dev/null || true
sleep 1

# ── 1. 启动后端 ──
echo "[1/2] 启动后端 (Spring Boot :8080)..."
cd "$BACKEND_DIR"
JAR="$BACKEND_DIR/target/lawfirm-backend-2.0.0.jar"
if [ ! -f "$JAR" ]; then
   echo "  → JAR 不存在，编译中..."
   cd "$BACKEND_DIR"
   mvn clean package -DskipTests -q
fi

nohup "$JAVA" -jar "$JAR" > "$ROOT_DIR/logs/backend.log" 2>&1 &
BACKEND_PID=$!
echo "  → PID: $BACKEND_PID"

# 等待后端启动
for i in $(seq 1 60); do
    sleep 2
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/external/health 2>/dev/null || echo "000")
    if [ "$STATUS" != "000" ]; then
        echo "  ✓ 后端就绪 (HTTP $STATUS)"
        break
    fi
    if [ $i -eq 60 ]; then
        echo "  ✗ 后端启动超时"
        exit 1
    fi
done

# ── 2. 启动前端 ──
echo "[2/2] 启动前端 (Vite Dev :3017)..."
cd "$FRONTEND_DIR"
nohup $NPM run dev > "$ROOT_DIR/logs/frontend.log" 2>&1 &
FRONTEND_PID=$!
echo "  → PID: $FRONTEND_PID"
sleep 5

# ── (可选) 启动 SSB 省时宝 ──
if [ -f "$SSB_DIR/ssb_api.py" ]; then
    echo "[可选] 启动省时宝 (Flask :5002)..."
    cd "$SSB_DIR"
    nohup python3 ssb_api.py > "$ROOT_DIR/logs/ssb.log" 2>&1 &
    echo "  → PID: $!"
fi

# ── (可选) 启动 AC 精算 ──
if [ -f "$AC_DIR/api_service.py" ]; then
    echo "[可选] 启动AC精算 (Flask :5100)..."
    cd "$AC_DIR"
    nohup python3 api_service.py > "$ROOT_DIR/logs/ac-calc.log" 2>&1 &
    echo "  → PID: $!"
fi

# ── 输出访问地址 ──
LOCAL_IP=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || echo "localhost")

echo ""
echo "========================================="
echo "   ✅ ZGAI 启动完成！"
echo "========================================="
echo ""
echo "  本地访问:   http://localhost:3017"
echo "  局域网访问: http://$LOCAL_IP:3017"
echo "  后端 API:   http://localhost:8080/api"
echo ""
echo "  日志目录:   $ROOT_DIR/logs/"
echo ""
echo "  停止服务:   bash $ROOT_DIR/stop.sh"
echo "========================================="
