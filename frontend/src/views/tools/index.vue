<template>
  <div class="tools-page">
    <PageHeader title="工具集" />

    <el-tabs v-model="activeTab" type="border-card" class="tools-tabs">
      <!-- ========== Tab 1: 常用工具 ========== -->
      <el-tab-pane label="🔧 常用工具" name="common">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-card class="tool-card">
              <template #header><div class="card-header"><el-icon><Money /></el-icon> 诉讼费计算器</div></template>
              <el-form label-width="100px">
                <el-form-item label="争议金额"><el-input-number v-model="litigation.amount" :min="0" :precision="2" style="width:100%" /></el-form-item>
                <el-form-item label="案件类型"><el-select v-model="litigation.type" style="width:100%">
                  <el-option label="财产案件" value="property" /><el-option label="离婚案件" value="divorce" />
                </el-select></el-form-item>
                <el-form-item><el-button type="primary" @click="calculateLitigation" style="width:100%">计算</el-button></el-form-item>
              </el-form>
              <el-alert v-if="litigation.result" :title="'¥' + litigation.result" type="success" :closable="false" />
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="tool-card">
              <template #header><div class="card-header"><el-icon><TrendCharts /></el-icon> 利息计算器</div></template>
              <el-form label-width="100px">
                <el-form-item label="本金"><el-input-number v-model="interest.principal" :min="0" :precision="2" style="width:100%" /></el-form-item>
                <el-form-item label="年利率(%)"><el-input-number v-model="interest.rate" :min="0" :max="100" :precision="4" style="width:100%" /></el-form-item>
                <el-form-item label="天数"><el-input-number v-model="interest.days" :min="0" style="width:100%" /></el-form-item>
                <el-form-item><el-button type="primary" @click="calculateInterest" style="width:100%">计算</el-button></el-form-item>
              </el-form>
              <el-alert v-if="interest.result" :title="'¥' + interest.result" type="success" :closable="false" />
            </el-card>
          </el-col>
          <el-col :span="8">
            <el-card class="tool-card">
              <template #header><div class="card-header"><el-icon><Calendar /></el-icon> 时效计算器</div></template>
              <el-form label-width="100px">
                <el-form-item label="起始日期"><el-date-picker v-model="limitation.startDate" type="date" style="width:100%" /></el-form-item>
                <el-form-item label="时效类型"><el-select v-model="limitation.type" style="width:100%">
                  <el-option label="普通诉讼时效(3年)" :value="3" /><el-option label="短期时效(1年)" :value="1" />
                </el-select></el-form-item>
                <el-form-item><el-button type="primary" @click="calculateLimitation" style="width:100%">计算</el-button></el-form-item>
              </el-form>
              <el-alert v-if="limitation.result" :title="limitation.result" type="success" :closable="false" />
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- ========== Tab 2: 省时宝 ========== -->
      <el-tab-pane label="⏱ 省时宝 — 法律文书自动生成" name="ssb">
        <div class="ssb-container">
          <!-- 服务状态 -->
          <div class="service-bar">
            <el-tag :type="ssbOnline ? 'success' : 'danger'" size="small">
              {{ ssbOnline ? '✅ 省时宝服务正常' : '❌ 省时宝服务未连接' }}
            </el-tag>
            <el-tag type="info" size="small">{{ ssbTemplateCount }} 个模板项目</el-tag>
          </div>

          <!-- 模板选择区 -->
          <div v-if="ssbOnline">
            <!-- 选中的模板 -->
            <div v-if="selectedTemplate" class="template-detail">
              <div class="back-bar">
                <el-button text @click="selectedTemplate = null">← 返回模板列表</el-button>
                <span class="template-title">📄 {{ selectedTemplate.name }}</span>
                <el-tag size="small">{{ selectedTemplate.files }} 个文件</el-tag>
              </div>

              <!-- 字段填写表单 -->
              <div v-if="templateFields.length > 0" class="fields-form">
                <h4>请填写以下字段信息：</h4>
                <el-form :model="ssbForm" label-width="160px" size="default">
                  <el-form-item
                    v-for="field in templateFields"
                    :key="field.name"
                    :label="field.label || field.name"
                  >
                    <el-input
                      v-if="field.type === 'text' || !field.type"
                      v-model="ssbForm[field.name]"
                      :placeholder="field.example || '请输入' + (field.label || field.name)"
                      style="width:100%"
                    />
                    <el-input-number
                      v-else-if="field.type === 'number'"
                      v-model="ssbForm[field.name]"
                      :min="0"
                      :precision="2"
                      style="width:100%"
                    />
                    <el-date-picker
                      v-else-if="field.type === 'date'"
                      v-model="ssbForm[field.name]"
                      type="date"
                      placeholder="选择日期"
                      style="width:100%"
                      value-format="YYYY-MM-DD"
                    />
                    <el-select v-else-if="field.type === 'select' && field.options" v-model="ssbForm[field.name]" style="width:100%">
                      <el-option v-for="opt in field.options" :key="opt" :label="opt" :value="opt" />
                    </el-select>
                    <el-input v-else v-model="ssbForm[field.name]" type="textarea" :rows="3" style="width:100%" />
                  </el-form-item>
                  <el-form-item>
                    <el-button type="primary" size="large" :loading="ssbGenerating" @click="handleGenerateDoc">
                      🚀 生成文档
                    </el-button>
                    <el-button @click="resetSsbForm">重置</el-button>
                  </el-form-item>
                </el-form>
              </div>
              <el-empty v-else-if="!fieldsLoading" description="此模板没有可填写的字段" />
              <div v-else class="loading-center"><el-icon class="is-loading" :size="24"><Loading /></el-icon> 加载字段中...</div>
            </div>

            <!-- 模板列表 -->
            <div v-else>
              <div v-if="ssbTemplates.length === 0 && !templatesLoading" class="loading-center">
                <el-empty description="暂无可用模板" />
              </div>
              <div v-else-if="templatesLoading" class="loading-center">
                <el-icon class="is-loading" :size="24"><Loading /></el-icon> 加载模板列表...
              </div>
              <div v-else class="template-grid">
                <div
                  v-for="tmpl in ssbTemplates"
                  :key="tmpl.path"
                  class="template-card"
                  @click="selectTemplate(tmpl)"
                >
                  <div class="tmpl-icon">📋</div>
                  <div class="tmpl-info">
                    <div class="tmpl-name">{{ tmpl.name }}</div>
                    <div class="tmpl-desc">{{ tmpl.description }}</div>
                    <div class="tmpl-meta">
                      <el-tag size="small">{{ tmpl.files }} 份文件</el-tag>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 服务离线 -->
          <el-empty v-else description="省时宝服务未启动，请运行: cd ssb && python3 ssb_api.py" />
        </div>
      </el-tab-pane>

      <!-- ========== Tab 3: AC 精算 ========== -->
      <el-tab-pane label="📊 AC 精算 — 债权利息计算" name="ac">
        <div class="ac-container">
          <div class="service-bar">
            <el-tag :type="acOnline ? 'success' : 'danger'" size="small">
              {{ acOnline ? '✅ AC精算服务正常' : '❌ AC精算服务未连接' }}
            </el-tag>
          </div>

          <div class="ac-layout">
            <!-- 输入区 -->
            <el-card class="ac-form-card">
              <template #header><span>📝 债权信息输入</span></template>
              <el-form :model="acForm" label-width="120px">
                <el-form-item label="本金 (元)"><el-input-number v-model="acForm.principal" :min="0" :precision="2" style="width:100%" /></el-form-item>
                <el-form-item label="年利率 (%)"><el-input-number v-model="acForm.annualRate" :min="0" :max="100" :precision="4" style="width:100%" /></el-form-item>
                <el-form-item label="罚息年利率 (%)"><el-input-number v-model="acForm.penaltyRate" :min="0" :max="100" :precision="4" style="width:100%" /></el-form-item>
                <el-form-item label="起算日"><el-date-picker v-model="acForm.startDate" type="date" style="width:100%" value-format="YYYY-MM-DD" /></el-form-item>
                <el-form-item label="截止日"><el-date-picker v-model="acForm.endDate" type="date" style="width:100%" value-format="YYYY-MM-DD" /></el-form-item>

                <!-- 还款记录 -->
                <el-form-item label="还款记录">
                  <div class="repayment-records">
                    <div v-for="(rec, idx) in acForm.repaymentRecords" :key="idx" class="record-row">
                      <el-date-picker v-model="rec.date" type="date" placeholder="日期" value-format="YYYY-MM-DD" style="width:140px" />
                      <el-input-number v-model="rec.amount" :min="0" :precision="2" placeholder="金额" style="width:160px" />
                      <el-select v-model="rec.type" style="width:110px">
                        <el-option label="正常还款" value="normal" />
                        <el-option label="部分还款" value="partial" />
                        <el-option label="提前还款" value="early" />
                      </el-select>
                      <el-button type="danger" text @click="removeRepayment(idx)">✕</el-button>
                    </div>
                    <el-button size="small" @click="addRepayment">＋ 添加还款记录</el-button>
                  </div>
                </el-form-item>

                <el-form-item>
                  <el-button type="primary" size="large" :loading="acCalculating" @click="handleCalculate">
                    🧮 开始计算
                  </el-button>
                  <el-button @click="resetAcForm">重置</el-button>
                </el-form-item>
              </el-form>
            </el-card>

            <!-- 结果区 -->
            <el-card v-if="acResult" class="ac-result-card">
              <template #header>
                <span>📊 计算结果</span>
                <el-tag type="success" size="small" style="float:right">耗时: {{ acResult.processingTimeMs || 0 }}ms</el-tag>
              </template>
              <div class="result-summary">
                <div class="summary-item">
                  <span class="label">本金</span>
                  <span class="value">¥{{ formatMoney(acResult.principal) }}</span>
                </div>
                <div class="summary-item highlight">
                  <span class="label">利息总额</span>
                  <span class="value">¥{{ formatMoney(acResult.total_interest) }}</span>
                </div>
                <div class="summary-item highlight">
                  <span class="label">罚息总额</span>
                  <span class="value">¥{{ formatMoney(acResult.total_penalty) }}</span>
                </div>
                <div class="summary-item total">
                  <span class="label">本息合计</span>
                  <span class="value">¥{{ formatMoney(acResult.total_amount) }}</span>
                </div>
              </div>
              <el-divider />
              <h4>利息明细</h4>
              <el-table :data="acResult.breakdown || []" size="small" border max-height="400">
                <el-table-column prop="period" label="期间" width="180" />
                <el-table-column prop="days" label="天数" width="80" />
                <el-table-column prop="principal" label="本金" width="140">
                  <template #default="{ row }">¥{{ formatMoney(row.principal) }}</template>
                </el-table-column>
                <el-table-column prop="daily_rate_display" label="日利率" width="100" />
                <el-table-column prop="interest" label="利息" width="140">
                  <template #default="{ row }">¥{{ formatMoney(row.interest) }}</template>
                </el-table-column>
                <el-table-column prop="penalty" label="罚息" width="140">
                  <template #default="{ row }">¥{{ formatMoney(row.penalty) }}</template>
                </el-table-column>
              </el-table>
            </el-card>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Money, TrendCharts, Calendar, Loading } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { getSsbTemplates, getSsbTemplateFields, generateSsbDocument, getSsbHealth } from '@/api/external'
