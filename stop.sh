#!/bin/bash
# ============================================================
# ZGAI 律所智能案件管理系统 — 停止脚本
# ============================================================
echo "[*] 正在停止所有 ZGAI 服务..."
lsof -ti:8080 2>/dev/null | xargs kill -9 2>/dev/null && echo "  ✓ 后端 (8080) 已停止" || echo "  - 后端未运行"
lsof -ti:3017 2>/dev/null | xargs kill -9 2>/dev/null && echo "  ✓ 前端 (3017) 已停止" || echo "  - 前端未运行"
lsof -ti:5002 2>/dev/null | xargs kill -9 2>/dev/null && echo "  ✓ SSB (5002) 已停止" || echo "  - SSB 未运行"
lsof -ti:5100 2>/dev/null | xargs kill -9 2>/dev/null && echo "  ✓ AC (5100) 已停止" || echo "  - AC 未运行"
echo "[✓] 全部停止完成"
