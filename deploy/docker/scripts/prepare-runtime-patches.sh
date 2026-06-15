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

echo "applied PostgreSQL runtime compatibility patches under $runtime_dir"