import { calculateAcDebt, getExternalHealth } from '@/api/external'

// ============================================================
// Tab 控制
// ============================================================
const activeTab = ref('common')

// ============================================================
// 诉讼费计算器
// ============================================================
const litigation = ref({ amount: 0, type: 'property', result: null })
const calculateLitigation = () => {
  const { amount, type } = litigation.value
  let fee = 0
  if (type === 'property') {
    if (amount <= 10000) fee = 50
    else if (amount <= 100000) fee = 50 + (amount - 10000) * 0.025
    else if (amount <= 200000) fee = 2300 + (amount - 100000) * 0.022
    else fee = 100
  } else {
    fee = 50 + Math.min(amount / 10000 * 50, 250)
  }
  litigation.value.result = fee.toFixed(2)
  ElMessage.success('计算完成')
}

// ============================================================
// 利息计算器
// ============================================================
const interest = ref({ principal: 0, rate: 4.35, days: 0, result: null })
const calculateInterest = () => {
  const { principal, rate, days } = interest.value
  if (principal <= 0 || days <= 0) { ElMessage.warning('请输入有效数值'); return }
  interest.value.result = ((principal * rate / 100 / 365) * days).toFixed(2)
  ElMessage.success('计算完成')
}

// ============================================================
// 时效计算器
// ============================================================
const limitation = ref({ startDate: null, type: 3, result: null })
const calculateLimitation = () => {
  if (!limitation.value.startDate) { ElMessage.warning('请选择日期'); return }
  const endDate = new Date(limitation.value.startDate)
  endDate.setFullYear(endDate.getFullYear() + limitation.value.type)
  limitation.value.result = '截止日期：' + endDate.toLocaleDateString('zh-CN')
  ElMessage.success('计算完成')
}

