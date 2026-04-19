<template>
  <div class="case-trash">
    <PageHeader title="回收站" />

    <div class="actions-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索案件名称、案号"
        clearable
        style="width: 300px; margin-right: 16px"
        @input="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <el-button @click="fetchTrashCases" :loading="loading">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="trashCases"
      stripe
      style="width: 100%; margin-top: 20px"
    >
      <el-table-column prop="caseNumber" label="案号" width="150" />
      <el-table-column prop="caseName" label="案件名称" min-width="200" />
      <el-table-column prop="caseTypeDesc" label="案件类型" width="100" />
      <el-table-column prop="statusDesc" label="状态" width="100" />
      <el-table-column prop="ownerName" label="主办律师" width="120" />
      <el-table-column prop="attorneyFee" label="律师费" width="120">
        <template #default="{ row }">
          {{ row.attorneyFee ? `¥${row.attorneyFee}` : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="handleRestore(row)">
            恢复
          </el-button>
          <el-button type="danger" size="small" @click="handlePermanentDelete(row)">
            永久删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-if="pagination.total > 0"
      v-model:current-page="pagination.page"
      v-model:page-size="pagination.size"
      :total="pagination.total"
      :page-sizes="[10, 20, 50, 100]"
      layout="total, sizes, prev, pager, next, jumper"
      style="margin-top: 20px; text-align: right"
      @current-change="fetchTrashCases"
      @size-change="fetchTrashCases"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import request from '@/utils/request'

const loading = ref(false)
const trashCases = ref([])
const searchKeyword = ref('')
const pagination = ref({
  page: 1,
  size: 20,
  total: 0
})

const fetchTrashCases = async () => {
  loading.value = true
  try {
    const response = await request.get('/cases', {
      params: {
        deleted: true,
        page: pagination.value.page,
        size: pagination.value.size
      }
    })

    if (response.data.code === 200) {
      trashCases.value = response.data.data.records
      pagination.value.total = response.data.data.total
    }
  } catch (error) {
    ElMessage.error('获取回收站数据失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleRestore = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要恢复案件 "${row.caseName}" 吗？`,
      '恢复案件',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    const response = await request.put(`/cases/${row.id}/restore`)

    if (response.data.code === 200) {
      ElMessage.success('案件恢复成功')
      fetchTrashCases()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('恢复案件失败')
      console.error(error)
    }
  }
}

const handlePermanentDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要永久删除案件 "${row.caseName}" 吗？此操作不可恢复！`,
      '永久删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'error',
        confirmButtonClass: 'el-button--danger'
      }
    )

    const response = await request.delete(`/cases/${row.id}`)

    if (response.data.code === 200) {
      ElMessage.success('案件已永久删除')
      fetchTrashCases()
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
      console.error(error)
    }
  }
}

const handleSearch = () => {
  // 搜索功能可以在后端实现，这里暂时只显示所有
  fetchTrashCases()
}

onMounted(() => {
  fetchTrashCases()
})
</script>

<style scoped>
.case-trash {
  padding: 20px;
}

.actions-bar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}
</style>
