<template>
  <div class="npa-performance">
    <PageHeader title="不良资产绩效看板" />

    <div v-loading="loading">
      <!-- 总体概览卡片 -->
      <el-row :gutter="16" class="overview-row">
        <el-col :span="6" v-for="item in overviewCards" :key="item.label">
          <el-card shadow="hover" class="overview-card">
            <div class="card-value" :style="{ color: item.color }">{{ item.value }}</div>
            <div class="card-label">{{ item.label }}</div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" style="margin-top: 20px">
        <!-- 按银行统计 -->
        <el-col :span="12">
          <el-card>
            <template #header><span>按委托银行统计</span></template>
            <el-table :data="byBank" border stripe size="small" v-if="byBank.length > 0">
              <el-table-column prop="bankName" label="银行" min-width="140" />
              <el-table-column prop="packageCount" label="资产包数" width="80" align="center" />
              <el-table-column prop="totalAmount" label="总额" width="140" align="right">
                <template #default="{ row }">{{ formatMoney(row.totalAmount) }}</template>
              </el-table-column>
              <el-table-column prop="recoveredAmount" label="已回收" width="140" align="right">
                <template #default="{ row }">{{ formatMoney(row.recoveredAmount) }}</template>
              </el-table-column>
              <el-table-column prop="recoveryRate" label="回收率" width="90" align="center">
                <template #default="{ row }">{{ row.recoveryRate }}%</template>
              </el-table-column>
            </el-table>
            <div v-else class="empty-hint">暂无数据</div>
          </el-card>
        </el-col>

        <!-- 按风险分布 -->
        <el-col :span="6">
          <el-card>
            <template #header><span>风险分布</span></template>
            <div class="risk-chart">
              <div class="risk-item">
                <el-tag type="danger" size="small">高</el-tag>
                <span class="risk-count">{{ riskDist.high || 0 }} 笔</span>
              </div>
              <div class="risk-item">
                <el-tag type="warning" size="small">中</el-tag>
                <span class="risk-count">{{ riskDist.medium || 0 }} 笔</span>
              </div>
              <div class="risk-item">
                <el-tag type="success" size="small">低</el-tag>
                <span class="risk-count">{{ riskDist.low || 0 }} 笔</span>
              </div>
            </div>
          </el-card>
        </el-col>

        <!-- 处置方式统计 -->
        <el-col :span="6">
          <el-card>
            <template #header><span>处置方式分布</span></template>
            <div class="disposal-chart">
              <div v-for="item in byDisposalMethod" :key="item.method" class="disposal-item">
                <span>{{ item.method }}</span>
                <el-tag size="small">{{ item.count }} 件</el-tag>
              </div>
              <div v-if="byDisposalMethod.length === 0" class="empty-hint">暂无数据</div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 月度回收趋势 -->
      <el-row style="margin-top: 16px">
        <el-col :span="24">
          <el-card>
            <template #header><span>月度回收趋势（近12个月）</span></template>
            <div class="trend-chart">
              <div class="bar-chart">
                <div v-for="(item, i) in monthlyTrend" :key="i" class="bar-wrapper" :title="item.month + ': ¥' + (item.amount || 0)">
                  <div class="bar" :style="{ height: calcBarHeight(item.amount) + 'px' }"></div>
                  <div class="bar-label">{{ item.month?.split('-')[1] }}月</div>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { getPerformanceDashboard } from '@/api/npa'

const loading = ref(true)
const overviewCards = ref([])
const byBank = ref([])
const byDisposalMethod = ref([])
const monthlyTrend = ref([])
const riskDist = ref({})
let maxTrendAmount = ref(0)

function formatMoney(v) { return v || v === 0 ? '¥' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : '-' }

function calcBarHeight(amount) {
  if (!maxTrendAmount.value || maxTrendAmount.value <= 0) return 2
  return Math.max(2, (amount / maxTrendAmount.value) * 180)
}

async function loadData() {
  loading.value = true
  try {
    const res = await getPerformanceDashboard()
    const data = res.data || {}

    // 概览
    const overview = data.overview || {}
    overviewCards.value = [
      { label: '债权总数', value: overview.totalAssets ?? '-', color: '#409eff' },
      { label: '已回收', value: overview.recoveredCount ?? '-', color: '#67c23a' },
      { label: '处置中', value: overview.inProgressCount ?? '-', color: '#e6a23c' },
      { label: '综合回收率', value: overview.recoveryRate != null ? overview.recoveryRate + '%' : '-', color: '#f56c6c' }
    ]

    byBank.value = data.byBank || []
    byDisposalMethod.value = data.byDisposalMethod || []
    monthlyTrend.value = data.monthlyTrend || []
    riskDist.value = data.riskDistribution || {}

    // 找最大值
    maxTrendAmount.value = monthlyTrend.value.reduce((max, item) => {
      const amt = Number(item.amount || 0)
      return amt > max ? amt : max
    }, 0)

  } catch (e) {
    console.error('加载绩效数据失败', e)
  } finally {
    loading.value = false
  }
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.npa-performance { padding: 20px; }
.overview-card { text-align: center; padding: 12px 0; .card-value { font-size: 28px; font-weight: bold; } .card-label { font-size: 13px; color: #666; margin-top: 4px; } }
.risk-chart { .risk-item { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid #f0f0f0; &:last-child { border-bottom: none; } } }
.disposal-chart { .disposal-item { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid #f0f0f0; } }
.trend-chart { padding: 10px 0; }
.bar-chart { display: flex; align-items: flex-end; justify-content: space-around; height: 220px; border-bottom: 1px solid #e0e0e0; padding-bottom: 0; }
.bar-wrapper { flex: 1; display: flex; flex-direction: column; align-items: center; height: 100%; justify-content: flex-end; }
.bar { width: 60%; background: linear-gradient(to top, #409eff, #79bbff); border-radius: 4px 4px 0 0; min-height: 2px; transition: height 0.3s; }
.bar-label { font-size: 11px; color: #999; margin-top: 4px; text-align: center; width: 100%; }
.empty-hint { color: #999; text-align: center; padding: 20px; }
</style>
