#!/usr/bin/env python3
from __future__ import annotations

import argparse
import os
import re
import shutil
import tempfile
import zipfile
from pathlib import Path


DOCKER_DIR = Path(__file__).resolve().parents[1]
PROJECT_ROOT = DOCKER_DIR.parents[1]
RBAC_DAO_JAR = "BOOT-INF/lib/rbac-dao-1.0.1-SNAPSHOT.jar"
MAPPER_XML = "mapper/UserPermissionMapper.xml"
SMALLINT_BOOLEAN_CASE = re.compile(
    r"CASE WHEN (?P<expr>[A-Za-z_][A-Za-z0-9_]*(?:\.[A-Za-z_][A-Za-z0-9_]*)?) "
    r"IS NULL THEN NULL WHEN (?P=expr) THEN 1 ELSE 0 END"
)


REPLACEMENTS = {
    """        FP.ID AS ID, MO.ID AS MENUOPERATE_ID,MO.CODE AS MENUOPERATE_CODE, FP.TYPE_ID AS USER_ID, FP.GROUP_POWER_FLAG AS GROUP_FLAG,
        FP.POSITION_POWER_FLAG AS POSITION_FLAG, ASSIGN_POS_FLAG AS ASSIGN_POS_FLAG,0 AS DEPARTMENT_FLAG, 0 AS ASSIGN_DEPT_FLAG,
        ASSIGN_STAFF_FLAG AS ASSIGN_STAFF_FLAG, FP.UNLIMITED_POWER AS NO_RESTRICT_FLAG,0 as DEALER_PERMISSION_FLAG,0 AS ASSIGN_CUSTOMPERMISSION_FLAG,0 AS ASSIGN_DATAPERMISSION_FLAG,0 as TYPEFLAG ,
""": """        FP.ID AS ID, MO.ID AS MENUOPERATE_ID,MO.CODE AS MENUOPERATE_CODE, FP.TYPE_ID AS USER_ID,
        CASE WHEN FP.GROUP_POWER_FLAG IS NULL THEN NULL WHEN FP.GROUP_POWER_FLAG = 1 THEN 1 ELSE 0 END AS GROUP_FLAG,
        CASE WHEN FP.POSITION_POWER_FLAG IS NULL THEN NULL WHEN FP.POSITION_POWER_FLAG = 1 THEN 1 ELSE 0 END AS POSITION_FLAG,
        CASE WHEN FP.ASSIGN_POS_FLAG IS NULL THEN NULL WHEN FP.ASSIGN_POS_FLAG = 1 THEN 1 ELSE 0 END AS ASSIGN_POS_FLAG,0 AS DEPARTMENT_FLAG, 0 AS ASSIGN_DEPT_FLAG,
        CASE WHEN FP.ASSIGN_STAFF_FLAG IS NULL THEN NULL WHEN FP.ASSIGN_STAFF_FLAG = 1 THEN 1 ELSE 0 END AS ASSIGN_STAFF_FLAG,
        CASE WHEN FP.UNLIMITED_POWER IS NULL THEN NULL WHEN FP.UNLIMITED_POWER = 1 THEN 1 ELSE 0 END AS NO_RESTRICT_FLAG,0 as DEALER_PERMISSION_FLAG,0 AS ASSIGN_CUSTOMPERMISSION_FLAG,0 AS ASSIGN_DATAPERMISSION_FLAG,0 as TYPEFLAG ,
""",
    """        FP.ID AS ID, MO.ID AS MENUOPERATE_ID,MO.CODE AS MENUOPERATE_CODE, FP.TYPE_ID AS USER_ID, FP.GROUP_POWER_FLAG AS GROUP_FLAG,
        FP.POSITION_POWER_FLAG AS POSITION_FLAG, ASSIGN_POS_FLAG AS ASSIGN_POS_FLAG,0 AS DEPARTMENT_FLAG, 0 AS ASSIGN_DEPT_FLAG,
        ASSIGN_STAFF_FLAG AS ASSIGN_STAFF_FLAG, FP.UNLIMITED_POWER AS NO_RESTRICT_FLAG,0 as DEALER_PERMISSION_FLAG,0 AS ASSIGN_CUSTOMPERMISSION_FLAG,0 AS ASSIGN_DATAPERMISSION_FLAG,0 as TYPEFLAG , 1 as PURVIEWTYPE,MO.FLOW_KEY as FLOWKEY
""": """        FP.ID AS ID, MO.ID AS MENUOPERATE_ID,MO.CODE AS MENUOPERATE_CODE, FP.TYPE_ID AS USER_ID,
        CASE WHEN FP.GROUP_POWER_FLAG IS NULL THEN NULL WHEN FP.GROUP_POWER_FLAG = 1 THEN 1 ELSE 0 END AS GROUP_FLAG,
        CASE WHEN FP.POSITION_POWER_FLAG IS NULL THEN NULL WHEN FP.POSITION_POWER_FLAG = 1 THEN 1 ELSE 0 END AS POSITION_FLAG,
        CASE WHEN FP.ASSIGN_POS_FLAG IS NULL THEN NULL WHEN FP.ASSIGN_POS_FLAG = 1 THEN 1 ELSE 0 END AS ASSIGN_POS_FLAG,0 AS DEPARTMENT_FLAG, 0 AS ASSIGN_DEPT_FLAG,
        CASE WHEN FP.ASSIGN_STAFF_FLAG IS NULL THEN NULL WHEN FP.ASSIGN_STAFF_FLAG = 1 THEN 1 ELSE 0 END AS ASSIGN_STAFF_FLAG,
        CASE WHEN FP.UNLIMITED_POWER IS NULL THEN NULL WHEN FP.UNLIMITED_POWER = 1 THEN 1 ELSE 0 END AS NO_RESTRICT_FLAG,0 as DEALER_PERMISSION_FLAG,0 AS ASSIGN_CUSTOMPERMISSION_FLAG,0 AS ASSIGN_DATAPERMISSION_FLAG,0 as TYPEFLAG , 1 as PURVIEWTYPE,MO.FLOW_KEY as FLOWKEY
""",
    """        UP.ID, UP.MENUOPERATE_ID,UP.MENUOPERATE_CODE, UP.USER_ID, UP.GROUP_FLAG, UP.POSITION_FLAG, UP.ASSIGN_POS_FLAG, UP.DEPARTMENT_FLAG,UP.ASSIGN_DEPT_FLAG,
        UP.ASSIGN_STAFF_FLAG, UP.NO_RESTRICT_FLAG,UP.DEALER_PERMISSION_FLAG,UP.ASSIGN_CUSTOMPERMISSION_FLAG,UP.ASSIGN_DATAPERMISSION_FLAG,1 as TYPEFLAG, UP.PURVIEW_TYPE as PURVIEWTYPE ,MO.FLOW_KEY as FLOWKEY
""": """        UP.ID, UP.MENUOPERATE_ID,UP.MENUOPERATE_CODE, UP.USER_ID, UP.GROUP_FLAG,
        CASE WHEN UP.POSITION_FLAG IS NULL THEN NULL WHEN UP.POSITION_FLAG = 1 THEN 1 ELSE 0 END AS POSITION_FLAG,
        CASE WHEN UP.ASSIGN_POS_FLAG IS NULL THEN NULL WHEN UP.ASSIGN_POS_FLAG = 1 THEN 1 ELSE 0 END AS ASSIGN_POS_FLAG,
        CASE WHEN UP.DEPARTMENT_FLAG IS NULL THEN NULL WHEN UP.DEPARTMENT_FLAG = 1 THEN 1 ELSE 0 END AS DEPARTMENT_FLAG,
        CASE WHEN UP.ASSIGN_DEPT_FLAG IS NULL THEN NULL WHEN UP.ASSIGN_DEPT_FLAG = 1 THEN 1 ELSE 0 END AS ASSIGN_DEPT_FLAG,
        CASE WHEN UP.ASSIGN_STAFF_FLAG IS NULL THEN NULL WHEN UP.ASSIGN_STAFF_FLAG = 1 THEN 1 ELSE 0 END AS ASSIGN_STAFF_FLAG,
        CASE WHEN UP.NO_RESTRICT_FLAG IS NULL THEN NULL WHEN UP.NO_RESTRICT_FLAG = 1 THEN 1 ELSE 0 END AS NO_RESTRICT_FLAG,
        CASE WHEN UP.DEALER_PERMISSION_FLAG IS NULL THEN NULL WHEN UP.DEALER_PERMISSION_FLAG = 1 THEN 1 ELSE 0 END AS DEALER_PERMISSION_FLAG,
        CASE WHEN UP.ASSIGN_CUSTOMPERMISSION_FLAG IS NULL THEN NULL WHEN UP.ASSIGN_CUSTOMPERMISSION_FLAG = 1 THEN 1 ELSE 0 END AS ASSIGN_CUSTOMPERMISSION_FLAG,
        CASE WHEN UP.ASSIGN_DATAPERMISSION_FLAG IS NULL THEN NULL WHEN UP.ASSIGN_DATAPERMISSION_FLAG = 1 THEN 1 ELSE 0 END AS ASSIGN_DATAPERMISSION_FLAG,
        1 as TYPEFLAG, UP.PURVIEW_TYPE as PURVIEWTYPE ,MO.FLOW_KEY as FLOWKEY
""",
    """        UP.ID, UP.USER_ID USER_ID, MI.NAME MENU_NAME, MO.NAME OPERATE_NAME, MO.FLOW_KEY FLOW_KEY, UP.GROUP_FLAG GROUP_FLAG, UP.POSITION_FLAG POSITION_FLAG,
        UP.ASSIGN_POS_FLAG ASSIGN_POS_FLAG, UP.ASSIGN_STAFF_FLAG ASSIGN_STAFF_FLAG, UP.DEALER_PERMISSION_FLAG DEALER_PERMISSION_FLAG, UP.NO_RESTRICT_FLAG NO_RESTRICT_FLAG, 1 as TYPEFLAG,
""": """        UP.ID, UP.USER_ID USER_ID, MI.NAME MENU_NAME, MO.NAME OPERATE_NAME, MO.FLOW_KEY FLOW_KEY, UP.GROUP_FLAG GROUP_FLAG,
        CASE WHEN UP.POSITION_FLAG IS NULL THEN NULL WHEN UP.POSITION_FLAG = 1 THEN 1 ELSE 0 END POSITION_FLAG,
        CASE WHEN UP.ASSIGN_POS_FLAG IS NULL THEN NULL WHEN UP.ASSIGN_POS_FLAG = 1 THEN 1 ELSE 0 END ASSIGN_POS_FLAG,
        CASE WHEN UP.ASSIGN_STAFF_FLAG IS NULL THEN NULL WHEN UP.ASSIGN_STAFF_FLAG = 1 THEN 1 ELSE 0 END ASSIGN_STAFF_FLAG,
        CASE WHEN UP.DEALER_PERMISSION_FLAG IS NULL THEN NULL WHEN UP.DEALER_PERMISSION_FLAG = 1 THEN 1 ELSE 0 END DEALER_PERMISSION_FLAG,
        CASE WHEN UP.NO_RESTRICT_FLAG IS NULL THEN NULL WHEN UP.NO_RESTRICT_FLAG = 1 THEN 1 ELSE 0 END NO_RESTRICT_FLAG, 1 as TYPEFLAG,
""",
    """        FP.ID, FP.TYPE_ID USER_ID, MI.NAME MENU_NAME, MO.NAME OPERATE_NAME, MO.FLOW_KEY FLOW_KEY, FP.GROUP_POWER_FLAG GROUP_FLAG, FP.POSITION_POWER_FLAG POSITION_FLAG,
        FP.ASSIGN_POS_FLAG ASSIGN_POS_FLAG, FP.ASSIGN_STAFF_FLAG ASSIGN_STAFF_FLAG, 0 as DEALER_PERMISSION_FLAG, FP.UNLIMITED_POWER NO_RESTRICT_FLAG, 0 as TYPEFLAG,
""": """        FP.ID, FP.TYPE_ID USER_ID, MI.NAME MENU_NAME, MO.NAME OPERATE_NAME, MO.FLOW_KEY FLOW_KEY,
        CASE WHEN FP.GROUP_POWER_FLAG IS NULL THEN NULL WHEN FP.GROUP_POWER_FLAG = 1 THEN 1 ELSE 0 END GROUP_FLAG,
        CASE WHEN FP.POSITION_POWER_FLAG IS NULL THEN NULL WHEN FP.POSITION_POWER_FLAG = 1 THEN 1 ELSE 0 END POSITION_FLAG,
        CASE WHEN FP.ASSIGN_POS_FLAG IS NULL THEN NULL WHEN FP.ASSIGN_POS_FLAG = 1 THEN 1 ELSE 0 END ASSIGN_POS_FLAG,
        CASE WHEN FP.ASSIGN_STAFF_FLAG IS NULL THEN NULL WHEN FP.ASSIGN_STAFF_FLAG = 1 THEN 1 ELSE 0 END ASSIGN_STAFF_FLAG,
        0 as DEALER_PERMISSION_FLAG,
        CASE WHEN FP.UNLIMITED_POWER IS NULL THEN NULL WHEN FP.UNLIMITED_POWER = 1 THEN 1 ELSE 0 END NO_RESTRICT_FLAG, 0 as TYPEFLAG,
""",
    """        RP.ID, RU.USER_ID USER_ID, MI.NAME MENU_NAME, MO.NAME OPERATE_NAME, MO.FLOW_KEY FLOW_KEY, RP.GROUP_FLAG GROUP_FLAG, RP.POSITION_FLAG POSITION_FLAG,
        RP.ASSIGN_POS_FLAG ASSIGN_POS_FLAG, RP.ASSIGN_STAFF_FLAG ASSIGN_STAFF_FLAG, RP.DEALER_PERMISSION_FLAG DEALER_PERMISSION_FLAG, RP.NO_RESTRICT_FLAG NO_RESTRICT_FLAG, 2 as TYPEFLAG,
""": """        RP.ID, RU.USER_ID USER_ID, MI.NAME MENU_NAME, MO.NAME OPERATE_NAME, MO.FLOW_KEY FLOW_KEY, RP.GROUP_FLAG GROUP_FLAG,
        CASE WHEN RP.POSITION_FLAG IS NULL THEN NULL WHEN RP.POSITION_FLAG = 1 THEN 1 ELSE 0 END POSITION_FLAG,
        CASE WHEN RP.ASSIGN_POS_FLAG IS NULL THEN NULL WHEN RP.ASSIGN_POS_FLAG = 1 THEN 1 ELSE 0 END ASSIGN_POS_FLAG,
        CASE WHEN RP.ASSIGN_STAFF_FLAG IS NULL THEN NULL WHEN RP.ASSIGN_STAFF_FLAG = 1 THEN 1 ELSE 0 END ASSIGN_STAFF_FLAG,
        CASE WHEN RP.DEALER_PERMISSION_FLAG IS NULL THEN NULL WHEN RP.DEALER_PERMISSION_FLAG = 1 THEN 1 ELSE 0 END DEALER_PERMISSION_FLAG,
        CASE WHEN RP.NO_RESTRICT_FLAG IS NULL THEN NULL WHEN RP.NO_RESTRICT_FLAG = 1 THEN 1 ELSE 0 END NO_RESTRICT_FLAG, 2 as TYPEFLAG,
""",
}


