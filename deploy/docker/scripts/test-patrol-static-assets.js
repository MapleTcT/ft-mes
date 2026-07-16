"use strict";

const assert = require("assert");
const fs = require("fs");
const path = require("path");

const dockerRoot = path.resolve(__dirname, "..");
const assetRoot = path.join(
  dockerRoot,
  "assets",
  "module-static",
  "PATROL",
  "inputStandard",
  "inputStandard",
  "inputStanEdit"
);
const nginxPath = path.join(dockerRoot, "nginx", "adp.conf");
const nginx = fs.readFileSync(nginxPath, "utf8");

for (const fileName of ["body.js", "body-es5.js"]) {
  const assetPath = path.join(assetRoot, fileName);
  const source = fs.readFileSync(assetPath, "utf8");
  const requestPath = `/greenDill/static/PATROL/inputStandard/inputStandard/inputStanEdit/${fileName}`;
  const aliasPath = `/usr/share/nginx/module-static/PATROL/inputStandard/inputStandard/inputStanEdit/${fileName}`;

  assert(
    source.includes("function editOrValueChange(valType, editType, operateField)"),
    `${fileName} must restore the input-standard value/edit interaction`
  );
  assert(!/\bdebugger\s*;/.test(source), `${fileName} must not pause the browser debugger`);
  assert(nginx.includes(`location = ${requestPath} {`), `${fileName} must have an exact Nginx route`);
  assert(nginx.includes(`alias ${aliasPath};`), `${fileName} must map to the restored asset`);

  new Function(source);
}

const exactRouteIndex = nginx.indexOf(
  "location = /greenDill/static/PATROL/inputStandard/inputStandard/inputStanEdit/body.js"
);
const placeholderRouteIndex = nginx.indexOf(
  "location ~ ^/greenDill/static/.*/body(?:-es5)?\\.js$"
);
assert(exactRouteIndex >= 0, "PATROL input-standard exact route must exist");
assert(placeholderRouteIndex >= 0, "generic body-script fallback must exist");
assert(
  exactRouteIndex < placeholderRouteIndex,
  "PATROL input-standard exact routes must precede the generic body-script fallback"
);

console.log("PATROL static asset acceptance: PASS");
