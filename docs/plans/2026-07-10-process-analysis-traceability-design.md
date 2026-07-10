# ProcessAnalysis Traceability Design

## Scope

Restore the existing WOM traceability interaction without recreating an opaque
legacy analytics product. The service provides batch/material/work-order
traceability from the current PostgreSQL facts and preserves the five legacy
endpoint contracts used by recovered WOM pages.

## Data ownership

WOM remains the owner of manufacturing tasks, process/activity execution,
material input and output. QCS remains the owner of inspection, report and
unqualified disposition. Material WMS remains the owner of completion inbound,
quality release and inventory transactions. ProcessAnalysis reads those facts
and owns only `pa_trace_snapshots`, an idempotent audit of explicit manual
statistics actions.

## Runtime flow

The WOM `prodprocessView` button calls `isProdprocessView`; a real task or task
execution row makes `dealRes=true`. The existing button then opens
`processBatchViewOut`, which renders a source-backed operational page. Its JSON
API returns task summary, process and activity stages, material lineage, QCS
records, WMS records and a chronological timeline.

Manual task/process/activity statistics validate the supplied execution-log ID,
serialize the current source row, and upsert one snapshot by tenant, source type
and source ID. Repeated calls increment the revision instead of duplicating
records.

## Acceptance

Acceptance requires a real WOM row selection and trace-button click, successful
gateway/Nacos routing, a rendered trace page with no console/network errors, an
explicit manual-statistics request, direct PostgreSQL proof in
`pa_trace_snapshots`, and cleanup back to zero for the acceptance marker.
