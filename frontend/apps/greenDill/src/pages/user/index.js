// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import App from "@/components/refer/selectDetail";
import router from "@/router/staff";
import Vuex from "vuex";
import { renderComp } from "../render.js";
// import '@/router/index.js';

window.pageConfig = {
  viewTitle: "user.select.title",
  type: "user",
  placeholderText: "user.enter.placeholder",
  nameType: 1,
  isSingleCheck: 0, // 多选-0  单选-1
  isRefer:true, // 是否为参照
  navBar: {
    title: "user.select.title",
    bgColor: ""
  }
};

// vuex 声明
const store = new Vuex.Store({
  state: {
    curChooseList: [], // 当前选中的list
    curPositionList: [],
    curDepartList: [],
    curShowPage: "" // 当前显示的页面
  },
  mutations: {
    updateData(state, params) {
      if (state && params) {
        state[params.name] = params.value;
      }
    },
    watchCurchooseList: function() {
      state.curChooseList != state.curChooseList;
    },
    clearAll(state) {
      state.curChooseList = [];
    }
  },
  actions: {},
  getters: {}
});

/* eslint-disable no-new */
renderComp({App, store, router});


// WEBPACK FOOTER //
// ./src/pages/user/index.js