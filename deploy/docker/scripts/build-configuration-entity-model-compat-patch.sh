#!/bin/sh
set -eu

docker_dir="$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)"
repo_root="$(CDPATH= cd -- "$docker_dir/../.." && pwd)"
adp_root="$(CDPATH= cd -- "$repo_root/.." && pwd)"
patch_root="$docker_dir/patches/configuration-entity-model-compat"
classes_dir="$(mktemp -d /tmp/adp-config-entity-model-compat.XXXXXX)/classes"
maven_repo="$adp_root/bap-server/assembly/repository/maven"

service_jar="$maven_repo/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/configuration-services-service-1.0.0-SNAPSHOT.jar"
openapi_jar="$maven_repo/com/supcon/supfusion/configuration/configuration-services-open-api/1.0.0-SNAPSHOT/configuration-services-open-api-1.0.0-SNAPSHOT.jar"
base_jar="$maven_repo/com/supcon/supfusion/configuration/configuration-services-base/1.0.0-SNAPSHOT/configuration-services-base-1.0.0-SNAPSHOT.jar"

for dep in "$service_jar" "$openapi_jar" "$base_jar"; do
  if [ ! -f "$dep" ]; then
    echo "missing patch compile dependency: $dep" >&2
    exit 1
  fi
done

mkdir -p "$classes_dir" "$patch_root"
classpath="$service_jar:$openapi_jar:$base_jar:$(find "$maven_repo" -name '*.jar' -type f | tr '\n' ':')"

javac -encoding UTF-8 -source 8 -target 8 \
  -cp "$classpath" \
  -d "$classes_dir" \
  "$repo_root/backend/modules/com/supcon/supfusion/configuration/configuration-services-open-api/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/openapi/utils/DtoUtils.java" \
  "$repo_root/backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/service/impl/ModelServiceImpl.java" \
  "$repo_root/backend/modules/com/supcon/supfusion/configuration/configuration-services-service/1.0.0-SNAPSHOT/com/supcon/supfusion/configuration/services/service/impl/EntityServiceImpl.java" \
  "$repo_root/backend/modules/com/supcon/supfusion/configuration/configuration-services-base/1.0.0-SNAPSHOT/com/supcon/supfusion/base/services/impl/MenuInfoServiceImpl.java"

jar cf "$patch_root/configuration-entity-model-compat.jar" -C "$classes_dir" .
echo "built $patch_root/configuration-entity-model-compat.jar"
