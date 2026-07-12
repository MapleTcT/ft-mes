#!/usr/bin/env python3
from __future__ import annotations

import json
import sys
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
UI = ROOT / "frontend/apps/bpi"
ACCEPTANCE = ROOT / "metadata/bpi-ui-acceptance.json"
REQUIRED = [
    "README.md",
    "package.json",
    "package-lock.json",
    "tsconfig.json",
    "vite.config.ts",
    "index.html",
    "src/api.ts",
    "src/main.ts",
    "src/styles.css",
    "src/types.ts",
    "tests/bpi-console.e2e.cjs",
]


def main() -> int:
    failures: list[str] = []
    for relative in REQUIRED:
        if not (UI / relative).is_file():
            failures.append(f"missing BPI UI file: frontend/apps/bpi/{relative}")
    if not ACCEPTANCE.is_file():
        failures.append("missing BPI UI acceptance evidence: metadata/bpi-ui-acceptance.json")

    if not failures:
        package = json.loads((UI / "package.json").read_text(encoding="utf-8"))
        if package.get("devDependencies", {}).get("vite") != "6.4.3":
            failures.append("BPI UI must remain on the audited Vite 6.4.3 patch baseline")
        scripts = package.get("scripts", {})
        for name in ("build", "test:e2e"):
            if name not in scripts:
                failures.append(f"BPI UI package is missing {name!r} script")

        api = (UI / "src/api.ts").read_text(encoding="utf-8")
        for required in ("const API_ROOT = '/bpi-api'", "localStorage.getItem('ticket')", "Idempotency-Key", "If-Match", "rejectCandidate"):
            if required not in api:
                failures.append(f"BPI UI API client is missing {required!r}")
        forbidden = ("BPI_INTERNAL_JWT_SECRET", "http://bpi-service", "https://bpi-service")
        for path in (UI / "src").rglob("*"):
            if path.is_file() and path.suffix in {".ts", ".css", ".html"}:
                text = path.read_text(encoding="utf-8")
                for marker in forbidden:
                    if marker in text:
                        failures.append(f"{path.relative_to(ROOT)} exposes forbidden marker {marker!r}")

        e2e = (UI / "tests/bpi-console.e2e.cjs").read_text(encoding="utf-8")
        for required in ("console", "pageerror", "requestfailed", "/tmp/bpi-console-desktop.png", "/tmp/bpi-console-candidate-rejected.png", "document.documentElement.scrollWidth"):
            if required not in e2e:
                failures.append(f"BPI UI E2E is missing {required!r} evidence")

        acceptance = json.loads(ACCEPTANCE.read_text(encoding="utf-8"))
        if acceptance.get("scope") != "deterministic BPI simulator browser acceptance":
            failures.append("BPI UI acceptance must declare its deterministic simulator scope")
        summary = acceptance.get("summary", {})
        if summary.get("browserTests") != 3 or summary.get("pass") != 3:
            failures.append("BPI UI acceptance must record three passing browser tests")
        if any(summary.get(key) != 0 for key in ("consoleErrors", "pageErrors", "requestFailures")):
            failures.append("BPI UI acceptance contains browser errors")
        limitations = " ".join(acceptance.get("limitations", []))
        for required in ("simulator", "PostgreSQL", "MES shell"):
            if required not in limitations:
                failures.append(f"BPI UI acceptance limitations are missing {required!r}")

    if failures:
        print("\n".join(f"BPI UI error: {item}" for item in failures), file=sys.stderr)
        return 1
    print("BPI UI source, auth boundary, command headers, and browser acceptance contract verified.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
