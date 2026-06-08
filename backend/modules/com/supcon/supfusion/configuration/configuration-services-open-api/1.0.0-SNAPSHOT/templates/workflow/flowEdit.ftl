<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
	<meta http-equiv="PRAGMA" content="text/html; charset=UTF-8;NO-CACHE" />
	<title>${getText('${flowName!}')}</title>
	<@ec_flow_edit />
</head>
<body>
<script type="text/javascript">
	//localFlag：用于指定loadpanel的类型，默认为true
	var localLoadPanelWidget = null;
	function createLoadPanel(localFlag,containerEl,_config){
		try{
			if(localFlag == undefined) localFlag = true;
			if(localFlag == false){
				//containerEl = (containerEl == undefined) ? window : containerEl;
				if(!_config) {
					_config = {container:containerEl/*,opacity:50,bgColor:"#666666"*/};
				} else {
					_config.container = containerEl;
				}
				if(!window.containerLoadPanelWidget){
					window.containerLoadPanelWidget = new CUI.loading(_config);
			    }
			}else{
				if(localLoadPanelWidget == null) localLoadPanelWidget = new CUI.loading({local:localFlag,prevEl:'localLoadPanel',paddingLeft:0});
			}
		}catch(e){}
	}
	//关闭的时候，由于无法判断目前出现的loadpanel类型，因此local和container两者都去关闭一下，并将错误捕获
	function closeLoadPanel(){
		try{
			localLoadPanelWidget.close();
			localLoadPanelWidget = null;
		}catch(e){localLoadPanelWidget = null;}
		try{
			window.containerLoadPanelWidget.close();
			window.containerLoadPanelWidget = null;
		}catch(e){window.containerLoadPanelWidget = null;}
	}
	</script>
	<@s.form id="workFlowForm">
	<@s.hidden name="flowId" id="flowId"></@s.hidden>
	<@s.hidden name="deploymentId" id="deploymentId"></@s.hidden>
	<@s.hidden name="flowName" id="flowName"></@s.hidden>
	<@s.hidden name="namekey" id="namekey"></@s.hidden>
	<@s.hidden name="requiredTime" id="requiredTime"></@s.hidden>
	<@s.hidden name="mobilequery" id="mobilequery"></@s.hidden>
	<@s.hidden name="mobileinitiate" id="mobileinitiate"></@s.hidden>
	<@s.hidden name="mobileapprove" id="mobileapprove"></@s.hidden>
	<@s.hidden name="allowInvalid" id="allowInvalid"></@s.hidden>
	<@s.hidden name="graduallyReject" id="graduallyReject"></@s.hidden>
	<@s.hidden name="recallAble" id="recallAble"></@s.hidden>
	<@s.hidden name="recallRemainTime" id="recallRemainTime"></@s.hidden>
	<@s.hidden name="mainViewViewCode" id="mainViewViewCode"></@s.hidden>
	<@s.hidden name="flowKey" id="flowKey"></@s.hidden>
	<@s.hidden name="des" id="des"></@s.hidden>
	<@s.hidden name="menuId" id="menuId"></@s.hidden>
	<@s.hidden name="version" id="version"></@s.hidden>
	<@s.hidden name="processVersion" id="processVersion"></@s.hidden>
	<@s.hidden name="entityCode" id="entityCode"></@s.hidden>
	<@s.hidden name="moduleCode" id="moduleCode"></@s.hidden>
	<@s.hidden name="powerXml" id="powerXml"></@s.hidden>
	<@s.hidden name="activeArr" id="activeArr"></@s.hidden>
	<@s.hidden name="flowEditFlag" id="flowEditFlag"></@s.hidden>
	<@s.hidden name="superviseNamesMultiIDs" id="superviseNamesMultiIDs"></@s.hidden>
	<@s.hidden name="systemVersion" id="systemVersion"></@s.hidden>
	<@s.hidden name="selectStaffs" id="selectStaffs"></@s.hidden>
	<@s.hidden name="signature" id="signature"></@s.hidden>
	
	</@s.form>
	<div id="workflowFlash" align="center" style="width:100%;height:100%;">
	</div>
	
	<script type="text/javascript" >
