# 后端落表排查索引

本目录用于沉淀后续专门线程的后端落表和业务含义排查结果。
目录路径：`docs/backend-table-audit/`。

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
| `business-quality-lims-qcs.md` | 已开始 | LIMS、QCS、Qualify 质量域；QCS 请检/报告明细 PostgreSQL 缺表已用 `107-qcs-inspect-detail-tables.sql` 处理，业务写动作仍需 marker 验收 |
| `business-production.md` | 测试矩阵已建立 | 生产、工单、报工、追溯；WTS 正常关闭主流程已 PASS，作废/终止分支待测；动作级落库仍需逐项补 |
| `wom-consumption-record-analysis.md` | 已完成专项解释 | WOM 投入明细已落库但不自动生成 `wom_mat_consum_recods` 的源码和远端库证据 |
| `wom-public-produce-task-created-analysis.md` | 已完成专项解释 | public `produceTaskCreated` 旧实现返回成功但不落库；测试运行包已显式禁用该入口，仍需产品确认是否恢复为对外契约；不是 PostgreSQL 兼容 SQL 缺口 |
| `material-service-dependency-analysis.md` | 已完成专项解释 | WOM/QCS 完工入库、库存回写依赖缺失的 `material` 租户服务；`100.99.133.43` Nacos、网关、PostgreSQL 和包扫描均已复验 |
| `processanalysis-dependency-analysis.md` | 已完成专项解释 | WOM 生产过程追溯依赖缺失的 `ProcessAnalysis` 租户服务；`100.99.133.43` Nacos、网关、PostgreSQL 和包扫描均已复验 |
| `business-equipment-energy-ehs.md` | 待开始 | 设备、能源、安环 |
| `persistence-acceptance.md` | 模板已建立 | 真实前端动作到 PostgreSQL 落库证明 |

## 验收规则

- 功能验收入口：[功能验收与落库验收规则](../functional-persistence-acceptance.md)
- 前端功能报告：[前端功能测试报告](../frontend-functional-test-report.md)
- 机器可读记录：`metadata/persistence-acceptance.json`
- 结构校验：`make persistence-acceptance-check`
