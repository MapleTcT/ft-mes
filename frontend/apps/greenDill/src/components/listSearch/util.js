import { Indicator } from "mint-ui";
import _commonJs from "@/assets/js/itemList/index.js";
import eventMap from "../../assets/js/util/eventMap.js";
import { isRefenceComp } from '@/assets/js/util/common.js';

//获取查询类型
export const getQueryType = data => {
    const { showType, columnType } = data;
    let type;
    if (showType == "DATE" || showType == "DATETIME") {
        type = "date";
    } else if (showType == "MONEY") {
        type = "section";
    } else if (showType == "SELECTCOMP" || showType == "SELECT") {
        type = "select";
    } else if (showType == "choose") {
        type = "choose";
    } else if (
        columnType == "INTEGER" ||
        columnType == "LONG" ||
        columnType == "DECIMAL" ||
        columnType == "MONEY"
    ) {
        type = "number";
    } else {
        type = "text";
    }
    return type;
};

/**
 * 绑定自定义事件
 * @param {funcname} value - 方法名
 * @param {funcbody} value - 方法体
 */
export const getCustomFunc = ({ funcname, funcbody }) => {
    const events = {};
    if (funcname && funcbody) {
        const funcNameArr = funcname.split(' ');
        const funcBodyArr = funcbody.split('@@@@');
        if (funcNameArr && funcNameArr.length > 0) {
            funcNameArr.forEach((func, index) => {
                const name = func.split('=')[0];
                const bodyStr =
                    (funcBodyArr[index] && funcBodyArr[index].replace(/\r\n/g, '')) || '';
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
}

export const getType = (el = {}) => {
    const { type, columnType, isTreeSystemCode, isPrefer } = el;
    const isTreeSelect = columnType == 'SYSTEMCODE' && isTreeSystemCode;
    const isSelect = type == 'select' && !isTreeSystemCode;
    let compType = '';
    if (isTreeSelect) compType = 'treeSelect';
    else if (type === 'number') compType = 'number';
    else if (type === 'date') compType = 'date';
    else if (isPrefer) compType = 'reference';
    else if (isSelect) compType = 'select';
    return compType;
}

const showMember = function() {
    let curPage = "";
    let text = item.placeholder;
    if (text.indexOf(this.$t('searchBar.reference.staff')) > -1) {
        curPage = "member";
    } else if (text.indexOf(this.$t('searchBar.reference.department')) > -1) {
        curPage = "department";
    } else if (text.indexOf(this.$t('searchBar.reference.position')) > -1) {
        curPage = "station";
    }
    this.$store.commit("updateData", {
        name: "curShowPage",
        value: curPage
    });
}

// 打开参照
export const openRefSelect = function(data = {}) {
    if (data.readonly && data.placeholder == "") { //配置页面设置只读
        return false;
    }
    // this.myToast("开发中");
    const $this = this;
    if (data && !data.referenceview.url && data.referenceview.url == "") {
        this.myToast(this.$t('notice.invalid.address'));
        return false;
    }
    this.showRefer = true;
    $this.referConfig.url = _commonJs.resolveReferUrl(
        data.referenceview.url,
        data
    );
    Indicator.open({
        text: this.$t('notice.loading'),
        //文字
        spinnerType: "fading-circle"
            //样式
    });
    this.$nextTick(function() {
        // 回调方法
        let iframe = document.getElementById("newPage");
        iframe.onload = function() {
            Indicator.close();
            if (iframe) {
                // 注册方法
                // 去掉原生头部
                // try {
                //     window.mobilejs.open_dialog();
                // } catch (err) {}
                // 选择回调方法
                let code = "";
                if (data.referenceview.code)
                    code = data.referenceview.code.replace(/\./gi, "_");
                iframe.contentWindow[`_callBack_ec_${code}`] = function(list) {
                    // 回调赋值
                    try {
                        $this.condition = _commonJs.referCallBackObject(
                            list, [data]
                        );
                    } catch (error) {

                    }

                    // $this.showRefer = false;
                    setTimeout(function() {
                        $this.showRefer = false;
                    }, 200);
                    // 恢复原生头部
                    // try {
                    //     window.mobilejs.close_dialog();
                    // } catch (err) {}
                };

                // 返回按钮方法
                iframe.contentWindow[`_close_ec_${code}`] = function() {
                    // 关闭iframe
                    // $this.showRefer = false;
                    setTimeout(function() {
                        $this.showRefer = false;
                    }, 200);
                    // 恢复原生头部
                    // try {
                    //     window.mobilejs.close_dialog();
                    // } catch (err) {}
                };
            }
        };
    });
}

// 构造condition对象
export const getQueryCondition = function(item) {
    const data = item.element;
    const queryType = getQueryType(data);
    //显示下拉
    const showPickerPop = (data) => {
        _commonJs.showDropdownList(this, data);
    };
    let con = [];
    const temp = {
        type: queryType,
        placeholder: "",
        clickEvent: "",
        readonly: data.readonly,
        value: "",
        showformat: data.showFormat,
        code: data.code,
        exp: data.exp,
        layRec: data.layRec,
        name: data.name,
        systemCode: data.fill,
        columnType: data.columnType,
        columnName: data.columnName,
        isPrefer: isRefenceComp(data), // 是否是参照
        multable: data.multable,
        cellCode: data.cellCode,
        namekey: data.namekey,
        isTreeSystemCode: data.isTreeSystemCode, //是否系统编码树形
        isOnlyLeaf: data.isOnlyLeaf, //系统编码树形单选是否仅可选叶子节点
        asscolumnname: item.asscolumnname,
        customFunc: getCustomFunc(item),
        defaultValue: data.defaultValue,
        fill: data.fill,
        cssstyle: data.cssstyle
    };
    const pSpace = this.$i18n.locale === 'zh' ? '' : " "; // 占位符空格
    switch (queryType) {
        case "date":
            var transform = {
                YMD: "date",
                YM: "date",
                Y: "date",
                DEFAULT: "date",
                YMD_HMS: "datetime",
                YMD_HM: "datetime",
                YMD_H: "datetime"
            };

            var transformat = { //为了实现查询时拼接完成的日期和时间
                YMD: "date",
                YM: "yearMonth",
                Y: "year",
                DEFAULT: "date",
                YMD_HMS: "datetime",
                YMD_HM: "datetimehm",
                YMD_H: "datetimeh"
            };

            con = [{
                    ...temp,
                    readonly: true,
                    name: `${data.name}_start`,
                    placeholder: temp.readonly ? "" : this.$t('placeholder.startTime.text'),
                    pickerName: `${data.key}_pickerStart`,
                    datetype: transform[data.showFormat],
                    datatypeformat: transformat[data.showFormat]
                },
                {
                    ...temp,
                    readonly: true,
                    name: `${data.name}_end`,
                    placeholder: temp.readonly ? "" : this.$t('placeholder.endTime.text'),
                    pickerName: `${data.key}_pickerEnd`,
                    datetype: transform[data.showFormat],
                    datatypeformat: transformat[data.showFormat]
                }
            ];
            break;
        case "text":
            con = [{...temp, placeholder: temp.readonly ? "" : this.$t('placeholder.enter.text') + pSpace + data.namekey }];
            break;
        case "section":
            con = [{...temp, placeholder: "", value1: "", value2: "" }];
            break;
        case "number":
            con = [{
                    ...temp,
                    name: `${data.name}_start`,
                    placeholder: temp.readonly ? "" : this.$t('placeholder.enter.text') + pSpace + data.namekey,
                    pickerName: `${data.key}_pickerStart`
                },
                {
                    ...temp,
                    name: `${data.name}_end`,
                    placeholder: temp.readonly ? "" : this.$t('placeholder.enter.text') + pSpace + data.namekey,
                    pickerName: `${data.key}_pickerEnd`
                }
            ];
            break;
        case "select":
            con = [{
                ...temp,
                clickEvent: showPickerPop,
                readonly: true,
                placeholder: temp.readonly ? "" : this.$t('placeholder.choose.text') + pSpace + data.namekey,
                value: "",
                pickerName: "pick_" + data.key,
                slots: [],
                isShowPicker: false
            }];

            // 参照
            if (temp.isPrefer) {
                con[0].clickEvent = new Function();
                con[0].referEvent = openRefSelect.bind(this);
                con[0].referenceview = data.referenceview;
                con[0].isPrefer = true;
                con[0].readonly = data.readonly;
            }

            // 布尔
            if (data.selfType == "BOOLEAN") {
                let arr = [this.$t('option.boolean.yes'), this.$t('option.boolean.no')];
                con[0].slots = [{
                    flex: 1,
                    valueIndex: 0,
                    defaultIndex: 0,
                    value: arr[0],
                    valueKey: 0,
                    values: arr
                }];
                con[0].selectData = ["1", "0"];
            }

            break;
        case "choose":
            con = [{
                ...temp,
                clickEvent: showMember.bind(this),
                readonly: true,
                placeholder: temp.readonly ? "" : this.$t('placeholder.choose.text') + pSpace + data.namekey.substring(0, 2),
                member: 1
            }];
            break;
    }
    return con;
}


// WEBPACK FOOTER //
// ./src/components/listSearch/util.js