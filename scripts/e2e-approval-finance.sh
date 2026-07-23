#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${ZGAI_BASE_URL:-http://127.0.0.1:8080/api}"
CONFIRMATION="${ZGAI_E2E_CONFIRM:-}"
FIXTURE_FILE="${ZGAI_E2E_FIXTURE_FILE:-}"

if [[ "$CONFIRMATION" != "RUN_WRITE_E2E" ]]; then
  echo "本脚本会创建并完成用印审批和开票申请。" >&2
  echo "仅可在隔离或已授权测试环境运行；请设置 ZGAI_E2E_CONFIRM=RUN_WRITE_E2E。" >&2
  exit 2
fi

for command_name in curl jq shasum; do
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

if [[ -z "$FIXTURE_FILE" || ! -f "$FIXTURE_FILE" ]]; then
  echo "ZGAI_E2E_FIXTURE_FILE 必须指向可上传的 PDF、Word、Excel 或图片。" >&2
  exit 2
fi

case "${FIXTURE_FILE##*.}" in
  pdf|PDF|doc|DOC|docx|DOCX|xls|XLS|xlsx|XLSX|png|PNG|jpg|JPG|jpeg|JPEG) ;;
  *)
    echo "验收夹具格式不受用印审批支持：$FIXTURE_FILE" >&2
    exit 2
    ;;
esac

WORK_DIR="$(mktemp -d "${TMPDIR:-/tmp}/zgai-e2e.XXXXXX")"
trap 'rm -rf "$WORK_DIR"' EXIT

timestamp="$(date '+%Y%m%d-%H%M%S')"
seal_title="E2E用印-${timestamp}"
invoice_title="E2E开票客户-${timestamp}"

login() {
  local username="$1"
  local password="$2"
  curl --fail --silent --show-error --max-time 20 \
    -H 'Content-Type: application/json' \
    --data "$(jq -nc --arg username "$username" --arg password "$password" \
      '{username: $username, password: $password}')" \
    "$BASE_URL/auth/login"
}

token_for() {
  local username="$1"
  local password="$2"
  local response token
  response="$(login "$username" "$password")"
  token="$(jq -r '.data.token // empty' <<<"$response")"
  if [[ -z "$token" ]]; then
    echo "账号 $username 登录未返回 token" >&2
    exit 1
  fi
  printf '%s' "$token"
}

api_json() {
  local method="$1"
  local token="$2"
  local endpoint="$3"
  local body="${4:-}"
  if [[ -n "$body" ]]; then
    curl --fail --silent --show-error --max-time 30 \
      -X "$method" \
      -H "Authorization: Bearer $token" \
      -H 'Content-Type: application/json' \
      --data "$body" \
      "$BASE_URL/$endpoint"
  else
    curl --fail --silent --show-error --max-time 30 \
      -X "$method" \
      -H "Authorization: Bearer $token" \
      "$BASE_URL/$endpoint"
  fi
}

assert_success() {
  local label="$1"
  local response="$2"
  if [[ "$(jq -r '.code // empty' <<<"$response")" != "200" ]]; then
    echo "[失败] $label：$(jq -c '.' <<<"$response")" >&2
    exit 1
  fi
}

assert_value() {
  local label="$1"
  local actual="$2"
  local expected="$3"
  if [[ "$actual" != "$expected" ]]; then
    echo "[失败] $label，预期 $expected，实际 $actual" >&2
    exit 1
  fi
}

expect_http_status() {
  local label="$1"
  local expected="$2"
  shift 2
  local status
  status="$(curl --silent --show-error --max-time 30 \
    -o "$WORK_DIR/response.json" -w '%{http_code}' "$@")"
  assert_value "$label HTTP 状态" "$status" "$expected"
}

lawyer_token="$(token_for "$ZGAI_LAWYER_USERNAME" "$ZGAI_LAWYER_PASSWORD")"
administrative_token="$(token_for "$ZGAI_ADMINISTRATIVE_USERNAME" "$ZGAI_ADMINISTRATIVE_PASSWORD")"
director_token="$(token_for "$ZGAI_DIRECTOR_USERNAME" "$ZGAI_DIRECTOR_PASSWORD")"
finance_token="$(token_for "$ZGAI_FINANCE_USERNAME" "$ZGAI_FINANCE_PASSWORD")"

administrative_user="$(api_json GET "$administrative_token" 'auth/current-user')"
director_user="$(api_json GET "$director_token" 'auth/current-user')"
finance_user="$(api_json GET "$finance_token" 'auth/current-user')"
administrative_user_id="$(jq -r '.data.userId' <<<"$administrative_user")"
assert_value "行政账号 APPROVAL_EDIT 权限" \
  "$(jq -r '.data.permissions | index("APPROVAL_EDIT") != null' <<<"$administrative_user")" "true"
assert_value "财务账号 INVOICE_PROCESS 权限" \
  "$(jq -r '.data.permissions | index("INVOICE_PROCESS") != null' <<<"$finance_user")" "true"
assert_value "主任账号 MANAGER 角色" \
  "$(jq -r '.data.roles | index("MANAGER") != null' <<<"$director_user")" "true"

