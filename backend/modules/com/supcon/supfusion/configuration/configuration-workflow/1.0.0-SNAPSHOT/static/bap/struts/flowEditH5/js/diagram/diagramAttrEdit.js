/**
 * 双击编辑逻辑
 * 修改流程图节点属性
 */
define(function (require, exports, module) {
    var bpmnPublic=require("./bpmnPublic.js");//基本方法
    var selectCrossEvent=require("./selectCrossEvent.js");//选择调用窗口事件定义
    var diConfig=require("./diagramConfig.js");//配置信息
    var typeControl=diConfig.typeControl;//类型配置信息
    var layer;
    layui.use('layer', function () {
        layer = layui.layer;
    });
    var initEvent={};//初始化事件集
    var dealEditEvent = function (element,callback) {
        var object = element.businessObject;
        var type=object.$type.split(":")[1];
        var info=getInstanceInfo(type);
        var title=info.name+getInternationalValByKey("js.ec.workflow.config.attrPanel");
        var area=info.area;
        var initData=bpmnPublic.getElementDataById(object.id,type);//获取初始值定义
        if(initData) initData.desc=object.name;//名称
        if(initData.expr) initData.expr=initData.expr.replace(new RegExp("&lt;",'gm'),'<');//逻辑表达式将"<"的转义符转换为符号
        layer.open({
            title: title,
            type: 1,
            skin: 'layui-layer-bpmn',
            area: area,
            content: template("edit"+type+"Temp", initData)
        });
        $("#international").append(template("internationTemp",{}));
        extendInterEvent(initData);//扩展国际化编码事件
        layui.use(['element','form'], function () {
            var form = layui.form;
            if(type=="SequenceFlow"){
                initData.encode=object.id;
                initData.reject=object.reject;
            }
            //初始赋值
            form.val(type,initData);
            $(".cui-noborder-input").focus();//打开的时候，光标自动定位到名称输入框;自动保存国际化编码
            silenceSaveInternationKey();//静默保存国际化编码
            form.render();//渲染
            if(initEvent[type]) initEvent[type](form,object);//初始化事件定义
            //点击提交
            form.on('submit(*)', function (data) {
                var field=data.field;
                if(field.desc!=field.international_workflow_showName||field.internationalKey!=field.workflow){
                	//国际化编码不对应时，使用国际化组件的生成值
                	field.desc=field.international_workflow_showName;
                	field.internationalKey=field.workflow;
                }
                if($.trim(field.desc)==""&&type!="SequenceFlow"){
                	bpmnPublic.showNotice(getInternationalValByKey("js.ec.workflow.config.alert.nameEmpty"),function(){
                		$(".cui-noborder-input").val("").focus();
                	});
            		return;
                }
                if(field.overdueReminders!=undefined&&field.overdueReminders=="on"){
                	if(!field.email&&!field.jabber&&!field.sms&&!field.app){
                		bpmnPublic.showNotice(getInternationalValByKey("js.ec.workflow.config.alert.noticeWarnSelect"));
                		return;
                	}
                }
                //流程效率填写需为正数
                if(field.requiredTime&&field.requiredTime!=""){
                	var reg = /^([1-9]+(\.\d+)?|0\.\d+)$/;
        			if(!reg.test(field.requiredTime)){
        				bpmnPublic.showNotice(getInternationalValByKey("js.ec.workflow.config.alert.timeValidate"));
        				return false;
        			}
                }
                saveFormData(field,object.id,type);//保存表单数据
                if(type=="SequenceFlow"){
                    var reject=field.reject;
                    callback(field.desc,reject);//回填流程名及线条类型
                }else{
                    callback(field.desc);//回填流程名
                }
                layer.closeAll();//关闭浮层
                $("#isEditFlag").val("1");//记录修改状态
                return false;
            });
            //取消操作
            var cancelCtrl = $("#cancelForm");
            cancelCtrl.on("click", function () {
                layer.closeAll();
            });
        }); 
        //阻止form表单enter键提交表单
        $(".layui-layer-bpmn form input").on("keydown",function(){
        	if(event.keyCode==13)return false;
        });
        //逻辑表达式帮助说明定义
        if(initData.hasOwnProperty("expr")){
        	var helpEle=$("#exprHelpinfo");
        	helpEle.helptip({refElm: "#exprHelpinforef", html: true , isCustom :false, width: 460 , title :getInternationalValByKey("js.ec.workflow.config.explain")});
        	helpEle.on("click",function(){
        		//帮助切换层级置与layer层之上
        		var zindex=$(".layui-layer").css("z-index");
        		$(".helptip-wrap").css("z-index",zindex+20);
        	});
        } 
    };
    //开始活动属性编辑面板事件定义
    initEvent.StartEvent= function(form) {
        var table = $("#starterTab");
        var addCtrl = $("#addLine");
        var deleteCtrl = $("#deleteLine");
        var viewCtrl=$("#viewSelect");
        registerTabOperate(table,addCtrl,deleteCtrl,"starter",form);//活动发起者table面板事件定义
        useNiceScroll($('.m-form-tableWrap'));//使用自定义滚动条
        selectUserType(table,form,addCtrl);//选择人员类型（发起者）
        selectView(viewCtrl,"start");//定义视图选择事件
    }
    //人工活动属性编辑面板
    initEvent.TaskEvent = function (form) {
        var a_table = $("#actorTab");
        var a_addCtrl = $("#addActor");
        var a_deleteCtrl = $("#deleteActor");
        var t_table = $("#triggerTab");
        var t_addCtrl = $("#addTrigger");
        var t_deleteCtrl = $("#deleteTrigger");
        var viewCtrl=$("#viewSelect");
        registerTabOperate(a_table, a_addCtrl, a_deleteCtrl, "actor", form,bindLimitRelation);//参与者table面板事件定义
        registerTabOperate(t_table, t_addCtrl, t_deleteCtrl, "event", form);//触发事件table面板事件定义
        useNiceScroll($('.m-form-tableWrap'));//使用自定义滚动条
        selectView(viewCtrl,"task");//定义视图选择事件
        selectActor(a_table,form,a_addCtrl);//定义参与者选择事件
    }
    //会签活动属性编辑面板
    initEvent.CountersignEvent = function (form) {
        var a_table = $("#actorTab");
        var a_addCtrl = $("#addActor");
        var a_deleteCtrl = $("#deleteActor");
        var t_table = $("#triggerTab");
        var t_addCtrl = $("#addTrigger");
        var t_deleteCtrl = $("#deleteTrigger");
        var s_table=$("#selectionScopeTab");
        var s_addCtrl=$("#addScope");
        var s_deleteCtrl=$("#deleteScope");
        var viewCtrl=$("#viewSelect");
        registerTabOperate(a_table, a_addCtrl, a_deleteCtrl, "actor", form,bindLimitRelation);//参与者table面板事件定义
        registerTabOperate(t_table, t_addCtrl, t_deleteCtrl, "event", form);//触发事件table面板事件定义
        registerTabOperate(s_table, s_addCtrl, s_deleteCtrl, "selectionScope", form);//选人范围table面板事件定义
        //事件监听
        form.on('checkbox(loopSign)', function (data) {
        	var ischecked=$(data.elem).is(":checked");//是否循环会签
            if(ischecked){
            	//是否循环会签选中后选人范围未选择则勾选第一项
            	var value=$("input:radio[name='loop']").val();
                if(!value||value=="1") $("input:radio[name='loop']").eq(0).prop("checked","checked");	
            }else{
            	//清空选中
            	$("input:radio[name='loop']:checked").prop("checked", false);
            }
            form.render();
        });
        form.on('radio(scope)', function (data) {
            //选人范围选中后需选中循环会签
            $("input:checkbox[name='loopSign']").prop("checked","checked");
            form.render();
        });
        useNiceScroll($('.m-form-tableWrap'));//使用自定义滚动条
        selectView(viewCtrl,"countersign");//定义视图选择事件
        selectActor(a_table,form,a_addCtrl);//定义参与者选择事件
        selectUserType(s_table,form,s_addCtrl);//选择人员类型（选人范围）
    }
    //自动活动
    initEvent.AutoEvent=function(){
    	var scriptCtrl=$("#scriptSelect");
    	var link=scriptCtrl.find(".search");
    	var $inp_name,$inp_code;
    	$inp_name=scriptCtrl.find("input[name='scriptName']");
    	$inp_code=scriptCtrl.find("input[name='scriptCode']");
    	link.on("click",function(){
    		selectCrossEvent("SCRIPT","",function(data){
    			//脚本选择回调
    			var item=data[0];
    			$inp_name&&$inp_name.val(item.name);
    			$inp_code&&$inp_code.val(item.scriptCode);
    		});	
    	});
    }
    //子流程属性编辑面板
    initEvent.SubProcessEvent=function(form){
        var table = $("#paramSetTab");
        var addCtrl = $("#addParamSet");
        var deleteCtrl = $("#deleteParamSet");
        registerTabOperate(table,addCtrl,deleteCtrl,"paramSet",form);//活动发起者table面板事件定义
        useNiceScroll($('.m-form-tableWrap'));//使用自定义滚动条
    }
    // 迁移线属性编辑面板
    initEvent.SequenceFlow=function(form,object){
        //指向作废、结束活动的迁移线不允许切换为驳回线
        var targetType=object.targetRef.$type;
        if(targetType=="bpmn:EndCancelEvent"||targetType=="bpmn:EndEvent"){
            $("#lineType option[value='1']").prop("disabled","disabled");
            form.render();
        }
        var s_table=$("#selectionScopeTab");
        var s_addCtrl=$("#addScope");
        var s_deleteCtrl=$("#deleteScope");
        registerTabOperate(s_table, s_addCtrl, s_deleteCtrl, "selectionScope", form);//选人范围table面板事件定义
        //事件监听
        form.on('checkbox(ableSelectStaff)', function (data) {
        	var ischecked=$(data.elem).is(":checked");//是否可选人
        	if(ischecked){
        		//是否可选人选中后选人范围未选择则勾选第一项
        		var value=$("input:radio[name='selectStaff']").val();
                if(!value||value=="1") $("input:radio[name='selectStaff']").eq(0).prop("checked","checked");
            }else{
            	//清空选中
            	$("input:radio[name='selectStaff']:checked").prop("checked", false);
            }
            form.render();
        });
        form.on('checkbox(requiredStaff)', function (data) {
            //必填选中后需选中是否可选人
        	var ischecked=$(data.elem).is(":checked");//是否必填
        	var ableChecked=$("input:checkbox[name='ableSelectStaff']").is(":checked");
        	if(ischecked&&!ableChecked){
        		$("input:checkbox[name='ableSelectStaff']").prop("checked","checked");
        		var value=$("input:radio[name='selectStaff']").val();
                if(!value||value=="1") $("input:radio[name='selectStaff']").eq(0).prop("checked","checked");
        	} 
            form.render();
        });
        form.on('checkbox(defaultSelectStaff)', function (data) {
            //默认上次选择人员选中后需选中是否可选人
        	var ischecked=$(data.elem).is(":checked");//是否默认上次选择人员
        	var ableChecked=$("input:checkbox[name='ableSelectStaff']").is(":checked");
        	if(ischecked&&!ableChecked){
        		$("input:checkbox[name='ableSelectStaff']").prop("checked","checked");
        		var value=$("input:radio[name='selectStaff']").val();
                if(!value||value=="1") $("input:radio[name='selectStaff']").eq(0).prop("checked","checked");
        	} 
            form.render();
        });
        form.on('radio(scope)', function (data) {
            //选人范围选中后需选中是否可选人
            $("input:checkbox[name='ableSelectStaff']").prop("checked","checked");
            form.render();
        });
        useNiceScroll($('.m-form-tableWrap'));//使用自定义滚动条
        selectUserType(s_table,form,s_addCtrl);//选择人员类型（选人范围）
    };
    /*
     * 表体增行删行操作
     * @param {table}        表体
     * @param {addCtrl}      增行控制标签
     * @param {deleteCtrl}   删行控制标签
     * @param {type}         表格类型
     * @param {form}         form实例
     */
    function registerTabOperate(table,addCtrl,deleteCtrl,type,form,callback){
        var lineSet=getLine(type);//获取行
        addCtrl.on("click", function () {
            if(type=="selectionScope"){
                //选人范围
                var scope=$("input[type='radio'][lay-filter='scope']:checked").val();
                if(scope!="5"){
                    bpmnPublic.showNotice(getInternationalValByKey("js.ec.workflow.config.alert.customSelect"));
                    return;
                }
            }
            var newLine = lineSet.newline;
            var emptyLine =table.find(".trEmpty");
            if (emptyLine && emptyLine.length > 0) {
                $(emptyLine[0]).html(newLine).removeClass("trEmpty");
            } else {
                table.append("<tr>" + newLine + "</tr>");
                table.find("tr.cur").removeClass("cur");
            }
            form.render();//渲染
        });
        //删行
        deleteCtrl.on("click", function () {
        	if(type=="selectionScope"){
                //选人范围
                var scope=$("input[type='radio'][lay-filter='scope']:checked").val();
                if(scope!="5"){
                    bpmnPublic.showNotice(getInternationalValByKey("js.ec.workflow.config.alert.customSelect"));
                    return;
                }
            }
            var curLine = $(table).find("tr.cur");
            if (!curLine || curLine.length == 0){
            	bpmnPublic.showNotice(getInternationalValByKey("js.ec.workflow.config.alert.lineSelect"));
                return;
            };
            curLine.remove();
            var len = $(table).find("tr").length;
            if (len < lineSet.num) {
                //小于最小行数补空行
                var emptyLine =lineSet.empty;
                table.append(emptyLine);
            }
        });
        //选中
        table.on("click", "tr", function (e) {
        	var isEmpty=$(this).hasClass("trEmpty");
        	table.find("tr").removeClass("cur");
        	if(!isEmpty) $(this).addClass("cur"); 
        });
        //表体渲染完成后的回调函数
        if(typeof callback=="function"){
            callback(table,form);
        }
    }
    //获取新行-新行内容配置信息
    function getLine(type){
        var line={};
        switch(type){
            case "starter"://活动发起者
                line.newline=template("starterTrTemp",{});
                line.empty='<tr class="trEmpty"><td></td><td></td><td></td></tr>';
                line.num=6;
                break;
            case "actor"://参与者
                line.newline=template("actorTrTemp",{});
                line.empty="<tr class='trEmpty'><td class='cell_10'></td><td class='cell_20'></td><td class='cell_10'></td><td class='cell_10'></td><td class='cell_20'></td><td class='cell_10'></td><td class='cell_10'></td><td class='cell_5'></td></tr>";
                line.num=11;
                break;
            case "event"://触发事件
                line.newline=template("triggerTrTemp",{});
                line.empty="<tr class='trEmpty'><td></td><td></td><td></td></tr>";
                line.num=11;
                break;
            case "selectionScope"://选人范围
                line.newline=template("selectionScopeTrTemp",{});
                line.empty='<tr class="trEmpty"><td></td><td></td><td></td><td></td><td></td><td></td><td></td></tr>';
                line.num=11;
                break;
            case "paramSet"://参数设置
                line.newline=template("paramSetTrTemp",{});
                line.empty='<tr class="trEmpty"><td></td><td></td><td></td></tr>';
                line.num=6;
                break;
        }
        return line;
    };
    //参与者限制关系绑定
    function bindLimitRelation(table,form){
        //初始绑定（主要设置当前设置条件下不可选状态）
        var tr=$(table).find("tr");
        $.each(tr,function(i,item){
            var isEmpty=$(item).hasClass("trEmpty");
            if(isEmpty){
                return false;
            }else{
                //是否无限制
                var limitElem = $(item).find("input[lay-filter='limit']");
                var unlimitElem = $(item).find("input[lay-filter='unlimit']");
                var ischecked=unlimitElem.is(":checked");
                if (ischecked) {
                    limitElem.prop("disabled", "disabled");
                    setDisableStyle(limitElem,true);//设置父级td样式
                } else {
                    checkedEle=$(item).find("input[lay-filter='limit']:checked");
                    if($("#groupRestrict").val()=="false"){
                    $.each(checkedEle,function(i,item){
                    	if($(item).attr("name")=="groupPower"){
                    		checkedEle.splice(i,1);
            			}
                    });
                    }
                    if(checkedEle.length>0){
                        unlimitElem.prop("disabled", "disabled");
                        setDisableStyle(unlimitElem,true);//设置父级td样式
                    }
                }
                form.render();
            }
        });
        //关联监听
        form.on('checkbox(unlimit)', function (data) {
            //无限制
            var limitElem = $(data.elem).closest("tr").find("input[lay-filter='limit']");
            var ischecked=$(data.elem).is(":checked");//是否无限制
            if (ischecked) {
                limitElem.prop("disabled", "disabled");
            } else {
            	if($("#groupRestrict").val()=="false"){
            		$.each(limitElem,function(i,item){
            			if($(item).attr("name")=="groupPower"){
            				limitElem.splice(i,1);
            			}
            		});
            	}
            	limitElem.prop("disabled", "");
            }
            setDisableStyle(limitElem,ischecked);//设置父级td样式
            form.render();
        });
        form.on('checkbox(limit)', function (data) {
            //限制
            var checked = $(data.elem).closest("tr").find("input[type='checkbox'][lay-filter='limit']:checked");
            var unlimitElem = $(data.elem).closest("tr").find("input[lay-filter='unlimit']");
            if($("#groupRestrict").val()=="false"){
        		$.each(checked,function(i,item){
        			if($(item).attr("name")=="groupPower"){
        				checked.splice(i,1);
        			}
        		});
        	}
            if (checked.length > 0) {
                unlimitElem.prop("disabled", "disabled");
            } else {
                unlimitElem.prop("disabled", "");
            }
            setDisableStyle(unlimitElem,checked.length > 0);//设置父级td样式
            //是否为组限制
            var name = $(data.elem).attr("name");
            if (name && name == "groupPower") {
                var ischecked = $(data.elem).is(":checked");
                if (!ischecked) {
                    //组限制未选择时取消子项radio选择
                    var ctrlRadio = $(data.elem).closest("td").find("input[type='radio'][lay-filter='limit']:checked");
                    ctrlRadio.prop("checked", "");
                } else {
                    //组限制已选择，默认选择全组(第2项)子项
                    var ctrlRadio = $(data.elem).closest("td").find("input[type='radio'][lay-filter='limit']");
                    $(ctrlRadio[1]).prop("checked", "checked");
                }
            }
            form.render();
        });
        form.on('radio(limit)', function (data) {
            //限制
            var limitElem = $(data.elem).closest("td").find("input[type='checkbox'][lay-filter='limit']");
            var ischecked = limitElem.is(":checked");
            var unlimitElem = $(data.elem).closest("tr").find("input[lay-filter='unlimit']");
            if (!ischecked) {
                limitElem.prop("checked", "checked");
                unlimitElem.prop("disabled", "disabled");//禁用无限制
                setDisableStyle(unlimitElem,true);//设置父级td样式
            }
            form.render();
        });
    }
    //保存表单数据
    function saveFormData(field,id,type){
        var prev=JSON.parse(JSON.stringify(window.processAttribute[id]));//使用深拷贝
        //存值转换
        for(var key in prev){
            var value=prev[key];
            if(field[key]==undefined) field[key]=value;//属性在表单不存在时，保持该属性值和初始配置一致；如tagBpmn
            if(typeof value=="boolean"){
                if(field[key]&&field[key]=="on"){
                    prev[key]=true;
                }else{
                    prev[key]=false;
                }
            }else{
            	prev[key]=field[key];
            }
        }
        prev.internationalKey=field.workflow;//国际化编码
        if(type=="CountersignEvent"&&prev["loopSign"]===false){
        	prev.loop="";	
        }
        if(type=="SequenceFlow"&&prev["ableSelectStaff"]===false){
        	prev.selectStaff="";	
        }
        //获取表体配置信息
        var tablist=getTableList(type);
        for(var key in tablist){
            prev[key]=tablist[key];
        }
        window.processAttribute[id]=prev;
    }
    //获取表体配置信息
    function getTableList(type){
        var result={};
        switch(type){
            case "StartEvent":
                 //活动发起者
                 var starter=getStarterTabInfo();
                 result["starter"]=starter;
                 break;
            case "TaskEvent":
                 //参与者
                 var actor=getActorTabInfo();
                 result["actor"]=actor;
                 //触发事件
                 var event =getEventTabInfo();
                 result["event"] = event;
                 break;
            case "CountersignEvent":
                //参与者
                var actor=getActorTabInfo();
                result["actor"]=actor;
                //触发事件
                var event =getEventTabInfo();
                result["event"] = event;
                //选人范围
                var selectionScope=getSelectionScopeInfo();
                result["selectionScope"] = selectionScope;
                break;
            case "SubProcessEvent":
                //参与者
                var paramSet=getParamSetTabInfo();
                result["paramSet"]=paramSet;
                break;
            case "SequenceFlow":
                //选人范围
                var selectionScope=getSelectionScopeInfo();
                result["selectionScope"] = selectionScope;
                break;

        }
        return result;
    }
    //获取活动发起者面板数据
    function getStarterTabInfo(){
        var starter=[];
        var starterTr=$("#starterTab tr");
        $.each(starterTr,function(i,item){
            var isEmpty=$(item).hasClass("trEmpty");
            if(isEmpty) return false;
            //非空情况
            var typeBtn=$(item).find("input[name='btn_userType']");
            var userType=$(item).find("td").eq(0).find("select").val();
            var typeId=typeBtn.attr("data-id");
            var typeName=typeBtn.attr("data-name");
            var arr={"userType":userType,"typeName":typeName,"typeId":typeId};
            starter.push(arr);
        });
        return starter;
    }
    //获取参与者面板数据
    function getActorTabInfo(){
        var actor = [];
        var actorTr = $("#actorTab tr");
        $.each(actorTr, function (i, item) {
            var isEmpty = $(item).hasClass("trEmpty");
            if (isEmpty) return false;
            //非空情况
            var arr = {};
            var typeBtn=$(item).find("input[name='btn_userType']");
            var postBtn=$(item).find("input[name='postOrder']");
            var personBtn=$(item).find("input[name='personOrder']");
            var ckb_group=$(item).find("input[name='groupPower']").is(":checked");
            arr.userType = $(item).find("select[name='sel_userType']").val();
            arr.typeName = typeBtn.attr("data-name");
            arr.typeId = typeBtn.attr("data-id");
            arr.groupPower = ckb_group;
            arr.assignPositions=postBtn.attr("data-val");//指定岗位
            arr.assignStaffs=personBtn.attr("data-val");//指定人员
            var keyItem = ["positionPower",'groupPower',"assignPositionPower", "assignStaffPower", "unLimitPower"];
            for (var k = 0; k < keyItem.length; k++) {
                var key = keyItem[k];
                arr[key] = $(item).find("input[name=" + key + "]").is(":checked");
            }
            actor.push(arr);
        });
        return actor;
    }
    //获取触发事件面板数据
    function getEventTabInfo(){
        var eventArr=[];
        var eventTr=$("#triggerTab tr");
        $.each(eventTr,function(i,item){
            var isEmpty=$(item).hasClass("trEmpty");
            if(isEmpty) return false;
            //非空情况
            var name=$(item).find("td").eq(0).find("input").val();
            var event=$(item).find("td").eq(1).find("select").val();
            var triggerName=$(item).find("td").eq(2).find("input").val();
            var arr={"name":name,"triggerPoint":event,"triggerName":triggerName};
            eventArr.push(arr);   
        });
        return eventArr;
    }
    //获取选人范围面板数据
    function getSelectionScopeInfo(){
        var selectionScope=[];
        var selectionScopeTr=$("#selectionScopeTab tr");
        $.each(selectionScopeTr, function (i, item) {
            var isEmpty = $(item).hasClass("trEmpty");
            if (isEmpty) return false;
            //非空情况
            var typeBtn=$(item).find("input[name='btn_userType']");
            var userType=$(item).find("td").eq(0).find("select").val();
            var typeId=typeBtn.attr("data-id");
            var typeName=typeBtn.attr("data-name");
            var groupName=$(item).find("td").eq(3).find("input").val();
            var order=$(item).find("td").eq(4).find("input").val();
            var arr={"userType":userType,"typeId":typeId,"typeName":typeName,"groupName":groupName,"order":order};
            selectionScope.push(arr);
        });
        return selectionScope;
    }
    //获取参数设置面板数据
    function getParamSetTabInfo(){
        var paramSet=[];
        var paramSetTr=$("#paramSetTab tr");
        $.each(paramSetTr,function(i,item){
            var isEmpty=$(item).hasClass("trEmpty");
            if(isEmpty) return false;
            //非空情况
            var val=$(item).find("td").eq(0).find("input").val();
            var subvar=$(item).find("td").eq(1).find("input").val();
            var type=$(item).find("td").eq(2).find("select").val();
            var arr={"var":val,"subvar":subvar,"type":type};
            paramSet.push(arr);
        });
        return paramSet;
    }
    //设置禁用单元格样式
    function setDisableStyle(elem,isDisable){
        if(!elem||elem.length==0) return;
        $.each(elem,function(i,item){
            var $td=$(item).closest("td");
            $td.css("opacity",isDisable?"0.7":"1");
        });
    }
    //定义视图选择事件
    function selectView(viewCtrl,type){
    	var link=viewCtrl;//#bug1933 点输入框的空白处也需要弹出参照页面
    	var $inp_name,$inp_code,$inp_type,$inp_url,$inp_text;
    	if(type=="start"){
    		//开始活动
    		$inp_name=viewCtrl.find("input[name='viewName']");
    		$inp_code=viewCtrl.find("input[name='viewCode']");
    		$inp_url=viewCtrl.find("input[name='doURL']");
    		$inp_text=viewCtrl.find("input[name='viewText']");
    	}else{
    		//人工、会签活动
    		$inp_name=viewCtrl.find("input[name='viewName']");
    		$inp_code=viewCtrl.find("input[name='viewCode']");
    		$inp_url=viewCtrl.find("input[name='viewUrl']");
    		$inp_type=viewCtrl.find("input[name='viewType']");
    		$inp_text=viewCtrl.find("input[name='viewText']");
    	}
    	link.on("click",function(){
    		selectCrossEvent("VIEW","env=runtime",function(data){
    			var item=data[0];
    			if(type=="start"&&item.viewType!="EDIT"){
    				bpmnPublic.showNotice(getInternationalValByKey("js.ec.workflow.config.alert.editViewSet"));
    				return;
    			}
    			//视图选择回调
    			$inp_name&&$inp_name.val(item.viewName);
    			$inp_code&&$inp_code.val(item.viewCode);
    			$inp_url&&$inp_url.val(item.viewUrl);
    			$inp_type&&$inp_type.val(item.viewType);
    			$inp_text&&$inp_text.val(item.viewName+"（"+item.viewCode+")");//显示视图名称、编码
    		});	
    	});
    };
    //定义参与者选择事件
    function selectActor(table,form,addCtrl){
    	selectUserType(table,form,addCtrl);//人员类型选择
    	//指定岗位
    	table.on("click",".btn-choose[name='postOrder']",function(){
    		var $this=$(this);
    		var $tr=$(this).closest("tr");
    		var $this_ckb=$this.closest("td").find("input[name='assignPositionPower']");
    		var $unLimit=$tr.find("input[name='unLimitPower']");
    		var assignPositonIds=getAssignPositonIds($this);//已选择的指定岗位
    		var param="assignPositonIds="+assignPositonIds;
    		selectCrossEvent("assignedPosition",param,function(data){
    			var strVal="";
    			$.each(data,function(i,item){
    				strVal+=item.positionID+","+item.positionName+","+item.haveFalg+";";
    			});
    			strVal=strVal.substr(0,strVal.length-1);
    			$this.attr("data-val",strVal);
    			//勾选指定岗位
    			$this_ckb.prop("checked","checked");
    			//无限制设置为不可选
    			$unLimit.prop("disabled","disabled");
    			setDisableStyle($unLimit,true);
    			form.render();
    		});
    	 });
    	 //指定人员
    	table.on("click",".btn-choose[name='personOrder']",function(){
    		var $this=$(this);
    		var $tr=$(this).closest("tr");
    		var $this_ckb=$this.closest("td").find("input[name='assignStaffPower']");//是否选中指定人员
    		var $unLimit=$tr.find("input[name='unLimitPower']");
    		var assignStaffs=getAssignStaffs($this);//已选择的指定人员
    		var param="assignStaffs="+assignStaffs;
    		selectCrossEvent("assignUser",param,function(data){
    			var strVal="";
    			$.each(data,function(i,item){
    				strVal+=+item.staffID+","+item.staffName+";";
    			});
    			strVal=strVal.substr(0,strVal.length-1);
    			$this.attr("data-val",strVal);
    			//勾选指定人员
    			$this_ckb.prop("checked","checked");
    			//无限制设置为不可选
    			$unLimit.prop("disabled","disabled");
    			setDisableStyle($unLimit,true);
    			form.render();
    		});
    	 });
    };
    //获取已选择的指定岗位
    function getAssignPositonIds(obj){
    	var data=$(obj).attr("data-val");
    	var arr=[],str="";
    	if(data&&data!=""){
    		var objs=data.split(";");
    		$.each(objs,function(i,item){
    			var params=item.split(",");
    			arr.push(params[0]+","+params[2]);
    		});	
    		str=arr.join(";");
    	}
    	return str;
    }
    //获取已选择的指定人员
    function getAssignStaffs(obj){
    	var data=$(obj).attr("data-val");
    	var arr=[],str="";
    	if(data&&data!=""){
    		var objs=data.split(";");
    		$.each(objs,function(i,item){
    			var params=item.split(",");
    			arr.push(params[0]);
    		});	
    		str=arr.join(",");
    	}
    	return str;
    }
    //选择人员类型
    function selectUserType(table,form,addCtrl){
    	//人员类型选择
    	table.on("click",".btn-choose[name='btn_userType']",function(e){
    		var $tr=$(this).closest("tr");
    		var $select=$tr.find("select[name='sel_userType']")
    		var $this=$(this);
    		var type=$select.val();
    		var index=$tr.index();
    		selectCrossEvent(type,"",function(data){
    			var dealD=popRepeatPower(data,type,table);//排除重复的权限
    			$.each(dealD,function(n,item){
    				var name=(type=="USER")?item.staffName:item.name;
    				var id=(type=="USER")?item.userID:item.id;
    				//人员选择回调
        			if(n==0){
        				$this.attr({"data-id":id,"data-name":name});
            			$tr.find("td").eq(1).html("<p class='nowrap' title="+name+">"+name+"</p>");
            			$select.prop("disabled","disabled");	
        			}else{
        				//创建新行
        				addCtrl.trigger("click");
        				var getTr=table.find("tr").not("tr.trEmpty");//排除空行
        				var lastTr=getTr.eq(getTr.length-1);//取最后一行即新行
        				lastTr.find("td").eq(1).html("<p class='nowrap' title="+name+">"+name+"</p>");
        				lastTr.find(".btn-choose[name='btn_userType']").attr({"data-name":name,"data-id":id});
        				lastTr.find("select[name='sel_userType']").prop("disabled","disabled").val(type);	
        			}	
    			});
    			form.render();
    		});
    	});	
    }
    //排除重复的权限配置(人员、部门、岗位、角色)
    function popRepeatPower(data,type,table){
    	var getTr=table.find("tr").not("tr.trEmpty");//排除空行
    	var curPower=[];
    	var norepeat=[];
    	$.each(getTr,function(i,item){
    		var $btn=$(item).find(".btn-choose[name='btn_userType']");
    		var $select=$(item).find("select[name='sel_userType']")
    		var userId=$btn.attr("data-id");//人员id;
    		var userType=$select.val();
    		if(userId&&userId!="") curPower.push(userType+"&"+userId);
    	});
    	$.each(data,function(i,obj){
			var id=(type=="USER")?obj.userID:obj.id;
			var tag=type+"&"+id.toString();
			var flag=curPower.indexOf(tag)>-1;
			if(!flag) norepeat.push(obj);
    	});
    	return norepeat;	
    }
    //扩展国际化编码事件
    function extendInterEvent(data){
    	var box=$("#international");
    	var inp=box.find(".m_wrap input");//国际化控制
    	var showKey=inp[0];//国际化编码值
    	var editVal=inp[1];//文本编辑
    	var language=inp[3];//切换语言
    	$(showKey).val(data.internationalKey);
    	$(editVal).val(data.desc);
    	//事件注册补充
    	$(editVal).on("change",function(){
    		var value=$(this).val();
    		var keyVal=$(showKey).val();
    		box.find("input[name='desc']").val(value);
    		box.find("input[name='internationalKey']").val(keyVal);
    	});
    	$(language).on("click",function(){
    		//语言切换层级置与layer层之上
    		var zindex=$(".layui-layer").css("z-index");
    		$(".InterDivContainer_class").css("z-index",zindex+20);
    	});
    };
    //使用自定义滚动条
    function useNiceScroll(obj) {
        obj.niceScroll({
            cursorcolor: "rgba(0,0,0,0.3)",//滚动条的颜色
            cursoropacitymax: 1, //滚动条的透明度，从0-1
            touchbehavior: false, //使光标拖动滚动像在台式电脑触摸设备
            cursorwidth: "5px", //滚动条的宽度
            cursorborder: "0", // 游标边框css定义
            cursorborderradius: "5px",//以像素为光标边界半径  圆角
            autohidemode: false, //是否隐藏滚动条  true的时候默认不显示滚动条，当鼠标经过的时候显示滚动条
            zindex: "auto",//给滚动条设置z-index值
            railpadding: { top: 0, right: 0, left: 0, bottom: 0 }//滚动条的位置
        });
    }
      /*
     * 类型对照关系
     * @param {type}        类型
     */
    function getInstanceInfo(type){
        var name,area;
        $.each(typeControl,function(i,item){
            if(item.tagBpmn==type||item.lowercase==type){
                name=item.name;
                area=item.area;
                return false;
            }
        });
        return {name:name,area:area};
    }
    //静默保存国际化编码
    function silenceSaveInternationKey(){
    	var name=$(".cui-noborder-input").val();
    	$(".cui-noborder-input").val(name).trigger("change");//重新赋值触发保存	
        $("#international_workflow_loading").css({"opacity":0,"filter":"alpha=0"});
        setTimeout(function(){
        	$("#international_workflow_loading").css({"opacity":100,"filter":"alpha=100"});
        },1500);
    }
    //防止冒泡事件
    function stopPropagation(e){

        e=window.event||e;

        if(document.all){  //只有ie识别

            e.cancelBubble=true;

        }else{

            e.stopPropagation();

        }

    }
    return dealEditEvent;
});
