#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${1:-${SCRIPT_DIR}/.env.cloud}"
MODE="${2:---preflight}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "配置文件不存在: $ENV_FILE" >&2
  echo "请从 ${SCRIPT_DIR}/.env.cloud.example 创建配置。" >&2
  exit 2
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

deployment_mode="${CLOUD_DEPLOYMENT_MODE:-}"
if [[ "$deployment_mode" != "DETACHED_DEMO" && "$deployment_mode" != "HYBRID_VPN" ]]; then
  echo "CLOUD_DEPLOYMENT_MODE 只能是 DETACHED_DEMO 或 HYBRID_VPN。" >&2
  exit 2
fi

required_secrets=(POSTGRES_PASSWORD INITIAL_ADMIN_PASSWORD JWT_SECRET CRYPTO_SECRET_KEY)
required_paths=(ZGAI_POSTGRES_DATA ZGAI_QDRANT_DATA ZGAI_CASE_FILES ZGAI_KNOWLEDGE_FILES \
  ZGAI_BACKUPS ZGAI_KNOWLEDGE_IMPORTS ZGAI_DOCUMENT_INTAKE ZGAI_APPROVAL_FILES ZGAI_FIRM_POLICY_SOURCE)

for name in "${required_secrets[@]}"; do
  value="${!name:-}"
  if [[ -z "$value" || "$value" == *CHANGE_ME* ]]; then
    echo "$name 尚未设置为真实密钥。" >&2
    exit 2
  fi
