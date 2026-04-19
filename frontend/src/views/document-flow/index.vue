<template>
  <div class="document-flow-page">
    <PageHeader title="公文流转" />

    <div class="flow-container">
      <el-row :gutter="20">
        <el-col :span="4">
          <el-card class="menu-card">
            <el-menu :default-active="activeType" @select="handleTypeChange">
              <el-menu-item index="">全部公文</el-menu-item>
              <el-menu-item index="DRAFT">草稿箱</el-menu-item>
              <el-menu-item index="PENDING">待审批</el-menu-item>
              <el-menu-item index="APPROVED">已通过</el-menu-item>
              <el-menu-item index="REJECTED">已驳回</el-menu-item>
            </el-menu>
          </el-card>
        </el-col>

        <el-col :span="20">
          <el-card>
            <template #header>
              <div style="display: flex; justify-content: space-between">
                <span>{{ currentTitle }}</span>
                <el-button type="primary" @click="dialogVisible = true">新建公文</el-button>
              </div>
            </template>

            <el-empty v-if="documents.length === 0" description="暂无公文" />

            <div v-else>
              <div v-for="doc in documents" :key="doc.id" class="doc-item">
                <div style="display: flex; justify-content: space-between">
                  <h4>{{ doc.title }}</h4>
                  <el-tag :type="getStatusType(doc.status)" size="small">
                    {{ formatStatus(doc.status) }}
                  </el-tag>
                </div>
                <div style="margin: 10px 0; color: #909399; font-size: 13px">
                  <span>类型：{{ doc.docType }}</span>
                  <span style="margin-left: 20px">创建人：{{ doc.creatorName }}</span>
                  <span style="margin-left: 20px">时间：{{ formatDate(doc.createdAt) }}</span>
                </div>
                <div style="margin-top: 10px">
                  <el-button v-if="doc.status === 'DRAFT'" size="small" @click="handleSubmit(doc)">
                    提交审批
                  </el-button>
                  <el-button size="small" @click="handleView(doc)">查看详情</el-button>
                </div>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <el-dialog v-model="dialogVisible" title="新建公文" width="600px">
      <el-form :model="formData" label-width="100px">
        <el-form-item label="公文标题">
          <el-input v-model="formData.title" />
        </el-form-item>
        <el-form-item label="公文类型">
          <el-select v-model="formData.docType" style="width: 100%">
            <el-option label="通知" value="通知" />
            <el-option label="公告" value="公告" />
            <el-option label="请示" value="请示" />
            <el-option label="报告" value="报告" />
          </el-select>
        </el-form-item>
        <el-form-item label="紧急程度">
          <el-radio-group v-model="formData.urgency">
            <el-radio label="普通">普通</el-radio>
            <el-radio label="紧急">紧急</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="主送机关">
          <el-input v-model="formData.mainReceiver" />
        </el-form-item>
        <el-form-item label="公文内容">
          <el-input v-model="formData.content" type="textarea" :rows="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 文档详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="公文详情" width="700px">
      <div v-if="selectedDoc" class="doc-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="公文标题" :span="2">
            {{ selectedDoc.title }}
          </el-descriptions-item>
          <el-descriptions-item label="公文类型">
            {{ selectedDoc.docType }}
          </el-descriptions-item>
          <el-descriptions-item label="紧急程度">
            {{ selectedDoc.urgency }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(selectedDoc.status)">
              {{ formatStatus(selectedDoc.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="创建人">
            {{ selectedDoc.creatorName }}
          </el-descriptions-item>
          <el-descriptions-item label="创建时间" :span="2">
            {{ formatDate(selectedDoc.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="主送机关">
            {{ selectedDoc.mainReceiver || '无' }}
          </el-descriptions-item>
          <el-descriptions-item label="公文内容" :span="2">
            <div style="white-space: pre-wrap; max-height: 300px; overflow-y: auto;">
              {{ selectedDoc.content || '无' }}
            </div>
          </el-descriptions-item>
        </el-descriptions>

        <div v-if="selectedDoc.approvalComments" style="margin-top: 20px;">
          <h4>审批意见</h4>
          <div style="background: #f5f7fa; padding: 10px; border-radius: 4px;">
            {{ selectedDoc.approvalComments }}
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import request from '@/utils/request'

const activeType = ref('')
const documents = ref([])
const dialogVisible = ref(false)
const detailDialogVisible = ref(false)
const selectedDoc = ref(null)

const formData = ref({
  title: '',
  docType: '通知',
  urgency: '普通',
  mainReceiver: '',
  content: ''
})

const currentTitle = computed(() => {
  const map = {
    '': '全部公文',
    'DRAFT': '草稿箱',
    'PENDING': '待审批',
    'APPROVED': '已通过',
    'REJECTED': '已驳回'
  }
  return map[activeType.value] || '全部公文'
})

const loadDocuments = async () => {
  try {
    const { data } = await request({
      url: '/approval',
      method: 'get',
      params: { page: 1, size: 10, status: activeType.value || undefined }
    })
    documents.value = data.records || []
  } catch (error) {
    ElMessage.error('加载失败')
  }
}

const handleTypeChange = (type) => {
  activeType.value = type
  loadDocuments()
}

const handleSave = async () => {
  try {
    await request({
      url: '/approval',
      method: 'post',
      data: {
        title: formData.value.title,
        approvalType: formData.value.docType,
        description: formData.value.content,
        urgency: formData.value.urgency === '紧急' ? 'HIGH' : 'NORMAL'
      }
    })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadDocuments()
  } catch (error) {
    ElMessage.error('保存失败')
  }
}

const handleSubmit = async (doc) => {
  try {
    await request({ url: `/approvals/${doc.id}/submit`, method: 'put' })
    ElMessage.success('提交成功')
    loadDocuments()
  } catch (error) {
    ElMessage.error('提交失败')
  }
}

const handleView = (doc) => {
  selectedDoc.value = doc
  detailDialogVisible.value = true
}

const formatStatus = (status) => {
  const map = { 'DRAFT': '草稿', 'PENDING': '待审批', 'APPROVED': '已通过', 'REJECTED': '已驳回' }
  return map[status] || status
}

const getStatusType = (status) => {
  const map = { 'DRAFT': 'info', 'PENDING': 'warning', 'APPROVED': 'success', 'REJECTED': 'danger' }
  return map[status] || ''
}

const formatDate = (date) => {
  if (!date) return ''
  return new Date(date).toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadDocuments()
})
</script>

<style scoped lang="scss">
.doc-item {
  padding: 20px;
  border-bottom: 1px solid #e4e7ed;
}
</style>
