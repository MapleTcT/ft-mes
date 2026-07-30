"use strict";

const assert = require("assert");
const fs = require("fs");
const path = require("path");

const dockerRoot = path.resolve(__dirname, "..");
const nginx = fs.readFileSync(path.join(dockerRoot, "nginx", "adp.conf"), "utf8");
const editors = ["flowEditH5-release", "flowEditH5"];
const locales = [
  ["zh_CN", "zh_cn"],
  ["zh_TW", "zh_tw"],
  ["en_US", "en_us"],
];

for (const editor of editors) {
  for (const [requestedLocale, bundledLocale] of locales) {
    const requestPath =
      `/bap/static/${editor}/js/${requestedLocale}/workflowInternational.js`;
    const bundledPath =
      `/usr/share/nginx/html/bap/static/${editor}/js/${bundledLocale}/workflowInternational.js`;

    assert(
      nginx.includes(`location = ${requestPath} {`),
      `missing workflow-editor locale route: ${requestPath}`
    );
    assert(
      nginx.includes(`alias ${bundledPath};`),
      `workflow-editor locale route must use bundled asset: ${bundledPath}`
    );
  }
}

console.log("workflow editor locale compatibility routes: PASS");
