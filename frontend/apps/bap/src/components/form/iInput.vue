<template>
  <span :class="params.condition.showFormat == 'PERCENT'?'unit-text':''">
    <!-- <input ref="input" :type="type" :value="currentValue" @input="handleInput" @blur="handleBlur" /> -->
    <input
      v-if="!((params.condition && params.condition.type =='longtext') || (params.condition.datagridCode && params.condition.columnType == 'LONGTEXT'))"
      class="inp_import"
      :ref="params.condition.newName?params.condition.newName:params.condition.name"
      :name="params.condition.name"
      :data-label="params.condition.label"
      :type="params.type?params.type:'text'"
      :placeholder="params.condition.placeholder"
      v-model="params.condition.value"
      :readonly="params.condition.type == 'select' || params.condition.type == 'date'?true:params.condition.readonly"
      :datagridCode="params.condition.datagridCode"
      v-on:focus="focusMoneyFormat"
      v-on:blur="validate(), blurNumFormat()"
      v-on:click="params.clickEvent && params.clickEvent(params.condition)"
      :hidden="params.condition.columnType=='MULTSELECT'?true:false"
      v-show="isShowMoneyDiv"
    >

    <!--长文本类型-->
    <textarea
      v-else
      rows="2"
      :id="params.condition.code"
      class="inp_import"
      :ref="params.condition.newName?params.condition.newName:params.condition.name"
      :name="params.condition.name"
      :data-label="params.condition.label"
      :type="params.type?params.type:'text'"
      :placeholder="params.condition.placeholder"
      v-model="params.condition.value"
      :readonly="params.condition.type == 'select' || params.condition.type == 'date'?true:params.condition.readonly"
      :datagridCode="params.condition.datagridCode"
      v-on:blur="validate()"
      v-on:click="params.clickEvent && params.clickEvent(params.condition)"
      :hidden="params.condition.columnType=='MULTSELECT'?true:false"
    >
		</textarea>

    <!--金额显示格式-->
    <div
      class="money_div money_div_placeholder"
      v-on:click="switchMoneyFormat"
      v-if="params.condition.columnType=='MONEY' && isShowDiv && !params.condition.value && !params.condition.readonly"
    >
      请输入
    </div>
    <div
      class="money_div"
      v-on:click="switchMoneyFormat"
      v-else-if="params.condition.columnType=='MONEY' && isShowDiv"
    >
      {{params.condition.valueFormat}}
    </div>
  </span>
