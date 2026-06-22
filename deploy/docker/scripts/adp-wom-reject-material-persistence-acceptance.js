#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { chromium, request } = require("playwright");

const baseUrl = (process.env.ADP_BASE_URL || "http://100.99.133.43:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const headless = process.env.ADP_HEADLESS !== "false";
const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@100.99.133.43";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const nowToken = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${nowToken}_WOM_REJECT_MATERIAL`;
const flowName = process.env.ADP_WOM_REJECT_FLOW || "prepare";
const outputDir =
  process.env.ADP_OUTPUT_DIR ||
  path.join("/tmp", `adp-wom-reject-material-persistence-${nowToken}`);
const outputPath =
  process.env.ADP_WOM_REJECT_MATERIAL_PERSISTENCE_OUTPUT ||
  path.join(outputDir, "wom-reject-material-persistence-results.json");
const action =
  process.env.ADP_WOM_REJECT_ACTION ||
  (flowName === "materia" || flowName === "batch" ? "effective" : "delete");

const FLOW_PROFILES = {
  prepare: {
    route: "/msService/WOM/rejectMaterilal/rejectMaterial/prePareRejectEdit",
    saveApi: "/msService/WOM/rejectMaterilal/rejectMaterial/prePareRejectEdit/save",
    submitApi: "/msService/WOM/rejectMaterilal/rejectMaterial/prePareRejectEdit/submit",
    deleteApi: "/msService/WOM/rejectMaterilal/rejectMaterial/delete",
    deploymentId: "6579611733721088",
    viewCode: "WOM_1.0.0_rejectMaterilal_prePareRejectEdit",
    processKey: "prePareRejectFlow",
    rejectTypeId: "WOM_rejectType/forPrepare",
    startActivityName: "start_duviwfq",
    effectiveActivityName: "TaskEvent_17rdgt4",
    effectiveOutcome: "SequenceFlow_1nu8kl7",
    effectiveOutcomeDes: "生效",
  },
  batch: {
    route: "/msService/WOM/rejectMaterilal/rejectMaterial/batchRejectEdit",
    saveApi: "/msService/WOM/rejectMaterilal/rejectMaterial/batchRejectEdit/save",
    submitApi: "/msService/WOM/rejectMaterilal/rejectMaterial/batchRejectEdit/submit",
    deleteApi: "/msService/WOM/rejectMaterilal/rejectMaterial/delete",
    deploymentId: "6579611733229568",
    viewCode: "WOM_1.0.0_rejectMaterilal_batchRejectEdit",
    processKey: "batchRejectFlow",
    rejectTypeId: "WOM_rejectType/forBatch",
    startActivityName: "start_6964gxi",
    effectiveActivityName: "TaskEvent_0nsp5v4",
    effectiveOutcome: "SequenceFlow_1dghow0",
    effectiveOutcomeDes: "生效",
    detailDgName: "dg1581993495790",
    rejectReasonId: "WOM_rejectReason/wrong",
    rejectNum: 2,
    initialReturnNum: 0,
    expectedReturnNum: 2,
  },
  materia: {
    route: "/msService/WOM/rejectMaterilal/rejectMaterial/materiaRejectEdit",
    saveApi: "/msService/WOM/rejectMaterilal/rejectMaterial/materiaRejectEdit/save",
    firstSubmitApi: "/msService/WOM/rejectMaterilal/rejectMaterial/materiaRejectEdit/submit",
    secondSubmitApi: "/msService/WOM/rejectMaterilal/rejectMaterial/materiaEditableEdit/submit",
    deleteApi: "/msService/WOM/rejectMaterilal/rejectMaterial/delete",
    deploymentId: "6579611733491712",
    viewCode: "WOM_1.0.0_rejectMaterilal_materiaRejectEdit",
    receiveViewCode: "WOM_1.0.0_rejectMaterilal_materiaEditableEdit",
    processKey: "materiaReject",
    rejectTypeId: "WOM_rejectType/factoryMateria",
    startActivityName: "start_cqxg3gf",
    firstActivityName: "TaskEvent_0hf8fwb",
    firstOutcome: "SequenceFlow_1ubtbie",
    firstOutcomeDes: "提交",
    secondActivityName: "TaskEvent_0t6vfw8",
    secondOutcome: "SequenceFlow_0igvfiq",
    secondOutcomeDes: "生效",
    detailDgName: "dg1597224100590",
    rejectReasonId: "WOM_rejectReason/wrong",
    rejectNum: 2,
    initialQuantity: 10,
    expectedFinalQuantity: 8,
  },
};

if (!Object.prototype.hasOwnProperty.call(FLOW_PROFILES, flowName)) {
  throw new Error(`Unsupported ADP_WOM_REJECT_FLOW=${flowName}`);
}

const profile = {
  ...FLOW_PROFILES[flowName],
  deploymentId: process.env.ADP_WOM_REJECT_DEPLOYMENT_ID || FLOW_PROFILES[flowName].deploymentId,
};

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function shellQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
}

function sqlLiteral(value) {
  return `'${String(value).replace(/'/g, "''")}'`;
}

function runRemote(command, input) {
  return execFileSync(
    "ssh",
    [
      "-o",
      "BatchMode=yes",
      "-o",
      "StrictHostKeyChecking=no",
      "-o",
      "UserKnownHostsFile=/dev/null",
      dbSshTarget,
      command,
    ],
    { input, encoding: "utf8", stdio: ["pipe", "pipe", "pipe"] }
  );
}

