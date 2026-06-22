#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path
from typing import Any


def read_inventory(path: Path) -> dict[tuple[str, str], dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        required = {"bucket", "object_key", "size", "etag", "last_modified", "status"}
        if not reader.fieldnames or set(reader.fieldnames) < required:
            raise SystemExit(f"{path} must be a TSV with columns: {', '.join(sorted(required))}")
        rows: dict[tuple[str, str], dict[str, str]] = {}
        for row in reader:
            bucket = (row.get("bucket") or "").strip()
            key = (row.get("object_key") or "").strip()
            if not bucket or not key:
                continue
            rows[(bucket, key)] = {
                "size": (row.get("size") or "").strip(),
                "etag": (row.get("etag") or "").strip(),
                "last_modified": (row.get("last_modified") or "").strip(),
                "status": (row.get("status") or "").strip(),
            }
        return rows


def to_int(value: str) -> int | None:
    try:
        return int(value)
    except ValueError:
        return None


def compare(
    source: dict[tuple[str, str], dict[str, str]],
    target: dict[tuple[str, str], dict[str, str]],
) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for bucket, key in sorted(set(source) | set(target)):
        source_row = source.get((bucket, key))
        target_row = target.get((bucket, key))
        if source_row is None:
            rows.append({"bucket": bucket, "object_key": key, "status": "SOURCE_MISSING"})
            continue
        if target_row is None:
            rows.append(
                {
                    "bucket": bucket,
                    "object_key": key,
                    "source_size": source_row["size"],
                    "source_etag": source_row["etag"],
                    "status": "TARGET_MISSING",
                }
            )
            continue
        if source_row["status"] != "OK" or target_row["status"] != "OK":
            rows.append(
                {
                    "bucket": bucket,
                    "object_key": key,
                    "source_size": source_row["size"],
                    "target_size": target_row["size"],
                    "status": "INVENTORY_ERROR",
                    "source_status": source_row["status"],
                    "target_status": target_row["status"],
                }
            )
            continue

        source_size = to_int(source_row["size"])
        target_size = to_int(target_row["size"])
        etag_checked = bool(source_row["etag"] and target_row["etag"])
        size_match = source_size is not None and source_size == target_size
        etag_match = (not etag_checked) or source_row["etag"] == target_row["etag"]
        if size_match and etag_match:
            status = "MATCH"
        elif not size_match:
            status = "SIZE_MISMATCH"
        else:
            status = "ETAG_MISMATCH"
        rows.append(
            {
                "bucket": bucket,
                "object_key": key,
                "source_size": source_row["size"],
                "target_size": target_row["size"],
                "source_etag": source_row["etag"],
                "target_etag": target_row["etag"],
                "status": status,
            }
        )
    return rows


def write_tsv(path: Path, rows: list[dict[str, Any]]) -> None:
    fields = [
        "bucket",
        "object_key",
        "source_size",
        "target_size",
        "source_etag",
        "target_etag",
        "source_status",
        "target_status",
        "status",
    ]
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, delimiter="\t")
        writer.writeheader()
        for row in rows:
            writer.writerow({field: row.get(field, "") for field in fields})


def main() -> int:
    parser = argparse.ArgumentParser(description="Compare source and target MinIO object inventories.")
    parser.add_argument("--source", type=Path, required=True, help="source-object-inventory.tsv")
    parser.add_argument("--target", type=Path, required=True, help="target-object-inventory.tsv")
    parser.add_argument("--output-dir", type=Path, required=True, help="directory for comparison reports")
    parser.add_argument("--allow-mismatch", action="store_true", help="write reports but return success even on differences")
    args = parser.parse_args()

    rows = compare(read_inventory(args.source), read_inventory(args.target))
    args.output_dir.mkdir(parents=True, exist_ok=True)
    tsv_path = args.output_dir / "object-inventory-comparison.tsv"
    json_path = args.output_dir / "object-inventory-comparison.json"
    write_tsv(tsv_path, rows)

    summary = {
        "totalObjects": len(rows),
        "match": sum(1 for row in rows if row["status"] == "MATCH"),
        "missing": sum(1 for row in rows if row["status"] in {"SOURCE_MISSING", "TARGET_MISSING"}),
        "mismatch": sum(1 for row in rows if row["status"] in {"SIZE_MISMATCH", "ETAG_MISMATCH"}),
        "errors": sum(1 for row in rows if row["status"] == "INVENTORY_ERROR"),
    }
    json_path.write_text(
        json.dumps({"summary": summary, "items": rows}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Wrote {tsv_path}")
    print(f"Wrote {json_path}")
    if args.allow_mismatch:
        return 0
    if summary["missing"] or summary["mismatch"] or summary["errors"]:
        print(
            "MinIO inventory comparison failed: "
            f"{summary['missing']} missing, {summary['mismatch']} mismatches, {summary['errors']} errors."
        )
        return 1
    print("MinIO inventory comparison passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
