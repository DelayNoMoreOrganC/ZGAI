<template>
  <div class="login-page" :class="{ 'video-unavailable': videoUnavailable }">
    <video
      class="background-video"
      autoplay
      loop
      muted
      playsinline
      preload="auto"
      aria-hidden="true"
      @canplay="videoReady = true"
      @error="videoUnavailable = true"
    >
      <source :src="backgroundVideo" type="video/mp4">
    </video>

    <header class="brand-bar liquid-glass" :class="{ visible: videoReady || videoUnavailable }">
      <div class="brand-identity">
        <span class="brand-mark">Z</span>
        <span class="brand-name">广东至高律师事务所</span>
      </div>
      <div class="system-label">
        <span class="status-dot" aria-hidden="true"></span>
        至高律所管理系统
      </div>
    </header>

    <main class="login-content">
      <section class="brand-message" aria-labelledby="firm-name">
        <p class="brand-eyebrow">GUANGDONG ZHIGAO LAW FIRM</p>
        <h1 id="firm-name" aria-label="广东至高律师事务所">
          <span
            v-for="(character, index) in firmNameCharacters"
            :key="`${character}-${index}`"
            class="heading-character"
            :style="{ '--character-delay': `${200 + index * 48}ms` }"
            aria-hidden="true"
          >{{ character }}</span>
        </h1>
        <p class="brand-subtitle">专业判断，始于清晰有序。</p>
        <p class="brand-description">案件、客户、审批与知识协同工作台</p>
      </section>

      <section class="login-panel liquid-glass" aria-labelledby="login-title">
        <div class="login-header">
          <p class="panel-eyebrow">ZGAI WORKSPACE</p>
          <h2 id="login-title">登录工作台</h2>
          <p>使用律所账号继续</p>
        </div>

        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model="loginForm.username"
              aria-label="用户名"
              placeholder="请输入用户名"
              prefix-icon="User"
              size="large"
              clearable
              autocomplete="username"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              aria-label="密码"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              size="large"
              show-password
              clearable
              autocomplete="current-password"
            />
          </el-form-item>

          <div class="login-options">
            <el-checkbox v-model="rememberMe">记住我</el-checkbox>
            <span>内部系统</span>
          </div>

          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-button"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form>

        <footer class="login-footer">© 2026 广东至高律师事务所</footer>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores'
import { ElMessage } from 'element-plus'

const backgroundVideo = 'https://d8j0ntlcm91z4.cloudfront.net/user_38xzZboKViGWJOttwIXH07lWA1P/hf_20260403_050628_c4e32401-fab4-4a27-b7a8-6e9291cd5959.mp4'
const firmNameCharacters = Array.from('广东至高律师事务所')

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginFormRef = ref(null)
const loading = ref(false)
const rememberMe = ref(false)
const videoReady = ref(false)
const videoUnavailable = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '用户名长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度在 6 到 100 个字符', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  try {
    await loginFormRef.value?.validate()
    loading.value = true

    await userStore.login(loginForm)

    ElMessage.success('登录成功')

    const redirect = userStore.requiresPasswordChange
      ? '/profile?passwordChange=required'
      : (route.query.redirect || '/')
    router.push(redirect)
  } catch (error) {
    if (error.message) {
      ElMessage.error(error.message)
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  position: relative;
  display: flex;
  min-height: 100vh;
  min-height: 100dvh;
  overflow: hidden;
  padding: 24px clamp(24px, 4.5vw, 72px) clamp(36px, 5vw, 64px);
  color: #fff;
  background: #101113;

  &.video-unavailable .background-video {
    display: none;
  }
}

.background-video {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center;
}

.liquid-glass {
  position: relative;
  overflow: hidden;
  background: rgba(9, 12, 16, 0.34);
  background-blend-mode: luminosity;
  border: 1px solid rgba(255, 255, 255, 0.24);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.16), 0 18px 50px rgba(0, 0, 0, 0.16);
  backdrop-filter: blur(12px) saturate(115%);
  -webkit-backdrop-filter: blur(12px) saturate(115%);
}

.brand-bar {
  position: absolute;
  z-index: 2;
  top: 24px;
  left: clamp(24px, 4.5vw, 72px);
  right: clamp(24px, 4.5vw, 72px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 56px;
  padding: 8px 10px 8px 12px;
  border-radius: 8px;
  opacity: 0;
  transform: translateY(-8px);
  transition: opacity 700ms ease, transform 700ms ease;

  &.visible {
    opacity: 1;
    transform: translateY(0);
  }
}

.brand-identity,
.system-label {
  display: flex;
  align-items: center;
}

.brand-identity {
  min-width: 0;
  gap: 12px;
}

.brand-mark {
  display: grid;
  flex: 0 0 36px;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 7px;
  background: #fff;
  color: #17191d;
  font-size: 17px;
  font-weight: 700;
}

.brand-name {
  overflow: hidden;
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
  text-overflow: ellipsis;
  text-shadow: 0 1px 8px rgba(0, 0, 0, 0.42);
}

.system-label {
  gap: 8px;
  padding: 8px 12px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.92);
  color: #25272b;
  font-size: 13px;
  font-weight: 600;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #2c8b57;
  box-shadow: 0 0 0 3px rgba(44, 139, 87, 0.14);
}

