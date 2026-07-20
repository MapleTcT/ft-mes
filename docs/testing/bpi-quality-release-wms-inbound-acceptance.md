# BPI 质量放行与完工入库验收

## 验收结论

2026-07-20 先在隔离 PostgreSQL 16.13 中完成 Flyway V1-V23、质量门状态机、事务 outbox、回执、
幂等和影子批次阻断验收，源码提交 `22ddadebd20ed9ed5d7efd19c3c0ed49967b9c90`，
`BpiQualityReleaseWmsPostgresAcceptanceTest` 为 4/4 通过。随后提交
`94e2b2288bf52966be58e9dda97039a5455466a8` 接入质量/库存产品页并完成六态、局部 503、重试、
关闭竞态和移动端浏览器验收。

同日在唯一测试环境 `10.11.100.17` 完成受保护的 expand-only V22 -> V23 升级。历史基线
`2096a8cd6274712657f8e4ffeb5e4ce40f72dc2f` 已证明 Java 8 release 路由、真实页面/API、认证负测、
service/adapter 重启读取以及目标 PostgreSQL 4/4 合同测试通过。该阶段没有真实 WMS 单据，历史结论为
`PASS_TARGET_V23_CONTROLLED_CONTRACTS_EXTERNAL_QCS_WMS_BLOCKED`。

当前最新目标结论为
**`PASS_TARGET_CONTROLLED_QCS_EVENT_KAFKA_WMS_POSTGRES_BROWSER_CLEANED`**。实现提交
`1ce3cb996ff81556763283a5401f7c19554099c2` 已把 query-first WMS adapter、精确幂等查单、单位与
`sourceSystem=BPI` 传播、API key 门禁、迁移 192 和运行编排部署进 `adp-mes-newbase`；前端修复提交
`ad36372936d99ca947d231fc552ae9c3e086c2cc` 已修复空证据区纵向挤压。受控、认证的 QCS Protobuf
marker 通过 Kafka 驱动目标栈 `material-wms` 创建真实完工入库单、明细、库存事务和批次库存，durable
回执再把 BPI 批次推进到 `INBOUNDED/r4`。完全相同 QCS 重放及强制 Kafka offset 重放均未重复增行或
增量，真实 ADP 页面五个 API 为 200 且浏览器错误为 0；取证后两个 marker 已定向清理为零残留。

该结论仍不是生产 READY：QCS 输入来自受控内部验收事件，不是外部 QCS 实例主动发布；目标 WMS
是仓库内维护的 `material-wms` 模块，外部 ERP/WMS 的业务冲销、宕机恢复和补偿演练尚未完成。
G-021 因此继续保持 `PARTIAL`。Phase 2、HTTP ingress、Kafka consumer、WMS outbox 和 WMS adapter
最终均为关闭状态，所有 allowlist/route 均恢复 `_DENY_ALL_`。机器证据为
`metadata/bpi-quality-release-wms-target-acceptance.json`。

## 目标环境增量验收

