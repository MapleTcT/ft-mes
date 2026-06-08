import { Indicator } from "mint-ui";
import _commonJs from "@/assets/js/itemList/index.js";
import { publicGetAxios, tryCatch, getIntl } from "@/assets/js/util/common.js";
import interfaceURL from "../../../assets/js/util/interface.js";

// 获取参照更多值
const getReferMoreObj = (values, referenceview, list, isPreferRefer) => {
  let { url: sUrl = "", mneType = "" } = referenceview;
  let getUrl = "";
  if (
    values.id == undefined ||
    values.id === "" ||
    values.id == getIntl("placeholder.choose.text")
  ) {
    return { id: "" };
  }

  var includes = [];

  if (list) {
    Object.keys(list).forEach(key => {
      if (list[key].columnType == "SYSTEMCODE") {
        includes.push(key + ".value");
      } else {
        includes.push(key);
      }
    });
  }

  if (includes.length == 0) {
    return false;
  }

  includes = includes.join(",");

  if (mneType === "other") {
    // 业务参照
    const urlSplit = sUrl.split("/");
    getUrl = `${urlSplit.slice(0, urlSplit.length - 1).join("/")}/data/${
      values.id
    }?includes=${includes}`;
  } else {
    // 基础参照
    getUrl = `${interfaceURL[`${mneType.toLowerCase()}`]}?id=${
      values.id
    }&includes=${includes}`;
  }

  publicGetAxios(
    {
      url: getUrl,
      type: "get"
    },
    res => {
      let klist = res.data;
      if (klist) {
        for (const ki in list) {
          if (list.hasOwnProperty(ki)) {
            const element = list[ki];
            try {
              let el = eval(`klist.${ki}`);
              // element.valueId = klist.id;
              if (element.columnType == "SYSTEMCODE") {
                element.value = el.value;
              } else {
                element.value = el;
              }
            } catch (error) {}
          }
        }
      }
    }
  );
};

const getReferenceId = (el={},id) => {
  const {isPrefer} = el;
  if(isPrefer){
    el.valueId = id;
  }
  return el;
}

/**
 * 执行参照对象的事件，目前支持beforecallback或者callback方法
 * @params {object} data 对象数据
 * @params {object} list 参照对象列表数据
 * @params {string} eventName 参照对象的事件名
 */
const executeReferObjEvent = (data, list, eventName) => {
  let eventBindConfig = _commonJs.getEventBindConfig([data], true, true); //获取字段的事件配置
  if (eventBindConfig.length > 0) {
    for (let m = 0; m < eventBindConfig.length; m++) {
      const elt = eventBindConfig[m];
      if (elt.ename && elt.ename == eventName) {
        if (elt.fbody && elt.fbody.indexOf("function") > -1) {
          var obj = list;
          var ex = { getcbplay: eval("(" + elt.fbody + ")") };
          return ex.getcbplay(obj, data.index);
        }
      }
    }
  }
};

const getShowKey = (condition = {}) => {
  const { name = "", regionType } = condition;
  const key = name.split(".");
  const intoNum = regionType === "DATAGRID" ? 1 : 2;
  const showKey = key.splice(intoNum, key.length - 1).join(".");
  return showKey;
};

