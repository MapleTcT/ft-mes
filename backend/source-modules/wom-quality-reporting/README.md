# wom-quality-reporting

PostgreSQL-first WOM/QCS quality quantity service. It records a standalone bad quantity against an exact WOM production task and output record, then keeps the WMS available/hold quantities consistent.

## Business contract

- The reported quantity is read from the selected `wom_mat_outpt_records` row; clients cannot redefine it.
- `good quantity = reported quantity - bad quantity`.
- Every create and reverse action writes an immutable event and synchronizes a matching WMS allocation.
- `requestId` makes retries idempotent; optimistic versions protect reversal from stale writes.
- QCS opens the same record through `qcs_inspects.source_id -> wom_produce_tasks.id`.
- Reversal restores the WMS line from `PARTIAL` to `QUALIFIED` when no other bad allocation remains.

## Endpoints

Authenticated gateway routes use the `/msService/WOM/quality-quantity` prefix:

- `GET /page?taskId=...` or `GET /page?inspectId=...`
- `GET /tasks`
- `GET /tasks/{taskId}/outputs`
- `GET /reports`
- `GET /quality-context/{inspectId}`
- `POST /reports`
- `POST /reports/{id}/retry`
- `POST /reports/{id}/reverse`
- `POST /reports/{id}/link-quality`

## Build and acceptance

```bash
make wom-quality-reporting-test
make wom-quality-reporting-stage-runtime
make acceptance-wom-quality-quantity-persistence \
  ADP_BASE_URL=http://10.11.100.17:18080 \
  ADP_BROWSER_BASE_URL=http://10.11.100.17:18080 \
  ADP_SSH_HOST=10.11.100.17
```

The live acceptance creates isolated WOM/QCS fixtures, reports `10 / 2 / 8`, verifies browser entry points and API payloads, checks PostgreSQL and WMS allocation states, retries idempotently, rehearses reversal, and removes every marker row.

The canonical schema is `deploy/docker/postgres/init/191-wom-quality-quantity-reporting.sql`.
