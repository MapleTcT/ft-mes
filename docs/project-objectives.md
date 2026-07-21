# 项目目标和交付路线

## 总目标

把当前 Windows 交付包恢复出的 ADP/MES 资产，整理成可以长期维护、持续开发、持续验证的代码仓库，并把默认数据库路线从 Oracle 切换到 PostgreSQL。

这个目标不是简单“能启动”。最终应达到：

- 代码仓库能承接新模块开发。
- 后端模块能逐步从恢复源码提升为可编译源码。
- Docker 测试环境默认 PostgreSQL，不依赖 Oracle 授权。
- Oracle 兼容只作为显式 legacy 路径保留，并且可以按模块退场。
- 平台功能和业务模块功能有清晰边界。
- 后续后端落表、业务表含义、页面/API 对表关系可以独立线程持续排查。

当前测试环境的验证边界见 [测试环境验证范围](runtime-validation-scope.md)。本阶段优先闭合平台登录、用户、组织、权限、菜单、待办、基础配置、Nacos、Keycloak、PostgreSQL 和 runtime patch；生产模块必须补完整功能测试用例和真实前端验收记录，不能只停留在菜单/API 可见。

功能验收和后端落库验收必须遵循 [功能验收与落库验收规则](functional-persistence-acceptance.md)。后续不能只补治理层、只跑静态检查或只看代码推断功能可用；涉及写业务数据的前端动作必须用唯一 marker 通过 PostgreSQL 查询证明真实落库。

2026-07-21 基础配置的 PostgreSQL 实体模型路径已继续闭合：字段约束生命周期 `33/33`、23 类标量字段矩阵 `36/36`、双模型 `OBJECT` 关联 fixture `10/10`，以及字段删除 `18/18` 均通过真实 `/msService/ec/engine/msManage` 父页面、API、PostgreSQL marker、失败回滚和受控清理。删除契约现已明确：普通删除只软删元数据并保留列/数据；只有不可撤销警告后的显式删除才执行 `DROP COLUMN RESTRICT`，并对依赖、事务失败、结构漂移、附件字段和固有主键 fail closed。证据为 `metadata/entity-model-object-association-acceptance.json` 和 `metadata/entity-model-field-delete-persistence-acceptance.json`；保存、软删、模型批删和 SQL 模型对账中的自动删列继续按设计禁用。

总目标完成状态见 [项目总目标验收总账](project-goal-acceptance.md)。该总账把可持续开发仓库、Oracle 替换、平台验证、生产模块完整验证、PostgreSQL 缺口治理和生产迁移前置项统一到机器可读账本 `metadata/project-goal-acceptance.json`，并由 `make project-goal-acceptance-check` 校验。总账为 `IN_PROGRESS_NOT_COMPLETE` 时，不能宣称当前目标已全部完成。

当前仍未闭合的目标缺口见 [目标缺口总账](goal-gap-register.md) 和
`metadata/goal-gap-register.json`，由 `make goal-gap-register-check` 校验。后续新线程接手业务包、
后端落表或生产迁移时，应先看这份总账，避免把局部页面/API smoke 通过误判为整体完成。

## 当前项目定位

当前仓库不再只有单一的“运行包恢复”定位，而是包含两条受同一验收体系约束的产品线：

1. **既有 ADP/MES 平台恢复线**：维护登录、组织、权限、菜单、配置、生产/质量等恢复资产，持续把可维护模块提升为源码模块并迁移到 PostgreSQL。
2. **BPI 新产品线**：建设智能批次与工艺数据中心，连接 JetLinks/IoT、MES 生产上下文、Kafka/Flink、PostgreSQL、QCS 和 WMS，先完成影子批次，再进入生产闭环和训练数据产品。

既有仓库主体仍来自 ADP/BAP 平台运行包，不等于原厂完整 MES 业务产品；BPI 则是本仓库中按可编译源码、版本化契约和真实验收新建的产品模块，不能与恢复代码的完整度混为一谈。

已恢复内容主要包括：

- 平台前端 source map 源码。
- 平台后端 sources.jar 源码。
- 运行服务反编译启动壳。
- Docker/Linux 测试部署编排。
- PostgreSQL runtime 兼容 SQL 和 patch 脚本。
- 基础模块、质量/QCS、EAM、生产、安环等恢复模块及部分运行包适配痕迹。
- PATROL `6.0.4.0` 共享巡检源码、PostgreSQL 迁移、菜单权限、工作流、回滚和 EamMs 增量补丁；真实页面已闭合配置、任务执行、正常/异常结果、待治理隐患生成、EAM 台账复显和 8 个统计/监控入口，Kafka 采集消费者也已真实到达 TagManagement。
- WOM 可见手工新建指令单源码模块 `wom-production-entry`、列表入口和 PostgreSQL 迁移 189；真实 marker `ADP_E2E_20260717101030_WOM_MANUAL_ENTRY` 已完成创建、幂等、重复批次保护、待办、提交、软删除和清理 9/9 验收。
- RM 批控配方可维护源码模块 `rm-formula-editor`、可见 `Web编辑` 入口和 PostgreSQL 迁移 190；真实 marker `ADP_E2E_20260717120436_RM_WEB_FORMULA` 连续三轮完成创建、幂等、乐观锁修订、六表回读、隔离投递失败/重试、桌面/移动端和清理验收。页面不再依赖 IE ActiveX/localhost:4433，现场 Batch/DCS 端点和业务签字作为生产切换前置项单独管理。
- `supEMS`、`energyPlan`、`EnergyConBase`、`EnergyPred` 四个能源源码包；当前因缺失 Indicator `6.0.4.0` 和 PostgreSQL 迁移保持阻断。

业务层面的生产、质量、设备、能源、安环等完整产品形态，需要后续按模块继续接入、落表排查和业务 smoke。PATROL 当前通过的是异常发现到 EAM 待治理记录，不代表未安装的 SESH 整改、复查和销项状态机；统计/监控已验收，Kafka -> 模拟登录 -> TagManagement 技术链也已通过，但测试库没有真实测点元数据和历史值，因此 `gather_data` 中位数落库与误差图表仍不能冒充 PASS，目标机破坏性回滚也仍需维护窗口。EMS 源码到位同样不等于可部署，必须先补真实 Indicator 依赖并完成 PostgreSQL 迁移。其中生产模块仍是当前核心目标，需要形成完整功能测试用例，覆盖主数据、指令/工单、备料/投料、作业许可、执行记录、报工、退料/尾料、状态流转、导入导出和落库证明。

## BPI 产品目标

BPI 的产品目标不是做一个监控大屏，而是把数采信号变成可审计的生产事实：

