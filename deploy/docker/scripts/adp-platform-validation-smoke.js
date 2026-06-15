#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");

const rootDir = path.resolve(__dirname, "../../..");
const scriptDir = __dirname;
const baseUrl = (process.env.ADP_BASE_URL || "http://10.11.100.17:18080").replace(/\/+$/, "");
const username = process.env.ADP_USERNAME || "admin";
const timestamp = new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14);
const outputDir =
  process.env.ADP_PLATFORM_OUTPUT_DIR || path.join("/tmp", `adp-platform-validation-${timestamp}`);
const platformMenuLimit = process.env.ADP_PLATFORM_MENU_LIMIT || "40";

const skipMenu = process.env.ADP_SKIP_MENU_SMOKE === "true";
const skipTodo = process.env.ADP_SKIP_TODO_SMOKE === "true";
const skipApi = process.env.ADP_SKIP_API_SMOKE === "true";

const sections = [
  {
    id: "platform-api",
    title: "Platform API smoke",
    requiredFor: ["login", "current-user", "permissions", "menus", "todo-api", "basic-config", "entity-config"],
    skip: skipApi,
    script: "adp-platform-api-smoke.js",
    env: {
      ADP_API_SMOKE_OUTPUT: path.join(outputDir, "platform-api-smoke.json"),
    },
    reportPath: path.join(outputDir, "platform-api-smoke.json"),
  },
  {
    id: "home-todo",
    title: "Home todo page smoke",
    requiredFor: ["login-page", "home-shell", "todo-page", "runtime-schema-errors"],
    skip: skipTodo,
    script: "adp-home-todo-smoke.js",
    env: {
      ADP_OUTPUT_DIR: path.join(outputDir, "home-todo"),
    },
    reportPath: path.join(outputDir, "home-todo", "home-todo-smoke-results.json"),
  },
  {
    id: "menu-pages",
    title: "Menu page smoke",
    requiredFor: ["menus", "front-routes", "page-network-errors", "visible-runtime-errors"],
    skip: skipMenu,
    script: "adp-menu-smoke.js",
    env: {
      ADP_OUTPUT_DIR: path.join(outputDir, "menu"),
      ADP_MENU_LIMIT: platformMenuLimit,
    },
    reportPath: path.join(outputDir, "menu", "menu-smoke-results.json"),
  },
];

function resolveModule(moduleName) {
  try {
    return require.resolve(moduleName);
  } catch (error) {
    return null;
  }
}

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function tail(value, maxLength = 4000) {
  const text = String(value || "");
  return text.length > maxLength ? text.slice(text.length - maxLength) : text;
}

function readJson(reportPath) {
  if (!fs.existsSync(reportPath)) {
    return null;
  }
  try {
    return JSON.parse(fs.readFileSync(reportPath, "utf8"));
  } catch (error) {
    return { parseError: error.message };
  }
}

function summarizeReport(section, parsed) {
  if (!parsed || parsed.parseError) {
    return parsed;
  }
  if (section.id === "platform-api" || section.id === "menu-pages") {
    return {
      total: parsed.total,
      passed: parsed.passed,
      failed: parsed.failed,
    };
  }
  if (section.id === "home-todo") {
    return {
      ok: parsed.ok,
      navigationStatus: parsed.navigationStatus,
      navigationError: parsed.navigationError,
      visibleError: parsed.visibleError,
      networkErrorCount: Array.isArray(parsed.networkErrors) ? parsed.networkErrors.length : null,
      consoleErrorCount: Array.isArray(parsed.consoleErrors) ? parsed.consoleErrors.length : null,
      pageErrorCount: Array.isArray(parsed.pageErrors) ? parsed.pageErrors.length : null,
    };
  }
  return parsed;
}

function runSection(section) {
  if (section.skip) {
    return {
      id: section.id,
      title: section.title,
      requiredFor: section.requiredFor,
      status: "skipped",
      ok: true,
      reportPath: section.reportPath,
    };
  }

  const scriptPath = path.join(scriptDir, section.script);
  const env = {
    ...process.env,
    ...section.env,
    ADP_BASE_URL: baseUrl,
  };
  const result = spawnSync(process.execPath, [scriptPath], {
    cwd: rootDir,
    env,
    encoding: "utf8",
  });
  const parsed = readJson(section.reportPath);
  const ok = result.status === 0 && !result.error;
  return {
    id: section.id,
    title: section.title,
    requiredFor: section.requiredFor,
    status: ok ? "passed" : "failed",
    ok,
    command: `node ${path.relative(rootDir, scriptPath)}`,
    exitCode: result.status,
    signal: result.signal,
    error: result.error ? result.error.message : null,
    reportPath: section.reportPath,
    summary: summarizeReport(section, parsed),
    stdoutTail: tail(result.stdout),
    stderrTail: tail(result.stderr),
  };
}

function main() {
  ensureDir(outputDir);

  const prerequisiteResults = [];
  const playwrightPath = resolveModule("playwright");
  if (!playwrightPath) {
    prerequisiteResults.push({
      id: "playwright",
      title: "Playwright dependency",
      status: "failed",
      ok: false,
      message: "Node module 'playwright' is required for ADP platform smoke scripts. Run `npm install` and `npx playwright install chromium`, or provide NODE_PATH with a Playwright installation.",
    });
  } else {
    prerequisiteResults.push({
      id: "playwright",
      title: "Playwright dependency",
      status: "passed",
      ok: true,
      resolvedPath: playwrightPath,
    });
  }

  const runnable = prerequisiteResults.every((result) => result.ok);
  const results = runnable ? sections.map(runSection) : [];
  const allResults = prerequisiteResults.concat(results);
  const failed = results.filter((result) => !result.ok);
  const failedPrerequisites = prerequisiteResults.filter((result) => !result.ok);
  const skipped = allResults.filter((result) => result.status === "skipped");
  const passed = allResults.filter((result) => result.ok && result.status !== "skipped");
  const report = {
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    baseUrl,
    username,
    outputDir,
    ok: failed.length === 0 && failedPrerequisites.length === 0,
    total: allResults.length,
    passed: passed.length,
    failed: failed.length + failedPrerequisites.length,
    skipped: skipped.length,
    scope: {
      platform: [
        "login",
        "current-user",
        "users-organizations-permissions",
        "menus",
        "todo",
        "basic-configuration",
        "nacos-keycloak-postgresql-runtime-patch-through-smoke",
      ],
      businessModules: "startup-menu-api-table-initial-check-only",
    },
    prerequisites: prerequisiteResults,
    results,
  };

  const reportPath = path.join(outputDir, "platform-validation-summary.json");
  fs.writeFileSync(reportPath, `${JSON.stringify(report, null, 2)}\n`);

  for (const result of allResults) {
    const summary = result.summary ? ` ${JSON.stringify(result.summary)}` : "";
    console.log(`${result.ok ? "OK" : "FAIL"} ${result.id} status=${result.status}${summary}`);
  }
  console.log(`SUMMARY total=${report.total} passed=${report.passed} failed=${report.failed} skipped=${report.skipped}`);
  console.log(`REPORT ${reportPath}`);

  if (failed.length || failedPrerequisites.length) {
    process.exitCode = 1;
  }
}

main();
