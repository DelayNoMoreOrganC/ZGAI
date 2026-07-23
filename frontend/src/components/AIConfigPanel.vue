<template>
  <div class="ai-config-panel">
    <div class="config-section">
      <h4>AI 模型配置</h4>
      <p class="config-tip">选择一个生成模型。LM Studio 通过局域网调用本地模型，业务数据不会发送到云端。</p>

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
          <el-radio-button value="LM_STUDIO" class="mode-option lmstudio-option">
            <div class="mode-option-content">
              <el-icon size="22" color="#3378b7"><Cpu /></el-icon>
              <div class="mode-text">
                <span class="mode-name">LM Studio（局域网）</span>
                <span class="mode-desc">Qwen3.6 35B · 本地推理</span>
              </div>
              <el-tag v-if="lmStudioConfig?.isDefault" type="warning" size="small" effect="dark" class="default-badge">当前使用</el-tag>
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
                <el-input
                  v-model="deepseekForm.apiKey"
                  type="password"
                  show-password
                  :placeholder="deepseekForm.apiKeyConfigured ? '已配置，留空则保留' : '请输入 API Key'"
                />
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

      <el-card v-show="activeMode === 'LM_STUDIO'" shadow="never" class="config-detail-card">
        <div class="detail-header">
          <div class="detail-title">
            <el-icon size="18" color="#3378b7"><Cpu /></el-icon>
            <span>LM Studio 局域网配置</span>
          </div>
          <el-tag v-if="lmStudioConfig?.isDefault" type="success" effect="plain">本地默认模型</el-tag>
        </div>
        <el-form label-position="top" size="small" class="config-form">
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12">
              <el-form-item label="API 地址">
                <el-input v-model="lmStudioForm.apiUrl" placeholder="http://192.168.1.200:1234/v1" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="API Token">
                <el-input
                  v-model="lmStudioForm.apiKey"
                  type="password"
                  show-password
                  :placeholder="lmStudioForm.apiKeyConfigured ? '已配置，留空则保留' : '请输入 LM Studio API Token'"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12">
              <el-form-item label="模型名称">
                <el-input v-model="lmStudioForm.modelName" placeholder="qwen/qwen3.6-35b-a3b" />
              </el-form-item>
            </el-col>
            <el-col :xs="12" :sm="6">
              <el-form-item label="Temperature">
                <el-slider v-model="lmStudioForm.temperature" :min="0" :max="1" :step="0.1" show-input size="small" />
              </el-form-item>
            </el-col>
            <el-col :xs="12" :sm="6">
              <el-form-item label="Max Tokens">
                <el-input-number v-model="lmStudioForm.maxTokens" :min="512" :max="32768" :step="512" style="width:100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
        <div class="detail-actions">
          <el-button :loading="lmStudioTesting" @click="testConnection(lmStudioConfig?.id, 'LM_STUDIO')">
            <el-icon><Connection /></el-icon>
            测试连接
          </el-button>
          <el-button type="primary" @click="saveConfig(lmStudioConfig?.id, lmStudioForm)">
            保存 LM Studio 配置
          </el-button>
        </div>
        <div v-if="lmStudioTestResult" class="test-result" :class="lmStudioTestResult.status">
          <el-icon v-if="lmStudioTestResult.status === 'ok'"><SuccessFilled /></el-icon>
          <el-icon v-else><WarningFilled /></el-icon>
          <span>{{ lmStudioTestResult.message }} ({{ lmStudioTestResult.duration || 0 }}ms)</span>
        </div>
      </el-card>
    </div>

    <div class="config-section legal-data-section">
      <div class="section-title-row">
        <div>
          <h4>法律数据源</h4>
          <p class="config-tip">元典提供法规、案例语义检索和引证核验，与上方生成模型并行使用。</p>
        </div>
        <el-tag type="info" effect="plain">不作为生成模型</el-tag>
      </div>

      <el-card shadow="never" class="config-detail-card">
        <div class="detail-header">
          <div class="detail-title">
            <el-icon size="18" color="#3378b7"><Search /></el-icon>
            <span>元典法律数据服务</span>
          </div>
          <el-switch v-model="yuandianForm.isEnabled" active-text="启用" />
        </div>
        <el-form label-position="top" size="small" class="config-form">
          <el-row :gutter="20">
            <el-col :xs="24" :sm="12">
              <el-form-item label="官方 API 地址">
                <el-input v-model="yuandianForm.apiUrl" readonly />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12">
              <el-form-item label="API Key">
                <el-input
                  v-model="yuandianForm.apiKey"
                  type="password"
                  show-password
                  :placeholder="yuandianForm.apiKeyConfigured ? '已配置，留空则保留' : '粘贴元典开放平台 API Key'"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
        <div class="detail-actions">
          <el-tooltip content="将执行一次法规检索并消耗元典积分">
            <el-button :loading="yuandianTesting" @click="testYuandianConnection">
              <el-icon><Connection /></el-icon>
              测试连接
            </el-button>
          </el-tooltip>
          <el-button type="primary" @click="saveConfig(yuandianConfig?.id, yuandianForm)">
            保存元典配置
          </el-button>
        </div>
        <div v-if="yuandianTestResult" class="test-result" :class="yuandianTestResult.status">
          <el-icon v-if="yuandianTestResult.status === 'ok'"><SuccessFilled /></el-icon>
          <el-icon v-else><WarningFilled /></el-icon>
          <span>{{ yuandianTestResult.message }} ({{ yuandianTestResult.duration || 0 }}ms)</span>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Cloudy, Connection, Cpu, Monitor, Search, SuccessFilled, WarningFilled } from '@element-plus/icons-vue'
