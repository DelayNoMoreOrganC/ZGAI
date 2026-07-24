import { expect, test } from '@playwright/test'

const credentials = {
  director: {
    username: process.env.ZGAI_DIRECTOR_USERNAME,
    password: process.env.ZGAI_DIRECTOR_PASSWORD
  },
  lawyer: {
    username: process.env.ZGAI_LAWYER_USERNAME,
    password: process.env.ZGAI_LAWYER_PASSWORD
  }
}

const assertEnvironment = () => {
  const missing = [
    'ZGAI_DIRECTOR_USERNAME', 'ZGAI_DIRECTOR_PASSWORD',
    'ZGAI_LAWYER_USERNAME', 'ZGAI_LAWYER_PASSWORD'
  ].filter(name => !process.env[name])
  if (process.env.RUN_RAG_EVALUATION_E2E !== 'CONFIRMED') {
    missing.push('RUN_RAG_EVALUATION_E2E=CONFIRMED')
  }
  if (missing.length) throw new Error(`缺少RAG评价E2E环境变量：${missing.join(', ')}`)
}

const login = async (page, role) => {
  await page.goto('/login')
  await page.getByPlaceholder('请输入用户名').fill(credentials[role].username)
  await page.getByPlaceholder('请输入密码').fill(credentials[role].password)
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/dashboard(?:$|\?)/)
}

const assertNoPageOverflow = async (page) => {
  await expect.poll(async () => page.evaluate(() =>
    document.documentElement.scrollWidth <= window.innerWidth
  )).toBe(true)
}

test.beforeAll(assertEnvironment)

test('知识管理员运行RAG评价且普通律师不能访问管理能力', async ({ page }) => {
  test.setTimeout(90_000)
  const sampleName = `劳动仲裁时效评价-${Date.now()}`

  await login(page, 'director')
  await page.goto('/knowledge/rag')
  await page.getByRole('button', { name: '检索评价' }).click()
  await expect(page.getByText('RAG 检索评价', { exact: true })).toBeVisible()
  await assertNoPageOverflow(page)
  await page.getByRole('button', { name: '新增样本' }).click()

  await page.getByLabel('样本名称').fill(sampleName)
  await page.getByLabel('评价问题').fill('劳动争议申请仲裁的时效期间是多久？')

  const expectedField = page.locator('.el-form-item').filter({ hasText: '预期命中文档' })
  await expectedField.locator('.el-select').click()
  await page.locator('.el-select-dropdown:visible')
    .getByText('劳动争议仲裁时效测试规则（#1）', { exact: true }).click({ force: true })

  const forbiddenField = page.locator('.el-form-item').filter({ hasText: '禁止命中文档' })
  await forbiddenField.locator('.el-select').click()
  await page.locator('.el-select-dropdown:visible')
    .getByText('禁止进入共享RAG的案件材料（#2）', { exact: false }).click({ force: true })

  await page.getByRole('button', { name: '保存样本' }).click()
  await expect(page.getByText(sampleName, { exact: true })).toBeVisible()
  await page.getByRole('button', { name: '运行启用样本' }).click()
  await expect(page.getByText('100%', { exact: true })).toBeVisible()
  await expect(page.getByText('越界命中').locator('..').getByText('0', { exact: true })).toBeVisible()
  await expect(page.getByText(sampleName, { exact: true }).last()).toBeVisible()
  await assertNoPageOverflow(page)

  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await login(page, 'lawyer')
  await page.goto('/knowledge/rag')
  await expect(page.getByRole('button', { name: '检索评价' })).toHaveCount(0)
  await assertNoPageOverflow(page)

  const checks = await page.evaluate(async () => {
    const token = localStorage.getItem('token')
    const evaluationResponse = await fetch('/api/knowledge/rag/evaluations', {
      headers: { Authorization: `Bearer ${token}` }
    })
    const searchResponse = await fetch('/api/knowledge/rag/search', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        question: '劳动争议申请仲裁的时效期间是多久？',
        providerType: 'LM_STUDIO'
      })
    })
    return {
      evaluationStatus: evaluationResponse.status,
      searchStatus: searchResponse.status,
      searchBody: await searchResponse.json()
    }
  })

  expect(checks.evaluationStatus).toBe(403)
  expect(checks.searchStatus).toBe(200)
  expect(checks.searchBody.data.sources.map(source => source.id)).toContain(1)
  expect(checks.searchBody.data.sources.map(source => source.id)).not.toContain(2)
})
