# 生产质量核心补丁测试环境回滚演练

## 范围

本演练只覆盖测试环境 `100.99.133.43` 上本轮生产质量核心补丁：

- `WOMMs-1.0.0.jar`；
- WOM 制造指令列表 `body.js`、`body-es5.js` 和 `i18n-value.js`；
- WOM 容器重启和 Nacos `prod@@WOMMs` 重新注册。

它证明当前补丁可以回退到 pre-coreflow 备份并恢复，不代表生产数据库、
MinIO、Keycloak、Nacos 全量配置、DNS 或其他业务模块已经完成生产回切演练。

## 执行入口

```bash
make rehearse-core-flow-runtime-rollback
```

脚本 `deploy/docker/scripts/adp-core-flow-runtime-rollback-rehearsal.js` 会：

1. 校验当前文件、回退备份、容器、Nacos 和页面状态；
2. 在远端创建本次临时恢复快照；
3. 原子替换为 `.bak-20260710-coreflow` 文件并重启 WOM；
4. 等待本次容器启动日志、Nacos healthy 实例和页面 HTTP 200；
5. 在 `finally` 阶段恢复演练前快照，再次完成同样的启动验证；
6. 只在恢复成功后删除本次临时快照。

## 2026-07-10 实际结果

Marker：`ADP_E2E_20260710034638_CORE_RUNTIME_ROLLBACK`。

| 阶段 | WOM JAR SHA-256 | `body.js` SHA-256 | 启动时间 | Nacos healthy | 页面 |
|---|---|---|---|---:|---:|
| 演练前 | `167bd1e3694ca5927c209f6c832262cf51fb961f1780dcd45eb66ee1c8a3e768` | `5eb740c7abe578dd62dd81ad936a146ac1bb0a61a686fa046691b72e2fe18090` | `2026-07-10T03:43:55.869434017Z` | 1 | 200 |
| 回退后 | `2579c2cc4d716f1b8baf8b26bc5c2f300655714c852170d72690cc2dd1c0db03` | `cac6956921ae681a33d11207dcd61d87cc0848a36413ea081c9452e28aec2e9a` | `2026-07-10T03:46:46.005480077Z` | 1 | 200 |
| 恢复后 | `167bd1e3694ca5927c209f6c832262cf51fb961f1780dcd45eb66ee1c8a3e768` | `5eb740c7abe578dd62dd81ad936a146ac1bb0a61a686fa046691b72e2fe18090` | `2026-07-10T03:48:11.780923494Z` | 1 | 200 |

三阶段的 `body-es5.js` 和 `i18n-value.js` 也分别完成备份哈希与恢复哈希
比对。两个重启阶段均要求本次 `StartedAt` 之后出现
`Started WOMMsApplication`，避免把 Nacos 尚未过期的旧实例误判为启动成功。

恢复后继续执行：

- 环境 smoke：`9/9 PASS`；
- 制造指令 marker `ADP_E2E_20260710035033_WOM_MANUFACTURING_ORDER`：生成、
  提交、生效、应用删除回滚 PASS，`workflowMetadataDrift=false`；
- 工具栏 marker `ADP_E2E_20260710035100_WOMSTART_HOLD_RESTART`：真实页面、
  API、PostgreSQL 状态流转 PASS，console/network/page error 为 0；
- 工具栏夹具清理：`cleanup|0|0|0|0`。

机器证据：`metadata/core-flow-runtime-rollback-rehearsal.json`。

## 边界

- 回退备份必须由部署人员事先核对来源和 SHA-256，脚本不会自动生成一个
  “看起来可用”的旧版本。
- 运行中失败时脚本会尝试恢复演练前快照；若报告出现 `RESTORE FAILED`，必须
  停止后续发布并从远端快照人工恢复。
- material/WMS 和 ProcessAnalysis 缺包不属于本演练可修复范围，仍按生产阻断
  处理。
