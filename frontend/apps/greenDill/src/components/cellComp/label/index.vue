<template>
  <span
    :style="getCssStyle(condition.cssstyle, condition)"
    :class="className"
  >
    <label
      class="comp-label-con"
      :name="labelName"
    >
      <span class="comp-label">{{labelName}}</span>
      <em
        v-if="isNullable"
        class="mustFill"
      >*</em>
    </label>
  </span>
</template>
<script>
// import { Toast, Indicator, Field } from "mint-ui";
import _commonJs from '../../../assets/js/itemList/index.js';
import Emitter from "../../../assets/js/emitter.js";

export default {
  name: "Label",
  props: {
    className: {
      type: String,
      default: ''
    },
    condition: {
      type: Object,
      default: {}
    },
    labelName: {
      type: String,
      default: ''
    },
    isNullable: {
      type: Boolean,
      default: false
    }
  },
  mixins: [Emitter],
  data() {
    return {
    };
  },

  mounted() {
    this.dispatch('detailView', 'createRef', `${this.condition.name}_label`, this.condition);
  },

  methods: {
    //获取样式配置信息
    getCssStyle: function (config, element) {
      return _commonJs.getCssStyle(config, element);
    },
  }
};
</script>
<style lang="less" scoped>
@import "./index.less";
</style>


// WEBPACK FOOTER //
// src/components/cellComp/label/index.vue