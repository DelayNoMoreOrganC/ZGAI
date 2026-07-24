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
  const suffix = `${Date.now()}-${test.info().project.name}`
  const sampleName = `劳动仲裁时效评价-${suffix}`
  const expectedTitle = `劳动争议仲裁时效测试规则-${suffix}`
  const forbiddenTitle = `禁止进入共享RAG的案件材料-${suffix}`

  await login(page, 'director')
  const fixtureArticles = await page.evaluate(async ({ expectedTitle, forbiddenTitle }) => {
    const token = localStorage.getItem('token')
    const create = async (body) => {
      const response = await fetch('/api/knowledge', {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
        body: JSON.stringify(body)
      })
      const result = await response.json()
      if (!response.ok || result.code !== 200) throw new Error(result.message || '创建RAG评价知识失败')
      return result.data
    }
    const expected = await create({
      title: expectedTitle,
      articleType: 'LEGAL_GUIDE',
      knowledgeSource: 'FIRM_KNOWLEDGE',
      content: '劳动争议申请仲裁的时效期间为一年。',
      isPublic: true,
      knowledgeEligible: true,
      validityStatus: 'EFFECTIVE'
    })
    const forbidden = await create({
      title: forbiddenTitle,
      articleType: 'CASE_MATERIAL',
      knowledgeSource: 'CASE_DEPOSIT',
      content: '劳动争议申请仲裁的时效期间为一年，但本材料禁止进入共享知识检索。',
      isPublic: false,
      knowledgeEligible: false,
      validityStatus: 'EFFECTIVE'
    })
    return { expected, forbidden }
  }, { expectedTitle, forbiddenTitle })

  await page.goto('/knowledge/rag')
  await page.getByRole('button', { name: '检索评价' }).click()
  await expect(page.getByText('RAG 检索评价', { exact: true })).toBeVisible()
  await assertNoPageOverflow(page)

  const candidateArticles = await page.evaluate(async () => {
    const token = localStorage.getItem('token')
    const response = await fetch('/api/knowledge/rag/evaluations/candidates', {
      headers: { Authorization: `Bearer ${token}` }
    })
    const body = await response.json()
    return body.data
  })
  const expectedArticle = candidateArticles.find(article => article.id === fixtureArticles.expected.id)
  const forbiddenArticle = candidateArticles.find(article => article.id === fixtureArticles.forbidden.id)
  expect(expectedArticle).toBeTruthy()
  expect(forbiddenArticle).toBeTruthy()

  await page.getByRole('button', { name: '新增样本' }).click()

  await page.getByLabel('样本名称').fill(sampleName)
  await page.getByLabel('评价问题').fill('劳动争议申请仲裁的时效期间是多久？')

  const expectedField = page.locator('.el-form-item').filter({ hasText: '预期命中文档' })
  await expectedField.locator('.el-select').click()
  await page.locator('.el-select-dropdown:visible')
    .getByText(`${expectedTitle}（#${expectedArticle.id}）`, { exact: true }).click({ force: true })

  const forbiddenField = page.locator('.el-form-item').filter({ hasText: '禁止命中文档' })
  await forbiddenField.locator('.el-select').click()
  await page.locator('.el-select-dropdown:visible')
    .getByText(`${forbiddenTitle}（#${forbiddenArticle.id}）`, { exact: false }).click({ force: true })

  await page.getByRole('button', { name: '保存样本' }).click()
  await expect(page.getByText(sampleName, { exact: true })).toBeVisible()
  const evaluationCaseId = await page.evaluate(async (name) => {
    const token = localStorage.getItem('token')
    const response = await fetch('/api/knowledge/rag/evaluations', {
      headers: { Authorization: `Bearer ${token}` }
    })
    const body = await response.json()
    return body.data.find(item => item.name === name)?.id
  }, sampleName)
  expect(evaluationCaseId).toBeTruthy()
  await page.getByRole('button', { name: '运行启用样本' }).click()
  const currentRun = page.locator('.el-table').last().locator('tr').filter({ hasText: sampleName })
  await expect(currentRun.getByText('命中', { exact: true })).toBeVisible()
  await expect(currentRun.getByText('无', { exact: true })).toBeVisible()
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
  expect(checks.searchBody.data.sources.map(source => source.id)).toContain(expectedArticle.id)
  expect(checks.searchBody.data.sources.map(source => source.id)).not.toContain(forbiddenArticle.id)

  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await login(page, 'director')
  await page.evaluate(async ({ evaluationCaseId, articleIds }) => {
    const token = localStorage.getItem('token')
    await fetch(`/api/knowledge/rag/evaluations/${evaluationCaseId}`, {
      method: 'DELETE', headers: { Authorization: `Bearer ${token}` }
    })
    for (const id of articleIds) {
      await fetch(`/api/knowledge/${id}`, {
        method: 'DELETE', headers: { Authorization: `Bearer ${token}` }
      })
    }
  }, { evaluationCaseId, articleIds: [expectedArticle.id, forbiddenArticle.id] })
})

test('知识管理员预检并批量导入Excel且普通律师被拒绝', async ({ page }) => {
  test.skip(!process.env.ZGAI_RAG_IMPORT_WORKBOOK, '未提供RAG评价Excel实例文件')
  test.setTimeout(60_000)
  const importedName = 'Excel导入-劳动仲裁材料'

  await login(page, 'director')
  await page.goto('/knowledge/rag')
  await page.getByRole('button', { name: '检索评价' }).click()
  await page.locator('input[type="file"]').setInputFiles(process.env.ZGAI_RAG_IMPORT_WORKBOOK)
  await page.getByRole('button', { name: '预检文件' }).click()
  await expect(page.getByText('允许确认导入', { exact: true })).toBeVisible()
  await expect(page.getByText('可导入 1 行', { exact: true })).toBeVisible()
  await page.getByRole('button', { name: '确认导入' }).click()
  await expect(page.getByText('已完成导入', { exact: true })).toBeVisible()
  await expect(page.getByRole('button', { name: '确认导入' })).toBeDisabled()
  await assertNoPageOverflow(page)

  let importedId
  await expect.poll(async () => {
    importedId = await page.evaluate(async (name) => {
      const token = localStorage.getItem('token')
      const response = await fetch('/api/knowledge/rag/evaluations', {
        headers: { Authorization: `Bearer ${token}` }
      })
      const body = await response.json()
      return body.data.find(item => item.name === name)?.id
    }, importedName)
    return importedId
  }).toBeTruthy()
  expect(importedId).toBeTruthy()

  await page.evaluate(async (id) => {
    const token = localStorage.getItem('token')
    await fetch(`/api/knowledge/rag/evaluations/${id}`, {
      method: 'DELETE', headers: { Authorization: `Bearer ${token}` }
    })
  }, importedId)

  await page.evaluate(() => {
    localStorage.clear()
    sessionStorage.clear()
  })
  await login(page, 'lawyer')
  const statuses = await page.evaluate(async () => {
    const token = localStorage.getItem('token')
    const headers = { Authorization: `Bearer ${token}` }
    const formData = new FormData()
    formData.append('file', new File(['not-an-xlsx'], '越权测试.xlsx', {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    }))
    const [template, importResponse] = await Promise.all([
      fetch('/api/knowledge/rag/evaluations/import-template', { headers }),
      fetch('/api/knowledge/rag/evaluations/import?dryRun=true', {
        method: 'POST', headers, body: formData
      })
    ])
    return [template.status, importResponse.status]
  })
  expect(statuses).toEqual([403, 403])
})
