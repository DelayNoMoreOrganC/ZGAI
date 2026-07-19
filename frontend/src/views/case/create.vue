<template>
  <div class="case-create">
    <PageHeader title="新建案件" :show-back="true" @back="$router.back()">
      <template #extra>
        <el-button v-if="isEditMode" type="primary" :loading="submitting" @click="handleSubmit">
          保存修改
        </el-button>
        <el-button v-else type="primary" :loading="approving" @click="handleSubmitApproval">
          提交立案申请
        </el-button>
      </template>
    </PageHeader>

    <div class="create-container">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
        class="case-form"
      >
        <!-- A. 基本信息 -->
        <div class="form-section">
          <div class="section-header">
            <h3>A. 基本信息</h3>
            <el-button type="primary" size="small" @click="handleAIFill">
              <el-icon><MagicStick /></el-icon>
              AI智能填充
            </el-button>
          </div>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="案件类型" prop="caseType">
                <el-select v-model="formData.caseType" placeholder="请选择案件类型">
                  <el-option label="民事" value="CIVIL" />
                  <el-option label="刑事" value="CRIMINAL" />
                  <el-option label="行政" value="ADMINISTRATIVE" />
                  <el-option label="非诉" value="NON_LITIGATION" />
                  <el-option label="顾问" value="CONSULTANT" />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="收案日期" prop="acceptanceDate">
                <el-date-picker
                  v-model="formData.acceptanceDate"
                  type="date"
                  placeholder="选择收案日期"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="案件名称" prop="caseName">
                <el-input
                  v-model="formData.caseName"
                  placeholder="为空时根据当事人生成"
                  maxlength="100"
                  show-word-limit
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="案件编号" prop="caseNumber">
                <el-input
                  v-model="formData.caseNumber"
                  placeholder="行政审批通过后自动生成"
                  disabled
                  @blur="handleCheckDuplicate"
                />
              </el-form-item>
            </el-col>

            <el-col v-if="formData.caseType === 'CRIMINAL'" :span="12">
              <el-form-item label="犯罪嫌疑人" prop="suspectName">
                <el-input v-model="formData.suspectName" placeholder="请输入犯罪嫌疑人" maxlength="100" />
              </el-form-item>
            </el-col>

            <el-col v-if="formData.caseType === 'NON_LITIGATION'" :span="12">
              <el-form-item label="涉案主体/标的物" prop="subjectMatter">
                <el-input v-model="formData.subjectMatter" placeholder="请输入涉案主体或标的物" maxlength="100" />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="案由" prop="caseReason">
                <el-input
                  v-model="formData.caseReason"
                  placeholder="请自由填写案由，如：买卖合同纠纷"
                  maxlength="80"
                  show-word-limit
                />
                <div class="reason-hints">
                  <el-tag
                    v-for="reason in currentReasonHints"
                    :key="reason"
                    size="small"
                    effect="plain"
                    @click="formData.caseReason = reason"
                  >
                    {{ reason }}
                  </el-tag>
                </div>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="业务类型" prop="businessType">
                <el-select v-model="formData.businessType" placeholder="请选择业务类型" filterable>
                  <el-option
                    v-for="type in currentBusinessTypes"
                    :key="type"
                    :label="type"
                    :value="type"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col v-if="showAgencyType" :span="12">
              <el-form-item label="代理类型" prop="agencyType">
                <el-select v-model="formData.agencyType" placeholder="请选择代理类型">
                  <el-option label="原告" value="原告" />
                  <el-option label="被告" value="被告" />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col v-if="showTrialStages" :span="24">
              <el-form-item label="审级" prop="trialStages">
                <el-select v-model="formData.trialStages" multiple placeholder="请选择审级" style="width: 100%">
                  <el-option
                    v-for="stage in currentTrialStages"
                    :key="stage"
                    :label="stage"
                    :value="stage"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col v-if="showCourtFields" :span="12">
              <el-form-item :label="trialOrganizationLabel" prop="court">
                <el-select
                  v-model="formData.court"
                  filterable
                  remote
                  allow-create
                  default-first-option
                  :remote-method="searchCourt"
                  :placeholder="trialOrganizationPlaceholder"
                >
                  <el-option
                    v-for="court in courtList"
                    :key="court"
                    :label="court"
                    :value="court"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col v-if="showCourtFields" :span="12">
              <el-form-item :label="caseNumberLabel" prop="courtCaseNumber">
                <el-input v-model="formData.courtCaseNumber" :placeholder="caseNumberPlaceholder" />
              </el-form-item>
            </el-col>

            <el-col v-if="showCourtFields" :span="12">
              <el-form-item label="开庭日期" prop="hearingDate">
                <el-date-picker
                  v-model="formData.hearingDate"
                  type="date"
                  placeholder="选择开庭日期"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col v-if="formData.caseType === 'CONSULTANT'" :span="12">
              <el-form-item label="服务开始时间" prop="serviceStartDate">
                <el-date-picker
                  v-model="formData.serviceStartDate"
                  type="date"
                  placeholder="选择开始日期"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col v-if="formData.caseType === 'CONSULTANT'" :span="12">
              <el-form-item label="服务结束时间" prop="serviceEndDate">
                <el-date-picker
                  v-model="formData.serviceEndDate"
                  type="date"
                  placeholder="选择结束日期"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col :span="24">
              <el-form-item label="案件标签" prop="tags">
                <el-select
                  v-model="formData.tags"
                  multiple
                  filterable
                  allow-create
                  placeholder="请选择或创建标签"
                >
                  <el-option
                    v-for="tag in commonTags"
                    :key="tag"
                    :label="tag"
                    :value="tag"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="24">
              <el-form-item label="案情简介" prop="summary">
                <el-input
                  v-model="formData.summary"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入案情简介"
                  maxlength="500"
                  show-word-limit
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="结案/归档">
                <el-checkbox v-model="showArchiveInfo">填写结案或归档信息</el-checkbox>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="案件主办" prop="ownerId">
                <el-select
                  v-model="formData.ownerId"
                  filterable
                  placeholder="搜索并选择案件主办"
                  style="width: 100%"
                >
                  <el-option
                    v-for="lawyer in lawyerList"
                    :key="lawyer.id"
                    :label="lawyer.label"
                    :value="lawyer.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="协办律师" prop="coOwners">
                <el-select
                  v-model="formData.coOwners"
                  multiple
                  filterable
                  placeholder="搜索并选择协办人员"
                  style="width: 100%"
                >
                  <el-option
                    v-for="lawyer in lawyerList"
                    :key="lawyer.id"
                    :label="lawyer.label"
                    :value="lawyer.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="律师助理" prop="assistants">
                <el-select
                  v-model="formData.assistants"
                  multiple
                  filterable
                  placeholder="搜索并选择实习律师或助理"
                  style="width: 100%"
                >
                  <el-option
                    v-for="assistant in assistantList"
                    :key="assistant.id"
                    :label="assistant.label"
                    :value="assistant.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- B. 当事人及关联方 -->
        <div class="form-section">
          <div class="section-header">
            <h3>B. 当事人及关联方</h3>
            <el-button type="primary" size="small" @click="handleAddParty">
              <el-icon><Plus /></el-icon>
              添加当事人
            </el-button>
          </div>

          <div v-if="formData.parties.length === 0" class="empty-tip">
            <el-empty description="暂无当事人，请添加" />
          </div>

          <div v-for="(party, index) in formData.parties" :key="index" class="party-item">
            <div class="party-header">
              <span>当事人 #{{ index + 1 }}</span>
              <div>
                <el-button text size="small" @click="handleCopyParty(index)">
                  <el-icon><DocumentCopy /></el-icon>
                  复制
                </el-button>
                <el-button text type="danger" size="small" @click="handleDeleteParty(index)">
                  <el-icon><Delete /></el-icon>
                  删除
                </el-button>
              </div>
            </div>

            <el-row :gutter="20">
              <el-col :span="8">
                <el-form-item
                  label="类型"
                  :prop="`parties.${index}.type`"
                  :rules="{ required: true, message: '请选择类型', trigger: 'change' }"
                >
                  <el-radio-group v-model="party.type">
                    <el-radio label="个人">个人</el-radio>
                    <el-radio label="单位">单位</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>

              <el-col :span="8">
                <el-form-item
                  label="委托方"
                  :prop="`parties.${index}.isClient`"
                  :rules="{ type: 'boolean', required: true, message: '请选择是否委托方', trigger: 'change' }"
                >
                  <el-switch v-model="party.isClient" />
                </el-form-item>
              </el-col>

              <el-col :span="8">
                <el-form-item
                  label="属性"
                  :prop="`parties.${index}.attribute`"
                  :rules="{ required: true, message: '请选择属性', trigger: 'change' }"
                >
                  <el-select v-model="party.attribute" placeholder="请选择属性">
                    <el-option label="原告" value="原告" />
                    <el-option label="被告" value="被告" />
                    <el-option label="第三人" value="第三人" />
                    <el-option label="共同原告" value="共同原告" />
                    <el-option label="共同被告" value="共同被告" />
                    <el-option label="申请人" value="申请人" />
                    <el-option label="被申请人" value="被申请人" />
                    <el-option label="上诉人" value="上诉人" />
                    <el-option label="被上诉人" value="被上诉人" />
                    <el-option label="管理人" value="管理人" />
                    <el-option label="债权人" value="债权人" />
                  </el-select>
                </el-form-item>
              </el-col>

              <el-col :span="12">
                <el-form-item
                  :label="party.type === '个人' ? '姓名' : '单位名称'"
                  :prop="`parties.${index}.name`"
                  :rules="[
                    { required: true, message: `请输入${party.type === '个人' ? '姓名' : '单位名称'}`, trigger: 'blur' },
                    { min: 2, max: 50, message: '长度在2-50个字符', trigger: 'blur' }
                  ]"
                >
                  <el-select
                    v-model="party.name"
                    filterable
                    allow-create
                    remote
                    :remote-method="searchClient"
                    placeholder="可从客户库选择"
                  >
                    <el-option
                      v-for="client in clientList"
                      :key="client"
                      :label="client"
                      :value="client"
                    />
                  </el-select>
                </el-form-item>
              </el-col>

              <el-col :span="12">
                <el-form-item
                  label="联系电话"
                  :prop="`parties.${index}.phone`"
                  :rules="[
                    { pattern: /^(1[3-9]\d{9}|0\d{2,3}-?\d{7,8})(-\d{1,6})?$/, message: '请输入正确的手机号或区号+固话', trigger: 'blur' }
                  ]"
                >
                  <el-input v-model="party.phone" placeholder="请输入手机号或区号+固话" />
                </el-form-item>
              </el-col>

              <!-- 个人类型额外字段 -->
              <template v-if="party.type === '个人'">
                <el-col :span="12">
                  <el-form-item
                    label="身份证号"
                    :prop="`parties.${index}.idCard`"
                    :rules="[
                      { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '请输入正确的身份证号码', trigger: 'blur' }
                    ]"
                  >
                    <el-input v-model="party.idCard" placeholder="请输入身份证号" />
                  </el-form-item>
                </el-col>

                <el-col :span="6">
                  <el-form-item label="性别" :prop="`parties.${index}.gender`">
                    <el-radio-group v-model="party.gender">
                      <el-radio label="男">男</el-radio>
                      <el-radio label="女">女</el-radio>
                    </el-radio-group>
                  </el-form-item>
                </el-col>

                <el-col :span="6">
                  <el-form-item label="民族" :prop="`parties.${index}.nation`">
                    <el-select v-model="party.nation" placeholder="请选择">
                      <el-option label="汉族" value="汉族" />
                      <el-option label="少数民族" value="少数民族" />
                    </el-select>
                  </el-form-item>
                </el-col>

                <el-col :span="24">
                  <el-form-item label="住址" :prop="`parties.${index}.address`">
                    <el-input v-model="party.address" placeholder="请输入住址" />
                  </el-form-item>
                </el-col>
              </template>

              <!-- 单位类型额外字段 -->
              <template v-if="party.type === '单位'">
                <el-col :span="12">
                  <el-form-item
                    label="信用代码"
                    :prop="`parties.${index}.creditCode`"
                    :rules="[
                      { pattern: /^[0-9A-HJ-NPQRTUWXY]{2}\d{6}[0-9A-HJ-NPQRTUWXY]{10}$/, message: '请输入正确的统一社会信用代码', trigger: 'blur' }
                    ]"
                  >
                    <el-input v-model="party.creditCode" placeholder="请输入统一社会信用代码" />
                  </el-form-item>
                </el-col>

                <el-col :span="12">
                  <el-form-item label="法定代表人" :prop="`parties.${index}.legalRep`">
                    <el-input v-model="party.legalRep" placeholder="请输入法定代表人" />
                  </el-form-item>
                </el-col>

                <el-col :span="24">
                  <el-form-item label="地址" :prop="`parties.${index}.address`">
                    <el-input v-model="party.address" placeholder="请输入单位地址" />
                  </el-form-item>
                </el-col>
              </template>

              <el-col :span="12">
                <el-form-item label="代理律师" :prop="`parties.${index}.opposingLawyer`">
                  <el-input v-model="party.opposingLawyer" placeholder="对方律师信息" />
                </el-form-item>
              </el-col>

              <el-col :span="24">
                <el-form-item label="备注" :prop="`parties.${index}.remark`">
                  <el-input
                    v-model="party.remark"
                    type="textarea"
                    :rows="2"
                    placeholder="请输入备注"
                  />
                </el-form-item>
              </el-col>

              <el-col :span="24">
                <el-form-item>
                  <el-checkbox v-model="party.syncToClient">
                    同步创建到客户库
                  </el-checkbox>
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </div>

        <!-- C. 代理律师费 -->
        <div class="form-section">
          <div class="section-header">
            <h3>C. 代理律师费</h3>
          </div>

          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="收费方式" prop="feeMethod">
                <el-radio-group v-model="formData.feeMethod">
                  <el-radio
                    v-for="method in currentFeeMethods"
                    :key="method"
                    :label="method"
                  >
                    {{ method }}
                  </el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="涉案标的(万元)" prop="amount">
                <el-input-number
                  v-model="formData.amount"
                  :min="0"
                  :precision="2"
                  :step="10"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="代理费(元)" prop="lawyerFee">
                <el-input-number
                  v-model="formData.lawyerFee"
                  :min="0"
                  :precision="2"
                  :step="100"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col v-if="isRiskFee" :span="12">
              <el-form-item label="风险费用(元)" prop="riskFee">
                <el-input-number
                  v-model="formData.riskFee"
                  :min="0"
                  :precision="2"
                  :step="100"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col v-if="isRiskFee" :span="12">
              <el-form-item label="风险比例(%)" prop="riskRatio">
                <el-input-number
                  v-model="formData.riskRatio"
                  :min="0"
                  :max="18"
                  :precision="2"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col v-if="formData.feeMethod === '免费代理'" :span="24">
              <el-form-item label="免费理由" prop="freeReason">
                <el-input
                  v-model="formData.freeReason"
                  type="textarea"
                  :rows="2"
                  maxlength="300"
                  show-word-limit
                  placeholder="请输入免费代理申请理由"
                />
              </el-form-item>
            </el-col>

            <el-col :span="24">
              <el-form-item label="收费备注" prop="feeRemark">
                <el-input
                  v-model="formData.feeRemark"
                  type="textarea"
                  :rows="3"
                  maxlength="250"
                  show-word-limit
                  placeholder="请输入收费备注"
                />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="案源比例(%)" prop="allocation.sourceRatio">
                <el-input-number
                  v-model="formData.allocation.sourceRatio"
                  :min="0"
                  :max="100"
                  :precision="2"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="部门比例(%)" prop="allocation.departmentRatio">
                <el-input-number
                  v-model="formData.allocation.departmentRatio"
                  :min="0"
                  :max="100"
                  :precision="2"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="律所比例(%)" prop="allocation.firmRatio">
                <el-input-number
                  v-model="formData.allocation.firmRatio"
                  :min="0"
                  :max="100"
                  :precision="2"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- D. 收费情况 -->
        <div class="form-section">
          <div class="section-header">
            <h3>D. 收费情况</h3>
            <el-button type="primary" size="small" @click="handleAddReceivable">
              <el-icon><Plus /></el-icon>
              添加收费记录
            </el-button>
          </div>

          <div v-if="formData.receivables.length === 0" class="empty-tip">
            <el-empty description="暂无收费记录，请添加" />
          </div>

          <div v-for="(receivable, index) in formData.receivables" :key="index" class="receivable-item">
            <el-row :gutter="20">
              <el-col :span="6">
                <el-form-item
                  label="回款金额(元)"
                  :prop="`receivables.${index}.amount`"
                  :rules="{ required: true, message: '请输入回款金额', trigger: 'blur' }"
                >
                  <el-input-number
                    v-model="receivable.amount"
                    :min="0"
                    :precision="2"
                    controls-position="right"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>

              <el-col :span="8">
                <el-form-item
                  label="收费日期"
                  :prop="`receivables.${index}.dueDate`"
                  :rules="{ required: true, message: '请选择收费日期', trigger: 'change' }"
                >
                  <el-date-picker
                    v-model="receivable.dueDate"
                    type="date"
                    placeholder="选择日期"
                    value-format="YYYY-MM-DD"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>

              <el-col :span="5">
                <el-form-item label="发票号码" :prop="`receivables.${index}.invoiceNumber`">
                  <el-input v-model="receivable.invoiceNumber" placeholder="请输入发票号码" />
                </el-form-item>
              </el-col>

              <el-col :span="5">
                <el-form-item label="开票日期" :prop="`receivables.${index}.invoiceDate`">
                  <el-date-picker
                    v-model="receivable.invoiceDate"
                    type="date"
                    placeholder="选择日期"
                    value-format="YYYY-MM-DD"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>

              <el-col :span="20">
                <el-form-item label="备注" :prop="`receivables.${index}.notes`">
                  <el-input v-model="receivable.notes" placeholder="请输入备注" />
                </el-form-item>
              </el-col>

              <el-col :span="4">
                <el-form-item label="操作">
                  <el-button type="danger" text @click="handleDeleteReceivable(index)">
                    删除
                  </el-button>
                </el-form-item>
              </el-col>
            </el-row>
          </div>
        </div>

        <!-- E. 结案/归档信息 -->
        <div class="form-section" v-if="showArchiveInfo">
          <div class="section-header">
            <h3>E. 结案/归档信息</h3>
          </div>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="结案状态" prop="closeStatus">
                <el-select v-model="formData.closeStatus" placeholder="请选择结案状态" clearable>
                  <el-option label="达成诉求" value="达成诉求" />
                  <el-option label="部分达成" value="部分达成" />
                  <el-option label="未达成" value="未达成" />
                  <el-option label="未委托" value="未委托" />
                  <el-option label="终止" value="终止" />
                  <el-option label="其他" value="其他" />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="结案日期" prop="closeDate">
                <el-date-picker
                  v-model="formData.closeDate"
                  type="date"
                  placeholder="选择结案日期"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="归档日期" prop="archiveDate">
                <el-date-picker
                  v-model="formData.archiveDate"
                  type="date"
                  placeholder="选择归档日期"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="档案保管地" prop="archiveLocation">
                <el-input
                  v-model="formData.archiveLocation"
                  placeholder="请输入档案保管地点"
                  maxlength="200"
                  show-word-limit
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- F. 关联信息 -->
        <div class="form-section">
          <div class="section-header">
            <h3>F. 关联信息</h3>
          </div>

          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="关联客户" prop="relatedClients">
                <el-select
                  v-model="formData.relatedClients"
                  multiple
                  filterable
                  placeholder="从客户库选择"
                >
                  <el-option
                    v-for="client in clientList"
                    :key="client"
                    :label="client"
                    :value="client"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="24">
              <el-form-item label="关联案件" prop="relatedCases">
                <el-select
                  v-model="formData.relatedCases"
                  multiple
                  filterable
                  placeholder="从案件库选择"
                >
                  <el-option
                    v-for="caseItem in caseOptions"
                    :key="caseItem.id"
                    :label="caseItem.name"
                    :value="caseItem.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="24">
              <el-form-item label="关联项目" prop="relatedProject">
                <el-input v-model="formData.relatedProject" placeholder="请输入关联项目" />
              </el-form-item>
            </el-col>

            <el-col :span="24">
              <el-form-item label="备注" prop="remark">
                <el-input
                  v-model="formData.remark"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入备注信息"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>
      </el-form>
    </div>

    <!-- AI智能填充组件 -->
    <AIDocumentFill
      v-model="aiFillDialogVisible"
      @confirm="handleAIFillConfirm"
    />

    <!-- 查重对话框 -->
    <el-dialog v-model="duplicateDialogVisible" title="疑似重复案件" width="800px">
      <el-table :data="duplicateCases" border>
        <el-table-column prop="caseName" label="案件名称" />
        <el-table-column prop="caseNumber" label="案号" />
        <el-table-column prop="court" label="法院" />
        <el-table-column prop="ownerName" label="主办律师" />
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleViewDuplicateCase(row)">
              查看
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, MagicStick, DocumentCopy, Delete
} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import AIDocumentFill from '@/components/AIDocumentFill.vue'
import { createCase, updateCase, checkDuplicate, getCaseDetail } from '@/api/case'
import { searchClients } from '@/api/client'
import { getUserList } from '@/api/user'
import { useSubmitForm } from '@/composables/useSubmitForm'

