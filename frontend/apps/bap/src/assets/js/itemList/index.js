import Axios from 'axios'; //接口请求
import { Toast, Indicator } from "mint-ui";
var commonJS = {};
//获取样式配置信息
commonJS.getCssStyle = function(config, element) {
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
commonJS.getValueByKey = function(data, element, nameKey, isCallbackObj) {
    var key = element.key,
        getKeys,
        temp = "";

    if (nameKey) {
        key = nameKey;
    }

    try {
        getKeys = key;
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
        temp = eval("data." + getKeys);
        if (isBoolean) { //布尔型
            // if (!temp || temp == '') return "";// 值为空时            
            temp = temp ? "是" : "否";               
        } else if (isDate && element.showFormat) { //日期型
            if (!temp || temp == '') return "";// 值为空时
            if (element.columnType == "DATE" && element.showFormat == "SELECTCOMP") {
                element.showFormat = "YMD";
            }
            if (element.columnType == "DATETIME" && element.showFormat == "SELECTCOMP") {
                element.showFormat = "YMD_HMS";
            }
            //时间格式化
            temp = this.getDateFormat(element.showFormat, new Date(temp));
        } else if (isNumber && element.showFormat && !nameKey) { // 数字型
            temp = this.getNumberFormat(
                element.showFormat,
                temp,
                element.precision
            );
        } else if (isSystemCode) { // 系统编码
            if (!temp || temp == '') return "";// 值为空时
            if (element.isPrefer) { //参照系统编码
                if (eval(`data.${getKeys}ForDisplay`)) {
                    temp = eval(`data.${getKeys}ForDisplay`);
                } else if (temp && temp.value) {
                    temp = temp.value;
                }
            } else {
                if (eval(`data.${getKeys}ForDisplay`)) {
                    temp = eval(`data.${getKeys}ForDisplay`);
                } else if (temp && temp.value) {
                    if (isCallbackObj) {
                        temp = temp;
                    } else {
                        temp = temp.value;
                    }
                }
            }

        } else if (isMoney && element.showFormat && !nameKey) { // 金额
            if (temp || temp == 0 || temp == "0") { //解决金额为0时不显示的情况
                temp = this.formatMoney(
                    element.showFormat,
                    element.precision,
                    temp
                );
            } else { //值为空时,不包含0
                return "";
            }
        }

    } catch (error) {
        // console.log(error);
    }

    return temp;
};

commonJS.formatValue = function(data, element, nameKey, flag) {
    return this.getValueByKey(data, element, nameKey, flag);
}

commonJS.showFormat = function(data, element, index) {
    var value;
    if (element && element.showType && element.showType.indexOf('LABEL') > -1) {
        value = element.namekey;
    } else {
        value = this.getValueByKey(data, element);
    }

    // 是否是按照显示方法显示
    if (element.showFormatFunc) {
        let reset = new Function(
            `var value = '${value}';
            var nRow = ${index};
            ${element.showFormatFunc}
            `
        )();
        return reset;
    } else {
        return value;
    }
}

//获取自定义事件绑定  TODO 多个事件绑定
commonJS.getEventBindConfig = function(data, isQuery, isKey) {
    var ev = [];
    for (var i = 0; i < data.length; i++) {
        var item = data[i],
            code;

        if (isKey) {
            code = item.element ? item.element.key : '';
        } else {
            code = item.cellCode ? item.cellCode : '';
        }

        if (item.condition) {
            item = item.condition;
            code = item.name ? item.name : '';
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
                var ename = tempfname.replace(/\s/g, "").split("=")[0].split("\"").join(""); //获取事件名
                var fname = tempfname.replace(/\s/g, "").split("=")[1].split("\'").join(""); //获取函数名
                var fbody = tempfbody;
                ev.push({
                    "ename": ename,
                    "fname": fname,
                    "fbody": fbody,
                    "code": code
                });
            }
        }

        if (item.callbackname && item.callbackbody) { //绑定callback事件,事件名为"callback"
            var cbfname = item.callbackname.split("(")[0];//获取函数名
            ev.push({
                "ename": "callback",
                "fname": cbfname,
                "fbody": item.callbackbody,
                "code": code
            });
        }
    }
    return ev;
}

