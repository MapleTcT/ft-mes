# 基础配置动作矩阵

本文件补充 G-012「基础配置」的动作级边界。它把已经通过的 marker
落库动作、只能只读观察的高风险配置、以及后续需要单独 marker/回滚方案的配置动作拆开，避免把
readiness smoke 误判成完整可写验收。

机器可读记录见 `metadata/basic-config-action-matrix.json`，校验命令：

```bash
make basic-config-action-matrix-check
make entity-model-config-crud-readiness-check
```

当前结论：基础配置动作矩阵仍是 `PARTIAL`。系统编码、普通 app 系统配置、低代码自定义字段模型映射、实体/模型元数据新增编辑删除已完成写入验收；身份、授权、凭证和密码策略目录只能由只读 smoke 观察；QCS/RM/BaseSet
运行配置后续新改动仍必须另做专用 marker、before/after SQL、回滚和业务回归；实体/模型 PostgreSQL 物理表自动创建与 Nacos/Keycloak 生产迁移仍是计划项。

| 动作 | 状态 | 写入策略 | 证据 | 完成前还需要 |
|---|---|---|---|---|
| 系统编码字典项/字典值 CRUD | PASS | marker-allowed | `metadata/persistence-acceptance.json`、`deploy/docker/scripts/adp-systemcode-persistence-acceptance.js` | 保持回归 |
| 系统配置 app 目录/配置项/配置值 CRUD | PASS | marker-allowed | `metadata/systemconfig-persistence-acceptance.json`、`deploy/docker/scripts/adp-systemconfig-persistence-acceptance.js` | 保持回归 |
| 用户目录 | READ_ONLY_GUARDED | forbidden-in-generic-smoke | `metadata/systemconfig-builtins-readiness-smoke.json` | 安全确认、专用 marker、回滚方案和登录 smoke |
| 打印服务授权 | READ_ONLY_GUARDED | forbidden-in-generic-smoke | `metadata/systemconfig-builtins-readiness-smoke.json` | license/授权确认、专用 marker、回滚方案和打印服务 smoke |
| AK/SK 凭证管理 | READ_ONLY_GUARDED | forbidden-in-generic-smoke | `metadata/systemconfig-builtins-readiness-smoke.json` | 安全确认、secret 轮换方案、专用 marker 和回滚方案 |
| 密码配置 | READ_ONLY_GUARDED | forbidden-in-generic-smoke | `metadata/systemconfig-builtins-readiness-smoke.json` | 安全确认、专用 marker、回滚方案和 admin 登录 smoke |
| 质量检验配置 | CONTROLLED_MARKER_REQUIRED | dedicated-business-marker-only | `metadata/systemconfig-builtins-readiness-smoke.json`、`metadata/systemconfig-controlled-runtime-config-acceptance.json`、`metadata/qcs-runtime-config-wom-checkoutbill-regression.json`、`metadata/qcs-runtime-config-wom-manu-inspect-regression.json`、`metadata/qcs-report-chain-qualified-current.json`、`metadata/qcs-report-chain-unqualified-current.json`、`metadata/production-module-test-cases.json` | 单项 QCS marker 保存/回读/回滚已完成；配置回滚后 WOM checkoutBill 质量活动、createManuInspect 制造请检、QCS 报告保存/合格生效回写、不合格生效回写并自动生成不合格处理单均已复跑 PASS；后续任何 QCS 运行配置写操作仍必须专用 marker、回滚和业务回归 |
| RM.ocd.RM | CONTROLLED_MARKER_REQUIRED | dedicated-business-marker-only | `metadata/systemconfig-builtins-readiness-smoke.json`、`metadata/systemconfig-controlled-runtime-config-rm-current.json`、`metadata/rm-runtime-config-batch-sync-regression.json`、`metadata/production-module-test-cases.json` | RM.MQ `brokerUrl` 单项 marker 保存/回读/回滚已完成；回滚后 RM 批控配方同步/删除复跑 PASS；外部 Batch 客户端编辑入口仍单独 BLOCKED |
| BaseSet.ocd.BaseSet | CONTROLLED_MARKER_REQUIRED | dedicated-business-marker-only | `metadata/systemconfig-builtins-readiness-smoke.json`、`metadata/systemconfig-controlled-runtime-config-baseset-current.json`、`metadata/baseset-runtime-config-wom-start-regression.json`、`metadata/baseset-runtime-config-wom-manu-inspect-regression.json`、`metadata/production-module-test-cases.json` | `isEnable` 已完成受控 typed mutation、回读、PostgreSQL before/after 和立即回滚；回滚后 WOM start 与 WOM/QCS 制造请检生成均复跑 PASS。后续其他 BaseSet 配置和 material 库存/入库边界仍需专用 marker。 |
| 低代码自定义字段模型映射启用/编辑/禁用 | PASS | marker-allowed | `metadata/custom-property-persistence-acceptance.json`、`deploy/docker/scripts/adp-custom-property-persistence-acceptance.js`、`deploy/docker/postgres/init/169-custom-property-project-property-compat.sql` | 保持回归；该项覆盖 customProperty 模型映射与 `runtime_property`/`project_property` 同步，实体/模型元数据 CRUD 由独立报告验收 |
| 实体/模型配置新增 | PASS | dedicated-marker-required | `metadata/entity-model-config-crud-persistence-acceptance.json`、`deploy/docker/scripts/adp-entity-model-config-crud-persistence-acceptance.js`、`metadata/runtime-configuration-readiness-smoke.json` | marker `ADP_E2E_202606221433_ENTITY_MODEL` 已证明 `ec_entity=1`、`ec_model=1`、内置 `ec_property=3`；PostgreSQL 物理模型表自动创建未在本报告中声明完成 |
| 实体/模型配置编辑 | PASS | dedicated-marker-required | `metadata/entity-model-config-crud-persistence-acceptance.json`、`deploy/docker/scripts/adp-entity-model-config-crud-persistence-acceptance.js`、`metadata/runtime-configuration-readiness-smoke.json` | 6 个写请求均 `HTTP 200/success=true`，PostgreSQL 证明 entity/model 描述中的 marker 已更新 |
| 实体/模型配置删除/禁用 | PASS | dedicated-marker-required | `metadata/entity-model-config-crud-persistence-acceptance.json`、`deploy/docker/scripts/adp-entity-model-config-crud-persistence-acceptance.js`、`metadata/entity-model-config-crud-readiness-probe.json` | model/entity `ordinaryDelete` 均 `HTTP 200/success=true`，PostgreSQL 证明 entity/model/property 软删并受控清理为 0 |
| 实体/模型 PostgreSQL 物理表自动创建 | PLANNED | dedicated-marker-required | `metadata/entity-model-config-crud-persistence-acceptance.json`、`metadata/basic-config-coverage.json` | 当前报告只证明元数据 CRUD；必须补 `ModelSyncDBUtils` PostgreSQL 物理表创建路径或正式风险接受，并用 `information_schema.tables` before/after 验收 |
| Nacos 生产配置 export/diff | PLANNED | production-evidence-required | `metadata/nacos-config-drift-smoke.json`、`deploy/nacos/production-migration/nacos-runtime-patch-evidence.example.json` | 生产 export/diff、漂移审阅、签名 patch、回退演练 |
| Keycloak 生产 realm 迁移 | PLANNED | production-evidence-required | `metadata/keycloak-jwt-runtime-smoke.json`、`deploy/keycloak/production-migration/keycloak-migration-evidence.example.json` | realm inventory、数据库恢复演练、secret 轮换、JWT/Nacos 同步、登录 smoke、回滚 |

