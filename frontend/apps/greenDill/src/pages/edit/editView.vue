<template>
  <div class="g_container">
    <div
      v-if="showRefer"
      class="refer"
    >
      <htmlPanel
        v-if="showRefer"
        :params="{url: referConfig.url}"
      ></htmlPanel>
    </div>
    <!--附件预览-->
    <div
      v-if="showAppendix"
      class="appendix"
    >
      <htmlPanel
        v-if="showAppendix"
        :params="{url: referConfig.url, isAppendix: true,id:appendixId, name:fileName}"
      ></htmlPanel>
    </div>
    <HeadBar
      ref="headbar"
      v-on:pageBackFunc="backHistory"
      :isAllowProxy="isAllowProxy"
      :headTitle="headTitle"
    ></HeadBar>
    <!-- <mt-button v-on:click="btnParam.func" class="m-btn" v-show="btnParam.isShow" size="small" type="default">{{btnParam.tit}}</mt-button> -->
    <div
      v-show="btnParam.isShow"
      :class="btnParam.className"
      v-html="btnParam.html"
      v-on:click="btnParam.func"
    ></div>
    <div class="m_center">
      <div class="m_centerBox">
        <baseLayout
          :params="formParams"
          ref="editForm"
          @getLayoutData='getLayoutData'
        >
        </baseLayout>
        <workFlow
          v-if="(pendingId || deploymentId) && isWorkflow"
          :params="routeParams"
          ref="workFlow"
        ></workFlow>

        <!-- 历史流程 -->
        <div
          v-if="isWorkflow"
          class="mt20"
        >
          <detailedData
            v-if="isLoadData"
            :currentData="currentData"
          ></detailedData>
        </div>

        <!-- 提交按钮 -->
        <template v-if="viewType !== 'VIEW' && (!isWorkflow || superEdit === 'true')">
          <div class="m_foot">
            <a
              v-on:click="handelBtnClick('cancel')"
              class="m_botton btn_save"
            >{{$t('button.cancel.text')}}</a>
            <a
              v-on:click="handelBtnClick('submit', true)"
              class="m_botton btn_submit"
            >{{$t('button.save.text')}}</a>
          </div>
        </template>
        <!-- <div
          class="m_foot"
          v-if="viewType != 'VIEW' && !isWorkflow && superEdit != 'true'"
        >
          <a
            v-on:click="handelBtnClick('cancel')"
            class="m_botton btn_save"
          >取消</a>
          <a
            v-on:click="handelBtnClick('submit', true)"
            class="m_botton btn_submit"
          >保存</a>
        </div>
        <div
          class="m_foot"
          v-if="viewType != 'VIEW' && superEdit == 'true'"
        >
          <a
            v-on:click="handelBtnClick('cancel')"
            class="m_botton btn_save"
          >取消</a>
          <a
            v-on:click="handelBtnClick('submit', true)"
            class="m_botton btn_submit"
          >保存</a>
        </div> -->
      </div>
    </div>
    <!--右侧按钮-->
    <RightBtn
      ref="RightBtn"
      :isAllowProxy="isAllowProxy"
    ></RightBtn>
    <div class="edit_modal"></div>
  </div>
</template>

<script>
import _commonJs from "@/assets/js/itemList/index.js";
import workFlow from "@/components/workFlow/workFlow";
// import editForm from "@/components/form/editForm";
import Emitter from "@/assets/js/emitter.js";
import baseLayout from "@/components/layout/index.vue";
import detailedData from "@/components/workFlow/detailedData";
import HeadBar from "@/components/HeadBar/HeadBar";
import Qs from "qs";
import { Indicator } from "mint-ui";
// import htmlPanel from "@/components/htmlPanel"; // iframe
import htmlPanel from "@/components/cellComp/htmlPanel/index.vue"; // iframe
import RightBtn from "@/components/HeadBar/RightBtn";
import { publicGetAxios, getQueryType } from '@/assets/js/util/common.js';
import API from './api.js';

