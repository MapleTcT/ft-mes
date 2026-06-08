<template>
<div class="m_headbtn" v-if="(pagename=='pagelist' && newProcessesList.length > 0 || buttonsList.length > 0) || (pagename=='pageedit' && (!urlParams.id && referenceCopy) || (editButtonsList && editButtonsList.length > 0))">
    <!--列表视图的按钮-->
    <div v-if="pagename=='pagelist'">
    <template v-if="(newProcessesList && newProcessesList.length > 0) || (buttonsList &&buttonsList.length > 0)">
        <!--多个按钮-->
        <a class="m_headmore" href="javascript:;" @click="openBtnDialog"></a>
        <mt-popup
            v-model="popupVisible"
            position="top"
            modal=false
            closeOnClickModal=false
            pop-transition="popup-fade"
        >
            <div class="mint-popup-1">
                <ul class="btn_more">
                    <!--配置按钮-->
                    <li v-if="btnList"  v-for="(btnList, btnIndex) in buttonsList" :key="btnList.CODE" @click="bindClick(btnList.ONCLICK)">
                        <i class="btn-icon" :class="btnList.ICONCLS"></i>{{btnList.NAME}}
                    </li>

                    <!--工作流制定按钮-->
                    <li v-if="list" v-for="(list, index) in newProcessesList" :key="list.CODE" @click="operateBarOnclickFoundation(url[index])">
                        <i class="btn-icon" :class="list.ICONCLS"></i>{{list.NAME}}
                    </li>
                </ul>
            </div>
        </mt-popup>
    </template>
    </div>

    <!--编辑视图的按钮-->
    <div v-if="pagename=='pageedit'">
    <template v-if="(!urlParams.id && referenceCopy) || (editButtonsList && editButtonsList.length > 0)">
        <a class="m_headmore" href="javascript:;" @click="openBtnDialog"></a>

        <!--按钮列表-->
        <mt-popup
            v-model="popupVisible"
            position="top"
            modal=false
            closeOnClickModal=false
            pop-transition="popup-fade"
            ref="mintPop"
        >
            <div ref="mintPopup" class="mint-popup-1">
                <ul class="btn_more">
                    <!--参照复制-->
                    <li v-if="!urlParams.id && referenceCopy" @click="openReferCopy(referCopyUrl, $event)">
                        <i class="btn-icon copy-icon"></i>
                        <span>参照复制</span>
                    </li>

                    <!--自定义按钮-->
                    <li v-for="editBtnList in editButtonsList" v-if="editBtnList.isShow" @click="editBtnList.func">
                        <i class="btn-icon" :class="editBtnList.className"></i>
                        <span>{{editBtnList.btnName}}</span>
                    </li>

                    <!--附件-->
                    <li v-if="urlParams.id" @click="openAppendixInterface()">
                        <i class="btn-icon appendix-icon"></i>
                        <span>附件</span>
                        <span class="appendix-num">{{AppendixNum}}</span>
                    </li>
                </ul>
            </div>
        </mt-popup>

        <!--附件下载页面-->
        <Appendix ref="appendix"></Appendix>
    </template>
    </div>
</div>
</template>
<script>
import { Popup } from 'mint-ui';
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import Appendix from "@/components/HeadBar/Appendix"; //附件

