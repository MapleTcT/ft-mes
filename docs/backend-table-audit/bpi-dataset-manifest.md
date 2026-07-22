# BPI 数据集清单与 Parquet 物化落表审计

## 范围

本审计覆盖 Flyway `V26__bpi_dataset_manifest_workbench.sql` 与
`V27__bpi_dataset_parquet_materialization.sql`，包含数据集定义、快照清单、版本锁定 Parquet 请求、
失败恢复、精确对象版本校验、读取、幂等、审计和清理。默认数据库保持 PostgreSQL；Oracle 不参与该
运行路径。结论同时包含本地回归和 `10.11.100.17` 目标 PostgreSQL 15.18/Flyway V27 真实整链。

| 业务动作 | 前端入口 | API endpoint | 后端入口 | 目标表 | 验收 SQL | 实际结果 | 状态 |
|---|---|---|---|---|---|---|---|
| 创建数据集定义 | `/bpi/#/datasets` | `POST /bpi/v1/datasets`；目标网关 `/bpi-api/datasets` | `DatasetController.createDefinition -> DatasetService.createDefinition -> DatasetPostgresRepository.insertDefinition` | `bpi_dataset_definitions`、`bpi_audit_events`、`bpi_api_idempotency` | 按 tenant/code/version 查询 state、revision、checksum、策略、refs 和审计 | 目标 HTTP 200，`ACTIVE/r1`、64 位 checksum；固定 prediction/cutoff/split 策略；幂等记录 `COMPLETED/200` | PASS_TARGET |
| 数据集定义授权失败关闭 | 同上 | 同上 | Spring Method Security + `DatasetService.assertScopes` | 无新增行 | 分别以 viewer 和其他产线 engineer 调用，再查定义计数 | 两次 HTTP 403，定义未创建 | PASS |
| 创建快照任务 | 同上 | `POST /bpi/v1/datasets/{id}/snapshots`；目标网关同构路由 | `DatasetService.createSnapshot -> DatasetPostgresRepository.insertSnapshot` | `bpi_dataset_snapshots`、审计、幂等 | 查询 `state='QUEUED'`、freezeAt、definition checksum、materialization state | 目标 HTTP 202，`QUEUED/r1`；幂等记录 `COMPLETED/202`；本地重放返回相同 snapshotId、未来 freezeAt 为 422 | PASS_TARGET |
| 异步生成清单 | 同上 | worker；随后 `GET /bpi/v1/dataset-snapshots/{id}` | `DatasetManifestProcessor.processOne -> claimPending -> DatasetManifestBuilder.build -> completeManifest` | `bpi_dataset_snapshots`、`bpi_dataset_snapshot_samples`、`bpi_audit_events` | 统计 total/included/excluded/cutoff_safe/leaked；查询排除原因、完成审计及同产线编码跨工厂干扰行 | 目标 `MANIFEST_READY/r3`、`3/1/2/3/0`；低置信、标签延迟、起点越界原因各 1（低置信与起点越界同属一行）；`PLANT-02` 干扰样本 0；定义/快照审计合计 3 | PASS_TARGET |
| 确定性重建 | 同上 | 第二次 POST + worker | 同上 | 同上 | 比较两个 snapshot 的 `manifest_checksum` | 两个不同 snapshotId 的 checksum 完全相同 | PASS |
| 终态不可变 | 不适用 | 直接负向 SQL | `trg_bpi_dataset_snapshot_transition` | `bpi_dataset_snapshots` | 尝试修改 `request_reason` | PostgreSQL 抛出 immutable 异常，终态未变化 | PASS |
| 阶段边界 | 详情抽屉 | GET snapshot | repository projection | `bpi_dataset_snapshots.manifest` | 查询 materialization/artifact 和 manifest phaseBoundary | `MANIFEST_ONLY/NOT_STARTED/null/false/false/false` | PASS |
| marker 清理 | 自动化测试 teardown | SQL DELETE | 本地 test cleanup；目标 `bpi-dataset-manifest-target-cleanup.sql` | 本轮定义、快照、样本及依赖 fixture/audit/idempotency 表 | 按 marker 精确复查十类投影 | definitions/snapshots/samples/shadowRuns/reviews/batches/rules/topologies/catalogs/idempotency 全部为 0 | PASS_TARGET_CLEANED |
| 请求 Parquet 并持久化受控失败 | `/bpi/#/datasets` 清单详情 | `POST /bpi/v1/dataset-snapshots/{id}/materializations`；GET materialization | `DatasetMaterializationService`；Python materializer | `bpi_dataset_materializations`、审计、幂等 | 查询 state/revision/attempt/failure/URI/SHA 与审计序列 | 同一任务 `QUEUED/r1 -> FAILED/r3`，attempt 1、`MATERIALIZATION_ERROR`，URI/SHA null | PASS_TARGET |
| 页面重试并完成版本锁定制品 | 同一失败详情 | `POST /bpi/v1/dataset-materializations/{id}/retry`；GET materialization | service retry；Worker claim/build/upload/readback/complete | materialization、snapshot sample、审计、幂等 | `bpi-dataset-materialization-target-verification.sql` | `READY/r6`、attempt 2、11341 bytes、1 row、26 fields；DB 与 exact-version 对象 SHA 一致 | PASS_TARGET |
| 重启、最小权限和 V27 清理 | READY 详情/运行时 | service restart GET；Worker DELETE；admin version DELETE；cleanup SQL | PostgreSQL repositories；MinIO policy/admin；cleanup | V26/V27 数据集表、审计、幂等和对象版本 | 重启复读；Worker 删除反证；11 类 marker/4 条 idempotency/对象版本/容器/开关终态 | 重启后 READY 不变；Worker `AccessDenied`；数据库与对象版本归零；Worker 0、开关 false | PASS_TARGET_CLEANED |

