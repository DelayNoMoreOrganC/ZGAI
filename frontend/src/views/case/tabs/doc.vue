<template>
  <div class="case-document-module">
    <aside class="folder-panel">
      <div class="panel-title">
        <span>案件文件夹</span>
        <el-tag size="small" effect="plain">{{ folders.length }} 类</el-tag>
      </div>

      <div
        class="folder-item"
        :class="{ active: selectedFolderPath === '' }"
        @click="selectedFolderPath = ''"
      >
        <el-icon><Files /></el-icon>
        <span>全部文件</span>
        <em>{{ documents.length }}</em>
      </div>

      <div
        v-for="folder in folders"
        :key="folder.id || folder.folderPath"
        class="folder-item"
        :class="{ active: selectedFolderPath === folder.folderPath }"
        @click="selectedFolderPath = folder.folderPath"
      >
        <el-icon><FolderOpened /></el-icon>
        <span>{{ folder.folderName }}</span>
        <em>{{ countByFolder(folder.folderPath) }}</em>
      </div>
    </aside>

    <section class="document-panel">
      <div class="document-toolbar">
        <div>
          <h3>{{ currentFolderLabel }}</h3>
          <p>文件存放于案件专属目录，数据库仅保存元数据和索引状态。</p>
        </div>
        <div class="toolbar-actions">
          <el-input
            v-model="keyword"
            clearable
            placeholder="搜索文件名、类型、标签"
            :prefix-icon="Search"
          />
          <el-button type="primary" @click="openUploadDialog">
            <el-icon><Upload /></el-icon>
            上传文件
          </el-button>
          <el-button @click="refreshAll">
            <el-icon><Refresh /></el-icon>
          </el-button>
        </div>
      </div>

      <el-alert
        v-if="isPendingApproval"
        type="warning"
        show-icon
        :closable="false"
        title="案件尚未审批通过，暂不能上传案件文件。"
      />

      <div v-else class="library-info">
        <div>
          <span>案卷目录</span>
          <strong>{{ caseFolderPath || '审批通过后自动生成' }}</strong>
        </div>
        <el-tag type="info" effect="plain">案件文件默认不进入 AI 知识库</el-tag>
      </div>

      <el-table
        v-loading="loading"
        :data="filteredDocuments"
        class="document-table"
        empty-text="暂无案件文件"
      >
        <el-table-column label="文件名" min-width="260">
          <template #default="{ row }">
            <div class="file-cell">
              <span class="file-badge">{{ getFileBadge(row.documentName) }}</span>
              <div>
                <strong>{{ row.documentName || row.originalFileName }}</strong>
                <small>{{ row.originalFileName }}</small>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="目录" min-width="150">
          <template #default="{ row }">
            {{ row.folderPath || '案件根目录' }}
          </template>
        </el-table-column>

        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.documentType || '其他' }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="版本" width="90" align="center">
          <template #default="{ row }">
            v{{ row.versionNo || 1 }}
          </template>
        </el-table-column>

        <el-table-column label="大小" width="110">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>

        <el-table-column label="上传人" width="120">
          <template #default="{ row }">
            {{ row.uploadByName || row.uploadBy || '-' }}
          </template>
        </el-table-column>

        <el-table-column label="上传时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="索引状态" width="120">
          <template #default="{ row }">
            <el-tag size="small" :type="getIndexStatusType(row.indexStatus)">
              {{ formatIndexStatus(row.indexStatus) }}
            </el-tag>
            <el-tag
              v-if="row.knowledgeEligible"
              class="knowledge-tag"
              size="small"
              type="warning"
              effect="plain"
            >
              可入库
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="downloadFile(row)">下载</el-button>
            <el-button link type="danger" @click="deleteFile(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="uploadDialogVisible" title="上传案件文件" width="560px">
      <el-form label-width="90px">
        <el-form-item label="存放目录">
          <el-select v-model="uploadForm.folderPath" placeholder="请选择目录" style="width: 100%">
            <el-option label="案件根目录" value="" />
            <el-option
              v-for="folder in folders"
              :key="folder.id || folder.folderPath"
              :label="folder.folderName"
              :value="folder.folderPath"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="文件类型">
          <el-select v-model="uploadForm.documentType" placeholder="请选择文件类型" style="width: 100%">
            <el-option
              v-for="type in documentTypes"
              :key="type"
              :label="type"
              :value="type"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="选择文件">
          <el-upload
            drag
            action="#"
            :auto-upload="false"
            :limit="1"
            :file-list="selectedFileList"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
          >
            <el-icon class="el-icon--upload"><Upload /></el-icon>
            <div class="el-upload__text">拖入文件，或点击选择</div>
            <template #tip>
              <div class="el-upload__tip">
                同名文件不会覆盖，系统会自动生成新版本。
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="submitUpload">
          上传
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Files,
  FolderOpened,
  Refresh,
  Search,
  Upload
} from '@element-plus/icons-vue'
import {
  deleteCaseDocument,
  downloadCaseDocument,
  getCaseDocumentFolders,
  getCaseDocuments,
  uploadCaseDocument
} from '@/api/case'

