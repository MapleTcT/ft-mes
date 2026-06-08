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
    converted = re.sub(
        r"\bGROUP_CONCAT\s*\(\s*([^)]+?)\s*\)",
        r"STRING_AGG(\1::text, ',')",
        converted,
        flags=re.IGNORECASE,
    )
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


def _if_line_dbtype(line: str) -> str | None:
    match = re.search(r"<if\s+test=\"([^\"]*\bdbType\b[^\"]*)\"", line)
    if match:
        return match.group(1)
    return None


def _line_if_depth(line: str) -> int:
    return len(re.findall(r"<if\b", line)) - len(re.findall(r"</if>", line))


def _postgres_if_test(original: str) -> str:
    if "dbType == null" in original or "dbType==null" in original or "dbType == ''" in original or "dbType==''" in original:
        return "dbType == null or dbType == '' or dbType == 'postgresql'"
    return "dbType == 'postgresql'"


def _replace_if_test(line: str, new_test: str) -> str:
    return re.sub(r"(<if\s+test=\")[^\"]*(\")", rf"\1{new_test}\2", line, count=1)


def _convert_postgres_xml_sql_line(line: str) -> str:
    converted = line.replace("`", "")
    converted = re.sub(
        r"\blimit\s+(#\{[^}]+\}|\$\{[^}]+\}|\d+)\s*,\s*(#\{[^}]+\}|\$\{[^}]+\}|\d+)",
        r"limit \2 offset \1",
        converted,
        flags=re.IGNORECASE,
    )
    converted = re.sub(r"\bIFNULL\s*\(", "COALESCE(", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bNOW\s*\(\s*\)", "CURRENT_TIMESTAMP", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bSYSDATE\s*\(\s*\)", "CURRENT_TIMESTAMP", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bCURDATE\s*\(\s*\)", "CURRENT_DATE", converted, flags=re.IGNORECASE)
    converted = re.sub(
        r"length\s*\(\s*MNE_CODE\s*\)\s*!=\s*char_length\s*\(\s*MNE_CODE\s*\)",
        "length(MNE_CODE) != octet_length(MNE_CODE)",
        converted,
        flags=re.IGNORECASE,
    )
    return converted


def postgres_xml_from_dynamic_dbtype(text: str) -> tuple[str, list[str]]:
    """Rewrite legacy dbType branches in default mapper XML to PostgreSQL-only SQL.

    Some ADP mapper XML files are loaded from classpath:mappers/*.xml and contain
    mutually-exclusive Oracle/MySQL/SQLServer branches. For PostgreSQL runtime,
    keeping those files unchanged means dbType=postgresql can render an empty SQL
    fragment. This keeps the same mapper namespace/id surface while removing
    Oracle/SQLServer branches from the default runtime mapper and converting the
    MySQL/MariaDB branch into the PostgreSQL branch.
    """
    if "dbType" not in text:
        return text, []

    output: list[str] = []
    skip_depth = 0
    changed = False
    branch_count = 0
    removed_count = 0

    for line in text.splitlines(keepends=True):
        if skip_depth:
            skip_depth += _line_if_depth(line)
            if skip_depth <= 0:
                skip_depth = 0
            changed = True
            continue

        test = _if_line_dbtype(line)
        if test is not None:
            lowered = test.lower()
            has_pg = "postgresql" in lowered or "postgres" in lowered
            has_not_oracle = re.search(r"dbtype\s*!=\s*'oracle'", lowered) is not None
            has_mysqlish = "mysql" in lowered or "mariadb" in lowered or "dbtype == null" in lowered or "dbtype==null" in lowered or "dbtype == ''" in lowered or "dbtype==''" in lowered
            has_oracle = "oracle" in lowered
            has_sqlserver = "sqlserver" in lowered

            if not has_pg and has_not_oracle:
                line = _replace_if_test(line, _postgres_if_test(test))
                branch_count += 1
                changed = True
            elif not has_pg and has_oracle and not has_mysqlish:
                skip_depth = _line_if_depth(line)
                removed_count += 1
                changed = True
                if skip_depth <= 0:
                    skip_depth = 0
                continue
            elif not has_pg and has_sqlserver and not has_mysqlish:
                skip_depth = _line_if_depth(line)
                removed_count += 1
                changed = True
                if skip_depth <= 0:
                    skip_depth = 0
                continue
            elif not has_pg and has_mysqlish:
                line = _replace_if_test(line, _postgres_if_test(test))
                branch_count += 1
                changed = True

        output.append(_convert_postgres_xml_sql_line(line))

    converted = "".join(output)
    converted = re.sub(
        r"(?m)([ \t]*<if test=\"dbType == 'postgresql'\">[^\n]*</if>\n)(?:\1)+",
        r"\1",
        converted,
    )
    warnings: list[str] = []
    if branch_count:
        warnings.append(f"postgres-default-branches:{branch_count}")
    if removed_count:
        warnings.append(f"removed-non-postgres-branches:{removed_count}")
    if changed:
        _, risky = postgres_xml_from_mysqlish(converted)
        warnings.extend(risky)
    return converted, warnings


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


def _convert_oracle_type_tokens(text: str) -> str:
    text = re.sub(r"\bVARCHAR2\s*\(", "VARCHAR(", text, flags=re.IGNORECASE)
    text = re.sub(r"\bNVARCHAR2\s*\(", "VARCHAR(", text, flags=re.IGNORECASE)
    text = re.sub(r"\bCLOB\b", "TEXT", text, flags=re.IGNORECASE)
    text = re.sub(r"\bBLOB\b", "BYTEA", text, flags=re.IGNORECASE)
    text = re.sub(r"\bNUMBER\s*\(\s*1\s*,\s*0\s*\)", "SMALLINT", text, flags=re.IGNORECASE)
    text = re.sub(r"\bNUMBER\s*\(\s*(\d+)\s*,\s*0\s*\)", r"NUMERIC(\1,0)", text, flags=re.IGNORECASE)
    text = re.sub(r"\bNUMBER\s*\(([^)]+)\)", r"NUMERIC(\1)", text, flags=re.IGNORECASE)
    return text


def _clean_column_type(value: str) -> str:
    value = _convert_oracle_type_tokens(value)
    value = re.sub(r"\bCHARACTER\s+SET\s+[A-Za-z0-9_]+", "", value, flags=re.IGNORECASE)
    value = re.sub(r"\bCOLLATE\s+[A-Za-z0-9_]+", "", value, flags=re.IGNORECASE)
    value = re.sub(r"\s+", " ", value)
    return value.strip()


def _normalize_default_value(value: str) -> str:
    value = value.strip()
    value = re.sub(r"\bcurrent_timestamp\s*\(\s*\)", "CURRENT_TIMESTAMP", value, flags=re.IGNORECASE)
    value = re.sub(r"\bnow\s*\(\s*\)", "CURRENT_TIMESTAMP", value, flags=re.IGNORECASE)
    value = re.sub(r"'0000-00-00(?:\s+00:00:00(?:\.000000)?)?'", "CURRENT_TIMESTAMP", value, flags=re.IGNORECASE)
    return value


def _convert_modify_column_statements(text: str) -> str:
    def convert(match: re.Match[str]) -> str:
        table = match.group("table")
        column = match.group("column")
        rest = _clean_column_type(match.group("rest"))
        attr_match = re.search(r"\s+(DEFAULT\b|NOT\s+NULL\b|NULL\b)", rest, flags=re.IGNORECASE)
        if attr_match:
            column_type = rest[: attr_match.start()].strip()
            attrs = rest[attr_match.start() :].strip()
        else:
            column_type = rest
            attrs = ""

        statements = [f"ALTER TABLE {table} ALTER COLUMN {column} TYPE {column_type};"]
        default_match = re.search(
            r"\bDEFAULT\s+(.+?)(?=\s+NOT\s+NULL\b|\s+NULL\b|$)",
            attrs,
            flags=re.IGNORECASE,
        )
        if default_match:
            default_value = _normalize_default_value(default_match.group(1))
            if default_value.upper() == "NULL":
                statements.append(f"ALTER TABLE {table} ALTER COLUMN {column} DROP DEFAULT;")
            else:
                statements.append(f"ALTER TABLE {table} ALTER COLUMN {column} SET DEFAULT {default_value};")
        if re.search(r"\bNOT\s+NULL\b", attrs, flags=re.IGNORECASE):
            statements.append(f"ALTER TABLE {table} ALTER COLUMN {column} SET NOT NULL;")
        elif re.search(r"\bNULL\b", attrs, flags=re.IGNORECASE):
            statements.append(f"ALTER TABLE {table} ALTER COLUMN {column} DROP NOT NULL;")
        return "\n".join(statements)

    return re.sub(
        r"(?im)^\s*ALTER\s+TABLE\s+(?P<table>[A-Za-z0-9_]+)\s+MODIFY(?:\s+COLUMN)?\s+(?P<column>[A-Za-z0-9_]+)\s+(?P<rest>[^;]+);",
        convert,
        text,
    )


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
    converted = re.sub(r"\s+FROM\s+DUAL\b", "", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bUSING\s+BTREE\b", "", converted, flags=re.IGNORECASE)
    converted = _convert_create_table_indexes(converted)
    converted = _convert_oracle_type_tokens(converted)

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
    converted = re.sub(r"\bCHARACTER\s+SET\s+[A-Za-z0-9_]+", "", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bCOLLATE\s+[A-Za-z0-9_]+", "", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bENGINE\s*=\s*[A-Za-z0-9_]+", "", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bCOMMENT\s*=\s*'(?:\\'|''|[^'])*'", "", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bINSERT\s+IGNORE\b", "INSERT", converted, flags=re.IGNORECASE)
    converted = re.sub(
        r"\s+ON\s+DUPLICATE\s+KEY\s+UPDATE\s+[^;]+;",
        " ON CONFLICT DO NOTHING;",
        converted,
        flags=re.IGNORECASE | re.DOTALL,
    )
    converted = re.sub(
        r"(?im)^\s*ALTER\s+TABLE\s+auth_user\s+DROP\s+(?:COLUMN\s+)?(?:ldap_user_name|user_directory_id)\s*;\s*$",
        "-- skipped PostgreSQL compatibility: keep auth_user legacy column",
        converted,
    )
    converted = re.sub(r"\bIFNULL\s*\(", "COALESCE(", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bNOW\s*\(\s*\)", "CURRENT_TIMESTAMP", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bCURRENT_TIMESTAMP\s*\(\s*\)", "CURRENT_TIMESTAMP", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bSYSDATE\s*\(\s*\)", "CURRENT_TIMESTAMP", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bCURDATE\s*\(\s*\)", "CURRENT_DATE", converted, flags=re.IGNORECASE)
    converted = re.sub(r"\bGROUP_CONCAT\s*\(\s*([^)]+?)\s*\)", r"STRING_AGG(\1::text, ',')", converted, flags=re.IGNORECASE)
    converted = re.sub(
        r"\s+or\s+[A-Za-z0-9_]+(?:\.[A-Za-z0-9_]+)?\s*=\s*'0000-00-00(?:\s+00:00:00(?:\.000000)?)?'",
        "",
        converted,
        flags=re.IGNORECASE,
    )
    converted = re.sub(
        r"\bDEFAULT\s+'0000-00-00(?:\s+00:00:00(?:\.000000)?)?'",
        "DEFAULT CURRENT_TIMESTAMP",
        converted,
        flags=re.IGNORECASE,
    )
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
    converted = _convert_modify_column_statements(converted)
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
        "defaultMappersPatched": [],
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

    for name in list(entries):
        if not name.endswith(".xml"):
            continue
        if detect_db_segment(name) is not None:
            continue
        info, data = entries[name]
        try:
            text = data.decode("utf-8")
            converted, warnings = postgres_xml_from_dynamic_dbtype(text)
        except UnicodeDecodeError:
            continue
        if converted == text:
            continue
        entries[name] = (info, converted.encode("utf-8"))
        changed = True
        report["defaultMappersPatched"].append(name)
        for warning in warnings:
            report["mapperWarnings"].append({"file": name, "pattern": warning})

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
        "defaultMapperPatchCount": sum(
            len(nested.get("defaultMappersPatched", []))
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
