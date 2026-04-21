<template>
  <div class="page-header">
    <div class="header-left">
      <el-button v-if="showBack" :icon="ArrowLeft" circle @click="handleBack" />
      <h2>{{ title }}</h2>
      <el-tag v-if="subtitle" type="info" size="small">{{ subtitle }}</el-tag>
    </div>
    <div class="header-right">
      <slot name="extra">
        <!-- Fallback to default slot if no extra content -->
        <slot></slot>
      </slot>
    </div>
  </div>
</template>

<script setup>
import { ArrowLeft } from '@element-plus/icons-vue'

defineProps({
  title: {
    type: String,
    required: true
  },
  subtitle: {
    type: String,
    default: ''
  },
  showBack: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['back'])

const handleBack = () => {
  emit('back')
}
</script>

<style scoped lang="scss">
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px 20px;
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;

    h2 {
      margin: 0;
      font-size: 18px;
      font-weight: 500;
      color: #333;
    }
  }

  .header-right {
    display: flex;
    gap: 10px;
  }
}
</style>
