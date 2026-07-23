<template>
  <div class="conflict-check">
    <PageHeader title="利冲检查" :show-back="true" @back="$router.back()">
      <template #extra>
        <el-button @click="handleReset">重置</el-button>
        <el-button type="primary" :loading="checking" @click="handleCheck">开始检查</el-button>
      </template>
    </PageHeader>

    <div class="check-container">
      <section class="search-panel">
        <el-form ref="formRef" :model="formData" :rules="formRules" label-position="top">
          <el-form-item label="拟签约客户" prop="clientName">
            <el-input
              v-model="formData.clientName"
              size="large"
              placeholder="输入客户姓名或单位全称"
              maxlength="100"
              clearable
              @keyup.enter="handleCheck"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
          </el-form-item>
        </el-form>
        <p class="scope-note">系统将在全所客户库、案件当事人库和已登记关联主体图谱中核对；命中不会自动开放无权客户或案件详情。</p>
      </section>

      <section class="result-panel">
        <div class="section-header">
          <h3>核对结果</h3>
          <div v-if="checked" class="header-actions">
            <el-tag :type="summaryTagType">{{ summaryText }}</el-tag>
            <el-button v-if="previewResult?.recordId" :icon="Download" @click="downloadReport(previewResult)">
              留档报告
            </el-button>
          </div>
        </div>

        <el-empty v-if="!checked" description="输入客户名称后开始全库核对" />

        <template v-else>
          <el-alert
            class="summary-alert"
            :type="summaryAlertType"
            :title="summaryText"
            :description="summaryDescription"
            show-icon
            :closable="false"
          />

          <el-table v-if="hits.length" :data="hits" border class="result-table">
            <el-table-column label="来源" width="120">
              <template #default="{ row }">{{ sourceLabel(row.sourceType) }}</template>
            </el-table-column>
            <el-table-column prop="subjectName" label="命中主体" min-width="190" />
            <el-table-column label="案件角色" width="130">
              <template #default="{ row }">{{ partyRoleLabel(row.subjectRole) }}</template>
            </el-table-column>
            <el-table-column label="匹配方式" width="130">
              <template #default="{ row }">{{ matchLabel(row.matchType) }}</template>
            </el-table-column>
            <el-table-column label="关联案件" width="100" align="center">
              <template #default="{ row }">{{ row.relatedCaseCount ?? '-' }}</template>
            </el-table-column>
            <el-table-column label="核对依据" min-width="260">
              <template #default="{ row }">{{ row.reason || '-' }}</template>
            </el-table-column>
          </el-table>

          <div class="result-block">
            <h4>处理建议</h4>
            <p>{{ handlingAdvice }}</p>
          </div>
        </template>
      </section>

      <section v-if="historyRecords.length" class="history-panel">
        <div class="section-header">
          <h3>历史检查记录</h3>
          <span class="history-count">共 {{ historyRecords.length }} 次</span>
        </div>
        <el-table :data="historyRecords" border>
          <el-table-column prop="reportNo" label="报告编号" width="190" />
          <el-table-column prop="checkedAt" label="检查时间" width="180">
            <template #default="{ row }">{{ formatDateTime(row.checkedAt) }}</template>
          </el-table-column>
          <el-table-column prop="checkedByName" label="检查人" width="120" />
          <el-table-column label="结果" width="130">
            <template #default="{ row }">
              <el-tag :type="riskTagType(row.conflictLevel)">{{ riskLabel(row.conflictLevel) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="命中摘要" min-width="180">
            <template #default="{ row }">客户 {{ row.matchedClientCount || 0 }}，案件 {{ row.matchedCaseCount || 0 }}，关联主体 {{ row.matchedRelatedSubjectCount || 0 }}</template>
          </el-table-column>
          <el-table-column label="正式审查" min-width="260">
            <template #default="{ row }">
              <template v-if="row.reviewStatus === 'COMPLETED'">
                <el-tag :type="reviewDecisionType(row.reviewDecision)">
                  {{ reviewDecisionLabel(row.reviewDecision) }}
                </el-tag>
                <div class="review-meta">{{ row.reviewedByName || '-' }} · {{ formatDateTime(row.reviewedAt) }}</div>
                <el-tooltip :content="row.reviewConclusion || '未填写审查意见'" placement="top">
                  <div class="review-conclusion">{{ row.reviewConclusion || '未填写审查意见' }}</div>
                </el-tooltip>
              </template>
              <el-tag v-else type="info">待行政审查</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <el-button v-if="row.canDownload" link type="primary" :icon="Download" @click="downloadReport(row)">下载</el-button>
              <el-button
                v-if="canReview && row.caseId && row.reviewStatus !== 'COMPLETED'"
                link
                type="primary"
                @click="selectReviewRecord(row)"
              >
                正式审查
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="reviewForm.recordId" class="review-panel">
          <div class="section-header">
            <div>
              <h3>行政正式审查</h3>
              <p>{{ reviewForm.reportNo }} · {{ reviewForm.subjectName }}</p>
            </div>
            <el-button text @click="cancelReview">取消</el-button>
          </div>

          <el-form label-position="top">
            <el-form-item label="审查结论" required>
              <el-radio-group v-model="reviewForm.decision">
                <el-radio-button value="PASSED">无冲突，通过</el-radio-button>
                <el-radio-button value="REJECTED">存在冲突，不通过</el-radio-button>
                <el-radio-button value="CONDITIONAL">附条件通过</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="审查意见" required>
              <el-input
                v-model="reviewForm.conclusion"
                type="textarea"
                :rows="3"
                maxlength="1000"
                show-word-limit
                placeholder="说明核查范围、既有委托关系及作出结论的理由"
              />
            </el-form-item>
            <el-form-item v-if="reviewForm.decision === 'CONDITIONAL'" label="书面豁免或风险处置依据" required>
              <el-input
                v-model="reviewForm.waiverBasis"
                type="textarea"
                :rows="3"
                maxlength="2000"
                show-word-limit
                placeholder="填写豁免文件、授权决定、信息隔离措施等依据"
              />
            </el-form-item>
            <el-form-item v-if="reviewForm.decision === 'CONDITIONAL'" label="书面依据原件" required>
              <div class="review-upload-block">
                <el-button
                  v-for="attachment in reviewForm.waiverAttachments"
                  :key="attachment.id"
                  link
                  type="primary"
                  @click="downloadWaiver(attachment)"
                >
                  {{ attachment.originalFileName }}
                </el-button>
                <el-upload
                  drag
                  :show-file-list="false"
                  :http-request="uploadWaiver"
                  accept=".pdf,.doc,.docx,.jpg,.jpeg,.png"
                  :disabled="waiverUploading"
                >
                  <el-icon><UploadFilled /></el-icon>
                  <div class="el-upload__text">拖入或点击上传 PDF、Word、JPG、PNG</div>
                  <template #tip><span>单个文件不超过 20MB，提交正式审查后锁定</span></template>
                </el-upload>
              </div>
            </el-form-item>
            <div class="review-actions">
              <el-button @click="cancelReview">取消</el-button>
              <el-button type="primary" :loading="reviewSubmitting" @click="submitReview">提交并锁定结论</el-button>
            </div>
          </el-form>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Download, Search, UploadFilled } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { useUserStore } from '@/stores'
import {
  downloadConflictCheckReport,
  getConflictCheckRecords,
  previewConflictCheck,
  reviewConflictCheckRecord,
  uploadConflictWaiverAttachment,
  downloadConflictWaiverAttachment
} from '@/api/client'

const route = useRoute()
const userStore = useUserStore()
const formRef = ref(null)
const checking = ref(false)
const reviewSubmitting = ref(false)
const waiverUploading = ref(false)
const checked = ref(false)
const previewResult = ref(null)
const historyRecords = ref([])
const formData = reactive({ clientName: '' })
const reviewForm = reactive({ recordId: null, reportNo: '', subjectName: '', decision: '', conclusion: '', waiverBasis: '', waiverAttachments: [] })
const canReview = computed(() => userStore.hasPermission('CASE_FILING_REVIEW'))

const formRules = {
  clientName: [{ required: true, message: '请输入拟签约客户姓名或名称', trigger: 'blur' }]
}

const isSuccessResponse = (response) => response?.success || response?.code === 200
const hits = computed(() => previewResult.value?.hits || [])

const riskLabel = (level) => ({
  DIRECT: '直接冲突线索',
  CASE_PARTY: '案件主体命中',
  EXISTING: '既有客户命中',
  RELATED: '关联主体命中',
  SIMILAR: '相似名称',
  NONE: '未发现线索'
}[level] || '待人工核对')

const riskTagType = (level) => ({
  DIRECT: 'danger', CASE_PARTY: 'warning', EXISTING: 'warning', RELATED: 'warning', SIMILAR: 'warning', NONE: 'success'
}[level] || 'info')

const reviewDecisionLabel = (decision) => ({
  PASSED: '无冲突，通过', REJECTED: '存在冲突，不通过', CONDITIONAL: '附条件通过'
}[decision] || '待行政审查')
const reviewDecisionType = (decision) => ({ PASSED: 'success', REJECTED: 'danger', CONDITIONAL: 'warning' }[decision] || 'info')

const summaryText = computed(() => riskLabel(previewResult.value?.conflictLevel))
const summaryTagType = computed(() => riskTagType(previewResult.value?.conflictLevel))
const summaryAlertType = computed(() => previewResult.value?.conflictLevel === 'DIRECT'
  ? 'error'
  : (previewResult.value?.conflictLevel === 'NONE' ? 'success' : 'warning'))
const summaryDescription = computed(() => previewResult.value?.conflictDescription || '检查完成，请进行人工核对。')
const handlingAdvice = computed(() => previewResult.value?.recommendation || '本结果仅为系统初筛，不替代正式利冲审查。')

const sourceLabel = (source) => ({ CLIENT: '客户库', CASE_PARTY: '案件当事人', RELATED_ENTITY: '关联主体', SIMILAR_SUBJECT: '相似主体' }[source] || source)
const matchLabel = (type) => ({ EXACT_NAME: '名称一致', SIMILAR_NAME: '名称相似', IDENTITY: '证件一致', RELATION_GRAPH: '关系图谱' }[type] || type)
const partyRoleLabel = (role) => ({
  PLAINTIFF: '原告', DEFENDANT: '被告', APPLICANT: '申请人', RESPONDENT: '被申请人',
  APPELLANT: '上诉人', APPELLEE: '被上诉人', CLIENT: '委托人', COUNTERPARTY: '相对方'
}[role] || role || '-')
const formatDateTime = (value) => {
  if (!value) return '-'
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value
    const pad = (number) => String(number).padStart(2, '0')
    return `${year}-${pad(month)}-${pad(day)} ${pad(hour)}:${pad(minute)}:${pad(second)}`
  }
  return String(value).replace('T', ' ').slice(0, 19)
}