// ============================================================
// 省时宝 (SSB)
// ============================================================
const ssbOnline = ref(false)
const ssbTemplateCount = ref(0)
const ssbTemplates = ref([])
const templatesLoading = ref(false)
const selectedTemplate = ref(null)
const templateFields = ref([])
const fieldsLoading = ref(false)
const ssbGenerating = ref(false)
const ssbForm = reactive({})

// 检查 SSB 服务状态
const checkSsbHealth = async () => {
  try {
    const res = await getSsbHealth()
    ssbOnline.value = res?.data?.success === true
    ssbTemplateCount.value = res?.data?.template_count || 0
  } catch { ssbOnline.value = false }
}

// 加载模板列表
const loadTemplates = async () => {
  templatesLoading.value = true
  try {
    const res = await getSsbTemplates()
    if (res?.data?.projects) ssbTemplates.value = res.data.projects
  } catch (e) { ElMessage.error('加载模板失败: ' + (e.message || '未知错误')) }
  finally { templatesLoading.value = false }
}

// 选中模板，加载字段
const selectTemplate = async (tmpl) => {
  selectedTemplate.value = tmpl
  fieldsLoading.value = true
  templateFields.value = []
  // 清空表单
  Object.keys(ssbForm).forEach(k => delete ssbForm[k])

  try {
    const res = await getSsbTemplateFields(tmpl.path)
    if (res?.data?.fields) {
      templateFields.value = res.data.fields
      // 初始化表单值
      res.data.fields.forEach(f => {
        if (f.type === 'number') ssbForm[f.name] = 0
        else ssbForm[f.name] = ''
      })
    } else if (res?.data) {
      // 可能是数组格式
      const fields = Array.isArray(res.data) ? res.data : (res.data.fields || [])
      templateFields.value = fields
      fields.forEach(f => {
        if (f.type === 'number') ssbForm[f.name] = 0
        else ssbForm[f.name] = ''
      })
    }
  } catch (e) {
    ElMessage.error('加载字段失败: ' + (e.message || '未知错误'))
    templateFields.value = []
  }
  finally { fieldsLoading.value = false }
}

