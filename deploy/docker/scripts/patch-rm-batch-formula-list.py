#!/usr/bin/env python3
from __future__ import annotations

import argparse
import io
import shutil
import tempfile
import zipfile
from pathlib import Path


NESTED_RM_SERVICE_JAR = "BOOT-INF/lib/com.supcon.greendill.RM.service-6.1.2.3.jar"
HTML_ENTRY = "static/RM/formula/formula/batchFormulaList.html"

OLD_INTERFACE_API = '''				"sourceData": "/msService/RM/formula/formula/data", //  表单数据
				"datagridData": "/msService/RM/formula/formula/data-", //  表格数据源
				"submit": "/msService/RM/formula/formula/batchFormulaList/submit", //  表单提交
				"save": "/msService/RM/formula/formula/batchFormulaList/save", //  表单保存
				"dealInfoList": "/msService/RM/formula/formula/dealInfo-list", //  意见处理
				"editStates": "/msService/RM/formula/formula/editStates", //  配置信息'''

NEW_INTERFACE_API = '''				"mainView": "/msService/RM/formula/formula/batchFormulaView", //  主查看视图
				"downloadXls": "/msService/RM/formula/formula/downloadXls", //  下载导入模板
				"importMainXls": "/msService/RM/formula/formula/importMainXls", //  导入Excel
				"deleteData": "/msService/RM/formula/formula/delete",// 删除数据
				"sourceData": "/msService/RM/formula/formula/batchFormulaList-", //  列表数据'''

DOWNLOAD_FILENAME_COMPAT_MARKER = "adp-rm-batch-formula-xlsx-filename"
DOWNLOAD_FILENAME_COMPAT_SCRIPT = f'''<script id="{DOWNLOAD_FILENAME_COMPAT_MARKER}">
(function () {{
    var nativeClick = window.HTMLAnchorElement && window.HTMLAnchorElement.prototype.click;
    if (!nativeClick) {{
        return;
    }}
    window.HTMLAnchorElement.prototype.click = function () {{
        if (this.download === "RM_batchFormulaList.xls" && /^blob:/i.test(this.href || "")) {{
            this.download = "RM_batchFormulaList.xlsx";
        }}
        return nativeClick.apply(this, arguments);
    }};
}})();
</script>'''

AUTH_COMPAT_MARKER = "adp-rm-batch-formula-auth"
AUTH_COMPAT_SCRIPT = f'''<script id="{AUTH_COMPAT_MARKER}">
(function () {{
    function findAccessToken() {{
        var stores = [window.localStorage, window.sessionStorage];
        var keys = ["ticket", "suposTicket", "SUPOS_TICKET", "token"];
        for (var storeIndex = 0; storeIndex < stores.length; storeIndex += 1) {{
            for (var keyIndex = 0; keyIndex < keys.length; keyIndex += 1) {{
                var token = stores[storeIndex].getItem(keys[keyIndex]);
                if (token && token.length > 20) {{
                    return token.replace(/^Bearer\\s+/i, "");
                }}
            }}
        }}
        return "";
    }}

    function isSameOrigin(url) {{
        try {{
            return new URL(url, window.location.href).origin === window.location.origin;
        }} catch (_error) {{
            return false;
        }}
    }}

    var xhrPrototype = window.XMLHttpRequest && window.XMLHttpRequest.prototype;
    if (xhrPrototype && !xhrPrototype.__adpRmAuthPatched) {{
        var nativeOpen = xhrPrototype.open;
        var nativeSend = xhrPrototype.send;
        var nativeSetRequestHeader = xhrPrototype.setRequestHeader;
        xhrPrototype.open = function (method, url) {{
            this.__adpRmRequestUrl = url;
            this.__adpRmHasAuthorization = false;
            return nativeOpen.apply(this, arguments);
        }};
        xhrPrototype.setRequestHeader = function (name, value) {{
            if (String(name).toLowerCase() === "authorization") {{
                this.__adpRmHasAuthorization = true;
            }}
            return nativeSetRequestHeader.apply(this, arguments);
        }};
        xhrPrototype.send = function () {{
            var token = findAccessToken();
            if (token && !this.__adpRmHasAuthorization && isSameOrigin(this.__adpRmRequestUrl)) {{
                nativeSetRequestHeader.call(this, "Authorization", "Bearer " + token);
            }}
            return nativeSend.apply(this, arguments);
        }};
        xhrPrototype.__adpRmAuthPatched = true;
    }}

    if (window.fetch && !window.fetch.__adpRmAuthPatched) {{
        var nativeFetch = window.fetch;
        var authenticatedFetch = function (input, init) {{
            var targetUrl = typeof input === "string" ? input : input.url;
            var token = findAccessToken();
            if (token && isSameOrigin(targetUrl)) {{
                init = init || {{}};
                var headers = new Headers(init.headers || (input && input.headers) || {{}});
                if (!headers.has("Authorization")) {{
                    headers.set("Authorization", "Bearer " + token);
                }}
                init.headers = headers;
            }}
            return nativeFetch.call(window, input, init);
        }};
        authenticatedFetch.__adpRmAuthPatched = true;
        window.fetch = authenticatedFetch;
    }}
}})();
</script>'''


