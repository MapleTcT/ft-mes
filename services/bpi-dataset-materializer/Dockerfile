ARG PYTHON_IMAGE=python:3.12.13-slim-bookworm
FROM ${PYTHON_IMAGE}

USER root

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

WORKDIR /app

COPY services/bpi-dataset-materializer/requirements.runtime.txt ./
RUN --mount=type=cache,target=/root/.cache/pip \
    pip install --requirement requirements.runtime.txt

COPY services/bpi-dataset-materializer/pyproject.toml ./
COPY services/bpi-dataset-materializer/README.md ./
COPY services/bpi-dataset-materializer/src ./src

RUN --mount=type=cache,target=/root/.cache/pip \
    pip install --no-build-isolation --no-deps . \
    && (getent group bpi >/dev/null 2>&1 \
        || addgroup --system --gid 10001 bpi) \
    && (id -u bpi >/dev/null 2>&1 \
        || adduser --system --uid 10001 --ingroup bpi --home /nonexistent bpi) \
    && mkdir -p /var/lib/bpi-materializer \
    && chown -R bpi:bpi /var/lib/bpi-materializer

USER 10001:10001

EXPOSE 19093

HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=6 \
  CMD python -c "import os,urllib.request; urllib.request.urlopen('http://127.0.0.1:'+os.getenv('BPI_DATASET_MATERIALIZER_HEALTH_PORT','19093')+'/health', timeout=2)"

ENTRYPOINT ["bpi-dataset-materializer"]
