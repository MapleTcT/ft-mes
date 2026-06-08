<template>
<!--岗位选择和部门选择-->
<div class="m_centerBox">
    <organizList v-on:updateChoooseList="this.params.updateChoooseList" ref="organizChild" :propBind.sync="propBind"></organizList>
</div>
</template>

<script>
// import groupSlideList from "./groupSlideList";
import organizList from "./organizList";
import {
    MessageBox
} from 'mint-ui'
export default {
    name: "groupSubset",
    data() {
        return {
            searchInpVal: "",
            sheetVisible: false,
            groupData: '',
            listType: 'flow',
            formName: "",
            layout: "",
            listData: "",
            allLoaded: false,
            bottomStatus: "",
            wrapperHeight: 0,
            topStatus: "",
            translate: 0,
            moveTranslate: 0,
            sidePanelVisible: false,
            onloadFun: null,
            nameType: '',
            isSingleCheck: 0,
            propBind: {
                linkcallBack: this.linkcallBack,
                nameType: this.params.nameType,
                isSingleCheck: this.params.isSingleCheck,
                // title: this.params.params,
                curChooseList: this.params.curChooseList,
                tit: this.params.childTit,
                listUrl: this.params.listUrl
            }
        };
    },
    props: ['params'],
    mounted() {
        let $this = this;
        // let $this = this;
        if ($this.params) {
            let params = $this.params;
            // let type = params.type;
            let name = params.name;
            $this.nameType = params.nameType;
            $this.isSingleCheck = params.isSingleCheck;
            $this.formName = name;
        }
    },
    methods: {
  
    },
    components: {
        organizList
    }
};
</script>

<!-- 样式加载 -->
<style lang="less" scoped>
@import "../../assets/css/groupSubset.less";

.m_centerBox {
    position: absolute !important;
    top: 200/@rem;
    bottom: 86/@rem;
    left: 0;
    right: 0;

    /deep/ .group_list {
        position: absolute;
        top: 70/@rem;
        bottom: 0/@rem;
        overflow: auto;
        left: 0;
        right: 0;
        max-height: none !important;
    }
}
</style>



// WEBPACK FOOTER //
// src/components/refer/groupSubset.vue