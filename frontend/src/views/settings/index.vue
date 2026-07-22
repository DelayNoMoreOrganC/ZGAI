<template>
  <div class="settings-page">
    <PageHeader title="系统设置" />

    <el-tabs v-model="activeTab" class="settings-tabs">
      <el-tab-pane label="运行状态" name="health">
        <section class="tab-panel health-panel" v-loading="healthLoading">
          <div class="section-heading">
            <div>
              <h3>系统运行状态</h3>
              <p>{{ healthCheckedAt || '正在读取运行状态' }}</p>
            </div>
            <div class="heading-actions">
              <el-tag :type="healthStatusType(systemHealth.status)" effect="plain">
                {{ healthStatusLabel(systemHealth.status) }}
              </el-tag>
              <el-button :icon="Refresh" :loading="healthLoading" circle title="刷新状态" @click="loadSystemHealth" />
            </div>
          </div>

          <div class="status-list">
            <div class="status-row">
              <div>
                <strong>业务数据库</strong>
                <span>{{ systemHealth.database?.product || '数据库' }}</span>
              </div>
              <el-tag :type="healthStatusType(systemHealth.database?.status)" effect="plain">
                {{ healthStatusLabel(systemHealth.database?.status) }}
              </el-tag>
            </div>
            <div v-for="item in storageHealthItems" :key="item.key" class="status-row">
              <div>
                <strong>{{ item.label }}</strong>
                <span>{{ storageDescription(item) }}</span>
              </div>
              <el-tag :type="healthStatusType(item.status)" effect="plain">
                {{ healthStatusLabel(item.status) }}
              </el-tag>
            </div>
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="用户管理" name="users">
        <section class="tab-panel">
          <div class="toolbar">
            <el-input
              v-model="userFilters.keyword"
              placeholder="姓名或账号"
              clearable
              class="search-input"
              @keyup.enter="handleSearchUsers"
              @clear="handleSearchUsers"
            >
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
            <el-select v-model="userFilters.departmentId" placeholder="所属部门" clearable filterable>
              <el-option
                v-for="department in departmentOptions"
                :key="department.id"
                :label="department.deptName"
                :value="department.id"
              />
            </el-select>
            <el-select v-model="userFilters.status" placeholder="账号状态" clearable>
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
            <el-button type="primary" :icon="Search" @click="handleSearchUsers">查询</el-button>
            <el-button :icon="Plus" @click="handleCreateUser">新建账号</el-button>
          </div>

          <el-table :data="userList" v-loading="userLoading" empty-text="暂无用户" stripe>
            <el-table-column prop="realName" label="姓名" width="110" />
            <el-table-column prop="username" label="账号" width="130" />
            <el-table-column prop="departmentName" label="所属部门" min-width="150" />
            <el-table-column prop="position" label="身份类别" width="110" />
            <el-table-column label="角色" min-width="180">
              <template #default="{ row }">
                <div class="tag-list">
                  <el-tag v-for="role in row.roles || []" :key="role" size="small" effect="plain">
                    {{ role }}
                  </el-tag>
                  <span v-if="!row.roles?.length" class="muted">未分配</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="82">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">
                  {{ row.status === 1 ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="270" fixed="right">
              <template #default="{ row }">
                <template v-if="!isLockedAccount(row)">
                  <el-button link type="primary" @click="handleEditUser(row)">编辑</el-button>
                  <el-button link type="warning" @click="handleResetPassword(row)">重置密码</el-button>
                  <el-button link @click="handleToggleUserStatus(row)">
                    {{ row.status === 1 ? '停用' : '启用' }}
                  </el-button>
                  <el-button link type="danger" @click="handleDeleteUser(row)">删除</el-button>
                </template>
                <el-tag v-else size="small" type="info" effect="plain">受保护账号</el-tag>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination
              v-model:current-page="userPagination.page"
              v-model:page-size="userPagination.size"
              :page-sizes="[20, 50, 100]"
              :total="userPagination.total"
              layout="total, sizes, prev, pager, next"
              @current-change="loadUsers"
              @size-change="handleUserPageSize"
            />
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="角色权限" name="roles">
        <section class="tab-panel" v-loading="roleLoading">
          <div class="section-heading compact-heading">
            <div>
              <h3>身份权限基线</h3>
              <p>共 {{ roleList.length }} 个有效角色</p>
            </div>
            <el-button :icon="Refresh" circle title="刷新角色" @click="loadRoles" />
          </div>
          <el-table :data="roleList" empty-text="暂无角色" stripe>
            <el-table-column prop="roleName" label="角色名称" width="140" />
            <el-table-column prop="roleCode" label="角色代码" width="170" />
            <el-table-column label="类型" width="95">
              <template #default="{ row }">
                <el-tag :type="row.systemRole ? 'info' : 'primary'" effect="plain">
                  {{ row.systemRole ? '系统' : '自定义' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="userCount" label="人数" width="75" align="center" />
            <el-table-column label="权限" min-width="360">
              <template #default="{ row }">
                <div class="tag-list permission-tags">
                  <el-tag v-for="permission in row.permissions || []" :key="permission" size="small" effect="plain">
                    {{ permissionLabel(permission) }}
                  </el-tag>
                  <span v-if="!row.permissions?.length" class="muted">未配置权限</span>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </el-tab-pane>

      <el-tab-pane label="数据范围" name="scope">
        <section class="tab-panel scope-panel">
          <div v-for="rule in dataScopeRules" :key="rule.name" class="scope-row">
            <div>
              <strong>{{ rule.name }}</strong>
              <span>{{ rule.description }}</span>
            </div>
            <el-tag effect="plain">{{ rule.scope }}</el-tag>
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="操作日志" name="logs">
        <section class="tab-panel">
          <div class="toolbar">
            <el-date-picker
              v-model="logFilters.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
            />
            <el-select v-model="logFilters.module" placeholder="操作模块" clearable filterable>
              <el-option v-for="module in auditModules" :key="module" :label="moduleLabel(module)" :value="module" />
            </el-select>
            <el-button type="primary" :icon="Search" @click="handleSearchLogs">查询</el-button>
            <el-button :icon="Refresh" @click="loadAuditLogs">刷新</el-button>
          </div>

          <el-table :data="auditLogs" v-loading="logLoading" empty-text="暂无审计记录" stripe>
            <el-table-column label="时间" width="175">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
            <el-table-column prop="userName" label="操作人" width="110" />
            <el-table-column label="模块" min-width="150">
              <template #default="{ row }">{{ moduleLabel(row.module) }}</template>
            </el-table-column>
            <el-table-column prop="operation" label="操作" min-width="150" />
            <el-table-column label="结果" width="82">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="plain">
                  {{ row.status === 1 ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="耗时" width="90">
              <template #default="{ row }">{{ row.executionTime ?? 0 }} ms</template>
            </el-table-column>
            <el-table-column prop="ip" label="来源 IP" width="140" />
            <el-table-column label="操作" width="75" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openAuditDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination">
            <el-pagination
              v-model:current-page="logPagination.page"
              :page-size="logPagination.size"
              :total="logPagination.total"
              layout="total, prev, pager, next"
              @current-change="loadAuditLogs"
            />
          </div>
        </section>
      </el-tab-pane>

      <el-tab-pane label="AI 配置" name="ai">
        <section class="tab-panel"><AIConfigPanel /></section>
      </el-tab-pane>

      <el-tab-pane label="数据备份" name="backup">
        <section class="tab-panel" v-loading="backupLoading">
          <div class="metric-strip">
            <div><span>上次备份</span><strong>{{ lastBackupTime }}</strong></div>
            <div><span>文件大小</span><strong>{{ backupSize }}</strong></div>
            <div><span>保留周期</span><strong>{{ retentionDays }} 天</strong></div>
          </div>

          <div class="section-heading compact-heading backup-heading">
            <div>
              <h3>备份历史</h3>
              <p>自动备份计划：每天 02:00</p>
            </div>
            <el-button type="primary" :icon="Download" :loading="backingUp" @click="handleBackupNow">
              立即备份
            </el-button>
          </div>

          <el-table :data="backupList" empty-text="暂无备份记录" stripe>
            <el-table-column prop="fileName" label="文件名" min-width="230" />
            <el-table-column label="大小" width="110">
              <template #default="{ row }">{{ formatBytes(row.fileSize) }}</template>
            </el-table-column>
            <el-table-column label="备份时间" width="180">
              <template #default="{ row }">{{ formatDateTime(row.backupTime) }}</template>
            </el-table-column>
            <el-table-column label="类型" width="90">
              <template #default="{ row }">{{ row.backupType === 'AUTO' ? '自动' : '手动' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.backupStatus === 'SUCCESS' ? 'success' : 'danger'" effect="plain">
                  {{ row.backupStatus === 'SUCCESS' ? '成功' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button
                  link
                  type="primary"
                  :disabled="row.backupStatus !== 'SUCCESS'"
                  @click="handleRestoreBackup(row)"
                >恢复</el-button>
                <el-button link type="danger" @click="handleDeleteBackup(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </section>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="userDialogVisible" :title="userForm.id ? '编辑账号' : '新建账号'" width="680px">
      <el-form ref="userFormRef" :model="userForm" :rules="userRules" label-width="90px">
        <el-row :gutter="18">
          <el-col :xs="24" :sm="12">
            <el-form-item label="账号" prop="username">
              <el-input v-model="userForm.username" :disabled="!!userForm.id" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="姓名" prop="realName"><el-input v-model="userForm.realName" /></el-form-item>
          </el-col>
        </el-row>
        <el-row v-if="!userForm.id" :gutter="18">
          <el-col :span="24">
            <el-form-item label="初始密码" prop="password">
              <el-input v-model="userForm.password" type="password" show-password autocomplete="new-password" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="18">
          <el-col :xs="24" :sm="12">
            <el-form-item label="联系电话" prop="phone"><el-input v-model="userForm.phone" /></el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="邮箱" prop="email"><el-input v-model="userForm.email" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="18">
          <el-col :xs="24" :sm="12">
            <el-form-item label="所属部门" prop="departmentId">
              <el-select v-model="userForm.departmentId" filterable style="width: 100%">
                <el-option
                  v-for="department in departmentOptions"
                  :key="department.id"
                  :label="department.deptName"
                  :value="department.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="身份类别" prop="position">
              <el-select v-model="userForm.position" filterable allow-create style="width: 100%">
                <el-option v-for="identity in identityOptions" :key="identity" :label="identity" :value="identity" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="userForm.roleIds" multiple filterable style="width: 100%">
            <el-option v-for="role in roleList" :key="role.id" :label="role.roleName" :value="role.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="账号状态" prop="status">
          <el-radio-group v-model="userForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="userSaving" @click="handleSaveUser">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="auditDetailVisible" title="审计记录" width="560px">
      <el-descriptions v-if="selectedAuditLog" :column="1" border>
        <el-descriptions-item label="时间">{{ formatDateTime(selectedAuditLog.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ selectedAuditLog.userName }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ moduleLabel(selectedAuditLog.module) }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ selectedAuditLog.operation }}</el-descriptions-item>
        <el-descriptions-item label="接口方法">{{ selectedAuditLog.method || '-' }}</el-descriptions-item>
        <el-descriptions-item label="来源 IP">{{ selectedAuditLog.ip || '-' }}</el-descriptions-item>
        <el-descriptions-item label="执行结果">{{ selectedAuditLog.status === 1 ? '成功' : '失败' }}</el-descriptions-item>
        <el-descriptions-item label="执行耗时">{{ selectedAuditLog.executionTime ?? 0 }} ms</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Plus, Refresh, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import AIConfigPanel from '@/components/AIConfigPanel.vue'
import { useUserStore } from '@/stores/user'
import {
  createUser,
  deleteUser,
  getUserList,
  resetPassword,
  toggleUserStatus,
  updateUser
} from '@/api/user'
import { getRoleList } from '@/api/role'
import { getDepartmentList } from '@/api/department'
import {
  createBackup,
  deleteBackup,
  getAuditLogDetail,
  getAuditLogModules,
  getAuditLogs,
  getBackups,
  getSystemHealthDetails,
  restoreBackup
} from '@/api/system'

const userStore = useUserStore()
const activeTab = ref('health')

const healthLoading = ref(false)
const systemHealth = ref({})
const healthCheckedAt = ref('')

const userLoading = ref(false)
const userSaving = ref(false)
const userList = ref([])
const departmentOptions = ref([])
const roleLoading = ref(false)
const roleList = ref([])
const userFilters = reactive({ keyword: '', departmentId: null, status: null })
const userPagination = reactive({ page: 1, size: 20, total: 0 })
const userDialogVisible = ref(false)
const userFormRef = ref(null)
const identityOptions = [
  '主任', '部门主管', '行政管理', '行政管理1', '行政管理2',
  '财务管理', '出纳', '律师', '实习律师', '律师助理', '助理'
]

const emptyUserForm = () => ({
  id: null,
  username: '',
  password: '',
  realName: '',
  phone: '',
  email: '',
  departmentId: null,
  position: '',
  roleIds: [],
  status: 1
})
const userForm = ref(emptyUserForm())
const userRules = {
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 2, max: 50, message: '账号长度为 2 至 50 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入初始密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度为 6 至 100 个字符', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  phone: [{ pattern: /^[+()\d\s-]{7,20}$/, message: '请输入手机号码或区号+固话', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  departmentId: [{ required: true, message: '请选择所属部门', trigger: 'change' }],
  position: [{ required: true, message: '请选择身份类别', trigger: 'change' }],
  roleIds: [{ type: 'array', required: true, min: 1, message: '请至少选择一个角色', trigger: 'change' }]
}

const dataScopeRules = [
  { name: '案件数据', description: '律师及部门主管按所属部门查看；行政可查看案件整体信息；主任查看全部。', scope: '部门 / 全所' },
  { name: '客户数据', description: '按案源人或承办人所属部门过滤；授权人员和主任可查看全部客户。', scope: '部门 / 授权' },
  { name: '利冲检查', description: '利冲核对覆盖全所客户和案件主体，不受日常客户列表范围限制。', scope: '全所' },
  { name: '案件文件', description: '文件访问继承案件可见范围，案件私密材料默认不进入 AI 知识库。', scope: '随案件' },
  { name: '开票记录', description: '发起人查看本人申请，财务人员按处理权限查看待办和反馈记录。', scope: '本人 / 财务' }
]

const logLoading = ref(false)
const auditLogs = ref([])
const auditModules = ref([])
const logFilters = reactive({ dateRange: [], module: '' })
const logPagination = reactive({ page: 1, size: 20, total: 0 })
const auditDetailVisible = ref(false)
const selectedAuditLog = ref(null)

const backupLoading = ref(false)
const backingUp = ref(false)
const backupList = ref([])
const retentionDays = ref(180)
const lastBackupTime = computed(() => backupList.value.length
  ? formatDateTime(backupList.value[0].backupTime)
  : '暂无备份')
const backupSize = computed(() => backupList.value.length
  ? formatBytes(backupList.value[0].fileSize)
  : '-')

const storageHealthItems = computed(() => {
  const storage = systemHealth.value.storage || {}
  const labels = { caseLibrary: '案件文件库', knowledgeLibrary: '知识库原件', backup: '数据库备份' }
  return ['caseLibrary', 'knowledgeLibrary', 'backup']
    .map(key => ({ key, label: labels[key], ...(storage[key] || {}) }))
})

const permissionLabels = {
  CASE_CREATE: '新建立案', CASE_VIEW: '查看案件', CASE_EDIT: '编辑案件', CASE_DELETE: '删除案件', CASE_ARCHIVE: '归档案件',
  CASE_IMPORT: '导入案件', CASE_EXPORT: '导出案件', CLIENT_CREATE: '新建客户', CLIENT_VIEW: '查看客户', CLIENT_EDIT: '编辑客户',
  CLIENT_DELETE: '删除客户', CLIENT_IMPORT: '导入客户', CLIENT_EXPORT: '导出客户', DOCUMENT_VIEW: '查看文档', DOCUMENT_EDIT: '编辑文档',
  DOCUMENT_DELETE: '删除文档', APPROVAL_VIEW: '查看审批', APPROVAL_EDIT: '处理审批', APPROVAL_DELETE: '删除审批', TODO_VIEW: '查看待办',
  TODO_EDIT: '编辑待办', TODO_DELETE: '删除待办', FINANCE_VIEW: '查看财务', FINANCE_EDIT: '处理财务', USER_VIEW: '查看用户',
  USER_EDIT: '编辑用户', ROLE_VIEW: '查看角色', ROLE_EDIT: '编辑角色', AI_CONFIG: 'AI 配置', SYSTEM_CONFIG: '系统配置',
  WORK_REPORT_REVIEW: '审核工作报告', KNOWLEDGE_DELETE: '删除知识', STATISTICS_VIEW: '查看统计', STATISTICS_EXPORT: '导出统计'
}
const moduleLabels = {
  BackupController: '数据备份', UserController: '用户管理', RoleController: '角色权限',
  CaseController: '案件管理', ClientController: '客户管理', ApprovalController: '审批管理',
  CaseDocumentController: '案件文件', FinanceController: '财务管理'
}

const loadSystemHealth = async () => {
  healthLoading.value = true
  try {
    const response = await getSystemHealthDetails()
    systemHealth.value = response.data || {}
    healthCheckedAt.value = formatDateTime(systemHealth.value.time || new Date())
  } catch {
    systemHealth.value = { status: 'unavailable' }
  } finally {
    healthLoading.value = false
  }
}

const loadDepartments = async () => {
  try {
    const response = await getDepartmentList()
    departmentOptions.value = response.data || []
  } catch {
    departmentOptions.value = []
  }
}

const loadRoles = async () => {
  roleLoading.value = true
  try {
    const response = await getRoleList({ page: 1, size: 200 })
    roleList.value = response.data?.content || []
  } catch {
    roleList.value = []
  } finally {
    roleLoading.value = false
  }
}

const loadUsers = async (page = userPagination.page) => {
  userPagination.page = Number(page) || 1
  userLoading.value = true
  try {
    const response = await getUserList({
      page: userPagination.page,
      size: userPagination.size,
      keyword: userFilters.keyword || undefined,
      departmentId: userFilters.departmentId || undefined,
      status: userFilters.status ?? undefined
    })
    userList.value = response.data?.content || []
    userPagination.total = response.data?.totalElements || 0
  } catch {
    userList.value = []
    userPagination.total = 0
  } finally {
    userLoading.value = false
  }
}

const handleSearchUsers = () => loadUsers(1)
const handleUserPageSize = () => loadUsers(1)
const handleCreateUser = () => {
  userForm.value = emptyUserForm()
  userDialogVisible.value = true
}
const handleEditUser = (user) => {
  userForm.value = {
    id: user.id,
    username: user.username,
    password: '',
    realName: user.realName || '',
    phone: user.phone || '',
    email: user.email || '',
    departmentId: user.departmentId,
    position: user.position || '',
    roleIds: [...(user.roleIds || [])],
    status: user.status
  }
  userDialogVisible.value = true
}

const handleSaveUser = async () => {
  await userFormRef.value?.validate()
  userSaving.value = true
  try {
    const common = {
      realName: userForm.value.realName,
      phone: userForm.value.phone || null,
      email: userForm.value.email || null,
      departmentId: userForm.value.departmentId,
      position: userForm.value.position,
      roleIds: userForm.value.roleIds,
      status: userForm.value.status
    }
    if (userForm.value.id) {
      await updateUser(userForm.value.id, common)
      ElMessage.success('账号已更新')
    } else {
      await createUser({ ...common, username: userForm.value.username, password: userForm.value.password })
      ElMessage.success('账号已创建')
    }
    userDialogVisible.value = false
    await Promise.all([loadUsers(), loadRoles()])
  } catch (error) {
    if (!isCancel(error)) ElMessage.error('保存账号失败')
  } finally {
    userSaving.value = false
  }
}

const handleResetPassword = async (user) => {
  try {
    const { value } = await ElMessageBox.prompt(`请输入“${user.realName}”的新密码`, '重置密码', {
      inputType: 'password',
      inputPattern: /^.{6,100}$/,
      inputErrorMessage: '密码长度为 6 至 100 个字符',
      confirmButtonText: '确认重置',
      cancelButtonText: '取消'
    })
    await resetPassword(user.id, { newPassword: value })
    ElMessage.success('密码已重置')
  } catch (error) {
    if (!isCancel(error)) ElMessage.error('重置密码失败')
  }
}

const handleToggleUserStatus = async (user) => {
  const nextStatus = user.status === 1 ? 0 : 1
  const action = nextStatus === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确定${action}账号“${user.realName}”吗？`, `${action}账号`, {
      type: 'warning', confirmButtonText: action, cancelButtonText: '取消'
    })
    await toggleUserStatus(user.id, nextStatus)
    ElMessage.success(`账号已${action}`)
    await loadUsers()
  } catch (error) {
    if (!isCancel(error)) ElMessage.error(`${action}账号失败`)
  }
}

const handleDeleteUser = async (user) => {
  try {
    await ElMessageBox.confirm(`确定删除账号“${user.realName}”吗？`, '删除账号', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
    await deleteUser(user.id)
    ElMessage.success('账号已删除')
    await Promise.all([loadUsers(), loadRoles()])
  } catch (error) {
    if (!isCancel(error)) ElMessage.error('删除账号失败')
  }
}

const isLockedAccount = (user) => {
  const username = String(user.username || '').toLowerCase()
  return ['admin', 'amin'].includes(username) || String(user.id) === String(userStore.userId)
}

const loadAuditModules = async () => {
  try {
    const response = await getAuditLogModules()
    auditModules.value = response.data || []
  } catch {
    auditModules.value = []
  }
}

const loadAuditLogs = async (page = logPagination.page) => {
  logPagination.page = Number(page) || 1
  logLoading.value = true
  try {
    const response = await getAuditLogs({
      page: logPagination.page,
      size: logPagination.size,
      module: logFilters.module || undefined,
      startDate: logFilters.dateRange?.[0] || undefined,
      endDate: logFilters.dateRange?.[1] || undefined
    })
    auditLogs.value = response.data?.content || []
    logPagination.total = response.data?.totalElements || 0
  } catch {
    auditLogs.value = []
    logPagination.total = 0
  } finally {
    logLoading.value = false
  }
}

const handleSearchLogs = () => loadAuditLogs(1)
const openAuditDetail = async (log) => {
  try {
    const response = await getAuditLogDetail(log.id)
    selectedAuditLog.value = response.data
    auditDetailVisible.value = true
  } catch {
    ElMessage.error('读取审计记录失败')
  }
}

const loadBackups = async () => {
  backupLoading.value = true
  try {
    const response = await getBackups()
    backupList.value = response.data || []
    retentionDays.value = backupList.value[0]?.retentionDays || 180
  } catch {
    backupList.value = []
  } finally {
    backupLoading.value = false
  }
}

const handleBackupNow = async () => {
  backingUp.value = true
  try {
    await createBackup()
    ElMessage.success('备份完成')
    await loadBackups()
  } catch {
    ElMessage.error('备份失败')
  } finally {
    backingUp.value = false
  }
}

const handleRestoreBackup = async (backup) => {
  try {
    await ElMessageBox.prompt(`恢复“${backup.fileName}”将覆盖当前数据库。请输入 RESTORE 继续。`, '恢复备份', {
      inputPattern: /^RESTORE$/,
      inputErrorMessage: '请输入 RESTORE',
      confirmButtonText: '确定恢复',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await restoreBackup(backup.id)
    ElMessage.success('备份恢复成功，系统将重新加载')
    setTimeout(() => window.location.reload(), 1500)
  } catch (error) {
    if (!isCancel(error)) ElMessage.error('恢复备份失败')
  }
}

const handleDeleteBackup = async (backup) => {
  try {
    await ElMessageBox.confirm(`确定删除备份“${backup.fileName}”吗？`, '删除备份', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
    await deleteBackup(backup.id)
    ElMessage.success('备份已删除')
    await loadBackups()
  } catch (error) {
    if (!isCancel(error)) ElMessage.error('删除备份失败')
  }
}

const healthStatusLabel = status => ({
  ready: '正常', degraded: '部分降级', readonly: '只读', missing: '目录缺失', unavailable: '不可用'
}[status] || '待检查')
const healthStatusType = status => ({
  ready: 'success', degraded: 'warning', readonly: 'warning', missing: 'danger', unavailable: 'danger'
}[status] || 'info')
const permissionLabel = code => permissionLabels[code] || code
const moduleLabel = module => moduleLabels[module] || module || '-'
const storageDescription = item => {
  if (item.status === 'missing') return '配置目录不存在'
  if (item.status === 'unavailable') return '当前无法读取目录状态'
  const storageType = item.storageClass === 'network' ? '网络存储' : '本机存储'
  return `${storageType} · 可用 ${formatBytes(item.usableBytes)}`
}
const formatBytes = bytes => {
  const value = Number(bytes)
  if (!Number.isFinite(value) || value < 0) return '容量未知'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let size = value
  let index = 0
  while (size >= 1024 && index < units.length - 1) {
    size /= 1024
    index += 1
  }
  return `${size.toFixed(index > 2 ? 1 : 0)} ${units[index]}`
}
const formatDateTime = value => {
  if (!value) return '-'
  const date = Array.isArray(value)
    ? new Date(value[0], (value[1] || 1) - 1, value[2] || 1, value[3] || 0, value[4] || 0, value[5] || 0)
    : new Date(value)
  return Number.isNaN(date.getTime()) ? '-' : date.toLocaleString('zh-CN')
}
const isCancel = error => error === 'cancel' || error === 'close'

watch(activeTab, tab => {
  if (tab === 'users' && !userList.value.length) loadUsers(1)
  if (tab === 'roles') loadRoles()
  if (tab === 'logs') Promise.all([loadAuditModules(), loadAuditLogs(1)])
  if (tab === 'backup') loadBackups()
})

onMounted(() => {
  loadSystemHealth()
  Promise.all([loadDepartments(), loadRoles()])
})
</script>

<style scoped lang="scss">
.settings-page {
  .settings-tabs {
    padding: 18px 20px 24px;
    background: #fff;
    border: 1px solid #e7e9ed;
    border-radius: 6px;
  }

  .tab-panel {
    min-height: 260px;
  }

  .health-panel {
    max-width: 960px;
  }

  .section-heading,
  .status-row,
  .scope-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20px;
  }

  .section-heading {
    padding-bottom: 18px;
    border-bottom: 1px solid #e7e9ed;

    h3,
    p {
      margin: 0;
    }

    h3 {
      color: #1f2329;
      font-size: 17px;
      font-weight: 600;
    }

    p {
      margin-top: 5px;
      color: #72777f;
      font-size: 13px;
    }
  }

  .compact-heading {
    margin-bottom: 16px;
  }

  .heading-actions {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .status-row,
  .scope-row {
    min-height: 70px;
    border-bottom: 1px solid #eef0f2;

    strong,
    span {
      display: block;
    }

    strong {
      color: #24262b;
      font-size: 14px;
      font-weight: 600;
    }

    span {
      margin-top: 5px;
      color: #737982;
      font-size: 13px;
      line-height: 1.5;
    }
  }

  .scope-panel {
    max-width: 960px;
  }

  .toolbar {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    margin-bottom: 16px;

    .search-input {
      width: 220px;
    }

    :deep(.el-select) {
      width: 170px;
    }
  }

  .tag-list {
    display: flex;
    flex-wrap: wrap;
    gap: 5px;
  }

  .permission-tags {
    padding: 5px 0;
  }

  .muted {
    color: #9499a1;
    font-size: 13px;
  }

  .pagination {
    display: flex;
    justify-content: flex-end;
    margin-top: 18px;
  }

  .metric-strip {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    margin-bottom: 22px;
    border-bottom: 1px solid #e7e9ed;

    > div {
      padding: 4px 20px 20px 0;
    }

    span,
    strong {
      display: block;
    }

    span {
      color: #737982;
      font-size: 13px;
    }

    strong {
      margin-top: 7px;
      color: #24262b;
      font-size: 17px;
      font-weight: 600;
    }
  }

  @media (max-width: 760px) {
    .settings-tabs {
      padding: 12px;
    }

    .metric-strip {
      grid-template-columns: 1fr;
    }

    .section-heading,
    .scope-row {
      align-items: flex-start;
      flex-direction: column;
    }
  }
}
</style>
