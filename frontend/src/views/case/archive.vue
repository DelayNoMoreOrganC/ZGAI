<template>
  <div class="case-archive">
    <PageHeader title="归档库" />

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

      <el-button @click="fetchArchivedCases" :loading="loading">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <el-table
      v-loading="loading"
      :data="archivedCases"
      stripe
      style="width: 100%; margin-top: 20px"
    >
      <el-table-column prop="caseNumber" label="案号" width="150" />
      <el-table-column prop="caseName" label="案件名称" min-width="200" />
      <el-table-column prop="caseTypeDesc" label="案件类型" width="100" />
      <el-table-column prop="caseReason" label="案由" min-width="150" />
      <el-table-column prop="court" label="审理法院" min-width="150" />
      <el-table-column prop="amount" label="标的额" width="120">
        <template #default="{ row }">
          {{ row.amount ? `¥${row.amount}` : '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="attorneyFee" label="律师费" width="120">
        <template #default="{ row }">
          {{ row.attorneyFee ? `¥${row.attorneyFee}` : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="handleViewDetail(row)">
            查看详情
          </el-button>
          <el-button type="success" size="small" @click="handleDownloadArchive(row)">
            下载档案
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
      @current-change="fetchArchivedCases"
      @size-change="fetchArchivedCases"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import request from '@/utils/request'

const loading = ref(false)
const archivedCases = ref([])
const searchKeyword = ref('')
const pagination = ref({
  page: 1,
  size: 20,
  total: 0
})

const fetchArchivedCases = async () => {
  loading.value = true
  try {
    const response = await request.get('/cases', {
      params: {
        archived: true,
        page: pagination.value.page,
        size: pagination.value.size
      }
    })

    if (response.data.code === 200) {
      archivedCases.value = response.data.data.records
      pagination.value.total = response.data.data.total
    }
  } catch (error) {
    ElMessage.error('获取归档库数据失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleViewDetail = (row) => {
  // 跳转到案件详情页
  window.location.href = `/#/cases/${row.id}`
}

const handleDownloadArchive = async (row) => {
  try {
    // 生成归档PDF
    const response = await request.post(`/cases/${row.id}/archive-pdf`, {}, {
      responseType: 'blob'
    })

    // 创建下载链接
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `案件归档_${row.caseNumber}_${row.caseName}.pdf`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('档案下载成功')
  } catch (error) {
    ElMessage.error('下载档案失败')
    console.error(error)
  }
}

const handleSearch = () => {
  // 搜索功能可以在后端实现，这里暂时只显示所有
  fetchArchivedCases()
}

onMounted(() => {
  fetchArchivedCases()
})
</script>

<style scoped>
.case-archive {
  padding: 20px;
}

.actions-bar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}
</style>
