<template>
  <div
    class="m_headbtn"
    :class="{'is_bottom_btn':isBottomBtn}"
    v-if="(viewType=='LIST' && listButtons.length > 0) || ((viewType=='EDIT' || viewType=='VIEW') && (!urlParams.id && referenceCopy) || (editButtonsList && editButtonsList.length > 0) || (urlParams.id && isAllowProxy) || hasAttachment)"
  >
    <!--列表视图的按钮-->
    <div
      class="m_headbtn_list"
      v-if="viewType=='LIST'"
    >
      <template v-if="isCustomBtn && customBtnList.length > 0">
        <!--多个按钮-->
        <a
          class="m_headmore"
          href="javascript:;"
          @click="openBtnDialog"
        ></a>
      </template>
      <!--工作流按钮或者基础单据的新增按钮-->
      <template v-if="!isCustomBtn && isBottomBtn && newProcessesList.length > 0">
        <!--单个按钮-->
        <a
          v-if="newProcessesList.length == 1"
          class="workflow_btn"
          href="javascript:;"
          @click="processBtnEve(0)"
        ></a>
        <!--多个按钮-->
        <a
          v-else
          class="workflow_btn"
          href="javascript:;"
          @click="openBtnDialog"
        ></a>
        <mt-popup
          v-model="popupVisible"
          position="bottom"
          modal=false
          closeOnClickModal=false
          pop-transition="popup-fade"
          @touchmove.prevent
        >
          <div class="mint-popup-1">
            <ul class="btn_more workflow_btn_more">
              <li
                v-if="list"
                v-for="(list, index) in newProcessesList"
                :key="list.CODE"
                @click="processBtnEve(index)"
              >
                {{list.namekey}}
              </li>
            </ul>
          </div>
        </mt-popup>
      </template>
    </div>

    <!--编辑视图的按钮-->
    <div v-if="viewType=='EDIT' || viewType=='VIEW'">
      <template v-if="(!urlParams.id && referenceCopy) || (editButtonsList && editButtonsList.length > 0) || (urlParams.id && isAllowProxy) || hasAttachment">
        <a
          class="m_headmore"
          href="javascript:;"
          @click="openBtnDialog"
        ></a>

        <!--按钮列表-->
        <mt-popup
          v-model="popupVisible"
          position="top"
          modal=false
          closeOnClickModal=false
          pop-transition="popup-fade"
          ref="mintPop"
          @touchmove.prevent
        >
          <div
            ref="mintPopup"
            class="mint-popup-1"
          >
            <ul class="btn_more">
              <!--参照复制-->
              <li
                v-if="!urlParams.id && referenceCopy"
                @click="openReferCopy(referenceCopy, $event)"
              >
                <i class="btn-icon copy-icon"></i>
                <span>{{$t('button.referCopy.text')}}</span>
              </li>

              <!--自定义按钮-->
              <li
                v-for="editBtnList in editButtonsList"
                v-if="editBtnList.isShow"
                @click="editBtnList.func"
              >
                <i
                  class="btn-icon"
                  :class="editBtnList.className"
                ></i>
                <span>{{editBtnList.btnName}}</span>
              </li>

              <!--委托-->
              <li
                v-if="urlParams.id && isAllowProxy"
                @click="openEntrustInterface()"
              >
                <i class="btn-icon entrust_icon"></i>
                <span>{{$t('button.entrust.text')}}</span>
              </li>

              <!--附件-->
              <li
                v-if="hasAttachment"
                @click="openAppendixInterface()"
              >
                <i class="btn-icon appendix-icon"></i>
                <span>{{$t('button.attach.text')}}</span>
                <span class="appendix-num">{{AppendixNum}}</span>
              </li>
            </ul>
          </div>
        </mt-popup>

        <!--附件下载页面-->
        <Appendix ref="appendix"></Appendix>

        <!--委托页面-->
        <Entrust ref="entrust"></Entrust>
      </template>
    </div>
    <div v-if="isCustomBtn && customBtnList.length > 0">
      <mt-popup
        v-model="popupVisible"
        :position="position"
        modal=false
        closeOnClickModal=false
        pop-transition="popup-fade"
        @touchmove.prevent
      >
        <div class="mint-popup-1">
          <ul class="btn_more">
            <!--配置按钮-->
            <li
              v-if="btnList"
              v-for="(btnList, btnIndex) in customBtnList"
              :key="`${btnList.id}_${btnIndex}`"
              v-on:click="bindClick(btnList.funcbody, btnList)"
            >
              <i
                class="btn-icon"
                :class="'cui-btn-' + btnList.buttonstyle"
              ></i>{{btnList.namekey}}
            </li>
          </ul>
        </div>
      </mt-popup>
    </div>
  </div>
