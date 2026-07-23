import { expect, test } from '@playwright/test'
import { createHash } from 'node:crypto'

const requiredEnvironment = [
  'ZGAI_LAWYER_USERNAME', 'ZGAI_LAWYER_PASSWORD',
  'ZGAI_ADMINISTRATIVE_USERNAME', 'ZGAI_ADMINISTRATIVE_PASSWORD',
  'ZGAI_DIRECTOR_USERNAME', 'ZGAI_DIRECTOR_PASSWORD',
  'ZGAI_FINANCE_USERNAME', 'ZGAI_FINANCE_PASSWORD'
]

const credentials = {
  lawyer: () => ({
    username: process.env.ZGAI_LAWYER_USERNAME,
    password: process.env.ZGAI_LAWYER_PASSWORD
  }),
  administrative: () => ({
    username: process.env.ZGAI_ADMINISTRATIVE_USERNAME,
    password: process.env.ZGAI_ADMINISTRATIVE_PASSWORD
  }),
  director: () => ({
    username: process.env.ZGAI_DIRECTOR_USERNAME,
    password: process.env.ZGAI_DIRECTOR_PASSWORD
  }),
  finance: () => ({
    username: process.env.ZGAI_FINANCE_USERNAME,
    password: process.env.ZGAI_FINANCE_PASSWORD
  })
}

const login = async (page, account) => {
  await page.goto('/login')
  await page.getByPlaceholder('请输入用户名').fill(account.username)
  await page.getByPlaceholder('请输入密码').fill(account.password)
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/dashboard(?:$|\?)/)
}

const selectOption = async (page, testId, option) => {
  await page.getByTestId(testId).click()
  await page.getByRole('option', { name: option, exact: true }).last().click()
}

const inputFor = (page, testId) => page
  .locator(`[data-testid="${testId}"] input, input[data-testid="${testId}"]`)
  .first()

const textareaFor = (page, testId) => page
  .locator(`[data-testid="${testId}"] textarea, textarea[data-testid="${testId}"]`)
  .first()

const fillNumber = async (page, testId, value) => {
  await inputFor(page, testId).fill(String(value))
}

const sha256 = (value) => createHash('sha256').update(value).digest('hex')

const readDownload = async (download) => {
  const stream = await download.createReadStream()
  const chunks = []
  for await (const chunk of stream) chunks.push(chunk)
  return Buffer.concat(chunks)
}

const createPdfFixture = async (context, title, lines) => {
  const fixture = await context.newPage()
  await fixture.setContent(`
    <main style="font-family: Arial, 'PingFang SC', sans-serif; padding: 64px; color: #111;">
      <h1 style="font-size: 34px; text-align: center; margin-bottom: 42px;">${title}</h1>
      ${lines.map(line => `<p style="font-size: 20px; line-height: 1.8;">${line}</p>`).join('')}
    </main>
  `)
  const buffer = await fixture.pdf({ format: 'A4', printBackground: true })
  await fixture.close()
  return buffer
}

const uploadCaseDocumentFixture = async (page, fileName, buffer, documentType = '法律文书') => {
  await page.getByTestId('case-document-upload-open').click()
  const dialog = page.locator('.case-document-upload-dialog')
  await expect(dialog).toBeVisible()
  await selectOption(page, 'case-document-type', documentType)
  await dialog.locator('input[type="file"]').setInputFiles({
    name: fileName,
    mimeType: 'application/pdf',
    buffer
  })
  await dialog.getByTestId('case-document-upload-submit').click()
  await expect(page.getByText('文件上传成功', { exact: true }).last()).toBeVisible()
}

const invoiceRowFor = (page, title) => page
  .locator('[data-testid^="invoice-row-"]')
  .filter({ hasText: title })
  .first()
  .locator('xpath=ancestor::tr')

const assertNoPageOverflow = async (page) => {
  await expect.poll(async () => page.evaluate(() =>
    document.documentElement.scrollWidth <= window.innerWidth
  )).toBe(true)
}

const authenticatedApi = async (page, url, { method = 'GET', body } = {}) => page.evaluate(
  async ({ requestUrl, requestMethod, requestBody }) => {
    const token = localStorage.getItem('token')
    const response = await fetch(requestUrl, {
      method: requestMethod,
      headers: {
        Authorization: `Bearer ${token}`,
        ...(requestBody ? { 'Content-Type': 'application/json' } : {})
      },
      body: requestBody ? JSON.stringify(requestBody) : undefined
    })
    let payload = null
    try {
      payload = await response.json()
    } catch {
      // Status-only responses are valid for access-control assertions.
    }
    return { status: response.status, payload }
  },
  { requestUrl: url, requestMethod: method, requestBody: body }
)

const futureDate = (daysAhead, hour, minute) => {
  const value = new Date()
  value.setDate(value.getDate() + daysAhead)
  value.setHours(hour, minute, 0, 0)
  return value
}

const chineseDateTime = value => `${value.getFullYear()}年${value.getMonth() + 1}月${value.getDate()}日${value.getHours()}:${String(value.getMinutes()).padStart(2, '0')}`

const approveFiling = async (page, caseName, comments) => {
  await page.goto('/approval')
  const card = page.locator('.filing-card').filter({ hasText: caseName }).first()
  await expect(card).toBeVisible()
  await card.getByRole('button', { name: '查看审批' }).click()
  const drawer = page.locator('.approval-detail-drawer')
  await expect(drawer.getByText(caseName, { exact: false }).first()).toBeVisible()
  await drawer.getByTestId('approval-approve').click()
  const dialog = page.locator('.el-message-box')
  await expect(dialog).toBeVisible()
  await dialog.locator('input').fill(comments)
  await dialog.getByRole('button', { name: /^(同意立案|终审通过)$/ }).click({ timeout: 5_000 })
  await expect(page.getByText('立案审批已通过', { exact: true })).toBeVisible()
}

test.beforeAll(() => {
  if (process.env.ZGAI_E2E_ENVIRONMENT !== 'ISOLATED'
    || process.env.ZGAI_E2E_CONFIRM !== 'RUN_BROWSER_WRITE_E2E') {
    throw new Error('写入型浏览器 E2E 仅可在隔离环境运行，请设置 ZGAI_E2E_ENVIRONMENT=ISOLATED 和 ZGAI_E2E_CONFIRM=RUN_BROWSER_WRITE_E2E')
  }
  const missing = requiredEnvironment.filter(name => !process.env[name])
  if (missing.length > 0) throw new Error(`缺少浏览器 E2E 环境变量：${missing.join(', ')}`)
})

test.describe.configure({ mode: 'serial' })
test.setTimeout(240_000)

