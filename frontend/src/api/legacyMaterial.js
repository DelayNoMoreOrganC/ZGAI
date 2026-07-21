import request from '@/utils/request'

export function searchLegacyMaterials(data) {
  return request({
    url: '/legacy-materials/search',
    method: 'post',
    data
  })
}
