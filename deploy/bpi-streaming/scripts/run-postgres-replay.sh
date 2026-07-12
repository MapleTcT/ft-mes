#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
STREAM_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
ROOT_DIR=$(CDPATH= cd -- "$STREAM_DIR/../.." && pwd)
ADP_DIR=$ROOT_DIR/deploy/docker
STREAM_ENV=${1:-$STREAM_DIR/.env}
ADP_ENV=${2:-$ADP_DIR/.env}

for file in "$STREAM_ENV" "$ADP_ENV"; do
    if [ ! -f "$file" ]; then
        printf 'ERROR: required deployment env file not found: %s\n' "$file" >&2
        exit 1
    fi
done

env_value() {
    python3 - "$1" "$2" "$3" <<'PY'
import sys
from pathlib import Path

path, key, fallback = sys.argv[1:]
value = fallback
for raw in Path(path).read_text(encoding="utf-8").splitlines():
    line = raw.rstrip("\r")
    if not line or line.startswith("#"):
        continue
    if "=" not in line:
        raise SystemExit(f"invalid env line in {path}")
    current, candidate = line.split("=", 1)
    if not current.replace("_", "a").isalnum():
        raise SystemExit(f"invalid env key in {path}: {current}")
    if current == key:
        value = candidate
print(value)
PY
}

stream_compose() {
    docker compose --env-file "$STREAM_ENV" -f "$STREAM_DIR/docker-compose.yml" "$@"
}

adp_compose() {
    docker compose --env-file "$ADP_ENV" -f "$ADP_DIR/docker-compose.yml" "$@"
}

MARKER=${BPI_PERSISTENCE_REPLAY_MARKER:-ADP_E2E_$(date -u +%Y%m%d_%H%M%S)_$$}
case $MARKER in
    ""|*[!A-Za-z0-9._-]*) printf 'ERROR: persistence marker contains unsafe characters\n' >&2; exit 1 ;;
esac
if [ "${#MARKER}" -lt 8 ] || [ "${#MARKER}" -gt 80 ]; then
    printf 'ERROR: persistence marker must be 8-80 characters\n' >&2
    exit 1
fi
if grep -q '^BPI_REPLAY_MARKER=' "$STREAM_ENV"; then
    printf 'ERROR: BPI_REPLAY_MARKER must not be declared in the stream env file\n' >&2
    exit 1
fi

TENANT=TENANT-E2E
PLANT=PLANT-E2E
LINE=LINE-$MARKER
PG_USER=$(env_value "$ADP_ENV" POSTGRES_USER adp)
BPI_DB=$(env_value "$ADP_ENV" BPI_DATABASE_NAME ft_mes_bpi)
DLQ_TOPIC=$(env_value "$STREAM_ENV" BPI_CANDIDATE_DLQ_TOPIC bpi.batch.candidate.dlq.v1)
SOURCE_TOPIC=$(env_value "$STREAM_ENV" BPI_CANDIDATE_TOPIC bpi.batch.candidate.v1)
EVIDENCE_DIR=$(env_value "$STREAM_ENV" BPI_REPLAY_EVIDENCE_DIR /tmp/bpi-streaming-evidence)
CONTAINER_REPORT=$(env_value "$STREAM_ENV" BPI_REPLAY_REPORT /evidence/bpi-kafka-replay.json)
REPORT_NAME=${CONTAINER_REPORT#/evidence/}
HOST_REPORT=$EVIDENCE_DIR/$REPORT_NAME
TIMEOUT_DEFAULT=$(env_value "$STREAM_ENV" BPI_PERSISTENCE_REPLAY_TIMEOUT_SECONDS 90)
KEEP_MARKER_DEFAULT=$(env_value "$STREAM_ENV" BPI_PERSISTENCE_REPLAY_KEEP_MARKER false)
TIMEOUT=${BPI_PERSISTENCE_REPLAY_TIMEOUT_SECONDS:-$TIMEOUT_DEFAULT}
KEEP_MARKER=${BPI_PERSISTENCE_REPLAY_KEEP_MARKER:-$KEEP_MARKER_DEFAULT}

case $EVIDENCE_DIR in /*) ;; *) printf 'ERROR: evidence directory must be absolute\n' >&2; exit 1 ;; esac
case $CONTAINER_REPORT in /evidence/*) ;; *) printf 'ERROR: replay report must be under /evidence\n' >&2; exit 1 ;; esac
case $REPORT_NAME in ""|*/*|*..*) printf 'ERROR: replay report must use one safe file name\n' >&2; exit 1 ;; esac
case $TIMEOUT in ""|*[!0-9]*) printf 'ERROR: persistence timeout must be an integer\n' >&2; exit 1 ;; esac
if [ "$TIMEOUT" -lt 10 ] || [ "$TIMEOUT" -gt 600 ]; then
    printf 'ERROR: persistence timeout must be 10-600 seconds\n' >&2
    exit 1
