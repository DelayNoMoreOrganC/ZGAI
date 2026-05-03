<template>
  <div class="npa-asset-detail">
    <PageHeader :title="`债权详情 - ${asset.debtorName || ''}`" :show-back="true" />

    <el-row :gutter="16" v-if="asset.id">
      <!-- 债权基本信息 -->
      <el-col :span="24">
        <el-card class="info-card">
          <template #header>
            <div class="card-header">
              <span>债权信息</span>
              <el-button size="small" @click="showEdit = true">编辑</el-button>
            </div>
          </template>
          <el-descriptions :column="4" border>
            <el-descriptions-item label="债务人" :span="2">{{ asset.debtorName }}</el-descriptions-item>
            <el-descriptions-item label="证件号">{{ asset.debtorIdNumber || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所属资产包">
              <el-link type="primary" @click="$router.push(`/npa/packages/${asset.packageId}`)">{{ asset.packageName }}</el-link>
            </el-descriptions-item>
            <el-descriptions-item label="本金">{{ formatMoney(asset.principalAmount) }}</el-descriptions-item>
            <el-descriptions-item label="利息">{{ formatMoney(asset.interestAmount) }}</el-descriptions-item>
            <el-descriptions-item label="债权总额" :span="2">{{ formatMoney(asset.totalAmount) }}</el-descriptions-item>
            <el-descriptions-item label="担保方式">{{ guaranteeLabel(asset.guaranteeType) }}</el-descriptions-item>
            <el-descriptions-item label="风险等级">
              <el-tag :type="riskTag(asset.riskLevel)">{{ riskLabel(asset.riskLevel) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="诉讼状态">
              <el-tag :type="lawsuitTag(asset.lawsuitStatus)">{{ lawsuitLabel(asset.lawsuitStatus) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusTag(asset.status)">{{ statusLabel(asset.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="管辖法院">{{ asset.court || '-' }}</el-descriptions-item>
            <el-descriptions-item label="案号">{{ asset.caseNumber || '-' }}</el-descriptions-item>
            <el-descriptions-item label="承办法官">{{ asset.judge || '-' }}</el-descriptions-item>
            <el-descriptions-item label="抵押物/保证人" :span="2">{{ asset.collateral || '-' }}</el-descriptions-item>
            <el-descriptions-item label="地址">{{ asset.address || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 回收信息 -->
      <el-col :span="8" style="margin-top: 16px">
        <el-card>
          <template #header><span>回收信息</span></template>
          <div class="recovery-info">
            <div class="rec-item">
              <span class="rec-label">预计回收</span>
              <span class="rec-value">{{ formatMoney(asset.estimatedRecovery) }}</span>
            </div>
            <div class="rec-item">
              <span class="rec-label">已回收</span>
              <span class="rec-value primary">{{ formatMoney(asset.recoveredAmount) }}</span>
            </div>
            <div class="rec-item">
              <span class="rec-label">回收率</span>
              <span class="rec-value"><el-progress :percentage="Number(asset.recoveryRate || 0)" :stroke-width="18" :status="asset.recoveryRate >= 80 ? 'success' : 'warning'" style="width: 160px" /></span>
            </div>
            <el-divider />
            <el-button type="primary" @click="showRecoveryDialog = true" size="small">录入回收金额</el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 处置方案 -->
      <el-col :span="8" style="margin-top: 16px">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>处置方案</span>
              <el-button size="small" type="primary" @click="showCreatePlan = true">新增方案</el-button>
            </div>
          </template>
          <div v-if="plans.length === 0" class="empty-hint">暂无处置方案</div>
          <div v-for="plan in plans" :key="plan.id" class="plan-item">
            <div class="plan-header">
              <el-tag size="small" :type="planStatusTag(plan.status)">{{ planStatusLabel(plan.status) }}</el-tag>
              <span class="plan-method">{{ methodLabel(plan.disposalMethod) }}</span>
            </div>
            <div class="plan-meta">目标: {{ formatMoney(plan.targetAmount) }} | 负责人: {{ plan.responsiblePerson || '-' }}</div>
          </div>
        </el-card>
      </el-col>

      <!-- 处置结果 -->
      <el-col :span="8" style="margin-top: 16px">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>处置结果</span>
              <el-button size="small" type="primary" @click="showCreateResult = true">录入结果</el-button>
            </div>
          </template>
          <div v-if="results.length === 0" class="empty-hint">暂无处置结果</div>
          <div v-for="r in results" :key="r.id" class="result-item">
            <div>{{ r.disposalDate }} — 回收 {{ formatMoney(r.actualRecovery) }}</div>
            <div class="result-meta">净回收: {{ formatMoney(r.netRecovery) }} | 回收率: {{ r.recoveryRate }}%</div>
          </div>
        </el-card>
      </el-col>

      <!-- 尽调记录 -->
      <el-col :span="24" style="margin-top: 16px">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>尽职调查</span>
              <el-button size="small" type="primary" @click="showCreateDiligence = true">新增尽调</el-button>
            </div>
          </template>
          <div v-if="diligences.length === 0" class="empty-hint">暂无尽调记录</div>
          <el-timeline v-else>
            <el-timeline-item v-for="dd in diligences" :key="dd.id" :timestamp="dd.investigationDate || '-'" placement="top">
              <div><strong>调查人:</strong> {{ dd.investigator || '-' }} | <strong>风险等级:</strong> <el-tag :type="riskTag(dd.riskLevel)" size="small">{{ riskLabel(dd.riskLevel) }}</el-tag></div>
              <div v-if="dd.propertyStatus"><strong>财产状况:</strong> {{ dd.propertyStatus }}</div>
              <div v-if="dd.recoveryAnalysis"><strong>回收分析:</strong> {{ dd.recoveryAnalysis }}</div>
              <div v-if="dd.recoveryEstimate"><strong>预计回收:</strong> {{ formatMoney(dd.recoveryEstimate) }}</div>
              <div v-if="dd.aiGenerated" style="color: #999; font-size: 12px">🤖 AI辅助生成</div>
              <el-button link type="primary" size="small" @click="viewDiligence(dd)">查看详情</el-button>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>

    <!-- 编辑对话框 -->
    <el-dialog v-model="showEdit" title="编辑债权" width="550px">
      <el-form :model="editForm" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="债务人"><el-input v-model="editForm.debtorName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="证件号"><el-input v-model="editForm.debtorIdNumber" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="本金"><el-input-number v-model="editForm.principalAmount" :precision="2" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="利息"><el-input-number v-model="editForm.interestAmount" :precision="2" :min="0" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="担保方式">
          <el-select v-model="editForm.guaranteeType" style="width:100%">
            <el-option label="信用" value="CREDIT" /><el-option label="抵押" value="MORTGAGE" />
            <el-option label="保证" value="GUARANTEE" /><el-option label="质押" value="PLEDGE" />
          </el-select>
        </el-form-item>
        <el-form-item label="诉讼状态">
          <el-select v-model="editForm.lawsuitStatus" style="width:100%">
            <el-option label="未诉" value="NOT_SUED" /><el-option label="已诉" value="SUED" />
            <el-option label="执行中" value="ENFORCING" /><el-option label="终本" value="TERMINATED" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="法院"><el-input v-model="editForm.court" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="案号"><el-input v-model="editForm.caseNumber" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="地址"><el-input v-model="editForm.address" /></el-form-item>
        <el-form-item label="抵押物/保证人"><el-input v-model="editForm.collateral" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" @click="handleEdit" :loading="savingEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 回收录入对话框 -->
    <el-dialog v-model="showRecoveryDialog" title="录入回收金额" width="400px">
      <el-form label-width="110px">
        <el-form-item label="回收金额">
          <el-input-number v-model="recoveryAmount" :precision="2" :min="0" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRecoveryDialog = false">取消</el-button>
        <el-button type="primary" @click="handleRecordRecovery" :loading="recovering">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新增尽调对话框 -->
    <el-dialog v-model="showCreateDiligence" title="新增尽职调查" width="700px">
      <el-form :model="diligenceForm" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="调查日期"><el-date-picker v-model="diligenceForm.investigationDate" type="date" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="调查人"><el-input v-model="diligenceForm.investigator" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="财产状况"><el-input v-model="diligenceForm.propertyStatus" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="经营状况"><el-input v-model="diligenceForm.businessStatus" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="联系历史"><el-input v-model="diligenceForm.contactHistory" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="回收分析"><el-input v-model="diligenceForm.recoveryAnalysis" type="textarea" :rows="2" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="预计回收"><el-input-number v-model="diligenceForm.recoveryEstimate" :precision="2" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="风险等级">
            <el-select v-model="diligenceForm.riskLevel" style="width:100%">
              <el-option label="高" value="HIGH" /><el-option label="中" value="MEDIUM" /><el-option label="低" value="LOW" />
            </el-select>
          </el-form-item></el-col>
        </el-row>
        <el-form-item label="调查结论"><el-input v-model="diligenceForm.conclusion" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDiligence = false">取消</el-button>
        <el-button type="primary" @click="handleCreateDiligence" :loading="savingDiligence">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新增方案对话框 -->
    <el-dialog v-model="showCreatePlan" title="新增处置方案" width="550px">
      <el-form :model="planForm" label-width="120px">
        <el-form-item label="方案名称"><el-input v-model="planForm.planName" /></el-form-item>
        <el-form-item label="处置方式">
          <el-select v-model="planForm.disposalMethod" style="width:100%">
            <el-option label="诉讼" value="LITIGATION" /><el-option label="执行" value="ENFORCEMENT" />
            <el-option label="和解" value="SETTLEMENT" /><el-option label="债权转让" value="ASSIGNMENT" />
            <el-option label="核销" value="WRITE_OFF" /><el-option label="破产" value="BANKRUPTCY" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="目标金额"><el-input-number v-model="planForm.targetAmount" :precision="2" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="计划完成"><el-date-picker v-model="planForm.deadline" type="date" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="负责人"><el-input v-model="planForm.responsiblePerson" /></el-form-item>
        <el-form-item label="方案详情"><el-input v-model="planForm.planDetail" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreatePlan = false">取消</el-button>
        <el-button type="primary" @click="handleCreatePlan" :loading="savingPlan">保存</el-button>
      </template>
    </el-dialog>

    <!-- 录入结果对话框 -->
    <el-dialog v-model="showCreateResult" title="录入处置结果" width="550px">
      <el-form :model="resultForm" label-width="120px">
        <el-form-item label="处置日期"><el-date-picker v-model="resultForm.disposalDate" type="date" style="width:100%" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="回收金额"><el-input-number v-model="resultForm.actualRecovery" :precision="2" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="处置费用"><el-input-number v-model="resultForm.costAmount" :precision="2" :min="0" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="回款方式">
          <el-select v-model="resultForm.recoveryMethod" style="width:100%">
            <el-option label="一次性" value="LUMP_SUM" /><el-option label="分期" value="INSTALLMENT" />
            <el-option label="以物抵债" value="ASSET" /><el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="结果描述"><el-input v-model="resultForm.resultDescription" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateResult = false">取消</el-button>
        <el-button type="primary" @click="handleCreateResult" :loading="savingResult">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { getAssetDetail, updateAsset, recordRecovery } from '@/api/npa'
import { getDiligenceList, createDiligence } from '@/api/npa'
import { getPlanList, createPlan } from '@/api/npa'
import { getResultList, createResult } from '@/api/npa'

const route = useRoute()
const assetId = Number(route.params.id)
const asset = ref({})

// 尽调
const diligences = ref([])
const showCreateDiligence = ref(false)
const savingDiligence = ref(false)
const diligenceForm = ref({ assetId, investigationDate: null, investigator: '', propertyStatus: '', businessStatus: '', contactHistory: '', recoveryAnalysis: '', recoveryEstimate: 0, riskLevel: 'MEDIUM', conclusion: '' })

// 方案
const plans = ref([])
const showCreatePlan = ref(false)
const savingPlan = ref(false)
const planForm = ref({ assetId, planName: '', disposalMethod: 'LITIGATION', targetAmount: 0, deadline: null, responsiblePerson: '', planDetail: '' })

// 结果
const results = ref([])
const showCreateResult = ref(false)
const savingResult = ref(false)
const resultForm = ref({ assetId, disposalDate: null, actualRecovery: 0, costAmount: 0, recoveryMethod: 'LUMP_SUM', resultDescription: '' })

// 回收录入
const showRecoveryDialog = ref(false)
const recoveryAmount = ref(0)
const recovering = ref(false)

// 编辑
const showEdit = ref(false)
const savingEdit = ref(false)
const editForm = ref({})

function formatMoney(v) { return v || v === 0 ? '¥' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : '-' }
function guaranteeLabel(s) { return { CREDIT: '信用', MORTGAGE: '抵押', GUARANTEE: '保证', PLEDGE: '质押' }[s] || '-' }
function lawsuitLabel(s) { return { NOT_SUED: '未诉', SUED: '已诉', ENFORCING: '执行中', TERMINATED: '终本' }[s] || '-' }
function lawsuitTag(s) { return { NOT_SUED: 'info', SUED: 'primary', ENFORCING: 'warning', TERMINATED: 'danger' }[s] || 'info' }
function riskTag(s) { return { HIGH: 'danger', MEDIUM: 'warning', LOW: 'success' }[s] || 'info' }
function riskLabel(s) { return { HIGH: '高', MEDIUM: '中', LOW: '低' }[s] || '-' }
function statusTag(s) { return { PENDING: 'info', IN_PROGRESS: 'warning', RECOVERED: 'success', CHARGE_OFF: 'danger' }[s] || 'info' }
function statusLabel(s) { return { PENDING: '待处置', IN_PROGRESS: '处置中', RECOVERED: '已回收', CHARGE_OFF: '核销' }[s] || s }
function planStatusTag(s) { return { PENDING_REVIEW: 'info', APPROVED: 'success', IN_PROGRESS: 'warning', COMPLETED: 'success', REJECTED: 'danger' }[s] || 'info' }
function planStatusLabel(s) { return { PENDING_REVIEW: '待审批', APPROVED: '已批准', IN_PROGRESS: '执行中', COMPLETED: '已完成', REJECTED: '已驳回' }[s] || s }
function methodLabel(s) { return { LITIGATION: '诉讼', ENFORCEMENT: '执行', SETTLEMENT: '和解', ASSIGNMENT: '债权转让', WRITE_OFF: '核销', BANKRUPTCY: '破产', OTHER: '其他' }[s] || '-' }

async function loadAll() {
  try {
    const res = await getAssetDetail(assetId)
    asset.value = res.data
    editForm.value = { ...res.data }
  } catch (e) { ElMessage.error('加载债权失败') }

  try { diligences.value = (await getDiligenceList(assetId)).data || [] } catch {}
  try { plans.value = (await getPlanList(assetId)).data || [] } catch {}
  try { results.value = (await getResultList(assetId)).data || [] } catch {}
}

async function handleEdit() {
  savingEdit.value = true
  try { await updateAsset(assetId, editForm.value); ElMessage.success('已更新'); showEdit.value = false; loadAll() }
  catch (e) { ElMessage.error('更新失败') }
  finally { savingEdit.value = false }
}

async function handleRecordRecovery() {
  recovering.value = true
  try { await recordRecovery(assetId, recoveryAmount.value); ElMessage.success('已录入'); showRecoveryDialog.value = false; loadAll() }
  catch (e) { ElMessage.error('录入失败') }
  finally { recovering.value = false }
}

async function handleCreateDiligence() {
  savingDiligence.value = true
  try {
    diligenceForm.value.assetId = assetId
    await createDiligence(diligenceForm.value)
    ElMessage.success('已添加'); showCreateDiligence.value = false
    diligenceForm.value = { assetId, investigationDate: null, investigator: '', propertyStatus: '', businessStatus: '', contactHistory: '', recoveryAnalysis: '', recoveryEstimate: 0, riskLevel: 'MEDIUM', conclusion: '' }
    diligences.value = (await getDiligenceList(assetId)).data || []
  } catch (e) { ElMessage.error('创建失败') }
  finally { savingDiligence.value = false }
}

async function handleCreatePlan() {
  savingPlan.value = true
  try {
    planForm.value.assetId = assetId
    await createPlan(planForm.value)
    ElMessage.success('已添加'); showCreatePlan.value = false
    planForm.value = { assetId, planName: '', disposalMethod: 'LITIGATION', targetAmount: 0, deadline: null, responsiblePerson: '', planDetail: '' }
    plans.value = (await getPlanList(assetId)).data || []
  } catch (e) { ElMessage.error('创建失败') }
  finally { savingPlan.value = false }
}

async function handleCreateResult() {
  savingResult.value = true
  try {
    resultForm.value.assetId = assetId
    await createResult(resultForm.value)
    ElMessage.success('已录入'); showCreateResult.value = false
    resultForm.value = { assetId, disposalDate: null, actualRecovery: 0, costAmount: 0, recoveryMethod: 'LUMP_SUM', resultDescription: '' }
    results.value = (await getResultList(assetId)).data || []
    loadAll()
  } catch (e) { ElMessage.error('录入失败') }
  finally { savingResult.value = false }
}

function viewDiligence(dd) {
  ElMessage.info(`尽调详情:\n财产: ${dd.propertyStatus || '-'}\n回收分析: ${dd.recoveryAnalysis || '-'}\n结论: ${dd.conclusion || '-'}`)
}

onMounted(() => loadAll())
</script>

<style scoped lang="scss">
.npa-asset-detail { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.info-card { margin-bottom: 0; }
.recovery-info { .rec-item { display: flex; justify-content: space-between; padding: 8px 0; .rec-label { color: #666; } .rec-value.primary { color: #409eff; font-weight: bold; } } }
.plan-item, .result-item { padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
.plan-header { display: flex; gap: 8px; align-items: center; }
.plan-meta, .result-meta { font-size: 12px; color: #999; margin-top: 4px; }
.empty-hint { color: #999; text-align: center; padding: 16px; }
</style>
