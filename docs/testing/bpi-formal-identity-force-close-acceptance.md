# BPI 正式身份双管理员强制结束验收

## 结论

2026-07-21 在唯一测试环境 `http://10.11.100.17:18080` 使用 marker
`ADP_BPI_FORMAL_IDENTITY_20260721134944` 完成真实 ADP 身份系统、两套独立浏览器会话、Java 8
Adapter、Java 17 BPI Service 和 PostgreSQL 15 的联合验收，结果为
`PASS_TARGET_FORMAL_IDENTITY_TWO_BROWSER_SESSIONS_CLEANED`。

申请人是既有 `admin` 会话；审批人由真实组织、认证和 RBAC API 临时创建并绑定现有
`systemRole`，随后通过 ADP 登录接口取得独立 legacy ticket。申请和审批均从
`/bpi/#/batches` 页面经 `/bpi-api` Adapter 发出，不再使用内部签名 token 代替第二管理员。

## 安全边界

- 临时审批账号密码只存在于验收进程环境，报告中固定为 `REDACTED`。
- Adapter 作用域通过备份目录内的隔离 Compose override 临时扩展，基础 `.env` 未修改。
- Adapter 镜像 ID 前、中、后均为
  `sha256:b21bc093eef5593113a422ed8df26880c277ccfed90a168e239a2951eec38977`。
- 900 秒 watchdog 会在验收进程失联时强制恢复基础 Adapter 配置；本轮正常收尾，watchdog 未触发。
- 六个 Phase 2、Kafka、WMS 写回开关在验收前、中、后均为 `false`。
- 只清理本 marker、临时账号和临时作用域，不删除非 marker 业务数据。

## 页面、API 与身份验收

| 步骤 | 页面/API | 实际结果 | 状态 |
|---|---|---|---|
| 创建临时人员 | `POST /inter-api/organization/v1/person` | HTTP 200；`org_person.valid=1` | PASS |
| 创建认证账号 | `POST /inter-api/auth/v1/user` | HTTP 200；`auth_user.valid=1`，密码只验证已编码 | PASS |
| 绑定管理员角色 | `POST /inter-api/rbac/v1/roleUser` | HTTP 200；`rbac_roleuser.valid=true`，关联 `systemRole` | PASS |
| 第二账号登录 | 登录接口与 `GET /inter-api/auth/v1/currentuser` | 两个请求均为 200；username、tenant `1000` 和 `systemRole` 匹配 | PASS |
| 匿名读取反证 | `GET /bpi-api/batches/{id}/force-close` | HTTP 401 | PASS |
| 申请强制结束 | `admin` 浏览器在 `/bpi/#/batches` 提交 REQUEST | HTTP 202；任务 `PENDING_APPROVAL/r1`，批次保持 `ACTIVE/r2` | PASS |
| 同人审批反证 | `admin` 浏览器提交 APPROVE | HTTP 403，明确要求另一管理员；没有终态写入 | PASS |
| 独立审批 | 第二 ADP 浏览器打开同一批次并点击“批准并强制结束” | HTTP 202；任务 `COMPLETED/r2`，批次 `CLOSED_RAW/r3` | PASS |
| 页面终态 | 第二浏览器读取完成态抽屉 | REQUESTED/CLOSED 时间线完整，按钮消失，`1600/1600` 无横向溢出 | PASS |

受控同人审批反证产生一条预期 403 console/BPI HTTP 记录；`unexpectedBpiHttpErrors=0`，
`pageErrors=0`，`requestFailures=0`。审批响应中的身份为：

- `requestedBy=legacy-ticket:admin`
- `decidedBy=legacy-ticket:bpi_reviewer_20260721134944`
- 两者不相等

## PostgreSQL 落库验收

身份链查询覆盖：

```sql
SELECT * FROM public.org_person WHERE code =
  'ADP_BPI_FORMAL_IDENTITY_20260721134944_PERSON';
SELECT * FROM public.auth_user WHERE user_name =
  'bpi_reviewer_20260721134944';
SELECT ru.*, r.code
FROM public.rbac_roleuser ru
JOIN public.rbac_role r ON r.id = ru.role_id
WHERE ru.user_name = 'bpi_reviewer_20260721134944';
```

批次链使用 `deploy/docker/scripts/bpi-force-close-acceptance-verification.sql` 在审批门闩前后各查询一次：

- 中间态：batch `ACTIVE/r2/end_time NULL`；task `PENDING_APPROVAL/r1`；REQUESTED event/audit 各 1；幂等 1。
- 最终态：batch `CLOSED_RAW/r3`；task `COMPLETED/r2`；event revision `2,3`；audit revision
  `1->2,2->3`；幂等 2。
- `end_time` 精确等于审批边界 `2026-07-21T13:44:44Z`。
- quality gate、WMS link 和 outbox 均为 0，没有触发生产写回。

清理后：

- 临时 `org_person`、`auth_user` 均为 `valid=0`；角色绑定和 `auth_user_role` 为 0。
- batch、force-close task、临时 commands flag 和 plant override 均为 0。
- Adapter 作用域精确恢复为 `admin=1000|PLANT-01|LINE-S07-01`。
- BPI Service、Adapter、PostgreSQL 均保持 healthy。

## 缺陷与修复

第一次运行 marker `ADP_BPI_FORMAL_IDENTITY_20260721134239` 暴露两项验收脚本缺陷：

1. 基础 Compose 直接生成 `BPI_ADAPTER_SUBJECT_SCOPE_RULES=admin=...`，临时 `.env` 中的同名变量不会覆盖。
2. `auth_user_role` 的 API 新增行允许冗余 `role_code/role_name` 为空，验收应通过 `role_id` 关联
   `rbac_role` 判断正式角色。

修复后使用隔离 Compose override 注入第二账号 scope，并通过角色表关联读取。失败轮也完成临时身份、
BPI marker、Adapter 和 watchdog 清理，没有留下活动账号或业务数据。

## 证据

- 机器记录：`metadata/bpi-formal-identity-force-close-acceptance.json`
- 待审批截图：`metadata/bpi-formal-identity-force-close-pending.png`
- 完成截图：`metadata/bpi-formal-identity-force-close-completed.png`
- 可复验入口：`make acceptance-bpi-formal-identity-force-close-target`
- 编排脚本：`deploy/docker/scripts/adp-bpi-formal-identity-force-close-acceptance.js`
- 页面脚本：`deploy/docker/scripts/adp-bpi-force-close-acceptance.js`
- JSON SHA-256：`1aa4658645d6e6788845ae7bff5243adb476aac83800defd5fdf83a5cbac860f`
- 待审批截图 SHA-256：`f6501a82840ea871460c976fcd7b8148f37681e119df5cffacac8ee7acb5738f`
- 完成截图 SHA-256：`774b461d0c0298dc7cc24ba600086b3a7ea5269da56fc75fc0bb5d844feb166f`

## 未关闭范围

本轮关闭的是 G-021 的“正式身份系统第二管理员会话”缺口，不代表 BPI 已具备生产放行条件。
物理 DEVICE/GATEWAY、与目录版本精确匹配的正式校准证书、真实连续 7-14 天影子运行、外部
ERP/WMS 查单/响应丢失/拒绝/冲销/补偿、受控生产激活和 Phase 3/4 训练模型仍未完成。
