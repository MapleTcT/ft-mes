#!/usr/bin/env python3
from __future__ import annotations

import re
import shutil
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
SOURCE_DIR = ROOT / "nacos-config"
DOCKER_DIR = Path(__file__).resolve().parents[1]
TARGET_DIR = DOCKER_DIR / "nacos-rendered"


def load_env() -> dict[str, str]:
    values: dict[str, str] = {}
    for env_file in (DOCKER_DIR / ".env.example", DOCKER_DIR / ".env"):
        if not env_file.exists():
            continue
        for raw_line in env_file.read_text(encoding="utf-8", errors="replace").splitlines():
            line = raw_line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            values[key.strip()] = value.strip().strip("'\"")
    return values


ENV = load_env()


def env(name: str, default: str) -> str:
    return ENV.get(name, default)


def keycloak_client() -> str:
    realm = env("ADP_KEYCLOAK_REALM", "dt")
    return env("KEYCLOAK_CLIENT", f"pc_{realm}")


def datasource_password() -> str:
    return env("SUPOS_SYSTEM_DB_PASSWORD", "adp123456")


REPLACEMENTS = {
    "${SUPOS_SYSTEM_DB_TYPE:oracle}": "${SUPOS_SYSTEM_DB_TYPE:postgresql}",
    "${SUPOS_SYSTEM_DB_NAME:orcl}": "${SUPOS_SYSTEM_DB_NAME:adp}",
    "${SUPOS_SYSTEM_DB_HOST:127.0.0.1}": "${SUPOS_SYSTEM_DB_HOST:postgres}",
    "${SUPOS_SYSTEM_DB_PORT:1521}": "${SUPOS_SYSTEM_DB_PORT:5432}",
    "${VALIDATIONQUERYSQL:SELECT 1}": "${VALIDATIONQUERYSQL:SELECT 1}",
    "${SUPOS_NACOS_ADDRESS:127.0.0.1:8848}": "${SUPOS_NACOS_ADDRESS:nacos:8848}",
    "${SUPOS_REDIS_HOST:127.0.0.1}": "${SUPOS_REDIS_HOST:redis}",
    "${SUPOS_REDIS_PORT:6379}": "${SUPOS_REDIS_PORT:6379}",
    "${SUPOS_KAFKA_BROKERS:127.0.0.1:9092}": "${SUPOS_KAFKA_BROKERS:kafka:9092}",
    "${SUPOS_ZOOKEEPER_NODES:127.0.0.1:2181}": "${SUPOS_ZOOKEEPER_NODES:zookeeper:2181}",
    "${supfusion.cloud.datasource.connect.system.db-type:oracle}": "${supfusion.cloud.datasource.connect.system.db-type:postgresql}",
    "127.0.0.1:9092": "${SUPOS_KAFKA_BROKERS:kafka:9092}",
    "127.0.0.1:2181": "${SUPOS_ZOOKEEPER_NODES:zookeeper:2181}",
    "http://localhost:30200": "${SUPOS_MINIO_ENDPOINT:http://minio:30200}",
    "http://127.0.0.1:30130": f"http://{env('I18N_HOST', 'i18n')}:{env('I18N_PORT', '8080')}",
    "${I18N_HOST:127.0.0.1}": "${I18N_HOST:i18n}",
    "${I18N_PORT:30130}": "${I18N_PORT:8080}",
    "${SUPOS_ADDRESS:http://127.0.0.1:${server.port}}": "${SUPOS_ADDRESS:http://10.11.100.17:18080}",
    "forceChange:": "",
}


