#!/usr/bin/env node
"use strict";

const fs = require("node:fs");
const path = require("node:path");
const { spawnSync } = require("node:child_process");
const { chromium, request } = require("playwright");

const repoRoot = path.resolve(__dirname, "../../..");
const stamp = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const password = process.env.ADP_PASSWORD || "123456";
const outputDir =
  process.env.ADP_OUTPUT_DIR || path.join("/tmp", `adp-qcs-other-quality-${stamp}`);
const outputPath =
  process.env.ADP_OUTPUT_PATH || path.join(outputDir, "qcs-other-quality-results.json");
const headless = process.env.ADP_HEADLESS !== "false";
const verbose = process.env.ADP_VERBOSE === "true";
const visibleErrorPattern =
  /(数据库操作异常|系统发生未知异常|系统错误|服务器异常|SQLGrammarException|could not extract ResultSet|Bad value for type long)/i;

const pages = [
  {
    key: "otherInspect",
    label: "其他检验申请",
    route: "/msService/QCS/inspect/inspect/otherInspectList",
    viewCode: "QCS_5.0.0.0_inspect_otherInspectList",
    expectedButtons: ["新增申请", "打开", "关闭", "批量提交", "删除"],
    addButton: true,
    editViewCode: "QCS_5.0.0.0_inspect_otherInspectEdit",
    expectedFormText: ["业务类型", "请检人", "请检部门", "请检时间"],
  },
  {
    key: "otherReport",
    label: "其他检验报告",
    route: "/msService/QCS/inspectReport/inspectReport/otherInspReportList",
    viewCode: "QCS_5.0.0.0_inspectReport_otherInspReportList",
    expectedButtons: ["删除"],
  },
  {
    key: "otherUnqualified",
    label: "其他不合格品处理",
    route: "/msService/QCS/unQlfDeal/unQlfDeal/otherUnQlfDealList",
    viewCode: "QCS_5.0.0.0_unQlfDeal_otherUnQlfDealList",
    expectedButtons: ["删除"],
  },
  {
    key: "qualityInspect",
    label: "质量巡检申请",
    route: "/msService/QCS/inspect/inspect/qualityInspectList",
    viewCode: "QCS_5.0.0.0_inspect_qualityInspectList",
    expectedButtons: ["新增申请", "打开", "关闭", "批量提交", "删除"],
    addButton: true,
    editViewCode: "QCS_5.0.0.0_inspect_qualityInspectEdit",
    expectedFormText: ["巡检类型", "业务类型", "请检人", "请检部门"],
  },
  {
    key: "qualityReport",
    label: "质量巡检报告",
    route: "/msService/QCS/inspectReport/inspectReport/quaInspReportList",
    viewCode: "QCS_5.0.0.0_inspectReport_quaInspReportList",
    expectedButtons: ["删除"],
  },
];

const layoutTargets = [
  ...pages.map((item) => ({ label: item.label, viewCode: item.viewCode, kind: "list" })),
  {
    label: "其他检验申请编辑",
    viewCode: "QCS_5.0.0.0_inspect_otherInspectEdit",
    kind: "edit",
  },
  {
    label: "其他检验申请查看",
    viewCode: "QCS_5.0.0.0_inspect_otherInspectView",
    kind: "view",
  },
  {
    label: "质量巡检申请编辑",
    viewCode: "QCS_5.0.0.0_inspect_qualityInspectEdit",
    kind: "edit",
  },
  {
    label: "质量巡检申请查看",
    viewCode: "QCS_5.0.0.0_inspect_qualityInspectView",
    kind: "view",
  },
  {
    label: "其他检验报告编辑",
    viewCode: "QCS_5.0.0.0_inspectReport_otherInspReportEdit",
    kind: "edit",
  },
  {
    label: "其他检验报告查看",
    viewCode: "QCS_5.0.0.0_inspectReport_otherInspReportView",
    kind: "view",
  },
  {
    label: "质量巡检报告编辑",
    viewCode: "QCS_5.0.0.0_inspectReport_quaInspReportEdit",
    kind: "edit",
  },
  {
    label: "质量巡检报告查看",
    viewCode: "QCS_5.0.0.0_inspectReport_quaInspReportView",
    kind: "view",
  },
  {
    label: "其他不合格品处理编辑",
    viewCode: "QCS_5.0.0.0_unQlfDeal_otherUnQlfDealEdit",
    kind: "edit",
  },
  {
    label: "其他不合格品处理查看",
    viewCode: "QCS_5.0.0.0_unQlfDeal_otherUnQlfDealView",
    kind: "view",
  },
];

