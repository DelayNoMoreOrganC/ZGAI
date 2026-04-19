// 模拟前端API调用
const fetch = require('node-fetch');

async function testFrontendAPI() {
  try {
    // 1. 登录
    const loginRes = await fetch('http://localhost:8080/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: 'admin', password: 'admin123' })
    });
    const loginData = await loginRes.json();
    const token = loginData.data.token;
    
    console.log('✅ 登录成功，token:', token.substring(0, 20) + '...');
    
    // 2. 获取待办
    const todosRes = await fetch('http://localhost:8080/api/todos/assignee/1', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const todosData = await todosRes.json();
    
    console.log('✅ 待办数量:', todosData.data.length);
    console.log('✅ 待办标题:', todosData.data.map(t => t.title).join(', '));
    
    // 3. 获取日程
    const calRes = await fetch('http://localhost:8080/api/calendar/user/1', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const calData = await calRes.json();
    
    console.log('✅ 日程数量:', calData.data.length);
    console.log('✅ 日程标题:', calData.data.map(c => c.title).join(', '));
    
  } catch (error) {
    console.error('❌ 错误:', error.message);
  }
}

testFrontendAPI();
