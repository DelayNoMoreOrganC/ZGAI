<template>
  <el-drawer
    v-model="visible"
    title="通知中心"
    direction="rtl"
    size="400px"
    :before-close="handleClose"
  >
    <template #header>
      <div class="notification-header">
        <span class="title">通知中心</span>
        <div class="actions">
          <el-button
            v-if="unreadCount > 0"
            type="primary"
            size="small"
            @click="markAllAsRead"
          >
            全部已读
          </el-button>
        </div>
      </div>
    </template>

    <div class="notification-content">
      <!-- 分类标签 -->
      <div class="category-tabs">
        <el-tabs v-model="activeCategory" @tab-change="handleCategoryChange">
          <el-tab-pane label="全部" name=""></el-tab-pane>
          <el-tab-pane label="待办提醒" name="TODO"></el-tab-pane>
          <el-tab-pane label="案件更新" name="CASE"></el-tab-pane>
          <el-tab-pane label="系统消息" name="SYSTEM"></el-tab-pane>
        </el-tabs>
      </div>

      <!-- 通知列表 -->
      <div v-loading="loading" class="notification-list">
        <div
          v-for="notification in notifications"
          :key="notification.id"
          class="notification-item"
          :class="{ 'unread': !notification.isRead }"
          @click="handleNotificationClick(notification)"
        >
          <div class="notification-icon">
            <span v-if="notification.category === 'TODO'">⏰</span>
            <span v-else-if="notification.category === 'CASE'">⚖️</span>
            <span v-else-if="notification.category === 'SYSTEM'">🔔</span>
            <span v-else>📢</span>
          </div>

          <div class="notification-body">
            <div class="notification-title">{{ notification.title }}</div>
            <div class="notification-content-text">{{ notification.content }}</div>
            <div class="notification-time">{{ formatTime(notification.createdAt) }}</div>
          </div>

          <div class="notification-actions">
            <el-button
              v-if="!notification.isRead"
              type="primary"
              size="small"
              link
              @click.stop="markAsRead(notification)"
            >
              标为已读
            </el-button>
            <el-button
              type="danger"
              size="small"
              link
              @click.stop="deleteNotification(notification)"
            >
              删除
            </el-button>
          </div>
        </div>

        <!-- 空状态 -->
        <el-empty
          v-if="!loading && notifications.length === 0"
          description="暂无通知"
          :image-size="100"
        />

        <!-- 加载更多 -->
        <div
          v-if="hasMore && !loading"
          class="load-more"
        >
          <el-button
            text
            @click="loadMore"
          >
            加载更多
          </el-button>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'update:unreadCount'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const notifications = ref([])
const activeCategory = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const hasMore = computed(() => notifications.value.length < total.value)

