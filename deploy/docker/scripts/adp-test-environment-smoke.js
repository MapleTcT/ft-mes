#!/usr/bin/env node
"use strict";

const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");

const repoRoot = path.resolve(__dirname, "../../..");

const sshHost = process.env.ADP_SSH_HOST || "100.99.133.43";
const sshUser = process.env.ADP_SSH_USER || "v6";
const sshTarget = process.env.ADP_SSH_TARGET || `${sshUser}@${sshHost}`;
const sshConnectTimeout = process.env.ADP_SSH_CONNECT_TIMEOUT || "8";
const baseUrl = (process.env.ADP_BASE_URL || `http://${sshHost}:18080`).replace(/\/+$/, "");
const secondaryBaseUrl = (process.env.ADP_SECONDARY_BASE_URL || `http://${sshHost}:18070`).replace(/\/+$/, "");
const outputPath =
  process.env.ADP_TEST_ENVIRONMENT_SMOKE_OUTPUT ||
  path.join("/tmp", `adp-test-environment-smoke-${new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14)}.json`);
const httpProbeTimeoutMs = Number(process.env.ADP_HTTP_PROBE_TIMEOUT_MS || 15000);

const expectedContainers = (process.env.ADP_EXPECTED_CONTAINERS ||
  [
    "adp-mes-newbase-nginx-1",
    "adp-mes-newbase-gateway-1",
    "adp-mes-newbase-postgres-1",
    "adp-mes-newbase-nacos-1",
    "adp-mes-newbase-keycloak-1",
    "adp-mes-newbase-minio-1",
  ].join(","))
  .split(",")
  .map((item) => item.trim())
  .filter(Boolean);

function ensureDir(filePath) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
}

function ensureSafeToken(value, label) {
  if (!/^[A-Za-z0-9_.:@/-]+$/.test(value)) {
    throw new Error(`${label} contains unsupported characters: ${value}`);
  }
}

function addCheck(checks, name, passed, evidence) {
  checks.push({
    name,
    status: passed ? "PASS" : "FAIL",
    evidence,
  });
}

function getRepoCommit() {
  const result = spawnSync("git", ["rev-parse", "HEAD"], {
    cwd: repoRoot,
    encoding: "utf8",
  });
  return result.status === 0 ? result.stdout.trim() : "UNKNOWN";
}

async function probeHttp(url, label) {
  const startedAt = Date.now();
  let method = "HEAD";
  let response = await fetchWithTimeout(url, {
    method,
    redirect: "manual",
    headers: {
      Accept: "text/html,application/json,*/*",
    },
  });
  let bodyBytes = 0;
  if (response.status === 405 || response.status === 501) {
    method = "GET";
    response = await fetchWithTimeout(url, {
      method,
      redirect: "manual",
      headers: {
        Accept: "text/html,application/json,*/*",
        Range: "bytes=0-2047",
      },
    });
    bodyBytes = await readSampleBytes(response);
  }
  return {
    label,
    url,
    method,
    status: response.status,
    ok: response.status >= 200 && response.status < 400,
    elapsedMs: Date.now() - startedAt,
    contentType: response.headers.get("content-type") || "",
    contentLengthHeader: response.headers.get("content-length") || "",
    cacheControl: response.headers.get("cache-control") || "",
    server: response.headers.get("server") || "",
    bodyBytes,
  };
}

async function fetchWithTimeout(url, options) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), httpProbeTimeoutMs);
  try {
    return await fetch(url, {
      ...options,
      signal: controller.signal,
    });
  } finally {
    clearTimeout(timer);
  }
}

async function readSampleBytes(response) {
  if (!response.body || typeof response.body.getReader !== "function") {
    return 0;
  }
  const reader = response.body.getReader();
  let bytes = 0;
  try {
    while (bytes < 2048) {
      const chunk = await reader.read();
      if (chunk.done) {
        break;
      }
      bytes += chunk.value ? chunk.value.byteLength : 0;
    }
  } finally {
    try {
      await reader.cancel();
    } catch {
      // The response may already be closed after a small ranged body.
    }
  }
  return bytes;
}

