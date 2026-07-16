# MES 模块包缺口审计

- 生成时间：`2026-07-16T09:19:37+08:00`
- 仓库基线：`aa9061e3417c1c53b03cfad035cdc8cfbdfaa7a2`
- 测试环境访问约束：仅通过 Tailscale `100.99.133.43`；当前不使用公司内网地址 `10.11.100.17`。

机器可读记录见 [`metadata/module-package-gap-audit.json`](../metadata/module-package-gap-audit.json)。

## 结论

当前并不是“所有业务模块都缺”。基础平台、生产、质量、设备和安环作业票的主体包已经进入源码仓库并在测试环境运行。仍需补齐的主要边界分为四类：

1. **完整模块包缺失**：共享巡检 `PATROL`、完整能源管理 `EMS/supEMS`。
2. **依赖包缺失**：`packConfigManag`、`SESGISConfig`，分别影响较新 WOM 包和 WTS 地图能力。
3. **包和源码存在但未部署**：`WOMmobile`，当前因版本冲突保留在源码仓库，不应误报为已上线。
4. **原厂包未提供但已有自研替代**：物料/WMS、`ProcessAnalysis`。两者已有可维护源码、运行服务和 PostgreSQL 数据证据，不再属于当前功能空白，但仍需继续业务验收。

因此，当前最明显的产品级缺口是：**设备巡检与安环巡检共用的巡检核心没有交付；能源页面只有基础层级节点骨架，没有完整能源业务模块。**

## 审计范围

| 证据面 | 范围 | 当前结果 |
|---|---|---|
| 原始交付包 | `/Users/zhangchu/Documents/MES包`、`/Users/zhangchu/Downloads/ADP` | 扫描 365 个 ZIP，并递归检查 55 个内层 ZIP |
| 恢复源码 | `/Users/zhangchu/Documents/ADP/mes-modules-source-repo/modules` | 40 个模块版本目录，34 个唯一模块代码 |
| 可持续源码 | `backend/source-modules` | `material-wms`、`process-analysis`、BPI adapter/outbox 等自研模块 |
| 远程运行目录 | `/home/v6/adp-mes-docker-newbase-20260611-181921/runtime/bap-server/module-Server` | 26 个模块运行目录 |
| 模块注册表 | PostgreSQL `mod_module_registry` | 33 个 `BIZ` 模块 |
| 服务发现 | Nacos `prod` group | 当前 91 个服务；WOM、RM、QCS、LIMS、EAM、WTS、material、ProcessAnalysis 等均有 1/1 健康实例 |
| 前端菜单 | 当前 `admin` 会话菜单树 | 506 个节点；无 PATROL/WOMmobile 菜单；存在完工入库台账、生产过程追溯和能源节点页面 |
| 数据库 | PostgreSQL `adp` | 无 PATROL/`mp_*` 表；WMS 表有验收数据；能源节点表存在但为空 |

归档扫描中的 2 个错误来自名为 `importProject.zip`、实际并非 ZIP 的历史文件，不影响本次目标模块判断。

## 领域覆盖

| 领域 | 包/源码 | 当前运行 | 判定 | 说明 |
|---|---|---|---|---|
| 基础平台 | BaseSet、DataSet、DocManage、HierarchicalMod、TeamInfo 等 | 已注册并运行 | `PRESENT` | 仍需按页面/API/落库逐项验收，不能由模块存在推导全部功能可用 |
| 生产制造 | WOM、RM、craftGraph | 已注册，核心服务 1/1 健康 | `PRESENT_PARTIAL` | WOM `6.1.3.4` 受 `packConfigManag` 缺包影响；当前运行版本不等于最新版完整能力 |
| 质量管理 | LIMS 系列、QCS、Qualify | 已注册，QCS/LIMS 1/1 健康 | `PRESENT` | 当前以 QC/LIMS 为主，QA 产品能力仍需单独规划，不能从包名推导完整 QA |
| 设备管理 | EAM、maintenance、SpareManage、OverhaulTicket 等 | 已注册，EAM 1/1 健康 | `PRESENT_PARTIAL` | 设备主体存在，但共用巡检核心 PATROL 缺失 |
| 安环作业票 | WTS、workAppointment | 已注册，WTS 1/1 健康 | `PRESENT_PARTIAL` | 作业票主体存在；GIS 地图依赖 `SESGISConfig` 缺失；安环巡检仍受 PATROL 缺失阻断 |
| 共享巡检 | 交付包中仅发现 PATROL 引用 | 无源码、无注册、无服务、无菜单、无表 | `MISSING_FULL_STACK` | 需要取得原包或新建共享巡检模块，供设备/安环共同使用 |
| 能源管理 | 仅发现 EMS/supEMS 引用与层级能源节点骨架 | 无 EMS 服务；能源节点表为空 | `MISSING_FULL_STACK` | `hm_energy_*` 不能等同于计量、抄表、平衡、能耗分析和告警等完整能源产品 |
| 物料/WMS | 未发现原厂完整 WMS 包；已有 `material-wms` 自研源码 | `material` 1/1 健康，菜单可见 | `CUSTOM_REPLACEMENT_DEPLOYED` | `wms_batch_stocks=2`、`wms_inventory_transactions=4`、`wms_quality_results=2`、`wms_stock_documents=2` |
| 生产过程追溯 | 原包未形成独立源码模块；已有 `process-analysis` 自研源码 | `ProcessAnalysis` 1/1 健康，菜单可见 | `CUSTOM_REPLACEMENT_DEPLOYED` | 已不属于缺包，但仍需纳入核心主流程真实 marker 验收 |
| 移动生产 | WOMmobile 包和源码存在 | 无注册、无服务、无菜单 | `PRESENT_NOT_DEPLOYED` | 因版本冲突暂不部署，需与当前 WOM 基线做兼容性验证后再接入 |

