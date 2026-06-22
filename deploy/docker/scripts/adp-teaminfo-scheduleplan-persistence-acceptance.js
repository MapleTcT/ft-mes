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
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${nowToken}_TEAMINFO_SCHEDULE_PLAN`;
const outputDir =
  process.env.ADP_OUTPUT_DIR || path.join("/tmp", `adp-teaminfo-scheduleplan-persistence-${nowToken}`);
const outputPath =
  process.env.ADP_TEAMINFO_SCHEDULEPLAN_PERSISTENCE_OUTPUT ||
  path.join(outputDir, "teaminfo-scheduleplan-persistence-results.json");

const route = "/msService/TeamInfo/schedulePlan/schedulePlan/schedulePlanList";
const saveApi = "/msService/TeamInfo/schedulePlan/schedulePlan/schedulePlanEdit/save";
const scheduleRuleCode = process.env.ADP_TEAMINFO_SCHEDULE_RULE_CODE || "ADP_SMOKE_TEAMINFO_SCHEDULE_RULE";

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
  const commonArgs = ["-o", "StrictHostKeyChecking=no", "-o", "UserKnownHostsFile=/dev/null"];
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

function formatDate(offsetDays) {
  const date = new Date(Date.now() + offsetDays * 24 * 60 * 60 * 1000);
  const pad = (value) => String(value).padStart(2, "0");
  return [date.getFullYear(), pad(date.getMonth() + 1), pad(date.getDate())].join("-");
}

function scheduleRuleSql() {
  return `
SELECT id, code, name
FROM public.team_schedule_rules
WHERE code = ${sqlLiteral(scheduleRuleCode)}
ORDER BY id DESC
LIMIT 1;
`;
}

function planSql() {
  return `
SELECT id, code, name, table_no, schedule_rule, start_date, next_date,
       schedule_round, advance_amount, coalesce(is_round_patrol::text, ''),
       coalesce(valid::text, ''), coalesce(status::text, ''), coalesce(create_time::text, '')
FROM public.team_schedule_plans
WHERE code = ${sqlLiteral(`${marker}_CODE`)}
   OR name = ${sqlLiteral(marker)}
ORDER BY create_time DESC NULLS LAST, id DESC;
`;
}

function sideEffectSql(planId) {
  return `
SELECT 'team_schedules_by_schedule_rule', count(*)
FROM public.team_schedules
WHERE schedule_rule = (
  SELECT schedule_rule FROM public.team_schedule_plans WHERE id = ${planId}
);
SELECT 'team_schedule_plans_di', count(*)
FROM public.team_schedule_plans_di
WHERE main_obj = ${planId}
   OR tableinfoid = ${planId}
   OR table_info_id = ${planId};