- 用生产指令、阀门路径、设备状态、流量、液位和物料/配方切换共同判断批次边界。
- 以事件时间、checkpoint、幂等 inbox/outbox 和版本化规则保证可回放与可解释。
- 自动形成批次、工艺参数、物料/能源耗用、质量证据和谱系，为后续 QCS/WMS 联动提供权威输入。
- 保留人工确认、拒绝、修订和异常救援入口，首期只运行影子批次，不直接改写 WOM/QCS/WMS 生产状态。
- 为 Iceberg/MLflow 训练数据产品保留 point-in-time、版本、质量码、校准和标签来源，禁止用无法追溯的聚合结果训练模型。

当前 BPI 已从设计进入目标环境实施：事件/API 契约、Java 17 PostgreSQL 服务、Java 8 适配器、操作台、模拟器、遥测入库、版本化规则/拓扑、候选/影子批次、Flink 事件时间与 Broadcast State、规则发布 transactional outbox、失败重试和审计已经具备可复验证据。拓扑/规则不再依赖日常 SQL fixture：页面已支持新建或复制拓扑/规则版本，拓扑发布前校验路径、环、JetLinks 产品/设备/属性、单位、校准和必需信号，独立管理员发布后版本不可变；Flyway V1-V9、真实 PostgreSQL marker 和 7 条浏览器 E2E 已通过。目标测试环境已运行独立 Java/PostgreSQL 与 Kafka/Flink/MinIO Compose；真实 ADP 会话、三 broker Kafka、Flink job、MinIO checkpoint、固定 marker 回放和带负载 TaskManager 恢复均已通过。2026-07-14 先以 marker `ADP_E2E_20260714_091536_BPI_JOINT` 闭合真实浏览器规则模拟/发布、outbox、Kafka、Flink `APPLIED`、候选入库、浏览器确认和影子批次；随后又以真实 WOM marker `ADP_E2E_20260714_203900_WOM_CTX_REVFIX` 和 replay marker `ADP_E2E_20260714_204100_MESCTX_REAL` 闭合页面 `start/hold`、PostgreSQL context outbox、Kafka offset、Flink join、唯一候选、浏览器确认及影子批次落库，并完成 typed inactive、定向清理和消费者默认关闭恢复。该验收同时修复了合成上下文残留与 WOM 版本域冲突，新增 `177` 版本时钟下限。IoT 基线 `MapleTcT/iot@beefd1d5` 已补齐可观测遥测 exporter、JetLinks 权威点位目录、来源序列证据 publisher、受控试点编排和 30m 运行时证据准入；受控遥测 marker `ADP_BPI_E2E_20260714_145738_757314` 已证明 EventBus、Kafka 与 Flink source。2026-07-15 目标环境先以 marker `ADP_E2E_20260715_004849_BPI_PRODUCT_TARGET` 完成 Flyway V9、真实页面拓扑创建/校验、职责分离发布、规则草稿、PostgreSQL 审计/幂等和重启读取，再升级到 Flyway V12，以 marker `ADP_E2E_20260715_POINTCAT_02` 完成真实页面点位快照导入和准入门禁。随后自动目录 revision `sha256:2a218d12...151ce5` 又闭合 `JetLinks -> Kafka -> BPI service -> PostgreSQL -> 浏览器`。2026-07-20 Flyway V22 进一步把 origin、binding fingerprint 和运行证据纳入 READY 联查；早期无遥测时证据为 `DISABLED/r2`，系统正确保持失败关闭。随后受控 MQTT 分支以两次独立 MQTT 3.1.1/QoS1 会话发送 `2026072003:5001..5003` 和重连后的 `2026072004:1..3`，六条 PUBACK、JetLinks 双表、Kafka `37 -> 43`、Flink、MES source evidence、六条数据质量原始证据和真实页面全部闭合。验收窗口内来源序列为 `QUALIFIED/DEVICE`，点位只因校准未批准保持 0 READY；30 分钟 TTL 到期后重新失败关闭。marker `BPI_LIVE_20260720_123058` 随后已把受控 MQTT 与真实 WOM context 汇合为同一 START/END 影子批次，目标 V23 又闭合受控 QCS、Kafka 和内部 material-wms 单据/库存。当前缺口已收敛为物理设备、正式校准、连续 7-14 天和外部 QCS/ERP-WMS，而不是软件链本身。因此 BPI 总目标保持 `PARTIAL`，禁止把受控输入外推为现场生产 READY。

本批 marker `ADP_E2E_20260715_0532_BPI_SOURCE_SEQUENCE` 已继续部署到目标环境：真实 ADP 同源页面
`http://10.11.100.17:18080/bpi/#/points` 的目录 GET 为 `200`，PostgreSQL 15.18 同一自动 revision 为
1 点/0 READY 且 `source_sequence_enabled=false`，页面显示五项阻断并保持 `BLOCKED`，浏览器错误为 0。
该结果只证明准入门禁在目标环境生效，不改变现场数据源尚未就绪和 G-021 保持 `PARTIAL` 的结论。

早期 `MapleTcT/iot@beefd1d5` 把 YAML 声明收紧为运行时证据门禁，并将 Redis 资格状态转成内容寻址
Protobuf 事件。Flyway V22 以 inbox、current evidence 和审计接收该事件；无遥测时目标环境记录两次
`DISABLED` 心跳，current revision 为 `r2`，目录快照携带 origin 和绑定指纹，页面保持 1 点/0 READY。
该历史快照证明失败关闭和证据链真实运行。

2026-07-20 的受控 MQTT 联合验收进一步证明多事件严格递增和重连换 epoch：marker
`ADP_BPI_MQTT_20260720_0918_EPOCH3` 与 `ADP_BPI_MQTT_20260720_0918_EPOCH4` 共 6 条 QoS1 消息
全部收到 PUBACK；JetLinks PostgreSQL、Kafka、Flink、MES 来源序列 current、数据质量 incident/raw event 和
真实浏览器均读取到对应序列。来源序列在证据有效窗口内为 `QUALIFIED`，但校准仍为 `UNVERIFIED`，
candidate/batch 均为 0。该结果仍是受控模拟器，不证明物理设备、断电恢复、END 边界或 7-14 天稳定性，
禁止据此打开自动批次。证据见
[BPI MQTT 接入跨仓库联合验收](testing/bpi-mqtt-ingress-joint-acceptance.md)。

