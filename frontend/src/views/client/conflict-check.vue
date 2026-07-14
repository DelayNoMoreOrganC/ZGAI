<template>
  <div class="conflict-check">
    <PageHeader title="利冲检查" :show-back="true" @back="$router.back()">
      <template #extra>
        <el-button @click="handleReset">重置</el-button>
        <el-button type="primary" :loading="checking" @click="handleCheck">
          开始检查
        </el-button>
      </template>
    </PageHeader>

    <div class="check-container">
      <section class="search-panel">
        <el-form ref="formRef" :model="formData" :rules="formRules" label-width="96px">
          <el-form-item label="客户姓名" prop="clientName">
            <el-input
              v-model="formData.clientName"
              size="large"
              placeholder="请输入拟签约客户姓名或名称"
              maxlength="100"
              clearable
              @keyup.enter="handleCheck"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-form-item>
        </el-form>
      </section>

      <section class="result-panel">
        <div class="section-header">
          <h3>客户库核对结果</h3>
          <el-tag v-if="checked" :type="summaryTagType">{{ summaryText }}</el-tag>
        </div>

        <el-empty v-if="!checked" description="输入客户姓名后进行客户库核对" />

        <template v-else>
          <el-alert
            class="summary-alert"
            :type="summaryAlertType"
            :title="summaryText"
            :description="summaryDescription"
            show-icon
            :closable="false"
          />

          <el-table
            v-if="matchedClients.length"
            :data="matchedClients"
            border
            class="result-table"
          >
            <el-table-column prop="clientName" label="客户名称" min-width="180" />
            <el-table-column prop="clientType" label="类型" width="100" />
            <el-table-column prop="clientRelationship" label="关系" width="120">
              <template #default="{ row }">{{ row.clientRelationship || '-' }}</template>
            </el-table-column>
            <el-table-column prop="clientRole" label="客户角色" width="120">
              <template #default="{ row }">{{ row.clientRole || '-' }}</template>
            </el-table-column>
            <el-table-column prop="sourceUserNames" label="案源人" width="140">
              <template #default="{ row }">{{ row.sourceUserNames || '-' }}</template>
            </el-table-column>
            <el-table-column prop="clientOwnerNames" label="承办人" width="140">
              <template #default="{ row }">{{ row.clientOwnerNames || row.ownerName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="departmentName" label="所属部门" width="150">
              <template #default="{ row }">{{ row.departmentName || '-' }}</template>
            </el-table-column>
          </el-table>

          <div v-if="similarNames.length" class="result-block">
            <h4>疑似相似名称</h4>
            <el-tag v-for="name in similarNames" :key="name" type="warning">
              {{ name }}
            </el-tag>
          </div>

          <div class="result-block">
            <h4>核对建议</h4>
            <p>{{ handlingAdvice }}</p>
          </div>
        </template>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { previewConflictCheck, searchClients } from '@/api/client'

const formRef = ref(null)
const checking = ref(false)
const checked = ref(false)
const matchedClients = ref([])
const previewResult = ref(null)

const formData = reactive({
  clientName: ''
})

const formRules = {
  clientName: [{ required: true, message: '请输入拟签约客户姓名或名称', trigger: 'blur' }]
}

const similarNames = computed(() => previewResult.value?.similarClientNames || [])

const summaryText = computed(() => {
  if (!checked.value) return ''
  if (previewResult.value?.conflictLevel === 'DIRECT') return '存在直接利益冲突线索'
  if (matchedClients.value.length > 0) return `客户库中找到 ${matchedClients.value.length} 条记录`
  if (similarNames.value.length > 0) return '未精确命中，发现相似名称'
  return '客户库中未找到匹配记录'
})

const summaryAlertType = computed(() => {
  if (previewResult.value?.conflictLevel === 'DIRECT') return 'error'
  if (matchedClients.value.length > 0 || similarNames.value.length > 0) return 'warning'
  return 'success'
})

const summaryTagType = computed(() => {
  if (summaryAlertType.value === 'error') return 'danger'
  return summaryAlertType.value
})

const summaryDescription = computed(() => {
  if (previewResult.value?.conflictDescription) return previewResult.value.conflictDescription
  if (matchedClients.value.length > 0) return '请根据客户名称、案源人、承办人、所属部门和客户角色进行人工核对。'
  if (similarNames.value.length > 0) return '请核对拟签约客户名称是否存在简称、省市差异或公司后缀差异。'
  return '当前客户库未检索到匹配记录，仍建议在正式立案前由行政管理完成利冲审查。'
})

const handlingAdvice = computed(() => {
  if (previewResult.value?.conflictLevel === 'DIRECT') {
    return '建议暂停签约或立案，联系行政管理制作利冲审查报告，并由负责人进一步确认。'
  }
  if (matchedClients.value.length > 0) {
    return '建议联系既有客户的案源人或承办人，核对是否存在同一客户、关联客户、相对方或正在服务事项。'
  }
  if (similarNames.value.length > 0) {
    return '建议检查客户全称、简称、曾用名、统一社会信用代码或身份证号码，确认是否为同一主体。'
  }
  return '可进入后续签约或立案流程，但此结果仅为客户库初筛，不替代正式利冲审查。'
})

const handleCheck = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    checking.value = true

    const keyword = formData.clientName.trim()
    const [searchRes, previewRes] = await Promise.all([
      searchClients(keyword),
      previewConflictCheck({
        clientName: keyword,
        clientType: '个人',
        clientRelationship: '当事人',
        clientRole: '原告'
      })
    ])

    matchedClients.value = searchRes.success ? (searchRes.data || []) : []
    previewResult.value = previewRes.success ? (previewRes.data || null) : null
    checked.value = true
    ElMessage.success('客户库核对完成')
  } catch (error) {
    if (error !== false) {
      console.error('利冲检查失败:', error)
      ElMessage.error('利冲检查失败，请稍后重试')
    }
  } finally {
    checking.value = false
  }
}

const handleReset = () => {
  formData.clientName = ''
  checked.value = false
  matchedClients.value = []
  previewResult.value = null
  formRef.value?.clearValidate()
}
</script>

<style scoped lang="scss">
.conflict-check {
  .check-container {
    display: flex;
    flex-direction: column;
    gap: 16px;
    padding: 20px;
  }

  .search-panel,
  .result-panel {
    background: #fff;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    padding: 24px;
  }

  .search-panel {
    max-width: 720px;
  }

  .section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 18px;

    h3 {
      margin: 0;
      font-size: 16px;
      font-weight: 600;
      color: #303133;
    }
  }

  .summary-alert {
    margin-bottom: 16px;
  }

  .result-table {
    margin-bottom: 16px;
  }

  .result-block {
    padding: 14px 16px;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    background: #fafafa;
    margin-top: 12px;

    h4 {
      margin: 0 0 10px;
      font-size: 14px;
      font-weight: 600;
      color: #303133;
    }

    p {
      margin: 0;
      color: #303133;
      line-height: 1.7;
    }

    .el-tag {
      margin-right: 8px;
      margin-bottom: 8px;
    }
  }
}
</style>
