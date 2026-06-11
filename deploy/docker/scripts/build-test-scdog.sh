#!/bin/sh
set -eu

script_dir="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
docker_dir="$(CDPATH= cd -- "$script_dir/.." && pwd)"
src="$docker_dir/patches/scdog-test-bypass/src/scdog_test_bypass.c"
out_dir="$docker_dir/patches/scdog-test-bypass/lib"
out="$out_dir/libSCDog.so"

cc_bin="${CC:-cc}"
if ! command -v "$cc_bin" >/dev/null 2>&1; then
  if command -v gcc >/dev/null 2>&1; then
    cc_bin=gcc
  else
    echo "C compiler not found; install gcc or set CC" >&2
    exit 1
  fi
fi

mkdir -p "$out_dir"
"$cc_bin" -shared -fPIC -O2 -Wall -Wextra -o "$out" "$src"
echo "built test SCDog shim: $out"
