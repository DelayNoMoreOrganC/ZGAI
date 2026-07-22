# ZGAI 至高律所管理系统

ZGAI 是面向律所内部真实使用的案件、客户、审批、案卷、财务与知识管理系统。当前研发原则是先稳定核心业务闭环和数据权限，再接入生产数据库、外部工具与本地 AI。

当前交接基线：2026-07-22。

## 权威文档

仓库只使用以下三份主项目文档：

- [README.md](README.md)：开发环境、启动、配置和验证方式。
- [PRD.md](PRD.md)：产品范围、业务规则、功能状态和验收标准。
- [handoff.md](handoff.md)：每周进度、当前风险、并行 Agent 分工和交接规则。

`ac-calc/` 下的文档属于独立算法子项目；`backend/maven-maven-3.8.6/` 下的文档属于第三方工具，不是 ZGAI 项目状态依据。

## 当前能力

- 律师、行政管理、主任/部门主管、财务四类工作台。
- 按部门隔离案件与客户数据，主任和授权人员按规则查看全所数据。
- 客户档案、案源人、承办人、部门归属和全库利冲检查。
- 承办人发起立案，行政先审、主任终审，支持同意、驳回理由和转审。
- 审批通过后建立一案一档，案件文件存入本机或 NAS，数据库保存元数据和版本。
- 发票申请、申请人修改/删除、财务反馈文件和完成锁定。
- 法规、律所制度、公共模板和经授权参考资料知识库；未配置向量服务时降级为关键词检索。
- 基于当前有权查看案件要素的旧资料检索和受控下载。
- PostgreSQL 初始化脚本、备份接口、存储健康检查和核心审计日志。
- 省时宝独立入口；深度融合暂缓。

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
| AI 接口 | OpenAI 兼容接口、DeepSeek 或 Ollama；未配置时不伪装为 AI 回答 |

## 目录结构

```text
ZGAI/
├── backend/                 Spring Boot API、数据模型与测试
├── frontend/                Vue 3 管理端
├── scripts/                 PostgreSQL、冒烟测试和 RAG 评价脚本
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

### 停止服务

```bash
./stop.sh
```

## 访问地址

| 服务 | 默认地址 |
|---|---|
| ZGAI 前端 | `http://localhost:3017` |
| ZGAI 后端 | `http://localhost:8080/api` |
| 健康检查 | `http://localhost:8080/api/health` |
| Swagger | `http://localhost:8080/api/swagger-ui/index.html` |
| 省时宝独立前端 | `http://localhost:3000`（可选） |

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
| `KNOWLEDGE_LIBRARY_ROOT` | 知识文档原件根目录 |
| `BACKUP_ROOT_PATH` | 数据库备份目录 |
| `LEGACY_CASE_ARCHIVE_ROOT_PATH` | 旧案资料只读根目录 |
| `QDRANT_HOST` / `QDRANT_PORT` | Qdrant 地址 |
| `MINIO_ENABLED` | 是否启用可选 MinIO；启用时同时配置 endpoint/access key/secret key |
| `PG_DUMP_PATH` | PostgreSQL 备份工具路径 |

详细默认值以 `backend/src/main/resources/application.yml`、`application-dev.yml` 和 `application-postgres.yml` 为准。

## 验证命令

### 后端

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@11 /opt/homebrew/opt/maven/bin/mvn test
```

当前基线：101 项测试通过。

### 前端

```bash
cd frontend
npm run build
```

当前构建通过，存在 Dart Sass legacy API 和大分块警告，不影响本阶段运行，后续应专项优化。

### 冒烟测试

```bash
./scripts/smoke-test-core.sh
./scripts/smoke-test-roles.sh
./scripts/evaluate-knowledge-rag.sh
```

角色冒烟测试需要通过环境变量提供普通律师、行政、主任和财务测试账号。不要在脚本或文档中写入密码。

## 数据与安全边界

- 普通账号只能读取其部门范围内案件；客户按案源人或承办人的部门关系过滤。
- 利冲检查覆盖全客户主体库，但命中不代表可以读取无权访问的客户详情。
- 行政可查看审批所需案件整体信息；主任具有全局权限；部门主管仅管理本部门。
- 案件私密文件默认禁止进入共享知识库与 RAG。
- 已废止法规、未确认授权的外部参考资料、非公开文章禁止进入 RAG。
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

当前系统适合局域网开发与受控试用，尚未完成正式生产上线所需的 PostgreSQL 数据迁移演练、HTTPS、灾备恢复演练、安全评估和真实角色全流程验收。不得把“可以启动”解释为“已经达到生产上线标准”。