CUI.Dialog.toggleAllButton('workFlowForm',null,true);
//指定岗位
function setAssignedPositions(objArr){
	
	var str="";
	
	for(var i=0;i<objArr.length;i++){
		var obj=objArr[i];
		str+=";"+obj.positionID+","+obj.positionName+","+obj.haveFalg;
	}
	str=str.substr(1);
	infosoftglobal.FusionChartsUtil.getChartObject("bpm_console").setAssignPosition(str);
}
//指定人员
function setAssignedStaffs(objArr){

	var str="";
	for(var i=0;i<objArr.length;i++){
		
		var obj=objArr[i];
		str+=";"+obj.staffID+","+obj.staffName;
	}
	str=str.substr(1);

	infosoftglobal.FusionChartsUtil.getChartObject("bpm_console").setAssignUser(str);
}

//人员回调
function setUsers(objArr){
	var str="";
	for(var i=0;i<objArr.length;i++){
		
		var obj=objArr[i];
		str+=";USER,"+obj.userID+","+obj.userName+","+obj.staffName;
		if(obj.groupName){
			str += "," + obj.groupName;
		}
	}
	str=str.substr(1);
	infosoftglobal.FusionChartsUtil.getChartObject("bpm_console").setUsers(str);
}

//角色回调
function setRoles(objArr){
	var str="";
	for(var i=0;i<objArr.length;i++){
		var obj=objArr[i];
		str+=";ROLE,"+obj.id+","+obj.name;
	}
	str=str.substr(1);
	infosoftglobal.FusionChartsUtil.getChartObject("bpm_console").setUsers(str);
}

//部门回调

function setDepts(objArr){
	var str="";
	for(var i=0;i<objArr.length;i++){
		var obj=objArr[i];
		str+=";DEPTMENT,"+obj.id+","+obj.name;
	}
	str=str.substr(1);
	infosoftglobal.FusionChartsUtil.getChartObject("bpm_console").setUsers(str);
}
//岗位回调

function setPositions(objArr){
	var str="";
	for(var i=0;i<objArr.length;i++){
		var obj=objArr[i];
		str+=";POSITION,"+obj.id+","+obj.name;
	}
	str=str.substr(1);
	infosoftglobal.FusionChartsUtil.getChartObject("bpm_console").setUsers(str);
}

function selectUser(mode,parameters){
	if(parameters!=""){
		parameters+="&";
	}
	parameters+="unassignStaffSupport=true";
	
	if(parameters != null && parameters.indexOf("entityCode") == -1){
		var entityCode = $("#entityCode").val();
		parameters+="&entityCode="+entityCode;
	}
	if($("#workFlowForm_companyType").val()=='GROUP'){
		parameters+="&crossCompanyFlag=true";
	}
	var url="";
	var windowStyle = "width=1000,height=650,top=120,left=120,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
	
	if(mode=="USER"){
		foundation.common.callOpenWeb("user",windowStyle,null,true,null,"setUsers",parameters);
	}else if(mode=="ROLE"){
		foundation.common.callOpenWeb("role",windowStyle,null,true,null,"setRoles",parameters);
	}else if(mode=="POSITION"){
		foundation.common.callOpenWeb("position",windowStyle,null,true,null,"setPositions",parameters);
	}else if(mode=="DEPTMENT"){
		foundation.common.callOpenWeb("department",windowStyle,null,true,null,"setDepts",parameters);
	}else if(mode=="WORKGROUP"){
		foundation.common.callOpenWeb("group",windowStyle,null,true,null,"setGroup",parameters);
	}else if(mode=="assignPosition"){
		foundation.common.callOpenWeb("assignedPosition",windowStyle,null,true,null,"setAssignedPositions",parameters);
	}else if(mode=="assignUser"){
		foundation.common.callOpenWeb("assignedStaff",windowStyle,null,true,null,"setAssignedStaffs",parameters);
	}else if(mode=="script"){
		foundation.common.callOpenWeb("script",windowStyle,null,true,null,"setScript",parameters);
	}
	
}
function setScript(objArr){
	var str="";
	for(var i=0;i<objArr.length;i++){
		var obj=objArr[i];
		
		str+=";"+obj.name+","+obj.scriptCode;
	}
	str=str.substr(1);
	infosoftglobal.FusionChartsUtil.getChartObject("bpm_console").setScript(str);
}