## 表所有权与约束

| 表 | 所有者 | 关键约束 | 允许动作 |
|---|---|---|---|
| `bpi.bpi_dataset_definitions` | BPI service | tenant+code+version 唯一；策略固定；refs 非空；UPDATE 触发器拒绝 | SELECT、INSERT |
| `bpi.bpi_dataset_snapshots` | BPI service/worker | tenant FK；版本唯一；状态约束；终态不可变；artifact 永远 null | SELECT、INSERT、受控 UPDATE |
| `bpi.bpi_dataset_snapshot_samples` | BPI worker | tenant 复合 FK；快照+review 主键；included/reason 一致；cutoff=prediction time；UPDATE 拒绝 | SELECT、INSERT |
| `bpi.bpi_dataset_materializations` | BPI service/Python materializer | tenant+snapshot+format 唯一活动任务；revision/attempt/state 转移受控；READY 要求 URI、versionId、SHA、bytes/rows/schema 完整；FAILED 不得伪留制品事实 | SELECT、INSERT、受控 UPDATE |

## 目标验收标识

- marker：`ADP_E2E_BPI_DATASET_TARGET_20260722_055000_A7C4`
- definition：`adc587a0-117e-4b4b-834c-94ae4b4df39d`，`ACTIVE/r1`
- snapshot：`522f11a1-9b51-4b38-99ce-d0f49aca6139`，`MANIFEST_READY/r3`
- manifest checksum：`4877203f7a9f3b306c0c65d2cc396b548ed13ef85cb275874aa74d4ca227e043`
- 目标验证 SQL：`deploy/docker/scripts/bpi-dataset-manifest-target-verification.sql`
- 目标清理 SQL：`deploy/docker/scripts/bpi-dataset-manifest-target-cleanup.sql`

V27 物化验收：

- marker：`ADP_E2E_BPI_PARQUET_20260722_105844_A1`
- snapshot：`117f0045-cf03-4177-8010-dc730c566f13`
- materialization：`02a0c765-c0f9-4c1b-a6cc-1e2dc2ca5983`，`READY/r6`，attempt `2`
- content SHA-256：`8837c21dd9a5ac181bd86d09e22e43421e0c9420fca943a1f765b614059df126`
- object versionId：`28b5a178-d972-4e5b-9c6f-c3ece7bb0838`
- 目标验证 SQL：`deploy/docker/scripts/bpi-dataset-materialization-target-verification.sql`
- 机器证据：`metadata/bpi-dataset-materialization-acceptance.json`

## 验收命令

```bash
BPI_TEST_DATABASE_URL=jdbc:postgresql://127.0.0.1:55441/ft_mes_bpi_v26_dataset_20260722_0414 \
BPI_TEST_DATABASE_USER=bpi_test \
BPI_TEST_DATABASE_PASSWORD='***' \
/private/tmp/adp-tools/apache-maven-3.9.9/bin/mvn -q -pl app -am \
  -Dtest=BpiDatasetManifestPostgresAcceptanceTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

口令不写入仓库。测试使用 Java 17；浏览器、模拟器和 Java 8 Adapter 由各自套件独立验收。
目标浏览器 runner 为 `deploy/docker/scripts/adp-bpi-dataset-manifest-target-acceptance.js`，fixture 为
`deploy/docker/scripts/bpi-dataset-manifest-target-fixture.sql`。目标输出先由 runner 标记为
`PASS_PENDING_DATABASE_VERIFICATION_AND_CLEANUP`，只有数据库验证和十类零残留清理都通过后，组合结论才是
`PASS_TARGET_BROWSER_API_POSTGRES_MANIFEST_ONLY_CLEANED`。

V27 runner 为 `deploy/docker/scripts/adp-bpi-dataset-materialization-target-acceptance.js`，可分别执行
`request-failure`、`retry-ready` 和 `read-ready`。V27 组合结论必须同时满足页面无非预期错误、PostgreSQL
状态/审计/幂等一致、按 MinIO `versionId` 回读一致、服务重启后可读、Worker 删除被拒绝、管理员只删除
测试版本、数据库和对象零残留、Worker 归零且默认开关保持 false；否则不得标记通过。
