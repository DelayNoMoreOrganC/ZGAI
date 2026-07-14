<template>
  <div class="client-create">
    <PageHeader :title="isEdit ? '编辑客户' : '新建客户'" :show-back="true" @back="$router.back()">
      <template #extra>
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          提交
        </el-button>
      </template>
    </PageHeader>

    <div class="create-container">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
        class="client-form"
      >
        <!-- 基本信息 -->
        <div class="form-section">
          <div class="section-header">
            <h3>基本信息</h3>
          </div>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="客户类型" prop="type">
                <el-radio-group v-model="formData.type" @change="handleTypeChange">
                  <el-radio value="personal">个人</el-radio>
                  <el-radio value="company">企业</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="所属部门" prop="departmentId">
                <el-select
                  v-model="formData.departmentId"
                  placeholder="请选择所属部门"
                  clearable
                  filterable
                  style="width: 100%"
                >
                  <el-option
                    v-for="department in departmentOptions"
                    :key="department.id"
                    :label="department.deptName"
                    :value="department.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item :label="formData.type === 'personal' ? '姓名' : '企业名称'" prop="name">
            <el-input
              v-model="formData.name"
              :placeholder="formData.type === 'personal' ? '请输入客户姓名' : '请输入企业名称'"
              maxlength="100"
              show-word-limit
            />
          </el-form-item>

          <el-row :gutter="20" v-if="formData.type === 'personal'">
            <el-col :span="12">
              <el-form-item label="性别" prop="gender">
                <el-radio-group v-model="formData.gender">
                  <el-radio value="男">男</el-radio>
                  <el-radio value="女">女</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="身份证号" prop="idCard">
                <el-input
                  v-model="formData.idCard"
                  placeholder="请输入身份证号"
                  maxlength="18"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="统一社会信用代码" prop="creditCode" v-if="formData.type === 'company'">
            <el-input
              v-model="formData.creditCode"
              placeholder="请输入统一社会信用代码"
              maxlength="18"
            />
          </el-form-item>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="联系电话" prop="phone">
                <el-input
                  v-model="formData.phone"
                  placeholder="请输入手机号或区号+固话"
                  maxlength="20"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="邮箱" prop="email">
                <el-input
                  v-model="formData.email"
                  placeholder="请输入邮箱地址"
                  maxlength="100"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="联系地址" prop="address">
            <el-input
              v-model="formData.address"
              type="textarea"
              :rows="3"
              placeholder="请输入联系地址"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>

          <el-form-item label="备注" prop="remark">
            <el-input
              v-model="formData.remark"
              type="textarea"
              :rows="4"
              placeholder="请输入备注信息"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { createClient, getClientDetail, updateClient } from '@/api/client'
import { getDepartmentList } from '@/api/department'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const submitting = ref(false)
const isEdit = computed(() => Boolean(route.params.id))

const formData = reactive({
  type: 'personal',
  departmentId: null,
  name: '',
  gender: '男',
  idCard: '',
  creditCode: '',
  phone: '',
  email: '',
  address: '',
  remark: ''
})
const departmentOptions = ref([])

// 表单校验规则
const formRules = {
  type: [{ required: true, message: '请选择客户类型', trigger: 'change' }],
  departmentId: [{ required: true, message: '请选择所属部门', trigger: 'change' }],
  name: [
    { required: true, message: '请输入客户姓名', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    {
      pattern: /^(1[3-9]\d{9}|0\d{2,3}-?\d{7,8})(-\d{1,6})?$/,
      message: '请输入正确的手机号或区号+固话',
      trigger: 'blur'
    }
  ],
  email: [
    { pattern: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/, message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  idCard: [
    { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '请输入正确的身份证号码', trigger: 'blur' }
  ],
  creditCode: [
    { pattern: /^[0-9A-HJ-NPQRTUWXY]{2}\d{6}[0-9A-HJ-NPQRTUWXY]{10}$/, message: '请输入正确的统一社会信用代码', trigger: 'blur' }
  ]
}

const toBackendClientType = (type) => {
  if (type === 'company' || type === '企业') return '企业'
  return '个人'
}

const toFormClientType = (type) => {
  if (type === '企业' || type === 'company') return 'company'
  return 'personal'
}

const buildPayload = () => ({
  clientType: toBackendClientType(formData.type),
  clientName: formData.name,
  gender: formData.type === 'personal' ? formData.gender : '',
  idCard: formData.type === 'personal' ? formData.idCard : '',
  creditCode: formData.type === 'company' ? formData.creditCode : '',
  phone: formData.phone,
  email: formData.email,
  address: formData.address,
  notes: formData.remark,
  departmentId: formData.departmentId,
  status: 'ACTIVE'
})

const loadDepartments = async () => {
  try {
    const response = await getDepartmentList()
    departmentOptions.value = response.data || []
  } catch (error) {
    console.error('加载部门列表失败:', error)
    ElMessage.error('加载部门列表失败')
  }
}

const loadClient = async () => {
  if (!isEdit.value) return

  try {
    const response = await getClientDetail(route.params.id)
    const client = response.data || {}
    formData.type = toFormClientType(client.clientType)
    formData.departmentId = client.departmentId || null
    formData.name = client.clientName || ''
    formData.gender = client.gender || '男'
    formData.idCard = client.idCard || ''
    formData.creditCode = client.creditCode || ''
    formData.phone = client.phone || ''
    formData.email = client.email || ''
    formData.address = client.address || ''
    formData.remark = client.notes || ''
  } catch (error) {
    console.error('加载客户信息失败:', error)
    ElMessage.error('加载客户信息失败')
  }
}

// 客户类型切换
const handleTypeChange = (type) => {
  // 清空特定类型的字段
  if (type === 'personal') {
    formData.creditCode = ''
  } else {
    formData.gender = '男'
    formData.idCard = ''
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    submitting.value = true

    const payload = buildPayload()
    const response = isEdit.value
      ? await updateClient(route.params.id, payload)
      : await createClient(payload)

    if (response.success) {
      ElMessage.success(isEdit.value ? '客户更新成功' : '客户创建成功')
      router.push('/client/list')
    } else {
      ElMessage.error(response.message || (isEdit.value ? '客户更新失败' : '客户创建失败'))
    }
  } catch (error) {
    if (error !== false) { // 排除表单验证失败
      console.error(isEdit.value ? '客户更新失败:' : '客户创建失败:', error)
      ElMessage.error(isEdit.value ? '客户更新失败，请稍后重试' : '客户创建失败，请稍后重试')
    }
  } finally {
    submitting.value = false
  }
}

// 取消
const handleCancel = () => {
  router.back()
}

onMounted(() => {
  loadDepartments()
  loadClient()
})
</script>

<style scoped lang="scss">
.client-create {
  .create-container {
    max-width: 900px;
    margin: 0 auto;
    padding: 20px;
  }

  .client-form {
    .form-section {
      background: #fff;
      padding: 30px;
      border-radius: 4px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);

      .section-header {
        margin-bottom: 30px;
        padding-bottom: 15px;
        border-bottom: 1px solid #ebeef5;

        h3 {
          margin: 0;
          font-size: 16px;
          font-weight: 500;
          color: #303133;
        }
      }
    }
  }
}
</style>
