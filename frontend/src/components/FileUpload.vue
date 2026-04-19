<template>
  <div class="file-upload">
    <el-upload
      v-model:file-list="fileList"
      :action="uploadUrl"
      :headers="uploadHeaders"
      :on-success="handleSuccess"
      :on-error="handleError"
      :on-progress="handleProgress"
      :before-upload="beforeUpload"
      :on-remove="handleRemove"
      :limit="limit"
      :accept="accept"
      :multiple="multiple"
      :drag="drag"
      list-type="picture-card"
    >
      <el-icon v-if="drag"><Plus /></el-icon>
      <el-button v-else type="primary">选择文件</el-button>
      <template #tip>
        <div class="el-upload__tip">
          {{ tip }}
        </div>
      </template>
    </el-upload>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: {
    type: Array,
    default: () => []
  },
  accept: {
    type: String,
    default: '*'
  },
  limit: {
    type: Number,
    default: 10
  },
  maxSize: {
    type: Number,
    default: 50
  },
  multiple: {
    type: Boolean,
    default: true
  },
  drag: {
    type: Boolean,
    default: false
  },
  tip: {
    type: String,
    default: '支持上传图片、文档等文件'
  }
})

const emit = defineEmits(['update:modelValue', 'success', 'error', 'progress', 'remove'])

const fileList = ref(props.modelValue)

const uploadUrl = computed(() => import.meta.env.VITE_APP_UPLOAD_URL)

const uploadHeaders = computed(() => ({
  'Authorization': `Bearer ${localStorage.getItem('token')}`
}))

const beforeUpload = (file) => {
  const isLtMaxSize = file.size / 1024 / 1024 < props.maxSize
  if (!isLtMaxSize) {
    ElMessage.error(`文件大小不能超过 ${props.maxSize}MB`)
    return false
  }
  return true
}

const handleSuccess = (response, file, fileList) => {
  ElMessage.success('上传成功')
  emit('success', response, file, fileList)
  emit('update:modelValue', fileList)
}

const handleError = (error, file, fileList) => {
  ElMessage.error('上传失败')
  emit('error', error, file, fileList)
}

const handleProgress = (event, file, fileList) => {
  emit('progress', event, file, fileList)
}

const handleRemove = (file, fileList) => {
  emit('remove', file, fileList)
  emit('update:modelValue', fileList)
}
</script>

<style scoped lang="scss">
.file-upload {
  :deep(.el-upload-list__item) {
    transition: all 0.3s;
  }
}
</style>
