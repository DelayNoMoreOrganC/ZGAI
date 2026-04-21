<template>
  <div class="ai-agent-sidebar" :class="{ 'collapsed': isCollapsed }">
    <!-- 折叠按钮 -->
    <div class="toggle-btn" @click="toggleSidebar">
      <el-icon :size="20">
        <component :is="isCollapsed ? 'DArrowRight' : 'DArrowLeft'" />
      </el-icon>
    </div>

    <!-- 侧边栏内容 -->
    <div class="sidebar-content" v-show="!isCollapsed">
      <!-- 头部 -->
      <div class="sidebar-header">
        <div class="header-info">
          <div class="icon">🤖</div>
          <div class="title">
            <h3>AI 助手</h3>
            <p class="subtitle">智能文档识别 · 自动分类</p>
          </div>
        </div>
        <el-button text @click="clearHistory" :disabled="history.length === 0">
          <el-icon><Delete /></el-icon>
        </el-button>
      </div>

      <!-- 文件上传区域 -->
      <div class="upload-section">
        <div
          class="upload-area"
          :class="{ 'drag-over': isDragOver, 'uploading': isUploading }"
          @dragover.prevent="handleDragOver"
          @dragleave.prevent="handleDragLeave"
          @drop.prevent="handleDrop"
          @click="triggerFileInput"
        >
          <input
            ref="fileInputRef"
            type="file"
            accept=".pdf,.jpg,.jpeg,.png,.bmp"
            multiple
            style="display: none"
            @change="handleFileSelect"
          >

          <div class="upload-icon" v-if="!isUploading">
            <el-icon :size="32"><UploadFilled /></el-icon>
          </div>
          <div class="upload-text" v-if="!isUploading">
            <p class="main-text">拖拽文件到这里</p>
            <p class="sub-text">或点击上传 · 支持 PDF/图片</p>
          </div>
          <div class="upload-progress" v-else>
            <el-progress :percentage="uploadProgress" :show-text="false" />
            <p>正在识别文档...</p>
          </div>
        </div>

        <!-- 快捷操作按钮 -->
        <div class="quick-actions">
          <el-button-group>
            <el-button size="small" @click="selectCase" :disabled="!selectedCaseId">
              <el-icon><FolderOpened /></el-icon>
              关联案件
            </el-button>
            <el-button size="small" @click="createTodoFromResult" :disabled="!latestResult">
              <el-icon><Plus /></el-icon>
              创建待办
            </el-button>
          </el-button-group>
        </div>
      </div>

      <!-- 识别结果展示 -->
      <div class="results-section">
        <div class="section-header">
          <h4>识别结果</h4>
          <el-tag size="small" v-if="history.length > 0">{{ history.length }} 条记录</el-tag>
        </div>

        <div class="results-list">
          <div v-if="history.length === 0" class="empty-state">
            <el-empty description="暂无识别结果" :image-size="80" />
          </div>

          <div
            v-for="(item, index) in history"
            :key="index"
            class="result-card"
            :class="{ 'processing': item.processing }"
          >
            <!-- 文件信息 -->
            <div class="file-info">
              <div class="file-icon">
                <el-icon><Document /></el-icon>
              </div>
              <div class="file-details">
                <div class="file-name">{{ item.fileName }}</div>
                <div class="file-time">{{ item.timestamp }}</div>
              </div>
              <div class="file-status">
                <el-tag
                  :type="item.success ? 'success' : 'danger'"
                  size="small"
                >
                  {{ item.success ? '成功' : '失败' }}
                </el-tag>
              </div>
            </div>

            <!-- 识别内容 -->
            <div v-if="item.success && item.data" class="result-content">
              <div class="result-grid">
                <div class="result-item" v-if="item.data.caseNumber">
                  <span class="label">案号:</span>
                  <span class="value">{{ item.data.caseNumber }}</span>
                </div>
                <div class="result-item" v-if="item.data.courtName">
                  <span class="label">法院:</span>
                  <span class="value">{{ item.data.courtName }}</span>
                </div>
                <div class="result-item" v-if="item.data.documentType">
                  <span class="label">文书类型:</span>
                  <span class="value">{{ item.data.documentType }}</span>
                </div>
                <div class="result-item" v-if="item.data.hearingDate">
                  <span class="label">开庭时间:</span>
                  <span class="value">{{ item.data.hearingDate }}</span>
                </div>
                <div class="result-item" v-if="item.data.plaintiffName">
                  <span class="label">原告:</span>
                  <span class="value">{{ item.data.plaintiffName }}</span>
                </div>
                <div class="result-item" v-if="item.data.defendantName">
                  <span class="label">被告:</span>
                  <span class="value">{{ item.data.defendantName }}</span>
                </div>
              </div>

              <!-- 智能建议操作 -->
              <div class="smart-actions">
                <el-button size="small" type="primary" @click="handleRecognizeCase(item)">
                  <el-icon><Search /></el-icon>
                  匹配案件
                </el-button>
                <el-button size="small" type="success" @click="handleCreateTodo(item)">
                  <el-icon><Plus /></el-icon>
                  创建待办
                </el-button>
                <el-button size="small" type="warning" @click="handleSaveToCase(item)">
                  <el-icon><Folder /></el-icon>
                  归档到案件
                </el-button>
              </div>
            </div>

            <!-- 错误信息 -->
            <div v-if="!item.success" class="error-content">
              <el-alert
                :title="item.error || '识别失败'"
                type="error"
                :closable="false"
                show-icon
              />
            </div>
          </div>
        </div>
      </div>

      <!-- AI设置 -->
      <div class="settings-section">
        <el-divider>AI 设置</el-divider>
        <div class="setting-item">
          <span>自动创建待办</span>
          <el-switch v-model="settings.autoTodo" />
        </div>
        <div class="setting-item">
          <span>自动匹配案件</span>
          <el-switch v-model="settings.autoMatchCase" />
        </div>
        <div class="setting-item">
          <span>自动归档</span>
          <el-switch v-model="settings.autoArchive" />
        </div>
      </div>
    </div>

    <!-- 案件选择对话框 -->
    <el-dialog v-model="caseDialogVisible" title="选择关联案件" width="600px">
      <el-input
        v-model="caseSearchKeyword"
        placeholder="搜索案件..."
        prefix-icon="Search"
        clearable
        @input="searchCases"
      />
      <el-table
        :data="filteredCases"
        class="case-table"
        @row-click="selectCaseRow"
        style="margin-top: 15px"
        highlight-current-row
      >
        <el-table-column prop="caseNumber" label="案号" width="150" />
        <el-table-column prop="caseName" label="案件名称" />
        <el-table-column prop="caseType" label="类型" width="80" />
      </el-table>
      <template #footer>
        <el-button @click="caseDialogVisible = false">取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  DArrowLeft, DArrowRight, Delete, UploadFilled,
  FolderOpened, Plus, Document, Search, Folder
} from '@element-plus/icons-vue'
import request from '@/utils/request'
import { getCaseList } from '@/api/case'
import { createTodo } from '@/api/todo'
import { createTimelineComment } from '@/api/case'
import { recognizeLegalDocument } from '@/api/ai'