function runSql(sql) {
  const command = [
    "docker",
    "exec",
    "-i",
    shellQuote(dbContainer),
    "psql",
    "-U",
    shellQuote(dbUser),
    "-d",
    shellQuote(dbName),
    "-v",
    "ON_ERROR_STOP=1",
    "-AtF",
    shellQuote("|"),
  ].join(" ");
  return runRemote(command, sql).trim();
}

function parseRows(raw) {
  return raw
    .split(/\r?\n/)
    .filter(Boolean)
    .map((line) => line.split("|"));
}

async function readJsonSafe(response) {
  const text = await response.text();
  try {
    return { json: JSON.parse(text), text };
  } catch (_error) {
    return { json: null, text };
  }
}

function findTicket(payload) {
  const candidates = [
    payload && payload.ticket,
    payload && payload.access_token,
    payload && payload.token,
    payload && payload.data && payload.data.ticket,
    payload && payload.data && payload.data.access_token,
    payload && payload.data && payload.data.token,
    payload && payload.result && payload.result.ticket,
    payload && payload.result && payload.result.access_token,
    payload && payload.result && payload.result.token,
  ];
  return candidates.find((value) => typeof value === "string" && value.length > 20);
}

async function login(api) {
  const attempts = [
    { userName: username, password, clientId: "pc_dt" },
    { username, password, clientId: "pc_dt" },
  ];
  const errors = [];
  for (const body of attempts) {
    const response = await api.post(`${baseUrl}/inter-api/auth/login`, {
      data: body,
      headers: {
        Accept: "application/json, text/plain, */*",
        "Content-Type": "application/json;charset=UTF-8",
      },
    });
    const parsed = await readJsonSafe(response);
    const ticket = response.ok() ? findTicket(parsed.json) : null;
    if (ticket) {
      return { ticket, status: response.status() };
    }
    errors.push({ status: response.status(), body: parsed.text.slice(0, 500) });
  }
  throw new Error(`Login failed for ${username}: ${JSON.stringify(errors)}`);
}

function generatedLongId() {
  return Math.floor(Date.now() * 1000 + Math.floor(Math.random() * 1000));
}

function buildMateriaFixture(companyId) {
  return {
    factoryMateriaId: Number(process.env.ADP_WOM_FACTORY_MATERIA_ID || generatedLongId()),
    companyId: Number(process.env.ADP_WOM_COMPANY_ID || companyId),
    initialQuantity: Number(profile.initialQuantity),
    rejectNum: Number(profile.rejectNum),
    expectedFinalQuantity: Number(profile.expectedFinalQuantity),
  };
}

function buildBatchFixture(companyId) {
  const baseId = Number(process.env.ADP_WOM_BATCH_BASE_ID || generatedLongId());
  return {
    batchMatNeedId: Number(process.env.ADP_WOM_BATCH_MAT_NEED_ID || baseId),
    batchMaterilId: Number(process.env.ADP_WOM_BATCH_MATERIL_ID || baseId + 1),
    batMaterilPartId: Number(process.env.ADP_WOM_BAT_MATERIL_PART_ID || baseId + 2),
    companyId: Number(process.env.ADP_WOM_COMPANY_ID || companyId),
    materialId: Number(process.env.ADP_WOM_BATCH_MATERIAL_ID || 1781551145996252),
    initialReturnNum: Number(profile.initialReturnNum),
    rejectNum: Number(profile.rejectNum),
    expectedReturnNum: Number(profile.expectedReturnNum),
  };
}

function materiaDetailRows(fixture) {
  return [
    {
      id: null,
      virtualId: `virtual_${nowToken}`,
      valid: true,
      factoryMateriaId: {
        id: fixture.factoryMateriaId,
        availiQuantity: fixture.initialQuantity,
      },
      rejectNum: fixture.rejectNum,
      rejectReason: { id: profile.rejectReasonId },
      remark: marker,
    },
  ];
}

function batchDetailRows(fixture) {
  return [
    {
      id: null,
      virtualId: `virtual_${nowToken}`,
      valid: true,
      batchingPartId: {
        id: fixture.batMaterilPartId,
        offerNum: 10,
        putinNum: 0,
      },
      materialId: { id: fixture.materialId },
      materialBatchNum: marker,
      rejectNum: fixture.rejectNum,
      rejectReason: { id: profile.rejectReasonId },
      remark: marker,
    },
  ];
}

function savePayload(fixture) {
  const payload = {
    rejectMaterial: {
      viewCode: profile.viewCode,
      rejectApplyDate: new Date().toISOString(),
      rejectApplyStaff: { id: 1 },
      rejectType: { id: profile.rejectTypeId },
      remark: marker,
    },
    viewCode: profile.viewCode,
    operateType: "save",
    deploymentId: profile.deploymentId,
    activityName: profile.startActivityName,
    workFlowVar: {
      outcome: "save",
      outcomeType: "save",
      deploymentId: profile.deploymentId,
      entityCode: "WOM_1.0.0_rejectMaterilal",
    },
    dgList: {},
    dgDeletedIds: {},
    uploadFileFormMap: [],
  };
  if (flowName === "materia") {
    payload.dgList = {
      [profile.detailDgName]: JSON.stringify(materiaDetailRows(fixture)),
    };
  } else if (flowName === "batch") {
    payload.dgList = {
      [profile.detailDgName]: JSON.stringify(batchDetailRows(fixture)),
    };
  }
  return payload;
}

