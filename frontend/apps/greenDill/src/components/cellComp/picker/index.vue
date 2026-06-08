
<template>
  <div :class="!params.readonly?'pr20':''">
    <iInput :params="{condition: params,clickEvent: showPickerPop}">
    </iInput>

    <i
      v-if="!params.readonly"
      class="more-icon"
      @click="bindEvents.onClick && bindEvents.onClick(), showPickerPop(params)"
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
    </div>
    <checkList
      :isShow="params.multable && params.isShowPicker"
      :con="params"
      v-on:checkcallback="checkcallback"
      :value="params.valueId?params.valueId:[]"
      :name="params.pickerName"
      :options="params.slots"
    ></checkList>
  </div>
</template>


<script>
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import iInput from "@/components/cellComp/iInput/index.vue";
import { Popup } from "mint-ui";
import Emitter from "@/assets/js/emitter.js";
import checkList from "@/components/cellComp/checkList/index.vue";

export default {
  name: "picker",
  props: {
    params: {
      type: Object,
      default: {}
    },
    callbackFunc: {
      type: Function,
      default: () => { }
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
      bindEvents: {},
    };
  },
  beforeMount() {
  },

  mounted() {
    this.bindEvents = {
      ...this.params.events
    }

    const { params: element } = this;
    const { value: curVal, name: nameKey } = element;
    const data = curVal;

    if (element.complex) return;

    // 多选
    if (element.type == "select" && element.multable) {
      let values = _commonJs.formatValue(data, element, nameKey);
      values = values ? values.split(",") : [];
      let valueKeys = [],
        valuesIds = [];
      for (let j = 0; j < element.slots.length; j++) {
        var et = element.slots[j];
        for (let i = 0; i < values.length; i++) {
          var elt = values[i];
          if (elt == et.value || elt == et.label) {
            valueKeys.push(et.label);
            valuesIds.push(et.value);
          }
        }
      }

      element.value = valueKeys.join(",");
      element.valueId = valuesIds;
      element.loadValue = element.value;

      return;
    };

    // 布尔
    if (element.columnType == "BOOLEAN") {
      if (element.value === this.$t('option.boolean.yes')) {
        element.valueId = true;
      } else if (element.value === this.$t('option.boolean.no')) {
        element.valueId = false;
      }
      element.loadValue = element.value;
      return;
    };

    if (element.slots && element.type == "select") {
      element.value = _commonJs.formatValue(data, element, nameKey);
      if (element.value && element.slots[0] && element.slots[0].values) {
        let valueIndex = element.slots[0].values.indexOf(element.value);
        element.valueId = element.slots[0].valueKey[valueIndex];
      }
      element.loadValue = element.value;

      return;
    }
  },

  methods: {
    //显示下拉
    showPickerPop: function (data) {
      const $this = this;
      _commonJs.showDropdown($this, data);
    },

    //下拉选择事件
    pickerEvent: function (type, value, name, con) {
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

    getFields: function () {
      return this.dispatch("iForm", "getFileds", this);
    }
  },
  //引用组件
  components: { iInput, checkList }
};
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less" scoped>
</style>



// WEBPACK FOOTER //
// src/components/cellComp/picker/index.vue