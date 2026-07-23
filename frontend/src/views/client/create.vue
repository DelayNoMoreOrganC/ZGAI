<template>
  <div class="client-create">
    <PageHeader :title="isEdit ? '编辑客户' : '新建客户'" :show-back="true" @back="$router.back()">
      <template #extra>
        <el-button @click="handleCancel">取消</el-button>
        <el-button data-testid="client-submit" type="primary" :loading="submitting" @click="handleSubmit">
          提交
        </el-button>
      </template>
    </PageHeader>

    <div class="create-container">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="132px"
        class="client-form"
      >
        <div class="form-section">
          <div class="section-header">
            <h3>客户基础信息</h3>
          </div>

          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="客户类型" prop="clientType">
                <el-select data-testid="client-type" v-model="formData.clientType" placeholder="请选择客户类型" style="width: 100%" @change="handleTypeChange">
                  <el-option v-for="item in clientTypeOptions" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="客户与律所关系" prop="clientRelationship">
                <el-select v-model="formData.clientRelationship" placeholder="请选择关系" style="width: 100%">
                  <el-option v-for="item in relationshipOptions" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="客户角色" prop="clientRole">
                <el-select data-testid="client-role" v-model="formData.clientRole" placeholder="请选择客户角色" style="width: 100%">
                  <el-option v-for="item in clientRoleOptions" :key="item" :label="item" :value="item" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="客户名称" prop="clientName">
            <el-input
              data-testid="client-name"
              v-model="formData.clientName"
              placeholder="请输入客户名称"
              maxlength="100"
              show-word-limit
            />
          </el-form-item>

          <el-row :gutter="20">
            <el-col :span="8" v-if="isIndividual">
              <el-form-item label="性别" prop="gender">
                <el-radio-group v-model="formData.gender">
                  <el-radio value="男">男</el-radio>
                  <el-radio value="女">女</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>

            <el-col :span="8" v-if="isIndividual">
              <el-form-item label="民族" prop="ethnicity">
                <el-input v-model="formData.ethnicity" placeholder="请输入民族" maxlength="30" />
              </el-form-item>
            </el-col>

            <el-col :span="8" v-if="isIndividual">
              <el-form-item label="身份证号码" prop="idCard">
                <el-input v-model="formData.idCard" placeholder="个人客户必填" maxlength="18" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20" v-if="!isIndividual">
            <el-col :span="8">
              <el-form-item label="统一社会信用代码" prop="creditCode">
                <el-input v-model="formData.creditCode" placeholder="请输入统一社会信用代码" maxlength="18" />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="法人姓名" prop="legalRepresentative">
                <el-input v-model="formData.legalRepresentative" placeholder="请输入法人姓名" maxlength="50" />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="法人身份证号码" prop="legalRepresentativeIdCard">
                <el-input v-model="formData.legalRepresentativeIdCard" placeholder="请输入法人身份证号码" maxlength="18" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="联系人" prop="contactPerson">
                <el-input v-model="formData.contactPerson" placeholder="请输入联系人" maxlength="50" />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="联系电话" prop="phone">
                <el-input v-model="formData.phone" placeholder="手机号、固话或区号+固话" maxlength="24" />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="微信号" prop="wechat">
                <el-input v-model="formData.wechat" placeholder="请输入微信号" maxlength="50" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="formData.email" placeholder="请输入邮箱地址" maxlength="100" />
              </el-form-item>
            </el-col>

            <el-col :span="16">
              <el-form-item label="地址" prop="address">
                <el-input v-model="formData.address" placeholder="请输入联系地址" maxlength="200" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="form-section">
          <div class="section-header">
            <h3>归属信息</h3>
          </div>

          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="客户所属部门" prop="departmentId">
                <el-select
                  data-testid="client-department"
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

            <el-col :span="8">
              <el-form-item label="案源人" prop="sourceUserIds">
                <el-select
                  data-testid="client-source-users"
                  v-model="formData.sourceUserIds"
                  multiple
                  filterable
                  collapse-tags
                  placeholder="请选择案源人"
                  style="width: 100%"
                >
                  <el-option v-for="user in userOptions" :key="user.id" :label="user.realName" :value="user.id" />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="客户所属人" prop="clientOwnerIds">
                <el-select
                  data-testid="client-owner-users"
                  v-model="formData.clientOwnerIds"
                  multiple
                  filterable
                  collapse-tags
                  placeholder="请选择所属人"
                  style="width: 100%"
                >
                  <el-option v-for="user in userOptions" :key="user.id" :label="user.realName" :value="user.id" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <div class="form-section">
          <div class="section-header">
            <h3>开票与补充信息</h3>
          </div>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="购方名称" prop="invoiceTitle">
                <el-input v-model="formData.invoiceTitle" placeholder="请输入购方名称" maxlength="100" />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="税号" prop="invoiceTaxNo">
                <el-input v-model="formData.invoiceTaxNo" placeholder="请输入税号" maxlength="50" />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="地址和电话" prop="invoiceAddressPhone">
                <el-input v-model="formData.invoiceAddressPhone" placeholder="请输入开票地址和电话" maxlength="200" />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="开户行及账号" prop="invoiceBankAccount">
                <el-input v-model="formData.invoiceBankAccount" placeholder="请输入开户行及账号" maxlength="200" />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="对方代理律师" prop="opposingLawyer">
                <el-input v-model="formData.opposingLawyer" placeholder="请输入对方代理律师" maxlength="100" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="备注" prop="notes">
            <el-input
              v-model="formData.notes"
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
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { createClient, getClientDetail, updateClient, previewConflictCheck } from '@/api/client'
import { getDepartmentList } from '@/api/department'
import { getUserOptions } from '@/api/user'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const submitting = ref(false)
const isEdit = computed(() => Boolean(route.params.id))
const isIndividual = computed(() => formData.clientType === '个人')
const isPrincipalClient = computed(() => formData.clientRelationship === '委托人')

