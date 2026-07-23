#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${1:-${SCRIPT_DIR}/.env.nas}"
COMPOSE_FILE="${ZGAI_COMPOSE_FILE:-${SCRIPT_DIR}/docker-compose.yml}"
CONFIG_VALIDATOR="${ZGAI_CONFIG_VALIDATOR:-${SCRIPT_DIR}/validate-config.sh}"

"$CONFIG_VALIDATOR" "$ENV_FILE"
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

if ! command -v docker >/dev/null 2>&1 || ! docker compose version >/dev/null 2>&1; then
  echo "需要 Docker Compose v2。" >&2
  exit 2
fi

mkdir -p "$ZGAI_BACKUPS"
if [[ ! -d "$ZGAI_BACKUPS" || ! -w "$ZGAI_BACKUPS" ]]; then
  echo "备份目录不可写: $ZGAI_BACKUPS" >&2
  exit 2
fi

LOCK_DIR="$ZGAI_BACKUPS/.postgres-backup.lock"
if ! mkdir "$LOCK_DIR" 2>/dev/null; then
  echo "已有备份任务运行中；若确认没有任务，请移除 ${LOCK_DIR}。" >&2
  exit 1
fi
cleanup_lock() { rmdir "$LOCK_DIR" 2>/dev/null || true; }
trap cleanup_lock EXIT INT TERM

BACKUP_NAME="lawfirm_backup_$(date '+%Y%m%d_%H%M%S').dump"

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" exec -T \
  -e BACKUP_NAME="$BACKUP_NAME" postgres sh -eu -c '
    umask 077
    part="/backups/${BACKUP_NAME}.part"
    final="/backups/${BACKUP_NAME}"
    cleanup() { rm -f "$part"; }
    trap cleanup EXIT INT TERM
    pg_dump --username="$POSTGRES_USER" --dbname="$POSTGRES_DB" \
      --format=custom --no-owner --no-privileges --file="$part"
    pg_restore --list "$part" >/dev/null
    mv "$part" "$final"
    cd /backups
    sha256sum "$BACKUP_NAME" > "${BACKUP_NAME}.sha256"
    sha256sum --check "${BACKUP_NAME}.sha256" >/dev/null
    trap - EXIT INT TERM
  '

if [[ ! -s "$ZGAI_BACKUPS/$BACKUP_NAME" || ! -s "$ZGAI_BACKUPS/$BACKUP_NAME.sha256" ]]; then
  echo "备份产物或校验文件不存在。" >&2
  exit 1
fi

RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-180}"
if ! [[ "$RETENTION_DAYS" =~ ^[1-9][0-9]*$ ]]; then
  echo "BACKUP_RETENTION_DAYS 必须是正整数。" >&2
  exit 2
fi

find "$ZGAI_BACKUPS" -maxdepth 1 -type f \
  \( -name 'lawfirm_backup_*.dump' -o -name 'lawfirm_backup_*.dump.sha256' \) \
  -mtime "+$RETENTION_DAYS" -delete

echo "PostgreSQL 备份与 pg_restore 目录校验完成。"
echo "文件: $BACKUP_NAME"
echo "保留策略: $RETENTION_DAYS 天"
