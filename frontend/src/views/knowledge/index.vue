<template>
  <div class="knowledge">
    <PageHeader title="知识库" />

    <div class="knowledge-container">
      <!-- 左侧分类导航 -->
      <div class="sidebar">
        <el-menu
          :default-active="activeSource"
          @select="handleSourceChange"
          class="type-menu"
        >
          <el-menu-item index="">
            <el-icon><Document /></el-icon>
            <span>全部文章</span>
          </el-menu-item>
          <el-menu-item index="LAW_REGULATION">
            <el-icon><Reading /></el-icon>
            <span>法律法规</span>
          </el-menu-item>
          <el-menu-item index="FIRM_POLICY">
            <el-icon><DocumentChecked /></el-icon>
            <span>律所制度</span>
          </el-menu-item>
          <el-menu-item index="PUBLIC_TEMPLATE">
            <el-icon><Tickets /></el-icon>
            <span>公共模板</span>
          </el-menu-item>
          <el-menu-item index="REFERENCE_MATERIAL">
            <el-icon><Collection /></el-icon>
            <span>参考资料</span>
          </el-menu-item>
        </el-menu>

        <el-divider />

        <div class="quick-links">
          <h4>快捷入口</h4>
          <el-link @click="handleCreate" type="primary">
            <el-icon><Plus /></el-icon>
            新建文章
          </el-link>
          <el-link @click="openImportDialog" style="display: block; margin-top: 10px;">
            <el-icon><Upload /></el-icon>
            导入文档
          </el-link>
          <el-link v-if="canManageKnowledge" @click="openSourceDialog" style="display: block; margin-top: 10px;">
            <el-icon><Connection /></el-icon>
            来源同步
          </el-link>
          <el-link v-if="canManageKnowledge" @click="showPendingReviews" style="display: block; margin-top: 10px;">
            <el-icon><DocumentChecked /></el-icon>
            待审核知识
          </el-link>
          <el-link @click="showMyArticles" style="display: block; margin-top: 10px;">
            <el-icon><User /></el-icon>
            我的文章
          </el-link>
        </div>
      </div>

      <!-- 右侧内容区 -->
      <div class="main-content">
        <!-- 搜索栏 -->
        <div class="search-bar">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索文章标题、内容、标签..."
            clearable
            @clear="handleSearch"
            @keyup.enter="handleSearch"
          >
            <template #append>
              <el-button @click="handleSearch" :icon="Search" />
            </template>
          </el-input>
        </div>

        <!-- 置顶文章 -->
        <div v-if="topArticles.length > 0" class="top-articles">
          <h3>
            <el-icon><Star /></el-icon>
            置顶推荐
          </h3>
          <div class="article-cards">
            <div
              v-for="article in topArticles"
              :key="article.id"
              class="article-card top"
              @click="handleView(article)"
            >
              <div class="card-header">
                <div class="tag-row">
                  <el-tag :type="getTypeTagType(article.articleType)" size="small">
                    {{ formatType(article.articleType) }}
                  </el-tag>
                  <el-tag size="small" effect="plain">
                    {{ formatKnowledgeSource(article.knowledgeSource) }}
                  </el-tag>
                </div>
                <span class="view-count">
                  <el-icon><View /></el-icon>
                  {{ article.viewCount }}
                </span>
              </div>
              <h4>{{ article.title }}</h4>
              <p class="summary">{{ article.summary || article.content?.replace(/<[^>]+>/g, '').substring(0, 100) }}</p>
              <div class="card-footer">
                <span class="author">{{ article.authorName }}</span>
                <span class="date">{{ formatDate(article.createdAt) }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 文章列表 -->
        <div class="article-list">
          <div class="list-header">
            <h3>{{ currentTitle }}</h3>
            <div class="header-actions">
              <el-button v-if="canManageKnowledge" @click="showPendingReviews">
                <el-icon><DocumentChecked /></el-icon>
                待审核
              </el-button>
              <el-button v-if="canManageKnowledge" @click="openSourceDialog">
                <el-icon><Connection /></el-icon>
                来源同步
              </el-button>
              <el-button @click="openImportDialog">
                <el-icon><Upload /></el-icon>
                导入文档
              </el-button>
              <el-button type="primary" @click="handleCreate">
                <el-icon><Plus /></el-icon>
                新建文章
              </el-button>
            </div>
          </div>

          <el-empty v-if="articles.length === 0" description="暂无文章" />

          <div v-else class="article-items">
            <div
              v-for="article in articles"
              :key="article.id"
              class="article-item"
              @click="handleView(article)"
            >
              <div class="item-header">
                <h4 class="title">{{ article.title }}</h4>
                <div class="tag-row">
                  <el-tag :type="getTypeTagType(article.articleType)" size="small">
                    {{ formatType(article.articleType) }}
                  </el-tag>
                  <el-tag size="small" effect="plain">
                    {{ formatKnowledgeSource(article.knowledgeSource) }}
                  </el-tag>
                  <el-tag size="small" :type="getIndexStatusType(article.indexStatus)">
                    {{ formatIndexStatus(article.indexStatus) }}
                  </el-tag>
                  <el-tag v-if="article.reviewStatus === 'PENDING_REVIEW'" size="small" type="warning">待审核</el-tag>
                  <el-tag
                    v-if="article.knowledgeSource === 'LAW_REGULATION'"
                    size="small"
                    :type="getValidityStatusType(article.validityStatus)"
                  >
                    {{ formatValidityStatus(article.validityStatus) }}
                  </el-tag>
                </div>
              </div>
              <p class="summary">{{ article.summary || article.content?.replace(/<[^>]+>/g, '').substring(0, 150) }}</p>
              <div class="item-meta">
                <span class="author">
                  <el-icon><User /></el-icon>
                  {{ article.authorName }}
                </span>
                <span class="category" v-if="article.category">
                  <el-icon><FolderOpened /></el-icon>
                  {{ article.category }}
                </span>
                <span class="category" v-if="article.issuingAuthority">
                  <el-icon><OfficeBuilding /></el-icon>
                  {{ article.issuingAuthority }}
                </span>
                <span class="tags" v-if="article.tags">
                  <el-icon><PriceTag /></el-icon>
                  {{ article.tags.split(',').slice(0, 3).join(', ') }}
                </span>
                <span class="stats">
                  <el-icon><View /></el-icon>
                  {{ article.viewCount }}
                  <el-icon style="margin-left: 10px;"><Star /></el-icon>
                  {{ article.likeCount }}
                </span>
                <span class="date">{{ formatDate(article.createdAt) }}</span>
                <span v-if="canManageKnowledge && article.reviewStatus === 'PENDING_REVIEW'" class="review-actions" @click.stop>
                  <el-button link type="success" @click="reviewArticle(article, 'APPROVED')">批准</el-button>
                  <el-button link type="danger" @click="reviewArticle(article, 'REJECTED')">驳回</el-button>
                </span>
              </div>
            </div>
          </div>

          <!-- 分页 -->
          <el-pagination
            v-if="total > 0"
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="total"
            layout="total, prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <el-dialog
      v-model="importDialogVisible"
      title="导入知识文档"
      width="560px"
      :close-on-click-modal="false"
      @closed="resetImportForm"
    >
      <el-form label-position="top">
        <el-form-item label="知识来源" required>
          <el-select v-model="importForm.knowledgeSource" style="width: 100%;">
            <el-option label="法律法规" value="LAW_REGULATION" />
            <el-option label="律所内部制度" value="FIRM_POLICY" />
            <el-option label="公共模板" value="PUBLIC_TEMPLATE" />
            <el-option label="参考资料" value="REFERENCE_MATERIAL" />
          </el-select>
        </el-form-item>
        <template v-if="importForm.knowledgeSource === 'LAW_REGULATION'">
          <el-form-item label="法规信息">
            <div class="metadata-row">
              <el-input v-model="importForm.issuingAuthority" placeholder="发布机关" maxlength="200" />
              <el-input v-model="importForm.documentNumber" placeholder="文号" maxlength="100" />
            </div>
          </el-form-item>
          <el-form-item label="生效与时效">
            <div class="metadata-row">
              <el-date-picker
                v-model="importForm.effectiveDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="生效日期"
                style="width: 100%;"
              />
              <el-select v-model="importForm.validityStatus" style="width: 100%;">
                <el-option label="现行有效" value="EFFECTIVE" />
                <el-option label="已修订" value="AMENDED" />
                <el-option label="已废止" value="REPEALED" />
                <el-option label="待核验" value="UNKNOWN" />
              </el-select>
            </div>
          </el-form-item>
        </template>
        <el-form-item label="来源依据">
          <el-input
            v-model="importForm.sourceReference"
            placeholder="官方网址、内部文件编号或经授权来源说明"
            maxlength="500"
          />
        </el-form-item>
        <el-alert
          v-if="importForm.knowledgeSource === 'REFERENCE_MATERIAL'"
          class="authorization-alert"
          title="外部参考资料只有在确认内部使用授权后，才会进入 AI 知识检索。"
          type="warning"
          :closable="false"
          show-icon
        />
        <el-form-item label="文档原件" required>
          <el-upload
            drag
            :auto-upload="false"
            :limit="1"
            accept=".pdf,.docx,.txt,.md"
            :file-list="importFileList"
            :on-change="handleImportFileChange"
            :on-remove="handleImportFileRemove"
          >
            <el-icon class="upload-icon"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖入文件，或点击选择</div>
            <template #tip>
              <div class="el-upload__tip">支持 PDF、DOCX、TXT、MD；扫描版 PDF 将在本机自动 OCR，原件不会发送云端</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="importForm.title" placeholder="不填写时使用文件名" maxlength="200" />
        </el-form-item>
        <el-form-item label="分类与标签">
          <div class="metadata-row">
            <el-input v-model="importForm.category" placeholder="分类，如：利益冲突" />
            <el-input v-model="importForm.tags" placeholder="标签，多个用逗号分隔" />
          </div>
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="importForm.summary" type="textarea" :rows="3" maxlength="1000" show-word-limit />
        </el-form-item>
        <div class="import-options">
          <el-checkbox
            v-if="importForm.knowledgeSource === 'REFERENCE_MATERIAL'"
            v-model="importForm.authorizationConfirmed"
          >已确认具备内部使用授权</el-checkbox>
          <el-checkbox v-model="importForm.isPublic">全所可见</el-checkbox>
          <el-checkbox
            v-model="importForm.knowledgeEligible"
            :disabled="!importForm.isPublic || (importForm.knowledgeSource === 'REFERENCE_MATERIAL' && !importForm.authorizationConfirmed)"
          >进入 AI 知识检索</el-checkbox>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="submitImport">导入</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="sourceDialogVisible" title="知识来源同步" width="min(920px, 92vw)" :close-on-click-modal="false">
      <el-tabs v-model="sourceTab">
        <el-tab-pane label="国家法律法规数据库" name="flk">
          <div class="starter-import">
            <div>
              <strong>基础法规库</strong>
              <p>从国家法律法规数据库加载 11 项常用现行法律及司法解释，下载后仍需审核发布。</p>
            </div>
            <el-button type="primary" :loading="sourceLoading" @click="loadStarterLaws">加载基础法规</el-button>
          </div>
          <el-input v-model="flkUrls" type="textarea" :rows="6"
            placeholder="每行一个 https://flk.npc.gov.cn 法规详情链接，单批最多50条" />
          <div class="source-actions">
            <el-button type="primary" :loading="sourceLoading" @click="createFlkPreview">生成预览</el-button>
            <span>网站拒绝自动下载时，项目会标记为“需补传”，不会绕过访问限制。</span>
          </div>
        </el-tab-pane>
        <el-tab-pane label="律所制度" name="policy">
          <div class="policy-scan">
            <div>
              <strong>NAS 制度目录</strong>
              <p>只读扫描文件名、相对路径和哈希，不修改源文件。</p>
            </div>
            <el-button type="primary" :loading="sourceLoading" @click="scanPolicies">开始扫描</el-button>
          </div>
        </el-tab-pane>
      </el-tabs>

        <el-table :data="importBatches" size="small" highlight-current-row @row-click="loadImportItems">
        <el-table-column prop="id" label="批次" width="80" />
        <el-table-column prop="sourceType" label="来源" width="130">
          <template #default="{ row }">{{ row.sourceType === 'FLK' ? '法律法规' : '律所制度' }}</template>
        </el-table-column>
        <el-table-column prop="itemCount" label="文件/链接" width="100" />
        <el-table-column prop="status" label="状态" min-width="140">
          <template #default="{ row }">{{ formatImportStatus(row.status) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210">
          <template #default="{ row }">
            <el-button v-if="row.sourceType === 'FLK' && ['DISCOVERED', 'PENDING_REVIEW', 'FAILED'].includes(row.status)"
              link type="primary" @click.stop="stageBatch(row)">重新尝试下载</el-button>
            <el-button link type="success" @click.stop="confirmBatch(row)">生成待审核条目</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-table v-if="importItems.length" :data="importItems" size="small" class="item-table"
        @selection-change="handleImportSelectionChange">
        <el-table-column type="selection" width="44" :selectable="isImportItemConfirmable" />
        <el-table-column prop="title" label="导入项目" min-width="220" show-overflow-tooltip />
        <el-table-column prop="sourceRelativePath" label="相对路径" min-width="180" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="130">
          <template #default="{ row }">{{ formatImportStatus(row.status) }}</template>
        </el-table-column>
        <el-table-column prop="errorMessage" label="处理说明" min-width="220" show-overflow-tooltip />
        <el-table-column label="补传官方文件" width="140">
          <template #default="{ row }">
            <el-upload v-if="row.status === 'NEEDS_UPLOAD'" action="#" :show-file-list="false" :auto-upload="false"
              accept=".pdf,.doc,.docx,.txt,.md"
              :on-change="file => uploadOfficialAttachment(row, file)">
              <el-button link type="primary">选择文件</el-button>
            </el-upload>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Document, Tickets, Reading, DocumentChecked, Collection,
  Plus, User, Search, Star, View, FolderOpened, PriceTag, Upload, UploadFilled, OfficeBuilding, Connection
} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import request from '@/utils/request'
import {
  confirmKnowledgeImport, createStarterFlkImport, getKnowledgeImportBatches, getKnowledgeImportItems,
  getPendingKnowledgeReviews, importKnowledgeDocument, previewFlkImport, reviewKnowledgeArticle,
  scanFirmPolicies, stageKnowledgeImport, uploadKnowledgeImportAttachment
} from '@/api/knowledge'
import { useUserStore } from '@/stores'