const loadHistory = async (name) => {
  const response = await getConflictCheckRecords(name)
  historyRecords.value = isSuccessResponse(response) ? (response.data || []) : []
}

const handleCheck = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    checking.value = true
    const keyword = formData.clientName.trim()
    const response = await previewConflictCheck({ clientName: keyword })
    if (!isSuccessResponse(response)) throw new Error(response?.message || '检查失败')
    previewResult.value = response.data
    checked.value = true
    await loadHistory(keyword)
    ElMessage.success('全库核对完成并已留痕')
  } catch (error) {
    if (error !== false) {
      console.error('利冲检查失败:', error)
      ElMessage.error(error?.message || '利冲检查失败，请稍后重试')
    }
  } finally {
    checking.value = false
  }
}

const downloadReport = async (record) => {
  try {
    const response = await downloadConflictCheckReport(record.recordId || record.id)
    const url = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.download = `${record.reportNo || '利冲检查报告'}.txt`
    link.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error('报告下载失败')
  }
}

const selectReviewRecord = (record) => {
  reviewForm.recordId = record.id
  reviewForm.reportNo = record.reportNo
  reviewForm.subjectName = record.subjectName
  reviewForm.decision = ''
  reviewForm.conclusion = ''
  reviewForm.waiverBasis = ''
  reviewForm.waiverAttachments = [...(record.waiverAttachments || [])]
}

