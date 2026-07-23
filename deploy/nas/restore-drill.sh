#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${1:-${SCRIPT_DIR}/.env.nas}"
DUMP_ARGUMENT="${2:-}"
COMPOSE_FILE="${ZGAI_COMPOSE_FILE:-${SCRIPT_DIR}/docker-compose.yml}"
CONFIG_VALIDATOR="${ZGAI_CONFIG_VALIDATOR:-${SCRIPT_DIR}/validate-config.sh}"

if [[ -z "$DUMP_ARGUMENT" ]]; then
  echo "用法: $0 [env文件] <备份dump文件>" >&2
  exit 2
fi

"$CONFIG_VALIDATOR" "$ENV_FILE"
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

if ! command -v docker >/dev/null 2>&1 || ! docker compose version >/dev/null 2>&1; then
  echo "需要 Docker Compose v2。" >&2
  exit 2
fi

BACKUP_ROOT="$(cd "$ZGAI_BACKUPS" && pwd -P)"
DUMP_DIR="$(cd "$(dirname "$DUMP_ARGUMENT")" && pwd -P)"
DUMP_FILE="$DUMP_DIR/$(basename "$DUMP_ARGUMENT")"
if [[ "$DUMP_DIR" != "$BACKUP_ROOT" || ! -f "$DUMP_FILE" || -L "$DUMP_FILE" || "$DUMP_FILE" == *.part ]]; then
  echo "只能演练备份目录中的正式 dump 文件，且不能使用符号链接或半成品。" >&2
  exit 2
fi

BACKUP_NAME="$(basename "$DUMP_FILE")"
DRILL_DB="zgai_drill_$(date '+%Y%m%d_%H%M%S')_$$"
if [[ "$DRILL_DB" == "${POSTGRES_DB:-zgai}" ]]; then
  echo "演练数据库不得与业务数据库同名。" >&2
  exit 2
fi

DRILL_OUTPUT="$(docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T \
  -e BACKUP_NAME="$BACKUP_NAME" -e DRILL_DB="$DRILL_DB" postgres sh -eu -c '
    cleanup() { dropdb --username="$POSTGRES_USER" --if-exists "$DRILL_DB" >/dev/null 2>&1 || true; }
    trap cleanup EXIT INT TERM
    pg_restore --list "/backups/$BACKUP_NAME" >/dev/null
    createdb --username="$POSTGRES_USER" --template=template0 "$DRILL_DB"
    pg_restore --exit-on-error --username="$POSTGRES_USER" --dbname="$DRILL_DB" \
      --no-owner --no-privileges "/backups/$BACKUP_NAME"
    schema_ok="$(psql --username="$POSTGRES_USER" --dbname="$DRILL_DB" --tuples-only --no-align \
      --command="SELECT to_regclass('\''public.\"user\"'\'') IS NOT NULL
        AND to_regclass('\''public.\"case\"'\'') IS NOT NULL
        AND to_regclass('\''public.client'\'') IS NOT NULL
        AND to_regclass('\''public.approval'\'') IS NOT NULL;")"
    if [ "$schema_ok" != "t" ]; then
      echo "恢复后的核心表校验失败" >&2
      exit 1
    fi
    psql --username="$POSTGRES_USER" --dbname="$DRILL_DB" --tuples-only --no-align \
      --command="SELECT '\''users='\'' || count(*) FROM \"user\"
        UNION ALL SELECT '\''cases='\'' || count(*) FROM \"case\"
        UNION ALL SELECT '\''clients='\'' || count(*) FROM client
        UNION ALL SELECT '\''approvals='\'' || count(*) FROM approval;"
  ')"

umask 077
REPORT_FILE="$ZGAI_BACKUPS/restore_drill_$(date '+%Y%m%d_%H%M%S').txt"
{
  echo "ZGAI PostgreSQL 恢复演练"
  echo "时间: $(date '+%Y-%m-%d %H:%M:%S %z')"
  echo "备份: $BACKUP_NAME"
  echo "结果: PASSED"
  echo "$DRILL_OUTPUT"
} > "$REPORT_FILE"

echo "PostgreSQL 离线恢复演练通过，临时数据库已删除。"
echo "报告: $(basename "$REPORT_FILE")"
