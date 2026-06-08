<template>
  <div id="workflow">

    <!-- 审批 1 -->
    <div class="bg-white">
      <div class="mt20 list-space">

        <template v-if="sortByType && sortByType['1'] && (sortByType['1'].length > 1 || sortByType['1'][0].selectStaff != '0' || routerData.type == '7')">
          <div
            v-for="(item, index) in sortByType['1']"
            class="m-work-list"
          >
            <selectPersonnel
              v-on:update="updatePerson"
              :params="{personnelTitle: item.name, chooseSwitch: item.selectStaff, changeData: item}"
            ></selectPersonnel>
            <div class="ms_check">
              <div class="ckb_wrap">
                <input
                  :ref="item.code"
                  type="radio"
                  class="inp_ckb"
                  name="router"
                  v-on:change="changeSelectData(item, 'submit')"
                >
                <label for="checkList"></label>
              </div>
            </div>
            <p v-if="sortByType['1'] && sortByType['1'].length > 1 && index != sortByType['1'].length-1" class="m-border"></p>
          </div>
        </template>
        
        <!-- 循环会签 -->
        <template v-if="routerData && routerData.type == '7'"> 
        <p class="m-border"></p>
        <div class="m-work-list">
          <selectPersonnel
            v-on:update="updatePerson"
            :params="{personnelTitle: '转发'}"
          ></selectPersonnel>
          <div class="ms_check">
            <div class="ckb_wrap green">
              <input
                :ref="item.code"
                type="checkbox"
                class="inp_ckb"
              >
              <label for="checkList"></label>
            </div>
          </div>
        </div>
        </template>

      </div>
    </div>

    <!-- 驳回 2 -->
    <div
      v-if="sortByType && sortByType['2'] && pendingId && (sortByType['2'].length > 1 || sortByType['2'][0].selectStaff != '0')"
      class="bg-white"
    >
      <div class="mt20 list-space">

        <div
          v-for="(item, index) in sortByType['2']"
          class="m-work-list"
        >
          <selectPersonnel
            v-on:update="updatePerson(item)"
            :params="{personnelTitle: item.name, chooseSwitch: item.selectStaff, changeData: item}"
          ></selectPersonnel>
          <div class="ms_check">
            <div class="ckb_wrap">
              <input
                :ref="item.code"
                type="radio"
                class="inp_ckb"
                name="router"
                v-on:change="changeSelectData(item, 'reject')"
              >
              <label for="checkList"></label>
            </div>
          </div>
          <p v-if="sortByType['2'] && sortByType['2'].length > 1 && index != sortByType['2'].length-1" class="m-border"></p>
        </div>
        
      </div>
    </div>

    <!-- 作废 3 -->
    <div v-if="sortByType && sortByType['3'] && pendingId" class="bg-white">
      <div class="mt20 list-space">
        <div v-for="(item, index) in sortByType['3']" class="m-work-list">
          <selectPersonnel
            v-on:update="updatePerson"
            :params="{personnelTitle: item.name, chooseSwitch: item.selectStaff, changeData: item}"
          ></selectPersonnel>
          <div class="ms_check">
            <div class="ckb_wrap">
              <input
                :ref="item.code"
                type="radio"
                class="inp_ckb"
                name="router"
                v-on:change="changeSelectData(item, 'cancel')"
              >
              <label for="checkList"></label>
            </div>
          </div>
          <p v-if="sortByType['3'] && sortByType['3'].length > 1 && index != sortByType['3'].length-1" class="m-border"></p>
        </div>
      </div>
    </div>

    <!-- 通知 4-->
    <div
      v-if="sortByType && sortByType['4']"
      class="bg-white"
    >
      <div class="mt20 list-space">

        <div
          v-for="(item, index) in sortByType['4']"
          class="m-work-list"
        >
          <selectPersonnel
            v-on:update="updatePerson"
            :params="{personnelTitle: item.name, chooseSwitch: item.selectStaff, changeData: item}"
          ></selectPersonnel>
          <div class="ms_check">
            <div class="ckb_wrap green">
              <input
                :ref="item.code"
                type="checkbox"
                class="inp_ckb"
                name="router"
                v-on:change="changeSelectData(item, 'notice')"
              >
              <label for="checkList"></label>
            </div>
          </div>
          <p v-if="sortByType['4'] && sortByType['4'].length > 1 && index != sortByType['4'].length-1" class="m-border"></p>

        </div>

      </div>
    </div>

    <div v-if="showComment" class="m-comment">
      <!-- <textarea :disabled="dealSet == '2'?true:false" ref="comments" :placeholder="dealSet == '2'?'':'请填写处理意见...'" id="workflow_comments" name="workFlowVar.comment"></textarea> -->
      <textarea ref="comments" placeholder="请填写处理意见..." id="workflow_comments" name="workFlowVar.comment"></textarea>      
    </div>

    <div v-if="routerData && routerData.outgoingTransitions" class="m_foot">
      <a
        v-on:click="handelBtnClick('save')"
        class="m_botton btn_save"
      >保存</a>
      <a
        v-if="sortByType && sortByType['2'] && pendingId"
        v-on:click="handelBtnClick('reject')"
        class="m_botton btn_reject"
      >驳回</a>
      <a
        v-on:click="handelBtnClick('submit')"
        class="m_botton btn_submit"
      >提交</a>
    </div>

  </div>
