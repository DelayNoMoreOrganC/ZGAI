<template>
  <div class="search-bar">
    <el-form :model="formData" :inline="true" @submit.prevent="handleSearch">
      <el-form-item
        v-for="field in fields"
        :key="field.prop"
        :label="field.label"
      >
        <el-input
          v-if="field.type === 'input'"
          v-model="formData[field.prop]"
          :placeholder="`请输入${field.label}`"
          clearable
        />
        <el-select
          v-if="field.type === 'select'"
          v-model="formData[field.prop]"
          :placeholder="`请选择${field.label}`"
          clearable
        >
          <el-option
            v-for="option in field.options"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </el-select>
        <el-date-picker
          v-if="field.type === 'date'"
          v-model="formData[field.prop]"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          clearable
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :icon="Search" @click="handleSearch">
          搜索
        </el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        <slot></slot>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'

const props = defineProps({
  fields: {
    type: Array,
    required: true
  }
})

const emit = defineEmits(['search', 'reset'])

const formData = ref({})

// 初始化表单数据
props.fields.forEach(field => {
  formData.value[field.prop] = field.defaultValue || ''
})

// 监听表单数据变化
watch(formData, (newVal) => {
  emit('search', newVal)
}, { deep: true })

const handleSearch = () => {
  emit('search', formData.value)
}

const handleReset = () => {
  props.fields.forEach(field => {
    formData.value[field.prop] = field.defaultValue || ''
  })
  emit('reset')
}
</script>

<style scoped lang="scss">
.search-bar {
  margin-bottom: 20px;
  padding: 20px;
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}
</style>
