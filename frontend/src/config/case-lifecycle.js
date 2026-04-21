/**
 * 案件生命周期流程配置
 * 根据PRD 3.1节定义不同案件类型的标准流程节点
 */

// 案件类型流程模板
export const CASE_TYPE_WORKFLOWS = {
  // 民事案件流程
  CIVIL: {
    name: '民事案件',
    stages: [
      { key: 'consult', label: '咨询', order: 1, autoTodos: ['联系客户了解案情', '评估案件可行性'] },
      { key: 'contract', label: '签约', order: 2, autoTodos: ['起草委托代理合同', '办理委托手续', '收取律师费'] },
      { key: 'drafting', label: '起草文书', order: 3, autoTodos: ['起草起诉状', '整理证据清单', '准备立案材料'] },
      { key: 'filing', label: '立案', order: 4, autoTodos: ['提交立案材料', '等待法院受理', '缴纳诉讼费'] },
      { key: 'trial1', label: '一审审理中', order: 5, autoTodos: ['准备庭审提纲', '参加庭审', '提交代理词'] },
      { key: 'judgment1', label: '一审判决', order: 6, autoTodos: ['领取判决书', '分析判决结果', '与当事人沟通'] },
      { key: 'trial2', label: '二审', order: 7, autoTodos: ['起草上诉状', '提交上诉材料', '参加二审庭审'] },
      { key: 'execution', label: '执行', order: 8, autoTodos: ['申请执行', '提供执行线索', '跟进执行进度'] },
      { key: 'closed', label: '结案归档', order: 9, autoTodos: ['整理卷宗材料', '撰写结案报告', '归档案件'] }
    ]
  },

  // 刑事案件流程
  CRIMINAL: {
    name: '刑事案件',
    stages: [
      { key: 'consult', label: '咨询', order: 1, autoTodos: ['了解案件基本情况', '评估辩护策略'] },
      { key: 'contract', label: '签约', order: 2, autoTodos: ['签订委托协议', '办理委托手续', '会见犯罪嫌疑人'] },
      { key: 'meeting', label: '会见', order: 3, autoTodos: ['首次会见', '了解案情细节', '提供法律咨询'] },
      { key: 'investigation', label: '审查起诉', order: 4, autoTodos: ['查阅案卷', '提出辩护意见', '申请取保候审'] },
      { key: 'trial1', label: '一审', order: 5, autoTodos: ['准备辩护材料', '参加庭审', '发表辩护意见'] },
      { key: 'judgment1', label: '一审判决', order: 6, autoTodos: ['领取判决书', '分析判决结果', '建议当事人是否上诉'] },
      { key: 'trial2', label: '二审', order: 7, autoTodos: ['办理上诉手续', '准备二审辩护', '参加二审庭审'] },
      { key: 'closed', label: '结案归档', order: 8, autoTodos: ['整理辩护卷宗', '撰写结案报告', '归档案件'] }
    ]
  },

  // 行政案件流程
  ADMINISTRATIVE: {
    name: '行政案件',
    stages: [
      { key: 'consult', label: '咨询', order: 1, autoTodos: ['了解行政行为情况', '评估起诉条件'] },
      { key: 'contract', label: '签约', order: 2, autoTodos: ['签订委托合同', '办理委托手续', '收集相关证据'] },
      { key: 'drafting', label: '起草文书', order: 3, autoTodos: ['起草起诉状', '整理证据材料', '准备立案材料'] },
      { key: 'filing', label: '立案', order: 4, autoTodos: ['提交立案材料', '等待法院受理', '缴纳诉讼费'] },
      { key: 'trial1', label: '一审', order: 5, autoTodos: ['准备庭审材料', '参加庭审', '发表代理意见'] },
      { key: 'judgment1', label: '一审判决', order: 6, autoTodos: ['领取判决书', '分析判决结果'] },
      { key: 'trial2', label: '二审', order: 7, autoTodos: ['提起上诉', '参加二审庭审'] },
      { key: 'closed', label: '结案归档', order: 8, autoTodos: ['整理案件材料', '撰写结案报告', '归档案件'] }
    ]
  },

  // 商事仲裁流程
  ARBITRATION: {
    name: '商事仲裁',
    stages: [
      { key: 'consult', label: '咨询', order: 1, autoTodos: ['了解仲裁请求', '审查仲裁条款'] },
      { key: 'contract', label: '签约', order: 2, autoTodos: ['签订委托合同', '办理委托手续'] },
      { key: 'application', label: '申请仲裁', order: 3, autoTodos: ['起草仲裁申请书', '提交仲裁材料', '缴纳仲裁费'] },
      { key: 'tribunal', label: '组庭', order: 4, autoTodos: ['选择仲裁员', '参加组庭程序'] },
      { key: 'hearing', label: '开庭', order: 5, autoTodos: ['准备仲裁庭审', '参加开庭', '提交代理意见'] },
      { key: 'award', label: '裁决', order: 6, autoTodos: ['领取裁决书', '分析裁决结果'] },
      { key: 'execution', label: '执行', order: 7, autoTodos: ['申请执行', '跟进执行进度'] },
      { key: 'closed', label: '结案归档', order: 8, autoTodos: ['整理仲裁卷宗', '撰写结案报告', '归档案件'] }
    ]
  },

  // 非诉案件流程
  NON_LITIGATION: {
    name: '非诉案件',
    stages: [
      { key: 'consult', label: '咨询', order: 1, autoTodos: ['了解服务需求', '评估服务方案'] },
      { key: 'contract', label: '签约', order: 2, autoTodos: ['签订法律服务合同', '办理委托手续'] },
      { key: 'investigation', label: '尽职调查', order: 3, autoTodos: ['开展尽职调查', '收集相关资料', '出具调查报告'] },
      { key: 'drafting', label: '出具文书', order: 4, autoTodos: ['起草法律文书', '内部审核', '交付文书'] },
      { key: 'delivery', label: '交付', order: 5, autoTodos: ['向客户交付成果', '解释文书内容', '客户确认'] },
      { key: 'followup', label: '跟进', order: 6, autoTodos: ['跟进实施情况', '提供后续咨询'] },
      { key: 'closed', label: '结案归档', order: 7, autoTodos: ['整理案件材料', '撰写结案报告', '归档案件'] }
    ]
  },

  // 默认流程（商事等）
  DEFAULT: {
    name: '默认流程',
    stages: [
      { key: 'consult', label: '咨询', order: 1, autoTodos: ['了解需求', '评估可行性'] },
      { key: 'contract', label: '签约', order: 2, autoTodos: ['签订合同', '办理手续'] },
      { key: 'drafting', label: '起草', order: 3, autoTodos: ['起草相关文书', '准备材料'] },
      { key: 'filing', label: '立案/申请', order: 4, autoTodos: ['提交申请', '等待受理'] },
      { key: 'processing', label: '处理中', order: 5, autoTodos: ['跟进进度', '应对反馈'] },
      { key: 'closed', label: '结案归档', order: 6, autoTodos: ['整理材料', '归档案件'] }
    ]
  }
}

