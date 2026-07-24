<template>
  <div class="ai-workbench" data-testid="ai-case-workbench">
    <header class="page-header" data-testid="ai-workbench-header">
      <div>
        <h1>案件 AI 助手</h1>
        <p>{{ selectedCaseLabel }}</p>
      </div>
      <div class="header-selects">
        <el-tag type="info" effect="plain">规则引擎 / 本地识别</el-tag>
        <el-select
          data-testid="ai-command-case"
          v-model="selectedCaseId" filterable remote clearable :remote-method="loadCases"
          :loading="caseLoading" placeholder="可选：手动指定案件" class="case-select"
        >
          <el-option
            v-for="item in caseOptions"
            :key="item.id"
            :label="formatCase(item)"
            :value="item.id"
            :disabled="item.canEdit !== true"
          />
        </el-select>
      </div>
    </header>

    <el-alert
      v-if="readOnlyMessage"
      data-testid="ai-command-readonly-alert"
      :title="readOnlyMessage"
      type="warning"
      show-icon
      :closable="false"
      class="permission-alert"
    />

    <el-tabs v-model="activeTab" class="work-tabs">
      <el-tab-pane label="案件操作" name="command">
        <section class="command-layout">
          <div class="command-panel">
            <el-input
              data-testid="ai-command-instruction"
              v-model="instruction"
              type="textarea"
              :rows="6"
              maxlength="4000"
              show-word-limit
              resize="none"
              placeholder="直接说明案件名称、案号、当事人或委托客户及要执行的事项，系统会自动识别案件"
              :disabled="commandLoading || !canOperateCommand"
            />
            <div class="command-actions">
              <div class="examples">
                <el-button text @click="useExample('8月10日9:30开庭，地点三号法庭')">开庭日程</el-button>
                <el-button text @click="useExample('明天下午3点开庭，地点第二审判庭')">相对日期</el-button>
                <el-button text @click="useExample('记录进展：今日已向法院提交补充证据')">案件进展</el-button>
              </div>
              <el-button
                data-testid="ai-command-submit"
                type="primary"
                :loading="commandLoading"
                :disabled="!canSubmit"
                @click="submitCommand"
              >
                <el-icon><Promotion /></el-icon>
                执行
              </el-button>
            </div>
          </div>

          <div class="result-panel" data-testid="ai-command-result" aria-live="polite">
            <el-empty v-if="!commandResult" description="暂无操作结果" :image-size="72" />
            <template v-else>
              <div class="result-heading">
                <el-tag :type="statusType(commandResult.status)" effect="plain">
                  {{ statusLabel(commandResult.status) }}
                </el-tag>
                <span>{{ commandResult.caseName || selectedCaseLabel }}</span>
              </div>
              <p v-if="commandResult.clarification" data-testid="ai-command-clarification" class="clarification">
                {{ commandResult.clarification }}
              </p>
              <div v-if="commandCandidates.length" class="command-candidates" data-testid="ai-command-candidates">
                <button
                  v-for="candidate in commandCandidates"
                  :key="candidate.caseId"
                  type="button"
                  class="candidate-row"
                  :disabled="candidate.canEdit !== true || commandLoading"
                  @click="selectCandidateAndSubmit(candidate)"
                >
                  <span>
                    <strong>{{ candidate.caseNumber || '未编号' }} · {{ candidate.caseName || '未命名案件' }}</strong>
                    <small>{{ (candidate.reasons || []).join('、') }}</small>
                  </span>
                  <el-tag :type="candidate.canEdit === true ? 'primary' : 'info'" effect="plain">
                    {{ candidate.canEdit === true ? `${candidate.score}% · 确认执行` : '仅可查看' }}
                  </el-tag>
                </button>
              </div>
              <el-alert
                v-if="commandResult.status === 'PROPOSED' && proposedStage"
                data-testid="ai-stage-proposal"
                :title="`案件阶段尚未变更，确认后将进入「${proposedStage}」`"
                description="阶段变更会完成当前阶段、创建下一阶段待办并写入审计记录。"
                type="warning"
                show-icon
                :closable="false"
                class="stage-proposal"
              />
              <div
                v-for="(action, index) in commandResult.actions"
                :key="`${action.actionType}-${index}`"
                :data-testid="`ai-command-action-${action.actionType}`"
                class="action-row"
              >
                <el-icon><CircleCheck v-if="!action.requiresConfirmation" /><Warning v-else /></el-icon>
                <div>
                  <strong>{{ actionLabel(action.actionType) }}</strong>
                  <span v-if="actionSummary(action)" class="action-payload">{{ actionSummary(action) }}</span>
                  <span>{{ action.reason }}</span>
                </div>
              </div>
              <el-button
                v-if="commandResult.status === 'PROPOSED'"
                data-testid="ai-command-confirm"
                type="warning"
                :loading="confirmingCommand"
                @click="confirmCommand"
              >
                <el-icon><Select /></el-icon>
                {{ proposedStage ? '确认变更案件阶段' : '确认执行' }}
              </el-button>
              <div v-if="commandExecuted" class="result-links">
                <el-button v-if="hasWorkspaceAction" data-testid="ai-command-view-calendar" @click="router.push('/calendar')">
                  查看日程与待办
                </el-button>
                <el-button v-if="hasTimelineAction" data-testid="ai-command-view-timeline" @click="openCaseTimeline">
                  查看案件日志
                </el-button>
              </div>
            </template>
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="智能归案" name="intake">
        <section class="intake-layout" data-testid="ai-intake-panel">
          <el-upload
            data-testid="ai-intake-upload"
            drag
            action="#"
            :auto-upload="false"
            :limit="1"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :disabled="!hasCaseEditPermission"
            accept=".pdf,.docx,.txt,.md,.png,.jpg,.jpeg"
            class="intake-upload"
          >
            <el-icon class="upload-icon"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖入案件文书，或点击选择</div>
          </el-upload>
          <el-button data-testid="ai-intake-analyze" type="primary" :loading="intakeLoading" :disabled="!selectedFile || !hasCaseEditPermission" @click="analyzeFile">
            <el-icon><Search /></el-icon>
            识别文书
          </el-button>

          <div v-if="intakeResult" class="intake-result" data-testid="ai-intake-result">
            <el-alert
              v-if="intakeResult.status === 'FAILED'"
              :title="intakeResult.message || '文书识别失败'"
              type="error"
              show-icon
              :closable="false"
            />
            <template v-else>
              <div class="analysis-grid">
                <div><span>文书类型</span><strong>{{ intakeForm.documentType || '-' }}</strong></div>
                <div><span>法院案号</span><strong>{{ intakeResult.analysis?.courtCaseNumber || '-' }}</strong></div>
                <div><span>法院/仲裁机构</span><strong>{{ intakeResult.analysis?.courtName || '-' }}</strong></div>
                <div><span>识别时间</span><strong>{{ intakeResult.analysis?.hearingDate || intakeResult.analysis?.judgmentDate || '-' }}</strong></div>
                <div><span>识别地点</span><strong>{{ intakeResult.analysis?.hearingPlace || '-' }}</strong></div>
                <div><span>识别期限</span><strong>{{ intakeResult.analysis?.deadline || '-' }}</strong></div>
              </div>

              <el-form label-position="top" class="confirm-form">
                <el-form-item label="归属案件">
                  <el-select data-testid="ai-intake-case" v-model="intakeForm.caseId" filterable class="full-width">
                    <el-option
                      v-for="candidate in candidateOptions"
                      :key="candidate.caseId"
                      :label="candidateLabel(candidate)"
                      :value="candidate.caseId"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="案件目录">
                  <el-select data-testid="ai-intake-folder" v-model="intakeForm.folderPath" class="full-width">
                    <el-option v-for="folder in folders" :key="folder" :label="folder" :value="folder" />
                  </el-select>
                </el-form-item>
                <el-form-item label="文书类型">
                  <el-input data-testid="ai-intake-document-type" v-model="intakeForm.documentType" />
                </el-form-item>
                <el-form-item>
                  <el-checkbox v-model="intakeForm.registerActivity">同步登记案件进展</el-checkbox>
                </el-form-item>
                <div v-if="hasHearingSuggestion" class="linked-action">
                  <el-checkbox v-model="intakeForm.createHearingCalendar">确认创建开庭日程</el-checkbox>
                  <div v-if="intakeForm.createHearingCalendar" class="linked-action-fields">
                    <el-form-item label="开庭时间">
                      <el-date-picker
                        v-model="intakeForm.hearingTime"
                        type="datetime"
                        value-format="YYYY-MM-DDTHH:mm:ss"
                        format="YYYY-MM-DD HH:mm"
                        class="full-width"
                      />
                    </el-form-item>
                    <el-form-item label="开庭地点">
                      <el-input v-model="intakeForm.hearingLocation" maxlength="200" />
                    </el-form-item>
                  </div>
                </div>
                <div v-if="hasDeadlineSuggestion" class="linked-action">
                  <el-checkbox v-model="intakeForm.createDeadlineTodo">确认创建期限待办</el-checkbox>
                  <div v-if="intakeForm.createDeadlineTodo" class="linked-action-fields">
                    <el-form-item label="期限时间">
                      <el-date-picker
                        v-model="intakeForm.deadlineTime"
                        type="datetime"
                        value-format="YYYY-MM-DDTHH:mm:ss"
                        format="YYYY-MM-DD HH:mm"
                        class="full-width"
                      />
                    </el-form-item>
                    <el-form-item label="待办标题">
                      <el-input v-model="intakeForm.deadlineTitle" maxlength="200" />
                    </el-form-item>
                  </div>
                </div>
              </el-form>
              <el-button data-testid="ai-intake-confirm" type="primary" :loading="filing" :disabled="!canFile" @click="confirmFiling">
                <el-icon><FolderChecked /></el-icon>
                确认归案
              </el-button>
            </template>
          </div>
        </section>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, FolderChecked, Promotion, Search, Select, UploadFilled, Warning } from '@element-plus/icons-vue'
