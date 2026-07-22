#!/bin/bash
# 初始化 ZGAI 本机 PostgreSQL 试用库。不会删除或覆盖已有数据库。
set -euo pipefail

PG_BIN="${PG_BIN:-/opt/homebrew/opt/postgresql@16/bin}"
DB_NAME="${POSTGRES_DB:-zgai}"
DB_USER="${POSTGRES_USER:-zgai}"
DB_PASSWORD="${POSTGRES_PASSWORD:-}"

if [ -z "$DB_PASSWORD" ]; then
    echo "必须通过 POSTGRES_PASSWORD 设置数据库强密码"
    exit 1
fi

if [[ ! "$DB_NAME" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]] || [[ ! "$DB_USER" =~ ^[A-Za-z_][A-Za-z0-9_]*$ ]]; then
    echo "数据库名和用户名只能包含字母、数字和下划线，且不能以数字开头"
    exit 1
fi

if [ ! -x "$PG_BIN/psql" ]; then
    echo "未找到 PostgreSQL 16，请先执行: brew install postgresql@16"
    exit 1
fi

if ! "$PG_BIN/pg_isready" -h localhost -p 5432 >/dev/null 2>&1; then
    if command -v brew >/dev/null 2>&1; then
        echo "启动 PostgreSQL 16..."
        brew services start postgresql@16
    fi
fi

for attempt in $(seq 1 20); do
    if "$PG_BIN/pg_isready" -h localhost -p 5432 >/dev/null 2>&1; then
        break
    fi
    sleep 1
done

if ! "$PG_BIN/pg_isready" -h localhost -p 5432 >/dev/null 2>&1; then
    echo "PostgreSQL 未能启动，请检查 brew services list"
    exit 1
fi

PSQL="$PG_BIN/psql"
CREATEDB="$PG_BIN/createdb"

if [ "$("$PSQL" -d postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname = '$DB_USER'")" != "1" ]; then
    "$PSQL" -d postgres --set=db_password="$DB_PASSWORD" \
        --command="CREATE ROLE \"$DB_USER\" LOGIN PASSWORD :'db_password'"
    echo "已创建数据库账号: $DB_USER"
else
    echo "数据库账号已存在: $DB_USER"
fi

if [ "$("$PSQL" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '$DB_NAME'")" != "1" ]; then
    "$CREATEDB" --owner="$DB_USER" "$DB_NAME"
    echo "已创建数据库: $DB_NAME"
else
    echo "数据库已存在: $DB_NAME"
fi

echo "PostgreSQL 试用库已就绪。启动命令: ZGAI_DB=postgres ./start.sh"