import request from '@/utils/request'

// ========== Data ==========
const configs = ref([])
const deepseekTesting = ref(false)
const ollamaTesting = ref(false)
const lmStudioTesting = ref(false)
const yuandianTesting = ref(false)
const deepseekTestResult = ref(null)
const ollamaTestResult = ref(null)
const lmStudioTestResult = ref(null)
const yuandianTestResult = ref(null)

// ========== Computed ==========
const deepseekConfig = computed(() => configs.value.find(c => c.providerType === 'DEEPSEEK_API'))
const ollamaConfig = computed(() => configs.value.find(c => c.providerType === 'LOCAL_QWEN'))
const lmStudioConfig = computed(() => configs.value.find(c => c.providerType === 'LM_STUDIO'))
const yuandianConfig = computed(() => configs.value.find(c => c.providerType === 'YUANDIAN_LEGAL'))
const activeMode = ref('DEEPSEEK_API') // 当前激活的模式

// Watch for config changes to update activeMode
watch([deepseekConfig, ollamaConfig, lmStudioConfig], () => {
  if (deepseekConfig.value?.isDefault) {
    activeMode.value = 'DEEPSEEK_API'
  } else if (ollamaConfig.value?.isDefault) {
    activeMode.value = 'LOCAL_QWEN'
  } else if (lmStudioConfig.value?.isDefault) {
    activeMode.value = 'LM_STUDIO'
  }
}, { immediate: true })

// DeepSeek form
const deepseekForm = ref({
  apiUrl: 'https://api.deepseek.com/v1/chat/completions',
  apiKey: '',
  apiKeyConfigured: false,
  modelName: 'deepseek-v4-flash',
  temperature: 0.3,
  maxTokens: 4096,
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
  apiKeyConfigured: false,
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

const lmStudioForm = ref({
  apiUrl: 'http://192.168.1.200:1234/v1',
  apiKey: '',
  apiKeyConfigured: false,
  modelName: 'qwen/qwen3.6-35b-a3b',
  temperature: 0.1,
  maxTokens: 8192,
  isEnabled: true,
  isDefault: true,
  configName: 'LM Studio局域网模型',
  providerType: 'LM_STUDIO',
  category: 'LOCAL_LEGAL_AI',
  description: '局域网LM Studio模型，用于本地法律问答、RAG和文书草稿生成。',
  timeoutSeconds: 300
})

const yuandianForm = ref({
  apiUrl: 'https://open.chineselaw.com',
  apiKey: '',
  apiKeyConfigured: false,
  modelName: 'law-and-case-search',
  temperature: 0,
  maxTokens: 2000,
  isEnabled: true,
  isDefault: false,
  configName: '元典法律数据服务',
  providerType: 'YUANDIAN_LEGAL',
  category: 'LEGAL_DATA',
  description: '元典开放平台法规、案例语义检索与法律引证核验。',
  timeoutSeconds: 60
})

// ========== Methods ==========
async function loadConfigs() {
  try {
    const { data } = await request({ url: '/ai/config', method: 'get' })
    configs.value = data || []
    syncFormFromConfig(deepseekConfig.value, deepseekForm.value)
    syncFormFromConfig(ollamaConfig.value, ollamaForm.value)
    syncFormFromConfig(lmStudioConfig.value, lmStudioForm.value)
    syncFormFromConfig(yuandianConfig.value, yuandianForm.value)
  } catch (e) {
    console.error('加载AI配置失败:', e)
  }
}

function syncFormFromConfig(config, form) {
  if (!config) return
  form.apiUrl = config.apiUrl || form.apiUrl
  form.apiKey = ''
  form.apiKeyConfigured = Boolean(config.apiKeyConfigured)
  form.modelName = config.modelName || form.modelName
  form.temperature = config.temperature ?? form.temperature
  form.maxTokens = config.maxTokens ?? form.maxTokens
  form.isEnabled = config.isEnabled ?? true
  form.isDefault = config.isDefault ?? false
}

async function saveConfig(id, form) {
  try {
    const { apiKeyConfigured, ...payload } = form
    await request({
      url: id ? `/ai/config/${id}` : '/ai/config',
      method: id ? 'put' : 'post',
      data: payload
    })
    ElMessage.success('配置已保存')
    await loadConfigs()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || '未知错误'))
  }
}

