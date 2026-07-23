import request from '@/utils/request'

// 获取知识库列表
export function getKnowledgeList(params) {
  return request({
    url: '/knowledge/list',
    method: 'get',
    params
  })
}

// 搜索知识库
export function searchKnowledge(keyword, params = {}) {
  return request({
    url: '/knowledge/search',
    method: 'get',
    params: {
      keyword,
      ...params
    }
  })
}

// 获取知识库详情
export function getKnowledgeDetail(id) {
  return request({
    url: `/knowledge/${id}`,
    method: 'get'
  })
}

// 创建知识库文章
export function createKnowledge(data) {
  return request({
    url: '/knowledge',
    method: 'post',
    data
  })
}

// 导入知识文档原件并提取正文
export function importKnowledgeDocument(data) {
  return request({
    url: '/knowledge/import',
    method: 'post',
    data,
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    timeout: 300000
  })
}

// 下载知识文档原件
export function downloadKnowledgeAttachment(id) {
  return request({
    url: `/knowledge/${id}/attachment`,
    method: 'get',
    responseType: 'blob'
  })
}

// 更新知识库文章
export function updateKnowledge(id, data) {
  return request({
    url: `/knowledge/${id}`,
    method: 'put',
    data
  })
}

// 删除知识库文章
export function deleteKnowledge(id) {
  return request({
    url: `/knowledge/${id}`,
    method: 'delete'
  })
}

// 获取知识库分类
export function getKnowledgeCategories() {
  return request({
    url: '/knowledge/categories',
    method: 'get'
  })
}

// AI知识问答 (RAG)
export function askAI(question, options = {}) {
  return request({
    url: '/knowledge/rag/search',
    method: 'post',
    data: {
      question,
      ...options
    },
    timeout: 300000
  })
}

export function previewFlkImport(urls) {
  return request({ url: '/knowledge/import-batches/flk/preview', method: 'post', data: { urls } })
}

export function createStarterFlkImport() {
  return request({ url: '/knowledge/import-batches/flk/starter', method: 'post' })
}

export function stageKnowledgeImport(batchId) {
  return request({ url: `/knowledge/import-batches/${batchId}/stage`, method: 'post', timeout: 600000 })
}

export function scanFirmPolicies() {
  return request({ url: '/knowledge/import-batches/firm-policies/scan', method: 'post', timeout: 120000 })
}

export function confirmKnowledgeImport(batchId, itemIds = []) {
  return request({ url: `/knowledge/import-batches/${batchId}/confirm`, method: 'post', data: { itemIds }, timeout: 600000 })
}

export function getKnowledgeImportBatches() {
  return request({ url: '/knowledge/import-batches', method: 'get' })
}

export function getPendingKnowledgeReviews(params = {}) {
  return request({ url: '/knowledge/pending-review', method: 'get', params })
}

export function getKnowledgeImportItems(batchId) {
  return request({ url: `/knowledge/import-batches/${batchId}/items`, method: 'get' })
}

export function uploadKnowledgeImportAttachment(itemId, file) {
  const data = new FormData()
  data.append('file', file)
  return request({
    url: `/knowledge/import-items/${itemId}/attachment`,
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}

export function reviewKnowledgeArticle(articleId, decision, reason = '') {
  return request({
    url: `/knowledge/articles/${articleId}/review`,
    method: 'post',
    data: { decision, reason }
  })
}
