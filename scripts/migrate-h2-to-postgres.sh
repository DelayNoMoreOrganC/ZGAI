#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BACKEND_DIR="${ROOT_DIR}/backend"
ACTION="${1:-plan}"
SOURCE_FILE="${MIGRATION_H2_SOURCE_FILE:-${BACKEND_DIR}/data/lawfirm.mv.db}"
RUN_ROOT="${MIGRATION_RUN_ROOT:-${BACKEND_DIR}/data/migration-runs}"
JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@11}"

case "$ACTION" in
  plan) ACTION_ENV=PLAN ;;
  dry-run) ACTION_ENV=DRY_RUN ;;
  execute) ACTION_ENV=EXECUTE ;;
  *) echo "用法: $0 [dry-run|plan|execute]" >&2; exit 2 ;;
esac

if [[ "$ACTION" != "dry-run" ]]; then
  for name in POSTGRES_URL POSTGRES_USER POSTGRES_PASSWORD; do
    value="${!name:-}"
    if [[ -z "$value" ]]; then
      echo "$name 未配置。" >&2
      exit 2
    fi
  done
  if [[ "$POSTGRES_URL" != jdbc:postgresql://* ]]; then
    echo "POSTGRES_URL 必须是 PostgreSQL JDBC 地址。" >&2
    exit 2
  fi
  if [[ "$ACTION" == "execute" && -z "${MIGRATION_CONFIRM_DATABASE:-}" ]]; then
    echo "execute 必须设置 MIGRATION_CONFIRM_DATABASE 为目标数据库名。" >&2
    exit 2
  fi
fi

if curl -fsS --max-time 2 http://127.0.0.1:8080/api/health >/dev/null 2>&1; then
  echo "ZGAI 后端仍在运行。请先停止系统，确保 H2 文件处于一致状态。" >&2
  exit 1
fi
if [[ ! -f "$SOURCE_FILE" || -L "$SOURCE_FILE" ]]; then
  echo "H2 源文件不存在或是符号链接: $SOURCE_FILE" >&2
  exit 2
fi

timestamp="$(date '+%Y%m%d_%H%M%S')"
run_dir="${RUN_ROOT}/${timestamp}_${ACTION}"
mkdir -p "$run_dir"
source_copy="$run_dir/lawfirm.mv.db"
cp -p "$SOURCE_FILE" "$source_copy"

if command -v sha256sum >/dev/null 2>&1; then
  source_sha="$(sha256sum "$source_copy" | awk '{print $1}')"
else
  source_sha="$(shasum -a 256 "$source_copy" | awk '{print $1}')"
fi
printf '%s  %s\n' "$source_sha" "$(basename "$source_copy")" > "$run_dir/lawfirm.mv.db.sha256"

echo "编译离线迁移工具..."
(
  cd "$BACKEND_DIR"
  JAVA_HOME="$JAVA_HOME" mvn -q -DskipTests package
)
application_jar="${BACKEND_DIR}/target/lawfirm-backend-2.0.0.jar"
if [[ ! -f "$application_jar" ]]; then
  echo "迁移工具构建产物不存在: $application_jar" >&2
  exit 1
fi
report_path="$run_dir/migration-report.json"
source_url="jdbc:h2:file:${run_dir}/lawfirm;MODE=MySQL;ACCESS_MODE_DATA=r;DB_CLOSE_ON_EXIT=FALSE"

echo "运行迁移${ACTION_ENV}，源文件只读副本: $source_copy"
if [[ "$ACTION" == "dry-run" ]]; then
  migration_main=com.lawfirm.migration.H2MigrationDryRun
  MIGRATION_SOURCE_COPY=true \
  MIGRATION_H2_URL="$source_url" \
  MIGRATION_SOURCE_SHA256="$source_sha" \
  MIGRATION_REPORT_PATH="$report_path" \
    "$JAVA_HOME/bin/java" -Dloader.main="$migration_main" \
      -cp "$application_jar" org.springframework.boot.loader.PropertiesLauncher
else
  migration_main=com.lawfirm.migration.H2ToPostgreSqlMigration
  MIGRATION_ACTION="$ACTION_ENV" \
  MIGRATION_SOURCE_COPY=true \
  MIGRATION_H2_URL="$source_url" \
  MIGRATION_SOURCE_SHA256="$source_sha" \
  MIGRATION_REPORT_PATH="$report_path" \
  POSTGRES_URL="$POSTGRES_URL" \
  POSTGRES_USER="$POSTGRES_USER" \
  POSTGRES_PASSWORD="$POSTGRES_PASSWORD" \
  MIGRATION_CONFIRM_DATABASE="${MIGRATION_CONFIRM_DATABASE:-}" \
    "$JAVA_HOME/bin/java" -Dloader.main="$migration_main" \
      -cp "$application_jar" org.springframework.boot.loader.PropertiesLauncher
fi

echo "迁移任务完成。"
echo "报告: $report_path"
echo "源副本 SHA-256: $source_sha"
