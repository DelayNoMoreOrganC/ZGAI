<template>
  <section class="letter-page" v-loading="loading">
    <header class="page-toolbar">
      <div>
        <h3>律所所函</h3>
        <p>根据案件信息生成统一格式所函，确认后提交行政用印审批。</p>
      </div>
      <el-button
        v-if="canCreate"
        type="primary"
        :loading="creating"
        @click="createDraft"
      >
        <el-icon><Plus /></el-icon>
        {{ letters.length ? '新建另一份所函' : '新建所函' }}
      </el-button>
    </header>

    <el-alert
      v-if="caseData.status !== 'ACTIVE'"
      type="info"
      :closable="false"
      title="所函只可在立案审批通过且案件办理中的状态创建。"
    />

    <el-empty v-if="!loading && !letters.length" description="暂无律所所函" />

    <template v-else-if="selected">
      <div class="letter-switcher">
        <button
          v-for="item in letters"
          :key="item.id"
          type="button"
          :class="{ active: item.id === selected.id }"
          @click="selectLetter(item)"
        >
          <span>{{ item.displayNumber }}</span>
          <el-tag size="small" :type="statusType(item.status)">{{ item.statusDesc }}</el-tag>
        </button>
      </div>

      <el-alert
        v-if="selected.rejectedReason"
        type="warning"
        :closable="false"
        :title="`用印审批驳回：${selected.rejectedReason}`"
      />

      <div class="editor-grid">
        <div class="letter-editor">
          <div class="section-heading">
            <h4>所函内容</h4>
            <span>{{ selected.editable ? '保存后可下载草稿或提交用印' : '内容已锁定' }}</span>
          </div>
          <el-form label-position="top" :disabled="!selected.editable">
            <el-form-item label="收函单位" required>
              <el-input v-model="form.recipient" maxlength="300" />
            </el-form-item>
            <el-form-item label="委托客户" required>
              <el-input v-model="form.clientName" maxlength="500" />
            </el-form-item>
            <el-form-item label="办案人" required>
              <el-input v-model="form.lawyerNames" maxlength="1000" />
            </el-form-item>
            <el-form-item label="相对方" required>
              <el-input v-model="form.opposingParty" maxlength="1000" />
            </el-form-item>
            <el-form-item label="案由" required>
              <el-input v-model="form.caseReason" maxlength="500" />
            </el-form-item>
            <div class="form-row">
              <el-form-item label="函种" required>
                <el-select v-model="form.letterTypeCode">
                  <el-option v-for="item in typeOptions" :key="item" :label="`${item}函`" :value="item" />
                </el-select>
              </el-form-item>
              <el-form-item label="落款日期" required>
                <el-date-picker v-model="form.issueDate" type="date" value-format="YYYY-MM-DD" />
              </el-form-item>
            </div>
            <el-form-item label="办案人联系电话" required>
              <el-input v-model="form.lawyerContacts" maxlength="1000" />
            </el-form-item>
            <el-form-item label="结尾用语" required>
              <el-input v-model="form.closingText" maxlength="100" />
            </el-form-item>
          </el-form>
          <div class="editor-actions">
            <el-button v-if="selected.editable" type="danger" plain :loading="cancelling" @click="cancelDraft">
              <el-icon><Delete /></el-icon>
              取消草稿
            </el-button>
            <el-button v-if="selected.editable" :loading="saving" @click="saveDraft">保存修改</el-button>
            <el-button @click="downloadDocx">
              <el-icon><Download /></el-icon>
              {{ selected.status === 'APPROVED' ? '下载正式所函' : '下载草稿' }}
            </el-button>
            <el-button
              v-if="selected.editable"
              type="primary"
              :loading="submitting"
              @click="submitForSeal"
            >
              <el-icon><Stamp /></el-icon>
              申请用印
            </el-button>
          </div>
        </div>

        <div class="preview-shell">
          <div class="section-heading">
            <h4>版式预览</h4>
            <span>正式文件全部使用黑色字体</span>
          </div>
          <article class="a4-preview">
            <h1>广东至高律师事务所函</h1>
            <p class="letter-number">{{ previewNumber }}</p>
            <p class="recipient"><u>{{ form.recipient || '收函单位' }}</u>：</p>
            <p class="body-copy">
              我所接受<u>{{ form.clientName || '委托客户' }}</u>的委托，指派<u>{{ form.lawyerNames || '办案人' }}</u>担任其与<u>{{ form.opposingParty || '相对方' }}</u>{{ form.caseReason || '相关纠纷' }}一案的代理人。
            </p>
            <p class="closing">{{ form.closingText || '特此函告！' }}</p>
            <div class="signature">
              <strong>广东至高律师事务所</strong>
              <strong>{{ chineseDate(form.issueDate) }}</strong>
            </div>
            <footer>
              <p>联系电话：0757—83283000　传真：0757—83905969</p>
              <p>代理律师电话：{{ form.lawyerContacts || '-' }}　邮编：528000</p>
              <p>地址：佛山市禅城区岭南大道北100号岭南大厦16层</p>
            </footer>
          </article>
        </div>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Download, Plus, Stamp } from '@element-plus/icons-vue'
