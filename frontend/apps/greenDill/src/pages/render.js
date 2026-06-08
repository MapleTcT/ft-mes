// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
// import Zepto from 'zepto-node';
import Vue from "vue";
import Vuex from "vuex";
import "@/assets/js/flexible.js";
import "core-js/stable/index.js";
import Axios from "axios"; // 接口请求
Vue.config.productionTip = false;
import Mint from "mint-ui";
import util from "@/assets/js/util/util.js"; // 引用公共函数
import cpicker from "@/components/cellComp/picker/index"; // 自定义选择控件
import "@/assets/css/mint-ui/style.css";
import "@/assets/css/reset.css";
import "@/assets/css/global.less";
import Es6Promise from "es6-promise";
import vueI18n from "vue-i18n";
import localZh from "@/assets/translations/zh-cn.json";
import localEn from "@/assets/translations/en-us.json";
import {
  addTicket,
  getCurrentLanguage,
  getAuthentication
} from "@/assets/js/util/common.js";

const language = localStorage.getItem("language");
// ajax添加认证
Zepto.ajaxSettings.beforeSend = function(xhr) {
  const ticket = addTicket();
  if (ticket) {
    xhr.setRequestHeader("Authorization", ticket);
  }
  if(language) xhr.setRequestHeader("Accept-Language", language);
};


if (language) { 
  Axios.interceptors.request.use((config)=>{
    config.headers['Accept-Language'] = language;
    return config;
  })
}

if (process.env.NODE_ENV !== "production") {
  // localStorage.setItem('ticket','7c2e99a2dcd04535a384aa65f926c785')
}

require("es6-promise").polyfill();
Es6Promise.polyfill();
Vue.use(Mint);
Vue.use(util);
Vue.use(cpicker);
Vue.use(Vuex);
Vue.prototype.$axios = Axios;
var $ = Zepto(window);
FastClick.attach(document.body);
Vue.use(vueI18n);
getAuthentication();
const i18n = new vueI18n({
  // 默认语言
  locale: getCurrentLanguage(),
  // 引入对应的语言包文件
  messages: {
    zh: localZh,
    en: localEn,
  },
});

export const renderComp = (config = {}) => {
  window.vue = new Vue({
    el: "#app",
      components: { App: config.App },
      template: "<App/>",
      i18n,
      ...config
  })
}

export { i18n };


// WEBPACK FOOTER //
// ./src/pages/render.js