def runtime_root_default() -> Path:
    candidate = PROJECT_ROOT / "runtime" / "bap-server"
    if candidate.exists():
        return candidate
    sibling = PROJECT_ROOT.parent / "bap-server"
    if sibling.exists():
        return sibling
    return candidate


def service_jar(runtime_root: Path) -> Path:
    service_dir = runtime_root / "base-Server" / "orgmanagement"
    jars = sorted(path for path in service_dir.glob("*.jar") if ".bak" not in path.name)
    if len(jars) != 1:
        raise FileNotFoundError(f"expected one jar in {service_dir}, found {len(jars)}")
    return jars[0]


def zip_info_copy(info: zipfile.ZipInfo) -> zipfile.ZipInfo:
    copied = zipfile.ZipInfo(info.filename, info.date_time)
    copied.comment = info.comment
    copied.extra = info.extra
    copied.internal_attr = info.internal_attr
    copied.external_attr = info.external_attr
    copied.create_system = info.create_system
    copied.compress_type = info.compress_type
    return copied


def patch_mapper(xml_bytes: bytes) -> tuple[bytes, int]:
    xml = xml_bytes.decode("utf-8").replace("\r\n", "\n")
    changes = 0
    matched_fragments = 0
    for old, new in REPLACEMENTS.items():
        if old in xml:
            xml = xml.replace(old, new)
            changes += 1
            matched_fragments += 1
        elif new in xml:
            matched_fragments += 1
            continue
    xml, normalized = SMALLINT_BOOLEAN_CASE.subn(
        r"CASE WHEN \g<expr> IS NULL THEN NULL WHEN \g<expr> = 1 THEN 1 ELSE 0 END",
        xml,
    )
    changes += normalized
    if matched_fragments == 0 and changes == 0:
        raise ValueError("expected mapper fragments not found")
    return xml.encode("utf-8"), changes


