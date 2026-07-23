<template>
  <div class="archive-center">
    <PageHeader title="归档中心" subtitle="律师核对材料，行政复核后生成不可覆盖的电子卷宗" />

    <el-tabs v-model="activeTab" @tab-change="load">
      <el-tab-pane label="归档任务" name="jobs">
        <div class="filter-bar">
          <el-select v-model="status" placeholder="全部状态" clearable @change="loadJobs">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-button :loading="loading" @click="loadJobs"><el-icon><Refresh /></el-icon>刷新</el-button>
        </div>
        <el-table v-loading="loading" :data="jobs" border @row-click="openJob">
          <el-table-column prop="caseNumber" label="案件编号" min-width="180" />
          <el-table-column prop="caseName" label="案件名称" min-width="260" show-overflow-tooltip />
          <el-table-column prop="createdByName" label="发起律师" width="110" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }"><el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag></template>
          </el-table-column>
          <el-table-column prop="currentStage" label="当前环节" min-width="170" />
          <el-table-column label="关键缺件" min-width="220">
            <template #default="{ row }">{{ row.missingCritical?.join('、') || '无' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }"><el-button link type="primary" @click.stop="openJob(row)">{{ row.canReview ? '处理复核' : '查看任务' }}</el-button></template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && !jobs.length" description="暂无归档任务" />
      </el-tab-pane>

      <el-tab-pane label="历史归档案件" name="history">
        <div class="filter-bar">
          <el-input v-model="keyword" placeholder="搜索案件名称、编号" clearable @keyup.enter="loadHistory" />
          <el-button :loading="loading" @click="loadHistory"><el-icon><Search /></el-icon>查询</el-button>
        </div>
        <el-table v-loading="loading" :data="archivedCases" border>
          <el-table-column prop="caseNumber" label="案件编号" min-width="180" />
          <el-table-column prop="caseName" label="案件名称" min-width="260" />
          <el-table-column prop="caseTypeDesc" label="案件类型" width="120" />
          <el-table-column prop="archiveDate" label="归档日期" width="120" />
          <el-table-column prop="archiveLocation" label="保管位置" min-width="240" show-overflow-tooltip />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }"><el-button link type="primary" @click="$router.push(`/case/${row.id}/archive`)">查看</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { getArchiveJobs } from '@/api/archive'
import { getCaseList } from '@/api/case'

const router = useRouter()
const activeTab = ref('jobs')
const loading = ref(false)
const status = ref('')
const jobs = ref([])
const archivedCases = ref([])
const keyword = ref('')
const statusOptions = [
  ['LAWYER_REVIEW', '律师核对'], ['ADMIN_REVIEW', '行政复核'], ['ASSEMBLING', '生成卷宗'],
  ['COMPLETED', '已归档'], ['FAILED', '处理失败'], ['REJECTED', '已驳回']
].map(([value, label]) => ({ value, label }))

const loadJobs = async () => {
  loading.value = true
  try { jobs.value = (await getArchiveJobs({ status: status.value || undefined })).data || [] }
  finally { loading.value = false }
}

const loadHistory = async () => {
  loading.value = true
  try {
    const response = await getCaseList({ archived: true, keyword: keyword.value || undefined, page: 0, size: 100 })
    archivedCases.value = response.data?.records || []
  } finally { loading.value = false }
}

const load = () => activeTab.value === 'jobs' ? loadJobs() : loadHistory()
const openJob = row => router.push(`/case/${row.caseId}/archive`)
const statusLabel = value => ({ PRECHECK: '材料预检查', OCR: '本地OCR', CLASSIFYING: '材料分类', EXTRACTING: '字段提取', LAWYER_REVIEW: '律师核对', ADMIN_REVIEW: '行政复核', ASSEMBLING: '生成卷宗', COMPLETED: '已归档', FAILED: '处理失败', REJECTED: '已驳回' }[value] || value)
const statusType = value => ({ COMPLETED: 'success', FAILED: 'danger', REJECTED: 'warning', ADMIN_REVIEW: 'warning', LAWYER_REVIEW: 'primary' }[value] || 'info')

onMounted(loadJobs)
</script>

<style scoped>
.archive-center { padding: 0; }
.filter-bar { display: flex; gap: 10px; margin: 16px 0; }
.filter-bar .el-select { width: 180px; }
.filter-bar .el-input { width: min(360px, 100%); }
:deep(.el-table__row) { cursor: pointer; }
@media (max-width: 760px) { .filter-bar { flex-wrap: wrap; } }
</style>
