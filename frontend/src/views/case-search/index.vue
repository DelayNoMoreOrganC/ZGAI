<template>
  <div class="case-search-page">
    <PageHeader title="类案检索" />

    <div class="search-container">
      <el-card class="search-card">
        <el-form :inline="true" :model="searchForm" @submit.prevent="handleSearch">
          <el-form-item label="关键词">
            <el-input
              v-model="searchForm.keyword"
              placeholder="输入案由、当事人、法院名称等"
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
            </el-select>
          </el-form-item>

          <el-form-item label="审理法院">
            <el-input v-model="searchForm.court" placeholder="法院名称" clearable style="width: 200px" />
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
            </div>

            <div class="case-summary" v-if="item.caseBrief">
              {{ item.caseBrief }}
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
  court: ''
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
    const { data } = await request({
      url: '/search',
      method: 'get',
      params: {
        page: currentPage.value - 1,
        size: pageSize.value,
        q: searchForm.value.keyword
      }
    })

    results.value = data.content || data || []
    total.value = data.totalElements || results.value.length
  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  searchForm.value = {
    keyword: '',
    caseType: '',
    court: ''
  }
  results.value = []
  total.value = 0
}

const handleView = (item) => {
  if (item.id) {
    router.push(`/case/${item.id}`)
  }
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

      &:hover {
        background: #f9f9f9;
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
      }

      .case-summary {
        color: #606266;
        line-height: 1.6;
      }
    }
  }
}
</style>
