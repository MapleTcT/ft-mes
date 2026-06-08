<template>
  <span :class="{ 'mp-section-wrapper': isAndroidLessVersionFive }">
  <template v-if="params.type!='section'">
    <!--查询字段-->
    <input
        v-if="params.type=='select' && params.columnType == 'SYSTEMCODE' && params.isTreeSystemCode"
        :class="curShow=='top'?'inp_import searchPanel' :'mp-inp searchPanel'"
        v-on:keypress="searchByEnter"
        :name="params.cellCode"
        :data-name="params.name"
        :ref="params.name"
        type="text"
        v-model="params.value"
        :readonly="params.readonly"
        @click="openSystemCode(params)"
        :placeholder="params.placeholder"
        @blur="validate">
    <input
        v-else
        :class="curShow=='top'?'inp_import searchPanel' :'mp-inp searchPanel'"
        v-on:keypress="searchByEnter"
        :name="params.cellCode"
        :data-name="params.name"
        :ref="params.name"
        type="text"
        v-model="params.value"
        :readonly="params.readonly"
        @click="params.type == 'date'?selectDate(params):params.clickEvent&&params.clickEvent(params)"
        :placeholder="params.placeholder"
        @blur="validate">

    <!--高级查询-->
    <i
      v-if="params.isPrefer && curShow=='side'"
      class="sidepanel btnPrefer"
      @click="params.referEvent(params)">
    </i>
    <!--系统编码树形，值为空-->
    <i v-else-if="!params.isPrefer && params.type=='select' && params.columnType == 'SYSTEMCODE' && params.isTreeSystemCode && !params.value && curShow=='side'"
        class="sidepanel btnPrefer"
        @click="openSystemCode(params)">
    </i>
    <!--系统编码树形，值不为空-->            
    <i v-else-if="!params.isPrefer && params.type=='select' && params.columnType == 'SYSTEMCODE' && params.isTreeSystemCode && params.value && curShow=='side'"
        class="systemcode-delete"
        @click="emptySearchInp(params)">
    </i>

    <!--快速查询-->
    <span v-if="curShow=='top'" class="btn">
      <i class="btnClear" @click="emptySearchInp(params)"></i>
      <i v-if="params.isPrefer" class="topbar btnPrefer" @click="params.referEvent(params)"></i>
      <!--系统编码树形-->
      <i v-else-if="params.type=='select' && params.columnType == 'SYSTEMCODE' && params.isTreeSystemCode"
          class="topbar btnPrefer"
          @click="openSystemCode(params)">
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
        year-format="{value}年"
        month-format="{value}月"
        date-format="{value}日"
        hour-format="{value}时"
        minute-format="{value}分"
        second-format="{value}秒"
        @confirm="dateConfirm(params)"
      >
      </mt-datetime-picker>
    </div>
    <custom-picker
      v-if="params.type=='select'"
      :name="params.pickerName"
      :isShow="params.isShowPicker"
      :slots="params.slots"
      :value="params.value"
      v-on:pickercallback="pickerEvent"
    >
    </custom-picker>
    <!--系统编码树形-->
    <systemCodeTree
        ref="systemCodeTree" 
        :params="params">
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
      :ref="params.name"
      v-model="params.value1"
      :readonly="params.readonly"
	  @blur="validate"
    >
    <i class="spline_from"></i>
    <input
      class="mp-inp half nth-child searchPanel"
      v-on:keypress="searchByEnter"
      :name="params.cellCode"
      :data-name="params.name"
      :ref="params.name"
      type="text"
      v-model="params.value2"
      :readonly="params.readonly"
	  @blur="validate"
    >
  </template>
  </span>
