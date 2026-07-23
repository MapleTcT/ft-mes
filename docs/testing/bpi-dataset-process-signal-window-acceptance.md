# BPI 工艺信号窗口目标验收

## 结论

2026-07-23 在唯一测试栈 `10.11.100.17 / adp-mes-newbase` 完成 BPI Phase 3C-C
工艺信号窗口真实页面、API 和 PostgreSQL 验收。状态为
`PASS_TARGET_BROWSER_API_POSTGRES_PROCESS_WINDOWS_CLEANED_DEFAULT_OFF`。

这次关闭的是“预测时点之前的过程测点能否按受控窗口形成不可变数据集事实”。它没有启动模型训练，
也没有证明真实物理设备、正式校准证书、200 个复核批次或 7 个生产日已经具备。

## 运行身份

| 项目 | 值 |
|---|---|
| marker | `ADP_E2E_BPI_WINDOWS_20260723_1235_A1` |
| release | `f7db2f98e82d481f6c53c5fa7539ac52c812e28f` |
| 数据库 | PostgreSQL 15.18 / `ft_mes_bpi` |
| Flyway | `32 -> 33`，expand-only |
| 页面 | `http://10.11.100.17:18080/bpi/#/datasets` |
| definition | `9e59e8fa-c3f7-4c5c-a1a8-53c1d0996092` |
| snapshot | `bc14794e-f388-47cd-9265-3c707bf3035c` |
| definition checksum | `8f7ff89ccd2b17d386ca7076995869fad158e0458151b3102a8a09835ce34da1` |
| manifest checksum | `4d0586bd42213d6270cee89102d3409ae498c66c196a6688ab9dd0c75bb04ed7` |

升级报告为 `metadata/bpi-integrated-upgrade-v33-target.json`，SHA-256 为
`c08445946b1abc309920fde99d2a50770e8c41b9c13d998a55cfc009da2ea342`。

## 页面动作

真实 `admin` 会话从“数据集清单”新建定义。页面默认给出两组可编辑、可增删、受校验的窗口：

| 特征引用 | 信号 | 类型/聚合 | 窗口 | 样本/间隔 | 单位 | 校准 |
|---|---|---|---|---|---|---|
| `process.window.flow_instant.mean_60s` | `flow.instant` | NUMERIC/MEAN | `-60s -> 0s` | 3 / 30s | `t/h` | 必须 |
| `process.window.pump_running.true_ratio_30s` | `pump.running` | BOOLEAN/TRUE_RATIO | `-30s -> 0s` | 2 / 30s | `bool` | 不要求 |

页面实际发出：

```http
POST /bpi-api/datasets
POST /bpi-api/datasets/9e59e8fa-c3f7-4c5c-a1a8-53c1d0996092/snapshots
GET  /bpi-api/dataset-snapshots/bc14794e-f388-47cd-9265-3c707bf3035c
```

定义返回 HTTP 200；快照返回 HTTP 202，后台完成后为 `MANIFEST_READY/r3`。详情页显示 3 个样本、
6 条窗口事实、2 READY 和 4 BLOCKED。

## PostgreSQL 事实

后端链路：

```text
DatasetController
  -> DatasetService
  -> DatasetManifestProcessor
  -> ProcessSignalWindowPostgresRepository
  -> ProcessSignalWindowBuilder
  -> DatasetManifestBuilder
  -> DatasetPostgresRepository
  -> bpi_dataset_process_signal_window_facts
```

fixture 使用拓扑绑定、点位目录、有效校准和 7 条遥测点：

- 流量正常点：10、20、30，均在预测时点前可用。
- 流量迟到点：999，采样时间在窗口内，但 ingest 晚于预测时点。
- 流量冻结后点：888，采样时间在窗口内，但 ingest 晚于快照 freezeAt。
- 泵状态：true、false。

目标查询结果：