const router = useRouter()
const route = useRoute()
const formRef = ref(null)
const aiFillDialogVisible = ref(false)
const duplicateDialogVisible = ref(false)
const duplicateCases = ref([])

// 判断是否为编辑模式
const isEditMode = computed(() => !!route.params.id)
const caseId = computed(() => route.params.id)

// 转换formData为后端DTO格式
const transformToRequest = () => {
  const feeMethodMap = {
    '固定收费': 'FIXED',
    '固定': 'FIXED',
    '风险收费': 'CONTINGENT',
    '固定+风险': 'BASE_PLUS_CONTINGENT',
    '基础+风险': 'BASE_PLUS_CONTINGENT',
    '免费代理': 'FREE',
    '未确定': 'UNDETERMINED',
    '其他': 'OTHER'
  }

  // 转换当事人类型和属性
  const partyTypeMap = {
    '个人': 'INDIVIDUAL',
    '单位': 'ORGANIZATION'
  }

  const partyRoleMap = {
    '原告': 'PLAINTIFF',
    '被告': 'DEFENDANT',
    '申请人': 'APPLICANT',
    '被申请人': 'RESPONDENT',
    '第三人': 'THIRD_PARTY',
    '上诉人': 'APPELLANT',
    '被上诉人': 'APPELLEE',
    '共同原告': 'CO_PLAINTIFF',
    '共同被告': 'CO_DEFENDANT',
    '管理人': 'ADMINISTRATOR',
    '债权人': 'CREDITOR'
  }

  // 转换当事人数据
  const parties = (formData.parties || []).map(party => ({
    id: party.id || null,
    partyType: partyTypeMap[party.type] || 'INDIVIDUAL',
    partyRole: partyRoleMap[party.attribute] || party.attribute,
    name: party.name || '',
    isClient: party.isClient || false,
    syncToClient: party.syncToClient || false,
    gender: party.gender || null,
    ethnicity: party.nation || null,
    idCard: party.idCard || null,
    creditCode: party.creditCode || null,
    phone: party.phone || null,
    address: party.address || null,
    legalRepresentative: party.legalRep || null,
    opposingLawyer: party.opposingLawyer || null,
    notes: party.remark || null
  }))

  return {
    // A. 基本信息
    caseType: formData.caseType,
    procedure: formData.procedure,
    caseName: formData.caseName,
    caseNumber: formData.caseNumber,
    caseReason: formData.caseReason,
    court: formData.court,
    acceptanceDate: formData.acceptanceDate || null,
    filingDate: formData.filingDate || null,
    deadlineDate: formData.deadlineDate || null,
    commissionDate: formData.commissionDate || null,
    suspectName: formData.suspectName || null,
    subjectMatter: formData.subjectMatter || null,
    businessType: formData.businessType || null,
    agencyType: formData.agencyType || null,
    serviceStartDate: formData.serviceStartDate || null,
    serviceEndDate: formData.serviceEndDate || null,
    trialStages: formData.trialStages?.join(',') || null,
    courtCaseNumber: formData.courtCaseNumber || null,
    hearingDate: formData.hearingDate || null,
    tags: formData.tags?.join(',') || null,
    summary: formData.summary,
    ownerId: formData.ownerId,
    coOwnerIds: formData.coOwners || [],
    assistantIds: formData.assistants || [],

    // 律师费信息（映射到后端字段）
    amount: formData.amount || null,
    attorneyFee: formData.lawyerFee || null,
    feeMethod: feeMethodMap[formData.feeMethod] || formData.feeMethod || null,
    feeNotes: formData.feeRemark || null,
    riskRatio: formData.riskRatio || null,
    riskFee: formData.riskFee || null,
    freeReason: formData.freeReason || null,
    allocationJson: JSON.stringify(formData.allocation || {}),

    // B. 当事人
    parties: parties,

    // D. 收费情况
    receivables: (formData.receivables || []).map(item => ({
      name: '回款',
      amount: item.amount,
      dueDate: item.dueDate,
      notes: [
        item.invoiceNumber ? `发票号码：${item.invoiceNumber}` : '',
        item.invoiceDate ? `开票日期：${item.invoiceDate}` : '',
        item.notes || ''
      ].filter(Boolean).join('；')
    })),

    // E. 关联信息
    clientIds: formData.relatedClients?.map(c => c.id || c) || [],
    relatedCaseIds: formData.relatedCases?.map(c => c.id || c) || []
  }
}

