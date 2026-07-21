<template>
  <div class="legacy-materials-page">
    <PageHeader title="旧系统资料检索" />

    <el-alert
      class="notice"
      title="当前阶段不做旧系统全量导入。请基于案件名称、案号、客户、案由、承办人或部门检索旧资料线索；旧资料目录配置后可同步检索文件路径。"
      type="info"
      show-icon
      :closable="false"
    />

    <section class="search-panel">
      <el-form :model="searchForm" label-width="84px" @submit.prevent="handleSearch">
        <div class="form-grid">
          <el-form-item label="关键词">
            <el-input
              v-model="searchForm.keyword"
              placeholder="可输入客户、案件名称、旧案号、承办人等"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="案件名称">
            <el-input v-model="searchForm.caseName" placeholder="例如：某某诉某某合同纠纷" clearable />
          </el-form-item>
          <el-form-item label="案号">
            <el-input v-model="searchForm.caseNumber" placeholder="可输入 ZGAI 或旧系统案号" clearable />
          </el-form-item>
          <el-form-item label="客户名称">
            <el-input v-model="searchForm.clientName" placeholder="拟查找的客户或主体名称" clearable />
          </el-form-item>
          <el-form-item label="案由">
            <el-input v-model="searchForm.caseReason" placeholder="例如：买卖合同纠纷" clearable />
          </el-form-item>
          <el-form-item label="承办人">
            <el-input v-model="searchForm.ownerName" placeholder="律师或承办人姓名" clearable />
          </el-form-item>
          <el-form-item label="部门">
            <el-input v-model="searchForm.departmentName" placeholder="例如：民商法务部" clearable />
          </el-form-item>
          <el-form-item label="结果上限">
            <el-input-number v-model="searchForm.limit" :min="10" :max="100" :step="10" style="width: 100%" />
          </el-form-item>
        </div>

        <div class="actions">
          <el-button type="primary" :loading="loading" @click="handleSearch">
            检索资料
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </el-form>
    </section>

    <section class="status-panel" v-if="searched">
      <el-tag :type="response.archivePathConfigured ? 'success' : 'warning'">
        {{ response.archivePathConfigured ? '旧资料目录已配置' : '旧资料目录未配置' }}
      </el-tag>
      <span class="status-text">{{ response.message }}</span>
      <span v-if="response.recordId" class="record-text">查询记录 #{{ response.recordId }}</span>
    </section>

    <section class="result-panel" v-loading="loading">
      <div class="result-header">
        <h3>检索结果</h3>
        <span>共 {{ response.total || 0 }} 条</span>
      </div>

      <el-empty v-if="searched && results.length === 0 && !loading" description="暂无匹配资料" />
      <el-empty v-else-if="!searched" description="请输入条件后开始检索" />

      <el-table v-else :data="results" border>
        <el-table-column label="来源" width="130">
          <template #default="{ row }">
            <el-tag :type="getSourceType(row.sourceType)">
              {{ formatSource(row.sourceType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题/名称" min-width="220" show-overflow-tooltip />
        <el-table-column prop="caseNumber" label="案号" min-width="150" show-overflow-tooltip />
        <el-table-column prop="clientName" label="客户" min-width="150" show-overflow-tooltip />
        <el-table-column prop="caseReason" label="案由" min-width="150" show-overflow-tooltip />
        <el-table-column prop="ownerName" label="承办人" width="110" />
        <el-table-column prop="departmentName" label="部门" min-width="140" />
        <el-table-column prop="matchReason" label="命中原因" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button size="small" text type="primary" @click="handleOpen(row)">
              {{ getActionText(row) }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { searchLegacyMaterials } from '@/api/legacyMaterial'

const router = useRouter()
const loading = ref(false)
const searched = ref(false)
const results = ref([])
const response = ref({
  total: 0,
  message: '',
  archivePathConfigured: false,
  recordId: null
})

const searchForm = reactive({
  keyword: '',
  caseName: '',
  caseNumber: '',
  clientName: '',
  caseReason: '',
  ownerName: '',
  departmentName: '',
  limit: 30
})

const hasCondition = () => {
  return [
    searchForm.keyword,
    searchForm.caseName,
    searchForm.caseNumber,
    searchForm.clientName,
    searchForm.caseReason,
    searchForm.ownerName,
    searchForm.departmentName
  ].some(value => value && String(value).trim())
}

const handleSearch = async () => {
  if (!hasCondition()) {
    ElMessage.warning('请至少输入一个检索条件')
    return
  }

  loading.value = true
  searched.value = true
  try {
    const { data } = await searchLegacyMaterials({ ...searchForm })
    response.value = data || {}
    results.value = response.value.results || []
    if (results.value.length === 0) {
      ElMessage.info(response.value.message || '未找到匹配资料')
    }
  } catch (error) {
    console.error('旧资料检索失败:', error)
    ElMessage.error('旧资料检索失败')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  Object.assign(searchForm, {
    keyword: '',
    caseName: '',
    caseNumber: '',
    clientName: '',
    caseReason: '',
    ownerName: '',
    departmentName: '',
    limit: 30
  })
  results.value = []
  searched.value = false
  response.value = {
    total: 0,
    message: '',
    archivePathConfigured: false,
    recordId: null
  }
}

const formatSource = (sourceType) => {
  const map = {
    ZGAI_CASE: 'ZGAI案件',
    ZGAI_CLIENT: 'ZGAI客户',
    LEGACY_FILE: '旧资料文件'
  }
  return map[sourceType] || '资料线索'
}

const getSourceType = (sourceType) => {
  const map = {
    ZGAI_CASE: 'success',
    ZGAI_CLIENT: 'info',
    LEGACY_FILE: 'warning'
  }
  return map[sourceType] || ''
}

const getActionText = (row) => {
  if (row.sourceType === 'ZGAI_CASE') return '打开案件'
  if (row.sourceType === 'ZGAI_CLIENT') return '打开客户'
  return row.materialPath ? '复制路径' : '查看线索'
}

const handleOpen = async (row) => {
  if (row.sourceType === 'ZGAI_CASE' && row.relatedId) {
    router.push(`/case/${row.relatedId}/basic`)
    return
  }
  if (row.sourceType === 'ZGAI_CLIENT' && row.relatedId) {
    router.push(`/client/${row.relatedId}`)
    return
  }
  if (row.materialPath) {
    await copyText(row.materialPath)
    ElMessage.success('资料路径已复制')
    return
  }
  ElMessage.info('该线索暂无可打开资料')
}

const copyText = async (text) => {
  if (navigator.clipboard && window.isSecureContext) {
    await navigator.clipboard.writeText(text)
    return
  }
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', '')
  textarea.style.position = 'fixed'
  textarea.style.left = '-9999px'
  document.body.appendChild(textarea)
  textarea.select()
  document.execCommand('copy')
  document.body.removeChild(textarea)
}
</script>

<style scoped lang="scss">
.legacy-materials-page {
  .notice {
    margin-bottom: 16px;
  }

  .search-panel,
  .result-panel,
  .status-panel {
    border: 1px solid #e8edf3;
    border-radius: 8px;
    background: #fff;
  }

  .search-panel {
    padding: 18px 18px 4px;
  }

  .form-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    column-gap: 18px;
  }

  .actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    margin: 4px 0 14px;
  }

  .status-panel {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-top: 14px;
    padding: 12px 16px;
  }

  .status-text {
    color: #374151;
  }

  .record-text {
    margin-left: auto;
    color: #909399;
    font-size: 13px;
  }

  .result-panel {
    margin-top: 14px;
    padding: 16px;
  }

  .result-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 14px;

    h3 {
      margin: 0;
      font-size: 16px;
      color: #1f2937;
    }

    span {
      color: #6b7280;
      font-size: 13px;
    }
  }

  @media (max-width: 900px) {
    .form-grid {
      grid-template-columns: 1fr;
    }

    .status-panel {
      align-items: flex-start;
      flex-direction: column;
    }

    .record-text {
      margin-left: 0;
    }
  }
}
</style>
