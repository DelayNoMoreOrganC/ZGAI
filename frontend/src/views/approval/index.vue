<template>
  <div class="approval">
    <PageHeader title="审批管理">
      <template #extra>
        <el-button type="primary" @click="handleCreateApproval" class="create-btn">
          <el-icon><Plus /></el-icon>
          发起审批
        </el-button>
      </template>
    </PageHeader>

    <!-- 筛选器 -->
    <ApprovalFilter @search="handleSearch" @reset="handleReset" />

    <div v-if="isAdministrativeUser" class="role-workbench admin-approval-workbench">
      <div class="workbench-title">
        <div>
          <h2>行政审批中心</h2>
          <p>集中处理立案申请、利冲审查和审批流转。</p>
        </div>
        <el-tag type="warning" size="large">待处理 {{ pendingList.length }}</el-tag>
      </div>
      <div v-if="pendingFilingApprovals.length > 0" class="filing-cards">
        <div
          v-for="item in pendingFilingApprovals"
          :key="item.id"
          class="filing-card"
          @click="openApprovalDetail(item)"
        >
          <div class="filing-card-main">
            <el-tag type="warning">{{ formatApprovalType(item.approvalType) }}</el-tag>
            <h3>{{ item.title }}</h3>
            <p>{{ item.caseName || item.content || '请进入详情进行利冲审查' }}</p>
          </div>
          <div class="filing-card-actions" @click.stop>
            <el-button type="primary" plain @click="openApprovalDetail(item)">查看审批</el-button>
            <el-button
              v-if="canHandleApproval(item)"
              type="success"
              class="approve-main-btn"
              @click="handleApprove(item)"
            >
              {{ getApproveActionText(item) }}
            </el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无立案审批待办" />
    </div>

    <div v-else class="role-workbench lawyer-approval-workbench">
      <div class="workbench-title">
        <div>
          <h2>我的立案申请</h2>
          <p>提交后可在“我发起的”查看当前审批人、审批状态和案件进度。</p>
        </div>
        <el-button type="primary" @click="activeTab = 'my-requests'">查看我的申请</el-button>
      </div>
      <div class="lawyer-steps">
        <span>提交立案申请</span>
        <span>行政利冲审查</span>
        <span>必要时主任终审</span>
        <span>建立案件档案</span>
      </div>
    </div>

    <el-tabs v-model="activeTab" type="card" class="approval-tabs"
      :class="'tab-' + activeTab">
      <!-- 待办 -->
      <el-tab-pane label="待办" name="pending">
        <div class="tab-content">
          <el-alert
            v-if="isCaseApprovalMode"
            class="filing-alert"
            type="info"
            show-icon
            :closable="false"
          >
            <template #title>
              正在查看当前案件的全部审批记录。点击“查看审批”可打开审批内容与处理意见。
            </template>
          </el-alert>
          <el-alert
            v-if="pendingCaseFilingCount > 0"
            class="filing-alert"
            type="warning"
            show-icon
            :closable="false"
          >
            <template #title>
              当前有 {{ pendingCaseFilingCount }} 个立案审批待处理，请进入审批详情后完成利冲审查，并点击“同意立案”或“驳回立案”。
            </template>
          </el-alert>
          <el-table :data="pendingList" border class="approval-table"
            :header-cell-style="{ background: '#f0f5ff', color: '#333', fontWeight: '600' }"
            :row-class-name="tableRowClassName">
            <el-table-column prop="title" label="审批标题" width="200" sortable />
            <el-table-column prop="approvalType" label="审批类型" width="140">
              <template #default="{ row }">
                <el-tag>{{ formatApprovalType(row.approvalType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="applicantName" label="申请人" width="100" sortable />
            <el-table-column prop="applyTime" label="申请时间" width="160" sortable />
            <el-table-column prop="currentApproverName" label="当前审批人" width="120" />
            <el-table-column prop="caseName" label="关联案件" width="200">
              <template #default="{ row }">
                <el-link v-if="row.caseId" @click="goToCase(row.caseId)" type="primary">
                  {{ row.caseName }}
                </el-link>
                <span v-else>{{ row.caseName || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusTagType(row.status)">
                  {{ row.statusDesc || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="320" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" plain @click="openApprovalDetail(row)">
                  查看审批
                </el-button>
                <template v-if="canHandleApproval(row)">
                  <el-button type="success" size="small" class="approve-main-btn" @click="handleApprove(row)">
                    {{ getApproveActionText(row) }}
                  </el-button>
                  <el-button type="warning" size="small" @click="handleReject(row)">
                    {{ isCaseFilingApproval(row) ? '驳回立案' : '驳回' }}
                  </el-button>
                  <el-button type="primary" size="small" @click="handleTransfer(row)">
                    转审
                  </el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 已办 -->
      <el-tab-pane label="已办" name="processed">
        <div class="tab-content">
          <el-table :data="processedList" border class="approval-table"
            :header-cell-style="{ background: '#f0f5ff', color: '#333', fontWeight: '600' }"
            :row-class-name="tableRowClassName">
            <el-table-column prop="title" label="审批标题" width="200" sortable />
            <el-table-column prop="approvalType" label="审批类型" width="140">
              <template #default="{ row }">{{ formatApprovalType(row.approvalType) }}</template>
            </el-table-column>
            <el-table-column prop="applicantName" label="申请人" width="100" sortable />
            <el-table-column prop="approvedTime" label="处理时间" width="160" sortable />
            <el-table-column prop="statusDesc" label="处理结果" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 'APPROVED' ? 'success' : row.status === 'REJECTED' ? 'danger' : 'info'">
                  {{ row.statusDesc || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="approvalNotes" label="处理意见" show-overflow-tooltip />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openApprovalDetail(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 我发起的 -->
      <el-tab-pane label="我发起的" name="my-requests">
        <div class="tab-content">
          <el-table :data="myRequestList" border class="approval-table"
            :header-cell-style="{ background: '#f0f5ff', color: '#333', fontWeight: '600' }"
            :row-class-name="tableRowClassName">
            <el-table-column prop="title" label="审批标题" width="200" sortable />
            <el-table-column prop="approvalType" label="审批类型" width="140">
              <template #default="{ row }">{{ formatApprovalType(row.approvalType) }}</template>
            </el-table-column>
            <el-table-column prop="applicantName" label="申请人" width="100" sortable />
            <el-table-column prop="applyTime" label="申请时间" width="160" sortable />
            <el-table-column prop="currentApproverName" label="当前审批人" width="120" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusTagType(row.status)">
                  {{ row.statusDesc || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="caseName" label="关联案件" width="200">
              <template #default="{ row }">
                <el-link v-if="row.caseId" @click="goToCase(row.caseId)" type="primary">
                  {{ row.caseName }}
                </el-link>
                <span v-else>{{ row.caseName || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openApprovalDetail(row)">查看</el-button>
                <el-button link type="primary" size="small" @click="openApprovalDetail(row)">进度</el-button>
                <el-button
                  link
                  type="warning"
                  size="small"
                  :disabled="row.status !== 'PENDING'"
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

    <ApprovalDetailDrawer
      v-model="detailDrawerVisible"
      :approval-id="selectedApprovalId"
      :initial-approval="selectedApproval"
      @handled="handleApprovalHandled"
    />
  </div>
</template>

<script setup>
import { computed, ref, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import PageHeader from '@/components/PageHeader.vue'
import ApprovalDetailDrawer from '@/components/ApprovalDetailDrawer.vue'
import ApprovalFilter from './ApprovalFilter.vue'
import {
  getApprovalList,
  getApprovalDetail,
  createApproval,
  approveApproval,
  rejectApproval,
  transferApproval,
  withdrawApproval
} from '@/api/approval'
import { getCaseList } from '@/api/case'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeTab = ref('pending')
const loading = ref(false)

const pendingList = ref([])
const processedList = ref([])
const myRequestList = ref([])
const detailDrawerVisible = ref(false)
const selectedApproval = ref(null)
const selectedApprovalId = ref(null)
const isCaseApprovalMode = computed(() => Boolean(route.query.caseId))
const pendingCaseFilingCount = computed(() => pendingList.value.filter(isCaseFilingApproval).length)
const pendingFilingApprovals = computed(() => pendingList.value.filter(isCaseFilingApproval))
const currentUserId = computed(() => Number(userStore.userInfo?.id || userStore.userId || 0))
const isSuperAdmin = computed(() => userStore.userInfo?.username === 'admin')
const currentPosition = computed(() => userStore.userInfo?.position || '')
const isDirector = computed(() => currentPosition.value === '主任')
const isAdministrativeUser = computed(() => {
  const position = currentPosition.value
  return isSuperAdmin.value || position.startsWith('行政管理') || position === '主任' || position === '财务管理'
})

const approvalTypeMap = {
  SEAL: '用印申请',
  REIMBURSEMENT: '费用报销',
  INVOICE: '开票申请',
  LEAVE: '请假出差',
  BUSINESS_TRIP: '出差申请',
  CONTRACT: '合同审批',
  OTHER: '其他审批',
  CASE_FILING: '立案审批',
  CASE_FILING_DIRECTOR: '主任终审'
}

const isSuccessResponse = (response) => response?.success || response?.code === 200
const formatApprovalType = (type) => approvalTypeMap[type] || type || '-'
const isCaseFilingApproval = (row) => ['CASE_FILING', 'CASE_FILING_DIRECTOR'].includes(row?.approvalType)
const getApproveActionText = (row) => {
  if (row?.approvalType === 'CASE_FILING_DIRECTOR') return '终审通过'
  if (row?.approvalType === 'CASE_FILING') return '同意立案'
  return '同意'
}
const canHandleApproval = (row) => {
  return row?.status === 'PENDING'
    && (isSuperAdmin.value || isDirector.value || Number(row.currentApproverId) === currentUserId.value)
}

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

// 跳转到案件详情
const goToCase = (caseId) => {
  router.push(`/case/${caseId}`)
}

const openApprovalDetail = (row) => {
  selectedApproval.value = row
  selectedApprovalId.value = row?.id || null
  detailDrawerVisible.value = true
}

const focusApprovalFromRoute = async () => {
  const approvalId = Number(route.query.approvalId)
  if (!approvalId) return false
  const allRows = [...pendingList.value, ...processedList.value, ...myRequestList.value]
  const target = allRows.find(item => Number(item.id) === approvalId)
  if (target) {
    openApprovalDetail(target)
    return true
  }
  try {
    const response = await getApprovalDetail(approvalId)
    if (isSuccessResponse(response) && response.data) {
      openApprovalDetail(response.data)
      return true
    }
  } catch (error) {
    console.warn('无法打开指定审批详情:', error)
  }
  return false
}

const focusFirstCaseApprovalFromRoute = () => {
  if (!isCaseApprovalMode.value) return false
  const allRows = [...pendingList.value, ...processedList.value, ...myRequestList.value]
  if (allRows.length > 0) {
    openApprovalDetail(allRows[0])
    return true
  }
  return false
}

const openRouteApproval = async () => {
  selectedApproval.value = null
  selectedApprovalId.value = null
  detailDrawerVisible.value = false
  await fetchApprovalList()
  const openedByApprovalId = await focusApprovalFromRoute()
  if (!openedByApprovalId) {
    const openedByCase = focusFirstCaseApprovalFromRoute()
    if (isCaseApprovalMode.value && !openedByCase) {
      ElMessage.warning('当前案件暂无审批记录')
    }
  }
}

// 页面加载时获取数据
onMounted(async () => {
  activeTab.value = isAdministrativeUser.value ? 'pending' : 'my-requests'
  await openRouteApproval()

  // 检查是否需要自动创建审批（从案件页面跳转过来）
  if (route.query.action === 'create' && route.query.caseId) {
    // 自动加载案件列表并打开创建审批对话框
    handleCreateApproval().then(() => {
      // 自动填充案件信息
      approvalForm.value.caseId = route.query.caseId
      approvalForm.value.title = `${route.query.caseName || '案件'}相关审批`
      approvalForm.value.description = `关于案件【${route.query.caseName || '未命名案件'}】的审批申请`

      ElMessage.success('已自动关联案件，请补充审批信息')
    })
  } else if (route.query.caseId) {
    // 只筛选案件的审批流程
    ElMessage.info(`正在查看案件的审批流程`)
  }
})

// 监听Tab切换，自动加载对应数据
watch(activeTab, () => {
  openRouteApproval()
})

watch(
  () => [route.query.caseId, route.query.approvalId],
  () => {
    openRouteApproval()
  }
)

const getStatusTagType = (status) => {
  const typeMap = {
    PENDING: 'primary',
    APPROVED: 'success',
    REJECTED: 'danger',
    WITHDRAWN: 'info',
    TRANSFERRED: 'warning',
    '审批中': 'primary',
    '已通过': 'success',
    '已同意': 'success',
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
    await createApproval({
      ...approvalForm.value,
      content: approvalForm.value.description
    })
    ElMessage.success('审批提交成功')
    approvalDialogVisible.value = false
    await fetchApprovalList()
  } catch (error) {
    console.error('提交审批失败:', error)
    ElMessage.error('提交失败')
  }
}

const handleApprove = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt(
      isCaseFilingApproval(row) ? '请填写利冲审查结论或立案审批意见' : '请输入审批意见（可选）',
      `${getApproveActionText(row)}：${row.title}`,
      {
        confirmButtonText: getApproveActionText(row),
        cancelButtonText: '取消',
        inputPlaceholder: isCaseFilingApproval(row) ? '例如：经核查，未发现利益冲突，同意立案' : '请输入审批意见',
        type: 'success'
      }
    )

    const response = await approveApproval(row.id, { comments: value || '' })
    if (isSuccessResponse(response)) {
      ElMessage.success(isCaseFilingApproval(row) ? '立案审批已通过' : '审批已同意')
      detailDrawerVisible.value = false
      await fetchApprovalList()
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
      isCaseFilingApproval(row) ? '请输入驳回立案原因（必填）' : '请输入驳回原因（必填）',
      `${isCaseFilingApproval(row) ? '驳回立案' : '驳回审批'}：${row.title}`,
      {
        confirmButtonText: isCaseFilingApproval(row) ? '驳回立案' : '确定驳回',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入驳回原因',
        inputPattern: /.+/,
        inputErrorMessage: '驳回原因不能为空',
        type: 'warning'
      }
    )

    const response = await rejectApproval(row.id, { comments: value })
    if (isSuccessResponse(response)) {
      ElMessage.success(isCaseFilingApproval(row) ? '立案申请已驳回' : '审批已驳回')
      detailDrawerVisible.value = false
      await fetchApprovalList()
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
    if (isSuccessResponse(response)) {
      ElMessage.success('审批已转审')
      await fetchApprovalList()
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
    if (isSuccessResponse(response)) {
      ElMessage.success('审批已撤回')
      await fetchApprovalList()
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
    const params = {
      caseId: route.query.caseId || null,
      page: 1,
      size: 100
    }

    if (activeTab.value === 'pending') {
      params.status = 'PENDING'
    } else if (activeTab.value === 'my-requests') {
      params.applicantId = currentUserId.value
    } else if (!isAdministrativeUser.value) {
      params.currentApproverId = currentUserId.value
    }

    const res = await getApprovalList({
      ...params
    })

    const records = res.data?.records || []
    // 根据当前Tab分配数据
    if (activeTab.value === 'pending') {
      pendingList.value = records
    } else if (activeTab.value === 'processed') {
      processedList.value = records.filter(item => ['APPROVED', 'REJECTED', 'WITHDRAWN', 'TRANSFERRED'].includes(item.status))
    } else {
      myRequestList.value = records
    }
  } catch (error) {
    ElMessage.error('获取审批列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleApprovalHandled = async () => {
  detailDrawerVisible.value = false
  await fetchApprovalList()
}

const handleSearch = (filters) => {
  // 搜索审批 - 应用筛选条件
  ElMessage.success('筛选功能已就绪，待接入API')
}

const handleReset = () => {
  // 重置筛选条件
  ElMessage.info('筛选条件已重置')
}

const tableRowClassName = ({ rowIndex }) => {
  return rowIndex % 2 === 0 ? 'even-row' : 'odd-row'
}
</script>

<style scoped lang="scss">
.approval {
  .role-workbench {
    margin-top: 18px;
    padding: 18px 20px;
    border: 1px solid #e5e7eb;
    border-radius: 12px;
    background: #fff;
    box-shadow: 0 2px 12px rgba(15, 23, 42, 0.06);
  }

  .workbench-title {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    align-items: center;

    h2 {
      margin: 0;
      font-size: 18px;
      color: #1f2937;
    }

    p {
      margin: 6px 0 0;
      color: #6b7280;
      font-size: 13px;
    }
  }

  .filing-cards {
    display: grid;
    gap: 12px;
    margin-top: 16px;
  }

  .filing-card {
    display: flex;
    justify-content: space-between;
    gap: 16px;
    padding: 14px;
    border: 1px solid #fde68a;
    border-radius: 10px;
    background: #fffbeb;
    cursor: pointer;

    &:hover {
      border-color: #f59e0b;
      box-shadow: 0 6px 16px rgba(245, 158, 11, 0.16);
    }
  }

  .filing-card-main {
    min-width: 0;

    h3 {
      margin: 10px 0 6px;
      font-size: 15px;
      color: #1f2937;
    }

    p {
      margin: 0;
      color: #6b7280;
      font-size: 13px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .filing-card-actions {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-shrink: 0;
  }

  .lawyer-steps {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 10px;
    margin-top: 16px;

    span {
      padding: 10px 12px;
      border-radius: 8px;
      background: #f8fafc;
      border: 1px solid #e5e7eb;
      color: #374151;
      text-align: center;
      font-size: 13px;
    }
  }

  .approval-tabs {
    margin-top: 20px;
    background: #fff;
    padding: 24px;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(24, 144, 255, 0.08);
    border: 1px solid #e6f7ff;

    :deep(.el-tabs__header) {
      margin-bottom: 24px;
      border-bottom: 2px solid #e6f7ff;
    }

    :deep(.el-tabs__item) {
      color: #666;
      font-weight: 500;
      padding: 0 24px;
      height: 40px;
      line-height: 40px;
      border: none;
      transition: all 0.3s;

      &:hover {
        color: #1890ff;
        background: #f0f5ff;
      }

      &.is-active {
        color: #1890ff;
        background: linear-gradient(135deg, #f0f5ff 0%, #e6f7ff 100%);
        border-bottom: 2px solid #1890ff;
        font-weight: 600;
      }
    }
  }

  .create-btn {
    background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
    border: none;
    border-radius: 8px;
    padding: 10px 24px;
    box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3);
    transition: all 0.3s;

    &:hover {
      background: linear-gradient(135deg, #40a9ff 0%, #1890ff 100%);
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(24, 144, 255, 0.4);
    }
  }

  .approval-table {
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 2px 12px rgba(24, 144, 255, 0.08);

    :deep(.el-table__header-wrapper) {
      th {
        background: #f0f5ff !important;
        color: #333 !important;
        font-weight: 600;
        border-bottom: 2px solid #1890ff;
      }
    }

    :deep(.el-table__body-wrapper) {
      .el-table__row {
        transition: all 0.3s;

        &.even-row {
          background: #ffffff;

          &:hover {
            background: #f0f5ff !important;
          }
        }

        &.odd-row {
          background: #fafcfe;

          &:hover {
            background: #f0f5ff !important;
          }
        }

        td {
          border-bottom: 1px solid #f0f0f0;
        }
      }
    }

    :deep(.el-table__border) {
      border: 1px solid #e6f7ff;
    }

    // 操作按钮优化
    .el-button {
      border-radius: 6px;
      transition: all 0.3s;

      &.el-button--success {
        background: linear-gradient(135deg, #52c41a 0%, #389e0d 100%);
        border: none;

        &:hover {
          background: linear-gradient(135deg, #73d13d 0%, #52c41a 100%);
          transform: translateY(-1px);
          box-shadow: 0 2px 8px rgba(82, 196, 26, 0.3);
        }
      }

      &.el-button--warning {
        background: linear-gradient(135deg, #faad14 0%, #d46b08 100%);
        border: none;

        &:hover {
          background: linear-gradient(135deg, #ffc53d 0%, #faad14 100%);
          transform: translateY(-1px);
          box-shadow: 0 2px 8px rgba(250, 173, 20, 0.3);
        }
      }

      &.el-button--primary {
        background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
        border: none;

        &:hover {
          background: linear-gradient(135deg, #40a9ff 0%, #1890ff 100%);
          transform: translateY(-1px);
          box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3);
        }
      }
    }
  }

  .tab-content {
    min-height: 400px;
  }

  .filing-alert {
    margin-bottom: 16px;
    border-radius: 8px;
  }

  .approve-main-btn {
    font-weight: 700;
  }
}
</style>
