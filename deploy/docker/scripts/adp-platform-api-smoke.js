#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { request } = require("playwright");

const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const outputPath =
  process.env.ADP_API_SMOKE_OUTPUT ||
  path.join("/tmp", `adp-platform-api-smoke-${new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14)}.json`);

const bodyErrorPattern =
  /(系统错误|系统异常|发生未知异常|Incomplete parameter|非法参数|Bad Request|404_NOT_FOUND|500 INTERNAL|Caused by:|\b[\w.]+Exception(?::|\s+at\b)|Invalid bound statement|relation .* does not exist)/i;

const checks = [
  {
    name: "menus-currentUser",
    method: "GET",
    path: "/inter-api/rbac/v1/menus/currentUser",
    expect: (payload) => countMenuTargets(payload) >= 30,
    describe: (payload) => `targets=${countMenuTargets(payload)}`,
  },
  {
    name: "rbac-flow-permissions",
    method: "POST",
    path: "/inter-api/rbac/v1/user/flow/userPermissions?",
    data: {},
    expect: (payload) => countList(payload) >= 1,
    describe: (payload) => `total=${countList(payload)}`,
  },
  {
    name: "signatureLog-empty",
    method: "POST",
    path: "/inter-api/signature/signatureLogListQuery",
    data: { current: 1, pageSize: 10 },
  },
  {
    name: "signatureLog-like-array",
    method: "POST",
    path: "/inter-api/signature/signatureLogListQuery",
    data: { current: 1, pageSize: 10, moduleName: ["smoke"] },
  },
  {
    name: "i18n-allkeyvalues-default-lang",
    method: "GET",
    path: "/inter-api/i18n/v1/resource/code/all/module_ids/allkeyvalues?moduleCodes=Supfusion%2Crbac",
  },
  {
    name: "i18n-forFront-default-lang",
    method: "GET",
    path: "/inter-api/i18n/v1/resource/code/all/module_ids/forFrontNoGateWay?moduleCodes=Supfusion%2CsupplantFrontend",
  },
  {
    name: "ip-black-white",
    method: "GET",
    path: "/inter-api/auth/v1/ip-black-white/list?ip=&current=1&pageSize=50",
  },
  {
    name: "identityCenterConfig",
    method: "GET",
    path: "/inter-api/auth/v1/queryIdentityCenterConfig",
  },
  {
    name: "pending-listByGroup",
    method: "GET",
    path: "/msService/baseService/pending/proxyPending/listByGroup?pageSize=20&pageNo=1&isAdmin=true",
  },
  {
    name: "printer-apps",
    method: "GET",
    path: "/inter-api/printer/v1/apps?source=2",
  },
  {
    name: "customPropertyTree",
    method: "GET",
    path: "/inter-api/customProperty/tree?type=module",
  },
  {
    name: "serviceManagerApps",
    method: "GET",
    path: "/msService/servicemanager/msModule/supos/app/list",
  },
  {
    name: "findMyProcesses",
    method: "GET",
    path: "/msService/baseService/pending/grup/findMyProcesses",
  },
  {
    name: "ec-engine-msManage",
    method: "GET",
    path: "/msService/ec/engine/msManage",
  },
  {
    name: "ec-module-msManage",
    method: "GET",
    path: "/msService/ec/module/msManage",
  },
  {
    name: "ec-engine-datalist",
    method: "GET",
    path: "/msService/ec/engine/datalist",
  },
];

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

function pickArray(...values) {
  return values.find((value) => Array.isArray(value));
}

function countList(payload) {
  const rows = pickArray(
    payload && payload.list,
    payload && payload.data,
    payload && payload.data && payload.data.list,
    payload && payload.data && payload.data.records,
    payload && payload.result,
    payload && payload.result && payload.result.list,
    payload && payload.result && payload.result.records
  );
  return rows ? rows.length : 0;
}

function pickMenuRoots(payload) {
  return (
    pickArray(
      payload,
      payload && payload.list,
      payload && payload.data,
      payload && payload.data && payload.data.list,
      payload && payload.data && payload.data.menus,
      payload && payload.result,
      payload && payload.result && payload.result.list,
      payload && payload.result && payload.result.menus
    ) || []
  );
}

function countMenuTargets(payload) {
  let count = 0;

  function visit(node) {
    if (!node || typeof node !== "object") {
      return;
    }
    const url = node.url || node.path || node.href || node.menuUrl || node.targetUrl || node.routeUrl;
    if (typeof url === "string" && url.trim() && !/^javascript:/i.test(url.trim())) {
      count += 1;
    }
    const children =
      pickArray(node.children, node.childrens, node.childMenus, node.subMenus, node.nodes, node.menuList) || [];
    children.forEach(visit);
  }

  pickMenuRoots(payload).forEach(visit);
  return count;
}

function responseHasError(text) {
  return bodyErrorPattern.test(String(text || ""));
}

async function runCheck(api, ticket, check) {
  const response = await api.fetch(`${baseUrl}${check.path}`, {
    method: check.method,
    data: check.data,
    headers: {
      Accept: "application/json, text/plain, */*",
      Authorization: `Bearer ${ticket}`,
      Cookie: `suposTicket=${encodeURIComponent(ticket)}`,
      langu_code: "zh_CN",
    },
  });
  const parsed = await readJsonSafe(response);
  const contentType = response.headers()["content-type"] || "";
  const shouldScanBody = !/text\/html/i.test(contentType);
  const statusOk = response.status() < 400;
  const bodyOk = !shouldScanBody || !responseHasError(parsed.text);
  const customOk = check.expect ? check.expect(parsed.json, parsed.text) : true;
  const ok = statusOk && bodyOk && customOk;

  return {
    name: check.name,
    method: check.method,
    path: check.path,
    ok,
    status: response.status(),
    contentType,
    detail: check.describe && parsed.json ? check.describe(parsed.json) : null,
    bodySnippet: ok ? undefined : parsed.text.slice(0, 1000),
  };
}

async function main() {
  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const ticket = await login(api);

  const results = [];
  for (const check of checks) {
    const result = await runCheck(api, ticket, check);
    results.push(result);
    const detail = result.detail ? ` ${result.detail}` : "";
    console.log(`${result.ok ? "OK" : "FAIL"} ${result.name} status=${result.status}${detail}`);
  }

  await api.dispose();

  const failed = results.filter((result) => !result.ok);
  const report = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    username,
    total: results.length,
    passed: results.length - failed.length,
    failed: failed.length,
    results,
  };
  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);

  console.log(`SUMMARY total=${report.total} passed=${report.passed} failed=${report.failed}`);
  console.log(`REPORT ${outputPath}`);

  if (failed.length) {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error);
  process.exit(1);
});