## 实体/模型动作验收

实体、模型、字段、视图和 runtime 元数据会影响低代码页面渲染、菜单入口和按钮权限，不能把只读 readiness 当成写入验收。2026-06-22 已新增 `metadata/entity-model-config-crud-persistence-acceptance.json`，
把实体/模型新增、编辑、删除/禁用从 `PLANNED` 升级为 `PASS`，并由 `make basic-config-action-matrix-check` 强制校验。

本次验收使用 marker `ADP_E2E_202606221433_ENTITY_MODEL`，在真实浏览器页面上下文打开
`/msService/ec/engine/msManage`，记录 `entity/save`、`model/save`、`model/ordinaryDelete`、
`entity/ordinaryDelete` 六个写请求，全部返回 `HTTP 200/success=true`。后端链路追踪到
`EntityController`、`EntityServiceImpl`、`ModelController`、`ModelServiceImpl`、`DtoUtils`、
`entityDao`、`modelDao` 和 `propertyDao`；PostgreSQL before/after SQL 证明 `ec_entity`、
`ec_model`、内置 `ec_property` 新增、编辑 marker、软删除和受控清理均成立。

需要保留的技术事实：当前 PostgreSQL 路径没有在该报告中自动创建业务物理表
`DS_E2E_063301`。报告明确记录 `ModelSyncDBUtils` 现有自动建表分支仍以 Oracle/SQLServer/MySQL/MariaDB
为主；如果产品要求 PostgreSQL 下低代码模型自动生成物理业务表，需要另立修复和验收项，不能用这份元数据 CRUD 验收冒充完成。
该缺口已经登记为动作 `entity-model-postgres-physical-table-autocreate`，状态固定为 `PLANNED`。关闭它前必须有真实前端 marker、后端 `ModelSyncDBUtils` 或替代路径追踪、`information_schema.tables` 物理表 before/after SQL、清理/回滚证据，以及 `make basic-config-action-matrix-check` 和 `make basic-config-coverage-check` 通过。

