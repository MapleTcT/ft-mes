# MES 模块包缺口审计

- 生成时间：`2026-07-16T23:36:34+08:00`
- 仓库基线：`299b0c8dd3993a819a58bf589d7b39bcbbfea580`
- 新包接入基线：`mes-modules-patrol-intake@0b8f37ebd13f3a9b72ae1a02cd59713e59fb68c0`
- 当前测试环境：公司内网 `v6@10.11.100.17`；历史 Tailscale 地址仅保留在对应旧验收记录中。

机器可读记录见 [`metadata/module-package-gap-audit.json`](../metadata/module-package-gap-audit.json)。

## 结论

当前并不是“所有业务模块都缺”。基础平台、生产、质量、设备和安环作业票的主体包已经进入源码仓库并在测试环境运行。新提供的 PATROL 与 EMS 原厂包也已经完成完整性扫描和源码接入。剩余边界分为五类：

1. **源码恢复、已部署、继续验收**：共享巡检 `PATROL` 已完成 Java 8 构建、目标 PostgreSQL 迁移、EamMs 部署，并以真实页面/API/marker 闭合输入标准、路线/区域/项目配置、计划、任务生成、查询、取消、下发、执行、结果录入、完成和复显；异常/隐患处置及统计仍不能从这些通过链外推。
2. **源码恢复但被依赖阻断**：`supEMS`、`energyPlan`、`EnergyConBase`、`EnergyPred` 已恢复，但缺少真实 `Indicator 6.0.4.0` api/core，且没有 PostgreSQL 初始化 SQL。
3. **其他依赖包缺失**：`packConfigManag`、`SESGISConfig`，分别影响较新 WOM 包和 WTS 地图能力。
4. **包和源码存在但未部署**：`WOMmobile`，当前因版本冲突保留在源码仓库，不应误报为已上线。
5. **原厂包未提供但已有自研替代**：物料/WMS、`ProcessAnalysis`。两者已有可维护源码、运行服务和 PostgreSQL 数据证据，不再属于当前功能空白，但仍需继续业务验收。

因此，PATROL 已不再是“缺包”或“待部署”，当前重点是扩大目标环境真实业务验收覆盖；能源模块也不再是“缺源码”，但仍被 Indicator 与 PostgreSQL 迁移缺口硬阻断。

## 审计范围

| 证据面 | 范围 | 当前结果 |
|---|---|---|
| 原始交付包 | `/Users/zhangchu/Documents/MES包`、`/Users/zhangchu/Downloads/ADP`、`supPlant-Patrol V6.0.4.0-210616-C`、`supEMS V6.0.4.0-210617-C` | 原审计 365 个 ZIP；新增 5 个目标 ZIP 均通过完整性检查 |
| 恢复源码 | `mes-modules-source-repo/modules` + `mes-modules-patrol-intake/modules` | 合并去重后 45 个模块版本目录、39 个唯一模块代码；新接入 PATROL 和 4 个 EMS 模块 |
| 可持续源码 | `backend/source-modules` | `material-wms`、`process-analysis`、BPI adapter/outbox 等自研模块 |
| 远程运行目录 | `/home/v6/adp-mes-docker-newbase-20260611-181921/runtime/bap-server/module-Server` | 26 个模块运行目录 |
| 模块注册表 | PostgreSQL `mod_module_registry` | 33 个 `BIZ` 模块 |
| 服务发现 | Nacos `prod` group | 当前 91 个服务；WOM、RM、QCS、LIMS、EAM、WTS、material、ProcessAnalysis 等均有 1/1 健康实例 |
| 前端菜单 | 最近一次 `admin` 会话菜单树 | 506 个节点；PATROL 迁移前无 PATROL/WOMmobile 菜单；存在完工入库台账、生产过程追溯和能源节点页面 |
| 数据库 | PostgreSQL 隔离克隆 + 目标 `adp` | PATROL 目标 37 表、24 菜单、102 操作、2 工作流及真实任务 marker PASS；能源节点表存在但为空 |

归档扫描中的 2 个错误来自名为 `importProject.zip`、实际并非 ZIP 的历史文件，不影响本次目标模块判断。

## 领域覆盖

