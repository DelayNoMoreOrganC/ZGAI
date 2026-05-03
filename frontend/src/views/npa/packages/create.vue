<template>
  <div class="npa-create-package">
    <PageHeader title="新建资产包" :show-back="true" />

    <el-card>
      <el-form :model="form" label-width="120px" :rules="rules" ref="formRef">
        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="资产包名称" prop="packageName">
              <el-input v-model="form.packageName" placeholder="如：XX银行2024年第一批不良资产包" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资产包编号" prop="packageCode">
              <el-input v-model="form.packageCode" placeholder="系统自动生成或手动输入" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="委托银行/AMC" prop="bankName">
              <el-input v-model="form.bankName" placeholder="如：中国工商银行XX分行" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人" prop="responsiblePerson">
              <el-input v-model="form.responsiblePerson" placeholder="负责人姓名" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="债权总额">
              <el-input-number v-model="form.totalAmount" :min="0" :precision="2" style="width: 100%" placeholder="0.00" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="债权笔数">
              <el-input-number v-model="form.assetCount" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="收购日期">
              <el-date-picker v-model="form.acquisitionDate" type="date" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="处置期限">
              <el-date-picker v-model="form.deadlineDate" type="date" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="资产包描述或备注" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">创建资产包</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import { createPackage } from '@/api/npa'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)

const form = reactive({
  packageName: '',
  packageCode: '',
  bankName: '',
  responsiblePerson: '',
  totalAmount: 0,
  assetCount: 0,
  acquisitionDate: null,
  deadlineDate: null,
  description: ''
})

const rules = {
  packageName: [{ required: true, message: '请输入资产包名称', trigger: 'blur' }],
  bankName: [{ required: true, message: '请输入委托银行', trigger: 'blur' }]
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await createPackage(form)
    ElMessage.success('资产包创建成功')
    router.push('/npa/packages')
  } catch (e) {
    ElMessage.error('创建失败: ' + (e.message || '未知错误'))
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="scss">
.npa-create-package {
  padding: 20px;
}
</style>