| 验收项 | 实际结果 | 状态 |
|---|---|---|
| V23 升级 | 先备份 `ft_mes_bpi`、Compose 环境和 UI，再执行 expand-only V22 -> V23；service/adapter 使用可回退镜像，schema 不允许降级 | PASS |
| 历史页面与 API 基线 | 登录 `200`；`GET /bpi-api/batches/52427282-eb88-5645-a246-b76fe6547038/release` 为 `200`；页面显示真实 `CLOSED_RAW/r2/SHADOW` 批次及两个正确空态 | PASS |
| 路由与认证负测 | 未登录 release 请求为 `401`；嵌套 `/release/export` 被 Java 8 adapter 精确拒绝为 `403` | PASS |
| 重启读取 | 仅重建 service/adapter 后再次打开 `/bpi/#/batches`，结果一致；console/page/request/BPI HTTP error 均为 `0` | PASS |
| 目标 PostgreSQL 合同 | `BpiQualityReleaseWmsPostgresAcceptanceTest` 在目标 `ft_mes_bpi` 执行 4/4；质量 pending/accepted/rejected、幂等冲突、WMS publication 门禁、入库/拒绝/未知状态及影子批次双层阻断全部通过 | PASS_CONTROLLED |
| 受控质量放行链 | 认证 `POST /internal/bpi/v1/qcs-quality-gates` 返回 201；批次 `CLOSED_RAW/r1 -> WAIT_QA/r2 -> RELEASED/r3`，同事务只发布一个 WMS command | PASS_TARGET_CONTROLLED |
| Kafka 与内部 WMS 落库 | 3 分区/RF3/minISR2 的 command/receipt 链路消费完成；query-first adapter 在目标 `material-wms` 创建唯一 document/line/transaction/stock，保存 `12.345 kg`、`sourceSystem=BPI` | PASS_TARGET_CONTROLLED |
| durable 回执 | 唯一回执 `d40b5e28-7697-5737-a716-be66e0be4a1f` 携带真实单号 `CIN-1e855173-73e2-535e-8ec2-dc34ded29c25-WARE-E2E`，批次进入 `INBOUNDED/r4` | PASS_TARGET_CONTROLLED |
| 幂等重放 | 相同 QCS payload 再发仍为 201 且计数不变；consumer offset `1 -> 0 -> 1` 强制重放后 document/line/transaction/stock 仍各 1，库存仍为 `12.345 kg` | PASS_TARGET_CONTROLLED |
| 真实业务页面 | marker `ADP_E2E_UI_20260720_222600_BPI_WMS` 在 `/bpi/#/batches` 显示 `ACCEPTED`、`INBOUNDED`、单据、检验、命令、回执和时间线；五个 API 均 200，四类浏览器错误均为 0 | PASS_TARGET_CONTROLLED |
| 空证据布局 | 空证据行修复为 `639x58` 水平文本，本地 E2E 17/17 及目标 cache-bust 页面复验通过 | PASS |
| marker 清理和安全退场 | 历史 4 个合同 marker 及本轮 2 个全链 marker 均定向清理；material `0/0/0/0`、BPI `0/0/0/0/0`；开关全关且 allowlist/route 为 deny-all | PASS_CLEANED |
| 外部 QCS 主动事件 | 本轮为受控内部 Protobuf marker，不是外部 QCS 实例主动发布和真实检验记录所有权联接 | BLOCKED |
| 外部 ERP/WMS 补偿 | 内部 `material-wms` 已真实落单且 query-first 重放通过；外部 ERP/WMS 冲销、宕机和补偿演练未执行 | BLOCKED |

历史空态截图为 `metadata/bpi-quality-release-wms-target.png`；当前全链截图为
`metadata/bpi-quality-release-wms-live-target.png` 和
`metadata/bpi-quality-release-wms-live-target-bottom.png`。升级报告、精确测试日志和全链证据保留在目标机：

- `/home/v6/adp-mes-docker-newbase-20260611-181921/backups/bpi-v23-1187d1da/bpi-integrated-upgrade-20260720T103517Z.json`
- `/home/v6/adp-mes-docker-newbase-20260611-181921/backups/bpi-v23-1187d1da/phase2-postgres-target-test-exact-markers.log`
- `/home/v6/adp-mes-docker-newbase-20260611-181921/backups/bpi-wms-20260720t214500-1ce3cb99/target-acceptance-before-cleanup.log`
- `/home/v6/adp-mes-docker-newbase-20260611-181921/backups/bpi-wms-20260720t214500-1ce3cb99/wms-command-replay.log`
- `/home/v6/adp-mes-docker-newbase-20260611-181921/backups/bpi-wms-20260720t214500-1ce3cb99/marker-cleanup.log`

历史合同 marker 为：

- `ADP_E2E_20260720_BPI_QW_6a9a47a9fd3c48f7aa17e18e92262fbc`
- `ADP_E2E_20260720_BPI_QW_2bf472b54fcb44b7a97622f2d388bf45`
- `ADP_E2E_20260720_BPI_QW_f119ce8206674ef4b282dc144c948217`
- `ADP_E2E_20260720_BPI_QW_f7a77465fe7e42eab34d1c7dc6876e49`

本轮全链 marker 为：

- `ADP_E2E_20260720_215500_BPI_WMS`，数量 `12.345 kg`
- `ADP_E2E_UI_20260720_222600_BPI_WMS`，数量 `12.346 kg`

目标最终开关仍为 `bpi.auto-confirm=false`、`bpi.qcs-link=false`、`bpi.shadow-only=true`、
`bpi.wms-link=false`，Phase 2 HTTP/Kafka、WMS outbox/adapter 均为 false；有效内部 JWT 对 Phase 2
HTTP ingress 的探针返回 `403`，证明不是只靠页面隐藏。

## 功能结果

