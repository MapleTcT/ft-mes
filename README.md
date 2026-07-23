# FT MES 与智能批次工艺数据中心

这是一个从 Windows ADP/MES 交付资产恢复、面向 Linux/Docker 和 PostgreSQL 持续演进的工程仓库，同时包含新建的智能批次与工艺数据中心（BPI）。仓库的目标不是让旧运行包“勉强启动”，而是逐步形成可编译、可测试、可部署、可落库验收、可回滚的 MES 产品代码基线。

> **当前总状态：`IN_PROGRESS_NOT_COMPLETE`。** 唯一测试栈 `adp-mes-newbase` 的 BPI PostgreSQL、Java 17 service、Java 8 adapter 和 Web 已 expand-only 到 Flyway V34 / revision `8c9c4192b179`。受控 MQTT/WOM START-END、影子批次、QCS 质量放行、内部 material-wms 蓝/红单，以及 V26 manifest、V27 精确版本 Parquet、V28 Iceberg/Polaris、V29 Object Lock 恢复包、V30 MLflow Dataset Input、V31 离线训练就绪评估、V32/V33 工艺信号窗口/权限加固和 V34 遥测窗口落表均有真实页面、API 和 PostgreSQL 等外部系统证据。marker `BPI_TLANDING_20260723_094606` 已证明受控 MQTT 经 JetLinks、Kafka、精确 scope consumer 落为窗口内 2 events/2 points，并在影子页面显示五项 `1/1`、event/observation `2/2` 和零拒绝/GAP/乱序；预热数据被正确排除，取证后 marker 清零、IoT 默认配置恢复。该结果仍不是物理设备和正式计量证书。真实物理点位与正式校准、200 个复核批次、7 个生产日、模型训练/注册/审批/推断、MLflow 生产安全与高可用、整站灾备、生产 RPO/RTO/容量、连续 7-14 天现场运行、外部 ERP/WMS 和生产迁移条件仍未完成，因此不能标记生产 READY。Phase 2/WMS、materializer、Polaris、catalog publisher、retention archiver、MLflow/registrar 及危险管理开关保持默认关闭。

## 项目定位

仓库承载两条边界明确、逐步集成的产品线：

| 产品线 | 目标 | 当前边界 |
|---|---|---|
| ADP/MES 恢复与 PostgreSQL 迁移 | 把恢复前端、后端、配置和运行资产提升为可维护工程，逐步闭合生产、质量、仓储等业务 | 恢复资产不等于原厂完整源码；每项功能必须重新做页面、API 和落库验收 |
| 智能批次与工艺数据中心（BPI） | 连接数采、生产上下文、工艺规则、质量和物料系统，自动识别批次边界并形成可追溯生产事实 | Phase 1 只生成影子批次，不直接改写 WOM、QCS 或 WMS 生产状态 |

默认数据库是 **PostgreSQL**。Oracle 只允许存在于显式 `legacy-template-only` 模板、迁移对照和 backlog 中，不允许重新成为默认运行路径。

## 当前完成度

| 范围 | 状态 | 已有证据 | 继续完成的门槛 |
|---|---|---|---|
| 可持续开发仓库 | `READY` | 根父 POM、源码模块边界、CI、Compose、依赖/文件库存和 PostgreSQL-first 门禁 | 新模块持续补测试、迁移和库存 |
| 既有 ADP/MES 平台 | `PARTIAL` | 登录、组织、权限、菜单及部分生产/质量功能有真实页面和 PostgreSQL marker 证据 | 生产矩阵仍有阻断项，业务链尚未全部闭合 |
| WOM 可见新建指令单 | `PASS` | `wom-production-entry` 源码模块、列表入口、PostgreSQL 参照、迁移 189、marker `ADP_E2E_20260717101030_WOM_MANUAL_ENTRY` 的 9/9 浏览器/API/落库验收 | public `produceTaskCreated` 是否开放仍需产品范围决定；不影响受控可见入口 |
| RM 批控配方 Web 编辑 | `PASS_WITH_EXTERNAL_DCS_BLOCKED` | `rm-formula-editor` 源码模块、迁移 190、可见 `Web编辑` 入口；marker `ADP_E2E_20260717120436_RM_WEB_FORMULA` 连续三轮完成桌面/移动页面、API、六表 PostgreSQL 回读、失败重试和清理 | 配置真实现场 Batch/DCS HTTPS 端点，加载生产主数据并完成投递确认与回滚签字 |
| PATROL 共享巡检 | `TARGET_HIDDEN_DANGER_PASS_PARTIAL` | 455 个 Java 文件构建 PASS；目标 37 表、24 菜单、102 操作、2 工作流验收 PASS；EamMs JAR SHA `af01d6a7...97f753`；异常隐患 marker `ADP_E2E_20260717003024_PATROL_HIDDEN_DANGER` 为 45/45 PASS，明细关联、幂等和 EAM 来源“巡检”复显均有证据 | 继续统计监控；完整隐患治理需真实 SESH；目标回滚需维护窗口确认 |
| EMS 能源管理 | `BLOCKED_MISSING_INDICATOR` | `supEMS`、`energyPlan`、`EnergyConBase`、`EnergyPred` 四个源码包和依赖关系已恢复 | 取得 Indicator `6.0.4.0` api/core，补 PostgreSQL 迁移，逐服务构建与验收 |
| BPI 产品链 | `PARTIAL` | Phase 1 影子批次、数据质量、规则治理和真实 WOM context 已验收；内部 QCS/WMS 蓝红链与正式身份职责分离已在目标闭合；V26-V34 已闭合 manifest、Parquet、Iceberg、Object Lock、MLflow Dataset Input、失败关闭的训练就绪评估、受控工艺信号窗口、权限加固和受控 MQTT 窗口落表 | 用物理 DEVICE/GATEWAY、正式校准、200 个复核批次和 7 个生产日替换受控来源；外部 ERP/WMS、整站灾备/容量、MLflow 生产安全与 Phase 4 模型仍未完成 |
| 目标测试环境 | `PASS_CONTROLLED_BPI_V34_TELEMETRY_LANDING_DEFAULT_OFF` | 受控 QCS/WMS、V25 蓝红 durable receipt、V26-V34 数据集/遥测链、19 门槛评估、窗口事实、MQTT -> PostgreSQL -> 页面、最小权限、幂等、零模型副作用和精确清理均闭合 | V34 只关闭受控单点短窗口落表；物理设备、正式校准、连续运行、容量和生产写回仍未完成 |
| 生产迁移 | `BLOCKED` | 迁移、回滚和签字门禁已经建立 | 数据、MinIO、Keycloak、TLS、安全、license、回滚演练和业务签字均需 READY |

权威状态以 [项目总目标验收总账](docs/project-goal-acceptance.md)、[目标缺口总账](docs/goal-gap-register.md)、[模块包缺口审计](docs/module-package-gap-audit.md)、[PATROL 恢复验收](docs/testing/patrol-module-recovery-acceptance.md) 和 [机器可读目标账本](metadata/project-goal-acceptance.json) 为准。当前模块包审计确认 PATROL 已从“部署中”进入“异常发现到 EAM 待治理台账 PASS、统计和完整 SESH 治理继续验收”；四个 EMS 源码包已恢复，但 `Indicator 6.0.4.0`、`packConfigManag`、`SESGISConfig` 仍是依赖缺口；WMS 与 ProcessAnalysis 已由可维护自研模块接续。README 是接手入口，不替代验收证据。

## 当前开发主线

当前以 BPI Phase 1 可信批次事实、Phase 3A 可复现数据集清单、Phase 3B-A 版本锁定 Parquet 制品、
Phase 3B-B 可核验 Iceberg/Object Lock 恢复包、Phase 3C-A MLflow Dataset Input、Phase 3C-B
离线训练就绪评估、Phase 3C-C 工艺信号窗口以及 Phase 3C-D/E 现场覆盖和遥测落表为同一条开发主线：

