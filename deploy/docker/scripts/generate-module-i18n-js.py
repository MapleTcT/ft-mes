#!/usr/bin/env python3
"""Convert a UTF-8 Java properties bundle into GreenDill i18n JavaScript."""

from __future__ import annotations

import argparse
import json
from collections import OrderedDict
from pathlib import Path
from typing import Iterable, MutableMapping


def logical_lines(lines: Iterable[str]) -> Iterable[str]:
    pending = ""
    for physical in lines:
        line = physical.rstrip("\r\n")
        if pending:
            line = pending + line.lstrip()
        trailing_backslashes = len(line) - len(line.rstrip("\\"))
        if trailing_backslashes % 2 == 1:
            pending = line[:-1]
            continue
        pending = ""
        yield line
    if pending:
        yield pending


def unescape_java_property(value: str) -> str:
    result: list[str] = []
    index = 0
    escapes = {"t": "\t", "n": "\n", "r": "\r", "f": "\f"}
    while index < len(value):
        char = value[index]
        if char != "\\" or index + 1 >= len(value):
            result.append(char)
            index += 1
            continue
        escaped = value[index + 1]
        if escaped == "u" and index + 5 < len(value):
            digits = value[index + 2:index + 6]
            try:
                result.append(chr(int(digits, 16)))
                index += 6
                continue
            except ValueError:
                pass
        result.append(escapes.get(escaped, escaped))
        index += 2
    return "".join(result)


def split_property(line: str) -> tuple[str, str]:
    escaped = False
    separator = len(line)
    for index, char in enumerate(line):
        if escaped:
            escaped = False
            continue
        if char == "\\":
            escaped = True
            continue
        if char in "=:" or char.isspace():
            separator = index
            break

    value_start = separator
    while value_start < len(line) and line[value_start].isspace():
        value_start += 1
    if value_start < len(line) and line[value_start] in "=:":
        value_start += 1
    while value_start < len(line) and line[value_start].isspace():
        value_start += 1
    return line[:separator], line[value_start:]


def parse_properties(text: str) -> MutableMapping[str, str]:
    resources: MutableMapping[str, str] = OrderedDict()
    for logical in logical_lines(text.splitlines(keepends=True)):
        stripped = logical.lstrip()
        if not stripped or stripped.startswith(("#", "!")):
            continue
        raw_key, raw_value = split_property(stripped)
        resources[unescape_java_property(raw_key)] = unescape_java_property(raw_value)
    return resources


def merge_properties(primary: Path, fallbacks: Iterable[Path]) -> tuple[MutableMapping[str, str], str]:
    resources: MutableMapping[str, str] = OrderedDict()
    source_names: list[str] = []
    for path in [*fallbacks, primary]:
        parsed = parse_properties(path.read_text(encoding="utf-8-sig"))
        resources.update(parsed)
        source_names.append(path.name)
    return resources, " + ".join(source_names)


