<template>
  <div class="knowledge-detail" v-loading="loading">
    <PageHeader title="文章详情" />

    <div class="detail-container" v-if="article">
      <!-- 文章头部 -->
      <div class="article-header">
        <h1>{{ article.title }}</h1>
        <div class="article-meta">
          <el-tag :type="getTypeTagType(article.articleType)">
            {{ formatType(article.articleType) }}
          </el-tag>
          <el-tag effect="plain">
            {{ formatKnowledgeSource(article.knowledgeSource) }}
          </el-tag>
          <el-tag :type="getIndexStatusType(article.indexStatus)">
            {{ formatIndexStatus(article.indexStatus) }}
          </el-tag>
          <el-tag v-if="article.knowledgeEligible === false" type="info">
            不进入AI知识库
          </el-tag>
          <span class="category" v-if="article.category">
            <el-icon><FolderOpened /></el-icon>
            {{ article.category }}
          </span>
          <span class="tags" v-if="article.tags">
            <el-icon><PriceTag /></el-icon>
            {{ article.tags }}
          </span>
        </div>
        <div class="article-info">
          <span class="author">
            <el-icon><User /></el-icon>
            {{ article.authorName }}
          </span>
          <span class="date">
            <el-icon><Calendar /></el-icon>
            {{ formatDate(article.createdAt) }}
          </span>
          <span class="stats">
            <el-icon><View /></el-icon>
            {{ article.viewCount }} 浏览
          </span>
          <span class="stats">
            <el-icon><Star /></el-icon>
            {{ article.likeCount }} 点赞
          </span>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="article-actions">
        <el-button type="primary" @click="handleLike">
          <el-icon><Star /></el-icon>
          点赞 ({{ article.likeCount }})
        </el-button>
        <el-button @click="handleEdit" v-if="canEdit">
          <el-icon><Edit /></el-icon>
          编辑
        </el-button>
        <el-button @click="handleBack">
          <el-icon><Back /></el-icon>
          返回列表
        </el-button>
      </div>

      <!-- 文章内容 -->
      <div class="article-content">
        <div class="summary" v-if="article.summary">
          <h3>摘要</h3>
          <p>{{ article.summary }}</p>
        </div>
        <div class="content" v-html="article.content"></div>
      </div>

      <!-- 附件 -->
      <div class="article-attachment" v-if="article.attachmentPath">
        <h3>
          <el-icon><Paperclip /></el-icon>
          附件
        </h3>
        <el-link :href="article.attachmentPath" target="_blank" type="primary">
          下载附件
        </el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  FolderOpened, PriceTag, User, Calendar, View, Star,
  Edit, Back, Paperclip
} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import request from '@/utils/request'
import { useUserStore } from '@/stores'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const article = ref(null)

// 权限检查：只有创建者或管理员可以编辑
const canEdit = computed(() => {
  const userStore = useUserStore()
  if (!userStore || !userStore.user) return false
  if (userStore.user.role === 'ADMIN') return true
  if (!article.value) return false
  return article.value.createdBy === userStore.user.id
})

// 加载文章详情
const loadArticle = async () => {
  loading.value = true
  try {
    const { data } = await request({
      url: `/knowledge/${route.params.id}`,
      method: 'get'
    })
    article.value = data
  } catch (error) {
    console.error('加载文章失败:', error)
    ElMessage.error('加载文章失败')
    router.back()
  } finally {
    loading.value = false
  }
}

// 点赞
const handleLike = async () => {
  try {
    await request({
      url: `/knowledge/${route.params.id}/like`,
      method: 'post'
    })
    article.value.likeCount++
    ElMessage.success('点赞成功')
  } catch (error) {
    console.error('点赞失败:', error)
    ElMessage.error('点赞失败')
  }
}

// 编辑
const handleEdit = () => {
  router.push(`/knowledge/${route.params.id}/edit`)
}

// 返回
const handleBack = () => {
  router.push('/knowledge/list')
}

// 格式化类型
const formatType = (type) => {
  const map = {
    'TEMPLATE': '文档模板',
    'CASE': '类案检索',
    'GUIDE': '办案指南',
    'EXPERIENCE': '经验分享'
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

const formatKnowledgeSource = (source) => {
  const map = {
    LAW_REGULATION: '法律法规',
    FIRM_POLICY: '律所制度',
    PUBLIC_TEMPLATE: '公共模板',
    REFERENCE_MATERIAL: '参考资料',
    FIRM_KNOWLEDGE: '全所知识',
    CASE_DEPOSIT: '案件沉淀'
  }
  return map[source] || '全所知识'
}

const formatIndexStatus = (status) => {
  const map = {
    PENDING: '待索引',
    INDEXED: '已索引',
    FAILED: '索引失败',
    FORBIDDEN: '禁止索引',
    NOT_INDEXED: '未索引'
  }
  return map[status] || '未索引'
}

const getIndexStatusType = (status) => {
  const map = {
    INDEXED: 'success',
    PENDING: 'warning',
    FAILED: 'danger',
    FORBIDDEN: 'info'
  }
  return map[status] || 'info'
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleString('zh-CN')
}

onMounted(() => {
  loadArticle()
})
</script>

<style scoped lang="scss">
.knowledge-detail {
  .detail-container {
    max-width: 900px;
    margin: 20px auto;
    background: #fff;
    padding: 40px;
    border-radius: 4px;

    .article-header {
      border-bottom: 1px solid #e4e7ed;
      padding-bottom: 20px;
      margin-bottom: 20px;

      h1 {
        margin: 0 0 15px;
        font-size: 28px;
        color: #303133;
      }

      .article-meta {
        display: flex;
        gap: 10px;
        flex-wrap: wrap;
        margin-bottom: 15px;

        span {
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: 14px;
          color: #606266;
        }
      }

      .article-info {
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

    .article-actions {
      margin-bottom: 30px;
      display: flex;
      gap: 10px;
    }

    .article-content {
      .summary {
        padding: 15px;
        background: #f9f9f9;
        border-left: 3px solid #409eff;
        margin-bottom: 30px;

        h3 {
          margin: 0 0 10px;
          font-size: 16px;
        }

        p {
          margin: 0;
          color: #606266;
          line-height: 1.8;
        }
      }

      .content {
        font-size: 15px;
        line-height: 1.8;
        color: #303133;

        :deep(h2) {
          margin: 30px 0 15px;
          font-size: 22px;
          color: #303133;
          border-bottom: 1px solid #e4e7ed;
          padding-bottom: 10px;
        }

        :deep(h3) {
          margin: 25px 0 12px;
          font-size: 18px;
          color: #303133;
        }

        :deep(p) {
          margin: 0 0 15px;
        }

        :deep(ul), :deep(ol) {
          margin: 0 0 15px;
          padding-left: 30px;
        }

        :deep(li) {
          margin-bottom: 8px;
        }
      }
    }

    .article-attachment {
      margin-top: 30px;
      padding-top: 20px;
      border-top: 1px solid #e4e7ed;

      h3 {
        display: flex;
        align-items: center;
        gap: 8px;
        margin: 0 0 10px;
        font-size: 16px;
      }
    }
  }
}
</style>
