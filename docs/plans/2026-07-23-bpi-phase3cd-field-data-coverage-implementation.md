# BPI Phase 3C-D Field Data Coverage Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a server-derived Shadow Run field-data coverage projection and a visible operator workbench without enabling model training.

**Architecture:** Extend the existing Shadow Run read model instead of creating a second campaign lifecycle. PostgreSQL computes source trust and fixed training-data thresholds from pinned catalog evidence and ACTIVE human reviews; the simulator, OpenAPI, TypeScript client, and browser acceptance expose the same additive contract.

**Tech Stack:** Java 17, Spring Boot, PostgreSQL 15, OpenAPI JSON, TypeScript/Vite, Node simulation, Playwright.

---

### Task 1: Define the additive domain and API contract

**Files:**
- Create: `services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/domain/ShadowRunSourceCoverage.java`
- Create: `services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/domain/ShadowRunTrainingDataCoverage.java`
- Modify: `services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/domain/ShadowRunView.java`
- Modify: `contracts/bpi-api/openapi.json`
- Modify: `frontend/apps/bpi/src/types.ts`

**Steps:**

1. Add the two immutable records and fixed policy threshold fields.
2. Add `sourceCoverage` and `trainingDataCoverage` to `ShadowRunView`.
3. Add required additive OpenAPI schemas and TypeScript interfaces.
4. Run `python3 scripts/verify-bpi-api-contracts.py`; expect PASS.
5. Commit the contract slice.

### Task 2: Implement PostgreSQL-derived coverage

**Files:**
- Modify: `services/bpi-service/app/src/main/java/com/mapletct/ftmes/bpi/infrastructure/postgres/ShadowRunPostgresRepository.java`
- Modify: `services/bpi-service/app/src/test/java/com/mapletct/ftmes/bpi/BpiShadowRunPostgresAcceptanceTest.java`

**Steps:**

1. Add failing JSON assertions for source counts and incomplete training coverage.
2. Add a direct SQL fixture proving 200 distinct batches, 7 UTC dates, 100 accepted START labels and 10 rejected START labels can satisfy the quantity-only projection.
3. Extend the existing lateral queries and map both records.
4. Keep `readyForApproval` unchanged.
5. Run the PostgreSQL acceptance test; expect all tests PASS.
6. Commit the backend slice.

### Task 3: Keep simulation deterministic

**Files:**
- Modify: `simulation/bpi/server.js`
- Modify: `simulation/bpi/bpi-simulation.test.js`

**Steps:**

1. Add failing assertions for source coverage and the four expected training blockers.
2. Derive production dates and START labels from ACTIVE reviews.
3. Return the same policy, thresholds and blocker codes as Java.
4. Run `node --test simulation/bpi/bpi-simulation.test.js`; expect PASS.
5. Commit the simulation slice.

### Task 4: Expose the operator workbench

**Files:**
- Modify: `frontend/apps/bpi/src/main.ts`
- Modify: `frontend/apps/bpi/src/styles.css`
- Modify: `frontend/apps/bpi/tests/bpi-console.e2e.cjs`

**Steps:**

1. Add failing browser assertions for `固定来源可信度`, `现场数据覆盖`, progress values and blocker codes.
2. Render compact list progress and two detail sections.
3. Keep text explicit that coverage is advisory and training is disabled.
4. Verify 1440px and 390px layouts without page overflow.
5. Run `npm run build` and the full Playwright suite; expect PASS.
6. Commit the UI slice.

### Task 5: Governance, deployment and acceptance

**Files:**
- Modify: `docs/project-objectives.md`
- Modify: `docs/goal-gap-register.md`
- Modify: `metadata/goal-gap-register.json`
- Create: `docs/testing/bpi-field-data-coverage-acceptance.md`
- Create: `metadata/bpi-field-data-coverage-acceptance.json`

**Steps:**

1. Run local API, Java, PostgreSQL, simulator and browser gates.
2. Deploy the exact commit to the single target BPI stack.
3. From the real ADP page, record current source/coverage gaps without fabricating 200 batches or 7 days.
4. Verify PostgreSQL/API/browser consistency, service health, model paths disabled and marker cleanup.
5. Update G-021 as `PARTIAL`, preserving the physical DEVICE/GATEWAY, formal calibration and real 200-batch/7-day gates.
6. Run `make ci`, review the diff, commit, push, and merge `main`.
