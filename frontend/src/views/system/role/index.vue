<template>
  <div class="role-management">
    <PageHeader title="角色管理">
      <template #extra>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增角色
        </el-button>
      </template>
    </PageHeader>

    <!-- 角色列表 -->
    <el-table :data="roleList" border v-loading="loading" stripe>
      <el-table-column prop="roleName" label="角色名称" width="150" />
      <el-table-column prop="roleCode" label="角色代码" width="150" />
      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="userCount" label="人员数量" width="100" align="center">
        <template #default="{ row }">
          <el-tag size="small">{{ row.userCount || 0 }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="角色类型" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="row.systemRole ? 'info' : 'success'" size="small">
            {{ row.systemRole ? '系统角色' : '自定义角色' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <template v-if="!row.systemRole">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="primary" size="small" @click="handleAssignPermissions(row)">分配权限</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
          <span v-else class="system-role-hint">由身份权限基线维护</span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 新增/编辑角色对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑角色' : '新增角色'"
      width="500px"
      @close="handleDialogClose"
    >
      <el-form :model="roleForm" :rules="roleRules" ref="roleFormRef" label-width="100px">
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="roleForm.roleName" placeholder="请输入角色名称" />
        </el-form-item>

        <el-form-item label="角色代码" prop="roleCode">
          <el-input v-model="roleForm.roleCode" placeholder="请输入角色代码，如：LAWYER" :disabled="isEdit" />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="roleForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入角色描述"
          />
        </el-form-item>

      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限对话框 -->
    <el-dialog v-model="permissionDialogVisible" title="分配权限" width="600px">
      <div class="permission-tree" v-loading="permissionLoading">
        <el-tree
          v-if="permissionTree.length"
          ref="permissionTreeRef"
          :data="permissionTree"
          :props="treeProps"
          show-checkbox
          node-key="key"
          default-expand-all
        />
        <el-empty v-else-if="!permissionLoading" description="暂无可分配权限" :image-size="64" />
      </div>
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePermissions">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import {
  getRoleList,
  createRole,
  updateRole,
  deleteRole,
  assignPermissions,
  getAvailablePermissions
} from '@/api/role'

const loading = ref(false)
const roleList = ref([])

// 分页
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// 对话框
const dialogVisible = ref(false)
const isEdit = ref(false)
const roleFormRef = ref(null)

// 表单数据
const roleForm = reactive({
  roleName: '',
  roleCode: '',
  description: ''
})

// 表单验证规则
const roleRules = {
  roleName: [
    { required: true, message: '请输入角色名称', trigger: 'blur' }
  ],
  roleCode: [
    { required: true, message: '请输入角色代码', trigger: 'blur' },
    { pattern: /^[A-Z][A-Z0-9_]{1,49}$/, message: '角色代码须以大写字母开头，仅包含大写字母、数字和下划线', trigger: 'blur' }
  ]
}

// 权限相关
const permissionDialogVisible = ref(false)
const currentRole = ref(null)
const permissionTreeRef = ref(null)
const permissionLoading = ref(false)
const permissionTree = ref([])
const treeProps = {
  children: 'children',
  label: 'name'
}

const permissionGroups = {
  CASE: '案件管理', CLIENT: '客户管理', DOCUMENT: '案件文件', APPROVAL: '审批管理',
  TODO: '待办与日程', CALENDAR: '待办与日程', FINANCE: '财务管理', INVOICE: '财务管理',
  KNOWLEDGE: '知识与 AI', AI: '知识与 AI', STATISTICS: '统计报表', WORK: '统计报表',
  USER: '系统管理', ROLE: '系统管理', SYSTEM: '系统管理', BACKUP: '系统管理', AUDIT: '系统管理'
}

const buildPermissionTree = (permissions) => {
  const groups = new Map()
  permissions.forEach(permission => {
    const prefix = String(permission.permissionCode || '').split('_')[0]
    const groupName = permissionGroups[prefix] || '其他权限'
    if (!groups.has(groupName)) groups.set(groupName, [])
    groups.get(groupName).push({
      key: `permission:${permission.id}`,
      permissionId: permission.id,
      name: `${permission.permissionName}（${permission.permissionCode}）`
    })
  })
  return Array.from(groups.entries()).map(([name, children]) => ({
    key: `group:${name}`,
    name,
    children
  }))
}

const loadPermissionTree = async () => {
  permissionLoading.value = true
  try {
    const response = await getAvailablePermissions()
    permissionTree.value = buildPermissionTree(response.data || [])
  } finally {
    permissionLoading.value = false
  }
}

// 获取角色列表
const fetchRoles = async () => {
  try {
    loading.value = true
    const res = await getRoleList({
      page: pagination.page - 1,
      size: pagination.size
    })

    roleList.value = res.data?.content || []
    pagination.total = res.data?.totalElements || 0
  } catch (error) {
    console.error('获取角色列表失败:', error)
    ElMessage.error('获取角色列表失败')
  } finally {
    loading.value = false
  }
}

// 新增角色
const handleAdd = () => {
  isEdit.value = false
  Object.assign(roleForm, {
    roleName: '',
    roleCode: '',
    description: ''
  })
  dialogVisible.value = true
}

// 编辑角色
const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(roleForm, {
    id: row.id,
    roleName: row.roleName,
    roleCode: row.roleCode,
    description: row.description
  })
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  try {
    await roleFormRef.value?.validate()

    if (isEdit.value) {
      await updateRole(roleForm.id, roleForm)
      ElMessage.success('更新角色成功')
    } else {
      await createRole(roleForm)
      ElMessage.success('新增角色成功')
    }

    dialogVisible.value = false
    fetchRoles()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提交失败:', error)
      ElMessage.error(isEdit.value ? '更新角色失败' : '新增角色失败')
    }
  }
}

