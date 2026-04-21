<template>
  <div class="case-search-page">
    <PageHeader title="类案检索" />

    <div class="search-container">
      <el-card class="search-card">
        <el-form :inline="true" :model="searchForm" @submit.prevent="handleSearch">
          <el-form-item label="关键词">
            <el-input
              v-model="searchForm.keyword"
              placeholder="输入案由、当事人、法院名称、法条等"
              style="width: 400px"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>

          <el-form-item label="案件类型">
            <el-select v-model="searchForm.caseType" placeholder="全部" clearable style="width: 150px">
              <el-option label="民事案件" value="民事" />
              <el-option label="刑事案件" value="刑事" />
              <el-option label="行政案件" value="行政" />
              <el-option label="执行案件" value="执行" />
            </el-select>
          </el-form-item>

          <el-form-item label="审理法院">
            <el-input v-model="searchForm.court" placeholder="法院名称" clearable style="width: 200px" />
          </el-form-item>

          <el-form-item label="文书类型">
            <el-select v-model="searchForm.docType" placeholder="全部" clearable style="width: 150px">
              <el-option label="判决书" value="判决书" />
              <el-option label="裁定书" value="裁定书" />
              <el-option label="调解书" value="调解书" />
              <el-option label="决定书" value="决定书" />
            </el-select>
          </el-form-item>

          <el-form-item label="时间范围">
            <el-date-picker
              v-model="searchForm.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="width: 250px"
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" @click="handleSearch" :loading="loading">
              搜索
            </el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="results-card" v-loading="loading">
        <template #header>
          <span>找到 <strong>{{ total }}</strong> 个相关案例</span>
        </template>

        <el-empty v-if="results.length === 0 && !loading" description="暂无搜索结果" />

        <div v-else class="case-list">
          <div v-for="item in results" :key="item.id" class="case-item" @click="handleView(item)">
            <div class="case-title">
              <h3>{{ item.title || '案号：' + item.caseNumber }}</h3>
              <el-tag size="small" v-if="item.caseType">{{ item.caseType }}</el-tag>
            </div>

            <div class="case-meta">
              <span>案号：{{ item.caseNumber }}</span>
              <span>法院：{{ item.court }}</span>
              <span>日期：{{ item.judgmentDate }}</span>
              <span v-if="item.docType">类型：{{ item.docType }}</span>
            </div>

            <div class="case-summary" v-if="item.caseBrief">
              {{ item.caseBrief }}
            </div>

            <div class="case-actions" style="margin-top: 10px;">
              <el-button size="small" type="primary" @click.stop="handleViewDetail(item)">
                查看详情
              </el-button>
              <el-button size="small" @click.stop="handleSimilar(item)">
                相似案例
              </el-button>
            </div>
          </div>
        </div>

        <el-pagination
          v-if="total > 0"
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="handleSearch"
          style="margin-top: 20px; justify-content: center"
        />
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import request from '@/utils/request'

const router = useRouter()

const searchForm = ref({
  keyword: '',
  caseType: '',
  court: '',
  docType: '',
  dateRange: null
})

const loading = ref(false)
const results = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const handleSearch = async () => {
  if (!searchForm.value.keyword) {
    ElMessage.warning('请输入关键词')
    return
  }

  loading.value = true

  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value,
      q: searchForm.value.keyword
    }

    // 添加可选过滤参数
    if (searchForm.value.caseType) {
      params.caseType = searchForm.value.caseType
    }
    if (searchForm.value.court) {
      params.court = searchForm.value.court
    }
    if (searchForm.value.docType) {
      params.docType = searchForm.value.docType
    }
    if (searchForm.value.dateRange && searchForm.value.dateRange.length === 2) {
      params.startDate = searchForm.value.dateRange[0]
      params.endDate = searchForm.value.dateRange[1]
    }

    const { data } = await request({
      url: '/search',
      method: 'get',
      params
    })

    results.value = data.content || data || []
    total.value = data.totalElements || results.value.length

    if (results.value.length === 0) {
      ElMessage.info('未找到相关案例，请尝试其他关键词')
    }
  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error('搜索失败，请稍后再试')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  searchForm.value = {
    keyword: '',
    caseType: '',
    court: '',
    docType: '',
    dateRange: null
  }
  results.value = []
  total.value = 0
}

const handleView = (item) => {
  if (item.id) {
    router.push(`/case/${item.id}`)
  }
}

const handleViewDetail = (item) => {
  // 打开案例详情页或对话框
  if (item.id) {
    router.push(`/case/${item.id}`)
  } else if (item.url) {
    window.open(item.url, '_blank')
  } else {
    ElMessage.info('该案例暂无详细信息')
  }
}

const handleSimilar = (item) => {
  // 基于当前案例搜索相似案例
  searchForm.value.keyword = item.caseBrief || item.title || item.caseNumber
  handleSearch()
}
</script>

<style scoped lang="scss">
.case-search-page {
  .search-container {
    margin-top: 20px;
  }

  .search-card, .results-card {
    margin-bottom: 20px;
  }

  .case-list {
    .case-item {
      padding: 20px;
      border-bottom: 1px solid #e4e7ed;
      cursor: pointer;
      transition: all 0.3s;

      &:hover {
        background: #f9f9f9;
        box-shadow: 0 2px 8px rgba(0,0,0,0.1);
      }

      .case-title {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 10px;

        h3 {
          margin: 0;
          font-size: 16px;
          color: #303133;
        }
      }

      .case-meta {
        display: flex;
        gap: 20px;
        font-size: 13px;
        color: #909399;
        margin-bottom: 10px;
        flex-wrap: wrap;
      }

      .case-summary {
        color: #606266;
        line-height: 1.6;
        margin-bottom: 10px;
      }

      .case-actions {
        display: flex;
        gap: 10px;
      }
    }
  }

  // 移动端适配
  @media (max-width: 768px) {
    .search-card {
      :deep(.el-form-item) {
        width: 100% !important;
        margin-bottom: 15px;
      }

      :deep(.el-input),
      :deep(.el-select),
      :deep(.el-date-picker) {
        width: 100% !important;
      }
    }

    .case-item {
      .case-meta {
        gap: 10px;
        font-size: 12px;
      }

      .case-actions {
        flex-direction: column;

        .el-button {
          width: 100%;
        }
      }
    }
  }
}
</style>
