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
const outputDir =
  process.env.ADP_OUTPUT_DIR ||
  path.join("/tmp", `adp-organization-group-persistence-${new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14)}`);
const outputPath =
  process.env.ADP_ORGANIZATION_GROUP_PERSISTENCE_OUTPUT ||
  path.join(outputDir, "organization-group-persistence-results.json");

const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@100.99.133.43";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";

const companyId = Number(process.env.ADP_ORG_COMPANY_ID || 1000);
const marker =
  process.env.ADP_E2E_MARKER ||
  `ADP_E2E_${new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14)}_GRP`;
const groupCode = process.env.ADP_ORG_GROUP_CODE || marker;
const createName = process.env.ADP_ORG_GROUP_NAME || marker;
const updateName = process.env.ADP_ORG_GROUP_UPDATE_NAME || `${marker}_EDIT`;
const createDescription = `${marker} create via organization browser context`;
const updateDescription = `${marker} update via organization browser context`;

const visibleErrorPattern =
  /(数据库操作异常|系统错误|系统异常|发生未知异常|SQLGrammarException|could not extract ResultSet|column .* does not exist|relation .* does not exist|500 INTERNAL|\b[\w.]+Exception(?::|\s+at\b))/i;

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
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

async function readJsonSafe(response) {
  const text = await response.text();
  try {
    return { json: JSON.parse(text), text };
  } catch (_error) {
    return { json: null, text };
  }
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
      return ticket;
    }
    errors.push({ status: response.status(), body: parsed.text.slice(0, 500) });
  }
  throw new Error(`Login failed for ${username}: ${JSON.stringify(errors)}`);
}

function shellQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
}

function sqlLiteral(value) {
  return `'${String(value).replace(/'/g, "''")}'`;
}

function runSql(sql) {
  const remoteCommand = [
    "docker",
    "exec",
    dbContainer,
    "psql",
    "-U",
    dbUser,
    "-d",
    dbName,
    "-v",
    "ON_ERROR_STOP=1",
    "-AtF",
    "|",
    "-c",
    sql,
  ]
    .map(shellQuote)
    .join(" ");
  return execFileSync("ssh", ["-o", "BatchMode=yes", "-o", "ConnectTimeout=8", dbSshTarget, remoteCommand], {
    encoding: "utf8",
    stdio: ["ignore", "pipe", "pipe"],
  }).trim();
}

function parseGroupRows(output) {
  if (!output) {
    return [];
  }
  return output.split(/\r?\n/).map((line) => {
    const [id, code, name, description, company, valid, fullPath, sort, managerId, managerName] = line.split("|");
    return { id, code, name, description, companyId: company, valid, fullPath, sort, managerId, managerName };
  });
}

function queryGroupByCode(code) {
  const sql = [
    "select id, code, name, coalesce(description,''), company_id, coalesce(valid::text,''),",
    "coalesce(full_path,''), coalesce(sort::text,''), coalesce(manager_id::text,''), coalesce(manager_name,'')",
    "from public.org_group",
    `where code = ${sqlLiteral(code)}`,
    "order by create_time desc nulls last, id desc;",
  ].join(" ");
  return { sql, rows: parseGroupRows(runSql(sql)) };
}

async function browserApi(page, method, urlPath, payload) {
  return page.evaluate(
    async ({ method: methodArg, urlPath: urlPathArg, payload: payloadArg }) => {
      const token =
        window.localStorage.getItem("suposTicket") ||
        window.localStorage.getItem("SUPOS_TICKET") ||
        window.localStorage.getItem("token") ||
        window.sessionStorage.getItem("suposTicket") ||
        window.sessionStorage.getItem("SUPOS_TICKET") ||
        window.sessionStorage.getItem("token") ||
        "";
      const response = await window.fetch(urlPathArg, {
        method: methodArg,
        credentials: "include",
        headers: {
          Accept: "application/json, text/plain, */*",
          Authorization: token ? `Bearer ${token}` : "",
          "Content-Type": "application/json;charset=UTF-8",
          langu_code: "zh_CN",
        },
        body: payloadArg === undefined ? undefined : JSON.stringify(payloadArg),
      });
      const text = await response.text();
      let json = null;
      try {
        json = JSON.parse(text);
      } catch (_error) {
        json = null;
      }
      return { ok: response.ok, status: response.status, text, json };
    },
    { method, urlPath, payload }
  );
}

