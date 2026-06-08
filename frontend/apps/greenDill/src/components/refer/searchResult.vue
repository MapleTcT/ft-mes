<template>
  <div class="">
    <div class="m_search">
      <input
        ref="input"
        v-on:keyup="keySearch"
        class="inp_search"
        type="text"
        :placeholder="placeholderClickText"
      />
      <i
        class="icon_close"
        v-show="showClearBtn"
        v-on:click="clearSearch"
      ></i>
      <span
        class="search_cancel"
        v-on:click="$emit('hideSearch')"
      >{{$t('button.cancel.text')}}</span>
    </div>
    <div class="m_centerBox">
      <div class="m_staffBoxWrap">
        <div class="ms_wrap">
          <!-- <groupSlideList v-if="showFlag" ref="groupSlideChild" :propDoubleBind.sync="propDoubleBind" :propSingleBind="propSingleBind"></groupSlideList> -->
          <groupSlideList
            v-if="showFlag"
            ref="groupSlideChild"
            v-on:updateChoooseList="this.params.updateChoooseList"
            :propBind="propBind"
          ></groupSlideList>
          <template v-else>
            <organizList
              v-on:updateChoooseList="this.params.updateChoooseList"
              ref="organizChild"
              :propBind="propBind_O"
            ></organizList>
          </template>
        </div>
      </div>
    </div>

  </div>
</template>

<script>
import groupSlideList from "./groupSlideList";
import organizList from "./organizList";
import { publicGetAxios } from '@/assets/js/util/common.js';
import baseService from './service.js';
import lodash from 'lodash';
export default {
  name: "searchResult",
  data() {
    return {
      searchInpVal: "",
      listType: "flow",
      formName: "",
      listData: "",
      allLoaded: false,
      onloadFun: null,
      groupData: [],
      showFlag: false,
      isSingleCheck: 0,
      showClearBtn: false,
      placeholderClickText: "",
      propBind: {
        footerH:
          this.$parent.$refs.footer && this.$parent.$refs.footer.clientHeight,
        // listUrl: this.params.listUrl,
        // url: this.basicUrl,
        isSingleCheck: this.params.isSingleCheck
      },
      propBind_O: {
        linkcallBack: this.linkcallBack,
        selectCallBack: this.selectCallBack,
        listUrl: this.params.listUrl,
        nameType: this.params.nameType,
        isSingleCheck: this.params.isSingleCheck,
        showPeopleNum: false,
        isSearch: true
      }
    };
  },
  props: {
    params: {
      type: Object,
      default: {}
    }
  },
  mounted() {
    // 获取焦点
    this.$refs.input.focus();
    this.isSingleCheck = this.params.isSingleCheck;
    this.showFlag = this.params.showFlag;
    this.basicUrl = this.params.listUrl;
    this.placeholderClickText = this.params.placeholderClickText;
    this.searchData = lodash.debounce(this.searchData, 800);
    this.getCrossCompany();
  },
  methods: {
    // 跨公司查询条件
    getCrossCompany: function () {
      const { crossCompanyFlag } = this.GetRequest(window.location.href);
      const { type } = window.pageConfig || {};
      if (crossCompanyFlag === 'true' && ['staff', 'user'].includes(type)) {
        this.basicUrl = baseService[`querry${type}`];
        if (this.basicUrl.indexOf('?') === -1) this.basicUrl = this.basicUrl + '?';
      }
    },
    // 清空搜索框
    clearSearch: function () {
      this.$refs.input.value = "";
      this.showClearBtn = false;
      this.searchData("");
    },

    searchData: function (value) {
      let url = this.basicUrl + "&keyword=" + value;
      if (this.params.nameType == "1") {
        this.getslideData(url);
      } else {
        this.getOrganizData(url);
      }
    },

    // 获取slide数据
    getslideData: function (url) {
      publicGetAxios(
        {
          url: url,
          data: {
            "current": 1,
            "pageSize": 20
          },
          type: "get"
        },
        (res) => {
          let data = res.data;
          // this.groupData = data.result;
          let k = this.redrawSelectData(
            this.$store.state.curChooseList,
            data.result
          );
          this.groupData = k;
        }
      );
    },

    // 获取organization数据
    getOrganizData: function (url) {
      this.$refs.organizChild.getOrganizData(url);
    },

    // 输入框搜索
    keySearch: function () {
      let value = event.target.value;
      value = value.replace(/^\s+|\s+$/g, "");
      event.target.value = value;
      const url = this.basicUrl + "&keyword=" + value;
      if (value != "") {
        this.showClearBtn = true;
        if (this.showFlag) {
          this.$refs.groupSlideChild.getLoadData({
            url: url,
            isRefresh: true
          });
        } else {
          this.searchData(value);
        }
      }
    }
  },

  components: {
    groupSlideList,
    organizList
  }
};
</script>

<!-- 样式加载 -->

<style lang="less" scoped>
@import "../../assets/css/groupSubset.less";

.g_container {
  .m_centerBox {
    position: absolute;
    top: 190 / @rem;
    bottom: 86 / @rem;
    left: 0;
    right: 0;

    /deep/ .group_list {
      position: absolute;
      top: 0;
      bottom: 0 / @rem;
      overflow: auto;
      left: 0;
      right: 0;
      max-height: none !important;
    }
  }
}
</style>


// WEBPACK FOOTER //
// src/components/refer/searchResult.vue