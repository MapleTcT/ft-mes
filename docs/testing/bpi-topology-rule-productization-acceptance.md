# BPI 拓扑与规则产品化验收

状态：`PASS_LOCAL_PRODUCTIZATION`
日期：2026-07-14
数据库：PostgreSQL 16.13
结论边界：本报告证明本地真实 PostgreSQL 与确定性浏览器链通过，不代表真实设备点位或生产 READY。

## 验收结论

| 验收项 | 入口 | 持久化/证据 | 结果 |
|---|---|---|---|
| 拓扑草稿创建 | `POST /bpi/v1/topologies/drafts` | `bpi_topology_versions`、`bpi_audit_events`、`bpi_api_idempotency` | PASS |
| 拓扑结构校验 | `POST /bpi/v1/topologies/{id}/validate` | FAILED/PASSED、结构化错误、checksum、revision、审计 | PASS |
| 拓扑独立发布 | `POST /bpi/v1/topologies/{id}/publish` | 创建人发布被拒绝，另一管理员发布为 PUBLISHED r3 | PASS |
| 规则草稿创建 | `POST /bpi/v1/rules/drafts` | 未绑定信号被拒绝；有效规则写入 `bpi_rule_versions` 和审计 | PASS |
| 幂等与并发 | 全部写入口 | 同键重放不重复插入，过期 revision 返回冲突 | PASS |
| 产品交互 | `/#/rules` | 新建拓扑、校验、发布、新建规则完整浏览器操作 | PASS |
| 移动端布局 | 390 x 844 | 页面级横向溢出为 0 | PASS |
| 浏览器错误 | Chromium E2E | console/page/request failed 为 0 | PASS |

## 真实 PostgreSQL 链

验收类：`services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/BpiTopologyRuleProductizationPostgresAcceptanceTest.java`

测试使用唯一 `ADP_E2E_BPI_PRODUCT_*` 租户 marker，在隔离 PostgreSQL 16.13 中从 Flyway V1 连续迁移到 V9，
通过 MockMvc 发送真实 HTTP 命令，并直接查询以下表：

```sql
SELECT state, validation_status, revision, published_by
FROM bpi.bpi_topology_versions
WHERE tenant_id = ? AND id = ?;

SELECT state, revision, created_by
FROM bpi.bpi_rule_versions
WHERE tenant_id = ? AND id = ?;

SELECT action, before_revision, after_revision
FROM bpi.bpi_audit_events
WHERE tenant_id = ? AND object_id = ?
ORDER BY after_revision;
```

实际结果摘要：有效拓扑为 `PUBLISHED|PASSED|3|topology-publisher`，规则为
`DRAFT|1|topology-creator`；拓扑审计依次为 `DRAFT_CREATED 0→1`、`VALIDATION_PASSED 1→2`、
`PUBLISHED 2→3`。故意缺少 bindings 的拓扑持久化为 `FAILED|2|1 error`。未绑定信号规则返回 422，
事务回滚后未留下规则或幂等占位。测试结束按 tenant marker 定向清理。

## 浏览器链

验收类：`frontend/apps/bpi/tests/bpi-console.e2e.cjs`

七条 Chromium E2E 全部通过，其中产品化用例执行：

1. 新建 `ADP_E2E_TOPOLOGY@1.0.0` 草稿；
2. 打开拓扑详情并校验节点、路径和 JetLinks 绑定；
3. 发布为不可变拓扑 r3；
4. 选择该已发布拓扑，创建 `ADP_E2E_BATCH_START@1.0.0` 规则草稿；
5. 回读模拟 API，确认拓扑 `PUBLISHED/PASSED/r3`、规则 `DRAFT/r1`；
6. 验证桌面抽屉、移动端无页面溢出、浏览器错误为 0。

## 复验命令

```bash
make bpi-api-contract-check

cd frontend/apps/bpi
npm run build
npm run test:e2e

# 设置隔离 PostgreSQL 的 BPI_TEST_DATABASE_* 后执行
mvn -f services/bpi-service/pom.xml -pl app -am \
  -Dtest=BpiTopologyRuleProductizationPostgresAcceptanceTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

## 未完成边界

- 规则发布现有 simulation checksum 门禁仍保留，但规则“双人审批工作流”尚未实现。
- 目标测试环境需部署 V9 和本批前后端后，再补真实 ADP 会话浏览器与 PostgreSQL marker 复验。
- 真实网关/协议设备点位、IoT 与 MES context 同 marker candidate/batch、7–14 天影子运行未完成。
- QCS/WMS 写回仍关闭。