function submitPayload(created, pendingId, step) {
  const submitViewCode = step.viewCode || profile.viewCode;
  return {
    id: Number(created.id),
    rejectMaterial: {
      id: Number(created.id),
      version: Number(created.version),
      viewCode: submitViewCode,
      rejectApplyDate: new Date().toISOString(),
      rejectApplyStaff: { id: 1 },
      rejectType: { id: profile.rejectTypeId },
      remark: `${marker}_${step.remarkSuffix || "SUBMIT"}`,
    },
    viewCode: submitViewCode,
    operateType: "submit",
    deploymentId: profile.deploymentId,
    pendingId,
    activityName: step.activityName,
    pendingActivityType: "4",
    webSignetFlag: false,
    workFlowVar: {
      outcome: step.outcome,
      outcomeType: "normal",
      outcomeDes: step.outcomeDes,
      outcomeDesZhCn: step.outcomeDes,
      outcomeMap: [
        {
          outcome: step.outcome,
          dec: step.outcomeDes,
          type: "normal",
          assignUser: "",
        },
      ],
      deploymentId: profile.deploymentId,
      entityCode: "WOM_1.0.0_rejectMaterilal",
    },
    dgList: {},
    dgDeletedIds: {},
    uploadFileFormMap: [],
  };
}

function rejectSql(id) {
  return `
SELECT id, version, valid, status, table_info_id, table_no, deployment_id,
       process_key, process_version, reject_apply_staff, reject_type, remark
FROM public.wom_reject_materials
WHERE id = ${id}
ORDER BY id DESC;
`;
}

function dealInfoSql(row) {
  return `
SELECT id,
       coalesce(activity_name, ''),
       coalesce(outcome, ''),
       coalesce(outcome_des, ''),
       coalesce(outcome_des_zh_cn, ''),
       coalesce(process_key, ''),
       coalesce(process_version::text, ''),
       coalesce(user_id::text, ''),
       coalesce(table_info_id::text, ''),
       coalesce(main_obj::text, '')
FROM public.wom_reject_materials_di
WHERE table_info_id = ${row.tableInfoId}
   OR main_obj = ${row.id}
ORDER BY id;
`;
}

function sideEffectSql(row) {
  return `
SELECT 'pending_count', count(*)
FROM public.wfm_task_pending
WHERE process_key = ${sqlLiteral(profile.processKey)}
  AND (
      model_id = ${row.id}
      OR table_info_id = ${row.tableInfoId}
      OR table_no = ${sqlLiteral(row.tableNo)}
  );
SELECT 'di_count', count(*)
FROM public.wom_reject_materials_di
WHERE table_info_id = ${row.tableInfoId}
   OR main_obj = ${row.id};
SELECT 'sv_count', count(*)
FROM public.wom_reject_materials_sv
WHERE table_info_id = ${row.tableInfoId};
`;
}

function pendingSql(row, activityName) {
  return `
SELECT id,
       coalesce(activity_name, ''),
       coalesce(task_description, ''),
       coalesce(process_key, ''),
       coalesce(model_id::text, ''),
       coalesce(table_info_id::text, ''),
       coalesce(table_no, '')
FROM public.wfm_task_pending
WHERE process_key = ${sqlLiteral(profile.processKey)}
  AND activity_name = ${sqlLiteral(activityName)}
  AND (
      model_id = ${row.id}
      OR table_info_id = ${row.tableInfoId}
      OR table_no = ${sqlLiteral(row.tableNo)}
  )
ORDER BY id DESC
LIMIT 1;
`;
}

function materiaFixtureInsertSql(fixture) {
  return `
INSERT INTO public.hm_factory_materias
  (id, version, valid, cid, availi_quantity, charparama, remark, create_staff_id, create_time, modify_staff_id, modify_time)
VALUES
  (${fixture.factoryMateriaId}, 0, 1, ${fixture.companyId}, ${fixture.initialQuantity}, ${sqlLiteral(marker)}, NULL, 1, now(), 1, now())
ON CONFLICT (id) DO UPDATE
SET valid = EXCLUDED.valid,
    cid = EXCLUDED.cid,
    availi_quantity = EXCLUDED.availi_quantity,
    charparama = EXCLUDED.charparama,
    remark = NULL,
    modify_staff_id = EXCLUDED.modify_staff_id,
    modify_time = EXCLUDED.modify_time
RETURNING id, valid, cid, availi_quantity, coalesce(charparama, ''), coalesce(remark, '');
`;
}