`;
}

function buildSavePayload(scheduleRule) {
  const startDate = formatDate(2);
  return {
    viewCode: "TeamInfo_1.0.0_schedulePlan_schedulePlanEdit",
    modelName: "TeamInfo_1.0.0_schedulePlan_SchedulePlan",
    operateType: "save",
    schedulePlan: {
      version: 0,
      valid: true,
      status: 99,
      cid: 1000,
      tableNo: `${marker}_TN`,
      code: `${marker}_CODE`,
      name: marker,
      scheduleRule: {
        id: scheduleRule.id,
        code: scheduleRule.code,
        name: scheduleRule.name,
      },
      startDate,
      nextDate: startDate,
      scheduleRound: 1,
      advanceAmount: 0,
      isRoundPatrol: false,
    },
    dgList: {},
    dgDeletedIds: {},
    viewSelect: "",
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
      return { status: response.status, ok: response.ok, body: text.slice(0, 4000), json };
    },
    { method, apiPath, payload }
  );
}

async function clickAdd(page, evidence) {
  const result = { clicked: false, error: null, dialogText: "" };
  try {
    const addButton = page.locator("#btn-add").first();
    await addButton.waitFor({ state: "visible", timeout: 10000 });
    await addButton.click();
    result.clicked = true;
    await page.waitForTimeout(1200);
    result.dialogText = await page.locator("body").innerText({ timeout: 3000 });
  } catch (error) {
    result.error = error.message;
  }
  evidence.frontend.addButton = result;
  return result;
}

async function runBrowser(ticket, scheduleRule, evidence) {
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
      if (/TeamInfo|layoutJson|getDataGirdCookie/.test(url)) {
        evidence.frontend.requests.push({
          method: requestItem.method(),
          url,
          postData: requestItem.postData(),
        });
      }
    });
    page.on("response", async (response) => {
      const url = response.url();
      if (!/TeamInfo|layoutJson|getDataGirdCookie/.test(url)) {
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

    const pageResponse = await page.goto(route, { waitUntil: "domcontentloaded", timeout: 45000 });
    await page.waitForTimeout(2500);
    const listScreenshot = path.join(outputDir, "schedule-plan-list-before.png");
    await page.screenshot({ path: listScreenshot, fullPage: true });
    evidence.frontend.route = {
      route,
      status: pageResponse ? pageResponse.status() : null,
      title: await page.title().catch(() => ""),
      screenshot: listScreenshot,
    };

    await clickAdd(page, evidence);
    const dialogScreenshot = path.join(outputDir, "schedule-plan-add-dialog.png");
    await page.screenshot({ path: dialogScreenshot, fullPage: true });
    evidence.frontend.addDialogScreenshot = dialogScreenshot;

    evidence.operations.save.payload = buildSavePayload(scheduleRule);
    evidence.operations.save.response = await browserFetch(page, "POST", saveApi, evidence.operations.save.payload);

    const afterSaveScreenshot = path.join(outputDir, "schedule-plan-after-save.png");
    await page.screenshot({ path: afterSaveScreenshot, fullPage: true });
    evidence.frontend.afterSaveScreenshot = afterSaveScreenshot;

    if (evidence.operations.save.response.status !== 200) {
      throw new Error(`Save HTTP failed: ${JSON.stringify(evidence.operations.save.response)}`);
    }
    if (evidence.operations.save.response.json && evidence.operations.save.response.json.code >= 400) {
      throw new Error(`Save business response failed: ${JSON.stringify(evidence.operations.save.response.json)}`);
    }

    const createdRaw = runSql(planSql());
    const rows = parseRows(createdRaw);
    evidence.database.afterSave = {
      verificationSql: planSql().trim(),
      raw: createdRaw,
      rows,
    };
    if (rows.length !== 1) {
      throw new Error(`Expected exactly one saved schedule plan for ${marker}, got ${rows.length}: ${createdRaw}`);
    }
    const [id, code, name, tableNo, scheduleRuleId, startDate, nextDate, scheduleRound, advanceAmount, roundPatrol, valid] =
      rows[0];
    evidence.database.createdPlan = {
      id,
      code,
      name,
      tableNo,
      scheduleRuleId,
      startDate,
      nextDate,
      scheduleRound,
      advanceAmount,
      roundPatrol,
      valid,
    };
    if (!["t", "true"].includes(String(valid).toLowerCase())) {
      throw new Error(`Saved schedule plan is not valid=true: ${createdRaw}`);
    }
    evidence.database.afterSave.sideEffectSql = sideEffectSql(id).trim();
    evidence.database.afterSave.sideEffectsRaw = runSql(sideEffectSql(id));
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
    frontend: {
      console: [],
      pageErrors: [],
      requestFailures: [],
      requests: [],
      responses: [],
    },
    operations: {
      save: { method: "POST", api: saveApi },
    },
    database: {
      scheduleRuleSql: scheduleRuleSql().trim(),
    },
    status: "RUNNING",
    issues: [],
  };

  try {
    const scheduleRuleRaw = runSql(scheduleRuleSql());
    const scheduleRuleRows = parseRows(scheduleRuleRaw);
    evidence.database.scheduleRuleRaw = scheduleRuleRaw;
    if (scheduleRuleRows.length !== 1) {
      throw new Error(`Expected one TeamInfo schedule rule prerequisite, got ${scheduleRuleRows.length}`);
    }
    const [id, code, name] = scheduleRuleRows[0];
    const scheduleRule = { id, code, name };
    evidence.database.scheduleRule = scheduleRule;

    const api = await request.newContext({ ignoreHTTPSErrors: true });
    const loginResult = await login(api);
    await api.dispose();
    evidence.login = { status: loginResult.status, ticket: Boolean(loginResult.ticket) };
    await runBrowser(loginResult.ticket, scheduleRule, evidence);
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
