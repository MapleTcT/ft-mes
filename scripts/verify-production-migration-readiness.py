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

TRACK_REQUIRED_KEYS = [
    "id",
    "title",
    "status",
    "owner",
    "scope",
    "currentEvidence",
    "requiredEvidence",
    "blockingIssues",
    "nextActions",
    "artifacts",
]

FORBIDDEN_SECRET_LITERALS = [
    "ft123456789",
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

    for key in ("currentEvidence", "requiredEvidence", "blockingIssues", "nextActions", "artifacts"):
        if not isinstance(track.get(key), list):
            fail(failures, f"{track_id}.{key} must be a list")

    if not as_list(track.get("requiredEvidence")):
        fail(failures, f"{track_id} must list requiredEvidence")

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
    for key in ("schemaVersion", "generatedAt", "repoCommit", "database", "status", "summary", "tracks"):
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

    if data.get("status") == "READY_FOR_PRODUCTION_MIGRATION":
        if any(status != "READY" for status in statuses):
            fail(failures, "overall READY_FOR_PRODUCTION_MIGRATION requires every track to be READY")
        signoff = next((track for track in tracks if isinstance(track, dict) and track.get("id") == "business-smoke-signoff"), {})
        artifacts = as_list(signoff.get("artifacts")) if isinstance(signoff, dict) else []
        if not artifacts:
            fail(failures, "business-smoke-signoff READY requires a signed artifact")


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
