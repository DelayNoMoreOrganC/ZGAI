#!/bin/bash
echo "=== 场景1：新建案件流程 ==="
TOKEN=$(curl -s http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

echo "1. 登录：${TOKEN:0:20}..."
echo ""

echo "2. 获取案件列表："
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/cases | grep -o '"total":[0-9]*'
echo ""

echo "3. 创建测试案件："
RESPONSE=$(curl -s -X POST http://localhost:8080/api/cases \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "caseName": "测试案件-用户场景",
    "caseType": "民事",
    "caseReason": "合同纠纷",
    "procedure": "一审",
    "level": "一般",
    "court": "北京市朝阳区人民法院",
    "summary": "这是一个测试案件"
  }')

echo $RESPONSE | grep -q "success\|200" && echo "✅ 案件创建成功" || echo "❌ 案件创建失败"
echo ""

echo "4. 验证案件在列表中："
curl -s -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/cases | grep -o "测试案件-用户场景" && echo "✅ 案件出现在列表中" || echo "❌ 案件未出现在列表中"
echo ""

echo "=== 场景2：AI问答测试 ==="
echo "1. 使用AI问答（英文问题避免编码问题）："
RAG_RESPONSE=$(curl -s -X POST http://localhost:8080/api/knowledge/rag/search \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"question":"What is civil litigation?"}')

echo $RAG_RESPONSE | grep -q "hasAnswer" && echo "✅ AI问答可访问" || echo "❌ AI问答失败"
echo ""

echo "=== 测试完成 ==="
