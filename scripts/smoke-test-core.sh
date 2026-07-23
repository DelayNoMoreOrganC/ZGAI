#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${ZGAI_BASE_URL:-http://127.0.0.1:8080/api}"
USERNAME="${ZGAI_TEST_USERNAME:-admin}"
PASSWORD="${ZGAI_TEST_PASSWORD:-}"

if [[ -z "$PASSWORD" ]]; then
  echo "请通过 ZGAI_TEST_PASSWORD 提供测试账号密码。" >&2
  exit 2
fi

for command_name in curl jq; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "缺少命令：$command_name" >&2
    exit 2
  fi
done

PASS_COUNT=0

assert_success() {
  local name="$1"
  local response="$2"
  local code
  code="$(jq -r '.code // empty' <<<"$response")"
  if [[ "$code" != "200" ]]; then
    echo "[失败] ${name}：$(jq -c '.' <<<"$response")" >&2
    exit 1
  fi
  PASS_COUNT=$((PASS_COUNT + 1))
  echo "[通过] $name"
}

get_authenticated() {
  local endpoint="$1"
  curl --fail --silent --show-error \
    -H "Authorization: Bearer $TOKEN" \
    "$BASE_URL/$endpoint"
}

health_response="$(curl --fail --silent --show-error "$BASE_URL/health")"
assert_success "后端与数据库健康检查" "$health_response"
if [[ "$(jq -r '.data.status' <<<"$health_response")" != "ready" ]]; then
  echo "[失败] 数据库状态不是 ready" >&2
  exit 1
fi

login_response="$(curl --fail --silent --show-error \
  -H 'Content-Type: application/json' \
  --data "$(jq -nc --arg username "$USERNAME" --arg password "$PASSWORD" \
    '{username: $username, password: $password}')" \
  "$BASE_URL/auth/login")"
assert_success "账号登录" "$login_response"
TOKEN="$(jq -r '.data.token // empty' <<<"$login_response")"
if [[ -z "$TOKEN" ]]; then
  echo "[失败] 登录响应未返回 token" >&2
  exit 1
fi

declare -a CHECKS=(
  "当前账号|auth/current-user"
  "案件台账|cases?page=0&size=5"
  "客户库|clients?page=0&size=5"
  "立案审批|approval?page=1&size=5"
  "发票申请|finance/invoices?page=0&size=5"
  "知识库|knowledge?page=0&size=5"
  "RAG运行状态|knowledge/rag/health"
)

for check in "${CHECKS[@]}"; do
  name="${check%%|*}"
  endpoint="${check#*|}"
  response="$(get_authenticated "$endpoint")"
  assert_success "$name" "$response"
done

echo "核心业务只读冒烟测试完成：$PASS_COUNT 项全部通过。"
