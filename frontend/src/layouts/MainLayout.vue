<template>
  <el-container class="main-layout" :class="{ 'is-mobile': isMobile }">
    <!-- 移动端遮罩层 -->
    <div
      v-if="isMobile && mobileSidebarVisible"
      class="sidebar-overlay"
      @click="closeMobileSidebar"
    ></div>

    <!-- 侧边栏 -->
    <el-aside
      :width="sidebarWidth"
      class="sidebar"
      :class="{ 'mobile-visible': mobileSidebarVisible }"
    >
      <div class="logo">
        <div class="window-dots" v-if="!isCollapse">
          <span class="dot red"></span>
          <span class="dot yellow"></span>
          <span class="dot green"></span>
        </div>
        <div class="brand-mark">Z</div>
        <div v-if="!isCollapse" class="brand-copy">
          <strong>ZGAI</strong>
          <span>案件与客户工作台</span>
        </div>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :unique-opened="true"
        router
        class="sidebar-menu"
        @select="closeMobileSidebar"
      >
        <template v-for="route in menuRoutes" :key="route.path">
          <el-sub-menu v-if="route.children && route.children.length > 0" :index="route.path">
            <template #title>
              <el-icon class="menu-icon"><component :is="getIconName(route.meta?.icon)" /></el-icon>
              <span>{{ route.meta?.title }}</span>
            </template>
            <el-menu-item
              v-for="child in route.children"
              :key="child.path"
              :index="child.path"
            >
              {{ child.meta?.title }}
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item v-else :index="route.path">
            <el-icon class="menu-icon"><component :is="getIconName(route.meta?.icon)" /></el-icon>
            <span>{{ route.meta?.title }}</span>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container class="main-container">
      <!-- 顶部栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-icon v-if="isMobile" class="collapse-btn" @click="toggleSidebar">
            <Expand />
          </el-icon>
          <el-breadcrumb v-if="!isMobile" separator="/">
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path" :to="item.path">
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
          <span v-else class="mobile-title">{{ currentPageTitle }}</span>
        </div>

        <div class="header-right">
          <!-- 全局搜索 -->
          <el-input
            v-if="!isMobile"
            v-model="searchKeyword"
            placeholder="搜索案件、客户..."
            prefix-icon="Search"
            class="search-input"
            clearable
            @input="handleSearchInput"
          />
          <el-button v-else circle class="header-btn" @click="handleSearch">
            <el-icon><Search /></el-icon>
          </el-button>

          <el-button v-if="!isMobile && showQuickCreateClient" class="quick-create" @click="router.push('/client/create')">
            <el-icon><UserFilled /></el-icon>
            新建客户
          </el-button>
          <el-button v-if="!isMobile && showQuickCreateCase" type="primary" class="quick-create primary" @click="router.push('/case/create')">
            <el-icon><FolderAdd /></el-icon>
            新建案件
          </el-button>

          <el-tooltip content="案件 AI 助手" placement="bottom">
            <el-button circle class="header-btn" @click="router.push('/ai/case-workbench')">
              <el-icon><MagicStick /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="审批与通知" placement="bottom">
            <el-button circle class="header-btn" @click="router.push('/approval')">
              <el-icon><Bell /></el-icon>
            </el-button>
          </el-tooltip>

          <!-- 用户头像 -->
          <el-dropdown @command="handleUserAction">
            <div class="user-avatar">
              <el-avatar v-if="!isMobile" :size="32" :src="userAvatar">
                {{ userName?.charAt(0) }}
              </el-avatar>
              <span v-if="!isMobile" class="user-name">{{ userName }}</span>
              <el-icon v-else><User /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item v-if="canAccessSettings" command="settings">系统设置</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容 -->
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade-transform" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>

  </el-container>
</template>

