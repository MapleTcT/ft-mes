#!/bin/sh
set -eu

if [ "$#" -ne 2 ]; then
  echo "usage: $0 <LIMS-boot-jar> <output-jar>" >&2
  exit 2
fi

lims_jar="$1"
output_jar="$2"
work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT

mkdir -p "$work_dir/lib" "$work_dir/classes"
(
  cd "$work_dir"
  jar xf "$lims_jar" BOOT-INF/lib
  cp BOOT-INF/lib/*.jar "$work_dir/lib/"
)

javac -source 1.8 -target 1.8 \
  -cp "$work_dir/lib/*" \
  -d "$work_dir/classes" \
  "$(dirname "$0")/src/com/supcon/orchid/QCS/utils/QCSConfigureUtil.java"

mkdir -p "$(dirname "$output_jar")"
jar cf "$output_jar" -C "$work_dir/classes" .
