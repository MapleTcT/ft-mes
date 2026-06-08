/**
 * 参数读取与参数保存的相互转换
 * 主要涉及权限配置、选人范围、国际化编码对照
 */
define(function (require, exports, module) {
	var bpmnPublic=require("./bpmnPublic.js");//基本方法
	var xmlConvert = require("./xmlConvert.js");//xml数据转换
	var transParam=function(){};
	//获取已保存（发布）的参数。存入全局变量
	transParam.prototype.getExistParam=function(){
		var activeArr=$("#activeArr").val();//获取配置过权限的流程节点
		var powerStr=$("#powerXml").val();//获取权限配置信息
		var selectStaffs=$("#selectStaffs").val();//选人范围
		//权限分配
		if(powerStr&&powerStr!=""){
			var powerXml=bpmnPublic.string2XML(powerStr);//字符串转xml格式
			var powerChild =powerXml.childNodes;
			var powerArr=xmlConvert.XmlToJson.parse(powerChild[0]);//xml字符串转json格式
			if(powerArr&&powerArr.items&&powerArr.items.length>0){
				//有权限配置信息
				saveExistParam(powerArr.items);
			}
		}	
		//选人范围
		if(selectStaffs&&selectStaffs!=""){
			var selArr=selectStaffs.split(";");
			if(selArr&&selArr.length>0){
				//有选人范围信息
				saveSelectParam(selArr);
			}	
		}
	}
	//将权限配置信息存入全局变量
	function saveExistParam(data){
		var powerBefore="";//上一次保存权限信息
		$.each(data,function(i,obj){
			var id=obj.taskName;
			var nodeData=window.processAttribute[id];
			if(!nodeData) return;//排除未定义情况（正常情况下不会出现）
			var type=nodeData.tagBpmn;//节点类型
			if(type=="StartEvent"){
				//开始活动（发起者权限信息）
				var keyobj=["typeId","typeName","userType"];//需要读取的属性值
				var power=getPower(obj.items,keyobj);
				nodeData.starter=power;
				powerBefore+=getOperatePowers(id,type,power);
				$("#entryUrl").val(nodeData.doURL);//开始活动的视图
			}else if(type=="TaskEvent"||type=="CountersignEvent"){
				//人工、会签活动（参与者权限信息）
				var keyobj=["typeId","typeName","userType","assignPositionPower","assignPositions","assignStaffPower","assignStaffs","groupPower","positionPower","unLimitPower"];//需要读取的属性值
				var power=getPower(obj.items,keyobj);
				nodeData.actor=power;
				powerBefore+=getOperatePowers(id,type,power);
			}
		});
		powerBefore=powerBefore.substr(0,powerBefore.length-1);
		$("#powerBefore").val(powerBefore);//上一次保存权限信息
	}
	//获取权限配置
	function getPower(data,keyobj){
		var powerArr=[];
		$.each(data,function(i,item){
			var powerObj={};
			for(var k=0;k<keyobj.length;k++){
				var key=keyobj[k];
				powerObj[key]=booleanTransform(item[key]);
			}
			powerArr.push(powerObj);
		});	
		return powerArr;
	}
	//选人范围属性配置信息
	function saveSelectParam(data){
		$.each(data,function(i,item){
			var attr=item.split(",");
			var id=attr[0];
			var nodeData=window.processAttribute[id];
			if(!nodeData) return;//排除未定义情况（正常情况下不会出现）
			var scope={
					"groupName": attr[4],
					"order": attr[5],
					"typeId": attr[2],
					"typeName": attr[3],
					"userType": attr[1]
			}
			if(!nodeData.selectionScope) nodeData.selectionScope=[];
			nodeData.selectionScope.push(scope);
		});
	};
	//记录当前流程节点id
	transParam.prototype.recordExistEle=function(){
		var data=window.processAttribute;
		var nodeIds=[];
		for(var key in data){
			nodeIds.push(key);
		}
		window.originalNodeCollection=nodeIds;//存入全局变量
	}
	//-----------------保存当前的参数设置------------------------
    transParam.prototype.saveCurrentParam=function(bpmnModeler){
    	var process=getPanelElementData(bpmnModeler);
    	var activeArr="";//获取配置过权限的流程节点
    	var selectStaffs="";//选人范围
    	var keyDescs="";//国际化编码
    	var operatePowers="";//权限信息
    	var menuOperateStr=getMenuOperateStr(process);//活动操作信息
    	for(var id in process){
    		var item=process[id];
    		if(item.tagBpmn=="StartEvent"||item.tagBpmn=="TaskEvent"||item.tagBpmn=="CountersignEvent"){
    			activeArr+=id+",";
    		}
    		if(item.tagBpmn=="StartEvent") $("#entryUrl").val(item.doURL);//开始活动的视图
    		if(item.actor&&item.actor.length>0){
    			//参与者
    			operatePowers+=getOperatePowers(id,item.tagBpmn,item.actor);
    		}
    		if(item.starter&&item.starter.length>0){
    			//发起者
    			operatePowers+=getOperatePowers(id,item.tagBpmn,item.starter);
    		}
    		if(item.selectionScope&&item.selectionScope.length>0){
    			//选人范围
    			selectStaffs+=getSelectInfo(id,item.selectionScope);
    		}
    		item.desc=(item.desc==null||item.desc==undefined)?"":item.desc;//描述信息为null或者undefined时用空格替换
    		if(item.internationalKey&&item.internationalKey!="") keyDescs+=item.internationalKey+","+item.desc+";";
    	}
    	activeArr=activeArr.substr(0,activeArr.length-1);
    	selectStaffs=selectStaffs.substr(0,selectStaffs.length-1);
    	operatePowers=operatePowers.substr(0,operatePowers.length-1);
    	menuOperateStr=menuOperateStr.substr(0,menuOperateStr.length-1);
    	$("#activeArr").val(activeArr);//活动
    	$("#selectStaffs").val(selectStaffs);//选人范围
    	$("#keyDescs").val(keyDescs);//国际化
    	$("#operatePowers").val(operatePowers);//权限配置
    	$("#menuOperateStr").val(menuOperateStr);//活动操作信息
	}
    //获取活动操作信息
    function getMenuOperateStr(data){
    	var operateStr="";
    	for(var id in data){
    		var str="";
    		var item=data[id];
    		if(item.tagBpmn=="StartEvent"){
    			//开始活动
    			str+=id+"|ec.workflow.formulate|"+item.doURL+"|ACTIVEOPERATE|"+item.ignorePermission;
    		}else if(item.tagBpmn=="TaskEvent"||item.tagBpmn=="CountersignEvent"){
    			//人工、会签活动
    			str+=id+"|"+item.internationalKey+"|"+item.viewUrl+"|FLOWOPERATE"+"|"+item.ignorePermission+"|isAllowProxy:"+item.isAllowProxy;
    			var remindFlag=0;
    			if(item.email){
    				remindFlag+=2;
    			}
    			if(item.jabber){
    				remindFlag+=1;
    			}
    			if(item.sms){
    				remindFlag+=4;
    			}
    			if(item.app){
    				remindFlag+=8;
    			}
    			if(remindFlag!=0){
    			    str+="|remindFlag:"+remindFlag;
    			}
    		}
    		if(str!=""){
    		    operateStr+=str+"$";
    		}
    	}
    	return operateStr;	
    }
    //获取权限配置属性信息
    function getOperatePowers(id,type,data){
    	var str="";
    	$.each(data,function(i,item){
    	    str+=id+"$$"+item.userType+"$$"+item.typeId;
    	    if(type=="TaskEvent"||type=="CountersignEvent"){
    	    	if(item.unLimitPower==false){
    	    		str+="$$"+item.positionPower+"$$"+item.groupPower;
        	    	if(item.assignPositionPower){
        	    		str+="$$"+item.assignPositions.split(";").join("||");
        	    	}else{
        	    		str+="$$false";
        	    	}
        	    	if(item.assignStaffPower){
        	    		str+="$$"+item.assignStaffs.split(";").join("||");
        	    	}else{
        	    		str+="$$false";
        	    	}
    	    	}
    	    }
    	    str+=";"
    	});
    	return str;
    }
    //获取选人范围属性配置信息
    function getSelectInfo(id,data){
    	var str="";
    	$.each(data,function(i,item){
    	    str+=id+","+item.userType+","+item.typeId+","+item.groupName+","+item.order+";"	
    	});
    	return str;
    }
    //获取当前面板的节点的参数信息
    function getPanelElementData(bpmnModeler){
    	var winsave=window.processAttribute;
    	var bpmnEle=bpmnModeler._definitions.rootElements[0].flowElements;
    	var existEle={};
    	$.each(bpmnEle,function(i,item){
    		var bid=item.id;
    		existEle[bid]=winsave[bid]
    	});
    	window.processAttribute=existEle;
    	return existEle;
    }
    //布尔型转换（不存在自填项，可以通过字符串等值替换）
    function booleanTransform(value){
        if(value==="true"){
        	value=true;
        }else if(value==="false"){
        	value=false
        }
        return value;
    }
    return new transParam();
});