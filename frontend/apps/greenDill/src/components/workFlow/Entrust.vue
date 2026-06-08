<template>

  <div
    id="entrust"
    class="entrust"
    v-if="isShowEntrust"
  >
    <header
      class="m_head"
      :style="{background: bgColor}"
    >
      <div class="m_hbox">
        <span
          class="head_arrow"
          v-on:click="backHistory"
        >
          <a
            href="javascript:;"
            class="m_arrow"
          ></a>
        </span>
        <h2 class="m_title">{{$t('workflow.entrust.title')}}</h2>
      </div>
    </header>
    <form
      method="post"
      name="form"
      :action="formUrl"
    >
      <div class="m_entrust_center">
        <!-- 委托方式 proxyType -->
        <div class="m_entrust entrust_type">
          <div class="type_title">{{$t('workflow.entrust.way')}}</div>
          <div class="type_content">
            <!--会签活动type为7时，委托方式只有全权委托-->
            <template v-if="routerType == '7' ">
              <div class="entrust_ms_check">
                <div class="entrust_ckb_wrap">
                  <input
                    type="radio"
                    value="3"
                    name="proxyType"
                    v-model="proxyType"
                    checked
                  >
                  <label for="checkList"></label>
                </div>
              </div>
              <span class="type_text">{{$t('workflow.enturst.handle.full')}}</span>
            </template>
            <template v-else>
              <div class="entrust_ms_check">
                <div class="entrust_ckb_wrap">
                  <input
                    type="radio"
                    value="2"
                    name="proxyType"
                    v-model="proxyType"
                    checked
                  >
                  <label for="checkList"></label>
                </div>
              </div>
              <span class="type_text">{{$t('workflow.enturst.handle.copy')}}</span>
              <div class="entrust_ms_check">
                <div class="entrust_ckb_wrap">
                  <input
                    type="radio"
                    value="3"
                    name="proxyType"
                    v-model="proxyType"
                  >
                  <label for="checkList"></label>
                </div>
              </div>
              <span class="type_text">{{$t('workflow.enturst.handle.full')}}</span>
            </template>

          </div>
        </div>

        <!-- 被委托人 -->
        <div class="m_entrust entrust_people">
          <selectPersonnel
            ref="proxyUsers"
            name="proxyUsers"
            :params="{personnelTitle: $t('workflow.enturst.person')}"
            @update="updatePerson"
          ></selectPersonnel>
        </div>

        <!-- 委托说明 proxDesc -->
        <div class="m_entrust entrust_explain">
          <div>{{$t('workflow.enturst.info')}}</div>
          <textarea
            class="explain_text"
            ref="proxDesc"
            name="proxDesc"
            :rows="isAndroidLessVersionFive ? 12 : 15"
            :placeholder="$t('workflow.enturst.info.placeholder')"
          ></textarea>
        </div>
      </div>
    </form>

    <div
      class="m_foot"
      @touchmove.prevent
    >
      <a
        class="m_botton"
        v-on:click="btnClickEvent('cancel')"
      >{{$t('button.cancel.text')}}</a>
      <a
        class="m_botton btn_sendout"
        v-on:click="btnClickEvent('sendout')"
      >{{$t('button.send.text')}}</a>
    </div>
  </div>
