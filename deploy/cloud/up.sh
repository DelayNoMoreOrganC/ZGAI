#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${1:-${SCRIPT_DIR}/.env.cloud}"

"${SCRIPT_DIR}/validate-config.sh" "$ENV_FILE" --runtime
docker compose --env-file "$ENV_FILE" -f "${SCRIPT_DIR}/docker-compose.yml" config --quiet
docker compose --env-file "$ENV_FILE" -f "${SCRIPT_DIR}/docker-compose.yml" up -d --build
docker compose --env-file "$ENV_FILE" -f "${SCRIPT_DIR}/docker-compose.yml" ps