.login-content {
  position: relative;
  z-index: 1;
  display: grid;
  width: 100%;
  grid-template-columns: minmax(0, 1fr) minmax(340px, 400px);
  gap: clamp(40px, 7vw, 120px);
  align-items: end;
  margin-top: 104px;
}

.brand-message {
  align-self: end;
  max-width: 820px;
  padding-bottom: 4px;
  text-shadow: 0 2px 20px rgba(0, 0, 0, 0.36);
}

.brand-eyebrow,
.panel-eyebrow {
  margin: 0;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.12em;
}

.brand-eyebrow {
  margin-bottom: 14px;
  opacity: 0;
  animation: fade-in 800ms ease 500ms forwards;
}

.brand-message h1 {
  display: flex;
  flex-wrap: nowrap;
  margin: 0;
  font-size: clamp(40px, 4.7vw, 70px);
  line-height: 1.1;
  font-weight: 500;
}

.heading-character {
  display: inline-block;
  opacity: 0;
  transform: translateX(-18px);
  animation: character-in 500ms ease var(--character-delay) forwards;
}

.brand-subtitle {
  margin: 20px 0 0;
  font-size: clamp(18px, 2vw, 25px);
  font-weight: 400;
  opacity: 0;
  animation: fade-in 900ms ease 850ms forwards;
}

.brand-description {
  margin: 8px 0 0;
  color: rgba(255, 255, 255, 0.8);
  font-size: 15px;
  opacity: 0;
  animation: fade-in 900ms ease 1050ms forwards;
}

.login-panel {
  width: 100%;
  padding: 28px;
  border-radius: 8px;
  opacity: 0;
  transform: translateY(14px);
  animation: panel-in 800ms ease 650ms forwards;
}

.login-header {
  margin-bottom: 26px;

  h2 {
    margin: 7px 0 6px;
    color: #fff;
    font-size: 25px;
    font-weight: 600;
  }

  p:last-child {
    margin: 0;
    color: rgba(255, 255, 255, 0.7);
    font-size: 13px;
  }
}

.panel-eyebrow {
  color: rgba(255, 255, 255, 0.62);
}

.login-form {
  :deep(.el-form-item) {
    margin-bottom: 18px;
  }

  :deep(.el-input__wrapper) {
    min-height: 46px;
    padding: 1px 13px;
    background: rgba(255, 255, 255, 0.9);
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.26) inset;

    &.is-focus {
      box-shadow: 0 0 0 2px #fff inset;
    }
  }

  :deep(.el-input__inner) {
    color: #202226;
  }

  :deep(.el-input__inner::placeholder) {
    color: #797d84;
  }

  :deep(.el-input__prefix),
  :deep(.el-input__suffix) {
    color: #5f646c;
  }
}

.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 32px;
  margin: -4px 0 15px;
  color: rgba(255, 255, 255, 0.65);
  font-size: 12px;

  :deep(.el-checkbox) {
    color: rgba(255, 255, 255, 0.88);
  }

  :deep(.el-checkbox__label) {
    color: inherit;
  }
}

.login-button.el-button--primary {
  width: 100%;
  height: 46px;
  border-color: #fff;
  background: #fff;
  color: #17191d;
  font-size: 15px;
  font-weight: 600;

  &:hover,
  &:focus {
    border-color: #eceef0;
    background: #eceef0;
    color: #17191d;
  }
}

.login-footer {
  margin-top: 22px;
  color: rgba(255, 255, 255, 0.56);
  font-size: 11px;
  text-align: center;
}

@keyframes character-in {
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes fade-in {
  to { opacity: 1; }
}

@keyframes panel-in {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 860px) {
  .login-page {
    min-height: 100dvh;
    overflow-y: auto;
    padding: 20px 24px 28px;
  }

  .brand-bar {
    top: 20px;
    left: 24px;
    right: 24px;
  }

  .login-content {
    grid-template-columns: 1fr;
    gap: 28px;
    align-content: end;
    margin-top: 92px;
  }

  .brand-message {
    padding-top: 52px;
  }

  .brand-message h1 {
    font-size: clamp(38px, 9vw, 58px);
  }

  .login-panel {
    max-width: 460px;
    justify-self: end;
  }
}

@media (max-width: 520px) {
  .login-page {
    padding: 14px 14px 18px;
  }

  .brand-bar {
    top: 14px;
    left: 14px;
    right: 14px;
  }

  .brand-name {
    max-width: 170px;
    font-size: 14px;
  }

  .system-label {
    display: none;
  }

  .login-content {
    gap: 20px;
    margin-top: 80px;
  }

  .brand-message {
    padding-top: 24px;
  }

  .brand-eyebrow {
    margin-bottom: 9px;
    font-size: 10px;
  }

  .brand-message h1 {
    max-width: 360px;
    font-size: clamp(32px, 8.8vw, 38px);
  }

  .brand-subtitle {
    margin-top: 12px;
    font-size: 17px;
  }

  .brand-description {
    margin-top: 5px;
    font-size: 12px;
  }

  .login-panel {
    padding: 22px 20px;
  }

  .login-header {
    margin-bottom: 20px;

    h2 {
      font-size: 22px;
    }
  }

  .login-footer {
    margin-top: 17px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .brand-bar,
  .brand-eyebrow,
  .heading-character,
  .brand-subtitle,
  .brand-description,
  .login-panel {
    opacity: 1;
    transform: none;
    animation: none;
    transition: none;
  }
}
</style>
