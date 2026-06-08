<#if (Parameters.openType)?default('page') != 'dialog'>
<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>${getText('foundation.staff.selectStaff')}</title>
<@maincss/>
<@mainjs/>
</head>
<body id="dialog_page">
</#if>


<input type="hidden" id="closePage" name="closePage" value="${(closePage)?string('true','false')}" />
<input type="hidden" id="callBackFuncName" name="callBackFuncName" value="${callBackFuncName}" id="callBackFuncName"/>
<input type="hidden" id="rowIndex" name="rowIndex" />
<input type="hidden" id="crossCompanyFlag" name="crossCompanyFlag" value="${(crossCompanyFlag)?string('true','false')}" />
<input type="hidden" id="companyId" name="companyId" value="${getCurrent('companyId')}" />
<input type="hidden" id="companyName" name="companyName" value="${getCurrent('companyName')}" />
<input type="hidden" id="hiddenTab" name="hiddenTab" value="1" />
<@loadpanel></@loadpanel>
<#if unassignStaffSupport??>
	<#assign tabhide = false>
<#else>	
	<#assign tabhide = true>
</#if>
<#if (Parameters.openType)?default('page') != 'dialog'>
<div>
</#if>


<#if (Parameters.openType)?default('page') != 'dialog'>
	<div id="staffListTab" class="etv-navset" style="position:relative;padding-top:3px \9;">
<#else>	
	<div id="staffListTab" class="etv-navset" style="overflow-y:hidden;" >
</#if>

	<ul class="etv-nav">
		<li ajax="true"  surl="/msService/ec/foundation/staff/common/departmentStaffList?openFrom=${(Parameters.openFrom)!'bap'}&closePage=${closePage?string('true','false')}&callBackFuncName=${callBackFuncName!}&companyId=${companyId!}&<#if isSingleMode?string('true','false')=='true' && defaultFilterNoUserStaff?string('true','false')=='true'>crossCompanyFlag=true</#if>&openType=${(Parameters.openType)!'page'}&multiSelect=${(Parameters.multiSelect)?string('false','true')}&enableAddToGroup=${(enableAddToGroup!false)?string}&defaultFilterNoUserStaff=${defaultFilterNoUserStaff?string('true','false')}">${getHtmlText('foundation.department.according')}</li>
		<li ajax="true"  surl="/msService/ec/foundation/staff/common/positionStaffList?openFrom=${(Parameters.openFrom)!'bap'}&closePage=${closePage?string('true','false')}&callBackFuncName=${callBackFuncName!}&companyId=${companyId!}&<#if isSingleMode?string('true','false')=='true' && defaultFilterNoUserStaff?string('true','false')=='true'>crossCompanyFlag=true</#if>&openType=${(Parameters.openType)!'page'}&multiSelect=${(Parameters.multiSelect)?string('false','true')}&enableAddToGroup=${(enableAddToGroup!false)?string}&defaultFilterNoUserStaff=${defaultFilterNoUserStaff?string('true','false')}">${getHtmlText('foundation.position.according')}</li>
		<#if unassignStaffSupport??&&unassignStaffSupport>
		<li ajax="true"  surl="/foundation/staff/common/unassignCompanyStaff?openFrom=${(Parameters.openFrom)!'bap'}&closePage=${closePage?string('true','false')}&callBackFuncName=${callBackFuncName!}&companyId=${companyId!}&crossCompanyFlag=${isSingleMode?string('false','true')}&openType=${(Parameters.openType)!'page'}&multiSelect=${(Parameters.multiSelect)?string('false','true')}&systemAdminFlag=${systemAdminFlag?string('true','false')}&enableAddToGroup=${(enableAddToGroup!false)?string}&defaultFilterNoUserStaff=${defaultFilterNoUserStaff?string('true','false')}">${getHtmlText('foundation.staff.unassignstaffselect')}</li>
		</#if>
		<#if dimissionStaffSupport?? && dimissionStaffSupport>
		<li ajax="true"  surl="/foundation/staff/common/dimissionFrame?openFrom=${(Parameters.openFrom)!'bap'}&closePage=${closePage?string('true','false')}&callBackFuncName=${callBackFuncName!}&openType=${(Parameters.openType)!'page'}&multiSelect=${(Parameters.multiSelect)?string('false','true')}">${getHtmlText('foundation.dimission.dimissionStaff')}</li>
		</#if>
		<!--<#if unUserDefinedGroupStaff?? && unUserDefinedGroupStaff>
		<li ajax="true" surl="/foundation/staff/common/userDefinedGroupStaffList?&closePage=${closePage?string('true','false')}&callBackFuncName=${callBackFuncName!}&companyId=${companyId!}&crossCompanyFlag=${crossCompanyFlag?string('true','false')}&openType=${(Parameters.openType)!'page'}&multiSelect=${(Parameters.multiSelect)?string('false','true')}&systemAdminFlag=${systemAdminFlag?string('true','false')}&defaultFilterNoUserStaff=${defaultFilterNoUserStaff?string('true','false')}">${getHtmlText('foundation.customGroup.userDefined')}</li>
		</#if>-->
		
	</ul>
	<div class="etv-content">
		<div frame_name="etv_frame_department"></div>
		<div frame_name="etv_frame_position"></div>
		<#if unassignStaffSupport?? && unassignStaffSupport><div frame_name="etv_frame_unassign"></div></#if>
		<#if dimissionStaffSupport?? && dimissionStaffSupport>
		<div frame_name="etv_frame_dimission"></div>
		</#if>
		<#if unUserDefinedGroupStaff?? && unUserDefinedGroupStaff><div frame_name="etv_frame_user"></div></#if>
	</div>
	<#if (Parameters.openType)?default('page') == 'page'>

	<@frame id="department_Button" region="south" height=28>
	 <div  align="right" style="margin-right:20px;position:absolute;bottom:0px;right:0;z-index:100;">
	 	<a id="bottom-submit" class="cui-btn-blue" style="margin-right:10px;"><span class="btn_r">${getHtmlText('common.button.choose')}</span></a>
	 	<a id="bottom-reset" class="cui-btn-blue" style="margin-right:10px;"><span class="btn_r">${getHtmlText('common.button.close')}</span></a>
	 </div>
	 </@frame>
	 </#if>