</template>
<script>
import AsyncValidator from "async-validator";
import { Toast, Indicator, Field } from "mint-ui";
import Emitter from "@/components/form/emitter.js";
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
export default {
  name: "iInput",
  props: ["params"],
  mixins: [Emitter],
  data() {
    return {
      isShowMes: false,
      message: "",
      index: 0,
      isShowDiv: true,
      isShowMoneyDiv: true,
      isReadOnly: ""
    };
  },
  watch: {
    "params.condition.value": function(newVal, oldVal) { //触发change事件
      const $this = this;
      if (
        newVal !== oldVal &&
        $this.params.condition &&
        $this.params.condition.loadValue != $this.params.condition.value &&
        $this.params.condition.type !== "text"
      ) {
        let el;
        if ($this.params.condition.datagridCode) { //表格的字段
          el = $this.$refs[$this.params.condition.newName];
        } else {
          el = $this.$refs[$this.params.condition.name];          
        }
        $this.params.condition.loadValue = null;
        $(el).change();
      }
    }
  },

  mounted() {
    const $this = this;
    console.log(this.params.condition.newName);
    // this.$emit('formadd', this);
    // console.log(this);
    this.dispatch("iForm", "form-add", this);
    if (this.params.condition.columnType == "MONEY") {
      this.isShowMoneyDiv = !this.isShowDiv;
    }

    // 查看页面只读,查看页面，长文本类型内容全部显示
    if (window._PAGECONFIG && window._PAGECONFIG.params) {
      $this.isReadOnly = window._PAGECONFIG.params.allReadOnly;
    }
    if (
      ($this.isReadOnly && $this.params.condition.type == "longtext") ||
      $this.params.condition.columnType == "LONGTEXT"
    ) {
      setTimeout(function() {
        var id = $this.params.condition.code;
        var ele = document.getElementById(id);
        if (ele) {
          ele.style.height = ele.scrollTop + ele.scrollHeight + "px";
        }
      }, 500);
    }
  },

  methods: {
    //获取焦点显示金额的值
    focusMoneyFormat: function() {
      if (this.params.condition.columnType == "MONEY") {
        this.isShowDiv = false;
      }
    },
    /**
     * 失去焦点格式化小数和金额类型的字段
     * 1.小数根据小数位数格式化
     * 2.金额根据默认、千分位或者万分位和小数位数格式化
     */
    blurNumFormat: function() {
      var columnType = this.params.condition.columnType;
      if (columnType == "MONEY") {
        //金额类型
        let decimal = this.params.condition.value;
        if (decimal) {
          if (_commonJs.isDecimal(decimal)) {
            this.isShowDiv = true;
            this.isShowMoneyDiv = false;
            let fmt = this.params.condition.showFormat;
            let precision = this.params.condition.precision;
            this.params.condition.value = _commonJs.getNumberFormat(
              fmt,
              decimal,
              precision
            );
            this.params.condition.valueFormat = _commonJs.formatMoney(
              fmt,
              precision,
              decimal
            );
          }
        }
      } else if (
        columnType == "DECIMAL" &&
        this.params.condition.showFormat != "PERCENT"
      ) {
        //小数类型
        let decimal = this.params.condition.value;
        if (decimal) {
          if (_commonJs.isDecimal(decimal)) {
            let fmt = this.params.condition.showFormat;
            let precision = this.params.condition.precision;
            this.params.condition.value = _commonJs.getNumberFormat(
              fmt,
              decimal,
              precision
            );
          }
        }
      }
    },
    //点击金额获取焦点切换金额显示格式
    switchMoneyFormat: function() {
      const $this = this;
      if (
        $this.params.condition.columnType == "MONEY" &&
        $this.params.condition.readonly == true
      ) {
        //查看视图不切换金额显示格式
        return;
      }
      let decimal = this.params.condition.value;
      if (_commonJs.isDecimal(decimal)) {
        let fmt = this.params.condition.showFormat;
        let precision = this.params.condition.precision;
        this.params.condition.value = _commonJs.getNumberFormat(
          fmt,
          decimal,
          precision
        );
      }
      $this.isShowDiv = false;
      $this.isShowMoneyDiv = true;
      $this.$nextTick(function() {
        let name = $this.params.condition.name;
        $this.$refs[name].focus();
      });
    },

    /**
     * 校验表单数据
     * @param callback 回调函数
     */
    validate(cb, type, status) {
      const $this = this;
      var blurType = type;
      $this.operateType = type;
      $this.status = status;
      var data = $this.params.condition;
      // 使用 async-validator
      const validator = new AsyncValidator({ [data.name]: data.rules });
      var value = data.valueId ? data.valueId : data.value;
      if (Object.prototype.toString.call(value) === "[object Array]")
        value = value.join(",");
      let model = { [data.name]: value };
      validator.validate(
        model,
        { keys: [data.name], first: true, firstFields: true, debug: true },
        (errors, type) => {
          $this.isShowMes = errors ? true : false;
          $this.message = errors ? errors[0].message : "";
          
          if (
            $this.operateType &&
            $this.operateType == "save" &&
            $this.message &&
            $this.message.indexOf("空") > -1
          ) {
            // 保存不验证非空, 验证格式
            $this.message = "";
          } else if (
            $this.operateType &&
            $this.operateType == "submit" &&
            $this.status == "cancel" &&
            $this.message 
          ) {
            // 作废不验证
            $this.message = "";
          }else if ($this.isShowMes) {
            if (
              $this.params.condition.type == "date" &&
              $this.message &&
              $this.message.indexOf("空") > -1
            ) {
              //日期类型为空不校验
              if ($this.operateType && $this.operateType == "submit") {
                $this.myToast($this.message);
                $this.scrollToError($this);
              } else {
                $this.message = "";
              }
            } else if ((data.columnType == "BOOLEAN" || data.columnType == "SYSTEMCODE") && blurType == undefined && status == undefined) { //布尔类型失去焦点不校验
              $this.message = "";
            } else {
              $this.myToast($this.message);
              $this.scrollToError($this);
            }
          }
          if (cb && typeof cb == "function") cb($this.message);
        }
      );
    },

    //获取DOM元素到页面顶部的距离
    getElementToPageTop: function(el) {
      let actualTop = el.offsetTop;
      let current = el.offsetParent;
      while (current !== null) {
        actualTop += current.offsetTop;
        current = current.offsetParent;
      }
      return actualTop;
    },

    //滚动条滑动到错误字段位置
    scrollToError: function(el) {
      var top = this.getElementToPageTop(el.$el);
      var elOffsetTop = el.$el.offsetTop;
      if (document.querySelector(".m_head")) {
        var headerHeight = document.querySelector(".m_head").scrollHeight;
        document.querySelector(".m_centerBox").scrollTop =
          top - headerHeight - elOffsetTop;
      } else {
        document.querySelector(".m_centerBox").scrollTop = top - elOffsetTop;
      }
    },

    //获取样式配置信息
    getCssStyle: function(config) {
      return _commonJs.getCssStyle(config);
    },

    // 获取condition
    getCondition: function() {
      // return this.$refs.input;
      return this.params.condition;
    },

    clickEvent: function(condition) {
      condition.clickEvent && condition.clickEvent(condition);
    }
  }
};
</script>
<style lang="less" scoped>
@import "../../assets/css/editView.less";
</style>


// WEBPACK FOOTER //
// src/components/form/iInput.vue