from __future__ import annotations

import json
import os
import stat
import sys
from pathlib import Path
from tempfile import NamedTemporaryFile
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import quote, urlencode
from urllib.request import Request, urlopen


CATALOG_NAME = "ft_mes_bpi"
PUBLISHER_PRINCIPAL = "bpi-dataset-catalog-publisher"
PUBLISHER_PRINCIPAL_ROLE = "bpi_dataset_catalog_publisher"
PUBLISHER_CATALOG_ROLE = "bpi_dataset_catalog_publisher"
RECOVERY_PRINCIPAL = "bpi-dataset-recovery-operator"
RECOVERY_PRINCIPAL_ROLE = "bpi_dataset_recovery_operator"
RECOVERY_CATALOG_ROLE = "bpi_dataset_recovery_operator"
PUBLISHER_GRANTS = frozenset(
    {
        "NAMESPACE_CREATE",
        "NAMESPACE_LIST",
        "NAMESPACE_READ_PROPERTIES",
        "NAMESPACE_WRITE_PROPERTIES",
        "TABLE_CREATE",
        "TABLE_LIST",
        "TABLE_READ_PROPERTIES",
        "TABLE_WRITE_PROPERTIES",
        "TABLE_READ_DATA",
        "TABLE_WRITE_DATA",
        "TABLE_ASSIGN_UUID",
        "TABLE_ADD_SCHEMA",
        "TABLE_SET_CURRENT_SCHEMA",
        "TABLE_ADD_PARTITION_SPEC",
        "TABLE_ADD_SNAPSHOT",
        "TABLE_SET_SNAPSHOT_REF",
        "TABLE_SET_PROPERTIES",
    }
)
RECOVERY_GRANTS = PUBLISHER_GRANTS | frozenset({"NAMESPACE_DROP", "TABLE_DROP"})


class BootstrapError(RuntimeError):
    pass


def required(name: str) -> str:
    value = os.getenv(name, "").strip()
    if not value:
        raise BootstrapError(f"{name} is required")
    return value


def enabled(name: str, default: bool = False) -> bool:
    value = os.getenv(name, str(default)).strip().lower()
    if value in {"true", "1", "yes", "on"}:
        return True
    if value in {"false", "0", "no", "off"}:
        return False
    raise BootstrapError(f"{name} must be true or false")


class PolarisApi:
    def __init__(self, base_url: str, realm: str, timeout: int = 15):
        self.base_url = base_url.rstrip("/")
        self.realm = realm
        self.timeout = timeout

    def token(self, client_id: str, client_secret: str) -> str:
        basic = __import__("base64").b64encode(
            f"{client_id}:{client_secret}".encode("utf-8")
        ).decode("ascii")
        status, body = self.request(
            "POST",
            "/api/catalog/v1/oauth/tokens",
            headers={
                "Authorization": f"Basic {basic}",
                "Content-Type": "application/x-www-form-urlencoded",
            },
            data=urlencode(
                {"grant_type": "client_credentials", "scope": "PRINCIPAL_ROLE:ALL"}
            ).encode("ascii"),
            expected={200},
        )
        token = body.get("access_token") if isinstance(body, dict) else None
        if status != 200 or not token:
            raise BootstrapError("Polaris did not return an OAuth access token")
        return str(token)

    def json(
        self,
        method: str,
        path: str,
        token: str,
        payload: dict[str, Any] | None = None,
        expected: set[int] | None = None,
    ) -> tuple[int, Any]:
        headers = {
            "Authorization": f"Bearer {token}",
            "Accept": "application/json",
        }
        data = None
        if payload is not None:
            headers["Content-Type"] = "application/json"
            data = json.dumps(payload, separators=(",", ":")).encode("utf-8")
        return self.request(method, path, headers, data, expected or {200})

    def request(
        self,
        method: str,
        path: str,
        headers: dict[str, str],
        data: bytes | None,
        expected: set[int],
    ) -> tuple[int, Any]:
        request = Request(
            f"{self.base_url}{path}",
            data=data,
            headers=headers | {"Polaris-Realm": self.realm},
            method=method,
        )
        try:
            with urlopen(request, timeout=self.timeout) as response:
                status = response.status
                raw = response.read()
        except HTTPError as exception:
            status = exception.code
            raw = exception.read()
        except URLError as exception:
            raise BootstrapError(f"Polaris is unavailable: {exception.reason}") from exception
        body: Any = None
        if raw:
            try:
                body = json.loads(raw)
            except json.JSONDecodeError:
                body = raw.decode("utf-8", errors="replace")[:500]
        if status not in expected:
            detail = body if isinstance(body, str) else json.dumps(body, sort_keys=True)
            raise BootstrapError(
                f"Polaris {method} {path} returned HTTP {status}: {detail[:500]}"
            )
        return status, body


