"use strict";

const assert = require("assert");
const fs = require("fs");
const path = require("path");

const dockerRoot = path.resolve(__dirname, "..");
const assetRoot = path.join(
  dockerRoot,
  "assets",
  "module-static",
  "PATROL",
  "inputStandard",
  "inputStandard",
  "inputStanEdit"
);
const nginxPath = path.join(dockerRoot, "nginx", "adp.conf");
const nginx = fs.readFileSync(nginxPath, "utf8");
const i18nPath = path.join(dockerRoot, "assets", "module-static", "PATROL", "i18n-value.js");
const i18n = fs.readFileSync(i18nPath, "utf8");
const routeAssetRoot = path.join(
  dockerRoot,
  "assets",
  "module-static",
  "PATROL",
  "patrolRoute",
  "workGroup",
  "workGroupList"
);
const areaEditorAssetRoot = path.join(
  dockerRoot,
  "assets",
  "module-static",
  "PATROL",
  "patrolRoute",
  "workGroup",
  "workAreaPtEdit"
);

for (const [key, value] of [
  ["ec.print.template.delete", "删除"],
  ["ec.print.template.Stop", "停用"],
  ["ec.print.template.import", "导入"],
]) {
  assert(
    i18n.includes(`window.InternationalResource["${key}"] = "${value}";`),
    `PATROL compatibility i18n must translate ${key}`
  );
}

for (const fileName of ["body.js", "body-es5.js"]) {
  const assetPath = path.join(assetRoot, fileName);
  const source = fs.readFileSync(assetPath, "utf8");
  const requestPath = `/greenDill/static/PATROL/inputStandard/inputStandard/inputStanEdit/${fileName}`;
  const aliasPath = `/usr/share/nginx/module-static/PATROL/inputStandard/inputStandard/inputStanEdit/${fileName}`;

  assert(
    source.includes("function editOrValueChange(valType, editType, operateField)"),
    `${fileName} must restore the input-standard value/edit interaction`
  );
  assert(!/\bdebugger\s*;/.test(source), `${fileName} must not pause the browser debugger`);
  assert(nginx.includes(`location = ${requestPath} {`), `${fileName} must have an exact Nginx route`);
  assert(nginx.includes(`alias ${aliasPath};`), `${fileName} must map to the restored asset`);

  new Function(source);
}

for (const fileName of ["body.js", "body-es5.js"]) {
  const assetPath = path.join(routeAssetRoot, fileName);
  assert(fs.existsSync(assetPath), `PATROL route compatibility asset must exist: ${fileName}`);
}
const routeBody = fs.readFileSync(path.join(routeAssetRoot, "body.js"), "utf8");
for (const marker of [
  "__ADP_PATROL_ROUTE_ACTIONS_INSTALLED__",
  "updateItemState",
  "checkRelationPlan",
  "deleteWorkGroups",
  "deleteWorkAreas",
]) {
  assert(routeBody.includes(marker), `PATROL route body must implement ${marker}`);
}
assert(
    routeBody.includes("event.stopImmediatePropagation()") &&
    routeBody.includes("closeMoreMenu(button)") &&
    routeBody.includes("closeMenuAfterCurrentClick") &&
    routeBody.includes('querySelector(".sup-datagrid-button-item")'),
  "PATROL route compatibility actions must own the click and close the More menu"
);

for (const fileName of ["body.js", "body-es5.js"]) {
  const assetPath = path.join(areaEditorAssetRoot, fileName);
  assert(fs.existsSync(assetPath), `PATROL area-editor compatibility asset must exist: ${fileName}`);
  new Function(fs.readFileSync(assetPath, "utf8"));
}
const areaEditorBody = fs.readFileSync(path.join(areaEditorAssetRoot, "body.js"), "utf8");
for (const marker of [
  "__ADP_PATROL_AREA_EDITOR_ACTIONS_INSTALLED__",
  "addLine()",
  "deleteLine(rowIndexes.join",
  "moveUpLine()",
  "moveDownLine()",
  'setValueByKey(rowIndex, "workGroupId.id", params.id)',
  "closeMenuAfterCurrentClick",
  'querySelector(".sup-datagrid-button-item")',
]) {
  assert(areaEditorBody.includes(marker), `PATROL area editor body must implement ${marker}`);
}

const exactRouteIndex = nginx.indexOf(
  "location = /greenDill/static/PATROL/inputStandard/inputStandard/inputStanEdit/body.js"
);
const placeholderRouteIndex = nginx.indexOf(
  "location ~ ^/greenDill/static/.*/body(?:-es5)?\\.js$"
);
assert(exactRouteIndex >= 0, "PATROL input-standard exact route must exist");
assert(placeholderRouteIndex >= 0, "generic body-script fallback must exist");
assert(
  exactRouteIndex < placeholderRouteIndex,
  "PATROL input-standard exact routes must precede the generic body-script fallback"
);
const patrolRouteBodyIndex = nginx.indexOf(
  "location = /greenDill/static/PATROL/patrolRoute/workGroup/workGroupList/body.js"
);
assert(patrolRouteBodyIndex >= 0, "nginx must expose the PATROL route compatibility body script");
assert(
  patrolRouteBodyIndex < placeholderRouteIndex,
  "PATROL route exact routes must precede the generic body-script fallback"
);
const patrolAreaEditorBodyIndex = nginx.indexOf(
  "location = /greenDill/static/PATROL/patrolRoute/workGroup/workAreaPtEdit/body.js"
);
assert(patrolAreaEditorBodyIndex >= 0, "nginx must expose the PATROL area-editor body script");
assert(
  patrolAreaEditorBodyIndex < placeholderRouteIndex,
  "PATROL area-editor exact routes must precede the generic body-script fallback"
);
assert(
  nginx.includes("location = /msService/PATROL/patrolRoute/workGroup/workAreaPtEdit {") &&
    nginx.includes(
      '<script src="/greenDill/static/PATROL/patrolRoute/workGroup/workAreaPtEdit/body.js"></script>'
    ),
  "PATROL area-editor HTML must load the compatibility body script"
);
assert(
  nginx.includes("alias /usr/share/nginx/module-static/PATROL/i18n-value.js;"),
  "PATROL page i18n routes must use the compatibility bundle"
);

console.log("PATROL static asset acceptance: PASS");
