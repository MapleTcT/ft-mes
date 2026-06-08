<template>
  <span :class="params.condition.showFormat == 'PERCENT'?'unit-text':''">
    <!-- <input ref="input" :type="type" :value="currentValue" @input="handleInput" @blur="handleBlur" /> -->
    <!-- 文本只读 -->
    <span
      v-if="viewType === 'VIEW' && ['TEXT','SELECT','SELECTCOMP'].includes(params.condition.showFormat)"
      :ref="params.condition.newName?params.condition.newName:params.condition.name"
      :name="params.condition.name"
      :data-label="params.condition.label"
      :type="params.type?params.type:'text'"
      class="inp_text_view"
      :placeholder="params.condition.placeholder"
      v-on:change="bindEvents.onChange && bindEvents.onChange(showVal,params.condition.index), changeNumber(showVal)"
      v-on:click="params.clickEvent && params.clickEvent(params.condition), bindEvents.onClick && bindEvents.onClick($event,params.condition.index)"
      :datagridCode="params.condition.datagridCode"
      :hidden="params.condition.columnType=='MULTSELECT'?true:false"
    >
      {{showVal}}
    </span>
    <template v-else-if="!(params.condition.type =='longtext')">
      <input
        class="inp_import"
        :class="isTextOverflow ? 'inp_text_overflow': 'inp_text_overflow_no'"
        :ref="params.condition.newName?params.condition.newName:params.condition.name"
        :name="params.condition.name"
        :data-label="params.condition.label"
        :type="params.type?params.type:'text'"
        :placeholder="params.condition.placeholder"
        v-model="showVal"
        :readonly="params.condition.type == 'select' || params.condition.type == 'date'?true:params.condition.readonly"
        :datagridCode="params.condition.datagridCode"
        v-on:focus="focusMoneyFormat(),switchScroll(false)"
        v-on:touchmove="touchmoveFuc()"
        v-on:blur="blurFormat(), switchScroll(true)"
        v-on:change="bindEvents.onChange && bindEvents.onChange(showVal,params.condition.index), changeNumber(showVal,true)"
        v-on:click="params.clickEvent && params.clickEvent(params.condition), bindEvents.onClick && bindEvents.onClick($event,params.condition.index)"
        :hidden="params.condition.columnType=='MULTSELECT'?true:false"
        v-show="!isShowMoneyDiv"
      >
    </template>

    <!--长文本类型-->
    <textarea
      v-else
      rows="2"
      class="inp_import"
      :style="params.condition && params.condition.cssstyle && getCssStyle(params.condition.cssstyle, params.condition)"
      :ref="params.condition.newName?params.condition.newName:params.condition.name"
      :name="params.condition.name"
      :data-label="params.condition.label"
      :type="params.type?params.type:'text'"
      :placeholder="params.condition.placeholder"
      v-model="showVal"
      :readonly="params.condition.type == 'select' || params.condition.type == 'date'?true:params.condition.readonly"
      :datagridCode="params.condition.datagridCode"
      v-on:focus="switchScroll(false)"
      v-on:touchmove="touchmoveFuc()"
      v-on:blur="blurFormat(), switchScroll(true)"
      v-on:change="bindEvents.onChange && bindEvents.onChange(showVal,params.condition.index)"
      v-on:click="params.clickEvent && params.clickEvent(params.condition), bindEvents.onClick && bindEvents.onClick($event,params.condition.index)"
      :hidden="params.condition.columnType=='MULTSELECT'?true:false"
    >
    </textarea>

    <!--金额显示格式-->
    <template v-if="params.condition.columnType=='MONEY' || ['THOUSAND', 'TEN_THOUSAND'].indexOf(params.condition.showFormat) > -1">
      <div
        class="money_div money_div_placeholder"
        v-on:click="switchMoneyFormat"
        v-if="isShowMoneyDiv && !showVal && !params.condition.readonly"
      >
        {{$t('placeholder.enter.text')}}
      </div>
      <div
        class="money_div"
        v-on:click="switchMoneyFormat"
        v-else-if="isShowMoneyDiv && showVal"
      >
        {{showFormatData()}}
      </div>
    </template>
  </span>
</template>
<script>
import AsyncValidator from "async-validator";
import { Toast, Indicator, Field } from "mint-ui";
import Emitter from "@/assets/js/emitter.js";
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import { isTextComp } from "@/assets/js/util/common.js"
import { isInteger, isLong, isDecimal } from '@/assets/js/util/validate.js';
import { getFormatPrecision, getShowFormat, FormatVal } from './util.js';

