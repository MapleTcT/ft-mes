<template>
    <div class="m-sidePanel">
        <htmlPanel
            v-if="showRefer"
            :params="{url:referConfig.url}"
        ></htmlPanel>
        <div
            class="m-layout"
            @click="propCloseSidePanel"
        ></div>
        <div class="m-panel">
            <div class="mp-content">
                <div
                    class="mp-box"
                    v-for="(item,i) in sortData"
                >
                <h3 class="mp-tit">{{item.dgname}}</h3>
                <div class="mp-tablist">
                    <ul v-for="(row,j) in item.sortTemp">
                        <li
                            v-for="(cell,k) in row"
                            v-on:click="handleClick"
                        >
                            <a
                                :name="cell.code"
                                :class="{'selected':selectItem.indexOf(cell.code)>-1}"
                            >{{cell.name}}</a>
                        </li>
                    </ul>
                </div>

                </div>
                <div
                    class="mp-box"
                    v-for="(item,i) in queryData"
                >
                <h3 class="mp-tit">{{item.name}}</h3>
                <div
                    class="mp-con"
                    v-for="(con,j) in item.condition"
                >
                    <searchInput :ref="con.pickerName?con.pickerName:con.name" :params="con" curShow='side'></searchInput>
                    
                </div>
                </div>
                
                <!-- <i class="split-line"></i>
                <div class="mp-box">
                    <h3 class="mp-tit">自定义</h3>
                    <a class="mp-arrow open"></a>
                    <div class="custom-box">
                        <div class="cb-box">
                            <div class="pub-select">
                                <label for="">请假天数</label>
                            </div>
                            <input class="cb-inp" type="text">
                            <i class="cb-delect"></i>
                        </div>
                        <div class="cb-box">
                            <div class="pub-select">
                                <label for="">请选择</label>
                            </div>
                            <input class="cb-inp" type="text">
                            <i class="cb-delect"></i>
                        </div>
                        <a class="cb-add"><img class="icon" src="../assets/images/icon_new2.png">新增</a>
                    </div>
                </div> -->
            </div>
            <div class="mp-foot">
                <a
                    class="m_button btn_reset"
                    @click="resetClassQuery"
                >重置</a>
                <a
                    class="m_button btn_finish"
                    v-on:click="getSearchData"
                >完成</a>
            </div>
        </div>
    </div>
</template>

