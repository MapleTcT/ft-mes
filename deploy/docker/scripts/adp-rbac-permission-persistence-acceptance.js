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
  process.env.ADP_OUTPUT_DIR || path.join("/tmp", `adp-rbac-permission-persistence-${stamp}`);
const outputPath =
  process.env.ADP_RBAC_PERMISSION_PERSISTENCE_OUTPUT ||
  path.join(outputDir, "rbac-permission-persistence-results.json");

const dbSshTarget = process.env.ADP_DB_SSH_TARGET || "v6@10.11.100.17";
const dbContainer = process.env.ADP_DB_CONTAINER || "adp-mes-newbase-postgres-1";
const dbName = process.env.ADP_DB_NAME || "adp";
const dbUser = process.env.ADP_DB_USER || "adp";

const mainPositionId = Number(process.env.ADP_RBAC_MAIN_POSITION_ID || 1);
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${stamp}_RBAC`;
const roleCode = process.env.ADP_RBAC_ROLE_CODE || `${marker}_ROLE`;
const roleName = process.env.ADP_RBAC_ROLE_NAME || `${marker}_ROLE_NAME`;
const updatedRoleName = `${roleName}_UPDATED`;
const personCode = process.env.ADP_RBAC_PERSON_CODE || `${marker}_PER`;
const personName = process.env.ADP_RBAC_PERSON_NAME || `${marker}_PERSON`;
const userName = process.env.ADP_RBAC_USER_NAME || `adp_e2e_${stamp}_rbac_user`;
const userPassword = process.env.ADP_RBAC_USER_PASSWORD || `Ft@${stamp.slice(8, 14)}Rb1`;
const menuCode = process.env.ADP_RBAC_PERMISSION_MENU_CODE || "personmanage";
const dataResourceCode = process.env.ADP_RBAC_DATA_RESOURCE_CODE || `${marker}_DATA_RESOURCE`;
const roleDataResourceName =
  process.env.ADP_RBAC_ROLE_DATA_RESOURCE_NAME || `${marker} role data resource`;
const userDataResourceName =
  process.env.ADP_RBAC_USER_DATA_RESOURCE_NAME || `${marker} user data resource`;
const dataResourceType = process.env.ADP_RBAC_DATA_RESOURCE_TYPE || "ADP_E2E";
const activeStatus = "sys_person_status/onWork";
const gender = "sys_gender/male";

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

function redactPayload(payload) {
  if (!payload || typeof payload !== "object") {
    return payload;
  }
  const text = JSON.stringify(payload).replace(/"password"\s*:\s*"[^"]*"/g, '"password":"[REDACTED]"');
  return JSON.parse(text);
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

function isIgnorableStatic404(item) {
  return item && item.status === 404 && /\/supide-app\/runtime\/permissions\/ping\.png(?:\?|$)/.test(item.url || "");
}

function isIgnorableConsoleError(item, ignoredHttpErrors) {
  return (
    item &&
    /Failed to load resource: the server responded with a status of 404/.test(item.text || "") &&
    ignoredHttpErrors.length > 0
  );
}

function truthyDb(value) {
  return value === "true" || value === "1";
}

function falsyDb(value) {
  return value === "false" || value === "0" || value === "";
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

function queryPersonByCode(code) {
  const sql = [
    "select id, code, name, coalesce(valid::text,''), coalesce(user_id::text,''), coalesce(user_name,''),",
    "coalesce(main_position::text,''), coalesce(description,'')",
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
      "valid",
      "userId",
      "userName",
      "mainPosition",
      "description",
    ]),
  };
}

function queryAuthUserByUserName(value) {
  const sql = [
    "select id, user_name, coalesce(person_id::text,''), coalesce(person_code,''),",
    "coalesce(person_name,''), coalesce(company_id::text,''), coalesce(valid::text,''),",
    "coalesce(description,''), coalesce(time_zone,'')",
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
      "description",
      "timeZone",
    ]),
  };
}

function queryMenuByCode(code) {
  const sql = [
    "select id, code, coalesce(name,''), coalesce(valid::text,'')",
    "from public.rbac_menuinfo",
    `where code = ${sqlLiteral(code)}`,
    "order by id desc",
    "limit 1;",
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), ["id", "code", "name", "valid"]),
  };
}

function queryRoleUser(roleId, userId) {
  const sql = [
    "select id, role_id, user_id, coalesce(user_name,''), coalesce(person_name,''), coalesce(person_code,''),",
    "coalesce(valid::text,''), coalesce(from_position::text,'')",
    "from public.rbac_roleuser",
    `where role_id = ${Number(roleId)} and user_id = ${Number(userId)}`,
    "order by create_time desc nulls last, id desc;",
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), [
      "id",
      "roleId",
      "userId",
      "userName",
      "personName",
      "personCode",
      "valid",
      "fromPosition",
    ]),
  };
}

function queryRolePermission(roleId, menuOperateId) {
  const sql = [
    "select id, role_id, menuoperate_id, coalesce(cid::text,''), coalesce(no_restrict_flag::text,''),",
    "coalesce(position_flag::text,''), coalesce(assign_staff_flag::text,''), coalesce(assign_pos_flag::text,''),",
    "coalesce(assign_dept_flag::text,'')",
    "from public.rbac_rolepermission",
    `where role_id = ${Number(roleId)} and menuoperate_id = ${Number(menuOperateId)}`,
    "order by create_time desc nulls last, id desc;",
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), [
      "id",
      "roleId",
      "menuOperateId",
      "cid",
      "noRestrictFlag",
      "positionFlag",
      "assignStaffFlag",
      "assignPosFlag",
      "assignDeptFlag",
    ]),
  };
}

function queryUserPermission(userId, menuOperateId) {
  const sql = [
    "select id, user_id, menuoperate_id, coalesce(menuoperate_code,''), coalesce(purview_type::text,''),",
    "coalesce(cid::text,''), coalesce(no_restrict_flag::text,''), coalesce(position_flag::text,''),",
    "coalesce(assign_staff_flag::text,''), coalesce(assign_pos_flag::text,''), coalesce(assign_dept_flag::text,'')",
    "from public.rbac_userpermission",
    `where user_id = ${Number(userId)} and menuoperate_id = ${Number(menuOperateId)} and purview_type = 1`,
    "order by create_time desc nulls last, id desc;",
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), [
      "id",
      "userId",
      "menuOperateId",
      "menuOperateCode",
      "purviewType",
      "cid",
      "noRestrictFlag",
      "positionFlag",
      "assignStaffFlag",
      "assignPosFlag",
      "assignDeptFlag",
    ]),
  };
}

function queryDataResourceGroupsFromDb() {
  const sql = [
    "select id, group_code, group_name, resource_url, module_code, coalesce(cid::text,'')",
    "from public.rbac_data_resource_group",
    "where cid = 1000 or cid is null",
    "order by id;",
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), ["id", "groupCode", "groupName", "resourceUrl", "moduleCode", "cid"]),
  };
}

function queryRoleDataPermission(roleId, groupCode) {
  const sql = [
    "select id, role_id, cid, group_code, resource_code, resource_name, coalesce(resource_type,''),",
    "coalesce(valid::text,''), coalesce(create_time::text,'')",
    "from public.rbac_role_data_permission",
    `where role_id = ${Number(roleId)} and group_code = ${sqlLiteral(groupCode)}`,
    "order by create_time desc nulls last, id desc;",
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), [
      "id",
      "roleId",
      "cid",
      "groupCode",
      "resourceCode",
      "resourceName",
      "resourceType",
      "valid",
      "createTime",
    ]),
  };
}

function queryRoleDataPermissionCtrl(roleId, groupCode) {
  const sql = [
    "select id, role_id, cid, group_code, coalesce(controlled::text,''), coalesce(modify_time::text,'')",
    "from public.rbac_role_data_permission_ctrl",
    `where role_id = ${Number(roleId)} and group_code = ${sqlLiteral(groupCode)}`,
    "order by create_time desc nulls last, id desc;",
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), ["id", "roleId", "cid", "groupCode", "controlled", "modifyTime"]),
  };
}

function queryUserDataPermission(userId, groupCode, purviewType, roleId = null) {
  const roleFilter = roleId === null ? "" : ` and role_id = ${Number(roleId)}`;
  const sql = [
    "select id, user_id, cid, group_code, coalesce(role_id::text,''), coalesce(purview_type::text,''),",
    "resource_code, resource_name, coalesce(resource_type,''), coalesce(valid::text,''), coalesce(create_time::text,'')",
    "from public.rbac_user_data_permission",
    `where user_id = ${Number(userId)} and group_code = ${sqlLiteral(groupCode)} and purview_type = ${Number(purviewType)}${roleFilter}`,
    "order by create_time desc nulls last, id desc;",
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), [
      "id",
      "userId",
      "cid",
      "groupCode",
      "roleId",
      "purviewType",
      "resourceCode",
      "resourceName",
      "resourceType",
      "valid",
      "createTime",
    ]),
  };
}

function queryUserDataPermissionCtrl(userId, groupCode) {
  const sql = [
    "select id, user_id, cid, group_code, coalesce(controlled::text,''), coalesce(modify_time::text,'')",
    "from public.rbac_user_data_permission_ctrl",
    `where user_id = ${Number(userId)} and group_code = ${sqlLiteral(groupCode)}`,
    "order by create_time desc nulls last, id desc;",
  ].join(" ");
  return {
    sql,
    rows: parseTableRows(runSql(sql), ["id", "userId", "cid", "groupCode", "controlled", "modifyTime"]),
  };
}

function requestMatcher(url) {
  return (
    /\/inter-api\/rbac\/v1\/(role|roleUser|rolePermission|userPermission|rolePermissions|userPermissions|data\/resource)(\/|\?|$)/.test(url) ||
    /\/inter-api\/rbac\/v1\/(role|user)\/\d+\/data\/resource(\/|\?|$)/.test(url) ||
    /\/inter-api\/organization\/v1\/person(\/|\?|$)/.test(url) ||
    /\/inter-api\/auth\/v1\/user(\/|\?|$)/.test(url)
  );
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

function pickArray(...values) {
  return values.find((value) => Array.isArray(value));
}

function permissionData(payload) {
  const root = (payload && payload.data) || payload || {};
  return {
    assign: pickArray(root.assign, root.data && root.data.assign) || [],
    unassign: pickArray(root.unassign, root.data && root.data.unassign) || [],
  };
}

function selectPermissionCandidate(permissionResult, kind) {
  const groups = permissionData(permissionResult.json);
  for (const item of groups.unassign) {
    const op = item && (item.op || item.operate || item);
    if (!op || op.flowKey || op.flow_key) {
      continue;
    }
    const permission = kind === "role" ? op.rolePermission : op.userPermission;
    const menuOperateId = Number((permission && permission.menuOperateId) || op.id);
    if (!menuOperateId) {
      continue;
    }
    const payload = {
      ...(permission || {}),
      menuOperateId,
      noRestrictFlag: true,
      positionFlag: false,
      departmentFlag: false,
      assignStaffFlag: false,
      assignPosFlag: false,
      assignDeptFlag: false,
      dealerPermissionFlag: false,
      assignCustomPermissionFlag: false,
      assignDataPermissionFlag: false,
    };
    if (kind === "user") {
      payload.menuOperateCode = payload.menuOperateCode || op.code || "";
      payload.purviewType = 1;
    }
    return {
      item,
      op,
      payload,
      menuOperateId,
      menuOperateCode: op.code || payload.menuOperateCode || "",
      menuOperateName: op.name || op.nameDisplay || "",
      countsBefore: { assign: groups.assign.length, unassign: groups.unassign.length },
    };
  }
  throw new Error(`No non-flow unassigned ${kind} permission operation found in response`);
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

  const rolePrecheck = queryRoleByCode(roleCode);
  if (rolePrecheck.rows.some((row) => truthyDb(row.valid))) {
    throw new Error(`Active test role already exists for code ${roleCode}`);
  }
  const personPrecheck = queryPersonByCode(personCode);
  if (personPrecheck.rows.some((row) => row.valid === "1")) {
    throw new Error(`Active test person already exists for code ${personCode}`);
  }
  const authPrecheck = queryAuthUserByUserName(userName);
  if (authPrecheck.rows.some((row) => row.valid === "1")) {
    throw new Error(`Active test user already exists for userName ${userName}`);
  }
  const menuPrecheck = queryMenuByCode(menuCode);
  if (!menuPrecheck.rows.length) {
    throw new Error(`No RBAC menu found for code ${menuCode}`);
  }
  const selectedMenu = menuPrecheck.rows[0];
  const dataResourceGroupPrecheck = queryDataResourceGroupsFromDb();
  if (!dataResourceGroupPrecheck.rows.length) {
    throw new Error("No RBAC data resource group found in public.rbac_data_resource_group");
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
  const consoleWarnings = [];
  const pageErrors = [];
  const requestFailures = [];
  const nonApiHttpErrors = [];
  const capturedRequests = [];
  const capturedResponses = [];
  const navigations = [];
  const cleanup = {
    userId: null,
    personId: null,
    roleId: null,
    roleUserId: null,
    rolePermissionId: null,
    userPermissionId: null,
  };

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
        postData: requestItem.postData()
          ? requestItem.postData().replace(/"password"\s*:\s*"[^"]*"/g, '"password":"[REDACTED]"')
          : null,
      });
    }
  });
  page.on("response", async (response) => {
    if (response.status() >= 400 && !requestMatcher(response.url())) {
      nonApiHttpErrors.push({
        method: response.request().method(),
        url: response.url(),
        status: response.status(),
      });
    }
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

  const operationLog = {};
  let roleAfterCreate;
  let roleAfterUpdate;
  let roleAfterDelete;
  let personAfterCreate;
  let authAfterCreate;
  let roleUserAfterCreate;
  let roleUserAfterDelete;
  let dataResourceGroupResult;
  let selectedDataResourceGroup;
  let selectedDataResourceGroupCode;
  let selectedDataResourceGroupName;
  let roleDataPermissionAfterCreate;
  let roleDataPermissionCtrlAfterCreate;
  let inheritedUserDataPermissionAfterRoleCreate;
  let roleDataPermissionAfterDisable;
  let roleDataPermissionCtrlAfterDisable;
  let inheritedUserDataPermissionAfterRoleDisable;
  let userDataPermissionAfterCreate;
  let userDataPermissionCtrlAfterCreate;
  let userDataPermissionAfterDisable;
  let userDataPermissionCtrlAfterDisable;
  let rolePermissionBefore;
  let rolePermissionCandidate;
  let rolePermissionAfterCreate;
  let rolePermissionAfterDelete;
  let userPermissionBefore;
  let userPermissionCandidate;
  let userPermissionAfterCreate;
  let userPermissionAfterDelete;
  let authAfterDelete;
  let personAfterUserDelete;
  let personAfterCleanup;

  try {
    const rolePage = `${baseUrl}/auth/#/role`;
    navigations.push({ route: "/auth/#/role", status: (await openAndSettle(page, rolePage))?.status() || null });

    const roleCreatePayload = {
      code: roleCode,
      name: roleName,
      description: `${marker} create via role page`,
      roleType: "ROLE_TYPE/default",
      tags: [],
    };
    const roleCreateResult = await browserApi(page, "POST", "/inter-api/rbac/v1/role", roleCreatePayload);
    ensureApiOk(roleCreateResult, "Role create");
    await page.waitForTimeout(800);
    roleAfterCreate = queryRoleByCode(roleCode);
    const createdRole = roleAfterCreate.rows.find((row) => truthyDb(row.valid));
    if (!createdRole || createdRole.name !== roleName || createdRole.description !== `${marker} create via role page`) {
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

    const roleUpdatePayload = {
      code: roleCode,
      name: updatedRoleName,
      description: `${marker} update via role page`,
      roleType: "ROLE_TYPE/default",
      tags: [],
      deleteIds: [],
    };
    const roleUpdateResult = await browserApi(page, "PUT", "/inter-api/rbac/v1/role", roleUpdatePayload);
    ensureApiOk(roleUpdateResult, "Role update");
    await page.waitForTimeout(800);
    roleAfterUpdate = queryRoleByCode(roleCode);
    const updatedRole = roleAfterUpdate.rows.find((row) => row.id === String(cleanup.roleId) && truthyDb(row.valid));
    if (!updatedRole || updatedRole.name !== updatedRoleName || updatedRole.description !== `${marker} update via role page`) {
      throw new Error(`Role update did not persist expected values: ${JSON.stringify(roleAfterUpdate.rows)}`);
    }
    operationLog.roleUpdate = {
      method: "PUT",
      api: "/inter-api/rbac/v1/role",
      payload: roleUpdatePayload,
      responseStatus: roleUpdateResult.status,
      responseBody: roleUpdateResult.json || roleUpdateResult.text.slice(0, 1000),
      verificationSql: roleAfterUpdate.sql,
      rows: roleAfterUpdate.rows,
    };

    const personCreatePayload = {
      code: personCode,
      name: personName,
      gender,
      mainPosition: mainPositionId,
      status: activeStatus,
      description: `${marker} prerequisite person for RBAC acceptance`,
      createUser: false,
      roles: [],
      roleNames: [],
    };
    const personCreateResult = await browserApi(page, "POST", "/inter-api/organization/v1/person", personCreatePayload);
    ensureApiOk(personCreateResult, "Prerequisite person create");
    await page.waitForTimeout(700);
    personAfterCreate = queryPersonByCode(personCode);
    const createdPerson = personAfterCreate.rows.find((row) => row.valid === "1");
    if (!createdPerson) {
      throw new Error(`Prerequisite person create did not persist: ${JSON.stringify(personAfterCreate.rows)}`);
    }
    cleanup.personId = Number(createdPerson.id);

    const userCreatePayload = {
      userName,
      password: userPassword,
      personId: cleanup.personId,
      role: [],
      timeZone: "CST+08:00",
      description: `${marker} prerequisite auth user for RBAC acceptance`,
      userType: 0,
    };
    const userCreateResult = await browserApi(page, "POST", "/inter-api/auth/v1/user", userCreatePayload);
    ensureApiOk(userCreateResult, "Prerequisite auth user create");
    await page.waitForTimeout(900);
    authAfterCreate = queryAuthUserByUserName(userName);
    const createdUser = authAfterCreate.rows.find((row) => row.valid === "1");
    if (
      !createdUser ||
      createdUser.personId !== String(cleanup.personId) ||
      createdUser.personCode !== personCode ||
      createdUser.personName !== personName
    ) {
      throw new Error(`Prerequisite auth user create did not persist: ${JSON.stringify(authAfterCreate.rows)}`);
    }
    cleanup.userId = Number(createdUser.id);
    operationLog.prerequisites = {
      personCreate: {
        method: "POST",
        api: "/inter-api/organization/v1/person",
        payload: personCreatePayload,
        responseStatus: personCreateResult.status,
        responseBody: personCreateResult.json || personCreateResult.text.slice(0, 1000),
        verificationSql: personAfterCreate.sql,
        rows: personAfterCreate.rows,
      },
      userCreate: {
        method: "POST",
        api: "/inter-api/auth/v1/user",
        payload: redactPayload(userCreatePayload),
        responseStatus: userCreateResult.status,
        responseBody: userCreateResult.json || userCreateResult.text.slice(0, 1000),
        verificationSql: authAfterCreate.sql,
        rows: authAfterCreate.rows,
      },
    };

    const roleUserPayload = {
      roleId: cleanup.roleId,
      users: [
        {
          id: cleanup.userId,
          userName,
          personName,
          personCode,
        },
      ],
    };
    const roleUserCreateResult = await browserApi(page, "POST", "/inter-api/rbac/v1/roleUser", roleUserPayload);
    ensureApiOk(roleUserCreateResult, "Role user create");
    await page.waitForTimeout(1000);
    roleUserAfterCreate = queryRoleUser(cleanup.roleId, cleanup.userId);
    const createdRoleUser = roleUserAfterCreate.rows.find((row) => truthyDb(row.valid) && row.fromPosition === "1");
    if (!createdRoleUser || createdRoleUser.userName !== userName || createdRoleUser.personCode !== personCode) {
      throw new Error(`Role user create did not persist expected values: ${JSON.stringify(roleUserAfterCreate.rows)}`);
    }
    cleanup.roleUserId = Number(createdRoleUser.id);
    operationLog.roleUserCreate = {
      method: "POST",
      api: "/inter-api/rbac/v1/roleUser",
      payload: roleUserPayload,
      responseStatus: roleUserCreateResult.status,
      responseBody: roleUserCreateResult.json || roleUserCreateResult.text.slice(0, 1000),
      verificationSql: roleUserAfterCreate.sql,
      rows: roleUserAfterCreate.rows,
    };

    dataResourceGroupResult = await browserApi(page, "GET", "/inter-api/rbac/v1/data/resource/groups");
    ensureApiOk(dataResourceGroupResult, "Data resource groups load");
    const dataResourceGroups =
      (dataResourceGroupResult.json &&
        dataResourceGroupResult.json.data &&
        dataResourceGroupResult.json.data.list) ||
      (dataResourceGroupResult.json && dataResourceGroupResult.json.list) ||
      [];
    selectedDataResourceGroup = dataResourceGroups[0] || dataResourceGroupPrecheck.rows[0];
    selectedDataResourceGroupCode =
      selectedDataResourceGroup.groupCode || selectedDataResourceGroup.group_code || selectedDataResourceGroup.code;
    selectedDataResourceGroupName =
      selectedDataResourceGroup.groupName || selectedDataResourceGroup.group_name || selectedDataResourceGroup.name || "";
    if (!selectedDataResourceGroupCode) {
      throw new Error(`No usable data resource group code found: ${JSON.stringify(selectedDataResourceGroup)}`);
    }

    const roleDataPermissionPayload = {
      controlled: true,
      dataResouceVOS: [
        {
          resourceCode: `${dataResourceCode}_ROLE`,
          resourceName: roleDataResourceName,
          resourceType: dataResourceType,
        },
      ],
    };
    const roleDataPermissionResult = await browserApi(
      page,
      "POST",
      `/inter-api/rbac/v1/role/${cleanup.roleId}/data/resource/${encodeURIComponent(selectedDataResourceGroupCode)}`,
      roleDataPermissionPayload
    );
    ensureApiOk(roleDataPermissionResult, "Role data resource permission save");
    await page.waitForTimeout(1000);
    roleDataPermissionAfterCreate = queryRoleDataPermission(cleanup.roleId, selectedDataResourceGroupCode);
    roleDataPermissionCtrlAfterCreate = queryRoleDataPermissionCtrl(cleanup.roleId, selectedDataResourceGroupCode);
    inheritedUserDataPermissionAfterRoleCreate = queryUserDataPermission(
      cleanup.userId,
      selectedDataResourceGroupCode,
      0,
      cleanup.roleId
    );
    const activeRoleDataPermission = roleDataPermissionAfterCreate.rows.find(
      (row) =>
        row.resourceCode === `${dataResourceCode}_ROLE` &&
        row.resourceName === roleDataResourceName &&
        truthyDb(row.valid)
    );
    const activeRoleDataCtrl = roleDataPermissionCtrlAfterCreate.rows.find((row) => row.controlled === "1");
    const inheritedUserDataPermission = inheritedUserDataPermissionAfterRoleCreate.rows.find(
      (row) =>
        row.resourceCode === `${dataResourceCode}_ROLE` &&
        row.resourceName === roleDataResourceName &&
        row.purviewType === "0" &&
        row.roleId === String(cleanup.roleId) &&
        truthyDb(row.valid)
    );
    if (!activeRoleDataPermission || !activeRoleDataCtrl || !inheritedUserDataPermission) {
      throw new Error(
        `Role data resource save did not persist expected role/user rows: ${JSON.stringify({
          roleRows: roleDataPermissionAfterCreate.rows,
          roleCtrlRows: roleDataPermissionCtrlAfterCreate.rows,
          inheritedUserRows: inheritedUserDataPermissionAfterRoleCreate.rows,
        })}`
      );
    }
    operationLog.roleDataResourceSave = {
      method: "POST",
      api: `/inter-api/rbac/v1/role/${cleanup.roleId}/data/resource/${selectedDataResourceGroupCode}`,
      payload: roleDataPermissionPayload,
      responseStatus: roleDataPermissionResult.status,
      responseBody: roleDataPermissionResult.json || roleDataPermissionResult.text.slice(0, 1000),
      selectedGroup: {
        groupCode: selectedDataResourceGroupCode,
        groupName: selectedDataResourceGroupName,
      },
      verificationSql: [
        roleDataPermissionAfterCreate.sql,
        roleDataPermissionCtrlAfterCreate.sql,
        inheritedUserDataPermissionAfterRoleCreate.sql,
      ].join("\n"),
      rows: {
        roleDataPermission: roleDataPermissionAfterCreate.rows,
        roleDataPermissionCtrl: roleDataPermissionCtrlAfterCreate.rows,
        inheritedUserDataPermission: inheritedUserDataPermissionAfterRoleCreate.rows,
      },
    };

    const roleDataPermissionDisablePayload = { controlled: false, dataResouceVOS: [] };
    const roleDataPermissionDisableResult = await browserApi(
      page,
      "POST",
      `/inter-api/rbac/v1/role/${cleanup.roleId}/data/resource/${encodeURIComponent(selectedDataResourceGroupCode)}`,
      roleDataPermissionDisablePayload
    );
    ensureApiOk(roleDataPermissionDisableResult, "Role data resource permission disable");
    await page.waitForTimeout(1000);
    roleDataPermissionAfterDisable = queryRoleDataPermission(cleanup.roleId, selectedDataResourceGroupCode);
    roleDataPermissionCtrlAfterDisable = queryRoleDataPermissionCtrl(cleanup.roleId, selectedDataResourceGroupCode);
    inheritedUserDataPermissionAfterRoleDisable = queryUserDataPermission(
      cleanup.userId,
      selectedDataResourceGroupCode,
      0,
      cleanup.roleId
    );
    const activeRoleDataAfterDisable = roleDataPermissionAfterDisable.rows.find((row) => truthyDb(row.valid));
    const activeInheritedUserDataAfterDisable = inheritedUserDataPermissionAfterRoleDisable.rows.find((row) =>
      truthyDb(row.valid)
    );
    const disabledRoleDataCtrl = roleDataPermissionCtrlAfterDisable.rows.find((row) => row.controlled === "0");
    if (activeRoleDataAfterDisable || activeInheritedUserDataAfterDisable || !disabledRoleDataCtrl) {
      throw new Error(
        `Role data resource disable did not clear active rows or set controlled=0: ${JSON.stringify({
          roleRows: roleDataPermissionAfterDisable.rows,
          roleCtrlRows: roleDataPermissionCtrlAfterDisable.rows,
          inheritedUserRows: inheritedUserDataPermissionAfterRoleDisable.rows,
        })}`
      );
    }
    operationLog.roleDataResourceDisable = {
      method: "POST",
      api: `/inter-api/rbac/v1/role/${cleanup.roleId}/data/resource/${selectedDataResourceGroupCode}`,
      payload: roleDataPermissionDisablePayload,
      responseStatus: roleDataPermissionDisableResult.status,
      responseBody: roleDataPermissionDisableResult.json || roleDataPermissionDisableResult.text.slice(0, 1000),
      verificationSql: [
        roleDataPermissionAfterDisable.sql,
        roleDataPermissionCtrlAfterDisable.sql,
        inheritedUserDataPermissionAfterRoleDisable.sql,
      ].join("\n"),
      rows: {
        roleDataPermission: roleDataPermissionAfterDisable.rows,
        roleDataPermissionCtrl: roleDataPermissionCtrlAfterDisable.rows,
        inheritedUserDataPermission: inheritedUserDataPermissionAfterRoleDisable.rows,
      },
    };

    const roleUserDeleteResult = await browserApi(
      page,
      "DELETE",
      `/inter-api/rbac/v1/roleUser/${cleanup.roleUserId}`
    );
    ensureApiOk(roleUserDeleteResult, "Role user delete");
    await page.waitForTimeout(1000);
    roleUserAfterDelete = queryRoleUser(cleanup.roleId, cleanup.userId);
    const activeRoleUserAfterDelete = roleUserAfterDelete.rows.find((row) => truthyDb(row.valid));
    if (activeRoleUserAfterDelete) {
      throw new Error(`Role user delete left an active row: ${JSON.stringify(roleUserAfterDelete.rows)}`);
    }
    cleanup.roleUserId = null;
    operationLog.roleUserDelete = {
      method: "DELETE",
      api: `/inter-api/rbac/v1/roleUser/${createdRoleUser.id}`,
      payload: null,
      responseStatus: roleUserDeleteResult.status,
      responseBody: roleUserDeleteResult.json || roleUserDeleteResult.text.slice(0, 1000),
      verificationSql: roleUserAfterDelete.sql,
      rows: roleUserAfterDelete.rows,
    };

    const authorityRolePage = `${baseUrl}/auth/#/authority?status=role&id=${cleanup.roleId}&name=${encodeURIComponent(updatedRoleName)}`;
    navigations.push({
      route: `/auth/#/authority?status=role&id=${cleanup.roleId}&name=${updatedRoleName}`,
      status: (await openAndSettle(page, authorityRolePage))?.status() || null,
    });
    const rolePermissionLoadResult = await browserApi(
      page,
      "GET",
      `/inter-api/rbac/v1/rolePermissions?menuId=${selectedMenu.id}&roleId=${cleanup.roleId}`
    );
    ensureApiOk(rolePermissionLoadResult, "Role permission load");
    rolePermissionCandidate = selectPermissionCandidate(rolePermissionLoadResult, "role");
    rolePermissionBefore = queryRolePermission(cleanup.roleId, rolePermissionCandidate.menuOperateId);
    if (rolePermissionBefore.rows.some((row) => row.roleId === String(cleanup.roleId))) {
      throw new Error(`Role permission already exists before test: ${JSON.stringify(rolePermissionBefore.rows)}`);
    }
    const rolePermissionAddPayload = {
      list: [
        {
          roleId: cleanup.roleId,
          addList: [{ ...rolePermissionCandidate.payload, roleId: cleanup.roleId }],
          deleteList: [],
        },
      ],
    };
    const rolePermissionAddResult = await browserApi(
      page,
      "POST",
      "/inter-api/rbac/v1/rolePermission",
      rolePermissionAddPayload
    );
    ensureApiOk(rolePermissionAddResult, "Role permission add");
    await page.waitForTimeout(1000);
    rolePermissionAfterCreate = queryRolePermission(cleanup.roleId, rolePermissionCandidate.menuOperateId);
    const createdRolePermission = rolePermissionAfterCreate.rows.find(
      (row) => row.roleId === String(cleanup.roleId) && row.menuOperateId === String(rolePermissionCandidate.menuOperateId)
    );
    if (!createdRolePermission || !truthyDb(createdRolePermission.noRestrictFlag)) {
      throw new Error(
        `Role permission add did not persist expected no_restrict row: ${JSON.stringify(rolePermissionAfterCreate.rows)}`
      );
    }
    cleanup.rolePermissionId = Number(createdRolePermission.id);
    operationLog.rolePermissionAdd = {
      method: "POST",
      api: "/inter-api/rbac/v1/rolePermission",
      payload: rolePermissionAddPayload,
      responseStatus: rolePermissionAddResult.status,
      responseBody: rolePermissionAddResult.json || rolePermissionAddResult.text.slice(0, 1000),
      verificationSql: rolePermissionAfterCreate.sql,
      rows: rolePermissionAfterCreate.rows,
      selectedOperation: {
        menuCode,
        menuId: selectedMenu.id,
        menuOperateId: rolePermissionCandidate.menuOperateId,
        menuOperateCode: rolePermissionCandidate.menuOperateCode,
        menuOperateName: rolePermissionCandidate.menuOperateName,
      },
    };

    const rolePermissionDeletePayload = {
      list: [
        {
          roleId: cleanup.roleId,
          addList: [],
          deleteList: [
            {
              id: cleanup.rolePermissionId,
              roleId: cleanup.roleId,
              menuOperateId: rolePermissionCandidate.menuOperateId,
              noRestrictFlag: true,
            },
          ],
        },
      ],
    };
    const rolePermissionDeleteResult = await browserApi(
      page,
      "POST",
      "/inter-api/rbac/v1/rolePermission",
      rolePermissionDeletePayload
    );
    ensureApiOk(rolePermissionDeleteResult, "Role permission delete");
    await page.waitForTimeout(1000);
    rolePermissionAfterDelete = queryRolePermission(cleanup.roleId, rolePermissionCandidate.menuOperateId);
    if (rolePermissionAfterDelete.rows.length) {
      throw new Error(`Role permission delete left rows: ${JSON.stringify(rolePermissionAfterDelete.rows)}`);
    }
    cleanup.rolePermissionId = null;
    operationLog.rolePermissionDelete = {
      method: "POST",
      api: "/inter-api/rbac/v1/rolePermission",
      payload: rolePermissionDeletePayload,
      responseStatus: rolePermissionDeleteResult.status,
      responseBody: rolePermissionDeleteResult.json || rolePermissionDeleteResult.text.slice(0, 1000),
      verificationSql: rolePermissionAfterDelete.sql,
      rows: rolePermissionAfterDelete.rows,
    };

    const authorityUserPage = `${baseUrl}/auth/#/authority?status=user&id=${cleanup.userId}&name=${encodeURIComponent(userName)}`;
    navigations.push({
      route: `/auth/#/authority?status=user&id=${cleanup.userId}&name=${userName}`,
      status: (await openAndSettle(page, authorityUserPage))?.status() || null,
    });
    const userDataPermissionPayload = {
      controlled: true,
      dataResouceVOS: [
        {
          resourceCode: `${dataResourceCode}_USER`,
          resourceName: userDataResourceName,
          resourceType: dataResourceType,
        },
      ],
    };
    const userDataPermissionResult = await browserApi(
      page,
      "POST",
      `/inter-api/rbac/v1/user/${cleanup.userId}/data/resource/${encodeURIComponent(selectedDataResourceGroupCode)}`,
      userDataPermissionPayload
    );
    ensureApiOk(userDataPermissionResult, "User data resource permission save");
    await page.waitForTimeout(1000);
    userDataPermissionAfterCreate = queryUserDataPermission(cleanup.userId, selectedDataResourceGroupCode, 1);
    userDataPermissionCtrlAfterCreate = queryUserDataPermissionCtrl(cleanup.userId, selectedDataResourceGroupCode);
    const activeUserDataPermission = userDataPermissionAfterCreate.rows.find(
      (row) =>
        row.resourceCode === `${dataResourceCode}_USER` &&
        row.resourceName === userDataResourceName &&
        row.purviewType === "1" &&
        truthyDb(row.valid)
    );
    const activeUserDataCtrl = userDataPermissionCtrlAfterCreate.rows.find((row) => row.controlled === "1");
    if (!activeUserDataPermission || !activeUserDataCtrl) {
      throw new Error(
        `User data resource save did not persist expected rows: ${JSON.stringify({
          userRows: userDataPermissionAfterCreate.rows,
          userCtrlRows: userDataPermissionCtrlAfterCreate.rows,
        })}`
      );
    }
    operationLog.userDataResourceSave = {
      method: "POST",
      api: `/inter-api/rbac/v1/user/${cleanup.userId}/data/resource/${selectedDataResourceGroupCode}`,
      payload: userDataPermissionPayload,
      responseStatus: userDataPermissionResult.status,
      responseBody: userDataPermissionResult.json || userDataPermissionResult.text.slice(0, 1000),
      selectedGroup: {
        groupCode: selectedDataResourceGroupCode,
        groupName: selectedDataResourceGroupName,
      },
      verificationSql: [userDataPermissionAfterCreate.sql, userDataPermissionCtrlAfterCreate.sql].join("\n"),
      rows: {
        userDataPermission: userDataPermissionAfterCreate.rows,
        userDataPermissionCtrl: userDataPermissionCtrlAfterCreate.rows,
      },
    };

    const userDataPermissionDisablePayload = { controlled: false, dataResouceVOS: [] };
    const userDataPermissionDisableResult = await browserApi(
      page,
      "POST",
      `/inter-api/rbac/v1/user/${cleanup.userId}/data/resource/${encodeURIComponent(selectedDataResourceGroupCode)}`,
      userDataPermissionDisablePayload
    );
    ensureApiOk(userDataPermissionDisableResult, "User data resource permission disable");
    await page.waitForTimeout(1000);
    userDataPermissionAfterDisable = queryUserDataPermission(cleanup.userId, selectedDataResourceGroupCode, 1);
    userDataPermissionCtrlAfterDisable = queryUserDataPermissionCtrl(cleanup.userId, selectedDataResourceGroupCode);
    const activeUserDataAfterDisable = userDataPermissionAfterDisable.rows.find((row) => truthyDb(row.valid));
    const disabledUserDataCtrl = userDataPermissionCtrlAfterDisable.rows.find((row) => row.controlled === "0");
    if (activeUserDataAfterDisable || !disabledUserDataCtrl) {
      throw new Error(
        `User data resource disable did not clear active rows or set controlled=0: ${JSON.stringify({
          userRows: userDataPermissionAfterDisable.rows,
          userCtrlRows: userDataPermissionCtrlAfterDisable.rows,
        })}`
      );
    }
    operationLog.userDataResourceDisable = {
      method: "POST",
      api: `/inter-api/rbac/v1/user/${cleanup.userId}/data/resource/${selectedDataResourceGroupCode}`,
      payload: userDataPermissionDisablePayload,
      responseStatus: userDataPermissionDisableResult.status,
      responseBody: userDataPermissionDisableResult.json || userDataPermissionDisableResult.text.slice(0, 1000),
      verificationSql: [userDataPermissionAfterDisable.sql, userDataPermissionCtrlAfterDisable.sql].join("\n"),
      rows: {
        userDataPermission: userDataPermissionAfterDisable.rows,
        userDataPermissionCtrl: userDataPermissionCtrlAfterDisable.rows,
      },
    };

    const userPermissionLoadResult = await browserApi(
      page,
      "GET",
      `/inter-api/rbac/v1/userPermissions?menuId=${selectedMenu.id}&userId=${cleanup.userId}`
    );
    ensureApiOk(userPermissionLoadResult, "User permission load");
    userPermissionCandidate = selectPermissionCandidate(userPermissionLoadResult, "user");
    userPermissionBefore = queryUserPermission(cleanup.userId, userPermissionCandidate.menuOperateId);
    if (userPermissionBefore.rows.some((row) => row.userId === String(cleanup.userId))) {
      throw new Error(`User permission already exists before test: ${JSON.stringify(userPermissionBefore.rows)}`);
    }
    const userPermissionAddPayload = {
      list: [
        {
          userId: cleanup.userId,
          addList: [
            {
              ...userPermissionCandidate.payload,
              userId: cleanup.userId,
              purviewType: 1,
              menuOperateCode: userPermissionCandidate.menuOperateCode,
            },
          ],
          deleteList: [],
        },
      ],
    };
    const userPermissionAddResult = await browserApi(
      page,
      "POST",
      "/inter-api/rbac/v1/userPermission",
      userPermissionAddPayload
    );
    ensureApiOk(userPermissionAddResult, "User permission add");
    await page.waitForTimeout(1000);
    userPermissionAfterCreate = queryUserPermission(cleanup.userId, userPermissionCandidate.menuOperateId);
    const createdUserPermission = userPermissionAfterCreate.rows.find(
      (row) => row.userId === String(cleanup.userId) && row.menuOperateId === String(userPermissionCandidate.menuOperateId)
    );
    if (
      !createdUserPermission ||
      createdUserPermission.purviewType !== "1" ||
      !truthyDb(createdUserPermission.noRestrictFlag)
    ) {
      throw new Error(
        `User permission add did not persist expected purview_type=1 row: ${JSON.stringify(userPermissionAfterCreate.rows)}`
      );
    }
    cleanup.userPermissionId = Number(createdUserPermission.id);
    operationLog.userPermissionAdd = {
      method: "POST",
      api: "/inter-api/rbac/v1/userPermission",
      payload: userPermissionAddPayload,
      responseStatus: userPermissionAddResult.status,
      responseBody: userPermissionAddResult.json || userPermissionAddResult.text.slice(0, 1000),
      verificationSql: userPermissionAfterCreate.sql,
      rows: userPermissionAfterCreate.rows,
      selectedOperation: {
        menuCode,
        menuId: selectedMenu.id,
        menuOperateId: userPermissionCandidate.menuOperateId,
        menuOperateCode: userPermissionCandidate.menuOperateCode,
        menuOperateName: userPermissionCandidate.menuOperateName,
      },
    };

    const userPermissionDeletePayload = {
      list: [
        {
          userId: cleanup.userId,
          addList: [],
          deleteList: [
            {
              id: cleanup.userPermissionId,
              userId: cleanup.userId,
              menuOperateId: userPermissionCandidate.menuOperateId,
              menuOperateCode: userPermissionCandidate.menuOperateCode,
              purviewType: 1,
              noRestrictFlag: true,
            },
          ],
        },
      ],
    };
    const userPermissionDeleteResult = await browserApi(
      page,
      "POST",
      "/inter-api/rbac/v1/userPermission",
      userPermissionDeletePayload
    );
    ensureApiOk(userPermissionDeleteResult, "User permission delete");
    await page.waitForTimeout(1000);
    userPermissionAfterDelete = queryUserPermission(cleanup.userId, userPermissionCandidate.menuOperateId);
    if (userPermissionAfterDelete.rows.length) {
      throw new Error(`User permission delete left rows: ${JSON.stringify(userPermissionAfterDelete.rows)}`);
    }
    cleanup.userPermissionId = null;
    operationLog.userPermissionDelete = {
      method: "POST",
      api: "/inter-api/rbac/v1/userPermission",
      payload: userPermissionDeletePayload,
      responseStatus: userPermissionDeleteResult.status,
      responseBody: userPermissionDeleteResult.json || userPermissionDeleteResult.text.slice(0, 1000),
      verificationSql: userPermissionAfterDelete.sql,
      rows: userPermissionAfterDelete.rows,
    };

    const userDeletePayload = { ids: [cleanup.userId] };
    const userDeleteResult = await browserApi(page, "DELETE", "/inter-api/auth/v1/user", userDeletePayload);
    ensureApiOk(userDeleteResult, "Cleanup auth user delete");
    await page.waitForTimeout(900);
    authAfterDelete = queryAuthUserByUserName(userName);
    personAfterUserDelete = queryPersonByCode(personCode);
    const deletedUser = authAfterDelete.rows.find((row) => row.id === String(cleanup.userId) && row.valid === "0");
    const unboundPerson = personAfterUserDelete.rows.find((row) => row.id === String(cleanup.personId) && row.valid === "1");
    if (!deletedUser || !unboundPerson || unboundPerson.userId || unboundPerson.userName) {
      throw new Error(
        `Cleanup auth user delete did not soft-delete user and clear person binding: ${JSON.stringify({
          authRows: authAfterDelete.rows,
          personRows: personAfterUserDelete.rows,
        })}`
      );
    }
    cleanup.userId = null;
    operationLog.cleanupUserDelete = {
      method: "DELETE",
      api: "/inter-api/auth/v1/user",
      payload: userDeletePayload,
      responseStatus: userDeleteResult.status,
      responseBody: userDeleteResult.json || userDeleteResult.text.slice(0, 1000),
      verificationSql: [authAfterDelete.sql, personAfterUserDelete.sql].join("\n"),
      rows: { authUser: authAfterDelete.rows, person: personAfterUserDelete.rows },
    };

    const personDeleteResult = await browserApi(page, "DELETE", `/inter-api/organization/v1/person/${cleanup.personId}`);
    ensureApiOk(personDeleteResult, "Cleanup person delete");
    await page.waitForTimeout(700);
    personAfterCleanup = queryPersonByCode(personCode);
    const cleanedPerson = personAfterCleanup.rows.find((row) => row.id === String(cleanup.personId) && row.valid === "0");
    if (!cleanedPerson) {
      throw new Error(`Cleanup person delete did not soft-delete org_person: ${JSON.stringify(personAfterCleanup.rows)}`);
    }
    cleanup.personId = null;
    operationLog.cleanupPersonDelete = {
      method: "DELETE",
      api: `/inter-api/organization/v1/person/${cleanedPerson.id}`,
      payload: null,
      responseStatus: personDeleteResult.status,
      responseBody: personDeleteResult.json || personDeleteResult.text.slice(0, 1000),
      verificationSql: personAfterCleanup.sql,
      rows: personAfterCleanup.rows,
    };

    const roleDeleteResult = await browserApi(page, "DELETE", `/inter-api/rbac/v1/role/${roleCode}`);
    ensureApiOk(roleDeleteResult, "Cleanup role delete");
    await page.waitForTimeout(900);
    roleAfterDelete = queryRoleByCode(roleCode);
    const activeRole = roleAfterDelete.rows.find((row) => truthyDb(row.valid));
    if (activeRole) {
      throw new Error(`Cleanup role delete left active role: ${JSON.stringify(roleAfterDelete.rows)}`);
    }
    cleanup.roleId = null;
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
  } finally {
    const cleanupErrors = [];
    try {
      if (selectedDataResourceGroupCode && cleanup.roleId) {
        const payload = { controlled: false, dataResouceVOS: [] };
        await browserApi(
          page,
          "POST",
          `/inter-api/rbac/v1/role/${cleanup.roleId}/data/resource/${encodeURIComponent(selectedDataResourceGroupCode)}`,
          payload
        ).catch((error) => cleanupErrors.push(`cleanup roleDataResource: ${error.message}`));
      }
      if (selectedDataResourceGroupCode && cleanup.userId) {
        const payload = { controlled: false, dataResouceVOS: [] };
        await browserApi(
          page,
          "POST",
          `/inter-api/rbac/v1/user/${cleanup.userId}/data/resource/${encodeURIComponent(selectedDataResourceGroupCode)}`,
          payload
        ).catch((error) => cleanupErrors.push(`cleanup userDataResource: ${error.message}`));
      }
      if (cleanup.userPermissionId && cleanup.userId && userPermissionCandidate) {
        const payload = {
          list: [
            {
              userId: cleanup.userId,
              addList: [],
              deleteList: [
                {
                  id: cleanup.userPermissionId,
                  userId: cleanup.userId,
                  menuOperateId: userPermissionCandidate.menuOperateId,
                  purviewType: 1,
                },
              ],
            },
          ],
        };
        await browserApi(page, "POST", "/inter-api/rbac/v1/userPermission", payload).catch((error) =>
          cleanupErrors.push(`cleanup userPermission: ${error.message}`)
        );
      }
      if (cleanup.rolePermissionId && cleanup.roleId && rolePermissionCandidate) {
        const payload = {
          list: [
            {
              roleId: cleanup.roleId,
              addList: [],
              deleteList: [
                {
                  id: cleanup.rolePermissionId,
                  roleId: cleanup.roleId,
                  menuOperateId: rolePermissionCandidate.menuOperateId,
                },
              ],
            },
          ],
        };
        await browserApi(page, "POST", "/inter-api/rbac/v1/rolePermission", payload).catch((error) =>
          cleanupErrors.push(`cleanup rolePermission: ${error.message}`)
        );
      }
      if (cleanup.roleUserId) {
        await browserApi(page, "DELETE", `/inter-api/rbac/v1/roleUser/${cleanup.roleUserId}`).catch((error) =>
          cleanupErrors.push(`cleanup roleUser: ${error.message}`)
        );
      }
      if (cleanup.userId) {
        await browserApi(page, "DELETE", "/inter-api/auth/v1/user", { ids: [cleanup.userId] }).catch((error) =>
          cleanupErrors.push(`cleanup user: ${error.message}`)
        );
      }
      if (cleanup.personId) {
        await browserApi(page, "DELETE", `/inter-api/organization/v1/person/${cleanup.personId}`).catch((error) =>
          cleanupErrors.push(`cleanup person: ${error.message}`)
        );
      }
      if (cleanup.roleId) {
        await browserApi(page, "DELETE", `/inter-api/rbac/v1/role/${roleCode}`).catch((error) =>
          cleanupErrors.push(`cleanup role: ${error.message}`)
        );
      }
    } finally {
      if (cleanupErrors.length) {
        operationLog.cleanupErrors = cleanupErrors;
      }
    }
  }

  const bodyText = await page.locator("body").innerText({ timeout: 5000 }).catch(() => "");
  const visibleError = String(bodyText)
    .split(/\r?\n/)
    .map((line) => line.trim())
    .find((line) => line && visibleErrorPattern.test(line));
  const screenshotPath = path.join(outputDir, "rbac-permission-persistence.png");
  await page.screenshot({ path: screenshotPath, fullPage: true }).catch(() => {});
  await browser.close();
  const ignoredNonApiHttpErrors = nonApiHttpErrors.filter(isIgnorableStatic404);
  const blockingNonApiHttpErrors = nonApiHttpErrors.filter((item) => !isIgnorableStatic404(item));
  const blockingConsoleErrors = consoleErrors.filter(
    (item) => !isIgnorableConsoleError(item, ignoredNonApiHttpErrors)
  );

  const report = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    username,
    marker,
    routes: ["/auth/#/role", "/auth/#/authority?status=role", "/auth/#/authority?status=user"],
    menuCode,
    dataResourceCode,
    roleCode,
    userName,
    personCode,
    userPasswordMode: "generated and redacted; only prerequisite user creation is tested here",
    dependencies: {
      mainPositionId,
      positionPrecheckSql: dependencyPrecheck.sql,
      positionPrecheckRows: dependencyPrecheck.rows,
      menuPrecheckSql: menuPrecheck.sql,
      menuPrecheckRows: menuPrecheck.rows,
      dataResourceGroupPrecheckSql: dataResourceGroupPrecheck.sql,
      dataResourceGroupPrecheckRows: dataResourceGroupPrecheck.rows,
    },
    backendTrace: {
      roleCreate:
        "RoleInterApiController.save -> RoleServiceImpl.saveRole -> RoleMapper.insert -> public.rbac_role -> RoleMneCodeServiceImpl.createRoleMneCode",
      roleUpdate:
        "RoleInterApiController.update -> RoleServiceImpl.update -> RoleMapper.updateById -> public.rbac_role",
      roleDelete:
        "RoleInterApiController.delete -> RoleServiceImpl.deleteRoles -> RoleMapper.deleteBatchIds -> public.rbac_role.valid=false/0 or physical delete -> rbac_role_mnecode cleanup",
      roleUserCreate:
        "RoleUserInterApiController.save -> RoleUserServiceImpl.saveRoleUsers -> RoleUserMapper.insert -> public.rbac_roleuser -> IUserAdapter.bindRole",
      roleUserDelete:
        "RoleUserInterApiController.delete -> RoleUserServiceImpl.deleteRoleUsers -> RoleUserMapper.deleteBatchIds or update from_position -> public.rbac_roleuser -> IUserAdapter.bindRole(false)",
      rolePermissionAdd:
        "RolePermissionController.addRolePermission -> RolePermissionServiceImpl.addOrUpdateRolePermission -> MyBatis-Plus saveOrUpdateBatch -> public.rbac_rolepermission -> role position/staff/department refs if selected",
      rolePermissionDelete:
        "RolePermissionController.addRolePermission(deleteList) -> RolePermissionServiceImpl.batchDeleteRolePermissions -> public.rbac_rolepermission and rbac_rolep* refs",
      userPermissionAdd:
        "UserPermissionController.addUserPermission -> UserPermissionServiceImpl.addOrUpdateUserPermission -> MyBatis-Plus saveOrUpdateBatch -> public.rbac_userpermission -> user position/staff/department refs if selected",
      userPermissionDelete:
        "UserPermissionController.addUserPermission(deleteList) -> UserPermissionServiceImpl.batchDeleteUserPermissions -> public.rbac_userpermission and rbac_userp* refs",
      dataResourceGroupLoad:
        "DataPermissionController.queryDataResourceGroups -> DataPermissionServiceImpl.getDataResourceGroups -> RbacDataResourceGroupServiceImpl.list -> public.rbac_data_resource_group",
      roleDataResourceSave:
        "DataPermissionController.saveDataResourceForRole -> DataPermissionServiceImpl.saveDataResourceForRole -> RbacRoleDataPermissionCtrlServiceImpl.saveOrUpdate/public.rbac_role_data_permission_ctrl -> RbacRoleDataPermissionServiceImpl.saveBatch/public.rbac_role_data_permission -> addUserPermissionByRole/public.rbac_user_data_permission purview_type=0 for bound users",
      roleDataResourceDisable:
        "DataPermissionController.saveDataResourceForRole(controlled=false) -> DataPermissionServiceImpl.saveDataResourceForRole -> public.rbac_role_data_permission_ctrl.controlled=0 -> remove role direct rows and inherited public.rbac_user_data_permission purview_type=0 rows",
      userDataResourceSave:
        "DataPermissionController.saveDataResourceForUser -> DataPermissionServiceImpl.saveDataResourceForUser -> RbacUserDataPermissionCtrlServiceImpl.saveOrUpdate/public.rbac_user_data_permission_ctrl -> RbacUserDataPermissionServiceImpl.saveBatch/public.rbac_user_data_permission purview_type=1",
      userDataResourceDisable:
        "DataPermissionController.saveDataResourceForUser(controlled=false) -> DataPermissionServiceImpl.saveDataResourceForUser -> public.rbac_user_data_permission_ctrl.controlled=0 -> remove direct public.rbac_user_data_permission purview_type=1 rows",
    },
    browser: {
      navigations,
      consoleErrors: compactErrors(consoleErrors),
      consoleWarnings: compactErrors(consoleWarnings),
      nonApiHttpErrors: compactErrors(nonApiHttpErrors),
      ignoredNonApiHttpErrors: compactErrors(ignoredNonApiHttpErrors),
      blockingConsoleErrors: compactErrors(blockingConsoleErrors),
      blockingNonApiHttpErrors: compactErrors(blockingNonApiHttpErrors),
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
      blockingConsoleErrors.length === 0 &&
      blockingNonApiHttpErrors.length === 0 &&
      pageErrors.length === 0 &&
      requestFailures.length === 0 &&
      Boolean(operationLog.roleCreate) &&
      Boolean(operationLog.roleUpdate) &&
      Boolean(operationLog.roleUserCreate) &&
      Boolean(operationLog.roleUserDelete) &&
      Boolean(operationLog.roleDataResourceSave) &&
      Boolean(operationLog.roleDataResourceDisable) &&
      Boolean(operationLog.rolePermissionAdd) &&
      Boolean(operationLog.rolePermissionDelete) &&
      Boolean(operationLog.userDataResourceSave) &&
      Boolean(operationLog.userDataResourceDisable) &&
      Boolean(operationLog.userPermissionAdd) &&
      Boolean(operationLog.userPermissionDelete) &&
      Boolean(operationLog.roleDelete),
  };

  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);
  console.log(`${report.ok ? "OK" : "FAIL"} rbac-permission-persistence marker=${marker}`);
  console.log(`REPORT ${outputPath}`);
  if (!report.ok) {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  ensureDir(path.dirname(outputPath));
  fs.writeFileSync(
    outputPath,
    `${JSON.stringify(
      {
        generatedAt: new Date().toISOString(),
        baseUrl,
        username,
        marker,
        ok: false,
        error: error && error.stack ? error.stack : String(error),
      },
      null,
      2
    )}\n`
  );
  console.error(error && error.stack ? error.stack : error);
  process.exit(1);
});
