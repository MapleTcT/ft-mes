# BPI Phase 3C-G 工艺窗口 Parquet v2 设计

## 1. 目标与阻断事实

V32/V34 已把 `process.window.*` 定义、遥测聚合值和不可变窗口事实写入
PostgreSQL manifest，但现有 `bpi.dataset-parquet.v1` 只认识七个固定上下文特征。
只要数据集选择任意工艺窗口，materializer 就会以 `unsupported refs` 失败。

这意味着当前链路只能证明窗口事实存在，不能把这些值交付给 Iceberg、MLflow 或后续
训练器。Phase 3D 之前必须先修复这一断点，不能用删除工艺特征或降低训练门槛绕过。

## 2. 契约决策

新增：

- artifact schema：`bpi.dataset-parquet.v2`
- materializer：`bpi-dataset-materializer/0.2.0`
- Parquet 列：`feature_process_window_values`
- Arrow 类型：`map<string, decimal128(24, 6)>`

v2 保留 v1 的固定列、顺序、标签和 point-in-time 元数据。工艺窗口 map 的 key 使用原始
`featureRef`，按字典序写入；value 使用 manifest 中已冻结的六位小数聚合值。这样：

1. 不需要把业务标识符转义成列名，避免 `-`、`.`、`_` 映射碰撞；
2. 新工艺窗口不再要求发布新的固定 Parquet 列；
3. 训练器可按 dataset definition 中的 `featureRefs` 精确展开；
4. 相同冻结事实仍产生相同字节和 SHA-256；
5. 没有工艺窗口的数据集写入空 map，不把缺失和空集合混为一谈。

只允许 manifest 已声明的 `process.window.*` 引用进入 map。其他未知命名空间继续失败关闭。
工艺窗口值必须是有限数值；缺失、NaN、Infinity、布尔或文本均拒绝物化。窗口 READY
完整性仍由 manifest/训练资格门槛负责，materializer 不重新解释实时遥测。

## 3. 向后兼容

- v1 历史对象、数据库行、Iceberg snapshot、恢复包和 MLflow Dataset Input 不修改、不重写；
- BPI API 的新 materialization 请求固定创建 v2/0.2.0；
- PostgreSQL 现有唯一键包含 schema/materializer version，因此同一快照可显式生成新的 v2
  任务，不覆盖 v1；
- 页面和按 ID 查询继续显示历史 v1；快照投影选择该快照最近创建的 materialization；
- worker 0.2.0 只领取 v2/0.2.0 任务，升级前必须确认没有遗留的非终态 v1 任务。

## 4. 验收

必须证明：

1. 两行相同冻结事实以不同输入顺序构建时，Parquet 字节和 SHA-256 完全一致；
2. map key 保留两个原始 `process.window.*` 引用且顺序稳定；
3. 数值精度固定为六位小数；
4. 未选择的 payload 不泄漏；
5. 未声明命名空间、非数值和非有限值失败；
6. Java API 创建 v2/0.2.0 任务；
7. 模拟器、OpenAPI 展示和文档不再把 v1 说成当前写入契约；
8. 历史 v1 验收记录保持历史口径，不回写为 v2。

## 5. 发布与回滚

本阶段不需要 Flyway 迁移，是扩展型制品契约升级。发布顺序为：

1. 构建并验证 0.2.0 worker；
2. 部署 API 使新请求写入 v2；
3. 显式开启 worker 的受控验收窗口；
4. 用唯一 marker 生成 v2，核对 PostgreSQL、Parquet map、MinIO exact version 和 checksum；
5. 关闭 worker并清理 marker。

回滚只恢复 API 新请求版本与 worker 镜像，不删除 v2 行或对象。已完成的 v2 制品保持可读，
下游在显式支持 v2 前不得自动消费。

## 6. 下一纵切

v2 闭合后再实现 Phase 3D-A 离线训练任务控制面。当前目标数据仍为 `BLOCKED`，训练入口必须
返回 422 且不创建任务、不写 MLflow。成功训练只允许使用通过当前 v2 资格策略的专用验收数据，
并继续保持模型注册、在线推断和生产激活为 false。
