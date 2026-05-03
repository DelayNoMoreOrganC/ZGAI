import request, { longTimeoutService } from '@/utils/request'

/**
 * AI文档智能识别
 */

/**
 * 智能识别法院文书（新接口）
 * @param {File} file - 文档文件
 * @param {Number} caseId - 关联案件ID（可选）
 * @returns {Promise}
 */
export function recognizeLegalDocument(file, caseId) {
  const formData = new FormData()
  formData.append('file', file)
  if (caseId) {
    formData.append('caseId', caseId)
  }

  return request({
    url: '/ai/documents/recognize',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 批量识别文档（新接口）
 * @param {File[]} files - 文档文件列表
 * @param {Number} caseId - 关联案件ID（可选）
 * @returns {Promise}
 */
export function recognizeLegalDocumentsBatch(files, caseId) {
  const formData = new FormData()
  files.forEach(file => {
    formData.append('files', file)
  })
  if (caseId) {
    formData.append('caseId', caseId)
  }

  return request({
    url: '/ai/documents/recognize-batch',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// OCR上传（保留旧接口以兼容）
export function ocrUpload(file) {
  return recognizeLegalDocument(file)
}

// AI提取（保留旧接口以兼容）
export function extractInfo(data) {
  return request({
    url: '/ai/extract',
    method: 'post',
    data
  })
}

// 自动填充案件
export function autoFillCase(caseId, data) {
  return request({
    url: `/ai/auto-fill/${caseId}`,
    method: 'post',
    data
  })
}

// 智能文档上传 - AI识别并创建待办/任务/日程/日志
export function uploadDocForAIRecognition(file) {
  const formData = new FormData()
  formData.append('file', file)

  // 使用长超时服务（120秒），因为AI识别需要较长时间
  return longTimeoutService({
    url: '/ai/documents/recognize',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 生成文书
export function generateDoc(data) {
  return request({
    url: '/ai/generate-doc',
    method: 'post',
    data,
    responseType: 'blob'
  })
}

// AI对话 - 通过后端代理调用（自动使用默认AI模式）
export function aiChat(data) {
  return request({
    url: '/ai/assist',
    method: 'post',
    data
  }).then(response => {
    // response = {code: 200, message, data: "AI回复内容"}
    // 提取 AI 回复文本
    const reply = typeof response === 'string' ? response : (response.data || response)
    return {
      success: true,
      data: reply || 'AI响应为空'
    }
  }).catch(error => {
    console.error('AI调用失败:', error)
    const msg = (error.response?.data?.message || error.message || '未知错误')
    // 如果是401，不弹错误（拦截器会处理登录跳转）
    if (msg.includes('401') || msg.includes('登录')) {
      return { success: false, message: '请先登录' }
    }
    return {
      success: false,
      message: 'AI服务暂时不可用: ' + msg
    }
  })
}

// 案件上下文对话
export function caseChat(caseId, data) {
  return request({
    url: `/ai/case-chat/${caseId}`,
    method: 'post',
    data
  })
}

// 获取AI使用日志
export function getAiLogs(params) {
  return request({
    url: '/ai/logs',
    method: 'get',
    params
  })
}

// 获取AI配置
export function getAiConfig() {
  return request({
    url: '/ai/config',
    method: 'get'
  })
}

// 更新AI配置
export function updateAiConfig(data) {
  return request({
    url: '/ai/config',
    method: 'put',
    data
  })
}
