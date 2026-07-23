export const CASE_TYPE_OPTIONS = [
  { label: '民事诉讼', value: 'CIVIL' },
  { label: '商事仲裁', value: 'ARBITRATION' },
  { label: '刑事', value: 'CRIMINAL' },
  { label: '行政', value: 'ADMINISTRATIVE' },
  { label: '非诉专项', value: 'NON_LITIGATION' },
  { label: '法律顾问', value: 'CONSULTANT' }
]

const roles = {
  CIVIL: [
    ['委托人', 'CLIENT'], ['原告', 'PLAINTIFF'], ['被告', 'DEFENDANT'], ['第三人', 'THIRD_PARTY'],
    ['共同原告', 'CO_PLAINTIFF'], ['共同被告', 'CO_DEFENDANT'], ['上诉人', 'APPELLANT'],
    ['被上诉人', 'APPELLEE'], ['申请人', 'APPLICANT'], ['被申请人', 'RESPONDENT']
  ],
  ARBITRATION: [
    ['委托人', 'CLIENT'], ['申请人', 'APPLICANT'], ['被申请人', 'RESPONDENT'],
    ['反请求申请人', 'COUNTERCLAIMANT'], ['反请求被申请人', 'COUNTER_RESPONDENT'], ['其他参与方', 'OTHER_PARTICIPANT']
  ],
  CRIMINAL: [
    ['委托人', 'CLIENT'], ['犯罪嫌疑人', 'SUSPECT'], ['被告人', 'DEFENDANT_CRIMINAL'],
    ['被害人', 'VICTIM'], ['近亲属', 'FAMILY_MEMBER'], ['附带民事原告', 'PLAINTIFF'], ['附带民事被告', 'DEFENDANT']
  ],
  ADMINISTRATIVE: [
    ['委托人', 'CLIENT'], ['行政相对人', 'ADMINISTRATIVE_COUNTERPART'], ['复议申请人', 'APPLICANT'],
    ['复议被申请人', 'RESPONDENT'], ['原告', 'PLAINTIFF'], ['行政机关', 'ADMINISTRATIVE_AUTHORITY'], ['第三人', 'THIRD_PARTY']
  ],
  NON_LITIGATION: [
    ['委托方', 'CLIENT'], ['目标公司', 'TARGET_COMPANY'], ['交易相对方', 'COUNTERPARTY'],
    ['投资方', 'INVESTOR'], ['融资方', 'FINANCIER'], ['债权人', 'CREDITOR'], ['债务人', 'DEBTOR'], ['合作机构', 'OTHER_PARTICIPANT']
  ],
  CONSULTANT: [
    ['顾问单位', 'CONSULTANT_UNIT'], ['服务对象', 'SERVICE_RECIPIENT'],
    ['关联公司', 'RELATED_COMPANY'], ['单项事务相对方', 'COUNTERPARTY']
  ]
}

