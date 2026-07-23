#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${1:-${SCRIPT_DIR}/.env.cloud}"

ZGAI_COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yml" \
ZGAI_CONFIG_VALIDATOR="${SCRIPT_DIR}/validate-config.sh" \
  "${SCRIPT_DIR}/../nas/backup-postgres.sh" "$ENV_FILE"

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

if [[ "${CLOUD_DEPLOYMENT_MODE:-}" == "HYBRID_VPN" ]]; then
  mkdir -p "$ZGAI_OFFSITE_BACKUP_ROOT"
  copied=0
  while IFS= read -r -d '' source_file; do
    target_file="$ZGAI_OFFSITE_BACKUP_ROOT/$(basename "$source_file")"
    if [[ ! -e "$target_file" ]]; then
      cp -p "$source_file" "$target_file.part"
      mv "$target_file.part" "$target_file"
      copied=$((copied + 1))
    fi
  done < <(find "$ZGAI_BACKUPS" -maxdepth 1 -type f \
    \( -name 'lawfirm_backup_*.dump' -o -name 'lawfirm_backup_*.dump.sha256' \) -print0)
  while IFS= read -r -d '' checksum_file; do
    (cd "$ZGAI_OFFSITE_BACKUP_ROOT" && sha256sum --check "$(basename "$checksum_file")" >/dev/null)
  done < <(find "$ZGAI_OFFSITE_BACKUP_ROOT" -maxdepth 1 -type f \
    -name 'lawfirm_backup_*.dump.sha256' -print0)
  echo "已向 NAS 异地备份目录复制 $copied 个新文件。"
fi
