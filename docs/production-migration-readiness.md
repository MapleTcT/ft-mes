# 生产迁移就绪账本

## 结论

当前仓库还不能进入生产迁移。测试环境已经能证明部分平台和业务页面运行状态，本轮补齐了生产迁移前置项的模板、目标 PostgreSQL 预检脚本和 artifact 门禁，但这些仍不是生产演练完成证据。

机器可读记录见 `metadata/production-migration-readiness.json`，结构和 artifact 校验命令：

```bash
make production-migration-readiness-check
```

生产切换总闸门见 `metadata/production-cutover-gate.json`，校验命令：

```bash
make production-cutover-gate-check
```

生产演练证据清单见 `metadata/production-rehearsal-plan.json` 和
`docs/production-migration/rehearsal-plan.md`，校验命令：

```bash
make production-rehearsal-plan-check
```

该闸门会交叉读取 `metadata/production-migration-readiness.json`、
`metadata/production-module-blockers.json`、
`metadata/production-module-backlog.json`、
`metadata/business-dependency-readiness-smoke.json`、
`metadata/business-dependency-package-scan.json` 和
`metadata/production-export-readiness-smoke.json`。只要迁移 readiness 不是
`READY_FOR_PRODUCTION_MIGRATION`，或生产模块 blocker/backlog 仍大于 0，总闸门必须保持
`NOT_READY_FOR_PRODUCTION_CUTOVER`。当前该报告记录 9 个切换前 gate：
8 个 `PLANNED`、1 个 `BLOCKED`，生产 blocker 为 6 个、生产 backlog 为 9 个；业务依赖和导出 readiness
仍为 BLOCKED 时，也必须作为业务 smoke 签字前置风险保留。

运行态 smoke 报告统一门禁为 `make runtime-smoke-reports-check`，当前覆盖 `metadata/test-environment-smoke.json`、`metadata/postgres-runtime-smoke.json`、`metadata/nacos-config-drift-smoke.json`、`metadata/keycloak-jwt-runtime-smoke.json` 和 `metadata/minio-runtime-smoke.json`。该门禁只做离线报告结构、PASS 状态、当前测试地址 `100.99.133.43`、关键检查项和 secret hygiene 校验，不访问远端，也不替代生产演练或业务签字。

`metadata/production-migration-readiness.json`、`metadata/production-cutover-gate.json` 和 `metadata/production-rehearsal-plan.json` 现在都包含 `sourceEvidence`。该字段会记录 `metadata/test-environment-smoke.json`、`metadata/platform-validation-smoke.json`、PostgreSQL/Nacos/Keycloak/MinIO runtime smoke、生产 blocker/backlog、业务依赖 readiness、业务包扫描和生产导出 readiness 的 `generatedAt`、状态和关键计数；对应校验器会重新读取当前源文件并逐项对账。只要上游证据刷新而迁移账本未同步，`make production-migration-readiness-check`、`make production-cutover-gate-check` 或 `make production-rehearsal-plan-check` 会失败。

`metadata/production-migration-readiness.json` 每条轨道还必须包含
`readyEvidenceCommands`。这些命令不是“已执行证明”，而是从当前
`PLANNED/BLOCKED` 状态推进到 `READY` 时必须补齐的复验入口；校验器会检查
每条轨道至少包含对应的 strict-ready 命令，防止生产迁移账本只有缺口描述、
没有可执行验收路径。

刷新任一上游 smoke 报告后，先运行下面的命令同步生产迁移 readiness、cutover gate 和 rehearsal plan 的 `sourceEvidence`，再跑检查：

```bash
make production-source-evidence-refresh
make production-source-evidence-refresh-check
```

该检查只证明生产迁移前置项已经被显式登记、引用文件真实存在、并做了基础敏感信息扫描；不代表生产迁移已完成。只有所有轨道达到 `READY` 且业务 smoke 已签字时，才能把整体状态改为 `READY_FOR_PRODUCTION_MIGRATION`。轨道或总闸门一旦标记为 `READY`，证据不能再引用 `.example`、`template`、`sample` 这类模板资产，也不能把当前测试地址 `100.99.133.43`、旧测试地址 `10.11.100.17` 或临时公网入口 `222.88.185.146` 冒充生产 rehearsal/cutover evidence；必须换成真实 rehearsal、签字或生产副本验证结果。`make production-evidence-ready-gate-regression-check` 会构造伪 READY 的回滚、license、网络/TLS、安全加固和业务 smoke 签字 evidence，并构造伪 READY 的生产迁移 readiness 总账和 cutover 总闸门，确认这些模板证据和测试环境 evidence 都会被 strict-ready 或总账校验器拒绝。

## 当前状态

