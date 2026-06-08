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
    <HeadBar
      ref="headbar"
      v-on:pageBackFunc="backHistory"
    ></HeadBar>
    <!-- <mt-button v-on:click="btnParam.func" class="m-btn" v-show="btnParam.isShow" size="small" type="default">{{btnParam.tit}}</mt-button> -->
    <div
      v-show="btnParam.isShow"
      :class="btnParam.className"
      v-html="btnParam.html"
      v-on:click="btnParam.func"
    ></div>
    <div class="m_center">
      <div
        class="m_centerBox"
        :style="{top: `${headbarTop}px`}"
      >

        <editForm
          :params="formParams"
          ref="editForm"
        ></editForm>
        <workFlow
          v-if="(pendingId || deploymentId) && layoutType == 'flow'"
          :params="routeParams"
          ref="workFlow"
        ></workFlow>

        <!-- 历史流程 -->
        <div
          v-if="layoutType == 'flow'"
          class="mt20"
        >
          <detailedData></detailedData>
        </div>

        <!-- 提交按钮 -->
        <div
          class="m_foot"
          v-if="!isReadOnly && layoutType == 'basic' && superEdit != 'true'"
        >
          <a
            v-on:click="handelBtnClick('cancel')"
            class="m_botton btn_save"
          >取消</a>
          <a
            v-on:click="handelBtnClick('save')"
            class="m_botton btn_submit"
          >保存</a>
        </div>
        <div
          class="m_foot"
          v-if="!isReadOnly && superEdit == 'true'"
        >
          <a
            v-on:click="handelBtnClick('cancel')"
            class="m_botton btn_save"
          >取消</a>
          <a
            v-on:click="handelBtnClick('submit')"
            class="m_botton btn_submit"
          >保存</a>
        </div>
      </div>
    </div>
     <!--右侧按钮-->
     <RightBtn ref="RightBtn"></RightBtn>
    <div class="edit_modal"></div>
  </div>
</template>

<script>
import _commonJs from "@/assets/js/itemList/index.js";
import workFlow from "@/components/workFlow/workFlow";
import editForm from "@/components/form/editForm";
import detailedData from "@/components/workFlow/detailedData";
import HeadBar from "@/components/HeadBar/HeadBar";
import Qs from "qs";
import { Indicator } from "mint-ui";
import htmlPanel from "@/components/htmlPanel"; // iframe
import RightBtn from "@/components/HeadBar/RightBtn";

