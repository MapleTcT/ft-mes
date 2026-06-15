# 生产迁移就绪账本

## 结论

当前仓库还不能进入生产迁移。测试环境已经能证明部分平台和业务页面运行状态，但生产迁移前置项仍缺少可复验的脚本、演练和签字证据。

机器可读记录见 `metadata/production-migration-readiness.json`，结构校验命令：

```bash
make production-migration-readiness-check
```

该检查只证明生产迁移前置项已经被显式登记和门禁约束，不代表生产迁移已完成。只有所有轨道达到 `READY` 且业务 smoke 已签字时，才能把整体状态改为 `READY_FOR_PRODUCTION_MIGRATION`。

## 当前状态

| 轨道 | 当前状态 | 当前证据 | 缺口 |
| --- | --- | --- | --- |
| PostgreSQL 数据迁移脚本 | BLOCKED | 已有 PostgreSQL init SQL 索引和 Oracle backlog | 缺生产源库抽取、增量同步、校验 SQL、数据量评估和演练报告 |
| 回滚方案 | NOT_STARTED | Docker 测试环境可重建 | 缺生产级备份、恢复、回切、DNS/端口回滚和演练时间窗 |
| license 策略 | PLANNED | 测试环境已走 license bypass 思路 | 缺生产授权策略、过期行为、应急开关和合法性确认 |
| MinIO 文件迁移 | BLOCKED | 历史包线索包含 MinIO `mes` / `transfer` bucket | 缺 bucket inventory、对象校验、迁移脚本、断点续传和回滚策略 |
| Keycloak 生产库策略 | PLANNED | 已有 realm 初始化和 JWT 公钥同步脚本 | 缺生产 realm/export/import、用户凭证策略、client secret 轮换和备份恢复 |
| Nacos / runtime patch 生产化 | PLANNED | 测试环境通过 render Nacos config 和 runtime patch 修复多个页面 | 缺生产配置差异清单、runtime patch 升级策略和回退脚本 |
| 端口 / 域名 / TLS | NOT_STARTED | Docker Compose 暴露测试端口 | 缺生产域名、证书、反向代理、HSTS、端口收敛和证书续期策略 |
| 安全加固 | NOT_STARTED | 默认路径已禁止 Oracle 作为生产默认数据库 | 缺账号密码轮换、最小权限、网络边界、镜像扫描、密钥管理和审计策略 |
| 业务 smoke 签字 | BLOCKED | 平台和 53 个业务页面 smoke 有证据 | WOM 生产动作页仍 React #130，生产写动作未完成落库验收，不能签字 |

## 必须补齐的生产迁移资产

### PostgreSQL 数据迁移脚本

需要新增独立的数据迁移目录或脚本入口，至少包括：

- 源库/目标库连接参数模板，不能提交真实密码。
- 全量表结构核对 SQL。
- 全量数据迁移脚本。
- 增量同步或冻结窗口说明。
- 行数、checksum、关键业务字段校验 SQL。
- 失败重试和幂等策略。
- 演练报告路径。

### 回滚方案

必须能回答“生产切换失败后如何回到切换前状态”：

- PostgreSQL 备份和恢复命令。
- MinIO 对象回滚方式。
- Keycloak realm 和用户数据回滚方式。
- Nacos 配置回滚方式。
- runtime patch 回滚方式。
- 端口、域名、TLS 入口回退方案。
- 回滚演练记录和负责人。

### license 策略

测试环境可以禁用软件狗授权，但生产环境不能只沿用测试 bypass：

- 明确生产是否采购/接入正式 license。
- 明确 license 失效时系统行为。
- 明确紧急 bypass 是否允许、谁审批、如何审计。
- 明确 Redis / Nacos / gateway license 配置的备份和恢复方式。

### MinIO 文件迁移

迁移前必须完成：

- bucket 清单。
- 对象数量和大小统计。
- hash 或 etag 校验策略。
- 迁移脚本和 dry-run 输出。
- 断点续传策略。
- 迁移后抽样下载验证。
- 回滚或双写策略。

### Keycloak 生产库策略

必须明确：

- realm export/import 流程。
- PostgreSQL 生产库连接和备份策略。
- admin 用户、client secret、JWT key 的轮换策略。
- 与 Nacos JWT public key 的同步验证。
- 老系统用户、角色和组织映射关系。

### Nacos / runtime patch 生产化

当前测试环境依赖 Nacos 渲染和 runtime patch。生产迁移前必须把这些差异变成可审计资产：

- Nacos 配置差异清单。
- runtime patch 清单和版本号。
- 每个 patch 的业务影响和回退方法。
- 重启顺序和健康检查。
- 配置漂移检查。

### 端口 / 域名 / TLS

生产入口必须在上线前确定：

- 外部域名和内部服务名。
- 网关、前端、Keycloak、Nacos、MinIO、PostgreSQL 的端口暴露边界。
- TLS 证书来源、部署位置和续期方式。
- 反向代理配置和 header 策略。
- 防火墙或安全组规则。

### 安全加固

至少完成：

- 默认账号禁用或改密。
- 测试密码和测试 token 清理。
- 服务账号最小权限。
- 数据库账号权限拆分。
- 密钥不落库、不提交仓库。
- Docker 镜像和运行用户加固。
- 访问日志、审计日志和异常告警策略。

### 业务 smoke 签字

生产迁移前必须有业务负责人签字的 smoke 记录：

- 平台登录、用户、组织、权限、菜单、待办。
- 基础配置。
- Nacos / Keycloak / PostgreSQL / runtime patch。
- 生产模块完整主流程。
- 每个会写业务数据的动作必须有 PostgreSQL 落库证据。
- 每个未通过项必须有明确风险接受或上线阻断结论。

## 阻断项

| ID | 阻断项 | 当前证据 | 下一步 |
| --- | --- | --- | --- |
| PMR-001 | WOM 生产动作页无法渲染 | `metadata/persistence-acceptance.json` 标记生产状态流转/报工为 `FAIL` | 恢复真实 edit/view component metadata，重新做 marker 落库验收 |
| PMR-002 | 数据迁移脚本未建立 | 仅有 PostgreSQL init SQL 和迁移索引 | 新增独立数据迁移脚本和 rehearsal 报告 |
| PMR-003 | 生产 license 策略未确认 | 测试环境以 bypass 降低验证阻力 | 明确生产授权、应急和审计策略 |
| PMR-004 | MinIO / Keycloak 生产迁移未演练 | 只有测试环境脚本和历史包线索 | 做 bucket/realm inventory、迁移 dry-run 和恢复演练 |

## 更新规则

- 任何轨道改为 `READY` 前，必须在 `metadata/production-migration-readiness.json` 填入可复验 evidence。
- 业务 smoke 未签字前，整体状态不能改为 `READY_FOR_PRODUCTION_MIGRATION`。
- 生产迁移脚本和回滚脚本禁止包含真实密码、token、证书私钥。
- 发现 PostgreSQL 缺表、缺列、类型不兼容或 Oracle 方言残留，必须进入幂等 SQL 或模块 backlog，不允许清库重建掩盖。
