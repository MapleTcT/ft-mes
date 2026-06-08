<#if (Parameters.openType)?default('page') != 'dialog'>
<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>${getText('foundation.user.select')}</title>
<@maincss/>
<@mainjs/>
</head>
<body id="dialog_page">
</#if>

<@loadpanel></@loadpanel>

<#if unassignUserSupport??>
	<#assign tabhide = false>
<#else> 
	<#assign tabhide = true>
</#if>
<@errorbar id="userListFramesetErrorbar" /> 
<#if (Parameters.openType)?default('page') != 'dialog'>
<div>
</#if>
<#if Parameters?? && Parameters.showRange??>
	<#assign showRangeParam = "showRange=" + Parameters.showRange +"&">
</#if>

 <#if (Parameters.openType)?default('page') != 'dialog'>
 	<div id="userListTab" class="etv-navset" style="position:relative;padding-top:3px \9;">
 <#else> 
    <div id="userListTab" class="etv-navset" style="overflow-y:hidden;">
 </#if>

  <ul class="etv-nav">
	  <#if groupCrossCompanyFlag?string('true','false')=='true'>
	  	<li ajax="true" useListObj="datatable_department_userListTable" surl="/msService/ec/foundation/user/common/departmentUserList?${showRangeParam!}crossCompanyFlag=${crossCompanyFlag?string('true','false')}&closePage=${(closePage)?string('true','false')}&callBackFuncName=${callBackFuncName!}&multiSelect=${multiSelect?string('true','false')}&openType=${(Parameters.openType)!'page'}&enableAddToGroup=${(enableAddToGroup!false)?string}">${getHtmlText('foundation.department.according')}</li>
	  	<li ajax="true" useListObj="datatable_position_userListTable" surl="/msService/ec/foundation/user/common/positionUserList?${showRangeParam!}crossCompanyFlag=${crossCompanyFlag?string('true','false')}&closePage=${closePage?string('true','false')}&callBackFuncName=${callBackFuncName!}&multiSelect=${multiSelect?string('true','false')}&openType=${(Parameters.openType)!'page'}&enableAddToGroup=${(enableAddToGroup!false)?string}">${getHtmlText('foundation.position.according')}</li>
	   	<#if unassignUserSupport??&&unassignUserSupport>
	   		<li ajax="true" useListObj="datatable_unassign_userListTable" surl="/msService/ec/foundation/user/common/unassignCompanyUser?${showRangeParam!}crossCompanyFlag=${crossCompanyFlag?string('true','false')}&closePage=${closePage?string('true','false')}&callBackFuncName=${callBackFuncName!}&systemAdminFlag=${systemAdminFlag?string('true','false')}&openType=${(Parameters.openType)!'page'}&multiSelect=${multiSelect?string('true','false')}&enableAddToGroup=${(enableAddToGroup!false)?string}">${getHtmlText('foundation.staff.unassignstaffselect')}</li>
	   	</#if>
	  <#else>
	   	<li ajax="true" useListObj="datatable_department_userListTable" surl="/msService/ec/foundation/user/common/departmentUserList?${showRangeParam!}crossCompanyFlag=${crossCompanyFlag?string('true','false')}&closePage=${(closePage)?string('true','false')}&callBackFuncName=${callBackFuncName!}&multiSelect=${multiSelect?string('true','false')}&openType=${(Parameters.openType)!'page'}&enableAddToGroup=${(enableAddToGroup!false)?string}">${getHtmlText('foundation.department.according')}</li>
	   	<li ajax="true" useListObj="datatable_position_userListTable" surl="/msService/ec/foundation/user/common/positionUserList?${showRangeParam!}crossCompanyFlag=${crossCompanyFlag?string('true','false')}&closePage=${closePage?string('true','false')}&callBackFuncName=${callBackFuncName!}&multiSelect=${multiSelect?string('true','false')}&openType=${(Parameters.openType)!'page'}&enableAddToGroup=${(enableAddToGroup!false)?string}">${getHtmlText('foundation.position.according')}</li>
	   	<#if unassignUserSupport??&&unassignUserSupport>
	   		<li ajax="true" useListObj="datatable_unassign_userListTable" surl="/msService/ec/foundation/user/common/unassignCompanyUser?${showRangeParam!}crossCompanyFlag=${crossCompanyFlag?string('true','false')}&closePage=${closePage?string('true','false')}&callBackFuncName=${callBackFuncName!}&systemAdminFlag=${systemAdminFlag?string('true','false')}&openType=${(Parameters.openType)!'page'}&multiSelect=${multiSelect?string('true','false')}&enableAddToGroup=${(enableAddToGroup!false)?string}">${getHtmlText('foundation.staff.unassignstaffselect')}</li>
	   	</#if>
	  </#if>
	  <#if unUserDefinedGroupUser?? && unUserDefinedGroupUser>
	  	<li ajax="true" useListObj="datatable_groupMemberUserList" surl="/msService/ec/foundation/user/common/userDefinedGroupUserList?&${showRangeParam!}crossCompanyFlag=${crossCompanyFlag?string('true','false')}&closePage=${closePage?string('true','false')}&callBackFuncName=${callBackFuncName!}&companyId=${companyId!}&openType=${(Parameters.openType)!'page'}&multiSelect=${(Parameters.multiSelect)!'false'}&systemAdminFlag=${systemAdminFlag?string('true','false')}">${getHtmlText('foundation.customGroup.userDefined')}</li>
	  </#if>
	  
  </ul>
  <div class="etv-content">
   <div frame_name="etv_frame_departmentUser"></div>
   <div frame_name="etv_frame_positionUser"></div>
   <#if unassignUserSupport?? && unassignUserSupport><div frame_name="etv_frame_unassignCompanyUser"></div></#if>
   <#if unUserDefinedGroupUser?? && unUserDefinedGroupUser><div frame_name="etv_frame_userDefinedGroupUser"></div></#if>
  </div>

  <#if (Parameters.openType)?default('page') == 'page'>
  	<@frame id="department_Button" region="south" height=28>
    	<div align="right" style="margin-right:20px;position:absolute;bottom:0px;right:0;z-index:100;">
    		<#if closePage?exists&&closePage==false>
    			<a id="bottom-submit" class="cui-btn-blue" style="margin-right:10px;"><span class="btn_r">${getHtmlText('common.button.choose')}</span></a>
    		<#else>
    			<a id="bottom-submit" class="cui-btn-blue" style="margin-right:10px;"><span class="btn_r">${getHtmlText('common.button.chooseandclose')}</span></a>
    		</#if>
    		<a id="bottom-reset" onclick="CUI.closeWindow()" class="cui-btn-blue" style="margin-right:10px;"><span class="btn_r">${getHtmlText('common.button.cancel')}</span></a>
    	</div>
  	</@frame>
  </#if>

 </div>

