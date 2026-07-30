"use strict";

const assert = require("assert");
const fs = require("fs");
const path = require("path");

const root = path.resolve(__dirname, "..", "..", "..");
const templatePath = path.join(
  root,
  "backend",
  "modules",
  "com",
  "supcon",
  "supfusion",
  "configuration",
  "configuration-services-open-api",
  "1.0.0-SNAPSHOT",
  "templates",
  "entity",
  "wf.ftl"
);
const buildScriptPath = path.join(
  root,
  "deploy",
  "docker",
  "scripts",
  "build-configuration-entity-model-compat-patch.sh"
);
const runtimePatcherPath = path.join(
  root,
  "deploy",
  "docker",
  "scripts",
  "patch-configuration-entity-model-runtime.py"
);

const template = fs.readFileSync(templatePath, "utf8");
const buildScript = fs.readFileSync(buildScriptPath, "utf8");
const runtimePatcher = fs.readFileSync(runtimePatcherPath, "utf8");

assert(
  template.includes('var flowConfigWindow=window.open(url,"_blank");'),
  "workflow configuration should keep the normal new-tab behavior"
);
assert(
  template.includes("if(!flowConfigWindow){") &&
    template.includes("window.location.assign(url);"),
  "workflow configuration should navigate in the current tab when popups are blocked"
);
assert(
  buildScript.includes('"$classes_dir/templates/entity"') &&
    buildScript.includes('"$classes_dir/templates/entity/wf.ftl"'),
  "workflow template must be packaged in the configuration compatibility patch"
);
assert(
  runtimePatcher.includes('"templates/entity/wf.ftl"'),
  "runtime patcher must replace the workflow management template"
);

console.log("workflow configuration popup fallback: PASS");
