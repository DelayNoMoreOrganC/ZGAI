<template>
  <el-drawer
    v-model="visible"
    title="审批资料"
    size="min(560px, 100vw)"
    class="approval-detail-drawer"
  >
    <div v-loading="loading" class="approval-drawer-body">
      <el-empty v-if="!loading && approvalList.length === 0" description="暂无审批记录" />

      <div
        v-for="approval in approvalList"
        :key="approval.id"
        class="approval-card"
        :class="{ active: selectedApproval?.id === approval.id }"
        @click="selectedApproval = approval"
      >
        <div class="approval-card-main">
          <strong>{{ approval.title }}</strong>
          <span>{{ formatDateTime(approval.applyTime) }}</span>
        </div>
        <el-tag :type="getApprovalStatusType(approval.status)" size="small">
          {{ approval.statusDesc || approval.status }}
        </el-tag>
      </div>

      <div v-if="selectedApproval" class="approval-detail-panel">
        <div class="approval-detail-header">
          <el-tag :type="isCaseFilingApproval(selectedApproval) ? 'warning' : 'primary'">
            {{ formatApprovalType(selectedApproval.approvalType) }}
          </el-tag>
          <el-tag :type="getApprovalStatusType(selectedApproval.status)">
            {{ selectedApproval.statusDesc || selectedApproval.status }}
          </el-tag>
        </div>

        <el-descriptions :column="1" border>
          <el-descriptions-item label="审批标题">{{ selectedApproval.title || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ selectedApproval.applicantName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="当前审批人">{{ selectedApproval.currentApproverName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ formatDateTime(selectedApproval.applyTime) }}</el-descriptions-item>
          <el-descriptions-item label="关联案件">{{ selectedApproval.caseName || selectedApproval.caseId || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="approval-content">
          <h3>审批内容</h3>
          <pre>{{ selectedApproval.content || '-' }}</pre>
        </div>

        <div v-if="selectedApproval.approvalType === 'CASE_CLOSURE' && selectedApproval.closureRequest" class="closure-review-section">
          <h3>结案复核资料</h3>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="结案方式">{{ selectedApproval.closureRequest.closureTypeDesc }}</el-descriptions-item>
            <el-descriptions-item label="案件结果">{{ selectedApproval.closureRequest.caseOutcome }}</el-descriptions-item>
            <el-descriptions-item label="费用处理">{{ selectedApproval.closureRequest.feeStatusDesc }}</el-descriptions-item>
            <el-descriptions-item label="客户交付">{{ selectedApproval.closureRequest.clientDeliveryStatusDesc }}</el-descriptions-item>
            <el-descriptions-item v-if="selectedApproval.closureRequest.clientDeliveryNotes" label="交付说明">
              {{ selectedApproval.closureRequest.clientDeliveryNotes }}
            </el-descriptions-item>
            <el-descriptions-item label="材料核对">
              {{ selectedApproval.closureRequest.documentsConfirmed ? '申请人已确认' : '尚未确认' }}
            </el-descriptions-item>
          </el-descriptions>
          <div class="approval-content closure-summary">
            <h3>结案小结</h3>
            <pre>{{ selectedApproval.closureRequest.closureSummary }}</pre>
          </div>
          <div class="closure-documents">
            <h3>结案依据文件</h3>
            <div v-for="document in selectedApproval.closureRequest.basisDocuments || []" :key="document.documentId" class="closure-document-row">
              <span>{{ document.documentName }}</span>
              <div class="closure-document-actions">
                <el-tag size="small" effect="plain">{{ document.documentType || '案件材料' }}</el-tag>
                <el-button link type="primary" @click="downloadClosureDocument(selectedApproval, document)">下载核对</el-button>
              </div>
            </div>
            <el-alert
              v-if="!selectedApproval.closureRequest.basisDocuments?.length"
              type="error"
              :closable="false"
              title="未找到结案依据文件，请驳回申请。"
            />
          </div>
        </div>

        <div v-if="selectedApproval.approvalType === 'SEAL'" class="seal-files-section">
          <h3>待用印文件</h3>
          <el-descriptions v-if="selectedApproval.lawFirmLetter" :column="1" border class="letter-approval-meta">
            <el-descriptions-item label="文件类型">律所所函</el-descriptions-item>
            <el-descriptions-item label="所函编号">
              {{ selectedApproval.lawFirmLetterNumber || '审批通过后自动编号' }}
            </el-descriptions-item>
          </el-descriptions>
          <el-alert
            v-if="selectedApproval.lawFirmLetter && selectedApproval.letterSequenceRequiresInitialNumber"
            type="warning"
            :closable="false"
            title="该年度和函种尚未启用编号，同意用印时请设置首次流水号。"
          />
          <div
            v-for="attachment in selectedApproval.sealAttachments || []"
            :key="attachment.id"
            class="seal-file-row"
            :data-testid="`seal-attachment-${attachment.id}`"
          >
            <div>
              <strong>{{ attachment.originalFileName }}</strong>
              <span>
                {{ formatFileSize(attachment.fileSize) }} ·
                {{ attachment.sourceType === 'CASE_DOCUMENT' ? '案件文件' : attachment.sourceType === 'GENERATED_LETTER' ? '系统生成所函' : '申请人上传' }}
              </span>
            </div>
            <div class="seal-file-actions">
              <el-tag size="small" :type="getApprovalStatusType(attachment.sealStatus)">
                {{ formatSealStatus(attachment.sealStatus) }}
              </el-tag>
              <el-button link type="primary" @click="downloadSealFile(selectedApproval, attachment)">
                下载审阅
              </el-button>
            </div>
          </div>
          <el-alert
            v-if="!selectedApproval.sealAttachments?.length"
            type="error"
            :closable="false"
            title="该用印申请未找到关联文件，请勿审批并联系申请人重新提交。"
          />
        </div>

        <div v-if="selectedApproval.approvalNotes" class="approval-content">
          <h3>处理意见</h3>
          <pre>{{ selectedApproval.approvalNotes }}</pre>
        </div>

        <div v-if="isCaseFilingApproval(selectedApproval)" class="conflict-review-section">
          <div class="section-title-row">
            <h3>委托方利冲审查</h3>
            <span>{{ selectedApproval.conflictChecks?.length || 0 }} 个主体</span>
          </div>

          <el-alert
            v-if="!selectedApproval.conflictChecks?.length"
            type="info"
            :closable="false"
            title="该申请为存量流程，尚未关联结构化利冲记录，请在审批意见中完整记录审查结论。"
          />

          <div
            v-for="record in selectedApproval.conflictChecks || []"
            :key="record.id"
            class="conflict-record"
          >
            <div class="conflict-record-head">
              <div>
                <strong>{{ record.subjectName }}</strong>
                <span>{{ record.reportNo }}</span>
              </div>
              <div class="conflict-tags">
                <el-tag :type="riskTagType(record.conflictLevel)" size="small">
                  {{ riskLabel(record.conflictLevel) }}
                </el-tag>
                <el-tag :type="reviewDecisionType(record.reviewDecision)" size="small">
                  {{ record.reviewStatus === 'COMPLETED' ? reviewDecisionLabel(record.reviewDecision) : '待行政审查' }}
                </el-tag>
              </div>
            </div>
            <p class="conflict-summary">{{ record.conclusion || '系统未生成初筛说明' }}</p>
            <div v-if="record.reviewStatus === 'COMPLETED'" class="review-result">
              <span>{{ record.reviewedByName || '-' }} · {{ formatDateTime(record.reviewedAt) }}</span>
              <p>{{ record.reviewConclusion || '-' }}</p>
              <p v-if="record.waiverBasis"><b>豁免/处置依据：</b>{{ record.waiverBasis }}</p>
              <div v-if="record.waiverAttachments?.length" class="waiver-attachments">
                <b>书面依据原件</b>
                <button
                  v-for="attachment in record.waiverAttachments"
                  :key="attachment.id"
                  type="button"
                  class="attachment-link"
                  @click="downloadWaiver(record, attachment)"
                >
                  {{ attachment.originalFileName }} · {{ formatFileSize(attachment.fileSize) }}
                </button>
              </div>
              <el-tag v-if="record.archivedAt" type="success" size="small">已随案件归档</el-tag>
            </div>
            <el-button
              :data-testid="`conflict-review-${record.id}`"
              v-else-if="canReviewConflict"
              link
              type="primary"
              @click="startConflictReview(record)"
            >
              填写正式审查
            </el-button>

            <el-form v-if="conflictReviewForm.recordId === record.id" class="inline-review-form" label-position="top">
              <el-form-item label="正式结论" required>
                <el-radio-group v-model="conflictReviewForm.decision">
                  <el-radio-button value="PASSED">无冲突，通过</el-radio-button>
                  <el-radio-button value="REJECTED">存在冲突，不通过</el-radio-button>
                  <el-radio-button value="CONDITIONAL">附条件通过</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="审查意见" required>
                <el-input
                  data-testid="conflict-review-conclusion"
                  v-model="conflictReviewForm.conclusion"
                  type="textarea"
                  :rows="3"
                  maxlength="1000"
                  show-word-limit
                  placeholder="说明核查范围、既有委托关系及结论理由"
                />
              </el-form-item>
              <el-form-item
                v-if="conflictReviewForm.decision === 'CONDITIONAL'"
                label="书面依据原件"
                required
              >
                <div class="waiver-upload-block">
                  <div v-if="record.waiverAttachments?.length" class="waiver-attachments">
                    <button
                      v-for="attachment in record.waiverAttachments"
                      :key="attachment.id"
                      type="button"
                      class="attachment-link"
                      @click="downloadWaiver(record, attachment)"
                    >
                      {{ attachment.originalFileName }} · {{ formatFileSize(attachment.fileSize) }}
                    </button>
                  </div>
                  <el-upload
                    drag
                    :show-file-list="false"
                    :http-request="options => uploadWaiver(record, options)"
                    accept=".pdf,.doc,.docx,.jpg,.jpeg,.png"
                    :disabled="waiverUploading"
                  >
                    <el-icon><UploadFilled /></el-icon>
                    <div class="el-upload__text">拖入或点击上传 PDF、Word、JPG、PNG</div>
                    <template #tip><span>单个文件不超过 20MB，正式审查提交后不可追加或替换</span></template>
                  </el-upload>
                </div>
              </el-form-item>
              <el-form-item
                v-if="conflictReviewForm.decision === 'CONDITIONAL'"
                label="书面豁免或风险处置依据"
                required
              >
                <el-input
                  v-model="conflictReviewForm.waiverBasis"
                  type="textarea"
                  :rows="3"
                  maxlength="2000"
                  show-word-limit
                  placeholder="填写书面豁免、授权决定或信息隔离措施"
                />
              </el-form-item>
              <div class="inline-review-actions">
                <el-button @click="cancelConflictReview">取消</el-button>
                <el-button data-testid="conflict-review-submit" type="primary" :loading="reviewSubmitting" @click="submitConflictReview">
                  提交并锁定
                </el-button>
              </div>
            </el-form>
          </div>
        </div>

        <div v-if="isCaseFilingApproval(selectedApproval)" class="approval-path">
          <h3>立案审批路径</h3>
          <div class="path-step done">发起人提交立案申请</div>
          <div class="path-step active">行政管理利冲审查并审批</div>
          <div class="path-step">行政初审通过后进入主任终审</div>
          <div class="path-step">审批通过后建立案件档案</div>
        </div>

        <div class="approval-flow">
          <h3>审批记录</h3>
          <el-timeline v-if="approvalFlows.length > 0">
            <el-timeline-item
              v-for="flow in approvalFlows"
              :key="flow.id"
              :timestamp="formatDateTime(flow.actionTime)"
              :type="getFlowType(flow.action)"
              placement="top"
            >
              <strong>{{ flow.approverName || '未知人员' }} · {{ formatFlowAction(flow.action) }}</strong>
              <p v-if="flow.comments">{{ flow.comments }}</p>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无流转记录" :image-size="64" />
        </div>

        <div v-if="canHandleSelectedApproval" class="approval-actions">
          <el-button type="warning" @click="handleReject(selectedApproval)">
            {{ isCaseFilingApproval(selectedApproval) ? '驳回立案' : selectedApproval.approvalType === 'CASE_CLOSURE' ? '驳回结案' : '驳回' }}
          </el-button>
          <el-button data-testid="approval-approve" type="success" @click="handleApprove(selectedApproval)">
            {{ getApproveActionText(selectedApproval) }}
          </el-button>
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getApprovalList,
  getApprovalDetail,
  getApprovalFlow,
  approveApproval,
  rejectApproval,
  downloadApprovalAttachment
} from '@/api/approval'
import { reviewConflictCheckRecord, uploadConflictWaiverAttachment, downloadConflictWaiverAttachment } from '@/api/client'
import { downloadCaseDocument } from '@/api/case'
import { UploadFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  approvalId: {
    type: [Number, String],
    default: null
  },
  caseId: {
    type: [Number, String],
    default: null
  },
  initialApproval: {
    type: Object,
    default: null
  }
})

const emit = defineEmits(['update:modelValue', 'handled'])
const userStore = useUserStore()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const loading = ref(false)
const approvalList = ref([])
const selectedApproval = ref(null)
const approvalFlows = ref([])
const reviewSubmitting = ref(false)
const waiverUploading = ref(false)
const conflictReviewForm = ref({ recordId: null, decision: '', conclusion: '', waiverBasis: '' })

const currentUserId = computed(() => Number(userStore.userInfo?.id || userStore.userId || 0))
const isSuperAdmin = computed(() => userStore.userInfo?.username === 'admin')
const isDirector = computed(() => userStore.userInfo?.position === '主任')
const canHandleSelectedApproval = computed(() => {
  const approval = selectedApproval.value
  return approval?.status === 'PENDING'
    && (isSuperAdmin.value || isDirector.value || Number(approval.currentApproverId) === currentUserId.value)
})
const canReviewConflict = computed(() => canHandleSelectedApproval.value
  && selectedApproval.value?.approvalType === 'CASE_FILING'
  && (isSuperAdmin.value || userStore.hasPermission('CASE_FILING_REVIEW')))

const isSuccessResponse = (response) => response?.success || response?.code === 200
const isCaseFilingApproval = (row) => ['CASE_FILING', 'CASE_FILING_DIRECTOR'].includes(row?.approvalType)
const getApproveActionText = (row) => {
  if (row?.approvalType === 'CASE_FILING_DIRECTOR') return '终审通过'
  if (row?.approvalType === 'CASE_FILING') return '同意立案'
  if (row?.approvalType === 'SEAL') return '同意用印'
  if (row?.approvalType === 'CASE_CLOSURE') return '确认结案'
  return '同意'
}

const formatApprovalType = (type) => {
  const map = {
    CASE_FILING: '立案审批',
    CASE_FILING_DIRECTOR: '主任终审',
    SEAL: '公章用印审批',
    CASE_CLOSURE: '案件结案复核',
    REIMBURSEMENT: '费用报销',
    INVOICE: '开票申请',
    CONTRACT: '合同审批',
    OTHER: '其他审批'
  }
  return map[type] || type || '-'
}

const getApprovalStatusType = (status) => {
  const map = {
    PENDING: 'primary',
    APPROVED: 'success',
    REJECTED: 'danger',
    TRANSFERRED: 'warning',
    WITHDRAWN: 'info'
  }
  return map[status] || ''
}
const riskLabel = (level) => ({
  DIRECT: '直接冲突线索', CASE_PARTY: '案件主体命中', EXISTING: '既有客户命中',
  RELATED: '关联主体命中', SIMILAR: '相似名称', NONE: '未发现线索'
}[level] || '待人工核对')
const riskTagType = (level) => ({
  DIRECT: 'danger', CASE_PARTY: 'warning', EXISTING: 'warning', RELATED: 'warning', SIMILAR: 'warning', NONE: 'success'
}[level] || 'info')
const reviewDecisionLabel = (decision) => ({
  PASSED: '无冲突，通过', REJECTED: '存在冲突，不通过', CONDITIONAL: '附条件通过'
}[decision] || '待行政审查')
const reviewDecisionType = (decision) => ({
  PASSED: 'success', REJECTED: 'danger', CONDITIONAL: 'warning'
}[decision] || 'info')

const flowActionMap = {
  SUBMIT: '提交审批',
  APPROVE: '审批通过',
  REJECT: '驳回审批',
  TRANSFER: '转交审批',
  WITHDRAW: '撤回审批',
  URGE: '催办'
}

const formatFlowAction = (action) => flowActionMap[action] || action || '-'
const getFlowType = (action) => ({
  APPROVE: 'success',
  REJECT: 'danger',
  TRANSFER: 'warning',
  WITHDRAW: 'info',
  SUBMIT: 'primary'
}[action] || 'primary')
const formatDateTime = (value) => {
  if (!value) return '-'
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value
    const pad = number => String(number).padStart(2, '0')
    return `${year}-${pad(month)}-${pad(day)} ${pad(hour)}:${pad(minute)}:${pad(second)}`
  }
  return String(value).replace('T', ' ')
}
const formatFileSize = (value) => {
  const bytes = Number(value || 0)
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}
const formatSealStatus = (status) => ({
  PENDING: '待审批', APPROVED: '同意用印', REJECTED: '已驳回', WITHDRAWN: '已撤回'
}[status] || status || '-')

const downloadSealFile = async (approval, attachment) => {
  try {
    const response = await downloadApprovalAttachment(approval.id, attachment.id)
    const blob = response.data instanceof Blob ? response.data : new Blob([response.data])
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = attachment.originalFileName || '用印文件'
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error(error?.message || '下载用印文件失败')
  }
}

const downloadClosureDocument = async (approval, closureDocument) => {
  try {
    const response = await downloadCaseDocument(approval.caseId, closureDocument.documentId)
    const blob = response.data instanceof Blob ? response.data : new Blob([response.data])
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = closureDocument.documentName || '结案依据'
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error(error?.message || '下载结案依据失败')
  }
}

const uploadWaiver = async (record, options) => {
  try {
    waiverUploading.value = true
    const response = await uploadConflictWaiverAttachment(record.id, options.file)
    if (!isSuccessResponse(response)) throw new Error(response?.message || '上传失败')
    record.waiverAttachments = [...(record.waiverAttachments || []), response.data]
    options.onSuccess?.(response)
    ElMessage.success('书面依据原件已上传')
  } catch (error) {
    options.onError?.(error)
    ElMessage.error(error?.message || '书面依据上传失败')
  } finally {
    waiverUploading.value = false
  }
}

const downloadWaiver = async (record, attachment) => {
  try {
    const response = await downloadConflictWaiverAttachment(record.id, attachment.id)
    const blob = response.data instanceof Blob ? response.data : new Blob([response.data])
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = attachment.originalFileName || '利冲豁免依据'
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error(error?.message || '下载书面依据失败')
  }
}

const loadApprovalFlow = async (approvalId) => {
  if (!approvalId) {
    approvalFlows.value = []
    return
  }
  try {
    const response = await getApprovalFlow(approvalId)
    approvalFlows.value = response.data || []
  } catch (error) {
    console.error('获取审批记录失败:', error)
    approvalFlows.value = []
  }
}

const loadApprovals = async () => {
  if (!visible.value) return
  try {
    loading.value = true
    if (props.approvalId) {
      const response = await getApprovalDetail(props.approvalId)
      approvalList.value = response.data ? [response.data] : []
    } else if (props.caseId) {
      const response = await getApprovalList({ caseId: props.caseId, page: 1, size: 100 })
      approvalList.value = response.data?.records || []
    } else if (props.initialApproval) {
      approvalList.value = [props.initialApproval]
    } else {
      approvalList.value = []
    }
    selectedApproval.value = approvalList.value[0] || null
  } catch (error) {
    console.error('获取审批资料失败:', error)
    ElMessage.error('获取审批资料失败')
  } finally {
    loading.value = false
  }
}

const startConflictReview = (record) => {
  conflictReviewForm.value = { recordId: record.id, decision: '', conclusion: '', waiverBasis: '' }
}

const cancelConflictReview = () => {
  conflictReviewForm.value = { recordId: null, decision: '', conclusion: '', waiverBasis: '' }
}

const submitConflictReview = async () => {
  const form = conflictReviewForm.value
  if (!form.decision) {
    ElMessage.warning('请选择正式审查结论')
    return
  }
  if (!form.conclusion.trim()) {
    ElMessage.warning('请填写审查意见')
    return
  }
  if (form.decision === 'CONDITIONAL' && !form.waiverBasis.trim()) {
    ElMessage.warning('附条件通过必须填写书面豁免或风险处置依据')
    return
  }
  const record = selectedApproval.value?.conflictChecks?.find(item => item.id === form.recordId)
  if (form.decision === 'CONDITIONAL' && !record?.waiverAttachments?.length) {
    ElMessage.warning('附条件通过必须上传书面豁免或风险处置依据原件')
    return
  }
  try {
    reviewSubmitting.value = true
    const response = await reviewConflictCheckRecord(form.recordId, {
      decision: form.decision,
      conclusion: form.conclusion.trim(),
      waiverBasis: form.waiverBasis.trim()
    })
    if (!isSuccessResponse(response)) throw new Error(response?.message || '提交失败')
    ElMessage.success('正式利冲审查已完成，结论已锁定')
    cancelConflictReview()
    await loadApprovals()
  } catch (error) {
    ElMessage.error(error?.message || '正式利冲审查提交失败')
  } finally {
    reviewSubmitting.value = false
  }
}

const handleApprove = async (approval) => {
  if (approval.approvalType === 'CASE_FILING' && approval.conflictChecks?.some(item => item.reviewStatus !== 'COMPLETED')) {
    ElMessage.warning('请先完成全部委托方的正式利冲审查')
    return
  }
  if (approval.approvalType === 'CASE_FILING' && approval.conflictChecks?.some(item => item.reviewDecision === 'REJECTED')) {
    ElMessage.warning('存在利冲审查不通过结论，请驳回立案申请')
    return
  }
  if (approval.approvalType === 'CASE_FILING' && approval.conflictChecks?.some(item =>
    item.reviewDecision === 'CONDITIONAL' && !item.waiverAttachments?.length)) {
    ElMessage.warning('附条件通过的利冲审查缺少书面依据原件')
    return
  }
  try {
    let initialLetterSerial
    if (approval.lawFirmLetter && approval.letterSequenceRequiresInitialNumber) {
      const serialResult = await ElMessageBox.prompt(
        '这是该年度、该函种的首次编号。请输入本次所函使用的流水号，后续审批将自动递增。',
        '设置首次所函编号',
        {
          confirmButtonText: '确认流水号',
          cancelButtonText: '取消',
          inputPlaceholder: '例如：1',
          inputPattern: /^(?:[1-9]\d{0,5})$/,
          inputErrorMessage: '请输入1至999999之间的整数',
          inputType: 'number'
        }
      )
      initialLetterSerial = Number(serialResult.value)
    }
    const { value } = await ElMessageBox.prompt(
      isCaseFilingApproval(approval)
        ? '请填写利冲审查结论或立案审批意见'
        : approval.approvalType === 'SEAL'
          ? '请填写用印审批意见'
          : approval.approvalType === 'CASE_CLOSURE'
            ? '请填写案件结果、费用、客户交付和结案依据核对意见'
            : '请输入审批意见（可选）',
      getApproveActionText(approval),
      {
        confirmButtonText: getApproveActionText(approval),
        cancelButtonText: '取消',
        inputPlaceholder: '请输入审批意见',
        inputPattern: (isCaseFilingApproval(approval) || ['SEAL', 'CASE_CLOSURE'].includes(approval.approvalType)) ? /\S+/ : undefined,
        inputErrorMessage: isCaseFilingApproval(approval)
          ? '立案审批意见不能为空'
          : approval.approvalType === 'SEAL'
            ? '用印审批意见不能为空'
            : approval.approvalType === 'CASE_CLOSURE' ? '结案复核意见不能为空' : undefined,
        type: 'success'
      }
    )
    const response = await approveApproval(approval.id, { comments: value || '', initialLetterSerial })
    if (isSuccessResponse(response)) {
      ElMessage.success(isCaseFilingApproval(approval)
        ? '立案审批已通过'
        : approval.approvalType === 'SEAL'
          ? '已同意用印'
          : approval.approvalType === 'CASE_CLOSURE' ? '案件结案复核已通过' : '审批已同意')
      emit('handled')
      await loadApprovals()
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('同意审批失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

const handleReject = async (approval) => {
  try {
    const { value } = await ElMessageBox.prompt(
      isCaseFilingApproval(approval) ? '请输入驳回立案原因（必填）' : '请输入驳回原因（必填）',
      isCaseFilingApproval(approval) ? '驳回立案' : '驳回审批',
      {
        confirmButtonText: isCaseFilingApproval(approval) ? '驳回立案' : '确定驳回',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入驳回原因，系统会发送给申请人',
        inputPattern: /.+/,
        inputErrorMessage: '驳回原因不能为空',
        type: 'warning'
      }
    )
    const response = await rejectApproval(approval.id, { comments: value })
    if (isSuccessResponse(response)) {
      ElMessage.success(isCaseFilingApproval(approval) ? '立案申请已驳回' : '审批已驳回')
      emit('handled')
      await loadApprovals()
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('驳回审批失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

watch(
  () => [visible.value, props.approvalId, props.caseId, props.initialApproval?.id],
  () => {
    if (visible.value) loadApprovals()
  },
  { immediate: true }
)

watch(
  () => selectedApproval.value?.id,
  (approvalId) => loadApprovalFlow(approvalId),
  { immediate: true }
)
</script>

<style scoped lang="scss">
.approval-drawer-body {
  min-height: 240px;
}

.approval-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  padding: 12px 14px;
  margin-bottom: 10px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.2s, background-color 0.2s;

  &.active,
  &:hover {
    border-color: #409eff;
    background-color: #f5f9ff;
  }
}

.approval-card-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;

  strong {
    color: #1f2937;
    font-size: 14px;
    line-height: 1.4;
  }

  span {
    color: #909399;
    font-size: 12px;
  }
}

.approval-detail-panel {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid #ebeef5;
}

.approval-detail-header {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}

.approval-content,
.approval-path,
.approval-flow {
  margin-top: 18px;

  h3 {
    margin: 0 0 10px;
    color: #1f2937;
    font-size: 15px;
  }
}

.approval-flow {
  margin-top: 18px;

  :deep(.el-timeline) {
    padding-left: 4px;
  }

  p {
    margin: 6px 0 0;
    color: #606266;
    line-height: 1.6;
    white-space: pre-wrap;
  }
}

.approval-content pre {
  min-height: 96px;
  padding: 12px;
  margin: 0;
  border-radius: 8px;
  background-color: #f8fafc;
  color: #374151;
  font-family: inherit;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.seal-files-section {
  margin-top: 18px;

  h3 {
    margin: 0 0 10px;
    color: #1f2937;
    font-size: 15px;
  }
}

.seal-file-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 58px;
  padding: 10px 12px;
  margin-bottom: 8px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;

  strong,
  span {
    display: block;
    overflow-wrap: anywhere;
  }

  strong { color: #1f2937; }
  span { margin-top: 4px; color: #6b7280; font-size: 12px; }
}

.seal-file-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 0 0 auto;
}

.closure-review-section {
  margin-top: 18px;
}

.closure-summary {
  margin-top: 14px;
}

.closure-documents {
  margin-top: 14px;
}

.closure-document-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
}

.closure-document-row span {
  min-width: 0;
  overflow-wrap: anywhere;
}

.closure-document-actions {
  display: flex;
  align-items: center;
  flex: 0 0 auto;
  gap: 8px;
}

.conflict-review-section {
  margin-top: 18px;

  .section-title-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 10px;

    h3 {
      margin: 0;
      color: #1f2937;
      font-size: 15px;
    }

    span {
      color: #6b7280;
      font-size: 12px;
    }
  }
}

.conflict-record {
  padding: 14px;
  margin-top: 10px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.conflict-record-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;

  > div:first-child {
    display: flex;
    flex-direction: column;
    gap: 4px;
    min-width: 0;
  }

  strong {
    color: #1f2937;
    font-size: 14px;
    overflow-wrap: anywhere;
  }

  span {
    color: #909399;
    font-size: 12px;
  }
}

.conflict-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.conflict-summary,
.review-result p {
  margin: 10px 0 0;
  color: #4b5563;
  font-size: 13px;
  line-height: 1.65;
}

.review-result {
  padding-top: 10px;
  margin-top: 10px;
  border-top: 1px solid #f0f2f5;

  > span {
    color: #6b7280;
    font-size: 12px;
  }
}

.waiver-attachments {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
  margin-top: 10px;
}

.attachment-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: #2563eb;
  cursor: pointer;
  font: inherit;
  line-height: 1.5;
  text-align: left;
  overflow-wrap: anywhere;
}

.attachment-link:hover {
  text-decoration: underline;
}

.waiver-upload-block {
  width: 100%;

  :deep(.el-upload),
  :deep(.el-upload-dragger) {
    width: 100%;
  }

  :deep(.el-upload-dragger) {
    padding: 18px 12px;
    border-radius: 8px;
  }
}

.inline-review-form {
  padding-top: 14px;
  margin-top: 12px;
  border-top: 1px solid #e5e7eb;

  :deep(.el-radio-group) {
    display: flex;
    flex-wrap: wrap;
  }
}

.inline-review-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 640px) {
  .conflict-record-head {
    flex-direction: column;
  }

  .conflict-tags {
    justify-content: flex-start;
  }
}

.path-step {
  padding: 10px 12px;
  margin-bottom: 8px;
  border-radius: 8px;
  background-color: #f8fafc;
  color: #606266;

  &.done {
    color: #15803d;
    background-color: #f0fdf4;
  }

  &.active {
    color: #1d4ed8;
    background-color: #eff6ff;
  }
}

.approval-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}
</style>
