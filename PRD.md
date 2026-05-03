# 🏛️ 律所智能案件管理系统 (ZGAI) — 产品需求文档

> **版本:** v2.0.0  
> **最后更新:** 2026-05-02  
> **状态:** 开发中（核心功能已完成，进入集成优化阶段）

---

## 一、项目概述

### 1.1 产品定位
面向中小型律所的一站式智能案件管理平台，集案件全生命周期管理、客户管理、财务管理、智能AI助手、知识库、外部工具集成于一体的SaaS化系统。

### 1.2 目标用户
- 律所主任 / 管理者 — 案件看板、数据统计、审批、绩效
- 主办律师 — 案件办理、日程、待办、文书、AI辅助
- 律师助理 — 案件创建、材料录入、时间线跟踪
- 行政人员 — OA审批、办公用品、会议室、考勤、固定资产

### 1.3 部署形态
- **开发阶段:** Vite Dev Server (port 3017) + Spring Boot (port 8080) + Flask 微服务
- **生产阶段:** Spring Boot 托管前端静态资源（npm run build 后）→ 单端口 8080 部署，支持局域网多用户

---

## 二、技术架构

### 2.1 技术栈

| 层级 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **前端框架** | Vue 3 (Composition API) | 3.5.32 | SPA，createWebHistory 路由 |
| **UI 库** | Element Plus | 2.13.7 | 中文语言包，完整图标库 |
| **构建工具** | Vite | 5.4.10 | ESM dev server, Rollup 构建 |
| **HTTP 客户端** | Axios | 1.15.0 | 请求/响应拦截器，JWT Token 注入 |
| **状态管理** | Pinia | — | 用户、应用、案件三种 store |
| **后端框架** | Spring Boot | 2.7.18 | Maven 项目，JDK 11 |
| **安全框架** | Spring Security | — | JWT Bearer Token 认证 |
| **数据库** | H2 (开发) / MySQL (生产计划) | — | 开发: `jdbc:h2:mem:lawfirm;MODE=MySQL` |
| **AI 集成** | DeepSeek API | — | 主AI，`deepseek-v4-flash` 模型 |
| **AI 备选** | Ollama (本地) | — | 本地推理，计划模型 `qwen3:8b` |
| **向量数据库** | Qdrant (本地) | — | 知识库 RAG 向量检索 |
| **嵌入模型** | Aliyun DashScope (text-embedding-v2) | — | 文本向量化 |
| **外部微服务** | Flask (Python) | — | SSB 省时宝 (port 5002)、AC 精算 (port 5100) |

### 2.2 架构图

```
┌─────────────────────────────────────────────────┐
│                    浏览器                         │
│          http://localhost:3017 (开发)               │
│          http://localhost:8080 (生产)               │
└──────────────┬──────────────────────┬────────────┘
               │ proxy /api → :8080   │
               ▼                      ▼
┌──────────────────────┐  ┌────────────────────────┐
│   Vite Dev Server    │  │  Spring Boot Backend   │
│   (port 3017)        │  │  (port 8080)           │
│   · Hot Reload       │  │  · REST API (/api)     │
│   · ESM Modules      │  │  · JWT Auth            │
│   · 源码编译          │  │  · 静态文件服务(生产)    │
└──────────────────────┘  └────────┬───────────────┘
                                   │
                    ┌──────────────┼──────────────┐
                    ▼              ▼              ▼
          ┌─────────────┐  ┌───────────┐  ┌──────────────┐
          │  H2 数据库   │  │DeepSeek AI│  │ Flask 微服务  │
          │ (内存/文件)   │  │ (云端API)  │  │ · SSB (:5002) │
          └─────────────┘  └───────────┘  │ · AC  (:5100) │
                                          └──────────────┘
```

---

## 三、功能模块详情

### 3.1 📊 工作台 (Dashboard)

