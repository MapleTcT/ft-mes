import { publicGetAxios } from "@/assets/js/util/common.js";

// 获取显示格式
export const getShowFormat = (value, showFormat) => {
  let formatValue = value;
  let regax = "";
  switch (showFormat) {
    case "THOUSAND":
      // 千分位
      regax = /(\d{1,3})(?=(\d{3})+(?:$|\.))/g;
      break;
    case "TEN_THOUSAND":
      // 万分位
      regax = /(\d{1,4})(?=(\d{4})+(?:$|\.))/g;
      break;
    case "PERCENT":
      // 百分比
      // formatValue = Number(formatValue * 100).toString();
      return formatValue;
    default:
      break;
  }
  if (regax) {
    const index = formatValue.indexOf(".");
    formatValue =
      index > -1
        ? formatValue
            .slice(0, index)
            .replace(regax, "$1,")
            .concat(formatValue.slice(index))
        : formatValue.replace(regax, "$1,");
  }

  return formatValue;
};

export const getFormatPrecision = (condition = {}, curVal) => {
  const { precision } = condition;
  let { value: formatvalue } = condition;
  if (curVal) formatvalue = curVal;
  formatvalue = formatvalue.toString().replace(/\s+/g, "");

  if (precision) {
    formatvalue = Number(formatvalue)
      .toFixed(Number(precision))
      .toString();
  }

  return formatvalue;
};



// WEBPACK FOOTER //
// ./src/components/cellComp/iInput/util.js