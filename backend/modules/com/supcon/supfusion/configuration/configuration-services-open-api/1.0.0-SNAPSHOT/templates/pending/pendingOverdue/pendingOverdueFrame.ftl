<#if (Parameters.openType)?default('page') != 'dialog'>
<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>${getText('ec.pendingOverdue.manage')}</title>
<@maincss/>
<@mainjs/>
</head>
<body>
<@loadpanel></@loadpanel>
<@errorbar id="workbenchErrorBar" offsetY=0 />
</#if>
<style type="text/css">
.ewc-dialog-el{height:100%;}
.datagrid-wrapper .paginatorbar-operatebar a{margin-left:1px;}
.datagrid-wrapper .paginatorbar-operatebar a:hover{margin-left:0px;}
#treeContent{background-color:#f3f3f3;overflow-x:hidden;overflow-y:auto;}
#pendingListTableContentDiv{margin-left:13px;float:left;*float:none;height:22px;border:1px solid #baddf1;border-bottom:0px;background:url("/bap/static/datatable/assets/roletop.gif") repeat-x;}
 </style>
<@errorbar id="pendingFrameErrorBar" />
<@frameset id="workbenchOperateBar" border=0>
	<@frame id="infoContent" region="center" offsetH=0 >
	<@s.hidden name="statisticsTime" id="statisticsTime"/>
		<div id="ptContent"  style="height:100%">
			<div  id="quickQueryDiv">
				<form id="queryForms" onsubmit="return false;">
				<@quickquery formId="queryForms" isExpandAll=true datatableId="pendingListTable" fieldcodes="flowName:ec.pending.flowName||activeName:ec.pending.activeName||tableNo:ec.pending.tableNo||base_staff_name:ec.pending.proxy.owner||base_department_name:ec.pending.ownerDeptName||summary:ec.property.summary" unique="LAST_QUERY_pendingOverdue_frame">
				     <@queryfield formId="queryForms" code="flowName"  isCustomize=true>
				     	<div class="fix-input">
				     		<input type="text" id="flowName"  class="cui-noborder-input" />
				     	</div>
				     </@queryfield>
				     <@queryfield formId="queryForms" code="activeName" isCustomize=true>
				     	<div class="fix-input">
				     		<input type="text" id="activeName"  class="cui-noborder-input" />
				     	</div>
				     </@queryfield>
				     <@queryfield formId="queryForms" code="tableNo" isCustomize=true>
				     	<div class="fix-input">
				     		<input type="text" id="tableNo"  class="cui-noborder-input" />
				     	</div>
				     </@queryfield>
				     <@queryfield formId="queryForms" code="base_staff_name" key="staff.name" mneurl="other" type="Staff" searchClick="ec.pending.queryList()"></@queryfield>
				     <@queryfield formId="queryForms" code="base_department_name" key="department.name" mneurl="other" type="Department" ishavingDown=true searchClick="ec.pending.queryList()"></@queryfield>
			    	<@queryfield formId="queryForms" code="summary" isCustomize=true>
			    		<div class="fix-input">
			     			<input type="text" id="summary"  class="cui-noborder-input" />
			     		</div>
			    	</@queryfield>
			    	<div class="quick-query-buttonbar" style="margin-bottom:6px;margin-right:12px;">
						<@querybutton formId="queryForms" type="adv" onclick="ec.pending.queryList()" onadvancedclick="ec.pending.advQuery()" />
				 		<@querybutton formId="queryForms" type="clear"  />
				 	</div>
				</@quickquery>
				</form>
			</div>
			<div id="advQueryArea" style="display:none;">
		   		<form id="advQueryForm"  name="advQueryForm" onsubmit="return false;">
		   			<table cellpadding="0" cellspacing="0" border="0"  width="94%" style="margin: 10px 0 0 12px;">
			             <tr>
			               <td width="10%" style="text-align:right;padding-right:3px;">${getHtmlText('ec.pending.flowName')}</td>
			               <td width="20%">
								<div class="fix-input">
									<input type="text" id="advFlowName"  class="cui-noborder-input" />
								</div>
			               </td>
			               <td width="10%" style="text-align:right;padding-right:3px;">${getHtmlText('ec.pending.activeName')}</td>
			               <td width="20%">   	
			               		<div class="fix-input">
			               			<input type="text" id="advActiveName"  class="cui-noborder-input" />
								</div>
			               	</td>
			             </tr>
			            <tr height="8px">
			            </tr>
			            <tr>
			                <td width="10%" style="text-align:right;padding-right:3px;">${getHtmlText('ec.pending.tableNo')}</td>
			               <td width="20%"> 	
			               		<div class="fix-input">
			               			<input type="text" id="advTableNo"  class="cui-noborder-input" />
								</div>
			               	</td>
			               <td width="10%" style="text-align:right;padding-right:3px;">${getHtmlText('ec.pending.proxy.owner')}</td>
			                <td width="20%">
			                 <@mneclient name="adv.staff.name" formId="advQueryForm" id="adv_staff_Name" url="other" classStyle="cui-noborder-input" type="Staff"   mnewidth=260 multiple=false clicked=true isPrecise=true/>
							</td>
			            <tr>
			            <tr height="8px">
			            </tr>
			             <tr>
			               <td width="10%" style="text-align:right;padding-right:3px;">${getHtmlText('ec.pending.ownerDeptName')}</td>
			                <td width="20%">
			                 <@mneclient name="adv.department.name" formId="advQueryForm" id="adv_department_Name" url="other" classStyle="cui-noborder-input" type="Department"   mnewidth=260 multiple=false clicked=true isPrecise=true/>
							</td>
							<td style="text-align:left;padding-left:5px;">
							<input type="checkbox"  checked="true" style="border:none;" id="adv_departmentLower" name="adv_departmentLower" value="true" />${getHtmlText('ec.pending.ownSubDept')}
							</td>
			            <tr>
			            <tr height="8px">
			            </tr>
			            <tr>
			            	<td width="10%" style="text-align:right;padding-right:3px;">${getHtmlText('ec.property.summary')}</td>
			            	<td width="20%">
			            		<div class="fix-input">
			               			<input type="text" id="advSummary" class="cui-noborder-input"/>
								</div>
			            	</td>
			            </tr>
		          </table>
		   		</form>
		   </div>
		
			<@datatable hidekey="['ID','TABLEID','STATISTICS_DATE']"  postData="${ptDataPost!}" pageInitMethod="ec.pending.intitPt" 
			dtPage="records"  transMethod="post" id="pendingListTable" dataUrl="/msService/ec/pending/pendingOverdue/pendingOverdueList" style="margin:6px 4px 2px 13px;" renderOverEvent="ec.pending.addPendingOverdueTop">
			<@operatebar operates="code:base_pendingOverdue||onclick:ec.pending.pendingOverdue();code:base_pendingOverdueRemind||onclick:ec.pending.remind()" resultType="json" >
			</@operatebar>
			<@datacolumn textalign="center" key="checkbox" type="checkbox" checkall="true" label="" width=60 />
			<@datacolumn key="TABLE_NO" label="${getHtmlText('ec.pending.tableNo')}" width=180 />
			<@datacolumn key="SUMMARY" label="${getHtmlText('ec.property.summary')}" width=180 />
			<@datacolumn key="TASK_TYPE" textalign="center" label="${getHtmlText('ec.pending.pendingType')}" type="select" options="{'0':'${getText('ec.pending.normalPending')}','1':'${getText('ec.pending.normalPending')}','2':'${getText('ec.pending.proxyPending')}'}"  width=60 />
			<@datacolumn key="DEPLOYMENT_NAME" label="${getHtmlText('ec.pending.flowName')}" width=120 />
			<@datacolumn key="TASK_NAME" label="${getHtmlText('ec.pending.activeName')}" width=120 />
			<@datacolumn key="USER_NAME" label="${getHtmlText('ec.pending.owner')}" width=70 />
			<@datacolumn key="DEPARTMENT_NAME" label="${getHtmlText('ec.pending.ownerDept')}" width=100 />
			<@datacolumn key="CREATE_TIME" textalign="center" type="datetime" label="${getHtmlText('ec.pending.createTime')}" width=130 />
			<@datacolumn key="STAFF_NAME" label="${getHtmlText('ec.pending.inputor')}" width=70 />
			<@datacolumn key="residence_time" textalign="right" label="${getHtmlText('ec.pending.residenceTime')}" width=120 />
			<@datacolumn key="overdue_time" textalign="right" label="${getHtmlText('ec.pending.overdueTime')}" width=120 />
			</@datatable>
		</div>
	</@frame>
