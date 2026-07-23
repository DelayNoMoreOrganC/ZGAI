<template>
  <div class="yuandian-page">
    <PageHeader title="元典法律检索">
      <template #extra>
        <el-button v-if="canConfigure" @click="router.push('/settings?tab=ai')">
          <el-icon><Setting /></el-icon>
          数据源设置
        </el-button>
      </template>
    </PageHeader>

    <section class="provider-bar">
      <div>
        <div class="provider-title">
          <span class="status-dot" :class="{ ready: isReady }"></span>
          元典法律数据服务
          <el-tag :type="isReady ? 'success' : 'warning'" effect="plain" size="small">
            {{ isReady ? '已就绪' : '待配置' }}
          </el-tag>
        </div>
        <p>{{ statusMessage }}</p>
      </div>
      <span class="billing-note">检索按元典账户积分计费</span>
    </section>

    <el-alert
      v-if="statusLoaded && !isReady"
      title="尚未配置元典 API Key"
      description="浏览器登录不能直接授权 ZGAI 后端。请由管理员在系统设置的 AI 配置中填写元典 API Key。"
      type="warning"
      show-icon
      :closable="false"
      class="setup-alert"
    />

    <el-tabs v-model="activeTab" class="search-tabs">
      <el-tab-pane label="法律法规" name="laws">
        <SearchPanel
          v-model="lawQuery"
          placeholder="例如：建设工程价款优先受偿权的适用条件"
          button-text="检索法规"
          :loading="searching"
          :disabled="!isReady"
          @search="runSearch('laws')"
        >
          <el-checkbox v-model="onlyEffective">仅检索现行有效法规</el-checkbox>
        </SearchPanel>
      </el-tab-pane>

      <el-tab-pane label="案例检索" name="cases">
        <SearchPanel
          v-model="caseQuery"
          placeholder="例如：股权转让中隐名股东资格确认的裁判规则"
          button-text="检索案例"
          :loading="searching"
          :disabled="!isReady"
          @search="runSearch('cases')"
        >
          <el-checkbox v-model="onlyAuthoritative">仅检索典型案例</el-checkbox>
        </SearchPanel>
      </el-tab-pane>

      <el-tab-pane label="引证核验" name="citations">
        <div class="citation-panel">
          <el-input
            v-model="citationText"
            type="textarea"
            :rows="8"
            maxlength="20000"
            show-word-limit
            placeholder="粘贴需要核验法律法规、法条或案号引用的文本"
            :disabled="!isReady"
          />
          <div class="citation-actions">
            <span>核验通常需要约 15 秒</span>
            <el-button type="primary" :loading="verifying" :disabled="!isReady" @click="verifyCitations">
              <el-icon><CircleCheck /></el-icon>
              开始核验
            </el-button>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <section v-if="activeTab !== 'citations'" class="result-section">
      <div v-if="hasSearched" class="result-heading">
        <span>检索结果</span>
        <span>{{ results.length }} 条</span>
      </div>
      <el-empty v-if="hasSearched && !searching && results.length === 0" description="没有找到相关结果" />
      <article v-for="item in results" :key="item.importToken" class="result-item">
        <div class="result-main">
          <div class="result-title-row">
            <h3>{{ item.title || '未命名资料' }}</h3>
            <el-tag v-if="item.validityStatus === 'EFFECTIVE'" type="success" effect="plain" size="small">现行有效</el-tag>
            <el-tag v-else-if="item.resultType === 'CASE'" type="info" effect="plain" size="small">案例参考</el-tag>
          </div>
          <div class="result-meta">
            <span v-if="item.referenceNo">{{ item.referenceNo }}</span>
            <span v-if="item.authority">{{ item.authority }}</span>
            <span v-if="item.date">{{ item.date }}</span>
            <span v-if="item.score != null">相关度 {{ formatScore(item.score) }}</span>
          </div>
          <p class="result-content">{{ item.content }}</p>
          <div v-if="item.tags" class="tag-row">
            <el-tag v-for="tag in splitTags(item.tags)" :key="tag" effect="plain" size="small">{{ tag }}</el-tag>
          </div>
        </div>
        <div class="result-actions">
          <el-tooltip :content="item.resultType === 'LAW' ? '导入后可进入法规知识库' : '案例仅作为参考资料，不进入共享 RAG'">
            <el-button
              :type="importedIds[item.importToken] ? 'success' : 'primary'"
              plain
              :loading="importingToken === item.importToken"
              :disabled="Boolean(importedIds[item.importToken])"
              @click="importResult(item)"
            >
              <el-icon><Download /></el-icon>
              {{ importedIds[item.importToken] ? '已导入' : '导入知识库' }}
            </el-button>
          </el-tooltip>
          <el-button v-if="importedIds[item.importToken]" link type="primary" @click="router.push(`/knowledge/${importedIds[item.importToken]}`)">
            查看资料
          </el-button>
        </div>
      </article>
    </section>

    <section v-if="activeTab === 'citations' && citationResult" class="citation-result">
      <div class="result-heading">
        <span>核验结果</span>
        <span>请求编号 {{ citationResult.request_id || '-' }}</span>
      </div>
      <div class="citation-summary">
        <div><strong>{{ citationResult.regulations?.length || 0 }}</strong><span>法规引用</span></div>
        <div><strong>{{ citationResult.cases?.length || 0 }}</strong><span>案例引用</span></div>
        <div><strong>{{ citationResult.chat_model || '-' }}</strong><span>核验模型</span></div>
      </div>
      <pre>{{ formattedCitationResult }}</pre>
    </section>
  </div>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores'