## 逐项缺口

| 模块/依赖 | 包 | 源码 | 运行目录/JAR | 注册/服务 | 菜单 | 表 | 状态 | 处理建议 |
|---|---:|---:|---:|---:|---:|---:|---|---|
| `PATROL` | 否 | 否 | 否 | 否 | 否 | 否 | `MISSING_FULL_STACK` | 优先向原厂/历史介质补包；无法取得时，按设备与安环共用边界新建巡检域 |
| `EMS/supEMS` | 否 | 否 | 否 | 否 | 否 | 否 | `MISSING_FULL_STACK` | 先定义能源计量点、表计、平衡、单耗、异常和结算边界，再决定补包或自研 |
| `packConfigManag` | 否 | 否 | 否 | 否 | 否 | 未确认 | `MISSING_DEPENDENCY` | 补齐与 WOM `6.1.3.4` 匹配的 `6.1.2.1/6.1.3.2` 包，之后重新做准入和编译 |
| `SESGISConfig` | 否 | 否 | 否 | 否 | 否 | 未确认 | `MISSING_DEPENDENCY` | 补齐服务端包后复验 WTS 地图；仅有前端/i18n 引用不能视为已交付 |
| `WOMmobile_5.0.0.0` | 是 | 是 | 否 | 否 | 否 | 未验收 | `PRESENT_NOT_DEPLOYED` | 建立与当前 WOM/基础平台的版本矩阵，隔离部署并做页面/API/PostgreSQL 验收 |
| `material-wms` | 自研 | 是 | 是 | 是 | 是 | 是 | `CUSTOM_REPLACEMENT_DEPLOYED` | 继续闭合请检、质量处置、完工入库、库存事务和批次追溯 |
| `process-analysis` | 自研 | 是 | 是 | 是 | 是 | 是 | `CUSTOM_REPLACEMENT_DEPLOYED` | 继续验证跨 WOM/QCS/WMS 的同一批次 marker 和反向追溯 |

## 原始包扫描证据

本轮同时搜索了：

- `PATROL_1.0.0`
- `EMS_1.0.0`
- `supEMS_1.0.0`
- `packConfigManag_6.1.2.1`
- `SESGISConfig_1.0.0`
- `WOMmobile_5.0.0.0`

唯一真实模块命中是 `supPlant-WOMmobile V6.1.2.2-211230-C.zip` 内层包中的 `WOMmobile_5.0.0.0`。PATROL、EMS、packConfigManag 和 SESGISConfig 只在其他模块元数据、前端或国际化资源中被引用，没有发现可部署模块包。

引用关系也解释了页面现状：

- EAM、BaseSet、TeamInfo 引用 PATROL，因此缺少共享巡检会同时影响设备和安环巡检。
- WOM `6.1.3.4` 引用 `packConfigManag`，因此不能把该新包直接替换到当前运行环境。
- WTS/EAM 引用 `SESGISConfig`，因此作业地图缺口不是单纯前端显示问题。
- HierarchicalMod 提供能源节点页面和 `hm_energy_*` 表，但不包含完整 EMS 业务链。

## 当前运行证据

当前只读复核经 `ssh v6@100.99.133.43` 完成：

- 核心模块健康实例：`WOM`、`RM`、`craftGraph`、`QCS`、`LIMS`、`EAM`、`WTS`、`material`、`ProcessAnalysis` 均为 `1/1`。
- Nacos 中没有 PATROL、WOMmobile、packConfigManag、SESGISConfig 的服务；`OEEMs` 名称包含 `EMS` 字符串，但它是 OEE 服务，不能据此认定能源模块存在。
- `mod_module_registry` 的 33 个 BIZ 模块覆盖恢复源码中的 34 个唯一模块代码，唯一未注册的恢复模块代码是 `WOMmobile_5.0.0.0`。
- 当前菜单树中存在 `/msService/material/wms` 和 `/msService/ProcessAnalysis/processAnalysis/exelogSecond/processBatchViewOut`；没有 PATROL 或 WOMmobile 菜单。
- PostgreSQL 中没有 PATROL 或 `mp_*` 表；`hm_energy_nets` 和 `hm_energy_nodes` 均为 0 行。

## 接入顺序

1. **不阻断当前生产质量主线**：继续闭合“制造指令 -> 投料/报工 -> 请检 -> 质量处置 -> 完工入库 -> 批次追溯”。
2. **优先补 PATROL**：它同时补设备和安环巡检，业务复用收益最高。
3. **补 WOM 依赖**：取得 `packConfigManag` 后，在隔离环境验证 WOM 新版本，不直接覆盖现有运行版本。
4. **补 WTS 地图依赖**：取得 `SESGISConfig` 后单独恢复 GIS 功能。
5. **能源模块单独立项**：在真实表计/测点和数据采集边界明确后，再补原包或建设新的能源产品模块。
6. **最后接 WOMmobile**：先解决版本兼容、认证、菜单权限和移动端 API 合同，再上线。

每次新包到位后必须运行模块准入扫描，并按真实页面、API、PostgreSQL marker 和回滚步骤复验。包、JAR、Nacos 服务或菜单单独存在，都不能作为“功能可用”的结论。