仓库级运行时准入随后补齐：规则发布契约固化 `productId`/`calibrationVersion`，Java 服务在发布事务内
按当前目录重验全部绑定；Flink 增加 `iot.point-catalog.snapshot.v1` 控制流，只向 evaluator UPSERT
全部绑定 READY 的规则。目录降级会发送 DELETE 并清空匹配规则的待决窗口，旧 event-time timer 不会
继续产出候选，精确校准恢复后也不会复用旧观测。2026-07-15 又完成控制面
`APPLIED/REJECTED` 与运行时 `READY/DEGRADED/INACTIVE` 分离：Flink 2.2.1 MiniCluster + Kafka 4.2
验证 checkpoint 前后双回执可见性、停用和 TaskManager 恢复；Embedded Kafka 3.8.1 + PostgreSQL
16.13/Flyway V13 验证 `DEGRADED -> READY`、事件时间排序、精确重放、双 DLQ 和最终
`APPLIED + READY/r5` 真实落库；操作台 `8/8` E2E 验证两个状态域独立显示。目标环境随后已完成
Flyway V13 expand-only 迁移，并从 V12 savepoint 有状态恢复到 Flink job
`40408b7907ca7b97ad750cc7d2bfb345`，该阶段为 `RUNNING/33-of-33`；来源目录仍为 1 点/0 READY。
后续 V14-V24、规则运行时回执、savepoint、单 broker 故障恢复、应用组件回退、受控 QCS/WMS、
强制结束兜底和真实候选/批次负载下的跨组件整体回切已分别闭合；现场来源、7-14 天影子运行和外部
QCS/ERP-WMS 仍未完成，因此 G-021 保持 `PARTIAL`。

2026-07-18 目标环境先扩展到 Flyway V14，并以 marker
`ADP_E2E_20260718_023214_BPI_LIFECYCLE` 闭合拓扑/规则稳定 JSON Pointer 版本比较、
规则历史回放、精确 simulation 证明提交、同 actor `422` 拒绝、独立管理员批准发布和独立管理员驳回。
真实 ADP 页面、Java 8 适配器、Java 17 服务和 PostgreSQL 15.18 均已复验：两条 simulation 为
`PASSED`，批准分支为 `PUBLISHED/r4 + APPROVED/r2 + 1 outbox`，驳回分支为
`DRAFT/r4 + REJECTED/r2 + 0 publication outbox`，6 条审计和 6 条幂等记录准确；非预期浏览器错误为 0，
marker 定向清理后残留为 0。版本比较与业务审批控制面因此从待办转为已闭合基线。

同日目标环境继续升级到 Flyway V15。marker `ADP_E2E_20260718_065300_BPI_RETIRE_V15B`
通过真实规则页面、Kafka 4.2、Flink 2.2.1、savepoint 和 PostgreSQL 15.18 闭合
`PUBLISHED -> RETIRED`、typed `RETIRE`、双 `APPLIED`、运行时 `READY -> INACTIVE`、
有状态恢复和 `2.0.1` 回滚草稿。规则退役前产生但退役后才消费的候选仍按历史已发布版本恰好一次
写入 candidate/inbox，真实候选页复显 `PENDING/r1` 和 2/2 证据；未确认候选，因此 batch、WOM、
QCS、WMS 均未写入。当前 marker 与旧诊断 marker 定向清理后 11 类残留均为 0。版本比较、审批、
受控退役、typed inactive、savepoint 升级和候选在途语义已成为发布回归基线。G-021 仍保持
`PARTIAL`：当时现场来源 READY、真实 END 边界、7-14 天影子运行、跨组件整体回切和
QCS/WMS 写回仍未闭合。证据见
[BPI 规则版本比较与审批生命周期验收](testing/bpi-rule-version-lifecycle-acceptance.md) 和
[BPI 规则退役、回滚与延迟候选落库验收](testing/bpi-rule-retirement-acceptance.md)。

2026-07-19 又修复了点位目录重复内容无法撤销准入的问题。`sourceRevision` 继续表示规范化内容，
event ID 与 PostgreSQL V16 唯一键增加观测时间；MES 消费者保留旧 event ID 滚动兼容。目标环境用
marker `ADP_BPI_REPEAT_SEQUENCE_20260719_1106` 真实验证来源序列 `false -> true -> false`：受控消息
HTTP 200、Kafka offset `2 -> 4`，最终入口恢复为 404，同一 false revision 保存 3 次独立观测，
最新目录仍为 1 点/0 READY。最终浏览器 marker `ADP_BPI_REPEAT_FINAL_20260719_110916` 为 PASS，
console/page/request error 均为 0。该修复保证运行时证据过期或配置回退能失败关闭，但不改变现场
标定和真实连续序列尚未就绪的事实。证据见
[BPI 点位目录重复观测与准入撤销验收](testing/bpi-point-catalog-repeated-observation-acceptance.md)。

同日又以 marker `ADP_BPI_BROKER_CHAOS_20260719_1129` 完成目标三 broker 真实故障恢复：停止
`kafka-2` 后 151 个分区 unavailable=0、低于 minISR=0，故障期间唯一 marker 以 `acks=all` 写入并
只消费一次；Flink job 始终 RUNNING，checkpoint `2481 -> 2482`，恢复后推进到 `2483` 且失败数为 0。
`kafka-2` 恢复 healthy 后全部分区回到 ISR=3，标准集群 smoke 又推进到 checkpoint `2485`。
因此单 broker 故障演练退出 G-021 阻断项。证据见
[BPI 单 Broker 故障恢复验收](testing/bpi-broker-failure-recovery-acceptance.md)。

同日又完成应用组件双向回退。marker `ADP_BPI_RUNTIME_ROLLBACK_20260719_120102` 将 Java 17 service
和 Java 8 adapter 回退到上一版镜像，真实 ADP 登录、`/bpi/#/points`、点位目录 API、Docker health
均通过，Flyway V16 与 9 类核心表计数不变，随后恢复精确 tag/image ID 并复验。marker
`ADP_BPI_FLINK_ROLLBACK_20260719_120659` 从当前 canonical savepoint 恢复上一版 JAR，再从上一版
savepoint 恢复当前 JAR；两次均保持 `allowNonRestoredState=false`、33/33 task 和 checkpoint 推进。
service/adapter/Flink 应用组件回退因此退出阻断项；当时真实业务负载下的跨组件整体回切仍未验收。证据见
[BPI 应用组件回滚验收](testing/bpi-application-rollback-acceptance.md)。

同日又关闭 JetLinks 平台前置条件：通过官方 API 安装协议、注册激活
`bpi-pilot-product-01` / `bpi-pilot-device-01`，并从受审映射生成 `instantFlow: double` metadata；
自动 revision `sha256:8b4fd91c...e3148022` 已进入 MES PostgreSQL。marker
`ADP_BPI_UNIT_ALIAS_20260719_1325` 通过真实 ADP 页面、API 和 PostgreSQL 证明 JetLinks `m³/h`
与 MES 拓扑 `m3/h` 等价，校验精确只剩标定和来源序列两项错误，浏览器错误为 0。marker 草稿、审计和
幂等记录已定向清理，自动快照保留。平台注册、设备激活、metadata 和单位因此退出 G-021 阻断项；
现场标定、强制连续单调 DEVICE/GATEWAY 来源序列、同 scope candidate/batch 和影子运行仍未完成。证据见
[BPI 试点平台前置条件与单位别名验收](testing/bpi-pilot-platform-prerequisites-acceptance.md)。