| 模块 | 页面/路由 | 操作 | API | 前端结果 | 后端结果 | 数据库表 | 验收状态 | 问题 |
|---|---|---|---|---|---|---|---|---|
| 批次质量 | `/bpi/#/batches` 质量区 | 接收受控 required ACCEPTED | `POST /internal/bpi/v1/qcs-quality-gates` | 真实目标页显示 `ACCEPTED` 和检验记录 | `CLOSED_RAW -> WAIT_QA -> RELEASED`，同事务生成唯一 WMS outbox | quality gate/link、batch、inbox、state、audit、outbox、WMS link | PASS_TARGET_CONTROLLED_CLEANED | 不是外部 QCS 主动事件 |
| 批次质量 | 同上 | 同 payload 重放 | 同上 | 页面投影不重复 | HTTP 201，状态与所有计数不变 | inbox + projections | PASS_TARGET_CONTROLLED_CLEANED | 无 |
| 批次质量 | 同上 | required final REJECTED | QCS Protobuf bridge | 确定性页面验收显示不合格且不显示已入库 | `REJECTED`，WMS outbox 为 0 | quality gate/link、batch、audit | PASS | 该异常分支为合同测试，不是本轮目标全链 marker |
| 完工入库 | `/bpi/#/batches` 库存区 | query-first、创建、精确复查和 durable receipt | Kafka + `bpi-wms-adapter` + material REST | 真实目标页显示单据、命令、回执和 `INBOUNDED` | 目标 `material-wms` 写入唯一单据/明细/事务/库存，receipt 使 BPI 进入 `INBOUNDED/r4` | BPI WMS link/batch/state/audit/inbox；material document/line/transaction/stock | PASS_TARGET_CONTROLLED_CLEANED | 外部 ERP/WMS 未联调 |
| 完工入库 | 同上 | 强制 Kafka command 重放 | command topic + query-first adapter | 页面单据号不变 | offset `1 -> 0 -> 1` 后四类 material 行仍各 1、数量不变 | material document/line/transaction/stock | PASS_TARGET_CONTROLLED_CLEANED | 无 |
| 完工入库 | 同上 | PUBLISHED 前回执、未知状态及合法拒绝 | WMS Protobuf bridge | 页面不按 HTTP 成功推断入库 | 409/422 均 fail-closed；合法拒绝保持 `RELEASED` 并标记 `FAILED` | WMS link、batch、state、audit、inbox | PASS | 异常分支为合同测试 |
| 安全门禁 | 无 | 影子批次尝试发 WMS | service + DB trigger | NOT_APPLICABLE | Java 与 PostgreSQL 双层拒绝 | outbox 0 | PASS | 无 |
| 查询 | `/bpi/#/batches` | 查询批次、详情、release、evidence、timeline | 五个 GET API | 全部 200；`ACCEPTED/INBOUNDED`、单据和时间线可见；空证据文本水平显示；浏览器错误 0 | Java 8 adapter/Java 17 service 返回目标 PostgreSQL projection | quality gate/link、WMS link、batch | PASS_TARGET_CONTROLLED_CLEANED | marker 取证后已清理 |
| 外部联合链 | 目标 ADP 页面 | 外部 QCS 主动请检、外部 ERP/WMS 冲销与宕机恢复 | 外部 adapters/API | 未执行 | 未激活 | 外部系统业务单据 | BLOCKED | 缺外部系统联调窗口和补偿演练 |

## 自动化命令

```bash
JAVA_HOME=/usr/local/Cellar/openjdk/17.0.1/libexec/openjdk.jdk/Contents/Home \
BPI_TEST_DATABASE_URL=jdbc:postgresql://localhost:55439/ft_mes_bpi_v23_quality_wms_final \
BPI_TEST_DATABASE_USER=postgres \
BPI_TEST_DATABASE_PASSWORD='<local-test-secret>' \
/Users/zhangchu/.m2/wrapper/dists/apache-maven-3.9.3-bin/6actqn1ngkbj8g7k704ro02jj7/apache-maven-3.9.3/bin/mvn \
  -q -pl app -am \
  -Dtest=BpiQualityReleaseWmsPostgresAcceptanceTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

结果：Flyway V1-V23 从空库成功；测试 `4/4`，失败 `0`、错误 `0`、跳过 `0`。

## 激活门槛

1. 外部 QCS/WMS adapter 必须只通过版本化 Protobuf/REST 合同交互，禁止直连对方内部表；当前内部
   `material-wms` adapter 已通过该边界，不能据此外推外部系统 READY。
2. 先开 `BPI_PHASE2_INTEGRATION_ENABLED` 和精确 tenant/plant/line allowlist，再按作用域启用
   `bpi.qcs-link`；真实回放通过后才允许启用 `bpi.wms-link`。
3. `BPI_WMS_OUTBOX_ENABLED` 只能在 broker/topic/ACL/consumer/精确查单能力全部就绪后启用；query-first
   重放已通过，但生产外部端点的超时和补偿仍需演练。
4. 目标 marker 必须证明页面动作、HTTP/Kafka、controller/service/repository、SQL、目标 WMS 单据号一致。
5. 已完成重复 QCS、PUBLISHED 前回执、拒绝/未知状态和 broker command 重放；激活前仍必须完成外部
   WMS 超时后查单、业务冲销/补偿及生产等价服务重启演练。
6. 任一异常时先关闭集成开关，保留 outbox/inbox/audit，不删除未决事实或伪造成功回执。
