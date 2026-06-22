# 基础配置覆盖账本

本文件是 G-012「基础配置」的覆盖边界说明。它不是源码推断，也不是
`make ci` 的替代品；每个 `PASS` 项必须能回到真实前端、HTTP 请求和
PostgreSQL 落库证据。机器可读记录见
`metadata/basic-config-coverage.json`，动作级边界见
`metadata/basic-config-action-matrix.json` / `docs/basic-config-action-matrix.md`，校验命令：

```bash
make basic-config-coverage-check
make basic-config-action-matrix-check
```

当前结论：G-012 仍是 `PARTIAL`。系统编码字典项/字典值，以及系统配置
app 目录/配置项/配置值已完成当前测试地址的 marker 落库验收；内置配置
目录已补充只读目录/详情/PostgreSQL 元数据 smoke，但编辑保存回读仍未逐项
验收；实体/模型/低代码配置运行时已补充只读 API、真实浏览器页面、PostgreSQL
元数据 readiness smoke，并已完成 customProperty 映射和实体/模型元数据 CRUD 专项验收；基础配置动作矩阵已把身份、授权、AK/SK、密码策略目录标为
`READ_ONLY_GUARDED`，把 QCS/RM/BaseSet 运行配置标为
`CONTROLLED_MARKER_REQUIRED`；Nacos/Keycloak 测试环境运行态 smoke 已纳入机器校验；实体/模型 PostgreSQL 物理表自动创建已单独登记为 `PARTIAL`；内置敏感配置写操作、其他业务运行配置和 Nacos/Keycloak 生产配置链路还不能按
整体基础配置完成来交付。

