<template>
  <div class="rag-search-page">
    <PageHeader title="AI知识库" />

    <el-alert
      class="scope-alert"
      title="当前阶段仅用于公开法规、律所内部制度、公共模板和办案指引检索，请勿输入真实案件材料、客户隐私或未脱敏信息。"
      type="warning"
      show-icon
      :closable="false"
    />

    <div class="scope-grid">
      <section v-for="item in knowledgeScopes" :key="item.title" class="scope-item">
        <strong>{{ item.title }}</strong>
        <span>{{ item.description }}</span>
      </section>
    </div>

    <el-card class="search-card">
      <el-input
        v-model="question"
        placeholder="请输入法规、制度、模板或办案指引问题，例如：立案审批流程、利冲规则、合同违约责任..."
        size="large"
        @keyup.enter="handleSearch"
      >
        <template #append>
          <el-button :icon="Search" @click="handleSearch" :loading="loading">
            搜索
          </el-button>
        </template>
      </el-input>

      <div class="example-questions">
        <span class="example-label">常用问题</span>
        <el-tag
          v-for="q in exampleQuestions"
          :key="q"
          @click="question = q; handleSearch()"
          style="cursor: pointer; margin: 5px;"
        >
          {{ q }}
        </el-tag>
      </div>
    </el-card>

    <el-card v-if="answer" class="answer-card" v-loading="loading">
      <template #header>
        <span>AI回答</span>
      </template>

      <div class="answer-content" v-text="formattedAnswer"></div>

      <el-divider v-if="sources && sources.length > 0" />

      <div v-if="sources && sources.length > 0" class="sources-section">
        <h4>参考文档</h4>
        <el-collapse v-model="activeSources">
          <el-collapse-item
            v-for="(source, index) in sources"
            :key="index"
            :title="source.title"
            :name="String(index)"
          >
            <div class="source-detail">
              <p><strong>分类：</strong>{{ source.category }}</p>
              <p><strong>来源：</strong>{{ formatKnowledgeSource(source.knowledgeSource) }}</p>
              <p><strong>索引：</strong>{{ formatIndexStatus(source.indexStatus) }}</p>
              <p><strong>摘要：</strong>{{ source.summary }}</p>
              <el-button
                size="small"
                @click="viewDocument(source.id)"
                type="primary"
              >
                查看完整文档
              </el-button>
            </div>
          </el-collapse-item>
        </el-collapse>
      </div>

      <div class="answer-meta">
        <el-tag :type="hasAnswer ? 'success' : 'warning'">
          {{ hasAnswer ? '找到相关文档' : '未找到相关文档' }}
        </el-tag>
        <el-tag v-if="documentCount" type="info">
          引用 {{ documentCount }} 篇文档
        </el-tag>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { searchKnowledge, askAI } from '@/api/knowledge'

const question = ref('')
const answer = ref('')
const sources = ref([])
const hasAnswer = ref(false)
const documentCount = ref(0)
const loading = ref(false)
const activeSources = ref(['0', '1', '2'])

const knowledgeScopes = [
  {
    title: '法律法规',
    description: '用于检索公开法律、法规、司法解释和常用规则。'
  },
  {
    title: '律所制度',
    description: '用于沉淀立案、利冲、归档、财务和审批规则。'
  },
  {
    title: '公共模板',
    description: '用于查询可复用合同、函件、报告和申请材料模板。'
  },
  {
    title: '参考检索',
    description: '参考 Alpha 类法律检索思路，先做制度和知识定位。'
  }
]

const exampleQuestions = [
  '立案审批流程',
  '利益冲突审查规则',
  '案件归档目录要求',
  '发票申请需要哪些材料',
  '劳动仲裁申请流程',
  '合同违约责任认定',
  '诉讼时效计算'
]

const formattedAnswer = computed(() => {
  if (!answer.value) return ''

  // 保持纯文本渲染，避免知识库内容被作为HTML执行。
  return answer.value
    .replace(/\*\*(.*?)\*\*/g, '$1')
    .replace(/^- /gm, '• ')
})

