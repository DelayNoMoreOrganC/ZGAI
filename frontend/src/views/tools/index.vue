<template>
  <div class="tools-page">
    <PageHeader title="工具集" />
    <el-row :gutter="20" class="tools-container">
      <el-col :span="8">
        <el-card class="tool-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><Money /></el-icon> 诉讼费计算器</span>
            </div>
          </template>
          <el-form label-width="100px">
            <el-form-item label="争议金额">
              <el-input-number v-model="litigation.amount" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
            <el-form-item label="案件类型">
              <el-select v-model="litigation.type" style="width: 100%">
                <el-option label="财产案件" value="property" />
                <el-option label="离婚案件" value="divorce" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="calculateLitigation" style="width: 100%">计算</el-button>
            </el-form-item>
          </el-form>
          <el-alert v-if="litigation.result" :title="'¥' + litigation.result" type="success" :closable="false" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="tool-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><TrendCharts /></el-icon> 利息计算器</span>
            </div>
          </template>
          <el-form label-width="100px">
            <el-form-item label="本金">
              <el-input-number v-model="interest.principal" :min="0" :precision="2" style="width: 100%" />
            </el-form-item>
            <el-form-item label="年利率(%)">
              <el-input-number v-model="interest.rate" :min="0" :max="100" :precision="4" style="width: 100%" />
            </el-form-item>
            <el-form-item label="天数">
              <el-input-number v-model="interest.days" :min="0" style="width: 100%" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="calculateInterest" style="width: 100%">计算</el-button>
            </el-form-item>
          </el-form>
          <el-alert v-if="interest.result" :title="'¥' + interest.result" type="success" :closable="false" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="tool-card">
          <template #header>
            <div class="card-header">
              <span><el-icon><Calendar /></el-icon> 时效计算器</span>
            </div>
          </template>
          <el-form label-width="100px">
            <el-form-item label="起始日期">
              <el-date-picker v-model="limitation.startDate" type="date" style="width: 100%" />
            </el-form-item>
            <el-form-item label="时效类型">
              <el-select v-model="limitation.type" style="width: 100%">
                <el-option label="普通诉讼时效(3年)" :value="3" />
                <el-option label="短期时效(1年)" :value="1" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="calculateLimitation" style="width: 100%">计算</el-button>
            </el-form-item>
          </el-form>
          <el-alert v-if="limitation.result" :title="limitation.result" type="success" :closable="false" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Money, TrendCharts, Calendar } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'

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

const interest = ref({ principal: 0, rate: 4.35, days: 0, result: null })
const calculateInterest = () => {
  const { principal, rate, days } = interest.value
  if (principal <= 0 || days <= 0) {
    ElMessage.warning('请输入有效数值')
    return
  }
  const interestAmount = (principal * rate / 100 / 365) * days
  interest.value.result = interestAmount.toFixed(2)
  ElMessage.success('计算完成')
}

const limitation = ref({ startDate: null, type: 3, result: null })
const calculateLimitation = () => {
  if (!limitation.value.startDate) {
    ElMessage.warning('请选择日期')
    return
  }
  const endDate = new Date(limitation.value.startDate)
  endDate.setFullYear(endDate.getFullYear() + limitation.value.type)
  limitation.value.result = '截止日期：' + endDate.toLocaleDateString('zh-CN')
  ElMessage.success('计算完成')
}
</script>

<style scoped lang="scss">
.tools-page {
  .tools-container { margin-top: 20px; }
  .tool-card { margin-bottom: 20px; }
}
</style>
