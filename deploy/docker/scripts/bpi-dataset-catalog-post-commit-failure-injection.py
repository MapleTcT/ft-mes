#!/usr/bin/env python3
from __future__ import annotations

import json
import os
import sys
from pathlib import Path
from tempfile import TemporaryDirectory
from uuid import UUID

import psycopg
from psycopg.rows import dict_row

from bpi_dataset_catalog_publisher.catalog import IcebergCatalogPublisher
from bpi_dataset_catalog_publisher.config import Settings
from bpi_dataset_catalog_publisher.repository import CatalogPublicationRepository
from bpi_dataset_catalog_publisher.source_object import SourceObjectStore
from bpi_dataset_catalog_publisher.worker import sanitize_error


CONFIRMATION = "INJECT_AFTER_REAL_CATALOG_COMMIT_BEFORE_POSTGRES_FENCE"


def required(name: str) -> str:
    value = os.getenv(name, "").strip()
    if not value:
        raise RuntimeError(f"{name} is required")
    return value


def connect(settings: Settings) -> psycopg.Connection:
    if settings.database_url:
        return psycopg.connect(settings.database_url, row_factory=dict_row)
    return psycopg.connect(row_factory=dict_row)


def assert_single_target(
    settings: Settings,
    publication_id: UUID,
    dataset_code: str,
) -> None:
    with connect(settings) as connection:
        active = connection.execute(
            """
            SELECT publication.id, publication.state, definition.dataset_code
              FROM bpi.bpi_dataset_catalog_publications publication
              JOIN bpi.bpi_dataset_materializations materialization
                ON materialization.tenant_id = publication.tenant_id
               AND materialization.id = publication.materialization_id
              JOIN bpi.bpi_dataset_snapshots snapshot
                ON snapshot.tenant_id = publication.tenant_id
               AND snapshot.id = materialization.snapshot_id
              JOIN bpi.bpi_dataset_definitions definition
                ON definition.tenant_id = snapshot.tenant_id
               AND definition.id = snapshot.dataset_id
             WHERE publication.state IN ('QUEUED', 'COMMITTING', 'VERIFYING')
               AND publication.catalog_name = %s
               AND publication.publisher_version = %s
             ORDER BY publication.created_at, publication.id
            """,
            (settings.catalog_name, settings.publisher_version),
        ).fetchall()
    if len(active) != 1:
        raise RuntimeError(
            f"exactly one active publication is required, found {len(active)}"
        )
    target = active[0]
    if (
        target["state"] != "QUEUED"
        or target["id"] != publication_id
        or target["dataset_code"] != dataset_code
    ):
        raise RuntimeError("the sole active publication is not the explicit queued target")


def main() -> None:
    if required("BPI_CATALOG_POST_COMMIT_FAILURE_INJECTION_CONFIRM") != CONFIRMATION:
        raise RuntimeError("post-commit failure injection confirmation is invalid")
    publication_id = UUID(required("BPI_CATALOG_POST_COMMIT_PUBLICATION_ID"))
    dataset_code = required("BPI_CATALOG_POST_COMMIT_DATASET_CODE")
    settings = Settings.from_environment()
    if not settings.enabled:
        raise RuntimeError("catalog publisher must be enabled for failure injection")

    assert_single_target(settings, publication_id, dataset_code)
    repository = CatalogPublicationRepository(settings)
    source_store = SourceObjectStore(settings)
    catalog = IcebergCatalogPublisher(settings)
    repository.ping()
    catalog.ping()
    claim = repository.recover_and_claim()
    if claim is None or claim.id != publication_id or claim.dataset_code != dataset_code:
        raise RuntimeError("the publisher did not claim the explicit acceptance target")
    if not claim.source_facts_verified:
        raise RuntimeError("the claimed publication source facts are not verified")

    Path(settings.work_directory).mkdir(parents=True, exist_ok=True)
    with TemporaryDirectory(
        prefix=f"post-commit-injection-{claim.id}-",
        dir=settings.work_directory,
    ) as directory:
        source = source_store.download_verified(
            claim,
            Path(directory) / "source.parquet",
        )
        commit = catalog.ensure_commit(claim, source)

    evidence = {
        "status": "EXPECTED_PROCESS_EXIT_AFTER_REAL_CATALOG_COMMIT",
        "publicationId": str(claim.id),
        "datasetCode": claim.dataset_code,
        "claimRevision": claim.revision,
        "attemptCount": claim.attempt_count,
        "icebergSnapshotId": str(commit.snapshot_id),
        "metadataLocation": commit.metadata_location,
        "schemaId": commit.schema_id,
        "partitionSpecId": commit.partition_spec_id,
        "postgresFenceWritten": False,
        "expectedExitCode": 86,
    }
    sys.stdout.write(json.dumps(evidence, sort_keys=True) + "\n")
    sys.stdout.flush()
    os._exit(86)


if __name__ == "__main__":
    try:
        main()
    except Exception as exception:
        sys.stderr.write(sanitize_error(exception) + "\n")
        raise SystemExit(1) from exception
