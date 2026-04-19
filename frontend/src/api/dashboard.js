import request from '@/utils/request'

// 获取工作台统计数据
export function getDashboardStats(userId) {
  return request({
    url: '/dashboard/stats',
    method: 'get',
    params: { userId }
  })
}

// 获取用户工作台完整数据
export function getUserDashboard(userId) {
  return request({
    url: '/dashboard',
    method: 'get',
    params: { userId }
  })
}