import { getCaseList } from '@/api/case'
import { useUserStore } from '@/stores'
import {
  confirmCaseCommand,
  confirmDocumentIntake,
  createDocumentIntake,
  submitCaseCommand
} from '@/api/ai'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const activeTab = ref(route.query.tab === 'intake' ? 'intake' : 'command')
const selectedCaseId = ref(route.query.caseId ? Number(route.query.caseId) : null)
const caseOptions = ref([])
const caseLoading = ref(false)
const instruction = ref('')
const commandLoading = ref(false)
const confirmingCommand = ref(false)
const commandResult = ref(null)
const selectedFile = ref(null)
const intakeLoading = ref(false)
const filing = ref(false)
const intakeResult = ref(null)
const folders = ['01_立案材料', '02_证据材料', '03_法律文书', '04_合同收费', '05_往来函件', '99_归档材料']
const intakeForm = reactive({
  caseId: null,
  folderPath: '',
  documentType: '',
  registerActivity: true,
  createHearingCalendar: false,
  hearingTime: '',
  hearingLocation: '',
  createDeadlineTodo: false,
  deadlineTime: '',
  deadlineTitle: ''
})

const selectedCaseLabel = computed(() => {
  const item = caseOptions.value.find(caseItem => caseItem.id === selectedCaseId.value)
  return item ? formatCase(item) : '无需预选案件，AI 将在您的权限范围内识别归属'
})
const selectedCase = computed(() => caseOptions.value.find(caseItem => caseItem.id === selectedCaseId.value))
const hasCaseEditPermission = computed(() => userStore.hasPermission('CASE_EDIT'))
const canOperateCase = computed(() => hasCaseEditPermission.value && selectedCase.value?.canEdit === true)
const canOperateCommand = computed(() => hasCaseEditPermission.value
  && (!selectedCaseId.value || canOperateCase.value))
