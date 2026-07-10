# 物料 WMS 完工入库模块设计

## 目标与边界

新建可独立构建、部署和演进的 `material-wms` 服务，补齐当前 WOM 核心链路缺失的物料服务。首期以完工入库为主，同时兼容现有 WOM 已经固化的生产领料出库和质检结果回写接口。默认数据库只使用 PostgreSQL，不增加 Oracle 运行依赖，也不复用未验证的旧仓储表。

首期支持以下真实调用：

- `POST /public/material/produceInSingles/produceInSingl/generateProductInSingle`：完工产出入库。
- `POST /public/material/produceOutSingle/produceOutSing/generateProduceOutSing`：生产领料出库；若旧调用在该接口传入 `comeType=produceIn`，仍按完工入库处理。
- `POST /material/foreign/foreign/checkProdResult`：按 WOM 产出记录 ID 回写合格或不合格状态。
- `GET /material/wms`：完工入库运行视图。
- `GET /material/wms/completion-inbounds`：完工入库分页查询。
- `GET /material/wms/completion-inbounds/{id}`：单据、明细和库存流水详情。

通用采购入库、销售出库、调拨、盘点、库位策略、条码打印和 ERP 对账不进入首期。ProcessAnalysis 继续作为后续独立模块接入。

## 方案选择

采用“旧接口兼容适配层 + 新库存内核”。不直接恢复未知的旧 material 包，也不让 WOM 直接操作 WMS 表。兼容层接受旧字段名、大小写和拼写错误，例如 `srcID/srcId`、`pruductDate/productDate`、`productBatch/produceBatchNum`，并返回 WOM 仍在判断的 `code=200` 结构。内部转换为稳定的库存单据命令。

备选方案一是只做两个返回成功的兼容接口，但无法证明库存落表，属于假成功，排除。备选方案二是一次性重建完整仓储系统，范围和风险都超过当前生产主线，排除。

## 数据模型

- `wms_stock_documents`：库存业务单据，类型为 `COMPLETION_INBOUND` 或 `PRODUCTION_ISSUE`。
- `wms_stock_document_lines`：物料、批次、仓库、库位、数量及质量状态。
- `wms_batch_stocks`：按租户、仓库、库位、物料和批次汇总的现存、可用、冻结数量。
- `wms_inventory_transactions`：不可变库存流水，记录现存、可用和冻结数量的每次变化。
- `wms_quality_results`：质检回写暂存与版本。允许质检先于入库到达，后续入库时自动应用。

来源系统、单据类型、来源单据 ID 和仓库构成单据头幂等键；来源明细 ID 构成实际记账幂等键；库存流水使用事件键防止重复记账。WOM 生产用料单会用同一个 `srcID` 按明细多次调用，因此已有单据头必须继续接收尚未记账的 `srcPartId`，不能整单提前返回。相同来源明细若数量或库存维度不一致则返回冲突。数量使用 `numeric(20,6)`。空仓库、空物料、非正数量和不支持的红字冲销都明确失败，不生成半张单据。

## 数据流与一致性

完工入库在单个数据库事务内创建单据、明细、批次库存和库存流水。未检数量进入冻结库存；合格数量进入可用库存；不合格数量保留在冻结库存。质检回写只在状态发生变化时移动可用/冻结数量，现存量不变。生产领料只允许扣减足够的可用库存，默认禁止负库存。

接口重复调用时返回原单据并标记 `idempotent=true`，不重复增加库存。数据库唯一约束是最终并发保护，应用层检查只用于给出稳定响应。任何明细失败都会回滚整张单据。

## 部署与验收

模块继承仓库父 POM，使用 Java 8、Spring Boot 2.1、Spring JDBC、Nacos Discovery 和 PostgreSQL。Docker 服务名及 Nacos `spring.application.name` 均为 `material`，使现有 WOM 无需改调用地址。编号 SQL 迁移同时支持新库初始化和现有测试库幂等应用。

验收必须覆盖：模块测试、Docker Compose 渲染、Nacos 健康实例、三个兼容接口、重复调用幂等、PostgreSQL 单据/明细/库存/流水落表、质检冻结转可用、领料扣减、运行视图浏览器 console/network 检查，以及 marker 数据清理。HTTP 200 但数据库无变化按失败处理。
