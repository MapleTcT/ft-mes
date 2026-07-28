"use strict";

const MAX_PROFILE_ITEMS = 8;

function requireText(value, location) {
  if (typeof value !== "string" || !value.trim()) {
    throw new Error(`${location} must be a non-empty string`);
  }
  return value.trim();
}

function optionalNumber(value, location) {
  if (value === null || value === undefined || value === "") {
    return null;
  }
  const number = Number(value);
  if (!Number.isFinite(number)) {
    throw new Error(`${location} must be numeric`);
  }
  return number;
}

function normalizeItem(item, index) {
  const location = `items[${index}]`;
  const minimum = optionalNumber(item.minimum, `${location}.minimum`);
  const maximum = optionalNumber(item.maximum, `${location}.maximum`);
  const rawResult = requireText(String(item.result ?? ""), `${location}.result`);
  const numericCandidate = Number(rawResult);
  const numericResult = Number.isFinite(numericCandidate) ? numericCandidate : null;
  if ((minimum === null) !== (maximum === null)) {
    throw new Error(`${location} must define both minimum and maximum`);
  }
  if (minimum !== null && minimum > maximum) {
    throw new Error(`${location}.minimum cannot exceed maximum`);
  }
  return {
    code: requireText(item.code, `${location}.code`),
    name: requireText(item.name, `${location}.name`),
    unit: requireText(item.unit || "1", `${location}.unit`),
    result: rawResult,
    numericResult,
    minimum,
    maximum,
    precision: Number.isInteger(item.precision) && item.precision >= 0
      ? item.precision
      : 2,
    source: requireText(item.source || "LAB", `${location}.source`),
    required: item.required !== false,
  };
}

function parseQualityProfile(raw, defaults) {
  const fallback = {
    profileCode: `${defaults.marker}_DEFAULT`,
    name: `${defaults.marker} quality standard`,
    materialName: `${defaults.marker} material`,
    testOnlyDraft: false,
    items: [
      {
        code: defaults.reportItemCode,
        name: defaults.reportItemName,
        unit: "EA",
        result: "合格",
        source: "LAB",
      },
    ],
  };
  if (!raw) {
    return {
      ...fallback,
      items: fallback.items.map(normalizeItem),
    };
  }

  let parsed;
  try {
    parsed = JSON.parse(raw);
  } catch (error) {
    throw new Error(`ADP_QCS_PROFILE_JSON is invalid JSON: ${error.message}`);
  }
  if (parsed.testOnlyDraft !== true) {
    throw new Error("ADP_QCS_PROFILE_JSON must set testOnlyDraft=true");
  }
  if (!Array.isArray(parsed.items) || parsed.items.length < 1) {
    throw new Error("ADP_QCS_PROFILE_JSON.items must contain at least one item");
  }
  if (parsed.items.length > MAX_PROFILE_ITEMS) {
    throw new Error(`ADP_QCS_PROFILE_JSON.items supports at most ${MAX_PROFILE_ITEMS} items`);
  }
  const items = parsed.items.map(normalizeItem);
  const duplicateCodes = items
    .map((item) => item.code)
    .filter((code, index, all) => all.indexOf(code) !== index);
  const duplicateNames = items
    .map((item) => item.name)
    .filter((name, index, all) => all.indexOf(name) !== index);
  if (duplicateCodes.length || duplicateNames.length) {
    throw new Error("ADP_QCS_PROFILE_JSON item codes and names must be unique");
  }

  return {
    profileCode: requireText(parsed.profileCode, "profileCode"),
    name: requireText(parsed.name, "name"),
    materialName: requireText(parsed.materialName || fallback.materialName, "materialName"),
    testOnlyDraft: true,
    items,
  };
}

function itemIsWithinLimits(item) {
  if (item.minimum === null || item.maximum === null || item.numericResult === null) {
    return true;
  }
  return item.numericResult >= item.minimum && item.numericResult <= item.maximum;
}

function assertProfileMode(profile, mode) {
  const results = profile.items.map(itemIsWithinLimits);
  if (mode === "qualified" && results.some((qualified) => !qualified)) {
    throw new Error("qualified QCS profile contains an out-of-range result");
  }
  if (
    mode === "unqualified"
    && profile.items.some((item) => item.minimum !== null)
    && results.every(Boolean)
  ) {
    throw new Error("unqualified QCS profile requires at least one out-of-range numeric result");
  }
}

function rangeDisplay(item) {
  if (item.minimum === null || item.maximum === null) {
    return item.result;
  }
  return `[${item.minimum}, ${item.maximum}] ${item.unit}`;
}

function findProfileItem(profile, reportName) {
  return profile.items.find((item) => item.name === reportName) || null;
}

module.exports = {
  assertProfileMode,
  findProfileItem,
  itemIsWithinLimits,
  parseQualityProfile,
  rangeDisplay,
};