def get_or_none(api: PolarisApi, path: str, token: str) -> Any | None:
    status, body = api.json("GET", path, token, expected={200, 404})
    return None if status == 404 else body


def ensure_catalog(
    api: PolarisApi,
    token: str,
    warehouse_location: str,
    endpoint_external: str,
    endpoint_internal: str,
) -> None:
    path = f"/api/management/v1/catalogs/{quote(CATALOG_NAME, safe='')}"
    catalog = get_or_none(api, path, token)
    payload = {
        "catalog": {
            "name": CATALOG_NAME,
            "type": "INTERNAL",
            "properties": {"default-base-location": warehouse_location},
            "storageConfigInfo": {
                "storageType": "S3",
                "allowedLocations": [warehouse_location],
                "endpoint": endpoint_external,
                "endpointInternal": endpoint_internal,
                "pathStyleAccess": True,
            },
        }
    }
    if catalog is None:
        _, catalog = api.json(
            "POST", "/api/management/v1/catalogs", token, payload, {201}
        )
    storage = catalog.get("storageConfigInfo") or {}
    properties = catalog.get("properties") or {}
    actual = {
        "name": catalog.get("name"),
        "type": catalog.get("type"),
        "location": properties.get("default-base-location"),
        "storageType": storage.get("storageType"),
        "endpoint": storage.get("endpoint"),
        "endpointInternal": storage.get("endpointInternal"),
        "pathStyleAccess": storage.get("pathStyleAccess"),
        "allowed": sorted(storage.get("allowedLocations") or []),
    }
    expected = {
        "name": CATALOG_NAME,
        "type": "INTERNAL",
        "location": warehouse_location,
        "storageType": "S3",
        "endpoint": endpoint_external,
        "endpointInternal": endpoint_internal,
        "pathStyleAccess": True,
        "allowed": [warehouse_location],
    }
    if actual != expected:
        raise BootstrapError("existing Polaris catalog does not match the fixed BPI contract")


def ensure_named_resource(
    api: PolarisApi,
    token: str,
    get_path: str,
    create_path: str,
    payload: dict[str, Any],
) -> Any:
    resource = get_or_none(api, get_path, token)
    if resource is None:
        _, resource = api.json("POST", create_path, token, payload, {201})
    return resource


def _credential_parts(path: Path, label: str) -> tuple[str, str]:
    if path.stat().st_mode & (stat.S_IRWXG | stat.S_IRWXO):
        raise BootstrapError(f"{label} credential file must have mode 0600")
    raw = path.read_text(encoding="utf-8").strip()
    if "\n" in raw or raw.count(":") != 1:
        raise BootstrapError(f"{label} credential file has an invalid format")
    client_id, client_secret = raw.split(":", 1)
    if not client_id or not client_secret:
        raise BootstrapError(f"{label} credential file contains an empty value")
    return client_id, client_secret


def _write_credential(
    path: Path,
    client_id: str,
    client_secret: str,
    *,
    uid_environment: str,
    gid_environment: str,
    temporary_prefix: str,
) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with NamedTemporaryFile(
        "w",
        encoding="utf-8",
        dir=path.parent,
        prefix=temporary_prefix,
        delete=False,
    ) as handle:
        temporary = Path(handle.name)
        handle.write(f"{client_id}:{client_secret}\n")
        handle.flush()
        os.fchmod(handle.fileno(), 0o600)
    try:
        os.chown(
            temporary,
            int(os.getenv(uid_environment, "10002")),
            int(os.getenv(gid_environment, "10002")),
        )
        os.replace(temporary, path)
    finally:
        temporary.unlink(missing_ok=True)


