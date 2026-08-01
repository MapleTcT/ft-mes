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
| PARTIAL | 11 |
| BLOCKED | 1 |
| FAIL | 0 |
| NOT_STARTED | 0 |

## 目标项明细

| ID | 目标项 | 状态 | 当前证据 | 缺口 |
| --- | --- | --- | --- | --- |
| G-001 | 可持续开发仓库基础 | READY | 父 POM、Makefile、`.gitignore`、CI、库存和门禁已建立；本轮纳入 BPI Flyway V35 最新遥测投影、admin 533 节点权限与 398 路由全量扫描、通知送达专项、Phase 3C-F 真实页面证据、Phase 3C-G Parquet v2/Iceberg exact-version 证据、最小输送单元真实整链证据、MES 制造到追溯总流程脚本/迁移/机器证据、工厂架构新增产线真实页面/API/PostgreSQL/WOM 引用证据、果糖喷射液化至糖化试运行整链资产、QCS/WTS 配置与工作流回归、QCS 来料检验物料/采样点/供应商参照回归、QCS 其他检验与质量巡检动作恢复验收、WOM 手工新增鉴权边界回归、WOM 产耗查看及执行记录详情/产耗/统计回归、WOM 指令/活动/检查/投料/产出执行记录动作回归、WOM 退料/用料/报工/尾料与完工入库 12 页操作恢复及真实浏览器只读数据库验收、WTS 基础设置 11 页新增表单及作业时间约束 CRUD 验收、WTS 业务管理/统计分析 11 页与 24 个运行时布局动作验收、Qualify 10 个页面动作栏恢复及分类/资质主档 PostgreSQL CRUD 验收、RM 配方管理 9 个菜单/42 个动作恢复及主数据 CRUD、普通配方启用、Web 编辑与导出鉴权验收、LIMSSample 5 个业务阶段/24 个原页面导航合并验收和当前实现版产品交互说明书；WTS 业务/统计验收保留 `docs/wts-business-statistics-functional-acceptance-20260731.md` 和 `metadata/wts-business-statistics-runtime-acceptance-20260731.json`；RM 验收同时保留 `docs/rm-formula-management-functional-acceptance-20260731.md` 和 `metadata/rm-formula-management-acceptance-20260731.json`；WOM 本轮验收保留 `docs/wom-material-report-actions-functional-acceptance-20260801.md` 和 `metadata/wom-material-report-actions-acceptance-20260801.json`；样品菜单验收保留 `docs/testing/lims-sample-menu-consolidation-acceptance-20260801.md` 和 `metadata/lims-sample-menu-consolidation-acceptance-20260801.json`。`metadata/ci-required-file-inventory.json` 当前记录 2266 个必需文件：2266 tracked、0 untracked、0 missing references。Qualify 导入文件处理和提醒流程业务执行、连续生产启用、物理现场窗口与数据量、整站灾备、MLflow 生产治理和外部 ERP/WMS 补偿仍未完成。 | 继续随模块提升补测试；新增或移动治理资产后继续运行 `make ci` |
| G-002 | 当前内容迁移 | PARTIAL | `docs/current-content-inventory.md` 和源码恢复目录 | 业务模块动作级源码/表关系仍需继续排查 |
| G-003 | Oracle 替换为 PostgreSQL 默认路径 | PARTIAL | Docker/POM 默认 PostgreSQL；`metadata/oracle-replacement-status.json` 当前汇总 `blockingIssueCount=0`、`runtimeConfigActiveOracleLineCount=0`、`directOracleDependencyCount=2`、`oracleBacklogReferenceCount=1640`、`postgresMigrationHighRiskCount=0`、`postgresMapperAuditErrorCount=0`，PostgreSQL 迁移数为 `257`；Oracle 只允许 legacy/profile/backlog；`metadata/oracle-migration-audit.json` 当前记录 1640 条已分类引用、0 条未分类引用。新增工厂架构迁移 211-216 只使用 PostgreSQL 幂等路径；迁移 217 仅更新飞天品牌主题配置；迁移 218 通过 PostgreSQL 大对象运行元数据恢复四个 QCS 布局；迁移 219-221 恢复来料检验运行时、产品/来料手工新增入口，并将 WTS 全模块设计态与项目草稿态元数据转换为 PostgreSQL 大对象兼容格式；迁移 222 将 QCS 配置扩展视图转换为有效 PostgreSQL 大对象引用；迁移 223 修复 QCS 检验报告已发布及草稿配置中 Hibernate `@Lob` 字段；迁移 224-228 处理 admin 全目录权限、OEE 工厂树层级、旧 OID/bigint 比较、通知移动设备令牌表及 WOM 产耗详情视图兼容；迁移 229 修复 QCS 检验申请精确范围的 Hibernate `@Lob` 配置；迁移 230-234 恢复 WTS 基础设置动作布局、大对象兼容、危害动作权限/标签及风险措施脚本入口；迁移 235-238 恢复 Qualify 动作布局、资质分类根节点/助记码别名及默认等级系统配置；迁移 239 恢复 WOM 工序执行记录详情、产耗查看和工艺统计；迁移 240 恢复指令/活动/检查/投料/产出执行记录详情、追溯和统计；迁移 241-245 恢复 RM 配方管理运行时动作、助记码表、配方类型根节点及配方树/质量备注 PostgreSQL 大对象兼容；迁移 246 恢复 QCS 来料检验物料、采样点和供应商参照；迁移 247 恢复 QCS 其他检验和质量巡检的新增、打开、关闭、批量提交及删除动作；迁移 248-255 恢复 WTS 业务管理/统计分析 24 个布局、编辑子表格、台账查询/导出、资质子实体以及动火批量打印冲突兼容；迁移 256 恢复 WOM 退料、用料、报工、尾料和完工入库台账动作；迁移 257 将样品管理 24 个原页面按 5 个生命周期阶段重组并保留页面 ID、URL 和权限，均未引入 Oracle 默认路径；V34 文档政策引用均归类为 `documentation-or-workflow`，验收 SQL 的 PostgreSQL `decode(..., 'hex')` 归类为 `postgres-conversion-tooling`；既有 Spring JDBC `rowNum` 回调仍归类为 `java-row-index-noise`，不是 Oracle 默认路径。PATROL 运行元数据引用归类为 `postgres-compat-reference`，真实 SQL 方言仍被阻断；模块准入门禁继续阻断不可检查包和 Oracle 默认路径 | Oracle backlog 仍有引用，需要逐模块迁移 |
| G-004 | 项目目标与交接说明 | READY | `docs/project-objectives.md`、`docs/sustainable-development.md` | 随真实验收继续更新 |
| G-005 | 后端落表排查交接入口 | READY | `docs/backend-table-audit-handoff.md`、`docs/backend-table-audit/00-index.md`、`docs/backend-table-audit/persistence-acceptance.md` 和 `make backend-table-audit-handoff-check` 已建立；门禁会校验交接文档、索引报告表、已开始/已完成专项报告文件、落库验收汇总、PostgreSQL 默认、当前测试环境和不允许清库绕过规则 | 表级业务含义仍需专门线程执行 |
| G-006 | 平台登录/认证 | READY | 2026-06-21 当前地址平台综合 smoke 复跑 `6/6` PASS：API base 和 browser base 均为 `http://100.99.133.43:18080`；platform API `16/16`、首页待办、组织部门点击、RBAC authority `9/9`、菜单抽样 `40/40`；证据 `metadata/platform-validation-smoke.json` 和 `/tmp/adp-platform-validation-1009913343-20260621052958/platform-validation-summary.json` | 保持回归 smoke |
| G-007 | 用户/人员/岗位/公司/组管理 | READY | 组织部门/组/岗位/公司/人员 CRUD 已落库；人员勾选创建账号已落库；独立用户管理账号新增/编辑/锁定/解锁/删除已落库；角色新增/编辑/删除、角色绑定/解绑用户已落库；2026-06-20 当前地址用公网浏览器入口和 `100.99.133.43` PostgreSQL 重新复验部门、组、岗位、公司、人员、人员创建账号和独立用户管理账号 PASS；用户管理 marker `ADP_E2E_20260620071641_AUSR`，证据 `/tmp/adp-auth-user-persistence-1009913343-current.json` | 保持回归 smoke |
| G-008 | 组织管理 | READY | 部门、组、岗位、公司、人员新增/编辑/删除已用 marker 查 PostgreSQL；人员 `createUser=true` 已证明 `org_person` 和 `auth_user` 绑定、同步和软删除；2026-06-20 当前地址复验 marker 覆盖 `ADP_E2E_20260620061140_ORGDEP`、`ADP_E2E_20260620063543_GRP/POS/COM`、`ADP_E2E_20260620063832_PER/PUSR`；岗位-角色关联 marker `ADP_E2E_20260620123813_ORGPOSROLE` 已证明 `POST/GET/DELETE /inter-api/organization/v1/position/role` 写入 `org_position_role`、兼容视图 `base_roleposition.valid=1`，解绑后两者清空；证据 `/tmp/adp-organization-position-role-persistence-1009913343-current.json` | 保持组织管理 CRUD、岗位角色关联和 RBAC 权限关系回归 smoke |
| G-009 | 权限/RBAC | READY | 2026-06-20 `100.99.133.43:18080` RBAC authority smoke `9/9` PASS；RBAC 落库复验 marker `ADP_E2E_20260620070327_RBAC` 已通过，API base `http://100.99.133.43:18080`、真实浏览器 base `http://222.88.185.146:18080`；角色新增/编辑/删除、角色用户绑定/解绑、`personmanage/addPerson` 角色/用户菜单操作权限新增/删除、角色/用户数据资源权限保存和关闭受控均返回 `200`，PostgreSQL 验证 `rbac_role`、`rbac_roleuser`、`rbac_rolepermission`、`rbac_userpermission`、`rbac_role_data_permission`、`rbac_role_data_permission_ctrl`、`rbac_user_data_permission`、`rbac_user_data_permission_ctrl` 均按预期写入、失效或删除；证据 `/tmp/adp-rbac-permission-persistence-1009913343-current.json` | 保持回归 smoke |
| G-010 | 菜单导航 | READY | 2026-06-21 当前地址真实浏览器菜单抽样复跑 `40/40` PASS：API base 和 browser base 均为 `http://100.99.133.43:18080`；无页面级阻断错误 | 继续扩大菜单覆盖 |
| G-011 | 待办 | READY | 2026-06-21 当前地址首页待办 smoke PASS：`http://100.99.133.43:18080` 导航 `200`，无 visible/network/console/page error | 继续保留回归 |
| G-012 | 基础配置 | PARTIAL | 系统编码与普通 app 系统配置 CRUD 已完成 marker 落库；内置目录 7/7 只读 smoke 通过，QCS/RM/BaseSet 各有受控单项配置保存、回读、回滚和代表性业务回归；customProperty 模型映射已完成启用/编辑/恢复/禁用落库；实体/模型物理表、字段约束 `33/33`、23 类标量 `36/36`、`OBJECT` 关联 `10/10` 均已通过；`ADP_E2E_20260721101217_PG_FIELD_DELETE` 又以 `18/18 PASS` 证明普通软删保留列/数据、显式 `DROP COLUMN RESTRICT`、依赖/漂移 fail closed、DDL/元数据同事务回滚、附件元数据删除、固有主键保护、HTTP 200 业务错误和 0/0/0/0/0/0 清理。证据新增 `metadata/entity-model-field-delete-persistence-acceptance.json`，实现提交 `31baf5d0`；动作矩阵为 `9 PASS / 4 READ_ONLY_GUARDED / 3 CONTROLLED_MARKER_REQUIRED / 2 PLANNED`，覆盖账本为 `5 PASS / 2 PARTIAL` | 自动删列继续按设计禁用；身份、授权、密钥、密码类配置仍只读；其他 BaseSet/RM/QCS 配置项及 Nacos/Keycloak 生产 export/diff、realm 迁移、secret 轮换、登录 smoke 和回退演练未完成 |
| G-013 | 生产模块完整功能 | PARTIAL | 生产模块测试矩阵当前 `44 PASS / 0 BLOCKED / 0 NOT_RUN`、44 条 route smoke 全部 PASS，模块 blocker 与 active backlog 均为 0。`...CLOSED_10` 保留从空 marker 开始且完全不续跑的基线；最新 `ADP_E2E_20260727_MES_FULL_CLOSED_11` 透明记录了业务动作前的列表分页失败，修复后从未变化的 `waitForRun` 夹具继续并将八个业务阶段各执行一次。PostgreSQL 精确证明 2/2 活动执行、投料明细/物料消耗 `1/1`、产出明细/物料产出 `2/2`、任务产量 `5` 且已检/合格、QCS `1/1`、WMS `1/2/1` 和追溯快照 `3/3`，页面网络、console、page error 与可见 warning/error 均为 0。工具栏最新复验 `ADP_E2E_20260622131959_WOMSTART_HOLD_RESTART`（task `9000006343993284`，`2026-06-22T13:21:24.930Z`）继续保留；独立不良数量、material/WMS、ProcessAnalysis、生产导出、二维码、可见手工新建指令单和 RM Web 配方编辑保持 PASS，旧 public `produceTaskCreated` 已按 `NOT_APPLICABLE` 退役。 | 仓库内生产主链与投入/产出台账已闭合，但真实现场 Batch/DCS 投递确认、连续运行、回滚演练和业务负责人签字仍是生产切换前置，因此不能称为正式生产完成。 |
| G-014 | Nacos 配置链路 | PARTIAL | render/publish 脚本、测试环境配置路径和 `make smoke-nacos-config` 入口存在；`metadata/nacos-config-drift-smoke.json` 已从 `100.99.133.43` 运行态 Nacos group `prod` 拉取 44 个 dataId，20 个关键检查 PASS，Oracle 残留 0，并验证 91 个注册服务中 18/18 个关键服务存在 healthy 实例，记录 27 个 hash drift；`make runtime-smoke-reports-check` 会持续校验该报告结构、PASS 状态和 secret hygiene | 生产 Nacos export/diff、drift 人工审阅、签名 patch 包和回退演练未补 |
| G-015 | Keycloak/JWT 链路 | PARTIAL | realm 初始化、JWT public key 同步脚本、source/target realm inventory 和对账工具已建立；`metadata/keycloak-jwt-runtime-smoke.json` 已在 `100.99.133.43` 验证 19/19 PASS：realm `dt`、`pc_dt/mobile_dt`、`supos` mapper、Nacos healthy keycloak 实例、Keycloak 公钥 hash 与 Nacos JWT hash 一致、网关登录和菜单加载均通过；`make runtime-smoke-reports-check` 会持续校验该报告的关键链路检查和 secret hygiene | 生产 realm export/import、用户迁移、client secret 轮换、生产 JWT 同步后的登录 smoke 和数据库备份恢复演练未补 |
| G-016 | PostgreSQL 运行与迁移治理 | PARTIAL | PostgreSQL migration index、watch rationale 和 mapper audit 已纳入门禁；`metadata/postgres-runtime-smoke.json` 已在 `100.99.133.43` 验证 PostgreSQL 15.18、1474 张 public 表、150 个 view、32/32 关键表、15/15 兼容列、8/8 兼容索引 PASS；`make runtime-smoke-reports-check` 会持续校验该报告的关键表/兼容列/索引检查；`make postgres-migration-check` 阻断清库类高风险语句，并要求 watch 语句具备 `DROP ... IF EXISTS` 或 `DELETE ... WHERE` 保护；`073-auth-user-lock-status-compat.sql` 已覆盖 `auth_user.error_count` 兼容缺口；`074-rbac-roleuser-valid-default.sql` 已覆盖 `rbac_roleuser.valid` 默认值兼容缺口；`075-rbac-data-resource-permission-tables.sql` 已覆盖 RBAC 数据资源权限缺表和继承同步兼容缺口；`079-wom-wait-put-records-table.sql` 已覆盖 WOM `WOM_WAIT_PUT_RECORDS` 缺表；`084`/`085` 已覆盖 WOM 完工报工弹窗 runtime extra view/linkage；`101` 已覆盖 WOM 产出明细报工完工数量同步；`104`/`105`/`106` 已覆盖 WOM 工序开始/结束 PostgreSQL 兼容缺口；`107` 已覆盖 QCS 请检/报告明细表缺口；`166`/`167` 已覆盖 WOM checkoutBill 复跑暴露的 `EC_MODULE.code LIKE 'QCS_%'` PostgreSQL legacy LIKE 类型解析兼容缺口 | 生产数据迁移和每个业务动作落库仍未完成 |
| G-017 | runtime patch | PARTIAL | runtime patch 脚本、WOM 动作页 JSON、引用视图 JSON、列表按钮 JSON SQL、完工报工弹窗 runtime JSON/linkage SQL、简易活动报工按钮 SQL、WOM 配置默认值 patch、runtime patch checksum manifest、Nacos/runtime patch evidence manifest 和 strict READY 校验器已建立；测试环境 WOM 核心 JAR/静态补丁已真实回退并恢复，两个版本分别完成本次 Spring Boot 启动、Nacos healthy 注册和页面 200，恢复后环境 `9/9`、制造指令和工具栏 marker 回归 PASS | 测试环境核心补丁回退已完成；真实生产 Nacos diff、签名 patch 包、post-publish smoke 和全栈生产回切演练仍未补，material/WMS 入库回写已通过源码模块、部署和 marker 验收 |
| G-018 | 业务模块完整测试用例 | PARTIAL | 53 个既有业务页面 smoke 和 admin 全目录扫描已有证据；生产模块矩阵当前 `44 PASS / 0 BLOCKED / 0 NOT_RUN`；落库总账当前 `300 PASS / 1 FAIL / 0 BLOCKED / 39 NOT_APPLICABLE`。admin 的 533/533 权限节点无操作缺口，但 398 个可导航页面仅 `185 PASS / 42 WARN / 171 FAIL`，其中 169 个为旧包 HTTP 200 空白页。MES、组织、RBAC、PATROL 与 WTS 代表业务落库已通过；本轮 WTS 业务管理/统计分析 11 个页面和 24 个运行时布局共 35 项全部 PASS，5 个新增表单、动火/台账详情和三类导出完成真实浏览器验收，7 个空列表详情与未授权本机打印控件共 8 个子交互保持 BLOCKED；本轮 RM 配方管理 9 个入口与 42 个预期动作全部可见，配方类型/工序类型 marker 完成新增、修改、软删除查库，普通配方启用落库为 `valid=true/state=enabled/status=99`，批量配方 004/003/001 的 Web 编辑、模板下载、Excel 导入和导出均可操作，Web 编辑器读到 200 条配方及一工序/一活动 marker，最终浏览器与网络错误为 0。QCS 来料检验物料、采样点和供应商参照及未选物料中文校验 4/4 通过，但均为只读动作，按规则计入 NOT_APPLICABLE；既有 WOM 执行记录、WTS 基础设置及制造到追溯、果糖试运行、QCS/WTS 配置、BPI 数据集与遥测证据继续保留；最新 WOM 工具栏复验 marker `ADP_E2E_20260622131959_WOMSTART_HOLD_RESTART`、task `9000006343993284`、时间 `2026-06-22T13:21:24.930Z` 继续保留。消息中心列表读取也已恢复，但在线 admin 场景的站内信 7/7、移动通知 7/7 仍为 `send_status=0`，登记为 `NOTIFY-001`，不能用列表 PASS 代替通知送达 PASS。 | 先关闭 `NOTIFY-001`，再逐包处理 169 个空白页；WTS 其余 10 个基础设置表单还需分别补提交落库验收，并为 7 类空列表准备受控记录、完成详情复验；动火实际打印需安装并授权标准打印控件；活动产出列表 API 仍需恢复真实产出活动可见性；PATROL 真实测点历史、果糖 QA/QC 正式限值、现场 Batch/DCS、连续运行、业务签字、BPI 长周期影子验收及外部 ERP/WMS 演练仍未完成。 |
| G-019 | PostgreSQL 缺口进入幂等 SQL/backlog | PARTIAL | Oracle/PostgreSQL 审计、init SQL、backlog 和 watch rationale 已纳入 CI；`metadata/persistence-acceptance.json` 当前有 `340` 项，其中 `300 PASS / 1 FAIL / 0 BLOCKED / 39 NOT_APPLICABLE`。既有 WOM/QCS/WMS/ProcessAnalysis、产线、果糖试运行、BPI 与配置兼容证据继续保留；迁移 224-257 补齐 TeamInfo admin 权限、OEE 工厂树层级、WTS `oid ~~ bigint` 窄兼容、移动设备 token 表、WOM 产耗详情视图、QCS 检验申请配置大对象及来料检验物料/采样点/供应商参照兼容、QCS 其他检验/质量巡检运行时动作、WTS 基础设置与业务/统计动作布局/大对象/权限/标签/脚本入口、台账查询/导出、资质子实体和动火打印兼容、Qualify 动作布局/资质分类根节点/助记码别名及默认等级系统配置、WOM 执行记录详情/追溯/统计、退料/用料/报工/尾料及完工入库台账动作、LIMSSample 5 阶段菜单重组，以及 RM 配方管理动作、助记码表、类型根节点和配方树/质量备注大对象兼容。当前迁移账本为 257 个脚本、0 个高风险语句、90 个受控观察项、0 个观察项安全问题，且 mapper 审计错误为 0；新增迁移在 PostgreSQL 上重复执行安全且未引入 Oracle 默认路径。本轮 LIMSSample 保留 24 个原页面和 74 个有效操作，5 个代表页面真实浏览器验收 5/5 PASS；本轮 WTS 业务管理/统计分析 24 个布局完成 PostgreSQL 大对象复读，页面和只读交互 35/35 PASS；本轮 RM 9/9 菜单和 42 个动作通过预期语义检查，配方类型/工序类型 marker 完成新增、修改和软删除查库，普通配方启用完成查库；移动 token 缺表已关闭，但真实在线场景仍有 `NOTIFY-001`：站内信 7/7、移动通知 7/7 均为 `send_status=0`，已进入 1 条 `FAIL_BACKLOG`，不能用消息列表 HTTP 200 代替通知送达。 | 修复 msgmanagement 通知 provider 与在线会话链，用新 marker 完成浏览器/API/PostgreSQL 复验；Qualify 导入、资质分配和提醒待办仍需补写完整业务落库验收；WTS 其余 10 个基础设置表单继续补写操作验收，并补齐空列表详情和实际打印授权复验；后续每发现新 PostgreSQL 缺口仍需追加幂等 SQL 或 backlog |
| G-020 | 生产迁移前置项 | BLOCKED | 生产迁移 readiness、cutover 总闸门、9 轨 rehearsal、数据库行数/checksum 对账、MinIO/Keycloak/Nacos/runtime patch、回滚、license、network/TLS、安全加固和 business smoke 签字门禁均已建立；当前 cutover 为 `NOT_READY_FOR_PRODUCTION_CUTOVER`，9 个 gate 中 8 个 PLANNED、1 个 BLOCKED，生产账本为 0 个 blocker、0 个 backlog；rehearsal 为 `REHEARSAL_BLOCKED`，9 个 track 中 8 个 PLANNED、1 个 BLOCKED。 | 数据迁移/回滚演练、真实 license 决策、真实 MinIO/Keycloak 迁移、真实生产域名/TLS、安全加固、现场 Batch/DCS 投递确认和真实业务 smoke 签字均未 READY；当前门禁只能证明“不允许切生产”及账本一致性。 |
| G-021 | 智能批次与工艺数据中心（BPI） | PARTIAL | BPI 控制面、数据面、质量/WMS 内部链与 V26-V35 数据/遥测链已有真实目标证据。V31 保留不可变 `BLOCKED` 历史评估，V32-V35 已闭合工艺窗口、现场覆盖、受控 MQTT 和实时事实。Phase 3C-G revision `007616b290ef` 已把冻结窗口交付到 Parquet v2/Iceberg。2026-07-27 marker `BPI_MIN_20260727_110711` 又以真实 WOM 指令和五个持续信号闭合 MQTT → JetLinks → Kafka/Flink → START/END 候选 → 页面确认 → 同一 CLOSED_RAW 影子批次，PostgreSQL 保存六条边界证据；清理后规则退役、校准撤销、命令开关 false，QCS/WMS 写入为 0。 | Phase 3C-C/D/E/F/G 与最小单元的受控实现已关闭，但模拟器和临时校准不是物理 DEVICE/GATEWAY、正式证书、容量或真实数据资格。仍需物理来源断电重连、正式校准、多点/多产线容量、至少 200 个复核批次、7 个生产日、正负标签、连续 7-14 天、外部 ERP/WMS、整站灾备与 MLflow 生产 RBAC/SSO/TLS/HA；Phase 3D-A 对当前数据必须失败关闭，不得把 G-021 改为 READY。 |

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

