import request from '@/utils/request'

/**
 * 类案检索API
 */

/**
 * 智能类案检索
 * @param {Object} data - 检索条件
 * @param {string} data.caseReason - 案由
 * @param {string} data.caseType - 案件类型
 * @param {number} data.amount - 争议金额
 * @param {string} data.court - 管辖法院
 * @param {number} data.excludeCaseId - 排除的案例ID
 * @param {number} data.limit - 返回结果数量限制
 */
export function searchSimilarCases(data) {
  return request({
    url: '/case-search/similar',
    method: 'post',
    data
  })
}

/**
 * 根据案件ID检索相似案例
 * @param {number} caseId - 目标案件ID
 * @param {number} limit - 返回结果数量限制
 */
export function searchSimilarByCaseId(caseId, limit = 10) {
  return request({
    url: `/case-search/similar/${caseId}`,
    method: 'get',
    params: { limit }
  })
}