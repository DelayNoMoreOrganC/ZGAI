<template>
  <div class="case-unit">
    <el-tabs v-model="activeTab" type="card">
      <!-- 财产保全 -->
      <el-tab-pane label="财产保全" name="preservation">
        <div class="tab-content">
          <div class="toolbar">
            <el-button type="primary" @click="handleAddPreservation">
              <el-icon><Plus /></el-icon>
              添加保全
            </el-button>
          </div>

          <el-table :data="preservationList" border v-loading="loading">
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="target" label="被申请人" width="150" />
            <el-table-column prop="subject" label="保全标的" width="200" />
            <el-table-column prop="amount" label="金额(元)" width="150">
              <template #default="{ row }">
                ¥{{ row.amount?.toLocaleString() || '0' }}
              </template>
            </el-table-column>
            <el-table-column prop="court" label="法院" width="200" />
            <el-table-column prop="date" label="保全日期" width="120" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusTagType(row.status)">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleEditPreservation(row)">
                  编辑
                </el-button>
                <el-button link type="danger" size="small" @click="handleDeletePreservation(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 执行情况 -->
      <el-tab-pane label="执行情况" name="execution">
        <div class="tab-content">
          <div class="toolbar">
            <el-button type="primary" @click="handleAddExecution">
              <el-icon><Plus /></el-icon>
              添加执行
            </el-button>
          </div>

          <el-table :data="executionList" border v-loading="loading">
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="caseNumber" label="执行案号" width="180" />
            <el-table-column prop="court" label="法院" width="200" />
            <el-table-column prop="applicant" label="申请人" width="120" />
            <el-table-column prop="respondent" label="被执行人" width="150" />
            <el-table-column prop="subject" label="执行标的" width="150" />
            <el-table-column prop="amount" label="金额(元)" width="120">
              <template #default="{ row }">
                ¥{{ row.amount?.toLocaleString() || '0' }}
              </template>
            </el-table-column>
            <el-table-column prop="date" label="执行日期" width="120" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusTagType(row.status)">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleEditExecution(row)">
                  编辑
                </el-button>
                <el-button link type="danger" size="small" @click="handleDeleteExecution(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 庭审记录 -->
      <el-tab-pane label="庭审记录" name="hearing">
        <div class="tab-content">
          <div class="toolbar">
            <el-button type="primary" @click="handleAddHearing">
              <el-icon><Plus /></el-icon>
              添加庭审
            </el-button>
          </div>

          <el-table :data="hearingList" border v-loading="loading">
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="date" label="日期" width="120" sortable />
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag :type="getHearingTypeTagType(row.type)">
                  {{ row.type }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="courtroom" label="法庭号" width="120" />
            <el-table-column prop="judge" label="审判员" width="120" />
            <el-table-column prop="clerk" label="书记员" width="120" />
            <el-table-column label="笔录" width="200">
              <template #default="{ row }">
                <div v-if="row.transcript" class="transcript-files">
                  <el-tag
                    v-for="file in row.transcript"
                    :key="file.id"
                    closable
                    @close="handleDeleteTranscript(row, file)"
                  >
                    {{ file.name }}
                  </el-tag>
                </div>
                <span v-else class="text-muted">暂无笔录</span>
              </template>
            </el-table-column>
            <el-table-column label="附件" width="200">
              <template #default="{ row }">
                <div v-if="row.attachments && row.attachments.length > 0" class="attachment-list">
                  <el-tag
                    v-for="file in row.attachments"
                    :key="file.id"
                    closable
                    @close="handleDeleteAttachment(row, file)"
                  >
                    {{ file.name }}
                  </el-tag>
                </div>
                <span v-else class="text-muted">暂无附件</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleEditHearing(row)">
                  编辑
                </el-button>
                <el-button link type="danger" size="small" @click="handleDeleteHearing(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 承办人员 -->
      <el-tab-pane label="承办人员" name="personnel">
        <div class="tab-content">
          <div class="toolbar">
            <el-button type="primary" @click="handleAddPersonnel">
              <el-icon><Plus /></el-icon>
              添加人员
            </el-button>
          </div>

          <el-table :data="personnelList" border v-loading="loading">
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column prop="position" label="职位" width="120">
              <template #default="{ row }">
                <el-tag :type="getPositionTagType(row.position)">
                  {{ row.position }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="phone" label="电话" width="150" />
            <el-table-column prop="court" label="法院" width="200" />
            <el-table-column prop="department" label="部门" width="150" />
            <el-table-column prop="remark" label="备注" show-overflow-tooltip />
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleEditPersonnel(row)">
                  编辑
                </el-button>
                <el-button link type="danger" size="small" @click="handleDeletePersonnel(row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const props = defineProps({
  caseData: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['refresh'])

const activeTab = ref('preservation')
const loading = ref(false)

// 财产保全列表
const preservationList = ref([
  {
    id: 1,
    target: '李四',
    subject: '房产查封',
    amount: 500000,
    court: '北京市朝阳区人民法院',
    date: '2024-03-15',
    status: '进行中'
  }
])

// 执行情况列表
const executionList = ref([])

// 庭审记录列表
const hearingList = ref([])

// 承办人员列表
const personnelList = ref([])

// 获取状态标签颜色
const getStatusTagType = (status) => {
  const typeMap = {
    '进行中': 'primary',
    '已完成': 'success',
    '已终止': 'info',
    '失败': 'danger'
  }
  return typeMap[status] || ''
}

// 获取庭审类型标签颜色
const getHearingTypeTagType = (type) => {
  const typeMap = {
    '开庭': 'danger',
    '谈话': 'warning',
    '调解': 'success',
    '证据交换': 'info'
  }
  return typeMap[type] || ''
}

// 获取职位标签颜色
const getPositionTagType = (position) => {
  const typeMap = {
    '审判员': 'danger',
    '书记员': 'primary',
    '法官助理': 'success',
    '执行员': 'warning'
  }
  return typeMap[position] || ''
}

// 财产保全操作
const handleAddPreservation = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入保全标的', '添加财产保全', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /^.{1,100}$/,
      inputErrorMessage: '保全标的长度为1-100个字符'
    })

    // 添加财产保全记录（本地数据，等待后端API实现）
    preservationList.value.push({
      id: Date.now(),
      target: '待填写',
      subject: value,
      amount: 0,
      court: '待填写',
      date: new Date().toISOString().split('T')[0],
      status: '待办理'
    })

    ElMessage.success('添加成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('添加保全失败:', error)
      ElMessage.error('添加保全失败')
    }
  }
}

