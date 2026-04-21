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

// AI对话
export function aiChat(data) {
  // 直接调用Ollama API（临时方案，绕过后端数据库问题）
  return fetch('http://localhost:11434/api/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      model: 'qwen3:8b',
      messages: [
        { role: 'system', content: '你是一个专业的法律助手，帮助律师处理案件、文档和日程管理。' },
        { role: 'user', content: data.message }
      ],
      stream: false
    })
  }).then(response => response.json())
    .then(result => {
      return {
        success: true,
        data: result.message?.content || result.response || 'AI响应为空'
      }
    })
    .catch(error => {
      console.error('Ollama调用失败:', error)
      return {
        success: false,
        message: 'AI服务暂时不可用'
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
