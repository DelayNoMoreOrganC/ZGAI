<template>
  <div class="npa-package-detail">
    <PageHeader :title="pkg.packageName || '资产包详情'" :show-back="true">
      <template #extra>
        <el-button type="primary" @click="showEditDialog = true" v-if="pkg.id">编辑</el-button>
      </template>
    </PageHeader>

    <el-row :gutter="16" v-if="pkg.id">
      <!-- 基本信息 -->
      <el-col :span="24">
        <el-card class="info-card">
          <template #header><span>基本信息</span></template>
          <el-descriptions :column="4" border>
            <el-descriptions-item label="资产包编号">{{ pkg.packageCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="委托银行">{{ pkg.bankName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="负责人">{{ pkg.responsiblePerson || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusTag(pkg.status)" size="small">{{ statusLabel(pkg.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="债权总额">{{ formatMoney(pkg.totalAmount) }}</el-descriptions-item>
            <el-descriptions-item label="债权笔数">{{ pkg.assetCount ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="已回收金额">{{ formatMoney(pkg.recoveredAmount) }}</el-descriptions-item>
            <el-descriptions-item label="回收率">
              <el-progress :percentage="Number(pkg.recoveryRate || 0)" :stroke-width="14" :status="pkg.recoveryRate >= 80 ? 'success' : 'warning'" style="width: 120px" />
            </el-descriptions-item>
            <el-descriptions-item label="收购日期">{{ pkg.acquisitionDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="处置期限">{{ pkg.deadlineDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="累计处置天数">{{ pkg.disposalDays ?? '-' }}天</el-descriptions-item>
            <el-descriptions-item label="创建人">{{ pkg.createdBy || '-' }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="4">{{ pkg.description || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <!-- 债权列表 -->
      <el-col :span="24" style="margin-top: 16px">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>债权列表</span>
              <el-button type="primary" size="small" @click="showCreateAsset = true">
                <el-icon><Plus /></el-icon>新增债权
              </el-button>
            </div>
          </template>
          <el-table :data="assetList" border v-loading="assetLoading" stripe>
            <el-table-column prop="debtorName" label="债务人" min-width="140">
              <template #default="{ row }">
                <el-link type="primary" @click="$router.push(`/npa/assets/${row.id}`)">{{ row.debtorName }}</el-link>
              </template>
            </el-table-column>
            <el-table-column prop="principalAmount" label="本金" width="130" align="right">
              <template #default="{ row }">{{ formatMoney(row.principalAmount) }}</template>
            </el-table-column>
            <el-table-column prop="totalAmount" label="总额" width="130" align="right">
              <template #default="{ row }">{{ formatMoney(row.totalAmount) }}</template>
            </el-table-column>
            <el-table-column prop="guaranteeType" label="担保方式" width="100" align="center">
              <template #default="{ row }">{{ guaranteeLabel(row.guaranteeType) }}</template>
            </el-table-column>
            <el-table-column prop="lawsuitStatus" label="诉讼状态" width="100" align="center">
              <template #default="{ row }">{{ lawsuitLabel(row.lawsuitStatus) }}</template>
            </el-table-column>
            <el-table-column prop="recoveredAmount" label="已回收" width="120" align="right">
              <template #default="{ row }">{{ formatMoney(row.recoveredAmount) }}</template>
            </el-table-column>
            <el-table-column prop="riskLevel" label="风险等级" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="riskTag(row.riskLevel)" size="small">{{ riskLabel(row.riskLevel) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="assetStatusTag(row.status)" size="small">{{ assetStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="$router.push(`/npa/assets/${row.id}`)">详情</el-button>
                <el-button link type="danger" size="small" @click="deleteAsset(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination" v-if="assetTotal > 0">
            <el-pagination background small layout="prev, pager, next" :total="assetTotal" :page-size="assetSize" v-model:current-page="assetPage" @current-change="loadAssets" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 编辑对话框 -->
    <el-dialog v-model="showEditDialog" title="编辑资产包" width="600px">
      <el-form :model="editForm" label-width="110px">
        <el-form-item label="资产包名称"><el-input v-model="editForm.packageName" /></el-form-item>
        <el-form-item label="委托银行"><el-input v-model="editForm.bankName" /></el-form-item>
        <el-form-item label="负责人"><el-input v-model="editForm.responsiblePerson" /></el-form-item>
        <el-form-item label="收购日期">
          <el-date-picker v-model="editForm.acquisitionDate" type="date" style="width: 100%" />
        </el-form-item>
        <el-form-item label="处置期限">
          <el-date-picker v-model="editForm.deadlineDate" type="date" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="editForm.description" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEditDialog = false">取消</el-button>
        <el-button type="primary" @click="handleEdit" :loading="editing">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新增债权对话框 -->
    <el-dialog v-model="showCreateAsset" title="新增债权" width="600px">
      <el-form :model="assetForm" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="债务人" required><el-input v-model="assetForm.debtorName" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="证件号"><el-input v-model="assetForm.debtorIdNumber" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="本金"><el-input-number v-model="assetForm.principalAmount" :precision="2" :min="0" style="width: 100%" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="利息"><el-input-number v-model="assetForm.interestAmount" :precision="2" :min="0" style="width: 100%" /></el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="担保方式">
              <el-select v-model="assetForm.guaranteeType" style="width: 100%">
                <el-option label="信用" value="CREDIT" />
                <el-option label="抵押" value="MORTGAGE" />
                <el-option label="保证" value="GUARANTEE" />
                <el-option label="质押" value="PLEDGE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="风险等级">
              <el-select v-model="assetForm.riskLevel" style="width: 100%">
                <el-option label="高" value="HIGH" />
                <el-option label="中" value="MEDIUM" />
                <el-option label="低" value="LOW" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="抵押物/保证人">
          <el-input v-model="assetForm.collateral" type="textarea" :rows="2" placeholder="抵押物描述或保证人信息" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="assetForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateAsset = false">取消</el-button>
        <el-button type="primary" @click="handleCreateAsset" :loading="creatingAsset">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { getPackageDetail, updatePackage } from '@/api/npa'
import { getAssetList, createAsset, deleteAsset as deleteAssetApi } from '@/api/npa'

const route = useRoute()
const pkgId = Number(route.params.id)

// 资产包
const pkg = ref({})
const showEditDialog = ref(false)
const editing = ref(false)
const editForm = ref({})

// 债权
const assetList = ref([])
const assetLoading = ref(false)
const assetPage = ref(1)
const assetSize = ref(10)
const assetTotal = ref(0)
const showCreateAsset = ref(false)
const creatingAsset = ref(false)
const assetForm = ref({
  packageId: pkgId,
  debtorName: '',
  debtorIdNumber: '',
  principalAmount: 0,
  interestAmount: 0,
  guaranteeType: 'CREDIT',
  riskLevel: 'MEDIUM',
  collateral: '',
  remark: ''
})

function formatMoney(val) {
  if (!val && val !== 0) return '¥0.00'
  return '¥' + Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

function statusTag(s) { return { PENDING: 'info', IN_PROGRESS: 'warning', SETTLED: 'success' }[s] || 'info' }
function statusLabel(s) { return { PENDING: '待处置', IN_PROGRESS: '处置中', SETTLED: '已结清' }[s] || s }
function guaranteeLabel(s) { return { CREDIT: '信用', MORTGAGE: '抵押', GUARANTEE: '保证', PLEDGE: '质押' }[s] || '-' }
function lawsuitLabel(s) { return { NOT_SUED: '未诉', SUED: '已诉', ENFORCING: '执行中', TERMINATED: '终本' }[s] || '-' }
function riskTag(s) { return { HIGH: 'danger', MEDIUM: 'warning', LOW: 'success' }[s] || 'info' }
function riskLabel(s) { return { HIGH: '高', MEDIUM: '中', LOW: '低' }[s] || '-' }
function assetStatusTag(s) { return { PENDING: 'info', IN_PROGRESS: 'warning', RECOVERED: 'success', CHARGE_OFF: 'danger' }[s] || 'info' }
function assetStatusLabel(s) { return { PENDING: '待处置', IN_PROGRESS: '处置中', RECOVERED: '已回收', CHARGE_OFF: '核销' }[s] || s }

async function loadPackage() {
  try {
    const res = await getPackageDetail(pkgId)
    pkg.value = res.data
    editForm.value = { ...res.data }
  } catch (e) {
    ElMessage.error('加载失败')
  }
}

async function loadAssets() {
  assetLoading.value = true
  try {
    const res = await getAssetList({ packageId: pkgId, page: assetPage.value - 1, size: assetSize.value })
    assetList.value = res.data.records || res.data.content || []
    assetTotal.value = res.data.total || 0
  } catch {}
  finally { assetLoading.value = false }
}

async function handleEdit() {
  editing.value = true
  try {
    await updatePackage(pkgId, editForm.value)
    ElMessage.success('已更新')
    showEditDialog.value = false
    loadPackage()
  } catch (e) { ElMessage.error('更新失败') }
  finally { editing.value = false }
}

async function handleCreateAsset() {
  if (!assetForm.value.debtorName) { ElMessage.warning('请输入债务人名称'); return }
  creatingAsset.value = true
  try {
    assetForm.value.packageId = pkgId
    await createAsset(assetForm.value)
    ElMessage.success('债权已添加')
    showCreateAsset.value = false
    assetForm.value = { packageId: pkgId, debtorName: '', debtorIdNumber: '', principalAmount: 0, interestAmount: 0, guaranteeType: 'CREDIT', riskLevel: 'MEDIUM', collateral: '', remark: '' }
    loadAssets()
    loadPackage()
  } catch (e) { ElMessage.error('创建失败') }
  finally { creatingAsset.value = false }
}

async function deleteAsset(row) {
  try {
    await ElMessageBox.confirm(`确定删除债务人"${row.debtorName}"的债权？`, '确认')
    await deleteAssetApi(row.id)
    ElMessage.success('已删除')
    loadAssets()
    loadPackage()
  } catch {}
}

onMounted(() => {
  loadPackage()
  loadAssets()
})
</script>

<style scoped lang="scss">
.npa-package-detail { padding: 20px; }
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.info-card { margin-bottom: 0; }
.pagination { margin-top: 12px; display: flex; justify-content: flex-end; }
</style>