TARGETED_OVERRIDES = {
    "supfusion-datasource-system.properties": {
        "supfusion.cloud.datasource.connect.system.db-type": env("SUPOS_SYSTEM_DB_TYPE", "postgresql"),
        "supfusion.cloud.datasource.connect.system.db-name": env("SUPOS_SYSTEM_DB_NAME", "adp"),
        "supfusion.cloud.datasource.connect.system.host": env("SUPOS_SYSTEM_DB_HOST", "postgres"),
        "supfusion.cloud.datasource.connect.system.port": env("SUPOS_SYSTEM_DB_PORT", "5432"),
        "supfusion.cloud.datasource.connect.system.username": env("SUPOS_SYSTEM_DB_USERNAME", "adp"),
        "supfusion.cloud.datasource.connect.system.password": datasource_password(),
        "supfusion.cloud.datasource.druid.validationQuerySql": env("VALIDATIONQUERYSQL", "SELECT 1"),
    },
    "supfusion-registry.properties": {
        "supfusion.cloud.nacos.server-addr": f"http://{env('SUPOS_NACOS_ADDRESS', 'nacos:8848')}",
        "supfusion.cloud.nacos.username": env("SUPOS_NACOS_USERNAME", "nacos"),
        "supfusion.cloud.nacos.password": env("SUPOS_NACOS_PASSWORD", "nacos"),
        "supfusion.cloud.registry.group": env("SUPOS_NACOS_REGISTRY_GROUP", "prod"),
        "supfusion.cloud.discovery.group": env("CLOUD_DISCOVERY_GROUP", "prod"),
    },
    "supfusion-file-server.properties": {
        "minio.endpoint": env("SUPOS_MINIO_ENDPOINT", "http://minio:30200"),
        "minio.accessKey": env("SUPOS_MINIO_ACCESS_KEY", "adpminio"),
        "minio.secretKey": env("SUPOS_MINIO_SECRET_KEY", "adpminio123"),
    },
    "supfusion-i18n.properties": {
        "server.port": env("I18N_PORT", "8080"),
    },
    "supfusion-mongodb-system.properties": {
        "spring.data.mongodb.uri": f"mongodb://{env('MONGO_USERNAME', 'root')}:{env('MONGO_PASSWORD', 'adpmongo123')}@{env('MONGO_HOST', 'mongo')}:{env('MONGO_PORT', '27017')}/",
        "spring.data.mongodb.database": env("MONGO_DBNAME", "supfusion"),
        "spring.data.mongodb.authentication-database": env("MONGO_AUTH_DBNAME", "admin"),
    },
    "supfusion-baseApplications.properties": {
        "WebSocketUrl": f"{env('ADP_PUBLIC_HOST', '10.11.100.17')}:{env('ADP_HTTP_PORT', '18080')}",
        "bap.allow.empty.password": "false",
        "LIMSBasic/LIMSBasic.dataPermission": "false",
        "LIMSSample/LIMSSample.dataPermission": "false",
        "QCS/QCS.dataPermission": "false",
        "spring.datasource.type": "com.supcon.supfusion.framework.scaffold.dbp.MultiTenantDatasource",
        "mybatis-plus.mapper-locations[0]": "classpath*:com/supcon/supfusion/i18n/dao/mapper/${supfusion.cloud.datasource.connect.system.db-type}/*.xml",
        "mybatis-plus.mapper-locations[1]": "classpath*:mappers/*.xml",
        "mybatis-plus.mapper-locations[2]": "classpath*:mapper/*.xml",
        "mybatis-plus.mapper-locations[3]": "classpath*:mappers/${supfusion.cloud.datasource.connect.system.db-type}/*.xml",
    },
    "supfusion-signature.properties": {
        "mybatis-plus.mapper-locations[0]": "classpath*:mappers/${supfusion.cloud.datasource.connect.system.db-type}/*.xml",
        "mybatis-plus.mapper-locations[1]": "classpath*:mappers/*.xml",
    },
    "supfusion-adp-lite.properties": {
        "supfusion.cloud.i18n.host": f"http://{env('I18N_HOST', 'i18n')}:{env('I18N_PORT', '8080')}",
        "supfusion.supos.supos-host": env("SUPOS_ADDRESS", "http://10.11.100.17:18080"),
        "supfusion.supos.ak": "adp-test-ak",
        "supfusion.supos.sk": "adp-test-sk",
    },
    "supfusion-jwt-common.properties": {
        "supfusion.cloud.jwt.tokenHead": "Bearer",
        "supfusion.cloud.jwt.secret": env("SUPOS_JWT_PUBLIC_KEY", env("SUPOS_JWT_SECRET", "adp-test-jwt-secret")),
    },
    "supfusion-gateway.properties": {
        "jwt.secret": env("SUPOS_JWT_SECRET", "adp-test-jwt-secret"),
        "snow-flake.datacenterId": env("SNOW_FLAKE_DATACENTER_ID", "0"),
        "snow-flake.workerId": env("SNOW_FLAKE_WORKER_ID", "0"),
        "keycloak.client": keycloak_client(),
        "keycloak.grant-type": env("KEYCLOAK_GRANT_TYPE", "password"),
    },
    "supfusion-entityconf.properties": {
        "supfusion.cloud.i18n.temp": "/tmp",
        "supos.task.api": "",
        "supos.task.log.api": "",
    },
    "supfusion-configuration-services.properties": {
        "work.dir": "/opt/adp",
    },
    "supfusion-mybatis-common.properties": {
        "mybatis-plus.mapper-locations[0]": "classpath*:com/supcon/supfusion/i18n/dao/mapper/${supfusion.cloud.datasource.connect.system.db-type}/*.xml",
        "mybatis-plus.mapper-locations[1]": "classpath*:mappers/*.xml",
        "mybatis-plus.mapper-locations[2]": "classpath*:mapper/*.xml",
        "mybatis-plus.mapper-locations[3]": "classpath*:mappers/${supfusion.cloud.datasource.connect.system.db-type}/*.xml",
    },
}

