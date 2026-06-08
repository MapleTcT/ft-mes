<script type="text/javascript" charset="utf-8" src="/bap/static/ec/js/common.js" />	
<@errorbar id="myExpectedPendingEditDialogErrorBar" />
<@s.form id="expecteSubmitForm" action="expectedConsignResult" namespace="/msService/ec/pending"  callback="expectedConsign_expConcallBackInfo">
	<@s.hidden name="id" id="expectConsignId" />
	<@s.hidden name="actionMode" id="actionMode" />
	<@s.hidden name="flowInfos" id="flowInfos" />
	<table class="infoTable" id="editInfo" cellpadding="0" cellspacing="0" border="0" align="center" width="90%" style="margin-top: 5px">
		<@s.if test="actionMode=='manage'">
			<tr>
				<td style="width: 20% ;color:#B30303;" align="right">${getHtmlText('ec.expectedConsign.consingorStaff')}</td>
				<td style="width: 80%;">
					<@s.hidden name="expectedConsign.userId" id="expectedUserId" />
					<#if expectedConsign??>
						<@selector pageType="selectStaff" name="expectedConsign_staffName" value="${expectedConsign.staffName}" id="exUserStaffName" readOnly=true onclick="foundation.expectedConsign.selectStaff" />
					<#else>
						<@selector pageType="selectStaff" name="expectedConsign_staffName" value="" id="exUserStaffName" readOnly=true onclick="foundation.expectedConsign.selectStaff" />
					</#if>
				</td>			
			</tr>
		</@s.if>
		<tr>
			<td style="width: 20%; color:#B30303" align="right">${getHtmlText('ec.expectedConsign.consinger')}</td>
			<td style="width: 80%">
				<div style="width: 97%;border: 1px solid #A5C7EF;">
					<@s.hidden name="consigners" id="consigners" />
					<span id="consignersContainer" />
					<input type="button" style="vertical-align: bottom;" class="selectbtnB" onclick="foundation.expectedConsign.callSelectConsign()"/>
				</div>
			</td>			
		</tr>
		<tr>
			<td style="width: 20%;color:#B30303;" align="right">${getHtmlText('ec.expectedConsign.startTime')}</td>
			<td style="width: 80%">
				<@datepicker name="expectedConsign.startDate" cssClass="cui-edit-field" cssStyle="margin-left:0px" readonly=true id="startTime" />				
			</td>
		</tr>
		<tr>
			<td align="right" style="color:#B30303;">${getHtmlText('ec.expectedConsign.endTime')}</td>
			<td>				
			  <@datepicker name="expectedConsign.endDate"  readonly=true id="endTime" cssStyle="margin-left:0px" cssClass="cui-edit-field" />		
			</td>
		</tr>
		<tr>
			<td align="right">${getHtmlText('ec.expectedConsign.memo')}</td>
			<td>				
			  <@s.textarea name="expectedConsign.memo" cssClass="cui-edit-textarea"  cssStyle="width:99%;height:80px" />		
			</td>
		</tr>
		<tr>
			<td align="left" colspan="2">${getHtmlText('ec.expectedConsign.includePending')}&#160;&#160;&#160;&#160;&#160;&#160;
				<input type="radio" id="includeBefore" name="includeBefore" value="true" checked="true" />${getHtmlText("common.radio.true")}&#160;&#160;&#160;
				<input type="radio" name="includeBefore" id="includeBefore" value="false" />${getHtmlText("common.radio.false")}
			</td>
		</tr>
	</table>
	<@datatable hidekey="['ID','TABLEID']" id="proxyPendingList"  transMethod="post" editable=false dataUrl="" firstLoad=false style="margin-left:4px;margin-right:2px;">
			<@operatebar operates="code:base_proxyPending_add||iconcls:add||name:${getHtmlText('ec.flowActive.select')}||onclick:foundation.expectedConsign.openFlowActive();code:base_proxyPending_delete||iconcls:delete||name:${getHtmlText('ec.expectedConsign.del')}||onclick:foundation.expectedConsign.deleteRow()" operateType="noPower"  resultType="json">
			</@operatebar>
			<@datacolumn key="flowName" label="${getHtmlText('ec.pending.tableNo')}" width=100 />
			<@datacolumn key="activeName" label="${getHtmlText('ec.pending.pendingType')}" type="select" options="{'0':'${getText('ec.pending.normalPending')}','1':'${getText('ec.pending.normalPending')}','2':'${getText('ec.pending.proxyPending')}'}"  width=100 />
			</@datatable>
