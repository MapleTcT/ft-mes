#!/usr/bin/env python3
from __future__ import annotations

import argparse
import importlib.util
import json
from pathlib import Path


def load_runtime_patch_module(script_path: Path):
    spec = importlib.util.spec_from_file_location("patch_postgres_runtime", script_path)
    if spec is None or spec.loader is None:
        raise SystemExit(f"Cannot load {script_path}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def iter_mapper_files(root: Path):
    for path in root.rglob("*.xml"):
        parts = set(path.parts)
        if parts & {".git", "__pycache__", "node_modules", "target", "dist", "metadata"}:
            continue
        if parts & {"oracle", "mysql", "mariadb", "sqlserver", "postgresql"}:
            continue
        yield path


def main() -> None:
    parser = argparse.ArgumentParser(description="Rewrite default ADP mapper XML files for PostgreSQL runtime.")
    parser.add_argument("roots", nargs="+", type=Path, help="Source directories to rewrite.")
    parser.add_argument("--runtime-patch-script", type=Path, default=Path(__file__).with_name("patch-postgres-runtime.py"))
    parser.add_argument("--report", type=Path, help="Optional JSON report path.")
    args = parser.parse_args()

    patch_module = load_runtime_patch_module(args.runtime_patch_script.resolve())
    changed: list[dict[str, object]] = []
    for root in args.roots:
        root = root.resolve()
        for path in iter_mapper_files(root):
            original = path.read_text(encoding="utf-8")
            converted, warnings = patch_module.postgres_xml_from_dynamic_dbtype(original)
            if converted == original:
                continue
            path.write_text(converted, encoding="utf-8")
            changed.append(
                {
                    "file": str(path),
                    "warnings": warnings,
                }
            )

    summary = {"changedFileCount": len(changed), "changedFiles": changed}
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({"changedFileCount": len(changed)}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
