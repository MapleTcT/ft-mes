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
| G-001 | 可持续开发仓库基础 | READY | 父 POM、Makefile、`.gitignore`、CI、库存和门禁已建立；`metadata/ci-required-file-inventory.json` 当前记录 1255 个 CI/治理/runtime 依赖资产，其中 1255 tracked、0 untracked、0 missing references，覆盖全部可维护 source-module、PostgreSQL 迁移、真实验收和 verifier，本轮新增 RM Web 配方编辑源码、迁移 190、桌面/移动截图、API/六表落库报告及门禁也已跟踪；`make ci` 包含 strict tracking，必需文件游离在 Git 外会直接失败。 | 继续随模块提升补测试；新增或移动治理资产后继续运行 `make ci` |
| G-002 | 当前内容迁移 | PARTIAL | `docs/current-content-inventory.md` 和源码恢复目录 | 业务模块动作级源码/表关系仍需继续排查 |
| G-003 | Oracle 替换为 PostgreSQL 默认路径 | PARTIAL | Docker/POM 默认 PostgreSQL；`metadata/oracle-replacement-status.json` 当前汇总 `blockingIssueCount=0`、`runtimeConfigActiveOracleLineCount=0`、`directOracleDependencyCount=2`、`oracleBacklogReferenceCount=1574`、`postgresMigrationHighRiskCount=0`、`postgresMapperAuditErrorCount=0`；Oracle 只允许 legacy/profile/backlog；`metadata/oracle-migration-audit.json` 当前记录 1574 条已分类引用、0 条未分类引用；PATROL 运行元数据引用归类为 `postgres-compat-reference`，Java `rowNum` 回调/索引和 JavaScript `rowNum` / `Number(...)` 均按非 SQL 噪声分类，真实 SQL 方言仍被阻断；模块准入门禁继续阻断不可检查包和 Oracle 默认路径 | Oracle backlog 仍有引用，需要逐模块迁移 |
| G-004 | 项目目标与交接说明 | READY | `docs/project-objectives.md`、`docs/sustainable-development.md` | 随真实验收继续更新 |
| G-005 | 后端落表排查交接入口 | READY | `docs/backend-table-audit-handoff.md`、`docs/backend-table-audit/00-index.md`、`docs/backend-table-audit/persistence-acceptance.md` 和 `make backend-table-audit-handoff-check` 已建立；门禁会校验交接文档、索引报告表、已开始/已完成专项报告文件、落库验收汇总、PostgreSQL 默认、当前测试环境和不允许清库绕过规则 | 表级业务含义仍需专门线程执行 |
| G-006 | 平台登录/认证 | READY | 2026-06-21 当前地址平台综合 smoke 复跑 `6/6` PASS：API base 和 browser base 均为 `http://100.99.133.43:18080`；platform API `16/16`、首页待办、组织部门点击、RBAC authority `9/9`、菜单抽样 `40/40`；证据 `metadata/platform-validation-smoke.json` 和 `/tmp/adp-platform-validation-1009913343-20260621052958/platform-validation-summary.json` | 保持回归 smoke |
| G-007 | 用户/人员/岗位/公司/组管理 | READY | 组织部门/组/岗位/公司/人员 CRUD 已落库；人员勾选创建账号已落库；独立用户管理账号新增/编辑/锁定/解锁/删除已落库；角色新增/编辑/删除、角色绑定/解绑用户已落库；2026-06-20 当前地址用公网浏览器入口和 `100.99.133.43` PostgreSQL 重新复验部门、组、岗位、公司、人员、人员创建账号和独立用户管理账号 PASS；用户管理 marker `ADP_E2E_20260620071641_AUSR`，证据 `/tmp/adp-auth-user-persistence-1009913343-current.json` | 保持回归 smoke |
| G-008 | 组织管理 | READY | 部门、组、岗位、公司、人员新增/编辑/删除已用 marker 查 PostgreSQL；人员 `createUser=true` 已证明 `org_person` 和 `auth_user` 绑定、同步和软删除；2026-06-20 当前地址复验 marker 覆盖 `ADP_E2E_20260620061140_ORGDEP`、`ADP_E2E_20260620063543_GRP/POS/COM`、`ADP_E2E_20260620063832_PER/PUSR`；岗位-角色关联 marker `ADP_E2E_20260620123813_ORGPOSROLE` 已证明 `POST/GET/DELETE /inter-api/organization/v1/position/role` 写入 `org_position_role`、兼容视图 `base_roleposition.valid=1`，解绑后两者清空；证据 `/tmp/adp-organization-position-role-persistence-1009913343-current.json` | 保持组织管理 CRUD、岗位角色关联和 RBAC 权限关系回归 smoke |
| G-009 | 权限/RBAC | READY | 2026-06-20 `100.99.133.43:18080` RBAC authority smoke `9/9` PASS；RBAC 落库复验 marker `ADP_E2E_20260620070327_RBAC` 已通过，API base `http://100.99.133.43:18080`、真实浏览器 base `http://222.88.185.146:18080`；角色新增/编辑/删除、角色用户绑定/解绑、`personmanage/addPerson` 角色/用户菜单操作权限新增/删除、角色/用户数据资源权限保存和关闭受控均返回 `200`，PostgreSQL 验证 `rbac_role`、`rbac_roleuser`、`rbac_rolepermission`、`rbac_userpermission`、`rbac_role_data_permission`、`rbac_role_data_permission_ctrl`、`rbac_user_data_permission`、`rbac_user_data_permission_ctrl` 均按预期写入、失效或删除；证据 `/tmp/adp-rbac-permission-persistence-1009913343-current.json` | 保持回归 smoke |
| G-010 | 菜单导航 | READY | 2026-06-21 当前地址真实浏览器菜单抽样复跑 `40/40` PASS：API base 和 browser base 均为 `http://100.99.133.43:18080`；无页面级阻断错误 | 继续扩大菜单覆盖 |
| G-011 | 待办 | READY | 2026-06-21 当前地址首页待办 smoke PASS：`http://100.99.133.43:18080` 导航 `200`，无 visible/network/console/page error | 继续保留回归 |
| G-012 | 基础配置 | PARTIAL | 配置类 init SQL 和业务页面 smoke 有覆盖；系统编码字典项/字典值 CRUD 已在当前地址用 marker `ADP_E2E_20260620072542_SYSCODE` 复验 `sys_entity`、`sys_code` 的 PostgreSQL 新增/编辑/软删除，证据 `/tmp/adp-systemcode-persistence-1009913343-current.json`；系统配置已在当前地址用 marker `ADP_E2E_20260620073225_SCFG` 复验 app 配置目录/配置项新增、配置值更新读取、删除清理，证据 `/tmp/adp-systemconfig-persistence-1009913343-current.json` 和 `metadata/systemconfig-persistence-acceptance.json`；系统配置内置目录已在 `100.99.133.43` 同地址复跑只读 smoke，7/7 个预期内置目录列表/详情 API、风险分类、编辑策略边界和 `systemconfig_config_catalog` PostgreSQL 元数据验证通过，身份目录、打印授权、AK/SK、密码策略等 4 个敏感目录只读，配置值仅以数量/hash 摘要保存，证据 `metadata/systemconfig-builtins-readiness-smoke.json`；QCS `reportShowIndexRange` 已通过 `metadata/systemconfig-controlled-runtime-config-acceptance.json` 在真实 `/systemconfig/#/sysconfig` 页面上下文完成专用 marker 保存、详情/按模块回读、PostgreSQL before/after 和立即回滚，远程库已恢复 `qualityStd`；配置回滚后 marker `ADP_E2E_20260621055313_WOM_CHECKOUTBILL` 已通过 `metadata/qcs-runtime-config-wom-checkoutbill-regression.json` 复跑 WOM checkoutBill 质量活动生成 QCS 请检单，且修复 `EC_MODULE.code LIKE 'QCS_%'` 暴露的 PostgreSQL legacy LIKE 类型兼容缺口（SQL 166/167）；配置回滚后 marker `ADP_E2E_20260621061644_WOM_MANU_INSPECT` 已通过 `metadata/qcs-runtime-config-wom-manu-inspect-regression.json` 复跑 WOM createManuInspect 制造请检生成 QCS 请检单，PostgreSQL 确认 WOM/QCS/批次明细真实落库；配置回滚后 marker `ADP_E2E_20260621065620_QCS_REPORT_QUAL` 已通过 `metadata/qcs-report-chain-qualified-current.json` 复跑 QCS 报告保存、合格生效和 WOM/批次回写；marker `ADP_E2E_20260621070426_QCS_UNQLF` 已通过 `metadata/qcs-report-chain-unqualified-current.json` 复跑 QCS 报告保存、不合格生效、WOM/批次回写并自动生成不合格处理单；RM.MQ `brokerUrl` 已通过 `metadata/systemconfig-controlled-runtime-config-rm-current.json` 在真实 `/systemconfig/#/sysconfig` 页面上下文完成专用 marker 保存、详情/按模块回读、PostgreSQL before/after 和立即回滚，远程库已恢复 `localhost`；回滚后 marker `ADP_E2E_20260621073500_RM_CFG_REG` 已通过 `metadata/rm-runtime-config-batch-sync-regression.json` 复跑 RM 批控配方同步/删除，`rm_formulas.id=757563696166144` 先写入 `valid=true/status=88` 后软删 `valid=false/status=0` 且有效 `rm_process_actives=0`；BaseSet `isEnable` 已通过 `metadata/systemconfig-controlled-runtime-config-baseset-current.json` 在真实 `/systemconfig/#/sysconfig` 页面上下文完成 typed mutation，值从 `true` 临时切到 `false`，详情/按模块回读和 PostgreSQL before/after 均确认后立即回滚为 `true`；回滚后 marker `ADP_E2E_20260621180558_WOMSTART` 已通过 `metadata/baseset-runtime-config-wom-start-regression.json` 复跑 WOM start 落库，marker `ADP_E2E_20260621180914_WOM_MANU_INSPECT` 已通过 `metadata/baseset-runtime-config-wom-manu-inspect-regression.json` 复跑 WOM 制造请检/QCS 检验单生成落库；低代码 customProperty 模型映射已通过 `metadata/custom-property-persistence-acceptance.json` 在真实 `/supplant/#/customFieldModelManage` 页面上下文完成 marker `ADP_E2E_20260621184706_CUSTOM_PROPERTY` 的启用、编辑、PostgreSQL before/after、恢复、禁用和受控清理，并由 SQL 169 兼容 legacy `project_property` 表名；实体/模型元数据 CRUD 已通过 `metadata/entity-model-config-crud-persistence-acceptance.json` 在真实 `/msService/ec/engine/msManage` 页面上下文完成 marker `ADP_E2E_202606221433_ENTITY_MODEL` 的实体新增、模型新增、实体编辑、模型编辑、模型删除、实体删除，6 个写请求均 `HTTP 200/success=true`，PostgreSQL 确认 `ec_entity=1`、`ec_model=1`、内置 `ec_property=3`、编辑 marker、软删和受控清理为 0；该报告明确不声明 PostgreSQL 自动创建物理模型表；`metadata/basic-config-action-matrix.json` 将 16 个基础配置动作拆成 `6 PASS / 4 READ_ONLY_GUARDED / 3 CONTROLLED_MARKER_REQUIRED / 3 PLANNED`，并由 `make basic-config-action-matrix-check` 交叉校验禁改项、专用 marker 要求、readiness 证据，以及 QCS/RM/BaseSet 受控运行配置与 `metadata/production-module-test-cases.json` 中生产用例状态的契约；实体/模型/低代码配置运行时已在 `100.99.133.43` 同地址复跑只读 readiness smoke，`customPropertyTree`、service manager、`ec` 工程期/模块配置 API 5/5 PASS，真实浏览器配置页 4/4 PASS，PostgreSQL `ec_entity/ec_model/ec_field/ec_view/runtime_view/runtime_extra_view/runtime_button/rbac_menuinfo` 元数据可读，证据 `metadata/runtime-configuration-readiness-smoke.json`；Nacos/Keycloak 测试环境运行态 smoke 已纳入 `make basic-config-coverage-check`，校验 `100.99.133.43`、PostgreSQL、Nacos prod group 关键配置、服务健康、JWT hash 同步、gateway 登录和 current-user 菜单；`metadata/basic-config-coverage.json` 明确 6 个基础配置区块当前为 `3 PASS / 3 PARTIAL / 0 NOT_RUN`，会阻止把局部通过、测试环境 runtime smoke、实体配置只读 readiness、内置目录只读证据、测试环境 Nacos/Keycloak runtime smoke 或实体/模型物理表自动创建未验收项误判为 G-012 完成 | QCS 受控配置回滚后的报告保存、合格/不合格生效回写和不合格处理单自动生成已完成当前地址复验；RM.MQ brokerUrl 受控配置保存/回滚和 RM 批控同步/删除回归已完成当前地址复验；BaseSet.isEnable 受控配置保存/回滚和 WOM/QCS 代表性回归已完成当前地址复验；customProperty 字段映射已完成当前地址复验；BaseSet/RM/QCS 其他配置项、RM 敏感配置和其他内置目录写操作、PostgreSQL 物理模型表自动创建、Nacos/Keycloak 生产 export/diff、realm 迁移、secret 轮换和回退演练仍需继续验收 |
| G-013 | 生产模块完整功能 | BLOCKED | 生产模块测试矩阵当前 `42 PASS / 2 BLOCKED / 0 NOT_RUN`、44 条 route smoke 全部 PASS；material/WMS、ProcessAnalysis、生产列表导出、WOM 二维码、可见手工新建指令单和 RM Web 配方编辑均已通过真实浏览器/API/PostgreSQL 或文件响应验收。RM marker `ADP_E2E_20260717120436_RM_WEB_FORMULA` 连续三轮完成创建、幂等、两次修订、隔离投递失败/重试、六表回读、桌面/移动端和清理，64 位 ID 按字符串传输；二维码与手工新建 marker 分别保持 10/10 和 9/9 PASS。历史 WOM 工具栏基线 `ADP_E2E_20260622131959_WOMSTART_HOLD_RESTART`、taskId `9000006343993284`、generatedAt `2026-06-22T13:21:24.930Z` 保留用于回归锚定。 | 2 条生产用例仍 BLOCKED：PROD-019/PROD-021 独立不良数量范围确认；public `produceTaskCreated` 仍作为独立产品范围 backlog。真实现场 Batch/DCS 投递确认作为生产切换前置单独验收。 |
| G-014 | Nacos 配置链路 | PARTIAL | render/publish 脚本、测试环境配置路径和 `make smoke-nacos-config` 入口存在；`metadata/nacos-config-drift-smoke.json` 已从 `100.99.133.43` 运行态 Nacos group `prod` 拉取 44 个 dataId，20 个关键检查 PASS，Oracle 残留 0，并验证 91 个注册服务中 18/18 个关键服务存在 healthy 实例，记录 27 个 hash drift；`make runtime-smoke-reports-check` 会持续校验该报告结构、PASS 状态和 secret hygiene | 生产 Nacos export/diff、drift 人工审阅、签名 patch 包和回退演练未补 |
| G-015 | Keycloak/JWT 链路 | PARTIAL | realm 初始化、JWT public key 同步脚本、source/target realm inventory 和对账工具已建立；`metadata/keycloak-jwt-runtime-smoke.json` 已在 `100.99.133.43` 验证 19/19 PASS：realm `dt`、`pc_dt/mobile_dt`、`supos` mapper、Nacos healthy keycloak 实例、Keycloak 公钥 hash 与 Nacos JWT hash 一致、网关登录和菜单加载均通过；`make runtime-smoke-reports-check` 会持续校验该报告的关键链路检查和 secret hygiene | 生产 realm export/import、用户迁移、client secret 轮换、生产 JWT 同步后的登录 smoke 和数据库备份恢复演练未补 |
| G-016 | PostgreSQL 运行与迁移治理 | PARTIAL | PostgreSQL migration index、watch rationale 和 mapper audit 已纳入门禁；`metadata/postgres-runtime-smoke.json` 已在 `100.99.133.43` 验证 PostgreSQL 15.18、1474 张 public 表、150 个 view、32/32 关键表、15/15 兼容列、8/8 兼容索引 PASS；`make runtime-smoke-reports-check` 会持续校验该报告的关键表/兼容列/索引检查；`make postgres-migration-check` 阻断清库类高风险语句，并要求 watch 语句具备 `DROP ... IF EXISTS` 或 `DELETE ... WHERE` 保护；`073-auth-user-lock-status-compat.sql` 已覆盖 `auth_user.error_count` 兼容缺口；`074-rbac-roleuser-valid-default.sql` 已覆盖 `rbac_roleuser.valid` 默认值兼容缺口；`075-rbac-data-resource-permission-tables.sql` 已覆盖 RBAC 数据资源权限缺表和继承同步兼容缺口；`079-wom-wait-put-records-table.sql` 已覆盖 WOM `WOM_WAIT_PUT_RECORDS` 缺表；`084`/`085` 已覆盖 WOM 完工报工弹窗 runtime extra view/linkage；`101` 已覆盖 WOM 产出明细报工完工数量同步；`104`/`105`/`106` 已覆盖 WOM 工序开始/结束 PostgreSQL 兼容缺口；`107` 已覆盖 QCS 请检/报告明细表缺口；`166`/`167` 已覆盖 WOM checkoutBill 复跑暴露的 `EC_MODULE.code LIKE 'QCS_%'` PostgreSQL legacy LIKE 类型解析兼容缺口 | 生产数据迁移和每个业务动作落库仍未完成 |
| G-017 | runtime patch | PARTIAL | runtime patch 脚本、WOM 动作页 JSON、引用视图 JSON、列表按钮 JSON SQL、完工报工弹窗 runtime JSON/linkage SQL、简易活动报工按钮 SQL、WOM 配置默认值 patch、runtime patch checksum manifest、Nacos/runtime patch evidence manifest 和 strict READY 校验器已建立；测试环境 WOM 核心 JAR/静态补丁已真实回退并恢复，两个版本分别完成本次 Spring Boot 启动、Nacos healthy 注册和页面 200，恢复后环境 `9/9`、制造指令和工具栏 marker 回归 PASS | 测试环境核心补丁回退已完成；真实生产 Nacos diff、签名 patch 包、post-publish smoke 和全栈生产回切演练仍未补，material/WMS 入库回写已通过源码模块、部署和 marker 验收 |
| G-018 | 业务模块完整测试用例 | PARTIAL | 53 个既有业务页面 smoke 已有证据；生产模块矩阵当前 `42 PASS / 2 BLOCKED / 0 NOT_RUN`；落库总账当前 `167 PASS / 0 FAIL / 2 BLOCKED / 2 NOT_APPLICABLE`。WOM 可见手工新建指令单、二维码和 RM Web 配方编辑均有真实浏览器/API/PostgreSQL marker，RM 连续三轮通过且模块 10 条单测通过；历史 WOM 工具栏基线 `ADP_E2E_20260622131959_WOMSTART_HOLD_RESTART`、taskId `9000006343993284`、generatedAt `2026-06-22T13:21:24.930Z` 继续保留。PATROL 计划/取消、配置 CRUD、执行、异常隐患、统计/监控和 Kafka 消费均有真实证据；material/WMS、ProcessAnalysis 和生产列表导出已闭合。 | PATROL 技术消费链已通过，但测试库 `tmm_tags=0` 且无测点历史，无法验收 `gather_data` 中位数落库和误差图表；GIS 定位/轨迹缺 `SESGISConfig`，完整整改/复查/销项缺真实 SESH；另有 2 条生产用例、public `produceTaskCreated` 产品范围决定、现场 Batch/DCS 切换确认和业务负责人签字未完成。 |
| G-019 | PostgreSQL 缺口进入幂等 SQL/backlog | PARTIAL | Oracle/PostgreSQL 审计、init SQL、backlog 和 watch rationale 已纳入 CI；`metadata/persistence-acceptance.json` 当前有 `171` 项，其中 `167 PASS / 0 FAIL / 2 BLOCKED / 2 NOT_APPLICABLE`；迁移库存为 `190 scripts`，PATROL 178-186 已完成目标复跑和真实功能验收，188 新增 WOM 二维码事务日序列和包装码表，189 新增 WOM 手工新建请求账本及活动批次 partial unique guard，190 新增 RM Web 编辑入口、修订与投递账本；`metadata/production-module-backlog.json` 用 3 个 active backlog 项覆盖未闭合产品范围项，其中 `0 FAIL_BACKLOG / 3 BLOCKED` | 不能证明所有未来发现项已闭环，需要持续记录 |
| G-020 | 生产迁移前置项 | BLOCKED | 生产迁移 readiness、cutover 总闸门、9 轨 rehearsal、数据库行数/checksum 对账、MinIO/Keycloak/Nacos/runtime patch、回滚、license、network/TLS、安全加固和 business smoke 签字门禁均已建立；当前 cutover 为 `NOT_READY_FOR_PRODUCTION_CUTOVER`，9 个 gate 中 8 个 PLANNED、1 个 BLOCKED，生产账本为 2 个 blocker、3 个 backlog；rehearsal 为 `REHEARSAL_BLOCKED`，9 个 track 中 8 个 PLANNED、1 个 BLOCKED。material-service、ProcessAnalysis、WOM 二维码、可见手工新建指令单和 RM Web 配方编辑均已退出阻断账本；business smoke signoff 校验器会交叉校验全部 2 个 blocker case、3 个 backlog 和未解决导出 target，要求逐项解决或提供结构化风险接受。 | 数据迁移/回滚演练、真实 license 决策、真实 MinIO/Keycloak 迁移、真实生产域名/TLS、安全加固、剩余产品范围、现场 Batch/DCS 投递确认和真实业务 smoke 签字均未 READY；当前门禁只能证明“不允许切生产”及账本一致性。 |
| G-021 | 智能批次与工艺数据中心（BPI） | PARTIAL | BPI 设计、契约、Java 8 adapter、Java 17 PostgreSQL 服务、操作台、Flink 数据面、transactional outbox、回执和审计已建立；目标环境已完成 Flyway V13 expand-only 迁移和 V12 savepoint 到 job `40408b7907ca7b97ad750cc7d2bfb345` 的有状态恢复，当前 `RUNNING/33-of-33`；独立 `APPLIED/REJECTED` 与 `READY/DEGRADED/INACTIVE` 回执、真实 PostgreSQL 字段和操作台展示均通过。 | 目标点位仍为 1 点/0 READY，试点设备未注册/激活且 metadata/单位/标定/连续单调 sequence 未就绪；真实遥测 + MES context 同 marker candidate/batch、版本比较/审批、broker/应用镜像/产品级回退、7-14 天影子运行、QCS/WMS 写回和训练数据阶段仍未完成。 |

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