function batchFixtureInsertSql(fixture) {
  return `
WITH need_upsert AS (
  INSERT INTO public.wom_batch_mat_needs
    (id, version, valid, cid, material_id, plan_num, offer_num, return_num,
     produce_batch_num, charparama, remark, create_staff_id, create_time,
     modify_staff_id, modify_time)
  VALUES
    (${fixture.batchMatNeedId}, 0, true, ${fixture.companyId}, ${fixture.materialId},
     10, 10, ${fixture.initialReturnNum}, ${sqlLiteral(marker)},
     ${sqlLiteral(marker)}, NULL, 1, now(), 1, now())
  ON CONFLICT (id) DO UPDATE
  SET valid = EXCLUDED.valid,
      cid = EXCLUDED.cid,
      material_id = EXCLUDED.material_id,
      plan_num = EXCLUDED.plan_num,
      offer_num = EXCLUDED.offer_num,
      return_num = EXCLUDED.return_num,
      produce_batch_num = EXCLUDED.produce_batch_num,
      charparama = EXCLUDED.charparama,
      remark = NULL,
      modify_staff_id = EXCLUDED.modify_staff_id,
      modify_time = EXCLUDED.modify_time
  RETURNING id
), head_upsert AS (
  INSERT INTO public.wom_batch_materils
    (id, version, valid, cid, need_id, product_id, need_num, offer_num, return_num,
     produce_batch_num, charparama, remark, create_staff_id, create_time,
     modify_staff_id, modify_time)
  VALUES
    (${fixture.batchMaterilId}, 0, true, ${fixture.companyId}, ${fixture.batchMatNeedId},
     ${fixture.materialId}, 10, 10, ${fixture.initialReturnNum}, ${sqlLiteral(marker)},
     ${sqlLiteral(marker)}, NULL, 1, now(), 1, now())
  ON CONFLICT (id) DO UPDATE
  SET valid = EXCLUDED.valid,
      cid = EXCLUDED.cid,
      need_id = EXCLUDED.need_id,
      product_id = EXCLUDED.product_id,
      need_num = EXCLUDED.need_num,
      offer_num = EXCLUDED.offer_num,
      return_num = EXCLUDED.return_num,
      produce_batch_num = EXCLUDED.produce_batch_num,
      charparama = EXCLUDED.charparama,
      remark = NULL,
      modify_staff_id = EXCLUDED.modify_staff_id,
      modify_time = EXCLUDED.modify_time
  RETURNING id
), part_upsert AS (
  INSERT INTO public.wom_bat_materil_parts
    (id, version, valid, cid, head_id, batching_need_id, material_id,
     material_batch_num, offer_num, putin_num, return_num, retirement_state,
     charparama, remark, create_staff_id, create_time, modify_staff_id, modify_time)
  VALUES
    (${fixture.batMaterilPartId}, 0, true, ${fixture.companyId}, ${fixture.batchMaterilId},
     ${fixture.batchMatNeedId}, ${fixture.materialId}, ${sqlLiteral(marker)}, 10, 0,
     ${fixture.initialReturnNum}, NULL, ${sqlLiteral(marker)}, NULL, 1, now(), 1, now())
  ON CONFLICT (id) DO UPDATE
  SET valid = EXCLUDED.valid,
      cid = EXCLUDED.cid,
      head_id = EXCLUDED.head_id,
      batching_need_id = EXCLUDED.batching_need_id,
      material_id = EXCLUDED.material_id,
      material_batch_num = EXCLUDED.material_batch_num,
      offer_num = EXCLUDED.offer_num,
      putin_num = EXCLUDED.putin_num,
      return_num = EXCLUDED.return_num,
      retirement_state = EXCLUDED.retirement_state,
      charparama = EXCLUDED.charparama,
      remark = NULL,
      modify_staff_id = EXCLUDED.modify_staff_id,
      modify_time = EXCLUDED.modify_time
  RETURNING id
)
SELECT 'need', id FROM need_upsert
UNION ALL
SELECT 'head', id FROM head_upsert
UNION ALL
SELECT 'part', id FROM part_upsert
ORDER BY 1;
`;
}

function companyIdSql() {
  return `
SELECT id, coalesce(valid::text, ''), coalesce(name, ''), coalesce(code, '')
FROM public.base_company
WHERE valid = 1
ORDER BY id
LIMIT 1;
`;
}

function factoryMateriaSql(fixture) {
  return `
SELECT id, valid, cid, availi_quantity, coalesce(charparama, ''), coalesce(remark, '')
FROM public.hm_factory_materias
WHERE id = ${fixture.factoryMateriaId};
`;
}

function batchFixtureSql(fixture) {
  return `
SELECT 'need',
       id,
       coalesce(valid::text, ''),
       coalesce(return_num::text, ''),
       coalesce(charparama, '')
FROM public.wom_batch_mat_needs
WHERE id = ${fixture.batchMatNeedId};
SELECT 'head',
       id,
       coalesce(valid::text, ''),
       coalesce(need_id::text, ''),
       coalesce(return_num::text, ''),
       coalesce(charparama, '')
FROM public.wom_batch_materils
WHERE id = ${fixture.batchMaterilId};
SELECT 'part',
       id,
       coalesce(valid::text, ''),
       coalesce(head_id::text, ''),
       coalesce(batching_need_id::text, ''),
       coalesce(return_num::text, ''),
       coalesce(retirement_state, ''),
       coalesce(charparama, '')
FROM public.wom_bat_materil_parts
WHERE id = ${fixture.batMaterilPartId};
`;
}

function rejectDetailSql(row) {
  return `
SELECT id,
       coalesce(valid::text, ''),
       coalesce(head_id::text, ''),
       coalesce(batching_part_id::text, ''),
       coalesce(factory_materia_id::text, ''),
       coalesce(material_batch_num, ''),
       coalesce(reject_num::text, ''),
       coalesce(reject_reason, ''),
       coalesce(remark, '')
FROM public.wom_rejct_matal_parts
WHERE head_id = ${row.id}
ORDER BY id;
`;
}

function firstRejectRow(raw) {
  const rows = parseRows(raw);
  if (rows.length !== 1) {
    throw new Error(`Expected exactly one reject material row, got ${rows.length}: ${raw}`);
  }
  const [
    id,
    version,
    valid,
    status,
    tableInfoId,
    tableNo,
    deployment,
    key,
    processVersion,
    rejectApplyStaff,
    rejectType,
    remark,
  ] = rows[0];
  return {
    id,
    version,
    valid,
    status,
    tableInfoId,
    tableNo,
    deploymentId: deployment,
    processKey: key,
    processVersion,
    rejectApplyStaff,
    rejectType,
    remark,
  };
}

