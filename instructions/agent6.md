你是前端开发Agent-6，负责完整前端开发。

工作目录：D:\ZGAI\frontend

请读取 D:\ZGAI\PRD.md 和 D:\ZGAI\DEV_INSTRUCTIONS.md。

请开发：

## 基础架构
1. vite.config.js + main.js + .env.development
2. 路由配置（/login + MainLayout下10个一级路由+案件子路由）
3. Pinia状态管理（user/app/case stores）
4. API封装（request.js + auth/case/calendar/todo/client/finance/approval/ai/system/notification.js）
5. MainLayout.vue（侧边导航+顶部栏+主内容区）
6. 公共组件（PageHeader/SearchBar/DataTable/FormDrawer/FormDialog/StatusTag/PriorityDot/FileUpload/RichEditor/ConfirmDialog/Timeline）

## 页面开发
7. 登录页
8. 工作台（统计卡片+日历+待办+快捷入口）
9. 案件列表（筛选+表格+看板）
10. 新建案件（分区表单+AI填充+查重+当事人动态表单）
11. 案件详情（进度条+5个Tab：基本案情/办案记录/受理单位/案件文档/案件动态）
12. 日程管理（月/周/日视图+待办清单）
13. 客户管理（列表+详情+沟通记录）
14. 财务管理（费用/律师费/收款/开票/统计）
15. 审批管理（发起/待办/已办/详情）
16. 行政OA（公告/会议室/考勤）
17. 统计报表（ECharts图表）
18. 系统设置（用户/角色/配置/日志/备份）

严格按PRD第三节每个模块的字段和交互逻辑实现。不要省略任何页面代码。
