// 调试配置文件 - 用于验证环境变量是否正确加载
console.log('=== 配置调试信息 ===');
console.log('import.meta.env.VITE_APP_BASE_API:', import.meta.env.VITE_APP_BASE_API);
console.log('import.meta.env.MODE:', import.meta.env.MODE);
console.log('baseURL fallback:', '/api');

export const DEBUG_CONFIG = {
  baseAPI: import.meta.env.VITE_APP_BASE_API || '/api',
  mode: import.meta.env.MODE,
  timestamp: new Date().toISOString()
};
