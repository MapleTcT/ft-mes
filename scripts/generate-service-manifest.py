#!/usr/bin/env python3
import json
import re
import subprocess
import zipfile
from pathlib import Path

REPO_ROOT = Path.cwd()
ADP_ROOT = REPO_ROOT.parent
BASE_SERVER = ADP_ROOT / "bap-server" / "base-Server"
CONFIG_GROUP = ADP_ROOT / "bap-server" / "config" / "configgroup"
OUT_SERVICES = REPO_ROOT / "backend" / "services"
METADATA_ROOT = REPO_ROOT / "metadata"


def zip_lines(jar: Path, member: str):
    try:
        with zipfile.ZipFile(jar) as archive:
            with archive.open(member) as fh:
                return fh.read().decode("utf-8", "replace").splitlines()
    except Exception:
        return []


def manifest_value(lines, key):
    prefix = f"{key}:"
    for line in lines:
        if line.startswith(prefix):
            return line.split(":", 1)[1].strip()
    return ""


def unfold_manifest(lines):
    unfolded = []
    for line in lines:
        if line.startswith(" ") and unfolded:
            unfolded[-1] += line[1:]
        else:
            unfolded.append(line)
    return unfolded


def jar_entries(jar: Path):
    try:
        with zipfile.ZipFile(jar) as archive:
            return archive.namelist()
    except Exception:
        return []


def read_text(path: Path):
    if not path.exists():
        return ""
    return path.read_text(encoding="utf-8", errors="replace")


def discover_config_ids(text: str):
    ids = []
    for match in re.finditer(r"data-id:\s*([A-Za-z0-9_.@-]+)|data-id=([A-Za-z0-9_.@-]+)", text):
        ids.append(next(group for group in match.groups() if group))
    return sorted(set(ids))


def config_value(config_file: Path, key: str):
    text = read_text(config_file)
    for line in text.splitlines():
        if line.strip().startswith(f"{key}="):
            return line.split("=", 1)[1].strip()
    return ""


def service_info(service_dir: Path):
    jars = sorted(service_dir.glob("*.jar"))
    if not jars:
        return None
    jar = jars[0]
    manifest = unfold_manifest(zip_lines(jar, "META-INF/MANIFEST.MF"))
    entries = jar_entries(jar)
    local_configs = [entry for entry in entries if re.search(r"(application|bootstrap).*\.(yml|yaml|properties)$", entry)]
    local_config_text = "\n".join("\n".join(zip_lines(jar, entry)) for entry in local_configs)
    local_file_configs = sorted(
        path for path in service_dir.iterdir()
        if path.is_file() and path.suffix.lower() in {".yml", ".yaml", ".properties", ".xml"}
    )
    local_file_text = "\n".join(read_text(path) for path in local_file_configs)
    config_ids = discover_config_ids(local_config_text + "\n" + local_file_text)
    config_files = [CONFIG_GROUP / data_id for data_id in config_ids if (CONFIG_GROUP / data_id).exists()]
    ports = []
    apps = []
    for config_file in config_files:
        port = config_value(config_file, "server.port")
        app = config_value(config_file, "spring.application.name")
        if port:
            ports.append({"config": config_file.name, "port": port})
        if app:
            apps.append({"config": config_file.name, "applicationName": app})
    libs = [entry for entry in entries if entry.startswith("BOOT-INF/lib/") and entry.endswith(".jar")]
    package_roots = sorted({
        "/".join(entry.split("/")[:3])
        for entry in entries
        if entry.endswith(".class") and (entry.startswith("BOOT-INF/classes/com/") or entry.startswith("com/"))
    })
    return {
        "service": service_dir.name,
        "jar": jar.relative_to(ADP_ROOT).as_posix(),
        "jarSizeBytes": jar.stat().st_size,
        "mainClass": manifest_value(manifest, "Main-Class"),
        "startClass": manifest_value(manifest, "Start-Class"),
        "springBootVersion": manifest_value(manifest, "Spring-Boot-Version"),
        "localConfigEntries": local_configs,
        "localConfigFiles": [path.name for path in local_file_configs],
        "nacosDataIds": config_ids,
        "resolvedPorts": ports,
        "resolvedApplications": apps,
        "embeddedLibCount": len(libs),
        "classPackageRoots": package_roots[:40],
    }


def write_service_readme(info):
    service_dir = OUT_SERVICES / info["service"]
    service_dir.mkdir(parents=True, exist_ok=True)
    lines = [
        f"# {info['service']}",
        "",
        f"- 原始 JAR: `{info['jar']}`",
        f"- Main-Class: `{info['mainClass'] or 'N/A'}`",
        f"- Start-Class: `{info['startClass'] or 'N/A'}`",
        f"- Spring Boot: `{info['springBootVersion'] or 'N/A'}`",
        f"- 嵌入依赖数量: `{info['embeddedLibCount']}`",
        "",
        "## 配置入口",
    ]
    if info["localConfigEntries"] or info["localConfigFiles"]:
        for item in info["localConfigEntries"]:
            lines.append(f"- JAR 内: `{item}`")
        for item in info["localConfigFiles"]:
            lines.append(f"- 外部文件: `{item}`")
    else:
        lines.append("- 未发现显式 application/bootstrap 配置入口。")
    lines.extend(["", "## Nacos Data IDs"])
    if info["nacosDataIds"]:
        lines.extend(f"- `{item}`" for item in info["nacosDataIds"])
    else:
        lines.append("- 未发现。")
    lines.extend(["", "## 端口和应用名"])
    for port in info["resolvedPorts"]:
        lines.append(f"- `{port['config']}`: `{port['port']}`")
    for app in info["resolvedApplications"]:
        lines.append(f"- `{app['config']}`: `{app['applicationName']}`")
    lines.append("")
    (service_dir / "README.md").write_text("\n".join(lines), encoding="utf-8")


def main():
    OUT_SERVICES.mkdir(parents=True, exist_ok=True)
    METADATA_ROOT.mkdir(parents=True, exist_ok=True)
    services = []
    for service_dir in sorted(path for path in BASE_SERVER.iterdir() if path.is_dir()):
        info = service_info(service_dir)
        if info:
            services.append(info)
            write_service_readme(info)
    summary = {"serviceCount": len(services), "services": services}
    (METADATA_ROOT / "backend-service-manifest.json").write_text(json.dumps(summary, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps(summary, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
