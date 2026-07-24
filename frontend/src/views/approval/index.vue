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
          <h2>{{ isDirector ? '主任审批中心' : '行政审批中心' }}</h2>
          <p>{{ isDirector
            ? '查看全所审批进度，重点处理立案终审和管理决策。'
            : '集中处理立案申请、利冲审查、公章用印、案件结案和审批流转。' }}</p>
        </div>
        <el-tag type="warning" size="large">待处理 {{ pendingList.length }}</el-tag>
      </div>
      <div v-if="pendingPriorityApprovals.length > 0" class="filing-cards">
        <div
          v-for="item in pendingPriorityApprovals"
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
              v-if="canHandleApproval(item) && !['SEAL', 'CASE_CLOSURE'].includes(item.approvalType)"
              type="success"
              class="approve-main-btn"
              @click="handleApprove(item)"
            >
              {{ getApproveActionText(item) }}
            </el-button>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无重点审批待办" />
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
        <span>主任终审</span>
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
          <el-table v-loading="loading" :data="pendingList" border class="approval-table" empty-text="暂无待审批记录"
            :header-cell-style="{ background: '#f0f5ff', color: '#333', fontWeight: '600' }"
            :row-class-name="tableRowClassName">
            <el-table-column prop="title" label="审批标题" width="200" sortable />
            <el-table-column prop="approvalType" label="审批类型" width="140">
              <template #default="{ row }">
                <el-tag>{{ formatApprovalType(row.approvalType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="applicantName" label="申请人" width="100" sortable />
            <el-table-column prop="applyTime" label="申请时间" width="160" sortable :formatter="formatTableDateTime" />
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
                  <el-button
                    v-if="!['SEAL', 'CASE_CLOSURE'].includes(row.approvalType)"
                    type="success"
                    size="small"
                    class="approve-main-btn"
                    @click="handleApprove(row)"
                  >
                    {{ getApproveActionText(row) }}
                  </el-button>
                  <el-button
                    v-if="!['SEAL', 'CASE_CLOSURE'].includes(row.approvalType)"
                    type="warning"
                    size="small"
                    @click="handleReject(row)"
                  >
                    {{ isCaseFilingApproval(row) ? '驳回立案' : '驳回' }}
                  </el-button>
                  <el-button v-else type="warning" size="small" @click="openApprovalDetail(row)">
                    {{ row.approvalType === 'CASE_CLOSURE' ? '核对结案资料' : '查看文件并审批' }}
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
          <el-table v-loading="loading" :data="processedList" border class="approval-table" empty-text="暂无已办记录"
            :header-cell-style="{ background: '#f0f5ff', color: '#333', fontWeight: '600' }"
            :row-class-name="tableRowClassName">
            <el-table-column prop="title" label="审批标题" width="200" sortable />
            <el-table-column prop="approvalType" label="审批类型" width="140">
              <template #default="{ row }">{{ formatApprovalType(row.approvalType) }}</template>
            </el-table-column>
            <el-table-column prop="applicantName" label="申请人" width="100" sortable />
            <el-table-column prop="approvedTime" label="处理时间" width="160" sortable :formatter="formatTableDateTime" />
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
          <el-table v-loading="loading" :data="myRequestList" border class="approval-table" empty-text="暂无我发起的审批"
            :header-cell-style="{ background: '#f0f5ff', color: '#333', fontWeight: '600' }"
            :row-class-name="tableRowClassName">
            <el-table-column prop="title" label="审批标题" width="200" sortable />
            <el-table-column prop="approvalType" label="审批类型" width="140">
              <template #default="{ row }">{{ formatApprovalType(row.approvalType) }}</template>
            </el-table-column>
            <el-table-column prop="applicantName" label="申请人" width="100" sortable />
            <el-table-column prop="applyTime" label="申请时间" width="160" sortable :formatter="formatTableDateTime" />
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

    <div v-if="totalRecords > 0" class="approval-pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[20, 50, 100]"
        :total="totalRecords"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>

    <!-- 发起审批对话框 -->
    <el-dialog v-model="approvalDialogVisible" title="发起审批" width="min(700px, calc(100vw - 24px))">
      <el-form :model="approvalForm" label-width="100px">
        <el-form-item label="审批标题" required>
          <el-input v-model="approvalForm.title" placeholder="请输入审批标题" />
        </el-form-item>

        <el-form-item label="审批类型" required>
          <el-select v-model="approvalForm.approvalType" placeholder="请选择审批类型" style="width: 100%">
            <el-option label="公章用印审批" value="SEAL" />
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

        <el-form-item v-if="approvalForm.approvalType !== 'SEAL'" label="审批人" required>
          <el-select
            v-model="approvalForm.currentApproverId"
            filterable
            placeholder="请选择审批人"
            style="width: 100%"
          >
            <el-option
              v-for="user in userOptions"
              :key="user.id"
              :label="`${user.realName || user.username}${user.position ? `（${user.position}）` : ''}`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item v-if="approvalForm.approvalType === 'SEAL'" label="用印文件" required>
          <el-upload
            drag
            action="#"
            :auto-upload="false"
            :limit="1"
            :file-list="sealFileList"
            accept=".pdf,.doc,.docx,.xls,.xlsx,.png,.jpg,.jpeg"
            :on-change="handleSealFileChange"
            :on-remove="handleSealFileRemove"
          >
            <el-icon><UploadFilled /></el-icon>
            <div class="el-upload__text">拖入待用印文件，或点击选择</div>
            <template #tip>
              <span>支持 PDF、Word、Excel、图片，单个文件不超过 50MB；提交后自动流转至行政人员。</span>
            </template>
          </el-upload>
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

    <el-dialog v-model="transferDialogVisible" title="转交审批" width="min(480px, calc(100vw - 24px))">
      <el-form label-width="90px">
        <el-form-item label="新审批人" required>
          <el-select
            v-model="transferForm.newApproverId"
            filterable
            placeholder="请选择接收人"
            style="width: 100%"
          >
            <el-option
              v-for="user in transferUserOptions"
              :key="user.id"
              :label="`${user.realName || user.username}${user.position ? `（${user.position}）` : ''}`"
              :value="user.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="转审原因" required>
          <el-input
            v-model="transferForm.comments"
            type="textarea"
            :rows="4"
            placeholder="请输入转审原因"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTransfer">确定转审</el-button>
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
import { Plus, UploadFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import PageHeader from '@/components/PageHeader.vue'
import ApprovalDetailDrawer from '@/components/ApprovalDetailDrawer.vue'
import ApprovalFilter from './ApprovalFilter.vue'
import {
  getApprovalList,
  getApprovalDetail,
  createApproval,
  createSealApproval,
  approveApproval,
  rejectApproval,
  transferApproval,
  withdrawApproval
} from '@/api/approval'
import { getCaseList } from '@/api/case'
import { getUserOptions } from '@/api/user'
import {
  isAdministrativeUser as resolveAdministrativeUser,
  isDevelopmentAdmin,
  isDirectorUser
} from '@/utils/userPersona'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const activeTab = ref('pending')
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const totalRecords = ref(0)
const appliedFilters = ref({})

const pendingList = ref([])
const processedList = ref([])
const myRequestList = ref([])
const detailDrawerVisible = ref(false)
const selectedApproval = ref(null)
const selectedApprovalId = ref(null)
const isCaseApprovalMode = computed(() => Boolean(route.query.caseId))
const pendingCaseFilingCount = computed(() => pendingList.value.filter(isCaseFilingApproval).length)
const pendingPriorityApprovals = computed(() => pendingList.value
  .filter(item => isCaseFilingApproval(item) || ['SEAL', 'CASE_CLOSURE'].includes(item.approvalType))
  .slice(0, 6))
const currentUserId = computed(() => Number(userStore.userInfo?.id || userStore.userId || 0))
const isSuperAdmin = computed(() => isDevelopmentAdmin(userStore))
const isDirector = computed(() => isDirectorUser(userStore))
const isAdministrativeUser = computed(() => isSuperAdmin.value || resolveAdministrativeUser(userStore) || isDirector.value)

const approvalTypeMap = {
  SEAL: '公章用印审批',
  REIMBURSEMENT: '费用报销',
  INVOICE: '开票申请',
  LEAVE: '请假出差',
  BUSINESS_TRIP: '出差申请',
  CONTRACT: '合同审批',
  OTHER: '其他审批',
  CASE_FILING: '立案审批',
  CASE_FILING_DIRECTOR: '主任终审',
  CASE_CLOSURE: '案件结案复核'
}

const isSuccessResponse = (response) => response?.success || response?.code === 200
const formatDateTime = (value) => {
  if (!value) return '-'
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0, second = 0] = value
    const pad = number => String(number).padStart(2, '0')
    return `${year}-${pad(month)}-${pad(day)} ${pad(hour)}:${pad(minute)}:${pad(second)}`
  }
  return String(value).replace('T', ' ')
}
const formatTableDateTime = (row, column, value) => formatDateTime(value)
const formatApprovalType = (type) => approvalTypeMap[type] || type || '-'
const isCaseFilingApproval = (row) => ['CASE_FILING', 'CASE_FILING_DIRECTOR'].includes(row?.approvalType)
const getApproveActionText = (row) => {
  if (row?.approvalType === 'CASE_FILING_DIRECTOR') return '终审通过'
  if (row?.approvalType === 'CASE_FILING') return '同意立案'
  if (row?.approvalType === 'SEAL') return '同意用印'
  if (row?.approvalType === 'CASE_CLOSURE') return '确认结案'
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
  currentApproverId: null,
  urgency: 'NORMAL',
  description: ''
})
const sealFile = ref(null)
const sealFileList = ref([])
const userOptions = ref([])
const transferDialogVisible = ref(false)
const transferTarget = ref(null)
const transferForm = ref({ newApproverId: null, comments: '' })
const transferUserOptions = computed(() => userOptions.value.filter(user => Number(user.id) !== currentUserId.value))

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
  currentPage.value = 1
  openRouteApproval()
})