1. 把已通过的 BPI 同一 marker 联合验收、目标环境 Flyway V12 自动点位目录和 topology/rule 产品化验收固化为发布回归基线，继续补版本比较、审批和产品级回退。
2. 先在 JetLinks 注册并激活 `bpi-pilot-device-01`，补 `instantFlow` metadata、单位、标定和 source sequence，导入新快照重新校验；READY 后与 MES production context 用同一 marker 闭合 candidate/batch，再进入 7-14 天影子运行。
3. 处理既有生产矩阵剩余 BLOCKED，继续制造指令、报工、请检、质量处置、完工入库和批次追溯核心链，不扩散到低优先级模块。
4. 扩展基础配置与生产迁移 rehearsal，但在真实业务 smoke 签字前保持 `NOT_READY_FOR_PRODUCTION_MIGRATION` 和 `NOT_READY_FOR_PRODUCTION_CUTOVER`。

## 更新规则

- 任何目标项改为 `READY` 前，必须有当前仓库内 artifact 或真实运行证据说明。
- 平台、生产和基础配置功能不能只用源码推断，必须回写 `docs/frontend-functional-test-report.md`、`docs/backend-table-audit/persistence-acceptance.md` 和 `metadata/persistence-acceptance.json`。
- 生产模块完整功能未通过前，`G-013` 必须保持 `BLOCKED` 或 `FAIL`。
- 生产迁移签字未完成前，`G-020` 必须保持 `BLOCKED` 或 `PARTIAL`。
- BPI 真实 IoT/MES 接入、目标产线 7-14 天影子运行和后续写回门槛未完成前，`G-021` 必须保持 `PARTIAL` 或 `BLOCKED`。
- 总状态只有在全部目标项 `READY` 后才能改为 `COMPLETE`。
