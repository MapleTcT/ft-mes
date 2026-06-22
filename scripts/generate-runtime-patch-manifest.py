#!/usr/bin/env python3
from __future__ import annotations

import argparse
import hashlib
import json
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
JSON_PATH = ROOT / "metadata/runtime-patch-manifest.json"
DOC_PATH = ROOT / "docs/production-migration/runtime-patch-manifest.md"

PATTERN_GROUPS = [
    {
        "category": "runtime-script",
        "description": "Scripts that render Nacos configs or patch the recovered runtime bundle.",
        "patterns": [
            "deploy/docker/scripts/prepare-runtime-patches.sh",
            "deploy/docker/scripts/render-nacos-configs.py",
            "deploy/docker/scripts/patch-*.py",
            "deploy/docker/scripts/build-test-scdog.sh",
            "deploy/docker/scripts/build-rm-import-transaction-patch.sh",
            "deploy/docker/scripts/build-wom-config-default-patch.sh",
            "deploy/docker/scripts/patch-lims-qcs-inspect-report-service.sh",
        ],
    },
    {
        "category": "nginx-runtime-config",
        "description": "Nginx runtime configuration used by the Docker frontend gateway.",
        "patterns": [
            "deploy/docker/nginx/*.conf",
        ],
    },
    {
        "category": "runtime-static-override",
        "description": "Static frontend runtime overrides mounted into the Docker frontend gateway.",
        "patterns": [
            "deploy/docker/assets/module-static/**/*",
        ],
    },
    {
        "category": "postgres-init-sql",
        "description": "Idempotent PostgreSQL initialization, compatibility, runtime view, and business smoke fixups.",
        "patterns": [
            "deploy/docker/postgres/init/*.sql",
        ],
    },
    {
        "category": "runtime-binary-patch",
        "description": "Compiled runtime patch payloads copied into the recovered Windows package at deploy time.",
        "patterns": [
            "deploy/docker/patches/**/*.jar",
            "deploy/docker/patches/**/*.so",
        ],
    },
    {
        "category": "runtime-patch-source",
        "description": "Source or template files used to rebuild runtime patch payloads.",
        "patterns": [
            "deploy/docker/patches/**/*.java",
            "deploy/docker/patches/**/*.c",
            "deploy/docker/patches/**/*.ftl",
        ],
    },
    {
        "category": "nacos-config-template",
        "description": "Sanitized Nacos configuration templates used by render-nacos-configs.py.",
        "patterns": [
            "deploy/nacos-config/*.properties",
        ],
    },
]

EXCLUDED_PATH_PARTS = {
    "qcs-redis-safe-payload",
    "build",
}


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


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


def collect_entries() -> list[dict[str, Any]]:
    seen: set[Path] = set()
    entries: list[dict[str, Any]] = []
    for group in PATTERN_GROUPS:
        for pattern in group["patterns"]:
            for path in sorted(ROOT.glob(pattern)):
                if not path.is_file() or path in seen:
                    continue
                relative_parts = path.relative_to(ROOT).parts
                if any(part in EXCLUDED_PATH_PARTS for part in relative_parts):
                    continue
                seen.add(path)
                relative = path.relative_to(ROOT).as_posix()
                entries.append(
                    {
                        "path": relative,
                        "category": group["category"],
                        "sizeBytes": path.stat().st_size,
                        "sha256": sha256(path),
                    }
                )
    entries.sort(key=lambda item: (item["category"], item["path"]))
    return entries


def summarize(entries: list[dict[str, Any]]) -> dict[str, Any]:
    by_category: dict[str, int] = {}
    bytes_by_category: dict[str, int] = {}
    for entry in entries:
        category = str(entry["category"])
        by_category[category] = by_category.get(category, 0) + 1
        bytes_by_category[category] = bytes_by_category.get(category, 0) + int(entry["sizeBytes"])
    return {
        "totalFiles": len(entries),
        "totalBytes": sum(int(entry["sizeBytes"]) for entry in entries),
        "byCategory": dict(sorted(by_category.items())),
        "bytesByCategory": dict(sorted(bytes_by_category.items())),
    }


