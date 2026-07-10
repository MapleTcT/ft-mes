# material-wms

PostgreSQL-first inventory service that restores the WOM material dependency under Nacos service name `material`.

## Supported compatibility endpoints

- `POST /public/material/produceInSingles/produceInSingl/generateProductInSingle`
- `POST /public/material/produceOutSingle/produceOutSing/generateProduceOutSing`
- `POST /material/foreign/foreign/checkProdResult`

Gateway-rewritten `/material/...` aliases are supported as well. The operational page is available through `/msService/material/wms` after deployment.

## Build and test

```bash
mvn -pl backend/source-modules/material-wms -am test
mvn -pl backend/source-modules/material-wms -am package -DskipTests
```

The canonical schema is `deploy/docker/postgres/init/174-material-wms-completion-inbound.sql`. Apply it before starting the service on an existing database. Fresh Docker databases apply it through the normal PostgreSQL init directory.
