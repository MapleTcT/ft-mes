#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { request } = require("playwright");

const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const roleCode = process.env.ADP_RBAC_AUTHORITY_ROLE_CODE || "systemRole";
const userId = process.env.ADP_RBAC_AUTHORITY_USER_ID || "1";
const menuCodes = (process.env.ADP_RBAC_AUTHORITY_MENU_CODES || "organizationmanage,personmanage,role")
  .split(",")
  .map((item) => item.trim())
  .filter(Boolean);
const outputPath =
  process.env.ADP_RBAC_AUTHORITY_SMOKE_OUTPUT ||
  path.join("/tmp", `adp-rbac-authority-smoke-${new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14)}.json`);

const errorPattern =
  /(数据库操作异常|系统错误|系统异常|SQLGrammarException|BadSqlGrammarException|could not extract ResultSet|column .* does not exist|relation .* does not exist|500 INTERNAL|\b[\w.]+Exception(?::|\s+at\b))/i;

function ensureDir(filePath) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
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

function authHeaders(ticket) {
  return {
    Accept: "application/json, text/plain, */*",
    Authorization: `Bearer ${ticket}`,
    Cookie: `suposTicket=${encodeURIComponent(ticket)}`,
    langu_code: "zh_CN",
  };
}

function pickArray(...values) {
  return values.find((value) => Array.isArray(value));
}

function getChildren(node) {
  return (
    pickArray(
      node && node.children,
      node && node.childrens,
      node && node.childMenus,
      node && node.subMenus,
      node && node.nodes,
      node && node.menuList
    ) || []
  );
}

function pickRoots(payload) {
  return (
    pickArray(
      payload,
      payload && payload.list,
      payload && payload.data,
      payload && payload.data && payload.data.list,
      payload && payload.data && payload.data.menus,
      payload && payload.result,
      payload && payload.result && payload.result.list
    ) || []
  );
}

function flattenMenus(payload) {
  const rows = [];
  function visit(node, parents) {
    if (!node || typeof node !== "object") {
      return;
    }
    const label = node.menuName || node.name || node.title || node.label || node.code || "";
    const nextParents = parents.concat(label).filter(Boolean);
    rows.push({
      id: node.id || node.menuInfoId,
      code: node.code || node.menuInfoCode || node.menuCode || "",
      label: nextParents.join(" > "),
    });
    getChildren(node).forEach((child) => visit(child, nextParents));
  }
  pickRoots(payload).forEach((root) => visit(root, []));
  return rows;
}

function getData(payload) {
  return (payload && payload.data) || payload || {};
}

function countPermissionGroups(payload) {
  const data = getData(payload);
  const assign = pickArray(data.assign, data.data && data.data.assign) || [];
  const unassign = pickArray(data.unassign, data.data && data.data.unassign) || [];
  const list = pickArray(data.list, data.data && data.data.list) || [];
  return { assign: assign.length, unassign: unassign.length, list: list.length };
}

async function getJson(api, ticket, pathName) {
  const response = await api.get(`${baseUrl}${pathName}`, { headers: authHeaders(ticket) });
  const parsed = await readJsonSafe(response);
  return {
    path: pathName,
    status: response.status(),
    ok: response.status() < 400 && !errorPattern.test(parsed.text || ""),
    json: parsed.json,
    bodySnippet: parsed.text.slice(0, 1000),
  };
}

async function findRole(api, ticket) {
  const result = await getJson(api, ticket, `/inter-api/rbac/v1/role/findOne?code=${encodeURIComponent(roleCode)}`);
  if (!result.ok) {
    throw new Error(`Role lookup failed: ${result.status} ${result.bodySnippet}`);
  }
  const data = getData(result.json);
  const id = data.id || (data.data && data.data.id);
  if (!id) {
    throw new Error(`Role ${roleCode} has no id in response: ${result.bodySnippet}`);
  }
  return { id, code: roleCode, raw: data };
}