export const CASE_TYPE_PROFILES = {
  CIVIL: {
    partyTitle: '诉讼主体及关联方',
    partyEmpty: '请添加委托人、原告、被告或其他诉讼参与人',
    reasonLabel: '案由',
    organizationLabel: '受理法院',
    externalNumberLabel: '法院案号',
    procedureLabel: '审级/程序',
    workflow: ['接洽利冲', '签约立案', '诉前准备', '立案或应诉', '举证答辩', '庭审', '裁判', '后续程序', '结案归档'],
    attentionItems: ['确认诉讼时效、管辖及送达地址', '核对保全必要性、担保方式和财产线索', '登记举证、答辩、开庭及上诉期限', '持续维护证据目录、争议焦点和庭审记录', '裁判后确认履行、上诉或执行方案']
  },
  ARBITRATION: {
    partyTitle: '仲裁主体及关联方',
    partyEmpty: '请添加申请人、被申请人或其他仲裁参与方',
    reasonLabel: '仲裁争议',
    organizationLabel: '仲裁机构',
    externalNumberLabel: '仲裁案号',
    procedureLabel: '仲裁程序',
    workflow: ['接洽利冲', '签约立案', '仲裁条款审查', '申请或答辩', '组庭', '举证', '开庭', '裁决', '执行衔接', '结案归档'],
    attentionItems: ['核验仲裁协议效力、仲裁事项和仲裁机构', '确认适用规则、仲裁地、语言及送达方式', '登记选定仲裁员、组庭、举证和开庭期限', '同步评估财产保全及法院协助事项', '裁决后确认履行、撤裁、不予执行或执行方案']
  },
  CRIMINAL: {
    partyTitle: '刑事案件相关人员',
    partyEmpty: '请添加委托人、犯罪嫌疑人、被告人或被害人',
    reasonLabel: '涉嫌罪名/事项',
    organizationLabel: '办案机关',
    externalNumberLabel: '外部案号',
    procedureLabel: '诉讼阶段',
    workflow: ['接洽利冲', '签约', '侦查与会见', '审查起诉', '阅卷', '一审', '二审或申诉', '结案归档'],
    attentionItems: ['核实委托人与当事人关系及委托手续', '持续登记羁押场所、强制措施和办案机关', '记录会见、阅卷、退补侦查和审查起诉期限', '按阶段维护辩护意见、证据线索和沟通记录', '判决送达后立即确认上诉期限和当事人意见']
  },
  ADMINISTRATIVE: {
    partyTitle: '行政程序主体',
    partyEmpty: '请添加行政相对人、行政机关或其他程序参与人',
    reasonLabel: '行政争议事项',
    organizationLabel: '复议/审理机关',
    externalNumberLabel: '外部案号',
    procedureLabel: '复议/诉讼程序',
    workflow: ['接洽利冲', '签约立案', '行政行为审查', '复议或起诉', '举证', '庭审', '裁判', '后续程序', '结案归档'],
    attentionItems: ['记录行政行为作出及送达日期', '核验复议前置、管辖和起诉期限', '明确被申请人/被告及行政行为依据', '跟踪行政机关举证期限和证据完整性', '裁判后确认履行、上诉、申诉或行政赔偿事项']
  },
  NON_LITIGATION: {
    partyTitle: '项目主体及关联方',
    partyEmpty: '请添加委托方、目标公司或交易相对方',
    reasonLabel: '项目事项',
    organizationLabel: '主管/合作机构',
    externalNumberLabel: '项目编号',
    procedureLabel: '项目阶段',
    workflow: ['接洽利冲', '签约立项', '资料收集', '调查核验', '起草或谈判', '内部复核', '成果交付', '整改跟踪', '项目归档'],
    attentionItems: ['确认项目范围、交付物、时间表和责任分工', '建立资料清单并记录来源、版本和缺失项', '识别交易前提、审批事项和重大风险', '正式交付前完成承办人及复核人双重检查', '保留反馈、定稿、签收和后续整改记录']
  },
  CONSULTANT: {
    partyTitle: '顾问服务关系',
    partyEmpty: '顾问单位将随客户选择自动加入，可补充关联公司或单项事务相对方',
    reasonLabel: '顾问事项',
    organizationLabel: '顾问单位',
    externalNumberLabel: '合同/项目编号',
    procedureLabel: '服务阶段',
    workflow: ['顾问建档', '服务计划', '需求受理', '分派办理', '审核交付', '定期报告', '续签评估', '终止或归档'],
    attentionItems: ['按顾问单位建立统一需求和交付登记', '每项新事务先识别相对方并复核利益冲突', '核对服务范围、除外事项和额外收费约定', '按响应时限分派承办人并保留审核、交付记录', '定期汇总服务台账、风险建议和续签事项']
  }
}

export const PARTY_ROLE_LABELS = Object.values(roles).flat().reduce((result, [label, value]) => {
  result[value] = label
  return result
}, {})

export const getPartyRoleOptions = caseType => (roles[caseType] || roles.CIVIL)
  .map(([label, value]) => ({ label, value }))

export const getCaseTypeLabel = value => CASE_TYPE_OPTIONS.find(item => item.value === value)?.label || value || '-'

export const getCaseTypeProfile = value => CASE_TYPE_PROFILES[value] || CASE_TYPE_PROFILES.CIVIL

export const getCaseTypeWorkflow = value => getCaseTypeProfile(value).workflow

export const normalizePartyRole = value => PARTY_ROLE_LABELS[value] || value
