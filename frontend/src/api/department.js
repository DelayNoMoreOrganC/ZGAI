import request from '@/utils/request'

export function getDepartmentList() {
  return request({
    url: '/department',
    method: 'get'
  })
}
