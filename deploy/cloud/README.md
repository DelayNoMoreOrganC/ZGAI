# ZGAI 腾讯云 Linux Demo

本目录用于把稳定 Demo 从开发 Mac 分离。当前容器栈支持腾讯云 Linux；Windows 云工作站不是默认方案。

## 模式

- `DETACHED_DEMO`：只使用云端脱敏数据，不连接律所 NAS 或模型机，适合低成本展示和轻量应用服务器。
- `HYBRID_VPN`：云端运行 ZGAI、PostgreSQL、Qdrant 和 LibreOffice，通过 VPN 私网访问律所 NAS 与模型机；适合 CVM/VPC。

两种模式都只把容器入口绑定到 `127.0.0.1`。宿主机 Nginx 负责域名和 HTTPS，公网不得开放 `1234`、`5432`、`6333`、`6334` 或 `445`。

## 准备

1. 安装 Docker Engine、Compose v2、Nginx 和证书工具。
2. 从 `.env.cloud.example` 创建 `.env.cloud`，生成四组不同的随机密钥，并执行 `chmod 600`。
3. 创建配置中的持久化目录。混合云模式先建立 VPN，再把 NAS 挂载到固定目录，并单独设置 `ZGAI_OFFSITE_BACKUP_ROOT`。
4. 取得域名证书后，运行下方渲染命令，再把生成结果安装为宿主机 Nginx 站点配置。

## 预检与启动

```bash
./deploy/cloud/validate-config.sh ./deploy/cloud/.env.cloud
./deploy/cloud/render-nginx-config.sh ./deploy/cloud/.env.cloud /tmp/zgai.nginx.conf
./deploy/cloud/up.sh ./deploy/cloud/.env.cloud
```

启动后先从服务器本机检查 `http://127.0.0.1:3017`，再检查域名 HTTPS。只有 `443` 对用户开放；证书首次签发需要 `80` 时，应在签发完成后只保留重定向用途。

## 备份与恢复演练

```bash
./deploy/cloud/backup-postgres.sh ./deploy/cloud/.env.cloud
./deploy/cloud/restore-drill.sh ./deploy/cloud/.env.cloud /path/to/lawfirm_backup_YYYYMMDD_HHMMSS.dump
```

应用内不允许在线覆盖 PostgreSQL。备份先在云端本地 SSD 生成、校验并原子落盘，混合云模式再增量复制到 NAS 异地目录；不直接把 `pg_dump` 输出写到跨 VPN 挂载。恢复演练使用随机临时数据库，检查核心表后自动删除；上线前必须保存演练报告和 SHA-256。

## 上线边界

- 轻量服务器只承载脱敏、低依赖 Demo；需要长期访问律所内网时使用 CVM/VPC + VPN。
- PostgreSQL 与 Qdrant 使用云服务器本机 SSD，不能放远程 SMB。
- 混合云中案件/知识原件可位于 VPN 挂载的 NAS；VPN 或 NAS 离线时必须停止文件写入，不得改落不受控目录。
- 生产数据上线前仍须完成 H2→PostgreSQL 迁移、真实角色 E2E、NAS 断线和恢复演练。