seal_response="$(curl --fail --silent --show-error --max-time 60 \
  -X POST "$BASE_URL/approval/seal" \
  -H "Authorization: Bearer $lawyer_token" \
  -F "title=$seal_title" \
  -F 'content=自动化验收：行政须审阅附件后处理。' \
  -F "file=@$FIXTURE_FILE")"
assert_success "律师发起用印审批" "$seal_response"
seal_id="$(jq -r '.data.id' <<<"$seal_response")"
assert_value "用印审批初始状态" "$(jq -r '.data.status' <<<"$seal_response")" "PENDING"
assert_value "用印附件数量" "$(jq -r '.data.sealAttachments | length' <<<"$seal_response")" "1"

assert_value "用印审批自动路由" \
  "$(jq -r '.data.currentApproverId' <<<"$seal_response")" "$administrative_user_id"

expect_http_status "律师不得审批本人用印申请" 403 \
  -X PUT "$BASE_URL/approval/$seal_id/approve" \
  -H "Authorization: Bearer $lawyer_token" \
  -H 'Content-Type: application/json' \
  --data '{"comments":"越权验收"}'

seal_detail="$(api_json GET "$administrative_token" "approval/$seal_id")"
assert_success "行政读取用印审批详情" "$seal_detail"
assert_value "行政详情附件数量" "$(jq -r '.data.sealAttachments | length' <<<"$seal_detail")" "1"

director_seal_detail="$(api_json GET "$director_token" "approval/$seal_id")"
assert_success "主任全局读取用印审批详情" "$director_seal_detail"
assert_value "主任查看不改变当前审批人" \
  "$(jq -r '.data.currentApproverId' <<<"$director_seal_detail")" "$administrative_user_id"

seal_approved="$(api_json PUT "$administrative_token" "approval/$seal_id/approve" \
  '{"comments":"E2E材料核对通过，同意用印"}')"
assert_success "行政同意用印" "$seal_approved"

seal_result="$(api_json GET "$lawyer_token" "approval/$seal_id")"
assert_value "申请人看到用印审批结果" "$(jq -r '.data.status' <<<"$seal_result")" "APPROVED"
assert_value "用印附件状态同步" \
  "$(jq -r '.data.sealAttachments[0].sealStatus' <<<"$seal_result")" "APPROVED"

invoice_body="$(jq -nc \
  --arg title "$invoice_title" \
  --arg source "$ZGAI_LAWYER_USERNAME" \
  '{invoiceType:"增值税普通发票", amount:12600, title:$title,
    taxNumber:"91440000E2E202607", invoiceContent:"法律服务费",
    executionDepartment:"E2E测试部门", sourceUserName:$source,
    remark:"自动化多角色闭环验收"}')"
invoice_response="$(api_json POST "$lawyer_token" 'finance/invoices' "$invoice_body")"
assert_success "律师发起开票申请" "$invoice_response"
invoice_id="$(jq -r '.data.id' <<<"$invoice_response")"
assert_value "开票申请初始状态" "$(jq -r '.data.status' <<<"$invoice_response")" "PENDING"

expect_http_status "律师不得上传发票反馈" 403 \
  -X POST "$BASE_URL/finance/invoices/$invoice_id/issue" \
  -H "Authorization: Bearer $lawyer_token" \
  -F 'invoiceNumber=E2E-UNAUTHORIZED' \
  -F "billingDate=$(date '+%Y-%m-%d')" \
  -F "file=@$FIXTURE_FILE"

invoice_issued="$(curl --fail --silent --show-error --max-time 60 \
  -X POST "$BASE_URL/finance/invoices/$invoice_id/issue" \
  -H "Authorization: Bearer $finance_token" \
  -F "invoiceNumber=E2E-${timestamp}" \
  -F "billingDate=$(date '+%Y-%m-%d')" \
  -F "file=@$FIXTURE_FILE")"
assert_success "财务上传发票反馈" "$invoice_issued"
assert_value "发票反馈状态" "$(jq -r '.data.status' <<<"$invoice_issued")" "FEEDBACK_UPLOADED"

invoice_completed="$(api_json POST "$finance_token" "finance/invoices/$invoice_id/complete")"
assert_success "财务完成开票" "$invoice_completed"
assert_value "开票锁定状态" "$(jq -r '.data.status' <<<"$invoice_completed")" "COMPLETED"

invoice_result="$(api_json GET "$lawyer_token" "finance/invoices/$invoice_id")"
assert_value "申请人看到完成状态" "$(jq -r '.data.status' <<<"$invoice_result")" "COMPLETED"

curl --fail --silent --show-error --max-time 60 \
  -H "Authorization: Bearer $lawyer_token" \
  "$BASE_URL/finance/invoices/$invoice_id/file" \
  -o "$WORK_DIR/invoice-feedback"
assert_value "发票反馈文件 SHA-256" \
  "$(shasum -a 256 "$WORK_DIR/invoice-feedback" | awk '{print $1}')" \
  "$(shasum -a 256 "$FIXTURE_FILE" | awk '{print $1}')"

echo "[通过] 用印审批：律师发起 → 越权拒绝 → 主任全局查看 → 行政审阅/同意 → 申请人查看结果"
echo "[通过] 开票申请：律师发起 → 越权拒绝 → 财务反馈/完成 → 申请人下载一致文件"
echo "验收记录：sealId=$seal_id invoiceId=$invoice_id"
