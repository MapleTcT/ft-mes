import { publicGetAxios } from "@/assets/js/util/common.js";
import interfaceURL from "../../../assets/js/util/interface.js";

// 系统编码slots赋值
const setConditionSlots = (eleObj = {}, resData = {}) => {
  let multables = [];
  let arrs = [""];
  let keys = [""];
  let valueKeys = [];
  let valuesIds = [];
  const { multable: isMultable = false } = eleObj;
  let { value: valList } = eleObj;
  valList = valList ? valList.split(",") : [];
  for (const key in resData) {
    if (resData.hasOwnProperty(key)) {
      const el = resData[key];
      if (isMultable) {
        multables.push({ label: el, value: key });
        for (let i = 0; i < valList.length; i++) {
          if (valList[i] === el || valList[i] === key) {
            valueKeys.push(el);
            valuesIds.push(key);
          }
        }
      } else {
        arrs.push(el);
        keys.push(key);
      }
    }
  }
  if (isMultable) {
    eleObj.slots = multables;
    eleObj.value = valueKeys.join(",");
    eleObj.valueId = valuesIds;
  } else {
    eleObj.slots = [
      {
        flex: 1,
        valueIndex: 0,
        defaultIndex: 0,
        value: arrs[0],
        valueKey: 0,
        values: arrs,
        valueKey: keys
      }
    ];

    if (eleObj.value) {
      let valueIndex = eleObj.slots[0].values.indexOf(eleObj.value);
      eleObj.valueId = eleObj.slots[0].valueKey[valueIndex];
    }
  }
};

/**
 * 打开系统编码树形界面,并加载系统编码数据
 * @params {object} vThis vue组件对象(this)
 * @params {object} data 字段数据对象
 * @params {boolean} isPagelist 是否列表视图，列表视图系统编码树形多选和单选一样
 */
export const openSystemCodeInterface = (vThis, data, isPagelist) => {
  if (data.readonly) {
    if (data.isTreeSystemCode && isPagelist) {
      //列表视图的系统编码树形，解决点击弹出键盘的问题
      if (data.placeholder == "") {
        //配置页面设置只读
        return false;
      }
    } else {
      return false;
    }
  }
  let url;
  let fillContent;
  if (data.fillContent) {
    fillContent = data.fillContent.fillContent;
  } else if (data.systemCode) {
    fillContent = data.systemCode.fillContent;
  }
  url = interfaceURL.valueTreeList + `?systemEntityCode=${fillContent}`;
  // url = "../static/data/systemcode.json"; //系统编码数据
  publicGetAxios(
    {
      url: url,
      type: "get"
    },
    function(result) {
      vThis.$refs.systemCodeTree.data = result.data;
      vThis.$refs.systemCodeTree.isShowSystemCode = true;
      if (data.multable && !isPagelist) {
        //树形多选
        vThis.$refs.systemCodeTree.showCheckbox = true;
      } else {
        //树形单选
        vThis.$refs.systemCodeTree.showCheckbox = false;
      }
    }
  );
  vThis.$forceUpdate();
};

/**
 * 获取系统编码的json数据
 * @params {object} eleObj element对象
 */
export const getSystemCodeJson = (eleObj = {}) => {
  let url;
  const { fill = {}, fillContent = {} } = eleObj;
  const fillkey = fillContent.fillContent || fill.fillContent;
  if (window.pageConfig) {
    url = window.pageConfig.baseServiceApi.systemCodeJson;
  }
  if (sessionStorage.getItem(fillkey)) {
    const slots = sessionStorage.getItem(fillkey);
    setConditionSlots(eleObj, JSON.parse(slots));
    return;
  }

  if(!fillkey) return;
  
  publicGetAxios(
    {
      url: url,
      type: "get",
      data: {
        systemEntityCode: fillkey
      }
    },
    result => {
      let resData = result.data;
      setConditionSlots(eleObj, resData);
      sessionStorage.setItem(fillkey, JSON.stringify(resData));
    }
  );
};

export const formatVal = (element = {}) => {
  const { value: curVal, name: nameKey, multable } = element;
  const data = curVal && curVal.id ? curVal.id : curVal;

  // 格式化值
  if (multable) {
    //多选
    let values = element.value;
    values = values ? values.split(",") : [];
    let valueKeys = [],
      valuesIds = [];
    if (values && element.slots && element.slots.length > 0) {
      for (let i = 0; i < values.length; i++) {
        var elt = values[i];
        for (let j = 0; j < element.slots.length; j++) {
          var et = element.slots[j];
          if (elt == et.value || elt == et.label) {
            valueKeys.push(et.label);
            valuesIds.push(et.value);
          }
        }
      }
      element.value = valueKeys.join(",");
      element.valueId = valuesIds;
    } else {
      element.value = element.valueId = values.join(",");
    }
    element.loadValue = element.value;

    return;
  } else if (!multable) {
    //单选
    let slots = element.slots && element.slots[0];
    if (element.valueId && slots && slots.values) {
      let valueIndex = slots.valueKey.indexOf(element.valueId);
      // if (valueIndex < 0) element.value = '';
      element.value = slots.values[valueIndex];
    }
    element.loadValue = element.value;

    return;
  }
};



// WEBPACK FOOTER //
// ./src/components/cellComp/systemCode/util.js