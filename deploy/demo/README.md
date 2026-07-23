# ZGAI Mac Demo 部署

本目录用于将稳定 Demo 与日常开发分离：PostgreSQL 和 Qdrant 运行在 Mac 的 Docker 中，ZGAI 前后端仍由仓库脚本启动，案件与知识文件写入 NAS，生成和 Embedding 由 Windows 模型机提供。

## 边界

- H2 只用于开发和自动测试，不作为多人 Demo 数据库。
- PostgreSQL 与 Qdrant 仅绑定 `127.0.0.1`，不得直接暴露到局域网或公网。
- NAS、模型机、数据库和 JWT 密钥均通过本机 `.env.demo` 或后端密钥文件注入，不提交 Git。
- 第一次切换 PostgreSQL 前，必须先完成 H2 备份、迁移校验和回滚演练；脚本不会自动迁移现有 H2 数据。
- 停止基础设施使用 `stop-infrastructure.sh`，不要执行 `docker compose down -v`。

## 首次准备

1. 安装 Docker Desktop、PostgreSQL 16 客户端和 JDK 11。
2. 从 `.env.demo.example` 复制生成 `.env.demo`，填写强密码和本机路径。
3. 保持 NAS 已挂载，并确认案件、知识、备份目录可读写。
4. 在模型机保持生成模型和 `text-embedding-nomic-embed-text-v1.5` 已加载。

## 启动

```bash
./deploy/demo/validate-config.sh
./deploy/demo/start-infrastructure.sh
./deploy/demo/validate-config.sh --runtime
./deploy/demo/start-application.sh
```

## 停止

```bash
./stop.sh
./deploy/demo/stop-infrastructure.sh
```

## 回滚

在 PostgreSQL 迁移验收前，现有 H2 文件仍保留且不覆盖。需要回退时停止 PostgreSQL 模式应用，确认 H2 文件备份完整，再按默认 `ZGAI_DB=h2` 启动开发环境。不得把迁移后的新业务数据直接丢弃后回切 H2；一旦多人开始写入 PostgreSQL，回滚必须通过已验证的数据库备份恢复完成。

## 腾讯云替代方向

当 Mac 需要频繁重启或持续开发时，可以把 ZGAI、PostgreSQL、Qdrant 和 LibreOffice 转移到腾讯云 Linux。模型机和 NAS 仍留在律所局域网，并通过受控 VPN 与云端连接。该方案必须先完成 VPN、HTTPS、访问白名单、数据库备份和 NAS 断线测试；不得把 SMB、LM Studio、PostgreSQL 或 Qdrant 端口直接开放到公网。
