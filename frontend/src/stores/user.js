import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi, getCurrentUser } from '@/api/auth'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null'),
    permissions: JSON.parse(localStorage.getItem('permissions') || '[]'),
    roles: JSON.parse(localStorage.getItem('roles') || '[]'),
    sessionValidated: false
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    userName: (state) => state.userInfo?.realName || state.userInfo?.username || '',
    userId: (state) => state.userInfo?.id || '',
    userRole: (state) => state.userInfo?.role || '',
    requiresPasswordChange: (state) => state.userInfo?.mustChangePassword === true,
    hasPermission: (state) => (permission) => state.permissions.includes(permission)
  },

  actions: {
    // 登录
    async login(loginForm) {
      try {
        const res = await loginApi(loginForm)
        this.token = res.data.token
        // 后端直接返回用户信息，不是 res.data.user
        this.userInfo = {
          id: res.data.userId,
          username: res.data.username,
          realName: res.data.realName,
          email: res.data.email,
          phone: res.data.phone,
          avatar: res.data.avatar,
          departmentId: res.data.departmentId,
          position: res.data.position,
          mustChangePassword: res.data.mustChangePassword === true
        }
        this.permissions = res.data.permissions || []
        this.roles = res.data.roles || []
        this.sessionValidated = true

        localStorage.setItem('token', this.token)
        localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
        localStorage.setItem('permissions', JSON.stringify(this.permissions))
        localStorage.setItem('roles', JSON.stringify(this.roles))

        return res
      } catch (error) {
        throw error
      }
    },

    // 获取用户信息
    async getUserInfo() {
      try {
        const res = await getCurrentUser()
        this.userInfo = {
          id: res.data.userId,
          username: res.data.username,
          realName: res.data.realName,
          email: res.data.email,
          phone: res.data.phone,
          avatar: res.data.avatar,
          departmentId: res.data.departmentId,
          position: res.data.position,
          mustChangePassword: res.data.mustChangePassword === true
        }
        this.permissions = res.data.permissions || []
        this.roles = res.data.roles || []
        this.sessionValidated = true
        localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
        localStorage.setItem('permissions', JSON.stringify(this.permissions))
        localStorage.setItem('roles', JSON.stringify(this.roles))
        return res
      } catch (error) {
        throw error
      }
    },

    // 登出
    async logout() {
      try {
        await logoutApi()
      } catch (error) {
        console.error('登出失败:', error)
      } finally {
        this.token = ''
        this.userInfo = null
        this.permissions = []
        this.roles = []
        this.sessionValidated = false
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
        localStorage.removeItem('permissions')
        localStorage.removeItem('roles')
      }
    },

    // 更新用户信息
    updateUserInfo(data) {
      this.userInfo = { ...this.userInfo, ...data }
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
    }
  }
})