const props = defineProps({
  caseData: {
    type: Object,
    default: () => ({})
  }
})

const loading = ref(false)
const uploading = ref(false)
const uploadDialogVisible = ref(false)
const keyword = ref('')
const selectedFolderPath = ref('')
const folders = ref([])
const documents = ref([])
const selectedFile = ref(null)
const selectedFileList = ref([])

const documentTypes = [
  '立案材料',
  '证据材料',
  '法律文书',
  '合同收费',
  '往来函件',
  '审批归档',
  '其他'
]

const uploadForm = reactive({
  folderPath: '',
  documentType: '其他'
})

const currentCaseId = computed(() => props.caseData?.id)
const caseFolderPath = computed(() => props.caseData?.caseFolderPath || '')
const isPendingApproval = computed(() => props.caseData?.status === 'PENDING_APPROVAL')

const currentFolderLabel = computed(() => {
  if (!selectedFolderPath.value) return '全部案件文件'
  return folders.value.find(folder => folder.folderPath === selectedFolderPath.value)?.folderName || selectedFolderPath.value
})

const filteredDocuments = computed(() => {
  const query = keyword.value.trim().toLowerCase()
  return documents.value.filter(doc => {
    const matchFolder = !selectedFolderPath.value || doc.folderPath === selectedFolderPath.value
    const haystack = [
      doc.documentName,
      doc.originalFileName,
      doc.documentType,
      doc.folderPath,
      doc.tags,
      doc.uploadByName
    ].filter(Boolean).join(' ').toLowerCase()
    return matchFolder && (!query || haystack.includes(query))
  })
})

const refreshAll = async () => {
  if (!currentCaseId.value) return
  loading.value = true
  try {
    const [folderRes, docRes] = await Promise.all([
      getCaseDocumentFolders(currentCaseId.value),
      getCaseDocuments(currentCaseId.value)
    ])
    folders.value = folderRes.data || []
    documents.value = docRes.data || []
  } catch (error) {
    console.error('加载案件文件失败:', error)
    ElMessage.error('加载案件文件失败')
  } finally {
    loading.value = false
  }
}

const openUploadDialog = () => {
  if (isPendingApproval.value) {
    ElMessage.warning('案件尚未审批通过，不能上传案件文件')
    return
  }
  uploadForm.folderPath = selectedFolderPath.value
  uploadForm.documentType = inferDocumentType(selectedFolderPath.value)
  selectedFile.value = null
  selectedFileList.value = []
  uploadDialogVisible.value = true
}

const handleFileChange = (file, fileList) => {
  selectedFile.value = file.raw
  selectedFileList.value = fileList.slice(-1)
}

const handleFileRemove = () => {
  selectedFile.value = null
  selectedFileList.value = []
}

const submitUpload = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  if (!uploadForm.documentType) {
    ElMessage.warning('请选择文件类型')
    return
  }

  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    formData.append('documentType', uploadForm.documentType)
    if (uploadForm.folderPath) {
      formData.append('folderPath', uploadForm.folderPath)
    }
    await uploadCaseDocument(currentCaseId.value, formData)
    ElMessage.success('文件上传成功')
    uploadDialogVisible.value = false
    await refreshAll()
  } catch (error) {
    console.error('上传案件文件失败:', error)
    ElMessage.error(error?.message || '上传案件文件失败')
  } finally {
    uploading.value = false
  }
}

const downloadFile = async (row) => {
  try {
    const response = await downloadCaseDocument(currentCaseId.value, row.id)
    const blob = response.data
    const filename = parseDownloadFilename(response.headers?.['content-disposition']) || row.documentName || row.originalFileName || '案件文件'
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = filename
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } catch (error) {
    console.error('下载案件文件失败:', error)
    ElMessage.error('下载案件文件失败')
  }
}

