<template>
  <div class="checklist">
    <div
      class="checklist_pop"
      v-if="isShow"
    >
      <div
        class="checklist_title"
        @touchmove.prevent
      >
        <a
          class="m_cancel"
          @click="sendOut('cancel',name)"
        >{{$t('button.cancel.text')}}</a>
        <a
          class="m_comfirm"
          @click="sendOut('comfirm',name)"
        >{{$t('button.ok.text')}}</a>
      </div>
      <mt-checklist
        :ref="name"
        v-model="delValue"
        algin="left"
        :options="options"
        @click.prevent.stop
      >
      </mt-checklist>
    </div>
    <div
      v-if="isShow"
      class="m_shadow"
      @click="sendOut('cancel',name)"
      @touchmove.prevent
      @click.prevent.stop
    ></div>
  </div>
</template>

<script>
import Vue from 'vue';
import {
  Checklist
} from 'mint-ui';
// Vue.component(Checklist.name, Checklist);
export default {
  name: "checkList",
  data: function () {
    return {
      delValue: ''
    };
  },
  props: ["isShow", "name", "value", "options", "con"],
  watch: {
    isShow: function (newVal, oldVal) {
      if (newVal) {
        this.delValue = this.value;
        this.closeTouch();
      } else {
        this.openTouch();
      }
      if (typeof this.$parent.$parent.callbackFunc === 'function') this.$parent.$parent.callbackFunc(newVal);
    }
  },
  methods: {
    sendOut: function (etype, name) {
      var checkList = [];
      var checkValue = this.$refs[name].value;
      var checkOption = this.$refs[name].options;
      for (let i = 0; i < checkValue.length; i++) { //获取选中的checkbox的数组值
        for (let j = 0; j < checkOption.length; j++) {
          if (checkOption[j].value === checkValue[i]) {
            checkList.push(checkOption[j]);
          }
        }
      }
      this.$emit("checkcallback", etype, checkList, name, this.con);
    },

    closeTouch: function () {
      document.getElementsByClassName("m_centerBox")[0].style.overflow = "hidden";
    },

    openTouch: function () {
      document.getElementsByClassName("m_centerBox")[0].style.overflow = "auto";
    }
  }
};
</script>

<style lang="less" scoped>
@import "./index.less";
</style>
<style lang="css">
.mint-checklist-title {
  margin: 0;
}

.mint-cell-wrapper {
  background-image: none;
  text-align: left;
}

.mint-checklist-label {
  position: relative;
}

.mint-checkbox-input {
  display: inline-block;
  opacity: 0;
  outline: 0;
  position: absolute;
  width: 25px;
  height: 25px;
  z-index: 100;
}
</style>



// WEBPACK FOOTER //
// src/components/cellComp/checkList/index.vue