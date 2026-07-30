#!/usr/bin/env node
"use strict";

const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const test = require("node:test");

const root = path.resolve(__dirname, "../../..");
const nginxPath = path.join(root, "deploy/docker/nginx/adp.conf");
const pagePath = path.join(
  root,
  "backend/source-modules/wom-production-entry/src/main/resources/static/wom-manual-task-create.html"
);
const acceptancePath = path.join(
  root,
  "deploy/docker/scripts/adp-wom-manual-task-entry-persistence-acceptance.js"
);

function locationBody(source, declaration) {
  const escaped = declaration.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const match = source.match(new RegExp(`${escaped} \\{([\\s\\S]*?)\\n    \\}`));
  assert.ok(match, `Missing nginx location: ${declaration}`);
  return match[1];
}

test("manual entry document is public while all data APIs remain authenticated", () => {
  const nginx = fs.readFileSync(nginxPath, "utf8");
  const pageLocation = locationBody(
    nginx,
    "location = /msService/WOM/produceTask/manual-entry/page"
  );
  const apiLocation = locationBody(
    nginx,
    "location ^~ /msService/WOM/produceTask/manual-entry/"
  );

  assert.doesNotMatch(pageLocation, /auth_request/);
  assert.match(pageLocation, /proxy_set_header Authorization "";/);
  assert.match(pageLocation, /proxy_set_header Cookie "";/);
  assert.match(apiLocation, /auth_request \/_adp_wom_production_entry_auth;/);
  assert.match(apiLocation, /proxy_set_header Authorization \$http_authorization;/);
});

test("manual entry page forwards the existing browser login ticket", () => {
  const page = fs.readFileSync(pagePath, "utf8");

  assert.match(page, /localStorage\.getItem\("ticket"\)/);
  assert.match(page, /headers\.Authorization =/);
  assert.match(page, /requestOptions\.headers = requestHeaders/);
  assert.match(page, /response\.status === 401 \|\| response\.status === 403/);
});

test("acceptance runs without a synthetic browser-wide authorization header", () => {
  const acceptance = fs.readFileSync(acceptancePath, "utf8");

  assert.doesNotMatch(acceptance, /extraHTTPHeaders/);
  assert.match(acceptance, /context\.addInitScript/);
  assert.match(acceptance, /entryDocumentRequest\.hasAuthorization/);
  assert.match(acceptance, /optionsRequest\.hasAuthorization/);
});