const router = useRouter()
const userStore = useUserStore()
const canManageKnowledge = computed(() => userStore.hasPermission('KNOWLEDGE_MANAGE'))

const activeSource = ref('')
const listMode = ref('catalog')
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const topArticles = ref([])
const articles = ref([])
const importDialogVisible = ref(false)
const importing = ref(false)
const importFile = ref(null)
const importFileList = ref([])
const importForm = ref(defaultImportForm())
const sourceDialogVisible = ref(false)
const sourceTab = ref('flk')
const sourceLoading = ref(false)
const flkUrls = ref('')
const importBatches = ref([])
const importItems = ref([])
const selectedImportBatchId = ref(null)
const selectedImportItemIds = ref([])

function defaultImportForm() {
  return {
    knowledgeSource: 'LAW_REGULATION',
    title: '',
    category: '',
    tags: '',
    summary: '',
    sourceReference: '',
    issuingAuthority: '',
    documentNumber: '',
    effectiveDate: '',
    validityStatus: 'UNKNOWN',
    authorizationConfirmed: false,
    isPublic: true,
    knowledgeEligible: true
  }
}

const currentTitle = computed(() => {
  if (listMode.value === 'mine') return '我的文章'
  if (listMode.value === 'review') return '待审核知识'
  if (searchKeyword.value) return `搜索结果：${searchKeyword.value}`
  const sourceMap = {
    LAW_REGULATION: '法律法规',
    FIRM_POLICY: '律所制度',
    PUBLIC_TEMPLATE: '公共模板',
    REFERENCE_MATERIAL: '参考资料'
  }
  return sourceMap[activeSource.value] || '全部文章'
})