function compactErrors(items) {
  return items.map((item) => {
    if (typeof item === "string") {
      return item.slice(0, 500);
    }
    return {
      ...item,
      body: item.body ? item.body.slice(0, 800) : item.body,
      text: item.text ? item.text.slice(0, 500) : item.text,
    };
  });
}

async function main() {
  ensureDir(outputDir);
  ensureDir(path.dirname(outputPath));

  if (!/^[0-9a-zA-Z_]+$/.test(groupCode) || groupCode.length > 50) {
    throw new Error(`Group code must match ^[0-9a-zA-Z_]+$ and be <= 50 chars: ${groupCode}`);
  }

  const precheck = queryGroupByCode(groupCode);
  if (precheck.rows.some((row) => row.valid === "1")) {
    throw new Error(`Active test group already exists for code ${groupCode}`);
  }

  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const ticket = await login(api);
  await api.dispose();

  const browser = await chromium.launch({ headless });
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
  const consoleErrors = [];
  const pageErrors = [];
  const requestFailures = [];
  const groupRequests = [];
  const groupResponses = [];

  page.on("console", (message) => {
    if (["error", "warning"].includes(message.type())) {
      consoleErrors.push({ type: message.type(), text: message.text() });
    }
  });
  page.on("pageerror", (error) => pageErrors.push(error.message));
  page.on("requestfailed", (requestItem) => {
    if (/\/inter-api\/organization\/v1\/group/.test(requestItem.url())) {
      requestFailures.push({
        method: requestItem.method(),
        url: requestItem.url(),
        failure: requestItem.failure() && requestItem.failure().errorText,
      });
    }
  });
  page.on("request", (requestItem) => {
    if (/\/inter-api\/organization\/v1\/group(\/|\?|$)/.test(requestItem.url())) {
      groupRequests.push({
        method: requestItem.method(),
        url: requestItem.url(),
        postData: requestItem.postData(),
      });
    }
  });
  page.on("response", async (response) => {
    if (!/\/inter-api\/organization\/v1\/group(\/|\?|$)/.test(response.url())) {
      return;
    }
    let body = "";
    try {
      body = (await response.text()).slice(0, 1200);
    } catch (_error) {
      body = "";
    }
    groupResponses.push({
      method: response.request().method(),
      url: response.url(),
      status: response.status(),
      body,
    });
  });

  const organizationUrl = `${baseUrl}/organization/#/organizationmanage`;
  const navigation = await page.goto(organizationUrl, { waitUntil: "domcontentloaded", timeout: 45000 });
  await page.waitForLoadState("networkidle", { timeout: 15000 }).catch(() => {});
  await page.waitForTimeout(1500);

  const createPayload = {
    code: groupCode,
    name: createName,
    companyId,
    description: createDescription,
    managerIds: [],
  };
  const createResult = await browserApi(page, "POST", "/inter-api/organization/v1/group", createPayload);
  if (!createResult.ok || visibleErrorPattern.test(createResult.text)) {
    throw new Error(`Group create failed with ${createResult.status}: ${createResult.text.slice(0, 800)}`);
  }
  await page.waitForLoadState("networkidle", { timeout: 10000 }).catch(() => {});
  await page.waitForTimeout(800);
  const afterCreate = queryGroupByCode(groupCode);
  const created = afterCreate.rows.find((row) => row.valid === "1");
  if (!created) {
    throw new Error(`Group create API returned success but no active PostgreSQL row found for ${groupCode}`);
  }

  const updatePayload = {
    id: Number(created.id),
    name: updateName,
    description: updateDescription,
    managerIds: [],
  };
  const updateResult = await browserApi(page, "PUT", "/inter-api/organization/v1/group", updatePayload);
  if (!updateResult.ok || visibleErrorPattern.test(updateResult.text)) {
    throw new Error(`Group update failed with ${updateResult.status}: ${updateResult.text.slice(0, 800)}`);
  }
  await page.waitForLoadState("networkidle", { timeout: 10000 }).catch(() => {});
  await page.waitForTimeout(800);
  const afterUpdate = queryGroupByCode(groupCode);
  const updated = afterUpdate.rows.find((row) => row.id === created.id && row.valid === "1");
  if (!updated || updated.name !== updateName || updated.description !== updateDescription) {
    throw new Error(`Group update did not persist expected values: ${JSON.stringify(afterUpdate.rows)}`);
  }

  const deleteResult = await browserApi(page, "DELETE", `/inter-api/organization/v1/group/${created.id}`);
  if (!deleteResult.ok || visibleErrorPattern.test(deleteResult.text)) {
    throw new Error(`Group delete failed with ${deleteResult.status}: ${deleteResult.text.slice(0, 800)}`);
  }
  await page.waitForLoadState("networkidle", { timeout: 10000 }).catch(() => {});
  await page.waitForTimeout(800);
  const afterDelete = queryGroupByCode(groupCode);
  const softDeleted = afterDelete.rows.find((row) => row.id === created.id && row.valid === "0");
  const activeAfterDelete = afterDelete.rows.find((row) => row.id === created.id && row.valid === "1");
  const physicallyDeleted = !afterDelete.rows.some((row) => row.id === created.id);
  if (activeAfterDelete || (!softDeleted && !physicallyDeleted)) {
    throw new Error(`Group delete did not remove or soft-delete row: ${JSON.stringify(afterDelete.rows)}`);
  }

  const bodyText = await page.locator("body").innerText({ timeout: 5000 }).catch(() => "");
  const visibleError = String(bodyText)
    .split(/\r?\n/)
    .map((line) => line.trim())
    .find((line) => line && visibleErrorPattern.test(line));
  const screenshotPath = path.join(outputDir, "organization-group-persistence.png");
  await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => {});
  await browser.close();

  const report = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    username,
    marker,
    route: "/organization/#/organizationmanage",
    backendTrace: {
      create:
        "GroupInterController.addDepartment -> GroupService.addGroup -> GroupServiceImpl.addGroup -> MyBatis Plus save -> public.org_group",
      update:
        "GroupInterController.updateDepartment -> GroupService.updateGroup -> GroupServiceImpl.updateGroup -> MyBatis Plus updateById -> public.org_group",
      delete:
        "GroupInterController.deleteDep -> GroupService.deleteGroupById -> GroupServiceImpl.deleteGroupById -> MyBatis Plus removeById -> public.org_group",
    },
    browser: {
      navigationStatus: navigation ? navigation.status() : null,
      consoleErrors: compactErrors(consoleErrors),
      pageErrors: compactErrors(pageErrors),
      requestFailures: compactErrors(requestFailures),
      visibleError: visibleError || null,
      screenshot: screenshotPath,
    },
    operations: {
      create: {
        method: "POST",
        api: "/inter-api/organization/v1/group",
        payload: createPayload,
        responseStatus: createResult.status,
        responseBody: createResult.json || createResult.text.slice(0, 1000),
        verificationSql: afterCreate.sql,
        rows: afterCreate.rows,
      },
      update: {
        method: "PUT",
        api: "/inter-api/organization/v1/group",
        payload: updatePayload,
        responseStatus: updateResult.status,
        responseBody: updateResult.json || updateResult.text.slice(0, 1000),
        verificationSql: afterUpdate.sql,
        rows: afterUpdate.rows,
      },
      delete: {
        method: "DELETE",
        api: `/inter-api/organization/v1/group/${created.id}`,
        payload: null,
        responseStatus: deleteResult.status,
        responseBody: deleteResult.json || deleteResult.text.slice(0, 1000),
        verificationSql: afterDelete.sql,
        rows: afterDelete.rows,
        deleteMode: softDeleted ? "soft-delete" : "physical-delete",
      },
    },
    capturedGroupRequests: groupRequests,
    capturedGroupResponses: groupResponses,
    ok:
      !visibleError &&
      consoleErrors.length === 0 &&
      pageErrors.length === 0 &&
      requestFailures.length === 0 &&
      Boolean(created) &&
      Boolean(updated) &&
      (Boolean(softDeleted) || physicallyDeleted),
  };

  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);
  console.log(
    `${report.ok ? "OK" : "FAIL"} organization-group-persistence marker=${marker} id=${created.id} deleteMode=${report.operations.delete.deleteMode}`
  );
  console.log(`REPORT ${outputPath}`);
  if (!report.ok) {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error);
  process.exit(1);
});