//数字型单位转换
commonJS.getNumberFormat = function(fmt, value, precision) {
    var unit = { PERCENT: "%" };
    if (value && precision) {
        value = Number(value).toFixed(Number(precision));
    }
    if (value == 0 || value == "0") { //判断数字为0的情况
        value = Number(value).toFixed(Number(precision));
    }
    if (value && fmt && unit[fmt]) {
        value = value + unit[fmt];
    }
    return value;
};
//金额格式化
commonJS.formatMoney = function(fmt, precision, decimal) {
    if (decimal || decimal == 0 || decimal == "0") {
        if (precision) {
            decimal = Number(decimal).toFixed(Number(precision));
        }
        if (Number(decimal)) {
            decimal = decimal.toString();
        }
    }
    decimal = decimal.replace(/\,/g, "");
    if (decimal != undefined && decimal != '') {
        var index = decimal.indexOf(".");
        var regax;
        if (fmt == "TEN_THOUSAND") { //万分位
            regax = /(\d{1,4})(?=(\d{4})+(?:$|\.))/g;
        } else { //千分位
            regax = /(\d{1,3})(?=(\d{3})+(?:$|\.))/g;
        }
        if (index === -1) {
            return decimal.replace(regax, "$1,");
        } else {
            return decimal.slice(0, index).replace(regax, "$1,").concat(decimal.slice(index));
        }
    } else {
        return decimal;
    }
}

//时间格式化
commonJS.getDateFormat = function(fmt, date) {
    if (!fmt || !date) {
        return '';
    }
    var transform = {
        YMD: "yyyy-MM-dd",
        YM: "yyyy-MM",
        Y: "yyyy",
        DEFAULT: "yyyy-MM-dd",
        YMD_HMS: "yyyy-MM-dd hh:mm:ss",
        YMD_HM: "yyyy-MM-dd hh:mm",
        YMD_H: "yyyy-MM-dd hh",
        MD_HM: "MM-dd hh:mm"
    };
    fmt = transform[fmt]; //获取转化格式
    return this.dateFtt(fmt, date);
};
//时间格式化
commonJS.dateFtt = function(fmt, date) {
    try {
        var o = {
            "M+": date.getMonth() + 1, //月份   
            "d+": date.getDate(), //日   
            "h+": date.getHours(), //小时   
            "m+": date.getMinutes(), //分   
            "s+": date.getSeconds(), //秒   
            "q+": Math.floor((date.getMonth() + 3) / 3), //季度   
            "S": date.getMilliseconds() //毫秒   
        };
        if (/(y+)/.test(fmt))
            fmt = fmt.replace(RegExp.$1, (date.getFullYear() + "").substr(4 - RegExp.$1.length));
        for (var k in o)
            if (new RegExp("(" + k + ")").test(fmt))
                fmt = fmt.replace(RegExp.$1, (RegExp.$1.length == 1) ? (o[k]) : (("00" + o[k]).substr(("" + o[k]).length)));
        return fmt;
    } catch (e) {
        console.log(e)
        return "";
    }
}

// 拼接参照url
commonJS.resolveReferUrl = function(url, data) {
    let refer = data.referenceview;
    let multiSelect = data.multable ? 1 : 0; // 默认单选
    let code = '';
    let clientType = '';
    let hideWebTitle = '';
    // url = "./staff.html"
    if (refer.code)
        code = refer.code.replace(/\./ig, '_');
    if (window._PAGECONFIG && window._PAGECONFIG.clientType) {
        clientType = window._PAGECONFIG.clientType;
    }
    if (window._PAGECONFIG && window._PAGECONFIG.hideWebTitle) {
        hideWebTitle = window._PAGECONFIG.hideWebTitle;
    }
    url = `${url}?isRefer=1&crossCompanyFlag=${refer.iscrosscompany}&condition=&callBackFuncName=_callBack_ec_${code}&multiSelect=${multiSelect}&closeFuncName=_close_ec_${code}&clientType=${clientType}&hideWebTitle=${hideWebTitle}`;
    return url;
}

