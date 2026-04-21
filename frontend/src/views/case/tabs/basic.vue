<template>
  <div class="case-basic">
    <div class="basic-content">
      <!-- 区块1: 案件信息 -->
      <div class="info-section">
        <div class="section-title">
          <h3>案件信息</h3>
          <el-button text type="primary" size="small" @click="handleEditSection('info')">
            编辑
          </el-button>
        </div>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="案件类型">
            <el-tag :type="getTypeTagType(caseData.caseType)">
              {{ caseData.caseType }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="案件程序">
            {{ caseData.procedure }}
          </el-descriptions-item>
          <el-descriptions-item label="案由">
            {{ caseData.caseReason }}
          </el-descriptions-item>
          <el-descriptions-item label="案号">
            {{ caseData.caseNumber }}
          </el-descriptions-item>
          <el-descriptions-item label="管辖法院">
            {{ caseData.court }}
          </el-descriptions-item>
          <el-descriptions-item label="委托客户">
            <div v-if="caseData.clientId">
              <el-tag type="success" size="small">有委托客户</el-tag>
              <el-button
                type="primary"
                size="small"
                text
                @click="handleViewClient"
                style="margin-left: 8px"
              >
                查看客户详情
              </el-button>
            </div>
            <span v-else class="text-muted">未关联客户</span>
          </el-descriptions-item>
          <el-descriptions-item label="案件等级">
            <span v-if="caseData.level === '重要'">🔴 重要</span>
            <span v-else-if="caseData.level === '一般'">🟡 一般</span>
            <span v-else>⚪ 次要</span>
          </el-descriptions-item>
          <el-descriptions-item label="立案时间">
            {{ caseData.filingDate || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="审限时间">
            <span :class="getDeadlineClass(caseData.deadlineDate)">
              {{ caseData.deadlineDate || '-' }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="委托时间">
            {{ caseData.commissionDate || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="胜诉金额" :span="1">
            ¥{{ caseData.winAmount || '0' }}
          </el-descriptions-item>
          <el-descriptions-item label="实际回款" :span="1">
            ¥{{ caseData.actualAmount || '0' }}
          </el-descriptions-item>
          <el-descriptions-item label="案件标签" :span="1">
            <el-tag
              v-for="tag in caseData.tags"
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

      <!-- 区块2: 当事人及关联方 -->
      <div class="info-section">
        <div class="section-title">
          <h3>当事人及关联方</h3>
          <el-button type="primary" size="small" @click="handleAddParty">
            <el-icon><Plus /></el-icon>
            添加当事人
          </el-button>
        </div>
        <el-tabs type="card" class="party-tabs">
          <el-tab-pane
            v-for="attr in uniqueAttributes"
            :key="attr"
            :label="getAttributeLabel(attr)"
          >
            <el-table :data="getPartiesByAttribute(attr)" border>
              <el-table-column prop="name" label="姓名/单位名称" width="150" />
              <el-table-column prop="type" label="类型" width="80">
                <template #default="{ row }">
                  <el-tag :type="row.type === '个人' ? 'primary' : 'success'" size="small">
                    {{ row.type }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="phone" label="联系电话" width="130" />
              <el-table-column prop="address" label="地址" show-overflow-tooltip />
              <el-table-column prop="idCard" label="身份证号/信用代码" width="180" />
              <el-table-column prop="opposingLawyer" label="代理律师" width="120" />
              <el-table-column label="操作" width="120" fixed="right">
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
          <el-button text type="primary" size="small" @click="handleEditTeam">
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
          <el-button text type="primary" size="small" @click="handleEditFee">
            编辑
          </el-button>
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="收费方式">
            <el-tag
              v-for="feeType in caseData.feeTypes"
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
            <span class="fee-amount">¥{{ caseData.lawyerFee || '0' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="收费简介" :span="2">
            {{ caseData.feeSummary || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="收费备注" :span="2">
            {{ caseData.feeRemark || '-' }}
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
          <el-button type="primary" size="small" @click="handleAddProcedure">
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
                <el-button text type="primary" size="small" @click="handleEditProcedure(procedure)">
                  编辑
                </el-button>
              </div>
            </template>
            <el-descriptions :column="2" size="small">
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
                {{ procedure.filingDate || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="开庭日期">
                {{ procedure.hearingDate || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="裁决日期">
                {{ procedure.judgmentDate || '-' }}
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
                closable
                @close="handleDeleteAttachment(procedure, file)"
              >
                {{ file.name }}
              </el-tag>
            </div>
          </el-card>
        </div>
      </div>

      <!-- 区块6: 关联案件 -->
      <div class="info-section">
        <div class="section-title">
          <h3>关联案件</h3>
          <el-button type="primary" size="small" @click="handleAddRelatedCase">
            <el-icon><Plus /></el-icon>
            添加关联
          </el-button>
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
      </div>

      <!-- 区块7: 办案策略 -->
      <div class="info-section">
        <div class="section-title">
          <h3>办案策略</h3>
          <el-button type="primary" size="small" @click="handleAddStrategy">
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
              <div class="strategy-actions">
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
              <el-input
                v-model="strategy.newComment"
                placeholder="输入评论..."
                @keyup.enter="handleAddComment(strategy, index)"
              >
                <template #append>
                  <el-button @click="handleAddComment(strategy, index)">发送</el-button>
                </template>
              </el-input>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 当事人编辑对话框 -->
    <el-dialog
      v-model="partyDialogVisible"
      :title="partyForm.id ? '编辑当事人' : '添加当事人'"
      width="700px"
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
                <el-option label="原告" value="PLAINTIFF" />
                <el-option label="被告" value="DEFENDANT" />
                <el-option label="申请人" value="APPLICANT" />
                <el-option label="被申请人" value="RESPONDENT" />
                <el-option label="第三人" value="THIRD_PARTY" />
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
    <el-dialog v-model="teamDialogVisible" title="编辑团队" width="600px">
      <el-form :model="teamForm" label-width="100px">
        <el-form-item label="主办律师" required>
          <el-select v-model="teamForm.ownerId" placeholder="请选择主办律师" style="width: 100%">
            <el-option
              v-for="lawyer in lawyerList"
              :key="lawyer.id"
              :label="lawyer.name"
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
              :label="lawyer.name"
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
              :label="assistant.name"
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
    <el-dialog v-model="feeDialogVisible" title="编辑费用信息" width="600px">
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
            <el-option label="固定收费" value="FIXED" />
            <el-option label="按比例收费" value="PERCENTAGE" />
            <el-option label="风险代理" value="RISK" />
            <el-option label="计时收费" value="HOURLY" />
            <el-option label="协商收费" value="NEGOTIATED" />
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
      width="700px"
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
      width="700px"
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
    <el-dialog v-model="infoDialogVisible" title="编辑案件信息" width="700px">
      <el-form :model="infoForm" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="案件类型">
              <el-select v-model="infoForm.caseType" placeholder="请选择案件类型" style="width: 100%">
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
            <el-form-item label="案件程序">
              <el-select v-model="infoForm.procedure" placeholder="请选择案件程序" style="width: 100%">
                <el-option label="一审" value="一审" />
                <el-option label="二审" value="二审" />
                <el-option label="再审" value="再审" />
                <el-option label="执行" value="执行" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="案由">
              <el-input v-model="infoForm.caseReason" placeholder="请输入案由" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="案号">
              <el-input v-model="infoForm.caseNumber" placeholder="请输入案号" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="管辖法院">
              <el-input v-model="infoForm.court" placeholder="请输入管辖法院" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="案件等级">
              <el-select v-model="infoForm.level" placeholder="请选择案件等级" style="width: 100%">
                <el-option label="重要" value="重要" />
                <el-option label="一般" value="一般" />
                <el-option label="次要" value="次要" />
              </el-select>
            </el-form-item>
          </el-col>

          <el-col :span="8">
            <el-form-item label="立案时间">
              <el-date-picker
                v-model="infoForm.filingDate"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>

          <el-col :span="8">
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

          <el-col :span="8">
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
              <el-input-number v-model="infoForm.winAmount" :min="0" :precision="2" placeholder="请输入胜诉金额" style="width: 100%" />
            </el-form-item>
          </el-col>

          <el-col :span="12">
            <el-form-item label="实际回款">
              <el-input-number v-model="infoForm.actualAmount" :min="0" :precision="2" placeholder="请输入实际回款" style="width: 100%" />
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
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { createParty, updateParty, deleteParty, updateCase } from '@/api/case'
import { getCaseProcedures, createCaseProcedure, updateCaseProcedure, deleteCaseProcedure } from '@/api/case'

const props = defineProps({
  caseData: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['refresh'])

const router = useRouter()

// ==================== 案件信息对话框 ====================
const infoDialogVisible = ref(false)
const infoForm = ref({
  caseType: '',
  procedure: '',
  caseReason: '',
  caseNumber: '',
  court: '',
  level: '一般',
  filingDate: '',
  deadlineDate: '',
  commissionDate: '',
  winAmount: null,
  actualAmount: null,
  tags: [],
  summary: ''
})

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

// 模拟律师和助理列表（实际应该从API获取）
const lawyerList = ref([
  { id: 1, name: '张律师' },
  { id: 2, name: '李律师' },
  { id: 3, name: '王律师' }
])

const assistantList = ref([
  { id: 4, name: '助理小赵' },
  { id: 5, name: '助理小钱' }
])

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
    '民事': 'primary',
    '商事': 'success',
    '仲裁': 'warning',
    '刑事': 'danger',
    '行政': 'info',
    '非诉': ''
  }
  return typeMap[type] || ''
}

// 获取审限样式
const getDeadlineClass = (date) => {
  if (!date) return ''
  const days = Math.ceil((new Date(date) - new Date()) / (1000 * 60 * 60 * 24))
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
  const attributes = parties.map(p => p.attribute)
  return [...new Set(attributes)]
})

// 获取属性标签
const getAttributeLabel = (attr) => {
  const labelMap = {
    '原告': '原告',
    '被告': '被告',
    '第三人': '第三人',
    '共同原告': '共同原告',
    '共同被告': '共同被告',
    '申请人': '申请人',
    '被申请人': '被申请人'
  }
  return labelMap[attr] || attr
}

// 根据属性筛选当事人
const getPartiesByAttribute = (attr) => {
  const parties = props.caseData.parties || []
  return parties.filter(p => p.attribute === attr)
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
    level: props.caseData.level || '一般',
    filingDate: props.caseData.filingDate || '',
    deadlineDate: props.caseData.deadlineDate || '',
    commissionDate: props.caseData.commissionDate || '',
    winAmount: props.caseData.winAmount || null,
    actualAmount: props.caseData.actualAmount || null,
    tags: props.caseData.tags || [],
    summary: props.caseData.summary || ''
  }
  infoDialogVisible.value = true
}

const handleSubmitInfo = async () => {
  try {
    await updateCase(props.caseData.id, infoForm.value)
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

// 查看客户详情
const handleViewClient = () => {
  if (props.caseData.clientId) {
    router.push(`/client/${props.caseData.clientId}`)
  } else {
    ElMessage.warning('该案件未关联客户')
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
    wonAmount: props.caseData.wonAmount || 0,
    actualReceived: props.caseData.actualReceived || 0
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

const handleDeleteAttachment = async (procedure, file) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除附件"${file.name}"吗？`,
      '删除附件',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    // 从程序中移除附件
    const index = procedure.attachments?.indexOf(file)
    if (index > -1) {
      procedure.attachments.splice(index, 1)
    }

    ElMessage.success('附件删除成功')
    emit('refresh')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除附件失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 关联案件操作
const handleAddRelatedCase = () => {
  ElMessage.info('添加关联案件功能：请从案件列表选择要关联的案件')
  // 这里可以打开一个案件选择对话框
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

const handleAddComment = (strategy, index) => {
  if (!strategy.newComment) return

  // 办案策略的评论功能已迁移到"案件动态"tab
  // 请切换到"案件动态"tab查看和添加评论
  ElMessage.info('办案策略讨论请使用"案件动态"tab中的评论功能')
  strategy.newComment = ''
}
</script>

<style scoped lang="scss">
.case-basic {
  padding: 30px;

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
        border-bottom: 2px solid #e4e7ed;

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
}
</style>