</template>

<script>
import AsyncValidator from 'async-validator';
import selectPersonnel from "@/components/selectPersonnel";
import { Toast, Indicator } from 'mint-ui';
export default {
  name: "workFlow",
  props: ["params"],
  data() {
    return {
      sortByType: {}, // 按迁移线类型分类
      deploymentId: this.params.deploymentId, // 流程id
      pendingId: this.params.pendingId, // 待办id
      routerData: {},
      selectData: [], // 当前选中的数据
      routeCode: "",
      rejectCode: "",
      noticeCode: [],
      showComment: false,
      cancelCode: "",
      commentText: "",
      dealSet: ""
    };
  },
  mounted() {
    var $this = this;

    // 请求路由接口
    $this.getData(
      {
        url: $this.params.url
      },
      function(data) {
        console.log(data);
        $this.routerData = data;
        $this.dealSet = data.dealSet;
        $this.getRoutes($this.routerData);
        $this.$nextTick(function(){
          $this.showComment = true; // 展示意见框
          if ($this.dealSet == "2") { //禁填意见之后隐藏意见框
            $this.showComment = false;
          }
          let outgoingTransitions = $this.routerData.outgoingTransitions;
          if (outgoingTransitions && outgoingTransitions.length == 1) {
            if ($this.$children[0]) {
              const changeDataCode = $this.$children[0].params.changeData.code;
              $this.autoCheckRouter(changeDataCode); 
            }
          }        
        })
      }
    );
  },
  methods: {
    // 获取路由配置信息 显示按钮
    getRoutes: function(routeData) {
      const $this = this;
      // if (!routeData || JSON.stringify(routeData) === '{}') return false;
      const { outgoingTransitions, loopCountersign } = routeData;
      const taskType = routeData.type;
      let sortTemp = {};
      if (outgoingTransitions && outgoingTransitions.length > 0) {
        outgoingTransitions.forEach(item => {
          const { type } = item;
          if (!sortTemp[type]) sortTemp[type] = [];
          sortTemp[type].push(item);
        });
      }
      $this.sortByType = sortTemp;
      const types = Object.keys($this.sortByType);
      const con = [];
      const reject = [];
      types.forEach(type => {
        const data = $this.sortByType[type];
        // const temp = $this.getRouteDomByType(type, data, taskType);
        if (
          type.toString() === "1" ||
          type.toString() === "4" ||
          type.toString() === "3"
        ) {
          // 普通线、通知线、作废线
          // con.push(temp);
        } else if (type.toString() === "2") {
          // 驳回线
          // reject.push(temp);
        }
        switch (type) { //有按钮的路由添加默认值
          case "1": // 普通迁移线
            if (data && (data.length == 1 && data[0].selectStaff == '0' && taskType.toString() != '7')) {
              $this.routeCode = data[0].code;
            }
            break;
          case "2": // 驳回线
            if (data && (data.length == 1 && data[0].selectStaff == '0')) {
              $this.rejectCode = data[0].code;
            }
            break;
          case "3": // 作废线
          case "4": // 通知线
            break;
          default:
            break;
        }
      });

      if (
        taskType.toString() === "7" &&
        loopCountersign &&
        loopCountersign.toString() === "2"
      ) {
        // 循环会签
        // const { loopChecked } = this.state;
      }
    },

    // 路由选择参数验证及组织
    routeParamValidate: function(operateType, status, lines, outcomeCode) {
      // const { dealRouteBtnEvent } = this.props;
      const $this = this;
      const mapJson = this.getMapJson(lines);
      const { dealSet } = $this.routerData;
      const { outcomeMapJson, requiredStaffError } = mapJson;
      let commentVal = $this.$refs.comments && $this.$refs.comments.value;
      if (requiredStaffError && requiredStaffError.length > 0) {
        $this.myToast(message);
        return;
      }

      // 校验意见框
      if(!$this.validatorComment(dealSet, commentVal)){
        return false;
      }

      const workFlowVar = {
        outcomeMapJson: JSON.stringify(outcomeMapJson),
        outcome: outcomeCode,
        outcomeDes: outcomeCode,
        comment: commentVal,
        activityName: $this.routerData.code,
        assignUser: outcomeMapJson[0].assignUser
      };
      const param = { workFlowVar };

      $this.submitData(operateType, status, param); // 提交
    },

    // 校验意见框
    validatorComment: function(dealSet, commentVal){
      const $this = this;
      let comments = $this.$refs.comments;
      if(comments&& comments.name && window.formData && window.formData[comments.name]){
        let commentName = comments.name;
        let commentRules = window.formData[commentName];
        commentRules = {
          pattern: commentRules[0].expression,
          message: commentRules[0].message
        };
        if(!new RegExp(commentRules.pattern).test(commentVal)){
          $this.myToast(commentRules.message.replace(/<[^>]+>/g, ''));  
          return false;
        }
      }

      if(dealSet == "1" && (!commentVal ||commentVal == "")){
        $this.myToast("请先填写处理意见!");
        return false;
      }

      return true;

    },

    // 获取迁移线的路由参数信息
    getMapJson: function(lines) {
      const { outgoingTransitions } = this.routerData;
      const outcomeMapJson = [];
      const requiredStaffError = []; // 指定选中人员必填报错
      if (lines && lines.length > 0) {
        for (let j = 0; j < lines.length; j += 1) {
          const tag = lines[j];
          for (let i = 0; i < outgoingTransitions.length; i += 1) {
            const item = outgoingTransitions[i];
            const { code, name, selectStaff, requiredStaff, type } = item;
            if (code === tag) {
              const info = {
                type: type.toString() !== "4" ? "normal" : "notification",
                dec: name,
                outcome: code
              };
              if (selectStaff.toString() !== "0") {
                // const assignUser = "1003,"; // 指定人员
                const assignUser = ""; // 指定人员
                info.assignUser = assignUser;
              }
              if(item.members && item.members.length > 0){
                let memberTemp = [];
                for (let j = 0; j < item.members.length; j++) {
                  const element = item.members[j];
                  element.user && memberTemp.push(element.user.id);
                }
                memberTemp = memberTemp.join(",");
                info.assignUser = `${info.assignUser}${memberTemp}`;
              }
              // 指定人员必填
              if (requiredStaff && info.assignUser === '') {
                  const errorStr = `路由的指定人员不能为空！`;
                  requiredStaffError.push(errorStr);
              }
              outcomeMapJson.push(info);
              break;
            }
          }
        }
      }
      return { outcomeMapJson, requiredStaffError };
    },

    //自动勾选路由
    autoCheckRouter: function(changeDataCode) {
      const $this = this;
      let code = '';
      let outgoingTransitions = $this.routerData.outgoingTransitions;
      let type = '';
      if (outgoingTransitions && outgoingTransitions.length > 0) {
        outgoingTransitions.forEach(item => {
          if (item.code == changeDataCode) {
            if (item.type == 1) {
              type = "submit";
            } else if (item.type == 2) {
              type = "reject";
            } else if (item.type == 3) {
              type = "cancel";
            } else if (item.type == 4) {
              type = "notice";
            }
            code = item.code;            
            $this.$refs[code][0].checked = true;
            $this.changeSelectData(item, type);            
          }
        });
      }
    },

    //删除或者添加路由人员之后，勾选路由状态变化
    updatePerson: function(res, item) { 
      const $this = this;
      const personList = res;
      const changeDataCode = item.code;
      if ((personList && personList.length > 0) || $this.routerData.outgoingTransitions.length == 1) {
        $this.autoCheckRouter(changeDataCode);
      } else {
        $this.$refs[changeDataCode][0].checked = false;
        $this.whetherEmptyRoutVal();
      }
    },

    //判断是否清空默认路由的值
    whetherEmptyRoutVal: function() {
      const $this = this;
      const taskType = $this.routerData.type;
      const types = Object.keys($this.sortByType);
      types.forEach(type => {
        const data = $this.sortByType[type];
        switch (type) {
          case "1": // 普通迁移线
            if (!(data && (data.length == 1 && data[0].selectStaff == '0' && taskType.toString() != '7'))) {
              $this.routeCode = '';
            }
            break;
          case "2": // 驳回线
            if (!(data && (data.length == 1 && data[0].selectStaff == '0'))) {
              $this.rejectCode = '';
            }
            break;
          case "3": // 作废线
            $this.cancelCode = '';
            break;
          case "4": // 通知线
            break;
          default:
            break;
        }
      });
    },
 
    // 选中的数据
    changeSelectData: function(data, type) {
      const $this = this;
      if (event) {
        var checkFlag = event.target.checked;        
      }
      let selectData = this.selectData;
      if (type != "notice") {
        $this.whetherEmptyRoutVal();
      }
      switch (type) {
        case "submit": //1审批
          $this.routeCode = data.code;
          selectData = [data];
          break;
        case "reject": //2驳回
          $this.rejectCode = data.code;
          selectData = [data];
          break;
        case "cancel": //3作废
          $this.cancelCode = data.code;
          selectData = [data];
          break;
        case "notice": //4通知
          if(!checkFlag){
            $this.noticeCode = $this.deleteArryItem($this.noticeCode, data);
          }else{
            $this.noticeCode.push(data.code);
          }
          selectData.push(data);
          break;
        default:
          break;
      }

      this.selectData = selectData;
    },

    handelBtnClick: function(type) {
      const operateType = type;
      const $this = this;
      const taskType = $this.routerData.type;
      // const { dealRouteBtnEvent, flowRootData } = this.props;

      // notification.destroy(); // 清除错误信息
      const selectMembers = $this.selectData;
      switch (operateType) {
        case "save":
          const { dealSet } = $this.routerData;        
          let commentVal = $this.$refs.comments && $this.$refs.comments.value;
          //校验意见框
          if (commentVal) {
            if(!$this.validatorComment(dealSet, commentVal)){
              return false;
            }
          }
          const workFlowVar = {
            comment: commentVal,
          };
          const param = { workFlowVar };
          $this.submitData("save", operateType, param); // 提交
          break;
        case "submit":
          if (!$this.routeCode && !$this.cancelCode && taskType && taskType.toString() !== "5") {
            // 除通知活动其他活动必须有一条出去的迁移线;
            $this.myToast("路由未做选择！");
            $this.routerToTop();
          } else if ($this.cancelCode && selectMembers && selectMembers[0].type == 3) {//作废
            const lines = [$this.cancelCode];
            this.routeParamValidate("submit", "cancel", lines, $this.cancelCode);
          } else {
            let lines = [];
            if (taskType.toString() !== "5") {
              lines = [$this.routeCode, ...$this.noticeCode];
            } else {
              lines = ["通知"];
            }
            this.routeParamValidate("submit", operateType, lines, $this.routeCode);
          }
          break;
        case "reject": //驳回
          if (!$this.rejectCode) {
            $this.myToast("驳回路由未做选择！");
            $this.routerToTop();
          } else {
            let lines = [];
            if ($this.noticeCode.length > 0) {
              lines = [$this.rejectCode, ...$this.noticeCode];
            } else {
              lines = [$this.rejectCode];
            }
            this.routeParamValidate("submit", operateType, lines, $this.rejectCode);
          }
          break;
        case "cancel": //作废
          const lines = [$this.cancelCode];
          this.routeParamValidate("submit", operateType, lines, $this.cancelCode);

          break;
        case "attention": //关注
          break;
        default:
          break;
      }
    },

    //将路由滚动到页面顶部
    routerToTop: function() {
      document.getElementById('workflow').scrollIntoView();
    },

    // 保存数据
    saveData: function() {
      const $this = this;
      this.onSaveEvent && new Function(this.onSaveEvent)();
    },

    // 提交数据
    submitData: function(operateType, status, workParams) {
      const $this = this;
      if (operateType == 'save') {
        $this.$parent && $this.$parent.$refs.editForm && $this.$parent.$refs.editForm.submitData({status: status, operateType: operateType, saveCommentParams:workParams}); // 提交        
      } else {
        $this.$parent && $this.$parent.$refs.editForm && $this.$parent.$refs.editForm.submitData({status: status, operateType: operateType, workParams:workParams}); // 提交        
      }
    },
    
    // 删除元素
    deleteArryItem(list,data){
      if(list && list.length > 0){
        for (let i = 0; i < list.length; i++) {
          const element = list[i];
          if(element == data.code){
            list.splice(i,1);
            break;
          }
        }
      }
      return list;
      
    }
  },

  //引用组件
  components: { selectPersonnel }
};
</script>
<!-- Add "scoped" attribute to limit CSS to this component only -->

<style lang="less" scoped>
@import "../../assets/css/workFlow.less";
</style>



// WEBPACK FOOTER //
// src/components/workFlow/workFlow.vue