#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
ENV_FILE="${1:-${SCRIPT_DIR}/.env.demo}"
MODE="${2:---preflight}"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Demo 配置不存在: $ENV_FILE" >&2
  echo "请从 ${SCRIPT_DIR}/.env.demo.example 创建本机配置。" >&2
  exit 2
fi

if [[ -f "${ROOT_DIR}/backend/data/.dev-secrets" ]]; then
  set -a
  # shellcheck disable=SC1091
  source "${ROOT_DIR}/backend/data/.dev-secrets"
  set +a
fi
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

required_secrets=(POSTGRES_PASSWORD INITIAL_ADMIN_PASSWORD JWT_SECRET CRYPTO_SECRET_KEY)
required_paths=(CASE_FILE_LIBRARY_ROOT KNOWLEDGE_FILE_LIBRARY_ROOT BACKUP_ROOT_PATH)

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
  if [[ -z "$value" || "$value" != /Volumes/* ]]; then
    echo "$name 必须指向 /Volumes 下的 NAS 挂载目录。" >&2
    exit 2
  fi
  if [[ ! -d "$value" || ! -r "$value" || ! -w "$value" ]]; then
    echo "$name 不存在或当前账号不可读写: $value" >&2
    exit 2
  fi
done

if [[ -n "${AI_EMBEDDING_MODEL:-}" ]] && ! [[ "${AI_EMBEDDING_DIMENSION:-}" =~ ^[1-9][0-9]*$ ]]; then
  echo "启用 Embedding 时 AI_EMBEDDING_DIMENSION 必须是正整数。" >&2
  exit 2
fi

if [[ -n "${LM_STUDIO_BASE_URL:-}" ]]; then
  if [[ -z "${LM_STUDIO_API_KEY:-}" ]]; then
    echo "模型服务启用了鉴权，但 LM_STUDIO_API_KEY 未配置。" >&2
    exit 2
  fi
  models_url="${LM_STUDIO_BASE_URL%/}/models"
  if ! curl -fsS --max-time 5 -H "Authorization: Bearer ${LM_STUDIO_API_KEY}" \
      "$models_url" >/dev/null; then
    echo "无法通过鉴权访问模型服务: $models_url" >&2
    exit 1
  fi
fi

if [[ "$MODE" == "--runtime" ]]; then
  pg_isready_command="$(command -v pg_isready || true)"
  if [[ -z "$pg_isready_command" ]]; then
    echo "未安装 PostgreSQL 客户端，无法执行运行态检查。" >&2
    exit 2
  fi
  "$pg_isready_command" -h 127.0.0.1 -p "${POSTGRES_PORT:-5432}" \
    -d "${POSTGRES_DB:-zgai}" -U "${POSTGRES_USER:-zgai}" >/dev/null
  curl -fsS --max-time 5 "http://127.0.0.1:${QDRANT_PORT:-6333}/collections" >/dev/null
fi

echo "Mac Demo 配置校验通过。"
echo "资料存储: NAS"
echo "业务与向量服务: Mac 本机"
echo "AI 推理: Windows 模型机"
echo "RAG 模式: $([[ -n "${AI_EMBEDDING_MODEL:-}" ]] && echo '待向量自检' || echo '关键词降级')"
