import request from '@/utils/request'

export const listLawFirmLetters = caseId => request({
  url: `/cases/${caseId}/law-firm-letters`,
  method: 'get'
})

export const createLawFirmLetter = caseId => request({
  url: `/cases/${caseId}/law-firm-letters`,
  method: 'post'
})

export const updateLawFirmLetter = (id, data) => request({
  url: `/law-firm-letters/${id}`,
  method: 'put',
  data
})

export const cancelLawFirmLetter = id => request({
  url: `/law-firm-letters/${id}`,
  method: 'delete'
})

export const submitLawFirmLetter = id => request({
  url: `/law-firm-letters/${id}/submit`,
  method: 'post'
})

export const downloadLawFirmLetter = id => request({
  url: `/law-firm-letters/${id}/docx`,
  method: 'get',
  responseType: 'blob',
  timeout: 120000
})