| 功能 | 状态 | 说明 |
|------|------|------|
| 统计卡片（当月案件、进行中、开庭、待办、收费） | ✅ 已完成 | 后端 `DashboardController` 提供数据 |
| 月/周日历视图 | ✅ 已完成 | 展示日程安排，支持点击查看详情 |
| 待办事项列表 | ✅ 已完成 | 按逾期/优先级排序，逾期红色高亮 |
| AI智能文档上传识别 | ✅ 已完成 | 拖拽上传，自动识别创建待办/日程 |
| 快捷入口（新建案件/客户/AI助手/上传） | ✅ 已完成 | 6个卡片入口 |
| **省时宝快捷卡片** | ✅ **新增** | 粉色卡片，点击跳转工具集 |
| **AC精算快捷卡片** | ✅ **新增** | 橙色卡片，点击跳转工具集 |

### 3.2 ⚖️ 案件管理 (Case)

| 功能 | 状态 | 说明 |
|------|------|------|
| 案件列表 | ✅ 已完成 | 分页、搜索、筛选、批量操作 |
| 新建案件 | ✅ 已完成 | 含当事人、费用、收费方式 |
| 案件详情（5个Tab页） | ✅ 已完成 | |
| ├─ 基本案情 (basic) | ✅ 已完成 | 案情摘要、当事人、受理单位 |
| ├─ 办案记录 (record) | ✅ 已完成 | 工作日志时间线 |
| ├─ 受理单位 (unit) | ✅ 已完成 | 法院/仲裁机构等 |
| ├─ 案件文档 (doc) | ✅ 已完成 | 文件上传/OCR/下载 |
| └─ 案件动态 (timeline) | ✅ 已完成 | 状态变更历史 |
| 案件编辑 | ✅ 已完成 | 复用创建页面 |
| 归档库 | ✅ 已完成 | 已结案案件归档管理 |
| 回收站 | ✅ 已完成 | 软删除管理 |
| 类案检索 | ✅ 已完成 | 案件相似性搜索 |

### 3.3 👥 客户管理 (Client)

| 功能 | 状态 | 说明 |
|------|------|------|
| 客户列表 | ✅ 已完成 | 搜索筛选 |
| 新建客户 | ✅ 已完成 | 含类型、等级、联系方式 |
| 客户详情 | ✅ 已完成 | 关联案件列表 |
| 客户编辑 | ✅ 已完成 | 复用创建页面 |

### 3.4 🤖 AI 智能助手

| 功能 | 状态 | 说明 |
|------|------|------|
| 通用法律问答 (DeepSeek) | ✅ **已完成** | 法律角色设定、专业回复 |
| 案件上下文问答 | ✅ 已完成 | 基于案件信息进行分析 |
| 浮动 AI 按钮（全站） | ✅ 已完成 | 头部右上角，点击弹出 |
| 最小化/展开切换 | ✅ 已完成 | ➖ 按钮最小化为悬浮球 |
| 双AI模式（DeepSeek / Ollama） | ✅ 已完成 | `ai_config` 表 `is_default` 切换 |
| AI 配置管理后端 | ✅ 已完成 | `AIConfigController` + `AIConfigService` |
| AI 使用日志 | ✅ 已完成 | `AILog` 实体记录每次调用 |
| 错误处理优化 | ✅ **已修复** | 数据库字段加 `@Lob` 防超长截断 |
| 权限兼容（无JWT访问） | ✅ **已修复** | `getCurrentUserId()` 回退 admin |
| DeepSeek API 自动重试 | ✅ **已修复** | 网络波动自动重试2次 |

### 3.5 📁 文档中心 (Document)

| 功能 | 状态 | 说明 |
|------|------|------|
| 文档列表 | ✅ 已完成 | 所有案件文档统一视图 |
| 文档 OCR 识别 | ✅ 已完成 | `AIDocumentController` |
| 文档 AI 提取 | ✅ 已完成 | `AIFeaturesController` |
| 文档自动填充 | ✅ 已完成 | 基于AI提取信息自动填充案件 |
| 分块上传 | ✅ 已完成 | 大文件分块上传 |

### 3.6 💰 财务管理 (Finance)

| 功能 | 状态 | 说明 |
|------|------|------|
| 财务总览看板 | ✅ 已完成 | |
| 费用管理 | ✅ 已完成 | |
| 收支记录 | ✅ 已完成 | |
| 发票管理 | ✅ 已完成 | |

