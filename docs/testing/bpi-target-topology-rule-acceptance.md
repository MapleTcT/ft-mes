# BPI 目标环境拓扑与规则产品化验收

## 结论

2026-07-15 在 `ubuntu-test`（Tailscale `100.99.133.43`）完成 BPI Flyway V9、拓扑创建/校验/独立发布、规则创建、PostgreSQL 落库和服务重启后读取验收，结论为 **PASS**。

本次使用唯一 marker `ADP_E2E_20260715_004849_BPI_PRODUCT_TARGET`。操作来自真实 ADP 登录后的 BPI 页面 `http://100.99.133.43:18091/#/rules`，不是直接插入业务表。机器可读证据见 [`metadata/bpi-target-topology-rule-acceptance.json`](../../metadata/bpi-target-topology-rule-acceptance.json)。

## 验收结果

| 验收项 | 页面/API | PostgreSQL 结果 | 状态 |
|---|---|---|---|
| V9 迁移 | `bpi-migrate` 单次执行，runtime smoke | `bpi.flyway_schema_history` 最大版本为 `9`，BPI schema 表数为 `19` | PASS |
| 真实认证 | ADP 登录 `200`，进入 `/#/rules` | 服务端会话映射到 tenant `1000`、plant `PLANT-01`、line `LINE-S07-01` | PASS |
| 拓扑草稿 | `POST /bpi-api/topologies/drafts` 返回 `200` | `bpi_topology_versions` 为 `DRAFT/r1` | PASS |
| 拓扑校验 | `POST /bpi-api/topologies/{id}/validate` 返回 `200` | `validation_status=PASSED`、错误数 `0`、validated checksum 匹配，revision `2` | PASS |
| 创建者发布门禁 | 创建者发布返回预期 `422` | 事务回滚，未产生错误发布审计或完成幂等记录 | PASS |
| 独立管理员发布 | 独立主体调用 `POST /bpi/v1/topologies/{id}/publish` 返回 `200` | `PUBLISHED/r3`，`published_by=target-topology-approver` | PASS |
| 规则草稿 | 页面选择已发布拓扑后 `POST /bpi-api/rules/drafts` 返回 `200` | `bpi_rule_versions` 为 `DRAFT/r1`，产线和 `feed.flow` 从拓扑继承 | PASS |
| 审计与幂等 | 页面和内部发布 API | 4 条动作审计、4 条 `COMPLETED/200` 幂等记录与业务行一致 | PASS |
| 服务重启 | 仅重启 `bpi-service`，等待 health | 服务在第 6 次轮询恢复 `healthy`，PostgreSQL 未重建 | PASS |
| 重启后页面读取 | 重新登录并打开 `/#/rules` | 页面唯一显示 `PUBLISHED/PASSED/r3` 拓扑和 `DRAFT/r1` 规则，console/page/request failure 均为 `0` | PASS |

## 落库证据

目标业务对象：

- topology id：`b3f1bf60-5680-4da9-b7c2-ed7696f839c7`
- rule id：`f9288dbe-1b40-465e-a32d-0697c1f3c301`
- topology code：`ADP_E2E_20260715_004849_BPI_PRODUCT_TARGET_TOPOLOGY`
- rule code：`ADP_E2E_20260715_004849_BPI_PRODUCT_TARGET_START`

直接查询的核心 SQL：

```sql
SELECT topology_code, state, revision, validation_status,
       jsonb_array_length(validation_errors) AS validation_error_count,
       validated_checksum = checksum AS validated_checksum_matches,
       validated_by, published_by
FROM bpi.bpi_topology_versions
WHERE tenant_id = '1000'
  AND topology_code = 'ADP_E2E_20260715_004849_BPI_PRODUCT_TARGET_TOPOLOGY';

SELECT r.rule_code, r.state, r.revision, r.plant_id, r.line_id,
       t.topology_code || '@' || t.version AS topology_version
FROM bpi.bpi_rule_versions r
JOIN bpi.bpi_topology_versions t ON t.id = r.topology_version_id
WHERE r.tenant_id = '1000'
  AND r.rule_code = 'ADP_E2E_20260715_004849_BPI_PRODUCT_TARGET_START';

SELECT object_type, action, actor_id, before_revision, after_revision
FROM bpi.bpi_audit_events
WHERE tenant_id = '1000'
  AND object_id IN (
    'b3f1bf60-5680-4da9-b7c2-ed7696f839c7',
    'f9288dbe-1b40-465e-a32d-0697c1f3c301'
  )
ORDER BY created_at;
```

实际结果摘要：拓扑 `PUBLISHED/PASSED/r3`，规则 `DRAFT/r1`；审计顺序为 `TOPOLOGY_DRAFT_CREATED -> TOPOLOGY_VALIDATION_PASSED -> TOPOLOGY_PUBLISHED -> RULE_DRAFT_CREATED`。创建/校验/独立发布/规则创建的 4 条幂等记录均为 `COMPLETED/200`。

## 部署与恢复边界

- 部署前保留 runtime 备份和 PostgreSQL custom-format schema dump，并使用 `pg_restore -l` 验证 dump 可读。
- 保留 V8 service/adapter 镜像标签；本轮只执行 V8 到 V9 的前向迁移，没有执行数据库回退或全栈回滚。
- V9 迁移后仅顺序重建 BPI service、adapter、web；既有 `adp-mes-newbase`、Kafka/Flink/MinIO 未被停止或替换。
- marker 数据暂时保留在测试库，供其他线程从真实页面和 PostgreSQL 复验；它不是生产主数据。

## 未覆盖范围

本次通过的是产品化拓扑/规则配置链。拓扑绑定使用受控测试设备和 `feed.flow` 信号，不证明真实网关/协议设备点位已经接入，也不证明 IoT 与 MES context 已用同一 marker 形成候选/批次。真实点位联合链、7-14 天影子运行、END 边界、QCS/WMS 写回和生产投用仍未完成。
