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

COPY services/bpi-dataset-catalog-publisher/requirements.runtime.txt ./
RUN pip install --requirement requirements.runtime.txt

COPY services/bpi-dataset-catalog-publisher/pyproject.toml ./
COPY services/bpi-dataset-catalog-publisher/README.md ./
COPY services/bpi-dataset-catalog-publisher/src ./src

RUN pip install --no-build-isolation --no-deps . \
    && (getent group 10002 >/dev/null 2>&1 \
        || addgroup --system --gid 10002 bpi_catalog) \
    && (getent passwd 10002 >/dev/null 2>&1 \
        || adduser --system --uid 10002 \
            --ingroup "$(getent group 10002 | cut -d: -f1)" \
            --home /nonexistent bpi_catalog) \
    && mkdir -p /var/lib/bpi-catalog-publisher \
    && chown -R 10002:10002 /var/lib/bpi-catalog-publisher

USER 10002:10002

EXPOSE 19094

HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=6 \
  CMD python -c "import os,urllib.request; urllib.request.urlopen('http://127.0.0.1:'+os.getenv('BPI_DATASET_CATALOG_PUBLISHER_HEALTH_PORT','19094')+'/health', timeout=2)"

ENTRYPOINT ["bpi-dataset-catalog-publisher"]
