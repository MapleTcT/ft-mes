<#assign expand = "true">
<#if (Parameters.openType)?default('page') != 'dialog'>
<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>${getText('ec.proxyPending.manage')}</title>
<@maincss/>
<@mainjs/>
</head>
<body>
<@loadpanel></@loadpanel>
<@errorbar id="workbenchErrorBar" offsetY=0 />
</#if>
<style type="text/css">
.ewc-dialog-el{height:100%;}
 </style>
<@errorbar id="proxyPendingFrameErrorBar" />
<@frameset id="workbenchOperateBar" border=0>
	<@frame id="infoContent" region="center" offsetH=0 >
		<div>
			<form id="department_queryForms" onsubmit="return false;">
			<@dataclassific id="dataClassific" formId="department_queryForms" confirmClick="ec.pending.showDataClass()" dataTableId="proxyPendingListTable" dgList="[{\"dgname\":\"${getText('ec.pending.pendingclass')}\",\"dgcode\":\"pendingGroup\",\"dgtype\":false,\"dgvalue\":[{\"code\":\"pendingbyuser\",\"dcvalue\":\"${getText('ec.pending.staffclass')}\"},{\"code\":\"pendingbyflow\",\"dcvalue\":\"${getText('ec.pending.flowclass')}\"},{\"code\":\"pending\",\"dcvalue\":\"${getText('ec.pending.noclass')}\"}]}]" />
			<!--<div class="cui-fast-split"></div>-->
			<@quickquery formId="department_queryForms" isExpandAll=true datatableId="proxyPendingListTable"  divStyle="margin:10px 0px 5px 0px;float:left;"
						 fieldcodes="flowName:ec.pending.flowName||activeName:ec.pending.activeName||tableNo:ec.pending.tableNo||staffName:ec.pending.proxy.owner||base_department_name:ec.pending.ownerDeptName||summary:ec.property.summary||create_time:ec.common.createTime" unique="LAST_QUERY_ec_pending_proxyPending_proxyPendingFrame" >
			     <@queryfield formId="department_queryForms" code="flowName"  isCustomize=true>
			     	<div class="fix-input">
			     		<input type="text" id="flowName"  class="cui-noborder-input" />
			     	</div>
			     </@queryfield>
			     <@queryfield formId="department_queryForms" code="activeName" isCustomize=true>
			     	<div class="fix-input">
			     		<input type="text" id="activeName"   class="cui-noborder-input" />
			     	</div>
			     </@queryfield>
			     <@queryfield formId="department_queryForms" code="tableNo" isCustomize=true>
			     	<div class="fix-input">
			     		<input type="text" id="tableNo"  class="cui-noborder-input" />
			     	</div> 
			     </@queryfield>
			     <@queryfield formId="department_queryForms" code="staffName" isCustomize=true>
			    	 <input type="hidden" id="staff.id" name="staff.id" value="" />
			    	 <@mneclient iframe=true formId="department_queryForms" name="staff.name" id="staffName" url="other" classStyle="cui-noborder-input" type="Staff" multiple=false clicked=true isPrecise=true searchClick="ec.pending.queryList()"/>
			     </@queryfield>
			     <@queryfield formId="department_queryForms" code="base_department_name"  key="department.name" mneurl="other" type="Department" ishavingDown=true></@queryfield>
				 <@queryfield formId="department_queryForms" code="summary" isCustomize=true>
				 	<div class="fix-input">
			     		<input type="text" id="summary" class="cui-noborder-input" />
			     	</div>
			     </@queryfield>
			     <@queryfield formId="department_queryForms" code="create_time" showFormat="YMD" isCustomize=true>
				  	 <div>
				  	 	<div style="float:left;width:45%">
					  	 <@datepicker name="createDate_start" id="createDate_start" formid="department_queryForms" type="date" cssStyle="float:left;width:90px"  exp="gequal" cssClass="cui-noborder-input" ></@datepicker>
						</div>
						<div style="float:left;width:10%;text-align:center;line-height:26px;">${getHtmlText('foundation.permissionQuery.to')}</div>
						<div style="float:left;width:45%">
						 <@datepicker name="createDate_end" id="createDate_end" formid="department_queryForms" type="date" cssStyle="float:left;width:90px"  exp="lequal" cssClass="cui-noborder-input" ></@datepicker>
				  		</div>
				  	</div>
			  	 </@queryfield>
			     <div class="quick-query-buttonbar" style="margin-bottom:6px;margin-right:12px;">
					<@querybutton formId="department_queryForms" type="adv" onclick="ec.pending.queryList()" onadvancedclick="ec.pending.advQueryDialogForm()"/>
			 	 	<@querybutton formId="department_queryForms" type="clear"  />
			 	 </div>
			</@quickquery>
			</form>
		</div>
		<div id="proxyPending_adv_query" style="display:none;">
			<form id="adv_queryForm" onsubmit="return false;">
				<table class="infoTable" id="editInfo" cellpadding="0" cellspacing="0" border="0" align="center" width="90%" style="margin-top: 5px">
				<tr style="height: 25px">
						<td align="right" style="width: 20%;padding-right:5px;">${getHtmlText('ec.pending.tableNo')}</td>
						<td style="width: 30%"><div class="fix-input"><input type="text" id="adv_tableNo"  class="cui-noborder-input" /></div></td>
						<td style="width: 20%;padding-right:5px;" align="right">${getHtmlText('ec.pending.proxy.owner')}</td>
						<td style="width: 30%"><@mneclient iframe=true name="adv.staff.name"  formId="adv_queryForm" id="adv_staffName" url="other" classStyle="cui-noborder-input" type="Staff" multiple=false clicked=true isPrecise=true/></td>
					</tr>
					<tr>
						<td align="right" style="width: 20%;padding-right:5px;">${getHtmlText('ec.pending.flowName')}</td>
						<td style="width: 30%"><div class="fix-input"><input type="text" id="adv_flowName"  class="cui-noborder-input" /></div></td>
						<td align="right" style="width: 20%;padding-right:5px;">${getHtmlText('ec.pending.activeName')}</td>
						<td style="width: 30%"><div class="fix-input"><input type="text" name="activeName" id="adv_activeName"  class="cui-noborder-input" /></div></td>

					</tr>
					<tr>
						<td align="right" style="width: 20%;padding-right:5px;">${getHtmlText('ec.pending.ownerDeptName')}</td>
						<td style="width: 30%" >
							<@mneclient iframe=true name="department.name" formId="adv_queryForm" clicked=true id="adv_departmentName" url="other" classStyle="cui-noborder-input" type="Department" multiple=false clicked=true isPrecise=true/>
						</td>
						<td style="text-align:left;padding-left:5px;">
						<input type="checkbox"  checked="true" style="border:none;" id="adv_departmentLower" name="adv_departmentLower" value="true" />${getHtmlText('ec.pending.ownSubDept')}
						</td>
						<td></td>
					</tr>
					<tr>
						<td style="text-align:right;width:20%;padding-right:5px;">${getHtmlText('ec.property.summary')}</td>
						<td style="width:30%">
							<div class="fix-input">
								<input type="text" id="adv_summary" class="cui-noborder-input" />
							</div>
						</td>
					</tr>
				</table>
			</form>
		</div>
		<div id="ptContent">
		
		</div>
	</@frame>
