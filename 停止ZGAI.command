#!/bin/bash

set -u

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
export PATH="/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:$PATH"

clear
echo "========================================="
echo "  ZGAI 至高律所管理系统停止服务"
echo "========================================="
echo ""

"$ROOT_DIR/stop.sh" all
result=$?

echo ""
if [ -t 0 ]; then
    read -r -p "按回车键关闭此窗口..." _
fi
exit "$result"