// 侧边栏状态
const isCollapsed = ref(false)
const isDragOver = ref(false)
const isUploading = ref(false)
const uploadProgress = ref(0)

// 文件输入
const fileInputRef = ref(null)

// 历史记录
const history = ref([])
const latestResult = ref(null)

// 案件选择
const caseDialogVisible = ref(false)
const selectedCaseId = ref(null)
const caseSearchKeyword = ref('')
const filteredCases = ref([])

// AI设置
const settings = ref({
  autoTodo: true,
  autoMatchCase: true,
  autoArchive: false
})

// 切换侧边栏
const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

// 清除历史
const clearHistory = () => {
  ElMessageBox.confirm('确定要清除所有识别记录吗？', '提示', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(() => {
    history.value = []
    latestResult.value = null
    ElMessage.success('已清除所有记录')
  }).catch(() => {})
}

// 拖拽处理
const handleDragOver = (e) => {
  e.preventDefault()
  isDragOver.value = true
}

const handleDragLeave = (e) => {
  e.preventDefault()
  isDragOver.value = false
}

const handleDrop = async (e) => {
  e.preventDefault()
  isDragOver.value = false

  const files = e.dataTransfer.files
  if (files.length > 0) {
    await processFiles(files)
  }
}

// 文件选择
const triggerFileInput = () => {
  fileInputRef.value.click()
}

const handleFileSelect = async (e) => {
  const files = e.target.files
  if (files.length > 0) {
    await processFiles(files)
  }
  // 重置input，允许重复选择同一文件
  e.target.value = ''
}

// 处理文件
const processFiles = async (files) => {
  if (isUploading.value) {
    ElMessage.warning('正在处理中，请稍候')
    return
  }

  const fileArray = Array.from(files)
  if (fileArray.length > 5) {
    ElMessage.warning('单次最多处理5个文件')
    return
  }

  isUploading.value = true
  uploadProgress.value = 0

  try {
    for (let i = 0; i < fileArray.length; i++) {
      const file = fileArray[i]
      uploadProgress.value = Math.round(((i + 1) / fileArray.length) * 100)

      await recognizeDocument(file)
    }

    ElMessage.success(`成功处理 ${fileArray.length} 个文件`)
  } catch (error) {
    console.error('文件处理失败:', error)
    ElMessage.error('文件处理失败')
  } finally {
    isUploading.value = false
    uploadProgress.value = 0
  }
}

// 识别文档
const recognizeDocument = async (file) => {
  const record = {
    fileName: file.name,
    timestamp: new Date().toLocaleString(),
    processing: true,
    success: false,
    data: null,
    error: null
  }

  history.value.unshift(record)

  try {
    const formData = new FormData()
    formData.append('file', file)

    if (selectedCaseId.value) {
      formData.append('caseId', selectedCaseId.value)
    }

    const response = await recognizeLegalDocument(file, selectedCaseId.value)

    if (response.success && response.data) {
      record.success = true
      record.data = response.data
      record.processing = false
      latestResult.value = response.data

      // 智能操作
      await performSmartActions(response.data)

      ElMessage.success(`${file.name} 识别成功`)
    } else {
      throw new Error(response.message || '识别失败')
    }
  } catch (error) {
    record.success = false
    record.processing = false
    record.error = error.message
    console.error('文档识别失败:', error)
  }
}

// 智能操作
const performSmartActions = async (result) => {
  try {
    // 自动匹配案件
    if (settings.value.autoMatchCase && result.caseNumber) {
      await matchCaseByNumber(result.caseNumber)
    }

    // 自动创建待办
    if (settings.value.autoTodo && result.hearingDate) {
      await createTodoFromResultData(result)
    }

    // 自动归档
    if (settings.value.autoArchive && selectedCaseId.value) {
      await archiveToCase(result)
    }
  } catch (error) {
    console.error('智能操作失败:', error)
  }
}

// 匹配案件
const matchCaseByNumber = async (caseNumber) => {
  try {
    const response = await getCaseList({
      page: 0,
      size: 100,
      keyword: caseNumber
    })

    if (response.data && response.data.records) {
      const matched = response.data.records.find(c => c.caseNumber === caseNumber)
      if (matched) {
        selectedCaseId.value = matched.id
        ElMessage.success(`已匹配案件：${matched.caseName}`)
        return matched
      }
    }
  } catch (error) {
    console.error('案件匹配失败:', error)
  }
}

// 创建待办
const createTodoFromResultData = async (result) => {
  try {
    const todoData = {
      title: `${result.documentType || '文书'}处理 - ${result.caseNumber || '未定案号'}`,
      content: `识别到${result.documentType || '文书'}信息：
案号：${result.caseNumber || '未识别'}
法院：${result.courtName || '未识别'}
开庭时间：${result.hearingDate || '未识别'}
原告：${result.plaintiffName || '未识别'}
被告：${result.defendantName || '未识别'}`,
      priority: result.hearingDate ? 'high' : 'medium',
      status: 'pending',
      dueDate: result.hearingDate || null,
      caseId: selectedCaseId.value || null
    }

    await createTodo(todoData)
    ElMessage.success('已自动创建待办事项')
  } catch (error) {
    console.error('创建待办失败:', error)
  }
}

// 归档到案件
const archiveToCase = async (result) => {
  if (!selectedCaseId.value) {
    ElMessage.warning('请先选择关联案件')
    return
  }

  try {
    // 在案件进度日志中记录
    const comment = `系统自动识别并归档文档：
- 文书类型：${result.documentType || '未识别'}
- 案号：${result.caseNumber || '未识别'}
- 法院：${result.courtName || '未识别'}
- 识别时间：${new Date().toLocaleString()}`

    await createTimelineComment(selectedCaseId.value, {
      content: comment,
      mentionIds: []
    })

    ElMessage.success('文档已归档到案件并记录日志')
  } catch (error) {
    console.error('归档失败:', error)
    ElMessage.error('归档失败')
  }
}

// 选择案件
const selectCase = () => {
  caseDialogVisible.value = true
  searchCases()
}

const searchCases = async () => {
  try {
    const response = await getCaseList({
      page: 0,
      size: 100,
      keyword: caseSearchKeyword.value
    })

    if (response.data && response.data.records) {
      filteredCases.value = response.data.records
    }
  } catch (error) {
    console.error('搜索案件失败:', error)
    ElMessage.error('搜索案件失败')
  }
}

const selectCaseRow = (row) => {
  selectedCaseId.value = row.id
  caseDialogVisible.value = false
  ElMessage.success(`已关联案件：${row.caseName}`)
}

// 手动操作按钮
const handleRecognizeCase = async (item) => {
  if (item.data && item.data.caseNumber) {
    await matchCaseByNumber(item.data.caseNumber)
  } else {
    ElMessage.warning('该识别结果中没有案号信息')
  }
}

const handleCreateTodo = async (item) => {
  if (item.data) {
    await createTodoFromResultData(item.data)
  }
}

const handleSaveToCase = async (item) => {
  if (!selectedCaseId.value) {
    ElMessage.warning('请先选择关联案件')
    selectCase()
    return
  }

  if (item.data) {
    await archiveToCase(item.data)
  }
}

const createTodoFromResult = async () => {
  if (latestResult.value) {
    await createTodoFromResultData(latestResult.value)
  } else {
    ElMessage.warning('没有可用的识别结果')
  }
}

// 初始化
onMounted(() => {
  // 加载设置
  const savedSettings = localStorage.getItem('ai-agent-settings')
  if (savedSettings) {
    try {
      Object.assign(settings.value, JSON.parse(savedSettings))
    } catch (e) {
      console.error('加载设置失败:', e)
    }
  }
})

// 监听设置变化并保存
import { watch } from 'vue'
watch(settings, (newSettings) => {
  localStorage.setItem('ai-agent-settings', JSON.stringify(newSettings))
}, { deep: true })
</script>

<style scoped lang="scss">
.ai-agent-sidebar {
  position: fixed;
  right: 0;
  top: 0;
  height: 100vh;
  width: 350px;
  background: #fff;
  box-shadow: -2px 0 8px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;

  &.collapsed {
    width: 40px;

    .sidebar-content {
      display: none;
    }
  }

  .toggle-btn {
    position: absolute;
    left: -20px;
    top: 50%;
    transform: translateY(-50%);
    width: 20px;
    height: 60px;
    background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
    border-radius: 8px 0 0 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: #fff;
    transition: all 0.3s;

    &:hover {
      background: linear-gradient(135deg, #40a9ff 0%, #1890ff 100%);
    }
  }

  .sidebar-content {
    height: 100%;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }

  .sidebar-header {
    padding: 20px;
    border-bottom: 1px solid #f0f0f0;
    background: linear-gradient(135deg, #f0f5ff 0%, #ffffff 100%);

    .header-info {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 10px;

      .icon {
        font-size: 28px;
      }

      .title {
        h3 {
          margin: 0;
          font-size: 16px;
          color: #333;
          font-weight: 600;
        }

        .subtitle {
          margin: 2px 0 0 0;
          font-size: 12px;
          color: #999;
        }
      }
    }
  }

  .upload-section {
    padding: 20px;
    border-bottom: 1px solid #f0f0f0;

    .upload-area {
      border: 2px dashed #d9d9d9;
      border-radius: 8px;
      padding: 30px 20px;
      text-align: center;
      cursor: pointer;
      transition: all 0.3s;
      background: #fafafa;

      &:hover {
        border-color: #1890ff;
        background: #f0f5ff;
      }

      &.drag-over {
        border-color: #1890ff;
        background: #e6f7ff;
        transform: scale(1.02);
      }

      &.uploading {
        border-style: solid;
        border-color: #1890ff;
        background: #f0f5ff;
      }

      .upload-icon {
        color: #1890ff;
        margin-bottom: 10px;
      }

      .upload-text {
        .main-text {
          margin: 0 0 5px 0;
          font-size: 14px;
          color: #333;
          font-weight: 500;
        }

        .sub-text {
          margin: 0;
          font-size: 12px;
          color: #999;
        }
      }

      .upload-progress {
        p {
          margin: 10px 0 0 0;
          font-size: 12px;
          color: #666;
        }
      }
    }

    .quick-actions {
      margin-top: 15px;
      display: flex;
      justify-content: center;

      .el-button-group {
        display: flex;
        gap: 8px;
      }
    }
  }

  .results-section {
    flex: 1;
    overflow: hidden;
    display: flex;
    flex-direction: column;

    .section-header {
      padding: 15px 20px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid #f0f0f0;

      h4 {
        margin: 0;
        font-size: 14px;
        color: #333;
        font-weight: 600;
      }
    }

    .results-list {
      flex: 1;
      overflow-y: auto;
      padding: 10px 20px;

      .empty-state {
        text-align: center;
        padding: 40px 20px;
      }

      .result-card {
        background: #fff;
        border: 1px solid #f0f0f0;
        border-radius: 8px;
        margin-bottom: 15px;
        overflow: hidden;

        &.processing {
          opacity: 0.7;
        }

        .file-info {
          display: flex;
          align-items: center;
          gap: 10px;
          padding: 12px;
          background: #fafafa;
          border-bottom: 1px solid #f0f0f0;

          .file-icon {
            color: #1890ff;
          }

          .file-details {
            flex: 1;
            min-width: 0;

            .file-name {
              font-size: 13px;
              color: #333;
              font-weight: 500;
              overflow: hidden;
              text-overflow: ellipsis;
              white-space: nowrap;
            }

            .file-time {
              font-size: 11px;
              color: #999;
              margin-top: 2px;
            }
          }
        }

        .result-content {
          padding: 12px;

          .result-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 8px;
            margin-bottom: 12px;

            .result-item {
              font-size: 12px;
              display: flex;
              flex-direction: column;
              gap: 2px;

              .label {
                color: #999;
                font-size: 11px;
              }

              .value {
                color: #333;
                font-weight: 500;
                word-break: break-all;
              }
            }
          }

          .smart-actions {
            display: flex;
            gap: 8px;
            flex-wrap: wrap;

            .el-button {
              flex: 1;
              min-width: 80px;
            }
          }
        }

        .error-content {
          padding: 12px;
        }
      }
    }
  }

  .settings-section {
    padding: 15px 20px;
    border-top: 1px solid #f0f0f0;
    background: #fafafa;

    .setting-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      &:last-child {
        margin-bottom: 0;
      }

      span {
        font-size: 13px;
        color: #666;
      }
    }
  }
}

.case-table {
  :deep(.el-table__row) {
    cursor: pointer;
  }
}
</style>
