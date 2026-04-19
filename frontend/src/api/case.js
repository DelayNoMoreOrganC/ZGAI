import request from '@/utils/request'

// 获取案件列表
export function getCaseList(params) {
  return request({
    url: '/cases',
    method: 'get',
    params
  })
}

// 获取案件详情
export function getCaseDetail(id) {
  return request({
    url: `/cases/${id}`,
    method: 'get'
  })
}

// 创建案件
export function createCase(data) {
  return request({
    url: '/cases',
    method: 'post',
    data
  })
}

// 更新案件
export function updateCase(id, data) {
  return request({
    url: `/cases/${id}`,
    method: 'put',
    data
  })
}

// 删除案件
export function deleteCase(id) {
  return request({
    url: `/cases/${id}`,
    method: 'delete'
  })
}

// 更新案件状态
export function updateCaseStatus(id, data) {
  return request({
    url: `/cases/${id}/status`,
    method: 'put',
    data: {
      targetStage: data.status,
      reason: data.reason || ''
    }
  })
}

// 归档案件
export function archiveCase(id, data) {
  return request({
    url: `/cases/${id}/archive`,
    method: 'put',
    data
  })
}

// 查重
export function checkDuplicate(params) {
  return request({
    url: '/cases/check-duplicate',
    method: 'get',
    params
  })
}

// 获取案件状态历史
export function getCaseStatusHistory(id) {
  return request({
    url: `/cases/${id}/status-history`,
    method: 'get'
  })
}

// 当事人管理
export function getParties(caseId) {
  return request({
    url: `/cases/${caseId}/parties`,
    method: 'get'
  })
}

export function createParty(caseId, data) {
  return request({
    url: `/cases/${caseId}/parties`,
    method: 'post',
    data
  })
}

export function updateParty(caseId, partyId, data) {
  return request({
    url: `/cases/${caseId}/parties/${partyId}`,
    method: 'put',
    data
  })
}

export function deleteParty(caseId, partyId) {
  return request({
    url: `/cases/${caseId}/parties/${partyId}`,
    method: 'delete'
  })
}

// 办案记录
export function getCaseRecords(caseId, params) {
  return request({
    url: `/cases/${caseId}/records`,
    method: 'get',
    params
  })
}

export function createCaseRecord(caseId, data) {
  return request({
    url: `/cases/${caseId}/records`,
    method: 'post',
    data
  })
}

export function updateCaseRecord(caseId, recordId, data) {
  return request({
    url: `/cases/${caseId}/records/${recordId}`,
    method: 'put',
    data
  })
}

export function deleteCaseRecord(caseId, recordId) {
  return request({
    url: `/cases/${caseId}/records/${recordId}`,
    method: 'delete'
  })
}

// 导出办案记录
export function exportCaseRecords(caseId, data) {
  return request({
    url: `/cases/${caseId}/records/export`,
    method: 'post',
    data,
    responseType: 'blob',
    headers: {
      'Accept': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    }
  })
}

// 导出办案记录(Word)
export function exportCaseRecordsWord(caseId, data) {
  return request({
    url: `/cases/${caseId}/records/export/word`,
    method: 'post',
    data,
    responseType: 'blob',
    headers: {
      'Accept': 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    }
  })
}

// 案件动态
export function getCaseTimeline(caseId) {
  return request({
    url: `/cases/${caseId}/timeline`,
    method: 'get'
  })
}

export function createTimelineComment(caseId, data) {
  return request({
    url: `/cases/${caseId}/timeline`,
    method: 'post',
    data
  })
}

export function deleteTimelineComment(caseId, timelineId) {
  return request({
    url: `/cases/${caseId}/timeline/${timelineId}`,
    method: 'delete'
  })
}

// 一键归档PDF
export function archiveCasePDF(id) {
  return request({
    url: `/cases/${id}/archive-pdf`,
    method: 'post',
    responseType: 'blob'
  })
}

// ==================== 案件文档管理 ====================

// 获取案件文档列表
export function getCaseDocuments(caseId) {
  return request({
    url: `/cases/${caseId}/documents`,
    method: 'get'
  })
}

// 上传案件文档
export function uploadCaseDocument(caseId, data) {
  return request({
    url: `/cases/${caseId}/documents`,
    method: 'post',
    data,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 获取文档详情
export function getCaseDocument(caseId, docId) {
  return request({
    url: `/cases/${caseId}/documents/${docId}`,
    method: 'get'
  })
}

// 更新文档信息
export function updateCaseDocument(caseId, docId, data) {
  return request({
    url: `/cases/${caseId}/documents/${docId}`,
    method: 'put',
    data
  })
}

// 移动文档到其他文件夹
export function moveCaseDocument(caseId, docId, folderPath) {
  return request({
    url: `/cases/${caseId}/documents/${docId}/move`,
    method: 'put',
    params: { folderPath }
  })
}

// 删除文档
export function deleteCaseDocument(caseId, docId) {
  return request({
    url: `/cases/${caseId}/documents/${docId}`,
    method: 'delete'
  })
}

// 根据类型获取文档列表
export function getDocumentsByType(caseId, documentType) {
  return request({
    url: `/cases/${caseId}/documents/type/${documentType}`,
    method: 'get'
  })
}

// 下载文档
export function downloadCaseDocument(caseId, docId) {
  return request({
    url: `/cases/${caseId}/documents/${docId}/download`,
    method: 'get',
    responseType: 'blob'
  })
}

// ==================== 案件程序管理 ====================

// 获取案件程序列表
export function getCaseProcedures(caseId) {
  return request({
    url: `/cases/${caseId}/procedures`,
    method: 'get'
  })
}

// 创建案件程序
export function createCaseProcedure(caseId, data) {
  return request({
    url: `/cases/${caseId}/procedures`,
    method: 'post',
    data
  })
}

// 更新案件程序
export function updateCaseProcedure(caseId, procedureId, data) {
  return request({
    url: `/cases/${caseId}/procedures/${procedureId}`,
    method: 'put',
    data
  })
}

// 删除案件程序
export function deleteCaseProcedure(caseId, procedureId) {
  return request({
    url: `/cases/${caseId}/procedures/${procedureId}`,
    method: 'delete'
  })
}
