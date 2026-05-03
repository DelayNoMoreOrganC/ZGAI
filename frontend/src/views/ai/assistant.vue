<template>
  <div class="ai-assistant">
    <!-- 最小化后的悬浮按钮 -->
    <el-button
      v-if="minimized"
      type="primary"
      circle
      size="large"
      class="ai-mini-btn"
      @click="minimized = false"
      title="打开AI法律助手"
    >
      <el-icon style="font-size:24px"><ChatDotRound /></el-icon>
    </el-button>

    <el-dialog
      v-model="dialogVisible"
      width="800px"
      :close-on-click-modal="false"
      class="ai-chat-dialog"
      :style="minimized ? 'display:none' : ''"
    >
      <template #header="{ close }">
        <div class="custom-dialog-header">
          <span class="dialog-title">🤖 AI法律助手</span>
          <div class="header-actions">
            <el-button text class="header-btn" @click="minimized = true" title="最小化">
              <el-icon><Minus /></el-icon>
            </el-button>
            <el-button text class="header-btn" @click="close" title="关闭">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>
      </template>
      <div class="chat-container">
        <!-- 模式切换 -->
        <div class="chat-header">
          <el-radio-group v-model="chatMode" @change="handleModeChange">
            <el-radio-button label="general">通用问答</el-radio-button>
            <el-radio-button label="case" :disabled="!currentCaseId">案件问答</el-radio-button>
          </el-radio-group>

          <el-select
            v-if="chatMode === 'case'"
            v-model="selectedCaseId"
            placeholder="选择案件"
            filterable
            style="width: 300px; margin-left: 10px"
            @change="handleCaseChange"
          >
            <el-option
              v-for="caseItem in caseList"
              :key="caseItem.id"
              :label="caseItem.name"
              :value="caseItem.id"
            />
          </el-select>
        </div>

        <!-- 聊天消息区域 -->
        <div class="chat-messages" ref="messagesContainer">
          <div
            v-for="(message, index) in messages"
            :key="index"
            class="message-item"
            :class="message.role"
          >
            <div class="message-avatar">
              <el-avatar v-if="message.role === 'user'" :size="32">
                {{ userName?.charAt(0) }}
              </el-avatar>
              <el-icon v-else class="ai-avatar"><ChatDotRound /></el-icon>
            </div>
            <div class="message-content">
              <div class="message-text" v-html="formatMessage(message.content)"></div>
              <div class="message-time">{{ message.time }}</div>
            </div>
          </div>

          <div v-if="loading" class="message-item assistant">
            <div class="message-avatar">
              <el-icon class="ai-avatar"><ChatDotRound /></el-icon>
            </div>
            <div class="message-content">
              <div class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="chat-input">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :rows="3"
            placeholder="请输入您的法律问题..."
            @keydown.enter.ctrl="handleSend"
            :disabled="loading"
          />
          <div class="input-actions">
            <span class="input-hint">Ctrl + Enter 发送</span>
            <el-button
              type="primary"
              :loading="loading"
              :disabled="!inputMessage.trim()"
              @click="handleSend"
            >
              发送
            </el-button>
          </div>
        </div>

        <!-- 快捷问题 -->
        <div class="quick-questions" v-if="messages.length === 0">
          <div class="quick-title">快捷问题：</div>
          <div class="quick-list">
            <el-tag
              v-for="(question, index) in quickQuestions"
              :key="index"
              @click="handleQuickQuestion(question)"
              class="quick-tag"
            >
              {{ question }}
            </el-tag>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Minus, Close } from '@element-plus/icons-vue'
import { aiChat, caseChat } from '@/api/ai'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  caseId: {
    type: Number,
    default: null
  },
  caseList: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:visible'])

const userStore = useUserStore()
const userName = computed(() => userStore.userName)

// 对话框可见性
const dialogVisible = computed({
  get: () => props.visible,
  set: (value) => emit('update:visible', value)
})

// 聊天模式
const chatMode = ref(props.caseId ? 'case' : 'general')
const selectedCaseId = ref(props.caseId)
const currentCaseId = computed(() => chatMode.value === 'case' ? selectedCaseId.value : null)

// 消息列表
const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const minimized = ref(false)
const messagesContainer = ref(null)

// 快捷问题
const quickQuestions = ref([
  '如何起草一份起诉状？',
  '合同违约应该怎么处理？',
  '离婚案件需要准备什么材料？',
  '劳动仲裁的流程是什么？'
])

// 监听props变化
watch(() => props.caseId, (newCaseId) => {
  if (newCaseId) {
    chatMode.value = 'case'
    selectedCaseId.value = newCaseId
  }
})

// 模式切换
const handleModeChange = (mode) => {
  if (mode === 'case' && !selectedCaseId.value) {
    ElMessage.warning('请先选择案件')
    return
  }
  // 清空消息或保留消息根据需求
  if (messages.value.length > 0) {
    messages.value = []
  }
}

// 案件切换
const handleCaseChange = (caseId) => {
  // 可以加载案件相关的历史对话
  selectedCaseId.value = caseId
}

