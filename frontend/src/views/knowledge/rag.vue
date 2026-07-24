<template>
  <div class="rag-search-page">
    <PageHeader title="AI知识库">
      <template #extra>
        <div class="header-actions">
          <el-button v-if="canManageKnowledge" :icon="DataAnalysis" @click="toggleEvaluationPanel">
            检索评价
          </el-button>
          <el-select v-model="providerType" class="provider-select" :loading="providerLoading">
            <el-option
              v-for="provider in availableProviders"
              :key="provider.providerType"
              :label="`${provider.displayName} · ${provider.modelName || '未命名模型'}`"
              :value="provider.providerType"
              :disabled="!provider.available"
            />
          </el-select>
        </div>
      </template>
    </PageHeader>

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

    <section v-if="canManageKnowledge && evaluationPanelVisible" class="evaluation-panel">
      <div class="evaluation-heading">
        <div>
          <h3>RAG 检索评价</h3>
          <p>评价只执行本地检索，不调用生成模型。预期文档须为已审核且允许共享检索的知识条目。</p>
        </div>
        <div class="evaluation-heading-actions">
          <el-button :icon="Download" :loading="templateDownloading" @click="downloadEvaluationTemplate">
            下载导入模板
          </el-button>
          <el-button type="primary" :icon="Plus" @click="startCreateEvaluation">新增样本</el-button>
        </div>
      </div>

      <section class="evaluation-import">
        <div class="evaluation-import-controls">
          <el-upload
            ref="evaluationUploadRef"
            action="#"
            accept=".xlsx"
            :auto-upload="false"
            :limit="1"
            :on-change="selectEvaluationWorkbook"
            :on-remove="clearEvaluationWorkbook"
          >
            <el-button :icon="Upload">选择评价样本</el-button>
          </el-upload>
          <el-button
            type="primary"
            plain
            :disabled="!evaluationWorkbook"
            :loading="evaluationImporting"
            @click="previewEvaluationWorkbook"
          >
            预检文件
          </el-button>
          <el-button
            type="success"
            :disabled="!evaluationWorkbook || !evaluationImportPreview?.canImport"
            :loading="evaluationImporting"
            @click="confirmEvaluationWorkbook"
          >
            确认导入
          </el-button>
        </div>
        <p>模板中的文档清单仅含ID和标题，不包含知识正文；单次最多200条，预检不会写入系统。</p>
        <div v-if="evaluationImportPreview" class="evaluation-import-summary">
          <el-tag>共 {{ evaluationImportPreview.rowCount }} 行</el-tag>
          <el-tag type="success">可导入 {{ evaluationImportPreview.validCount }} 行</el-tag>
          <el-tag type="info">跳过 {{ evaluationImportPreview.skippedCount }} 行</el-tag>
          <el-tag :type="evaluationImportStatusType">
            {{ evaluationImportStatusText }}
          </el-tag>
        </div>
        <el-table v-if="evaluationImportPreview" :data="evaluationImportPreview.rows" size="small" max-height="260">
          <el-table-column prop="rowNumber" label="行" width="58" />
          <el-table-column prop="name" label="样本" min-width="140" show-overflow-tooltip />
          <el-table-column prop="question" label="问题" min-width="220" show-overflow-tooltip />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="importRowTagType(row.status)">{{ importRowStatus(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="message" label="校验结果" min-width="190" show-overflow-tooltip />
        </el-table>
      </section>

      <el-form v-if="evaluationEditing" ref="evaluationFormRef" :model="evaluationForm"
        :rules="evaluationRules" label-position="top" class="evaluation-form">
        <div class="evaluation-form-grid">
          <el-form-item label="样本名称" prop="name">
            <el-input v-model="evaluationForm.name" maxlength="120" />
          </el-form-item>
          <el-form-item label="是否启用">
            <el-switch v-model="evaluationForm.enabled" active-text="纳入一键评价" />
          </el-form-item>
        </div>
        <el-form-item label="评价问题" prop="question">
          <el-input v-model="evaluationForm.question" type="textarea" :rows="2" maxlength="1000" show-word-limit />
        </el-form-item>
        <div class="evaluation-form-grid two-columns">
          <el-form-item label="预期命中文档" prop="expectedArticleIds">
            <el-select v-model="evaluationForm.expectedArticleIds" multiple filterable class="full-width"
              placeholder="至少选择一篇文档">
              <el-option v-for="article in articleOptions" :key="article.id"
                :label="formatArticleOption(article)" :value="article.id" :disabled="!article.ragIndexable" />
            </el-select>
          </el-form-item>
          <el-form-item label="禁止命中文档">
            <el-select v-model="evaluationForm.forbiddenArticleIds" multiple filterable class="full-width"
              placeholder="用于验证隐私或失效内容不被检索">
              <el-option v-for="article in articleOptions" :key="article.id"
                :label="formatArticleOption(article)" :value="article.id" />
            </el-select>
          </el-form-item>
        </div>
        <div class="form-actions">
          <el-button @click="cancelEvaluationEdit">取消</el-button>
          <el-button type="primary" :loading="evaluationSaving" @click="submitEvaluation">保存样本</el-button>
        </div>
      </el-form>

      <div v-if="evaluationSummary" class="evaluation-summary">
        <div><strong>{{ evaluationSummary.total }}</strong><span>启用样本</span></div>
        <div><strong>{{ evaluationSummary.top3HitRate }}%</strong><span>Top-3 命中率</span></div>
        <div><strong>{{ evaluationSummary.forbiddenHitCount }}</strong><span>越界命中</span></div>
        <div><strong>{{ evaluationSummary.passed }}</strong><span>本轮通过</span></div>
      </div>

      <div class="evaluation-toolbar">
        <span>评价样本 {{ evaluationCases.length }} 项</span>
        <el-button type="success" :icon="VideoPlay" :loading="evaluationRunning" @click="runEvaluation">
          运行启用样本
        </el-button>
      </div>
      <el-table :data="evaluationCases" size="small" empty-text="尚未建立评价样本">
        <el-table-column prop="name" label="样本" min-width="150" />
        <el-table-column prop="question" label="问题" min-width="260" show-overflow-tooltip />
        <el-table-column label="预期文档" min-width="150">
          <template #default="{ row }">{{ formatArticleIds(row.expectedArticleIds) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="editEvaluation(row)">修改</el-button>
            <el-button link type="danger" @click="removeEvaluation(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <h4 v-if="evaluationRuns.length" class="run-heading">最近运行记录</h4>
      <el-table v-if="evaluationRuns.length" :data="evaluationRuns.slice(0, 20)" size="small">
        <el-table-column prop="caseName" label="样本" min-width="150" />
        <el-table-column label="Top-3" width="90">
          <template #default="{ row }"><el-tag :type="row.top3Hit ? 'success' : 'danger'">{{ row.top3Hit ? '命中' : '未命中' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="越界" width="90">
          <template #default="{ row }"><el-tag :type="row.forbiddenHit ? 'danger' : 'success'">{{ row.forbiddenHit ? '命中' : '无' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="searchMethod" label="检索方式" width="100" />
        <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
        <el-table-column label="结果" width="80">
          <template #default="{ row }"><el-tag :type="row.passed ? 'success' : 'danger'">{{ row.passed ? '通过' : '失败' }}</el-tag></template>
        </el-table-column>
      </el-table>
    </section>

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
        <span>{{ answerMode === 'LLM' ? 'AI回答' : '知识检索结果' }}</span>
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
              <p v-if="source.issuingAuthority"><strong>发布机关：</strong>{{ source.issuingAuthority }}</p>
              <p v-if="source.documentNumber"><strong>文号：</strong>{{ source.documentNumber }}</p>
              <p v-if="source.validityStatus"><strong>有效状态：</strong>{{ formatValidityStatus(source.validityStatus) }}</p>
              <p><strong>索引：</strong>{{ formatIndexStatus(source.indexStatus) }}</p>
              <p><strong>摘要：</strong>{{ source.summary }}</p>
              <div v-if="source.excerpt" class="source-excerpt">
                <strong>命中片段：</strong>
                <p>{{ source.excerpt }}</p>
              </div>
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
        <el-tag v-if="searchMethod" type="info">
          {{ searchMethod === 'VECTOR' ? '语义检索' : '关键词检索' }}
        </el-tag>
        <el-tag v-if="answerMode === 'RETRIEVAL_ONLY'" type="warning">
          本次为原文检索
        </el-tag>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, DataAnalysis, Download, Plus, Upload, VideoPlay } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import {
  askAI, deleteRagEvaluationCase, downloadRagEvaluationTemplate, getRagEvaluationCandidates,
  getRagEvaluationCases, getRagEvaluationRuns, importRagEvaluationWorkbook,
  runRagEvaluationSuite, saveRagEvaluationCase, searchKnowledge
} from '@/api/knowledge'
import { getAvailableAiProviders } from '@/api/ai'
import { useUserStore } from '@/stores'

const question = ref('')
const answer = ref('')
const sources = ref([])
const hasAnswer = ref(false)
const documentCount = ref(0)
const loading = ref(false)
const activeSources = ref(['0', '1', '2'])
const searchMethod = ref('')
const answerMode = ref('')
const providerType = ref('LM_STUDIO')
const availableProviders = ref([])
const providerLoading = ref(false)
const userStore = useUserStore()
const canManageKnowledge = computed(() => userStore.hasPermission('KNOWLEDGE_MANAGE'))
const evaluationPanelVisible = ref(false)
const evaluationEditing = ref(false)
const evaluationSaving = ref(false)
const evaluationRunning = ref(false)
const evaluationFormRef = ref(null)
const evaluationUploadRef = ref(null)
const evaluationCases = ref([])
const evaluationRuns = ref([])
const evaluationSummary = ref(null)
const articleOptions = ref([])
const evaluationWorkbook = ref(null)
const evaluationImportPreview = ref(null)
const evaluationImporting = ref(false)
const templateDownloading = ref(false)
const evaluationForm = ref(emptyEvaluationForm())
const evaluationRules = {
  name: [{ required: true, message: '请输入样本名称', trigger: 'blur' }],
  question: [{ required: true, message: '请输入评价问题', trigger: 'blur' }],
  expectedArticleIds: [{ type: 'array', required: true, min: 1, message: '至少选择一篇预期文档', trigger: 'change' }]
}

const selectedProvider = computed(() => availableProviders.value.find(item => item.providerType === providerType.value))
const evaluationImportStatusText = computed(() => {
  if (evaluationImportPreview.value?.importedCount > 0) return '已完成导入'
  return evaluationImportPreview.value?.canImport ? '允许确认导入' : '请先修正错误行'
})
const evaluationImportStatusType = computed(() =>
  evaluationImportPreview.value?.importedCount > 0 || evaluationImportPreview.value?.canImport ? 'success' : 'danger'
)

const loadProviders = async () => {
  providerLoading.value = true
  try {
    const response = await getAvailableAiProviders()
    availableProviders.value = response.data || []
    const local = availableProviders.value.find(item => item.providerType === 'LM_STUDIO' && item.available)
    providerType.value = local?.providerType || availableProviders.value.find(item => item.available)?.providerType || 'LM_STUDIO'
  } finally {
    providerLoading.value = false
  }
}

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
  searchMethod.value = ''
  answerMode.value = ''

  try {
    if (selectedProvider.value && !selectedProvider.value.local) {
      await ElMessageBox.confirm(
        '本次问题将发送至外部模型服务。请确认内容不含案件材料、客户隐私或未脱敏信息。',
        '使用云端模型',
        { type: 'warning', confirmButtonText: '确认发送', cancelButtonText: '取消' }
      )
    }
    const aiResponse = await askAI(question.value, { topK: 5, providerType: providerType.value })
    const result = aiResponse.data || {}
    answer.value = result.answer || '暂未生成回答。'
    hasAnswer.value = Boolean(result.hasAnswer)
    documentCount.value = result.documentCount || result.sources?.length || 0
    sources.value = result.sources || []
    searchMethod.value = result.searchMethod || ''
    answerMode.value = result.answerMode || 'RETRIEVAL_ONLY'

  } catch (error) {
    console.warn('RAG接口不可用，降级为普通知识库检索', error)
    await fallbackKeywordSearch()
  } finally {
    loading.value = false
  }
}

onMounted(loadProviders)

function emptyEvaluationForm() {
  return { id: null, name: '', question: '', expectedArticleIds: [], forbiddenArticleIds: [], enabled: true }
}

const toggleEvaluationPanel = async () => {
  evaluationPanelVisible.value = !evaluationPanelVisible.value
  if (evaluationPanelVisible.value) {
    await Promise.all([loadEvaluationData(), loadArticleOptions()])
  }
}

const loadArticleOptions = async () => {
  const response = await getRagEvaluationCandidates()
  articleOptions.value = response.data || []
}

const loadEvaluationData = async () => {
  const [casesResponse, runsResponse] = await Promise.all([
    getRagEvaluationCases(), getRagEvaluationRuns()
  ])
  evaluationCases.value = casesResponse.data || []
  evaluationRuns.value = runsResponse.data || []
}

const startCreateEvaluation = () => {
  evaluationForm.value = emptyEvaluationForm()
  evaluationEditing.value = true
}

const editEvaluation = (row) => {
  evaluationForm.value = {
    id: row.id,
    name: row.name,
    question: row.question,
    expectedArticleIds: [...(row.expectedArticleIds || [])],
    forbiddenArticleIds: [...(row.forbiddenArticleIds || [])],
    enabled: row.enabled !== false
  }
  evaluationEditing.value = true
}

const cancelEvaluationEdit = () => {
  evaluationEditing.value = false
  evaluationForm.value = emptyEvaluationForm()
}

const submitEvaluation = async () => {
  await evaluationFormRef.value?.validate()
  evaluationSaving.value = true
  try {
    const payload = { ...evaluationForm.value }
    delete payload.id
    await saveRagEvaluationCase(payload, evaluationForm.value.id)
    ElMessage.success('评价样本已保存')
    cancelEvaluationEdit()
    await loadEvaluationData()
  } finally {
    evaluationSaving.value = false
  }
}

const removeEvaluation = async (row) => {
  await ElMessageBox.confirm(`确定删除评价样本“${row.name}”吗？历史运行记录仍会保留。`, '删除样本', { type: 'warning' })
  await deleteRagEvaluationCase(row.id)
  ElMessage.success('评价样本已删除')
  await loadEvaluationData()
}

const runEvaluation = async () => {
  evaluationRunning.value = true
  try {
    const response = await runRagEvaluationSuite()
    evaluationSummary.value = response.data
    ElMessage.success(`评价完成：${response.data?.passed || 0}/${response.data?.total || 0} 项通过`)
    await loadEvaluationData()
  } finally {
    evaluationRunning.value = false
  }
}

const downloadEvaluationTemplate = async () => {
  templateDownloading.value = true
  try {
    const response = await downloadRagEvaluationTemplate()
    const url = URL.createObjectURL(response.data)
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = 'RAG评价样本模板.xlsx'
    anchor.click()
    URL.revokeObjectURL(url)
  } finally {
    templateDownloading.value = false
  }
}

const selectEvaluationWorkbook = (uploadFile) => {
  evaluationWorkbook.value = uploadFile.raw
  evaluationImportPreview.value = null
}

const clearEvaluationWorkbook = () => {
  evaluationWorkbook.value = null
  evaluationImportPreview.value = null
}

const previewEvaluationWorkbook = async () => {
  if (!evaluationWorkbook.value) return
  evaluationImporting.value = true
  try {
    const response = await importRagEvaluationWorkbook(evaluationWorkbook.value, true)
    evaluationImportPreview.value = response.data
    if (!response.data?.canImport) ElMessage.warning('预检未通过，请修正错误行后重新选择文件')
  } finally {
    evaluationImporting.value = false
  }
}

const confirmEvaluationWorkbook = async () => {
  if (!evaluationWorkbook.value || !evaluationImportPreview.value?.canImport) return
  evaluationImporting.value = true
  try {
    const response = await importRagEvaluationWorkbook(evaluationWorkbook.value, false)
    evaluationWorkbook.value = null
    evaluationUploadRef.value?.clearFiles()
    evaluationImportPreview.value = { ...response.data, canImport: false }
    ElMessage.success(`已导入 ${response.data?.importedCount || 0} 条评价样本`)
    await loadEvaluationData()
  } finally {
    evaluationImporting.value = false
  }
}

const importRowTagType = (status) => ({ VALID: 'success', SKIPPED: 'info', ERROR: 'danger' }[status] || 'info')
const importRowStatus = (status) => ({ VALID: '通过', SKIPPED: '跳过', ERROR: '错误' }[status] || status)

const formatArticleIds = (ids = []) => ids.map(id => {
  const article = articleOptions.value.find(item => item.id === id)
  return article ? article.title : `#${id}`
}).join('、') || '-'

const formatArticleOption = (article) => `${article.title}（#${article.id}）${article.ragIndexable ? '' : ' · 禁止进入RAG'}`

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

  answer.value = generateRetrievalAnswer(relevantDocs)
  hasAnswer.value = true
  documentCount.value = relevantDocs.length
  sources.value = relevantDocs.slice(0, 3).map(normalizeSource)
  searchMethod.value = 'KEYWORD'
  answerMode.value = 'RETRIEVAL_ONLY'
}

