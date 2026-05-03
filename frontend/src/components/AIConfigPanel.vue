<template>
  <div class="ai-config-panel">
    <div class="config-section">
      <h4>AI 模型配置</h4>
      <p class="config-tip">选择 AI 模式：DeepSeek（云端）或 Ollama（本地），二选一。切换后自动保存默认模式。</p>

      <!-- 模式切换开关 -->
      <div class="mode-toggle-wrapper">
        <el-radio-group v-model="activeMode" size="large" @change="handleModeSwitch">
          <el-radio-button value="DEEPSEEK_API" class="mode-option deepseek-option">
            <div class="mode-option-content">
              <el-icon size="22" color="#409EFF"><Cloudy /></el-icon>
              <div class="mode-text">
                <span class="mode-name">DeepSeek（云端）</span>
                <span class="mode-desc">deepseek-v4-flash · 高速推理</span>
              </div>
              <el-tag v-if="deepseekConfig?.isDefault" type="warning" size="small" effect="dark" class="default-badge">当前使用</el-tag>
            </div>
          </el-radio-button>
          <el-radio-button value="LOCAL_QWEN" class="mode-option ollama-option">
            <div class="mode-option-content">
              <el-icon size="22" color="#67C23A"><Monitor /></el-icon>
              <div class="mode-text">
                <span class="mode-name">Ollama（本地）</span>
                <span class="mode-desc">qwen3:8b · 本地部署</span>
              </div>
              <el-tag v-if="ollamaConfig?.isDefault" type="warning" size="small" effect="dark" class="default-badge">当前使用</el-tag>
            </div>
          </el-radio-button>
        </el-radio-group>
      </div>

      <!-- DeepSeek 配置面板 -->
      <el-card v-show="activeMode === 'DEEPSEEK_API'" shadow="never" class="config-detail-card">
        <div class="detail-header">
          <div class="detail-title">
            <el-icon size="18" color="#409EFF"><Cloudy /></el-icon>
            <span>DeepSeek 云端配置</span>
          </div>
        </div>
        <el-form label-position="top" size="small" class="config-form">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="API 地址">
                <el-input v-model="deepseekForm.apiUrl" placeholder="https://api.deepseek.com/v1/chat/completions" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="API Key">
                <el-input v-model="deepseekForm.apiKey" type="password" show-password placeholder="sk-xxxxxxxx" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="模型名称">
                <el-input v-model="deepseekForm.modelName" placeholder="deepseek-v4-flash" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="Temperature">
                <el-slider v-model="deepseekForm.temperature" :min="0" :max="2" :step="0.1" show-input size="small" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="Max Tokens">
                <el-input-number v-model="deepseekForm.maxTokens" :min="256" :max="32768" :step="512" style="width:100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
        <div class="detail-actions">
          <el-button :loading="deepseekTesting" @click="testConnection(deepseekConfig?.id)">
            <el-icon><Connection /></el-icon> 测试连接
          </el-button>
          <el-button type="primary" @click="saveConfig(deepseekConfig?.id, deepseekForm)">
            保存 DeepSeek 配置
          </el-button>
        </div>
        <div v-if="deepseekTestResult" class="test-result" :class="deepseekTestResult.status">
          <el-icon v-if="deepseekTestResult.status === 'ok'"><SuccessFilled /></el-icon>
          <el-icon v-else><WarningFilled /></el-icon>
          <span>{{ deepseekTestResult.message }} ({{ deepseekTestResult.duration }}ms)</span>
        </div>
      </el-card>

      <!-- Ollama 配置面板 -->
      <el-card v-show="activeMode === 'LOCAL_QWEN'" shadow="never" class="config-detail-card">
        <div class="detail-header">
          <div class="detail-title">
            <el-icon size="18" color="#67C23A"><Monitor /></el-icon>
            <span>Ollama 本地配置</span>
          </div>
        </div>
        <el-form label-position="top" size="small" class="config-form">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="API 地址">
                <el-input v-model="ollamaForm.apiUrl" placeholder="http://localhost:11434" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="模型名称">
                <el-input v-model="ollamaForm.modelName" placeholder="qwen3:8b" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="Temperature">
                <el-slider v-model="ollamaForm.temperature" :min="0" :max="2" :step="0.1" show-input size="small" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="Max Tokens">
                <el-input-number v-model="ollamaForm.maxTokens" :min="256" :max="32768" :step="512" style="width:100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
        <div class="detail-actions">
          <el-button :loading="ollamaTesting" @click="testConnection(ollamaConfig?.id)">
            <el-icon><Connection /></el-icon> 测试连接
          </el-button>
          <el-button type="primary" @click="saveConfig(ollamaConfig?.id, ollamaForm)">
            保存 Ollama 配置
          </el-button>
        </div>
        <div v-if="ollamaTestResult" class="test-result" :class="ollamaTestResult.status">
          <el-icon v-if="ollamaTestResult.status === 'ok'"><SuccessFilled /></el-icon>
          <el-icon v-else><WarningFilled /></el-icon>
          <span>{{ ollamaTestResult.message }} ({{ ollamaTestResult.duration }}ms)</span>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Cloudy, Monitor, Connection, SuccessFilled, WarningFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'

