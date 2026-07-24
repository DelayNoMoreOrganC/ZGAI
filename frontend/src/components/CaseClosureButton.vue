<template>
  <el-button
    data-testid="case-closure-open"
    :disabled="latest?.status === 'PENDING'"
    @click="openDialog"
  >
    <el-icon><CircleCheck /></el-icon>
    {{ buttonText }}
  </el-button>

  <el-dialog v-model="visible" title="申请案件结案" :width="dialogWidth" destroy-on-close>
    <el-alert
      v-if="latest"
      :type="statusType(latest.status)"
      :closable="false"
      :title="`最近申请：${statusText(latest.status)}`"
      :description="latest.reviewNotes || undefined"
      class="closure-status"
    />

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-form-item label="结案方式" prop="closureType">
        <el-select v-model="form.closureType" data-testid="closure-type" placeholder="请选择结案方式">
          <el-option v-for="item in closureTypes" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>

      <el-form-item label="案件结果" prop="caseOutcome">
        <el-input
          v-model="form.caseOutcome"
          data-testid="closure-outcome"
          type="textarea"
          :rows="3"
          maxlength="1000"
          show-word-limit
          placeholder="概述裁判、调解、执行、项目交付或委托终止结果"
        />
      </el-form-item>

      <el-form-item label="结案小结" prop="closureSummary">
        <el-input
          v-model="form.closureSummary"
          data-testid="closure-summary"
          type="textarea"
          :rows="5"
          maxlength="5000"
          show-word-limit
          placeholder="总结办理过程、关键工作、结果、风险提示及后续事项（至少20字）"
        />
      </el-form-item>

      <div class="form-grid">
        <el-form-item label="费用处理" prop="feeStatus">
          <el-select v-model="form.feeStatus" data-testid="closure-fee-status" placeholder="请选择">
            <el-option label="费用已结清" value="SETTLED" />
            <el-option label="欠费已确认并另行跟进" value="OUTSTANDING_CONFIRMED" />
            <el-option label="无收费或免费案件" value="NO_FEE" />
          </el-select>
        </el-form-item>
        <el-form-item label="客户交付" prop="clientDeliveryStatus">
          <el-select v-model="form.clientDeliveryStatus" data-testid="closure-delivery-status" placeholder="请选择">
            <el-option label="已向客户交付/告知" value="COMPLETED" />
            <el-option label="无需交付" value="NOT_REQUIRED" />
            <el-option label="待交付事项已书面确认" value="PENDING_CONFIRMED" />
          </el-select>
        </el-form-item>
      </div>

      <el-form-item
        v-if="form.clientDeliveryStatus !== 'NOT_REQUIRED'"
        label="客户交付或后续安排说明"
        prop="clientDeliveryNotes"
      >
        <el-input
          v-model="form.clientDeliveryNotes"
          data-testid="closure-delivery-notes"
          type="textarea"
          :rows="2"
          maxlength="1000"
          placeholder="填写交付时间、方式、接收人或尚待处理事项"
        />
      </el-form-item>

      <el-form-item label="结案依据文件" prop="basisDocumentIds">
        <el-select
          v-model="form.basisDocumentIds"
          data-testid="closure-documents"
          multiple
          filterable
          collapse-tags
          placeholder="至少选择一份裁判文书、结案报告或成果文件"
          style="width: 100%"
        >
          <el-option
            v-for="document in documents"
            :key="document.id"
            :label="`${document.originalFileName || document.documentName} · ${document.documentType || '案件材料'}`"
            :value="document.id"
          />
        </el-select>
        <div v-if="documents.length === 0" class="field-tip">案件暂无文件，请先到案件文档页上传结案依据。</div>
      </el-form-item>

      <el-form-item prop="documentsConfirmed">
        <el-checkbox v-model="form.documentsConfirmed" data-testid="closure-documents-confirmed">
          已核对案件目录、结案依据、费用和客户交付情况，申请行政复核
        </el-checkbox>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button
        data-testid="closure-submit"
        type="primary"
        :loading="submitting"
        @click="submit"
      >提交行政复核</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { CircleCheck } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getCaseDocuments } from '@/api/case'
