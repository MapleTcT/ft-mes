<template>
<div class="m_box">
    <div v-if="showRefer">
        <htmlPanel v-if="showRefer" :params="{url: referConfig.url}"></htmlPanel>
    </div>
    <div class="m_wrap">
        <span class="ml_text">
            <em v-if="this.params.isrequired" class="mustFill">*</em>{{personnelTitle}}</span>
        <div class="ml_person">
            <div class="mp_item pickbox" v-for="(item, index) in personnelList" :key="item.id">
                <span class="name">{{item.name}}</span>
                <a class="i_cancel" @click='deletePersonnel(index)'><i>×</i></a>      
            </div>
            <div v-if="chooseSwitch" class="mp_item">
                <a class="mp_add" @click="openRefSelect()"></a>
            </div>
        </div>
    </div>
</div>
</template>

<script>
import htmlPanel from "./htmlPanel"; // iframe
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import { Toast, Indicator } from 'mint-ui';
export default {
    name: "selectPersonnel",
    data: function() {
        return {
            personnelList: [], //人员列表
            personnelTitle: "", //人员标题
            showRefer: false, // 是否显示参照
            referConfig: {},
            chooseSwitch: false
        };
    },
    props: ['params'],
    mounted() {
        let $this = this;
        $this.personnelTitle = $this.params.personnelTitle;
        $this.personnelList = $this.params.personnelList?$this.params.personnelList:[];
        $this.chooseSwitch = $this.params.chooseSwitch == "0"?false:true;
    },
    methods: {
        // 打开参照并添加人员
        openRefSelect: function() {
            const $this = this;
            var data = {
                multable: true,
                referenceview: {code: "sysbase_1.0_user_ref", iscrosscompany: false, name: "userRef", url: ""}
            };
            
            // 配置人员页面
            if(window._PAGECONFIG && window._PAGECONFIG.userRef){
                data.referenceview.url = window._PAGECONFIG.userRef;
            } 

            if(!data.referenceview.url && data.referenceview.url == ''){
                this.myToast("无效地址");
                return false;
            }
            this.showRefer = true;
            // 获取参照参数，跨公司 回填到url
            $this.referConfig.url = _commonJs.resolveReferUrl(data.referenceview.url, data);
            $this.referConfig.url += `&selectPeople=${$this.params.changeData.selectStaff}`; //拼接selectStaff
            Indicator.open({
                text: '加载中...', //文字
                spinnerType: 'fading-circle', //样式
            });
            this.$nextTick(function() { // 回调方法
                let iframe = document.getElementById("newPage");
                iframe.onload = function() {
                    Indicator.close();
                    if (iframe) {
                        // 去掉原生头部
                        // try{
                        //     window.mobilejs.open_dialog();
                        // }catch(err){

                        // }
                        // 注册方法
                        // 选择回调方法
                        let code = '';
                        if(data.referenceview.code)
                        code = data.referenceview.code.replace(/\./ig, '_');
                        iframe.contentWindow[`_callBack_ec_${code}`] = function(list) {
                            for (var i = 0; i < list.length; i++) {
                                $this.personnelList.push(list[i]);
                            }
                            $this.personnelList = $this.arrayWeighting($this.personnelList);
                            $this.$emit("update", $this.personnelList, $this.params.changeData);
                            setTimeout(function() {
                                $this.showRefer = false;
                            }, 200);
                            if($this.params && $this.params.changeData){
                                $this.params.changeData.members = $this.personnelList;
                            }
                            // 恢复原生头部
                            // try{
                            //     window.mobilejs.close_dialog();
                            // }catch(err){

                            // }
                        };

                        // 返回按钮方法
                        iframe.contentWindow[`_close_ec_${code}`] = function() {
                            // 关闭iframe
                            setTimeout(function() {
                                $this.showRefer = false;
                            }, 200);
                            // 恢复原生头部
                            // try{
                            //     window.mobilejs.close_dialog();
                            // }catch(err){

                            // }
                        }
                    }
                };
            });
        },

        //删除人员
        deletePersonnel: function(index) {
            var data = this.personnelList;
            data.splice(index, 1);
            this.$emit("update", this.personnelList, this.params.changeData);
            if(this.params && this.params.changeData){
                this.params.changeData.members = this.personnelList;
            }
        },

        //数组去重
        arrayWeighting: function(oldArr) {
            var allArr = [];
            for (var i = 0; i < oldArr.length; i++) {
            　　var flag = true;
            　　for (var j = 0; j < allArr.length; j++) {
            　　　　if (oldArr[i].id == allArr[j].id) {
            　　　　　　flag = false;
            　　　　}
            　　}
            　　if (flag) {
            　　　　allArr.push(oldArr[i]);
            　　}
            }
            return allArr;
        }
    },
    components: {
        htmlPanel
    }
};
</script>

<!-- 样式加载 -->
<style lang="less" scoped>
@import "../assets/css/selectPersonnel.less";
</style>



// WEBPACK FOOTER //
// src/components/selectPersonnel.vue