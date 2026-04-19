import request from '@/utils/request'

// 获取日程列表
export function getCalendarList(params) {
  return request({
    url: '/calendar',
    method: 'get',
    params
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
