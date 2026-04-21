/**
 * 移动端表格转卡片工具函数
 */

import { h } from 'vue'

/**
 * 将表格数据转换为卡片组件
 * @param {Array} tableData - 表格数据
 * @param {Array} columns - 列配置
 * @param {Object} options - 配置选项
 */
export function transformTableToCards(tableData, columns, options = {}) {
  const {
    titleField = 'title',
    actionField = 'actions',
    emptyText = '暂无数据'
  } = options

  if (!tableData || tableData.length === 0) {
    return h('div', { class: 'empty-state' }, emptyText)
  }

  return h('div', { class: 'table-card-item' }, tableData.map((row, index) => {
    const cardRows = columns
      .filter(col => col.prop && !col.type) // 排除操作列
      .map(col => {
        const value = getNestedValue(row, col.prop)
        return h('div', { class: 'card-row' }, [
          h('span', { class: 'card-label' }, col.label || col.prop),
          h('span', { class: 'card-value' }, formatCellValue(value, col))
        ])
      })

    const header = h('div', { class: 'card-header' }, [
    h('span', { class: 'card-title' }, getNestedValue(row, titleField)),
    h('div', { class: 'card-actions' }, getNestedValue(row, actionField) || [])
  ])

    return h('div', { class: 'card-item' }, [
      header,
      h('div', { class: 'card-body' }, cardRows),
      h('div', { class: 'card-footer' }, [
        h('el-button', {
          size: 'small',
          type: 'primary',
          link: true,
          onClick: () => handleViewDetail(row, index)
        }, '详情')
      ])
    ])
  }))
}

/**
 * 获取嵌套属性值
 * @param {Object} obj - 数据对象
 * @param {String} path - 属性路径
 */
function getNestedValue(obj, path) {
  if (!path) return obj

  const keys = path.split('.')
  let result = obj

  for (const key of keys) {
    if (result && typeof result === 'object') {
      result = result[key]
    } else {
      return undefined
    }
  }

  return result
}

/**
 * 格式化单元格值
 * @param {*} value - 原始值
 * @param {Object} column - 列配置
 */
function formatCellValue(value, column) {
  if (value === null || value === undefined) {
    return '-'
  }

  // 处理标签
  if (column.tagType) {
    const tagMap = {
      primary: 'primary',
      success: 'success',
      warning: 'warning',
      danger: 'danger',
      info: 'info'
    }
    return h('el-tag', {
      type: tagMap[column.tagType] || 'info',
      size: 'small'
    }, String(value))
  }

  // 处理日期
  if (column.dataType === 'date' && value) {
    const date = new Date(value)
    return date.toLocaleDateString('zh-CN')
  }

  // 处理金额
  if (column.dataType === 'money' && value) {
    return `¥${Number(value).toLocaleString()}`
  }

  // 默认返回字符串
  return String(value)
}

/**
 * 查看详情处理
 * @param {Object} row - 行数据
 * @param {Number} index - 行索引
 */
function handleViewDetail(row, index) {
  // 触发查看详情事件，由父组件处理
  console.log('View detail:', row, index)
}

/**
 * 判断是否为移动端
 * @returns {Boolean}
 */
export function isMobile() {
  return window.innerWidth < 768
}

/**
 * 监听窗口大小变化
 * @param {Function} callback - 回调函数
 * @returns {Function} 清理函数
 */
export function watchResize(callback) {
  const handler = () => callback(window.innerWidth)

  window.addEventListener('resize', handler)

  return () => {
    window.removeEventListener('resize', handler)
  }
}

/**
 * 触摸反馈动画
 * @param {HTMLElement} element - 目标元素
 */
export function addTouchFeedback(element) {
  if (!element) return

  element.addEventListener('touchstart', () => {
    element.style.transform = 'scale(0.98)'
    element.style.transition = 'transform 0.1s'
  })

  element.addEventListener('touchend', () => {
    element.style.transform = 'scale(1)'
  })
}

/**
 * 防止双击缩放
 */
export function preventDoubleTapZoom() {
  if (!document.addEventListener) return

  let lastTouchEnd = 0

  document.addEventListener('touchend', (e) => {
    const now = Date.now()
    if (now - lastTouchEnd <= 300) {
      e.preventDefault()
    }
    lastTouchEnd = now
  }, false)
}

/**
 * 添加安全区域适配（iPhone刘海屏）
 */
export function addSafeAreaPadding() {
  const style = document.createElement('style')
  style.textContent = `
    @supports (padding: max(0px)) {
      body {
        padding-left: env(safe-area-inset-left);
        padding-right: env(safe-area-inset-right);
        padding-top: env(safe-area-inset-top);
        padding-bottom: env(safe-area-inset-bottom);
      }
    }
  `
  document.head.appendChild(style)
}