def render_javascript(
    resources: MutableMapping[str, str], source_name: str, module_code: str = "MODULE"
) -> str:
    safe_module_code = "".join(char if char.isalnum() else "_" for char in module_code).upper()
    lines = [
        f"// Generated from {source_name}; do not edit by hand.",
        "window.InternationalResource = window.InternationalResource || {};",
    ]
    for key, value in resources.items():
        lines.append(
            "window.InternationalResource["
            + json.dumps(key, ensure_ascii=False)
            + "] = "
            + json.dumps(value, ensure_ascii=False)
            + ";"
        )
    lines.extend(
        [
            "",
            f"(function install{safe_module_code}I18nCompatibility(resources) {{",
            f'  var patchFlag = "__adpModuleI18nPatched_{safe_module_code}";',
            f'  var observerFlag = "__adpModuleI18nObserver_{safe_module_code}";',
            "  var attempts = 0;",
            "",
            "  function accessibleWindows() {",
            "    var windows = [];",
            "    function add(candidate) {",
            "      try {",
            "        if (candidate && candidate.document && windows.indexOf(candidate) === -1) {",
            "          windows.push(candidate);",
            "        }",
            "      } catch (_error) {}",
            "    }",
            "    add(window);",
            "    add(window.parent);",
            "    add(window.top);",
            "    return windows;",
            "  }",
            "",
            "  function patchInternational(targetWindow) {",
            "    var reactApi = targetWindow.ReactAPI;",
            "    var international = reactApi && reactApi.international;",
            "    if (!international || typeof international.getText !== \"function\") {",
            "      return false;",
            "    }",
            "    if (typeof international.getLanguageObjData === \"function\") {",
            "      var languageData = international.getLanguageObjData();",
            "      if (languageData && typeof languageData === \"object\") {",
            "        Object.keys(resources).forEach(function copyResource(key) {",
            "          languageData[key] = resources[key];",
            "        });",
            "      }",
            "    }",
            "    if (!international[patchFlag]) {",
            "      var originalGetText = international.getText.bind(international);",
            "      international.getText = function getTextWithModuleFallback(key) {",
            "        var value = originalGetText.apply(null, arguments);",
            "        if ((value === key || value === \"\" || value == null) && resources[key] != null) {",
            "          return resources[key];",
            "        }",
            "        return value;",
            "      };",
            "      international[patchFlag] = true;",
            "    }",
            "    return true;",
            "  }",
            "",
            "  function translateTextNode(node) {",
            "    var original = node && node.nodeValue;",
            "    if (typeof original !== \"string\") return;",
            "    var trimmed = original.trim();",
            "    if (resources[trimmed] == null) return;",
            "    node.nodeValue = original.replace(trimmed, resources[trimmed]);",
            "  }",
            "",
            "  function translateSubtree(root) {",
            "    if (!root || !root.ownerDocument || !root.ownerDocument.createTreeWalker) return;",
            "    var doc = root.ownerDocument;",
            "    var targetWindow = doc.defaultView || window;",
            "    var nodeFilter = targetWindow.NodeFilter || window.NodeFilter;",
            "    if (!nodeFilter) return;",
            "    var walker = doc.createTreeWalker(root, nodeFilter.SHOW_TEXT, null, false);",
            "    var node;",
            "    while ((node = walker.nextNode())) {",
            "      var parent = node.parentNode;",
            "      if (!parent || /^(SCRIPT|STYLE|TEXTAREA|INPUT)$/i.test(parent.nodeName || \"\")) continue;",
            "      translateTextNode(node);",
            "    }",
            "  }",
            "",
            "  function installObserver(targetWindow) {",
            "    var doc = targetWindow.document;",
            "    if (!doc || !doc.body) return;",
            "    translateSubtree(doc.body);",
            "    if (!targetWindow.MutationObserver || targetWindow[observerFlag]) return;",
            "    targetWindow[observerFlag] = true;",
            "    var observer = new targetWindow.MutationObserver(function translateMutations(mutations) {",
            "      mutations.forEach(function translateMutation(mutation) {",
            "        if (mutation.type === \"characterData\") {",
            "          translateTextNode(mutation.target);",
            "          return;",
            "        }",
            "        Array.prototype.forEach.call(mutation.addedNodes || [], function translateAdded(node) {",
            "          if (node.nodeType === 3) translateTextNode(node);",
            "          if (node.nodeType === 1) translateSubtree(node);",
            "        });",
            "      });",
            "    });",
            "    observer.observe(doc.body, { childList: true, subtree: true, characterData: true });",
            "  }",
            "",
            "  function install() {",
            "    var patched = false;",
            "    accessibleWindows().forEach(function installWindow(targetWindow) {",
            "      patched = patchInternational(targetWindow) || patched;",
            "      installObserver(targetWindow);",
            "    });",
            "    attempts += 1;",
            "    return patched || attempts >= 100;",
            "  }",
            "",
            "  if (!install()) {",
            "    var timer = window.setInterval(function retryInstall() {",
            "      if (install()) window.clearInterval(timer);",
            "    }, 100);",
            "  }",
            "  if (document.readyState === \"loading\") {",
            "    document.addEventListener(\"DOMContentLoaded\", install);",
            "  }",
            f"}})(window.InternationalResource);",
        ]
    )
    return "\n".join(lines) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--properties", required=True, type=Path)
    parser.add_argument("--fallback-properties", action="append", default=[], type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--module-code")
    args = parser.parse_args()

    resources, source_name = merge_properties(args.properties, args.fallback_properties)
    if not resources:
        parser.error(f"no resources found in {args.properties}")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    module_code = args.module_code or args.properties.stem.split("_", 1)[0]
    args.output.write_text(
        render_javascript(resources, source_name, module_code), encoding="utf-8"
    )
    print(f"generated {len(resources)} resources at {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
