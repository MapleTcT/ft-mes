# BPI Service

This is the independent Java 17/Spring Boot 3.4 authority for BPI production facts. It does not
inherit the recovered Java 8 parent and it never reads or writes WOM, QCS or WMS internal tables.

Modules:

- `batch-rule-runtime`: framework-free deterministic rule primitives shared by live and replay jobs.
- `app`: REST, application services, PostgreSQL repositories and Flyway migrations.

The Phase 1 service accepts boundary candidates through an internal replay/event-ingest boundary.
START confirmation creates one `ACTIVE` shadow batch per tenant/line; END confirmation closes the
matching batch as `CLOSED_RAW`. Candidate review, START/END evidence, state history, inbox/API
idempotency and audit are persisted transactionally in PostgreSQL.

Flyway V10-V12 add the versioned point-catalog readiness boundary. `BPI_ADMIN` imports immutable
source snapshots, while all BPI readers consume only the latest snapshot for their tenant/plant/line
scope. A topology validation pins the snapshot ID and checksum and fails closed when a bound device
is unregistered or inactive, its property/unit is unavailable, or its calibration is not verified.
Publication atomically checks that the pinned snapshot is still current, so a newer source import
cannot race a previously validated topology into production. The runtime `bpi_service` role has only
the DML privileges required by the catalog and calibration repositories and never owns Flyway DDL.

Flyway V17 separates untrusted source calibration claims from MES-approved evidence. A source
`VERIFIED` value cannot make a point operationally ready. `BPI_ADMIN` submits certificate reference,
SHA-256 checksum and validity, a different administrator must approve or reject it, and approved
evidence can be revoked. Readiness requires an effective `APPROVED` record with the exact tenant,
plant, line, product, device, property and calibration version at both snapshot observation time and
current time. The point page exposes the source claim and MES evidence independently; every command
uses idempotency, optimistic revision and audit records.

Flyway V13 separates control-plane application from evaluator runtime readiness. An `APPLIED`
publication proves that Flink accepted the rule identity and checksum after checkpoint; it does not
prove that the evaluator has a usable point catalog. The independent runtime receipt persists
`READY`, `DEGRADED` or `INACTIVE`, its point-catalog event/revision and degradation reason. Older
receipts cannot overwrite newer event-time truth, exact replay is idempotent, and application status
remains unchanged while readiness moves between states.

The production candidate path is the disabled-by-default `bpi.batch.candidate.v1` Kafka consumer.
It validates Protobuf, canonical key, required headers and explicit tenant/plant/line allowlists,
then acknowledges only after the shared PostgreSQL transaction returns. Exact redelivery is safe
through the inbox/candidate identities; poison records or exhausted retries are published to
`bpi.batch.candidate.dlq.v1`. Enabling the listener without replacing the `_DENY_ALL_` allowlists
cannot write business data.

Flyway V19 adds the data-quality incident workbench. The disabled-by-default
`bpi.data-quality.v1` Protobuf consumer validates exact topic/key/headers, payload size, event time
and explicit tenant/plant/line allowlists before committing. Valid events aggregate by scoped
source/device/property/issue identity while retaining immutable raw events. Operators read an
impact-ordered queue with a signed, scope-bound snapshot-cutoff cursor, then move incidents through
`OPEN -> ACKNOWLEDGED -> RESOLVED`; reassignment and resolution append lifecycle/audit records.
A newer event can reopen a resolved incident, while an older late event is retained without
reopening it. Incidents changed after a cursor's `snapshotAt` are intentionally excluded from that
old cursor and become visible after a queue refresh; this is a live-work-queue cutoff, not historical
row versioning. Poison records are routed to `bpi.data-quality.dlq.v1`.

Flyway V20 adds auditable shadow-run acceptance. A run pins one published rule, topology and point-
catalog snapshot, then recomputes publication, application, runtime and operational point readiness
before start. Engineers review only matching `CLOSED_RAW` shadow batches against human boundaries and
reference quantities. Completion requires 7-14 days and the configured sample count; independent admin
approval additionally requires at least 95% boundary agreement, cumulative quantity tolerance and zero
unresolved CRITICAL data-quality incidents. Re-review supersedes rather than deletes history, every
command uses idempotency plus optimistic revision, and approval can never be performed by the creator.
This workflow changes only BPI PostgreSQL state; it does not write WOM, QCS, WMS, PLC or DCS.

```bash
make bpi-service-test
```

Real PostgreSQL acceptance additionally requires `BPI_TEST_DATABASE_URL`,
`BPI_TEST_DATABASE_USER`, and `BPI_TEST_DATABASE_PASSWORD`.

The focused shadow-run lifecycle test uses a fresh PostgreSQL schema migrated through V20:

```bash
JAVA_HOME=/path/to/jdk17 mvn -f services/bpi-service/pom.xml -pl :bpi-service -am \
  -Dtest=BpiShadowRunPostgresAcceptanceTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

The focused data-quality acceptance uses real PostgreSQL and Embedded Kafka:

```bash
JAVA_HOME=/path/to/jdk17 mvn -f services/bpi-service/pom.xml -pl :bpi-service -am \
  -Dtest=DataQualityKafkaRecordProcessorTest,BpiDataQualityKafkaPostgresAcceptanceTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

The rule-application consumer has a dedicated Embedded Kafka + real PostgreSQL acceptance test:

```bash
JAVA_HOME=/path/to/jdk17 mvn -f acceptance/bpi-runtime/pom.xml -pl :bpi-service -am \
  -Dtest=BpiRuleApplicationKafkaPostgresAcceptanceTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

It verifies `read_committed`, aborted-transaction invisibility, listener restart/replay,
application terminal-state protection, independent `DEGRADED -> READY` persistence, stale runtime
receipt suppression, exact replay and topic-specific DLQ behavior. A transactional producer emulates
the Flink sink; actual checkpoint output is covered separately by the Flink MiniCluster acceptance.

The repository Docker topology keeps Java 8 and Java 17 separate. Start only the isolated BPI
profile with `make up-bpi`. PostgreSQL initialization creates `ft_mes_bpi`, a DDL-owning
`bpi_migrator` role and a DML-only `bpi_service` role; the Flyway one-shot container must complete
before the runtime service starts. Replace every `*-dev-only-*` value in `.env` before deployment.
