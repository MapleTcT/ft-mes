/**
 * 鼠标移到已处理的流程节点，需要显示节点信息
 */
define(function (require, exports, module) {
	var bpmnPublic=require("./bpmnPublic.js");//基本方法
    var layer;
    layui.use('layer', function () {
        layer = layui.layer;
    });
    var showNodeDetail=function(){};
    //显示提示信息
    showNodeDetail.prototype.showInfo = function (data) {
        var type = data.$type;
        //人工和会签活动显示节点处理信息
        if (type=="bpmn:TaskEvent"||type=="bpmn:CountersignEvent") {
            //类别为流程节点时显示节点详情
            var ele = $(".djs-shape[data-element-id=" + data.id + "]");
            var isEffect=ele.attr("data-effect")=="1";//是否生效
            if(!isEffect) return;
            //获取节点详情信息
            getNodeInfo(data.id,function(str){
            	if(!str||str=="") return;
            	layer.tips(str, ele, {
                    tips: [3, '#ffff99'],
                    shadeClose: true,
                    area: 'auto',
                    maxWidth: 200,
                    maxHeight: 145,
                    time: 0//是否定时关闭，0表示不关闭
                });
            });  
        }else if(type=="bpmn:SequenceFlow"){
        	// 迁移线显示描述信息
        	if(data.id){
        		var lineD=window.processAttribute[data.id];
        		var desc=lineD.description;
        		var ele = $(".djs-connection[data-element-id=" + data.id + "]");
        		if(!desc||desc=="") return;
        		layer.tips(desc, ele, {
                    tips: [3, '#ffff99'],
                    shadeClose: true,
                    area: 'auto',
                    maxWidth: 200,
                    maxHeight: 145,
                    time: 0//是否定时关闭，0表示不关闭
                });
        	}
        }else{
        	layer&&layer.closeAll();
        	window.ajaxDealInfo&&window.ajaxDealInfo.abort();//终止ajax异步请求
        }
    };
    //获取节点信息
    function getNodeInfo(id,callback){
    	var isCur=$("g[data-element-id="+id+"]").find(".djs-outline").attr("data-state")=="cur";//是否为当前处理人
    	var tableInfoId=$("#fvTableInfoId",parent.document).val();
    	var modelCode=$("#modelCode",parent.document).val();
    	var urlPend="/msService/ec/workflow/getPendingInfo.action?tableInfoId="+tableInfoId+"&activityName="+id;
    	var urlDeal="/msService/ec/workflow/getDealInfoByActivity.action?tableInfoId="+tableInfoId+"&activityName="+id+"&modelCode="+modelCode;
    	window.ajaxDealInfo=$.ajax({
    		url:isCur?urlPend:urlDeal,
    		type:"post",
    		dataType:"text",
    		success:function(data){
    			if(!data||data=="") return;
    			var info=formInfo(JSON.parse(data),isCur);
    			if(typeof callback=="function"){
    				callback(info);
    			}
    		},
    		error:function(e){
    			console.log("/msService/ec/workflow/getDealInfoByActivity.action接口报错");
    		}
    	})
    	
    }
    //节点信息生成
    function formInfo(data,isCur){
    	var info=[],result="";
    	if(data.result&&data.result.length>0){
    		$.each(data.result,function(i,item){
    			var str="";
    			if(!isCur){
    				str=item.NAME+"&nbsp;<span style='white-space:nowrap;display:inline-block'>"
    				   +bpmnPublic.dateFtt("yyyy-MM-dd hh:mm:ss",new Date(item.CREATE_TIME))
    				   +"</span>&nbsp;"+getInternationalValByKey("js.ec.workflow.view.performInfo")
    				   +item.OUTCOME_DES;
    			}else{
    				str=item.NAME;
    			}
    			info.push(str);
    		});
    	}
    	if(!isCur){
    		result=info.join('</br>');
    	}else{
    		if(info.length>0) result=getInternationalValByKey("js.ec.workflow.view.todoPerson")+"</br>"+info.join('&nbsp;');
    	}
    	return result;
    }
    //关闭提示
    showNodeDetail.prototype.closeTips=function(data){
        layer&&layer.closeAll();
    }
    return showNodeDetail;
});