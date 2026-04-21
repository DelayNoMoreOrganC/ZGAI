<template>
  <div class="finance">
    <PageHeader title="财务管理" />

    <el-tabs v-model="activeTab" type="card" class="finance-tabs"
      :class="'tab-' + activeTab">
      <!-- 费用记录 -->
      <el-tab-pane label="费用记录" name="expenses">
        <div class="tab-content">
          <div class="toolbar">
            <el-button type="primary" @click="handleAddExpense" class="add-btn">
              <el-icon><Plus /></el-icon>
              添加费用
            </el-button>
            <el-select v-model="expenseType" placeholder="费用类型" clearable>
              <el-option label="诉讼费" value="litigation" />
              <el-option label="保全费" value="preservation" />
              <el-option label="鉴定费" value="appraisal" />
              <el-option label="公证费" value="notary" />
              <el-option label="差旅费" value="travel" />
              <el-option label="快递费" value="express" />
              <el-option label="其他" value="other" />
            </el-select>
          </div>

          <el-table :data="expenseList" border class="finance-table"
            :header-cell-style="{ background: '#f0f5ff', color: '#333', fontWeight: '600' }"
            :row-class-name="tableRowClassName">
            <el-table-column prop="date" label="日期" width="120" sortable />
            <el-table-column prop="type" label="费用类型" width="120">
              <template #default="{ row }">
                <el-tag>{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="caseName" label="关联案件" width="200">
              <template #default="{ row }">
                <el-link v-if="row.caseId" @click="goToCase(row.caseId)" type="primary">
                  {{ row.caseName }}
                </el-link>
                <span v-else>{{ row.caseName || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="金额(元)" width="120">
              <template #default="{ row }">
                ¥{{ row.amount?.toLocaleString() || '0' }}
              </template>
            </el-table-column>
            <el-table-column prop="payee" label="收款方" width="150" />
            <el-table-column prop="applicant" label="申请人" width="100" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === '已报销' ? 'success' : 'warning'">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" show-overflow-tooltip />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button link type="primary" size="small">编辑</el-button>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 律师费管理 -->
      <el-tab-pane label="律师费管理" name="lawyer-fees">
        <div class="tab-content">
          <div class="fee-summary">
            <div class="summary-card">
              <span class="label">总应收律师费：</span>
              <span class="value">¥{{ totalFees.toLocaleString() }}</span>
            </div>
            <div class="summary-card">
              <span class="label">已收：</span>
              <span class="value received">¥{{ receivedFees.toLocaleString() }}</span>
            </div>
            <div class="summary-card">
              <span class="label">待收：</span>
              <span class="value pending">¥{{ pendingFees.toLocaleString() }}</span>
            </div>
          </div>

          <el-table :data="lawyerFeeList" border class="finance-table"
            :header-cell-style="{ background: '#f0f5ff', color: '#333', fontWeight: '600' }"
            :row-class-name="tableRowClassName">
            <el-table-column prop="caseName" label="案件名称" width="200">
              <template #default="{ row }">
                <el-link v-if="row.caseId" @click="goToCase(row.caseId)" type="primary">
                  {{ row.caseName }}
                </el-link>
                <span v-else>{{ row.caseName || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="clientName" label="客户" width="120" />
            <el-table-column prop="feeType" label="收费方式" width="100" />
            <el-table-column prop="totalFee" label="应收律师费(元)" width="130">
              <template #default="{ row }">
                ¥{{ row.totalFee?.toLocaleString() || '0' }}
              </template>
            </el-table-column>
            <el-table-column prop="receivedFee" label="已收(元)" width="100">
              <template #default="{ row }">
                ¥{{ row.receivedFee?.toLocaleString() || '0' }}
              </template>
            </el-table-column>
            <el-table-column prop="pendingFee" label="待收(元)" width="100">
              <template #default="{ row }">
                ¥{{ row.pendingFee?.toLocaleString() || '0' }}
              </template>
            </el-table-column>
            <el-table-column label="收款进度" width="150">
              <template #default="{ row }">
                <el-progress :percentage="getPaymentProgress(row)" />
              </template>
            </el-table-column>
            <el-table-column prop="owner" label="主办律师" width="100" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button link type="primary" size="small">详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 收款记录 -->
      <el-tab-pane label="收款记录" name="payments">
        <div class="tab-content">
          <div class="toolbar">
            <el-button type="primary" @click="handleAddPayment" class="add-btn">
              <el-icon><Plus /></el-icon>
              添加收款
            </el-button>
            <el-date-picker
              v-model="paymentDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
            />
          </div>

          <el-table :data="paymentList" border class="finance-table"
            :header-cell-style="{ background: '#f0f5ff', color: '#333', fontWeight: '600' }"
            :row-class-name="tableRowClassName">
            <el-table-column prop="date" label="收款日期" width="120" sortable />
            <el-table-column prop="caseName" label="关联案件" width="200">
              <template #default="{ row }">
                <el-link v-if="row.caseId" @click="goToCase(row.caseId)" type="primary">
                  {{ row.caseName }}
                </el-link>
                <span v-else>{{ row.caseName || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="clientName" label="付款客户" width="120" />
            <el-table-column prop="amount" label="收款金额(元)" width="130">
              <template #default="{ row }">
                <span class="amount">¥{{ row.amount?.toLocaleString() || '0' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="method" label="收款方式" width="100">
              <template #default="{ row }">
                <el-tag>{{ row.method }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="account" label="收款账户" width="150" />
            <el-table-column prop="operator" label="经办人" width="100" />
            <el-table-column prop="remark" label="备注" show-overflow-tooltip />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button link type="primary" size="small">查看</el-button>
                <el-button link type="danger" size="small">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <!-- 开票记录 -->
      <el-tab-pane label="开票记录" name="invoices">
        <div class="tab-content">
          <div class="toolbar">
            <el-button type="primary" @click="handleAddInvoice" class="add-btn">
              <el-icon><Plus /></el-icon>
              新增开票
            </el-button>
          </div>

          <el-table :data="invoiceList" border class="finance-table"
            :header-cell-style="{ background: '#f0f5ff', color: '#333', fontWeight: '600' }"
            :row-class-name="tableRowClassName">
            <el-table-column prop="invoiceDate" label="开票日期" width="120" sortable />
            <el-table-column prop="invoiceNumber" label="发票号码" width="150" />
            <el-table-column prop="type" label="发票类型" width="100">
              <template #default="{ row }">
                <el-tag>{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="clientName" label="购买方" width="200" />
            <el-table-column prop="amount" label="金额(元)" width="120">
              <template #default="{ row }">
                ¥{{ row.amount?.toLocaleString() || '0' }}
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === '已开票' ? 'success' : 'warning'">
                  {{ row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" show-overflow-tooltip />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button link type="primary" size="small">查看</el-button>
                <el-button link type="primary" size="small">打印</el-button>
                <el-button link type="danger" size="small">作废</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 添加费用对话框 -->
    <el-dialog v-model="expenseDialogVisible" title="添加费用" width="600px">
      <el-form :model="expenseForm" label-width="100px">
        <el-form-item label="日期" required>
          <el-date-picker
            v-model="expenseForm.date"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="费用类型" required>
          <el-select v-model="expenseForm.type" placeholder="请选择费用类型" style="width: 100%">
            <el-option label="诉讼费" value="litigation" />
            <el-option label="保全费" value="preservation" />
            <el-option label="鉴定费" value="appraisal" />
            <el-option label="公证费" value="notary" />
            <el-option label="差旅费" value="travel" />
            <el-option label="快递费" value="express" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>

        <el-form-item label="关联案件">
          <el-select
            v-model="expenseForm.caseId"
            filterable
            placeholder="请选择案件（可选）"
            style="width: 100%"
            clearable
          >
            <el-option
              v-for="caseItem in caseList"
              :key="caseItem.id"
              :label="`${caseItem.caseNumber} - ${caseItem.caseName}`"
              :value="caseItem.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="金额" required>
          <el-input-number v-model="expenseForm.amount" :min="0" :precision="2" placeholder="请输入金额" style="width: 100%" />
        </el-form-item>

        <el-form-item label="收款方" required>
          <el-input v-model="expenseForm.payee" placeholder="请输入收款方" />
        </el-form-item>

        <el-form-item label="申请人">
          <el-input v-model="expenseForm.applicant" placeholder="请输入申请人" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="expenseForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="expenseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitExpense">确定</el-button>
      </template>
    </el-dialog>

    <!-- 添加收款对话框 -->
    <el-dialog v-model="paymentDialogVisible" title="添加收款" width="600px">
      <el-form :model="paymentForm" label-width="100px">
        <el-form-item label="收款日期" required>
          <el-date-picker
            v-model="paymentForm.date"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="关联案件">
          <el-select
            v-model="paymentForm.caseId"
            filterable
            placeholder="请选择案件（可选）"
            style="width: 100%"
            clearable
          >
            <el-option
              v-for="caseItem in caseList"
              :key="caseItem.id"
              :label="`${caseItem.caseNumber} - ${caseItem.caseName}`"
              :value="caseItem.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="付款客户" required>
          <el-input v-model="paymentForm.clientName" placeholder="请输入付款客户" />
        </el-form-item>

        <el-form-item label="收款金额" required>
          <el-input-number v-model="paymentForm.amount" :min="0" :precision="2" placeholder="请输入金额" style="width: 100%" />
        </el-form-item>

        <el-form-item label="收款方式" required>
          <el-select v-model="paymentForm.method" placeholder="请选择收款方式" style="width: 100%">
            <el-option label="银行转账" value="BANK_TRANSFER" />
            <el-option label="现金" value="CASH" />
            <el-option label="支票" value="CHECK" />
            <el-option label="支付宝" value="ALIPAY" />
            <el-option label="微信" value="WECHAT" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>

        <el-form-item label="收款账户">
          <el-input v-model="paymentForm.account" placeholder="请输入收款账户" />
        </el-form-item>

        <el-form-item label="经办人">
          <el-input v-model="paymentForm.operator" placeholder="请输入经办人" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="paymentForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="paymentDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitPayment">确定</el-button>
      </template>
    </el-dialog>

    <!-- 新增开票对话框 -->
    <el-dialog v-model="invoiceDialogVisible" title="新增开票" width="600px">
      <el-form :model="invoiceForm" label-width="100px">
        <el-form-item label="开票日期" required>
          <el-date-picker
            v-model="invoiceForm.invoiceDate"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="发票号码" required>
          <el-input v-model="invoiceForm.invoiceNumber" placeholder="请输入发票号码" />
        </el-form-item>

        <el-form-item label="发票类型" required>
          <el-select v-model="invoiceForm.type" placeholder="请选择发票类型" style="width: 100%">
            <el-option label="增值税专用发票" value="VAT_SPECIAL" />
            <el-option label="增值税普通发票" value="VAT ordinary" />
            <el-option label="电子发票" value="ELECTRONIC" />
          </el-select>
        </el-form-item>

        <el-form-item label="关联案件">
          <el-select
            v-model="invoiceForm.caseId"
            filterable
            placeholder="请选择案件（可选）"
            style="width: 100%"
            clearable
          >
            <el-option
              v-for="caseItem in caseList"
              :key="caseItem.id"
              :label="`${caseItem.caseNumber} - ${caseItem.caseName}`"
              :value="caseItem.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="购买方" required>
          <el-input v-model="invoiceForm.clientName" placeholder="请输入购买方名称" />
        </el-form-item>

        <el-form-item label="金额" required>
          <el-input-number v-model="invoiceForm.amount" :min="0" :precision="2" placeholder="请输入金额" style="width: 100%" />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="invoiceForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="invoiceDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitInvoice">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/PageHeader.vue'
import { getExpenses, getPayments, getInvoices, getFinanceDashboard, createExpense, createPayment, createInvoice } from '@/api/finance'
import { getCaseList } from '@/api/case'

const router = useRouter()

const activeTab = ref('expenses')
const expenseType = ref('')
const paymentDateRange = ref([])
const loading = ref(false)

// ==================== 费用对话框 ====================
const expenseDialogVisible = ref(false)
const expenseForm = ref({
  date: '',
  type: 'litigation',
  caseId: null,
  amount: null,
  payee: '',
  applicant: '',
  status: 'PENDING',
  remark: ''
})
const caseList = ref([])

// ==================== 收款对话框 ====================
const paymentDialogVisible = ref(false)
const paymentForm = ref({
  date: '',
  caseId: null,
  clientName: '',
  amount: null,
  method: 'BANK_TRANSFER',
  account: '',
  operator: '',
  remark: ''
})

// ==================== 开票对话框 ====================
const invoiceDialogVisible = ref(false)
const invoiceForm = ref({
  invoiceDate: '',
  invoiceNumber: '',
  type: 'VAT_SPECIAL',
  caseId: null,
  clientName: '',
  amount: null,
  status: 'PENDING',
  remark: ''
})

// 统计数据
const totalFees = ref(0)
const receivedFees = ref(0)
const pendingFees = ref(0)

// 数据列表
const expenseList = ref([])
const lawyerFeeList = ref([])
const paymentList = ref([])
const invoiceList = ref([])

// 获取费用记录
const fetchExpenses = async () => {
  try {
    loading.value = true
    const res = await getExpenses({ page: 1, size: 100 })
    expenseList.value = res.data?.records || []
  } catch (error) {
    ElMessage.error('获取费用记录失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 获取收款记录
const fetchPayments = async () => {
  try {
    loading.value = true
    const res = await getPayments({ page: 1, size: 100 })
    paymentList.value = res.data?.records || []
  } catch (error) {
    ElMessage.error('获取收款记录失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 获取开票记录
const fetchInvoices = async () => {
  try {
    loading.value = true
    const res = await getInvoices({ page: 1, size: 100 })
    invoiceList.value = res.data?.records || []
  } catch (error) {
    ElMessage.error('获取开票记录失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 获取财务仪表盘数据
const fetchFinanceDashboard = async () => {
  try {
    const res = await getFinanceDashboard()
    totalFees.value = res.data?.totalFees || 0
    receivedFees.value = res.data?.receivedFees || 0
    pendingFees.value = res.data?.pendingFees || 0

    // 律师费列表从仪表盘数据获取
    lawyerFeeList.value = res.data?.lawyerFees || []
  } catch (error) {
    ElMessage.error('获取财务统计数据失败')
    console.error(error)
  }
}

const getPaymentProgress = (row) => {
  if (!row.totalFee) return 0
  return Math.round((row.receivedFee / row.totalFee) * 100)
}

const handleAddExpense = async () => {
  // 加载案件列表
  try {
    const { data } = await getCaseList({ page: 1, size: 100 })
    caseList.value = data.records || []
  } catch (error) {
    console.error('加载案件列表失败:', error)
  }

  expenseForm.value = {
    date: new Date().toISOString().split('T')[0],
    type: 'litigation',
    caseId: null,
    amount: null,
    payee: '',
    applicant: '',
    status: 'PENDING',
    remark: ''
  }
  expenseDialogVisible.value = true
}

const handleSubmitExpense = async () => {
  if (!expenseForm.value.date) {
    ElMessage.warning('请选择日期')
    return
  }
  if (!expenseForm.value.amount) {
    ElMessage.warning('请输入金额')
    return
  }
  if (!expenseForm.value.payee) {
    ElMessage.warning('请输入收款方')
    return
  }

  try {
    await createExpense(expenseForm.value)
    ElMessage.success('费用添加成功')
    expenseDialogVisible.value = false
    fetchExpenses()
  } catch (error) {
    console.error('添加费用失败:', error)
    ElMessage.error('添加失败')
  }
}

const handleAddPayment = async () => {
  // 加载案件列表
  try {
    const { data } = await getCaseList({ page: 1, size: 100 })
    caseList.value = data.records || []
  } catch (error) {
    console.error('加载案件列表失败:', error)
  }

  paymentForm.value = {
    date: new Date().toISOString().split('T')[0],
    caseId: null,
    clientName: '',
    amount: null,
    method: 'BANK_TRANSFER',
    account: '',
    operator: '',
    remark: ''
  }
  paymentDialogVisible.value = true
}

const handleSubmitPayment = async () => {
  if (!paymentForm.value.date) {
    ElMessage.warning('请选择收款日期')
    return
  }
  if (!paymentForm.value.amount) {
    ElMessage.warning('请输入收款金额')
    return
  }
  if (!paymentForm.value.clientName) {
    ElMessage.warning('请输入付款客户')
    return
  }

  try {
    await createPayment(paymentForm.value)
    ElMessage.success('收款添加成功')
    paymentDialogVisible.value = false
    fetchPayments()
  } catch (error) {
    console.error('添加收款失败:', error)
    ElMessage.error('添加失败')
  }
}

const handleAddInvoice = async () => {
  // 加载案件列表
  try {
    const { data } = await getCaseList({ page: 1, size: 100 })
    caseList.value = data.records || []
  } catch (error) {
    console.error('加载案件列表失败:', error)
  }

  invoiceForm.value = {
    invoiceDate: new Date().toISOString().split('T')[0],
    invoiceNumber: '',
    type: 'VAT_SPECIAL',
    caseId: null,
    clientName: '',
    amount: null,
    status: 'PENDING',
    remark: ''
  }
  invoiceDialogVisible.value = true
}

const handleSubmitInvoice = async () => {
  if (!invoiceForm.value.invoiceDate) {
    ElMessage.warning('请选择开票日期')
    return
  }
  if (!invoiceForm.value.invoiceNumber) {
    ElMessage.warning('请输入发票号码')
    return
  }
  if (!invoiceForm.value.amount) {
    ElMessage.warning('请输入金额')
    return
  }
  if (!invoiceForm.value.clientName) {
    ElMessage.warning('请输入购买方')
    return
  }

  try {
    await createInvoice(invoiceForm.value)
    ElMessage.success('开票记录添加成功')
    invoiceDialogVisible.value = false
    fetchInvoices()
  } catch (error) {
    console.error('添加开票记录失败:', error)
    ElMessage.error('添加失败')
  }
}

// 切换Tab时加载数据
const handleTabChange = (tabName) => {
  switch(tabName) {
    case 'expenses':
      fetchExpenses()
      break
    case 'payments':
      fetchPayments()
      break
    case 'invoices':
      fetchInvoices()
      break
    case 'lawyer-fees':
      fetchFinanceDashboard()
      break
  }
}

// 跳转到案件详情
const goToCase = (caseId) => {
  router.push(`/case/${caseId}`)
}

onMounted(() => {
  fetchFinanceDashboard()
  fetchExpenses()
})

// 监听Tab切换，自动加载对应数据
watch(activeTab, (newTab) => {
  handleTabChange(newTab)
})

const tableRowClassName = ({ rowIndex }) => {
  return rowIndex % 2 === 0 ? 'even-row' : 'odd-row'
}
</script>

<style scoped lang="scss">
.finance {
  .finance-tabs {
    margin-top: 20px;
    background: #fff;
    padding: 24px;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(24, 144, 255, 0.08);
    border: 1px solid #e6f7ff;

    :deep(.el-tabs__header) {
      margin-bottom: 24px;
      border-bottom: 2px solid #e6f7ff;
    }

    :deep(.el-tabs__item) {
      color: #666;
      font-weight: 500;
      padding: 0 24px;
      height: 40px;
      line-height: 40px;
      border: none;
      transition: all 0.3s;

      &:hover {
        color: #1890ff;
        background: #f0f5ff;
      }

      &.is-active {
        color: #1890ff;
        background: linear-gradient(135deg, #f0f5ff 0%, #e6f7ff 100%);
        border-bottom: 2px solid #1890ff;
        font-weight: 600;
      }
    }
  }

  .add-btn {
    background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
    border: none;
    border-radius: 8px;
    padding: 10px 20px;
    box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3);
    transition: all 0.3s;

    &:hover {
      background: linear-gradient(135deg, #40a9ff 0%, #1890ff 100%);
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(24, 144, 255, 0.4);
    }
  }

  .finance-table {
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 2px 12px rgba(24, 144, 255, 0.08);

    :deep(.el-table__header-wrapper) {
      th {
        background: #f0f5ff !important;
        color: #333 !important;
        font-weight: 600;
        border-bottom: 2px solid #1890ff;
      }
    }

    :deep(.el-table__body-wrapper) {
      .el-table__row {
        transition: all 0.3s;

        &.even-row {
          background: #ffffff;

          &:hover {
            background: #f0f5ff !important;
          }
        }

        &.odd-row {
          background: #fafcfe;

          &:hover {
            background: #f0f5ff !important;
          }
        }

        td {
          border-bottom: 1px solid #f0f0f0;
        }
      }
    }

    :deep(.el-table__border) {
      border: 1px solid #e6f7ff;
    }
  }

  .tab-content {
    .toolbar {
      display: flex;
      gap: 12px;
      margin-bottom: 20px;
      flex-wrap: wrap;
      align-items: center;

      :deep(.el-select) {
        .el-select__wrapper {
          border-radius: 8px;
          border: 1px solid #d9d9d0;
          transition: all 0.3s;

          &:hover {
            border-color: #1890ff;
          }

          &.is-focus {
            border-color: #1890ff;
            box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
          }
        }
      }

      :deep(.el-date-editor) {
        .el-input__wrapper {
          border-radius: 8px;
          border: 1px solid #d9d9d0;
          transition: all 0.3s;

          &:hover {
            border-color: #1890ff;
          }

          &.is-focus {
            border-color: #1890ff;
            box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.1);
          }
        }
      }
    }

    .fee-summary {
      display: flex;
      gap: 20px;
      margin-bottom: 24px;

      .summary-card {
        flex: 1;
        background: linear-gradient(135deg, #f0f5ff 0%, #ffffff 100%);
        padding: 24px;
        border-radius: 12px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        border: 1px solid #e6f7ff;
        box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
        transition: all 0.3s;

        &:hover {
          transform: translateY(-2px);
          box-shadow: 0 4px 16px rgba(24, 144, 255, 0.15);
        }

        .label {
          font-size: 15px;
          color: #666;
          font-weight: 500;
        }

        .value {
          font-size: 24px;
          font-weight: bold;
          color: #1890ff;

          &.received {
            color: #52c41a;
          }

          &.pending {
            color: #faad14;
          }
        }
      }
    }

    .amount {
      color: #52c41a;
      font-weight: 600;
      font-size: 15px;
    }
  }
}
</style>