</template>
<script>
import { Toast, Indicator } from "mint-ui";
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import selectPersonnel from "@/components/workFlow/selectPersonnel";
import axios from 'axios'; //接口请求
import Qs from 'qs';
import { publicGetAxios } from '../../assets/js/util/common';
export default {
  name: 'Entrust',
  data() {
    return {
      isShowEntrust: false, //是否显示委托界面
      proxyType: "",
      proxyUsers: "",
      formUrl: "",
      isAndroidLessVersionFive: false, // 安卓手机系统是否小于等于5.0
      routerType: '' //工作流活动类型
    }
  },

  //数据监听
  watch: {
    'isShowEntrust': {
      deep: true,
      handler: function (newVal, oldVal) {//解决ios穿透的问题
        if (newVal) { //显示
          this.routerType = this.$root.$children[0].$refs.workFlow.routerData.type;
          if (this.routerType == '7') { //会签活动
            this.proxyType = "3";
          } else {
            this.proxyType = "2";
          }

          this.closeTouch();
        } else {
          this.openTouch();
        }
      }
    }
  },
  beforeMount() {
    if (window.pageConfig) {
      this.bgColor = window.pageConfig.bgColor ? window.pageConfig.bgColor : ''; // 背景色
    }
  },
  mounted() {
    // 获取当前安卓手机版本号小于等于5的进行样式处理
    this.isAndroidLessVersionFive = _commonJs.isLessEqualAndroidVersion(5);
  },
  methods: {
    // 返回上一个页面
    backHistory: function () {
      this.isShowEntrust = false; //关闭委托界面
    },

    closeTouch: function () {
      document.getElementsByClassName("m_centerBox")[0].style.overflow = "hidden";
    },

    openTouch: function () {
      document.getElementsByClassName("m_centerBox")[0].style.overflow = "auto";
    },

    //选择被委托人
    updatePerson: function () {
      const $this = this;
      let entrustPeople = $this.$refs.proxyUsers.personnelList;
      let user;
      $this.proxyUsers = "";
      for (let i = 0; i < entrustPeople.length; i++) {
        user = entrustPeople[i].id;
        $this.proxyUsers += user + ",";
      }
    },

    //组织发送的数据
    dataSubmit: function () {
      event.preventDefault();
      let $this = this;
      let data = {};
      let urlParams = $this.GetRequest(window.location.href);
      if (urlParams && urlParams.pendingId) {
        data.pendingId = urlParams.pendingId;
      }
      data.proxyType = $this.proxyType;
      $this.updatePerson();
      if ($this.proxyUsers.length > 0) {
        data.proxyUsers = $this.proxyUsers;
      } else {
        $this.myToast(this.$t('notice.enturst.person'));
        return false;
      }
      data.proxDesc = this.$refs.proxDesc.value; //委托说明
      return data;
    },

    btnClickEvent: function (type) {
      let $this = this;
      if (type == "sendout") {
        let prefix = window.pageConfig.prefix;
        let url = prefix + "/baseService/workflow/proxyPendingResult";
        let data = $this.dataSubmit(); // 获取发送的参数
        if (!data) {
          return false;
        }
        if (data.proxDesc.length >= 255) {
          this.myToast(this.$t('workflow.enturst.info.limit'));
          return false;
        }
        publicGetAxios({
          url: url,
          type: 'get',
          data: data,
          headers: { "Content-Type": "application/json;charset=UTF-8" },
          error: (error) => {
            let data = error.response.data;
            let errorMsg = data.message || data.msg;
            Toast({
              message: errorMsg,
              iconClass: "mintui mintui-error"
            });
          }
        }, (response) => {
          if (response.code === 200) {
            Toast({
              message: this.$t('notice.send.success'),
              iconClass: "mintui mintui-success"
            });
            if ($this.proxyType == "2") { //复制委托
              $this.isShowEntrust = false; //关闭委托界面
            } else if ($this.proxyType == "3") { //全权委托返回列表视图
              setTimeout(() => {
                $this.isShowEntrust = false; //关闭委托界面
                // window.history.back(); // TODO返回
                try {
                  window.mobilejs.close();
                } catch (err) {
                  try {
                    window.history.back();
                  } catch (err) {
                  }
                }
              }, 1000);
            }
          } else {
            Toast({
              message: this.$t('notice.send.fail'),
              iconClass: "mintui mintui-error"
            });
          }
        });
      } else if (type == "cancel") {
        $this.isShowEntrust = false;
      }
    }
  },

  //引用组件
  components: {
    selectPersonnel
  }
}
</script>
<style lang="less" scoped>
@import "./entrust.less";
</style>



// WEBPACK FOOTER //
// src/components/workFlow/Entrust.vue