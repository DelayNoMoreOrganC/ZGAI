import { defineStore } from 'pinia'
import { getCaseDetail, getCaseList } from '@/api/case'

export const useCaseStore = defineStore('case', {
  state: () => ({
    currentCase: null,
    caseList: [],
    caseFilters: {
      caseType: '',
      status: '',
      level: '',
      ownerId: '',
      court: '',
      dateRange: []
    },
    loading: false
  }),

  getters: {
    hasActiveCase: (state) => !!state.currentCase,
    activeCaseId: (state) => state.currentCase?.id || ''
  },

  actions: {
    // 获取案件列表
    async fetchCaseList(params = {}) {
      this.loading = true
      try {
        const res = await getCaseList({ ...this.caseFilters, ...params })
        this.caseList = res.data.records || []
        return res
      } catch (error) {
        throw error
      } finally {
        this.loading = false
      }
    },

    // 获取案件详情
    async fetchCaseDetail(id) {
      this.loading = true
      try {
        const res = await getCaseDetail(id)
        this.currentCase = res.data
        return res
      } catch (error) {
        throw error
      } finally {
        this.loading = false
      }
    },

    // 更新筛选条件
    updateFilters(filters) {
      this.caseFilters = { ...this.caseFilters, ...filters }
    },

    // 重置筛选条件
    resetFilters() {
      this.caseFilters = {
        caseType: '',
        status: '',
        level: '',
        ownerId: '',
        court: '',
        dateRange: []
      }
    },

    // 清空当前案件
    clearCurrentCase() {
      this.currentCase = null
    }
  }
})
