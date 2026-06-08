import Axios from "axios"; //接口请求
import { Indicator, Toast } from "mint-ui";
import {
  publicGetAxios,
  tryCatch,
  getUrlConcat,
  getRequestParams,
  getIntl,
  getAuthentication,
  getLoginInfo,
} from "../util/common.js";
import interfaceURL from "../util/interface.js";
import { merge } from "lodash";

var commonJS = {};
//获取样式配置信息
commonJS.getCssStyle = function (config, element) {
  var cssInfo = {};
  if (element) {
    cssInfo["text-align"] = element.textalign; //对齐方式
    cssInfo["display"] = element.isHidden ? "none" : "block";
  }
  if (!config) return cssInfo;
  config = config.replace(/\ +/g, "");
  config = config.replace(/[\r\n]/g, "");
  var css = config.split(";");
  for (var i = 0; i < css.length; i++) {
    var cssItem = css[i].split(":");
    cssInfo[cssItem[0]] = cssItem[1];
  }
  return cssInfo;
};
//根据key查找对应的数值
commonJS.getValueByKey = function (data = {}, element, nameKey, isCallbackObj) {
  var key = element.key,
    getKeys,
    temp = "";
  var viewType = window.pageConfig && window.pageConfig.viewType;
  const viewArr = ["LIST", "REFERENCE"];

  // nameKey 区分列表和编辑数据
  if (nameKey) {
    key = nameKey;
  }

  // 合并自定义字段数据
  if (data.attrMap) {
    data = merge(data, data.attrMap);
  }

  if (
    !element.isCustom &&
    element.regionType !== "DATAGRID" &&
    !element.complex &&
    !viewArr.includes(viewType)
  ) {
    key = key.substr(key.indexOf(".") + 1); //截取.之后的字符
  }

  try {
    getKeys = key;
    const floatNumber = ["THOUSAND", "TEN_THOUSAND"];
    var dateType = ["DATE", "DATETIME"];
    var numberType = ["INTEGER", "LONG", "DECIMAL"];
    var moneyType = ["MONEY"];
    var systemCode = ["SYSTEMCODE"];
    var boolean = ["BOOLEAN"];
    var isDate = dateType.indexOf(element.columnType) > -1; // 日期型
    var isNumber = numberType.indexOf(element.columnType) > -1; // 数字型
    var isMoney = moneyType.indexOf(element.columnType) > -1; // 金额型
    var isSystemCode = systemCode.indexOf(element.columnType) > -1; // 系统编码
    var isBoolean = boolean.indexOf(element.columnType) > -1; // 布尔值
    // const isFloatNum = floatNumber.includes(element.showFormat); // 浮点数 千分位 万分位
    const isFloatNum = floatNumber.indexOf(element.showFormat) > -1; // 浮点数 千分位 万分位
    temp = eval("data." + getKeys);
    if (isBoolean) {
      //布尔型
      // if (!temp || temp == '') return "";// 值为空时
      // temp = temp ? "是" : "否";
      if (temp === true) {
        temp = getIntl("option.boolean.yes");
      } else if (temp === false) {
        temp = getIntl("option.boolean.no");
      } else {
        temp = "";
      }
    } else if (isDate && element.showFormat) {
      //日期型
      if (!temp || temp == "") return ""; // 值为空时
      if (element.columnType == "DATE" && element.showFormat == "SELECTCOMP") {
        element.showFormat = "YMD";
      }
      if (
        element.columnType == "DATETIME" &&
        element.showFormat == "SELECTCOMP"
      ) {
        element.showFormat = "YMD_HMS";
      }
      if (element.complex) {
        if (!isNaN(temp)) {
          temp = Number(temp);
        } else {
          temp = this.timeStrToTimeStamp(temp, element.showFormat);
        }
      }
      //时间格式化
      temp = this.getDateFormat(element.showFormat, new Date(temp));
    } else if (isNumber && !isFloatNum && !nameKey) {
      // 数字型
      temp = this.getNumberFormat(element.showFormat, temp, element.precision);
    } else if (isSystemCode) {
      // 系统编码
      if (!temp || temp == "") return ""; // 值为空时
      if (eval(`data.${getKeys}ForDisplay`))
        temp = eval(`data.${getKeys}ForDisplay`);
      if (element.isPrefer) {
        //参照系统编码
        if (temp && temp.value) {
          temp = temp.value;
        }
      } else {
        if (temp && temp.value) {
          //编辑视图的表头系统编码
          if (isCallbackObj) {
            temp = temp;
          } else {
            temp = temp.value;
          }
        } else if (temp && JSON.stringify(temp) == "{}") {
          temp = "";
        }
      }
    } else if ((isMoney || isFloatNum) && !nameKey) {
      // 金额
      if (temp || temp == 0 || temp == "0") {
        //解决金额为0时不显示的情况
        temp = this.formatMoney(element.showFormat, element.precision, temp);
      } else {
        //值为空时,不包含0
        return "";
      }
    }
  } catch (error) {
    // console.log(error);
  }

  if (temp === 0 || temp) return temp;
  return ''
};

commonJS.formatValue = function (data, element, nameKey, flag) {
  return this.getValueByKey(data, element, nameKey, flag);
};

commonJS.showFormat = function (data, element, index) {
  var value;
  if (element && element.showType && element.showType.indexOf("LABEL") > -1) {
    value = element.namekey;
  } else {
    value = this.getValueByKey(data, element);
  }

  // 系统编码赋值
  const slots = element.slots;
  if (slots && value) {
    const valTemp = value.split(",");
    const sysTemp = [];
    for (let i = 0; i < slots.length; i++) {
      const item = slots[i];
      for (let j = 0; j < valTemp.length; j++) {
        const el = valTemp[j];
        if (item.value === el) {
          sysTemp.push(item.label);
          break;
        }
      }
    }
    value = sysTemp.join(",");
  }

  // 是否是按照显示方法显示
  if (element.showFormatFunc) {
    let reset = new Function(
      `var value = '${value}';
            var nRow = ${index};
            ${element.showFormatFunc}
            `,
    )();
    return reset;
  } else {
    return value;
  }
};

