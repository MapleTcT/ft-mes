#!/bin/sh
set -eu

docker_dir="$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)"
default_runtime="$(CDPATH= cd -- "$docker_dir/../.." && pwd)/runtime/bap-server"
runtime_dir="${1:-$default_runtime}"
patch_dir="$runtime_dir/patches"

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