const readOnlyMessage = computed(() => {
  if (!hasCaseEditPermission.value) return '当前账号仅可查看案件，AI 日程、待办、进展和智能归案需要案件编辑权限。'
  if (selectedCaseId.value && !canOperateCase.value) return '当前案件仅可查看，不能通过 AI 写入案件数据。'
  return ''
})
const canSubmit = computed(() => Boolean(canOperateCommand.value && instruction.value.trim()))
const commandCandidates = computed(() => commandResult.value?.candidates || [])
const commandExecuted = computed(() => ['AUTO_EXECUTED', 'CONFIRMED'].includes(commandResult.value?.status))
const proposedStage = computed(() => commandResult.value?.actions
  ?.find(action => action.actionType === 'CHANGE_STAGE')?.payload?.targetStage || '')
const hasWorkspaceAction = computed(() => commandResult.value?.actions?.some(action =>
  ['CREATE_CALENDAR', 'CREATE_TODO'].includes(action.actionType)
))
const hasTimelineAction = computed(() => commandResult.value?.actions?.some(action =>
  ['CREATE_CALENDAR', 'ADD_ACTIVITY', 'CHANGE_STAGE'].includes(action.actionType)
))
const hasHearingSuggestion = computed(() => Boolean(
  intakeResult.value?.analysis?.hearingDate || intakeResult.value?.analysis?.hearingPlace
))
const hasDeadlineSuggestion = computed(() => Boolean(intakeResult.value?.analysis?.deadline))
const canFile = computed(() => {
  const hearingReady = !intakeForm.createHearingCalendar
    || (intakeForm.hearingTime && intakeForm.hearingLocation.trim())
  const deadlineReady = !intakeForm.createDeadlineTodo
    || (intakeForm.deadlineTime && intakeForm.deadlineTitle.trim())
  return intakeResult.value?.status === 'ANALYZED'
    && intakeForm.caseId && intakeForm.folderPath && intakeForm.documentType.trim()
    && hearingReady && deadlineReady
})
const candidateOptions = computed(() => {
  const candidates = [...(intakeResult.value?.candidates || [])]
  caseOptions.value.forEach(item => {
    if (!candidates.some(candidate => candidate.caseId === item.id)) {
      candidates.push({ caseId: item.id, caseName: item.caseName, caseNumber: item.caseNumber, score: 0, reasons: ['手动选择'] })
    }
  })
  return candidates
})

