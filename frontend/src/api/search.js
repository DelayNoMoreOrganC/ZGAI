import request from '@/utils/request'

/**
 * 全局搜索
 * @param {string} keyword - 搜索关键词
 * @param {string} type - 搜索类型：all/case/client/document
 */
export function globalSearch(keyword, type = 'all') {
  return request({
    url: '/search',
    method: 'get',
    params: {
      q: encodeURIComponent(keyword),
      type: type
    }
  })
}