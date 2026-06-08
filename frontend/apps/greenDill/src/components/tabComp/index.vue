<template>
  <div>
    <div
      ref="tabBar"
      class="m-tab"
    >
      <mt-navbar
        class="tab-bar"
        v-model="selected"
      >
        <mt-tab-item
          v-if="layOutList && layOutList.length > 1"
          v-for="(item, index) in layOutList"
          :class="textClassName"
          :id="index+1"
          :data-key="item.tabCode"
          :key="`${item.tabCode}_${updt}`"
          :ref="`tabBar_${index+1}`"
        >
          <div
            class="tab-item-txt"
            :title="item.tabName"
            @touchstart="gotouchstart"
            @touchmove="gotouchend"
            @touchend="gotouchmove"
          >{{item.tabName}}</div>
        </mt-tab-item>
      </mt-navbar>
    </div>
    <mt-tab-container
      v-model="selected"
      :swipeable="swipeable"
    >
      <mt-tab-container-item
        v-for="(item, index) in layOutList"
        :id="index+1"
        :key="`${item.tabCode}_${updt}`"
      >
        <editLayout
          :params="{...formParams}"
          :layOut="{...item, tabIndex: index+1}"
          :formData="formData"
          :ref="`layout_${item.tabCode}`"
          :isFileView="params.isFileView"
        ></editLayout>
      </mt-tab-container-item>
    </mt-tab-container>
  </div>
</template>

<script>
import { Navbar, TabItem } from 'mint-ui';
import { publicGetAxios } from '@/assets/js/util/common.js';
import _commonJs from "@/assets/js/itemList/index.js"; //引用模块进来
import editLayout from "@/components/form/editLayout.vue";
import Iform from "@/components/form/Iform";
import './index.less'

export default {
  name: "tabComp",
  data() {
    return {
      formParams: [],
      selected: 1,
      layOutList: [],
      requestParams: {},
      formData: {},
      tabBarRef: {},
      timeOutEvent: 0,
      textClassName: "tab-item",
      updt: 1, // 解决不更新值问题
      swipeable: true
    };
  },
  props: ['params'],
  beforeMount() {
    this.updateData();
  },
  mounted() {
  },
  updated() {
    const { tabBar } = this.$refs;
    const { scrollLeft } = tabBar;
    const tabItem = this.$refs[`tabBar_${this.selected}`];
    if (tabItem) {
      const { offsetLeft = 0, offsetWidth = 0 } = tabItem[0].$el;
      const itemLeft = (offsetLeft + offsetWidth);
      const winW = window.innerWidth;
      if (itemLeft > winW) {
        tabBar.scrollLeft = itemLeft - winW;
      } else if (offsetLeft < scrollLeft && scrollLeft !== 0) {
        tabBar.scrollLeft = offsetLeft;
      }
    }
  },
  methods: {
    gotouchstart(e) {
      let that = this;
      clearTimeout(this.timeOutEvent);//清除定时器
      this.timeOutEvent = 0;
      this.timeOutEvent = setTimeout(() => {
        //执行长按要执行的内容，
        this.myToast(e.target.title);
      }, 500);//这里设置定时
    },
    gotouchend() {
      clearTimeout(this.timeOutEvent);
      if (this.timeOutEvent != 0) {
        //onclick事件
      }
    },
    gotouchmove() {
      clearTimeout(this.timeOutEvent);//清除定时器
      this.timeOutEvent = 0;
    },
    switchTab(index = 1, callback = () => { }) {
      this.selected = index;
      this.$nextTick(() => {
        callback();
      });
    },
    updateData(params) {
      let temp = this.params;
      if (params) temp = params;
      this.layOutList = temp.layOutList;
      this.formParams = temp.params;
      this.formData = temp.formData;
      if (this.layOutList.length > 4) {
        this.textClassName = `${this.textClassName} maxWidth`
      }
      this.updt = Math.random();
    },

  },
  //引用组件
  components: {
    editLayout
  }
};
</script>


// WEBPACK FOOTER //
// src/components/tabComp/index.vue