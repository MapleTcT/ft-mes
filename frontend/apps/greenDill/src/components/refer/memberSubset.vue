<template>
  <div class="m_centerBox">
    <div class="m_box">
      <h3 class="m_title">{{$t('refer.organisation.title')}}</h3>
      <ul
        class="m_list"
        id="groupMenu"
      >
        <li v-on:click="$emit('linkOther', {groupId: params.companyId, type:'department', isCompany:crossCompanyFlag, nameType: 2,isSingleCheck: isSingleCheck})">
          <img
            class="ml_icon"
            src="@/assets/images/pic_zzjg.png"
            alt=""
          >
          <span class="ml_text">{{$t('refer.nav.organisation')}}</span>
        </li>
        <li v-on:click="$emit('linkOther', {groupId: userGroup.id, type:'curDepartment', groupInfo, nameType: 2, isSingleCheck: isSingleCheck})">
          <img
            class="ml_icon"
            src="@/assets/images/pic_bm.png"
            alt=""
          >
          <span class="ml_text">{{userGroup.text}}</span>
        </li>
      </ul>
    </div>
    <div class="m_staffBoxWrap">
      <div class="ms_wrap">
        <h3 class="m_box m_title">{{$t('refer.nav.topContacts')}}</h3>
        <groupSlideList
          v-on:updateChoooseList="this.params.updateChoooseList"
          :propBind="propBind"
        ></groupSlideList>
      </div>
    </div>
  </div>
</template>

<script>
import groupSlideList from "./groupSlideList";
import { publicGetAxios } from '@/assets/js/util/common.js';
import baseService from './service.js';
// import sidePanel from "./sidePanel";
export default {
  name: "memberSubset",
  data() {
    return {
      formName: "",
      listData: "",
      allLoaded: false,
      onloadFun: null,
      userGroup: {
        text: '',
        id: ''
      },
      groupInfo: '',
      // curChooseList: [],
      isSingleCheck: 0,
      propBind: {
        footerH: this.$parent.$refs.footer && this.$parent.$refs.footer.clientHeight,
        // listUrl: this.params.listUrl, // TODO 获取常用联系人
        isSingleCheck: 0, //单选 多选
      }
    };
  },
  props: {
    params: {
      default: {},
      type: Object
    }
  },
  beforeMount() {
    const { isSingleCheck } = this.params;
    this.propBind.isSingleCheck = this.isSingleCheck = isSingleCheck;
    let urlConfig = this.GetRequest(window.location.href);
    this.crossCompanyFlag = urlConfig.crossCompanyFlag === 'true' ? true : false;
  },
  mounted() {
    const { name } = this.params;
    this.propBind.footerH = this.$parent.$refs.footer && this.$parent.$refs.footer.clientHeight;
    // 当前登录人 常用联系人
    publicGetAxios({
      url: baseService.currentDepart, //获取当前部门接口
      type: 'get'
    }, (res = {}) => {
      const { data } = res;
      if (data) {
        this.userGroup.text = data.name;
        this.userGroup.id = data.id;
        this.groupInfo = data;
      }
    })
    this.formName = name;
  },
  methods: {

  },
  components: {
    groupSlideList
  }
};
</script>

<!-- 样式加载 -->
<style lang="less" scoped>
@import "../../assets/css/groupSubset.less";
</style>


// WEBPACK FOOTER //
// src/components/refer/memberSubset.vue