window.importCommonJs || loadScript("/msService/WTS/commonJs/loader");
importCommonJs("wtsCommon");
//计划body.js
var tabName_new = "新增活动"; //页签名称
var tabName_action = "作业活动";
var tabName_non = "未定义";

var parentTabId = 'tabs-6';// 页签父级tab的布局id

//页签从0开始计算
var tabIndex = 0;

//记录页签的id 与iframe的序号
var tab$iframe = new Map();

 //打开类型--
var openType = 'iframe';

var actionSrc = "/msService/workAppointment/workAction/workAction/workActionView?"+"openType=" + openType;


  //页面初始化
  function onLoad() {
    setTimeout(() => {
        //判断是否需要回显数据
        var workPlanId = ReactAPI.getParamsInRequestUrl().id;
        var actions;
        if (workPlanId) {//查询是否需要回显数据
            //--查询关联活动的信息
            var result = ReactAPI.request({
                type: "get",
                data: { planId: workPlanId },
                url: "/msService/workAppointment/workPlan/workPlan/getActionsByPlanId",
                async: false
            });
            if (result.code == 500) {
                ReactAPI.showMessage("f", "作业活动加载失败,请联系系统管理员！");
                return false;
            } else {
                actions = result.data
            }
        }

        if (workPlanId && actions.length > 0) {//回显数据
            //回显
           var tabIds=  actionEcho(actions);
            //选中第一个页签
           ReactAPI.Layout.switchTabTo(tabIds[0]);
        }
    });
}
