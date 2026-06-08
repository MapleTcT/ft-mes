<template>
  <span>
    <!-- <htmlPanel
            v-if="showRefer"
            :params="{url: referConfig.url}"
        ></htmlPanel> -->

    <!-- 时间 -->
    <template v-if="params && params.type=='date'">
      <dateTime
        :params="params"
        :callbackFunc="callbackFunc"
      ></dateTime>
    </template>

    <!-- 参照 -->
    <template v-else-if="params && params.isPrefer">
      <reference :params="params"></reference>
    </template>

    <!-- 下拉 -->
    <template v-else-if="params && params.type=='select'&& params.columnType != 'SYSTEMCODE'">
      <picker
        :params="params"
        :callbackFunc="callbackFunc"
      ></picker>
    </template>

    <!--系统编码-->
    <template v-else-if="params && params.type=='select' && params.columnType == 'SYSTEMCODE'">
      <systemCode
        :params="params"
        :callbackFunc="callbackFunc"
      ></systemCode>
    </template>

    <!-- text -->
    <template v-else-if="params && params.type!='section'">
      <iInput :params="{condition: params}">
      </iInput>
    </template>
  </span>
</template>

<script>
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import iInput from "@/components/cellComp/iInput/index.vue";
import AsyncValidator from "async-validator";
import { Toast, Indicator, Popup } from "mint-ui";
import Emitter from "@/assets/js/emitter.js";
import checkList from "@/components/cellComp/checkList/index.vue";
import dateTime from "@/components/cellComp/dateTime/index.vue";
import systemCode from "@/components/cellComp/systemCode/index.vue";
import reference from '../reference/index.vue';
import picker from '../picker/index.vue';
export default {
  name: "inputTagment",
  props: {
    params: {
      type: Object,
      default: {}
    },
    newName: {
      type: String,
      default: ''
    },
    rowIndex: {
      type: Number
    }
  },
  mixins: [Emitter],
  data() {
    return {
      layout: "",
      popupVisible: false,
      datagrid: [],
      referConfig: {},
      showRefer: false,
      bindEvents: {}
    };
  },

  beforeUpdate() {
    this.initNewName();
  },

  beforeMount() {
    const { params: element } = this;
    const { value: curVal } = element;
    const data = curVal;
    if (!data) _commonJs.showDefaultValue(element); // 默认值展示
    this.initNewName();
  },

  mounted() {
    this.bindEvents = {
      ...this.params.events
    }
  },

  methods: {
    getFields: function () {
      return this.dispatch("iForm", "getFileds", this);
    },
    initNewName: function () {
      if (this.newName) {
        this.params.newName = this.newName;
      }
      if (this.rowIndex) {
        this.params.index = this.rowIndex;
      }
    },
    // 关闭/打开下拉回调
    callbackFunc: function (isOpen) {
      const tabComp = this.getCompByName('tabComp');
      tabComp.swipeable = !isOpen;
      this.moveFlag = isOpen;
      this.$el.removeEventListener('touchmove', this.limitTouchMove);
      this.$el.addEventListener('touchmove', this.limitTouchMove);
    },

    limitTouchMove: function (e) {
      if (this.moveFlag) e.stopPropagation();
    }
  },
  //引用组件
  components: { iInput, checkList, dateTime, systemCode, reference, picker }
};
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less" scoped>
@import "./index.less";
</style>



// WEBPACK FOOTER //
// src/components/cellComp/inputTagment/index.vue