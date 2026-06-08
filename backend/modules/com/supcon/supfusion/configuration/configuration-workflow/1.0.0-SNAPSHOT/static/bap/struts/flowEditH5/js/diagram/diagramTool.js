/**
 * 工具栏事件定义
 */
define(function (require, exports, module) {
    var bpmnPublic=require("./bpmnPublic.js");//基本方法
    var paramTransform=require("./paramTransform.js");//权限配置、选人范围参数转换
    var xmlConvert;//流程图数据转换
    var bpmnModeler;//流程图实例
    var layer;
    layui.use('layer', function () {
        layer = layui.layer;
    });
    var initToolEvent = function (obj, bpmn, xmlfun) {
        bpmnModeler = bpmn;//流程图实例
        xmlConvert = xmlfun;//xml转换方法
        this.init(obj);
    };
    //工具栏事件初始化
    initToolEvent.prototype.init = function (obj) {
        var btns = $(obj).find(".m_menu_item input");
        var switchBtn = $(obj).find(".m_switch_item input");
        var viewCtrl=$(".viewScale .view_btn");
        //工具栏操作
        btns.on("click", function () {
            var index = $(this).index();
            dealClick(index);
        });
        //视图缩放
        viewCtrl.on("click",function(){
        	var ctrlVal=$(this).attr("data-id");
        	dealOperate("stepZoom", { "value": ctrlVal });
        });
        //模式切换
        switchBtn.on("click", function () {
            var isCur = $(this).hasClass("cur");
            if (isCur) return;
            $(this).addClass("cur").siblings().removeClass("cur");
            switchMode($(this).index());//模式切换
        });
    }
    //点击事件处理函数
    function dealClick(index) {
        switch (index) {
            case 0:
                //发布
                realseDiagram();
                break;
            case 1:
                //保存
                saveDiagram();
                break;
            case 2:
                //复制
                dealOperate("copy");
                break;
            case 3:
                //粘贴
                dealOperate("paste");
                break;
            case 4:
                //垂直
                dealOperate("alignElements", { "type": "left" });
                break;
            case 5:
                //水平
                dealOperate("alignElements", { "type": "bottom" });
                break;
            case 6:
                //重做
                dealOperate("redo");
                break;
            case 7:
                //撤销
                dealOperate("undo");
                break;
            case 8:
                //参照
                getReference();
                break;
            case 9:
                //帮助
                showHelpInfo();
                break;
        }
    };
    //复制、粘贴、重做、撤销、放大缩小
    function dealOperate(type, option) {
        var actions = bpmnModeler.injector._instances.editorActions;
        if (!option) {
            actions.trigger(type);
        } else {
            actions.trigger(type, option);
        }
    }
    //在线帮助信息
    function showHelpInfo(){
    	 layer.open({
             title: getInternationalValByKey("js.ec.workflow.config.tool.help"),
             type: 1,
             skin: 'layui-layer-bpmn',
             area: ["700px", "auto"],
             content: template("helpTemp",{})
         });
    }
    //发布流程图
    function realseDiagram() {
        layer.open({
            title: getInternationalValByKey("js.ec.workflow.config.tool.publish"),
            type: 1,
            skin: 'layui-layer-bpmn',
            area: ["500px", "auto"],
            content: template("releaseTemp",{})
        });
        layui.use(['element', 'form'], function () {
            var form = layui.form;
            //初始赋值
            form.val("release", {
                name: $("#flowName").val(),
                type: "0",
                des: $("#des").val()
            });
            form.render();//渲染
            //点击提交
            form.on('submit(*)', function (data) {
                var field = data.field;
                $("#des").val(field.des);//描述
                $("#publishType").val(field.type);
                layer.closeAll();//关闭浮层
                if(field.type=="1"){
                	//修改发布
                	var isModify=getFlowChangeState();//加了新的活动或者迁移线，或者删了原来的活动都必须全新发布
                	if(isModify){
                		bpmnPublic.showNotice(getInternationalValByKey("js.ec.workflow.config.alert.modifyPublishWarn"));
                        return;
                	}
                }
                //发布验证
                bpmnModeler.verificXML({ format: true },false,function (noError) {
                    if (!noError) {
                        bpmnPublic.showNotice(getInternationalValByKey("js.ec.workflow.config.alert.formatError"));
                        return;
                    }
                    bpmnModeler.saveXML({ format: true },function (err, xml) {
                        if (err) {
                            return console.error('could not save BPMN 2.0 diagram', err);
                        }
                        publishOrSaveFlow("publish",xml);//检查国际化编码后，发布流程
                    });
                });
                return false;
            });
            //取消操作
            var cancelCtrl = $("#cancelForm");
            cancelCtrl.on("click", function () {
                layer.closeAll();
            });
        });

    }
    //保存流程图
    function saveDiagram() {
        var isEdit=$("#isEditFlag").val()=="1";//流程是否被编辑
        var bpmnChange=bpmnModeler.injector._instances.commandStack._getUndoAction();//通过判断是否有回退步骤判断流程是否被编辑
        if(!isEdit&&!bpmnChange){
        	bpmnPublic.showNotice(getInternationalValByKey("js.ec.workflow.config.alert.noChange"));
        	return;
        }
        //保存前权限验证
    	bpmnModeler.verificXML({ format: true },true,function (noError) {
            if (!noError) {
                bpmnPublic.showNotice(getInternationalValByKey("js.ec.workflow.config.alert.saveError"));
                return;
            }
            bpmnModeler.saveXML({ format: true },function (err, xml) {
                if (err) {
                    return console.error('could not save BPMN 2.0 diagram', err);
                }
                if($("#flowEditFlag").val()=="1"){
                	var str=getInternationalValByKey("js.ec.workflow.config.alert.saveConfirm");
                    layer.confirm(str,{
                    	title: getInternationalValByKey("js.ec.workflow.config.alert.saveNotice"),
                    	skin: 'layui-layer-bpmn',
                      area: ["360px", "auto"],
                        btn: [getInternationalValByKey("js.ec.workflow.config.confirmFlag"),getInternationalValByKey("js.ec.workflow.config.cancel")] //按钮
                      }, function(){
                    	  layer.closeAll();
                    	  publishOrSaveFlow("save",xml);//检查国际化编码后，保存流程
                      }, function(){
                          layer.closeAll();
                      });	
                }else{
                	publishOrSaveFlow("save",xml);//保存流程
                }
            });
        }); 
    }
    //保存或发布流程
    function publishOrSaveFlow(tag,xml){
    	checkInternationalKey(function(){
    		publishOrSaveFlowAjax(tag,xml);
    	});
    	
    }
    //请求保存发布接口
    function publishOrSaveFlowAjax(tag,xml){
    	var _config={bgColor: "#666666",head: getInternationalValByKey("js.ec.workflow.config.wait"),opacity: 50,show: true};
        if(window.createLoadPanel){createLoadPanel(false,null,_config);};
    	var config={
    			"publish":{
    				"name":getInternationalValByKey("js.ec.workflow.config.tool.publish"),
    				"url":"/msService/ec/workflow/flowPublish"
    			},
    			"save":{
    				"name":getInternationalValByKey("js.ec.workflow.config.tool.save"),
    				"url":"/msService/ec/workflow/flowSave"
    			}
    	};
    	var transXml = xmlConvert.xmlConvertRorFlash.dealXMLData(createXML(xml),true);//xml转换
        var param=getParameter(tag,transXml);//获取参数
        var url=config[tag].url;//接口地址
        var tagName=config[tag].name;//操作名称
        $.ajax({
			url:url,
	    	type:"post",
	    	dataType:"xml",
	    	data:param,
	    	success:function(msg){
	    		closeLoadPanel();//关闭加载中
	    		var xmlChild =msg.childNodes;
	    		var xmlEle=xmlChild[0];
		        if(xmlEle&&xmlEle.nodeName=="flow"){	
		        	ec_wf_tableErrorBarWidget.showMessage(tagName+getInternationalValByKey("js.ec.workflow.config.alert.success") ,"s");//右上角提示信息
		        	bpmnPublic.formConsoleOut([{type:"success",info:tagName+getInternationalValByKey("js.ec.workflow.config.alert.success")}]);//控制台输出
		        	//在不刷新页面的情况下重置浏览器参数
		        	resetParameter(xmlEle);
		        	if (window.opener) {
		        	    window.opener.reloadTableData();//刷新父窗口列表
		        	}
		        	bpmnModeler.injector._instances.commandStack.clear(true);//保存后清空回退、前进操作保存
		        	$("#isEditFlag").val("0");//保存（发布）后流程改为未编辑状态
		        }else if(xmlChild[0]&&xmlChild[0].nodeName=="versionConflict"){
		        	//版本冲突
		        	var version=xmlChild[0].getAttribute("version");
		        	if(version&&version!="") $("#version").val(version);
		        	var str=getInternationalValByKey("js.ec.workflow.config.alert.cover");
                    layer.confirm(str,{
                    	title: getInternationalValByKey("js.ec.workflow.config.alert.coverNotice"),
                    	skin: 'layui-layer-bpmn',
                      area: ["360px", "auto"],
                        btn: [getInternationalValByKey("js.ec.workflow.config.confirmFlag"),getInternationalValByKey("js.ec.workflow.config.cancel")] //按钮
                      }, function(){
                    	  layer.closeAll();
                    	  publishOrSaveFlowAjax(tag,xml);//重新请求接口
                      }, function(){
                          layer.closeAll();
                      });
		        }else{
		        	ec_wf_tableErrorBarWidget.showMessage(tagName+getInternationalValByKey("js.ec.workflow.config.alert.fail") ,"f");//右上角提示信息
		        	var errInfo=xmlChild[0].getAttribute("mess");//报错信息
		        	bpmnPublic.formConsoleOut([{type:"error",info:tagName+getInternationalValByKey("js.ec.workflow.config.alert.failAnd")+errInfo+"！"}]);//控制台输出
		        };
	    	},
	    	error:function(e){
	    		console.log("错误",url+"报错");
	    	}
		});
    }
    //在不刷新页面的情况下重置浏览器参数，避免发布成功口刷新页面请求的还是旧数据
    function resetParameter(xmlEle){
    	var deploymentId=xmlEle.getAttribute("deploymentId");
    	var processVersion=xmlEle.getAttribute("processVersion");
    	var version=xmlEle.getAttribute("version");
    	var flowId=xmlEle.getAttribute("id");
    	var des=$("#des").val();
    	var href=window.location.href;
    	var winURL=href.split("?")[0];//页面根地址
    	var winParm=href.split("?")[1].split("&");//页面参数
    	var newParm=[];
    	//绑定重新流程版本
    	$("#processVersion").val(processVersion);
    	$("#version").val(version);	
    	$("#deploymentId").val(deploymentId);
    	$("#flowId").val(flowId);
    	$.each(winParm,function(i,item){
    		var ele=item.split("=");
    		var key=ele[0];
    		if(key=="flowId"){
    			item=key+"="+flowId;
    		}else if(key=="deploymentId"){
    			item=key+"="+deploymentId;
    		}else if(key=="version"){
    			item=key+"="+version;
    		}else if(key=="flowVersion"){
    			item=key+"="+processVersion;
    		}else if(key=="des"){
    			item=key+"="+des;
    		}
    		newParm.push(item);
    	});
    	var redirectURL=winURL+"?"+newParm.join("&");//重置页面参数
    	window.history.pushState({},0,redirectURL);
    }
    //设计模式、源码模式切换
    function switchMode(index) {
        switch (index) {
            case 0:
                //设计模式
                $(".m_codeSource").hide();//隐藏源码
                $(".djs-container svg").show();//显示流程画布
                break;
            case 1:
                //源码模式
                $(".m_codeSource").show();//显示源码
                $(".djs-container svg").hide();//隐藏流程画布
                $("#showCodeSource").html("");//清空之前的源码
                //流程图转源码
                bpmnModeler.saveXML({ format: true },function (err, xml) {
                    if (err) {
                        return console.error('could not save BPMN 2.0 diagram', err);
                    }
                    //转源码前先检查国际化编码
                    checkInternationalKey(function(){
                    	var transXml = xmlConvert.xmlConvertRorFlash.dealXMLData(createXML(xml),false);
                        $("#showCodeSource").text(transXml);
                    });
                });
                break;
        }
    }
    //打开参照流程
    function getReference() {
    	var windowStyle = "width=1000,height=650,top=120,left=120,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
    	foundation.common.callOpenWeb("getReference",windowStyle,null,true,null,"","");
    }
    //流程参照回调函数
    window.getReferenceDeploymentId=function(DeploymentId,entityCode,permission,customStaff){
    		var sameEntity = 0;//是否与参考流程在同一个实体下，不在同一视图下不保留视图，只保留权限
    		if($("#entityCode").val()==entityCode){
    			sameEntity=1;
    		}
    		var param={
    				"deploymentId":DeploymentId,
    				"referencePermission":"1",
    				"referenceSelectStaffs":"1"
    		}
    		$.ajax({
    			url:"/msService/ec/workflow/getReferenceFlow.action",
    	    	type:"post",
    	    	dataType:"text",
    	    	data:param,
    	    	success:function(data){
    	    		if(data&&data!=""){
    	                var result=resetElementId(data);//参照流程的节点id需要和参照流程区分开，避免权限配置错乱
    	    			//绑定权限、选人范围
    	    			$("#powerXml").val(result[1]);
    	    			$("#selectStaffs").val(result[2]);
    	    			var xml=bpmnPublic.string2XML(result[0]);
    	    			var newXml = xmlConvert.xmlConvertRorBpmn.dealXMLData(xml);
    	    			openDiagram(newXml,sameEntity);//打开参照流程图 
    	                paramTransform.getExistParam();//绑定参照流程的权限配置信息、选人范围
    	                $("#isEditFlag").val("1");//打开参照流程后将流程改为已编辑状态
    	    		}
    	    	},
    	    	error:function(e){
    	    		console.log("错误","/msService/ec/workflow/getReferenceFlow.action报错");
    	    	}
    		})
    	}
    //参照流程的节点id需要和参照流程区分开，避免权限配置错乱
    function resetElementId(xmlstr){
    	var result=xmlstr.split("#xml#");
    	var xml=bpmnPublic.string2XML(result[0]);
    	xmlConvert.xmlConvertRorBpmn.dealXMLData(xml);
    	if(!window.processAttribute||window.processAttribute=={}) return;
    	var tag='ref'+getRand(3);//参照随机编码
    	for(var id in window.processAttribute){
    		for(var i=0;i<result.length;i++){
    			result[i]=result[i].replace(new RegExp(id,'gm'),id+'_'+tag);//全部替换
    		}
    	}
    	return result;
    };
    //获取随机数
    function getRand(num){
        var rand="";
        for(var i = 0; i < num; i++){
            var r = Math.floor(Math.random() * 10);
            rand += r;
        }
        return rand;
    }
    /**
    * 打开流程图实例
    *
    * @param {bpmnXML} 要显示的流程图数据格式
    * @param {emptyView} 是否保留视图，emptyView==0清空视图；emptyView==1保留视图
    */
    function openDiagram(bpmnXML,emptyView) {
        // 导入流程图
        bpmnModeler.importXML(bpmnXML, function (err) {
            if (err) {
                return console.error('could not import BPMN 2.0 diagram', err);
            }
            // 访问建模组件
            var canvas = bpmnModeler.get('canvas');
            var overlays = bpmnModeler.get('overlays');
            bpmnPublic.bindDefaultAttr();//绑定默认属性
            resetInternationalKey();//参照流程的国际化编码需要和参照流程区分开，不然两者会同步修改
            if(emptyView==0) clearViewConfig();//不同实体情况下清空视图配置
        });
    }
    //重置参照视图的国际化编码
    function resetInternationalKey(){
    	if(!window.processAttribute||window.processAttribute=={}) return;
    	var data=window.processAttribute;
    	for(var id in data){
    		var item=data[id];
    		//使用立即执行函数，解决js单线程使异步函数延迟执行问题
    		(function(e){
    			bpmnPublic.saveInternationalKey(null,e.desc,function(key){
        			e.internationalKey=key;
        		});
    		})(item);	
    	}
    };
    //清空视图配置信息
    function clearViewConfig(){
    	if(!window.processAttribute||window.processAttribute=={}) return;
    	var data=window.processAttribute;
    	for(var id in data){
    		var item =data[id];
    		var vkey=[];
    		if(item.tagBpmn=="StartEvent"){
    			vkey=["doURL","viewName","viewCode"];
    		}else if(item.tagBpmn=="TaskEvent"||item.tagBpmn=="CountersignEvent"){
    			vkey=["viewUrl","viewName","viewCode","viewType","viewText"];
    		}
    		//清空视图配置
    		if(vkey&&vkey.length>0){
    			$.each(vkey,function(k,key){
    				item[key]="";
    			});
    		}
    	}
    }
    //获取参数
    function getParameter(tag,xml){
    	paramTransform.saveCurrentParam(bpmnModeler);//流程参数转换
    	var param={};
		param["des"]=$("#des").val();//描述
		param["version"]=$("#version").val();//流程版本
		param["superviseNamesMultiIDs"]=$("#superviseNamesMultiIDs").val();//督办人
		param["mobileinitiate"]=$('#mobileinitiate').val();//移动客户端发起流程
		param["recallRemainTime"]=$('#recallRemainTime').val();//撤回时效
		param["requiredTime"]=$("#requiredTime").val();//规定完成时间
		param["flowName"]=encodeURIComponent($.trim($("#flowName").val()));//流程名称
		param["mainViewViewCode"]=$('#mainViewViewCode').val();//主查看视图
		param["menuId"]=$("#menuId").val();//菜单id
	    param["flowKey"]=$.trim($("#flowKey").val());//流程编码
	    param["signature"]=$('#signature').val();//支持电子签名
		param["mobileapprove"]=$('#mobileapprove').val();//启用客户端审批
		param["flowEditFlag"]=$("#flowEditFlag").val();//是否可以修改发布的标志
		param["graduallyReject"]=$('#graduallyReject').val();//逐级驳回
		param["allowInvalid"]=$('#allowInvalid').val();//允许管理员作废
		param["entityCode"]=$("#entityCode").val();//实体编码
		param["recallAble"]=$('#recallAble').val();//可撤回
		param["namekey"]=$("#namekey").val();//流程名称国际化编码
		param["mobilequery"]="";//作废参数，传空值
		param["linkRangeChage"]="";//不需要传，传空值
		param["flowXML"]=xml;//流程xml数据
		param["entryUrl"]=$("#entryUrl").val();//实体视图(取开始活动的视图)
        //权限及选人范围
		param["activeArr"]=$("#activeArr").val();//活动
		param["selectStaffs"]=$("#selectStaffs").val();//选人范围
		param["keyDescs"]=$("#keyDescs").val();//国际化编码对照
		param["updatePowerString"]=getUpdatePowerString();//权限修改记录
		if(tag=="publish"){
			//发布
			param["menuOperateStr"]=$("#menuOperateStr").val();//活动操作信息
			param["operatePowers"]=$("#operatePowers").val();//权限
			if($("#publishType").val()=="1"){
				//修改发布
				param["publishType"]="modify";
				param["deploymentId"]=$("#deploymentId").val();//流程id
				delete param.flowEditFlag;
				delete param.flowKey;	
			}else{
				//全新发布
				param["preDeploymentId"]=$("#deploymentId").val();//流程id
			}
		}else if(tag=="save"){
			//保存
			param["deploymentId"]=$("#deploymentId").val();//流程id
			if($("#flowEditFlag").val()=="1"){
				param["publishPower"]=$("#operatePowers").val();
			}else{
				param["operatePowers"]=$("#operatePowers").val();
			}
			
		}
		// 保存发布接口添加env参数
		var env=getQueryString("env");
	    if(env) param["env"]=env;
		return param;
    }
    //权限修改记录
    function getUpdatePowerString(){
    	var prev=$("#powerBefore").val()&&$("#powerBefore").val().split(";");//上一次保存权限
    	var cur=$("#operatePowers").val()&&$("#operatePowers").val().split(";");//当前权限信息
    	var str="";
    	var record={"prev":[],"cur":[]};//排除因权限配置重复导致权限修改记录异常的情况
    	//是否为新增
    	$.each(cur,function(i,item){
    		var child=item.split("$$");
    		var powerKey=child.slice(0,3).join("$$");
    		var state=getPowerState(powerKey,prev,record["prev"]);
    		if(state.flag){
    			//修改前存在--是否修改
    			if(item!=state.item){
    				str+="update:"+powerKey+"$$from:"+state.item.replace(powerKey,"")+" to:"+item.replace(powerKey,"")+";";
    			}
    		}else{
    			//修改前不存在--新增
    			str+="add:"+item+";";
    		}
    	});
    	//是否为删除
    	$.each(prev,function(i,item){
    		var child=item.split("$$");
    		var powerKey=child.slice(0,3).join("$$");
    		var state=getPowerState(powerKey,cur,record["cur"]);
    		if(!state.flag){
    			//修改后不存在--删除
    			str+="delete:"+item+";";
    		}
    	});
    	return str;
    }
    //按权限关键字搜索查看是否有匹配项
    function getPowerState(key,data,record){
    	var isFind=false;
    	var findItem;
    	$.each(data,function(i,item){
    		var child=item.split("$$");
    		var powerKey=child.slice(0,3).join("$$");
    		var isRecord=record.indexOf(i)>-1;
    		if(key==powerKey&&!isRecord){
    			isFind=true;
    			findItem=item;
    			record.push(i);
    			return false;
    		}
    	});
    	return {"flag":isFind,"item":findItem};
    }
    //获取流程修改状态
    function getFlowChangeState(){
        var oldFlow=window.originalNodeCollection;
        var nodeData=bpmnModeler._definitions.rootElements[0].flowElements;
        if(!oldFlow||!nodeData) return;
        var flag=false;
        $.each(nodeData,function(i,item){
        	if(oldFlow.indexOf(item.id)<0){
                //修改前不存在--新加元素
                flag=true;
                return false;
             }
         });
        $.each(oldFlow,function(i,id){
           var isFind=false;
           for(var k=0;k<nodeData.length;k++){
        	   var obj=nodeData[k];
        	   if(id==obj.id){
        		   isFind=true;
        		   break;
        	   }
           }
           if(!isFind){
        	 //修改后不存在--删除元素
        	   flag=true;
        	   return false;
           }
        });
        return flag;
    }
    //检查国际化编码是否完整
    function checkInternationalKey(callback){
    	var nodeData=bpmnModeler._definitions.rootElements[0].flowElements;
    	if(!nodeData||nodeData.length==0)return;
    	//异步函数不应该用直接循环，不然项目的执行顺序是不确定的，应该用递归
    	(function loop(i) {
    		var item=nodeData[i];
    		var id=item.id;
    		var type=item.$type.split(":")[1];
    		var eleData=bpmnPublic.getElementDataById(id,type);
    		eleData.desc=item.name;
    		//未生成国际化编码的需生成国际化编码
    		if(eleData.internationalKey&&eleData.internationalKey!=""){
    			if (++i<nodeData.length) {
    				loop(i);

    			} else {
    				//执行完毕
    				if(typeof callback=="function"){
    		    		callback();
    		    	}
    			}
    		}else{
    			bpmnPublic.saveInternationalKey(null,eleData.desc,function(key){
        			eleData.internationalKey=key;
    				window.processAttribute[id]=eleData;
        			if (++i<nodeData.length) {
        				loop(i);

        			} else {
        				//执行完毕
        				if(typeof callback=="function"){
        		    		callback();
        		    	}
        			}
        		});
    		}	
    	})(0);
    };
    //创建xml
    function createXML(str) {
        var parser = new DOMParser();
        var xmlDoc = parser.parseFromString(str, "text/xml");
        return xmlDoc;
    }
    /***
     * 获取浏览器参数
     */
    function getQueryString(name) {
        var reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i");
        var r = window.location.search.substr(1).match(reg);
        if (r != null) return unescape(r[2]);
        return null;
    }
    return initToolEvent;
});