export default {
  name: "detailView",
  data() {
    return {
      layoutType: "basic",
      isReadOnly: "",
      routeParams: {},
      formParams: {},
      headbarTop: 0,
      showRefer: false,
      referConfig: {},
      btnParam: {
        tit: "",
        func: new Function()
      },
      superEdit: "",
    };
  },
  beforeMount() {
    const $this = this;

    //超级编辑单据
    const queryURL = Qs.parse(location.search.substring(1)); //当前浏览器地址数据转换为对象类型
    if (queryURL && queryURL.superEdit) {
      $this.superEdit = queryURL.superEdit;
    }

    // 查看页面只读
    if (window._PAGECONFIG && window._PAGECONFIG.params) {
      $this.isReadOnly = window._PAGECONFIG.params.allReadOnly;
    }

    // 取配置信息
    if (window._PAGECONFIG) {
      $this.layoutType = window._PAGECONFIG.params.type;
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
      url: $this.getRouterParams(
        window._PAGECONFIG && window._PAGECONFIG.workFlowUrl
      ),
      deploymentId: $this.deploymentId,
      pendingId: $this.pendingId
    };

    // form组件参数
    $this.formParams = {
      submitUrl: window._PAGECONFIG && window._PAGECONFIG.submitUrl,
      deploymentId: $this.deploymentId,
      pendingId: $this.pendingId,
      userInfo: window._PAGECONFIG && window._PAGECONFIG.userInfo,
      viewCode: window._PAGECONFIG && window._PAGECONFIG.viewCode,
      layoutUrl: window._PAGECONFIG && window._PAGECONFIG.layoutUrl,
      layoutDataUrl: window._PAGECONFIG && window._PAGECONFIG.layoutDataUrl,
      updateRefer: $this.updateRefer
    };
  },
  mounted() {
    const $this = this;
    $this.headbarTop = $this.$refs.headbar
      ? $this.$refs.headbar.$vnode.elm.clientHeight
      : 0;

    // 注册全局方法
    $this.exportFunc(`setValueByName`, $this.setValueByName); // 修改值
    $this.exportFunc(`getconditionByName`, $this.getconditionByName); // 获取condition
    $this.exportFunc(`addBtn`, $this.addBtn); // 添加按钮
    $this.exportFunc(`refreshFormData`, $this.refreshFormData); // 刷新 form数据
    $this.exportFunc(`refreshDatagridData`, $this.refreshDatagridData); // 刷新 datagrid数据
    $this.exportFunc(`openRefSelecFunc`, $this.openRefSelecFunc); // 参照方法
    $this.exportFunc(`updateRefer`, $this.updateRefer);
  },
  methods: {
    getRouterParams(url) {
      // var url = "/msService/ec/workflow/getActiveRoot.action"; // 路由
      // var url = "../static/data/router.json"; // 路由
      url = this.pendingId
        ? `${url}?pendingId=${this.pendingId}`
        : `${url}?deploymentId=${this.deploymentId}`;
      return url;
    },

    // 基础表单提交
    handelBtnClick(operateType) {
      this.$refs.editForm.submitData({
        status: operateType,
        operateType: operateType
      }); // 提交
    },

    // 返回上一个页面
    backHistory: function() {
      try {
        window.mobilejs.close();
      } catch (err) {
        try {
          window.history.back();
        } catch (err) {}
      }
    },

    updateRefer: function(flag, url) {
      this.showRefer = flag;
      if (url) this.referConfig.url = url;
    },

    /**
     * 修改值通过name参数
     * @param {string} name key值
     * @param {string} value 显示值
     * @param {string} valueId 保存提交值
     */
    setValueByName: function(name, value, valueId) {
      const $this = this;
      let form = $this.$refs.editForm.$refs;
      if (name && form[name]) {
        let condition = form[name][0].params;
        condition.value = value;
      }
    },

    /**
     * 获取condition值
     * @param {string} name key值 datagridcode_row_key
     * @return {array} condition值
     */
    getconditionByName: function(name) {
      const $this = this;
      let form = $this.$refs.editForm.$refs;
      if (name && form[name]) {
        let temp = form[name];
        let condition = [];
        for (let i = 0; i < temp.length; i++) {
          const element = temp[i];
          condition.push(element.params);
        }
        return condition;
      }
    },

    // 传入接口重新渲染表单数据
    refreshFormData: function(url) {
      this.$refs.editForm.getDetailData(url);
    },

    // 传入接口重新渲染datagrid
    refreshDatagridData: function(url) {
      this.$refs.editForm.getDatagridByLayout(url);
    },

    /**
     * 添加自定义按钮
     * @param {function} func 自定义点击事件
     * @param {string} className class名称
     * @param {string} html 自定义html
     */
    addBtn: function(func, className, html) {
      const $this = this;
      // 渲染按钮
      $this.btnParam.isShow = true;
      $this.btnParam.html = html ? html : "";
      $this.btnParam.className = className ? "" : "m-btn";
      // $this.btnParam.tit = tit;

      if (func && typeof func == "function") {
        $this.btnParam.func = func;

        // 参照回调例子
        // $this.btnParam.func = function(){
        //   vue.openRefSelecFunc({
        //     url: '/vue/itemList.html#/',
        //     multable: 0,
        //   }, function(list){
        //     console.log(list);
        //     let url = `/wupin/wpApply/wpApply/edit__mobile__?entityCode=wupin_1.0.0.00_wpApply&id=${list.id}&__pc__=VGFza0V2ZW50XzE1MGQ2ZmxfcmVmNzEyX3JlZjI3NXwzNDM_&dataJson=1`
        //     vue.refreshFormData(url);
        //     vue.refreshDatagridData(`/wupin/wpApply/wpApply/data-dg1563325923861?datagridCode=wupin_1.0.0.00_wpApply_edit__mobile__dg1563325923861&wpApply.id=1194&rt=json`);
        //   });
        // }
      }
    },

    /**
     * 打开参照
     * @params data 数据格式
     * {
     *   url: "",
     *   multable: 0, 单选，多选
     *   referenceview:{
     *      code: "" ,
     *      iscrosscompany: "", 跨集团
     *   }
     * }
     */
    openRefSelecFunc: function(data, callbackFunc, goBack) {
      const $this = this;
      // 获取参照参数，跨公司 回填到url
      let url = data.url;
      if (!url && url == "") {
        this.myToast("无效地址");
        return false;
      }

      if (!data.referenceview) {
        data.referenceview = {
          code: new Date().getTime().toString(),
          iscrosscompany: ""
        };
      }

      $this.showRefer = true;
      $this.referConfig.url = _commonJs.resolveReferUrl(data.url, data);
      $this.$children[0].isReadOnly = true;

      $this.$children[0].$forceUpdate();
      Indicator.open({
        text: "加载中...",
        //文字
        spinnerType: "fading-circle"
        //样式
      });
      $this.$nextTick(function() {
        // 回调方法
        let iframe = document.getElementById("newPage");
        iframe.onload = function() {
          Indicator.close();
          if (iframe) {
            // 去掉原生头部
            //   try {
            //       window.mobilejs.open_dialog();
            //   } catch (err) {}
            // 注册方法
            // 选择回调方法
            let code = "";
            if (data.referenceview.code)
              code = data.referenceview.code.replace(/\./gi, "_");
            iframe.contentWindow[`_callBack_ec_${code}`] = function(list) {
              // 恢复原生头部
              //   try {
              //       window.mobilejs.close_dialog();
              //   } catch (err) {}

              setTimeout(function() {
                $this.showRefer = false;
              }, 200);

              if (list && list.length > 0) {
                list = list[0];
              }

              // 回调赋值
              if (callbackFunc && typeof callbackFunc == "function") {
                callbackFunc(list);
              }
            };

            // 返回按钮方法
            iframe.contentWindow[`_close_ec_${code}`] = function() {
              // 关闭iframe
              setTimeout(function() {
                $this.showRefer = false;
              }, 200);

              //返回回调
              if (goBack && typeof goBack == "function") {
                goBack();
              }
              // 恢复原生头部
              //   try {
              //       window.mobilejs.close_dialog();
              //   } catch (err) {}
            };
          }
        };
      });
    }
  },
  //引用组件
  components: {
    workFlow,
    editForm,
    detailedData,
    HeadBar,
    htmlPanel,
    RightBtn
  }
};
</script>
<!-- Add "scoped" attribute to limit CSS to this component only -->

<style lang="less" scoped>
@import "../../assets/css/editView.less";
.refer {
  position: fixed;
  width: 100%;
  height: 100%;
  right: 0px;
  top: 0;
  z-index: 999;
  overflow: hidden;
}
</style>



// WEBPACK FOOTER //
// src/pages/edit/editView.vue