//获取自定义事件绑定  TODO 多个事件绑定
commonJS.getEventBindConfig = function (data, isQuery, isKey) {
  var ev = [];
  if (!data) data = [];
  for (var i = 0; i < data.length; i++) {
    var item = data[i],
      code;

    if (isKey) {
      code = item.element ? item.element.key : "";
    } else {
      code = item.cellCode ? item.cellCode : "";
    }

    if (item.condition) {
      item = item.condition;
      code = item.name ? item.name : "";
    }

    if (!isQuery && item.element) {
      item = item.element;
    }
    if (item.funcname && item.funcbody) {
      // 解析多个事件和多个事件体
      let fnameArrys = item.funcname.split(")'");
      let fbodyArrys = item.funcbody.split("@@@@");
      for (let i = 0; i < fbodyArrys.length; i++) {
        let tempfname = fnameArrys[i];
        let tempfbody = fbodyArrys[i];
        tempfname = `${tempfname})'`;
        var ename = tempfname
          .replace(/\s/g, "")
          .split("=")[0]
          .split('"')
          .join(""); //获取事件名
        var fname = tempfname
          .replace(/\s/g, "")
          .split("=")[1]
          .split("'")
          .join(""); //获取函数名
        var fbody = tempfbody;
        ev.push({
          ename: ename,
          fname: fname,
          fbody: fbody,
          code: code,
        });
      }
    }

    if (item.callbackname && item.callbackbody) {
      //绑定callback事件,事件名为"callback"
      var cbfname = item.callbackname.split("(")[0]; //获取函数名
      ev.push({
        ename: "callback",
        fname: cbfname,
        fbody: item.callbackbody,
        code: code,
      });
    }
  }
  return ev;
};

//数字型单位转换
commonJS.getNumberFormat = function (fmt, value, precision) {
  var unit = { PERCENT: "%" };
  if (value) {
    if (precision) {
      if (fmt == "PERCENT") {
        value = Number(value * 100).toFixed(Number(precision));
      } else {
        value = Number(value).toFixed(Number(precision));
      }
    } else {
      if (fmt == "PERCENT") {
        value = Number(value * 100);
      } else {
        value = Number(value);
      }
    }
  }
  if (value == 0 || value == "0") {
    //判断数字为0的情况
    value = Number(value).toFixed(Number(precision));
  }
  if (value && fmt && unit[fmt]) {
    value = value + unit[fmt];
  }
  if (value == null) {
    value = "";
  }
  return value;
};
//金额格式化
commonJS.formatMoney = function (fmt, precision, decimal) {
  if (decimal || decimal == 0 || decimal == "0") {
    if (precision) {
      decimal = Number(decimal).toFixed(Number(precision));
    }
    if (Number(decimal)) {
      decimal = decimal.toString();
    }
  }
  decimal = decimal.replace(/\,/g, "");
  if (decimal != undefined && decimal != "") {
    var index = decimal.indexOf(".");
    var regax;
    if (fmt == "TEN_THOUSAND") {
      //万分位
      regax = /(\d{1,4})(?=(\d{4})+(?:$|\.))/g;
    } else {
      //千分位
      regax = /(\d{1,3})(?=(\d{3})+(?:$|\.))/g;
    }
    if (index === -1) {
      return decimal.replace(regax, "$1,");
    } else {
      return decimal
        .slice(0, index)
        .replace(regax, "$1,")
        .concat(decimal.slice(index));
    }
  } else {
    return decimal;
  }
};

//时间格式化
commonJS.getDateFormat = function (fmt, date) {
  if (!fmt || !date) {
    return "";
  }
  var transform = {
    YMD: "yyyy-MM-dd",
    YM: "yyyy-MM",
    Y: "yyyy",
    DEFAULT: "yyyy-MM-dd",
    YMD_HMS: "yyyy-MM-dd hh:mm:ss",
    YMD_HM: "yyyy-MM-dd hh:mm",
    YMD_H: "yyyy-MM-dd hh",
    MD_HM: "MM-dd hh:mm",
  };
  fmt = transform[fmt]; //获取转化格式
  return this.dateFtt(fmt, date);
};
//时间格式化
commonJS.dateFtt = function (fmt, date) {
  try {
    var o = {
      "M+": date.getMonth() + 1, //月份
      "d+": date.getDate(), //日
      "h+": date.getHours(), //小时
      "m+": date.getMinutes(), //分
      "s+": date.getSeconds(), //秒
      "q+": Math.floor((date.getMonth() + 3) / 3), //季度
      S: date.getMilliseconds(), //毫秒
    };
    if (/(y+)/.test(fmt))
      fmt = fmt.replace(
        RegExp.$1,
        (date.getFullYear() + "").substr(4 - RegExp.$1.length),
      );
    for (var k in o)
      if (new RegExp("(" + k + ")").test(fmt))
        fmt = fmt.replace(
          RegExp.$1,
          RegExp.$1.length == 1
            ? o[k]
            : ("00" + o[k]).substr(("" + o[k]).length),
        );
    return fmt;
  } catch (e) {
    console.log(e);
    return "";
  }
};

//时间字符串转时间戳
commonJS.timeStrToTimeStamp = function (date, showFormat) {
  date = date.substring(0, date.length);
  if (!date) return '';
  date = date.replace(/-/g, "/");
  if (showFormat === "YM") {
    date = date.replace(/-/g, "/") + "/01";
  } else if (showFormat === "YMD_H" || new Date(date) == "Invalid Date") {
    date = date.replace(/-/g, "/") + ":00";
  }
  var timestamp = new Date(date).getTime();
  return timestamp;
};

