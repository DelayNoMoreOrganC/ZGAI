#!/bin/zsh
set -e

ROOT_DIR="/Users/juno/ZGAI"
BACKEND_DIR="$ROOT_DIR/backend"
SECRETS_FILE="$BACKEND_DIR/data/.dev-secrets"

if [[ -f "$SECRETS_FILE" ]]; then
  set -a
  source "$SECRETS_FILE"
  set +a
fi

export JAVA_HOME="/opt/homebrew/opt/openjdk@11/libexec/openjdk.jdk/Contents/Home"
export PATH="/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:$PATH"
CODEX_PDFTOPPM="/Users/juno/.cache/codex-runtimes/codex-primary-runtime/dependencies/bin/override/pdftoppm"
if [[ -x "$CODEX_PDFTOPPM" && -z "${PDFTOPPM_COMMAND:-}" ]]; then
  export PDFTOPPM_COMMAND="$CODEX_PDFTOPPM"
fi
cd "$BACKEND_DIR"
exec "$JAVA_HOME/bin/java" -jar target/lawfirm-backend-2.0.0.jar