// 自定义字段
const getCustomRelate = (data = {}, condition = {}, isDt, index) => {
  const { related_key_map: relateMap = {} } = data;
  const dateType = ["DATE", "DATETIME"];
  const {
    iscustom,
    relatedKey,
    columnType,
    regionType,
    multable,
    isPrefer
  } = condition;
  const dtFlag = regionType === "DATAGRID";
  const isDate = dateType.indexOf(columnType) > -1; // 日期型
  let condis;
  if (isDt && dtFlag && condition.index === index) {
    // 表格
    condis = condition;
  } else if (!dtFlag && !isDt) {
    condis = condition;
  }
  if (condis && relatedKey && relateMap[relatedKey] && iscustom) {
    let curKey = relateMap[relatedKey];
    let val = _commonJs.getValueByKey(data, condis, relatedKey);
    try {
      val = eval(`data.${curKey}`);
    } catch (error) {}
    if (isDate) val = _commonJs.getValueByKey(data, condis, curKey);
    const curVal = val || "";
    condis.value = curVal;
    if (isPrefer) {
      // 参照
      curKey = curKey.split(".");
      curKey.splice(curKey.length - 1, 1);
      curKey = curKey.join(".") + ".id";
      try {
        condis.valueId = eval(`data.${curKey}`);
      } catch (error) {}
    } else if (columnType === "SYSTEMCODE" && !multable) {
      condis.value = (val && val.value) || "";
      condis.valueId = (val && val.id) || condis.value;
    } else if (columnType === "SYSTEMCODE" && multable) {
      condis.valueId = curVal.split(",");
      const { slots } = condis;
      let values = [];
      slots.forEach(item => {
        if (condis.valueId.includes(item.value)) {
          values.push(item.label);
        }
      });
      condis.value = values.join(",");
    }
  }
};

/**
 * 打开参照视图界面
 * @params {object} vThis vue组件对象(this)
 * @params {object} data 字段数据对象
 */
const OpenReferViewSelect = function(vThis, data = {}) {
  if (data.readonly || !data.referenceview) {
    return false;
  }

  // 获取参照参数，跨公司 回填到url
  if (!data.referenceview.url && data.referenceview.url == "") {
    vThis.myToast(getIntl("notice.invalid.address"));
    return false;
  }
  if (vThis.$root.$children["0"].$refs.RightBtn.hidden) {
    vThis.$root.$children["0"].$refs.RightBtn.isRefer = true; //判断悬浮按钮是否显示
  }
  // this.showRefer = true;

  vThis.referConfig.url = _commonJs.resolveReferUrl(
    data.referenceview.url,
    data
  ); //拼接参照url
  vThis.updateRefer(true, vThis.referConfig.url);
  Indicator.open({
    text: getIntl("notice.loading"), //文字
    spinnerType: "fading-circle" //样式
  });

  // 回调方法
  vThis.$nextTick(function() {
    let iframe = document.getElementById("newPage");
    iframe.onload = function() {
      Indicator.close();
      if (iframe) {
        // 去掉原生头部
        // try {
        //   window.mobilejs.open_dialog();
        // } catch (err) {}

        // 注册方法
        // 选择回调方法
        let code = "";
        if (data.referenceview.code) {
          code = data.referenceview.code.replace(/\./gi, "_");
        }
        iframe.contentWindow[`_callBack_ec_${code}`] = function(list) {
          // 恢复原生头部
          // try {
          //   window.mobilejs.close_dialog();
          // } catch (err) {}
          if (vThis.$root.$children["0"].$refs.RightBtn.hidden) {
            vThis.$root.$children["0"].$refs.RightBtn.isRefer = false; //判断悬浮按钮是否显示
            vThis.$root.$children["0"].$refs.RightBtn.dragButton();
          }

          // 回调赋值
          // data = $this.referCallBackObject(list, [data]);
          if (list && list.length > 0) {
            if (data.columnType == "MULTSELECT") {
              //多选
              if (executeReferObjEvent(data, list, "onBeforeSet") === false)
                return false;
              data = vThis.getReferDataCallBack(list, data);
              setTimeout(function() {
                vThis.showRefer = false;
                vThis.updateRefer(false);
              }, 200);
              vThis.$nextTick(function() {
                executeReferObjEvent(data, list, "onAfterSet");
              });
              return true;
            }
            list = list[0];
          }

          //对象类型的字段绑定执行beforecallback方法
          if (data.events && data.events.beforecallback) {
            if (
              tryCatch(data.events.beforecallback, list, data.index) === false
            )
              return false;
          } else {
            if (executeReferObjEvent(data, list, "beforecallback") === false)
              return false;
          }

          var endName = data.name.split(".");
          var sourcename =
            data.regionType === "DATAGRID" ? endName[0] : endName[1];
          let listcon = {};

          // 参照关联赋值
          const fields = vThis.getCompByName("iForm").getFields();

          // 当前字段赋值
          _commonJs.getValueByKeyEdit(data, list, "", true);
          for (let i = 0; i < fields.length; i++) {
            const element = fields[i];
            const { condition = {} } = element.params;
            const { name: conName = "" } = condition;
            const { index } = data;
            let condis = {};
            const dtFlag = condition.regionType === "DATAGRID";
            const isDt = data.regionType === "DATAGRID";

            // 关联字段
            if (
              data.name !== conName &&
              conName.indexOf(`${sourcename}.`) > -1
            ) {
              if (isDt && dtFlag && condition.index === index) {
                // 表格
                condis = condition;
              } else if (!dtFlag && !isDt) {
                condis = condition;
              }
            }

            if (condis.name) {
              const showKey = getShowKey(condis);
              try {
                const tval = eval(`list.${showKey}`);
                if (tval || tval === false) {
                  // 参照取id值
                  // li.condition[0].valueId = vThis.getReferValueId(list, 'id');
                  _commonJs.getValueByKeyEdit(condis, list, "", true);
                } else {
                  listcon[showKey] = getReferenceId(condis,list.id);
                }
              } catch (error) {
                listcon[showKey] = getReferenceId(condis,list.id);
              }
            }

            // 自定义字段
            getCustomRelate(list, condition, isDt, index);
          }
          // 请求参照关联值
          if (!data.iscustom) {
            getReferMoreObj(list, data.referenceview, listcon, true);
          }

          // 赋值id
          if(data.isPrefer) {
            data.valueId = list.id;
          }

          // 关闭参照层
          setTimeout(function() {
            vThis.showRefer = false;
            vThis.updateRefer(false);
          }, 200);

          //对象类型的字段绑定执行callback方法
          vThis.$nextTick(function() {
            if (executeReferObjEvent(data, list, "callback") === false) {
              return false;
            }
          });
        };

        // 返回按钮方法
        iframe.contentWindow[`_close_ec_${code}`] = function() {
          // 关闭iframe
          setTimeout(function() {
            // vThis.showRefer = false;
            vThis.updateRefer(false);
          }, 200);
          // 恢复原生头部
          // try {
          //   window.mobilejs.close_dialog();
          // } catch (err) {}
        };
      }
    };
  });
};

