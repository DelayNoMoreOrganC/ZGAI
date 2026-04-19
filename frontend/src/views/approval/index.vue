<template>
  <div class="approval">
    <PageHeader title="审批管理">
      <template #extra>
        <el-button type="primary" @click="handleCreateApproval">
          <el-icon><Plus /></el-icon>
          发起审批
        </el-button>
      </template>
    </PageHeader>

    <!-- 筛选器 -->
    <ApprovalFilter @search="handleSearch" @reset="handleReset" />

    <el-tabs v-model="activeTab" type="card" class="approval-tabs">
      <!-- 待办 -->
      <el-tab-pane label="待办" name="pending">
        <div class="tab-content">
          <el-table :data="pendingList" border>
            <el-table-column prop="title" label="审批标题" width="200" sortable />
            <el-table-column prop="type" label="审批类型" width="120">
              <template #default="{ row }">
                <el-tag>{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="applicant" label="申请人" width="100" sortable />
            <el-table-column prop="applyTime" label="申请时间" width="160" sortable />
            <el-table-column prop="currentNode" label="当前节点" width="120" />
            <el-table-column prop="caseName" label="关联案件" width="200" />
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button type="success" size="small" @click="handleApprove(row)">
                  同意
                </el-button>
                <el-button type="warning" size="small" @click="handleReject(row)">
                  驳回
                </el-button>
                <el-button type="primary" size="small" @click="handleTransfer(row)">
                  转审
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 已办 -->
      <el-tab-pane label="已办" name="processed">
        <div class="tab-content">
          <el-table :data="processedList" border>
            <el-table-column prop="title" label="审批标题" width="200" sortable />
            <el-table-column prop="type" label="审批类型" width="120" />
            <el-table-column prop="applicant" label="申请人" width="100" sortable />
            <el-table-column prop="processTime" label="处理时间" width="160" sortable />
            <el-table-column prop="result" label="处理结果" width="80">
              <template #default="{ row }">
                <el-tag :type="row.result === '同意' ? 'success' : row.result === '驳回' ? 'danger' : 'info'">
                  {{ row.result }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="comment" label="处理意见" show-overflow-tooltip />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button link type="primary" size="small">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 我发起的 -->
      <el-tab-pane label="我发起的" name="my-requests">
        <div class="tab-content">
          <el-table :data="myRequestList" border>
            <el-table-column prop="title" label="审批标题" width="200" sortable />
            <el-table-column prop="type" label="审批类型" width="120" />
            <el-table-column prop="applicant" label="申请人" width="100" sortable />
            <el-table-column prop="applyTime" label="申请时间" width="160" sortable />
            <el-table-column prop="currentNode" label="当前节点" width="120" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusTagType(row.status)">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="caseName" label="关联案件" width="200" />
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button link type="primary" size="small">查看</el-button>
                <el-button link type="primary" size="small">进度</el-button>
                <el-button
                  link
                  type="warning"
                  size="small"
                  :disabled="row.status !== '审批中'"
                  @click="handleWithdraw(row)"
                >
                  撤回
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 发起审批对话框 -->
    <el-dialog v-model="approvalDialogVisible" title="发起审批" width="700px">
      <el-form :model="approvalForm" label-width="100px">
        <el-form-item label="审批标题" required>
          <el-input v-model="approvalForm.title" placeholder="请输入审批标题" />
        </el-form-item>

        <el-form-item label="审批类型" required>
          <el-select v-model="approvalForm.approvalType" placeholder="请选择审批类型" style="width: 100%">
            <el-option label="用印申请" value="SEAL" />
            <el-option label="报销申请" value="REIMBURSEMENT" />
            <el-option label="请假申请" value="LEAVE" />
            <el-option label="出差申请" value="BUSINESS_TRIP" />
            <el-option label="合同审批" value="CONTRACT" />
            <el-option label="其他审批" value="OTHER" />
          </el-select>
        </el-form-item>

        <el-form-item label="关联案件">
          <el-select
            v-model="approvalForm.caseId"
            filterable
            placeholder="请选择案件（可选）"
            style="width: 100%"
            clearable
          >
            <el-option
              v-for="caseItem in caseList"
              :key="caseItem.id"
              :label="`${caseItem.caseNumber} - ${caseItem.caseName}`"
              :value="caseItem.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="紧急程度">
          <el-radio-group v-model="approvalForm.urgency">
            <el-radio label="NORMAL">普通</el-radio>
            <el-radio label="HIGH">紧急</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="审批内容" required>
          <el-input
            v-model="approvalForm.description"
            type="textarea"
            :rows="6"
            placeholder="请详细描述审批事由、金额、时间等关键信息"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="approvalDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitApproval">提交审批</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import ApprovalFilter from './ApprovalFilter.vue'
import {
  getApprovalList,
  createApproval,
  approveApproval,
  rejectApproval,
  transferApproval,
  withdrawApproval
} from '@/api/approval'
import { getCaseList } from '@/api/case'

const activeTab = ref('pending')
const loading = ref(false)

const pendingList = ref([])
const processedList = ref([])
const myRequestList = ref([])

// 审批对话框
const approvalDialogVisible = ref(false)
const approvalForm = ref({
  title: '',
  approvalType: 'SEAL',
  caseId: null,
  urgency: 'NORMAL',
  description: ''
})

// 案件列表
const caseList = ref([])

// 页面加载时获取数据
onMounted(() => {
  fetchApprovalList()
})

// 监听Tab切换，自动加载对应数据
watch(activeTab, () => {
  fetchApprovalList()
})

const getStatusTagType = (status) => {
  const typeMap = {
    '审批中': 'primary',
    '已通过': 'success',
    '已驳回': 'danger',
    '已撤回': 'info'
  }
  return typeMap[status] || ''
}

const handleCreateApproval = async () => {
  // 加载案件列表
  try {
    const { data } = await getCaseList({ page: 1, size: 100 })
    caseList.value = data.records || []
  } catch (error) {
    console.error('加载案件列表失败:', error)
  }

  approvalForm.value = {
    title: '',
    approvalType: 'SEAL',
    caseId: null,
    urgency: 'NORMAL',
    description: ''
  }
  approvalDialogVisible.value = true
}

const handleSubmitApproval = async () => {
  if (!approvalForm.value.title) {
    ElMessage.warning('请输入审批标题')
    return
  }
  if (!approvalForm.value.description) {
    ElMessage.warning('请输入审批内容')
    return
  }

  try {
    await createApproval(approvalForm.value)
    ElMessage.success('审批提交成功')
    approvalDialogVisible.value = false
    fetchApprovalList()
  } catch (error) {
    console.error('提交审批失败:', error)
    ElMessage.error('提交失败')
  }
}

const handleApprove = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt(
      '请输入审批意见（可选）',
      `同意审批：${row.title}`,
      {
        confirmButtonText: '确定同意',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入审批意见',
        type: 'success'
      }
    )

    const response = await approveApproval(row.id, { comments: value || '' })
    if (response.success) {
      ElMessage.success('审批已同意')
      // 刷新列表
      fetchApprovalList()
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

const handleReject = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt(
      '请输入驳回原因（必填）',
      `驳回审批：${row.title}`,
      {
        confirmButtonText: '确定驳回',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入驳回原因',
        inputPattern: /.+/,
        inputErrorMessage: '驳回原因不能为空',
        type: 'warning'
      }
    )

    const response = await rejectApproval(row.id, { comments: value })
    if (response.success) {
      ElMessage.success('审批已驳回')
      // 刷新列表
      fetchApprovalList()
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

const handleTransfer = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt(
      '请输入转审原因（必填）',
      `转审：${row.title}`,
      {
        confirmButtonText: '确定转审',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入转审原因',
        inputPattern: /.+/,
        inputErrorMessage: '转审原因不能为空',
        type: 'info'
      }
    )

    const response = await transferApproval(row.id, { comments: value })
    if (response.success) {
      ElMessage.success('审批已转审')
      // 刷新列表
      fetchApprovalList()
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('转审审批失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

const handleWithdraw = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要撤回审批"${row.title}"吗？`,
      '撤回审批',
      {
        confirmButtonText: '确定撤回',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response = await withdrawApproval(row.id)
    if (response.success) {
      ElMessage.success('审批已撤回')
      // 刷新列表
      fetchApprovalList()
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('撤回审批失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

// 刷新审批列表
const fetchApprovalList = async () => {
  try {
    loading.value = true
    const res = await getApprovalList({
      status: activeTab.value === 'pending' ? 'PENDING' :
             activeTab.value === 'processed' ? 'APPROVED' : 'ALL',
      page: 1,
      size: 100
    })

    // 根据当前Tab分配数据
    if (activeTab.value === 'pending') {
      pendingList.value = res.data?.records || []
    } else if (activeTab.value === 'processed') {
      processedList.value = res.data?.records || []
    } else {
      myRequestList.value = res.data?.records || []
    }
  } catch (error) {
    ElMessage.error('获取审批列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleSearch = (filters) => {
  // 搜索审批 - 应用筛选条件
  ElMessage.success('筛选功能已就绪，待接入API')
}

const handleReset = () => {
  // 重置筛选条件
  ElMessage.info('筛选条件已重置')
}
</script>

<style scoped lang="scss">
.approval {
  .approval-tabs {
    margin-top: 20px;
    background-color: #fff;
    padding: 20px;
    border-radius: 4px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }

  .tab-content {
    min-height: 400px;
  }
}
</style>
