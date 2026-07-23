import request from '@/utils/request'

export function getYuandianStatus() {
  return request({ url: '/legal-sources/yuandian/status', method: 'get' })
}

export function searchYuandianLaws(data) {
  return request({ url: '/legal-sources/yuandian/laws/search', method: 'post', data })
}

export function searchYuandianCases(data) {
  return request({ url: '/legal-sources/yuandian/cases/search', method: 'post', data })
}

export function verifyYuandianCitations(content) {
  return request({
    url: '/legal-sources/yuandian/citations/verify',
    method: 'post',
    data: { content },
    timeout: 120000
  })
}

export function importYuandianKnowledge(importToken) {
  return request({
    url: '/legal-sources/yuandian/knowledge/import',
    method: 'post',
    data: { importToken }
  })
}
