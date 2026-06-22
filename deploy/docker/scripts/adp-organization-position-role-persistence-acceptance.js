#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { chromium, request } = require("playwright");

const stamp = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const baseUrl = (process.env.ADP_BASE_URL || "http://100.99.133.43:18080").replace(/\/+$/, "");
const browserBaseUrl = (process.env.ADP_BROWSER_BASE_URL || baseUrl).replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const headless = process.env.ADP_HEADLESS !== "false";
const outputDir =
  process.env.ADP_OUTPUT_DIR ||
  path.join("/tmp", `adp-organization-position-role-persistence-${stamp}`);
const outputPath =
  process.env.ADP_ORGANIZATION_POSITION_ROLE_PERSISTENCE_OUTPUT ||
  path.join(outputDir, "organization-position-role-persistence-results.json");

const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@100.99.133.43";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";
const dbSshConnectTimeout = process.env.ADP_DB_SSH_CONNECT_TIMEOUT || "20";
const dbSqlRetries = Number(process.env.ADP_DB_SQL_RETRIES || 3);

const companyId = Number(process.env.ADP_ORG_COMPANY_ID || 1000);
const depId = Number(process.env.ADP_ORG_POSITION_DEP_ID || 1);
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${stamp}_ORGPOSROLE`;
const roleCode = process.env.ADP_ORG_POSITION_ROLE_CODE || `${marker}_ROLE`;
const roleName = process.env.ADP_ORG_POSITION_ROLE_NAME || `${marker}_ROLE_NAME`;
const positionCode = process.env.ADP_ORG_POSITION_ROLE_POSITION_CODE || `${marker}_POS`;
const positionName = process.env.ADP_ORG_POSITION_ROLE_POSITION_NAME || `${marker}_POSITION`;

const visibleErrorPattern =
  /(数据库操作异常|系统错误|系统异常|发生未知异常|SQLGrammarException|BadSqlGrammarException|could not extract ResultSet|column .* does not exist|relation .* does not exist|500 INTERNAL|\b[\w.]+Exception(?::|\s+at\b))/i;

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

function findId(payload) {
  const candidates = [
    payload && payload.id,
    payload && payload.data && payload.data.id,
    payload && payload.result && payload.result.id,
  ];
  const value = candidates.find((candidate) => candidate !== undefined && candidate !== null && candidate !== "");
  return value === undefined ? null : Number(value);
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
  let lastError;
  for (let attempt = 1; attempt <= dbSqlRetries; attempt += 1) {
    try {
      return execFileSync(
        "ssh",
        ["-o", "BatchMode=yes", "-o", `ConnectTimeout=${dbSshConnectTimeout}`, dbSshTarget, remoteCommand],
        {
          encoding: "utf8",
          stdio: ["ignore", "pipe", "pipe"],
        }
      ).trim();
    } catch (error) {
      lastError = error;
      if (attempt < dbSqlRetries) {
        execFileSync("sleep", ["2"]);
      }
    }
  }
  throw lastError;
}

function parseTableRows(output, columns) {
  if (!output) {
    return [];
  }
  return output.split(/\r?\n/).map((line) => {
    const values = line.split("|");
    return Object.fromEntries(columns.map((column, index) => [column, values[index] || ""]));
  });
}

function truthyDb(value) {
  return value === "true" || value === "1";
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

function requestMatcher(url) {
  return (
    /\/inter-api\/organization\/v1\/position(\/|\?|$)/.test(url) ||
    /\/inter-api\/organization\/v1\/position\/role(\/|\?|$)/.test(url) ||
    /\/inter-api\/rbac\/v1\/role(\/|\?|$)/.test(url)
  );
}

function queryDepartmentForPosition(targetDepId, targetCompanyId) {
  const sql = [
    "select id, code, name, company_id, valid",
    "from public.org_department",
    `where id = ${Number(targetDepId)} and company_id = ${Number(targetCompanyId)} and valid = 1;`,
  ].join(" ");
  return { sql, rows: parseTableRows(runSql(sql), ["id", "code", "name", "companyId", "valid"]) };
}

function queryRoleByCode(code) {
  const sql = [
    "select id, code, name, coalesce(description,''), coalesce(cid::text,''), coalesce(valid::text,''),",
    "coalesce(modify_time::text,''), coalesce(uuid,'')",
    "from public.rbac_role",
    `where code = ${sqlLiteral(code)}`,
    "order by create_time desc nulls last, id desc;",
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), ["id", "code", "name", "description", "cid", "valid", "modifyTime", "uuid"]),
  };
}

function queryPositionByCode(code) {
  const sql = [
    "select id, code, name, coalesce(description,''), company_id, dep_id, coalesce(parent_id::text,''),",
    "coalesce(valid::text,''), coalesce(full_path,''), coalesce(lay_rec,''), coalesce(lay_no::text,'')",
    "from public.org_position",
    `where code = ${sqlLiteral(code)}`,
    "order by create_time desc nulls last, id desc;",
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), [
      "id",
      "code",
      "name",
      "description",
      "companyId",
      "depId",
      "parentId",
      "valid",
      "fullPath",
      "layRec",
      "layNo",
    ]),
  };
}

function queryPositionRole(positionId, roleId) {
  const sql = [
    "select id, position_id, role_id, coalesce(creator,''), coalesce(modifier,''),",
    "coalesce(create_time::text,''), coalesce(modify_time::text,'')",
    "from public.org_position_role",
    `where position_id = ${Number(positionId)} and role_id = ${Number(roleId)}`,
    "order by create_time desc nulls last, id desc;",
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), [
      "id",
      "positionId",
      "roleId",
      "creator",
      "modifier",
      "createTime",
      "modifyTime",
    ]),
  };
}

function queryBaseRolePosition(positionId, roleId) {
  const sql = [
    "select id, version, position_id, role_id, coalesce(valid::text,'')",
    "from public.base_roleposition",
    `where position_id = ${Number(positionId)} and role_id = ${Number(roleId)}`,
    "order by id desc;",
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), ["id", "version", "positionId", "roleId", "valid"]),
  };
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

function ensureApiOk(result, label) {
  if (!result.ok || visibleErrorPattern.test(result.text || "")) {
    throw new Error(`${label} failed with ${result.status}: ${String(result.text || "").slice(0, 1200)}`);
  }
}

async function openAndSettle(page, url) {
  const response = await page.goto(url, { waitUntil: "domcontentloaded", timeout: 45000 });
  await page.waitForLoadState("networkidle", { timeout: 15000 }).catch(() => {});
  await page.waitForTimeout(1000);
  return response;
}

async function main() {
  ensureDir(outputDir);
  ensureDir(path.dirname(outputPath));

  if (!/^[0-9a-zA-Z_]+$/.test(roleCode) || roleCode.length > 80) {
    throw new Error(`Role code must match ^[0-9a-zA-Z_]+$ and be <= 80 chars: ${roleCode}`);
  }
  if (!/^[0-9a-zA-Z_]+$/.test(positionCode) || positionCode.length > 80) {
    throw new Error(`Position code must match ^[0-9a-zA-Z_]+$ and be <= 80 chars: ${positionCode}`);
  }

  const departmentPrecheck = queryDepartmentForPosition(depId, companyId);
  if (!departmentPrecheck.rows.length) {
    throw new Error(`No active department ${depId} found for company ${companyId}`);
  }
  const rolePrecheck = queryRoleByCode(roleCode);
  if (rolePrecheck.rows.some((row) => truthyDb(row.valid))) {
    throw new Error(`Active test role already exists for code ${roleCode}`);
  }
  const positionPrecheck = queryPositionByCode(positionCode);
  if (positionPrecheck.rows.some((row) => row.valid === "1")) {
    throw new Error(`Active test position already exists for code ${positionCode}`);
  }

  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const ticket = await login(api);
  await api.dispose();

  let browser;
  let page;
  const consoleErrors = [];
  const consoleWarnings = [];
  const pageErrors = [];
  const requestFailures = [];
  const capturedRequests = [];
  const capturedResponses = [];
  const navigations = [];
  const cleanupErrors = [];
  const cleanup = { roleId: null, roleCreated: false, positionId: null, positionRoleLinked: false };
  const operationLog = {};

  try {
    browser = await chromium.launch({ headless });
    const context = await browser.newContext({
      baseURL: browserBaseUrl,
      ignoreHTTPSErrors: true,
      viewport: { width: 1600, height: 1000 },
      extraHTTPHeaders: { Authorization: `Bearer ${ticket}` },
    });
    await context.addCookies([
      { name: "suposTicket", value: ticket, url: browserBaseUrl },
      { name: "SUPOS_TICKET", value: ticket, url: browserBaseUrl },
    ]);
    await context.addInitScript((token) => {
      window.localStorage.clear();
      window.sessionStorage.clear();
      ["suposTicket", "SUPOS_TICKET", "token"].forEach((key) => {
        window.localStorage.setItem(key, token);
        window.sessionStorage.setItem(key, token);
      });
    }, ticket);

    page = await context.newPage();
    page.on("console", (message) => {
      if (message.type() === "error") {
        consoleErrors.push({ type: message.type(), text: message.text() });
      } else if (message.type() === "warning") {
        consoleWarnings.push({ type: message.type(), text: message.text() });
      }
    });
    page.on("pageerror", (error) => pageErrors.push(error.message));
    page.on("requestfailed", (requestItem) => {
      if (requestMatcher(requestItem.url())) {
        requestFailures.push({
          method: requestItem.method(),
          url: requestItem.url(),
          failure: requestItem.failure() && requestItem.failure().errorText,
        });
      }
    });
    page.on("request", (requestItem) => {
      if (requestMatcher(requestItem.url())) {
        capturedRequests.push({
          method: requestItem.method(),
          url: requestItem.url(),
          postData: requestItem.postData(),
        });
      }
    });
    page.on("response", async (response) => {
      if (!requestMatcher(response.url())) {
        return;
      }
      let body = "";
      try {
        body = (await response.text()).slice(0, 1200);
      } catch (_error) {
        body = "";
      }
      capturedResponses.push({
        method: response.request().method(),
        url: response.url(),
        status: response.status(),
        body,
      });
    });

    navigations.push({
      route: "/auth/#/role",
      status: (await openAndSettle(page, `${browserBaseUrl}/auth/#/role`))?.status() || null,
    });

    const roleCreatePayload = {
      code: roleCode,
      name: roleName,
      description: `${marker} prerequisite role for organization position role acceptance`,
      roleType: "ROLE_TYPE/default",
      tags: [],
    };
    const roleCreateResult = await browserApi(page, "POST", "/inter-api/rbac/v1/role", roleCreatePayload);
    ensureApiOk(roleCreateResult, "Role create");
    cleanup.roleCreated = true;
    cleanup.roleId = findId(roleCreateResult.json) || cleanup.roleId;
    await page.waitForTimeout(800);
    const roleAfterCreate = queryRoleByCode(roleCode);
    const createdRole = roleAfterCreate.rows.find((row) => truthyDb(row.valid));
    if (!createdRole || createdRole.name !== roleName) {
      throw new Error(`Role create did not persist expected values: ${JSON.stringify(roleAfterCreate.rows)}`);
    }
    cleanup.roleId = Number(createdRole.id);
    operationLog.roleCreate = {
      method: "POST",
      api: "/inter-api/rbac/v1/role",
      payload: roleCreatePayload,
      responseStatus: roleCreateResult.status,
      responseBody: roleCreateResult.json || roleCreateResult.text.slice(0, 1000),
      verificationSql: roleAfterCreate.sql,
      rows: roleAfterCreate.rows,
    };

    navigations.push({
      route: "/organization/#/organizationmanage",
      status: (await openAndSettle(page, `${browserBaseUrl}/organization/#/organizationmanage`))?.status() || null,
    });

    const positionCreatePayload = {
      code: positionCode,
      name: positionName,
      companyId,
      depId,
      parentId: null,
      description: `${marker} prerequisite position for organization position role acceptance`,
      managerIds: [],
    };
    const positionCreateResult = await browserApi(
      page,
      "POST",
      "/inter-api/organization/v1/position",
      positionCreatePayload
    );
    ensureApiOk(positionCreateResult, "Position create");
    cleanup.positionId = findId(positionCreateResult.json) || cleanup.positionId;
    await page.waitForTimeout(800);
    const positionAfterCreate = queryPositionByCode(positionCode);
    const createdPosition = positionAfterCreate.rows.find((row) => row.valid === "1");
    if (!createdPosition || createdPosition.name !== positionName) {
      throw new Error(`Position create did not persist expected values: ${JSON.stringify(positionAfterCreate.rows)}`);
    }
    cleanup.positionId = Number(createdPosition.id);
    operationLog.positionCreate = {
      method: "POST",
      api: "/inter-api/organization/v1/position",
      payload: positionCreatePayload,
      responseStatus: positionCreateResult.status,
      responseBody: positionCreateResult.json || positionCreateResult.text.slice(0, 1000),
      verificationSql: positionAfterCreate.sql,
      rows: positionAfterCreate.rows,
    };

    const positionRoleAddPayload = {
      positionId: cleanup.positionId,
      roleIds: [cleanup.roleId],
    };
    const positionRoleAddResult = await browserApi(
      page,
      "POST",
      "/inter-api/organization/v1/position/role",
      positionRoleAddPayload
    );
    ensureApiOk(positionRoleAddResult, "Position role add");
    cleanup.positionRoleLinked = true;
    await page.waitForTimeout(800);
    const positionRoleAfterAdd = queryPositionRole(cleanup.positionId, cleanup.roleId);
    const baseRolePositionAfterAdd = queryBaseRolePosition(cleanup.positionId, cleanup.roleId);
    const linkedRow = positionRoleAfterAdd.rows.find(
      (row) => row.positionId === String(cleanup.positionId) && row.roleId === String(cleanup.roleId)
    );
    const linkedViewRow = baseRolePositionAfterAdd.rows.find(
      (row) =>
        row.positionId === String(cleanup.positionId) &&
        row.roleId === String(cleanup.roleId) &&
        row.valid === "1"
    );
    if (!linkedRow || !linkedViewRow) {
      throw new Error(
        `Position role add returned success but PostgreSQL did not show relation: ${JSON.stringify({
          tableRows: positionRoleAfterAdd.rows,
          viewRows: baseRolePositionAfterAdd.rows,
        })}`
      );
    }
    operationLog.positionRoleAdd = {
      method: "POST",
      api: "/inter-api/organization/v1/position/role",
      payload: positionRoleAddPayload,
      responseStatus: positionRoleAddResult.status,
      responseBody: positionRoleAddResult.json || positionRoleAddResult.text.slice(0, 1000),
      verificationSql: [positionRoleAfterAdd.sql, baseRolePositionAfterAdd.sql].join("\n"),
      rows: {
        orgPositionRole: positionRoleAfterAdd.rows,
        baseRoleposition: baseRolePositionAfterAdd.rows,
      },
    };

    const positionRoleQueryResult = await browserApi(
      page,
      "GET",
      `/inter-api/organization/v1/position/role?positionId=${cleanup.positionId}`
    );
    ensureApiOk(positionRoleQueryResult, "Position role query");
    operationLog.positionRoleQuery = {
      method: "GET",
      api: `/inter-api/organization/v1/position/role?positionId=${cleanup.positionId}`,
      payload: null,
      responseStatus: positionRoleQueryResult.status,
      responseBody: positionRoleQueryResult.json || positionRoleQueryResult.text.slice(0, 1000),
      verificationSql: [positionRoleAfterAdd.sql, baseRolePositionAfterAdd.sql].join("\n"),
      rows: {
        orgPositionRole: positionRoleAfterAdd.rows,
        baseRoleposition: baseRolePositionAfterAdd.rows,
      },
    };

    const positionRoleDeleteResult = await browserApi(
      page,
      "DELETE",
      `/inter-api/organization/v1/position/role?roleId=${cleanup.roleId}&positionId=${cleanup.positionId}`
    );
    ensureApiOk(positionRoleDeleteResult, "Position role delete");
    cleanup.positionRoleLinked = false;
    await page.waitForTimeout(800);
    const positionRoleAfterDelete = queryPositionRole(cleanup.positionId, cleanup.roleId);
    const baseRolePositionAfterDelete = queryBaseRolePosition(cleanup.positionId, cleanup.roleId);
    if (positionRoleAfterDelete.rows.length || baseRolePositionAfterDelete.rows.length) {
      throw new Error(
        `Position role delete left PostgreSQL rows: ${JSON.stringify({
          tableRows: positionRoleAfterDelete.rows,
          viewRows: baseRolePositionAfterDelete.rows,
        })}`
      );
    }
    operationLog.positionRoleDelete = {
      method: "DELETE",
      api: `/inter-api/organization/v1/position/role?roleId=${cleanup.roleId}&positionId=${cleanup.positionId}`,
      payload: null,
      responseStatus: positionRoleDeleteResult.status,
      responseBody: positionRoleDeleteResult.json || positionRoleDeleteResult.text.slice(0, 1000),
      verificationSql: [positionRoleAfterDelete.sql, baseRolePositionAfterDelete.sql].join("\n"),
      rows: {
        orgPositionRole: positionRoleAfterDelete.rows,
        baseRoleposition: baseRolePositionAfterDelete.rows,
      },
    };

    const positionDeleteResult = await browserApi(
      page,
      "DELETE",
      `/inter-api/organization/v1/position/${cleanup.positionId}`
    );
    ensureApiOk(positionDeleteResult, "Cleanup position delete");
    await page.waitForTimeout(800);
    const positionAfterDelete = queryPositionByCode(positionCode);
    const deletedPosition = positionAfterDelete.rows.find(
      (row) => row.id === String(cleanup.positionId) && row.valid === "0"
    );
    if (!deletedPosition) {
      throw new Error(`Cleanup position delete did not soft-delete row: ${JSON.stringify(positionAfterDelete.rows)}`);
    }
    cleanup.positionId = null;
    operationLog.positionDelete = {
      method: "DELETE",
      api: `/inter-api/organization/v1/position/${deletedPosition.id}`,
      payload: null,
      responseStatus: positionDeleteResult.status,
      responseBody: positionDeleteResult.json || positionDeleteResult.text.slice(0, 1000),
      verificationSql: positionAfterDelete.sql,
      rows: positionAfterDelete.rows,
    };

    const roleDeleteResult = await browserApi(page, "DELETE", `/inter-api/rbac/v1/role/${roleCode}`);
    ensureApiOk(roleDeleteResult, "Cleanup role delete");
    await page.waitForTimeout(800);
    const roleAfterDelete = queryRoleByCode(roleCode);
    const activeRole = roleAfterDelete.rows.find((row) => truthyDb(row.valid));
    if (activeRole) {
      throw new Error(`Cleanup role delete left active role: ${JSON.stringify(roleAfterDelete.rows)}`);
    }
    cleanup.roleId = null;
    cleanup.roleCreated = false;
    operationLog.roleDelete = {
      method: "DELETE",
      api: `/inter-api/rbac/v1/role/${roleCode}`,
      payload: null,
      responseStatus: roleDeleteResult.status,
      responseBody: roleDeleteResult.json || roleDeleteResult.text.slice(0, 1000),
      verificationSql: roleAfterDelete.sql,
      rows: roleAfterDelete.rows,
      deleteMode: roleAfterDelete.rows.length ? "logic delete valid=false/0" : "physical delete",
    };

    const bodyText = await page.locator("body").innerText({ timeout: 5000 }).catch(() => "");
    const visibleError = String(bodyText)
      .split(/\r?\n/)
      .map((line) => line.trim())
      .find((line) => line && visibleErrorPattern.test(line));
    const screenshotPath = path.join(outputDir, "organization-position-role-persistence.png");
    await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => {});
    await browser.close();
    browser = null;

    const report = {
      generatedAt: new Date().toISOString(),
      baseUrl,
      browserBaseUrl,
      username,
      marker,
      routes: ["/auth/#/role", "/organization/#/organizationmanage"],
      dependencies: {
        companyId,
        depId,
        departmentPrecheckSql: departmentPrecheck.sql,
        departmentPrecheckRows: departmentPrecheck.rows,
      },
      backendTrace: {
        roleCreate:
          "RoleInterApiController.save -> RoleServiceImpl.saveRole -> RoleMapper.insert -> public.rbac_role",
        positionCreate:
          "PositionInterController.addPosition -> PositionServiceImpl.addPositionWithoutKafka -> MyBatis Plus save -> public.org_position",
        positionRoleAdd:
          "PositionInterController.addPositionRole -> PositionServiceImpl.addPositionRole -> PositionRoleMapper.insert -> public.org_position_role -> public.base_roleposition view",
        positionRoleDelete:
          "PositionInterController.deletePositionRole -> PositionServiceImpl.deletePositionRole -> PositionRoleMapper.delete -> public.org_position_role -> public.base_roleposition view",
      },
      browser: {
        navigations,
        consoleErrors: compactErrors(consoleErrors),
        consoleWarnings: compactErrors(consoleWarnings),
        pageErrors: compactErrors(pageErrors),
        requestFailures: compactErrors(requestFailures),
        visibleError: visibleError || null,
        screenshot: screenshotPath,
      },
      operations: operationLog,
      capturedRequests,
      capturedResponses,
      ok:
        !visibleError &&
        consoleErrors.length === 0 &&
        pageErrors.length === 0 &&
        requestFailures.length === 0 &&
        Boolean(operationLog.positionRoleAdd) &&
        Boolean(operationLog.positionRoleDelete),
    };

    fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);
    console.log(
      `${report.ok ? "OK" : "FAIL"} organization-position-role-persistence marker=${marker} role=${roleCode} position=${positionCode}`
    );
    console.log(`REPORT ${outputPath}`);
    if (!report.ok) {
      process.exitCode = 1;
    }
  } finally {
    if (page) {
      if (cleanup.positionRoleLinked && cleanup.roleId && cleanup.positionId) {
        await browserApi(
          page,
          "DELETE",
          `/inter-api/organization/v1/position/role?roleId=${cleanup.roleId}&positionId=${cleanup.positionId}`
        ).catch((error) => cleanupErrors.push(`cleanup position role: ${error.message}`));
      }
      if (cleanup.positionId) {
        await browserApi(page, "DELETE", `/inter-api/organization/v1/position/${cleanup.positionId}`).catch((error) =>
          cleanupErrors.push(`cleanup position: ${error.message}`)
        );
      }
      if (cleanup.roleId || cleanup.roleCreated) {
        await browserApi(page, "DELETE", `/inter-api/rbac/v1/role/${roleCode}`).catch((error) =>
          cleanupErrors.push(`cleanup role: ${error.message}`)
        );
      }
    }
    if (browser) {
      await browser.close().catch(() => {});
    }
    if (cleanupErrors.length) {
      console.error(`Cleanup warnings: ${cleanupErrors.join("; ")}`);
    }
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error);
  process.exit(1);
});
