# Deploy Reference

这里保存 Linux 迁移时需要参考的配置文本，不保存 Windows 二进制。

## 目录

```text
nacos-config/             # 从 bap-server/config/configgroup 复制并脱敏
docker/                   # Linux Docker Compose 测试部署
database/                 # 数据库迁移和兼容策略说明
nginx/                    # 从 nginx/conf 复制的 Nginx 配置
windows-start-reference/  # Windows 启停脚本，仅用于分析启动顺序和参数
```

## 脱敏策略

复制进本仓库的配置已经把常见敏感字段替换为 `${REPLACE_ME}`，包括：

- password / pwd / passwd
- secret / token
- accessKey / secretKey
- ak / sk

真实生产值应通过 Linux 环境变量、Nacos 密文配置或部署平台密钥管理注入，不要提交到 git。

## Linux 迁移入口

Windows 运行包通过 `nssm` 注册服务；Linux 迁移时建议改为：

- 基础组件：systemd 或 Docker Compose 管理 Nginx、Redis、Zookeeper、Kafka、Nacos、Keycloak、MinIO。
- Java 服务：统一使用 Linux JDK 8/11，按 `backend/services/*/README.md` 的启动类和配置生成 systemd unit。
- 静态前端：Nginx root 指向部署后的静态目录，反代网关 `8008`。

本次测试环境 Docker 化入口见 [docker/README.md](docker/README.md)，默认对外暴露：

- 前端/Nginx：`18080`
- 网关调试端口：`18008`
- Nacos 调试端口：`18848`

数据库迁移策略见 [database/README.md](database/README.md) 和 [Oracle 到 PostgreSQL 替换路线](../docs/oracle-to-postgres-transition.md)。当前默认测试编排是 PostgreSQL-first，Oracle 只作为显式 legacy 配置保留。
