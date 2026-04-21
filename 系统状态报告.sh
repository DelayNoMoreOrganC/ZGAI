#!/bin/bash
echo "=== 律所智能案件管理系统 - 实时状态 ==="
echo ""
echo "【前端服务】"
curl -s -o /dev/null -w "HTTP状态: %{http_code}\n" http://localhost:3017
echo ""
echo "【后端服务】"
curl -s -o /dev/null -w "HTTP状态: %{http_code}\n" http://localhost:8080/api/auth/login -X POST -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}'
echo ""
echo "【AI服务】"
curl -s http://localhost:11434/api/tags | grep -q "name" && echo "✅ Ollama AI运行中" || echo "❌ AI服务未启动"
echo ""
echo "【快速登录】"
TOKEN=$(curl -s http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)
if [ -n "$TOKEN" ]; then
  echo "✅ 登录成功 Token: ${TOKEN:0:30}..."
else
  echo "❌ 登录失败"
fi
echo ""
echo "【访问地址】"
echo "前端: http://localhost:3017"
echo "账号: admin / admin123"
