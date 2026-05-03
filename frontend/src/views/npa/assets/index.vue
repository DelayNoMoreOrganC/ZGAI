<template>
  <div class="npa-assets">
    <PageHeader title="债权管理" />

    <div class="toolbar">
      <el-input v-model="searchQuery" placeholder="搜索债务人..." clearable style="width: 250px" @keyup.enter="handleSearch">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 130px" @change="fetchData">
        <el-option label="待处置" value="PENDING" />
        <el-option label="处置中" value="IN_PROGRESS" />
        <el-option label="已回收" value="RECOVERED" />
        <el-option label="核销" value="CHARGE_OFF" />
      </el-select>
      <el-select v-model="filterRisk" placeholder="风险等级" clearable style="width: 120px" @change="fetchData">
        <el-option label="高" value="HIGH" />
        <el-option label="中" value="MEDIUM" />
        <el-option label="低" value="LOW" />
      </el-select>
    </div>

    <el-table :data="assetList" border v-loading="loading" stripe>
      <el-table-column type="index" label="#" width="50" />
      <el-table-column prop="debtorName" label="债务人" min-width="140">
        <template #default="{ row }">
          <el-link type="primary" @click="$router.push(`/npa/assets/${row.id}`)">{{ row.debtorName }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="packageName" label="所属资产包" width="160" />
      <el-table-column prop="principalAmount" label="本金" width="130" align="right">
        <template #default="{ row }">{{ formatMoney(row.principalAmount) }}</template>
      </el-table-column>
      <el-table-column prop="totalAmount" label="债权总额" width="130" align="right">
        <template #default="{ row }">{{ formatMoney(row.totalAmount) }}</template>
      </el-table-column>
      <el-table-column prop="guaranteeType" label="担保方式" width="90" align="center">
        <template #default="{ row }">{{ guaranteeLabel(row.guaranteeType) }}</template>
      </el-table-column>
      <el-table-column prop="lawsuitStatus" label="诉讼状态" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="lawsuitTag(row.lawsuitStatus)" size="small">{{ lawsuitLabel(row.lawsuitStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="recoveredAmount" label="已回收" width="120" align="right">
        <template #default="{ row }">{{ formatMoney(row.recoveredAmount) }}</template>
      </el-table-column>
      <el-table-column prop="riskLevel" label="风险" width="70" align="center">
        <template #default="{ row }">
          <el-tag :type="riskTag(row.riskLevel)" size="small">{{ riskLabel(row.riskLevel) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="$router.push(`/npa/assets/${row.id}`)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination v-model:current-page="page" :page-size="size" :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { getAssetList, searchDebtor } from '@/api/npa'

const loading = ref(false)
const assetList = ref([])
const page = ref(1)
const size = ref(15)
const total = ref(0)
const searchQuery = ref('')
const filterStatus = ref('')
const filterRisk = ref('')

function formatMoney(v) { return v || v === 0 ? '¥' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : '-' }
function guaranteeLabel(s) { return { CREDIT: '信用', MORTGAGE: '抵押', GUARANTEE: '保证', PLEDGE: '质押' }[s] || '-' }
function lawsuitLabel(s) { return { NOT_SUED: '未诉', SUED: '已诉', ENFORCING: '执行中', TERMINATED: '终本' }[s] || '-' }
function lawsuitTag(s) { return { NOT_SUED: 'info', SUED: 'primary', ENFORCING: 'warning', TERMINATED: 'danger' }[s] || 'info' }
function riskTag(s) { return { HIGH: 'danger', MEDIUM: 'warning', LOW: 'success' }[s] || 'info' }
function riskLabel(s) { return { HIGH: '高', MEDIUM: '中', LOW: '低' }[s] || '-' }
function statusTag(s) { return { PENDING: 'info', IN_PROGRESS: 'warning', RECOVERED: 'success', CHARGE_OFF: 'danger' }[s] || 'info' }
function statusLabel(s) { return { PENDING: '待处置', IN_PROGRESS: '处置中', RECOVERED: '已回收', CHARGE_OFF: '核销' }[s] || s }

async function fetchData() {
  loading.value = true
  try {
    const params = { page: page.value - 1, size: size.value }
    if (filterStatus.value) params.status = filterStatus.value
    if (filterRisk.value) params.riskLevel = filterRisk.value
    const res = await getAssetList(params)
    assetList.value = res.data.records || res.data.content || []
    total.value = res.data.total || 0
  } catch (e) { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

async function handleSearch() {
  if (!searchQuery.value.trim()) { fetchData(); return }
  loading.value = true
  try {
    const res = await searchDebtor(searchQuery.value)
    assetList.value = res.data || []
    total.value = assetList.value.length
  } catch {}
  finally { loading.value = false }
}

onMounted(() => fetchData())
</script>

<style scoped lang="scss">
.npa-assets { padding: 20px; }
.toolbar { display: flex; gap: 12px; margin-bottom: 16px; }
.pagination { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
