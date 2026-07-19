# BPI 试点平台前置条件与单位别名验收

## 结论

2026-07-19，目标测试环境 `10.11.100.17` 完成
`JetLinks API -> JetLinks PostgreSQL -> Kafka 自动目录 -> MES PostgreSQL -> ADP 浏览器/拓扑门禁`
验收，状态为 `PASS_PLATFORM_PREREQUISITES_WITH_BLOCKED_FIELD_EVIDENCE`。

JetLinks 产品、设备、`instantFlow` metadata 和标准单位已补齐。MES 最新目录从“平台注册缺失”收敛到
只剩“标定未验证、来源序列未启用”两个真实现场阻断项。点位仍为 1 点/0 READY，未开启自动批次。

## JetLinks 平台准入

`MapleTcT/iot` 新增
`docker/bpi-shadow-pilot/scripts/reconcile-pilot-prerequisites.py`：默认只读诊断，`--apply` 只在隔离试点
通过 JetLinks 官方 API 安装协议、创建/激活受审产品和设备。它拒绝覆盖不同 metadata、协议或产品归属，
不直接写 JetLinks 表，不输出 MQTT 密钥，也不修改校准和来源序列声明。

最终目标状态：

| 对象 | 状态 |
|---|---|
| 官方协议 | 已部署，MQTT transport |
| `bpi-pilot-product-01` | 激活，metadata 含 `instantFlow: double` |
| `bpi-pilot-device-01` | 已注册激活，当前 `offline` |
| 映射单位 | `m3/h` 解析为 JetLinks `cubicMeterPerHour` / `m³/h` |
| 幂等复跑 | 0 个动作，`PASS_PLATFORM_PREREQUISITES` |
| READY 声明 | `catalogReadyClaimed=false` |

目标报告：

`/data/docker/ft-mes-iot-pilot-c077279f/docker/bpi-shadow-pilot/evidence/pilot-prerequisites-after.json`

## 自动目录落库

JetLinks 自动生成 revision：

`sha256:8b4fd91c2b2169b693a7035f9b0a9b3a23d44ab4ac625016cf904486e3148022`

MES PostgreSQL 查询：

```sql
select s.id, s.source_revision, s.point_count, s.ready_point_count,
       e.product_id, e.device_id, e.property_id, e.unit,
       e.registered, e.property_present, e.device_state,
       e.calibration_status, e.source_sequence_enabled
from bpi.bpi_point_catalog_snapshots s
join bpi.bpi_point_catalog_entries e on e.snapshot_id = s.id
where s.id = 'ca213975-4b22-4230-8cd9-968b0d1ce61a';
```

结果为：`registered=true`、`property_present=true`、`device_state=ACTIVE`、`unit=m³/h`，但
`calibration_status=UNVERIFIED`、`source_sequence_enabled=false`，因此 `ready_point_count=0`。

## 单位别名修复

JetLinks 使用 Unicode 上标 `m³/h`，MES 拓扑和现场映射使用 ASCII `m3/h`。原服务直接比较字符串，
会把同一工程单位误报为 `POINT_UNIT_MISMATCH`。本次增加 `UnitSymbolNormalizer`，执行 NFKC 规范化、
空白去除和大小写归一后再比较；数值和量纲没有被转换，非等价单位仍保持失败关闭。

本地验证：

| 检查 | 结果 |
|---|---|
| `UnitSymbolNormalizerTest` | 2/2 PASS |
| BPI reactor | contracts 17、runtime 9、service 55；0 failure/error，27 个需显式环境的集成项跳过 |
| 新 PostgreSQL 16 + Flyway V1-V16 | `BpiPointCatalogPostgresAcceptanceTest` 2/2 PASS |
| 浏览器脚本语法 | PASS |

PostgreSQL 验收特意让目录保存真实 `m³/h`，拓扑继续绑定 `m3/h`，避免测试与生产同时使用同一错误形式。

## 目标部署与真实页面

只重建 `bpi-service`：

| 项目 | 值 |
|---|---|
| 镜像 | `ft-mes-bpi-service:20260719-unit-alias` |
| image ID | `sha256:ae6facb7931fee84bd8186fd2a315fbe24cbb92b745c3e05ee8e31b1db581e92` |
| JAR SHA-256 | `7ea328f3862cd82327c3ea5278f7fbbf49d8a3dc6376d5ab168d49b6f4cbacf0` |
| 容器 | `f18f85cc133c7f835cd0b38ef48868a517f36bb201af0ed9007fab64ab9a8cc2` |
| 健康 | `healthy` |
| 回滚镜像 | `ft-mes-bpi-service:20260719-repeat-observation` |

真实浏览器 marker `ADP_BPI_UNIT_ALIAS_20260719_1325` 使用 `sync-validate`：

1. 登录 ADP 返回 `200`，访问 `http://10.11.100.17:18080/bpi/#/points`；
2. 读取自动快照，页面显示 1 点/0 READY 和两项阻断；
3. 通过真实页面创建 `ADP_BPI_UNIT_ALIAS_20260719_1325_BLOCKED_TOPOLOGY`；
4. 绑定期望单位 `m3/h`，校验读取快照实际单位 `m³/h`；
5. API 精确返回 `POINT_CALIBRATION_NOT_VERIFIED`、`POINT_SOURCE_SEQUENCE_DISABLED`，
   未返回 `POINT_UNIT_MISMATCH`；
6. 页面保持 DRAFT/FAILED，发布按钮不可用；console/page/request failure 均为 0。

浏览器报告：`/tmp/ADP_BPI_UNIT_ALIAS_20260719_1325-point-unit-alias.json`。
截图：`/tmp/ADP_BPI_UNIT_ALIAS_20260719_1325-point-unit-alias.png`。两者不提交源码仓库。

## marker 清理

验收后在一个事务中按拓扑 code/ID 定向删除：

- `bpi_audit_events` 2 行；
- `bpi_api_idempotency` 2 行；
- `bpi_topology_versions` 1 行。

复核结果为 `remainingTopology=0`，自动快照
`ca213975-4b22-4230-8cd9-968b0d1ce61a` 仍存在。没有删除或改写 JetLinks 产品、设备、点位目录和历史业务数据。

## 剩余门槛

G-021 继续保持 `PARTIAL`：

1. 校准版本 `pilot-unverified-20260714` 尚未由现场审核；
2. `requireSourceSequence=false`，且没有连续单调 DEVICE/GATEWAY epoch + sequence 真实证据；
3. 同 scope 的真实 IoT 遥测、MES production context、candidate、人工确认和 batch 尚未闭合；
4. 7-14 天影子运行、边界认同率、累计量偏差、QCS/WMS 写回和训练数据阶段尚未完成。

机器可读结果见
[`metadata/bpi-pilot-platform-prerequisites-acceptance.json`](../../metadata/bpi-pilot-platform-prerequisites-acceptance.json)。
