<template>
  <div>
    <div v-if="showRefer">
      <htmlPanel
        v-if="showRefer"
        :params="{url: referConfig.url}"
      ></htmlPanel>
    </div>
    <div class="m_search">
      <!-- <form onsubmit="return false" :exporturl="this.$parent.basicUrl"> -->
      <div class="m_box m_query">
        <div
          class="ms_select dropdown"
          id="dropdown"
          v-on:click="actionSheet()"
        >
          <div
            id="inputSelect"
            class="input_select flex-item"
          >
            <span id="selectContent">{{showActionVal}}</span>
            <img
              src="../../assets/images/btn_xl.png"
              :alt="$t('searchBar.imgAlt.dropDown')"
            >
          </div>
        </div>
        <div class="flex-item ml">
          <!--快速查询-->
          <div
            class="ms_input"
            v-for="(item,index) in condition"
          >
            <searchInput
              :key="item.pickerName?item.pickerName:item.name"
              :ref="item.pickerName?item.pickerName:item.name"
              :params="item"
              curShow='top'
              :propSearchConfigData="propSearchConfigData"
            ></searchInput>
          </div>
        </div>
        <!--参照视图的高级查询-->
        <span
          v-if="curRefer"
          class="refer_filter"
          v-on:click="openSidePanel()"
        >
          <i></i>
        </span>
        <!-- 查询全部单据 -->
        <div
          class="query-btn"
          v-on:click="getSearchData({dataId:'query'})"
          data-id="query"
        >
          <span class="query-span">{{$t('button.search.text')}}</span>
        </div>
      </div>

      <footerBar
        ref="footerBar"
        :listButtons="listButtons"
        :openSidePanel="openSidePanel"
        :isWorkflow="propIsWorkflow"
        :curRefer="curRefer"
        :headHidden="hidden"
        :getSearchData="getSearchData"
        v-if="!curRefer"
      ></footerBar>

      <mt-actionsheet
        :actions="actionData"
        v-model="sheetVisible"
        :cancel-text="$t('button.cancel.text')"
      ></mt-actionsheet>
      <!--高级查询-->
      <sidePanel
        :class="{'hideSidePanel':!sidePanelVisible}"
        :propSearchConfigData="propSearchConfigData"
        :propCloseSidePanel="closeSidePanel"
        ref="sidePanel"
      >
      </sidePanel>
      <!-- </form> -->
    </div>
  </div>
</template>

<script>
import sidePanel from "@/components/listSearch/sidePanel"; //侧边栏
// import htmlPanel from "@/components/htmlPanel"; // iframe
import htmlPanel from "@/components/cellComp/htmlPanel/index.vue"; // iframe
import footerBar from './footerBar.vue';
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import filtration from "@/assets/js/util/filtration"; // 搜索筛选
import searchInput from "@/components/listSearch/searchInput";
import HeadBtn from "@/components/HeadBar/HeadBtn";
import { getQueryCondition } from './util.js';
import { getIsHideWebTitle } from '@/assets/js/util/common.js';