fi
case $KEEP_MARKER in true|false) ;; *) printf 'ERROR: keep marker must be true or false\n' >&2; exit 1 ;; esac

pg_exec() {
    adp_compose exec -T postgres psql -X -v ON_ERROR_STOP=1 -qAt \
        --username "$PG_USER" --dbname "$BPI_DB" "$@"
}

cleanup_marker() {
    [ "$KEEP_MARKER" = false ] || return 0
    pg_exec --set marker="$MARKER" <<'SQL' >/dev/null 2>&1 || true
WITH removed AS (
    DELETE FROM bpi.bpi_batch_candidates
     WHERE tenant_id = 'TENANT-E2E'
       AND line_id = 'LINE-' || :'marker'
     RETURNING candidate_key
)
DELETE FROM bpi.bpi_inbox_events i
 USING removed r
 WHERE i.tenant_id = 'TENANT-E2E'
   AND i.source = 'bpi.batch.candidate.v1'
   AND i.idempotency_key = r.candidate_key::text;

DELETE FROM bpi.bpi_rule_versions
 WHERE tenant_id = 'TENANT-E2E'
   AND rule_code = 'START-' || :'marker'
   AND version = '1';
SQL
}
trap cleanup_marker EXIT HUP INT TERM

service_id=$(adp_compose ps -q bpi-service)
if [ -z "$service_id" ] || [ "$(docker inspect -f '{{.State.Health.Status}}' "$service_id" 2>/dev/null || true)" != healthy ]; then
    printf 'ERROR: healthy bpi-service container is required\n' >&2
    exit 1
fi

container_env() {
    docker inspect -f '{{range .Config.Env}}{{println .}}{{end}}' "$service_id" \
        | awk -F= -v key="$1" '$1 == key {sub(/^[^=]*=/, ""); print; exit}'
}

scope_allows() {
    configured=$(container_env "$1")
    expected=$2
    normalized=$(printf '%s' "$configured" | tr -d ' ')
    case ",$normalized," in *,\*,*|*,$expected,*) return 0 ;; *) return 1 ;; esac
}

if [ "$(container_env BPI_CANDIDATE_KAFKA_ENABLED)" != true ]; then
    printf 'ERROR: bpi-service candidate Kafka consumer is not enabled\n' >&2
    exit 1
fi
if [ "$(container_env BPI_CANDIDATE_KAFKA_TOPIC)" != "$SOURCE_TOPIC" ]; then
    printf 'ERROR: bpi-service candidate source topic differs from the stream deployment\n' >&2
    exit 1
fi
if [ "$(container_env BPI_CANDIDATE_KAFKA_DLQ_TOPIC)" != "$DLQ_TOPIC" ]; then
    printf 'ERROR: bpi-service candidate DLQ topic differs from the stream deployment\n' >&2
    exit 1
fi
scope_allows BPI_CANDIDATE_KAFKA_ALLOWED_TENANT_IDS "$TENANT" \
    || { printf 'ERROR: candidate consumer tenant scope rejects %s\n' "$TENANT" >&2; exit 1; }
scope_allows BPI_CANDIDATE_KAFKA_ALLOWED_PLANT_IDS "$PLANT" \
    || { printf 'ERROR: candidate consumer plant scope rejects %s\n' "$PLANT" >&2; exit 1; }
scope_allows BPI_CANDIDATE_KAFKA_ALLOWED_LINE_IDS "$LINE" \
    || { printf 'ERROR: candidate consumer line scope rejects %s\n' "$LINE" >&2; exit 1; }

pg_exec --set marker="$MARKER" <<'SQL' >/dev/null
INSERT INTO bpi.bpi_topology_versions
    (id, tenant_id, topology_code, version, state, checksum, definition, created_by)
