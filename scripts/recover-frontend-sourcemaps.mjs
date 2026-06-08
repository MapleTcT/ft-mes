#!/usr/bin/env node
import fs from "node:fs";
import path from "node:path";

const repoRoot = path.resolve(process.cwd());
const adpRoot = path.resolve(repoRoot, "..");
const staticRoot = path.join(adpRoot, "bap-server", "bap-workspace", "bap-static");
const outRoot = path.join(repoRoot, "frontend", "apps");
const metadataRoot = path.join(repoRoot, "metadata");

const skipPatterns = [
  /node_modules/i,
  /^\(webpack\)/,
  /webpack\/bootstrap/i,
  /webpack-runtime/i,
  /^multi /i,
  /^external /i,
  /dll-reference/i,
  /^webpack:\/{0,3}\/?buildin\//i,
];

function walk(dir, predicate, acc = []) {
  if (!fs.existsSync(dir)) return acc;
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const fullPath = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walk(fullPath, predicate, acc);
    } else if (predicate(fullPath)) {
      acc.push(fullPath);
    }
  }
  return acc;
}

function normalizeSourcePath(source) {
  let value = String(source || "").replaceAll("\\", "/");
  value = value.replace(/^webpack:\/{0,3}/, "");
  value = value.replace(/^\/+/, "");
  value = value.replace(/^\.\/+/, "");

  const appSrcIndex = value.indexOf("/src/");
  if (appSrcIndex >= 0) value = value.slice(appSrcIndex + 1);
  if (value.startsWith("src/") === false) {
    const srcIndex = value.indexOf("src/");
    if (srcIndex >= 0) value = value.slice(srcIndex);
  }

  value = value.replace(/^(\.?\/)+/, "");
  value = value.replace(/\?.*$/, (query) => {
    const safe = query.slice(1).replace(/[^A-Za-z0-9_.-]+/g, "_");
    return safe ? `.__${safe}` : "";
  });
  value = value.replace(/[:*?"<>|]/g, "_");
  value = value.replace(/\.\.(\/|$)/g, "");
  return value || "unknown-source.js";
}

function shouldSkipSource(source) {
  const clean = String(source || "").replace(/^webpack:\/{0,3}/, "");
  return skipPatterns.some((pattern) => pattern.test(clean));
}

function writeUnique(targetPath, content, seen) {
  const normalizedContent = content == null ? "" : String(content);
  const prior = seen.get(targetPath);
  if (prior === normalizedContent) return { written: false, duplicate: true, targetPath };

  let finalPath = targetPath;
  if (prior != null && prior !== normalizedContent) {
    const parsed = path.parse(targetPath);
    let index = 2;
    do {
      finalPath = path.join(parsed.dir, `${parsed.name}__variant${index}${parsed.ext}`);
      index += 1;
    } while (seen.has(finalPath));
  }

  fs.mkdirSync(path.dirname(finalPath), { recursive: true });
  fs.writeFileSync(finalPath, normalizedContent);
  seen.set(finalPath, normalizedContent);
  return { written: true, duplicate: false, targetPath: finalPath };
}

fs.mkdirSync(outRoot, { recursive: true });
fs.mkdirSync(metadataRoot, { recursive: true });

const maps = walk(staticRoot, (file) => file.endsWith(".map"));
const seen = new Map();
const byApp = new Map();
const recovered = [];
const failures = [];

for (const mapFile of maps) {
  const relMap = path.relative(staticRoot, mapFile).replaceAll("\\", "/");
  const app = relMap.split("/")[0] || "root";
  let mapJson;
  try {
    mapJson = JSON.parse(fs.readFileSync(mapFile, "utf8"));
  } catch (error) {
    failures.push({ map: relMap, error: error.message });
    continue;
  }

  const sources = Array.isArray(mapJson.sources) ? mapJson.sources : [];
  const sourcesContent = Array.isArray(mapJson.sourcesContent) ? mapJson.sourcesContent : [];
  const appStats = byApp.get(app) || {
    mapFiles: 0,
    sourceEntries: 0,
    recoveredFiles: 0,
    skippedEntries: 0,
    duplicateEntries: 0,
  };
  appStats.mapFiles += 1;
  appStats.sourceEntries += sources.length;

  for (let index = 0; index < sources.length; index += 1) {
    const source = sources[index];
    const content = sourcesContent[index];
    if (content == null || shouldSkipSource(source)) {
      appStats.skippedEntries += 1;
      continue;
    }

    const relativeSourcePath = normalizeSourcePath(source);
    const targetPath = path.join(outRoot, app, relativeSourcePath);
    const result = writeUnique(targetPath, content, seen);
    if (result.written) {
      appStats.recoveredFiles += 1;
      recovered.push({
        app,
        map: relMap,
        source,
        file: path.relative(repoRoot, result.targetPath).replaceAll("\\", "/"),
      });
    } else {
      appStats.duplicateEntries += 1;
    }
  }
  byApp.set(app, appStats);
}

const summary = {
  source: path.relative(repoRoot, staticRoot).replaceAll("\\", "/"),
  totalMapFiles: maps.length,
  recoveredFiles: recovered.length,
  failedMapFiles: failures.length,
  apps: Object.fromEntries([...byApp.entries()].sort(([a], [b]) => a.localeCompare(b))),
};

fs.writeFileSync(path.join(metadataRoot, "frontend-sourcemap-summary.json"), `${JSON.stringify(summary, null, 2)}\n`);
fs.writeFileSync(path.join(metadataRoot, "frontend-recovered-files.json"), `${JSON.stringify(recovered, null, 2)}\n`);
if (failures.length > 0) {
  fs.writeFileSync(path.join(metadataRoot, "frontend-sourcemap-failures.json"), `${JSON.stringify(failures, null, 2)}\n`);
}

console.log(JSON.stringify(summary, null, 2));
