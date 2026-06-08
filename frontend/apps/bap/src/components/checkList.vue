<template>
<div class="checklist">
    <div class="checklist_pop" v-if="isShow">
        <div class="checklist_title">
            <a class="m_cancel" @click="sendOut('cancel',name)">取消</a>
            <a class="m_comfirm" @click="sendOut('comfirm',name)">确定</a>
        </div>
        <mt-checklist :ref="name" v-model="delValue" algin="left" :options="options">
        </mt-checklist>
    </div>
    <div v-if="isShow" class="m_shadow" @click="sendOut('cancel',name)"></div>
</div>
</template>

<script>
import Vue from 'vue';
import {
    Checklist
} from 'mint-ui';
Vue.component(Checklist.name, Checklist);
export default {
    name: "checkList",
    data: function () {
        return {
            delValue: ''
        };
    },
    props: ["isShow", "name", "value", "options", "con"],
    watch: {
        isShow: function(newVal, oldVal){
            if(newVal){
                this.delValue = this.value;
            }
        }
    },
    methods: {
        sendOut: function (etype, name) {
            var checkList = [];
            var obj;
            var checkValue = this.$refs[name].value;
            var checkOption = this.$refs[name].options;
            for (let i = 0; i < checkValue.length; i++) { //获取选中的checkbox的数组值
                obj = checkOption.find(function (x) {
                    return x.value === checkValue[i];
                });
                checkList.push(obj);
            }
            this.$emit("checkcallback", etype, checkList, name, this.con);
        },
    }
};
</script>

<style lang="less" scoped>
@import "../assets/css/checkList.less";
</style><style lang="css">
.mint-checklist-title {
    margin: 0;
}

.mint-cell-wrapper {
    background-image: none;
    text-align: left;
}
</style>



// WEBPACK FOOTER //
// src/components/checkList.vue