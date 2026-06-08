#!/usr/bin/env python3
from __future__ import annotations

import argparse
import io
import json
import re
import shutil
import zipfile
from pathlib import Path
from typing import Iterable


DB_DIRS = ("postgresql", "mariadb", "mysql", "oracle", "sqlserver")
SOURCE_DB_ORDER = ("mariadb", "mysql", "oracle", "sqlserver")

DBP_PATCH_CLASSES = (
    "com/supcon/supfusion/framework/scaffold/dbp/constants/DbType.class",
    "com/supcon/supfusion/framework/scaffold/dbp/MultiTenantDatasource.class",
    "com/supcon/supfusion/framework/scaffold/dbp/factory/DataSourceRepository.class",
    "com/supcon/supfusion/framework/scaffold/dbp/factory/DefaultDataSourceFactory.class",
    "com/supcon/supfusion/framework/scaffold/dbp/factory/jdbc/DataSourceProvider.class",
    "com/supcon/supfusion/framework/scaffold/dbp/factory/line/PostgresqlDataSourceProductionLine.class",
    "com/supcon/supfusion/framework/scaffold/dbp/factory/populator/MultiTenantDatabasePopulator.class",
    "com/supcon/supfusion/framework/scaffold/dbp/factory/populator/MultiTenantDatabasePopulatorUtils.class",
    "com/supcon/supfusion/framework/scaffold/dbp/factory/populator/version/PostgresqlVersionTableProvider.class",
    "com/supcon/supfusion/framework/scaffold/dbp/factory/populator/version/VersionTableProviderFactory.class",
    "com/supcon/supfusion/framework/scaffold/dbp/util/DataId.class",
    "com/supcon/supfusion/framework/scaffold/dbp/util/TenantUtils.class",
)

UNSUPPORTED_SQL_PATTERNS = (
    "ON DUPLICATE KEY",
    "GROUP_CONCAT",
    "FIND_IN_SET",
    "DATE_FORMAT",
    "STR_TO_DATE",
    "LAST_INSERT_ID",
    "INSERT IGNORE",
)

RISKY_SQL_PATTERNS = UNSUPPORTED_SQL_PATTERNS + (
    " MODIFY COLUMN ",
    " CHANGE COLUMN ",
    "ADD INDEX",
    "DROP INDEX",
    "ENGINE=",
)


def read_zip_entries(data: bytes) -> dict[str, tuple[zipfile.ZipInfo, bytes]]:
    entries: dict[str, tuple[zipfile.ZipInfo, bytes]] = {}
    with zipfile.ZipFile(io.BytesIO(data), "r") as zf:
        for info in zf.infolist():
            entries[info.filename] = (info, zf.read(info.filename))
    return entries


def write_zip_entries(entries: dict[str, tuple[zipfile.ZipInfo, bytes]]) -> bytes:
    out = io.BytesIO()
    with zipfile.ZipFile(out, "w") as zf:
        for name, (info, data) in entries.items():
            new_info = zipfile.ZipInfo(name, date_time=info.date_time)
            new_info.compress_type = info.compress_type
            new_info.comment = info.comment
            new_info.extra = info.extra
            new_info.internal_attr = info.internal_attr
            new_info.external_attr = info.external_attr
            new_info.create_system = info.create_system
            zf.writestr(new_info, data)
    return out.getvalue()


def make_info(name: str, compress_type: int = zipfile.ZIP_DEFLATED) -> zipfile.ZipInfo:
    info = zipfile.ZipInfo(name)
    info.compress_type = compress_type
    return info


def segment_replace(path: str, source: str, target: str) -> str | None:
    parts = path.split("/")
    try:
        index = parts.index(source)
    except ValueError:
        return None
    parts[index] = target
    return "/".join(parts)


def detect_db_segment(path: str) -> str | None:
    parts = path.split("/")
    for part in parts:
        if part in DB_DIRS:
            return part
    return None


