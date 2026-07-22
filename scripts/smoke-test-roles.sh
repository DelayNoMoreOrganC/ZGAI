#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${ZGAI_BASE_URL:-http://127.0.0.1:8080/api}"

for command_name in curl jq; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "缺少命令：$command_name" >&2
    exit 2
  fi
done

declare -a REQUIRED_VARS=(
  ZGAI_LAWYER_USERNAME ZGAI_LAWYER_PASSWORD
  ZGAI_ADMINISTRATIVE_USERNAME ZGAI_ADMINISTRATIVE_PASSWORD
  ZGAI_DIRECTOR_USERNAME ZGAI_DIRECTOR_PASSWORD
  ZGAI_FINANCE_USERNAME ZGAI_FINANCE_PASSWORD
)

for variable_name in "${REQUIRED_VARS[@]}"; do
  if [[ -z "${!variable_name:-}" ]]; then
    echo "缺少环境变量：$variable_name" >&2
    exit 2
  fi
done

login() {
  local username="$1"
  local password="$2"
  curl --fail --silent --show-error --max-time 15 \
    -H 'Content-Type: application/json' \
    --data "$(jq -nc --arg username "$username" --arg password "$password" \
      '{username: $username, password: $password}')" \
    "$BASE_URL/auth/login"
}

get_authenticated() {
  local token="$1"
  local endpoint="$2"
  curl --fail --silent --show-error --max-time 15 \
    -H "Authorization: Bearer $token" \
    "$BASE_URL/$endpoint"
}

assert_permission() {
  local label="$1"
  local current_user="$2"
  local permission="$3"
  local expected="$4"
  local actual
  actual="$(jq -r --arg permission "$permission" '.data.permissions | index($permission) != null' <<<"$current_user")"
  if [[ "$actual" != "$expected" ]]; then
    echo "[失败] $label 的 $permission 预期为 $expected，实际为 $actual" >&2
    exit 1
  fi
}

check_read_endpoint() {
  local label="$1"
  local token="$2"
  local endpoint="$3"
  local response
  response="$(get_authenticated "$token" "$endpoint")"
  if [[ "$(jq -r '.code // empty' <<<"$response")" != "200" ]]; then
    echo "[失败] $label：$(jq -c '.' <<<"$response")" >&2
    exit 1
  fi
}

check_persona() {
  local label="$1"
  local username="$2"
  local password="$3"
  shift 3

  local login_response token current_user permission expectation rule
  login_response="$(login "$username" "$password")"
  token="$(jq -r '.data.token // empty' <<<"$login_response")"
  if [[ -z "$token" ]]; then
    echo "[失败] $label 登录未返回 token" >&2
    exit 1
  fi

  current_user="$(get_authenticated "$token" 'auth/current-user')"
  for rule in "$@"; do
    permission="${rule%%=*}"
    expectation="${rule#*=}"
    assert_permission "$label" "$current_user" "$permission" "$expectation"
  done

  check_read_endpoint "$label 案件台账" "$token" 'cases?page=0&size=1'
  check_read_endpoint "$label 客户库" "$token" 'clients?page=0&size=1'
  check_read_endpoint "$label 审批" "$token" 'approval?page=1&size=1'
  check_read_endpoint "$label 发票" "$token" 'finance/invoices?page=0&size=1'
  check_read_endpoint "$label 知识库" "$token" 'knowledge?page=0&size=1'
  echo "[通过] $label：账号、权限和五项核心只读接口"
}

check_persona "普通律师" "$ZGAI_LAWYER_USERNAME" "$ZGAI_LAWYER_PASSWORD" \
  CASE_VIEW=true CASE_CREATE=true SYSTEM_CONFIG=false FINANCE_EDIT=false
check_persona "行政管理" "$ZGAI_ADMINISTRATIVE_USERNAME" "$ZGAI_ADMINISTRATIVE_PASSWORD" \
  CASE_VIEW=true APPROVAL_EDIT=true SYSTEM_CONFIG=false
check_persona "主任" "$ZGAI_DIRECTOR_USERNAME" "$ZGAI_DIRECTOR_PASSWORD" \
  CASE_DELETE=true CLIENT_DELETE=true SYSTEM_CONFIG=true AI_CONFIG=true
check_persona "财务" "$ZGAI_FINANCE_USERNAME" "$ZGAI_FINANCE_PASSWORD" \
  FINANCE_VIEW=true FINANCE_EDIT=true SYSTEM_CONFIG=false

echo "四类角色只读回归完成：全部通过。"
