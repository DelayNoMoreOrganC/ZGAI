import request from '@/utils/request'

export const createCaseClosureRequest = (caseId, data) => request({
  url: `/cases/${caseId}/closure-requests`,
  method: 'post',
  data
})

export const getLatestCaseClosureRequest = caseId => request({
  url: `/cases/${caseId}/closure-requests/latest`,
  method: 'get'
})