</template>
<script>
import { Popup } from 'mint-ui';
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import Appendix from "@/components/HeadBar/Appendix"; //附件
import Entrust from "@/components/workFlow/Entrust"; //委托
import { openLinkUrl } from '../../assets/js/util/common.js'

export default {
  name: 'HeadBtn',
  data() {
    return {
      viewType: "", //视图类型
      popupVisible: false,
      url: [],
      referenceCopy: "", //参照复制
      onClickEvent: [],
      urlParams: "",
      editButtonsList: [],
      AppendixNum: 0,
      workflowBtnList: [], //工作流按钮
      customBtnList: [], //自定义按钮
      position: "top",
      hasAttachment: false, //是否启用附件功能
      isWorkflow: "", //是否启用工作流
      newProcessesList: [] // 底部新增按钮的显示
    }
  },
  props: {
    listButtons: {
      type: Array,
      default: () => []
    },
    isAllowProxy: {
      type: Boolean,
      default: false
    },
    isCustomBtn: {
      type: Boolean,
      default: false
    },
    isWorkflowBtn: {
      type: Boolean,
      default: false
    },
    isBottomBtn: {
      type: Boolean,
      default: false
    }
  },
  mounted() {
    var $this = this;
    let clientType = '';
    let hideWebTitle = '';
    let newProcessesList = [];
    let { listButtons } = this;

    // 注册全局方法
    $this.exportFunc(`operateBarOnclickFoundation`, $this.operateBarOnclickFoundation);
    $this.exportFunc(`addBtnList`, $this.addBtnList); // 添加按钮列表

    // 获取地址参数
    $this.urlParams = $this.GetRequest(window.location.href);

    if (window.pageConfig && window.pageConfig.viewType) { //页面名称
      $this.viewType = window.pageConfig.viewType;
    }

    if (window.pageConfig && window.pageConfig.workflowEnabled) { //是否启用工作流
      $this.isWorkflow = window.pageConfig.workflowEnabled;
    }

    if (window.pageConfig && window.pageConfig.hasAttachment) { //附件
      $this.hasAttachment = window.pageConfig.hasAttachment;
    }

    if ($this.urlParams && $this.urlParams.clientType) {
      clientType = $this.urlParams.clientType;
    }
    if ($this.urlParams && $this.urlParams.hideWebTitle) {
      hideWebTitle = $this.urlParams.hideWebTitle;
    }

    //工作流按钮和自定义按钮的分配

    if (listButtons.length > 0) {
      for (var i = 0; i < listButtons.length; i++) {
        if (listButtons[i]) {
          const { operatetype } = listButtons[i];
          if (operatetype) { //自定义按钮
            if (operatetype === "ADD") {
              newProcessesList.push(listButtons[i]);
            } else {
              $this.customBtnList.push(listButtons[i]);
            }
          } else { //工作流按钮
            const clickUrl = listButtons[i].onclick;
            const processUrl = `${clickUrl}&clientType=${clientType}&hideWebTitle=${hideWebTitle}`
            newProcessesList.push({ ...listButtons[i], url: processUrl });
          }
        }
      }
      this.newProcessesList = newProcessesList;
    }

    if (window.pageConfig && window.pageConfig.interfaceApi) { //参照复制功能
      $this.referenceCopy = window.pageConfig.interfaceApi.referenceCopy;
    }

    //自定义按钮的弹窗显示位置
    if (this.isCustomBtn && this.isBottomBtn) {
      this.position = "right";
    } else {
      this.position = "top";
    }
  },

  methods: {
    //工作流按钮
    operateBarOnclickFoundation: function (addUrl) {
      if (addUrl) {
        // window.location.href = addUrl;
        openLinkUrl(addUrl);
      }
    },

    //打开按钮弹窗
    openBtnDialog: function () {
      this.popupVisible = true;
      if ((this.viewType == 'EDIT' || this.viewType == 'VIEW') && this.urlParams.id) {
        this.getAppendixNum(); //获取附件个数
      }
    },

    //打开参照复制
    openReferCopy: function (referenceCopy, event) {
      event.stopPropagation();
      const $this = this;
      $this.popupVisible = false; //关闭弹窗

      var referenceview = {
        code: new Date().getTime().toString(),
        iscrosscompany: false
      };
      var data = {
        url: referenceCopy,
        multable: 0,
        referenceview: referenceview
      };

      var callbackFunc = function (list) {
        let prefix = window.pageConfig && window.pageConfig.prefix; //url前缀
        if (window.pageConfig && window.pageConfig.interfaceApi) { //获取表单数据地址
          let layoutDataUrl;
          let urlParams = $this.GetRequest(window.location.href); // 获取地址参数
          if (window.pageConfig) {
            layoutDataUrl = window.pageConfig.interfaceApi.sourceData;
          }
          if (list.id) {
            layoutDataUrl = layoutDataUrl + `/${list.id}`; // + `?pendingId=${list.pendingId}`
          }
          vue.refreshFormData(layoutDataUrl, true, list.id); //刷新form数据
        }
      };
      vue.openRefSelecFunc(data, callbackFunc);
    },

    /**
     * 添加编辑视图页面的列表自定义按钮
     * @param {function} func 自定义点击事件
     * @param {string} className class名称
     * @param {string} btnName 自定义按钮的名称
     */
    addBtnList: function (func, className, btnName) {
      const $this = this;
      var btnObj = {};
      if (func && typeof func == "function") {
        btnObj.func = func;
      }
      btnObj.className = className ? className : "";
      btnObj.btnName = btnName ? btnName : "";
      btnObj.isShow = true;
      $this.editButtonsList.push(btnObj);
    },

    //打开附件下载页面
    openAppendixInterface: function () {
      const $this = this;
      _commonJs.openAppendixListInterface($this);
    },

    //获取附件个数
    getAppendixNum: function () {
      const $this = this;
      _commonJs.getAppendixNums($this);
    },

    //打开委托界面
    openEntrustInterface: function () {
      this.popupVisible = false; //关闭弹窗
      this.$refs.entrust.isShowEntrust = true;
      this.$forceUpdate();
    },

    bindClick: function (evt, btnList) {
      let $this = this;
      if (evt) {
        try {
          eval("(" + evt + ")")(event);
        } catch (error) {
          console.error(error);
        }
      }
      if (btnList.viewselect) {
        $this.$nextTick(function () {
          let viewCode = window.pageConfig && window.pageConfig.viewCode;
          let entityCode = window.pageConfig && window.pageConfig.entityCode;
          let clientType = "";
          let hideWebTitle = "";
          let urlParams = vue.GetRequest(window.location.href); // 获取地址参数
          if (urlParams && urlParams.clientType) {
            clientType = urlParams.clientType;
          }
          if (urlParams && urlParams.hideWebTitle) {
            hideWebTitle = urlParams.hideWebTitle;
          }

          let url;
          if ($this.isWorkflow) {
            url = btnList.viewselect.url +
              `&${btnList.pc}&viewCode=${viewCode}&entityCode=${entityCode}&iscrosscompany=${btnList.viewselect.iscrosscompany}&openType=${btnList.viewselect.openType}&viewType=${btnList.viewselect.name}&buttonCode=${btnList.buttonoperationcode}` +
              `&clientType=${clientType}&hideWebTitle=${hideWebTitle}`;
          } else {
            url = btnList.viewselect.url +
              `?${btnList.pc}&viewCode=${viewCode}&entityCode=${entityCode}&iscrosscompany=${btnList.viewselect.iscrosscompany}&openType=${btnList.viewselect.openType}&viewType=${btnList.viewselect.name}&buttonCode=${btnList.buttonoperationcode}` +
              `&clientType=${clientType}&hideWebTitle=${hideWebTitle}`;
          }
          // window.location.href = url;
          openLinkUrl(url);

        });
      }
    },
    //流程按钮和新建按钮
    processBtnEve: function (index) {
      const { isWorkflow, newProcessesList } = this;
      const { url, onclick, funcbody, viewselect } = newProcessesList[index];
      if (viewselect) { //基础单据
        this.bindClick(funcbody, newProcessesList[index]);
      } else { //流程单据
        this.operateBarOnclickFoundation(url);
      }
    }
  },

  components: {
    Appendix,
    Entrust
  }
}
</script>
<style lang="less" scoped>
@import "./headBtn.less";
</style>
<style lang="less">
.v-modal {
  opacity: 0.3;
  z-index: 1000 !important;
}
</style>



// WEBPACK FOOTER //
// src/components/HeadBar/HeadBtn.vue