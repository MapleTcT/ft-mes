#!/usr/bin/env python3
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from collections import Counter
from pathlib import Path
from typing import Iterable


ROOT = Path(__file__).resolve().parents[1]
DEFAULT_REPORT = ROOT / "metadata/ci-required-file-inventory.json"

DIRECT_FILES = {
    ".github/workflows/verify.yml",
    "Makefile",
    "package.json",
    "pom.xml",
    "backend/pom.xml",
    "backend/source-modules/pom.xml",
    "deploy/pom.xml",
}

SCAN_GLOBS = {
    ".github/workflows/*.yml",
    "deploy/business-smoke/production-migration/**/*.json",
    "deploy/business-smoke/production-migration/**/*.md",
    "deploy/business-smoke/production-migration/**/*.py",
    "deploy/database/production-migration/**/*.example",
    "deploy/database/production-migration/**/*.md",
    "deploy/database/production-migration/**/*.py",
    "deploy/database/production-migration/**/*.sh",
    "deploy/database/production-migration/**/*.sql",
    "deploy/database/production-migration/**/*.tsv",
    "deploy/database/production-migration/**/*.txt",
    "deploy/docker/postgres/init/*.sql",
    "deploy/docker/assets/module-static/**/*",
    "deploy/docker/nginx/*.conf",
    "deploy/docker/patches/*/.gitignore",
    "deploy/docker/patches/*/build.sh",
    "deploy/docker/patches/*/src/**/*.java",
    "deploy/docker/scripts/*.js",
    "deploy/docker/scripts/*.py",
    "deploy/docker/scripts/*.sh",
    "deploy/nacos-config/*.properties",
    "deploy/keycloak/production-migration/**/*.example",
    "deploy/keycloak/production-migration/**/*.json",
    "deploy/keycloak/production-migration/**/*.md",
    "deploy/keycloak/production-migration/**/*.py",
    "deploy/keycloak/production-migration/**/*.sh",
    "deploy/license/production-migration/**/*.json",
    "deploy/license/production-migration/**/*.md",
    "deploy/license/production-migration/**/*.py",
    "deploy/minio/production-migration/**/*.example",
    "deploy/minio/production-migration/**/*.json",
    "deploy/minio/production-migration/**/*.md",
    "deploy/minio/production-migration/**/*.py",
    "deploy/minio/production-migration/**/*.sh",
    "deploy/minio/production-migration/**/*.txt",
    "deploy/network/production-migration/**/*.json",
    "deploy/network/production-migration/**/*.md",
    "deploy/network/production-migration/**/*.py",
    "deploy/rollback/production-migration/**/*.json",
    "deploy/rollback/production-migration/**/*.md",
    "deploy/rollback/production-migration/**/*.py",
    "deploy/security/production-migration/**/*.json",
    "deploy/security/production-migration/**/*.md",
    "deploy/security/production-migration/**/*.py",
    "docs/**/*.md",
    "metadata/*.json",
    "metadata/*.png",
    "scripts/*.py",
}

REFERENCE_SOURCE_GLOBS = {
    "Makefile",
    "scripts/*.py",
    "deploy/*/production-migration/scripts/*.py",
}

FORCE_TRACKED_GLOBS = {
    "deploy/docker/patches/*/*.jar",
}

EXCLUDED_INVENTORY_PARTS = {
    "build",
}

PATH_RE = re.compile(
    r"(?<![A-Za-z0-9_./-])"
    r"((?:\.github|backend|deploy|docs|metadata|scripts)/"
    r"[A-Za-z0-9_./@+,-]+)"
)

TRAILING_PUNCTUATION = ".,;:)'\"]}>"


def run_git(args: list[str], path: Path | None = None) -> subprocess.CompletedProcess[str]:
    command = ["git", *args]
    if path is not None:
        command.extend(["--", str(path)])
    return subprocess.run(command, cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE)


def git_bool(args: list[str], path: Path) -> bool:
    return run_git(args, path).returncode == 0


