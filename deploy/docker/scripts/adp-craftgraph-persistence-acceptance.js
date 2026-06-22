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
const productCode = process.env.ADP_CRAFTGRAPH_PRODUCT_CODE || "ADP_E2E_20260618200829_WOM_CHECKOUTBILL_MAT";
const nowToken = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const marker = process.env.ADP_E2E_MARKER || `ADP_E2E_${nowToken}_CRAFTGRAPH`;
const outputDir =
  process.env.ADP_OUTPUT_DIR || path.join("/tmp", `adp-craftgraph-persistence-${nowToken}`);
const outputPath =
  process.env.ADP_CRAFTGRAPH_PERSISTENCE_OUTPUT ||
  path.join(outputDir, "craftgraph-persistence-results.json");

const route = "/msService/craftGraph/basicInfo/basicInfo/basicInfoList";
const saveApi = "/msService/craftGraph/basicInfo/basicInfo/basicInfoEdit/submit";

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

function productSql() {
  return `
SELECT id, code, name, coalesce(valid::text, ''), coalesce(status::text, '')
FROM public.baseset_materials
WHERE code = ${sqlLiteral(productCode)}
ORDER BY id DESC
LIMIT 1;
`;
}

function craftSql() {
  return `
SELECT id, craft_code, craft_name, version_info, coalesce(remark, ''),
       coalesce(product::text, ''), coalesce(valid::text, ''),
       coalesce(status::text, ''), coalesce(version::text, '')
FROM public.craft_basic_infos
WHERE craft_code = ${sqlLiteral(marker)}
ORDER BY id DESC;
`;
}

function treeSql(craftId) {
  return `
SELECT tree_node_id, coalesce(valid::text, ''), coalesce(status::text, ''), coalesce(version::text, '')
FROM public.craft_tree_structures
WHERE tree_node_id::text = ${sqlLiteral(craftId)}
ORDER BY id DESC;
`;
}

