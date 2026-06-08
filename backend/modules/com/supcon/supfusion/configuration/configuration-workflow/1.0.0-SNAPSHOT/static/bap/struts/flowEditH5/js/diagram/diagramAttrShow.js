/**
 * 单击元素关联右侧属性面板
 * 修改流程图节点属性
 */
define(function (require, exports, module) {
    var bpmnPublic=require("./bpmnPublic.js");//基本方法
    var bindElementAttr = function (element) {
        if(element){
            var object=element.businessObject;
            var id=object.id;
            var type=object.$type.split(":")[1];
            var initData=bpmnPublic.getElementDataById(object.id,type);//获取初始值定义
            if(initData) initData.desc=(object.name==null||object.name==undefined)?"":object.name;
            if(type=="SequenceFlow"){
                initData.encode=object.id;
                initData.reject=object.reject;
            }
            bindDiagramAttr(initData,type);//绑定属性面板
        }else{
            bindDiagramAttr();//点击空白处，显示流程图信息
        }    
    };
    //绑定属性面板
    function bindDiagramAttr(data,type){
        var showAttr=getShowAttribute(data,type);
        if(showAttr.length==0){
        	//绑定默认属性
        	bpmnPublic.bindDefaultAttr();
        }else{
        	//绑定属性面板
            $("#diagramAttrBox").html(template("attrTemp",showAttr));
        }  
    }
    //各类型需显示的属性
    function getShowAttribute(data,type){
        var attr=[];
        switch(type){
            case "TaskEvent":
                attr = [{
                    "key": getInternationalValByKey("js.ec.workflow.config.name"),
                    "value": data.desc
                }, {
                    "key": getInternationalValByKey("js.ec.workflow.config.taskType"),
                    "value": data.taskType
                }, {
                    "key": getInternationalValByKey("js.ec.workflow.config.callView"),
                    "value": data.viewName
                },{
                    "key": getInternationalValByKey("js.ec.workflow.config.candidate"),
                    "value": data.staffIds
                }];
                break;
            case "JoinEvent":
                attr = [{
                    "key": getInternationalValByKey("js.ec.workflow.config.name"),
                    "value": data.desc
                }]
                break;
            case "ForkEvent":
                attr = [{
                    "key": getInternationalValByKey("js.ec.workflow.config.name"),
                    "value": data.desc
                }];
                break;
            case "DecisionEvent":
                attr=[{
                    "key": getInternationalValByKey("js.ec.workflow.config.name"),
                    "value": data.desc
                },{
                    "key": getInternationalValByKey("js.ec.workflow.config.expression"),
                    "value": data.expr
                }];
                break;
            case "CountersignEvent":
                attr=[{
                    "key": getInternationalValByKey("js.ec.workflow.config.name"),
                    "value": data.desc
                },{
                    "key": getInternationalValByKey("js.ec.workflow.config.callView"),
                    "value": data.viewName
                },{
                    "key": getInternationalValByKey("js.ec.workflow.config.loopSign"),
                    "value": data.loopSign?getInternationalValByKey("js.ec.workflow.config.yes"):getInternationalValByKey("js.ec.workflow.config.no")
                },{
                    "key": getInternationalValByKey("js.ec.workflow.config.corssCompany"),
                    "value": data.loop=="2"?getInternationalValByKey("js.ec.workflow.config.yes"):getInternationalValByKey("js.ec.workflow.config.no")
                }];
                break;
            case "AutoEvent":
                attr=[{
                    "key": getInternationalValByKey("js.ec.workflow.config.name"),
                    "value": data.desc
                },{
                    "key": getInternationalValByKey("js.ec.workflow.config.groovy"),
                    "value": data.scriptName
                }];
                break;
            case "SubProcessEvent":
                attr=[{
                    "key": getInternationalValByKey("js.ec.workflow.config.name"),
                    "value": data.desc
                },{
                    "key": getInternationalValByKey("js.ec.workflow.config.encode"),
                    "value": data["sub-process-key"]
                }];
                break; 
            case "SequenceFlow":
                attr=[{
                    "key":getInternationalValByKey("js.ec.workflow.config.name"),
                    "value":data.desc
                },{
                    "key":getInternationalValByKey("js.ec.workflow.config.type"),
                    "value":data.reject=="0"?getInternationalValByKey("js.ec.workflow.config.commonLine"):getInternationalValByKey("js.ec.workflow.config.commonRejectLine")
                },{
                    "key":getInternationalValByKey("js.ec.workflow.config.expression"),
                    "value":data.expr
                }]   
        }
        return attr;
    }
    return bindElementAttr;
});
