#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

write_base() {
  local file="$1"
  cat > "$file" <<'EOF'
CLOUD_DEPLOYMENT_MODE=DETACHED_DEMO
POSTGRES_PASSWORD=database-password
INITIAL_ADMIN_PASSWORD=initial-password
JWT_SECRET=jwt-secret-abcdefghijklmnopqrstuvwxyz-1234
CRYPTO_SECRET_KEY=crypto-secret-abcdefghijklmnopqrstuvwxyz
ZGAI_LISTEN_ADDRESS=127.0.0.1
ZGAI_HTTP_PORT=3017
ZGAI_DOMAIN=demo.test.example
ZGAI_POSTGRES_DATA=/srv/zgai/postgres
ZGAI_QDRANT_DATA=/srv/zgai/qdrant
ZGAI_CASE_FILES=/srv/zgai/case-files
ZGAI_KNOWLEDGE_FILES=/srv/zgai/knowledge-files
ZGAI_BACKUPS=/srv/zgai/backups
ZGAI_KNOWLEDGE_IMPORTS=/srv/zgai/knowledge-imports
ZGAI_DOCUMENT_INTAKE=/srv/zgai/document-intake
ZGAI_APPROVAL_FILES=/srv/zgai/approval-files
ZGAI_FIRM_POLICY_SOURCE=/srv/zgai/firm-policies
BACKUP_RETENTION_DAYS=180
EOF
}

valid="$TMP_DIR/valid.env"
write_base "$valid"
"$SCRIPT_DIR/validate-config.sh" "$valid" >/dev/null
rendered="$TMP_DIR/zgai.nginx.conf"
"$SCRIPT_DIR/render-nginx-config.sh" "$valid" "$rendered" >/dev/null
rendered_text="$(<"$rendered")"
if [[ "$rendered_text" != *"server_name demo.test.example;"* \
    || "$rendered_text" != *"proxy_pass http://127.0.0.1:3017;"* \
    || "$rendered_text" == *"demo.example.com"* ]]; then
  echo "Nginx 配置渲染失败" >&2
  exit 1
fi

public="$TMP_DIR/public.env"
write_base "$public"
printf '%s\n' 'ZGAI_LISTEN_ADDRESS=0.0.0.0' >> "$public"
if "$SCRIPT_DIR/validate-config.sh" "$public" >/dev/null 2>&1; then
  echo "公开绑定校验未生效" >&2
  exit 1
fi

invalid_domain="$TMP_DIR/invalid-domain.env"
write_base "$invalid_domain"
printf '%s\n' 'ZGAI_DOMAIN=https://demo.test.example' >> "$invalid_domain"
if "$SCRIPT_DIR/validate-config.sh" "$invalid_domain" >/dev/null 2>&1; then
  echo "非法域名校验未生效" >&2
  exit 1
fi

invalid_port="$TMP_DIR/invalid-port.env"
write_base "$invalid_port"
printf '%s\n' 'ZGAI_HTTP_PORT=70000' >> "$invalid_port"
if "$SCRIPT_DIR/validate-config.sh" "$invalid_port" >/dev/null 2>&1; then
  echo "非法端口校验未生效" >&2
  exit 1
fi

insecure_mode="$TMP_DIR/insecure-mode.env"
write_base "$insecure_mode"
chmod 644 "$insecure_mode"
runtime_output="$TMP_DIR/runtime-output.txt"
if "$SCRIPT_DIR/validate-config.sh" "$insecure_mode" --runtime >"$runtime_output" 2>&1; then
  echo "配置文件权限校验未生效" >&2
  exit 1
fi
runtime_text="$(<"$runtime_output")"
if [[ "$runtime_text" != *"权限必须是 600"* ]]; then
  echo "运行态未优先拒绝宽松配置文件权限" >&2
  exit 1
fi

hybrid="$TMP_DIR/hybrid.env"
write_base "$hybrid"
cat >> "$hybrid" <<'EOF'
CLOUD_DEPLOYMENT_MODE=HYBRID_VPN
ZGAI_NAS_MOUNT_ROOT=/mnt/zgai-nas
ZGAI_OFFSITE_BACKUP_ROOT=/mnt/zgai-nas/backups
VPN_HEALTHCHECK_HOST=10.10.0.1
ZGAI_CASE_FILES=/mnt/zgai-nas/case-files
ZGAI_KNOWLEDGE_FILES=/mnt/zgai-nas/knowledge-files
ZGAI_APPROVAL_FILES=/mnt/zgai-nas/approval-files
ZGAI_FIRM_POLICY_SOURCE=/mnt/zgai-nas/firm-policies
LM_STUDIO_BASE_URL=http://10.10.0.20:1234/v1
EOF
"$SCRIPT_DIR/validate-config.sh" "$hybrid" >/dev/null

public_model="$TMP_DIR/public-model.env"
cp "$hybrid" "$public_model"
printf '%s\n' 'LM_STUDIO_BASE_URL=http://203.0.113.20:1234/v1' >> "$public_model"
if "$SCRIPT_DIR/validate-config.sh" "$public_model" >/dev/null 2>&1; then
  echo "公网模型地址校验未生效" >&2
  exit 1
fi

echo "cloud-config-tests=passed"
