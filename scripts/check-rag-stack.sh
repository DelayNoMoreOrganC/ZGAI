#!/usr/bin/env bash

set -euo pipefail

LM_BASE_URL="${AI_EMBEDDING_BASE_URL:-${LM_STUDIO_BASE_URL:-http://127.0.0.1:1234/v1}}"
EMBEDDING_MODEL="${AI_EMBEDDING_MODEL:-}"
EXPECTED_DIMENSION="${AI_EMBEDDING_DIMENSION:-1024}"
EMBEDDING_API_KEY="${AI_EMBEDDING_API_KEY:-${LM_STUDIO_API_KEY:-}}"
QDRANT_URL="${QDRANT_URL:-http://${QDRANT_HOST:-127.0.0.1}:${QDRANT_PORT:-6333}}"
QDRANT_COLLECTION="${QDRANT_COLLECTION_NAME:-lawfirm-knowledge}"

for command in curl jq; do
  if ! command -v "$command" >/dev/null 2>&1; then
    echo "缺少命令: $command" >&2
    exit 2
  fi
done

if [[ -z "$EMBEDDING_MODEL" ]]; then
  echo "未设置 AI_EMBEDDING_MODEL，向量检索应保持关键词降级模式。" >&2
  exit 2
fi

if ! [[ "$EXPECTED_DIMENSION" =~ ^[1-9][0-9]*$ ]]; then
  echo "AI_EMBEDDING_DIMENSION 必须是正整数。" >&2
  exit 2
fi

LM_BASE_URL="${LM_BASE_URL%/}"
QDRANT_URL="${QDRANT_URL%/}"
if [[ "$LM_BASE_URL" == */embeddings ]]; then
  EMBEDDINGS_URL="$LM_BASE_URL"
  MODELS_URL="${LM_BASE_URL%/embeddings}/models"
elif [[ "$LM_BASE_URL" == */v1 ]]; then
  EMBEDDINGS_URL="$LM_BASE_URL/embeddings"
  MODELS_URL="$LM_BASE_URL/models"
else
  EMBEDDINGS_URL="$LM_BASE_URL/v1/embeddings"
  MODELS_URL="$LM_BASE_URL/v1/models"
fi

auth_args=()
if [[ -n "$EMBEDDING_API_KEY" ]]; then
  auth_args=(-H "Authorization: Bearer ${EMBEDDING_API_KEY}")
fi

echo "[1/4] 检查模型服务"
models_response="$(curl -fsS --max-time 15 "${auth_args[@]}" "$MODELS_URL")" || {
  echo "无法连接模型服务: $MODELS_URL" >&2
  exit 1
}
if ! jq -e --arg model "$EMBEDDING_MODEL" '.data | any(.id == $model)' <<<"$models_response" >/dev/null; then
  echo "模型服务未返回指定 Embedding 模型: $EMBEDDING_MODEL" >&2
  exit 1
fi
echo "  模型已加载: $EMBEDDING_MODEL"

echo "[2/4] 检查 Embedding 输出"
embedding_body="$(jq -nc --arg model "$EMBEDDING_MODEL" \
  '{model:$model,input:["ZGAI RAG 向量服务连通性检查"]}')"
embedding_response="$(curl -fsS --max-time 60 "${auth_args[@]}" \
  -H 'Content-Type: application/json' -X POST "$EMBEDDINGS_URL" -d "$embedding_body")" || {
  echo "Embedding 调用失败: $EMBEDDINGS_URL" >&2
  exit 1
}
actual_embedding_dimension="$(jq -r '.data[0].embedding | length // 0' <<<"$embedding_response")"
if [[ "$actual_embedding_dimension" != "$EXPECTED_DIMENSION" ]]; then
  echo "Embedding 维度不匹配：期望 $EXPECTED_DIMENSION，实际 $actual_embedding_dimension" >&2
  exit 1
fi
echo "  Embedding 维度: $actual_embedding_dimension"

echo "[3/4] 检查 Qdrant collection"
collection_response="$(curl -fsS --max-time 15 \
  "$QDRANT_URL/collections/$QDRANT_COLLECTION")" || {
  echo "无法读取 Qdrant collection: $QDRANT_COLLECTION" >&2
  echo "请确认 Qdrant 已启动，并在配置 Embedding 后重启 ZGAI 以初始化集合。" >&2
  exit 1
}
actual_qdrant_dimension="$(jq -r '
  .result.config.params.vectors as $vectors
  | if ($vectors.size? != null) then $vectors.size
    elif (($vectors | type) == "object" and ($vectors | length) == 1)
      then ($vectors | to_entries[0].value.size // 0)
    else 0 end' <<<"$collection_response")"
if [[ "$actual_qdrant_dimension" != "$EXPECTED_DIMENSION" ]]; then
  echo "Qdrant 维度不匹配：期望 $EXPECTED_DIMENSION，实际 $actual_qdrant_dimension" >&2
  exit 1
fi
echo "  Qdrant collection: $QDRANT_COLLECTION ($actual_qdrant_dimension 维)"

echo "[4/4] 验证结果"
echo "RAG 向量底座配置一致，可以运行知识索引和评价集。"