const cancelReview = () => {
  reviewForm.recordId = null
  reviewForm.reportNo = ''
  reviewForm.subjectName = ''
  reviewForm.decision = ''
  reviewForm.conclusion = ''
  reviewForm.waiverBasis = ''
  reviewForm.waiverAttachments = []
}

const uploadWaiver = async (options) => {
  try {
    waiverUploading.value = true
    const response = await uploadConflictWaiverAttachment(reviewForm.recordId, options.file)
    reviewForm.waiverAttachments.push(response.data)
    options.onSuccess?.(response)
    ElMessage.success('书面依据原件已上传')
  } catch (error) {
    options.onError?.(error)
    ElMessage.error(error?.message || '书面依据上传失败')
  } finally {
    waiverUploading.value = false
  }
}

const downloadWaiver = async (attachment) => {
  try {
    const response = await downloadConflictWaiverAttachment(reviewForm.recordId, attachment.id)
    const url = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.download = attachment.originalFileName || '利冲豁免依据'
    link.click()
    URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error('书面依据下载失败')
  }
}

const submitReview = async () => {
  if (!reviewForm.decision) {
    ElMessage.warning('请选择正式审查结论')
    return
  }
  if (!reviewForm.conclusion.trim()) {
    ElMessage.warning('请填写审查意见')
    return
  }
  if (reviewForm.decision === 'CONDITIONAL' && !reviewForm.waiverBasis.trim()) {
    ElMessage.warning('附条件通过必须填写书面豁免或风险处置依据')
    return
  }
  if (reviewForm.decision === 'CONDITIONAL' && !reviewForm.waiverAttachments.length) {
    ElMessage.warning('附条件通过必须上传书面豁免或风险处置依据原件')
    return
  }
  try {
    reviewSubmitting.value = true
    await reviewConflictCheckRecord(reviewForm.recordId, {
      decision: reviewForm.decision,
      conclusion: reviewForm.conclusion.trim(),
      waiverBasis: reviewForm.waiverBasis.trim()
    })
    ElMessage.success('正式利冲审查已完成，结论已锁定')
    cancelReview()
    await loadHistory(formData.clientName.trim())
  } catch (error) {
    ElMessage.error(error?.message || '提交正式审查失败')
  } finally {
    reviewSubmitting.value = false
  }
}