// 加载置顶文章
const loadTopArticles = async () => {
  try {
    const { data } = await request({
      url: '/knowledge/top',
      method: 'get'
    })
    topArticles.value = data || []
  } catch (error) {
    console.error('加载置顶文章失败:', error)
    ElMessage.error('加载置顶文章失败')
  }
}

// 加载文章列表
const loadArticles = async () => {
  try {
    listMode.value = 'catalog'
    const params = { page: currentPage.value - 1, size: pageSize.value }
    let url = '/knowledge'

    if (searchKeyword.value.trim()) {
      url = '/knowledge/search'
      params.keyword = searchKeyword.value.trim()
      if (activeSource.value) params.source = activeSource.value
    } else if (activeSource.value) {
      url = `/knowledge/source/${activeSource.value}`
    }

    const { data } = await request({
      url,
      method: 'get',
      params
    })
    articles.value = data.content || []
    total.value = data.totalElements || 0
  } catch (error) {
    console.error('加载文章列表失败:', error)
    ElMessage.error('加载文章列表失败')
  }
}

// 加载我的文章
const loadMyArticles = async () => {
  try {
    listMode.value = 'mine'
    activeSource.value = ''
    searchKeyword.value = ''
    const { data } = await request({
      url: '/knowledge/my',
      method: 'get',
      params: {
        page: currentPage.value - 1,
        size: pageSize.value
      }
    })
    articles.value = data.content || []
    total.value = data.totalElements || 0
  } catch (error) {
    console.error('加载我的文章失败:', error)
    ElMessage.error('加载失败')
  }
}

