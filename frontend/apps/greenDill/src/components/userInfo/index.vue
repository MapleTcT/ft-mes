<template>
  <div
    :style="showOrHidePanel()"
    class="m-user-info list-bt list-space list-flex"
  >
    <span class="ml_name">{{info.userName}}</span>
    <span class="ml_time">{{info.createTime}}</span>
    <template>
      <label
        v-if="info.position"
        id="selectPositionShow"
      >{{info.position}}</label>
      <label
        v-else
        id="selectPositionShow"
        @click="showPickPos"
      >{{pickPos.value}}</label>
      <input
        type="hidden"
        id="selectPositionGet"
        value=""
      >
      <a
        v-if="showUserPickpos && !isReadOnly"
        class="m_dropdown"
        @click="showPickPos"
      >
      </a>
    </template>
    <custom-picker
      v-if="showUserPickpos && !isReadOnly"
      :name="pickPos.name"
      :isShow="pickPos.isShow"
      :slots="pickPos.slots"
      :value="pickPos.value"
      v-on:pickercallback="pickerPosEvent"
    ></custom-picker>
  </div>

</template>

<script>
import { Navbar, TabItem } from 'mint-ui';
import { publicGetAxios } from '@/assets/js/util/common.js';
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import './index.less'

export default {
  name: "UserInfo",
  data() {
    return {
      formParams: [],
      selected: 1,
      layOutList: [],
      info: {},
      createTime: '2020-01-02',
      showUserPickpos: '',
      pickPos: {
        name: "pickPos",
        isShow: false,
        slots: [{
          flex: 1,
          values: []
        }],
        value: ""
      },
      isReadOnly: false,
    };
  },
  props: ["params"],
  methods: {
    getUserInfo(resData) {
      const $this = this;
      const { interfaceApi = {}, viewCode } = window.pageConfig || {};
      let time = resData && resData.createTime ? new Date(resData.createTime) : new Date();
      $this.createTime = _commonJs.getDateFormat("YMD_HM", time);
      let urlParams = $this.GetRequest(window.location.href);
      let url;
      if (urlParams.id) {
        url = interfaceApi.editStates + `?viewCode=${viewCode}&id=${urlParams.id}`;
      } else {
        url = interfaceApi.editStates + `?viewCode=${viewCode}`;
      }
      publicGetAxios({
        url: url,
        type: "get",
      }, function (result) {
        let data = result.data;
        let staff = data.creatorInfo.staff;
        $this.info = {
          userName: staff && staff.name,
          staffId: staff && staff.id,
          position: data.creatorInfo.position && data.creatorInfo.position.name,
          createTime: $this.createTime
        };
        if (data.creatorInfo.positions && data.creatorInfo.positions.length > 0) {
          $this.isShowMorePos(data.creatorInfo.positions);
          // 多岗位初始默认值
          if (!data.creatorInfo.position && !urlParams.id) {
            $this.info.positions = data.creatorInfo.positions[0];
          }
        }
      });
    },

    //多岗位逻辑 
    isShowMorePos(positions) {
      if (positions.length <= 1) {
        this.showUserPickpos = false;
        this.pickPos.value = positions[0] ? positions[0].name : '';
      } else {
        this.showUserPickpos = true;
        let slots = [];
        for (let i = 0; i < positions.length; i++) {
          const element = positions[i];
          slots.push(element.name);
        }
        this.pickPos.value = slots[0];
        this.pickPos.slots[0].values = slots;
        this.pickPos.positions = positions;
      }
    },

    // 展示用户多职位
    showPickPos() {
      if (this.isReadOnly) {
        return false;
      }
      this.pickPos.isShow = true;
    },

    // 选择对应职位
    pickerPosEvent(type, value, name) {
      var con = this.pickPos;
      const $this = this;
      if (type == "comfirm") {
        con.value = value;
        con.positions.forEach(item => {
          if (item.name == value) {
            $this.info.positions = item;
          }
        });
      }
      con.isShow = false;
    },

    showOrHidePanel() {
      if (this.params.isWorkflow) {
        return '';
      }
      return 'display:none';
    },

    getUserData() {
      return this.info;
    }
  },
  //引用组件
  components: {
  }
};
</script>


// WEBPACK FOOTER //
// src/components/userInfo/index.vue