const loadCases = async (keyword = '') => {
  caseLoading.value = true
  try {
    const response = await getCaseList({ page: 1, size: 100, keyword: keyword || undefined })
    caseOptions.value = response.data?.records || []
  } finally {
    caseLoading.value = false
  }
}

const formatCase = item => `${item.caseNumber || '未编号'} · ${item.caseName || '未命名案件'}`
const useExample = value => {
  instruction.value = `${selectedCaseId.value ? '本案' : '【案件名称或案号】'}${value}`
}
const newIdempotencyKey = () => globalThis.crypto?.randomUUID?.()
  || `${Date.now()}-${Math.random().toString(16).slice(2)}`

const submitCommand = async (caseIdOverride) => {
  commandLoading.value = true
  try {
    const response = await submitCaseCommand({
      caseId: typeof caseIdOverride === 'number' ? caseIdOverride : selectedCaseId.value,
      instruction: instruction.value.trim(),
      idempotencyKey: newIdempotencyKey()
    })
    commandResult.value = response.data
    if (response.data?.status === 'AUTO_EXECUTED') ElMessage.success('案件操作已执行并留痕')
  } finally {
    commandLoading.value = false
  }
}

const selectCandidateAndSubmit = candidate => {
  if (candidate.canEdit !== true) return
  selectedCaseId.value = candidate.caseId
  if (!caseOptions.value.some(item => item.id === candidate.caseId)) {
    caseOptions.value.push({
      id: candidate.caseId,
      caseName: candidate.caseName,
      caseNumber: candidate.caseNumber,
      canEdit: true
    })
  }
  submitCommand(candidate.caseId)
}