// 拼接参照url
commonJS.resolveReferUrl = function (url, data) {
  let refer = data.referenceview;
  let multiSelect = data.multable ? 1 : 0; // 默认单选
  let code = "";
  let clientType = "";
  let hideWebTitle = "";
  let urlParams = vue.GetRequest(window.location.href); // 获取地址参数
  const { refCondition, refConditionAPI } = data; // 参照自定义条件
  let conditionKey = [];
  let conditionParams = "";
  if (refer.code) code = refer.code.replace(/\./gi, "_");
  if (urlParams && urlParams.clientType) {
    clientType = urlParams.clientType;
  }
  if (urlParams && urlParams.hideWebTitle) {
    hideWebTitle = urlParams.hideWebTitle;
  }
  // dev参照用
  if (process.env.NODE_ENV !== "production") {
    url = "./itemList.html";
    // url = "./staff.html";
    // url = "./position.html";
    // url = "./department.html";
    // url = `http://192.168.91.64:8080${url}`;
    // url='http://192.168.91.63:8080/greenDill/mobile-static/staff.html#/'
  }

  if (url.indexOf("?") === -1) {
    url = `${url}?`;
  }

  // 参照自定义条件
  if (refCondition) {
    try {
      const refConf = new Function(refCondition)();
      conditionParams = encodeURI(refConf); // 参数转码处理
      const condition = vue.GetRequest(`?${conditionParams}`);
      conditionKey = conditionKey.concat(Object.keys(condition));
    } catch (error) {
      console.error(error.message);
    }
  }

  // 通过API赋值
  if (refConditionAPI) {
    conditionKey = conditionKey.concat(Object.keys(refConditionAPI));
    conditionParams += `${conditionParams ? "&" : ""}${getUrlConcat(
      refConditionAPI,
    )}`;
  }

  if (conditionParams) {
    console.log(conditionParams);
    url = `${url}&${conditionParams}&customConditionKey=${conditionKey.join(
      ",",
    )}&`;
  }

  url = `${url}isRefer=1&crossCompanyFlag=${refer.iscrosscompany}&condition=&callBackFuncName=_callBack_ec_${code}&multiSelect=${multiSelect}&closeFuncName=_close_ec_${code}&clientType=${clientType}&hideWebTitle=${hideWebTitle}&moduleType=Mis`;
  return url;
};

// 构造筛选参照回填值-查询
commonJS.referCallBackObject = function (list, condition) {
  // 取最后的值
  let keyName = condition[0].name.split(".");
  keyName = keyName[keyName.length - 1];
  // 构造Id对象
  if (condition[0].filterId) {
    delete condition[0].filterId;
  }
  // condition[0].filterId = JSON.parse(JSON.stringify(condition[0]));
  condition[0].filterId = Object.assign({}, condition[0]);

  if (condition[0].asscolumnname) {
    condition[0].filterId.columnName = condition[0].asscolumnname;
  } else {
    condition[0].filterId.columnName = "ID";
  }
  condition[0].filterId.columnType = "LONG";
  condition[0].filterId.type = "0";
  condition[0].filterId.exp = "equal";
  // 回调赋值
  if (list.length > 0) {
    let arr = [],
      ids = [];
    for (let i = 0; i < list.length; i++) {
      const element = list[i];
      arr.push(element[keyName]);
      ids.push(element.id);
    }
    condition[0].value = arr.join(",");
    condition[0].filterId.valueId = ids.join(",");
  } else {
    condition[0].value = list[keyName];
    condition[0].filterId.valueId = list.id.toString();
  }
  condition[0].filterId.value = condition[0].valueId
    ? condition[0].valueId
    : condition[0].value;

  return condition;
};

// 处理图片字段
const getPreviewPic = (element = {}, objThis = {}, nameKey) => {
  // const isBlob = value.every(
  //   item => item.url.indexOf('blob:') === 0
  // );
  const { valueId: imgId } = element;
  const { interfaceApi } = window.pageConfig || {};
  const { PreviewPicture } = interfaceURL;
  const { entityCode = "" } = objThis.urlParams;
  const url = `${PreviewPicture}?id=${imgId}&type=picture&entityCode=${entityCode}`;
  const isBlob = url.indexOf("blob:") === 0;
  if (!isBlob) {
    const { methodType, serverName, url: authUrl } = getAuthentication();
    const newList = [];
    const transTask = [];
    const req = (() => {
      return publicGetAxios(
        { url, type: "get", data: { methodType, serverName, url: authUrl } },
        res => {
          const { data } = res;
          const task = dataURLtoBlob(
            `data:image/png;base64,${data.image}`,
          ).then(blob => {
            newList.push(URL.createObjectURL(blob));
          });
          transTask.push(task);
        },
      );
    })();
    Promise.all([req]).then(() =>
      Promise.all(transTask).then(() => {
        element.value = newList[0];
        element.valueId = imgId;
        if (objThis.$refs[nameKey])
          objThis.$refs[nameKey][0].imgList = [element.value];
        element.loadValue = element.value;
      }),
    );
  } else {
  }
};

// base64转blob
const dataURLtoBlob = b64 => {
  if (!HTMLCanvasElement.prototype.toBlob) {
    Object.defineProperty(HTMLCanvasElement.prototype, "toBlob", {
      value(callback, type, quality) {
        const canvas = this;
        setTimeout(() => {
          const binStr = atob(canvas.toDataURL(type, quality).split(",")[1]);
          const len = binStr.length;
          const arr = new Uint8Array(len);
          for (let i = 0; i < len; i += 1) {
            arr[i] = binStr.charCodeAt(i);
          }
          callback(new Blob([arr], { type: type || "image/png" }));
        });
      },
    });
  }
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.onerror = reject;
    img.onload = function onload() {
      const canvas = document.createElement("canvas");
      canvas.width = img.width;
      canvas.height = img.height;
      const ctx = canvas.getContext("2d");
      ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
      canvas.toBlob(resolve);
    };
    img.src = b64;
  });
};

