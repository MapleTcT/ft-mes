<template>
  <div class="">
    <!-- 头部菜单 -->
    <h3
      v-if="this.propBind.tit || headList.length > 0"
      class="m_crumbs"
      id="breadMenu"
    >
      <span v-on:click="headLineLink(-1), returnHome()">{{this.propBind.tit}}</span>
      <template v-for="(list, index) in headList">
        <i
          v-if="index == 0"
          class="m_more_font"
        >&gt;</i>
        <span
          v-on:click="headLineLink(index)"
          :class="index == headList.length-1? 'cur':''"
        >{{list.name}}
          <i
            v-if="index < headList.length-1"
            class="m_more_font"
          >&gt;</i>
        </span>
      </template>
      </span>
    </h3>
    <!-- 公司切换 -->
    <companySwitch
      v-if="headList.length && showSwitchCompany"
      :defaultValue="headList[0]"
      :switchCompany="switchCompany"
    />
    <div
      v-if="groupData && groupData.length > 0 && !(selectPeople == '3' && groupId != -1)"
      class="m_box padding0 group_list"
      id="groupBox"
    >
      <template v-for="(list, index) in groupData">
        <div
          class="m_groupBox"
          v-on:click="curSelectFunc(index)"
        >
          <div
            class="ms_check"
            v-if="isSingleCheck == '0'"
            v-on:click="checkSelectFunc(index)"
          >
            <div class="ckb_wrap">
              <!-- <input class="inp_ckb" type="checkbox" data-val="60" v-model="list.isChecked"><label></label> -->
              <i
                data-val="60"
                :class="list.isChecked ? 'inp_ckb_checkbox checked': 'inp_ckb_checkbox'"
              ></i>
            </div>
          </div>
          <div
            class="ms_info"
            v-bind:style="{border:index == groupData.length-1?'none':''}"
          >
            <div class="info_box">
              <!-- <label for="">{{isSingleCheck}}</label> -->
              <span class="name">{{list.name}}</span>
              <span
                v-if="list.personNum && showPeopleNum"
                class="subinfo"
              >（{{list.personNum}}{{$t('refer.unit.person')}}）</span>
              <span
                class="more"
                v-on:click="linkMore(index)"
                v-if="!list.leaf && list.children && list.children.length > 0 || (list.personNum !== 0 && showPeopleNum)"
              >
                <i class="m_more"></i>
              </span>
            </div>
          </div>
        </div>
      </template>

    </div>
  </div>
</template>

