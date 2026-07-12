# BPI Flink 边界算子验收

## 结论

`BoundaryKeyedBroadcastFunction` 已实现 Flink 2.2.1 单产线/局部组事件时间边界算子。
算子使用 `tenant + line + locality group + boundary kind` 作为 keyed state 分区，使用
broadcast state 接收发布规则，并复用 `batch-rule-runtime` 完成 START/END 判断。

流式模块自动测试为 **41/41 PASS**，其中 10 项使用 Flink 官方
`KeyedBroadcastOperatorTestHarness`，另有 6 项使用官方
`KeyedTwoInputStreamOperatorTestHarness`，不是自制上下文 mock。

## 状态与恢复契约

- keyed window state 使用 `ValueState<byte[]>`，magic=`BPIS`、写入 version=2、兼容读取 version=1；
- broadcast rule state 使用 `MapState<String, byte[]>`，magic=`BPIR`、写入 version=2、兼容读取 version=1；
- codec 遇到未知版本、错误 magic、重复信号或尾随数据时失败，不静默清空状态；
- 信号 map 编码前按名称排序，同一逻辑状态生成确定字节；
- checkpoint 恢复测试同时验证 window、broadcast rule 和 event-time timer；
- BPIS/v2 同时保存有界、确定排序的规则相关观测，checkpoint 后可继续迟到重算；
- BPIS/v1 恢复状态标记为历史不完整，实时处理可继续，但迟到修正只进入修订队列；
- 生产指令、批次、拓扑或规则版本变化时创建新边界生命周期，旧 timer 被删除或按
  `nextTimerEpochMs` 判定失效。

## 事件时间与输出契约

| 场景 | 预期 | 结果 |
|---|---|---|
| 发布规则后输入 order/flow | hold 到期前不产生候选 | PASS |
| watermark 到达 hold 截止时间 | timer 产生一个 START 候选 | PASS |
| 继续推进 watermark | 不重复产生候选 | PASS |
| checkpoint 后恢复并推进 watermark | 产生一个与恢复前语义一致的候选 | PASS |
| 未发布规则 | 不产生候选，输出 `RULE_NOT_FOUND` issue | PASS |
| START 缺生产指令 | 不产生候选，输出 `CONTEXT_ID_MISSING` issue | PASS |
| 同版本规则内容变化 | 保留旧规则，输出 `RULE_VERSION_CONFLICT` issue | PASS |
| final candidate | 输出 Protobuf wire bytes，可解析且契约有效 | PASS |
| maxSilence | 在 `lastEventTime + maxSilence + 1ms` 转 UNKNOWN | PASS |
| allowed lateness 内迟到观测 | 对开放窗口按 event time 确定性重算并产生候选 | PASS |
| 超过 allowed lateness | 输出 `LATE_EVENT_REVISION_REQUIRED`，不改状态 | PASS |
| 候选发出后的迟到证据 | 输出修订 issue，不覆盖候选 | PASS |
| checkpoint 后迟到观测 | 恢复观测历史并完成同语义重算 | PASS |
| 格式错误的迟到观测 | 输出 `EVALUATION_REJECTED`，不击穿 Flink task | PASS |

算子不直接输出 Protobuf Java 对象。Harness 证明该对象会触发 Kryo 对 Protobuf 内部不可变
descriptor/map 的复制失败，因此数据面改为稳定 Protobuf wire bytes，后续 Kafka sink 直接发送。

## 当前限制

- Kafka Protobuf source/sink、topic、partition key 和 exactly-once transaction 尚未接线。
- 规则中的 `watermarkDelay` 尚未接入 Kafka source 的 watermark strategy。
- `LATE_EVENT_REVISION_REQUIRED` 尚未接入持久化修订队列和人工处置消费端。
- BPIS/v1 到 BPIS/v2 的目标集群 savepoint 升级尚未实机演练。
- 规则更新流目前是已解码的 `BoundaryRuleUpdate`，Kafka 规则命令格式和 schema registry 尚未接线。
