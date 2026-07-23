import request from '@/utils/request'

export const getArchiveReadiness = caseId => request({ url: `/cases/${caseId}/archive-readiness`, method: 'get' })
export const createArchiveJob = (caseId, data) => request({ url: `/cases/${caseId}/archive-jobs`, method: 'post', data })
export const getArchiveJobs = params => request({ url: '/archive-jobs', method: 'get', params })
export const getArchiveJob = jobId => request({ url: `/archive-jobs/${jobId}`, method: 'get' })
export const patchArchiveDocuments = (jobId, items) => request({ url: `/archive-jobs/${jobId}/documents`, method: 'patch', data: { items } })
export const uploadArchiveSupplement = (jobId, file, catalogSeq) => {
  const data = new FormData()
  data.append('file', file)
  data.append('catalogSeq', catalogSeq)
  return request({ url: `/archive-jobs/${jobId}/documents`, method: 'post', data, timeout: 300000 })
}
export const patchArchiveFields = (jobId, fields) => request({ url: `/archive-jobs/${jobId}/fields`, method: 'patch', data: { fields } })
export const submitArchiveJob = jobId => request({ url: `/archive-jobs/${jobId}/submit`, method: 'post' })
export const reviewArchiveJob = (jobId, data) => request({ url: `/archive-jobs/${jobId}/review`, method: 'post', data, timeout: 1800000 })
export const previewArchiveOutput = jobId => request({ url: `/archive-jobs/${jobId}/preview`, method: 'get', responseType: 'blob', timeout: 1800000 })
export const downloadArchiveOutput = jobId => request({ url: `/archive-jobs/${jobId}/download`, method: 'get', responseType: 'blob', timeout: 300000 })