// 提交审批状态
const approving = ref(false)

// 使用表单防重复提交hook
const { submitting, canSubmit, handleSubmit: handleFormSubmit } = useSubmitForm(
  async () => {
    await formRef.value?.validate()
    if (!validateFilingBusinessRules()) return
    const requestData = transformToRequest()

    // 根据是否为编辑模式调用不同API
    if (isEditMode.value) {
      await updateCase(caseId.value, requestData)
    } else {
      await createCase(requestData)
    }

    router.push('/case/list')
  },
  {
    get successMessage() {
      return isEditMode.value ? '案件更新成功' : '案件创建成功'
    },
    confirmMessage: null,
    beforeSubmit: async () => {
      // 验证至少有一个当事人
      if (!formData.parties || formData.parties.length === 0) {
        ElMessage.warning('请至少添加一个当事人')
        return false
      }

      // 验证当事人必填字段
      for (let i = 0; i < formData.parties.length; i++) {
        const party = formData.parties[i]
        if (!party.type) {
          ElMessage.warning(`第${i + 1}个当事人：请选择类型`)
          return false
        }
        if (!party.name || party.name.trim() === '') {
          ElMessage.warning(`第${i + 1}个当事人：请输入${party.type === '个人' ? '姓名' : '单位名称'}`)
          return false
        }
        if (!party.attribute) {
          ElMessage.warning(`第${i + 1}个当事人：请选择属性`)
          return false
        }
      }
      return validateFilingBusinessRules()
    }
  }
)

