#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${1:-${SCRIPT_DIR}/.env.cloud}"
DUMP_FILE="${2:-}"

if [[ -z "$DUMP_FILE" ]]; then
  echo "用法: $0 [env文件] <备份dump文件>" >&2
  exit 2
fi

ZGAI_COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yml" \
ZGAI_CONFIG_VALIDATOR="${SCRIPT_DIR}/validate-config.sh" \
  "${SCRIPT_DIR}/../nas/restore-drill.sh" "$ENV_FILE" "$DUMP_FILE"