</@s.form>

<script type="text/javascript" charset="utf-8" language="javascript">

CUI.ns("foundation.expectedConsign");

//多选被委托人
foundation.expectedConsign.callSelectConsign=function(){
	var windowStyle = "width=800,height=600,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
	foundation.common.callOpenWeb("user",windowStyle,null,null,false,"expectedConsign_getConsigner","forCompanyAdmin=true");

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
	if(CUI('#expectedUserId')&&CUI('#expectedUserId').val()==""){
		myExpectedPendingEditDialogErrorBarWidget.showMessage("${getHtmlText('ec.expectedConsign.consingorStaffNull')}","f");
		return ;
	}
	if(CUI('#flowInfos').val()==""){
		myExpectedPendingEditDialogErrorBarWidget.show("${getHtmlText('ec.exceptedConsign.flowInfosnull')}","f");
		return ;
	}
	if(CUI('#consigners').val()==""){
		myExpectedPendingEditDialogErrorBarWidget.show("${getHtmlText('ec.exceptedConsign.consignersnull')}","f");
		return ;
	}
	if(CUI('#startTime').val()==""){
		myExpectedPendingEditDialogErrorBarWidget.show("${getHtmlText('ec.exceptedConsign.startTimenull')}","f");
		return ;
	}
	if(CUI('#endTime').val()==""){
		myExpectedPendingEditDialogErrorBarWidget.show("${getHtmlText('ec.exceptedConsign.endTimenull')}","f");
		return ;
	}
	if(CUI('#startTime').val()>CUI('#endTime').val()){
		myExpectedPendingEditDialogErrorBarWidget.show("${getHtmlText('ec.exceptedConsign.startTimegtendTimenull')}","f");
		return ;
	}
	CUI('#expecteSubmitForm').submit();
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
	var windowStyle = "width=800,height=600,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
	flow_callOpenWeb("flowActiveList",windowStyle,null,'true','false',"expectedConsign_getFlowActive","forCompanyAdmin=true&unassignUserSupport=true");
}
expectedConsign_getFlowActive=function(arr){
	
	var flowInfo=CUI("#flowInfos").val();
	var str="";
	var flowArr=flowInfo.split(";");
	var html=CUI("#flowActiveTable").html();
	for(var i=0;i<arr.length;i++){
		var obj=arr[i];
		var code = obj.active_code;
		var name = obj.active_name;
		var flowName = obj.flowName;
		var flowKey=obj.flowKey;
		var flag=0;
		for(var j=0;j<flowArr.length;j++){
			var flows=flowArr[j];
			var actArr=flows.split(",");
			if(actArr[0]==flowKey&&actArr[1]==code){
				flag=1;
			}
		}
		if(flag==0){
			str+=";"+flowKey+","+code;
			html+='<tr><td><input type="hidden" name="expected_flowKey" value="'+flowKey+'"/><input type="hidden" name="flowActiveCode" value="'+code+'"/><div style="width:200px">'+flowName+'</div></td><td><input type="hidden" name="flowActiveName" value="'+name+'"/><div style="width:200px">'+name+'</div></td><td><div style="width:100px" onclick="foundation.expectedConsign.deleteRow(this)" flowKey="'+flowKey+'" code="'+code+'">${getHtmlText("ec.expectedConsign.del")}</div></td></tr>';	
		}
		
	}
	if(str!=""){
		str=str.substr(1);
		CUI("#flowInfos").val(str)
	}
	if(html!=''){ 
		CUI("#flowActiveTable").html(html);
	}
}
foundation.expectedConsign.deleteRow=function(obj){
	if(confirm("${getText('ec.common.checkDelete')}")){
		
		var flowInfos=CUI("#flowInfos").val();
		var flowKey=CUI(obj).attr('flowKey');
		var code =CUI(obj).attr('code');
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
		CUI(obj).parent().parent().remove();
	}
}
foundation.expectedConsign.selectStaff=function(){
	
	var windowStyle = "width=800,height=600,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
	foundation.common.callOpenWeb("user",windowStyle,null,false,false,"expectedConsign_getStaffUser","forCompanyAdmin=true");

}
expectedConsign_getStaffUser=function(arr){
	var userId=arr[0].userID;
	var staffName=arr[0].staffName;
	CUI("#expectedUserId").val(userId);
	CUI("#exUserStaffName").val(staffName);
}
</script>