| 轨道 | 当前状态 | 当前证据 | 缺口 |
| --- | --- | --- | --- |
| PostgreSQL 数据迁移脚本 | PLANNED | `deploy/database/production-migration/` 预检入口、源库 row-count inventory、目标 PostgreSQL 预检、source/target 行数和 checksum 对比脚本、数据库迁移演练 evidence manifest 和 strict-ready 校验器、PostgreSQL init SQL 索引和 Oracle backlog；`metadata/postgres-runtime-smoke.json` 已在 `100.99.133.43` 测试环境验证 PostgreSQL 15、1474 张 public 表、150 个 view、32 个关键表、15 个兼容列和 8 个兼容索引 8/8 PASS；`make runtime-smoke-reports-check` 会离线校验该报告仍符合当前运行态预期 | 缺增量同步或冻结窗口设计、生产 checksum SQL 实填、表级迁移顺序、数据量评估、strict-ready 数据库迁移 evidence 和演练报告 |
| 回滚方案 | PLANNED | `docs/production-migration/rollback-runbook.md`；`deploy/rollback/production-migration/` 已提供 rollback evidence manifest 和 READY 校验器 | 缺生产级备份、恢复、回切、DNS/端口回滚和演练时间窗的真实证据文件 |
| license 策略 | PLANNED | `docs/production-migration/license-strategy.md`；`deploy/license/production-migration/` 已提供 license decision 模板和 READY 校验器；测试环境已走 license bypass 思路 | 缺生产授权策略、过期行为、应急开关审批、审计规则和合法性确认 |
| MinIO 文件迁移 | PLANNED | `docs/production-migration/minio-migration-runbook.md`；`deploy/minio/production-migration/` 已提供源/目标 bucket inventory、对象清单对账工具、MinIO migration evidence manifest 和 strict-ready 校验器；`metadata/minio-runtime-smoke.json` 已在 `100.99.133.43` 测试环境验证 MinIO 容器、bucket discovery 和 `dtbucket` / `system001bucket` 对象 inventory 8/8 PASS；`make runtime-smoke-reports-check` 会离线校验该报告的 bucket 摘要和 secret hygiene；历史包线索包含 MinIO `mes` / `transfer` bucket | 缺生产 endpoint 的 bucket inventory、对象校验、真实 MinIO migration evidence、迁移 dry-run、断点续传、抽样下载和回滚演练 |
| Keycloak 生产库策略 | PLANNED | `docs/production-migration/keycloak-production-runbook.md`；`deploy/keycloak/production-migration/` 已提供 source/target realm inventory、对账工具、Keycloak migration evidence manifest 和 strict-ready 校验器；已有 realm 初始化、JWT 公钥同步脚本和测试环境 Keycloak/JWT runtime smoke；`metadata/keycloak-jwt-runtime-smoke.json` 显示 19/19 PASS，Keycloak 公钥 hash 与 Nacos JWT hash 一致，网关登录/菜单通过；`make runtime-smoke-reports-check` 会离线校验该报告的 realm/client/mapper/JWT/网关链路预期 | 缺生产 realm export/import、数据库备份恢复、真实 Keycloak migration evidence、用户凭证策略、client secret 轮换和生产 JWT 同步后的登录 smoke 演练 |
| Nacos / runtime patch 生产化 | PLANNED | `docs/production-migration/nacos-runtime-patch-runbook.md`；`metadata/runtime-patch-manifest.json`；`docs/production-migration/runtime-patch-manifest.md`；`deploy/nacos/production-migration/` 已提供 Nacos/runtime patch evidence manifest 和 strict-ready 校验器；已有 render Nacos config、runtime patch 入口和测试环境 Nacos drift smoke，`metadata/nacos-config-drift-smoke.json` 显示 44 个远端 dataId 全部可取、20 个关键检查 PASS、Oracle 残留 0、18/18 个关键服务 healthy；`make runtime-smoke-reports-check` 会离线校验该报告的 group `prod`、服务健康和 secret hygiene | 缺生产配置差异清单、真实 Nacos/runtime patch evidence、签名 patch 包、升级策略和回退演练 |
| 端口 / 域名 / TLS | PLANNED | `docs/production-migration/network-tls-checklist.md`；`deploy/network/production-migration/` 已提供 network/TLS evidence 模板和 READY 校验器；`metadata/test-environment-smoke.json` 已验证当前 `100.99.133.43` 测试入口 HTTP/SSH/Docker 9/9 PASS；`make runtime-smoke-reports-check` 会离线校验该测试入口报告 | 缺真实生产域名、证书、反向代理、HSTS、端口收敛、防火墙/安全组和证书续期策略证据 |
| 安全加固 | PLANNED | `docs/production-migration/security-hardening-checklist.md`；`deploy/security/production-migration/` 已提供 security hardening evidence 模板和 READY 校验器 | 缺真实账号密码轮换、测试账号清理、最小权限、网络边界、镜像扫描、密钥管理、审计日志和告警策略执行证据 |
| 业务 smoke 签字 | BLOCKED | 平台和 53 个业务页面 smoke 有证据；生产模块测试矩阵当前 43 条，`37 PASS / 6 BLOCKED / 0 NOT_RUN`，其中 41 条 route smoke PASS；WOM 状态流转、无明细 stop、产出/投入明细报工、提前放料、下推备料需求、工序开始结束、活动开始结束、简易活动报工、生产请检、备料退料，QCS 报告生成/编辑/结果保存/合格/不合格处置/降级放行/退回/检验放行，WTS 正常关闭/停工，WAPS 工作计划创建/调整/取消/审批，RM 导入/批量同步/删除，craftGraph 创建/更新/删除已形成真实前端/API 和 PostgreSQL 证据；`metadata/business-dependency-readiness-smoke.json` 证明 material-service 与 process-analysis 仍为外部业务包/服务 blocker；`metadata/production-module-blockers.json` 记录 6 个生产 blocker；`metadata/production-module-backlog.json` 记录 9 个生产 backlog，包括 public `produceTaskCreated` 已显式禁用/待产品确认、WOM 手工创建/导入范围确认和 WOM `makeTaskList` 二维码运行包接口 404；`deploy/business-smoke/production-migration/` 已提供 business smoke signoff evidence 模板和 READY 校验器 | 剩余 6 条生产用例、WOM 手工创建/导入范围确认、public `produceTaskCreated` 已显式禁用/待产品确认和 WOM 二维码运行包接口缺失仍未解决或签署风险接受，不能签字 |

## 轨道 READY 复验命令

这些命令同步写入 `metadata/production-migration-readiness.json` 的
`readyEvidenceCommands`，并由 `make production-migration-readiness-check`
强制校验。

