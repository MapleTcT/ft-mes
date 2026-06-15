#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { chromium, request } = require("playwright");

const timestamp = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const headless = process.env.ADP_HEADLESS !== "false";
const outputDir =
  process.env.ADP_PRODUCTION_DISCOVERY_OUTPUT ||
  path.join("/tmp", `adp-production-action-discovery-${timestamp}`);
const clickCreate = process.env.ADP_PRODUCTION_DISCOVERY_CLICK_CREATE !== "false";

const defaultTargets = [
  {
    id: "PROD-002",
    label: "WOM make task create discovery",
    url: "/msService/WOM/produceTask/produceTask/makeTaskList",
  },
];

const visibleErrorPattern =
  /(系统错误|系统异常|发生未知异常|SQLGrammarException|could not extract ResultSet|404_NOT_FOUND|500 INTERNAL|\bHTTP\s*(404|500)\b|\b(404|500)\s+(Not Found|Internal Server Error)\b|Caused by:|\b[\w.]+Exception(?::|\s+at\b)|Invalid bound statement|relation .* does not exist|column .* does not exist)/i;

const createTextPattern = /(新增|新建|添加|创建|\badd\b|\bcreate\b)/i;
const unsafeTextPattern =
  /(删除|移除|提交|保存|确定|确认|导入|导出|下发|关闭|作废|审核|审批|批量|清空|查询|搜索|刷新|编辑|修改|禁用|启用|退回|取消)/i;

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function normalizeUrl(targetUrl) {
  return new URL(targetUrl, `${baseUrl}/`).toString();
}

function parseTargets() {
  const raw = process.env.ADP_PRODUCTION_DISCOVERY_TARGETS || "";
  const values = raw
    .split(/[\n,]+/)
    .map((value) => value.trim())
    .filter(Boolean);

  if (!values.length) {
    return defaultTargets;
  }

  return values.map((value, index) => {
    const parts = value.split("|").map((part) => part.trim());
    const url = parts[0];
    return {
      id: parts[1] || `PROD-DISCOVERY-${String(index + 1).padStart(2, "0")}`,
      label: parts[2] || `production route discovery ${index + 1}`,
      url,
    };
  });
}