随后 Flyway V17 将来源声明的 `readyPointCount` 明确降级为
`source_claim_ready_point_count`，新增独立 `bpi_point_calibrations` 证据表、校准工作台、四眼审批、
有效期和撤销。目标 marker `ADP_E2E_CAL_20260719_160131` 已从真实 `/bpi/#/points` 页面闭合
`PENDING/r1 -> APPROVED/r2 -> REVOKED/r3`，同一提交人批准被 `422` 拒绝；PostgreSQL 直查为
3 条动作审计和 3 条 `COMPLETED/200` 幂等记录。测试证据故意使用非匹配版本，批准期间真实点位仍为
0 READY，撤销后继续 `BLOCKED`。软件侧校准治理因此退出阻断项，但现场真实证书、精确版本匹配和
来源序列尚未完成，G-021 继续保持 `PARTIAL`。证据见
[BPI 点位校准治理与失败关闭验收](testing/bpi-point-calibration-governance-acceptance.md)。

同日 Flyway V18 为校准证据列表增加与 scope 和排序一致的索引，并将读取契约改为 HMAC 签名、
完整筛选 scope 绑定的稳定快照 keyset cursor。marker `ADP_E2E_CAL_PAGE_20260719_164813`
在目标真实 adapter/service/PostgreSQL 和 `/bpi/#/points` 页面以 `limit=2` 读取 2+2 条：
两页 `snapshotAt` 一致、4 个 ID 无重复，篡改和跨 scope 游标均为 `422`，页面搜索保持且浏览器错误为 0；
验收前后表均为 4 行。证据见
[BPI 点位校准高基数分页验收](testing/bpi-point-calibration-pagination-acceptance.md)。

同日点位目录读取也从一次性完整响应升级为兼容式高基数读取：点位页显式使用默认 100、上限 200 的
keyset cursor，规则和拓扑旧调用不带分页参数时继续获得完整快照。cursor 以 HMAC 绑定 tenant、plant、
line、search 和不可变 snapshot。目标 marker `ADP_E2E_POINT_PAGE_20260719_1858` 在真实页面、adapter、
service 和 PostgreSQL 上完成 `2+2+1`、5 个唯一 ID、服务端搜索、游标篡改/换 search `422`，并证明
新 current 出现后旧 cursor 仍固定旧快照。两个 marker snapshot、6 条 entry、2 条审计和 2 条幂等记录
已定向清理为 0，原 1 点 current snapshot 已恢复。该项关闭目录读取高基数风险，但不改变现场真实目录
仍只有 1 点、校准和来源序列未就绪，G-021 保持 `PARTIAL`。证据见
[BPI 点位目录高基数分页验收](testing/bpi-point-catalog-pagination-acceptance.md)。

同日源码侧新增 Flyway V19 数据质量事件工作台：严格校验 `bpi.data-quality.v1` 的 topic、key、
headers、Protobuf、scope、payload 和时间偏移，非法消息隔离到 DLQ；合法事件按租户、工厂、产线、
工段、来源、设备、属性和问题类型聚合，并保留不可变 raw fact、生命周期和审计。列表使用 HMAC
scope-bound keyset cursor，页面支持汇总、筛选、详情、分派、重新分派和解决。真实本地 PostgreSQL +
Embedded Kafka 6/6、Java 8 adapter 完整模块 18/18、模拟器 9/9 和浏览器 13/13 已通过。目标测试环境
又以 `297e0aaf` 升级到 Flyway V19；marker `ADP_E2E_DQ_20260719_215100_297E0AAF` 在真实 Kafka
partition 2 offset `5 -> 6`、ADP 登录、浏览器/API 和 PostgreSQL 完成
`OPEN/r1 -> ACKNOWLEDGED/r2 -> RESOLVED/r3`，raw event 保持 1，console/page/request error 均为 0。
随后 incident、raw、action、audit、idempotency 和 inbox 定向清零，consumer 恢复关闭和 deny-all，
六个分区 lag 与 DLQ 均为 0。该工作台阶段状态为 `PASS_TARGET_POSTGRES_KAFKA_BROWSER_CLEANUP`；当时真实 Flink
自动产出和 7-14 天现场影子运行尚未闭合，不能据此把 BPI 总目标写成生产 READY。证据见
[BPI 数据质量事件工作台验收](testing/bpi-data-quality-workbench-acceptance.md)。

随后同日完成真实 Flink 自动数据质量全链验收。源码 `ddec7709` 增加来源序列缺口/重复/冲突/乱序、
source epoch 回退、时钟漂移、点质量和 max-silence 状态检测，并从 canonical savepoint
`savepoint-990e6d-74b13f8ce600` 以 `allowNonRestoredState=false` 将目标作业从 33 扩展到 36 tasks。
最终 marker `ADP_E2E_DQ_FLINK_20260719_2344_3ca0fff3` 在真实
`1000 / PLANT-01 / LINE-S07-01` 范围内由 selected telemetry 触发，Flink 恰好提交
`SOURCE_SEQUENCE_GAP`、`POINT_QUALITY_BAD`、`CLOCK_DRIFT`、`SOURCE_SEQUENCE_DUPLICATE` 四条事件；
Java 17 consumer 将其落为 4 个 `OPEN/r1/event_count=1` incident、4 个 raw、4 个 inbox、4 个 action
和 4 个 audit，真实 BPI 页面精确显示 4 行并打开 raw 详情，所有 API 为 2xx 且浏览器错误为 0。
验收脚本同时修复 scope 透传、Compose 意外重启和共享产线 context revision 三项问题，并强制回放前后
job ID 不变；consumer 最终恢复关闭和 deny-all，13 组诊断/最终 marker 数据定向清理为 0。由此“真实
Flink 自动 producer 未验收”退出缺口；现场 READY 点位、同 scope candidate/batch、7-14 天影子运行和
QCS/WMS 写回仍未完成，G-021 继续保持 `PARTIAL`。证据见
[BPI Flink 自动数据质量全链验收](testing/bpi-flink-data-quality-acceptance.md)。

