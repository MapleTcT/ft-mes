#!/usr/bin/env python3
from __future__ import annotations

import argparse
import importlib.util
import json
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
READINESS_PATH = ROOT / "metadata/production-migration-readiness.json"
CUTOVER_PATH = ROOT / "metadata/production-cutover-gate.json"
REHEARSAL_GENERATOR = ROOT / "scripts/generate-production-rehearsal-plan.py"


def load_module(name: str, path: Path) -> Any:
    spec = importlib.util.spec_from_file_location(name, path)
    if not spec or not spec.loader:
        raise RuntimeError(f"Cannot load module from {path.relative_to(ROOT)}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def read_json(path: Path) -> dict[str, Any]:
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except FileNotFoundError as error:
        raise RuntimeError(f"Missing JSON file: {path.relative_to(ROOT)}") from error
    except json.JSONDecodeError as error:
        raise RuntimeError(f"Invalid JSON in {path.relative_to(ROOT)}: {error}") from error
    if not isinstance(data, dict):
        raise RuntimeError(f"{path.relative_to(ROOT)} must be a JSON object")
    return data


def write_json(path: Path, data: dict[str, Any]) -> None:
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def git_head() -> str:
    try:
        return subprocess.check_output(
            ["git", "rev-parse", "HEAD"],
            cwd=ROOT,
            text=True,
            stderr=subprocess.DEVNULL,
        ).strip()
    except (OSError, subprocess.CalledProcessError):
        return "unknown"


def expected_source_evidence(module: Any, label: str) -> dict[str, Any]:
    failures: list[str] = []
    evidence = module.expected_source_evidence(failures)
    if failures:
        joined = "; ".join(failures)
        raise RuntimeError(f"Cannot build {label} sourceEvidence: {joined}")
    return evidence


def update_ledger(path: Path, expected: dict[str, Any], timestamp: str, head: str) -> bool:
    data = read_json(path)
    updated = json.loads(json.dumps(data))
    updated["generatedAt"] = timestamp
    updated["repoCommit"] = head
    updated["sourceEvidence"] = expected
    if updated == data:
        return False
    write_json(path, updated)
    return True


def check_ledger(path: Path, expected: dict[str, Any], label: str) -> bool:
    data = read_json(path)
    actual = data.get("sourceEvidence")
    if actual == expected:
        print(f"{label} sourceEvidence is current.")
        return True
    print(f"{label} sourceEvidence is stale. Run: make production-source-evidence-refresh", file=sys.stderr)
    return False


def run_rehearsal_generator(check: bool) -> int:
    args = [sys.executable, str(REHEARSAL_GENERATOR.relative_to(ROOT))]
    if check:
        args.append("--check")
    return subprocess.call(args, cwd=ROOT)


def main() -> int:
    parser = argparse.ArgumentParser(
        description=(
            "Refresh production migration/cutover sourceEvidence from current smoke ledgers, "
            "then regenerate the production rehearsal plan."
        )
    )
    parser.add_argument("--check", action="store_true", help="check ledgers are already synchronized")
    args = parser.parse_args()

    readiness_module = load_module(
        "verify_production_migration_readiness",
        ROOT / "scripts/verify-production-migration-readiness.py",
    )
    cutover_module = load_module(
        "verify_production_cutover_gate",
        ROOT / "scripts/verify-production-cutover-gate.py",
    )
    readiness_evidence = expected_source_evidence(readiness_module, "production migration readiness")

    if args.check:
        ok = True
        ok = check_ledger(READINESS_PATH, readiness_evidence, "Production migration readiness") and ok
        cutover_evidence = expected_source_evidence(cutover_module, "production cutover gate")
        ok = check_ledger(CUTOVER_PATH, cutover_evidence, "Production cutover gate") and ok
        return run_rehearsal_generator(check=True) if ok else 1

    timestamp = datetime.now(timezone.utc).replace(microsecond=0).isoformat()
    head = git_head()
    changed_readiness = update_ledger(READINESS_PATH, readiness_evidence, timestamp, head)
    cutover_evidence = expected_source_evidence(cutover_module, "production cutover gate")
    changed_cutover = update_ledger(CUTOVER_PATH, cutover_evidence, timestamp, head)
    print(f"{'Updated' if changed_readiness else 'Current'} {READINESS_PATH.relative_to(ROOT)}")
    print(f"{'Updated' if changed_cutover else 'Current'} {CUTOVER_PATH.relative_to(ROOT)}")
    return run_rehearsal_generator(check=False)


if __name__ == "__main__":
    raise SystemExit(main())
