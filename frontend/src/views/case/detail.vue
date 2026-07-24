<template>
  <div class="case-detail" data-testid="case-detail" v-loading="loading">
    <el-result v-if="loadError" icon="error" title="案件详情加载失败" :sub-title="loadError">
      <template #extra>
        <el-button @click="$router.push('/case/list')">返回案件列表</el-button>
        <el-button type="primary" @click="fetchCaseDetail">重新加载</el-button>
      </template>
    </el-result>

    <template v-else>
    <!-- 顶部固定区域 -->
    <div class="detail-header" data-testid="case-detail-header">
      <div class="header-left">
        <el-button circle @click="$router.back()">
          <el-icon><ArrowLeft /></el-icon>
        </el-button>
        <div class="case-info">
          <h2 class="case-name">{{ caseDetail.caseName }}</h2>
          <div class="case-meta">
            <span class="case-number">{{ caseDetail.caseNumber || '暂未生成案件编号' }}</span>
            <el-tag size="small" effect="plain">{{ caseDetail.caseTypeDesc || caseDetail.caseType || '案件' }}</el-tag>
            <el-tag size="small" :type="statusTagType">{{ caseDetail.statusDesc || caseDetail.status || '待完善' }}</el-tag>
          </div>
        </div>
      </div>

      <div class="header-right" data-testid="case-detail-actions">
        <el-button v-if="canEdit" @click="handleEdit">
          <el-icon><Edit /></el-icon>
          编辑
        </el-button>
        <el-button v-if="canUseCaseAI" type="primary" @click="openAIWorkbench">
          <el-icon><ChatDotRound /></el-icon>
          AI助手
        </el-button>
        <CaseClosureButton
          v-if="canRequestClosure"
          :case-id="caseDetail.id"
          @submitted="handleClosureSubmitted"
        />
        <el-button v-if="canArchive" @click="openArchiveWorkflow">
          <el-icon><FolderOpened /></el-icon>
          归档
        </el-button>
        <el-dropdown @command="handleMoreAction">
          <el-button>
            更多
            <el-icon><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item v-if="canCopy" command="copy">复制案件</el-dropdown-item>
              <el-dropdown-item command="export">导出信息</el-dropdown-item>
              <el-dropdown-item v-if="canDelete" command="delete" divided>删除案件</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

    </div>

    <!-- 进度条 -->
    <div class="progress-section" data-testid="case-stage-progress">
      <div class="progress-bar">
        <div
          v-for="(stage, index) in caseStages"
          :key="stage.key"
          class="progress-item"
          :class="{
            'active': isStageActive(stage.key),
            'completed': isStageCompleted(stage.key),
            'current': isCurrentStage(stage.key),
            'disabled': !canChangeStatus
          }"
          :aria-disabled="!canChangeStatus"
          @click="handleStageClick(stage)"
        >
          <div class="stage-icon">
            <el-icon v-if="isStageCompleted(stage.key)"><Select /></el-icon>
            <span v-else>{{ index + 1 }}</span>
          </div>
          <div class="stage-label">{{ stage.label }}</div>
          <div v-if="index < caseStages.length - 1" class="stage-line"></div>
        </div>
      </div>
      <div class="progress-info">
        <span>当前阶段：{{ caseDetail.currentStage }}</span>
        <el-button v-if="canChangeStatus" text type="primary" size="small" @click="handleUpdateStage">
          更新阶段
        </el-button>
        <el-button text type="success" size="small" @click="handleViewApprovals">
          查看审批
        </el-button>
      </div>
    </div>

    <!-- Tab导航 -->
    <nav class="detail-tabs" aria-label="案件详情导航">
      <button
        v-for="tab in detailTabs"
        :key="tab.name"
        type="button"
        class="detail-tab"
        :data-testid="`case-tab-${tab.name}`"
        :class="{ active: activeTab === tab.name }"
        :aria-current="activeTab === tab.name ? 'page' : undefined"
        @click="navigateTab(tab.name)"
      >
        {{ tab.label }}
      </button>
    </nav>

    <!-- Tab内容 -->
    <div class="tab-content" data-testid="case-tab-content">
      <router-view :case-data="caseDetail" @refresh="fetchCaseDetail" />
    </div>

    <ApprovalDetailDrawer
      v-model="approvalDrawerVisible"
      :case-id="caseDetail.id"
      @handled="handleApprovalHandled"
    />
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft, Edit, FolderOpened, ArrowDown, Select, ChatDotRound
} from '@element-plus/icons-vue'
import { getCaseDetail, updateCaseStatus, rollbackCaseStatus, deleteCase, createCase } from '@/api/case'
import { getCaseTypeWorkflow } from '@/utils/caseTypeProfiles'
import ApprovalDetailDrawer from '@/components/ApprovalDetailDrawer.vue'
import CaseClosureButton from '@/components/CaseClosureButton.vue'
import { useUserStore } from '@/stores/user'
import { formatFeeMethod } from '@/utils/feeMethod'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const loadError = ref('')
const approvalDrawerVisible = ref(false)
const detailTabs = [
  { label: '基本案情', name: 'basic' },
  { label: '办案记录', name: 'record' },
  { label: '受理单位', name: 'unit' },
  { label: '案件文档', name: 'doc' },
  { label: '案件动态', name: 'timeline' },
  { label: '智能归档', name: 'archive' }
]
const activeTab = computed(() => {
  const routeTab = route.path.split('/').filter(Boolean).pop()
  return detailTabs.some(item => item.name === routeTab) ? routeTab : 'basic'
})
const caseDetail = reactive({
  id: null,
  caseName: '',
  caseNumber: '',
  currentStage: '咨询',
  caseType: '',
  ownerId: null
})
const canEdit = computed(() => caseDetail.canEdit === true)
const canDelete = computed(() => caseDetail.canDelete === true)
const canArchive = computed(() => caseDetail.canArchive === true)
const canChangeStatus = computed(() => caseDetail.canChangeStatus === true)
const canCopy = computed(() => userStore.hasPermission('CASE_CREATE'))
const canUseCaseAI = computed(() => canEdit.value && userStore.hasPermission('CASE_EDIT'))
const canRequestClosure = computed(() => {
  if (!canEdit.value || caseDetail.status !== 'ACTIVE' || !caseDetail.stageProgress?.length) return false
  const stages = caseDetail.stageProgress
  return stages[stages.length - 1]?.status === 'IN_PROGRESS'
})
const statusTagType = computed(() => ({
  FILING_REVIEW: 'warning', ACTIVE: 'primary', CLOSED: 'success', ARCHIVED: 'info'
}[caseDetail.status] || 'info'))

