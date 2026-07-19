#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
ENV_FILE=${1:-$DEPLOY_DIR/.env}
MARKER=${BPI_FLINK_ROLLBACK_MARKER:-ADP_BPI_FLINK_ROLLBACK_$(date -u +%Y%m%d_%H%M%S)}
REPORT=${BPI_FLINK_ROLLBACK_REPORT:-/tmp/$MARKER.json}
BACKUP_DIR=${BPI_FLINK_ROLLBACK_BACKUP_DIR:-}
ROLLBACK_JAR=${BPI_FLINK_ROLLBACK_JAR:-}

if [ "${BPI_FLINK_ROLLBACK_CONFIRM:-}" != "ROLLBACK_BPI_FLINK_JOB_AND_RESTORE" ]; then
    printf 'ERROR: set BPI_FLINK_ROLLBACK_CONFIRM=ROLLBACK_BPI_FLINK_JOB_AND_RESTORE\n' >&2
    exit 1
fi
if [ ! -f "$ENV_FILE" ]; then
    printf 'ERROR: BPI deployment env file not found: %s\n' "$ENV_FILE" >&2
    exit 1
fi
case $MARKER in
    *[!A-Za-z0-9_-]*|'') printf 'ERROR: invalid Flink rollback marker\n' >&2; exit 1 ;;
