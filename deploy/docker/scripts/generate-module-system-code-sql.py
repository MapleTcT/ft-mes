#!/usr/bin/env python3
"""Generate idempotent PostgreSQL system-code seed SQL from BAP XML."""

from __future__ import annotations

import argparse
import hashlib
from dataclasses import dataclass
from pathlib import Path
import xml.etree.ElementTree as ET


@dataclass(frozen=True)
class SystemCode:
    id: int
    code: str
    name: str
    display_name: str
    entity_code: str
    entity_name: str
    valid: int
    leaf: int
    default_flag: int
    full_path: str
    parent_id: int | None
    lay_no: int | None
    lay_rec: str
    seq_id: int | None
    sort: str | None
    des_a: str
    des_b: str
    des_c: str
    memo: str


@dataclass(frozen=True)
class SystemEntity:
    id: int
    code: str
    name: str
    display_name: str
    module_code: str
    list_type: str
    valid: int
    multi_flag: int
    sys_default: int
    memo: str
    codes: tuple[SystemCode, ...]


def bool_int(value: str, default: bool = False) -> int:
    normalized = (value or "").strip().lower()
    if not normalized:
        return int(default)
    return int(normalized in {"1", "true", "yes", "y"})


def optional_int(value: str) -> int | None:
    normalized = (value or "").strip()
    return int(normalized) if normalized else None


def stable_id(namespace: str, value: str) -> int:
    digest = hashlib.sha256(f"{namespace}:{value}".encode("utf-8")).digest()
    return 7_000_000_000_000_000 + int.from_bytes(digest[:6], "big")


def parse_system_codes(path: Path) -> tuple[SystemEntity, ...]:
    root = ET.parse(path).getroot()
    entities: list[SystemEntity] = []
    for entity in root.findall("systementity"):
        entity_code = entity.get("code", "").strip()
        entity_name = entity.get("name", "").strip()
        codes: list[SystemCode] = []
        for code in entity.findall("systemcode"):
            logical_id = code.get("id", "").strip()
            lay_rec = code.get("layRec", "").strip()
            code_id = optional_int(lay_rec) or stable_id("system-code", logical_id)
            codes.append(
                SystemCode(
                    id=code_id,
                    code=code.get("code", "").strip(),
                    name=code.get("value", "").strip(),
                    display_name=code.get("fullPathName", "").strip(),
                    entity_code=entity_code,
                    entity_name=entity_name,
                    valid=bool_int(code.get("valid", ""), True),
                    leaf=bool_int(code.get("leaf", ""), True),
                    default_flag=bool_int(code.get("defaultFlag", "")),
                    full_path=logical_id,
                    parent_id=optional_int(code.get("parentId", "")),
                    lay_no=optional_int(code.get("layNo", "")),
                    lay_rec=lay_rec,
                    seq_id=optional_int(code.get("seqId", "")),
                    sort=(code.get("sort", "").strip() or None),
                    des_a=code.get("codeDesA", "").strip(),
                    des_b=code.get("codeDesB", "").strip(),
                    des_c=code.get("codeDesC", "").strip(),
                    memo=code.get("memo", "").strip(),
                )
            )
        entities.append(
            SystemEntity(
                id=stable_id("system-entity", entity_code),
                code=entity_code,
                name=entity_name,
                display_name=entity_name,
                module_code=entity.get("moduleCode", "").strip(),
                list_type=entity.get("listType", "list").strip() or "list",
                valid=bool_int(entity.get("valid", ""), True),
                multi_flag=bool_int(entity.get("multiFlag", "")),
                sys_default=bool_int(entity.get("sysDefault", "")),
                memo=entity.get("memo", "").strip(),
                codes=tuple(codes),
            )
        )
    return tuple(entities)


def sql_text(value: str | None) -> str:
    if value is None:
        return "NULL"
    return "'" + value.replace("'", "''") + "'"


def sql_number(value: int | str | None, pg_type: str = "") -> str:
    if value is None:
        return f"NULL::{pg_type}" if pg_type else "NULL"
    return str(value)


