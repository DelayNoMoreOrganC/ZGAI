#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ENV_FILE="${1:-${SCRIPT_DIR}/.env.demo}"

"${SCRIPT_DIR}/validate-config.sh" "$ENV_FILE" --runtime

set -a
if [[ -f "${ROOT_DIR}/backend/data/.dev-secrets" ]]; then
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/backend/data/.dev-secrets"
fi
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

export POSTGRES_URL="jdbc:postgresql://127.0.0.1:${POSTGRES_PORT:-5432}/${POSTGRES_DB:-zgai}"
export PG_DUMP_PATH="${PG_DUMP_PATH:-$(command -v pg_dump || true)}"
export PG_RESTORE_PATH="${PG_RESTORE_PATH:-$(command -v pg_restore || true)}"
if [[ -z "$PG_DUMP_PATH" ]]; then
  echo "未安装 pg_dump，Demo 不允许在没有数据库备份能力时启动。" >&2
  exit 2
fi
if [[ -z "$PG_RESTORE_PATH" ]]; then
  echo "未安装 pg_restore，Demo 不允许在无法校验备份时启动。" >&2
  exit 2
fi

cd "$ROOT_DIR"
ZGAI_DB=postgres ./start.sh
