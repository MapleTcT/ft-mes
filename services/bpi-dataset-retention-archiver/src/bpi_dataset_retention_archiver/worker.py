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
from minio.error import S3Error

from bpi_dataset_catalog_publisher.source_object import SourceObjectContractError

from .archive_store import RetentionArchiveContractError, RetentionArchiveStore
from .config import ConfigurationError, Settings
from .health import HealthServer, HealthState
from .repository import LostClaimError, RetentionArchiveRepository


LOGGER = logging.getLogger("bpi-dataset-retention-archiver")
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
    if isinstance(exception, RetentionArchiveContractError):
        return "RETENTION_ARCHIVE_CONTRACT_VIOLATION"
    if isinstance(exception, S3Error):
        return "OBJECT_LOCK_STORE_ERROR"
    if isinstance(exception, psycopg.Error):
        return "DATABASE_ERROR"
    if isinstance(exception, pyarrow.ArrowException):
        return "RECOVERY_DATA_ERROR"
    return "RETENTION_ARCHIVE_ERROR"


class RetentionArchiverWorker:
    def __init__(self, settings: Settings, stop_event: threading.Event):
        self._settings = settings
        self._stop = stop_event
        self._health = HealthState(enabled=settings.enabled)
        self._server = HealthServer(settings.health_port, self._health)
        self._repository: RetentionArchiveRepository | None = None
        self._store: RetentionArchiveStore | None = None

    def run(self) -> None:
        self._server.start()
        try:
            if not self._settings.enabled:
                LOGGER.info("dataset retention archiver is disabled")
                self._health.cycle_succeeded()
                self._wait_until_stopped()
                return
            Path(self._settings.work_directory).mkdir(parents=True, exist_ok=True)
            self._repository = RetentionArchiveRepository(self._settings)
            self._store = RetentionArchiveStore(self._settings)
            while not self._stop.is_set():
                self._cycle()
                self._stop.wait(self._settings.poll_interval_seconds)
        finally:
            self._server.close()

    def _cycle(self) -> None:
        assert self._repository is not None
        assert self._store is not None
        try:
            self._repository.ping()
            self._store.ping()
            claim = self._repository.recover_and_claim()
            if claim is None:
                self._health.cycle_succeeded()
                return
            self._health.update(
                status="READY",
                last_cycle_at=_now(),
                active_archive_id=str(claim.id),
            )
            try:
                if not claim.source_facts_verified:
                    raise SourceObjectContractError(
                        "frozen archive facts do not match READY source and Iceberg publication"
                    )
                if claim.retain_until <= datetime.now(UTC):
                    raise RetentionArchiveContractError(
                        "immutable retain-until timestamp has already expired"
                    )
                with TemporaryDirectory(
                    prefix=f"{claim.id}-",
                    dir=self._settings.work_directory,
                ) as directory:
                    root = Path(directory)
                    source = self._store.download_source(
                        claim, root / "source.parquet"
                    )
                    if source.semantic_checksum != claim.catalog_semantic_checksum:
                        raise RetentionArchiveContractError(
                            "source semantic checksum no longer matches Iceberg verification"
                        )
                    bundle = self._store.ensure_bundle(claim, source.path)
                    self._repository.mark_verifying(claim, bundle)
                    verification = self._store.verify(
                        claim, bundle, root / "recovered-source.parquet"
                    )
                    self._repository.complete(claim, verification)
                LOGGER.info(
                    "retention archive locked id=%s rows=%s checksum=%s sourceVersion=%s manifestVersion=%s",
                    claim.id,
                    verification.row_count,
                    verification.semantic_checksum,
                    bundle.source.version_id,
                    bundle.manifest.version_id,
                )
                self._health.cycle_succeeded()
            except LostClaimError as exception:
                LOGGER.warning(
                    "retention archive claim lost id=%s: %s", claim.id, exception
                )
                self._health.update(
                    status="DEGRADED",
                    last_cycle_at=_now(),
                    last_error="retention archive claim was lost",
                    active_archive_id=None,
                )
            except Exception as exception:
                detail = sanitize_error(exception)
                code = failure_code(exception)
                recorded = self._repository.fail(claim, code, detail)
                LOGGER.error(
                    "retention archive failed id=%s code=%s recorded=%s detail=%s",
                    claim.id,
                    code,
                    recorded,
                    detail,
                )
                self._health.update(
                    status="DEGRADED",
                    last_cycle_at=_now(),
                    last_error=f"{code}: {detail}",
                    active_archive_id=None,
                )
        except Exception as exception:
            detail = sanitize_error(exception)
            LOGGER.error("retention archiver cycle failed: %s", detail)
            self._health.update(
                status="DEGRADED",
                last_cycle_at=_now(),
                last_error=detail,
                active_archive_id=None,
            )

    def _wait_until_stopped(self) -> None:
        while not self._stop.wait(60):
            self._health.cycle_succeeded()


def main() -> None:
    logging.basicConfig(
        level=os.getenv(
            "BPI_DATASET_RETENTION_ARCHIVER_LOG_LEVEL", "INFO"
        ).upper(),
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
    )
    try:
        settings = Settings.from_environment()
    except ConfigurationError as exception:
        raise SystemExit(
            f"invalid retention archiver configuration: {exception}"
        ) from exception

    stop_event = threading.Event()

    def stop(_signum, _frame) -> None:
        stop_event.set()

    signal.signal(signal.SIGTERM, stop)
    signal.signal(signal.SIGINT, stop)
    RetentionArchiverWorker(settings, stop_event).run()


if __name__ == "__main__":
    main()
