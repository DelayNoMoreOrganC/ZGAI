<template>
  <div class="rich-editor">
    <QuillEditor
      v-model:content="content"
      :options="editorOptions"
      content-type="html"
      @update:content="handleUpdate"
    />
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  placeholder: {
    type: String,
    default: '请输入内容...'
  },
  readOnly: {
    type: Boolean,
    default: false
  },
  minHeight: {
    type: String,
    default: '200px'
  }
})

const emit = defineEmits(['update:modelValue'])

const content = ref(props.modelValue)

const editorOptions = computed(() => ({
  theme: 'snow',
  placeholder: props.placeholder,
  readOnly: props.readOnly,
  modules: {
    toolbar: [
      ['bold', 'italic', 'underline', 'strike'],
      ['blockquote', 'code-block'],
      [{ 'header': 1 }, { 'header': 2 }],
      [{ 'list': 'ordered'}, { 'list': 'bullet' }],
      [{ 'indent': '-1'}, { 'indent': '+1' }],
      [{ 'size': ['small', false, 'large', 'huge'] }],
      [{ 'header': [1, 2, 3, 4, 5, 6, false] }],
      [{ 'color': [] }, { 'background': [] }],
      [{ 'align': [] }],
      ['link', 'image'],
      ['clean']
    ]
  }
}))

watch(() => props.modelValue, (newVal) => {
  content.value = newVal
})

const handleUpdate = (value) => {
  emit('update:modelValue', value)
}
</script>

<style scoped lang="scss">
.rich-editor {
  :deep(.ql-editor) {
    min-height: v-bind(minHeight);
  }

  :deep(.ql-container) {
    border-radius: 0 0 4px 4px;
  }

  :deep(.ql-toolbar) {
    border-radius: 4px 4px 0 0;
  }
}
</style>