| 事实 | 实际 |
|---|---:|
| dataset definitions | 1 |
| snapshots | 1，`MANIFEST_READY/r3` |
| samples | 3 total / 1 included / 2 excluded |
| label leakage | 0 |
| cross-plant rows | 0 |
| process-window facts | 6 total / 2 READY / 4 BLOCKED |
| valid fingerprints/checksums | 6 / 6 |
| flow mean | 20 |
| flow source/accepted/late | 4 / 3 / 1 |
| flow unit/type/calibration mismatches | 0 / 0 / 0 |
| pump true ratio | 0.5 |
| telemetry events/points | 7 / 7 |
| idempotency | 2 COMPLETED，HTTP 200/202 |

均值为 20 而不是 213.4，证明 999 迟到点没有进入特征；`source_point_count=4` 而不是 5，
证明冻结后的 888 点完全没有进入快照事实。低置信度和标签延迟样本没有测点，形成 4 条 BLOCKED
事实，并携带样本不足、最大间隔超限和指标不可计算 blocker；样本因此失败关闭。

直接执行以下更新：

```sql
UPDATE bpi.bpi_dataset_process_signal_window_facts
   SET numeric_value = 999
 WHERE snapshot_id = 'bc14794e-f388-47cd-9265-3c707bf3035c';
```

PostgreSQL 返回 `BPI dataset process signal window facts are immutable`，旧事实未被覆盖。

完整查询为
`deploy/docker/scripts/bpi-dataset-manifest-target-verification.sql`。

## V33 权限补丁

第一次受控部署在 V32 迁移后被最小权限门禁主动停止：新触发器函数继承 PostgreSQL 默认
`PUBLIC EXECUTE`，导致 `bpi_materializer` 间接拥有 BPI 函数执行权。运行容器没有切换。

没有改写已落库的 V32 校验和，而是新增
`V33__bpi_function_execution_privilege_hardening.sql`：

```sql
REVOKE EXECUTE ON ALL FUNCTIONS IN SCHEMA bpi FROM PUBLIC;
ALTER DEFAULT PRIVILEGES IN SCHEMA bpi
    REVOKE EXECUTE ON FUNCTIONS FROM PUBLIC;
```

V33 在现有 V32 库和全新 V1-V33 空库上均通过。materializer、catalog publisher、
retention archiver 和 MLflow registrar 四个专用角色的 provision/grant/verify 全部 PASS。

## 浏览器

- 桌面：`1440x900`，3 个样本、6 条窗口事实，console/page/request errors 均为 0。
- 移动：viewport/body/document 为 `390/390/390`，页面级横向溢出为 0。
- 页面明确显示 `MANIFEST_ONLY`、`NOT_STARTED` 和 `POINT-IN-TIME`。

截图：

- `metadata/bpi-dataset-process-signal-window-desktop-target.png`
- `metadata/bpi-dataset-process-signal-window-mobile-target.png`

## 精确清理

取证后按外键顺序删除本轮 marker。定义、快照、样本、6 条窗口事实、7 条遥测、点位目录、校准、
影子复核、批次、规则、拓扑、审计和幂等等 19 类投影全部为 0。

清理后：

- `bpi-service`、`bpi-adapter`、`bpi-wms-adapter` 均 healthy。
- `/bpi/` 返回 HTTP 200。
- materializer、Polaris、publisher、archiver、MLflow 和 registrar 运行数为 0。
- 模型训练、模型注册、在线推理和生产激活均为 false。

聚合机器证据：
`metadata/bpi-dataset-process-signal-window-acceptance.json`。

## 下一门槛

1. 把受控 fixture 换成 `MapleTcT/iot` 的真实 MQTT 点位身份和来源序列。
2. 对试点产线装载正式测点绑定与有效校准证书。
3. 连续积累至少 200 个复核批次、7 个生产日以及 accepted/rejected 标签覆盖。
4. 使用 V2 readiness policy 重新评估；在 `ELIGIBLE` 之前不启动训练。
