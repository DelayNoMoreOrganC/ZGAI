# 律所智能案件管理系统 v2.0.0

> **智能案件全流程管理平台** - 面向60人律所的生产级系统

## 🎉 2.0版本发布 - 案件管理完整可用

**发布日期**: 2026-04-19

**核心里程碑**: 案件管理全链路打通，系统具备生产环境部署能力

---

## ⚡ 快速开始

### 环境要求

- **Java**: JDK 11+
- **Node.js**: v16+
- **数据库**: H2 (开发) / MySQL 8.0 (生产)
- **缓存**: Redis (可选)

### 启动步骤

#### 1. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端服务启动在: `http://localhost:8080`

#### 2. 启动前端

```bash
cd frontend
npm install  # 首次启动需要安装依赖
npm run dev
```

前端服务启动在: `http://localhost:3017`

#### 3. 登录系统

- **访问地址**: http://localhost:3017
- **默认账号**: `admin`
- **默认密码**: `admin123`

---

## ✨ 核心功能

### 📊 工作台
- 统计卡片（本月案件/进行中/待办数/收费）
- 日历视图（开庭/审限/立案可视化）
- 待办事项（按优先级排序）
- 快捷入口

### ⚖️ 案件管理
- **案件列表** - 多维度筛选、搜索、分页
- **案件详情** - 5个Tab完整呈现
  - 基本案情（案件信息/当事人/代理费）
  - 办案记录（工时记录/文档附件）
  - 受理单位（保全/执行/庭审/承办人）
  - 案件文档（树形目录/AI识别）
  - 案件动态（操作日志/评论）
- **归档库** - 已归档案件独立管理
- **回收站** - 软删除机制，支持恢复/永久删除
- **生命周期流转** - 正向/回退流转，自动创建待办

### 🤖 AI智能辅助
- **AI OCR** - 法院文书智能识别
- **AI文书生成** - 起诉状/答辩状/代理词
- **AI问答** - RAG知识库检索
- **使用日志** - Token计费/使用统计

### 📅 日程管理
- 日程安排（开庭/审限/提醒）
- 待办管理（优先级/截止时间）
- 逾期预警（3天红/7天橙）

### 👥 客户管理
- 客户档案（基本信息/关联案件）
- 沟通记录（跟进历史）
- 利益冲突检索

### 💰 财务管理
- 费用记录（诉讼费/保全费/差旅费）
- 律师费管理（已收/待收）
- 收款记录/开票管理

### ✅ 审批管理
- 6种预置模板（用印/报销/开票/请假/采购/证照）
- 审批流转（同意/驳回/转审/撤回）

### 📚 知识库
- RAG知识库（3篇文档）
- 文章管理（分类/标签/搜索）

---

## 🛠️ 技术栈

### 后端
- **框架**: Spring Boot 2.7.18
- **语言**: Java 11
- **数据库**: H2 (开发) / MySQL 8.0 (生产)
- **ORM**: Spring Data JPA
- **认证**: JWT (JJWT 0.11.5)
- **文档**: MinIO 8.5.7
- **缓存**: Redis (可选)

### 前端
- **框架**: Vue 3
- **UI**: Element Plus
- **构建**: Vite
- **状态**: Pinia
- **路由**: Vue Router
- **HTTP**: Axios
- **图表**: ECharts

---

## 📁 项目结构

```
ZGAI/
├── backend/                    # 后端项目
│   ├── src/main/java/
│   │   └── com/lawfirm/
│   │       ├── controller/     # 控制器层
│   │       ├── service/        # 服务层
│   │       ├── repository/     # 数据访问层
│   │       ├── entity/         # 实体类
│   │       ├── dto/            # 数据传输对象
│   │       └── config/         # 配置类
│   ├── src/main/resources/
│   │   ├── application.yml     # 应用配置
│   │   └── schema.sql          # 数据库初始化
│   └── pom.xml                 # Maven配置
│
├── frontend/                   # 前端项目
│   ├── src/
│   │   ├── api/                # API接口
│   │   ├── components/         # 组件
│   │   ├── layouts/            # 布局
│   │   ├── views/              # 页面
│   │   ├── stores/             # 状态管理
│   │   ├── router/             # 路由
│   │   └── utils/              # 工具函数
│   ├── package.json            # NPM配置
│   └── vite.config.js          # Vite配置
│
├── PRD.md                      # 产品需求文档
├── PRD功能清单.md              # 功能实现清单
├── CHANGELOG.md                # 版本历史
└── README.md                   # 本文件
```

---

## 🔧 配置说明

### 后端配置

**文件**: `backend/src/main/resources/application.yml`

```yaml
spring:
  datasource:
    # 开发环境使用H2
    url: jdbc:h2:mem:lawfirm
    # 生产环境切换为MySQL
    # url: jdbc:mysql://localhost:3306/lawfirm?useUnicode=true&characterEncoding=utf8

server:
  port: 8080

# AI配置（可选）
ai:
  deepseek:
    api-key: your-api-key
    base-url: https://api.deepseek.com
```

### 前端配置

**文件**: `frontend/vite.config.js`

```javascript
export default {
  server: {
    port: 3017,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
}
```

---

## 📊 系统数据状态

| 实体 | 数量 | 说明 |
|------|------|------|
| 案件 | 6个 | 包括2个归档、1个回收站 |
| 日程 | 2条 | 开庭日期提醒 |
| 知识库 | 3篇 | RAG检索文档 |
| 工作汇报 | 3条 | 周报/月报 |
| 审批 | 1条 | 用印申请 |
| 卷宗 | 1个 | 案件文档 |

---

## ⚠️ 重要提示

### AI功能配置

AI文书生成功能需要配置API Key:

1. 获取DeepSeek API Key
2. 修改`application.yml`中的`ai.deepseek.api-key`
3. 或在系统设置中在线配置

### 生产环境部署

1. **切换数据库** - H2 → MySQL 8.0
2. **配置MinIO** - 对象存储服务
3. **配置Redis** - 缓存和会话管理
4. **HTTPS配置** - 生产环境必须启用
5. **备份策略** - 每日自动备份，保留180天

---

## 🐛 Bug反馈

- **问题反馈**: 请在GitHub Issues提交
- **功能建议**: 欢迎提交Feature Request
- **安全漏洞**: 请私密提交至security@

---

## 📄 开源协议

本项目采用私有协议，未经授权不得用于商业用途。

---

## 👥 开发团队

- **产品经理**: [Your Name]
- **技术负责人**: [Your Name]
- **开发团队**: [Your Team]

---

**律所智能案件管理系统 v2.0.0** | 让法律工作更智能

*最后更新: 2026-04-19*
