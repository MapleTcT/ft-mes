"use strict";

const assert = require("assert");
const fs = require("fs");
const path = require("path");
const vm = require("vm");

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
const hiddenDangerMigration = fs.readFileSync(
  path.join(dockerRoot, "postgres", "init", "186-patrol-hidden-danger-eam-risk-compat.sql"),
  "utf8"
);
const i18nStartupMigration = fs.readFileSync(
  path.join(dockerRoot, "postgres", "init", "015-i18n-dingtalk-startup.sql"),
  "utf8"
);
const womToolbarMigration = fs.readFileSync(
  path.join(dockerRoot, "postgres", "init", "168-wom-maketasklist-toolbar-interaction-compat.sql"),
  "utf8"
);
const hiddenDangerSourcePatch = fs.readFileSync(
  path.join(dockerRoot, "scripts", "patch-patrol-postgres-source.py"),
  "utf8"
);
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
const itemEditorAssetRoot = path.join(
  dockerRoot,
  "assets",
  "module-static",
  "PATROL",
  "patrolRoute",
  "workArea",
  "workItemPtEdit"
);
const resultEditorAssetRoot = path.join(
  dockerRoot,
  "assets",
  "module-static",
  "PATROL",
  "patrolTask",
  "potrolTask",
  "enteringResultEdit"
);
const hiddenDangerAssetRoot = path.join(
  dockerRoot,
  "assets",
  "module-static",
  "PATROL",
  "patrolTask",
  "taskDetail",
  "abnormalSummary"
);

for (const [key, value] of [
  ["ec.print.template.delete", "删除"],
  ["ec.print.template.Stop", "停用"],
  ["ec.print.template.import", "导入"],
  ["ec.view.button.insertRow", "插行"],
  ["ec.view.button.moveRowUp", "上移"],
  ["ec.view.button.moveRowDown", "下移"],
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
  const assetPath = path.join(hiddenDangerAssetRoot, fileName);
  const source = fs.readFileSync(assetPath, "utf8");
  const requestPath = `/greenDill/static/PATROL/patrolTask/taskDetail/abnormalSummary/${fileName}`;
  const aliasPath = `/usr/share/nginx/module-static/PATROL/patrolTask/taskDetail/abnormalSummary/${fileName}`;

  if (fileName === "body.js") {
    assert(
      source.includes("__ADP_PATROL_HIDDEN_DANGER_ACTION_INSTALLED__"),
      `${fileName} must install the hidden-danger action once`
    );
  }
  assert(nginx.includes(`location = ${requestPath} {`), `${fileName} must have an exact Nginx route`);
  assert(nginx.includes(`alias ${aliasPath};`), `${fileName} must map to the restored asset`);
  new Function(source);
}

const hiddenDangerBody = fs.readFileSync(path.join(hiddenDangerAssetRoot, "body.js"), "utf8");
for (const marker of [
  'target.closest("#btn-createTask")',
  'text("PATROL.custom.danger.isCreated")',
  'text("PATROL.custom.danger.created"',
  'CREATE_URL = "/msService/PATROL/patrolTask/taskDetail/createHiddenDanger"',
  "root.createTask = createHiddenDanger",
]) {
  assert(hiddenDangerBody.includes(marker), `PATROL hidden-danger body must implement ${marker}`);
}

for (const fileName of ["body.js", "body-es5.js"]) {
  const assetPath = path.join(resultEditorAssetRoot, fileName);
  const source = fs.readFileSync(assetPath, "utf8");
  const requestPath = `/greenDill/static/PATROL/patrolTask/potrolTask/enteringResultEdit/${fileName}`;
  const aliasPath = `/usr/share/nginx/module-static/PATROL/patrolTask/potrolTask/enteringResultEdit/${fileName}`;

  assert(source.includes("__ADP_PATROL_RESULT_JUDGE_INSTALLED__"), `${fileName} must install judge once`);
  assert(nginx.includes(`location = ${requestPath} {`), `${fileName} must have an exact Nginx route`);
  assert(nginx.includes(`alias ${aliasPath};`), `${fileName} must map to the restored asset`);
  new Function(source);
}

const resultJudgeSandbox = { window: {} };
vm.runInNewContext(
  fs.readFileSync(path.join(resultEditorAssetRoot, "body.js"), "utf8"),
  resultJudgeSandbox
);
const judge = resultJudgeSandbox.window.judge;
const numberType = { id: "PATROL_valueType/number" };
const inputType = { id: "PATROL_editType/input" };
assert.strictEqual(typeof judge, "function", "PATROL result editor must expose window.judge");
assert.strictEqual(judge(numberType, inputType, "15", "10~20"), true, "numeric ranges must pass");
assert.strictEqual(judge(numberType, inputType, "21", "10~20"), false, "numeric ranges must fail");
assert.strictEqual(judge(numberType, inputType, "10", ">=10"), true, "ASCII operators must work");
assert.strictEqual(judge(numberType, inputType, "9", "≥10"), false, "legacy operators must work");
assert.strictEqual(judge(numberType, inputType, "5", "<0|>10"), false, "OR rules must fail cleanly");
assert.strictEqual(judge(numberType, inputType, "11", "<0|>10"), true, "OR rules must pass");
assert.strictEqual(judge(numberType, inputType, "not-a-number", ">0"), false, "invalid values must fail");
assert.strictEqual(judge(numberType, inputType, "", "0~1"), false, "blank values must not become zero");
assert.strictEqual(judge(numberType, inputType, null, "0~1"), false, "null values must not become zero");
assert.strictEqual(judge(numberType, inputType, "1", null), false, "missing ranges must fail safely");
assert.strictEqual(judge({ id: "PATROL_valueType/char" }, {}, " 正常 ", "正常"), true);

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
  'updateState(ITEM_GRID, "workItem", 1',
  'updateState(ITEM_GRID, "workItem", 0',
  'buttonId === "btn-start"',
  "#btn-start",
]) {
  assert(routeBody.includes(marker), `PATROL route body must implement ${marker}`);
}

