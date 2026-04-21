// 批量操作 - 案件列表页
import { batchCloseCases, batchArchiveCases, batchDeleteCases, batchChangeOwner } from '@/api/case'

// 选择变化
const handleSelectionChange = (selection) => {
  selectedCases.value = selection
}

// 批量操作处理
const handleBatchAction = async (command) => {
  if (selectedCases.value.length === 0) {
    ElMessage.warning('请先选择案件')
    return
  }

  const caseIds = selectedCases.value.map(c => c.id)

  try {
    switch (command) {
      case 'close':
        await handleBatchClose(caseIds)
        break
      case 'archive':
        await handleBatchArchive(caseIds)
        break
      case 'changeOwner':
        await handleBatchChangeOwner(caseIds)
        break
      case 'delete':
        await handleBatchDelete(caseIds)
        break
    }
    // 清空选择
    selectedCases.value = []
    // 刷新列表
    fetchCaseList()
  } catch (error) {
    console.error('Batch operation failed:', error)
    ElMessage.error(error.response?.data?.message || '操作失败')
  }
}

// 批量结案
const handleBatchClose = async (caseIds) => {
  try {
    await ElMessageBox.confirm(
      `确定要批量结案这 ${caseIds.length} 个案件吗？`,
      '批量结案',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await batchCloseCases(caseIds)
    ElMessage.success(`已批量结案 ${caseIds.length} 个案件`)
  } catch (error) {
    if (error !== 'cancel') {
      throw error
    }
  }
}

// 批量归档
const handleBatchArchive = async (caseIds) => {
  try {
    await ElMessageBox.confirm(
      `确定要批量归档这 ${caseIds.length} 个案件吗？`,
      '批量归档',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    await batchArchiveCases(caseIds)
    ElMessage.success(`已批量归档 ${caseIds.length} 个案件`)
  } catch (error) {
    if (error !== 'cancel') {
      throw error
    }
  }
}

// 批量修改主办律师
const handleBatchChangeOwner = async (caseIds) => {
  try {
    const { value: ownerId } = await ElMessageBox.prompt(
      '请输入新的主办律师ID',
      '批量修改主办律师',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /^[0-9]+$/,
        inputErrorMessage: '请输入有效的用户ID'
      }
    )

    if (!ownerId) return

    await batchChangeOwner(caseIds, parseInt(ownerId))
    ElMessage.success(`已批量修改 ${caseIds.length} 个案件的主办律师`)
  } catch (error) {
    if (error !== 'cancel') {
      throw error
    }
  }
}

// 批量删除
const handleBatchDelete = async (caseIds) => {
  try {
    await ElMessageBox.confirm(
      `确定要批量删除这 ${caseIds.length} 个案件吗？此操作不可恢复！`,
      '批量删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'error',
        confirmButtonClass: 'el-button--danger'
      }
    )

    await batchDeleteCases(caseIds)
    ElMessage.success(`已批量删除 ${caseIds.length} 个案件`)
  } catch (error) {
    if (error !== 'cancel') {
      throw error
    }
  }
}