```text
JetLinks/IoT 测点 + MES 生产指令/生产上下文
                    |
                    v
            Kafka 版本化事件契约
                    |
                    v
       Flink 事件时间 + 规则 Broadcast State
                    |
                    v
       批次边界候选 + 数据质量 + 双运行回执
                    |
                    v
      BPI PostgreSQL 候选/批次/证据/审计
                    |
                    v
             人工确认与影子批次
                    |
                    v
       QCS/WMS 幂等联动与批次谱系
                    |
                    v
       时间点数据集清单（不可变 manifest）
                    |
                    v
       版本锁定 Parquet（目标已验收）
                    |
                    v
 Iceberg v2 + Polaris catalog（目标页面/fencing 已验收）
                    |
                    v
 Object Lock 单数据集恢复包（目标恢复/销毁演练已验收）
                    |
                    v
 MLflow Dataset Input（目标失败/重试/重启已验收）
                    |
                    v
 point-in-time 工艺信号窗口（受控目标已验收）
                    |
                    v
 MQTT/JetLinks/Kafka 遥测落 PostgreSQL 并按影子窗口覆盖（受控目标已验收）
                    |
                    v
 离线训练就绪评估（目标真实 BLOCKED；不启动训练）
```

MES 目标业务链保持为：

```text
制造指令 -> 投料/报工 -> 请检 -> 合格/不合格处置 -> 完工入库 -> 批次追溯
```

BPI Phase 1 只有在选定产线连续运行 7-14 天，并通过边界人工认同率、累计量偏差和数据质量门槛后，才允许进入 QCS/WMS 生产写回阶段。

当前最短交付主线只有一条：

```text
浏览器发布规则 -> 当前点位目录重验 -> PostgreSQL outbox -> Kafka -> Flink checkpoint
-> 独立 application receipt + runtime readiness receipt -> PostgreSQL APPLIED/READY/audit -> IoT/MES context
-> candidate -> 浏览器确认 -> batch/evidence/audit
```

这条链已使用唯一 marker 在目标环境以受控来源闭合。`MapleTcT/iot` 保留已验收的 JetLinks EventBus 链，并新增正式 MQTT 3.1.1/QoS1 受控入口、exporter、Kafka offset、JetLinks PostgreSQL 和 Flink source 证据；它从 JetLinks 权威注册/metadata 生成内容寻址点位快照和来源序列证据，经 Kafka 自动落入 BPI PostgreSQL。当前版本只有精确匹配目录绑定、来源为 DEVICE/GATEWAY、同 epoch 至少两次严格递增且证据未过期时才可进入 READY。当前仓库也已补 WOM PostgreSQL 同事务触发捕获、显式 scope/state 映射和 Java 8 Kafka 发布器。marker `BPI_LIVE_20260720_123058` 已把受控 MQTT 与真实 WOM context 汇合为同一 START/END 影子批次；下一步是用物理设备和正式证书重复同一链，而不是把本次测试专用证据升级为现场结论。

## 已实现的 BPI 能力

