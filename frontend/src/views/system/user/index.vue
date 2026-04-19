<template>
  <div class="user-management">
    <PageHeader title="用户管理">
      <template #extra>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增用户
        </el-button>
      </template>
    </PageHeader>

    <!-- 筛选器 -->
    <div class="filter-bar">
      <el-input
        v-model="filters.keyword"
        placeholder="搜索用户名或真实姓名"
        clearable
        style="width: 200px; margin-right: 10px"
        @keyup.enter="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <el-select
        v-model="filters.departmentId"
        placeholder="选择部门"
        clearable
        style="width: 150px; margin-right: 10px"
      >
        <el-option label="全部部门" :value="null" />
        <el-option label="管理层" :value="1" />
        <el-option label="民事部" :value="2" />
        <el-option label="刑事部" :value="3" />
        <el-option label="行政部" :value="4" />
      </el-select>

      <el-select
        v-model="filters.status"
        placeholder="选择状态"
        clearable
        style="width: 120px; margin-right: 10px"
      >
        <el-option label="全部状态" :value="null" />
        <el-option label="启用" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>

      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <!-- 用户列表 -->
    <el-table :data="userList" border v-loading="loading" stripe>
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="realName" label="真实姓名" width="120" />
      <el-table-column prop="gender" label="性别" width="60">
        <template #default="{ row }">
          {{ row.gender === 'MALE' ? '男' : row.gender === 'FEMALE' ? '女' : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="phone" label="手机号" width="130" />
      <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
      <el-table-column prop="departmentName" label="部门" width="120" />
      <el-table-column label="角色" width="150">
        <template #default="{ row }">
          <el-tag
            v-for="role in row.roles"
            :key="role.id"
            size="small"
            style="margin-right: 5px"
          >
            {{ role.roleName }}
          </el-tag>
          <span v-if="!row.roles || row.roles.length === 0" style="color: #999">未分配</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="160">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">
            编辑
          </el-button>
          <el-button link type="warning" size="small" @click="handleResetPassword(row)">
            重置密码
          </el-button>
          <el-button
            link
            :type="row.status === 1 ? 'warning' : 'success'"
            size="small"
            @click="handleToggleStatus(row)"
          >
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
          <el-button link type="primary" size="small" @click="handleAssignRoles(row)">
            分配角色
          </el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 新增/编辑用户对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '新增用户'"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form :model="userForm" :rules="userRules" ref="userFormRef" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名" :disabled="isEdit" />
        </el-form-item>

        <el-form-item label="密码" prop="password" v-if="!isEdit">
          <el-input v-model="userForm.password" type="password" placeholder="请输入密码" show-password />
        </el-form-item>

        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="userForm.realName" placeholder="请输入真实姓名" />
        </el-form-item>

        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="userForm.gender">
            <el-radio label="MALE">男</el-radio>
            <el-radio label="FEMALE">女</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>

        <el-form-item label="部门" prop="departmentId">
          <el-select v-model="userForm.departmentId" placeholder="请选择部门" style="width: 100%">
            <el-option label="管理层" :value="1" />
            <el-option label="民事部" :value="2" />
            <el-option label="刑事部" :value="3" />
            <el-option label="行政部" :value="4" />
          </el-select>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="userForm.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配角色对话框 -->
    <el-dialog v-model="roleDialogVisible" title="分配角色" width="400px">
      <el-form label-width="80px">
        <el-form-item label="用户">
          <span>{{ currentUser?.realName }}（{{ currentUser?.username }}）</span>
        </el-form-item>
        <el-form-item label="角色">
          <el-checkbox-group v-model="selectedRoles">
            <el-checkbox v-for="role in allRoles" :key="role.id" :label="role.id">
              {{ role.roleName }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveRoles">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import {
  getUserList,
  createUser,
  updateUser,
  deleteUser,
  toggleUserStatus,
  resetPassword,
  assignRoles
} from '@/api/user'
import { getAllRoles } from '@/api/role'

const loading = ref(false)
const userList = ref([])

// 分页
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// 筛选器
const filters = reactive({
  keyword: '',
  departmentId: null,
  status: null
})

// 对话框
const dialogVisible = ref(false)
const isEdit = ref(false)
const userFormRef = ref(null)

// 表单数据
const userForm = reactive({
  username: '',
  password: '',
  realName: '',
  gender: 'MALE',
  phone: '',
  email: '',
  departmentId: null,
  status: 1
})

// 表单验证规则
const userRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  departmentId: [
    { required: true, message: '请选择部门', trigger: 'change' }
  ]
}

// 角色相关
const roleDialogVisible = ref(false)
const currentUser = ref(null)
const allRoles = ref([])
const selectedRoles = ref([])

// 获取用户列表
const fetchUsers = async () => {
  try {
    loading.value = true
    const res = await getUserList({
      page: pagination.page,
      size: pagination.size,
      keyword: filters.keyword || undefined,
      departmentId: filters.departmentId,
      status: filters.status
    })

    userList.value = res.data?.content || []
    pagination.total = res.data?.totalElements || 0
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

// 获取全部角色
const fetchRoles = async () => {
  try {
    const res = await getAllRoles()
    allRoles.value = res.data || []
  } catch (error) {
    console.error('获取角色列表失败:', error)
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  fetchUsers()
}

// 重置
const handleReset = () => {
  filters.keyword = ''
  filters.departmentId = null
  filters.status = null
  pagination.page = 1
  fetchUsers()
}

// 新增用户
const handleAdd = () => {
  isEdit.value = false
  Object.assign(userForm, {
    username: '',
    password: '',
    realName: '',
    gender: 'MALE',
    phone: '',
    email: '',
    departmentId: null,
    status: 1
  })
  dialogVisible.value = true
}

// 编辑用户
const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(userForm, {
    id: row.id,
    username: row.username,
    realName: row.realName,
    gender: row.gender,
    phone: row.phone,
    email: row.email,
    departmentId: row.departmentId,
    status: row.status
  })
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  try {
    await userFormRef.value?.validate()

    if (isEdit.value) {
      await updateUser(userForm.id, userForm)
      ElMessage.success('更新用户成功')
    } else {
      await createUser(userForm)
      ElMessage.success('新增用户成功')
    }

    dialogVisible.value = false
    fetchUsers()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提交失败:', error)
      ElMessage.error(isEdit.value ? '更新用户失败' : '新增用户失败')
    }
  }
}

// 对话框关闭
const handleDialogClose = () => {
  userFormRef.value?.resetFields()
}

// 重置密码
const handleResetPassword = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt(
      `请输入用户"${row.realName}"的新密码`,
      '重置密码',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /^.{6,20}$/,
        inputErrorMessage: '密码长度在 6 到 20 个字符'
      }
    )

    await resetPassword(row.id, { newPassword: value })
    ElMessage.success('密码重置成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重置密码失败:', error)
      ElMessage.error('重置密码失败')
    }
  }
}

