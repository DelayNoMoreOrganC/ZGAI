<template>
  <section class="archive-workflow" v-loading="loading">
    <el-result v-if="error" icon="error" title="归档任务加载失败" :sub-title="error">
      <template #extra><el-button type="primary" @click="load">重新加载</el-button></template>
    </el-result>

    <template v-else>
      <div v-if="!job" class="archive-start">
        <el-empty description="该案件尚未发起智能归档" />
        <div v-if="readiness" class="readiness">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="案件状态">{{ statusLabel(readiness.caseStatus) }}</el-descriptions-item>
            <el-descriptions-item label="可用材料">{{ readiness.documentCount }} 份</el-descriptions-item>
          </el-descriptions>
          <el-alert v-if="readiness.warnings?.length" type="warning" :closable="false" :title="readiness.warnings.join('；')" />
          <el-alert v-if="readiness.missingCritical?.length" type="warning" :closable="false" title="关键材料待补充或由行政例外复核">
            <template #default>{{ readiness.missingCritical.join('、') }}</template>
          </el-alert>
        </div>
        <el-button v-if="readiness?.canStart && readiness.caseStatus !== 'ARCHIVED'" data-testid="archive-start" type="primary" :loading="creating" @click="startArchive()">
          <el-icon><FolderChecked /></el-icon>开始智能归档
        </el-button>
        <el-button v-if="readiness?.canStart && readiness.caseStatus === 'ARCHIVED'" type="primary" @click="openCorrection">
          发起更正归档
        </el-button>
      </div>

      <template v-else>
        <header class="workflow-header">
          <div>
            <h3>{{ job.caseName }}</h3>
            <p>{{ job.caseNumber || '暂未生成案件编号' }} · {{ job.templateVersion }}</p>
          </div>
          <div class="header-actions">
            <el-tag data-testid="archive-status" :type="statusType(job.status)">{{ statusLabel(job.status) }}</el-tag>
            <el-button v-if="job.status === 'COMPLETED' && readiness?.canStart && readiness.caseStatus === 'ARCHIVED'" @click="openCorrection">更正归档</el-button>
            <el-button v-if="canPreview" :loading="previewing" @click="preview">预览卷宗</el-button>
            <el-button v-if="job.canDownload" data-testid="archive-download" type="primary" @click="download">下载电子卷宗</el-button>
          </div>
        </header>

        <el-progress :percentage="job.progress || 0" :status="job.status === 'FAILED' ? 'exception' : job.status === 'COMPLETED' ? 'success' : undefined" />
        <p class="stage-text">{{ job.currentStage }}</p>
        <el-alert v-if="job.errorMessage" type="error" :closable="false" :title="job.errorMessage" />
        <el-alert v-if="job.reviewReason && job.status === 'REJECTED'" type="warning" :closable="false" :title="`驳回理由：${job.reviewReason}`" />
        <el-alert v-if="job.correctionReason" type="info" :closable="false" :title="`更正原因：${job.correctionReason}`" />
        <el-alert v-if="job.missingCritical?.length" type="warning" :closable="false" title="关键材料缺失">
          <template #default>{{ job.missingCritical.join('、') }}</template>
        </el-alert>

        <el-tabs v-model="activePane" class="workflow-tabs">
          <el-tab-pane label="材料目录" name="documents">
            <el-table :data="job.documents" row-key="id" border>
              <el-table-column label="归档" width="72" align="center">
                <template #default="{ row }"><el-checkbox v-model="row.included" :disabled="!job.canEdit" /></template>
              </el-table-column>
              <el-table-column prop="originalFileName" label="材料名称" min-width="240" show-overflow-tooltip />
              <el-table-column label="卷内目录" min-width="260">
                <template #default="{ row }">
                  <el-select v-if="job.canEdit" v-model="row.catalogSeq" :disabled="!row.included" filterable style="width:100%">
                    <el-option v-for="item in catalogOptions" :key="item.value" :label="`${item.value}. ${item.label}`" :value="item.value" />
                  </el-select>
                  <span v-else>{{ row.catalogSeq ? `${row.catalogSeq}. ${row.catalogName}` : '待人工归类' }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="sourcePageCount" label="页数" width="72" align="center" />
              <el-table-column label="识别依据" min-width="190" show-overflow-tooltip>
                <template #default="{ row }">{{ row.classificationReason || '-' }}</template>
              </el-table-column>
            </el-table>
            <div v-if="job.canEdit" class="supplement-panel">
              <el-upload
                v-model:file-list="supplementFiles"
                drag
                action="#"
                :auto-upload="false"
                :limit="1"
                :on-change="handleSupplementChange"
                :on-remove="clearSupplement"
              >
                <el-icon class="upload-icon"><UploadFilled /></el-icon>
                <div>拖入归档补充材料，或点击选择文件</div>
              </el-upload>
              <el-select v-model="supplementCatalogSeq" placeholder="选择卷内目录" filterable>
                <el-option v-for="item in catalogOptions" :key="item.value" :label="`${item.value}. ${item.label}`" :value="item.value" />
              </el-select>
              <el-button type="primary" :loading="uploading" :disabled="!supplementFile || !supplementCatalogSeq" @click="uploadSupplement">
                上传并加入目录
              </el-button>
            </div>
            <div v-if="job.canEdit" class="pane-actions"><el-button type="primary" @click="saveDocuments">保存材料目录</el-button></div>
          </el-tab-pane>

          <el-tab-pane label="归档表格" name="fields">
            <el-form label-position="top" class="field-grid">
              <el-form-item v-for="field in job.fields" :key="field.key" :label="field.key">
                <el-input v-model="field.value" :type="longField(field.key) ? 'textarea' : 'text'" :rows="longField(field.key) ? 4 : undefined" :disabled="!job.canEdit" />
                <small>{{ field.extractionReason || '待律师核对' }}<template v-if="field.sourcePage"> · 第{{ field.sourcePage }}页</template></small>
              </el-form-item>
            </el-form>
            <div v-if="job.canEdit" class="pane-actions"><el-button type="primary" @click="saveFields">保存表格字段</el-button></div>
          </el-tab-pane>

          <el-tab-pane v-if="job.output" label="归档结果" name="output">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="文件名">{{ job.output.fileName }}</el-descriptions-item>
              <el-descriptions-item label="版本">v{{ job.output.versionNo }}</el-descriptions-item>
              <el-descriptions-item label="总页数">{{ job.output.pageCount }}</el-descriptions-item>
              <el-descriptions-item label="源材料页数">{{ job.output.sourcePageCount }}</el-descriptions-item>
              <el-descriptions-item label="缺页/重复">{{ job.output.gapPages }} / {{ job.output.duplicatePages }}</el-descriptions-item>
              <el-descriptions-item label="SHA-256"><code>{{ job.output.contentSha256 }}</code></el-descriptions-item>
              <el-descriptions-item label="来源清单校验"><code>{{ job.output.manifestSha256 }}</code></el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>
        </el-tabs>

        <footer class="workflow-footer">
          <el-button v-if="job.canSubmit" data-testid="archive-submit" type="primary" @click="submit">提交行政复核</el-button>
          <template v-if="job.canReview">
            <el-button data-testid="archive-reject" type="danger" plain @click="openReview('REJECT')">驳回</el-button>
            <el-button data-testid="archive-approve" type="success" @click="openReview('APPROVE')">批准并生成电子卷宗</el-button>
          </template>
        </footer>
      </template>
    </template>

    <el-dialog v-model="reviewVisible" :title="reviewForm.decision === 'APPROVE' ? '行政归档复核' : '驳回归档'" width="520px">
      <el-form label-position="top">
        <el-form-item label="复核意见"><el-input v-model="reviewForm.reason" data-testid="archive-review-reason" type="textarea" :rows="3" /></el-form-item>
        <el-form-item v-if="reviewForm.decision === 'APPROVE' && job?.missingCritical?.length" label="缺件例外理由" required>
          <el-input v-model="reviewForm.exceptionReason" type="textarea" :rows="4" placeholder="说明允许缺件归档的依据和后续处理方式" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewVisible = false">取消</el-button>
        <el-button data-testid="archive-review-confirm" :type="reviewForm.decision === 'APPROVE' ? 'success' : 'danger'" :loading="reviewing" @click="review">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="correctionVisible" title="发起更正归档" width="520px">
      <el-form label-position="top">
        <el-form-item label="更正原因" required>
          <el-input v-model="correctionReason" type="textarea" :rows="4" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="correctionVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="startCorrection">创建新版本任务</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FolderChecked, UploadFilled } from '@element-plus/icons-vue'
import {
  createArchiveJob, downloadArchiveOutput, getArchiveJob, getArchiveJobs, getArchiveReadiness,
  patchArchiveDocuments, patchArchiveFields, previewArchiveOutput, reviewArchiveJob, submitArchiveJob,
  uploadArchiveSupplement
} from '@/api/archive'

const props = defineProps({ caseId: { type: [String, Number], default: null }, jobId: { type: [String, Number], default: null } })
const emit = defineEmits(['completed', 'changed'])
const loading = ref(false)
const creating = ref(false)
const reviewing = ref(false)
const previewing = ref(false)
const uploading = ref(false)
const error = ref('')
const readiness = ref(null)
const job = ref(null)
const activePane = ref('documents')
const reviewVisible = ref(false)
const reviewForm = ref({ decision: 'APPROVE', reason: '', exceptionReason: '' })
const correctionVisible = ref(false)
const correctionReason = ref('')
const supplementFiles = ref([])
const supplementFile = ref(null)
const supplementCatalogSeq = ref(null)
const catalogOptions = [
  [2, '发票回执等收费凭证'], [3, '委托代理合同'], [4, '授权委托书'], [5, '起诉状、上诉状或答辩状'],
  [6, '阅卷笔录、会见当事人谈话笔录'], [7, '证据材料'], [8, '诉讼保全、证据保全及相关裁判文书'],
  [9, '承办律师代理意见'], [10, '集体讨论记录'], [11, '代理词或辩护词'], [12, '出庭通知书或传票'],
  [13, '庭审笔录'], [14, '裁定书、判决书、调解书'], [15, '执行申请书及执行文书']
].map(([value, label]) => ({ value, label }))
const canPreview = computed(() => ['LAWYER_REVIEW', 'ADMIN_REVIEW', 'REJECTED', 'COMPLETED'].includes(job.value?.status))

const load = async () => {
  loading.value = true
  error.value = ''
  try {
    if (props.jobId) {
      job.value = (await getArchiveJob(props.jobId)).data
    } else if (props.caseId) {
      readiness.value = (await getArchiveReadiness(props.caseId)).data
      const jobs = (await getArchiveJobs({ caseId: props.caseId })).data || []
      job.value = jobs[0] || null
    }
  } catch (e) {
    error.value = e?.message || '无法读取归档任务'
  } finally {
    loading.value = false
  }
}

const startArchive = async (reason = '') => {
  creating.value = true
  try {
    job.value = (await createArchiveJob(props.caseId, { idempotencyKey: crypto.randomUUID(), correctionReason: reason || undefined })).data
    ElMessage.success(job.value.status === 'FAILED' ? '任务已建立，请检查归档引擎状态' : '归档分析已完成，请核对材料')
    emit('changed')
  } finally { creating.value = false }
}

const openCorrection = () => {
  correctionReason.value = ''
  correctionVisible.value = true
}
const startCorrection = async () => {
  if (!correctionReason.value.trim()) return ElMessage.warning('请填写归档更正原因')
  await startArchive(correctionReason.value.trim())
  correctionVisible.value = false
}

const saveDocuments = async () => {
  const items = job.value.documents.map(item => ({
    id: item.id, included: item.included, catalogSeq: item.catalogSeq,
    catalogName: catalogOptions.find(option => option.value === item.catalogSeq)?.label || item.catalogName
  }))
  job.value = (await patchArchiveDocuments(job.value.id, items)).data
  ElMessage.success('材料目录已保存')
}

const saveFields = async () => {
  const fields = Object.fromEntries(job.value.fields.map(field => [field.key, field.value || '']))
  job.value = (await patchArchiveFields(job.value.id, fields)).data
  ElMessage.success('归档表格字段已保存')
}

const handleSupplementChange = uploadFile => { supplementFile.value = uploadFile.raw || null }
const clearSupplement = () => { supplementFile.value = null }
const uploadSupplement = async () => {
  if (!supplementFile.value || !supplementCatalogSeq.value) return
  uploading.value = true
  try {
    job.value = (await uploadArchiveSupplement(job.value.id, supplementFile.value, supplementCatalogSeq.value)).data
    supplementFiles.value = []
    supplementFile.value = null
    supplementCatalogSeq.value = null
    ElMessage.success('补充材料已加入归档目录')
  } finally { uploading.value = false }
}

const submit = async () => {
  await ElMessageBox.confirm('提交后材料和字段将交由行政人员复核。', '提交归档复核', { type: 'warning' })
  await saveDocuments()
  await saveFields()
  job.value = (await submitArchiveJob(job.value.id)).data
  ElMessage.success('已提交行政复核')
  emit('changed')
}

const openReview = decision => {
  reviewForm.value = { decision, reason: '', exceptionReason: '' }
  reviewVisible.value = true
}

const review = async () => {
  if (reviewForm.value.decision === 'REJECT' && !reviewForm.value.reason.trim()) return ElMessage.warning('请填写驳回理由')
  if (reviewForm.value.decision === 'APPROVE' && job.value.missingCritical?.length && !reviewForm.value.exceptionReason.trim()) return ElMessage.warning('请填写缺件例外理由')
  reviewing.value = true
  try {
    job.value = (await reviewArchiveJob(job.value.id, reviewForm.value)).data
    reviewVisible.value = false
    if (job.value.status === 'COMPLETED') {
      activePane.value = 'output'
      ElMessage.success('电子卷宗已生成，案件已归档锁定')
      emit('completed')
    } else if (job.value.status === 'FAILED') {
      ElMessage.error(job.value.errorMessage || '电子卷宗生成失败，案件仍保持已结案')
    } else {
      ElMessage.success('归档任务已驳回')
    }
    emit('changed')
  } finally { reviewing.value = false }
}

const download = async () => {
  const response = await downloadArchiveOutput(job.value.id)
  const url = URL.createObjectURL(response.data)
  const link = document.createElement('a')
  link.href = url
  link.download = job.value.output?.fileName || '电子卷宗.pdf'
  link.click()
  URL.revokeObjectURL(url)
}

const preview = async () => {
  const tab = window.open('', '_blank')
  previewing.value = true
  try {
    const response = await previewArchiveOutput(job.value.id)
    const url = URL.createObjectURL(response.data)
    if (tab) tab.location.href = url
    else window.open(url, '_blank')
    window.setTimeout(() => URL.revokeObjectURL(url), 60000)
  } catch (e) {
    if (tab) tab.close()
    throw e
  } finally { previewing.value = false }
}

const longField = key => ['案情简介', '结案小结', '承办律师意见', '主任审批意见'].includes(key)
const statusLabel = status => ({ PRECHECK: '材料预检查', OCR: '本地OCR', CLASSIFYING: '材料分类', EXTRACTING: '字段提取', LAWYER_REVIEW: '律师核对', ADMIN_REVIEW: '行政复核', ASSEMBLING: '生成卷宗', COMPLETED: '已归档', FAILED: '处理失败', REJECTED: '已驳回', CLOSED: '已结案', ARCHIVED: '已归档' }[status] || status || '-')
const statusType = status => ({ COMPLETED: 'success', FAILED: 'danger', REJECTED: 'warning', ADMIN_REVIEW: 'warning', LAWYER_REVIEW: 'primary' }[status] || 'info')

watch(() => [props.caseId, props.jobId], load)
onMounted(load)
</script>

<style scoped>
.archive-workflow { min-height: 320px; }
.archive-start { display: flex; flex-direction: column; align-items: center; gap: 18px; padding: 30px; }
.readiness { width: min(760px, 100%); display: grid; gap: 12px; }
.workflow-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 20px; margin-bottom: 18px; }
.workflow-header h3 { margin: 0 0 6px; font-size: 18px; }
.workflow-header p, .stage-text { margin: 0; color: #6b7280; font-size: 13px; }
.header-actions { display: flex; align-items: center; gap: 10px; }
.stage-text { margin: 8px 0 16px; }
.workflow-tabs { margin-top: 18px; }
.pane-actions, .workflow-footer { display: flex; justify-content: flex-end; gap: 10px; padding-top: 16px; }
.supplement-panel { display: grid; grid-template-columns: minmax(260px, 1fr) 280px auto; align-items: center; gap: 12px; margin-top: 16px; padding: 14px; border: 1px solid #dfe3e8; border-radius: 8px; background: #f8f9fb; }
.supplement-panel :deep(.el-upload-dragger) { padding: 14px; border-radius: 6px; background: #fff; }
.upload-icon { margin-right: 6px; vertical-align: middle; }
.workflow-footer { border-top: 1px solid #e5e7eb; margin-top: 20px; }
.field-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 18px; }
.field-grid small { display: block; margin-top: 5px; color: #8a919c; }
code { font-size: 11px; overflow-wrap: anywhere; }
@media (max-width: 760px) { .workflow-header { flex-direction: column; } .field-grid, .supplement-panel { grid-template-columns: 1fr; } }
</style>