// 是否显示结案/归档信息
const showArchiveInfo = ref(false)

// 表单数据
const formData = reactive({
  // A. 基本信息
  caseType: '',
  procedure: 'FILING_REVIEW',
  caseName: '',
  caseNumber: '',
  caseReason: '',
  court: '',
  acceptanceDate: new Date().toISOString().slice(0, 10),
  filingDate: '',
  deadlineDate: '',
  commissionDate: '',
  suspectName: '',
  subjectMatter: '',
  businessType: '',
  agencyType: '',
  serviceStartDate: '',
  serviceEndDate: '',
  trialStages: [],
  courtCaseNumber: '',
  hearingDate: '',
  tags: [],
  summary: '',
  ownerId: '',
  coOwners: [],
  assistants: [],

  // B. 当事人
  parties: [],

  // C. 代理律师费
  feeMethod: '',
  amount: null,
  lawyerFee: null,
  riskFee: null,
  riskRatio: null,
  freeReason: '',
  feeRemark: '',

  allocation: {
    sourceRatio: null,
    departmentRatio: null,
    firmRatio: null
  },

  // D. 应收款
  receivables: [],

  // E. 关联信息
  relatedClients: [],
  relatedCases: [],
  relatedProject: '',
  remark: '',

  // F. 结案/归档信息
  closeStatus: '',
  closeDate: '',
  archiveDate: '',
  archiveLocation: ''
})

