/**
 * 类型对照
 */
define(function (require, exports, module) {
    //大小定义
    var shape={
        width:"40",
        height:"40"
    };
    // 类型配置信息
    var typeControl = [
        {
            "name": getInternationalValByKey("js.ec.workflow.config.startEvent"),
            "tagName": "start",
            "tagBpmn": "StartEvent",
            "lowercase":"startevent",
            "icon": "/bap/static/flowEditH5/images/icon_StartEvent.png",
            "isCreated":true,
            "defaultAttr":{
                "desc":getInternationalValByKey("js.ec.workflow.config.startEvent"),
                "tagBpmn": "StartEvent",
                "internationalKey":"",
                "doURL":"",
                "viewName":"",
                "viewCode":"",
                "ignorePermission":false,
                "starter":[]
            },
            "area":["650px","auto"]
        },
        {
            "name": getInternationalValByKey("js.ec.workflow.config.endEvent"),
            "tagName": "end",
            "tagBpmn": "EndEvent",
            "lowercase":"endevent",
            "icon": "/bap/static/flowEditH5/images/icon_EndEvent.png",
            "isCreated":true,
            "defaultAttr":{
                "desc":getInternationalValByKey("js.ec.workflow.config.endEvent"),
                "tagBpmn": "EndEvent",
                "internationalKey":"",
                "effectFlag":true,
                "email":false,
                "jabber":false,
                "sms":false,
                "app":false
            },
            "area":["450px","auto"]
        },
        {
            "name": getInternationalValByKey("js.ec.workflow.config.endCancelEvent"),
            "tagName": "end-cancel",
            "tagBpmn": "EndCancelEvent",
            "lowercase":"endcancelevent",
            "icon": "/bap/static/flowEditH5/images/icon_EndCancelEvent.png",
            "isCreated":false,
            "defaultAttr":{
                "desc":getInternationalValByKey("js.ec.workflow.config.endCancelEvent"),
                "tagBpmn": "EndCancelEvent",
                "internationalKey":""
            }
        },
        {
            "name": getInternationalValByKey("js.ec.workflow.config.taskEvent"),
            "tagName": "task",
            "tagBpmn": "TaskEvent",
            "lowercase":"taskevent",
            "icon": "/bap/static/flowEditH5/images/icon_TaskEvent.png",
            "isCreated":false,
            "defaultAttr":{
                "desc":getInternationalValByKey("js.ec.workflow.config.taskEvent"),
                "tagBpmn": "TaskEvent",
                "internationalKey":"",
                "taskType":"普通活动",
                "viewUrl":"",
                "viewName":"",
                "viewCode":"",
                "viewType":"",
                "target":"_blank",
                "staffIds":"",
                "sequence":"",
                "bulkDealFlag":false,
                "dealSet":"0",
                "webSignetFalg":false,
                "recallAble":true,
                "customParam":"",
                "showInSimpleDealInfo":true,
                "mobileApprove":true,
                "ignorePermission":false,
                "isAllowProxy":true,
                "inputorFlag":false,
                "leaderFlag":false,
                "bigLeaderFlag":false,
                "activityDealFlag":false,
                "flowDealFlag":false,
                "attentFlag":false,
                "email":false,
                "jabber":false,
                "sms":false,
                "app":false,
                "requiredTime":"",
                "overdueReminders":false,
                "doubleSign":false,
                "actor":[],
                "event":[]
            },
            "area":["900px","auto"]
        },
        {
            "name": getInternationalValByKey("js.ec.workflow.config.countersignEvent"),
            "tagName": "countersign",
            "tagBpmn": "CountersignEvent",
            "lowercase": "countersignevent",
            "icon": "/bap/static/flowEditH5/images/icon_CountersignEvent.png",
            "isCreated":false,
            "defaultAttr":{
                "desc":getInternationalValByKey("js.ec.workflow.config.countersignEvent"),
                "tagBpmn": "CountersignEvent",
                "internationalKey":"",
                "viewUrl":"",
                "viewName":"",
                "viewCode":"",
                "viewType":"",
                "target":"_blank",
                "staffIds":"",
                "sequence":"",
                "bulkDealFlag":false,
                "loopSign":false,
                "loop":"",
                "dealSet":"0",
                "webSignetFalg":false,
                "recallAble":true,
                "customParam":"",
                "showInSimpleDealInfo":true,
                "mobileApprove":true,
                "ignorePermission":false,
                "isAllowProxy":true,
                "inputorFlag":false,
                "leaderFlag":false,
                "bigLeaderFlag":false,
                "activityDealFlag":false,
                "flowDealFlag":false,
                "attentFlag":false,
                "email":false,
                "jabber":false,
                "sms":false,
                "app":false,
                "requiredTime":"",
                "overdueReminders":false,
                "doubleSign":false,
                "actor":[],
                "event":[],
                "selectionScope":[]    
            },
            "area":["910px","auto"]
        },
        {
            "name": getInternationalValByKey("js.ec.workflow.config.autoEvent"),
            "tagName": "auto",
            "tagBpmn": "AutoEvent",
            "lowercase": "autoevent",
            "icon": "/bap/static/flowEditH5/images/icon_AutoEvent.png",
            "isCreated":false,
            "defaultAttr":{
                "desc":getInternationalValByKey("js.ec.workflow.config.autoEvent"),
                "tagBpmn": "AutoEvent",
                "internationalKey":"",
                "scriptName":"",
                "scriptCode":"",
                "email":false,
                "jabber":false,
                "sms":false,
                "app":false
            },
            "area":["650px","auto"]
        },
        {
            "name": getInternationalValByKey("js.ec.workflow.config.infoEvent"),
            "tagName": "info",
            "tagBpmn": "InfoEvent",
            "lowercase": "infoevent",
            "icon": "/bap/static/flowEditH5/images/icon_InfoEvent.png",
            "isCreated":false,
            "defaultAttr":{
                "desc":getInternationalValByKey("js.ec.workflow.config.infoEvent"),
                "tagBpmn": "InfoEvent",
                "internationalKey":"",
                "topic":""
            },
            "area":["500px","auto"]
        },
        {
            "name": getInternationalValByKey("js.ec.workflow.config.decisionEvent"),
            "tagName": "decision",
            "tagBpmn": "DecisionEvent",
            "lowercase":"decisionevent",
            "icon": "/bap/static/flowEditH5/images/icon_DecisionEvent.png",
            "isCreated":false,
            "defaultAttr":{
                "desc":getInternationalValByKey("js.ec.workflow.config.decisionEvent"),
                "tagBpmn": "DecisionEvent",
                "internationalKey":"",
                "expr":""
            },
            "area":["650px","auto"]
        },
        {
            "name": getInternationalValByKey("js.ec.workflow.config.forkEvent"),
            "tagName": "fork",
            "tagBpmn": "ForkEvent",
            "lowercase":"forkevent",
            "icon": "/bap/static/flowEditH5/images/icon_ForkEvent.png",
            "isCreated":false,
            "defaultAttr":{
                "desc":getInternationalValByKey("js.ec.workflow.config.forkEvent"),
                "tagBpmn": "ForkEvent",
                "internationalKey":""
            },
            "area":["450px","auto"]
        },
        {
            "name": getInternationalValByKey("js.ec.workflow.config.joinEvent"),
            "tagName": "join",
            "tagBpmn": "JoinEvent",
            "lowercase":"joinevent",
            "icon": "/bap/static/flowEditH5/images/icon_JoinEvent.png",
            "isCreated":false,
            "defaultAttr":{
                "desc":getInternationalValByKey("js.ec.workflow.config.joinEvent"),
                "tagBpmn": "JoinEvent",
                "internationalKey":"",
                "multiplicity":""
            },
            "area":["350px","auto"]
        },
        {
            "name": getInternationalValByKey("js.ec.workflow.config.subProcessEvent"),
            "tagName": "sub-process",
            "tagBpmn": "SubProcessEvent",
            "lowercase": "subprocessevent",
            "icon": "/bap/static/flowEditH5/images/icon_SubProcessEvent.png",
            "isCreated":false,
            "defaultAttr":{
                "desc":getInternationalValByKey("js.ec.workflow.config.subProcessEvent"),
                "tagBpmn": "SubProcessEvent",
                "internationalKey":"",
                "sub-process-key":"",
                "paramSet":[]
            },
            "area":["650px","auto"]
        },{
            "name":getInternationalValByKey("js.ec.workflow.config.sequenceFlow"),
            "tagName": "transition",
            "tagBpmn": "SequenceFlow",
            "lowercase": "sequenceflow",
            "icon": "/bap/static/flowEditH5/images/icon_line01.png",
            "isCreated":true,
            "defaultAttr":{
                "desc":"",
                "tagBpmn": "SequenceFlow",
                "internationalKey":"",
                "encode":"",
                "reject":"0",
                "ableSelectStaff":false,
                "selectStaff":"",
                "requiredStaff":false,
                "defaultSelectStaff":false,
                "sequence":"",
                "expr":"",
                "selectionScope":[],
                "description":''
            },
            "area":["700px","auto"]
        }
    ];
    //类型对照关系
    var typeArr={};
	$.each(typeControl, function (i, item) {
        typeArr[item.tagBpmn] = {"tool":item.name,"show":item.defaultAttr.desc};
    });
    var config={
        "shape":shape,
        "typeControl":typeControl,
        "typeArr":typeArr
    }
    return config;
});