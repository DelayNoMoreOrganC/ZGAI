import request from '@/utils/request'

// 获取审批列表
export function getApprovalList(params) {
  return request({
    url: '/approval',
    method: 'get',
    params,
    // ApprovalQueryRequest is intentionally 1-based; most legacy list APIs are 0-based.
    skipPageNormalization: true
  })
}

// 获取审批详情
export function getApprovalDetail(id) {
  return request({
    url: `/approval/${id}`,
    method: 'get'
  })
}

// 获取审批流程记录
export function getApprovalFlow(id) {
  return request({
    url: `/approval/${id}/flow`,
    method: 'get'
  })
}

// 创建审批
export function createApproval(data) {
  return request({
    url: '/approval',
    method: 'post',
    data
  })
}

// 发起公章用印审批（上传文件或引用案件文档）
export function createSealApproval(data) {
  return request({
    url: '/approval/seal',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

export function downloadApprovalAttachment(approvalId, attachmentId) {
  return request({
    url: `/approval/${approvalId}/attachments/${attachmentId}/download`,
    method: 'get',
    responseType: 'blob',
    timeout: 120000
  })
}

// 同意审批
export function approveApproval(id, data) {
  return request({
    url: `/approval/${id}/approve`,
    method: 'put',
    data
  })
}

// 驳回审批
export function rejectApproval(id, data) {
  return request({
    url: `/approval/${id}/reject`,
    method: 'put',
    data
  })
}

// 转审
export function transferApproval(id, data) {
  return request({
    url: `/approval/${id}/transfer`,
    method: 'put',
    data
  })
}

// 撤回审批
export function withdrawApproval(id) {
  return request({
    url: `/approval/${id}/withdraw`,
    method: 'put'
  })
}

// 催办
export function urgeApproval(id) {
  return request({
    url: `/approval/${id}/urge`,
    method: 'post'
  })
}
