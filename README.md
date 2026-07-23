# ZGAI 至高律所管理系统

ZGAI 是面向律所内部真实使用的案件、客户、审批、案卷、财务与知识管理系统。当前研发原则是先稳定核心业务闭环和数据权限，再接入生产数据库、外部工具与本地 AI。

当前交接基线：2026-07-24。

## 权威文档

仓库只使用以下三份主项目文档：

- [README.md](README.md)：开发环境、启动、配置和验证方式。
- [PRD.md](PRD.md)：产品范围、业务规则、功能状态和验收标准。
- [handoff.md](handoff.md)：每周进度、当前风险、并行 Agent 分工和交接规则。

`ac-calc/` 下的文档属于独立算法子项目；`backend/maven-maven-3.8.6/` 下的文档属于第三方工具，不是 ZGAI 项目状态依据。

## 当前能力

- 律师、行政管理、主任/部门主管、财务四类工作台。
- 员工初始密码与管理员重置密码均触发首次登录强制改密；完成前由后端阻断全部业务接口，`admin/amin` 开发账号不参与存量迁移。
- 按部门隔离案件与客户数据，主任和授权人员按规则查看全所数据。
- 客户档案、案源人、承办人、部门归属、关联主体，以及覆盖客户主体、案件当事人和一层显式关系的全库利冲检查、立案自动关联、行政正式审查、书面豁免原件、审批阻断与终审归档。
- 承办人发起立案，行政先审、主任终审，支持同意、驳回理由和转审。
- 公章用印审批支持律师上传文件、案件文书快速申请、行政待办、附件下载审阅、审批意见/驳回理由和结果留痕。
- 审批通过后建立一案一档，案件文件存入本机或 NAS，数据库保存元数据和版本。
- 顾问案件文件页提供法律意见书快捷上传，继续沿用案件权限、标准目录和版本管理。
- 发票申请、申请人修改/删除、财务反馈文件和完成锁定。
- 法规、律所制度、公共模板和经授权参考资料知识库；法规/制度导入具有独立待审核队列、单次结论锁定、状态回写与审核后索引，受监管来源修改后自动重新送审；未配置向量服务时明确降级为关键词检索。
- 元典法规/案例语义检索、法律引证核验和逐条导入知识库；需要单独配置开放平台 API Key。
- LM Studio 局域网模型接入；可由 Qwen 等 OpenAI 兼容模型完成问答、RAG 回答和文书草稿生成。
- 案件 AI 助手可把律师的明确指令转换为日程、待办和案件进展，支持常用中文相对日期并拒绝写入过去时间；高风险阶段变化必须确认，所有动作留痕且幂等。
- 智能归案支持文字型文档、本地中文 OCR、标准法院案号/期限提取、权限内候选案件匹配和人工确认后写入案件 NAS 目录；案件材料固定使用本地 LM Studio 或本地规则降级，不自动发送云端，识别到的开庭与期限可在律师校正确认后同步为日程或待办。
- 民事智能一键归档支持律师材料/字段核对、补传、行政复核、同源 PDF 预览、本地 OCR/Qwen、固定版式表格、书签、可搜索文本层、页数守恒、来源清单和 NAS 版本锁定；旧直接归档入口已停用。
- 基于当前有权查看案件要素的旧资料检索和受控下载。
- PostgreSQL 初始化脚本、可校验备份、离线恢复演练脚本、存储健康检查和核心审计日志。
- GD、AC、SSB 采用能力内化路线：GD 民事归档首期已进入 ZGAI 原生流程，AC 与 SSB 当前代码仅作为后续规则和算法参考，外部跳转不视为最终融合。

完整状态和待优化项见 [PRD.md](PRD.md)。

## 技术栈

