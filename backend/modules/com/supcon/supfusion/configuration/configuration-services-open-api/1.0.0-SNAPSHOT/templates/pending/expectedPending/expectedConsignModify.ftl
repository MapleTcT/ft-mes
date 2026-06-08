<#if (Parameters.openType)?default('page') == 'frame'>
<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>${getText('ec.myExConsign.manage')}</title>
<@maincss/>
<@mainjs/>
</head>
<body id="dialog_page">
<@loadpanel></@loadpanel>
<@errorbar id="workbenchErrorBar" offsetY=0 />
</#if>

<script type="text/javascript" charset="utf-8" src="/bap/static/ec/js/common.js"></script>
<@errorbar id="expectedConsignModifyErrorbar"/>
<@s.form id="expecteSubmitForm" action="expectedConsignResult" namespace="/msService/ec/pending"  callback="parent.expectedConsign_expConcallBackInfo1">
	<@s.hidden name="expectedConsign.id" />
	<@s.hidden name="flowInfos" id="flowInfos" />
	<@s.hidden name="actionMode" id="actionMode" />
	<@s.hidden name="consignersAddIds" id="consigners" />
	<@s.hidden name="expectedConsign.createDate" />
	<table class="infoTable" id="editInfo" cellpadding="0" cellspacing="0" border="0" align="center" width="90%" style="margin-top: 5px">
		<@s.if test="actionMode=='manage'">
			<tr>
				<td style="width:20%;" class="lab cui-lmust">${getHtmlText('ec.expectedConsign.consingorStaff')}</td>
				<td style="width:80%">
					<@s.hidden name="expectedConsign.userId" id="expectedUserId" />
					<@selector pageType="selectStaff" cssClass="cui-noborder-input" name="expectedConsign_staffName" value="${expectedConsign.staffName}" id="exUserStaffName" readOnly=true onclick="foundation.expectedConsign.selectStaff" clearFunc="foundation.expectedConsign.clearSelectStaff()"/>
				</td>
			</tr>
		</@s.if>
		<tr>
			<td class="lab cui-lmust">${getHtmlText("ec.expectedConsign.consinger")}</td>
			<td>
				<@selector pageType="user" cssClass="cui-noborder-input" name="consignorStaffName" value="${expectedConsign.consignorStaffName}" id="userStaffName" readOnly=true onclick="foundation.expectedConsign.callSelectSingleConsign" clearFunc="foundation.expectedConsign.clearSelectSingleConsign()"/>
			</td>			
		</tr>
		<tr>
			<td class="lab cui-lmust">${getHtmlText('ec.expectedConsign.startTime')}</td>
			<td>	
			  <@datepicker name="expectedConsign.startDate" value="${expectedConsign.startDate}" id="startTime"  cssClass="cui-noborder-input" cssStyle="margin:0px"/>
			</td>
		</tr>
		<tr>
			<td class="lab cui-lmust">${getHtmlText('ec.expectedConsign.endTime')}</td>
			<td>				
			  <@datepicker name="expectedConsign.endDate" value="${expectedConsign.endDate}" id="endTime" cssClass="cui-noborder-input" cssStyle="margin:0px"/>		
			</td>
		</tr>
		<tr>
			<td class="lab">${getHtmlText('ec.flowActive.flowName')}</td>
			<td>
				<div class="fix-input-readonly"><input class="cui-noborder-input" style="margin-left:0px;" type="text" value="${expectedConsign.flowName}" readonly="" /></div>
			</td>			
		</tr>
		<tr>
			<td class="lab cui-lmust"> ${getHtmlText('ec.flowActive.activeName')}</td>
			<td>
				<#-- 
					<@s.hidden name="expectedConsign.activeCode" id="m_FlowActiveCode" />
					<@selector pageType="flowActiveList" cssClass="cui-noborder-input" name="flowActiveList" value="${expectedConsign.activeName}" id="m_FlowActiveName" readOnly=true onclick="foundation.expectedConsign.openSignleFlowActive" clearFunc="foundation.expectedConsign.clearSignleFlowActive()"/>
				 -->
				 <div class="fix-input-readonly"><input class="cui-noborder-input" style="margin-left:0px;" type="text" value="${expectedConsign.activeName}" readonly="" /></div>
			</td>			
		</tr>
		<tr>
			<td class="lab v-align">${getHtmlText('ec.expectedConsign.memo')}</td>
			<td class="cui-vte">				
			  <div class="fix-input-readonly"><@s.textarea name="expectedConsign.memo" cssClass="cui-noborder-textarea"  cssStyle="width:99%;" /></div>	
			</td>
		</tr>
	</table>
