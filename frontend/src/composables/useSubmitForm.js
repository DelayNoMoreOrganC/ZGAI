import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * 表单防重复提交hook
 * @param {Function} submitFn - 提交函数
 * @param {Object} options - 配置选项
 * @returns {Object} - 返回提交相关的状态和方法
 */
export function useSubmitForm(submitFn, options = {}) {
  const {
    // 提交成功消息
    successMessage = '操作成功',
    // 提交失败消息
    errorMessage = '操作失败',
    // 确认提示
    confirmMessage = null,
    // 是否显示成功消息
    showSuccessMessage = true,
    // 是否显示失败消息
    showErrorMessage = true,
    // 提交前回调
    beforeSubmit = null,
    // 提交后回调
    afterSubmit = null,
    // 错误回调
    onError = null
  } = options

  // 提交状态
  const submitting = ref(false)

  // 是否可以提交
  const canSubmit = computed(() => !submitting.value)

  // 提交表单
  const handleSubmit = async (...args) => {
    // 检查是否正在提交
    if (submitting.value) {
      ElMessage.warning('请勿重复提交')
      return false
    }

    // 确认提示
    if (confirmMessage) {
      try {
        await ElMessageBox.confirm(confirmMessage, '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
      } catch {
        return false
      }
    }

    try {
      // 设置提交状态
      submitting.value = true

      // 执行提交前回调
      if (beforeSubmit && typeof beforeSubmit === 'function') {
        const beforeResult = await beforeSubmit(...args)
        if (beforeResult === false) {
          return false
        }
      }

      // 执行提交函数
      const result = await submitFn(...args)

      // 显示成功消息
      if (showSuccessMessage) {
        ElMessage.success(typeof successMessage === 'function' ? successMessage(result) : successMessage)
      }

      // 执行提交后回调
      if (afterSubmit && typeof afterSubmit === 'function') {
        await afterSubmit(result, ...args)
      }

      return result
    } catch (error) {
      console.error('表单提交失败:', error)

      // 显示错误消息
      if (showErrorMessage) {
        const message = error?.response?.data?.message || error?.message || errorMessage
        ElMessage.error(message)
      }

      // 执行错误回调
      if (onError && typeof onError === 'function') {
        onError(error, ...args)
      }

      throw error
    } finally {
      // 重置提交状态
      submitting.value = false
    }
  }

  // 重置提交状态
  const resetSubmit = () => {
    submitting.value = false
  }

  return {
    submitting,
    canSubmit,
    handleSubmit,
    resetSubmit
  }
}

/**
 * 批量操作的防重复提交hook
 * @param {Function} submitFn - 批量操作函数
 * @param {Object} options - 配置选项
 * @returns {Object} - 返回批量操作相关的状态和方法
 */
export function useBatchSubmit(submitFn, options = {}) {
  const {
    successMessage = '批量操作成功',
    errorMessage = '批量操作失败',
    confirmMessage = '确定要对选中的项目执行此操作吗？',
    // 是否需要选择
    requireSelection = true
  } = options

  // 选择的项目
  const selectedItems = ref([])

  // 提交状态
  const submitting = ref(false)

  // 是否可以提交
  const canSubmit = computed(() => !submitting.value && (!requireSelection || selectedItems.value.length > 0))

  // 处理选择变化
  const handleSelectionChange = (selection) => {
    selectedItems.value = selection
  }

  // 批量提交
  const handleBatchSubmit = async (...args) => {
    // 检查是否选择了项目
    if (requireSelection && selectedItems.value.length === 0) {
      ElMessage.warning('请至少选择一个项目')
      return false
    }

    // 使用基础的useSubmitForm逻辑
    const { handleSubmit } = useSubmitForm(
      () => submitFn(selectedItems.value, ...args),
      { successMessage, errorMessage, confirmMessage }
    )

    return handleSubmit()
  }

  return {
    selectedItems,
    submitting,
    canSubmit,
    handleSelectionChange,
    handleBatchSubmit
  }
}
