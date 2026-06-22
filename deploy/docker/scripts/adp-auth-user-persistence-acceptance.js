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
  process.env.ADP_OUTPUT_DIR || path.join("/tmp", `adp-auth-user-persistence-${stamp}`);
const outputPath =
  process.env.ADP_AUTH_USER_PERSISTENCE_OUTPUT ||
  path.join(outputDir, "auth-user-persistence-results.json");

const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@100.99.133.43";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";

const mainPositionId = Number(process.env.ADP_AUTH_USER_MAIN_POSITION_ID || 1);
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${stamp}_AUSR`;
const personCode = process.env.ADP_AUTH_USER_PERSON_CODE || `${marker}_PER`;
const personName = process.env.ADP_AUTH_USER_PERSON_NAME || `${marker}_PERSON`;
const userName = process.env.ADP_AUTH_USER_NAME || `adp_e2e_${stamp}_auth_user`;
const userPassword = process.env.ADP_AUTH_USER_PASSWORD || `Ft@${stamp.slice(8, 14)}Aa1`;
const createDescription = `${marker} create via auth user page`;
const updateDescription = `${marker} update via auth user page`;
const createPhone = process.env.ADP_AUTH_USER_PERSON_PHONE || `135${stamp.slice(6, 14)}`;
const createEmail = process.env.ADP_AUTH_USER_PERSON_EMAIL || `${userName}@example.test`;
const activeStatus = "sys_person_status/onWork";
const gender = "sys_gender/male";

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
    "coalesce(user_id::text,''), coalesce(user_name,''), coalesce(modify_time::text,'')",
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
      "userId",
      "userName",
      "modifyTime",
    ]),
  };
}

function queryAuthUserByUserName(value) {
  const sql = [
    "select id, user_name, coalesce(person_id::text,''), coalesce(person_code,''),",
    "coalesce(person_name,''), coalesce(company_id::text,''), coalesce(valid::text,''),",
    "coalesce(user_type::text,''), coalesce(description,''), coalesce(time_zone,''),",
    "coalesce(has_lock::text,''), left(coalesce(password,''), 4), length(coalesce(password,''))::text",
    "from public.auth_user",
    `where user_name = ${sqlLiteral(value)}`,
    "order by create_time desc nulls last, id desc;",
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
      "userType",
      "description",
      "timeZone",
      "hasLock",
      "passwordPrefix",
      "passwordLength",
    ]),
  };
}

function queryUserRoleCount(userId) {
  const sql = [
    "select count(*)::text",
    "from public.auth_user_role",
    `where user_id = ${sqlLiteral(userId)};`,
  ].join(" ");
  return { sql, rows: parseTableRows(runSql(sql), ["count"]) };
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

function redactPayload(payload) {
  if (!payload) {
    return payload;
  }
  const clone = { ...payload };
  if (clone.password) {
    clone.password = "[REDACTED]";
  }
  return clone;
}

function isLockedValue(value) {
  return value === "true" || value === "1";
}

function isUnlockedValue(value) {
  return value === "false" || value === "0";
}

function requestMatcher(url) {
  return /\/inter-api\/auth\/v1\/user(\/|\?|$)/.test(url) ||
    /\/inter-api\/organization\/v1\/person(\/|\?|$)/.test(url);
}

async function main() {
  ensureDir(outputDir);
  ensureDir(path.dirname(outputPath));

  if (!/^[0-9a-zA-Z_]+$/.test(personCode) || personCode.length > 50) {
    throw new Error(`Person code must match ^[0-9a-zA-Z_]+$ and be <= 50 chars: ${personCode}`);
  }
  if (!/^\w+$/.test(userName) || userName.length > 50) {
    throw new Error(`User name must match ^\\w+$ and be <= 50 chars: ${userName}`);
  }

  const dependencyPrecheck = queryMainPosition(mainPositionId);
  if (!dependencyPrecheck.rows.length) {
    throw new Error(`No active main position ${mainPositionId} found`);
  }
  const mainPosition = dependencyPrecheck.rows[0];

  const personPrecheck = queryPersonByCode(personCode);
  if (personPrecheck.rows.some((row) => row.valid === "1")) {
    throw new Error(`Active test person already exists for code ${personCode}`);
  }
  const authPrecheck = queryAuthUserByUserName(userName);
  if (authPrecheck.rows.some((row) => row.valid === "1")) {
    throw new Error(`Active test user already exists for userName ${userName}`);
  }

  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const ticket = await login(api);
  await api.dispose();

  const browser = await chromium.launch({ headless });
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

  const page = await context.newPage();
  const consoleErrors = [];
  const pageErrors = [];
  const requestFailures = [];
  const capturedRequests = [];
  const capturedResponses = [];

  page.on("console", (message) => {
    if (["error", "warning"].includes(message.type())) {
      consoleErrors.push({ type: message.type(), text: message.text() });
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
        postData: requestItem.postData()
          ? requestItem.postData().replace(/"password"\s*:\s*"[^"]*"/g, '"password":"[REDACTED]"')
          : null,
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

  const authUrl = `${browserBaseUrl}/auth/#/user`;
  const navigation = await page.goto(authUrl, { waitUntil: "domcontentloaded", timeout: 45000 });
  await page.waitForLoadState("networkidle", { timeout: 15000 }).catch(() => {});
  await page.waitForTimeout(1500);

  const personCreatePayload = {
    code: personCode,
    name: personName,
    gender,
    mainPosition: mainPositionId,
    status: activeStatus,
    phone: createPhone,
    email: createEmail,
    description: `${marker} prerequisite person for auth user page`,
    createUser: false,
    roles: [],
    roleNames: [],
  };
  const personCreateResult = await browserApi(
    page,
    "POST",
    "/inter-api/organization/v1/person",
    personCreatePayload
  );
  if (!personCreateResult.ok || visibleErrorPattern.test(personCreateResult.text)) {
    throw new Error(`Prerequisite person create failed with ${personCreateResult.status}: ${personCreateResult.text.slice(0, 800)}`);
  }
  await page.waitForLoadState("networkidle", { timeout: 10000 }).catch(() => {});
  await page.waitForTimeout(700);

  const personAfterCreate = queryPersonByCode(personCode);
  const createdPerson = personAfterCreate.rows.find((row) => row.valid === "1");
  if (!createdPerson) {
    throw new Error(`Prerequisite person API returned success but no active org_person row found for ${personCode}`);
  }
  const personId = createdPerson.id;

  const userCreatePayload = {
    userName,
    password: userPassword,
    personId: Number(personId),
    role: [],
    timeZone: "CST+08:00",
    description: createDescription,
    userType: 0,
  };
  const userCreateResult = await browserApi(page, "POST", "/inter-api/auth/v1/user", userCreatePayload);
  if (!userCreateResult.ok || visibleErrorPattern.test(userCreateResult.text)) {
    throw new Error(`Auth user create failed with ${userCreateResult.status}: ${userCreateResult.text.slice(0, 800)}`);
  }
  await page.waitForLoadState("networkidle", { timeout: 10000 }).catch(() => {});
  await page.waitForTimeout(900);

  const authAfterCreate = queryAuthUserByUserName(userName);
  const createdUser = authAfterCreate.rows.find((row) => row.valid === "1");
  const personAfterUserCreate = queryPersonByCode(personCode);
  const boundPerson = personAfterUserCreate.rows.find((row) => row.id === personId && row.valid === "1");
  if (
    !createdUser ||
    createdUser.userName !== userName ||
    createdUser.personId !== personId ||
    createdUser.personCode !== personCode ||
    createdUser.personName !== personName ||
    createdUser.companyId !== mainPosition.companyId ||
    createdUser.userType !== "0" ||
    createdUser.description !== createDescription ||
    createdUser.timeZone !== "CST+08:00" ||
    !createdUser.passwordPrefix ||
    Number(createdUser.passwordLength) <= userPassword.length ||
    !boundPerson ||
    boundPerson.userId !== createdUser.id ||
    boundPerson.userName !== userName
  ) {
    throw new Error(
      `Auth user create did not persist expected values: ${JSON.stringify({
        authRows: authAfterCreate.rows,
        personRows: personAfterUserCreate.rows,
      })}`
    );
  }
  const roleCountAfterCreate = queryUserRoleCount(createdUser.id);

  const userUpdatePayload = {
    id: Number(createdUser.id),
    personId: Number(personId),
    role: [],
    timeZone: "JST+09:00",
    description: updateDescription,
    userType: 0,
  };
  const userUpdateResult = await browserApi(page, "PUT", "/inter-api/auth/v1/user", userUpdatePayload);
  if (!userUpdateResult.ok || visibleErrorPattern.test(userUpdateResult.text)) {
    throw new Error(`Auth user update failed with ${userUpdateResult.status}: ${userUpdateResult.text.slice(0, 800)}`);
  }
  await page.waitForLoadState("networkidle", { timeout: 10000 }).catch(() => {});
  await page.waitForTimeout(900);

  const authAfterUpdate = queryAuthUserByUserName(userName);
  const updatedUser = authAfterUpdate.rows.find((row) => row.id === createdUser.id && row.valid === "1");
  const roleCountAfterUpdate = queryUserRoleCount(createdUser.id);
  if (
    !updatedUser ||
    updatedUser.description !== updateDescription ||
    updatedUser.timeZone !== "JST+09:00" ||
    updatedUser.personId !== personId ||
    updatedUser.personName !== personName
  ) {
    throw new Error(`Auth user update did not persist expected values: ${JSON.stringify(authAfterUpdate.rows)}`);
  }

  const lockPayload = { id: Number(createdUser.id), lock: true };
  const lockResult = await browserApi(page, "PUT", "/inter-api/auth/v1/user/status", lockPayload);
  if (!lockResult.ok || visibleErrorPattern.test(lockResult.text)) {
    throw new Error(`Auth user lock failed with ${lockResult.status}: ${lockResult.text.slice(0, 800)}`);
  }
  await page.waitForTimeout(700);
  const authAfterLock = queryAuthUserByUserName(userName);
  const lockedUser = authAfterLock.rows.find((row) => row.id === createdUser.id && row.valid === "1");
  if (!lockedUser || !isLockedValue(lockedUser.hasLock)) {
    throw new Error(`Auth user lock did not persist has_lock=true: ${JSON.stringify(authAfterLock.rows)}`);
  }

  const unlockPayload = { id: Number(createdUser.id), lock: false };
  const unlockResult = await browserApi(page, "PUT", "/inter-api/auth/v1/user/status", unlockPayload);
  if (!unlockResult.ok || visibleErrorPattern.test(unlockResult.text)) {
    throw new Error(`Auth user unlock failed with ${unlockResult.status}: ${unlockResult.text.slice(0, 800)}`);
  }
  await page.waitForTimeout(700);
  const authAfterUnlock = queryAuthUserByUserName(userName);
  const unlockedUser = authAfterUnlock.rows.find((row) => row.id === createdUser.id && row.valid === "1");
  if (!unlockedUser || !isUnlockedValue(unlockedUser.hasLock)) {
    throw new Error(`Auth user unlock did not persist has_lock=false: ${JSON.stringify(authAfterUnlock.rows)}`);
  }

  const userDeletePayload = { ids: [Number(createdUser.id)] };
  const userDeleteResult = await browserApi(page, "DELETE", "/inter-api/auth/v1/user", userDeletePayload);
  if (!userDeleteResult.ok || visibleErrorPattern.test(userDeleteResult.text)) {
    throw new Error(`Auth user delete failed with ${userDeleteResult.status}: ${userDeleteResult.text.slice(0, 800)}`);
  }
  await page.waitForLoadState("networkidle", { timeout: 10000 }).catch(() => {});
  await page.waitForTimeout(900);

  const authAfterDelete = queryAuthUserByUserName(userName);
  const deletedUser = authAfterDelete.rows.find((row) => row.id === createdUser.id && row.valid === "0");
  const personAfterUserDelete = queryPersonByCode(personCode);
  const unboundPerson = personAfterUserDelete.rows.find((row) => row.id === personId && row.valid === "1");
  const roleCountAfterDelete = queryUserRoleCount(createdUser.id);
  if (!deletedUser || !unboundPerson || unboundPerson.userId || unboundPerson.userName) {
    throw new Error(
      `Auth user delete did not soft-delete user and clear person binding: ${JSON.stringify({
        authRows: authAfterDelete.rows,
        personRows: personAfterUserDelete.rows,
      })}`
    );
  }
  if (roleCountAfterDelete.rows[0] && roleCountAfterDelete.rows[0].count !== "0") {
    throw new Error(`Auth user delete did not clear auth_user_role: ${JSON.stringify(roleCountAfterDelete.rows)}`);
  }

  const personDeleteResult = await browserApi(page, "DELETE", `/inter-api/organization/v1/person/${personId}`);
  if (!personDeleteResult.ok || visibleErrorPattern.test(personDeleteResult.text)) {
    throw new Error(`Cleanup person delete failed with ${personDeleteResult.status}: ${personDeleteResult.text.slice(0, 800)}`);
  }
  await page.waitForTimeout(700);
  const personAfterCleanup = queryPersonByCode(personCode);
  const cleanedPerson = personAfterCleanup.rows.find((row) => row.id === personId && row.valid === "0");
  if (!cleanedPerson) {
    throw new Error(`Cleanup person delete did not soft-delete org_person: ${JSON.stringify(personAfterCleanup.rows)}`);
  }

  const bodyText = await page.locator("body").innerText({ timeout: 5000 }).catch(() => "");
  const visibleError = String(bodyText)
    .split(/\r?\n/)
    .map((line) => line.trim())
    .find((line) => line && visibleErrorPattern.test(line));
  const screenshotPath = path.join(outputDir, "auth-user-persistence.png");
  await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => {});
  await browser.close();

  const report = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    browserBaseUrl,
    username,
    marker,
    route: "/auth/#/user",
    userName,
    personCode,
    userPasswordMode: "generated and redacted; auth_user.password verified as encoded non-plaintext value",
    dependencies: {
      mainPositionId,
      positionPrecheckSql: dependencyPrecheck.sql,
      positionPrecheckRows: dependencyPrecheck.rows,
    },
    backendTrace: {
      create:
        "UserController.createUser -> UserServiceImpl.creatUser -> userMapper.insert -> public.auth_user -> PersonServiceAdapter.saveOrUpdateUsers -> public.org_person.user_id/user_name",
      update:
        "UserController.updateUser -> UserServiceImpl.updateUser -> userMapper.update -> public.auth_user.description/time_zone/person fields -> UserRoleService refreshes public.auth_user_role",
      lock:
        "UserController.lockUser -> UserServiceImpl.updateUser(hasLock=true) -> userMapper.update -> public.auth_user.has_lock",
      unlock:
        "UserController.lockUser -> UserServiceImpl.updateUser(hasLock=false) -> userMapper.update -> public.auth_user.has_lock",
      delete:
        "UserController.batchDeleteUser -> UserServiceImpl.batchDelet -> userMapper.delete soft delete public.auth_user -> userRoleService.remove public.auth_user_role -> PersonServiceAdapter.deleteUsersByPersonIds clears public.org_person binding",
      cleanup:
        "PersonInterController.deletePerson -> PersonService.deletePerson -> public.org_person.valid=0",
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
      prerequisitePersonCreate: {
        method: "POST",
        api: "/inter-api/organization/v1/person",
        payload: personCreatePayload,
        responseStatus: personCreateResult.status,
        responseBody: personCreateResult.json || personCreateResult.text.slice(0, 1000),
        verificationSql: personAfterCreate.sql,
        rows: personAfterCreate.rows,
      },
      create: {
        method: "POST",
        api: "/inter-api/auth/v1/user",
        payload: redactPayload(userCreatePayload),
        responseStatus: userCreateResult.status,
        responseBody: userCreateResult.json || userCreateResult.text.slice(0, 1000),
        verificationSql: [authAfterCreate.sql, personAfterUserCreate.sql].join("\n"),
        rows: {
          authUser: authAfterCreate.rows,
          person: personAfterUserCreate.rows,
          authUserRoleCount: roleCountAfterCreate.rows,
        },
      },
      update: {
        method: "PUT",
        api: "/inter-api/auth/v1/user",
        payload: userUpdatePayload,
        responseStatus: userUpdateResult.status,
        responseBody: userUpdateResult.json || userUpdateResult.text.slice(0, 1000),
        verificationSql: [authAfterUpdate.sql, roleCountAfterUpdate.sql].join("\n"),
        rows: {
          authUser: authAfterUpdate.rows,
          authUserRoleCount: roleCountAfterUpdate.rows,
        },
      },
      lock: {
        method: "PUT",
        api: "/inter-api/auth/v1/user/status",
        payload: lockPayload,
        responseStatus: lockResult.status,
        responseBody: lockResult.json || lockResult.text.slice(0, 1000),
        verificationSql: authAfterLock.sql,
        rows: authAfterLock.rows,
      },
      unlock: {
        method: "PUT",
        api: "/inter-api/auth/v1/user/status",
        payload: unlockPayload,
        responseStatus: unlockResult.status,
        responseBody: unlockResult.json || unlockResult.text.slice(0, 1000),
        verificationSql: authAfterUnlock.sql,
        rows: authAfterUnlock.rows,
      },
      delete: {
        method: "DELETE",
        api: "/inter-api/auth/v1/user",
        payload: userDeletePayload,
        responseStatus: userDeleteResult.status,
        responseBody: userDeleteResult.json || userDeleteResult.text.slice(0, 1000),
        verificationSql: [authAfterDelete.sql, personAfterUserDelete.sql, roleCountAfterDelete.sql].join("\n"),
        rows: {
          authUser: authAfterDelete.rows,
          person: personAfterUserDelete.rows,
          authUserRoleCount: roleCountAfterDelete.rows,
        },
        deleteMode: "soft-delete auth_user; clear org_person.user_id/user_name; remove auth_user_role",
      },
      cleanupPersonDelete: {
        method: "DELETE",
        api: `/inter-api/organization/v1/person/${personId}`,
        payload: null,
        responseStatus: personDeleteResult.status,
        responseBody: personDeleteResult.json || personDeleteResult.text.slice(0, 1000),
        verificationSql: personAfterCleanup.sql,
        rows: personAfterCleanup.rows,
      },
    },
    capturedRequests,
    capturedResponses,
    ok:
      !visibleError &&
      consoleErrors.length === 0 &&
      pageErrors.length === 0 &&
      requestFailures.length === 0 &&
      Boolean(createdPerson) &&
      Boolean(createdUser) &&
      Boolean(updatedUser) &&
      Boolean(lockedUser) &&
      Boolean(unlockedUser) &&
      Boolean(deletedUser) &&
      Boolean(cleanedPerson),
  };

  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);
  console.log(
    `${report.ok ? "OK" : "FAIL"} auth-user-persistence marker=${marker} personId=${personId} userId=${createdUser.id}`
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
