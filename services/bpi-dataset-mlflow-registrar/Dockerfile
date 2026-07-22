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

COPY services/bpi-dataset-mlflow-registrar/requirements.runtime.txt ./
RUN pip install --requirement requirements.runtime.txt

COPY services/bpi-dataset-mlflow-registrar/pyproject.toml ./
COPY services/bpi-dataset-mlflow-registrar/README.md ./
COPY services/bpi-dataset-mlflow-registrar/src ./src

RUN pip install --no-build-isolation --no-deps . \
    && (getent group 10005 >/dev/null 2>&1 \
        || addgroup --system --gid 10005 bpi_mlflow) \
    && (getent passwd 10005 >/dev/null 2>&1 \
        || adduser --system --uid 10005 \
            --ingroup "$(getent group 10005 | cut -d: -f1)" \
            --home /nonexistent bpi_mlflow)

USER 10005:10005

EXPOSE 19096

HEALTHCHECK --interval=10s --timeout=3s --start-period=20s --retries=6 \
  CMD python -c "import os,urllib.request; urllib.request.urlopen('http://127.0.0.1:'+os.getenv('BPI_DATASET_MLFLOW_REGISTRAR_HEALTH_PORT','19096')+'/health', timeout=2)"

ENTRYPOINT ["bpi-dataset-mlflow-registrar"]