// 发送消息
const handleSend = async () => {
  if (!inputMessage.value.trim() || loading.value) {
    return
  }

  const userMessage = inputMessage.value.trim()
  inputMessage.value = ''

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: userMessage,
    time: formatTime(new Date())
  })

  // 滚动到底部
  await nextTick()
  scrollToBottom()

  try {
    loading.value = true

    let response
    if (chatMode.value === 'case') {
      // 案件问答
      response = await caseChat(selectedCaseId.value, {
        message: userMessage,
        caseId: selectedCaseId.value
      })
    } else {
      // 通用问答
      response = await aiChat({
        message: userMessage
      })
    }

    // 检查 aiChat 是否返回了错误
    if (response && response.success === false) {
      throw new Error(response.message || 'AI响应异常')
    }

    // 添加AI回复
    messages.value.push({
      role: 'assistant',
      content: response.data || response,
      time: formatTime(new Date())
    })

  } catch (error) {
    console.error('AI问答失败:', error)
    ElMessage.error('AI问答失败：' + (error.message || '未知错误'))

    // 添加错误消息
    messages.value.push({
      role: 'assistant',
      content: '抱歉，AI助手暂时无法回答您的问题。请稍后再试。',
      time: formatTime(new Date())
    })
  } finally {
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

// 快捷问题
const handleQuickQuestion = (question) => {
  inputMessage.value = question
  handleSend()
}

// 格式化消息
const formatMessage = (content) => {
  if (!content) return ''
  // 将换行符转换为HTML换行
  return content
    .replace(/\n/g, '<br>')
    .replace(/ /g, '&nbsp;')
}

// 格式化时间
const formatTime = (date) => {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  }).format(date)
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}
</script>

<style scoped lang="scss">
.ai-chat-dialog {
  :deep(.el-dialog__body) {
    padding: 0;
  }
}

.chat-container {
  display: flex;
  flex-direction: column;
  height: 600px;
}

.chat-header {
  display: flex;
  align-items: center;
  padding: 15px 20px;
  border-bottom: 1px solid #f0f0f0;
  background-color: #fafafa;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background-color: #f5f7fa;
}

.message-item {
  display: flex;
  margin-bottom: 20px;
  align-items: flex-start;

  &.user {
    flex-direction: row-reverse;

    .message-content {
      align-items: flex-end;
    }

    .message-text {
      background-color: #1890ff;
      color: #fff;
    }
  }

  &.assistant {
    .message-text {
      background-color: #fff;
      color: #333;
    }
  }
}

.message-avatar {
  flex-shrink: 0;
  margin: 0 10px;

  .ai-avatar {
    font-size: 24px;
    display: block;
  }
}

.message-content {
  display: flex;
  flex-direction: column;
  max-width: 70%;
}

.message-text {
  padding: 10px 15px;
  border-radius: 8px;
  word-wrap: break-word;
  line-height: 1.6;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.message-time {
  font-size: 12px;
  color: #999;
  margin-top: 5px;
  padding: 0 5px;
}

.typing-indicator {
  display: flex;
  gap: 5px;
  padding: 15px;

  span {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background-color: #1890ff;
    animation: typing 1.4s infinite ease-in-out;

    &:nth-child(1) {
      animation-delay: -0.32s;
    }

    &:nth-child(2) {
      animation-delay: -0.16s;
    }
  }
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.chat-input {
  padding: 15px 20px;
  border-top: 1px solid #f0f0f0;
  background-color: #fff;

  .input-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 10px;
  }

  .input-hint {
    font-size: 12px;
    color: #999;
  }
}

.quick-questions {
  padding: 15px 20px;
  border-top: 1px solid #f0f0f0;
  background-color: #fafafa;

  .quick-title {
    font-size: 14px;
    color: #666;
    margin-bottom: 10px;
  }

  .quick-list {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
  }

  .quick-tag {
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      background-color: #1890ff;
      color: #fff;
      border-color: #1890ff;
    }
  }
}

/* AI悬浮小球按钮 */
.ai-mini-btn {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
  width: 56px !important;
  height: 56px !important;
  box-shadow: 0 4px 16px rgba(24, 144, 255, 0.4);
  transition: all 0.3s;

  &:hover {
    transform: scale(1.1);
    box-shadow: 0 6px 24px rgba(24, 144, 255, 0.6);
  }
}

/* 自定义对话框标题栏 */
.custom-dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 4px;

  .dialog-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }

  .header-actions {
    display: flex;
    gap: 4px;

    .header-btn {
      width: 32px;
      height: 32px;
      padding: 4px;
      border-radius: 4px;
      display: flex;
      align-items: center;
      justify-content: center;

      &:hover {
        background-color: #f0f2f5;
      }
    }
  }
}

// 移动端适配
@media (max-width: 768px) {
  .ai-chat-dialog {
    :deep(.el-dialog) {
      width: 95% !important;
      margin: 0 auto;
    }
  }

  .chat-container {
    height: 500px;
  }

  .message-content {
    max-width: 85%;
  }

  .chat-header {
    flex-direction: column;
    align-items: stretch;
    gap: 10px;

    .el-select {
      width: 100% !important;
      margin-left: 0 !important;
    }
  }
}
</style>
