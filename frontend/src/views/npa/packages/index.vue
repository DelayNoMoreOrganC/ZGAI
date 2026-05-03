<template>
  <div class="npa-packages">
    <PageHeader title="不良资产包管理" />

    <!-- 统计概览卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="4" v-for="stat in statsCards" :key="stat.label">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ stat.value }}</div>
          <div class="stat-label">{{ stat.label }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 工具栏 -->
    <div class="toolbar">
      <el-input v-model="searchQuery" placeholder="搜索资产包..." clearable style="width: 280px" @clear="fetchData" @keyup.enter="fetchData">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" @click="$router.push('/npa/packages/create')">
        <el-icon><Plus /></el-icon>新建资产包
      </el-button>
    </div>

    <!-- 表格 -->
    <el-table :data="packageList" border v-loading="loading" stripe>
      <el-table-column prop="packageCode" label="编号" width="140" />
      <el-table-column prop="packageName" label="资产包名称" min-width="180">
        <template #default="{ row }">
          <el-link type="primary" @click="$router.push(`/npa/packages/${row.id}`)">{{ row.packageName }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="bankName" label="委托银行" width="140" />
      <el-table-column prop="totalAmount" label="债权总额" width="140" align="right">
        <template #default="{ row }">{{ formatMoney(row.totalAmount) }}</template>
      </el-table-column>
      <el-table-column prop="recoveredAmount" label="已回收" width="130" align="right">
        <template #default="{ row }">{{ formatMoney(row.recoveredAmount) }}</template>
      </el-table-column>
      <el-table-column prop="recoveryRate" label="回收率" width="100" align="center">
        <template #default="{ row }">
          <el-progress :percentage="Number(row.recoveryRate || 0)" :status="row.recoveryRate >= 80 ? 'success' : row.recoveryRate >= 30 ? 'warning' : 'exception'" :stroke-width="16" />
        </template>
      </el-table-column>
      <el-table-column prop="assetCount" label="笔数" width="70" align="center" />
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="responsiblePerson" label="负责人" width="90" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="$router.push(`/npa/packages/${row.id}`)">详情</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="page"
        :page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="fetchData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { getPackageList, deletePackage, getPackageStats } from '@/api/npa'

const loading = ref(false)
const packageList = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchQuery = ref('')

const statsCards = ref([
  { label: '总资产包', value: '-', key: 'totalPackages' },
  { label: '债权总数', value: '-', key: 'totalAssets' },
  { label: '债权总额', value: '-', key: 'totalAmount', isMoney: true },
  { label: '已回收', value: '-', key: 'totalRecovered', isMoney: true },
  { label: '综合回收率', value: '-', key: 'recoveryRate', isPercent: true }
])

function formatMoney(val) {
  if (!val && val !== 0) return '-'
  return '¥' + Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function statusTagType(status) {
  const map = { PENDING: 'info', IN_PROGRESS: 'warning', SETTLED: 'success' }
  return map[status] || 'info'
}

function statusLabel(status) {
  const map = { PENDING: '待处置', IN_PROGRESS: '处置中', SETTLED: '已结清' }
  return map[status] || status
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getPackageList({ page: page.value - 1, size: size.value })
    packageList.value = res.data.records || res.data.content || []
    total.value = res.data.total || 0
  } catch (e) {
    ElMessage.error('加载失败: ' + e.message)
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  try {
    const res = await getPackageStats()
    const data = res.data
    statsCards.value[0].value = data.totalPackages ?? '-'
    statsCards.value[1].value = data.totalAssets ?? '-'
    statsCards.value[2].value = data.totalAmount ? formatMoney(data.totalAmount) : '-'
    statsCards.value[3].value = data.totalRecovered ? formatMoney(data.totalRecovered) : '-'
    statsCards.value[4].value = data.recoveryRate != null ? data.recoveryRate + '%' : '-'
  } catch (e) {
    console.warn('加载统计失败', e)
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除资产包"${row.packageName}"？`, '确认')
    await deletePackage(row.id)
    ElMessage.success('已删除')
    fetchData()
    loadStats()
  } catch {}
}

onMounted(() => {
  fetchData()
  loadStats()
})
</script>

<style scoped lang="scss">
.npa-packages {
  padding: 20px;
}
.stats-row {
  margin-bottom: 20px;
  .stat-card {
    text-align: center;
    .stat-value {
      font-size: 24px;
      font-weight: bold;
      color: #409eff;
    }
    .stat-label {
      font-size: 13px;
      color: #666;
      margin-top: 4px;
    }
  }
}
.toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}
.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