| 区块 | 状态 | 页面/入口 | 已验收动作 | 数据库表/证据 | 缺口 |
|---|---|---|---|---|---|
| 系统编码字典项/字典值 | PASS | `/systemcode/#/` | 字典项新增/编辑/删除，字典值新增/编辑/删除，删除后软删联动 | `sys_entity`、`sys_code`；`metadata/persistence-acceptance.json`、`docs/backend-table-audit/persistence-acceptance.md` | 暂无当前阻断，保持回归 |
| 系统配置 app 目录/配置项/配置值 | PASS | `/systemconfig/#/sysconfig` | app 配置目录和配置项新增、查询、详情读取、配置值更新、按模块读取、删除清理 | `systemconfig_config_catalog`、`systemconfig_config_info`、`systemconfig_config_option`、`systemconfig_config_version`；`metadata/systemconfig-persistence-acceptance.json` | 暂无当前阻断，保持回归 |
| 系统配置内置目录 | PARTIAL | `/systemconfig/#/sysconfig` | 已确认列表、详情接口和 PostgreSQL 目录元数据可读，覆盖用户目录、打印服务授权、AK/SK 凭证、密码配置、质量检验配置、RM/BaseSet 等目录；已记录敏感/业务运行配置分类、编辑策略边界，并将配置值脱敏为数量/hash 摘要；QCS `reportShowIndexRange` 已完成一次专用 marker 保存、详情/按模块回读、PostgreSQL before/after 和立即回滚；配置回滚后 WOM checkoutBill 质量活动生成 QCS 请检单、WOM createManuInspect 制造请检生成 QCS 请检单、QCS 报告保存/合格生效回写、不合格生效回写并自动生成不合格处理单均已复跑 PASS；RM.MQ `brokerUrl` 已完成一次专用 marker 保存、详情/按模块回读、PostgreSQL before/after 和立即回滚，回滚后 RM 批控配方同步/删除复跑 PASS；BaseSet `isEnable` 已完成一次 typed mutation 保存、详情/按模块回读、PostgreSQL before/after 和立即回滚，回滚后 WOM start 与 WOM/QCS 制造请检生成复跑 PASS | `metadata/systemconfig-builtins-readiness-smoke.json`、`metadata/systemconfig-controlled-runtime-config-acceptance.json`、`metadata/qcs-runtime-config-wom-checkoutbill-regression.json`、`metadata/qcs-runtime-config-wom-manu-inspect-regression.json`、`metadata/qcs-report-chain-qualified-current.json`、`metadata/qcs-report-chain-unqualified-current.json`、`metadata/systemconfig-controlled-runtime-config-rm-current.json`、`metadata/rm-runtime-config-batch-sync-regression.json`、`metadata/systemconfig-controlled-runtime-config-baseset-current.json`、`metadata/baseset-runtime-config-wom-start-regression.json`、`metadata/baseset-runtime-config-wom-manu-inspect-regression.json`、`metadata/systemconfig-persistence-acceptance.json` | 身份、授权、密钥、密码类配置禁止通用 smoke 改值；BaseSet/RM/QCS 其他运行配置、RM 敏感配置和其他内置目录写操作仍需专用 marker、回滚方案和业务回归；material 库存/入库仍归 PROD-022 |
| 实体/模型/低代码配置运行时 | PASS | configuration/entity runtime pages | 已确认 `customPropertyTree`、service manager、`ec` 工程期/模块配置 API，真实浏览器打开 4 个配置页，PostgreSQL `ec_*`、`runtime_*` 和菜单元数据可读；已通过 `/supplant/#/customFieldModelManage` 完成自定义字段模型映射启用、编辑 marker、恢复、禁用和受控清理；已通过 `/msService/ec/engine/msManage` 完成实体/模型新增、编辑、删除/禁用、内置属性清理和受控清理 | `ec_entity`、`ec_model`、`ec_property`、`ec_field`、`ec_view`、`runtime_view`、`runtime_extra_view`、`runtime_button`、`rbac_menuinfo`、`base_cp_model_mapping`、`runtime_property`、`project_property`、`base_cp_view_mapping`、`information_schema.tables`；`metadata/runtime-configuration-readiness-smoke.json`、`metadata/custom-property-persistence-acceptance.json`、`metadata/entity-model-config-crud-persistence-acceptance.json` | 暂无当前动作级阻断；注意本次实体/模型 CRUD 只证明元数据链路，报告明确未声明 PostgreSQL 自动创建物理模型表 |
| 实体/模型 PostgreSQL 物理表自动创建 | PARTIAL | `/msService/ec/engine/msManage`；areaId `configuration-physical-model-table` | 已捕获实体/模型元数据 CRUD 验收中的 `information_schema.tables` 查询和非声明事实：当前 marker 表 `DS_E2E_063301` 未由 PostgreSQL 路径自动创建 | `ec_model`、`ec_property`、`information_schema.tables`、未来 marker 物理表；`metadata/entity-model-config-crud-persistence-acceptance.json`、`metadata/basic-config-action-matrix.json` | 需要补 `ModelSyncDBUtils` PostgreSQL 物理表创建分支或产品风险接受；关闭前必须用真实前端 marker 和 PostgreSQL before/after SQL 证明物理表创建与清理 |
| Nacos/Keycloak 生产配置链路 | PARTIAL | production configuration runbooks | 测试环境 Nacos drift smoke 已拉取 `prod` group 配置并验证 PostgreSQL/JWT/服务健康关键检查；Keycloak/JWT runtime smoke 已验证 realm public key、Nacos JWT 同步、gateway 登录和 current-user 菜单 | `metadata/nacos-config-drift-smoke.json`、`metadata/keycloak-jwt-runtime-smoke.json` | 生产 Nacos export/diff、Keycloak realm 迁移、secret 轮换、生产登录 smoke 和回退演练未执行 |

## 回归命令

