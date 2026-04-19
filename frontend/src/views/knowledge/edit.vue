<template>
  <div class="knowledge-edit">
    <PageHeader :title="isEdit ? '编辑文章' : '新建文章'" />

    <div class="edit-container">
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        class="article-form"
      >
        <el-form-item label="文章标题" prop="title">
          <el-input
            v-model="form.title"
            placeholder="请输入文章标题"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="文章类型" prop="articleType">
          <el-select v-model="form.articleType" placeholder="请选择文章类型">
            <el-option label="文档模板" value="TEMPLATE" />
            <el-option label="类案检索" value="CASE" />
            <el-option label="办案指南" value="GUIDE" />
            <el-option label="经验分享" value="EXPERIENCE" />
          </el-select>
        </el-form-item>

        <el-form-item label="分类">
          <el-input v-model="form.category" placeholder="例如：合同、劳动、侵权" />
        </el-form-item>

        <el-form-item label="标签">
          <el-input
            v-model="form.tags"
            placeholder="多个标签用逗号分隔，例如：合同,违约金,赔偿"
          />
        </el-form-item>

        <el-form-item label="摘要">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="3"
            placeholder="简短描述文章内容（可选）"
            maxlength="1000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="文章内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="15"
            placeholder="支持富文本HTML格式"
          />
          <div class="form-tip">
            提示：可以使用HTML标签进行格式化，例如 &lt;h2&gt;标题&lt;/h2&gt;、&lt;p&gt;段落&lt;/p&gt;
          </div>
        </el-form-item>

        <el-form-item label="选项">
          <el-checkbox v-model="form.isTop">置顶显示</el-checkbox>
          <el-checkbox v-model="form.isPublic">公开（全所可见）</el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ isEdit ? '保存修改' : '发布文章' }}
          </el-button>
          <el-button @click="handleCancel">取消</el-button>
          <el-button @click="handlePreview" v-if="!isEdit">预览</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import request from '@/utils/request'

const router = useRouter()
const route = useRoute()

const formRef = ref(null)
const submitting = ref(false)

const isEdit = computed(() => !!route.params.id)

const form = ref({
  title: '',
  articleType: '',
  category: '',
  tags: '',
  summary: '',
  content: '',
  isTop: false,
  isPublic: true
})

const rules = {
  title: [
    { required: true, message: '请输入文章标题', trigger: 'blur' }
  ],
  articleType: [
    { required: true, message: '请选择文章类型', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入文章内容', trigger: 'blur' }
  ]
}

// 加载文章详情（编辑模式）
const loadArticle = async () => {
  try {
    const { data } = await request({
      url: `/knowledge/${route.params.id}`,
      method: 'get'
    })
    form.value = {
      title: data.title,
      articleType: data.articleType,
      category: data.category,
      tags: data.tags,
      summary: data.summary,
      content: data.content,
      isTop: data.isTop,
      isPublic: data.isPublic
    }
  } catch (error) {
    console.error('加载文章失败:', error)
    ElMessage.error('加载文章失败')
    router.back()
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true

  try {
    const isEdit = !!route.params.id
    const url = isEdit ? `/knowledge/${route.params.id}` : '/knowledge'
    const method = isEdit ? 'put' : 'post'

    await request({
      url,
      method,
      data: form.value
    })

    ElMessage.success(isEdit ? '保存成功' : '发布成功')
    router.push('/knowledge/list')
  } catch (error) {
    console.error('保存失败:', error)
    ElMessage.error('保存失败，请重试')
  } finally {
    submitting.value = false
  }
}

// 取消
const handleCancel = () => {
  ElMessageBox.confirm('确定要取消吗？未保存的内容将丢失。', '提示', {
    type: 'warning'
  }).then(() => {
    router.back()
  }).catch(() => {})
}

// 预览
const handlePreview = () => {
  const previewData = JSON.stringify(form.value)
  sessionStorage.setItem('knowledge_preview', previewData)
  window.open('/knowledge/preview', '_blank')
}

onMounted(() => {
  if (isEdit.value) {
    loadArticle()
  }
})
</script>

<style scoped lang="scss">
.knowledge-edit {
  .edit-container {
    max-width: 900px;
    margin: 20px auto;
    background: #fff;
    padding: 30px;
    border-radius: 4px;

    .article-form {
      .form-tip {
        margin-top: 8px;
        font-size: 12px;
        color: #999;
      }
    }
  }
}
</style>
