<template>
  <div id="app">
    <div class="g_container">
      <HeadBar
        ref="headbar"
        v-on:pageBackFunc="backHistory"
      ></HeadBar>

      <div class="m_center">
        <div class="m_centerBox">
          <searchBar
            :propListType="listType"
            :propSearchConfigData="searchConfigData"
            :updatePanelActive="updatePanelActive"
          ></searchBar>
          <div class="m_box m_goodsBoxWrap">
            <div class="page-loadmore">
              <div
                :class="showPanel? 'page-loadmore-wrapper needsclick': 'page-loadmore-wrapper needsclick active'"
                ref="wrapper"
                :style="{ height: wrapperHeight + 'px' }"
              >
                <p
                  class="nolist-tip"
                  v-if="noList"
                >暂无数据</p>
                <mt-loadmore
                  :top-method="loadTop"
                  @translate-change="translateChange"
                  @top-status-change="handleTopChange"
                  :bottom-method="loadBottom"
                  @bottom-status-change="handleBottomChange"
                  :bottom-all-loaded="allLoaded"
                  ref="loadmore"
                  :auto-fill="false"
                >
                  <div
                    v-on:click="touchListEvent(listD)"
                    class="m_goodsBox needsclick"
                    v-for="(listD,lindex) in listData"
                  >
                    <!-- 选择框 -->
                    <div
                      v-if="isRefer && !isSingleCheck"
                      class="ms_check"
                      v-on:change="checklistFunc(listD)"
                    >
                      <div class="ckb_wrap">
                        <input
                          type="checkbox"
                          class="inp_ckb"
                        >
                        <label for="checkList"></label>
                      </div>
                    </div>
                    <!-- 表头 -->
                    <div
                      class="info_box bill_title"
                      v-if="listType=='flow'"
                    >
                      <div class="info_box over_width">
                        <!-- <span class="number">NFFD-20190212-101</span> -->
                        <span
                          v-if="listD.tableNo"
                          class="number"
                        >{{listD.tableNo}}</span>
                      </div>
                      <template v-if="listD.pending.taskDescription">
                        <div
                          v-if="listD.status && listD.status == 88"
                          class="info_box over_width fr todo"
                        >
                          <span class="state todo">{{listD.pending.taskDescription}}</span>
                        </div>
                        <div
                          v-if="listD.status && listD.status == 99"
                          class="info_box over_width fr effective"
                        >
                          <span class="state effective">{{listD.pending.taskDescription}}</span>
                        </div>
                        <div
                          v-if="listD.status && listD.status == 77"
                          class="info_box over_width fr hang"
                        >
                          <span class="state hang">{{listD.pending.taskDescription}}</span>
                        </div>
                        <div
                          v-if="listD.status && listD.status == 0"
                          class="info_box over_width fr undo"
                        >
                          <span class="state undo">{{listD.pending.taskDescription}}</span>
                        </div>
                      </template>

                    </div>
                    <!-- 表内容 -->
                    <div :class='isRefer && !isSingleCheck?"ms_info ml":""'>
                      <div class="info_box model">
                        <table>
                          <tbody>
                            <tr style="height:0">
                              <td
                                v-for="(width,windex) in layout.pageConfig"
                                :style='{"width":width+"%"}'
                              ></td>
                            </tr>
                            <tr v-for="(trItem,trIndex) in layout.sort" class="table_tr">
                              <td
                                v-for="(tdItem,tdIndex) in trItem"
                                :colspan='tdItem.colspan'
                                :rowspan='tdItem.rowspan'
                              >
                                <div class="subinfo needsclick">

                                  <label
                                    v-html="getShowFormat(listD,tdItem.element, tdIndex)"
                                    v-if="tdItem.element && tdItem.element.showType && tdItem.element.showType.indexOf('LABEL')>-1"
                                    class="titName needsclick"
                                    :style="getCssStyle(tdItem.cssstyle,tdItem.element)"
                                    :name="tdItem.cellCode"
                                  >{{tdItem.element.namekey}}</label>
                                  <label
                                    v-html="getShowFormat(listD,tdItem.element, tdIndex)"
                                    v-else-if="tdItem.element"
                                    class="attrVal needsclick"
                                    :style="getCssStyle(tdItem.cssstyle,tdItem.element)"
                                    :name="tdItem.cellCode"
                                  >{{getValueByKey(listD,tdItem.element)}}</label>
                                </div>
                              </td>
                            </tr>
                          </tbody>
                        </table>
                      </div>
                    </div>
                    <!-- 表尾 -->
                    <div
                      class="info_box"
                      v-if="listType=='flow'"
                    >
                      <div class="info_box bill_time">
                        <span
                          v-if="listD.createStaff"
                          class="subinfo"
                        >{{listD.createStaff.name}}</span>
                        <span
                          v-if="listD.createTime"
                          class="subinfo"
                        >{{formatTime("YMD_HMS", listD.createTime)}}</span>
                      </div>
                    </div>
                  </div>
                  <div
                    slot="top"
                    class="mint-loadmore-top"
                  >
                    <span
                      v-show="topStatus !== 'loading'"
                      :class="{ 'is-rotate': topStatus === 'drop' }"
                    >↓</span>
                    <span v-show="topStatus === 'loading'">
                      <mt-spinner
                        type="fading-circle"
                        color="#294fbc"
                      ></mt-spinner>
                    </span>
                  </div>
                  <div
                    slot="bottom"
                    class="mint-loadmore-bottom"
                  >
                    <span
                      v-show="bottomStatus !== 'loading'"
                      :class="{ 'is-rotate': bottomStatus === 'drop' }"
                    >↑</span>
                    <span v-show="bottomStatus === 'loading'">
                      <mt-spinner
                        type="fading-circle"
                        color="#294fbc"
                      ></mt-spinner>
                    </span>
                    <span v-show="allLoaded == true">加载完成没有更多数据</span>
                  </div>
                </mt-loadmore>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div
      class="m_foot"
      ref="footer"
      v-if="isRefer && !isSingleCheck"
    >
      <a
        class="m_botton btn btn_cancel"
        v-on:click="closeRefer"
      >取消</a>
      <a
        class="m_botton btn_submit"
        v-on:click="submitData"
      >确认</a>
    </div>
    <div class="edit_modal"></div>
  </div>