import { ElButton, ElMessage } from 'element-plus'
import { CircleCheck, Download, Search, Setting } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import {
  getYuandianStatus,
  importYuandianKnowledge,
  searchYuandianCases,
  searchYuandianLaws,
  verifyYuandianCitations
} from '@/api/yuandian'

const SearchPanel = defineComponent({
  props: {
    modelValue: { type: String, default: '' },
    placeholder: { type: String, default: '' },
    buttonText: { type: String, default: '检索' },
    loading: Boolean,
    disabled: Boolean
  },
  emits: ['update:modelValue', 'search'],
  setup(props, { emit, slots }) {
    return () => h('div', { class: 'query-panel' }, [
      h('div', { class: 'query-row' }, [
        h('input', {
          value: props.modelValue,
          disabled: props.disabled,
          placeholder: props.placeholder,
          class: 'native-query-input',
          onInput: event => emit('update:modelValue', event.target.value),
          onKeyup: event => event.key === 'Enter' && emit('search')
        }),
        h(ElButton, {
          type: 'primary',
          loading: props.loading,
          disabled: props.disabled,
          onClick: () => emit('search')
        }, { default: () => [h(Search), props.buttonText] })
      ]),
      h('div', { class: 'query-options' }, slots.default?.())
    ])
  }
})

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('laws')
const lawQuery = ref('')
const caseQuery = ref('')
const citationText = ref('')
const onlyEffective = ref(true)
const onlyAuthoritative = ref(true)
const status = ref(null)
const statusLoaded = ref(false)
const searching = ref(false)
const verifying = ref(false)
const hasSearched = ref(false)
const results = ref([])
const citationResult = ref(null)
const importingToken = ref('')
const importedIds = ref({})

const canConfigure = computed(() => userStore.hasPermission('SYSTEM_CONFIG'))
const isReady = computed(() => Boolean(status.value?.configured && status.value?.enabled))
const statusMessage = computed(() => {
  if (!statusLoaded.value) return '正在检查服务状态'
  if (!status.value?.enabled) return '服务已停用，请联系系统管理员'
  if (!status.value?.configured) return '需要配置 API Key 后使用'
  return '提供法规、案例语义检索和法律引证核验；AI 文本生成仍使用系统当前模型'
})
const formattedCitationResult = computed(() => JSON.stringify(citationResult.value, null, 2))

onMounted(loadStatus)

async function loadStatus() {
  try {
    const { data } = await getYuandianStatus()
    status.value = data
  } catch (error) {
    status.value = { configured: false, enabled: false }
  } finally {
    statusLoaded.value = true
  }
}

async function runSearch(type) {
  const query = (type === 'laws' ? lawQuery.value : caseQuery.value).trim()
  if (!query) {
    ElMessage.warning('请输入检索内容')
    return
  }
  searching.value = true
  hasSearched.value = true
  results.value = []
  try {
    const payload = { query, limit: 8 }
    const response = type === 'laws'
      ? await searchYuandianLaws({ ...payload, onlyEffective: onlyEffective.value })
      : await searchYuandianCases({ ...payload, onlyAuthoritative: onlyAuthoritative.value })
    results.value = response.data?.results || []
  } catch (error) {
    ElMessage.error(error.message || '元典检索失败')
  } finally {
    searching.value = false
  }
}

