import { expect, test } from '@playwright/test'

const roleDefinitions = [
  {
    label: '普通律师',
    usernameEnv: 'ZGAI_LAWYER_USERNAME',
    passwordEnv: 'ZGAI_LAWYER_PASSWORD',
    dashboardText: '日程安排',
    calendarExpected: true
  },
  {
    label: '行政管理',
    usernameEnv: 'ZGAI_ADMINISTRATIVE_USERNAME',
    passwordEnv: 'ZGAI_ADMINISTRATIVE_PASSWORD',
    dashboardText: '待办事项',
    calendarExpected: false,
    coreRoute: '/approval',
    corePageText: '行政审批中心'
  },
  {
    label: '主任',
    usernameEnv: 'ZGAI_DIRECTOR_USERNAME',
    passwordEnv: 'ZGAI_DIRECTOR_PASSWORD',
    dashboardText: '主任工作台',
    calendarExpected: true,
    coreRoute: '/approval',
    corePageText: '主任审批中心'
  },
  {
    label: '财务',
    usernameEnv: 'ZGAI_FINANCE_USERNAME',
    passwordEnv: 'ZGAI_FINANCE_PASSWORD',
    dashboardText: '财务工作台',
    calendarExpected: false,
    coreRoute: '/finance/invoices',
    corePageText: '财务管理'
  }
]

const credentialsFor = (role) => ({
  username: process.env[role.usernameEnv],
  password: process.env[role.passwordEnv]
})

const assertEnvironment = () => {
  const missing = roleDefinitions.flatMap(role => [role.usernameEnv, role.passwordEnv])
    .filter(name => !process.env[name])
  if (missing.length > 0) {
    throw new Error(`缺少多角色浏览器 E2E 环境变量：${missing.join(', ')}`)
  }
}

const login = async (page, role) => {
  const credentials = credentialsFor(role)
  await page.goto('/login')
  await page.getByPlaceholder('请输入用户名').fill(credentials.username)
  await page.getByPlaceholder('请输入密码').fill(credentials.password)
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/dashboard(?:$|\?)/)
  await expect(page.getByText(role.dashboardText, { exact: false }).first()).toBeVisible()
}

const assertNoPageOverflow = async (page) => {
  await expect.poll(async () => page.evaluate(() =>
    document.documentElement.scrollWidth <= window.innerWidth
  )).toBe(true)
}

test.beforeAll(() => assertEnvironment())

for (const role of roleDefinitions) {
  test(`${role.label}工作台遵循角色边界并适配当前视口`, async ({ page }, testInfo) => {
    const consoleErrors = []
    page.on('console', message => {
      if (message.type() === 'error') consoleErrors.push(message.text())
    })
    page.on('pageerror', error => consoleErrors.push(error.message))

    await login(page, role)
    await assertNoPageOverflow(page)

    if (testInfo.project.name === 'mobile-chrome') {
      await page.locator('.collapse-btn').click()
      await expect(page.locator('.sidebar')).toHaveClass(/mobile-visible/)
      await expect(page.locator('.sidebar-menu')).toBeVisible()
      await expect(page.locator('.sidebar-menu').getByText('日程管理', { exact: true }))
        .toHaveCount(role.calendarExpected ? 1 : 0)
      await page.locator('.sidebar-overlay').click()
      await expect(page.locator('.sidebar')).not.toHaveClass(/mobile-visible/)
      await assertNoPageOverflow(page)
    }

    if (role.coreRoute) {
      await page.goto(role.coreRoute)
      await expect(page.getByText(role.corePageText, { exact: true }).first()).toBeVisible()
      await assertNoPageOverflow(page)
    }

    expect(consoleErrors, `浏览器控制台错误：${consoleErrors.join('\n')}`).toEqual([])
    await testInfo.attach(`${role.label}-${testInfo.project.name}`, {
      body: await page.screenshot({ fullPage: true }),
      contentType: 'image/png'
    })
  })
}
