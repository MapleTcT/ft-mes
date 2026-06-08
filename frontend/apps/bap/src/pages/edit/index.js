// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
// import Zepto from 'zepto-node';
import Vue from 'vue';
import App from './editView';
// import router from '../../router';
import Vuex from 'vuex';
// import '../../router/index.js';
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
//     pagename: "pageedit",
//     clientType: "mobile",
//     mobileViewName:"edit__mobile__",
//     hideWebTitle :"false",
//     params: {
//         name: "编辑视图",
//         type: "flow",
//         allReadOnly:false
//     },
//     navBar: {
//         title: "编辑视图",
//            bgColor: "",
//            hidden: false
//     },
//     dealInfoUrl:"/wupin/wpApply/wpApply/dealInfo-list.action",
//     referenceCopy:"/wupin/wpApply/wpApply/ref.action",
//     layoutUrl: '/msService/ec/view/viewJson?view.code=wupin_1.0.0.00_wpApply_edit__mobile__&entity.code=wupin_1.0.0.00_wpApply', // 布局
//     layoutDataUrl: "/wupin/wpApply/wpApply/edit__mobile__?entityCode=wupin_1.0.0.00_wpApply&id=1130&__pc__=dGFzazM0Mnx3cEFwcGx5&dataJson=1",
//     submitUrl:"/wupin/wpApply/wpApply/edit__mobile__/submit?__pc__=dGFzazM0Mnx3cEFwcGx5&_bapFieldPermissonModelCode_=wupin_1.0.0.00_wpApply_WpApply&_bapFieldPermissonModelName_=WpApply&superEdit=false ",
//     workFlowUrl: "/msService/ec/workflow/getActiveRoot.action",
//     viewCode: "wupin_1.0.0.00_wpApply_edit__mobile__",
//     entityCode:"wupin_1.0.0.00_wpApply",
//     datagridKey:"wupin_wpApply_wpApply_edit_datagrids",
//     dataGridDataUrl:"/wupin/wpApply/wpApply/data-",
//     documentType:"wupin_wpApply_wpApply",
//     staffRef:"/foundation/staff/common/staffRefvue.action",
//     userRef:"/foundation/user/common/userRefvue.action",
//     userInfo: {
//         modelAlias: 'wpApply',
//         positions:[{positionLayRec:"1000",createPositionId:"1000",name:"1"}  ,{positionLayRec:"1011",createPositionId:"1011",name:"测试测试测试（测试当时多所多多多多）"} ],
//         createDepartmentId: "1000",
//         staff:{id:"1001",name:"liweimin"},
//         userid:"1001",
//         createTime:"2019-10-15 15:58:00"
//     }
// }


/* eslint-disable no-new */
window.vue = new Vue({
    el: '#app',
    // router,
    components: { App },
    template: '<App/>'
});


// WEBPACK FOOTER //
// ./src/pages/edit/index.js