# BPI Console

Independent TypeScript/Vite operator console for the Java 17 BPI service. It implements the Phase 1
overview, candidate review and shadow-batch archive workflows from
`docs/designs/bpi-interaction-design.md`.

The browser calls only the same-origin `/bpi-api` boundary. In the MES shell it forwards the existing
`localStorage.ticket` bearer token to the Java 8 adapter; it never receives or signs the internal BPI
JWT. The Vite proxy maps `/bpi-api` to the deterministic simulator during local acceptance.

```bash
npm ci
npm audit --audit-level=moderate
npm run build
npm run test:e2e
```

The Playwright test exercises desktop candidate confirmation, candidate rejection without batch creation,
and mobile layout. Screenshots are written to `/tmp`, not committed as product assets.
