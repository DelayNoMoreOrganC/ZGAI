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
  padding: 0 0 16px;
  background: transparent;
  border-bottom: 1px solid #e7e9ed;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
    min-width: 0;

    h2 {
      margin: 0;
      font-size: 20px;
      font-weight: 600;
      color: #1f2329;
      line-height: 1.3;
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    justify-content: flex-end;
    flex-wrap: wrap;
    gap: 10px;
    min-width: 0;
  }
}

@media (max-width: 760px) {
  .page-header {
    align-items: stretch;
    flex-direction: column;
    gap: 12px;
    padding-bottom: 14px;

    .header-left h2 {
      font-size: 18px;
    }

    .header-right {
      justify-content: flex-start;
      width: 100%;
    }
  }
}
</style>
