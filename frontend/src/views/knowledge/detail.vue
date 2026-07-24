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
          <el-tag v-if="article.reviewStatus === 'PENDING_REVIEW'" type="warning">待审核</el-tag>
          <el-tag v-if="article.reviewStatus === 'REJECTED'" type="danger">已驳回</el-tag>
          <el-tag
            v-if="article.knowledgeSource === 'LAW_REGULATION'"
            :type="getValidityStatusType(article.validityStatus)"
          >
            {{ formatValidityStatus(article.validityStatus) }}
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

      <el-alert
        v-if="article.reviewStatus === 'REJECTED'"
        class="review-result"
        :title="`审核驳回：${article.reviewReason || '请修订后重新提交'}`"
        type="error"
        :closable="false"
        show-icon
      />

      <div class="source-metadata" v-if="hasSourceMetadata">
        <div v-if="article.issuingAuthority"><span>发布机关</span><strong>{{ article.issuingAuthority }}</strong></div>
        <div v-if="article.documentNumber"><span>文号/编号</span><strong>{{ article.documentNumber }}</strong></div>
        <div v-if="article.effectiveDate"><span>生效日期</span><strong>{{ formatDateOnly(article.effectiveDate) }}</strong></div>
        <div v-if="article.sourceReference"><span>来源依据</span><strong>{{ article.sourceReference }}</strong></div>
        <div v-if="article.knowledgeSource === 'REFERENCE_MATERIAL'">
          <span>授权状态</span>
          <strong>{{ article.authorizationConfirmed ? '已确认内部使用授权' : '尚未确认' }}</strong>
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
        <el-button @click="handleReindex" v-if="canEdit && article.knowledgeEligible" :loading="reindexing">
          <el-icon><Refresh /></el-icon>
          重新索引
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
        <div class="content" v-text="articlePlainText"></div>
      </div>

      <!-- 附件 -->
      <div class="article-attachment" v-if="article.attachmentPath">
        <h3>
          <el-icon><Paperclip /></el-icon>
          附件
        </h3>
        <el-button link type="primary" :loading="attachmentDownloading" @click="handleDownloadAttachment">
          {{ article.attachmentName || '下载原始文档' }}
        </el-button>
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
  Edit, Back, Paperclip, Refresh
} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import request from '@/utils/request'
import { downloadKnowledgeAttachment } from '@/api/knowledge'
import { useUserStore } from '@/stores'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const article = ref(null)
const userStore = useUserStore()
const attachmentDownloading = ref(false)
const reindexing = ref(false)

// 权限检查：只有创建者或管理员可以编辑
const canEdit = computed(() => {
  if (!article.value) return false
  const user = userStore.userInfo || {}
  if (userStore.hasPermission('KNOWLEDGE_MANAGE')) return true
  return Number(article.value.authorId) === Number(user.id)
})

const articlePlainText = computed(() => {
  const content = article.value?.content || ''
  if (!content.includes('<')) return content
  return new DOMParser().parseFromString(content, 'text/html').body.textContent || ''
})

const hasSourceMetadata = computed(() => {
  const value = article.value
  return Boolean(value && (
    value.issuingAuthority || value.documentNumber || value.effectiveDate ||
    value.sourceReference || value.knowledgeSource === 'REFERENCE_MATERIAL'
  ))
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

const handleDownloadAttachment = async () => {
  attachmentDownloading.value = true
  try {
    const response = await downloadKnowledgeAttachment(route.params.id)
    const url = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.download = article.value.attachmentName || '知识文档'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } catch (error) {
    console.error('下载知识文档失败:', error)
    ElMessage.error('下载知识文档失败')
  } finally {
    attachmentDownloading.value = false
  }
}

const handleReindex = async () => {
  reindexing.value = true
  try {
    const { data } = await request({
      url: `/knowledge/${route.params.id}/reindex`,
      method: 'post'
    })
    article.value = data
    const message = data.indexStatus === 'INDEXED'
      ? '知识文档已完成语义索引'
      : '当前未配置向量服务，已保留关键词检索'
    ElMessage.success(message)
  } catch (error) {
    console.error('重新索引失败:', error)
  } finally {
    reindexing.value = false
  }
}

// 格式化类型
const formatType = (type) => {
  const map = {
    'DOCUMENT': '知识文档',
    'TEMPLATE': '公共模板',
    'CASE': '案件沉淀',
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

const formatValidityStatus = (status) => {
  const map = {
    EFFECTIVE: '现行有效',
    AMENDED: '已修订',
    REPEALED: '已废止',
    UNKNOWN: '待核验'
  }
  return map[status] || '待核验'
}

const getValidityStatusType = (status) => {
  const map = {
    EFFECTIVE: 'success',
    AMENDED: 'warning',
    REPEALED: 'danger',
    UNKNOWN: 'info'
  }
  return map[status] || 'info'
}

const formatDateOnly = (date) => {
  if (!date) return ''
  if (Array.isArray(date)) {
    const [year, month, day] = date
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  }
  return String(date).substring(0, 10)
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return ''
  if (Array.isArray(date)) {
    const [year, month, day, hour = 0, minute = 0] = date
    const pad = number => String(number).padStart(2, '0')
    return `${year}-${pad(month)}-${pad(day)} ${pad(hour)}:${pad(minute)}`
  }
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

    .source-metadata {
      display: grid;
      grid-template-columns: repeat(2, minmax(0, 1fr));
      gap: 12px 24px;
      margin-bottom: 24px;
      padding: 16px 0;
      border-bottom: 1px solid #e4e7ed;

      div {
        display: flex;
        flex-direction: column;
        gap: 4px;
        min-width: 0;
      }

      span {
        color: #909399;
        font-size: 12px;
      }

      strong {
        color: #303133;
        font-size: 14px;
        font-weight: 500;
        overflow-wrap: anywhere;
      }
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
        white-space: pre-wrap;
        word-break: break-word;

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
