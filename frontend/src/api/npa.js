import request from '@/utils/request'

// ========== 资产包 ==========
export function getPackageList(params) {
  return request({
    url: '/npa/packages',
    method: 'get',
    params
  })
}

export function getPackageDetail(id) {
  return request({
    url: `/npa/packages/${id}`,
    method: 'get'
  })
}

export function createPackage(data) {
  return request({
    url: '/npa/packages',
    method: 'post',
    data
  })
}

export function updatePackage(id, data) {
  return request({
    url: `/npa/packages/${id}`,
    method: 'put',
    data
  })
}

export function deletePackage(id) {
  return request({
    url: `/npa/packages/${id}`,
    method: 'delete'
  })
}

export function getPackageStats() {
  return request({
    url: '/npa/packages/stats/overview',
    method: 'get'
  })
}

// ========== 债权 ==========
export function getAssetList(params) {
  return request({
    url: '/npa/assets',
    method: 'get',
    params
  })
}

export function getAssetDetail(id) {
  return request({
    url: `/npa/assets/${id}`,
    method: 'get'
  })
}

export function createAsset(data) {
  return request({
    url: '/npa/assets',
    method: 'post',
    data
  })
}

export function updateAsset(id, data) {
  return request({
    url: `/npa/assets/${id}`,
    method: 'put',
    data
  })
}

export function deleteAsset(id) {
  return request({
    url: `/npa/assets/${id}`,
    method: 'delete'
  })
}

export function recordRecovery(id, recoveredAmount) {
  return request({
    url: `/npa/assets/${id}/recovery`,
    method: 'put',
    data: { recoveredAmount }
  })
}

export function searchDebtor(keyword) {
  return request({
    url: '/npa/assets/search',
    method: 'get',
    params: { keyword }
  })
}

// ========== 尽职调查 ==========
export function getDiligenceList(assetId) {
  return request({
    url: `/npa/assets/${assetId}/diligence`,
    method: 'get'
  })
}

export function getDiligenceDetail(id) {
  return request({
    url: `/npa/diligence/${id}`,
    method: 'get'
  })
}

export function createDiligence(data) {
  return request({
    url: '/npa/diligence',
    method: 'post',
    data
  })
}

export function updateDiligence(id, data) {
  return request({
    url: `/npa/diligence/${id}`,
    method: 'put',
    data
  })
}

export function deleteDiligence(id) {
  return request({
    url: `/npa/diligence/${id}`,
    method: 'delete'
  })
}

// ========== 处置方案 ==========
export function getPlanList(assetId) {
  return request({
    url: `/npa/assets/${assetId}/plans`,
    method: 'get'
  })
}

export function getPlanDetail(id) {
  return request({
    url: `/npa/plans/${id}`,
    method: 'get'
  })
}

export function createPlan(data) {
  return request({
    url: '/npa/plans',
    method: 'post',
    data
  })
}

export function updatePlan(id, data) {
  return request({
    url: `/npa/plans/${id}`,
    method: 'put',
    data
  })
}

export function approvePlan(id, data) {
  return request({
    url: `/npa/plans/${id}/approve`,
    method: 'put',
    data
  })
}

export function deletePlan(id) {
  return request({
    url: `/npa/plans/${id}`,
    method: 'delete'
  })
}

// ========== 处置结果 ==========
export function getResultList(assetId) {
  return request({
    url: `/npa/assets/${assetId}/results`,
    method: 'get'
  })
}

export function getResultDetail(id) {
  return request({
    url: `/npa/results/${id}`,
    method: 'get'
  })
}

export function createResult(data) {
  return request({
    url: '/npa/results',
    method: 'post',
    data
  })
}

// ========== 绩效统计 ==========
export function getPerformanceDashboard() {
  return request({
    url: '/npa/performance/dashboard',
    method: 'get'
  })
}

export function getDisposalStats() {
  return request({
    url: '/npa/performance/disposal-stats',
    method: 'get'
  })
}
