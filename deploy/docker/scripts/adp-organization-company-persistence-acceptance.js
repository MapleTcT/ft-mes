#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { chromium, request } = require("playwright");

const stamp = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const headless = process.env.ADP_HEADLESS !== "false";
const outputDir =
  process.env.ADP_OUTPUT_DIR ||
  path.join("/tmp", `adp-organization-company-persistence-${stamp}`);
const outputPath =
  process.env.ADP_ORGANIZATION_COMPANY_PERSISTENCE_OUTPUT ||
  path.join(outputDir, "organization-company-persistence-results.json");

const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@10.11.100.17";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";

const parentCompanyId = Number(process.env.ADP_ORG_PARENT_COMPANY_ID || 1000);
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${stamp}_COM`;
const companyCode = process.env.ADP_ORG_COMPANY_CODE || marker;
const createShortName = process.env.ADP_ORG_COMPANY_SHORT_NAME || marker;
const createFullName = process.env.ADP_ORG_COMPANY_FULL_NAME || `${marker}_FULL`;
const updateShortName = process.env.ADP_ORG_COMPANY_UPDATE_SHORT_NAME || `${marker}_EDIT`;
const updateFullName = process.env.ADP_ORG_COMPANY_UPDATE_FULL_NAME || `${marker}_FULL_EDIT`;
const adminUserName =
  process.env.ADP_ORG_COMPANY_ADMIN_USERNAME || `adp_e2e_${stamp}_com_admin`;
const adminPassword =
  process.env.ADP_ORG_COMPANY_ADMIN_PASSWORD || `Ft@${stamp.slice(8, 14)}Aa`;
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

function parseCompanyRows(output) {
  if (!output) {
    return [];
  }
  return output.split(/\r?\n/).map((line) => {
    const [
      id,
      code,
      shortName,
      fullName,
      description,
      parentId,
      valid,
      fullPath,
      layRec,
      layNo,
      sort,
      oldId,
    ] = line.split("|");
    return {
      id,
      code,
      shortName,
      fullName,
      description,
      parentId,
      valid,
      fullPath,
      layRec,
      layNo,
      sort,
      oldId,
    };
  });
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

function queryParentCompany(id) {
  const sql = [
    "select id, code, short_name, full_name, coalesce(valid::text,''), coalesce(full_path,''), coalesce(lay_rec,'')",
    "from public.org_company",
    `where id = ${Number(id)} and valid = 1;`,
  ].join(" ");
  return { sql, raw: runSql(sql) };
}

function queryCompanyByCode(code) {
  const sql = [
    "select id, code, short_name, full_name, coalesce(description,''), coalesce(parent_id::text,''),",
    "coalesce(valid::text,''), coalesce(full_path,''), coalesce(lay_rec,''), coalesce(lay_no::text,''),",
    "coalesce(sort::text,''), coalesce(old_id,'')",
    "from public.org_company",
    `where code = ${sqlLiteral(code)}`,
    "order by create_time desc nulls last, id desc;",
  ].join(" ");
  return { sql, rows: parseCompanyRows(runSql(sql)) };
}

function queryCompanyMnecode(companyId) {
  const sql = [
    "select id, company_id, coalesce(mne_code,''), coalesce(company_short_name,'')",
    "from public.org_company_mnecode",
    `where company_id = ${sqlLiteral(companyId)}`,
    "order by id;",
  ].join(" ");
  return { sql, rows: parseTableRows(runSql(sql), ["id", "companyId", "mneCode", "companyShortName"]) };
}

function queryCompanyDepartments(companyId) {
  const sql = [
    "select id, code, name, coalesce(sys_flag::text,''), coalesce(valid::text,''), coalesce(full_path,''), coalesce(lay_rec,'')",
    "from public.org_department",
    `where company_id = ${sqlLiteral(companyId)}`,
    "order by sys_flag desc, id;",
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), ["id", "code", "name", "sysFlag", "valid", "fullPath", "layRec"]),
  };
}

function queryCompanyPositions(companyId) {
  const sql = [
    "select id, code, name, dep_id, coalesce(sys_flag::text,''), coalesce(valid::text,''), coalesce(full_path,''), coalesce(lay_rec,'')",
    "from public.org_position",
    `where company_id = ${sqlLiteral(companyId)}`,
    "order by sys_flag desc, id;",
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), ["id", "code", "name", "depId", "sysFlag", "valid", "fullPath", "layRec"]),
  };
}

function queryCompanyPeople(adminName) {
  const sql = [
    "select id, code, name, coalesce(valid::text,''), coalesce(sys_flag::text,''), coalesce(user_name,''),",
    "coalesce(user_id::text,''), coalesce(main_position::text,'')",
    "from public.org_person",
    `where code = ${sqlLiteral(adminName)} or user_name = ${sqlLiteral(adminName)}`,
    "order by id;",
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), ["id", "code", "name", "valid", "sysFlag", "userName", "userId", "mainPosition"]),
  };
}

function queryCompanyAuthUsers(adminName) {
  const sql = [
    "select id, user_name, coalesce(company_id::text,''), coalesce(current_company_id::text,''),",
    "coalesce(person_id::text,''), coalesce(valid::text,''), coalesce(user_type::text,'')",
    "from public.auth_user",
    `where user_name = ${sqlLiteral(adminName)}`,
    "order by id;",
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), [
      "id",
      "userName",
      "companyId",
      "currentCompanyId",
      "personId",
      "valid",
      "userType",
    ]),
  };
}

function querySideEffects(companyId, adminName) {
  return {
    departments: queryCompanyDepartments(companyId),
    positions: queryCompanyPositions(companyId),
    people: queryCompanyPeople(adminName),
    authUsers: queryCompanyAuthUsers(adminName),
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

function assertActiveCompany(row, expected) {
  if (!row || row.valid !== "1") {
    throw new Error(`Expected active company row, got ${JSON.stringify(row)}`);
  }
  for (const [key, value] of Object.entries(expected)) {
    if (row[key] !== String(value)) {
      throw new Error(`Company ${key} expected ${value}, got ${row[key]} in ${JSON.stringify(row)}`);
    }
  }
}

async function main() {
  ensureDir(outputDir);
  ensureDir(path.dirname(outputPath));

  if (!/^[0-9a-zA-Z_]+$/.test(companyCode) || companyCode.length > 50) {
    throw new Error(`Company code must match ^[0-9a-zA-Z_]+$ and be <= 50 chars: ${companyCode}`);
  }
  if (!/^\w+$/.test(adminUserName)) {
    throw new Error(`Company admin username must match ^\\w+$: ${adminUserName}`);
  }

  const parentPrecheck = queryParentCompany(parentCompanyId);
  if (!parentPrecheck.raw) {
    throw new Error(`No active parent company ${parentCompanyId} found`);
  }
  const precheck = queryCompanyByCode(companyCode);
  if (precheck.rows.some((row) => row.valid === "1")) {
    throw new Error(`Active test company already exists for code ${companyCode}`);
  }
  const authPrecheck = queryCompanyAuthUsers(adminUserName);
  if (authPrecheck.rows.length > 0) {
    throw new Error(`Test admin user already exists for username ${adminUserName}`);
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
  const companyRequests = [];
  const companyResponses = [];

  page.on("console", (message) => {
    if (["error", "warning"].includes(message.type())) {
      consoleErrors.push({ type: message.type(), text: message.text() });
    }
  });
  page.on("pageerror", (error) => pageErrors.push(error.message));
  page.on("requestfailed", (requestItem) => {
    if (/\/inter-api\/organization\/v1\/compan(?:y|ies)/.test(requestItem.url())) {
      requestFailures.push({
        method: requestItem.method(),
        url: requestItem.url(),
        failure: requestItem.failure() && requestItem.failure().errorText,
      });
    }
  });
  page.on("request", (requestItem) => {
    if (/\/inter-api\/organization\/v1\/compan(?:y|ies)(\/|\?|$)/.test(requestItem.url())) {
      companyRequests.push({
        method: requestItem.method(),
        url: requestItem.url(),
        postData: requestItem.postData(),
      });
    }
  });
  page.on("response", async (response) => {
    if (!/\/inter-api\/organization\/v1\/compan(?:y|ies)(\/|\?|$)/.test(response.url())) {
      return;
    }
    let body = "";
    try {
      body = (await response.text()).slice(0, 1200);
    } catch (_error) {
      body = "";
    }
    companyResponses.push({
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
    code: companyCode,
    shortName: createShortName,
    fullName: createFullName,
    parentId: parentCompanyId,
    description: createDescription,
    tags: [],
    userName: adminUserName,
    password: adminPassword,
  };
  const createResult = await browserApi(page, "POST", "/inter-api/organization/v1/company", createPayload);
  if (!createResult.ok || visibleErrorPattern.test(createResult.text)) {
    throw new Error(`Company create failed with ${createResult.status}: ${createResult.text.slice(0, 1000)}`);
  }
  await page.waitForLoadState("networkidle", { timeout: 10000 }).catch(() => {});
  await page.waitForTimeout(1000);
  const afterCreate = queryCompanyByCode(companyCode);
  const created = afterCreate.rows.find((row) => row.valid === "1");
  assertActiveCompany(created, {
    code: companyCode,
    shortName: createShortName,
    fullName: createFullName,
    description: createDescription,
    parentId: parentCompanyId,
  });
  const mnecodeAfterCreate = queryCompanyMnecode(created.id);
  if (mnecodeAfterCreate.rows.length === 0) {
    throw new Error(`Company create did not generate org_company_mnecode rows for ${created.id}`);
  }
  const sideEffectsAfterCreate = querySideEffects(created.id, adminUserName);
  if (!sideEffectsAfterCreate.departments.rows.some((row) => row.valid === "1" && row.sysFlag === "1")) {
    throw new Error(`Company create did not persist an active virtual department: ${JSON.stringify(sideEffectsAfterCreate.departments.rows)}`);
  }
  if (!sideEffectsAfterCreate.positions.rows.some((row) => row.valid === "1" && row.sysFlag === "1")) {
    throw new Error(`Company create did not persist an active virtual position: ${JSON.stringify(sideEffectsAfterCreate.positions.rows)}`);
  }
  if (!sideEffectsAfterCreate.people.rows.some((row) => row.valid === "1" && row.sysFlag === "1")) {
    throw new Error(`Company create did not persist an active virtual person: ${JSON.stringify(sideEffectsAfterCreate.people.rows)}`);
  }
  if (!sideEffectsAfterCreate.authUsers.rows.some((row) => row.userName === adminUserName && row.companyId === String(created.id))) {
    throw new Error(`Company create did not persist auth_user for ${adminUserName}: ${JSON.stringify(sideEffectsAfterCreate.authUsers.rows)}`);
  }

  const updatePayload = {
    id: Number(created.id),
    shortName: updateShortName,
    fullName: updateFullName,
    description: updateDescription,
    tags: [],
  };
  const updateResult = await browserApi(page, "PUT", "/inter-api/organization/v1/company", updatePayload);
  if (!updateResult.ok || visibleErrorPattern.test(updateResult.text)) {
    throw new Error(`Company update failed with ${updateResult.status}: ${updateResult.text.slice(0, 1000)}`);
  }
  await page.waitForLoadState("networkidle", { timeout: 10000 }).catch(() => {});
  await page.waitForTimeout(1000);
  const afterUpdate = queryCompanyByCode(companyCode);
  const updated = afterUpdate.rows.find((row) => row.id === created.id && row.valid === "1");
  assertActiveCompany(updated, {
    code: companyCode,
    shortName: updateShortName,
    fullName: updateFullName,
    description: updateDescription,
    parentId: parentCompanyId,
  });
  const mnecodeAfterUpdate = queryCompanyMnecode(created.id);
  if (!mnecodeAfterUpdate.rows.some((row) => row.companyShortName === updateShortName)) {
    throw new Error(`Company update did not refresh mnecode rows: ${JSON.stringify(mnecodeAfterUpdate.rows)}`);
  }
  const sideEffectsAfterUpdate = querySideEffects(created.id, adminUserName);

  const deleteResult = await browserApi(page, "DELETE", `/inter-api/organization/v1/company/${created.id}`);
  if (!deleteResult.ok || visibleErrorPattern.test(deleteResult.text)) {
    throw new Error(`Company delete failed with ${deleteResult.status}: ${deleteResult.text.slice(0, 1000)}`);
  }
  await page.waitForLoadState("networkidle", { timeout: 10000 }).catch(() => {});
  await page.waitForTimeout(1000);
  const afterDelete = queryCompanyByCode(companyCode);
  const deleted = afterDelete.rows.find((row) => row.id === created.id && row.valid === "0");
  if (!deleted) {
    throw new Error(`Company delete did not soft-delete row: ${JSON.stringify(afterDelete.rows)}`);
  }
  const mnecodeAfterDelete = queryCompanyMnecode(created.id);
  if (mnecodeAfterDelete.rows.length > 0) {
    throw new Error(`Company delete did not remove mnecode rows: ${JSON.stringify(mnecodeAfterDelete.rows)}`);
  }
  const sideEffectsAfterDelete = querySideEffects(created.id, adminUserName);
  if (sideEffectsAfterDelete.departments.rows.some((row) => row.valid === "1")) {
    throw new Error(`Company delete left active departments: ${JSON.stringify(sideEffectsAfterDelete.departments.rows)}`);
  }
  if (sideEffectsAfterDelete.positions.rows.some((row) => row.valid === "1")) {
    throw new Error(`Company delete left active positions: ${JSON.stringify(sideEffectsAfterDelete.positions.rows)}`);
  }
  if (sideEffectsAfterDelete.authUsers.rows.some((row) => row.valid === "1")) {
    throw new Error(`Company delete left active auth_user rows: ${JSON.stringify(sideEffectsAfterDelete.authUsers.rows)}`);
  }

  const bodyText = await page.locator("body").innerText({ timeout: 5000 }).catch(() => "");
  const visibleError = String(bodyText)
    .split(/\r?\n/)
    .map((line) => line.trim())
    .find((line) => line && visibleErrorPattern.test(line));
  const screenshotPath = path.join(outputDir, "organization-company-persistence.png");
  await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => {});
  await browser.close();

  const report = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    username,
    marker,
    route: "/organization/#/organizationmanage",
    dependencies: {
      parentCompanyId,
      parentPrecheckSql: parentPrecheck.sql,
      parentPrecheckRows: parentPrecheck.raw,
    },
    backendTrace: {
      create:
        "CompanyInterController.addCompany -> CompanyService.addCompany -> CompanyServiceImpl.addCompany -> MyBatis Plus save -> public.org_company; DepartmentService.addVirtualDept -> public.org_department; PositionService.addVirtualPos -> public.org_position; PersonService.addVirtualPerson -> public.org_person/org_person_position/org_person_department/org_person_company; OrgMnecodeServiceImpl.addOrgMnecode -> public.org_company_mnecode; OrganizationAdapter.createUser -> UserApiService.createUser -> UserServiceImpl.creatUser -> public.auth_user",
      update:
        "CompanyInterController.updateCompany -> CompanyService.saveOrUpdateCom -> CompanyServiceImpl.saveOrUpdateCom -> MyBatis Plus updateBatchById/saveOrUpdate -> public.org_company; OrgMnecodeServiceImpl refresh -> public.org_company_mnecode",
      delete:
        "CompanyInterController.delCompany -> CompanyService.delCompany -> CompanyServiceImpl.delCompany -> MyBatis Plus updateById -> public.org_company.valid=0; soft-delete company departments/positions; OrgMnecodeServiceImpl.deleteOrgMnecodeByOrgId -> public.org_company_mnecode; OrganizationAdapter.deleteCompanyUser -> UserServiceImpl.deleteUserByCompanyId -> public.auth_user.valid=0",
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
        api: "/inter-api/organization/v1/company",
        payload: { ...createPayload, password: "[REDACTED]" },
        responseStatus: createResult.status,
        responseBody: createResult.json || createResult.text.slice(0, 1000),
        verificationSql: afterCreate.sql,
        rows: afterCreate.rows,
        mnecodeSql: mnecodeAfterCreate.sql,
        mnecodeRows: mnecodeAfterCreate.rows,
        sideEffects: sideEffectsAfterCreate,
      },
      update: {
        method: "PUT",
        api: "/inter-api/organization/v1/company",
        payload: updatePayload,
        responseStatus: updateResult.status,
        responseBody: updateResult.json || updateResult.text.slice(0, 1000),
        verificationSql: afterUpdate.sql,
        rows: afterUpdate.rows,
        mnecodeSql: mnecodeAfterUpdate.sql,
        mnecodeRows: mnecodeAfterUpdate.rows,
        sideEffects: sideEffectsAfterUpdate,
      },
      delete: {
        method: "DELETE",
        api: `/inter-api/organization/v1/company/${created.id}`,
        payload: null,
        responseStatus: deleteResult.status,
        responseBody: deleteResult.json || deleteResult.text.slice(0, 1000),
        verificationSql: afterDelete.sql,
        rows: afterDelete.rows,
        mnecodeSql: mnecodeAfterDelete.sql,
        mnecodeRows: mnecodeAfterDelete.rows,
        sideEffects: sideEffectsAfterDelete,
        deleteMode: "soft-delete",
        authUserDeleteMode: sideEffectsAfterDelete.authUsers.rows.length > 0 ? "soft-delete" : "physical-delete",
      },
    },
    capturedCompanyRequests: companyRequests.map((requestItem) => ({
      ...requestItem,
      postData: requestItem.postData && requestItem.postData.includes(adminPassword)
        ? requestItem.postData.replace(adminPassword, "[REDACTED]")
        : requestItem.postData,
    })),
    capturedCompanyResponses: companyResponses,
    ok:
      !visibleError &&
      consoleErrors.length === 0 &&
      pageErrors.length === 0 &&
      requestFailures.length === 0 &&
      Boolean(created) &&
      Boolean(updated) &&
      Boolean(deleted) &&
      mnecodeAfterCreate.rows.length > 0 &&
      mnecodeAfterUpdate.rows.length > 0 &&
      mnecodeAfterDelete.rows.length === 0 &&
      sideEffectsAfterDelete.departments.rows.every((row) => row.valid !== "1") &&
      sideEffectsAfterDelete.positions.rows.every((row) => row.valid !== "1") &&
      sideEffectsAfterDelete.authUsers.rows.every((row) => row.valid !== "1"),
  };

  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);
  console.log(
    `${report.ok ? "OK" : "FAIL"} organization-company-persistence marker=${marker} id=${created.id} deleteMode=${report.operations.delete.deleteMode}`
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
