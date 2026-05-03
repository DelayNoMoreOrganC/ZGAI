<template>
  <div class="npa-diligence">
    <PageHeader title="尽职调查管理" />

    <el-table :data="diligenceList" border v-loading="loading" stripe>
      <el-table-column prop="debtorName" label="债务人" min-width="140">
        <template #default="{ row }">
          <el-link type="primary" @click="$router.push(`/npa/assets/${row.assetId}`)">{{ row.debtorName }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="investigationDate" label="调查日期" width="120" />
      <el-table-column prop="investigator" label="调查人" width="100" />
      <el-table-column prop="riskLevel" label="风险等级" width="90" align="center">
        <template #default="{ row }">
          <el-tag :type="{HIGH:'danger',MEDIUM:'warning',LOW:'success'}[row.riskLevel] || 'info'" size="small">
            {{ {HIGH:'高',MEDIUM:'中',LOW:'低'}[row.riskLevel] || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="recoveryEstimate" label="预计回收" width="130" align="right">
        <template #default="{ row }">{{ formatMoney(row.recoveryEstimate) }}</template>
      </el-table-column>
      <el-table-column prop="propertyStatus" label="财产状况" min-width="180" show-overflow-tooltip />
      <el-table-column prop="recoveryAnalysis" label="回收分析" min-width="180" show-overflow-tooltip />
      <el-table-column label="AI辅助" width="80" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.aiGenerated" type="success" size="small">🤖是</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="viewDetail(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 详情抽屉 -->
    <el-drawer v-model="showDrawer" :title="'尽调详情 - ' + (currentDd.debtorName || '')" size="500px">
      <el-descriptions v-if="currentDd.id" :column="1" border>
        <el-descriptions-item label="调查日期">{{ currentDd.investigationDate || '-' }}</el-descriptions-item>
        <el-descriptions-item label="调查人">{{ currentDd.investigator || '-' }}</el-descriptions-item>
        <el-descriptions-item label="风险等级">{{ riskLabel(currentDd.riskLevel) }}</el-descriptions-item>
        <el-descriptions-item label="预计回收">{{ formatMoney(currentDd.recoveryEstimate) }}</el-descriptions-item>
        <el-descriptions-item label="财产状况">{{ currentDd.propertyStatus || '-' }}</el-descriptions-item>
        <el-descriptions-item label="经营状况">{{ currentDd.businessStatus || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系历史">{{ currentDd.contactHistory || '-' }}</el-descriptions-item>
        <el-descriptions-item label="回收分析">{{ currentDd.recoveryAnalysis || '-' }}</el-descriptions-item>
        <el-descriptions-item label="调查结论">{{ currentDd.conclusion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="AI辅助生成">{{ currentDd.aiGenerated ? '是' : '否' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { getDiligenceList } from '@/api/npa'
import { getAssetList } from '@/api/npa'

const loading = ref(false)
const diligenceList = ref([])
const showDrawer = ref(false)
const currentDd = ref({})

function formatMoney(v) { return v || v === 0 ? '¥' + Number(v).toLocaleString('zh-CN', { minimumFractionDigits: 2 }) : '-' }
function riskLabel(s) { return { HIGH: '高', MEDIUM: '中', LOW: '低' }[s] || '-' }

async function fetchAll() {
  loading.value = true
  try {
    // 先获取所有债权，再逐条加载尽调
    const res = await getAssetList({ page: 0, size: 999 })
    const assets = res.data.records || res.data.content || []
    const allDd = []
    for (const asset of assets.slice(0, 50)) {
      try {
        const ddRes = await getDiligenceList(asset.id)
        const list = ddRes.data || []
        list.forEach(dd => { dd.debtorName = asset.debtorName })
        allDd.push(...list)
      } catch {}
    }
    allDd.sort((a, b) => new Date(b.investigationDate || 0) - new Date(a.investigationDate || 0))
    diligenceList.value = allDd
  } catch {}
  finally { loading.value = false }
}

function viewDetail(row) {
  currentDd.value = row
  showDrawer.value = true
}

onMounted(() => fetchAll())
</script>

<style scoped lang="scss">
.npa-diligence { padding: 20px; }
</style>
