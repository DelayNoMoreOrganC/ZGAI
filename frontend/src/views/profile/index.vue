<template>
  <div class="profile-page">
    <PageHeader title="个人中心" />

    <section class="profile-section">
      <div class="identity-row">
        <el-avatar :size="56" :src="user.avatar">{{ initials }}</el-avatar>
        <div>
          <h3>{{ user.realName || user.username }}</h3>
          <p>{{ user.position || '未设置身份类别' }}</p>
        </div>
      </div>

      <el-descriptions :column="isMobile ? 1 : 2" border>
        <el-descriptions-item label="账号">{{ user.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="身份类别">{{ user.position || '-' }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ user.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ user.email || '-' }}</el-descriptions-item>
      </el-descriptions>
    </section>

    <section class="profile-section password-section">
      <h3>修改密码</h3>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input v-model="form.oldPassword" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-button type="primary" :loading="submitting" @click="submit">更新密码</el-button>
      </el-form>
    </section>
  </div>
</template>

<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { changePassword } from '@/api/auth'
import { useUserStore } from '@/stores'

const userStore = useUserStore()
const formRef = ref()
const submitting = ref(false)
const isMobile = computed(() => window.innerWidth < 768)
const user = computed(() => userStore.userInfo || {})
const initials = computed(() => (user.value.realName || user.value.username || 'Z').charAt(0))

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmation = (rule, value, callback) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
    return
  }
  callback()
}

const rules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, max: 100, message: '密码长度为 8 至 100 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmation, trigger: 'blur' }
  ]
}

const submit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await changePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword
    })
    form.oldPassword = ''
    form.newPassword = ''
    form.confirmPassword = ''
    formRef.value?.clearValidate()
    ElMessage.success('密码已更新')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.profile-page {
  max-width: 920px;
  margin: 0 auto;
}

.profile-section {
  padding: 24px;
  background: #fff;
  border: 1px solid #e2e5e9;
  border-radius: 8px;
}

.profile-section + .profile-section {
  margin-top: 16px;
}

.identity-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.identity-row h3,
.password-section h3 {
  margin: 0;
  color: #1d1d1f;
  font-size: 18px;
  font-weight: 600;
}

.identity-row p {
  margin: 6px 0 0;
  color: #6e737a;
}

.password-section :deep(.el-form) {
  max-width: 480px;
  margin-top: 20px;
}

@media (max-width: 767px) {
  .profile-section {
    padding: 18px;
  }
}
</style>