/**
 * 删除参照类型字段的值和参照关联的值
 * @params {object} vThis vue组件对象(this)
 * @params {object} data 字段数据对象
 */
const deleteReferValues = function(vThis, data = {}) {
  let inputRef = vThis.params.name;
  const { regionType } = data;
  if (typeof data.value == "string") {
    data.valueId = "";
    data.value = "";
    if (!data.datagridCode) {
      document.getElementsByName(inputRef).value = "";
    }
  }
  if (data.isPrefer) {
    // 删除参照关联的值
    var endName = data.name.split(".");
    var sourcename = regionType === "DATAGRID" ? endName[0] : endName[1];
    const fields = vThis.getCompByName("iForm").getFields();
    for (let i = 0; i < fields.length; i++) {
      const element = fields[i];
      const { condition = {} } = element.params;
      const { index } = data;
      let condis = {};
      const dtFlag = condition.regionType === "DATAGRID";
      const isDt = data.regionType === "DATAGRID";

      // 自定义字段
      getCustomRelate({}, condition, isDt, index);

      if (condition.name && condition.name.indexOf(`${sourcename}.`) > -1) {
        if (isDt && dtFlag && condition.index === index) {
          // 表格
          condis = condition;
        } else if (!dtFlag && !isDt) {
          condis = condition;
        }
      }

      // 清空
      condis.value = "";
      condis.valueId = "";
      condis.valueFormat = "";
      if(condis.formVal) delete condis.formVal;

      if (typeof condis.value == "string") {
        document.getElementsByName(condis.name).value = "";
      }
      if (condis.columnType == "MONEY") {
        //金额类型
        condis.valueFormat = "";
      }
    }
  }
  vThis.$forceUpdate();
};

export { OpenReferViewSelect, deleteReferValues, tryCatch };



// WEBPACK FOOTER //
// ./src/components/cellComp/reference/util.js