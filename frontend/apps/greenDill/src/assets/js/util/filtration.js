import { isArray } from 'lodash';
import {GetRequest} from './common.js';

// 获取自定义参数
const getCustomCondition = () => {
  const { permissionCode: perCode, viewCode } = window.pageConfig || {};
  const customCon = { customCondition: {} };
  const param = GetRequest();
  if (param && param.customConditionKey) {
    const customKeys = param.customConditionKey.split(',');
    const condition = {};
    customKeys.forEach(key => {
      condition[`${key}`] = param[`${key}`];
    });
    customCon.customCondition = condition;
  }
  const isVaild = perCode && perCode !== null && perCode !== 'null';
  customCon.permissionCode = isVaild ? perCode : viewCode;
  return customCon;
};

// 筛选规则
function generateChainCond(cond, layarr, field) {
  if (layarr.length > 1) {
    var subconds = cond.subconds;
    for (var i = 0; i < subconds.length; i++) {
      if (subconds[i].type == "2" && subconds[i].joinInfo == layarr[0]) {
        generateChainCond(subconds[i], layarr.slice(1), field);
        return;
      }
    }
    var chain = {};
    chain.type = "2";
    chain.joinInfo = layarr[0];
    chain.subconds = [];
    cond.subconds.push(chain);
    generateChainCond(chain, layarr.slice(1), field);
  } else {
    cond.subconds.push(field);
  }
}

// 格式条件
function formatCondition(element, condobj, dataPost) {
  var fastCol = element.name;
  let fastColValue = element.value; //输入的值
  if (element.valueId && element.valueId != "") {
    fastColValue = element.valueId; //输入的值
    if(isArray(fastColValue)) fastColValue = fastColValue.join(',');
  }
  var fieldName = fastCol.replace(/_start$/, "").replace(/_end$/, "");
  if (
    fastColValue != null &&
    fastColValue != "undefined" &&
    fastColValue != ""
  ) {
    // if (ec_wupin_wupin_wupin_list_quickquery_info[fieldName]) {
    if (element) {
      var fieldObj = {};
      fieldObj.type = "0";
      fieldObj.columnName = element.columnName;
      fieldObj.dbColumnType = element.columnType;

      //解决兼容性，低版本安卓不支持startsWith和endsWith
      if (typeof String.prototype.startsWith !== "function") {
        String.prototype.startsWith = function(prefix) {
          return this.slice(0, prefix.length) === prefix;
        };
      }
      if (typeof String.prototype.endsWith !== "function") {
        String.prototype.endsWith = function(suffix) {
          return this.indexOf(suffix, this.length - suffix.length) !== -1;
        };
      }

      if (fastCol.endsWith("_start")) {
        var datatypeformat = element.datatypeformat;
        if (datatypeformat) {
          if (datatypeformat == "date") {
            fastColValue += " 00:00:00";
          } else if (datatypeformat == "year") {
            fastColValue += "-01-01 00:00:00";
          } else if (datatypeformat == "yearMonth") {
            fastColValue += "-01 00:00:00";
          } else if (datatypeformat == "datetimehm") {
            fastColValue += ":00";
          } else if (datatypeformat == "datetimeh") {
            fastColValue += ":00:00";
          }
        }
        var showFormat = element.showformat;
        if (showFormat === "PERCENT") {
          fastColValue = parseFloat(fastColValue) / 100 + "";
        }
        fieldObj.operator = ">=";
        fieldObj.paramStr = "?";
      } else if (fastCol.endsWith("_end")) {
        var datatypeformat = element.datatypeformat;
        if (datatypeformat) {
          if (datatypeformat == "date") {
            fastColValue += " 23:59:59";
          } else if (datatypeformat == "year") {
            fastColValue += "-12-31 23:59:59";
          } else if (datatypeformat == "yearMonth") {
            var str = fastColValue.split("-");
            var lastDay = new Date(str[0], str[1], 0).getDate();
            fastColValue += "-" + lastDay + " 23:59:59";
          } else if (datatypeformat == "datetimehm") {
            fastColValue += ":59";
          } else if (datatypeformat == "datetimeh") {
            fastColValue += ":59:59";
          }
        }
        var showFormat = element.showformat;
        if (showFormat === "PERCENT") {
          fastColValue = parseFloat(fastColValue) / 100 + "";
        }
        fieldObj.operator = "<=";
        fieldObj.paramStr = "?";
      } else {
        var lowerCheck = "";
        if (lowerCheck.length > 0 && lowerCheck.prop("checked")) {
          var layRec = element.layRec;
          var tableName = "";
          if (layRec.split(",").length > 1) {
            var level = layRec.split("-").length - 1;
            tableName = layRec.split("-")[level - 1].split(",")[0];
          } else {
            tableName =
              ec_wupin_wupin_wupin_list_quickquery_info["MainTableName"];
          }
          fieldObj.operator = "=includeCustSub#" + tableName;
          fieldObj.paramStr = "?";
        } else {
          var exp = element.exp;
          var caseSensitive = element.caseSensitive;
          if (exp == "equal") {
            fieldObj.operator = "=";
            fieldObj.paramStr = "?";
          } else if (exp == "unequal") {
            fieldObj.operator = "<>";
            fieldObj.paramStr = "?";
          } else if (exp == "llike") {
            fieldObj.operator = "like";
            if (caseSensitive == "true") {
              fieldObj.operator += "caseSensitive";
            }
            fieldObj.paramStr = "?%";
          } else if (exp == "rlike") {
            fieldObj.operator = "like";
            if (caseSensitive == "true") {
              fieldObj.operator += "caseSensitive";
            }
            fieldObj.paramStr = "%?";
          } else if (exp == "like") {
            fieldObj.operator = "like";
            if (caseSensitive == "true") {
              fieldObj.operator += "caseSensitive";
            }
            fieldObj.paramStr = "%?%";
          } else {
            fieldObj.operator = "=";
            fieldObj.paramStr = "?";
          }
        }
      }
      fieldObj.value = fastColValue;
      var layrec = element.layRec;
      if (layrec.indexOf("-") > 1) {
        generateChainCond(condobj, layrec.split("-"), fieldObj);
      } else {
        condobj.subconds.push(fieldObj);
      }
    } else {
      dataPost += "&" + fastCol + "=" + encodeURIComponent(fastColValue.trim());
    }
  }

  return {
    condobj: condobj,
    dataPost: dataPost
  };
}

