#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${1:-${SCRIPT_DIR}/.env.nas}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "配置文件不存在: $ENV_FILE" >&2
  echo "请从 ${SCRIPT_DIR}/.env.nas.example 创建本机配置。" >&2
  exit 2
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

required_secrets=(POSTGRES_PASSWORD INITIAL_ADMIN_PASSWORD JWT_SECRET CRYPTO_SECRET_KEY ARCHIVE_WORKER_TOKEN)
required_paths=(ZGAI_POSTGRES_DATA ZGAI_QDRANT_DATA ZGAI_CASE_FILES ZGAI_KNOWLEDGE_FILES \
  ZGAI_BACKUPS ZGAI_KNOWLEDGE_IMPORTS ZGAI_DOCUMENT_INTAKE ZGAI_APPROVAL_FILES \
  ZGAI_ARCHIVE_CACHE ZGAI_FIRM_POLICY_SOURCE)

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
if (( ${#ARCHIVE_WORKER_TOKEN} < 32 )); then
  echo "ARCHIVE_WORKER_TOKEN 须至少 32 个字符。" >&2
  exit 2
fi

for name in "${required_paths[@]}"; do
  value="${!name:-}"
  if [[ -z "$value" || "$value" != /* ]]; then
    echo "$name 必须设置为 NAS 上的绝对路径。" >&2
    exit 2
  fi
done

if [[ "${ZGAI_POSTGRES_DATA}" == "${ZGAI_QDRANT_DATA}" ]]; then
  echo "PostgreSQL 与 Qdrant 必须使用不同目录。" >&2
  exit 2
fi

if [[ -n "${AI_EMBEDDING_MODEL:-}" ]] && ! [[ "${AI_EMBEDDING_DIMENSION:-}" =~ ^[1-9][0-9]*$ ]]; then
  echo "启用 Embedding 时 AI_EMBEDDING_DIMENSION 必须是正整数。" >&2
  exit 2
fi

echo "NAS staging 配置校验通过。"
echo "公开入口: ${ZGAI_LISTEN_ADDRESS:-0.0.0.0}:${ZGAI_HTTP_PORT:-3017}"
echo "向量模式: $([[ -n "${AI_EMBEDDING_MODEL:-}" ]] && echo '待连通性验证' || echo '关键词降级')"
