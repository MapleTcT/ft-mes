<template>
  <span>
    <!-- <htmlPanel
      v-if="showRefer"
      :params="{url: referConfig.url}"
    ></htmlPanel> -->

    <!-- 时间 -->    
    <template v-if="params && params.type=='date'">
      <dateTime :params="params"></dateTime>
    </template>

    <!-- 参照 && 下拉 -->
    <template v-else-if="params && params.type=='select'&& params.columnType != 'SYSTEMCODE'">
      <div :class="(params.isPrefer && !params.readonly) || (params.type=='select' && !params.readonly) ? switchClassName():''">
        <iInput :params="params.isPrefer?{
            condition: params,
          }:{
            condition: params,
            clickEvent: showPickerPop
          }"></iInput>
        <template v-if="params.isPrefer && params.columnType=='MULTSELECT'">
          <span class="refer-list">
            <span
              v-for="(item,index) in params.newMultiLists.values"
              class="refer-item"
            >
              {{item}}
              <i
                v-if="!params.readonly"
                v-on:click="delReferItem(params.newMultiLists, index)"
                class="close-icon"
              ></i>
            </span>
            <span
              v-on:click="openReferPage"
              v-if="params.newMultiLists.values && params.newMultiLists.values.length > 1"
              class="more"
            >......</span>
            <span
              v-else-if="(!params.newMultiLists.values || params.newMultiLists.values.length == 0) && !params.readonly"
              class="more refer-item"
              style="color: #838383;"
            >请选择</span>
          </span>
        </template>

        <mt-popup
          class="refer-popup"
          v-model="popupVisible"
          popup-transition="popup-fade"
        >
          <h2>已选（{{params.newMultiLists.values && params.newMultiLists.values.length}}）</h2>
          <span class="refer-list list-block">
            <span
              v-for="(item,index) in params.newMultiLists.values"
              class="refer-item"
            >
              {{item}}
              <i
                v-if="!params.readonly"
                v-on:click="delReferItem(params.newMultiLists, index)"
                class="close-icon"
              ></i>
            </span>
          </span>
        </mt-popup>

        <i
          v-if="!params.readonly && params.isPrefer && !params.value"
          class="refer-icon"
          @click="openRefSelect(params)"
        ></i>
        <!--多选-->
        <i
          v-else-if="!params.readonly && params.isPrefer && params.value && params.multable"
          class="refer-icon"
          @click="openRefSelect(params)"
        ></i>
        <!--单选-->
        <i
          v-else-if="!params.readonly && params.isPrefer && params.value && !params.multable"
          class="refer-delete"
          @click="deleteRefValue(params)"
        ></i>
        <i
          v-else-if="!params.readonly"
          class="more-icon"
          @click="showPickerPop(params)"
        ></i>
        <div
          class="pickerPop"
          @touchmove.prevent
        >
          <!-- 下拉框 -->
          <custom-picker
            :ref="params.name"
            :name="params.pickerName"
            :isShow="!params.multable && params.isShowPicker"
            :slots="params.slots"
            :value="params.value"
            :con="params"
            v-on:pickercallback="pickerEvent"
          ></custom-picker>
          <checkList
            :isShow="params.multable && params.isShowPicker"
            :con="params"
            v-on:checkcallback="checkcallback"
            :value="params.valueId?params.valueId:[]"
            :name="params.pickerName"
            :options="params.slots"
          ></checkList>
        </div>
      </div>
    </template>

    <!--系统编码-->
    <template v-else-if="params && params.type=='select' && params.columnType == 'SYSTEMCODE'">
      <systemCode :params="params"></systemCode>
    </template>

    <!-- text -->
    <template v-else-if="params && params.type!='section'">
      <iInput :params="{
          condition: params,
        }"></iInput>
    </template>

  </span>
</template>

