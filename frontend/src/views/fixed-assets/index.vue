<template>
  <div class="fixed-assets-page">
    <PageHeader title="固定资产管理">
      <template #extra>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          新增资产
        </el-button>
      </template>
    </PageHeader>

    <!-- 搜索筛选 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchForm" inline>
        <el-form-item label="资产名称">
          <el-input v-model="searchForm.name" placeholder="请输入" clearable style="width: 200px" />
        </el-form-item>
        <el-form-item label="类别">
          <el-select v-model="searchForm.category" placeholder="请选择" clearable style="width: 150px">
            <el-option label="全部" value="" />
            <el-option label="电子设备" value="电子设备" />
            <el-option label="办公家具" value="办公家具" />
            <el-option label="车辆" value="车辆" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="使用状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 120px">
            <el-option label="全部" value="" />
            <el-option label="在用" value="在用" />
            <el-option label="闲置" value="闲置" />
            <el-option label="报废" value="报废" />
            <el-option label="维修中" value="维修中" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadAssets">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card" shadow="never">
      <el-table :data="assets" border v-loading="loading" style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="assetNumber" label="资产编号" width="130" />
        <el-table-column prop="name" label="资产名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="category" label="类别" width="100">
          <template #default="{ row }">
            <el-tag :type="getCategoryTagType(row.category)">
              {{ row.category }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="value" label="价值(元)" width="120" align="right">
          <template #default="{ row }">
            ¥{{ row.value?.toLocaleString() || '0' }}
          </template>
        </el-table-column>
        <el-table-column prop="purchaseDate" label="购置日期" width="110" />
        <el-table-column prop="user" label="使用人" width="100" />
        <el-table-column prop="location" label="存放位置" width="120" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusTagType(row.status)">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lifespan" label="使用年限" width="100" align="center" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleView(row)">
              查看
            </el-button>
            <el-button link type="primary" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button link type="success" size="small" @click="handleTransfer(row)">
              调拨
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
          @size-change="loadAssets"
          @current-change="loadAssets"
        />
      </div>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="110px">
        <el-form-item label="资产编号" prop="assetNumber">
          <el-input v-model="form.assetNumber" placeholder="例如：GD-2024-001" />
        </el-form-item>
        <el-form-item label="资产名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入资产名称" />
        </el-form-item>
        <el-form-item label="类别" prop="category">
          <el-select v-model="form.category" placeholder="请选择类别" style="width: 100%">
            <el-option label="电子设备" value="电子设备" />
            <el-option label="办公家具" value="办公家具" />
            <el-option label="车辆" value="车辆" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="价值(元)" prop="value">
          <el-input-number v-model="form.value" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="购置日期" prop="purchaseDate">
              <el-date-picker
                v-model="form.purchaseDate"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="使用年限" prop="lifespan">
              <el-input-number v-model="form.lifespan" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="使用人" prop="user">
          <el-input v-model="form.user" placeholder="请输入使用人" />
        </el-form-item>
        <el-form-item label="存放位置" prop="location">
          <el-input v-model="form.location" placeholder="请输入存放位置" />
        </el-form-item>
        <el-form-item label="使用状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="在用" value="在用" />
            <el-option label="闲置" value="闲置" />
            <el-option label="报废" value="报废" />
            <el-option label="维修中" value="维修中" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 查看详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="资产详情" width="600px">
      <el-descriptions :column="2" border v-if="currentAsset">
        <el-descriptions-item label="资产编号">{{ currentAsset.assetNumber }}</el-descriptions-item>
        <el-descriptions-item label="资产名称">{{ currentAsset.name }}</el-descriptions-item>
        <el-descriptions-item label="类别">{{ currentAsset.category }}</el-descriptions-item>
        <el-descriptions-item label="价值">¥{{ currentAsset.value?.toLocaleString() || '0' }}</el-descriptions-item>
        <el-descriptions-item label="购置日期">{{ currentAsset.purchaseDate }}</el-descriptions-item>
        <el-descriptions-item label="使用年限">{{ currentAsset.lifespan }}年</el-descriptions-item>
        <el-descriptions-item label="使用人">{{ currentAsset.user || '-' }}</el-descriptions-item>
        <el-descriptions-item label="存放位置">{{ currentAsset.location || '-' }}</el-descriptions-item>
        <el-descriptions-item label="使用状态">{{ currentAsset.status }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ currentAsset.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import request from '@/utils/request'

const loading = ref(false)
const assets = ref([])

// 搜索表单
const searchForm = reactive({
  name: '',
  category: '',
  status: ''
})

// 分页
const pagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// 对话框
const dialogVisible = ref(false)
const detailDialogVisible = ref(false)
const dialogTitle = computed(() => form.id ? '编辑资产' : '新增资产')
const submitting = ref(false)

// 表单
const form = reactive({
  id: null,
  assetNumber: '',
  name: '',
  category: '',
  value: 0,
  purchaseDate: '',
  lifespan: 0,
  user: '',
  location: '',
  status: '在用',
  remark: ''
})

const rules = {
  assetNumber: [{ required: true, message: '请输入资产编号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入资产名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择类别', trigger: 'change' }],
  value: [{ required: true, message: '请输入价值', trigger: 'blur' }],
  purchaseDate: [{ required: true, message: '请选择购置日期', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}

// 当前查看的资产
const currentAsset = ref(null)

// 加载资产列表
const loadAssets = async () => {
  try {
    loading.value = true
    const { data } = await request.get('/fixed-assets', {
      params: {
        ...searchForm,
        page: pagination.page - 1,
        size: pagination.size
      }
    })

    assets.value = data.content || data.records || []
    pagination.total = data.totalElements || data.total || 0
  } catch (error) {
    console.error('加载失败:', error)
    ElMessage.error('加载失败：' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 重置搜索
const handleReset = () => {
  searchForm.name = ''
  searchForm.category = ''
  searchForm.status = ''
  pagination.page = 1
  loadAssets()
}

// 获取类别标签颜色
const getCategoryTagType = (category) => {
  const typeMap = {
    '电子设备': 'primary',
    '办公家具': 'success',
    '车辆': 'warning',
    '其他': 'info'
  }
  return typeMap[category] || ''
}

// 获取状态标签颜色
const getStatusTagType = (status) => {
  const typeMap = {
    '在用': 'success',
    '闲置': 'warning',
    '报废': 'info',
    '维修中': 'danger'
  }
  return typeMap[status] || ''
}

// 新增
const handleAdd = () => {
  Object.assign(form, {
    id: null,
    assetNumber: `GD-${new Date().getFullYear()}-${String(Math.floor(Math.random() * 10000)).padStart(4, '0')}`,
    name: '',
    category: '',
    value: 0,
    purchaseDate: '',
    lifespan: 0,
    user: '',
    location: '',
    status: '在用',
    remark: ''
  })
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  Object.assign(form, row)
  dialogVisible.value = true
}

// 查看
const handleView = async (row) => {
  try {
    const { data } = await request.get(`/fixed-assets/${row.id}`)
    currentAsset.value = data
    detailDialogVisible.value = true
  } catch (error) {
    console.error('加载详情失败:', error)
    ElMessage.error('加载详情失败')
  }
}

// 调拨
const handleTransfer = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入新的使用人', '资产调拨', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '请输入使用人姓名',
      inputPattern: /^.{1,50}$/,
      inputErrorMessage: '使用人长度为1-50个字符'
    })

    await request.put(`/fixed-assets/${row.id}/transfer`, { user: value })
    ElMessage.success('调拨成功')
    loadAssets()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('调拨失败:', error)
      ElMessage.error('调拨失败')
    }
  }
}

// 删除
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除"${row.name}"吗？`, '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })

    await request.delete(`/fixed-assets/${row.id}`)
    ElMessage.success('删除成功')
    loadAssets()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    const valid = await form.ref.validate()
    if (!valid) return

    submitting.value = true

    if (form.id) {
      await request.put(`/fixed-assets/${form.id}`, form)
      ElMessage.success('更新成功')
    } else {
      await request.post('/fixed-assets', form)
      ElMessage.success('新增成功')
    }

    dialogVisible.value = false
    loadAssets()
  } catch (error) {
    console.error('提交失败:', error)
    ElMessage.error('提交失败：' + (error.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}

// 关闭对话框
const handleDialogClose = () => {
  form.ref?.resetFields()
}

// 初始化
onMounted(() => {
  loadAssets()
})
</script>

<style scoped lang="scss">
.fixed-assets-page {
  .search-card {
    margin-bottom: 20px;
  }

  .table-card {
    .pagination {
      margin-top: 20px;
      display: flex;
      justify-content: flex-end;
    }
  }
}
</style>
