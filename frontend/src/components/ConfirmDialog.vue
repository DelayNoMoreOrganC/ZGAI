<template>
  <el-dialog
    v-model="dialogVisible"
    :title="title"
    :width="width"
    :before-close="handleClose"
  >
    <div class="confirm-content">
      <el-icon :size="40" :color="iconColor">
        <Warning v-if="type === 'warning'" />
        <InfoFilled v-else-if="type === 'info'" />
        <SuccessFilled v-else-if="type === 'success'" />
        <CircleCloseFilled v-else />
      </el-icon>
      <p>{{ message }}</p>
    </div>
    <template #footer>
      <el-button @click="handleCancel">{{ cancelText }}</el-button>
      <el-button :type="confirmType" @click="handleConfirm">
        {{ confirmText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import {
  Warning,
  InfoFilled,
  SuccessFilled,
  CircleCloseFilled
} from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '提示'
  },
  message: {
    type: String,
    required: true
  },
  type: {
    type: String,
    default: 'warning'
  },
  confirmText: {
    type: String,
    default: '确定'
  },
  cancelText: {
    type: String,
    default: '取消'
  },
  width: {
    type: String,
    default: '420px'
  }
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

const dialogVisible = ref(props.modelValue)

watch(() => props.modelValue, (newVal) => {
  dialogVisible.value = newVal
})

const iconColor = computed(() => {
  const colorMap = {
    warning: '#e6a23c',
    info: '#909399',
    success: '#67c23a',
    error: '#f56c6c'
  }
  return colorMap[props.type]
})

const confirmType = computed(() => {
  const typeMap = {
    warning: 'warning',
    info: 'info',
    success: 'success',
    error: 'danger'
  }
  return typeMap[props.type]
})

const handleClose = () => {
  dialogVisible.value = false
  emit('update:modelValue', false)
}

const handleConfirm = () => {
  emit('confirm')
  handleClose()
}

const handleCancel = () => {
  emit('cancel')
  handleClose()
}
</script>

<style scoped lang="scss">
.confirm-content {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 0;

  p {
    margin: 0;
    font-size: 16px;
    color: #333;
  }
}
</style>
