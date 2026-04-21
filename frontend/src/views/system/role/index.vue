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
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="160">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleEdit(row)">
            编辑
          </el-button>
          <el-button link type="primary" size="small" @click="handleAssignPermissions(row)">
            分配权限
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

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="roleForm.status">
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

    <!-- 分配权限对话框 -->
    <el-dialog v-model="permissionDialogVisible" title="分配权限" width="600px">
      <div class="permission-tree">
        <el-tree
          ref="permissionTreeRef"
          :data="permissionTree"
          :props="treeProps"
          show-checkbox
          node-key="id"
          default-expand-all
        />
      </div>
      <template #footer>
        <el-button @click="permissionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePermissions">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import {
  getRoleList,
  createRole,
  updateRole,
  deleteRole,
  assignPermissions
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
  description: '',
  status: 1
})

// 表单验证规则
const roleRules = {
  roleName: [
    { required: true, message: '请输入角色名称', trigger: 'blur' }
  ],
  roleCode: [
    { required: true, message: '请输入角色代码', trigger: 'blur' },
    { pattern: /^[A-Z_]+$/, message: '角色代码只能包含大写字母和下划线', trigger: 'blur' }
  ]
}

// 权限相关
const permissionDialogVisible = ref(false)
const currentRole = ref(null)
const permissionTreeRef = ref(null)
const treeProps = {
  children: 'children',
  label: 'name'
}

// 权限树数据（模拟，实际应该从后端获取）
const permissionTree = ref([
  {
    id: 1,
    name: '案件管理',
    children: [
      { id: 11, name: '案件查看' },
      { id: 12, name: '案件创建' },
      { id: 13, name: '案件编辑' },
      { id: 14, name: '案件删除' }
    ]
  },
  {
    id: 2,
    name: '客户管理',
    children: [
      { id: 21, name: '客户查看' },
      { id: 22, name: '客户创建' },
      { id: 23, name: '客户编辑' }
    ]
  },
  {
    id: 3,
    name: '文档管理',
    children: [
      { id: 31, name: '文档查看' },
      { id: 32, name: '文档上传' },
      { id: 33, name: '文档下载' }
    ]
  },
  {
    id: 4,
    name: '日程管理',
    children: [
      { id: 41, name: '日程查看' },
      { id: 42, name: '日程创建' },
      { id: 43, name: '日程编辑' }
    ]
  },
  {
    id: 5,
    name: '系统管理',
    children: [
      { id: 51, name: '用户管理' },
      { id: 52, name: '角色管理' },
      { id: 53, name: '权限管理' }
    ]
  }
])

// 获取角色列表
const fetchRoles = async () => {
  try {
    loading.value = true
    const res = await getRoleList({
      page: pagination.page,
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
    description: '',
    status: 1
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
    description: row.description,
    status: row.status
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
const handleAssignPermissions = (row) => {
  currentRole.value = row
  // 加载角色已有的权限并设置选中状态
  const rolePermissions = row.permissions || []
  selectedPermissions.value = rolePermissions.map(p => p.id)
  permissionDialogVisible.value = true
}

// 保存权限分配
const handleSavePermissions = async () => {
  try {
    const checkedKeys = permissionTreeRef.value?.getCheckedKeys() || []
    await assignPermissions(currentRole.value.id, { permissionIds: checkedKeys })
    ElMessage.success('分配权限成功')
    permissionDialogVisible.value = false
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

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
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
  }
}
</style>