def git_tracked_paths() -> set[str]:
    result = subprocess.run(
        ["git", "ls-files"],
        cwd=ROOT,
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    if result.returncode != 0:
        raise SystemExit(f"git ls-files failed: {result.stderr.strip()}")
    return {line.strip() for line in result.stdout.splitlines() if line.strip()}


def git_ignored_paths(paths: Iterable[str]) -> set[str]:
    path_list = sorted(set(paths))
    if not path_list:
        return set()
    result = subprocess.run(
        ["git", "check-ignore", "--stdin"],
        cwd=ROOT,
        input="\n".join(path_list) + "\n",
        text=True,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    if result.returncode not in {0, 1}:
        raise SystemExit(f"git check-ignore failed: {result.stderr.strip()}")
    return {line.strip() for line in result.stdout.splitlines() if line.strip()}


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def is_inside_metadata_noise(path: Path) -> bool:
    parts = path.relative_to(ROOT).parts
    return len(parts) >= 2 and parts[0] == "metadata" and parts[1] in {"tmp", "tools"}


def is_ignored(path: Path) -> bool:
    return git_bool(["check-ignore", "-q"], path)


def is_tracked(path: Path) -> bool:
    return git_bool(["ls-files", "--error-unmatch"], path)


def add_existing_file(candidates: set[Path], path: Path, report_path: Path) -> None:
    if not path.exists() or not path.is_file():
        return
    if path.resolve() == report_path.resolve():
        return
    if is_inside_metadata_noise(path):
        return
    candidates.add(path.resolve())


def should_exclude_candidate(path: Path) -> bool:
    parts = path.relative_to(ROOT).parts
    if any(part in EXCLUDED_INVENTORY_PARTS for part in parts):
        return True
    if parts == (
        "deploy",
        "docker",
        "patches",
        "configuration-entity-model-compat",
        "configuration-entity-model-compat.jar",
    ):
        return True
    if "qcs-redis-safe-payload" in parts and path.name != ".gitignore":
        return True
    return False


def glob_candidates(report_path: Path) -> set[Path]:
    candidates: set[Path] = set()
    for relative_path in DIRECT_FILES:
        add_existing_file(candidates, ROOT / relative_path, report_path)
    for pattern in SCAN_GLOBS:
        for path in ROOT.glob(pattern):
            if should_exclude_candidate(path):
                continue
            add_existing_file(candidates, path, report_path)
    return candidates


def force_tracked_candidates(report_path: Path) -> set[Path]:
    candidates: set[Path] = set()
    for pattern in FORCE_TRACKED_GLOBS:
        for path in ROOT.glob(pattern):
            if should_exclude_candidate(path):
                continue
            add_existing_file(candidates, path, report_path)
    return candidates


def iter_reference_sources() -> Iterable[Path]:
    for pattern in REFERENCE_SOURCE_GLOBS:
        for path in ROOT.glob(pattern):
            if path.is_file():
                yield path


def referenced_candidates(report_path: Path) -> tuple[set[Path], set[str]]:
    candidates: set[Path] = set()
    missing: set[str] = set()
    for source in iter_reference_sources():
        try:
            text = source.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        for match in PATH_RE.finditer(text):
            raw = match.group(1).rstrip(TRAILING_PUNCTUATION)
            if any(marker in raw for marker in ("*", "$", "{", "}")):
                continue
            if raw.endswith("/.env"):
                continue
            path = ROOT / raw
            if path.resolve() == report_path.resolve():
                continue
            if raw.startswith("scripts/") and not path.exists():
                deploy_docker_relative = ROOT / "deploy/docker" / raw
                if deploy_docker_relative.exists() and deploy_docker_relative.is_file():
                    add_existing_file(candidates, deploy_docker_relative, report_path)
                    continue
            if path.exists() and path.is_file():
                add_existing_file(candidates, path, report_path)
            elif "." in Path(raw).name:
                missing.add(raw)
    return candidates, missing


def inventory_item(path: Path, tracked_paths: set[str], ignored_paths: set[str], force_tracked_paths: set[str]) -> dict[str, object]:
    rel_path = relative(path)
    tracked = rel_path in tracked_paths
    ignored = rel_path in ignored_paths
    if tracked:
        state = "tracked"
    elif rel_path in force_tracked_paths:
        state = "untracked"
    elif ignored:
        state = "ignored"
    else:
        state = "untracked"
    return {
        "path": relative(path),
        "state": state,
        "tracked": tracked,
        "ignored": ignored,
    }


def path_prefix(path_value: str, depth: int) -> str:
    parts = path_value.split("/")
    return "/".join(parts[: min(depth, len(parts))])


def nested_state_counts(items: list[dict[str, object]], depth: int) -> dict[str, dict[str, int]]:
    counts: dict[str, Counter[str]] = {}
    for item in items:
        prefix = path_prefix(str(item["path"]), depth)
        state = str(item["state"])
        counts.setdefault(prefix, Counter())[state] += 1
    return {
        prefix: dict(sorted(counter.items()))
        for prefix, counter in sorted(counts.items())
    }


def untracked_directory_counts(items: list[dict[str, object]], depth: int = 2) -> list[dict[str, object]]:
    counts = Counter(
        path_prefix(str(item["path"]), depth)
        for item in items
        if item["state"] == "untracked"
    )
    return [
        {"pathPrefix": prefix, "count": count}
        for prefix, count in sorted(counts.items(), key=lambda pair: (-pair[1], pair[0]))
    ]


def build_report(report_path: Path) -> dict[str, object]:
    candidates = glob_candidates(report_path)
    forced = force_tracked_candidates(report_path)
    candidates.update(forced)
    referenced, missing = referenced_candidates(report_path)
    candidates.update(referenced)

    tracked_paths = git_tracked_paths()
    ignored_paths = git_ignored_paths(relative(path) for path in candidates)
    force_tracked_paths = {relative(path) for path in forced}
    report_candidates = [
        path
        for path in candidates
        if relative(path) in tracked_paths or relative(path) not in ignored_paths or relative(path) in force_tracked_paths
    ]

    items = [
        inventory_item(path, tracked_paths, ignored_paths, force_tracked_paths)
        for path in sorted(report_candidates, key=relative)
    ]
    state_counts: dict[str, int] = {}
    for item in items:
        state = str(item["state"])
        state_counts[state] = state_counts.get(state, 0) + 1

    return {
        "schemaVersion": 1,
        "description": "Deterministic inventory of repository files used by CI, governance ledgers, runtime smoke evidence, and production migration gates.",
        "policy": {
            "defaultCheck": "inventory-current",
            "strictTrackedMode": "fails when any required non-ignored file is untracked or any referenced file is missing",
            "ignoredFiles": "ignored generated files are excluded unless directly referenced",
        },
        "summary": {
            "totalFiles": len(items),
            "tracked": state_counts.get("tracked", 0),
            "untracked": state_counts.get("untracked", 0),
            "ignored": state_counts.get("ignored", 0),
            "missingReferences": len(missing),
        },
        "stateByTopLevel": nested_state_counts(items, 1),
        "untrackedByDirectory": untracked_directory_counts(items),
        "missingReferences": sorted(missing),
        "items": items,
    }


def write_report(report: dict[str, object], report_path: Path) -> None:
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def read_existing(report_path: Path) -> dict[str, object] | None:
    try:
        with report_path.open(encoding="utf-8") as handle:
            data = json.load(handle)
    except FileNotFoundError:
        return None
    except json.JSONDecodeError as error:
        raise SystemExit(f"Invalid JSON in {report_path.relative_to(ROOT)}: {error}") from error
    if not isinstance(data, dict):
        raise SystemExit(f"{report_path.relative_to(ROOT)} must contain a JSON object")
    return data


def main() -> int:
    parser = argparse.ArgumentParser(description="Generate or verify the CI-required file inventory.")
    parser.add_argument("--report", default=str(DEFAULT_REPORT), help="Inventory report path.")
    parser.add_argument("--check", action="store_true", help="Fail if the existing report is stale.")
    parser.add_argument("--strict-tracked", action="store_true", help="Fail when required files are untracked or missing.")
    args = parser.parse_args()

    report_path = Path(args.report).expanduser()
    if not report_path.is_absolute():
        report_path = ROOT / report_path

    report = build_report(report_path)
    if args.check:
        existing = read_existing(report_path)
        if existing != report:
            print(f"CI required file inventory is stale: {report_path.relative_to(ROOT)}", file=sys.stderr)
            print("Run: make ci-required-file-inventory", file=sys.stderr)
            return 1
    else:
        write_report(report, report_path)
        print(f"CI required file inventory written: {report_path.relative_to(ROOT)}")

    summary = report["summary"]  # type: ignore[index]
    if args.strict_tracked and isinstance(summary, dict):
        missing = int(summary.get("missingReferences") or 0)
        untracked = int(summary.get("untracked") or 0)
        if missing or untracked:
            print(
                f"CI required file strict tracking failed: missingReferences={missing}, untracked={untracked}",
                file=sys.stderr,
            )
            return 1

    if isinstance(summary, dict):
        print(
            "CI required file inventory verification passed "
            f"(tracked={summary.get('tracked')}, untracked={summary.get('untracked')}, missing={summary.get('missingReferences')})."
        )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
