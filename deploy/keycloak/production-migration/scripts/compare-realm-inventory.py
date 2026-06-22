#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path
from typing import Any


def load(path: Path) -> dict[str, Any]:
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict):
        raise SystemExit(f"{path} must contain an object")
    return data


def by_client_id(data: dict[str, Any]) -> dict[str, dict[str, Any]]:
    return {
        str(item.get("clientId", "")): item
        for item in data.get("clients", [])
        if isinstance(item, dict) and item.get("clientId")
    }


def compare_sets(name: str, source: set[str], target: set[str]) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for item in sorted(source - target):
        rows.append({"scope": name, "item": item, "status": "TARGET_MISSING"})
    for item in sorted(target - source):
        rows.append({"scope": name, "item": item, "status": "SOURCE_MISSING"})
    for item in sorted(source & target):
        rows.append({"scope": name, "item": item, "status": "MATCH"})
    return rows


def compare_clients(source: dict[str, Any], target: dict[str, Any]) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    source_clients = by_client_id(source)
    target_clients = by_client_id(target)
    for client_id in sorted(set(source_clients) | set(target_clients)):
        source_client = source_clients.get(client_id)
        target_client = target_clients.get(client_id)
        if source_client is None:
            rows.append({"scope": "client", "item": client_id, "status": "SOURCE_MISSING"})
            continue
        if target_client is None:
            rows.append({"scope": "client", "item": client_id, "status": "TARGET_MISSING"})
            continue

        checks = [
            "enabled",
            "publicClient",
            "bearerOnly",
            "directAccessGrantsEnabled",
            "standardFlowEnabled",
            "implicitFlowEnabled",
            "redirectUris",
            "webOrigins",
            "defaultClientScopes",
        ]
        mismatched = [field for field in checks if source_client.get(field) != target_client.get(field)]
        rows.append(
            {
                "scope": "client",
                "item": client_id,
                "status": "MATCH" if not mismatched else "CLIENT_MISMATCH",
                "detail": ",".join(mismatched),
            }
        )
    return rows


def compare_components(source: dict[str, Any], target: dict[str, Any]) -> list[dict[str, Any]]:
    def key(item: Any) -> str:
        if not isinstance(item, dict):
            return ""
        return "|".join(str(item.get(field, "")) for field in ("providerType", "providerId", "name"))

    return compare_sets(
        "component",
        {key(item) for item in source.get("components", []) if key(item)},
        {key(item) for item in target.get("components", []) if key(item)},
    )


def compare(source: dict[str, Any], target: dict[str, Any]) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for field in ("realm", "realmEnabled", "sslRequired"):
        rows.append(
            {
                "scope": "realm",
                "item": field,
                "source": source.get(field),
                "target": target.get(field),
                "status": "MATCH" if source.get(field) == target.get(field) else "REALM_MISMATCH",
            }
        )

    public_key_status = (
        "MATCH"
        if source.get("publicKeyFingerprint") == target.get("publicKeyFingerprint")
        else "JWT_KEY_DIFF_REQUIRES_NACOS_SYNC"
    )
    rows.append(
        {
            "scope": "realm",
            "item": "publicKeyFingerprint",
            "source": source.get("publicKeyFingerprint"),
            "target": target.get("publicKeyFingerprint"),
            "status": public_key_status,
        }
    )

    rows.extend(compare_clients(source, target))
    rows.extend(compare_sets("role", set(source.get("roles", [])), set(target.get("roles", []))))
    rows.extend(compare_sets("clientScope", set(source.get("clientScopes", [])), set(target.get("clientScopes", []))))
    rows.extend(compare_components(source, target))
    return rows


def write_tsv(path: Path, rows: list[dict[str, Any]]) -> None:
    fields = ["scope", "item", "source", "target", "status", "detail"]
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields, delimiter="\t")
        writer.writeheader()
        for row in rows:
            writer.writerow({field: row.get(field, "") for field in fields})


def main() -> int:
    parser = argparse.ArgumentParser(description="Compare source and target Keycloak realm inventories.")
    parser.add_argument("--source", type=Path, required=True, help="source-realm-inventory.json")
    parser.add_argument("--target", type=Path, required=True, help="target-realm-inventory.json")
    parser.add_argument("--output-dir", type=Path, required=True)
    parser.add_argument("--allow-difference", action="store_true", help="write reports but return success even on differences")
    args = parser.parse_args()

    rows = compare(load(args.source), load(args.target))
    args.output_dir.mkdir(parents=True, exist_ok=True)
    tsv_path = args.output_dir / "realm-inventory-comparison.tsv"
    json_path = args.output_dir / "realm-inventory-comparison.json"
    write_tsv(tsv_path, rows)

    difference_statuses = {
        "REALM_MISMATCH",
        "JWT_KEY_DIFF_REQUIRES_NACOS_SYNC",
        "CLIENT_MISMATCH",
        "SOURCE_MISSING",
        "TARGET_MISSING",
    }
    summary = {
        "totalChecks": len(rows),
        "match": sum(1 for row in rows if row["status"] == "MATCH"),
        "differences": sum(1 for row in rows if row["status"] in difference_statuses),
        "byStatus": {},
    }
    for row in rows:
        summary["byStatus"][row["status"]] = summary["byStatus"].get(row["status"], 0) + 1

    json_path.write_text(
        json.dumps({"summary": summary, "items": rows}, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Wrote {tsv_path}")
    print(f"Wrote {json_path}")
    if args.allow_difference:
        return 0
    if summary["differences"]:
        print(f"Keycloak realm inventory comparison failed: {summary['differences']} difference(s).")
        return 1
    print("Keycloak realm inventory comparison passed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