| 轨道 | READY 前必须补齐的命令入口 |
| --- | --- |
| PostgreSQL 数据迁移脚本 | `ADP_PROD_MIGRATION_ENV=/secure/path/adp-prod-migration.env make production-source-inventory`<br>`ADP_PROD_MIGRATION_ENV=/secure/path/adp-prod-migration.env make production-target-preflight`<br>`make production-rowcount-compare PROD_MIGRATION_REPORT_DIR=/secure/path/adp-production-migration-preflight`<br>`make production-checksum-compare PROD_MIGRATION_REPORT_DIR=/secure/path/adp-production-migration-preflight`<br>`make production-db-migration-ready-check DB_MIGRATION_EVIDENCE=/secure/path/adp-db-migration-evidence.json` |
| 回滚方案 | `make production-rollback-ready-check ROLLBACK_EVIDENCE=/secure/path/adp-rollback-evidence.json` |
| license 策略 | `make production-license-ready-check LICENSE_DECISION=/secure/path/adp-license-decision.json` |
| MinIO 文件迁移 | `ADP_MINIO_MIGRATION_ENV=/secure/path/adp-minio-migration.env make production-minio-source-inventory`<br>`ADP_MINIO_MIGRATION_ENV=/secure/path/adp-minio-migration.env make production-minio-target-inventory`<br>`make production-minio-compare MINIO_MIGRATION_REPORT_DIR=/secure/path/adp-minio-migration-preflight`<br>`make production-minio-migration-ready-check MINIO_MIGRATION_EVIDENCE=/secure/path/adp-minio-migration-evidence.json` |
| Keycloak 生产库策略 | `ADP_KEYCLOAK_MIGRATION_ENV=/secure/path/adp-keycloak-migration.env make production-keycloak-source-export`<br>`ADP_KEYCLOAK_MIGRATION_ENV=/secure/path/adp-keycloak-migration.env make production-keycloak-target-export`<br>`make production-keycloak-compare KEYCLOAK_MIGRATION_REPORT_DIR=/secure/path/adp-keycloak-migration-preflight`<br>`make production-keycloak-migration-ready-check KEYCLOAK_MIGRATION_EVIDENCE=/secure/path/adp-keycloak-migration-evidence.json` |
| Nacos / runtime patch 生产化 | `make runtime-patch-manifest-check`<br>`make smoke-nacos-config ADP_SSH_HOST=/production-or-rehearsal-host`<br>`make production-nacos-runtime-patch-ready-check NACOS_RUNTIME_PATCH_EVIDENCE=/secure/path/adp-nacos-runtime-patch-evidence.json` |
| 端口 / 域名 / TLS | `make production-network-tls-ready-check NETWORK_TLS_PLAN=/secure/path/adp-network-tls-plan.json` |
| 安全加固 | `make production-security-hardening-ready-check SECURITY_HARDENING_PLAN=/secure/path/adp-security-hardening-plan.json` |
| 业务 smoke 签字 | `make business-package-scan`<br>`make smoke-business-dependencies ADP_BASE_URL=/production-or-rehearsal-url`<br>`make smoke-production-export-readiness ADP_BASE_URL=/production-or-rehearsal-url`<br>`make production-business-smoke-signoff-ready-check BUSINESS_SMOKE_SIGNOFF=/secure/path/adp-business-smoke-signoff.json` |

## 已建立的生产迁移资产