import { createCaseClosureRequest, getLatestCaseClosureRequest } from '@/api/closure'

const props = defineProps({ caseId: { type: [Number, String], required: true } })
const emit = defineEmits(['submitted'])

const visible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const documents = ref([])
const latest = ref(null)
const dialogWidth = computed(() => window.innerWidth < 700 ? 'calc(100vw - 24px)' : '680px')
const closureTypes = [
  ['裁判/裁决结案', 'JUDGMENT'], ['调解或和解结案', 'SETTLEMENT'],
  ['撤诉/撤回申请', 'WITHDRAWAL'], ['执行完毕', 'EXECUTION_COMPLETED'],
  ['委托事项完成', 'SERVICE_COMPLETED'], ['委托终止', 'TERMINATED'], ['其他', 'OTHER']
].map(([label, value]) => ({ label, value }))
const form = reactive({
  closureType: '', caseOutcome: '', closureSummary: '', feeStatus: '',
  clientDeliveryStatus: '', clientDeliveryNotes: '', documentsConfirmed: false, basisDocumentIds: []
})
const rules = {
  closureType: [{ required: true, message: '请选择结案方式', trigger: 'change' }],
  caseOutcome: [{ required: true, min: 2, message: '请填写案件结果', trigger: 'blur' }],
  closureSummary: [{ required: true, min: 20, message: '结案小结至少20个字符', trigger: 'blur' }],
  feeStatus: [{ required: true, message: '请选择费用处理状态', trigger: 'change' }],
  clientDeliveryStatus: [{ required: true, message: '请选择客户交付状态', trigger: 'change' }],
  clientDeliveryNotes: [{ validator: (_rule, value, callback) => {
    if (form.clientDeliveryStatus !== 'NOT_REQUIRED' && !value?.trim()) callback(new Error('请填写客户交付或后续安排说明'))
    else callback()
  }, trigger: 'blur' }],
  basisDocumentIds: [{ type: 'array', min: 1, required: true, message: '请至少选择一份结案依据文件', trigger: 'change' }],
  documentsConfirmed: [{ validator: (_rule, value, callback) => value ? callback() : callback(new Error('请完成材料核对确认')), trigger: 'change' }]
}

const statusText = status => ({ PENDING: '待行政复核', APPROVED: '已确认结案', REJECTED: '已驳回', WITHDRAWN: '已撤回' }[status] || status)
const statusType = status => ({ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', WITHDRAWN: 'info' }[status] || 'info')
const buttonText = computed(() => latest.value?.status === 'PENDING' ? '结案审核中' : latest.value?.status === 'REJECTED' ? '重新申请结案' : '申请结案')

const openDialog = async () => {
  visible.value = true
  try {
    const [documentResponse, closureResponse] = await Promise.all([
      getCaseDocuments(props.caseId), getLatestCaseClosureRequest(props.caseId)
    ])
    documents.value = documentResponse.data || []
    latest.value = closureResponse.data || null
  } catch (error) {
    ElMessage.error(error?.message || '结案资料加载失败')
  }
}

const loadLatest = async () => {
  try {
    const response = await getLatestCaseClosureRequest(props.caseId)
    latest.value = response.data || null
  } catch (error) {
    latest.value = null
  }
}

onMounted(loadLatest)
watch(() => props.caseId, loadLatest)

const submit = async () => {
  if (!(await formRef.value?.validate().catch(() => false))) return
  submitting.value = true
  try {
    const response = await createCaseClosureRequest(props.caseId, { ...form })
    latest.value = response.data
    ElMessage.success('结案申请已提交行政复核')
    visible.value = false
    emit('submitted', response.data)
  } catch (error) {
    ElMessage.error(error?.message || '结案申请提交失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.closure-status { margin-bottom: 16px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.field-tip { margin-top: 6px; color: #909399; font-size: 12px; }
@media (max-width: 700px) { .form-grid { grid-template-columns: 1fr; gap: 0; } }
</style>
