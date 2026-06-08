<template>
  <div id="app">
    <div
      class="g_container"
      :class="{ 'g_container_reset_color': isAndroidLessVersionFive }"
    >
      <HeadBar
        ref="headbar"
        v-on:pageBackFunc="backHistory"
        :headTitle='formName'
      ></HeadBar>
      <template v-if="!showSearch">
        <div class="m_search">
          <input
            v-on:click="linkSearch"
            class="inp_search"
            type="text"
            :placeholder="placeholderText"
          >
        </div>
        <!--人员选择一-->
        <memberSubset
          v-if="curShowPage == 'memberSubset'"
          v-on:linkOther="linkOther"
          ref="memberSubset"
          :params="{
            name: formName, 
            childTit: childTit, 
            nameType: curNameType, 
            isSingleCheck: isSingleCheck, 
            updateChoooseList: updateChoooseList,
            listUrl: listUrl,
            companyId:loginCompanyInfo.id,
          }"
        ></memberSubset>

        <!--岗位选择和部门选择-->
        <groupSubset
          v-else-if="curShowPage == 'groupSubset'"
          ref="groupSubset"
          :params="{
            name: formName, 
            childTit: childTit ,
            nameType: curNameType, 
            isSingleCheck: isSingleCheck, 
            updateChoooseList: updateChoooseList,
            listUrl: listUrl
          }"
        ></groupSubset>

        <!--人员选择二-->
        <selectContainer
          v-else-if="curShowPage == 'selectContainer'"
          :key="companyId"
          :params="{
            groupId: selectContainerParam.groupId, 
            nameType: curNameType, 
            childTit: childTit, 
            isSingleCheck: isSingleCheck, 
            updateChoooseList: updateChoooseList, 
            returnHome: returnHome,
            groupUrl: groupUrl,
            listUrl: listUrl,
            showSwitchCompany:showSwitchCompany,
            companyInfo:companyInfo,
            type:curType
          }"
        ></selectContainer>
      </template>
      <searchResult
        v-else-if="showSearch"
        v-on:hideSearch="hideSearch"
        :params="searchParams"
      ></searchResult>
      <div
        class="m_foot"
        ref="footer"
        v-if="isSingleCheck == '0'"
      >
        <div class="m_selInfo">
          <span class="ms_text">
            {{$t('refer.state.selected')}}
            <em
              class="num"
              id="selectNum"
            >{{this.$store.state.curChooseList.length}}</em>
            {{isMemeberChoose?$t('refer.unit.person'):$t('refer.unit.ge')}}
          </span>
        </div>
        <a
          class="m_botton btn_submit"
          v-on:click="submitData"
        >{{$t('button.determine.text')}}</a>
      </div>
    </div>
    <div class="edit_modal"></div>
  </div>
</template>