def ensure_principal_contract(
    api: PolarisApi,
    admin_token: str,
    credential_path: Path,
    *,
    principal_name: str,
    rotation_environment: str,
    uid_environment: str,
    gid_environment: str,
    temporary_prefix: str,
    label: str,
) -> tuple[str, str]:
    principal_path = (
        f"/api/management/v1/principals/{quote(principal_name, safe='')}"
    )
    principal = get_or_none(api, principal_path, admin_token)
    if principal is None:
        _, created = api.json(
            "POST",
            "/api/management/v1/principals",
            admin_token,
            {
                "principal": {
                    "name": principal_name,
                    "properties": {"owner": "ft-mes-bpi"},
                },
                "credentialRotationRequired": False,
            },
            {201},
        )
        principal = created["principal"]
        credentials = created["credentials"]
        _write_credential(
            credential_path,
            str(credentials["clientId"]),
            str(credentials["clientSecret"]),
            uid_environment=uid_environment,
            gid_environment=gid_environment,
            temporary_prefix=temporary_prefix,
        )
    elif not credential_path.exists():
        if not enabled(rotation_environment):
            raise BootstrapError(
                f"{label} principal exists but its credential file is missing; "
                "explicit rotation is required"
            )
        _, rotated = api.json("POST", f"{principal_path}/rotate", admin_token, expected={200})
        credentials = rotated["credentials"]
        _write_credential(
            credential_path,
            str(credentials["clientId"]),
            str(credentials["clientSecret"]),
            uid_environment=uid_environment,
            gid_environment=gid_environment,
            temporary_prefix=temporary_prefix,
        )
        principal = rotated["principal"]
    client_id, client_secret = _credential_parts(credential_path, label)
    if principal.get("clientId") != client_id:
        raise BootstrapError(f"{label} credential client ID does not match Polaris")
    return client_id, client_secret


def ensure_principal(
    api: PolarisApi, admin_token: str, credential_path: Path
) -> tuple[str, str]:
    return ensure_principal_contract(
        api,
        admin_token,
        credential_path,
        principal_name=PUBLISHER_PRINCIPAL,
        rotation_environment="BPI_POLARIS_PUBLISHER_CREDENTIAL_ROTATION_ENABLED",
        uid_environment="BPI_POLARIS_PUBLISHER_CREDENTIAL_UID",
        gid_environment="BPI_POLARIS_PUBLISHER_CREDENTIAL_GID",
        temporary_prefix=".publisher-",
        label="publisher",
    )


def ensure_recovery_principal(
    api: PolarisApi, admin_token: str, credential_path: Path
) -> tuple[str, str]:
    return ensure_principal_contract(
        api,
        admin_token,
        credential_path,
        principal_name=RECOVERY_PRINCIPAL,
        rotation_environment="BPI_POLARIS_RECOVERY_CREDENTIAL_ROTATION_ENABLED",
        uid_environment="BPI_POLARIS_RECOVERY_CREDENTIAL_UID",
        gid_environment="BPI_POLARIS_RECOVERY_CREDENTIAL_GID",
        temporary_prefix=".recovery-",
        label="recovery",
    )


def ensure_role_contract(
    api: PolarisApi,
    token: str,
    *,
    principal_name: str,
    principal_role_name: str,
    catalog_role_name: str,
    expected_grants: frozenset[str],
    label: str,
) -> None:
    principal_role_path = (
        f"/api/management/v1/principal-roles/{quote(principal_role_name, safe='')}"
    )
    ensure_named_resource(
        api,
        token,
        principal_role_path,
        "/api/management/v1/principal-roles",
        {"principalRole": {"name": principal_role_name}},
    )
    assigned_path = (
        f"/api/management/v1/principals/{quote(principal_name, safe='')}"
        "/principal-roles"
    )
    _, assigned = api.json("GET", assigned_path, token)
    if principal_role_name not in {role["name"] for role in assigned["roles"]}:
        api.json(
            "PUT",
            assigned_path,
            token,
            {"principalRole": {"name": principal_role_name}},
            {201},
        )

    catalog_role_path = (
        f"/api/management/v1/catalogs/{CATALOG_NAME}/catalog-roles/"
        f"{quote(catalog_role_name, safe='')}"
    )
    ensure_named_resource(
        api,
        token,
        catalog_role_path,
        f"/api/management/v1/catalogs/{CATALOG_NAME}/catalog-roles",
        {"catalogRole": {"name": catalog_role_name}},
    )
    grants_path = f"{catalog_role_path}/grants"
    _, existing = api.json("GET", grants_path, token)
    actual = {
        grant["privilege"]
        for grant in existing["grants"]
        if grant.get("type") == "catalog"
    }
    non_catalog = [grant for grant in existing["grants"] if grant.get("type") != "catalog"]
    unexpected = actual - expected_grants
    if unexpected or non_catalog:
        raise BootstrapError(f"{label} catalog role contains unexpected privileges")
    for privilege in sorted(expected_grants - actual):
        api.json(
            "PUT",
            grants_path,
            token,
            {"grant": {"type": "catalog", "privilege": privilege}},
            {201},
        )
    mapping_path = (
        f"/api/management/v1/principal-roles/{quote(principal_role_name, safe='')}"
        f"/catalog-roles/{CATALOG_NAME}"
    )
    _, mapped = api.json("GET", mapping_path, token)
    if catalog_role_name not in {role["name"] for role in mapped["roles"]}:
        api.json(
            "PUT",
            mapping_path,
            token,
            {"catalogRole": {"name": catalog_role_name}},
            {201},
        )