// 切换状态
const handleToggleStatus = async (row) => {
  try {
    const action = row.status === 1 ? '禁用' : '启用'
    await ElMessageBox.confirm(`确定要${action}用户"${row.realName}"吗？`, '确认操作', {
      type: 'warning'
    })

    const newStatus = row.status === 1 ? 0 : 1
    await toggleUserStatus(row.id, newStatus)
    ElMessage.success(`${action}成功`)
    fetchUsers()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('切换状态失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

// 分配角色
const handleAssignRoles = async (row) => {
  currentUser.value = row
  // 设置已分配的角色
  selectedRoles.value = row.roles?.map(r => r.id) || []
  roleDialogVisible.value = true
}

// 保存角色分配
const handleSaveRoles = async () => {
  try {
    await assignRoles(currentUser.value.id, { roleIds: selectedRoles.value })
    ElMessage.success('分配角色成功')
    roleDialogVisible.value = false
    fetchUsers()
  } catch (error) {
    console.error('分配角色失败:', error)
    ElMessage.error('分配角色失败')
  }
}

// 删除用户
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除用户"${row.realName}"吗？此操作不可恢复。`, '删除确认', {
      type: 'warning'
    })

    await deleteUser(row.id)
    ElMessage.success('删除成功')
    fetchUsers()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除用户失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 分页
const handleSizeChange = (size) => {
  pagination.size = size
  pagination.page = 1
  fetchUsers()
}

const handleCurrentChange = (page) => {
  pagination.page = page
  fetchUsers()
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

onMounted(() => {
  fetchUsers()
  fetchRoles()
})
</script>

<style scoped lang="scss">
.user-management {
  .filter-bar {
    display: flex;
    align-items: center;
    padding: 20px;
    background-color: #fff;
    border-radius: 4px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
    margin-bottom: 20px;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