test('律师建客户立案并完成文件、用印、发票和电子卷宗归档闭环', async ({ browser }) => {
  const suffix = Date.now().toString().slice(-8)
  const clientName = `E2E客户-${suffix}有限公司`
  const opponentName = `E2E相对方-${suffix}`
  const caseName = `${clientName} Vs ${opponentName}`

  const lawyerContext = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const lawyerPage = await lawyerContext.newPage()
  await login(lawyerPage, credentials.lawyer())

  await lawyerPage.goto('/client/create')
  await selectOption(lawyerPage, 'client-type', '企业')
  await selectOption(lawyerPage, 'client-role', '原告')
  await inputFor(lawyerPage, 'client-name').fill(clientName)
  await selectOption(lawyerPage, 'client-department', '民商法务部')
  await selectOption(lawyerPage, 'client-source-users', '验收律师')
  await lawyerPage.keyboard.press('Escape')
  await selectOption(lawyerPage, 'client-owner-users', '验收律师')
  await lawyerPage.keyboard.press('Escape')
  await lawyerPage.getByTestId('client-submit').click()
  await expect(lawyerPage).toHaveURL(/\/client\/list/)

  await lawyerPage.getByPlaceholder('搜索客户名称、案源人、承办人...').fill(clientName)
  await lawyerPage.keyboard.press('Enter')
  const clientRow = lawyerPage.getByRole('row').filter({ hasText: clientName }).last()
  await expect(clientRow).toBeVisible()
  await clientRow.getByRole('button', { name: '详情', exact: true }).click()
  await expect(lawyerPage.getByRole('heading', { name: clientName })).toBeVisible()
  const clientId = lawyerPage.url().match(/\/client\/(\d+)/)?.[1]
  expect(clientId).toBeTruthy()
  await lawyerPage.getByTestId('client-create-case').click()
  await expect(lawyerPage).toHaveURL(/\/case\/create\?clientId=/)

  await selectOption(lawyerPage, 'case-type', '民事诉讼')
  await inputFor(lawyerPage, 'case-name').fill(caseName)
  await inputFor(lawyerPage, 'case-reason').fill('买卖合同纠纷')
  await selectOption(lawyerPage, 'case-business-type', '公司')
  await selectOption(lawyerPage, 'case-trial-stages', '一审')
  await lawyerPage.keyboard.press('Escape')
  await selectOption(lawyerPage, 'case-owner', '验收律师（律师） - 民商法务部')

  await lawyerPage.getByTestId('case-add-party').click()
  await lawyerPage.getByTestId('party-type-0').getByText('单位', { exact: true }).click()
  await lawyerPage.getByTestId('party-client-0').click()
  await selectOption(lawyerPage, 'party-role-0', '原告')
  await selectOption(lawyerPage, 'party-name-0', clientName)

  await lawyerPage.getByTestId('case-add-party').click()
  await selectOption(lawyerPage, 'party-role-1', '被告')
  const opponentInput = inputFor(lawyerPage, 'party-name-1')
  await opponentInput.fill(opponentName)
  await lawyerPage.getByRole('option', { name: opponentName, exact: true }).last().click()

  await lawyerPage.getByTestId('case-fee-method').getByText('固定收费', { exact: true }).click()
  await fillNumber(lawyerPage, 'case-lawyer-fee', 10000)
  await fillNumber(lawyerPage, 'case-allocation-source', 100)
  await fillNumber(lawyerPage, 'case-allocation-department', 0)
  await fillNumber(lawyerPage, 'case-allocation-firm', 0)
  await lawyerPage.getByTestId('case-submit-approval').click()
  await expect(lawyerPage).toHaveURL(/\/case\/list/)
  await lawyerPage.getByPlaceholder('输入案件名称或案号').fill(caseName)
  await lawyerPage.keyboard.press('Enter')
  await expect(lawyerPage.getByText('待审批', { exact: true }).first()).toBeVisible()

  const administrativeContext = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const administrativePage = await administrativeContext.newPage()
  await login(administrativePage, credentials.administrative())
  await administrativePage.goto('/approval')
  const administrativeCard = administrativePage.locator('.filing-card').filter({ hasText: caseName }).first()
  await expect(administrativeCard).toBeVisible()
  await administrativeCard.getByRole('button', { name: '查看审批' }).click()
  const administrativeDrawer = administrativePage.locator('.approval-detail-drawer')
  await administrativeDrawer.getByRole('button', { name: '填写正式审查' }).click()
  await administrativeDrawer.getByText('无冲突，通过', { exact: true }).click()
  await textareaFor(administrativePage, 'conflict-review-conclusion')
    .fill('已核对全所客户及案件主体，未发现利益冲突。')
  await administrativeDrawer.getByTestId('conflict-review-submit').click()
  await expect(administrativePage.getByText('正式利冲审查已完成，结论已锁定', { exact: true })).toBeVisible()
  await administrativeDrawer.getByTestId('approval-approve').click()
  const administrativeDialog = administrativePage.locator('.el-message-box')
  await administrativeDialog.locator('input').fill('行政利冲审查通过，同意提交主任终审。')
  await administrativeDialog.getByRole('button', { name: '同意立案' }).click()
  await expect(administrativePage.getByText('立案审批已通过', { exact: true })).toBeVisible()

  const directorContext = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const directorPage = await directorContext.newPage()
  await login(directorPage, credentials.director())
  await approveFiling(directorPage, caseName, '主任终审通过，同意建立案件档案。')

  await lawyerPage.goto('/case/list')
  await lawyerPage.getByPlaceholder('输入案件名称或案号').fill(caseName)
  await lawyerPage.keyboard.press('Enter')
  await lawyerPage.getByText(caseName, { exact: true }).first().click()
  await expect(lawyerPage).toHaveURL(/\/case\/\d+\/basic/)
  await expect(lawyerPage.getByText(clientName, { exact: true }).first()).toBeVisible()
  await expect(lawyerPage.getByTestId('case-number')).toHaveText(/\[\d{4}\]粤至高.+字第\d{3}号/)
  await expect(lawyerPage.getByTestId('case-filing-date')).toHaveText(/\d{4}-\d{2}-\d{2}/)

  const caseId = lawyerPage.url().match(/\/case\/(\d+)\//)?.[1]
  expect(caseId).toBeTruthy()
  const lawFirmCaseNumber = (await lawyerPage.getByTestId('case-number').textContent())?.trim()
  expect(lawFirmCaseNumber).toBeTruthy()

  const hearingAt = futureDate(35, 9, 30)
  const todoAt = futureDate(30, 10, 0)
  const hearingLocation = `E2E第三审判庭-${suffix}`
  const todoTitle = `E2E提交庭前提纲-${suffix}`

  await lawyerPage.goto(`/ai/case-workbench?caseId=${caseId}`)
  await expect(lawyerPage.getByTestId('ai-case-workbench')).toBeVisible()
  await expect(lawyerPage.getByTestId('ai-command-case')).toContainText(caseName)

  const caseBeforeStageChange = await authenticatedApi(lawyerPage, `/api/cases/${caseId}`)
  expect(caseBeforeStageChange.status).toBe(200)
  expect(caseBeforeStageChange.payload.data.currentStage).toBe('接洽利冲')
  await textareaFor(lawyerPage, 'ai-command-instruction').fill('本案进入诉前准备阶段')
  await lawyerPage.getByTestId('ai-command-submit').click()
  await expect(lawyerPage.getByTestId('ai-command-clarification'))
    .toContainText('当前只能进入下一阶段「签约立案」')
  const caseAfterRejectedSkip = await authenticatedApi(lawyerPage, `/api/cases/${caseId}`)
  expect(caseAfterRejectedSkip.payload.data.currentStage).toBe('接洽利冲')

  await textareaFor(lawyerPage, 'ai-command-instruction').fill('本案进入签约立案阶段')
  await lawyerPage.getByTestId('ai-command-submit').click()
  await expect(lawyerPage.getByTestId('ai-stage-proposal')).toContainText('案件阶段尚未变更')
  await expect(lawyerPage.getByTestId('ai-stage-proposal')).toContainText('签约立案')
  const caseBeforeStageConfirmation = await authenticatedApi(lawyerPage, `/api/cases/${caseId}`)
  expect(caseBeforeStageConfirmation.payload.data.currentStage).toBe('接洽利冲')
  await lawyerPage.getByTestId('ai-command-confirm').click()
  const stageConfirmDialog = lawyerPage.locator('.el-message-box')
  await expect(stageConfirmDialog).toContainText('再次确认案件阶段变更')
  await expect(stageConfirmDialog).toContainText('签约立案')
  await stageConfirmDialog.getByRole('button', { name: '确认变更', exact: true }).click()
  await expect(lawyerPage.getByText('已确认', { exact: true }).first()).toBeVisible()

  const caseAfterStageConfirmation = await authenticatedApi(lawyerPage, `/api/cases/${caseId}`)
  expect(caseAfterStageConfirmation.payload.data.currentStage).toBe('签约立案')
  const stageTodos = await authenticatedApi(lawyerPage, `/api/todos/case/${caseId}`)
  expect(stageTodos.payload.data).toEqual(expect.arrayContaining([
    expect.objectContaining({ title: '完成民事委托和立案材料检查', status: 'PENDING' })
  ]))
  await lawyerPage.getByTestId('ai-command-view-timeline').click()
  await expect(lawyerPage.getByText(/案件阶段从「接洽利冲」变更为「签约立案」/).first()).toBeVisible()

  await lawyerPage.goto(`/ai/case-workbench?caseId=${caseId}`)
  await textareaFor(lawyerPage, 'ai-command-instruction').fill(`本案${chineseDateTime(hearingAt)}开庭`)
  await lawyerPage.getByTestId('ai-command-submit').click()
  await expect(lawyerPage.getByTestId('ai-command-clarification')).toContainText('请补充开庭或听证地点')
  const calendarsBeforeCompleteCommand = await authenticatedApi(lawyerPage, `/api/calendar/case/${caseId}`)
  expect(calendarsBeforeCompleteCommand.status).toBe(200)
  const calendarCountBefore = calendarsBeforeCompleteCommand.payload.data.length
  expect(calendarsBeforeCompleteCommand.payload.data.some(item => item.location === hearingLocation)).toBe(false)

  await textareaFor(lawyerPage, 'ai-command-instruction')
    .fill(`本案${chineseDateTime(hearingAt)}开庭，地点${hearingLocation}`)
  const hearingResponsePromise = lawyerPage.waitForResponse(response =>
    response.url().includes('/api/ai/case-commands') && response.request().method() === 'POST')
  await lawyerPage.getByTestId('ai-command-submit').click()
  const hearingResponse = await hearingResponsePromise
  const hearingPayload = await hearingResponse.json()
  expect(hearingResponse.status()).toBe(200)
  expect(hearingPayload.data.status).toBe('AUTO_EXECUTED')
  await expect(lawyerPage.getByTestId('ai-command-action-CREATE_CALENDAR')).toContainText(hearingLocation)
  await expect(lawyerPage.getByTestId('ai-command-view-calendar')).toBeVisible()
  await expect(lawyerPage.getByTestId('ai-command-view-timeline')).toBeVisible()

  const calendars = await authenticatedApi(lawyerPage, `/api/calendar/case/${caseId}`)
  expect(calendars.status).toBe(200)
  expect(calendars.payload.data).toHaveLength(calendarCountBefore + 1)
  expect(calendars.payload.data).toEqual(expect.arrayContaining([
    expect.objectContaining({ caseId: Number(caseId), location: hearingLocation, calendarType: 'HEARING' })
  ]))

  await lawyerPage.getByTestId('ai-command-view-timeline').click()
  await expect(lawyerPage).toHaveURL(new RegExp(`/case/${caseId}/timeline$`))
  await expect(lawyerPage.getByText(/已登记开庭：/).first()).toBeVisible()
  await expect(lawyerPage.getByText(new RegExp(hearingLocation)).first()).toBeVisible()

  await lawyerPage.goto(`/ai/case-workbench?caseId=${caseId}`)
  await textareaFor(lawyerPage, 'ai-command-instruction')
    .fill(`${chineseDateTime(todoAt)}提醒我${todoTitle}`)
  await lawyerPage.getByTestId('ai-command-submit').click()
  await expect(lawyerPage.getByTestId('ai-command-action-CREATE_TODO')).toContainText(todoTitle)
  await lawyerPage.getByTestId('ai-command-view-calendar').click()
  await expect(lawyerPage).toHaveURL(/\/calendar$/)
  await expect(lawyerPage.getByText(new RegExp(todoTitle)).first()).toBeVisible()

  const todos = await authenticatedApi(lawyerPage, `/api/todos/case/${caseId}`)
  expect(todos.status).toBe(200)
  expect(todos.payload.data).toEqual(expect.arrayContaining([
    expect.objectContaining({ caseId: Number(caseId), status: 'PENDING' })
  ]))

  const financeWriteContext = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const financeWritePage = await financeWriteContext.newPage()
  await login(financeWritePage, credentials.finance())
  await financeWritePage.goto(`/ai/case-workbench?caseId=${caseId}`)
  await expect(financeWritePage.getByTestId('ai-command-readonly-alert')).toBeVisible()
  await expect(textareaFor(financeWritePage, 'ai-command-instruction')).toBeDisabled()
  await expect(financeWritePage.getByTestId('ai-command-submit')).toBeDisabled()
  const financeCommand = await authenticatedApi(financeWritePage, '/api/ai/case-commands', {
    method: 'POST',
    body: {
      caseId: Number(caseId),
      instruction: `记录进展：${todoTitle}`,
      idempotencyKey: `finance-denied-${suffix}`
    }
  })
  expect(financeCommand.status).toBe(403)
  await financeWriteContext.close()

  const intakeFileName = `E2E民事传票-${suffix}.png`
  const fixturePage = await lawyerContext.newPage()
  await fixturePage.setViewportSize({ width: 1400, height: 900 })
  await fixturePage.setContent(`
    <main style="font-family: Arial, 'PingFang SC', sans-serif; padding: 70px; color: #111; background: white;">
      <h1 style="font-size: 58px; text-align: center; margin: 0 0 46px;">佛山市南海区人民法院 民事传票</h1>
      <p style="font-size: 42px; line-height: 1.8;">律所案号：${lawFirmCaseNumber}</p>
      <p style="font-size: 42px; line-height: 1.8;">案件名称：${caseName}</p>
      <p style="font-size: 42px; line-height: 1.8;">法院案号：（2026）粤0605民初12345号</p>
      <p style="font-size: 42px; line-height: 1.8;">开庭时间：2026年8月10日9时30分，地点：第三审判庭。</p>
      <p style="font-size: 42px; line-height: 1.8;">举证期限截至2026年8月5日。</p>
    </main>
  `)
  const intakeImage = await fixturePage.screenshot({ type: 'png', fullPage: true })
  await fixturePage.close()

  await lawyerPage.goto(`/ai/case-workbench?caseId=${caseId}&tab=intake`)
  await expect(lawyerPage.getByTestId('ai-intake-panel')).toBeVisible()
  await lawyerPage.getByTestId('ai-intake-upload').locator('input[type="file"]').setInputFiles({
    name: intakeFileName,
    mimeType: 'image/png',
    buffer: intakeImage
  })
  const intakeResponsePromise = lawyerPage.waitForResponse(response =>
    response.url().includes('/api/ai/document-intakes')
      && response.request().method() === 'POST'
      && !response.url().includes('/confirm'), { timeout: 90_000 })
  await lawyerPage.getByTestId('ai-intake-analyze').click()
  const intakeResponse = await intakeResponsePromise
  const intakePayload = await intakeResponse.json()
  expect(intakeResponse.status()).toBe(200)
  expect(intakePayload.data.status).toBe('ANALYZED')
  expect(String(intakePayload.data.analysis.courtCaseNumber)).toContain('粤0605民初12345号')
  expect(intakePayload.data.analysis.deadline).toBe('2026-08-05')
  expect(intakePayload.data.candidates[0].caseId).toBe(Number(caseId))
  expect(intakePayload.data.candidates[0].score).toBeGreaterThan(0)
  const intakeId = intakePayload.data.id
  await expect(lawyerPage.getByTestId('ai-intake-result')).toContainText('传票')
  await expect(lawyerPage.getByTestId('ai-intake-case')).toContainText(caseName)

  const administrativeIntakeStatus = await administrativePage.evaluate(async intakeIdValue => {
    const token = localStorage.getItem('token')
    const response = await fetch(`/api/ai/document-intakes/${intakeIdValue}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    return response.status
  }, intakeId)
  expect(administrativeIntakeStatus).toBe(403)

  await lawyerPage.getByTestId('ai-intake-confirm').click()
  await expect(lawyerPage.getByText('文件已归入案件并登记进展', { exact: false })).toBeVisible()
  await lawyerPage.goto(`/case/${caseId}/doc`)
  await expect(lawyerPage.locator('[data-testid^="case-document-"]').filter({ hasText: intakeFileName }).first()).toBeVisible()

  const documentName = `E2E代理意见-${suffix}.pdf`
  const documentBody = await createPdfFixture(lawyerContext, '民事案件代理意见', [
    `案件：${caseName}`,
    `律所案号：${lawFirmCaseNumber}`,
    '本文件仅用于隔离环境归档与用印流程测试。'
  ])
  const sealTitle = `${caseName}-${documentName}用印申请`

  await lawyerPage.goto(`/case/${caseId}/doc`)
  await lawyerPage.getByTestId('case-document-upload-open').click()
  const uploadDialog = lawyerPage.locator('.case-document-upload-dialog')
  await expect(uploadDialog).toBeVisible()
  await selectOption(lawyerPage, 'case-document-type', '法律文书')
  await uploadDialog.locator('input[type="file"]').setInputFiles({
    name: documentName,
    mimeType: 'application/pdf',
    buffer: documentBody
  })
  await uploadDialog.getByTestId('case-document-upload-submit').click()
  await expect(lawyerPage.getByText('文件上传成功', { exact: true })).toBeVisible()

  const documentCell = lawyerPage.locator('[data-testid^="case-document-"]').filter({ hasText: documentName }).first()
  await expect(documentCell).toBeVisible()
  const documentRow = documentCell.locator('xpath=ancestor::tr')
  await documentRow.getByRole('button', { name: '申请用印' }).click()
  const sealDialog = lawyerPage.locator('.case-document-seal-dialog')
  await expect(sealDialog).toBeVisible()
  await inputFor(lawyerPage, 'seal-approval-title').fill(sealTitle)
  await textareaFor(lawyerPage, 'seal-approval-content')
    .fill('申请加盖律所公章。用印用途：提交法院；用印份数：两份；提交对象：案件承办法院。')
  await sealDialog.getByTestId('seal-approval-submit').click()
  await expect(lawyerPage.getByText('用印申请已提交，并发送至行政人员待办', { exact: true })).toBeVisible()

  await administrativePage.goto('/approval')
  const sealCard = administrativePage.locator('.filing-card').filter({ hasText: sealTitle }).first()
  await expect(sealCard).toBeVisible()
  await sealCard.getByRole('button', { name: '查看审批' }).click()
  const sealDrawer = administrativePage.locator('.approval-detail-drawer')
  await expect(sealDrawer.getByText(sealTitle, { exact: true }).first()).toBeVisible()
  const sealAttachment = sealDrawer.locator('[data-testid^="seal-attachment-"]').filter({ hasText: documentName }).first()
  await expect(sealAttachment).toContainText('案件文件')
  const downloadPromise = administrativePage.waitForEvent('download')
  await sealAttachment.getByRole('button', { name: '下载审阅' }).click()
  const downloadedBody = await readDownload(await downloadPromise)
  expect(sha256(downloadedBody)).toBe(sha256(documentBody))
  await sealDrawer.getByTestId('approval-approve').click()
  const sealApprovalDialog = administrativePage.locator('.el-message-box')
  await sealApprovalDialog.locator('input').fill('文件内容及用印用途核对无误，同意用印。')
  await sealApprovalDialog.getByRole('button', { name: '同意用印' }).click()
  await expect(administrativePage.getByText('已同意用印', { exact: true })).toBeVisible()

  await lawyerPage.goto('/approval')
  const sealRequestRow = lawyerPage.getByRole('row').filter({ hasText: sealTitle }).last()
  await expect(sealRequestRow).toContainText('已同意')
  await expect(sealRequestRow.getByRole('button', { name: '撤回', exact: true })).toBeDisabled()
  await sealRequestRow.getByRole('button', { name: '查看', exact: true }).click()
  const lawyerSealDrawer = lawyerPage.locator('.approval-detail-drawer')
  await expect(lawyerSealDrawer.getByText('同意用印', { exact: true }).first()).toBeVisible()
  await expect(lawyerSealDrawer.getByText('文件内容及用印用途核对无误，同意用印。', { exact: true }).first()).toBeVisible()

  const archiveFixtures = [
    [`委托代理合同-${suffix}.pdf`, '委托代理合同', '合同收费'],
    [`授权委托书-${suffix}.pdf`, '授权委托书', '立案材料'],
    [`民事判决书-${suffix}.pdf`, '民事判决书', '法律文书']
  ]
  await lawyerPage.goto(`/case/${caseId}/doc`)
  for (const [fileName, title, documentType] of archiveFixtures) {
    const body = await createPdfFixture(lawyerContext, title, [
      `案件：${caseName}`,
      `法院案号：（2026）粤0605民初12345号`,
      `测试材料编号：${title}-${suffix}`
    ])
    await uploadCaseDocumentFixture(lawyerPage, fileName, body, documentType)
  }

  await lawyerPage.goto('/finance/invoices')
  const deletedInvoiceTitle = `E2E待删除开票-${suffix}`
  await lawyerPage.getByTestId('invoice-create-open').click()
  let invoiceDialog = lawyerPage.locator('.invoice-application-dialog')
  await inputFor(lawyerPage, 'invoice-title').fill(deletedInvoiceTitle)
  await fillNumber(lawyerPage, 'invoice-amount', 100)
  await invoiceDialog.getByTestId('invoice-submit').click()
  await expect(lawyerPage.getByText('发票申请已提交', { exact: true })).toBeVisible()
  let deletedInvoiceRow = invoiceRowFor(lawyerPage, deletedInvoiceTitle)
  await expect(deletedInvoiceRow).toContainText('待审查')
  await deletedInvoiceRow.getByRole('button', { name: '删除', exact: true }).click()
  const deleteDialog = lawyerPage.locator('.el-message-box')
  await deleteDialog.getByRole('button', { name: '删除', exact: true }).click()
  await expect(lawyerPage.getByText('开票申请已删除', { exact: true })).toBeVisible()
  await expect(lawyerPage.locator('[data-testid^="invoice-row-"]').filter({ hasText: deletedInvoiceTitle })).toHaveCount(0)

  const invoiceTitle = `${clientName}开票申请`
  const revisedRemark = `E2E修订后备注-${suffix}`
  const invoiceNumber = `E2E-INV-${suffix}`
  const invoiceBody = Buffer.from(`ZGAI invoice feedback browser fixture ${suffix}\n`, 'utf8')
  await lawyerPage.getByTestId('invoice-create-open').click()
  invoiceDialog = lawyerPage.locator('.invoice-application-dialog')
  await inputFor(lawyerPage, 'invoice-contract-no').fill(`HT-${suffix}`)
  await inputFor(lawyerPage, 'invoice-title').fill(invoiceTitle)
  await fillNumber(lawyerPage, 'invoice-amount', 10000)
  await lawyerPage.getByTestId('invoice-case').click()
  await lawyerPage.getByRole('option').filter({ hasText: caseName }).last().click()
  await inputFor(lawyerPage, 'invoice-department').fill('民商法务部')
  await inputFor(lawyerPage, 'invoice-source-user').fill('验收律师')
  await inputFor(lawyerPage, 'invoice-tax-number').fill(`91440600${suffix}`)
  await textareaFor(lawyerPage, 'invoice-remark').fill('初始备注')
  await invoiceDialog.getByTestId('invoice-submit').click()
  await expect(lawyerPage.getByText('发票申请已提交', { exact: true })).toBeVisible()

  let invoiceRow = invoiceRowFor(lawyerPage, invoiceTitle)
  await expect(invoiceRow).toContainText('待审查')
  await invoiceRow.getByRole('button', { name: '修改', exact: true }).click()
  invoiceDialog = lawyerPage.locator('.invoice-application-dialog')
  await fillNumber(lawyerPage, 'invoice-amount', 12600)
  await textareaFor(lawyerPage, 'invoice-remark').fill(revisedRemark)
  await invoiceDialog.getByTestId('invoice-submit').click()
  await expect(lawyerPage.getByText('发票申请已修改', { exact: true })).toBeVisible()
  invoiceRow = invoiceRowFor(lawyerPage, invoiceTitle)
  await expect(invoiceRow).toContainText('¥12,600')

  const financeContext = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const financePage = await financeContext.newPage()
  await login(financePage, credentials.finance())
  const financeIntakeStatus = await financePage.evaluate(async () => {
    const token = localStorage.getItem('token')
    const body = new FormData()
    body.append('file', new Blob(['read-only intake'], { type: 'text/plain' }), 'read-only.txt')
    const response = await fetch('/api/ai/document-intakes', {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
      body
    })
    return response.status
  })
  expect(financeIntakeStatus).toBe(403)
  await financePage.goto('/finance/invoices')
  let financeInvoiceRow = invoiceRowFor(financePage, invoiceTitle)
  await expect(financeInvoiceRow).toContainText('¥12,600')
  await financeInvoiceRow.getByRole('button', { name: '查看', exact: true }).click()
  const invoiceDetailDialog = financePage.locator('.invoice-detail-dialog')
  await expect(invoiceDetailDialog).toContainText(revisedRemark)
  await expect(invoiceDetailDialog).toContainText(caseName)
  await expect(invoiceDetailDialog).not.toContainText('/private/tmp')
  await invoiceDetailDialog.getByRole('button', { name: '关闭', exact: true }).click()

  await financeInvoiceRow.getByRole('button', { name: '开票反馈', exact: true }).click()
  const feedbackDialog = financePage.locator('.invoice-feedback-dialog')
  await inputFor(financePage, 'invoice-feedback-number').fill(invoiceNumber)
  await feedbackDialog.locator('input[type="file"]').setInputFiles({
    name: `电子发票-${suffix}.pdf`,
    mimeType: 'application/pdf',
    buffer: invoiceBody
  })
  await feedbackDialog.getByTestId('invoice-feedback-submit').click()
  await expect(financePage.getByText('开票反馈已提交', { exact: true })).toBeVisible()
  financeInvoiceRow = invoiceRowFor(financePage, invoiceTitle)
  await expect(financeInvoiceRow).toContainText('已反馈待完成')

  await lawyerPage.goto('/finance/invoices')
  invoiceRow = invoiceRowFor(lawyerPage, invoiceTitle)
  await expect(invoiceRow).toContainText('已反馈待完成')
  await expect(invoiceRow.getByRole('button', { name: '修改', exact: true })).toHaveCount(0)
  await expect(invoiceRow.getByRole('button', { name: '删除', exact: true })).toHaveCount(0)
  const invoiceDownloadPromise = lawyerPage.waitForEvent('download')
  await invoiceRow.getByRole('button', { name: '反馈文件', exact: true }).click()
  const downloadedInvoice = await readDownload(await invoiceDownloadPromise)
  expect(sha256(downloadedInvoice)).toBe(sha256(invoiceBody))

  await financeInvoiceRow.getByRole('button', { name: '完成开票', exact: true }).click()
  const completeDialog = financePage.locator('.el-message-box')
  await completeDialog.getByRole('button', { name: '确认完成', exact: true }).click()
  await expect(financePage.getByText('开票记录已完成并锁定', { exact: true })).toBeVisible()
  financeInvoiceRow = invoiceRowFor(financePage, invoiceTitle)
  await expect(financeInvoiceRow).toContainText('已完成')
  await expect(financeInvoiceRow.getByRole('button', { name: '开票反馈', exact: true })).toHaveCount(0)
  await expect(financeInvoiceRow.getByRole('button', { name: '完成开票', exact: true })).toHaveCount(0)

  await lawyerPage.goto('/finance/invoices')
  invoiceRow = invoiceRowFor(lawyerPage, invoiceTitle)
  await expect(invoiceRow).toContainText('已完成')
  await expect(invoiceRow.getByRole('button', { name: '反馈文件', exact: true })).toBeVisible()
  await expect(invoiceRow.getByRole('button', { name: '修改', exact: true })).toHaveCount(0)
  await expect(invoiceRow.getByRole('button', { name: '删除', exact: true })).toHaveCount(0)

  const mobileConsoleErrors = []
  for (const page of [lawyerPage, administrativePage, directorPage]) {
    page.on('console', message => {
      if (message.type() === 'error') mobileConsoleErrors.push(message.text())
    })
    page.on('pageerror', error => mobileConsoleErrors.push(error.message))
  }

  await lawyerPage.setViewportSize({ width: 390, height: 844 })
  await lawyerPage.goto(`/client/${clientId}`)
  await expect(lawyerPage.getByTestId('client-detail')).toBeVisible()
  await expect(lawyerPage.getByRole('heading', { name: clientName })).toBeVisible()
  await expect(lawyerPage.getByTestId('client-create-case')).toBeVisible()
  await assertNoPageOverflow(lawyerPage)

  await lawyerPage.goto(`/case/${caseId}/basic`)
  await expect(lawyerPage.getByTestId('case-detail')).toBeVisible()
  await expect(lawyerPage.getByTestId('case-detail-header')).toContainText(caseName)
  await expect(lawyerPage.getByTestId('case-tab-doc')).toBeVisible()
  await assertNoPageOverflow(lawyerPage)

  await lawyerPage.goto(`/ai/case-workbench?caseId=${caseId}`)
  await expect(lawyerPage.getByTestId('ai-case-workbench')).toBeVisible()
  await expect(lawyerPage.getByTestId('ai-workbench-header')).toContainText(caseName)
  await assertNoPageOverflow(lawyerPage)

  await administrativePage.setViewportSize({ width: 390, height: 844 })
  await administrativePage.goto(`/case/${caseId}/basic`)
  await expect(administrativePage.getByTestId('case-detail')).toBeVisible()
  await expect(administrativePage.getByTestId('case-detail-header')).toContainText(caseName)
  await assertNoPageOverflow(administrativePage)

  await directorPage.setViewportSize({ width: 390, height: 844 })
  await directorPage.goto(`/case/${caseId}/basic`)
  await expect(directorPage.getByTestId('case-detail')).toBeVisible()
  await expect(directorPage.getByTestId('case-detail-header')).toContainText(caseName)
  await assertNoPageOverflow(directorPage)
  expect(mobileConsoleErrors, `核心移动端页面控制台错误：${mobileConsoleErrors.join('\n')}`).toEqual([])

  await lawyerPage.setViewportSize({ width: 1440, height: 900 })
  const closeCaseStatus = await lawyerPage.evaluate(async caseIdValue => {
    const token = localStorage.getItem('token')
    const response = await fetch('/api/cases/batch/close', {
      method: 'PUT',
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({ caseIds: [Number(caseIdValue)] })
    })
    return response.status
  }, caseId)
  expect(closeCaseStatus).toBe(200)

  await lawyerPage.goto(`/case/${caseId}/archive`)
  await expect(lawyerPage.getByTestId('archive-start')).toBeVisible()
  await lawyerPage.getByTestId('archive-start').click()
  await expect(lawyerPage.getByTestId('archive-status')).toContainText('律师核对', { timeout: 120_000 })
  await expect(lawyerPage.getByText(`委托代理合同-${suffix}.pdf`, { exact: true })).toBeVisible()
  await expect(lawyerPage.getByText(`授权委托书-${suffix}.pdf`, { exact: true })).toBeVisible()
  await expect(lawyerPage.getByText(`民事判决书-${suffix}.pdf`, { exact: true })).toBeVisible()
  const conflictReportRow = lawyerPage.getByRole('row').filter({ hasText: '利冲审查报告_' }).first()
  await expect(conflictReportRow).toBeVisible()
  const conflictReportCheckbox = conflictReportRow.locator('.el-checkbox')
  await conflictReportCheckbox.click()
  await expect(conflictReportCheckbox.locator('input[type="checkbox"]')).not.toBeChecked()
  await lawyerPage.getByTestId('archive-submit').click()
  const archiveSubmitDialog = lawyerPage.locator('.el-message-box')
  await archiveSubmitDialog.getByRole('button', { name: '确定', exact: true }).click()
  await expect(lawyerPage.getByTestId('archive-status')).toContainText('行政复核')

  const archiveJobResponse = await lawyerPage.evaluate(async caseIdValue => {
    const token = localStorage.getItem('token')
    const response = await fetch(`/api/archive-jobs?caseId=${caseIdValue}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    return response.json()
  }, caseId)
  const archiveJobId = archiveJobResponse.data[0].id
  const financeArchiveReviewStatus = await financePage.evaluate(async jobIdValue => {
    const token = localStorage.getItem('token')
    const response = await fetch(`/api/archive-jobs/${jobIdValue}/review`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({ decision: 'APPROVE', reason: '越权测试' })
    })
    return response.status
  }, archiveJobId)
  expect(financeArchiveReviewStatus).toBe(403)

  await administrativePage.setViewportSize({ width: 1440, height: 900 })
  await administrativePage.goto(`/case/${caseId}/archive`)
  await expect(administrativePage.getByTestId('archive-status')).toContainText('行政复核')
  await administrativePage.getByTestId('archive-approve').click()
  await textareaFor(administrativePage, 'archive-review-reason').fill('材料目录及归档字段核对无误，同意归档。')
  await administrativePage.getByTestId('archive-review-confirm').click()
  await expect(administrativePage.getByTestId('archive-status')).toContainText('已归档', { timeout: 180_000 })
  await expect(administrativePage.getByText('缺页/重复', { exact: true })).toBeVisible()
  const archiveDownloadPromise = administrativePage.waitForEvent('download')
  await administrativePage.getByTestId('archive-download').click()
  const archivePdf = await readDownload(await archiveDownloadPromise)
  expect(archivePdf.subarray(0, 5).toString('ascii')).toBe('%PDF-')
  expect(archivePdf.length).toBeGreaterThan(5_000)

  const archivedCaseStatus = await lawyerPage.evaluate(async caseIdValue => {
    const token = localStorage.getItem('token')
    const response = await fetch(`/api/cases/${caseIdValue}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    const payload = await response.json()
    return payload.data.status
  }, caseId)
  expect(archivedCaseStatus).toBe('ARCHIVED')

  await financeContext.close()
  await directorContext.close()
  await administrativeContext.close()
  await lawyerContext.close()
})

test('法律顾问案件完成顾问单位、意见书上传和快速用印闭环', async ({ browser }) => {
  const suffix = Date.now().toString().slice(-8)
  const clientName = `E2E顾问单位-${suffix}有限公司`
  const caseName = `${clientName}2026年度常年法律顾问`
  const contactName = `顾问联系人-${suffix}`
  const opinionName = `法律意见书-${suffix}.pdf`
  const sealTitle = `${caseName}-${opinionName}用印申请`

  const lawyerContext = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const administrativeContext = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const directorContext = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const financeContext = await browser.newContext({ viewport: { width: 1440, height: 900 } })
  const lawyerPage = await lawyerContext.newPage()
  const administrativePage = await administrativeContext.newPage()
  const directorPage = await directorContext.newPage()
  const financePage = await financeContext.newPage()

  await login(lawyerPage, credentials.lawyer())
  await login(administrativePage, credentials.administrative())
  await login(directorPage, credentials.director())
  await login(financePage, credentials.finance())

  await lawyerPage.goto('/client/create')
  await selectOption(lawyerPage, 'client-type', '企业')
  await selectOption(lawyerPage, 'client-role', '顾问单位')
  await inputFor(lawyerPage, 'client-name').fill(clientName)
  await selectOption(lawyerPage, 'client-department', '民商法务部')
  await selectOption(lawyerPage, 'client-source-users', '验收律师')
  await lawyerPage.keyboard.press('Escape')
  await selectOption(lawyerPage, 'client-owner-users', '验收律师')
  await lawyerPage.keyboard.press('Escape')
  await lawyerPage.getByTestId('client-submit').click()
  await expect(lawyerPage).toHaveURL(/\/client\/list/)

  await lawyerPage.getByPlaceholder('搜索客户名称、案源人、承办人...').fill(clientName)
  await lawyerPage.keyboard.press('Enter')
  const clientRow = lawyerPage.getByRole('row').filter({ hasText: clientName }).last()
  await clientRow.getByRole('button', { name: '详情', exact: true }).click()
  await lawyerPage.getByTestId('client-create-case').click()

  await selectOption(lawyerPage, 'case-type', '法律顾问')
  await inputFor(lawyerPage, 'case-name').fill(caseName)
  await inputFor(lawyerPage, 'case-reason').fill('2026年度常年法律顾问服务')
  await selectOption(lawyerPage, 'case-business-type', '常年法律顾问')
  await expect(lawyerPage.getByTestId('case-consultant-client')).toContainText(clientName)
  await inputFor(lawyerPage, 'case-consultant-contact-name').fill(contactName)
  await inputFor(lawyerPage, 'case-consultant-contact-department').fill('法务部')
  await inputFor(lawyerPage, 'case-consultant-contact-title').fill('法务经理')
  await inputFor(lawyerPage, 'case-consultant-contact-phone').fill('0757-81234567')
  await inputFor(lawyerPage, 'case-consultant-contact-email').fill(`legal-${suffix}@example.test`)
  await lawyerPage.getByPlaceholder('选择开始日期').fill('2026-01-01')
  await lawyerPage.getByPlaceholder('选择开始日期').press('Enter')
  await lawyerPage.getByPlaceholder('选择结束日期').fill('2026-12-31')
  await lawyerPage.getByPlaceholder('选择结束日期').press('Enter')
  await lawyerPage.getByPlaceholder('选择续签提醒日期').fill('2026-11-30')
  await lawyerPage.getByPlaceholder('选择续签提醒日期').press('Enter')
  await selectOption(lawyerPage, 'case-consultant-service-scopes', '合同审查')
  await selectOption(lawyerPage, 'case-consultant-service-scopes', '日常咨询')
  await lawyerPage.keyboard.press('Escape')
  await inputFor(lawyerPage, 'case-consultant-response').fill('普通事项2个工作日内响应，紧急事项4小时内响应')
  await textareaFor(lawyerPage, 'case-consultant-included').fill('日常咨询、合同审查及法律培训')
  await textareaFor(lawyerPage, 'case-consultant-excluded').fill('诉讼、仲裁及专项尽职调查另行委托')
  await selectOption(lawyerPage, 'case-owner', '验收律师（律师） - 民商法务部')
  await lawyerPage.getByTestId('case-fee-method').getByText('固定收费', { exact: true }).click()
  await fillNumber(lawyerPage, 'case-lawyer-fee', 36000)
  await fillNumber(lawyerPage, 'case-allocation-source', 100)
  await fillNumber(lawyerPage, 'case-allocation-department', 0)
  await fillNumber(lawyerPage, 'case-allocation-firm', 0)
  await lawyerPage.getByTestId('case-submit-approval').click()
  await expect(lawyerPage).toHaveURL(/\/case\/list/)

  await administrativePage.goto('/approval')
  const filingCard = administrativePage.locator('.filing-card').filter({ hasText: caseName }).first()
  await expect(filingCard).toBeVisible()
  await filingCard.getByRole('button', { name: '查看审批' }).click()
  const administrativeDrawer = administrativePage.locator('.approval-detail-drawer')
  await expect(administrativeDrawer).toContainText('固定收费 36000元')
  await administrativeDrawer.getByRole('button', { name: '填写正式审查' }).click()
  await administrativeDrawer.getByText('无冲突，通过', { exact: true }).click()
  await textareaFor(administrativePage, 'conflict-review-conclusion')
    .fill('已核对全所顾问单位、客户及关联案件，未发现利益冲突。')
  await administrativeDrawer.getByTestId('conflict-review-submit').click()
  await administrativeDrawer.getByTestId('approval-approve').click()
  let decisionDialog = administrativePage.locator('.el-message-box')
  await decisionDialog.locator('input').fill('行政审查通过，同意提交主任终审。')
  await decisionDialog.getByRole('button', { name: '同意立案' }).click()
  await expect(administrativePage.getByText('立案审批已通过', { exact: true })).toBeVisible()
  await approveFiling(directorPage, caseName, '主任终审通过，同意建立顾问服务档案。')

  await lawyerPage.goto('/case/list')
  await lawyerPage.getByPlaceholder('输入案件名称或案号').fill(caseName)
  await lawyerPage.keyboard.press('Enter')
  await lawyerPage.getByText(caseName, { exact: true }).first().click()
  await expect(lawyerPage).toHaveURL(/\/case\/(\d+)\/basic/)
  const caseId = lawyerPage.url().match(/\/case\/(\d+)\//)?.[1]
  expect(caseId).toBeTruthy()
  await expect(lawyerPage.getByText(clientName, { exact: true }).first()).toBeVisible()
  await expect(lawyerPage.getByText(contactName, { exact: false }).first()).toBeVisible()
  await expect(lawyerPage.getByText('2026-01-01 至 2026-12-31', { exact: true })).toBeVisible()
  await expect(lawyerPage.getByText('合同审查,日常咨询', { exact: true })).toBeVisible()

  await lawyerPage.goto(`/case/${caseId}/doc`)
  await expect(lawyerPage.getByTestId('case-legal-opinion-upload-open')).toBeVisible()
  await lawyerPage.getByTestId('case-legal-opinion-upload-open').click()
  const uploadDialog = lawyerPage.locator('.case-document-upload-dialog')
  await expect(lawyerPage.getByTestId('case-document-type')).toContainText('法律意见书')
  const opinionBody = await createPdfFixture(lawyerContext, '法律意见书', [
    `顾问单位：${clientName}`,
    '事项：合同审查法律意见',
    '本文件仅用于隔离环境顾问服务及用印流程测试。'
  ])
  await uploadDialog.locator('input[type="file"]').setInputFiles({
    name: opinionName,
    mimeType: 'application/pdf',
    buffer: opinionBody
  })
  await uploadDialog.getByTestId('case-document-upload-submit').click()
  await expect(lawyerPage.getByText('文件上传成功', { exact: true }).last()).toBeVisible()
  const opinionCell = lawyerPage.locator('[data-testid^="case-document-"]').filter({ hasText: opinionName }).first()
  await expect(opinionCell).toBeVisible()
  const opinionRow = opinionCell.locator('xpath=ancestor::tr')
  await expect(opinionRow).toContainText('法律意见书')

  const financeUploadStatus = await financePage.evaluate(async ({ id, name }) => {
    const token = localStorage.getItem('token')
    const body = new FormData()
    body.append('documentType', '法律意见书')
    body.append('file', new Blob(['unauthorized'], { type: 'application/pdf' }), name)
    const response = await fetch(`/api/cases/${id}/documents`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
      body
    })
    return response.status
  }, { id: caseId, name: `越权意见书-${suffix}.pdf` })
  expect(financeUploadStatus).toBe(403)

  await opinionRow.getByRole('button', { name: '申请用印' }).click()
  const sealDialog = lawyerPage.locator('.case-document-seal-dialog')
  await inputFor(lawyerPage, 'seal-approval-title').fill(sealTitle)
  await textareaFor(lawyerPage, 'seal-approval-content')
    .fill('申请对顾问法律意见书加盖公章。用印用途：交付顾问单位；用印份数：一份；提交对象：顾问单位。')
  await sealDialog.getByTestId('seal-approval-submit').click()
  await expect(lawyerPage.getByText('用印申请已提交，并发送至行政人员待办', { exact: true })).toBeVisible()

  await administrativePage.goto('/approval')
  const sealCard = administrativePage.locator('.filing-card').filter({ hasText: sealTitle }).first()
  await expect(sealCard).toBeVisible()
  await sealCard.getByRole('button', { name: '查看审批' }).click()
  const sealDrawer = administrativePage.locator('.approval-detail-drawer')
  const sealAttachment = sealDrawer.locator('[data-testid^="seal-attachment-"]').filter({ hasText: opinionName }).first()
  const downloadPromise = administrativePage.waitForEvent('download')
  await sealAttachment.getByRole('button', { name: '下载审阅' }).click()
  expect(sha256(await readDownload(await downloadPromise))).toBe(sha256(opinionBody))
  await sealDrawer.getByTestId('approval-approve').click()
  decisionDialog = administrativePage.locator('.el-message-box')
  await decisionDialog.locator('input').fill('法律意见书内容及用印用途核对无误，同意用印。')
  await decisionDialog.getByRole('button', { name: '同意用印' }).click()
  await expect(administrativePage.getByText('已同意用印', { exact: true })).toBeVisible()

  await lawyerPage.goto('/approval')
  const sealRow = lawyerPage.getByRole('row').filter({ hasText: sealTitle }).last()
  await expect(sealRow).toContainText('已同意')

  await financeContext.close()
  await directorContext.close()
  await administrativeContext.close()
  await lawyerContext.close()
})