// 构造筛选参照回填值
commonJS.referCallBackObject = function(list, condition) {
    // 取最后的值
    let keyName = condition[0].name.split('.');
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
        condition[0].filterId.columnName = "ID"
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
    condition[0].filterId.value = condition[0].valueId ? condition[0].valueId : condition[0].value;

    return condition;
}

// 获取参照更多值
commonJS.getReferMoreObj = function(values, sUrl, list, isPreferRefer) {
    const $this = this;
    if (values.id == undefined || values.id === '' || values.id == "请选择") {
        return { "id": "" };
    }

    var includes = ['id'];

    if (list) {
        Object.keys(list).forEach((key) => {
            if (list[key].columnType == "SYSTEMCODE") {
                includes.push(key + ".value");
            } else {
                includes.push(key);
            }
        })
    };

    if (includes.length == 0) {
        return false;
    };

    includes = includes.join(",");
    // includes += typeName + ",";
    // includes = includes.substr(0, includes.length - 1);
    var strArr = sUrl.split('');
    sUrl = strArr[0].substr(0, strArr[0].lastIndexOf('/')) + '/get' + strArr[1];
    if (sUrl.indexOf("?") > 0) {
        sUrl += "&id=" + values.id;
    } else {
        sUrl += "?id=" + values.id;
    }
    sUrl += "&includes=" + includes;
    Axios({
        url: sUrl,
        method: "post"
    }).then((res) => {
        let klist = res.data;
        if (klist) {
            for (const ki in list) {
                if (list.hasOwnProperty(ki)) {
                    const element = list[ki];
                    try {
                        let el = eval(`klist.${ki}`);
                        // if (el) {
                        //     element.value = el;
                        // }
                        $this.getValueByKeyEdit(element, klist, "", isPreferRefer);
                    } catch (error) {

                    }
                }
            }
        }
    });
}

/**
 * 根据key查找对应的数值-表单
 * @params {object} element
 * @params {object} newData 数据
 * @params {object} objThis vue组件对象(this)
 * @params {boolean} isPreferRefer 是否参照类型
 */
