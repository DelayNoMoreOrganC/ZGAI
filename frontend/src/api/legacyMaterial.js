import request from '@/utils/request'

export function searchLegacyMaterials(data) {
  return request({
    url: '/legacy-materials/search',
    method: 'post',
    data
  })
}

export function downloadLegacyMaterial(resultId) {
  return request({
    url: `/legacy-materials/files/${resultId}/download`,
    method: 'get',
    responseType: 'blob'
  })
}
