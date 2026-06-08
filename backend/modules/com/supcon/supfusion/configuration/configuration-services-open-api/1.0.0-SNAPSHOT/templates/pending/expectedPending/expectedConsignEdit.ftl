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
<@errorbar id="expectedConsignEditErrorbar" />
<@errorbar id="expecteSubmitFormDialogErrorBar" />
<@s.form id="expecteSubmitForm" action="expectedConsignResult" namespace="/msService/ec/pending"  callback="parent.expectedConsign_expConcallBackInfo">
	<@s.hidden name="id" id="expectConsignId" />
	<@s.hidden name="actionMode" id="actionMode" />
	<@s.hidden name="flowInfos" id="flowInfos" />
	<table class="cui-fd-infotable" id="editInfo" cellpadding="0" cellspacing="0">
		<@s.if test="actionMode=='manage'">
			<tr>
				<td style="width:15%;" class="lab cui-lmust">${getHtmlText('ec.expectedConsign.consingorStaff')}</td>
				<td style="width:85%" colspan="3">
					<@s.hidden name="expectedConsign.userId" id="expectedUserId" />
					 <@mneclient iframe=true formId="expecteSubmitForm" displayFieldName="staffname"  name="expectedConsign.staffName" id="exUserStaffName" url="/msService/ec/foundation/user/common/userListFrameset" onkeyupfuncname="foundation.expectedConsign.keyupcallback(obj);" classStyle="cui-noborder-input" cssStyle="width:97%;" value="${(expectedConsign.staffName)!}" type="User"  multiple=false clicked=true isEdit=true />
					<#--
				 		<#if expectedConsign??>
							<@selector pageType="selectStaff" cssClass="cui-noborder-input" name="expectedConsign_staffName" value="${expectedConsign.staffName}" id="exUserStaffName" readOnly=true onclick="foundation.expectedConsign.selectStaff" />
						<#else>
							<@selector pageType="selectStaff" cssClass="cui-noborder-input" name="expectedConsign_staffName" value="" id="exUserStaffName" readOnly=true onclick="foundation.expectedConsign.selectStaff" />
						</#if>
					-->
				</td>			
			</tr>
		</@s.if>
		<tr>
			<td style="width:15%;" class="lab cui-lmust">${getHtmlText('ec.expectedConsign.consinger')}</td>
			<td style="width: 85%" colspan="3">
				<@mneclient iframe=true name="consigners" formId="expecteSubmitForm" id="consigners_staffName" displayFieldName="staffname"  url="/msService/ec/foundation/user/common/userListFrameset?forCompanyAdmin=true&multiSelect=true" classStyle="cui-noborder-input" type="User" cssStyle="width:97%;" onkeyupfuncname="getconsigners_staffNameMultiInfo()" multiple=true clicked=true isEdit=true/>
				<#--
				 <@multiselect id="consigners" name="consigners" displayFieldName="staffName" url="/msService/ec/foundation/user/common/userListFrameset?forCompanyAdmin=true" type="other"  />
				-->
			</td>			
		</tr>
		<tr>
				<td  style="width: 15%;" class="lab cui-lmust">${getHtmlText('ec.expectedConsign.startDate')}</td>
				<td  style="width: 35%">
					<@datepicker name="expectedConsign.startDate" cssStyle="margin-left:0px;width:97%;" cssClass="cui-noborder-input" id="startTime" />				
				</td>
				<td  style="width: 15%;" class="lab cui-lmust">${getHtmlText('ec.expectedConsign.endDate')}</td>
				<td  style="width: 35%">				
				  	<@datepicker name="expectedConsign.endDate" cssStyle="width:97%;" cssClass="cui-noborder-input"   id="endTime" />		
				</td>
		</tr>
		<tr>
			<td class="lab v-align">${getHtmlText('ec.expectedConsign.memo')}</td>
			<td colspan="3" class="cui-vte">	
				<div class="fix-input">			
				  	<textarea name="expectedConsign.memo" class="cui-noborder-textarea"></textarea>			
				</div>
			</td>
		</tr>
		<tr>
			<td align="left" colspan="2">${getHtmlText('foundation.pending.wtsyhd')}
				<input type="checkbox" id="selectAll" name="selectAll" value="true"/>
			</td>
		</tr>
		<tr>
			<td align="left" colspan="2">${getHtmlText('ec.expectedConsign.includePending')}
				<input type="radio" id="includeBefore" name="includeBefore" value="true" checked="true" />${getHtmlText("common.radio.true")}&#160;&#160;&#160;
				<input type="radio" name="includeBefore" id="includeBefore" value="false" />${getHtmlText("common.radio.false")}
			</td>
		</tr>
	</table>
	<@datatable hidekey="['ID','flowKey','activeCode']" id="proxyPendingList" paginator=false transMethod="post" editable=false dataUrl="" firstLoad=false style="margin-right:2px;margin-left:8px;">
		<@operatebar operates="code:base_proxyPending_add||iconcls:add||name:${getHtmlText('ec.flowActive.select')}||onclick:foundation.expectedConsign.openFlowActive();code:base_proxyPending_delete||iconcls:del||name:${getHtmlText('ec.expectedConsign.del')}||onclick:foundation.expectedConsign.deleteRow()" operateType="noPower"  resultType="json">
		</@operatebar>
		<@datacolumn key="flowName" label="${getHtmlText('ec.flowActive.flowName')}" width=100 />
		<@datacolumn key="activeName" label="${getHtmlText('ec.flowActive.activeName')}" width=100 />
	</@datatable>
