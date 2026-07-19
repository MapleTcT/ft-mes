# BPI 点位校准治理与失败关闭验收

## 结论

2026-07-19 在目标环境 `10.11.100.17` 完成点位校准治理验收，结论为
**`PASS_CONTROLLED_TARGET_FAIL_CLOSED`**。marker
`ADP_E2E_CAL_20260719_160131` 从真实 ADP 页面闭合：

```text
提交校准证据 PENDING/r1
  -> 同一提交人批准被 422 拒绝
  -> 独立 BPI_ADMIN 批准为 APPROVED/r2 + EFFECTIVE
  -> 非匹配 calibrationVersion 不能放行真实点位
  -> 页面撤销为 REVOKED/r3
  -> PostgreSQL 状态、审计、幂等和动态 READY 直查
```

能力通过不等于现场点位已放行。验收故意使用
`ADP_E2E_CAL_20260719_160131_NON_MATCHING`，与真实目录声明的
`pilot-unverified-20260714` 不同；最终证据已撤销，试点点位仍为 0 READY。
机器记录见
[`metadata/bpi-point-calibration-governance-acceptance.json`](../../metadata/bpi-point-calibration-governance-acceptance.json)。

## 页面、API 与结果

| 页面/阶段 | 动作 | API | 实际结果 | 状态 |
|---|---|---|---|---|
| `/bpi/#/points` | 提交证据 | `POST /bpi-api/point-calibrations` | `200`，`PENDING/r1`，提交人 `legacy-ticket:admin` | PASS |
| 校准复核弹窗 | 同一提交人批准 | `POST /bpi-api/point-calibrations/{id}/approve` | `422`，弹窗保持打开，未生成批准审计 | PASS |
| Java 17 service | 独立管理员批准 | `POST /bpi/v1/point-calibrations/{id}/approve` | `200`，`APPROVED/r2`，决定人为独立 reviewer | PASS |
| `/bpi/#/points` | 重新读取目录 | `GET /bpi-api/point-catalog/current` | marker 版本不匹配，真实点位仍 `BLOCKED`、0 READY | PASS |
| `/bpi/#/points` | 撤销证据 | `POST /bpi-api/point-calibrations/{id}/revoke` | `200`，`REVOKED/r3` | PASS |
| `/bpi/#/points` | 最终列表与目录读取 | 两类 `GET` | 撤销证据可审计复显，真实点位无证据绑定 | PASS |

浏览器共捕获 11 个 BPI 请求。唯一 console error 是同人审批请求的精确 URL 返回预期
`422` 时 Chromium 生成的 resource message；脚本只在该负向动作的窄窗口按 URL 和状态码识别，
并确认失败后批准按钮恢复为可用状态。
最终 `unexpectedConsoleErrors=0`、`pageErrors=0`、`requestFailures=0`。
撤销完成后列表“最近处置人”显示 `legacy-ticket:admin`，不再误显示独立批准人。

![点位校准治理最终页面](../../metadata/bpi-point-calibration-governance.png)

## PostgreSQL 落库

Flyway 已从 V16 expand-only 升级到 V17，`bpi_point_catalog_snapshots.ready_point_count`
改名为 `source_claim_ready_point_count`，明确来源声明不能直接成为 MES 运行准入。校准证据保存在
`bpi.bpi_point_calibrations`，审批和撤销通过 revision 及状态约束保持不可变审计语义。

核心直查：

```sql
SELECT id, calibration_version, state, revision,
       submitted_by, decided_by, revoked_by
FROM bpi.bpi_point_calibrations
WHERE calibration_version =
      'ADP_E2E_CAL_20260719_160131_NON_MATCHING';

SELECT action, actor_id, before_revision, after_revision,
       detail->>'state' AS resulting_state
FROM bpi.bpi_audit_events
WHERE object_type = 'POINT_CALIBRATION'
  AND object_id = 'b5132f15-0dfa-4468-baaf-86b269f613e8'
ORDER BY created_at;

SELECT idempotency_key, method, resource_path, state, response_status,
       response_body->>'state' AS resulting_state
FROM bpi.bpi_api_idempotency
WHERE response_body->>'calibrationVersion' =
      'ADP_E2E_CAL_20260719_160131_NON_MATCHING'
ORDER BY created_at;
```

实际结果为：

- 校准证据唯一行为 `REVOKED/r3`；提交、决定、撤销主体均准确。
- 审计恰好三条：`SUBMITTED 0->1`、`APPROVED 1->2`、`REVOKED 2->3`。
- 成功命令幂等恰好三条，均为 `COMPLETED/200`；预期 `422` 没有伪装成成功命令。
- 自动快照 `ca213975-4b22-4230-8cd9-968b0d1ce61a` 为 1 个点，匹配有效证据 0，
  `source_sequence_enabled=false`，目录 API 因而返回 0 READY。

## 构建与回归

本轮本地使用 PostgreSQL 16/Flyway V17 完成 82 条 Java 17 测试：事件契约 17、规则运行时 9、
service 56；Java 8 adapter 17 条测试、模拟器 8 条、Chromium E2E 10 条、前端生产构建和四类静态
契约门禁均通过。

目标 Java 17 服务运行镜像为
`ft-mes-bpi-service:20260719-point-calibration-v17b`
（image ID `sha256:a72c6eea420c6cbecaed880e0f7322b6529bdf6a041a00bf62dab6ca741751d7`）。
部署时还发现 adapter Dockerfile 只复制子模块，却从聚合 POM 启动构建。修复后目标 amd64
主机从子模块 POM 成功生成镜像
`sha256:0fc9ed80eaea78f18c0d59a8732e481a9b8c111fdeb684fef3354b866f70820d`；其中
`app.jar` SHA-256 与当前 healthy 运行镜像完全相同：
`f3f0ecaacf3fab2bd6114c075f548e16bd7b835dff9bacf6c005d61c194a8b00`。

## 未闭合边界

- 现场计量人员尚未提交并审核与 `pilot-unverified-20260714` 精确匹配的真实证书和校验和。
- JetLinks 映射仍未强制来源序列，连续单调 DEVICE/GATEWAY 运行证据尚未形成。
- 没有生成 candidate/batch，也没有写 WOM、QCS 或 WMS。
- 7-14 天影子运行、边界人工认同率、累计量偏差和真实负载整体回切仍未验收。

因此 G-021 继续保持 `PARTIAL`，禁止手工修改快照或复用本次 marker 伪造 READY。