function progress(message) {
  if (verbose) {
    console.error(`[qcs-other-quality] ${message}`);
  }
}

function repoCommit() {
  const result = spawnSync("git", ["rev-parse", "HEAD"], {
    cwd: repoRoot,
    encoding: "utf8",
  });
  return result.status === 0 ? result.stdout.trim() : "UNKNOWN";
}

function ticketFrom(payload) {
  const candidates = [
    payload && payload.ticket,
    payload && payload.token,
    payload && payload.access_token,
    payload && payload.data && payload.data.ticket,
    payload && payload.data && payload.data.token,
    payload && payload.data && payload.data.access_token,
  ];
  return candidates.find((value) => typeof value === "string" && value.length > 20);
}

async function responseBody(response) {
  const text = await response.text();
  try {
    return { text, json: JSON.parse(text) };
  } catch (_error) {
    return { text, json: null };
  }
}

async function login(api) {
  const attempts = [
    { userName: username, password, clientId: "pc_dt" },
    { username, password, clientId: "pc_dt" },
  ];
  const failures = [];
  for (const data of attempts) {
    const response = await api.post(`${baseUrl}/inter-api/auth/login`, {
      data,
      headers: {
        Accept: "application/json",
        "Content-Type": "application/json;charset=UTF-8",
      },
    });
    const parsed = await responseBody(response);
    const ticket = response.ok() ? ticketFrom(parsed.json) : null;
    if (ticket) {
      return { ticket, status: response.status() };
    }
    failures.push({ status: response.status(), body: parsed.text.slice(0, 300) });
  }
  throw new Error(`Login failed: ${JSON.stringify(failures)}`);
}

function normalizeText(value) {
  return String(value || "").replace(/\s+/g, " ").trim();
}

async function visibleExactText(page, text) {
  const matches = page.getByText(text, { exact: true });
  const count = await matches.count();
  for (let index = 0; index < count; index += 1) {
    if (await matches.nth(index).isVisible().catch(() => false)) {
      return matches.nth(index);
    }
  }
  return null;
}

async function checkLayout(context, target) {
  const url = `/msService/baseService/view/layoutJson?viewCode=${encodeURIComponent(
    target.viewCode
  )}&isEs5=true`;
  const response = await context.request.get(url);
  const parsed = await responseBody(response);
  const item = {
    ...target,
    endpoint: url,
    httpStatus: response.status(),
    responseBytes: Buffer.byteLength(parsed.text, "utf8"),
    status: "PASS",
    issues: [],
  };
  if (response.status() !== 200) {
    item.issues.push(`layoutJson returned HTTP ${response.status()}`);
  }
  if (!parsed.text || visibleErrorPattern.test(parsed.text)) {
    item.issues.push("layoutJson contains an empty or server-error payload");
  }
  if (parsed.text.includes("ec.common.tableNo")) {
    item.issues.push("layoutJson still contains raw ec.common.tableNo text");
  }
  if (parsed.text.includes("默认操作")) {
    item.issues.push("layoutJson still contains the default-operation placeholder");
  }
  if (item.issues.length) {
    item.status = "FAIL";
  }
  return item;
}