// 表单验证规则
const formRules = {
  caseType: [{ required: true, message: '请选择案件类型', trigger: 'change' }],
  acceptanceDate: [{ required: true, message: '请选择收案日期', trigger: 'change' }],
  caseReason: [{ required: true, message: '请填写案由', trigger: 'blur' }],
  businessType: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
  ownerId: [{ required: true, message: '请选择主办律师', trigger: 'change' }],
  feeMethod: [{ required: true, message: '请选择收费方式', trigger: 'change' }],
  lawyerFee: [],
  suspectName: [{ required: true, message: '请输入犯罪嫌疑人', trigger: 'blur' }],
  serviceStartDate: [{ required: true, message: '请选择合同服务开始时间', trigger: 'change' }],
  serviceEndDate: [{ required: true, message: '请选择合同服务结束时间', trigger: 'change' }],
  freeReason: [{ required: true, message: '请输入免费代理申请理由', trigger: 'blur' }],
  riskRatio: []
}

// 预置数据
const businessTypeOptions = {
  CIVIL: ['婚姻家庭', '公司', '金融', '证券', '保险', '海事海商', '建设工程', '劳动', '知识产权', '破产与重组', '医疗纠纷', '其他'],
  CRIMINAL: ['一般代理', '当事人自行委托', '法律援助', '法定通知辩护', '扩大通知辩护', '刑事附带民事诉讼'],
  ADMINISTRATIVE: ['一般代理/应诉', '行政申诉'],
  NON_LITIGATION: ['公司', '金融', '证券', '保险', '反垄断', '建设工程与房地产', '劳动', '知识产权', '税法', '海事海商', '环境资源与能源', '破产与重组', '其他', '代书与咨询'],
  CONSULTANT: ['常年法律顾问', '专项法律顾问']
}

const trialStageOptions = {
  CIVIL: ['仲裁', '一审', '二审', '执行', '再审', '重审一审', '重审二审', '特别程序', '破产程序'],
  CRIMINAL: ['侦查', '审查起诉', '一审', '二审', '申诉', '再审', '重审一审', '重审二审'],
  ADMINISTRATIVE: ['行政复议', '行政裁决', '一审', '二审', '执行', '再审', '重审一审', '重审二审']
}

const feeMethodOptions = {
  CIVIL: ['固定收费', '风险收费', '固定+风险', '其他', '免费代理', '未确定'],
  CRIMINAL: ['固定收费', '其他', '免费代理', '未确定'],
  ADMINISTRATIVE: ['固定收费', '其他', '免费代理', '未确定'],
  NON_LITIGATION: ['固定收费', '风险收费', '固定+风险', '其他', '免费代理', '未确定'],
  CONSULTANT: ['固定收费', '风险收费', '固定+风险', '其他', '免费代理', '未确定']
}

const caseReasonIndex = {
  CIVIL: ['人格权纠纷', '婚姻家庭纠纷', '继承纠纷', '物权纠纷', '机动车交通事故责任纠纷', '医疗损害责任纠纷'],
  CRIMINAL: ['诈骗罪', '合同诈骗罪', '职务侵占罪', '非法吸收公众存款罪', '故意伤害罪', '危险驾驶罪'],
  ADMINISTRATIVE: ['行政处罚纠纷', '行政许可纠纷', '行政强制纠纷', '政府信息公开纠纷', '行政赔偿纠纷'],
  NON_LITIGATION: ['专项法律服务', '尽职调查', '法律意见书', '合规审查', '破产重整专项'],
  CONSULTANT: ['常年法律顾问', '专项法律顾问']
}

