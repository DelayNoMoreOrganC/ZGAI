<template>
  <div class="client-create">
    <PageHeader title="新建客户" :show-back="true" @back="$router.back()">
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
                  <el-radio label="personal">个人</el-radio>
                  <el-radio label="company">企业</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="客户等级" prop="level">
                <el-select v-model="formData.level" placeholder="请选择客户等级">
                  <el-option label="普通" value="normal" />
                  <el-option label="重要" value="important" />
                  <el-option label="VIP" value="vip" />
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
                  <el-radio label="男">男</el-radio>
                  <el-radio label="女">女</el-radio>
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
                  placeholder="请输入联系电话"
                  maxlength="11"
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
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { createClient } from '@/api/client'

const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)

const formData = reactive({
  type: 'personal',
  level: 'normal',
  name: '',
  gender: '男',
  idCard: '',
  creditCode: '',
  phone: '',
  email: '',
  address: '',
  remark: ''
})

// 表单校验规则
const formRules = {
  type: [{ required: true, message: '请选择客户类型', trigger: 'change' }],
  level: [{ required: true, message: '请选择客户等级', trigger: 'change' }],
  name: [
    { required: true, message: '请输入客户姓名', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
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

    const response = await createClient(formData)

    if (response.success) {
      ElMessage.success('客户创建成功')
      router.push('/client')
    } else {
      ElMessage.error(response.message || '客户创建失败')
    }
  } catch (error) {
    if (error !== false) { // 排除表单验证失败
      console.error('客户创建失败:', error)
      ElMessage.error('客户创建失败，请稍后重试')
    }
  } finally {
    submitting.value = false
  }
}

// 取消
const handleCancel = () => {
  router.back()
}
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