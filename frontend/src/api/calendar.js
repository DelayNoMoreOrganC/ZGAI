import request from '@/utils/request'

// 获取日程列表（兼容前端调用）
export function getCalendarList(params) {
  // 判断是获取事件列表还是分页查询
  if (params.startDate && params.endDate) {
    // 使用事件列表接口
    return request({
      url: '/calendar/events',
      method: 'get',
      params: {
        start: params.startDate,
        end: params.endDate
      }
    })
  }

  // 使用分页接口
  return request({
    url: '/calendar',
    method: 'get',
    params: {
      page: params.page || 0,
      size: params.size || 10,
      userId: params.userId
    }
  })
}

// 创建日程
export function createCalendar(data) {
  return request({
    url: '/calendar',
    method: 'post',
    data
  })
}

// 更新日程
export function updateCalendar(id, data) {
  return request({
    url: `/calendar/${id}`,
    method: 'put',
    data
  })
}

// 删除日程
export function deleteCalendar(id) {
  return request({
    url: `/calendar/${id}`,
    method: 'delete'
  })
}

// Export aliases for calendar events - compatibility with component imports
export { getCalendarList as getCalendarEvents }
export { createCalendar as createEvent }
export { updateCalendar as updateEvent }
export { deleteCalendar as deleteEvent }