const currentReasonHints = computed(() => caseReasonIndex[formData.caseType] || [])
const currentBusinessTypes = computed(() => businessTypeOptions[formData.caseType] || [])
const currentTrialStages = computed(() => trialStageOptions[formData.caseType] || [])
const currentFeeMethods = computed(() => feeMethodOptions[formData.caseType] || [])
const showTrialStages = computed(() => ['CIVIL', 'CRIMINAL', 'ADMINISTRATIVE'].includes(formData.caseType))
const showCourtFields = computed(() => ['CIVIL', 'CRIMINAL', 'ADMINISTRATIVE'].includes(formData.caseType))
const showAgencyType = computed(() => formData.caseType === 'CRIMINAL' && formData.businessType === '刑事附带民事诉讼')
const isArbitrationCase = computed(() => formData.trialStages?.includes('仲裁'))
const trialOrganizationLabel = computed(() => isArbitrationCase.value ? '审理机构' : '受理法院')
const trialOrganizationPlaceholder = computed(() => isArbitrationCase.value ? '请输入或选择仲裁委员会/审理机构' : '请输入或搜索法院')
const caseNumberLabel = computed(() => isArbitrationCase.value ? '仲裁案号' : '法院案号')
const caseNumberPlaceholder = computed(() => isArbitrationCase.value ? '请输入仲裁案号' : '请输入法院案号')
const isRiskFee = computed(() => ['风险收费', '固定+风险', '基础+风险'].includes(formData.feeMethod))

watch(() => formData.caseType, () => {
  formData.businessType = ''
  formData.trialStages = []
  formData.feeMethod = ''
  formData.agencyType = ''
})

watch(() => formData.feeMethod, () => {
  if (!isRiskFee.value) {
    formData.riskRatio = null
    formData.riskFee = null
  }
  if (formData.feeMethod !== '免费代理') {
    formData.freeReason = ''
  }
})

const validateFilingBusinessRules = () => {
  if (formData.caseType === 'CRIMINAL' && !formData.suspectName?.trim()) {
    ElMessage.warning('刑事案件请填写犯罪嫌疑人')
    return false
  }
  if (formData.caseType === 'CONSULTANT') {
    if (!formData.serviceStartDate || !formData.serviceEndDate) {
      ElMessage.warning('顾问案件请填写合同服务开始和结束时间')
      return false
    }
    if (new Date(formData.serviceStartDate) > new Date(formData.serviceEndDate)) {
      ElMessage.warning('合同服务开始时间不能晚于结束时间')
      return false
    }
  }
  if (showTrialStages.value && (!formData.trialStages || formData.trialStages.length === 0)) {
    ElMessage.warning('请选择审级')
    return false
  }
  if (['固定收费', '固定', '基础+风险', '固定+风险'].includes(formData.feeMethod)) {
    if (!formData.lawyerFee || formData.lawyerFee <= 0) {
      ElMessage.warning('固定收费或固定+风险案件请填写代理金额')
      return false
    }
  }
  if (isRiskFee.value) {
    const hasRiskFee = formData.riskFee !== null && formData.riskFee !== undefined && Number(formData.riskFee) > 0
    const hasRiskRatio = formData.riskRatio !== null && formData.riskRatio !== undefined && Number(formData.riskRatio) > 0
    if (!hasRiskFee && !hasRiskRatio) {
      ElMessage.warning('风险收费案件请填写风险费用或风险比例')
      return false
    }
    if (hasRiskRatio && Number(formData.riskRatio) > 18) {
      ElMessage.warning('请检查风险代理收费比例是否合规，风险比例不得超过18%')
      return false
    }
  }
  if (formData.feeMethod === '免费代理' && !formData.freeReason?.trim()) {
    ElMessage.warning('免费代理案件请填写免费理由')
    return false
  }
  const sourceRatio = Number(formData.allocation.sourceRatio || 0)
  const departmentRatio = Number(formData.allocation.departmentRatio || 0)
  const firmRatio = Number(formData.allocation.firmRatio || 0)
  const total = sourceRatio + departmentRatio + firmRatio
  if (total !== 100) {
    ElMessage.warning('分配情况中：案源比例 + 部门比例 + 律所比例必须等于100%')
    return false
  }
  return true
}

const commonTags = ref(['紧急', 'VIP客户', '法援', '涉黑恶', '无罪辩护', '重大疑难案件', '群体性案件', '媒体关注'])

const userOptions = ref([])
const caseHandlerPositions = ['主任', '部门主管', '合伙人', '律师', '实习律师', '助理', '律师助理']
const assistantPositions = ['实习律师', '助理', '律师助理']

const normalizeUserOption = (user) => ({
  id: user.id,
  name: user.realName || user.username,
  label: `${user.realName || user.username}${user.position ? `（${user.position}）` : ''}${user.departmentName ? ` - ${user.departmentName}` : ''}`,
  position: user.position || '',
  departmentName: user.departmentName || ''
})

const isCaseHandler = (user) => {
  const position = user.position || ''
  return caseHandlerPositions.some(item => position.includes(item))
}

const isAssistant = (user) => {
  const position = user.position || ''
  return assistantPositions.some(item => position.includes(item))
}

const lawyerList = computed(() => userOptions.value.filter(isCaseHandler))
const assistantList = computed(() => userOptions.value.filter(isAssistant))

const courtList = ref([])
const clientList = ref([])
const caseOptions = ref([])

const loadUserOptions = async () => {
  try {
    const response = await getUserList({ page: 0, size: 300, status: 1 })
    const pageData = response.data || {}
    const users = pageData.content || pageData.records || pageData || []
    userOptions.value = users
      .filter(user => user.status === undefined || user.status === 1)
      .map(normalizeUserOption)
  } catch (error) {
    console.error('加载承办人员失败:', error)
    userOptions.value = []
    ElMessage.error('加载承办人员失败，请检查员工账号数据')
  }
}

const isSuccessResponse = (response) => response?.success || response?.code === 200