// 生成文档
const handleGenerateDoc = async () => {
  if (!selectedTemplate.value) return
  ssbGenerating.value = true

  // 构建提取数据
  const extractedData = {}
  templateFields.value.forEach(f => {
    const val = ssbForm[f.name]
    if (val !== '' && val !== null && val !== undefined) {
      extractedData[f.name] = val instanceof Date ? val.toISOString().split('T')[0] : String(val)
    }
  })

  try {
    const res = await generateSsbDocument({
      project_path: selectedTemplate.value.path,
      extracted_data: extractedData
    })

    if (res?.code === 200) {
      const resultData = res.data
      ElMessage.success('文档生成成功！')

      // 如果有文件列表，显示下载链接
      if (resultData?.files && resultData.files.length > 0) {
        const fileList = resultData.files
        ElMessageBox.alert(
          fileList.map(f => `📄 ${f.name || f}`).join('\n'),
          '生成文件列表',
          { confirmButtonText: '好的' }
        )
      }

      // 如果返回了下载 URL
      if (resultData?.download_url) {
        window.open(resultData.download_url, '_blank')
      }
    } else {
      ElMessage.error(res?.message || '文档生成失败')
    }
  } catch (e) {
    ElMessage.error('文档生成失败: ' + (e.message || '未知错误'))
  }
  finally { ssbGenerating.value = false }
}

const resetSsbForm = () => {
  templateFields.value.forEach(f => {
    if (f.type === 'number') ssbForm[f.name] = 0
    else ssbForm[f.name] = ''
  })
}

// ============================================================
// AC 精算
// ============================================================
const acOnline = ref(false)
const acCalculating = ref(false)
const acResult = ref(null)

const acForm = reactive({
  principal: 1000000,
  annualRate: 5,
  penaltyRate: 7,
  startDate: '2023-01-01',
  endDate: '2024-06-30',
  repaymentRecords: []
})

const addRepayment = () => {
  acForm.repaymentRecords.push({ date: '', amount: 0, type: 'normal' })
}
const removeRepayment = (idx) => {
  acForm.repaymentRecords.splice(idx, 1)
}

// 检查 AC 服务
const checkAcHealth = async () => {
  try {
    const res = await getExternalHealth()
    acOnline.value = res?.data?.acCalc === 'online'
  } catch { acOnline.value = false }
}

