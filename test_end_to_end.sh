#!/bin/bash

echo "=== 端到端功能测试 ==="
echo ""

TOKEN=$(cat D:/ZGAI/backend/token.txt 2>/dev/null)

echo "1. 前端访问测试:"
curl -s "http://localhost:3017/" -I | grep "HTTP"

echo ""
echo "2. 创建案件流程:"
echo "- 创建案件"
CREATE_CASE=$(curl -s -X POST "http://localhost:8080/api/cases" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"caseNumber":"TEST2026001","caseName":"端到端测试案件","caseType":"CIVIL","clientName":"测试客户","clientPhone":"13800138000"}')
echo $CREATE_CASE | grep -o '"code":[0-9]*'

echo ""
echo "3. 创建日程:"
echo "- 创建日程事件"
CREATE_CAL=$(curl -s -X POST "http://localhost:8080/api/calendar" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"title":"测试日程","calendarType":"meeting","startTime":"2026-04-20T10:00:00","endTime":"2026-04-20T11:00:00","location":"会议室"}')
echo $CREATE_CAL | grep -o '"code":[0-9]*'

echo ""
echo "4. 创建工作汇报:"
echo "- 提交周报"
CREATE_REPORT=$(curl -s -X POST "http://localhost:8080/api/work-reports" -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"title":"端到端测试周报","reportType":"WEEKLY","reportDate":"2026-04-19T10:00:00","workSummary":"测试内容"}')
echo $CREATE_REPORT | grep -o '"code":[0-9]*'

echo ""
echo "=== 端到端测试完成 ==="
