#!/usr/bin/env node
"use strict";

const crypto = require("crypto");
const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");

const repoRoot = path.resolve(__dirname, "../../..");

const sshHost = process.env.ADP_SSH_HOST || "100.99.133.43";
const sshUser = process.env.ADP_SSH_USER || "v6";
const sshTarget = process.env.ADP_SSH_TARGET || `${sshUser}@${sshHost}`;
const sshConnectTimeout = process.env.ADP_SSH_CONNECT_TIMEOUT || "8";
const minioContainer = process.env.ADP_MINIO_CONTAINER || "adp-mes-newbase-minio-1";
const minioEndpoint = process.env.ADP_MINIO_ENDPOINT || "http://127.0.0.1:30200";
const expectedBuckets = (
  process.env.ADP_MINIO_EXPECTED_BUCKETS === undefined
    ? "dtbucket,system001bucket"
    : process.env.ADP_MINIO_EXPECTED_BUCKETS
)
  .split(",")
  .map((item) => item.trim())
  .filter(Boolean);
const requestedBuckets = (process.env.ADP_MINIO_BUCKETS || "")
  .split(",")
  .map((item) => item.trim())
  .filter(Boolean);
const outputPath =
  process.env.ADP_MINIO_RUNTIME_SMOKE_OUTPUT ||
  path.join("/tmp", `adp-minio-runtime-smoke-${new Date().toISOString().replace(/[-:T.Z]/g, "").slice(0, 14)}.json`);

function ensureDir(filePath) {
  fs.mkdirSync(path.dirname(filePath), { recursive: true });
}

function ensureSafeToken(value, label) {
  if (!/^[A-Za-z0-9_.:@/-]+$/.test(value)) {
    throw new Error(`${label} contains unsupported characters: ${value}`);
  }
}

function ensureSafeBucket(value) {
  if (!/^[A-Za-z0-9][A-Za-z0-9_.-]*$/.test(value)) {
    throw new Error(`bucket contains unsupported characters: ${value}`);
  }
}

function shellSingleQuote(value) {
  return `'${String(value).replace(/'/g, "'\\''")}'`;
}

function sha256(text) {
  return crypto.createHash("sha256").update(String(text || "")).digest("hex");
}

function getRepoCommit() {
  const result = spawnSync("git", ["rev-parse", "HEAD"], {
    cwd: repoRoot,
    encoding: "utf8",
  });
  return result.status === 0 ? result.stdout.trim() : "UNKNOWN";
}

function parseDelimited(stdout) {
  const blocks = new Map();
  let current = null;
  let lines = [];
  for (const line of stdout.split(/\n/)) {
    const begin = line.match(/^__ADP_BEGIN__(.+)$/);
    if (begin) {
      current = begin[1];
      lines = [];
      continue;
    }
    const end = line.match(/^__ADP_END__(.+)$/);
    if (end) {
      blocks.set(end[1], lines.join("\n").trim());
      current = null;
      lines = [];
      continue;
    }
    if (current) {
      lines.push(line);
    }
  }
  return blocks;
}

function parseJsonLines(text) {
  const rows = [];
  const errors = [];
  for (const rawLine of String(text || "").split(/\n/)) {
    const line = rawLine.trim();
    if (!line) {
      continue;
    }
    try {
      rows.push(JSON.parse(line));
    } catch (error) {
      errors.push({ line: line.slice(0, 200), error: error.message });
    }
  }
  return { rows, errors };
}

function normalizeBucketName(row) {
  const raw = row.key || row.name || "";
  return String(raw).replace(/\/+$/, "");
}

function normalizeObjectKey(row) {
  return String(row.key || row.name || "").replace(/^\/+/, "");
}

function buildCredentialSetup(endpoint) {
  return `ENDPOINT=${shellSingleQuote(endpoint)}
U=\${MINIO_ROOT_USER:-\${MINIO_ACCESS_KEY:-}}
P=\${MINIO_ROOT_PASSWORD:-\${MINIO_SECRET_KEY:-}}
if [ -z "$U" ] || [ -z "$P" ]; then
  echo "missing minio root/access credential env" >&2
  exit 13
fi
mc alias set local "$ENDPOINT" "$U" "$P" >/dev/null
`;
}