watch(
  () => [route.query.caseId, route.query.approvalId],
  () => {
    currentPage.value = 1
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
  try {
    const [caseResponse] = await Promise.all([
      getCaseList({ page: 0, size: 100 }),
      loadUserOptions()
    ])
    caseList.value = caseResponse.data?.records || []
  } catch (error) {
    console.error('加载审批选项失败:', error)
  }

  approvalForm.value = {
    title: '',
    approvalType: 'SEAL',
    caseId: null,
    currentApproverId: null,
    urgency: 'NORMAL',
    description: ''
  }
  sealFile.value = null
  sealFileList.value = []
  approvalDialogVisible.value = true
}

const handleSealFileChange = (file, fileList) => {
  sealFile.value = file.raw
  sealFileList.value = fileList.slice(-1)
}

const handleSealFileRemove = () => {
  sealFile.value = null
  sealFileList.value = []
}

const loadUserOptions = async () => {
  const response = await getUserOptions({ size: 300 })
  userOptions.value = response.data || []
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
  if (approvalForm.value.approvalType === 'SEAL' && !sealFile.value) {
    ElMessage.warning('请选择需要用印的文件')
    return
  }
  if (approvalForm.value.approvalType !== 'SEAL' && !approvalForm.value.currentApproverId) {
    ElMessage.warning('请选择审批人')
    return
  }

  try {
    let response
    if (approvalForm.value.approvalType === 'SEAL') {
      const formData = new FormData()
      formData.append('title', approvalForm.value.title.trim())
      formData.append('content', approvalForm.value.description.trim())
      if (approvalForm.value.caseId) formData.append('caseId', approvalForm.value.caseId)
      formData.append('file', sealFile.value)
      response = await createSealApproval(formData)
    } else {
      response = await createApproval({
        ...approvalForm.value,
        content: approvalForm.value.description
      })
    }
    if (!isSuccessResponse(response)) {
      ElMessage.error(response.message || '提交失败')
      return
    }
    ElMessage.success('审批提交成功')
    approvalDialogVisible.value = false
    await fetchApprovalList()
  } catch (error) {
    console.error('提交审批失败:', error)
    ElMessage.error('提交失败')
  }
}

const handleApprove = async (row) => {
  if (row.approvalType === 'CASE_FILING' && row.conflictChecks?.some(item => item.reviewStatus !== 'COMPLETED')) {
    openApprovalDetail(row)
    ElMessage.warning('请在审批资料中先完成全部委托方的正式利冲审查')
    return
  }
  if (row.approvalType === 'CASE_FILING' && row.conflictChecks?.some(item => item.reviewDecision === 'REJECTED')) {
    openApprovalDetail(row)
    ElMessage.warning('存在利冲审查不通过结论，请驳回立案申请')
    return
  }
  try {
    const { value } = await ElMessageBox.prompt(
      isCaseFilingApproval(row)
        ? '请填写利冲审查结论或立案审批意见'
        : row.approvalType === 'SEAL' ? '请填写用印审批意见' : '请输入审批意见（可选）',
      `${getApproveActionText(row)}：${row.title}`,
      {
        confirmButtonText: getApproveActionText(row),
        cancelButtonText: '取消',
        inputPlaceholder: isCaseFilingApproval(row) ? '例如：经核查，未发现利益冲突，同意立案' : '请输入审批意见',
        inputPattern: (isCaseFilingApproval(row) || row.approvalType === 'SEAL') ? /\S+/ : undefined,
        inputErrorMessage: isCaseFilingApproval(row)
          ? '立案审批意见不能为空'
          : row.approvalType === 'SEAL' ? '用印审批意见不能为空' : undefined,
        type: 'success'
      }
    )

    const response = await approveApproval(row.id, { comments: value || '' })
    if (isSuccessResponse(response)) {
      ElMessage.success(isCaseFilingApproval(row)
        ? '立案审批已通过'
        : row.approvalType === 'SEAL' ? '已同意用印' : '审批已同意')
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
    if (userOptions.value.length === 0) await loadUserOptions()
    transferTarget.value = row
    transferForm.value = { newApproverId: null, comments: '' }
    transferDialogVisible.value = true
  } catch (error) {
    console.error('加载转审人员失败:', error)
    ElMessage.error('加载人员列表失败')
  }
}

const submitTransfer = async () => {
  if (!transferForm.value.newApproverId) {
    ElMessage.warning('请选择新审批人')
    return
  }
  if (!transferForm.value.comments.trim()) {
    ElMessage.warning('请输入转审原因')
    return
  }
  try {
    const response = await transferApproval(transferTarget.value.id, transferForm.value)
    if (!isSuccessResponse(response)) {
      ElMessage.error(response.message || '操作失败')
      return
    }
    ElMessage.success('审批已转交')
    transferDialogVisible.value = false
    await fetchApprovalList()
  } catch (error) {
    console.error('转审审批失败:', error)
    ElMessage.error('操作失败')
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
      page: currentPage.value,
      size: pageSize.value,
      ...appliedFilters.value
    }

    if (activeTab.value === 'pending' && !params.status) {
      params.status = 'PENDING'
    } else if (activeTab.value === 'processed' && !params.status) {
      params.statusGroup = 'PROCESSED'
    } else if (activeTab.value === 'my-requests') {
      params.applicantId = currentUserId.value
    }

    if (activeTab.value !== 'my-requests'
      && !isCaseApprovalMode.value
      && !isSuperAdmin.value
      && !isDirector.value) {
      params.currentApproverId = currentUserId.value
    }

    const res = await getApprovalList({
      ...params
    })

    const records = res.data?.records || []
    totalRecords.value = Number(res.data?.total || 0)
    // 根据当前Tab分配数据
    if (activeTab.value === 'pending') {
      pendingList.value = records
    } else if (activeTab.value === 'processed') {
      processedList.value = records
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

const handleSearch = async (filters) => {
  const [startDate, endDate] = filters.dateRange || []
  appliedFilters.value = {
    approvalType: filters.approvalType || undefined,
    status: filters.status || undefined,
    keyword: filters.keyword?.trim() || undefined,
    startDate: startDate || undefined,
    endDate: endDate || undefined
  }
  currentPage.value = 1
  await fetchApprovalList()
}

const handleReset = async () => {
  appliedFilters.value = {}
  currentPage.value = 1
  await fetchApprovalList()
}

const handlePageChange = async () => {
  await fetchApprovalList()
}

const handleSizeChange = async () => {
  currentPage.value = 1
  await fetchApprovalList()
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
    border-radius: 8px;
    background: #fff;
    box-shadow: 0 1px 3px rgba(15, 23, 42, 0.06);
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
    border-radius: 8px;
    background: #fffbeb;
    cursor: pointer;

    &:hover {
      border-color: #f59e0b;
      box-shadow: 0 2px 8px rgba(245, 158, 11, 0.12);
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
    border-radius: 8px;
    box-shadow: 0 1px 3px rgba(15, 23, 42, 0.06);
    border: 1px solid #e5e7eb;

    :deep(.el-tabs__header) {
      margin-bottom: 24px;
      border-bottom: 1px solid #e5e7eb;
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
        background: #f5f5f7;
      }

      &.is-active {
        color: #1890ff;
        background: #f5f5f7;
        border-bottom: 2px solid #007aff;
        font-weight: 600;
      }
    }
  }

  .create-btn {
    background: #007aff;
    border-color: #007aff;
    border-radius: 8px;
    padding: 10px 24px;
    box-shadow: none;

    &:hover {
      background: #0068d9;
      border-color: #0068d9;
    }
  }

  .approval-table {
    border-radius: 8px;
    overflow: hidden;
    box-shadow: none;

    :deep(.el-table__header-wrapper) {
      th {
        background: #f5f5f7 !important;
        color: #333 !important;
        font-weight: 600;
        border-bottom: 1px solid #d1d5db;
      }
    }

    :deep(.el-table__body-wrapper) {
      .el-table__row {
        transition: all 0.3s;

        &.even-row {
          background: #ffffff;

          &:hover {
            background: #f5f5f7 !important;
          }
        }

        &.odd-row {
          background: #fafcfe;

          &:hover {
            background: #f5f5f7 !important;
          }
        }

        td {
          border-bottom: 1px solid #f0f0f0;
        }
      }
    }

    :deep(.el-table__border) {
      border: 1px solid #e5e7eb;
    }

    // 操作按钮优化
    .el-button {
      border-radius: 6px;
      transition: all 0.3s;

      &.el-button--success {
        background: #34c759;
        border: none;

        &:hover {
          background: #2eb34f;
        }
      }

      &.el-button--warning {
        background: #ff9f0a;
        border: none;

        &:hover {
          background: #e88f00;
        }
      }

      &.el-button--primary {
        background: #007aff;
        border: none;

        &:hover {
          background: #0068d9;
        }
      }
    }
  }

  .tab-content {
    min-height: 400px;
  }

  .approval-pagination {
    display: flex;
    justify-content: flex-end;
    padding: 16px 0 4px;
  }

  .filing-alert {
    margin-bottom: 16px;
    border-radius: 8px;
  }

  .approve-main-btn {
    font-weight: 700;
  }

  @media (max-width: 768px) {
    min-width: 0;

    .role-workbench,
    .approval-tabs {
      padding: 14px 12px;
    }

    .workbench-title,
    .filing-card {
      align-items: flex-start;
      flex-direction: column;
    }

    .workbench-title > :last-child,
    .filing-card-actions {
      width: 100%;
    }

    .filing-card-main p {
      white-space: normal;
    }

    .filing-card-actions {
      flex-wrap: wrap;
    }

    .lawyer-steps {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .approval-tabs {
      :deep(.el-tabs__nav-wrap) {
        overflow-x: auto;
      }

      :deep(.el-tabs__item) {
        padding: 0 14px;
      }
    }

    .approval-pagination {
      justify-content: flex-start;
      overflow-x: auto;
    }
  }
}
</style>