</@s.form>
<script type="text/javascript" charset="utf-8" language="javascript">
(function() {
	CUI.ns("foundation.expectedConsign");
	CUI("#startDate").datepicker({id:"startDate", picker: "<input type='button' value='&nbsp;' class='cui-calpick'></input>"}); 
    CUI("#endDate").datepicker({id:"endDate", picker: "<input type='button' value='&nbsp;' class='cui-calpick'></input>"}); 
	foundation.expectedConsign.keyupcallback=function(obj){
		if(obj!=null&&obj!=undefined&&obj[0].id!=undefined&&obj[0].id!=null){
				var userId=obj[0].id;
				CUI("#expectedUserId").val(userId);
		}
		if(obj!=null&&obj!=undefined&&obj[0].staffname!=undefined&&obj[0].staffname!=null){
			var staffName=obj[0].staffname;
			CUI("#exUserStaffName").val(staffName);
		}
	}
	//多选被委托人
	foundation.expectedConsign.callSelectConsign=function(){
		var windowStyle = "width=800,height=600,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
		foundation.common.callOpenWeb("user",windowStyle,null,null,false,"expectedConsign_getConsigner","forCompanyAdmin=true&unassignUserSupport=true");
	
	}//删除被委托人
	foundation.expectedConsign.expectedDeleteUser=function(imgObj){
		var oldAdminIDArrays = CUI("#consigners").val().split(";");
		var newAdminIDs = "";
		var userId=CUI(imgObj).attr('userid');
		for(var i=0; i<oldAdminIDArrays.length; i++){
			var arr=oldAdminIDArrays[i].split(",");
			if(arr[0]== userId) continue;
			newAdminIDs = (newAdminIDs == "") ? oldAdminIDArrays[i] : newAdminIDs + ";" + oldAdminIDArrays[i];
		}
		CUI("#consigners").val(newAdminIDs);
		CUI(imgObj).parent().remove();
	}
	
	foundation.expectedConsign.saveExpCon=function(){
	  var error=false;
	  var form=CUI("#expecteSubmitForm");
	  var errorMessages=new Array();
	  var errorFields=new Array();
	  var flag=false;
	  clearErrorMessages(form);
	  clearErrorLabels(form);
		if(CUI('#expectedUserId')&&CUI('#expectedUserId').val()==""){
			errorMessages.push("${getHtmlText('ec.expectedConsign.consingorStaffNull')}");
			errorFields.push(CUI("#expectedConsign_staffName",CUI("#expecteSubmitForm")));
			error=true;
		}
		if(CUI('#consigners_staffNameAddIds').val()==""){
			errorMessages.push("${getHtmlText('ec.exceptedConsign.consignersnull')}");
			errorFields.push(CUI("#expecteSubmitForm #consigners_staffNameMultiIDsContainerDiv"));
			errorFields.push(CUI("#expecteSubmitForm #consigners_staffNameMneInput"));
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
		if(CUI('#flowInfos').val()=="" && $('#selectAll').prop("checked")==false){
			errorMessages.push("${getHtmlText('ec.exceptedConsign.flowInfosnull')}");
			error=true;
		}
	//	CUI('#consignersAddIds').val(CUI('#consigners_staff_nameAddIds').val());
		if(error){
			addError(form,expectedConsignEditErrorbarWidget,errorFields,errorMessages);
			CUI.each(errorFields,function(){
				showErrorField(CUI(this));
			});
			return ;
		}
		else{
			CUI('#expecteSubmitForm').submit();
		}
	}
	
	//选人的回调
	expectedConsign_getConsigner=function(oStaffs){
		var ostaffstr="";
		var html="";
		for(var j=0;j<oStaffs.length;j++){
			var userId=oStaffs[j].userID;
			var staffId=oStaffs[j].staffID;
			var userName=oStaffs[j].userName;
			var flag = 0;
			//先判断该人员是否已经被选择，若已经选择，则过滤
			if(CUI("#consigners").val() != ""){
				var alreadyIDs = CUI("#consigners").val().split(";");
				for(var i=0; i<alreadyIDs.length; i++){
					var arr=alreadyIDs[i].split(",");
					if(arr[0] == userId){
						flag = 1;
						break;
					}
				}
				
			}
			if(flag == 0){
				ostaffstr+=";"+userId+","+userName+","+staffId;
				html+="<span>"+oStaffs[j].staffName+"<img src='/bap/static/foundation/images/icon-del.gif' style='cursor:hand;vertical-align:bottom;' onclick='foundation.expectedConsign.expectedDeleteUser(this);' userid='"+userId+"'/>&#160;&#160;&#160;&#160;</span>";
			}
		}
		if(ostaffstr!=""){
			ostaffstr=ostaffstr.substr(1);
		}
		var IDs = (CUI("#consigners").val() == "") ?ostaffstr : CUI("#consigners").val() + ";" + ostaffstr;
		CUI("#consigners").val(IDs);
	
		var str = CUI("#consignersContainer").html() + html;
		CUI("#consignersContainer").html(str);
	}
	
	foundation.expectedConsign.openFlowActive=function(){
		var paramCon="forCompanyAdmin=true&unassignUserSupport=true";
		/*
		if(CUI("#expectedUserId").val()!=""&&CUI("#expectedUserId").val()!=undefined){
			paramCon+="&userId="+CUI("#expectedUserId").val();
		}
		*/
		var windowStyle = "width=800,height=600,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
		flow_callOpenWeb("flowActiveList",windowStyle,null,'true','false',"expectedConsign_getFlowActive",paramCon);
	}
	expectedConsign_getFlowActive=function(arr){
		
		var flowInfo=CUI("#flowInfos").val();
		var str='';
		var flowArr=flowInfo.split(";");
		var html=CUI("#flowActiveTable").html();
		for(var i=0;i<arr.length;i++){
			var tempObj = new Object();
			var obj=arr[i];
			tempObj.activeCode = obj.active_code;
			tempObj.activeName = obj.active_name;
			tempObj.flowName = obj.flowName;
			tempObj.flowKey = obj.flowKey;
			var flag=0;
			for(var j=0;j<flowArr.length;j++){
				var flows=flowArr[j];
				var actArr=flows.split(",");
				if(actArr[0]==obj.flowKey&&actArr[1]==obj.active_code){
					flag=1;
				}
			}
			if(flag==0){
				str+=";"+tempObj.flowKey+","+tempObj.activeCode;
				var elTr = datatable_proxyPendingList.addNewRowWithValue(null, tempObj);
			}
		}
		if(str!=""){
			if(flowInfo!=''){
				str=flowInfo+''+str;
			}else{
				str=str.substr(1);
			}
			
			CUI("#flowInfos").val(str)
		}
		//datatable_proxyPendingList.setAllRowEdited();
		//var rows = datatable_proxyPendingList.getEditData();
		//console.log(rows);
	}
	foundation.expectedConsign.deleteRow=function(){
		if(datatable_proxyPendingList.selectedRows.length == 0){
			expectedConsignEditErrorbarWidget.show("${getHtmlText('ec.expectedConsign.noSelected')}","f");
			return ;
		}
		if(confirm("${getText('ec.common.checkDelete')}")){
			var deleteRows = datatable_proxyPendingList.delRow();
			var flowInfos=CUI("#flowInfos").val();
			var flowKey=deleteRows[0].flowKey;
			var code =deleteRows[0].activeCode;
			var flowArr=flowInfos.split(";");
			var newFlowInfos="";
			for(var i=0;i<flowArr.length;i++){
				var arr=flowArr[i].split(',');
				if(flowKey==arr[0]&&code==arr[1]){
					continue;
				}
				newFlowInfos = (newFlowInfos == "") ? flowArr[i] : newFlowInfos + ";" + flowArr[i];
	
			}
			CUI("#flowInfos").val(newFlowInfos);
		}
	}
	foundation.expectedConsign.selectStaff=function(){
		
		var windowStyle = "width=800,height=600,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
		foundation.common.callOpenWeb("user",windowStyle,null,false,false,"expectedConsign_getStaffUser","forCompanyAdmin=true");
	
	}
	expectedConsign_getStaffUser=function(arr){
		
	}

}) ();


ec_myPending_saveExpCon=function(){
	if(CUI('#expectedUserId')&&CUI('#expectedUserId').val()==""){
		expectedConsignEditErrorbarWidget.show("${getHtmlText('ec.expectedConsign.consingorStaffNull')}","f");
		return ;
	}
	if(CUI('#consigners_staffNameAddIds').val()==""){
		expectedConsignEditErrorbarWidget.show("${getHtmlText('ec.exceptedConsign.consignersnull')}","f");
		return ;
	}
	if(CUI('#flowInfos').val()=="" && $('#selectAll').prop("checked")==false){
		expectedConsignEditErrorbarWidget.show("${getHtmlText('ec.exceptedConsign.flowInfosnull')}","f");
		return ;
	}
	if(CUI('#consigners').val()==""){
		expectedConsignEditErrorbarWidget.show("${getHtmlText('ec.exceptedConsign.consignersnull')}","f");
		return ;
	}
	if(CUI('#startTime').val()==""){
		expectedConsignEditErrorbarWidget.show("${getHtmlText('ec.exceptedConsign.startTimenull')}","f");
		return ;
	}
	if(CUI('#endTime').val()==""){
		expectedConsignEditErrorbarWidget.show("${getHtmlText('ec.exceptedConsign.endTimenull')}","f");
		return ;
	}
	if(CUI('#startTime').val()>CUI('#endTime').val()){
		expectedConsignEditErrorbarWidget.show("${getHtmlText('ec.exceptedConsign.startTimegtendTimenull')}","f");
		return ;
	}
	CUI('#expecteSubmitForm').submit();
}


</script>


<#if (Parameters.openType)?default('page') != 'dialog'>
</body>
</html>
</#if>