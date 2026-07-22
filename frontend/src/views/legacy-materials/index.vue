<template>
  <div class="legacy-materials-page">
    <PageHeader title="旧系统资料检索" />

    <el-alert
      class="notice"
      title="选择当前账号有权查看的案件，系统将自动提取案号、案件名称、客户及当事人等要素，在旧案资料目录中查找对应材料。"
      type="info"
      show-icon
      :closable="false"
    />

    <section class="search-panel">
      <el-form :model="searchForm" label-width="88px" @submit.prevent="handleSearch">
        <div class="form-grid">
          <el-form-item label="来源案件" required>
            <el-select
              v-model="searchForm.caseId"
              filterable
              remote
              clearable
              reserve-keyword
              :remote-method="loadCaseOptions"
              :loading="caseLoading"
              placeholder="输入案件名称或案号检索本人可见案件"
              style="width: 100%"
            >
              <el-option
                v-for="item in caseOptions"
                :key="item.id"
                :label="formatCaseOption(item)"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="结果上限">
            <el-input-number
              v-model="searchForm.limit"
              :min="10"
              :max="100"
              :step="10"
              style="width: 100%"
            />
          </el-form-item>
        </div>

        <div class="actions">
          <el-button type="primary" :loading="loading" @click="handleSearch">
            检索旧案材料
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </el-form>
    </section>

    <section v-if="searched" class="status-panel">
      <div class="status-row">
        <el-tag :type="response.archivePathConfigured ? 'success' : 'warning'">
          {{ response.archivePathConfigured ? '旧资料目录已接入' : '旧资料目录未配置' }}
        </el-tag>
        <span class="status-text">{{ response.message }}</span>
        <span v-if="response.recordId" class="record-text">查询记录 #{{ response.recordId }}</span>
      </div>
      <div v-if="response.searchElements?.length" class="element-list">
        <span class="element-title">本次检索要素</span>
        <el-tag
          v-for="element in response.searchElements"
          :key="element"
          type="info"
          effect="plain"
        >
          {{ element }}
        </el-tag>
      </div>
    </section>

    <section class="result-panel" v-loading="loading">
      <div class="result-header">
        <div>
          <h3>旧案材料</h3>
          <p v-if="response.sourceCaseName">
            {{ response.sourceCaseNumber || '未编号' }} · {{ response.sourceCaseName }}
          </p>
        </div>
        <span>共 {{ response.total || 0 }} 条</span>
      </div>

      <el-empty v-if="searched && results.length === 0 && !loading" description="暂无匹配材料" />
      <el-empty v-else-if="!searched" description="请选择来源案件后开始检索" />

      <el-table v-else :data="results" border>
        <el-table-column prop="title" label="文件名称" min-width="280" show-overflow-tooltip />
        <el-table-column prop="matchReason" label="命中要素" min-width="240" show-overflow-tooltip />
        <el-table-column label="文件大小" width="110">
          <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="修改时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.lastModifiedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              text
              :disabled="!row.downloadable || !row.legacyFileId"
              :loading="downloadingId === row.legacyFileId"
              @click="handleDownload(row)"
            >
              下载
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { getCaseList } from '@/api/case'
import { downloadLegacyMaterial, searchLegacyMaterials } from '@/api/legacyMaterial'

const loading = ref(false)
const caseLoading = ref(false)
const downloadingId = ref(null)
const searched = ref(false)
const results = ref([])
const caseOptions = ref([])
const response = ref(createEmptyResponse())

const searchForm = reactive({
  caseId: null,
  limit: 30
})

function createEmptyResponse() {
  return {
    total: 0,
    message: '',
    archivePathConfigured: false,
    recordId: null,
    sourceCaseId: null,
    sourceCaseName: '',
    sourceCaseNumber: '',
    searchElements: []
  }
}

const loadCaseOptions = async (keyword = '') => {
  caseLoading.value = true
  try {
    const { data } = await getCaseList({
      page: 1,
      size: 50,
      keyword: keyword?.trim() || undefined
    })
    caseOptions.value = data?.records || []
  } catch (error) {
    console.error('加载案件选项失败:', error)
    caseOptions.value = []
  } finally {
    caseLoading.value = false
  }
}

const formatCaseOption = (item) => {
  return `${item.caseNumber || '未编号'} · ${item.caseName || '未命名案件'}`
}

const handleSearch = async () => {
  if (!searchForm.caseId) {
    ElMessage.warning('请先选择来源案件')
    return
  }

  loading.value = true
  searched.value = true
  try {
    const { data } = await searchLegacyMaterials({ ...searchForm })
    response.value = data || createEmptyResponse()
    results.value = response.value.results || []
    if (results.value.length === 0) {
      ElMessage.info(response.value.message || '未找到匹配材料')
    }
  } catch (error) {
    console.error('旧案材料检索失败:', error)
    results.value = []
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  searchForm.caseId = null
  searchForm.limit = 30
  results.value = []
  searched.value = false
  response.value = createEmptyResponse()
  loadCaseOptions()
}

const handleDownload = async (row) => {
  if (!row.legacyFileId) return
  downloadingId.value = row.legacyFileId
  try {
    const download = await downloadLegacyMaterial(row.legacyFileId)
    const blob = download.data instanceof Blob ? download.data : new Blob([download.data])
    const url = URL.createObjectURL(blob)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = row.title || '旧案材料'
    document.body.appendChild(anchor)
    anchor.click()
    document.body.removeChild(anchor)
    URL.revokeObjectURL(url)
  } catch (error) {
    console.error('下载旧案材料失败:', error)
  } finally {
    downloadingId.value = null
  }
}

const formatFileSize = (size) => {
  if (size === null || size === undefined) return '-'
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}

const formatDateTime = (value) => {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 16)
}

onMounted(() => loadCaseOptions())
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
    grid-template-columns: minmax(0, 2fr) minmax(180px, 1fr);
    column-gap: 18px;
  }

  .actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    margin: 4px 0 14px;
  }

  .status-panel {
    margin-top: 14px;
    padding: 14px 16px;
  }

  .status-row,
  .element-list {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
  }

  .status-text {
    color: #374151;
  }

  .record-text {
    margin-left: auto;
    color: #909399;
    font-size: 13px;
  }

  .element-list {
    margin-top: 12px;
    padding-top: 12px;
    border-top: 1px solid #eef1f5;
  }

  .element-title {
    color: #606266;
    font-size: 13px;
  }

  .result-panel {
    margin-top: 14px;
    padding: 16px;
  }

  .result-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    margin-bottom: 14px;

    h3 {
      margin: 0;
      font-size: 16px;
      color: #1f2937;
    }

    p,
    > span {
      margin: 5px 0 0;
      color: #6b7280;
      font-size: 13px;
    }
  }

  @media (max-width: 900px) {
    .form-grid {
      grid-template-columns: 1fr;
    }

    .record-text {
      margin-left: 0;
    }
  }
}
</style>