export default {
  name: "detailView",
  mixins: [Emitter, API],
  data() {
    return {
      headTitle: '',
      isWorkflow: "", //是否启用工作流
      // isReadOnly: "",
      routeParams: {},
      formParams: {},
      headbarTop: 0,
      showRefer: false,
      referConfig: {},
      appendixId: '',
      btnParam: {
        tit: "",
        func: new Function()
      },
      superEdit: "",
      viewType: "", //视图类型
      isLoadData: false,
      urrentData: {}, //当前活动数据
      isAllowProxy: false, //是否支持委托
      showAppendix: false, //附件预览
      currentData: "",
      fileName: '',
    };
  },
  beforeMount() {
    const $this = this;

    //超级编辑单据
    const queryURL = Qs.parse(location.search.substring(1)); //当前浏览器地址数据转换为对象类型
    if (queryURL && queryURL.superEdit) {
      $this.superEdit = queryURL.superEdit;
    }

    //视图类型,viewType=VIEW时，为查看视图
    if (window.pageConfig && window.pageConfig.viewType) {
      $this.viewType = window.pageConfig.viewType;
    }

    // 取配置信息
    if (window.pageConfig) {
      $this.isWorkflow = window.pageConfig.workflowEnabled;
    }

    // 获取地址参数
    let requestParams = $this.GetRequest(window.location.href);
    $this.deploymentId =
      requestParams && requestParams.deploymentId
        ? requestParams.deploymentId
        : "";
    $this.pendingId =
      requestParams && requestParams.pendingId ? requestParams.pendingId : "";

    // workFlow参数
    $this.routeParams = {
      url: $this.getRouterParams(window.pageConfig.baseServiceApi.flowRoot),
      deploymentId: $this.deploymentId,
      pendingId: $this.pendingId
    };


    // form组件参数
    $this.formParams = {
      submitUrl: window.pageConfig && requestParams.id ?
        window.pageConfig.interfaceApi.submit + `?__pc__=${requestParams.__pc__}` + `&id=${requestParams.id}` :
        window.pageConfig.interfaceApi.submit + `?__pc__=${requestParams.__pc__}`,

      deploymentId: $this.deploymentId,
      pendingId: $this.pendingId,
      viewCode: window.pageConfig && window.pageConfig.viewCode,
      layoutUrl: window.pageConfig.baseServiceApi.layoutJson + `?viewCode=${window.pageConfig.viewCode}`, //布局数据
      layoutDataUrl: window.pageConfig.interfaceApi.sourceData + `/${requestParams.id}`, //表单数据
      updateRefer: $this.updateRefer,
      isWorkflow: this.isWorkflow
    };

    //路由接口
    if (window.pageConfig && window.pageConfig.baseServiceApi.flowRoot) {
      let url = `${window.pageConfig.baseServiceApi.flowRoot}`;
      url = $this.pendingId ? `${url}?pendingId=${$this.pendingId}` : `${url}?deploymentId=${$this.deploymentId}`;
      if ($this.pendingId || $this.deploymentId) {
        publicGetAxios({ url: url }, function (result) {
          $this.currentData = result.data;
          $this.isAllowProxy = result.data.isAllowProxy;
        });
      }
    }

    //获取附件信息
    if (requestParams.id && window.pageConfig && window.pageConfig.interfaceApi.uploadList) {
      const { interfaceApi: { uploadList }, viewCode = '', modelCode, prefix } = window.pageConfig || {};
      let tableInfoId;
      if ($this.isWorkflow) {
        if (requestParams && requestParams.tableInfoId) {
          tableInfoId = requestParams.tableInfoId;
        } else {
          tableInfoId = $this.$root.$children[0].$refs.editForm && $this.$root.$children[0].$refs.editForm.tableInfoId;
        }
      } else {
        tableInfoId = requestParams.id;
      }
      let linkId = tableInfoId ? tableInfoId : "";
      let type = _commonJs.getUploadType($this.isWorkflow, modelCode);
      let uploadUrl = `${prefix}${uploadList}?linkId=${linkId}&type=${type}&viewCode=${viewCode}`;
      publicGetAxios({
        url: uploadUrl,
        type: "get"
      }, function (result) {
        if (result.data) {
          vue.appendixList = result.data; //注册全局附件信息变量
        }
      });
    }
  },
  mounted() {
    const $this = this;
    $this.headbarTop = $this.$refs.headbar
      ? $this.$refs.headbar.$el.offsetHeight
      : 0;

  },
  methods: {
    //组织路由数据接口
    getRouterParams(url) {
      url = this.pendingId
        ? `${url}?pendingId=${this.pendingId}`
        : `${url}?deploymentId=${this.deploymentId}`;
      return url;
    },

    // 基础表单提交
    handelBtnClick(operateType, isValidate) {
      this.$refs.editForm.submitData({
        status: operateType,
        operateType: operateType,
        isValidate: isValidate
      }); // 提交
    },

    // 返回上一个页面
    backHistory: function () {
      // window.history.back();
      try {
        window.mobilejs.close();
      } catch (err) {
        try {
          window.history.back();
        } catch (err) { }
      }
    },
    // 布局数据
    getLayoutData: function (data) {
      this.headTitle = data.title || window.pageConfig.viewTitle;
    }

  },
  //引用组件
  components: {
    workFlow,
    // editForm,
    baseLayout,
    detailedData,
    HeadBar,
    htmlPanel,
    RightBtn
  }
};
</script>
<!-- Add "scoped" attribute to limit CSS to this component only -->

<style lang="less" scoped>
@import "./editView.less";
.refer,
.appendix {
  position: fixed;
  width: 100%;
  height: 100%;
  right: 0px;
  top: 0;
  z-index: 4000;
  overflow: hidden;
}
</style>


// WEBPACK FOOTER //
// src/pages/edit/editView.vue