VALUES
    (md5('TENANT-E2E|TOPO-E2E|1')::uuid, 'TENANT-E2E', 'TOPO-E2E', '1',
     'PUBLISHED', 'acceptance-topology-v1', '{}'::jsonb, 'bpi-postgres-replay')
ON CONFLICT (tenant_id, topology_code, version)
DO UPDATE SET state = 'PUBLISHED';

INSERT INTO bpi.bpi_rule_versions
    (id, tenant_id, rule_code, version, topology_version_id, state, checksum, definition, created_by)
SELECT md5('TENANT-E2E|' || :'marker' || '|1')::uuid,
       'TENANT-E2E', 'START-' || :'marker', '1', t.id, 'PUBLISHED',
       'acceptance:' || :'marker', '{}'::jsonb, 'bpi-postgres-replay'
  FROM bpi.bpi_topology_versions t
 WHERE t.tenant_id = 'TENANT-E2E'
   AND t.topology_code = 'TOPO-E2E'
   AND t.version = '1'
ON CONFLICT (tenant_id, rule_code, version)
DO UPDATE SET state = 'PUBLISHED', checksum = EXCLUDED.checksum,
              topology_version_id = EXCLUDED.topology_version_id;
SQL

dlq_end_offset() {
    stream_compose exec -T kafka-1 /opt/kafka/bin/kafka-get-offsets.sh \
        --bootstrap-server kafka-1:19092 --topic "$DLQ_TOPIC" --time -1 \
        | awk -F: '{sum += $3} END {print sum + 0}'
}

DLQ_BEFORE=$(dlq_end_offset)
export BPI_REPLAY_MARKER=$MARKER
sh "$SCRIPT_DIR/run-replay.sh" "$STREAM_ENV"

CANDIDATE_KEY=$(python3 - "$HOST_REPORT" <<'PY'
import json, sys
print(json.load(open(sys.argv[1], encoding="utf-8"))["candidate"]["candidateKey"])
PY
)

deadline=$(( $(date +%s) + TIMEOUT ))
while :; do
    candidate_count=$(pg_exec --set candidate_key="$CANDIDATE_KEY" <<'SQL'
SELECT count(*)
  FROM bpi.bpi_batch_candidates
 WHERE tenant_id = 'TENANT-E2E'
   AND candidate_key = :'candidate_key'::uuid;
SQL
)
    [ "$candidate_count" = 1 ] && break
    if [ "$(date +%s)" -ge "$deadline" ]; then
        printf 'ERROR: candidate did not reach PostgreSQL before timeout\n' >&2
        exit 1
    fi
    sleep 1
done

PERSISTENCE_JSON=$(pg_exec --set candidate_key="$CANDIDATE_KEY" <<'SQL'
SELECT json_build_object(
    'inboxCount', (SELECT count(*) FROM bpi.bpi_inbox_events
                    WHERE tenant_id='TENANT-E2E'
                      AND source='bpi.batch.candidate.v1'
                      AND idempotency_key=:'candidate_key'),
    'candidateCount', count(*),
    'state', min(state),
    'evidenceSource', min(evidence->0->>'source'))::text
  FROM bpi.bpi_batch_candidates
 WHERE tenant_id='TENANT-E2E'
   AND candidate_key=:'candidate_key'::uuid;
SQL
)
DLQ_AFTER=$(dlq_end_offset)

python3 - "$HOST_REPORT" "$PERSISTENCE_JSON" "$DLQ_BEFORE" "$DLQ_AFTER" <<'PY'
import json, sys
from pathlib import Path

path = Path(sys.argv[1])
report = json.loads(path.read_text(encoding="utf-8"))
persistence = json.loads(sys.argv[2])
before, after = int(sys.argv[3]), int(sys.argv[4])
expected = {"inboxCount": 1, "candidateCount": 1, "state": "PENDING", "evidenceSource": "bpi-stream-engine"}
if persistence != expected:
    raise SystemExit(f"unexpected PostgreSQL evidence: {persistence}")
if after != before:
    raise SystemExit(f"candidate DLQ end offset changed during replay: {before} -> {after}")
report["persistence"] = {**persistence,
    "database": "PostgreSQL",
    "dlqEndOffsetBefore": before,
    "dlqEndOffsetAfter": after,
    "status": "PASS",
}
path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"BPI Kafka/Flink/PostgreSQL replay: PASS ({path})")
PY
