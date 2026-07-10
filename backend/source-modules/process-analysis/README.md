# Process Analysis

`process-analysis` is the PostgreSQL-first traceability service used by WOM's
existing production-process trace button. It does not copy or replace WOM, QCS,
or WMS business facts. The operational trace view reads those source tables and
the legacy manual-statistics endpoints persist idempotent snapshots in
`pa_trace_snapshots`.

Build and test:

```bash
make process-analysis-test
make process-analysis-stage-runtime
```

Service registration name: `ProcessAnalysis` in Nacos group `prod`.

Primary routes:

- `/analysisParam/analysisParam/isProdprocessView`
- `/processAnalysis/exelogSecond/processBatchViewOut`
- `/processAnalysis/api/trace`
- `/paramDetail/paramDetail/analysisiTask`
- `/paramStatRec/paramStatRec/manualStatProcess`
- `/paramStatRec/paramStatRec/manualStatActive`
