from __future__ import annotations

import re

from .mlflow_client import MlflowContractError, MlflowTransportError


SECRET_PATTERN = re.compile(
    r"(?i)(?P<authorization>authorization)(?P<authorization_sep>\s*[=:]\s*)"
    r"(?:bearer\s+)?[^\s,;]+|"
    r"(?P<key>[a-z0-9_-]*(?:password|secret|access[_-]?key|api[_-]?key|credential|token)"
    r"[a-z0-9_-]*)(?P<key_sep>\s*[=:]\s*)[^\s,;]+|"
    r"\bbearer\s+[^\s,;]+|://[^/@\s]+:[^/@\s]+@"
)


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
    if isinstance(exception, MlflowContractError):
        return "MLFLOW_CONTRACT_VIOLATION"
    if isinstance(exception, MlflowTransportError):
        return "MLFLOW_TRANSPORT_ERROR"
    if exception.__class__.__module__.split(".", 1)[0] == "psycopg":
        return "DATABASE_ERROR"
    return "MLFLOW_REGISTRATION_ERROR"