<script>
import groupSubset from "./groupSubset";
import memberSubset from "./memberSubset";
import selectContainer from "./selectContainer";
import searchResult from "./searchResult";
import HeadBar from "@/components/HeadBar/HeadBar";
import _commonJs from "@/assets/js/itemList/index.js";
import { publicGetAxios, getLoginInfo } from '@/assets/js/util/common.js';
import baseService from './service.js';
export default {
  name: "selectDetail",
  data() {
    return {
      sheetVisible: false,
      groupData: [1, 2, 3, 4, 5, 6, 7],
      headList: [],
      formName: "",
      placeholderText: "",
      isSingleCheck: 0,
      childTit: "",
      curNameType: "",
      curChooseList: [], //当前选中的列表
      isMemeberChoose: 0,
      memberBack: [], //记录返回前的操作
      selectContainerParam: {},
      curShowPage: "", // 组件名
      showSearch: false,
      listUrl: '',
      companyId: '',
      companyInfo: {},
      curType: '',
      searchParams: {
        //搜索params
        showFlag: false,
        isSingleCheck: 0,
        listUrl: "",
        placeholderClickText: ""
      },
      groupUrl: '',
      isAndroidLessVersionFive: false, // 安卓手机系统是否小于等于5.0
      showSwitchCompany: false
    };
  },
  props: ["params"],
  async beforeMount() {
    const { company: { id: companyId, name: companyName } } = await getLoginInfo();
    this.companyId = companyId;
    this.companyInfo = this.loginCompanyInfo = { id: companyId, name: companyName };
    this.getConfig();

  },
  mounted() {
    // 获取当前安卓手机版本号小于等于5的进行样式处理
    this.isAndroidLessVersionFive = _commonJs.isLessEqualAndroidVersion(5);

  },
  methods: {
    getConfig: function (companyId = this.companyId) {
      const { type } = window.pageConfig || {};
      this.curType = type;
      this.getUrlConfig();
      switch (type) {
        case 'staff':
          this.curShowPage = "memberSubset";
          this.memberBack = [this.curShowPage];
          this.searchParams.showFlag = true;
          this.isMemeberChoose = 1;
          break;
        case 'user':
          this.curShowPage = "memberSubset";
          this.memberBack = [this.curShowPage];
          this.searchParams.showFlag = true;
          this.isMemeberChoose = 1;
          break;
        case 'company':
          break;

        default:
          break;
      }
      this.searchParams.isSingleCheck = this.isSingleCheck;
      this.searchParams.listUrl = this.listUrl;
      // this.searchParams.curChooseList = this.curChooseList;
      this.searchParams.updateChoooseList = this.updateChoooseList;
      this.searchParams.nameType = this.curNameType;

      // curChooseList 清空重新赋值
      this.$store.commit("clearAll");
      this.curChooseList = this.$store.state.curChooseList;
    },
    getUrlConfig: function (companyId = this.companyId) {
      let name, groupId, placeholderText, type;
      if (this.params) {
        // 参数传参
        name = this.params.name;
        groupId = this.params.groupId;
        this.curNameType = this.params.nameType;
        this.isSingleCheck = this.params.isSingleCheck;
      } else if (window.pageConfig) { // 配置项
        const { pageConfig: cofig } = window;
        name = this.$t(cofig.viewTitle);
        placeholderText = this.$t(cofig.placeholderText);
        type = cofig.type;
        this.curNameType = cofig.nameType;
        this.isSingleCheck = cofig.isSingleCheck;
      }

      // 地址取参数
      let urlConfig = this.GetRequest(window.location.href);
      let conditionParams = '', crossCompanyFlag = '', selectPeople = '';
      if (urlConfig) {
        const { multiSelect, callBackFuncName, closeFuncName, condition = '' } = urlConfig;
        this.isSingleCheck = multiSelect - 0 == 1 ? 0 : 1;
        this.callBackFuncName = callBackFuncName;
        this.closeFuncName = closeFuncName;
        // 分割condition 筛选参数
        if (condition) conditionParams = `&${condition}`;
        // 添加跨集团搜索
        if (urlConfig.crossCompanyFlag != "undefined") {
          crossCompanyFlag = `&crossCompanyFlag=${urlConfig.crossCompanyFlag}`;
        }
        if (urlConfig.selectPeople) {
          selectPeople = `&selectPeople=${urlConfig.selectPeople}`;
        }
      }

      let searchUrl;
      this.formName = name;
      this.placeholderText = placeholderText;
      this.searchParams.placeholderClickText = placeholderText;
      this.curShowPage = "groupSubset";
      this.groupUrl = `/inter-api/organization/v1/departments/ref?companyId=${companyId}${conditionParams}${crossCompanyFlag}`;
      this.childTit = this.$t(`refer.headName.${['staff', 'user'].includes(type) ? 'contact' : type}`);
      this.listUrl = baseService[type];
      if (!['company', 'currentDepart'].includes(type)) {
        this.listUrl = this.listUrl + `&companyId=${companyId}`
      }
    },
    // 隐藏搜索
    hideSearch: function () {
      this.showSearch = false;
    },
    // 跳转
    linkOther: function (res) {
      const { groupInfo } = res;
      this.companyId = this.loginCompanyInfo.id;
      this.getUrlConfig(this.companyId);
      this.selectContainerParam = res;
      this.showSwitchCompany = res.isCompany;
      this.curShowPage = "selectContainer";
      this.companyInfo = groupInfo ? groupInfo : this.loginCompanyInfo;
      this.curType = res.type ? res.type : this.curType;
      this.oldCompanyId = '';
    },
    // 返回上一个页面
    backHistory: function () {
      if (typeof window[this.closeFuncName] == "function") {
        window[this.closeFuncName](); // 执行给关闭方法
      }
    },
    // 到搜索页面
    linkSearch: function () {
      this.showSearch = true;
    },
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
    // 更新选中数据
    updateChoooseList: function (res) {
      if (this.isSingleCheck == "1") {
        // 单选
        this.$store.commit("updateData", {
          name: "curChooseList",
          value: res
        });

        this.rememberLink(() => {
          if (typeof window[this.callBackFuncName] == "function") {
            window[this.callBackFuncName](this.$store.state.curChooseList); // 回调selectCallBack方法
          }

          // 是否有回调函数
          if (this.params && this.params.callback && typeof this.params.callback == "function") {
            this.params.callback();
          }
        });


      }
    },
    // 确定人员
    submitData: function () {
      console.log(this.$store.state.curChooseList);
      if (!this.$store.state.curChooseList || this.$store.state.curChooseList.length == 0) {
        this.myToast(this.$t('notice.selectOne.atLeast'));
        return false;
      }
      if (this.curNameType == "1") {
        // 人员选择
        // this.rememberLink();
      }
      this.$store.commit("updateData", {
        name: "curShowPage",
        value: "pagelist"
      });

      this.rememberLink(() => {
        if (typeof window[this.callBackFuncName] == "function") {
          window[this.callBackFuncName](this.$store.state.curChooseList); // 回调selectCallBack方法
        }

        // 是否有回调函数
        if (
          this.params &&
          this.params.callback &&
          typeof this.params.callback == "function"
        ) {
          this.params.callback();
        }
      });
    },

    // 记录常用联系人
    rememberLink: function (callback) {
      let ids = [];
      let list = this.$store.state.curChooseList;

      // 排除岗位页面
      if (this.curShowPage === 'groupSubset') {
        callback && typeof callback == "function" && callback();
        return;
      }

      if (list && list.id) {
        ids = list.id;
      } else {
        for (let i = 0; i < list.length; i++) {
          const element = list[i];
          ids.push(element.id);
        }
        ids = ids.join(",");
      }

      // 接口记录
      // publicGetAxios({
      //   url: "/foundation/user/common/staffUseRecord.action?ids=" + ids,
      //   // type: "get"
      // },
      //   function (res) {
      //     if (res.success) {
      //       console.log("保存成功");
      //     }
      //     if (callback && typeof callback == "function") {
      //       callback();
      //     }
      //   }
      // );

      if (callback && typeof callback == "function") {
        callback();
      }
    },
    // 联系人返回首页
    returnHome: function () {
      this.curShowPage = this.memberBack ? this.memberBack[0] : "";
    },

    updateCompanyId: function (companyInfo) {
      const { id: companyId } = companyInfo;
      if (companyId !== this.oldCompanyId) {
        this.companyId = companyId;
        this.getUrlConfig(companyId);
        this.selectContainerParam = { ...this.selectContainerParam, groupId: companyId };
        this.companyInfo = companyInfo;
        this.curShowPage = "selectContainer";
      }
      this.oldCompanyId = companyId;
    }
  },
  components: {
    memberSubset,
    groupSubset,
    searchResult,
    selectContainer,
    HeadBar
  }
};
</script>

<!-- 样式加载 -->

<style lang="less" scoped>
@import "../../assets/css/groupSubset.less";
</style>



// WEBPACK FOOTER //
// src/components/refer/selectDetail.vue