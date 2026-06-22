window.importCommonJs || loadScript("/msService/WTS/commonJs/loader");
importCommonJs("wtsCommon");
//计划body.js
var tabName_new = "新增活动"; //页签名称
var tabName_action = "作业活动";
var tabName_non = "未定义";

//按钮样式
var sup_btn_del = "sup-btn-del"; //删除
var sup_btn_add = 'sup-btn-add'; //修改
//按钮前缀
var btn_perfix = "customSupBtn-";

//作业活动的新增_pc_
var operateCode = "workActionList_addAction_add_workAppointment_6.1.6.1_workAction_workActionList"; //菜单按钮编码
var action_pc_ = "";

//打开类型--
var openType = 'iframe';

var actionSrc = "";

function getActionSrc() {
    var powerCode = {};
    if (typeof ReactAPI !== "undefined" && ReactAPI.getPowerCode) {
        powerCode = ReactAPI.getPowerCode(operateCode) || {};
    }
    action_pc_ = powerCode[operateCode] || "";
    return "/msService/workAppointment/workAction/workAction/workActionEdit?_pc_=" + action_pc_ + "&openType=" + openType;
}

var parentTabId = 'tabs-6'; // 页签父级tab的布局id

//页签从0开始计算
var tabIndex = 0;

//记录页签的id 与iframe的序号
var tab$iframe = new Map();

//页面初始化
function onLoad() {
    actionSrc = getActionSrc();
    //作业时间默认值
    ReactAPI.getComponentAPI()["DatePicker"].APIs("workTicketPlan.workTime").setValue(new Date().getTime() + 24 * 60 * 60 * 1000);

    //回显
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
            var tabIds = actionEchoEdit(actions);

            //添加一个新增页面
            var tabId = ticketAddTabEdit(actionSrc);
            //自定义页签样式--添加--按钮--add--绑定事件
            var innerHtml = getInnerHtml(sup_btn_add, tabName_new, tabId);
            var thisTab = $("#" + tabId);
            thisTab.html(innerHtml.innerHtml);

            //选中第一个页签
            ReactAPI.Layout.switchTabTo(tabIds[0]);
        } else {
            //新增初始化

            //添加第一个页签
            var tabId = ticketAddTabEdit(actionSrc);
            var firstTab = tabId; //保存第一个页签id
            //自定义页签样式--添加--按钮--删除--绑定事件
            var innerHtml = getInnerHtml(sup_btn_del, tabName_action, tabId);
            var thisTab = $("#" + tabId);
            thisTab.html(innerHtml.innerHtml);

            //添加第二页签
            tabId = ticketAddTabEdit(actionSrc);
            //自定义页签样式--添加--按钮--新增--绑定按钮点击事件
            innerHtml = getInnerHtml(sup_btn_add, tabName_new, tabId);
            thisTab = $("#" + tabId);
            thisTab.html(innerHtml.innerHtml);

            //选中第一个页签--
            ReactAPI.Layout.switchTabTo(firstTab);
        }
    });
}

//回显数据
function actionEchoEdit(actions) {
    var tabIds = [];
    actions.forEach(function (element) {
        var tabId = ticketAddTabEdit(actionSrc + "&id=" + element.id);
        tabIds.push(tabId); //保存第一个页签id
        //自定义页签样式--添加--按钮--删除--绑定事件
        var innerHtml = getInnerHtml(sup_btn_del, tabName_action, tabId);
        var thisTab = $("#" + tabId);
        thisTab.html(innerHtml.innerHtml);
    });
    //用于 选中第一个页签--
    return tabIds;
}

/**
 *
 * @param id
 * @param title
 * @param url
 */
function ticketAddTabEdit(url) {
    //添加页签
    var id = "action-" + tabIndex;
    ReactAPI.Layout.addTab(parentTabId, {
        id: id,
        title: tabName_non,
        url: url
    });
    //隐藏默认删除按钮
    $('#' + id + ' > i ').remove();
    //记录页签的id 与iframe的序号
    tab$iframe.set(id, tabIndex);
    //序号++
    tabIndex++;
    //添加页签的点击事件-->新增页签-->添加按钮点击事件和页签点击事件合并！！！
    $("#" + id).on('click', function (node) {
        onclickTab(node); //点击页签事件
    });
    return id;
}