<script>
import htmlPanel from "@/components/htmlPanel"; // iframe
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import filtration from "@/assets/js/util/filtration"; // 搜索筛选
import { Toast, Indicator } from "mint-ui";
import searchInput from "@/components/listSearch/searchInput"
export default {
    name: "sidePanel",
    data() {
        return {
            selectItem: [],
            sortData: [],
            dateVal: "",
            queryData: [],
            eventBindConfig: [],
            startdate: "",
            enddate: "",
            referConfig: {},
            showRefer: false
        };
    },
    props: ["propCloseSidePanel", "propSearchConfigData"],
    //数据监听
    watch: {
        propSearchConfigData: function(newVal, oldVal) {
            let $this = this;
            this.queryData = this.getQueryData(newVal.query);
            this.sortData = this.getSortData(newVal.dataclassify);
            console.log(this.queryData);
            $this.eventBindConfig = _commonJs.getEventBindConfig(newVal.query, true); //获取额外的事件绑定配置

            $this.$nextTick(function() {
                // 判断是否为api里的事件
                if ($this.eventBindConfig.length > 0) {
                    let evt = $this.eventBindConfig;
                    for (let i = 0; i < evt.length; i++) {
                        const elt = evt[i];
                        if (elt.fbody && elt.fbody.indexOf("bapApi") > -1) {
                            new Function(elt.split("bapApi.")[1])();
                        } else if (elt.fbody && elt.fbody.indexOf("function") > -1) {
                            _bapApi.bindEventConfig([elt], "searchPanel");
                        }
                    }
                }
            });
        }
    },
    mounted() {
        const $this = this;
        // 注册全局方法
        // $this.exportFunc("setDefaultValue", $this.setDefaultValue);

    },
    methods: {
        //获取分类数据
        getSortData: function(data) {
            var $this = this;
            if (data && data.length > 0) {
                for (var i = 0; i < data.length; i++) {
                    var item = data[i];
                    item.sortTemp = $this.getTemp(item.dgvalue);
                    item.dgvalue && $this.getDefault(item.dgvalue); //默认选中的条件
                }
            }
            return data;
        },
        //点击事件
        handleClick: function(e) {
            if (e.target.nodeName.toLowerCase() === "a") {
                let li = e.target.parentNode.parentNode.children;
                const tag = e.target.name;
                this.clearSiblings(li, tag);
                var dtIndex = this.selectItem.indexOf(tag);
                if (dtIndex > -1) {
                    this.selectItem.splice(dtIndex, 1);
                } else {
                    this.selectItem.push(tag);
                }
            }
        },

        // 单选 清空兄弟节点选中
        clearSiblings: function(siblings, tagName) {
            if (siblings) {
                for (let i = 0; i < siblings.length; i++) {
                    const element = siblings[i].childNodes;
                    if (element && element[0].name && element[0].name != tagName) {
                        let tname = element[0].name;
                        let dtIndex = this.selectItem.indexOf(tname);
                        if (dtIndex > -1) {
                            this.selectItem.splice(dtIndex, 1);
                        }
                    }
                }
            }
        },

        //获取模板
        getTemp: function(list) {
            if (!list) {
                return "";
            }
            var arrTemp = [];
            var index = 0;
            var sectionCount = 3;
            for (var i = 0; i < list.length; i++) {
                index = parseInt(i / sectionCount);
                if (arrTemp.length <= index) {
                    arrTemp.push([]);
                }
                arrTemp[index].push(list[i]);
            }
            return arrTemp;
        },
        //获取默认选中
        getDefault: function(data) {
            var $this = this;
            for (var i = 0; i < data.length; i++) {
                var item = data[i];
                if (item.isDefault == "true") {
                    $this.selectItem.push(item.code);
                }
            }
        },
        //获取查询条件
        getQueryData: function(data) {
            var $this = this;
            var queryList = [];
            for (var i = 0; i < data.length; i++) {
                var item = data[i].element;
                item.cellCode = data[i].cellCode;
                var queryCon = $this.getQueryCondition(data[i]);
                var info = {
                    name: item.namekey,
                    condition: queryCon
                };
                queryList.push(info);
            }
            return queryList;
        },
        //获取查询条件配置信息
        getQueryCondition: function(item) {
            const data = item.element;
            var $this = this;
            var queryType = $this.getQueryType(data.showType);
            var con = [];
            var temp = {
                type: queryType,
                placeholder: "请选择开始时间",
                clickEvent: "",
                readonly: false,
                value: "",
                showformat: data.showFormat,
                code: data.code,
                exp: data.exp,
                layRec: data.layRec,
                name: data.name,
                systemCode: data.fill,
                columnType: data.columnType,
                columnName: data.columnName,
                isPrefer: false,
                multable: data.multable,
                cellCode: data.cellCode,
                namekey: data.namekey,
                isTreeSystemCode: data.isTreeSystemCode, //是否系统编码树形
                isOnlyLeaf: data.isOnlyLeaf, //系统编码树形单选是否仅可选叶子节点
                asscolumnname: item.asscolumnname
            };
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

                    con = [
                        {
                            ...temp,
                            // clickEvent: $this.selectDate,
                            readonly: true,
                            name: `${data.name}_start`,
                            placeholder: "请选择开始时间",
                            pickerName: `${data.key}_pickerStart`,
                            datetype: transform[data.showFormat],
                            datatypeformat: transformat[data.showFormat]
                        },
                        {
                            ...temp,
                            // clickEvent: $this.selectDate,
                            readonly: true,
                            name: `${data.name}_end`,
                            placeholder: "请选择结束时间",
                            pickerName: `${data.key}_pickerEnd`,
                            datetype: transform[data.showFormat],
                            datatypeformat: transformat[data.showFormat]
                        }
                    ];
                    break;
                case "text":
                    con = [{ ...temp, placeholder: "请输入" + data.namekey }];
                    break;
                case "section":
                    con = [{ ...temp, placeholder: "", value1: "", value2: "" }];
                    break;
                case "select":
                    con = [
                        {
                            ...temp,
                            clickEvent: $this.showPickerPop,
                            readonly: true,
                            placeholder: "请选择" + data.namekey,
                            value: "",
                            pickerName: "pick_" + data.key,
                            slots: [],
                            isShowPicker: false
                        }
                    ];

                    // 参照
                    if (data.isrefselect) {
                        con[0].clickEvent = new Function();
                        con[0].referEvent = $this.openRefSelect;
                        con[0].referenceview = data.referenceview;
                        con[0].isPrefer = true;
                        con[0].readonly = false;
                    }

                    // 系统编码树形
                    if (data.columnType == 'SYSTEMCODE' && data.isTreeSystemCode) {
                        con[0].readonly = false;
                    }

                    // 布尔
                    if (data.selfType == "BOOLEAN") {
                        let arr = ["是", "否"];
                        con[0].slots = [
                        {
                            flex: 1,
                            valueIndex: 0,
                            defaultIndex: 0,
                            value: arr[0],
                            valueKey: 0,
                            values: arr
                        }
                        ];
                        con[0].selectData = ["1", "0"];
                    }

                    break;
                case "choose":
                con = [
                    {
                        ...temp,
                        clickEvent: $this.showMember,
                        readonly: true,
                        placeholder: "请选择" + data.namekey.substring(0, 2),
                        member: 1
                    }
                ];
                break;
            }
            return con;
        },
        //获取查询类型
        getQueryType: function(ctype) {
            var type;
            if (ctype == "DATE" || ctype == "DATETIME") {
                type = "date";
            } else if (ctype == "MONEY") {
                type = "section";
            } else if (ctype == "SELECTCOMP" || ctype == "SELECT") {
                type = "select";
            } else {
                type = "text";
            }
            return type;
        },

        //显示下拉
        showPickerPop: function(data) {
            const $this = this;
            if (
                (!data.slots || (data.slots && data.slots.length == 0)) &&
                data.systemCode &&
                data.systemCode.fillContent
            ) {
                $this.getData(
                {
                    url: "/foundation/systemCode/systemCodeJson.action",
                    type: "post",
                    data: {
                    systemEntityCode: data.systemCode.fillContent
                    }
                },
                function(res) {
                    // console.log(res);
                    if (res) {
                    let arr = [""],
                        keys = [""];
                    for (const key in res) {
                        if (res.hasOwnProperty(key)) {
                            const element = res[key];
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
                            values: arr
                        }
                    ];
                    data.isShowPicker = true;
                    }
                }
                );
            } else {
                data.isShowPicker = true;
            }
        },
        //下拉选择事件
        pickerEvent: function(type, value, name) {
            var queryD = this.queryData;
            var con;
            for (var i = 0; i < queryD.length; i++) {
                var item = queryD[i];
                if (item.condition[0].pickerName == name) {
                    con = item.condition[0];
                    break;
                }
            }
            if (type == "comfirm") {
                con.value = value;
                if (con.selectData && con.slots) {
                    let index = con.slots[0].values.indexOf(value);
                    con.valueId = con.selectData[index];
                }
            }
            con.isShowPicker = false;
        },

        // 打开参照
        openRefSelect: function(data) {
            console.log(data);
            // this.myToast("开发中");
            const $this = this;
            if (data && !data.referenceview.url && data.referenceview.url == "") {
                this.myToast("无效地址");
                return false;
            }
            this.showRefer = true;
            $this.referConfig.url = _commonJs.resolveReferUrl(
                data.referenceview.url,
                data
            );
            Indicator.open({
                text: "加载中...",
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
                                list,
                                [data]
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
        },

        //重置查询条件
        resetClassQuery: function() {
            this.selectItem = [];
            this.queryData = this.getQueryData(this.propSearchConfigData.query);
            this.startdate = this.curstart;
            this.enddate = this.curend;
            // this.dateVal = '';
        },
        showMember: function(item) {
            // console.log(item);
            let curPage = "";
            let text = item.placeholder;
            if (text.indexOf("人员") > -1) {
                curPage = "member";
            } else if (text.indexOf("部门") > -1) {
                curPage = "department";
            } else if (text.indexOf("岗位") > -1) {
                curPage = "station";
            }
            this.$store.commit("updateData", {
                name: "curShowPage",
                value: curPage
            });
        },

        // enter键查询
        searchByEnter: function() {
            var keycode = event.keyCode;
            var searchName = event.target.value;
            //keycode是键码，13也是电脑物理键盘的 enter
            if (keycode == "13") {
                console.log(2);
                event.preventDefault();
                this.getSearchData();
                // 失去焦点
                event.target.blur();
            }
        },

        // 查询数据
        getSearchData: function() {
            console.log(this.queryData);
            let tempData;
            let arr = this.queryData;
            let temp = [];
            let searchUrl;
            for (var i = 0; i < arr.length; i++) {
                var el = arr[i].condition;
                for (var j = 0; j < el.length; j++) {
                    temp.push(el[j]);
                }
            }

            for (var j = 0; j < temp.length; j++) {
                let name = temp[j].name;
                let value = temp[j].value;
                if (this.$refs[name] && !this.$refs[name][0].validate()) {
                    return;
                }
            }

            searchUrl = this.propSearchConfigData.searchUrl;
            if (typeof searchUrl == "string") {
                searchUrl = searchUrl;
            } else if (typeof searchUrl == "object") {
                searchUrl = searchUrl.query;
            }
            let queryList = this.selectItem.join(",");
            tempData = filtration(temp, searchUrl, queryList);

            if (!tempData) {
                return false;
            }

            // 刷新数据
            this.$parent.getSearchData(tempData);

            // 关闭侧滑菜单
            this.propCloseSidePanel();
        },

        // 默认值选项
        // setDefaultValue: function(obj){
        //   const $this = this;
        //   $this.defalutValue = obj;
        //   if(obj){
        //     for (let i = 0; i < obj.length; i++) {
        //       const el = obj[i];
        //       let etName = el.name;
        //       console.log(etName)
        //       console.log($this.$refs[etName]);
            
        //     }
        //   }
        // }

    },
    components: {
        htmlPanel,
        searchInput
    }
};
</script>
<!-- Add "scoped" attribute to limit CSS to this component only -->

<style lang="less">
@import "../../assets/css/sidePanel.less";
</style>


// WEBPACK FOOTER //
// src/components/listSearch/sidePanel.vue