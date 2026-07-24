import { expect, test } from '@playwright/test'

const requiredEnvironment = [
  'ZGAI_LAWYER_USERNAME', 'ZGAI_LAWYER_PASSWORD',
  'ZGAI_ADMINISTRATIVE_USERNAME', 'ZGAI_ADMINISTRATIVE_PASSWORD',
  'ZGAI_DIRECTOR_USERNAME', 'ZGAI_DIRECTOR_PASSWORD',
  'ZGAI_FINANCE_USERNAME', 'ZGAI_FINANCE_PASSWORD'
]

const credentials = role => ({
  username: process.env[`ZGAI_${role}_USERNAME`],
  password: process.env[`ZGAI_${role}_PASSWORD`]
})

const login = async (page, account) => {
  await page.goto('/login')
  await page.getByPlaceholder('请输入用户名').fill(account.username)
  await page.getByPlaceholder('请输入密码').fill(account.password)
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/dashboard(?:$|\?)/)
}

const api = async (page, url, { method = 'GET', body } = {}) => page.evaluate(
  async ({ requestUrl, requestMethod, requestBody }) => {
    const response = await fetch(requestUrl, {
      method: requestMethod,
      headers: {
        Authorization: `Bearer ${localStorage.getItem('token')}`,
        ...(requestBody ? { 'Content-Type': 'application/json' } : {})
      },
      body: requestBody ? JSON.stringify(requestBody) : undefined
    })
    let payload = null
    try { payload = await response.json() } catch { /* status-only response */ }
    return { status: response.status, payload }
  },
  { requestUrl: url, requestMethod: method, requestBody: body }
)

const textareaFor = (page, testId) => page
  .locator(`[data-testid="${testId}"] textarea, textarea[data-testid="${testId}"]`)
  .first()

const reviewAndApprove = async (page, caseName) => {
  await page.goto('/approval')
  const card = page.locator('.filing-card').filter({ hasText: caseName }).first()
  await expect(card).toBeVisible()
  await card.getByRole('button', { name: '查看审批' }).click()
  const drawer = page.locator('.approval-detail-drawer')
  await drawer.getByRole('button', { name: '填写正式审查' }).click()
  await drawer.getByText('无冲突，通过', { exact: true }).click()
  await textareaFor(page, 'conflict-review-conclusion').fill('已核对全所主体，无利益冲突。')
  await drawer.getByTestId('conflict-review-submit').click()
  await expect(page.getByText('正式利冲审查已完成，结论已锁定', { exact: true })).toBeVisible()
  await drawer.getByTestId('approval-approve').click()
  const dialog = page.locator('.el-message-box')
  await dialog.locator('input').fill('行政正式利冲审查通过。')
  await dialog.getByRole('button', { name: '同意立案', exact: true }).click()
  await expect(page.getByText('立案审批已通过', { exact: true })).toBeVisible()
}

const directorApprove = async (page, caseName) => {
  await page.goto('/approval')
  const card = page.locator('.filing-card').filter({ hasText: caseName }).first()
  await expect(card).toBeVisible()
  await card.getByRole('button', { name: '查看审批' }).click()
  const drawer = page.locator('.approval-detail-drawer')
  await drawer.getByTestId('approval-approve').click()
  const dialog = page.locator('.el-message-box')
  await dialog.locator('input').fill('主任终审通过。')
  await dialog.getByRole('button', { name: '终审通过', exact: true }).click()
  await expect(page.getByText('立案审批已通过', { exact: true })).toBeVisible()
}

test.beforeAll(() => {
  if (process.env.ZGAI_E2E_ENVIRONMENT !== 'ISOLATED'
    || process.env.ZGAI_E2E_CONFIRM !== 'RUN_BROWSER_WRITE_E2E') {
    throw new Error('写入型浏览器 E2E 仅可在隔离环境运行')
  }
  const missing = requiredEnvironment.filter(name => !process.env[name])
  if (missing.length > 0) throw new Error(`缺少浏览器 E2E 环境变量：${missing.join(', ')}`)
})

test.describe.configure({ mode: 'serial' })
test.setTimeout(360_000)