</template>

<script>
import searchBar from "@/components/listSearch/searchBar"; //搜索栏
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import _bapApi from "@/assets/js/itemList/bapApi.js"; //引用模块进来
import HeadBar from "@/components/HeadBar/HeadBar";
// import {
//     Toast
// } from 'mint-ui';
window._bapApi = _bapApi;
export default {
  name: "itemList",
  data() {
    return {
      listType: "",
      formName: "",
      layout: "",
      listData: [],
      allLoaded: false,
      bottomStatus: "",
      wrapperHeight: 300,
      topStatus: "",
      translate: 0,
      moveTranslate: 0,
      onloadFun: null,
      pageConfig: "",
      searchConfigData: "",
      eventBindConfig: [],
      basicUrl: "",
      isRefer: false, // 是否为参照页面
      crossCompanyFlag: "",
      postData: {
        "page.pageNo": 1,
        "page.pageSize": 20
      },
      showPanel: false,
      noList: false,
      totalPages: "",
      ptInitFlag: false, // 列表首次加载
      checklist: [], // 选中的列表
      isAndroidLessVersionFive: false // 安卓手机系统是否小于等于5.0
    };
  },
  props: ["params"],
  beforeMount() {
    let $this = this;
    if ($this.params) {
      $this.listType = $this.params.type;
      $this.formName = $this.params.name;
    } else if (window._PAGECONFIG) {
      const config = window._PAGECONFIG;
      $this.listType = config.params.type;
      $this.formName = config.params.name;

      if (config.isRefer) {
        // 判断为参照页面
        $this.isRefer = true;
      }
      $this.isSingleCheck = config.multiSelect - 0 == 1 ? 0 : 1;
    }

    // 判断是否是作为参照页面
    // 地址取参数
    let urlConfig = this.GetRequest(window.location.href);
    let conditionParams = "";
    if (urlConfig) {
      $this.isSingleCheck = urlConfig.multiSelect - 0 == 1 ? 0 : 1;
      $this.callBackFuncName = urlConfig.callBackFuncName;
      $this.closeFuncName = urlConfig.closeFuncName;
      // 分割condition 筛选参数
      let condition = urlConfig.condition;
      conditionParams = condition;

      if (urlConfig.isRefer - 0 == 1) {
        // 判断为参照页面
        $this.isRefer = true;
        // 添加跨集团搜索
        if (urlConfig.crossCompanyFlag != "undefined") {
          $this.crossCompanyFlag = `&crossCompanyFlag=${urlConfig.crossCompanyFlag}`;
        }
      }
    }
  },
  mounted() {
    let $this = this;
    let url1, url2;
    // 流程列表
    if (this.listType == "flow") {
      url1 =
        "/msService/ec/view/viewJson?view.code=wupin_1.0.0.00_wpApply_list__mobile__&entity.code=wupin_1.0.0.00_wpApply";
      // url1 = "../static/data/2.json"
      url2 = {
        query: "/wupin/wpApply/wpApply/list__mobile__-query?1=1",
        pending: "/wupin/wpApply/wpApply/list__mobile__-pending?1=1"
      };
    }

    // 读取html 里的配置
    url1 =
      (window._PAGECONFIG && window._PAGECONFIG.layoutUrl
        ? window._PAGECONFIG.layoutUrl
        : url1) + $this.crossCompanyFlag;
    url2 =
      window._PAGECONFIG && window._PAGECONFIG.layoutDataUrl
        ? window._PAGECONFIG.layoutDataUrl
        : url2; // 搜索和列表接口

    // 判断是否为字符串
    if (typeof url2 == "string") {
      $this.basicUrl = url2 + $this.crossCompanyFlag;
    } else if (typeof url2 == "object") {
      let searchType = window.localStorage.getItem("searchType");
      if (searchType) {
        $this.basicUrl = url2[searchType] + $this.crossCompanyFlag;
      } else {
        $this.basicUrl = url2.pending + $this.crossCompanyFlag;
      }
      window.localStorage.setItem("searchType", "pending");
    }
    // $this.basicUrl = url2;
    // var url1 = "../vue/static/data/" + type + "LayoutData.json"; //布局信息
    // var url2 = "../vue/static/data/" + type + "Data.json"; //表单数据
    $this.getData(
      {
        url: url1
      },
      function(data) {
        data = JSON.parse(data);
        //布局json
        try {
          var list = data.list;
          $this.searchConfigData = {
            query: data.query,
            dataclassify: data.dataclassify,
            searchUrl: url2,
            isRefer: $this.isRefer,
            crossCompanyFlag: $this.crossCompanyFlag
          }; //查询组件配置信息
          $this.onloadFun = data.pageConfig.onload; //onload函数
          $this.pageConfig = data.pageConfig;
          $this.layout = $this.sortLayout(list, data.pageConfig); //布局单元格分组
          $this.getLoadData($this.basicUrl, "loadTop"); //数据json
          $this.eventBindConfig = _commonJs.getEventBindConfig(data.list); //获取额外的事件绑定配置
          $this.$nextTick(function() {
            $this.redrawListPanel();
            $this.$refs.wrapper.addEventListener("touchstart", function(event) {
              event.target.classList.add("needsclick");
            });
            try {
              if ($this.onloadFun) new Function($this.onloadFun)();
            } catch (error) {
              console.log("自定义代码出错！");
            }
          });
        } catch (e) {
          console.log("页面布局json格式错误！");
        }
      }
    );
    // 获取当前安卓手机版本号小于等于5的进行样式处理
    this.isAndroidLessVersionFive = _commonJs.isLessEqualAndroidVersion(5);
  },
  methods: {
    //请求列表数据
    getLoadData: function(url, type, loadType, postData) {
      var $this = this;
      $this.getData(
        {
          url: url,
          type: "POST",
          data: postData ? postData : $this.postData,
          loadType: loadType
        },
        function(data) {
          let list = data;
          $this.totalPages = list.totalPages;
          $this.noList = false;
          if (list.result == 0) {
            // 无数据
            $this.noList = true;
          }
          //表单数据
          if (type == "loadBottom") {
            var hisData = $this.listData;
            $this.listData = hisData.concat(list.result);
          } else {
            $this.listData = list.result;
          }
          $this.$nextTick(function() {
            // this.myToast("成功");
            $this.$refs.wrapper.addEventListener("touchstart", function(event) {
              event.target.classList.add("needsclick");
            });
            //渲染完成后执行
            try {
              if ($this.pageConfig.ptPageInit && !$this.ptInitFlag) {
                new Function($this.pageConfig.ptPageInit)();
                $this.ptInitFlag = true;
              }
              if ($this.pageConfig.renderOver)
                new Function($this.pageConfig.renderOver)(); // 列表请求回调方法
              // 判断是否为api里的事件
              if ($this.eventBindConfig.length > 0) {
                let evt = $this.eventBindConfig;
                for (let i = 0; i < evt.length; i++) {
                  const elt = evt[i];
                  if (elt.fbody && elt.fbody.indexOf("bapApi") > -1) {
                    new Function(elt.split("bapApi.")[1])();
                  } else if (elt.fbody && elt.fbody.indexOf("function") > -1) {
                    _bapApi.bindEventConfig([elt]);
                  }
                }
              }
            } catch (error) {
              console.log("自定义代码出错！");
            }
          });
        }
      );
    },
    handleBottomChange: function(status) {
      this.bottomStatus = status;
    },

    // 重新绘制列表高度
    redrawListPanel: function() {
      // var $this = this;
      // this.wrapperHeight = 400;
      let footerH = this.$refs.footer && this.$refs.footer.clientHeight;
      footerH = footerH ? footerH : 0;
      this.wrapperHeight =
        document.documentElement.clientHeight -
        this.$refs.wrapper.getBoundingClientRect().top -
        footerH; //设置高度
      this.wrapperHeight = Math.floor(this.wrapperHeight);
    },

    //下拉刷新逻辑
    loadTop: function() {
      setTimeout(() => {
        //这里执行刷新列表逻辑
        let $this = this;
        let type = $this.listType;
        var url = $this.basicUrl;
        $this.getLoadData(url, "loadTop", "dropDown");
        $this.$refs.loadmore.onTopLoaded();
      }, 1500);
    },
    handleTopChange: function(status) {
      this.moveTranslate = 1;
      this.topStatus = status;
    },
    translateChange: function(translate) {
      const translateNum = +translate;
      this.translate = translateNum.toFixed(2);
      this.moveTranslate = (1 + translateNum / 70).toFixed(2);
    },
    //上拉加载更多
    loadBottom: function() {
      let $this = this;
      if (this.totalPages <= this.postData["page.pageNo"]) {
        setTimeout(function() {
          $this.$refs.loadmore.onBottomLoaded();
          $this.myToast("暂无更多");
        }, 500);
        return;
      } else {
        $this.postData["page.pageNo"]++;
      }
      setTimeout(() => {
        //这里执行加载更多逻辑
        let type = this.listType;
        var url = $this.basicUrl;
        $this.getLoadData(url, "loadBottom");
        $this.$refs.loadmore.onBottomLoaded();
        // this.myToast("刷新成功");
      }, 1500);
    },
    //布局单元格分组
    sortLayout: function(data, config) {
      var col = [];
      var sort = [];
      for (var i = 0; i < data.length; i++) {
        var item = data[i];
        if (item.firstTd == "1" && i > 0) {
          sort.push(col);
          col = [];
        }
        col.push(item);
        if (i == data.length - 1) sort.push(col);
      }
      var sortTab = {
        pageConfig: config.colwidth.split(","),
        sort: sort
      };
      return sortTab;
    },

    // 单击列表事件
    touchListEvent: function(res) {
      if (this.isRefer) {
        if(this.isSingleCheck == 1){
          // 当前为参照 单选选中列表
          this.checklist.push(res);
          this.singleCheck();
          this.closeRefer();
        }
        
        
      } else {
        // 显示详情
        if (window._PAGECONFIG && window._PAGECONFIG.openUrl) {
          let clientType = "";
          let hideWebTitle = "";
          if (window._PAGECONFIG && window._PAGECONFIG.clientType) {
            clientType = window._PAGECONFIG.clientType;
          }
          if (window._PAGECONFIG && window._PAGECONFIG.hideWebTitle) {
            hideWebTitle = window._PAGECONFIG.hideWebTitle;
          }
          if (res.pending && res.pending.id) {
            window.location.href = `${window._PAGECONFIG.openUrl}&id=${res.id}&pendingId=${res.pending.id}&tableInfoId=${res.tableInfoId}&clientType=${clientType}&hideWebTitle=${hideWebTitle}`;
          } else {
            window.location.href = `${window._PAGECONFIG.openUrl}&id=${res.id}&tableInfoId=${res.tableInfoId}&clientType=${clientType}&hideWebTitle=${hideWebTitle}`;
          }
        }
      }
    },

    // 选中列表
    checklistFunc: function(data) {
      if (event.target.checked) {
        this.checklist.push(data);
      }
    },
    // 取消选择 关闭参照
    closeRefer: function() {
      if (typeof window[this.closeFuncName] == "function") {
        window[this.closeFuncName]();
      }
    },

    // 单选选中
    singleCheck: function() {
      this.submitData();
    },

    // 确认选中
    submitData: function() {
      if (!this.checklist || this.checklist.length == 0) {
        this.myToast("至少选择一行数据");
        return false;
      }
      if (typeof window[this.callBackFuncName] == "function") {
        window[this.callBackFuncName](this.checklist);
      }
    },
    //根据key查找对应的数值
    getValueByKey: function(data, element) {
      return _commonJs.formatValue(data, element);
    },
    // 按显示方法显示
    getShowFormat: function(data, element, index) {
      return _commonJs.showFormat(data, element, index);
    },
    //获取样式配置信息
    getCssStyle: function(config, element) {
      return _commonJs.getCssStyle(config, element);
    },

    // 时间格式化
    formatTime: function(fmt, date) {
      return _commonJs.getDateFormat(fmt, new Date(date));
    },

    // 返回状态样式
    returnState: function() {},

    // 退出app webview
    backHistory: function() {
      if (this.isRefer) {
        this.closeRefer();
      } else {
        window.localStorage.setItem("searchType", "");
        try {
          window.mobilejs.webGoBack();
        } catch (err) {
          try {
            window.history.back();
          } catch (err) {}
        }
      }
    },
    updatePanelActive(flag) {
      this.showPanel = flag;
    }
  },
  //引用组件
  components: {
    searchBar,
    HeadBar
  }
};
</script>

<!-- 样式加载 -->

<style lang="less" scoped>
@import "../../assets/css/itemList.less";

.nolist-tip {
  padding: 40 / @rem;
  font-size: 32 / @rem;
  text-align: center;
  color: #c7c7c7;
}

.info_box .state.todo {
    &.todo-reset-color {
        background: #dfecf2;
    }
}
.bill_title .info_box.over_width.fr.todo {
    &.todo-reset-color {
        &::after {
            background: #dfecf2;
        }
    }
}
</style>
<style lang="css">
label.titName.needsclick div {
  display: inline-block;
}
</style>



// WEBPACK FOOTER //
// src/pages/itemList/itemList.vue