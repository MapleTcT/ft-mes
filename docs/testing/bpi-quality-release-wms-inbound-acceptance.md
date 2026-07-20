# BPI 质量放行与完工入库验收

## 验收结论

2026-07-20 在本地隔离 PostgreSQL 16.13 中完成 QCS 质量门与 WMS 完工入库软件链验收，状态为
**`PASS_LOCAL_POSTGRES_CONTRACTS_NOT_TARGET_ACTIVATED`**。源代码提交为
`22ddadebd20ed9ed5d7efd19c3c0ed49967b9c90`，自动化测试类为
`BpiQualityReleaseWmsPostgresAcceptanceTest`，4/4 通过。

本轮证明的是合同、状态机、事务、幂等和真实 PostgreSQL 落表，不包含真实 QCS/WMS 目标系统或
真实业务单据。后续提交 `94e2b2288bf52966be58e9dda97039a5455466a8` 已接入批次质量与库存
产品页，并以确定性模拟器完成 6 类状态、局部 503、重试和移动端验收；该浏览器证据见
`metadata/bpi-quality-inventory-ui-acceptance.json`，不能替代本文件的 PostgreSQL 证据，也不能冒充
目标 QCS/WMS 联合验收。Phase 2 仍默认关闭，Phase 1 仍为 `SHADOW_ONLY`。

## 功能结果

| 模块 | 页面/路由 | 操作 | API | 前端结果 | 后端结果 | 数据库表 | 验收状态 | 问题 |
|---|---|---|---|---|---|---|---|---|
| 批次质量 | `/bpi/#/batches` 质量区 | 接收 required PENDING | QCS Protobuf bridge | 模拟页面显示 `WAIT_QA` 和待最终确认项 | `WAIT_QA`，快照落库 | quality gate/link、batch、inbox、state、audit | PASS | 目标 QCS 未接入 |
| 批次质量 | 同上 | 同 payload 重放 | QCS Protobuf bridge | 查询投影不重复展示 | 不重复写入 | inbox + projections | PASS | 目标 QCS 未接入 |
| 批次质量 | 同上 | required 全部 final ACCEPTED | QCS Protobuf bridge | 模拟页面显示全部必检合格 | `RELEASED`，同事务生成 1 个 WMS outbox | quality gate/link、batch、outbox、WMS link、audit | PASS | 目标 QCS/WMS 未接入 |
| 批次质量 | 同上 | required final REJECTED | QCS Protobuf bridge | 模拟页面显示不合格且不显示已入库 | `REJECTED`，WMS outbox 为 0 | quality gate/link、batch、audit | PASS | 目标 QCS 未接入 |
| 完工入库 | `/bpi/#/batches` 库存区 | PUBLISHED 前回执 | WMS Protobuf bridge | 页面不按 HTTP 成功推断入库 | 409 且 inbox 回滚 | 无新增 | PASS | 目标 WMS 未接入 |
| 完工入库 | 同上 | accepted + document id | WMS Protobuf bridge | 仅有 durable `documentId` 时显示已入库 | `INBOUNDED`，单据号真实保存 | WMS link、batch、state、audit、inbox | PASS | 页面为模拟数据，目标单据未创建 |
| 完工入库 | 同上 | 未知状态及 rejected + error code | WMS Protobuf bridge | 模拟页面显示入库失败、错误码和详情 | 未知枚举值 `99` 返回 422 且事务无残留；合法拒绝保持 `RELEASED` 并标记 `FAILED` | WMS link、batch、state、audit、inbox | PASS | 目标 WMS 未接入 |
| 安全门禁 | 无 | 影子批次尝试发 WMS | service + DB trigger | NOT_APPLICABLE | Java 与 PostgreSQL 双层拒绝 | outbox 0 | PASS | 无 |
| 查询 | `GET /bpi/v1/batches/{batchId}/release` | 查询质量门、检验、回执 | OpenAPI/Java REST | 产品页已接入，17/17 浏览器回归通过 | 返回 PostgreSQL projection | quality gate/link、WMS link、batch | PASS | 目标部署仍 BLOCKED |
| 真实联合链 | 目标 ADP 页面 | 真实请检、放行、入库、查单 | Kafka + QCS/WMS adapters | 未执行 | 未激活 | 目标业务表/单据 | BLOCKED | 缺真实适配与目标 marker |

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

1. 真实 QCS/WMS adapter 必须只通过版本化 Protobuf 合同交互，禁止直连对方内部表。
2. 先开 `BPI_PHASE2_INTEGRATION_ENABLED` 和精确 tenant/plant/line allowlist，再按作用域启用
   `bpi.qcs-link`；真实回放通过后才允许启用 `bpi.wms-link`。
3. `BPI_WMS_OUTBOX_ENABLED` 只能在 broker/topic/ACL/consumer/查单能力全部就绪后启用。
4. 目标 marker 必须证明页面动作、HTTP/Kafka、controller/service/repository、SQL、目标 WMS 单据号一致。
5. 必须演练重复回执、PUBLISHED 前回执、WMS 超时后查单、拒绝、服务重启和 broker 重放。
6. 任一异常时先关闭集成开关，保留 outbox/inbox/audit，不删除未决事实或伪造成功回执。
