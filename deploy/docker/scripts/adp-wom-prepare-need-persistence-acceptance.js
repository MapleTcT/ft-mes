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
const dbSshPassword = process.env.ADP_DB_SSH_PASSWORD || "";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const nowToken = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${nowToken}_WOM_PREPARE_NEED`;
const outputDir =
  process.env.ADP_OUTPUT_DIR ||
  path.join("/tmp", `adp-wom-prepare-need-persistence-${nowToken}`);
const outputPath =
  process.env.ADP_WOM_PREPARE_NEED_PERSISTENCE_OUTPUT ||
  path.join(outputDir, "wom-prepare-need-persistence-results.json");
const idBase = 8200000000000000n + BigInt(Date.now() % 900000000000) * 100n + BigInt(process.pid % 100);
const ids = {
  material: idBase + 1n,
  formula: idBase + 2n,
  task: idBase + 3n,
};
const factoryId = process.env.ADP_WOM_FACTORY_ID || "7000000000001000";
const tableNo = `${marker}_TASK_TN`;
const materialCode = `${marker}_MAT`;
const formulaCode = `${marker}_FORM`;
const batchNo = `${marker}_BATCH`;
const route = "/msService/WOM/produceTask/produceTask/prepareMakeTaskList";
const gridId = "WOM_1.0.0_produceTask_prepareMakeTaskList_produceTask_sdg";
const buttonId = "btn-prePareNeed";

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
  const commonArgs = [
    "-o",
    "StrictHostKeyChecking=no",
    "-o",
    "UserKnownHostsFile=/dev/null",
  ];
  if (dbSshPassword) {
    return execFileSync("sshpass", ["-e", "ssh", ...commonArgs, dbSshTarget, command], {
      input,
      encoding: "utf8",
      env: { ...process.env, SSHPASS: dbSshPassword },
      stdio: ["pipe", "pipe", "pipe"],
    });
  }
  return execFileSync("ssh", ["-o", "BatchMode=yes", ...commonArgs, dbSshTarget, command], {
    input,
    encoding: "utf8",
    stdio: ["pipe", "pipe", "pipe"],
  });
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

function seedSql() {
  const commonCols =
    "version, valid, cid, create_staff_id, create_time, create_department_id, create_position_id, group_id, owner_staff_id, owner_department_id, owner_position_id, position_lay_rec";
  const commonVals = "0, true, 1000, 1, now(), 1, 1, 1000, 1, 1, 1, '1'";
  return `
BEGIN;

INSERT INTO public.baseset_materials (
  id, ${commonCols}, code, name, table_no
) VALUES (
  ${ids.material}, ${commonVals}, ${sqlLiteral(materialCode)}, ${sqlLiteral(`${marker} material`)}, ${sqlLiteral(`${materialCode}_TN`)}
) ON CONFLICT (id) DO NOTHING;

INSERT INTO public.rm_formulas (
  id, ${commonCols}, formual_code, formula_name, set_process, product_id, table_no
) VALUES (
  ${ids.formula}, ${commonVals}, ${sqlLiteral(formulaCode)}, ${sqlLiteral(`${marker} formula`)}, 'RM_formulaType/simpleFormula', ${ids.material}, ${sqlLiteral(`${formulaCode}_TN`)}
) ON CONFLICT (id) DO NOTHING;

INSERT INTO public.wom_produce_tasks (
  id, ${commonCols}, status, table_no, table_info_id, batch_contral, finish_num,
  formula_id, plan_num, plan_start_time, plan_end_time, produce_batch_num,
  product_id, task_run_state, task_type, need_pack, is_analy, is_abnormal,
  is_prepared, advance_charge, pre_pare_state, work_area_id, remark
) VALUES (
  ${ids.task}, ${commonVals}, 99, ${sqlLiteral(tableNo)}, ${ids.task}, false, 0,
  ${ids.formula}, 1, now() - interval '1 day', now() + interval '1 day', ${sqlLiteral(batchNo)},
  ${ids.material}, 'WOM_runState/waitForRun', 'WOM_taskType/manufacture', false, false, false,
  false, false, 'WOM_prePareNeedState/waitPrePare', ${factoryId}, NULL
) ON CONFLICT (id) DO UPDATE SET
  valid = true,
  status = 99,
  task_run_state = 'WOM_runState/waitForRun',
  task_type = 'WOM_taskType/manufacture',
  is_prepared = false,
  pre_pare_state = 'WOM_prePareNeedState/waitPrePare',
  work_area_id = ${factoryId};

COMMIT;

SELECT 'seed', ${sqlLiteral(marker)}, ${ids.task}, ${sqlLiteral(tableNo)}, ${factoryId};
`;
}

function verificationSql() {
  return `
