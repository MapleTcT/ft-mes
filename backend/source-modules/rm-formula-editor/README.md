# RM Formula Editor

`rm-formula-editor` is the source-backed replacement for the recovered RM Batch formula path that depended on IE ActiveX and a localhost `4433` bridge. It provides an authenticated Web editor, PostgreSQL persistence and an auditable HTTP boundary for Batch/DCS delivery.

## API

- `GET /msService/RM/formula-editor/formulas`: search formulas.
- `GET /msService/RM/formula-editor/formulas/{id}`: load a formula with processes and activities.
- `GET /msService/RM/formula-editor/references/materials`: list valid PostgreSQL product master data.
- `GET /msService/RM/formula-editor/references/batch-servers`: list valid Batch server master data.
- `POST /msService/RM/formula-editor/formulas`: create idempotently by request ID.
- `PUT /msService/RM/formula-editor/formulas/{id}`: update with optimistic version checking.
- `POST /msService/RM/formula-editor/formulas/{id}/deliveries`: create a versioned delivery.
- `POST /msService/RM/formula-editor/deliveries/{id}/retry`: retry a failed delivery.

Nginx exposes the Web UI at `/msService/RM/formula/editor` and protects both UI and API with the current ADP session.

## Persistence

Migration `deploy/docker/postgres/init/190-rm-web-formula-editor.sql` adds the visible `Web编辑` entry and the audit tables:

- Existing business tables: `rm_formulas`, `rm_formula_processes`, `rm_process_actives`.
- Revision ledger: `rm_formula_editor_revisions`.
- Delivery ledger: `rm_formula_deliveries`, `rm_formula_delivery_attempts`.

Formula, process and activity changes are written in one JDBC transaction. Request IDs prevent duplicate create/update operations, and the formula version protects concurrent edits.
A valid product is required and both product/Batch server references are checked against PostgreSQL before saving.
All API identifier fields (`id` and `*Id`) are serialized as strings. The recovered platform uses 64-bit keys above JavaScript's safe integer range, so clients must keep identifiers as opaque decimal strings; counters, versions and quantities remain numeric.

## Batch/DCS Boundary

Set `RM_FORMULA_DELIVERY_URL` and, when required, `RM_FORMULA_DELIVERY_TOKEN` for the real target. A delivery stores the exact formula version and records every HTTP attempt, response status and acknowledgement state.

The internal simulator is disabled by default. `deploy/docker/docker-compose.acceptance.yml` enables it only for isolated contract/retry testing. Simulator success is not plant Batch/DCS acceptance; production completion requires the real endpoint, vendor payload/acknowledgement mapping and owner sign-off.

## Verification

```bash
make rm-formula-editor-test
make rm-formula-editor-stage-runtime
make acceptance-rm-web-formula-editor-persistence
make rm-web-formula-editor-acceptance-check
```

Current test-environment evidence is recorded in `metadata/rm-web-formula-editor-acceptance.json` with desktop and mobile screenshots.
The source module currently has 10 unit tests, including immutable retry payload and actual Jackson serialization coverage for unsafe 64-bit identifiers.
