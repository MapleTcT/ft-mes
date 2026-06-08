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
      >取消</span>
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
        listUrl: this.params.listUrl.query,
        // url: this.basicUrl,
        isSingleCheck: this.params.isSingleCheck
      },
      propBind_O: {
        linkcallBack: this.linkcallBack,
        selectCallBack: this.selectCallBack,
        listUrl: this.params.listUrl.query,
        listMoreUrl: this.params.listUrl.more,
        nameType: this.params.nameType,
        isSingleCheck: this.params.isSingleCheck,
        showPeopleNum: false,
        isSearch: true
      }
    };
  },
  props: ["params"],
  mounted() {
    let $this = this;
    // 获取焦点
    $this.$refs.input.focus();
    $this.isSingleCheck = $this.params.isSingleCheck;
    $this.showFlag = $this.params.showFlag;
    $this.basicUrl = $this.params.listUrl.query;
    $this.placeholderClickText = $this.params.placeholderClickText;
  },
  methods: {
    // 清空搜索框
    clearSearch: function() {
      this.$refs.input.value = "";
      this.showClearBtn = false;
      this.searchData("");
    },
    searchData: function(value) {
      let $this = this;
      let url = $this.basicUrl + "&queryString=" + value;
      if ($this.params.nameType == "1") {
        $this.getslideData(url);
      } else {
        $this.getOrganizData(url);
      }
    },
    // 获取slide数据
    getslideData: function(url) {
      let $this = this;
      $this.getData(
        {
          url: url,
          data: {
            "page.pageNo": 1,
            "page.pageSize": 20
          },
          type: "get"
        },
        function(res) {
          if (res.success) {
            let data = res.data;
            // $this.groupData = data.result;
            let k = $this.redrawSelectData(
              $this.$store.state.curChooseList,
              data.result
            );
            $this.groupData = k;
          }
        }
      );
    },
    // 获取organization数据
    getOrganizData: function(url) {
      this.$refs.organizChild.getOrganizData(url);
    },
    // 输入框搜索
    keySearch: function() {
      let $this = this,
        url;
      let value = event.target.value;
      value = value.replace(/^\s+|\s+$/g, "");
      event.target.value = value;
      url = $this.basicUrl + "&queryString=" + value;
      if (value != "") {
        $this.showClearBtn = true;
        if ($this.showFlag) {
          $this.$refs.groupSlideChild.getLoadData({
            url: url,
            isRefresh: true
          });
        } else {
          $this.searchData(value);
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