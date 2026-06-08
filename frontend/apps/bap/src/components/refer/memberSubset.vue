<template>
<div class="m_centerBox">
    <div class="m_box">
        <h3 class="m_title">按组织架构</h3>
        <ul class="m_list" id="groupMenu">
            <li v-on:click="$emit('linkOther', {groupId: -1, nameType: 2,isSingleCheck: isSingleCheck})">
                <img class="ml_icon" src="@/assets/images/pic_zzjg.png" alt="">
                <span class="ml_text">组织架构</span>
            </li>
            <li v-on:click="$emit('linkOther', {groupId: userGroup.id, nameType: 2,isSingleCheck: isSingleCheck})">
                <img class="ml_icon" src="@/assets/images/pic_bm.png" alt="">
                <span class="ml_text">{{userGroup.text}}</span>
            </li>
        </ul>
    </div>
    <div class="m_staffBoxWrap">
        <div class="ms_wrap">
            <h3 class="m_box m_title">常用联系人</h3>
            <groupSlideList v-on:updateChoooseList="this.params.updateChoooseList" :propBind="propBind"></groupSlideList>
        </div>
    </div>
</div>
</template>

<script>
import groupSlideList from "./groupSlideList";
// import sidePanel from "./sidePanel";
export default {
    name: "memberSubset",
    data() {
        return {
            formName: "",
            listData: "",
            allLoaded: false,
            onloadFun: null,
            userGroup: {
                text: '',
                id: ''
            },
            // curChooseList: [],
            isSingleCheck: 0,
            propBind:{
                footerH: this.$parent.$refs.footer && this.$parent.$refs.footer.clientHeight,
                listUrl: this.params.listUrl,
                isSingleCheck: 0, //单选 多选
            }
        };
    },
    props: ['params'],
    beforeMount() {
        let $this = this;
        $this.isSingleCheck = this.params && this.params.isSingleCheck;
        $this.propBind.isSingleCheck = $this.isSingleCheck;
    },
    mounted() {
        let $this = this;
        var name = this.params && this.params.name;
        $this.propBind.footerH = this.$parent.$refs.footer && this.$parent.$refs.footer.clientHeight;
        // 当前登录人 常用联系人
        $this.getData({
            url: '/foundation/user/common/getCurrentStaffDepartment', //获取当前部门接口
            type: 'get'
        }, function (res) {
            if (res.success) {
                var data = res.data;
                $this.userGroup.text = data.department.name;
                $this.userGroup.id = data.department.id;
            }
        })
        $this.formName = name;
    },
    methods: {

    },
    components: {
        groupSlideList
    }
};
</script>

<!-- 样式加载 -->
<style lang="less" scoped>
@import "../../assets/css/groupSubset.less";
</style>


// WEBPACK FOOTER //
// src/components/refer/memberSubset.vue