</@frameset>

<script type="text/javascript" charset="utf-8" language="javascript">
(function(){

	//注册命名空间
	CUI.ns("ec.pending");
	ec.pending.queryType="user";
	ec.pending.addPendingOverdueTop =function(){
		var closingTime = CUI('#statisticsTime').val();
		if($("#pendingListTableContentDiv").length == 0){
			var PendingOverdueTop ="<div id='pendingListTableContentDiv'>"
						+"<table cellpadding='0' cellspacing='0' border='0' class='detail-top'>"
						+"<tr><td style='padding-left:5px;'>${getHtmlText('ec.pending.closeTime')} :"
						+"<span id='closingTime' readonly='true' style='padding-right:20px;'>"+closingTime+"</span>"
						+"</td></tr></table></div>";
			$(PendingOverdueTop).insertAfter($("#queryForms").eq(0));
	    	$("#pendingListTableContentDiv").width($("#pendingListTable .paginatorbar").eq(0).width()-2);
		}
		else{
		   CUI("#closingTime").html(closingTime);
		}
		pendingListTableWidget._initDomElements();
    }
	
	//查询
	ec.pending.queryList=function(groupType,pageNo){
	    pendingListTableWidget.setAttributeConfig('queryFunc',{
          writeOnce: true,
          value:"ec.pending.queryList(1)"
       });
		var url="/msService/ec/pending/pendingOverdue/pendingOverdueList";
		var pendingList=pendingListTableWidget;
		var dataPost=ec.pending.getFormData();
		if(pageNo!=undefined){
		 	dataPost+="&records.pageNo="+pageNo;
		}
		pendingList.defaultPostData = "";
		pendingList.setRequestDataUrl(url,dataPost);		
	};
	ec.pending.advQuery=function(){
	     ec.pending.operateType='advQuery';
	     CUI(function(){
         ec.pending.advQueryDialog=new CUI.Dialog({
            title:"<@s.text name='ec.tableInfo.modifyOwner.advQuery'/>",
            elementId:"advQueryArea",
            formId:"advQueryForm",
            modal:true,
	        type:3,
	        dragable:true,
	         buttons:[{name:"<@s.text name='foundation.common.query'/>",
	                handler:"ec.pending.advQueryList()"},
	                {name:"<@s.text name='common.button.clear'/>",
                  handler:function(){CUI.resetForm('advQueryForm')}
                 },
	               {name:"<@s.text name='common.button.cancel'/>",
	                handler:function(){this.close()}
	               }]
	      
             });
           ec.pending.advQueryDialog.show();
      })
	}
	ec.pending.advQueryList=function(groupType,pageNo){
	    pendingListTableWidget.setAttributeConfig('queryFunc',{
          writeOnce: true,
          value:"ec.pending.advQueryList(1)"
       });
		var url="/msService/ec/pending/pendingOverdue/pendingOverdueList";
		var pendingList=pendingListTableWidget;
		var dataPost=ec.pending.getAdvFormData();
		if(pageNo!=undefined){
		 	dataPost+="&records.pageNo="+pageNo;
		}
		pendingList.defaultPostData = "";
		pendingList.setRequestDataUrl(url,dataPost);
		if(ec.pending.advQueryDialog.isShow!=-1){
	       ec.pending.advQueryDialog.close();
	    }		
	};
	ec.pending.getAdvFormData=function(){
		var obj=CUI("#advQueryForm");
		var dataPost= "&staffName=" +encodeURIComponent(CUI.trim(CUI("input[name='adv.staff.name']",obj).val()));
		dataPost += "&departmentName=" +encodeURIComponent(CUI.trim(CUI("input[name='adv.department.name']",obj).val()));
		//dataPost += "&createDateS=" +encodeURIComponent(CUI.trim(CUI("#createDateS",obj).val()));
		//dataPost += "&createDateE=" +encodeURIComponent(CUI.trim(CUI("#createDateE",obj).val()));
		dataPost += "&flowName=" +encodeURIComponent(CUI.trim(CUI("#advFlowName",obj).val()));
		dataPost += "&activeName=" +encodeURIComponent(CUI.trim(CUI("#advActiveName",obj).val()));
		dataPost += "&tableNo=" +encodeURIComponent(CUI.trim(CUI("#advTableNo",obj).val()));
		dataPost += '&summary=' + encodeURIComponent(CUI.trim($('#advSummary', obj).val()));
		
		var departmentLower=CUI("#adv_departmentLower").prop("checked");
		if(departmentLower==true){
			dataPost +="&departmentLower=true";
		}
		//var pageSize=CUI('input[name="pendingListTable_PageLink_PageCount"]').val();
		//dataPost += "&records.pageSize="+encodeURIComponent(pageSize);	
		return dataPost;
	};
	ec.pending.getFormData=function(){
		var obj=CUI("#queryForms");
		
		var dataPost= "&staffName=" +encodeURIComponent(CUI.trim(CUI("input[name='staff.name']",obj).val()));
		dataPost += "&departmentName=" +encodeURIComponent(CUI.trim(CUI("input[name='department.name']",obj).val()));
		//dataPost += "&createDateS=" +encodeURIComponent(CUI.trim(CUI("#createDateS",obj).val()));
		//dataPost += "&createDateE=" +encodeURIComponent(CUI.trim(CUI("#createDateE",obj).val()));
		dataPost += "&flowName=" +encodeURIComponent(CUI.trim(CUI("#flowName",obj).val()));
		dataPost += "&activeName=" +encodeURIComponent(CUI.trim(CUI("#activeName",obj).val()));
		dataPost += "&tableNo=" +encodeURIComponent(CUI.trim(CUI("#tableNo",obj).val()));
		dataPost += '&summary=' + encodeURIComponent(CUI.trim($('#summary', obj).val())); 
		
		
		var departmentLower=CUI("#DepartmentLower").prop("checked");
		if(departmentLower==true){
			dataPost +="&departmentLower=true";
		}
		//var pageSize=CUI('input[name="pendingListTable_PageLink_PageCount"]').val();
		//dataPost += "&records.pageSize="+encodeURIComponent(pageSize);	
		dataPost+="&"+queryForms_getCookieParam();
		return dataPost;
	};
	ec.pending.firstLoad=function(){
		var url="/msService/ec/pending/pendingOverdue/pendingOverdueList";
		var dataPost=ec.pending.getFormData();
		url+="?ptDataPost="+encodeURIComponent(dataPost);
		CUI("#base_pendingRemind").hide();
		CUI("#base_proxyPending").hide();
		CUI("#ptContent").load(url);
	};

	ec.pending.viewPendings=function(evt,obj){
		//alert("USERID:" + obj.USERID );
		ec.pending.queryType='pending';
		//var userName=obj.USERNAME;
		var userId=obj.USERID;
		var deptId=obj.DEPTID;
		var dataPost=ec.pending.getFormData();
		dataPost+="&userId="+userId+"&deptId="+deptId;
		var url="/msService/ec/pending/proxyPending/proxyPendingList?ptDataPost="+encodeURIComponent(dataPost);
		CUI("#ptContent").load(url);
	};
	ec.pending.viewPendingsByFlow=function(evt,obj){
		ec.pending.queryType='pending'
		var flowKey=obj.FLOWKEY;
		var activeCode=obj.ACTIVECODE;
		var dataPost=ec.pending.getFormData();
		dataPost+="&flowKey="+flowKey+"&activeCode="+activeCode;
		var url="/msService/ec/pending/proxyPending/proxyPendingList?ptDataPost="+encodeURIComponent(dataPost);
		CUI("#ptContent").load(url);
	};
	//ec.pending.firstLoad();
	
	ec.pending.proxyPending=function(){
		
		var pendingIds='';
		
		//var selectedRowsObj=pendingListTableWidget.getEditData();
		var selectedRowsObj=pendingListTableWidget.selectedRows;
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
					
					CUI("#queryButton").click();
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
		var selectedRowsObj=pendingListTableWidget.selectedRows;
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
	ec.pending.pendingOverdue=function(){
		CUI.Dialog.confirm("${getHtmlText('foundation.pendingOverdue.checkConfirm')}", function(){
				CUI.post("/msService/ec/pending/pendingOverdue/pendingOverdueMark",ec.pending.callBackInfo, "json");
		},null,"${getHtmlText('ec.common.confirm')}");
	
	}
	ec.pending.callBackInfo=function(res){
		if(res.dealSuccessFlag == true){
			pendingFrameErrorBarWidget.showMessage("${getHtmlText('ec.common.saveandclosesuccessful')}","s");
			CUI('#statisticsTime').val(res.statisticsTime);
			setTimeout(function(){
				ec.pending.queryList();
			},1500);
		}else{
			pendingFrameErrorBarWidget.showMessage("${getHtmlText('ec.common.unsuccessfully')}",'f');
			
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
	ec.pending.intitPt=function(){
		if(ec.pending.queryType=='user'){
			CUI("#pendingListTable_dcSpan").text("${getText('ec.pending.staffclass')}");
			CUI("#pendingListTable_dcSpan").attr("title","${getText('ec.pending.staffclass')}");
			
		}else if(ec.pending.queryType=='flow'){
			CUI("#pendingListTable_dcSpan").text("${getText('ec.pending.flowclass')}");
			CUI("#pendingListTable_dcSpan").attr("title","${getText('ec.pending.flowclass')}");
		}else{
			CUI("#pendingListTable_dcSpan").text("${getText('ec.pending.noclass')}");
			CUI("#pendingListTable_dcSpan").attr("title","${getText('ec.pending.noclass')}");
		}
		CUI("#pendingListTable_dcSpan").addClass('dc-text'); 
	}
	//催办
	ec.pending.remind=function(){
		//var selectedRowsObj=pendingListTableWidget.getEditData();
		var selectedRowsObj=pendingListTableWidget.selectedRows;
		if(selectedRowsObj.length==0){
			pendingFrameErrorBarWidget.showMessage("${getHtmlText('ec.expectedConsign.noSelected')}","f");
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
})();
</script>
<#if (Parameters.openType)?default('page') != 'dialog'>
</body>
</html>
</#if>