| 资产 | 用途 | 当前状态 |
| --- | --- | --- |
| `deploy/database/production-migration/README.md` | 数据迁移预检入口说明 | 模板已建立 |
| `deploy/database/production-migration/.env.example` | 源库/目标库连接参数占位模板 | 模板已建立，不含真实密码 |
| `deploy/database/production-migration/scripts/run-target-preflight.sh` | 目标 PostgreSQL schema inventory 和 row count 预检 | 已纳入 `make runtime-script-check` 语法检查 |
| `deploy/database/production-migration/scripts/run-source-inventory.sh` | 生产源库 row count inventory；默认 PostgreSQL，`oracle-legacy` 只允许作为迁移源快照 | 已纳入 `make runtime-script-check` 语法检查 |
| `deploy/database/production-migration/scripts/compare-row-counts.py` | 对比 `source-row-counts.tsv` 和 `target-row-counts.tsv`，输出 TSV/JSON 对账报告 | 已纳入 `make runtime-script-check` 语法检查 |
| `deploy/database/production-migration/scripts/compare-checksums.py` | 对比 `source-checksums.tsv` 和 `target-checksums.tsv`，输出 TSV/JSON checksum 对账报告 | 已纳入 `make runtime-script-check` 编译检查 |
| `deploy/database/production-migration/migration-evidence.example.json` | 数据库迁移演练 evidence manifest 模板；串联 source inventory、target preflight、row-count/checksum 对账、target PostgreSQL runtime smoke 和 rollback linkage | 模板已建立；只能作为 `PLANNED` 结构样例 |
| `deploy/database/production-migration/scripts/validate-migration-evidence.py` | 数据库迁移演练 evidence 结构、READY 规则和敏感信息校验器；`--strict-ready` 会拒绝模板/example/sample 证据 | 已纳入 `make runtime-script-check` 编译检查和 `make production-db-migration-evidence-check` |
| `deploy/database/production-migration/sql/target-schema-inventory.sql` | 目标 PostgreSQL 表、列、索引清单 SQL | 模板已建立 |
| `deploy/database/production-migration/sql/target-row-counts-template.sql` | 单表行数模板 | 模板已建立 |
| `deploy/database/production-migration/sql/target-checksum-template.sql` | 关键表 checksum 模板 | 模板已建立，需要按业务表实填 |
| `deploy/database/production-migration/table-list.example.txt` | 迁移预检表清单样例 | 模板已建立 |
| `deploy/database/production-migration/source-checksums.example.tsv` | source checksum 对账输入样例 | 模板已建立，不含真实数据 |
| `deploy/database/production-migration/target-checksums.example.tsv` | target checksum 对账输入样例 | 模板已建立，不含真实数据 |
| `deploy/docker/scripts/adp-postgres-runtime-smoke.js` | 通过 SSH 进入当前测试 PostgreSQL 容器，只读验证版本、schema 规模、关键平台/业务表、兼容列和兼容索引 | 已纳入 `make runtime-script-check` 语法检查，可通过 `make smoke-postgres-runtime` 复验 |
| `metadata/postgres-runtime-smoke.json` | `100.99.133.43` 测试环境 PostgreSQL runtime smoke 结果 | 8/8 PASS；PostgreSQL 15.18；1474 张 public 表；32/32 关键表、15/15 兼容列、8/8 兼容索引存在 |
| `docs/production-migration/rollback-runbook.md` | 生产回滚 runbook | 模板已建立 |
| `deploy/rollback/production-migration/README.md` | 回滚证据 manifest 和 READY 校验入口说明 | 模板已建立 |
| `deploy/rollback/production-migration/rollback-evidence.example.json` | PostgreSQL / MinIO / Keycloak / Nacos / runtime patch / 域名 TLS 回滚证据模板 | 模板已建立，不含真实密钥 |
| `deploy/rollback/production-migration/scripts/validate-rollback-evidence.py` | 回滚证据结构、组件覆盖、READY 规则和敏感信息校验器 | 已纳入 `make runtime-script-check` 编译检查 |
| `docs/production-migration/license-strategy.md` | 生产 license 决策模板 | 模板已建立 |
| `deploy/license/production-migration/README.md` | 生产 license 决策 evidence 和 READY 校验入口说明 | 模板已建立 |
| `deploy/license/production-migration/license-decision.example.json` | 正式授权、过期行为、应急 bypass、配置备份恢复和审计监控决策模板 | 模板已建立，不含真实授权材料 |
| `deploy/license/production-migration/scripts/validate-license-decision.py` | license 决策覆盖、READY 规则和敏感信息校验器 | 已纳入 `make runtime-script-check` 编译检查 |
| `deploy/network/production-migration/README.md` | 生产域名、TLS、反向代理、防火墙和服务暴露边界 evidence 入口 | 模板已建立 |
| `deploy/network/production-migration/network-tls-plan.example.json` | 生产域名、证书、代理、服务暴露、防火墙和验证动作 evidence 模板 | 模板已建立，不含真实密钥 |
| `deploy/network/production-migration/scripts/validate-network-tls-plan.py` | network/TLS 证据覆盖、READY 规则和敏感信息校验器 | 已纳入 `make runtime-script-check` 编译检查 |
| `deploy/docker/scripts/adp-test-environment-smoke.js` | 当前测试环境 HTTP/SSH/Docker 可达性 smoke，覆盖 `18080`、`18070` 和核心容器 | 已纳入 `make runtime-script-check` 语法检查，可通过 `make smoke-test-environment` 复验 |
| `metadata/test-environment-smoke.json` | `100.99.133.43` 当前测试环境入口 smoke 结果 | 9/9 PASS；HTTP 18080/18070、SSH、nginx/gateway/PostgreSQL/Nacos/Keycloak/MinIO 均通过 |
| `deploy/security/production-migration/README.md` | 生产安全加固 control evidence 和 READY 校验入口说明 | 模板已建立 |
| `deploy/security/production-migration/security-hardening-plan.example.json` | 默认账号、测试账号、secret、数据库权限、镜像、容器、审计和告警 evidence 模板 | 模板已建立，不含真实密钥 |
| `deploy/security/production-migration/scripts/validate-security-hardening-plan.py` | security hardening 证据覆盖、READY 规则和敏感信息校验器 | 已纳入 `make runtime-script-check` 编译检查 |
| `docs/production-migration/minio-migration-runbook.md` | MinIO bucket/object 迁移 runbook | 模板已建立 |
| `deploy/minio/production-migration/README.md` | MinIO 文件迁移 inventory/compare 入口说明 | 模板已建立 |
| `deploy/minio/production-migration/.env.example` | MinIO 源/目标 endpoint 和 alias 占位模板 | 模板已建立，不含真实密钥 |
| `deploy/minio/production-migration/bucket-list.example.txt` | MinIO bucket 清单样例 | 模板已建立 |
| `deploy/minio/production-migration/scripts/run-bucket-inventory.sh` | MinIO 源/目标对象清单采集脚本 | 已纳入 `make runtime-script-check` 语法检查 |
| `deploy/minio/production-migration/scripts/compare-bucket-inventory.py` | MinIO 源/目标对象清单对账脚本 | 已纳入 `make runtime-script-check` 语法检查 |
| `deploy/minio/production-migration/minio-migration-evidence.example.json` | source/target bucket inventory、object comparison、dry-run、sample download、runtime smoke 和 rollback linkage evidence 模板 | 模板已建立；只能作为 `PLANNED` 结构样例 |
| `deploy/minio/production-migration/scripts/validate-minio-migration-evidence.py` | MinIO migration evidence 结构、READY 规则和敏感信息校验器；`--strict-ready` 会拒绝模板/example/sample 证据 | 已纳入 `make runtime-script-check` 编译检查和 `make production-minio-migration-evidence-check` |
| `deploy/docker/scripts/adp-minio-runtime-smoke.js` | 通过 SSH 验证远端测试环境 MinIO 容器、bucket discovery 和对象 inventory | 已纳入 `make runtime-script-check` 语法检查，可通过 `make smoke-minio-runtime` 复验测试环境 |
| `metadata/minio-runtime-smoke.json` | `100.99.133.43` 测试环境 MinIO runtime smoke 结果 | 8/8 PASS；2 个 bucket；31 个对象；104285 bytes；报告不包含 MinIO 密钥或 raw object key |
| `docs/production-migration/keycloak-production-runbook.md` | Keycloak realm/user/client 迁移 runbook | 模板已建立 |
| `deploy/keycloak/production-migration/README.md` | Keycloak source/target realm inventory 和 compare 入口说明 | 模板已建立 |
| `deploy/keycloak/production-migration/.env.example` | Keycloak source/target Admin API 占位模板 | 模板已建立，不含真实密码 |
| `deploy/keycloak/production-migration/scripts/export-realm-inventory.sh` | Keycloak source/target realm 清单导出脚本 | 已纳入 `make runtime-script-check` 语法检查 |
| `deploy/keycloak/production-migration/scripts/compare-realm-inventory.py` | Keycloak source/target realm 清单对账脚本 | 已纳入 `make runtime-script-check` 语法检查 |
| `deploy/keycloak/production-migration/keycloak-migration-evidence.example.json` | source/target realm inventory、realm comparison、database backup/restore、secret rotation、JWT/Nacos sync、auth smoke 和 rollback linkage evidence 模板 | 模板已建立；只能作为 `PLANNED` 结构样例 |
| `deploy/keycloak/production-migration/scripts/validate-keycloak-migration-evidence.py` | Keycloak migration evidence 结构、READY 规则和敏感信息校验器；`--strict-ready` 会拒绝模板/example/sample 证据 | 已纳入 `make runtime-script-check` 编译检查和 `make production-keycloak-migration-evidence-check` |
| `deploy/docker/scripts/adp-keycloak-jwt-runtime-smoke.js` | 通过 SSH 验证远端 Keycloak realm、client、scope mapper、Nacos JWT 公钥同步、Nacos keycloak 服务注册和网关登录/菜单链路 | 已纳入 `make runtime-script-check` 语法检查，可通过 `make smoke-keycloak-jwt` 复验测试环境 |
| `metadata/keycloak-jwt-runtime-smoke.json` | `100.99.133.43` 测试环境 Keycloak/JWT runtime smoke 结果 | 19/19 PASS；Keycloak 公钥 hash 与 Nacos JWT hash 一致；网关登录和菜单加载通过 |
| `docs/production-migration/nacos-runtime-patch-runbook.md` | Nacos/runtime patch 生产化 runbook | 模板已建立 |
| `deploy/nacos/production-migration/README.md` | Nacos/runtime patch 生产化 evidence manifest 和 READY 校验入口说明 | 模板已建立 |
| `deploy/nacos/production-migration/nacos-runtime-patch-evidence.example.json` | rendered config baseline、target Nacos export、diff review、signed patch package、publish window、post-publish smoke 和 rollback linkage evidence 模板 | 模板已建立；只能作为 `PLANNED` 结构样例 |
| `deploy/nacos/production-migration/scripts/validate-nacos-runtime-patch-evidence.py` | Nacos/runtime patch evidence 结构、READY 规则和敏感信息校验器；`--strict-ready` 会拒绝模板/example/sample 证据 | 已纳入 `make runtime-script-check` 编译检查和 `make production-nacos-runtime-patch-check` |
| `deploy/docker/scripts/adp-nacos-config-drift-smoke.js` | 通过 SSH 从远端 Nacos 容器拉取运行态配置和 service registry，输出 hash drift、关键 PostgreSQL/JWT/Nacos 检查、Oracle 残留摘要和关键服务健康实例数 | 已纳入 `make runtime-script-check` 语法检查，可通过 `make smoke-nacos-config` 复验测试环境 |
| `metadata/nacos-config-drift-smoke.json` | `100.99.133.43` 测试环境 Nacos 配置漂移和服务注册 smoke 结果 | 44 个 dataId 全部可取，关键检查 20/20 PASS，Oracle 残留 0，Nacos group `prod` 注册服务 91 个，18/18 个关键服务 healthy，仍有 27 个 hash drift 待生产发布前审阅 |
| `scripts/verify-runtime-smoke-reports.py` | 离线校验已提交的测试环境入口、PostgreSQL、Nacos、Keycloak/JWT 和 MinIO runtime smoke 报告 | 已纳入 `make ci`，可通过 `make runtime-smoke-reports-check` 复验报告结构、PASS 状态、当前 host、预期检查项和 secret hygiene |
| `metadata/runtime-patch-manifest.json` | runtime patch、Nacos 模板和 PostgreSQL init SQL checksum 清单 | 已生成，并由 `make runtime-patch-manifest-check` 校验 |
| `docs/production-migration/runtime-patch-manifest.md` | runtime patch checksum 清单的人读版 | 已生成，并由 `make runtime-patch-manifest-check` 校验 |
| `docs/production-migration/network-tls-checklist.md` | 端口、域名、TLS checklist | 模板已建立 |
| `docs/production-migration/security-hardening-checklist.md` | 安全加固 checklist | 模板已建立 |
| `docs/production-migration/business-smoke-signoff-template.md` | 业务 smoke 签字模板 | 模板已建立但当前 BLOCKED |
| `deploy/business-smoke/production-migration/README.md` | 业务 smoke、落库范围和负责人签字 evidence 入口说明 | 模板已建立 |
| `deploy/business-smoke/production-migration/business-smoke-signoff.example.json` | 平台、基础配置、生产主流程、QCS/LIMS、PostgreSQL 缺口和 owner signoff evidence 模板 | 模板已建立，不含真实账号、密码或个人隐私 |
| `deploy/business-smoke/production-migration/scripts/validate-business-smoke-signoff.py` | business smoke signoff 证据覆盖、READY 规则和敏感信息校验器 | 已纳入 `make runtime-script-check` 编译检查 |
| `scripts/verify-production-evidence-ready-gates.py` | 对 rollback/license/network/security/business smoke 的 strict-ready 规则，以及生产迁移 readiness 总账和 cutover 总闸门的 READY 规则做负向回归，确保模板 evidence 和测试环境地址不能冒充 READY | 已纳入 `make ci`，可通过 `make production-evidence-ready-gate-regression-check` 复验 |
| `deploy/docker/scripts/adp-business-dependency-readiness-smoke.js` | 复验 material 仓储/库存服务和 ProcessAnalysis 追溯服务是否具备进入业务 marker 验收的条件 | 可通过 `make smoke-business-dependencies` 运行；当前结果仍为 BLOCKED |
| `metadata/business-dependency-readiness-smoke.json` | `100.99.133.43` 当前业务外部依赖 readiness 结果 | 2/2 依赖 BLOCKED；material alias 和 ProcessAnalysis alias 均无健康 Nacos 实例，相关认证端点仍返回 503 |
| `docs/production-module-backlog.md` | 生产模块 backlog 账本说明 | 当前 9 条未闭合 backlog；覆盖生产 blocker、动作级假成功/范围确认和 WOM 二维码运行包接口缺失 |
| `metadata/production-module-backlog.json` | 生产模块 backlog 机器账本 | 当前 9 条未闭合 item；`make production-module-backlog-check` 会校验全部落库 `FAIL/BLOCKED`、生产 `BLOCKED` 和无生产用例 ID 的 WOM 工具栏 `BLOCKED` 动作均被覆盖 |
| `metadata/production-rehearsal-plan.json` | 生产演练证据计划机器账本 | 当前 `REHEARSAL_BLOCKED`；9 条轨道，8 条 `PLANNED`、1 条 `BLOCKED`，由 `make production-rehearsal-plan-check` 校验与 readiness、cutover、runtime smoke 和生产 blocker/backlog 账本一致 |
| `docs/production-migration/rehearsal-plan.md` | 生产演练证据计划人读版 | 列出每条轨道的 readiness/cutover 状态、需要执行的 evidence command 和当前 blocker；不代表生产可以切换 |
| `scripts/refresh-production-source-evidence.py` | 上游 smoke 报告刷新后，同步生产迁移 readiness、cutover gate 和 rehearsal plan 的 `sourceEvidence` | 已接入 `make production-source-evidence-refresh` 和 `make production-source-evidence-refresh-check` |

