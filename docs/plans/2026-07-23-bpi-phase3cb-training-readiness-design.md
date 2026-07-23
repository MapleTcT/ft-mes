# BPI Phase 3C-B 离线训练就绪评估设计

## 1. 目标与第一性原理

本阶段回答一个问题：当前不可变 Dataset Input 是否已经具备训练首个可解释模型的最低证据。它不负责训练模型。

首个模型目标固定为 `BATCH_START_BOUNDARY_REVIEW_RISK`：在自动 START 边界生成后，估计该边界需要人工复核的风险。选择它是因为它直接服务“批次边界可信度”，且输出只用于建议和复核，不会写 PLC、改批次或绕过人工确认。

三行样本可以验证软件链，不能证明模型有效。系统必须把“数据可追溯”和“数据足够训练”分开，缺少真实过程窗口、拒绝样本、生产日期或现场周期时返回 `BLOCKED`，不能降低门槛制造绿色状态。

## 2. 输入事实

评估只读取已经 `REGISTERED` 的 MLflow Dataset Input 及其钉扎的 PostgreSQL 事实：

- V26 dataset definition、snapshot 和 snapshot samples；
- V19/V25 已批准影子运行和人工批次复核；
- 固定 rule、topology、point catalog 版本；
- V30 registration 的行数、digest、精确 source 和三项读回验证；
- 与来源影子运行时间重叠的 CRITICAL 数据质量事件。

评估不读取未版本化的实时曲线，不调用训练框架，也不写 MLflow。

## 3. 准入策略

策略版本为 `bpi-training-readiness/batch-start-boundary-v1`，包含 19 个独立门槛：

1. Dataset Input、血缘和来源事实已验证；
2. prediction/cutoff/split policy 符合 point-in-time 约束；
3. MLflow 行数、纳入样本和快照计数完全对账；
4. 特征截止和标签可用时间不存在未来泄漏；
5. 物料、工段、规则、拓扑、点位目录上下文齐全；
6. 至少两个 `signal.*`、`telemetry.*`、`process.window.*` 或 `parameter.window.*` 过程窗口特征；
7. 声明 `review.boundary_acceptance` 标签；
8. 至少 200 个纳入样本；
9. 至少 200 个独立批次；
10. 至少覆盖 7 个生产日期；
11. 至少两个生产时间切分组；
12. 排除比例不高于 20%；
13. 至少 100 个 START 接受标签；
14. 至少 10 个 START 拒绝标签；
15. 纳入样本无缺失 START 标签；
16. 所有来源影子运行均已批准；
17. 来源影子运行满足 7 天现场周期；
18. 所有钉扎点位目录保留 READY 证据；
19. 来源窗口无未解决 CRITICAL 数据质量事件。

任何一项失败都返回 `BLOCKED`。只有全部通过才返回 `ELIGIBLE`，而 `ELIGIBLE` 也只允许后续提交训练申请。

## 4. PostgreSQL 与不变量

Flyway V31 新增 `bpi.bpi_dataset_training_readiness_assessments`。每次评估写入新行，使用 `(tenant_id, mlflow_registration_id, objective_code, policy_version, assessment_sequence)` 唯一约束。

记录包含输入 revision、manifest checksum、dataset digest、thresholds、observed metrics、19 项 gate、blocker codes、assessment checksum、actor/reason/time。终态仅为 `ELIGIBLE/BLOCKED`，触发器禁止 UPDATE，运行角色无 DELETE 权限。

固定阶段边界为：

```json
{
  "assessmentOnly": true,
  "trainingStarted": false,
  "modelCreated": false,
  "modelRegistered": false,
  "onlineInferenceEnabled": false,
  "productionActivationAllowed": false
}
```

## 5. API 与权限

| Method | Path | 语义 |
| --- | --- | --- |
| GET | `/bpi/v1/dataset-mlflow-registrations/{registrationId}/training-readiness-assessments` | 查询当前策略最近一次评估 |
| POST | `/bpi/v1/dataset-mlflow-registrations/{registrationId}/training-readiness-assessments` | 按 registration revision 幂等创建一次评估 |
| GET | `/bpi/v1/dataset-training-readiness-assessments/{assessmentId}` | 回读指定不可变评估 |

写操作只允许 `BPI_ENGINEER/BPI_ADMIN`，要求 `Idempotency-Key`、`If-Match` 和至少 3 字符原因。读写都执行 tenant/plant/line scope。重复幂等请求返回同一响应，不增加 assessment sequence。

## 6. 页面交互

数据交付链变为七步：

```text
Manifest -> Parquet -> Iceberg -> 恢复包 -> Dataset Input -> 训练资格 -> 模型训练
```

只有 Dataset Input 为 `REGISTERED` 才显示“评估训练资格”。结果卡展示模型目标、策略、样本/批次、生产日/切分组、过程窗口、标签类别、排除率、现场周期、质量事件、失败门槛、checksum 和模型阶段边界。页面刷新后从 PostgreSQL 回读最近一次评估；再次评估创建新序号，不覆盖历史。

## 7. 验收与回滚

本地必须通过 builder 单测、PostgreSQL 15 集成测试、OpenAPI/Java 8 路由、确定性模拟 API 和桌面/390px Playwright。目标环境必须用唯一 marker 从真实 ADP 页面创建评估，查询 PostgreSQL、审计与幂等，并在评估前后确认 MLflow run/input/model 计数未被改变；重启后仍能回读，最后定向清理 marker。

V31 是 expand-only。回滚只回退 API/页面入口，不删除迁移、不降级 Flyway、不修改 V26-V30 事实。

## 8. 下一纵切

当前真实缺口不是训练代码，而是过程窗口特征。下一阶段应先把边界前后流量、泵、阀、液位和关键工艺参数按事件时间形成版本化窗口特征，重新生成至少 200 个独立批次、7 个生产日和足够正反标签的数据集。满足本评估后，才设计离线训练、时间切分评估、模型卡、四眼审批和 shadow inference。