<#if (Parameters.openType)?default('page') != 'dialog'>
</div>
</#if>

<script type="text/javascript" charset="utf-8" language="javascript">
(function(){
  CUI.ns("foundation.user");

  var userListTabWidget;
  	userListTabWidget = new CUI.TabView("userListTab",{
	isscroll : true,
	
	<#if (Parameters.openType)?default('page') == 'frame'>
	iframe: true,
	</#if>
	
	//height:415,
	removeable : false
 });

 //双击或点击选择按钮进行选取
 foundation.user.sendBackUserInfo = function(event,oRow){
  <#if (Parameters.openType)?default('page') == 'frame'>
	var frameWindow =  eval( userListTabWidget.activeContent.getAttribute( 'frame_name' ) );
	</#if>	
 
 
  var tableWidget = null;
  $('li','.etv-nav').each(function(index){
   if(CUI(this).attr('class') == 'selected') {
    tableWidget = eval( <#if (Parameters.openType)?default('page') == 'frame'> 'frameWindow.' + </#if>CUI(this).attr('useListObj'));
   }
  });
  var arrObj = new Array();
  var oRows = new Array();

  if(event == undefined && tableWidget != null){
   oRows = tableWidget.getSelectedRow();
  }else{
   oRows.push(oRow);
  }
  if(oRows.length == 0){
   userListFramesetErrorbarWidget.showMessage("${getHtmlText('foundation.user.checkselected')}","f");
   return false;
  }
  for(var i=0; i<oRows.length; i++){
   var oUser = new Object();
   oUser.id = oRows[i].id;
   oUser.userID = oRows[i].id;
   oUser.userId = oRows[i].id;
   oUser.userCode = oRows[i].code;
   oUser.userName = oRows[i].name;
   oUser.name=oRows[i].name;//兼容助记码的displayFieldName
   if( oRows[i].staff ){
	   oUser.staffID = oRows[i].staff.id;
	   oUser.staffCode = oRows[i].staff.code;
	   oUser.staffName = oRows[i].staff.name;
	   oUser.staffname = oRows[i].staff.name;
   }
   arrObj.push(oUser); 
  }
  try{
    <#if (Parameters.openType)?default('page') == 'page'>
    if(CUI("#callBackFuncName").val() != ""){
     if (eval("opener." + CUI("#callBackFuncName").val() + "(arrObj)") == false) {
     	return;
     }
    <#elseif (Parameters.openType)?default('page') == 'frame'>
      var frameWindow =  eval( userListTabWidget.activeContent.getAttribute( 'frame_name' ) );
      if(frameWindow.CUI("#callBackFuncName").val() != ""){
     if (eval("parent." + frameWindow.CUI("#callBackFuncName").val() + "(arrObj)") == false) {
     	return;
     }
    <#else>
    if(CUI("#callBackFuncName").val() != ""){
     if (eval(CUI("#callBackFuncName").val() + "(arrObj)") == false) {
     	return;
     }
    </#if>
   }else{
    getStaffInfo(arrObj);
   }
   <#if (Parameters.openType)?default('page') == 'page'>
   setTimeout(function(){
    try {
     if(CUI("#closePage").val() != "false") {
      top.opener.focus();
      CUI.closeWindow();
     }
    }catch(e){}
   },1000);
   </#if>
   <#if adderror??>
   </#if>
   	<#if (Parameters.openFrom)?default('bap') != 'supplant'>
    userListFramesetErrorbarWidget.show("${getHtmlText('foundation.add.success')}", "s");
    </#if>
  }catch(e){
   userListFramesetErrorbarWidget.show("${getHtmlText('foundation.add.failure')}", "f");
  }
 };
 
  // 供外部调用
 <#assign requestUri = ((request.requestURI)!'')?split('.action')[0]>
 <#assign requestUri = requestUri?replace('/', '_', 'r')>
 // 供外部调用
 foundation.common.${requestUri}__callbackFunction = function(){
 	 <#if (Parameters.openType)?default('page') == 'frame'>
	  var frameWindow =  eval( userListTabWidget.activeContent.getAttribute( 'frame_name' ) );
	 </#if>	
 	
 	 var tableWidget = null;
	  $('li','.etv-nav').each(function(index){
		   if(CUI(this).attr('class') == 'selected') {
		    tableWidget = eval( <#if (Parameters.openType)?default('page') == 'frame'> 'frameWindow.' + </#if>CUI(this).attr('useListObj'));
		   }
     });
     if(tableWidget.getSelectedRow().length == 0){
     	<#if (Parameters.openType)?default('page') == 'frame'>
     	parent.CUI.Dialog.alert("${getHtmlText('foundation.user.checkselected')}");
     	<#else>
     	CUI.Dialog.alert("${getHtmlText('foundation.user.checkselected')}");
     	</#if>
		return false;
     }
  	 if($('#userListTab .selected').html().indexOf("${getText('foundation.customGroup.userDefined')}") > -1){
  	 	<#if (Parameters.openType)?default('page') == 'frame'>frameWindow.</#if>foundation.userDefinedGroupUser.sendBackGroupUserList();
  	 }else{
  	 	foundation.user.sendBackUserInfo();
  	 }
 };
	<#if (Parameters.openType)?default('page') == 'page'>
	$(function(){
		$("#bottom-submit").click( function(){
			if($('#userListTab .selected').html().indexOf("${getText('foundation.customGroup.userDefined')}") > -1){
				 foundation.userDefinedGroupUser.sendBackGroupUserList();
			}else{
				foundation.user.sendBackUserInfo();
			}
		});
	});
	</#if>
}) ();
</script>

<#if (Parameters.openType)?default('page') != 'dialog'>
</body>
</html>
</#if>
