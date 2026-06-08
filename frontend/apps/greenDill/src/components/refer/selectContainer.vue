<template>
  <div class="m_centerBox">
    <!-- 头部菜单 -->
    <organizList
      v-on="{updateChoooseList: this.params.updateChoooseList, returnHome: this.params.returnHome}"
      ref="organizChild"
      :propBind="propBind_O"
      :showSwitchCompany="params.showSwitchCompany"
      :companyInfo="params.companyInfo"
      :type="params.type"
    ></organizList>

    <div class="m_staffBoxWrap">
      <div class="ms_wrap">
        <groupSlideList
          v-on:returnHome="this.params.returnHome"
          v-on:updateChoooseList="this.params.updateChoooseList"
          ref="groupSlideChild"
          :propBind="propBind"
        ></groupSlideList>
      </div>
    </div>
  </div>
</template>

<script>
import groupSlideList from "./groupSlideList";
import organizList from "./organizList";
export default {
  name: "selectContainer",
  data() {
    return {
      sheetVisible: false,
      groupData: [],
      formName: "",
      basicUrl: "",
      isSingleCheck: 0,
      curChooseList: [],
      propBind_O: { // orgainz配置项
        linkcallBack: this.linkcallBack,
        selectCallBack: this.selectCallBack,
        listUrl: this.params.groupUrl,
        nameType: this.params.nameType,
        isSingleCheck: this.params.isSingleCheck,
        showPeopleNum: true,
        isMemeberChoose: 1,
        groupId: this.params.groupId,
        tit: this.params.childTit,
        updatePanel: ""
      },
      propBind: { // slide配置项
        footerH: this.$parent.$refs.footer && this.$parent.$refs.footer.clientHeight,
        listUrl: `${this.params.listUrl}${this.params.groupId ? '&departmentId=' + this.params.groupId : ''}`,
        isSingleCheck: this.params.isSingleCheck
      }
    };
  },
  props: ["params"],
  mounted() {
    let $this = this;
    let groupId = $this.params && $this.params.groupId;
    // $this.basicUrl = `${this.params.listUrl}&departmentId=${groupId}`;
    $this.propBind_O.updatePanel = this.$refs.groupSlideChild && this.$refs.groupSlideChild.updatePanel;
  },
  methods: {
    //根据key查找对应的数值
    getValueByKey: function (data, element) {
      var key = element.key;
      var isDate = element.showType == "DATE";
      var getKey = key.split(".");
      var temp = data;
      for (var k = 1; k < getKey.length; k++) {
        if (temp[getKey[k]]) {
          temp = temp[getKey[k]];
          if (isDate && element.showFormat) {
            //时间格式化
            temp = this.dateFtt(element.showFormat, new Date(temp));
          }
        } else {
          return "";
        }
      }
      return temp;
    },
    /**
     * 点击链接后回调
     * unFresh {bool} 是否刷新列表 默认为true
     * list, unFresh, isAll
     */
    linkcallBack: function (params) {
      // 刷新列表
      const {
        list = {},
        unFresh = false,
        isAll = false,
        isHead = false
      } = params;
      let url = this.$refs.groupSlideChild.loadLink.split("&departmentId");
      url = `${url[0]}${list.id ? '&departmentId=' + list.id : ''}`;
      let flag = list.isChecked;
      let pageSize = 20;
      // 单选的时候刷新下拉列表，多选的时候不刷新
      this.$refs.groupSlideChild.pageNum["current"] = 1;
      if (isAll) {
        pageSize = 500;
      }
      if (isHead) flag = 2;
      this.$refs.groupSlideChild.pageNum["pageSize"] = pageSize;
      this.$refs.groupSlideChild.getLoadData({
        url: url,
        isChecked: flag,
        isRefresh: unFresh ? false : true
      });
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
</style>


// WEBPACK FOOTER //
// src/components/refer/selectContainer.vue