<script>
import { MessageBox } from "mint-ui";
import { publicGetAxios, getLoginInfo } from '../../assets/js/util/common.js';
import companySwitch from './companySwitch';
export default {
  name: "organizList",
  data() {
    return {
      searchInpVal: "",
      groupData: [],
      headList: [],
      listData: "",
      showPeopleNum: false,
      basicGetDataPram: {
        url: "",
        more: ""
      }, //接口基础配置
      onloadFun: null,
      curSelectFunc: "",
      checkSelectFunc: new Function(),
      title: "",
      departmentSelectes: [],
      isSingleCheck: 0, // 默认多选
      isMemeberChoose: 0, //是否人员选择 区分单选 （人员选择时直接进入下级，其他情况选择） 0-false 1-true
      selectPeople: "",
      groupId: "",
      headName: "",
      headChildList: []
    };
  },
  props: {
    propBind: {
      type: Object,
      defalut: {}
    },
    showSwitchCompany: {
      type: Boolean,
      default: false
    },
    companyInfo: {
      type: Object,
      default: null
    },
    type: {
      type: String,
      default: ''
    }
  },
  mounted() {

    const { selectPeople = "" } = this.GetRequest(window.location.href) || {};
    this.selectPeople = selectPeople
    // let url = '../static/data/memberList.json';
    const { nameType = '', groupId, listUrl, isSingleCheck, showPeopleNum = '', isMemeberChoose = "" } = this.propBind;
    // let propBind = this.propBind
    this.groupId = groupId;
    this.basicGetDataPram.url = listUrl;

    // 配置单选多选
    this.isSingleCheck = isSingleCheck == "0" ? isSingleCheck : ''

    // 配置是否显示人数
    this.showPeopleNum = showPeopleNum;

    // 配置是否人员选择
    this.isMemeberChoose = isMemeberChoose;

    // 按不同选项配置选择事件
    if (this.isSingleCheck == "0") {
      //多选
      if (this.isMemeberChoose == "1") {
        //人员选择
        this.curSelectFunc = this.onSelectToMember;
      } else {
        this.curSelectFunc = this.onSelect;
      }
    } else {
      // 单选
      if (this.isMemeberChoose == "1") {
        //人员选择
        // this.curSelectFunc = this.linkMore;
        this.curSelectFunc = this.onSelectToMember;
      } else {
        this.curSelectFunc = this.onSelect;
      }
    }

    if (this.isMemeberChoose == "1") {
      this.checkSelectFunc = this.curSelectFunc;
      this.curSelectFunc = new Function();
    } else {
      this.checkSelectFunc = new Function();
    }

    if (this.isMemeberChoose) {
      //获取公司名称
      if (this.companyInfo) {
        this.headList.push(this.companyInfo)
      }
    }

    if (window.pageConfig) { // 配置项
      this.headName = this.$t(window.pageConfig.viewTitle);
    }

    this.getInitHeadList(this.propBind);
  },
  methods: {
    // 获取初始层级列表
    getInitHeadList: function (temp = {}) {
      const { groupId = "" } = temp
      let url = this.basicGetDataPram.url;
      if (temp.isSearch) {
      } else {
        if (groupId && groupId != -1) {
          // 显示当前id下的层级
          //   url = url + "&id=" + groupId;

          // TODO 获取登陆人所在部门路径
          // var url2;
          // if (this.type === 'user') {
          //   url2 = "/foundation/department/common/getDepartmentFullPathInfoForUser.action?selectPeople=" + this.selectPeople;
          // } else {
          //   url2 = "/foundation/department/common/getDepartmentFullPathInfo.action";
          // }

          // // 当前登录人层级
          // publicGetAxios(
          //   {
          //     url: url2,
          //     type: "get"
          //   },
          //   function (res) {
          //     if (res.success) {
          //       this.headList = res.data;
          //     }
          //   }
          // );
          this.getOrganizData(url, groupId);
        } else {
          this.getOrganizData(url, -1);
        }
      }
    },

    // 切换公司
    switchCompany: function (list) {
      this.headList = [{ id: list.id, name: list.shortName }];
      this.getInitHeadList(this.propBind);
    },

    // 获取层级列表
    getOrganizData: function (url, groupId, callback) {
      let type = this.type;
      const { companyInfo = {} } = this;
      if (!['department', 'position', 'curDepartment'].includes(type)) {
        type = 'department';
        if (!callback) groupId = null;
      }
      // 层级列表
      if (companyInfo.id !== groupId && groupId && ![1000, -1].includes(groupId)) {
        if (type === 'curDepartment') type = 'department';
        url = `${url}&${type}Id=${groupId}`;
      }
      publicGetAxios(
        {
          url: url,
          type: "get"
        },
        (res) => {
          if (res) {
            let data = res.data;
            let { children: result } = data;
            this.updateGroup(result, callback);
            if (this.type === 'curDepartment') result.shift();
            this.headChildList.push(result);
          }
        }
      );
    },

    // 更新数据
    updateGroup: function (data, callback) {
      let result = data;
      if (this.isMemeberChoose == 1) {
        result = this.depPeoSelectData(
          this.$store.state.curChooseList,
          // this.departmentSelectes,
          result
        );
      } else {
        result = this.redrawSelectData(
          this.$store.state.curChooseList,
          result
        );
      }
      this.groupData = result;
      if (
        this.propBind.updatePanel &&
        typeof this.propBind.updatePanel == "function"
      ) {
        this.propBind.updatePanel();
      }

      if (callback && typeof callback == "function") {
        callback(result);
      }
    },

    // 返回主页
    returnHome: function () {
      this.$emit("returnHome", "");
    },

    // 选择人员
    onSelectToMember: function (index) {
      let list = this.groupData[index];
      if (list.isChecked) {
        list.isChecked = false;
        this.departmentSelectes[index] &&
          delete this.departmentSelectes[index];
        // let arr = this.$store.state.curChooseList;
        // this.$store.state.curChooseList = this.filterArr(arr, list);
      } else {
        list.isChecked = true;
        this.departmentSelectes.push(list);
        // this.$store.state.curChooseList.push(list);
      }
      this.$set(this.groupData, index, list); //更新数据
      // 人员刷新 多选
      if (this.isMemeberChoose == "1" && this.isSingleCheck == "0") {
        this.redrawListData({ list: list, unFresh: true, isAll: true });
      }

      this.$store.state.curChooseList = this.uniqArr(
        this.$store.state.curChooseList
      ); // 数组去重
      // this.$store.commit("", 'curChooseList', this.$store.state.curChooseList);
    },

    // 选择事件
    onSelect: function (index) {
      var k = this.groupData[index];
      if (this.isSingleCheck == "0") {
        // 多选
        this.multiSelect(index);
      } else {
        // 单选
        this.singleSelect(k);
      }
    },

    // 多选
    multiSelect: function (index) {
      let list = this.groupData[index];
      let { children: child } = list;
      let arr = this.$store.state.curChooseList;
      if (!child) child = []
      // childLength = child.split(',').length;
      if (list.isChecked) {
        // 未选中
        // if (event.target.checked) { //选中
        list.isChecked = false;

        let curArr = child ? child.concat(list) : [];
        arr = this.filterArr(arr, curArr);
      } else {
        //选中
        list.isChecked = true;
        arr.push(list);
        arr = arr.concat(child);
      }
      this.$set(this.groupData, index, list); //更新数据
      arr = this.uniqArr(arr); // 数组去重
      // this.$emit("updateChoooseList", arr);
      this.$store.commit("updateData", {
        name: "curChooseList",
        value: arr
      }); // 更新选中列表
    },

    // 单选
    singleSelect: function (obj) {
      this.curChooseList = obj;
      // this.$store.commit("updateData",{name: 'curChooseList', value: this.curChooseList}); // 更新选中列表
      this.$emit("updateChoooseList", this.curChooseList);
    },

    // 面包屑链接
    headLineLink: function (index) {
      let data;
      let length = this.headList.length;
      if (index == -1) {
        data = {
          id: -1,
          name: this.$t('refer.headName.department')
        };
        return;
      } else {
        data = this.headList[index];
      }
      if (this.headList && index < length) {
        this.headList.splice(index + 1, length - index);
        this.headChildList.splice(index + 2, length - 1 - index)
      } else {
        this.headList.splice(1, 1);
        this.headChildList.splice(1, 1);
      }

      let listIndex = index + 1;
      if (this.propBind.tit) listIndex = index;
      const list = this.headChildList[listIndex] || [];

      //查层级
      this.updateGroup(list, () => {
        this.redrawListData({ list: this.headList[this.headList.length - 1], isHead: true });
      })
    },

    // 更新列表数据
    // list, unFresh, isAll
    redrawListData: function (params) {
      let propBind = this.propBind ? this.propBind : "";
      if (propBind && typeof propBind.linkcallBack == "function") {
        let callback = propBind.linkcallBack;
        callback(params);
      }
    },

    // 树形菜单链接 more
    linkMore: function (index) {
      event.stopPropagation();
      let list = this.groupData[index];
      const { leaf, children: child = [] } = list;
      //查层级
      if (list.isChecked && !this.isMemeberChoose) {
        // 当前层级选中 不进入子集
        return;
      }
      let url = this.basicGetDataPram.url;
      this.redrawListData({ list: list });
      this.groupData = [];
      this.headList.push({
        text: list.name,
        id: list.id
      });
      this.refreshHeadList(list);
      this.updateGroup(child);
      child && this.headChildList.push(child)
    },

    // 刷新子集
    refreshChildList(url, list) {
      this.getOrganizData(url, list.id, function () {
        // 点击的添加到面包屑菜单中
        if (list.leaf) {
          //叶子节点
          // MessageBox('提示', '无子节点');
          return;
        } else {
          this.headList.push({
            text: list.name,
            id: list.id
          });

          if (this.isMemeberChoose == "1") {
            // 人员选择
            this.redrawListData({ list: list });
          }

          this.refreshHeadList(list);
        }
      });
    },

    // 更新子集
    refreshHeadList(list) {
      this.$set(this.headList, this.headList.length - 1, list); //更新数据
      setTimeout(function () {
        document.getElementById("breadMenu").scrollLeft = 99999; //滚动条最右边
      }, 100);
    }
  },
  components: {
    companySwitch
  }
};
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="less" scoped>
@import "../../assets/css/groupSubset.less";
</style>


// WEBPACK FOOTER //
// src/components/refer/organizList.vue