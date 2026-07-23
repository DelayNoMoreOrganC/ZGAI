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
SSB_REPO_DIR="$ROOT_DIR/ssb-repo"
SSB_CLIENT_DIR="$SSB_REPO_DIR/client"
AC_DIR="$ROOT_DIR/ac-calc"
NPM="npm"
DB_MODE="${ZGAI_DB:-h2}"
EXTERNAL_TOOLS_ENABLED="${VITE_ENABLE_EXTERNAL_TOOLS:-false}"
LOCAL_IP=$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || echo "localhost")

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

wait_for_http() {
    local label="$1"
    local url="$2"
    local attempts="${3:-30}"
    local status="000"
    local i
    for i in $(seq 1 "$attempts"); do
        status="$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null)" || status="000"
        if [ "$status" = "200" ]; then
            echo "  ✓ $label 就绪 (HTTP $status)"
            return 0
        fi
        sleep 1
    done
    echo "  ! $label 未就绪，详见 logs 目录"
    return 1
}

# ── 数据库模式 ──
if [ "$DB_MODE" = "postgres" ]; then
    PG_BIN="${PG_BIN:-/opt/homebrew/opt/postgresql@16/bin}"
    if [ -d "$PG_BIN" ]; then
        export PATH="$PG_BIN:$PATH"
        export PG_DUMP_PATH="${PG_DUMP_PATH:-$PG_BIN/pg_dump}"
    fi
    if ! command -v pg_isready >/dev/null 2>&1; then
        echo "  ✗ PostgreSQL 客户端不可用，请先安装 postgresql@16"
        exit 1
    fi
    if ! pg_isready \
        -h "${POSTGRES_HOST:-localhost}" \
        -p "${POSTGRES_PORT:-5432}" \
        -d "${POSTGRES_DB:-zgai}" \
        -U "${POSTGRES_USER:-zgai}" >/dev/null 2>&1; then
        echo "  ✗ PostgreSQL 尚未就绪，请先启动服务并创建 zgai 数据库"
        exit 1
    fi
    export SPRING_PROFILES_ACTIVE=postgres
    echo "[*] 数据库模式: PostgreSQL"
elif [ "$DB_MODE" = "h2" ]; then
    unset SPRING_PROFILES_ACTIVE
    echo "[*] 数据库模式: H2（本地开发）"
else
    echo "  ✗ 不支持的 ZGAI_DB=$DB_MODE，可选值为 h2 或 postgres"
    exit 1
fi

# ── Secrets ──
generate_secret() {
    openssl rand -hex 32
}

if [ "$DB_MODE" = "postgres" ]; then
    for required_var in POSTGRES_PASSWORD INITIAL_ADMIN_PASSWORD JWT_SECRET CRYPTO_SECRET_KEY; do
        if [ -z "${!required_var:-}" ]; then
            echo "  ✗ PostgreSQL 模式必须设置 $required_var"
            exit 1
        fi
    done
else
    DEV_SECRETS_FILE="$BACKEND_DIR/data/.dev-secrets"
    if [ -f "$DEV_SECRETS_FILE" ]; then
        set -a
        # shellcheck disable=SC1090
        source "$DEV_SECRETS_FILE"
        set +a
    else
        export JWT_SECRET="${JWT_SECRET:-$(generate_secret)}"
        export CRYPTO_SECRET_KEY="${CRYPTO_SECRET_KEY:-$(generate_secret)}"
        export INITIAL_ADMIN_PASSWORD="${INITIAL_ADMIN_PASSWORD:-$(openssl rand -base64 18 | tr -d '/+=')}"
        umask 077
        {
            printf 'JWT_SECRET=%s\n' "$JWT_SECRET"
            printf 'CRYPTO_SECRET_KEY=%s\n' "$CRYPTO_SECRET_KEY"
            printf 'INITIAL_ADMIN_PASSWORD=%s\n' "$INITIAL_ADMIN_PASSWORD"
        } > "$DEV_SECRETS_FILE"
        echo "[*] 已生成本机开发密钥: $DEV_SECRETS_FILE"
        echo "[*] 新数据库初始管理员密码: $INITIAL_ADMIN_PASSWORD"
        echo "    仅数据库为空时生效，请首次登录后立即修改。"
    fi
fi

# ── 创建必要目录 ──
mkdir -p "$ROOT_DIR/logs"
if [ "$DB_MODE" = "h2" ]; then
    mkdir -p "$ROOT_DIR/backend/data"
fi