const showMyArticles = () => {
  currentPage.value = 1
  loadMyArticles()
}

const loadPendingReviews = async () => {
  if (!canManageKnowledge.value) return
  try {
    listMode.value = 'review'
    activeSource.value = ''
    searchKeyword.value = ''
    const { data } = await getPendingKnowledgeReviews({
      page: currentPage.value - 1,
      size: pageSize.value
    })
    articles.value = data.content || []
    total.value = data.totalElements || 0
  } catch (error) {
    console.error('加载待审核知识失败:', error)
  }
}

const showPendingReviews = () => {
  currentPage.value = 1
  loadPendingReviews()
}

// 类型切换
const handleSourceChange = (source) => {
  listMode.value = 'catalog'
  activeSource.value = source
  searchKeyword.value = ''
  currentPage.value = 1
  loadArticles()
}

// 搜索
const handleSearch = () => {
  listMode.value = 'catalog'
  currentPage.value = 1
  loadArticles()
}

// 分页
const handlePageChange = () => {
  if (listMode.value === 'mine') loadMyArticles()
  else if (listMode.value === 'review') loadPendingReviews()
  else loadArticles()
}

// 查看文章
const handleView = (article) => {
  router.push(`/knowledge/${article.id}`)
}

// 创建文章
const handleCreate = () => {
  router.push('/knowledge/create')
}

