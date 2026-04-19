import request from '@/utils/request'

// 获取通知列表
export function getNotificationList(params) {
  return request({
    url: '/notification',
    method: 'get',
    params
  })
}

// 获取未读通知数
export function getUnreadCount() {
  return request({
    url: '/notification/unread-count',
    method: 'get'
  })
}

// 标记已读
export function markAsRead(id) {
  return request({
    url: `/notification/${id}/read`,
    method: 'put'
  })
}

// 标记全部已读
export function markAllAsRead() {
  return request({
    url: '/notification/read-all',
    method: 'put'
  })
}

// 删除通知
export function deleteNotification(id) {
  return request({
    url: `/notification/${id}`,
    method: 'delete'
  })
}
