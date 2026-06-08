// 全局函数注册
import { Indicator } from "mint-ui";
import Axios from "axios"; //接口请求
import eventMap from "./eventMap.js";
import interfaceURL from '../util/interface.js';

// 添加ticket
export const addTicket = () => {
  const ticket = localStorage.getItem("ticket");
  const cookie = getCookie('suposTicket');
  if (ticket) return `Bearer ${ticket}`;
  if (cookie) return `Bearer ${cookie}`;
  return '';
}

export const publicGetAxios = (param, callback) => {
  let config;
  const ticket = addTicket();
  // loading加载
  if (!(param.loadType && param.loadType == "dropDown")) {
    setTimeout(() => {
      Indicator.open({
        text: getIntl("notice.loading"),
        //文字
        spinnerType: "fading-circle"
        //样式
      });
    }, 0);
  }

  if (param.type && param.type.toLocaleLowerCase() == "post") {
    // post 请求
    config = {
      method: "post",
      url: param.url,
      data: param.data,
      dataType: param.dataType,
      headers: param.headers
    };
  } else {
    // get 请求
    config = {
      method: param.type || "get",
      url: param.url,
      params: param.data
    };
  }

  if (ticket) {
    config.headers = { ...param.headers, Authorization: ticket };
  }

  return new Promise((resolve, reject) => {
    Axios({ ...param, ...config })
      .then(
        (result = {}) => {
          Indicator.close();
          if (result.status == 200 && (result.data || result.data === 0)) {
            if (typeof callback == "function") {
              callback(result.data);
              var modal = document.getElementsByClassName("edit_modal");
              setTimeout(() => {
                modal[0].style.display = "none";
                Indicator.close();
              }, 500);
            }
          }
          resolve(result.data);
        },
        err => {
          Indicator.close();
          reject(err);
          if (typeof param.error === "function") param.error(err);
        }
      )
      .catch(function (error) {
        console.log(error);
        Indicator.close();
        reject(error);
        if (typeof param.error === "function") param.error(error);
      });
  });
};

export const tryCatch = (func, ...params) => {
  if (func) {
    try {
      const temp = func(...params);
      return temp;
    } catch (error) {
      console.log("执行错误", error);
      console.error("自定义代码错误", error.message);
    }
  }
  return true;
};

// 获取自定义事件
export const combineEvents = (funcname, funcbody) => {
  const events = {};
  if (funcname && funcbody) {
    const funcNameArr = funcname.split(" ");
    const funcBodyArr = funcbody.split("@@@@");
    if (funcNameArr && funcNameArr.length > 0) {
      funcNameArr.forEach((func, index) => {
        const name = func.split("=")[0];
        const bodyStr =
          (funcBodyArr[index] && funcBodyArr[index].replace(/\r\n/g, "")) || "";
        try {
          const body = (bodyStr && eval(`(${bodyStr})`)) || nullFunc;
          if (name && eventMap[name]) {
            events[eventMap[name]] = body;
          }
        } catch (error) {
          console.error(error.message);
        }
      });
    }
  }
  return events;
};

//获取类型
export const getQueryType = (data = {}) => {
  const { showType, columnType } = data;
  const referComp = ["SELECTCOMP", "SUPERVISION", "MULTSELECT"];
  const isReferComp = referComp.indexOf(showType) > -1; // 是否为参照类型
  var isDatagrid = data.regionType;
  var type;
  if (showType == "DATE" || showType == "DATETIME") {
    type = "date";
  } else if (
    showType == "SELECTCOMP" ||
    showType == "RADIO" ||
    showType == "SELECT" ||
    showType == "MULTSELECT" ||
    showType == "CHECKBOX"
  ) {
    type = "select";
  } else if (showType == "choose") {
    type = "choose";
  } else if (showType == "PASSWORDFIELD") {
    type = "password";
  } else if (showType == "DATAGRID") {
    // datagrid
    type = "datagrid";
  } else if (showType == "PROPERTYATTACHMENT") {
    // 附件
    type = "attachment";
  } else if (showType == "PICTURE") {
    // 图片
    type = "picture";
  } else if (showType == "TEXTAREA") {
    //长文本
    type = "longtext";
  } else {
    type = "text";
  }
  return type;
};

export const isTextComp = data => {
  const type = getQueryType(data);
  const ValidateType = ["text", "textfield", "longtext"];
  if (ValidateType.indexOf(type) > -1) {
    return true;
  }
  return false;
};

export const isRefenceComp = data => {
  const { showType, columnType, referenceview } = data;
  const referComp = ["SELECTCOMP", "SUPERVISION", "MULTSELECT"];
  const isReferComp = referComp.indexOf(showType) > -1; // 是否为参照类型
  if ((isReferComp || referenceview) && columnType !== "SYSTEMCODE") {
    return true;
  }
  return false;
};