export default {
  name: "searchBar",
  data() {
    return {
      searchInpVal: "",
      showActionVal: "",
      actionData: [],
      sheetVisible: false,
      sidePanelVisible: false,
      condition: [],
      dateVal: "",
      queryData: [],
      eventBindConfig: [],
      startdate: "",
      enddate: "",
      curRefer: false, // 当前为参照
      referConfig: {},
      showRefer: false, // 是否显示参照
      hidden: false //是否显示头部
    };
  },
  props: {
    listButtons: {
      type: Array,
      default: () => []
    },
    propIsWorkflow: {
      type: Boolean,
      default: false
    },
    propSearchConfigData: {
      type: Object,
      default: {}
    },
    updatePanelActive: {
      type: Function,
      default: () => { }
    }
  },
  //数据监听
  watch: {
    propSearchConfigData: function (newVal, oldVal) {
      if (newVal !== oldVal) {
        let $this = this;
        this.curRefer = newVal.isRefer;
        this.queryData = newVal.query;
        this.actionData = this.getQueryList(newVal.query);
        this.setActionVal(this.actionData[0]); //默认选中第一项
        // $this.eventBindConfig = _commonJs.getEventBindConfig(newVal.query, true); //获取额外的事件绑定配置
        newVal.url = this.$parent.basicUrl;
      }
    },
    sheetVisible: function (newVal, oldVal) {
      if (newVal !== oldVal)
        this.updatePanelActive(newVal);
    }
  },
  beforeMount() {
    this.hidden = getIsHideWebTitle();
    if (window.pageConfig && window.pageConfig.navBar) {
      this.hidden = window.pageConfig.navBar.hidden ? window.pageConfig.navBar.hidden : false; //todo
    }

  },
  mounted() {
    const $this = this;
  },
  methods: {
    getQueryList: function (data) {
      var $this = this;
      var queryList = [];
      for (var i = 0; i < data.length; i++) {
        var item = data[i].element;
        item.cellCode = data[i].cellCode;
        var queryCon = getQueryCondition.bind(this)(data[i]);
        var info = {
          name: item.namekey,
          condition: queryCon,
          method: $this.setActionVal
        };
        queryList.push(info);
      }
      return queryList;
    },

    // 绑定自定义事件
    bindFunc: function () {
      const $this = this;
      // 判断是否为api里的事件
      if ($this.eventBindConfig.length > 0) {
        let evt = $this.eventBindConfig;
        for (let i = 0; i < evt.length; i++) {
          const elt = evt[i];
          if (elt.fbody && elt.fbody.indexOf("bapApi") > -1) {
            new Function(elt.split("bapApi.")[1])();
          } else if (elt.fbody && elt.fbody.indexOf("function") > -1) {
            _bapApi.bindEventConfig([elt], "searchBar");
          }
        }
      }
    },

    //清空搜索框
    emptySearchInp: function (obj) {
      if (obj.type == "section") {
        obj.value1 = "";
        obj.value2 = "";
      } else if (obj.type == "choose") {
        this.$store.commit("updateData", {
          name: "curChooseList",
          value: []
        });
      } else if (obj.type == "select") {
        obj.value = "";
        obj.valueId = "";
      } else if (obj.type == "date") {
        if (obj.pickerName == "pickerEnd") {
          this.enddate = this.curend;
        } else if (obj.pickerName == "pickerStart") {
          this.startdate = this.curstart;
        }
        obj.value = "";
      } else {
        obj.value = "";
      }
    },

    // 打开action sheet
    actionSheet: function () {
      this.sheetVisible = true;
    },

    //查询下拉菜单
    setActionVal: function (obj) {
      const $this = this;
      this.showActionVal = obj.name;
      this.condition = obj.condition;
      $this.$nextTick(function () {
        // $this.bindFunc();
        $this.$parent && $this.$parent.redrawListPanel && $this.$parent.redrawListPanel();
      });
    },

    //打开侧边菜单
    openSidePanel: function () {
      this.sidePanelVisible = true;
      this.updatePanelActive(true);
    },

    //关闭侧边菜单
    closeSidePanel: function () {
      this.sidePanelVisible = false;
      this.updatePanelActive(false);
    },

    // enter键查询
    searchByEnter: function () {
      var keycode = event.keyCode;
      var searchName = event.target.value;
      //keycode是键码，13也是电脑物理键盘的 enter
      if (keycode == "13") {
        console.log(2);
        event.preventDefault();
        this.getSearchData({
          dataId: "query"
        });
        // 失去焦点
        event.target.blur();
      }
    },

    // 查询数据
    getSearchData: function (config) {
      let $this = this;
      let temp;
      let name = this.condition[0].name;
      if (this.$refs[name] && !this.$refs[name][0].validate()) {
        return;
      }

      if (config && config.url && config.data) {
        temp = config;
      } else {
        let basicUrl;
        const dataId = config && config.dataId ? config.dataId : 'query';
        basicUrl = this.propSearchConfigData.searchUrl + dataId;
        window.localStorage.setItem("searchType", dataId);
        temp = filtration(this.condition, basicUrl);
      }

      if (!temp) {
        return false;
      }

      this.$parent.basicUrl = temp.url;
      Object.assign(temp.data, { pageNo: 1, pageSize: 20, paging: true })
      this.$parent.postData = temp.data;
      this.$parent.getLoadData(
        this.$parent.basicUrl +
        `?${$this.propSearchConfigData.crossCompanyFlag}`,
        "",
        this.$parent.postData
      );
      window.localStorage.setItem("queryType", "quickQuery"); //快速查询
    }
  },

  computed: {
    getNewSelectData() {
      return this.$store.state.curChooseList ? this.$store.state.curChooseList.name : "";
    },
    getStoreItem: function (value) {
      return this.$store.state.curChooseList;
    }
  },

  //引用组件
  components: {
    sidePanel,
    htmlPanel,
    searchInput,
    HeadBtn,
    footerBar
  }
};
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less">
@import "./searchBar.less";
.slide-fade-enter-active {
  transition: all 0.3s ease;
}

.slide-fade-leave-active {
  transition: all 0.8s cubic-bezier(1, 0.5, 0.8, 1);
}

.slide-fade-enter,
    .slide-fade-leave-to
    /* .slide-fade-leave-active for below version 2.1.8 */ {
  transform: translateX(10px);
  opacity: 0;
}
</style>


// WEBPACK FOOTER //
// src/components/listSearch/searchBar.vue