const openImportDialog = () => {
  const allowedSources = ['LAW_REGULATION', 'FIRM_POLICY', 'PUBLIC_TEMPLATE', 'REFERENCE_MATERIAL']
  importForm.value.knowledgeSource = allowedSources.includes(activeSource.value)
    ? activeSource.value
    : 'LAW_REGULATION'
  importDialogVisible.value = true
}

const handleImportFileChange = (uploadFile, uploadFiles) => {
  importFile.value = uploadFile.raw
  importFileList.value = uploadFiles.slice(-1)
}

const handleImportFileRemove = () => {
  importFile.value = null
  importFileList.value = []
}

const resetImportForm = () => {
  importForm.value = defaultImportForm()
  importFile.value = null
  importFileList.value = []
}

const submitImport = async () => {
  if (!importFile.value) {
    ElMessage.warning('请选择要导入的知识文档')
    return
  }
  importing.value = true
  try {
    const data = new FormData()
    data.append('file', importFile.value)
    data.append('knowledgeSource', importForm.value.knowledgeSource)
    data.append('articleType', importForm.value.knowledgeSource === 'PUBLIC_TEMPLATE' ? 'TEMPLATE' : 'DOCUMENT')
    data.append('isPublic', String(importForm.value.isPublic))
    data.append('knowledgeEligible', String(importForm.value.isPublic && importForm.value.knowledgeEligible))
    data.append('validityStatus', importForm.value.validityStatus || 'UNKNOWN')
    data.append('authorizationConfirmed', String(importForm.value.authorizationConfirmed))
    ;['title', 'category', 'tags', 'summary', 'sourceReference', 'issuingAuthority', 'documentNumber', 'effectiveDate'].forEach(key => {
      const value = importForm.value[key]?.trim()
      if (value) data.append(key, value)
    })
    await importKnowledgeDocument(data)
    const requiresReview = ['LAW_REGULATION', 'FIRM_POLICY', 'REFERENCE_MATERIAL'].includes(importForm.value.knowledgeSource)
    ElMessage.success(requiresReview ? '文档已提交审核，可在“我的文章”查看' : '知识文档导入成功')
    importDialogVisible.value = false
    currentPage.value = 1
    await Promise.all([loadArticles(), loadTopArticles()])
  } catch (error) {
    console.error('导入知识文档失败:', error)
  } finally {
    importing.value = false
  }
}