### 3.7 ✅ 审批管理 (Approval)

| 功能 | 状态 | 说明 |
|------|------|------|
| 审批列表 | ✅ 已完成 | |
| 审批流管理 | ✅ 已完成 | `ApprovalFlow` 实体 |
| 待办审批 | ✅ 已完成 | |

### 3.8 📚 知识库 (Knowledge)

| 功能 | 状态 | 说明 |
|------|------|------|
| 知识文章管理 | ✅ 已完成 | CRUD、分类、搜索 |
| AI 知识问答 (RAG) | ✅ 已完成 | `RAGKnowledgeController` + `QdrantVectorService` |
| 向量嵌入 | ✅ 已完成 | Aliyun DashScope text-embedding-v2 |
| 知识库初始化 | ✅ 已完成 | `KnowledgeArticleInitService` |

### 3.9 🏢 行政管理 (Admin OA)

| 功能 | 状态 | 说明 |
|------|------|------|
| 公告管理 | ✅ 已完成 | `AnnouncementController` |
| 考勤记录 | ✅ 已完成 | `AttendanceController` |
| 会议室管理 | ✅ 已完成 | `MeetingRoomController` + 预约 |
| 办公用品管理 | ✅ 已完成 | `OfficeSuppliesController` |
| 固定资产管理 | ✅ 已完成 | `FixedAssetController` |
| 公文流转 | ✅ 已完成 | `DocumentFlow` 路由 |

### 3.10 📈 统计报表 (Statistics)

| 功能 | 状态 | 说明 |
|------|------|------|
| 总览统计 | ✅ 已完成 | 案件/收费概览 |
| 案件趋势图 | ✅ 已完成 | |
| 案件类型分布 | ✅ 已完成 | |
| 收费统计 | ✅ 已完成 | |
| 律师绩效 | ✅ 已完成 | |
| 胜诉率/回款率 | ✅ 已完成 | |
| Excel/PDF 导出 | ✅ 已完成 | |

### 3.11 🏦 不良资产管理 (NPA)

| 功能 | 状态 | 说明 |
|------|------|------|
| 资产包管理 | ✅ 已完成 | CRUD、详情 |
| 债权管理 | ✅ 已完成 | 资产详情 |
| 尽调管理 | ✅ 已完成 | 尽职调查 |
| 处置跟踪 | ✅ 已完成 | 处置计划与结果 |
| 绩效看板 | ✅ 已完成 | |

### 3.12 🔧 工具集 (Tools)

| 功能 | 状态 | 说明 |
|------|------|------|
| 诉讼费计算器 | ✅ 已完成 | 支持财产/离婚案件 |
| 利息计算器 | ✅ 已完成 | 本金×利率×天数 |
| 时效计算器 | ✅ 已完成 | 截止日期计算 |
| **省时宝集成** | 🟡 **待开发** | 调用 SSB API `/api/generate` 文档生成 |
| **AC精算集成** | 🟡 **待开发** | 调用 AC API 债权利息计算 |

### 3.13 ⏱ 省时宝 (SSB) — 外部微服务

| 功能 | 状态 | 说明 |
|------|------|------|
| API 健康检查 | ✅ 已完成 | `/api/health` |
| 模板管理 | ✅ 已完成 | `/api/templates` 列表/文件/字段 |
| 文档生成 | ✅ 已完成 | `/api/generate` 基于模板生成 |
| PDF 提取 | ✅ 已完成 | `/api/extract-pdf` |
| 前端集成页面 | 🟡 **待开发** | 工具集页面调用，或独立页面 |

### 3.14 📊 AC 精算 — 外部微服务

| 功能 | 状态 | 说明 |
|------|------|------|
| 债权利息计算 (Streamlit) | ✅ 已完成 | API 层运行在 port 5100 |
| 自动填充 | ✅ 已完成 | Tesseract OCR + AI 提取 |
| 历史记录 | ✅ 已完成 | 计算结果保存 |
| 前端集成页面 | 🟡 **待开发** | 工具集页面调用 |

### 3.15 ⚙️ 系统管理 (Settings/Admin)

