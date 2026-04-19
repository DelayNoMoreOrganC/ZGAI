import request from '@/utils/request'

// 获取用户列表
export function getUserList(params) {
  return request({
    url: '/users',
    method: 'get',
    params
  })
}

// 获取用户详情
export function getUserDetail(id) {
  return request({
    url: `/users/${id}`,
    method: 'get'
  })
}

// 新增用户
export function createUser(data) {
  return request({
    url: '/users',
    method: 'post',
    data
  })
}

// 更新用户
export function updateUser(id, data) {
  return request({
    url: `/users/${id}`,
    method: 'put',
    data
  })
}

// 删除用户
export function deleteUser(id) {
  return request({
    url: `/users/${id}`,
    method: 'delete'
  })
}

// 切换用户状态
export function toggleUserStatus(id, status) {
  return request({
    url: `/users/${id}/status`,
    method: 'put',
    params: { status }
  })
}

// 重置密码
export function resetPassword(id, data) {
  return request({
    url: `/users/${id}/reset-password`,
    method: 'put',
    data
  })
}

// 修改密码
export function changePassword(data) {
  return request({
    url: '/users/change-password',
    method: 'put',
    data
  })
}

// 分配角色
export function assignRoles(id, data) {
  return request({
    url: `/users/${id}/roles`,
    method: 'put',
    data
  })
}