esac
case $BACKUP_DIR in
    /) printf 'ERROR: BPI_FLINK_ROLLBACK_BACKUP_DIR cannot be the filesystem root\n' >&2; exit 1 ;;
    /*) ;;
    *) printf 'ERROR: BPI_FLINK_ROLLBACK_BACKUP_DIR must be absolute\n' >&2; exit 1 ;;
esac
case $ROLLBACK_JAR in
    /*) ;;
    *) printf 'ERROR: BPI_FLINK_ROLLBACK_JAR must be an absolute path\n' >&2; exit 1 ;;
esac
if [ ! -f "$ROLLBACK_JAR" ]; then
    printf 'ERROR: rollback JAR not found: %s\n' "$ROLLBACK_JAR" >&2
    exit 1
fi
if [ -e "$BACKUP_DIR" ]; then
    printf 'ERROR: rollback backup directory already exists: %s\n' "$BACKUP_DIR" >&2
    exit 1
fi

env_value() {
    key=$1
    fallback=$2
    value=$(sed -n "s/^$key=//p" "$ENV_FILE" | tail -1)
    if [ -n "$value" ]; then
        printf '%s' "$value"
    else
        printf '%s' "$fallback"
    fi
}

case $(env_value BPI_JOB_JAR '') in
    /*) CURRENT_JAR=$(env_value BPI_JOB_JAR '') ;;
    '') CURRENT_JAR=$DEPLOY_DIR/../../streaming/bpi-stream-engine/target/bpi-stream-engine-0.1.0-SNAPSHOT-job.jar ;;
    *) CURRENT_JAR=$DEPLOY_DIR/$(env_value BPI_JOB_JAR '') ;;
esac
CURRENT_JAR=$(python3 -c 'import os,sys; print(os.path.realpath(sys.argv[1]))' "$CURRENT_JAR")
ROLLBACK_JAR=$(python3 -c 'import os,sys; print(os.path.realpath(sys.argv[1]))' "$ROLLBACK_JAR")
if [ ! -f "$CURRENT_JAR" ]; then
    printf 'ERROR: current JAR not found: %s\n' "$CURRENT_JAR" >&2
    exit 1
fi
if [ "$CURRENT_JAR" = "$ROLLBACK_JAR" ]; then
    printf 'ERROR: current and rollback JAR paths must differ\n' >&2
    exit 1
fi

CURRENT_JAR_SHA256=$(sha256sum "$CURRENT_JAR" | awk '{print $1}')
ROLLBACK_JAR_SHA256=$(sha256sum "$ROLLBACK_JAR" | awk '{print $1}')
if [ "$CURRENT_JAR_SHA256" = "$ROLLBACK_JAR_SHA256" ]; then
    printf 'ERROR: current and rollback JAR content must differ\n' >&2
    exit 1
fi

mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"
ENV_BACKUP=$BACKUP_DIR/bpi-streaming.env.before
cp "$ENV_FILE" "$ENV_BACKUP"
chmod 600 "$ENV_BACKUP"

CURRENT_CAPTURE_REPORT=$BACKUP_DIR/current-savepoint-capture.json
ROLLBACK_RESTORE_REPORT=$BACKUP_DIR/rollback-restore.json
ROLLBACK_CAPTURE_REPORT=$BACKUP_DIR/rollback-savepoint-capture.json
CURRENT_RESTORE_REPORT=$BACKUP_DIR/current-restore.json
RECOVERY_RESTORE_REPORT=$BACKUP_DIR/recovery-restore.json
PHASE=PRECHECK
CURRENT_SAVEPOINT=
ROLLBACK_SAVEPOINT=
CURRENT_RESTORED=false
RECOVERY_STATUS=NOT_REQUIRED
REPORT_WRITTEN=false

update_env() {
    selected_jar=$1
    selected_savepoint=$2
    python3 - "$ENV_FILE" "$selected_jar" "$selected_savepoint" <<'PY'
import os
import stat
import sys
from pathlib import Path

path = Path(sys.argv[1])
updates = {
    "BPI_JOB_JAR": sys.argv[2],
    "BPI_FLINK_RESTORE_SAVEPOINT_PATH": sys.argv[3],
    "BPI_FLINK_ALLOW_NON_RESTORED_STATE": "false",
}
mode = stat.S_IMODE(path.stat().st_mode)
lines = path.read_text(encoding="utf-8").splitlines()
seen = set()
result = []
for line in lines:
    key = line.split("=", 1)[0] if "=" in line else ""
    if key in updates:
        result.append(f"{key}={updates[key]}")
        seen.add(key)
    else:
        result.append(line)
for key, value in updates.items():
    if key not in seen:
        result.append(f"{key}={value}")
temporary = path.with_name(path.name + ".rollback-tmp")
temporary.write_text("\n".join(result) + "\n", encoding="utf-8")
os.chmod(temporary, mode)
temporary.replace(path)
PY
}

json_value() {
    file=$1
    expression=$2
    python3 - "$file" "$expression" <<'PY'
import json
import sys

with open(sys.argv[1], encoding="utf-8") as handle:
    value = json.load(handle)
for part in sys.argv[2].split("."):
    value = value[part]
print(value)
PY
}

capture_savepoint() {
    output=$1
    BPI_SAVEPOINT_CAPTURE_REPORT=$output \
        sh "$SCRIPT_DIR/capture-upgrade-savepoint.sh" "$ENV_FILE"
}

restore_from_configured_savepoint() {
    output=$1
    BPI_SAVEPOINT_RESTORE_REPORT=$output \
    BPI_STREAM_RESTORE_CONFIRM=RESTORE_BPI_FLINK_FROM_SAVEPOINT \
        sh "$SCRIPT_DIR/restore-from-savepoint.sh" "$ENV_FILE"
}

compose() {
    docker compose --env-file "$ENV_FILE" -f "$DEPLOY_DIR/docker-compose.yml" "$@"
}

mounted_job_jar() {
    jobmanager_id=$(compose ps -q bpi-jobmanager)
    if [ -z "$jobmanager_id" ]; then
        return 1
    fi
    docker inspect "$jobmanager_id" | python3 -c '
import json, sys
container = json.load(sys.stdin)[0]
matches = [mount.get("Source", "") for mount in container.get("Mounts", [])
           if mount.get("Destination") == "/opt/flink/usrlib/bpi-stream-engine-job.jar"]
print(matches[0] if len(matches) == 1 else "")
'
}

job_snapshot() {
    rest_url="http://$(env_value BPI_BIND_ADDRESS 127.0.0.1):$(env_value BPI_FLINK_REST_PORT 18081)"
    curl -fsS "$rest_url/jobs/overview" | python3 -c '
import json, sys
jobs = [job for job in json.load(sys.stdin).get("jobs", [])
        if job.get("name") == "ft-mes-bpi-batch-boundary-v1" and job.get("state") == "RUNNING"]
if len(jobs) != 1:
    raise SystemExit("expected exactly one RUNNING BPI job")
job = jobs[0]
print(f"{job.get('"'"'jid'"'"')}|{job.get('"'"'tasks'"'"', {}).get('"'"'running'"'"', 0)}|{job.get('"'"'tasks'"'"', {}).get('"'"'total'"'"', 0)}")
'
}

write_report() {
    status=$1
    phase=$2
    recovery=$3
    export REPORT status phase recovery MARKER BACKUP_DIR ENV_BACKUP
    export CURRENT_JAR CURRENT_JAR_SHA256 ROLLBACK_JAR ROLLBACK_JAR_SHA256
    export CURRENT_CAPTURE_REPORT ROLLBACK_RESTORE_REPORT ROLLBACK_CAPTURE_REPORT
    export CURRENT_RESTORE_REPORT RECOVERY_RESTORE_REPORT CURRENT_SAVEPOINT ROLLBACK_SAVEPOINT
    export CURRENT_RESTORED
    python3 <<'PY'
import datetime
import json
import os
from pathlib import Path

def load_optional(name):
    value = os.environ.get(name, "")
    path = Path(value) if value else None
    if path and path.is_file():
        return json.loads(path.read_text(encoding="utf-8"))
    return None

report = {
    "generatedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
    "status": os.environ["status"],
    "phase": os.environ["phase"],
    "marker": os.environ["MARKER"],
    "strategy": "CURRENT_SAVEPOINT_TO_PREVIOUS_JAR_TO_CURRENT_JAR",
    "statePolicy": {
        "savepointFormat": "CANONICAL",
        "allowNonRestoredState": False,
        "databaseDowngrade": False,
    },
    "artifacts": {
        "currentJar": os.environ["CURRENT_JAR"],
        "currentJarSha256": os.environ["CURRENT_JAR_SHA256"],
        "rollbackJar": os.environ["ROLLBACK_JAR"],
        "rollbackJarSha256": os.environ["ROLLBACK_JAR_SHA256"],
        "environmentBackup": os.environ["ENV_BACKUP"],
        "backupDirectory": os.environ["BACKUP_DIR"],
    },
    "savepoints": {
        "currentBeforeRollback": os.environ.get("CURRENT_SAVEPOINT") or None,
        "capturedFromRollbackJar": os.environ.get("ROLLBACK_SAVEPOINT") or None,
    },
    "stages": {
        "currentCapture": load_optional("CURRENT_CAPTURE_REPORT"),
        "rollbackRestore": load_optional("ROLLBACK_RESTORE_REPORT"),
        "rollbackCapture": load_optional("ROLLBACK_CAPTURE_REPORT"),
        "currentRestore": load_optional("CURRENT_RESTORE_REPORT"),
        "recoveryRestore": load_optional("RECOVERY_RESTORE_REPORT"),
    },
    "recoveryGuard": {
        "status": os.environ["recovery"],
        "currentJarRestored": os.environ["CURRENT_RESTORED"] == "true",
    },
    "persistence": {
        "required": False,
        "status": "NOT_APPLICABLE",
        "reason": "The rehearsal changes only Flink application artifacts and savepoint state.",
    },
}
path = Path(os.environ["REPORT"])
path.parent.mkdir(parents=True, exist_ok=True)
path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(f"BPI Flink job rollback rehearsal: {report['status']} / {report['phase']} ({path})")
PY
}

cleanup() {
    status=$?
    trap - EXIT HUP INT TERM
    if [ "$status" -ne 0 ] && [ "$CURRENT_RESTORED" != true ] && [ -n "$CURRENT_SAVEPOINT" ]; then
        recovery_savepoint=$CURRENT_SAVEPOINT
        if [ -n "$ROLLBACK_SAVEPOINT" ]; then
            recovery_savepoint=$ROLLBACK_SAVEPOINT
        fi
        printf 'Recovery guard: restoring current Flink JAR from %s\n' "$recovery_savepoint" >&2
        update_env "$CURRENT_JAR" "$recovery_savepoint"
        if restore_from_configured_savepoint "$RECOVERY_RESTORE_REPORT"; then
            if [ "$(python3 -c 'import os,sys; print(os.path.realpath(sys.argv[1]))' "$(mounted_job_jar)")" = "$CURRENT_JAR" ]; then
                RECOVERY_STATUS=PASS
                CURRENT_RESTORED=true
            else
                RECOVERY_STATUS=FAILED_MOUNT_MISMATCH
            fi
        else
            RECOVERY_STATUS=FAILED_RESTORE
        fi
    fi
    if [ "$status" -ne 0 ] && [ "$REPORT_WRITTEN" != true ]; then
        write_report FAIL "$PHASE" "$RECOVERY_STATUS" || true
    fi
    exit "$status"
}
trap cleanup EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM

BASELINE_JOB=$(job_snapshot)
PHASE=CURRENT_SAVEPOINT_CAPTURE
capture_savepoint "$CURRENT_CAPTURE_REPORT"
CURRENT_SAVEPOINT=$(json_value "$CURRENT_CAPTURE_REPORT" savepoint.path)
write_report IN_PROGRESS "$PHASE" "$RECOVERY_STATUS"

PHASE=ROLLBACK_JAR_RESTORE
update_env "$ROLLBACK_JAR" "$CURRENT_SAVEPOINT"
restore_from_configured_savepoint "$ROLLBACK_RESTORE_REPORT"
ROLLBACK_MOUNT=$(python3 -c 'import os,sys; print(os.path.realpath(sys.argv[1]))' "$(mounted_job_jar)")
if [ "$ROLLBACK_MOUNT" != "$ROLLBACK_JAR" ]; then
    printf 'ERROR: rollback JobManager mounted %s instead of %s\n' "$ROLLBACK_MOUNT" "$ROLLBACK_JAR" >&2
    exit 1
fi
ROLLBACK_JOB=$(job_snapshot)

PHASE=ROLLBACK_SAVEPOINT_CAPTURE
capture_savepoint "$ROLLBACK_CAPTURE_REPORT"
ROLLBACK_SAVEPOINT=$(json_value "$ROLLBACK_CAPTURE_REPORT" savepoint.path)
write_report IN_PROGRESS "$PHASE" "$RECOVERY_STATUS"

PHASE=CURRENT_JAR_RESTORE
update_env "$CURRENT_JAR" "$ROLLBACK_SAVEPOINT"
restore_from_configured_savepoint "$CURRENT_RESTORE_REPORT"
CURRENT_MOUNT=$(python3 -c 'import os,sys; print(os.path.realpath(sys.argv[1]))' "$(mounted_job_jar)")
if [ "$CURRENT_MOUNT" != "$CURRENT_JAR" ]; then
    printf 'ERROR: restored JobManager mounted %s instead of %s\n' "$CURRENT_MOUNT" "$CURRENT_JAR" >&2
    exit 1
fi
RESTORED_JOB=$(job_snapshot)
CURRENT_RESTORED=true
PHASE=COMPLETE
write_report PASS "$PHASE" "$RECOVERY_STATUS"
REPORT_WRITTEN=true

printf 'Baseline job: %s\n' "$BASELINE_JOB"
printf 'Rollback job: %s\n' "$ROLLBACK_JOB"
printf 'Restored job: %s\n' "$RESTORED_JOB"
