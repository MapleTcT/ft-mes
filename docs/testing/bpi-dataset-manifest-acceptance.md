# BPI 数据集清单工作台验收

## 结论

2026-07-22 完成 BPI Phase 3A 数据集清单工作台的本地软件闭环，状态为
**`PASS_LOCAL_BROWSER_API_POSTGRES_MANIFEST_ONLY`**。

本轮实现并验证了受控数据集定义、时间点冻结、批准影子批次筛选、低置信度与标签延迟排除、
特征/标签隔离、确定性 checksum、异步 worker、幂等重放、租户/工厂/产线范围控制、终态不可变、
桌面与移动端清单检查。PostgreSQL 16.13 从空库应用 Flyway V1-V26；浏览器 E2E `20/20 PASS`，
模拟 API `15/15 PASS`，Java 8 Adapter 路由测试通过，定向 Java API/worker/PostgreSQL 验收通过。

交付边界是 **manifest-only**：数据库明确保持 `materializationState=NOT_STARTED`、
`artifactUri=null`、`icebergReady=false`、`mlflowRegistered=false`、`modelTrained=false`。
本轮不声明训练数据制品、Iceberg 表、MLflow 注册、模型训练或生产预测已经完成。

## 功能验收

| 模块 | 页面/路由 | 操作 | API | 前端结果 | 后端结果 | 数据库表 | 状态 |
|---|---|---|---|---|---|---|---|
| 数据集定义 | `/bpi/#/datasets` | 选择受控特征/标签、产线、置信度和标签延迟，创建不可变版本 | `POST /bpi/v1/datasets` | 对话框校验、保存、列表刷新和详情抽屉正常 | `BPI_ENGINEER/BPI_ADMIN` 可写；同 key 重放原响应；越权角色/产线返回 403 | `bpi_dataset_definitions`、`bpi_audit_events`、`bpi_api_idempotency` | PASS |
| 清单快照 | `/bpi/#/datasets` | 选择 freezeAt、产线和规则版本，排队并轮询清单 | `POST /bpi/v1/datasets/{id}/snapshots`；`GET /bpi/v1/dataset-snapshots/{id}` | 页面从 `QUEUED` 到 `MANIFEST_READY`，显示 checksum、计数和排除原因 | worker 以 `FOR UPDATE SKIP LOCKED` 领取，持久化 3 个时间点样本：1 included、2 excluded | `bpi_dataset_snapshots`、`bpi_dataset_snapshot_samples`、审计表 | PASS |
| 时间点与泄漏控制 | 清单详情抽屉 | 检查 prediction time、feature cutoff、标签可用时间及 payload | 同上 GET | 抽屉显示样本边界和排除原因 | 3/3 样本 `feature_cutoff=prediction_time`；feature payload 中标签字段 0 个 | `bpi_dataset_snapshot_samples` | PASS |
| 确定性与不可变 | 同一数据集 | 对同一 freezeAt 再生成快照并尝试修改终态 | 同上 POST/GET | 两次清单 checksum 一致 | 两个独立 snapshot 得到相同 64 位 checksum；终态 UPDATE 被触发器拒绝 | `bpi_dataset_snapshots` | PASS |
| 阶段边界 | 清单详情抽屉 | 检查交付阶段和下游状态 | 同上 GET | 明确显示 `MANIFEST ONLY` | artifact 为空，物化/注册/训练均未开始 | `bpi_dataset_snapshots.manifest` | PASS |
| 响应式交互 | 桌面 `1440x900`；移动 `390x844` | 创建、打开定义和清单详情、检查底部导航与抽屉 | 确定性模拟器 | 无非预期 console/page/request error，无页面级横向溢出 | 浏览器套件不声明 PostgreSQL 落库 | 不适用 | PASS |

## 后端链路

