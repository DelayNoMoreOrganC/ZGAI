#!/bin/bash
# ZGAI services shutdown helper. SIGKILL is used only after the grace period.

set -u

BACKEND_PORT="${ZGAI_BACKEND_PORT:-8080}"
FRONTEND_PORT="${ZGAI_FRONTEND_PORT:-3017}"
SSB_FRONTEND_PORT="${ZGAI_SSB_FRONTEND_PORT:-3000}"
SSB_BACKEND_PORT="${ZGAI_SSB_BACKEND_PORT:-5000}"
SSB_PROXY_PORT="${ZGAI_SSB_PROXY_PORT:-5002}"
AC_PORT="${ZGAI_AC_PORT:-5100}"
STOP_TIMEOUT_SECONDS="${ZGAI_STOP_TIMEOUT_SECONDS:-20}"

case "$STOP_TIMEOUT_SECONDS" in
  ''|*[!0-9]*)
    echo "[!] ZGAI_STOP_TIMEOUT_SECONDS must be a non-negative integer" >&2
    exit 2
    ;;
esac

is_selected() {
  local service="$1"
  if [ "$#" -eq 1 ] || [ "${2:-}" = "all" ]; then
    return 0
  fi
  shift
  for requested in "$@"; do
    [ "$requested" = "$service" ] && return 0
  done
  return 1
}

running_pids() {
  local pids="$1"
  local running=""
  for pid in $pids; do
    if kill -0 "$pid" 2>/dev/null; then
      running="$running $pid"
    fi
  done
  echo "$running"
}

stop_port() {
  local label="$1"
  local port="$2"
  local pids
  local running
  local elapsed=0
  local max_checks=$((STOP_TIMEOUT_SECONDS * 4))

  pids="$(lsof -nP -tiTCP:"$port" -sTCP:LISTEN 2>/dev/null || true)"
  if [ -z "$pids" ]; then
    echo "  - $label ($port) is not running"
    return 0
  fi

  echo "  * Stopping $label ($port), PID: $(echo "$pids" | tr '\n' ' ')"
  kill -TERM $pids 2>/dev/null || true

  while [ "$elapsed" -lt "$max_checks" ]; do
    running="$(running_pids "$pids")"
    if [ -z "$running" ]; then
      echo "  + $label stopped gracefully"
      return 0
    fi
    sleep 0.25
    elapsed=$((elapsed + 1))
  done

  running="$(running_pids "$pids")"
  if [ -n "$running" ]; then
    echo "  ! $label did not stop within ${STOP_TIMEOUT_SECONDS}s; forcing shutdown"
    kill -KILL $running 2>/dev/null || true
  fi
}

if [ "$#" -eq 0 ]; then
  set -- all
fi

for requested in "$@"; do
  case "$requested" in
    all|backend|frontend|ssb|ac) ;;
    *)
      echo "Usage: $0 [all|backend|frontend|ssb|ac] [...]" >&2
      exit 2
      ;;
  esac
done

echo "[*] Stopping selected ZGAI services..."
is_selected backend "$@" && stop_port "backend" "$BACKEND_PORT"
is_selected frontend "$@" && stop_port "frontend" "$FRONTEND_PORT"
if is_selected ssb "$@"; then
  stop_port "SSB frontend" "$SSB_FRONTEND_PORT"
  stop_port "SSB backend" "$SSB_BACKEND_PORT"
  stop_port "SSB proxy" "$SSB_PROXY_PORT"
fi
is_selected ac "$@" && stop_port "AC" "$AC_PORT"
echo "[+] Shutdown completed"