2026-06-22 新增 `make smoke-entity-model-config-crud-readiness` 和
`make entity-model-config-crud-readiness-check`。该 probe 会登录测试环境，打开
`/msService/ec/engine/msManage`、`/msService/ec/module/msManage`、现有实体编辑页和现有模型管理/编辑页，调用
`/msService/ec/entity/list`、`/msService/ec/model/list`、`/msService/ec/model/formatTableName`
等只读或派生接口，并查询 PostgreSQL 中 `ec_entity`、`ec_model`、`ec_field`、`ec_view`、
`runtime_view`、`runtime_extra_view`、`runtime_button` 的当前元数据。证据写入
`metadata/entity-model-config-crud-readiness-probe.json`，状态固定为 `READINESS_ONLY` /
`NOT_VERIFIED`：它只证明实体/模型配置入口、只读 API 和元数据表可访问，不证明新增、编辑、删除/禁用已经真实落库。
当前探测还把 legacy 模板页直开时的 `YAHOO/CUI/jQuery/foundation is not defined`
归类为 `LEGACY_TEMPLATE_GLOBALS_WHEN_OPENED_DIRECTLY` warning；这些 warning 会留在报告里，不能据此宣称实体/模型编辑页面完全无前端噪声。

## 低代码字段映射验收

2026-06-22 已补一条低代码 customProperty 模型映射专项证据：`make acceptance-custom-property-persistence`
在真实 `/supplant/#/customFieldModelManage` 页面上下文中选择
`TeamInfo_1.0.0_schedual_Schedule_bigintparama`，执行启用映射、编辑
`ADP_E2E_*_CUSTOM_PROPERTY` 显示元数据、PostgreSQL before/after 查询、恢复原显示元数据、禁用映射和受控清理。证据见
`metadata/custom-property-persistence-acceptance.json`。本次还补充
`deploy/docker/postgres/init/169-custom-property-project-property-compat.sql`，用 PostgreSQL view
暴露 legacy `project_property` 表名到 `runtime_property`，修复 customProperty 服务中
`PropertyProjectMapper` 对该 legacy 表名的访问。

该项只能证明 `base_cp_model_mapping`、`runtime_property`、`project_property`
和可选 `base_cp_view_mapping` 的自定义字段映射链路可写可回滚；实体/模型元数据 CRUD
由 `metadata/entity-model-config-crud-persistence-acceptance.json` 单独验收，二者不能互相替代。

## 业务运行配置变更契约

质量检验配置、`RM.ocd.RM`、`BaseSet.ocd.BaseSet` 会影响已经验收通过的生产主流程，不能用普通系统配置 smoke 直接改值。`metadata/basic-config-action-matrix.json`
已经为这三个 `CONTROLLED_MARKER_REQUIRED` 动作记录 `acceptanceContract`，并由 `make basic-config-action-matrix-check` 交叉读取 `metadata/production-module-test-cases.json`：