</div>

<#if (Parameters.openType)?default('page') != 'dialog'>
</div>
</#if>

<script type="text/javascript" charset="UTF-8" language="javascript">
changeBtnStyle();//按钮交互效果
(function($){
	var staffListTabWidget;
	staffListTabWidget = new CUI.TabView("staffListTab",{
		isscroll : true,
		
		<#if (Parameters.openType)?default('page') == 'frame'>
		iframe: true,
		</#if>
		//height : 483,
		removeable : false
		
	});
	// 供外部调用
	<#assign requestUri = ((request.requestURI)!'')?split('.action')[0]>
	<#assign requestUri = requestUri?replace('/', '_', 'r')>
	// 供外部调用
	foundation.common.${requestUri}__callbackFunction = function(){
		var retVal = true;
		var currentTab = staffListTabWidget.activeContent;
		
		<#if (Parameters.openType)?default('page') == 'frame'>
			var frameWindow =  eval( currentTab.getAttribute( 'frame_name' ) );
			eval("retVal = frameWindow." + frameWindow.CUI('input[name="tabCallBackFuncName"]').val() + "();");
		<#else>	
			eval("retVal = " + CUI('input[name="tabCallBackFuncName"]', currentTab).val() + "();");
			
		</#if>
		return retVal;
			
	};
	<#if (Parameters.openType)?default('page') == 'page'>
	$(function(){ //调整样式，以显示“选择”，“取消”按钮
		$("#bottom-submit").click( function(){
			if( $("#staffListTab .selected").html().indexOf("${getText('foundation.department.according')}") != -1) {
				foundation.staff.sendBackDepartmentStaffInfo();
			} else if($("#staffListTab .selected").html().indexOf("${getText('foundation.position.according')}") != -1) {
				foundation.staff.sendBackStaffInfo();
			} else if ( $("#staffListTab .selected").html().indexOf("${getText('foundation.staff.unassignmanage')}") != -1) {
				foundation.unassignStaff.sendBackStaffInfo();
			} else if ($('#staffListTab .selected').html().indexOf("${getText('foundation.customGroup.userDefined')}") > -1){
				foundation.userDefinedGroupStaff.sendBackGroupStaffList();
			}
		});
		$("#bottom-reset").click( function(){
			window.close();
		});
	});
	</#if>
})(jQuery);
</script>

<#if (Parameters.openType)?default('page') != 'dialog'>
</body>
</html>
</#if>