</@frameset>

<script type="text/javascript" charset="utf-8" language="javascript">
(function(){

	//注册命名空间
	CUI.ns("ec.pending");
	ec.pending.queryType="user";
	ec.pending.operateType="quickQuery";//quickQuery快速查询，advQuery高级查询
	//查询
	ec.pending.queryList=function(groupType ,pageNo){
		var url="/msService/ec/pending/proxyPending/listByGroup";
		var isDetail=false;
		if($(".datagrid").closest(".cui-datagrid-js").attr("id")=="proxyPendingListTable2"){
			isDetail= true;
		}
		if(isDetail){
			var pendingList=proxyPendingListTable2Widget;
		}else{
			var pendingList=proxyPendingListTableWidget;
		}
		
		if(!ec.pending.getFormData()){
			return false;
		}
		var dataPost=ec.pending.getFormData();
		pendingList.defaultPostData = "";
		pendingList.setAttributeConfig('queryFunc', {
                writeOnce: true,
                value: "ec.pending.queryList(ec.pending.queryType)"
            }); 
		 if(undefined != pageNo) {
       		dataPost += "&records.pageNo=" + pageNo;
       	}
        dataPost+="&"+department_queryForms_getCookieParam();
        if(isDetail){
        	datatable_proxyPendingListTable2.setRequestDataUrl(url,dataPost);
        }else{
        	datatable_proxyPendingListTable.setRequestDataUrl(url,dataPost);
        }
		
		if($("input[id^='queryParam']")){
			$("input[name='deptId']").val('');
			$("input[name='userId']").val('');
			$("input[name='flowKey']").val('');
			$("input[name='activeCode']").val('');
		}	
	};
	//高级查询
	ec.pending.advQueryList=function(groupType,pageNo){
		var url="/msService/ec/pending/proxyPending/listByGroup";
		var isDetail=false;
		if($(".datagrid").closest(".cui-datagrid-js").attr("id")=="proxyPendingListTable2"){
			isDetail= true;
		}
		if(isDetail){
			var pendingList=proxyPendingListTable2Widget;
		}else{
			var pendingList=proxyPendingListTableWidget;
		}
		var dataPost=ec.pending.getAdvFormData();
		pendingList.defaultPostData = "";
		 
		pendingList.setAttributeConfig('queryFunc', {
            writeOnce: true,
            value: "ec.pending.advQueryList(ec.pending.queryType)"
        });
  
        if(undefined != pageNo) {
       		dataPost += "&records.pageNo=" + pageNo;
       	}
         
		pendingList.setRequestDataUrl(url,dataPost);
		if(ec.pending.advQueryDialog.isShow!=-1){
			ec.pending.advQueryDialog.close();
		}
		if($("input[id^='queryParam']")){
			$("input[name='deptId']").val('');
			$("input[name='userId']").val('');
			$("input[name='flowKey']").val('');
			$("input[name='activeCode']").val('');
		}	
	}
	ec.pending.advQueryDialogForm=function(){
		ec.pending.advQueryDialog = new CUI.Dialog({
				title: "${getHtmlText('ec.tableInfo.modifyOwner.advQuery')}",
				elementId: "proxyPending_adv_query",
				formId: "adv_queryForm",
				modal:true,
				type:3,
				buttons:[{	name:"${getHtmlText('common.button.query')}",
							handler:function(){ec.pending.advQueryList();}
						},
						{	name:"${getHtmlText('common.button.clear')}",
							handler:function(){ec.pending.resetAdvQuery();}
						},
						{	name:"${getHtmlText('common.button.cancel')}",
							handler:function(){this.close()}
						}]
			});
			ec.pending.advQueryDialog.show();
	
	}
	//重置高级查询
	ec.pending.resetAdvQuery=function(){
		CUI.resetForm('adv_queryForm');
	}
	//组织高级查询条件
	ec.pending.getAdvFormData=function(){
		var dataPost= "&staffName=" +encodeURIComponent(CUI.trim(CUI("#adv_staff_name",$('#adv_queryForm')).val()));
		dataPost += "&departmentName=" +encodeURIComponent(CUI.trim(CUI("#department_name",$('#adv_queryForm')).val()));
		dataPost += "&flowName=" +encodeURIComponent(CUI.trim(CUI("#adv_flowName").val()));
		dataPost += "&activeName=" +encodeURIComponent(CUI.trim(CUI("#adv_activeName").val()));
		dataPost += "&tableNo=" +encodeURIComponent(CUI.trim(CUI("#adv_tableNo").val()));
		dataPost += "&summary=" + encodeURIComponent(CUI.trim($('#adv_summary').val()));
		dataPost += "&groupType=" +ec.pending.queryType;
		var departmentLower=CUI("#adv_departmentLower").prop("checked");
		if(departmentLower==true){
			dataPost +="&departmentLower=true";
		}
		var pageSize=CUI('input[name="proxyPendingListTable_PageLink_PageCount"]').val();
		dataPost += "&records.pageSize="+encodeURIComponent(pageSize);	
		return dataPost;
	}
	//组织快速查询条件
	ec.pending.getFormData=function(){
		var timeE=CUI.trim(CUI("#createDate_end").val());
		var timeS=CUI.trim(CUI("#createDate_start").val());
		if(timeE!=''&&null!=timeE){
			timeE += " 23:59:59";
		}
		var isDetail=false;
		if($(".datagrid").closest(".cui-datagrid-js").attr("id")=="proxyPendingListTable2"){
			isDetail= true;
		}
		if(timeE!=''&&!isDateTime(timeE)||timeS!=''&&!isDateTime(timeS)){
			proxyPendingFrameErrorBarWidget.showMessage("${getHtmlText('ec.list.validate.datetime')}","f");
			return false;
		}
		if(timeE!=''&&timeS!=''&&timeS>timeE){
			proxyPendingFrameErrorBarWidget.showMessage("${getHtmlText('foundation.staffCalendar.startLarger')}","f");
			return false;
		}
		var dataPost= "&staffName=" +encodeURIComponent(CUI.trim(CUI("#staff_name").val()));
		dataPost += "&staff.id=" +encodeURIComponent(CUI.trim($("[id='staff.id']").val()));
		dataPost += "&departmentName=" +encodeURIComponent(CUI.trim(CUI("#department_name",$('#department_queryForms')).val()));
		dataPost += "&department.id=" +encodeURIComponent(CUI.trim($("[id='department.id']").val()));
		dataPost += "&createDate_start=" +encodeURIComponent(timeS);
		dataPost += "&createDate_end=" +encodeURIComponent(timeE);
		dataPost += "&flowName=" +encodeURIComponent(CUI.trim(CUI("#flowName").val()));
		dataPost += "&activeName=" +encodeURIComponent(CUI.trim(CUI("#activeName").val()));
		dataPost += "&tableNo=" +encodeURIComponent(CUI.trim(CUI("#tableNo").val()));
		dataPost += '&summary=' + encodeURIComponent(CUI.trim($('#summary').val()));
		dataPost += "&groupType=" +ec.pending.queryType;
		var departmentLower=CUI("#DepartmentLower").prop("checked");
		if(departmentLower==true){
			dataPost +="&departmentLower=true";
		}
		var pageSize = CUI('input[name="proxyPendingListTable_PageLink_PageCount"]').val();
		if(isDetail){
			pageSize = CUI('input[name="proxyPendingListTable2_PageLink_PageCount"]').val();
		}
		dataPost += "&records.pageSize="+encodeURIComponent(pageSize);	
		return dataPost;
	};
	ec.pending.firstLoad=function(){
		var url="/msService/ec/pending/proxyPending/listByUser";
		var dataPost=ec.pending.getFormData();
		url+="?ptDataPost="+encodeURIComponent(dataPost);
		CUI("#base_pendingRemind").hide();
		CUI("#base_proxyPending").hide();
		CUI("#ptContent").load(url);
		setTimeout(function(){
			ec.pending.queryList();
		}, 1000);
		
	};

	ec.pending.viewPendings=function(evt,obj){
		//alert("USERID:" + obj.USERID );
		ec.pending.queryType='pending';
		var staffName=obj.STAFFNAME;
		var staffId=obj.STAFFID;
		var deptName=obj.DEPTNAME;
		var userId=obj.USERID;
		var deptId=obj.DEPTID;
		var dataPost=ec.pending.getFormData();
		dataPost+="&userId="+userId+"&deptId="+deptId;
		if($("#department_queryForms_btn_collapse").css("display")=="none"||$("#department_queryForms_btn_collapse").css("display")==undefined){
			$("#queryParam__jQSelect01").find("h4").trigger("click");
			$("#ulqueryParam__jQSelect01").find("li").eq(0).trigger("click");
		}
		$("#staff_name").val(staffName);
		$("[id='staff.id']").val(staffId);
		$("#staff_name_mneTipLabel").css("display","none");
		$("#department_name").val(deptName);
		$("[id='department.id']").val(deptId);
		$("#department_name_mneTipLabel").css("display","none")
		var url = "/msService/ec/pending/proxyPending/proxyPendingList?ptDataPost=" + encodeURIComponent(dataPost);
		CUI("#ptContent").load(url);
		setTimeout(function(){
			var pturl="/msService/ec/pending/proxyPending/listByGroup";
			ec.pending.queryList();
		}, 1000);
	};
	ec.pending.viewPendingsByFlow=function(evt,obj){
		ec.pending.queryType='pending'
		$("#flowName").val(obj.FLOWNAME);
		$("#activeName").val(obj.ACTIVENAME);
		var flowKey=obj.FLOWKEY;
		var activeCode=obj.ACTIVECODE;
		var dataPost=ec.pending.getFormData();
		dataPost+="&flowKey="+flowKey+"&activeCode="+activeCode;
		var url = "/msService/ec/pending/proxyPending/proxyPendingList?ptDataPost=" + encodeURIComponent(dataPost) + "&flowKey=" + flowKey + "&activeCode=" + activeCode;
		CUI("#ptContent").load(url);
		setTimeout(function(){
			var pturl="/msService/ec/pending/proxyPending/listByGroup";
			ec.pending.queryList();
		}, 1000);
	};
	ec.pending.firstLoad();
	
	ec.pending.proxyPending=function(){
		
		var pendingIds='';
		var isDetail=false;
		if($(".datagrid").closest(".cui-datagrid-js").attr("id")=="proxyPendingListTable2"){
			isDetail= true;
		}
		if(isDetail){
			var selectedRowsObj=proxyPendingListTable2Widget.selectedRows;
		}else{
			var selectedRowsObj=proxyPendingListTableWidget.selectedRows;
		}
	//	var selectedRowsObj=proxyPendingListTableWidget.getEditData();
	//	var selectedRowsObj=proxyPendingListTable2Widget.selectedRows;
		if(selectedRowsObj.length==0){
			proxyPendingFrameErrorBarWidget.showMessage("${getHtmlText('ec.expectedConsign.noSelected')}","f");
			return false;
		}
		for(var i=0;i<selectedRowsObj.length;i++){
			var obj=selectedRowsObj[i];
			pendingIds+=","+obj['ID'];
		}
		
		var url="/msService/ec/workflow/proxyPending?pendingIds="+pendingIds.substr(1);
		//alert(url);
		CUI(function(){
			ec.pending.proxyPendingDialog = new CUI.Dialog({
				title: "${getHtmlText('ec.proxyPending.manage')}",
				url:url,
				modal:true,
				type:3,
				dragable:true,
				buttons:[
						{	name:"${getHtmlText('common.button.check')}",
							handler:function(){ec.pending.saveProxyPending()}
						},
						{	name:"${getHtmlText('common.button.cancel')}",
							handler:function(){this.close()}
						}]
			})
			ec.pending.proxyPendingDialog.show();
		});
	};
	
	ec.pending.saveProxyPending=function(){
		if(CUI("#proxyUsers_MultiIDs").val()==""){
			proxyPendIngDialogErrorBarWidget.show("${getHtmlText('ec.proxyPending.proxyor')}","f");
			return ;
		}else{
			CUI("#proxyUsersInput").val(CUI("#proxyUsers_MultiIDs").val());
		}
		CUI('#SubmitForm').submit();
	};
	
	proxyPendingCallBackInfo=function(res){
		if(res.dealSuccessFlag == true){
			proxyPendIngDialogErrorBarWidget.show("${getHtmlText('ec.common.saveandclosesuccessful')}","s");
			setTimeout(function(){
				try{
					ec.pending.proxyPendingDialog.close();	
					
					ec.pending.queryList();
				}catch(e){}
			},1500);
		}else{
			proxyPendingFrameErrorBarWidget.show("${getHtmlText('ec.common.unsuccessfully')}",'f');
			if(CUI.Dialog) CUI.Dialog.toggleAllButton();
		}
	};
	//催办
	ec.pending.remind=function(){
		//var selectedRowsObj=pendingListTableWidget.getEditData();
		var selectedRowsObj=proxyPendingListTable2Widget.selectedRows;
		if(selectedRowsObj.length==0){
			proxyPendingFrameErrorBarWidget.showMessage("${getHtmlText('ec.expectedConsign.noSelected')}","f");
			return false;
		}
		var pendingIds='';
		for(var i=0;i<selectedRowsObj.length;i++){
			var obj=selectedRowsObj[i];
			pendingIds+=","+obj['ID'];
		}
		if(pendingIds!=""){
			pendingIds=pendingIds.substr(1);
		}
		var url="/msService/ec/workflow/remindSet?pendingIds="+pendingIds;
		ec.pending.remindDialog = new CUI.Dialog({
			title: "${getHtmlText('ec.pending.remind')}",
			url:url,
			modal:true,
			type:3,
			dragable:true,
			buttons:[
					{	name:"${getHtmlText('common.button.check')}",
						handler:function(){
							ec.pending.remindResult();
						}
					},
					{	name:"${getHtmlText('common.button.cancel')}",
						handler:function(){this.close()}
		
					}]
			});
		ec.pending.remindDialog.show();
	
		
	}
	ec.pending.remindResult=function(){
		var pandion=CUI("#pandion").prop("checked");
		var email=CUI("#email").prop("checked");
		var sms=CUI("#sms").prop("checked");
		if(!pandion&&!email&&!sms){
			remidnSubmitFormDialogErrorBarWidget.showMessage("${getHtmlText('ec.pengding.nullType')}","f");
			return false;
		}
		var remindContent=CUI("#remindContent").val();
		if(remindContent!=""){
			if(sms&&remindContent.length>20){
				remidnSubmitFormDialogErrorBarWidget.showMessage("${getHtmlText('ec.remind.remindContent')}","f");
				return false;
			}
		}
		var i=0;
		if(pandion){
			i+=1;
		}
		if(email){
			i+=2;
		}
		if(sms){
			i+=4;
		}
		CUI("#remindTypeId").val(i);
		CUI('#remidnSubmitForm').submit();
	}
	//回调
	waitWork_remindCallBack=function(res){
		if(res.dealSuccessFlag == true){
			remidnSubmitFormDialogErrorBarWidget.showMessage("${getHtmlText('ec.common.saveandclosesuccessful')}","s");
			setTimeout(function(){
				try{ec.pending.remindDialog.close();}catch(e){}
			},1500);
		}else{
			remidnSubmitFormDialogErrorBarWidget.showMessage("${getHtmlText('ec.common.unsuccessfully')}",'f');
			if(CUI.Dialog) CUI.Dialog.toggleAllButton();
			
		}
	}
	ec.pending.setEditFlag=function(obj){
		var td=obj.cellHtmlObj;
		if(!obj['CHECKBOX']||obj['CHECKBOX']=='false'){
			td.parentNode.setAttribute('isEdited', 'false');
		}
	}
	ec.pending.selectStaff=function(){
		var windowStyle = "width=800,height=600,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
		foundation.common.callOpenWeb("user",windowStyle,null,null,'true',"ec.pending.staffInfoCallBack","forCompanyAdmin=true&unassignUserSupport=true");
	
	}
	ec.pending.selectAdvepartment=function(){
		//var windowStyle = "width=800,height=600,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
		ec.pending._dialog = foundation.common.select({
				pageType : 'department',
				closePage : true,
				callBackFuncName : 'ec.pending.advDeptInfoCallBack',
				params : 'forCompanyAdmin=true&unassignUserSupport=true'
			});
		//foundation.common.select("department",windowStyle,null,null,'true',"ec.pending.deptInfoCallBack","forCompanyAdmin=true&unassignUserSupport=true");
	
	}
	ec.pending.advDeptInfoCallBack=function(arr){
		CUI('#adv_staffName').val(arr[0].staffName);
	}
	
	ec.pending.staffInfoCallBack=function(arr){
		CUI('#staffName').val(arr[0].staffName);
	}
	ec.pending.selectPDepartment=function(){
		//var windowStyle = "width=800,height=600,scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
		ec.pending._dialog = foundation.common.select({
				pageType : 'department',
				closePage : true,
				callBackFuncName : 'ec.pending.deptInfoCallBack',
				params : 'forCompanyAdmin=true&unassignUserSupport=true'
			});
		//foundation.common.select("department",windowStyle,null,null,'true',"ec.pending.deptInfoCallBack","forCompanyAdmin=true&unassignUserSupport=true");
	
	}
	ec.pending.deptInfoCallBack=function(arr){
		CUI('#Department_name').val(arr[0].name);ec.pending._dialog.close();
	}
	ec.pending.showDataClass=function(){
		var url="";
		var type=$('input[type="radio"]:checked',$('#department_queryForms_dcUl')).val();
		if(type=='pendingbyflow'&&ec.pending.queryType!='flow'){
			ec.pending.queryType='flow';
			url="/msService/ec/pending/proxyPending/listByFlow";
			CUI("#base_pendingRemind").hide();
			CUI("#base_proxyPending").hide();
		}else if(type=='pending'&&ec.pending.queryType!='pending'){
			ec.pending.queryType='pending';
			url="/msService/ec/pending/proxyPending/proxyPendingList";
			CUI("#base_pendingRemind").show();
			CUI("#base_proxyPending").show();
			setTimeout(function(){
			var pturl="/msService/ec/pending/proxyPending/listByGroup";
			ec.pending.queryList();
			}, 1000);
		}else if(type=='pendingbyuser'&&ec.pending.queryType!='user'){
			ec.pending.queryType='user';
			CUI("#base_pendingRemind").hide();
			CUI("#base_proxyPending").hide();
			url="/msService/ec/pending/proxyPending/listByUser";
			setTimeout(function(){
				ec.pending.queryList();
			}, 800);
		}else{
			return ;
		}
		var dataPost=ec.pending.getFormData();
		url+="?ptDataPost="+encodeURIComponent(dataPost);
		CUI("#ptContent").load(url);
	}
	ec.pending.intitPt=function(){
		if(ec.pending.queryType=='user'){
			CUI("#proxyPendingListTable_dcSpan").text("${getText('ec.pending.staffclass')}");
			CUI("#proxyPendingListTable_dcSpan").attr("title","${getText('ec.pending.staffclass')}");
			
		}else if(ec.pending.queryType=='flow'){
			CUI("#proxyPendingListTable_dcSpan").text("${getText('ec.pending.flowclass')}");
			CUI("#proxyPendingListTable_dcSpan").attr("title","${getText('ec.pending.flowclass')}");
		}else{
			CUI("#proxyPendingListTable_dcSpan").text("${getText('ec.pending.noclass')}");
			CUI("#proxyPendingListTable_dcSpan").attr("title","${getText('ec.pending.noclass')}");
		}
		CUI("#proxyPendingListTable_dcSpan").addClass('dc-text'); 
	}
	
	// 删除
	ec.pending.deleteMethod = function(){
		var rows = proxyPendingListTable2Widget.selectedRows;
		if(rows.length == 0){
			proxyPendingFrameErrorBarWidget.showMessage("${getHtmlText('ec.expectedConsign.noSelected')}", "f");
			return;
		}
		CUI.Dialog.confirm("${getHtmlText('ec.proxyPending.checkDelete')}", function(){
			var idStr = '';
			$.each(rows, function(i, item){
				idStr += idStr.length == 0 ? item.ID : ',' + item.ID;
			});
			$.ajax({
				async : false,
				type : "POST",
				url : "/msService/ec/workflow/checkBatchDelete",
				data : {pendingIds : idStr},
				success : function(res){
					if(res.dealSuccessFlag){
						$.post("/msService/ec/workflow/batchDeletePending", {pendingIds : idStr}, function(res){
							if(res.dealSuccessFlag){
								proxyPendingFrameErrorBarWidget.showMessage("${getHtmlText('foundation.common.deleteandrefreshsuccessful')}", "s");
								var dataPost = ec.pending.getFormData();
								if($("input[id^='queryParam']")){
									dataPost += ($("#queryParam_deptId").val() != null && $("#queryParam_deptId").val() != '' ? "&deptId=" + $("#queryParam_deptId").val() : '');
									dataPost += ($("#queryParam_userId").val() != null && $("#queryParam_userId").val() != '' ? "&userId=" + $("#queryParam_userId").val() : '');
									dataPost += ($("#queryParam_flowKey").val() != null && $("#queryParam_flowKey").val() != '' ? "&flowKey=" + $("#queryParam_flowKey").val() : '');
									dataPost += ($("#queryParam_activeCode").val() != null && $("#queryParam_activeCode").val() != '' ? "&activeCode=" + $("#queryParam_activeCode").val() : '');
								}
								proxyPendingListTable2Widget.setRequestDataUrl("/msService/ec/pending/proxyPending/listByGroup", dataPost);
							}							
						});
					}else{
						proxyPendingFrameErrorBarWidget.showMessage("${getHtmlText('com.supcon.orchid.container.exceptions.NO_PENDING')}", "f");
					}
				},
				error : function(XMLHttpRequest, textStatus, errorThrown){
					var msg = CUI.parseJSON(XMLHttpRequest.responseText);
					if (msg.exceptionMsg) {
						proxyPendingFrameErrorBarWidget.showMessage(msg.exceptionMsg, "f");
					}
				}			
			});
		});
		
	}
})();
</script>
<#if (Parameters.openType)?default('page') != 'dialog'>
</body>
</html>
</#if>