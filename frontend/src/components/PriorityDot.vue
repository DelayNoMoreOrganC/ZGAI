<template>
  <div class="priority-dot" :class="priorityClass">
    <span class="dot"></span>
    <span class="label">{{ label }}</span>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  priority: {
    type: String,
    required: true
  }
})

const priorityMap = {
  high: { label: '紧急', class: 'high' },
  medium: { label: '重要', class: 'medium' },
  low: { label: '普通', class: 'low' },
  重要: { label: '重要', class: 'medium' },
  一般: { label: '一般', class: 'low' },
  次要: { label: '次要', class: 'low' }
}

const label = computed(() => priorityMap[props.priority]?.label || props.priority)

const priorityClass = computed(() => priorityMap[props.priority]?.class || 'low')
</script>

<style scoped lang="scss">
.priority-dot {
  display: flex;
  align-items: center;
  gap: 6px;

  .dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
  }

  &.high .dot {
    background-color: #f56c6c;
  }

  &.medium .dot {
    background-color: #e6a23c;
  }

  &.low .dot {
    background-color: #909399;
  }

  .label {
    font-size: 14px;
  }
}
</style>