// 案件阶段 - 根据案件类型动态获取
const caseStages = computed(() => {
  if (caseDetail.stageProgress?.length) {
    return caseDetail.stageProgress.map(stage => ({
      key: stage.stageName,
      label: stage.stageName,
      status: stage.status
    }))
  }
  return getCaseTypeWorkflow(caseDetail.caseType).map(stage => ({ key: stage, label: stage, status: 'PENDING' }))
})

// 判断阶段是否已完成
const isStageCompleted = (stageKey) => {
  const stage = caseStages.value.find(item => item.key === stageKey)
  if (stage?.status === 'COMPLETED') return true
  const stageIndex = caseStages.value.findIndex(s => s.key === stageKey)
  const currentIndex = caseStages.value.findIndex(s => s.label === caseDetail.currentStage)
  return stageIndex < currentIndex
}

// 判断阶段是否已激活（已完成或当前）
const isStageActive = (stageKey) => {
  return isStageCompleted(stageKey) || isCurrentStage(stageKey)
}

// 判断是否为当前阶段
const isCurrentStage = (stageKey) => {
  const stage = caseStages.value.find(s => s.key === stageKey)
  return stage?.status === 'IN_PROGRESS' || stage?.label === caseDetail.currentStage
}

// 获取案件详情
const fetchCaseDetail = async () => {
  try {
    loading.value = true
    loadError.value = ''
    const { id } = route.params
    const res = await getCaseDetail(id)
    Object.assign(caseDetail, res.data)
  } catch (error) {
    loadError.value = error?.message || '请检查网络连接或案件访问权限'
    ElMessage.error('获取案件详情失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 编辑案件
const handleEdit = () => {
  router.push(`/case/${caseDetail.id}/edit`)
}

// 显示AI助手
const openAIWorkbench = () => {
  router.push({ path: '/ai/case-workbench', query: { caseId: caseDetail.id } })
}

const openArchiveWorkflow = () => router.push(`/case/${caseDetail.id}/archive`)

// 查看审批流程
const handleViewApprovals = () => {
  approvalDrawerVisible.value = true
}

const handleApprovalHandled = async () => {
  await fetchCaseDetail()
}

const handleClosureSubmitted = async () => {
  await fetchCaseDetail()
  approvalDrawerVisible.value = true
}

// 更多操作
const handleMoreAction = async (command) => {
  switch (command) {
    case 'copy':
      try {
        await ElMessageBox.confirm(
          '复制案件将创建一个新案件，包含当前案件的所有信息。是否继续？',
          '复制案件',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'info'
          }
        )

        // 获取当前案件详细信息
        const detailRes = await getCaseDetail(caseDetail.id)

        // 构造新案件数据
        const newCaseData = {
          ...detailRes.data,
          id: undefined, // 移除ID，让系统生成新的
          caseNumber: `${detailRes.data.caseNumber}-副本`,
          caseName: `${detailRes.data.caseName}（副本）`,
          status: 'CONSULTATION', // 重置状态为咨询
          createdAt: undefined,
          updatedAt: undefined
        }

        // 创建新案件
        const newCase = await createCase(newCaseData)

        ElMessage.success('案件复制成功')
        router.push(`/case/${newCase.data.id}`)
      } catch (error) {
        if (error !== 'cancel') {
          console.error('复制案件失败:', error)
          ElMessage.error('复制失败')
        }
      }
      break
    case 'export':
      try {
        await ElMessageBox.confirm(
          '导出案件信息将生成包含案件详细信息的文档，是否继续？',
          '导出案件',
          {
            confirmButtonText: '确定导出',
            cancelButtonText: '取消',
            type: 'info'
          }
        )

        // 生成案件信息文本内容
        const caseText = `
案件信息
==================
案件编号：${caseDetail.caseNumber || '无'}
案件名称：${caseDetail.caseName || '无'}
案件类型：${caseDetail.caseType || '无'}
案件程序：${caseDetail.procedure || '无'}
案由：${caseDetail.caseReason || '无'}
管辖法院：${caseDetail.court || '无'}
立案日期：${caseDetail.filingDate || '无'}
结案日期：${caseDetail.deadlineDate || '无'}
委托日期：${caseDetail.commissionDate || '无'}
案件状态：${caseDetail.statusDesc || caseDetail.status || '无'}

案件摘要
------------------
${caseDetail.summary || '无'}

主办律师：${caseDetail.ownerName || '无'}
协办律师：${caseDetail.coOwners?.map(o => o.name).join('、') || '无'}
律师助理：${caseDetail.assistants?.map(a => a.name).join('、') || '无'}

当事人信息
------------------
${caseDetail.parties?.map(p => `
${p.attribute} - ${p.name}
  类型：${p.type}
  联系电话：${p.phone || '无'}
  身份证号/信用代码：${p.idCard || p.creditCode || '无'}
  地址：${p.address || '无'}
`).join('') || '无'}

律师费信息
------------------
收费方式：${caseDetail.feeMethod ? formatFeeMethod(caseDetail.feeMethod) : caseDetail.feeTypes?.map(formatFeeMethod).join('、') || '无'}
争议标的：${caseDetail.subjectMatter || '无'}
律师费金额：${caseDetail.attorneyFee || caseDetail.lawyerFee || '无'}
收费摘要：${caseDetail.feeDescription || caseDetail.feeSummary || '无'}
收费备注：${caseDetail.feeNotes || caseDetail.feeRemark || '无'}

应收款信息
------------------
${caseDetail.receivables?.map(r => `
  ${r.name}：${r.amount}元
  应收日期：${r.dueDate || '无'}
  备注：${r.notes || '无'}
`).join('') || '无'}

备注
------------------
${caseDetail.remark || '无'}
        `.trim()

        // 创建Blob并下载
        const blob = new Blob([caseText], { type: 'text/plain;charset=utf-8' })
        const link = document.createElement('a')
        link.href = window.URL.createObjectURL(blob)
        link.download = `案件_${caseDetail.caseNumber}_${caseDetail.caseName}.txt`
        link.click()
        window.URL.revokeObjectURL(link.href)

        ElMessage.success('导出成功')
      } catch (error) {
        if (error !== 'cancel') {
          console.error('导出案件失败:', error)
          ElMessage.error('导出失败')
        }
      }
      break
    case 'delete':
      try {
        await ElMessageBox.confirm('确定要删除该案件吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await deleteCase(caseDetail.id)
        ElMessage.success('删除成功')
        router.push('/case/list')
      } catch {
        // 用户取消
      }
      break
  }
}

// 点击阶段
const handleStageClick = async (stage) => {
  if (!canChangeStatus.value) {
    return
  }
  try {
    const currentStageKey = caseStages.value.find(s => s.label === caseDetail.currentStage)?.key

    // 检查是否是当前阶段
    if (stage.key === currentStageKey) {
      ElMessage.info('当前已是该阶段')
      return
    }

    // 检查是前进还是回退
    const currentStageIndex = caseStages.value.findIndex(s => s.key === currentStageKey)
    const targetStageIndex = caseStages.value.findIndex(s => s.key === stage.key)
    const isRollback = targetStageIndex < currentStageIndex

    let confirmMessage = `确定要将案件阶段从"${caseDetail.currentStage}"变更为"${stage.label}"吗？`

    // 如果是回退，需要填写原因
    if (isRollback) {
      try {
        const { value } = await ElMessageBox.prompt(
          `回退阶段需要填写原因\n确定要将案件从"${caseDetail.currentStage}"回退到"${stage.label}"吗？`,
          '回退确认',
          {
            confirmButtonText: '确定回退',
            cancelButtonText: '取消',
            inputPlaceholder: '请输入回退原因',
            inputPattern: /^.{2,100}$/,
            inputErrorMessage: '回退原因长度为2-100个字符'
          }
        )

        confirmMessage = `确定要回退到"${stage.label}"吗？原因：${value}`

        await ElMessageBox.confirm(confirmMessage, '确认回退', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })

        // 调用更新状态API（带原因）
        await rollbackCaseStatus(caseDetail.id, {
          status: stage.label,
          reason: value
        })

        ElMessage.success(`已回退到"${stage.label}"阶段`)
      } catch (error) {
        if (error !== 'cancel') {
          console.error('回退失败:', error)
          ElMessage.error('回退失败')
        }
        return
      }
    } else {
      if (targetStageIndex !== currentStageIndex + 1) {
        ElMessage.warning('案件阶段只能依次推进，请先完成当前阶段')
        return
      }
      // 前进阶段，只需确认
      await ElMessageBox.confirm(confirmMessage, '确认变更', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      })

      // 调用更新状态API
      await updateCaseStatus(caseDetail.id, {
        status: stage.label
      })

      ElMessage.success(`已更新到"${stage.label}"阶段`)

    }

    // 刷新案件详情
    await fetchCaseDetail()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('更新阶段失败:', error)
      ElMessage.error('更新阶段失败')
    }
  }
}

