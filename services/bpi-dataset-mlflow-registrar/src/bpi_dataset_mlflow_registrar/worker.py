from __future__ import annotations

import logging
import os
import signal
import threading
from datetime import UTC, datetime
from typing import TYPE_CHECKING

from .config import ConfigurationError, Settings
from .errors import LostClaimError, failure_code, sanitize_error
from .health import HealthServer, HealthState
from .mlflow_client import MlflowContractError, MlflowTrackingClient

if TYPE_CHECKING:
    from .repository import MlflowRegistrationRepository


LOGGER = logging.getLogger("bpi-dataset-mlflow-registrar")
def _now() -> str:
    return datetime.now(UTC).isoformat()


class MlflowRegistrarWorker:
    def __init__(self, settings: Settings, stop_event: threading.Event):
        self._settings = settings
        self._stop = stop_event
        self._health = HealthState(enabled=settings.enabled)
        self._server = HealthServer(settings.health_port, self._health)
        self._repository: MlflowRegistrationRepository | None = None
        self._client: MlflowTrackingClient | None = None

    def run(self) -> None:
        self._server.start()
        try:
            if not self._settings.enabled:
                LOGGER.info("dataset MLflow registrar is disabled")
                self._health.cycle_succeeded()
                self._wait_until_stopped()
                return
            assert self._settings.tracking_uri is not None
            from .repository import MlflowRegistrationRepository

            self._repository = MlflowRegistrationRepository(self._settings)
            self._client = MlflowTrackingClient(
                self._settings.tracking_uri,
                self._settings.request_timeout_seconds,
                self._settings.tracking_token,
            )
            while not self._stop.is_set():
                self._cycle()
                self._stop.wait(self._settings.poll_interval_seconds)
        finally:
            self._server.close()

    def _cycle(self) -> None:
        assert self._repository is not None
        assert self._client is not None
        try:
            self._repository.ping()
            claim = self._repository.recover_and_claim()
            if claim is None:
                self._client.ping()
                self._health.cycle_succeeded()
                return
            self._health.update(
                status="READY",
                last_cycle_at=_now(),
                active_registration_id=str(claim.id),
            )
            try:
                if not claim.source_facts_verified:
                    raise MlflowContractError(
                        "frozen registration facts do not match the LOCKED recovery archive"
                    )
                self._client.ping()
                result = self._client.register(claim)
                self._repository.complete(claim, result)
                LOGGER.info(
                    "MLflow dataset registered id=%s experiment=%s run=%s digest=%s",
                    claim.id,
                    result.experiment_id,
                    result.run_id,
                    claim.dataset_digest,
                )
                self._health.cycle_succeeded()
            except LostClaimError as exception:
                LOGGER.warning(
                    "MLflow registration claim lost id=%s: %s", claim.id, exception
                )
                self._health.update(
                    status="DEGRADED",
                    last_cycle_at=_now(),
                    last_error="MLflow registration claim was lost",
                    active_registration_id=None,
                )
            except Exception as exception:
                detail = sanitize_error(exception)
                code = failure_code(exception)
                recorded = self._repository.fail(claim, code, detail)
                LOGGER.error(
                    "MLflow registration failed id=%s code=%s recorded=%s detail=%s",
                    claim.id,
                    code,
                    recorded,
                    detail,
                )
                self._health.update(
                    status="DEGRADED",
                    last_cycle_at=_now(),
                    last_error=f"{code}: {detail}",
                    active_registration_id=None,
                )
        except Exception as exception:
            detail = sanitize_error(exception)
            LOGGER.error("MLflow registrar cycle failed: %s", detail)
            self._health.update(
                status="DEGRADED",
                last_cycle_at=_now(),
                last_error=detail,
                active_registration_id=None,
            )

    def _wait_until_stopped(self) -> None:
        while not self._stop.wait(60):
            self._health.cycle_succeeded()


def main() -> None:
    logging.basicConfig(
        level=os.getenv("BPI_DATASET_MLFLOW_REGISTRAR_LOG_LEVEL", "INFO").upper(),
        format="%(asctime)s %(levelname)s %(name)s %(message)s",
    )
    try:
        settings = Settings.from_environment()
    except ConfigurationError as exception:
        raise SystemExit(
            f"invalid MLflow registrar configuration: {exception}"
        ) from exception

    stop_event = threading.Event()

    def stop(_signum, _frame) -> None:
        stop_event.set()

    signal.signal(signal.SIGTERM, stop)
    signal.signal(signal.SIGINT, stop)
    MlflowRegistrarWorker(settings, stop_event).run()


if __name__ == "__main__":
    main()