function setGroup(objArr){
	var str="";
	for(var i=0;i<objArr.length;i++){
		var obj=objArr[i];
		
		str+=";WORKGROUP,"+obj.id+","+obj.name;
	}
	str=str.substr(1);
	infosoftglobal.FusionChartsUtil.getChartObject("bpm_console").setUsers(str);
	
}
function openViewList(parameters){

	var windowStyle = "width=1000,height=650,top=120,left=120,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
	foundation.common.callOpenWeb("viewList",windowStyle,null,true,null,"getViewInfo",parameters);
	
}
function getViewInfo(objArr){
	var str="";
	for(var i=0;i<objArr.length;i++){
		var obj=objArr[i];
		str+=obj.viewName+","+obj.viewUrl+","+obj.viewType+","+obj.viewCode;
	}
	infosoftglobal.FusionChartsUtil.getChartObject("bpm_console").setActionUrl(str);
}
var jsReady = false;
function isReady() {
	return jsReady;
}

function showEdit(){
	var ec_flow_edit_swf_url = "<@ec_flow_edit_swf_url />";
    pageInit();
    var myChart = new FusionCharts(ec_flow_edit_swf_url, "bpm_console",'100%','100%','#FFFFFF',null,null,null,'${getCurrent('language')}');
	myChart.render("workflowFlash");
	
}
function pageInit() {

 jsReady = true;
}	

showEdit();
//设置流程的属性---id,name,key,memo
//修改流程
function setFlowFc(deploymentId,version,flowId,name,key,entityCode,menuId,flowEditFlag,namekey,requiredTime,systemVersion,processVersion,moduleCode,dec,mobilequery,mobileinitiate,mobileapprove,allowInvalid,graduallyReject,recallAble,recallRemainTime,mainViewViewCode,env,signature){
	setTimeout(function(){infosoftglobal.FusionChartsUtil.getChartObject("bpm_console").setFlow(deploymentId,version,flowId,name,key,entityCode,menuId,flowEditFlag,namekey,requiredTime,systemVersion,processVersion,moduleCode,dec,mobilequery,mobileinitiate,mobileapprove,allowInvalid,graduallyReject,recallAble,recallRemainTime,mainViewViewCode,env,signature)},1500);
}
//新建流程
function setNewFlowFc(name,key,memo,entityCode,menuId,namekey,requiredTime,systemVersion,moduleCode,mobilequery,mobileinitiate,mobileapprove,allowInvalid,graduallyReject,recallAble,recallRemainTime,mainViewViewCode,env,signature){
	setTimeout(function(){infosoftglobal.FusionChartsUtil.getChartObject("bpm_console").setNewFlow(name,key,memo,entityCode,menuId,namekey,requiredTime,systemVersion,moduleCode,mobilequery,mobileinitiate,mobileapprove,allowInvalid,graduallyReject,recallAble,recallRemainTime,mainViewViewCode,env,signature)},1500);
}
//赋值flow信息,流程内部调用，防止流程尚未加载完就调用
function setFlowInfo(){
	setPublishedAtvice();
	if($("#deploymentId").val()!=""){
		setFlowFc($("#deploymentId").val(),$("#version").val(),$("#flowId").val(),$("#flowName").val(),$("#flowKey").val(),$("#entityCode").val(),$("#menuId").val()
		,$("#flowEditFlag").val(),$("#namekey").val(),$("#requiredTime").val(),$("#systemVersion").val(),$("#processVersion").val(),$("#moduleCode").val(),$("#des").val(),$("#mobilequery").val(),$("#mobileinitiate").val(),$("#mobileapprove").val(),$('#allowInvalid').val(),$('#graduallyReject').val(),$('#recallAble').val(),$('#recallRemainTime').val(),$('#mainViewViewCode').val(),"${env!'runtime'}",$('#signature').val());
	}else{
		
		setNewFlowFc($("#flowName").val(),$("#flowKey").val(),$("#des").val(),$("#entityCode").val(),$("#menuId").val(),$("#namekey").val(),$("#requiredTime").val(),$("#systemVersion").val(),$("#moduleCode").val(),$("#mobilequery").val(),$("#mobileinitiate").val(),$('#mobileapprove').val(),$('#allowInvalid').val(),$('#graduallyReject').val(),$('#recallAble').val(),$('#recallRemainTime').val(),$('#mainViewViewCode').val(),"${env!'runtime'}",$('#signature').val());
	}
	
}
//流程调用督办人设置，流程内部调用，防止流程尚未加载完就调用
function getFlowSupervise(){
	infosoftglobal.FusionChartsUtil.getChartObject("bpm_console").setFlowSupervise($("#superviseNamesMultiIDs").val());
}
//流程调用迁移线选人设置，流程内部调用，防止流程尚未加载完就调用
function getSelectUserXml(){
	infosoftglobal.FusionChartsUtil.getChartObject("bpm_console").setSelectUserXml($("#selectStaffs").val());
}
//流程调用权限的xml,流程内部调用，防止流程尚未加载完就调用
function getFlowPowerXml(){
	
	infosoftglobal.FusionChartsUtil.getChartObject("bpm_console").setFlowPowerXml($("#powerXml").val());
}
//流程调用权限的xml,流程内部调用，防止流程尚未加载完就调用
function setPublishedAtvice(){
	infosoftglobal.FusionChartsUtil.getChartObject("bpm_console").setPublishedAtvice($("#activeArr").val());
}


