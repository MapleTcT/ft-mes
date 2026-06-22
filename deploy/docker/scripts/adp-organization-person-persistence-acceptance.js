#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { execFileSync } = require("child_process");
const { chromium, request } = require("playwright");

const stamp = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const baseUrl = (process.env.ADP_BASE_URL || "http://100.99.133.43:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const headless = process.env.ADP_HEADLESS !== "false";
const outputDir =
  process.env.ADP_OUTPUT_DIR ||
  path.join("/tmp", `adp-organization-person-persistence-${stamp}`);
const outputPath =
  process.env.ADP_ORGANIZATION_PERSON_PERSISTENCE_OUTPUT ||
  path.join(outputDir, "organization-person-persistence-results.json");

const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@100.99.133.43";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";

const mainPositionId = Number(process.env.ADP_ORG_PERSON_MAIN_POSITION_ID || 1);
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${stamp}_PER`;
const personCode = process.env.ADP_ORG_PERSON_CODE || marker;
const createName = process.env.ADP_ORG_PERSON_NAME || marker;
const updateName = process.env.ADP_ORG_PERSON_UPDATE_NAME || `${marker}_EDIT`;
const createPhone = process.env.ADP_ORG_PERSON_PHONE || `139${stamp.slice(6, 14)}`;
const updatePhone = process.env.ADP_ORG_PERSON_UPDATE_PHONE || `138${stamp.slice(6, 14)}`;
const createEmail = process.env.ADP_ORG_PERSON_EMAIL || `${personCode.toLowerCase()}@example.test`;
const updateEmail =
  process.env.ADP_ORG_PERSON_UPDATE_EMAIL || `${personCode.toLowerCase()}_edit@example.test`;
const createDescription = `${marker} create via organization browser context`;
const updateDescription = `${marker} update via organization browser context`;
const createGender = "sys_gender/male";
const updateGender = "sys_gender/female";
const activeStatus = "sys_person_status/onWork";

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

function parseTableRows(output, columns) {
  if (!output) {
    return [];
  }
  return output.split(/\r?\n/).map((line) => {
    const values = line.split("|");
    return Object.fromEntries(columns.map((column, index) => [column, values[index] || ""]));
  });
}

function queryMainPosition(positionId) {
  const sql = [
    "select p.id, p.code, p.name, p.dep_id, p.company_id, coalesce(p.valid::text,''),",
    "coalesce(d.name,''), coalesce(c.short_name,'')",
    "from public.org_position p",
    "left join public.org_department d on d.id = p.dep_id",
    "left join public.org_company c on c.id = p.company_id",
    `where p.id = ${Number(positionId)} and p.valid = 1;`,
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), [
      "id",
      "code",
      "name",
      "depId",
      "companyId",
      "valid",
      "departmentName",
      "companyName",
    ]),
  };
}

function queryPersonByCode(code) {
  const sql = [
    "select id, code, name, coalesce(gender,''), coalesce(status,''), coalesce(main_position::text,''),",
    "coalesce(phone,''), coalesce(email,''), coalesce(description,''), coalesce(valid::text,''),",
    "coalesce(sys_flag::text,''), coalesce(user_id::text,''), coalesce(user_name,''),",
    "coalesce(direct_leader_id::text,''), coalesce(grand_leader_id::text,''),",
    "coalesce(entry_date,''), coalesce(title,''), coalesce(qualification,''), coalesce(education,''),",
    "coalesce(major,''), coalesce(id_number,''), coalesce(avatar_url,''), coalesce(sign_pic_url,'')",
    "from public.org_person",
    `where code = ${sqlLiteral(code)}`,
    "order by create_time desc nulls last, id desc;",
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), [
      "id",
      "code",
      "name",
      "gender",
      "status",
      "mainPosition",
      "phone",
      "email",
      "description",
      "valid",
      "sysFlag",
      "userId",
      "userName",
      "directLeaderId",
      "grandLeaderId",
      "entryDate",
      "title",
      "qualification",
      "education",
      "major",
      "idNumber",
      "avatarUrl",
      "signPicUrl",
    ]),
  };
}

function queryRelation(table, idColumn, personId) {
  const columnsByTable = {
    org_person_position: ["id", "positionId", "personId", "valid"],
    org_person_department: ["id", "deptId", "positionId", "personId", "valid"],
    org_person_company: ["id", "companyId", "positionId", "personId", "valid"],
  };
  const selectByTable = {
    org_person_position:
      "select id, position_id, person_id, coalesce(valid::text,'') from public.org_person_position",
    org_person_department:
      "select id, dept_id, position_id, person_id, coalesce(valid::text,'') from public.org_person_department",
    org_person_company:
      "select id, company_id, position_id, person_id, coalesce(valid::text,'') from public.org_person_company",
  };
  const sql = [
    selectByTable[table],
    `where ${idColumn} = ${sqlLiteral(personId)}`,
    "order by id;",
  ].join(" ");
  return { sql, rows: parseTableRows(runSql(sql), columnsByTable[table]) };
}

function queryPersonMnecode(personId) {
  const sql = [
    "select id, person_id, coalesce(mne_code,''), coalesce(person_name,'')",
    "from public.org_person_mnecode",
    `where person_id = ${sqlLiteral(personId)}`,
    "order by id;",
  ].join(" ");
  return { sql, rows: parseTableRows(runSql(sql), ["id", "personId", "mneCode", "personName"]) };
}

function queryAuthUsers(personId, code) {
  const sql = [
    "select id, user_name, coalesce(person_id::text,''), coalesce(person_code,''),",
    "coalesce(person_name,''), coalesce(company_id::text,''), coalesce(valid::text,'')",
    "from public.auth_user",
    `where person_id = ${sqlLiteral(personId)} or person_code = ${sqlLiteral(code)} or user_name = ${sqlLiteral(code)}`,
    "order by id;",
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), [
      "id",
      "userName",
      "personId",
      "personCode",
      "personName",
      "companyId",
      "valid",
    ]),
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

function findActiveRelation(rows, personId) {
  return rows.find((row) => row.personId === String(personId) && row.valid === "1");
}

function findSoftDeletedRelation(rows, personId) {
  return rows.find((row) => row.personId === String(personId) && row.valid === "0");
}

async function main() {
  ensureDir(outputDir);
  ensureDir(path.dirname(outputPath));

  if (!/^[0-9a-zA-Z_]+$/.test(personCode) || personCode.length > 50) {
    throw new Error(`Person code must match ^[0-9a-zA-Z_]+$ and be <= 50 chars: ${personCode}`);
  }

  const dependencyPrecheck = queryMainPosition(mainPositionId);
  if (!dependencyPrecheck.rows.length) {
    throw new Error(`No active main position ${mainPositionId} found`);
  }

  const precheck = queryPersonByCode(personCode);
  if (precheck.rows.some((row) => row.valid === "1")) {
    throw new Error(`Active test person already exists for code ${personCode}`);
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
  const personRequests = [];
  const personResponses = [];

  page.on("console", (message) => {
    if (["error", "warning"].includes(message.type())) {
      consoleErrors.push({ type: message.type(), text: message.text() });
    }
  });
  page.on("pageerror", (error) => pageErrors.push(error.message));
  page.on("requestfailed", (requestItem) => {
    if (/\/inter-api\/organization\/v1\/person(\/|\?|$)/.test(requestItem.url())) {
      requestFailures.push({
        method: requestItem.method(),
        url: requestItem.url(),
        failure: requestItem.failure() && requestItem.failure().errorText,
      });
    }
  });
  page.on("request", (requestItem) => {
    if (/\/inter-api\/organization\/v1\/person(\/|\?|$)/.test(requestItem.url())) {
      personRequests.push({
        method: requestItem.method(),
        url: requestItem.url(),
        postData: requestItem.postData(),
      });
    }
  });
  page.on("response", async (response) => {
    if (!/\/inter-api\/organization\/v1\/person(\/|\?|$)/.test(response.url())) {
      return;
    }
    let body = "";
    try {
      body = (await response.text()).slice(0, 1200);
    } catch (_error) {
      body = "";
    }
    personResponses.push({
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
    code: personCode,
    name: createName,
    gender: createGender,
    mainPosition: mainPositionId,
    status: activeStatus,
    phone: createPhone,
    email: createEmail,
    description: createDescription,
    createUser: false,
    roles: [],
    roleNames: [],
  };
  const createResult = await browserApi(page, "POST", "/inter-api/organization/v1/person", createPayload);
  if (!createResult.ok || visibleErrorPattern.test(createResult.text)) {
    throw new Error(`Person create failed with ${createResult.status}: ${createResult.text.slice(0, 800)}`);
  }
  await page.waitForLoadState("networkidle", { timeout: 10000 }).catch(() => {});
  await page.waitForTimeout(800);
  const afterCreate = queryPersonByCode(personCode);
  const created = afterCreate.rows.find((row) => row.valid === "1");
  if (!created) {
    throw new Error(`Person create API returned success but no active PostgreSQL row found for ${personCode}`);
  }
  const personId = created.id;
  const positionAfterCreate = queryRelation("org_person_position", "person_id", personId);
  const departmentAfterCreate = queryRelation("org_person_department", "person_id", personId);
  const companyAfterCreate = queryRelation("org_person_company", "person_id", personId);
  const mnecodeAfterCreate = queryPersonMnecode(personId);
  const authAfterCreate = queryAuthUsers(personId, personCode);
  if (
    created.name !== createName ||
    created.gender !== createGender ||
    created.status !== activeStatus ||
    created.mainPosition !== String(mainPositionId) ||
    created.phone !== createPhone ||
    created.email !== createEmail ||
    created.description !== createDescription
  ) {
    throw new Error(`Person create persisted unexpected values: ${JSON.stringify(afterCreate.rows)}`);
  }
  if (
    !findActiveRelation(positionAfterCreate.rows, personId) ||
    !findActiveRelation(departmentAfterCreate.rows, personId) ||
    !findActiveRelation(companyAfterCreate.rows, personId) ||
    !mnecodeAfterCreate.rows.length
  ) {
    throw new Error(
      `Person create did not persist expected relations/mnecode: ${JSON.stringify({
        positionAfterCreate,
        departmentAfterCreate,
        companyAfterCreate,
        mnecodeAfterCreate,
      })}`
    );
  }

  const updatePayload = {
    id: Number(personId),
    name: updateName,
    gender: updateGender,
    mainPosition: mainPositionId,
    status: activeStatus,
    phone: updatePhone,
    email: updateEmail,
    description: updateDescription,
  };
  const updateResult = await browserApi(page, "PUT", "/inter-api/organization/v1/person", updatePayload);
  if (!updateResult.ok || visibleErrorPattern.test(updateResult.text)) {
    throw new Error(`Person update failed with ${updateResult.status}: ${updateResult.text.slice(0, 800)}`);
  }
  await page.waitForLoadState("networkidle", { timeout: 10000 }).catch(() => {});
  await page.waitForTimeout(800);
  const afterUpdate = queryPersonByCode(personCode);
  const updated = afterUpdate.rows.find((row) => row.id === personId && row.valid === "1");
  const mnecodeAfterUpdate = queryPersonMnecode(personId);
  if (
    !updated ||
    updated.name !== updateName ||
    updated.gender !== updateGender ||
    updated.status !== activeStatus ||
    updated.phone !== updatePhone ||
    updated.email !== updateEmail ||
    updated.description !== updateDescription ||
    updated.mainPosition !== String(mainPositionId)
  ) {
    throw new Error(`Person update did not persist expected values: ${JSON.stringify(afterUpdate.rows)}`);
  }
  if (!mnecodeAfterUpdate.rows.length || !mnecodeAfterUpdate.rows.some((row) => row.personName === updateName)) {
    throw new Error(`Person update did not refresh mnecode rows: ${JSON.stringify(mnecodeAfterUpdate.rows)}`);
  }

  const deleteResult = await browserApi(page, "DELETE", `/inter-api/organization/v1/person/${personId}`);
  if (!deleteResult.ok || visibleErrorPattern.test(deleteResult.text)) {
    throw new Error(`Person delete failed with ${deleteResult.status}: ${deleteResult.text.slice(0, 800)}`);
  }
  await page.waitForLoadState("networkidle", { timeout: 10000 }).catch(() => {});
  await page.waitForTimeout(800);
  const afterDelete = queryPersonByCode(personCode);
  const deleted = afterDelete.rows.find((row) => row.id === personId && row.valid === "0");
  const positionAfterDelete = queryRelation("org_person_position", "person_id", personId);
  const departmentAfterDelete = queryRelation("org_person_department", "person_id", personId);
  const companyAfterDelete = queryRelation("org_person_company", "person_id", personId);
  const mnecodeAfterDelete = queryPersonMnecode(personId);
  const authAfterDelete = queryAuthUsers(personId, personCode);
  if (!deleted) {
    throw new Error(`Person delete did not soft-delete row: ${JSON.stringify(afterDelete.rows)}`);
  }
  if (
    !findSoftDeletedRelation(positionAfterDelete.rows, personId) ||
    !findSoftDeletedRelation(departmentAfterDelete.rows, personId) ||
    !findSoftDeletedRelation(companyAfterDelete.rows, personId)
  ) {
    throw new Error(
      `Person delete did not soft-delete expected relations: ${JSON.stringify({
        positionAfterDelete,
        departmentAfterDelete,
        companyAfterDelete,
      })}`
    );
  }
  if (mnecodeAfterDelete.rows.length) {
    throw new Error(`Person delete did not remove mnecode rows: ${JSON.stringify(mnecodeAfterDelete.rows)}`);
  }
  if (deleted.userId || deleted.userName) {
    throw new Error(`Person delete did not clear user binding: ${JSON.stringify(deleted)}`);
  }

  const bodyText = await page.locator("body").innerText({ timeout: 5000 }).catch(() => "");
  const visibleError = String(bodyText)
    .split(/\r?\n/)
    .map((line) => line.trim())
    .find((line) => line && visibleErrorPattern.test(line));
  const screenshotPath = path.join(outputDir, "organization-person-persistence.png");
  await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => {});
  await browser.close();

  const report = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    username,
    marker,
    route: "/organization/#/organizationmanage",
    dependencies: {
      mainPositionId,
      positionPrecheckSql: dependencyPrecheck.sql,
      positionPrecheckRows: dependencyPrecheck.rows,
    },
    backendTrace: {
      create:
        "PersonInterController.addPerson -> PersonService.addPersonAndUser(createUser=false) -> PersonServiceImpl.addPersonWithoutKafka -> MyBatis Plus save -> public.org_person; insert public.org_person_position/public.org_person_department/public.org_person_company; OrgMnecodeServiceImpl.addOrgMnecode -> public.org_person_mnecode",
      update:
        "PersonInterController.updatePerson -> PersonService.updatePerson -> PersonServiceImpl.updatePersonWithoutKafka -> MyBatis Plus updateById -> public.org_person; OrgMnecodeServiceImpl refresh -> public.org_person_mnecode",
      delete:
        "PersonInterController.deletePerson -> PersonService.deletePerson -> PersonServiceImpl.deletePerson -> public.org_person.valid=0; PositionPersonServiceImpl/DepartmentPersonServiceImpl/CompanyPersonServiceImpl relation valid=0; OrgMnecodeServiceImpl.deleteOrgMnecodeByOrgId -> public.org_person_mnecode; PersonMapper.deleteUserByPersonId clears user binding",
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
        api: "/inter-api/organization/v1/person",
        payload: createPayload,
        responseStatus: createResult.status,
        responseBody: createResult.json || createResult.text.slice(0, 1000),
        verificationSql: afterCreate.sql,
        rows: afterCreate.rows,
        relationSql: [
          positionAfterCreate.sql,
          departmentAfterCreate.sql,
          companyAfterCreate.sql,
          mnecodeAfterCreate.sql,
          authAfterCreate.sql,
        ],
        relations: {
          position: positionAfterCreate.rows,
          department: departmentAfterCreate.rows,
          company: companyAfterCreate.rows,
          mnecode: mnecodeAfterCreate.rows,
          authUsers: authAfterCreate.rows,
        },
      },
      update: {
        method: "PUT",
        api: "/inter-api/organization/v1/person",
        payload: updatePayload,
        responseStatus: updateResult.status,
        responseBody: updateResult.json || updateResult.text.slice(0, 1000),
        verificationSql: afterUpdate.sql,
        rows: afterUpdate.rows,
        mnecodeSql: mnecodeAfterUpdate.sql,
        mnecodeRows: mnecodeAfterUpdate.rows,
      },
      delete: {
        method: "DELETE",
        api: `/inter-api/organization/v1/person/${personId}`,
        payload: null,
        responseStatus: deleteResult.status,
        responseBody: deleteResult.json || deleteResult.text.slice(0, 1000),
        verificationSql: afterDelete.sql,
        rows: afterDelete.rows,
        relationSql: [
          positionAfterDelete.sql,
          departmentAfterDelete.sql,
          companyAfterDelete.sql,
          mnecodeAfterDelete.sql,
          authAfterDelete.sql,
        ],
        relations: {
          position: positionAfterDelete.rows,
          department: departmentAfterDelete.rows,
          company: companyAfterDelete.rows,
          mnecode: mnecodeAfterDelete.rows,
          authUsers: authAfterDelete.rows,
        },
        deleteMode: "soft-delete",
        userAccountMode: "createUser=false; no auth_user account expected",
      },
    },
    capturedPersonRequests: personRequests,
    capturedPersonResponses: personResponses,
    ok:
      !visibleError &&
      consoleErrors.length === 0 &&
      pageErrors.length === 0 &&
      requestFailures.length === 0 &&
      Boolean(created) &&
      Boolean(updated) &&
      Boolean(deleted) &&
      !mnecodeAfterDelete.rows.length,
  };

  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);
  console.log(
    `${report.ok ? "OK" : "FAIL"} organization-person-persistence marker=${marker} id=${personId} deleteMode=${report.operations.delete.deleteMode}`
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
