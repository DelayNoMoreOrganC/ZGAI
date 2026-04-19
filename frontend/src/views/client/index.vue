<template>
  <div class="client">
    <PageHeader title="客户管理">
      <template #extra>
        <el-button type="primary" @click="handleCreate">
          <el-icon><Plus /></el-icon>
          新建客户
        </el-button>
      </template>
    </PageHeader>

    <!-- 搜索筛选区 -->
    <div class="filter-section">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索客户名称、联系人..."
        clearable
        style="width: 300px"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <el-select v-model="filterType" placeholder="客户类型" clearable style="width: 150px">
        <el-option label="个人" value="personal" />
        <el-option label="企业" value="company" />
      </el-select>

      <el-select v-model="filterLevel" placeholder="客户等级" clearable style="width: 150px">
        <el-option label="VIP" value="vip" />
        <el-option label="重要" value="important" />
        <el-option label="普通" value="normal" />
      </el-select>

      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <!-- 客户列表 -->
    <el-table :data="clientList" border v-loading="loading">
      <el-table-column prop="name" label="客户名称" width="200">
        <template #default="{ row }">
          <el-link type="primary" @click="handleViewDetail(row)">
            {{ row.name }}
          </el-link>
        </template>
      </el-table-column>

      <el-table-column prop="type" label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.type === 'personal' ? 'primary' : 'success'" size="small">
            {{ row.type === 'personal' ? '个人' : '企业' }}
          </el-tag>
        </template>
      </el-table-column>

      <el-table-column prop="contact" label="联系人" width="120" />
      <el-table-column prop="phone" label="联系电话" width="130" />
      <el-table-column prop="email" label="邮箱" width="180" />
      <el-table-column prop="caseCount" label="关联案件" width="100" />
      <el-table-column prop="totalAmount" label="总收费(万元)" width="130">
        <template #default="{ row }">
          {{ row.totalAmount || '0' }}
        </template>
      </el-table-column>
      <el-table-column prop="level" label="客户等级" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.level === 'vip'" type="danger">VIP</el-tag>
          <el-tag v-else-if="row.level === 'important'" type="warning">重要</el-tag>
          <el-tag v-else>普通</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="160" sortable />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="handleViewDetail(row)">
            详情
          </el-button>
          <el-button link type="primary" size="small" @click="handleEdit(row)">
            编辑
          </el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next"
      />
    </div>

    <!-- 客户详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="客户详情" width="900px">
      <el-tabs v-model="detailActiveTab">
        <el-tab-pane label="基本信息" name="basic">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="客户名称">
              {{ currentClient.name }}
            </el-descriptions-item>
            <el-descriptions-item label="客户类型">
              <el-tag :type="currentClient.type === 'personal' ? 'primary' : 'success'">
                {{ currentClient.type === 'personal' ? '个人' : '企业' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="联系人">
              {{ currentClient.contact }}
            </el-descriptions-item>
            <el-descriptions-item label="联系电话">
              {{ currentClient.phone }}
            </el-descriptions-item>
            <el-descriptions-item label="邮箱">
              {{ currentClient.email }}
            </el-descriptions-item>
            <el-descriptions-item label="客户等级">
              <el-tag v-if="currentClient.level === 'vip'" type="danger">VIP</el-tag>
              <el-tag v-else-if="currentClient.level === 'important'" type="warning">重要</el-tag>
              <el-tag v-else>普通</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="地址" :span="2">
              {{ currentClient.address }}
            </el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">
              {{ currentClient.remark }}
            </el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <el-tab-pane label="关联案件" name="cases">
          <el-table :data="currentClient.cases || []" border>
            <el-table-column prop="caseName" label="案件名称" />
            <el-table-column prop="caseNumber" label="案号" width="150" />
            <el-table-column prop="caseType" label="案件类型" width="100" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag>{{ row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="沟通记录" name="communications">
          <div class="communications-section">
            <el-button type="primary" size="small" @click="handleAddCommunication">
              <el-icon><Plus /></el-icon>
              添加记录
            </el-button>

            <el-timeline class="communication-timeline">
              <el-timeline-item
                v-for="record in currentClient.communications || []"
                :key="record.id"
                :timestamp="record.date"
              >
                <el-card>
                  <div class="communication-header">
                    <span class="communicator">{{ record.communicator }}</span>
                    <span class="method">{{ record.method }}</span>
                  </div>
                  <div class="communication-content">{{ record.content }}</div>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-tab-pane>

        <el-tab-pane label="收费统计" name="fees">
          <div class="fee-stats">
            <div class="stat-item">
              <span class="label">总收费：</span>
              <span class="value">¥{{ currentClient.totalAmount || '0' }}</span>
            </div>
            <div class="stat-item">
              <span class="label">已收：</span>
              <span class="value received">¥{{ currentClient.receivedAmount || '0' }}</span>
            </div>
            <div class="stat-item">
              <span class="label">待收：</span>
              <span class="value pending">¥{{ currentClient.pendingAmount || '0' }}</span>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="利益冲突检索" name="conflict">
          <div class="conflict-section">
            <el-button type="primary" @click="handleConflictCheck">
              <el-icon><Search /></el-icon>
              执行利益冲突检索
            </el-button>

            <div v-if="conflictResult" class="conflict-result">
              <h4>检索结果</h4>
              <el-alert
                :type="conflictResult.hasConflict ? 'error' : 'success'"
                :title="conflictResult.hasConflict ? '发现利益冲突' : '未发现利益冲突'"
                :description="conflictResult.description"
                show-icon
              />
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { useRouter } from 'vue-router'
import {
  getClientList,
  deleteClient,
  searchClients,
  conflictCheck
} from '@/api/client'

const router = useRouter()
const loading = ref(false)
const searchKeyword = ref('')
const filterType = ref('')
const filterLevel = ref('')
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const detailDialogVisible = ref(false)
const detailActiveTab = ref('basic')
const conflictResult = ref(null)

const currentClient = ref({})

const clientList = ref([])

// 加载客户列表
const fetchClientList = async () => {
  try {
    loading.value = true
    const params = {
      page: currentPage.value,
      size: pageSize.value
    }
    const res = await getClientList(params)
    clientList.value = res.data || []
    total.value = res.total || 0
  } catch (error) {
    console.error('加载客户列表失败:', error)
    ElMessage.error('加载客户列表失败')
  } finally {
    loading.value = false
  }
}

// 页面加载时获取数据
onMounted(() => {
  fetchClientList()
})

const handleCreate = () => {
  router.push('/client/create')
}

const handleSearch = async () => {
  try {
    if (!searchKeyword.value.trim()) {
      fetchClientList()
      return
    }
    loading.value = true
    const res = await searchClients(searchKeyword.value)
    clientList.value = res.data || []
    total.value = res.data?.length || 0
  } catch (error) {
    console.error('搜索客户失败:', error)
    ElMessage.error('搜索客户失败')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  searchKeyword.value = ''
  filterType.value = ''
  filterLevel.value = ''
  fetchClientList()
}

const handleViewDetail = (client) => {
  currentClient.value = {
    ...client,
    cases: [
      {
        caseName: '张三诉李四买卖合同纠纷',
        caseNumber: '（2024）京0105民初12345号',
        caseType: '民事',
        status: '审理中'
      }
    ],
    communications: [
      {
        id: '1',
        date: '2024-04-15 10:00',
        communicator: '张律师',
        method: '电话',
        content: '沟通案件进展，客户对进度表示满意'
      }
    ],
    totalAmount: '15.5',
    receivedAmount: '10.0',
    pendingAmount: '5.5'
  }
  detailDialogVisible.value = true
}

const handleEdit = (client) => {
  router.push(`/client/${client.id}/edit`)
}

const handleDelete = async (client) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除客户"${client.name}"吗？删除后可进入回收站恢复。`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await deleteClient(client.id)
    ElMessage.success('删除成功')
    fetchClientList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除客户失败:', error)
      ElMessage.error('删除客户失败')
    }
  }
}

const handleAddCommunication = () => {
  ElMessage.info('请先在客户详情中添加沟通记录')
}

const handleConflictCheck = async () => {
  try {
    if (!currentClient.value.id) {
      ElMessage.warning('请先选择客户')
      return
    }

    ElMessage.info('正在执行利益冲突检索...')

    const response = await conflictCheck(currentClient.value.id)

    if (response.success) {
      conflictResult.value = response.data
      ElMessage.success(conflictResult.value.hasConflict ? '发现利益冲突' : '未发现利益冲突')
    } else {
      ElMessage.error('利益冲突检索失败')
    }
  } catch (error) {
    console.error('利益冲突检索失败:', error)
    ElMessage.error('利益冲突检索失败')
  }
}
</script>

<style scoped lang="scss">
.client {
  .filter-section {
    display: flex;
    gap: 10px;
    margin-bottom: 20px;
    padding: 15px 20px;
    background-color: #f5f7fa;
    border-radius: 4px;
    flex-wrap: wrap;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .communications-section {
    .communication-timeline {
      margin-top: 20px;

      .communication-header {
        display: flex;
        gap: 10px;
        margin-bottom: 8px;

        .communicator {
          font-weight: 500;
          color: #333;
        }

        .method {
          font-size: 12px;
          color: #909399;
        }
      }

      .communication-content {
        color: #606266;
        line-height: 1.6;
      }
    }
  }

  .fee-stats {
    .stat-item {
      display: inline-block;
      margin-right: 30px;
      margin-bottom: 15px;
      font-size: 16px;

      .label {
        color: #666;
      }

      .value {
        font-weight: bold;
        color: #333;

        &.received {
          color: #67c23a;
        }

        &.pending {
          color: #e6a23c;
        }
      }
    }
  }

  .conflict-section {
    .conflict-result {
      margin-top: 20px;

      h4 {
        margin: 0 0 10px;
        font-size: 14px;
        color: #333;
      }
    }
  }
}
</style>
