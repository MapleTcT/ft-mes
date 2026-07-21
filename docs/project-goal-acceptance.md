# 项目总目标验收总账

## 结论

当前总目标仍是 `IN_PROGRESS_NOT_COMPLETE`。

本总账用于把“可持续开发仓库、Oracle 替换、平台功能验证、生产模块完整验证、PostgreSQL 缺口治理、生产迁移前置项”放在同一张可复验账本里。它不替代真实前端测试、后端落库验收或生产迁移演练；它负责防止局部 smoke 通过后误判为整体完成。

机器可读记录见 `metadata/project-goal-acceptance.json`，校验命令：

```bash
make project-goal-acceptance-check
```

运行态 smoke 报告统一门禁为 `make runtime-smoke-reports-check`，它会校验已提交的测试环境入口、PostgreSQL、Nacos、Keycloak/JWT 和 MinIO 报告仍为 PASS、指向当前测试地址 `100.99.133.43`、包含预期检查项且不包含明文密钥。该门禁只证明报告资产可复验，不替代真实生产演练。

## 跨账本一致性

`make project-goal-acceptance-check` 不只是检查本文件和
`metadata/project-goal-acceptance.json` 的结构。它还会交叉读取平台 smoke、
生产模块测试矩阵、生产模块 blocker、生产模块 backlog、落库验收、生产迁移 readiness 和 cutover
gate，防止把局部通过项手工改成整体 READY：

- 平台登录、权限、菜单和待办标记 READY 时，必须有 `metadata/platform-validation-smoke.json`
  中对应 section 的 PASS 证据。
- Nacos、Keycloak/JWT、PostgreSQL 运行态和生产迁移前置项引用 runtime smoke 时，必须有
  `metadata/nacos-config-drift-smoke.json`、`metadata/keycloak-jwt-runtime-smoke.json`、
  `metadata/postgres-runtime-smoke.json`、`metadata/test-environment-smoke.json` 和
  `metadata/minio-runtime-smoke.json` 中对应 PASS 证据；runtime patch 必须有
  `metadata/runtime-patch-manifest.json` checksum 清单。
- 生产矩阵仍有 `FAIL` 时，`G-013` 必须是 `FAIL`；仍有 `BLOCKED` 或 `NOT_RUN`
  时，`G-013` 必须保持 `BLOCKED`。
- 生产模块测试用例、生产 backlog 或落库验收仍有未闭合项时，`G-018` 不能标记为 `READY`。
- 生产迁移 readiness、cutover gate、生产 blocker 或生产 backlog 仍未闭合时，`G-020` 不能标记为 `READY`。
- `G-020` 必须引用数据库迁移、回滚、license、MinIO、Keycloak、Nacos/runtime、network/TLS、安全加固和业务 smoke 签字 9 条生产迁移轨道的脚本、模板或校验器；缺任一轨道 artifact 时校验失败。
- BPI 的真实 PostgreSQL/Kafka/Flink/浏览器/目标环境/影子运行证据未闭合时，`G-021` 必须保持 `PARTIAL` 或 `BLOCKED`，设计文档、模拟器或本地单测不能单独支持 `READY`。
- 基础配置必须引用 `metadata/basic-config-coverage.json`、`docs/basic-config-coverage.md`、
  `metadata/basic-config-action-matrix.json`、`docs/basic-config-action-matrix.md`、
  `scripts/verify-basic-config-coverage.py` 和 `scripts/verify-basic-config-action-matrix.py`；覆盖账本或动作矩阵仍有 `PARTIAL`、`BLOCKED`
  或 `NOT_RUN` 区块时，`G-012` 不能标记为 `READY`。
- CI/治理依赖资产库存仍有缺失引用或未跟踪必需文件时，`G-001` 不能标记为 `READY`；提交/交接前必须通过 `make ci-required-file-strict-check`。

## 状态口径

| 状态 | 含义 |
| --- | --- |
| `READY` | 当前证据足以证明该目标项已完成。 |
| `PARTIAL` | 有可复验证据，但覆盖范围不足或仍有明确缺口。 |
| `BLOCKED` | 有上游阻断，无法完成验收闭环。 |
| `FAIL` | 已执行验证但结果与预期不符。 |
| `NOT_STARTED` | 尚未形成可复验证据。 |

