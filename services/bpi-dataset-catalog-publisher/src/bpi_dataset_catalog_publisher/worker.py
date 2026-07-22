from __future__ import annotations

import logging
import os
import re
import signal
import threading
from datetime import UTC, datetime
from pathlib import Path
from tempfile import TemporaryDirectory

import psycopg
import pyarrow
import requests
from minio.error import S3Error

from .catalog import CatalogContractError, IcebergCatalogPublisher
from .config import ConfigurationError, Settings
from .health import HealthServer, HealthState
from .repository import CatalogPublicationRepository, LostClaimError
from .source_object import SourceObjectContractError, SourceObjectStore


LOGGER = logging.getLogger("bpi-dataset-catalog-publisher")
SECRET_PATTERN = re.compile(
    r"(?i)(?P<authorization>authorization)(?P<authorization_sep>\s*[=:]\s*)"
    r"(?:bearer\s+)?[^\s,;]+|"
    r"(?P<key>[a-z0-9_-]*(?:password|secret|access[_-]?key|api[_-]?key|credential|token)"
    r"[a-z0-9_-]*)(?P<key_sep>\s*[=:]\s*)[^\s,;]+|"
    r"\bbearer\s+[^\s,;]+|://[^/@\s]+:[^/@\s]+@"
)


def _now() -> str:
    return datetime.now(UTC).isoformat()


def sanitize_error(exception: BaseException) -> str:
    message = f"{type(exception).__name__}: {exception}"

    def redact(match: re.Match[str]) -> str:
        if match.group("authorization"):
            return (
                f"{match.group('authorization')}"
                f"{match.group('authorization_sep')}[REDACTED]"
            )
        if match.group("key"):
            return f"{match.group('key')}{match.group('key_sep')}[REDACTED]"
        if match.group(0).lower().startswith("bearer"):
            return "Bearer [REDACTED]"
        return "://[REDACTED]@"

    return " ".join(SECRET_PATTERN.sub(redact, message).split())[:900]


def failure_code(exception: BaseException) -> str:
    if isinstance(exception, SourceObjectContractError):
        return "SOURCE_OBJECT_CONTRACT_VIOLATION"
    if isinstance(exception, CatalogContractError):
        return "ICEBERG_CATALOG_CONTRACT_VIOLATION"
    if isinstance(exception, S3Error):
        return "SOURCE_OBJECT_ERROR"
    if isinstance(exception, requests.RequestException):
        return "POLARIS_UNAVAILABLE"
    if isinstance(exception, psycopg.Error):
        return "DATABASE_ERROR"
    if isinstance(exception, pyarrow.ArrowException):
        return "ICEBERG_DATA_ERROR"
    return "CATALOG_PUBLICATION_ERROR"


class CatalogPublisherWorker:
    def __init__(self, settings: Settings, stop_event: threading.Event):
        self._settings = settings
        self._stop = stop_event
        self._health = HealthState(enabled=settings.enabled)
        self._server = HealthServer(settings.health_port, self._health)
        self._repository: CatalogPublicationRepository | None = None
        self._source_store: SourceObjectStore | None = None
        self._catalog: IcebergCatalogPublisher | None = None

    def run(self) -> None:
        self._server.start()
        try:
            if not self._settings.enabled:
                LOGGER.info("dataset catalog publisher is disabled")
                self._health.cycle_succeeded()
                self._wait_until_stopped()
                return
            Path(self._settings.work_directory).mkdir(parents=True, exist_ok=True)
            self._repository = CatalogPublicationRepository(self._settings)
            self._source_store = SourceObjectStore(self._settings)
            self._catalog = IcebergCatalogPublisher(self._settings)
            while not self._stop.is_set():
                self._cycle()
                self._stop.wait(self._settings.poll_interval_seconds)
        finally:
            self._server.close()

    def _cycle(self) -> None:
        assert self._repository is not None
        assert self._source_store is not None
        assert self._catalog is not None
        try:
            self._repository.ping()
            self._catalog.ping()
            claim = self._repository.recover_and_claim()
            if claim is None:
                self._health.cycle_succeeded()
                return
            self._health.update(
                status="READY",
                last_cycle_at=_now(),
                active_publication_id=str(claim.id),
            )
            try:
                if not claim.source_facts_verified:
                    raise SourceObjectContractError(
                        "frozen publication facts do not match the READY materialization"
                    )
                with TemporaryDirectory(
                    prefix=f"{claim.id}-",
                    dir=self._settings.work_directory,
                ) as directory:
                    source = self._source_store.download_verified(
                        claim,
                        Path(directory) / "source.parquet",
                    )
                    commit = self._catalog.ensure_commit(claim, source)
                    self._repository.mark_verifying(claim, commit)
                    verification = self._catalog.verify(claim, source, commit)
                    self._repository.complete(claim, verification)
                LOGGER.info(
                    "catalog publication ready id=%s snapshot=%s rows=%s sha256=%s",
                    claim.id,
                    commit.snapshot_id,
                    verification.row_count,
                    verification.semantic_checksum,
                )
                self._health.cycle_succeeded()
            except LostClaimError as exception:
                LOGGER.warning("catalog publication claim lost id=%s: %s", claim.id, exception)
                self._health.update(
                    status="DEGRADED",
                    last_cycle_at=_now(),
                    last_error="catalog publication claim was lost",
                    active_publication_id=None,
                )
            except Exception as exception:
                detail = sanitize_error(exception)
                code = failure_code(exception)
                recorded = self._repository.fail(claim, code, detail)
                LOGGER.error(
                    "catalog publication failed id=%s code=%s recorded=%s detail=%s",
                    claim.id,
                    code,
                    recorded,
                    detail,
                )
                self._health.update(
                    status="DEGRADED",
                    last_cycle_at=_now(),
                    last_error=f"{code}: {detail}",
                    active_publication_id=None,
                )
        except Exception as exception:
            detail = sanitize_error(exception)
            LOGGER.error("catalog publisher cycle failed: %s", detail)
            self._health.update(
                status="DEGRADED",
                last_cycle_at=_now(),
                last_error=detail,
                active_publication_id=None,
            )

    def _wait_until_stopped(self) -> None:
        while not self._stop.wait(60):
            self._health.cycle_succeeded()


def main() -> None:
    logging.basicConfig(
        level=os.getenv(
            "BPI_DATASET_CATALOG_PUBLISHER_LOG_LEVEL", "INFO"
        ).upper(),
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
    )
    try:
        settings = Settings.from_environment()
    except ConfigurationError as exception:
        raise SystemExit(
            f"invalid catalog publisher configuration: {exception}"
        ) from exception

    stop_event = threading.Event()

    def stop(_signum, _frame) -> None:
        stop_event.set()

    signal.signal(signal.SIGTERM, stop)
    signal.signal(signal.SIGINT, stop)
    CatalogPublisherWorker(settings, stop_event).run()


if __name__ == "__main__":
    main()