/**
 * 根据案件类型获取流程配置
 * @param {string} caseType - 案件类型
 * @returns {object} 流程配置
 */
export function getWorkflowByCaseType(caseType) {
  return CASE_TYPE_WORKFLOWS[caseType] || CASE_TYPE_WORKFLOWS.DEFAULT
}

/**
 * 获取案件的所有阶段
 * @param {string} caseType - 案件类型
 * @returns {array} 阶段数组
 */
export function getStagesByCaseType(caseType) {
  const workflow = getWorkflowByCaseType(caseType)
  return workflow.stages || []
}

/**
 * 根据阶段key获取阶段信息
 * @param {string} caseType - 案件类型
 * @param {string} stageKey - 阶段key
 * @returns {object|null} 阶段信息
 */
export function getStageInfo(caseType, stageKey) {
  const stages = getStagesByCaseType(caseType)
  return stages.find(s => s.key === stageKey) || null
}

/**
 * 获取阶段的自动待办列表
 * @param {string} caseType - 案件类型
 * @param {string} stageKey - 阶段key
 * @returns {array} 待办事项数组
 */
export function getStageAutoTodos(caseType, stageKey) {
  const stageInfo = getStageInfo(caseType, stageKey)
  return stageInfo?.autoTodos || []
}

/**
 * 计算阶段进度百分比
 * @param {string} caseType - 案件类型
 * @param {string} currentStage - 当前阶段
 * @returns {number} 进度百分比 (0-100)
 */