- Java 8 旧平台认证适配器与 Java 17 BPI 服务边界。
- OpenAPI、Protobuf 事件契约、兼容性基线和契约门禁。
- `MapleTcT/iot` 已实现 JetLinks 解码后遥测 exporter、权威点位目录 publisher、来源序列证据 publisher 和受控 MQTT 接入：显式设备/测点映射、稳定身份、Redis 持久资格状态、磁盘缓冲、Kafka 幂等发送、内容寻址事件/目录 revision、Micrometer 指标和失败关闭。目标机两次独立 QoS1 会话已证明 `2026072003:5001..5003` 与重连后的 `2026072004:1..3`；六条 PUBACK、JetLinks 双表、Kafka `37 -> 43`、MES current/DQ 六条原始证据均已核对。只有 DEVICE/GATEWAY 同一 epoch 至少两次严格递增、绑定指纹匹配且证据未过期时才允许 QUALIFIED。
- MES production context outbox 已实现 `176` 同事务捕获和 `177` 版本时钟下限、显式产线/状态映射、`BLOCKED_*` 失败关闭、Java 8 `SKIP LOCKED` 抢占、Kafka 幂等发送、重试/毒消息终止和 Micrometer 指标；目标机已通过真实 WOM `start/stop`、waitforrun/runing/finished 三条 `SENT|1` 上下文、Flink join 和 START/END 影子批次确认。启用脚本现在强制要求 `runing=active` 与 `finished=inactive` 两个映射同时存在。
- PostgreSQL Flyway schema、遥测入库、规则/拓扑、回放模拟、候选确认、影子批次、证据和审计。
- 拓扑/规则产品化：页面可新建或复制版本，拓扑发布前校验路径、环、JetLinks 产品/设备/属性、单位、校准和必需信号；独立管理员发布后版本不可变，规则草稿只能引用已发布拓扑及其绑定信号。Flyway V1-V9、真实 PostgreSQL marker 和 7 条浏览器 E2E 已通过；目标环境 marker `ADP_E2E_20260715_004849_BPI_PRODUCT_TARGET` 又验证了真实 ADP 会话、V9 落库、创建人发布拒绝、独立发布和服务重启后读取。
- 点位目录与校准准入：Flyway V10-V12 保存不可变来源快照和源属性/规范属性身份；V17-V18 把来源校准声明与 MES 证据分开并增加稳定游标；V22 新增来源序列 current evidence、目录 origin/fingerprint 和精确 READY 联查。真实点位页已用测试校准闭合提交、同人审批 `422`、独立审批、1 READY、规则退役联锁和撤销；验收后目录恢复 `pilot-unverified-20260714`、0 READY。测试校准不等于现场计量证书，来源 TTL 到期后仍会失败关闭。
- 规则运行时目录准入：规则发布 Protobuf 固化 `productId` 和 `calibrationVersion`，Java 服务在发布事务内重验当前目录；Flink 订阅 `iot.point-catalog.snapshot.v1`，仅把全部绑定 READY 的规则 UPSERT 到 evaluator。目录降级会 DELETE 规则并清空待决窗口，旧 timer 不再产候选，恢复后从新观测重新累积。控制面 `APPLIED/REJECTED` 与运行时 `READY/DEGRADED/INACTIVE` 已形成独立 Protobuf、Flink sink、Kafka source/DLQ、Flyway V15 字段和 UI 状态列；目标环境已完成 savepoint 有状态升级、规则 `READY -> INACTIVE`、回滚草稿及延迟候选落库复验。
- 规则发布 transactional outbox、Kafka 投递状态、失败重试、乐观并发、规则应用回执和独立运行时就绪回执。
- Flink 事件时间、生产上下文 join、规则生命周期、索引路由、边界计算和三个事务 sink。
- BPI 操作台、确定性模拟服务和 20 条浏览器 E2E；页面保持 `APPLIED` 时可独立显示 `DEGRADED -> READY`，点位页分栏显示来源声明和可增量加载的 MES 校准证据，影子验收页支持任务创建、启动、批次复核、结束评估、拒绝/取消和独立批准。批次详情支持受控两步强制结束，并通过 `getBatchRelease` 独立展示质量门、必检明细、WMS 命令/幂等键、拒绝详情和 durable 单据；数据集页支持受控定义、时间点冻结、清单轮询、排除原因和阶段边界；质量/WMS 慢响应或 503 不遮蔽核心批次事实，并有桌面/移动端和关闭竞态验收。
- 数据质量事件工作台已完成 Flyway V19、Kafka 严格准入/DLQ、PostgreSQL 聚合与不可变原始证据、HMAC keyset 分页、分派/重新分派/解决状态机、Java 8 适配器和桌面/移动页面。本地真实 PostgreSQL + Embedded Kafka 为 6/6、适配器完整模块为 18/18、模拟器为 9/9、浏览器为 13/13；目标 marker `ADP_E2E_DQ_20260719_215100_297E0AAF` 又完成真实 ADP 登录、Kafka partition 2 offset `5 -> 6`、`OPEN/r1 -> ACKNOWLEDGED/r2 -> RESOLVED/r3`、raw fact 保留、API 全 2xx、浏览器零错误、六类 PostgreSQL 记录定向清零和 consumer deny-all 恢复，状态为 `PASS_TARGET_POSTGRES_KAFKA_BROWSER_CLEANUP`。
- 影子运行验收已完成 Flyway V20、规则/拓扑/点位目录固定、9 项启动准入、7-14 天和最少样本配置、人工边界/参考量复核、关键数据质量阻断、职责分离批准、幂等和乐观并发。目标 marker `ADP_E2E_SHADOW_20260720_0152_V20` 以 10 个 `CLOSED_RAW` 批次达到 19/20 边界认同和累计量偏差 0%，最终 `APPROVED/r14`；57 个浏览器 BPI 响应无错误，16 条审计和 10 条成功幂等准确，WOM/QCS/WMS 状态未改变，marker 清理为 0。8 天为受控时间压缩，现场连续运行仍待执行。
- 分层运行开关和旧 MES 原生菜单门禁已完成 Flyway V21。提交 `df6fdb0e5ddb929626dd0ea3c81b170afbaa62a4` 让 Java 8 adapter 在旧菜单读取点真实执行 `bpi.ui`，真实页面闭合 ENABLE/DISABLE/INHERIT、菜单 `28/0 -> 29/1 -> 28/0`、iframe 进入、PostgreSQL `1/3/3 -> 0/0/0` marker 清理和 adapter 故障时 gateway 回退；最终测试环境保留 LINE active/enabled r1，生产写入仍为 0。
- QCS 质量放行与 WMS 完工入库合同已在提交 `22ddadebd20ed9ed5d7efd19c3c0ed49967b9c90` 形成默认关闭的 Phase 2 纵切：Flyway V23、revisioned required-inspection snapshot、`CLOSED_RAW -> WAIT_QA -> RELEASED/REJECTED`、确定性 WMS outbox、PUBLISHED 回执门禁、durable `documentId -> INBOUNDED`、未知回执状态 fail-closed、影子批次数据库触发器和租户复合外键。本地干净 PostgreSQL 16.13 从 V1-V23 迁移后 4/4 API/SQL 验收通过。提交 `94e2b2288bf52966be58e9dda97039a5455466a8` 实现产品页和六态浏览器验收；目标 V23 合同基线又完成 4 个 marker、4/4 测试、12 表零残留和重启读取。实现提交 `1ce3cb996ff81556763283a5401f7c19554099c2` 新增 material 迁移 192、受 API key 保护的精确幂等查单、query-first WMS adapter、单位/sourceSystem 传播和 Compose 编排。目标受控 QCS marker 随后闭合 Kafka command/receipt、material 单据/明细/事务/库存、`INBOUNDED/r4`、相同 QCS 重放、强制 Kafka 重放、真实页面五个 200 API 和零残留清理；前端提交 `ad36372936d99ca947d231fc552ae9c3e086c2cc` 修复空证据纵向挤压。外部 QCS 主动事件和外部 ERP/WMS 补偿仍未验收，全部开关保持关闭。
- 受控强制结束已完成 Flyway V24、待审批任务、职责分离、批准边界、审计/幂等和真实页面。marker `ADP_E2E_20260721053253_BPI_FORCE_CLOSE` 的申请使 batch 保持 `ACTIVE/r2`、task 进入 `PENDING_APPROVAL/r1`；同一申请人批准返回 403，不同 `BPI_ADMIN` 批准后为 `CLOSED_RAW/r3 + COMPLETED/r2`。Java 8 adapter 精确 GET/POST 路由修复通过 31/31，真实 PostgreSQL 证明 QCS/WMS/outbox 为 0，marker 清理为 0。
- 完工入库冲销已完成 Flyway V25、正式 ADP 双身份四眼审批、追加式红单、Kafka command/receipt、内部 material-wms 双单据和净库存归零；目标 marker `ADP_BPI_FORMAL_WMS_REVERSAL_20260721190630` 的 BPI/material 双库、身份和隔离 Kafka 残留均清理为 0。该结论只覆盖内部 material-wms，不替代外部 ERP/WMS 联调。
- Phase 3A 数据集清单已完成 Flyway V26、受控特征/标签字典、批准影子复核来源、时间点 cutoff、置信度/标签延迟排除、确定性 checksum、异步 worker、终态不可变、OpenAPI、Java 8 adapter 和桌面/移动页面。目标 marker `ADP_E2E_BPI_DATASET_TARGET_20260722_055000_A7C4` 从真实页面创建 definition、排队 snapshot 并进入 `MANIFEST_READY/r3`；PostgreSQL 为 `3 total / 1 included / 2 excluded / 3 cutoff-safe / 0 leaked / 0 cross-plant`，十类 marker 残留为 0。当前明确停在 `MANIFEST_ONLY`，不声明 Iceberg、MLflow 或模型已就绪。
- Phase 3B-A 已完成 Flyway V27、Java 17 任务 API、Java 8 精确路由、独立 Python 3.12 materializer、PyArrow 固定 schema/稳定排序、MinIO 私有版本桶、内容寻址对象键、精确 `versionId` URI、上传后下载复算 SHA-256、单副本领取/fencing 和默认关闭编排。目标 marker `ADP_E2E_BPI_PARQUET_20260722_105844_A1` 又闭合真实页面 `FAILED -> retry -> READY`、精确对象校验、服务重启、最小权限和定向清理；不可变 manifest 仍与物化投影分离。
- Phase 3B-B 已完成 Flyway V28、四个 scoped/idempotent catalog API、Java 8 精确路由、独立 Python publisher、Apache Polaris 1.4.1 持久 metastore、PyIceberg 0.11.1、独立私有 warehouse 和默认关闭编排。clean release `b7356aa07496` 的三组目标 marker 已闭合真实桌面/移动发布、受控失败与同任务重试、真实 catalog commit 后 exit 86、stale-claim 恢复到同一单一 snapshot、最小权限和精确清理；浏览器错误为 0，七个目录开关恢复 false。
- V29 Object Lock 恢复包已完成真实页面请求、archiver 受控失败、同 archive 页面重试到 `LOCKED/r7/attempt2`、两个 GOVERNANCE exact versions、PostgreSQL r1-r7/幂等、archiver/recovery operator 最小权限，以及隔离 Polaris snapshot `4888963949559974798` 的 1-row time-travel 恢复。恢复 table/namespace 和 6 个 recovery warehouse 版本被物理清除，原 training snapshot `2413939455193407789` 保持不变；marker、四类对象版本和 sidecar 均归零，九个相关开关为 false。该结论不替代整站灾备、生产留存政策、MLflow 或模型投产。
- Phase 3C-A 已完成 Flyway V30、四个 scoped/idempotent API、Java 8 精确路由、独立 MLflow `3.14.0` PostgreSQL backend、metadata-only Dataset Input registrar、私有 artifact bucket 和默认关闭编排。目标 marker `ADP_E2E_BPI_MLFLOW_20260723_022000_A1` 在 Tracking Server 停止时真实进入 `FAILED/r3/attempt1/MLFLOW_TRANSPORT_ERROR` 且 MLflow 保持 0 run/0 input；恢复后页面重试同一 registration 到 `REGISTERED/r6/attempt2`，MLflow 恰有 1 run/1 dataset/1 input，重启后仍为 1。精确 versioned S3 source、PostgreSQL 六态审计/幂等、MinIO 最小权限、桌面/移动零错误和定向清理均通过；模型相关表为 0，所有训练/注册/推断/激活标志为 false。
- Phase 3C-B 已完成 Flyway V31、三个 scoped/idempotent API、Java 8 精确路由、19 门槛确定性服务、不可变 sequence/checksum、桌面/移动评估抽屉和默认不训练边界。目标 marker `ADP_E2E_BPI_READINESS_20260723_091500_A1` 从真实页面得到两次 `BLOCKED/r1` 评估，序号 1/2、19 gates、8 blockers、同 checksum、2 audit、2 COMPLETED/200；相同 key 重放不增加行，UPDATE 被 trigger 拒绝，service/adapter 重启后可重发现。MLflow 评估前后均为 1 run/1 dataset/1 input，三类模型表均为 0；marker、Polaris/MinIO 对象、临时 MLflow 卷和 sidecar 已精确归零。该 v1 评估当时缺少窗口；后续 V32/V33 只关闭受控窗口实现，仍不能降低真实样本和现场证据门槛。
- Phase 3C-C 已完成 Flyway V32 工艺信号窗口事实和 V33 函数权限加固。第一次 V32 升级被最小权限门禁正确停止且未切换运行容器；追加 V33 后现有库、全新 V1-V33 库和四个 worker 角色校验均通过。目标 marker `ADP_E2E_BPI_WINDOWS_20260723_1235_A1` 从真实页面创建流量 MEAN 与泵 TRUE_RATIO 两组窗口，3 个样本形成 6 条不可变事实（2 READY / 4 BLOCKED）；流量 `source=4/accepted=3/late=1/mean=20`，冻结后值不入快照，泵运行比例为 0.5。桌面/移动零错误，19 类 marker 投影归零，主三服务 healthy、可选 sidecar 为 0。该受控结果只关闭窗口实现与失败关闭；下一步是接入真实 `MapleTcT/iot` identity、正式校准并积累 200 批/7 天后重跑 v2 readiness，不启动模型。
- Phase 3C-D/E 已完成现场来源/数据量覆盖投影和 Flyway V34 遥测窗口索引。目标 marker `BPI_TLANDING_20260723_094606` 以受控 MQTT QoS1 经 JetLinks、Kafka 和 default-off 精确 scope consumer 落入 PostgreSQL；3 条预热事件被影子运行窗口排除，sequence `4,5` 形成 2 events/2 points/0 rejects，页面显示五项 `1/1`、event/observation `2/2`、gap/out-of-order `0/0`。29 个 BPI 请求全为 2xx，桌面/390px 移动错误为 0，取消后 marker 清零并恢复 5m/10m、未验证校准和 UNCERTAIN。该证据只关闭受控落表纵切，不声明物理设备、正式计量、现场容量或模型资格。
- Kafka + PostgreSQL 回执消费验收：`read_committed`、回滚不可见、重启重放、`DEGRADED -> READY` 落库、旧事件抑制、精确幂等和双 source DLQ。
- Kafka 4.2 + Flink 2.2.1 MiniCluster 验收：成功 checkpoint 后 `APPLIED + READY` 可见、未完成事务不可见、停用提交 `APPLIED + INACTIVE`、TaskManager 重启恢复规则终态、同版本规则禁止重新启用且两类回执无重复。
- 目标测试环境独立 BPI 运行栈：真实 ADP `suposTicket` 经可信网关校验，Java 8 适配器签发短期内部 JWT，Java 17 服务读取独立 PostgreSQL。
- 目标测试环境独立流处理栈：三 broker Kafka、十个 BPI topic、Flink 2.2.1、两个 TaskManager、MinIO checkpoint、唯一 marker 回放和带负载 TaskManager 重启恢复。
- 目标环境受控联合验收：marker `BPI_LIVE_20260720_123058` 从真实 WOM start、三条 context outbox、受控 MQTT/JetLinks、Kafka/Flink `APPLIED+READY` 进入 START/END 两条候选；真实 BPI 页面确认后，同一批次从 `ACTIVE/r1` 进入 `CLOSED_RAW/r2`。验收后两条规则 `RETIRED/INACTIVE`、测试校准 `REVOKED`、命令开关恢复继承、WOM finished context 为 inactive/SENT，Flink 保持 `RUNNING 36/36`。
- 非 HTTPS 测试入口写命令兼容：浏览器不支持 `crypto.randomUUID()` 时改用 `crypto.getRandomValues()` 生成 UUID v4，并有 E2E 覆盖。

