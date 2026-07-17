# WOM Print

`wom-print` restores the WOM production-task QR workflow as a buildable Java 8
source module. The default runtime is PostgreSQL; the recovered Oracle/Redis
implementation is reference material only.

The service owns:

- `GET /msService/WOM/printManage/printDate/generateCode`
- `POST /msService/WOM/printManage/generateQrCode`
- `POST /msService/WOM/printManage/backfill-printInfo`
- QR PNG rendering and task-scoped generation history
- daily `yyMMdd + 5 digit sequence` allocation with transactional locking
- request-id idempotency and payload-conflict detection

`getPrintByLineId` deliberately returns no automatic printer until a real
line-to-printer mapping source is available. The generation page lets the
operator select a configured printer explicitly instead of guessing the first
device.

The generated QR payload preserves the recovered product contract:

```text
[batch],[unique code],[material code],[manufacture date],[expiry date],G0001
```

Build and stage it with:

```bash
make wom-print-test
make wom-print-stage-runtime
```

Schema migration: `deploy/docker/postgres/init/188-wom-print-qrcode.sql`.