def clone_info(info: zipfile.ZipInfo) -> zipfile.ZipInfo:
    result = zipfile.ZipInfo(info.filename, date_time=info.date_time)
    result.compress_type = info.compress_type
    result.comment = info.comment
    result.extra = info.extra
    result.internal_attr = info.internal_attr
    result.external_attr = info.external_attr
    result.create_system = info.create_system
    return result


def read_entries(data: bytes) -> dict[str, tuple[zipfile.ZipInfo, bytes]]:
    entries: dict[str, tuple[zipfile.ZipInfo, bytes]] = {}
    with zipfile.ZipFile(io.BytesIO(data), "r") as zf:
        for info in zf.infolist():
            entries[info.filename] = (info, zf.read(info.filename))
    return entries


def write_entries(entries: dict[str, tuple[zipfile.ZipInfo, bytes]]) -> bytes:
    output = io.BytesIO()
    with zipfile.ZipFile(output, "w") as zf:
        for name, (info, data) in entries.items():
            zf.writestr(clone_info(info), data)
    return output.getvalue()


def replace_once(text: str, old: str, new: str) -> tuple[str, bool]:
    if new in text:
        return text, False
    if old not in text:
        raise RuntimeError(f"expected RM page fragment was not found: {old[:80]!r}")
    return text.replace(old, new, 1), True


def inject_download_filename_compat(text: str) -> tuple[str, bool]:
    if DOWNLOAD_FILENAME_COMPAT_MARKER in text:
        return text, False
    if "</body>" in text:
        return text.replace("</body>", f"{DOWNLOAD_FILENAME_COMPAT_SCRIPT}\n</body>", 1), True
    if "</html>" in text:
        return text.replace("</html>", f"{DOWNLOAD_FILENAME_COMPAT_SCRIPT}\n</html>", 1), True
    return f"{text}\n{DOWNLOAD_FILENAME_COMPAT_SCRIPT}\n", True


def inject_auth_compat(text: str) -> tuple[str, bool]:
    if AUTH_COMPAT_MARKER in text:
        return text, False
    if "</body>" in text:
        return text.replace("</body>", f"{AUTH_COMPAT_SCRIPT}\n</body>", 1), True
    if "</html>" in text:
        return text.replace("</html>", f"{AUTH_COMPAT_SCRIPT}\n</html>", 1), True
    return f"{text}\n{AUTH_COMPAT_SCRIPT}\n", True


def patch_html(data: bytes) -> tuple[bytes, list[str]]:
    text = data.decode("utf-8")
    changes: list[str] = []

    replacements = (
        ('"viewType": "EXTRA"', '"viewType": "LIST"', "viewType"),
        ('"workflowEnabled": true', '"workflowEnabled": false', "workflowEnabled"),
        (OLD_INTERFACE_API, NEW_INTERFACE_API, "interfaceApi"),
        ("/greenDill/static/scripts/edit.js", "/greenDill/static/scripts/list.js", "runtimeScript"),
    )
    for old, new, label in replacements:
        text, changed = replace_once(text, old, new)
        if changed:
            changes.append(label)

    text, changed = inject_download_filename_compat(text)
    if changed:
        changes.append("downloadFilename")

    text, changed = inject_auth_compat(text)
    if changed:
        changes.append("browserAuth")

    return text.encode("utf-8"), changes


def patch_service_jar(data: bytes) -> tuple[bytes, list[str]]:
    entries = read_entries(data)
    entry = entries.get(HTML_ENTRY)
    if entry is None:
        raise RuntimeError(f"{HTML_ENTRY} was not found in {NESTED_RM_SERVICE_JAR}")

    info, html = entry
    patched_html, changes = patch_html(html)
    if not changes:
        return data, changes

    entries[HTML_ENTRY] = (info, patched_html)
    return write_entries(entries), changes


def patch_outer_jar(jar_path: Path, backup_suffix: str, dry_run: bool) -> bool:
    jar_data = jar_path.read_bytes()
    entries = read_entries(jar_data)
    nested_entry = entries.get(NESTED_RM_SERVICE_JAR)
    if nested_entry is None:
        raise RuntimeError(f"{NESTED_RM_SERVICE_JAR} was not found in {jar_path}")

    nested_info, nested_data = nested_entry
    patched_nested, changes = patch_service_jar(nested_data)
    if not changes:
        print(f"{jar_path}: already patched")
        return False

    print(f"{jar_path}: patched {', '.join(changes)}")
    if dry_run:
        return True

    entries[NESTED_RM_SERVICE_JAR] = (nested_info, patched_nested)
    patched_outer = write_entries(entries)

    backup_path = jar_path.with_name(jar_path.name + backup_suffix)
    if not backup_path.exists():
        shutil.copy2(jar_path, backup_path)

    with tempfile.NamedTemporaryFile(dir=jar_path.parent, delete=False) as temp_file:
        temp_path = Path(temp_file.name)
        temp_file.write(patched_outer)
    shutil.copystat(jar_path, temp_path)
    temp_path.replace(jar_path)
    return True


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Patch RM batchFormulaList static page in RMMs jar.")
    parser.add_argument("jar", type=Path, help="Path to RMMs-1.0.0.jar")
    parser.add_argument("--backup-suffix", default=".bak-rm-batch-list", help="Suffix for the first backup copy.")
    parser.add_argument("--dry-run", action="store_true", help="Validate patchability without writing the jar.")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    patch_outer_jar(args.jar, args.backup_suffix, args.dry_run)


if __name__ == "__main__":
    main()
