<template>
  <div class="knowledge">
    <PageHeader title="知识库" />

    <div class="knowledge-container">
      <!-- 左侧分类导航 -->
      <div class="sidebar">
        <el-menu
          :default-active="activeType"
          @select="handleTypeChange"
          class="type-menu"
        >
          <el-menu-item index="">
            <el-icon><Document /></el-icon>
            <span>全部文章</span>
          </el-menu-item>
          <el-menu-item index="TEMPLATE">
            <el-icon><Tickets /></el-icon>
            <span>文档模板</span>
          </el-menu-item>
          <el-menu-item index="CASE">
            <el-icon><Folder /></el-icon>
            <span>类案检索</span>
          </el-menu-item>
          <el-menu-item index="GUIDE">
            <el-icon><Compass /></el-icon>
            <span>办案指南</span>
          </el-menu-item>
          <el-menu-item index="EXPERIENCE">
            <el-icon><ChatDotRound /></el-icon>
            <span>经验分享</span>
          </el-menu-item>
        </el-menu>

        <el-divider />

        <div class="quick-links">
          <h4>快捷入口</h4>
          <el-link @click="handleCreate" type="primary">
            <el-icon><Plus /></el-icon>
            新建文章
          </el-link>
          <el-link @click="loadMyArticles" style="display: block; margin-top: 10px;">
            <el-icon><User /></el-icon>
            我的文章
          </el-link>
        </div>
      </div>

      <!-- 右侧内容区 -->
      <div class="main-content">
        <!-- 搜索栏 -->
        <div class="search-bar">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索文章标题、内容、标签..."
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button @click="handleSearch" :icon="Search" />
            </template>
          </el-input>
        </div>

        <!-- 置顶文章 -->
        <div v-if="topArticles.length > 0" class="top-articles">
          <h3>
            <el-icon><Star /></el-icon>
            置顶推荐
          </h3>
          <div class="article-cards">
            <div
              v-for="article in topArticles"
              :key="article.id"
              class="article-card top"
              @click="handleView(article)"
            >
              <div class="card-header">
                <el-tag :type="getTypeTagType(article.articleType)" size="small">
                  {{ formatType(article.articleType) }}
                </el-tag>
                <span class="view-count">
                  <el-icon><View /></el-icon>
                  {{ article.viewCount }}
                </span>
              </div>
              <h4>{{ article.title }}</h4>
              <p class="summary">{{ article.summary || article.content?.replace(/<[^>]+>/g, '').substring(0, 100) }}</p>
              <div class="card-footer">
                <span class="author">{{ article.authorName }}</span>
                <span class="date">{{ formatDate(article.createdAt) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 文章列表 -->
        <div class="article-list">
          <div class="list-header">
            <h3>{{ currentTitle }}</h3>
            <el-button type="primary" @click="handleCreate">
              <el-icon><Plus /></el-icon>
              新建文章
            </el-button>
          </div>

          <el-empty v-if="articles.length === 0" description="暂无文章" />

          <div v-else class="article-items">
            <div
              v-for="article in articles"
              :key="article.id"
              class="article-item"
              @click="handleView(article)"
            >
              <div class="item-header">
                <h4 class="title">{{ article.title }}</h4>
                <el-tag :type="getTypeTagType(article.articleType)" size="small">
                  {{ formatType(article.articleType) }}
                </el-tag>
              </div>
              <p class="summary">{{ article.summary || article.content?.replace(/<[^>]+>/g, '').substring(0, 150) }}</p>
              <div class="item-meta">
                <span class="author">
                  <el-icon><User /></el-icon>
                  {{ article.authorName }}
                </span>
                <span class="category" v-if="article.category">
                  <el-icon><FolderOpened /></el-icon>
                  {{ article.category }}
                </span>
                <span class="tags" v-if="article.tags">
                  <el-icon><PriceTag /></el-icon>
                  {{ article.tags.split(',').slice(0, 3).join(', ') }}
                </span>
                <span class="stats">
                  <el-icon><View /></el-icon>
                  {{ article.viewCount }}
                  <el-icon style="margin-left: 10px;"><Star /></el-icon>
                  {{ article.likeCount }}
                </span>
                <span class="date">{{ formatDate(article.createdAt) }}</span>
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <el-pagination
            v-if="total > 0"
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            layout="total, prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Document, Tickets, Folder, Compass, ChatDotRound,
  Plus, User, Search, Star, View, FolderOpened, PriceTag
} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import request from '@/utils/request'

const router = useRouter()

const activeType = ref('')
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const topArticles = ref([])
const articles = ref([])

const currentTitle = computed(() => {
  if (searchKeyword.value) return `搜索结果：${searchKeyword.value}`
  const typeMap = {
    'TEMPLATE': '文档模板',
    'CASE': '类案检索',
    'GUIDE': '办案指南',
    'EXPERIENCE': '经验分享'
  }
  return typeMap[activeType.value] || '全部文章'
})

// 临时Mock数据（后端API暂时不可用）
const mockArticles = ref([
  {
    id: 1,
    title: '劳动合同解除通知书模板',
    articleType: 'TEMPLATE',
    category: '劳动',
    tags: '劳动合同,解除,通知',
    summary: '标准劳动合同解除通知书模板，包含多种解除情形',
    content: '<h2>劳动合同解除通知书</h2><p>尊敬的______：</p><p>根据《劳动合同法》相关规定...</p>',
    viewCount: 128,
    likeCount: 15,
    isTop: true,
    authorName: '张三律师',
    createdAt: '2026-04-15T10:30:00'
  },
  {
    id: 2,
    title: '合同纠纷胜诉案例：违约金过高调整',
    articleType: 'CASE',
    category: '合同',
    tags: '违约金,调整,胜诉',
    summary: '代理某公司买卖合同纠纷案，成功将违约金从30%调整至实际损失的1.3倍',
    content: '<h2>案情简介</h2><p>原告某公司起诉被告...</p>',
    viewCount: 256,
    likeCount: 32,
    isTop: true,
    authorName: '张三律师',
    createdAt: '2026-04-14T09:15:00'
  }
])

// 加载置顶文章
const loadTopArticles = async () => {
  try {
    const { data } = await request({
      url: '/knowledge/top',
      method: 'get'
    })
    topArticles.value = data || []
  } catch (error) {
    console.error('加载置顶文章失败:', error)
    ElMessage.error('加载置顶文章失败')
  }
}

// 加载文章列表
const loadArticles = async () => {
  try {
    const { data } = await request({
      url: '/knowledge',
      method: 'get',
      params: { page: currentPage.value - 1, size: pageSize.value }
    })
    articles.value = data.content || []
    total.value = data.totalElements || 0
  } catch (error) {
    console.error('加载文章列表失败:', error)
    ElMessage.error('加载文章列表失败')
  }
}

// 加载我的文章
const loadMyArticles = async () => {
  try {
    activeType.value = ''
    searchKeyword.value = ''
    const { data } = await request({
      url: '/knowledge/my',
      method: 'get',
      params: {
        page: 0,
        size: 100
      }
    })
    articles.value = data.content || []
    total.value = data.totalElements || 0
  } catch (error) {
    console.error('加载我的文章失败:', error)
    ElMessage.error('加载失败')
  }
}

// 类型切换
const handleTypeChange = (type) => {
  activeType.value = type
  searchKeyword.value = ''
  currentPage.value = 1
  loadArticles()
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  loadArticles()
}

// 分页
const handlePageChange = () => {
  loadArticles()
}

// 查看文章
const handleView = (article) => {
  router.push(`/knowledge/${article.id}`)
}

// 创建文章
const handleCreate = () => {
  router.push('/knowledge/create')
}

// 格式化类型
const formatType = (type) => {
  const map = {
    'TEMPLATE': '模板',
    'CASE': '类案',
    'GUIDE': '指南',
    'EXPERIENCE': '经验'
  }
  return map[type] || type
}

// 类型标签颜色
const getTypeTagType = (type) => {
  const map = {
    'TEMPLATE': 'success',
    'CASE': 'primary',
    'GUIDE': 'warning',
    'EXPERIENCE': 'info'
  }
  return map[type] || ''
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadTopArticles()
  loadArticles()
})
</script>

