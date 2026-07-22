from __future__ import annotations

import json
import threading
from dataclasses import dataclass, field
from datetime import UTC, datetime
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from typing import Any


def _now() -> str:
    return datetime.now(UTC).isoformat()


@dataclass
class HealthState:
    enabled: bool
    status: str = "STARTING"
    last_cycle_at: str | None = None
    last_success_at: str | None = None
    last_error: str | None = None
    active_registration_id: str | None = None
    _lock: threading.Lock = field(default_factory=threading.Lock, repr=False)

    def update(self, **values: Any) -> None:
        with self._lock:
            for name, value in values.items():
                setattr(self, name, value)

    def snapshot(self) -> dict[str, Any]:
        with self._lock:
            return {
                "service": "bpi-dataset-mlflow-registrar",
                "enabled": self.enabled,
                "status": self.status,
                "lastCycleAt": self.last_cycle_at,
                "lastSuccessAt": self.last_success_at,
                "lastError": self.last_error,
                "activeRegistrationId": self.active_registration_id,
            }

    def cycle_succeeded(self) -> None:
        now = _now()
        self.update(
            status="READY" if self.enabled else "DISABLED",
            last_cycle_at=now,
            last_success_at=now,
            last_error=None,
            active_registration_id=None,
        )


class HealthServer:
    def __init__(self, port: int, state: HealthState):
        state_ref = state

        class Handler(BaseHTTPRequestHandler):
            def do_GET(self) -> None:  # noqa: N802
                if self.path != "/health":
                    self.send_error(404)
                    return
                payload = state_ref.snapshot()
                status = 503 if payload["status"] == "DEGRADED" else 200
                body = json.dumps(payload, separators=(",", ":")).encode()
                self.send_response(status)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)

            def log_message(self, _format: str, *_args: Any) -> None:
                return

        self._server = ThreadingHTTPServer(("0.0.0.0", port), Handler)
        self._thread = threading.Thread(
            target=self._server.serve_forever,
            name="bpi-mlflow-registrar-health",
            daemon=True,
        )

    def start(self) -> None:
        self._thread.start()

    def close(self) -> None:
        self._server.shutdown()
        self._server.server_close()