async function testYuandianConnection() {
  const id = yuandianConfig.value?.id
  if (!id) {
    ElMessage.warning('请先保存元典配置再测试')
    return
  }
  yuandianTesting.value = true
  try {
    const { data } = await request({ url: `/ai/config/test/${id}`, method: 'post', timeout: 120000 })
    yuandianTestResult.value = data || { status: 'error', message: '无法连接', duration: 0 }
    if (yuandianTestResult.value.status === 'ok') {
      ElMessage.success('元典法律检索连接正常')
    } else {
      ElMessage.warning(yuandianTestResult.value.message)
    }
  } catch (e) {
    yuandianTestResult.value = { status: 'error', message: e.message || '请求失败', duration: 0 }
  } finally {
    yuandianTesting.value = false
  }
}

async function handleModeSwitch(mode) {
  const configByMode = {
    DEEPSEEK_API: deepseekConfig.value,
    LOCAL_QWEN: ollamaConfig.value,
    LM_STUDIO: lmStudioConfig.value
  }
  const modeLabels = {
    DEEPSEEK_API: 'DeepSeek（云端）',
    LOCAL_QWEN: 'Ollama（本地）',
    LM_STUDIO: 'LM Studio（局域网）'
  }
  const target = configByMode[mode]
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
      `切换到 ${modeLabels[mode]} 模式？`,
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
    ElMessage.success(`已切换到 ${modeLabels[mode]} 模式`)
    await loadConfigs()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('切换失败: ' + (e.message || '未知错误'))
    }
    // Revert toggle if cancelled
    activeMode.value = configs.value.find(config => config.isDefault)?.providerType || 'DEEPSEEK_API'
  }
}

async function testConnection(id, provider) {
  if (!id) {
    ElMessage.warning('请先保存配置再测试')
    return
  }
  const isDeepseek = provider === 'DEEPSEEK_API' || id === deepseekConfig.value?.id
  const isLmStudio = provider === 'LM_STUDIO' || id === lmStudioConfig.value?.id
  if (isDeepseek) deepseekTesting.value = true
  else if (isLmStudio) lmStudioTesting.value = true
  else ollamaTesting.value = true
  try {
    const { data } = await request({ url: `/ai/config/test/${id}`, method: 'post' })
    const result = data || { status: 'error', message: '无法连接', duration: 0 }
    if (isDeepseek) deepseekTestResult.value = result
    else if (isLmStudio) lmStudioTestResult.value = result
    else ollamaTestResult.value = result
    if (result.status === 'ok') {
      ElMessage.success(`✅ 连接成功 (${result.duration}ms)`)
    } else {
      ElMessage.warning(`⚠️ ${result.message}`)
    }
  } catch (e) {
    const result = { status: 'error', message: e.message || '请求失败', duration: 0 }
    if (isDeepseek) deepseekTestResult.value = result
    else if (isLmStudio) lmStudioTestResult.value = result
    else ollamaTestResult.value = result
  } finally {
    deepseekTesting.value = false
    ollamaTesting.value = false
    lmStudioTesting.value = false
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

.legal-data-section {
  margin-top: 32px;
  padding-top: 28px;
  border-top: 1px solid #e4e7ed;
}

.section-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
}

.section-title-row h4 {
  margin: 0 0 8px;
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
