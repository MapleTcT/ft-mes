<template>
    <div class="appendix" v-if="isShowAppendix">
        <header class="m_head" :style="{background: bgColor}">
            <div class="m_hbox">
                <span class="head_arrow" @click="backHistory">
                    <a href="javascript:;" class="m_arrow"></a>            
                </span>
                <h2 class="m_title">附件</h2>
            </div>
        </header>
        <div class="m_center">
            <ul class="m_list">
                <li class="m-flex bt" v-for="(appendixlist, index) in appendixList" id="appendixlist.id" @click="downloadAppendix(appendixlist.id)">
                    <div class="appendix-icons" :class="getAppendixType(appendixlist.name)"></div>
                    <div class="appendix-info">
                        <span class="appendix-name">{{getAppendixName(appendixlist.path)}}</span>
                        <span class="appendix-time">
                            {{formatTime("YMD_HM", appendixlist.createTime)}} 
                            下载次数：{{appendixlist.downloadTimes}}
                        </span>
                    </div>
                    <div class="appendix-size">{{appendixlist.sizeDis}}</div>
                </li>
            </ul>
        </div>
    </div>
</template>
<script>
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
export default {
    name: 'Appendix',
    data() {
        return { 
            isShowAppendix: false, //是否显示附件列表界面
            appendixList: [], //附件信息列表
        }
    },
    beforeMount() {
        if (window._PAGECONFIG && window._PAGECONFIG.navBar) {
            this.bgColor = window._PAGECONFIG.navBar.bgColor?window._PAGECONFIG.navBar.bgColor:''; // 背景色
        }
    },
    methods: {
        // 返回上一个页面
        backHistory: function() {
            this.isShowAppendix = false; //关闭附件界面
        },

        //获取附件名称
        getAppendixName: function(appendixPath) {
            var appendixName = appendixPath.substring(appendixPath.lastIndexOf('\\') + 1, appendixPath.length);
            return appendixName;
        },

        //获取附件类型，并根据附件类型设置class样式
        getAppendixType: function(appendixName) {
            var appendixType = appendixName.substring(appendixName.lastIndexOf('.') + 1, appendixName.length);
            appendixType = appendixType.toLowerCase();
            var className = "";
            switch(appendixType) {
                case "doc":
                case "docx":
                    className = "appendix-icon-word";
                    break;
                case "xls":
                case "xlsx":
                    className = "appendix-icon-excel";
                    break;
                case "png":
                case "bmp":
                case "jpg":
                case "jpeg":
                case "gif":
                case "svg":
                    className = "appendix-icon-pic";
                    break;
                case "ppt":
                case "pptx":
                    className = "appendix-icon-ppt";
                    break;
                case "txt":
                    className = "appendix-icon-txt";
                    break;
                default:
                    className = "appendix-icon-qt";
                    break;
            }
            return className;
        },

        //附件下载
        downloadAppendix: function(id) {
            var urlParams = this.GetRequest(window.location.href); // 获取地址参数
            let entityCode = urlParams ? urlParams.entityCode : "";
            var src = "/foundation/workbench/download.action" + `?id=${id}&entityCode=${entityCode}`;
            var iframe = document.createElement('iframe');
            iframe.style.display = 'none';
            iframe.src = "javascript: '<script>location.href=\"" + src + "\"<\/script>'";
            document.getElementsByTagName('body')[0].appendChild(iframe);
        },

        // 时间格式化
        formatTime: function(fmt, date) {
            return _commonJs.getDateFormat(fmt, new Date(date));
        }
    }
}
</script>
<style lang="less" scoped>
    @import "../../assets/css/Appendix.less";
</style>



// WEBPACK FOOTER //
// src/components/HeadBar/Appendix.vue