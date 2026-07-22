from __future__ import annotations

import importlib.util
import unittest
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[2]
MODULE_PATH = ROOT / "deploy/polaris/bootstrap_bpi_catalog.py"
SPEC = importlib.util.spec_from_file_location("bootstrap_bpi_catalog", MODULE_PATH)
assert SPEC is not None and SPEC.loader is not None
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class FakePolarisApi:
    def __init__(self, catalog: dict[str, Any] | None) -> None:
        self.catalog = catalog
        self.calls: list[tuple[str, str, dict[str, Any] | None]] = []

    def json(
        self,
        method: str,
        path: str,
        token: str,
        payload: dict[str, Any] | None = None,
        expected: set[int] | None = None,
    ) -> tuple[int, Any]:
        self.calls.append((method, path, payload))
        if method == "GET":
            return (404, None) if self.catalog is None else (200, self.catalog)
        if method == "POST" and payload is not None:
            self.catalog = payload["catalog"]
            return 201, self.catalog
        raise AssertionError(f"unexpected fake Polaris call: {method} {path}")


def catalog_response(endpoint: str = "http://minio:30200") -> dict[str, Any]:
    return {
        "type": "INTERNAL",
        "name": "ft_mes_bpi",
        "properties": {
            "default-base-location": "s3://bpi-iceberg-warehouse/warehouse"
        },
        "storageConfigInfo": {
            "allowedKmsKeys": [],
            "endpoint": endpoint,
            "endpointInternal": "http://minio:30200",
            "pathStyleAccess": True,
            "storageType": "S3",
            "allowedLocations": ["s3://bpi-iceberg-warehouse/warehouse"],
        },
    }


class CatalogBootstrapContractTest(unittest.TestCase):
    def ensure(self, api: FakePolarisApi) -> None:
        MODULE.ensure_catalog(
            api,
            "admin-token",
            "s3://bpi-iceberg-warehouse/warehouse",
            "http://minio:30200",
            "http://minio:30200",
        )

    def test_accepts_polaris_1_4_response_without_read_only(self) -> None:
        api = FakePolarisApi(catalog_response())

        self.ensure(api)

        self.assertEqual(["GET"], [call[0] for call in api.calls])

    def test_create_payload_uses_only_supported_catalog_fields(self) -> None:
        api = FakePolarisApi(None)

        self.ensure(api)

        payload = api.calls[1][2]
        assert payload is not None
        self.assertNotIn("readOnly", payload["catalog"])

    def test_rejects_storage_endpoint_drift(self) -> None:
        api = FakePolarisApi(catalog_response("http://unexpected-minio:9000"))

        with self.assertRaisesRegex(
            MODULE.BootstrapError, "does not match the fixed BPI contract"
        ):
            self.ensure(api)


if __name__ == "__main__":
    unittest.main()