function parseDockerPs(text) {
  return String(text || "")
    .split(/\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .map((line) => {
      const [name, image, status] = line.split(/\t/);
      return {
        name: name || "",
        image: image || "",
        status: status || "",
        up: /^Up\b/.test(status || ""),
      };
    })
    .filter((row) => row.name);
}

function runSshProbe() {
  ensureSafeToken(sshConnectTimeout, "ADP_SSH_CONNECT_TIMEOUT");
  const remoteScript = [
    "set -eu",
    "printf '__ADP_HOSTNAME__%s\\n' \"$(hostname)\"",
    "printf '__ADP_WHOAMI__%s\\n' \"$(whoami)\"",
    "printf '__ADP_DOCKER_PS_BEGIN__\\n'",
    "docker ps --format '{{.Names}}\\t{{.Image}}\\t{{.Status}}'",
    "printf '__ADP_DOCKER_PS_END__\\n'",
  ].join("\n");
  const result = spawnSync(
    "ssh",
    ["-o", "BatchMode=yes", "-o", `ConnectTimeout=${sshConnectTimeout}`, "-T", sshTarget, "sh", "-s"],
    {
      input: remoteScript,
      encoding: "utf8",
      maxBuffer: 20 * 1024 * 1024,
    }
  );
  if (result.error) {
    throw result.error;
  }
  if (result.status !== 0) {
    throw new Error(`SSH probe failed with status ${result.status}: ${result.stderr}`);
  }

  const hostname = (result.stdout.match(/^__ADP_HOSTNAME__(.*)$/m) || [])[1] || "";
  const whoami = (result.stdout.match(/^__ADP_WHOAMI__(.*)$/m) || [])[1] || "";
  const dockerMatch = result.stdout.match(/__ADP_DOCKER_PS_BEGIN__\n([\s\S]*?)\n__ADP_DOCKER_PS_END__/);
  return {
    hostname,
    whoami,
    containers: parseDockerPs(dockerMatch ? dockerMatch[1] : ""),
  };
}

async function main() {
  ensureSafeToken(sshHost, "ADP_SSH_HOST");
  ensureSafeToken(sshUser, "ADP_SSH_USER");

  const checks = [];
  const httpResults = [];
  let sshProbe = {
    hostname: "",
    whoami: "",
    containers: [],
  };
  let error = "";

  try {
    for (const [url, label] of [
      [baseUrl, "frontend-18080"],
      [secondaryBaseUrl, "secondary-18070"],
    ]) {
      const result = await probeHttp(url, label);
      httpResults.push(result);
      addCheck(
        checks,
        `http-${label}`,
        result.ok,
        `method=${result.method}; status=${result.status}; bytes=${result.bodyBytes}; elapsedMs=${result.elapsedMs}`
      );
    }

    sshProbe = runSshProbe();
    addCheck(checks, "ssh-reachable", true, `sshTarget=${sshTarget}; hostname=${sshProbe.hostname}; user=${sshProbe.whoami}`);
    const containersByName = new Map(sshProbe.containers.map((container) => [container.name, container]));
    for (const name of expectedContainers) {
      const container = containersByName.get(name);
      addCheck(checks, `container-up-${name}`, Boolean(container && container.up), container ? container.status : "missing");
    }
  } catch (caught) {
    error = caught.stack || caught.message;
    addCheck(checks, "test-environment-smoke", false, caught.message);
  }

  const failedChecks = checks.filter((check) => check.status !== "PASS");
  const report = {
    schemaVersion: 1,
    generatedAt: new Date().toISOString(),
    repoCommit: getRepoCommit(),
    database: "PostgreSQL",
    environment: {
      sshHost,
      sshUser,
      baseUrl,
      secondaryBaseUrl,
    },
    summary: {
      status: failedChecks.length === 0 ? "PASS" : "FAIL",
      totalChecks: checks.length,
      pass: checks.length - failedChecks.length,
      fail: failedChecks.length,
      expectedContainerCount: expectedContainers.length,
      runningExpectedContainers: checks.filter((check) => check.name.startsWith("container-up-") && check.status === "PASS").length,
    },
    checks,
    http: httpResults,
    ssh: {
      hostname: sshProbe.hostname,
      whoami: sshProbe.whoami,
      containers: sshProbe.containers,
    },
    expectedContainers,
    evidence: {
      method: "HTTP HEAD probes, small ranged GET fallback, plus SSH docker ps over the current test host.",
      secretHandling: "No application credentials, database passwords, SSH keys, or MinIO secrets are read or written by this smoke.",
      error,
    },
  };

  ensureDir(outputPath);
  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);
  console.log(JSON.stringify(report.summary, null, 2));
  console.log(`Test environment smoke report: ${outputPath}`);
  if (report.summary.status !== "PASS") {
    process.exitCode = 1;
  }
}

main();