if env("ADP_RENDER_DATASOURCE_COMPAT", "false").lower() not in {"1", "true", "yes", "on"}:
    for key in (
        "${SUPOS_SYSTEM_DB_TYPE:oracle}",
        "${SUPOS_SYSTEM_DB_NAME:orcl}",
        "${SUPOS_SYSTEM_DB_HOST:127.0.0.1}",
        "${SUPOS_SYSTEM_DB_PORT:1521}",
        "${supfusion.cloud.datasource.connect.system.db-type:oracle}",
    ):
        REPLACEMENTS.pop(key, None)
    TARGETED_OVERRIDES.pop("supfusion-datasource-system.properties", None)


def set_property(text: str, key: str, value: str) -> str:
    pattern = re.compile(rf"^{re.escape(key)}=.*$", re.MULTILINE)
    line = f"{key}={value}"
    if pattern.search(text):
        return pattern.sub(line, text)
    return text.rstrip() + "\n" + line + "\n"


def render_text(name: str, text: str) -> str:
    for src, dst in REPLACEMENTS.items():
        text = text.replace(src, dst)
    text = re.sub(r"^(.*mapper-locations(?:\[[0-9]+])?=)classpath:", r"\1classpath*:", text, flags=re.MULTILINE)
    for key, value in TARGETED_OVERRIDES.get(name, {}).items():
        text = set_property(text, key, value)
    return text


def main() -> None:
    if not SOURCE_DIR.is_dir():
        raise SystemExit(f"Nacos config source not found: {SOURCE_DIR}")

    if TARGET_DIR.exists():
        shutil.rmtree(TARGET_DIR)
    TARGET_DIR.mkdir(parents=True)

    count = 0
    for src in sorted(SOURCE_DIR.glob("*.properties")):
        text = src.read_text(encoding="utf-8", errors="replace")
        (TARGET_DIR / src.name).write_text(render_text(src.name, text), encoding="utf-8")
        count += 1

    print(f"rendered {count} Nacos config files to {TARGET_DIR}")


if __name__ == "__main__":
    main()
