# ZGAI NAS 容器部署

本目录用于 NAS Docker Compose 试用部署。正式配置从 `.env.nas.example` 复制到未纳入 Git 的 `.env.nas`，真实密码和模型 Token 不得写入仓库。

Compose 同时部署 ZGAI 后端、前端、PostgreSQL、Qdrant 和内部 `archive-worker`。归档 Worker 只挂载案件文件目录与 OCR 缓存，不开放公网端口，不维护独立账号或案件数据库；它通过内部强令牌接受 ZGAI 的 OCR、分类、固定版式表格和 PDF 合卷任务。

## 启动

```bash
./deploy/nas/validate-config.sh ./deploy/nas/.env.nas
./deploy/nas/up.sh ./deploy/nas/.env.nas
```

## PostgreSQL 备份

```bash
./deploy/nas/backup-postgres.sh ./deploy/nas/.env.nas
```

脚本使用 PostgreSQL 16 容器内的 `pg_dump` 生成 custom-format dump，先写入 `.part`，经 `pg_restore --list` 校验后原子改名，并生成 SHA-256 文件。只清理超过 `BACKUP_RETENTION_DAYS` 的 `lawfirm_backup_*.dump` 及其校验文件。

## 恢复演练

```bash
./deploy/nas/restore-drill.sh ./deploy/nas/.env.nas /volume1/ZGAI/backups/lawfirm_backup_YYYYMMDD_HHMMSS.dump
```

演练只允许读取配置备份目录中的正式 dump，创建随机临时数据库，恢复后检查用户、案件、客户和审批核心表，随后删除临时数据库并在备份目录写入演练报告。脚本不会覆盖当前 `POSTGRES_DB`。

生产上线前至少完成一次真实恢复演练，并把演练报告与对应 dump 的 SHA-256 一并留档。快照不能替代 PostgreSQL dump 和恢复验证。
