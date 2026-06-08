#!/bin/sh
set -eu

jar_path="${1:?jar path is required}"
shift || true

if [ ! -f "$jar_path" ]; then
  echo "JAR not found: $jar_path" >&2
  exit 1
fi

mkdir -p /root/initFile /opt/adp/logs /opt/adp/tmp /tmp

postgres_driver="/opt/adp/drivers/postgresql.jar"
classpath="$jar_path"
if [ -f "$postgres_driver" ]; then
  classpath="$postgres_driver:$jar_path"
fi

service_dir="$(basename "$(dirname "$jar_path")")"
script_root="/opt/adp/sql/$service_dir"
script_root_arg=""
if [ -d "$script_root/postgresql" ]; then
  script_root_arg="-Dsupfusion.cloud.datasource.script.script-root-dirs=file:$script_root"
fi
script_error_args="-Dsupfusion.cloud.datasource.script.continue-on-error=true -Dsupfusion.cloud.datasource.script.ignore-failed-drops=true"
mapper_locations="classpath*:mappers/*.xml,classpath*:mapper/*.xml,classpath*:mappers/postgresql/*.xml,classpath*:mapper/postgresql/*.xml,classpath*:com/supcon/supfusion/**/dao/mapper/postgresql/*.xml,classpath*:com/supcon/supfusion/**/dao/mappers/postgresql/*.xml"
service_extra_args=""
if [ "$service_dir" = "notification-sms-jincang" ]; then
  service_extra_args="-Dsupfusion.cloud.registry.enabled=false -Dsupfusion.cloud.discovery.enabled=false -Dspring.cloud.nacos.discovery.enabled=false -Dspring.cloud.service-registry.auto-registration.enabled=false"
fi

if [ "$#" -eq 0 ] || [ "${1#--}" != "$1" ]; then
  exec java ${JAVA_OPTS:-} \
    -Dbap.home=/opt/adp/bap-server \
    -Djava.io.tmpdir=/opt/adp/tmp \
    ${script_root_arg} \
    ${script_error_args} \
    ${service_extra_args} \
    -Dsupfusion.cloud.tenant.register=false \
    -Dsupfusion.cloud.registry.group="${SUPOS_NACOS_GROUP:-prod}" \
    -Dsupfusion.cloud.discovery.group="${SUPOS_NACOS_GROUP:-prod}" \
    -Dspring.cloud.nacos.discovery.group="${SUPOS_NACOS_GROUP:-prod}" \
    -Dspring.cloud.nacos.config.group="${SUPOS_NACOS_GROUP:-prod}" \
    "-Dmybatis-plus.mapper-locations=$mapper_locations" \
    -Dspring.cloud.nacos.config.server-addr="${SUPOS_NACOS_ADDRESS:-nacos:8848}" \
    -Dspring.cloud.nacos.discovery.server-addr="${SUPOS_NACOS_ADDRESS:-nacos:8848}" \
    -Dnacos.server-addr="${SUPOS_NACOS_ADDRESS:-nacos:8848}" \
    -cp "$classpath" org.springframework.boot.loader.JarLauncher "$@"
fi

main_class="$1"
shift || true
exec java ${JAVA_OPTS:-} \
  -Dbap.home=/opt/adp/bap-server \
  -Djava.io.tmpdir=/opt/adp/tmp \
  ${script_root_arg} \
  ${script_error_args} \
  ${service_extra_args} \
  -Dsupfusion.cloud.tenant.register=false \
  -Dsupfusion.cloud.registry.group="${SUPOS_NACOS_GROUP:-prod}" \
  -Dsupfusion.cloud.discovery.group="${SUPOS_NACOS_GROUP:-prod}" \
  -Dspring.cloud.nacos.discovery.group="${SUPOS_NACOS_GROUP:-prod}" \
  -Dspring.cloud.nacos.config.group="${SUPOS_NACOS_GROUP:-prod}" \
  "-Dmybatis-plus.mapper-locations=$mapper_locations" \
  -Dnacos.server-addr="${SUPOS_NACOS_ADDRESS:-nacos:8848}" \
  -cp "$classpath" "$main_class" "$@"