/**
 * 根据key查找对应的数值-表单
 * @params {object} element
 * @params {object} newData 数据
 * @params {object} objThis vue组件对象(this)
 * @params {boolean} isPreferRefer 是否参照类型
 */
commonJS.getValueByKeyEdit = function (
  element,
  newData,
  objThis = {},
  isPreferRefer,
) {
  const $this = this;
  let data = objThis.editData;

  if (newData) {
    data = newData;
  }

  // 多选下拉赋值，系统编码树形多选赋值
  if (element && element.slotsAll) {
    element.slots = element.slotsAll;
  }

  if (element && element.name && data) {
    let nameKey = "";
    const { columnType } = element;
    nameKey = element.name;
    // 参照回填
    if (isPreferRefer) {
      // const arr = element.name.split(".");
      // nameKey = arr[arr.length - 1];
      nameKey = nameKey.substr(nameKey.indexOf(".") + 1); //截取.之后的字符
    }

    element.value = $this.formatValue(
      data,
      element,
      nameKey,
      element.columnType == "SYSTEMCODE" ? false : true,
    );

    // element.value = element.value ? element.value.toString() : '';
    if (element.value || element.value === 0 || element.value === "0") {
      //解决数据为0时不显示的问题
      element.value = element.value.toString();
    } else {
      element.value = "";
    }
    if (!isPreferRefer) {
      element.loadValue = element.value;
    }

    // 参照
    if (element.isPrefer) {
      if (columnType === "MULTSELECT") {
        let muiltiNames = this.formatValue(
          data,
          element,
          `${nameKey}multiselectNames`,
        );
        let muiltiIds = this.formatValue(
          data,
          element,
          `${nameKey}multiselectIDs`,
        );
        element.oldMultiLists = {
          values: muiltiNames && muiltiNames.split(","),
          ids: muiltiIds && muiltiIds.split(","),
        };
        element.newMultiLists = {
          values: muiltiNames && muiltiNames.split(","),
          ids: muiltiIds && muiltiIds.split(","),
        };
      } else {
        let index = nameKey.lastIndexOf(".");
        // 对表格处理
        if (element.regionType === "DATAGRID") index = nameKey.indexOf(".");
        let idName;
        if (index > -1) {
          idName = nameKey.substring(0, index);
          idName = `${idName}.id`;
        } else {
          idName = `id`;
        }
        if (isPreferRefer) idName = `id`; // 参照关联id
        element.valueId = this.formatValue(data, element, idName);
      }
      return;
    }

    //数据为金额类型
    if (element.value && element.columnType == "MONEY") {
      return;
    }

    // 数据为对象型
    if (element.value && element.value.id) {
      let { id, value } = element.value;
      element.valueId = id;
      element.value = value;
      element.loadValue = element.value;
      return;
    }

    // 小数 精度显示类型
    if (element.precision || element.columnType == "DECIMAL") {
      return;
    }

    // 整型和长整型类型
    if (element.columnType == "INTEGER" || element.columnType == "LONG") {
      return;
    }

    // complex 表单元素
    if (element.complex) {
      let tempVid = $this.formatValue(
        data,
        element,
        `extraColMap._complex_${nameKey}`,
      );
      if (element.multable && tempVid && element.slots && element.slots[0]) {
        // 多选
        element.valueId = tempVid;
        let tempValueId = element.valueId.split(",");
        let tempValue = [],
          tempIds = [];
        for (let i = 0; i < tempValueId.length; i++) {
          const et = tempValueId[i];
          for (let j = 0; j < element.slots.length; j++) {
            const el = element.slots[j];
            if (el.value == et) {
              tempValue.push(el.label);
              tempIds.push(el.value);
            }
          }
        }
        element.value = tempValue;
        element.valueId = tempIds;
      } else if (
        tempVid &&
        element.slots &&
        element.slots[0] &&
        element.slots[0].valueKey
      ) {
        // 单选
        element.valueId = tempVid;
        let valueIndex = element.slots[0].valueKey.indexOf(element.valueId);
        element.value = element.slots[0].values[valueIndex];
      } else {
        element.value = tempVid;
      }
      // element.value = $this.formatValue(data, element, `extraColMap._complex_${nameKey}`);
      element.loadValue = element.value;
      return;
    }

    // 时间类型
    if (element.type == "date") {
      return;
    }

    // 图片
    if (element.type == "picture") {
      let imgId = $this.formatValue(data, element, `${nameKey}Document.id`);
      let entityCode = objThis.urlParams ? objThis.urlParams.entityCode : "";
      if (imgId && entityCode) {
        element.valueId = imgId;
        getPreviewPic(element, objThis, nameKey);
      }
      return;
    }

    // 表体的附件
    if (element.type == "attachment" && element.datagridCode) {
      let attachmentName = element.name;
      let attachmentId = $this.formatValue(
        data,
        element,
        `${attachmentName}MultiFileIds`,
      );
      let fileName = $this.formatValue(
        data,
        element,
        `${attachmentName}MultiFileNames`,
      );
      let fileType = $this.formatValue(
        data,
        element,
        `${attachmentName}MultiFileIcons`,
      );

      if (attachmentId && fileName) {
        element.valueId = attachmentId;
        element.value = fileName;

        element.multiFile[`${attachmentName}MultiFileIds`] = element.valueId;
        element.multiFile[`${attachmentName}MultiFileNames`] = element.value;
        element.multiFile[`${attachmentName}MultiFileIcons`] = fileType;

        element.loadValue = element.value;
      }

      return;
    }
  }
};