2026-07-20 继续完成 Flyway V20 和影子运行验收产品闭环。新增 `/bpi/#/shadowRuns` 页面、10 个
版本化 API、`bpi_shadow_runs` 与 `bpi_shadow_run_batch_reviews`，把规则/拓扑/点位目录固定、9 项
启动准入、7-14 天观察、人工边界与参考量复核、关键数据质量阻断、四眼批准、revision、幂等和审计
纳入同一任务。目标 marker `ADP_E2E_SHADOW_20260720_0152_V20` 从真实页面闭合
`DRAFT/r1 -> RUNNING/r2 -> EVALUATING/r13 -> APPROVED/r14`：10 批有效复核、19/20 边界通过、
认同率 `0.95`、自动量/参考量均 `1000 t`、累计量偏差 0；未解决 CRITICAL 事件使批准返回 `422`，
页面处置为 `RESOLVED/r3` 后才允许独立管理员批准。PostgreSQL 直查为 16 条审计、10 条成功幂等，
10 个来源批次继续保持 `CLOSED_RAW / NOT_APPLICABLE / NOT_REQUESTED`，没有 WOM/QCS/WMS 写回；
57 个浏览器响应均为 2xx，浏览器错误为 0，最终十类 marker 残留均为 0。该验收使用数据库受控压缩
8 天，只关闭软件门槛和产品交互，不构成现场连续运行证据；真实 READY 点位、同 scope 实时
candidate/batch、真实 END 边界、连续 7-14 天和 QCS/WMS 写回仍未完成，G-021 保持 `PARTIAL`。证据见
[BPI 影子运行与人工验收闭环](testing/bpi-shadow-run-acceptance.md)。

同日继续完成 Flyway V21 运行开关治理。新增 `/bpi/#/featureFlags`、分层覆盖表、审计与幂等写链，
以 `GLOBAL < TENANT < PLANT < LINE` 解析 6 个开关。`bpi.commands` 与 `bpi.rule-management` 已在
真实后端执行点强制校验；`bpi.shadow-only=true`、`bpi.auto-confirm=false` 和 `bpi.wms-link=false`
保持 Phase 1 不可编辑门禁；`bpi.ui` 明确标为 `PENDING_SHELL_INTEGRATION`，不把旧 ADP 壳未接入
伪报为完成。目标 marker `ADP_E2E_BPI_FLAGS_20260720_034527_0cf61838` 经真实页面完成 LINE 级
禁用和恢复继承，PostgreSQL 清理前为开关/审计/幂等 `1/2/2`，定向清理后为 `0/0/0`；桌面和
移动页面错误均为 0，WOM/QCS/WMS/PLC/DCS 写入均为 0。该治理能力已成为发布回归门禁，但不改变
现场连续影子运行和生产写回尚未完成的结论。证据见
[BPI 运行开关治理目标验收](testing/bpi-feature-flag-governance-acceptance.md)。

同日继续关闭 `bpi.ui` 的旧平台执行缺口。提交
`df6fdb0e5ddb929626dd0ea3c81b170afbaa62a4` 在 Java 8 adapter 接管旧 MES
`/inter-api/rbac/v1/menus/currentUser` 读取点，按认证主体的服务端 tenant/plant/line scope 解析
Java 17 `bpi.ui` 有效值，只注入或移除 BPI 菜单，并保留 Nginx 到 gateway 原菜单的进程级回退。
marker `ADP_E2E_BPI_SHELL_20260720_050100_df6fdb0e` 经真实页面闭合
`ENABLE -> DISABLE -> INHERIT`，PostgreSQL 清理前为开关/审计/幂等 `1/3/3`、清理后
`0/0/0`；原生菜单同步为 `28/0 -> 29/1 -> 28/0`，iframe 打开“实时生产态势”。只停止
adapter 的回退演练仍返回 gateway 的 28 个原菜单，恢复后 adapter healthy。最终测试环境又通过真实页面
保留 LINE `bpi.ui=true/active/r1`，桌面与移动 BPI 动作错误为 0，Flink job 保持
`RUNNING 36/36`。登录后旧 portal 的 `userPortal 401` 作为独立既有问题保留，不计入 BPI 零错误结论。
该闭环只完成导航治理，不改变现场连续影子运行和生产写回仍未完成的结论；G-021 继续保持 `PARTIAL`。
证据见 [BPI 旧 MES 原生菜单开关目标验收](testing/bpi-shell-menu-gate-acceptance.md)。

同日提交 `94e2b2288bf52966be58e9dda97039a5455466a8` 完成批次详情“质量与库存”产品页，接入
`GET /bpi/v1/batches/{batchId}/release`，以确定性模拟器覆盖 `CLOSED_RAW` 空态、`WAIT_QA`、
质量拒绝、WMS `PENDING/REJECTED` 和带 durable document 的 `INBOUNDED` 六态。页面在 release
投影慢响应或局部 `503` 时仍先显示批次事实、边界证据和时间线，并可局部重试；关闭竞态、390x844
移动布局和“HTTP 200 不等于已入库”的失败关闭规则均通过。完整浏览器回归为 17/17，模拟器回归为
12/12。该证据只证明产品页与合同的确定性行为，不证明目标环境 Flyway V23、真实 QCS/WMS 或业务单据；
Phase 2 继续默认关闭，G-021 保持 `PARTIAL`。证据见
[BPI 批次质量与库存页面验收](testing/bpi-quality-inventory-ui-acceptance.md)。

同日又以 `2096a8cd6274712657f8e4ffeb5e4ce40f72dc2f` 镜像源码和
`0c8c391291152d41dfcdf1e6fe2a3387be7944ce` 验收源码，将唯一 `adp-mes-newbase` 测试运行栈
受保护地从 Flyway V22 expand-only 升级到 V23。真实 Java 8 adapter 已精确放行
`GET /bpi-api/batches/{batchId}/release` 并拒绝嵌套路由；真实批次页在 service/adapter 重启前后均
为 200、浏览器错误 0。目标 PostgreSQL 15.18 又执行 4 个唯一 tenant/batch marker，4/4 事务测试和
独立 12 表零残留查询通过，原影子批次与全局关闭开关不变。由此“Java 8 入口适配、目标 V23/静态包
部署、目标页面/API/PostgreSQL 软件合同验收”已退出 G-021 阻断项；真实 QCS 检验单、WMS 入库单、
外部查单/补偿和开关激活仍未完成，G-021 继续保持 `PARTIAL`。证据见
[BPI 质量放行与完工入库验收](testing/bpi-quality-release-wms-inbound-acceptance.md) 和
`metadata/bpi-quality-release-wms-target-acceptance.json`。

同日晚些时候，实现提交 `1ce3cb996ff81556763283a5401f7c19554099c2` 继续关闭“目标 WMS 没有
可维护适配器和真实落单”缺口：material 迁移 192 增加 `sourceSystem/idempotencyKey/unit` 合同与精确
唯一索引，受 API key 保护的查单接口和 query-first WMS adapter 均进入 Docker 编排。受控认证 QCS
marker `ADP_E2E_20260720_215500_BPI_WMS` 经 BPI 事务 outbox、目标三 broker Kafka 和 adapter，
在目标 `material-wms` 真实写入唯一完工入库单、明细、库存事务和批次库存 `12.345 kg`；durable receipt
使批次进入 `INBOUNDED/r4`。完全相同 QCS 重放和强制 Kafka offset `1 -> 0 -> 1` 重放均未增加
单据、明细、事务或库存。浏览器 marker `ADP_E2E_UI_20260720_222600_BPI_WMS` 又通过五个 200 API
显示 `ACCEPTED/INBOUNDED`、单据和时间线，提交
`ad36372936d99ca947d231fc552ae9c3e086c2cc` 修复空证据文本纵向挤压。取证后两个 marker 在 BPI
与 material 表均清理为 0，Phase 2/WMS 开关全关、allowlist/route 恢复 deny-all。