function firstPendingRow(raw) {
  const rows = parseRows(raw);
  if (rows.length !== 1) {
    throw new Error(`Expected exactly one pending task row, got ${rows.length}: ${raw}`);
  }
  const [id, activityName, taskDescription, key, modelId, tableInfoId, tableNo] = rows[0];
  return { id, activityName, taskDescription, processKey: key, modelId, tableInfoId, tableNo };
}

function firstFactoryMateriaRow(raw) {
  const rows = parseRows(raw);
  if (rows.length !== 1) {
    throw new Error(`Expected exactly one factory materia row, got ${rows.length}: ${raw}`);
  }
  const [id, valid, cid, availiQuantity, charparama, remark] = rows[0];
  return {
    id,
    valid,
    cid,
    availiQuantity: Number(availiQuantity),
    charparama,
    remark,
  };
}

function firstCompanyRow(raw) {
  const rows = parseRows(raw);
  if (rows.length !== 1) {
    throw new Error(`Expected exactly one valid company row, got ${rows.length}: ${raw}`);
  }
  const [id, valid, name, code] = rows[0];
  return { id, valid, name, code };
}

function batchFixtureRows(raw) {
  const rows = parseRows(raw);
  const byKind = Object.fromEntries(rows.map((row) => [row[0], row]));
  if (!byKind.need || !byKind.head || !byKind.part) {
    throw new Error(`Expected need/head/part batch fixture rows, got: ${raw}`);
  }
  return {
    need: {
      id: byKind.need[1],
      valid: byKind.need[2],
      returnNum: Number(byKind.need[3]),
      marker: byKind.need[4],
    },
    head: {
      id: byKind.head[1],
      valid: byKind.head[2],
      needId: byKind.head[3],
      returnNum: Number(byKind.head[4]),
      marker: byKind.head[5],
    },
    part: {
      id: byKind.part[1],
      valid: byKind.part[2],
      headId: byKind.part[3],
      needId: byKind.part[4],
      returnNum: Number(byKind.part[5]),
      retirementState: byKind.part[6],
      marker: byKind.part[7],
    },
  };
}

async function browserFetch(page, method, apiPath, payload) {
  return page.evaluate(
    async ({ method: requestMethod, apiPath: pathValue, payload: bodyValue }) => {
      const response = await fetch(pathValue, {
        method: requestMethod,
        headers: {
          Accept: "application/json, text/plain, */*",
          "Content-Type": "application/json;charset=UTF-8",
        },
        body: bodyValue === undefined ? undefined : JSON.stringify(bodyValue),
      });
      const text = await response.text();
      let json = null;
      try {
        json = JSON.parse(text);
      } catch (_error) {
        json = null;
      }
      return { status: response.status, ok: response.ok, body: text, json };
    },
    { method, apiPath, payload }
  );
}

