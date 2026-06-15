#!/usr/bin/env python3
"""Patch WTS runtime compatibility gaps in recovered Linux deployments.

This handles two package-migration issues seen in the recovered WTS module:

* desktop low-code HTML pages may miss vendors.sesgis.js, which prevents the
  Webpack list/edit entry from executing because chunk 7 never loads.
* selected WTS work-ticket LIST views may have runtime_extra_view.view_json as
  NULL after PostgreSQL import. The script rebuilds small LIST layouts from
  ec_view/ec_field metadata and writes both runtime_extra_view and ec_extra_view.
"""

from __future__ import annotations

import argparse
import base64
import io
import json
import os
import re
import shutil
import subprocess
import sys
import time
import zipfile
from pathlib import Path
from typing import Any, Dict, Iterable, List, Tuple
import xml.etree.ElementTree as ET


INNER_WTS_SERVICE_JAR = "BOOT-INF/lib/com.supcon.greendill.WTS.service-6.1.8.2.jar"
STAT_ICON_SOURCE = "custom/WTS/_static_jobStatistics/assets/images/comp_icon.png"
STAT_ICON_ALIAS = "static/WTS/assets/images/comp_icon.png"
SCRIPT_MARKER = "greenDill/static/scripts/vendors.sesgis.js"
ECHARTS_RE = re.compile(
    r'(<script[^>]+src="/greenDill/static/scripts/vendors\.echarts\.js\?v=([^"]+)"[^>]*></script>)'
)


def copy_zip_info(info: zipfile.ZipInfo) -> zipfile.ZipInfo:
    copied = zipfile.ZipInfo(info.filename, info.date_time)
    copied.comment = info.comment
    copied.extra = info.extra
    copied.internal_attr = info.internal_attr
    copied.external_attr = info.external_attr
    copied.create_system = info.create_system
    copied.compress_type = info.compress_type
    return copied


def patch_inner_wts_jar(data: bytes) -> Tuple[bytes, Dict[str, Any]]:
    patched_files: List[str] = []
    added_alias = False
    output = io.BytesIO()
    with zipfile.ZipFile(io.BytesIO(data), "r") as source:
        names = set(source.namelist())
        with zipfile.ZipFile(output, "w") as target:
            for info in source.infolist():
                content = source.read(info.filename)
                if info.filename.endswith(".html"):
                    text = content.decode("utf-8", "ignore")
                    is_lowcode_desktop = (
                        "greenDill/static/scripts/vendors.echarts.js" in text
                        and SCRIPT_MARKER not in text
                        and any(
                            script in text
                            for script in (
                                "greenDill/static/scripts/list.js",
                                "greenDill/static/scripts/edit.js",
                                "greenDill/static/scripts/treeList.js",
                            )
                        )
                    )
                    if is_lowcode_desktop:
                        match = ECHARTS_RE.search(text)
                        if not match:
                            raise RuntimeError(f"cannot find vendors.echarts script in {info.filename}")
                        version = match.group(2)
                        insert = (
                            match.group(1)
                            + "\n"
                            + f'<script type="text/javascript" src="/greenDill/static/scripts/vendors.sesgis.js?v={version}"></script>'
                        )
                        text = ECHARTS_RE.sub(insert, text, count=1)
                        content = text.encode("utf-8")
                        patched_files.append(info.filename)
                target.writestr(copy_zip_info(info), content)

            if STAT_ICON_ALIAS not in names and STAT_ICON_SOURCE in names:
                alias_info = zipfile.ZipInfo(STAT_ICON_ALIAS, time.localtime()[:6])
                alias_info.compress_type = zipfile.ZIP_DEFLATED
                target.writestr(alias_info, source.read(STAT_ICON_SOURCE))
                added_alias = True

    return output.getvalue(), {"html_patched": patched_files, "added_static_alias": added_alias}


def patch_wts_jar(jar_path: Path, backup_suffix: str) -> Dict[str, Any]:
    original = jar_path.read_bytes()
    changed = False
    output = io.BytesIO()
    stats: Dict[str, Any] = {}
    with zipfile.ZipFile(io.BytesIO(original), "r") as source:
        if INNER_WTS_SERVICE_JAR not in source.namelist():
            raise RuntimeError(f"missing nested WTS service jar: {INNER_WTS_SERVICE_JAR}")
        with zipfile.ZipFile(output, "w") as target:
            for info in source.infolist():
                content = source.read(info.filename)
                if info.filename == INNER_WTS_SERVICE_JAR:
                    content, stats = patch_inner_wts_jar(content)
                    changed = bool(stats["html_patched"] or stats["added_static_alias"])
                target.writestr(copy_zip_info(info), content)

    if changed:
        backup = jar_path.with_name(jar_path.name + backup_suffix)
        if not backup.exists():
            shutil.copy2(jar_path, backup)
        temp_path = jar_path.with_name(jar_path.name + ".tmp-wts-runtime-compat")
        temp_path.write_bytes(output.getvalue())
        shutil.copystat(jar_path, temp_path)
        temp_path.replace(jar_path)
        stats["backup"] = str(backup)
    stats["changed"] = changed
    return stats