test('四类案件经两级审批后按各自流程推进且不能绕过状态控制', async ({ browser }) => {
  const suffix = Date.now().toString().slice(-8)
  const definitions = [
    {
      caseType: 'ARBITRATION', label: '仲裁', procedure: '仲裁申请', partyRole: 'APPLICANT',
      firstStage: '接洽利冲', nextStage: '签约立案', skipStage: '仲裁条款审查',
      todoTitle: '完成仲裁委托和立案材料检查'
    },
    {
      caseType: 'CRIMINAL', label: '刑事', procedure: '侦查', partyRole: 'CLIENT',
      firstStage: '接洽利冲', nextStage: '签约', skipStage: '侦查与会见',
      todoTitle: '完成刑事委托手续', suspectName: `嫌疑人-${suffix}`
    },
    {
      caseType: 'ADMINISTRATIVE', label: '行政', procedure: '行政复议', partyRole: 'ADMINISTRATIVE_COUNTERPART',
      firstStage: '接洽利冲', nextStage: '签约立案', skipStage: '行政行为审查',
      todoTitle: '完成行政委托和救济材料检查'
    },
    {
      caseType: 'CONSULTANT', label: '顾问', procedure: '常年法律顾问', partyRole: 'CONSULTANT_UNIT',
      firstStage: '顾问建档', nextStage: '服务计划', skipStage: '需求受理',
      todoTitle: '制定顾问服务计划', consultant: true
    }
  ]

  const lawyerContext = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const lawyerPage = await lawyerContext.newPage()
  await login(lawyerPage, credentials('LAWYER'))
  const currentUser = await api(lawyerPage, '/api/auth/current-user')
  const ownerId = currentUser.payload?.data?.userId || currentUser.payload?.data?.id
  expect(ownerId).toBeTruthy()

  for (const definition of definitions) {
    definition.caseName = `E2E${definition.label}阶段案件-${suffix}`
    const clientName = `E2E${definition.label}委托方-${suffix}`
    const request = {
      caseType: definition.caseType,
      procedure: definition.procedure,
      caseName: definition.caseName,
      caseReason: `${definition.label}业务事项`,
      court: definition.caseType === 'ARBITRATION' ? '佛山仲裁委员会' : '佛山市测试办理机关',
      ownerId,
      businessType: definition.procedure,
      suspectName: definition.suspectName || null,
      feeMethod: 'OTHER',
      allocationJson: JSON.stringify({ sourceRatio: 100, departmentRatio: 0, firmRatio: 0 }),
      parties: [{
        partyType: 'ORGANIZATION', partyRole: definition.partyRole, name: clientName,
        isClient: true, syncToClient: false
      }],
      ...(definition.consultant ? {
        consultantUnitName: clientName,
        serviceStartDate: '2026-01-01',
        serviceEndDate: '2026-12-31',
        consultantServiceScope: '合同审查,日常咨询'
      } : {})
    }
    const created = await api(lawyerPage, '/api/cases', { method: 'POST', body: request })
    expect(created.status).toBe(200)
    expect(created.payload?.code).toBe(200)
    definition.caseId = created.payload.data.id
    expect(created.payload.data.status).toBe('PENDING_APPROVAL')
    expect(created.payload.data.currentStage).toBe(definition.firstStage)
  }

  const pending = definitions[0]
  await lawyerPage.goto(`/ai/case-workbench?caseId=${pending.caseId}`)
  await textareaFor(lawyerPage, 'ai-command-instruction').fill(`本案进入${pending.nextStage}阶段`)
  await lawyerPage.getByTestId('ai-command-submit').click()
  await expect(lawyerPage.getByTestId('ai-command-clarification'))
    .toContainText('案件立案审批通过且处于办理中时才能变更办理阶段')
  const stillPending = await api(lawyerPage, `/api/cases/${pending.caseId}`)
  expect(stillPending.payload.data.currentStage).toBe(pending.firstStage)

  const administrativeContext = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const administrativePage = await administrativeContext.newPage()
  await login(administrativePage, credentials('ADMINISTRATIVE'))
  for (const definition of definitions) await reviewAndApprove(administrativePage, definition.caseName)

  const directorContext = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const directorPage = await directorContext.newPage()
  await login(directorPage, credentials('DIRECTOR'))
  for (const definition of definitions) await directorApprove(directorPage, definition.caseName)

  for (const definition of definitions) {
    await lawyerPage.goto(`/ai/case-workbench?caseId=${definition.caseId}`)
    await textareaFor(lawyerPage, 'ai-command-instruction').fill(`本案进入${definition.skipStage}阶段`)
    await lawyerPage.getByTestId('ai-command-submit').click()
    await expect(lawyerPage.getByTestId('ai-command-clarification'))
      .toContainText(`当前只能进入下一阶段「${definition.nextStage}」`)

    await textareaFor(lawyerPage, 'ai-command-instruction').fill(`本案进入${definition.nextStage}阶段`)
    await lawyerPage.getByTestId('ai-command-submit').click()
    await expect(lawyerPage.getByTestId('ai-stage-proposal')).toContainText(definition.nextStage)
    await lawyerPage.getByTestId('ai-command-confirm').click()
    const dialog = lawyerPage.locator('.el-message-box')
    await expect(dialog).toContainText(definition.nextStage)
    await dialog.getByRole('button', { name: '确认变更', exact: true }).click()
    await expect(lawyerPage.getByText('已确认', { exact: true }).first()).toBeVisible()

    const updated = await api(lawyerPage, `/api/cases/${definition.caseId}`)
    expect(updated.payload.data.status).toBe('ACTIVE')
    expect(updated.payload.data.currentStage).toBe(definition.nextStage)
    const todos = await api(lawyerPage, `/api/todos/case/${definition.caseId}`)
    expect(todos.payload.data).toEqual(expect.arrayContaining([
      expect.objectContaining({ title: definition.todoTitle, status: 'PENDING' })
    ]))
  }

  const bypass = await api(lawyerPage, `/api/cases/${definitions[0].caseId}`, {
    method: 'PUT', body: { archiveDate: '2026-07-24' }
  })
  expect(bypass.status).toBe(400)
  expect(bypass.payload?.message).toContain('归档信息必须通过智能归档流程登记')
  const unchanged = await api(lawyerPage, `/api/cases/${definitions[0].caseId}`)
  expect(unchanged.payload.data.status).toBe('ACTIVE')

  const financeContext = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const financePage = await financeContext.newPage()
  await login(financePage, credentials('FINANCE'))
  const financeAttempt = await api(financePage, '/api/ai/case-commands', {
    method: 'POST',
    body: {
      caseId: definitions[0].caseId,
      instruction: `本案进入${definitions[0].skipStage}阶段`,
      idempotencyKey: `finance-stage-${suffix}`
    }
  })
  expect(financeAttempt.status).toBe(403)

  await Promise.all([
    lawyerContext.close(), administrativeContext.close(), directorContext.close(), financeContext.close()
  ])
})