async function runBrowser(ticket, evidence) {
  const browser = await chromium.launch({ headless });
  try {
    const context = await browser.newContext({
      baseURL: baseUrl,
      ignoreHTTPSErrors: true,
      viewport: { width: 1600, height: 1000 },
      extraHTTPHeaders: { Authorization: `Bearer ${ticket}` },
    });
    await context.addCookies([
      { name: "suposTicket", value: ticket, url: baseUrl },
      { name: "SUPOS_TICKET", value: ticket, url: baseUrl },
    ]);
    await context.addInitScript((token) => {
      window.localStorage.clear();
      window.sessionStorage.clear();
      ["suposTicket", "SUPOS_TICKET", "token", "ticket"].forEach((key) => {
        window.localStorage.setItem(key, token);
        window.sessionStorage.setItem(key, token);
      });
    }, ticket);

    const page = await context.newPage();
    page.on("console", (message) => {
      if (["error", "warning"].includes(message.type())) {
        evidence.frontend.console.push({ type: message.type(), text: message.text() });
      }
    });
    page.on("pageerror", (error) => evidence.frontend.pageErrors.push(error.message));
    page.on("requestfailed", (requestItem) => {
      evidence.frontend.requestFailures.push({
        method: requestItem.method(),
        url: requestItem.url(),
        failure: requestItem.failure() && requestItem.failure().errorText,
      });
    });
    page.on("request", (requestItem) => {
      const url = requestItem.url();
      if (/rejectMaterilal|layoutJson/.test(url)) {
        evidence.frontend.requests.push({
          method: requestItem.method(),
          url,
          postData: requestItem.postData(),
        });
      }
    });
    page.on("response", async (response) => {
      const url = response.url();
      if (!/rejectMaterilal|layoutJson/.test(url)) {
        return;
      }
      let body = "";
      try {
        body = (await response.text()).slice(0, 4000);
      } catch (_error) {
        body = "";
      }
      evidence.frontend.responses.push({
        method: response.request().method(),
        url,
        status: response.status(),
        body,
      });
    });

    const sameOriginResponse = await page.goto("/greenDill/static/scripts/styles.js", {
      waitUntil: "domcontentloaded",
      timeout: 45000,
    });
    evidence.frontend.sameOrigin = {
      route: "/greenDill/static/scripts/styles.js",
      status: sameOriginResponse ? sameOriginResponse.status() : null,
      title: await page.title().catch(() => ""),
    };

    evidence.frontend.editRoute = await page.evaluate(async (pathValue) => {
      const response = await fetch(pathValue, { headers: { Accept: "text/html,*/*" } });
      const body = await response.text();
      return {
        route: pathValue,
        status: response.status,
        ok: response.ok,
        bodySample: body.slice(0, 1000),
      };
    }, profile.route);
    if (evidence.frontend.editRoute.status !== 200) {
      throw new Error(`Reject edit route failed: ${JSON.stringify(evidence.frontend.editRoute)}`);
    }

    let fixture = null;
    if (flowName === "materia" || flowName === "batch") {
      const companySql = companyIdSql();
      const companyRaw = runSql(companySql);
      const company = firstCompanyRow(companyRaw);
      evidence.testFixture.company = {
        sql: companySql.trim(),
        raw: companyRaw,
        row: company,
      };
      fixture =
        flowName === "materia"
          ? buildMateriaFixture(Number(company.id))
          : buildBatchFixture(Number(company.id));
    }
    if (fixture && flowName === "materia") {
      evidence.testFixture.factoryMateria = {
        insertSql: materiaFixtureInsertSql(fixture).trim(),
        insertRaw: runSql(materiaFixtureInsertSql(fixture)),
        beforeSql: factoryMateriaSql(fixture).trim(),
        beforeRaw: runSql(factoryMateriaSql(fixture)),
      };
      evidence.testFixture.factoryMateria.before = firstFactoryMateriaRow(
        evidence.testFixture.factoryMateria.beforeRaw
      );
    } else if (fixture && flowName === "batch") {
      evidence.testFixture.batch = {
        insertSql: batchFixtureInsertSql(fixture).trim(),
        insertRaw: runSql(batchFixtureInsertSql(fixture)),
        beforeSql: batchFixtureSql(fixture).trim(),
        beforeRaw: runSql(batchFixtureSql(fixture)),
      };
      evidence.testFixture.batch.before = batchFixtureRows(evidence.testFixture.batch.beforeRaw);
    }

    evidence.operations.save.payload = savePayload(fixture);
    evidence.operations.save.response = await browserFetch(
      page,
      "POST",
      profile.saveApi,
      evidence.operations.save.payload
    );
    if (
      evidence.operations.save.response.status !== 200 ||
      !evidence.operations.save.response.json ||
      evidence.operations.save.response.json.code !== 200 ||
      !evidence.operations.save.response.json.data ||
      !evidence.operations.save.response.json.data.id
    ) {
      throw new Error(`Save failed: ${JSON.stringify(evidence.operations.save.response)}`);
    }

    const createdId = evidence.operations.save.response.json.data.id;
    const createdRaw = runSql(rejectSql(createdId));
    const created = firstRejectRow(createdRaw);
    evidence.database.afterSave = {
      rejectSql: rejectSql(createdId).trim(),
      rejectRaw: createdRaw,
      row: created,
      sideEffectSql: sideEffectSql(created).trim(),
      sideEffectsRaw: runSql(sideEffectSql(created)),
    };
    if (created.valid !== "t") {
      throw new Error(`Saved reject material is not valid=true: ${createdRaw}`);
    }
    if (created.processKey !== profile.processKey) {
      throw new Error(`Unexpected process key after save: ${createdRaw}`);
    }
    if (!evidence.database.afterSave.sideEffectsRaw.includes("pending_count|1")) {
      throw new Error(`Expected one pending task after save: ${evidence.database.afterSave.sideEffectsRaw}`);
    }
    if (!evidence.database.afterSave.sideEffectsRaw.includes("di_count|1")) {
      throw new Error(`Expected one deal-info row after save: ${evidence.database.afterSave.sideEffectsRaw}`);
    }
    if (flowName === "materia") {
      evidence.database.afterSave.detailSql = rejectDetailSql(created).trim();
      evidence.database.afterSave.detailRaw = runSql(rejectDetailSql(created));
      if (!evidence.database.afterSave.detailRaw.includes(String(fixture.factoryMateriaId))) {
        throw new Error(`Materia detail row was not persisted: ${evidence.database.afterSave.detailRaw}`);
      }
      if (!evidence.database.afterSave.detailRaw.includes(String(fixture.rejectNum))) {
        throw new Error(`Materia detail reject_num was not persisted: ${evidence.database.afterSave.detailRaw}`);
      }
    } else if (flowName === "batch") {
      evidence.database.afterSave.detailSql = rejectDetailSql(created).trim();
      evidence.database.afterSave.detailRaw = runSql(rejectDetailSql(created));
      if (!evidence.database.afterSave.detailRaw.includes(String(fixture.batMaterilPartId))) {
        throw new Error(`Batch detail row was not persisted: ${evidence.database.afterSave.detailRaw}`);
      }
      if (!evidence.database.afterSave.detailRaw.includes(String(fixture.rejectNum))) {
        throw new Error(`Batch detail reject_num was not persisted: ${evidence.database.afterSave.detailRaw}`);
      }
    }

    if (action === "effective") {
      const pendingId = evidence.operations.save.response.json.data.pendingId;
      let effective = null;
      if (flowName === "materia") {
        const firstSubmitPath = `${profile.firstSubmitApi}?id=${created.id}`;
        evidence.operations.submit = { method: "POST", api: firstSubmitPath };
        evidence.operations.submit.payload = submitPayload(created, pendingId, {
          activityName: profile.firstActivityName,
          outcome: profile.firstOutcome,
          outcomeDes: profile.firstOutcomeDes,
          remarkSuffix: "SUBMIT",
        });
        evidence.operations.submit.response = await browserFetch(
          page,
          "POST",
          firstSubmitPath,
          evidence.operations.submit.payload
        );
        if (
          evidence.operations.submit.response.status !== 200 ||
          !evidence.operations.submit.response.json ||
          evidence.operations.submit.response.json.code !== 200 ||
          !evidence.operations.submit.response.json.data ||
          evidence.operations.submit.response.json.data.success !== true
        ) {
          throw new Error(`Materia first submit failed: ${JSON.stringify(evidence.operations.submit.response)}`);
        }

        const firstSubmitRaw = runSql(rejectSql(created.id));
        const firstSubmit = firstRejectRow(firstSubmitRaw);
        const receivePendingRaw = runSql(pendingSql(firstSubmit, profile.secondActivityName));
        const receivePending = firstPendingRow(receivePendingRaw);
        evidence.database.afterFirstSubmit = {
          rejectSql: rejectSql(created.id).trim(),
          rejectRaw: firstSubmitRaw,
          row: firstSubmit,
          pendingSql: pendingSql(firstSubmit, profile.secondActivityName).trim(),
          pendingRaw: receivePendingRaw,
          pending: receivePending,
          sideEffectSql: sideEffectSql(firstSubmit).trim(),
          sideEffectsRaw: runSql(sideEffectSql(firstSubmit)),
          dealInfoSql: dealInfoSql(firstSubmit).trim(),
          dealInfoRaw: runSql(dealInfoSql(firstSubmit)),
        };
        if (!evidence.database.afterFirstSubmit.dealInfoRaw.includes(profile.firstOutcome)) {
          throw new Error(`Materia first submit deal-info outcome was not persisted: ${evidence.database.afterFirstSubmit.dealInfoRaw}`);
        }
        if (!evidence.database.afterFirstSubmit.sideEffectsRaw.includes("pending_count|1")) {
          throw new Error(`Materia receive pending task was not created: ${evidence.database.afterFirstSubmit.sideEffectsRaw}`);
        }

        const secondSubmitPath = `${profile.secondSubmitApi}?id=${firstSubmit.id}`;
        evidence.operations.receiveSubmit = { method: "POST", api: secondSubmitPath };
        evidence.operations.receiveSubmit.payload = submitPayload(firstSubmit, Number(receivePending.id), {
          viewCode: profile.receiveViewCode,
          activityName: profile.secondActivityName,
          outcome: profile.secondOutcome,
          outcomeDes: profile.secondOutcomeDes,
          remarkSuffix: "EFFECTIVE",
        });
        evidence.operations.receiveSubmit.response = await browserFetch(
          page,
          "POST",
          secondSubmitPath,
          evidence.operations.receiveSubmit.payload
        );
        if (
          evidence.operations.receiveSubmit.response.status !== 200 ||
          !evidence.operations.receiveSubmit.response.json ||
          evidence.operations.receiveSubmit.response.json.code !== 200 ||
          !evidence.operations.receiveSubmit.response.json.data ||
          evidence.operations.receiveSubmit.response.json.data.success !== true
        ) {
          throw new Error(`Materia receive submit failed: ${JSON.stringify(evidence.operations.receiveSubmit.response)}`);
        }
      } else {
        const submitPath = `${profile.submitApi}?id=${created.id}`;
        evidence.operations.submit = { method: "POST", api: submitPath };
        evidence.operations.submit.payload = submitPayload(created, pendingId, {
          activityName: profile.effectiveActivityName,
          outcome: profile.effectiveOutcome,
          outcomeDes: profile.effectiveOutcomeDes,
          remarkSuffix: "EFFECTIVE",
        });
        evidence.operations.submit.response = await browserFetch(
          page,
          "POST",
          submitPath,
          evidence.operations.submit.payload
        );
        if (
          evidence.operations.submit.response.status !== 200 ||
          !evidence.operations.submit.response.json ||
          evidence.operations.submit.response.json.code !== 200 ||
          !evidence.operations.submit.response.json.data ||
          evidence.operations.submit.response.json.data.success !== true
        ) {
          throw new Error(`Submit effective failed: ${JSON.stringify(evidence.operations.submit.response)}`);
        }
      }

      const effectiveRaw = runSql(rejectSql(created.id));
      effective = firstRejectRow(effectiveRaw);
      evidence.database.afterEffective = {
        rejectSql: rejectSql(created.id).trim(),
        rejectRaw: effectiveRaw,
        row: effective,
        sideEffectSql: sideEffectSql(effective).trim(),
        sideEffectsRaw: runSql(sideEffectSql(effective)),
        dealInfoSql: dealInfoSql(effective).trim(),
        dealInfoRaw: runSql(dealInfoSql(effective)),
      };
      if (effective.valid !== "t") {
        throw new Error(`Effective reject material should remain valid=true: ${effectiveRaw}`);
      }
      if (effective.status !== "99") {
        throw new Error(`Expected status=99 after effective submit: ${effectiveRaw}`);
      }
      if (!evidence.database.afterEffective.sideEffectsRaw.includes("pending_count|0")) {
        throw new Error(`Pending tasks were not cleared after effective submit: ${evidence.database.afterEffective.sideEffectsRaw}`);
      }
      const expectedEffectiveOutcome = flowName === "materia" ? profile.secondOutcome : profile.effectiveOutcome;
      if (!evidence.database.afterEffective.dealInfoRaw.includes(expectedEffectiveOutcome)) {
        throw new Error(`Effective deal-info outcome was not persisted: ${evidence.database.afterEffective.dealInfoRaw}`);
      }
      if (flowName === "materia") {
        evidence.database.afterEffective.detailSql = rejectDetailSql(effective).trim();
        evidence.database.afterEffective.detailRaw = runSql(rejectDetailSql(effective));
        evidence.testFixture.factoryMateria.afterSql = factoryMateriaSql(fixture).trim();
        evidence.testFixture.factoryMateria.afterRaw = runSql(factoryMateriaSql(fixture));
        evidence.testFixture.factoryMateria.after = firstFactoryMateriaRow(
          evidence.testFixture.factoryMateria.afterRaw
        );
        if (evidence.testFixture.factoryMateria.after.availiQuantity !== fixture.expectedFinalQuantity) {
          throw new Error(
            `Expected factory materia availi_quantity=${fixture.expectedFinalQuantity}, got ${evidence.testFixture.factoryMateria.afterRaw}`
          );
        }
        if (!evidence.database.afterEffective.dealInfoRaw.includes(profile.firstOutcome)) {
          throw new Error(`Materia first submit outcome disappeared from deal-info: ${evidence.database.afterEffective.dealInfoRaw}`);
        }
      } else if (flowName === "batch") {
        evidence.database.afterEffective.detailSql = rejectDetailSql(effective).trim();
        evidence.database.afterEffective.detailRaw = runSql(rejectDetailSql(effective));
        evidence.testFixture.batch.afterSql = batchFixtureSql(fixture).trim();
        evidence.testFixture.batch.afterRaw = runSql(batchFixtureSql(fixture));
        evidence.testFixture.batch.after = batchFixtureRows(evidence.testFixture.batch.afterRaw);
        if (evidence.testFixture.batch.after.need.returnNum !== fixture.expectedReturnNum) {
          throw new Error(`Batch material need return_num was not updated: ${evidence.testFixture.batch.afterRaw}`);
        }
        if (
          evidence.testFixture.batch.after.head.needId !== String(fixture.batchMatNeedId) ||
          evidence.testFixture.batch.after.head.returnNum !== fixture.expectedReturnNum
        ) {
          throw new Error(`Batch material head return_num was not updated: ${evidence.testFixture.batch.afterRaw}`);
        }
        if (
          evidence.testFixture.batch.after.part.headId !== String(fixture.batchMaterilId) ||
          evidence.testFixture.batch.after.part.needId !== String(fixture.batchMatNeedId) ||
          evidence.testFixture.batch.after.part.returnNum !== fixture.expectedReturnNum ||
          evidence.testFixture.batch.after.part.retirementState !== "WOM_retirementState/returned"
        ) {
          throw new Error(`Batch material part return/retirement state was not updated: ${evidence.testFixture.batch.afterRaw}`);
        }
      }
    } else if (action === "delete") {
      const deletePayload = { ids: `${created.id}@${created.version}` };
      evidence.operations.delete.payload = deletePayload;
      evidence.operations.delete.response = await browserFetch(page, "POST", profile.deleteApi, deletePayload);
      if (
        evidence.operations.delete.response.status !== 200 ||
        !evidence.operations.delete.response.json ||
        evidence.operations.delete.response.json.code !== 200
      ) {
        throw new Error(`Delete failed: ${JSON.stringify(evidence.operations.delete.response)}`);
      }

      const deletedRaw = runSql(rejectSql(created.id));
      const deleted = firstRejectRow(deletedRaw);
      evidence.database.afterDelete = {
        rejectSql: rejectSql(created.id).trim(),
        rejectRaw: deletedRaw,
        row: deleted,
        sideEffectSql: sideEffectSql(deleted).trim(),
        sideEffectsRaw: runSql(sideEffectSql(deleted)),
      };
      if (deleted.valid !== "f") {
        throw new Error(`Deleted reject material is not valid=false: ${deletedRaw}`);
      }
      if (!evidence.database.afterDelete.sideEffectsRaw.includes("pending_count|0")) {
        throw new Error(`Pending tasks were not cleaned after delete: ${evidence.database.afterDelete.sideEffectsRaw}`);
      }
    } else {
      throw new Error(`Unsupported ADP_WOM_REJECT_ACTION=${action}`);
    }

    evidence.status = "PASS";
  } finally {
    await browser.close();
  }
}