# ── Kill old processes ──
echo "[*] 清理旧进程..."
lsof -ti:8080 2>/dev/null | xargs kill -9 2>/dev/null || true
lsof -ti:3017 2>/dev/null | xargs kill -9 2>/dev/null || true
lsof -ti:3000 2>/dev/null | xargs kill -9 2>/dev/null || true
lsof -ti:5000 2>/dev/null | xargs kill -9 2>/dev/null || true
lsof -ti:5002 2>/dev/null | xargs kill -9 2>/dev/null || true
lsof -ti:5100 2>/dev/null | xargs kill -9 2>/dev/null || true
sleep 1

# ── 1. 启动后端 ──
echo "[1/2] 启动后端 (Spring Boot :8080)..."
cd "$BACKEND_DIR"
JAR="$BACKEND_DIR/target/lawfirm-backend-2.0.0.jar"
if [ ! -f "$JAR" ] || find "$BACKEND_DIR/src" "$BACKEND_DIR/pom.xml" -newer "$JAR" -print -quit | grep -q .; then
   echo "  → 后端源码有更新，编译中..."
   cd "$BACKEND_DIR"
   mvn package -DskipTests -q
fi

nohup "$JAVA" -jar "$JAR" > "$ROOT_DIR/logs/backend.log" 2>&1 &
BACKEND_PID=$!
echo "  → PID: $BACKEND_PID"

# 等待后端启动
for i in $(seq 1 60); do
    sleep 2
    STATUS="$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/health 2>/dev/null)" || STATUS="000"
    if [ "$STATUS" = "200" ]; then
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

# ── (可选) 启动 SSB 省时宝与 AC 精算 ──
if [ "$EXTERNAL_TOOLS_ENABLED" = "true" ] && [ -f "$SSB_DIR/ssb_api.py" ]; then
    echo "[可选] 启动省时宝 ZGAI 代理 (Flask :5002)..."
    cd "$SSB_DIR"
    nohup python3 ssb_api.py > "$ROOT_DIR/logs/ssb.log" 2>&1 &
    echo "  → PID: $!"
    wait_for_http "省时宝 ZGAI 代理" "http://127.0.0.1:5002/api/health" 20 || true
fi

if [ "$EXTERNAL_TOOLS_ENABLED" = "true" ] && [ -f "$SSB_REPO_DIR/server/app_new.py" ]; then
    echo "[可选] 启动独立省时宝后端 (Flask :5000)..."
    cd "$SSB_REPO_DIR/server"
    nohup python3 -m flask --app app_new:app run --host=0.0.0.0 --port=5000 > "$ROOT_DIR/logs/ssb-standalone-backend.log" 2>&1 &
    echo "  → PID: $!"
    wait_for_http "独立省时宝后端" "http://127.0.0.1:5000/api/health" 30 || true
fi

if [ "$EXTERNAL_TOOLS_ENABLED" = "true" ] && [ -f "$SSB_CLIENT_DIR/package.json" ] && [ -d "$SSB_CLIENT_DIR/node_modules" ]; then
    echo "[可选] 启动独立省时宝前端 (Vite :3000)..."
    cd "$SSB_CLIENT_DIR"
    nohup env VITE_API_BASE_URL="http://$LOCAL_IP:5000/api" $NPM run dev -- --host 0.0.0.0 --strictPort > "$ROOT_DIR/logs/ssb-standalone-frontend.log" 2>&1 &
    echo "  → PID: $!"
    wait_for_http "独立省时宝前端" "http://127.0.0.1:3000" 30 || true
fi

# ── (可选) 启动 AC 精算 ──
if [ "$EXTERNAL_TOOLS_ENABLED" = "true" ] && [ -f "$AC_DIR/api_service.py" ]; then
    echo "[可选] 启动AC精算 (Flask :5100)..."
    cd "$AC_DIR"
    nohup python3 api_service.py > "$ROOT_DIR/logs/ac-calc.log" 2>&1 &
    echo "  → PID: $!"
fi

# ── 输出访问地址 ──
echo ""
echo "========================================="
echo "   ✅ ZGAI 启动完成！"
echo "========================================="
echo ""
echo "  本地访问:   http://localhost:3017"
echo "  局域网访问: http://$LOCAL_IP:3017"
echo "  后端 API:   http://localhost:8080/api"
if [ "$EXTERNAL_TOOLS_ENABLED" = "true" ] && [ -f "$SSB_CLIENT_DIR/package.json" ]; then
    echo "  省时宝:     http://$LOCAL_IP:3000"
fi
echo "  数据库:     $DB_MODE"
echo ""
echo "  日志目录:   $ROOT_DIR/logs/"
echo ""
echo "  停止服务:   bash $ROOT_DIR/stop.sh"
echo "========================================="