commonJS.getValueByKeyEdit = function(element, newData, objThis, isPreferRefer) {
    const $this = this;
    let data = objThis.editData;

    if (newData) {
        data = newData;
    }
    // 多选下拉赋值
    if (element && objThis.slotsAll && objThis.slotsAll[element.name]) {
        element.slots = objThis.slotsAll[element.name];
    }
    if (element && element.name && data) {
        let nameKey = '';
        nameKey = element.name;
        if (isPreferRefer) {
            var arr = (element.name).split(".");
            nameKey = arr[arr.length - 1];
        }
        element.value = $this.formatValue(data, element, nameKey, true);
        // element.value = element.value ? element.value.toString() : '';        
        if (element.value || element.value == 0 ||  element.value == "0") { //解决数据为0时不显示的问题
            element.value = element.value.toString();
        } else {
            element.value = '';
        }
        if (!isPreferRefer) {
            element.loadValue = element.value;
        }


        //数据为金额类型
        if (element.value && element.columnType == "MONEY") {
            let fmt = element.showFormat;
            let precision = element.precision;
            let decimal = element.value;
            element.valueFormat = $this.formatMoney(fmt, precision, decimal);
            return;
        }

        // 数据为对象型
        if (element.value && element.value.id) {
            let { id, value } = element.value;
            element.valueId = id;
            element.value = value;
            return;
        }

        // 小数 精度显示类型
        if (element.precision) {
            let decimal = element.value;
            if ($this.isDecimal(decimal)) {
                decimal = Number(decimal).toFixed(Number(element.precision)).toString();
            } else {
                decimal = '';
            }
            element.value = decimal;
        }

        // complex
        if (element.complex) {
            let tempVid = $this.formatValue(data, element, `extraColMap._complex_${nameKey}`);
            if (element.multable && tempVid && element.slots && element.slots[0]) { // 多选
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

            } else if (tempVid && element.slots && element.slots[0] && element.slots[0].valueKey) { // 单选
                element.valueId = tempVid;
                let valueIndex = element.slots[0].valueKey.indexOf(element.valueId);
                element.value = element.slots[0].values[valueIndex];
            } else {
                element.value = tempVid;
            }
            // element.value = $this.formatValue(data, element, `extraColMap._complex_${nameKey}`);
            return;
        }

        // 时间类型
        if (element.type == "date") {

            return;
        }

        // 多选控件
        if (element.columnType == "MULTSELECT") {
            let muiltiNames = $this.formatValue(data, element, `${nameKey}multiselectNames`);
            let muiltiIds = $this.formatValue(data, element, `${nameKey}multiselectIDs`);
            element.oldMultiLists = {
                values: muiltiNames && muiltiNames.split(","),
                ids: muiltiIds && muiltiIds.split(",")
            }
            element.newMultiLists = {
                values: muiltiNames && muiltiNames.split(","),
                ids: muiltiIds && muiltiIds.split(",")
            };
            return;
        }

        // 多选
        if (element.type == "select" && element.multable) {
            let values = $this.formatValue(data, element, nameKey);
            values = values ? values.split(",") : [];
            let valueKeys = [],
                valuesIds = [];
            for (let j = 0; j < element.slots.length; j++) {
                var et = element.slots[j];
                for (let i = 0; i < values.length; i++) {
                    var elt = values[i];
                    if (elt == et.value || elt == et.label) {
                        valueKeys.push(et.label);
                        valuesIds.push(et.value);
                    }
                }

            }

            element.value = valueKeys.join(",");
            element.valueId = valuesIds;

            return;

        }

        // Boolean
        if (element.columnType == "BOOLEAN") {
            try {
                element.valueId = eval(`data.${nameKey}`);
            } catch (error) {

            }
            return;
        }

        // 图片
        if (element.type == 'picture') {
            let imgId = $this.formatValue(data, element, `${nameKey}Document.id`);
            let entityCode = objThis.urlParams ? objThis.urlParams.entityCode : "";
            if (imgId && entityCode) {
                element.value = `/foundation/workbench/download?id=${imgId}&entityCode=${entityCode}`;
                element.valueId = imgId;
                if (objThis.$refs[nameKey]) objThis.$refs[nameKey][0].imgList = [element.value];
            }

            return;
        }

        // 单选
        if (!element.isPrefer && element.slots && element.type == "select") {
            element.value = $this.formatValue(data, element, nameKey);
            if (element.value && element.slots[0] && element.slots[0].values) {
                let valueIndex = element.slots[0].values.indexOf(element.value);
                element.valueId = element.slots[0].valueKey[valueIndex];
            }

            return;

        }

        // 参照
        if (element.isPrefer || element.type == "select") {
            let index = nameKey.lastIndexOf(".");
            let idName;
            if (index > -1) {
                idName = nameKey.substring(0, index);
                idName = `${idName}.id`;
            } else {
                idName = `id`;
            }

            element.valueId = $this.formatValue(data, element, idName);
        }


    }

}

/**
 * 公用有效性验证
 * 验证字符、数字、长度、金额、日期的字段 
 */