## 必须补齐的生产迁移资产

### PostgreSQL 数据迁移脚本

已有 `deploy/database/production-migration/` 作为预检和校验入口，并已经覆盖源库行数 inventory、目标 PostgreSQL 预检、source/target 行数对比和 checksum 对比：

```bash
ADP_PROD_MIGRATION_ENV=/secure/path/adp-prod-migration.env make production-source-inventory
ADP_PROD_MIGRATION_ENV=/secure/path/adp-prod-migration.env make production-target-preflight
make production-rowcount-compare PROD_MIGRATION_REPORT_DIR=/tmp/adp-production-migration-preflight
make production-checksum-compare PROD_MIGRATION_REPORT_DIR=/tmp/adp-production-migration-preflight
make production-db-migration-evidence-check
make production-db-migration-ready-check DB_MIGRATION_EVIDENCE=/secure/path/adp-db-migration-evidence.json
```

当前测试 PostgreSQL 运行态可通过下面命令复验：

```bash
make smoke-postgres-runtime ADP_SSH_HOST=100.99.133.43
```

本次结果见 `metadata/postgres-runtime-smoke.json`：远端
`adp-mes-newbase-postgres-1` 容器内数据库为 PostgreSQL 15.18，public schema
1474 张表、150 个 view、约 590MB；平台、组织、权限、待办、低代码元数据、
WOM、RM、QCS、WTS、WAPS 和批次相关 32 个关键表均可读，`auth_user.error_count`、
`wfm_task_pending.pending_source`、`rbac_roleuser.valid`、`wom_wait_put_records`
和 `rm_process_actives` 等已知 PostgreSQL 兼容列/索引均存在。该结果只证明测试
PostgreSQL 运行态，不替代生产源库 inventory、全量/增量迁移、checksum 或
rehearsal 报告。

