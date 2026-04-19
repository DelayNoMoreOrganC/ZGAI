<template>
  <div class="case-create">
    <PageHeader title="新建案件" :show-back="true" @back="$router.back()">
      <template #extra>
        <el-button @click="handleSaveDraft">保存草稿</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          提交案件
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
                  <el-option label="民事" value="民事" />
                  <el-option label="商事" value="商事" />
                  <el-option label="仲裁" value="仲裁" />
                  <el-option label="刑事" value="刑事" />
                  <el-option label="行政" value="行政" />
                  <el-option label="非诉" value="非诉" />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="案件程序" prop="procedure">
                <el-select v-model="formData.procedure" placeholder="请选择案件程序">
                  <el-option label="一审" value="一审" />
                  <el-option label="二审" value="二审" />
                  <el-option label="再审" value="再审" />
                  <el-option label="执行" value="执行" />
                  <el-option label="其他" value="其他" />
                </el-select>
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
                  placeholder="为空时自动生成"
                  @blur="handleCheckDuplicate"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="案由" prop="caseReason">
                <el-select
                  v-model="formData.caseReason"
                  filterable
                  allow-create
                  placeholder="请选择或输入案由"
                >
                  <el-option
                    v-for="reason in caseReasonList"
                    :key="reason"
                    :label="reason"
                    :value="reason"
                  />
                </el-select>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="管辖法院" prop="court">
                <el-select
                  v-model="formData.court"
                  filterable
                  remote
                  :remote-method="searchCourt"
                  placeholder="请搜索法院"
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

            <el-col :span="8">
              <el-form-item label="立案时间" prop="filingDate">
                <el-date-picker
                  v-model="formData.filingDate"
                  type="date"
                  placeholder="选择日期"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="审限时间" prop="deadlineDate">
                <el-date-picker
                  v-model="formData.deadlineDate"
                  type="date"
                  placeholder="选择日期"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>
            </el-col>

            <el-col :span="8">
              <el-form-item label="委托时间" prop="commissionDate">
                <el-date-picker
                  v-model="formData.commissionDate"
                  type="date"
                  placeholder="选择日期"
                  value-format="YYYY-MM-DD"
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
              <el-form-item label="案件简述" prop="summary">
                <el-input
                  v-model="formData.summary"
                  type="textarea"
                  :rows="3"
                  placeholder="请输入案件简述"
                  maxlength="500"
                  show-word-limit
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="案件等级" prop="level">
                <el-radio-group v-model="formData.level">
                  <el-radio label="重要">重要</el-radio>
                  <el-radio label="一般">一般</el-radio>
                  <el-radio label="次要">次要</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="案件主办" prop="ownerId">
                <el-select
                  v-model="formData.ownerId"
                  filterable
                  placeholder="选择主办律师"
                >
                  <el-option
                    v-for="lawyer in lawyerList"
                    :key="lawyer.id"
                    :label="lawyer.name"
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
                  placeholder="选择协办律师"
                >
                  <el-option
                    v-for="lawyer in lawyerList"
                    :key="lawyer.id"
                    :label="lawyer.name"
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
                  placeholder="选择律师助理"
                >
                  <el-option
                    v-for="assistant in assistantList"
                    :key="assistant.id"
                    :label="assistant.name"
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
                    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
                  ]"
                >
                  <el-input v-model="party.phone" placeholder="请输入联系电话" />
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
              <el-form-item label="收费方式" prop="feeTypes">
                <el-checkbox-group v-model="formData.feeTypes">
                  <el-checkbox label="定额">定额</el-checkbox>
                  <el-checkbox label="风险代理">风险代理</el-checkbox>
                  <el-checkbox label="计时">计时</el-checkbox>
                  <el-checkbox label="计件">计件</el-checkbox>
                  <el-checkbox label="免费">免费</el-checkbox>
                </el-checkbox-group>
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="标的额(元)" prop="amount">
                <el-input-number
                  v-model="formData.amount"
                  :min="0"
                  :precision="2"
                  :step="1000"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>

            <el-col :span="12">
              <el-form-item label="标的物" prop="subjectMatter">
                <el-input v-model="formData.subjectMatter" placeholder="请输入标的物" />
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

            <el-col :span="24">
              <el-form-item label="收费简介" prop="feeSummary">
                <el-input
                  v-model="formData.feeSummary"
                  type="textarea"
                  :rows="2"
                  maxlength="200"
                  show-word-limit
                  placeholder="请输入收费简介"
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
          </el-row>
        </div>

        <!-- D. 应收款信息 -->
        <div class="form-section">
          <div class="section-header">
            <h3>D. 应收款信息</h3>
            <el-button type="primary" size="small" @click="handleAddReceivable">
              <el-icon><Plus /></el-icon>
              添加应收款
            </el-button>
          </div>

          <div v-if="formData.receivables.length === 0" class="empty-tip">
            <el-empty description="暂无应收款，请添加" />
          </div>

          <div v-for="(receivable, index) in formData.receivables" :key="index" class="receivable-item">
            <el-row :gutter="20">
              <el-col :span="6">
                <el-form-item
                  label="款项名称"
                  :prop="`receivables.${index}.name`"
                  :rules="{ required: true, message: '请输入款项名称', trigger: 'blur' }"
                >
                  <el-input v-model="receivable.name" placeholder="请输入款项名称" />
                </el-form-item>
              </el-col>

              <el-col :span="6">
                <el-form-item
                  label="应收金额(元)"
                  :prop="`receivables.${index}.amount`"
                  :rules="{ required: true, message: '请输入应收金额', trigger: 'blur' }"
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
                  label="约定收款日期"
                  :prop="`receivables.${index}.dueDate`"
                  :rules="{ required: true, message: '请选择收款日期', trigger: 'change' }"
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
        <div class="form-section">
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, MagicStick, DocumentCopy, Delete, UploadFilled
} from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import AIDocumentFill from '@/components/AIDocumentFill.vue'
import { createCase, checkDuplicate } from '@/api/case'
import { searchClients } from '@/api/client'
import { useSubmitForm } from '@/composables/useSubmitForm'

