<template>
  <div class="case-basic">
    <div class="basic-content">
      <!-- 区块1: 案件信息 -->
      <div class="info-section">
        <div class="section-title">
          <h3>案件信息</h3>
          <el-button v-if="canEdit" text type="primary" size="small" @click="handleEditSection('info')">
            编辑
          </el-button>
        </div>
        <el-descriptions :column="basicColumns" border>
          <el-descriptions-item label="案件类型">
            <el-tag :type="getTypeTagType(caseData.caseType)">
              {{ getCaseTypeLabel(caseData.caseType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item :label="currentCaseProfile.procedureLabel">
            {{ procedureDisplay }}
          </el-descriptions-item>
          <el-descriptions-item :label="currentCaseProfile.reasonLabel">
            {{ caseData.caseReason }}
          </el-descriptions-item>
          <el-descriptions-item label="案件编号">
            <span data-testid="case-number">{{ caseData.caseNumber }}</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="caseData.caseType !== 'CONSULTANT'" :label="currentCaseProfile.organizationLabel">
            {{ caseData.court }}
          </el-descriptions-item>
          <el-descriptions-item label="委托客户">
            <el-tag v-if="primaryClientName" type="success" size="small">{{ primaryClientName }}</el-tag>
            <span v-else class="text-muted">未关联客户</span>
          </el-descriptions-item>
          <el-descriptions-item label="立案时间">
            <span v-if="caseData.filingDate" data-testid="case-filing-date">{{ formatDate(caseData.filingDate) }}</span>
            <span v-else class="text-muted">审批通过后生成</span>
          </el-descriptions-item>
          <el-descriptions-item label="审限时间">
            <span :class="getDeadlineClass(caseData.deadlineDate)">
              {{ formatDate(caseData.deadlineDate) }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="委托时间">
            {{ formatDate(caseData.commissionDate) }}
          </el-descriptions-item>
          <el-descriptions-item label="胜诉金额" :span="1">
            ¥{{ formatMoney(caseData.wonAmount ?? caseData.winAmount) }}
          </el-descriptions-item>
          <el-descriptions-item label="实际回款" :span="1">
            ¥{{ formatMoney(caseData.actualReceived ?? caseData.actualAmount) }}
          </el-descriptions-item>
          <el-descriptions-item label="案件标签" :span="1">
            <el-tag
              v-for="tag in displayTags"
              :key="tag"
              size="small"
              style="margin-right: 5px"
            >
              {{ tag }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="案件简述" :span="3">
            {{ caseData.summary || '暂无简述' }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <div class="info-section conflict-status-section">
        <div class="section-title">
          <h3>立案利冲审查</h3>
          <el-tag v-if="caseData.conflictChecks?.length" type="info">
            {{ caseData.conflictChecks.length }} 个委托方
          </el-tag>
        </div>
        <el-empty
          v-if="!caseData.conflictChecks?.length"
          description="该案件尚未关联结构化利冲记录"
          :image-size="64"
        />
        <el-table v-else :data="caseData.conflictChecks" border size="small">
          <el-table-column prop="subjectName" label="检查对象" min-width="160" />
          <el-table-column prop="reportNo" label="报告编号" width="190" />
          <el-table-column label="系统初筛" width="140">
            <template #default="{ row }">
              <el-tag :type="conflictRiskTagType(row.conflictLevel)" size="small">
                {{ conflictRiskLabel(row.conflictLevel) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="正式结论" min-width="180">
            <template #default="{ row }">
              <el-tag :type="conflictDecisionTagType(row.reviewDecision)" size="small">
                {{ row.reviewStatus === 'COMPLETED' ? conflictDecisionLabel(row.reviewDecision) : '待行政审查' }}
              </el-tag>
              <div v-if="row.reviewedAt" class="conflict-review-meta">
                {{ row.reviewedByName || '-' }} · {{ formatDateTime(row.reviewedAt) }}
              </div>
            </template>
          </el-table-column>
          <el-table-column label="书面依据" min-width="180">
            <template #default="{ row }">
              <div v-if="row.waiverAttachments?.length" class="conflict-attachment-list">
                <el-button
                  v-for="attachment in row.waiverAttachments"
                  :key="attachment.id"
                  link
                  type="primary"
                  size="small"
                  @click="handleDownloadWaiver(row, attachment)"
                >
                  {{ attachment.originalFileName }}
                </el-button>
              </div>
              <span v-else class="text-muted">无</span>
            </template>
          </el-table-column>
          <el-table-column label="归档" width="110" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.archivedAt" type="success" size="small">已归档</el-tag>
              <span v-else class="text-muted">未归档</span>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="info-section type-workflow-section">
        <div class="section-title">
          <h3>{{ getCaseTypeLabel(caseData.caseType) }}办理要素</h3>
        </div>
        <div class="workflow-strip">
          <span
            v-for="(step, index) in currentWorkflow"
            :key="step"
            class="workflow-step"
            :class="`is-${getWorkflowStepStatus(step)}`"
          >
            <b>{{ index + 1 }}</b>{{ step }}
          </span>
        </div>

        <el-descriptions v-if="caseData.caseType === 'CONSULTANT'" :column="compactColumns" border class="type-details">
          <el-descriptions-item label="顾问单位">{{ consultantUnitDisplayName }}</el-descriptions-item>
          <el-descriptions-item label="服务期限">{{ formatDate(caseData.serviceStartDate) }} 至 {{ formatDate(caseData.serviceEndDate) }}</el-descriptions-item>
          <el-descriptions-item label="主要联系人">{{ consultantContactSummary }}</el-descriptions-item>
          <el-descriptions-item label="续签提醒">{{ formatDate(caseData.renewalReminderDate) }}</el-descriptions-item>
          <el-descriptions-item label="服务范围" :span="2">{{ caseData.consultantServiceScope || '-' }}</el-descriptions-item>
          <el-descriptions-item label="响应约定" :span="2">{{ caseData.consultantResponseRequirement || '-' }}</el-descriptions-item>
          <el-descriptions-item label="包含服务">{{ caseData.consultantIncludedServices || '-' }}</el-descriptions-item>
          <el-descriptions-item label="除外服务">{{ caseData.consultantExcludedServices || '-' }}</el-descriptions-item>
        </el-descriptions>
        <el-descriptions v-else :column="basicColumns" border class="type-details">
          <el-descriptions-item :label="currentCaseProfile.organizationLabel">{{ caseData.court || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="currentCaseProfile.externalNumberLabel">{{ caseData.courtCaseNumber || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="currentCaseProfile.procedureLabel">{{ caseData.trialStages || procedureDisplay }}</el-descriptions-item>
          <el-descriptions-item v-if="caseData.caseType === 'CRIMINAL'" label="犯罪嫌疑人/被告人">{{ caseData.suspectName || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="caseData.caseType === 'CRIMINAL'" label="代理类型">{{ caseData.agencyType || '-' }}</el-descriptions-item>
          <el-descriptions-item v-if="caseData.caseType === 'NON_LITIGATION'" label="项目标的">{{ caseData.subjectMatter || '-' }}</el-descriptions-item>
          <el-descriptions-item label="业务类型">{{ caseData.businessType || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="attention-panel">
          <div class="attention-title">律师办理重点</div>
          <ul>
            <li v-for="item in currentCaseProfile.attentionItems" :key="item">{{ item }}</li>
          </ul>
        </div>
      </div>

      <!-- 区块2: 类型化主体及关联方 -->
      <div class="info-section">
        <div class="section-title">
          <h3>{{ currentCaseProfile.partyTitle }}</h3>
          <el-button v-if="canEdit" type="primary" size="small" @click="handleAddParty">
            <el-icon><Plus /></el-icon>
            添加当事人
          </el-button>
        </div>
        <el-empty v-if="uniqueAttributes.length === 0" :description="currentCaseProfile.partyEmpty" :image-size="64" />
        <el-tabs v-else type="card" class="party-tabs">
          <el-tab-pane
            v-for="attr in uniqueAttributes"
            :key="attr"
            :name="attr"
            :label="getAttributeLabel(attr)"
          >
            <el-table :data="getPartiesByAttribute(attr)" border>
              <el-table-column prop="name" label="姓名/单位名称" width="150" />
              <el-table-column prop="type" label="类型" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.partyType === 'INDIVIDUAL' ? 'primary' : 'success'" size="small">
                    {{ row.partyTypeDesc || row.type || row.partyType }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="phone" label="联系电话" width="130" />
              <el-table-column prop="address" label="地址" show-overflow-tooltip />
              <el-table-column prop="idCard" label="身份证号/信用代码" width="180" />
              <el-table-column prop="opposingLawyer" label="代理律师" width="120" />
              <el-table-column v-if="canEdit" label="操作" width="120" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="handleEditParty(row)">
                    编辑
                  </el-button>
                  <el-button link type="danger" size="small" @click="handleDeleteParty(row)">
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>

      <!-- 区块3: 办理人员 -->
      <div class="info-section">
        <div class="section-title">
          <h3>办理人员</h3>
          <el-button v-if="canEdit" text type="primary" size="small" @click="handleEditTeam">
            编辑团队
          </el-button>
        </div>
        <div class="team-grid">
          <div class="team-item">
            <div class="team-label">主办律师</div>
            <div class="team-value">{{ caseData.ownerName || '-' }}</div>
          </div>
          <div class="team-item">
            <div class="team-label">协办律师</div>
            <div class="team-value">
              <el-tag
                v-for="coOwner in caseData.coOwners"
                :key="coOwner.id"
                style="margin-right: 5px"
              >
                {{ coOwner.name }}
              </el-tag>
              <span v-if="!caseData.coOwners || caseData.coOwners.length === 0">-</span>
            </div>
          </div>
          <div class="team-item">
            <div class="team-label">律师助理</div>
            <div class="team-value">
              <el-tag
                v-for="assistant in caseData.assistants"
                :key="assistant.id"
                type="info"
                style="margin-right: 5px"
              >
                {{ assistant.name }}
              </el-tag>
              <span v-if="!caseData.assistants || caseData.assistants.length === 0">-</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 区块4: 代理律师费 -->
      <div class="info-section">
        <div class="section-title">
          <h3>代理律师费</h3>
          <el-button v-if="canEdit" text type="primary" size="small" @click="handleEditFee">
            编辑
          </el-button>
        </div>
        <el-descriptions :column="compactColumns" border>
          <el-descriptions-item label="收费方式">
            <el-tag
              v-for="feeType in displayFeeTypes"
              :key="feeType"
              style="margin-right: 5px"
            >
              {{ feeType }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="标的额">
            ¥{{ caseData.amount || '0' }}
          </el-descriptions-item>
          <el-descriptions-item label="标的物">
            {{ caseData.subjectMatter || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="代理费">
            <span class="fee-amount">¥{{ formatMoney(caseData.attorneyFee ?? caseData.lawyerFee) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="收费简介" :span="2">
            {{ caseData.feeDescription || caseData.feeSummary || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="收费备注" :span="2">
            {{ caseData.feeNotes || caseData.feeRemark || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="fee-payment">
          <h4>收款记录汇总</h4>
          <el-table :data="caseData.payments || []" border size="small">
            <el-table-column prop="date" label="收款日期" width="120" />
            <el-table-column prop="amount" label="金额" width="120">
              <template #default="{ row }">
                ¥{{ row.amount }}
              </template>
            </el-table-column>
            <el-table-column prop="method" label="收款方式" />
            <el-table-column prop="note" label="备注" show-overflow-tooltip />
          </el-table>
        </div>
      </div>

      <!-- 区块5: 案件程序 -->
      <div class="info-section">
        <div class="section-title">
          <h3>案件程序</h3>
          <el-button v-if="canEdit" type="primary" size="small" @click="handleAddProcedure">
            <el-icon><Plus /></el-icon>
            添加程序
          </el-button>
        </div>
        <div class="procedure-cards">
          <el-card
            v-for="(procedure, index) in caseData.procedures"
            :key="index"
            class="procedure-card"
            shadow="hover"
          >
            <template #header>
              <div class="procedure-header">
                <span>{{ procedure.name }}</span>
                <el-button v-if="canEdit" text type="primary" size="small" @click="handleEditProcedure(procedure)">
                  编辑
                </el-button>
              </div>
            </template>
            <el-descriptions :column="compactColumns" size="small">
              <el-descriptions-item label="案号">
                {{ procedure.caseNumber || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="地区">
                {{ procedure.region || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="法院">
                {{ procedure.court || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="承办人">
                {{ procedure.judge || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="立案日期">
                {{ formatDate(procedure.filingDate) }}
              </el-descriptions-item>
              <el-descriptions-item label="开庭日期">
                {{ formatDate(procedure.hearingDate) }}
              </el-descriptions-item>
              <el-descriptions-item label="裁决日期">
                {{ formatDate(procedure.judgmentDate) }}
              </el-descriptions-item>
              <el-descriptions-item label="结果">
                <el-tag :type="getJudgmentTagType(procedure.result)">
                  {{ procedure.result || '-' }}
                </el-tag>
              </el-descriptions-item>
            </el-descriptions>
            <div v-if="procedure.attachments && procedure.attachments.length > 0" class="procedure-attachments">
              <div class="attachment-label">附件：</div>
              <el-tag
                v-for="file in procedure.attachments"
                :key="file.id"
              >
                {{ file.name }}
              </el-tag>
            </div>
          </el-card>
        </div>
        <el-empty v-if="!caseData.procedures?.length" description="尚未登记案件程序" :image-size="64" />
      </div>

      <!-- 区块6: 关联案件 -->
      <div class="info-section">
        <div class="section-title">
          <h3>关联案件</h3>
        </div>
        <el-table :data="caseData.relatedCases || []" border>
          <el-table-column prop="name" label="案件名称" />
          <el-table-column prop="procedure" label="程序" width="100" />
          <el-table-column prop="court" label="法院" width="200" />
          <el-table-column prop="caseNumber" label="案号" width="180" />
          <el-table-column prop="ownerName" label="办理人" width="100" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button link type="primary" @click="handleViewRelatedCase(row)">
                查看
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!caseData.relatedCases?.length" description="暂无关联案件" :image-size="64" />
      </div>

      <!-- 区块7: 办案策略 -->
      <div class="info-section">
        <div class="section-title">
          <h3>办案策略</h3>
          <el-button v-if="canEdit" type="primary" size="small" @click="handleAddStrategy">
            <el-icon><Plus /></el-icon>
            添加策略
          </el-button>
        </div>
        <div class="strategy-list">
          <div
            v-for="(strategy, index) in caseData.strategies"
            :key="index"
            class="strategy-item"
          >
            <div class="strategy-header">
              <span class="strategy-title">{{ strategy.title }}</span>
              <div v-if="canEdit" class="strategy-actions">
                <el-button text type="primary" size="small" @click="handleEditStrategy(strategy)">
                  编辑
                </el-button>
                <el-button text type="danger" size="small" @click="handleDeleteStrategy(index)">
                  删除
                </el-button>
              </div>
            </div>
            <div class="strategy-content">{{ strategy.content }}</div>
            <div class="strategy-discussion">
              <h4>讨论区</h4>
              <div v-for="comment in strategy.comments" :key="comment.id" class="comment-item">
                <span class="comment-author">{{ comment.author }}:</span>
                <span class="comment-text">{{ comment.text }}</span>
                <span class="comment-time">{{ comment.time }}</span>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-if="!caseData.strategies?.length" description="暂无办案策略" :image-size="64" />
      </div>
    </div>

    <!-- 当事人编辑对话框 -->
    <el-dialog
      v-model="partyDialogVisible"
      :title="partyForm.id ? '编辑当事人' : '添加当事人'"
      :width="dialogWidth"
    >
      <el-form :model="partyForm" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="当事人类型" required>
              <el-select v-model="partyForm.partyType" placeholder="请选择">
                <el-option label="个人" value="INDIVIDUAL" />
                <el-option label="企业" value="ENTERPRISE" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="当事人角色" required>
              <el-select v-model="partyForm.partyRole" placeholder="请选择">
                <el-option v-for="role in currentPartyRoleOptions" :key="role.value" :label="role.label" :value="role.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="姓名/企业名称" required>
          <el-input v-model="partyForm.name" placeholder="请输入" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="partyForm.phone" placeholder="请输入" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别">
              <el-radio-group v-model="partyForm.gender">
                <el-radio label="男">男</el-radio>
                <el-radio label="女">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="身份证号">
          <el-input v-model="partyForm.idCard" placeholder="请输入身份证号" />
        </el-form-item>

        <el-form-item label="统一社会信用代码">
          <el-input v-model="partyForm.creditCode" placeholder="请输入信用代码" />
        </el-form-item>

        <el-form-item label="地址">
          <el-input v-model="partyForm.address" placeholder="请输入地址" />
        </el-form-item>

        <el-form-item label="代理律师">
          <el-input v-model="partyForm.opposingLawyer" placeholder="对方代理律师" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="partyForm.notes" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>

        <el-form-item label="同步为客户">
          <el-checkbox v-model="partyForm.isClient">将该当事人同步到客户库</el-checkbox>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="partyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveParty">保存</el-button>
      </template>
    </el-dialog>

    <!-- 团队编辑对话框 -->
    <el-dialog v-model="teamDialogVisible" title="编辑团队" :width="dialogWidth">
      <el-form :model="teamForm" label-width="100px">
        <el-form-item label="主办律师" required>
          <el-select v-model="teamForm.ownerId" placeholder="请选择主办律师" style="width: 100%">
            <el-option
              v-for="lawyer in lawyerList"
              :key="lawyer.id"
              :label="lawyer.label"
              :value="lawyer.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="协办律师">
          <el-select
            v-model="teamForm.coOwnerIds"
            multiple
            placeholder="请选择协办律师"
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

        <el-form-item label="律师助理">
          <el-select
            v-model="teamForm.assistantIds"
            multiple
            placeholder="请选择律师助理"
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
      </el-form>

      <template #footer>
        <el-button @click="teamDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveTeam">保存</el-button>
      </template>
    </el-dialog>

    <!-- 费用编辑对话框 -->
    <el-dialog v-model="feeDialogVisible" title="编辑费用信息" :width="dialogWidth">
      <el-form :model="feeForm" label-width="100px">
        <el-form-item label="律师费">
          <el-input-number
            v-model="feeForm.attorneyFee"
            :min="0"
            :precision="2"
            placeholder="请输入律师费"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="费用方式">
          <el-select v-model="feeForm.feeMethod" placeholder="请选择" style="width: 100%">
            <el-option
              v-for="option in FEE_METHOD_OPTIONS"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="案件金额">
          <el-input-number
            v-model="feeForm.amount"
            :min="0"
            :precision="2"
            placeholder="请输入案件金额"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="胜诉金额">
          <el-input-number
            v-model="feeForm.wonAmount"
            :min="0"
            :precision="2"
            placeholder="请输入胜诉金额"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="实际回款">
          <el-input-number
            v-model="feeForm.actualReceived"
            :min="0"
            :precision="2"
            placeholder="请输入实际回款"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="feeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveFee">保存</el-button>
      </template>
    </el-dialog>

    <!-- 程序编辑对话框 -->
    <el-dialog
      v-model="procedureDialogVisible"
      :title="procedureForm.id ? '编辑程序' : '添加程序'"
      :width="dialogWidth"
    >
      <el-form :model="procedureForm" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="程序名称" required>
              <el-input v-model="procedureForm.name" placeholder="如：一审、二审" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="案号">
              <el-input v-model="procedureForm.caseNumber" placeholder="请输入" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="地区">
              <el-input v-model="procedureForm.region" placeholder="如：北京市" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="法院">
              <el-input v-model="procedureForm.court" placeholder="请输入" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="承办人">
              <el-input v-model="procedureForm.judge" placeholder="法官/仲裁员" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="程序类型">
              <el-select v-model="procedureForm.procedureType" placeholder="请选择" style="width: 100%">
                <el-option label="一审" value="FIRST_INSTANCE" />
                <el-option label="二审" value="SECOND_INSTANCE" />
                <el-option label="再审" value="RETRIAL" />
                <el-option label="执行" value="EXECUTION" />
                <el-option label="仲裁" value="ARBITRATION" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="立案日期">
              <el-date-picker
                v-model="procedureForm.filingDate"
                type="date"
                placeholder="选择日期"
                style="width: 100%"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="开庭日期">
              <el-date-picker
                v-model="procedureForm.hearingDate"
                type="date"
                placeholder="选择日期"
                style="width: 100%"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="裁决日期">
              <el-date-picker
                v-model="procedureForm.judgmentDate"
                type="date"
                placeholder="选择日期"
                style="width: 100%"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注">
          <el-input v-model="procedureForm.notes" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="procedureDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveProcedure">保存</el-button>
      </template>
    </el-dialog>

    <!-- 策略编辑对话框 -->
    <el-dialog
      v-model="strategyDialogVisible"
      :title="strategyForm.index !== null ? '编辑策略' : '添加策略'"
      :width="dialogWidth"
    >
      <el-form :model="strategyForm" label-width="80px">
        <el-form-item label="策略标题" required>
          <el-input v-model="strategyForm.title" placeholder="请输入策略标题" />
        </el-form-item>

        <el-form-item label="策略内容" required>
          <el-input
            v-model="strategyForm.content"
            type="textarea"
            :rows="6"
            placeholder="请输入策略内容"
          />
        </el-form-item>

        <el-form-item label="优先级">
          <el-radio-group v-model="strategyForm.priority">
            <el-radio label="HIGH">高</el-radio>
            <el-radio label="MEDIUM">中</el-radio>
            <el-radio label="LOW">低</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="strategyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveStrategy">保存</el-button>
      </template>
    </el-dialog>

    <!-- 案件信息编辑对话框 -->
    <el-dialog v-model="infoDialogVisible" title="编辑案件信息" :width="dialogWidth">
      <el-form :model="infoForm" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="案件类型">
              <el-select v-model="infoForm.caseType" disabled style="width: 100%">
                <el-option v-for="item in CASE_TYPE_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item :label="editCaseProfile.procedureLabel">
              <el-input v-model="infoForm.procedure" placeholder="请输入当前程序或阶段" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item :label="editCaseProfile.reasonLabel">
              <el-input v-model="infoForm.caseReason" placeholder="请输入案件或项目事项" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="案号">
              <el-input v-model="infoForm.caseNumber" placeholder="请输入案号" />
            </el-form-item>
          </el-col>

          <el-col v-if="infoForm.caseType !== 'CONSULTANT'" :span="12">
            <el-form-item :label="editCaseProfile.organizationLabel">
              <el-input v-model="infoForm.court" :placeholder="`请输入${editCaseProfile.organizationLabel}`" />
            </el-form-item>
          </el-col>

          <el-col v-if="infoForm.caseType !== 'CONSULTANT'" :span="12">
            <el-form-item :label="editCaseProfile.externalNumberLabel">
              <el-input v-model="infoForm.courtCaseNumber" :placeholder="`请输入${editCaseProfile.externalNumberLabel}`" />
            </el-form-item>
          </el-col>

          <el-col v-if="infoForm.caseType !== 'CONSULTANT'" :span="12">
            <el-form-item label="业务类型">
              <el-input v-model="infoForm.businessType" placeholder="请输入业务类型" />
            </el-form-item>
          </el-col>

          <el-col v-if="['CIVIL', 'ARBITRATION', 'CRIMINAL', 'ADMINISTRATIVE'].includes(infoForm.caseType)" :span="12">
            <el-form-item :label="editCaseProfile.procedureLabel">
              <el-input v-model="infoForm.trialStages" placeholder="多个程序可用逗号分隔" />
            </el-form-item>
          </el-col>

          <el-col v-if="infoForm.caseType === 'CRIMINAL'" :span="12">
            <el-form-item label="犯罪嫌疑人/被告人">
              <el-input v-model="infoForm.suspectName" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>

          <el-col v-if="infoForm.caseType === 'CRIMINAL'" :span="12">
            <el-form-item label="代理类型">
              <el-input v-model="infoForm.agencyType" placeholder="请输入代理或辩护类型" />
            </el-form-item>
          </el-col>

          <el-col v-if="infoForm.caseType === 'NON_LITIGATION'" :span="24">
            <el-form-item label="项目标的">
              <el-input v-model="infoForm.subjectMatter" placeholder="请输入项目标的或目标" />
            </el-form-item>
          </el-col>

          <template v-if="infoForm.caseType === 'CONSULTANT'">
            <el-col :span="12">
              <el-form-item label="顾问单位" required>
                <el-input v-model="infoForm.consultantUnitName" placeholder="请输入顾问单位" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="主要联系人">
                <el-input v-model="infoForm.consultantContactName" placeholder="请输入联系人" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="服务开始" required>
                <el-date-picker v-model="infoForm.serviceStartDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="服务结束" required>
                <el-date-picker v-model="infoForm.serviceEndDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="服务范围">
                <el-input v-model="infoForm.consultantServiceScope" type="textarea" :rows="2" placeholder="请输入服务范围" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="响应约定">
                <el-input v-model="infoForm.consultantResponseRequirement" placeholder="请输入响应时间和方式" />
              </el-form-item>
            </el-col>
          </template>

          <el-col :span="12">
            <el-form-item label="审限时间">
              <el-date-picker
                v-model="infoForm.deadlineDate"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="委托时间">
              <el-date-picker
                v-model="infoForm.commissionDate"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="胜诉金额">
              <el-input-number v-model="infoForm.wonAmount" :min="0" :precision="2" placeholder="请输入胜诉金额" style="width: 100%" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="实际回款">
              <el-input-number v-model="infoForm.actualReceived" :min="0" :precision="2" placeholder="请输入实际回款" style="width: 100%" />
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="标签">
              <el-input v-model="infoForm.tags" placeholder="多个标签用逗号分隔" />
            </el-form-item>
          </el-col>

          <el-col :span="24">
            <el-form-item label="案件摘要">
              <el-input
                v-model="infoForm.summary"
                type="textarea"
                :rows="4"
                placeholder="请输入案件摘要"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <template #footer>
        <el-button @click="infoDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitInfo">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { createParty, updateParty, deleteParty, updateCase } from '@/api/case'
import { getCaseProcedures, createCaseProcedure, updateCaseProcedure, deleteCaseProcedure } from '@/api/case'
import { getUserOptions } from '@/api/user'
import { downloadConflictWaiverAttachment } from '@/api/client'
import { CASE_TYPE_OPTIONS, getCaseTypeLabel, getCaseTypeProfile, getPartyRoleOptions, PARTY_ROLE_LABELS } from '@/utils/caseTypeProfiles'
import { FEE_METHOD_OPTIONS, formatFeeMethod } from '@/utils/feeMethod'

const props = defineProps({
  caseData: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['refresh'])

const router = useRouter()
const viewportWidth = ref(window.innerWidth)
const canEdit = computed(() => props.caseData.canEdit === true)
const basicColumns = computed(() => viewportWidth.value < 760 ? 1 : 3)
const compactColumns = computed(() => viewportWidth.value < 760 ? 1 : 2)
const dialogWidth = computed(() => viewportWidth.value < 760 ? 'calc(100vw - 24px)' : '700px')
const updateViewportWidth = () => {
  viewportWidth.value = window.innerWidth
}

window.addEventListener('resize', updateViewportWidth)
onBeforeUnmount(() => window.removeEventListener('resize', updateViewportWidth))

const currentCaseProfile = computed(() => getCaseTypeProfile(props.caseData.caseType))
const currentWorkflow = computed(() => currentCaseProfile.value.workflow || [])
const currentPartyRoleOptions = computed(() => getPartyRoleOptions(props.caseData.caseType))
const consultantContactSummary = computed(() => [
  props.caseData.consultantContactName,
  props.caseData.consultantContactDepartment,
  props.caseData.consultantContactTitle,
  props.caseData.consultantContactPhone,
  props.caseData.consultantContactEmail
].filter(Boolean).join(' / ') || '-')
const consultantUnitDisplayName = computed(() => {
  if (props.caseData.consultantUnitName) return props.caseData.consultantUnitName
  const parties = props.caseData.parties || []
  const party = parties.find(item =>
    ['CONSULTANT_UNIT', '顾问单位', 'CLIENT', '委托人'].includes(item.partyRole || item.attribute)
  ) || parties.find(item => item.isClient) || parties.find(item =>
    ['ORGANIZATION', '单位'].includes(item.partyType || item.type)
  )
  return party?.name || '-'
})
const procedureDisplay = computed(() => {
  if (props.caseData.caseType === 'CONSULTANT' && props.caseData.currentStage) {
    return props.caseData.currentStage
  }
  const labels = { FILING_REVIEW: '立案审批', FILING: '立案', ACTIVE: '办理中' }
  return props.caseData.trialStages || labels[props.caseData.procedure] || props.caseData.procedure || '-'
})

const getWorkflowStepStatus = (step) => {
  const progress = (props.caseData.stageProgress || []).find(item => item.stageName === step)
  if (progress?.status === 'COMPLETED') return 'completed'
  if (progress?.status === 'IN_PROGRESS' || props.caseData.currentStage === step) return 'active'
  return 'pending'
}

// ==================== 案件信息对话框 ====================
const infoDialogVisible = ref(false)
const infoForm = ref({
  caseType: '',
  procedure: '',
  caseReason: '',
  caseNumber: '',
  court: '',
  courtCaseNumber: '',
  trialStages: '',
  businessType: '',
  suspectName: '',
  agencyType: '',
  subjectMatter: '',
  consultantUnitName: '',
  consultantContactName: '',
  serviceStartDate: '',
  serviceEndDate: '',
  consultantServiceScope: '',
  consultantResponseRequirement: '',
  deadlineDate: '',
  commissionDate: '',
  wonAmount: null,
  actualReceived: null,
  tags: [],
  summary: ''
})
const editCaseProfile = computed(() => getCaseTypeProfile(infoForm.value.caseType))

// 当事人对话框
const partyDialogVisible = ref(false)
const partyForm = ref({
  id: null,
  partyType: 'INDIVIDUAL',
  partyRole: 'PLAINTIFF',
  name: '',
  phone: '',
  gender: '男',
  idCard: '',
  creditCode: '',
  address: '',
  opposingLawyer: '',
  notes: '',
  isClient: false
})

// 团队对话框
const teamDialogVisible = ref(false)
const teamForm = ref({
  ownerId: null,
  coOwnerIds: [],
  assistantIds: []
})

const userOptions = ref([])
const caseHandlerPositions = ['主任', '部门主管', '合伙人', '律师', '实习律师', '助理', '律师助理']
const assistantPositions = ['实习律师', '助理', '律师助理']
const isPositionMatched = (user, positions) => positions.some(item => (user.position || '').includes(item))
const lawyerList = computed(() => userOptions.value.filter(user => isPositionMatched(user, caseHandlerPositions)))
const assistantList = computed(() => userOptions.value.filter(user => isPositionMatched(user, assistantPositions)))

const loadUserOptions = async () => {
  try {
    const response = await getUserOptions({ size: 300 })
    const users = response.data || []
    userOptions.value = users
      .filter(user => user.status === undefined || user.status === 1)
      .map(user => ({
        id: user.id,
        name: user.realName || user.username,
        label: `${user.realName || user.username}${user.position ? `（${user.position}）` : ''}${user.departmentName ? ` - ${user.departmentName}` : ''}`,
        position: user.position || '',
        departmentName: user.departmentName || ''
      }))
  } catch (error) {
    console.error('加载案件办理人员失败:', error)
    userOptions.value = []
    ElMessage.error('加载办理人员失败，请检查员工账号数据')
  }
}

onMounted(loadUserOptions)

// 费用对话框
const feeDialogVisible = ref(false)
const feeForm = ref({
  attorneyFee: 0,
  feeMethod: 'FIXED',
  amount: 0,
  wonAmount: 0,
  actualReceived: 0
})

// 程序对话框
const procedureDialogVisible = ref(false)
const procedureForm = ref({
  id: null,
  name: '',
  caseNumber: '',
  region: '',
  court: '',
  judge: '',
  procedureType: 'FIRST_INSTANCE',
  filingDate: '',
  hearingDate: '',
  judgmentDate: '',
  notes: ''
})

// 策略对话框
const strategyDialogVisible = ref(false)
const strategyForm = ref({
  index: null,
  title: '',
  content: '',
  priority: 'MEDIUM'
})

// 获取案件类型标签颜色
const getTypeTagType = (type) => {
  const typeMap = {
    CIVIL: 'primary',
    ARBITRATION: 'warning',
    CRIMINAL: 'danger',
    ADMINISTRATIVE: 'info',
    NON_LITIGATION: 'success',
    CONSULTANT: 'success'
  }
  return typeMap[type]
}

const normalizeDate = (value) => {
  if (!value) return ''
  if (Array.isArray(value)) {
    const [year, month, day] = value
    return [year, String(month).padStart(2, '0'), String(day).padStart(2, '0')].join('-')
  }
  return String(value).slice(0, 10)
}

const formatDate = (value) => normalizeDate(value) || '-'
const formatDateTime = (value) => {
  if (!value) return '-'
  if (Array.isArray(value)) {
    const [year, month, day, hour = 0, minute = 0] = value
    const pad = number => String(number).padStart(2, '0')
    return `${year}-${pad(month)}-${pad(day)} ${pad(hour)}:${pad(minute)}`
  }
  return String(value).replace('T', ' ').slice(0, 16)
}

const handleDownloadWaiver = async (record, attachment) => {
  try {
    const response = await downloadConflictWaiverAttachment(record.id, attachment.id)
    const url = URL.createObjectURL(response.data)
    const link = document.createElement('a')
    link.href = url
    link.download = attachment.originalFileName || '利冲豁免依据'
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error(error?.message || '下载书面依据失败')
  }
}

const conflictRiskLabel = (level) => ({
  DIRECT: '直接冲突线索', CASE_PARTY: '案件主体命中', EXISTING: '既有客户命中', RELATED: '关联主体命中',
  SIMILAR: '相似名称', NONE: '未发现线索'
}[level] || '待人工核对')
const conflictRiskTagType = (level) => ({
  DIRECT: 'danger', CASE_PARTY: 'warning', EXISTING: 'warning', RELATED: 'warning', SIMILAR: 'warning', NONE: 'success'
}[level] || 'info')
const conflictDecisionLabel = (decision) => ({
  PASSED: '无冲突，通过', REJECTED: '存在冲突，不通过', CONDITIONAL: '附条件通过'
}[decision] || '待行政审查')
const conflictDecisionTagType = (decision) => ({
  PASSED: 'success', REJECTED: 'danger', CONDITIONAL: 'warning'
}[decision] || 'info')

const formatMoney = (value) => {
  const amount = Number(value ?? 0)
  return Number.isFinite(amount) ? amount.toLocaleString('zh-CN') : '0'
}

const primaryClientName = computed(() => {
  if (props.caseData.clientName) return props.caseData.clientName
  if (props.caseData.caseType === 'CONSULTANT' && props.caseData.consultantUnitName) {
    return props.caseData.consultantUnitName
  }
  return (props.caseData.parties || []).find(p => p.isClient)?.name || ''
})

const displayTags = computed(() => {
  const tags = props.caseData.tags
  if (Array.isArray(tags)) return tags.filter(Boolean)
  if (typeof tags === 'string') {
    return tags.split(/[,，]/).map(tag => tag.trim()).filter(Boolean)
  }
  return []
})

const displayFeeTypes = computed(() => {
  if (Array.isArray(props.caseData.feeTypes)) {
    return props.caseData.feeTypes.filter(Boolean).map(formatFeeMethod)
  }
  if (props.caseData.feeMethod) return [formatFeeMethod(props.caseData.feeMethod)]
  return []
})

// 获取审限样式
const getDeadlineClass = (date) => {
  if (!date) return ''
  const days = Math.ceil((new Date(normalizeDate(date)) - new Date()) / (1000 * 60 * 60 * 24))
  if (days < 0) return 'deadline-overdue'
  if (days <= 3) return 'deadline-urgent'
  if (days <= 7) return 'deadline-warning'
  return ''
}

// 获取裁判结果标签颜色
const getJudgmentTagType = (result) => {
  if (!result) return 'info'
  if (result.includes('胜诉')) return 'success'
  if (result.includes('败诉')) return 'danger'
  if (result.includes('调解')) return 'warning'
  return 'info'
}

// 获取当事人唯一属性
const uniqueAttributes = computed(() => {
  const parties = props.caseData.parties || []
  const attributes = parties.map(p => p.partyRole || p.attribute).filter(Boolean)
  return [...new Set(attributes)]
})

// 获取属性标签
const getAttributeLabel = (attr) => {
  const party = (props.caseData.parties || []).find(item => (item.partyRole || item.attribute) === attr)
  return party?.partyRoleDesc || PARTY_ROLE_LABELS[attr] || attr
}

// 根据属性筛选当事人
const getPartiesByAttribute = (attr) => {
  const parties = props.caseData.parties || []
  return parties.filter(p => (p.partyRole || p.attribute) === attr)
}

// 编辑区块
const handleEditSection = (section) => {
  // 打开编辑案件信息对话框
  infoForm.value = {
    caseType: props.caseData.caseType || '',
    procedure: props.caseData.procedure || '',
    caseReason: props.caseData.caseReason || '',
    caseNumber: props.caseData.caseNumber || '',
    court: props.caseData.court || '',
    courtCaseNumber: props.caseData.courtCaseNumber || '',
    trialStages: props.caseData.trialStages || '',
    businessType: props.caseData.businessType || '',
    suspectName: props.caseData.suspectName || '',
    agencyType: props.caseData.agencyType || '',
    subjectMatter: props.caseData.subjectMatter || '',
    consultantUnitName: props.caseData.consultantUnitName || '',
    consultantContactName: props.caseData.consultantContactName || '',
    serviceStartDate: normalizeDate(props.caseData.serviceStartDate),
    serviceEndDate: normalizeDate(props.caseData.serviceEndDate),
    consultantServiceScope: props.caseData.consultantServiceScope || '',
    consultantResponseRequirement: props.caseData.consultantResponseRequirement || '',
    deadlineDate: normalizeDate(props.caseData.deadlineDate),
    commissionDate: normalizeDate(props.caseData.commissionDate),
    wonAmount: props.caseData.wonAmount ?? props.caseData.winAmount ?? null,
    actualReceived: props.caseData.actualReceived ?? props.caseData.actualAmount ?? null,
    tags: displayTags.value.join(','),
    summary: props.caseData.summary || ''
  }
  infoDialogVisible.value = true
}

const handleSubmitInfo = async () => {
  if (infoForm.value.caseType === 'CONSULTANT') {
    if (!infoForm.value.consultantUnitName?.trim() || !infoForm.value.serviceStartDate || !infoForm.value.serviceEndDate) {
      ElMessage.warning('顾问案件请完整填写顾问单位和服务期限')
      return
    }
    if (new Date(infoForm.value.serviceStartDate) > new Date(infoForm.value.serviceEndDate)) {
      ElMessage.warning('服务开始时间不能晚于结束时间')
      return
    }
  }
  try {
    const payload = {
      ...infoForm.value,
      tags: Array.isArray(infoForm.value.tags) ? infoForm.value.tags.join(',') : infoForm.value.tags
    }
    await updateCase(props.caseData.id, payload)
    ElMessage.success('案件信息更新成功')
    infoDialogVisible.value = false
    emit('refresh')
  } catch (error) {
    console.error('更新案件信息失败:', error)
    ElMessage.error('更新失败')
  }
}

// 当事人操作
const handleAddParty = () => {
  partyForm.value = {
    id: null,
    partyType: 'INDIVIDUAL',
    partyRole: 'PLAINTIFF',
    name: '',
    phone: '',
    gender: '男',
    idCard: '',
    creditCode: '',
    address: '',
    opposingLawyer: '',
    notes: '',
    isClient: false
  }
  partyDialogVisible.value = true
}

const handleEditParty = (party) => {
  partyForm.value = {
    id: party.id,
    partyType: party.partyType || 'INDIVIDUAL',
    partyRole: party.partyRole || 'PLAINTIFF',
    name: party.name || '',
    phone: party.phone || '',
    gender: party.gender || '男',
    idCard: party.idCard || '',
    creditCode: party.creditCode || '',
    address: party.address || '',
    opposingLawyer: party.opposingLawyer || '',
    notes: party.notes || '',
    isClient: party.isClient || false
  }
  partyDialogVisible.value = true
}

const handleDeleteParty = async (party) => {
  try {
    await ElMessageBox.confirm('确定要删除该当事人吗？', '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })

    await deleteParty(props.caseData.id, party.id)
    ElMessage.success('删除成功')
    emit('refresh')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除当事人失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleSaveParty = async () => {
  if (!partyForm.value.name) {
    ElMessage.warning('请输入当事人姓名/企业名称')
    return
  }

  try {
    if (partyForm.value.id) {
      await updateParty(props.caseData.id, partyForm.value.id, partyForm.value)
      ElMessage.success('更新成功')
    } else {
      await createParty(props.caseData.id, partyForm.value)
      ElMessage.success('添加成功')
    }
    partyDialogVisible.value = false
    emit('refresh')
  } catch (error) {
    console.error('保存当事人失败:', error)
    ElMessage.error('保存失败')
  }
}

// 团队操作
const handleEditTeam = () => {
  teamForm.value = {
    ownerId: props.caseData.ownerId || null,
    coOwnerIds: props.caseData.coOwners?.map(m => m.id) || [],
    assistantIds: props.caseData.assistants?.map(m => m.id) || []
  }
  teamDialogVisible.value = true
}

const handleSaveTeam = async () => {
  if (!teamForm.value.ownerId) {
    ElMessage.warning('请选择主办律师')
    return
  }

  try {
    await updateCase(props.caseData.id, {
      ownerId: teamForm.value.ownerId,
      coOwnerIds: teamForm.value.coOwnerIds,
      assistantIds: teamForm.value.assistantIds
    })
    ElMessage.success('团队更新成功')
    teamDialogVisible.value = false
    emit('refresh')
  } catch (error) {
    console.error('更新团队失败:', error)
    ElMessage.error('更新失败')
  }
}

// 费用操作
const handleEditFee = () => {
  feeForm.value = {
    attorneyFee: props.caseData.attorneyFee || 0,
    feeMethod: props.caseData.feeMethod || 'FIXED',
    amount: props.caseData.amount || 0,
    wonAmount: props.caseData.wonAmount ?? props.caseData.winAmount ?? 0,
    actualReceived: props.caseData.actualReceived ?? props.caseData.actualAmount ?? 0
  }
  feeDialogVisible.value = true
}

const handleSaveFee = async () => {
  try {
    await updateCase(props.caseData.id, feeForm.value)
    ElMessage.success('费用信息更新成功')
    feeDialogVisible.value = false
    emit('refresh')
  } catch (error) {
    console.error('更新费用失败:', error)
    ElMessage.error('更新失败')
  }
}

// 程序操作
const handleAddProcedure = () => {
  procedureForm.value = {
    id: null,
    name: '',
    caseNumber: '',
    region: '',
    court: '',
    judge: '',
    procedureType: 'FIRST_INSTANCE',
    filingDate: '',
    hearingDate: '',
    judgmentDate: '',
    notes: ''
  }
  procedureDialogVisible.value = true
}

const handleEditProcedure = (procedure) => {
  procedureForm.value = {
    id: procedure.id,
    name: procedure.name || '',
    caseNumber: procedure.caseNumber || '',
    region: procedure.region || '',
    court: procedure.court || '',
    judge: procedure.judge || '',
    procedureType: procedure.procedureType || 'FIRST_INSTANCE',
    filingDate: procedure.filingDate || '',
    hearingDate: procedure.hearingDate || '',
    judgmentDate: procedure.judgmentDate || '',
    notes: procedure.notes || ''
  }
  procedureDialogVisible.value = true
}

const handleSaveProcedure = async () => {
  if (!procedureForm.value.name) {
    ElMessage.warning('请输入程序名称')
    return
  }

  try {
    if (procedureForm.value.id) {
      await updateCaseProcedure(props.caseData.id, procedureForm.value.id, procedureForm.value)
      ElMessage.success('更新成功')
    } else {
      await createCaseProcedure(props.caseData.id, procedureForm.value)
      ElMessage.success('添加成功')
    }
    procedureDialogVisible.value = false
    emit('refresh')
  } catch (error) {
    console.error('保存程序失败:', error)
    ElMessage.error('保存失败')
  }
}

const handleViewRelatedCase = (relatedCase) => {
  router.push(`/case/${relatedCase.id}`)
}

// 办案策略操作
const handleAddStrategy = () => {
  strategyForm.value = {
    index: null,
    title: '',
    content: '',
    priority: 'MEDIUM'
  }
  strategyDialogVisible.value = true
}

const handleEditStrategy = (strategy) => {
  const index = props.caseData.strategies?.indexOf(strategy) ?? -1
  strategyForm.value = {
    index,
    title: strategy.title || '',
    content: strategy.content || '',
    priority: strategy.priority || 'MEDIUM'
  }
  strategyDialogVisible.value = true
}

const handleDeleteStrategy = async (index) => {
  try {
    await ElMessageBox.confirm('确定要删除该策略吗？', '提示', {
      type: 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    })

    const strategies = [...(props.caseData.strategies || [])]
    strategies.splice(index, 1)

    await updateCase(props.caseData.id, { strategies })
    ElMessage.success('删除成功')
    emit('refresh')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除策略失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const handleSaveStrategy = async () => {
  if (!strategyForm.value.title || !strategyForm.value.content) {
    ElMessage.warning('请填写策略标题和内容')
    return
  }

  try {
    const strategies = [...(props.caseData.strategies || [])]
    const newStrategy = {
      title: strategyForm.value.title,
      content: strategyForm.value.content,
      priority: strategyForm.value.priority,
      comments: [],
      createdAt: new Date().toISOString()
    }

    if (strategyForm.value.index !== null) {
      // 编辑
      const existing = strategies[strategyForm.value.index]
      strategies[strategyForm.value.index] = {
        ...existing,
        ...newStrategy
      }
    } else {
      // 新增
      strategies.push(newStrategy)
    }

    await updateCase(props.caseData.id, { strategies })
    ElMessage.success(strategyForm.value.index !== null ? '更新成功' : '添加成功')
    strategyDialogVisible.value = false
    emit('refresh')
  } catch (error) {
    console.error('保存策略失败:', error)
    ElMessage.error('保存失败')
  }
}

</script>

<style scoped lang="scss">
.case-basic {
  padding: 24px;

  .basic-content {
    .info-section {
      margin-bottom: 30px;

      &:last-child {
        margin-bottom: 0;
      }

      .section-title {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 15px;
        padding-bottom: 10px;
        border-bottom: 1px solid #e4e7ed;

        h3 {
          margin: 0;
          font-size: 16px;
          font-weight: 500;
          color: #333;
          border-left: 4px solid #1890ff;
          padding-left: 12px;
        }
      }

      .party-tabs {
        margin-top: 15px;
      }

      .conflict-review-meta {
        margin-top: 5px;
        color: #6b7280;
        font-size: 12px;
        line-height: 1.4;
      }

      .conflict-attachment-list {
        display: flex;
        flex-direction: column;
        align-items: flex-start;

        .el-button {
          height: auto;
          padding: 2px 0;
          white-space: normal;
          text-align: left;
          overflow-wrap: anywhere;
        }
      }

      .workflow-strip {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
        margin-bottom: 16px;

        .workflow-step {
          display: inline-flex;
          align-items: center;
          gap: 6px;
          min-height: 32px;
          padding: 5px 10px;
          border: 1px solid #dfe3e8;
          border-radius: 6px;
          background: #f7f8fa;
          color: #4b5563;
          font-size: 13px;

          b {
            display: inline-flex;
            align-items: center;
            justify-content: center;
            width: 18px;
            height: 18px;
            border-radius: 50%;
            background: #e7eef8;
            color: #1d4ed8;
            font-size: 11px;
          }

          &.is-completed {
            border-color: #b7d8c4;
            background: #f1f8f4;
            color: #25613d;

            b {
              background: #d7ecdf;
              color: #25613d;
            }
          }

          &.is-active {
            border-color: #8bb5e8;
            background: #eef5fd;
            color: #174f8f;
            font-weight: 600;
          }
        }
      }

      .type-details {
        margin-top: 4px;
      }

      .attention-panel {
        margin-top: 16px;
        padding: 14px 16px;
        border-left: 3px solid #d39a37;
        background: #fbf8f1;

        .attention-title {
          margin-bottom: 8px;
          color: #513f1d;
          font-size: 14px;
          font-weight: 600;
        }

        ul {
          display: grid;
          grid-template-columns: repeat(2, minmax(0, 1fr));
          gap: 7px 24px;
          margin: 0;
          padding-left: 20px;
          color: #5d5546;
          font-size: 13px;
          line-height: 1.55;
        }
      }

      .team-grid {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
        gap: 15px;

        .team-item {
          background-color: #f5f7fa;
          padding: 15px;
          border-radius: 4px;

          .team-label {
            font-size: 12px;
            color: #909399;
            margin-bottom: 8px;
          }

          .team-value {
            font-size: 14px;
            color: #333;
          }
        }
      }

      .fee-amount {
        font-size: 18px;
        font-weight: bold;
        color: #f56c6c;
      }

      .fee-payment {
        margin-top: 20px;
        padding-top: 20px;
        border-top: 1px dashed #e4e7ed;

        h4 {
          margin: 0 0 15px;
          font-size: 14px;
          color: #333;
        }
      }

      .procedure-cards {
        display: grid;
        grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
        gap: 15px;

        .procedure-card {
          .procedure-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            font-weight: 500;
          }

          .procedure-attachments {
            margin-top: 10px;
            padding-top: 10px;
            border-top: 1px solid #e4e7ed;
            display: flex;
            align-items: center;
            gap: 8px;
            flex-wrap: wrap;

            .attachment-label {
              font-size: 12px;
              color: #909399;
            }
          }
        }
      }

      .strategy-list {
        .strategy-item {
          background-color: #f5f7fa;
          padding: 15px;
          border-radius: 4px;
          margin-bottom: 15px;

          &:last-child {
            margin-bottom: 0;
          }

          .strategy-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 10px;

            .strategy-title {
              font-weight: 500;
              color: #333;
            }
          }

          .strategy-content {
            margin-bottom: 15px;
            color: #606266;
            line-height: 1.6;
          }

          .strategy-discussion {
            padding-top: 15px;
            border-top: 1px dashed #e4e7ed;

            h4 {
              margin: 0 0 10px;
              font-size: 14px;
              color: #333;
            }

            .comment-item {
              margin-bottom: 8px;
              font-size: 13px;

              .comment-author {
                font-weight: 500;
                color: #1890ff;
              }

              .comment-text {
                color: #606266;
                margin: 0 8px;
              }

              .comment-time {
                color: #909399;
                font-size: 12px;
              }
            }
          }
        }
      }
    }
  }

  .deadline-overdue {
    color: #f56c6c;
    font-weight: bold;
  }

  .deadline-urgent {
    color: #f56c6c;
  }

  .deadline-warning {
    color: #e6a23c;
  }

  @media (max-width: 760px) {
    padding: 16px;

    .basic-content .info-section {
      margin-bottom: 24px;

      .section-title {
        align-items: flex-start;
        gap: 10px;

        h3 {
          font-size: 15px;
        }
      }

      .attention-panel ul {
        grid-template-columns: 1fr;
      }

      .procedure-cards {
        grid-template-columns: minmax(0, 1fr);
      }

      .team-grid {
        grid-template-columns: minmax(0, 1fr);
      }
    }

    :deep(.el-dialog .el-row) {
      margin-left: 0 !important;
      margin-right: 0 !important;
    }

    :deep(.el-dialog .el-col-12) {
      flex: 0 0 100%;
      max-width: 100%;
      padding-left: 0 !important;
      padding-right: 0 !important;
    }
  }
}
</style>
