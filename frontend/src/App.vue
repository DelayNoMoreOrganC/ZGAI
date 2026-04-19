<template>
  <router-view v-slot="{ Component, route }">
    <transition :name="transitionName" mode="out-in" @before-enter="handleBeforeEnter" @after-enter="handleAfterEnter">
      <keep-alive :include="keepAliveComponents">
        <component :is="Component" :key="route.path" />
      </keep-alive>
    </transition>
  </router-view>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

// 过渡动画名称
const transitionName = ref('fade-transform')

// 需要缓存的路由组件
const keepAliveComponents = ref([
  'Dashboard',
  'CaseList',
  'Calendar',
  'Client',
  'Document',
  'Finance'
])

// 监听路由变化，根据路由类型决定动画效果
watch(() => route.path, (newPath, oldPath) => {
  if (!oldPath) return

  // 根据路由层级决定动画方向
  const toDepth = newPath.split('/').length
  const fromDepth = oldPath.split('/').length

  if (toDepth > fromDepth) {
    // 进入下一级
    transitionName.value = 'slide-left'
  } else if (toDepth < fromDepth) {
    // 返回上一级
    transitionName.value = 'slide-right'
  } else {
    // 平级切换
    transitionName.value = 'fade-transform'
  }
})

// 进入前回调
const handleBeforeEnter = () => {
  // 可以在这里添加页面切换前的逻辑
  document.body.style.overflow = 'hidden'
}

// 进入后回调
const handleAfterEnter = () => {
  // 可以在这里添加页面切换后的逻辑
  document.body.style.overflow = ''
}
</script>

<style>
/* 全局重置样式 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial,
    'Noto Sans', sans-serif, 'Apple Color Emoji', 'Segoe UI Emoji', 'Segoe UI Symbol',
    'Noto Color Emoji';
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

#app {
  width: 100%;
  height: 100vh;
  overflow: hidden;
}

/* 淡入淡出 + 平移动画 */
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.3s cubic-bezier(0.55, 0, 0.1, 1);
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(30px) scale(0.98);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(-30px) scale(0.98);
}

/* 向左滑动动画（进入下一级） */
.slide-left-enter-active,
.slide-left-leave-active {
  transition: all 0.3s cubic-bezier(0.55, 0, 0.1, 1);
}

.slide-left-enter-from {
  opacity: 0;
  transform: translateX(100%);
}

.slide-left-leave-to {
  opacity: 0;
  transform: translateX(-30%);
}

/* 向右滑动动画（返回上一级） */
.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 0.3s cubic-bezier(0.55, 0, 0.1, 1);
}

.slide-right-enter-from {
  opacity: 0;
  transform: translateX(-30%);
}

.slide-right-leave-to {
  opacity: 0;
  transform: translateX(100%);
}

/* 淡入淡出动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 缩放淡入动画 */
.zoom-fade-enter-active,
.zoom-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.55, 0, 0.1, 1);
}

.zoom-fade-enter-from {
  opacity: 0;
  transform: scale(0.95);
}

.zoom-fade-leave-to {
  opacity: 0;
  transform: scale(1.05);
}

/* 页面加载动画 */
@keyframes page-loading {
  0% {
    transform: scale(0.9);
    opacity: 0;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.page-loading {
  animation: page-loading 0.3s ease-out;
}
</style>
