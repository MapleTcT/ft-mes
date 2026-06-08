<template>
    <header v-if="!hidden || isRefer" class="m_head" :style="{background: bgColor}">
        <div class="m_hbox">
            <span @click="pageBackFunc" class="head_arrow">
                <a href="javascript:;" class="m_arrow"></a>            
            </span>
            <h2 class="m_title">{{formName}}</h2>

            <!--按钮-->
            <div class="head_btn" v-if="!isRefer">
                <!--刷新-->
                <span class="head_refresh" @click="refresh">
                    <a class="m_refresh" href="javascript:;"></a>
                </span>

                <!--头部单据操作按钮-->
                <HeadBtn></HeadBtn>
            </div>

        </div>
    </header>
</template>
<script>
import { Header } from 'mint-ui';
import HeadBtn from "./HeadBtn";
export default {
    name: 'HeadBar',
    props:["params"],
    data() {
        return { 
            formName: "页面",
            bgColor: "",
            hidden: false,
            pagename: "",
            classType: "",
            newUrlList: [],
            titleName: "",
            isRefer: false

        }
    },
    beforeMount() {
        if(window._PAGECONFIG && window._PAGECONFIG.navBar){
            this.formName = window._PAGECONFIG.navBar.title?window._PAGECONFIG.navBar.title:'视图';
            this.bgColor = window._PAGECONFIG.navBar.bgColor?window._PAGECONFIG.navBar.bgColor:''; // 背景色
            this.hidden = window._PAGECONFIG.navBar.hidden?window._PAGECONFIG.navBar.hidden:false;
            this.isRefer = window._PAGECONFIG.isRefer;
        }
    },

    mounted() {
        if (window._PAGECONFIG && window._PAGECONFIG.pagename) { //页面名称
            this.pagename = window._PAGECONFIG.pagename;
        }
    },

    methods: {
        // 页面返回方法
        pageBackFunc: function(){
            this.$emit('pageBackFunc');
        },

        //刷新
        refresh: function(){
            location.reload(window.location.href);
        }

    },

    components: {
        HeadBtn
    }
}
</script>
<style lang="less" scoped>
@import "../../assets/css/global.less";
</style>


// WEBPACK FOOTER //
// src/components/HeadBar/HeadBar.vue