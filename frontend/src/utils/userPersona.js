const normalizedRoles = (userStore) => (userStore?.roles || []).map(role => String(role))

export const isDevelopmentAdmin = (userStore) => userStore?.userInfo?.username === 'admin'

export const isDirectorUser = (userStore) => {
  const position = userStore?.userInfo?.position || ''
  return position === '主任' || normalizedRoles(userStore).includes('MANAGER')
}

export const isAdministrativeUser = (userStore) => {
  const position = userStore?.userInfo?.position || ''
  return position.startsWith('行政管理')
    || normalizedRoles(userStore).some(role => role.startsWith('ADMINISTRATIVE'))
}

export const isFinanceUser = (userStore) => {
  const position = userStore?.userInfo?.position || ''
  return position.includes('财务')
    || position === '出纳'
    || normalizedRoles(userStore).some(role => ['FINANCE', 'INVOICE_PROCESSOR'].includes(role))
}

export const isDepartmentManager = (userStore) => {
  const position = userStore?.userInfo?.position || ''
  return isDirectorUser(userStore)
    || position === '主管'
    || position === '部门主管'
    || normalizedRoles(userStore).includes('DEPT_HEAD')
}
