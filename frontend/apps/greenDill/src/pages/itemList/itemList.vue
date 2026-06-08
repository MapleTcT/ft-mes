<template>
  <div id="app">
    <div class="g_container">
      <HeadBar
        v-if="isLoad"
        ref="headbar"
        :listButtons="listButtons"
        v-on:pageBackFunc="backHistory"
        :headTitle="headTitle"
      ></HeadBar>

      <div class="m_center">
        <div class="m_centerBox">
          <!--快速查询和高级查询-->
          <searchBar
            :listButtons="listButtons"
            :propIsWorkflow="isWorkflow"
            :propSearchConfigData="searchConfigData"
            :updatePanelActive="updatePanelActive"
            ref="searchBar"
          >
          </searchBar>
          <div class="m_box m_goodsBoxWrap">
            <div class="page-loadmore">
              <div
                class="page-loadmore-wrapper needsclick"
                :class="showPanel?'':'active'"
                ref="wrapper"
                :style="{ height: wrapperHeight + 'px' }"
              >
                <p
                  class="nolist-tip"
                  v-if="noList"
                >{{$t('itemList.noData')}}</p>
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
                    class="m_goodsBox"
                    :class="lindex === listData.length -1? '':'list-btm-border'"
                    v-for="(listD,lindex) in listData"
                  >
                    <div
                      v-if="isListMulti || (isRefer && !isSingleCheck)"
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
                    <div
                      v-on:click="touchListEvent(listD)"
                      class="needsclick"
                      :class="isListMulti || (isRefer && !isSingleCheck)?'hasCheck':''"
                    >
                      <!-- 选择框 -->

                      <!-- 表头 -->
                      <div
                        class="info_box bill_title"
                        v-if="isWorkflow"
                      >
                        <div class="info_box over_width">
                          <!-- <span class="number">NFFD-20190212-101</span> -->
                          <span
                            v-if="listD.tableNo"
                            class="number"
                          >{{listD.tableNo}}</span>
                        </div>
                        <template v-if="listD.pending && listD.pending.taskDescription">
                          <div
                            class="info_box over_width fr"
                            :class="getStatusClassName(listD)"
                          ><span
                              class="state"
                              :class="getStatusClassName(listD)"
                            >{{listD.pending.taskDescription}}</span></div>
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
                              <tr
                                v-for="(trItem,trIndex) in layout.sort"
                                class="table_tr"
                              >
                                <td
                                  v-for="(tdItem,tdIndex) in trItem"
                                  :colspan='tdItem.colspan'
                                  :rowspan='tdItem.rowspan'
                                  :data-key="tdItem.element && tdItem.element.key"
                                >
                                  <div
                                    class="subinfo needsclick"
                                    :class="phoneType == 'android'?'subinfo-android-font':'subinfo-ios-font'"
                                    :style="getCssStyle(tdItem.cssstyle,tdItem.element)"
                                    @click="openRelationalView(listD, tdItem.element, $event)"
                                  >
                                    <label
                                      v-html="getShowFormat(listD,tdItem.element, tdIndex)"
                                      v-if="tdItem.element && tdItem.element.showType && tdItem.element.showType.indexOf('LABEL')>-1"
                                      class="titName needsclick"
                                      :name="tdItem.cellCode"
                                    >
                                      {{tdItem.element.namekey}}
                                    </label>
                                    <label
                                      v-html="getShowFormat(listD,tdItem.element, tdIndex)"
                                      v-else-if="tdItem.element"
                                      class="attrVal needsclick"
                                      :name="tdItem.cellCode"
                                    >
                                      {{getValueByKey(listD,tdItem.element)}}
                                    </label>
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
                        v-if="isWorkflow"
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
                    <span v-show="allLoaded == true">{{$t('message.loadmore.nodata')}}</span>
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
      >{{$t('button.cancel.text')}}</a>
      <a
        class="m_botton btn_submit"
        v-on:click="submitData"
      >{{$t('button.confirm.text')}}</a>
    </div>
    <div class="edit_modal"></div>
  </div>