def render_sql(entities: tuple[SystemEntity, ...], source_path: Path) -> str:
    entity_values = []
    code_values = []
    for entity in entities:
        entity_values.append(
            "(" + ", ".join(
                [
                    str(entity.id),
                    sql_text(entity.list_type),
                    sql_text(entity.code),
                    sql_text(entity.name),
                    sql_text(entity.display_name),
                    sql_text(entity.module_code),
                    "1000",
                    str(entity.valid),
                    str(entity.multi_flag),
                    str(entity.sys_default),
                    sql_text(entity.memo),
                ]
            ) + ")"
        )
        for code in entity.codes:
            code_values.append(
                "(" + ", ".join(
                    [
                        str(code.id),
                        sql_text(entity.list_type),
                        sql_text(code.code),
                        sql_text(code.entity_code),
                        sql_text(code.name),
                        sql_text(code.display_name),
                        "1000",
                        str(code.valid),
                        str(code.leaf),
                        str(code.default_flag),
                        sql_text(code.full_path),
                        sql_text(code.display_name),
                        sql_number(code.parent_id, "bigint"),
                        sql_text(code.entity_name),
                        sql_number(code.lay_no),
                        sql_text(code.lay_rec),
                        sql_number(code.seq_id, "bigint"),
                        sql_number(code.sort),
                        sql_text(code.des_a),
                        sql_text(code.des_b),
                        sql_text(code.des_c),
                        sql_text(code.memo),
                    ]
                ) + ")"
            )

    source_sha = hashlib.sha256(source_path.read_bytes()).hexdigest()
    lines = [
        f"-- Source: {source_path.name}",
        f"-- Source SHA-256: {source_sha}",
        "-- Generated by deploy/docker/scripts/generate-module-system-code-sql.py",
        "",
        "BEGIN;",
        "",
        "WITH source(id, type, code, name, display_name, module_id, cid, valid, multi_flag, sys_default, memo) AS (",
        "    VALUES",
        "        " + ",\n        ".join(entity_values),
        "), updated AS (",
        "    UPDATE public.sys_entity target",
        "       SET row_version = 0, type = source.type, name = source.name,",
        "           display_name = source.display_name, module_id = source.module_id,",
        "           cid = source.cid, valid = source.valid, multi_flag = source.multi_flag,",
        "           sys_default = source.sys_default, memo = source.memo, modify_time = CURRENT_TIMESTAMP",
        "      FROM source",
        "     WHERE target.code = source.code",
        " RETURNING target.code",
        ")",
        "INSERT INTO public.sys_entity",
        "    (id, row_version, type, code, name, display_name, module_id, cid, valid, multi_flag, sys_default, memo, source)",
        "SELECT source.id, 0, source.type, source.code, source.name, source.display_name,",
        "       source.module_id, source.cid, source.valid, source.multi_flag, source.sys_default, source.memo, 'module'",
        "  FROM source",
        " WHERE NOT EXISTS (SELECT 1 FROM public.sys_entity current_row WHERE current_row.code = source.code)",
        "ON CONFLICT (id) DO UPDATE SET",
        "    type = EXCLUDED.type, code = EXCLUDED.code, name = EXCLUDED.name,",
        "    display_name = EXCLUDED.display_name, module_id = EXCLUDED.module_id,",
        "    cid = EXCLUDED.cid, valid = EXCLUDED.valid, multi_flag = EXCLUDED.multi_flag,",
        "    sys_default = EXCLUDED.sys_default, memo = EXCLUDED.memo, modify_time = CURRENT_TIMESTAMP;",
        "",
        "WITH source(id, type, code, entity_code, name, display_name, cid, valid, leaf, default_flag,",
        "            full_path, full_path_name, parent_id, parent_name, lay_no, lay_rec, seq_id, sort,",
        "            des_a, des_b, des_c, memo) AS (",
        "    VALUES",
        "        " + ",\n        ".join(code_values),
        "), updated AS (",
        "    UPDATE public.sys_code target",
        "       SET row_version = 0, type = source.type, name = source.name, display_name = source.display_name,",
        "           cid = source.cid, valid = source.valid, leaf = source.leaf, default_flag = source.default_flag,",
        "           full_path = source.full_path, full_path_name = source.full_path_name,",
        "           parent_id = source.parent_id, parent_name = source.parent_name, lay_no = source.lay_no,",
        "           lay_rec = source.lay_rec, seq_id = source.seq_id, sort = source.sort,",
        "           des_a = source.des_a, des_b = source.des_b, des_c = source.des_c,",
        "           memo = source.memo, modify_time = CURRENT_TIMESTAMP",
        "      FROM source",
        "     WHERE target.entity_code = source.entity_code AND target.code = source.code",
        " RETURNING target.entity_code, target.code",
        ")",
        "INSERT INTO public.sys_code",
        "    (id, row_version, type, code, entity_code, name, display_name, cid, valid, leaf, default_flag,",
        "     full_path, full_path_name, parent_id, parent_name, lay_no, lay_rec, seq_id, sort, des_a, des_b, des_c, memo)",
        "SELECT source.id, 0, source.type, source.code, source.entity_code, source.name, source.display_name,",
        "       source.cid, source.valid, source.leaf, source.default_flag, source.full_path, source.full_path_name,",
        "       source.parent_id, source.parent_name, source.lay_no, source.lay_rec, source.seq_id, source.sort,",
        "       source.des_a, source.des_b, source.des_c, source.memo",
        "  FROM source",
        " WHERE NOT EXISTS (",
        "       SELECT 1 FROM public.sys_code current_row",
        "        WHERE current_row.entity_code = source.entity_code AND current_row.code = source.code",
        " )",
        "ON CONFLICT (id) DO UPDATE SET",
        "    type = EXCLUDED.type, code = EXCLUDED.code, entity_code = EXCLUDED.entity_code,",
        "    name = EXCLUDED.name, display_name = EXCLUDED.display_name, cid = EXCLUDED.cid,",
        "    valid = EXCLUDED.valid, leaf = EXCLUDED.leaf, default_flag = EXCLUDED.default_flag,",
        "    full_path = EXCLUDED.full_path, full_path_name = EXCLUDED.full_path_name,",
        "    parent_id = EXCLUDED.parent_id, parent_name = EXCLUDED.parent_name, lay_no = EXCLUDED.lay_no,",
        "    lay_rec = EXCLUDED.lay_rec, seq_id = EXCLUDED.seq_id, sort = EXCLUDED.sort,",
        "    des_a = EXCLUDED.des_a, des_b = EXCLUDED.des_b, des_c = EXCLUDED.des_c,",
        "    memo = EXCLUDED.memo, modify_time = CURRENT_TIMESTAMP;",
        "",
        "COMMIT;",
        "",
    ]
    return "\n".join(lines)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, type=Path)
    args = parser.parse_args()
    print(render_sql(parse_system_codes(args.input), args.input), end="")


if __name__ == "__main__":
    main()
