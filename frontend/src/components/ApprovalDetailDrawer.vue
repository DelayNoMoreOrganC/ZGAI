<template>
  <el-drawer
    v-model="visible"
    title="审批资料"
    size="520px"
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
          <span>{{ approval.applyTime || '-' }}</span>
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
          <el-descriptions-item label="申请时间">{{ selectedApproval.applyTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="关联案件">{{ selectedApproval.caseName || selectedApproval.caseId || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="approval-content">
          <h3>审批内容</h3>
          <pre>{{ selectedApproval.content || '-' }}</pre>
        </div>

        <div v-if="isCaseFilingApproval(selectedApproval)" class="approval-path">
          <h3>立案审批路径</h3>
          <div class="path-step done">发起人提交立案申请</div>
          <div class="path-step active">行政管理利冲审查并审批</div>
          <div class="path-step">如为免费代理，进入主任终审</div>
          <div class="path-step">审批通过后建立案件档案</div>
        </div>

        <div v-if="canHandleSelectedApproval" class="approval-actions">
          <el-button type="warning" @click="handleReject(selectedApproval)">
            {{ isCaseFilingApproval(selectedApproval) ? '驳回立案' : '驳回' }}
          </el-button>
          <el-button type="success" @click="handleApprove(selectedApproval)">
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
import { getApprovalList, getApprovalDetail, approveApproval, rejectApproval } from '@/api/approval'
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

const currentUserId = computed(() => Number(userStore.userInfo?.id || userStore.userId || 0))
const isSuperAdmin = computed(() => userStore.userInfo?.username === 'admin')
const canHandleSelectedApproval = computed(() => {
  const approval = selectedApproval.value
  return approval?.status === 'PENDING' && (isSuperAdmin.value || Number(approval.currentApproverId) === currentUserId.value)
})

const isSuccessResponse = (response) => response?.success || response?.code === 200
const isCaseFilingApproval = (row) => ['CASE_FILING', 'CASE_FILING_DIRECTOR'].includes(row?.approvalType)
const getApproveActionText = (row) => {
  if (row?.approvalType === 'CASE_FILING_DIRECTOR') return '终审通过'
  if (row?.approvalType === 'CASE_FILING') return '同意立案'
  return '同意'
}

const formatApprovalType = (type) => {
  const map = {
    CASE_FILING: '立案审批',
    CASE_FILING_DIRECTOR: '主任终审',
    SEAL: '用印申请',
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

const handleApprove = async (approval) => {
  try {
    const { value } = await ElMessageBox.prompt(
      isCaseFilingApproval(approval) ? '请填写利冲审查结论或立案审批意见' : '请输入审批意见（可选）',
      getApproveActionText(approval),
      {
        confirmButtonText: getApproveActionText(approval),
        cancelButtonText: '取消',
        inputPlaceholder: '请输入审批意见',
        type: 'success'
      }
    )
    const response = await approveApproval(approval.id, { comments: value || '' })
    if (isSuccessResponse(response)) {
      ElMessage.success(isCaseFilingApproval(approval) ? '立案审批已通过' : '审批已同意')
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
.approval-path {
  margin-top: 18px;

  h3 {
    margin: 0 0 10px;
    color: #1f2937;
    font-size: 15px;
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