// 计算
const handleCalculate = async () => {
  if (!acForm.principal || !acForm.startDate || !acForm.endDate) {
    ElMessage.warning('请填写本金、起算日和截止日')
    return
  }

  acCalculating.value = true
  acResult.value = null

  const payload = {
    principal: Number(acForm.principal),
    annual_rate: acForm.annualRate / 100,
    start_date: acForm.startDate,
    end_date: acForm.endDate,
    repayment_records: acForm.repaymentRecords
      .filter(r => r.date && r.amount > 0)
      .map(r => ({ date: r.date, amount: Number(r.amount), type: r.type }))
  }

  if (acForm.penaltyRate > 0) payload.penalty_rate = acForm.penaltyRate / 100

  try {
    const res = await calculateAcDebt(payload)
    if (res?.code === 200 && res?.data) {
      acResult.value = res.data
      ElMessage.success('计算完成！')
    } else {
      ElMessage.error(res?.message || '计算失败')
    }
  } catch (e) {
    ElMessage.error('计算失败: ' + (e.message || 'AC精算服务暂不可用'))
  }
  finally { acCalculating.value = false }
}

const resetAcForm = () => {
  acForm.principal = 1000000
  acForm.annualRate = 5
  acForm.penaltyRate = 7
  acForm.startDate = '2023-01-01'
  acForm.endDate = '2024-06-30'
  acForm.repaymentRecords = []
  acResult.value = null
}

// ============================================================
// 通用工具函数
// ============================================================
const formatMoney = (val) => {
  if (val === null || val === undefined) return '0.00'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ============================================================
// 初始化
// ============================================================
onMounted(async () => {
  await checkSsbHealth()
  await checkAcHealth()
  if (ssbOnline.value) await loadTemplates()
})
</script>

<style scoped lang="scss">
.tools-page {
  .tools-tabs {
    margin-top: 16px;
  }

  .tool-card {
    margin-bottom: 20px;
    .card-header {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 500;
    }
  }

  // ========== SSB 样式 ==========
  .ssb-container, .ac-container {
    .service-bar {
      display: flex;
      gap: 12px;
      align-items: center;
      margin-bottom: 20px;
    }

    .back-bar {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-bottom: 16px;
      padding: 12px 16px;
      background: #fafafa;
      border-radius: 8px;
      .template-title {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }
    }

    .template-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
      gap: 16px;

      .template-card {
        display: flex;
        gap: 16px;
        padding: 20px;
        background: #fff;
        border: 1px solid #ebeef5;
        border-radius: 8px;
        cursor: pointer;
        transition: all 0.25s;

        &:hover {
          border-color: #409eff;
          box-shadow: 0 4px 16px rgba(64, 158, 255, 0.15);
          transform: translateY(-2px);
        }

        .tmpl-icon {
          font-size: 36px;
          flex-shrink: 0;
        }
        .tmpl-info {
          flex: 1;
          min-width: 0;
          .tmpl-name {
            font-size: 15px;
            font-weight: 600;
            color: #303133;
            margin-bottom: 4px;
          }
          .tmpl-desc {
            font-size: 13px;
            color: #909399;
            line-height: 1.5;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
            margin-bottom: 8px;
          }
        }
      }
    }

    .fields-form {
      h4 { margin-bottom: 16px; color: #606266; }
      padding: 16px;
      background: #fafafa;
      border-radius: 8px;
    }

    .loading-center {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      padding: 60px 0;
      color: #909399;
    }
  }

  // ========== AC 样式 ==========
  .ac-layout {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 20px;
    align-items: start;

    @media (max-width: 1200px) {
      grid-template-columns: 1fr;
    }

    .ac-form-card {
      .repayment-records {
        .record-row {
          display: flex;
          gap: 8px;
          align-items: center;
          margin-bottom: 8px;
        }
      }
    }

    .ac-result-card {
      .result-summary {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 12px;

        .summary-item {
          padding: 12px;
          background: #f5f7fa;
          border-radius: 6px;
          display: flex;
          flex-direction: column;
          gap: 4px;
          .label { font-size: 13px; color: #909399; }
          .value { font-size: 18px; font-weight: 600; color: #303133; }

          &.highlight { background: #ecf5ff; .value { color: #409eff; } }
          &.total {
            grid-column: 1 / -1;
            background: linear-gradient(135deg, #f0f9ff, #e6f7ff);
            .value { font-size: 24px; color: #f56c6c; }
          }
        }
      }
    }
  }
}
</style>