async function checkAddInteraction(page, target, item) {
  const addButton = await visibleExactText(page, "新增申请");
  if (!addButton) {
    throw new Error("新增申请按钮不可见");
  }
  const beforeUrl = page.url();
  const beforeFrameCount = await page.locator("iframe:visible").count();
  const editResponses = [];
  const responseListener = (response) => {
    if (
      response.url().includes(target.editViewCode) ||
      response.url().includes(target.editViewCode.split("_").pop())
    ) {
      editResponses.push({
        status: response.status(),
        method: response.request().method(),
        url: response.url(),
      });
    }
  };
  page.on("response", responseListener);
  const popupPromise = page.waitForEvent("popup", { timeout: 5000 }).catch(() => null);
  await addButton.click();
  const popup = await popupPromise;
  if (popup) {
    await popup.waitForLoadState("domcontentloaded", { timeout: 30000 }).catch(() => {});
  }
  await page.waitForTimeout(2200);
  page.off("response", responseListener);

  let surface = page;
  let surfaceKind = "page";
  const visibleFrames = page.locator("iframe:visible");
  const afterFrameCount = await visibleFrames.count();
  const visibleDialogs = page.locator(
    ".ant-modal:visible, .layui-layer:visible, [role='dialog']:visible"
  );
  const dialogCount = await visibleDialogs.count();
  if (popup) {
    surface = popup;
    surfaceKind = "popup";
  } else if (afterFrameCount > beforeFrameCount) {
    const frame = visibleFrames.last().contentFrame();
    if (frame) {
      surface = frame;
      surfaceKind = "frame";
      await frame.locator("body").waitFor({ state: "visible", timeout: 15000 });
    }
  } else if (dialogCount > 0) {
    surface = visibleDialogs.last();
    surfaceKind = "dialog";
  }
  const rawBodyText =
    surfaceKind === "dialog"
      ? await surface.innerText()
      : await surface.locator("body").innerText();
  const bodyText = normalizeText(rawBodyText);
  const expectedVisible = target.expectedFormText.every((text) => bodyText.includes(text));
  const popupUrl = popup ? popup.url() : null;
  const surfaceChanged =
    Boolean(popup) || page.url() !== beforeUrl || afterFrameCount > beforeFrameCount || dialogCount > 0;
  const successfulEditResponse =
    (popupUrl && popupUrl.includes(target.editViewCode.split("_").pop())) ||
    editResponses.some((response) => response.status < 400);
  item.addInteraction = {
    beforeUrl,
    afterUrl: page.url(),
    popupUrl,
    beforeFrameCount,
    afterFrameCount,
    dialogCount,
    surfaceKind,
    surfaceChanged,
    editResponses,
    expectedFormText: target.expectedFormText,
    expectedFormTextVisible: expectedVisible,
    bodyTextExcerpt: bodyText.slice(0, 800),
  };
  if (!surfaceChanged || !successfulEditResponse) {
    throw new Error("新增申请按钮未真正打开编辑页面");
  }
  if (!expectedVisible) {
    throw new Error(`新增申请未打开完整表单：${target.expectedFormText.join("、")}`);
  }
  if (visibleErrorPattern.test(bodyText)) {
    throw new Error("新增申请表单出现系统或数据库异常");
  }

  if (popup) {
    await popup.close();
    return;
  }

  const close = await visibleExactText(page, "取消");
  if (close) {
    await close.click().catch(() => {});
  } else {
    const closeIcon = page
      .locator(".ant-modal-close:visible, .layui-layer-close:visible, [title='关闭']:visible")
      .last();
    if (await closeIcon.isVisible().catch(() => false)) {
      await closeIcon.click().catch(() => {});
    }
  }
  await page.waitForTimeout(300);
}

async function checkPage(page, target, evidence) {
  const item = {
    module: target.label,
    route: target.route,
    viewCode: target.viewCode,
    expectedButtons: target.expectedButtons,
    visibleButtons: [],
    httpStatus: null,
    status: "PASS",
    issues: [],
  };
  const firstServerError = evidence.frontend.serverErrors.length;
  const firstClientError = evidence.frontend.clientErrors.length;
  try {
    progress(`opening ${target.label}`);
    const navigation = await page.goto(target.route, {
      waitUntil: "domcontentloaded",
      timeout: 60000,
    });
    item.httpStatus = navigation && navigation.status();
    await page.waitForTimeout(1800);
    const bodyText = normalizeText(await page.locator("body").innerText());
    item.bodyTextExcerpt = bodyText.slice(0, 1200);
    for (const buttonText of target.expectedButtons) {
      if (await visibleExactText(page, buttonText)) {
        item.visibleButtons.push(buttonText);
      }
    }
    const missing = target.expectedButtons.filter((text) => !item.visibleButtons.includes(text));
    if (missing.length) {
      item.issues.push(`缺少按钮：${missing.join("、")}`);
    }
    if (bodyText.includes("ec.common.tableNo")) {
      item.issues.push("页面仍显示 ec.common.tableNo");
    }
    if (bodyText.includes("ec.list.taskDescription")) {
      item.issues.push("页面仍显示 ec.list.taskDescription");
    }
    if (bodyText.includes("默认操作")) {
      item.issues.push("页面仍显示默认操作占位文案");
    }
    const visibleError = bodyText.match(visibleErrorPattern);
    if (visibleError) {
      item.issues.push(`页面显示错误：${visibleError[0]}`);
    }
    if (target.addButton && !missing.includes("新增申请")) {
      try {
        await checkAddInteraction(page, target, item);
      } catch (error) {
        item.issues.push(error.message);
      }
    }
    item.serverErrors = evidence.frontend.serverErrors.slice(firstServerError);
    item.clientErrors = evidence.frontend.clientErrors.slice(firstClientError);
    if (item.serverErrors.length) {
      item.issues.push(`出现 ${item.serverErrors.length} 个 HTTP 5xx`);
    }
    const screenshot = path.join(outputDir, `${target.key}.png`);
    await page.screenshot({ path: screenshot, fullPage: true });
    item.screenshot = screenshot;
  } catch (error) {
    item.issues.push(error.message);
  }
  if (item.issues.length) {
    item.status = "FAIL";
  }
  return item;
}