def docker_psql_text(container: str, sql: str, check: bool = True) -> str:
    proc = subprocess.run(
        ["docker", "exec", "-i", container, "psql", "-U", "adp", "-d", "adp", "-At", "-c", sql],
        text=True,
        capture_output=True,
        check=False,
    )
    if check and proc.returncode != 0:
        raise RuntimeError(proc.stderr.strip() or proc.stdout.strip())
    return proc.stdout


def docker_psql_exec(container: str, sql: str) -> None:
    proc = subprocess.run(
        ["docker", "exec", "-i", container, "psql", "-U", "adp", "-d", "adp", "-v", "ON_ERROR_STOP=1"],
        input=sql,
        text=True,
        capture_output=True,
        check=False,
    )
    if proc.returncode != 0:
        raise RuntimeError(proc.stderr.strip() or proc.stdout.strip())


def query_json(container: str, sql: str) -> List[Dict[str, Any]]:
    text = docker_psql_text(container, sql).strip()
    return json.loads(text or "[]")


def xml_text(root: ET.Element, tag: str) -> str | None:
    found = root.find(f".//{tag}")
    return found.text if found is not None else None


def parse_field_config(config: str | None) -> Dict[str, Any]:
    result: Dict[str, Any] = {}
    if not config:
        return result
    try:
        root = ET.fromstring(config)
    except ET.ParseError:
        return result

    width = xml_text(root, "width")
    if width and width.isdigit():
        result["width"] = int(width)

    is_hidden = xml_text(root, "isHidden")
    if is_hidden is not None:
        result["isHidden"] = is_hidden.lower() == "true"

    fill = root.find(".//fill")
    if fill is not None:
        fill_content = xml_text(fill, "fillContent") or ""
        if fill_content:
            result["fill"] = {
                "fillName": xml_text(fill, "fillName") or "system code",
                "fillType": xml_text(fill, "fillType") or "3",
                "fillContent": fill_content,
            }
    return result


FIELD_PRIORITY = {
    "workTicketNo": 0,
    "ticketNo": 1,
    "workType": 2,
    "content": 3,
    "applyDept.name": 4,
    "applyStaff.name": 5,
    "applyTime": 6,
    "startTime": 7,
    "endTime": 8,
    "contractor.name": 9,
    "workDept.name": 10,
}


def field_sort_key(field: Dict[str, Any]) -> Tuple[int, int, str]:
    key = field.get("field_key") or ""
    return (1 if field.get("is_hidden") else 0, FIELD_PRIORITY.get(key, 100), field.get("code") or "")


def model_code(entity_code: str) -> str:
    if entity_code.endswith("_workTicket"):
        return entity_code + "_WorkTicket"
    return entity_code


def build_layout(view: Dict[str, Any], fields: Iterable[Dict[str, Any]]) -> Dict[str, Any]:
    view_fields = []
    for field in sorted(fields, key=field_sort_key):
        key = field.get("field_key") or field.get("name") or field.get("display_name") or "unknown"
        config = parse_field_config(field.get("config"))
        item: Dict[str, Any] = {
            "key": key,
            "namekey": field.get("display_name") or field.get("name") or key,
            "showType": field.get("show_type") or "TEXTFIELD",
            "showFormat": field.get("show_format") or "TEXT",
            "width": config.get("width", 150 if key.endswith("Time") else 120),
            "isHidden": config.get("isHidden", bool(field.get("is_hidden"))),
            "columnType": field.get("column_type") or "TEXT",
        }
        if "fill" in config:
            item["fill"] = config["fill"]
        view_fields.append(item)

    main_display = "workTicketNo"
    if not any(field["key"] == main_display for field in view_fields) and view_fields:
        main_display = view_fields[0]["key"]
    base_url = re.sub(r"/[^/]+$", "", view.get("url") or "")

    return {
        "pageType": "LIST",
        "title": view.get("title") or view["code"],
        "url": view.get("url") or "",
        "isMain": True,
        "hasAttachment": bool(view.get("has_attachment")),
        "onlyForQuery": bool(view.get("only_for_query")),
        "components": [
            {
                "type": "layout",
                "layoutmethod": "column",
                "components": [
                    {
                        "type": "layoutSearchWidget",
                        "code": "query",
                        "layoutName": "layoutSearchWidget",
                        "layoutmethod": "container",
                        "fix_h": 150,
                        "fastProperty": [],
                        "advProperty": [],
                    },
                    {
                        "type": "layoutDatagrid",
                        "layoutmethod": "container",
                        "ratio_h": 100,
                        "modelCode": model_code(view["entity_code"]),
                        "hasFastQuery": True,
                        "mainDisplayName": main_display,
                        "idPrefix": "compat_" + view["code"],
                        "buttons": [],
                        "fields": view_fields,
                        "downloadXls": base_url + "/downloadXls" if base_url else "",
                        "importMainXls": base_url + "/importMainXls" if base_url else "",
                    },
                ],
            }
        ],
        "isFileView": True,
        "moveFlag": bool(view.get("move_flag")),
    }


