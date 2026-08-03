#!/usr/bin/env node
"use strict";

const assert = require("assert");
const fs = require("fs");
const path = require("path");
const vm = require("vm");

const root = path.resolve(__dirname, "../../..");
const nginx = fs.readFileSync(path.join(root, "deploy/docker/nginx/adp.conf"), "utf8");
const assetPath = path.join(
  root,
  "deploy/docker/assets/module-static/LIMSSample/i18n-value.js"
);
const source = fs.readFileSync(assetPath, "utf8");
const sandbox = {
  window: {},
  document: { readyState: "complete", documentElement: null, body: null },
};
sandbox.window.window = sandbox.window;
sandbox.window.parent = sandbox.window;
sandbox.window.top = sandbox.window;
sandbox.window.document = sandbox.document;
sandbox.window.ReactAPI = {
  international: {
    getText: (key) => key,
    getLanguageObjData: () => ({}),
  },
};
vm.runInNewContext(source, sandbox, { filename: assetPath });

const resources = sandbox.window.InternationalResource;
assert(resources, "LIMSSample i18n asset did not create InternationalResource");
assert.strictEqual(resources["LIMSSample.viewtitle.randon1587435700996"], "样品登记");
assert.strictEqual(resources["LIMSSample.sample.openAnalySampleRef"], "参照样品模板");
assert.strictEqual(resources["EditView.operate.button.submit"], "提交");
assert.strictEqual(resources["LIMSBasic.viewtitle.randon1585804987375"], "样品模板参照");
assert(
  nginx.includes("location ~ ^/greenDill/static/LIMSSample/.*/i18n-value\\.js$"),
  "Nginx is missing the LIMSSample-specific i18n route"
);
assert(
  nginx.includes("alias /usr/share/nginx/module-static/LIMSSample/i18n-value.js;"),
  "Nginx LIMSSample route does not serve the shared resource"
);

console.log(`LIMSSample static i18n assets PASS (${Object.keys(resources).length} resources)`);
