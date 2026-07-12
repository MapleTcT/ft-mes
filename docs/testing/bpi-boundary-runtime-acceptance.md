# BPI 边界规则运行时验收

## 结论

单产线 START/END 边界判断所需的第一版无状态框架运行时已实现于
`services/bpi-service/batch-rule-runtime`。运行时本身不依赖 Spring、PostgreSQL 或 Flink，
输入为版本化规则、上一次 keyed state、信号观测或 event-time timer，输出为新状态、置信度、
证据快照和是否首次达到候选阈值。

当前自动测试为 **6/6 PASS**。这证明判定公式可被实时作业和历史回放复用，不证明 Kafka/Flink
状态恢复或真实候选消息已经完成。

## 已验收语义

| 场景 | 预期 | 结果 |
|---|---|---|
| required + 2 个 quorum + 不同 `for` | 只有 event-time 连续窗口全部成熟才首次 eligible | PASS |
| 同一状态再次 timer | 保持 eligible，但 `newlyEligible=false`，不重复发候选 | PASS |
| `maxSilence` 超时 | 已满足条件转 `UNKNOWN`，不永久沿用最后值 | PASS |
| `UNCERTAIN` 质量 | 质量因子 0.5 进入固定分母，不能因缺失缩小分母抬分 | PASS |
| composite penalty | 按 `base * (1-penalty)` 并受版本上限约束 | PASS |
| 旧 event-time 观测 | 标记 ignored，不倒退信号状态 | PASS |
| `RISING` | 至少两个数值样本，按相邻增量判断 | PASS |
| 不可能 quorum 配置 | 规则构建阶段拒绝 | PASS |

质量因子固定为：GOOD=1.0、UNCERTAIN=0.5、SUBSTITUTED=0.3、BAD/STALE=0。
空闲与保持时间均由事件时间推进，不使用处理时间或样本数量替代。

## 第一条模板验证

测试规则 `S05-FEED-START / 1.0.0` 使用：

- `order.active == true`：REQUIRED，权重 40；
- `feed.pump == true for 3s`：QUORUM，权重 20；
- `feed.flow > 2.0 for 10s`：QUORUM，权重 30；
- `column.level RISING for 15s`：OPTIONAL，权重 10；
- quorum=2，候选最低置信度 0.85，maxSilence=30s。

在 order、pump、flow 成立且 event time 到达第 11 秒时，结果为 confidence=0.9，首次 quorum
事件稳定识别为 `EVT-FLOW`。后续相同 timer 不再次发射。

## 尚未覆盖

- Flink `KeyedBroadcastProcessFunction`、RocksDB state、watermark 和 checkpoint/savepoint。
- 迟到事件在 allowed lateness 内的窗口重算；当前纯运行时对旧于信号最新时间的观测显式 ignored。
- 规则 JSON Schema 解析、已发布版本 broadcast 和 locality group 校验。
- `BatchCandidateV1` 生成、Kafka 投递以及 BPI inbox/PostgreSQL 联合 marker。
- END 与 ACTIVE/END_CANDIDATE 的应用层批次状态关联。
