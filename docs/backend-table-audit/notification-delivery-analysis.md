# 通知分发专项分析

## 结论

`NOTIFY-001` 当前状态为 `FAIL_NOTIFICATION_DELIVERY`。

消息中心列表、WTS 业务落库、通知任务落表和终端送达是四个独立验收面。
不能把消息中心列表 PASS 当作通知送达 PASS，也不能用 WTS 正常封票替代站内信或移动通知验收。

## 真实复验

- 环境：唯一 PostgreSQL 测试栈。
- marker：`ADP_E2E_20260730080536_WTS_FIREWORK`。
- 前置：真实 admin 门户保持在线，浏览器建立 1 个
  `/inter-api/ws/v1/notice/notification` WebSocket。
- 业务动作：动火票创建、风险与气体分析、审批、执行、封票。
- WTS 结果：`status=99`、`job_status=WTS_jobStatus/normalClose`、活动待办 0。
- 消息中心：真实页面 PASS；任务查询连续 10/10 返回 HTTP 200，38-150 ms。
- 通知送达：站内信 7/7 失败，移动通知 7/7 失败，两个通道的 `send_status` 均为 0。

验收 SQL：

```sql
SELECT count(*), count(*) FILTER (WHERE send_status = 0)
FROM public.notice_msg_mobile202607
WHERE create_time BETWEEN timestamp '2026-07-30 08:05:36'
                      AND timestamp '2026-07-30 08:06:02';

SELECT count(*), count(*) FILTER (WHERE send_status = 0)
FROM public.notice_msg_stationletter202607
WHERE create_time BETWEEN timestamp '2026-07-30 08:05:36'
                      AND timestamp '2026-07-30 08:06:02';
```

两条查询均返回 `7|7`。

## 已修复边界

1. 默认 Compose 已停止旧 notification 五服务与 `msgmanagement` 的重复注册。
2. `NoticeProtocolMapper` 已限定暴露到 PostgreSQL mapper 扫描路径。
3. PostgreSQL 已补齐缺失的 `mobile_device_token` 表，原“relation does not exist”异常不再出现。
4. 消息中心的 topic/task 查询和真实页面已恢复。

这些修复只关闭了服务冲突、mapper 和数据库结构阻断，没有关闭终端送达问题。

## 当前失败边界

- `msgmanagement` 对站内信和移动通知均返回
  `code=100113000, message=all fail`。
- `NoticeStationLetterServiceImpl` 将该响应记录为“站内信发送失败”。
- `MobileServiceImpl` 在响应数据不完整后继续解引用并触发 `NullPointerException`。
- 服务启动时另有 `notificationEngine.zip` 缺失警告，但当前没有证据证明它是本次送达失败的直接原因。
- 当前只能确认“在线 admin WebSocket 未被通知发送路径接受”，尚不能在没有源码级调用链证据时断言是
  token、用户会话注册、网关转发或 engine 制品中的哪一项。

## 关闭条件

`NOTIFY-001` 只有同时满足以下条件才能改为 PASS：

1. admin 门户保持在线，浏览器确认 WebSocket 已连接。
2. 使用新 marker 完整执行一次 WTS 流程。
3. 每个待办对应站内信 `send_status=1`，且浏览器实际收到消息。
4. 已登记移动设备时，移动通知 `send_status=1`；没有设备时应返回明确的离线/无设备状态，不能抛空指针。
5. `msgmanagement` 日志中没有 `all fail`、`NullPointerException` 或数据库结构异常。
6. 消息中心任务查询、WTS 终态和 PostgreSQL 通知月表三方一致。

机器记录：`metadata/notification-delivery-analysis.json`。
