#!/usr/bin/env python3
"""Patch PATROL 6.0.4.0 generated SQL for PostgreSQL-compatible quoting."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path


UTILITY_RELATIVE_PATH = Path(
    "service/src/main/java/com/supcon/orchid/PATROL/util/PATROLSqlUtils.java"
)
SERVICE_IMPL_RELATIVE_PATH = Path(
    "service/src/main/java/com/supcon/orchid/PATROL/services/impl"
)
PATROL_PLAN_SERVICE_RELATIVE_PATH = (
    SERVICE_IMPL_RELATIVE_PATH / "PATROLPatrolPlanServiceImpl.java"
)
UTILITY_METHOD_MARKER = b"public static String normalizeIdentifierQuotes"
UTILITY_IMPORT = b"import com.supcon.orchid.db.DbUtils;"
UTILITY_IMPORT_ANCHOR = b"import com.supcon.orchid.foundation.entities.Company;"
UTILITY_CLASS_ANCHOR = b"public class PATROLSqlUtils extends BaseServiceImpl {"
QUALIFIED_HELPER = (
    b"com.supcon.orchid.PATROL.util.PATROLSqlUtils.normalizeIdentifierQuotes"
)
REPLACEMENTS = (
    (
        b"buffer.toString().replace('\"', '`')",
        QUALIFIED_HELPER + b"(buffer.toString())",
    ),
    (
        b"realSql.toString().replace('\"', '`')",
        QUALIFIED_HELPER + b"(realSql.toString())",
    ),
    (
        b"treesql.replace('\"', '`')",
        QUALIFIED_HELPER + b"(treesql)",
    ),
    # PostgreSQL folds unquoted result aliases to lower case, while the generated
    # association-field merger reads these keys as upper case.
    (b' AS OID";', b' AS \\"OID\\"";'),
    (b' AS ID1";', b' AS \\"ID1\\"";'),
    (b' AS ID2";', b' AS \\"ID2\\"";'),
    (b' AS VAL";', b' AS \\"VAL\\"";'),
    (b' AS REALVAL";', b' AS \\"REALVAL\\"";'),
    # Once aliases are quoted, PostgreSQL requires the ORDER BY reference to
    # use the same case-sensitive identifier.
    (
        b'? "ID1 ASC" : "OID ASC";',
        b'? "\\"ID1\\" ASC" : "\\"OID\\" ASC";',
    ),
    (
        b'? "ID2 ASC" : "ID1 ASC";',
        b'? "\\"ID2\\" ASC" : "\\"ID1\\" ASC";',
    ),
)

PLAN_ID_ASSIGNMENT = b"potrolTask.setPatrolPlanId(potrolPlan.getId());//"
PLAN_ASSOCIATION_ASSIGNMENT = b"potrolTask.setPatrolPlan(potrolPlan);"
TASK_DETAIL_COLUMNS = b"EAM_ID,TASK_AREA_ID,SORT) "
PATCHED_TASK_DETAIL_COLUMNS = (
    b"EAM_ID,TASK_AREA_ID,SORT,VALID,VERSION,TASK_DETAIL_STATE) "
)
TASK_DETAIL_VALUES_PREFIX = b'+ " values('
TASK_DETAIL_VALUES_SUFFIX = b')";'
TASK_DETAIL_DEFAULT_MARKER = b'ps.setString(30, "PATROL_taskDetailState/pending")'
TASK_DETAIL_BATCH_ANCHOR = b"                    ps.addBatch();"


def patch_utility(source: bytes) -> tuple[bytes, bool]:
    if UTILITY_CLASS_ANCHOR not in source:
        raise ValueError("PATROLSqlUtils class declaration was not found")

    changed = False
    newline = b"\r\n" if b"\r\n" in source else b"\n"
    if UTILITY_IMPORT not in source:
        if UTILITY_IMPORT_ANCHOR not in source:
            raise ValueError("PATROLSqlUtils import anchor was not found")
        source = source.replace(
            UTILITY_IMPORT_ANCHOR,
            UTILITY_IMPORT_ANCHOR + newline + UTILITY_IMPORT,
            1,
        )
        changed = True

    if UTILITY_METHOD_MARKER not in source:
        method = newline.join(
            (
                b"",
                b"    public static String normalizeIdentifierQuotes(String sql) {",
                b"        return normalizeIdentifierQuotes(sql, DbUtils.getDbName());",
                b"    }",
                b"",
                b"    static String normalizeIdentifierQuotes(String sql, String databaseName) {",
                b"        if (sql == null) {",
                b"            return null;",
                b"        }",
                b"        boolean mysql = databaseName != null",
                b"                && databaseName.toLowerCase(java.util.Locale.ROOT).contains(\"mysql\");",
                b"        return mysql ? sql.replace('\"', '`') : sql;",
                b"    }",
            )
        )
        source = source.replace(UTILITY_CLASS_ANCHOR, UTILITY_CLASS_ANCHOR + method, 1)
        changed = True

    return source, changed


def normalize_helper_line_indentation(source: bytes) -> tuple[bytes, int]:
    normalized_lines = []
    changed = 0
    for line in source.splitlines(keepends=True):
        if QUALIFIED_HELPER not in line:
            normalized_lines.append(line)
            continue
        content = line.lstrip(b" \t")
        prefix = line[: len(line) - len(content)]
        normalized_prefix = prefix
        while b" \t" in normalized_prefix:
            normalized_prefix = normalized_prefix.replace(b" \t", b"\t")
        if normalized_prefix != prefix:
            line = normalized_prefix + content
            changed += 1
        normalized_lines.append(line)
    return b"".join(normalized_lines), changed


def patch_service(source: bytes) -> tuple[bytes, int, int]:
    replacement_count = 0
    for original, replacement in REPLACEMENTS:
        count = source.count(original)
        if count:
            source = source.replace(original, replacement)
            replacement_count += count
    source, indentation_count = normalize_helper_line_indentation(source)
    return source, replacement_count, indentation_count


def patch_task_generation(source: bytes) -> tuple[bytes, int]:
    """Keep generated task relations and detail defaults persistence-complete."""

    changed = 0
    newline = b"\r\n" if b"\r\n" in source else b"\n"

    if PLAN_ASSOCIATION_ASSIGNMENT not in source:
        assignment_at = source.find(PLAN_ID_ASSIGNMENT)
        if assignment_at < 0:
            raise ValueError("PATROL task plan-id assignment was not found")
        line_start = source.rfind(newline, 0, assignment_at) + len(newline)
        line_end = source.find(newline, assignment_at)
        if line_end < 0:
            raise ValueError("PATROL task plan-id assignment line is incomplete")
        indentation = source[line_start:assignment_at]
        insertion = newline + indentation + PLAN_ASSOCIATION_ASSIGNMENT
        source = source[:line_end] + insertion + source[line_end:]
        changed += 1

    if PATCHED_TASK_DETAIL_COLUMNS not in source:
        if source.count(TASK_DETAIL_COLUMNS) != 1:
            raise ValueError("PATROL task-detail insert column list was not found uniquely")
        source = source.replace(
            TASK_DETAIL_COLUMNS, PATCHED_TASK_DETAIL_COLUMNS, 1
        )
        changed += 1

    if TASK_DETAIL_DEFAULT_MARKER not in source:
        column_at = source.find(PATCHED_TASK_DETAIL_COLUMNS)
        values_at = source.find(TASK_DETAIL_VALUES_PREFIX, column_at)
        values_end = source.find(TASK_DETAIL_VALUES_SUFFIX, values_at)
        if values_at < 0 or values_end < 0:
            raise ValueError("PATROL task-detail insert placeholders were not found")
        source = (
            source[:values_end]
            + b",?,?,?"
            + source[values_end:]
        )

        batch_at = source.find(TASK_DETAIL_BATCH_ANCHOR, values_end)
        if batch_at < 0:
            raise ValueError("PATROL task-detail batch anchor was not found")
        defaults = newline.join(
            (
                b"                    ps.setBoolean(28, true);// valid",
                b"                    ps.setInt(29, 0);// version",
                b'                    ps.setString(30, "PATROL_taskDetailState/pending");// pending',
                b"",
            )
        )
        source = source[:batch_at] + defaults + source[batch_at:]
        changed += 1

    return source, changed


def patch_module(module_root: Path, check: bool, source_commit: str) -> dict[str, object]:
    utility_path = module_root / UTILITY_RELATIVE_PATH
    service_root = module_root / SERVICE_IMPL_RELATIVE_PATH
    if not utility_path.is_file() or not service_root.is_dir():
        raise ValueError(f"not a PATROL 6.0.4.0 source module: {module_root}")

    utility_before = utility_path.read_bytes()
    utility_after, utility_changed = patch_utility(utility_before)
    patched_files = []
    replacement_count = 0
    indentation_count = 0
    task_generation_fix_count = 0
    for path in sorted(service_root.glob("PATROL*ServiceImpl.java")):
        before = path.read_bytes()
        after, current_count, current_indentation_count = patch_service(before)
        current_task_generation_count = 0
        if path == module_root / PATROL_PLAN_SERVICE_RELATIVE_PATH:
            after, current_task_generation_count = patch_task_generation(after)
        if current_count or current_indentation_count:
            patched_files.append(str(path.relative_to(module_root)))
            replacement_count += current_count
            indentation_count += current_indentation_count
        if current_task_generation_count:
            relative_path = str(path.relative_to(module_root))
            if relative_path not in patched_files:
                patched_files.append(relative_path)
            task_generation_fix_count += current_task_generation_count
        if not check and after != before:
            path.write_bytes(after)

    remaining = []
    for path in sorted(service_root.glob("PATROL*ServiceImpl.java")):
        source = path.read_bytes()
        if not check:
            source, _, _ = patch_service(source)
            if path == module_root / PATROL_PLAN_SERVICE_RELATIVE_PATH:
                source, _ = patch_task_generation(source)
        if b".replace('\"', '`')" in source:
            remaining.append(str(path.relative_to(module_root)))

    if remaining:
        raise ValueError(f"unhandled identifier quote conversions: {remaining}")
    if check and (
        utility_changed
        or replacement_count
        or indentation_count
        or task_generation_fix_count
    ):
        raise ValueError(
            f"source patch is required: utilityChanged={utility_changed}, "
            f"replacements={replacement_count}, indentation={indentation_count}, "
            f"taskGeneration={task_generation_fix_count}"
        )
    if not check and utility_changed:
        utility_path.write_bytes(utility_after)

    helper_references = sum(
        path.read_bytes().count(QUALIFIED_HELPER)
        for path in service_root.glob("PATROL*ServiceImpl.java")
    )
    return {
        "moduleRoot": str(module_root),
        "sourceCommit": source_commit,
        "mode": "check" if check else "apply",
        "utilityChanged": utility_changed,
        "replacementCount": replacement_count,
        "indentationFixCount": indentation_count,
        "taskGenerationFixCount": task_generation_fix_count,
        "patchedFileCount": len(patched_files),
        "patchedFiles": patched_files,
        "helperReferenceCount": helper_references,
        "status": "PASS",
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--module-root", required=True, type=Path)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--source-commit", default="unknown")
    parser.add_argument("--report", type=Path)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        report = patch_module(
            args.module_root.expanduser().resolve(), args.check, args.source_commit
        )
    except (OSError, ValueError) as error:
        print(f"PATROL source patch failed: {error}", file=sys.stderr)
        return 2

    payload = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(payload, encoding="utf-8")
    print(payload, end="")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