仍然还需要：

- 全量数据迁移脚本。
- 增量同步或冻结窗口说明。
- 行数、checksum、关键业务字段校验 SQL 的实填版本。
- 通过 `production-db-migration-ready-check` 的数据库迁移演练 evidence manifest。
- 失败重试和幂等策略。
- 至少一次 rehearsal 报告。

### 回滚方案

必须能回答“生产切换失败后如何回到切换前状态”：

已有 `deploy/rollback/production-migration/` 作为回滚证据 manifest 和 READY 校验入口：

```bash
make production-rollback-evidence-check
make production-rollback-evidence-check ROLLBACK_EVIDENCE=/secure/path/adp-rollback-evidence.json
make production-rollback-ready-check ROLLBACK_EVIDENCE=/secure/path/adp-rollback-evidence.json
```

- PostgreSQL 备份和恢复命令的演练输出。
- MinIO 对象回滚方式。
- Keycloak realm 和用户数据回滚方式。
- Nacos 配置回滚方式。
- runtime patch 回滚方式。
- 端口、域名、TLS 入口回退方案。
- 回滚演练记录和负责人。

### license 策略

测试环境可以禁用软件狗授权，但生产环境不能只沿用测试 bypass：

已有 `deploy/license/production-migration/` 作为生产 license 决策 evidence 和 READY 校验入口：

```bash
make production-license-strategy-check
make production-license-strategy-check LICENSE_DECISION=/secure/path/adp-license-decision.json
make production-license-ready-check LICENSE_DECISION=/secure/path/adp-license-decision.json
```

- 明确生产是否采购/接入正式 license。
- 明确 license 失效时系统行为。
- 明确紧急 bypass 是否允许、谁审批、如何审计。
- 明确 Redis / Nacos / gateway license 配置的备份和恢复方式。

### MinIO 文件迁移

已有 `deploy/minio/production-migration/` 作为源/目标 bucket inventory 和对象对账入口：

```bash
ADP_MINIO_MIGRATION_ENV=/secure/path/adp-minio-migration.env make production-minio-source-inventory
ADP_MINIO_MIGRATION_ENV=/secure/path/adp-minio-migration.env make production-minio-target-inventory
make production-minio-compare MINIO_MIGRATION_REPORT_DIR=/tmp/adp-minio-migration-preflight
make production-minio-migration-evidence-check
make production-minio-migration-ready-check MINIO_MIGRATION_EVIDENCE=/secure/path/adp-minio-migration-evidence.json
```

测试环境 runtime 链路可通过下面命令复验：

```bash
make smoke-minio-runtime ADP_SSH_HOST=100.99.133.43
```

本次结果见 `metadata/minio-runtime-smoke.json`：远端 `adp-mes-newbase-minio-1`
容器内 MinIO API `http://127.0.0.1:30200` 可访问，`dtbucket` 和
`system001bucket` 均完成只读 inventory，8/8 检查 PASS，合计 31 个对象、
104285 bytes。该报告不会提交 MinIO access key、secret key、raw object key 或
对象内容，只保留 bucket 汇总和对象 key hash 前缀样本。

迁移前仍必须完成：

- bucket 清单。
- 对象数量和大小统计。
- hash 或 etag 校验策略。
- 迁移脚本和 dry-run 输出。
- 断点续传策略。
- 通过 `production-minio-migration-ready-check` 的 MinIO migration evidence manifest。
- 迁移后抽样下载验证。
- 回滚或双写策略。

### Keycloak 生产库策略

已有 `deploy/keycloak/production-migration/` 作为 source/target realm inventory 和对账入口：

```bash
ADP_KEYCLOAK_MIGRATION_ENV=/secure/path/adp-keycloak-migration.env make production-keycloak-source-export
ADP_KEYCLOAK_MIGRATION_ENV=/secure/path/adp-keycloak-migration.env make production-keycloak-target-export
make production-keycloak-compare KEYCLOAK_MIGRATION_REPORT_DIR=/tmp/adp-keycloak-migration-preflight
make production-keycloak-migration-evidence-check
make production-keycloak-migration-ready-check KEYCLOAK_MIGRATION_EVIDENCE=/secure/path/adp-keycloak-migration-evidence.json
```

