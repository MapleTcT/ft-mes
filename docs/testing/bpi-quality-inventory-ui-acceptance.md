# BPI 批次质量与库存页面验收

## 验收结论

2026-07-20 基于源码提交
`94e2b2288bf52966be58e9dda97039a5455466a8` 完成批次详情“质量与库存”产品页验收，状态为
**`PASS_DETERMINISTIC_BROWSER_NOT_TARGET_ACTIVATED`**。

本次接入真实 OpenAPI 读合同 `GET /bpi/v1/batches/{batchId}/release`，但浏览器测试的数据由
确定性模拟器提供。它证明页面可以正确呈现质量门、检验明细、WMS 命令和最终单据，并不证明
目标环境已经部署 Flyway V23，也不证明真实 QCS/WMS 已产生业务单据。Phase 1 继续保持
`SHADOW_ONLY`，Phase 2 开关继续默认关闭。

## 状态验收

| 批次状态 | 质量门 | WMS 回执 | 页面业务结论 | 验收状态 |
|---|---|---|---|---|
| `CLOSED_RAW` | 无 | 无 | 尚未进入质量放行；尚未生成入库命令 | PASS |
| `WAIT_QA` | `WAITING` | 无 | 等待 1 项必检项目完成，展示待最终确认检验 | PASS |
| `REJECTED` | `REJECTED` | 无 | 存在不合格必检项目，禁止显示“已入库” | PASS |
| `RELEASED` | `ACCEPTED` | `PENDING` | 入库处理中；展示命令事件和幂等键 | PASS |
| `RELEASED` | `ACCEPTED` | `REJECTED` | 入库失败；展示 `WMS_LOCATION_LOCKED` 和拒绝详情 | PASS |
| `INBOUNDED` | `ACCEPTED` | `ACCEPTED` + `documentId` | 只有持久化单据 `WMS-IN-ADP-E2E-0001` 存在时显示“已入库” | PASS |

页面还按 fail-closed 原则处理 `ACCEPTED` 但缺少 `documentId` 的非法组合：显示“回执不完整 / BLOCKED”，
不把 HTTP 200 或回执枚举单独解释为入库成功。

## 浏览器验收

| 场景 | 页面/路由 | 操作 | API | 实际结果 | 状态 |
|---|---|---|---|---|---|
| 桌面六态 | `/bpi/#/batches` | 逐条打开六种批次 | `GET /bpi/v1/batches/{id}/release` | 六类业务文案、检验记录、外部修订、事件 ID、幂等键、错误码和单据号均正确；响应头均为 `getBatchRelease` | PASS |
| 慢响应 | 同上 | 将质量/WMS 响应延迟 600 ms | 同上 | 批次事实、边界证据和时间线先显示，质量与库存区块独立加载 | PASS |
| 局部 503 | 同上，390x844 | 注入 `503` 后打开入库失败批次 | 同上 | 核心事实仍可见，局部显示 traceId `ADP-E2E-RELEASE-TRACE-503`，重试后恢复 | PASS |
| 关闭竞态 | 同上 | 慢响应期间关闭抽屉 | 同上 | 旧响应不会重新打开抽屉或覆盖其他详情 | PASS |
| 移动布局 | 同上，390x844 | 查看检验、失败回执和底部操作区 | 同上 | 抽屉 `scrollWidth <= clientWidth`，底部关闭按钮不遮挡固定导航 | PASS |

主动注入的 503 会由 Chromium 记录一条预期网络 console 消息；测试精确断言该消息只有一条，
其余 console、page error 和 request failure 均为 0。

## 自动化结果

```text
npm run build
  PASS - TypeScript 5.7.2 + Vite 6.4.3

npm run test:e2e
  17 tests, 17 pass, 0 fail

make bpi-simulation-test
  12 tests, 12 pass, 0 fail

make bpi-api-contract-check bpi-ui-static-check
  PASS - operations=66, simulated=54, implemented=54, phase2Reads=1
```

截图：

- `/tmp/bpi-console-batch-quality-inventory.png`，1440x900，SHA-256
  `de2ff01ee89566c14775b713ac279192e01a90f3ded9464ccb81f0604d78d646`
- `/tmp/bpi-console-batch-quality-inventory-mobile.png`，390x844，SHA-256
  `966dd0816e1ab7c0c3cf85ef48ad4068ef34dfab77525a08677096dd4bb7c2c4`

截图是本机测试临时证据，不作为部署制品提交。

## 持久化边界

本 UI 套件不落库。真实 PostgreSQL V1-V23、QCS required-inspection snapshot、WMS transactional
outbox、durable document receipt、拒绝分支和影子批次双层阻断的 4/4 验收继续以
`metadata/bpi-quality-release-wms-inbound-acceptance.json` 为准。

仍需完成：

1. 将 V23 和本页面静态包部署到目标测试环境。
2. 接入真实 QCS/WMS adapter、Kafka topic/ACL 和按幂等键查单能力。
3. 使用同一 marker 闭合真实页面、HTTP/Kafka、Java 服务、PostgreSQL 投影和 WMS 单据号。
4. 演练超时查单、拒绝、重复回执、服务重启、broker 重放和受控关闭开关。
