#!/usr/bin/env python3
from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
REPORT_PATH = ROOT / "metadata/production-migration-readiness.json"
DOC_PATH = ROOT / "docs/production-migration-readiness.md"

SOURCE_EVIDENCE_PATHS = {
    "testEnvironmentSmoke": "metadata/test-environment-smoke.json",
    "platformValidationSmoke": "metadata/platform-validation-smoke.json",
    "postgresRuntimeSmoke": "metadata/postgres-runtime-smoke.json",
    "nacosConfigSmoke": "metadata/nacos-config-drift-smoke.json",
    "keycloakJwtSmoke": "metadata/keycloak-jwt-runtime-smoke.json",
    "minioRuntimeSmoke": "metadata/minio-runtime-smoke.json",
    "productionBlockers": "metadata/production-module-blockers.json",
    "productionBacklog": "metadata/production-module-backlog.json",
    "businessDependencyReadiness": "metadata/business-dependency-readiness-smoke.json",
    "businessPackageScan": "metadata/business-dependency-package-scan.json",
    "productionExportReadiness": "metadata/production-export-readiness-smoke.json",
}

REQUIRED_TRACK_IDS = {
    "postgres-data-migration",
    "rollback-plan",
    "license-strategy",
    "minio-file-migration",
    "keycloak-production-db",
    "nacos-runtime-patch",
    "ports-domain-tls",
    "security-hardening",
    "business-smoke-signoff",
}

ALLOWED_TRACK_STATUSES = {"READY", "PLANNED", "BLOCKED", "NOT_STARTED"}
ALLOWED_REPORT_STATUSES = {
    "NOT_READY_FOR_PRODUCTION_MIGRATION",
    "READY_FOR_PRODUCTION_MIGRATION",
}
PLACEHOLDER_EVIDENCE_MARKERS = (
    ".example",
    "example.",
    "-example",
    "_example",
    "template",
    "sample",
)
PLACEHOLDER_VALUES = {"TBD", "TODO", "CHANGE_ME"}
TEST_ENVIRONMENT_EVIDENCE_MARKERS = (
    "100.99.133.43",
    "10.11.100.17",
    "222.88.185.146",
    "ubuntu-test",
    "v6-2288H-V6",
    "metadata/test-environment-smoke.json",
)

TRACK_REQUIRED_KEYS = [
    "id",
    "title",
    "status",
    "owner",
    "scope",
    "currentEvidence",
    "requiredEvidence",
    "readyEvidenceCommands",
    "blockingIssues",
    "nextActions",
    "artifacts",
]

REQUIRED_READY_COMMAND_FRAGMENTS = {
    "postgres-data-migration": [
        "production-source-inventory",
        "production-target-preflight",
        "production-rowcount-compare",
        "production-checksum-compare",
        "production-db-migration-ready-check",
    ],
    "rollback-plan": ["production-rollback-ready-check"],
    "license-strategy": ["production-license-ready-check"],
    "minio-file-migration": [
        "production-minio-source-inventory",
        "production-minio-target-inventory",
        "production-minio-compare",
        "production-minio-migration-ready-check",
    ],
    "keycloak-production-db": [
        "production-keycloak-source-export",
        "production-keycloak-target-export",
        "production-keycloak-compare",
        "production-keycloak-migration-ready-check",
    ],
    "nacos-runtime-patch": [
        "runtime-patch-manifest-check",
        "smoke-nacos-config",
        "production-nacos-runtime-patch-ready-check",
    ],
    "ports-domain-tls": ["production-network-tls-ready-check"],
    "security-hardening": ["production-security-hardening-ready-check"],
    "business-smoke-signoff": [
        "business-package-scan",
        "smoke-business-dependencies",
        "smoke-production-export-readiness",
        "production-business-smoke-signoff-ready-check",
    ],
}

FORBIDDEN_SECRET_LITERALS = [
    "ft" + "123456789",
    "BEGIN PRIVATE KEY",
    "BEGIN RSA PRIVATE KEY",
    "BEGIN OPENSSH PRIVATE KEY",
    "AKIA",
]

SECRET_ASSIGNMENT_RE = re.compile(
    r"(?im)^\s*(password|passwd|secret|token|access[_-]?key|secret[_-]?key)\s*[:=]\s*"
    r"(?!CHANGE_ME|TBD|TODO|<|\$\{|\$)[^\s#]{8,}"
)


def fail(failures: list[str], message: str) -> None:
    failures.append(message)
    print(f"FAIL: {message}", file=sys.stderr)


def as_list(value: Any) -> list[Any]:
    return value if isinstance(value, list) else []


