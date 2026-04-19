<template>
  <el-drawer
    v-model="drawerVisible"
    :title="title"
    :size="size"
    :before-close="handleClose"
    destroy-on-close
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      :label-width="labelWidth"
      :label-position="labelPosition"
    >
      <el-form-item
        v-for="field in fields"
        :key="field.prop"
        :label="field.label"
        :prop="field.prop"
      >
        <el-input
          v-if="field.type === 'input'"
          v-model="formData[field.prop]"
          :type="field.inputType || 'text'"
          :placeholder="field.placeholder || `请输入${field.label}`"
          :disabled="field.disabled"
          :readonly="field.readonly"
          clearable
        />
        <el-input
          v-if="field.type === 'textarea'"
          v-model="formData[field.prop]"
          type="textarea"
          :rows="field.rows || 4"
          :placeholder="field.placeholder || `请输入${field.label}`"
          :disabled="field.disabled"
          clearable
        />
        <el-input-number
          v-if="field.type === 'number'"
          v-model="formData[field.prop]"
          :min="field.min"
          :max="field.max"
          :step="field.step || 1"
          :disabled="field.disabled"
        />
        <el-select
          v-if="field.type === 'select'"
          v-model="formData[field.prop]"
          :placeholder="field.placeholder || `请选择${field.label}`"
          :disabled="field.disabled"
          :multiple="field.multiple"
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
          type="date"
          :placeholder="field.placeholder || `请选择${field.label}`"
          :disabled="field.disabled"
          clearable
        />
        <el-date-picker
          v-if="field.type === 'daterange'"
          v-model="formData[field.prop]"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :disabled="field.disabled"
          clearable
        />
        <el-radio-group
          v-if="field.type === 'radio'"
          v-model="formData[field.prop]"
          :disabled="field.disabled"
        >
          <el-radio
            v-for="option in field.options"
            :key="option.value"
            :label="option.value"
          >
            {{ option.label }}
          </el-radio>
        </el-radio-group>
        <el-checkbox-group
          v-if="field.type === 'checkbox'"
          v-model="formData[field.prop]"
          :disabled="field.disabled"
        >
          <el-checkbox
            v-for="option in field.options"
            :key="option.value"
            :label="option.value"
          >
            {{ option.label }}
          </el-checkbox>
        </el-checkbox-group>
        <el-switch
          v-if="field.type === 'switch'"
          v-model="formData[field.prop]"
          :disabled="field.disabled"
        />
        <slot
          v-if="field.type === 'slot'"
          :name="field.prop"
          :field="field"
          :value="formData[field.prop]"
        ></slot>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="drawer-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">
          {{ submitText }}
        </el-button>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  title: {
    type: String,
    default: '表单'
  },
  fields: {
    type: Array,
    required: true
  },
  data: {
    type: Object,
    default: () => ({})
  },
  rules: {
    type: Object,
    default: () => ({})
  },
  size: {
    type: String,
    default: '600px'
  },
  labelWidth: {
    type: String,
    default: '100px'
  },
  labelPosition: {
    type: String,
    default: 'right'
  },
  submitText: {
    type: String,
    default: '确定'
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'submit', 'close'])

const drawerVisible = ref(props.modelValue)
const formRef = ref(null)
const formData = ref({})

// 初始化表单数据
const initFormData = () => {
  formData.value = {}
  props.fields.forEach(field => {
    if (field.type === 'checkbox' || field.multiple) {
      formData.value[field.prop] = props.data[field.prop] || field.defaultValue || []
    } else {
      formData.value[field.prop] = props.data[field.prop] ?? field.defaultValue ?? ''
    }
  })
}

watch(() => props.modelValue, (newVal) => {
  drawerVisible.value = newVal
  if (newVal) {
    initFormData()
  }
})

watch(() => props.data, () => {
  initFormData()
}, { deep: true })

const handleClose = () => {
  drawerVisible.value = false
  emit('update:modelValue', false)
  emit('close')
  formRef.value?.resetFields()
}

const handleSubmit = async () => {
  try {
    await formRef.value?.validate()
    emit('submit', formData.value)
  } catch (error) {
    ElMessage.error('请检查表单数据')
  }
}
</script>

<style scoped lang="scss">
.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