export function calculateStageProgress(caseType, currentStage) {
  const stages = getStagesByCaseType(caseType)
  const currentIndex = stages.findIndex(s => s.key === currentStage)

  if (currentIndex === -1) return 0
  return Math.round(((currentIndex + 1) / stages.length) * 100)
}

/**
 * 获取下一个阶段
 * @param {string} caseType - 案件类型
 * @param {string} currentStage - 当前阶段
 * @returns {object|null} 下一阶段信息
 */
export function getNextStage(caseType, currentStage) {
  const stages = getStagesByCaseType(caseType)
  const currentIndex = stages.findIndex(s => s.key === currentStage)

  if (currentIndex === -1 || currentIndex === stages.length - 1) {
    return null
  }

  return stages[currentIndex + 1]
}

/**
 * 检查是否可以回退到指定阶段
 * @param {string} caseType - 案件类型
 * @param {string} currentStage - 当前阶段
 * @param {string} targetStage - 目标阶段
 * @returns {boolean} 是否可以回退
 */
export function canRollbackTo(caseType, currentStage, targetStage) {
  const stages = getStagesByCaseType(caseType)
  const currentIndex = stages.findIndex(s => s.key === currentStage)
  const targetIndex = stages.findIndex(s => s.key === targetStage)

  // 目标阶段必须在当前阶段之前，且不是第一个阶段（咨询阶段一般不能回退）
  return targetIndex >= 0 && targetIndex < currentIndex && targetIndex > 0
}

/**
 * 生成阶段待办事项数据
 * @param {string} caseType - 案件类型
 * @param {string} stageKey - 阶段key
 * @param {string} caseId - 案件ID
 * @param {string} caseName - 案件名称
 * @param {string} assigneeId - 负责人ID
 * @returns {array} 待办事项数组
 */
export function generateStageTodos(caseType, stageKey, caseId, caseName, assigneeId) {
  const autoTodos = getStageAutoTodos(caseType, stageKey)

  return autoTodos.map((title, index) => ({
    title: `[${caseName}] ${title}`,
    caseId,
    caseName,
    assigneeId,
    priority: index === 0 ? 'high' : 'medium', // 第一个待办高优先级
    status: 'PENDING',
    dueDate: calculateDueDate(index), // 根据优先级计算截止日期
    description: `案件"${caseName}"进入"${stageKey}"阶段，需要完成：${title}`
  }))
}

/**
 * 计算待办截止日期（简单实现）
 * @param {number} daysFromNow - 距今天数
 * @returns {string} YYYY-MM-DD格式的日期
 */
function calculateDueDate(daysFromNow) {
  const date = new Date()
  date.setDate(date.getDate() + daysFromNow + 1) // 默认+1天
  return date.toISOString().split('T')[0]
}

export default {
  CASE_TYPE_WORKFLOWS,
  getWorkflowByCaseType,
  getStagesByCaseType,
  getStageInfo,
  getStageAutoTodos,
  calculateStageProgress,
  getNextStage,
  canRollbackTo,
  generateStageTodos
}
