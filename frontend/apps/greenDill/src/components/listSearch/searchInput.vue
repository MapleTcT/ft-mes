<template>
  <span
    class="searchInput"
    :class="{ 'mp-section-wrapper': isAndroidLessVersionFive }"
  >
    <template v-if="params.type!='section'">
      <!--查询字段-->
      <input
        :style="getCssStyle(params.cssstyle, params)"
        :class="curShow=='top'?'inp_import searchPanel search_select' :'mp-inp searchPanel'"
        v-on:keypress="searchByEnter"
        :name="params.cellCode"
        :data-name="params.name"
        :data-ref="params.pickerName ? params.pickerName : params.name"
        :ref="params.name"
        type="text"
        v-model="params.value"
        :readonly="params.readonly"
        @click.prevent="handleClick()"
        :placeholder="params.placeholder"
        @blur="handleBlur()"
      >

      <!--高级查询-->
      <i
        v-if="params.isPrefer && curShow=='side' && !(params.readonly && params.placeholder == '')"
        class="sidepanel btnPrefer"
        @click="params.referEvent(params)"
      >
      </i>
      <!--系统编码树形，值为空-->
      <i
        v-else-if="!params.isPrefer && params.type=='select' && params.columnType == 'SYSTEMCODE' && params.isTreeSystemCode && !params.value && curShow=='side'  && !(params.readonly && params.placeholder == '')"
        class="sidepanel btnPrefer"
        @click="openSystemCode(params)"
      >
      </i>
      <!--系统编码树形，值不为空-->
      <i
        v-else-if="!params.isPrefer && params.type=='select' && params.columnType == 'SYSTEMCODE' && params.isTreeSystemCode && params.value && curShow=='side'  && !(params.readonly && params.placeholder == '')"
        class="systemcode-delete"
        @click="emptySearchInp(params)"
      >
      </i>

      <!--快速查询-->
      <span
        v-if="curShow=='top'"
        class="btn"
      >
        <i
          v-if="!(params.readonly && params.placeholder == '')"
          class="btnClear"
          @click="emptySearchInp(params)"
        ></i>
        <i
          v-if="params.isPrefer && !(params.readonly && params.placeholder == '')"
          class="topbar btnPrefer"
          @click="params.referEvent(params)"
        ></i>
        <!--系统编码树形-->
        <i
          v-else-if="params.type=='select' && params.columnType == 'SYSTEMCODE' && params.isTreeSystemCode  && !(params.readonly && params.placeholder == '')"
          class="topbar btnPrefer"
          @click="openSystemCode(params)"
        >
        </i>
      </span>
      <!-- @touchmove.prevent 阻止默认事件，此方法可以在选择时间时阻止页面也跟着滚动。 -->
      <div
        v-if="params.type=='date'"
        class="pickerPop"
        @touchmove.prevent
      >
        <!-- 年月日时分选择 -->
        <mt-datetime-picker
          :endDate="enddate"
          :startDate="startdate"
          lockScroll="true"
          :ref="params.pickerName"
          v-model="dateVal"
          class="myPicker"
          :type="params.datetype"
          :year-format="`{value}${$t('cellComp.date.unit.year')}`"
          :month-format="`{value}${$t('cellComp.date.unit.month')}`"
          :date-format="`{value}${$t('cellComp.date.unit.day')}`"
          :hour-format="`{value}${$t('cellComp.date.unit.hour')}`"
          :minute-format="`{value}${$t('cellComp.date.unit.minute')}`"
          :second-format="`{value}${$t('cellComp.date.unit.second')}`"
          :cancel-text="$t('button.cancel.text')"
          :confirm-text="$t('button.ok.text')"
          @confirm="dateConfirm(params)"
        >
        </mt-datetime-picker>
      </div>
      <template v-else-if="params.type=='select'">
        <!-- 单选 -->
        <custom-picker
          v-if="!params.multable"
          :name="params.pickerName"
          :isShow="params.isShowPicker"
          :slots="params.slots"
          :value="params.value"
          v-on:pickercallback="pickerEvent"
        ></custom-picker>
        <!-- 列表多选下拉框 -->
        <checkList
          v-else-if="params.multable"
          :isShow="params.isShowPicker"
          :con="params"
          :value="params.valueId?params.valueId:[]"
          :name="params.pickerName"
          :options="params.slots"
          v-on:checkcallback="checkcallback"
        >
        </checkList>
      </template>
      <!--系统编码树形-->
      <systemCodeTree
        ref="systemCodeTree"
        :params="params"
      >
      </systemCodeTree>
    </template>
    <template
      v-if="params.type=='section'"
      class="mp-section"
    >
      <input
        :class="type=='top'?'inp_import searchPanel' :'mp-inp searchPanel'"
        v-on:keypress="searchByEnter"
        type="text"
        :name="params.cellCode"
        :data-name="params.name"
        :data-ref="params.pickerName ? params.pickerName : params.name"
        :ref="params.name"
        v-model="params.value1"
        :readonly="params.readonly"
        v-on:click="params.clickEvent && params.clickEvent()"
        @blur="validate"
      >
      <i class="spline_from"></i>
      <input
        class="mp-inp half nth-child searchPanel"
        v-on:keypress="searchByEnter"
        :name="params.cellCode"
        :data-name="params.name"
        :data-ref="params.pickerName ? params.pickerName : params.name"
        :ref="params.name"
        type="text"
        v-model="params.value2"
        :readonly="params.readonly"
        v-on:click="params.clickEvent && params.clickEvent()"
        @blur="validate"
      >
    </template>
  </span>