def as_dict(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def read_json_file(relative_path: str, failures: list[str]) -> dict[str, Any]:
    path = ROOT / relative_path
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError:
        fail(failures, f"missing source evidence report: {relative_path}")
        return {}
    except json.JSONDecodeError as error:
        fail(failures, f"invalid JSON in source evidence report {relative_path}: {error}")
        return {}
    if not isinstance(data, dict):
        fail(failures, f"source evidence report must be a JSON object: {relative_path}")
        return {}
    return data


def source_status(data: dict[str, Any]) -> str | None:
    if isinstance(data.get("status"), str):
        return data["status"]
    summary = as_dict(data.get("summary"))
    if isinstance(summary.get("status"), str):
        return summary["status"]
    for key in ("blockers", "blockedCases", "blockedDependencies", "blocked"):
        value = summary.get(key)
        if isinstance(value, int) and value > 0:
            return "BLOCKED"
    if isinstance(data.get("overallStatus"), str):
        return data["overallStatus"]
    if data.get("ok") is True:
        return "PASS"
    if data.get("ok") is False:
        return "FAIL"
    return None


def compact_metrics(key: str, data: dict[str, Any]) -> dict[str, Any]:
    summary = as_dict(data.get("summary"))
    environment = as_dict(data.get("environment"))
    target = as_dict(data.get("target"))
    metric_keys_by_source = {
        "testEnvironmentSmoke": [
            "totalChecks",
            "pass",
            "fail",
            "expectedContainerCount",
            "runningExpectedContainers",
        ],
        "productionBlockers": ["blockers", "blockedCases"],
        "productionBacklog": ["totalItems", "blocked", "failBacklog"],
        "businessDependencyReadiness": ["dependencies", "ready", "blocked"],
        "businessPackageScan": [
            "filesVisited",
            "archivesScanned",
            "nestedArchivesScanned",
            "archiveEntriesScanned",
            "blockedDependencies",
            "candidateDependencies",
        ],
        "productionExportReadiness": [
            "targets",
            "pagePass",
            "visibleExportActions",
            "runtimeExportActions",
            "verifiedDataExports",
            "ready",
            "actionRequired",
            "blocked",
        ],
    }
    if key == "platformValidationSmoke":
        return {
            "ok": data.get("ok"),
            "total": data.get("total"),
            "passed": data.get("passed"),
            "failed": data.get("failed"),
            "baseUrl": data.get("baseUrl"),
            "browserBaseUrl": data.get("browserBaseUrl"),
        }
    if key == "testEnvironmentSmoke":
        metrics = {name: summary.get(name) for name in metric_keys_by_source[key]}
        metrics["sshHost"] = environment.get("sshHost")
        metrics["baseUrl"] = environment.get("baseUrl")
        return metrics
    if key == "businessDependencyReadiness":
        metrics = {name: summary.get(name) for name in metric_keys_by_source[key]}
        metrics["sshHost"] = target.get("sshHost")
        metrics["baseUrl"] = target.get("baseUrl")
        return metrics
    if key in metric_keys_by_source:
        return {name: summary.get(name) for name in metric_keys_by_source[key]}
    return dict(summary)


def source_evidence_entry(key: str, relative_path: str, failures: list[str]) -> dict[str, Any]:
    data = read_json_file(relative_path, failures)
    return {
        "path": relative_path,
        "generatedAt": data.get("generatedAt"),
        "status": source_status(data),
        "metrics": compact_metrics(key, data),
    }


def expected_source_evidence(failures: list[str]) -> dict[str, Any]:
    return {
        key: source_evidence_entry(key, relative_path, failures)
        for key, relative_path in SOURCE_EVIDENCE_PATHS.items()
    }


def check_source_evidence(data: dict[str, Any], failures: list[str]) -> None:
    actual = data.get("sourceEvidence")
    if not isinstance(actual, dict):
        fail(failures, "production migration readiness report must include sourceEvidence")
        return
    expected = expected_source_evidence(failures)
    for key, expected_entry in expected.items():
        if actual.get(key) != expected_entry:
            fail(failures, f"sourceEvidence.{key} is stale or incorrect; update production migration readiness evidence")
    extra_keys = sorted(set(actual) - set(expected))
    if extra_keys:
        fail(failures, "production migration readiness sourceEvidence contains unknown keys: " + ", ".join(extra_keys))


def is_placeholder_evidence_ref(value: Any) -> bool:
    if not isinstance(value, str):
        return False
    stripped = value.strip()
    if not stripped or stripped in PLACEHOLDER_VALUES:
        return True
    lowered = stripped.lower()
    name = Path(stripped).name.lower()
    return any(marker in lowered or marker in name for marker in PLACEHOLDER_EVIDENCE_MARKERS)


def is_test_environment_evidence_ref(value: Any) -> bool:
    if not isinstance(value, str):
        return False
    lowered = value.strip().lower()
    return any(marker.lower() in lowered for marker in TEST_ENVIRONMENT_EVIDENCE_MARKERS)


def check_ready_evidence_refs(track_id: str, field: str, values: list[Any], failures: list[str]) -> None:
    for index, value in enumerate(values):
        if is_placeholder_evidence_ref(value):
            fail(
                failures,
                f"{track_id} READY {field}[{index}] must reference real rehearsal/signoff evidence, not a template/example: {value}",
            )
        if is_test_environment_evidence_ref(value):
            fail(
                failures,
                f"{track_id} READY {field}[{index}] must reference production rehearsal/signoff evidence, not test-environment evidence: {value}",
            )


def check_artifact_path(track_id: str, artifact: Any, failures: list[str]) -> None:
    if not isinstance(artifact, str) or not artifact.strip():
        fail(failures, f"{track_id}.artifacts must contain non-empty relative paths")
        return

    relative = Path(artifact)
    if relative.is_absolute() or ".." in relative.parts:
        fail(failures, f"{track_id} artifact must stay inside the repository: {artifact}")
        return

    path = ROOT / relative
    if not path.exists():
        fail(failures, f"{track_id} artifact does not exist: {artifact}")
        return

    if path.is_dir():
        fail(failures, f"{track_id} artifact must point to a file, not a directory: {artifact}")
        return

    try:
        text = path.read_text(encoding="utf-8")
    except UnicodeDecodeError:
        fail(failures, f"{track_id} artifact must be text and reviewable: {artifact}")
        return

    for literal in FORBIDDEN_SECRET_LITERALS:
        if literal in text:
            fail(failures, f"{track_id} artifact appears to contain a forbidden secret literal: {artifact}")

    if SECRET_ASSIGNMENT_RE.search(text):
        fail(failures, f"{track_id} artifact appears to contain a non-placeholder secret assignment: {artifact}")


def read_json(failures: list[str]) -> dict[str, Any]:
    try:
        with REPORT_PATH.open(encoding="utf-8") as handle:
            data = json.load(handle)
    except FileNotFoundError:
        fail(failures, f"missing readiness report: {REPORT_PATH.relative_to(ROOT)}")
        return {}
    except json.JSONDecodeError as error:
        fail(failures, f"invalid JSON in {REPORT_PATH.relative_to(ROOT)}: {error}")
        return {}
    if not isinstance(data, dict):
        fail(failures, "production migration readiness report must be a JSON object")
        return {}
    return data


def check_doc(failures: list[str]) -> None:
    if not DOC_PATH.exists():
        fail(failures, f"required document missing: {DOC_PATH.relative_to(ROOT)}")
        return

    text = DOC_PATH.read_text(encoding="utf-8")
    required_fragments = [
        "metadata/production-migration-readiness.json",
        "| 轨道 | 当前状态 | 当前证据 | 缺口 |",
        "PostgreSQL 数据迁移脚本",
        "回滚方案",
        "license 策略",
        "MinIO 文件迁移",
        "Keycloak 生产库策略",
        "端口 / 域名 / TLS",
        "安全加固",
        "业务 smoke 签字",
        "metadata/production-module-backlog.json",
        "sourceEvidence",
        "metadata/test-environment-smoke.json",
        "metadata/platform-validation-smoke.json",
    ]
    for fragment in required_fragments:
        if fragment not in text:
            fail(failures, f"production migration readiness document missing required text: {fragment}")


def check_track(index: int, track: Any, failures: list[str]) -> str | None:
    if not isinstance(track, dict):
        fail(failures, f"tracks[{index}] must be an object")
        return None

    for key in TRACK_REQUIRED_KEYS:
        if key not in track:
            fail(failures, f"tracks[{index}] missing required key: {key}")

    track_id = str(track.get("id", "")).strip()
    status = track.get("status")
    if status not in ALLOWED_TRACK_STATUSES:
        fail(failures, f"{track_id or f'tracks[{index}]'} has invalid status: {status!r}")
        return None

    for key in ("title", "owner", "scope"):
        if not str(track.get(key, "")).strip():
            fail(failures, f"{track_id} must include non-empty {key}")

    for key in ("currentEvidence", "requiredEvidence", "readyEvidenceCommands", "blockingIssues", "nextActions", "artifacts"):
        if not isinstance(track.get(key), list):
            fail(failures, f"{track_id}.{key} must be a list")

    if not as_list(track.get("requiredEvidence")):
        fail(failures, f"{track_id} must list requiredEvidence")
    commands = as_list(track.get("readyEvidenceCommands"))
    if not commands:
        fail(failures, f"{track_id} must include readyEvidenceCommands")
    command_text = "\n".join(str(command) for command in commands)
    for fragment in REQUIRED_READY_COMMAND_FRAGMENTS.get(track_id, []):
        if fragment not in command_text:
            fail(failures, f"{track_id} readyEvidenceCommands missing required fragment: {fragment}")

    artifacts = as_list(track.get("artifacts"))
    for artifact in artifacts:
        check_artifact_path(track_id, artifact, failures)

    if status == "READY":
        if not as_list(track.get("currentEvidence")):
            fail(failures, f"{track_id} READY must include currentEvidence")
        if as_list(track.get("blockingIssues")):
            fail(failures, f"{track_id} READY must not include blockingIssues")
        if not artifacts:
            fail(failures, f"{track_id} READY must include at least one artifact")
        check_ready_evidence_refs(track_id, "currentEvidence", as_list(track.get("currentEvidence")), failures)
        check_ready_evidence_refs(track_id, "artifacts", artifacts, failures)
    else:
        if not as_list(track.get("blockingIssues")):
            fail(failures, f"{track_id} {status} must include blockingIssues")
        if not as_list(track.get("nextActions")):
            fail(failures, f"{track_id} {status} must include nextActions")

    return status


def check_summary(data: dict[str, Any], failures: list[str]) -> None:
    summary = data.get("summary")
    if not isinstance(summary, dict):
        fail(failures, "summary must be an object")
        return

    tracks = as_list(data.get("tracks"))
    statuses = [track.get("status") for track in tracks if isinstance(track, dict)]
    expected = {
        "totalTracks": len(tracks),
        "ready": statuses.count("READY"),
        "planned": statuses.count("PLANNED"),
        "blocked": statuses.count("BLOCKED"),
        "notStarted": statuses.count("NOT_STARTED"),
    }
    for key, value in expected.items():
        if summary.get(key) != value:
            fail(failures, f"summary.{key}={summary.get(key)!r} does not match expected {value}")


def check_report(data: dict[str, Any], failures: list[str]) -> None:
    for key in ("schemaVersion", "generatedAt", "repoCommit", "database", "status", "summary", "tracks", "sourceEvidence"):
        if key not in data:
            fail(failures, f"report missing top-level key: {key}")

    if data.get("database") != "PostgreSQL":
        fail(failures, "production migration readiness database must remain PostgreSQL")

    if data.get("status") not in ALLOWED_REPORT_STATUSES:
        fail(failures, f"invalid readiness status: {data.get('status')!r}")

    repo_commit = data.get("repoCommit")
    if not isinstance(repo_commit, str) or len(repo_commit.strip()) < 7:
        fail(failures, "repoCommit must identify the code baseline used for readiness tracking")

    tracks = data.get("tracks")
    if not isinstance(tracks, list):
        fail(failures, "tracks must be a list")
        return

    seen_ids: set[str] = set()
    statuses: list[str] = []
    for index, track in enumerate(tracks):
        status = check_track(index, track, failures)
        if not isinstance(track, dict):
            continue
        track_id = str(track.get("id", "")).strip()
        if track_id in seen_ids:
            fail(failures, f"duplicate track id: {track_id}")
        seen_ids.add(track_id)
        if status:
            statuses.append(status)

    missing_ids = sorted(REQUIRED_TRACK_IDS - seen_ids)
    if missing_ids:
        fail(failures, "readiness report missing required tracks: " + ", ".join(missing_ids))

    extra_ids = sorted(seen_ids - REQUIRED_TRACK_IDS)
    if extra_ids:
        fail(failures, "readiness report contains unknown tracks: " + ", ".join(extra_ids))

    check_summary(data, failures)

    signoff = next((track for track in tracks if isinstance(track, dict) and track.get("id") == "business-smoke-signoff"), {})
    if isinstance(signoff, dict):
        signoff_text = json.dumps(signoff, ensure_ascii=False)
        for required in ("metadata/production-module-blockers.json", "metadata/production-module-backlog.json"):
            if required not in signoff_text:
                fail(failures, f"business-smoke-signoff track must reference {required}")

    if data.get("status") == "READY_FOR_PRODUCTION_MIGRATION":
        if any(status != "READY" for status in statuses):
            fail(failures, "overall READY_FOR_PRODUCTION_MIGRATION requires every track to be READY")
        artifacts = as_list(signoff.get("artifacts")) if isinstance(signoff, dict) else []
        if not artifacts:
            fail(failures, "business-smoke-signoff READY requires a signed artifact")

    check_source_evidence(data, failures)


def main() -> int:
    failures: list[str] = []
    check_doc(failures)
    data = read_json(failures)
    if data:
        check_report(data, failures)
    if failures:
        print(f"Production migration readiness verification failed: {len(failures)} issue(s).", file=sys.stderr)
        return 1
    print("Production migration readiness verification passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
