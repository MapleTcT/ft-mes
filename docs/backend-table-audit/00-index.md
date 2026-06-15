# 后端落表排查索引

本目录用于沉淀后续专门线程的后端落表和业务含义排查结果。

入口说明见：

- [后端落表业务排查交接](../backend-table-audit-handoff.md)
- [项目目标和交付路线](../project-objectives.md)
- [Oracle 到 PostgreSQL 替换路线](../oracle-to-postgres-transition.md)

## 建议报告清单

| 报告 | 状态 | 说明 |
| --- | --- | --- |
| `platform-auth-rbac-org.md` | 待开始 | 登录、组织、角色、菜单、权限 |
| `platform-entity-config.md` | 待开始 | `ec_*` 元数据、实体配置、动态页面 |
| `platform-flow-todo.md` | 待开始 | 流程、待办、任务调度 |
| `business-quality-lims-qcs.md` | 待开始 | LIMS、QCS、Qualify 质量域 |
| `business-production.md` | 测试矩阵已建立 | 生产、工单、报工、追溯；动作级落库仍需逐项补 |
| `business-equipment-energy-ehs.md` | 待开始 | 设备、能源、安环 |
| `persistence-acceptance.md` | 模板已建立 | 真实前端动作到 PostgreSQL 落库证明 |

## 验收规则

- 功能验收入口：[功能验收与落库验收规则](../functional-persistence-acceptance.md)
- 前端功能报告：[前端功能测试报告](../frontend-functional-test-report.md)
- 机器可读记录：`metadata/persistence-acceptance.json`
- 结构校验：`make persistence-acceptance-check`
