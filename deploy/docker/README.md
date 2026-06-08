# ADP/MES Docker Test Deployment

This directory runs the recovered Windows ADP/MES package on Linux with Docker Compose.

## Remote Layout

Expected server path:

```text
/home/v6/adp-mes-docker/
  deploy/docker/            # this directory
  runtime/bap-server/       # copied from the original Windows package
  runtime/logs/             # Java service logs
```

## Services

- PostgreSQL, Redis, MongoDB, MinIO and Nginx images from `.env`
- bundled ZooKeeper, Kafka, Nacos 1.2.1 and Keycloak 10.0.1 from `bap-server/assembly`
- Nginx frontend on `${ADP_HTTP_PORT:-18080}`
- ADP gateway on `${ADP_GATEWAY_PORT:-18008}`
- 23 Java services from `bap-server/base-Server`

## Commands

```bash
cd /home/v6/adp-mes-docker/deploy/docker
cp .env.example .env
python3 scripts/render-nacos-configs.py
docker compose --env-file .env up -d postgres redis mongo zookeeper kafka nacos keycloak minio
docker compose --env-file .env run --rm nacos-config
python3 scripts/patch-postgres-runtime.py \
  --base-server /home/v6/adp-mes-docker/runtime/bap-server/base-Server \
  --report /home/v6/adp-mes-docker/runtime/postgres-patch-report.json
docker compose --env-file .env up -d
docker compose --env-file .env ps
```

Open:

```text
http://10.11.100.17:18080/
```

## PostgreSQL Note

The Docker profile uses PostgreSQL by default, adds an external PostgreSQL JDBC driver to each Java service classpath, and provides `patch-postgres-runtime.py` to inject PostgreSQL DBP classes and generated `postgresql` mapper directories into nested runtime JARs. The mapper generation is mechanical and records risky SQL patterns in `runtime/postgres-patch-report.json`; any remaining failures should be fixed from that report and container logs.
