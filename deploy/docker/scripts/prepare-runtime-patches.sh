#!/bin/sh
set -eu

docker_dir="$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)"
default_runtime="$(CDPATH= cd -- "$docker_dir/../.." && pwd)/runtime/bap-server"
runtime_dir="${1:-$default_runtime}"
patch_dir="$runtime_dir/patches"
python_bin="${PYTHON:-python3}"

if [ ! -d "$runtime_dir" ]; then
  echo "runtime bap-server not found: $runtime_dir" >&2
  exit 1
fi

if [ ! -f "$docker_dir/patches/scdog-test-bypass/lib/libSCDog.so" ]; then
  "$docker_dir/scripts/build-test-scdog.sh"
fi
mkdir -p "$patch_dir"
copied=0
for patch_jar in "$docker_dir"/patches/*/*.jar "$docker_dir"/patches/*/*/*.jar; do
  [ -f "$patch_jar" ] || continue
  case "$patch_jar" in
    */qcs-redis-safe-payload/*)
      echo "skip legacy external QCS patch jar: $patch_jar" >&2
      continue
      ;;
    */rm-import-transaction/*)
      echo "skip legacy external RM import patch jar: $patch_jar" >&2
      continue
      ;;
    */configuration-entity-model-compat/*)
      echo "skip nested configuration entity/model patch jar: $patch_jar" >&2
      continue
      ;;
  esac
  cp "$patch_jar" "$patch_dir/"
  copied=$((copied + 1))
done

echo "installed $copied runtime patch jar(s) into $patch_dir"

"$python_bin" "$docker_dir/scripts/patch-postgres-runtime.py" \
  --base-server "$runtime_dir/base-Server" \
  --report "$runtime_dir/postgres-patch-report.json"
"$python_bin" "$docker_dir/scripts/patch-basicmanagement-signature-mapper.py" \
  --runtime-root "$runtime_dir"
"$python_bin" "$docker_dir/scripts/patch-signature-log-service-fallback.py" \
  --runtime-root "$runtime_dir"
"$python_bin" "$docker_dir/scripts/patch-orgmanagement-rbac-permission-mapper.py" \
  --runtime-root "$runtime_dir"
"$python_bin" "$docker_dir/scripts/patch-orgmanagement-keycloak-admin-url.py" \
  --runtime-root "$runtime_dir"
"$python_bin" "$docker_dir/scripts/patch-orgmanagement-standalone-auth-tasks.py" \
  --runtime-root "$runtime_dir"
"$python_bin" "$docker_dir/scripts/patch-operatetools-standalone-app-list.py" \
  --runtime-root "$runtime_dir"
"$python_bin" "$docker_dir/scripts/patch-eam-reactapi-ready.py" \
  --static-root "$runtime_dir/bap-workspace/bap-static"
"$docker_dir/scripts/build-configuration-entity-model-compat-patch.sh"
"$python_bin" "$docker_dir/scripts/patch-configuration-entity-model-runtime.py" \
  --runtime-root "$runtime_dir"

if [ -f "$runtime_dir/module-Server/LIMS/manual/LIMS-1.0.0.jar" ]; then
  "$docker_dir/scripts/patch-lims-qcs-inspect-report-service.sh" "$runtime_dir"
fi
if [ -f "$runtime_dir/module-Server/RMMs/manual/RMMs-1.0.0.jar" ]; then
  rm_jar="$runtime_dir/module-Server/RMMs/manual/RMMs-1.0.0.jar"
  "$python_bin" "$docker_dir/scripts/patch-rm-batch-formula-list.py" "$rm_jar"
  rm_tmp_jar="$runtime_dir/module-Server/RMMs/manual/RMMs-1.0.0.jar.tmp"
  "$docker_dir/scripts/build-rm-import-transaction-patch.sh" \
    --input-rm-jar "$rm_jar" \
    --output-rm-jar "$rm_tmp_jar"
  mv "$rm_tmp_jar" "$rm_jar"
fi
if [ -f "$runtime_dir/module-Server/WOMMs/manual/WOMMs-1.0.0.jar" ]; then
  wom_jar="$runtime_dir/module-Server/WOMMs/manual/WOMMs-1.0.0.jar"
  wom_tmp_jar="$runtime_dir/module-Server/WOMMs/manual/WOMMs-1.0.0.jar.tmp"
  "$docker_dir/scripts/build-wom-public-produce-created-disabled-boot-jar.sh" \
    --input-wom-jar "$wom_jar" \
    --output-wom-jar "$wom_tmp_jar"
  mv "$wom_tmp_jar" "$wom_jar"
fi
if [ -f "$runtime_dir/module-Server/WTSs/manual/WTSs-1.0.0.jar" ]; then
  "$python_bin" "$docker_dir/scripts/patch-wts-runtime-compat.py" \
    --wts-jar "$runtime_dir/module-Server/WTSs/manual/WTSs-1.0.0.jar" \
    --no-db
fi

echo "applied PostgreSQL runtime compatibility patches under $runtime_dir"
