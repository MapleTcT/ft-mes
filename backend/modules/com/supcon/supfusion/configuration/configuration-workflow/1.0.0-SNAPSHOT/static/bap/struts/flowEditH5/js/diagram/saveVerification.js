/**
 * 流程图保存前的数据验证
 * 开始活动、结束活动和其他普通活动（人工或会签，自动），而且活动之间必须用迁移线链接，且方向一致（即有完整的开始结束（通知活动可以没有结束节点）的流程路线）。
 */
define(function (require, exports, module) {
    var bpmnPublic=require("./bpmnPublic.js");//基本方法
    var diConfig=require("./diagramConfig.js");//配置信息
    var publicTypeArr= diConfig.typeArr;//类型对照关系
	/*流程验证
	 * @param {Object} [data] 流程数据
	 * @param {boolbean} isOnlysPower 只做权限验证
	 * @param {Function} done 回调函数
	 * */
    var saveVerification=function(data,isOnlysPower,callback){
        var lineArr=[];//线条
        var shapeArr=[];//形状
        $.each(data,function(i,item){
            var type=item.$type;
            if(type=="bpmn:SequenceFlow"){
                lineArr.push(item);
            }else{
                shapeArr.push(item);
            }
        });
        var shapeErr=shapeVerification(lineArr,shapeArr);//流程节点验证
        var lineErr=lineVerification(lineArr,shapeArr);//流程迁移线验证
        var errorAll=shapeErr.concat(lineErr);
        if(isOnlysPower){
        	//只做权限验证的时候（保存操作只做权限验证）
        	var ErrFilter=[];
        	$.each(errorAll,function(i,item){
        		if(item.type=="errorPower"){
        			ErrFilter.push(item);
        		}
        	});
        	errorAll=ErrFilter;
        }
        bpmnPublic.formConsoleOut(errorAll);//输出报错提示
        if(typeof callback=="function"){
            var noError=errorAll.length==0;
            callback(noError);
        }
    }
    //流程节点验证
    function shapeVerification(ldata,sdata){
        var shapeError=[];
        var typeArr=[];
        var typeCounter=0;
        var startCounter=0
        var subError=[];
        $.each(sdata,function(i,item){
            var type=item.$type.split(":")[1];
            var id=item.id;
            subError=subError.concat(getVerificateInfo(type,id,ldata));//获取验证结果
            typeArr.push(type);   
            if(type!="StartEvent"&&type!="EndEvent"&&type!="EndCancelEvent") typeCounter++;
            if(type=="StartEvent") startCounter++;
        });
        if(typeArr.length==0) shapeError.push({type:"error",info:"message:"+getInternationalValByKey("js.ec.workflow.config.alert.emptyFlow")})
        if(startCounter==0) shapeError.push({type:"error",info:"message:"+getInternationalValByKey("js.ec.workflow.config.alert.startNodeMust")});
        if(startCounter>1) shapeError.push({type:"error",info:"message:"+getInternationalValByKey("js.ec.workflow.config.alert.startNodeOnly")});
        if(typeCounter==0) shapeError.push({type:"error",info:"message:"+getInternationalValByKey("js.ec.workflow.config.alert.otherNodeNeed")});
        shapeError=shapeError.concat(subError);
        return shapeError;
    }
    //属性设置验证条目
    function getVerificateInfo(type,id,ldata){
        var eleData=bpmnPublic.getElementDataById(id,type);//获取初始值定义 
        var taskName=eleData.desc;
        var errorInfo=[];
        //属性值判断
        for(var key in eleData){
            var value=eleData[key];
            if(key=="desc"&&value==""){
                errorInfo.push({type:"error",info:"message:"+taskName+getInternationalValByKey("js.ec.workflow.config.alert.activityNameEmpty")});
            }
            if(key=="doURL"&&value==""){
                errorInfo.push({type:"error",info:"message:"+taskName+getInternationalValByKey("js.ec.workflow.config.alert.activityViewEmpty")});
            }
            if(key=="viewUrl"){
            	if(value==""){
            		var noticeStr=type=="StartEvent"?taskName+getInternationalValByKey("js.ec.workflow.config.alert.activityViewEmpty"):taskName+getInternationalValByKey("js.ec.workflow.config.alert.activityViewSet");
            		errorInfo.push({type:"error",info:"message:"+noticeStr});
            	}else{
            		//编辑视图不能设置批量处理
            		if(eleData.viewType=="EDIT"&&eleData.bulkDealFlag==true){
                		errorInfo.push({type:"error",info:"message:"+taskName+getInternationalValByKey("js.ec.workflow.config.alert.bulkDealFlagSet")});
                	}   
            	}  
            }
            if(key=="starter"||key=="actor"||key=="selectionScope"){
            	var powerFillState=getPowerSet(value,key);//权限设置信息是否完整
            	$.each(powerFillState,function(i,state){
            		if(state=="1") errorInfo.push({type:"errorPower",info:"message:"+taskName+getInternationalValByKey("js.ec.workflow.config.alert.powerSet")});
                    if(state=="2") errorInfo.push({type:"errorPower",info:"message:"+taskName+getInternationalValByKey("js.ec.workflow.config.alert.powerIncomplete")});
                    if(state=="3") errorInfo.push({type:"errorPower",info:"message:"+taskName+getInternationalValByKey("js.ec.workflow.config.alert.assignPositionPower")});
                    if(state=="4") errorInfo.push({type:"errorPower",info:"message:"+taskName+getInternationalValByKey("js.ec.workflow.config.alert.assignStaffPower")});
            	});
            }
            if(key=="scriptName"&&value==""){
                errorInfo.push({type:"error",info:"message:"+taskName+getInternationalValByKey("js.ec.workflow.config.alert.selectScript")});
            }
        }
        //选择路由或者由它出去的迁移线中必须填写逻辑条件
        if(type=="DecisionEvent"){
            var hasExpression=eleData.expr!=""||getLineExpression(id,ldata);
            if(!hasExpression) errorInfo.push({type:"error",info:"message:"+getInternationalValByKey("js.ec.workflow.config.alert.exprssionMust")});
        }
        //聚合活动的聚合数不能大于迁移线进线数
        if(type=="JoinEvent"){
        	var joinNum=getJoinLineNum(id,ldata);//获取聚合活动迁入的迁移线数量
        	if(eleData.multiplicity!=""&&Number(eleData.multiplicity)>joinNum){
        		errorInfo.push({type:"error",info:"message:"+getInternationalValByKey("js.ec.workflow.config.alert.joinNumber")});
        	}
        }
        return errorInfo;
    }
    //权限设置信息是否完整
    function getPowerSet(data,type){
    	var flag=[];
    	$.each(data,function(i,item){
    		if(item.typeName==""||item.typeId==""){
    			flag.push(1);//权限未配置
    			return true; //continue向下执行循环
    		}
    		if(type=="actor"){
    			//参与者
    			if(!item.unLimitPower&&!item.positionPower&&!item.assignPositionPower&&!item.assignStaffPower&&!item.groupPower){
    				flag.push(2);//权限配置信息不完整
    				return true; //continue向下执行循环
    			}else if(item.assignPositionPower&&item.assignPositions==""){
    				flag.push(3);//勾选指定岗位，未选择指定岗位的情况
    				return true; //continue向下执行循环
    			}else if(item.assignStaffPower&&item.assignStaffs==""){
    				flag.push(4);//勾选指定人员，未选择指定人员的情况
    				return true; //continue向下执行循环
    			}
    		}
    	});
    	return flag;
    }
    //流程迁移线描述验证
    function lineVerification(ldata,sdata) {
        var lineError=[];
        $.each(ldata, function (i, item) {
            var lineName=item.name;
            var startType=item.sourceRef.$type.split(":")[1];
            var startName=item.sourceRef.name;
            if(startType=="TaskEvent"||startType=="CountersignEvent"||startType=="StartEvent"){
                //人工活动、会签活动、开始活动出去的迁移线必须要有名称
                if(!lineName||lineName=="") lineError.push({type:"error",info:"message:"+startName+getInternationalValByKey("js.ec.workflow.config.alert.describeMust")});
            }
        });
        //图形迁移线连接验证
        $.each(sdata,function(i,item){
            var type=item.$type.split(":")[1];
            var id=item.id;
            var name=item.name;
            var hasOutGoing=getLineLink(id,ldata,"sourceRef");
            var hasInComing=getLineLink(id,ldata,"targetRef");
            var mustOutgoing=true;//必须有出去的迁移线
            var mustIncomeing=true;//必须有进入的迁移线
            //排除开始活动、结束活动、作废活动（人工活动的普通活动）的特殊情况，其他流程节点必须有进入和出去的迁移线
            if(type=="TaskEvent"){
                var initData=bpmnPublic.getElementDataById(id,type);//获取初始值定义 
                if(initData.taskType=="通知活动") mustOutgoing=false;//通知活动不一定有出去的迁移线
            }
            if(type=="StartEvent") mustIncomeing=false;//开始活动没有进入的迁移线
            if(type=="EndEvent"||type=="EndCancelEvent") mustOutgoing=false;//结束活动、作废活动没有出去的迁移线
            //验证条件
            if(!hasOutGoing&&mustOutgoing) lineError.push({type:"error",info:"message:"+name+getInternationalValByKey("js.ec.workflow.config.alert.outlineMust")});
            if(!hasInComing&&mustIncomeing) lineError.push({type:"error",info:"message:"+name+getInternationalValByKey("js.ec.workflow.config.alert.incomeMust")});
        });
        return lineError;
    }
    //是否存在迁移线
    function getLineLink(id,data,tag){
        var isFind=false;
        $.each(data,function(i,item){
            var eleId=item[tag].id;
            if(eleId==id){
                isFind=true;
                return false;
            }
        });
        return isFind;
    };
    //选择路由出来的迁移线是否有逻辑表达式
    function getLineExpression(id,data){
        var flag=false;
        $.each(data,function(i,item){
            var lineId=item.id;
            var type=item.$type.split(":")[1];
            var startId=item.sourceRef.id;
            if(id==startId){
                var initData=bpmnPublic.getElementDataById(lineId,type);//获取初始值定义 
                flag=initData.expr!="";
                if(flag) return false;//不止一条迁移线，遍历结束前未找到有定义的逻辑表达式则不跳出循环
            }
        });
        return flag;
    }
    //获取聚合活动进入的迁移线数量
    function getJoinLineNum(id,data){
    	var num=0;
    	$.each(data,function(i,item){
            var endId=item.targetRef.id;
            if(id==endId){
            	num++;
            }
        });
    	return num;
    }
    return saveVerification;
});