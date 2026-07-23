export const FEE_METHOD_OPTIONS = [
  { label: '固定收费', value: 'FIXED' },
  { label: '风险收费', value: 'CONTINGENT' },
  { label: '固定+风险', value: 'BASE_PLUS_CONTINGENT' },
  { label: '其他', value: 'OTHER' }
]

const FEE_METHOD_LABELS = {
  FIXED: '固定收费',
  CONTINGENT: '风险收费',
  BASE_PLUS_CONTINGENT: '固定+风险',
  FIXED_PLUS_CONTINGENT: '固定+风险',
  OTHER: '其他',
  FREE: '免费代理',
  UNDETERMINED: '未确定',
  PERCENTAGE: '按比例收费',
  RISK: '风险收费',
  HOURLY: '计时收费',
  NEGOTIATED: '协商收费'
}

export const formatFeeMethod = (value) => FEE_METHOD_LABELS[value] || value || '-'