def build_manifest() -> dict[str, Any]:
    entries = collect_entries()
    category_descriptions = {
        str(group["category"]): str(group["description"])
        for group in PATTERN_GROUPS
    }
    return {
        "schemaVersion": 1,
        "generatedAt": datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
        "repoCommit": git_head(),
        "database": "PostgreSQL",
        "purpose": "Production migration runtime patch and Nacos configuration checksum manifest.",
        "summary": summarize(entries),
        "categoryDescriptions": category_descriptions,
        "entries": entries,
        "productionUse": {
            "requiredBeforePromotion": [
                "Render target Nacos configs with production placeholder values.",
                "Compare this manifest with the signed production patch bundle manifest.",
                "Publish only files whose checksum matches the signed manifest.",
                "Keep the previous manifest and patch bundle for rollback.",
                "Run platform and business smoke after Nacos publish and runtime restart.",
            ],
            "rollback": [
                "Restore the previous Nacos config export.",
                "Redeploy the previous runtime patch bundle.",
                "Re-run health checks and smoke tests listed in the production migration readiness ledger.",
            ],
        },
    }


def render_markdown(manifest: dict[str, Any]) -> str:
    lines = [
        "# Runtime Patch Manifest",
        "",
        "This file is generated by `scripts/generate-runtime-patch-manifest.py`.",
        "It records the current runtime patch, PostgreSQL init SQL, and Nacos template checksum surface for production migration review.",
        "",
        "## Summary",
        "",
        "| Field | Value |",
        "| --- | --- |",
        f"| Generated At | `{manifest['generatedAt']}` |",
        f"| Repo Commit | `{manifest['repoCommit']}` |",
        f"| Database Target | `{manifest['database']}` |",
        f"| Total Files | `{manifest['summary']['totalFiles']}` |",
        f"| Total Bytes | `{manifest['summary']['totalBytes']}` |",
        "",
        "## Categories",
        "",
        "| Category | Files | Bytes | Description |",
        "| --- | ---: | ---: | --- |",
    ]
    by_category = manifest["summary"]["byCategory"]
    bytes_by_category = manifest["summary"]["bytesByCategory"]
    descriptions = manifest["categoryDescriptions"]
    for category in sorted(by_category):
        lines.append(
            f"| `{category}` | {by_category[category]} | {bytes_by_category.get(category, 0)} | {descriptions.get(category, '')} |"
        )

    lines.extend(
        [
            "",
            "## Manifest Entries",
            "",
            "| Category | Path | Size | SHA-256 |",
            "| --- | --- | ---: | --- |",
        ]
    )
    for entry in manifest["entries"]:
        lines.append(
            f"| `{entry['category']}` | `{entry['path']}` | {entry['sizeBytes']} | `{entry['sha256']}` |"
        )

    lines.extend(
        [
            "",
            "## Production Use",
            "",
            "Before promotion:",
        ]
    )
    for item in manifest["productionUse"]["requiredBeforePromotion"]:
        lines.append(f"- {item}")
    lines.append("")
    lines.append("Rollback:")
    for item in manifest["productionUse"]["rollback"]:
        lines.append(f"- {item}")
    lines.append("")
    return "\n".join(lines)


def write_outputs(manifest: dict[str, Any]) -> None:
    JSON_PATH.parent.mkdir(parents=True, exist_ok=True)
    DOC_PATH.parent.mkdir(parents=True, exist_ok=True)
    JSON_PATH.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    DOC_PATH.write_text(render_markdown(manifest), encoding="utf-8")


def stable_for_check(manifest: dict[str, Any], existing: dict[str, Any]) -> dict[str, Any]:
    current = json.loads(json.dumps(manifest))
    current["generatedAt"] = existing.get("generatedAt")
    current["repoCommit"] = existing.get("repoCommit")
    return current


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="check generated outputs are current")
    args = parser.parse_args()

    manifest = build_manifest()
    if args.check:
        if not JSON_PATH.exists() or not DOC_PATH.exists():
            print("Runtime patch manifest outputs are missing. Run: make runtime-patch-manifest", file=sys.stderr)
            return 1
        try:
            existing = json.loads(JSON_PATH.read_text(encoding="utf-8"))
        except json.JSONDecodeError as error:
            print(f"Runtime patch manifest JSON is invalid: {error}", file=sys.stderr)
            return 1
        current_for_check = stable_for_check(manifest, existing)
        if existing != current_for_check:
            print("Runtime patch manifest JSON is stale. Run: make runtime-patch-manifest", file=sys.stderr)
            return 1
        expected_doc = render_markdown(existing)
        if DOC_PATH.read_text(encoding="utf-8") != expected_doc:
            print("Runtime patch manifest document is stale. Run: make runtime-patch-manifest", file=sys.stderr)
            return 1
        print("Runtime patch manifest is current.")
        return 0

    write_outputs(manifest)
    print(f"Wrote {JSON_PATH.relative_to(ROOT)}")
    print(f"Wrote {DOC_PATH.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