SELECT 'task', id, table_no, coalesce(task_run_state, ''), coalesce(is_prepared::text, ''), coalesce(pre_pare_state, ''), coalesce(work_area_id::text, ''), coalesce(version::text, '')
FROM public.wom_produce_tasks
WHERE id = ${ids.task};

SELECT 'need', n.id, coalesce(n.table_no, ''), coalesce(n.factory_id::text, ''), coalesce(n.need_ref_source, ''), coalesce(n.need_state, ''), coalesce(n.material_type, ''), coalesce(n.status::text, ''), coalesce(n.valid::text, '')
FROM public.wom_pre_pare_needs n
JOIN public.wom_pre_pare_need_refs r ON r.head_id = n.id
WHERE r.origins = ${sqlLiteral(String(ids.task))}
ORDER BY n.id;

SELECT 'ref', id, coalesce(head_id::text, ''), coalesce(table_info_id::text, ''), coalesce(origins, ''), coalesce(produce_batch_num, ''), coalesce(product_id::text, ''), coalesce(plan_num::text, '')
FROM public.wom_pre_pare_need_refs
WHERE origins = ${sqlLiteral(String(ids.task))}
ORDER BY id;

SELECT 'diCount', count(*)::text
FROM public.wom_pre_pare_needs_di
WHERE table_info_id IN (
  SELECT n.table_info_id
  FROM public.wom_pre_pare_needs n
  JOIN public.wom_pre_pare_need_refs r ON r.head_id = n.id
  WHERE r.origins = ${sqlLiteral(String(ids.task))}
);
`;
}

function assertPersistence(rawRows) {
  const rows = parseRows(rawRows);
  const task = rows.find((row) => row[0] === "task");
  const need = rows.find((row) => row[0] === "need");
  const ref = rows.find((row) => row[0] === "ref");
  const diCount = rows.find((row) => row[0] === "diCount");
  const failures = [];

  if (!task || task[4] !== "true" || task[5] !== "WOM_prePareNeedState/waitPrePare" || task[6] !== String(factoryId)) {
    failures.push(`wom_produce_tasks was not marked prepared as expected: ${JSON.stringify(task)}`);
  }
  if (!need || need[3] !== String(factoryId) || need[4] !== "WOM_needRefSource/task" || need[5] !== "WOM_needState/waitConfirmOrder" || need[6] !== "WOM_materialType/demand" || need[8] !== "true") {
    failures.push(`wom_pre_pare_needs header was not inserted as expected: ${JSON.stringify(need)}`);
  }
  if (!ref || ref[4] !== String(ids.task) || ref[5] !== batchNo || ref[6] !== String(ids.material) || Number(ref[7]) !== 1) {
    failures.push(`wom_pre_pare_need_refs row was not inserted as expected: ${JSON.stringify(ref)}`);
  }
  if (!diCount || Number(diCount[1]) < 0) {
    failures.push(`wom_pre_pare_needs_di verification did not run: ${JSON.stringify(diCount)}`);
  }
  if (failures.length) {
    throw new Error(failures.join("; "));
  }
  return { rows, task, need, ref, diCount };
}

async function selectMarkerRow(page) {
  await page.waitForFunction(
    (expectedMarker) => document.body && document.body.innerText.includes(expectedMarker),
    marker,
    { timeout: 30000 }
  );
  return page.evaluate(
    ({ gridCode, expectedMarker }) => {
      const gridFactory = window.ReactAPI && window.ReactAPI.getComponentAPI("SupDataGrid");
      const grid = gridFactory && typeof gridFactory.APIs === "function" && gridFactory.APIs(gridCode);
      if (!grid || typeof grid.getRows !== "function") {
        return { ok: false, error: "grid api missing" };
      }
      const rows = grid.getRows ? grid.getRows() : [];
      const rowIndex = rows.findIndex((row) => String(row.tableNo || "").includes(expectedMarker));
      if (rowIndex < 0) {
        return { ok: false, rowCount: rows.length, tableNos: rows.map((row) => row.tableNo).slice(0, 10) };
      }
      grid.setSelecteds(String(rowIndex));
      const selecteds = grid.getSelecteds ? grid.getSelecteds() : [];
      const selected = selecteds[0] || {};
      return {
        ok: selecteds.length === 1,
        rowIndex,
        selectedCount: selecteds.length,
        selectedId: selected.id,
        tableNo: selected.tableNo,
        workAreaName: selected.workAreaId && selected.workAreaId.name,
        taskRunState: selected.taskRunState && selected.taskRunState.id,
        isPrepared: selected.isPrepared,
        prePareState: selected.prePareState && selected.prePareState.id,
      };
    },
    { gridCode: gridId, expectedMarker: marker }
  );
}

async function invokeRuntimeButton(page, runtimeButtonId) {
  return page.evaluate((buttonIdToInvoke) => {
    const buttonCell = document.getElementById(buttonIdToInvoke);
    const buttonItem = buttonCell && buttonCell.querySelector(".sup-datagrid-button-item");
    if (!buttonItem) {
      return { ok: false, error: `button item not found: ${buttonIdToInvoke}` };
    }

    const fiberKey = Object.keys(buttonItem).find((key) => key.startsWith("__reactInternalInstance$"));
    let fiber = fiberKey && buttonItem[fiberKey];
    while (fiber) {
      if (fiber.type && fiber.type.name === "HeaderButtonCell") {
        const instance = fiber.stateNode;
        const button = instance && instance.props && instance.props.button;
        if (!instance || !button || typeof instance.setWindowFunc !== "function") {
          return { ok: false, error: `runtime button instance is incomplete: ${buttonIdToInvoke}` };
        }

        let showTipResult = true;
        if (typeof instance.showTipFunc === "function") {
          showTipResult = instance.showTipFunc();
        }
        if (!showTipResult) {
          return { ok: false, error: `runtime button showTipFunc returned false: ${buttonIdToInvoke}` };
        }

        const operateEvent = instance.setWindowFunc(button.funcname, button.funcbody);
        if (typeof operateEvent !== "function") {
          return { ok: false, error: `runtime button event is not callable: ${buttonIdToInvoke}` };
        }
        operateEvent({ type: "adp-e2e-runtime-button", target: buttonItem, currentTarget: buttonItem });
        return {
          ok: true,
          triggerMode: "runtime-button-event",
          buttonId: buttonIdToInvoke,
          buttonConfigId: button.id,
          buttonName: button.showname || button.name || button.NAME,
          operationCode: button.buttonoperationcode || button.CODE,
        };
      }
      fiber = fiber.return;
    }

    return { ok: false, error: `HeaderButtonCell not found: ${buttonIdToInvoke}` };
  }, runtimeButtonId);
}

async function gotoRouteWhenReady(page, evidence) {
  const attempts = [];
  let lastResponse = null;
  for (let attempt = 1; attempt <= 8; attempt += 1) {
    lastResponse = await page.goto(route, { waitUntil: "domcontentloaded", timeout: 45000 });
    const status = lastResponse && lastResponse.status();
    attempts.push({ attempt, status });
    if (![502, 503, 504].includes(status)) {
      evidence.navigation = { status, attempts };
      return lastResponse;
    }
    await page.waitForTimeout(5000);
  }
  evidence.navigation = { status: lastResponse && lastResponse.status(), attempts };
  return lastResponse;
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
      ["suposTicket", "SUPOS_TICKET", "token"].forEach((key) => {
        window.localStorage.setItem(key, token);
        window.sessionStorage.setItem(key, token);
      });
    }, ticket);

    const page = await context.newPage();
    page.on("console", (message) => {
      if (["error", "warning"].includes(message.type())) {
        evidence.console.push({ type: message.type(), text: message.text() });
      }
    });
    page.on("pageerror", (error) => evidence.pageErrors.push(error.message));
    page.on("requestfailed", (requestItem) => {
      evidence.requestFailures.push({
        method: requestItem.method(),
        url: requestItem.url(),
        failure: requestItem.failure() && requestItem.failure().errorText,
      });
    });
    page.on("request", (requestItem) => {
      const url = requestItem.url();
      if (/prepareMakeTaskList|generatePrepareNeed|layoutJson|systemCodeJson/.test(url)) {
        evidence.requests.push({
          method: requestItem.method(),
          url,
          postData: requestItem.postData(),
        });
      }
    });
    page.on("response", async (response) => {
      const url = response.url();
      if (!/prepareMakeTaskList|generatePrepareNeed|layoutJson|systemCodeJson/.test(url)) {
        return;
      }
      let body = "";
      try {
        body = (await response.text()).slice(0, 8000);
      } catch (_error) {
        body = "";
      }
      evidence.responses.push({
        method: response.request().method(),
        url,
        status: response.status(),
        body,
      });
    });

    await gotoRouteWhenReady(page, evidence);
    evidence.screenshots.before = path.join(outputDir, "wom-prepare-need-before.png");
    await page.screenshot({ path: evidence.screenshots.before, fullPage: true });

    const selection = await selectMarkerRow(page);
    evidence.selection = selection;
    if (!selection.ok) {
      throw new Error(`Failed to select marker prepare row: ${JSON.stringify(selection)}`);
    }

    const generateResponsePromise = page.waitForResponse(
      (response) => {
        const postData = response.request().postData() || "";
        return (
          response.url().includes("/WOM/produceTask/produceTask/generatePrepareNeed") &&
          postData.includes(`ids=${ids.task}`)
        );
      },
      { timeout: 30000 }
    );
    const invocation = await invokeRuntimeButton(page, buttonId);
    evidence.buttonInvocation = invocation;
    if (!invocation.ok) {
      throw new Error(`prepare-need runtime button invocation failed: ${JSON.stringify(invocation)}`);
    }

    await page.waitForTimeout(800);
    evidence.screenshots.confirm = path.join(outputDir, "wom-prepare-need-confirm.png");
    await page.screenshot({ path: evidence.screenshots.confirm, fullPage: true });

    const confirmButton = page
      .locator(".ant-modal-confirm-btns button, .ant-modal button, button")
      .filter({ hasText: /确\s*定|确认/ });
    const confirmButtonCount = await confirmButton.count();
    evidence.confirmButtonCount = confirmButtonCount;
    if (confirmButtonCount < 1) {
      throw new Error("prepare-need confirmation button was not found");
    }
    await confirmButton.last().click({ timeout: 10000 });

    const generateResponse = await generateResponsePromise;
    const body = await generateResponse.text();
    let json = null;
    try {
      json = JSON.parse(body);
    } catch (_error) {
      json = null;
    }
    evidence.generatePrepareNeed = {
      status: generateResponse.status(),
      body: body.slice(0, 8000),
      json,
    };
    const dealSuccess =
      json && (json.dealSuccessFlag === true || (json.data && json.data.dealSuccessFlag === true));
    if (generateResponse.status() !== 200 || !dealSuccess) {
      throw new Error(`generatePrepareNeed did not pass: ${JSON.stringify(evidence.generatePrepareNeed)}`);
    }

    await page.waitForTimeout(1200);
    evidence.afterBody = (await page.locator("body").innerText()).slice(0, 5000);
    evidence.screenshots.after = path.join(outputDir, "wom-prepare-need-after.png");
    await page.screenshot({ path: evidence.screenshots.after, fullPage: true });
    await context.close();
  } finally {
    await browser.close();
  }
}

function assertFrontendClean(evidence) {
  const failedResponses = evidence.responses.filter((response) => response.status >= 400);
  const consoleErrors = evidence.console.filter((message) => message.type === "error");
  const failures = [];

  if (failedResponses.length) {
    failures.push(`HTTP ${failedResponses.map((response) => `${response.status} ${response.url}`).join("; ")}`);
  }
  if (evidence.requestFailures.length) {
    failures.push(`request failures ${JSON.stringify(evidence.requestFailures)}`);
  }
  if (consoleErrors.length) {
    failures.push(`console errors ${JSON.stringify(consoleErrors)}`);
  }
  if (evidence.pageErrors.length) {
    failures.push(`page errors ${JSON.stringify(evidence.pageErrors)}`);
  }

  evidence.frontendClean = {
    failedResponses,
    requestFailures: evidence.requestFailures,
    consoleErrors,
    pageErrors: evidence.pageErrors,
    status: failures.length ? "FAIL" : "PASS",
  };

  if (failures.length) {
    throw new Error(`Frontend acceptance had blocking errors: ${failures.join(" | ")}`);
  }
}

async function main() {
  ensureDir(outputDir);
  const evidence = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    marker,
    ids: Object.fromEntries(Object.entries(ids).map(([key, value]) => [key, value.toString()])),
    route,
    gridId,
    tableNo,
    materialCode,
    formulaCode,
    batchNo,
    console: [],
    pageErrors: [],
    requestFailures: [],
    requests: [],
    responses: [],
    screenshots: {},
  };

  try {
    evidence.seed = { sqlResult: runSql(seedSql()) };
    const api = await request.newContext({ ignoreHTTPSErrors: true });
    const loginResult = await login(api);
    await api.dispose();
    evidence.login = { status: loginResult.status, ticket: Boolean(loginResult.ticket) };

    await runBrowser(loginResult.ticket, evidence);
    assertFrontendClean(evidence);
    const verifyRaw = runSql(verificationSql());
    evidence.persistence = assertPersistence(verifyRaw);
    evidence.verificationSql = verificationSql().trim();
    evidence.status = "PASS";
  } catch (error) {
    evidence.status = "FAIL";
    evidence.error = error.stack || error.message;
    fs.writeFileSync(outputPath, JSON.stringify(evidence, null, 2));
    throw error;
  }

  fs.writeFileSync(outputPath, JSON.stringify(evidence, null, 2));
  console.log(
    JSON.stringify(
      {
        status: evidence.status,
        marker,
        outputPath,
        generatePrepareNeed: evidence.generatePrepareNeed,
        persistence: {
          task: evidence.persistence.task,
          need: evidence.persistence.need,
          ref: evidence.persistence.ref,
          diCount: evidence.persistence.diCount,
        },
        frontendClean: evidence.frontendClean,
      },
      null,
      2
    )
  );
}

main().catch((error) => {
  console.error(error.stack || error.message);
  process.exit(1);
});