done
if (( ${#JWT_SECRET} < 32 || ${#CRYPTO_SECRET_KEY} < 32 )); then
  echo "JWT_SECRET 和 CRYPTO_SECRET_KEY 均须至少 32 个字符。" >&2
  exit 2
fi
if [[ "$JWT_SECRET" == "$CRYPTO_SECRET_KEY" ]]; then
  echo "JWT_SECRET 与 CRYPTO_SECRET_KEY 必须使用不同的随机值。" >&2
  exit 2
fi

for name in "${required_paths[@]}"; do
  value="${!name:-}"
  if [[ -z "$value" || "$value" != /* || "$value" == /tmp/* ]]; then
    echo "$name 必须是非 /tmp 的绝对持久化路径。" >&2
    exit 2
  fi
done

if [[ "${ZGAI_LISTEN_ADDRESS:-}" != "127.0.0.1" ]]; then
  echo "云端容器入口必须绑定 127.0.0.1，由宿主机 Nginx 提供 HTTPS。" >&2
  exit 2
fi
if ! [[ "${ZGAI_HTTP_PORT:-}" =~ ^[1-9][0-9]{0,4}$ ]] \
    || (( ZGAI_HTTP_PORT > 65535 )); then
  echo "ZGAI_HTTP_PORT 必须是 1-65535 的端口号。" >&2
  exit 2
fi
if [[ -z "${ZGAI_DOMAIN:-}" || "$ZGAI_DOMAIN" == "demo.example.com" \
    || ! "$ZGAI_DOMAIN" =~ ^[A-Za-z0-9.-]+\.[A-Za-z]{2,}$ ]]; then
  echo "ZGAI_DOMAIN 必须设置为真实域名，不包含协议。" >&2
  exit 2
fi
if [[ "$ZGAI_POSTGRES_DATA" == "$ZGAI_QDRANT_DATA" ]]; then
  echo "PostgreSQL 与 Qdrant 必须使用不同目录。" >&2
  exit 2
fi
if ! [[ "${BACKUP_RETENTION_DAYS:-180}" =~ ^[1-9][0-9]*$ ]]; then
  echo "BACKUP_RETENTION_DAYS 必须是正整数。" >&2
  exit 2
fi
if [[ -n "${AI_EMBEDDING_MODEL:-}" ]] && ! [[ "${AI_EMBEDDING_DIMENSION:-}" =~ ^[1-9][0-9]*$ ]]; then
  echo "启用 Embedding 时 AI_EMBEDDING_DIMENSION 必须是正整数。" >&2
  exit 2
fi

if [[ "$deployment_mode" == "DETACHED_DEMO" ]]; then
  if [[ -n "${ZGAI_NAS_MOUNT_ROOT:-}" || -n "${VPN_HEALTHCHECK_HOST:-}" ]]; then
    echo "DETACHED_DEMO 不得配置 NAS/VPN；该模式只使用云端脱敏数据。" >&2
    exit 2
  fi
  if [[ -n "${LM_STUDIO_BASE_URL:-}" ]]; then
    echo "DETACHED_DEMO 不得连接律所模型机；请使用 HYBRID_VPN。" >&2
    exit 2
  fi
else
  if [[ -z "${ZGAI_NAS_MOUNT_ROOT:-}" || "$ZGAI_NAS_MOUNT_ROOT" != /* ]]; then
    echo "HYBRID_VPN 必须设置 ZGAI_NAS_MOUNT_ROOT。" >&2
    exit 2
  fi
  if [[ -z "${VPN_HEALTHCHECK_HOST:-}" ]]; then
    echo "HYBRID_VPN 必须设置 VPN_HEALTHCHECK_HOST。" >&2
    exit 2
  fi
  if [[ -z "${ZGAI_OFFSITE_BACKUP_ROOT:-}" || "$ZGAI_OFFSITE_BACKUP_ROOT" != /* ]]; then
    echo "HYBRID_VPN 必须设置 ZGAI_OFFSITE_BACKUP_ROOT。" >&2
    exit 2
  fi
  for name in ZGAI_CASE_FILES ZGAI_KNOWLEDGE_FILES ZGAI_APPROVAL_FILES ZGAI_FIRM_POLICY_SOURCE ZGAI_OFFSITE_BACKUP_ROOT; do
    value="${!name}"
    if [[ "$value" != "$ZGAI_NAS_MOUNT_ROOT" && "$value" != "$ZGAI_NAS_MOUNT_ROOT"/* ]]; then
      echo "$name 必须位于 ZGAI_NAS_MOUNT_ROOT 内。" >&2
      exit 2
    fi
  done
  if [[ -z "${LM_STUDIO_BASE_URL:-}" || ! "$LM_STUDIO_BASE_URL" =~ ^http://(10\.|192\.168\.|172\.(1[6-9]|2[0-9]|3[01])\.) ]]; then
    echo "HYBRID_VPN 的 LM_STUDIO_BASE_URL 必须使用 VPN 内的 RFC1918 私网地址。" >&2
    exit 2
  fi
fi

if [[ "$MODE" == "--runtime" ]]; then
  env_mode="$(stat -c '%a' "$ENV_FILE" 2>/dev/null || true)"
  if [[ -z "$env_mode" || "$env_mode" != "600" ]]; then
    echo "云端配置文件权限必须是 600: $ENV_FILE" >&2
    exit 2
  fi
  if ! command -v docker >/dev/null 2>&1 || ! docker compose version >/dev/null 2>&1; then
    echo "需要 Docker Compose v2。" >&2
    exit 2
  fi
  for name in "${required_paths[@]}"; do
    value="${!name}"
    if [[ ! -d "$value" || ! -r "$value" || ! -w "$value" ]]; then
      echo "$name 不存在或不可读写: $value" >&2
      exit 2
    fi
  done
  if [[ "$deployment_mode" == "HYBRID_VPN" ]]; then
    if ! command -v findmnt >/dev/null 2>&1 || ! findmnt -T "$ZGAI_NAS_MOUNT_ROOT" >/dev/null 2>&1; then
      echo "NAS 根目录尚未通过 Linux 挂载点提供: $ZGAI_NAS_MOUNT_ROOT" >&2
      exit 1
    fi
    if ! ping -c 1 -W 3 "$VPN_HEALTHCHECK_HOST" >/dev/null 2>&1; then
      echo "VPN 健康检查失败: $VPN_HEALTHCHECK_HOST" >&2
      exit 1
    fi
    if [[ ! -d "$ZGAI_OFFSITE_BACKUP_ROOT" || ! -w "$ZGAI_OFFSITE_BACKUP_ROOT" ]]; then
      echo "NAS 异地备份目录不可写: $ZGAI_OFFSITE_BACKUP_ROOT" >&2
      exit 1
    fi
  fi
fi

echo "腾讯云 Linux 配置校验通过。"
echo "部署模式: $deployment_mode"
echo "容器入口: 127.0.0.1:${ZGAI_HTTP_PORT:-3017}"
echo "公网入口: https://${ZGAI_DOMAIN}"
