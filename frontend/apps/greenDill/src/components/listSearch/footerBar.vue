<template>
  <div class="m_box m_control">
    <template v-if="isWorkflow">
      <span
        class="ms_pending"
        v-on:click="getSearchData({dataId:'pending'})"
        data-id="pending"
      >
        <i></i>
        <span>{{$t('button.pending.text')}}</span>
      </span>

      <!-- 工作流按钮 -->
      <HeadBtn
        v-if="listButtons.length >0"
        ref="headBtn"
        :listButtons="listButtons"
        :isWorkflowBtn="true"
        :isBottomBtn="true"
      ></HeadBtn>
      <!--没有流程按钮-->
      <span
        v-else
        class="noWorkflowBtn_line"
      ></span>

      <!-- 头部按钮隐藏 -->
      <template v-if="headHidden">
        <div class="filter_headbtn">
          <span
            class="ms_filter"
            v-on:click="openSidePanel()"
          >
            <i></i>
          </span>
          <span class="headbtn_line"></span>
          <!-- 自定义按钮 -->
          <HeadBtn
            v-if="listButtons.length >0"
            :listButtons="listButtons"
            :isCustomBtn="true"
            :isWorkflowBtn="true"
            :isBottomBtn="true"
          ></HeadBtn>
        </div>
      </template>
      <!-- 头部按钮显示 -->
      <template v-else>
        <span
          class="ms_filter"
          v-on:click="openSidePanel()"
        >
          <i></i>
          <span>{{$t('button.filter.text')}}</span>
        </span>
      </template>

    </template>

    <!-- 基础单据 -->
    <template v-if="!isWorkflow">
      <span class="ms_pending">
      </span>
      <HeadBtn
        v-if="listButtons.length >0"
        ref="headBtn"
        :listButtons="listButtons"
        :isCustomBtn="false"
        :isWorkflowBtn="false"
        :isBottomBtn="true"
      ></HeadBtn>
      <!-- 头部按钮隐藏 -->
      <template v-if="headHidden">
        <div class="filter_headbtn">
          <span
            class="ms_filter"
            v-on:click="openSidePanel()"
          >
            <i></i>
          </span>
          <span class="headbtn_line"></span>
          <!-- 自定义按钮 -->
          <HeadBtn
            v-if="listButtons.length >0"
            ref="headBtn"
            :listButtons="listButtons"
            :isCustomBtn="true"
            :isWorkflowBtn="false"
            :isBottomBtn="true"
          ></HeadBtn>
        </div>
      </template>
      <!-- 头部按钮显示 -->
      <template v-else>
        <span
          class="ms_filter ms_filter_right"
          v-on:click="openSidePanel()"
        >
          <i></i>
        </span>
      </template>
    </template>
  </div>
</template>

<script>
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import { Toast, Indicator } from "mint-ui";
import HeadBtn from "@/components/HeadBar/HeadBtn";
import { getQueryType } from './util.js';
import { isRefenceComp } from '@/assets/js/util/common.js';

export default {
  name: "footerBar",
  data() {
    return {
    };
  },
  props: {
    headHidden: {
      type: Boolean,
      default: false,
    },
    curRefer: {
      type: Boolean,
      default: false
    },
    isWorkflow: {
      type: Boolean,
      default: false
    },
    getSearchData: {
      type: Function,
      default: () => { }
    },
    openSidePanel: {
      type: Function,
      default: () => { }
    },
    listButtons: {
      type: Array,
      default: () => []
    }
  },

  beforeMount() {

  },
  mounted() {
  },
  methods: {

  },

  //引用组件
  components: {
    HeadBtn
  }
};
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
@import "./searchBar.less";
</style>


// WEBPACK FOOTER //
// src/components/listSearch/footerBar.vue