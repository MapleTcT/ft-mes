import { getIntl } from "@/assets/js/util/common.js";
// 表单值校验规则工具
export const getValidateRule = (data, compType) => {
  const { namekey, columnType, maxLength, precision } = data;
  const rule = [];
  const regExpList = {
    isNumber: /^\d+$/, // 只能是数字
    isInt: /^-?[0-9]\d*$/, // 只能是整数，可以有正负号和0
    isIntPlus: /^[1-9]\d*$/, // 正整数
    isIntMinus: /^-[1-9]\d*$/, // 负整数
    isFloat: /^-?([1-9]\d*\.\d*|0\.\d*[1-9]\d*|0?\.0+|0)$/, // 只能是浮点数（小数），可以有正负号
    isFloatPlus: /^-?\d+(\.\d+)?$/, // 浮点数,可以有正负号
    isFloatMinus: /^((-\d+(\.\d+)?)|(0+(\.0+)?))$/ // 非正浮点数
  };
  if (columnType === "INTEGER") {
    const regInfo = {
      pattern: regExpList.isInt,
      message: `${namekey}${getIntl("validate.tip.interger")}`
    };
    rule.push(regInfo);
  } else if (columnType === "MONEY") {
    const regInfo = {
      pattern: regExpList.isFloatPlus,
      message: `${namekey}${getIntl("validate.tip.incorrect")}`
    };
    rule.push(regInfo);
  } else if (columnType === "LONG") {
    const regInfo = {
      pattern: regExpList.isInt,
      message: `${namekey}${getIntl("validate.tip.long")}`
    };
    rule.push(regInfo);
  } else if (columnType === "DECIMAL") {
    let regInfo;
    if (precision === 0 || precision === undefined) {
      //小数位设置为0或者不设置
      regInfo = {
        pattern: regExpList.isInt,
        message: `${namekey}${getIntl("validate.tip.interger")}`
      };
    } else {
      regInfo = {
        pattern: regExpList.isFloat,
        message: `${namekey}${getIntl("validate.tip.decimal")}`
      };
    }
    rule.push(regInfo);
  }

  // 输入框组件开启长度验证
  const numberType = ["INTEGER", "LONG", "DECIMAL", "MONEY"];
  const maxValidateType = ["input", "text", "textfield", "longtext"];
  const isNumber = numberType.indexOf(columnType) > -1; // 是否为数字型
  if (maxLength && maxValidateType.indexOf(compType) > -1 && !isNumber)
    rule.push({
      max: maxLength,
      message: `${getIntl("validate.tip.length.exceed")}${namekey}${getIntl(
        "validate.tip.length.max"
      )}${maxLength}！`
    });

  // 数字区间校验
  if (isNumber) {
    const { minValue, maxValue } = data;
    if (minValue && maxValue) {
      rule.push({
        type: "number",
        transform: value => {
          return value && value !== null ? Number(value) : 0;
        },
        min: Number(minValue),
        max: Number(maxValue),
        message: `${namekey}${getIntl(
          "validate.tip.enter.range"
        )}${minValue}-${maxValue}`
      });
    }
  }

  return rule;
};

/**
 * 公用有效性验证
 * 验证字符、数字、长度、金额、日期的字段
 */
export const isInteger = thevalue => {
  const reg = /^[-+]?\d+$/;
  if (!reg.test(thevalue)) {
    return false;
  } else {
    if (thevalue >= -Math.pow(2, 31) && thevalue < Math.pow(2, 31)) {
      return true;
    } else {
      return false;
    }
  }
};

export const isLong = thevalue => {
  const reg = /^[-+]?\d+$/;
  if (!reg.test(thevalue)) {
    return false;
  } else {
    if (thevalue >= -Math.pow(2, 63) && thevalue < Math.pow(2, 63)) {
      return true;
    } else {
      return false;
    }
  }
};

export const isDecimal = thevalue => {
  const reg = /^[-+]?\d+\.*\d*$/;
  if (!reg.test(thevalue)) {
    return false;
  } else {
    return true;
  }
};

export const isDate = thevalue => {
  const reg = /^(\d{4})(-|\/)(\d{2})\2(\d{2})$/;
  if (!reg.test(thevalue)) {
    return false;
  } else {
    return true;
  }
  return true;
};

export const isDateTime = thevalue => {
  const reg = /^(?:(?:1[6-9]|[2-9]\d)?\d{2}[\-](?:0[1,3-9]|1[0-2])[\-](?:29|30))(?: (?:0\d|1\d|2[0-3]))?(?:\:(?:0\d|[1-5]\d))?(?:\:(?:0\d|[1-5]\d))?$|^(?:(?:1[6-9]|[2-9]\d)?\d{2}[\-](?:0[1,3,5,7,8]|1[02])[\-]31)(?: (?:0\d|1\d|2[0-3]))?(?:\:(?:0\d|[1-5]\d))?(?:\:(?:0\d|[1-5]\d))?$|^(?:(?:1[6-9]|[2-9]\d)?(?:0[48]|[2468][048]|[13579][26])[\-]02[\-]29)(?: (?:0\d|1\d|2[0-3]))?(?:\:(?:0\d|[1-5]\d))?(?:\:(?:0\d|[1-5]\d))?$|^(?:(?:16|[2468][048]|[3579][26])00[\-]02[\-]29)(?: (?:0\d|1\d|2[0-3]))?(?:\:(?:0\d|[1-5]\d))?(?:\:(?:0\d|[1-5]\d))?$|^(?:(?:1[6-9]|[2-9]\d)?\d{2}[\-](?:0[1-9]|1[0-2])[\-](?:0[1-9]|1\d|2[0-8]))(?: (?:0\d|1\d|2[0-3]))?(?:\:(?:0\d|[1-5]\d))?(?:\:(?:0\d|[1-5]\d))?$/;
  if (!reg.test(thevalue)) {
    return false;
  } else {
    return true;
  }
  return true;
};

export const checklong = (thevalue, checklen) => {
  var iLen = 0;
  var tmpValue = thevalue.replace(/^\s+|\s+$/g, "");
  for (var j = 0; j < tmpValue.length; j++) {
    iLen += tmpValue.charCodeAt(j) > 127 ? 3 : 1;
  }
  if (iLen > parseInt(checklen)) {
    return false;
  } else {
    return true;
  }
  return true;
};



// WEBPACK FOOTER //
// ./src/assets/js/util/validate.js