const refreshBatches = async () => {
  if (!canManageKnowledge.value) return
  const response = await getKnowledgeImportBatches()
  importBatches.value = response.data || []
}

const openSourceDialog = async () => {
  sourceDialogVisible.value = true
  importItems.value = []
  selectedImportBatchId.value = null
  selectedImportItemIds.value = []
  await refreshBatches()
}

const createFlkPreview = async () => {
  const urls = flkUrls.value.split(/\r?\n/).map(value => value.trim()).filter(Boolean)
  if (!urls.length) return ElMessage.warning('请粘贴法规详情链接')
  sourceLoading.value = true
  try {
    await previewFlkImport(urls)
    flkUrls.value = ''
    await refreshBatches()
    ElMessage.success('法规链接预览已建立')
  } finally { sourceLoading.value = false }
}

const loadStarterLaws = async () => {
  sourceLoading.value = true
  try {
    const response = await createStarterFlkImport()
    const batch = response.data
    await stageKnowledgeImport(batch.id)
    await refreshBatches()
    await loadImportItems(batch)
    ElMessage.success('基础法规已下载并暂存，请核对后生成待审核条目')
  } finally { sourceLoading.value = false }
}

const scanPolicies = async () => {
  sourceLoading.value = true
  try {
    await scanFirmPolicies()
    await refreshBatches()
    ElMessage.success('NAS制度目录扫描完成')
  } finally { sourceLoading.value = false }
}

const loadImportItems = async row => {
  const response = await getKnowledgeImportItems(row.id)
  importItems.value = response.data || []
  selectedImportBatchId.value = row.id
  selectedImportItemIds.value = []
}

const handleImportSelectionChange = rows => {
  selectedImportItemIds.value = rows.map(row => row.id)
}

const isImportItemConfirmable = row => row.status === 'STAGED'
  || (importBatches.value.find(batch => batch.id === row.batchId)?.sourceType === 'FIRM_POLICY' && row.status === 'DISCOVERED')

const stageBatch = async row => {
  sourceLoading.value = true
  try {
    await stageKnowledgeImport(row.id)
    await Promise.all([refreshBatches(), loadImportItems(row)])
  } finally { sourceLoading.value = false }
}

const confirmBatch = async row => {
  sourceLoading.value = true
  try {
    const itemIds = selectedImportBatchId.value === row.id ? selectedImportItemIds.value : []
    await confirmKnowledgeImport(row.id, itemIds)
    await Promise.all([refreshBatches(), loadImportItems(row), loadArticles()])
    ElMessage.success('已生成待审核知识条目')
  } finally { sourceLoading.value = false }
}