因此 G-021 的当前剩余缺口不再包括“内部 material-wms 未落单”或“Kafka command 未重放”；仍保留
外部 QCS 实例主动事件与真实检验记录所有权联接、外部 ERP/WMS 冲销/宕机补偿、物理设备与正式校准
以及连续 7-14 天现场运行，目标状态继续为 `PARTIAL`，不能标记生产 READY。

2026-07-21 又完成 Flyway V25 完工入库冲销软件链：`INBOUNDED` 批次经 REQUEST、不同管理员
APPROVE、独立红单 outbox 和 WMS accepted/rejected receipt 迁移到 `INBOUND_REVERSED` 或恢复
`INBOUNDED`，原蓝单始终只读保留。全新 PostgreSQL 16.13 从 V1-V25 迁移通过，定向 API/落库
`10/10`、模拟 API `14/14`、浏览器 `19/19`，桌面/390px 移动端和清理均通过。由此 BPI 本地冲销
审批/状态机缺口已关闭；V25 目标部署、正式双身份、真实 Kafka 与外部 ERP/WMS 联调仍未关闭，所有
Phase 2/WMS 开关保持默认关闭。证据见
[BPI 完工入库冲销验收](testing/bpi-wms-inbound-reversal-acceptance.md)。

2026-07-21 又补齐两个异常治理基线。marker `ADP_E2E_20260721_015610_WMS_RECON` 证明管理员可在
真实批次页核对并重新排队原 WMS event/key；marker `ADP_E2E_20260720193226_WMS_OUTAGE` 真实停止
内部 `material-wms`，证明首次命令进入 DLQ 且零落单，恢复后重排原命令只生成一套 `12.345 kg`
入库事实。随后提交 `bcf40e2f91a18163fb8523598d06a980414d9ce6` 将目标升级到 Flyway V24，marker
`ADP_E2E_20260721053253_BPI_FORCE_CLOSE` 从真实页面申请强制结束：待审批时 batch 保持
`ACTIVE/r2`，同一申请人批准 HTTP 403，不同 `BPI_ADMIN` 批准后 task 为 `COMPLETED/r2`、batch 为
`CLOSED_RAW/r3`，批准边界、event/audit/idempotency 均真实落库，QCS/WMS/outbox 为 0。Java 8 adapter
精确路由修复提交 `1962f599b3ea90b1863548f45998f6e0fa89cc1d` 已部署并通过 31/31 测试；取证后 marker
残留为 0，全部 Phase 2/WMS 开关仍关闭。该能力是异常人工兜底，不替代自动 END 边界和现场运行门禁。

同日 marker `ADP_BPI_INTEGRATED_ROLLBACK_20260721_2054` 又完成真实候选/批次负载下的跨组件整体
回切。测试窗口同时回退 Java 17 Service、兼容 Java 8 Adapter `1962f599` 和旧 Flink JAR；旧 Flink
由目录、规则、生产上下文和遥测生成恰好 1 个候选，真实候选页确认后 PostgreSQL 形成
`CONFIRMED/r2` 与 `ACTIVE/r1/SHADOW` 批次。随后从双 savepoint 恢复当前 Flink JAR，并精确恢复
当前 Service 与带 `X-BPI-Adapter-Contract-Version: 1` 的 Adapter `d63ea4b0`；恢复前后候选、批次、
边界证据、状态事件、审计的 ID、revision 和计数完全一致，旧栈/当前栈浏览器 API 均为 200 且错误为 0。
marker 定向清理后 8 类残留为 0，原 Adapter scope、Flink `36/36`、Flyway V24、六个关闭开关和
watchdog 状态均复原。因此“真实业务负载跨组件整体回切”退出 G-021 缺口；物理设备、正式校准、现场
连续 7-14 天、外部 ERP/WMS 和生产激活仍未完成。证据见
[BPI 整栈回滚验收](testing/bpi-integrated-rollback-acceptance.md)。

同日 marker `ADP_BPI_FORMAL_IDENTITY_20260721134944` 又关闭正式身份系统第二管理员会话缺口。
验收通过组织、认证和 RBAC API 临时创建第二个 `systemRole` 用户；`admin` 与该用户分别取得真实
ADP legacy ticket，并在两套独立浏览器会话中经 `/bpi-api` 完成 REQUEST 和 APPROVE。PostgreSQL
中间态为 `ACTIVE/r2 + PENDING_APPROVAL/r1`，终态为 `CLOSED_RAW/r3 + COMPLETED/r2`，
`requested_by=legacy-ticket:admin`、`decided_by=legacy-ticket:bpi_reviewer_20260721134944`。临时身份、
marker 和作用域清理为 0，Adapter 镜像/基线 scope 精确恢复，六个写回开关始终为 false。因此正式
ADP 双会话审批退出 G-021 缺口；现场物理来源、正式校准、连续 7-14 天和外部 ERP/WMS 仍未完成。
证据见 [BPI 正式身份双管理员强制结束验收](testing/bpi-formal-identity-force-close-acceptance.md)。

权威设计和验收入口：

- [BPI 总设计](designs/batch-process-intelligence.md)
- [BPI 交互设计](designs/bpi-interaction-design.md)
- [BPI API 目录](api/bpi-api-catalog.md)
- [BPI 工程测试计划](testing/bpi-engineering-test-plan.md)
- [BPI 点位目录重复观测与准入撤销验收](testing/bpi-point-catalog-repeated-observation-acceptance.md)
- [BPI 单 Broker 故障恢复验收](testing/bpi-broker-failure-recovery-acceptance.md)
- [BPI 应用组件回滚验收](testing/bpi-application-rollback-acceptance.md)
- [BPI 整栈回滚验收](testing/bpi-integrated-rollback-acceptance.md)
- [BPI 试点平台前置条件与单位别名验收](testing/bpi-pilot-platform-prerequisites-acceptance.md)
- [BPI 点位校准治理与失败关闭验收](testing/bpi-point-calibration-governance-acceptance.md)
- [BPI 点位校准高基数分页验收](testing/bpi-point-calibration-pagination-acceptance.md)
- [BPI 点位目录高基数分页验收](testing/bpi-point-catalog-pagination-acceptance.md)
- [BPI 数据质量事件工作台验收](testing/bpi-data-quality-workbench-acceptance.md)
- [BPI Flink 自动数据质量全链验收](testing/bpi-flink-data-quality-acceptance.md)
- [BPI 影子运行与人工验收闭环](testing/bpi-shadow-run-acceptance.md)
- [BPI 运行开关治理目标验收](testing/bpi-feature-flag-governance-acceptance.md)
- [BPI 旧 MES 原生菜单开关目标验收](testing/bpi-shell-menu-gate-acceptance.md)
- [BPI 批次质量与库存页面验收](testing/bpi-quality-inventory-ui-acceptance.md)
- [BPI 质量放行与完工入库验收](testing/bpi-quality-release-wms-inbound-acceptance.md)
- [BPI 完工入库冲销验收](testing/bpi-wms-inbound-reversal-acceptance.md)
- [BPI 批次受控强制结束目标验收](testing/bpi-force-close-acceptance.md)
- [BPI 正式身份双管理员强制结束验收](testing/bpi-formal-identity-force-close-acceptance.md)
- `metadata/project-goal-acceptance.json` 中的 `G-021`