//根据页签获取ifream
function getIframeByTab(tabId) {
    return $("iframe").eq(tab$iframe.get(tabId));
}

/**
 *
 * @param thisClass 页签按钮样式 del/add/recover
 * @param tabText 页签内容
 * @param tabIndex  页签样式的id 用于绑定事件 ---> tabid+thisClass
 * @returns  id & innerHtml(页签内部的html)
 */
function getInnerHtml(thisClass, tabText, tabId) {

    // 根据页签获取ifream
    var thisIframe = getIframeByTab(tabId);

    switch (thisClass) {

        case 'sup-btn-del':
            supClass = 'sup-btn-del';
            // 标记--该页签所在的iframe--如果添加的是删除按钮,在编辑的页签--数据需要保存
            thisIframe.attr("saveTag", "true");
            break;
        case 'sup-btn-add':
            supClass = 'sup-btn-add';
            // 标记--该页签所在的iframe--如果添加的是添加按钮，初始化的页签--数据不需要保存
            thisIframe.attr("saveTag", "false");
            break;
        default:
            '';

    }

    var iId = btn_perfix + tabId;
    var innerHtml = '<i id=' + iId + ' tabId=' + tabId + ' btnType=' + supClass + ' class="sup-btn-icon ' + supClass + '"></i>' + tabText + "<style onload=\"tabbtnonload(\'" + iId + "\' )\"></style>";
    return { id: iId, innerHtml: innerHtml };
}

//添加点击事件
function tabbtnonload(iId) {
    $("#" + iId).on('click', function (node) {
        onclickTabBtn(node); //点击页签按钮事件
    });
}

//页签按钮点击事件
function onclickTabBtn(node) {
    var tabId = $(node.currentTarget).attr('tabid');
    var thisTab = $("#" + tabId);
    var thisIframe = getIframeByTab(tabId);

    //按钮标签id
    var btnId = node.currentTarget.id;
    //如果是删除
    if ($('#' + btnId).attr('btntype') == sup_btn_del) {
        //点击删除时提示--->删除后的作业活动，将不会保存，请确认！
        ReactAPI.openConfirm({
            message: "删除后的作业活动，将不会保存，请确认！",
            buttons: [{
                operatetype: "yes",
                text: "确认",
                type: "primary",
                onClick: function onClick() {
                    //有id--->调用删除接口
                    var thisActionId = thisIframe[0].contentWindow.ReactAPI.getParamsInRequestUrl().id;
                    if (thisActionId) {
                        var result = ReactAPI.request({
                            type: "get",
                            data: { actionId: thisActionId },
                            url: "/msService/workAppointment/workPlan/workPlan/deleteActionById",
                            async: false
                        });
                        if (result.code != 200) {
                            ReactAPI.showMessage('f', "作业活动删除失败！请联系管理员!");
                            return false;
                        }
                    }
                    //数据不保存
                    thisIframe.attr("saveTag", "false");
                    //页签隐藏-->只隐藏不删除--防止iframe序号改变
                    ReactAPI.Layout.hideTab(tabId);
                    ReactAPI.closeConfirm();
                }
            }, {
                operatetype: "cancel",
                text: "取消",
                onClick: function onClick() {
                    ReactAPI.closeConfirm();
                }
            }]
        });
    }
}

function onclickTab(node) {

    var tabId = node.currentTarget.id;
    var selectTab = tabId; //记录选中的页签--防止添加新页签后选中改变
    var thisTab = $("#" + tabId);
    var thisIframe = getIframeByTab(tabId);
    //按钮标签id
    var btnId = btn_perfix + tabId;
    //如果是添加
    if ($('#' + btnId).attr('btntype') == sup_btn_add) {
        //当前按钮改为作业活动页签
        var innerHtml = getInnerHtml(sup_btn_del, tabName_action, tabId);
        thisTab.html(innerHtml.innerHtml);
        //添加新的新增活动页签

        //添加新新增活动页签
        tabId = ticketAddTabEdit(actionSrc);
        //自定义页签样式--添加--按钮--新增--绑定按钮点击事件
        innerHtml = getInnerHtml(sup_btn_add, tabName_new, tabId);
        thisTab = $("#" + tabId);
        thisTab.html(innerHtml.innerHtml);
        //选中前一个页签--
        ReactAPI.Layout.switchTabTo(selectTab);
    }
}

