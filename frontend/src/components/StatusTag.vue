<template>
  <el-tag :type="tagType" :size="size" :effect="effect">
    {{ label }}
  </el-tag>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  status: {
    type: [String, Number],
    required: true
  },
  statusMap: {
    type: Object,
    required: true
  },
  size: {
    type: String,
    default: 'default'
  },
  effect: {
    type: String,
    default: 'light'
  }
})

const label = computed(() => props.statusMap[props.status]?.label || props.status)

const tagType = computed(() => props.statusMap[props.status]?.type || 'info')
</script>