1. 把已通过的 BPI 同一 WOM 指令 MQTT START/END 联合验收、目标环境 Flyway V25、正式身份完工入库冲销、自动点位目录、校准稳定分页、topology/rule
   产品化、版本比较、职责分离审批、受控退役、savepoint、单 broker 故障、应用组件双向回退和真实
   候选/批次负载下的整栈回切固化为发布回归基线。
2. JetLinks 产品/设备、`instantFlow` metadata、单位以及受控 MQTT + MES production context 同一指令 START/END candidate/batch 已闭合；下一步由现场计量人员提交正式证书，由独立管理员审核其与目录 `calibrationVersion` 精确匹配，并用物理 DEVICE/GATEWAY epoch、连续单调 sequence 和断电恢复重跑整条链，随后进入 7-14 天真实影子运行。
3. 同时保持 `ADP_E2E_20260727_MES_FULL_CLOSED_10` 完全不续跑基线和 `ADP_E2E_20260727_MES_FULL_CLOSED_11` 精确计数回归；随后用真实主数据、现场 Batch/DCS、连续运行和业务签字复跑，不扩散到低优先级模块。
4. 扩展基础配置与生产迁移 rehearsal，但在真实业务 smoke 签字前保持 `NOT_READY_FOR_PRODUCTION_MIGRATION` 和 `NOT_READY_FOR_PRODUCTION_CUTOVER`。

## 更新规则

- 任何目标项改为 `READY` 前，必须有当前仓库内 artifact 或真实运行证据说明。
- 平台、生产和基础配置功能不能只用源码推断，必须回写 `docs/frontend-functional-test-report.md`、`docs/backend-table-audit/persistence-acceptance.md` 和 `metadata/persistence-acceptance.json`。
- 生产模块完整功能未通过前，`G-013` 必须保持 `BLOCKED` 或 `FAIL`。
- 生产迁移签字未完成前，`G-020` 必须保持 `BLOCKED` 或 `PARTIAL`。
- BPI 真实 IoT/MES 接入、目标产线 7-14 天影子运行和后续写回门槛未完成前，`G-021` 必须保持 `PARTIAL` 或 `BLOCKED`。
- 总状态只有在全部目标项 `READY` 后才能改为 `COMPLETE`。