def postgres_xml_from_mysqlish(text: str) -> tuple[str, list[str]]:
    converted = text
    converted = converted.replace("`", "")
    converted = re.sub(r"\bIFNULL\s*\(", "COALESCE(", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bNOW\s*\(\s*\)", "CURRENT_TIMESTAMP", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bSYSDATE\s*\(\s*\)", "CURRENT_TIMESTAMP", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bCURDATE\s*\(\s*\)", "CURRENT_DATE", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bTRUE\b", "true", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bFALSE\b", "false", converted, flags=re.IGNORECASE)
    converted = re.sub(
        r"LIMIT\s+(#\{[^}]+\}|\$\{[^}]+\}|\d+)\s*,\s*(#\{[^}]+\}|\$\{[^}]+\}|\d+)",
        r"LIMIT \2 OFFSET \1",
        converted,
        flags=re.IGNORECASE,
    )
    converted = re.sub(r"\bINSERT\s+IGNORE\b", "INSERT", converted, flags=re.IGNORECASE)

    flags = [pattern for pattern in UNSUPPORTED_SQL_PATTERNS if pattern.lower() in converted.lower()]
    return converted, flags


def _strip_inline_comments(text: str) -> str:
    # PostgreSQL stores comments via COMMENT ON statements; inline MySQL COMMENT
    # clauses are removed here so the DDL remains executable.
    text = re.sub(r"\s+COMMENT\s+'(?:\\'|''|[^'])*'", "", text, flags=re.IGNORECASE)
    return re.sub(r'\s+COMMENT\s+"(?:\\"|[^"])*"', "", text, flags=re.IGNORECASE)


def _convert_create_table_indexes(text: str) -> str:
    out_lines: list[str] = []
    for line in text.splitlines():
        match = re.match(
            r"(?P<indent>\s*)(?P<prefix>,?\s*)UNIQUE\s+(?:KEY|INDEX)\s+(?P<name>[A-Za-z0-9_]+)\s*\((?P<cols>[^)]+)\)\s*(?:USING\s+BTREE)?(?P<comma>\s*,?)\s*$",
            line,
            flags=re.IGNORECASE,
        )
        if match:
            out_lines.append(
                f"{match.group('indent')}{match.group('prefix')}CONSTRAINT {match.group('name')} UNIQUE ({match.group('cols')}){match.group('comma')}"
            )
            continue

        if re.match(r"\s*,?\s*(?:KEY|INDEX)\s+[A-Za-z0-9_]+\s*\(", line, flags=re.IGNORECASE):
            continue

        out_lines.append(line)
    return "\n".join(out_lines)


def _convert_alter_indexes(text: str) -> str:
    def add_index(match: re.Match[str]) -> str:
        table = match.group(1)
        index = match.group(2)
        columns = match.group(3)
        return f"CREATE INDEX IF NOT EXISTS {index} ON {table} ({columns});"

    text = re.sub(
        r"ALTER\s+TABLE\s+([A-Za-z0-9_]+)\s+ADD\s+CONSTRAINT\s+([A-Za-z0-9_]+)\s+UNIQUE\s+KEY\s*\(([^;]+)\)\s*;",
        r"ALTER TABLE \1 ADD CONSTRAINT \2 UNIQUE (\3);",
        text,
        flags=re.IGNORECASE,
    )
    text = re.sub(
        r"ALTER\s+TABLE\s+([A-Za-z0-9_]+)\s+ADD\s+(?:INDEX|KEY)\s+([A-Za-z0-9_]+)\s*\(([^;]+)\)\s*;",
        add_index,
        text,
        flags=re.IGNORECASE,
    )
    text = re.sub(
        r"ALTER\s+TABLE\s+([A-Za-z0-9_]+)\s+DROP\s+(?:INDEX|KEY)\s+([A-Za-z0-9_]+)\s*;",
        r"DROP INDEX IF EXISTS \2;",
        text,
        flags=re.IGNORECASE,
    )
    return text


def _add_tenant_id_columns(text: str) -> str:
    def add_column(match: re.Match[str]) -> str:
        body = match.group("body")
        if re.search(r"\btenant_id\b", body, flags=re.IGNORECASE):
            return match.group(0)
        insert = "\n   tenant_id VARCHAR(64) DEFAULT NULL,"
        body_with_column, count = re.subn(
            r"(\n\s*(?:PRIMARY\s+KEY|CONSTRAINT|UNIQUE)\b)",
            insert + r"\1",
            body,
            count=1,
            flags=re.IGNORECASE,
        )
        if count == 0:
            body_with_column = body.rstrip() + insert.rstrip(",") + "\n"
        return f"{match.group('head')}{body_with_column}{match.group('tail')}"

    return re.sub(
        r"(?P<head>CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?[A-Za-z0-9_]+\s*\()(?P<body>.*?)(?P<tail>\n\s*\)\s*;)",
        add_column,
        text,
        flags=re.IGNORECASE | re.DOTALL,
    )


def postgres_sql_from_mysqlish(text: str) -> tuple[str, list[str]]:
    converted = text.replace("`", "")
    converted = re.sub(r"(?m)^\s*##", "--", converted)
    converted = _strip_inline_comments(converted)
    converted = re.sub(r"\bUSING\s+BTREE\b", "", converted, flags=re.IGNORECASE)
    converted = _convert_create_table_indexes(converted)

    converted = re.sub(r"\bBIGINT\s*\(\s*\d+\s*\)", "BIGINT", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bINT\s*\(\s*\d+\s*\)", "INTEGER", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bINTEGER\s*\(\s*\d+\s*\)", "INTEGER", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bSMALLINT\s*\(\s*\d+\s*\)", "SMALLINT", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bTINYINT\s*(?:\(\s*\d+\s*\))?", "SMALLINT", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bDOUBLE(?!\s+PRECISION)\s*(?:\([^)]*\))?", "DOUBLE PRECISION", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bDATETIME\s*(\(\s*\d+\s*\))?", r"TIMESTAMP\1", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bLONGTEXT\b|\bMEDIUMTEXT\b", "TEXT", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bLONGBLOB\b|\bMEDIUMBLOB\b|\bBLOB\b", "BYTEA", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bAUTO_INCREMENT\b", "", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bDEFAULT\s+CHARSET\s*=\s*[A-Za-z0-9_]+", "", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bCHARSET\s*=\s*[A-Za-z0-9_]+", "", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bCOLLATE\s*=\s*[A-Za-z0-9_]+", "", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bENGINE\s*=\s*[A-Za-z0-9_]+", "", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bCOMMENT\s*=\s*'(?:\\'|''|[^'])*'", "", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bINSERT\s+IGNORE\b", "INSERT", converted, flags=re.IGNORECASE)
    converted = re.sub(
        r"\s+ON\s+DUPLICATE\s+KEY\s+UPDATE\s+[^;]+;",
        " ON CONFLICT DO NOTHING;",
        converted,
        flags=re.IGNORECASE | re.DOTALL,
    )
    converted = re.sub(r"\bIFNULL\s*\(", "COALESCE(", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bNOW\s*\(\s*\)", "CURRENT_TIMESTAMP", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bSYSDATE\s*\(\s*\)", "CURRENT_TIMESTAMP", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bCURDATE\s*\(\s*\)", "CURRENT_DATE", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bTRUE\b", "true", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bFALSE\b", "false", converted, flags=re.IGNORECASE)
    converted = re.sub(
        r"\b(DOUBLE\s+PRECISION|SMALLINT|INTEGER|BIGINT|TEXT|BYTEA|TIMESTAMP|BOOLEAN)(DEFAULT|NOT|NULL)\b",
        r"\1 \2",
        converted,
        flags=re.IGNORECASE,
    )
    converted = re.sub(
        r"\b(create_time\s+TIMESTAMP\s+NOT\s+NULL)(?!\s+DEFAULT)",
        r"\1 DEFAULT CURRENT_TIMESTAMP",
        converted,
        flags=re.IGNORECASE,
    )
    converted = _convert_alter_indexes(converted)
    converted = re.sub(r",\s*\)", "\n)", converted)
    converted = _add_tenant_id_columns(converted)
    converted = re.sub(r"[ \t]+;", ";", converted)

    flags = [pattern for pattern in RISKY_SQL_PATTERNS if pattern.lower() in converted.lower()]
    return converted, flags


def collect_donor_classes(jar_paths: Iterable[Path]) -> dict[str, bytes]:
    candidates: list[tuple[int, Path, str, bytes]] = []
    for outer in jar_paths:
        with zipfile.ZipFile(outer, "r") as zf:
            for lib_name in zf.namelist():
                if not (lib_name.startswith("BOOT-INF/lib/scaffold-dbp-") and lib_name.endswith(".jar")):
                    continue
                data = zf.read(lib_name)
                with zipfile.ZipFile(io.BytesIO(data), "r") as nested:
                    names = set(nested.namelist())
                    if all(name in names for name in DBP_PATCH_CLASSES):
                        priority = 0 if "scaffold-dbp-1.0.6.RELEASE.jar" in lib_name else 1
                        candidates.append((priority, outer, lib_name, data))
    for _, outer, lib_name, data in sorted(candidates, key=lambda item: (item[0], str(item[1]), item[2])):
        with zipfile.ZipFile(io.BytesIO(data), "r") as nested:
            class_bytes = {name: nested.read(name) for name in DBP_PATCH_CLASSES}
        if "scaffold-dbp-1.0.6.RELEASE.jar" in lib_name:
            return class_bytes
    if candidates:
        _, _, _, data = sorted(candidates, key=lambda item: (item[0], str(item[1]), item[2]))[0]
        with zipfile.ZipFile(io.BytesIO(data), "r") as nested:
            return {name: nested.read(name) for name in DBP_PATCH_CLASSES}
    raise SystemExit("No scaffold-dbp donor with PostgreSQL classes was found.")


def patch_nested_jar(
    nested_name: str,
    nested_data: bytes,
    donor_classes: dict[str, bytes],
    remove_oracle_drivers: bool,
) -> tuple[bytes, dict]:
    entries = read_zip_entries(nested_data)
    changed = False
    report = {
        "nested": nested_name,
        "dbpPatched": False,
        "oracleDriversRemoved": [],
        "postgresMappersAdded": [],
        "postgresScriptsAdded": [],
        "mapperWarnings": [],
        "scriptWarnings": [],
    }

    if nested_name.startswith("BOOT-INF/lib/scaffold-dbp-"):
        patched_any = False
        for class_name, class_bytes in donor_classes.items():
            current = entries.get(class_name)
            if current is not None and current[1] == class_bytes:
                continue
            base_info = entries.get(class_name, (make_info(class_name), b""))[0]
            entries[class_name] = (base_info, class_bytes)
            patched_any = True
        if patched_any:
            changed = True
            report["dbpPatched"] = True

    if remove_oracle_drivers and re.search(r"/ojdbc[^/]*\.jar$", nested_name):
        report["oracleDriversRemoved"].append(nested_name)
        return b"", report | {"removed": True}

    xml_sources: dict[str, tuple[int, str]] = {}
    for name in list(entries):
        if not name.endswith(".xml"):
            continue
        db_segment = detect_db_segment(name)
        if db_segment not in SOURCE_DB_ORDER:
            continue
        target_name = segment_replace(name, db_segment, "postgresql")
        if target_name is None:
            continue
        priority = SOURCE_DB_ORDER.index(db_segment)
        current = xml_sources.get(target_name)
        if current is None or priority < current[0]:
            xml_sources[target_name] = (priority, name)

    for target_name, (_, source_name) in sorted(xml_sources.items()):
        info, data = entries[source_name]
        try:
            text = data.decode("utf-8")
            converted, warnings = postgres_xml_from_mysqlish(text)
            out_data = converted.encode("utf-8")
        except UnicodeDecodeError:
            out_data = data
            warnings = ["non-utf8-xml-copied-without-conversion"]
        target_info = make_info(target_name, info.compress_type)
        entries[target_name] = (target_info, out_data)
        changed = True
        report["postgresMappersAdded"].append({"from": source_name, "to": target_name})
        for warning in warnings:
            report["mapperWarnings"].append({"file": target_name, "pattern": warning})

    sql_sources: dict[str, tuple[int, str]] = {}
    for name in list(entries):
        if not name.lower().endswith(".sql"):
            continue
        db_segment = detect_db_segment(name)
        if db_segment not in SOURCE_DB_ORDER:
            continue
        target_name = segment_replace(name, db_segment, "postgresql")
        if target_name is None:
            continue
        priority = SOURCE_DB_ORDER.index(db_segment)
        current = sql_sources.get(target_name)
        if current is None or priority < current[0]:
            sql_sources[target_name] = (priority, name)

    for target_name, (_, source_name) in sorted(sql_sources.items()):
        info, data = entries[source_name]
        try:
            text = data.decode("utf-8")
            converted, warnings = postgres_sql_from_mysqlish(text)
            out_data = converted.encode("utf-8")
        except UnicodeDecodeError:
            out_data = data
            warnings = ["non-utf8-sql-copied-without-conversion"]
        target_info = make_info(target_name, info.compress_type)
        entries[target_name] = (target_info, out_data)
        changed = True
        report["postgresScriptsAdded"].append({"from": source_name, "to": target_name})
        for warning in warnings:
            report["scriptWarnings"].append({"file": target_name, "pattern": warning})

    if not changed:
        return nested_data, report
    return write_zip_entries(entries), report


def patch_outer_jar(
    outer: Path,
    donor_classes: dict[str, bytes],
    backup_suffix: str,
    remove_oracle_drivers: bool,
) -> dict:
    original = outer.read_bytes()
    entries = read_zip_entries(original)
    changed = False
    report = {"jar": str(outer), "changed": False, "nestedReports": []}

    for name in list(entries):
        if not (name.startswith("BOOT-INF/lib/") and name.endswith(".jar")):
            continue
        info, nested_data = entries[name]
        patched_data, nested_report = patch_nested_jar(name, nested_data, donor_classes, remove_oracle_drivers)
        report["nestedReports"].append(nested_report)
        if nested_report.get("removed"):
            del entries[name]
            changed = True
            continue
        if patched_data != nested_data:
            entries[name] = (info, patched_data)
            changed = True

    if changed:
        backup = outer.with_name(outer.name + backup_suffix)
        if not backup.exists():
            shutil.copy2(outer, backup)
        outer.write_bytes(write_zip_entries(entries))
        report["changed"] = True
        report["backup"] = str(backup)
    return report


def main() -> None:
    parser = argparse.ArgumentParser(description="Patch ADP Spring Boot runtime JARs for PostgreSQL.")
    parser.add_argument("--base-server", type=Path, required=True, help="Path to bap-server/base-Server")
    parser.add_argument("--report", type=Path, required=True, help="JSON report output path")
    parser.add_argument("--backup-suffix", default=".pre-pgpatch.bak")
    parser.add_argument("--keep-oracle-drivers", action="store_true")
    args = parser.parse_args()

    jars = sorted(args.base_server.glob("*/*.jar"))
    backup_jars = sorted(args.base_server.glob("*/*.jar" + args.backup_suffix))
    donor_classes = collect_donor_classes(backup_jars + jars)
    reports = [
        patch_outer_jar(jar, donor_classes, args.backup_suffix, not args.keep_oracle_drivers)
        for jar in jars
    ]

    summary = {
        "jarCount": len(jars),
        "changedJarCount": sum(1 for item in reports if item["changed"]),
        "postgresMapperCount": sum(
            len(nested.get("postgresMappersAdded", []))
            for item in reports
            for nested in item["nestedReports"]
        ),
        "postgresScriptCount": sum(
            len(nested.get("postgresScriptsAdded", []))
            for item in reports
            for nested in item["nestedReports"]
        ),
        "dbpPatchedCount": sum(
            1
            for item in reports
            for nested in item["nestedReports"]
            if nested.get("dbpPatched")
        ),
        "oracleDriversRemovedCount": sum(
            len(nested.get("oracleDriversRemoved", []))
            for item in reports
            for nested in item["nestedReports"]
        ),
    }
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps({"summary": summary, "reports": reports}, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(summary, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
