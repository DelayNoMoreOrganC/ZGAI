import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', {
  state: () => ({
    sidebar: {
      opened: true,
      withoutAnimation: false
    },
    device: 'desktop',
    size: localStorage.getItem('size') || 'default',
    language: localStorage.getItem('language') || 'zh-cn'
  }),

  getters: {
    isSidebarOpened: (state) => state.sidebar.opened,
    isMobile: (state) => state.device === 'mobile'
  },

  actions: {
    // 切换侧边栏
    toggleSidebar() {
      this.sidebar.opened = !this.sidebar.opened
      this.sidebar.withoutAnimation = false
    },

    // 关闭侧边栏
    closeSidebar(withoutAnimation = false) {
      this.sidebar.opened = false
      this.sidebar.withoutAnimation = withoutAnimation
    },

    // 切换设备
    toggleDevice(device) {
      this.device = device
    },

    // 设置组件尺寸
    setSize(size) {
      this.size = size
      localStorage.setItem('size', size)
    },

    // 设置语言
    setLanguage(language) {
      this.language = language
      localStorage.setItem('language', language)
    }
  }
})
