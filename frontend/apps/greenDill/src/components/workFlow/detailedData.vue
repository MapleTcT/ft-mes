<template>
  <div
    class="m_box m_detailed"
    v-if="detailedDataList.length > 0"
  >
    <div class="m_wrap">

      <!--显示详情和隐藏详情的按钮-->
      <div
        class="mp_details"
        @click="switchDetails"
      >
        <div
          class="mp_details_display"
          v-if="expandDealInfo"
        >
          <img
            src="@/assets/images/clyjicon_xs.png"
            :alt="$t('workflow.detail.show')"
          >
          <span>{{$t('workflow.detail.show')}}</span>
        </div>
        <div
          class="mp_details_display"
          v-else
        >
          <img
            src="@/assets/images/clyjicon_yc.png"
            :alt="$t('workflow.detail.hide')"
          >
          <span>{{$t('workflow.detail.hide')}}</span>
        </div>
      </div>

      <!--显示详情-->
      <div
        class="mp_box"
        v-for="(item, index) in detailedDataList"
        :key="index"
        v-if="!expandDealInfo"
      >
        <!--处理意见的图标 start-->
        <!--会签线-->
        <div
          class="mp_icon mp_state_hq"
          v-if="item[11]=='7' && item[12]=='1'"
        ></div>
        <!--提交线-->
        <div
          class="mp_icon mp_state_launch"
          v-else-if="item[12]=='1'"
        ></div>
        <!--驳回线-->
        <div
          class="mp_icon mp_state_oppose"
          v-else-if="item[12]=='2'"
        ></div>
        <!--作废线-->
        <div
          class="mp_icon mp_state_cancel"
          v-else-if="item[12]=='3'"
        ></div>
        <!--通知线-->
        <div
          class="mp_icon mp_state_notice"
          v-else-if="item[12]=='4' || item[11]=='5'"
        ></div>
        <!--委托线-->
        <div
          class="mp_icon mp_state_wt"
          v-else-if="item[12]==null && item[7]=='CONSIGNOR'"
        ></div>
        <!--保存线-->
        <div
          class="mp_icon mp_state_save"
          v-else-if="item[12]==null && item[7]!='RECALL'"
        ></div>
        <!--撤回线-->
        <div
          class="mp_icon mp_state_recall"
          v-else-if="item[7]=='RECALL'"
        ></div>
        <!--处理意见的图标 end-->

        <!--活动名称：1提交线、2驳回线、3作废线、4通知线、null保存线、RECALL撤回线-->
        <div
          class="mp_title mp_launch"
          v-if="item[12]=='1' || item[12]=='2' || item[12]=='3' || item[12]=='4' || item[12]==null || item[7]=='RECALL'"
        >
          {{item[0]}}
        </div>

        <!--处理人和处理时间-->
        <div class="mp_time">{{item[4]}} {{formatTime("MD_HM", item[1])}}</div>

        <!--连接线-->
        <div
          v-if="index < detailedDataList.length - 1"
          v-html="(item[2] && item[2] != 'undefined')?item[2]:''"
          class="mp_tips line_solid"
        >
        </div>
        <div
          v-else-if="index == detailedDataList.length - 1 && currentData && currentData.type"
          v-html="(item[2] && item[2] != 'undefined')?item[2]:''"
          class="mp_tips line_solid"
        >
        </div>
        <div
          v-else-if="index == detailedDataList.length - 1 && !(currentData && currentData.type)"
          v-html="(item[2] && item[2] != 'undefined')?item[2]:''"
          class="mp_tips"
        >
        </div>

      </div>

      <!--隐藏详情-->
      <div
        class="mp_box"
        v-for="(item, index) in detailedDataList"
        :key="index"
        v-if="((item[2] && item[2] != 'undefined') || index==detailedDataList.length-1) && expandDealInfo"
      >

        <!--处理意见的图标 start-->
        <!--会签线-->
        <div
          class="mp_icon mp_state_hq"
          v-if="item[11]=='7' && item[12]=='1'"
        ></div>
        <!--提交线-->
        <div
          class="mp_icon mp_state_launch"
          v-else-if="item[12]=='1'"
        ></div>
        <!--驳回线-->
        <div
          class="mp_icon mp_state_oppose"
          v-else-if="item[12]=='2'"
        ></div>
        <!--作废线-->
        <div
          class="mp_icon mp_state_cancel"
          v-else-if="item[12]=='3'"
        ></div>
        <!--通知线-->
        <div
          class="mp_icon mp_state_notice"
          v-else-if="item[12]=='4' || item[11]=='5'"
        ></div>
        <!--委托线-->
        <div
          class="mp_icon mp_state_wt"
          v-else-if="item[12]==null && item[7]=='CONSIGNOR'"
        ></div>
        <!--保存线-->
        <div
          class="mp_icon mp_state_save"
          v-else-if="item[12]==null && item[7]!='RECALL'"
        ></div>
        <!--撤回线-->
        <div
          class="mp_icon mp_state_recall"
          v-else-if="item[7]=='RECALL'"
        ></div>
        <!--处理意见的图标 end-->

        <!--活动名称：1提交线、2驳回线、3作废线、4通知线、null保存线、RECALL撤回线-->
        <div
          class="mp_title mp_launch"
          v-if="item[12]=='1' || item[12]=='2' || item[12]=='3' || item[12]=='4' || item[12]==null || item[7]=='RECALL'"
        >
          {{item[0]}}
        </div>

        <!--处理人和处理时间-->
        <div class="mp_time">{{item[4]}} {{formatTime("MD_HM", item[1])}}</div>

        <!--连接线-->
        <div
          v-if="index < detailedDataList.length-1 && !detailedDataList[index+1][2]"
          v-html="(item[2] && item[2] != 'undefined')?item[2]:''"
          class="mp_tips line_dashed"
        >
        </div>
        <div
          v-else-if="index < detailedDataList.length-1"
          v-html="(item[2] && item[2] != 'undefined')?item[2]:''"
          class="mp_tips line_solid"
        >
        </div>
        <div
          v-if="index == detailedDataList.length-1 && currentData && currentData.type"
          v-html="(item[2] && item[2] != 'undefined')?item[2]:''"
          class="mp_tips line_solid"
        >
        </div>
        <div
          v-else-if="index == detailedDataList.length-1 && !(currentData && currentData.type)"
          v-html="(item[2] && item[2] != 'undefined')?item[2]:''"
          class="mp_tips"
        >
        </div>
      </div>

      <!--当前操作线-->
      <div
        class="mp_box"
        v-if="currentData && currentData.type"
      >
        <div class="mp_icon mp_state_edit"></div>
        <div class="mp_title mp_edit">{{currentData.name}}</div>
      </div>
    </div>
  </div>
