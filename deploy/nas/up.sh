#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${1:-${SCRIPT_DIR}/.env.nas}"

"${SCRIPT_DIR}/validate-config.sh" "$ENV_FILE"

if ! command -v docker >/dev/null 2>&1 || ! docker compose version >/dev/null 2>&1; then
  echo "需要 Docker Compose v2。" >&2
  exit 2
fi

docker compose --env-file "$ENV_FILE" -f "${SCRIPT_DIR}/docker-compose.yml" config --quiet
docker compose --env-file "$ENV_FILE" -f "${SCRIPT_DIR}/docker-compose.yml" up -d --build
docker compose --env-file "$ENV_FILE" -f "${SCRIPT_DIR}/docker-compose.yml" ps
