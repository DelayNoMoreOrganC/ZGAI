<template>
  <div class="case-list">
    <PageHeader title="案件列表">
      <template #extra>
        <el-button v-if="canCreate" type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新建案件
        </el-button>
      </template>
    </PageHeader>

    <!-- 筛选条件区 -->
    <div class="filter-section">
      <el-form
        :model="filterForm"
        class="filter-form"
        :class="{ 'show-advanced': advancedFiltersVisible }"
        label-position="top"
      >
        <el-form-item label="案件/案号">
          <el-input
            v-model="filterForm.keyword"
            placeholder="输入案件名称或案号"
            clearable
            class="filter-control"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="案件类型">
          <el-select v-model="filterForm.caseType" placeholder="请选择" clearable class="filter-control">
            <el-option
              v-for="option in CASE_TYPE_OPTIONS"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="案件状态">
          <el-select v-model="filterForm.status" placeholder="请选择" clearable class="filter-control">
            <el-option label="待审批" value="PENDING_APPROVAL" />
            <el-option label="立案驳回" value="FILING_REJECTED" />
            <el-option label="待立案" value="PENDING_FILING" />
            <el-option label="审理中" value="ACTIVE" />
            <el-option label="结案" value="CLOSED" />
            <el-option label="归档" value="ARCHIVED" />
          </el-select>
        </el-form-item>

        <el-form-item label="案由" class="advanced-filter">
          <el-input
            v-model="filterForm.caseReason"
            placeholder="输入案由关键词"
            clearable
            class="filter-control"
            @keyup.enter="handleSearch"
          />
        </el-form-item>

        <el-form-item label="部门" class="advanced-filter">
          <el-select v-model="filterForm.departmentId" placeholder="请选择部门" clearable filterable class="filter-control">
            <el-option
              v-for="department in departmentList"
              :key="department.id"
              :label="department.deptName"
              :value="department.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="主办律师" class="advanced-filter">
          <el-select v-model="filterForm.ownerId" placeholder="请选择" clearable filterable class="filter-control">
            <el-option
              v-for="lawyer in lawyerList"
              :key="lawyer.id"
              :label="lawyer.name"
              :value="lawyer.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="审理机构" class="advanced-filter">
          <el-select
            v-model="filterForm.court"
            placeholder="法院或仲裁机构"
            clearable
            filterable
            remote
            allow-create
            default-first-option
            :remote-method="searchCourt"
            class="filter-control"
          >
            <el-option
              v-for="court in courtList"
              :key="court"
              :label="court"
              :value="court"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="立案日期" class="advanced-filter">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            class="date-filter"
          />
        </el-form-item>

        <el-form-item class="filter-actions">
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button class="advanced-toggle" @click="advancedFiltersVisible = !advancedFiltersVisible">
            <el-icon><Filter /></el-icon>
            {{ advancedFiltersVisible ? '收起筛选' : '更多筛选' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 视图切换和批量操作 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <el-radio-group v-model="viewMode" @change="handleViewChange">
          <el-radio-button value="list">
            <el-icon><List /></el-icon>
            列表视图
          </el-radio-button>
          <el-radio-button value="kanban">
            <el-icon><Grid /></el-icon>
            本页看板
          </el-radio-button>
        </el-radio-group>

        <el-dropdown v-if="selectedCases.length > 0 && canUseBatchActions" trigger="click" @command="handleBatchAction">
          <el-button type="primary">
            批量操作 ({{ selectedCases.length }})
            <el-icon><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-if="canEdit" command="changeOwner" :disabled="!canBatchChangeOwner">修改主办律师</el-dropdown-item>
              <el-dropdown-item v-if="canDelete" command="delete" :disabled="!canBatchDelete" divided>批量删除</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <div class="toolbar-right">
        <el-button @click="handleRefresh">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 列表视图 -->
    <div v-show="viewMode === 'list'" class="list-view">
      <DataTable
        :table-data="caseList"
        :loading="loading"
        :total="total"
        :current-page="currentPage"
        :page-size="pageSize"
        :show-selection="canUseBatchActions"
        :selection-selectable="canSelectForBatch"
        :show-actions="true"
        :action-width="actionWidth"
        :show-empty-action="canCreate && !hasActiveFilters"
        empty-action-text="新建案件"
        empty-text="暂无符合条件的案件"
        @selection-change="handleSelectionChange"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        @empty-action="handleCreate"
      >
        <el-table-column prop="caseTypeDesc" label="类型" width="80" sortable />

        <el-table-column prop="statusDesc" label="状态" width="100" sortable>
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)" size="small">
              {{ row.statusDesc || row.status }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="caseName" label="案件名称" width="200" sortable>
          <template #default="{ row }">
            <el-link type="primary" @click="handleViewDetail(row)">
              {{ row.caseName }}
            </el-link>
          </template>
        </el-table-column>

        <el-table-column prop="caseNumber" label="案号" width="150" sortable />

        <el-table-column prop="parties" label="案件主体" width="180">
          <template #default="{ row }">
            <span>{{ row.parties || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="currentStage" label="当前阶段" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">
              {{ row.currentStage || '待登记' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="ownerName" label="主办律师" width="80" sortable />

        <el-table-column prop="court" label="审理机构" width="150" />

        <el-table-column prop="nextHearingDate" label="下次开庭" width="110" sortable>
          <template #default="{ row }">
            <div v-if="row.nextHearingDate || row.hearingDate" :class="getHearingClass(row.nextHearingDate || row.hearingDate)">
              {{ formatDate(row.nextHearingDate || row.hearingDate) }}
            </div>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>

        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="handleViewDetail(row)">
            详情
          </el-button>
          <el-button v-if="canEditRow(row)" link type="primary" size="small" @click="handleEdit(row)">
            编辑
          </el-button>
          <el-button v-if="canArchiveRow(row)" link type="warning" size="small" @click="handleArchive(row)">
            归档
          </el-button>
          <el-button v-if="canDeleteRow(row)" link type="danger" size="small" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </DataTable>
    </div>

    <!-- 看板视图 -->
    <div v-show="viewMode === 'kanban'" class="kanban-view">
      <div class="kanban-board">
        <div
          v-for="stage in caseStages"
          :key="stage.key"
          class="kanban-column"
        >
          <div class="column-header">
            <h3>{{ stage.label }}</h3>
            <el-badge :value="getStageCount(stage.key)" class="stage-badge" />
          </div>
          <div class="column-cards">
            <div
              v-for="caseItem in getCasesByStage(stage.key)"
              :key="caseItem.id"
              class="case-card"
              @click="handleViewDetail(caseItem)"
            >
              <div class="card-header">
                <el-tag size="small" :type="getTypeTagType(caseItem.caseType)">
                  {{ caseItem.caseTypeDesc || getCaseTypeLabel(caseItem.caseType) }}
                </el-tag>
              </div>
              <div class="card-title">{{ caseItem.caseName }}</div>
              <div class="card-meta">
                <div class="meta-item">
                  <el-icon><User /></el-icon>
                  {{ caseItem.ownerName }}
                </div>
                <div class="meta-item" v-if="caseItem.nextHearingDate || caseItem.hearingDate">
                  <el-icon><Calendar /></el-icon>
                  {{ formatDate(caseItem.nextHearingDate || caseItem.hearingDate) }}
                </div>
              </div>
              <div class="card-footer">
                <span class="case-number">{{ caseItem.caseNumber }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="ownerDialogVisible" title="修改主办律师" width="min(440px, calc(100vw - 32px))">
      <el-form label-position="top">
        <el-form-item label="新主办律师" required>
          <el-select v-model="newOwnerId" filterable placeholder="请选择主办律师" style="width: 100%">
            <el-option
              v-for="lawyer in lawyerList"
              :key="lawyer.id"
              :label="lawyer.name"
              :value="lawyer.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ownerDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="batchSubmitting" @click="confirmOwnerChange">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, List, Grid, ArrowDown, Refresh, User, Calendar, Filter
} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import DataTable from '@/components/DataTable.vue'
import {
  getCaseList,
  deleteCase,
  batchDeleteCases,
  batchChangeOwner
} from '@/api/case'
import { getDepartmentList } from '@/api/department'
import { getUserOptions } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { CASE_TYPE_OPTIONS, getCaseTypeLabel, getCaseTypeWorkflow } from '@/utils/caseTypeProfiles'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

// 筛选表单
const filterForm = reactive({
  keyword: '',
  caseType: '',
  status: '',
  caseReason: '',
  departmentId: null,
  ownerId: '',
  court: '',
  clientId: null, // 客户ID筛选
  dateRange: []
})

// 视图模式
const viewMode = ref('list')

// 数据
const caseList = ref([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const selectedCases = ref([])
const departmentList = ref([])
const ownerDialogVisible = ref(false)
const newOwnerId = ref(null)
const batchSubmitting = ref(false)
const advancedFiltersVisible = ref(false)

const lawyerList = ref([])

const canCreate = computed(() => userStore.hasPermission('CASE_CREATE'))
const canEdit = computed(() => userStore.hasPermission('CASE_EDIT'))
const canArchive = computed(() => userStore.hasPermission('CASE_ARCHIVE'))
const canDelete = computed(() => userStore.hasPermission('CASE_DELETE'))
const canUseBatchActions = computed(() => canEdit.value || canArchive.value || canDelete.value)
const actionWidth = computed(() => 72
  + (canEdit.value ? 52 : 0)
  + (canArchive.value ? 52 : 0)
  + (canDelete.value ? 52 : 0))
const hasActiveFilters = computed(() => Boolean(
  filterForm.keyword.trim()
  || filterForm.caseType
  || filterForm.status
  || filterForm.caseReason.trim()
  || filterForm.departmentId
  || filterForm.ownerId
  || filterForm.court
  || filterForm.clientId
  || filterForm.dateRange?.length
))
const selectedRowsAllow = (field) => selectedCases.value.length > 0
  && selectedCases.value.every(item => item[field] === true)
const canBatchChangeOwner = computed(() => selectedRowsAllow('canEdit')
  && selectedCases.value.every(item => !['CLOSED', 'ARCHIVED'].includes(item.status)))
const canBatchDelete = computed(() => selectedRowsAllow('canDelete'))
const canEditRow = (row) => row.canEdit === true
const canArchiveRow = (row) => row.canArchive === true
const canDeleteRow = (row) => row.canDelete === true
const canSelectForBatch = (row) => canEditRow(row) || canArchiveRow(row) || canDeleteRow(row)

// 法院列表
const courtList = ref([])

const caseStages = computed(() => {
  if (filterForm.caseType) {
    return getCaseTypeWorkflow(filterForm.caseType).map(stage => ({ key: stage, label: stage, mode: 'stage' }))
  }
  return [
    { key: 'PENDING_APPROVAL', label: '待审批', mode: 'status' },
    { key: 'ACTIVE', label: '办理中', mode: 'status' },
    { key: 'CLOSED', label: '已结案', mode: 'status' },
    { key: 'ARCHIVED', label: '已归档', mode: 'status' }
  ]
})

const fetchLawyerList = async () => {
  try {
    const res = await getUserOptions({ size: 300 })
    const users = res.data || []
    const handlerPositions = ['主任', '部门主管', '合伙人', '律师', '实习律师', '助理', '律师助理']
    lawyerList.value = users
      .filter(user => (user.status === undefined || user.status === 1)
        && handlerPositions.some(position => (user.position || '').includes(position)))
      .map(user => ({ id: user.id, name: user.realName || user.username }))
  } catch (error) {
    console.error('获取主办律师列表失败', error)
    lawyerList.value = []
  }
}

// 获取案件列表
const fetchCaseList = async () => {
  try {
    loading.value = true
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      keyword: filterForm.keyword.trim() || undefined,
      caseType: filterForm.caseType || undefined,
      status: filterForm.status || undefined,
      caseReason: filterForm.caseReason || undefined,
      departmentId: filterForm.departmentId || undefined,
      ownerId: filterForm.ownerId || undefined,
      court: filterForm.court || undefined,
      clientId: filterForm.clientId || undefined,
      startDate: filterForm.dateRange?.[0] || undefined,
      endDate: filterForm.dateRange?.[1] || undefined
    }
    const res = await getCaseList(params)
    caseList.value = res.data?.records || []  // 后端PageResult的data字段包含records
    total.value = res.data?.total || 0
  } catch (error) {
    caseList.value = []
    total.value = 0
    ElMessage.error('获取案件列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const fetchDepartmentList = async () => {
  try {
    const res = await getDepartmentList()
    departmentList.value = res.data || []
  } catch (error) {
    console.error('获取部门列表失败', error)
  }
}

// 搜索法院
const searchCourt = async (query) => {
  if (!query) return
  // 使用全国主要法院数据库
  const majorCourts = [
    '北京市朝阳区人民法院', '北京市海淀区人民法院', '北京市东城区人民法院', '北京市西城区人民法院',
    '上海市浦东新区人民法院', '上海市黄浦区人民法院', '上海市徐汇区人民法院', '上海市静安区人民法院',
    '广州市越秀区人民法院', '广州市天河区人民法院', '广州市海珠区人民法院', '广州市白云区人民法院',
    '深圳市福田区人民法院', '深圳市罗湖区人民法院', '深圳市南山区人民法院', '深圳市宝安区人民法院',
    '杭州市西湖区人民法院', '杭州市上城区人民法院', '杭州市下城区人民法院', '杭州市江干区人民法院',
    '南京市鼓楼区人民法院', '南京市玄武区人民法院', '南京市秦淮区人民法院', '南京市建邺区人民法院',
    '成都市武侯区人民法院', '成都市锦江区人民法院', '成都市青羊区人民法院', '成都市金牛区人民法院',
    '武汉市江汉区人民法院', '武汉市武昌区人民法院', '武汉市洪山区人民法院', '武汉市汉阳区人民法院',
    '西安市雁塔区人民法院', '西安市碑林区人民法院', '西安市莲湖区人民法院', '西安市新城人民法院',
    '重庆市渝中区人民法院', '重庆市江北区人民法院', '重庆市南岸区人民法院', '重庆市九龙坡区人民法院',
    '天津市和平区人民法院', '天津市河西区人民法院', '天津市南开区人民法院', '天津市河北区人民法院',
    '苏州市姑苏区人民法院', '苏州市虎丘区人民法院', '苏州市吴中区人民法院', '苏州市相城区人民法院',
    '青岛市市南区人民法院', '青岛市市北区人民法院', '青岛市崂山区人民法院', '青岛市李沧区人民法院',
    '大连市中山区人民法院', '大连市西岗区人民法院', '大连市沙河口区人民法院', '大连市甘井子区人民法院',
    '厦门市思明区人民法院', '厦门市湖里区人民法院', '厦门市海沧区人民法院', '厦门市集美区人民法院',
    '长沙市岳麓区人民法院', '长沙市芙蓉区人民法院', '长沙市天心区人民法院', '长沙市开福区人民法院',
    '济南市历下区人民法院', '济南市市中区人民法院', '济南市槐荫区人民法院', '济南市天桥区人民法院',
    '沈阳市和平区人民法院', '沈阳市沈河区人民法院', '沈阳市大东区人民法院', '沈阳市铁西区人民法院',
    '哈尔滨市南岗区人民法院', '哈尔滨市道里区人民法院', '哈尔滨市道外区人民法院', '哈尔滨市香坊区人民法院',
    '郑州市金水区人民法院', '郑州市中原区人民法院', '郑州市二七区人民法院', '郑州市管城回族区人民法院'
  ]
  courtList.value = majorCourts.filter(court => court.includes(query))
}

// 获取开庭日期样式
const getHearingClass = (date) => {
  const days = Math.ceil((new Date(date) - new Date()) / (1000 * 60 * 60 * 24))
  if (days < 0) return 'hearing-overdue'
  if (days <= 3) return 'hearing-urgent'
  if (days <= 7) return 'hearing-soon'
  return 'hearing-normal'
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return '-'
  const d = new Date(date)
  return `${d.getMonth() + 1}-${d.getDate()}`
}

// 获取案件类型标签颜色
const getTypeTagType = (type) => {
  const typeMap = {
    CIVIL: 'primary',
    ARBITRATION: 'warning',
    CRIMINAL: 'danger',
    ADMINISTRATIVE: 'info',
    NON_LITIGATION: '',
    CONSULTANT: 'success',
    '民事': 'primary',
    '仲裁': 'warning',
    '刑事': 'danger',
    '行政': 'info',
    '非诉': '',
    '法律顾问': 'success'
  }
  return typeMap[type] || ''
}

const getStatusTagType = (status) => {
  const statusMap = {
    PENDING_APPROVAL: 'warning',
    FILING_REJECTED: 'danger',
    PENDING_FILING: 'info',
    ACTIVE: 'success',
    CLOSED: '',
    ARCHIVED: 'info'
  }
  return statusMap[status] || ''
}

// 看板视图：根据阶段筛选案件
const getCasesByStage = (stageKey) => {
  const column = caseStages.value.find(item => item.key === stageKey)
  if (column?.mode === 'status') {
    return caseList.value.filter(item => item.status === stageKey)
  }
  return caseList.value.filter(item => item.currentStage === stageKey)
}

// 看板视图：获取阶段案件数
const getStageCount = (stageKey) => {
  return getCasesByStage(stageKey).length
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  fetchCaseList()
}

// 重置
const handleReset = () => {
  Object.assign(filterForm, {
    keyword: '',
    caseType: '',
    status: '',
    caseReason: '',
    departmentId: null,
    ownerId: '',
    court: '',
    clientId: null,
    dateRange: []
  })
  advancedFiltersVisible.value = false
  handleSearch()
}

// 视图切换
const handleViewChange = () => {
  // removed debug log
}

// 刷新
const handleRefresh = () => {
  fetchCaseList()
}

// 选择变化
const handleSelectionChange = (selection) => {
  selectedCases.value = selection
}

// 分页变化
const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1 // 改变每页大小时重置到第一页
  fetchCaseList()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchCaseList()
}

// 新建案件
const handleCreate = () => {
  router.push('/case/create')
}

// 查看详情
const handleViewDetail = (row) => {
  router.push(`/case/${row.id}`)
}

// 编辑
const handleEdit = (row) => {
  router.push(`/case/${row.id}/edit`)
}

const handleArchive = row => router.push(`/case/${row.id}/archive`)

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该案件吗? 删除后可进入回收站恢复。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteCase(row.id)
    ElMessage.success('删除成功')
    fetchCaseList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 批量操作
const handleBatchAction = async (command) => {
  if ((command === 'changeOwner' && !canBatchChangeOwner.value)
    || (command === 'delete' && !canBatchDelete.value)) {
    ElMessage.warning('所选案件的状态或操作权限不满足该批量操作条件')
    return
  }
  switch (command) {
    case 'changeOwner':
      newOwnerId.value = null
      ownerDialogVisible.value = true
      break
    case 'delete':
      try {
        await ElMessageBox.confirm(`确定要删除选中的 ${selectedCases.value.length} 个案件吗？删除后可在回收站恢复。`, '批量删除', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        // 使用批量操作API
        const caseIds = selectedCases.value.map(c => c.id)
        await batchDeleteCases(caseIds)

        ElMessage.success(`成功删除 ${selectedCases.value.length} 个案件`)
        selectedCases.value = []
        fetchCaseList()
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('批量删除失败: ' + (error.message || '未知错误'))
        }
      }
      break
  }
}

const confirmOwnerChange = async () => {
  if (!newOwnerId.value) {
    ElMessage.warning('请选择主办律师')
    return
  }
  try {
    batchSubmitting.value = true
    const caseIds = selectedCases.value.map(item => item.id)
    await batchChangeOwner(caseIds, Number(newOwnerId.value))
    ElMessage.success(`成功修改 ${caseIds.length} 个案件的主办律师`)
    selectedCases.value = []
    ownerDialogVisible.value = false
    await fetchCaseList()
  } catch (error) {
    ElMessage.error('修改主办律师失败: ' + (error.message || '未知错误'))
  } finally {
    batchSubmitting.value = false
  }
}

onMounted(() => {
  // 检查是否有客户筛选参数
  if (route.query.clientId) {
    filterForm.clientId = route.query.clientId
    // 可选：在页面顶部显示筛选信息
    if (route.query.clientName) {
      ElMessage.info(`正在查看客户"${route.query.clientName}"的关联案件`)
    }
  }

  fetchCaseList()
  fetchDepartmentList()
  fetchLawyerList()
})
</script>

<style scoped lang="scss">
.case-list {
  .filter-section {
    margin-bottom: 12px;
    padding: 16px;
    background: #fff;
    border: 1px solid var(--zg-border-color, #e5e7eb);
    border-radius: 8px;

    .filter-form {
      display: grid;
      grid-template-columns: repeat(4, minmax(150px, 1fr));
      gap: 12px;

      :deep(.el-form-item) {
        min-width: 0;
        margin: 0;
      }

      :deep(.el-form-item__label) {
        height: auto;
        margin-bottom: 6px;
        color: #60646c;
        line-height: 20px;
      }

      .filter-control,
      .date-filter {
        width: 100%;
      }

      .filter-actions {
        align-self: end;

        :deep(.el-form-item__content) {
          flex-wrap: nowrap;
        }
      }

      .advanced-toggle {
        display: none;
      }
    }
  }

  .toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    margin-bottom: 12px;
    min-height: 40px;

    .toolbar-left {
      display: flex;
      flex-wrap: wrap;
      gap: 10px;
      align-items: center;
    }
  }

  .list-view {
    :deep(.data-table) {
      padding: 0;
    }

    .parties-cell {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;

      .party {
        flex: 1;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .vs {
        color: #999;
        font-size: 10px;
      }
    }

    .hearing-overdue {
      color: #f56c6c;
      font-weight: bold;
    }

    .hearing-urgent {
      color: #f56c6c;
    }

    .hearing-soon {
      color: #e6a23c;
    }

    .hearing-normal {
      color: #606266;
    }

    .text-muted {
      color: #999;
    }
  }

  .kanban-view {
    .kanban-board {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
      gap: 12px;
      overflow-x: auto;
      padding-bottom: 12px;

      .kanban-column {
        background-color: #f7f8fa;
        border: 1px solid var(--zg-border-color, #e5e7eb);
        border-radius: 8px;
        padding: 12px;
        min-height: 420px;
        display: flex;
        flex-direction: column;

        .column-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 15px;
          padding-bottom: 10px;
          border-bottom: 1px solid #e4e7ed;

          h3 {
            margin: 0;
            font-size: 14px;
            font-weight: 500;
            color: #333;
          }
        }

        .column-cards {
          flex: 1;
          display: flex;
          flex-direction: column;
          gap: 12px;
          overflow-y: auto;

          .case-card {
            background-color: #fff;
            border-radius: 6px;
            padding: 12px;
            border: 1px solid #e5e7eb;
            cursor: pointer;
            transition: border-color 0.2s, box-shadow 0.2s;

            &:hover {
              border-color: #b8c7dc;
              box-shadow: 0 2px 8px rgba(31, 41, 55, 0.08);
            }

            .card-header {
              display: flex;
              justify-content: flex-end;
              align-items: center;
              margin-bottom: 8px;
            }

            .card-title {
              font-size: 14px;
              font-weight: 500;
              color: #333;
              margin-bottom: 10px;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }

            .card-meta {
              display: flex;
              gap: 12px;
              margin-bottom: 8px;
              font-size: 12px;
              color: #666;

              .meta-item {
                display: flex;
                align-items: center;
                gap: 4px;
              }
            }

            .card-footer {
              display: flex;
              justify-content: space-between;
              align-items: center;

              .case-number {
                font-size: 12px;
                color: #999;
              }
            }
          }
        }
      }
    }
  }

  @media (max-width: 1100px) {
    .filter-section .filter-form {
      grid-template-columns: repeat(3, minmax(150px, 1fr));
    }
  }

  @media (max-width: 820px) {
    .filter-section .filter-form {
      grid-template-columns: repeat(2, minmax(140px, 1fr));
    }
  }

  @media (max-width: 600px) {
    .filter-section {
      padding: 12px;

      .filter-form {
        grid-template-columns: minmax(0, 1fr);

        .advanced-filter {
          display: none;
        }

        &.show-advanced .advanced-filter {
          display: flex;
        }

        .advanced-toggle {
          display: inline-flex;
        }

        .filter-actions :deep(.el-form-item__content) {
          flex-wrap: wrap;
          gap: 8px;

          .el-button {
            flex: 1;
            min-width: 92px;
            margin: 0;
          }
        }
      }
    }

    .toolbar {
      align-items: stretch;
      flex-direction: column;

      .toolbar-left,
      .toolbar-right,
      :deep(.el-radio-group) {
        width: 100%;
      }

      :deep(.el-radio-button) {
        flex: 1;
      }

      :deep(.el-radio-button__inner) {
        width: 100%;
      }

      .toolbar-right .el-button {
        width: 100%;
        margin: 0;
      }
    }
  }
}
</style>