const router = useRouter()
const formRef = ref(null)
const aiFillDialogVisible = ref(false)
const duplicateDialogVisible = ref(false)
const duplicateCases = ref([])

// 转换formData为后端DTO格式
const transformToRequest = () => {
  // 转换收费方式数组为字符串
  const feeMethodMap = {
    '固定收费': 'FIXED',
    '按比例收费': 'PERCENTAGE',
    '风险代理': 'CONTINGENT',
    '计时收费': 'HOURLY',
    '协商收费': 'NEGOTIATED'
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
    '被上诉人': 'APPELLEE'
  }

  // 将中文feeTypes转换为英文feeMethod
  let feeMethod = null
  if (formData.feeTypes && formData.feeTypes.length > 0) {
    feeMethod = formData.feeTypes.map(type => feeMethodMap[type] || type).join(',')
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
    filingDate: formData.filingDate || null,
    deadlineDate: formData.deadlineDate || null,
    commissionDate: formData.commissionDate || null,
    tags: formData.tags?.join(',') || null,
    summary: formData.summary,
    level: formData.level === '重要' ? 'IMPORTANT' : formData.level === '次要' ? 'MINOR' : 'GENERAL',
    ownerId: formData.ownerId,
    coOwnerIds: formData.coOwners || [],
    assistantIds: formData.assistants || [],

    // 律师费信息（映射到后端字段）
    amount: formData.amount || null,
    attorneyFee: formData.lawyerFee || null,
    feeMethod: feeMethod,
    feeDescription: formData.feeSummary || null,
    feeNotes: formData.feeRemark || null,

    // B. 当事人
    parties: parties,

    // D. 应收款
    receivables: formData.receivables || [],

    // E. 关联信息
    clientIds: formData.relatedClients?.map(c => c.id || c) || [],
    relatedCaseIds: formData.relatedCases?.map(c => c.id || c) || []
  }
}

// 使用表单防重复提交hook
const { submitting, canSubmit, handleSubmit: handleFormSubmit } = useSubmitForm(
  async () => {
    await formRef.value?.validate()
    const requestData = transformToRequest()
    await createCase(requestData)
    router.push('/case/list')
  },
  {
    successMessage: '案件创建成功',
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
      return true
    }
  }
)

// 表单数据
const formData = reactive({
  // A. 基本信息
  caseType: '',
  procedure: '',
  caseName: '',
  caseNumber: '',
  caseReason: '',
  court: '',
  filingDate: '',
  deadlineDate: '',
  commissionDate: '',
  tags: [],
  summary: '',
  level: '一般',
  ownerId: '',
  coOwners: [],
  assistants: [],

  // B. 当事人
  parties: [],

  // C. 代理律师费
  feeTypes: [],
  amount: null,
  subjectMatter: '',
  lawyerFee: null,
  feeSummary: '',
  feeRemark: '',

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
  procedure: [{ required: true, message: '请选择案件程序', trigger: 'change' }],
  caseName: [{ required: true, message: '请输入案件名称', trigger: 'blur' }],
  caseReason: [{ required: true, message: '请选择案由', trigger: 'change' }],
  court: [{ required: true, message: '请选择管辖法院', trigger: 'change' }],
  level: [{ required: true, message: '请选择案件等级', trigger: 'change' }],
  ownerId: [{ required: true, message: '请选择主办律师', trigger: 'change' }],
  feeTypes: [
    {
      type: 'array',
      required: true,
      message: '请选择收费方式',
      trigger: 'change'
    }
  ],
  lawyerFee: [{ required: true, message: '请输入代理费', trigger: 'blur' }]
}

// 预置数据
const caseReasonList = ref([
  '买卖合同纠纷', '借款合同纠纷', '租赁合同纠纷', '劳动争议',
  '机动车交通事故责任纠纷', '离婚纠纷', '继承纠纷', '侵权责任纠纷'
])

const commonTags = ref(['紧急', 'VIP客户', '群体性案件', '媒体关注', '复杂案件'])

const lawyerList = ref([
  { id: 1, name: '张律师' },
  { id: 2, name: '李律师' },
  { id: 3, name: '王律师' }
])

const assistantList = ref([
  { id: 4, name: '小张' },
  { id: 5, name: '小李' }
])

const courtList = ref([])
const clientList = ref([])
const caseOptions = ref([])

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
  courtList.value = majorCourts.filter(court => court.includes(query))
}

// 搜索客户
const searchClient = async (query) => {
  if (!query) return
  try {
    const response = await searchClients(query)
    if (response.success) {
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
    name: '',
    amount: null,
    dueDate: '',
    remark: ''
  })
}

// 删除应收款
const handleDeleteReceivable = (index) => {
  formData.receivables.splice(index, 1)
}

// 保存草稿
const handleSaveDraft = () => {
  try {
    // 保存草稿到localStorage
    const draftData = {
      formData: JSON.parse(JSON.stringify(formData)),
      savedAt: new Date().toISOString()
    }

    localStorage.setItem('case_draft', JSON.stringify(draftData))

    ElMessage.success('草稿已保存')
  } catch (error) {
    console.error('保存草稿失败:', error)
    ElMessage.error('保存草稿失败')
  }
}

// 提交表单 - 使用防重复提交hook
const handleSubmit = () => {
  handleFormSubmit()
}

onMounted(() => {
  // 设置默认主办律师为当前用户
  // formData.ownerId = getCurrentUserId()
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
