# BPI 外部 WMS 协议软件一致性验收

## 结论

2026-07-21 在本地受控 Java 17 / Maven 3.9.3 环境执行：

```bash
JAVA_HOME=/usr/local/Cellar/openjdk/17.0.1/libexec/openjdk.jdk/Contents/Home \
MVN=/Users/zhangchu/.m2/wrapper/dists/apache-maven-3.9.3-bin/6actqn1ngkbj8g7k704ro02jj7/apache-maven-3.9.3/bin/mvn \
make bpi-wms-adapter-test
```

`bpi-wms-adapter` 共 `16/16 PASS`，其中 `WmsCommandProcessorTest` 为 `10/10`，
`MaterialWmsHttpClientTest` 为 `5/5`，应用上下文为 `1/1`。本轮修复了一个明确的
响应不确定窗口：外部 WMS 已提交创建、但 HTTP 响应丢失时，adapter 会在同一次 Kafka
投递中按原幂等键精确查单；只有查到业务事实完全一致的耐久单据才发送 accepted receipt。

状态为 `PASS_SOFTWARE_PROTOCOL_ONLY`。内部 `material-wms` 的追加式红字单持久化合同随后已通过
本地测试，但 BPI 冲销命令、补偿审批和外部 ERP/WMS 仍未接通；这不是外部实例联调，也不代表
生产开关已经完成，`G-021` 继续保持 `PARTIAL`。

## 验收矩阵

| 场景 | 输入/故障 | 预期协议行为 | 实际结果 | 状态 |
|---|---|---|---|---|
| 原单查到 | 幂等键第一次查询即命中 | 不再创建，核对完整业务事实后发 accepted receipt | 未调用 create，返回 durable document | PASS |
| 正常创建 | 第一次查无，创建成功 | 创建后必须再次精确查到耐久单据才发 accepted receipt | 两次 query、一次 create，事实一致 | PASS |
| 响应丢失且已落单 | create 抛 transient，二次 query 命中 | 同次恢复，不重复创建，不发 rejected receipt | 返回 accepted，保留原命令身份 | PASS |
| 响应丢失且未查到 | create 抛 transient，二次 query 为空 | 保持 transient，交给 Kafka 重试 | 未发任何业务回执 | PASS |
| 响应丢失且查单失败 | create 与恢复 query 均 transient | 抛出原不确定异常并附加查单异常 | 未误判为业务拒绝 | PASS |
| 业务拒绝 | 外部返回 4xx/业务 code | 二次查原单；仍不存在才发 rejected receipt | `MATERIAL_WMS_409` 被分类为 terminal rejection | PASS |
| 外部服务故障 | 外部返回 5xx | 分类为 transient，由 Kafka 重试/DLQ 策略处理 | `503` 未生成 rejected receipt | PASS |
| 幂等事实冲突 | 同 key 对应数量或库存维度不一致 | fail closed，禁止接受或再次创建 | `WMS_IDEMPOTENCY_CONFLICT` | PASS |
| 单位不一致 | 命令单位与精确 route 基础单位不符 | WMS 调用前拒绝 | `WMS_UNIT_MISMATCH` | PASS |
| Kafka 身份冲突 | topic/key/header 与 payload 不一致 | 不调用 WMS，进入既有 DLQ 路径 | fail closed | PASS |

## HTTP 合同

- 查询：`GET /material/wms/completion-inbounds/by-idempotency?sourceSystem=BPI&idempotencyKey=...`
- 创建：`POST /material/produceInSingles/produceInSingl/generateProductInSingle`
- 隔离头：`X-Tenant-Id` 与 `X-BPI-WMS-Key`
- 创建事实：`sourceSystem=BPI`、原 `idempotencyKey`、`srcID=command event id`、
  `redBlue=blue`、单行来源 ID、物料、批次、仓库、库位、数量和单位
- 成功门槛：HTTP 成功本身不成立，必须随后通过原幂等键查到且全部事实一致
- 不确定失败：不得生成 rejected receipt；继续由 Kafka 固定重试和 DLQ 保护
- 业务拒绝：先查单排除“已提交但返回冲突”，确认无单后才生成 rejected receipt

## 剩余生产门槛

1. 用真实外部 ERP/WMS 测试实例执行同一 marker 的查询、超时、4xx、5xx 和响应丢失演练。
2. 将已实现的内部耐久红字单合同接入 BPI 独立命令、四眼审批、Protobuf/Kafka adapter 和补偿回执。
3. 在真实页面、Kafka、BPI PostgreSQL 与外部 WMS 数据库之间完成 before/after 查询和清理。
4. 在上述证据闭合前保持 Phase 2、WMS outbox、WMS adapter 和 scope feature flag 关闭。