测试环境 runtime 链路可通过下面命令复验：

```bash
make smoke-keycloak-jwt ADP_SSH_HOST=100.99.133.43
```

本次结果见 `metadata/keycloak-jwt-runtime-smoke.json`：realm `dt`、`pc_dt` / `mobile_dt`、`supos` client scope mapper、Nacos `prod@@keycloak` healthy 实例、Nacos JWT 公钥同步、网关登录和菜单加载均通过。该结果只证明测试环境运行链路，不替代生产 realm export/import、数据库备份恢复和 secret 轮换演练。

必须明确：

- realm export/import 流程。
- PostgreSQL 生产库连接和备份策略。
- admin 用户、client secret、JWT key 的轮换策略。
- 与 Nacos JWT public key 的同步验证。
- 通过 `production-keycloak-migration-ready-check` 的 Keycloak migration evidence manifest。
- 老系统用户、角色和组织映射关系。

### Nacos / runtime patch 生产化

当前测试环境依赖 Nacos 渲染和 runtime patch。生产迁移前必须把这些差异变成可审计资产：

- 测试环境可用 `make smoke-nacos-config ADP_SSH_HOST=100.99.133.43` 复验远端 Nacos group `prod`。本次 smoke 结果见 `metadata/nacos-config-drift-smoke.json`：44 个 dataId 全部可取，20 个关键检查 PASS，非注释 Oracle 残留为 0；Nacos group `prod` 注册服务 91 个，18/18 个关键服务存在 healthy 实例；27 个 hash drift 需要生产发布前逐项审阅。
- Nacos 配置差异清单。
- 当前仓库 runtime patch 清单、版本号和 checksum 已由 `metadata/runtime-patch-manifest.json` / `docs/production-migration/runtime-patch-manifest.md` 建立，生产迁移前必须再与生产签名 patch 包和 Nacos export diff 做比对。
- 已有 `deploy/nacos/production-migration/nacos-runtime-patch-evidence.example.json` 作为 evidence manifest 模板；真实生产或 rehearsal 证据必须通过 `make production-nacos-runtime-patch-ready-check NACOS_RUNTIME_PATCH_EVIDENCE=/secure/path/adp-nacos-runtime-patch-evidence.json`。
- 每个 patch 的业务影响和回退方法。
- 重启顺序和健康检查。
- 配置漂移检查。

### 端口 / 域名 / TLS

已有 `deploy/network/production-migration/` 作为生产网络/TLS evidence 和 READY 校验入口：

```bash
make production-network-tls-check
make production-network-tls-check NETWORK_TLS_PLAN=/secure/path/adp-network-tls-plan.json
make production-network-tls-ready-check NETWORK_TLS_PLAN=/secure/path/adp-network-tls-plan.json
```

当前测试环境入口可以用下面命令复验：

```bash
make smoke-test-environment ADP_SSH_HOST=100.99.133.43 ADP_BASE_URL=http://100.99.133.43:18080
```

本次结果见 `metadata/test-environment-smoke.json`：`100.99.133.43` 的
`18080` 和 `18070` HTTP 入口均用 `HTTP HEAD` 探测返回 200，SSH 登录到 `v6-2288H-V6` 成功，
nginx、gateway、PostgreSQL、Nacos、Keycloak 和 MinIO 核心容器均为 Up。
该结果只证明测试环境入口可达，不替代生产 DNS、HTTPS 证书、续期、防火墙、
HSTS 或反向代理 header 证据。

生产入口必须在上线前确定：

- 外部域名和内部服务名。
- 网关、前端、Keycloak、Nacos、MinIO、PostgreSQL 的端口暴露边界。
- TLS 证书来源、部署位置和续期方式。
- 反向代理配置和 header 策略。
- 防火墙或安全组规则。
- 外部 HTTPS、证书续期、HSTS、端口边界和登录 smoke 的真实 evidence。

### 安全加固

已有 `deploy/security/production-migration/` 作为生产安全加固 evidence 和 READY 校验入口：

```bash
make production-security-hardening-check
make production-security-hardening-check SECURITY_HARDENING_PLAN=/secure/path/adp-security-hardening-plan.json
make production-security-hardening-ready-check SECURITY_HARDENING_PLAN=/secure/path/adp-security-hardening-plan.json
```

至少完成：

- 默认账号禁用或改密。
- 测试密码和测试 token 清理。
- 测试 license seed、模拟登录和 debug bypass 清理或正式风险接受。
- 服务账号最小权限。
- 数据库账号权限拆分。
- 密钥不落库、不提交仓库。
- Docker 镜像和运行用户加固。
- 访问日志、审计日志和异常告警策略。

### 业务 smoke 签字

已有 `deploy/business-smoke/production-migration/` 作为业务 smoke signoff evidence 和 READY 校验入口：

```bash
make production-business-smoke-signoff-check
make production-business-smoke-signoff-check BUSINESS_SMOKE_SIGNOFF=/secure/path/adp-business-smoke-signoff.json
make production-business-smoke-signoff-ready-check BUSINESS_SMOKE_SIGNOFF=/secure/path/adp-business-smoke-signoff.json
```

生产迁移前必须有业务负责人签字的 smoke 记录：

- 平台登录、用户、组织、权限、菜单、待办。
- 基础配置。
- Nacos / Keycloak / PostgreSQL / runtime patch。
- 生产模块完整主流程。
- 每个会写业务数据的动作必须有 PostgreSQL 落库证据。
- 每个未通过项必须有明确风险接受或上线阻断结论。
- `metadata/production-module-blockers.json` 中的每个 `PROD-*` 阻断 case、`metadata/production-module-backlog.json` 中的每个未闭合 item，以及 `metadata/production-export-gap-breakdown.json` 中的每个未解决导出 target 必须被 business smoke signoff 覆盖；若仍未解决，READY 前必须逐项签署结构化风险接受，`riskAcceptance` 必须包含 `decision=ACCEPTED`、`acceptedBy`、`signedAt` 和真实 `evidence`，普通字符串不能作为生产签署证据。
- Business、technical、database 和 release owner 必须在 signoff evidence 中明确 APPROVED；否则不能 READY。
- `PASS` smoke、`PASS` 落库范围和 `APPROVED` owner signoff 的证据不能引用 `.example`、`template`、`sample` 等模板资产，必须换成真实生产或 rehearsal 证据。

