// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue';
import App from '@/components/refer/selectDetail';
import router from '@/router/staff';
import Vuex from 'vuex';
// import '@/router/index.js';
import '@/assets/js/flexible.js';
// import "babel-polyfill";
import Axios from 'axios'; //接口请求
Vue.config.productionTip = false;
Vue.prototype.$axios = Axios;
import Mint from 'mint-ui';
// import $ from 'zepto';
import util from '@/assets/js/util/util.js'; //引用公共函数
import cpicker from '@/components/picker/index'; //自定义选择控件
import '@/assets/css/mint-ui/style.css';
import '@/assets/css/reset.css';
import '@/assets/css/global.less';
import Es6Promise from 'es6-promise';
require('es6-promise').polyfill();
Es6Promise.polyfill();
Vue.use(Mint);
Vue.use(util);
Vue.use(cpicker);
Vue.use(Vuex);
Vue.prototype.$axios = Axios;
var $ = Zepto(window);
FastClick.attach(document.body);

window._PAGECONFIG = {
    name: '用户选择',
    placeholderText: '输入用户编码或名称',
    nameType: 1,
    isSingleCheck: 0, // 多选-0  单选-1
    navBar: {
        title: "用户选择",
        bgColor: ""
    },

};

// vuex 声明
const store = new Vuex.Store({
    state: {
        curChooseList: [], // 当前选中的list
        curPositionList: [],
        curDepartList: [],
        curShowPage: '' // 当前显示的页面
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
    actions: {

    },
    getters: {

    }
});

/* eslint-disable no-new */
window.vue = new Vue({
    el: '#app',
    store,
    router,
    components: { App },
    template: '<App/>'
});


// WEBPACK FOOTER //
// ./src/pages/user/index.js