| 层级 | 技术 |
|---|---|
| 前端 | Vue 3、Vite 5、Element Plus、Pinia、Vue Router、Axios |
| 后端 | Java 11、Spring Boot 2.7.18、Spring Security、JPA、MyBatis |
| 开发数据库 | H2 文件库 |
| 多人试用/生产目标 | PostgreSQL 16 |
| 案件与知识文件 | 本机或 SMB/NAS 挂载目录 |
| 向量检索 | Qdrant，可选；未配置时使用关键词检索 |
| AI 接口 | LM Studio 默认负责生成；DeepSeek/GLM/Kimi 仅由用户明确选择；元典负责法律数据检索与引证核验；未配置时不伪装为 AI 回答 |
| 归档引擎 | ZGAI 原生任务流 + 私有 Python Worker（PyMuPDF、PaddleOCR/Tesseract、LibreOffice）；Worker 无独立账号和业务数据库 |

## 目录结构

```text
ZGAI/
├── backend/                 Spring Boot API、数据模型与测试
├── frontend/                Vue 3 管理端
├── archive-worker/          ZGAI 私有归档计算组件，不是独立产品
├── scripts/                 PostgreSQL、冒烟测试和 RAG 评价脚本
├── docs/                    旧案导入模板、NAS/AI 部署与预算资料
├── deploy/cloud/            腾讯云 Linux 脱敏展示与 VPN 混合云部署包
├── deploy/nas/              NAS 容器部署、备份和恢复演练
├── ac-calc/                 AC 精算独立子项目
├── ssb/                     省时宝代理（存在时由启动脚本启动）
├── ssb-repo/                省时宝独立项目（存在时由启动脚本启动）
├── start.sh                 一键启动
├── stop.sh                  停止服务
├── PRD.md                   产品与验收基线
└── handoff.md               并行开发交接基线
```

## 环境要求

- macOS 或兼容的类 Unix 环境。
- JDK 11。
- Maven 3.8+。
- Node.js 18+ 与 npm。
- PostgreSQL 16：仅在 `ZGAI_DB=postgres` 时需要。
- NAS：需要案件文件持久化时挂载；开发测试可使用本地目录。

## 快速启动

### H2 开发模式

```bash
./start.sh
```

首次启动会在已被 Git 忽略的 `backend/data/.dev-secrets` 生成并持久化开发密钥。不要删除该文件后继续读取原 H2 数据，否则既有加密字段将无法解密。

### PostgreSQL 模式

```bash
brew install postgresql@16
./scripts/setup-postgres.sh
ZGAI_DB=postgres ./start.sh
```

PostgreSQL 连接示例见 `.env.postgres.example`。不要把真实密码、AI 密钥或 NAS 凭据提交到 Git。

### H2 迁移到 PostgreSQL

迁移只能读取后端完全停止后生成的 H2 副本，且目标 PostgreSQL 必须是独立空库。先用当前实体结构完成内存兼容演练，再执行 PostgreSQL 计划，最后明确确认目标库名后执行：

```bash
./stop.sh
./scripts/migrate-h2-to-postgres.sh dry-run
./scripts/migrate-h2-to-postgres.sh plan
MIGRATION_CONFIRM_DATABASE=zgai_migration ./scripts/migrate-h2-to-postgres.sh execute
```

脚本不会修改原 H2 文件。每次运行保存源副本、SHA-256 和 JSON 报告，Java 入口还会独立校验绝对路径、只读模式、非符号链接和实际摘要；执行过程按外键顺序迁移、保留主键、校正 PostgreSQL 序列，并逐表验证行数和主键摘要。目标库已有任何业务数据时会拒绝覆盖。迁移后仍须完成浏览器角色验收和 PostgreSQL 逻辑备份/恢复演练，才能切换多人试用。

2026-07-23 已使用当前 H2 停机副本完成兼容演练：66 张逻辑表、11078 行全部通过行数和主键摘要校验。该结果证明当前数据与目标实体结构可迁移，不代表已完成真实 PostgreSQL 16 迁移、恢复或并发验收。

### 停止服务

```bash
./stop.sh
```

停止脚本会先发送 `SIGTERM`，等待 Spring 关闭数据库连接和文件资源；仅在默认 20 秒超时后才强制终止。可按服务单独停止，也可通过环境变量覆盖隔离实例端口：

```bash
./stop.sh backend
./stop.sh frontend
ZGAI_BACKEND_PORT=18080 ZGAI_STOP_TIMEOUT_SECONDS=30 ./stop.sh backend
```

## 访问地址

