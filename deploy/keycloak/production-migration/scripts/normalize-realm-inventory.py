#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import hashlib
import json
from pathlib import Path
from typing import Any


def load_json(path: Path, default: Any) -> Any:
    if not path.exists():
        return default
    return json.loads(path.read_text(encoding="utf-8"))


def clean(value: Any) -> str:
    text = "" if value is None else str(value)
    return text.replace("\t", " ").replace("\r", " ").replace("\n", " ").strip()


def fingerprint(value: str) -> str:
    if not value:
        return ""
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def sorted_strings(items: Any) -> list[str]:
    if not isinstance(items, list):
        return []
    return sorted(clean(item) for item in items if clean(item))


def client_inventory(clients: Any) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for item in clients if isinstance(clients, list) else []:
        if not isinstance(item, dict):
            continue
        rows.append(
            {
                "clientId": clean(item.get("clientId")),
                "enabled": bool(item.get("enabled")),
                "publicClient": bool(item.get("publicClient")),
                "bearerOnly": bool(item.get("bearerOnly")),
                "directAccessGrantsEnabled": bool(item.get("directAccessGrantsEnabled")),
                "standardFlowEnabled": bool(item.get("standardFlowEnabled")),
                "implicitFlowEnabled": bool(item.get("implicitFlowEnabled")),
                "redirectUris": sorted_strings(item.get("redirectUris")),
                "webOrigins": sorted_strings(item.get("webOrigins")),
                "defaultClientScopes": sorted_strings(item.get("defaultClientScopes")),
            }
        )
    return sorted(rows, key=lambda row: row["clientId"])


def named_inventory(items: Any, name_key: str = "name") -> list[str]:
    names = []
    for item in items if isinstance(items, list) else []:
        if isinstance(item, dict) and clean(item.get(name_key)):
            names.append(clean(item.get(name_key)))
    return sorted(set(names))


def component_inventory(items: Any) -> list[dict[str, str]]:
    rows: list[dict[str, str]] = []
    for item in items if isinstance(items, list) else []:
        if not isinstance(item, dict):
            continue
        rows.append(
            {
                "name": clean(item.get("name")),
                "providerId": clean(item.get("providerId")),
                "providerType": clean(item.get("providerType")),
            }
        )
    return sorted(rows, key=lambda row: (row["providerType"], row["providerId"], row["name"]))


def write_tsv(path: Path, fieldnames: list[str], rows: list[dict[str, Any]]) -> None:
    with path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=fieldnames, delimiter="\t")
        writer.writeheader()
        for row in rows:
            writer.writerow({field: row.get(field, "") for field in fieldnames})


def main() -> int:
    parser = argparse.ArgumentParser(description="Normalize Keycloak Admin API inventory responses.")
    parser.add_argument("--role", choices=["source", "target"], required=True)
    parser.add_argument("--realm", required=True)
    parser.add_argument("--raw-dir", type=Path, required=True)
    parser.add_argument("--output-dir", type=Path, required=True)
    args = parser.parse_args()

    realm = load_json(args.raw_dir / "realm.json", {})
    clients = client_inventory(load_json(args.raw_dir / "clients.json", []))
    roles = named_inventory(load_json(args.raw_dir / "roles.json", []))
    client_scopes = named_inventory(load_json(args.raw_dir / "client-scopes.json", []))
    components = component_inventory(load_json(args.raw_dir / "components.json", []))
    users_count = load_json(args.raw_dir / "users-count.json", 0)
    users_sample = load_json(args.raw_dir / "users-sample.json", [])

    inventory = {
        "role": args.role,
        "realm": args.realm,
        "realmEnabled": bool(realm.get("enabled")) if isinstance(realm, dict) else False,
        "sslRequired": clean(realm.get("sslRequired")) if isinstance(realm, dict) else "",
        "publicKeyFingerprint": fingerprint(clean(realm.get("public_key")) if isinstance(realm, dict) else ""),
        "clients": clients,
        "roles": roles,
        "clientScopes": client_scopes,
        "components": components,
        "users": {
            "count": users_count if isinstance(users_count, int) else clean(users_count),
            "sampleCount": len(users_sample) if isinstance(users_sample, list) else 0,
        },
    }

    args.output_dir.mkdir(parents=True, exist_ok=True)
    (args.output_dir / f"{args.role}-realm-inventory.json").write_text(
        json.dumps(inventory, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    write_tsv(
        args.output_dir / f"{args.role}-clients.tsv",
        [
            "clientId",
            "enabled",
            "publicClient",
            "bearerOnly",
            "directAccessGrantsEnabled",
            "standardFlowEnabled",
            "implicitFlowEnabled",
            "redirectUris",
            "webOrigins",
            "defaultClientScopes",
        ],
        [
            {
                **client,
                "redirectUris": ",".join(client["redirectUris"]),
                "webOrigins": ",".join(client["webOrigins"]),
                "defaultClientScopes": ",".join(client["defaultClientScopes"]),
            }
            for client in clients
        ],
    )
    write_tsv(args.output_dir / f"{args.role}-roles.tsv", ["role"], [{"role": role} for role in roles])
    write_tsv(
        args.output_dir / f"{args.role}-client-scopes.tsv",
        ["clientScope"],
        [{"clientScope": scope} for scope in client_scopes],
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
