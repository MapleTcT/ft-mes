
<template>
  <div :class="!params.readonly?'pr30':''">
    <iInput :params="{condition: params}">
    </iInput>
    <template v-if="params.columnType=='MULTSELECT'">
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
        >{{$t('placeholder.choose.text')}}</span>
      </span>
    </template>

    <mt-popup
      class="refer-popup"
      v-model="popupVisible"
      popup-transition="popup-fade"
    >
      <h2>{{$t('cellComp.reference.choosed')}}（{{params.newMultiLists.values && params.newMultiLists.values.length}}）</h2>
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
      v-if="!params.readonly && !params.value"
      class="refer-icon"
      @click="bindEvents.onClick && bindEvents.onClick($event,params.index), openRefSelect(params)"
    ></i>
    <!--多选-->
    <i
      v-else-if="!params.readonly && params.value && params.multable"
      class="refer-icon"
      @click="bindEvents.onClick && bindEvents.onClick($event,params.index), openRefSelect(params)"
    ></i>
    <!--单选-->
    <i
      v-else-if="!params.readonly && params.value && !params.multable"
      class="refer-delete"
      @click="deleteRefValue(params)"
    ></i>
    <i
      v-else-if="!params.readonly"
      class="more-icon"
      @click="bindEvents.onClick && bindEvents.onClick($event,params.index), showPickerPop(params)"
    ></i>
  </div>
</template>


<script>
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import iInput from "@/components/cellComp/iInput/index.vue";
import { Popup } from "mint-ui";
import Emitter from "@/assets/js/emitter.js";
import { union } from 'lodash';
import { OpenReferViewSelect, deleteReferValues, tryCatch } from './util.js';

export default {
  name: "reference",
  props: {
    params: {
      type: Object,
      default: {}
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
  beforeMount() {
  },

  mounted() {
    this.bindEvents = {
      ...this.params.events
    }
  },

  methods: {
    //显示下拉
    showPickerPop: function (data) {
      const $this = this;
      _commonJs.showDropdown($this, data);
    },

    // 打开参照
    openRefSelect: function (data) {
      const $this = this;
      OpenReferViewSelect($this, data);
    },

    //删除参照的值
    deleteRefValue: function (data) {
      const $this = this;
      if (tryCatch(this.bindEvents.onbeforeclear, data, this.params.index) === false) return false;
      deleteReferValues($this, data);
      tryCatch(this.bindEvents.onafterclear, data, this.params.index);
    },

    // 获取valueId
    getReferValueId: function (list, tempName) {
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
    openReferPage: function () {
      this.popupVisible = true;
    },

    // 多选控件删除数据
    delReferItem: function (list, index) {
      if (tryCatch(this.bindEvents.onbeforeclear, list, this.params.index) === false) return false;
      if (list) {
        list.values && list.values.splice(index, 1);
        list.ids && list.ids.splice(index, 1);
      }
      tryCatch(this.bindEvents.onafterclear, list, this.params.index);
      this.$forceUpdate();
    },

    // 多选控件获取id
    getReferListId: function (data) {
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
    getReferDataCallBack: function (list, data) {
      let tempListName = list.map(arr => eval(`arr.${data.displayfield}`));
      let tempListId = list.map(arr => arr.id);
      data.newMultiLists.values = data.newMultiLists.values
        ? union(data.newMultiLists.values, tempListName)
        : tempListName;
      data.newMultiLists.ids = data.newMultiLists.ids
        ? union(data.newMultiLists.ids, tempListId)
        : tempListId;
      data.value = data.newMultiLists.values;

      return data;
    },

    getFields: function () {
      return this.dispatch("iForm", "getFileds", this);
    }
  },
  //引用组件
  components: { iInput }
};
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less" scoped>
@import "./index.less";
</style>



// WEBPACK FOOTER //
// src/components/cellComp/reference/index.vue