## 非目标

当前阶段不做这些事：

- 不把 `backend/modules/**/META-INF/maven/**/pom.xml` 全量纳入 Maven reactor。
- 不提交 Windows 二进制运行包、fat jar、exe、dll、内置 JDK。
- 不把 Oracle 当默认数据库继续适配。
- 不在没有业务说明和落表证据时声称完整 MES 业务闭环已完成。
- 不通过重置数据库来掩盖 PostgreSQL 兼容缺口。
- 不把生产迁移前置项混入当前测试环境闭环；数据迁移、回滚、license、MinIO、Keycloak 生产库、TLS、安全加固和业务签字需要单独补。

## 工作流

### 1. 仓库工程化

目标：让仓库具备持续开发入口。

已具备：

- 根 `pom.xml` 父级 POM。
- `backend/source-modules/` 可编译源码承接区。
- `deploy/docker/` PostgreSQL-first Compose 编排。
- `Makefile` 统一验证、部署、smoke 命令。
- GitHub Actions 验证 Maven reactor 和 Compose 语法。
- `scripts/verify-sustainable-repo.py` 验证仓库治理硬约束。
- `scripts/create-backend-source-module.py` 创建标准后端源码模块。
- `scripts/precheck-module-intake.py` 对新业务包或恢复模块做只读准入预检。
- `metadata/module-intake-latest-basic-modules.json` / `docs/module-intake-latest-basic-modules.md` 记录真实 `MES包/最新基础模块` 的 report-only 准入结果、扫描覆盖和阻断项。
- `scripts/verify-source-modules.py` 校验已提升后端源码模块，并阻止默认源码路径重新带入 Oracle 驱动、配置和 mapper 资源。
- `scripts/generate-current-content-inventory.py` 生成当前迁移内容库存。
- `scripts/generate-backend-dependency-inventory.py` 生成恢复后端模块依赖库存。
- `scripts/generate-oracle-migration-audit.py` 生成 Oracle 迁移 backlog。
- `scripts/generate-postgres-migration-inventory.py` 生成 PostgreSQL 初始化脚本索引和 watch 语句说明。
- `scripts/generate-oracle-replacement-status.py` 生成 Oracle 替换状态总账。
- `deploy/docker/scripts/adp-platform-validation-smoke.js` 汇总平台 API、菜单和待办 smoke，输出统一平台验证报告。

后续增强：

- 每提升一个后端模块，就给模块补单元测试或最小集成测试。
- 每新增一个业务运行包，先跑 `make module-intake-check INTAKE=/path/to/package-or-dir`，再补对应 smoke 脚本或测试清单。
- 已存在的 `MES包/最新基础模块` 先按准入账本处理 7z 不可检查包和 `DataSet_6.1.2.2` 中的 `jdbc:oracle:thin` 阻断项，再进入默认源码区。
- 每次测试环境修复后，优先跑 `make smoke-platform` 形成平台验证报告，再进入业务模块 smoke。

### 2. Oracle 替换

目标：逐步把运行环境、Mapper、SQL、配置从 Oracle 默认切换到 PostgreSQL。

原则：

- PostgreSQL 是默认路径。
- Oracle 只允许显式 legacy profile 或 `.env.oracle-legacy.example`。
- 父 POM 默认 `dependencyManagement` 不管理 Oracle JDBC，Oracle 驱动只能在 `oracle-legacy` profile 下出现。
- 方言差异集中到 DAO/Mapper/migration 层。
- 可编译源码模块的默认 `src/main` 不允许带入 Oracle JDBC URL、driver、Hibernate dialect 或 `mapper/oracle` 资源。
- 每个模块必须有审计结果、迁移脚本和 smoke 证据。
- 每个 PostgreSQL 缺表、缺列、类型不兼容或 Oracle 方言残留，都落到幂等 SQL 或模块 backlog，不用清库重建掩盖；`make postgres-migration-check` 会阻断清库类高风险语句，并要求 watch 语句是 `DROP ... IF EXISTS` 或 `DELETE ... WHERE`。

### 3. 后端落表业务排查

目标：形成“页面/API/服务/Mapper/表/字段/业务含义”的映射。

这块建议交给专门线程推进，输入和产出见 [后端落表业务排查交接](backend-table-audit-handoff.md)。

### 4. 产品说明书

目标：基于真实运行模块和落表证据，逐步补产品说明。

说明书应区分：

- 平台能力：组织、权限、菜单、流程、实体配置、通知、打印、任务调度等。
- 业务模块：生产、设备、质量、能源、安环、仓储、追溯等。
- 已验证功能、仅菜单可见功能、缺数据/缺脚本功能、未接入功能。
- 生产模块的完整功能测试用例、业务前置数据、页面/API/表证据和未通过项 backlog。

### 5. 功能验收和落库证明

目标：把“页面能不能用”和“业务动作有没有真实写库”拆成可复验的证据。

固定产出：

- [前端功能测试报告](frontend-functional-test-report.md)
- [后端落库验收报告](backend-table-audit/persistence-acceptance.md)
- [机器可读落库验收记录](../metadata/persistence-acceptance.json)
- [生产模块 Backlog 账本](production-module-backlog.md)
- [机器可读生产模块 Backlog](../metadata/production-module-backlog.json)
- [业务模块接入验收要求](business-module-intake-requirements.md)
- [机器可读业务模块接入要求](../metadata/business-module-intake-requirements.json)
- [生产迁移就绪账本](production-migration-readiness.md)
- [机器可读生产迁移就绪记录](../metadata/production-migration-readiness.json)
- [生产切换总闸门](production-cutover-gate.md)
- [机器可读生产切换总闸门](../metadata/production-cutover-gate.json)

原则：