const normalizeSource = (doc) => ({
  id: doc.id,
  title: doc.title,
  category: doc.category,
  knowledgeSource: doc.knowledgeSource,
  indexStatus: doc.indexStatus,
  issuingAuthority: doc.issuingAuthority,
  documentNumber: doc.documentNumber,
  validityStatus: doc.validityStatus,
  summary: doc.summary || (doc.content ? `${doc.content.substring(0, 100)}...` : ''),
  excerpt: doc.excerpt || ''
})

const generateRetrievalAnswer = (docs) => {
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

const formatValidityStatus = (status) => {
  const map = {
    EFFECTIVE: '现行有效',
    AMENDED: '已修订',
    REPEALED: '已废止',
    UNKNOWN: '待核验'
  }
  return map[status] || '待核验'
}
</script>

<style scoped lang="scss">
.rag-search-page {
  max-width: 1120px;
  margin: 0 auto;

  .scope-alert {
    margin-bottom: 14px;
  }

  .header-actions,
  .evaluation-toolbar,
  .form-actions {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    gap: 10px;
  }

  .provider-select {
    width: 290px;
  }

  .evaluation-panel {
    margin-bottom: 18px;
    padding: 16px;
    border: 1px solid #dfe5ec;
    border-radius: 8px;
    background: #fff;
  }

  .evaluation-heading {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;

    h3 { margin: 0 0 6px; font-size: 16px; }
    p { margin: 0; color: #6b7280; font-size: 13px; }
  }

  .evaluation-heading-actions,
  .evaluation-import-controls,
  .evaluation-import-summary {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
  }

  .evaluation-import {
    margin: 16px 0;
    padding: 14px;
    border: 1px solid #e5e7eb;
    background: #f8fafc;

    p { margin: 8px 0; color: #6b7280; font-size: 12px; }
    .evaluation-import-summary { margin: 10px 0; }
  }

  .evaluation-form {
    margin: 16px 0;
    padding: 14px;
    border: 1px solid #e5e7eb;
    background: #f8fafc;
  }

  .evaluation-form-grid {
    display: grid;
    grid-template-columns: minmax(0, 1fr) 180px;
    gap: 14px;

    &.two-columns { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  }

  .full-width { width: 100%; }

  .evaluation-summary {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 10px;
    margin: 16px 0;

    div { padding: 12px; border: 1px solid #e5e7eb; border-radius: 6px; }
    strong { display: block; font-size: 20px; color: #111827; }
    span { font-size: 12px; color: #6b7280; }
  }

  .evaluation-toolbar { justify-content: space-between; margin: 14px 0 8px; }
  .run-heading { margin: 18px 0 8px; }

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

  .source-excerpt {
    margin: 10px 0;

    p {
      margin: 6px 0 0;
      padding: 10px;
      white-space: pre-line;
      line-height: 1.7;
      color: #303133;
      background: #f6f8fa;
      border-left: 3px solid #409eff;
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
    .header-actions { align-items: stretch; flex-direction: column; }
    .provider-select { width: min(290px, 100%); }
    .scope-grid {
      grid-template-columns: 1fr;
    }
    .evaluation-heading { flex-direction: column; }
    .evaluation-form-grid,
    .evaluation-form-grid.two-columns,
    .evaluation-summary { grid-template-columns: 1fr; }
  }
}
</style>
