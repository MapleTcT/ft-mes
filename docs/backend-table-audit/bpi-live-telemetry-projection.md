# BPI 实时遥测投影落库验收

## 结论

Flyway V35 新增 `bpi.bpi_telemetry_point_latest`，用于 BPI 实时生产态势的低成本在线读取。
原始审计事实仍保存在 `bpi_telemetry_events` 和 `bpi_telemetry_points`，latest 表只是一点一行的
可重建投影，不替代历史、训练或审计数据。

2026-07-23 在 `10.11.100.17` 使用 marker
`ADP_E2E_20260723_BPI_LIVE_V35_07` 完成 MQTT、Kafka、API、真实页面与 PostgreSQL 复验。
窗口内 2 条事件和 2 条点位事实生成 1 条 latest 行；页面与 API 精确读取
`12.5 m3/h / GOOD / IN_ORDER`。取证后 marker 残留为 0。

## 动作与表映射

| 业务动作 | 前端入口 | API endpoint | 后端入口 | 目标表 | 实际结果 | 状态 |
|---|---|---|---|---|---|---|
| 接收窗口遥测并更新在线态 | 受控 MQTT；`/bpi/#/shadowRuns` 启动窗口 | MQTT -> `iot.telemetry.selected.v1` | `TelemetryKafkaListener -> TelemetryKafkaRecordProcessor -> TelemetryIngestionService -> TelemetryPostgresRepository` | `bpi_telemetry_events`、`bpi_telemetry_points`、`bpi_telemetry_point_latest` | 2 events/2 points/1 latest，0 rejects | PASS |
| 查询产线实时态势 | `/bpi/#/overview` | `GET /bpi/v1/overview` | `OverviewController.overview -> OverviewService.overview -> OverviewPostgresRepository.findOverview` | latest、topology、batch、candidate、data quality 表只读联查 | `12.5 m3/h`、`GOOD/IN_ORDER`、真实采样时间返回 | PASS |
| 查询点位事实抽屉 | overview 产线行 | `GET /bpi/v1/lines/{lineId}/live-evidence` | `OverviewController.liveEvidence -> OverviewService.liveEvidence -> OverviewPostgresRepository` | latest + 最近 `bpi_telemetry_points` + 数据质量事件 | 5 samples、6 PASS、1 production-context WARN、7 incidents | PASS |
| 迟到保护 | 无直接页面动作 | repository upsert 条件 | `TelemetryPostgresRepository.insertPoint` 同事务 UPSERT | `bpi_telemetry_point_latest` | 只允许更晚 sample time，或同时间更高 epoch/sequence 覆盖 | PASS_TEST |
| marker 清理 | 验收 runner | cleanup SQL | PostgreSQL 定向事务 | telemetry/latest/run/rule/topology/catalog/calibration | 7 类 remaining 全为 0 | PASS_CLEANED |

## 事务与覆盖规则

`TelemetryPostgresRepository` 在原始 event 和 point 写入事务内执行：

```sql
INSERT INTO bpi.bpi_telemetry_point_latest (...)
VALUES (...)
ON CONFLICT (tenant_id, plant_id, line_id, product_id, device_id, property_id)
DO UPDATE SET ...
WHERE EXCLUDED.sample_time > bpi_telemetry_point_latest.sample_time
   OR (
        EXCLUDED.sample_time = bpi_telemetry_point_latest.sample_time
        AND EXCLUDED.source_epoch > bpi_telemetry_point_latest.source_epoch
   )
   OR (
        EXCLUDED.sample_time = bpi_telemetry_point_latest.sample_time
        AND EXCLUDED.source_epoch = bpi_telemetry_point_latest.source_epoch
        AND EXCLUDED.sequence > bpi_telemetry_point_latest.sequence
   );
```

主键为：

```text
tenant_id + plant_id + line_id + product_id + device_id + property_id
```

外键关联同 tenant 的 `bpi_telemetry_events`，事件删除会级联删除投影。V35 不回填历史表，部署后的
新遥测自然建立最新态。

## 复验 SQL

```sql
SELECT tenant_id, plant_id, line_id, product_id, device_id, property_id,
       numeric_value, unit, quality_code, sequence_disposition,
       sample_time, calibration_version
  FROM bpi.bpi_telemetry_point_latest
 WHERE tenant_id = '1000'
   AND plant_id = 'PLANT-01'
   AND line_id = 'LINE-S07-01'
   AND calibration_version =
       'pilot-telemetry-ADP_E2E_20260723_BPI_LIVE_V35_07';

SELECT count(*) AS event_rows
  FROM bpi.bpi_telemetry_events
 WHERE event_id LIKE 'ADP_E2E_20260723_BPI_LIVE_V35_07_WINDOW:%';

SELECT count(*) AS point_rows,
       array_agg(sequence ORDER BY sequence) AS sequences,
       array_agg(quality_code ORDER BY sequence) AS qualities
  FROM bpi.bpi_telemetry_points point
  JOIN bpi.bpi_telemetry_events event
    ON event.tenant_id = point.tenant_id
   AND event.id = point.telemetry_event_id
 WHERE event.event_id LIKE 'ADP_E2E_20260723_BPI_LIVE_V35_07_WINDOW:%';
```

实际结果摘要：

```text
event_rows=2
point_rows=2
sequences={4,5}
qualities={GOOD,GOOD}
latest_rows=1
latest_value=12.5
latest_unit=m3/h
latest_quality=GOOD
latest_sequence_disposition=IN_ORDER
```

正式验收 SQL：

- `deploy/docker/scripts/bpi-telemetry-landing-acceptance-verification.sql`
- `deploy/docker/scripts/bpi-telemetry-landing-acceptance-cleanup.sql`

## 边界

latest 表仅服务在线态势，不可作为训练快照或审计真相。受控模拟器、临时校准和单点短窗口不能
替代物理设备、正式计量、多产线容量或 7-14 天连续现场证据；`G-021` 保持 `PARTIAL`。