async function run() {
  ensureDir(outputPath);
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const startedAt = new Date().toISOString();
  const results = [];
  const report = { baseUrl, username, roleCode, userId, menuCodes, startedAt, results };

  try {
    const ticket = await login(api);
    const role = await findRole(api, ticket);
    report.roleId = role.id;

    const menuTree = await getJson(api, ticket, "/inter-api/rbac/v1/menus/ref?restrict=false");
    results.push({
      name: "menus-ref",
      path: menuTree.path,
      status: menuTree.status,
      ok: menuTree.ok,
      bodySnippet: menuTree.ok ? undefined : menuTree.bodySnippet,
    });
    if (!menuTree.ok) {
      throw new Error(`Menu tree failed: ${menuTree.status} ${menuTree.bodySnippet}`);
    }

    const menus = flattenMenus(menuTree.json);
    report.menuCount = menus.length;
    const selectedMenus = menuCodes
      .map((code) => menus.find((menu) => menu.code === code))
      .filter(Boolean);
    report.selectedMenus = selectedMenus;
    if (selectedMenus.length === 0) {
      throw new Error(`No requested menu codes found. Requested=${menuCodes.join(",")} menuCount=${menus.length}`);
    }

    const assignedRole = await getJson(api, ticket, `/inter-api/rbac/v1/rolePermissions/assigned?roleId=${role.id}`);
    results.push({
      name: "rolePermissions-assigned",
      path: assignedRole.path,
      status: assignedRole.status,
      ok: assignedRole.ok,
      counts: countPermissionGroups(assignedRole.json),
      bodySnippet: assignedRole.ok ? undefined : assignedRole.bodySnippet,
    });

    const assignedUser = await getJson(api, ticket, `/inter-api/rbac/v1/userPermissions/assigned?userId=${userId}`);
    results.push({
      name: "userPermissions-assigned",
      path: assignedUser.path,
      status: assignedUser.status,
      ok: assignedUser.ok,
      counts: countPermissionGroups(assignedUser.json),
      bodySnippet: assignedUser.ok ? undefined : assignedUser.bodySnippet,
    });

    for (const menu of selectedMenus) {
      const roleResult = await getJson(
        api,
        ticket,
        `/inter-api/rbac/v1/rolePermissions?menuId=${encodeURIComponent(menu.id)}&roleId=${encodeURIComponent(role.id)}`
      );
      results.push({
        name: `rolePermissions-${menu.code}`,
        menu,
        path: roleResult.path,
        status: roleResult.status,
        ok: roleResult.ok,
        counts: countPermissionGroups(roleResult.json),
        bodySnippet: roleResult.ok ? undefined : roleResult.bodySnippet,
      });

      const userResult = await getJson(
        api,
        ticket,
        `/inter-api/rbac/v1/userPermissions?menuId=${encodeURIComponent(menu.id)}&userId=${encodeURIComponent(userId)}`
      );
      results.push({
        name: `userPermissions-${menu.code}`,
        menu,
        path: userResult.path,
        status: userResult.status,
        ok: userResult.ok,
        counts: countPermissionGroups(userResult.json),
        bodySnippet: userResult.ok ? undefined : userResult.bodySnippet,
      });
    }
  } finally {
    await api.dispose();
  }

  report.finishedAt = new Date().toISOString();
  report.total = results.length;
  report.failed = results.filter((result) => !result.ok).length;
  report.passed = report.total - report.failed;
  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);

  if (report.failed > 0) {
    console.error(`RBAC authority smoke failed: ${report.failed}/${report.total}. Report: ${outputPath}`);
    process.exit(1);
  }
  console.log(`RBAC authority smoke passed: ${report.passed}/${report.total}. Report: ${outputPath}`);
}

run().catch((error) => {
  ensureDir(outputPath);
  fs.writeFileSync(
    outputPath,
    `${JSON.stringify(
      {
        baseUrl,
        username,
        roleCode,
        userId,
        menuCodes,
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
