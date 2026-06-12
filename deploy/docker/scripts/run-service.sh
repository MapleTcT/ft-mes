#!/bin/sh
set -eu

jar_path="${1:?jar path is required}"
shift || true

if [ ! -f "$jar_path" ]; then
  echo "JAR not found: $jar_path" >&2
  exit 1
fi

tmp_dir="/opt/adp/tmp"
mkdir -p /root/initFile /opt/adp/logs "$tmp_dir" /tmp
export TMPDIR="$tmp_dir"
export LD_LIBRARY_PATH="/opt/adp/lib:/usr/lib:${LD_LIBRARY_PATH:-}"
if [ ! -f /root/initFile/bj.png ]; then
  for icon in \
    /opt/adp/bap-server/bap-workspace/bap-static/bap/static/bap-themes/work-grey/img/icon_bj.png \
    /opt/adp/bap-server/bap-workspace/bap-static/bap/static/bap-themes/preview-grey/img/icon_bj.png \
    /opt/adp/bap-server/bap-workspace/bap-static/bap/static/mobile/bap-themes/work-grey/img/icon_bj.png
  do
    if [ -f "$icon" ]; then
      cp "$icon" /root/initFile/bj.png
      break
    fi
  done
fi

postgres_driver="/opt/adp/drivers/postgresql.jar"
classpath="$jar_path"
if [ -f "$postgres_driver" ]; then
  classpath="$postgres_driver:$jar_path"
