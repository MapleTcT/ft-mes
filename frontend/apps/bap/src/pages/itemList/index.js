// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue';
import App from './itemList';
import Vuex from 'vuex';
import '@/assets/js/flexible.js';
// import "babel-polyfill";
import Axios from 'axios'; //接口请求
Vue.config.productionTip = false;
import Mint from 'mint-ui';
import util from '@/assets/js/util/util.js'; //引用公共函数
import cpicker from '@/components/picker/index'; //自定义选择控件
import '@/assets/css/mint-ui/style.css';
import '@/assets/css/reset.css';
import '@/assets/css/global.less';
import Es6Promise from 'es6-promise';
import { Tree } from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';
import '@/assets/css/systemCodeTree.less';

require('es6-promise').polyfill();
Es6Promise.polyfill();
Vue.use(Mint);
Vue.use(util);
Vue.use(cpicker);
Vue.use(Vuex);
Vue.use(Tree);
Vue.prototype.$axios = Axios;
var $ = Zepto(window);
FastClick.attach(document.body);

// window._PAGECONFIG = {
//     pagename: "pagelist",
//     clientType: "mobile",
//     hideWebTitle :"false",
//     params: {
//         name: "列表视图",
//         type: "flow"
//     },
//     navBar: {
//         title: "列表视图",
//         bgColor: ""
//     },
//     newProcesses:[{"ONCLICK":"operateBarOnclickFoundation('/wupin/wpApply/wpApply/edit?deploymentId=1147&entityCode=wupin_1.0.0.00_wpApply&__pc__=c3RhcnQzNTJ8d3BBcHBseQ__')","NAME":"添加","ICONCLS":"cui-btn-add","PROCESSKEY":"wpApply","CODE":"start352"}],
//     openUrl:"/foundation/user/open-pending?entityCode=wupin_1.0.0.00_wpApply&__pc__=d3VwaW5fMS4wLjAuMDBfd3BBcHBseV9saXN0X3NlbGZ8",
//     newUrl:"/wupin/wpApply/wpApply/edit?entityCode=wupin_1.0.0.00_wpApply&__pc__=bGlzdF9hZGRfYWRkX3d1cGluXzEuMC4wLjAwX3dwQXBwbHlfbGlzdHw_&superEdit=true",
//     layoutUrl: '/msService/ec/view/viewJson?view.code=wupin_1.0.0.00_wpApply_list__mobile__&entity.code=wupin_1.0.0.00_wpApply', // 布局
//     layoutDataUrl:  {"query":"/wupin/wpApply/wpApply/list__mobile__-query?1=1"
//      , "pending":"/wupin/wpApply/wpApply/list__mobile__-pending?1=1"} ,
//     buttons:[{"ONCLICK":"wupin.wpApply.wpApply.list__mobile__.addlist__mobile__()","NAME":"新增","ICONCLS":"cui-btn-add","SEPARATENUM":"0","USEINMORE":"false","CODE":"list_add_add_wupin_1.0.0.00_wpApply_list"},{"ONCLICK":"customClick(event)","NAME":"添加","ICONCLS":"cui-btn-add","SEPARATENUM":"0","USEINMORE":"false","CODE":"list__mobile___add001_add_wupin_1.0.0.00_wpApply_list__mobile__"},,{"ONCLICK":"customClick(event)","NAME":"删除","ICONCLS":"cui-btn-del","SEPARATENUM":"0","USEINMORE":"false","CODE":"list__mobile___delete_del_wupin_1.0.0.00_wpApply_list__mobile__"}],
//     filterConfig: {
//         viewCode: "wupin_1.0.0.00_wpApply_list__mobile__",
//         modelAlias: 'wpApply'
//     }
// };
    

/* eslint-disable no-new */
window.vue = new Vue({
    el: '#app',
    // router,
    components: { App },
    template: '<App/>'
});


// WEBPACK FOOTER //
// ./src/pages/itemList/index.js