| 领域 | 包/源码 | 当前运行 | 判定 | 说明 |
|---|---|---|---|---|
| 基础平台 | BaseSet、DataSet、DocManage、HierarchicalMod、TeamInfo 等 | 已注册并运行 | `PRESENT` | 仍需按页面/API/落库逐项验收，不能由模块存在推导全部功能可用 |
| 生产制造 | WOM、RM、craftGraph | 已注册，核心服务 1/1 健康 | `PRESENT_PARTIAL` | WOM `6.1.3.4` 受 `packConfigManag` 缺包影响；当前运行版本不等于最新版完整能力 |
| 质量管理 | LIMS 系列、QCS、Qualify | 已注册，QCS/LIMS 1/1 健康 | `PRESENT` | 当前以 QC/LIMS 为主，QA 产品能力仍需单独规划，不能从包名推导完整 QA |
| 设备管理 | EAM、maintenance、SpareManage、OverhaulTicket、PATROL | EAM 健康；PATROL 由 EamMs 承载，配置、任务状态和现场结果链 PASS | `PRESENT_PARTIAL` | 设备主体和共用巡检执行链可用；异常/隐患与统计仍需验收 |
| 安环作业票 | WTS、workAppointment、PATROL | WTS 健康；PATROL 安环巡检共享核心已部署 | `PRESENT_PARTIAL` | 作业票主体存在；GIS 仍缺 `SESGISConfig`；安环巡检入口需继续按同一 PATROL 数据域复验 |
| 共享巡检 | `PATROL_6.0.4.0` | 目标 PostgreSQL/EamMs 已部署；任务取消及执行完成 marker 均 PASS | `SOURCE_RECOVERED_RUNTIME_PARTIAL` | 37 表、7 实体、27 模型、74 视图、24 菜单、102 操作、2 工作流；配置、计划、任务状态、结果录入和完成链 PASS，异常/隐患及统计继续验收 |
| 能源管理 | `supEMS`、`energyPlan`、`EnergyConBase`、`EnergyPred` | 四模块源码已恢复；无可部署构件 | `SOURCE_RECOVERED_BLOCKED_DEPENDENCY` | 81 个 Java 文件直接依赖缺失的 Indicator；随包 SQL 无 PostgreSQL 版本 |
| 物料/WMS | 未发现原厂完整 WMS 包；已有 `material-wms` 自研源码 | `material` 1/1 健康，菜单可见 | `CUSTOM_REPLACEMENT_DEPLOYED` | `wms_batch_stocks=2`、`wms_inventory_transactions=4`、`wms_quality_results=2`、`wms_stock_documents=2` |
| 生产过程追溯 | 原包未形成独立源码模块；已有 `process-analysis` 自研源码 | `ProcessAnalysis` 1/1 健康，菜单可见 | `CUSTOM_REPLACEMENT_DEPLOYED` | 已不属于缺包，但仍需纳入核心主流程真实 marker 验收 |
| 移动生产 | WOMmobile 包和源码存在 | 无注册、无服务、无菜单 | `PRESENT_NOT_DEPLOYED` | 因版本冲突暂不部署，需与当前 WOM 基线做兼容性验证后再接入 |

## 逐项缺口

| 模块/依赖 | 包 | 源码 | 运行目录/JAR | 注册/服务 | 菜单 | 表 | 状态 | 处理建议 |
|---|---:|---:|---:|---:|---:|---:|---|---|
| `PATROL_1.0.0` | 是 | 是 | 已部署 | EamMs 承载 | 24 菜单启用 | 37 表目标验收 | `SOURCE_RECOVERED_RUNTIME_PARTIAL` | 保持配置和任务执行完成回归；继续异常/隐患、统计和经确认的目标回滚演练 |
| `supEMS` + 3 个能源子模块 | 是 | 是 | 否 | 否 | 包内存在 | PostgreSQL 未迁移 | `SOURCE_RECOVERED_BLOCKED_DEPENDENCY` | 先取得 Indicator `6.0.4.0` api/core，再按依赖顺序编译和迁移 |
| `Indicator_6.0.4.0` | 否 | 否 | 否 | 否 | 否 | 未确认 | `MISSING_DEPENDENCY` | 禁止用空接口桩绕过；取得原包后重新执行四个 EMS reactor 构建 |
| `packConfigManag` | 否 | 否 | 否 | 否 | 否 | 未确认 | `MISSING_DEPENDENCY` | 补齐与 WOM `6.1.3.4` 匹配的 `6.1.2.1/6.1.3.2` 包，之后重新做准入和编译 |
| `SESGISConfig` | 否 | 否 | 否 | 否 | 否 | 未确认 | `MISSING_DEPENDENCY` | 补齐服务端包后复验 WTS 地图；仅有前端/i18n 引用不能视为已交付 |
| `WOMmobile_5.0.0.0` | 是 | 是 | 否 | 否 | 否 | 未验收 | `PRESENT_NOT_DEPLOYED` | 建立与当前 WOM/基础平台的版本矩阵，隔离部署并做页面/API/PostgreSQL 验收 |
| `material-wms` | 自研 | 是 | 是 | 是 | 是 | 是 | `CUSTOM_REPLACEMENT_DEPLOYED` | 继续闭合请检、质量处置、完工入库、库存事务和批次追溯 |
| `process-analysis` | 自研 | 是 | 是 | 是 | 是 | 是 | `CUSTOM_REPLACEMENT_DEPLOYED` | 继续验证跨 WOM/QCS/WMS 的同一批次 marker 和反向追溯 |