// 更新阶段（按钮触发，显示阶段选择）
const handleUpdateStage = async () => {
  if (!canChangeStatus.value) return
  try {
    const validStages = caseStages.value.map(s => s.label)
    const { value } = await ElMessageBox.prompt(
      `请输入要变更到的阶段（${validStages.join('、')}）`,
      '更新案件阶段',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPlaceholder: caseDetail.currentStage,
        inputValue: caseDetail.currentStage
      }
    )

    // 验证输入的阶段是否有效
    if (!validStages.includes(value)) {
      ElMessage.error(`无效的阶段，请输入：${validStages.join('、')}`)
      return
    }

    const stage = caseStages.value.find(s => s.label === value)
    if (stage) {
      await handleStageClick(stage)
    }
  } catch (error) {
    // 用户取消
  }
}

const navigateTab = (newTab) => {
  const id = route.params.id || caseDetail.id
  if (!id) return
  const targetPath = `/case/${id}/${newTab}`
  if (route.path !== targetPath) {
    router.push(targetPath)
  }
}

watch(() => route.params.id, fetchCaseDetail, { immediate: true })
</script>

<style scoped lang="scss">
.case-detail {
  min-height: 320px;
  min-width: 0;

  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px;
    background-color: #fff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    margin-bottom: 20px;

    .header-left {
      display: flex;
      align-items: center;
      gap: 20px;

      .case-info {
        min-width: 0;

        .case-name {
          margin: 0 0 5px;
          font-size: 18px;
          font-weight: 500;
          color: #333;
          overflow-wrap: anywhere;
        }

        .case-meta {
          display: flex;
          align-items: center;
          flex-wrap: wrap;
          gap: 8px;
        }

        .case-number {
          font-size: 14px;
          color: #999;
        }
      }
    }

    .header-right {
      display: flex;
      flex-wrap: wrap;
      justify-content: flex-end;
      gap: 10px;
    }
  }

  .progress-section {
    background-color: #fff;
    padding: 25px 30px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    margin-bottom: 20px;
    overflow-x: auto;

    .progress-bar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;
      min-width: max-content;

      .progress-item {
        flex: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        position: relative;
        cursor: pointer;
        min-width: 92px;

        .stage-icon {
          width: 32px;
          height: 32px;
          border-radius: 50%;
          background-color: #e4e7ed;
          color: #909399;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 14px;
          font-weight: 500;
          transition: all 0.3s;
          z-index: 2;
        }

        .stage-label {
          margin-top: 8px;
          font-size: 12px;
          color: #909399;
          transition: all 0.3s;
        }

        .stage-line {
          position: absolute;
          top: 16px;
          left: 50%;
          width: 100%;
          height: 2px;
          background-color: #e4e7ed;
          z-index: 1;
        }

        &.active {
          .stage-icon {
            background-color: #1890ff;
            color: #fff;
          }

          .stage-label {
            color: #1890ff;
          }

          .stage-line {
            background-color: #1890ff;
          }
        }

        &.completed {
          .stage-icon {
            background-color: #67c23a;
            color: #fff;
          }

          .stage-label {
            color: #67c23a;
          }

          .stage-line {
            background-color: #67c23a;
          }
        }

        &.current {
          .stage-icon {
            background-color: #1890ff;
            color: #fff;
            box-shadow: 0 0 0 4px rgba(24, 144, 255, 0.2);
            animation: pulse 2s infinite;
          }

          .stage-label {
            color: #1890ff;
            font-weight: 500;
          }
        }

        &.disabled {
          cursor: default;
        }
      }
    }

    .progress-info {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 15px;
      padding-top: 15px;
      border-top: 1px solid #e4e7ed;
      font-size: 14px;
      color: #606266;
    }
  }

  .detail-tabs {
    display: flex;
    align-items: stretch;
    gap: 4px;
    background-color: #fff;
    padding: 0 12px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    margin-bottom: 20px;
    overflow-x: auto;

    .detail-tab {
      flex: 0 0 auto;
      min-height: 48px;
      padding: 0 16px;
      border: 0;
      border-bottom: 2px solid transparent;
      background: transparent;
      color: #606266;
      font: inherit;
      cursor: pointer;
      white-space: nowrap;
      transition: color 0.2s, border-color 0.2s;

      &:hover {
        color: #1d5f9f;
      }

      &.active {
        border-bottom-color: #1d5f9f;
        color: #1d5f9f;
        font-weight: 600;
      }
    }
  }

  .tab-content {
    min-width: 0;
    overflow: hidden;
    background-color: #fff;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
  }
}