// 搜索法院
const searchCourt = async (query) => {
  if (!query) return
  // 使用全国主要法院数据库
  const majorCourts = [
    '北京市朝阳区人民法院', '北京市海淀区人民法院', '北京市东城区人民法院', '北京市西城区人民法院',
    '上海市浦东新区人民法院', '上海市黄浦区人民法院', '上海市徐汇区人民法院', '上海市静安区人民法院',
    '广州市越秀区人民法院', '广州市天河区人民法院', '广州市海珠区人民法院', '广州市白云区人民法院',
    '深圳市福田区人民法院', '深圳市罗湖区人民法院', '深圳市南山区人民法院', '深圳市宝安区人民法院',
    '杭州市西湖区人民法院', '杭州市上城区人民法院', '杭州市下城区人民法院', '杭州市江干区人民法院',
    '南京市鼓楼区人民法院', '南京市玄武区人民法院', '南京市秦淮区人民法院', '南京市建邺区人民法院',
    '成都市武侯区人民法院', '成都市锦江区人民法院', '成都市青羊区人民法院', '成都市金牛区人民法院',
    '武汉市江汉区人民法院', '武汉市武昌区人民法院', '武汉市洪山区人民法院', '武汉市汉阳区人民法院',
    '西安市雁塔区人民法院', '西安市碑林区人民法院', '西安市莲湖区人民法院', '西安市新城人民法院',
    '重庆市渝中区人民法院', '重庆市江北区人民法院', '重庆市南岸区人民法院', '重庆市九龙坡区人民法院',
    '天津市和平区人民法院', '天津市河西区人民法院', '天津市南开区人民法院', '天津市河北区人民法院',
    '苏州市姑苏区人民法院', '苏州市虎丘区人民法院', '苏州市吴中区人民法院', '苏州市相城区人民法院',
    '青岛市市南区人民法院', '青岛市市北区人民法院', '青岛市崂山区人民法院', '青岛市李沧区人民法院',
    '大连市中山区人民法院', '大连市西岗区人民法院', '大连市沙河口区人民法院', '大连市甘井子区人民法院',
    '厦门市思明区人民法院', '厦门市湖里区人民法院', '厦门市海沧区人民法院', '厦门市集美区人民法院',
    '长沙市岳麓区人民法院', '长沙市芙蓉区人民法院', '长沙市天心区人民法院', '长沙市开福区人民法院',
    '济南市历下区人民法院', '济南市市中区人民法院', '济南市槐荫区人民法院', '济南市天桥区人民法院',
    '沈阳市和平区人民法院', '沈阳市沈河区人民法院', '沈阳市大东区人民法院', '沈阳市铁西区人民法院',
    '哈尔滨市南岗区人民法院', '哈尔滨市道里区人民法院', '哈尔滨市道外区人民法院', '哈尔滨市香坊区人民法院',
    '郑州市金水区人民法院', '郑州市中原区人民法院', '郑州市二七区人民法院', '郑州市管城回族区人民法院'
  ]
  const matched = majorCourts.filter(court => court.includes(query))
  courtList.value = matched.includes(query) ? matched : [query, ...matched]
}

// 搜索客户
const searchClient = async (query) => {
  if (!query) return
  try {
    const response = await searchClients(query)
    if (isSuccessResponse(response)) {
      clientList.value = response.data.map(client => client.name || client.clientName)
    }
  } catch (error) {
    console.error('搜索客户失败:', error)
    // 降级到本地搜索
    clientList.value = [
      '张三',
      '李四',
      '某某科技有限公司'
    ].filter(client => client.includes(query))
  }
}

// 查重
const handleCheckDuplicate = async () => {
  if (!formData.caseName && !formData.caseNumber) return

  try {
    const res = await checkDuplicate({
      name: formData.caseName,
      caseNumber: formData.caseNumber
    })

    if (res.data && res.data.length > 0) {
      duplicateCases.value = res.data
      duplicateDialogVisible.value = true
    }
  } catch (error) {
    console.error('查重失败:', error)
  }
}

// AI智能填充
const handleAIFill = () => {
  aiFillDialogVisible.value = true
}

// 处理AI智能填充确认
const handleAIFillConfirm = (result) => {
  if (!result) return

  // 填充基本信息
  if (result.caseNumber) formData.caseNumber = result.caseNumber
  if (result.courtName) formData.court = result.courtName
  if (result.caseReason) formData.caseReason = result.caseReason
  if (result.hearingDate) {
    // 如果识别到开庭时间，自动设置为审限时间
    formData.deadlineDate = result.hearingDate
  }

  // 填充当事人信息
  if (result.plaintiffName || result.defendantName) {
    const parties = []

    if (result.plaintiffName) {
      parties.push({
        type: '个人',
        name: result.plaintiffName,
        isClient: false,
        attribute: '原告',
        phone: result.contactPhone || '',
        idCard: '',
        gender: '',
        nation: '',
        address: '',
        creditCode: '',
        legalRep: '',
        opposingLawyer: '',
        remark: '',
        syncToClient: false
      })
    }

    if (result.defendantName) {
      parties.push({
        type: '个人',
        name: result.defendantName,
        isClient: false,
        attribute: '被告',
        phone: result.contactPhone || '',
        idCard: '',
        gender: '',
        nation: '',
        address: '',
        creditCode: '',
        legalRep: '',
        opposingLawyer: '',
        remark: '',
        syncToClient: false
      })
    }

    if (parties.length > 0) {
      formData.parties = parties
    }
  }

  // 自动生成案件名称（如果为空）
  if (!formData.caseName && result.plaintiffName && result.defendantName) {
    formData.caseName = `${result.plaintiffName} Vs ${result.defendantName}`
  }

  ElMessage.success('信息已填充到表单，请核对后提交')
}

// 添加当事人
const handleAddParty = () => {
  formData.parties.push({
    type: '个人',
    name: '',
    isClient: false,
    attribute: '',
    phone: '',
    idCard: '',
    gender: '',
    nation: '',
    address: '',
    creditCode: '',
    legalRep: '',
    opposingLawyer: '',
    remark: '',
    syncToClient: false
  })
}

// 复制当事人
const handleCopyParty = (index) => {
  const party = formData.parties[index]
  formData.parties.splice(index + 1, 0, { ...party })
}

// 删除当事人
const handleDeleteParty = (index) => {
  formData.parties.splice(index, 1)
}

// 添加应收款
const handleAddReceivable = () => {
  formData.receivables.push({
    amount: null,
    dueDate: '',
    invoiceNumber: '',
    invoiceDate: '',
    notes: ''
  })
}

// 删除应收款
const handleDeleteReceivable = (index) => {
  formData.receivables.splice(index, 1)
}

// 保存草稿 - 真正保存到后端数据库
const handleSaveDraft = async () => {
  try {
    // 验证基本必填项（比正式立案宽松）
    if (!formData.caseName || formData.caseName.trim() === '') {
      ElMessage.warning('请输入案件名称')
      return
    }
    if (!formData.caseType) {
      ElMessage.warning('请选择案件类型')
      return
    }

    ElMessageBox.confirm(
      '保存草稿将创建案件，状态为"咨询中"，是否继续？',
      '保存草稿',
      {
        confirmButtonText: '保存',
        cancelButtonText: '取消',
        type: 'info'
      }
    ).then(async () => {
      try {
        const requestData = transformToRequest()

        // 创建草稿案件（咨询状态）
        const response = await createCase(requestData)
        const caseData = response.data || response
        const caseId = caseData.id || caseData.data?.id

        if (!caseId) {
          throw new Error('保存草稿失败：未获取到案件ID')
        }

        ElMessage.success('草稿已保存到数据库')
        localStorage.removeItem('case_draft') // 清除本地草稿

        // 跳转到案件详情页
        setTimeout(() => {
          router.push({ name: 'CaseDetail', params: { id: caseId } })
        }, 1000)

      } catch (error) {
        console.error('保存草稿失败:', error)
        ElMessage.error('保存草稿失败：' + (error.message || '未知错误'))
      }
    }).catch(() => {
      // 用户取消
    })

  } catch (error) {
    console.error('保存草稿失败:', error)
    ElMessage.error('保存草稿失败')
  }
}

