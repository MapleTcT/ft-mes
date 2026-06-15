# 后端落库验收报告

## 当前状态

本文件是后端“业务动作是否真实落库”的验收入口。当前仅建立报告模板，不代表任何业务动作已经通过落库验收。

落库验收必须来自真实前端操作或等效 E2E 操作，并且必须直接查询 PostgreSQL 证明数据变化。

## 总览

| 指标 | 数量 |
| --- | ---: |
| 验收动作 | 0 |
| PASS | 0 |
| FAIL | 0 |
| BLOCKED | 0 |
| NOT_APPLICABLE | 0 |

## 落库验收明细

| 业务动作 | 前端入口 | API endpoint | 后端入口 | 目标表 | 验收 SQL | 实际结果 | 状态 |
|---|---|---|---|---|---|---|---|

## 证据要求

- 每个写操作必须带唯一 marker，例如 `ADP_E2E_YYYYMMDD_HHMMSS_xxx`。
- 必须记录 request payload、response status 和 response body 关键字段。
- 必须追踪 controller / service / mapper / SQL / 目标表。
- 必须记录 PostgreSQL 查询语句和结果摘要。
- 更新、删除、禁用、启用、状态变更类操作必须证明字段变化。
- 接口成功但查库无变化时标记 `FAIL`。
- 不落库功能必须说明原因并标记 `NOT_APPLICABLE`。