function buildOverviewScript() {
  return `set -eu
emit() {
  name="$1"
  printf '%s\\n' "__ADP_BEGIN__\${name}"
  cat
  printf '\\n%s\\n' "__ADP_END__\${name}"
}
${buildCredentialSetup(minioEndpoint)}
(mc --version || true) | emit mc_version
(minio --version || true) | emit minio_version
printf '%s\\n' OK | emit alias_status
mc ls local --json | emit buckets_json
`;
}

function buildInventoryScript(buckets) {
  const bucketList = buckets.map(shellSingleQuote).join(" ");
  return `set -eu
emit() {
  name="$1"
  printf '%s\\n' "__ADP_BEGIN__\${name}"
  cat
  printf '\\n%s\\n' "__ADP_END__\${name}"
}
${buildCredentialSetup(minioEndpoint)}
for bucket in ${bucketList}; do
  printf '%s\\n' "__ADP_BEGIN__bucket:\${bucket}"
  set +e
  mc ls --recursive --json "local/\${bucket}"
  rc=$?
  set -e
  printf '\\n%s\\n' "__ADP_END__bucket:\${bucket}"
  printf '%s\\n' "$rc" | emit "bucket_rc:\${bucket}"
done
`;
}

function runRemote(script, label) {
  ensureSafeToken(minioContainer, "ADP_MINIO_CONTAINER");
  ensureSafeToken(sshConnectTimeout, "ADP_SSH_CONNECT_TIMEOUT");
  const result = spawnSync(
    "ssh",
    [
      "-o",
      "BatchMode=yes",
      "-o",
      `ConnectTimeout=${sshConnectTimeout}`,
      "-T",
      sshTarget,
      "docker",
      "exec",
      "-i",
      minioContainer,
      "sh",
      "-s",
    ],
    {
      input: script,
      encoding: "utf8",
      maxBuffer: 100 * 1024 * 1024,
    }
  );
  if (result.error) {
    throw result.error;
  }
  if (result.status !== 0) {
    throw new Error(`${label} remote command failed with status ${result.status}: ${result.stderr}`);
  }
  return parseDelimited(result.stdout);
}

function summarizeObjects(bucket, text, rcText) {
  const rc = Number(String(rcText || "").trim() || "1");
  const parsed = parseJsonLines(text);
  const objects = parsed.rows.filter((row) => row.status === "success" && normalizeObjectKey(row) && row.type !== "folder");
  const totalSizeBytes = objects.reduce((sum, row) => sum + Number(row.size || 0), 0);
  const lastModifiedValues = objects
    .map((row) => row.lastModified)
    .filter(Boolean)
    .sort();
  return {
    name: bucket,
    inventoryStatus: rc === 0 && parsed.errors.length === 0 ? "PASS" : "FAIL",
    commandExitCode: rc,
    objectCount: objects.length,
    totalSizeBytes,
    maxObjectSizeBytes: objects.reduce((max, row) => Math.max(max, Number(row.size || 0)), 0),
    earliestLastModified: lastModifiedValues[0] || "",
    latestLastModified: lastModifiedValues[lastModifiedValues.length - 1] || "",
    sampleObjectKeySha256Prefixes: objects
      .slice(0, 5)
      .map((row) => sha256(`${bucket}/${normalizeObjectKey(row)}`).slice(0, 16)),
    parseErrorCount: parsed.errors.length,
  };
}

function addCheck(checks, name, passed, evidence) {
  checks.push({
    name,
    status: passed ? "PASS" : "FAIL",
    evidence,
  });
}

function writeReport(report) {
  ensureDir(outputPath);
  fs.writeFileSync(outputPath, `${JSON.stringify(report, null, 2)}\n`);
}