@media (max-width: 760px) {
  .case-detail {
    .detail-header {
      align-items: stretch;
      flex-direction: column;
      gap: 14px;
      padding: 16px;

      .header-left {
        align-items: flex-start;
        gap: 12px;
      }

      .header-right {
        justify-content: flex-start;

        .el-button {
          margin-left: 0;
        }
      }
    }

    .progress-section {
      padding: 18px 14px;
      max-width: 100%;

      .progress-bar {
        justify-content: flex-start;
      }

      .progress-info {
        justify-content: flex-start;
        min-width: max-content;
      }
    }

    .detail-tabs {
      max-width: 100%;
      margin-bottom: 14px;
      padding: 0 4px;

      .detail-tab {
        min-height: 44px;
        padding: 0 12px;
      }
    }
  }
}

.approval-drawer-body {
  min-height: 240px;

  .approval-card {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 12px;
    padding: 12px 14px;
    margin-bottom: 10px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    cursor: pointer;
    transition: border-color 0.2s, background-color 0.2s;

    &.active,
    &:hover {
      border-color: #409eff;
      background-color: #f5f9ff;
    }
  }

  .approval-card-main {
    display: flex;
    flex-direction: column;
    gap: 6px;
    min-width: 0;

    strong {
      color: #1f2937;
      font-size: 14px;
      line-height: 1.4;
    }

    span {
      color: #909399;
      font-size: 12px;
    }
  }

  .approval-detail-panel {
    margin-top: 18px;
    padding-top: 18px;
    border-top: 1px solid #ebeef5;
  }

  .approval-detail-header {
    display: flex;
    gap: 8px;
    margin-bottom: 14px;
  }

  .approval-content {
    margin-top: 18px;

    h3 {
      margin: 0 0 10px;
      color: #1f2937;
      font-size: 15px;
    }

    pre {
      min-height: 96px;
      padding: 12px;
      margin: 0;
      border-radius: 8px;
      background-color: #f8fafc;
      color: #374151;
      font-family: inherit;
      line-height: 1.7;
      white-space: pre-wrap;
      word-break: break-word;
    }
  }

  .approval-actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    margin-top: 18px;
  }
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 4px rgba(24, 144, 255, 0.2);
  }
  50% {
    box-shadow: 0 0 0 8px rgba(24, 144, 255, 0.1);
  }
  100% {
    box-shadow: 0 0 0 4px rgba(24, 144, 255, 0.2);
  }
}
</style>