// 快速查询
export default function(data, url, queryclassify) {
  var nowData = data;
  if (!nowData || nowData.length == 0) {
    return false;
  }
  var nodeParam = "";
  var dataPost = {};
  var condobj = {};

  if (window.pageConfig && window.pageConfig.viewCode) {
    condobj.viewCode = window.pageConfig.viewCode;
    condobj.modelAlias = window.pageConfig.modelAlias;
  } else {
    console.log("搜索未配置参数");
    return false;
  }

  condobj.condName = "fastCond";
  condobj.remark = "fastCond";
  condobj.subconds = [];
  // 数据
  // nowData.each(function(index) {
  for (var index = 0; index < nowData.length; index++) {
    var element = nowData[index];
    if (
      element.filterId &&
      (element.filterId.value == element.value ||
        element.filterId.value == element.valueId)
    ) {
      const t = element.filterId;
      let temp = formatCondition(t, condobj, dataPost);
      condobj = temp.condobj;
      dataPost = temp.dataPost;
    }
    let temp = formatCondition(element, condobj, dataPost);
    condobj = temp.condobj;
    dataPost = temp.dataPost;
  }
  console.log(dataPost)
  dataPost.fastQueryCond = JSON.stringify(condobj);
  dataPost.flowBulkFlag = false;
  // dataPost += "?fastQueryCond=" + encodeURIComponent(JSON.stringify(condobj));
  // dataPost += "&flowBulkFlag=false";

  if (queryclassify) {
    // dataPost += "&classifyCodes=" + queryclassify;
    dataPost.classifyCodes = queryclassify;
  }

  if (nodeParam != "") {
    if (url.indexOf(".action?") < 0) {
      url += "?1=1";
    }
    url += nodeParam;
  }

  // 清空上次拼接的url
  if (url && url.indexOf(".action?") > -1) {
    url = url.split("?")[0];
  }

  console.log(dataPost)

  return {
    url: url,
    data: {...dataPost,...getCustomCondition()}
  };
}



// WEBPACK FOOTER //
// ./src/assets/js/util/filtration.js