- QCS 运行配置变更前必须准备专用 `ADP_E2E` marker、回滚方案和 QCS 报告链路回归，并要求 `PROD-018/031/032/033/034/035/036/037/038/039/040/041/042` 当前保持 `PASS`；`PROD-019` 仍作为 material/坏品数量边界 blocker 单独跟踪。
- RM 运行配置变更前必须准备专用 marker、回滚方案和 RM 配方回归，并要求 `PROD-008/009/043` 当前保持 `PASS`；`PROD-010` 外部 Batch 客户端编辑入口仍单独阻断。
- BaseSet 运行配置变更前必须准备专用 marker、回滚方案和 WOM/QCS 回归，并要求 `PROD-001/002/003/004/031/035/036` 当前保持 `PASS`；`PROD-022` material 库存/入库边界仍单独阻断。

这三个动作从 `CONTROLLED_MARKER_REQUIRED` 升级前，必须记录前端请求、`systemconfig_config_info` / `systemconfig_config_version` 的 PostgreSQL before/after SQL、回滚后的查询结果，以及对应生产用例的回归结果。已经补过单配置 marker 的目录仍保持受控状态，因为后续任何新配置项、敏感配置或外部客户端入口都必须另有专用 marker 和业务回归。

2026-06-21 已补一条 QCS 受控配置专项证据：`make acceptance-systemconfig-controlled-runtime-config`
在真实 `/systemconfig/#/sysconfig` 页面上下文中把 `QCS.reportShowIndexRange`
从 `qualityStd` 临时改为 `ADP_E2E_*_SCFG_QCS_RUNTIME`，通过详情 API、按模块读取 API 和 PostgreSQL
`systemconfig_config_info` 查到 marker，然后立即回滚为 `qualityStd` 并再次查库确认。证据见
`metadata/systemconfig-controlled-runtime-config-acceptance.json`。该证据只关闭“单配置保存/回读/回滚”
子项，不替代 QCS 请检、报告保存、合格/不合格处置等生产链路回归，所以状态仍保持
`CONTROLLED_MARKER_REQUIRED`。

同日补充配置回滚后的 WOM/QCS 质量活动回归：`make acceptance-wom-checkoutbill-persistence`
先暴露 `EC_MODULE.code LIKE 'QCS_%'` 在 PostgreSQL 下的类型兼容问题，已由
`166-postgres-legacy-bigint-text-like-operator.sql` 和
`167-postgres-legacy-varchar-text-like-operator.sql` 修复。修复后 marker
`ADP_E2E_20260621055313_WOM_CHECKOUTBILL` 通过公网浏览器入口打开
`makeTaskBatchView`，调用 `checkoutBill/generate/{activeId}` 返回
`HTTP 200/code=200/操作成功`，PostgreSQL 确认 `wom_task_actives`、`wom_wait_put_records`、
`wom_proc_reports`、`wom_acti_exelogs`、`qcs_inspects`、`qcs_inspect_stds`、
`qcs_inspect_coms` 和 `baseset_batch_infos` 均写入预期状态。证据见
`metadata/qcs-runtime-config-wom-checkoutbill-regression.json`。这仍只是 QCS 生产链路中的质量活动生成检验单回归，
不能替代制造请检、报告结果保存、合格/不合格处置的整链复跑。

同日继续补充配置回滚后的 WOM 制造请检回归：`make acceptance-wom-manu-inspect-persistence`
用 marker `ADP_E2E_20260621061644_WOM_MANU_INSPECT` 打开 `makeTaskList`，
刷新 `makeTaskList-pending`，通过同页面上下文选择 marker 工单并调用
`createManuInspect`。接口返回 `HTTP 200/code=200/data.success=true/操作成功`，PostgreSQL
确认 `wom_produce_tasks`、`wom_wait_put_records`、`wom_produce_task_exelog`、
`baseset_batch_infos`、`qcs_inspects`、`qcs_inspect_stds` 和 `qcs_inspect_coms`
均写入预期状态。证据见 `metadata/qcs-runtime-config-wom-manu-inspect-regression.json`。
该项只关闭“制造请检生成 QCS 请检单”回归。