const uploadOfficialAttachment = async (row, uploadFile) => {
  if (!uploadFile.raw) return
  await uploadKnowledgeImportAttachment(row.id, uploadFile.raw)
  const batch = importBatches.value.find(item => item.id === row.batchId)
  if (batch) await loadImportItems(batch)
  ElMessage.success('官方文件已暂存')
}

const reviewArticle = async (article, decision) => {
  let reason = ''
  if (decision === 'REJECTED') {
    const result = await ElMessageBox.prompt('请输入驳回原因', '驳回知识条目', { inputValidator: value => Boolean(value?.trim()) || '请填写原因' })
    reason = result.value
  } else {
    await ElMessageBox.confirm('确认该条目来源与内容无误，并允许进入全所知识检索？', '批准发布', { type: 'warning' })
  }
  await reviewKnowledgeArticle(article.id, decision, reason)
  await (listMode.value === 'review' ? loadPendingReviews() : loadArticles())
  ElMessage.success(decision === 'APPROVED' ? '已批准发布' : '已驳回')
}

const formatImportStatus = status => ({
  DISCOVERED: '待处理',
  NEEDS_UPLOAD: '需补传',
  STAGED: '已暂存',
  CONVERSION_REQUIRED: '需转换格式',
  PENDING_REVIEW: '待审核',
  APPROVED: '已批准',
  REJECTED: '已驳回',
  FAILED: '处理失败'
}[status] || status || '-')

// 格式化类型
const formatType = (type) => {
  const map = {
    'DOCUMENT': '文档',
    'TEMPLATE': '模板',
    'CASE': '案件沉淀',
    'GUIDE': '指南',
    'EXPERIENCE': '经验'
  }
  return map[type] || type
}

// 类型标签颜色
const getTypeTagType = (type) => {
  const map = {
    'DOCUMENT': 'primary',
    'TEMPLATE': 'success',
    'CASE': 'primary',
    'GUIDE': 'warning',
    'EXPERIENCE': 'info'
  }
  return map[type] || ''
}

const formatKnowledgeSource = (source) => {
  const map = {
    LAW_REGULATION: '法规',
    FIRM_POLICY: '制度',
    PUBLIC_TEMPLATE: '模板',
    REFERENCE_MATERIAL: '参考',
    FIRM_KNOWLEDGE: '全所',
    CASE_DEPOSIT: '案件沉淀'
  }
  return map[source] || '全所'
}

const formatIndexStatus = (status) => {
  const map = {
    PENDING: '待索引',
    INDEXED: '已索引',
    FAILED: '失败',
    FORBIDDEN: '禁止',
    NOT_INDEXED: '未索引'
  }
  return map[status] || '未索引'
}

const getIndexStatusType = (status) => {
  const map = {
    INDEXED: 'success',
    PENDING: 'warning',
    FAILED: 'danger',
    FORBIDDEN: 'info'
  }
  return map[status] || 'info'
}

const formatValidityStatus = (status) => {
  const map = {
    EFFECTIVE: '现行有效',
    AMENDED: '已修订',
    REPEALED: '已废止',
    UNKNOWN: '待核验'
  }
  return map[status] || '待核验'
}

const getValidityStatusType = (status) => {
  const map = {
    EFFECTIVE: 'success',
    AMENDED: 'warning',
    REPEALED: 'danger',
    UNKNOWN: 'info'
  }
  return map[status] || 'info'
}

// 格式化日期
const formatDate = (date) => {
  if (!date) return ''
  if (Array.isArray(date)) {
    const [year, month, day] = date
    const pad = number => String(number).padStart(2, '0')
    return `${year}-${pad(month)}-${pad(day)}`
  }
  return new Date(date).toLocaleDateString('zh-CN')
}

onMounted(() => {
  loadTopArticles()
  loadArticles()
})
</script>