// 获取通知列表
const fetchNotifications = async (page = 1) => {
  loading.value = true
  try {
    const params = {
      page: page, // request.js拦截器会自动减1
      size: pageSize.value
    }

    if (activeCategory.value) {
      params.category = activeCategory.value
    }

    const response = await request.get('/notification', { params })

    if (response.data.code === 200) {
      const data = response.data.data
      if (page === 1) {
        notifications.value = data.records || []
      } else {
        notifications.value.push(...(data.records || []))
      }
      total.value = data.total || 0
      currentPage.value = page
    }
  } catch (error) {
    ElMessage.error('获取通知失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 加载更多
const loadMore = () => {
  fetchNotifications(currentPage.value + 1)
}

// 切换分类
const handleCategoryChange = () => {
  notifications.value = []
  fetchNotifications(1)
}

// 点击通知
const handleNotificationClick = (notification) => {
  if (!notification.isRead) {
    markAsRead(notification)
  }

  // 如果有关联数据，跳转到相关页面
  if (notification.relatedType === 'CASE' && notification.relatedId) {
    window.location.href = `/#/case/${notification.relatedId}`
  } else if (notification.relatedType === 'TODO' && notification.relatedId) {
    // 跳转到待办详情页（如果有的话）
    window.location.href = `/#/todos`
  }
}

// 标记为已读
const markAsRead = async (notification) => {
  try {
    const response = await request.put(`/notification/${notification.id}/read`)
    if (response.data.code === 200) {
      notification.isRead = true
      notification.readTime = new Date().toISOString()
      // 通知父组件更新未读数
      emit('update:unreadCount')
      fetchUnreadCount()
    }
  } catch (error) {
    ElMessage.error('标记失败')
    console.error(error)
  }
}

// 全部标记为已读
const markAllAsRead = async () => {
  try {
    const response = await request.put('/notification/read-all')
    if (response.data.code === 200) {
      notifications.value.forEach(n => {
        n.isRead = true
        n.readTime = new Date().toISOString()
      })
      ElMessage.success('已全部标记为已读')
      emit('update:unreadCount')
      fetchUnreadCount()
    }
  } catch (error) {
    ElMessage.error('操作失败')
    console.error(error)
  }
}

// 删除通知
const deleteNotification = async (notification) => {
  try {
    const response = await request.delete(`/notification/${notification.id}`)
    if (response.data.code === 200) {
      const index = notifications.value.findIndex(n => n.id === notification.id)
      if (index > -1) {
        notifications.value.splice(index, 1)
      }
      total.value--
      ElMessage.success('删除成功')
      if (!notification.isRead) {
        emit('update:unreadCount')
        fetchUnreadCount()
      }
    }
  } catch (error) {
    ElMessage.error('删除失败')
    console.error(error)
  }
}

// 格式化时间
const formatTime = (time) => {
  if (!time) return ''

  const date = new Date(time)
  const now = new Date()
  const diff = now - date

  // 小于1分钟
  if (diff < 60000) {
    return '刚刚'
  }
  // 小于1小时
  if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`
  }
  // 小于1天
  if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`
  }
  // 小于7天
  if (diff < 604800000) {
    return `${Math.floor(diff / 86400000)}天前`
  }
  // 其他
  return date.toLocaleDateString('zh-CN')
}

// 获取未读数量（外部调用）
const fetchUnreadCount = async () => {
  try {
    const response = await request.get('/notification/unread-count')
    if (response.data.code === 200) {
      emit('update:unreadCount', response.data.data)
    }
  } catch (error) {
    console.error('获取未读数失败:', error)
  }
}

// 关闭抽屉
const handleClose = () => {
  visible.value = false
}

// 监听抽屉打开
watch(visible, (newVal) => {
  if (newVal) {
    fetchNotifications(1)
  }
})

// 暴露方法给父组件
defineExpose({
  fetchUnreadCount
})
</script>

<style scoped lang="scss">
.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;

  .title {
    font-size: 16px;
    font-weight: 500;
    color: #333;
  }

  .actions {
    display: flex;
    gap: 10px;
  }
}

.notification-content {
  height: 100%;
  display: flex;
  flex-direction: column;

  .category-tabs {
    flex-shrink: 0;
    margin-bottom: 10px;
  }

  .notification-list {
    flex: 1;
    overflow-y: auto;
    padding-right: 10px;

    .notification-item {
      display: flex;
      gap: 12px;
      padding: 15px;
      margin-bottom: 10px;
      background-color: #fff;
      border: 1px solid #e8e8e8;
      border-radius: 4px;
      cursor: pointer;
      transition: all 0.3s;

      &:hover {
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      }

      &.unread {
        background-color: #f0f7ff;
        border-color: #1890ff;
      }

      .notification-icon {
        flex-shrink: 0;
        font-size: 24px;
        line-height: 1;
      }

      .notification-body {
        flex: 1;
        min-width: 0;

        .notification-title {
          font-size: 14px;
          font-weight: 500;
          color: #333;
          margin-bottom: 5px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        .notification-content-text {
          font-size: 13px;
          color: #666;
          margin-bottom: 8px;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }

        .notification-time {
          font-size: 12px;
          color: #999;
        }
      }

      .notification-actions {
        flex-shrink: 0;
        display: flex;
        flex-direction: column;
        gap: 5px;
        opacity: 0;
        transition: opacity 0.3s;
      }

      &:hover .notification-actions {
        opacity: 1;
      }
    }

    .load-more {
      text-align: center;
      padding: 10px 0;
    }
  }
}

// 滚动条样式
.notification-list::-webkit-scrollbar {
  width: 6px;
}

.notification-list::-webkit-scrollbar-thumb {
  background-color: #d9d9d9;
  border-radius: 3px;
}

.notification-list::-webkit-scrollbar-thumb:hover {
  background-color: #bfbfbf;
}
</style>
