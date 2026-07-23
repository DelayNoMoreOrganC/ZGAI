import request from '@/utils/request'

// 获取客户列表
export function getClientList(params) {
  return request({
    url: '/clients',
    method: 'get',
    params
  })
}

// 获取客户详情
export function getClientDetail(id) {
  return request({
    url: `/clients/${id}`,
    method: 'get'
  })
}

// 创建客户
export function createClient(data) {
  return request({
    url: '/clients',
    method: 'post',
    data
  })
}

// 更新客户
export function updateClient(id, data) {
  return request({
    url: `/clients/${id}`,
    method: 'put',
    data
  })
}

// 删除客户
export function deleteClient(id) {
  return request({
    url: `/clients/${id}`,
    method: 'delete'
  })
}

// 搜索客户
export function searchClients(keyword) {
  return request({
    url: '/clients/search',
    method: 'get',
    params: { keyword }
  })
}

// 获取客户的案件
export function getClientCases(id) {
  return request({
    url: `/clients/${id}/cases`,
    method: 'get'
  })
}

// 获取沟通记录
export function getCommunications(clientId, params) {
  return request({
    url: `/clients/${clientId}/communications`,
    method: 'get',
    params
  })
}

// 创建沟通记录
export function createCommunication(clientId, data) {
  return request({
    url: `/clients/${clientId}/communications`,
    method: 'post',
    data
  })
}

// 更新沟通记录
export function updateCommunication(clientId, communicationId, data) {
  return request({
    url: `/clients/${clientId}/communications/${communicationId}`,
    method: 'put',
    data
  })
}

// 删除沟通记录
export function deleteCommunication(clientId, communicationId) {
  return request({
    url: `/clients/${clientId}/communications/${communicationId}`,
    method: 'delete'
  })
}

// 利益冲突检索
export function conflictCheck(id) {
  return request({
    url: `/clients/${id}/conflict-check`,
    method: 'get'
  })
}

// 客户建档前利益冲突预检
export function previewConflictCheck(data) {
  return request({
    url: '/clients/conflict-check',
    method: 'post',
    data
  })
}

// 查询利冲检查历史
export function getConflictCheckRecords(subjectName) {
  return request({
    url: '/clients/conflict-check/records',
    method: 'get',
    params: { subjectName }
  })
}

// 下载利冲检查留档报告
export function downloadConflictCheckReport(id) {
  return request({
    url: `/clients/conflict-check/records/${id}/report`,
    method: 'get',
    responseType: 'blob'
  })
}

// 行政人员提交正式利冲审查结论
export function reviewConflictCheckRecord(id, data) {
  return request({
    url: `/clients/conflict-check/records/${id}/review`,
    method: 'put',
    data
  })
}

export function uploadConflictWaiverAttachment(id, file) {
  const data = new FormData()
  data.append('file', file)
  return request({
    url: `/clients/conflict-check/records/${id}/waiver-attachments`,
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function downloadConflictWaiverAttachment(recordId, attachmentId) {
  return request({
    url: `/clients/conflict-check/records/${recordId}/waiver-attachments/${attachmentId}/download`,
    method: 'get',
    responseType: 'blob'
  })
}

export function getClientRelations(clientId) {
  return request({ url: `/clients/${clientId}/relations`, method: 'get' })
}

export function createClientRelation(clientId, data) {
  return request({ url: `/clients/${clientId}/relations`, method: 'post', data })
}

export function deleteClientRelation(clientId, relationId) {
  return request({ url: `/clients/${clientId}/relations/${relationId}`, method: 'delete' })
}

// 别名函数，保持向后兼容
export function checkConflict(id) {
  return conflictCheck(id)
}