function refleshPage(){
	if(opener.CUI('#ec_wf_table').length>0){
		opener.ec.wf.refleshDataTable();
	}
}
//注册命名空间
CUI.ns("foundation.international");
function getInterFrame(keyname){
		var	key=keyname;
		var modulecode = keyname.split(".")[0];
			
	  	var params="moduleCode="+modulecode+"&name=workflow&callBackFuncName=foundation.international.getWorkflowInternational&openType=page";
	  	params+="&key="+encodeURIComponent(key);
		var windowStyle = "width=1000,height=650,top=120,left=120,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
		foundation.common.callOpenWeb("getInterFrame",windowStyle,null,true,null,"foundation.international.addCallbackworkflow",params);	
}

foundation.international.addCallbackworkflow = function(res){
	infosoftglobal.FusionChartsUtil.getChartObject("bpm_console").getInterValue(res.value);
}

function getReference(){
	var windowStyle = "width=1000,height=650,top=120,left=120,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
	foundation.common.callOpenWeb("getReference",windowStyle,null,true,null,"","");
}
function getReferenceDeploymentId(DeploymentId,entityCode,permission,customStaff){
	var sameEntity = 0;//是否与参考流程在同一个实体下
	if($("#entityCode").val()==entityCode){
		sameEntity=1;
	}
	infosoftglobal.FusionChartsUtil.getChartObject("bpm_console").getReference(DeploymentId,sameEntity,permission,customStaff);
}
function openProgressBar(){
	CUI.Dialog.toggleAllButton('workFlowForm',null,true);
}
function closeProgressBar(){
	if(CUI.Dialog)CUI.Dialog.toggleAllButton('workFlowForm', true);
}
function refresh(deploymentId,flowId,flowVersion){
	if(CUI('#deploymentId').val()==''){
		var href = window.location.href;
	    href = href.replace("deploymentId=&","deploymentId="+deploymentId+"&");
		if(flowId!=null){
	    	href = href.replace("flowId=&","flowId="+flowId+"&");
		}
	    href = href.replace("flowVersion=&","flowVersion="+flowVersion+"&");
	    window.location.href = href;
	}
}
</script>
	
</body>
</html>
