<template>
  <header
    v-if="!hidden || isRefer"
    class="m_head"
    :style="{background: bgColor}"
  >
    <div class="m_hbox">
      <div
        v-on:click="pageBackFunc"
        class="head_arrow"
      >
        <a
          href="javascript:;"
          class="m_arrow"
        ></a>
      </div>
      <h2 class="m_title">{{headTitle}}</h2>

      <!--按钮-->
      <div
        class="head_btn"
        v-if="!isRefer"
      >
        <!--刷新-->
        <span
          class="head_refresh"
          @click="refresh"
        >
          <a
            class="m_refresh"
            href="javascript:;"
          ></a>
        </span>

        <!--头部单据操作按钮-->
        <HeadBtn
          ref="headBtn"
          :listButtons="listButtons"
          :isAllowProxy="isAllowProxy"
          :isCustomBtn="true"
        ></HeadBtn>
      </div>

    </div>
  </header>
</template>
<script>
import { Header } from 'mint-ui';
import HeadBtn from "./HeadBtn";
import { getIntl, getIsHideWebTitle } from '../../assets/js/util/common.js';

export default {
  name: 'HeadBar',
  props: ["params", "listButtons", "isAllowProxy", "headTitle"],
  data() {
    return {
      bgColor: "",
      hidden: false,
      viewType: "", //视图类型
      classType: "",
      newUrlList: [],
      isRefer: false, //是否参照视图
      viewType: "", //视图类型
    }
  },
  beforeMount() {
    this.hidden = getIsHideWebTitle();
    if (window.pageConfig) {
      this.bgColor = window.pageConfig.bgColor ? window.pageConfig.bgColor : ''; // 背景色
      this.viewType = window.pageConfig.viewType ? window.pageConfig.viewType : "";
      if (this.viewType === "REFERENCE") { //参照视图
        this.isRefer = true;
      }
      // this.hidden = window.pageConfig.navBar.hidden?window.pageConfig.navBar.hidden:false;
    }
  },

  mounted() {
    if (window.pageConfig && window.pageConfig.viewType) {
      this.viewType = window.pageConfig.viewType;
    }
  },

  methods: {
    // 页面返回方法
    pageBackFunc: function () {
      this.$emit('pageBackFunc');
    },

    //刷新
    refresh: function () {
      location.reload(window.location.href);
    }
  },

  components: {
    HeadBtn
  }
}
</script>
<style lang="less" scoped>
@import "../../assets/css/global.less";
</style>


// WEBPACK FOOTER //
// src/components/HeadBar/HeadBar.vue