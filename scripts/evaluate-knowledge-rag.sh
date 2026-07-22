#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${ZGAI_BASE_URL:-http://127.0.0.1:8080/api}"
USERNAME="${ZGAI_TEST_USERNAME:-admin}"
PASSWORD="${ZGAI_TEST_PASSWORD:-}"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DATASET="${ZGAI_RAG_EVAL_DATASET:-${ROOT_DIR}/docs/knowledge-rag-evaluation-v1.json}"

for command in curl jq; do
  if ! command -v "$command" >/dev/null 2>&1; then
    echo "缺少命令: $command" >&2
    exit 2
  fi
done

if [[ -z "$PASSWORD" ]]; then
  echo "请通过 ZGAI_TEST_PASSWORD 提供测试账号密码。" >&2
  exit 2
fi

if [[ ! -f "$DATASET" ]]; then
  echo "评价集不存在: $DATASET" >&2
  exit 2
fi

login_response="$(curl --max-time 10 -sS -X POST "${BASE_URL}/auth/login" \
  -H 'Content-Type: application/json' \
  -d "$(jq -nc --arg username "$USERNAME" --arg password "$PASSWORD" '{username:$username,password:$password}')")"
token="$(jq -r '.data.token // empty' <<<"$login_response")"
if [[ -z "$token" ]]; then
  echo "登录失败: $(jq -r '.message // "未知错误"' <<<"$login_response")" >&2
  exit 2
fi

case_count=0
source_expected=0
source_hits=0
term_expected=0
term_hits=0
privacy_failures=0

while IFS= read -r encoded_case; do
  case_json="$(printf '%s' "$encoded_case" | base64 --decode)"
  case_id="$(jq -r '.id' <<<"$case_json")"
  question="$(jq -r '.question' <<<"$case_json")"
  expected_source="$(jq -r '.expectedSource // empty' <<<"$case_json")"
  request_body="$(jq -nc --arg question "$question" '{question:$question,topK:5}')"
  response="$(curl --max-time 30 -sS -X POST "${BASE_URL}/knowledge/rag/search" \
    -H "Authorization: Bearer ${token}" \
    -H 'Content-Type: application/json' \
    -d "$request_body")"

  if [[ "$(jq -r '.code' <<<"$response")" != "200" ]]; then
    echo "[FAIL] ${case_id}: 接口错误 $(jq -r '.message' <<<"$response")"
    privacy_failures=$((privacy_failures + 1))
    case_count=$((case_count + 1))
    continue
  fi

  combined_text="$(jq -r '[(.data.answer // ""), (.data.sources[]?.excerpt // "")] | join("\n")' <<<"$response")"
  source_titles="$(jq -r '[.data.sources[]?.title] | join("、")' <<<"$response")"
  case_ok=true

  if [[ -n "$expected_source" ]]; then
    source_expected=$((source_expected + 1))
    if jq -e --arg source "$expected_source" '.data.sources | any(.title == $source)' <<<"$response" >/dev/null; then
      source_hits=$((source_hits + 1))
    else
      case_ok=false
      echo "[FAIL] ${case_id}: 未命中预期来源 ${expected_source}，实际 ${source_titles:-无}"
    fi
  fi

  while IFS= read -r term; do
    [[ -z "$term" ]] && continue
    term_expected=$((term_expected + 1))
    if [[ "$combined_text" == *"$term"* ]]; then
      term_hits=$((term_hits + 1))
    else
      case_ok=false
      echo "[MISS] ${case_id}: 命中内容缺少“${term}”"
    fi
  done < <(jq -r '.expectedTerms[]?' <<<"$case_json")

  while IFS= read -r forbidden_source; do
    [[ -z "$forbidden_source" ]] && continue
    if jq -e --arg source "$forbidden_source" '.data.sources | any(.knowledgeSource == $source)' <<<"$response" >/dev/null; then
      privacy_failures=$((privacy_failures + 1))
      case_ok=false
      echo "[FAIL] ${case_id}: 返回了禁止来源 ${forbidden_source}"
    fi
  done < <(jq -r '.forbiddenSources[]?' <<<"$case_json")

  if [[ "$case_ok" == true ]]; then
    echo "[PASS] ${case_id}: ${source_titles:-未返回来源}"
  fi
  case_count=$((case_count + 1))
done < <(jq -r '.cases[] | @base64' "$DATASET")

source_rate="$(jq -n --argjson hits "$source_hits" --argjson total "$source_expected" 'if $total == 0 then 1 else $hits / $total end')"
term_rate="$(jq -n --argjson hits "$term_hits" --argjson total "$term_expected" 'if $total == 0 then 1 else $hits / $total end')"
required_source_rate="$(jq -r '.acceptance.sourceHitRate' "$DATASET")"
required_term_rate="$(jq -r '.acceptance.excerptTermHitRate' "$DATASET")"

echo
echo "评价结果: 用例=${case_count}, 来源命中=${source_hits}/${source_expected} (${source_rate}), 条文词命中=${term_hits}/${term_expected} (${term_rate}), 隐私越界=${privacy_failures}"

if ! jq -e -n \
  --argjson source "$source_rate" \
  --argjson sourceRequired "$required_source_rate" \
  --argjson term "$term_rate" \
  --argjson termRequired "$required_term_rate" \
  --argjson privacy "$privacy_failures" \
  '$source >= $sourceRequired and $term >= $termRequired and $privacy == 0' >/dev/null; then
  echo "RAG 评价未达到门槛。" >&2
  exit 1
fi

echo "RAG 评价达到当前首期门槛。"
