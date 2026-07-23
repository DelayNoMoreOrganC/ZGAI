import { expect, test } from '@playwright/test'
import { createHash } from 'node:crypto'

const requiredEnvironment = [
  'ZGAI_LAWYER_USERNAME', 'ZGAI_LAWYER_PASSWORD',
  'ZGAI_ADMINISTRATIVE_USERNAME', 'ZGAI_ADMINISTRATIVE_PASSWORD',
  'ZGAI_DIRECTOR_USERNAME', 'ZGAI_DIRECTOR_PASSWORD'
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
test.setTimeout(120_000)

test('律师建客户并提交立案，完成两级审批、案件文件上传和快速用印', async ({ browser }) => {
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
  const documentName = `E2E用印材料-${suffix}.pdf`
  const documentBody = Buffer.from(`ZGAI seal approval browser fixture ${suffix}\n`, 'utf8')
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

  await directorContext.close()
  await administrativeContext.close()
  await lawyerContext.close()
})
