<template>
  <div class="search-page">
    <PageHeader title="全局搜索">
      <template #extra>
        <el-input
          v-model="searchKeyword"
          placeholder="输入关键词搜索..."
          clearable
          style="width: 400px"
          @input="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" @click="handleManualSearch">搜索</el-button>
      </template>
    </PageHeader>

    <!-- 搜索类型筛选 -->
    <div class="filter-section">
      <el-radio-group v-model="searchType" @change="handleTypeChange">
        <el-radio-button label="all">全部</el-radio-button>
        <el-radio-button label="case">案件</el-radio-button>
        <el-radio-button label="client">客户</el-radio-button>
        <el-radio-button label="document">文档</el-radio-button>
      </el-radio-group>

      <div class="result-count">
        找到 <span class="count">{{ searchResults.length }}</span> 条结果
      </div>
    </div>

    <!-- 搜索结果 -->
    <div v-loading="loading" class="search-results">
      <div v-if="!loading && searchResults.length === 0 && hasSearched" class="no-results">
        <el-empty description="未找到相关结果" />
      </div>

      <div v-else-if="!loading && searchResults.length > 0" class="results-list">
        <div
          v-for="(result, index) in searchResults"
          :key="index"
          class="result-item"
          @click="handleResultClick(result)"
        >
          <div class="result-header">
            <el-tag :type="getTypeColor(result.type)" size="small">
              {{ result.typeDesc }}
            </el-tag>
            <span class="match-field">匹配字段：{{ result.matchField }}</span>
          </div>

          <div class="result-title">
            <el-link :underline="false" type="primary">
              {{ result.title }}
            </el-link>
          </div>

          <div class="result-subtitle">
            {{ result.subtitle }}
          </div>
        </div>
      </div>

      <div v-else-if="!loading && !hasSearched" class="search-hint">
        <el-empty description="请输入关键词进行搜索" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { globalSearch } from '@/api/search'
import { debounce } from 'lodash-es'

const router = useRouter()
const route = useRoute()

const searchKeyword = ref('')
const searchType = ref('all')
const searchResults = ref([])
const loading = ref(false)
const hasSearched = ref(false)

// 页面加载时，如果 URL 有搜索关键词参数，自动搜索
onMounted(() => {
  const query = route.query.q
  if (query) {
    searchKeyword.value = query
    performSearch()
  }
})

// 执行搜索
const performSearch = async () => {
  if (!searchKeyword.value.trim()) {
    return
  }

  if (searchKeyword.value.length < 2) {
    return
  }

  try {
    loading.value = true
    hasSearched.value = true

    const res = await globalSearch(searchKeyword.value, searchType.value)

    if (res.success) {
      searchResults.value = res.data || []
    } else {
      ElMessage.error(res.message || '搜索失败')
      searchResults.value = []
    }
  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error('搜索失败，请稍后重试')
    searchResults.value = []
  } finally {
    loading.value = false
  }
}

// 使用 debounce 的搜索函数（300ms）
const handleSearch = debounce(performSearch, 300)

// 手动触发搜索（用于按钮点击）
const handleManualSearch = () => {
  handleSearch.cancel()  // 取消待执行的 debounce
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  if (searchKeyword.value.length < 2) {
    ElMessage.warning('搜索关键词至少包含2个字符')
    return
  }
  performSearch()
}

// 切换搜索类型
const handleTypeChange = () => {
  if (searchKeyword.value.trim()) {
    handleSearch.cancel()
    performSearch()
  }
}

// 点击搜索结果
const handleResultClick = (result) => {
  if (result.url) {
    router.push(result.url)
  }
}

// 获取类型对应的颜色
const getTypeColor = (type) => {
  const colorMap = {
    'CASE': 'success',
    'CLIENT': 'primary',
    'DOCUMENT': 'warning'
  }
  return colorMap[type] || 'info'
}
</script>

<style scoped lang="scss">
.search-page {
  .filter-section {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding: 15px 20px;
    background-color: #f5f7fa;
    border-radius: 4px;

    .result-count {
      font-size: 14px;
      color: #606266;

      .count {
        font-size: 18px;
        font-weight: bold;
        color: #409eff;
        margin: 0 4px;
      }
    }
  }

  .search-results {
    min-height: 400px;

    .results-list {
      .result-item {
        padding: 20px;
        margin-bottom: 15px;
        background-color: #fff;
        border: 1px solid #e4e7ed;
        border-radius: 4px;
        cursor: pointer;
        transition: all 0.3s;

        &:hover {
          box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
          border-color: #409eff;
        }

        .result-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 10px;

          .match-field {
            font-size: 12px;
            color: #909399;
          }
        }

        .result-title {
          font-size: 16px;
          font-weight: 500;
          color: #303133;
          margin-bottom: 8px;
        }

        .result-subtitle {
          font-size: 14px;
          color: #606266;
          line-height: 1.5;
        }
      }
    }

    .search-hint,
    .no-results {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 400px;
    }
  }
}
</style>