</template>
<script>
import AsyncValidator from 'async-validator';
import { Toast, Indicator, Field  } from 'mint-ui';
import Emitter from '@/components/form/emitter.js';
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import systemCodeTree from "@/components/form/systemCodeTree";
export default {
  name: 'searchInput',
  props: ["params", "curShow"],
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
    _commonJs.startAndEndTime($this, 1990);
  },
  mounted () {
    // 获取当前安卓手机版本号小于等于5的进行样式处理
    this.isAndroidLessVersionFive = _commonJs.isLessEqualAndroidVersion(5);
  },

  methods: {
    //校验高级查询和快速查询数据，包含："INTEGER", "LONG", "DECIMAL", "MONEY"
    validate() {
		const $this = this;
		var data = $this.params;
		var pVlaue = $this.params.value;
		var namekey = $this.params.namekey;
		var message;
		var isSuccess = true;
		if (pVlaue) {
			switch(data.columnType) {
				case "INTEGER":
					if (_commonJs.isInteger(pVlaue)) {
						isSuccess = true;
					} else {
						message = namekey + "字段为整数，您输入的数据不正确！";			
						isSuccess = false;
						$this.myToast(message);
					}
					break;
				case "DECIMAL":
					if (_commonJs.isDecimal(pVlaue)) {
						isSuccess = true;
					} else {
						message = namekey + "字段为小数，您输入的数据不正确！";		
						isSuccess = false;
						$this.myToast(message);
					}
					break;
				case "MONEY":
					if (_commonJs.isDecimal(pVlaue)) {
						isSuccess = true;
					} else {
						message = namekey + "字段为金额，您输入的数据不正确！";	
						isSuccess = false;
						$this.myToast(message);
					}
					break;
				case "LONG":
					if (_commonJs.isLong(pVlaue)) {
						isSuccess = true;
					} else {
						message = namekey + "字段为长整型，您输入的数据不正确！";	
						isSuccess = false;
						$this.myToast(message);
					}
					break;
				default:
					break;
			}
		}
		return isSuccess;
    },

    //点击事件
    handleClick: function(e) {
      if (e.target.nodeName.toLowerCase() === "a") {
        let li = e.target.parentNode.parentNode.children;
        const tag = e.target.name;
        this.clearSiblings(li, tag);
        var dtIndex = this.selectItem.indexOf(tag);
        if (dtIndex > -1) {
          this.selectItem.splice(dtIndex, 1);
        } else {
          this.selectItem.push(tag);
        }
      }
    },

    // 打开时间选择器
    selectDate: function(item) {
      // 如果已经选过日期，则再次打开时间选择器时，日期回显（不需要回显的话可以去掉 这个判断）
      if (item.value) {
        this.dateVal = item.value;
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
          }else if (item.showformat == "YMD") {
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
    dateConfirm: function(item) {
      const $this = this;
      // 限制时间范围
      const pickname = item.pickerName;
      let end, start;
      // const value = this.$refs[pickname][0].value;
      if(pickname.indexOf("End") > -1){
        let startName = pickname.replace("End", "Start");
        let refs = $this.$parent.$refs[startName][0] && $this.$parent.$refs[startName][0].params?$this.$parent.$refs[startName][0].params:'';
        end = $this.$refs[pickname] && $this.$refs[pickname].params?new Date($this.$refs[pickname].params.value).getTime():'';
        start = refs?new Date(refs.value).getTime():'';
      }else if(pickname.indexOf("Start") > -1){
        let endName = pickname.replace("Start", "End");
        let refs = $this.$parent.$refs[endName][0] && $this.$parent.$refs[endName][0].params?$this.$parent.$refs[endName][0].params:'';
        end = refs?new Date(refs.value).getTime():'';
        start = $this.$refs[pickname] && $this.$refs[pickname].params?new Date($this.$refs[pickname].params.value).getTime():'';
      }
      let formatTime = '';
      if (typeof this.dateVal == "object") {
        formatTime = _commonJs.getDateFormat(item.showformat, this.dateVal);        
      } else {
        formatTime = this.dateVal;
      }
      let curData = new Date(formatTime).getTime();
      if (start && pickname && pickname.indexOf("End") > -1 && curData <= start) {
        
        this.myToast("结束时间不能小于开始时间");
        return;

      } else if (end && pickname && pickname.indexOf("Start") > -1 && curData >= end) {
        this.myToast("开始时间不能大于结束时间");
        // this.startdate = value;
        // this.enddate = this.curend;
        return;

      }

      // 时间选择器确定按钮，并把时间转换成我们需要的时间格式
      item.value = formatTime
      
    },

    //下拉选择事件
    pickerEvent: function(type, value, name) {
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

    //重置查询条件
    resetClassQuery: function() {
      this.selectItem = [];
      this.queryData = this.getQueryData(this.propSearchConfigData.query);
      this.startdate = this.curstart;
      this.enddate = this.curend;
      // this.dateVal = '';
    },
    showMember: function(item) {
      // console.log(item);
      let curPage = "";
      let text = item.placeholder;
      if (text.indexOf("人员") > -1) {
        curPage = "member";
      } else if (text.indexOf("部门") > -1) {
        curPage = "department";
      } else if (text.indexOf("岗位") > -1) {
        curPage = "station";
      }
      this.$store.commit("updateData", {
        name: "curShowPage",
        value: curPage
      });
    },

    // enter键查询
    searchByEnter: function() {
      var keycode = event.keyCode;
      var searchName = event.target.value;
      //keycode是键码，13也是电脑物理键盘的 enter
      if (keycode == "13") {
        console.log(2);
        event.preventDefault();
        this.getSearchData();
        // 失去焦点
        event.target.blur();
      }
    },

    // 查询数据
    getSearchData: function() {
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

      searchUrl = this.propSearchConfigData.searchUrl;
      if (typeof searchUrl == "string") {
        searchUrl = searchUrl;
      } else if (typeof searchUrl == "object") {
        searchUrl = searchUrl.query;
      }
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
    emptySearchInp: function(obj) {
      let inputRef =  this.params.name;      
      obj.valueId = "";
      obj.value = "";
      this.$refs[inputRef].value = "";
    },

    //打开系统编码选择界面
    openSystemCode: function(data) {
        const $this = this;
        _commonJs.openSystemCodeInterface($this, data, true);
    },

    // 默认值选项
    // setDefaultValue: function(obj){
    //   const $this = this;
    //   if(obj){
    //     for (let i = 0; i < obj.length; i++) {
    //       const el = obj[i];
    //       let etName = el.name;
    //       console.log(etName)
    //       console.log($this.$refs[etName]);
    //     }
    //   }
    // },

  },
  //引用组件
  components: { systemCodeTree }
}
</script>
<style lang="less">
 /* @import "../../assets/css/sidePanel.less"; */
 /* @import "../../assets/css/editView.less"; */
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