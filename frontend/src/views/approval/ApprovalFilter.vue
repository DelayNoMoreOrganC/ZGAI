<template>
  <div class="approval-filter">
    <el-form :inline="true" :model="filterForm" class="filter-form">
      <el-form-item label="审批类型">
        <el-select v-model="filterForm.approvalType" placeholder="全部" clearable style="width: 150px">
          <el-option label="全部" value="" />
          <el-option label="立案审批" value="CASE_FILING" />
          <el-option label="主任终审" value="CASE_FILING_DIRECTOR" />
          <el-option label="用印申请" value="SEAL" />
          <el-option label="费用报销" value="REIMBURSEMENT" />
          <el-option label="开票申请" value="INVOICE" />
          <el-option label="请假出差" value="LEAVE" />
          <el-option label="采购申请" value="PURCHASE" />
          <el-option label="证照借用" value="LICENSE" />
        </el-select>
      </el-form-item>

      <el-form-item label="状态">
        <el-select v-model="filterForm.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="全部" value="" />
          <el-option label="待审批" value="PENDING" />
          <el-option label="已同意" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
          <el-option label="已撤回" value="WITHDRAWN" />
        </el-select>
      </el-form-item>

      <el-form-item label="申请时间">
        <el-date-picker
          v-model="filterForm.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 240px"
        />
      </el-form-item>

      <el-form-item label="关键词">
        <el-input
          v-model="filterForm.keyword"
          placeholder="搜索标题/内容/申请人"
          clearable
          style="width: 200px"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
        <el-button @click="handleReset">
          <el-icon><RefreshLeft /></el-icon>
          重置
        </el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const emit = defineEmits(['search', 'reset'])

const filterForm = ref({
  approvalType: '',
  status: '',
  dateRange: null,
  keyword: ''
})

const handleSearch = () => {
  emit('search', { ...filterForm.value })
}

const handleReset = () => {
  filterForm.value = {
    approvalType: '',
    status: '',
    dateRange: null,
    keyword: ''
  }
  emit('reset')
}
</script>

<style scoped>
.approval-filter {
  margin-bottom: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 4px;
}

.filter-form {
  margin: 0;
}

@media (max-width: 768px) {
  .approval-filter {
    padding: 12px;
  }

  .filter-form :deep(.el-form-item) {
    display: flex;
    width: 100%;
    margin-right: 0;
  }

  .filter-form :deep(.el-form-item__content) {
    flex: 1;
    min-width: 0;
  }

  .filter-form :deep(.el-select),
  .filter-form :deep(.el-input),
  .filter-form :deep(.el-date-editor) {
    width: 100% !important;
  }
}
</style>