const handleEditPreservation = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入保全标的', '编辑财产保全', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: row.subject,
      inputPattern: /^.{1,100}$/,
      inputErrorMessage: '保全标的长度为1-100个字符'
    })

    // 更新财产保全记录（本地数据，等待后端API实现）
    row.subject = value
    ElMessage.success('更新成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('编辑保全失败:', error)
      ElMessage.error('编辑保全失败')
    }
  }
}

const handleDeletePreservation = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除保全标的"${row.subject}"吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 删除财产保全记录（本地数据，等待后端API实现）
    const index = preservationList.value.findIndex(item => item.id === row.id)
    if (index > -1) {
      preservationList.value.splice(index, 1)
    }

    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除保全失败:', error)
      ElMessage.error('删除保全失败')
    }
  }
}

// 执行情况操作
const handleAddExecution = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入执行案号', '添加执行情况', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /^.{1,50}$/,
      inputErrorMessage: '执行案号长度为1-50个字符'
    })

    executionList.value.push({
      id: Date.now(),
      caseNumber: value,
      court: '待填写',
      applicant: '待填写',
      respondent: '待填写',
      subject: '待填写',
      amount: 0,
      date: new Date().toISOString().split('T')[0],
      status: '进行中'
    })

    ElMessage.success('添加成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('添加执行失败:', error)
      ElMessage.error('添加执行失败')
    }
  }
}