export default {
    name: 'HeadBtn',
    data() {
        return { 
            pagename: "",
            newProcessesList: [],
            popupVisible: false,
            url: [],
            referenceCopy: "", //参照复制
            referCopyUrl: "", //参照复制地址
            buttonsList: [], ////配置按钮信息
            onClickEvent: [],
            urlParams: "",
            editButtonsList: [],
            AppendixNum: 0   
        }
    },
    mounted () {
        var $this = this;
        let clientType = '';
        let hideWebTitle ='';
        // 注册全局方法
        $this.exportFunc(`operateBarOnclickFoundation`, $this.operateBarOnclickFoundation);
        $this.exportFunc(`addBtnList`, $this.addBtnList); // 添加按钮列表

        // 获取地址参数
        $this.urlParams = $this.GetRequest(window.location.href);

        if (window._PAGECONFIG && window._PAGECONFIG.pagename) { //页面名称
            $this.pagename = window._PAGECONFIG.pagename;
        }
        if (window._PAGECONFIG && window._PAGECONFIG.clientType) {
            clientType = window._PAGECONFIG.clientType;
        }
        if (window._PAGECONFIG && window._PAGECONFIG.hideWebTitle) {
            hideWebTitle = window._PAGECONFIG.hideWebTitle;
        }
        
        if (window._PAGECONFIG && window._PAGECONFIG.newProcesses) { //工作流制定按钮
            var newProcessesList = window._PAGECONFIG.newProcesses;
            for (var k = 0; k < newProcessesList.length; k++) {
                if (newProcessesList[k]) { //为了解决数组中有空值的问题
                    $this.newProcessesList.push(newProcessesList[k]);
                }
            }
            for (var i = 0; i <　$this.newProcessesList.length; i++) {
                var onClick = $this.newProcessesList[i].ONCLICK;
                $this.url[i] = $this.getParenthesesStr(onClick);
                $this.url[i] =  $this.url[i].replace(/\'/g, "") + `&clientType=${clientType}&hideWebTitle=${hideWebTitle}`;
            }
        }

        if (window._PAGECONFIG && window._PAGECONFIG.buttons) { //配置按钮
            var buttonsList = window._PAGECONFIG.buttons;
            for (var j = 0; j < buttonsList.length; j++) {
                if (buttonsList[j]) { //为了解决数组中有空值的问题
                    $this.buttonsList.push(buttonsList[j]);
                }
            }
        }

        if (window._PAGECONFIG && window._PAGECONFIG.referenceCopy) { //参照复制功能
            $this.referenceCopy = window._PAGECONFIG.referenceCopy;
            $this.referCopyUrl = window._PAGECONFIG.referenceCopy;
        }
    },

    methods: {
        //新建单据
        operateBarOnclickFoundation: function(addUrl) {
            if(addUrl){
                window.location.href = addUrl;
            }
        },
        
        //打开按钮弹窗
        openBtnDialog: function() {
            this.popupVisible = true;
            if (this.pagename == 'pageedit' && this.urlParams.id) {
                this.getAppendixNum(); //获取附件个数
            }
        },

        //打开参照复制
        openReferCopy: function(referCopyUrl, event) {
            event.stopPropagation();
            const $this = this;
            $this.popupVisible = false; //关闭弹窗

            var referenceview = {
                code: new Date().getTime().toString(),
                iscrosscompany: false
            };
            var data = {
                url: referCopyUrl,
                multable: 0,
                referenceview: referenceview
            };

            var callbackFunc = function(list){
                if (window._PAGECONFIG && window._PAGECONFIG.layoutDataUrl) { //获取表单数据地址
                    let layoutDataUrl = window._PAGECONFIG && window._PAGECONFIG.layoutDataUrl;
                    let urlParams = $this.GetRequest(layoutDataUrl);
                    layoutDataUrl = layoutDataUrl.replace(`id=${urlParams.id}`, `id=${list.id}`);
                    vue.refreshFormData(layoutDataUrl); //刷新form数据
                }

                if (window._PAGECONFIG && window._PAGECONFIG.dataGridDataUrl) { //获取表格数据地址
                    let dataGridDataUrl = window._PAGECONFIG && window._PAGECONFIG.dataGridDataUrl;
                    let alias = window._PAGECONFIG && window._PAGECONFIG.userInfo.modelAlias;
                    let datagrid = $this.$root.$children[0].$refs.editForm.datagrid;
                    let basciUrl = "";
                    for (var i = 0; i < datagrid.length; i++ ) {
                        let dtEl = datagrid[i];
                        let datagridKey = dtEl.config.DataGridCode;
                        let dtCode = dtEl.name;
                        basciUrl = `${dataGridDataUrl}${dtCode}?datagridCode=${datagridKey}&${alias}.id=${list.id}&rt=json`;
                        vue.refreshDatagridData(basciUrl);//刷新datagrid数据 
                    }         
                }
            };

            $this.$parent.openRefSelecFunc(data, callbackFunc);
        },

        /**
         * 添加编辑视图页面的列表自定义按钮
         * @param {function} func 自定义点击事件
         * @param {string} className class名称
         * @param {string} btnName 自定义按钮的名称
         */
        addBtnList: function(func, className, btnName) {
            const $this = this;
            var btnObj = {};
            if (func && typeof func == "function") {
                btnObj.func = func;
            }
            btnObj.className = className ? className : "";
            btnObj.btnName = btnName ? btnName : "";
            btnObj.isShow = true;
            $this.editButtonsList.push(btnObj);
        },

        //打开附件下载页面
        openAppendixInterface: function() {
            const $this = this;
            _commonJs.openAppendixListInterface($this);
        },

        //获取附件个数
        getAppendixNum: function() {
            const $this = this;
            _commonJs.getAppendixNums($this);
        },

        /**
         * 取出小括号内的内容
         * @param text
         * @returns {string}
         */
        getParenthesesStr: function(text) {
            let result = ''
            if (text == "") {
                return result;                
            }
            let regex = /\((.+?)\)/g;
            let options = text.match(regex);
            if (options) {
                let option = options[0];
                if (option) {
                    result = option.substring(1, option.length - 1);
                }
            }
            return result;
        },

        bindClick: (evt) => {
            eval(evt);
        }
    },

    components: {
        Appendix
    }
}
</script>
<style lang="less" scoped>
@import "../../assets/css/headBtn.less";
</style>
<style lang="css" scoped>
.mint-popup-top {
    max-width: 4rem;
    position: absolute;
    top: 1.33333333rem;
    right: 0;
    left: auto;
    -webkit-transform: translate3d(0, 0, 0);
    transform: translate3d(0, 0, 0);
}
.mint-popup-1 {
    min-width: 2rem;
    padding: .1rem .266667rem .1rem .4rem;
    text-align: left;
}
.mint-popup-1:before {
    display: inline-block;
    width: 0;
    height: 0;
    border: solid transparent;
    border-width: .266667rem;
    border-bottom-color: #fff;
    content: "";
    position: absolute;
    top: -0.5rem;
    right: 0.18rem;
}
</style>
<style lang="css">
    .v-modal {
        opacity: 0.3;
    }
</style>



// WEBPACK FOOTER //
// src/components/HeadBar/HeadBtn.vue