本地 MiniCluster、目标流处理集群、早期联合写链以及本轮 MQTT/WOM 联合链是不同证据等级。本轮已经把 JetLinks source 与真实 WOM context 用同一 marker 汇合到 START/END candidate 和 CLOSED_RAW batch，但来源仍是受控模拟器，不能升级为物理设备现场闭环。详细 marker、目标表、SQL、缺陷修复和恢复结果记录在 `docs/testing/bpi-live-mqtt-wom-start-end-acceptance.md`。

## 目标测试环境（更新至 2026-07-23）

当前 ADP/PATROL 运维与验收入口为公司内网 `10.11.100.17`。运行面只保留一个 ADP Compose project：`adp-mes-newbase`；BPI service、adapter、Web 和 PostgreSQL 已并入该 Compose，Kafka/Flink/MinIO 仍由隔离侧车栈承载，不是第二套 ADP。BPI PostgreSQL 当前为 PostgreSQL 15.18/Flyway V34，业务 service、adapter 和 WMS adapter 均为 revision `8c9c4192b17953c48208efd31ef6528de04d96c6`。遥测 consumer 只在显式 allowlist 下运行，HTTP ingress 仍默认关闭；Polaris、materializer、publisher、retention archiver、MLflow 和 registrar 仍只在受保护验收窗口临时启动。V34 取证后 marker 为 0，IoT 试点映射恢复未验证/UNCERTAIN，模型和生产开关保持 false；Compose project 只引用正式 `docker-compose.yml`。Flink REST 仅绑定测试机 Tailscale 地址 `100.99.133.43:18081`，不作为业务前端入口。

