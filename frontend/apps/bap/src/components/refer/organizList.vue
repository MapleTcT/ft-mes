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
    <div
      v-if="groupData && groupData.length > 0"
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
                v-if="list.peopleNum && showPeopleNum"
                class="subinfo"
              >（{{list.peopleNum}}人）</span>
              <span
                class="more"
                v-on:click="linkMore(index)"
                v-if="!list.leaf && list.child && list.child.length > 0 || (list.peopleNum !== '0' && showPeopleNum)"
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
      isMemeberChoose: 0 //是否人员选择 区分单选 （人员选择时直接进入下级，其他情况选择） 0-false 1-true
    };
  },
  props: ["propBind"],
  mounted() {
    let $this = this;
    // let url = '../static/data/memberList.json';
    let temp = $this.propBind ? $this.propBind : "";
    // let propBind = $this.propBind
    let nameType = temp.nameType ? temp.nameType : "";

    $this.basicGetDataPram.url = temp.listUrl;
    $this.basicGetDataPram.more = temp.listMoreUrl ? temp.listMoreUrl : "";

    // 配置单选多选
    $this.isSingleCheck =
      temp && (temp.isSingleCheck || temp.isSingleCheck == "0")
        ? temp.isSingleCheck
        : "";

    // 配置是否显示人数
    $this.showPeopleNum = temp && temp.showPeopleNum ? temp.showPeopleNum : "";

    // 配置是否人员选择
    $this.isMemeberChoose =
      temp && temp.isMemeberChoose ? temp.isMemeberChoose : "";

    // 按不同选项配置选择事件
    if ($this.isSingleCheck == "0") {
      //多选
      if ($this.isMemeberChoose == "1") {
        //人员选择
        $this.curSelectFunc = $this.onSelectToMember;
      } else {
        $this.curSelectFunc = $this.onSelect;
      }
    } else {
      // 单选
      if ($this.isMemeberChoose == "1") {
        //人员选择
        // $this.curSelectFunc = $this.linkMore;
        $this.curSelectFunc = $this.onSelectToMember;
      } else {
        $this.curSelectFunc = $this.onSelect;
      }
    }

    if ($this.isMemeberChoose == "1") {
      $this.checkSelectFunc = $this.curSelectFunc;
      $this.curSelectFunc = new Function();
    } else {
      $this.checkSelectFunc = new Function();
    }

    if ($this.isMemeberChoose) {
      //接口请求公司名称
      $this.getData({
        url: '/foundation/company/common/getCurrentCompanyData',
        type: 'get'
      }, function (res) {
        if (res.success) {
          var data = res.data;
          $this.headList = [{
            id: data.id,
            name: data.name
          }];
        }
      });
    }

    $this.getInitHeadList(temp);
  },
  methods: {
    // 获取初始层级列表
    getInitHeadList: function(temp) {
      const $this = this;
      let groupId = temp ? temp.groupId : "";
      let url = $this.basicGetDataPram.url;
      if (temp.isSearch) {
      } else {
        if (groupId && groupId != -1) {
          // 显示当前id下的层级
          //   url = url + "&id=" + groupId;
          // 当前登录人层级
          $this.getData(
            {
              url:
                "/foundation/department/common/getDepartmentFullPathInfo?",
              type: "get"
            },
            function(res) {
              if (res.success) {
                $this.headList = res.data;
              }
            }
          );
          $this.getOrganizData(url, groupId);
        } else {
          $this.getOrganizData(url, "-1");
        }
      }
    },
    // 获取层级列表
    getOrganizData: function(url, groupId, callback) {
      let $this = this;
      // 层级列表
      if (groupId) {
        url = `${url}&id=${groupId}`;
      }
      $this.getData(
        {
          url: url,
          type: "get"
        },
        function(res) {
          if (res.success) {
            let data = res.data;
            if ($this.isMemeberChoose == 1) {
              data.result = $this.depPeoSelectData(
                $this.$store.state.curChooseList,
                // $this.departmentSelectes,
                data.result
              );
            } else {
              data.result = $this.redrawSelectData(
                $this.$store.state.curChooseList,
                data.result
              );
            }
            $this.groupData = data.result;
            // if ($this.headList && $this.headList.length == 0) {
            // }
            if (
              $this.propBind.updatePanel &&
              typeof $this.propBind.updatePanel == "function"
            ) {
              $this.propBind.updatePanel();
            }

            if (callback && typeof callback == "function") {
              callback(data.result);
            }
          }
        }
      );
    },
    // 返回主页
    returnHome: function() {
      this.$emit("returnHome", "");
    },
    // 选择人员
    onSelectToMember: function(index) {
      let $this = this;
      let list = $this.groupData[index];
      if (list.isChecked) {
        list.isChecked = false;
        $this.departmentSelectes[index] &&
          delete $this.departmentSelectes[index];
        // let arr = $this.$store.state.curChooseList;
        // $this.$store.state.curChooseList = $this.filterArr(arr, list);
      } else {
        list.isChecked = true;
        $this.departmentSelectes.push(list);
        // $this.$store.state.curChooseList.push(list);
      }
      this.$set($this.groupData, index, list); //更新数据
      // 人员刷新 多选
      if ($this.isMemeberChoose == "1" && $this.isSingleCheck == "0") {
        $this.redrawListData({ list: list, unFresh: true, isAll: true });
      }

      $this.$store.state.curChooseList = $this.uniqArr(
        $this.$store.state.curChooseList
      ); // 数组去重
      // $this.$store.commit("", 'curChooseList', $this.$store.state.curChooseList);
    },
    // 选择事件
    onSelect: function(index) {
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
    multiSelect: function(index) {
      let $this = this;
      let list = this.groupData[index];
      let child = list.child;
      let arr = $this.$store.state.curChooseList;
      // childLength = child.split(',').length;
      if (list.isChecked) {
        // 未选中
        // if (event.target.checked) { //选中
        list.isChecked = false;

        let curArr = list.child ? list.child.concat(list) : [];
        arr = $this.filterArr(arr, curArr);
      } else {
        //选中
        list.isChecked = true;
        arr.push(list);
        arr = arr.concat(list.child);
      }
      this.$set($this.groupData, index, list); //更新数据
      arr = $this.uniqArr(arr); // 数组去重
      // $this.$emit("updateChoooseList", arr);
      this.$store.commit("updateData", {
        name: "curChooseList",
        value: arr
      }); // 更新选中列表
    },

    // 单选
    singleSelect: function(obj) {
      this.curChooseList = obj;
      // this.$store.commit("updateData",{name: 'curChooseList', value: this.curChooseList}); // 更新选中列表
      this.$emit("updateChoooseList", this.curChooseList);
    },

    // 面包屑链接
    headLineLink: function(index) {
      let $this = this,
        data;
      let length = $this.headList.length;
      if (index == -1) {
        data = {
          id: -1,
          name: "部门"
        };
      } else {
        data = $this.headList[index];
      }
      //查层级
      $this.getOrganizData($this.basicGetDataPram.url, data.id, function() {
        $this.redrawListData({ list: data, isHead: true });
        if ($this.headList && index < length) {
          $this.headList.splice(index + 1, length - index);
        } else {
          $this.headList.splice(1, 1);
        }
      });
    },

    // 更新列表数据
    // list, unFresh, isAll
    redrawListData: function(params) {
      let $this = this;
      let propBind = $this.propBind ? $this.propBind : "";
      if (propBind && typeof propBind.linkcallBack == "function") {
        let callback = propBind.linkcallBack;
        callback(params);
      }
    },
    // 树形菜单链接 more
    linkMore: function(index) {
      event.stopPropagation();
      let $this = this;
      let list = $this.groupData[index];
      //查层级
      if (list.isChecked && !$this.isMemeberChoose) {
        // 当前层级选中 不进入子集
        return;
      }
      let url = $this.basicGetDataPram.more
        ? $this.basicGetDataPram.more
        : $this.basicGetDataPram.url;

      if (!list.leaf && list.child && list.child.length > 0) {
        $this.refreshChildList(url, list);
      } else {
        $this.redrawListData({ list: list });
        $this.groupData = [];
        $this.headList.push({
          text: list.name,
          id: list.id
        });
        $this.refreshHeadList(list);
      }
    },

    // 刷新子集
    refreshChildList(url, list) {
      const $this = this;
      $this.getOrganizData(url, list.id, function() {
        // 点击的添加到面包屑菜单中
        if (list.leaf) {
          //叶子节点
          // MessageBox('提示', '无子节点');
          return;
        } else {
          $this.headList.push({
            text: list.name,
            id: list.id
          });

          if ($this.isMemeberChoose == "1") {
            // 人员选择
            $this.redrawListData({ list: list });
          }

          $this.refreshHeadList(list);
        }
      });
    },

    // 更新子集
    refreshHeadList(list) {
      const $this = this;
      $this.$set($this.headList, $this.headList.length - 1, list); //更新数据
      setTimeout(function() {
        document.getElementById("breadMenu").scrollLeft = 99999; //滚动条最右边
      }, 100);
    }
  }
  // components: { groupSlideList }
};
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->

<style lang="less" scoped>
@import "../../assets/css/groupSubset.less";
</style>



// WEBPACK FOOTER //
// src/components/refer/organizList.vue