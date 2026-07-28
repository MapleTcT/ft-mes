"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const {
  assertProfileMode,
  itemIsWithinLimits,
  parseQualityProfile,
  rangeDisplay,
} = require("./qcs-quality-profile");

const defaults = {
  marker: "ADP_E2E_PROFILE",
  reportItemCode: "DEFAULT_ITEM",
  reportItemName: "默认项目",
};

test("default profile preserves the existing one-item text result", () => {
  const profile = parseQualityProfile("", defaults);
  assert.equal(profile.items.length, 1);
  assert.equal(profile.items[0].result, "合格");
  assert.equal(profile.testOnlyDraft, false);
});

test("numeric draft profile validates ranges and qualified mode", () => {
  const profile = parseQualityProfile(
    JSON.stringify({
      profileCode: "FRU_SACCH_V1",
      name: "糖化液试运行标准",
      materialName: "糖化液",
      testOnlyDraft: true,
      items: [
        {
          code: "DE",
          name: "DE 值",
          unit: "%",
          minimum: 95,
          maximum: 98,
          result: 96.4,
        },
      ],
    }),
    defaults
  );
  assert.equal(itemIsWithinLimits(profile.items[0]), true);
  assert.equal(rangeDisplay(profile.items[0]), "[95, 98] %");
  assert.doesNotThrow(() => assertProfileMode(profile, "qualified"));
});

test("qualified profile rejects an out-of-range result", () => {
  const profile = parseQualityProfile(
    JSON.stringify({
      profileCode: "FRU_SACCH_V1",
      name: "糖化液试运行标准",
      testOnlyDraft: true,
      items: [
        {
          code: "PH",
          name: "pH",
          unit: "1",
          minimum: 5.4,
          maximum: 6.2,
          result: 6.8,
        },
      ],
    }),
    defaults
  );
  assert.throws(() => assertProfileMode(profile, "qualified"), /out-of-range/);
});

test("draft marker is mandatory for custom profiles", () => {
  assert.throws(
    () => parseQualityProfile(
      JSON.stringify({
        profileCode: "UNCONTROLLED",
        name: "未受控标准",
        items: [{ code: "PH", name: "pH", unit: "1", result: 5.8 }],
      }),
      defaults
    ),
    /testOnlyDraft=true/
  );
});