## 阻断项

| ID | 阻断项 | 当前证据 | 下一步 |
| --- | --- | --- | --- |
| PMR-001 | 剩余生产主流程还不能业务签字 | 生产模块测试矩阵当前 43 条，`37 PASS / 6 BLOCKED / 0 NOT_RUN`；已通过动作覆盖 WOM 状态流转、报工、备料、请检、QCS 报告/判定、WTS、WAPS、RM 导入同步和 craftGraph CRUD；但 RM 批量配方显式编辑入口、物料服务/库存入库、ProcessAnalysis 追溯、完整报工中的独立不良数/库存边界、生产相关导出入口和可见手工创建入口仍缺 before/after 数据库或业务包证据 | 补齐外部 Batch 客户端、物料/库存、ProcessAnalysis 等业务包和入口后，从真实前端执行剩余 6 条 BLOCKED 用例并查 PostgreSQL；全部 PASS 后再填 business smoke signoff |
| PMR-002 | 数据迁移和回滚未演练 | 已有目标 PostgreSQL 预检、源库 inventory、行数对账和回滚 evidence manifest；但没有生产副本 rehearsal 输出 | 补生产源库 inventory、全量/增量迁移脚本、rollback evidence 文件和 rehearsal 报告 |
| PMR-003 | 生产 license 策略未确认 | 已有 license decision 模板和 READY 校验器；测试环境以 bypass 降低验证阻力 | 明确生产授权、失效行为、应急 bypass 审批、审计和合法性策略 |
| PMR-004 | MinIO / Keycloak 生产迁移未演练 | MinIO 已有 bucket inventory、对象对账脚本和 strict READY evidence 校验器；Keycloak 已有 source/target realm inventory、对账脚本和 strict READY evidence 校验器；仍缺生产或生产副本的实际输出 | 做生产 bucket/realm inventory、MinIO 迁移 dry-run、Keycloak export/import、JWT 同步、抽样下载和恢复演练 |
| PMR-005 | 生产网络/TLS evidence 未 READY | 已有 network/TLS plan 模板和 strict READY 校验器；测试环境 `100.99.133.43:18080` 和 `metadata/test-environment-smoke.json` 只能证明运行 smoke，不代表生产域名/TLS | 补真实生产域名、证书、反向代理、防火墙/安全组、HSTS、证书续期和外部 HTTPS smoke evidence |
| PMR-006 | 生产安全加固 evidence 未 READY | 已有 security hardening plan 模板和 strict READY 校验器；测试账号、测试密码、runtime bypass 和本地 Compose 默认值不能作为生产安全证明 | 补真实账号轮换、测试账号清理、secret manager、数据库授权、镜像扫描、容器运行用户、审计日志和告警 evidence |
| PMR-007 | 业务 smoke signoff evidence 未 READY | 已有 business smoke signoff 模板和 strict READY 校验器；QCS 报告保存、合格/不合格生效回写已有当前地址 marker 证据，但剩余 WOM/material/export/外部客户端阻断、生产或 rehearsal 复跑和 owner 签字仍未 READY | 补真实前端 smoke、所有写动作落库、PostgreSQL 缺口闭环、风险接受和 owner APPROVED 记录 |

## 更新规则

- 任何轨道改为 `READY` 前，必须在 `metadata/production-migration-readiness.json` 填入可复验证据。
- `READY` 证据不能使用 `.example`、`template`、`sample` 等模板文件；生产切换总闸门还会校验每个 gate 的状态必须与 readiness 账本同名轨道一致。
- `make production-evidence-ready-gate-regression-check` 必须持续覆盖 readiness 总账和 cutover 总闸门，防止 PostgreSQL 数据迁移、MinIO、Keycloak、Nacos/runtime patch 等轨道仅凭模板 evidence 被误标为 `READY`。
- 业务 smoke 未签字前，整体状态不能改为 `READY_FOR_PRODUCTION_MIGRATION`。
- 生产迁移脚本和回滚脚本禁止包含真实密码、token、证书私钥。
- `make production-migration-readiness-check` 会校验 artifact 文件存在并做基础敏感信息扫描。
- `make production-network-tls-check` 只校验 network/TLS 模板结构；真实生产证据必须通过 `make production-network-tls-ready-check NETWORK_TLS_PLAN=/secure/path/adp-network-tls-plan.json`。
- `make production-security-hardening-check` 只校验 security hardening 模板结构；真实生产证据必须通过 `make production-security-hardening-ready-check SECURITY_HARDENING_PLAN=/secure/path/adp-security-hardening-plan.json`。
- `make production-business-smoke-signoff-check` 会校验 business smoke signoff 模板结构、生产阻断清单、生产 backlog、导出 target 覆盖和敏感信息；真实生产签字必须通过 `make production-business-smoke-signoff-ready-check BUSINESS_SMOKE_SIGNOFF=/secure/path/adp-business-smoke-signoff.json`。
- `make production-nacos-runtime-patch-check` 只校验 Nacos/runtime patch evidence 模板结构；真实生产证据必须通过 `make production-nacos-runtime-patch-ready-check NACOS_RUNTIME_PATCH_EVIDENCE=/secure/path/adp-nacos-runtime-patch-evidence.json`。
- `make production-minio-migration-evidence-check` 只校验 MinIO migration evidence 模板结构；真实生产证据必须通过 `make production-minio-migration-ready-check MINIO_MIGRATION_EVIDENCE=/secure/path/adp-minio-migration-evidence.json`。
- `make production-keycloak-migration-evidence-check` 只校验 Keycloak migration evidence 模板结构；真实生产证据必须通过 `make production-keycloak-migration-ready-check KEYCLOAK_MIGRATION_EVIDENCE=/secure/path/adp-keycloak-migration-evidence.json`。
- 发现 PostgreSQL 缺表、缺列、类型不兼容或 Oracle 方言残留，必须进入幂等 SQL 或模块 backlog，不允许清库重建掩盖。
