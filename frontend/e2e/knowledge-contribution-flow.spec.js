import { expect, test } from '@playwright/test'

const credentials = {
  director: { username: process.env.ZGAI_DIRECTOR_USERNAME, password: process.env.ZGAI_DIRECTOR_PASSWORD },
  lawyer: { username: process.env.ZGAI_LAWYER_USERNAME, password: process.env.ZGAI_LAWYER_PASSWORD },
  finance: { username: process.env.ZGAI_FINANCE_USERNAME, password: process.env.ZGAI_FINANCE_PASSWORD }
}

const assertEnvironment = () => {
  const missing = [
    'ZGAI_DIRECTOR_USERNAME', 'ZGAI_DIRECTOR_PASSWORD',
    'ZGAI_LAWYER_USERNAME', 'ZGAI_LAWYER_PASSWORD',
    'ZGAI_FINANCE_USERNAME', 'ZGAI_FINANCE_PASSWORD'
  ].filter(name => !process.env[name])
  if (process.env.RUN_KNOWLEDGE_REVIEW_E2E !== 'CONFIRMED') missing.push('RUN_KNOWLEDGE_REVIEW_E2E=CONFIRMED')
  if (missing.length) throw new Error(`缺少知识投稿E2E环境变量：${missing.join(', ')}`)
}

const login = async (page, role) => {
  await page.goto('/login')
  await page.getByPlaceholder('请输入用户名').fill(credentials[role].username)
  await page.getByPlaceholder('请输入密码').fill(credentials[role].password)
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/dashboard(?:$|\?)/)
}

const resetSession = async page => {
  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
}

const api = async (page, path, options = {}) => page.evaluate(async ({ path, options }) => {
  const token = localStorage.getItem('token')
  const response = await fetch(`/api${path}`, {
    ...options,
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json', ...(options.headers || {}) }
  })
  let body = null
  try { body = await response.json() } catch { /* no response body */ }
  return { status: response.status, body }
}, { path, options })

const createContribution = async (page, title) => {
  await page.goto('/knowledge/create')
  await expect(page.getByText('本次内容将提交知识管理员审核')).toBeVisible()
  await page.getByLabel('文章标题').fill(title)
  await page.getByLabel('文章内容').fill(`${title}。用于验证投稿审核、权限隔离和 RAG 准入。`)
  await page.getByRole('button', { name: '提交审核' }).click()
  await expect(page).toHaveURL(/\/knowledge\/list\?view=mine/)
  const item = page.locator('.article-item').filter({ hasText: title })
  await expect(item.getByText('待审核', { exact: true })).toBeVisible()
  const mine = await api(page, '/knowledge/my?page=0&size=20')
  return mine.body.data.content.find(article => article.title === title).id
}

test.beforeAll(assertEnvironment)

test('律师投稿经主任批准或驳回，全程权限隔离并通知投稿人', async ({ page }) => {
  test.setTimeout(120_000)
  const suffix = `${Date.now()}-${test.info().project.name}`
  const approvedTitle = `审核通过模板-${suffix}`
  const rejectedTitle = `需脱敏模板-${suffix}`
  const rejectionReason = '请删除客户名称后重新提交'
  const createdIds = []

  await login(page, 'lawyer')
  createdIds.push(await createContribution(page, approvedTitle))
  createdIds.push(await createContribution(page, rejectedTitle))

  await resetSession(page)
  await login(page, 'finance')
  const hidden = await api(page, `/knowledge/${createdIds[0]}`)
  const forbiddenReview = await api(page, `/knowledge/articles/${createdIds[0]}/review`, {
    method: 'POST', body: JSON.stringify({ decision: 'APPROVED', reason: '' })
  })
  expect(hidden.status).toBe(403)
  expect(forbiddenReview.status).toBe(403)

  await resetSession(page)
  await login(page, 'director')
  await page.goto('/knowledge/list')
  await page.getByText('待审核知识', { exact: true }).click()
  const approvedItem = page.locator('.article-item').filter({ hasText: approvedTitle })
  await approvedItem.getByRole('button', { name: '批准' }).click()
  await page.getByRole('button', { name: '确定' }).click()
  await expect(approvedItem).toHaveCount(0)

  const rejectedItem = page.locator('.article-item').filter({ hasText: rejectedTitle })
  await rejectedItem.getByRole('button', { name: '驳回' }).click()
  await page.locator('.el-message-box input').fill(rejectionReason)
  await page.getByRole('button', { name: '确定' }).click()
  await expect(rejectedItem).toHaveCount(0)

  await resetSession(page)
  await login(page, 'lawyer')
  await page.goto('/knowledge/list?view=mine')
  await expect(page.locator('.article-item').filter({ hasText: rejectedTitle }).getByText('已驳回')).toBeVisible()
  const notificationResult = await api(page, '/notification?page=1&size=50')
  const notifications = notificationResult.body.data.content
  expect(notifications.some(item => item.relatedId === createdIds[0] && item.title === '知识投稿已批准')).toBe(true)
  expect(notifications.some(item => item.relatedId === createdIds[1] && item.content.includes(rejectionReason))).toBe(true)

  const published = await api(page, `/knowledge/${createdIds[0]}`)
  expect(published.status).toBe(200)
  expect(published.body.data.reviewStatus).toBe('APPROVED')
  expect(published.body.data.knowledgeEligible).toBe(true)

  for (const id of createdIds) await api(page, `/knowledge/${id}`, { method: 'DELETE' })
})