export default {
  name: "iInput",
  props: ["params"],
  props: {
    params: {
      type: Object,
      default: { condition: {} }
    },
  },
  mixins: [Emitter],
  data() {
    return {
      isShowMes: false,
      message: "",
      index: 0,
      // isShowDiv: true,
      isShowMoneyDiv: false,
      viewType: "", //视图类型
      bindEvents: {},
      lastEdit: "",
      isTextOverflow: true,
      workflowEnabled: "",  //是否启用工作流
      showVal: '',
      valueFormat: '',
    };
  },

  watch: {
    "params.condition.value": function (newVal, oldVal) { //触发change事件
      const { condition = {} } = this.params;
      const { value } = condition;
      this.formatValue();
      if (
        newVal !== oldVal &&
        condition.loadValue != condition.value &&
        condition.type !== "text" &&
        condition.type !== "longtext"
      ) {
        let el;
        if (condition.datagridCode) { //表格的字段
          el = this.$refs[condition.newName];
        } else {
          el = this.$refs[condition.name];
        }
        condition.loadValue = null;
        $(el).change();
      }
    },
    "showVal": function (newVal, oldVal) {
      if (newVal !== oldVal) {
        const { condition = {} } = this.params;
        const { columnType } = condition;
        if (['DECIMAL', 'MONEY'].includes(columnType)) {
          // 记录修改值
          this.getNumberInRange(newVal, this.params.condition);
        }
        // TODO 可以格式化作为提交值
        this.params.condition.formVal = newVal;
      }
    }
  },

  mounted() {
    const $this = this;
    const { value: initVal } = this.params.condition;
    // console.log(this.params.condition.newName);
    // this.$emit('formadd', this);
    this.dispatch("iForm", "form-add", this);
    if (this.params.condition.columnType == "MONEY" && !this.params.condition.isPrefer) {
      this.isShowMoneyDiv = true;
    }

    //视图类型,viewType=VIEW时，为查看视图
    if (window.pageConfig && window.pageConfig.viewType) {
      $this.viewType = window.pageConfig.viewType;
    }

    $this.workflowEnabled = window.pageConfig && window.pageConfig.workflowEnabled;

    this.bindEvents = {
      ...this.params.condition.events
    }

    // this.showVal = initVal;
    this.formatValue();

    this.lastEdit = this.showVal;
  },

  methods: {
    //获取焦点显示金额的值
    focusMoneyFormat: function () {
      if (this.params.condition.columnType == "MONEY" || ['THOUSAND', 'TEN_THOUSAND'].indexOf(this.params.condition.showFormat) > -1) {
        this.isShowMoneyDiv = false;
      }
    },

    // 是否为百分比
    isPercent: function (condition) {
      const { showFormat } = condition;
      return showFormat === 'PERCENT';
    },

    // 数值范围
    getNumberInRange: function (num, condition) {
      const { precision } = condition;
      let temp = num;
      let rangeNum = num;
      if (precision && precision > 0) {
        // 浮点数最大值限制
        temp = num * 10 ** precision;
      }
      if (temp > 2 ** 53 - 1 || temp < -(2 ** 53) + 1) {
        rangeNum = this.lastEdit;
      } else {
        this.lastEdit = num;
      }
      return rangeNum;
    },

    changeNumber: function (value, isValidate = false) {
      if (['DECIMAL', 'MONEY'].includes(this.params.condition.columnType)) {
        let condition = this.params.condition;
        this.showVal = this.getNumberInRange(value, condition);
      }
      if (isValidate)
        this.$nextTick(() => {
          setTimeout(() => {
            this.validate('', 'save');
          }, 200);
        })
    },

    /**
     * 失去焦点之后格式化字段
     * 1.小数根据小数位数格式化
     * 2.金额根据默认、千分位或者万分位和小数位数格式化
     * 3.格式化数字类型的字段
     * 4.格式化编码、字符、摘要、长文本类型字段，去除空格
     */
    blurFormat: function () {
      const numberType = ["DECIMAL", "MONEY"];
      const textType = ["text", "longtext"]; //编码、字符、摘要、长文本类型字段
      const showType = ['THOUSAND', 'TEN_THOUSAND'];
      const integerType = ["INTEGER", "LONG"];
      const { condition = {} } = this.params;
      const { columnType, type, showFormat } = condition;
      let numberVal = this.showVal;
      const isNumber = Number(numberVal) || Number(numberVal) === 0;
      if (!numberVal) return;
      if (textType.indexOf(type) > -1) this.showVal = numberVal.toString().replace(/\s+/g, "");
      if (integerType.indexOf(columnType) > -1 && isNumber) {
        numberVal = this.getNumberRange(columnType, numberVal);
        if (isInteger(numberVal) || isLong(numberVal)) {
          this.showVal = parseInt(numberVal, 10);
        }
        if (isDecimal(numberVal)) {
          this.showVal = _commonJs.numRound(numberVal);
        }
      } else if (numberType.indexOf(columnType) > -1 && isNumber) {
        const precisionVal = getFormatPrecision(condition, numberVal);
        const valueFormat = getShowFormat(precisionVal, showFormat);
        this.showVal = valueFormat;
        if (showType.indexOf(showFormat) > -1) {
          condition.valueFormat = valueFormat;
          this.showVal = precisionVal;
        }
      }

      if (columnType === 'MONEY' || showType.indexOf(showFormat) > -1) {
        this.isShowMoneyDiv = true;
      }
    },

    getNumberRange: function (type, value) {
      let numberVal = value;
      switch (type) {
        case 'INTEGER':
          if (numberVal > 2 ** 31 - 1 || numberVal < - (2 ** 31)) {
            numberVal = numberVal > 0 ? numberVal.slice(0, 10) : numberVal.slice(0, 11);
          }
          break;
        case 'LONG':
          if (numberVal > 2 ** 63 - 1 || numberVal < -(2 ** 63)) {
            numberVal = numberVal > 0 ? numberVal.slice(0, 19) : numberVal.slice(0, 20);
          }
          break;

        default:
          break;
      }
      return numberVal;
    },

    //点击金额获取焦点切换金额显示格式
    switchMoneyFormat: function () {
      const $this = this;
      if ($this.params.condition.readonly == true) {
        //查看视图不切换金额显示格式
        return;
      }
      // let decimal = this.params.condition.value;
      // if (isDecimal(decimal)) {
      //   let fmt = this.params.condition.showFormat;
      //   let precision = this.params.condition.precision;
      //   this.showVal = _commonJs.getNumberFormat(
      //     fmt,
      //     decimal,
      //     precision
      //   );
      // }
      $this.isShowMoneyDiv = false;
      $this.$nextTick(function () {
        let name = $this.params.condition.newName || $this.params.condition.name;
        $this.$refs[name] && $this.$refs[name].focus();
      });
    },

    /**
     * 校验表单数据
     * 不校验情况：
     * 保存（不验证非空, 验证格式），驳回，查看视图提交
     * 表格不可编辑不校验
     * @param callback 回调函数
     * @param type 操作类型
     * @param status 工作流状态
     * @param isValidate 是否校验 
     */
    validate: function (cb, type, status, isValidate) {
      this.operateType = type;
      this.status = status;
      let { condition = {} } = this.params;
      const { regionType, isEditable, name, rules, valueId, showFormat, columnType, oldMultiLists = {}, newMultiLists = {} } = condition;
      // 使用 async-validator
      const validator = new AsyncValidator({ [name]: rules });
      const noValidateStatus = ['cancel', 'reject'];
      let value = valueId ? valueId : this.showVal;
      if (isTextComp(condition)) { value = this.showVal }; // 参照关联文本会带valueId
      if ((isInteger(value) || isLong(value)) && showFormat == "PERCENT") { value = Number(value / 100).toString(); }; // 百分比值校验
      if (columnType === 'MULTSELECT') value = (oldMultiLists.ids) || newMultiLists.ids;
      if (Object.prototype.toString.call(value) === "[object Array]")
        value = value.join(",");
      let model = { [name]: value };
      validator.validate(
        model,
        { keys: [name], first: true, firstFields: true, debug: true },
        (errors, type) => {
          this.isShowMes = errors ? true : false;
          this.message = errors ? errors[0].message || '' : "";
          const hasEmptyCharacter = this.message.indexOf(this.$t('validate.error.empty')) > -1;
          const scroollToError = () => {
            this.myToast(this.message);
            this.scrollToError(this);
          }

          if (isValidate && this.isShowMes) {
            scroollToError();
          } else {
            // 校验非空
            if (this.isShowMes) {
              if (this.operateType == "submit" && !noValidateStatus.includes(this.status) && this.viewType !== 'VIEW') {
                scroollToError();
                if (regionType === "DATAGRID" && isEditable === false) {
                  this.message = '';
                }
              } else if (this.operateType !== "save") {
                this.message = '';
              }

              // 校验规则
              if (this.operateType == "save") {
                if (hasEmptyCharacter) this.message = "";
                else scroollToError();
              }
            }

          }
          if (cb && typeof cb == "function") cb(this.message);
        }
      );
    },

    //获取DOM元素到页面顶部的距离
    getElementToPageTop: function (el) {
      let actualTop = el.offsetTop;
      let current = el.offsetParent;
      while (current !== null) {
        actualTop += current.offsetTop;
        current = current.offsetParent;
      }
      return actualTop;
    },

    //滚动条滑动到错误字段位置
    scrollToError: function (el) {
      var data = this.params.condition;
      // 页签滚动
      this.getCompByName('tabComp').switchTab(data.tabIndex, () => {
        var top = this.getElementToPageTop(el.$el);
        var elOffsetTop = el.$el.offsetTop;
        if (document.querySelector(".m_head")) {
          var headerHeight = document.querySelector(".m_head").scrollHeight;
          document.querySelector(".m_centerBox").scrollTop =
            top - headerHeight - elOffsetTop;
        } else {
          document.querySelector(".m_centerBox").scrollTop = top - elOffsetTop;
        }
      });
    },

    //获取样式配置信息
    getCssStyle: function (config) {
      return _commonJs.getCssStyle(config);
    },

    // 获取condition
    getCondition: function () {
      // return this.$refs.input;
      return this.params.condition;
    },

    clickEvent: function (condition) {
      condition.clickEvent && condition.clickEvent(condition);
    },

    // 格式化值
    formatValue: function () {
      // 格式化数值
      const { params = {} } = this;
      const { condition = {} } = params;
      const { columnType, showFormat, precision, value } = condition;
      const showType = ['THOUSAND', 'TEN_THOUSAND', 'MONEY'];
      this.showVal = value;

      if (!value) return;

      if (showType.indexOf(showFormat) > -1) {
        condition.valueFormat = _commonJs.formatMoney(showFormat, precision, value);
        this.isShowMoneyDiv = true;
      }

      //数据为金额类型
      if (value && columnType == "MONEY") {
        this.showVal = _commonJs.getNumberFormat(showFormat, value, precision);
        return;
      }

      // 小数 精度显示类型
      if (precision || columnType == "DECIMAL") {
        let decimal = value;
        if (isDecimal(decimal)) {
          if (showFormat == "PERCENT") {
            //百分比显示
            decimal = Number(decimal * 100)
              .toFixed(Number(precision))
              .toString();
          } else {
            decimal = Number(decimal)
              .toFixed(Number(precision))
              .toString();
          }
        } else {
          decimal = "";
        }
        this.showVal = decimal;
        return;
      }

      // 整型和长整型类型
      if (columnType == "INTEGER" || columnType == "LONG") {
        let intValue = value;
        if (isInteger(intValue) || isLong(intValue)) {
          if (showFormat == "PERCENT") {
            //百分比显示
            intValue = Number(intValue * 100).toString();
          } else {
            intValue = Number(intValue).toString();
          }
        } else {
          intValue = "";
        }
        this.showVal = intValue;
      }
    },

    //获取样式配置信息
    getCssStyle: function (config, element) {
      return _commonJs.getCssStyle(config, element);
    },

    showFormatData: function () {
      const { condition = {} } = this.params;
      const { valueFormat } = condition;
      if (isNaN(this.showVal)) {
        return this.showVal;
      }
      return valueFormat;
    },

    // 切换滑动
    switchScroll: function (flag) {
      event.stopPropagation();
      const tabComp = this.getCompByName('tabComp');
      tabComp.swipeable = flag;
      this.focus = !flag;
    },

    touchmoveFuc: function () {
      if (this.focus) {
        event.stopPropagation();
      }
    }
  }
};
</script>
<style lang="less" scoped>
@import "./index.less";
</style>


// WEBPACK FOOTER //
// src/components/cellComp/iInput/index.vue