// 参照默认值
const getRefDefalutVal = async (key, obj = {}) => {
  const loginInfo = await getLoginInfo() || {};
  const { name: lackValue } = obj;
  let tempName = lackValue.split('.');
  tempName = tempName[tempName.length - 1];
  const matchingType = {
    currentUser:'staff',
    currentComp:'company',
    currentDepart:'department',
    currentPosition:'mainPosition',
    currentPost:'mainPosition',
  }
  const curVal = loginInfo[matchingType[key]] || {};
  obj.value = curVal[tempName];
  obj.valueId = curVal.id;
}

// 获取默认值展示
commonJS.showDefaultValue = function (element, isList = false) {
  const { defaultValue, columnType, regionType, isloadData } = element;
  const urlParams = getRequestParams() || {};
  const hasDefaultVal = defaultValue || typeof defaultValue === "boolean";
  // 表格非新增也支持默认值
  if (
    ((regionType === "DATAGRID" && !isloadData) || !urlParams.id) &&
    hasDefaultVal
  ) {
    if (columnType == "SYSTEMCODE") {
      element.valueId = defaultValue;
    } else if (columnType === "BOOLEAN") {
      element.value =
        defaultValue === true
          ? getIntl("option.boolean.yes")
          : getIntl("option.boolean.no");
      element.valueId = !isList ? defaultValue : defaultValue ? '1' : '0';
    } else if(['currentUser','currentComp','currentDepart','currentPosition','currentPost'].includes(defaultValue)){
      getRefDefalutVal(defaultValue,element);
    }else {
      element.value = !isList ? defaultValue : defaultValue.toString();
    }
  }
};

/**
 * 判断当前安卓手机版本是否小于等于某个版本号
 * @params  [version][需要比较的版本号]
 * @returns
 */
commonJS.isLessEqualAndroidVersion = function (version) {
  var u = window.navigator.userAgent;
  var isAndroid = u.indexOf("Android") > -1 || u.indexOf("Adr") > -1;
  if (!isAndroid) {
    return false;
  }
  var androidVersionRes = u.match(/Android (\d+).* /);
  return androidVersionRes && androidVersionRes[1]
    ? +androidVersionRes[1] <= version
    : false;
};

/**
 * 判断当前设备是安卓还是ios
 * @returns
 */
