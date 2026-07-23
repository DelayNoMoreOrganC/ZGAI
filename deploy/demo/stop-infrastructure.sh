#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="${1:-$SCRIPT_DIR/.env.demo}"

if [ ! -f "$ENV_FILE" ]; then
    echo "配置文件不存在: $ENV_FILE"
    exit 2
fi

if ! docker compose version >/dev/null 2>&1; then
    echo "Docker Compose v2 不可用，无法停止 Demo 基础设施。"
    exit 2
fi

docker compose \
    --env-file "$ENV_FILE" \
    -f "$SCRIPT_DIR/docker-compose.infrastructure.yml" \
    stop

echo "PostgreSQL 与 Qdrant 已停止，数据卷保留。"
echo "不要执行 docker compose down -v，除非已确认可以销毁 Demo 数据。"
