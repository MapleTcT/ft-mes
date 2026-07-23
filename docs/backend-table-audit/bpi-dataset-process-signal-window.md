# BPI 工艺信号窗口落表审计

## 边界

Phase 3C-C 在数据集预测时点之前读取已绑定的过程测点，把聚合结果和阻断原因固化为不可变事实。
它不创建模型、不训练模型、不注册模型，也不允许在线推理或生产激活。

## 表和迁移

| 迁移 | 对象 | 用途 |
|---|---|---|
| V32 | `bpi_dataset_definitions.process_signal_windows` | 保存最多 20 组不可变窗口定义 |
| V32 | `bpi_dataset_process_signal_window_facts` | 每个 snapshot/review/feature 一条 point-in-time 事实 |
| V32 | `trg_bpi_dataset_process_signal_window_immutable` | 拒绝事实 UPDATE |
| V33 | BPI 函数默认权限 | 撤销 `PUBLIC EXECUTE` 并锁定后续默认权限 |

事实表主键为 `(snapshot_id, review_id, feature_ref)`。租户、snapshot、review、shadow run、
batch 均使用复合外键；READY 必须有值、无 blocker、满足最少样本和最大间隔，BLOCKED 必须至少有一个
blocker。

## 读取和写入链

| 动作 | 入口 | 后端 | 表 |
|---|---|---|---|
| 创建窗口定义 | `POST /bpi/v1/datasets` | `DatasetController -> DatasetService -> DatasetPostgresRepository` | definitions + audit + idempotency |
| 查询 point-in-time 证据 | snapshot worker | `ProcessSignalWindowPostgresRepository.findEvidence` | topology、catalog、calibration、telemetry、review/batch |
| 计算窗口 | snapshot worker | `ProcessSignalWindowBuilder.build` | 内存确定性聚合 |
| 固化清单 | snapshot worker | `DatasetManifestBuilder -> DatasetPostgresRepository.completeSnapshot` | facts + samples + snapshot + audit |
| 页面读回 | `GET /bpi/v1/dataset-snapshots/{id}` | `DatasetController -> DatasetPostgresRepository` | 只读 |

`findEvidence` 同时要求：

- 逻辑信号存在于已发布拓扑 binding。
- 物理点位存在于 topology 固定的 catalog snapshot。
- 设备 ACTIVE、registered、property present。
- 单位和值类型匹配。
- 要求校准时存在同版本、有效期内的 APPROVED 校准。
- quality code 在窗口允许集合内。
- sample time 位于窗口内。
- ingest time 不晚于 prediction time 才能成为特征值。
- ingest time 晚于 snapshot freezeAt 的点不进入冻结事实。

任一条件不满足均产生明确 blocker，不用 0、空字符串或最后值兜底。

## 目标结果

marker `ADP_E2E_BPI_WINDOWS_20260723_1235_A1`：

| 查询 | 结果 |
|---|---|
| definition process windows | 2 |
| snapshot samples | 3 |
| facts | 6 |
| READY / BLOCKED | 2 / 4 |
| flow source / accepted / late | 4 / 3 / 1 |
| flow MEAN | 20 |
| pump source / accepted | 2 / 2 |
| pump TRUE_RATIO | 0.5 |
| cutoff-safe facts | 6 |
| valid fingerprint/checksum | 6 |
| label leakage / cross plant | 0 / 0 |
| immutable UPDATE | rejected |

验收 SQL：

- `deploy/docker/scripts/bpi-dataset-manifest-target-fixture.sql`
- `deploy/docker/scripts/bpi-dataset-process-window-target-fixture.sql`
- `deploy/docker/scripts/bpi-dataset-manifest-target-verification.sql`
- `deploy/docker/scripts/bpi-dataset-manifest-target-cleanup.sql`

## 权限

`bpi_service` 只获得事实表 SELECT/INSERT。四个可选 worker 角色不能执行 BPI schema 函数。
V33 同时撤销现有函数和后续默认函数对 PUBLIC 的 EXECUTE，避免通过 PUBLIC 间接绕过专用角色门禁。

## 清理

cleanup 先删除 process-window facts，再删除 snapshot samples 和上游数据集；遥测 point 先于 event，
topology 先于 catalog entry/snapshot。目标 marker 的 19 类投影全部为 0。

## 状态

`已恢复并完成目标验收`。该状态只覆盖受控点位的工艺窗口固化和失败关闭，不覆盖真实设备、
正式校准、样本量、生产日覆盖或模型资格。
