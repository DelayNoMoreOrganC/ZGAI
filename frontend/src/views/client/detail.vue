<template>
  <div class="client-detail" data-testid="client-detail" v-loading="loading">
    <el-result v-if="loadError" icon="error" title="客户详情加载失败" :sub-title="loadError">
      <template #extra>
        <el-button @click="router.push('/client/list')">返回客户列表</el-button>
        <el-button type="primary" @click="loadClientPage">重新加载</el-button>
      </template>
    </el-result>

    <template v-else>
    <PageHeader
      :title="clientData.name || '客户详情'"
      :subtitle="[clientData.type, clientData.departmentName].filter(Boolean).join(' · ')"
      :show-back="true"
      @back="$router.back()"
    >
      <template #extra>
        <el-button @click="router.push({ path: '/client/conflict-check', query: { name: clientData.name } })">
          <el-icon><Warning /></el-icon>
          发起利冲
        </el-button>
        <el-button v-if="canEditClient" @click="handleEdit">
          <el-icon><Edit /></el-icon>
          编辑
        </el-button>
        <el-button data-testid="client-create-case" v-if="canCreateCase" type="primary" @click="handleCreateCase">
          <el-icon><Plus /></el-icon>
          新建案件
        </el-button>
      </template>
    </PageHeader>

    <el-tabs v-model="activeTab" class="detail-tabs">
      <!-- 基本信息 -->
      <el-tab-pane label="基本信息" name="basic">
        <div class="tab-content">
          <el-descriptions :column="detailColumns" border>
            <el-descriptions-item label="客户类型">
              <el-tag :type="isIndividual ? 'primary' : 'success'">
                {{ clientData.type || '-' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="客户关系">{{ clientData.clientRelationship || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所属部门">
              {{ clientData.departmentName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="客户角色">{{ clientData.clientRole || '-' }}</el-descriptions-item>
            <el-descriptions-item label="案源人">{{ clientData.sourceUserNames || '-' }}</el-descriptions-item>
            <el-descriptions-item label="承办人">{{ clientData.clientOwnerNames || clientData.ownerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">
              {{ clientData.phone || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="邮箱">
              {{ clientData.email || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="联系地址" :span="2">
              {{ clientData.address || '-' }}
            </el-descriptions-item>

            <!-- 个人信息 -->
            <template v-if="isIndividual">
              <el-descriptions-item label="性别">
                {{ clientData.gender }}
              </el-descriptions-item>
              <el-descriptions-item label="身份证号">
                {{ clientData.idCard || '-' }}
              </el-descriptions-item>
            </template>

            <!-- 企业信息 -->
            <template v-else>
              <el-descriptions-item label="统一社会信用代码" :span="2">
                {{ clientData.creditCode || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="法定代表人">{{ clientData.legalRepresentative || '-' }}</el-descriptions-item>
              <el-descriptions-item label="联系人">{{ clientData.contactPerson || '-' }}</el-descriptions-item>
            </template>

            <el-descriptions-item label="备注" :span="2">
              {{ clientData.remark || '-' }}
            </el-descriptions-item>

            <el-descriptions-item label="客户状态">{{ clientStatusLabel }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">
              {{ clientData.createTime }}
            </el-descriptions-item>
            <el-descriptions-item label="更新时间">
              {{ clientData.updateTime }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </el-tab-pane>

      <el-tab-pane name="relations">
        <template #label>
          关联主体 <el-badge :value="relationList.length" :hidden="relationList.length === 0" />
        </template>
        <div class="tab-content">
          <div v-if="canManageRelations" class="toolbar">
            <el-button v-if="canManageRelations" type="primary" @click="handleAddRelation">
              <el-icon><Plus /></el-icon>
              新增关联主体
            </el-button>
          </div>
          <el-table v-if="relationList.length" :data="relationList" border>
            <el-table-column label="关联方向" width="110">
              <template #default="{ row }">
                <el-tag :type="row.direction === 'OUTBOUND' ? 'primary' : 'info'" size="small">
                  {{ row.direction === 'OUTBOUND' ? '本客户登记' : '反向关联' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="relationTypeName" label="关系类型" width="130" />
            <el-table-column prop="relatedSubjectName" label="关联主体" min-width="200" />
            <el-table-column prop="targetCreditCode" label="统一社会信用代码" width="190" />
            <el-table-column prop="description" label="关系说明" min-width="220" show-overflow-tooltip />
            <el-table-column prop="createdByName" label="登记人" width="110" />
            <el-table-column label="登记时间" width="170">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column v-if="canManageRelations" label="操作" width="90" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.direction === 'OUTBOUND'"
                  link
                  type="danger"
                  @click="handleDeleteRelation(row)"
                >
                  删除
                </el-button>
                <span v-else class="muted-text">只读</span>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="尚未登记母子公司、控制人、曾用名或其他关联主体" />
        </div>
      </el-tab-pane>

      <!-- 关联案件 -->
      <el-tab-pane name="cases">
        <template #label>
          关联案件 <el-badge :value="caseList.length" :hidden="caseList.length === 0" />
        </template>
        <div class="tab-content">
          <div v-if="caseList.length > 0">
            <el-table :data="caseList" border>
              <el-table-column prop="caseName" label="案件名称" width="200">
                <template #default="{ row }">
                  <el-link type="primary" @click="goToCase(row.id)">
                    {{ row.caseName }}
                  </el-link>
                </template>
              </el-table-column>
              <el-table-column prop="caseNumber" label="案号" width="150" />
              <el-table-column prop="caseType" label="案件类型" width="110">
                <template #default="{ row }">
                  <el-tag size="small">{{ getCaseTypeLabel(row.caseType) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="getStatusTagType(row.status)" size="small">
                    {{ getCaseStatusLabel(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="amount" label="标的额(元)" width="120">
                <template #default="{ row }">
                  ¥{{ (row.amount || 0).toLocaleString() }}
                </template>
              </el-table-column>
              <el-table-column prop="ownerName" label="主办律师" width="100" />
              <el-table-column label="立案时间" width="120">
                <template #default="{ row }">{{ formatDate(row.filingDate || row.createdAt) }}</template>
              </el-table-column>
            </el-table>
          </div>
          <el-empty v-else description="暂无关联案件" />
        </div>
      </el-tab-pane>

      <!-- 沟通记录 -->
      <el-tab-pane name="communications">
        <template #label>
          沟通记录 <el-badge :value="communicationList.length" :hidden="communicationList.length === 0" />
        </template>
        <div class="tab-content">
          <div class="toolbar">
            <el-button v-if="canEditClient" type="primary" @click="handleAddCommunication">
              <el-icon><Plus /></el-icon>
              新建记录
            </el-button>
            <el-button @click="handleConflictCheck">
              <el-icon><Warning /></el-icon>
              利益冲突检索
            </el-button>
          </div>

          <el-timeline v-if="communicationList.length > 0" class="communication-timeline">
            <el-timeline-item
              v-for="record in communicationList"
              :key="record.id"
              :timestamp="record.communicationDate"
              placement="top"
            >
              <el-card class="communication-card" shadow="hover">
                <div class="communication-header">
                  <div class="communication-user">
                    <el-avatar :size="32">
                      {{ record.operatorName?.charAt(0) || '记' }}
                    </el-avatar>
                    <div class="user-info">
                      <div class="user-name">{{ record.operatorName || `记录人 #${record.operatorId}` }}</div>
                      <div class="communication-way">{{ record.way }}</div>
                    </div>
                  </div>
                  <div v-if="canEditClient" class="communication-actions">
                    <el-button text type="primary" size="small" @click="handleEditCommunication(record)">
                      编辑
                    </el-button>
                    <el-button text type="danger" size="small" @click="handleDeleteCommunication(record)">
                      删除
                    </el-button>
                  </div>
                </div>
                <div class="communication-content">
                  {{ record.content }}
                </div>
                <div v-if="record.nextFollowUp" class="communication-followup">
                  <el-icon><Calendar /></el-icon>
                  下次跟进：{{ record.nextFollowUp }}
                </div>
              </el-card>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无沟通记录" />
        </div>
      </el-tab-pane>

      <!-- 收费统计 -->
      <el-tab-pane v-if="canViewFinance" label="收费统计" name="finance">
        <div class="tab-content">
          <div class="finance-cards">
            <div class="finance-card">
              <div class="card-label">案件总数</div>
              <div class="card-value">{{ financeStats.totalCases }}</div>
            </div>
            <div class="finance-card">
              <div class="card-label">应收总额</div>
              <div class="card-value">¥{{ (financeStats.totalReceivable || 0).toLocaleString() }}</div>
            </div>
            <div class="finance-card">
              <div class="card-label">已收总额</div>
              <div class="card-value income">¥{{ (financeStats.totalReceived || 0).toLocaleString() }}</div>
            </div>
            <div class="finance-card">
              <div class="card-label">待收总额</div>
              <div class="card-value pending">¥{{ ((financeStats.totalReceivable || 0) - (financeStats.totalReceived || 0)).toLocaleString() }}</div>
            </div>
          </div>

          <div class="finance-list">
            <h4>收费明细</h4>
            <el-table :data="financeList" border>
              <el-table-column prop="caseName" label="案件名称" width="200" />
              <el-table-column prop="caseNumber" label="案号" width="150" />
              <el-table-column prop="lawyerFee" label="律师费(元)" width="120">
                <template #default="{ row }">
                  ¥{{ (row.lawyerFee || 0).toLocaleString() }}
                </template>
              </el-table-column>
              <el-table-column prop="receivedAmount" label="已收(元)" width="120">
                <template #default="{ row }">
                  <span class="income">¥{{ (row.receivedAmount || 0).toLocaleString() }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="pendingAmount" label="待收(元)" width="120">
                <template #default="{ row }">
                  <span class="pending">¥{{ ((row.lawyerFee || 0) - (row.receivedAmount || 0)).toLocaleString() }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="row.status === 'completed' ? 'success' : 'warning'" size="small">
                    {{ row.status === 'completed' ? '已结清' : '进行中' }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="relationDialogVisible" title="新增关联主体" :width="dialogWidth">
      <el-form label-position="top">
        <el-form-item label="关系类型" required>
          <el-select v-model="relationForm.relationType" placeholder="请选择" style="width: 100%">
            <el-option v-for="option in relationTypeOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="主体来源">
          <el-radio-group v-model="relationInputMode">
            <el-radio-button value="EXISTING">选择现有客户</el-radio-button>
            <el-radio-button value="MANUAL">登记外部主体</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="relationInputMode === 'EXISTING'" label="关联客户" required>
          <el-select
            v-model="relationForm.targetClientId"
            filterable
            remote
            :remote-method="searchRelationClients"
            :loading="relationSearchLoading"
            placeholder="输入客户名称检索"
            style="width: 100%"
          >
            <el-option
              v-for="option in relationClientOptions"
              :key="option.id"
              :label="option.clientName"
              :value="option.id"
            />
          </el-select>
        </el-form-item>
        <template v-else>
          <el-form-item label="关联主体名称" required>
            <el-input v-model="relationForm.targetSubjectName" maxlength="200" placeholder="填写单位全称、个人姓名或曾用名" />
          </el-form-item>
          <el-form-item label="统一社会信用代码">
            <el-input v-model="relationForm.targetCreditCode" maxlength="50" placeholder="企业主体建议填写" />
          </el-form-item>
        </template>
        <el-form-item label="关系说明">
          <el-input
            v-model="relationForm.description"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            placeholder="说明持股、控制、担保、历史名称等核对依据"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="relationDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="relationSubmitting" @click="submitRelation">保存关系</el-button>
      </template>
    </el-dialog>

    <!-- 新建沟通记录对话框 -->
    <el-dialog
      v-model="communicationDialogVisible"
      :title="communicationForm.id ? '编辑沟通记录' : '新建沟通记录'"
      :width="dialogWidth"
    >
      <el-form :model="communicationForm" label-width="100px">
        <el-form-item label="沟通日期" required>
          <el-date-picker
            v-model="communicationForm.communicationDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择沟通日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="沟通方式" required>
          <el-select v-model="communicationForm.way" placeholder="请选择">
            <el-option label="电话" value="电话" />
            <el-option label="微信" value="微信" />
            <el-option label="邮件" value="邮件" />
            <el-option label="面谈" value="面谈" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="沟通内容" required>
          <el-input
            v-model="communicationForm.content"
            type="textarea"
            :rows="5"
            placeholder="请输入沟通内容"
          />
        </el-form-item>
        <el-form-item label="下次跟进">
          <el-date-picker
            v-model="communicationForm.nextFollowUp"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="communicationDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitCommunication">确定</el-button>
      </template>
    </el-dialog>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, Plus, Warning, Calendar } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { useUserStore } from '@/stores'
import { getCaseTypeLabel } from '@/utils/caseTypeProfiles'
import {
  getClientDetail,
  getClientCases,
  getCommunications,
  createCommunication,
  updateCommunication,
  deleteCommunication,
  checkConflict,
  searchClients,
  getClientRelations,
  createClientRelation,
  deleteClientRelation
} from '@/api/client'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('basic')
const loading = ref(false)
const loadError = ref('')
const clientId = computed(() => route.params.id)
const viewportWidth = ref(window.innerWidth)
const detailColumns = computed(() => viewportWidth.value < 760 ? 1 : 2)
const dialogWidth = computed(() => viewportWidth.value < 760 ? 'calc(100vw - 24px)' : '600px')
const updateViewportWidth = () => {
  viewportWidth.value = window.innerWidth
}
window.addEventListener('resize', updateViewportWidth)
onBeforeUnmount(() => window.removeEventListener('resize', updateViewportWidth))

const clientData = reactive({
  id: '',
  name: '',
  type: '',
  departmentId: null,
  departmentName: '',
  phone: '',
  email: '',
  address: '',
  gender: '',
  idCard: '',
  creditCode: '',
  remark: '',
  createTime: '',
  updateTime: '',
  canEdit: false,
  canDelete: false
})

const isIndividual = computed(() => clientData.type === '个人')
const canEditClient = computed(() => clientData.canEdit === true)
const canCreateCase = computed(() => userStore.hasPermission('CASE_CREATE'))
const canViewFinance = computed(() => userStore.hasPermission('FINANCE_VIEW'))
const clientStatusLabel = computed(() => ({ ACTIVE: '正常', INACTIVE: '停用' }[clientData.status] || clientData.status || '-'))

const caseList = ref([])
const communicationList = ref([])
const financeStats = reactive({
  totalCases: 0,
  totalReceivable: 0,
  totalReceived: 0
})
const financeList = ref([])
const relationList = ref([])
const relationDialogVisible = ref(false)
const relationSubmitting = ref(false)
const relationSearchLoading = ref(false)
const relationInputMode = ref('EXISTING')
const relationClientOptions = ref([])
const relationForm = reactive({
  relationType: '',
  targetClientId: null,
  targetSubjectName: '',
  targetCreditCode: '',
  description: ''
})
const canManageRelations = computed(() => canEditClient.value)
const relationTypeOptions = [
  { value: 'PARENT_COMPANY', label: '母公司' },
  { value: 'SUBSIDIARY', label: '子公司' },
  { value: 'AFFILIATE', label: '关联企业' },
  { value: 'ACTUAL_CONTROLLER', label: '实际控制人' },
  { value: 'LEGAL_REPRESENTATIVE', label: '法定代表人' },
  { value: 'FORMER_NAME', label: '曾用名' },
  { value: 'GUARANTOR', label: '担保关系' },
  { value: 'OTHER', label: '其他关联' }
]

// 沟通记录对话框
const communicationDialogVisible = ref(false)
const communicationForm = reactive({
  id: null,
  communicationDate: '',
  way: '电话',
  content: '',
  nextFollowUp: ''
})

// 获取客户详情
const fetchClientDetail = async () => {
  const response = await getClientDetail(clientId.value)
  if (response.success) {
    const data = response.data || {}
    Object.assign(clientData, {
      ...data,
      name: data.clientName || data.name || '',
      type: data.clientType || data.type || '',
      remark: data.notes || data.remark || '',
      createTime: formatDateTime(data.createdAt || data.createTime),
      updateTime: formatDateTime(data.updatedAt || data.updateTime)
    })
  }
}

// 获取关联案件
const fetchClientCases = async () => {
  try {
    const response = await getClientCases(clientId.value)
    if (response.success) {
      caseList.value = Array.isArray(response.data) ? response.data : (response.data?.list || [])

      // 计算收费统计
      financeStats.totalCases = caseList.value.length
      financeList.value = caseList.value.map(caseItem => ({
        ...caseItem,
        lawyerFee: caseItem.lawyerFee || 0,
        receivedAmount: caseItem.receivedAmount || 0,
        pendingAmount: (caseItem.lawyerFee || 0) - (caseItem.receivedAmount || 0)
      }))

      financeStats.totalReceivable = financeList.value.reduce((sum, item) => sum + (item.lawyerFee || 0), 0)
      financeStats.totalReceived = financeList.value.reduce((sum, item) => sum + (item.receivedAmount || 0), 0)
    }
  } catch (error) {
    console.error('获取关联案件失败:', error)
  }
}

// 获取沟通记录
const fetchCommunications = async () => {
  try {
    const response = await getCommunications(clientId.value, {
      page: 0,
      size: 100
    })
    if (response.success) {
      const records = Array.isArray(response.data) ? response.data : (response.data?.list || [])
      communicationList.value = records.map(record => ({
        ...record,
        way: record.communicationType || record.way || '-',
        nextFollowUp: record.nextFollowDate || record.nextFollowUp || ''
      }))
    }
  } catch (error) {
    console.error('获取沟通记录失败:', error)
  }
}

const fetchRelations = async () => {
  try {
    const response = await getClientRelations(clientId.value)
    relationList.value = response.data || []
  } catch (error) {
    console.error('获取关联主体失败:', error)
  }
}

const handleAddRelation = () => {
  relationInputMode.value = 'EXISTING'
  relationClientOptions.value = []
  Object.assign(relationForm, {
    relationType: '', targetClientId: null, targetSubjectName: '', targetCreditCode: '', description: ''
  })
  relationDialogVisible.value = true
}

const searchRelationClients = async (keyword) => {
  const value = String(keyword || '').trim()
  if (!value) {
    relationClientOptions.value = []
    return
  }
  try {
    relationSearchLoading.value = true
    const response = await searchClients(value)
    const clients = Array.isArray(response.data) ? response.data : (response.data?.list || [])
    relationClientOptions.value = clients.filter(item => Number(item.id) !== Number(clientId.value))
  } catch (error) {
    relationClientOptions.value = []
  } finally {
    relationSearchLoading.value = false
  }
}

const submitRelation = async () => {
  if (!relationForm.relationType) {
    ElMessage.warning('请选择关系类型')
    return
  }
  if (relationInputMode.value === 'EXISTING' && !relationForm.targetClientId) {
    ElMessage.warning('请选择关联客户')
    return
  }
  if (relationInputMode.value === 'MANUAL' && !relationForm.targetSubjectName.trim()) {
    ElMessage.warning('请填写关联主体名称')
    return
  }
  try {
    relationSubmitting.value = true
    await createClientRelation(clientId.value, {
      relationType: relationForm.relationType,
      targetClientId: relationInputMode.value === 'EXISTING' ? relationForm.targetClientId : null,
      targetSubjectName: relationInputMode.value === 'MANUAL' ? relationForm.targetSubjectName.trim() : null,
      targetCreditCode: relationInputMode.value === 'MANUAL' ? relationForm.targetCreditCode.trim() : null,
      description: relationForm.description.trim()
    })
    relationDialogVisible.value = false
    ElMessage.success('关联主体已保存，后续利冲将纳入核对')
    await fetchRelations()
  } catch (error) {
    ElMessage.error(error?.message || '保存关联主体失败')
  } finally {
    relationSubmitting.value = false
  }
}

const handleDeleteRelation = async (relation) => {
  try {
    await ElMessageBox.confirm('删除后，后续利冲将不再使用该关系。确定继续吗？', '删除关联主体', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    })
    await deleteClientRelation(clientId.value, relation.id)
    ElMessage.success('关联关系已删除')
    await fetchRelations()
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.message || '删除关联关系失败')
  }
}

const formatDate = (value) => value ? String(value).replace('T', ' ').slice(0, 10) : '-'
const formatDateTime = (value) => value ? String(value).replace('T', ' ').slice(0, 19) : '-'

// 编辑客户
const handleEdit = () => {
  router.push(`/client/${clientId.value}/edit`)
}

// 新建案件
const handleCreateCase = () => {
  router.push({
    path: '/case/create',
    query: { clientId: clientId.value, clientName: clientData.name }
  })
}

// 跳转到案件详情
const goToCase = (caseId) => {
  router.push(`/case/${caseId}`)
}

// 获取状态标签类型
const getStatusTagType = (status) => {
  const typeMap = {
    CONSULTATION: 'info', FILING_REVIEW: 'warning', ACTIVE: 'primary', CLOSED: 'success', ARCHIVED: 'info'
  }
  return typeMap[status] || 'info'
}

const getCaseStatusLabel = (status) => ({
  CONSULTATION: '咨询', FILING_REVIEW: '待审批', ACTIVE: '办理中', CLOSED: '已结案', ARCHIVED: '已归档'
}[status] || status || '-')

// 新建沟通记录
const handleAddCommunication = () => {
  Object.assign(communicationForm, {
    id: null,
    communicationDate: new Date().toISOString().slice(0, 10),
    way: '电话',
    content: '',
    nextFollowUp: ''
  })
  communicationDialogVisible.value = true
}

// 编辑沟通记录
const handleEditCommunication = (record) => {
  Object.assign(communicationForm, {
    id: record.id,
    communicationDate: record.communicationDate || '',
    way: record.way,
    content: record.content,
    nextFollowUp: record.nextFollowUp
  })
  communicationDialogVisible.value = true
}

// 提交沟通记录
const handleSubmitCommunication = async () => {
  if (!communicationForm.communicationDate || !communicationForm.way || !communicationForm.content.trim()) {
    ElMessage.warning('请完整填写沟通日期、方式和内容')
    return
  }
  try {
    const data = {
      communicationType: communicationForm.way,
      communicationDate: communicationForm.communicationDate,
      content: communicationForm.content.trim(),
      nextFollowDate: communicationForm.nextFollowUp || null
    }

    let response
    if (communicationForm.id) {
      response = await updateCommunication(clientId.value, communicationForm.id, data)
    } else {
      response = await createCommunication(clientId.value, data)
    }

    if (response.success) {
      ElMessage.success(communicationForm.id ? '编辑成功' : '创建成功')
      communicationDialogVisible.value = false
      await fetchCommunications()
    }
  } catch (error) {
    ElMessage.error('操作失败')
  }
}

// 删除沟通记录
const handleDeleteCommunication = async (record) => {
  try {
    await ElMessageBox.confirm('确定要删除这条沟通记录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await deleteCommunication(clientId.value, record.id)
    ElMessage.success('删除成功')
    await fetchCommunications()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 利益冲突检索
const handleConflictCheck = async () => {
  try {
    ElMessage.info('正在进行利益冲突检索...')
    const response = await checkConflict(clientId.value)
    if (response.success) {
      if (response.data.hasConflict) {
        ElMessageBox.alert(
          `检测到潜在利益冲突：${response.data.conflictDescription}`,
          '利益冲突警告',
          { type: 'warning' }
        )
      } else {
        ElMessage.success('未检测到利益冲突')
      }
    }
  } catch (error) {
    ElMessage.error('利益冲突检索失败')
  }
}

const loadClientPage = async () => {
  try {
    loading.value = true
    loadError.value = ''
    await fetchClientDetail()
    await Promise.all([fetchClientCases(), fetchCommunications(), fetchRelations()])
  } catch (error) {
    caseList.value = []
    communicationList.value = []
    relationList.value = []
    loadError.value = error?.message || '请检查网络连接或客户访问权限'
    ElMessage.error('获取客户详情失败')
  } finally {
    loading.value = false
  }
}

watch(clientId, loadClientPage, { immediate: true })
</script>

<style scoped lang="scss">
.client-detail {
  min-height: 320px;
  min-width: 0;

  .detail-tabs {
    margin-top: 20px;
    padding: 0 20px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    background: #fff;

    .tab-content {
      padding: 20px 0 24px;
      min-height: 400px;
    }
  }

  .communication-timeline {
    margin-top: 20px;

    .communication-card {
      border-radius: 8px;

      .communication-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;

        .communication-user {
          display: flex;
          align-items: center;
          gap: 12px;

          .user-info {
            .user-name {
              font-weight: 500;
              color: #333;
            }

            .communication-way {
              font-size: 12px;
              color: #999;
            }
          }
        }
      }

      .communication-content {
        color: #606266;
        line-height: 1.6;
        margin-bottom: 12px;
      }

      .communication-followup {
        display: flex;
        align-items: center;
        gap: 6px;
        padding: 8px 12px;
        background: #f0f5ff;
        border-radius: 4px;
        color: #1890ff;
        font-size: 13px;
      }
    }
  }

  .finance-cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 20px;
    margin-bottom: 30px;

    .finance-card {
      padding: 20px;
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      background: #fff;
      color: #1f2937;

      .card-label {
        font-size: 14px;
        color: #6b7280;
        margin-bottom: 8px;
      }

      .card-value {
        font-size: 24px;
        font-weight: bold;
      }

      .income {
        color: #67c23a;
      }

      .pending {
        color: #e6a23c;
      }
    }
  }

  .finance-list {
    h4 {
      margin-bottom: 16px;
      color: #333;
    }

    .income {
      color: #67c23a;
      font-weight: 500;
    }

    .pending {
      color: #e6a23c;
      font-weight: 500;
    }
  }

  .toolbar {
    margin-bottom: 20px;
    display: flex;
    gap: 12px;
  }

  .muted-text {
    color: #9ca3af;
    font-size: 12px;
  }
}

@media (max-width: 760px) {
  .client-detail {
    .detail-tabs {
      max-width: 100%;
      margin-top: 14px;
      padding: 0 12px;

      :deep(.el-tabs__nav-wrap) {
        overflow-x: auto;
      }

      :deep(.el-tabs__nav-scroll) {
        overflow: visible;
      }

      .tab-content {
        min-width: 0;
        overflow: hidden;
        min-height: 300px;
        padding-top: 14px;
      }
    }

    .toolbar {
      align-items: stretch;
      flex-direction: column;

      .el-button {
        width: 100%;
        margin-left: 0;
      }
    }

    .communication-timeline {
      padding-left: 0;

      .communication-card .communication-header {
        align-items: flex-start;
        flex-direction: column;
        gap: 10px;
      }
    }

    .finance-cards {
      grid-template-columns: minmax(0, 1fr);
      gap: 12px;
      margin-bottom: 20px;
    }

    :deep(.el-dialog .el-form-item__label) {
      line-height: 1.3;
    }
  }
}
</style>
