#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path


def read_counts(path: Path) -> dict[str, dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        required = {"table_name", "row_count", "status"}
        if not reader.fieldnames or set(reader.fieldnames) < required:
            raise SystemExit(f"{path} must be a TSV with columns: table_name, row_count, status")
        rows: dict[str, dict[str, str]] = {}
        for row in reader:
            table = (row.get("table_name") or "").strip()
            if not table:
                continue
            rows[table] = {
                "row_count": (row.get("row_count") or "").strip(),
                "status": (row.get("status") or "").strip(),
            }
        return rows


def to_int(value: str) -> int | None:
    try:
        return int(value)
    except ValueError:
        return None


def compare(source: dict[str, dict[str, str]], target: dict[str, dict[str, str]]) -> list[dict[str, object]]:
    results: list[dict[str, object]] = []
    for table in sorted(set(source) | set(target)):
        source_row = source.get(table)
        target_row = target.get(table)
        if source_row is None:
            results.append(
                {
                    "table": table,
                    "sourceCount": None,
                    "targetCount": target_row["row_count"] if target_row else None,
                    "delta": None,
                    "status": "SOURCE_MISSING",
                }
            )
            continue
        if target_row is None:
            results.append(
                {
                    "table": table,
                    "sourceCount": source_row["row_count"],
                    "targetCount": None,
                    "delta": None,
                    "status": "TARGET_MISSING",
                }
            )
            continue

        if source_row["status"] != "OK" or target_row["status"] != "OK":
            results.append(
                {
                    "table": table,
                    "sourceCount": source_row["row_count"],
                    "targetCount": target_row["row_count"],
                    "delta": None,
                    "status": "COUNT_ERROR",
                    "sourceStatus": source_row["status"],
                    "targetStatus": target_row["status"],
                }
            )
            continue

        source_count = to_int(source_row["row_count"])
        target_count = to_int(target_row["row_count"])
        if source_count is None or target_count is None:
            results.append(
                {
                    "table": table,
                    "sourceCount": source_row["row_count"],
                    "targetCount": target_row["row_count"],
                    "delta": None,
                    "status": "INVALID_COUNT",
                }
            )
            continue

        delta = target_count - source_count
        results.append(
            {
                "table": table,
                "sourceCount": source_count,
                "targetCount": target_count,
                "delta": delta,
                "status": "MATCH" if delta == 0 else "MISMATCH",
            }
        )
    return results


def write_tsv(path: Path, rows: list[dict[str, object]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(
            handle,
            fieldnames=["table", "sourceCount", "targetCount", "delta", "status"],
            delimiter="\t",
        )
        writer.writeheader()
        for row in rows:
            writer.writerow({key: row.get(key, "") for key in writer.fieldnames})


def main() -> int:
    parser = argparse.ArgumentParser(description="Compare production migration source and target row-count reports.")
    parser.add_argument("--source", type=Path, required=True, help="source-row-counts.tsv")
    parser.add_argument("--target", type=Path, required=True, help="target-row-counts.tsv")
    parser.add_argument("--output-dir", type=Path, required=True, help="directory for comparison reports")
    parser.add_argument("--allow-mismatch", action="store_true", help="write reports but return success even when counts differ")
    args = parser.parse_args()

    source = read_counts(args.source)
    target = read_counts(args.target)
    rows = compare(source, target)

    args.output_dir.mkdir(parents=True, exist_ok=True)
    tsv_path = args.output_dir / "row-count-comparison.tsv"
    json_path = args.output_dir / "row-count-comparison.json"
    write_tsv(tsv_path, rows)

    summary = {
        "totalTables": len(rows),
        "match": sum(1 for row in rows if row["status"] == "MATCH"),
        "mismatch": sum(1 for row in rows if row["status"] == "MISMATCH"),
        "errors": sum(1 for row in rows if row["status"] not in {"MATCH", "MISMATCH"}),
    }
    json_path.write_text(
        json.dumps({"summary": summary, "items": rows}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    print(f"Wrote {tsv_path}")
    print(f"Wrote {json_path}")
    if args.allow_mismatch:
        return 0
    if summary["mismatch"] or summary["errors"]:
        print(
            f"Row-count comparison failed: {summary['mismatch']} mismatches, {summary['errors']} errors.",
        )
        return 1
    print("Row-count comparison passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
