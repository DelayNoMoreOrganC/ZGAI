<template>
  <div class="npa-disposal">
    <PageHeader title="处置跟踪" />

    <!-- 统计小卡片 -->
    <el-row :gutter="12" class="stats-row">
      <el-col :span="6" v-for="s in stats" :key="s.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ s.count }}</div>
          <div class="stat-label">{{ s.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Tab 切换 -->
    <el-tabs v-model="activeTab" class="tabs">
      <el-tab-pane label="待审批方案" name="pending">
        <el-table :data="pendingPlans" border v-loading="loading" stripe>
          <el-table-column prop="debtorName" label="债务人" min-width="140">
            <template #default="{ row }">
              <el-link type="primary" @click="$router.push(`/npa/assets/${row.assetId}`)">{{ row.debtorName }}</el-link>
            </template>
          </el-table-column>
          <el-table-column prop="planName" label="方案名称" min-width="160" />
          <el-table-column prop="disposalMethod" label="处置方式" width="100" align="center">
            <template #default="{ row }">{{ methodLabel(row.disposalMethod) }}</template>
          </el-table-column>
          <el-table-column prop="targetAmount" label="目标金额" width="130" align="right">
            <template #default="{ row }">{{ formatMoney(row.targetAmount) }}</template>
          </el-table-column>
          <el-table-column prop="deadline" label="计划完成" width="110" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button size="small" type="success" @click="handleApprove(row, true)">批准</el-button>
              <el-button size="small" type="danger" @click="handleApprove(row, false)">驳回</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="执行中" name="active">
        <el-table :data="activePlans" border v-loading="loading" stripe>
          <el-table-column prop="debtorName" label="债务人" min-width="140">
            <template #default="{ row }">
              <el-link type="primary" @click="$router.push(`/npa/assets/${row.assetId}`)">{{ row.debtorName }}</el-link>
            </template>
          </el-table-column>
          <el-table-column prop="planName" label="方案名称" min-width="160" />
          <el-table-column prop="disposalMethod" label="方式" width="80" align="center">
            <template #default="{ row }">{{ methodLabel(row.disposalMethod) }}</template>
          </el-table-column>
          <el-table-column prop="targetAmount" label="目标" width="120" align="right">
            <template #default="{ row }">{{ formatMoney(row.targetAmount) }}</template>
          </el-table-column>
          <el-table-column prop="responsiblePerson" label="负责人" width="90" />
          <el-table-column prop="deadline" label="截止日" width="100" />
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="$router.push(`/npa/assets/${row.assetId}`)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="已完成" name="completed">
        <el-table :data="completedPlans" border v-loading="loading" stripe>
          <el-table-column prop="debtorName" label="债务人" min-width="140" />
          <el-table-column prop="planName" label="方案名称" min-width="160" />
          <el-table-column prop="disposalMethod" label="方式" width="80" align="center">
            <template #default="{ row }">{{ methodLabel(row.disposalMethod) }}</template>
          </el-table-column>
          <el-table-column prop="targetAmount" label="目标金额" width="120" align="right">
            <template #default="{ row }">{{ formatMoney(row.targetAmount) }}</template>
          </el-table-column>
          <el-table-column prop="completionDate" label="完成日期" width="110" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { getDisposalStats } from '@/api/npa'
import { getAssetList } from '@/api/npa'
import { getPlanList, approvePlan } from '@/api/npa'

const loading = ref(false)
const activeTab = ref('pending')
const allPlans = ref([])

const stats = ref([
  { label: '待审批', count: 0, key: 'pendingReview' },
  { label: '已批准', count: 0, key: 'approved' },
  { label: '执行中', count: 0, key: 'executing' },
  { label: '已完成', count: 0, key: 'completed' }
])

const pendingPlans = computed(() => allPlans.value.filter(p => p.status === 'PENDING_REVIEW'))
const activePlans = computed(() => allPlans.value.filter(p => p.status === 'IN_PROGRESS' || p.status === 'APPROVED'))
const completedPlans = computed(() => allPlans.value.filter(p => p.status === 'COMPLETED'))

function formatMoney(v) { return v || v === 0 ? '¥' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : '-' }
function methodLabel(s) {
  return { LITIGATION: '诉讼', ENFORCEMENT: '执行', SETTLEMENT: '和解', ASSIGNMENT: '债权转让', WRITE_OFF: '核销', BANKRUPTCY: '破产', OTHER: '其他' }[s] || s
}

async function fetchAll() {
  loading.value = true
  try {
    // 加载所有资产获取方案
    const res = await getAssetList({ page: 0, size: 999 })
    const assets = res.data.records || res.data.content || []
    const plans = []
    for (const asset of assets.slice(0, 50)) {
      try {
        const planRes = await getPlanList(asset.id)
        const list = planRes.data || []
        list.forEach(p => { p.debtorName = asset.debtorName })
        plans.push(...list)
      } catch {}
    }
    plans.sort((a, b) => new Date(b.deadline || 0) - new Date(a.deadline || 0))
    allPlans.value = plans

    // 加载统计
    try {
      const statsRes = await getDisposalStats()
      const data = statsRes.data || {}
      stats.value.forEach(s => { s.count = data[s.key] || 0 })
    } catch {}
  } catch {}
  finally { loading.value = false }
}

async function handleApprove(plan, approved) {
  const action = approved ? '批准' : '驳回'
  try {
    await ElMessageBox.prompt(`请输入${action}意见`, action)
    const { value } = await ElMessageBox.prompt(`请输入${action}意见`, action)
    await approvePlan(plan.id, { approved, comment: value || '' })
    ElMessage.success(`${action}成功`)
    fetchAll()
  } catch {}
}

onMounted(() => fetchAll())
</script>

<style scoped lang="scss">
.npa-disposal { padding: 20px; }
.stats-row { margin-bottom: 20px; }
.stat-card { text-align: center; .stat-value { font-size: 28px; font-weight: bold; color: #409eff; } .stat-label { font-size: 13px; color: #666; margin-top: 4px; } }
.tabs { margin-top: 0; }
</style>
