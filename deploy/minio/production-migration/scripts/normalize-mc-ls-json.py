#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path
from typing import Any


OBJECT_FIELDS = ["bucket", "object_key", "size", "etag", "last_modified", "status"]
SUMMARY_FIELDS = ["bucket", "object_count", "total_size", "status"]


def clean(value: Any) -> str:
    text = "" if value is None else str(value)
    return text.replace("\t", " ").replace("\r", " ").replace("\n", " ").strip()


def read_items(path: Path) -> tuple[list[dict[str, str]], int]:
    rows: list[dict[str, str]] = []
    parse_errors = 0
    for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        if not line.strip():
            continue
        try:
            item = json.loads(line)
        except json.JSONDecodeError:
            parse_errors += 1
            rows.append(
                {
                    "object_key": f"JSON_PARSE_ERROR_LINE_{line_number}",
                    "size": "",
                    "etag": "",
                    "last_modified": "",
                    "status": "ERROR",
                }
            )
            continue
        if not isinstance(item, dict):
            continue

        item_type = clean(item.get("type")).lower()
        key = clean(item.get("key") or item.get("name") or item.get("object"))
        if not key:
            continue
        if item_type in {"folder", "directory"}:
            continue

        rows.append(
            {
                "object_key": key,
                "size": clean(item.get("size")),
                "etag": clean(item.get("etag") or item.get("eTag")),
                "last_modified": clean(item.get("lastModified") or item.get("last_modified") or item.get("time")),
                "status": "OK",
            }
        )
    return rows, parse_errors


def append_rows(path: Path, fieldnames: list[str], rows: list[dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    write_header = not path.exists() or path.stat().st_size == 0
    with path.open("a", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames, delimiter="\t")
        if write_header:
            writer.writeheader()
        for row in rows:
            writer.writerow({field: row.get(field, "") for field in fieldnames})


def to_int(value: str) -> int:
    try:
        return int(value)
    except ValueError:
        return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Normalize `mc ls --recursive --json` output.")
    parser.add_argument("--bucket", required=True)
    parser.add_argument("--input", type=Path, required=True)
    parser.add_argument("--object-output", type=Path, required=True)
    parser.add_argument("--summary-output", type=Path, required=True)
    args = parser.parse_args()

    items, parse_errors = read_items(args.input)
    object_rows = [
        {
            "bucket": args.bucket,
            "object_key": row["object_key"],
            "size": row["size"],
            "etag": row["etag"],
            "last_modified": row["last_modified"],
            "status": row["status"],
        }
        for row in items
    ]
    append_rows(args.object_output, OBJECT_FIELDS, object_rows)

    ok_rows = [row for row in items if row["status"] == "OK"]
    summary_status = "ERROR" if parse_errors else "OK"
    append_rows(
        args.summary_output,
        SUMMARY_FIELDS,
        [
            {
                "bucket": args.bucket,
                "object_count": len(ok_rows),
                "total_size": sum(to_int(row["size"]) for row in ok_rows),
                "status": summary_status,
            }
        ],
    )
    if parse_errors:
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
