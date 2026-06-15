# 前端功能测试报告

## 当前状态

本文件是功能验收记录入口。当前仅建立报告模板，不代表系统功能已经完成验收。

执行功能验收时，必须按照 [功能验收与落库验收规则](functional-persistence-acceptance.md) 更新本文件，并同步更新 `metadata/persistence-acceptance.json`。

## 环境

| 项 | 值 |
| --- | --- |
| 前端入口 | `http://10.11.100.17:18080` |
| 默认数据库 | PostgreSQL |
| Oracle 状态 | legacy-template-only / oracle-legacy only |
| 测试方式 | 真实浏览器或等效 E2E |

## 总览

| 指标 | 数量 |
| --- | ---: |
| 被测功能 | 0 |
| PASS | 0 |
| FAIL | 0 |
| BLOCKED | 0 |
| NOT_APPLICABLE | 0 |

## 功能验收明细

| 模块 | 页面/路由 | 操作 | API | 前端结果 | 后端结果 | 数据库表 | 验收状态 | 问题 |
|---|---|---|---|---|---|---|---|---|

## 记录要求

- 每个功能必须记录真实页面操作步骤、预期结果和实际结果。
- 必须记录 console error、page error、network error、可见系统错误和空白页情况。
- 会改变业务数据的操作必须在 [后端落库验收报告](backend-table-audit/persistence-acceptance.md) 中有对应记录。
- 未执行的功能不要写成 `PASS`。