for (const fileName of ["body.js", "body-es5.js"]) {
  const assetPath = path.join(itemEditorAssetRoot, fileName);
  assert(fs.existsSync(assetPath), `PATROL item-editor compatibility asset must exist: ${fileName}`);
  new Function(fs.readFileSync(assetPath, "utf8"));
}
const itemEditorBody = fs.readFileSync(path.join(itemEditorAssetRoot, "body.js"), "utf8");
for (const marker of [
  "__ADP_PATROL_ITEM_EDITOR_ACTIONS_INSTALLED__",
  "addLine()",
  "insertLine(rowIndex)",
  "deleteLine(indexes.join",
  "moveUpLine()",
  "moveDownLine()",
  'setValueByKey(rowIndex, "workId.id", context.areaId)',
  'setValueByKey(rowIndex, "routeId.id", context.routeId)',
  "deleteWorkItems?workItemIds=",
  "PATROL_patrolRoute_workArea_onsave",
  "closeMenuAfterCurrentClick",
]) {
  assert(itemEditorBody.includes(marker), `PATROL item editor body must implement ${marker}`);
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
const patrolResultEditorBodyIndex = nginx.indexOf(
  "location = /greenDill/static/PATROL/patrolTask/potrolTask/enteringResultEdit/body.js"
);
assert(patrolResultEditorBodyIndex >= 0, "nginx must expose the PATROL result-editor body script");
assert(
  patrolResultEditorBodyIndex < placeholderRouteIndex,
  "PATROL result-editor exact routes must precede the generic body-script fallback"
);
const patrolHiddenDangerBodyIndex = nginx.indexOf(
  "location = /greenDill/static/PATROL/patrolTask/taskDetail/abnormalSummary/body.js"
);
assert(patrolHiddenDangerBodyIndex >= 0, "nginx must expose the PATROL hidden-danger body script");
assert(
  patrolHiddenDangerBodyIndex < placeholderRouteIndex,
  "PATROL hidden-danger exact routes must precede the generic body-script fallback"
);
assert(
  nginx.includes("location = /msService/PATROL/patrolTask/taskDetail/abnormalSummary {") &&
    nginx.includes(
      '<script src="/greenDill/static/PATROL/patrolTask/taskDetail/abnormalSummary/body.js"></script>'
    ),
  "PATROL abnormal-summary HTML must load the hidden-danger compatibility action"
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
const patrolItemEditorBodyIndex = nginx.indexOf(
  "location = /greenDill/static/PATROL/patrolRoute/workArea/workItemPtEdit/body.js"
);
assert(patrolItemEditorBodyIndex >= 0, "nginx must expose the PATROL item-editor body script");
assert(
  patrolItemEditorBodyIndex < placeholderRouteIndex,
  "PATROL item-editor exact routes must precede the generic body-script fallback"
);
assert(
  nginx.includes("location = /msService/PATROL/patrolRoute/workArea/workItemPtEdit {") &&
    nginx.includes(
      '<script src="/greenDill/static/PATROL/patrolRoute/workArea/workItemPtEdit/body.js"></script>'
    ),
  "PATROL item-editor HTML must load the compatibility body script"
);
assert(
  nginx.includes("alias /usr/share/nginx/module-static/PATROL/i18n-value.js;"),
  "PATROL page i18n routes must use the compatibility bundle"
);
const eamRiskPageIndex = nginx.indexOf(
  "location = /msService/EAM/businessConfig/riskHandle/riskRecord {"
);
const genericMsServiceIndex = nginx.indexOf("location ^~ /msService/ {");
assert(eamRiskPageIndex >= 0, "EAM risk-record page must have an exact Nginx route");
assert(
  eamRiskPageIndex < genericMsServiceIndex,
  "EAM risk-record page patch must precede the generic msService proxy"
);
assert(
  nginx.includes('/greenDill/static/scripts/vendors.sesgis.js?v=1677582772048'),
  "EAM risk-record page must load the SESGIS vendor before edit.js"
);
assert(
  hiddenDangerSourcePatch.includes("PATROL_COMPATIBILITY_PENDING"),
  "PATROL source patch must mark compatibility risks as pending"
);
for (const marker of [
  "EAM_1.0.0_businessConfig_riskRecorddg1578550214154",
  "payload text := $eam_risk_grid$",
  "EAM risk-record datagrid JSON is incomplete",
  "SESHRM_riskResource/005",
  "SESHRM.systemEntityname.randon1570600798462",
  "SESHRM.systemCodevalue.randon1589806103444",
  "SESHRM inspection source system code is missing",
  "SESHRM inspection source translation is missing",
]) {
  assert(
    hiddenDangerMigration.includes(marker),
    `PATROL hidden-danger migration must restore ${marker}`
  );
}

const i18nResourceTable = i18nStartupMigration.match(
  /CREATE TABLE IF NOT EXISTS public\.supfusion_i18n_resource\s*\(([\s\S]*?)\);/
);
assert(i18nResourceTable, "i18n bootstrap must define supfusion_i18n_resource");
assert(
  /\bmodifier\s+timestamp\b/.test(i18nResourceTable[1]),
  "i18n resource modifier must match the runtime java.util.Date mapping"
);
assert(
  hiddenDangerMigration.includes("ALTER COLUMN modifier TYPE timestamp without time zone") &&
    hiddenDangerMigration.includes("modifier = CURRENT_TIMESTAMP") &&
    !hiddenDangerMigration.includes("modifier = 'system'"),
  "PATROL hidden-danger migration must repair and write timestamp modifiers"
);
assert(
  womToolbarMigration.includes("modifier = CURRENT_TIMESTAMP") &&
    !womToolbarMigration.includes("modifier = 'system'"),
  "WOM toolbar translations must write timestamp modifiers"
);

console.log("PATROL static asset acceptance: PASS");