| 入口/运行面 | 地址或项目 | 当前结果 |
|---|---|---|
| 既有 ADP/MES + PATROL | `http://10.11.100.17:18080` | 当前公司内网入口；PATROL 配置、任务执行、异常结果、待治理隐患生成和 EAM 台账复显链 PASS |
| BPI 操作台 | `http://10.11.100.17:18080/bpi/#/overview` | 复用真实 ADP 登录；规则和候选页面均已在当前地址复验 |
| BPI Java/PostgreSQL/MinIO/Polaris | service `http://10.11.100.17:19091`；DB `ft_mes_bpi`；buckets `bpi-datasets` / `bpi-iceberg-warehouse` / `bpi-dataset-recovery` | PostgreSQL 15.18/Flyway V34；V28-V34 catalog、恢复包、MLflow Input、训练就绪、过程窗口、权限和遥测落表均通过受控目标验收；本轮 marker 0，模型/生产开关 false |
| Kafka/Flink/MinIO | `ft-mes-bpi-streaming`；REST `http://100.99.133.43:18081` | Kafka 4.2 三 broker；Flink 2.2.1 job `ffe9ab719bbf7250b682f77f75641f17` 为 RUNNING/36-of-36，最新复验 checkpoint `5533` |
| 固定 marker 回放 | `ADP_E2E_20260714_071034_1503790` | 只产生 1 个候选，数据质量错误 0 |
| TaskManager 恢复 | 带负载重启 1 个 TaskManager | 30/30 task 恢复，重启后继续完成 checkpoint |
| 浏览器/Kafka/Flink/PostgreSQL 联合写链 | `ADP_E2E_20260714_091536_BPI_JOINT` | 规则发布与应用、唯一候选、影子批次、2 条证据、状态事件和审计全部 PASS |
| 真实 WOM production context 联合链 | WOM `ADP_E2E_20260714_203900_WOM_CTX_REVFIX`；replay `ADP_E2E_20260714_204100_MESCTX_REAL` | WOM 页面 `start/hold`、outbox `SENT`、Kafka context offset、Flink 唯一候选、浏览器确认和 PostgreSQL 影子批次全部 PASS；遥测为受控回放 |
| JetLinks EventBus source 链 | `ADP_BPI_E2E_20260714_145738_757314` | EventBus subscriber、exporter received/enqueued/published、Kafka partition 4 offset `3 -> 4`、Flink consumer offset `4/4` 和 lag `0` 全部 PASS；不声明 candidate/batch |
| 拓扑/规则产品化链 | `ADP_E2E_20260715_004849_BPI_PRODUCT_TARGET` | 真实页面拓扑创建/校验、创建人发布拒绝、独立管理员发布、规则草稿、PostgreSQL 审计/幂等及服务重启后读取全部 PASS |
| 点位目录准入硬门禁 | `ADP_E2E_20260715_POINTCAT_02` | 真实页面导入和幂等重放 PASS；拓扑被四项硬错误阻断且重启后仍可读；试点设备状态保持 BLOCKED |
| JetLinks MQTT、点位目录与序列证据 | `ADP_BPI_MQTT_20260720_0918_*` / `sha256:7ad962...94ab` | 两次独立 MQTT QoS1 会话 6/6 PUBACK；JetLinks 双表、Kafka `37 -> 43`、来源证据 consumer lag=0/DLQ=0、MES current 和 `/#/points` PASS；验收时 `QUALIFIED/DEVICE`，1 点/0 READY 仅受校准阻断，证据 TTL 后重新失败关闭 |
| MQTT 与 WOM START/END 联合链 | `BPI_LIVE_20260720_123058` | 真实 WOM start/stop、三条 SENT context、测试校准四眼审批、START/END 规则、MQTT 5+5 PUBACK、两条 CONFIRMED candidate、同一 CLOSED_RAW batch、UNIT_MISMATCH 页面处置和受控恢复全部 PASS |
| 规则版本比较与审批 | `ADP_E2E_20260718_023214_BPI_LIFECYCLE` | Flyway V14；真实页面比较/模拟/提交，同 actor 422，独立管理员批准/驳回，PostgreSQL 审计/幂等和清理 PASS |
| 规则退役与延迟候选 | `ADP_E2E_20260718_065300_BPI_RETIRE_V15B` | Flyway V15；typed RETIRE、`APPLIED + INACTIVE`、savepoint、回滚草稿、退役后延迟候选恰好一次落库和真实候选页 PASS |
| 点位校准稳定分页 | `ADP_E2E_CAL_PAGE_20260719_164813` | Flyway V18；真实 API 2+2 条同快照、HMAC 游标篡改/跨 scope `422`、页面增量加载/搜索保持和 PostgreSQL 只读直查 PASS |
| 数据质量事件工作台 | `ADP_E2E_DQ_FLINK_20260719_2344_3ca0fff3` | Flink 自动提交 GAP、质量坏点、时钟漂移和重复四类事件；Kafka、PostgreSQL、真实页面和清理 PASS |
| 影子运行验收 | `ADP_E2E_SHADOW_20260720_0152_V20` | Flyway V20；真实页面启动/10 批复核/完成、CRITICAL 阻断与处置、独立批准、PostgreSQL 审计/幂等/外部写隔离和清理 PASS；8 天为受控时间压缩 |
| 运行开关治理 | `ADP_E2E_BPI_FLAGS_20260720_034527_0cf61838` | Flyway V21；真实页面 LINE SET false/INHERIT、审计/幂等、`1/2/2 -> 0/0/0` 清理、桌面/移动页面 PASS |
| 旧 MES 原生菜单门禁 | `ADP_E2E_BPI_SHELL_20260720_050100_df6fdb0e` | `bpi.ui` ENABLE/DISABLE/INHERIT、原生菜单及 iframe、`1/3/3 -> 0/0/0` 清理、adapter-down gateway 回退和最终 LINE active/enabled r1 全部 PASS |
| V23 质量/库存目标验收 | `ADP_E2E_20260720_BPI_QW_*` 4 个合同 marker；`ADP_E2E_20260720_215500_BPI_WMS` 与 `ADP_E2E_UI_20260720_222600_BPI_WMS` 两个全链 marker | expand-only V22->V23、401/403、重启读取、目标合同 4/4；受控 QCS -> Kafka -> query-first adapter -> material 单据/库存 -> receipt -> `INBOUNDED/r4`、QCS/Kafka 重放、真实页面五 API、布局修复和双库零残留 PASS；外部 QCS/ERP-WMS 补偿仍 BLOCKED |
| WMS 原单核对与停机恢复 | `ADP_E2E_20260721_015610_WMS_RECON`；`ADP_E2E_20260720193226_WMS_OUTAGE` | 原 event/key 核对、幂等/并发、真实停止 material-wms、命令 DLQ、恢复后单一 `12.345 kg` 入库事实、双库清理和开关恢复 PASS |
| V24 受控强制结束 | `ADP_E2E_20260721053253_BPI_FORCE_CLOSE` | 真实页面申请 202、待审批 PostgreSQL、同人批准 403、独立批准 202、`CLOSED_RAW/r3`、两条 timeline、零 QCS/WMS 副作用、31/31 adapter 回归和 marker 清理 PASS |
| V25 完工入库冲销 | `ADP_BPI_FORMAL_WMS_REVERSAL_20260721190630` | 目标正式双身份、蓝/红 command/receipt、BPI/material 双 PostgreSQL、净库存归零、真实页面与清理 PASS；外部 ERP/WMS 仍未联跑 |
| V26 数据集清单 | `ADP_E2E_BPI_DATASET_TARGET_20260722_055000_A7C4` | expand-only V26、真实 ADP 页面 definition 200、snapshot 202、`MANIFEST_READY/r3`、目标 PostgreSQL `3/1/2/3/0`、跨工厂 0、幂等 2/2、桌面/移动零错误及十类 marker 零残留 PASS |
| V27 版本锁定 Parquet | `ADP_E2E_BPI_PARQUET_20260722_105844_A1` | expand-only V27；真实页面请求后受控 `FAILED/MATERIALIZATION_ERROR`、页面重试到 `READY/r6`；PostgreSQL 审计与幂等、MinIO exact versionId/SHA/bytes/rows/schema、服务重启读取、最小权限和精确清理全部 PASS；V27 本身不声明 catalog/MLflow/模型就绪 |
| V28 Iceberg 目录发布 | `ADP_E2E_BPI_ICEBERG_20260722_175829_A1` / retry / fencing marker | 真实页面发布与失败重试、exact MinIO、Polaris time-travel、post-commit exit 86、stale-claim 恢复到同一 snapshot、桌面/移动、最小权限和清理 PASS |
| V29 Object Lock 恢复包 | `ADP_E2E_BPI_ARCHIVE_20260722_215300_A1` | 真实页面 `FAILED -> retry -> LOCKED`、PostgreSQL r1-r7、两个 exact retained versions、最小权限、隔离恢复/物理清除、原 publication 不变和零残留/default-off PASS；不替代整站灾备 |
| V30 MLflow Dataset Input | `ADP_E2E_BPI_MLFLOW_20260723_022000_A1` | 真实页面 `FAILED -> retry -> REGISTERED`、PostgreSQL r1-r6/幂等、MLflow 精确 source 的 1 run/1 dataset/1 input、停机无副作用、重启不重复、MinIO 最小权限及零残留/default-off PASS；不声明模型已训练或投产 |
| V31 离线训练就绪评估 | `ADP_E2E_BPI_READINESS_20260723_091500_A1` | 真实页面两次 `BLOCKED`、19 gates/8 blockers、幂等重放、不可变 trigger、重启回读、移动布局、MLflow/model 零副作用、Polaris/MinIO/PostgreSQL 精确清理和正式单编排恢复 PASS；数据资格仍为 BLOCKED |
| V32/V33 工艺信号窗口 | `ADP_E2E_BPI_WINDOWS_20260723_1235_A1` | 真实页面创建 2 个窗口，PostgreSQL 固化 6 条不可变事实（2 READY / 4 BLOCKED）；流量 mean 20、泵 true ratio 0.5，迟到/冻结后点被排除；首次权限门禁正确止损，V33 四角色最小权限、桌面/移动和 19 类清理 PASS；不声明物理来源、正式校准或模型资格 |
| V34 IoT 遥测窗口落表 | `BPI_TLANDING_20260723_094606` | 受控 MQTT 5/5 PUBACK；JetLinks -> Kafka -> scoped BPI consumer -> PostgreSQL；预热 3 条不计窗口，窗口内 2 events/2 points/0 rejects，页面五项 `1/1` 与 2/2、零 GAP/乱序；29 个请求全 2xx、桌面/移动零错误、marker 清零和 IoT 默认恢复 PASS；不声明物理现场 |
| 验收清理 | typed inactive + 定向 SQL + consumer deny-all | Flink 确认 inactive；marker topology/rule/candidate/batch 均为 0；读路径复验 PASS |

访问 BPI 前需要先在同一浏览器完成 ADP 登录，BPI 不保存或复制旧平台密码。适配器接受真实旧平台不透明会话票据，也保留严格 issuer/audience 校验的 JWT 路径；角色和租户/工厂/产线范围均由服务端映射，未配置映射时默认拒绝。

