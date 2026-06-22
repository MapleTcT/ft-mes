# Production Nacos And Runtime Patch Evidence

This folder contains the machine-readable evidence template and validator for
promoting Nacos configuration and runtime patches to production.

The runtime patch manifest records the checksum surface, but it does not prove a
production publish is ready. A READY production record must prove the rendered
configuration was exported, diffed and reviewed, the patch bundle was signed,
the publish window and restart sequence were executed, post-publish smoke passed
and rollback evidence exists.

## Files

| File | Purpose |
| --- | --- |
| `nacos-runtime-patch-evidence.example.json` | Placeholder-only evidence template for production Nacos/runtime patch promotion. |
| `scripts/validate-nacos-runtime-patch-evidence.py` | Validates required phases, evidence references, READY rules and secret hygiene. |

## Commands

```bash
make production-nacos-runtime-patch-check
make production-nacos-runtime-patch-check NACOS_RUNTIME_PATCH_EVIDENCE=/secure/path/adp-nacos-runtime-patch-evidence.json
make production-nacos-runtime-patch-ready-check NACOS_RUNTIME_PATCH_EVIDENCE=/secure/path/adp-nacos-runtime-patch-evidence.json
```

The default check validates the example template shape. The strict-ready check
is for a real production or rehearsal evidence record and rejects placeholder
values, `.example` paths, template/sample evidence, open blocking issues and
missing phase summaries.

## Production Rule

Do not promote Nacos configuration or runtime patch files from this repository
using only `metadata/runtime-patch-manifest.json`. Promotion requires real
source export, target export or rendered baseline, diff review, signed patch
bundle evidence, post-publish smoke and rollback evidence.
