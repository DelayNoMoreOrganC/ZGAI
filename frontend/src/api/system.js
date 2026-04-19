import request from '@/utils/request'

// 用户管理
export function getUserList(params) {
  return request({
    url: '/users',
    method: 'get',
    params
  })
}

export function getUserDetail(id) {
  return request({
    url: `/users/${id}`,
    method: 'get'
  })
}

export function createUser(data) {
  return request({
    url: '/users',
    method: 'post',
    data
  })
}

export function updateUser(id, data) {
  return request({
    url: `/users/${id}`,
    method: 'put',
    data
  })
}

export function deleteUser(id) {
  return request({
    url: `/users/${id}`,
    method: 'delete'
  })
}

export function resetPassword(id) {
  return request({
    url: `/users/${id}/reset-password`,
    method: 'post'
  })
}

// 角色管理
export function getRoleList(params) {
  return request({
    url: '/roles',
    method: 'get',
    params
  })
}

export function createRole(data) {
  return request({
    url: '/roles',
    method: 'post',
    data
  })
}

export function updateRole(id, data) {
  return request({
    url: `/roles/${id}`,
    method: 'put',
    data
  })
}

export function deleteRole(id) {
  return request({
    url: `/roles/${id}`,
    method: 'delete'
  })
}

// 审计日志
export function getAuditLogs(params) {
  return request({
    url: '/audit-logs',
    method: 'get',
    params
  })
}

// 系统配置
export function getSystemConfig() {
  return request({
    url: '/system/config',
    method: 'get'
  })
}

export function updateSystemConfig(data) {
  return request({
    url: '/system/config',
    method: 'put',
    data
  })
}

// 数据备份
export function createBackup() {
  return request({
    url: '/system/backup',
    method: 'post'
  })
}