export const getRequestParams = (url = window.location.href) => {
  var url = decodeURI(url); //获取url中"?"符后的字串
  var theRequest = new Object();
  if (url.indexOf("?") != -1) {
    var str = url.split("?");
    let strs = str[1].split("&");
    for (var i = 0; i < strs.length; i++) {
      strs[i] = strs[i].split("#")[0];
      theRequest[strs[i].split("=")[0]] = unescape(strs[i].split("=")[1]);
    }
    return theRequest;
  } else {
    return "";
  }
};

/**
 * 用于get方法后面参数的拼接，传入data是对象
 * @param {*} name
 */
export const getUrlConcat = data => {
  let dataStr = ""; // 数据拼接字符串
  Object.keys(data).forEach(key => {
    dataStr += `${key}=${data[key]}&`;
  });
  if (dataStr !== "") {
    dataStr = dataStr.substr(0, dataStr.lastIndexOf("&")); // 去除掉最后一个"&"字符
  }
  return dataStr;
};

// 获取自定义参数
// isOldRef 标识人员部门岗位类型参照返回值不同
export const getCustomCondition = (isOldRef = false) => {
  const { permissionCode = null } = window.pageConfig || {};
  const customCon = { customCondition: {} };
  const param = getRequestParams();
  if (param && param.customConditionKey) {
    const customKeys = param.customConditionKey.split(",");
    const condition = {};
    customKeys.forEach(key => {
      condition[`${key}`] = param[`${key}`];
    });
    customCon.customCondition = condition;
  }
  if (permissionCode && permissionCode !== "null")
    customCon.permissionCode = permissionCode; // 参照视图添加permissionCode参数
  if (isOldRef) {
    return { ...customCon.customCondition };
  }
  return customCon;
};

// 头部是否显示
export const getIsHideWebTitle = () => {
  let urlParams = getRequestParams(); // 获取地址参数
  const { hideWebTitle: hideWebTitleWin = "" } = window.pageConfig;
  const { hideWebTitle: hideWebTitleUrl = "" } = urlParams;
  const hideWebTitle =
    hideWebTitleWin.toString() === "true" ||
    hideWebTitleUrl.toString() === "true";
  // if (hideWebTitle) this.hidden = hideWebTitle;
  return hideWebTitle || false;
};

// 链接跳转
export const openLinkUrl = url => {
  try {
    // window.mobilejs.pushVC(`http://192.168.91.46:8081/${url}`);
    window.mobilejs.pushVC(url);
  } catch (error) {
    window.open(url, "_self");
  }
};
// 获取cookie
export const getCookie = name => {
  const reg = new RegExp(`(^| )${name}=([^;]*)(;|$)`);
  const arr = document.cookie.match(reg);
  if (arr) return unescape(arr[2]);
  return null;
};
// 获取当前语言
export const getCurrentLanguage = () => {
  const defaultBapLan = "zh";
  const currentLan = getCookie("language");
  const bapLanMap = {
    zh_CN: "zh",
    en_US: "en"
  };
  return bapLanMap[currentLan] || defaultBapLan;
};
// 获取国际化值
export const getIntl = key => {
  return window.vue ? window.vue.$t(key) : key;
};

let currentLoginInfo;

// 获取当前登录人信息
export const getLoginInfo = async () => {
  if (currentLoginInfo) return currentLoginInfo;
  await publicGetAxios({ url: interfaceURL.getCurrentLoginInfo }, (res) => {
    if (res) {
      const { data } = res;
      currentLoginInfo = data;
    }
  });
  return currentLoginInfo || {};
};

let authentication;

// 获取鉴权信息
export const getAuthentication = () => {
  if (authentication) return authentication;
  const { interfaceApi = {} } = window.pageConfig || {}; // 基础服务接口配置信息
  if (interfaceApi.authenticationMessage) {
    publicGetAxios({ url: interfaceApi.authenticationMessage }, res => {
      if (res) authentication = res.data;
    });
  }
  return "";
};

export const GetRequest = () => {
  const url = decodeURI(url); //获取url中"?"符后的字串
  let theRequest = new Object();
  if (url.indexOf("?") != -1) {
    const str = url.split("?");
    let strs = str[1].split("&");
    for (var i = 0; i < strs.length; i++) {
      strs[i] = strs[i].split("#")[0];
      theRequest[strs[i].split("=")[0]] = unescape(strs[i].split("=")[1]);
    }
    return theRequest;
  } else {
    return "";
  }
}



// WEBPACK FOOTER //
// ./src/assets/js/util/common.js