async function main() {
  ensureDir(outputDir);
  const evidence = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    dbSshTarget,
    marker,
    flow: flowName,
    action,
    profile: {
      route: profile.route,
      saveApi: profile.saveApi,
      deleteApi: profile.deleteApi,
      submitApi: profile.submitApi || null,
      firstSubmitApi: profile.firstSubmitApi || null,
      secondSubmitApi: profile.secondSubmitApi || null,
      deploymentId: profile.deploymentId,
      viewCode: profile.viewCode,
      processKey: profile.processKey,
      rejectTypeId: profile.rejectTypeId,
    },
    frontend: {
      console: [],
      pageErrors: [],
      requestFailures: [],
      requests: [],
      responses: [],
    },
    operations: {
      save: { method: "POST", api: profile.saveApi },
      delete: { method: "POST", api: profile.deleteApi },
    },
    testFixture: {},
    database: {},
    status: "RUNNING",
    issues: [],
  };

  try {
    const api = await request.newContext({ ignoreHTTPSErrors: true });
    const loginResult = await login(api);
    await api.dispose();
    evidence.login = { status: loginResult.status, ticket: Boolean(loginResult.ticket) };
    await runBrowser(loginResult.ticket, evidence);
  } catch (error) {
    evidence.status = "FAIL";
    evidence.issues.push(error.stack || error.message);
    process.exitCode = 1;
  } finally {
    fs.writeFileSync(outputPath, JSON.stringify(evidence, null, 2));
    console.log(JSON.stringify({ status: evidence.status, marker, outputPath }, null, 2));
  }
}

main();