async function main() {
  fs.mkdirSync(outputDir, { recursive: true });
  const evidence = {
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    repoCommit: repoCommit(),
    database: "PostgreSQL",
    baseUrl,
    status: "RUNNING",
    summary: {
      testedFeatures: pages.length + layoutTargets.length,
      pass: 0,
      fail: 0,
      blocked: 0,
      notApplicable: 0,
    },
    frontend: {
      pages: [],
      layouts: [],
      console: [],
      pageErrors: [],
      requestFailures: [],
      clientErrors: [],
      serverErrors: [],
    },
    persistence: {
      required: false,
      status: "NOT_APPLICABLE",
      reason: "本轮只恢复并验证运行时页面布局与动作入口，不保存业务单据。",
    },
    issues: [],
  };

  let browser;
  try {
    const api = await request.newContext({ ignoreHTTPSErrors: true });
    const loginResult = await login(api);
    await api.dispose();
    evidence.login = { status: loginResult.status, authenticated: true };

    browser = await chromium.launch({ headless });
    const context = await browser.newContext({
      baseURL: baseUrl,
      ignoreHTTPSErrors: true,
      viewport: { width: 1920, height: 1080 },
    });
    await context.addCookies([
      { name: "suposTicket", value: loginResult.ticket, url: baseUrl },
      { name: "SUPOS_TICKET", value: loginResult.ticket, url: baseUrl },
    ]);
    await context.addInitScript((ticket) => {
      localStorage.setItem("ticket", ticket);
      localStorage.setItem("suposTicket", ticket);
      localStorage.setItem("SUPOS_TICKET", ticket);
      localStorage.setItem("token", ticket);
    }, loginResult.ticket);

    for (const target of layoutTargets) {
      const item = await checkLayout(context, target);
      evidence.frontend.layouts.push(item);
      evidence.summary[item.status === "PASS" ? "pass" : "fail"] += 1;
    }

    const page = await context.newPage();
    page.setDefaultTimeout(15000);
    let activeStage = "initialization";
    page.on("console", (message) => {
      if (["error", "warning"].includes(message.type())) {
        evidence.frontend.console.push({
          stage: activeStage,
          type: message.type(),
          text: message.text(),
        });
      }
    });
    page.on("pageerror", (error) => {
      evidence.frontend.pageErrors.push({
        stage: activeStage,
        name: error.name,
        message: error.message,
      });
    });
    page.on("requestfailed", (requestItem) => {
      evidence.frontend.requestFailures.push({
        stage: activeStage,
        method: requestItem.method(),
        url: requestItem.url(),
        failure: requestItem.failure(),
      });
    });
    page.on("response", (response) => {
      const entry = {
        stage: activeStage,
        status: response.status(),
        method: response.request().method(),
        url: response.url(),
      };
      if (response.status() >= 400 && response.status() < 500) {
        evidence.frontend.clientErrors.push(entry);
      }
      if (response.status() >= 500) {
        evidence.frontend.serverErrors.push(entry);
      }
    });

    for (const target of pages) {
      activeStage = target.label;
      const item = await checkPage(page, target, evidence);
      evidence.frontend.pages.push(item);
      evidence.summary[item.status === "PASS" ? "pass" : "fail"] += 1;
    }

    const blockingConsole = evidence.frontend.console.filter(
      (entry) => entry.type === "error" && !/favicon|ResizeObserver/i.test(entry.text)
    );
    if (evidence.summary.fail || evidence.frontend.pageErrors.length || blockingConsole.length) {
      throw new Error("QCS 其他检验/质量巡检页面回归未通过");
    }
    evidence.status = "PASS";
  } catch (error) {
    evidence.status = "FAIL";
    evidence.issues.push(error.message);
  } finally {
    if (browser) {
      await browser.close();
    }
    fs.writeFileSync(outputPath, `${JSON.stringify(evidence, null, 2)}\n`, "utf8");
  }

  console.log(
    JSON.stringify(
      {
        status: evidence.status,
        outputPath,
        summary: evidence.summary,
        layouts: evidence.frontend.layouts.map((item) => ({
          label: item.label,
          status: item.status,
          httpStatus: item.httpStatus,
          responseBytes: item.responseBytes,
          issues: item.issues,
        })),
        pages: evidence.frontend.pages.map((item) => ({
          label: item.module,
          status: item.status,
          visibleButtons: item.visibleButtons,
          issues: item.issues,
        })),
        issues: evidence.issues,
      },
      null,
      2
    )
  );
  if (evidence.status !== "PASS") {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
