<template>
<div v-if="hidden && pagename=='pageedit' && !isRefer && showRightBtn">
    <!--v-if="(!urlParams.id && referenceCopy) || (editButtonsList && editButtonsList.length > 0)"-->
    <div class="m_rightbtn" 
        ref="rightbtn"
        :style="{'width':itemWidth+'px','height':itemHeight+'px','right':right+'px','top':top+'px'}">

        <!--悬浮图标-->
        <div class="m_rightbtn_bg" @click ="onBtnClicked($event)">
        </div>

        <!--按钮列表-->
        <mt-popup
            v-model="popupVisible"
            position="right"
            modal=false
            closeOnClickModal=false
            pop-transition="popup-fade"
            ref="mintPop"
        >
            <div ref="mintPopup" class="mint-popup-1">
                <ul class="btn_ul">
                    <!--参照复制-->
                    <li v-if="!urlParams.id && referenceCopy"  @click="openReferCopy(referCopyUrl, $event)">
                        <i class="btn-icon copy-icon"></i>参照复制
                    </li>

                    <!--自定义按钮-->
                    <li v-for="editBtnList in editButtonsList" v-if="editBtnList.isShow" @click="editBtnList.func(), clickEvent()">
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

        <div class="btn_modal" ref="btnModal"></div>

        <!--参照复制界面-->
        <div v-if="showRefer">
            <htmlPanel
                v-if="showRefer"
                :params="{url: referCopyUrl}"
            ></htmlPanel>
        </div>
    </div>
</div>
</template>
<script>
import { Popup } from 'mint-ui';
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import Appendix from "@/components/HeadBar/Appendix"; //附件

