import request from '@/utils/request'

// 获取全部文档（跨案件聚合视图）
export function getAllDocuments(params) {
  return request({
    url: '/documents',
    method: 'get',
    params
  })
}

// 获取文档详情
export function getDocument(id) {
  return request({
    url: `/documents/${id}`,
    method: 'get'
  })
}

// 下载文档（需要后端实现下载接口）
export function downloadDocument(id) {
  return request({
    url: `/documents/${id}/download`,
    method: 'get',
    responseType: 'blob'
  })
}

// 删除文档（需要后端实现）
export function deleteDocument(id) {
  return request({
    url: `/documents/${id}`,
    method: 'delete'
  })
}
