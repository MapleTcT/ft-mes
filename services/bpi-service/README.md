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

The production candidate path is the disabled-by-default `bpi.batch.candidate.v1` Kafka consumer.
It validates Protobuf, canonical key, required headers and explicit tenant/plant/line allowlists,
then acknowledges only after the shared PostgreSQL transaction returns. Exact redelivery is safe
through the inbox/candidate identities; poison records or exhausted retries are published to
`bpi.batch.candidate.dlq.v1`. Enabling the listener without replacing the `_DENY_ALL_` allowlists
cannot write business data.

```bash
make bpi-service-test
```

Real PostgreSQL acceptance additionally requires `BPI_TEST_DATABASE_URL`,
`BPI_TEST_DATABASE_USER`, and `BPI_TEST_DATABASE_PASSWORD`.

The rule-application consumer has a dedicated Embedded Kafka + real PostgreSQL acceptance test:

```bash
JAVA_HOME=/path/to/jdk17 mvn -f acceptance/bpi-runtime/pom.xml -pl :bpi-service -am \
  -Dtest=BpiRuleApplicationKafkaPostgresAcceptanceTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

It verifies `read_committed`, aborted-transaction invisibility, listener restart/replay,
terminal-state protection and DLQ behavior. A transactional producer emulates the Flink sink, so
this test is not evidence that a deployed Flink job emitted the receipt after checkpoint success.

The repository Docker topology keeps Java 8 and Java 17 separate. Start only the isolated BPI
profile with `make up-bpi`. PostgreSQL initialization creates `ft_mes_bpi`, a DDL-owning
`bpi_migrator` role and a DML-only `bpi_service` role; the Flyway one-shot container must complete
before the runtime service starts. Replace every `*-dev-only-*` value in `.env` before deployment.