| 功能 | 状态 | 说明 |
|------|------|------|
| 用户管理 | ✅ 已完成 | `UserController` / `UserControllerCompat` |
| 角色权限管理 | ✅ 已完成 | RBAC: Role → Permission |
| AI 配置面板 | ✅ 已完成 | DeepSeek/Ollama 切换 |
| 系统配置 | ✅ 已完成 | `SystemConfigController` |
| 数据备份 | ✅ 已完成 | `BackupController` |
| 操作审计日志 | ✅ 已完成 | `AuditLogController` |
| 数据库测试 | ✅ 已完成 | `DatabaseTestController` |

### 3.16 🔔 通知与待办

| 功能 | 状态 | 说明 |
|------|------|------|
| 通知面板 | ✅ 已完成 | 全站右上角红点提示 |
| 待办事项管理 | ✅ 已完成 | 全功能 CRUD + 优先级 |
| 案件阶段待办模板 | ✅ 已完成 | `CaseStageTodoTemplate` |
| 期限提醒服务 | ✅ 已完成 | `DeadlineReminderService` |

---

## 四、API 路由概览

| 模块 | 基础路径 | 控制器 |
|------|---------|--------|
| 认证 | `/auth/*` | `AuthController` |
| 用户 | `/users`, `/user/*` | `UserController`, `UserControllerCompat` |
| 案件 | `/cases`, `/case/*` | `CaseController`, `CaseBatchController` |
| 客户 | `/clients` | `ClientController` |
| 待办 | `/todos` | `TodoController` |
| 财务 | `/finance` | `FinanceController` |
| 审批 | `/approvals` | `ApprovalController`, `ApprovalControllerCompat` |
| 文档 | `/documents` | `DocumentControllerCompat`, `CaseDocumentController` |
| AI助手 | `/ai/assist`, `/ai/chat`, `/ai/config` | `AiChatController`, `AIConfigController` |
| AI文档 | `/ai/documents/*` | `AIDocumentController` |
| AI日志 | `/ai/logs` | `AILogController` |
| 知识库 | `/knowledge/*` | `KnowledgeArticleController`, `RAGKnowledgeController` |
| 统计 | `/statistics` | `StatisticsController` |
| 日历 | `/calendar` | `CalendarController` |
| 外部集成 | `/external/*` | `ExternalApiController` |
| 通知 | `/notification` | `NotificationController` |
| 部门 | `/departments` | `DepartmentController` |
| NPA | `/npa/*` | `NpaPackageController`, `NpaAssetController`... |
| 系统 | `/system/*` | `SystemConfigController`, `BackupController` |
| 审计 | `/audit-logs` | `AuditLogController` |
| 工具集 | `/tools` | 前端页面，无专用后端 |

---

## 五、数据模型 (核心实体 60+)

| 分类 | 实体 | 数量 |
|------|------|------|
| 用户与权限 | User, Role, Permission, UserRole, RolePermission | 5 |
| 案件 | Case, CaseRecord, CaseDocument, CaseProcedure, CaseStage, CaseTimeline, CaseMember, CasePersonnel, Party, CaseStatusHistory, CaseExecution, PropertyPreservation, CommunicationRecord, HearingRecord | 14 |
| 案件流程 | CaseFlowTemplate, CaseStageTodoTemplate, StageTodoTemplate | 3 |
| 客户 | Client | 1 |
| 财务 | FinanceRecord, Payment, Invoice | 3 |
| 审批 | Approval, ApprovalFlow | 2 |
| AI | AIConfig, AILog | 2 |
| 知识库 | KnowledgeArticle | 1 |
| NPA | NpaPackage, NpaAsset, NpaDueDiligence, NpaDisposalPlan, NpaDisposalResult | 5 |
| 行政 | MeetingRoom, MeetingBooking, OfficeSupplies/Supply, OfficeSupply/SupplyRecord, FixedAsset, Announcement, AnnouncementRead, AttendanceRecord | 10 |
| 待办 | Todo, WorkReport | 2 |
| 系统 | SystemConfig, Dictionary, Department, DataBackup, AuditLog, Notification | 6 |
| 其他 | Calendar, Dossier, BaseEntity, LogicalDeleteEntity | 4 |
| **总计** | | **~58** |

