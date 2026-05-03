import request from '@/utils/request'

// ============================================================
// 省时宝 (SSB) — 法律文档自动生成
// ============================================================

/**
 * 获取模板项目列表
 */
export function getSsbTemplates() {
  return request({
    url: '/external/shengshibao/templates',
    method: 'get'
  })
}

/**
 * 获取模板文件列表
 * @param {string} projectPath - 模板项目路径（需编码）
 */
export function getSsbTemplateFiles(projectPath) {
  return request({
    url: `/external/shengshibao/templates/${encodeURIComponent(projectPath)}/files`,
    method: 'get'
  })
}

/**
 * 获取模板字段定义
 * @param {string} projectPath - 模板项目路径
 */
export function getSsbTemplateFields(projectPath) {
  return request({
    url: `/external/shengshibao/templates/${encodeURIComponent(projectPath)}/fields`,
    method: 'get'
  })
}

/**
 * 生成文档
 * @param {object} data - { project_path, extracted_data: { 字段名: 值 } }
 */
export function generateSsbDocument(data) {
  return request({
    url: '/external/shengshibao/generate',
    method: 'post',
    data
  })
}

/**
 * 省时宝健康检查
 */
export function getSsbHealth() {
  return request({
    url: '/external/shengshibao/health',
    method: 'get'
  })
}

// ============================================================
// AC 精算 — 债权精算引擎
// ============================================================

/**
 * 单笔债权精算
 * @param {object} data
 * {
 *   principal: 1000000.00,        // 本金
 *   annual_rate: 0.05,            // 年利率
 *   penalty_rate: 0.07,           // 罚息年利率（可选）
 *   start_date: "2020-01-01",     // 起算日
 *   end_date: "2024-06-30",       // 截止日
 *   repayment_records: [          // 还款记录（可选）
 *     { date: "2023-06-21", amount: 50000.00, type: "normal" }
 *   ],
 *   rate_adjustments: []          // 利率调整（可选）
 * }
 */
export function calculateAcDebt(data) {
  return request({
    url: '/external/ac-calc',
    method: 'post',
    data
  })
}

/**
 * 批量债权精算
 */
export function batchCalculateAcDebt(data) {
  return request({
    url: '/external/ac-calc/batch',
    method: 'post',
    data
  })
}

/**
 * 获取外部服务健康状态
 */
export function getExternalHealth() {
  return request({
    url: '/external/health',
    method: 'get'
  })
}