commonJS.isInteger = function(thevalue) {
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

commonJS.isLong = function(thevalue) {
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

commonJS.isDecimal = function(thevalue) {
    const reg = /^[-+]?\d+\.*\d*$/;
    if (!reg.test(thevalue)) {
        return false;
    } else {
        return true;
    }
};

commonJS.isDate = function(thevalue) {
    const reg = /^(\d{4})(-|\/)(\d{2})\2(\d{2})$/;
    if (!reg.test(thevalue)) {
        return false;
    } else {
        return true;
    }
    return true;
};

commonJS.isDateTime = function(thevalue) {
    const reg = /^(?:(?:1[6-9]|[2-9]\d)?\d{2}[\-](?:0[1,3-9]|1[0-2])[\-](?:29|30))(?: (?:0\d|1\d|2[0-3]))?(?:\:(?:0\d|[1-5]\d))?(?:\:(?:0\d|[1-5]\d))?$|^(?:(?:1[6-9]|[2-9]\d)?\d{2}[\-](?:0[1,3,5,7,8]|1[02])[\-]31)(?: (?:0\d|1\d|2[0-3]))?(?:\:(?:0\d|[1-5]\d))?(?:\:(?:0\d|[1-5]\d))?$|^(?:(?:1[6-9]|[2-9]\d)?(?:0[48]|[2468][048]|[13579][26])[\-]02[\-]29)(?: (?:0\d|1\d|2[0-3]))?(?:\:(?:0\d|[1-5]\d))?(?:\:(?:0\d|[1-5]\d))?$|^(?:(?:16|[2468][048]|[3579][26])00[\-]02[\-]29)(?: (?:0\d|1\d|2[0-3]))?(?:\:(?:0\d|[1-5]\d))?(?:\:(?:0\d|[1-5]\d))?$|^(?:(?:1[6-9]|[2-9]\d)?\d{2}[\-](?:0[1-9]|1[0-2])[\-](?:0[1-9]|1\d|2[0-8]))(?: (?:0\d|1\d|2[0-3]))?(?:\:(?:0\d|[1-5]\d))?(?:\:(?:0\d|[1-5]\d))?$/;
    if (!reg.test(thevalue)) {
        return false;
    } else {
        return true;
    }
    return true;
};

commonJS.checklong = function(thevalue, checklen) {
    var iLen = 0;
    var tmpValue = thevalue.replace(/^\s+|\s+$/g, "");
    for (var j = 0; j < tmpValue.length; j++) {
        iLen += (tmpValue.charCodeAt(j) > 127) ? 3 : 1;
    }
    if (iLen > parseInt(checklen)) {
        return false;
    } else {
        return true;
    }
    return true;
};

/**
 * 最少3位，首位字母，其余字符、数字、下划线或.
 */
commonJS.isValidateIdentifier = function(thevalue) {
    var validate = /^[a-zA-Z_]{1}[a-zA-Z0-9_\.]+[a-zA-Z0-9_]+$/;
    if (validate.test(thevalue)) {
        return true;
    } else {
        return false
    }
};

/**
 * 最少2位，首位字母，其余字符、数字、下划线或.
 */
commonJS._isValidateIdentifier = function(thevalue) {
    var validate = /^[a-zA-Z_]{1}[a-zA-Z0-9_\.]+$/;
    if (validate.test(thevalue)) {
        return true;
    } else {
        return false
    }
};


/**
 * 判断当前安卓手机版本是否小于等于某个版本号
 * @params  [version][需要比较的版本号]
 * @returns
 */
commonJS.isLessEqualAndroidVersion = function (version) {
    var u = window.navigator.userAgent;
    var isAndroid = u.indexOf('Android') > -1 ||  u.indexOf('Adr') > -1;
    if (!isAndroid) {
        return false;
    }
    var androidVersionRes = u.match(/Android (\d+).* /);
    return androidVersionRes && androidVersionRes[1] ? +androidVersionRes[1] <= version : false
};

/**
 * 获取格式起始和结束时间
 * @params {object} timeThis vue组件对象(this)
 * @params {time} startyear 起始时间
 */
commonJS.startAndEndTime = function(timeThis, startyear) {
    (function() {
        const curtime = new Date();
        const year = curtime.getFullYear();
        const month = curtime.getMonth();
        const date = curtime.getDate();
        const start = new Date(`${startyear}/${month+1}/${date}`);
        const end = new Date(`${year + 100}/${month+1}/${date}`);
        timeThis.startdate = start;
        timeThis.enddate = end;
        timeThis.curstart = start;
        timeThis.curend = end;
    })();
};

/**
 * 显示下拉框
 * @params {object} vThis vue组件对象(this)
 * @params {object} data 字段数据对象
 */
commonJS.showDropdown = function(vThis, data) {
    if (data.readonly) {
        return false;
    }
    // 下拉接口
    if (data.slots.length == 0 && data.fillContent && data.fillContent.fillType == 3 && !data.multable) { // 系统编码
        vThis.getData({
            url: "/foundation/systemCode/systemCodeJson.action",
            type: "post",
            data: {
                systemEntityCode: data.fillContent.fillContent
            }
        }, function(res) {
            if (res) {
                let arr = [""],
                    keys = [""],
                    multables = [];
                for (const key in res) {
                    if (res.hasOwnProperty(key)) {
                        const element = res[key];
                        arr.push(element);
                        keys.push(key);
                        multables.push({ label: element, value: key });
                    }
                }
                if (data.multable) { // 多选
                    data.slots = multables;
                } else { // 单选下拉
                    data.slots = [{
                        flex: 1,
                        valueIndex: 0,
                        defaultIndex: 0,
                        value: arr[0],
                        valueKey: 0,
                        values: arr,
                        valueKey: keys
                    }];
                }
                data.isShowPicker = true;
            }
        });
        data.isShowPicker = true;
    } else {
        data.isShowPicker = true;
        vThis.$nextTick(function() {
            let name = vThis.params.name;
            let value = vThis.params.value;
            if (value) {
                vThis.$refs[name].$refs["pick_" + name].setValues([value]);
            }
        });
    }
};

/**
 * 打开系统编码树形界面,并加载系统编码数据
 * @params {object} vThis vue组件对象(this)
 * @params {object} data 字段数据对象
 * @params {boolean} isPagelist 是否列表视图，列表视图系统编码树形多选和单选一样
 */
commonJS.openSystemCodeInterface = function(vThis, data, isPagelist) {
    if (data.readonly) {
        return false;
    }
    let url;
    let fillContent;
    if (data.fillContent) {
        fillContent = data.fillContent.fillContent;
    } else if (data.systemCode) {
        fillContent = data.systemCode.fillContent;
    }
    url = "/foundation/systemCode/codeValueManager/valueTreeList.action" + `?systemEntityCode=${fillContent}`;
    // url = "../static/data/systemcode.json"; //系统编码数据                
    vThis.getData({
        url: url,
        type: "post",
        data: {
            id: -1
        }
    }, function (treeData) {
        if (treeData) {
            vThis.$refs.systemCodeTree.data = treeData;
            vThis.$refs.systemCodeTree.isShowSystemCode = true;
            if (data.multable && !isPagelist) { //树形多选
                vThis.$refs.systemCodeTree.showCheckbox = true;
            } else { //树形单选
                vThis.$refs.systemCodeTree.showCheckbox = false;
            }
        }
    });
    vThis.$forceUpdate();
};

/**
 * 删除参照类型字段的值和参照关联的值
 * @params {object} vThis vue组件对象(this)
 * @params {object} data 字段数据对象
 */
commonJS.deleteReferValues = function(vThis, data) {
    let inputRef = vThis.params.name;
    if (typeof data.value == "string") {
      data.valueId = "";
      data.value = "";
      document.getElementsByName(inputRef).value = "";
    }
    if (data.isPrefer) { // 删除参照关联的值
        var endName = data.name.split(".");
        endName = endName[endName.length - 1];
        var sourcename = data.name.replace(endName, "");
        var layout = vThis.$parent.$parent.layout;
        for (let i = 0; i < layout.length; i++) {
            const element = layout[i];
            for (let j = 0; j < element.length; j++) {
                const li = element[j];
                const condition = li.condition[0];
                if (condition && condition.name && condition.name.indexOf(sourcename) > -1) {
                    if (typeof condition.value == "string") {
                        condition.value = "";
                        condition.valueId = "";
                        document.getElementsByName(condition.name).value = ""; 
                    }
                } else if (li.datagrid) {
                    if (data.datagridCode == li.datagrid.datagridCode) {
                        let dtEl = data.index == 0 || data.index ? li.datagrid.elements[data.index] : li.datagrid.elements;
                        for (let k = 0; k < dtEl.length; k++) {
                            const dtli = dtEl[k];
                            const dtCondition = dtli.condition;
                            if (dtCondition.name && dtCondition.name.indexOf(sourcename) > -1) {
                                if (typeof dtCondition.value == "string") {
                                    dtCondition.value = "";
                                    dtCondition.valueId = "";
                                    document.getElementsByName(dtCondition.name).value = ""; 
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    vThis.$forceUpdate();
};

/**
 * 打开参照视图界面
 * @params {object} vThis vue组件对象(this)
 * @params {object} data 字段数据对象
 */
commonJS.OpenReferViewSelect = function(vThis, data) {
    const $this = this;
    if (data.readonly) {
        return false;
    }

    // 获取参照参数，跨公司 回填到url
    if (!data.referenceview.url && data.referenceview.url == "") {
        vThis.myToast("无效地址");
        return false;
    }
    if (vThis.$root.$children["0"].$refs.RightBtn.hidden) {
        vThis.$root.$children["0"].$refs.RightBtn.isRefer = true; //判断悬浮按钮是否显示        
    }
    // this.showRefer = true;

    vThis.referConfig.url = $this.resolveReferUrl(data.referenceview.url, data); //拼接参照url
    vThis.updateRefer(true, vThis.referConfig.url);
    Indicator.open({
        text: "加载中...", //文字
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

                    setTimeout(function() {
                        vThis.showRefer = false;
                        vThis.updateRefer(false);
                    }, 200);

                    if (vThis.$root.$children["0"].$refs.RightBtn.hidden) {
                        vThis.$root.$children["0"].$refs.RightBtn.isRefer = false; //判断悬浮按钮是否显示
                        vThis.$root.$children["0"].$refs.RightBtn.dragButton();     
                    }

                    // 回调赋值
                    // data = $this.referCallBackObject(list, [data]);
                    if (list && list.length > 0) {
                        if (data.columnType == "MULTSELECT") { //多选
                            data = vThis.getReferDataCallBack(list, data);
                            return true;
                        }
                        list = list[0];
                    }

                    var endName = data.name.split(".");
                    endName = endName[endName.length - 1];
                    var sourcename = data.name.replace(endName, "");
                    let listcon = {},
                        listParams = [];

                    // 参照关联赋值
                    var layout = vThis.$parent.$parent.layout;
                    for (let i = 0; i < layout.length; i++) {
                        const element = layout[i];
                        for (let j = 0; j < element.length; j++) {
                            const li = element[j];
                            if (li.condition[0] && li.condition[0].name && li.condition[0].name.indexOf(sourcename) > -1) {
                                var tempName = li.condition[0].name.replace(`${sourcename}`, "");
                                try {
                                    if (eval(`list.${tempName}`)) {
                                        // li.condition[0].value = eval(`list.${tempName}`);
                                        // 参照取id值
                                        // li.condition[0].valueId = vThis.getReferValueId(list, 'id');
                                        $this.getValueByKeyEdit(li.condition[0], list, "", true);
                                    } else {
                                        listcon[tempName] = li.condition[0];
                                    }
                                } catch (error) {
                                    listcon[tempName] = li.condition[0];
                                }
                            } else if (li.datagrid) {
                                if (data.datagridCode == li.datagrid.datagridCode) {
                                    let dtEl = data.index == 0 || data.index ? li.datagrid.elements[data.index] : li.datagrid.elements;
                                    for (let k = 0; k < dtEl.length; k++) {
                                        const dtli = dtEl[k];
                                        if (dtli.condition.name && dtli.condition.name.indexOf(sourcename) > -1) {
                                            var tempName = dtli.condition.name.replace(`${sourcename}`, "");
                                            try {
                                                if (eval(`list.${tempName}`)) {
                                                    // dtli.condition.value = eval(`list.${tempName}`);
                                                    // 参照取id值
                                                    // dtli.condition.valueId = vThis.getReferValueId(list, 'id');
                                                    $this.getValueByKeyEdit(dtli.condition, list, "", true);
                                                } else {
                                                    listcon[tempName] = dtli.condition;
                                                }
                                            } catch (error) {
                                                listcon[tempName] = dtli.condition;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    let isPreferRefer = true;
                    // 请求参照关联值
                    $this.getReferMoreObj(list, data.referenceview.url, listcon, isPreferRefer);

                    //对象类型的字段绑定执行beforecallback方法和callback方法
                    vThis.$nextTick(function() {
                        let eventBindConfig = $this.getEventBindConfig([data], true, true); //获取字段的事件配置
                        if (eventBindConfig.length > 0) {
                            for (let m = 0; m < eventBindConfig.length; m++) {
                                const elt = eventBindConfig[m];
                                if (elt.ename && elt.ename == "beforecallback") { //执行beforecallback方法
                                    if (elt.fbody && elt.fbody.indexOf("function") > -1) {
                                        var obj = list;
                                        var exbefore = {getbeforecbplay: eval("(" + elt.fbody + ")")};
                                        exbefore.getbeforecbplay(obj);
                                    }
                                }

                                if (elt.ename && elt.ename == "callback") { //执行callback方法
                                    if (elt.fbody && elt.fbody.indexOf("function") > -1) {
                                        var obj = list;
                                        var ex = {getcbplay: eval("(" + elt.fbody + ")")};
                                        ex.getcbplay(obj);
                                    }
                                }
                            }
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
 * 打开附件列表界面
 * @params {object} vThis vue组件对象(this)
 */
commonJS.openAppendixListInterface = function(vThis) {
    let url;
    let entityCode = vThis.urlParams ? vThis.urlParams.entityCode : "";
    let linkId = vThis.urlParams ? vThis.urlParams.tableInfoId : "";
    url = "/foundation/workbench/upload-list.action" + `?linkId=${linkId}&entityCode=${entityCode}&type=Table&__res_html=true`;
    // url = "../static/data/upload.json"; //附件列表数据                
    vThis.getData({
        url: url,
        type: "get",
    }, function (res) {
        if (res.result) {
            vThis.$refs.appendix.appendixList = res.result;
            vThis.popupVisible = false;
            vThis.$refs.appendix.isShowAppendix = true;
        }
    });
    vThis.$forceUpdate();
};

/**
 * 获取附件个数
 * @params {object} vThis vue组件对象(this)
 */
commonJS.getAppendixNums = function(vThis) {
    let linkId = vThis.urlParams ? vThis.urlParams.tableInfoId : "";
    var url = "/foundation/workbench/upload-count.action" + `?linkId=${linkId}&type=Table`;
    vThis.getData({
        url: url,
        type: "get",
    }, function (res) {
        if (res || res === 0) {
            vThis.AppendixNum = res;
        }
    });
};


export default commonJS;


// WEBPACK FOOTER //
// ./src/assets/js/itemList/index.js