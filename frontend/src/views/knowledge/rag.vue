<template>
  <div class="rag-search-page">
    <PageHeader title="AI知识问答" />

    <el-card class="search-card">
      <el-input
        v-model="question"
        placeholder="请输入法律问题，例如：劳动仲裁申请流程、合同违约责任..."
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

      <div class="answer-content" v-html="formattedAnswer"></div>

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

const exampleQuestions = [
  '劳动仲裁申请流程',
  '合同违约责任认定',
  '刑事案件辩护要点',
  '如何收集证据',
  '诉讼时效计算',
  '离婚案件财产分割',
  '交通事故赔偿标准',
  '借款合同利息计算'
]

const formattedAnswer = computed(() => {
  if (!answer.value) return ''

  // 简单的Markdown格式化
  return answer.value
    .replace(/\n\n/g, '</p><p>')
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/- (.*?)(<br>|$)/g, '• $1$2')
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
    // 步骤1: 搜索相关文档
    const searchResponse = await searchKnowledge(question.value, { size: 5 })
    const relevantDocs = searchResponse.data?.records || searchResponse.data || []

    if (relevantDocs.length === 0) {
      answer.value = `抱歉，知识库中没有找到与"${question.value}"相关的内容。

建议：
1. 尝试使用不同的关键词
2. 检查输入是否有误
3. 联系专业律师咨询`
      hasAnswer.value = false
      loading.value = false
      return
    }

    // 步骤2: 尝试调用AI生成答案
    try {
      const aiResponse = await askAI(question.value, {
        context: relevantDocs.map(doc => ({
          id: doc.id,
          title: doc.title,
          content: doc.content || doc.summary
        }))
      })

      answer.value = aiResponse.data?.answer || generateMockAnswer(relevantDocs)
      hasAnswer.value = true
      documentCount.value = relevantDocs.length

      sources.value = relevantDocs.slice(0, 3).map(doc => ({
        id: doc.id,
        title: doc.title,
        category: doc.category,
        summary: doc.summary || (doc.content?.substring(0, 100) + '...')
      }))

    } catch (aiError) {
      // AI接口不可用，使用增强的模拟答案
      console.warn('AI接口不可用，使用增强答案', aiError)
      answer.value = generateMockAnswer(relevantDocs)
      hasAnswer.value = true
      documentCount.value = relevantDocs.length

      sources.value = relevantDocs.slice(0, 3).map(doc => ({
        id: doc.id,
        title: doc.title,
        category: doc.category,
        summary: doc.summary || (doc.content?.substring(0, 100) + '...')
      }))
    }

  } catch (error) {
    console.error('搜索失败', error)
    ElMessage.error('搜索失败，请稍后再试')
    answer.value = '系统暂时无法回答您的问题，请稍后再试。'
    hasAnswer.value = false
  } finally {
    loading.value = false
  }
}

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

  answer += `\n💡 **提示**：`
  answer += `\n- 以上结果来自知识库文档检索`
  answer += `\n- 建议查看完整文档获取详细信息`
  answer += `\n- 如需更准确的解答，请咨询专业律师`
  answer += `\n- 配置AI服务可获得智能问答功能`

  return answer
}

const viewDocument = (id) => {
  window.open(`#/knowledge/${id}`, '_blank')
}
</script>

<style scoped lang="scss">
.rag-search-page {
  max-width: 900px;
  margin: 0 auto;

  .search-card {
    margin-bottom: 20px;

    .example-questions {
      margin-top: 15px;
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
}
</style>