</template>

<script>
import AsyncValidator from 'async-validator';
import { Toast, Indicator, Field } from 'mint-ui';
import checkList from "@/components/cellComp/checkList/index.vue";
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import filtration from "@/assets/js/util/filtration"; // 搜索筛选
import systemCodeTree from "@/components/cellComp/systemCodeTree/index.vue";
import { startAndEndTime } from '../cellComp/dateTime/util.js';
import { isInteger, isLong, isDecimal } from '@/assets/js/util/validate.js';
import { openSystemCodeInterface, formatVal, getSystemCodeJson } from '../cellComp/systemCode/util.js';
import { getType } from './util.js';

export default {
  name: 'searchInput',
  props: ["params", "curShow", "propSearchConfigData"],
  data() {
    return {
      selectItem: [],
      sortData: [],
      dateVal: "",
      queryData: [],
      eventBindConfig: [],
      startdate: "",
      enddate: "",
      referConfig: {},
      showRefer: false,
      isAndroidLessVersionFive: false // 安卓手机系统是否小于等于5.0
    }
  },
  beforeMount() {
    const $this = this;
    // 格式起始和结束时间
    startAndEndTime($this, 1990);
  },
  watch: {
    "params.value": function (newVal, oldVal) {
      const { type } = this.params;
      const { onChange = () => { } } = this.params.customFunc;
      if (newVal !== oldVal && type === 'select') {
        onChange(newVal);
      }
    },
    "params.slots": function (newVal, oldVal) {
      if (newVal !== oldVal) {
        formatVal(this.params);
      }
    },
  },
  mounted() {
    if (this.params.defaultValue) {
      _commonJs.showDefaultValue(this.params, true);
    }
    this.oldVal = this.params.value;
    const compType = getType(this.params);
    if (compType === 'treeSelect') {
      // openSystemCodeInterface(this, this.params, true);
    } else if (compType === 'select') {
      getSystemCodeJson(this.params);
    }

    // 获取当前安卓手机版本号小于等于5的进行样式处理
    this.isAndroidLessVersionFive = _commonJs.isLessEqualAndroidVersion(5);
  },

  methods: {
    //校验高级查询和快速查询数据，包含："INTEGER", "LONG", "DECIMAL", "MONEY"
    validate() {
      var data = this.params;
      var pVlaue = this.params.value;
      var namekey = this.params.namekey;
      var message;
      var isSuccess = true;
      if (pVlaue) {
        switch (data.columnType) {
          case "INTEGER":
            if (isInteger(pVlaue)) {
              isSuccess = true;
            } else {
              message = namekey + this.$t('validate.tip.invailid.interger');
              isSuccess = false;
              this.myToast(message);
            }
            break;
          case "DECIMAL":
            if (isDecimal(pVlaue)) {
              isSuccess = true;
            } else {
              message = namekey + this.$t('validate.tip.invailid.float');
              isSuccess = false;
              this.myToast(message);
            }
            break;
          case "MONEY":
            if (isDecimal(pVlaue)) {
              isSuccess = true;
            } else {
              message = namekey + this.$t('validate.tip.invailid.money');
              isSuccess = false;
              this.myToast(message);
            }
            break;
          case "LONG":
            if (isLong(pVlaue)) {
              if (data.showformat === 'PERCENT' && pVlaue % 100 !== 0) {
                // 字段为长整型百分比
                isSuccess = false;
                this.myToast(namekey + this.$t('validate.tip.invailid.longPercent'));
              } else {
                isSuccess = true;
              }

            } else {
              message = namekey + this.$t('validate.tip.invailid.long');
              isSuccess = false;
              this.myToast(message);
            }
            break;
          default:
            break;
        }
      }
      if (!isSuccess) this.params.value = ''
      return isSuccess;
    },

    //点击事件
    handleClick: function (e) {
      const { clickEvent = () => { } } = this.params;
      const compType = getType(this.params);
      switch (compType) {
        case 'treeSelect':
          this.openSystemCode(this.params);
          break;
        case 'date':
          this.selectDate(this.params);
          break;
        default:
          if (typeof clickEvent === 'function') clickEvent(this.params);
          break;
      }
      this.handleCustom('onclick');
    },

    handleBlur: function () {
      const compType = getType(this.params);
      if (compType === 'number') this.numConfirm(this.params);
      this.validate();
      this.handleCustom('onchange');
    },

    // 打开时间选择器
    selectDate: function (item) {
      if (item.readonly && item.placeholder == "") { //配置页面设置只读
        return false;
      }
      // 如果已经选过日期，则再次打开时间选择器时，日期回显（不需要回显的话可以去掉 这个判断）
      if (item.value) {
        if (item.showformat == "YMD_H") {
          this.dateVal = new Date(item.value.replace(/-/g, "/") + ":00");
        } else {
          this.dateVal = new Date(item.value.replace(/-/g, "/"));
        }
      } else {
        this.dateVal = new Date();
      }
      this.$refs[item.pickerName].open();

      //根据设置的日期和日期时间的显示格式显示
      var dateClass = this.$refs[item.pickerName].$el.getElementsByClassName("picker-slot");
      for (var i = 0; i < dateClass.length; i++) {
        dateClass[i].style.display = "block";
      }
      if (item.datetype == "date") { //日期类型
        if (item.showformat == "YM") {
          dateClass[2].style.display = "none";
        } else if (item.showformat == "Y") {
          dateClass[1].style.display = "none";
          dateClass[2].style.display = "none";
        }
      } else if (item.datetype == "datetime") { //日期时间类型
        if (item.showformat == "YMD_H") {
          dateClass[4].style.display = "none";
        } else if (item.showformat == "YMD") {
          dateClass[3].style.display = "none";
          dateClass[4].style.display = "none";
        } else if (item.showformat == "YM") {
          dateClass[2].style.display = "none";
          dateClass[3].style.display = "none";
          dateClass[4].style.display = "none";
        } else if (item.showformat == "Y") {
          dateClass[1].style.display = "none";
          dateClass[2].style.display = "none";
          dateClass[3].style.display = "none";
          dateClass[4].style.display = "none";
        }
      }
    },

    //确认选择时间
    dateConfirm: function (item) {
      const $this = this;
      // 限制时间范围
      const pickname = item.pickerName;
      let end, start;
      let showformat = item.showformat;
      // const value = this.$refs[pickname][0].value;
      if (pickname.indexOf("End") > -1) {
        let startName = pickname.replace("End", "Start");
        let refs = $this.$parent.$refs[startName][0] && $this.$parent.$refs[startName][0].params ? $this.$parent.$refs[startName][0].params : '';
        let params = $this.$refs[pickname] && $this.$refs[pickname].params;
        let endValue = params ? params.value : '';
        let startValue = refs ? refs.value : '';
        if (showformat == "YMD_H") {
          endValue = endValue && endValue.replace(/-/g, "/") + ":00";
          startValue = startValue && startValue.replace(/-/g, "/") + ":00"
        }
        end = params ? new Date(endValue).getTime() : '';
        start = refs ? new Date(startValue).getTime() : '';
      } else if (pickname.indexOf("Start") > -1) {
        let endName = pickname.replace("Start", "End");
        let refs = $this.$parent.$refs[endName][0] && $this.$parent.$refs[endName][0].params ? $this.$parent.$refs[endName][0].params : '';
        let params = $this.$refs[pickname] && $this.$refs[pickname].params;
        let endValue = refs ? refs.value : '';
        let startValue = params ? params.value : '';
        if (showformat == "YMD_H") {
          endValue = endValue && endValue.replace(/-/g, "/") + ":00";
          startValue = startValue && startValue.replace(/-/g, "/") + ":00"
        }
        end = refs ? new Date(endValue).getTime() : '';
        start = params ? new Date(startValue).getTime() : '';
      }
      let formatTime = '';
      if (typeof this.dateVal == "object") {
        formatTime = _commonJs.getDateFormat(showformat, this.dateVal);
      } else {
        formatTime = this.dateVal;
      }
      let formattime = formatTime;
      if (showformat == "YMD_H") {
        formattime = formattime && formattime.replace(/-/g, "/") + ":00";
      }
      let curData = new Date(formattime).getTime();
      // let curData = _commonJs.timeStrToTimeStamp(formatTime, showformat);

      if (start && pickname && pickname.indexOf("End") > -1 && curData < start) {
        this.myToast(this.$t('notice.endTime.range'));
        return;
      } else if (end && pickname && pickname.indexOf("Start") > -1 && curData > end) {
        this.myToast(this.$t('notice.startTime.range'));
        return;
      }

      // 时间选择器确定按钮，并把时间转换成我们需要的时间格式
      item.value = formatTime;
      this.handleCustom('onchange');

    },

    //校验数字类型字段
    numConfirm: function (item) {
      const $this = this;
      const pickname = item.pickerName;
      const name = item.name;
      let end, start;
      if (pickname.indexOf("End") > -1) {//范围最大值与范围最小值比较
        let endName = item.pickerName;
        let startName = pickname.replace("End", "Start");
        let parentRefs = $this.$parent.$refs;
        let endParams = parentRefs[endName][0] && parentRefs[endName][0].params ? parentRefs[endName][0].params : '';
        let startParams = parentRefs[startName][0] && parentRefs[startName][0].params ? parentRefs[startName][0].params : '';
        end = endParams ? endParams.value : '';
        start = startParams ? startParams.value : '';
        if (start && end && Number(end) < Number(start)) {
          endParams.value = "";
          return;
        }
      } else if (pickname.indexOf("Start") > -1) {//范围最小值与范围最大值比较
        let startName = item.pickerName;
        let endName = pickname.replace("Start", "End");
        let parentRefs = $this.$parent.$refs;
        let startParams = parentRefs[startName][0] && parentRefs[startName][0].params ? parentRefs[startName][0].params : '';
        let endParams = parentRefs[endName][0] && parentRefs[endName][0].params ? parentRefs[endName][0].params : '';
        end = endParams ? endParams.value : '';
        start = startParams ? startParams.value : '';
        if (start && end && Number(start) > Number(end)) {
          startParams.value = "";
          return;
        }
      }
    },

    //下拉选择事件
    pickerEvent: function (type, value, name) {
      var con = this.params;
      if (type == "comfirm") {
        con.value = value;
        if (con.selectData && con.slots) {
          let index = con.slots[0].values.indexOf(value);
          con.valueId = con.selectData[index];
        }
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

    //重置查询条件
    resetClassQuery: function () {
      this.selectItem = [];
      this.queryData = this.getQueryData(this.propSearchConfigData.query);
      this.startdate = this.curstart;
      this.enddate = this.curend;
      // this.dateVal = '';
    },
    showMember: function (item) {
      // console.log(item);
      let curPage = "";
      let text = item.placeholder;
      if (text.indexOf(this.$t('searchBar.reference.staff')) > -1) {
        curPage = "member";
      } else if (text.indexOf(this.$t('searchBar.reference.department')) > -1) {
        curPage = "department";
      } else if (text.indexOf(this.$t('searchBar.reference.position')) > -1) {
        curPage = "station";
      }
      this.$store.commit("updateData", {
        name: "curShowPage",
        value: curPage
      });
    },

    // enter键查询
    searchByEnter: function () {
      var keycode = event.keyCode;
      var searchName = event.target.value;
      //keycode是键码，13也是电脑物理键盘的 enter
      if (keycode == "13") {
        console.log(2);
        event.preventDefault();
        // this.getSearchData();
        this.$parent.getSearchData();
        // 失去焦点
        event.target.blur();
      }
    },

    // 查询数据
    getSearchData: function () {
      console.log(this.queryData);
      let tempData;
      let arr = this.queryData;
      let temp = [];
      let searchUrl;
      for (var i = 0; i < arr.length; i++) {
        var el = arr[i].condition;
        for (var j = 0; j < el.length; j++) {
          temp.push(el[j]);
        }
      }

      searchUrl = this.propSearchConfigData.searchUrl + "query";
      let queryList = this.selectItem.join(",");
      tempData = filtration(temp, searchUrl, queryList);

      if (!tempData) {
        return false;
      }

      // 刷新数据
      this.$parent.getSearchData(tempData);

      // 关闭侧滑菜单
      this.propCloseSidePanel();
    },

    // 清空搜索框
    emptySearchInp: function (obj) {
      let inputRef = this.params.name;
      obj.valueId = "";
      obj.value = "";
      this.$refs[inputRef].value = "";
    },

    //打开树形系统编码选择界面
    openSystemCode: function (data) {
      openSystemCodeInterface(this, data, true);
    },

    // 自定义onchange执行
    handleCustom: function (type) {
      const { value } = this.params;
      const { onChange = () => { }, onClick = () => { } } = this.params.customFunc;
      switch (type) {
        case 'onchange':
          if (this.oldVal !== value) {
            onChange(value);
          }
          break;
        case 'onclick':
          onClick()
          break;
        default:
          break;
      }

      this.oldVal = value;
    },

    getCurType: function () {
      return getType(this.params);
    },

    //获取样式配置信息
    getCssStyle: function (config) {
      return _commonJs.getCssStyle(config);
    },

  },
  //引用组件
  components: { systemCodeTree, checkList }
}
</script>
<style lang="less">
span.searchInput {
  position: relative;
  width: 100%;
  // min-width: 5.6rem;
  display: inline-flex;
}
.mp-section-wrapper {
  .inp_import {
    &::-webkit-input-placeholder {
      line-height: 1.5;
    }
  }
}
</style>


// WEBPACK FOOTER //
// src/components/listSearch/searchInput.vue