// ========== Data ==========
const configs = ref([])
const deepseekTesting = ref(false)
const ollamaTesting = ref(false)
const deepseekTestResult = ref(null)
const ollamaTestResult = ref(null)

// ========== Computed ==========
const deepseekConfig = computed(() => configs.value.find(c => c.providerType === 'DEEPSEEK_API'))
const ollamaConfig = computed(() => configs.value.find(c => c.providerType === 'LOCAL_QWEN'))
const activeMode = ref('DEEPSEEK_API') // 当前激活的模式

// Watch for config changes to update activeMode
watch([deepseekConfig, ollamaConfig], () => {
  if (deepseekConfig.value?.isDefault) {
    activeMode.value = 'DEEPSEEK_API'
  } else if (ollamaConfig.value?.isDefault) {
    activeMode.value = 'LOCAL_QWEN'
  }
}, { immediate: true })

// DeepSeek form
const deepseekForm = ref({
  apiUrl: 'https://api.deepseek.com/v1/chat/completions',
  apiKey: '',
  modelName: 'deepseek-v4-flash',
  temperature: 0.3,
  maxTokens: 8192,
  isEnabled: true,
  isDefault: true,
  configName: 'DeepSeek API（云端）',
  providerType: 'DEEPSEEK_API',
  category: 'LEGAL_CHAT',
  description: 'DeepSeek云端API（v4-flash），用于智能法律问答和文档分析。',
  timeoutSeconds: 60
})

// Ollama form
const ollamaForm = ref({
  apiUrl: 'http://localhost:11434',
  apiKey: '',
  modelName: 'qwen3:8b',
  temperature: 0.1,
  maxTokens: 4096,
  isEnabled: true,
  isDefault: false,
  configName: 'Ollama本地配置',
  providerType: 'LOCAL_QWEN',
  category: 'DOCUMENT',
  description: 'Ollama本地模型，用于本地AI文档识别。',
  timeoutSeconds: 60
})

// ========== Methods ==========
async function loadConfigs() {
  try {
    const { data } = await request({ url: '/ai/config', method: 'get' })
    configs.value = data || []
    syncFormFromConfig(deepseekConfig.value, deepseekForm.value)
    syncFormFromConfig(ollamaConfig.value, ollamaForm.value)
  } catch (e) {
    console.error('加载AI配置失败:', e)
  }
}

function syncFormFromConfig(config, form) {
  if (!config) return
  form.apiUrl = config.apiUrl || form.apiUrl
  form.apiKey = config.apiKey || ''
  form.modelName = config.modelName || form.modelName
  form.temperature = config.temperature ?? form.temperature
  form.maxTokens = config.maxTokens ?? form.maxTokens
  form.isEnabled = config.isEnabled ?? true
  form.isDefault = config.isDefault ?? false
}

async function saveConfig(id, form) {
  try {
    const payload = { ...form }
    await request({ url: `/ai/config/${id}`, method: 'put', data: payload })
    ElMessage.success('配置已保存')
    await loadConfigs()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || '未知错误'))
  }
}