## 当前总览

| 指标 | 数量 |
| --- | ---: |
| 目标项 | 21 |
| READY | 9 |
| PARTIAL | 10 |
| BLOCKED | 2 |
| FAIL | 0 |
| NOT_STARTED | 0 |

## 目标项明细

| ID | 目标项 | 状态 | 当前证据 | 缺口 |
| --- | --- | --- | --- | --- |
| G-001 | 可持续开发仓库基础 | READY | 父 POM、Makefile、`.gitignore`、CI、库存和门禁已建立；`metadata/ci-required-file-inventory.json` 当前记录 1591 个 CI/治理/runtime 依赖资产，其中 1591 tracked、0 untracked、0 missing references。本轮纳入 PostgreSQL QCS 显示兼容迁移 194-196、配置字段同步迁移 197、默认关闭的 Java 8 QCS quality-gate sidecar、精确 BPI batch resolver、BPI Flyway V24、query-first WMS adapter、目标 QCS/Kafka/material-wms/PostgreSQL/browser 历史验收、QCS 列表/编辑/只读页和双 LIMS provider 冷启动证据、WMS 原命令核对、material-wms 故障恢复、受控强制结束、真实目标 QCS 页面到 Kafka/BPI 的首发和幂等重放，以及实体模型字段生命周期、23 类 PostgreSQL 标量类型矩阵和独立 `OBJECT` 关联验收资产。受控 QCS 激活已验收并恢复关闭；连续生产启用和外部 ERP/WMS 补偿仍未完成。 | 继续随模块提升补测试；新增或移动治理资产后继续运行 `make ci` |
| G-002 | 当前内容迁移 | PARTIAL | `docs/current-content-inventory.md` 和源码恢复目录 | 业务模块动作级源码/表关系仍需继续排查 |
| G-003 | Oracle 替换为 PostgreSQL 默认路径 | PARTIAL | Docker/POM 默认 PostgreSQL；`metadata/oracle-replacement-status.json` 当前汇总 `blockingIssueCount=0`、`runtimeConfigActiveOracleLineCount=0`、`directOracleDependencyCount=2`、`oracleBacklogReferenceCount=1608`、`postgresMigrationHighRiskCount=0`、`postgresMapperAuditErrorCount=0`，PostgreSQL 迁移数为 `197`；Oracle 只允许 legacy/profile/backlog；`metadata/oracle-migration-audit.json` 当前记录 1608 条已分类引用、0 条未分类引用，新增项只是 PostgreSQL 迁移 195 中的供应商来源说明并归类为 `postgres-compat-reference`；`ShadowRunPostgresRepository`、`FeatureFlagPostgresRepository`、来源序列、点位校准及 `BatchReleasePostgresRepository` 的 Spring JDBC `rowNum` 回调参数均归类为 `java-row-index-noise`；PATROL 运行元数据引用归类为 `postgres-compat-reference`，JavaScript `rowNum` / `Number(...)` 均按非 SQL 噪声分类，真实 SQL 方言仍被阻断；模块准入门禁继续阻断不可检查包和 Oracle 默认路径 | Oracle backlog 仍有引用，需要逐模块迁移 |
| G-004 | 项目目标与交接说明 | READY | `docs/project-objectives.md`、`docs/sustainable-development.md` | 随真实验收继续更新 |
| G-005 | 后端落表排查交接入口 | READY | `docs/backend-table-audit-handoff.md`、`docs/backend-table-audit/00-index.md`、`docs/backend-table-audit/persistence-acceptance.md` 和 `make backend-table-audit-handoff-check` 已建立；门禁会校验交接文档、索引报告表、已开始/已完成专项报告文件、落库验收汇总、PostgreSQL 默认、当前测试环境和不允许清库绕过规则 | 表级业务含义仍需专门线程执行 |
| G-006 | 平台登录/认证 | READY | 2026-06-21 当前地址平台综合 smoke 复跑 `6/6` PASS：API base 和 browser base 均为 `http://100.99.133.43:18080`；platform API `16/16`、首页待办、组织部门点击、RBAC authority `9/9`、菜单抽样 `40/40`；证据 `metadata/platform-validation-smoke.json` 和 `/tmp/adp-platform-validation-1009913343-20260621052958/platform-validation-summary.json` | 保持回归 smoke |
| G-007 | 用户/人员/岗位/公司/组管理 | READY | 组织部门/组/岗位/公司/人员 CRUD 已落库；人员勾选创建账号已落库；独立用户管理账号新增/编辑/锁定/解锁/删除已落库；角色新增/编辑/删除、角色绑定/解绑用户已落库；2026-06-20 当前地址用公网浏览器入口和 `100.99.133.43` PostgreSQL 重新复验部门、组、岗位、公司、人员、人员创建账号和独立用户管理账号 PASS；用户管理 marker `ADP_E2E_20260620071641_AUSR`，证据 `/tmp/adp-auth-user-persistence-1009913343-current.json` | 保持回归 smoke |
| G-008 | 组织管理 | READY | 部门、组、岗位、公司、人员新增/编辑/删除已用 marker 查 PostgreSQL；人员 `createUser=true` 已证明 `org_person` 和 `auth_user` 绑定、同步和软删除；2026-06-20 当前地址复验 marker 覆盖 `ADP_E2E_20260620061140_ORGDEP`、`ADP_E2E_20260620063543_GRP/POS/COM`、`ADP_E2E_20260620063832_PER/PUSR`；岗位-角色关联 marker `ADP_E2E_20260620123813_ORGPOSROLE` 已证明 `POST/GET/DELETE /inter-api/organization/v1/position/role` 写入 `org_position_role`、兼容视图 `base_roleposition.valid=1`，解绑后两者清空；证据 `/tmp/adp-organization-position-role-persistence-1009913343-current.json` | 保持组织管理 CRUD、岗位角色关联和 RBAC 权限关系回归 smoke |
| G-009 | 权限/RBAC | READY | 2026-06-20 `100.99.133.43:18080` RBAC authority smoke `9/9` PASS；RBAC 落库复验 marker `ADP_E2E_20260620070327_RBAC` 已通过，API base `http://100.99.133.43:18080`、真实浏览器 base `http://222.88.185.146:18080`；角色新增/编辑/删除、角色用户绑定/解绑、`personmanage/addPerson` 角色/用户菜单操作权限新增/删除、角色/用户数据资源权限保存和关闭受控均返回 `200`，PostgreSQL 验证 `rbac_role`、`rbac_roleuser`、`rbac_rolepermission`、`rbac_userpermission`、`rbac_role_data_permission`、`rbac_role_data_permission_ctrl`、`rbac_user_data_permission`、`rbac_user_data_permission_ctrl` 均按预期写入、失效或删除；证据 `/tmp/adp-rbac-permission-persistence-1009913343-current.json` | 保持回归 smoke |
| G-010 | 菜单导航 | READY | 2026-06-21 当前地址真实浏览器菜单抽样复跑 `40/40` PASS：API base 和 browser base 均为 `http://100.99.133.43:18080`；无页面级阻断错误 | 继续扩大菜单覆盖 |
| G-011 | 待办 | READY | 2026-06-21 当前地址首页待办 smoke PASS：`http://100.99.133.43:18080` 导航 `200`，无 visible/network/console/page error | 继续保留回归 |
| G-012 | 基础配置 | PARTIAL | 系统编码与普通 app 系统配置 CRUD 已完成 marker 落库；内置目录 7/7 只读 smoke 通过，QCS/RM/BaseSet 各有受控单项配置保存、回读、回滚和代表性业务回归；customProperty 模型映射已完成启用/编辑/恢复/禁用落库；`ADP_E2E_20260721_1240_ENTITY_MODEL_PHYSICAL_TABLE` 已完成实体/模型、物理表和 12 列幂等验收；`ADP_E2E_20260721071510_PG_FIELD` 以 `33/33 PASS` 完成字段约束、索引和危险变更回滚；`ADP_E2E_20260721075749_PG_TYPE_MATRIX` 以 `36/36 PASS` 覆盖 23 种标量 `DbColumnType`；`ADP_E2E_20260721084718_PG_OBJECT_ASSOC` 又以 `10/10 PASS` 覆盖一对一/多对一 `OBJECT`、LONG/TEXT/BAPCODE 目标键、两种参数格式、四次逻辑 join、幂等重放、非法目标事务回滚和零残留；证据 `metadata/entity-model-field-persistence-acceptance.json`、`metadata/entity-model-field-type-matrix-acceptance.json` 和 `metadata/entity-model-object-association-acceptance.json`，`OBJECT` 实现提交 `3650f817`；动作矩阵为 `8 PASS / 4 READ_ONLY_GUARDED / 3 CONTROLLED_MARKER_REQUIRED / 2 PLANNED`，覆盖账本为 `5 PASS / 2 PARTIAL` | 身份、授权、密钥、密码类配置仍只读；其他 BaseSet/RM/QCS 配置项和自动删列仍需专用 marker；Nacos/Keycloak 生产 export/diff、realm 迁移、secret 轮换、登录 smoke 和回退演练未完成 |
| G-013 | 生产模块完整功能 | BLOCKED | 生产模块测试矩阵当前 `44 PASS / 0 BLOCKED / 0 NOT_RUN`、44 条 route smoke 全部 PASS；独立不良数量 marker `ADP_E2E_20260717141017_WOM_BAD_QTY` 已通过 WOM/QCS 真实入口、API 幂等、QCS 关联、WMS 合格/冻结数量分配、PostgreSQL 事件账本、冲销和清理。material/WMS、ProcessAnalysis、生产列表导出、WOM 二维码、可见手工新建指令单和 RM Web 配方编辑继续保持 PASS。历史 WOM 工具栏基线 `ADP_E2E_20260622131959_WOMSTART_HOLD_RESTART`、taskId `9000006343993284`、generatedAt `2026-06-22T13:21:24.930Z` 保留用于回归锚定。 | 测试矩阵已无阻断，但 public `produceTaskCreated` 仍有 1 个产品范围 backlog；真实现场 Batch/DCS 投递确认和业务负责人签字仍是生产切换前置。 |
| G-014 | Nacos 配置链路 | PARTIAL | render/publish 脚本、测试环境配置路径和 `make smoke-nacos-config` 入口存在；`metadata/nacos-config-drift-smoke.json` 已从 `100.99.133.43` 运行态 Nacos group `prod` 拉取 44 个 dataId，20 个关键检查 PASS，Oracle 残留 0，并验证 91 个注册服务中 18/18 个关键服务存在 healthy 实例，记录 27 个 hash drift；`make runtime-smoke-reports-check` 会持续校验该报告结构、PASS 状态和 secret hygiene | 生产 Nacos export/diff、drift 人工审阅、签名 patch 包和回退演练未补 |
| G-015 | Keycloak/JWT 链路 | PARTIAL | realm 初始化、JWT public key 同步脚本、source/target realm inventory 和对账工具已建立；`metadata/keycloak-jwt-runtime-smoke.json` 已在 `100.99.133.43` 验证 19/19 PASS：realm `dt`、`pc_dt/mobile_dt`、`supos` mapper、Nacos healthy keycloak 实例、Keycloak 公钥 hash 与 Nacos JWT hash 一致、网关登录和菜单加载均通过；`make runtime-smoke-reports-check` 会持续校验该报告的关键链路检查和 secret hygiene | 生产 realm export/import、用户迁移、client secret 轮换、生产 JWT 同步后的登录 smoke 和数据库备份恢复演练未补 |
| G-016 | PostgreSQL 运行与迁移治理 | PARTIAL | PostgreSQL migration index、watch rationale 和 mapper audit 已纳入门禁；`metadata/postgres-runtime-smoke.json` 已在 `100.99.133.43` 验证 PostgreSQL 15.18、1474 张 public 表、150 个 view、32/32 关键表、15/15 兼容列、8/8 兼容索引 PASS；`make runtime-smoke-reports-check` 会持续校验该报告的关键表/兼容列/索引检查；`make postgres-migration-check` 阻断清库类高风险语句，并要求 watch 语句具备 `DROP ... IF EXISTS` 或 `DELETE ... WHERE` 保护；`073-auth-user-lock-status-compat.sql` 已覆盖 `auth_user.error_count` 兼容缺口；`074-rbac-roleuser-valid-default.sql` 已覆盖 `rbac_roleuser.valid` 默认值兼容缺口；`075-rbac-data-resource-permission-tables.sql` 已覆盖 RBAC 数据资源权限缺表和继承同步兼容缺口；`079-wom-wait-put-records-table.sql` 已覆盖 WOM `WOM_WAIT_PUT_RECORDS` 缺表；`084`/`085` 已覆盖 WOM 完工报工弹窗 runtime extra view/linkage；`101` 已覆盖 WOM 产出明细报工完工数量同步；`104`/`105`/`106` 已覆盖 WOM 工序开始/结束 PostgreSQL 兼容缺口；`107` 已覆盖 QCS 请检/报告明细表缺口；`166`/`167` 已覆盖 WOM checkoutBill 复跑暴露的 `EC_MODULE.code LIKE 'QCS_%'` PostgreSQL legacy LIKE 类型解析兼容缺口 | 生产数据迁移和每个业务动作落库仍未完成 |
| G-017 | runtime patch | PARTIAL | runtime patch 脚本、WOM 动作页 JSON、引用视图 JSON、列表按钮 JSON SQL、完工报工弹窗 runtime JSON/linkage SQL、简易活动报工按钮 SQL、WOM 配置默认值 patch、runtime patch checksum manifest、Nacos/runtime patch evidence manifest 和 strict READY 校验器已建立；测试环境 WOM 核心 JAR/静态补丁已真实回退并恢复，两个版本分别完成本次 Spring Boot 启动、Nacos healthy 注册和页面 200，恢复后环境 `9/9`、制造指令和工具栏 marker 回归 PASS | 测试环境核心补丁回退已完成；真实生产 Nacos diff、签名 patch 包、post-publish smoke 和全栈生产回切演练仍未补，material/WMS 入库回写已通过源码模块、部署和 marker 验收 |
| G-018 | 业务模块完整测试用例 | PARTIAL | 53 个既有业务页面 smoke 已有证据；生产模块矩阵当前 `44 PASS / 0 BLOCKED / 0 NOT_RUN`；落库总账当前 `200 PASS / 0 FAIL / 1 BLOCKED / 2 NOT_APPLICABLE`。独立不良数量 marker `ADP_E2E_20260717141017_WOM_BAD_QTY` 已形成 WOM/QCS/WMS/PostgreSQL 完整证据；历史 WOM 工具栏基线 `ADP_E2E_20260622131959_WOMSTART_HOLD_RESTART`、taskId `9000006343993284`、generatedAt `2026-06-22T13:21:24.930Z` 继续保留。PATROL 计划/取消、配置 CRUD、执行、异常隐患、统计/监控和 Kafka 消费均有真实证据；material/WMS、ProcessAnalysis、生产列表导出、二维码、手工新建和 RM Web 编辑已闭合；BPI marker `BPI_LIVE_20260720_123058` 已闭合同一指令 START/END，marker `ADP_E2E_20260721021607_QCS_BPI` 又闭合真实 WOM/QCS 页面、事务 outbox、Kafka、BPI 首发/重放和双库清理；QCS 合格/不合格双分支补齐列表、编辑、只读显示和冷启动证据；实体模型字段类型 marker `ADP_E2E_20260721075749_PG_TYPE_MATRIX` 以 `36/36 PASS` 补齐标量类型链，`ADP_E2E_20260721084718_PG_OBJECT_ASSOC` 又以 `10/10 PASS` 补齐 `OBJECT` 一对一/多对一关联、回滚和清理证据。 | PATROL 仍缺真实测点历史、`SESGISConfig` 和真实 SESH；public `produceTaskCreated` 产品范围决定、现场 Batch/DCS 切换确认和业务负责人签字未完成；BPI 还需物理设备同等序列、正式批准校准、7-14 天影子验收及外部 ERP/WMS 查单/冲销/补偿演练。 |
| G-019 | PostgreSQL 缺口进入幂等 SQL/backlog | PARTIAL | Oracle/PostgreSQL 审计、init SQL、backlog 和 watch rationale 已纳入 CI；`metadata/persistence-acceptance.json` 当前有 `203` 项，其中 `200 PASS / 0 FAIL / 1 BLOCKED / 2 NOT_APPLICABLE`；迁移库存为 `197 scripts`、`78 watch statements`、`0 watch safety issues`。迁移 193 增加默认关闭的 QCS -> BPI transactional outbox，迁移 194-196 补齐 QCS 显示兼容，迁移 197 退役与应用自有 ModelSync/FieldSync 竞争的 fallback trigger；marker `ADP_E2E_20260721021607_QCS_BPI` 已通过真实 QCS/Kafka/BPI 链，marker `ADP_E2E_20260721071510_PG_FIELD` 以 `33/33 PASS` 通过字段约束与索引验收，marker `ADP_E2E_20260721075749_PG_TYPE_MATRIX` 以 `36/36 PASS` 覆盖 23 种标量类型，marker `ADP_E2E_20260721084718_PG_OBJECT_ASSOC` 又以 `10/10 PASS` 覆盖 `OBJECT` 一对一/多对一关联、LONG/TEXT/BAPCODE 存储、失败回滚和 0/0/0/0 清理。既有目标受控 QCS -> Kafka -> material-wms -> receipt 链、原命令核对以及内部 WMS 停机/DLQ/恢复也已直接核对 BPI 与 material 两库；`metadata/production-module-backlog.json` 当前只保留 1 个 public `produceTaskCreated` 产品范围项，其中 `0 FAIL_BACKLOG / 1 BLOCKED`。 | 不能证明所有未来发现项已闭环，需要持续记录 |
| G-020 | 生产迁移前置项 | BLOCKED | 生产迁移 readiness、cutover 总闸门、9 轨 rehearsal、数据库行数/checksum 对账、MinIO/Keycloak/Nacos/runtime patch、回滚、license、network/TLS、安全加固和 business smoke 签字门禁均已建立；当前 cutover 为 `NOT_READY_FOR_PRODUCTION_CUTOVER`，9 个 gate 中 8 个 PLANNED、1 个 BLOCKED，生产账本为 0 个 blocker、1 个 backlog；rehearsal 为 `REHEARSAL_BLOCKED`，9 个 track 中 8 个 PLANNED、1 个 BLOCKED。独立不良数量已退出阻断账本；business smoke signoff 校验器继续要求剩余 backlog 和未解决导出 target 逐项解决或提供结构化风险接受。 | 数据迁移/回滚演练、真实 license 决策、真实 MinIO/Keycloak 迁移、真实生产域名/TLS、安全加固、public `produceTaskCreated` 产品范围、现场 Batch/DCS 投递确认和真实业务 smoke 签字均未 READY；当前门禁只能证明“不允许切生产”及账本一致性。 |
| G-021 | 智能批次与工艺数据中心（BPI） | PARTIAL | BPI 设计、契约、Java 8 adapter、Java 17 PostgreSQL 服务、操作台和 Flink 数据面已建立；Phase 1 继续保持 shadow-only。marker `BPI_LIVE_20260720_123058` 已用真实 WOM start/stop、受控 MQTT、Kafka/Flink/PostgreSQL 和真实浏览器闭合同一影子批次。Flyway V24 marker `ADP_E2E_20260721053253_BPI_FORCE_CLOSE` 闭合异常人工兜底和职责分离。marker `ADP_E2E_20260721021607_QCS_BPI` 又从真实 WOM/QCS 页面生成生效合格报告，通过同事务 outbox、Java 8 sidecar 和三 broker Kafka 把影子批次推进到 `RELEASED/r3/ACCEPTED`；同一事件重放后 gate/link/inbox/state/audit/WMS-outbox 仍为 `1/1/1/2/2/0`，DLQ/lag 和双库残留均为 0，六开关恢复 false。受控认证 QCS 到 `material-wms` 的完工入库、原命令核对和内部 WMS 停机/DLQ/恢复也已通过。证据在 `metadata/qcs-bpi-quality-gate-target-acceptance.json`、`metadata/bpi-force-close-target-acceptance.json`、`metadata/bpi-quality-release-wms-target-acceptance.json`、`metadata/bpi-wms-reconciliation-target-acceptance.json` 和 `metadata/bpi-wms-outage-recovery-target-acceptance.json`。 | 当前仍缺物理 DEVICE/GATEWAY 与真实现场证书、真实连续 7-14 天影子运行、正式身份系统第二管理员会话、外部 ERP/WMS 查单协议、响应丢失、冲销/补偿、真实负载跨组件整体回切和受控生产激活；Phase 3/4 训练数据与建议型模型未开始。 |