- 必须用真实前端页面或等效 E2E 操作，不用源码静态阅读代替功能测试。
- 必须记录 console error、network error、API、payload、response 和页面实际结果。
- 对新增、编辑、删除、禁用、启用、状态变更等动作，必须追踪 Controller/Service/Mapper/SQL/目标表，并查询 PostgreSQL。
- `PASS` 只代表真实前端操作、后端链路和 PostgreSQL 证据都齐全；未执行、被阻断或不落库功能必须分别标记 `BLOCKED` 或 `NOT_APPLICABLE`。

## 验收口径

仓库级验收：

- `make verify` 通过。
- `make ci` 通过。
- `make sustainable-check` 通过。
- `make project-goal-acceptance-check` 通过。
- `make goal-gap-register-check` 通过。
- `make source-module-check` 通过。
- `make source-module-test` 通过。
- `make runtime-script-check` 通过。
- `make persistence-acceptance-check` 通过。
- `make production-testcase-check` 通过。
- `make production-blocker-check` 通过。
- `make production-module-backlog-check` 通过。
- `make business-module-intake-requirements-check` 通过。
- `make production-action-map-check` 通过。
- `make production-migration-readiness-check` 通过。
- `make module-intake-check` 对新接入业务包或模块通过，或已有 report-only 证据和 backlog。
- `make module-intake-precheck-regression-check` 通过，证明准入预检能拦住 Oracle POM 和 `mapper/oracle` 默认路径污染。
- `make module-intake-candidate-report-check` 通过，证明真实候选包准入阻断项和扫描覆盖已进入仓库账本。
- `make inventory-check` 通过。
- `make backend-dependency-check` 通过。
- `make oracle-audit-check` 通过。
- `make postgres-migration-check` 通过。
- `make oracle-replacement-check` 通过。
- GitHub Actions `Verify` 通过。
- 新模块能继承父 POM 并纳入 `backend/source-modules`。
- Docker Compose 默认渲染为 PostgreSQL。
- 父 POM 默认依赖管理不暴露 Oracle JDBC。
- Oracle 配置只出现在 legacy 文档、模板、默认源码路径之外的对照资料或待迁移清单中。

数据库迁移验收：

- 每个模块有 PostgreSQL 迁移脚本或明确无表变更说明。
- 迁移脚本幂等。
- SQL audit 阻断项有处理记录。
- 对应页面/API smoke 通过。
- 平台修复后的 `make smoke-platform` 报告通过，或失败项已进入幂等 SQL/backlog。

业务说明验收：

- 每个业务模块有菜单/API/表/字段/流程说明。
- 生产模块有完整功能测试用例矩阵，并按真实前端操作逐项验收。
- 生产模块未闭合项必须进入 Backlog；接口 `200` 但不落库的动作不得混入已完成项。
- 每条核心业务链路有输入、操作、状态变化和数据库结果。
- 每个会改变业务数据的前端动作都有 PostgreSQL 落库验收记录。
- 未验证或缺包的部分明确标注，不混进已完成功能。

生产迁移验收：

- [生产迁移就绪账本](production-migration-readiness.md) 覆盖 PostgreSQL 数据迁移脚本、回滚方案、license 策略、MinIO 文件迁移、Keycloak 生产库策略、Nacos/runtime patch 生产化、端口/域名/TLS、安全加固和业务 smoke 签字。
- `metadata/production-migration-readiness.json` 通过 `make production-migration-readiness-check`。
- 整体状态保持 `NOT_READY_FOR_PRODUCTION_MIGRATION`，直到所有轨道都有脚本、演练或签字证据。
- 生产迁移脚本、回滚脚本和配置模板不得提交真实密码、token、证书私钥。

## 推荐优先级

1. 基础平台：登录、用户、组织、权限、菜单、系统配置、流程、待办。
2. 低代码实体配置：`ec_module`、`ec_entity`、`ec_model`、`ec_field`、`ec_view` 等元数据。
3. 业务基础模块：BaseSet、TeamInfo、Qualify、TagManagement、HierarchicalMod。
4. 质量域：LIMS、QCS、LIMSDC、LIMSInterface、LIMSMaterial、LIMSSTDS。
5. 生产模块完整功能测试用例和主流程验收。
6. 设备/能源/安环等业务运行包。

## 当前下一步

当前只保留两条有明确完成条件的执行主线：

### 主线 A：BPI Phase 0/1

1. 保持已通过的同一 marker `UI -> Outbox -> Kafka -> Flink -> application receipt -> PostgreSQL -> candidate confirm -> batch/evidence/audit` 联合验收，以及 `selected telemetry -> Flink data quality -> Kafka -> PostgreSQL -> browser` 自动质量链，作为每次发布的回归基线。
2. 保持已通过的目标环境 Flyway V24、savepoint 恢复、点位目录与来源序列证据自动同步、真实 ADP 会话、页面拓扑创建/校验、稳定版本比较、规则 simulation 证明、职责分离审批、受控退役、正式 ADP 双会话强制结束四眼审批、typed inactive、PostgreSQL revision、运行开关治理和重启读取作为发布回归；日常配置不得回退到 SQL fixture、直接改表或手工伪造 READY 快照。
3. 保持已通过的单 broker 故障、service/adapter/Flink 双 savepoint 整栈回切、入口流量恢复、真实候选确认和 PostgreSQL 不变性为发布回归；生产切换前只需在生产等价维护窗口复跑并取得业务签字。
4. 以已通过的 `MapleTcT/iot` 受控 MQTT 双会话链和 `mes-production-context-outbox` 为基线配置试点产线；当前产品、设备、`instantFlow` metadata、单位及受控 `source_epoch + sequence` 重连语义已进入自动链，下一步补真实校准并用物理 DEVICE/GATEWAY 重复同等验收。
5. 受控 MQTT 与 WOM context 的同指令 START/END 已通过；下一步用物理设备和正式校准替换受控来源，按同一生产指令重复 Kafka、Flink、BPI PostgreSQL candidate/batch、自动 END 和浏览器证据链，再连续运行 7-14 天影子批次。
6. 达到现场边界人工认同率、累计量偏差和数据质量门槛后，才接入外部 QCS 主动事件及外部 ERP/WMS 写回、查单、拒绝、冲销和补偿。

### 主线 B：既有生产/质量核心链

1. 继续按 [功能验收与落库验收规则](functional-persistence-acceptance.md) 对真实页面执行唯一 marker 测试。
2. 闭合制造指令、投料/报工、请检、合格/不合格处置、完工入库和批次追溯，而不是扩散到低优先级模块。
3. 同步更新前端报告、后端落库报告和机器可读账本；接口 `200` 但未落库必须记为 `FAIL`。

后端落表业务排查和仓库治理继续作为两条主线的支撑，不再替代产品功能开发、真实运行和落库证明。