| 服务 | 默认地址 |
|---|---|
| ZGAI 前端 | `http://localhost:3017` |
| ZGAI 后端 | `http://localhost:8080/api` |
| 健康检查 | `http://localhost:8080/api/health` |
| Swagger | `http://localhost:8080/api/swagger-ui/index.html` |

局域网测试使用运行主机的局域网 IP 替换 `localhost`。

## 关键环境变量

| 变量 | 用途 |
|---|---|
| `ZGAI_DB` | `h2` 或 `postgres` |
| `POSTGRES_URL` | PostgreSQL JDBC 地址 |
| `POSTGRES_USER` | PostgreSQL 用户 |
| `POSTGRES_PASSWORD` | PostgreSQL 密码 |
| `INITIAL_ADMIN_PASSWORD` | 首次初始化开发管理员密码 |
| `CASE_FILE_LIBRARY_ROOT` | 案件文件库根目录 |
| `APPROVAL_FILE_ROOT` | 独立上传的公章用印审批文件持久化目录 |
| `KNOWLEDGE_LIBRARY_ROOT` | 知识文档原件根目录 |
| `BACKUP_ROOT_PATH` | 数据库备份目录 |
| `LEGACY_CASE_ARCHIVE_ROOT_PATH` | 旧案资料只读根目录 |
| `QDRANT_HOST` / `QDRANT_PORT` | Qdrant 地址 |
| `QDRANT_ENABLED` | 是否启用向量库；停用或 Embedding 未配置时不连接 Qdrant |
| `QDRANT_COLLECTION_NAME` / `QDRANT_VECTOR_SIZE` | 知识集合名称与向量维度，维度必须与 Embedding 一致 |
| `AI_EMBEDDING_PROVIDER` | Embedding 提供方；本地默认 `LM_STUDIO`，可设为 `DISABLED` |
| `AI_EMBEDDING_BASE_URL` | 本地 Embedding OpenAI 兼容地址，通常与 LM Studio 同机 |
| `AI_EMBEDDING_MODEL` | 模型机实际加载的 Embedding 模型 ID；留空时不启用向量检索 |
| `AI_EMBEDDING_DIMENSION` | Embedding 输出维度，必须与 Qdrant collection 一致 |
| `AI_EMBEDDING_API_KEY` | Embedding 服务鉴权令牌；仅后端保存，本地服务可按配置留空 |
| `YUANDIAN_API_KEY` | 元典开放平台 API Key；也可由管理员在系统设置中配置 |
| `YUANDIAN_BASE_URL` | 元典官方服务地址，默认 `https://open.chineselaw.com` |
| `LM_STUDIO_BASE_URL` | LM Studio OpenAI 兼容地址，例如 `http://192.168.1.200:1234/v1` |
| `LM_STUDIO_MODEL` | LM Studio 中已加载的生成模型名称 |
| `LM_STUDIO_API_KEY` | LM Studio 鉴权令牌；仅保存在环境变量或本机忽略文件中 |
| `LM_STUDIO_MAX_CONCURRENT` | ZGAI 同时放行到模型机的生成任务数；32GB显存默认`1` |
| `LM_STUDIO_QUEUE_WAIT_SECONDS` | 本地生成任务最长排队时间，默认`600`秒 |
| `LM_STUDIO_RETRY_COUNT` | 遇到502/503/504时的重试次数，默认`1` |
| `LM_STUDIO_CURL_FALLBACK_ENABLED` | Java连接被本机网络组件中断时启用安全兼容通道，默认`true` |
| `AI_DOCUMENT_INTAKE_TEMP_DIR` | 待归案文件临时接收区，确认后删除临时副本 |
| `TESSERACT_COMMAND` / `PDFTOPPM_COMMAND` | 本地图片和扫描 PDF OCR 命令路径 |
| `OCR_LANGUAGE` | Tesseract 语言，默认 `chi_sim+eng` |
| `OCR_MAX_FILE_SIZE` | OCR 单文件上限，默认 `52428800` 字节（50MB） |
| `OCR_MAX_PDF_PAGES` | PDF 文字提取或扫描识别页数上限，默认 `100` 页 |
| `ARCHIVE_WORKER_BASE_URL` | 私有归档 Worker 地址；NAS Compose 使用 `http://archive-worker:8091` |
| `ARCHIVE_WORKER_TOKEN` | 后端与 Worker 共享的内部强令牌，不得提交仓库 |
| `ARCHIVE_WORKER_TIMEOUT_SECONDS` | OCR、分类和合卷最长等待时间，默认 `1800` 秒 |
| `ARCHIVE_ALLOWED_ROOTS` | Worker 只允许读取/写入的案件文件根目录 |
| `ARCHIVE_OCR_CACHE` | 按文件 SHA-256 与 OCR 版本保存的本地缓存目录 |
| `CLIENT_ALL_VIEW_USERS` | 首次绑定 `CLIENT_AUDITOR` 角色的账号，逗号分隔；后续通过角色管理维护 |
| `INVOICE_PROCESSORS` | 首次绑定 `INVOICE_PROCESSOR` 角色的账号，逗号分隔 |
| `CASE_FILING_MANAGERS` | 首次绑定 `CASE_FILING_ADMIN` 角色的账号，逗号分隔 |
| `VITE_ENABLE_EXTERNAL_TOOLS` | 设为 `true` 才显示 SSB、AC 外部工具入口 |
| `MINIO_ENABLED` | 是否启用可选 MinIO；启用时同时配置 endpoint/access key/secret key |
| `PG_DUMP_PATH` | PostgreSQL 备份工具路径 |
| `PG_RESTORE_PATH` | PostgreSQL 备份完整性校验工具路径 |
| `BACKUP_RETENTION_DAYS` | 已校验数据库备份保留天数，默认 `180` |

