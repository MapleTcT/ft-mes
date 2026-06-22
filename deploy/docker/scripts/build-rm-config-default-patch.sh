#!/bin/sh
set -eu

docker_dir="$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)"
repo_root="$(CDPATH= cd -- "$docker_dir/../.." && pwd)"
adp_root="$(CDPATH= cd -- "$repo_root/.." && pwd)"
patch_root="$docker_dir/patches/rm-config-defaults"
build_dir="$patch_root/build"
classes_dir="$build_dir/classes"
src_dir="$patch_root/src"
jar_path="$patch_root/rm-config-defaults.jar"
maven_repo="$adp_root/bap-server/assembly/repository/maven"

spring_beans="$maven_repo/org/springframework/spring-beans/5.1.13.RELEASE/spring-beans-5.1.13.RELEASE.jar"
spring_context="$maven_repo/org/springframework/spring-context/5.1.13.RELEASE/spring-context-5.1.13.RELEASE.jar"
spring_core="$maven_repo/org/springframework/spring-core/5.1.13.RELEASE/spring-core-5.1.13.RELEASE.jar"
system_config_api="$maven_repo/com/supcon/supfusion/system-config-api/1.0.0-SNAPSHOT/system-config-api-1.0.0-SNAPSHOT.jar"

for dep in "$spring_beans" "$spring_context" "$spring_core" "$system_config_api"; do
  if [ ! -f "$dep" ]; then
    echo "missing patch compile dependency: $dep" >&2
    exit 1
  fi
done

rm -rf "$build_dir"
mkdir -p "$classes_dir"

javac -encoding UTF-8 -source 8 -target 8 \
  -cp "$spring_beans:$spring_context:$spring_core:$system_config_api" \
  -d "$classes_dir" \
  "$src_dir/com/supcon/orchid/RM/utils/RMConfigure.java"

jar cf "$jar_path" -C "$classes_dir" .
echo "built $jar_path"