详细证据和结论边界见 [V34 IoT 遥测落表验收](docs/testing/bpi-iot-telemetry-landing-acceptance.md)、[V32/V33 工艺信号窗口验收](docs/testing/bpi-dataset-process-signal-window-acceptance.md)、[V31 离线训练就绪验收](docs/testing/bpi-dataset-training-readiness-acceptance.md)、[V30 MLflow Dataset Input 验收](docs/testing/bpi-dataset-mlflow-registration-acceptance.md)、[V29 Object Lock 恢复包验收](docs/testing/bpi-dataset-retention-archive-acceptance.md)、[Iceberg 目录发布验收](docs/testing/bpi-dataset-catalog-publication-acceptance.md)、[Parquet 目标验收](docs/testing/bpi-dataset-materialization-acceptance.md)、[数据集清单验收](docs/testing/bpi-dataset-manifest-acceptance.md)、[受控强制结束验收](docs/testing/bpi-force-close-acceptance.md)、[完工入库冲销验收](docs/testing/bpi-wms-inbound-reversal-acceptance.md)、[内部 WMS 蓝红整链验收](docs/testing/bpi-formal-identity-wms-roundtrip-acceptance.md)、[MQTT 与 WOM START/END 联合验收](docs/testing/bpi-live-mqtt-wom-start-end-acceptance.md)、[影子运行验收](docs/testing/bpi-shadow-run-acceptance.md)和 [IoT 仓库 MQTT 接入验收](https://github.com/MapleTcT/iot/tree/main/docs/testing)。BPI 产品总目标仍为 `PARTIAL`：V26-V34 的受控数据与遥测纵切已闭合，剩余的是物理点位/正式校准下的窗口证据、200 个真实复核批次、7 个生产日、物理设备来源序列、外部 ERP/WMS、整站灾备/容量、MLflow 生产安全/高可用和 Phase 4 模型。

## 第一次接手

### 工具链

| 区域 | 基线 |
|---|---|
| 既有 ADP/MES reactor | Java 8、Maven 3.6+ |
| BPI service / Flink | Java 17、Maven 3.6.3+（推荐 Maven 3.9.x） |
| BPI 操作台 | Node.js、npm、Vite |
| 部署和真实落库验收 | Docker Compose、PostgreSQL、Python 3 |

不要用 Java 8 运行 BPI reactor，也不要为了兼容旧 JAR 把 Java 17 模块降级或并入旧服务进程。两侧只通过版本化 HTTP 或事件契约交互。

执行相应门禁前先确认实际命中的工具链，避免 shell 中的旧 Maven 覆盖已安装版本：

```bash
java -version
mvn -version
command -v java
command -v mvn
```

`make ci` 使用 Java 8 主仓库基线；`make ci-java17` 必须由 Maven `3.6.3+` 和 Java `17` 执行。本仓库建议固定 Maven `3.9.x`；如果 BPI 门禁报插件要求 Maven `3.6.3` 或 JDK `17`，说明当前 `PATH` / `JAVA_HOME` 仍命中旧工具链，不是业务代码编译失败。

### 最短可信验证

从仓库根目录执行：

```bash
make help
make verify
make ci

JAVA_HOME=$(/usr/libexec/java_home -v 17) \
make ci-java17
```

本地验证真实 Flink checkpoint 与 Kafka 事务边界：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
make bpi-rule-application-flink-acceptance
```

该命令默认启动测试进程内的一次性 Kafka 4.2 KRaft server 和 Flink MiniCluster，不需要 Docker 或 PostgreSQL。Linux 请把 `JAVA_HOME` 换成实际 JDK 17 路径；也可用 `BPI_TEST_KAFKA_BOOTSTRAP_SERVERS` 指向专用外部 Kafka。报告默认写入 `/tmp/bpi-rule-application-flink-kafka-acceptance.json`。

常用 BPI 验证入口：

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 17) \
  mvn -f services/bpi-service/pom.xml -pl app -am test

JAVA_HOME=$(/usr/libexec/java_home -v 17) \
  mvn -f streaming/pom.xml -pl bpi-stream-engine -am test

make bpi-api-contract-check
make bpi-simulation-test
make bpi-ui-build
make bpi-dataset-materializer-test
make bpi-dataset-catalog-publisher-test
```

目标环境受控联合验收需要先准备唯一 marker 的 topology/rule fixture，并仅对一个
tenant/plant/line 打开消费者白名单。执行顺序为：

```bash
# 先在真实 BPI 页面完成规则模拟和发布，再运行流处理回放
make bpi-stream-joint-replay

# 浏览器确认候选并查库后，先从 Flink Broadcast State 移除该规则
make bpi-stream-rule-deactivate
```

最后必须运行 `deploy/bpi-runtime/sql/joint-acceptance-cleanup.sql` 定向清理 marker，
恢复 runtime consumer 默认关闭，并重新执行浏览器只读 smoke。不要在生产环境直接
使用验收 fixture；完整操作和证据要求见联合验收报告。

### Kafka + PostgreSQL 回执验收

使用独立测试库并显式提供测试凭据：

```bash
export BPI_TEST_DATABASE_URL='jdbc:postgresql://localhost:5432/bpi_acceptance'
export BPI_TEST_DATABASE_USER='bpi_acceptance'
export BPI_TEST_DATABASE_PASSWORD='本地私密值'

JAVA_HOME=$(/usr/libexec/java_home -v 17) \
mvn -f acceptance/bpi-runtime/pom.xml -pl :bpi-service -am \
  -Dtest=BpiRuleApplicationKafkaPostgresAcceptanceTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

这个测试证明 Kafka 消费端与 PostgreSQL 的事务、幂等和 DLQ 边界；它与 Flink MiniCluster 测试是两份互补证据，不能拼接成未经执行的“浏览器到数据库全链路通过”。

## 本地启动

### 既有 ADP/MES 测试栈

```bash
cd deploy/docker
cp .env.example .env
python3 scripts/render-nacos-configs.py
docker compose --env-file .env up -d
```

### BPI Kafka/Flink 测试栈

先构建 shaded job，再准备本地私有配置：

```bash
make bpi-stream-package
cp deploy/bpi-streaming/.env.example deploy/bpi-streaming/.env
make bpi-stream-deploy-preflight
make up-bpi-stream
make bpi-stream-cluster-smoke
```

需要停止时执行 `make down-bpi-stream`；该命令保留 Kafka 和 MinIO named volumes，便于做重启与 checkpoint 恢复验证。详细变量、容量预检和 marker 回放见 [BPI 流处理部署说明](deploy/bpi-streaming/README.md)。

### BPI Java/PostgreSQL 运行栈

Java 17 服务与其 PostgreSQL 数据库使用独立 Compose 项目，不覆盖既有 ADP/MES 编排：

```bash
make bpi-service-package
make bpi-adapter-package
make bpi-ui-build
cp deploy/bpi-runtime/.env.example deploy/bpi-runtime/.env
# 修改所有 change-me 值和私网 Kafka 地址
sh deploy/bpi-runtime/scripts/preflight.sh deploy/bpi-runtime/.env
docker compose --env-file deploy/bpi-runtime/.env \
  -f deploy/bpi-runtime/docker-compose.yml up -d --build
sh deploy/bpi-runtime/scripts/smoke.sh deploy/bpi-runtime/.env
```

Java 服务和 Web 默认分别只监听 `127.0.0.1:19091`、`127.0.0.1:18090`，测试机可显式把 Web 改为 Tailscale 地址。Kafka 候选、点位目录、规则发布和规则应用消费者默认全部关闭，必须先设置租户/工厂/产线 allowlist 才能打开。详见 [BPI 运行栈部署说明](deploy/bpi-runtime/README.md)。

不要提交 `.env`、真实密码、token、证书私钥、数据库 dump、运行日志或现场数据。

## 验收证据

| 验收面 | 当前证据 | 结论边界 |
|---|---|---|
| 拓扑/规则产品化 | [本地产品化验收](metadata/bpi-topology-rule-productization-acceptance.json)、[目标环境产品化验收](metadata/bpi-target-topology-rule-acceptance.json) | Flyway V1-V9、本地 PostgreSQL/浏览器和目标环境真实 ADP 页面/API/PostgreSQL/重启读取均通过；真实点位和产品级回退仍未覆盖 |
| 点位目录准入与拓扑门禁 | [目标环境验收](metadata/bpi-point-catalog-readiness-acceptance.json) | Flyway V10-V12、真实页面/API/PostgreSQL/幂等/重启读取通过；控制 PASS，但试点设备当前仍 BLOCKED |
| 点位目录自动同步 | [Kafka 同步验收](metadata/bpi-point-catalog-kafka-sync-acceptance.json) | JetLinks 权威目录、Protobuf、Kafka、MES 消费、PostgreSQL、DLT、重启幂等和真实浏览器读取通过；数据源仍 BLOCKED |
| 规则运行时就绪回执 | [多层验收](metadata/bpi-rule-runtime-readiness-acceptance.json)、[目标退役链](metadata/bpi-rule-retirement-acceptance.json) | 本地分层验收保留；目标 Flyway V15 已以独立 marker 证明 application `APPLIED` 与 runtime `READY -> INACTIVE`，两者仍不冒充真实设备连续运行 |
| BPI 浏览器状态交互 | [BPI UI 验收](metadata/bpi-ui-acceptance.json)、[质量与库存 UI 验收](metadata/bpi-quality-inventory-ui-acceptance.json)、[数据集清单验收](metadata/bpi-dataset-manifest-acceptance.json)、[Parquet 目标验收](metadata/bpi-dataset-materialization-acceptance.json)、[Object Lock 验收](metadata/bpi-dataset-retention-archive-acceptance.json)、[MLflow Dataset Input 验收](metadata/bpi-dataset-mlflow-registration-acceptance.json)、[训练就绪评估](metadata/bpi-dataset-training-readiness-acceptance.json)、[工艺信号窗口验收](metadata/bpi-dataset-process-signal-window-acceptance.json) | 真实目标 ADP 页面已闭合 Parquet、Iceberg、恢复包、MLflow 登记、19 门槛训练资格评估和工艺信号窗口的失败关闭、重试/幂等、重启及桌面/移动读取；非预期错误和溢出均为 0 |
| Phase 3A 数据集清单 | [目标浏览器/API/PostgreSQL 验收](metadata/bpi-dataset-manifest-acceptance.json) | Flyway V26、真实目标页面、确定性 checksum、时间点泄漏控制、跨工厂隔离、幂等和清理通过；manifest 保持不可变，V27 物化投影与其分离 |
| Phase 3B-A Parquet 制品 | [目标浏览器/API/PostgreSQL/MinIO 验收](metadata/bpi-dataset-materialization-acceptance.json)、[实现设计](docs/plans/2026-07-22-bpi-phase3b-materialized-artifact-design.md) | Flyway V27、任务 API、Python worker、私有版本桶、exact versionId/SHA、受控失败/页面重试、服务重启、最小权限和零残留清理均在目标通过；worker 默认关闭，catalog 进度由 Phase 3B-B 单独记账 |
| Phase 3B-B Iceberg catalog | [目标完整纵切](metadata/bpi-dataset-catalog-publication-acceptance.json)、[实现设计](docs/plans/2026-07-22-bpi-phase3b-iceberg-catalog-design.md) | Flyway V28、真实页面、Polaris/PyIceberg time-travel、exact source、失败重试、post-commit fencing 恢复到同一 snapshot、桌面/移动和清理通过；publisher 默认关闭 |
| V29 Object Lock 恢复包 | [目标页面/API/PostgreSQL/Object Lock/恢复验收](metadata/bpi-dataset-retention-archive-acceptance.json)、[完整报告](docs/testing/bpi-dataset-retention-archive-acceptance.md) | 同 archive `FAILED -> retry -> LOCKED`、两个 exact retained versions、最小权限、隔离 time-travel 恢复、恢复副本物理清除、原 publication 不变和零残留/default-off 通过；整站灾备、生产 RPO/RTO 与留存政策仍开放 |
| Phase 3C-A MLflow Dataset Input | [目标页面/API/PostgreSQL/MLflow/MinIO 验收](metadata/bpi-dataset-mlflow-registration-acceptance.json)、[完整报告](docs/testing/bpi-dataset-mlflow-registration-acceptance.md) | 同 registration `FAILED -> retry -> REGISTERED`、停机无 MLflow 副作用、精确 source、重启不重复、最小权限和零残留/default-off 通过；模型训练/注册/审批/推断及 MLflow 生产安全仍开放 |
| Phase 3C-B 离线训练就绪 | [目标页面/API/PostgreSQL/MLflow 反证验收](metadata/bpi-dataset-training-readiness-acceptance.json)、[完整报告](docs/testing/bpi-dataset-training-readiness-acceptance.md) | 两次不可变 BLOCKED、19 gates/8 blockers、幂等、同 checksum、重启回读、零模型副作用和精确清理通过；v1 冻结快照当时没有信号窗口且样本不足，不允许启动训练 |
| Phase 3C-C 工艺信号窗口 | [目标页面/API/PostgreSQL 验收](metadata/bpi-dataset-process-signal-window-acceptance.json)、[完整报告](docs/testing/bpi-dataset-process-signal-window-acceptance.md) | Flyway V32/V33、2 个定义、6 条不可变 point-in-time 事实、迟到/冻结后排除、明确 BLOCKED、最小权限、桌面/移动和精确清理通过；受控 fixture 不替代物理 DEVICE/GATEWAY、正式校准或 200 批/7 天 |
| 数据质量事件工作台 | [本地与目标全链验收](metadata/bpi-data-quality-workbench-acceptance.json)、[Flink 自动链](metadata/bpi-flink-data-quality-acceptance.json) | Flyway V19；本地 PostgreSQL + Embedded Kafka、Java 8 adapter、模拟器和浏览器 E2E 通过；目标 Flink 自动事件、真实 Kafka/ADP 页面/API/PostgreSQL、清理和 consumer deny-all 恢复通过 |
| 影子运行验收工作台 | [目标环境验收](metadata/bpi-shadow-run-acceptance.json) | Flyway V20；真实页面、API、10 批复核、数据质量阻断/处置、四眼批准、PostgreSQL 审计/幂等/外部写隔离和清理通过；8 天为受控时间压缩，不能代替现场连续运行 |
| 旧 MES 原生菜单门禁 | [目标环境验收](metadata/bpi-shell-menu-gate-acceptance.json) | Flyway V21；真实旧 MES 菜单、BPI 恢复页、API、PostgreSQL、iframe、桌面/移动布局和 adapter 故障回退 18/18；只治理导航可见性，不替代 API 授权或生产写回 |
| QCS 放行与 WMS 完工入库 | [目标正向链](metadata/bpi-quality-release-wms-target-acceptance.json)、[正式身份蓝红整链](metadata/bpi-formal-identity-wms-roundtrip-acceptance.json)、[产品页验收](metadata/bpi-quality-inventory-ui-acceptance.json) | Flyway V23 正向链和 V25 内部冲销链均在目标闭合；正式双身份、蓝红 command/receipt、双 PostgreSQL、净库存归零和清理通过。外部 QCS/ERP-WMS 仍 BLOCKED，全部开关默认关闭 |
| 批次受控强制结束 | [目标页面/API/PostgreSQL 验收](metadata/bpi-force-close-target-acceptance.json)、[正式身份验收](metadata/bpi-formal-identity-force-close-acceptance.json) | Flyway V24、真实页面申请、同人 403、正式不同管理员批准、task/event/audit/idempotency、零 QCS/WMS 副作用、布局和清理通过；物理 END 仍待现场验收 |
| 回执 PostgreSQL 状态迁移 | [回执落库验收](metadata/bpi-rule-application-receipt-acceptance.json) | 真实 PostgreSQL，不含真实 broker |
| Kafka 消费重启、幂等与 DLQ | [Kafka/PostgreSQL 联合验收](metadata/bpi-rule-application-kafka-postgres-acceptance.json) | 本地 Embedded Kafka + PostgreSQL/Flyway V13，覆盖 application/readiness 双 source，不含 Flink job |
| Flink checkpoint、事务可见性与恢复 | [Flink/Kafka 验收](metadata/bpi-rule-application-flink-kafka-acceptance.json) | 真实 Flink MiniCluster + Kafka 4.2，覆盖 application/readiness 双 sink；本地文件 checkpoint，不含 PostgreSQL/MinIO |
| 目标环境运行与分段链路 | [目标环境验收](metadata/bpi-test-environment-acceptance.json) | 浏览器只读链、Kafka/Flink 数据面和恢复测试通过 |
| 目标环境受控联合写链 | [浏览器/Kafka/Flink/PostgreSQL 联合验收](metadata/bpi-browser-kafka-postgres-joint-acceptance.json) | 同一 marker 受控 Phase 1 链通过，不含真实 IoT/MES 上下文 |
| 目标环境规则版本、审批与退役 | [版本生命周期](metadata/bpi-rule-version-lifecycle-acceptance.json)、[退役与延迟候选](metadata/bpi-rule-retirement-acceptance.json) | Flyway V14/V15、真实页面、审批职责分离、typed inactive、savepoint、回滚草稿和延迟候选落库通过；不含现场 READY 和生产写回 |
| IoT MQTT、exporter、目录与序列证据 | [MQTT 联合验收](metadata/bpi-mqtt-ingress-joint-acceptance.json) | 受控 MQTT 3.1.1/QoS1 双会话、JetLinks 双表、Kafka/Flink、MES current/DQ PostgreSQL 和真实页面已闭合；不含物理现场设备、校准批准或 candidate/batch |
| MES production context 工程链 | [真实运行验收](docs/testing/bpi-mes-production-context-runtime-acceptance.md) | 目标机真实 WOM 页面、outbox、Kafka/Flink、候选确认、影子批次和收尾恢复通过 |
| MQTT/WOM START/END 联合链 | [机器证据](metadata/bpi-live-mqtt-wom-start-end-acceptance.json) | 同一真实 WOM 指令已闭合受控 MQTT、JetLinks、Kafka/Flink、两条候选和 CLOSED_RAW 影子批次；来源和校准仍为测试专用 |
| 现场真实链 | [项目总目标验收总账](docs/project-goal-acceptance.md) | 物理设备、正式证书和连续 7-14 天现场影子运行未完成，BPI 总目标保持 `PARTIAL` |

证据等级从低到高为：静态/单元测试、模拟浏览器、真实 PostgreSQL、本地 Kafka + PostgreSQL、本地 Flink + Kafka、目标集群全链路、现场影子运行。每一级只证明自己实际执行的边界，不能用两份分离测试冒充一条没有跑过的联合链路。

## 验收原则

功能完成必须形成“真实页面 -> HTTP -> 后端链路 -> PostgreSQL”证据：

- 不能只凭源码、静态检查或 `make ci` 判断功能完成；菜单可见和接口 `200` 也不能单独证明业务完成。
- 写操作使用唯一 `ADP_E2E_*` marker，并直接查询 PostgreSQL 验证新增、更新、状态变化和清理。
- 记录页面/路由、操作步骤、console error、network error、API、payload、response、Controller/Service/Mapper/SQL 和目标表。
- Kafka `PUBLISHED` 只说明 broker 已接收；Flink `APPLIED` 回执才说明规则随成功 checkpoint 进入运行状态。
- 使用 `PASS / FAIL / BLOCKED / NOT_APPLICABLE` 如实记录，禁止为了绿色报告改写结果。
- 模拟器、本地 MiniCluster 和单元测试不能冒充目标服务器或现场产线验收。

固定入口：

- [功能验收与落库验收规则](docs/functional-persistence-acceptance.md)
- [前端功能测试报告](docs/frontend-functional-test-report.md)
- [后端落库验收报告](docs/backend-table-audit/persistence-acceptance.md)
- [机器可读落库账本](metadata/persistence-acceptance.json)
- [BPI 工程测试计划](docs/testing/bpi-engineering-test-plan.md)
- [BPI 数据质量事件工作台验收](docs/testing/bpi-data-quality-workbench-acceptance.md)
- [BPI QCS 放行与 WMS 完工入库验收](docs/testing/bpi-quality-release-wms-inbound-acceptance.md)
- [BPI 批次质量与库存页面验收](docs/testing/bpi-quality-inventory-ui-acceptance.md)
- [BPI 批次受控强制结束验收](docs/testing/bpi-force-close-acceptance.md)
- [目标缺口总账](docs/goal-gap-register.md)

## 仓库结构

```text
frontend/apps/                 恢复前端与 BPI 操作台
backend/modules/               sources.jar 恢复源码，只读排查和迁移依据
backend/decompiled-services/   运行服务反编译启动壳
backend/source-modules/        可编译、可测试、可持续维护的源码模块
services/bpi-service/          Java 17 BPI PostgreSQL 服务与规则运行时
streaming/bpi-stream-engine/   Java 17 Flink 批次边界引擎
contracts/bpi-api/             BPI OpenAPI 与实施/模拟能力清单
contracts/bpi-events/          Protobuf 事件契约与兼容性基线
simulation/bpi/                无外部依赖的交互/API 模拟器
deploy/docker/                 ADP/MES PostgreSQL-first 测试编排
deploy/bpi-runtime/            BPI Java 17 服务与独立 PostgreSQL 编排
deploy/bpi-streaming/          Kafka/Flink/MinIO 流处理编排
deploy/database/               PostgreSQL 迁移与生产迁移证据工具
docs/                          目标、设计、测试、落库和交接文档
metadata/                      机器可读验收与治理账本
scripts/                       构建、恢复、审计和门禁脚本
```

原始 Windows 包仍保留在仓库上层的 `../bap-server/`、`../Commands/`、`../nginx/` 和 `../Manual/`，不进入默认源码 reactor。

## 恢复资产边界

- 从 `366` 个 source map 恢复约 `991` 个前端源码文件。
- 解包 `250` 个 ADP 相关 `sources.jar`，包含约 `4807` 个 Java 和 `398` 个 XML 文件。
- 对 `23` 个可运行服务补充反编译启动壳和服务清单。

这些数字描述“可排查资产”，不代表全部模块已经可编译或可维护。需要持续开发的模块必须逐步提升到 `backend/source-modules/`，补齐父 POM、依赖边界、测试、PostgreSQL migration 和运行验收。

## 接手顺序

1. 阅读 [项目工作指令](AGENTS.md)、[项目目标](docs/project-objectives.md) 和本 README 的状态边界。
2. 查看 [项目总目标验收总账](docs/project-goal-acceptance.md) 与 [目标缺口总账](docs/goal-gap-register.md)。
3. BPI 开发先读 [BPI 总设计](docs/designs/batch-process-intelligence.md)、[交互设计](docs/designs/bpi-interaction-design.md) 和 [API 目录](docs/api/bpi-api-catalog.md)。
4. 既有业务修复先读 [后端落表排查交接](docs/backend-table-audit-handoff.md) 和对应模块审计。
5. 新业务包先执行 `make module-intake-check INTAKE=/path/to/package-or-dir`，再决定是否进入默认源码路径。

## 当前未闭合事项

- 保持已完成的目标环境同一 marker 浏览器发布、outbox、Kafka、Flink 应用回执、PostgreSQL、候选确认、批次/证据/审计联合验收为发布回归基线。
- 保持 Flyway V22、影子运行验收、点位目录与来源序列证据、拓扑/规则产品化、审批、退役、typed inactive、savepoint、延迟候选落库、真实 ADP 会话和 PostgreSQL marker 清理为每次发布回归基线。
- 单 broker 故障和 service/adapter/Flink 组件回退已完成；下一步在生产等价维护窗口做带真实业务负载的跨组件整体回切与流量恢复。
- 试点产品/设备、`instantFlow` metadata 和单位已注册激活；受控 MQTT 已证明双会话序列和重连 epoch 语义。下一步由现场计量人员提交与目录 `calibrationVersion` 精确匹配的真实证据，并用物理设备重复同一验收，禁止手工伪造 READY 快照。
- 保持已闭合的同一 WOM 指令 START/END candidate/batch 为发布回归，用物理设备和正式校准替换受控 MQTT/测试校准后重复整条链，并连续运行 7-14 天影子批次。
- 使用已经通过软件闭环的影子验收工作台，在选定产线真实连续运行 7-14 天，采集人工边界认同率、累计量偏差、关键数据质量事件和停机/重连恢复证据。
- QCS/WMS 幂等写回、异常补偿、谱系、完工入库闭环和后续训练数据产品。
- 既有 MES 生产、质量、仓储主链剩余页面/API/落库阻断项。

生产迁移在数据库、回滚、license、MinIO、Keycloak、Nacos/runtime、TLS、安全和业务签字证据全部完成前，必须保持 `NOT_READY_FOR_PRODUCTION_MIGRATION`。