function main() {
  ensureSafeToken(sshHost, "ADP_SSH_HOST");
  ensureSafeToken(sshUser, "ADP_SSH_USER");
  ensureSafeToken(minioEndpoint, "ADP_MINIO_ENDPOINT");
  for (const bucket of [...requestedBuckets, ...expectedBuckets]) {
    ensureSafeBucket(bucket);
  }

  const checks = [];
  let report;
  try {
    const overview = runRemote(buildOverviewScript(), "MinIO overview");
    const aliasStatus = overview.get("alias_status") || "";
    const bucketLines = parseJsonLines(overview.get("buckets_json") || "");
    const discoveredBuckets = bucketLines.rows
      .filter((row) => row.status === "success")
      .map(normalizeBucketName)
      .filter(Boolean)
      .sort();
    const bucketSet = new Set(discoveredBuckets);
    const bucketsToInspect = requestedBuckets.length > 0 ? requestedBuckets : discoveredBuckets;

    addCheck(checks, "ssh-target-reachable", true, `sshTarget=${sshUser}@${sshHost}`);
    addCheck(checks, "minio-container-reachable", true, `container=${minioContainer}`);
    addCheck(checks, "minio-alias-auth", aliasStatus.trim() === "OK", `endpoint=${minioEndpoint}; aliasStatus=${aliasStatus.trim()}`);
    addCheck(checks, "bucket-discovery", discoveredBuckets.length > 0 && bucketLines.errors.length === 0, `bucketCount=${discoveredBuckets.length}`);
    for (const expectedBucket of expectedBuckets) {
      addCheck(checks, `expected-bucket-${expectedBucket}`, bucketSet.has(expectedBucket), `expected=${expectedBucket}`);
    }

    const inventoryBlocks = bucketsToInspect.length > 0 ? runRemote(buildInventoryScript(bucketsToInspect), "MinIO bucket inventory") : new Map();
    const buckets = bucketsToInspect.map((bucket) =>
      summarizeObjects(bucket, inventoryBlocks.get(`bucket:${bucket}`) || "", inventoryBlocks.get(`bucket_rc:${bucket}`) || "1")
    );
    for (const bucket of buckets) {
      addCheck(
        checks,
        `bucket-inventory-${bucket.name}`,
        bucket.inventoryStatus === "PASS",
        `objects=${bucket.objectCount}; totalSizeBytes=${bucket.totalSizeBytes}; parseErrors=${bucket.parseErrorCount}`
      );
    }

    const failedChecks = checks.filter((check) => check.status !== "PASS");
    report = {
      schemaVersion: 1,
      generatedAt: new Date().toISOString(),
      repoCommit: getRepoCommit(),
      database: "PostgreSQL",
      environment: {
        sshHost,
        sshUser,
        minioContainer,
        minioEndpoint,
      },
      summary: {
        status: failedChecks.length === 0 ? "PASS" : "FAIL",
        totalChecks: checks.length,
        pass: checks.length - failedChecks.length,
        fail: failedChecks.length,
        bucketCount: discoveredBuckets.length,
        inspectedBucketCount: buckets.length,
        bucketsWithObjects: buckets.filter((bucket) => bucket.objectCount > 0).length,
        totalObjects: buckets.reduce((sum, bucket) => sum + bucket.objectCount, 0),
        totalSizeBytes: buckets.reduce((sum, bucket) => sum + bucket.totalSizeBytes, 0),
      },
      checks,
      buckets: buckets.map((bucket) => ({
        ...bucket,
        discoveredAtTopLevel: bucketSet.has(bucket.name),
      })),
      evidence: {
        method: "ssh docker exec minio mc ls --json and mc ls --recursive --json",
        secretHandling: "MinIO credentials were read from container environment only and are not written to this report.",
        mcVersion: (overview.get("mc_version") || "").split(/\n/)[0] || "",
        minioVersion: (overview.get("minio_version") || "").split(/\n/)[0] || "",
      },
    };
  } catch (error) {
    addCheck(checks, "minio-runtime-smoke", false, error.message);
    report = {
      schemaVersion: 1,
      generatedAt: new Date().toISOString(),
      repoCommit: getRepoCommit(),
      database: "PostgreSQL",
      environment: {
        sshHost,
        sshUser,
        minioContainer,
        minioEndpoint,
      },
      summary: {
        status: "FAIL",
        totalChecks: checks.length,
        pass: checks.filter((check) => check.status === "PASS").length,
        fail: checks.filter((check) => check.status !== "PASS").length,
        bucketCount: 0,
        inspectedBucketCount: 0,
        bucketsWithObjects: 0,
        totalObjects: 0,
        totalSizeBytes: 0,
      },
      checks,
      buckets: [],
      evidence: {
        method: "ssh docker exec minio mc ls --json and mc ls --recursive --json",
        secretHandling: "MinIO credentials were read from container environment only and are not written to this report.",
        error: error.stack || error.message,
      },
    };
  }

  writeReport(report);
  console.log(JSON.stringify(report.summary, null, 2));
  console.log(`MinIO runtime smoke report: ${outputPath}`);
  if (report.summary.status !== "PASS") {
    process.exitCode = 1;
  }
}

main();