<script setup>
import { computed, ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const getIconName = icon => icon || 'Document'

// 响应式相关
const windowWidth = ref(window.innerWidth)
const isMobile = computed(() => windowWidth.value < 768)

// 移动端侧边栏抽屉状态
const mobileSidebarVisible = ref(false)

// 导航始终展示文字与内嵌子菜单，避免折叠模式把二级功能变成悬浮弹层。
const isCollapse = computed(() => false)

const sidebarWidth = computed(() => {
  if (isMobile.value) {
    return '0px' // 移动端完全隐藏
  }
  return '248px'
})

// 当前页面标题（移动端）
const currentPageTitle = computed(() => {
  const matched = route.matched.filter(item => item.meta && item.meta.title)
  return matched.length > 0 ? matched[matched.length - 1].meta.title : '律所系统'
})

// 当前激活的菜单
const activeMenu = computed(() => {
  const { path } = route
  const standaloneMenus = [
    '/case/create', '/case/archive', '/case/trash',
    '/client/create', '/client/conflict-check',
    '/finance/invoices', '/ai/case-workbench',
    '/knowledge/rag', '/knowledge/yuandian', '/knowledge/list'
  ]
  if (standaloneMenus.includes(path)) return path
  if (path.startsWith('/case/')) {
    return '/case/list'
  }
  if (path.startsWith('/client/')) {
    return '/client/list'
  }
  if (path === '/finance') return '/finance/invoices'
  return path
})

const isAdministrativeUser = computed(() => {
  const position = userStore.userInfo?.position || ''
  return position.startsWith('行政管理') || userStore.roles.some(role => String(role).includes('ADMINISTRATIVE'))
})

const isFinanceUser = computed(() => {
  const position = userStore.userInfo?.position || ''
  return position.includes('财务') || position === '出纳' || userStore.roles.includes('FINANCE')
})

const canUseLegalCalendar = computed(() => !isAdministrativeUser.value && !isFinanceUser.value)
const canCreateCase = computed(() => userStore.hasPermission('CASE_CREATE'))
const canCreateClient = computed(() => userStore.hasPermission('CLIENT_CREATE'))
const showQuickCreateCase = computed(() => canCreateCase.value && !route.path.startsWith('/case'))
const showQuickCreateClient = computed(() => canCreateClient.value && !route.path.startsWith('/client'))

// 菜单路由
const menuRoutes = computed(() => {
  const routes = [
    { path: '/dashboard', meta: { title: '工作台', icon: 'House' } },
    {
      path: '/calendar',
      meta: { title: '日程管理', icon: 'Calendar' },
      visible: canUseLegalCalendar.value
    },
    {
      path: '/case',
      meta: { title: '案件管理', icon: 'Files' },
      permission: 'CASE_VIEW',
      children: [
        { path: '/case/list', meta: { title: '全部案件' } },
        { path: '/case/create', meta: { title: '新建案件' }, permission: 'CASE_CREATE' },
        { path: '/case/archive', meta: { title: '归档中心' }, permission: 'CASE_VIEW' },
        { path: '/case/trash', meta: { title: '回收站' }, permission: 'CASE_DELETE' }
      ]
    },
    {
      path: '/client',
      meta: { title: '客户管理', icon: 'User' },
      permission: 'CLIENT_VIEW',
      children: [
        { path: '/client/list', meta: { title: '全部客户' } },
        { path: '/client/create', meta: { title: '新建客户' }, permission: 'CLIENT_CREATE' },
        { path: '/client/conflict-check', meta: { title: '利冲检查' } }
      ]
    },
    {
      path: '/finance',
      meta: { title: '财务管理', icon: 'Money' },
      children: [
        { path: '/finance/invoices', meta: { title: '发票申请' } }
      ]
    },
    {
      path: '/approval',
      meta: { title: '审批管理', icon: 'CircleCheck' },
      permission: 'APPROVAL_VIEW'
    },
    {
      path: '/ai',
      meta: { title: 'AI管理', icon: 'Cpu' },
      permission: 'CASE_VIEW',
      children: [
        { path: '/ai/case-workbench', meta: { title: '案件AI助手' } },
        { path: '/knowledge/rag', meta: { title: '知识问答' } }
      ]
    },
    {
      path: '/knowledge',
      meta: { title: '知识库管理', icon: 'Reading' },
      children: [
        { path: '/knowledge/yuandian', meta: { title: '元典法律检索' } },
        { path: '/knowledge/list', meta: { title: '法规/制度库' } }
      ]
    }
  ]

  return routes
    .filter(item => item.visible !== false && (!item.permission || userStore.hasPermission(item.permission)))
    .map(item => ({
      ...item,
      children: item.children?.filter(child => !child.permission || userStore.hasPermission(child.permission))
    }))
})

// 面包屑
const breadcrumbs = computed(() => {
  const matched = route.matched.filter(item => item.meta && item.meta.title)
  return matched.map(item => ({
    path: item.path,
    title: item.meta.title
  }))
})

// 用户信息
const userName = computed(() => userStore.userName)
const userAvatar = computed(() => userStore.userInfo?.avatar)
const canAccessSettings = computed(() => userStore.hasPermission('SYSTEM_CONFIG'))

// 搜索
const searchKeyword = ref('')
// 全局搜索（带debounce 300ms）
let searchDebounce = null
const handleSearchInput = () => {
  clearTimeout(searchDebounce)
  searchDebounce = setTimeout(() => {
    handleSearch()
  }, 300)
}

const handleSearch = () => {
  if (searchKeyword.value.trim() || isMobile.value) {
    router.push({ path: '/search', query: { q: searchKeyword.value } })
  }
}

// 切换侧边栏
const toggleSidebar = () => {
  mobileSidebarVisible.value = !mobileSidebarVisible.value
}

// 关闭移动端侧边栏
const closeMobileSidebar = () => {
  mobileSidebarVisible.value = false
}

// 用户操作
const handleUserAction = async (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'settings':
      router.push('/settings')
      break
    case 'logout':
      try {
        await ElMessageBox.confirm('确定要退出登录吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await userStore.logout()
        router.push('/login')
        ElMessage.success('已退出登录')
      } catch {
        // 用户取消
      }
      break
  }
}

// 监听窗口大小变化
const handleResize = () => {
  windowWidth.value = window.innerWidth

  // 窗口变大时，关闭移动端侧边栏
  if (!isMobile.value) {
    mobileSidebarVisible.value = false
  }
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)

  if (userStore.isLoggedIn) {
    try {
      await userStore.getUserInfo()
    } catch {
      await userStore.logout()
      router.push('/login')
      return
    }
  }

})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped lang="scss">
.main-layout {
  height: 100vh;
  width: 100vw;
  background: var(--zg-bg);
  color: #1d1d1f;

  .sidebar {
    background: rgba(245, 247, 250, 0.78);
    border-right: 1px solid rgba(196, 202, 211, 0.72);
    backdrop-filter: blur(22px);
    -webkit-backdrop-filter: blur(22px);
    transition: width 0.24s ease, transform 0.24s ease;
    overflow: hidden;
    position: relative;
    z-index: 1000;
    box-shadow: inset -1px 0 0 rgba(255, 255, 255, 0.65);

    .logo {
      height: 92px;
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 18px 18px 12px;
      color: #1d1d1f;
      border-bottom: 1px solid rgba(210, 214, 220, 0.6);
      white-space: nowrap;
      overflow: hidden;
      position: relative;

      .window-dots {
        position: absolute;
        top: 16px;
        left: 18px;
        display: flex;
        gap: 7px;

        .dot {
          width: 11px;
          height: 11px;
          border-radius: 50%;
          box-shadow: inset 0 0 0 1px rgba(0, 0, 0, 0.08);
        }

        .red { background: #ff5f57; }
        .yellow { background: #ffbd2e; }
        .green { background: #28c840; }
      }

      .brand-mark {
        width: 38px;
        height: 38px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
        margin-top: 18px;
        background: #30343b;
        color: #fff;
        font-size: 18px;
        font-weight: 700;
        box-shadow: 0 12px 24px rgba(41, 48, 58, 0.2);
      }

      .brand-copy {
        display: flex;
        flex-direction: column;
        gap: 2px;
        margin-top: 18px;
        min-width: 0;

        strong {
          font-size: 15px;
          line-height: 1.1;
          letter-spacing: 0;
          color: #17181c;
        }

        span {
          font-size: 12px;
          color: #777f8c;
        }
      }
    }

    .sidebar-menu {
      border-right: none;
      background: transparent;
      padding: 14px 10px;

      :deep(.el-menu-item),
      :deep(.el-sub-menu__title) {
        height: 38px;
        line-height: 38px;
        margin: 4px 0;
        border-radius: 7px;
        color: #4f5865;
        font-size: 14px;
        font-weight: 500;

        &:hover {
          background: rgba(255, 255, 255, 0.72);
          color: #111827;
        }

        &.is-active {
          background: rgba(255, 255, 255, 0.96);
          color: #111827;
          box-shadow:
            0 1px 2px rgba(17, 24, 39, 0.08),
            0 10px 22px rgba(31, 41, 55, 0.08);
        }
      }

      :deep(.el-sub-menu .el-menu) {
        background: transparent;
      }

      :deep(.el-sub-menu__icon-arrow) {
        color: #9ca3af;
      }

      .menu-icon {
        margin-right: 10px;
        font-size: 17px;
        color: #697386;
      }
    }

    .sidebar-footer {
      position: absolute;
      left: 14px;
      right: 14px;
      bottom: 16px;
      padding: 12px;
      border-radius: 8px;
      background: rgba(255, 255, 255, 0.62);
      border: 1px solid rgba(218, 222, 228, 0.8);

      .footer-label {
        display: block;
        font-size: 12px;
        font-weight: 600;
        color: #3f4652;
        margin-bottom: 4px;
      }

      .footer-text {
        display: block;
        font-size: 12px;
        line-height: 1.5;
        color: #7a838f;
      }
    }
  }

  .main-container {
    display: flex;
    flex-direction: column;
    min-width: 0;

    .header {
      background: rgba(255, 255, 255, 0.72);
      border-bottom: 1px solid rgba(218, 222, 228, 0.8);
      backdrop-filter: blur(20px);
      -webkit-backdrop-filter: blur(20px);
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 0 24px;
      height: 64px;
      box-shadow: 0 1px 0 rgba(255, 255, 255, 0.8);

      .header-left {
        display: flex;
        align-items: center;
        gap: 16px;
        min-width: 0;

        .collapse-btn {
          width: 32px;
          height: 32px;
          border-radius: 8px;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          font-size: 17px;
          cursor: pointer;
          color: #6b7280;
          transition: background-color 0.18s ease, color 0.18s ease;

          &:hover {
            background: #f1f3f6;
            color: #111827;
          }
        }

        :deep(.el-breadcrumb__inner) {
          color: #7b8491;
          font-weight: 500;
        }

        :deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
          color: #1f2937;
        }

        .mobile-title {
          font-size: 16px;
          font-weight: 600;
          color: #1f2937;
        }
      }

      .header-right {
        display: flex;
        align-items: center;
        gap: 10px;

        .search-input {
          width: 280px;

          :deep(.el-input__wrapper) {
            height: 36px;
            border-radius: 8px;
            background: rgba(242, 244, 247, 0.88);
            box-shadow: inset 0 0 0 1px rgba(224, 228, 233, 0.9);
          }
        }

        .quick-create {
          height: 36px;
          border-radius: 8px;
          border-color: rgba(211, 216, 224, 0.95);
          color: #374151;
          background: rgba(255, 255, 255, 0.72);
          font-weight: 500;

          &.primary {
            border: none;
            background: #1f2937;
            color: #fff;
            box-shadow: 0 8px 18px rgba(31, 41, 55, 0.16);
          }
        }

        .user-avatar {
          display: flex;
          align-items: center;
          gap: 8px;
          cursor: pointer;
          padding: 4px 8px;
          border-radius: 8px;
          transition: background-color 0.18s ease;

          &:hover {
            background-color: #f1f3f6;
          }

          .user-name {
            font-size: 14px;
            color: #374151;
          }
        }
      }
    }

    .main-content {
      background: transparent;
      overflow-y: auto;
      padding: 24px;

      :deep(.el-card),
      :deep(.info-section),
      :deep(.detail-header),
      :deep(.progress-section),
      :deep(.calendar-section),
      :deep(.todo-section),
      :deep(.ai-upload-section) {
        border-radius: 8px;
        border: 1px solid rgba(224, 228, 235, 0.86);
        box-shadow: 0 10px 30px rgba(31, 41, 55, 0.06);
      }
    }
  }

  // 移动端适配样式
  &.is-mobile {
    .sidebar {
      position: fixed;
      top: 0;
      left: 0;
      height: 100vh;
      width: 200px !important;
      transform: translateX(-100%);
      box-shadow: 12px 0 40px rgba(31, 41, 55, 0.18);

      &.mobile-visible {
        transform: translateX(0);
      }
    }

    .sidebar-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background-color: rgba(0, 0, 0, 0.5);
      z-index: 999;
    }

    .main-container {
      .header {
        padding: 0 15px;

        .header-right {
          gap: 10px;

          .search-input {
            width: auto;
          }
        }
      }

      .main-content {
        padding: 15px;
      }
    }
  }
}

// 响应式断点
@media (max-width: 768px) {
  .main-layout {
    .main-container {
      .header {
        .header-left {
          gap: 10px;

          .collapse-btn {
            font-size: 18px;
          }
        }

        .header-right {
          gap: 8px;

          .header-btn {
            padding: 8px;
          }
        }
      }
    }
  }
}

@media (min-width: 769px) and (max-width: 1199px) {
  .main-layout {
    .sidebar {
      width: 248px !important;
    }

    .main-container {
      .header {
        .search-input {
          width: 180px;
        }

        .quick-create {
          display: none;
        }
      }
    }
  }
}

@media (min-width: 769px) and (max-width: 900px) {
  .main-layout .main-container .header {
    .search-input,
    .user-name {
      display: none;
    }
  }
}

// 页面切换动画
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(30px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}
</style>