</template>

<script>
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import { publicGetAxios } from '@/assets/js/util/common.js';
export default {
  name: "detailedData",
  data: function () {
    return {
      detailedDataList: [], //详情数据列表
      expandDealInfo: true, //处理意见默认不展开
    };
  },
  props: ['params', "currentData"],
  mounted() {
    var $this = this;
    let urlParams = $this.GetRequest(window.location.href);
    let tableInfoId = $this.$parent.$refs.editForm.tableInfoId;
    //处理意见
    var url;
    if (window.pageConfig && window.pageConfig.interfaceApi.dealInfoList) {
      url = `${window.pageConfig.interfaceApi.dealInfoList}?tableInfoId=${tableInfoId}&expandDealInfo=true&groupByTask=false`;
    }
    // url = "static/data/opinion.json";       
    publicGetAxios({ url: url }, function (result) {
      $this.detailedDataList = result.data;
    });
  },
  methods: {
    //点击切换处理意见的展开与收起状态
    switchDetails: function () {
      this.expandDealInfo = !this.expandDealInfo;
    },

    // 时间格式化
    formatTime: function (fmt, date) {
      return _commonJs.getDateFormat(fmt, new Date(date));
    },
  }
};
</script>

<!-- 样式加载 -->
<style lang="less" scoped>
@import "./detailedData.less";
</style>



// WEBPACK FOOTER //
// src/components/workFlow/detailedData.vue