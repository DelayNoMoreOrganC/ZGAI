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
            <el-option label="知识文档" value="DOCUMENT" />
            <el-option label="公共模板" value="TEMPLATE" />
            <el-option label="办案指南" value="GUIDE" />
            <el-option label="经验参考" value="EXPERIENCE" />
          </el-select>
        </el-form-item>

        <el-form-item label="知识来源" prop="knowledgeSource">
          <el-select v-model="form.knowledgeSource" placeholder="请选择知识来源">
            <el-option label="法律法规" value="LAW_REGULATION" />
            <el-option label="律所制度" value="FIRM_POLICY" />
            <el-option label="公共模板" value="PUBLIC_TEMPLATE" />
            <el-option label="参考资料" value="REFERENCE_MATERIAL" />
            <el-option label="全所知识" value="FIRM_KNOWLEDGE" />
          </el-select>
          <div class="form-tip">
            第一阶段 AI 知识库只索引法律法规、律所制度、公共模板和参考资料；案件沉淀需人工审核后再开放。
          </div>
        </el-form-item>

        <el-form-item label="分类">
          <el-input v-model="form.category" placeholder="例如：合同、劳动、侵权" />
        </el-form-item>

        <template v-if="form.knowledgeSource === 'LAW_REGULATION'">
          <el-form-item label="发布机关">
            <el-input v-model="form.issuingAuthority" maxlength="200" placeholder="例如：全国人民代表大会常务委员会" />
          </el-form-item>
          <el-form-item label="文号">
            <el-input v-model="form.documentNumber" maxlength="100" placeholder="请输入法规文号（如有）" />
          </el-form-item>
          <el-form-item label="生效日期">
            <el-date-picker
              v-model="form.effectiveDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择生效日期"
            />
          </el-form-item>
          <el-form-item label="有效状态">
            <el-select v-model="form.validityStatus">
              <el-option label="现行有效" value="EFFECTIVE" />
              <el-option label="已修订" value="AMENDED" />
              <el-option label="已废止" value="REPEALED" />
              <el-option label="待核验" value="UNKNOWN" />
            </el-select>
          </el-form-item>
        </template>

        <el-form-item label="来源依据">
          <el-input
            v-model="form.sourceReference"
            maxlength="500"
            placeholder="官方网址、内部制度编号或经授权来源说明"
          />
        </el-form-item>

        <el-form-item v-if="form.knowledgeSource === 'REFERENCE_MATERIAL'" label="授权确认">
          <el-checkbox v-model="form.authorizationConfirmed">已确认具备律所内部使用授权</el-checkbox>
          <div class="form-tip">未确认授权的外部参考资料可以保存，但不会进入 AI 检索。</div>
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
            placeholder="请输入法规正文、制度内容、模板文本或参考资料"
          />
        </el-form-item>

        <el-form-item label="选项">
          <el-checkbox v-model="form.isTop">置顶显示</el-checkbox>
          <el-checkbox v-model="form.isPublic">全所可见</el-checkbox>
          <el-checkbox
            v-model="form.knowledgeEligible"
            :disabled="!form.isPublic || (form.knowledgeSource === 'REFERENCE_MATERIAL' && !form.authorizationConfirmed)"
          >
            允许进入 AI 知识库
          </el-checkbox>
          <div class="form-tip">关闭“全所可见”后，文章仅本人和管理员可见，且不会进入 AI 检索。</div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">
            {{ isEdit ? '保存修改' : '发布文章' }}
          </el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
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
  articleType: 'DOCUMENT',
  knowledgeSource: 'LAW_REGULATION',
  category: '',
  tags: '',
  summary: '',
  content: '',
  sourceReference: '',
  issuingAuthority: '',
  documentNumber: '',
  effectiveDate: '',
  validityStatus: 'UNKNOWN',
  authorizationConfirmed: false,
  isTop: false,
  isPublic: true,
  knowledgeEligible: true
})

const rules = {
  title: [
    { required: true, message: '请输入文章标题', trigger: 'blur' }
  ],
  articleType: [
    { required: true, message: '请选择文章类型', trigger: 'change' }
  ],
  knowledgeSource: [
    { required: true, message: '请选择知识来源', trigger: 'change' }
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
      knowledgeSource: data.knowledgeSource || inferKnowledgeSource(data.articleType),
      category: data.category,
      tags: data.tags,
      summary: data.summary,
      content: data.content,
      sourceReference: data.sourceReference || '',
      issuingAuthority: data.issuingAuthority || '',
      documentNumber: data.documentNumber || '',
      effectiveDate: normalizeDate(data.effectiveDate),
      validityStatus: data.validityStatus || 'UNKNOWN',
      authorizationConfirmed: data.authorizationConfirmed === true,
      isTop: data.isTop,
      isPublic: data.isPublic,
      knowledgeEligible: data.knowledgeEligible !== false
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
      data: {
        ...form.value,
        knowledgeEligible: form.value.isPublic && form.value.knowledgeEligible
      }
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

const inferKnowledgeSource = (articleType) => {
  if (articleType === 'TEMPLATE') return 'PUBLIC_TEMPLATE'
  return 'FIRM_KNOWLEDGE'
}

const normalizeDate = (date) => {
  if (!date) return ''
  if (Array.isArray(date)) {
    const [year, month, day] = date
    return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
  }
  return String(date).substring(0, 10)
}

onMounted(() => {
  if (isEdit.value) {
    loadArticle()
  }
})

watch(
  () => form.value.articleType,
  (articleType) => {
    if (!isEdit.value && articleType === 'TEMPLATE') {
      form.value.knowledgeSource = 'PUBLIC_TEMPLATE'
      form.value.knowledgeEligible = true
    }
  }
)

watch(
  () => form.value.isPublic,
  (isPublic) => {
    if (!isPublic) form.value.knowledgeEligible = false
  }
)

watch(
  () => [form.value.knowledgeSource, form.value.authorizationConfirmed],
  ([source, authorizationConfirmed]) => {
    if (source === 'REFERENCE_MATERIAL' && !authorizationConfirmed) {
      form.value.knowledgeEligible = false
    }
  }
)
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
