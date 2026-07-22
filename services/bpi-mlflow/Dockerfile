ARG MLFLOW_IMAGE=m.daocloud.io/docker.io/library/python:3.12.13-slim-bookworm
FROM ${MLFLOW_IMAGE}

ARG PIP_INDEX_URL=https://pypi.org/simple
ARG PIP_DEFAULT_TIMEOUT=120

USER root

ENV PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PIP_INDEX_URL=${PIP_INDEX_URL} \
    PIP_DEFAULT_TIMEOUT=${PIP_DEFAULT_TIMEOUT}

COPY services/bpi-mlflow/requirements.runtime.txt /tmp/requirements.runtime.txt
RUN pip install --requirement /tmp/requirements.runtime.txt

COPY services/bpi-mlflow/start-mlflow.sh /usr/local/bin/start-bpi-mlflow
RUN chmod 0555 /usr/local/bin/start-bpi-mlflow \
    && (getent group 10004 >/dev/null 2>&1 \
        || addgroup --system --gid 10004 bpi_mlflow_server) \
    && (getent passwd 10004 >/dev/null 2>&1 \
        || adduser --system --uid 10004 \
            --ingroup "$(getent group 10004 | cut -d: -f1)" \
            --home /nonexistent bpi_mlflow_server)

ENV HOME=/tmp

USER 10004:10004

EXPOSE 5000

HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=12 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:5000/health', timeout=2)"

ENTRYPOINT ["/usr/local/bin/start-bpi-mlflow"]
