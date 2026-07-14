#!/bin/sh
set -eu

ROOT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/../../.." && pwd)
REPORT=${BPI_FLINK_ACCEPTANCE_REPORT:-/tmp/bpi-rule-application-flink-kafka-acceptance.json}
case "$REPORT" in
    /*) ;;
    *) REPORT="$ROOT_DIR/$REPORT" ;;
esac

command -v mvn >/dev/null 2>&1 || {
    printf 'ERROR: Maven 3.9+ is required\n' >&2
    exit 1
}

rm -f "$REPORT"
(
    cd "$ROOT_DIR"
    BPI_FLINK_RULE_ACCEPTANCE_ENABLED=true \
    BPI_FLINK_ACCEPTANCE_REPORT="$REPORT" \
    mvn -f streaming/pom.xml -pl bpi-stream-engine -am \
        -Dtest=BpiRuleApplicationFlinkKafkaAcceptanceTest \
        -Dsurefire.failIfNoSpecifiedTests=false test
)

python3 - "$REPORT" <<'PY'
import json
import sys
from pathlib import Path

path = Path(sys.argv[1])
if not path.is_file():
    raise SystemExit(f"ERROR: Flink acceptance report was not written: {path}")
report = json.loads(path.read_text(encoding="utf-8"))
if report.get("status") != "PASS_LOCAL_FLINK_MINICLUSTER_KAFKA":
    raise SystemExit("ERROR: Flink acceptance report is not PASS")
checkpoints = report.get("checkpoints", {})
if checkpoints.get("completedCount", 0) < 3 or checkpoints.get("restoredCount", 0) < 1:
    raise SystemExit("ERROR: Flink checkpoint/restart evidence is incomplete")
print(f"BPI Flink rule-application acceptance: {path}")
PY