const deleteFile = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认删除文件“${row.documentName || row.originalFileName}”？文件会移入案件文件库回收区。`,
      '删除案件文件',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }
    )
    await deleteCaseDocument(currentCaseId.value, row.id)
    ElMessage.success('文件已删除')
    await refreshAll()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除案件文件失败:', error)
      ElMessage.error('删除案件文件失败')
    }
  }
}

const countByFolder = (folderPath) => {
  return documents.value.filter(doc => doc.folderPath === folderPath).length
}

const inferDocumentType = (folderPath) => {
  const folder = folders.value.find(item => item.folderPath === folderPath)
  const name = folder?.folderName || ''
  if (name.includes('立案')) return '立案材料'
  if (name.includes('证据')) return '证据材料'
  if (name.includes('法律文书')) return '法律文书'
  if (name.includes('合同') || name.includes('收费')) return '合同收费'
  if (name.includes('往来')) return '往来函件'
  if (name.includes('归档') || name.includes('审批')) return '审批归档'
  return '其他'
}

const getFileBadge = (name = '') => {
  const ext = name.includes('.') ? name.split('.').pop().toUpperCase() : 'FILE'
  return ext.slice(0, 4)
}

const formatFileSize = (bytes) => {
  if (!bytes) return '-'
  const units = ['B', 'KB', 'MB', 'GB']
  let value = Number(bytes)
  let index = 0
  while (value >= 1024 && index < units.length - 1) {
    value /= 1024
    index += 1
  }
  return `${value.toFixed(value >= 10 || index === 0 ? 0 : 1)} ${units[index]}`
}

const formatDateTime = (value) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
}

const formatIndexStatus = (status) => {
  const map = {
    NOT_INDEXED: '未索引',
    PENDING: '待索引',
    INDEXED: '已索引',
    FAILED: '失败',
    FORBIDDEN: '禁止'
  }
  return map[status] || '未索引'
}

const getIndexStatusType = (status) => {
  const map = {
    INDEXED: 'success',
    PENDING: 'warning',
    FAILED: 'danger',
    FORBIDDEN: 'info'
  }
  return map[status] || 'info'
}

const parseDownloadFilename = (disposition = '') => {
  const utf8Match = disposition.match(/filename\*=UTF-8''([^;]+)/)
  if (utf8Match) return decodeURIComponent(utf8Match[1])
  const normalMatch = disposition.match(/filename="?([^"]+)"?/)
  return normalMatch ? normalMatch[1] : ''
}

watch(currentCaseId, () => {
  refreshAll()
}, { immediate: true })
</script>

<style scoped lang="scss">
.case-document-module {
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr);
  gap: 16px;
  min-height: 560px;
}

.folder-panel,
.document-panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.folder-panel {
  padding: 14px;
}

.panel-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.folder-item {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  min-height: 38px;
  padding: 0 10px;
  border-radius: 6px;
  color: #4b5563;
  cursor: pointer;

  span {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  em {
    font-style: normal;
    color: #9ca3af;
    font-size: 12px;
  }

  &:hover,
  &.active {
    background: #f3f4f6;
    color: #111827;
  }
}

.document-panel {
  min-width: 0;
  padding: 16px;
}

.document-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;

  h3 {
    margin: 0;
    font-size: 18px;
    color: #111827;
  }

  p {
    margin: 6px 0 0;
    font-size: 13px;
    color: #6b7280;
  }
}

.toolbar-actions {
  display: grid;
  grid-template-columns: 260px auto 40px;
  gap: 8px;
  align-items: center;
}

.document-table {
  margin-top: 12px;
}

.library-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 12px 0;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f8fafc;

  div {
    min-width: 0;
  }

  span {
    display: block;
    margin-bottom: 3px;
    color: #6b7280;
    font-size: 12px;
  }

  strong {
    display: block;
    color: #111827;
    font-size: 13px;
    font-weight: 600;
    overflow-wrap: anywhere;
  }
}

.knowledge-tag {
  margin-left: 4px;
}

.file-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;

  strong,
  small {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  strong {
    color: #111827;
    font-weight: 600;
  }

  small {
    margin-top: 2px;
    color: #9ca3af;
    font-size: 12px;
  }
}

.file-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 42px;
  width: 42px;
  height: 34px;
  border-radius: 6px;
  background: #eef2ff;
  color: #3730a3;
  font-size: 11px;
  font-weight: 700;
}

:deep(.el-upload) {
  width: 100%;
}

:deep(.el-upload-dragger) {
  width: 100%;
}

@media (max-width: 900px) {
  .case-document-module {
    grid-template-columns: 1fr;
  }

  .toolbar-actions,
  .document-toolbar,
  .library-info {
    display: flex;
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
