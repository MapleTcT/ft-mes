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
| `business-production.md` | 核心主线已验收 | 制造、报工、请检、质量处置、完工入库和追溯已完成真实页面/API/PostgreSQL marker；其余产品范围与导出项独立跟踪 |
| `wom-consumption-record-analysis.md` | 已完成专项解释 | WOM 投入明细已落库但不自动生成 `wom_mat_consum_recods` 的源码和远端库证据 |
| `wom-public-produce-task-created-analysis.md` | 已完成退役验收 | public `produceTaskCreated` 旧实现返回成功但不落库；当前已正式废弃并以 `HTTP 200/code=400/已废弃` 明确拒绝，PostgreSQL marker `0 -> 0`，`PROD-ACTION-007` 按 `NOT_APPLICABLE` 关闭；不是 PostgreSQL 兼容 SQL 缺口 |
| `material-service-dependency-analysis.md` | 已完成专项解释 | WOM/QCS 完工入库、库存回写依赖缺失的 `material` 租户服务；`100.99.133.43` Nacos、网关、PostgreSQL 和包扫描均已复验 |
| `processanalysis-dependency-analysis.md` | 已恢复并验收 | WOM 生产过程追溯由 `process-analysis` 源码模块恢复；`100.99.133.43` Nacos、网关、真实按钮和 PostgreSQL marker 均已复验 |
| `business-equipment-energy-ehs.md` | 待开始 | 设备、能源、安环 |
| `bpi-phase1-persistence.md` | 已恢复并验收 | BPI 候选确认/拒绝、影子批次、证据、状态、幂等和审计 PostgreSQL 事务链 |
| `bpi-telemetry-ingress.md` | 已恢复并验收 | 遥测 replay 幂等、序列状态、点级拒绝和隔离；HTTP 默认关闭 |
| `bpi-candidate-protobuf-ingress.md` | 已恢复并验收 | Flink `BatchCandidateV1` 完整证据经 Protobuf bridge 入库，并已由目标环境 Kafka/浏览器联合 marker 复验 |
| `bpi-candidate-kafka-ingress.md` | 已恢复并验收 | 目标环境三 broker、Flink、candidate listener、DLQ、PostgreSQL 和真实候选页面已闭合；退役前产生、退役后消费的候选按历史发布版本恰好一次落库 |
| `bpi-quality-release-wms-inbound.md` | 已开始 | 本地与目标 V23 软件合同 PASS、真实 QCS/WMS 外部系统 BLOCKED；目标 Java 8 release 路由、质量/库存页、重启读取、4/4 PostgreSQL marker 和 12 表零残留已通过，Phase 2 开关保持关闭 |
| `bpi-dataset-manifest.md` | 已恢复并验收 | Flyway V26-V28 数据集定义、快照、样本、Parquet、Iceberg、审计和幂等链已由真实 ADP 页面/API/PostgreSQL/MinIO/Polaris marker 闭合；V28 post-commit fencing 已恢复到同一 snapshot |
| `bpi-dataset-retention-archive.md` | 已恢复并验收 | Flyway V29 Object Lock 恢复包已完成真实页面失败/重试、PostgreSQL r1-r7、精确对象版本、最小权限、隔离 Polaris 恢复/物理清除和零残留退场；不替代整站灾备 |
| `bpi-dataset-mlflow-registration.md` | 已恢复并验收 | Flyway V30 MLflow Dataset Input 已完成真实页面失败/重试、PostgreSQL r1-r6、精确 source、1 run/1 input、重启不重复、最小权限和零残留退场；不声明模型训练或投产 |
| `bpi-dataset-training-readiness.md` | 本地已实现，目标待验收 | Flyway V31 对 REGISTERED Dataset Input 生成不可变离线训练就绪评估；19 门槛失败关闭，模型训练/注册/推理/激活均保持 false |
| [`../testing/bpi-rule-retirement-acceptance.md`](../testing/bpi-rule-retirement-acceptance.md) | V15 受控影子验收 PASS | 规则退役、typed inactive、savepoint 有状态升级、回滚草稿、延迟候选落库和 11 类 marker 清理；不声明现场 READY 或生产写回 |
| `bpi-iot-replay-runtime-acceptance.md` | 已恢复并验收 | IoT 信号经边界引擎、Protobuf、候选确认到 PostgreSQL 的跨模块闭环 |
| `persistence-acceptance.md` | 模板已建立 | 真实前端动作到 PostgreSQL 落库证明 |

## 验收规则

- 功能验收入口：[功能验收与落库验收规则](../functional-persistence-acceptance.md)
- 前端功能报告：[前端功能测试报告](../frontend-functional-test-report.md)
- 机器可读记录：`metadata/persistence-acceptance.json`
- 结构校验：`make persistence-acceptance-check`
