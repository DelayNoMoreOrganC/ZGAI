#!/bin/bash
# ============================================================
# ZGAI 律所智能案件管理系统 — 生产启动脚本
# 使用 Nginx + Spring Boot 后端
# 适用: macOS 局域网多人部署
# ============================================================
set -e

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"
JAVA_HOME="/opt/homebrew/opt/openjdk@11"
JAVA="$JAVA_HOME/bin/java"

echo "========================================="
echo "   ZGAI 生产模式 — 启动中"
echo "========================================="

# ── 创建目录 ──
mkdir -p "$ROOT_DIR/logs"
mkdir -p "$ROOT_DIR/backend/data"

# ── 停止旧进程 ──
echo "[*] 清理旧进程..."
lsof -ti:8080 2>/dev/null | xargs kill -9 2>/dev/null || true
sleep 1

# ── 1. 构建前端（如需更新） ──
echo "[1/3] 构建前端..."
cd "$FRONTEND_DIR"
npx vite build 2>&1 | tail -3
echo "  → dist/ 就绪"

# ── 2. 启动后端 ──
echo "[2/3] 启动后端 (Spring Boot :8080)..."
cd "$BACKEND_DIR"
JAR="$BACKEND_DIR/target/lawfirm-backend-2.0.0.jar"

if [ ! -f "$JAR" ]; then
    echo "  → JAR 不存在，编译中..."
    export JAVA_HOME="$JAVA_HOME"
    mvn clean package -DskipTests -q
fi

export JAVA_HOME="$JAVA_HOME"
nohup "$JAVA" -jar "$JAR" > "$ROOT_DIR/logs/backend.log" 2>&1 &
BACKEND_PID=$!
echo "  → PID: $BACKEND_PID"

# 等待后端就绪
for i in $(seq 1 60); do
    sleep 2
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 http://localhost:8080/api/external/health 2>/dev/null || echo "000")
    if [ "$STATUS" != "000" ]; then
        echo "  ✓ 后端就绪"
        break
    fi
    if [ $i -eq 60 ]; then
        echo "  ✗ 后端启动超时"
        exit 1
    fi
done

# ── 3. 验证 Nginx ──
echo "[3/3] 验证 Nginx 配置..."
if command -v nginx &> /dev/null; then
    echo "  → Nginx 已安装"
    nginx -t 2>&1 | head -1 || echo "  ⚠ Nginx 配置检查失败，请手动检查"
    echo "  → 执行: sudo nginx -s reload  (重载配置)"
    echo "  → 执行: sudo nginx  (首次启动)"
else
    echo "  ⚠ Nginx 未安装，安装: brew install nginx"
fi

# ── 输出 ──
LOCAL_IP=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || echo "localhost")

echo ""
echo "========================================="
echo "   ✅ ZGAI 生产模式启动完成！"
echo "========================================="
echo ""
echo "  🖥  本地访问:   http://localhost:80"
echo "  🌐  局域网访问: http://$LOCAL_IP"
echo "  ⚙️  后端 API:   http://localhost:8080/api"
echo ""
echo "  📋  日志:       $ROOT_DIR/logs/backend.log"
echo "  📦  前端:       $ROOT_DIR/frontend/dist/"
echo ""
echo "  🛑  停止:       bash stop.sh"
echo "========================================="
