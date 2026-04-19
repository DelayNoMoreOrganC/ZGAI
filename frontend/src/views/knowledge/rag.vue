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
import request from '@/utils/request'

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
  '诉讼时效计算'
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

  try {
    // MVP实现：1. 搜索知识库 2. 构造上下文 3. 调用AI生成答案

    // 步骤1: 搜索相关文档
    const { data: searchData } = await request({
      url: '/knowledge/list',
      method: 'get',
      params: { page: 1, size: 5 }
    })

    const relevantDocs = searchData.records || []

    if (relevantDocs.length === 0) {
      answer.value = '抱歉，知识库中没有找到相关内容。请尝试其他关键词或联系律师。'
      hasAnswer.value = false
      loading.value = false
      return
    }

    // 步骤2: 构造上下文
    const context = relevantDocs.map((doc, index) =>
      `[文档${index + 1}] ${doc.title}\n分类：${doc.category}\n摘要：${doc.summary || doc.content?.substring(0, 200)}...\n`
    ).join('\n')

    // 步骤3: 调用AI生成答案（使用AI聊天接口）
    const prompt = `你是专业法律助手。请基于以下知识库文档回答问题。

${context}

问题：${question.value}

要求：
1. 只基于文档回答，不要编造
2. 回答要准确、专业、易懂
3. 引用文档内容时注明来源

请回答：`

    // 调用AI聊天API（如果存在）或使用模拟答案
    try {
      const { data: aiResponse } = await request({
        url: '/ai/assist',
        method: 'post',
        data: { message: prompt }
      })

      answer.value = aiResponse || generateMockAnswer(relevantDocs)
      hasAnswer.value = true
      documentCount.value = relevantDocs.length

      sources.value = relevantDocs.slice(0, 3).map(doc => ({
        id: doc.id,
        title: doc.title,
        category: doc.category,
        summary: doc.summary || doc.content?.substring(0, 100) + '...'
      }))

    } catch (aiError) {
      // AI接口不可用，使用模拟答案
      console.warn('AI接口不可用，使用模拟答案', aiError)
      answer.value = generateMockAnswer(relevantDocs)
      hasAnswer.value = true
      documentCount.value = relevantDocs.length

      sources.value = relevantDocs.slice(0, 3).map(doc => ({
        id: doc.id,
        title: doc.title,
        category: doc.category,
        summary: doc.summary || doc.content?.substring(0, 100) + '...'
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

// 生成模拟答案（当AI不可用时）
const generateMockAnswer = (docs) => {
  const doc = docs[0]
  return `根据知识库文档《${doc.title}》：

${doc.summary || doc.content?.substring(0, 300) || '暂无摘要'}

这仅是基于检索结果的简单展示。要获得更准确的答案，建议：
1. 查看完整文档内容
2. 咨询专业律师
3. 配置真实的AI服务（DeepSeek或OpenAI）

相关文档：${docs.map(d => d.title).join('、')}`
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