import {
  cancelLawFirmLetter,
  createLawFirmLetter,
  downloadLawFirmLetter,
  listLawFirmLetters,
  submitLawFirmLetter,
  updateLawFirmLetter
} from '@/api/lawFirmLetter'

const props = defineProps({ caseData: { type: Object, required: true } })
const emit = defineEmits(['refresh'])
const loading = ref(false)
const creating = ref(false)
const saving = ref(false)
const submitting = ref(false)
const cancelling = ref(false)
const letters = ref([])
const selected = ref(null)
const typeOptions = ['民', '刑', '行', '仲', '非', '顾', '案']
const form = reactive({})

const caseData = computed(() => props.caseData)
const canCreate = computed(() => props.caseData.canEdit === true && props.caseData.status === 'ACTIVE')
const previewNumber = computed(() => selected.value?.letterNumber || `(${yearOf(form.issueDate)})粤至高${form.letterTypeCode || '案'}函字第【待编号】号`)

const loadLetters = async (selectId) => {
  loading.value = true
  try {
    const response = await listLawFirmLetters(props.caseData.id)
    letters.value = response.data || []
    const target = letters.value.find(item => item.id === selectId) || letters.value[0] || null
    selectLetter(target)
  } catch (error) {
    ElMessage.error(error?.message || '律所所函加载失败')
  } finally {
    loading.value = false
  }
}

const selectLetter = item => {
  selected.value = item
  Object.keys(form).forEach(key => delete form[key])
  if (item) Object.assign(form, {
    recipient: item.recipient,
    clientName: item.clientName,
    lawyerNames: item.lawyerNames,
    opposingParty: item.opposingParty,
    caseReason: item.caseReason,
    letterTypeCode: item.letterTypeCode,
    lawyerContacts: item.lawyerContacts,
    closingText: item.closingText,
    issueDate: item.issueDate,
    lockVersion: item.lockVersion
  })
}

const createDraft = async () => {
  creating.value = true
  try {
    const response = await createLawFirmLetter(props.caseData.id)
    ElMessage.success('已根据案件信息生成所函草稿')
    await loadLetters(response.data.id)
  } catch (error) {
    ElMessage.error(error?.message || '所函草稿生成失败')
  } finally {
    creating.value = false
  }
}

const cancelDraft = async () => {
  try {
    await ElMessageBox.confirm('取消后该草稿将从所函列表移除，案件日志会保留本次操作。确认取消？', '取消所函草稿', {
      confirmButtonText: '确认取消', cancelButtonText: '返回编辑', type: 'warning'
    })
    cancelling.value = true
    await cancelLawFirmLetter(selected.value.id)
    ElMessage.success('所函草稿已取消')
    await loadLetters()
    emit('refresh')
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.message || '取消草稿失败')
  } finally {
    cancelling.value = false
  }
}

const validate = () => {
  const required = { recipient: '收函单位', clientName: '委托客户', lawyerNames: '办案人', opposingParty: '相对方', caseReason: '案由', lawyerContacts: '办案人联系电话', closingText: '结尾用语' }
  const missing = Object.entries(required).filter(([key]) => !String(form[key] || '').trim() || String(form[key]).startsWith('请填写')).map(([, label]) => label)
  if (missing.length) {
    ElMessage.warning(`请补充：${missing.join('、')}`)
    return false
  }
  return true
}

const saveDraft = async (quiet = false) => {
  if (!validate()) return false
  saving.value = true
  try {
    const response = await updateLawFirmLetter(selected.value.id, { ...form })
    const index = letters.value.findIndex(item => item.id === response.data.id)
    if (index >= 0) letters.value[index] = response.data
    selectLetter(response.data)
    if (!quiet) ElMessage.success('所函内容已保存')
    return true
  } catch (error) {
    ElMessage.error(error?.message || '保存失败')
    return false
  } finally {
    saving.value = false
  }
}

const submitForSeal = async () => {
  if (!await saveDraft(true)) return
  try {
    await ElMessageBox.confirm('提交后内容将锁定，并发送至行政人员的公章用印待办。确认提交？', '申请公章用印', {
      confirmButtonText: '确认提交', cancelButtonText: '取消', type: 'warning'
    })
    submitting.value = true
    const response = await submitLawFirmLetter(selected.value.id)
    ElMessage.success('所函已提交用印审批')
    await loadLetters(response.data.id)
    emit('refresh')
  } catch (error) {
    if (error !== 'cancel') ElMessage.error(error?.message || '提交用印审批失败')
  } finally {
    submitting.value = false
  }
}

