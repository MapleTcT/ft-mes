"use strict";

const assert = require("assert");
const fs = require("fs");
const path = require("path");

const dockerRoot = path.resolve(__dirname, "..");
const htmlPath = path.join(
  dockerRoot,
  "assets",
  "module-static",
  "PATROL",
  "patrolMonit",
  "patrol-monit-fallback.html"
);
const nginxPath = path.join(dockerRoot, "nginx", "adp.conf");

const html = fs.readFileSync(htmlPath, "utf8");
const nginx = fs.readFileSync(nginxPath, "utf8");
const inlineScript = html.match(/<script>([\s\S]*?)<\/script>/);

assert(inlineScript, "PATROL monitor fallback must include an inline runtime script");
assert(
  html.includes("SESGISConfig 服务端包尚未提供"),
  "PATROL monitor fallback must state the missing GIS dependency"
);
assert(
  html.includes('id="task-rows"') && html.includes('id="route-count"'),
  "PATROL monitor fallback must expose the task table and live metrics"
);
assert(
  html.includes("/msService/PATROL/patrolTask/potrolTask/selectData"),
  "PATROL monitor fallback must use the real PATROL task API"
);
assert(
  nginx.includes("location = /msService/SESGISConfig/themeConfig/themeLayers/index"),
  "Nginx must route the missing SESGISConfig entry to the operational fallback"
);

new Function(inlineScript[1]);

console.log("PATROL monitor fallback static acceptance: PASS");