详细默认值以 `backend/src/main/resources/application.yml`、`application-dev.yml` 和 `application-postgres.yml` 为准。

## 验证命令

### 后端

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@11 /opt/homebrew/opt/maven/bin/mvn test
```

当前基线：304 项测试通过（2026-07-24 全量复验）。必须使用 JDK 11；本机默认较新 JDK 可能在 Lombok 测试编译阶段失败。

完整员工档案接口 `GET /api/users`、`GET /api/users/{id}` 需要 `USER_VIEW`。案件主办、案源人、审批人和日程参与人等业务选择器统一调用 `GET /api/users/options`，只返回在职人员的 ID、姓名、部门和身份类别，不返回账号、电话、邮箱、角色或登录信息。

可执行 JAR 正在运行时不要执行会覆盖同一路径的 `mvn package`。需要重新打包时先执行 `./stop.sh backend`；`start.sh` 已固定为先优雅停止旧服务，再按需构建和启动。

隔离前端测试可以覆盖代理目标，不必连接默认 `8080`：

```bash
VITE_BACKEND_TARGET=http://127.0.0.1:18081 npm run dev -- --port 13117
```

登录后可调用 `GET /api/ocr/health` 检查本地 OCR 能力。返回结果分别标识文字文档提取、图片 OCR 和扫描 PDF OCR 是否可用；缺少 Tesseract、`chi_sim/eng` 语言包或 pdftoppm 时会返回 `DEGRADED`，不会伪报服务正常。`POST /api/ocr/recognize` 只接受 PDF、DOCX、TXT、MD、PNG、JPG/JPEG，并受文件大小和 PDF 页数限制。

具有 `SYSTEM_CONFIG` 权限的管理员也可在“系统设置 → 运行状态”查看 OCR 三项能力；OCR 降级会进入统一系统健康汇总，但不影响文字型 PDF、DOCX 和文本文件的直接提取。

### 前端

```bash
cd frontend
npm run build
```

当前构建通过，存在 Dart Sass legacy API 和大分块警告，不影响本阶段运行，后续应专项优化。

### Playwright 浏览器回归

Playwright 使用本机 Chrome，不在仓库保存密码、登录态、截图或 trace。角色工作台回归需通过环境变量提供四类测试账号：

```bash
cd frontend
ZGAI_E2E_FRONTEND_URL=http://127.0.0.1:3018 \
ZGAI_LAWYER_USERNAME="$LAWYER_USER" ZGAI_LAWYER_PASSWORD="$LAWYER_PASSWORD" \
ZGAI_ADMINISTRATIVE_USERNAME="$ADMIN_USER" ZGAI_ADMINISTRATIVE_PASSWORD="$ADMIN_PASSWORD" \
ZGAI_DIRECTOR_USERNAME="$DIRECTOR_USER" ZGAI_DIRECTOR_PASSWORD="$DIRECTOR_PASSWORD" \
ZGAI_FINANCE_USERNAME="$FINANCE_USER" ZGAI_FINANCE_PASSWORD="$FINANCE_PASSWORD" \
npm run test:e2e:roles
```

客户、立案两级审批、智能归案、案件文件、快速用印和发票反馈锁定闭环会写入数据，只能在隔离库中运行，并必须显式设置 `ZGAI_E2E_CONFIRM=RUN_BROWSER_WRITE_E2E`：

```bash
cd frontend
ZGAI_E2E_CONFIRM=RUN_BROWSER_WRITE_E2E \
ZGAI_E2E_ENVIRONMENT=ISOLATED \
ZGAI_E2E_FRONTEND_URL=http://127.0.0.1:3018 \
ZGAI_LAWYER_USERNAME="$LAWYER_USER" ZGAI_LAWYER_PASSWORD="$LAWYER_PASSWORD" \
ZGAI_ADMINISTRATIVE_USERNAME="$ADMIN_USER" ZGAI_ADMINISTRATIVE_PASSWORD="$ADMIN_PASSWORD" \
ZGAI_DIRECTOR_USERNAME="$DIRECTOR_USER" ZGAI_DIRECTOR_PASSWORD="$DIRECTOR_PASSWORD" \
ZGAI_FINANCE_USERNAME="$FINANCE_USER" ZGAI_FINANCE_PASSWORD="$FINANCE_PASSWORD" \
npm run test:e2e:filing -- --project=desktop-chrome
```

`scripts/setup-e2e-personas.sh` 可在全新隔离库中幂等创建/校准四类虚构账号，要求 `ZGAI_E2E_ENVIRONMENT=ISOLATED` 和显式确认值 `PROVISION_PERSONAS`。当前基线为四角色桌面/手机共 8 项与“客户 → 立案 → 中文图片 OCR → 确认归案 → 快速用印 → 发票闭环 → 结案 → 律师归档核对 → 行政复核 → 电子卷宗下载”写入闭环 1 项通过；同时验证财务无归档审批权限、模型离线本地降级、暂存件所有权、文件 SHA-256、页守恒和成功后案件锁定。安装 Playwright 后 `npm audit` 仍报告 4 个中等和 5 个高危依赖问题，需单独评估升级，不应在业务回归提交中盲目执行破坏性 `npm audit fix --force`。

### 归档 Worker

```bash
PYTHONPATH=archive-worker python3 -m unittest discover -s archive-worker/tests -v
```

当前基线：3 项测试通过，覆盖 PDF 页守恒/来源清单、图片 A4 等比转换和 TXT/MD 直接提取。隔离本地文件根目录的浏览器闭环已通过；正式 NAS 容器、脱敏真实卷宗、500 页和故障注入仍待验收。

### 部署配置

```bash
find deploy scripts -type f -name '*.sh' -exec bash -n {} \;
./deploy/cloud/test-validate-config.sh
./deploy/nas/validate-config.sh /path/to/.env.nas
```

云端配置测试会覆盖公网误绑定、非法域名/端口、配置权限及公网模型地址等拒绝场景。NAS 校验应使用本机真实但不入库的 `.env.nas`；Docker Compose 解析与容器构建仍须在安装 Docker Compose v2 的目标主机执行。

### 冒烟测试

```bash
./scripts/smoke-test-core.sh
./scripts/smoke-test-roles.sh
./scripts/evaluate-knowledge-rag.sh
```

配置本地 Embedding 和 Qdrant 后，可先运行只读自检：

```bash
./scripts/check-rag-stack.sh
```

自检会核验模型 ID、Embedding 实际维度、Qdrant collection 维度及三者一致性，不会输出 API Token，也不会修改知识数据。

NAS 容器部署的 PostgreSQL 备份与离线恢复演练：

```bash
./deploy/nas/backup-postgres.sh ./deploy/nas/.env.nas
./deploy/nas/restore-drill.sh ./deploy/nas/.env.nas /volume1/ZGAI/backups/lawfirm_backup_YYYYMMDD_HHMMSS.dump
```

恢复演练创建临时数据库并检查用户、案件、客户和审批核心表，不覆盖当前业务库。腾讯云快照或 NAS 快照不能替代该逻辑备份与恢复验证。

角色冒烟测试需要通过环境变量提供普通律师、行政、主任和财务测试账号。不要在脚本或文档中写入密码。

用印审批与开票反馈的四角色写流程可通过以下脚本复验。脚本会真实创建并完成测试记录，因此仅可在隔离环境或已明确授权的测试库运行；缺少固定确认值时会拒绝执行。

```bash
ZGAI_BASE_URL=http://127.0.0.1:8080/api \
ZGAI_E2E_CONFIRM=RUN_WRITE_E2E \
ZGAI_E2E_FIXTURE_FILE=/absolute/path/to/test.pdf \
ZGAI_LAWYER_USERNAME="$LAWYER_USER" \
ZGAI_LAWYER_PASSWORD="$LAWYER_PASSWORD" \
ZGAI_ADMINISTRATIVE_USERNAME="$ADMINISTRATIVE_USER" \
ZGAI_ADMINISTRATIVE_PASSWORD="$ADMINISTRATIVE_PASSWORD" \
ZGAI_DIRECTOR_USERNAME="$DIRECTOR_USER" \
ZGAI_DIRECTOR_PASSWORD="$DIRECTOR_PASSWORD" \
ZGAI_FINANCE_USERNAME="$FINANCE_USER" \
ZGAI_FINANCE_PASSWORD="$FINANCE_PASSWORD" \
./scripts/e2e-approval-finance.sh
```

该脚本校验律师发起、本人及无权账号越权拒绝、主任全局只读、行政用印审批、财务反馈与完成锁定，以及申请人下载文件的 SHA-256 一致性。脚本不会输出 Token 或密码，临时响应在退出时自动清理。

2026-07-24 已在独立端口、独立 H2 和临时文件根目录中使用虚构账号完成客户、立案利冲、两级审批、建档、智能归案、文件、快速用印、发票和民事电子卷宗闭环。Playwright 覆盖四角色桌面/手机工作台共 8 项，并以一条新案件完成律师结案与归档核对、财务审批拒绝、行政批准、PDF 下载和 `ARCHIVED` 锁定。生成卷宗为 A4 12 页，5 页源材料无缺页或重复，PDF 与 manifest 版本化写入且无暂存半成品。该结果验证代码基线，但不替代真实员工账号、PostgreSQL、目标 NAS、真实 LM Studio、脱敏卷宗或故障注入验收。

## 数据与安全边界

- 普通账号只能读取其部门范围内案件；客户按案源人或承办人的部门关系过滤。
- 员工首次登录或密码被管理员重置后，只能进入个人中心修改密码；直接调用业务 API 会返回 `428`，不能绕过前端限制。
- 利冲检查覆盖全客户主体库，但命中不代表可以读取无权访问的客户详情。
- 行政可查看审批所需案件整体信息；主任具有全局权限；部门主管仅管理本部门。
- 案件私密文件默认禁止进入共享知识库与 RAG。
- 智能归案文件在律师确认前只保存在 72 小时有效的临时接收区；系统每小时自动清理过期原件和敏感分析内容，只返回当前账号有权案件作为候选。
- AI 不得自动审批、驳回、结案、归档、删除、对外发送或形成正式法律结论。
- 通用 AI 问答和案件问答是只读入口；案件写操作只能通过案件 AI 工作台的幂等指令、动作白名单和确认流程执行。
- 新 AI 日志不保存完整输入输出或异常原文；案件指令只保存纯元数据摘要和 SHA-256。具有 `SYSTEM_CONFIG` 权限的管理员可在“系统设置 → 隐私治理”预览历史记录，并在选择覆盖全部待处理记录的已校验备份、输入固定确认词后执行事务性脱敏；系统不会在启动时静默删除历史数据。
- 模型回答和文书草稿按纯文本渲染，不得把未经净化的 AI 输出作为 HTML 注入页面。
- 已废止法规、未确认授权的外部参考资料、非公开文章禁止进入 RAG。
- 元典法规可由用户逐条导入知识库；元典案例默认作为参考资料，未确认授权前禁止进入共享 RAG。
- LM Studio 调用由 ZGAI 统一排队，并通过原生 Chat API 关闭不必要的内部推理；生产和多人试用不得绕过后端直接调用模型机。
- API 不向浏览器暴露 NAS 绝对路径、备份绝对路径、AI 密钥或内部异常堆栈。
- 高风险写操作必须具备后端权限校验和审计记录，前端隐藏按钮不构成权限控制。

## 并行开发

所有 Agent 开始前先阅读 [handoff.md](handoff.md)，认领单一模块和文件边界。推荐每个 Agent 使用独立 `codex/*` 分支或独立 worktree，禁止多个 Agent 同时修改共享布局、权限矩阵、核心实体或同一数据库脚本。

提交前必须：

1. 与最新基线同步。
2. 运行受影响模块测试。
3. 运行后端全量测试和前端构建。
4. 更新 `handoff.md` 的变更记录与已知风险。
5. 确认没有提交账号、密码、密钥、真实客户隐私或 NAS 凭据。

## 当前部署定位

当前系统适合局域网开发与受控试用，尚未完成正式生产上线所需的真实 PostgreSQL 16 目标库迁移/回滚、HTTPS、灾备恢复演练、安全评估和真实角色全流程验收。不得把“兼容演练通过”或“可以启动”解释为“已经达到生产上线标准”。

Mac 稳定 Demo 的基础设施、预检和停止脚本位于 [deploy/demo](deploy/demo/README.md)。该方案把 PostgreSQL/Qdrant 与开发数据库分开，文件仍落 NAS，AI 仍由 Windows 模型机提供；现有 H2 数据不会被脚本自动迁移。

腾讯云 Linux 部署包位于 [deploy/cloud](deploy/cloud/README.md)，提供 `DETACHED_DEMO` 与 `HYBRID_VPN` 两种互斥模式。前者只允许脱敏云端数据，后者要求 VPN、NAS 挂载和私网模型地址；容器入口只绑定 `127.0.0.1`，部署脚本根据已校验域名与端口渲染宿主机 Nginx HTTPS 配置。

## 已确认生产方向

- 核心系统最终部署在 NAS 容器中，AI 推理由独立模型机提供。
- 当前模型机为 Windows 11、i9-14900KF、用户确认的 32GB 显存 GPU、64GB 内存。推荐在模型机运行生成模型、Embedding 和后续重排服务；PostgreSQL、Qdrant、ZGAI 与原件继续常驻 NAS。
- 若 NAS 的 Qdrant 实测性能不足，可单独把 Qdrant 迁至模型机本地 NVMe，并定时将快照备份回 NAS；不建议把整个 ZGAI 长期部署到模型机。
- 本地 AI 优先；DeepSeek、GLM、Kimi 由用户手动选择，公用 Token 仅在后端安全配置。
- Word/Excel 由 Apache POI 生成，PDF 预览和转换由 LibreOffice headless 容器承担。
- 旧案导入采集模板：[ZGAI旧案资料导入模板_v1.xlsx](docs/ZGAI旧案资料导入模板_v1.xlsx)。
- 部署、设备与预算方案：[NAS部署_AI接入与预算方案_2026-07-23.md](docs/NAS部署_AI接入与预算方案_2026-07-23.md)。
- 若 Mac 需要持续开发或频繁重启，可将稳定 Demo 的 ZGAI、PostgreSQL、Qdrant 和 LibreOffice 转移到腾讯云 Linux。脱敏且不访问律所内网资源的展示可用轻量服务器；需要持续连接 NAS、模型机或其他云内服务时，优先 CVM/VPC + VPN。Linux 为首选，云端 Windows 仅在出现 Windows 专属软件依赖时评估。