// 提交表单 - 使用防重复提交hook
const handleSubmit = () => {
  handleFormSubmit()
}

// 提交审批 - 保存案件后跳转到审批页面
const handleSubmitApproval = async () => {
  try {
    approving.value = true

    // 验证表单
    await formRef.value?.validate()

    // 验证至少有一个当事人
    if (!formData.parties || formData.parties.length === 0) {
      ElMessage.warning('请至少添加一个当事人')
      return
    }
    if (!validateFilingBusinessRules()) {
      return
    }

    const requestData = transformToRequest()

    // 创建案件后，后端会自动生成立案审批单并流转至行政管理
    let savedCaseId
    if (isEditMode.value) {
      await updateCase(caseId.value, requestData)
      savedCaseId = caseId.value
    } else {
      const response = await createCase(requestData)
      const caseData = response.data || response
      savedCaseId = caseData.id || caseData.data?.id
      if (!savedCaseId) {
        throw new Error('创建案件失败：未获取到案件ID')
      }
    }

    ElMessage.success('立案申请已提交，案件状态为待审批')
    router.push('/case/list')

  } catch (error) {
    console.error('提交立案申请失败:', error)
    ElMessage.error('提交立案申请失败：' + (error.message || '未知错误'))
  } finally {
    approving.value = false
  }
}

onMounted(async () => {
  await loadUserOptions()

  // 如果是编辑模式，加载案件数据
  if (isEditMode.value) {
    try {
      const response = await getCaseDetail(caseId.value)
      const caseData = response.data

      // 将后端数据转换为表单数据
      formData.caseType = caseData.caseType || ''
      formData.procedure = caseData.procedure || ''
      formData.caseName = caseData.caseName || ''
      formData.caseNumber = caseData.caseNumber || ''
      formData.caseReason = caseData.caseReason || ''
      formData.court = caseData.court || ''
      formData.acceptanceDate = caseData.acceptanceDate || new Date().toISOString().slice(0, 10)
      formData.suspectName = caseData.suspectName || ''
      formData.subjectMatter = caseData.subjectMatter || ''
      formData.businessType = caseData.businessType || ''
      formData.agencyType = caseData.agencyType || ''
      formData.serviceStartDate = caseData.serviceStartDate || ''
      formData.serviceEndDate = caseData.serviceEndDate || ''
      formData.trialStages = caseData.trialStages ? caseData.trialStages.split(',').filter(Boolean) : []
      formData.courtCaseNumber = caseData.courtCaseNumber || ''
      formData.hearingDate = caseData.hearingDate || ''
      formData.amount = caseData.amount || null
      formData.lawyerFee = caseData.attorneyFee || null
      const feeMethodReverseMap = {
        FIXED: '固定收费',
        CONTINGENT: '风险收费',
        BASE_PLUS_CONTINGENT: '固定+风险',
        FIXED_PLUS_CONTINGENT: '固定+风险',
        FREE: '免费代理',
        UNDETERMINED: '未确定',
        OTHER: '其他'
      }
      formData.feeMethod = feeMethodReverseMap[caseData.feeMethod] || caseData.feeMethod || ''
      formData.riskRatio = caseData.riskRatio || null
      formData.riskFee = caseData.riskFee || null
      formData.freeReason = caseData.freeReason || ''
      formData.feeRemark = caseData.feeNotes || ''
      if (caseData.allocationJson) {
        try {
          formData.allocation = JSON.parse(caseData.allocationJson)
        } catch (error) {
          console.warn('分配情况解析失败:', error)
        }
      }
      formData.filingDate = caseData.filingDate || null
      formData.deadlineDate = caseData.deadlineDate || null
      formData.summary = caseData.summary || ''

      // 当事人数据转换
      if (caseData.parties && caseData.parties.length > 0) {
        formData.parties = caseData.parties.map(p => ({
          id: p.id,
          type: p.partyType === 'INDIVIDUAL' ? '个人' : '单位',
          attribute: p.partyRole,
          name: p.name,
          phone: p.phone,
          address: p.address
        }))
      }

      // 应收款数据
      if (caseData.receivables && caseData.receivables.length > 0) {
        formData.receivables = caseData.receivables
      }

      // 关联客户和案件
      if (caseData.relatedClients && caseData.relatedClients.length > 0) {
        formData.relatedClients = caseData.relatedClients
      }
      if (caseData.relatedCases && caseData.relatedCases.length > 0) {
        formData.relatedCases = caseData.relatedCases
      }

      // 结案/归档信息
      if (caseData.closeDate || caseData.archiveDate) {
        showArchiveInfo.value = true
        formData.closeDate = caseData.closeDate || null
        formData.archiveDate = caseData.archiveDate || null
      }
    } catch (error) {
      ElMessage.error('加载案件数据失败')
      router.push('/case/list')
    }
  }
})
</script>

<style scoped lang="scss">
.case-create {
  .create-container {
    background-color: #fff;
    padding: 30px;
    border-radius: 4px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }

  .form-section {
    margin-bottom: 40px;
    padding-bottom: 30px;
    border-bottom: 1px dashed #e4e7ed;

    &:last-child {
      border-bottom: none;
    }

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 20px;

      h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 500;
        color: #333;
        border-left: 4px solid #1890ff;
        padding-left: 12px;
      }
    }

    .party-item,
    .receivable-item {
      background-color: #fafafa;
      padding: 20px;
      border-radius: 4px;
      margin-bottom: 15px;
      border: 1px solid #e4e7ed;

      .party-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 15px;
        padding-bottom: 10px;
        border-bottom: 1px solid #e4e7ed;
        font-weight: 500;
        color: #333;
      }

      &:last-child {
        margin-bottom: 0;
      }
    }

    .empty-tip {
      text-align: center;
      padding: 20px;
      background-color: #fafafa;
      border-radius: 4px;
    }
  }

  :deep(.el-form-item__label) {
    font-weight: 400;
    color: #606266;
  }

  :deep(.el-input-number) {
    width: 100%;
  }

  .upload-demo {
    margin-bottom: 20px;
  }

  .ai-result {
    margin-top: 20px;
    padding: 15px;
    background-color: #f5f7fa;
    border-radius: 4px;

    h4 {
      margin: 0 0 10px;
      font-size: 14px;
      color: #333;
    }

    pre {
      margin: 0;
      font-size: 12px;
      color: #666;
      white-space: pre-wrap;
      word-wrap: break-word;
    }
  }
}
</style>