const handleReset = () => {
  formData.clientName = ''
  checked.value = false
  previewResult.value = null
  historyRecords.value = []
  formRef.value?.clearValidate()
}

onMounted(() => {
  const name = String(route.query.name || '').trim()
  if (name) formData.clientName = name
})
</script>

<style scoped lang="scss">
.conflict-check {
  .check-container {
    display: flex;
    flex-direction: column;
    gap: 16px;
    padding: 20px;
  }

  .search-panel,
  .result-panel,
  .history-panel {
    padding: 24px;
    background: #fff;
    border: 1px solid var(--app-border-color, #e5e7eb);
    border-radius: 8px;
  }

  .search-panel { max-width: 680px; }

  .scope-note,
  .history-count {
    margin: 0;
    color: #6b7280;
    font-size: 13px;
    line-height: 1.6;
  }

  .section-header,
  .header-actions {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .section-header {
    margin-bottom: 18px;
    h3 { margin: 0; font-size: 16px; font-weight: 600; color: #1f2937; }
  }

  .summary-alert,
  .result-table { margin-bottom: 16px; }

  .result-block {
    padding: 14px 16px;
    border: 1px solid var(--app-border-color, #e5e7eb);
    border-radius: 8px;
    background: #f8fafc;

    h4 { margin: 0 0 8px; font-size: 14px; }
    p { margin: 0; color: #374151; line-height: 1.7; }
  }

  .review-panel {
    margin-top: 18px;
    padding-top: 20px;
    border-top: 1px solid var(--app-border-color, #e5e7eb);

    .section-header p { margin: 5px 0 0; color: #6b7280; font-size: 13px; }
    .review-actions { display: flex; justify-content: flex-end; gap: 10px; }
  }

  .review-meta,
  .review-conclusion {
    margin-top: 5px;
    color: #6b7280;
    font-size: 12px;
    line-height: 1.4;
  }

  .review-upload-block {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
    width: 100%;

    :deep(.el-upload),
    :deep(.el-upload-dragger) { width: 100%; }
    :deep(.el-upload-dragger) { padding: 18px 12px; border-radius: 8px; }
  }

  .review-conclusion {
    max-width: 240px;
    overflow: hidden;
    color: #374151;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  @media (max-width: 640px) {
    .check-container { padding: 12px; }
    .search-panel, .result-panel, .history-panel { padding: 16px; }
    .section-header { align-items: flex-start; }
    .header-actions { align-items: flex-end; flex-direction: column; }
  }
}
</style>
