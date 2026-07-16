(function installPatrolResultJudge(root) {
  "use strict";

  if (root.__ADP_PATROL_RESULT_JUDGE_INSTALLED__) {
    return;
  }

  var NUMBER_VALUE_TYPE = "PATROL_valueType/number";
  var INPUT_EDIT_TYPE = "PATROL_editType/input";
  var NUMBER_PATTERN = "-?(?:\\d+(?:\\.\\d+)?|\\.\\d+)";

  function identifier(value) {
    if (value && typeof value === "object") {
      return value.id || value.value || value.code || "";
    }
    return value || "";
  }

  function normalizeExpression(expression) {
    return String(expression)
      .trim()
      .replace(/＞/g, ">")
      .replace(/＜/g, "<")
      .replace(/＝/g, "=")
      .replace(/≥/g, ">=")
      .replace(/≤/g, "<=");
  }

  function matchesNumericExpression(actualValue, expression) {
    var normalized = normalizeExpression(expression);
    var rangeMatch;
    var comparisonMatch;
    var lower;
    var upper;
    var expected;
    var operator;

    if (!normalized) {
      return false;
    }

    rangeMatch = normalized.match(
      new RegExp("^(" + NUMBER_PATTERN + ")\\s*[~～]\\s*(" + NUMBER_PATTERN + ")$")
    );
    if (rangeMatch) {
      lower = Number(rangeMatch[1]);
      upper = Number(rangeMatch[2]);
      return actualValue >= Math.min(lower, upper) && actualValue <= Math.max(lower, upper);
    }

    comparisonMatch = normalized.match(
      new RegExp("^(>=|<=|>|<|={1,2})?\\s*(" + NUMBER_PATTERN + ")$")
    );
    if (!comparisonMatch) {
      return false;
    }

    operator = comparisonMatch[1] || "=";
    expected = Number(comparisonMatch[2]);
    if (operator === ">=") {
      return actualValue >= expected;
    }
    if (operator === "<=") {
      return actualValue <= expected;
    }
    if (operator === ">") {
      return actualValue > expected;
    }
    if (operator === "<") {
      return actualValue < expected;
    }
    return actualValue === expected;
  }

  function judge(valType, editType, value, normalRange) {
    var actualValue;
    var expressions;
    var index;
    var rawValue;

    if (normalRange === null || normalRange === undefined) {
      return false;
    }

    if (identifier(valType) === NUMBER_VALUE_TYPE && identifier(editType) === INPUT_EDIT_TYPE) {
      rawValue = String(value === null || value === undefined ? "" : value).trim();
      if (!rawValue) {
        return false;
      }
      actualValue = Number(rawValue);
      if (!isFinite(actualValue)) {
        return false;
      }

      expressions = String(normalRange).split("|");
      for (index = 0; index < expressions.length; index += 1) {
        if (matchesNumericExpression(actualValue, expressions[index])) {
          return true;
        }
      }
      return false;
    }

    return String(value === null || value === undefined ? "" : value).trim() ===
      String(normalRange).trim();
  }

  root.judge = judge;
  root.__ADP_PATROL_RESULT_JUDGE_INSTALLED__ = true;
})(window);