function onSaveplan() {
    try {

        var submitType = ReactAPI.getOperateType();
        if (submitType == 'submit' || submitType == 'save') {
            //提交//保存

            ReactAPI.openLoading("处理中");
            //作业活动数据
            window.workActions = [];
            //需要保存的iframe 的 index
            //属性标记为"saveTag"---->"true"
            window.saveIndex = []; //需要保存的iframe索引
            window.readyIndex = []; //页面校验通过的iframe索引
            $('iframe').each(function (index) {
                if ($('iframe').eq(index).attr("saveTag") == "true") {
                    window.saveIndex.push(index);
                }
            });
            //触发子页面保存
            $('iframe').each(function (index) {
                if ($('iframe').eq(index).attr("saveTag") == "true") {
                    window.currentIndex = index;
                    $('iframe')[index].contentWindow.ReactAPI.submitFormData("save"); // onsave---> window.readyIndex.push(window.currentIndex)
                }
            });

            //存在页面 onsave 未触发 页面校验未通过
            var notReadyIndex = window.saveIndex.filter(function (o) {
                return !window.readyIndex.includes(o);
            });

            //校验失败则在相应页签显示标记
            window.saveIndex.forEach(function (thisIndex) {
                var thisIframe = $('iframe').eq(thisIndex);
                var id = $(thisIframe).parents(".layout-tab-wrap").attr("data-id");
                $('#' + id + ' > i.layout-tab-error').remove(); //防止重复标记
                if (notReadyIndex.includes(thisIndex)) {
                    $("[id='" + id + "']").append('<i class="layout-tab-error"></i>');
                }
            });
            if (notReadyIndex.length > 0) {
                ReactAPI.closeLoading();
                ReactAPI.showMessage('f', "请将作业活动信息补充完整！");
                return false;
            }

            var allData = {};
            //计划
            allData.workPlan = getRegularSaveData(window);
            allData.workPlan.id = ReactAPI.getParamsInRequestUrl().id;
            //活动
            allData.workActions = window.workActions;

            //调用--保存接口---》》》》
            console.log("作业计划保存接口---》》》》" + allData);
            //同步
            var result = ReactAPI.request({
                type: "post",
                data: allData,
                url: "/msService/workAppointment/workPlan/workPlan/workPlanUltraSubmit",
                async: false
            });

            if (result.code == 200) {
                //回刷---
                window.opener.ReactAPI.getComponentAPI()["SupDataGrid"].APIs("workAppointment_6.1.6.1_workPlan_workPlanList_workTicketPlan_sdg").refreshDataByRequst({
                    type: "POST",
                    url: "/msService/workAppointment/workPlan/workTicketPlan/workPlanList-query",
                    param: {
                        classifyCodes: "",
                        customCondition: {},
                        datagridCode: "workAppointment_6.1.6.1_workPlan_workPlanList",
                        permissionCode: "workAppointment_6.1.6.1_workPlan_workPlanList",
                        pageNo: 1,
                        paging: true,
                        pageSize: 10
                    }
                });

                setTimeout(function () {
                    ReactAPI.closeLoading();ReactAPI.openLoading("处理成功", '2');
                }, 1000);

                setTimeout(function () {
                    ReactAPI.closeLoading();
                    window.close(); //关闭页面
                }, 3000);
            } else {
                ReactAPI.closeLoading();
                console.log(result);
                ReactAPI.showMessage('f', "作业计划保存失败！请联系管理员!");
            }
            return false;
        }
    } catch (error) {
        console.log(error);
    }
}