commonJS.judgePhoneType = function () {
  let ua = window.navigator.userAgent,
    app = window.navigator.appVersion,
    phoneType;
  if (!!ua.match(/\(i[^;]+;( U;)? CPU.+Mac OS X/)) {
    // ios端
    phoneType = "ios";
  } else if (ua.indexOf("Android") > -1 || ua.indexOf("Adr") > -1) {
    // android端
    phoneType = "android";
  }
  return phoneType;
};

/**
 * 编辑视图的显示下拉框
 * @params {object} vThis vue组件对象(this)
 * @params {object} data 字段数据对象
 */
commonJS.showDropdown = function (vThis, data) {
  if (data.readonly) {
    return false;
  }
  data.isShowPicker = true;
  if (!data.multable) {
    vThis.$nextTick(function () {
      let name = vThis.params.name;
      let value = vThis.params.value;
      if (value) {
        vThis.$refs[name] &&
          vThis.$refs[name].$refs["pick_" + name] &&
          vThis.$refs[name].$refs["pick_" + name].setValues([value]);
      }
    });
  }
};

/**
 * 列表视图的显示下拉框
 * @params {object} vThis vue组件对象(this)
 * @params {object} data 字段数据对象
 */
commonJS.showDropdownList = function (vThis, data) {
  if (data.readonly && data.placeholder == "") {
    //配置页面设置只读
    return false;
  }
  if (
    (!data.slots || (data.slots && data.slots.length == 0)) &&
    data.systemCode &&
    data.systemCode.fillContent
  ) {
    let url;
    if (window.pageConfig) {
      url = window.pageConfig.baseServiceApi.systemCodeJson;
    }
    publicGetAxios(
      {
        url: url,
        type: "get",
        data: {
          systemEntityCode: data.systemCode.fillContent,
        },
      },
      function (result) {
        let resData = result.data;
        let arr = [""],
          keys = [""];
        for (const key in resData) {
          if (resData.hasOwnProperty(key)) {
            const element = resData[key];
            arr.push(element);
            keys.push(key);
          }
        }
        data.selectData = keys;
        data.slots = [
          {
            flex: 1,
            valueIndex: 0,
            defaultIndex: 0,
            value: arr[0],
            valueKey: 0,
            values: arr,
          },
        ];
        data.isShowPicker = true;
      },
    );
  } else {
    data.isShowPicker = true;
  }
};

/**
 * 打开附件列表界面
 * @params {object} vThis vue组件对象(this)
 */
commonJS.openAppendixListInterface = function (vThis) {
  if (vue.appendixList && vue.appendixList.length > 0) {
    vThis.$refs.appendix.appendixList = vue.appendixList; //附件信息
  }
  vThis.popupVisible = false; //关闭按钮弹窗
  vThis.$refs.appendix.isShowAppendix = true; //显示附件页面
  vThis.$forceUpdate();
};

/**
 * 获取附件个数
 * @params {object} vThis vue组件对象(this)
 */
commonJS.getAppendixNums = function (vThis) {
  let tableInfoId;
  let workflowEnabled = window.pageConfig && window.pageConfig.workflowEnabled;
  if (workflowEnabled) {
    if (vThis.urlParams && vThis.urlParams.tableInfoId) {
      tableInfoId = vThis.urlParams.tableInfoId;
    } else {
      tableInfoId = vThis.$root.$children[0].$refs.editForm.tableInfoId;
    }
  } else {
    tableInfoId = vThis.urlParams.id;
  }

  let linkId = tableInfoId ? tableInfoId : "";
  let modelCode = window.pageConfig && window.pageConfig.modelCode;
  let type = this.getUploadType(workflowEnabled, modelCode);
  const {
    prefix,
    viewCode,
    interfaceApi: { countFile },
  } = window.pageConfig || {};
  var url =`${prefix}${countFile}?linkId=${linkId}&type=${type}&viewCode=${viewCode}`;
  publicGetAxios(
    {
      url: url,
      type: "get"
    },
    function(result) {
      vThis.AppendixNum = result.data;
    }
  );
};

/**
 * 获取所有的兄弟节点
 * @params {object} elm 目标节点
 * @returns 返回兄弟节点
 */
commonJS.getSibling = function (elm) {
  var siblings = [];
  var pChild = elm.parentNode.children;
  for (var i = 0; i < pChild.length; i++) {
    if (pChild[i] !== elm) {
      siblings.push(pChild[i]);
    }
  }
  return siblings;
};

/**
 * 查询是否重复的文件
 * @params files 文件
 * @params {Array} filesList 文件列表
 * @returns 返回是否存在重复的文件
 */
commonJS.isRepeatFile = function (files, filesList) {
  let isrepeatfile = false;
  for (let i = 0; i < filesList.length; i++) {
    if (filesList[i].name == files.name) {
      isrepeatfile = true;
    }
  }
  return isrepeatfile;
};

/**
 * 返回重复的文件的文件名
 * @params files 文件
 * @params {Array} filesList 文件列表
 * @returns 返回重复的文件的文件名
 */
commonJS.repeatFileName = function (files, filesList) {
  let repeatFileName = "";
  for (let i = 0; i < filesList.length; i++) {
    if (filesList[i].name == files.name) {
      repeatFileName = filesList[i].name;
    }
  }
  return repeatFileName;
};

/**
 * 获取附件类型type，非工作流单据附件上传type参数为模块名_实体名_模型名
 * @params {boolean} workflowEnabled 是否启用工作流
 * @params {string} modelCode 模型编码
 * @returns 返回附件类型type
 */
commonJS.getUploadType = function (workflowEnabled, modelCode) {
  const code = modelCode.split("_");
  code.splice(1, 1);
  return workflowEnabled ? "Table" : code.join("_");
};

/**
 * 点击附件信息打开下拉框
 * @params {int} id 附件id
 * @params {object} event
 * @params {int} num 子元素的位数
 */
commonJS.openAppendixDropdown = function (id, event, num) {
  let curTargetElm = event.currentTarget;
  let btnSiblings;
  let curTBtn;
  if (curTargetElm.id == "appendix_" + id) {
    btnSiblings = this.getSibling(curTargetElm);
    curTBtn = curTargetElm.children[num];
    for (let i = 0; i < btnSiblings.length; i++) {
      if (btnSiblings[i].children.length > num) {
        btnSiblings[i].children[num].style.display = "none";
      }
    }
    if (curTBtn.style.display == "block") {
      curTBtn.style.display = "none";
    } else {
      curTBtn.style.display = "block";
    }
  }
};

let curConvertTime;
let converFlag = 0;
let retryTimes = 0;

// 判断附件是否转换成功
const isFileSuccessTrans = (vThis, fileObj = {}, isPicture) => {
  const { id: fileId, name } = fileObj;
  if (!vThis.isShowAppendix) {
    converFlag = 0;
    retryTimes = 0;
    return;
  }
  let urlParams = getRequestParams(window.location.href); // 获取地址参数
  let entityCode = urlParams ? urlParams.entityCode : "";
  const { methodType, serverName, url: authUrl } = getAuthentication();
  publicGetAxios(
    {
      url: interfaceURL.checkPreviewStatus,
      type: "get",
      data: {
        id: fileId,
        entityCode,
        methodType,
        serverName,
        url: authUrl,
        address: window.location.origin
      },
    },
    res => {
      const {
        data: { previewUrl, status },
        message
      } = res;
      switch (status) {
        case "true": // 转换成功
          vue.updateAppendix(true, previewUrl, fileId, name);
          Indicator.open({
            text: getIntl("notice.loading"), //文字
            spinnerType: "fading-circle", //样式
          });
          // 回调方法;
          vThis.$nextTick(function () {
            let iframe = document.getElementById("newPage");
            iframe.onload = function () {
              //处理图片附件的显示样式
              if (iframe && isPicture) {
                let contentWindow = iframe.contentWindow;
                let body = contentWindow.document.getElementsByTagName(
                  "body",
                )[0];
                let img = contentWindow.document.getElementsByTagName("img")[0];
                body.style.position = "relative";
                body.style.background = "rgb(14, 14, 14)";
                img.style.maxWidth = "100%";
                img.style.maxHeight = "100%";
                img.style.position = "absolute";
                img.style.top = "0";
                img.style.left = "0";
                img.style.right = "0";
                img.style.bottom = "0";
                img.style.margin = "auto";
              }

              Indicator.close();
            };
          });
          break;
        case "false": // 转换失败
          Toast({
            message: message || getIntl("notice.convert.fail"),
            iconClass: "mintui mintui-error"
          });
          break;
        case "convert": // 转换中
          const curTime = new Date().getTime();
          if (converFlag == 0) curConvertTime = new Date().getTime();
          if (curTime - curConvertTime < 30 * 1000) {
            // 大于30s 转换失败
            setTimeout(
              () => isFileSuccessTrans(vThis, fileObj, isPicture),
              4000,
            );
            Indicator.close();
            Indicator.open({
              text: getIntl("notice.converting"), //文字
              spinnerType: "fading-circle", //样式
            });
            converFlag++;
          } else {
            Indicator.close();
            Toast({
              message: getIntl("notice.convert.timeout"),
              iconClass: "mintui mintui-error",
            });
            converFlag = 0;
          }
          break;
        case "retry": // 异常，重试
          if (retryTimes <= 2) {
            isFileSuccessTrans(vThis, fileObj, isPicture);
            retryTimes++;
          } else {
            Toast({
              message: getIntl("notice.convert.retry"),
              iconClass: "mintui mintui-error",
            });
            retryTimes = 0;
          }

          break;

        default:
          break;
      }
    },
  );
};

/**
 * 预览附件
 * @params {object} vThis vue组件对象(this)
 * @params {Array} appendixlist 附件id
 * @params {object} event
 */
commonJS.previewAppendixs = function (vThis, appendixlist, event) {
  event.stopPropagation();
  if (this.judgePreviewAppendixs(appendixlist.name)) {
    //支持预览
    let previewAppendixType = [
      "doc",
      "docx",
      "txt",
      "log",
      "xls",
      "xlsx",
      "ppt",
      "pptx",
      "pdf",
    ]; //需要转成pdf的文件格式
    const picType = ['png', 'jpeg', 'jpg', 'gif'];
    let type = appendixlist.name
      .substring(
        appendixlist.name.lastIndexOf(".") + 1,
        appendixlist.name.length,
      )
      .toLowerCase();
    const { PreviewAttachments } = window.pageConfig.interfaceApi;
    const { prefix } = window.pageConfig;
    let previewFile = `${prefix}${PreviewAttachments}`;
    // let previewFile = `/msService/expense/baseService/workbench/preview`; // 修改模型名
    let previewFileUrl;
    let isPicture;
    let size = appendixlist && appendixlist.sizeDis;

    if (size) {
      if (size.indexOf("M") > -1 || size.indexOf("G") > -1) {
        if (size.indexOf("M") > -1) {
          size = Number(size.replace("M", ""));
        } else if (size.indexOf("G") > -1) {
          size = size.replace("G", "");
          size = Number(size) * 1024;
        }
        let maxSize;
        if (type == "xls" || type == "xlsx") {
          maxSize = 10;
        } else {
          maxSize = 100;
        }
        if (size >= maxSize) {
          //不支持预览
          // Toast({
          //   message: "预览文件大于" + maxSize + "M，不支持预览!",
          //   iconClass: "mintui mintui-error"
          // });
          // 不支持预览直接下载文件
          commonJS.downloadAppendixs(appendixlist, event);
          return false;
        }
      }
    }

    // 转换预览附件
    isFileSuccessTrans(vThis, appendixlist, picType.indexOf(type) > -1);
  } else {
    // 不支持预览直接下载文件
    commonJS.downloadAppendixs(appendixlist, event);
  }
};

/**
 * 判断附件是否支持预览
 * @params {string} appendixName 附件名称
 * @returns 返回附件是否支持预览
 */
commonJS.judgePreviewAppendixs = function (appendixName) {
  let isPreviewAppendix;
  let previewAppendixType = [
    "doc",
    "docx",
    "txt",
    "log",
    "xls",
    "xlsx",
    "ppt",
    "pptx",
    "pdf",
    "jpg",
    "jpeg",
    "png",
    "gif",
    "bmp",
  ]; //支持预览的文件格式
  var appendixType = appendixName
    .substring(appendixName.lastIndexOf(".") + 1, appendixName.length)
    .toLowerCase();
  if (previewAppendixType.indexOf(appendixType) > -1) {
    //支持预览
    isPreviewAppendix = true;
  } else {
    //不支持预览
    isPreviewAppendix = false;
  }
  return isPreviewAppendix;
};

/**
 * 下载附件
 * @params {object} vThis vue组件对象(this)
 * @params {int} id 附件id
 * @params {object} event
 */
commonJS.downloadAppendixs = function (obj, event) {
  event.stopPropagation();
  // let downloadFile = `/msService/baseService/workbench/download`; //附件下载接口
  const { id, name } = obj;
  const { downloadFile } = interfaceURL;
  const { url, serverName, methodType } = getAuthentication();
  let urlParams = getRequestParams(window.location.href); // 获取地址参数
  let entityCode = urlParams ? urlParams.entityCode : "";
  publicGetAxios(
    {
      url: `${downloadFile}?id=${id}&methodType=${methodType}&url=${url}&serverName=${serverName}&entityCode=${entityCode}`,
      responseType: "blob",
    },
    res => {
      const blob = new Blob([res], {
        type: "application/octet-stream;charset=UTF-8",
      });
      if ("msSaveOrOpenBlob" in navigator) {
        window.navigator.msSaveOrOpenBlob(blob, name);
      } else {
        const content = window.document.body;
        const downloadA = window.document.createElement("a");
        const href = window.URL.createObjectURL(blob);
        const ev = document.createEvent("HTMLEvents");
        ev.initEvent("click", false, true);
        downloadA.href = href;
        downloadA.download = name;
        downloadA.className = "needsclick";
        downloadA.style.touchAction = "none";
        content.appendChild(downloadA);
        downloadA.dispatchEvent(ev);
        downloadA.click();
        content.removeChild(downloadA);
        window.URL.revokeObjectURL(href);
      }
    },
  );
};

/**
 * 获取附件类型，并根据附件类型设置class样式
 * @params {string} appendixName 附件名称
 * @returns 返回对应的class名称
 */
commonJS.getAppendixTypes = function (appendixName) {
  var appendixType = appendixName.substring(
    appendixName.lastIndexOf(".") + 1,
    appendixName.length,
  );
  appendixType = appendixType.toLowerCase();
  var className = "";
  switch (appendixType) {
    case "doc":
    case "docm":
    case "docx":
    case "dot":
    case "dotm":
    case "dotx":
    case "odt":
      className = "appendix-icon-word";
      break;
    case "xls":
    case "xlsx":
    case "xlsb":
    case "xlsm":
      className = "appendix-icon-excel";
      break;
    case "png":
    case "bmp":
    case "jpg":
    case "jpeg":
    case "gif":
    case "svg":
    case "raw":
    case "jfif":
      className = "appendix-icon-pic";
      break;
    case "ppt":
    case "pptm":
    case "pptx":
    case "pot":
    case "potm":
    case "potx":
    case "pps":
    case "ppsm":
    case "ppsx":
      className = "appendix-icon-ppt";
      break;
    case "txt":
      className = "appendix-icon-txt";
      break;
    case "pdf":
      className = "appendix-icon-pdf";
      break;
    case "xml":
      className = "appendix-icon-xml";
      break;
    default:
      className = "appendix-icon-qt";
      break;
  }
  return className;
};

/**
 * 添加和删除附件
 * @params {object} event
 * @params {string} attachmentFile 添加或者删除的附件信息
 * @params {object} condition 附件字段的信息
 * @params {string} type 操作附件的类型，upload添加附件，delete删除附件
 * @params {object} vThis vue组件对象(this)
 */
commonJS.updateAppendixs = function (
  event,
  attachmentFile,
  condition,
  type,
  vThis,
) {
  var obj = event.target;
  let tempName = obj.name || condition.name;
  if (tempName && tempName.indexOf(".") > -1) {
    let typeName = window.pageConfig && window.pageConfig.modelAlias;
    tempName = tempName.replace(`${typeName}.`, "");
  }
  const fileList = vThis.attachmentFile[`${tempName}Files.filePath`] || [];
  const typeList = vThis.attachmentFile[`${tempName}Files.fileType`] || [];
  const codeList = vThis.attachmentFile[`${tempName}Files.propertyCode`] || [];
  if (type == "upload") {
    let workflowEnabled =
      window.pageConfig && window.pageConfig.workflowEnabled;
    let modelCode = window.pageConfig && window.pageConfig.modelCode;
    let data = attachmentFile.data;

    fileList.push(data.path);
    typeList.push("attachment");
    codeList.push(condition.propertyCode);

    vThis.attachmentFile[`${tempName}Files.filePath`] = fileList;
    vThis.attachmentFile[`${tempName}Files.fileType`] = typeList;
    vThis.attachmentFile[`${tempName}Files.propertyCode`] = codeList;
    vThis.attachmentFile[`${tempName}Files.type`] = this.getUploadType(
      workflowEnabled,
      modelCode,
    );
  } else if (type == "delete") {
    vThis.attachmentFileDel = {};
    if (attachmentFile.id) {
      vThis.ids2delList.push(attachmentFile.id);
      vThis.attachmentFileDel[`ids2del`] = vThis.ids2delList;
    } else {
      let attachmentName = attachmentFile.name;
      const index = fileList.findIndex(filePath => {
        return filePath.slice(filePath.lastIndexOf("\\") + 1) == attachmentName;
      });
      fileList.splice(index, 1);
      typeList.splice(index, 1);
      codeList.splice(index, 1);
      if (fileList.length == 0) {
        Object.keys(vThis.attachmentFile).forEach(key => {
          delete vThis.attachmentFile[key];
        });
      } else {
        vThis.attachmentFile[`${tempName}Files.filePath`] = fileList;
        vThis.attachmentFile[`${tempName}Files.fileType`] = typeList;
        vThis.attachmentFile[`${tempName}Files.propertyCode`] = codeList;
      }
    }
  }
  vue.attachmentFile = vThis.attachmentFile;
  vue.attachmentFileDel = vThis.attachmentFileDel;
  obj.value = null;
};

/**
 * 合并两个数组
 * @params {Array} arr1 数组一
 * @params {Array} arr2 数组二
 * @returns 返回合并生成的数组arr3
 */
commonJS.mergedArray = function (arr1, arr2) {
  var arr3 = [];

  // 遍历arr1
  if (arr1) {
    for (var i = 0; i < arr1.length; i++) {
      arr3.push(arr1[i]);
    }
  }

  // 遍历arr2
  if (arr2) {
    for (var j = 0; j < arr2.length; j++) {
      arr3.push(arr2[j]);
    }
  }

  return arr3;
};

/**
 * 整数的四舍五入
 * @params {int} number 数字
 * @returns 返回四舍五入之后的数值
 */
commonJS.numRound = function (number) {
  return number < 0 ? -1 * Math.round(-1 * number) : Math.round(number);
};

/**
 * 删除所有HTML标签
 * @params {str} str 字符
 * @returns 返回删除HTML标签的字符串
 */
commonJS.encodeHtml = function (str) {
  return str.replace(/<[^<>]+?>/g, ""); //
};

/**
 * 获取单位
 * @params {object} condition
 * @returns 返回获取的单位
 */
commonJS.getUnitsByFormat = function (condition) {
  let numberType = ["INTEGER", "LONG", "DECIMAL"];
  let viewType = window.pageConfig && window.pageConfig.viewType;
  if (condition) {
    let fmt = condition.showFormat;
    let unit = { PERCENT: "%" };
    if (numberType.indexOf(condition.columnType) > -1) {
      if (viewType == "EDIT") {
        if (fmt && unit[fmt]) {
          return unit[fmt];
        } else {
          return "";
        }
      } else if (viewType == "VIEW") {
        if (fmt && unit[fmt] && condition.value) {
          return unit[fmt];
        } else {
          return "";
        }
      }
    }
  }
  return "";
};

export default commonJS;



// WEBPACK FOOTER //
// ./src/assets/js/itemList/index.js