function buildCreatePayload(product) {
  return {
    viewCode: "craftGraph_1.0_basicInfo_basicInfoEdit",
    modelName: "craftGraph_1.0_basicInfo_basicInfo",
    operateType: "save",
    basicInfo: {
      version: 0,
      valid: true,
      status: 99,
      cid: 1000,
      craftCode: marker,
      craftName: `${marker}_NAME`,
      versionInfo: "V1",
      remark: `create ${marker}`,
      product: {
        id: product.id,
        code: product.code,
        name: product.name,
      },
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

async function setupContext(ticket) {
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
    ["suposTicket", "SUPOS_TICKET", "token", "ticket"].forEach((key) => {
      window.localStorage.setItem(key, token);
      window.sessionStorage.setItem(key, token);
    });
  }, ticket);
  return { browser, context };
}

async function runBrowser(ticket, product, evidence) {
  const { browser, context } = await setupContext(ticket);
  try {
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
      if (/craftGraph|layoutJson|getDataGirdCookie|basicInfoEdit/.test(url)) {
        evidence.frontend.requests.push({
          method: requestItem.method(),
          url,
          postData: requestItem.postData(),
        });
      }
    });
    page.on("response", async (response) => {
      const url = response.url();
      if (!/craftGraph|layoutJson|getDataGirdCookie|basicInfoEdit/.test(url)) {
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

    const pageResponse = await page.goto(route, { waitUntil: "domcontentloaded", timeout: 60000 });
    await page.waitForTimeout(5000);
    const bodyText = await page.locator("body").innerText({ timeout: 10000 });
    evidence.frontend.visibleButtons = ["新增", "修改", "删除"].filter((text) => bodyText.includes(text));
    if (!evidence.frontend.visibleButtons.includes("修改")) {
      throw new Error(`craftGraph list modify button is not visible. Body: ${bodyText.slice(0, 1000)}`);
    }

    const listBefore = path.join(outputDir, "craftgraph-list-before-create.png");
    await page.screenshot({ path: listBefore, fullPage: true });
    evidence.frontend.route = {
      route,
      status: pageResponse ? pageResponse.status() : null,
      title: await page.title().catch(() => ""),
      screenshot: listBefore,
    };

    evidence.operations.create.payload = buildCreatePayload(product);
    evidence.operations.create.response = await browserFetch(page, "POST", saveApi, evidence.operations.create.payload);
    if (evidence.operations.create.response.status !== 200) {
      throw new Error(`Create HTTP failed: ${JSON.stringify(evidence.operations.create.response)}`);
    }
    if (
      evidence.operations.create.response.json &&
      evidence.operations.create.response.json.code >= 400
    ) {
      throw new Error(`Create business response failed: ${JSON.stringify(evidence.operations.create.response.json)}`);
    }

    const createdRaw = runSql(craftSql());
    const createdRows = parseRows(createdRaw);
    evidence.database.afterCreate = {
      verificationSql: craftSql().trim(),
      raw: createdRaw,
      rows: createdRows,
    };
    if (createdRows.length !== 1) {
      throw new Error(`Expected one craft row after create, got ${createdRows.length}: ${createdRaw}`);
    }
    const [craftId, craftCode, craftName, versionInfo, remark, productId, valid, status, version] = createdRows[0];
    evidence.database.createdCraft = {
      craftId,
      craftCode,
      craftName,
      versionInfo,
      remark,
      productId,
      valid,
      status,
      version,
    };
    if (!["t", "true"].includes(String(valid).toLowerCase())) {
      throw new Error(`Created craft row is not valid=true: ${createdRaw}`);
    }

    await page.goto(route, { waitUntil: "domcontentloaded", timeout: 60000 });
    await page.waitForTimeout(5000);
    const markerCell = page.getByText(marker, { exact: true }).first();
    await markerCell.waitFor({ state: "visible", timeout: 30000 });
    await markerCell.click();
    const selectedScreenshot = path.join(outputDir, "craftgraph-row-selected-before-edit.png");
    await page.screenshot({ path: selectedScreenshot, fullPage: true });
    evidence.frontend.selectedScreenshot = selectedScreenshot;

    const editResponsePromise = page.waitForResponse(
      (response) => response.url().includes("basicInfoEdit") && response.request().method() === "GET",
      { timeout: 30000 }
    );
    await page.locator("#btn-edit").click();
    const editResponse = await editResponsePromise;
    evidence.operations.openEdit = {
      method: "GET",
      api: editResponse.url(),
      status: editResponse.status(),
    };

    const editFrame = page.frameLocator('iframe[src*="basicInfoEdit"]');
    await editFrame.locator("#edit_form_basicInfo\\.craftName").waitFor({ state: "visible", timeout: 30000 });
    const editScreenshot = path.join(outputDir, "craftgraph-edit-before-save.png");
    await page.screenshot({ path: editScreenshot, fullPage: true });
    evidence.frontend.editScreenshot = editScreenshot;

    const updatedName = `${marker}_UPDATED_NAME`;
    const updatedVersion = "V2";
    const updatedRemark = `updated ${marker}`;
    await editFrame.locator("#edit_form_basicInfo\\.craftName").fill(updatedName);
    await editFrame.locator("#edit_form_basicInfo\\.versionInfo").fill(updatedVersion);
    await editFrame.locator("textarea").first().fill(updatedRemark);

    const saveResponsePromise = page.waitForResponse(
      (response) => response.url().includes(saveApi) && response.request().method() === "POST",
      { timeout: 30000 }
    );
    await page.locator(".ant-modal button.ant-btn-primary").filter({ hasText: "保存" }).last().click();
    const saveResponse = await saveResponsePromise;
    const saveParsed = await readJsonSafe(saveResponse);
    evidence.operations.update = {
      method: "POST",
      api: saveApi,
      payload: {
        craftCode: marker,
        craftName: updatedName,
        versionInfo: updatedVersion,
        remark: updatedRemark,
      },
      responseStatus: saveResponse.status(),
      responseBody: saveParsed.text.slice(0, 4000),
      responseJson: saveParsed.json,
    };
    if (saveResponse.status() !== 200) {
      throw new Error(`Update HTTP failed: ${saveResponse.status()} ${saveParsed.text.slice(0, 1000)}`);
    }
    if (saveParsed.json && saveParsed.json.code >= 400) {
      throw new Error(`Update business response failed: ${JSON.stringify(saveParsed.json)}`);
    }
    await page.waitForTimeout(2500);
    const afterUpdateScreenshot = path.join(outputDir, "craftgraph-after-update.png");
    await page.screenshot({ path: afterUpdateScreenshot, fullPage: true });
    evidence.frontend.afterUpdateScreenshot = afterUpdateScreenshot;

    const updatedRaw = runSql(craftSql());
    const updatedRows = parseRows(updatedRaw);
    evidence.database.afterUpdate = {
      verificationSql: craftSql().trim(),
      raw: updatedRaw,
      rows: updatedRows,
    };
    if (updatedRows.length !== 1) {
      throw new Error(`Expected one craft row after update, got ${updatedRows.length}: ${updatedRaw}`);
    }
    const updatedRow = updatedRows[0];
    evidence.database.updatedCraft = {
      craftId: updatedRow[0],
      craftCode: updatedRow[1],
      craftName: updatedRow[2],
      versionInfo: updatedRow[3],
      remark: updatedRow[4],
      productId: updatedRow[5],
      valid: updatedRow[6],
      status: updatedRow[7],
      version: updatedRow[8],
    };
    if (updatedRow[2] !== updatedName || updatedRow[3] !== updatedVersion || updatedRow[4] !== updatedRemark) {
      throw new Error(`Update did not persist expected values: ${updatedRaw}`);
    }

    const treeRaw = runSql(treeSql(craftId));
    evidence.database.treeStructure = {
      verificationSql: treeSql(craftId).trim(),
      raw: treeRaw,
      rows: parseRows(treeRaw),
    };
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
      create: { method: "POST", api: saveApi },
      openEdit: { method: "GET" },
      update: { method: "POST", api: saveApi },
    },
    database: {
      productSql: productSql().trim(),
    },
    status: "RUNNING",
    issues: [],
  };

  try {
    const productRaw = runSql(productSql());
    const productRows = parseRows(productRaw);
    evidence.database.productRaw = productRaw;
    if (productRows.length !== 1) {
      throw new Error(`Expected one craftGraph product prerequisite, got ${productRows.length}: ${productRaw}`);
    }
    const [id, code, name, valid, status] = productRows[0];
    const product = { id, code, name, valid, status };
    evidence.database.product = product;

    const api = await request.newContext({ ignoreHTTPSErrors: true });
    const loginResult = await login(api);
    await api.dispose();
    evidence.login = { status: loginResult.status, ticket: Boolean(loginResult.ticket) };
    await runBrowser(loginResult.ticket, product, evidence);
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