## 原始包扫描证据

原审计搜索了：

- `PATROL_1.0.0`
- `EMS_1.0.0`
- `supEMS_1.0.0`
- `packConfigManag_6.1.2.1`
- `SESGISConfig_1.0.0`
- `WOMmobile_5.0.0.0`

当时唯一真实模块命中是 `supPlant-WOMmobile V6.1.2.2-211230-C.zip` 内层包中的 `WOMmobile_5.0.0.0`。本轮用户新增提供 `PATROL_6.0.4.0.zip` 以及 `supEMS`、`energyPlan`、`EnergyConBase`、`EnergyPred` 四个真实能源 ZIP，因此原“PATROL/EMS 缺包”结论已经失效。`packConfigManag`、`SESGISConfig` 和 `Indicator 6.0.4.0` 仍未找到。

引用关系也解释了页面现状：

- EAM、BaseSet、TeamInfo 引用 PATROL；新包确认该模块正是设备和安环共用巡检核心。
- 四个 EMS 包的依赖顺序为 `BaseSet -> HierarchicalMod -> DataSet -> Indicator -> supEMS -> energyPlan -> EnergyConBase -> EnergyPred`。
- WOM `6.1.3.4` 引用 `packConfigManag`，因此不能把该新包直接替换到当前运行环境。
- WTS/EAM 引用 `SESGISConfig`，因此作业地图缺口不是单纯前端显示问题。
- HierarchicalMod 提供能源节点页面和 `hm_energy_*` 表，但不包含完整 EMS 业务链。

## 当前运行证据

当前运行复核和写链验收经 `ssh v6@10.11.100.17` 完成：

- 核心模块健康实例：`WOM`、`RM`、`craftGraph`、`QCS`、`LIMS`、`EAM`、`WTS`、`material`、`ProcessAnalysis` 均为 `1/1`。
- PATROL 没有独立微服务名，原模块由健康的 `EamMs` 容器承载；部署 JAR SHA-256 为 `97d3a265...e43d2ab`。Nacos 中仍没有 WOMmobile、packConfigManag、SESGISConfig；`OEEMs` 是 OEE 服务，不能据此认定能源模块存在。
- `mod_module_registry` 的 33 个 BIZ 模块覆盖恢复源码中的 34 个唯一模块代码，唯一未注册的恢复模块代码是 `WOMmobile_5.0.0.0`。
- 当前 PATROL 有 24 个有效 EAM 菜单、102 个操作权限和 2 个工作流；WOMmobile 仍无菜单。
- PATROL 目标库已有 37 张实体表；`verify/001-patrol-acceptance.sql` 和真实任务状态 marker 均 PASS。`hm_energy_nets` 和 `hm_energy_nodes` 均为 0 行。

## 接入顺序

1. **不阻断当前生产质量主线**：继续闭合“制造指令 -> 投料/报工 -> 请检 -> 质量处置 -> 完工入库 -> 批次追溯”。
2. **扩大 PATROL 功能验收**：部署、配置 CRUD、计划/任务取消、任务下发/执行、结果录入和完成链已 PASS；继续完成异常/隐患、统计和经确认的目标回滚。
3. **补 Indicator 并恢复 EMS**：不得伪造依赖；四服务必须分别构建、迁移、健康检查和验收。
4. **补 WOM 依赖**：取得 `packConfigManag` 后，在隔离环境验证 WOM 新版本，不直接覆盖现有运行版本。
5. **补 WTS 地图依赖**：取得 `SESGISConfig` 后单独恢复 GIS 功能。
6. **最后接 WOMmobile**：先解决版本兼容、认证、菜单权限和移动端 API 合同，再上线。

每次新包到位后必须运行模块准入扫描，并按真实页面、API、PostgreSQL marker 和回滚步骤复验。包、JAR、Nacos 服务或菜单单独存在，都不能作为“功能可用”的结论。
