#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${1:-${SCRIPT_DIR}/.env.cloud}"
OUTPUT_FILE="${2:-${SCRIPT_DIR}/zgai.nginx.conf}"

"${SCRIPT_DIR}/validate-config.sh" "$ENV_FILE" >/dev/null
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

output_dir="$(cd "$(dirname "$OUTPUT_FILE")" && pwd -P)"
output_path="$output_dir/$(basename "$OUTPUT_FILE")"
temporary_path="${output_path}.part"
umask 077
trap 'rm -f "$temporary_path"' EXIT INT TERM

sed \
  -e "s/demo\.example\.com/${ZGAI_DOMAIN}/g" \
  -e "s/127\.0\.0\.1:3017/127.0.0.1:${ZGAI_HTTP_PORT}/g" \
  "${SCRIPT_DIR}/zgai.nginx.conf.example" > "$temporary_path"

mv "$temporary_path" "$output_path"
trap - EXIT INT TERM
echo "Nginx 配置已生成: $output_path"