---

## 六、开发进度总览

### 6.1 完成度矩阵

| 模块 | 后端 | 前端 | 集成测试 | 优先级 |
|------|------|------|---------|--------|
| 工作台 Dashboard | ✅ 100% | ✅ 100% | ✅ | P0 |
| 案件管理 Case | ✅ 100% | ✅ 100% | ✅ | P0 |
| 客户管理 Client | ✅ 100% | ✅ 100% | ✅ | P0 |
| AI 智能助手 | ✅ 100% | ✅ 100% | ✅ | P0 |
| 文档中心 Document | ✅ 100% | ✅ 100% | 🟡 部分 | P0 |
| 财务管理 Finance | ✅ 100% | ✅ 100% | 🟡 部分 | P0 |
| 审批管理 Approval | ✅ 100% | ✅ 100% | 🟡 部分 | P1 |
| 知识库 Knowledge | ✅ 100% | ✅ 100% | ✅ | P1 |
| 行政管理 Admin OA | ✅ 100% | ✅ 100% | 🟡 部分 | P1 |
| 统计报表 Statistics | ✅ 100% | ✅ 100% | 🟡 部分 | P1 |
| 不良资产 NPA | ✅ 100% | ✅ 100% | 🟡 部分 | P2 |
| 类案检索 CaseSearch | ✅ 100% | ✅ 100% | 🟡 部分 | P2 |
| 工具集 Tools | ✅ 100% | ✅ 100% | 🟡 部分 | P2 |
| **省时宝 SSB 独立 App** | ✅ 100% | ✅ **构建完成** | ✅ **:3000 运行中** | P1 |
| **AC 精算独立 App** | ✅ 100% | 🟡 需安装 Streamlit | ❌ 网络问题暂缓 | P1 |
| 系统管理 Settings | ✅ 100% | ✅ 100% | 🟡 部分 | P1 |

### 6.2 独立应用集成架构

```
用户访问 ZGAI 工作台
       │
       ├─ 工作台卡片「省时宝」→ 新标签页 http://localhost:3000 (Vue 3 独立前端)
       │
       ├─ 工作台卡片「AC精算」→ 新标签页 http://localhost:8501 (Streamlit 独立应用)
       │
       └─ 工具集页面 → 本地小工具 (诉讼费/利息/时效计算器)
```

### 6.3 当前运行服务一览

| 服务 | 端口 | 说明 |
|------|------|------|
| ZGAI 前端 (Vite Dev) | 3017 | 主系统前端 |
| ZGAI 后端 (Spring Boot) | 8080 | 主系统 API + AI |
| **省时宝 API** (Flask) | 5002 | 文档模板/生成 API |
| **省时宝独立前端** (Vite) | 3000 | SSB 完整 Vue 3 客户端 ✅ |
| **AC精算 API** (Flask) | 5100 | 债权计算 API |
| AC精算独立前端 (Streamlit) | 8501 | 🟡 需安装 streamlit 后启动 |
| SSB 完整仓库 | ssb-repo/ | 含服务端 + 客户端源码 |

### 6.2 待办事项 (按优先级)

| 优先级 | 任务 | 说明 |
|--------|------|------|
| **P0** | 数据库迁移 MySQL | 生产前必须完成，H2 内存数据库重启丢失数据 |
| **P0** | 多用户部署文档 | Nginx 配置 / Docker Compose |
| **P1** | 省时宝前端集成 | 在工具集页面调用 SSB API 文档生成 |
| **P1** | AC精算前端集成 | 嵌入 Streamlit 或封装为 iframe/API 调用 |
| **P1** | 生产环境 API Key 管理 | 从环境变量读取改为加密配置中心 |
| **P2** | 性能优化: JS 包体积 | 当前主入口 1.1MB，Element Plus 1.0MB，需 Code Splitting |
| **P2** | 移动端适配 | 已有基本响应式，需完善 |
| **P2** | 国际化 i18n | 当前仅中文 |
| **P2** | E2E 测试 | Cypress/Playwright 自动化 |
| **P3** | 容器化部署 | Dockerfile + docker-compose.yml |
| **P3** | CI/CD 流水线 | GitHub Actions |