const confirmCommand = async () => {
  if (proposedStage.value) {
    try {
      await ElMessageBox.confirm(
        `确认将当前案件推进到「${proposedStage.value}」阶段吗？`,
        '再次确认案件阶段变更',
        {
          confirmButtonText: '确认变更',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
    } catch {
      return
    }
  }
  confirmingCommand.value = true
  try {
    const response = await confirmCaseCommand(commandResult.value.commandId)
    commandResult.value = response.data
    await loadCases()
    ElMessage.success('已确认执行')
  } finally {
    confirmingCommand.value = false
  }
}

const statusLabel = status => ({
  NEEDS_CLARIFICATION: '需要补充', AUTO_EXECUTED: '已执行', PROPOSED: '等待确认',
  CONFIRMED: '已确认', FAILED: '失败'
}[status] || status)
const statusType = status => ({ AUTO_EXECUTED: 'success', CONFIRMED: 'success', PROPOSED: 'warning', FAILED: 'danger' }[status] || 'info')
const actionLabel = type => ({ CREATE_CALENDAR: '建立日程', CREATE_TODO: '建立待办', ADD_ACTIVITY: '登记进展', CHANGE_STAGE: '变更案件阶段' }[type] || type)
const formatActionTime = value => value ? value.replace('T', ' ').slice(0, 16) : ''
const actionSummary = action => {
  const payload = action?.payload || {}
  if (action.actionType === 'CREATE_CALENDAR') {
    return [formatActionTime(payload.startTime), payload.location].filter(Boolean).join(' · ')
  }
  if (action.actionType === 'CREATE_TODO') {
    return [payload.title, formatActionTime(payload.dueTime)].filter(Boolean).join(' · ')
  }
  if (action.actionType === 'ADD_ACTIVITY') return payload.content || ''
  if (action.actionType === 'CHANGE_STAGE') return payload.targetStage || ''
  return ''
}
const openCaseTimeline = () => {
  const caseId = commandResult.value?.caseId || selectedCaseId.value
  if (caseId) router.push(`/case/${caseId}/timeline`)
}

const resetLinkedActions = () => {
  intakeForm.createHearingCalendar = false
  intakeForm.hearingTime = ''
  intakeForm.hearingLocation = ''
  intakeForm.createDeadlineTodo = false
  intakeForm.deadlineTime = ''
  intakeForm.deadlineTitle = ''
}
const normalizeExtractedDateTime = value => {
  if (typeof value !== 'string') return ''
  const match = value.trim().match(/^(\d{4}-\d{2}-\d{2})[T\s](\d{2}:\d{2})(?::\d{2})?/)
  return match ? `${match[1]}T${match[2]}:00` : ''
}
const handleFileChange = file => {
  selectedFile.value = file.raw
  intakeResult.value = null
  resetLinkedActions()
}
const handleFileRemove = () => {
  selectedFile.value = null
  intakeResult.value = null
  resetLinkedActions()
}
const analyzeFile = async () => {
  intakeLoading.value = true
  try {
    const response = await createDocumentIntake(selectedFile.value)
    intakeResult.value = response.data
    intakeForm.caseId = response.data?.candidates?.[0]?.caseId || selectedCaseId.value
    intakeForm.folderPath = response.data?.suggestedFolder || '05_往来函件'
    intakeForm.documentType = response.data?.suggestedDocumentType || '其他'
    resetLinkedActions()
    intakeForm.hearingTime = normalizeExtractedDateTime(response.data?.analysis?.hearingDate)
    intakeForm.hearingLocation = response.data?.analysis?.hearingPlace || ''
    intakeForm.deadlineTime = normalizeExtractedDateTime(response.data?.analysis?.deadline)
    intakeForm.deadlineTitle = response.data?.analysis?.deadline ? `${intakeForm.documentType}期限` : ''
    if (response.data?.status === 'ANALYZED') ElMessage.success('文书识别完成，请核对后归案')
  } finally {
    intakeLoading.value = false
  }
}

const candidateLabel = candidate => `${candidate.caseNumber || '未编号'} · ${candidate.caseName || '未命名'} · 匹配 ${candidate.score || 0}%`
const confirmFiling = async () => {
  filing.value = true
  try {
    const response = await confirmDocumentIntake(intakeResult.value.id, { ...intakeForm })
    intakeResult.value = response.data
    ElMessage.success(response.data?.message || '文件已归入案件')
  } finally {
    filing.value = false
  }
}

onMounted(async () => {
  await loadCases()
  if (selectedCaseId.value && !caseOptions.value.some(item => item.id === selectedCaseId.value)) {
    selectedCaseId.value = null
  }
})
</script>

<style scoped lang="scss">
.ai-workbench { width: 100%; max-width: 1180px; min-width: 0; margin: 0 auto; }
.page-header { display: flex; align-items: center; justify-content: space-between; gap: 24px; margin-bottom: 16px; }
.page-header h1 { margin: 0 0 6px; font-size: 24px; letter-spacing: 0; color: #1d1d1f; }
.permission-alert { margin-bottom: 16px; }
.result-links { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 16px; }
.stage-proposal { margin-bottom: 12px; }
.page-header p { margin: 0; color: #6e6e73; font-size: 14px; }
.case-select { width: min(420px, 44vw); }
.header-selects { display: flex; gap: 10px; align-items: center; }
.work-tabs { min-width: 0; background: #fff; border: 1px solid #e5e5e7; border-radius: 8px; padding: 0 24px 24px; }
.command-layout { display: grid; grid-template-columns: minmax(0, 1.15fr) minmax(320px, .85fr); gap: 24px; min-height: 430px; }
.command-panel, .result-panel { min-width: 0; padding-top: 18px; }
.result-panel { border-left: 1px solid #e5e5e7; padding-left: 24px; }
.command-actions { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-top: 14px; }
.examples { display: flex; flex-wrap: wrap; }
.result-heading { display: flex; align-items: center; gap: 12px; margin-bottom: 20px; font-weight: 600; }
.clarification { padding: 12px; margin: 0 0 16px; background: #f5f5f7; border-left: 3px solid #86868b; line-height: 1.6; }
.command-candidates { display: grid; gap: 8px; margin: 0 0 18px; }
.candidate-row { width: 100%; min-height: 64px; display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 10px 12px; border: 1px solid #d2d2d7; border-radius: 8px; background: #fff; color: #1d1d1f; text-align: left; cursor: pointer; }
.candidate-row:hover:not(:disabled) { border-color: #409eff; background: #f7faff; }
.candidate-row:disabled { cursor: not-allowed; opacity: .64; }
.candidate-row > span { min-width: 0; display: flex; flex-direction: column; gap: 5px; }
.candidate-row strong, .candidate-row small { overflow-wrap: anywhere; }
.candidate-row small { color: #6e6e73; }
.action-row { display: flex; gap: 10px; padding: 12px 0; border-bottom: 1px solid #ededee; }
.action-row div { display: flex; flex-direction: column; gap: 4px; }
.action-row span { color: #6e6e73; font-size: 13px; line-height: 1.5; }
.action-row .action-payload { color: #1d1d1f; }
.intake-layout { max-width: 800px; padding-top: 18px; }
.intake-upload { margin-bottom: 12px; }
.upload-icon { font-size: 40px; color: #5470c6; margin-bottom: 8px; }
.intake-result { margin-top: 24px; border-top: 1px solid #e5e5e7; padding-top: 20px; }
.analysis-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1px; background: #e5e5e7; border: 1px solid #e5e5e7; margin-bottom: 20px; }
.analysis-grid div { background: #fff; padding: 14px; min-width: 0; }
.analysis-grid span, .analysis-grid strong { display: block; overflow-wrap: anywhere; }
.analysis-grid span { color: #6e6e73; font-size: 12px; margin-bottom: 5px; }
.confirm-form { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 18px; }
.linked-action { grid-column: 1 / -1; padding: 14px 0; border-top: 1px solid #ededee; }
.linked-action-fields { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 18px; margin-top: 12px; }
.full-width { width: 100%; }
@media (max-width: 800px) {
  .work-tabs { padding: 0 12px 16px; }
  .page-header { align-items: stretch; flex-direction: column; }
  .case-select { width: 100%; }
  .header-selects { flex-direction: column; align-items: stretch; }
  .command-layout { grid-template-columns: 1fr; }
  .command-actions { align-items: stretch; flex-direction: column; }
  .command-actions > .el-button { width: 100%; margin-left: 0; }
  .examples { display: grid; grid-template-columns: 1fr; }
  .examples .el-button { justify-content: flex-start; width: 100%; margin-left: 0; white-space: normal; }
  .result-panel { border-left: 0; border-top: 1px solid #e5e5e7; padding-left: 0; }
  .candidate-row { align-items: stretch; flex-direction: column; }
  .candidate-row .el-tag { align-self: flex-start; }
  .analysis-grid, .confirm-form, .linked-action-fields { grid-template-columns: 1fr; }
  :deep(.intake-upload .el-upload),
  :deep(.intake-upload .el-upload-dragger) { width: 100%; }
}
</style>
