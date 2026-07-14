# BPI 点位目录准入与拓扑硬门禁验收

## 结论

本轮状态为 **`PASS_CONTROL_WITH_BLOCKED_SOURCE`**。点位目录、幂等导入、拓扑快照
绑定、失败关闭、审计、最小权限和服务重启后读取均已在目标环境通过真实页面、API
和 PostgreSQL 验收；目标 JetLinks 试点设备本身仍为 **`BLOCKED`**，不允许发布为
可驱动批次规则的生产点位。

- 目标机：`100.99.133.43`
- ADP 登录入口：`http://100.99.133.43:18080`
- BPI 操作台：`http://100.99.133.43:18091/#/points`
- marker：`ADP_E2E_20260715_POINTCAT_02`
- Flyway：V12，BPI 基表 21 张
- 机器记录：`metadata/bpi-point-catalog-readiness-acceptance.json`

## 源状态

验收前直接查询目标 JetLinks PostgreSQL，不由 BPI 页面猜测设备状态：

| 对象 | 实际状态 | BPI 规范化结果 |
|---|---|---|
| `bpi-pilot-product-01` | 产品存在，`state=0`，metadata 为空 | `propertyPresent=false` |
| `bpi-pilot-device-01` | `state=notActive`，`registry_time=NULL` | `deviceState=INACTIVE`、`registered=false` |
| `instantFlow` | exporter 映射到 `flow.instant`，单位 `m3/h` | 保存源属性与规范属性两套身份 |
| 标定 | `pilot-unverified-20260714` 未验证 | `calibrationStatus=UNVERIFIED` |

这组状态真实反映试点环境，不是为了制造失败而构造的 mock。

## 浏览器验收

| 页面/路由 | 操作 | API | 页面/API 结果 | 状态 |
|---|---|---|---|---|
| `/#/points` | 真实 ADP 会话打开“点位目录”并导入 JetLinks 状态快照 | `POST /bpi-api/point-catalog/snapshots` | `200`；页面显示 `0/1 就绪`、`instantFlow -> flow.instant` 和四个中文阻断原因 | PASS |
| `/#/points` | 使用同一 payload 和 Idempotency-Key 重放 | 同上 | `200`，`Idempotent-Replay: true`，返回原 snapshot ID | PASS |
| `/#/rules` | 创建绑定该点位的拓扑草稿 | `POST /bpi-api/topologies/drafts` | `200`，草稿 revision 1 | PASS |
| `/#/rules` | 校验拓扑 | `POST /bpi-api/topologies/{id}/validate` | `200`，`DRAFT/FAILED/r2`，四项 ERROR 和一项 WARNING；不显示发布按钮 | PASS |
| 服务重启后 `/#/points`、`/#/rules` | 只读复验既有 marker | GET current catalog/topologies/topology | 点位和失败拓扑仍唯一可见；无 console/page/network error | PASS |

写阶段报告为 `/tmp/bpi-point-catalog-ADP_E2E_20260715_POINTCAT_02.json`，重启后只读
报告为 `/tmp/bpi-point-catalog-ADP_E2E_20260715_POINTCAT_02-read.json`。两阶段登录均为
`200`，浏览器 console error、page error 和 request failure 均为 0。

## PostgreSQL 验收

| 业务动作 | 目标表 | 验收 SQL 摘要 | 实际结果 | 状态 |
|---|---|---|---|---|
| 导入点位快照 | `bpi_point_catalog_snapshots`、`bpi_point_catalog_entries` | 按 `source_revision` 和 snapshot ID 查询 | snapshot `14ceaa1e-...`，`point_count=1`、`ready_point_count=0`；源/规范属性为 `instantFlow -> flow.instant` | PASS |
| 幂等重放 | `bpi_api_idempotency` | 按 Idempotency-Key 查询并统计同 source revision | `COMPLETED/POST/200`，快照总数仍为 1 | PASS |
| 拓扑失败关闭 | `bpi_topology_versions` | 查询 state、validation、revision、pin 和 JSON 错误码 | `DRAFT/FAILED/r2`，固定 snapshot `14ceaa1e-...`，发布不允许 | PASS |
| 审计 | `bpi_audit_events` | 按 snapshot/topology ID 顺序查询 | 导入、草稿创建、校验失败三条动作均存在 | PASS |

拓扑错误码：

- `POINT_DEVICE_NOT_REGISTERED`
- `POINT_DEVICE_NOT_ACTIVE`
- `POINT_PROPERTY_NOT_AVAILABLE`
- `POINT_CALIBRATION_NOT_VERIFIED`

警告码为 `POINT_SOURCE_SEQUENCE_DISABLED`。应用角色 `bpi_service` 对两张点位目录表仅有
`INSERT/SELECT`，无 `UPDATE/DELETE`。

## 回归结果

- OpenAPI：42 个操作，28 个模拟，27 个服务实现，检查通过。
- 确定性模拟：6/6 PASS。
- BPI 前端：构建通过，浏览器 E2E 8/8 PASS。
- Java 8 适配器：测试通过。
- Java 17 BPI 服务：Flyway V12 的真实 PostgreSQL/Kafka 全量测试通过。

## 未闭合项

本轮只证明“未就绪点位一定不能越过发布门禁”，不证明设备已经具备现场生产条件。
下一步必须在 JetLinks 完成设备注册/激活、产品 metadata、单位、标定和来源序列治理，
导入一个更新快照并重新校验拓扑。点位变为 READY 后，才进入同 scope 的真实 IoT 事件
与 WOM context 联合 marker 和 7-14 天影子运行。
