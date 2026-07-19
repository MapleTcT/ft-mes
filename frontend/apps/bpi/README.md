# BPI Console

Independent TypeScript/Vite operator console for the Java 17 BPI service. It implements the Phase 1
overview, candidate review, shadow-batch archive, and checksum-gated rule/topology workflows from
`docs/designs/bpi-interaction-design.md`. The data-quality workbench adds scoped impact summaries,
an impact-ordered incident queue, immutable event evidence, lifecycle history, acknowledge/reassign
and resolve commands.

The browser calls only the same-origin `/bpi-api` boundary. In the MES shell it forwards the existing
`localStorage.ticket` bearer token to the Java 8 adapter; it never receives or signs the internal BPI
JWT. The Vite proxy maps `/bpi-api` to the deterministic simulator during local acceptance.

```bash
npm ci
npm audit --audit-level=moderate
npm run build
npm run test:e2e
```

The Playwright suite exercises START shadow-batch creation, END closure to `CLOSED_RAW`, candidate
rejection without batch creation, batch suspend/resume, rule replay/publication, independent
`APPLIED` versus runtime `READY/DEGRADED/INACTIVE` receipts, and mobile layout.
It also closes a data-quality incident through acknowledge, reassignment and resolution while
asserting that raw events and append-only audit history remain available.
Screenshots are written to `/tmp`, not committed as product assets. Rule browser tests use the
deterministic simulator; real PostgreSQL evidence is recorded separately in
`metadata/bpi-rule-management-acceptance.json`.
