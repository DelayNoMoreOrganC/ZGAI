<template>
  <div class="client">
    <PageHeader title="客户管理">
      <template #extra>
        <el-button v-if="canCreate" type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新建客户
        </el-button>
      </template>
    </PageHeader>

    <!-- 搜索筛选区 -->
    <div class="filter-section">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索客户名称、案源人、承办人..."
        clearable
        class="search-input"
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <el-select v-model="filterType" placeholder="客户类型" clearable class="filter-select">
        <el-option
          v-for="type in clientTypeOptions"
          :key="type"
          :label="type"
          :value="type"
        />
      </el-select>

      <el-select v-model="filterDepartmentId" placeholder="所属部门" clearable filterable class="filter-select">
        <el-option
          v-for="department in departmentOptions"
          :key="department.id"
          :label="department.deptName"
          :value="department.id"
        />
      </el-select>

      <el-button type="primary" @click="handleSearch" class="search-btn">
        <el-icon><Search /></el-icon>
        搜索
      </el-button>
      <el-button @click="handleReset" class="reset-btn">重置</el-button>
    </div>

    <!-- 客户列表 -->
    <DataTable
      :table-data="clientList"
      :loading="loading"
      :total="total"
      :current-page="currentPage"
      :page-size="pageSize"
      :show-index="false"
      :show-actions="true"
      :action-width="actionWidth"
      :show-empty-action="canCreate && !hasActiveFilters"
      empty-action-text="新建客户"
      empty-text="暂无符合条件的客户"
      @size-change="handleSizeChange"
      @current-change="handleCurrentChange"
      @empty-action="handleCreate"
    >
      <el-table-column prop="name" label="客户名称" width="200">
        <template #default="{ row }">
          <el-link type="primary" @click="handleViewDetail(row)">
            {{ row.name }}
          </el-link>
        </template>
      </el-table-column>

      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="getClientTypeTag(row.type)" size="small" effect="plain">
            {{ row.type || '-' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="sourceUserNames" label="案源人" width="150">
        <template #default="{ row }">
          {{ row.sourceUserNames || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="clientOwnerNames" label="承办人" width="150">
        <template #default="{ row }">
          {{ row.clientOwnerNames || row.ownerName || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="caseCount" label="关联案件" width="100">
        <template #default="{ row }">
          <el-link v-if="row.caseCount > 0" type="primary" @click="handleViewRelatedCases(row)">
            {{ row.caseCount }} 个案件
          </el-link>
          <span v-else>0 个案件</span>
        </template>
      </el-table-column>
      <el-table-column prop="departmentName" label="所属部门" width="130">
        <template #default="{ row }">
          <el-tag v-if="row.departmentName" type="info" effect="plain">{{ row.departmentName }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160" sortable />
      <template #actions="{ row }">
        <el-button link type="primary" size="small" @click="handleViewDetail(row)">
          详情
        </el-button>
        <el-button v-if="canEditRow(row)" link type="primary" size="small" @click="handleEdit(row)">
          编辑
        </el-button>
        <el-button v-if="canDeleteRow(row)" link type="danger" size="small" @click="handleDelete(row)">
          删除
        </el-button>
      </template>
    </DataTable>

  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import DataTable from '@/components/DataTable.vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import {
  getClientList,
  deleteClient
} from '@/api/client'
import { getDepartmentList } from '@/api/department'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const searchKeyword = ref('')
const filterType = ref('')
const filterDepartmentId = ref('')
const departmentOptions = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const clientList = ref([])
const clientTypeOptions = ['个人', '企业', '金融机构', '事业单位', '党政机关', '社会团体', '其他']

const canCreate = computed(() => userStore.hasPermission('CLIENT_CREATE'))
const canEdit = computed(() => userStore.hasPermission('CLIENT_EDIT'))
const canDelete = computed(() => userStore.hasPermission('CLIENT_DELETE'))
const hasActiveFilters = computed(() => Boolean(
  searchKeyword.value.trim() || filterType.value || filterDepartmentId.value
))
const actionWidth = computed(() => 72 + (canEdit.value ? 52 : 0) + (canDelete.value ? 52 : 0))

const isPrivilegedEditor = computed(() => {
  const username = userStore.userInfo?.username
  const position = userStore.userInfo?.position
  return username === 'admin' || position === '主任'
})

const parseUserIds = (value) => String(value || '')
  .split(',')
  .map(id => Number(id.trim()))
  .filter(Number.isFinite)

const isRelatedUser = (client) => {
  const currentUserId = Number(userStore.userId)
  if (!Number.isFinite(currentUserId)) return false
  return [
    ...parseUserIds(client.sourceUserIds),
    ...parseUserIds(client.clientOwnerIds),
    Number(client.ownerId)
  ].some(id => Number.isFinite(id) && id === currentUserId)
}

const canEditRow = (client) => canEdit.value && (isPrivilegedEditor.value || isRelatedUser(client))
const canDeleteRow = (client) => canDelete.value && (isPrivilegedEditor.value || isRelatedUser(client))

const normalizeClient = (client = {}) => ({
  ...client,
  name: client.clientName || client.name || '',
  type: client.clientType || client.type || '',
  contact: client.legalRepresentative || client.clientName || client.contact || '',
  sourceUserNames: client.sourceUserNames || '',
  clientOwnerNames: client.clientOwnerNames || client.ownerName || '',
  departmentId: client.departmentId || null,
  departmentName: client.departmentName || '',
  createTime: formatDate(client.createdAt),
  remark: client.notes || client.remark || ''
})

const normalizeClients = (clients = []) => clients.map(normalizeClient)

const formatDate = (value) => {
  if (!value) return ''
  if (Array.isArray(value)) {
    const [year, month, day, hour, minute] = value
    const date = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
    return hour == null ? date : `${date} ${String(hour).padStart(2, '0')}:${String(minute || 0).padStart(2, '0')}`
  }
  return String(value).replace('T', ' ').slice(0, 16)
}

// 加载客户列表
const fetchClientList = async () => {
  try {
    loading.value = true
    const params = {
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value.trim() || undefined,
      clientType: filterType.value || undefined,
      departmentId: filterDepartmentId.value || undefined
    }
    const res = await getClientList(params)
    const pageData = res.data || {}
    clientList.value = normalizeClients(pageData.records || [])
    total.value = pageData.total || 0
  } catch (error) {
    clientList.value = []
    total.value = 0
    console.error('加载客户列表失败:', error)
    ElMessage.error('加载客户列表失败')
  } finally {
    loading.value = false
  }
}

// 页面加载时获取数据
onMounted(() => {
  loadDepartments()
  fetchClientList()
})

const loadDepartments = async () => {
  try {
    const res = await getDepartmentList()
    departmentOptions.value = res.data || []
  } catch (error) {
    console.error('加载部门列表失败:', error)
  }
}

const handleCreate = () => {
  router.push('/client/create')
}

const handleSearch = () => {
  currentPage.value = 1
  fetchClientList()
}

const handleReset = () => {
  searchKeyword.value = ''
  filterType.value = ''
  filterDepartmentId.value = ''
  currentPage.value = 1
  fetchClientList()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  currentPage.value = 1
  fetchClientList()
}

const handleCurrentChange = (page) => {
  currentPage.value = page
  fetchClientList()
}

const getClientTypeTag = (type) => {
  if (type === '个人') return 'primary'
  if (type === '金融机构') return 'warning'
  if (type === '党政机关' || type === '事业单位') return 'info'
  return 'success'
}

const handleViewDetail = (client) => {
  router.push(`/client/${client.id}`)
}

// 查看关联案件
const handleViewRelatedCases = (client) => {
  // 跳转到案件列表，并自动筛选该客户的案件
  router.push({
    path: '/case/list',
    query: { clientId: client.id, clientName: client.name }
  })
}

const handleEdit = (client) => {
  router.push(`/client/${client.id}/edit`)
}

const handleDelete = async (client) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除客户"${client.name}"吗？存在关联案件时不能删除。`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteClient(client.id)
    ElMessage.success('删除成功')
    if (clientList.value.length === 1 && currentPage.value > 1) {
      currentPage.value -= 1
    }
    fetchClientList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除客户失败:', error)
      ElMessage.error('删除客户失败')
    }
  }
}

</script>

<style scoped lang="scss">
.client {
  .filter-section {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 16px;
    padding: 16px;
    background: #fff;
    border: 1px solid var(--zg-border-color, #e5e7eb);
    border-radius: 8px;
    flex-wrap: wrap;

    .search-input {
      width: min(340px, 100%);
    }

    .filter-select {
      width: 160px;
    }
  }

  :deep(.data-table) {
    padding: 0;
  }

  @media (max-width: 760px) {
    .filter-section {
      align-items: stretch;
      padding: 12px;

      .search-input,
      .filter-select {
        width: 100%;
      }

      .search-btn,
      .reset-btn {
        flex: 1;
      }
    }
  }
}
</style>