</template>

<script>
import searchBar from "@/components/listSearch/searchBar"; //搜索栏
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import _bapApi from "@/assets/js/itemList/bapApi.js"; //引用模块进来
import HeadBar from "@/components/HeadBar/HeadBar";
import axios from 'axios';
import { publicGetAxios, getCustomCondition, openLinkUrl } from '@/assets/js/util/common.js';
import { merge } from 'lodash';
import { getSystemCodeJson } from '../../components/cellComp/systemCode/util.js';
import API from './api.js';
import interfaceURL from '../../assets/js/util/interface.js';
import { tryCatch } from '../../components/cellComp/reference/util';

window._bapApi = _bapApi;
export default {
  name: "itemList",
  data() {
    return {
      isWorkflow: "", //是否启用工作流
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
      searchConfigData: {},
      eventBindConfig: [],
      basicUrl: "",
      isRefer: false, // 是否为参照页面
      crossCompanyFlag: "",
      postData: {
        "classifyCodes": "",
        "customCondition": {},
        "pageNo": 1,
        "pageSize": 20,
        "paging": true
      },
      showPanel: false,
      noList: false,
      totalPages: "",
      ptInitFlag: false, // 列表首次加载
      checklist: [], // 选中的列表
      isAndroidLessVersionFive: false, // 安卓手机系统是否小于等于5.0
      phoneType: "", //判断当前设备的类型
      totalCount: "", //单据总个数
      totalPendingCount: "", //待办单据总个数
      totalResult: [], //总共的列表数据信息
      totalPendingResult: [], //总共的待办列表数据信息
      urlParams: "",
      isLoad: false, //视图头部是否加载
      viewType: "", //视图类型
      listButtons: [], //配置的按钮
      isListMulti: false
    };
  },
  props: ["params"],
  mixins: [API],
  beforeMount() {
    let $this = this;
    if ($this.params) {
      $this.listType = $this.params.type;
      $this.formName = $this.params.name;
    } else if (window.pageConfig) {
      const config = window.pageConfig;
      $this.isWorkflow = config.workflowEnabled;
      $this.formName = config.viewTitle;
      $this.viewType = config.viewType ? config.viewType : "";

      if ($this.viewType === "REFERENCE") { //判断为参照页面
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

    $this.getLayoutJson(); //获取列表布局数据

    $this.urlParams = $this.GetRequest(window.location.href); // 获取地址参数

    // 获取当前安卓手机版本号小于等于5的进行样式处理
    this.isAndroidLessVersionFive = _commonJs.isLessEqualAndroidVersion(5);

    this.phoneType = _commonJs.judgePhoneType(); //判断设备类型，android或者ios

  },

  methods: {
    /**
     * 获取列表布局数据
     */
    getLayoutJson: function () {
      let $this = this;
      let url1; //列表布局数据接口
      let url2; //列表数据接口

      // 流程列表TODO
      if ($this.isWorkflow) {
      }

      // 读取html 里的配置
      if (window.pageConfig) {
        let viewCode = window.pageConfig.viewCode;
        let menuCode = window.pageConfig.menuCode;
        let entityCode = window.pageConfig.entityCode;
        url1 = window.pageConfig.baseServiceApi.layoutJson + `?viewCode=${viewCode}&menuCode=${menuCode}&entityCode=${entityCode}`;
        // url1 = "static/data/layoutJson.json";
        url2 = window.pageConfig.interfaceApi.sourceData;
        let searchType = window.localStorage.getItem("searchType");
        const defaultSearchType = $this.isRefer ? 'query' : 'pending';
        searchType = searchType && !$this.isRefer ? searchType : defaultSearchType;
        searchType = searchType && $this.isWorkflow ? searchType : 'query';
        $this.basicUrl = url2 + `${searchType}`;
        window.localStorage.setItem("searchType", searchType);
        // $this.basicUrl = "static/data/list.json";
      }

      publicGetAxios({
        url: url1,
        type: "get"
      }, (result) => {
        let data = result.data; //列表布局json
        try {
          var list = data.list;
          $this.searchConfigData = {
            query: data.query,
            dataclassify: data.dataclassify,
            dataGroupProperty: data.dataGroupProperty,
            searchUrl: url2,
            // searchUrl: $this.basicUrl,
            isRefer: $this.isRefer,
            crossCompanyFlag: $this.crossCompanyFlag
          }; //查询组件配置信息
          $this.headTitle = data.title || window.pageConfig.viewTitle;
          $this.listButtons = data.buttons;//按钮列表
          $this.isLoad = true;
          $this.onloadFun = data.pageConfig.onload; //onload函数
          $this.pageConfig = data.pageConfig;
          $this.layout = $this.sortLayout(list, data.pageConfig); //布局单元格分组
          // 获取系统编码值
          // $this.getSystemCodeSlots();
          $this.eventBindConfig = _commonJs.getEventBindConfig(data.list); //获取额外的事件绑定配置
          $this.$nextTick(() => {
            $this.redrawListPanel();
            $this.$refs.wrapper.addEventListener("touchstart", function (event) {
              event.target.classList.add("needsclick");
            });
            // 默认查数据
            try {
              const queryData = this.$refs.searchBar.$refs.sidePanel.combineQueryData();
              this.postData = { ...queryData.data, pageNo: 1, pageSize: 20, paging: true };
              this.getLoadData(this.basicUrl, "loadTop");
            } catch (error) {
            }
            // 执行onload方法
            tryCatch(new Function($this.onloadFun));
          });
        } catch (e) {
          console.log("页面布局json格式错误！");
        }
      });
    },

    /**
     * 请求列表数据
     * @params {string} url 获取列表数据的接口
     * @params {string} type 1.当type="loadTop"时，下拉加载，2.当type="loadBottom"时，上拉加载
     * @params {string} loadType
     * @params {object} postData 传输的参数
     */
    getLoadData: function (url, type, loadType, postData) {
      var $this = this;
      const curUrl = url.indexOf('?') > -1 ? url : `${url}?`;
      const refCondition = getCustomCondition();
      publicGetAxios({
        // url: `${url}`,
        url: `${curUrl}${$this.crossCompanyFlag} `,
        type: "POST",
        dataType: 'json',
        headers: {
          "Content-Type": "application/json;charset=UTF-8"
        },
        data: JSON.stringify(postData ? { ...postData, ...refCondition } : { ...$this.postData, ...refCondition }),
        loadType: loadType
      }, function (result) {
        let list = result.data;
        $this.totalPages = list.totalPages;
        $this.noList = false;
        if (list.result == 0) { // 无数据
          $this.noList = true;
        }
        //表单数据
        if (type == "loadBottom") {
          var hisData = $this.listData;
          $this.listData = hisData.concat(list.result);
        } else {
          $this.listData = list.result;
        }
        $this.$nextTick(function () {
          $this.$refs.wrapper.addEventListener("touchstart", function (event) {
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
      });
    },

    handleBottomChange: function (status) {
      this.bottomStatus = status;
    },

    // 重新绘制列表高度
    redrawListPanel: function () {
      let footerH = this.$refs.footer && this.$refs.footer.clientHeight;
      const { searchBar = {} } = this.$refs;
      let { footerBar = {} } = searchBar.$refs;
      footerBar = footerBar.$el && footerBar.$el.clientHeight || 0;
      footerH = footerH ? footerH : 0;
      this.wrapperHeight =
        document.documentElement.clientHeight -
        this.$refs.wrapper.getBoundingClientRect().top -
        footerH - footerBar; //设置高度
      this.wrapperHeight = Math.floor(this.wrapperHeight);
    },

    //下拉刷新逻辑
    loadTop: function () {
      setTimeout(() => {
        //这里执行刷新列表逻辑
        let $this = this;
        // let type = $this.listType;
        var url = $this.basicUrl;
        $this.getLoadData(url, "loadTop", "dropDown", { ...this.postData, "pageNo": 1, });
        $this.$refs.loadmore.onTopLoaded();
      }, 1500);
    },

    handleTopChange: function (status) {
      this.moveTranslate = 1;
      this.topStatus = status;
    },

    translateChange: function (translate) {
      const translateNum = +translate;
      this.translate = translateNum.toFixed(2);
      this.moveTranslate = (1 + translateNum / 70).toFixed(2);
    },

    //上拉加载更多
    loadBottom: function () {
      let $this = this;
      if (this.totalPages <= this.postData["pageNo"]) {
        setTimeout(function () {
          $this.$refs.loadmore.onBottomLoaded();
          $this.myToast($this.$t('notice.noMore.data'));
        }, 500);
        return;
      } else {
        $this.postData["pageNo"]++;
      }
      setTimeout(() => {
        //这里执行加载更多逻辑
        // let type = this.listType;
        var url = $this.basicUrl;
        $this.getLoadData(url, "loadBottom");
        $this.$refs.loadmore.onBottomLoaded();
        // this.myToast("刷新成功");
      }, 1500);
    },

    //布局单元格分组
    sortLayout: function (data = [], config) {
      var col = [];
      var sort = [];
      for (var i = 0; i < data.length; i++) {
        var item = data[i];
        const { element = {} } = item;
        const { showType, columnType, isCustom } = element;
        item.showType = showType === 'LABEL' ? 'label' : 'text';
        // 获取系统编码值
        if (showType !== 'label' && columnType === 'SYSTEMCODE' && isCustom) {
          this.getSystemCodeSlots(element);
        }
        if (item.firstTd == "1" && i > 0) {
          sort.push(col);
          col = [];
        }
        col.push(item);
        if (i == data.length - 1) sort.push(col);
      }
      var sortTab = {
        pageConfig: config.colwidth.toString().split(","),
        sort: sort
      };
      return sortTab;
    },

    //获取普通单据的数据信息
    handleBasicRecordDbClick: function name(record, paramStr) {
      const { interfaceApi, listButtons, entityCode } = window.pageConfig;
      let checkUserPower = interfaceURL.checkUserPower;
      const { cid } = record;
      const $this = this;
      const modBtnCode = (listButtons && listButtons['MODIFY']) || '';
      if (checkUserPower) {
        let checkPerParam = `entityCode=${entityCode}`;
        checkPerParam += modBtnCode ? `&menuOperateCode=${modBtnCode}` : '';
        checkPerParam += cid ? `&cid=${cid}` : '';
        const checkPerUrl = `${checkUserPower}?${checkPerParam}`;
        publicGetAxios({
          url: checkPerUrl,
          type: "get"
        }, function (result) {
          if (result.code === 200) {
            let data = result.data;
            const {
              __pc__,
              openType,
              iscrosscompany,
              isEditView,
              buttonCode,
              url
            } = data;
            const tempData = { ...data, isEditView: !!modBtnCode };
            let editView = window.pageConfig && window.pageConfig.interfaceApi.editView;
            const viewType = tempData.isEditView || isEditView ? 'edit' : 'view';
            paramStr += __pc__ ? `&__pc__=${__pc__}` : '';
            paramStr += viewType ? `&viewType=${viewType}` : '';
            paramStr += openType ? `&openType=${openType}` : '';
            paramStr += iscrosscompany ? `&iscrosscompany=${iscrosscompany}` : '';
            paramStr += buttonCode ? `&buttonCode=${buttonCode}` : '';
            if (url) {
              openLinkUrl(`${url}?${paramStr}`)
            } else if (editView) {
              openLinkUrl(`${editView}${paramStr}`)
            }
          } else {
            console.log("出错！");
          }
        });
      }
    },

    // 单击列表事件todo
    touchListEvent: function (res) {
      event.stopPropagation();
      // API事件
      const { APIclickEvt } = this;
      if (APIclickEvt && typeof APIclickEvt === "function") {
        APIclickEvt(res);
        return;
      }
      if (this.isRefer) {
        if (this.isSingleCheck == 1) { // 当前为参照 单选选中列表
          this.checklist.push(res);
          this.singleCheck();
          // this.closeRefer();
        }
      } else { // 显示详情
        let openUrl = interfaceURL.openPending;
        // let openUrl = "/edit.html";
        let clientType = "";
        let hideWebTitle = "";
        let pc = "";
        let workflowEnabled = window.pageConfig && window.pageConfig.workflowEnabled;
        let entityCode = window.pageConfig && window.pageConfig.entityCode;
        if (this.urlParams && this.urlParams.clientType) {
          clientType = this.urlParams.clientType;
        }
        if (this.urlParams && this.urlParams.hideWebTitle) {
          hideWebTitle = this.urlParams.hideWebTitle;
        }
        if (this.urlParams && this.urlParams.__pc__) {
          pc = this.urlParams.__pc__;
        }
        if (workflowEnabled) { //启用工作流
          let workflowUrl = `${openUrl}?__pc__=${pc}&entityCode=${entityCode}&id=${res.id}&tableInfoId=${res.tableInfoId}&clientType=${clientType}&hideWebTitle=${hideWebTitle}`;
          if (res.pending && res.pending.id) {
            workflowUrl = `${openUrl}?__pc__=${pc}&entityCode=${entityCode}&id=${res.id}&pendingId=${res.pending.id}&tableInfoId=${res.tableInfoId}&clientType=${clientType}&hideWebTitle=${hideWebTitle}`;
          }
          // window.open(workflowUrl, '_self');
          openLinkUrl(workflowUrl);
        } else { //基础单据
          const { viewCode, entityCode } = window.pageConfig;
          const { id: recordId, tableInfoId, pending } = res || {};
          const { id: pendingId, deploymentId } = pending || {};
          let paramStr = `id=${recordId}`;
          paramStr += pc ? `&__pc__=${pc}` : '';
          paramStr += viewCode ? `&viewCode=${viewCode}` : '';
          paramStr += entityCode ? `&entityCode=${entityCode}` : '';
          paramStr += tableInfoId ? `&tableInfoId=${tableInfoId}` : '';
          paramStr += pendingId ? `&pendingId=${pendingId}` : '';
          paramStr += deploymentId ? `&deploymentId=${deploymentId}` : '';
          paramStr += clientType ? `&clientType=${clientType}` : '';
          paramStr += hideWebTitle ? `&hideWebTitle=${hideWebTitle}` : '';
          this.handleBasicRecordDbClick(res, paramStr);
        }
      }
    },

    // 获取系统编码多选值
    getSystemCodeSlots: function (element) {
      if (element && element.multable) {
        console.log(1, element)
        getSystemCodeJson(element, element.multable);
      }
    },

    getStatusClassName: function (element = {}) {
      const { status } = element;
      switch (status) {
        case 88:
          return 'todo';
          break;
        case 99:
          return 'effective';
          break;
        case 77:
          return 'hang';
          break;
        case 0:
          return 'undo';
          break;

        default:
          return 'todo';
          break;
      }
    },

    /**
     * 打开链接的关联视图todo
     */
    openRelationalView: function (listD, element = {}, event) {
      let id;
      let clientType = "";
      let hideWebTitle = "";
      if (!element.linkView) return;
      if (listD && element) {
        let { name } = element;
        name = name.split(".")[0];
        if (listD.hasOwnProperty(name)) {
          id = listD[name].id;
        }
      }
      if (element.linkView) {
        if (this.urlParams && this.urlParams.clientType) {
          clientType = this.urlParams.clientType;
        }
        if (this.urlParams && this.urlParams.hideWebTitle) {
          hideWebTitle = this.urlParams.hideWebTitle;
        }
        // window.location.href = `${element.linkView.url}?id=${id}&clientType=${clientType}&hideWebTitle=${hideWebTitle}`;
        openLinkUrl(`${element.linkView.url}?id=${id}&clientType=${clientType}&hideWebTitle=${hideWebTitle}`)
      }
      event.stopPropagation();
    },

    // 选中列表
    checklistFunc: function (data) {
      event.stopPropagation();
      if (event.target.checked) {
        this.checklist.push(data);
      } else {
        this.checklist = this.checklist.filter((t) => t.id !== data.id)
      }
    },

    // 取消选择 关闭参照
    closeRefer: function () {
      if (typeof window[this.closeFuncName] == "function") {
        window[this.closeFuncName]();
      }
    },

    // 单选选中
    singleCheck: function () {
      const { layout = {} } = this;
      const { sort = [] } = layout;
      const relatedKeyMap = {};
      const attr_data = {};
      let list = this.checklist[0];
      sort.forEach((item = []) => {
        item.forEach((el = {}) => {
          const { element = {} } = el;
          const { isCustom, showType, relatedKey, key, originKey } = element;
          if (showType !== "LABEL" && isCustom && relatedKey) {
            relatedKeyMap[relatedKey] = originKey || key;
            const curKey = relatedKeyMap[relatedKey];
            const keyName = curKey.split('.');
            if (keyName.length > 1) {
              if (list[keyName[0]]) list[keyName[0]] = merge(list[keyName[0]], list[keyName[0]].attrMap);
            } else if (list.attrMap) {
              attr_data[curKey] = list.attrMap[curKey];
            }

          }
        });
      });
      list.related_key_map = relatedKeyMap;
      this.checklist[0] = { ...list, ...attr_data };
      this.submitData();
    },

    // 确认选中
    submitData: function () {
      if (!this.checklist || this.checklist.length == 0) {
        this.myToast(this.$t('notice.selectOne.atLeast'));
        return false;
      }
      if (typeof window[this.callBackFuncName] == "function") {
        window[this.callBackFuncName](this.checklist);
      }
    },

    //根据key查找对应的数值
    getValueByKey: function (data, element = {}) {
      const { isCustom, multable, slots, columnType } = element
      let curVal = _commonJs.formatValue(data, element);
      // 系统编码
      if (isCustom && multable && slots && columnType === 'SYSTEMCODE') {
        let tmpVal = [];
        slots.forEach((item) => {
          if (item.value.indexOf(curVal) > -1) {
            tmpVal.push(item.label);
          }
        });
        curVal = tmpVal.join(',');
      }
      return curVal;

    },

    // 按显示方法显示
    getShowFormat: function (data, element, index) {
      return _commonJs.showFormat(data, element, index);
    },

    //获取样式配置信息
    getCssStyle: function (config, element) {
      return _commonJs.getCssStyle(config, element);
    },

    // 时间格式化
    formatTime: function (fmt, date) {
      return _commonJs.getDateFormat(fmt, new Date(date));
    },

    // 返回状态样式
    returnState: function () { },

    // 退出app webview
    backHistory: function () {
      if (this.isRefer) {
        this.closeRefer();
      } else {
        window.localStorage.setItem("searchType", "");
        try {
          // window.mobilejs.webGoBack();
          window.mobilejs.close();
        } catch (err) {
          try {
            window.history.back();
          } catch (err) { }
        }
      }
    },

    updatePanelActive(flag) {
      const phoneType = _commonJs.judgePhoneType();
      if (phoneType === 'ios') {
        this.showPanel = flag;
      }
    },
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
@import "./itemList.less";

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