const handleSearch = async () => {
  if (!question.value.trim()) {
    ElMessage.warning('请输入问题')
    return
  }

  loading.value = true
  answer.value = ''
  sources.value = []
  hasAnswer.value = false
  documentCount.value = 0

  try {
    const aiResponse = await askAI(question.value, { topK: 5 })
    const result = aiResponse.data || {}
    answer.value = result.answer || '暂未生成回答。'
    hasAnswer.value = Boolean(result.hasAnswer)
    documentCount.value = result.documentCount || result.sources?.length || 0
    sources.value = result.sources || []

  } catch (error) {
    console.warn('RAG接口不可用，降级为普通知识库检索', error)
    await fallbackKeywordSearch()
  } finally {
    loading.value = false
  }
}

const fallbackKeywordSearch = async () => {
  const searchResponse = await searchKnowledge(question.value, { size: 5 })
  const relevantDocs = searchResponse.data?.content || searchResponse.data?.records || searchResponse.data || []

  if (relevantDocs.length === 0) {
    answer.value = `抱歉，知识库中没有找到与"${question.value}"相关的内容。

建议：
1. 尝试使用不同的关键词
2. 检查输入是否有误
3. 联系专业律师咨询`
    hasAnswer.value = false
    return
  }

  answer.value = generateMockAnswer(relevantDocs)
  hasAnswer.value = true
  documentCount.value = relevantDocs.length
  sources.value = relevantDocs.slice(0, 3).map(normalizeSource)
}

const normalizeSource = (doc) => ({
  id: doc.id,
  title: doc.title,
  category: doc.category,
  knowledgeSource: doc.knowledgeSource,
  indexStatus: doc.indexStatus,
  summary: doc.summary || (doc.content ? `${doc.content.substring(0, 100)}...` : '')
})

// 生成增强的模拟答案（当AI不可用时）
const generateMockAnswer = (docs) => {
  if (!docs || docs.length === 0) {
    return '抱歉，没有找到相关信息。'
  }

  const topDoc = docs[0]
  let answer = `根据知识库检索结果，找到以下相关信息：\n\n`

  // 添加最相关的文档内容
  answer += `**${topDoc.title}**\n\n`
  if (topDoc.summary) {
    answer += `${topDoc.summary}\n\n`
  } else if (topDoc.content) {
    answer += `${topDoc.content.substring(0, 400)}...\n\n`
  }

  // 如果有多个相关文档，列出其他文档
  if (docs.length > 1) {
    answer += `其他相关文档：\n`
    docs.slice(1, 4).forEach((doc, index) => {
      answer += `${index + 1}. ${doc.title}\n`
    })
  }

  answer += `\n提示：`
  answer += `\n- 以上结果来自知识库文档检索`
  answer += `\n- 建议查看完整文档获取详细信息`
  answer += `\n- 如需更准确的解答，请咨询专业律师`
  answer += `\n- 配置AI服务可获得智能问答功能`

  return answer
}

const viewDocument = (id) => {
  window.open(`#/knowledge/${id}`, '_blank')
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
</script>

<style scoped lang="scss">
.rag-search-page {
  max-width: 900px;
  margin: 0 auto;

  .scope-alert {
    margin-bottom: 14px;
  }

  .scope-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 10px;
    margin-bottom: 16px;
  }

  .scope-item {
    min-height: 78px;
    padding: 12px;
    border: 1px solid #e8edf3;
    border-radius: 6px;
    background: #fff;

    strong {
      display: block;
      margin-bottom: 6px;
      font-size: 14px;
      color: #1f2937;
    }

    span {
      font-size: 12px;
      line-height: 1.5;
      color: #6b7280;
    }
  }

  .search-card {
    margin-bottom: 20px;

    .example-questions {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
      margin-top: 15px;

      .example-label {
        font-size: 13px;
        color: #6b7280;
      }
    }
  }

  .answer-card {
    .answer-content {
      font-size: 15px;
      line-height: 1.8;
      color: #333;
      white-space: pre-wrap;
    }

    .sources-section {
      margin-top: 20px;

      h4 {
        margin-bottom: 15px;
        color: #606266;
      }

      .source-detail {
        p {
          margin: 8px 0;
          color: #606266;
        }
      }
    }

    .answer-meta {
      margin-top: 20px;
      display: flex;
      gap: 10px;
    }
  }

  @media (max-width: 900px) {
    .scope-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }

  @media (max-width: 560px) {
    .scope-grid {
      grid-template-columns: 1fr;
    }
  }
}
</style>
