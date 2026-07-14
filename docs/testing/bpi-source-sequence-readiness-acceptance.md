# BPI 来源序列硬准入验收

## 结论

状态：`PASS_TARGET_GATE_WITH_BLOCKED_SOURCE`

P1 批次规则点位现在必须具备设备或网关级 `sourceEpoch + sequence`。Exporter 生成的回退序列仍可用于
影子观测，但不会再把点位提升为 `READY`，也不能通过拓扑校验。

验收 marker：`ADP_E2E_20260715_0532_BPI_SOURCE_SEQUENCE`

## 真实 PostgreSQL 验收

隔离环境使用 PostgreSQL `16.13`，从空库执行 Flyway `V1-V12`。测试构造三类点位：

| 点位 | 设备/属性/单位/标定 | 来源序列 | 结果 |
|---|---|---|---|
| `DEVICE-S07-01 / flow.instant` | 全部通过 | 已启用 | `READY` |
| `DEVICE-S07-02 / tank.level` | 多项缺失 | 未启用 | `BLOCKED` |
| `DEVICE-S07-03 / flow.sequence-missing` | 全部通过 | 未启用 | `BLOCKED / SOURCE_SEQUENCE_DISABLED` |

验收测试：

```text
BpiPointCatalogPostgresAcceptanceTest
tests=1, failures=0, errors=0, skipped=0
```

关键查询与断言：

```sql
SELECT point_count || '|' || ready_point_count || '|' || checksum
FROM bpi.bpi_point_catalog_snapshots
WHERE tenant_id = :marker AND id = :snapshot_id;
-- 3|1|<snapshot checksum>

SELECT validated_point_catalog_snapshot_id::text || '|' || validated_point_catalog_checksum
FROM bpi.bpi_topology_versions
WHERE tenant_id = :marker AND id = :topology_id;
-- only the source-sequence-ready topology is validated and publishable
```

仅缺来源序列的拓扑验证结果为 `FAILED`，且唯一错误为
`POINT_SOURCE_SEQUENCE_DISABLED / ERROR`。测试同时验证两次不可变快照共 6 条 point entry、13 条幂等命令、
导入审计、旧快照发布冲突和独立管理员发布；每次测试结束后按 marker 定向清理。

## 前端验收

`npm --prefix frontend/apps/bpi run test:e2e` 共 `8/8 PASS`。点位页面把来源序列缺失显示为
“来源序列缺失”，与后端 `readinessIssues` 一致；console、page、request failure 均为 0。

## 目标环境复验

目标入口为 `http://100.99.133.43:18091/#/points`。本批只重建独立 Compose project
`ft-mes-bpi-runtime` 的 `bpi-service` 和 `bpi-web`，未停止既有 ADP/MES、Kafka、Flink、JetLinks 或
BPI PostgreSQL。服务镜像为 `sha256:1cdbbf814a20615307ab4a3adcfe38977a260865d85ef8a3f645be361670345c`，
回滚镜像为 `sha256:6b336517e2e14711519cc3d6bd786135eb1cf6de1168752c017497e4113160c6`；
旧 JAR 和前端静态目录保存在 `/home/v6/bpi-deploy-backups/20260715-source-sequence-0532`。

真实 ADP 登录后执行 `sync-read`：

- `GET /bpi-api/point-catalog/current?plantId=PLANT-01&lineId=LINE-S07-01` 返回 `200`；
- 页面读取 revision `sha256:2a218d12d6ed8bea024c38f6d2e06656f20703fadf920256dc98b17c2f151ce5`；
- 页面显示 1 个点、0 READY，来源序列“未启用”，准入状态 `BLOCKED`；
- API/页面同时包含 `DEVICE_NOT_REGISTERED`、`DEVICE_NOT_ACTIVE`、`PROPERTY_NOT_AVAILABLE`、
  `CALIBRATION_NOT_VERIFIED`、`SOURCE_SEQUENCE_DISABLED`；
- console、page、request failure 均为 0，运行时 smoke 为 PASS。

目标 PostgreSQL `15.18` 只读查询：

```sql
WITH latest AS (
  SELECT id, tenant_id, source_revision, point_count, ready_point_count
  FROM bpi.bpi_point_catalog_snapshots
  WHERE plant_id = 'PLANT-01' AND line_id = 'LINE-S07-01'
  ORDER BY observed_at DESC, imported_at DESC
  LIMIT 1
)
SELECT l.source_revision, l.point_count, l.ready_point_count,
       e.registered, e.device_state, e.property_present,
       e.calibration_status, e.source_sequence_enabled
FROM latest l
JOIN bpi.bpi_point_catalog_entries e
  ON e.tenant_id = l.tenant_id AND e.snapshot_id = l.id;
-- sha256:2a218d...151ce5 | 1 | 0 | false | INACTIVE | false | UNVERIFIED | false
```

浏览器机器证据和截图为 `/tmp/bpi-source-sequence-target-browser.json`、
`/tmp/bpi-source-sequence-target-browser.png`。目标验收脚本要求五项拓扑错误全部为 ERROR、warning 为空。

## 回归

- BPI API contract：42 operations，28 simulated，27 service implemented，PASS。
- BPI simulator：6/6 PASS。
- BPI service：41 tests，0 failures/errors；20 个依赖外部环境的套件按门禁跳过。
- BPI UI build：TypeScript/Vite PASS。
- BPI browser E2E：8/8 PASS。

## 未完成边界

本验收证明硬准入语义、页面呈现、目标部署和 PostgreSQL 落库正确，不代表目标试点设备已经提供真实来源序列。
目标环境 `bpi-pilot-device-01` 仍必须完成注册/激活、产品 metadata、单位、标定和设备/网关原生序列治理，
等待 JetLinks 自动生成新目录 revision 后再做拓扑校验。
