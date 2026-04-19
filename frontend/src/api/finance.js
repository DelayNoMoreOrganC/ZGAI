import request from '@/utils/request'

// 费用记录
export function getExpenses(params) {
  return request({
    url: '/finance/expenses',
    method: 'get',
    params
  })
}

export function createExpense(data) {
  return request({
    url: '/finance/expenses',
    method: 'post',
    data
  })
}

export function updateExpense(id, data) {
  return request({
    url: `/finance/expenses/${id}`,
    method: 'put',
    data
  })
}

export function deleteExpense(id) {
  return request({
    url: `/finance/expenses/${id}`,
    method: 'delete'
  })
}

// 律师费管理
export function getFees(params) {
  return request({
    url: '/finance/fees',
    method: 'get',
    params
  })
}

// 收款记录
export function getPayments(params) {
  return request({
    url: '/finance/payments',
    method: 'get',
    params
  })
}

export function createPayment(data) {
  return request({
    url: '/finance/payments',
    method: 'post',
    data
  })
}

export function updatePayment(id, data) {
  return request({
    url: `/finance/payments/${id}`,
    method: 'put',
    data
  })
}

export function deletePayment(id) {
  return request({
    url: `/finance/payments/${id}`,
    method: 'delete'
  })
}

// 开票记录
export function getInvoices(params) {
  return request({
    url: '/finance/invoices',
    method: 'get',
    params
  })
}

export function createInvoice(data) {
  return request({
    url: '/finance/invoices',
    method: 'post',
    data
  })
}

export function updateInvoice(id, data) {
  return request({
    url: `/finance/invoices/${id}`,
    method: 'put',
    data
  })
}

export function deleteInvoice(id) {
  return request({
    url: `/finance/invoices/${id}`,
    method: 'delete'
  })
}

// 案件收支汇总
export function getFinanceSummary(caseId) {
  return request({
    url: `/finance/summary/${caseId}`,
    method: 'get'
  })
}

// 财务仪表盘
export function getFinanceDashboard() {
  return request({
    url: '/finance/dashboard',
    method: 'get'
  })
}