export default {
    name: 'RightBtn',
    data() {
        return { 
            pagename: "",
            popupVisible: false,
            currentTop: 0,
            clientHeight: 0,
            left: 0,
            top: 0,
            right: 0,
            itemWidth: 70,
            itemHeight: 70,
            referenceCopy: "", //参照复制
            referCopyUrl: "", //参照复制地址
            showRefer: false, // 是否显示参照复制界面
            hidden: false, //是否显示头部
            urlParams: "",
            editButtonsList: [],
            isRefer:false,
            showRightBtn: true,
            AppendixNum: 0
        }
    },
    props:["params"],
    watch:{
        'right':{
            deep:true,
            handler: function(newVal, oldVal) {
                console.log(newVal)
                if (newVal == 0) {
                    const btnModal = this.$refs.btnModal;
                    btnModal.style.display = "none";
                }
            }
        }
    },
    created(){
        this.clientHeight = document.documentElement.clientHeight;
        this.right = 0;
        this.top = this.clientHeight/2 - this.itemHeight/2; //初始位置居中显示
    },
    beforeMount() {
        if(window._PAGECONFIG && window._PAGECONFIG.isRefer){
            this.isRefer = window._PAGECONFIG.isRefer;
        }
    },
    mounted () {
        var $this = this;

        // 获取地址参数
        $this.urlParams = $this.GetRequest(window.location.href);

        //是否显示头部
        if(window._PAGECONFIG && window._PAGECONFIG.navBar){
            $this.hidden = window._PAGECONFIG.navBar.hidden?window._PAGECONFIG.navBar.hidden:false;
        }

        if (window._PAGECONFIG && window._PAGECONFIG.pagename) { //页面名称
            $this.pagename = window._PAGECONFIG.pagename;
        }

        if (window._PAGECONFIG && window._PAGECONFIG.referenceCopy) { //参照复制功能
            $this.referenceCopy = window._PAGECONFIG.referenceCopy;
            $this.referCopyUrl = window._PAGECONFIG.referenceCopy;
        }
        
        // 注册全局方法
        if ($this.hidden) {
            $this.exportFunc(`addBtnList`, $this.addBtnList); // 添加按钮列表            
        }
        
        $this.dragButton(); //右侧悬浮按钮上下拖动
        
    },

    methods: {
        //实现右侧悬浮按钮上下拖动
        dragButton: function () {
            let $this = this;
            $this.$nextTick(() => {
                const rightbtn = $this.$refs.rightbtn;
                const btnModal = $this.$refs.btnModal;

                if (rightbtn && btnModal) {
                    // 获取元素和初始值
                    var disX = 0, 
                        disY = 0;

                    // 获取浏览器可见区域宽高，div宽高
                    var browserWidth = document.documentElement.clientWidth,
                        browserHeight = document.documentElement.clientHeight,
                        boxWidth = rightbtn.offsetWidth,
                        boxHeight = rightbtn.offsetHeight;
                    
                    
                    rightbtn.addEventListener("touchstart", (e) => { //拖动开始
                        var e = e || window.event;
                        let touch = e.targetTouches[0];
                        disX = touch.clientX - rightbtn.offsetLeft; //鼠标相对于div左侧位置
                        disY = touch.clientY - rightbtn.offsetTop; //鼠标相对于div上侧位置
                        rightbtn.style.transition = 'none';
                        btnModal.style.display = "block";
                    });

                    rightbtn.addEventListener("touchmove", (e) => { //拖动过程
                        var e = e || window.event;
                        e.preventDefault();
                        if (e.targetTouches.length === 1) {//一根手指
                            let touch = e.targetTouches[0];
                            $this.right = 0;
                            if ((touch.clientY - disY) <= 0) {
                                $this.top = 0;
                            } else if ((boxHeight - disY + touch.clientY) >= browserHeight) {
                                $this.top = browserHeight - boxHeight;
                            } else {
                                $this.top = touch.clientY - $this.itemHeight/2; 
                            }
                            btnModal.style.display = "block";
                        }
                    });

                    rightbtn.addEventListener("touchend", () => { //拖动结束
                        rightbtn.style.transition = 'all 0.2s';
                        $this.right = 0;
                        $this.top = $this.top;
                        btnModal.style.display = "none";
                    });
                }
            });
        },

        //打开按钮弹窗
        onBtnClicked: function(event) {
            if (this.popupVisible) {
                this.popupVisible = false;
                this.right = 0;
            } else {
                this.popupVisible = true;
                this.right = 150;
                if (this.pagename == 'pageedit' && this.urlParams.id) {
                    this.getAppendixNum(); //获取附件个数
                }
                // let mintPopupWid = this.$refs.mintPopup.style.width;
                // this.right = mintPopupWid;
            }
            if (event) {
                event.stopPropagation();                
            }
        },

        //打开参照复制
        openReferCopy: function(referCopyUrl, event) {
            event.stopPropagation();
            const $this = this;
            $this.popupVisible = false; //关闭弹窗
            $this.showRightBtn = false;

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
                    let datagrid = $this.$parent.$refs.editForm.datagrid;
                    let basciUrl = "";
                    for (var i = 0; i < datagrid.length; i++ ) {
                        let dtEl = datagrid[i];
                        let datagridKey = dtEl.config.DataGridCode;
                        let dtCode = dtEl.name;
                        basciUrl = `${dataGridDataUrl}${dtCode}?datagridCode=${datagridKey}&${alias}.id=${list.id}&rt=json`;
                        vue.refreshDatagridData(basciUrl);//刷新datagrid数据 
                    }         
                }
                setTimeout(function(){
                    $this.right = 0;
                    $this.showRightBtn = true;
                    $this.dragButton(); //右侧悬浮按钮上下拖动
                },300);
            };

            var goBack = function() {
                $this.right = 0;
                $this.showRightBtn = true;
            } 
            $this.$parent.openRefSelecFunc(data, callbackFunc, goBack);
            
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
            $this.dragButton(); //右侧悬浮按钮上下拖动
        },

        //点击自定义事件之后关闭弹窗，并将悬浮按钮靠右显示
        clickEvent: function() {
            let $this = this;
            $this.popupVisible = false; //关闭弹窗
            $this.right = 0;
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
    },

    components: {
        Appendix
    }
}
</script>
<style lang="less" scoped>
@import "../../assets/css/rightBtn.less";
</style>
<style lang="css" scoped>
    .mint-popup {
        position: absolute;
        right: -150px;
    }
    .mint-popup-1 {
        width: 130px;
        min-height: 1.58666667rem;
        padding: 5px 5px 5px 15px;
        text-align: left;
    }
    .btn_ul {
        padding: 5px 0;
    }
    .btn_modal {
        position: fixed;
        left: 0;
        top: 0;
        width: 100%;
        height: 100%;
        opacity: 0;
        background: #fff;
        z-index: 1999;
        display: none;
    }
</style>
<style lang="css">
    .v-modal {
        opacity: 0.3;
    }
</style>



// WEBPACK FOOTER //
// src/components/HeadBar/RightBtn.vue