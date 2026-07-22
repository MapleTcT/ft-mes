ARG PYTHON_IMAGE=python:3.12.13-slim-bookworm
FROM ${PYTHON_IMAGE}

USER root

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

WORKDIR /app

COPY services/bpi-dataset-catalog-publisher/requirements.runtime.txt ./
RUN pip install --requirement requirements.runtime.txt

COPY services/bpi-dataset-catalog-publisher/pyproject.toml ./
COPY services/bpi-dataset-catalog-publisher/README.md ./
COPY services/bpi-dataset-catalog-publisher/src ./src

RUN pip install --no-build-isolation --no-deps . \
    && (getent group bpi >/dev/null 2>&1 \
        || addgroup --system --gid 10002 bpi) \
    && (id -u bpi >/dev/null 2>&1 \
        || adduser --system --uid 10002 --ingroup bpi --home /nonexistent bpi) \
    && mkdir -p /var/lib/bpi-catalog-publisher \
    && chown -R bpi:bpi /var/lib/bpi-catalog-publisher

USER 10002:10002

EXPOSE 19094

HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=6 \
  CMD python -c "import os,urllib.request; urllib.request.urlopen('http://127.0.0.1:'+os.getenv('BPI_DATASET_CATALOG_PUBLISHER_HEALTH_PORT','19094')+'/health', timeout=2)"

ENTRYPOINT ["bpi-dataset-catalog-publisher"]