```text
DatasetController
  -> DatasetService
  -> DatasetPostgresRepository
  -> bpi_dataset_definitions / bpi_dataset_snapshots
     / bpi_dataset_snapshot_samples / bpi_audit_events

DatasetManifestProcessor
  -> DatasetPostgresRepository.claimPending
  -> DatasetManifestBuilder
  -> DatasetPostgresRepository.completeManifest
```

定义和快照写操作要求 `Idempotency-Key` 与 `If-Match`。定义只能使用 `If-Match: 0`；快照必须
使用不可变定义当前 revision。快照只能选择定义内的产线，并且 freezeAt 时每条产线都必须存在
已批准、可用的影子运行与人工复核。未来 freezeAt、越权范围、未批准来源和泄漏风险字段均失败关闭。

## PostgreSQL 验收

数据库：`ft_mes_bpi_v26_dataset_20260722_0414`，PostgreSQL `16.13`，当前 Flyway 版本 `26`。
动态 marker 为 `ADP_E2E_BPI_DATASET_<UUID>`，测试结束后 definitions/snapshots/samples 均为 0。

关键验收 SQL：

```sql
SELECT state, snapshot_version, manifest_checksum, included_count, excluded_count,
       materialization_state, artifact_uri
FROM bpi.bpi_dataset_snapshots
WHERE tenant_id = :tenant_id AND id = :snapshot_id;

SELECT count(*)::integer AS total,
       count(*) FILTER (WHERE included)::integer AS included,
       count(*) FILTER (WHERE NOT included)::integer AS excluded,
       count(*) FILTER (WHERE feature_cutoff = prediction_time)::integer AS cutoff_safe,
       count(*) FILTER (
           WHERE jsonb_exists(feature_payload, 'review.manual_start_time')
              OR jsonb_exists(feature_payload, 'review.reference_quantity'))::integer AS leaked
FROM bpi.bpi_dataset_snapshot_samples
WHERE tenant_id = :tenant_id AND snapshot_id = :snapshot_id;

SELECT count(*)
FROM bpi.bpi_audit_events
WHERE tenant_id = :tenant_id
  AND object_id IN (:snapshot_id_1, :snapshot_id_2)
  AND action = 'DATASET_MANIFEST_READY';
```

实际结果摘要：`MANIFEST_READY`、`1 included`、`2 excluded`、`3 cutoff_safe`、`0 leaked`；
`CONFIDENCE_BELOW_THRESHOLD=1`、`LABEL_DELAY_EXCEEDED=1`；第二次快照 checksum 与第一次相同，
两个清单完成审计恰好 2 条。额外插入的 `PLANT-02 / LINE-S07-01 / DATASET-CROSS-PLANT`
已审批干扰事实未进入 `PLANT-01` 清单，样本表直查为 `cross_plant_rows=0`。

## 浏览器证据

- 桌面：`/tmp/bpi-dataset-manifest-desktop.png`，`1440x900`，SHA-256
  `131c362185672fab6062466cfa51b4c7b2350dca612b5dc84be40b159d788daa`
- 移动：`/tmp/bpi-dataset-manifest-mobile.png`，`390x844`，SHA-256
  `837c4dcfece56537682673fb064cc7d8bd932690915b8fb53989176a79406929`

截图为本机临时证据，不作为部署制品提交。浏览器使用确定性模拟器；真实 Java API 和数据库由
独立 PostgreSQL 验收覆盖，不能将两者拼接成尚未执行的目标环境端到端结论。

## 未关闭门槛

1. 将 V26、Java 8 Adapter 和前端静态包部署到 `10.11.100.17` 唯一 `adp-mes-newbase` 环境。
2. 使用真实 ADP 会话从页面创建 marker，捕获 API，再直查目标 PostgreSQL 并定向清理。
3. Phase 3B 另行实现可版本化物化制品；未实现前不得出现 Iceberg/MLflow/模型 ready 状态。
4. 模型训练、在线推断、漂移监控和生产激活仍需后续独立目标与验收。