```bash
make acceptance-systemcode-persistence \
  ADP_BASE_URL=http://100.99.133.43:18080 \
  ADP_BROWSER_BASE_URL=http://100.99.133.43:18080 \
  SYSTEMCODE_PERSISTENCE_OUTPUT=/tmp/adp-systemcode-persistence-1009913343-current.json

make acceptance-systemconfig-persistence \
  ADP_BASE_URL=http://100.99.133.43:18080 \
  ADP_BROWSER_BASE_URL=http://100.99.133.43:18080 \
  SYSTEMCONFIG_PERSISTENCE_OUTPUT=metadata/systemconfig-persistence-acceptance.json

make smoke-systemconfig-builtins \
  ADP_BASE_URL=http://100.99.133.43:18080 \
  ADP_BROWSER_BASE_URL=http://100.99.133.43:18080 \
  SYSTEMCONFIG_BUILTINS_OUTPUT=metadata/systemconfig-builtins-readiness-smoke.json

make acceptance-systemconfig-controlled-runtime-config \
  ADP_BASE_URL=http://100.99.133.43:18080 \
  ADP_BROWSER_BASE_URL=http://100.99.133.43:18080 \
  SYSTEMCONFIG_CONTROLLED_OUTPUT=metadata/systemconfig-controlled-runtime-config-acceptance.json

make acceptance-systemconfig-controlled-runtime-config \
  ADP_BASE_URL=http://100.99.133.43:18080 \
  ADP_BROWSER_BASE_URL=http://100.99.133.43:18080 \
  SYSTEMCONFIG_CONTROLLED_TARGET_MODE=rm \
  SYSTEMCONFIG_CONTROLLED_OUTPUT=metadata/systemconfig-controlled-runtime-config-rm-current.json

make acceptance-systemconfig-controlled-runtime-config \
  ADP_BASE_URL=http://100.99.133.43:18080 \
  ADP_BROWSER_BASE_URL=http://100.99.133.43:18080 \
  SYSTEMCONFIG_CONTROLLED_TARGET_MODE=baseset \
  SYSTEMCONFIG_CONTROLLED_OUTPUT=metadata/systemconfig-controlled-runtime-config-baseset-current.json

ADP_BASE_URL=http://100.99.133.43:18080 \
  ADP_BROWSER_BASE_URL=http://222.88.185.146:18080 \
  ADP_WOM_START_PERSISTENCE_OUTPUT=metadata/baseset-runtime-config-wom-start-regression.json \
  node deploy/docker/scripts/adp-wom-start-persistence-acceptance.js

ADP_BASE_URL=http://100.99.133.43:18080 \
  ADP_BROWSER_BASE_URL=http://222.88.185.146:18080 \
  ADP_PAGE_TIMEOUT_MS=240000 \
  ADP_NAV_WAIT_UNTIL=domcontentloaded \
  ADP_WOM_MANU_INSPECT_PERSISTENCE_OUTPUT=metadata/baseset-runtime-config-wom-manu-inspect-regression.json \
  node deploy/docker/scripts/adp-wom-manu-inspect-persistence-acceptance.js

ADP_BASE_URL=http://100.99.133.43:18080 \
  ADP_E2E_MARKER=ADP_E2E_YYYYMMDD_HHMMSS_RM_CFG_REG \
  ADP_RM_BATCH_FORMULA_ID=<unique-id> \
  ADP_RM_BATCH_SYNC_PERSISTENCE_OUTPUT=metadata/rm-runtime-config-batch-sync-regression.json \
  node deploy/docker/scripts/adp-rm-batch-sync-persistence-acceptance.js

make acceptance-wom-checkoutbill-persistence \
  ADP_BASE_URL=http://100.99.133.43:18080 \
  ADP_BROWSER_BASE_URL=http://222.88.185.146:18080 \
  WOM_CHECKOUTBILL_PERSISTENCE_OUTPUT=metadata/qcs-runtime-config-wom-checkoutbill-regression.json

make acceptance-wom-manu-inspect-persistence \
  ADP_BASE_URL=http://100.99.133.43:18080 \
  ADP_BROWSER_BASE_URL=http://222.88.185.146:18080 \
  WOM_MANU_INSPECT_PERSISTENCE_OUTPUT=metadata/qcs-runtime-config-wom-manu-inspect-regression.json

make acceptance-qcs-report-chain-persistence \
  ADP_BASE_URL=http://100.99.133.43:18080 \
  ADP_BROWSER_BASE_URL=http://222.88.185.146:18080 \
  QCS_REPORT_CHAIN_MODE=qualified \
  QCS_REPORT_CHAIN_PERSISTENCE_OUTPUT=metadata/qcs-report-chain-qualified-current.json

make acceptance-qcs-report-chain-persistence \
  ADP_BASE_URL=http://100.99.133.43:18080 \
  ADP_BROWSER_BASE_URL=http://222.88.185.146:18080 \
  QCS_REPORT_CHAIN_MODE=unqualified \
  QCS_REPORT_CHAIN_PERSISTENCE_OUTPUT=metadata/qcs-report-chain-unqualified-current.json

make smoke-runtime-configuration \
  ADP_BASE_URL=http://100.99.133.43:18080 \
  ADP_BROWSER_BASE_URL=http://100.99.133.43:18080 \
  RUNTIME_CONFIG_SMOKE_OUTPUT=metadata/runtime-configuration-readiness-smoke.json

make acceptance-custom-property-persistence \
  ADP_BASE_URL=http://100.99.133.43:18080 \
  ADP_BROWSER_BASE_URL=http://100.99.133.43:18080 \
  CUSTOM_PROPERTY_ACCEPTANCE_OUTPUT=metadata/custom-property-persistence-acceptance.json \
  ADP_API_TIMEOUT_MS=90000

ADP_BASE_URL=http://100.99.133.43:18080 \
  ADP_BROWSER_BASE_URL=http://222.88.185.146:18080 \
  ADP_ENTITY_MODEL_CONFIG_CRUD_OUTPUT=metadata/entity-model-config-crud-persistence-acceptance.json \
  node deploy/docker/scripts/adp-entity-model-config-crud-persistence-acceptance.js

make smoke-nacos-config \
  ADP_SSH_HOST=100.99.133.43 \
  NACOS_CONFIG_SMOKE_OUTPUT=metadata/nacos-config-drift-smoke.json

make smoke-keycloak-jwt \
  ADP_SSH_HOST=100.99.133.43 \
  KEYCLOAK_JWT_SMOKE_OUTPUT=metadata/keycloak-jwt-runtime-smoke.json

make basic-config-coverage-check
make basic-config-action-matrix-check
```