const downloadDocx = async () => {
  try {
    const response = await downloadLawFirmLetter(selected.value.id)
    const blob = response.data instanceof Blob ? response.data : new Blob([response.data])
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = `律所所函_${selected.value.letterNumber || '待编号'}.docx`
    link.click()
    URL.revokeObjectURL(link.href)
  } catch (error) {
    ElMessage.error(error?.message || '下载失败')
  }
}

const statusType = status => ({ DRAFT: 'info', REJECTED: 'danger', PENDING_SEAL: 'warning', APPROVED: 'success' }[status] || 'info')
const yearOf = value => value ? Number(String(value).slice(0, 4)) : new Date().getFullYear()
const chineseDate = value => {
  if (!value) return '-'
  const date = new Date(`${value}T00:00:00`)
  const digits = '〇一二三四五六七八九'
  const year = String(date.getFullYear()).split('').map(item => digits[Number(item)]).join('')
  const number = n => n <= 10 ? '零一二三四五六七八九十'[n] : n < 20 ? `十${'一二三四五六七八九'[n - 11] || ''}` : `${'二三'[Math.floor(n / 10) - 2]}十${n % 10 ? '一二三四五六七八九'[n % 10 - 1] : ''}`
  return `${year}年${number(date.getMonth() + 1)}月${number(date.getDate())}日`
}

watch(() => props.caseData.id, id => { if (id) loadLetters() })
onMounted(() => { if (props.caseData.id) loadLetters() })
</script>

<style scoped>
.letter-page { display: grid; gap: 16px; }
.page-toolbar, .section-heading, .editor-actions { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.page-toolbar h3, .section-heading h4 { margin: 0; color: #1d1d1f; }
.page-toolbar p, .section-heading span { margin: 4px 0 0; color: #6e6e73; font-size: 13px; }
.letter-switcher { display: flex; gap: 8px; overflow-x: auto; padding-bottom: 2px; }
.letter-switcher button { min-width: 220px; height: 44px; border: 1px solid #d2d2d7; background: #fff; padding: 0 10px; display: flex; align-items: center; justify-content: space-between; cursor: pointer; border-radius: 6px; }
.letter-switcher button.active { border-color: #1677ff; box-shadow: 0 0 0 1px #1677ff; }
.editor-grid { display: grid; grid-template-columns: minmax(330px, 0.8fr) minmax(480px, 1.2fr); gap: 18px; align-items: start; }
.letter-editor, .preview-shell { border: 1px solid #dedee3; background: #fff; border-radius: 8px; padding: 18px; }
.section-heading { border-bottom: 1px solid #ececf0; padding-bottom: 12px; margin-bottom: 16px; }
.form-row { display: grid; grid-template-columns: 1fr 1.4fr; gap: 12px; }
.form-row :deep(.el-select), .form-row :deep(.el-date-editor) { width: 100%; }
.editor-actions { justify-content: flex-end; border-top: 1px solid #ececf0; padding-top: 14px; flex-wrap: wrap; }
.preview-shell { background: #f5f5f7; }
.a4-preview { aspect-ratio: 210 / 297; width: min(100%, 680px); margin: 0 auto; background: #fff; color: #000; padding: 11.4% 10.7% 6.7% 11.4%; box-sizing: border-box; box-shadow: 0 2px 10px rgba(0,0,0,.08); font-family: 'FangSong', 'STFangsong', serif; overflow: hidden; }
.a4-preview h1 { margin: 0 0 1.2em; text-align: center; font-size: clamp(20px, 2.3vw, 36px); line-height: 1.2; font-weight: 700; white-space: nowrap; }
.a4-preview p { font-size: clamp(12px, 1.25vw, 16px); line-height: 1.85; margin: 0; }
.letter-number { text-align: right; font-weight: 700; margin-bottom: 1.4em !important; }
.recipient { font-weight: 700; }
.body-copy { text-indent: 2em; min-height: 9em; text-align: justify; }
.body-copy u, .recipient u { text-underline-offset: 3px; font-weight: 700; }
.closing { text-indent: 2em; margin-top: 1.2em !important; }
.signature { display: grid; justify-items: end; gap: .45em; margin-top: 2.1em; font-size: clamp(12px, 1.25vw, 16px); }
.a4-preview footer { margin-top: 3em; font-weight: 700; }
.a4-preview footer p { font-size: clamp(10px, 1.05vw, 14px); line-height: 1.7; }
@media (max-width: 1050px) { .editor-grid { grid-template-columns: 1fr; } .a4-preview { width: min(100%, 620px); } }
@media (max-width: 600px) { .page-toolbar { align-items: flex-start; flex-direction: column; } .form-row { grid-template-columns: 1fr; } .letter-editor, .preview-shell { padding: 12px; } .a4-preview h1 { white-space: normal; } }
</style>