## 本轮 WOM 工具栏最新证据

2026-06-22 21:21 再次执行 `make smoke-wom-toolbar-row`，API/DB 基准为
`http://100.99.133.43:18080`，浏览器入口为 `http://222.88.185.146:18080`。
最新 marker `ADP_E2E_20260622131959_WOMSTART_HOLD_RESTART`、taskId
`9000006343993284`、`generatedAt=2026-06-22T13:21:24.930Z`，状态仍为
`PASS_WITH_KNOWN_BLOCKERS`。本轮确认 `WOM.custom...` 和 `ec.common.tableNo`
不再泄漏；无选中时 8 个行按钮统一提示 `请先选择一条指令单！`，选中 marker 后
查询、仅查待办、清空、保持、重启和结束报工入口均完成真实点击或接口复验。
PostgreSQL 回读 `wom_produce_tasks.id=9000006343993284` 最终为
`WOM_runState/runing/status=99/version=5`，`wom_wait_put_records.proc_report_id=758002551887104`。
ProcessAnalysis 追溯已在 2026-07-10 由源码模块、真实 WOM 按钮和 PostgreSQL marker 验收闭合。
2026-07-17 又通过 `metadata/wom-qrcode-browser-acceptance.json` 和
`metadata/wom-qrcode-persistence-acceptance.json` 关闭二维码历史 404：真实普通点击生成
两张 `320x320` PNG，PostgreSQL 写入两行，幂等、冲突拒绝、打印状态回填和 marker
清理均通过。当前工具栏 8 个动作全部 PASS；上面的 `PASS_WITH_KNOWN_BLOCKERS`
仅描述六月历史 row-smoke，不是当前依赖状态。

