#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${ZGAI_BASE_URL:-http://127.0.0.1:18083/api}"
CONFIRMATION="${ZGAI_E2E_CONFIRM:-}"
ENVIRONMENT="${ZGAI_E2E_ENVIRONMENT:-}"
INITIAL_PASSWORD="${ZGAI_E2E_INITIAL_PASSWORD:-}"

if [[ "$ENVIRONMENT" != "ISOLATED" || "$CONFIRMATION" != "PROVISION_PERSONAS" ]]; then
  echo "本脚本会创建或重置四类测试账号，请设置 ZGAI_E2E_ENVIRONMENT=ISOLATED 和 ZGAI_E2E_CONFIRM=PROVISION_PERSONAS。" >&2
  exit 2
fi

declare -a REQUIRED_VARS=(
  ZGAI_ADMIN_USERNAME ZGAI_ADMIN_PASSWORD ZGAI_E2E_INITIAL_PASSWORD
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

if [[ "$INITIAL_PASSWORD" == "$ZGAI_LAWYER_PASSWORD"
   || "$INITIAL_PASSWORD" == "$ZGAI_ADMINISTRATIVE_PASSWORD"
   || "$INITIAL_PASSWORD" == "$ZGAI_DIRECTOR_PASSWORD"
   || "$INITIAL_PASSWORD" == "$ZGAI_FINANCE_PASSWORD" ]]; then
  echo "ZGAI_E2E_INITIAL_PASSWORD 必须与四个最终测试密码不同。" >&2
  exit 2
fi

for command_name in curl jq; do
  if ! command -v "$command_name" >/dev/null 2>&1; then
    echo "缺少命令：$command_name" >&2
    exit 2
  fi
done

login_token() {
  local username="$1"
  local password="$2"
  curl --fail --silent --show-error --max-time 20 \
    -H 'Content-Type: application/json' \
    --data "$(jq -nc --arg username "$username" --arg password "$password" \
      '{username: $username, password: $password}')" \
    "$BASE_URL/auth/login" | jq -er '.data.token'
}

api_json() {
  local method="$1"
  local token="$2"
  local endpoint="$3"
  local body="${4:-}"
  if [[ -n "$body" ]]; then
    curl --fail --silent --show-error --max-time 30 -X "$method" \
      -H "Authorization: Bearer $token" -H 'Content-Type: application/json' \
      --data "$body" "$BASE_URL/$endpoint"
  else
    curl --fail --silent --show-error --max-time 30 -X "$method" \
      -H "Authorization: Bearer $token" "$BASE_URL/$endpoint"
  fi
}

require_success() {
  local label="$1"
  local response="$2"
  if [[ "$(jq -r '.code // empty' <<<"$response")" != "200" ]]; then
    echo "[失败] $label：$(jq -c '{code,message}' <<<"$response")" >&2
    exit 1
  fi
}

ADMIN_TOKEN="$(login_token "$ZGAI_ADMIN_USERNAME" "$ZGAI_ADMIN_PASSWORD")"
ROLES_RESPONSE="$(api_json GET "$ADMIN_TOKEN" 'roles/all')"
DEPARTMENTS_RESPONSE="$(api_json GET "$ADMIN_TOKEN" 'department')"
require_success "读取角色目录" "$ROLES_RESPONSE"
require_success "读取部门目录" "$DEPARTMENTS_RESPONSE"

DEPARTMENT_ID="$(jq -er '.data[] | select(.deptName == "民商法务部") | .id' <<<"$DEPARTMENTS_RESPONSE" | head -1)"

role_ids() {
  local csv="$1"
  jq -c --arg csv "$csv" '
    ($csv | split(",")) as $wanted
    | [.data[] | select(.roleCode as $code | $wanted | index($code)) | .id]
  ' <<<"$ROLES_RESPONSE"
}

provision_user() {
  local label="$1"
  local username="$2"
  local final_password="$3"
  local real_name="$4"
  local position="$5"
  local role_codes="$6"
  local phone="$7"
  local roles user_response user_id body reset_response user_token change_response

  roles="$(role_ids "$role_codes")"
  if [[ "$(jq 'length' <<<"$roles")" != "$(awk -F, '{print NF}' <<<"$role_codes")" ]]; then
    echo "[失败] $label 所需角色不存在：$role_codes" >&2
    exit 1
  fi

  user_response="$(api_json GET "$ADMIN_TOKEN" "users?page=0&size=20&keyword=$username")"
  require_success "$label 查询" "$user_response"
  user_id="$(jq -r --arg username "$username" '.data.content[]? | select(.username == $username) | .id' <<<"$user_response" | head -1)"
  body="$(jq -nc --arg username "$username" --arg password "$INITIAL_PASSWORD" \
    --arg realName "$real_name" --arg phone "$phone" --arg position "$position" \
    --argjson departmentId "$DEPARTMENT_ID" --argjson roleIds "$roles" \
    '{username:$username,password:$password,realName:$realName,phone:$phone,
      departmentId:$departmentId,position:$position,status:1,roleIds:$roleIds}')"

  if [[ -z "$user_id" ]]; then
    user_response="$(api_json POST "$ADMIN_TOKEN" 'users' "$body")"
    require_success "$label 创建" "$user_response"
    user_id="$(jq -er '.data.id' <<<"$user_response")"
  else
    body="$(jq -c 'del(.username,.password)' <<<"$body")"
    user_response="$(api_json PUT "$ADMIN_TOKEN" "users/$user_id" "$body")"
    require_success "$label 资料与角色校准" "$user_response"
    reset_response="$(api_json PUT "$ADMIN_TOKEN" "users/$user_id/reset-password" \
      "$(jq -nc --arg newPassword "$INITIAL_PASSWORD" '{newPassword:$newPassword}')")"
    require_success "$label 临时密码重置" "$reset_response"
  fi

  user_token="$(login_token "$username" "$INITIAL_PASSWORD")"
  change_response="$(api_json PUT "$user_token" 'users/change-password' \
    "$(jq -nc --arg oldPassword "$INITIAL_PASSWORD" --arg newPassword "$final_password" \
      '{oldPassword:$oldPassword,newPassword:$newPassword}')")"
  require_success "$label 首次改密" "$change_response"
  echo "[通过] $label 测试账号已准备"
}

provision_user "普通律师" "$ZGAI_LAWYER_USERNAME" "$ZGAI_LAWYER_PASSWORD" \
  "验收律师" "律师" "LAWYER" "13800000001"
provision_user "行政管理" "$ZGAI_ADMINISTRATIVE_USERNAME" "$ZGAI_ADMINISTRATIVE_PASSWORD" \
  "验收行政" "行政管理" "ADMINISTRATIVE,CASE_FILING_ADMIN" "13800000002"
provision_user "主任" "$ZGAI_DIRECTOR_USERNAME" "$ZGAI_DIRECTOR_PASSWORD" \
  "验收主任" "部门负责人" "MANAGER" "13800000003"
provision_user "财务" "$ZGAI_FINANCE_USERNAME" "$ZGAI_FINANCE_PASSWORD" \
  "验收财务" "财务" "FINANCE,INVOICE_PROCESSOR" "13800000004"

echo "四类隔离测试账号准备完成。"
