// 配置文件 - 环境变量配置
export const DEBUG_CONFIG = {
  baseAPI: import.meta.env.VITE_APP_BASE_API || '/api',
  mode: import.meta.env.MODE,
  timestamp: new Date().toISOString()
};