def patch_wts_layouts(container: str) -> Dict[str, Any]:
    view_sql = """
select coalesce(json_agg(row_to_json(t)), '[]'::json)::text
from (
  select v.code, v.title, v.url, v.entity_code, v.has_attachment, v.only_for_query, v.move_flag
  from ec_view v
  join runtime_extra_view r on r.view_code = v.code
  where v.code like 'WTS_1.0.0_workTicket_%'
    and v.code not like '%__mobile__'
    and v.type = 'LIST'
    and r.view_json is null
    and exists (
      select 1 from ec_field f where f.view_code = v.code and f.code like '%_LISTPT_%'
    )
  order by v.code
) t;
"""
    field_sql = """
select coalesce(json_agg(row_to_json(t)), '[]'::json)::text
from (
  select f.view_code, f.code, f.field_key, f.display_name, f.name, f.show_type,
         f.show_format, f.column_type, f.is_hidden, f.config
  from ec_field f
  join ec_view v on v.code = f.view_code
  join runtime_extra_view r on r.view_code = v.code
  where v.code like 'WTS_1.0.0_workTicket_%'
    and v.code not like '%__mobile__'
    and v.type = 'LIST'
    and r.view_json is null
    and f.code like '%_LISTPT_%'
  order by f.view_code, f.code
) t;
"""
    views = query_json(container, view_sql)
    fields = query_json(container, field_sql)
    fields_by_view: Dict[str, List[Dict[str, Any]]] = {}
    for field in fields:
        fields_by_view.setdefault(field["view_code"], []).append(field)

    if not views:
        return {"updated": 0, "views": []}

    timestamp = time.strftime("%Y%m%d_%H%M%S")
    runtime_backup = f"runtime_extra_view_wts_layout_backup_{timestamp}"
    ec_backup = f"ec_extra_view_wts_layout_backup_{timestamp}"
    statements = [
        "BEGIN;",
        f"create table {runtime_backup} as select * from runtime_extra_view where view_code like 'WTS_1.0.0_workTicket_%';",
        f"create table {ec_backup} as select * from ec_extra_view where view_code like 'WTS_1.0.0_workTicket_%';",
    ]
    updated_views = []
    for view in views:
        layout = build_layout(view, fields_by_view.get(view["code"], []))
        raw = json.dumps(layout, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
        encoded = base64.b64encode(raw).decode("ascii")
        code = view["code"].replace("'", "''")
        statements.append(
            "update runtime_extra_view "
            f"set view_json = lo_from_bytea(0, decode('{encoded}', 'base64')) "
            f"where view_code = '{code}' and view_json is null;"
        )
        statements.append(
            "update ec_extra_view "
            f"set view_json = convert_from(decode('{encoded}', 'base64'), 'UTF8') "
            f"where view_code = '{code}' and view_json is null;"
        )
        updated_views.append({"code": view["code"], "fields": len(layout["components"][0]["components"][1]["fields"])})
    statements.append("COMMIT;")
    docker_psql_exec(container, "\n".join(statements))
    return {"updated": len(updated_views), "runtime_backup": runtime_backup, "ec_backup": ec_backup, "views": updated_views}


def restart_container(name: str) -> None:
    subprocess.run(["docker", "restart", name], check=True)


def default_root() -> Path:
    return Path(__file__).resolve().parents[3]


def parse_args() -> argparse.Namespace:
    root = Path(os.environ.get("ADP_DEPLOY_ROOT", default_root())).expanduser()
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=root)
    parser.add_argument(
        "--wts-jar",
        type=Path,
        default=root / "runtime/bap-server/module-Server/WTSs/manual/WTSs-1.0.0.jar",
    )
    parser.add_argument("--postgres-container", default=os.environ.get("POSTGRES_CONTAINER", "adp-mes-newbase-postgres-1"))
    parser.add_argument("--wts-container", default=os.environ.get("WTS_CONTAINER", "adp-mes-newbase-WTSs-1"))
    parser.add_argument(
        "--baseservice-container",
        default=os.environ.get("BASESERVICE_CONTAINER", "adp-mes-newbase-baseService-1"),
    )
    parser.add_argument("--no-jar", action="store_true", help="skip WTS jar patch")
    parser.add_argument("--no-db", action="store_true", help="skip runtime_extra_view layout patch")
    parser.add_argument("--restart", action="store_true", help="restart WTS and baseService containers after changes")
    parser.add_argument("--backup-suffix", default=f".bak-wts-runtime-compat-{int(time.time())}")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    result: Dict[str, Any] = {}
    try:
        if not args.no_jar:
            result["jar"] = patch_wts_jar(args.wts_jar.expanduser().resolve(), args.backup_suffix)
        if not args.no_db:
            result["db"] = patch_wts_layouts(args.postgres_container)
        if args.restart:
            if result.get("jar", {}).get("changed"):
                restart_container(args.wts_container)
            if result.get("db", {}).get("updated"):
                restart_container(args.baseservice_container)
            result["restarted"] = {
                "wts": bool(result.get("jar", {}).get("changed")),
                "baseService": bool(result.get("db", {}).get("updated")),
            }
    except Exception as error:
        print(f"ERROR: {error}", file=sys.stderr)
        return 1

    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
