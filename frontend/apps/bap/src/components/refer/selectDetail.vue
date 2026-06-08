<template>
<div id="app">
    <div class="g_container" :class="{ 'g_container_reset_color': isAndroidLessVersionFive }">
        <HeadBar ref="headbar" v-on:pageBackFunc="backHistory"></HeadBar>
        <template v-if="!showSearch">
            <div class="m_search">
                <input v-on:click="linkSearch" class="inp_search" type="text" :placeholder="placeholderText">
            </div>
            <!--人员选择一-->
            <memberSubset v-if="curShowPage == 'memberSubset'" v-on:linkOther="linkOther" ref="memberSubset" :params="{
                name: formName, 
                childTit: childTit, 
                nameType: curNameType, 
                isSingleCheck: isSingleCheck, 
                updateChoooseList: updateChoooseList,
                listUrl: listUrl
            }"></memberSubset>

            <!--岗位选择和部门选择-->
            <groupSubset v-else-if="curShowPage == 'groupSubset'" ref="groupSubset" :params="{
                name: formName, 
                childTit: childTit ,
                nameType: curNameType, 
                isSingleCheck: isSingleCheck, 
                updateChoooseList: updateChoooseList,
                listUrl: listUrl
            }"></groupSubset>

            <!--人员选择二-->
            <selectContainer v-else-if="curShowPage == 'selectContainer'" :params="{
                groupId: selectContainerParam.groupId, 
                nameType: curNameType, 
                childTit: childTit, 
                isSingleCheck: isSingleCheck, 
                updateChoooseList: updateChoooseList, 
                returnHome: returnHome,
                groupUrl: groupUrl,
                listUrl: listUrl,
            }"></selectContainer>
        </template>
        <searchResult v-else-if="showSearch" v-on:hideSearch="hideSearch" :params="searchParams"></searchResult>
        <div class="m_foot" ref="footer" v-if="isSingleCheck == '0'">
            <div class="m_selInfo">
                <span class="ms_text">
          已选：
          <em class="num" id="selectNum">{{this.$store.state.curChooseList.length}}</em>
          {{isMemeberChoose?'人':'个'}}
        </span>
            </div>
            <a class="m_botton btn_submit" v-on:click="submitData">确定</a>
        </div>
    </div>
    <div class="edit_modal"></div>
</div>
</template>

