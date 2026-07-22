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
    timeout: 120000
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
    }
  })
}
