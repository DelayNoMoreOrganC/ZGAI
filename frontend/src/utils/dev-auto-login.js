/**
 * 开发环境自动登录工具
 * 用于本地开发时自动登录，避免手动输入密码
 */

import { ElMessage } from 'element-plus'
import axios from 'axios'

// 开发环境默认账号
const DEV_CREDENTIALS = {
  username: 'admin',
  password: 'admin123'
}

// 检查是否为开发环境
const isDevelopment = () => {
  return import.meta.env.MODE === 'development' ||
         import.meta.env.DEV ||
         window.location.hostname === 'localhost'
}

// 自动登录
export const autoLogin = async () => {
  // 只在开发环境执行
  if (!isDevelopment()) {
    return false
  }

  // 检查是否已登录
  const existingToken = localStorage.getItem('token')
  if (existingToken) {
    console.log('✅ 已存在登录token，跳过自动登录')
    return true
  }

  try {
    console.log('🔧 开发环境自动登录中...')

    const response = await axios.post('/api/auth/login', DEV_CREDENTIALS, {
      baseURL: import.meta.env.VITE_APP_BASE_API || '/api',
      timeout: 5000
    })

    if (response.data.success && response.data.data.token) {
      // 存储登录信息
      localStorage.setItem('token', response.data.data.token)
      localStorage.setItem('userInfo', JSON.stringify(response.data.data))

      console.log('✅ 自动登录成功')

      // 显示提示（可选，调试时可以注释掉）
      // ElMessage.success('开发环境自动登录成功')

      return true
    } else {
      console.error('❌ 自动登录失败：响应格式异常', response.data)
      return false
    }
  } catch (error) {
    console.error('❌ 自动登录失败：', error.message)

    // 开发环境显示错误提示
    if (isDevelopment()) {
      ElMessage.warning(`自动登录失败：${error.message}，请手动登录`)
    }

    return false
  }
}

// 清除登录信息（用于调试）
export const clearAuth = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  console.log('🗑️ 登录信息已清除')
  // 自动刷新页面
  window.location.reload()
}

export default {
  autoLogin,
  clearAuth,
  isDevelopment
}