## 判定规则

- G-012 不能因为系统编码和系统配置 app 目录通过，就被标为 `READY`。
- `metadata/basic-config-coverage.json` 中只要存在 `PARTIAL`、`BLOCKED`
  或 `NOT_RUN` 区块，G-012 必须保持非 `READY`。
- 配置类页面不能只看接口 `200`。会改变业务数据的操作必须有 marker、
  请求记录、后端入口、目标表和 PostgreSQL 查询结果。
- 系统配置内置目录 smoke 只允许做只读列表/详情/数据库元数据验证；身份目录、
  打印授权、AK/SK、密码策略等敏感配置不能由通用 smoke 修改。报告中的配置值
  只能以数量/hash 摘要形式保存。
- 实体/模型/低代码配置运行时的只读 smoke 只能证明 API、页面、菜单和元数据
  可用；customProperty 字段映射与实体/模型元数据 CRUD 分别有独立 marker 落库验收，二者互不替代；PostgreSQL 物理模型表自动创建仍需另行验收。
- Nacos/Keycloak 的测试环境 smoke 只能证明当前测试环境链路可用，不能
  代表生产配置迁移完成；生产切换仍必须按 G-020 的迁移前置项验收。
- `make basic-config-coverage-check` 会读取 Nacos/Keycloak runtime smoke 报告并校验
  `100.99.133.43`、PostgreSQL、关键服务健康、JWT hash 同步和 gateway 登录/菜单证据。
- `make basic-config-action-matrix-check` 会把基础配置动作矩阵和覆盖账本、内置目录
  readiness smoke、实体/模型 runtime readiness smoke 和实体/模型 CRUD persistence 报告交叉校验；身份目录、打印授权、
  AK/SK、密码策略不能被通用 smoke 改值，QCS/RM/BaseSet 运行配置必须先有专用 marker、
  回滚方案和业务回归。
