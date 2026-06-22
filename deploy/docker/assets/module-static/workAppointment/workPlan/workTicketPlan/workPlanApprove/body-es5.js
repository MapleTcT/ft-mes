window.importCommonJs || loadScript("/msService/WTS/commonJs/loader");
importCommonJs("wtsCommon");
//计划body.js
var tabName_new = "新增活动"; //页签名称
var tabName_action = "作业活动";
var tabName_non = "未定义";

var parentTabId = 'tabs-6'; // 页签父级tab的布局id

//页签从0开始计算
var tabIndex = 0;

//记录页签的id 与iframe的序号
var tab$iframe = new Map();

//打开类型--
var openType = 'iframe';

var actionSrc = "/msService/workAppointment/workAction/workAction/workActionView?" + "openType=" + openType;

//页面初始化
function onLoad() {
    //pageSet--页面字段设置
    pageSet();
    setTimeout(function () {
        //判断是否需要回显数据
        var workPlanId = ReactAPI.getParamsInRequestUrl().id;
        var actions;
        if (workPlanId) {
            //查询是否需要回显数据
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
                actions = result.data;
            }
        }

        if (workPlanId && actions.length > 0) {
            //回显数据
            //回显
            var tabIds = actionEcho(actions);
            //选中第一个页签
            ReactAPI.Layout.switchTabTo(tabIds[0]);
        }
    });
}

function pageSet() {
    for (component in ReactAPI.getComponentAPI()) {
        if (component != 'SupDataGrid' && component != "Label" && component != "Map" && component != "Picture") {
            //表格、标签、图片、地图图层不处理
            var fields = ReactAPI.getComponentAPI()[component]; //该组件的所有字段
            for (var key in fields) {
                if (key != "APIs") {
                    //只读
                    fields.APIs(key).setReadonly(true);
                    //不必填
                    fields.APIs(key).setRequired(false);
                    ReactAPI.getComponentAPI('Label').APIs(key).setNullableStyle(false);
                }
            }
        }
    }
}