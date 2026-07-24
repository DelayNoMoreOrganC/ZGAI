#!/bin/bash

set -u

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
export PATH="/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:$PATH"

clear
echo "========================================="
echo "  ZGAI 至高律所管理系统快速启动"
echo "========================================="
echo ""

if "$ROOT_DIR/start.sh"; then
    echo ""
    echo "系统已启动，正在打开浏览器..."
    if command -v open >/dev/null 2>&1; then
        open "http://localhost:3017"
    fi
    result=0
else
    result=$?
    echo ""
    echo "启动失败。请查看以下日志："
    echo "  $ROOT_DIR/logs/backend.log"
    echo "  $ROOT_DIR/logs/frontend.log"
fi

echo ""
if [ -t 0 ]; then
    read -r -p "按回车键关闭此窗口..." _
fi
exit "$result"
