ARG PYTHON_IMAGE=python:3.12.13-slim-bookworm
FROM ${PYTHON_IMAGE}

ARG PIP_INDEX_URL=https://pypi.org/simple
ARG PIP_DEFAULT_TIMEOUT=120

USER root

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PIP_INDEX_URL=${PIP_INDEX_URL} \
    PIP_DEFAULT_TIMEOUT=${PIP_DEFAULT_TIMEOUT}

WORKDIR /app

COPY services/bpi-dataset-retention-archiver/requirements.runtime.txt ./
RUN pip install --requirement requirements.runtime.txt

COPY services/bpi-dataset-catalog-publisher/pyproject.toml /tmp/catalog-source/
COPY services/bpi-dataset-catalog-publisher/README.md /tmp/catalog-source/
COPY services/bpi-dataset-catalog-publisher/src /tmp/catalog-source/src
RUN pip install --no-build-isolation --no-deps /tmp/catalog-source

COPY services/bpi-dataset-retention-archiver/pyproject.toml ./
COPY services/bpi-dataset-retention-archiver/README.md ./
COPY services/bpi-dataset-retention-archiver/src ./src

RUN pip install --no-build-isolation --no-deps . \
    && (getent group 10003 >/dev/null 2>&1 \
        || addgroup --system --gid 10003 bpi_retention) \
    && (getent passwd 10003 >/dev/null 2>&1 \
        || adduser --system --uid 10003 \
            --ingroup "$(getent group 10003 | cut -d: -f1)" \
            --home /nonexistent bpi_retention) \
    && mkdir -p /var/lib/bpi-retention-archiver \
    && chown -R 10003:10003 /var/lib/bpi-retention-archiver

USER 10003:10003

EXPOSE 19095

HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=6 \
  CMD python -c "import os,urllib.request; urllib.request.urlopen('http://127.0.0.1:'+os.getenv('BPI_DATASET_RETENTION_ARCHIVER_HEALTH_PORT','19095')+'/health', timeout=2)"

ENTRYPOINT ["bpi-dataset-retention-archiver"]
