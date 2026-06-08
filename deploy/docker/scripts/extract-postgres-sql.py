#!/usr/bin/env python3
from __future__ import annotations

import argparse
import io
import json
import re
import zipfile
from pathlib import Path


def safe_stem(value: str) -> str:
    value = re.sub(r"[^A-Za-z0-9_.-]+", "-", value)
    value = value.strip(".-")
    return value[:36] or "script"


def extract_service_sql(service_dir: Path, output_root: Path) -> dict:
    service = service_dir.name
    out_dir = output_root / service / "postgresql"
    written: list[str] = []
    sequence = 0

    for outer_path in sorted(service_dir.glob("*.jar")):
        with zipfile.ZipFile(outer_path, "r") as outer:
            for nested_name in sorted(outer.namelist()):
                if not (nested_name.startswith("BOOT-INF/lib/") and nested_name.endswith(".jar")):
                    continue
                nested_data = outer.read(nested_name)
                with zipfile.ZipFile(io.BytesIO(nested_data), "r") as nested:
                    for sql_name in sorted(nested.namelist()):
                        if not (
                            sql_name.startswith("META-INF/postgresql/")
                            and sql_name.lower().endswith(".sql")
                        ):
                            continue
                        out_dir.mkdir(parents=True, exist_ok=True)
                        nested_stem = safe_stem(Path(nested_name).stem)
                        sql_stem = safe_stem(Path(sql_name).stem)
                        out_name = f"{sequence:03d}_{nested_stem}_{sql_stem}.sql"
                        if len(out_name) > 60:
                            out_name = f"{sequence:03d}_{sql_stem[:48]}.sql"
                        out_path = out_dir / out_name
                        out_path.write_bytes(nested.read(sql_name))
                        written.append(str(out_path.relative_to(output_root)))
                        sequence += 1

    return {"service": service, "scriptCount": len(written), "scripts": written}


def main() -> None:
    parser = argparse.ArgumentParser(description="Extract generated PostgreSQL SQL scripts per ADP service.")
    parser.add_argument("--base-server", type=Path, required=True, help="Path to bap-server/base-Server")
    parser.add_argument("--output", type=Path, required=True, help="Output root mounted as /opt/adp/sql")
    parser.add_argument("--report", type=Path, help="Optional JSON report path")
    args = parser.parse_args()

    args.output.mkdir(parents=True, exist_ok=True)
    reports = [
        extract_service_sql(service_dir, args.output)
        for service_dir in sorted(path for path in args.base_server.iterdir() if path.is_dir())
    ]
    summary = {
        "serviceCount": len(reports),
        "scriptCount": sum(item["scriptCount"] for item in reports),
        "servicesWithScripts": [item["service"] for item in reports if item["scriptCount"]],
        "reports": reports,
    }
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(json.dumps(summary, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({k: v for k, v in summary.items() if k != "reports"}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