<style scoped lang="scss">
.knowledge {
  .knowledge-container {
    display: flex;
    gap: 20px;
    margin-top: 20px;
  }

  .sidebar {
    width: 200px;
    flex-shrink: 0;

    .type-menu {
      border-right: none;
    }

    .quick-links {
      padding: 10px 0;

      h4 {
        margin: 0 0 10px;
        font-size: 14px;
        color: #666;
      }

      .el-link {
        width: 100%;
      }
    }
  }

  .main-content {
    flex: 1;
    background: #fff;
    padding: 20px;
    border-radius: 4px;

    .search-bar {
      margin-bottom: 20px;
    }

    .top-articles {
      margin-bottom: 30px;

      h3 {
        display: flex;
        align-items: center;
        gap: 8px;
        margin: 0 0 15px;
        color: #333;
      }

      .article-cards {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
        gap: 15px;

        .article-card {
          padding: 15px;
          border: 1px solid #e4e7ed;
          border-radius: 8px;
          cursor: pointer;
          transition: all 0.3s;

          &:hover {
            box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
            transform: translateY(-2px);
          }

          &.top {
            border-color: #f56c6c;
            background: #fef0f0;
          }

          .card-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;

            .view-count {
              display: flex;
              align-items: center;
              gap: 4px;
              font-size: 12px;
              color: #999;
            }
          }

          h4 {
            margin: 0 0 10px;
            font-size: 16px;
            color: #333;
          }

          .summary {
            margin: 0 0 15px;
            font-size: 13px;
            color: #666;
            line-height: 1.6;
            display: -webkit-box;
            -webkit-line-clamp: 3;
            -webkit-box-orient: vertical;
            overflow: hidden;
          }

          .card-footer {
            display: flex;
            justify-content: space-between;
            font-size: 12px;
            color: #999;
          }
        }
      }
    }

    .article-list {
      .list-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 20px;

        h3 {
          margin: 0;
          color: #333;
        }
      }

      .article-items {
        .article-item {
          padding: 20px;
          border-bottom: 1px solid #e4e7ed;
          cursor: pointer;
          transition: background 0.3s;

          &:hover {
            background: #f9f9f9;
          }

          &:last-child {
            border-bottom: none;
          }

          .item-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            margin-bottom: 10px;

            .title {
              margin: 0;
              font-size: 16px;
              color: #303133;
              flex: 1;
            }
          }

          .summary {
            margin: 0 0 15px;
            color: #606266;
            line-height: 1.6;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
          }

          .item-meta {
            display: flex;
            gap: 20px;
            font-size: 13px;
            color: #909399;

            span {
              display: flex;
              align-items: center;
              gap: 4px;
            }
          }
        }
      }

      .el-pagination {
        margin-top: 20px;
        justify-content: center;
      }
    }
  }
}
</style>