## 当前最高优先级缺口

1. 把已通过的 BPI 同一 WOM 指令 MQTT START/END 联合验收、目标环境 Flyway V22、自动点位目录、校准稳定分页、topology/rule
   产品化、版本比较、职责分离审批、受控退役、savepoint、单 broker 故障和应用组件双向回退固化为
   发布回归基线，继续补真实业务负载下的跨组件整体回切。
2. JetLinks 产品/设备、`instantFlow` metadata、单位以及受控 MQTT + MES production context 同一指令 START/END candidate/batch 已闭合；下一步由现场计量人员提交正式证书，由独立管理员审核其与目录 `calibrationVersion` 精确匹配，并用物理 DEVICE/GATEWAY epoch、连续单调 sequence 和断电恢复重跑整条链，随后进入 7-14 天真实影子运行。
3. 处理既有生产矩阵剩余 BLOCKED，继续制造指令、报工、请检、质量处置、完工入库和批次追溯核心链，不扩散到低优先级模块。
4. 扩展基础配置与生产迁移 rehearsal，但在真实业务 smoke 签字前保持 `NOT_READY_FOR_PRODUCTION_MIGRATION` 和 `NOT_READY_FOR_PRODUCTION_CUTOVER`。

## 更新规则

- 任何目标项改为 `READY` 前，必须有当前仓库内 artifact 或真实运行证据说明。
- 平台、生产和基础配置功能不能只用源码推断，必须回写 `docs/frontend-functional-test-report.md`、`docs/backend-table-audit/persistence-acceptance.md` 和 `metadata/persistence-acceptance.json`。
- 生产模块完整功能未通过前，`G-013` 必须保持 `BLOCKED` 或 `FAIL`。
- 生产迁移签字未完成前，`G-020` 必须保持 `BLOCKED` 或 `PARTIAL`。
- BPI 真实 IoT/MES 接入、目标产线 7-14 天影子运行和后续写回门槛未完成前，`G-021` 必须保持 `PARTIAL` 或 `BLOCKED`。
- 总状态只有在全部目标项 `READY` 后才能改为 `COMPLETE`。