<style scoped lang="scss">
.knowledge {
  .knowledge-container {
    display: flex;
    gap: 20px;
    margin-top: 20px;
  }

  .sidebar {
    width: 200px;
    flex-shrink: 0;

    .type-menu {
      border-right: none;
    }

    .quick-links {
      padding: 10px 0;

      h4 {
        margin: 0 0 10px;
        font-size: 14px;
        color: #666;
      }

      .el-link {
        width: 100%;
      }
    }
  }

  .main-content {
    flex: 1;
    background: #fff;
    padding: 20px;
    border-radius: 4px;

    .search-bar {
      margin-bottom: 20px;
    }

    .authorization-alert {
      margin-bottom: 16px;
    }

    .top-articles {
      margin-bottom: 30px;

      h3 {
        display: flex;
        align-items: center;
        gap: 8px;
        margin: 0 0 15px;
        color: #333;
      }

      .article-cards {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
        gap: 15px;

        .article-card {
          padding: 15px;
          border: 1px solid #e4e7ed;
          border-radius: 8px;
          cursor: pointer;
          transition: all 0.3s;

          &:hover {
            box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
            transform: translateY(-2px);
          }

          &.top {
            border-color: #f56c6c;
            background: #fef0f0;
          }

          .card-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;

            .tag-row {
              display: flex;
              gap: 6px;
              align-items: center;
              flex-wrap: wrap;
            }

            .view-count {
              display: flex;
              align-items: center;
              gap: 4px;
              font-size: 12px;
              color: #999;
            }
          }

          h4 {
            margin: 0 0 10px;
            font-size: 16px;
            color: #333;
          }

          .summary {
            margin: 0 0 15px;
            font-size: 13px;
            color: #666;
            line-height: 1.6;
            display: -webkit-box;
            -webkit-line-clamp: 3;
            -webkit-box-orient: vertical;
            overflow: hidden;
          }

          .card-footer {
            display: flex;
            justify-content: space-between;
            font-size: 12px;
            color: #999;
          }
        }
      }
    }

    .article-list {
      .list-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 20px;

        h3 {
          margin: 0;
          color: #333;
        }

        .header-actions {
          display: flex;
          gap: 8px;
        }
      }

      .article-items {
        .article-item {
          padding: 20px;
          border-bottom: 1px solid #e4e7ed;
          cursor: pointer;
          transition: background 0.3s;

          &:hover {
            background: #f9f9f9;
          }

          &:last-child {
            border-bottom: none;
          }

          .item-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            gap: 12px;
            margin-bottom: 10px;

            .title {
              margin: 0;
              font-size: 16px;
              color: #303133;
              flex: 1;
            }
          }

          .tag-row {
            display: flex;
            gap: 6px;
            align-items: center;
            flex-wrap: wrap;
            flex-shrink: 0;
          }

          .summary {
            margin: 0 0 15px;
            color: #606266;
            line-height: 1.6;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
          }

          .item-meta {
            display: flex;
            gap: 20px;
            font-size: 13px;
            color: #909399;

            span {
              display: flex;
              align-items: center;
              gap: 4px;
            }
          }
        }
      }

      .el-pagination {
        margin-top: 20px;
        justify-content: center;
      }
    }
  }

  .metadata-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
    width: 100%;
  }

  .import-options {
    display: flex;
    gap: 20px;
  }

  :deep(.el-upload),
  :deep(.el-upload-dragger) {
    width: 100%;
  }

  .upload-icon {
    font-size: 34px;
    color: #606266;
  }

  .starter-import,
  .source-actions,
  .policy-scan {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin: 14px 0 18px;
  }

  .starter-import p,
  .source-actions span,
  .policy-scan p {
    color: var(--zg-text-secondary);
    font-size: 13px;
  }

  .starter-import { margin-bottom: 16px; }
  .starter-import p, .policy-scan p { margin: 4px 0 0; }
  .item-table { margin-top: 18px; }
  .review-actions { display: inline-flex; align-items: center; }

  @media (max-width: 768px) {
    .knowledge-container { flex-direction: column; }
    .sidebar { width: 100%; }
    .starter-import, .source-actions, .policy-scan { align-items: stretch; flex-direction: column; }
  }
}
</style>
