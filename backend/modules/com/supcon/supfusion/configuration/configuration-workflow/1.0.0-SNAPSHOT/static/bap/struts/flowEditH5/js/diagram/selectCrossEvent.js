/**
 * 跨窗口选择内容
 * 定义回填事件
 */
define(function (require, exports, module) {
	var callbackFun;//回调事件
	var nameConfig={
			"USER":{"name":getInternationalValByKey("js.ec.workflow.config.user"),"tag":"user"},
	        "ROLE":{"name":getInternationalValByKey("js.ec.workflow.config.role"),"tag":"role"},
	        "POSITION":{"name":getInternationalValByKey("js.ec.workflow.config.position"),"tag":"position"},
	        "DEPTMENT":{"name":getInternationalValByKey("js.ec.workflow.config.department"),"tag":"department"},
	        "VIEW":{"name":getInternationalValByKey("js.ec.workflow.config.view"),"tag":"viewList"},
	        "assignedPosition":{"name":getInternationalValByKey("js.ec.workflow.config.assignPositionPower"),"tag":"assignedPosition"},
	        "assignUser":{"name":getInternationalValByKey("js.ec.workflow.config.assignStaffPower"),"tag":"assignedStaff"},
	        "SCRIPT":{"name":getInternationalValByKey("js.ec.workflow.config.groovy"),"tag":"script"},
	        "getReference":{"name":getInternationalValByKey("js.ec.workflow.config.tool.reference"),"tag":"getReference"}
	        }
	//跨窗体人员选择
    var crossEvent=function(type,param,callback){
    	var name=getInternationalValByKey("js.ec.workflow.config.choose")+nameConfig[type].name;
    	var typeTag=nameConfig[type].tag;
    	var entityCode = $("#entityCode").val();
    	if(!param||param==null||param=="null"){
    		param="";
    	}
    	if(param!="") param+="&";//有自定义参数
    	//窗体参数
    	if(type=="VIEW"){
    		param+="entity.code="+entityCode;
    		param+="&workflowViewChoose=true";//新加的参数
    	}else{
    		param+="entityCode="+entityCode;
    	}
    	callbackFun=new callbackEvent(callback);//创建回调实例
    	param+="&unassignStaffSupport=true";//人员、脚本选择
		if($("#workFlowForm_companyType").val()=='GROUP'){
    		param+="&crossCompanyFlag=true";
    	}
    	//窗体样式
    	var windowStyle = "width=1000,height=650,top=120,left=120,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
    	foundation.common.callOpenWeb(typeTag,windowStyle,name,true,null,"_callback_openWin",param);	
    }
	//回调事件
	window._callback_openWin=function(objs){
		if (objs == null || objs == undefined || objs.length <= 0) {
			return false;
		}
		callbackFun.back(objs);
	}
	//govery脚本回调
	window.setScript=function(objArr){
		_callback_openWin(objArr);
	}
	//视图选择回调
	window.getViewInfo=function(objArr){
		_callback_openWin(objArr);
	}
    //回调实例
    var callbackEvent=function(fun){
    	this.back=fun;
    }
    //变量监听---国际化编码浮层会被layui浮层覆盖问题解决
    foundation.international={}; // 解决IE9下生产环境Object.defineProperty: 参数不是 Object报错
    Object.defineProperty(foundation.international,'workflowdialog', {
        get: function() { //取值的时候会触发
            return simpledialog;
        },
        set: function(value) { //更新值的时候会触发
        	simpledialog = value;
        	setTimeout(function(){
        		var layIndex=$(".layui-layer").css("z-index");
            	var diaIndex=$(".ewc-dialog-blove").css("z-index");
        		if(Number(layIndex)>Number(diaIndex)){
        			$(".ewc-dialog-blove").css("z-index",Number(layIndex)+10);
        			$("#ygddfdiv").css("z-index",Number(layIndex)+20);
        			$(".elp-mask,.elp-shim-ie").css("z-index",Number(layIndex)+5);
        		}
    		},100);
        }
    });
	return crossEvent;
});