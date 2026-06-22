#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path
from typing import Any


def normalized_row(row: dict[str, str]) -> dict[str, str]:
    return {str(key).strip().lower(): (value or "").strip() for key, value in row.items()}


def table_name(row: dict[str, str], path: Path) -> str:
    direct = row.get("table_name", "").strip()
    if direct:
        return direct

    schema = row.get("schema", "").strip()
    table = row.get("table", "").strip() or row.get("name", "").strip()
    if schema and table:
        return f"{schema}.{table}"
    if table:
        return table

    raise SystemExit(f"{path} must include table_name or schema/table columns")


def read_checksums(path: Path) -> dict[str, dict[str, str]]:
    with path.open(encoding="utf-8", newline="") as handle:
        reader = csv.DictReader(handle, delimiter="\t")
        if not reader.fieldnames:
            raise SystemExit(f"{path} must be a TSV with columns: table_name, checksum")

        normalized_fields = {field.strip().lower() for field in reader.fieldnames}
        if "checksum" not in normalized_fields:
            raise SystemExit(f"{path} must include a checksum column")
        if "table_name" not in normalized_fields and "table" not in normalized_fields:
            raise SystemExit(f"{path} must include table_name or schema/table columns")

        rows: dict[str, dict[str, str]] = {}
        for raw_row in reader:
            row = normalized_row(raw_row)
            table = table_name(row, path)
            checksum = row.get("checksum", "").strip().lower()
            rows[table] = {
                "row_count": row.get("row_count", "").strip(),
                "checksum": checksum,
                "status": (row.get("status", "OK").strip() or "OK").upper(),
            }
        return rows


def status_for_rows(source: dict[str, str], target: dict[str, str]) -> str:
    if source["status"] != "OK" or target["status"] != "OK":
        return "CHECKSUM_ERROR"
    if not source["checksum"] or not target["checksum"]:
        return "MISSING_CHECKSUM"
    if (
        source["row_count"]
        and target["row_count"]
        and source["row_count"] != target["row_count"]
    ):
        return "ROW_COUNT_MISMATCH"
    if source["checksum"] != target["checksum"]:
        return "MISMATCH"
    return "MATCH"


def compare(source: dict[str, dict[str, str]], target: dict[str, dict[str, str]]) -> list[dict[str, Any]]:
    results: list[dict[str, Any]] = []
    for table in sorted(set(source) | set(target)):
        source_row = source.get(table)
        target_row = target.get(table)
        if source_row is None:
            results.append(
                {
                    "table": table,
                    "sourceRowCount": None,
                    "targetRowCount": target_row["row_count"] if target_row else None,
                    "sourceChecksum": None,
                    "targetChecksum": target_row["checksum"] if target_row else None,
                    "status": "SOURCE_MISSING",
                }
            )
            continue
        if target_row is None:
            results.append(
                {
                    "table": table,
                    "sourceRowCount": source_row["row_count"],
                    "targetRowCount": None,
                    "sourceChecksum": source_row["checksum"],
                    "targetChecksum": None,
                    "status": "TARGET_MISSING",
                }
            )
            continue

        status = status_for_rows(source_row, target_row)
        result: dict[str, Any] = {
            "table": table,
            "sourceRowCount": source_row["row_count"],
            "targetRowCount": target_row["row_count"],
            "sourceChecksum": source_row["checksum"],
            "targetChecksum": target_row["checksum"],
            "status": status,
        }
        if status == "CHECKSUM_ERROR":
            result["sourceStatus"] = source_row["status"]
            result["targetStatus"] = target_row["status"]
        results.append(result)
    return results


def write_tsv(path: Path, rows: list[dict[str, Any]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(
            handle,
            fieldnames=[
                "table",
                "sourceRowCount",
                "targetRowCount",
                "sourceChecksum",
                "targetChecksum",
                "status",
            ],
            delimiter="\t",
        )
        writer.writeheader()
        for row in rows:
            writer.writerow({key: row.get(key, "") for key in writer.fieldnames})


def summarize(rows: list[dict[str, Any]]) -> dict[str, int]:
    mismatch_statuses = {"MISMATCH", "ROW_COUNT_MISMATCH"}
    return {
        "totalTables": len(rows),
        "match": sum(1 for row in rows if row["status"] == "MATCH"),
        "mismatch": sum(1 for row in rows if row["status"] in mismatch_statuses),
        "missing": sum(1 for row in rows if row["status"] in {"SOURCE_MISSING", "TARGET_MISSING"}),
        "errors": sum(1 for row in rows if row["status"] in {"CHECKSUM_ERROR", "MISSING_CHECKSUM"}),
    }


def main() -> int:
    parser = argparse.ArgumentParser(description="Compare production migration source and target checksum reports.")
    parser.add_argument("--source", type=Path, required=True, help="source-checksums.tsv")
    parser.add_argument("--target", type=Path, required=True, help="target-checksums.tsv")
    parser.add_argument("--output-dir", type=Path, required=True, help="directory for comparison reports")
    parser.add_argument("--allow-mismatch", action="store_true", help="write reports but return success even when checksums differ")
    args = parser.parse_args()

    source = read_checksums(args.source)
    target = read_checksums(args.target)
    rows = compare(source, target)

    args.output_dir.mkdir(parents=True, exist_ok=True)
    tsv_path = args.output_dir / "checksum-comparison.tsv"
    json_path = args.output_dir / "checksum-comparison.json"
    write_tsv(tsv_path, rows)

    summary = summarize(rows)
    json_path.write_text(
        json.dumps({"summary": summary, "items": rows}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )

    print(f"Wrote {tsv_path}")
    print(f"Wrote {json_path}")
    if args.allow_mismatch:
        return 0
    if summary["mismatch"] or summary["missing"] or summary["errors"]:
        print(
            "Checksum comparison failed: "
            f"{summary['mismatch']} mismatches, {summary['missing']} missing, {summary['errors']} errors.",
        )
        return 1
    print("Checksum comparison passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