def ensure_roles(api: PolarisApi, token: str) -> None:
    ensure_role_contract(
        api,
        token,
        principal_name=PUBLISHER_PRINCIPAL,
        principal_role_name=PUBLISHER_PRINCIPAL_ROLE,
        catalog_role_name=PUBLISHER_CATALOG_ROLE,
        expected_grants=PUBLISHER_GRANTS,
        label="publisher",
    )


def ensure_recovery_roles(api: PolarisApi, token: str) -> None:
    ensure_role_contract(
        api,
        token,
        principal_name=RECOVERY_PRINCIPAL,
        principal_role_name=RECOVERY_PRINCIPAL_ROLE,
        catalog_role_name=RECOVERY_CATALOG_ROLE,
        expected_grants=RECOVERY_GRANTS,
        label="recovery",
    )


def main() -> int:
    if not enabled("BPI_POLARIS_CATALOG_BOOTSTRAP_ENABLED"):
        print("BPI Polaris catalog bootstrap is disabled")
        return 0
    if not enabled("BPI_POLARIS_ENABLED"):
        raise BootstrapError("BPI_POLARIS_ENABLED must be true for catalog bootstrap")
    base_url = required("BPI_POLARIS_BASE_URL")
    realm = required("BPI_POLARIS_REALM")
    bucket = required("BPI_ICEBERG_WAREHOUSE_BUCKET")
    endpoint_external = required("BPI_ICEBERG_S3_ENDPOINT_EXTERNAL").rstrip("/")
    endpoint_internal = required("BPI_ICEBERG_S3_ENDPOINT_INTERNAL").rstrip("/")
    credential_path = Path(required("BPI_POLARIS_PUBLISHER_CREDENTIAL_FILE"))
    if not credential_path.is_absolute():
        raise BootstrapError("BPI_POLARIS_PUBLISHER_CREDENTIAL_FILE must be absolute")
    recovery_credential_path = Path(
        required("BPI_POLARIS_RECOVERY_CREDENTIAL_FILE")
    )
    if not recovery_credential_path.is_absolute():
        raise BootstrapError("BPI_POLARIS_RECOVERY_CREDENTIAL_FILE must be absolute")
    api = PolarisApi(base_url, realm)
    admin_token = api.token(
        required("BPI_POLARIS_BOOTSTRAP_CLIENT_ID"),
        required("BPI_POLARIS_BOOTSTRAP_CLIENT_SECRET"),
    )
    warehouse_location = f"s3://{bucket}/warehouse"
    ensure_catalog(
        api,
        admin_token,
        warehouse_location,
        endpoint_external,
        endpoint_internal,
    )
    client_id, client_secret = ensure_principal(api, admin_token, credential_path)
    ensure_roles(api, admin_token)
    recovery_client_id, recovery_client_secret = ensure_recovery_principal(
        api, admin_token, recovery_credential_path
    )
    ensure_recovery_roles(api, admin_token)
    api.token(client_id, client_secret)
    api.token(recovery_client_id, recovery_client_secret)
    print(
        "BPI Polaris catalog, publisher and isolated recovery roles: PASS"
    )
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except BootstrapError as exception:
        print(f"ERROR: {exception}", file=sys.stderr)
        raise SystemExit(1)