<script>
import groupSubset from "./groupSubset";
import memberSubset from "./memberSubset";
import selectContainer from "./selectContainer";
import searchResult from "./searchResult";
import HeadBar from "@/components/HeadBar/HeadBar";
import _commonJs from "@/assets/js/itemList/index.js";
export default {
    name: "selectDetail",
    data() {
        return {
            sheetVisible: false,
            groupData: [1, 2, 3, 4, 5, 6, 7],
            headList: [],
            formName: "",
            placeholderText: "",
            isSingleCheck: 0,
            childTit: "",
            curNameType: "",
            curChooseList: [], //当前选中的列表
            isMemeberChoose: 0,
            memberBack: [], //记录返回前的操作
            selectContainerParam: {},
            curShowPage: "", // 组件名
            showSearch: false,
            listUrl: '',
            searchParams: {
                //搜索params
                showFlag: false,
                isSingleCheck: 0,
                listUrl: "",
                placeholderClickText: ""
            },
            groupUrl:'',
            isAndroidLessVersionFive: false // 安卓手机系统是否小于等于5.0
        };
    },
    props: ["params"],
    beforeMount() {
        let $this = this;
        let name, groupId, placeholderText;
        if ($this.params) {
            // 参数传参
            name = $this.params.name;
            groupId = $this.params.groupId;
            $this.curNameType = $this.params.nameType;
            $this.isSingleCheck = $this.params.isSingleCheck;
        } else if (window._PAGECONFIG) { // 配置项
            const config = window._PAGECONFIG;
            name = config.name;
            placeholderText = config.placeholderText;
            $this.curNameType = config.nameType;
            $this.isSingleCheck = config.isSingleCheck;
        }

        // 地址取参数
        let urlConfig = this.GetRequest(window.location.href);
        let conditionParams = '', crossCompanyFlag='', selectPeople = '';
        if (urlConfig) {
            $this.isSingleCheck = urlConfig.multiSelect - 0 == 1 ? 0 : 1;
            $this.callBackFuncName = urlConfig.callBackFuncName;
            $this.closeFuncName = urlConfig.closeFuncName;
            // 分割condition 筛选参数
            let condition = urlConfig.condition;
            conditionParams = condition;
            // 添加跨集团搜索
            if(urlConfig.crossCompanyFlag != "undefined"){
                crossCompanyFlag = `&crossCompanyFlag=${urlConfig.crossCompanyFlag}`;
            }
            if (urlConfig.selectPeople) {
                selectPeople = `&selectPeople=${urlConfig.selectPeople}`;
            }
        }

        let searchUrl;
        $this.formName = name;
        $this.placeholderText = placeholderText;
        this.searchParams.placeholderClickText = placeholderText;
        this.curShowPage = "groupSubset";
        if (name.indexOf("人员") > -1) {
            this.childTit = "联系人";
            this.curShowPage = "memberSubset";
            this.memberBack = [this.curShowPage];
            this.searchParams.showFlag = true;
            this.isMemeberChoose = 1;
            searchUrl = {
                query: `/foundation/user/common/queryStaffByName?${crossCompanyFlag}`,
                // more: `/foundation/user/common/getStaffDetailRefInfo?`
            };
            $this.listUrl = `/foundation/user/common/getStaffDetailRefInfo?${conditionParams}${crossCompanyFlag}`;
        } else if (name.indexOf("岗位") > -1) {
            this.childTit = "岗位";
            // searchUrl = `/foundation/position/common/queryPositionByName?`;
            searchUrl = {
                query: `/foundation/position/common/queryPositionByName?${crossCompanyFlag}`,
                more: `/foundation/position/sortitem/getPositionDetailRefInfo?${crossCompanyFlag}`
            };
            $this.listUrl = `/foundation/position/sortitem/getPositionDetailRefInfo?${conditionParams}${crossCompanyFlag}`;
        } else if (name.indexOf("部门") > -1) {
            this.childTit = "部门";
            searchUrl = {
                query: `/foundation/department/common/queryDepartmentByName?${crossCompanyFlag}`,
                more: `/foundation/department/common/getDepartmentDetailRefInfo?${crossCompanyFlag}`
            };
            // searchUrl = `/foundation/department/common/queryDepartmentByName?`;
            $this.listUrl = `/foundation/department/common/getDepartmentDetailRefInfo?${conditionParams}${crossCompanyFlag}`;
        } else if (name.indexOf("用户") > -1) {
            this.childTit = "联系人";
            this.curShowPage = "memberSubset";
            this.memberBack = [this.curShowPage];
            this.searchParams.showFlag = true;
            this.isMemeberChoose = 1;
            searchUrl = {
                query: `/foundation/user/common/queryUserByName?${crossCompanyFlag}${selectPeople}`, //查询接口
                // more: `/foundation/user/common/getUserDetailRefInfo.action`
            };
            $this.listUrl = `/foundation/user/common/getUserDetailRefInfo?${conditionParams}${crossCompanyFlag}${selectPeople}`; //获取常用联系人接口
        }
        if (name.indexOf("用户") > -1) {
            $this.groupUrl = `/foundation/department/common/getUserDepartmentDetailRefInfo?${crossCompanyFlag}${selectPeople}`; //点击组织架构，获取公司下的部门
        } else {
            $this.groupUrl = `/foundation/department/common/getDepartmentDetailRefInfo?${conditionParams}${crossCompanyFlag}`;
        }
        this.searchParams.isSingleCheck = $this.isSingleCheck;
        this.searchParams.listUrl = searchUrl;
        // this.searchParams.curChooseList = $this.curChooseList;
        this.searchParams.updateChoooseList = $this.updateChoooseList;
        this.searchParams.nameType = $this.curNameType;

        // curChooseList 清空重新赋值
        this.$store.commit("clearAll");
        this.curChooseList = this.$store.state.curChooseList;
    },
    mounted() {
        console.log(this);
        const $this = this;
        // 获取当前安卓手机版本号小于等于5的进行样式处理
        this.isAndroidLessVersionFive = _commonJs.isLessEqualAndroidVersion(5);

    },
    methods: {
        // 隐藏搜索
        hideSearch: function () {
            this.showSearch = false;
        },
        // 跳转
        linkOther: function (res) {
            this.selectContainerParam = res;
            this.curShowPage = "selectContainer";
            // this.memberBack.push(this.curShowPage);
        },
        // 返回上一个页面
        backHistory: function () {
            const $this = this;
            if (typeof window[$this.closeFuncName] == "function") {
                window[$this.closeFuncName](); // 执行给关闭方法
            }
        },
        // 到搜索页面
        linkSearch: function () {
            this.showSearch = true;
        },
        //根据key查找对应的数值
        getValueByKey: function (data, element) {
            var key = element.key;
            var isDate = element.showType == "DATE";
            var getKey = key.split(".");
            var temp = data;
            for (var k = 1; k < getKey.length; k++) {
                if (temp[getKey[k]]) {
                    temp = temp[getKey[k]];
                    if (isDate && element.showFormat) {
                        //时间格式化
                        temp = this.dateFtt(element.showFormat, new Date(temp));
                    }
                } else {
                    return "";
                }
            }
            return temp;
        },
        // 更新数据
        updateChoooseList: function (res) {
            const $this = this;
            if (this.isSingleCheck == "1") {
                // 单选
                this.$store.commit("updateData", {
                    name: "curChooseList",
                    value: res
                });

                $this.rememberLink(function(){
                    if (typeof window[$this.callBackFuncName] == "function") {
                        window[$this.callBackFuncName]($this.$store.state.curChooseList); // 回调selectCallBack方法
                    }

                    // 是否有回调函数
                    if ($this.params && $this.params.callback && typeof $this.params.callback == "function") {
                        $this.params.callback();
                    }
                });

                
            }
        },
        // 确定人员
        submitData: function () {
            let $this = this;
            console.log(this.$store.state.curChooseList);
            if(!this.$store.state.curChooseList || this.$store.state.curChooseList.length == 0){
                $this.myToast("至少选择一行数据");
                return false;
            }
            if (this.curNameType == "1") {
                // 人员选择
                // $this.rememberLink();
            }
            this.$store.commit("updateData", {
                name: "curShowPage",
                value: "pagelist"
            });

            $this.rememberLink(function(){
                if (typeof window[$this.callBackFuncName] == "function") {
                    window[$this.callBackFuncName]($this.$store.state.curChooseList); // 回调selectCallBack方法
                }
                
                // 是否有回调函数
                if (
                   $this.params &&
                    $this.params.callback &&
                    typeof $this.params.callback == "function"
                ) {
                    $this.params.callback();
                }
            });
        },

        // 记录常用联系人
        rememberLink: function (callback) {
            let $this = this;
            let ids = [];
            let list = this.$store.state.curChooseList;

            if($this.curShowPage != "memberSubset"){
                callback && typeof callback == "function" && callback();
                return ;
            }

            if(list && list.id){
                ids = list.id;
            }else{
                for (let i = 0; i < list.length; i++) {
                    const element = list[i];
                    ids.push(element.id);
                }
                ids = ids.join(",");
            }
            
            // 接口记录
            $this.getData({
                    url: "/foundation/user/common/staffUseRecord?ids=" + ids,
                    // type: "get"
                },
                function (res) {
                    if (res.success) {
                        console.log("保存成功");
                    }
                    if(callback && typeof callback == "function"){
                        callback();
                    }
                }
            );
        },
        // 联系人返回首页
        returnHome: function () {
            this.curShowPage = this.memberBack ? this.memberBack[0] : "";
        }
    },
    components: {
        memberSubset,
        groupSubset,
        searchResult,
        selectContainer,
        HeadBar
    }
};
</script>

<!-- 样式加载 -->

<style lang="less" scoped>
@import "../../assets/css/groupSubset.less";
</style>



// WEBPACK FOOTER //
// src/components/refer/selectDetail.vue