</@s.form>
<script type="text/javascript" charset="utf-8" language="javascript">
(function(){

	CUI.ns("foundation.expectedConsign");
	foundation.expectedConsign.deleteCallBack=function(res){
		if(res.dealSuccessFlag == true){
			expectedConsignModifyErrorbarWidget.show("${getHtmlText('ec.common.saveandclosesuccessful')}","f");
			foundation.expectedConsign.refleshDataTable();
		}else{
			expectedConsignModifyErrorbarWidget.show("${getHtmlText('ec.common.unsuccessfully')}","f");
		}
	}
	
	foundation.expectedConsign.saveSignleExpCon=function(){
	     var error=false;
	     var flag=false;
	  var form=CUI("#expecteSubmitForm");
	  var errorMessages=new Array();
	  var errorFields=new Array();
	  clearErrorMessages(form);
	  clearErrorLabels(form);
	  	if(CUI('#exUserStaffName',form).val()==""){
			errorMessages.push("${getHtmlText('ec.exceptedConsign.userStaffsnull')}");
			errorFields.push(CUI('#exUserStaffName',form));
			error=true;
		}
		if(CUI('#userStaffName',form).val()==""){
			errorMessages.push("${getHtmlText('ec.exceptedConsign.consignersnull')}");
			errorFields.push(CUI('#userStaffName',form));
			error=true;
		}
		if(CUI('#startTime').val()==""){
			errorMessages.push("${getHtmlText('ec.exceptedConsign.startTimenull')}");
			errorFields.push(CUI("#expecteSubmitForm #startTime"));
			error=true;
		}
		else if(!CUI('#startTime').validate()){
			if(!flag){
		    	errorMessages.push("${getHtmlText('ec.expectedConsign.errorDateFormat')}");
		    	flag=true;
		    }
			errorFields.push(CUI("#expecteSubmitForm #startTime"));
			error=true;
			
		}
		if(CUI('#endTime').val()==""){
			errorMessages.push("${getHtmlText('ec.exceptedConsign.endTimenull')}");
			errorFields.push(CUI("#expecteSubmitForm #endTime"));
			error=true;
		}
		else if(!CUI('#endTime').validate()){
		    if(!flag){
		    	errorMessages.push("${getHtmlText('ec.expectedConsign.errorDateFormat')}");
		    }
			errorFields.push(CUI("#expecteSubmitForm #endTime"));
			error=true;
		}
		if(CUI('#startTime').val()!=""&&CUI('#startTime').validate()&&CUI('#endTime').val()!=""&&CUI('#endTime').validate()){
			if(CUI('#startTime').val()>CUI('#endTime').val()){
				errorMessages.push("${getHtmlText('ec.exceptedConsign.startTimegtendTimenull')}");
				errorFields.push(CUI("#expecteSubmitForm #startTime"));
				error=true;
			}
		}
		if(CUI('#m_FlowActiveCode',form).val()==""){
			errorMessages.push("${getHtmlText('ec.exceptedConsign.flowInfosnull')}");
			errorFields.push(CUI('#m_FlowActiveName',form));
			error=true;
			/*expectedConsignModifyErrorbarWidget.show("${getHtmlText('ec.exceptedConsign.flowInfosnull')}","f");
			return ;*/
		}
		if(error){
			addError(form,expectedConsignModifyErrorbarWidget,errorFields,errorMessages);
			CUI.each(errorFields,function(){
				showErrorField(CUI(this));
			});
			return ;
		}
		else{
			CUI('#expecteSubmitForm').submit();
		}
	}
	
	
	//单选被委托人
	foundation.expectedConsign.callSelectSingleConsign=function(){
		var windowStyle = "width=800,height=600,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
		foundation.common.callOpenWeb("user",windowStyle,null,null,'false',"expectedConsign_getSignleConsigner","forCompanyAdmin=true&unassignUserSupport=true");
	
		
	}
	expectedConsign_getSignleConsigner=function(oStaffs){
		var userId=oStaffs[0].userID;
		var staffId=oStaffs[0].staffID;
		var staffName=oStaffs[0].staffName;
		var userName=oStaffs[0].userName;
		CUI("#consigners").val(userId);
		CUI("#userStaffName").val(staffName);
		
	}
	
	/*
	foundation.expectedConsign.openSignleFlowActive=function(){
		var paramCon="forCompanyAdmin=true&unassignUserSupport=true";
		if(CUI("#expectedUserId").length>0&&CUI("#expectedUserId").val()!=""){
			paramCon+="&userId="+CUI("#expectedUserId").val();
		}
		var windowStyle = "width=800,height=600,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
		flow_callOpenWeb("flowActiveList",windowStyle,null,false,false,"expectedConsign_getSignleFlowActive",paramCon);
	}
	expectedConsign_getSignleFlowActive=function(arr){
			var code = arr[0].active_code;
			var name = arr[0].active_name;
			var flowName=arr[0].flowName;
			var flowKey=arr[0].flowKey;
			CUI("#m_FlowActiveCode").val(code);
			CUI("#m_FlowActiveName").val(name);
			CUI("#m_flowName").val(flowName);
			CUI("#flowInfos").val(flowKey+","+code);
	}
	*/
	
	foundation.expectedConsign.selectStaff=function(){
		
		var windowStyle = "width=800,height=600,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
		foundation.common.callOpenWeb("user",windowStyle,null,false,false,"expectedConsign_getStaffUser","forCompanyAdmin=true&unassignUserSupport=true");
	
	}
	expectedConsign_getStaffUser=function(arr){
		var userId=arr[0].userID;
		var staffName=arr[0].staffName;
		CUI("#expectedUserId").val(userId);
		CUI("#exUserStaffName").val(staffName);
	}
	foundation.expectedConsign.clearSelectStaff=function(){
		CUI("#expectedUserId").val('');
		CUI("#exUserStaffName").val('');
	}
	foundation.expectedConsign.clearSelectSingleConsign=function(){
		CUI("#consigners").val('');
		CUI("#userStaffName").val('');
	}
	/*
	foundation.expectedConsign.clearSignleFlowActive=function(){
		CUI("#m_FlowActiveCode").val('');
		CUI("#m_FlowActiveName").val('');
		CUI("#m_flowName").val('');
		CUI("#flowInfos").val('');
	}
	*/
}) ();


ec_myPending_saveSignleExpCon=function(){
	if(CUI('#userStaffName').val()==""){
		expectedConsignModifyErrorbarWidget.show("${getHtmlText('ec.exceptedConsign.consignersnull')}","f");
		return ;
	}
	if(CUI('#startTime').val()==""){
		expectedConsignModifyErrorbarWidget.show("${getHtmlText('ec.exceptedConsign.startTimenull')}","f");
		return ;
	}
	if(CUI('#endTime').val()==""){
		expectedConsignModifyErrorbarWidget.show("${getHtmlText('ec.exceptedConsign.endTimenull')}","f");
		return ;
	}
	if(CUI('#m_FlowActiveCode').val()==""){
		expectedConsignModifyErrorbarWidget.show("${getHtmlText('ec.exceptedConsign.flowInfosnull')}","f");
		return ;
	}
	CUI('#expecteSubmitForm').submit();
}
</script>

<#if (Parameters.openType)?default('page') != 'dialog'>
</body>
</html>
</#if>