<script>
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import iInput from "@/components/form/iInput";
import AsyncValidator from "async-validator";
import { Toast, Indicator, Popup } from "mint-ui";
// import htmlPanel from "@/components/htmlPanel"; // iframe
import checkList from "@/components/checkList";
import dateTime from "@/components/form/dateTime";
import systemCode from "@/components/form/systemCode";
export default {
  name: "inputTagment",
  props: ["params"],
  data() {
    return {
      layout: "",
      popupVisible: false,
      datagrid: [],
      indexName: "",
      referConfig: {},
      showRefer: false
    };
  },
  beforeMount() {
    const $this = this;
    if (this.params && this.params.index) {
      this.indexName = `&${this.params.index}`;
    }
  },
  methods: {
    //下拉选择事件
    pickerEvent: function(type, value, name, con) {
      var layout = this.layout;
      var con;
      if (type == "comfirm") {
        let index = con.slots[0].values.indexOf(value);
        con.value = value;
        con.valueId = con.slots[0].valueKey[index];
        // if(con.columnType == "DATAGRID"){
        //   con.valueId = {id:con.slots[0].valueKey[index]};
        // }
      }
      con.isShowPicker = false;
    },

    //显示下拉
    showPickerPop: function(data) {
        const $this = this;
        _commonJs.showDropdown($this, data);
    },

    // 打开参照
    openRefSelect: function(data) {
        const $this = this;
        _commonJs.OpenReferViewSelect($this, data);
    },

    //删除参照的值
    deleteRefValue: function(data) {
        const $this = this;
        _commonJs.deleteReferValues($this, data)
    },

    // 获取valueId
    getReferValueId: function(list, tempName) {
      let valueId = "";
      if (tempName.indexOf("id") == -1) {
        if (tempName.indexOf(".") == -1) {
          valueId = list.id;
          return valueId;
        }
        let index = tempName.lastIndexOf(".");
        let idName = tempName.substring(0, index);
        idName = idName ? `${idName}.id` : `id`;
        valueId = eval(`list.${idName}`);
      } else if (tempName.indexOf("id") > -1) {
        valueId = eval(`list.${tempName}`);
      }

      return valueId;
    },

    // 展开人员选择
    openReferPage: function() {
      this.popupVisible = true;
    },

    // 多选控件删除数据
    delReferItem: function(list, index) {
      if (list) {
        list.values && list.values.splice(index, 1);
        list.ids && list.ids.splice(index, 1);
      }
      this.$forceUpdate();
    },

    // 多选控件获取id
    getReferListId: function(data) {
      if (data) {
        let temp = [];
        for (let i = 0; i < data.length; i++) {
          const element = data[i];
          if (element.id) {
            temp.push(element.id);
          }
        }
        return temp;
      }
    },

    // 多选控件参照数据回填
    getReferDataCallBack: function(list, data) {
      let tempListName = list.map(arr => eval(`arr.${data.displayfield}`));
      let tempListId = list.map(arr => arr.id);
      data.newMultiLists.values = data.newMultiLists.values
        ? data.newMultiLists.values.concat(tempListName)
        : tempListName;
      data.newMultiLists.ids = data.newMultiLists.ids
        ? data.newMultiLists.ids.concat(tempListId)
        : tempListId;
      // 去重
      const resId = [],
        resValues = [];
      data.newMultiLists.ids.forEach((element, i) => {
        if (element && resId.indexOf(element) == -1) {
          resId.push(element);
          resValues.push(data.newMultiLists.values[i]);
        }
      });
      data.newMultiLists.ids = resId;
      data.newMultiLists.values = resValues;
      data.value = resValues;

      return data;
    },

    // 多选checkbox回调
    checkcallback: function(ctype, list, name, con) {
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

    //根据不同类型的字段显示不同的class名
    switchClassName: function() {
      if (this.params.type == "select" && !this.params.isPrefer) {
        return "pr20";
      } else {
        return "pr30";
      }
    }
  },
  //引用组件
  components: { iInput, checkList, dateTime, systemCode }
};
</script>
<!-- Add "scoped" attribute to limit CSS to this component only -->

<style lang="less" scoped>
@import "../../assets/css/editView.less";
</style>



// WEBPACK FOOTER //
// src/components/form/inputTagment.vue