同日继续补充配置回滚后的 QCS 报告链路回归：`make acceptance-qcs-report-chain-persistence`
分别以 `QCS_REPORT_CHAIN_MODE=qualified` 和 `QCS_REPORT_CHAIN_MODE=unqualified`
在当前测试机执行。合格 marker `ADP_E2E_20260621065620_QCS_REPORT_QUAL`
先通过 WOM 前端创建制造请检，再推进 QCS 请检、报告保存和两段生效，PostgreSQL
确认 `qcs_inspect_reports.status=99/check_result=合格`，WOM 任务/待入库/执行日志回写
`已检/合格`，批次回写 `BaseSet_checkResult/qualified`。不合格 marker
`ADP_E2E_20260621070426_QCS_UNQLF` 同路径确认报告 `status=99/check_result=不合格`，
WOM 与批次回写不合格，并自动生成 `qcs_un_qlf_deals.id=757556916282624`。证据见
`metadata/qcs-report-chain-qualified-current.json` 和
`metadata/qcs-report-chain-unqualified-current.json`。

同日补充 RM 受控运行配置专项证据：`make acceptance-systemconfig-controlled-runtime-config
SYSTEMCONFIG_CONTROLLED_TARGET_MODE=rm` 在真实 `/systemconfig/#/sysconfig`
页面上下文中把 `RM.MQ.brokerUrl` 从 `localhost` 临时改为
`ADP_E2E_20260621072600_SCFG_RM_RUNTIME`，通过详情 API、按模块读取 API 和
PostgreSQL `systemconfig_config_info` 查到 marker，然后立即回滚为 `localhost`。
证据见 `metadata/systemconfig-controlled-runtime-config-rm-current.json`。回滚后用
marker `ADP_E2E_20260621073500_RM_CFG_REG` 复跑 RM 批控配方
`batch/sync` 和 `batch/delete`，PostgreSQL 确认 `rm_formulas.id=757563696166144`
先写入 `valid=true/status=88`，删除后变为 `valid=false/status=0`，有效
`rm_process_actives` 计数为 `0`。证据见
`metadata/rm-runtime-config-batch-sync-regression.json`。本项不修改
RM username/password 等敏感配置，也不替代 `PROD-010` 外部 Batch 客户端显式编辑入口。

2026-06-22 补充 BaseSet 受控运行配置专项证据：`make acceptance-systemconfig-controlled-runtime-config
SYSTEMCONFIG_CONTROLLED_TARGET_MODE=baseset` 在真实 `/systemconfig/#/sysconfig`
页面上下文中把 `BaseSet.isEnable` 从 `true` 临时切换为 `false`，通过详情 API、
按模块读取 API 和 PostgreSQL `systemconfig_config_info` 查到 typed mutation，
然后立即回滚为 `true`。证据见
`metadata/systemconfig-controlled-runtime-config-baseset-current.json`。回滚后用
marker `ADP_E2E_20260621180558_WOMSTART` 复跑 WOM 制造指令单开始，
PostgreSQL 确认 `wom_produce_tasks`、`wom_wait_put_records`、
`wom_proc_reports` 和 `wom_produce_task_exelog` 写入预期状态；证据见
`metadata/baseset-runtime-config-wom-start-regression.json`。随后用 marker
`ADP_E2E_20260621180914_WOM_MANU_INSPECT` 复跑 WOM 制造请检/QCS 检验单生成，
`createManuInspect` 返回 `HTTP 200/code=200/success=true`，PostgreSQL 确认
WOM 任务/待办/执行日志、`baseset_batch_infos` 待检状态以及
`qcs_inspects/qcs_inspect_stds/qcs_inspect_coms` 均写入预期状态；证据见
`metadata/baseset-runtime-config-wom-manu-inspect-regression.json`。本项仍不关闭
`PROD-022` material 库存/入库阻断，也不代表后续 BaseSet 其他配置可跳过专用
marker、回滚和业务回归。

## 安全规则

- 通用 smoke 不允许修改身份目录、打印授权、AK/SK、密码策略。
- 所有会改业务运行配置的动作必须使用 `ADP_E2E_*` marker，并记录前端操作、API、后端入口、目标表和 PostgreSQL 查询。
- 内置目录证据只能保存值的数量或 hash 摘要，不能提交明文密钥、token 或授权串。
- 实体/模型/低代码配置的只读 readiness 只能证明页面/API/元数据可读；实体/模型元数据 CRUD 已有独立 marker 落库验收，但 PostgreSQL 物理模型表自动创建仍不能被默认视为完成。
- Nacos/Keycloak 测试环境 smoke 不能替代生产 export/diff、realm 迁移、secret 轮换和回退演练。