const handleEditExecution = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入执行案号', '编辑执行情况', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: row.caseNumber,
      inputPattern: /^.{1,50}$/,
      inputErrorMessage: '执行案号长度为1-50个字符'
    })

    row.caseNumber = value
    ElMessage.success('更新成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('编辑执行失败:', error)
      ElMessage.error('编辑执行失败')
    }
  }
}

const handleDeleteExecution = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除执行案号"${row.caseNumber}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const index = executionList.value.findIndex(item => item.id === row.id)
    if (index > -1) {
      executionList.value.splice(index, 1)
    }

    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除执行失败:', error)
      ElMessage.error('删除执行失败')
    }
  }
}

// 庭审记录操作
const handleAddHearing = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入庭审类型', '添加庭审记录', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPlaceholder: '开庭/谈话/调解/证据交换',
      inputPattern: /^.{1,20}$/,
      inputErrorMessage: '庭审类型长度为1-20个字符'
    })

    hearingList.value.push({
      id: Date.now(),
      date: new Date().toISOString().split('T')[0],
      type: value,
      courtroom: '待填写',
      judge: '待填写',
      clerk: '待填写',
      transcript: null,
      status: '进行中'
    })

    ElMessage.success('添加成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('添加庭审失败:', error)
      ElMessage.error('添加庭审失败')
    }
  }
}

const handleEditHearing = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入庭审类型', '编辑庭审记录', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: row.type,
      inputPattern: /^.{1,20}$/,
      inputErrorMessage: '庭审类型长度为1-20个字符'
    })

    row.type = value
    ElMessage.success('更新成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('编辑庭审失败:', error)
      ElMessage.error('编辑庭审失败')
    }
  }
}

const handleDeleteHearing = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除${row.type}记录吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const index = hearingList.value.findIndex(item => item.id === row.id)
    if (index > -1) {
      hearingList.value.splice(index, 1)
    }

    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除庭审失败:', error)
      ElMessage.error('删除庭审失败')
    }
  }
}

const handleDeleteTranscript = async (row, file) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除笔录"${file.name}"吗？`,
      '删除笔录',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 从庭审记录中移除笔录
    const index = row.transcripts?.indexOf(file)
    if (index > -1) {
      row.transcripts.splice(index, 1)
    }

    ElMessage.success('笔录删除成功')
    emit('refresh')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除笔录失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleDeleteAttachment = async (row, file) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除附件"${file.name}"吗？`,
      '删除附件',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 从庭审记录中移除附件
    const index = row.attachments?.indexOf(file)
    if (index > -1) {
      row.attachments.splice(index, 1)
    }

    ElMessage.success('附件删除成功')
    emit('refresh')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除附件失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 承办人员操作
const handleAddPersonnel = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入人员姓名', '添加承办人员', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /^.{1,20}$/,
      inputErrorMessage: '姓名长度为1-20个字符'
    })

    personnelList.value.push({
      id: Date.now(),
      name: value,
      position: '待填写',
      phone: '待填写',
      court: '待填写',
      notes: ''
    })

    ElMessage.success('添加成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('添加人员失败:', error)
      ElMessage.error('添加人员失败')
    }
  }
}

const handleEditPersonnel = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入人员姓名', '编辑承办人员', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputValue: row.name,
      inputPattern: /^.{1,20}$/,
      inputErrorMessage: '姓名长度为1-20个字符'
    })

    row.name = value
    ElMessage.success('更新成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('编辑人员失败:', error)
      ElMessage.error('编辑人员失败')
    }
  }
}

const handleDeletePersonnel = async (row) => {
  try {
    await ElMessageBox.confirm(`确定要删除人员"${row.name}"吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const index = personnelList.value.findIndex(item => item.id === row.id)
    if (index > -1) {
      personnelList.value.splice(index, 1)
    }

    ElMessage.success('删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除人员失败:', error)
      ElMessage.error('删除人员失败')
    }
  }
}
</script>

<style scoped lang="scss">
.case-unit {
  padding: 30px;

  .tab-content {
    .toolbar {
      margin-bottom: 20px;
    }

    .transcript-files,
    .attachment-list {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }

    .text-muted {
      color: #909399;
      font-size: 13px;
    }
  }
}
</style>