---

## 七、已知问题 (Known Issues)

| 问题 | 影响 | 当前状态 |
|------|------|---------|
| `@Index` 注解导致 H2 MySQL 模式表丢失 | 数据表无法自动创建 | ⚠️ 注意避免使用 |
| H2 内存数据库重启数据丢失 | 开发频繁重启需重新初始化 | 🟡 计划迁移 MySQL |
| Vite Dev 模式页面加载 200+ 请求 | 开发环境首次访问慢 | ✅ 加载遮罩已缓解 |
| 构建产物较大 (1.1MB + 1.0MB) | 首屏加载时间较长 | 🟡 需 Code Splitting |
| DeepSeek API 偶发网络波动 | 偶尔超时 | ✅ 已加自动重试 |
| 部分 API 端点缺 JWT 返回 403 | 前端直接调用报错 | ✅ AI 端点已修复 |

---

## 八、部署指南（草稿）

### 8.1 开发环境启动

```bash
# 1. 后端 (终端1)
cd backend
export JAVA_HOME=/opt/homebrew/opt/openjdk@11
DEEPSEEK_API_KEY="sk-xxx" mvn spring-boot:run

# 2. 前端 (终端2)
cd frontend
npm run dev   # → localhost:3017

# 3. 省时宝 (终端3)
cd ssb
python3 ssb_api.py   # → localhost:5002

# 4. AC精算 (终端4)
cd ac-calc
python3 api_service.py  # → localhost:5100
```

### 8.2 多用户生产部署

```bash
# 1. 构建前端
cd frontend && npm run build  # → dist/

# 2. 启动后端（自动服务静态文件）
cd backend
DEEPSEEK_API_KEY="sk-xxx" mvn spring-boot:run
# → http://你的IP:8080 即可访问
```

### 8.3 推荐生产架构

```
Nginx (port 80/443)
├── / → 前端静态文件 (proxy_pass to build/dist)
├── /api → Spring Boot (proxy_pass to localhost:8080/api)
├── /ssb → SSB Flask (proxy_pass to localhost:5002)
└── /ac → AC Flask (proxy_pass to localhost:5100)
```

---

## 九、附录

### 9.1 项目结构

```
/Users/juno/ZGAI/
├── backend/               # Spring Boot 后端
│   └── src/main/java/com/lawfirm/
│       ├── config/        # 配置类 (Web, Security, Jackson, DataInitializer)
│       ├── controller/    # 51个控制器
│       ├── dto/           # 数据传输对象
│       ├── entity/        # 58个数据实体
│       ├── enums/         # 24个枚举类型
│       ├── exception/     # 全局异常处理
│       ├── init/          # 初始化服务
│       ├── repository/    # JPA 仓库
│       ├── security/      # JWT 认证/过滤器
│       ├── service/       # 63个服务类
│       ├── util/          # 工具类
│       └── vo/            # 视图对象
├── frontend/              # Vue 3 前端
│   └── src/
│       ├── api/           # 16个API模块
│       ├── components/    # 16个通用组件
│       ├── layouts/       # 主布局 (MainLayout)
│       ├── router/        # 路由配置 (40+路由)
│       ├── stores/        # Pinia 状态管理
│       ├── utils/         # 工具 (request.js, auto-login)
│       └── views/         # 46个页面组件
├── ssb/                   # 省时宝 Flask 服务
│   └── ssb_api.py         # API 网关
├── ssb-repo/              # SSB 完整仓库
└── ac-calc/               # AC 精算 Streamlit 应用
```

### 9.2 关键配置

```
server.servlet.context-path: /api   # 后端 API 前缀
Vite proxy: /api → localhost:8080   # 前端代理
前端 baseURL: /api                   # Axios 基础路径
H2 数据库: jdbc:h2:mem:lawfirm;MODE=MySQL
```

### 9.3 PRD 维护规范

- 每次完成新功能后同步更新本文档
- 新增端点/实体/页面在对应章节追加
- 完成的功能标记状态从 🟡 改为 ✅
- 发现新问题在 「已知问题」 章节追加
