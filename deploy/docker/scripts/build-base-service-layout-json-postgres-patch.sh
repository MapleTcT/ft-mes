#!/bin/sh
set -eu

docker_dir="$(CDPATH= cd -- "$(dirname "$0")/.." && pwd)"
repo_root="$(CDPATH= cd -- "$docker_dir/../.." && pwd)"
adp_root="$(CDPATH= cd -- "$repo_root/.." && pwd)"
patch_root="$docker_dir/patches/base-service-layout-json-postgres"
build_dir="$patch_root/build"
classes_dir="$build_dir/classes"
stub_dir="$build_dir/stubs"
src_dir="$patch_root/src"
jar_path="$build_dir/base-service-layout-json-postgres.jar"
maven_repo="$adp_root/bap-server/assembly/repository/maven"

if [ ! -d "$maven_repo" ]; then
  echo "missing maven repository: $maven_repo" >&2
  exit 1
fi

rm -rf "$build_dir"
mkdir -p "$classes_dir" "$stub_dir/com/supcon/orchid/ec/entities"

cat > "$stub_dir/com/supcon/orchid/ec/entities/Entity.java" <<'STUB'
package com.supcon.orchid.ec.entities;

public class Entity {
    public Entity() {}
    public void setCode(String code) {}
    public String getCode() { return null; }
}
STUB

cat > "$stub_dir/com/supcon/orchid/ec/entities/ExtraView.java" <<'STUB'
package com.supcon.orchid.ec.entities;

public class ExtraView {
    public ExtraView() {}
    public void setCode(String code) {}
    public void setViewJson(String viewJson) {}
    public String getViewJson() { return null; }
    public void setConfig(String config) {}
}
STUB

cat > "$stub_dir/com/supcon/orchid/ec/entities/View.java" <<'STUB'
package com.supcon.orchid.ec.entities;

import com.supcon.orchid.ec.enums.ShowType;
import com.supcon.orchid.ec.enums.ViewType;

public class View {
    public View() {}
    public void setCode(String code) {}
    public String getCode() { return null; }
    public void setTitle(String title) {}
    public String getTitle() { return null; }
    public void setIsShadow(Boolean isShadow) {}
    public Boolean getIsShadow() { return Boolean.FALSE; }
    public void setShadowView(View shadowView) {}
    public View getShadowView() { return null; }
    public void setEntity(Entity entity) {}
    public Entity getEntity() { return null; }
    public void setType(ViewType type) {}
    public ViewType getType() { return null; }
    public void setMobile(Boolean mobile) {}
    public Boolean getMobile() { return Boolean.FALSE; }
    public void setUrl(String url) {}
    public String getUrl() { return null; }
    public void setHasAttachment(Boolean hasAttachment) {}
    public Boolean getHasAttachment() { return Boolean.FALSE; }
    public void setMoveFlag(Boolean moveFlag) {}
    public Boolean getMoveFlag() { return Boolean.FALSE; }
    public void setOnlyForQuery(Boolean onlyForQuery) {}
    public Boolean getOnlyForQuery() { return Boolean.FALSE; }
    public void setShowType(ShowType showType) {}
    public void setDealInfoShow(Boolean dealInfoShow) {}
    public Boolean getDealInfoShow() { return Boolean.FALSE; }
    public void setDealInfoGroup(String dealInfoGroup) {}
    public String getDealInfoGroup() { return null; }
    public void setEnableSimpleDealInfo(Boolean enableSimpleDealInfo) {}
    public Boolean getEnableSimpleDealInfo() { return Boolean.FALSE; }
    public void setRetrialFlag(Boolean retrialFlag) {}
    public Boolean getRetrialFlag() { return Boolean.FALSE; }
    public void setScriptCode(String scriptCode) {}
    public String getScriptCode() { return null; }
    public void setModuleCode(String moduleCode) {}
    public String getModuleCode() { return null; }
    public void setAttachmentFlag(Boolean attachmentFlag) {}
    public Boolean getAttachmentFlag() { return Boolean.FALSE; }
    public void setExtraView(ExtraView extraView) {}
    public ExtraView getExtraView() { return null; }
}
STUB

classpath="$(find "$maven_repo" -name '*.jar' | tr '\n' ':')"

javac -encoding UTF-8 -source 8 -target 8 \
  -cp "$classpath" \
  -sourcepath "$src_dir:$stub_dir" \
  -d "$classes_dir" \
  "$src_dir/com/supcon/orchid/ec/cache/BaseServiceCacheService.java"

jar cf "$jar_path" -C "$classes_dir" com/supcon/orchid/ec/cache/BaseServiceCacheService.class
echo "built $jar_path"
