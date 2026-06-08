// 全局函数注册
import _commonJs from "@/assets/js/itemList/index.js";
import { getValidateRule } from "@/assets/js/util/validate.js";
import {
  getQueryType,
  combineEvents,
  isRefenceComp,
  getIntl
} from "@/assets/js/util/common.js";

const commonFunc = {
  // 获取规则验证
  getRules: element => {
    const compType = getQueryType(element);
    const validateRule = getValidateRule(element, compType); // 获取验证规则
    return [
      {
        required: !element.nullable,
        message: `${element.namekey}${getIntl("validate.tip.empty")}`
      },
      ...validateRule
    ];
  },

  // 构造radio下拉列表 checkbox fill自定义值
  getSlotsByFill: (element, isMultable) => {
    try {
      if (element && element.fillContent) {
        let fillContent = element.fillContent.fillContent;
        let keys = [""],
          values = [""];
        let multables = [];
        let orders = element.fillContent.fillOrder.split(",");
        for (let i = 0; i < orders.length; i++) {
          const et = orders[i];
          if (isMultable) {
            multables.push({
              label: fillContent[et],
              value: et
            });
          } else {
            keys.push(et);
            values.push(fillContent[et]);
          }
        }
        if (isMultable) {
          element.slots = multables;
        } else {
          element.slots = [
            {
              flex: 1,
              valueIndex: 0,
              defaultIndex: 0,
              value: values[0],
              valueKey: keys,
              values: values
            }
          ];
        }
      }
    } catch (error) {
      if (isMultable) {
        element.slots = [{ label: "", value: "" }];
      } else {
        element.slots = [
          {
            flex: 1,
            valueIndex: 0,
            defaultIndex: 0,
            value: [""],
            valueKey: "",
            values: [""]
          }
        ];
      }
    }
  },

  getdItemdition: (data, referCode, isReadOnly) => {
    var $this = this;
    var queryType = getQueryType(data);
    var con = [];
    var numberType = ["INTEGER", "LONG", "DECIMAL", "MONEY"];
    var isPassword = "PASSWORDFIELD".indexOf(data.columnType) > 1; // 密码型
    var isNumber = numberType.indexOf(data.columnType) > -1; //数字型
    var temp = {
      label: data.label || data.namekey,
      type: queryType,
      columnType: data.columnType,
      placeholder: "",
      clickEvent: "",
      readonly: isReadOnly || data.isreadonly || data.readonly,
      value: "",
      showFormat: data.showFormat,
      showType: data.showType,
      isNumber: isNumber,
      isPassword: isPassword,
      code: data.code,
      name: data.key,
      namekey: data.namekey,
      slots: [],
      // systemCode: data.fill,
      isPrefer: isRefenceComp(data), // 是否是参照
      multable: data.multable,
      nullable: data.nullable,
      displayfield: data.displayfield,
      newMultiLists: {},
      complex: data.complex,
      propertyCode: data.propertyCode,
      precision: data.precision,
      isHidden: data.hide,
      valueId: "",
      rules: commonFunc.getRules(data),
      regionType: data.regionType,
      isTreeSystemCode:
        data.isTreeSystemCode || (data.fill && data.fill.listType === "TREE"), //是否系统编码树形
      isOnlyLeaf: data.isOnlyLeaf, //系统编码树形单选是否仅可选叶子节点
      refCondition: data.refCondition, //参照参数
      tabIndex: data.tabIndex,
      maxLength: data.maxLength,
      events: combineEvents(data.funcname, data.funcbody),
      iscustom: data.isCustom, // 是否为自定义字段
      relatedKey: data.relatedKey,
      defaultValue: data.defaultValue
    };
    // 兼容dg参照多选multable为false的情况
    if(temp.isPrefer&&data.columnType==='MULTSELECT'){
      temp.multable=true;
    }

    if (typeof temp.readonly == "undefined") temp.readonly = false;

    switch (queryType) {
      case "label":
        con = [
          {
            ...temp
          }
        ];
        break;
      case "date":
        var transform = {
          YMD: "date",
          YM: "date",
          Y: "date",
          DEFAULT: "date",
          YMD_HMS: "datetime",
          YMD_HM: "datetime",
          YMD_H: "datetime",
          isNumber: isNumber,
          code: data.code
        };
        con = [
          {
            ...temp,
            // clickEvent: $this.selectDate,
            // readonly: true,
            // placeholder: `请选择${data.namekey}`,
            placeholder: temp.readonly
              ? ""
              : getIntl("placeholder.choose.text"),
            pickerName: `${data.key}_pickerStart`,
            datetype: transform[data.showFormat]
          }
        ];
        break;
      case "text":
      case "longtext":
        con = [
          {
            ...temp,
            placeholder: temp.readonly ? "" : getIntl("placeholder.enter.text")
          }
        ];
        break;
      case "section":
        con = [
          {
            ...temp,
            placeholder: "",
            value1: "",
            value2: ""
          }
        ];
        break;
      case "select":
        con = [
          {
            ...temp,
            clickEvent: $this.showPickerPop, // 是否参照
            // readonly: true,
            placeholder: temp.readonly
              ? ""
              : getIntl("placeholder.choose.text"),
            fillContent: data.fill,
            value: "",
            pickerName: "pick_" + data.key,
            slots: [],
            // slots: [{
            //     flex: 1,
            //     values: ["开发工程师", "岗位1", "岗位2", "岗位3", "岗位4"]
            // }],
            isShowPicker: false
          }
        ];

        // 多选
        if (data.columnType == "SYSTEMCODE") {
          // con[0].slots = data.slots;
          // if (!data.slots) {
          //   // con[0].slots = _commonJs.getSystemCodeJson(con[0], data.multable);
          //   con[0].slots
          // } else {
          //   con[0].slots = data.slots;
          // }
        }

        // 布尔
        if (data.columnType == "BOOLEAN") {
          let arr = [
            "",
            getIntl("option.boolean.yes"),
            getIntl("option.boolean.no")
          ];
          con[0].slots = [
            {
              flex: 1,
              valueIndex: 0,
              defaultIndex: 0,
              value: arr[0],
              valueKey: ["", true, false],
              values: arr
            }
          ];
        }

        break;
      case "choose":
        con = [
          {
            ...temp,
            // clickEvent: $this.showMember,
            // readonly: true,
            placeholder: temp.readonly
              ? ""
              : getIntl("placeholder.choose.text"),
            member: 1
          }
        ];
        break;
      // dataGrid类型
      case "datagrid":
        con = "";
        break;

      // 附件
      case "attachment":
        con = [
          {
            ...temp
          }
        ];
        break;

      // 图片
      case "picture":
        con = [
          {
            ...temp
          }
        ];
        break;

      default:
        con = [
          {
            ...temp
          }
        ];
        break;
    }

    if (data.complex) {
      commonFunc.getSlotsByFill(con[0], data.multable);
      // $this.getSlotsByFill(con[0], true);
    }

    // 参照
    if (temp.isPrefer) {
      con[0].clickEvent = new Function();
      con[0].referenceview = data.referenceview;
    }

    return con[0];
  }
};

// 构造condition
export const getdItemdition = commonFunc.getdItemdition;

// 绑定自定义事件
export const initConfigEvent = objThis => {
  if (objThis.eventBindConfig.length > 0) {
    let evt = objThis.eventBindConfig;
    for (let i = 0; i < evt.length; i++) {
      const elt = evt[i];
      if (elt.fbody && elt.fbody.indexOf("bapApi") > -1) {
        new Function(elt.split("bapApi.")[1])();
      } else if (elt.fbody && elt.fbody.indexOf("function") > -1) {
        _bapApi.bindEventConfig([elt]);
      }
    }
  }
};



// WEBPACK FOOTER //
// ./src/components/form/util.js