async function handleModeSwitch(mode) {
  const target = mode === 'DEEPSEEK_API' ? deepseekConfig.value : ollamaConfig.value
  if (!target) {
    ElMessage.warning('该模式配置尚未创建')
    return
  }
  if (target.isDefault) {
    // Already default, no change needed
    return
  }
  try {
    await ElMessageBox.confirm(
      `切换到 ${mode === 'DEEPSEEK_API' ? 'DeepSeek（云端）' : 'Ollama（本地）'} 模式？`,
      '切换 AI 模式',
      { confirmButtonText: '确认切换', cancelButtonText: '取消', type: 'info' }
    )
    // Set all to not default first
    for (const c of configs.value) {
      if (c.id !== target.id && c.isDefault) {
        await request({ url: `/ai/config/${c.id}`, method: 'put', data: { ...c, isDefault: false } })
      }
    }
    // Set chosen as default
    await request({ url: `/ai/config/${target.id}`, method: 'put', data: { ...target, isDefault: true } })
    ElMessage.success(`已切换到 ${mode === 'DEEPSEEK_API' ? 'DeepSeek' : 'Ollama'} 模式`)
    await loadConfigs()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('切换失败: ' + (e.message || '未知错误'))
    }
    // Revert toggle if cancelled
    if (mode === 'DEEPSEEK_API') {
      activeMode.value = 'LOCAL_QWEN'
    } else {
      activeMode.value = 'DEEPSEEK_API'
    }
  }
}

async function testConnection(id) {
  if (!id) {
    ElMessage.warning('请先保存配置再测试')
    return
  }
  const isDeepseek = id === deepseekConfig.value?.id
  if (isDeepseek) deepseekTesting.value = true
  else ollamaTesting.value = true
  try {
    const { data } = await request({ url: `/ai/config/test/${id}`, method: 'post' })
    const result = data || { status: 'error', message: '无法连接', duration: 0 }
    if (isDeepseek) deepseekTestResult.value = result
    else ollamaTestResult.value = result
    if (result.status === 'ok') {
      ElMessage.success(`✅ 连接成功 (${result.duration}ms)`)
    } else {
      ElMessage.warning(`⚠️ ${result.message}`)
    }
  } catch (e) {
    const result = { status: 'error', message: e.message || '请求失败', duration: 0 }
    if (isDeepseek) deepseekTestResult.value = result
    else ollamaTestResult.value = result
  } finally {
    deepseekTesting.value = false
    ollamaTesting.value = false
  }
}

onMounted(() => {
  loadConfigs()
})
</script>

<style scoped>
.ai-config-panel {
  padding: 16px 0;
}

.config-tip {
  color: #909399;
  font-size: 13px;
  margin-bottom: 24px;
}

/* ====== 模式切换 ====== */
.mode-toggle-wrapper {
  margin-bottom: 24px;
}

.mode-toggle-wrapper :deep(.el-radio-group) {
  display: flex;
  gap: 16px;
  width: 100%;
}

.mode-toggle-wrapper :deep(.el-radio-button) {
  flex: 1;
}

.mode-toggle-wrapper :deep(.el-radio-button__inner) {
  width: 100%;
  padding: 16px 20px;
  border-radius: 12px !important;
  border: 2px solid #e4e7ed;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  transition: all 0.3s;
}

.mode-toggle-wrapper :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  border-color: #409eff;
  box-shadow: 0 4px 16px rgba(64,158,255,0.15);
  background: #f0f7ff;
}

.mode-toggle-wrapper :deep(.el-radio-button:last-child .el-radio-button__original-radio:checked + .el-radio-button__inner) {
  border-color: #67c23a;
  box-shadow: 0 4px 16px rgba(103,194,58,0.15);
  background: #f0f9eb;
}

.mode-option-content {
  display: flex;
  align-items: center;
  gap: 12px;
  text-align: left;
}

.mode-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.mode-name {
  font-weight: 600;
  font-size: 15px;
  color: #303133;
}

.mode-desc {
  font-size: 12px;
  color: #909399;
}

.default-badge {
  margin-left: auto;
}

/* ====== 配置详情面板 ====== */
.config-detail-card {
  border: 1px solid #e4e7ed;
  border-radius: 12px;
  transition: all 0.3s;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  margin-bottom: 8px;
  border-bottom: 1px solid #ebeef5;
}

.detail-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  font-size: 15px;
}

.config-form {
  margin-bottom: 8px;
}

:deep(.el-form-item) {
  margin-bottom: 16px;
}

.detail-actions {
  display: flex;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.test-result {
  margin-top: 12px;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}
.test-result.ok {
  background: #f0f9eb;
  color: #67c23a;
}
.test-result.error {
  background: #fef0f0;
  color: #f56c6c;
}
</style>
