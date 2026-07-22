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

from .config import ConfigurationError, Settings
from .health import HealthServer, HealthState
from .object_store import DatasetObjectStore, ObjectStoreContractError
from .parquet import DatasetContractError, build_parquet
from .repository import LostClaimError, MaterializationRepository


LOGGER = logging.getLogger("bpi-dataset-materializer")
SECRET_PATTERN = re.compile(
    r"(?i)(password|secret|access[_-]?key)(\s*[=:]\s*)[^\s,;]+|://[^/@\s]+:[^/@\s]+@"
)


def _now() -> str:
    return datetime.now(UTC).isoformat()


def sanitize_error(exception: BaseException) -> str:
    message = f"{type(exception).__name__}: {exception}"

    def redact(match: re.Match[str]) -> str:
        if match.group(1):
            return f"{match.group(1)}{match.group(2)}[REDACTED]"
        return "://[REDACTED]@"

    return " ".join(SECRET_PATTERN.sub(redact, message).split())[:900]


def failure_code(exception: BaseException) -> str:
    if isinstance(exception, DatasetContractError):
        return "DATASET_CONTRACT_VIOLATION"
    if isinstance(exception, ObjectStoreContractError):
        return "OBJECT_STORE_CONTRACT_VIOLATION"
    if isinstance(exception, S3Error):
        return "OBJECT_STORE_ERROR"
    if isinstance(exception, psycopg.Error):
        return "DATABASE_ERROR"
    if isinstance(exception, pyarrow.ArrowException):
        return "PARQUET_WRITE_ERROR"
    return "MATERIALIZATION_ERROR"


class MaterializerWorker:
    def __init__(self, settings: Settings, stop_event: threading.Event):
        self._settings = settings
        self._stop = stop_event
        self._health = HealthState(enabled=settings.enabled)
        self._server = HealthServer(settings.health_port, self._health)
        self._repository: MaterializationRepository | None = None
        self._object_store: DatasetObjectStore | None = None

    def run(self) -> None:
        self._server.start()
        try:
            if not self._settings.enabled:
                LOGGER.info("dataset materializer is disabled")
                self._health.cycle_succeeded()
                self._wait_until_stopped()
                return
            Path(self._settings.work_directory).mkdir(parents=True, exist_ok=True)
            self._repository = MaterializationRepository(self._settings)
            self._object_store = DatasetObjectStore(self._settings)
            while not self._stop.is_set():
                self._cycle()
                self._stop.wait(self._settings.poll_interval_seconds)
        finally:
            self._server.close()

    def _cycle(self) -> None:
        assert self._repository is not None
        assert self._object_store is not None
        try:
            self._repository.ping()
            self._object_store.validate_bucket()
            claim = self._repository.recover_and_claim()
            if claim is None:
                self._health.cycle_succeeded()
                return
            self._health.update(
                status="READY",
                last_cycle_at=_now(),
                active_materialization_id=str(claim.id),
            )
            try:
                samples = self._repository.load_samples(claim)
                with TemporaryDirectory(
                    prefix=f"{claim.id}-",
                    dir=self._settings.work_directory,
                ) as directory:
                    artifact = build_parquet(
                        claim,
                        samples,
                        Path(directory) / "dataset.parquet",
                    )
                    stored = self._object_store.ensure_uploaded(claim, artifact)
                    self._repository.complete(claim, artifact, stored)
                LOGGER.info(
                    "materialization ready id=%s rows=%s sha256=%s",
                    claim.id,
                    artifact.row_count,
                    artifact.content_sha256,
                )
                self._health.cycle_succeeded()
            except LostClaimError as exception:
                LOGGER.warning("materialization claim lost id=%s: %s", claim.id, exception)
                self._health.update(
                    status="DEGRADED",
                    last_cycle_at=_now(),
                    last_error="materialization claim was lost",
                    active_materialization_id=None,
                )
            except Exception as exception:
                detail = sanitize_error(exception)
                code = failure_code(exception)
                recorded = self._repository.fail(claim, code, detail)
                LOGGER.exception(
                    "materialization failed id=%s code=%s recorded=%s",
                    claim.id,
                    code,
                    recorded,
                )
                self._health.update(
                    status="DEGRADED",
                    last_cycle_at=_now(),
                    last_error=f"{code}: {detail}",
                    active_materialization_id=None,
                )
        except Exception as exception:
            detail = sanitize_error(exception)
            LOGGER.exception("materializer cycle failed: %s", detail)
            self._health.update(
                status="DEGRADED",
                last_cycle_at=_now(),
                last_error=detail,
                active_materialization_id=None,
            )

    def _wait_until_stopped(self) -> None:
        while not self._stop.wait(60):
            self._health.cycle_succeeded()


def main() -> None:
    logging.basicConfig(
        level=os.getenv("BPI_DATASET_MATERIALIZER_LOG_LEVEL", "INFO").upper(),
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
    )
    try:
        settings = Settings.from_environment()
    except ConfigurationError as exception:
        raise SystemExit(f"invalid materializer configuration: {exception}") from exception

    stop_event = threading.Event()

    def stop(_signum, _frame) -> None:
        stop_event.set()

    signal.signal(signal.SIGTERM, stop)
    signal.signal(signal.SIGINT, stop)
    MaterializerWorker(settings, stop_event).run()


if __name__ == "__main__":
    main()
