# BPI 数据集 Object Lock 恢复包落表审计

## 范围

本审计覆盖 Flyway `V29__bpi_dataset_object_lock_recovery_archive.sql`、真实 ADP 页面、Java 8 adapter、Java 17 service、Python archiver、PostgreSQL 15.18、MinIO Object Lock、Polaris/Iceberg 恢复演练和精确清理。默认运行数据库仍为 PostgreSQL；Oracle 不参与此链路。

| 业务动作 | 前端入口 | API endpoint | 后端入口 | 目标表 | 验收 SQL | 实际结果 | 状态 |
|---|---|---|---|---|---|---|---|
| 请求恢复包 | `/bpi/#/datasets` | `POST /bpi/v1/dataset-catalog-publications/{publicationId}/retention-archives` | `DatasetRetentionArchiveController -> DatasetRetentionArchiveService -> DatasetRetentionArchivePostgresRepository.insert` | `bpi_dataset_retention_archives`、`bpi_audit_events`、`bpi_api_idempotency` | 按 tenant/publication/id 查询 state/revision/attempt；核对审计和幂等 | HTTP 202，`QUEUED/r1`，同 publication/archiver/profile 只产生一个活动任务 | PASS_TARGET |
| Worker 失败持久化 | 同一详情 | archive GET | `RetentionArchiverWorker -> RetentionArchiveRepository.fail` | archive、audit | 查询 failure_code/detail、归档对象字段和 attempt | `FAILED/r3/attempt1/RETENTION_ARCHIVE_ERROR`；未伪留 LOCKED 事实 | PASS_TARGET |
| 页面重试并锁定 | 同一失败详情 | `POST /bpi/v1/dataset-retention-archives/{archiveId}/retry` | service revision/fencing -> repository.retry；worker claim/archive/verify/complete | archive、audit、idempotency | 查询 r1-r7 状态序列、两个 `COMPLETED/202`、对象版本和 retention | 同一 archive ID 达到 `LOCKED/r7/attempt2`，两对象 exact version/readback/retention 全部匹配 | PASS_TARGET |
| Object Lock 权限反证 | 不适用 | S3 stat/get/delete by versionId | MinIO policy | 不落 PostgreSQL 新业务行 | exact version 可读；archiver delete；无 bypass admin delete；recovery operator 越权 list/delete | 两 retained 版本可读；删除分别为 `AccessDenied`/`InvalidRequest`；业务 warehouse list 为 `AccessDenied` | PASS_TARGET_SECURITY |
| 恢复与销毁演练 | 不适用 | Polaris Iceberg REST + PyIceberg + S3 exact-version | `recovery_rehearsal.py` | Polaris metastore；独立 recovery warehouse | 比较 original/recovery snapshot、row count、semantic checksum；复查 purge 后 table/namespace/object versions | 恢复 snapshot `4888963949559974798` 为 1 row、checksum 一致；恢复表/namespace 和 6 个物理版本归零；原 training table 未改变 | PASS_TARGET_RECOVERY |
| marker 和运行态清理 | 不适用 | cleanup SQL、Polaris/MinIO 精确管理操作、Compose 停侧车 | V26-V29 lineage、audit、idempotency 和 fixture 表 | 全 marker 投影、对象版本、table/namespace、容器数和开关 | 所有测试投影和对象为 0，主 BPI 三服务健康，九个危险/可选开关 false | PASS_TARGET_CLEANED |

## 表所有权与约束

| 表 | 所有者 | 关键约束 | 允许动作 |
| --- | --- | --- | --- |
| `bpi.bpi_dataset_retention_archives` | BPI service / retention archiver | tenant+publication+archiver+profile 唯一活动任务；严格 revision/state/attempt fencing；`LOCKED` 必须具备 source/manifest exact version、Object Lock、row/checksum 和 recovery verification；终态事实不可任意回退 | service 负责 INSERT/retry/read，archiver 只做受控 claim/fail/complete |
| `bpi.bpi_audit_events` | BPI service / archiver | 每次业务 revision 对应明确事件；tenant/scope 不可跨越 | 追加、查询 |
| `bpi.bpi_api_idempotency` | BPI service | tenant+actor+method+path+key 约束；完成响应可重放 | 插入、完成、读取 |

Object Lock bucket 不是业务数据库表，但 `archive_bucket`、两个 exact `version_id`、manifest SHA、retention mode/until、对象数/字节数和验证元数据必须先持久化并对账，任务才能进入 `LOCKED`。

## 目标验收标识

- marker：`ADP_E2E_BPI_ARCHIVE_20260722_215300_A1`
- runtime revision：`ef8036b8b71718f3bb4f65ede3e9ba9cca093a82`
- archive：`c2d585f4-5793-4f17-a230-aa98440d3293`
- publication：`ae56f35f-013e-4973-bd5a-98b1ab4ef8e4`
- original snapshot：`2413939455193407789`
- recovery snapshot：`4888963949559974798`
- 机器证据：`metadata/bpi-dataset-retention-archive-acceptance.json`
- 完整报告：`docs/testing/bpi-dataset-retention-archive-acceptance.md`

## 验收边界

本报告证明单数据集恢复包的软件合同、最小权限、Object Lock 和隔离恢复/销毁闭环，不证明整站灾备、生产 RPO/RTO、跨故障域复制、容量或模型投产。相关开关必须继续默认关闭。