function targetSlug(target, index) {
  const raw = `${String(index + 1).padStart(2, "0")}-${target.id}-${target.url}`;
  return raw
    .replace(/^https?:\/\//i, "")
    .replace(/[^a-z0-9._-]+/gi, "-")
    .replace(/^-+|-+$/g, "")
    .slice(0, 120);
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

function findVisibleError(bodyText) {
  return String(bodyText || "")
    .split(/\r?\n/)
    .map((line) => line.trim())
    .find((line) => line && visibleErrorPattern.test(line));
}

function truncate(value, limit = 500) {
  const text = String(value || "");
  return text.length > limit ? `${text.slice(0, limit)}...` : text;
}

async function collectClickableElements(page) {
  return page.evaluate(() => {
    function isVisible(element) {
      const style = window.getComputedStyle(element);
      const box = element.getBoundingClientRect();
      return (
        style &&
        style.visibility !== "hidden" &&
        style.display !== "none" &&
        box.width > 0 &&
        box.height > 0
      );
    }

    const selector = [
      "button",
      "a",
      "input[type=button]",
      "input[type=submit]",
      "[role=button]",
      ".el-button",
      ".ivu-btn",
      ".ant-btn",
      ".btn",
    ].join(",");

    return Array.from(document.querySelectorAll(selector))
      .filter(isVisible)
      .map((element, index) => {
        const text =
          element.innerText ||
          element.textContent ||
          element.getAttribute("value") ||
          element.getAttribute("title") ||
          element.getAttribute("aria-label") ||
          "";
        const discoveryId = `adp-discovery-${index}`;
        element.setAttribute("data-adp-discovery-id", discoveryId);
        return {
          discoveryId,
          tag: element.tagName.toLowerCase(),
          text: text.replace(/\s+/g, " ").trim(),
          title: element.getAttribute("title") || "",
          ariaLabel: element.getAttribute("aria-label") || "",
          className: element.getAttribute("class") || "",
          id: element.getAttribute("id") || "",
          disabled: Boolean(element.disabled || element.getAttribute("aria-disabled") === "true"),
        };
      })
      .filter((item) => item.text || item.title || item.ariaLabel)
      .slice(0, 120);
  });
}

async function collectFormFields(page) {
  return page.evaluate(() => {
    function isVisible(element) {
      const style = window.getComputedStyle(element);
      const box = element.getBoundingClientRect();
      return (
        style &&
        style.visibility !== "hidden" &&
        style.display !== "none" &&
        box.width > 0 &&
        box.height > 0
      );
    }

    function labelFor(element) {
      const id = element.getAttribute("id");
      if (id) {
        const explicit = document.querySelector(`label[for="${CSS.escape(id)}"]`);
        if (explicit && explicit.textContent) {
          return explicit.textContent.replace(/\s+/g, " ").trim();
        }
      }
      const wrapper = element.closest("label,.el-form-item,.ivu-form-item,.ant-form-item,.form-group,.field");
      if (wrapper && wrapper.textContent) {
        return wrapper.textContent.replace(/\s+/g, " ").trim().slice(0, 120);
      }
      return "";
    }

    const selector = [
      "input",
      "textarea",
      "select",
      "[contenteditable=true]",
      ".el-select",
      ".ivu-select",
      ".ant-select",
      ".el-date-editor",
    ].join(",");

    return Array.from(document.querySelectorAll(selector))
      .filter(isVisible)
      .map((element) => ({
        tag: element.tagName.toLowerCase(),
        type: element.getAttribute("type") || "",
        name: element.getAttribute("name") || "",
        placeholder: element.getAttribute("placeholder") || "",
        title: element.getAttribute("title") || "",
        ariaLabel: element.getAttribute("aria-label") || "",
        label: labelFor(element),
        value: element.tagName.toLowerCase() === "input" ? "" : "",
        required: Boolean(element.required || element.getAttribute("aria-required") === "true"),
        className: element.getAttribute("class") || "",
      }))
      .slice(0, 160);
  });
}

async function collectDialogs(page) {
  return page.evaluate(() => {
    function isVisible(element) {
      const style = window.getComputedStyle(element);
      const box = element.getBoundingClientRect();
      return (
        style &&
        style.visibility !== "hidden" &&
        style.display !== "none" &&
        box.width > 0 &&
        box.height > 0
      );
    }

    const selector = [
      "[role=dialog]",
      ".el-dialog",
      ".ivu-modal",
      ".ant-modal",
      ".modal",
      ".dialog",
      ".supos-dialog",
    ].join(",");

    return Array.from(document.querySelectorAll(selector))
      .filter(isVisible)
      .map((element) => ({
        className: element.getAttribute("class") || "",
        title:
          (element.querySelector(".el-dialog__title,.ivu-modal-header,.ant-modal-title,.modal-title") || {})
            .textContent || "",
        text: (element.innerText || element.textContent || "").replace(/\s+/g, " ").trim().slice(0, 1000),
      }))
      .slice(0, 20);
  });
}

function pickSafeCreateButton(buttons) {
  return buttons.find((button) => {
    const label = `${button.text} ${button.title} ${button.ariaLabel}`.trim();
    return !button.disabled && createTextPattern.test(label) && !unsafeTextPattern.test(label);
  });
}

function isQueryLikePost(entry) {
  return (
    entry.method === "POST" &&
    /(-pending|-list|\bquery\b|search|page|dataGrid|getData|list-|List-|\/data-|\bpaging\b)/i.test(
      `${entry.url} ${entry.requestBody}`,
    )
  );
}

function isPotentialWrite(entry) {
  if (!["POST", "PUT", "DELETE", "PATCH"].includes(entry.method)) {
    return false;
  }
  return !isQueryLikePost(entry);
}

async function discoverTarget(context, target, index) {
  const slug = targetSlug(target, index);
  const initialScreenshot = path.join(outputDir, `${slug}-initial.png`);
  const afterCreateScreenshot = path.join(outputDir, `${slug}-after-create-click.png`);
  const page = await context.newPage();
  const network = [];
  const consoleErrors = [];
  const pageErrors = [];
  const requestFailures = [];

  page.on("response", async (response) => {
    const type = response.request().resourceType();
    if (!["document", "xhr", "fetch", "script", "stylesheet"].includes(type)) {
      return;
    }
    const requestItem = response.request();
    const postData = requestItem.postData();
    const item = {
      phase: "observed",
      status: response.status(),
      method: requestItem.method(),
      type,
      url: response.url(),
      requestBody: postData ? truncate(postData) : "",
      contentType: response.headers()["content-type"] || "",
      errorLike: response.status() >= 400,
    };
    if (response.status() >= 400) {
      try {
        item.responseBody = truncate(await response.text(), 500);
      } catch (_error) {
        item.responseBody = "";
      }
    }
    network.push(item);
  });

  page.on("console", (message) => {
    if (message.type() === "error") {
      consoleErrors.push(message.text());
    }
  });
  page.on("pageerror", (error) => pageErrors.push(error.message));
  page.on("requestfailed", (requestItem) => {
    if (["document", "xhr", "fetch", "script", "stylesheet"].includes(requestItem.resourceType())) {
      requestFailures.push({
        method: requestItem.method(),
        type: requestItem.resourceType(),
        url: requestItem.url(),
        failure: requestItem.failure() && requestItem.failure().errorText,
      });
    }
  });

  const resolvedUrl = normalizeUrl(target.url);
  const navigation = await page.goto(resolvedUrl, { waitUntil: "domcontentloaded", timeout: 45000 });
  await page.waitForLoadState("networkidle", { timeout: 8000 }).catch(() => {});
  await page.waitForTimeout(1500);
  await page.screenshot({ path: initialScreenshot, fullPage: true });

  const beforeText = await page.locator("body").innerText({ timeout: 5000 }).catch(() => "");
  const visibleErrorBefore = findVisibleError(beforeText) || null;
  const buttonsBefore = await collectClickableElements(page);
  const fieldsBefore = await collectFormFields(page);
  const safeCreateButton = pickSafeCreateButton(buttonsBefore);

  let clickedCreate = null;
  let clickError = null;
  let dialogsAfterClick = [];
  let fieldsAfterClick = [];
  let buttonsAfterClick = [];
  let visibleErrorAfter = null;
  let afterClickNetworkStart = network.length;
  if (safeCreateButton && clickCreate) {
    clickedCreate = safeCreateButton;
    try {
      await page.locator(`[data-adp-discovery-id="${safeCreateButton.discoveryId}"]`).click({ timeout: 8000 });
      await page.waitForLoadState("networkidle", { timeout: 5000 }).catch(() => {});
      await page.waitForTimeout(1200);
      await page.screenshot({ path: afterCreateScreenshot, fullPage: true });
      const afterText = await page.locator("body").innerText({ timeout: 5000 }).catch(() => "");
      visibleErrorAfter = findVisibleError(afterText) || null;
      dialogsAfterClick = await collectDialogs(page);
      fieldsAfterClick = await collectFormFields(page);
      buttonsAfterClick = await collectClickableElements(page);
    } catch (error) {
      clickError = error.message;
    }
  }

  const nonGetRequests = network.filter((entry) => entry.method !== "GET");
  const potentialWriteRequests = network.filter(isPotentialWrite);
  const networkErrors = network.filter((entry) => entry.errorLike);
  const discoveryStatus = clickError
    ? "CREATE_BUTTON_CLICK_FAILED"
    : safeCreateButton && !clickCreate
      ? "CREATE_BUTTON_FOUND_NOT_CLICKED"
      : safeCreateButton && (dialogsAfterClick.length || fieldsAfterClick.length)
      ? "CREATE_ENTRY_DISCOVERED"
      : safeCreateButton
        ? "CREATE_BUTTON_CLICKED_NO_FORM_DETECTED"
        : "NO_SAFE_CREATE_BUTTON_FOUND";

  const result = {
    target,
    resolvedUrl,
    navigationStatus: navigation ? navigation.status() : null,
    discoveryStatus,
    screenshots: {
      initial: initialScreenshot,
      afterCreateClick: fs.existsSync(afterCreateScreenshot) ? afterCreateScreenshot : "",
    },
    safety: {
      createClickEnabled: clickCreate,
      submittedBusinessData: false,
      clickedOnlySafeCreateCandidate: Boolean(clickedCreate),
      nonGetRequests,
      potentialWriteRequests,
    },
    visibleErrorBefore,
    visibleErrorAfter,
    consoleErrors,
    pageErrors,
    requestFailures,
    networkErrors,
    buttonsBefore,
    fieldsBefore,
    safeCreateCandidate: safeCreateButton || null,
    clickedCreate,
    clickError,
    dialogsAfterClick,
    fieldsAfterClick,
    buttonsAfterClick,
    networkAfterCreateClick: network.slice(afterClickNetworkStart),
    network,
    nextAction:
      discoveryStatus === "CREATE_ENTRY_DISCOVERED"
        ? "Review dialog fields and network endpoints, then execute marker-based create in a dedicated persistence acceptance run."
        : "Locate the create entry manually or inspect static page/runtime metadata before attempting marker-based create.",
  };

  await page.close();
  return result;
}

function resultHasErrors(result) {
  return Boolean(
    result.visibleErrorBefore ||
      result.visibleErrorAfter ||
      result.consoleErrors.length ||
      result.pageErrors.length ||
      result.requestFailures.length ||
      result.networkErrors.length,
  );
}

async function main() {
  ensureDir(outputDir);
  const reportPath = path.join(outputDir, "production-action-discovery.json");
  const targets = parseTargets();

  const api = await request.newContext({ ignoreHTTPSErrors: true });
  const ticket = await login(api);
  await api.dispose();

  const browser = await chromium.launch({ headless });
  const context = await browser.newContext({
    baseURL: baseUrl,
    ignoreHTTPSErrors: true,
    viewport: { width: 1440, height: 960 },
    extraHTTPHeaders: {
      Authorization: `Bearer ${ticket}`,
    },
  });
  await context.addCookies([
    { name: "suposTicket", value: ticket, url: baseUrl },
    { name: "SUPOS_TICKET", value: ticket, url: baseUrl },
  ]);
  await context.addInitScript((token) => {
    window.localStorage.setItem("suposTicket", token);
    window.localStorage.setItem("SUPOS_TICKET", token);
    window.localStorage.setItem("token", token);
    window.sessionStorage.setItem("suposTicket", token);
    window.sessionStorage.setItem("SUPOS_TICKET", token);
    window.sessionStorage.setItem("token", token);
  }, ticket);

  const results = [];
  for (const [index, item] of targets.entries()) {
    results.push(await discoverTarget(context, item, index));
  }

  const errorCount = results.filter(resultHasErrors).length;
  const report = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    clickCreate,
    targets,
    summary: {
      totalTargets: results.length,
      targetsWithErrors: errorCount,
      createEntriesDiscovered: results.filter((item) => item.discoveryStatus === "CREATE_ENTRY_DISCOVERED").length,
      createButtonsFoundNotClicked: results.filter((item) => item.discoveryStatus === "CREATE_BUTTON_FOUND_NOT_CLICKED")
        .length,
      potentialWriteRequests: results.reduce((total, item) => total + item.safety.potentialWriteRequests.length, 0),
    },
    targetResults: results,
  };

  if (results.length === 1) {
    Object.assign(report, results[0]);
  } else {
    report.discoveryStatus = errorCount ? "MULTI_TARGET_DISCOVERY_WITH_ERRORS" : "MULTI_TARGET_DISCOVERY_COMPLETE";
  }

  fs.writeFileSync(reportPath, JSON.stringify(report, null, 2));
  await context.close();
  await browser.close();

  console.log(`DISCOVERY_STATUS ${report.discoveryStatus}`);
  console.log(`REPORT ${reportPath}`);
  for (const result of results) {
    console.log(`DISCOVERY_TARGET ${result.target.id} ${result.discoveryStatus} ${result.target.url}`);
    if (result.screenshots.initial) {
      console.log(`SCREENSHOT_INITIAL ${result.target.id} ${result.screenshots.initial}`);
    }
    if (result.screenshots.afterCreateClick) {
      console.log(`SCREENSHOT_AFTER_CREATE ${result.target.id} ${result.screenshots.afterCreateClick}`);
    }
  }

  if (errorCount) {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