async function importResult(item) {
  importingToken.value = item.importToken
  try {
    const { data } = await importYuandianKnowledge(item.importToken)
    importedIds.value[item.importToken] = data.id
    ElMessage.success(item.resultType === 'LAW' ? '法规已导入知识库' : '案例已作为参考资料导入')
  } catch (error) {
    ElMessage.error(error.message || '导入失败')
  } finally {
    importingToken.value = ''
  }
}

async function verifyCitations() {
  if (!citationText.value.trim()) {
    ElMessage.warning('请粘贴需要核验的文本')
    return
  }
  verifying.value = true
  citationResult.value = null
  try {
    const { data } = await verifyYuandianCitations(citationText.value.trim())
    citationResult.value = data
  } catch (error) {
    ElMessage.error(error.message || '引证核验失败')
  } finally {
    verifying.value = false
  }
}

function splitTags(tags) {
  return tags.split(',').map(tag => tag.trim()).filter(Boolean).slice(0, 5)
}

function formatScore(score) {
  return `${Math.round(Number(score) * 100)}%`
}
</script>

<style scoped>
.yuandian-page {
  max-width: 1180px;
  margin: 0 auto;
}

.provider-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 18px 20px;
  border: 1px solid #dfe3e8;
  border-radius: 8px;
  background: #fff;
}

.provider-title {
  display: flex;
  align-items: center;
  gap: 9px;
  color: #20242a;
  font-size: 15px;
  font-weight: 650;
}

.provider-bar p,
.billing-note {
  margin: 6px 0 0;
  color: #68707a;
  font-size: 13px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #d99b2b;
}

.status-dot.ready {
  background: #25855a;
}

.setup-alert {
  margin-top: 14px;
}

.search-tabs {
  margin-top: 22px;
}

:deep(.query-panel) {
  padding: 18px 0 20px;
}

:deep(.query-row) {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px;
}

:deep(.native-query-input) {
  min-width: 0;
  height: 40px;
  padding: 0 13px;
  border: 1px solid #cfd5dc;
  border-radius: 6px;
  color: #20242a;
  font: inherit;
  outline: none;
}

:deep(.native-query-input:focus) {
  border-color: #3378b7;
  box-shadow: 0 0 0 2px rgba(51, 120, 183, 0.12);
}

:deep(.native-query-input:disabled) {
  background: #f4f5f6;
}

:deep(.query-options) {
  min-height: 24px;
  padding-top: 12px;
  color: #68707a;
}

.result-heading {
  display: flex;
  justify-content: space-between;
  padding: 13px 0;
  border-bottom: 1px solid #dfe3e8;
  color: #68707a;
  font-size: 13px;
}

.result-heading span:first-child {
  color: #20242a;
  font-size: 15px;
  font-weight: 650;
}

.result-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 132px;
  gap: 28px;
  padding: 22px 0;
  border-bottom: 1px solid #e7eaee;
}

.result-title-row,
.result-meta,
.tag-row,
.result-actions,
.citation-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.result-title-row h3 {
  margin: 0;
  color: #20242a;
  font-size: 16px;
  line-height: 1.5;
  letter-spacing: 0;
}

.result-meta {
  flex-wrap: wrap;
  margin-top: 7px;
  color: #737b85;
  font-size: 12px;
}

.result-meta span + span::before {
  content: '·';
  margin-right: 8px;
}

.result-content {
  display: -webkit-box;
  overflow: hidden;
  margin: 13px 0;
  color: #40464e;
  font-size: 14px;
  line-height: 1.75;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 5;
}

.tag-row {
  flex-wrap: wrap;
}

.result-actions {
  align-self: start;
  flex-direction: column;
}

.citation-panel {
  padding: 18px 0 24px;
}

.citation-actions {
  justify-content: space-between;
  margin-top: 12px;
  color: #737b85;
  font-size: 12px;
}

.citation-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border-bottom: 1px solid #e7eaee;
}

.citation-summary div {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 18px 0;
}

.citation-summary strong {
  color: #20242a;
  font-size: 18px;
}

.citation-summary span {
  color: #737b85;
  font-size: 12px;
}

.citation-result pre {
  overflow: auto;
  max-height: 520px;
  padding: 16px;
  border: 1px solid #dfe3e8;
  border-radius: 6px;
  background: #f7f8fa;
  color: #31363d;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
}

@media (max-width: 720px) {
  .provider-bar,
  .result-item {
    grid-template-columns: 1fr;
  }

  .provider-bar {
    align-items: flex-start;
    flex-direction: column;
  }

  :deep(.query-row) {
    grid-template-columns: 1fr;
  }

  .result-actions {
    align-items: flex-start;
  }

  .citation-summary {
    grid-template-columns: 1fr;
  }
}
</style>