fi
patch_dir="/opt/adp/bap-server/patches"
if [ -d "$patch_dir" ]; then
  for patch_jar in "$patch_dir"/*.jar; do
    [ -f "$patch_jar" ] || continue
    case "$(basename "$patch_jar")" in
      scaffold-dbp-postgresql-url.jar)
        continue
        ;;
    esac
    classpath="$patch_jar:$classpath"
  done
fi

service_dir="$(basename "$(dirname "$jar_path")")"
script_root="/opt/adp/sql/$service_dir"
script_root_arg=""
if [ -d "$script_root/postgresql" ]; then
  script_root_arg="-Dsupfusion.cloud.datasource.script.script-root-dirs=file:$script_root"
fi
script_error_args="-Dsupfusion.cloud.datasource.script.continue-on-error=true -Dsupfusion.cloud.datasource.script.ignore-failed-drops=true"
mapper_locations="classpath*:mappers/*.xml,classpath*:mapper/*.xml,classpath*:mappers/postgresql/*.xml,classpath*:mapper/postgresql/*.xml,classpath*:com/supcon/supfusion/**/dao/mapper/postgresql/*.xml,classpath*:com/supcon/supfusion/**/dao/mappers/postgresql/*.xml"
datasource_args="-Dsupfusion.cloud.datasource.connect.use-system=true -Dsupfusion.cloud.datasource.connect.system.db-type=${SUPOS_SYSTEM_DB_TYPE:-postgresql} -Dsupfusion.cloud.datasource.connect.system.db-version=${SUPOS_SYSTEM_DB_VERSION:-default} -Dsupfusion.cloud.datasource.connect.system.db-name=${SUPOS_SYSTEM_DB_NAME:-adp} -Dsupfusion.cloud.datasource.connect.system.host=${SUPOS_SYSTEM_DB_HOST:-postgres} -Dsupfusion.cloud.datasource.connect.system.port=${SUPOS_SYSTEM_DB_PORT:-5432} -Dsupfusion.cloud.datasource.connect.system.username=${SUPOS_SYSTEM_DB_USERNAME:-adp} -Dsupfusion.cloud.datasource.connect.system.password=${SUPOS_SYSTEM_DB_PASSWORD:-adp123456}"
service_extra_args="-Dlogging.level.com.supcon.supfusion.framework.cloud.i18n.context.support.RemoteBundleMessageSource=${ADP_REMOTE_I18N_LOG_LEVEL:-OFF} -Dlogging.level.springfox.documentation.swagger.readers.operation.OperationHttpMethodReader=${ADP_SWAGGER_LOG_LEVEL:-OFF}"
if [ "$service_dir" = "notification-sms-jincang" ]; then
  service_extra_args="-Dsupfusion.cloud.registry.enabled=false -Dsupfusion.cloud.discovery.enabled=false -Dspring.cloud.nacos.discovery.enabled=false -Dspring.cloud.service-registry.auto-registration.enabled=false"
fi
case "$service_dir" in
  orgmanagement)
    service_extra_args="${service_extra_args} -Dintegration.supos.enabled=${INTEGRATION_SUPOS_ENABLED:-false} -Dentitlement.server=${SUPOS_ENTITLEMENT_SERVER:-license:8010} -Dentitlement.libc.type=${SUPOS_ENTITLEMENT_LIBC_TYPE:-glibc}"
    ;;
esac
case "$service_dir" in
  notification-dingtalk|notification-mobile|notification-sms-jincang|notification-wechat)
    service_extra_args="${service_extra_args} -Dsystemconfig.ribbon.NIWSServerListClassName=com.netflix.loadbalancer.ConfigurationBasedServerList -Dsystemconfig.ribbon.listOfServers=sysmanagement:30220 -Dnotification-admin.ribbon.NIWSServerListClassName=com.netflix.loadbalancer.ConfigurationBasedServerList -Dnotification-admin.ribbon.listOfServers=notification-admin:30102 -Di18n.ribbon.NIWSServerListClassName=com.netflix.loadbalancer.ConfigurationBasedServerList -Di18n.ribbon.listOfServers=i18n:8080 -Dsupfusion.cloud.i18n.host=http://i18n:8080"
    ;;
esac
case "$service_dir" in
  notification-dingtalk)
    service_extra_args="${service_extra_args} -Dspring.autoconfigure.exclude=cn.supcon.supfusion.systemconfig.config.SystemConfigAutoConfiguration"
    ;;
esac

if [ "$#" -eq 0 ] || [ "${1#--}" != "$1" ]; then
  exec java ${JAVA_OPTS:-} \
    -Dbap.home=/opt/adp/bap-server \
    -Djava.io.tmpdir="$tmp_dir" \
    -Djava.library.path=/opt/adp/lib:/usr/lib \
    -Djna.library.path=/opt/adp/lib \
    ${script_root_arg} \
    ${script_error_args} \
    ${datasource_args} \
    ${service_extra_args} \
    -Dsupfusion.cloud.tenant.init-getter=false \
    -Dsupfusion.cloud.tenant.register=false \
    -Dsupfusion.cloud.registry.group="${SUPOS_NACOS_GROUP:-prod}" \
    -Dsupfusion.cloud.discovery.group="${SUPOS_NACOS_GROUP:-prod}" \
    -Dspring.cloud.nacos.discovery.group="${SUPOS_NACOS_GROUP:-prod}" \
    -Dspring.cloud.nacos.config.group="${SUPOS_NACOS_GROUP:-prod}" \
    "-Dmybatis-plus.mapper-locations=$mapper_locations" \
    -Dspring.cloud.nacos.config.server-addr="${SUPOS_NACOS_ADDRESS:-nacos:8848}" \
    -Dspring.cloud.nacos.discovery.server-addr="${SUPOS_NACOS_ADDRESS:-nacos:8848}" \
    -Dnacos.server-addr="${SUPOS_NACOS_ADDRESS:-nacos:8848}" \
    -Dnacos.discovery.server-addr="${SUPOS_NACOS_ADDRESS:-nacos:8848}" \
    -cp "$classpath" org.springframework.boot.loader.JarLauncher "$@"
fi

main_class="$1"
shift || true
exec java ${JAVA_OPTS:-} \
  -Dbap.home=/opt/adp/bap-server \
  -Djava.io.tmpdir="$tmp_dir" \
  -Djava.library.path=/opt/adp/lib:/usr/lib \
  -Djna.library.path=/opt/adp/lib \
  ${script_root_arg} \
  ${script_error_args} \
  ${datasource_args} \
  ${service_extra_args} \
  -Dsupfusion.cloud.tenant.init-getter=false \
  -Dsupfusion.cloud.tenant.register=false \
  -Dsupfusion.cloud.registry.group="${SUPOS_NACOS_GROUP:-prod}" \
  -Dsupfusion.cloud.discovery.group="${SUPOS_NACOS_GROUP:-prod}" \
  -Dspring.cloud.nacos.discovery.group="${SUPOS_NACOS_GROUP:-prod}" \
  -Dspring.cloud.nacos.config.group="${SUPOS_NACOS_GROUP:-prod}" \
  "-Dmybatis-plus.mapper-locations=$mapper_locations" \
  -Dnacos.server-addr="${SUPOS_NACOS_ADDRESS:-nacos:8848}" \
  -Dnacos.discovery.server-addr="${SUPOS_NACOS_ADDRESS:-nacos:8848}" \
  -cp "$classpath" "$main_class" "$@"