// 对话框关闭
const handleDialogClose = () => {
  roleFormRef.value?.resetFields()
}

// 分配权限
const handleAssignPermissions = async (row) => {
  currentRole.value = row
  try {
    await loadPermissionTree()
    permissionDialogVisible.value = true
    await nextTick()
    const checkedKeys = (row.permissionIds || []).map(id => `permission:${id}`)
    permissionTreeRef.value?.setCheckedKeys(checkedKeys)
  } catch (error) {
    console.error('获取权限目录失败:', error)
    ElMessage.error('获取权限目录失败')
  }
}

// 保存权限分配
const handleSavePermissions = async () => {
  try {
    const checkedKeys = permissionTreeRef.value?.getCheckedKeys() || []
    const permissionIds = checkedKeys
      .filter(key => String(key).startsWith('permission:'))
      .map(key => Number(String(key).slice('permission:'.length)))
      .filter(Number.isFinite)
    await assignPermissions(currentRole.value.id, { permissionIds })
    ElMessage.success('分配权限成功')
    permissionDialogVisible.value = false
    fetchRoles()
  } catch (error) {
    console.error('分配权限失败:', error)
    ElMessage.error('分配权限失败')
  }
}

// 删除角色
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除角色"${row.roleName}"吗？此操作不可恢复。`, '删除确认', {
      type: 'warning'
    })

    await deleteRole(row.id)
    ElMessage.success('删除成功')
    fetchRoles()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除角色失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 分页
const handleSizeChange = (size) => {
  pagination.size = size
  pagination.page = 1
  fetchRoles()
}

const handleCurrentChange = (page) => {
  pagination.page = page
  fetchRoles()
}

onMounted(() => {
  fetchRoles()
})
</script>

<style scoped lang="scss">
.role-management {
  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .permission-tree {
    max-height: 400px;
    overflow-y: auto;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    padding: 10px;
    min-height: 180px;
  }

  .system-role-hint {
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }
}
</style>