def replace_mapper_in_nested_jar(nested_bytes: bytes) -> tuple[bytes, int]:
    with tempfile.TemporaryDirectory(prefix="rbac-dao-") as tmp:
        nested_in = Path(tmp) / "in.jar"
        nested_out = Path(tmp) / "out.jar"
        nested_in.write_bytes(nested_bytes)
        changes = 0
        with zipfile.ZipFile(nested_in, "r") as zin, zipfile.ZipFile(nested_out, "w") as zout:
            if MAPPER_XML not in zin.namelist():
                raise FileNotFoundError(f"missing mapper: {MAPPER_XML}")
            for info in zin.infolist():
                data = zin.read(info.filename)
                if info.filename == MAPPER_XML:
                    data, changes = patch_mapper(data)
                zout.writestr(zip_info_copy(info), data)
        return nested_out.read_bytes(), changes


def patch_service_jar(jar_path: Path, backup_suffix: str) -> int:
    backup = jar_path.with_name(jar_path.name + backup_suffix)
    if not backup.exists():
        shutil.copy2(jar_path, backup)

    changes = 0
    patched = False
    with tempfile.TemporaryDirectory(prefix="orgmanagement-rbac-") as tmp:
        output = Path(tmp) / jar_path.name
        with zipfile.ZipFile(jar_path, "r") as zin, zipfile.ZipFile(output, "w") as zout:
            for info in zin.infolist():
                data = zin.read(info.filename)
                if info.filename == RBAC_DAO_JAR:
                    data, changes = replace_mapper_in_nested_jar(data)
                    patched = True
                zout.writestr(zip_info_copy(info), data)

        if not patched:
            raise FileNotFoundError(f"{jar_path} does not contain {RBAC_DAO_JAR}")

        mode = jar_path.stat().st_mode
        shutil.move(str(output), jar_path)
        os.chmod(jar_path, mode)
    return changes


def restore_service_jar(jar_path: Path, backup_suffix: str) -> bool:
    backup = jar_path.with_name(jar_path.name + backup_suffix)
    if not backup.exists():
        return False
    shutil.copy2(backup, jar_path)
    return True


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Patch orgmanagement RBAC permission mapper for PostgreSQL smallint UNION compatibility."
    )
    parser.add_argument("--runtime-root", type=Path, default=runtime_root_default())
    parser.add_argument("--backup-suffix", default=".pre-rbac-permission-mapper.bak")
    parser.add_argument("--restore", action="store_true")
    args = parser.parse_args()

    jar_path = service_jar(args.runtime_root.resolve())
    if args.restore:
        restored = restore_service_jar(jar_path, args.backup_suffix)
        print(f"{'restored' if restored else 'missing backup for'} orgmanagement: {jar_path}")
        return

    changes = patch_service_jar(jar_path, args.backup_suffix)
    state = "patched" if changes else "already patched"
    print(f"{state} orgmanagement RBAC permission mapper: {jar_path}")


if __name__ == "__main__":
    main()
