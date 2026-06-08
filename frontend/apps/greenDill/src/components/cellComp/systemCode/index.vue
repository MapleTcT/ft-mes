<template>
  <div :class="(params.type=='select' && !params.readonly) ? switchClassName():''">
    <iInput
      v-if="params.isTreeSystemCode"
      :params="{
        condition: params,
        clickEvent: openSystemCode
      }"
    >
    </iInput>
    <iInput
      v-else
      :params="{
        condition: params,
        clickEvent: showPickerPop
      }"
    >
    </iInput>
    <!--系统编码树形，值不为空-->
    <i
      v-if="!params.readonly && params.isTreeSystemCode && params.value"
      class="refer-delete"
      @click="deleteRefValue(params)"
    >
    </i>
    <!--系统编码树形，值为空-->
    <i
      v-else-if="!params.readonly && params.isTreeSystemCode && !params.value"
      class="systemcode-icon"
      @click="bindEvents.onClick && bindEvents.onClick(), openSystemCode(params)"
    >
    </i>
    <!--系统编码列表-->
    <i
      v-else-if="!params.readonly"
      class="more-icon"
      @click="bindEvents.onClick && bindEvents.onClick(), showPickerPop(params)"
    >
    </i>
    <!--系统编码列表-->
    <div
      class="pickerPop"
      @touchmove.prevent
    >
      <!-- 列表单选下拉框 -->
      <custom-picker
        v-if="!params.multable"
        :ref="params.name"
        :name="params.pickerName"
        :isShow="!params.multable && params.isShowPicker"
        :slots="params.slots"
        :value="params.value"
        :con="params"
        @pickercallback="pickerEvent"
      >
      </custom-picker>
    </div>
    <!-- 列表多选下拉框 -->
    <checkList
      v-if="params.multable"
      :isShow="params.multable && params.isShowPicker"
      :con="params"
      @checkcallback="checkcallback"
      :value="params.valueId?params.valueId:[]"
      :name="params.pickerName"
      :options="params.slots"
    >
    </checkList>
    <!--系统编码树形-->
    <systemCodeTree
      ref="systemCodeTree"
      :params="params"
    >
    </systemCodeTree>
  </div>
</template>
  
<script>
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import iInput from "@/components/cellComp/iInput/index.vue";
// import checkList from "@/components/checkList";
import checkList from "@/components/cellComp/checkList/index.vue";
// import systemCodeTree from "@/components/form/systemCodeTree";
import systemCodeTree from "@/components/cellComp/systemCodeTree/index.vue";
import Emitter from "@/assets/js/emitter.js";
import { deleteReferValues } from '../reference/util.js';
import { openSystemCodeInterface, getSystemCodeJson, formatVal } from './util.js';

export default {
  name: "systemCode",
  props: {
    params: {
      type: Object,
      default: {}
    }
  },
  mixins: [Emitter],
  data() {
    return {
      bindEvents: {}
    };
  },

  watch: {
    "params.slots": function (newVal, oldVal) {
      if (newVal !== oldVal) {
        formatVal(this.params);
      }
    },
  },

  beforeMount() {
    // 加载list
    getSystemCodeJson(this.params);

  },

  mounted() {
    this.bindEvents = {
      ...this.params.events
    }

    formatVal(this.params);
  },

  methods: {
    //打开系统编码选择界面,并加载系统编码数据
    openSystemCode: function (data) {
      const $this = this;
      openSystemCodeInterface($this, data);
    },

    //显示系统编码列表下拉
    showPickerPop: function (data) {
      const $this = this;
      _commonJs.showDropdown($this, data);
    },

    //删除参照的值
    deleteRefValue: function (data) {
      const $this = this;
      deleteReferValues($this, data)
    },

    //根据不同类型的字段显示不同的class名
    switchClassName: function () {
      if (this.params.isTreeSystemCode) {
        return "pr30";
      } else {
        return "pr20";
      }
    },

    //下拉选择事件
    pickerEvent: function (type, value, name, con) {
      var layout = this.layout;
      var con;
      if (type == "comfirm") {
        let index = con.slots[0].values.indexOf(value);
        con.value = value;
        con.valueId = con.slots[0].valueKey[index];
      }
      con.isShowPicker = false;
    },

    // 多选checkbox回调
    checkcallback: function (ctype, list, name, con) {
      let values = [],
        valueIds = [];
      if (ctype == "comfirm") {
        list.forEach(element => {
          values.push(element.label);
          valueIds.push(element.value);
        });
        con.value = values.join(",");
        con.valueId = valueIds;
      }
      con.isShowPicker = false;
    },
  },

  //引用组件
  components: { iInput, checkList, systemCodeTree }
};
</script>
<!-- Add "scoped" attribute to limit CSS to this component only -->

<style lang="less" scoped>
@import "./index.less";
</style>
  


// WEBPACK FOOTER //
// src/components/cellComp/systemCode/index.vue