const clientTypeOptions = ['个人', '企业', '金融机构', '事业单位', '党政机关', '社会团体', '其他']
const relationshipOptions = ['委托人', '当事人', '对方当事人', '顾问单位', '关联企业', '股东', '法定代表人']
const clientRoleOptions = [
  '委托人', '顾问单位',
  '原告', '被告', '第三人', '共同被告', '共同原告',
  '申请人', '被申请人', '上诉人', '被上诉人',
  '管理人', '债权人', '行政相对人', '被害人'
]

const formData = reactive({
  clientType: '个人',
  clientRelationship: '委托人',
  clientRole: '',
  departmentId: null,
  sourceUserIds: [],
  clientOwnerIds: [],
  clientName: '',
  gender: '男',
  ethnicity: '',
  idCard: '',
  creditCode: '',
  contactPerson: '',
  phone: '',
  email: '',
  wechat: '',
  address: '',
  legalRepresentative: '',
  legalRepresentativeIdCard: '',
  invoiceTitle: '',
  invoiceTaxNo: '',
  invoiceAddressPhone: '',
  invoiceBankAccount: '',
  opposingLawyer: '',
  notes: ''
})

const departmentOptions = ref([])
const userOptions = ref([])

const validateIdCardWhenIndividual = (_rule, value, callback) => {
  if (!isIndividual.value) {
    callback()
    return
  }
  if (!value) {
    callback(new Error('个人客户必须填写身份证号码'))
    return
  }
  if (!/(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/.test(value)) {
    callback(new Error('请输入正确的身份证号码'))
    return
  }
  callback()
}

const validatePrincipalRequired = (message) => (_rule, value, callback) => {
  if (!isPrincipalClient.value) {
    callback()
    return
  }
  if (Array.isArray(value) ? value.length === 0 : !value) {
    callback(new Error(message))
    return
  }
  callback()
}

const formRules = {
  clientType: [{ required: true, message: '请选择客户类型', trigger: 'change' }],
  clientRelationship: [{ required: true, message: '请选择客户与律所关系', trigger: 'change' }],
  clientRole: [{ required: true, message: '请选择客户角色', trigger: 'change' }],
  clientName: [
    { required: true, message: '请输入客户名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  departmentId: [{ validator: validatePrincipalRequired('委托人客户必须选择客户所属部门'), trigger: 'change' }],
  sourceUserIds: [{ validator: validatePrincipalRequired('委托人客户必须选择案源人'), trigger: 'change' }],
  clientOwnerIds: [{ validator: validatePrincipalRequired('委托人客户必须选择客户所属人'), trigger: 'change' }],
  phone: [
    {
      pattern: /^(1[3-9]\d{9}|0\d{2,3}-?\d{7,8}|0\d{2,3}\d{7,8})(-\d{1,6})?$/,
      message: '请输入正确的手机号、固话或区号+固话',
      trigger: 'blur'
    }
  ],
  email: [
    { pattern: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/, message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  idCard: [{ validator: validateIdCardWhenIndividual, trigger: 'blur' }],
  creditCode: [
    { pattern: /^[0-9A-HJ-NPQRTUWXY]{2}\d{6}[0-9A-HJ-NPQRTUWXY]{10}$/, message: '请输入正确的统一社会信用代码', trigger: 'blur' }
  ],
  legalRepresentativeIdCard: [
    { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '请输入正确的身份证号码', trigger: 'blur' }
  ]
}

const isSuccessResponse = (response) => response?.success || response?.code === 200

const idsToString = (ids) => (ids || []).join(',')
const stringToIds = (value) => {
  if (!value) return []
  return String(value).split(',').map(item => Number(item)).filter(Boolean)
}

const buildPayload = () => ({
  clientType: formData.clientType,
  clientRelationship: formData.clientRelationship,
  clientRole: formData.clientRole,
  clientName: formData.clientName,
  gender: isIndividual.value ? formData.gender : '',
  ethnicity: isIndividual.value ? formData.ethnicity : '',
  idCard: isIndividual.value ? formData.idCard : '',
  creditCode: !isIndividual.value ? formData.creditCode : '',
  contactPerson: formData.contactPerson,
  phone: formData.phone,
  email: formData.email,
  wechat: formData.wechat,
  address: formData.address,
  legalRepresentative: !isIndividual.value ? formData.legalRepresentative : '',
  legalRepresentativeIdCard: !isIndividual.value ? formData.legalRepresentativeIdCard : '',
  invoiceTitle: formData.invoiceTitle,
  invoiceTaxNo: formData.invoiceTaxNo,
  invoiceAddressPhone: formData.invoiceAddressPhone,
  invoiceBankAccount: formData.invoiceBankAccount,
  opposingLawyer: formData.opposingLawyer,
  notes: formData.notes,
  departmentId: formData.departmentId,
  sourceUserIds: idsToString(formData.sourceUserIds),
  clientOwnerIds: idsToString(formData.clientOwnerIds),
  ownerId: formData.clientOwnerIds[0] || null,
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

const loadUsers = async () => {
  try {
    const response = await getUserOptions({ size: 200 })
    userOptions.value = response.data || []
  } catch (error) {
    console.error('加载人员列表失败:', error)
    ElMessage.error('加载人员列表失败')
  }
}

const loadClient = async () => {
  if (!isEdit.value) return

  try {
    const response = await getClientDetail(route.params.id)
    const client = response.data || {}
    formData.clientType = client.clientType || '个人'
    formData.clientRelationship = client.clientRelationship || '委托人'
    formData.clientRole = client.clientRole || ''
    formData.departmentId = client.departmentId || null
    formData.sourceUserIds = stringToIds(client.sourceUserIds)
    formData.clientOwnerIds = stringToIds(client.clientOwnerIds || client.ownerId)
    formData.clientName = client.clientName || ''
    formData.gender = client.gender || '男'
    formData.ethnicity = client.ethnicity || ''
    formData.idCard = client.idCard || ''
    formData.creditCode = client.creditCode || ''
    formData.contactPerson = client.contactPerson || ''
    formData.phone = client.phone || ''
    formData.email = client.email || ''
    formData.wechat = client.wechat || ''
    formData.address = client.address || ''
    formData.legalRepresentative = client.legalRepresentative || ''
    formData.legalRepresentativeIdCard = client.legalRepresentativeIdCard || ''
    formData.invoiceTitle = client.invoiceTitle || ''
    formData.invoiceTaxNo = client.invoiceTaxNo || ''
    formData.invoiceAddressPhone = client.invoiceAddressPhone || ''
    formData.invoiceBankAccount = client.invoiceBankAccount || ''
    formData.opposingLawyer = client.opposingLawyer || ''
    formData.notes = client.notes || ''
  } catch (error) {
    console.error('加载客户信息失败:', error)
    ElMessage.error('加载客户信息失败')
  }
}

const handleTypeChange = () => {
  if (isIndividual.value) {
    formData.creditCode = ''
    formData.legalRepresentative = ''
    formData.legalRepresentativeIdCard = ''
  } else {
    formData.gender = '男'
    formData.ethnicity = ''
    formData.idCard = ''
  }
}

const runConflictPreview = async (payload) => {
  if (isEdit.value) return true

  const response = await previewConflictCheck(payload)
  if (!isSuccessResponse(response) || !response.data) return true
  const result = response.data

  if (result.conflictLevel === 'DIRECT') {
    ElMessageBox.alert(result.conflictDescription || '存在直接利益冲突，请停止建档并提交行政管理审查。', '利益冲突提示', {
      type: 'error',
      confirmButtonText: '知道了'
    })
    return false
  }

  if (result.conflictLevel === 'SIMILAR') {
    await ElMessageBox.confirm(
      result.conflictDescription || '发现高度相似客户，请核实客户名称是否正确。是否继续新增？',
      '客户名称核实',
      {
        type: 'warning',
        confirmButtonText: '继续新增',
        cancelButtonText: '返回修改'
      }
    )
  }

  if (result.conflictLevel === 'EXISTING' && result.conflictDescription) {
    ElMessage.warning(result.conflictDescription)
  }

  return true
}

const handleSubmit = async () => {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
    submitting.value = true

    const payload = buildPayload()
    const canContinue = await runConflictPreview(payload)
    if (!canContinue) return

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
    if (error !== false && error !== 'cancel') {
      console.error(isEdit.value ? '客户更新失败:' : '客户创建失败:', error)
      ElMessage.error(isEdit.value ? '客户更新失败，请稍后重试' : '客户创建失败，请稍后重试')
    }
  } finally {
    submitting.value = false
  }
}

const handleCancel = () => {
  router.back()
}

onMounted(() => {
  loadDepartments()
  loadUsers()
  loadClient()
})
</script>

<style scoped lang="scss">
.client-create {
  .create-container {
    max-width: 1120px;
    margin: 0 auto;
    padding: 20px;
  }

  .client-form {
    display: flex;
    flex-direction: column;
    gap: 16px;

    .form-section {
      background: #fff;
      padding: 24px;
      border: 1px solid #ebeef5;
      border-radius: 8px;

      .section-header {
        margin-bottom: 24px;
        padding-bottom: 12px;
        border-bottom: 1px solid #ebeef5;

        h3 {
          margin: 0;
          font-size: 16px;
          font-weight: 600;
          color: #303133;
        }
      }
    }
  }
}
</style>
