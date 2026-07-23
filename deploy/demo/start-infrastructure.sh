#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${1:-${SCRIPT_DIR}/.env.demo}"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.infrastructure.yml"

"${SCRIPT_DIR}/validate-config.sh" "$ENV_FILE" --preflight

if ! command -v docker >/dev/null 2>&1 || ! docker compose version >/dev/null 2>&1; then
  echo "Mac 尚未安装或启动 Docker Desktop，无法启动 PostgreSQL/Qdrant。" >&2
  exit 2
fi

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" config --quiet
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps

"${SCRIPT_DIR}/validate-config.sh" "$ENV_FILE" --runtime
