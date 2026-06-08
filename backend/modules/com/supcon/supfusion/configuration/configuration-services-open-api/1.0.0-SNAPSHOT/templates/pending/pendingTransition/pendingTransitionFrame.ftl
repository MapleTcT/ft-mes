<#if (Parameters.openType)?default('page') != 'dialog'>
<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>${getText('foundation.workflow.pendingTransition')}</title>
<@maincss/>
<@mainjs/>
</head>
<body>
<@loadpanel></@loadpanel>
<@errorbar id="workbenchErrorBar" offsetY=0 />
</#if>
<style type="text/css">
.ewc-dialog-el{height:100%;}
.elm-layout-doc-in-wrap .elm-layout-wrap-in-west{border-right:0px;}
.euc-layout-resize{border-top:1px solid #0269A3;border-bottom:1px solid #0269A3;}
</style>
<@errorbar id="pendingTransitionDialogErrorBar"></@errorbar>
<@frameset id="pendingTransitionManage" border="0">
   <@frame id="pendingTransitionMain" region="center" offsetH=4>
        <div id="pendingTransitionQuickQueryDiv">
	       <form id="pendingTransitionQueryQueryForm"  name="pendingTransitionQueryQueryForm" onsubmit="return false;">
	         <@quickquery formId="pendingTransitionQueryQueryForm" isExpandAll=true datatableId="pendingTableList"  onclick="ec.pending.pendingTransition.queryForm()"
	         			  fieldcodes="flowName:ec.pending.flowName||tableName:ec.workflow.pendingTransition.tableName||tableNo:ec.workflow.pendingTransition.tableNO||staffname:ec.workflow.pendingTransition.creator||ownerName:ec.workflow.modifyOwner.ownerName||departmentname:ec.workflow.modifyOwner.ownerDepartment||positionname:ec.workflow.modifyOwner.ownerPosition||summary:ec.property.summary" unique="LAST_QUERY_ec_pendingTransition_pendingTransitionFrame">
					     <@queryfield formId="pendingTransitionQueryQueryForm" code="flowName" isCustomize=true>
					     	<div class="fix-input">
					     		<input type="text" name="flowName" id="flowName"  class="cui-noborder-input" />
					     	</div>
					     </@queryfield>
					     <@queryfield formId="pendingTransitionQueryQueryForm" code="tableName" isCustomize=true>
					    	 <div class="fix-input">
					    	 	<input id="tableName" name="tableName" class="cui-noborder-input" style="margin-left:0px;" type="text"/>
					    	 </div>
					     </@queryfield>
					     <@queryfield formId="pendingTransitionQueryQueryForm" code="tableNo" isCustomize=true>
					     <div class="fix-input">
					     	<input id="tableNo" name="tableNo" class="cui-noborder-input" style="margin-left:0px;" type="text"/>
					     </div>
					     </@queryfield>
					     <#-- 制单人 -->
					     <@queryfield formId="pendingTransitionQueryQueryForm" code="staffname"  isCustomize=true>
					     	 <input type="hidden" id="creatorId" name="creator.id" />
					     	 <@mneclient iframe=true name="creator.name" id ="creator_name" onkeyupfuncname="ec.pending.pendingTransition.staffInfoCallback(obj)" formId="pendingTransitionQueryQueryForm" classStyle="cui-noborder-input" id="is_staff_name" url="other" cssStyle="padding-left: 0px; width: 95%;" type="Staff"  mnewidth=260 multiple=false clicked=true isPrecise=true searchClick="ec.pending.pendingTransition.queryForm()"/>
						 </@queryfield>
					     <#-- 负责人、负责人部门、负责人岗位 -->
					    <@queryfield formId="pendingTransitionQueryQueryForm" code="ownerName" isCustomize=true>
			     			<input type="hidden" id="ownerId" name="owner.id" />
              	 			<@mneclient iframe=true name="owner.name" id="owner_name" formId="pendingTransitionQueryQueryForm"  onkeyupfuncname="ec.pending.pendingTransition.ownerInfoCallback(obj)" url="/msService/ec/foundation/staff/common/staffListFrameset" classStyle="cui-noborder-input" type="Staff" multiple=false clicked=true isPrecise=true searchClick="ec.pending.pendingTransition.queryForm()"/>
			     		</@queryfield>
			     		<@queryfield formId="pendingTransitionQueryQueryForm" code="departmentname"  isCustomize=true>
			     	  		<@mneclient iframe=true name="department.name" id="owner_department_name" onkeyupfuncname="ec.pending.pendingTransition.departmentInfoCallback(obj)" formId="pendingTransitionQueryQueryForm" url="/msService/ec/foundation/department/common/departmentListFrameset" classStyle="cui-noborder-input" type="Department" multiple=false clicked=true isPrecise=true searchClick="ec.pending.pendingTransition.queryForm()"/>
				 	  		<input id="ownerDepartmentId" name="department.id" type="hidden"/>
				 		</@queryfield>
			     		<@queryfield formId="pendingTransitionQueryQueryForm" code="positionname"  isCustomize=true>
			     	 		<@mneclient iframe=true name="position.name" id="owner_position_name" onkeyupfuncname="ec.pending.pendingTransition.positionInfoCallback(obj)" formId="pendingTransitionQueryQueryForm" url="/msService/ec/foundation/position/common/positionListFrameset" classStyle="cui-noborder-input" type="Position" multiple=false clicked=true isPrecise=true searchClick="ec.pending.pendingTransition.queryForm()"/>
				  			<input id="ownerPositionId" name="position.id" type="hidden"/>
				 		</@queryfield>
					     <#-- 摘要 -->
					     <@queryfield formId="pendingTransitionQueryQueryForm" code="summary" isCustomize=true>
					     	<div class="fix-input">
					     		<input id="summary" class="cui-noborder-input" style="margin-left:0px;" type="text"/>
					     	</div>
					     </@queryfield>
					     <div class="quick-query-buttonbar" style="margin-bottom:6px;margin-right:12px;">
						     <@querybutton formId="pendingTransitionQueryQueryForm" type="adv"  onclick="ec.pending.pendingTransition.queryForm()" onadvancedclick="ec.pending.pendingTransition.advQuery()" />
					         <@querybutton formId="pendingTransitionQueryQueryForm" type="clear" />
						</div>
				</@quickquery>
	       </form>
	   </div>
	   <div id="pendingTransitionAdvQueryDiv" style="display:none;">
	   		<form id="pendingTransitionAdvQueryForm"  name="pendingTransitionAdvQueryForm" onsubmit="return false;">
	   			<table  width="94%" class="cui-fd-infotable">
		            <tr>
		                <td width="10%" class="lab">${getHtmlText('ec.workflow.pendingTransition.flowName')}</td>
		                <td width="20%">
		            		<div class="fix-input">
		            			<input id="flowName" name="flowName" class="cui-noborder-input" style="margin-left:0px;" type="text"/>
		            		</div>
		            	</td>
		                <td width="10%" class="lab">${getHtmlText('ec.workflow.pendingTransition.tableName')}</td>
		                <td width="20%">
		                	<div class="fix-input">
		                		<input id="tableName" name="tableName" class="cui-noborder-input" style="margin-left:0px;" type="text"/>
		                	</div>
		                </td>
		            </tr>
		            <tr>
		            	<td width="10%" class="lab">${getHtmlText('ec.workflow.pendingTransition.tableNO')}</td>
		            	<td width="20%">
		            		<div class="fix-input">
		            			<input id="tableNo" name="tableNo" class="cui-noborder-input" style="margin-left:0px;" type="text"/>
		            		</div>
		            	</td>
		            	<td width="10%" class="lab">${getHtmlText('ec.workflow.pendingTransition.creator')}</td>
						<td width="20%">
							<input type="hidden" id="creatorId" name="creator.id" />
		                	<@mneclient name="creator.name" id="creator_name" formId="pendingTransitionAdvQueryForm"  onkeyupfuncname="ec.pending.pendingTransition.staffInfoCallback1(obj)" url="/msService/ec/foundation/staff/common/staffListFrameset" classStyle="cui-noborder-input" type="Staff" multiple=false clicked=true isPrecise=true/>
						</td>
		            </tr>
		            <tr>
               			<td width="10%" style="text-align:right;padding-right:3px;">${getHtmlText('ec.workflow.modifyOwner.ownerName')}</td>
               			<td width="20%">
               				<input type="hidden" id="ownerId" name="owner.id" />
               				<@mneclient name="owner.name" id="owner_name" formId="pendingTransitionAdvQueryForm"  onkeyupfuncname="ec.pending.pendingTransition.ownerInfoCallback1(obj)" url="/msService/ec/foundation/staff/common/staffListFrameset" classStyle="cui-noborder-input" type="Staff" multiple=false clicked=true isPrecise=true/>
               			</td>
	           			<td width="10%" style="text-align:right;padding-right:3px;">${getHtmlText('ec.workflow.modifyOwner.ownerDepartment')}</td>
	            			<td width="20%">
	             			<@mneclient name="adv.department.name" formId="pendingTransitionAdvQueryForm" id="owner_department_name" onkeyupfuncname="ec.pending.pendingTransition.departmentInfoCallback1(obj)"  url="/msService/ec/foundation/department/common/departmentListFrameset" classStyle="cui-noborder-input" type="Department" multiple=false clicked=true isPrecise=true/>
				 			<input id="ownerDepartmentId" name="adv.department.id" type="hidden"/>
						</td>
            		</tr>
					<tr>
						<td width="10%" style="text-align:right;padding-right:3px;">${getHtmlText('ec.workflow.modifyOwner.ownerPosition')}</td>
						<td width="20%">
							<@mneclient name="adv.position.name" id="owner_position_name" onkeyupfuncname="ec.pending.pendingTransition.positionInfoCallback1(obj)" formId="pendingTransitionAdvQueryForm" url="/msService/ec/foundation/position/common/positionListFrameset" classStyle="cui-noborder-input" type="Position" multiple=false clicked=true isPrecise=true/>
							<input id="ownerPositionId" name="adv.position.id" type="hidden"/>
						</td>
						<td width="10%" class="lab">${getHtmlText('ec.property.summary')}</td>
						<td width="20%">
							<div class="fix-input">
								<input type="text" name="summary" id="summary" class="cui-noborder-input" style="margin-left:0px;">
							</div>
						</td>
					</tr>
	          </table>
	   		</form> 
	   </div>
      <@datatable formId="pendingTransition_queryTableForm" exportExcel=false id="pendingTableList" hidekey="['ID','PKEY','VERSION','ACTIVITYNAME','STATUS']" dtPage="records" transMethod="post" style="margin:6px 4px 2px 13px;">
            <@operatebar operates="code:pendingTransitionWorkbench_edit||iconcls:goto||name:${getHtmlText('foundation.workflow.pendingTransition.transition')}||onclick:ec.pending.pendingTransition.transition();
            					   code:flowWorkbench_hangUp||iconcls:pause||name:${getHtmlText('foundation.workflow.flow.hangUp')}||onclick:ec.pending.pendingTransition.hangUp();
            					   code:flowWorkbench_restore||iconcls:recover||name:${getHtmlText('foundation.workflow.flow.end')}||onclick:ec.pending.pendingTransition.restore();
            					   code:modifyOwnerWorkbench_edit||iconcls:edit||name:${getHtmlText('foundation.workflow.modifyOwner.operate')}||onclick:ec.pending.pendingTransition.modify();
            					   code:modifyOwnerWorkbench_del||iconcls:nullify||name:${getHtmlText('foundation.workflow.modifyOwner.delete')}||onclick:ec.pending.pendingTransition.deleteMethod();
            					   code:flowWorkbench_addPending||iconcls:edit||name:${getHtmlText('foundation.workflow.flow.addPending')}||onclick:ec.pending.pendingTransition.addPending()" resultType="json"/>
			<@datacolumn key="checkbox" type="checkbox" checkall="true" textalign="center" label="" width="24" />
			<@datacolumn key="TABLENO" label="${getHtmlText('ec.workflow.modifyOwner.tableNo')}" width="180" textalign="left"/>
			<@datacolumn key="SUMMARY" label="${getHtmlText('ec.property.summary')}" width="180" textalign="left"/>
			<@datacolumn key="TABLENAME" label="${getHtmlText('ec.workflow.modifyOwner.tableName')}" width="100" textalign="left"/>
			<@datacolumn key="FLOWNAME" label="${getHtmlText('ec.workflow.pendingTransition.flowName')}" width="100" textalign="left"/>
			<@datacolumn key="PENDINGOWNER" label="${getHtmlText('ec.workflow.pendingTransition.pendingOwner')}" width="200" textalign="left"/>
			<@datacolumn key="STATE" label="${getHtmlText('ec.workflow.modifyOwner.tableState')}" width="100" textalign="left"/>
			<@datacolumn key="OWNER" label="${getHtmlText('ec.workflow.modifyOwner.ownerName')}" width="100" textalign="left"/>
			<@datacolumn key="DEPARTMENT" label="${getHtmlText('ec.workflow.modifyOwner.ownerDepartment')}" width="100" textalign="left"/>
			<@datacolumn key="POSITION" label="${getHtmlText('ec.workflow.modifyOwner.ownerPosition')}" width="100" textalign="left"/>
			<@datacolumn key="CREATOR" label="${getHtmlText('ec.workflow.modifyOwner.tableCreator')}" width="100" textalign="left"/>
			<@datacolumn key="DEPARTMENT_CREATOR" label="${getHtmlText('ec.workflow.modifyOwner.tableCreatorDep')}" width="100" textalign="left"/>
			<@datacolumn key="POSITION_CREATOR" label="${getHtmlText('ec.workflow.modifyOwner.tableCreatorPos')}" width="100" textalign="left"/>
			<@datacolumn key="CREATETIME" label="${getHtmlText('ec.workflow.modifyOwner.tableCreatorTime')}" width="130" type="datetime" textalign="center"/>
	</@datatable>
   </@frame>
</@frameset>
<div id="ec_tableInfo_modifyOwner_edit_div" style="display:none;">
    <@s.form id="modify_Owner_Edit_Form" action="save"  namespace="/msService/ec/tableInfo/modifyOwner/modify"  toggle_all_button="true" callback="ec.pending.pendingTransition.callback">
    	<@s.hidden id="tableIds" name="tableIds"/>
		<table class="infoTable" id="editTable" cellpadding="0" cellspacing="0" border="0" align="center" width="90%" style="margin-top: 20px">
			<tr>
			    <td style="width: 20%;" class="lab">${getHtmlText('ec.workflow.modifyOwner.original')}</td>
			    <td style="width: 70%">
			        <div class="fix-input"><input id="originalPerson"  name="originalPerson" class="cui-noborder-input" style="margin-left:0px;" type="text" readonly/></div>
			    </td>
			</tr>
	        <tr>
	            <td style="width: 20%" class="lab cui-lmust">${getHtmlText('ec.workflow.modifyOwner.new')}</td>
	            <td style="width: 70%">
	            	<input type="hidden" id="staffId" name="staffId"/>
	            	<input type="hidden" id="ownerId" name="owner.staff.id"/>
                	<@mneclient name="owner.staff.name" isCrossCompany=true id="staff_name1" formId="modify_Owner_Edit_Form" onkeyupfuncname="ec.pending.pendingTransition.ownerInfoCallback_edit(obj)" url="/msService/ec/foundation/staff/common/staffListFrameset" classStyle="cui-noborder-input" type="Staff" multiple=false clicked=true isEdit=true editCustomCallback="ec.pending.pendingTransition.ownerInfoCallback_edit(obj)" />
	            </td>
	        </tr>
	        <tr>
	        <td style="width: 20%;" class="lab">${getHtmlText('ec.workflow.modifyOwner.new.position')}</td>
	        <td style="width: 70%">
            	<select id="newPositionId" name="positionId" style="width:100%;border-color:#bcbcbc" disabled="disabled" class="cui-edit-field">
             		<option></option>
             	</select>
			</td>
	        </tr>
            <tr> 
				<td width="10%" class="lab cui-lmust v-align">${getHtmlText('ec.workflow.modifyOwner.modifyReason')}</td>
				<td width="20%" class="cui-vte">
			    	<div class="fix-input"><textarea id="modifyReason" name="modifyReason" class="cui-noborder-textarea"></textarea></div>
			    </td>
        	</tr>
		</table>
	</@s.form>
</div>

<script type="text/javascript">
CUI.ns("ec.pending.pendingTransition");
	$(function(){
		  $(".btn-act").hover(
			  function () {
				$(this).addClass("edit-btn-hover");
			  },function () {
				$(this).removeClass("edit-btn-hover");
		  });
		  $(".btn-act").mousedown(
			  function () {
				$(this).addClass("edit-btn-click");
		  });
		  $(".btn-act").mouseup(
			  function () {
				$(this).removeClass("edit-btn-click");
		  });
	})
	ec.pending.pendingTransition.operateType='quickQuery';
	ec.pending.pendingTransition.staffInfoCallback=function(obj){
		if(obj!=null&&obj!=undefined&&obj[0].id!=undefined&&obj[0].id!=null){
			CUI("#creatorId",CUI("#pendingTransitionQueryQueryForm")).val(obj[0].id);
		}
	}
	ec.pending.pendingTransition.staffInfoCallback1=function(obj){
		if(obj!=null&&obj!=undefined&&obj[0].id!=undefined&&obj[0].id!=null){
			CUI("#creatorId",CUI("#pendingTransitionAdvQueryForm")).val(obj[0].id);
		}
	}
	ec.pending.pendingTransition.checkNull=function(formId,type){
	      var obj=CUI("#"+formId);
		  var flag=0;
		  CUI("input,select",obj).each(function(){
		       if(CUI.trim(CUI(this).val())!=""){
		          flag=1;
		          return false;
		       }
		   });
		   if(flag==0){
		   		if(type!='advQuery'){
	    	   		workbenchErrorBarWidget.showMessage("${getHtmlText('foundation.permissionQuery.pleaseInputQueryCondition')}","f");
		   	 	}else{
		   	 		pendingTransitionAdvQueryFormDialogErrorBarWidget.showMessage("${getHtmlText('foundation.permissionQuery.pleaseInputQueryCondition')}","f");
		   	 	}
		     	return false;
		   }else{
		     	return true;
		   }
   }
	ec.pending.pendingTransition.advQuery=function(){
	     ec.pending.pendingTransition.operateType='advQuery';
	     CUI(function(){
         ec.pending.pendingTransition.advQueryDialog=new CUI.Dialog({
            title:"<@s.text name='ec.tableInfo.modifyOwner.advQuery'/>",
            elementId:"pendingTransitionAdvQueryDiv",
            formId:"pendingTransitionAdvQueryForm",
			modal:true,
			width: 522,
			height: 455,
	        dragable:true,
	         buttons:[{name:"<@s.text name='foundation.common.query'/>",
	                handler:"ec.pending.pendingTransition.advQueryForm()"},
	                {name:"<@s.text name='common.button.clear'/>",
	                 handler:function(){CUI.resetForm('pendingTransitionAdvQueryForm')}
	                },
	               {name:"<@s.text name='common.button.cancel'/>",
	                handler:function(){this.close()}
	               }]
	      
             });
           ec.pending.pendingTransition.advQueryDialog.show();
      })
	}
	ec.pending.pendingTransition.advQueryForm=function(type,pageNo){
         if(ec.pending.pendingTransition.checkNull('pendingTransitionAdvQueryForm','advQuery')){
	        pendingTableListWidget.setAttributeConfig('queryFunc',{
	          writeOnce: true,
	          value:"ec.pending.pendingTransition.advQueryForm(1)"
	        });
		    var url="/msService/ec/tableInfo/modifyOwner/search";
		    var dataPost=ec.pending.pendingTransition.getPostData('pendingTransitionAdvQueryForm');
		    if(pageNo!=undefined){
		        dataPost+="&records.pageNo="+pageNo;
		    }
		    dataPost+="&creator="+encodeURIComponent(CUI.trim(CUI("input[name='adv.staff.name']",CUI("#pendingTransitionAdvQueryForm")).val()));
		    pendingTableListWidget.setRequestDataUrl(url,dataPost);
		    if(ec.pending.pendingTransition.advQueryDialog.isShow!=-1){
		       ec.pending.pendingTransition.advQueryDialog.close();
		    }
        }
	}
	ec.pending.pendingTransition.queryForm=function(type,pageNo){
		if(ec.pending.pendingTransition.checkNull('pendingTransitionQueryQueryForm','quickQuery')){
		   ec.pending.pendingTransition.operateType='quickQuery';
		   pendingTableListWidget.setAttributeConfig('queryFunc',{
	          writeOnce: true,
	          value:"ec.pending.pendingTransition.queryForm(1)"
	       });
		   var url="/msService/ec/tableInfo/modifyOwner/search";
		   var dataPost=ec.pending.pendingTransition.getPostData('pendingTransitionQueryQueryForm');
		   if(pageNo!=undefined){
		        dataPost+="&records.pageNo="+pageNo;
		   }
		   dataPost+="&creator="+encodeURIComponent(CUI.trim(CUI("input[name='staff.name']",CUI("#pendingTransitionQueryQueryForm")).val()));
		   dataPost+="&"+pendingTransitionQueryQueryForm_getCookieParam();
		   pendingTableListWidget.setRequestDataUrl(url,dataPost);
		}
	}
	ec.pending.pendingTransition.getPostData = function(formId){
	    var obj = CUI("#"+formId);
	    var dataPost = '';
	    var dataPost = "flowName=" + encodeURIComponent(CUI.trim(CUI("#flowName", obj).val()));
	    dataPost += "&tableNo=" + encodeURIComponent(CUI.trim(CUI("#tableNo", obj).val()));
	    dataPost += "&tableName=" + encodeURIComponent(CUI.trim(CUI("#tableName", obj).val()));
	    dataPost += "&creatorId=" + encodeURIComponent(CUI.trim(CUI("#creatorId", obj).val()));
	    dataPost += "&creatorName=" + encodeURIComponent(CUI.trim(CUI("#creator_name", obj).val()));
	    dataPost += "&ownerId=" + encodeURIComponent(CUI.trim(CUI("#ownerId", obj).val()));
	    dataPost += "&ownerName=" + encodeURIComponent(CUI.trim($("#owner_name", obj).val()));
	    dataPost += "&ownerDepartmentId=" + encodeURIComponent(CUI.trim($("#ownerDepartmentId", obj).val()));
	    dataPost += "&ownerDepartmentName=" + encodeURIComponent(CUI.trim($("#owner_position_name", obj).val()));
	    dataPost += "&ownerPositionId=" + encodeURIComponent(CUI.trim($("#ownerPositionId", obj).val()));
	    dataPost += "&ownerPositionName=" + encodeURIComponent(CUI.trim($("#owner_department_name", obj).val()));
	    dataPost += '&summary=' + encodeURIComponent(CUI.trim($('#summary', obj).val()));
	       
	    var pageSize = CUI('input[name="pendingTableList_PageLink_PageCount"]').val();
	    dataPost += "&pageSize=" + encodeURIComponent(pageSize);	
	    return dataPost;
	}
	
	ec.pending.pendingTransition.transition = function(){
	   if(ec.pending.pendingTransition.selectedAny()){
		    var rows = pendingTableListWidget.selectedRows;
		    if (rows.length > 1) {
		    	workbenchErrorBarWidget.showMessage("${getHtmlText('ec.pendingTransition.checkTransit')}", "f");
		    	return;
		    }
		    if (rows[0].STATUS != 88) {
				workbenchErrorBarWidget.showMessage("${getHtmlText('ec.pendingTransition.checkStatus')}", "f");
				return;
			}
			var checkPowerUrl = "/foundation/userPermission/checkUserPower?menuOperateCodes=pendingTransitionWorkbench_edit";
			CUI.ajax({
		   		   type : "POST",
		   		   url : checkPowerUrl,
		   		   data : "",
		   		   success : function(res){
		   		       var processKey = (rows[0].PKEY == undefined ? '' : rows[0].PKEY);
				       var tableId = rows[0].ID;
				       var state = (rows[0].STATE == undefined ? '' : rows[0].STATE);
				       var originalActivityName = (rows[0].ACTIVITYNAME == undefined ? '' : rows[0].ACTIVITYNAME);
				       var processVersion = rows[0].VERSION == undefined ? '' : rows[0].VERSION;
				       var flowName = (rows[0].FLOWNAME == undefined ? '' : rows[0].FLOWNAME);
				       var url = "/msService/ec/pending/pendingTransition/pendingTransitionEdit?openType=frame&tableId=" + tableId + "&activeName=" + encodeURIComponent(state) + "&processKey=" + processKey + "&originalActivityName=" + encodeURIComponent(originalActivityName) + "&processVersion=" + processVersion + "&flowName=" + encodeURIComponent(flowName);
				       if(  !ec.pending.pendingTransition.transitionDialog ){
			               ec.pending.pendingTransition.transitionDialog = new CUI.Dialog({
			                  title:"<@s.text name='ec.workflow.pendingTransition.edit'/>",
			                  url:url,
			                  modal:true,
			                  
			                  iframe: 'ec_pending_pendingTransition_transitionDialog_iframe',
			                  
						      type:3,
						      dragable:true,
						      buttons:[{name:"<@s.text name='common.button.save'/>",
						                handler:function(){
											ec_pending_pendingTransition_transitionDialog_iframe.ec_pending_pendingTransition_transitionSave();
										}},
						               {name:"<@s.text name='common.button.cancel'/>",
						                handler:function(){this.close();}}]
						      
			               });
			           }else{
							ec.pending.pendingTransition.transitionDialog._config.url = url;
					   }
			           ec.pending.pendingTransition.transitionDialog.show();
					
				   }
			});
	   }
	}
	
	ec.pending.pendingTransition.end=function(){
		 if(ec.pending.pendingTransition.selectedAny()){
			 var tableId=pendingTableListWidget.selectedRows[0].ID;
			CUI.Dialog.confirm("${getHtmlText('foundation.workflow.flow.checkEndFlow')}", function(){
					CUI.post('/msService/ec/pending/pendingTransition/end?tableId='+tableId,null,function(msg){
					if(msg.dealSuccessFlag == true){
						workbenchErrorBarWidget.showMessage("${getHtmlText('foundation.workbench.mainFrame.dealSuccessed')}","s");
						setTimeout(function(){	
							if(ec.pending.pendingTransition.operateType=='quickQuery'){
						      ec.pending.pendingTransition.queryForm();
						    }
						    else{
						      ec.pending.pendingTransition.advQueryForm();
						    }
						},1500);
					}else{
						workbenchErrorBarWidget.showMessage("${getHtmlText('foundation.staff.frame.Processing_failure')}","f");			
					}
				});
			});
		}
	}
	
	ec.pending.pendingTransition.hangUp=function(){
		 if(ec.pending.pendingTransition.selectedAny()){
		 		var length = pendingTableListWidget.selectedRows.length;
		 		if(length>1){
		 			workbenchErrorBarWidget.showMessage("${getHtmlText('ec.business.view.choose.only')}","f");			
		 			return ;
		 		}
		 		var status=pendingTableListWidget.selectedRows[0].STATUS;
		 		if(status==77){
		 			workbenchErrorBarWidget.showMessage("${getHtmlText('foundation.pending.isExistHangUp')}","f");			
		 			return ;
		 		}
		 		if (status == 0) {
		 			workbenchErrorBarWidget.showMessage("${getHtmlText('foundation.pending.isInvalid')}", "f");
		 			return;
		 		}
		 		if (status == 99) {
		 			workbenchErrorBarWidget.showMessage("${getHtmlText('foundation.pending.isEffective')}", "f");
					return;		 			
		 		}
				var tableId=pendingTableListWidget.selectedRows[0].ID;
				CUI.Dialog.confirm("${getHtmlText('foundation.workflow.flow.checkHangUp')}", function(){
						CUI.post('/msService/ec/pending/pendingTransition/hangUp?tableId='+tableId,null,function(msg){
						if(msg.dealSuccessFlag== true){
							workbenchErrorBarWidget.showMessage("${getHtmlText('foundation.workbench.mainFrame.dealSuccessed')}","s");
							setTimeout(function(){	
								if(ec.pending.pendingTransition.operateType=='quickQuery'){
							      ec.pending.pendingTransition.queryForm();
							    }
							    else{
							      ec.pending.pendingTransition.advQueryForm();
							    }
							},1500);
						}else{
							workbenchErrorBarWidget.showMessage("${getHtmlText('foundation.staff.frame.Processing_failure')}","f");			
						}
					});
				});
			}
	
	}
	ec.pending.pendingTransition.restore=function(){
		 if(ec.pending.pendingTransition.selectedAny()){
		 		 var length = pendingTableListWidget.selectedRows.length;
		 		 if(length>1){
		 			 workbenchErrorBarWidget.showMessage("${getHtmlText('ec.business.view.choose.only')}","f");			
		 			 return ;
		 		 }
				 var tableId=pendingTableListWidget.selectedRows[0].ID;
				 var status=pendingTableListWidget.selectedRows[0].STATUS;
				 if(status!=77){
		 			workbenchErrorBarWidget.showMessage("${getHtmlText('foundation.pending.isExistRestore')}","f");			
		 			return ;
		 		}
				CUI.Dialog.confirm("${getHtmlText('foundation.workflow.flow.checkRestore')}", function(){
						CUI.post('/msService/ec/pending/pendingTransition/restore?tableId='+tableId,null,function(msg){
						if(msg.dealSuccessFlag == true){
							workbenchErrorBarWidget.showMessage("${getHtmlText('foundation.workbench.mainFrame.dealSuccessed')}","s");
							setTimeout(function(){	
								if(ec.pending.pendingTransition.operateType=='quickQuery'){
							      ec.pending.pendingTransition.queryForm();
							    }
							    else{
							      ec.pending.pendingTransition.advQueryForm();
							    }
							},1500);
						}else{
							workbenchErrorBarWidget.showMessage("${getHtmlText('foundation.staff.frame.Processing_failure')}","f");			
						}
					});
				});
			}
	
	}
	
	ec.pending.pendingTransition.selectedAny = function(){
		if(pendingTableListWidget.selectedRows.length == 0){
		  	workbenchErrorBarWidget.showMessage("${getHtmlText('ec.common.checkselected')}","f");
			return false;
		}else{
			 var processKey = pendingTableListWidget.selectedRows[0].PKEY;
			 if(processKey == undefined){
			 	workbenchErrorBarWidget.showMessage("${getHtmlText('com.supcon.orchid.container.exceptions.PROCESS_IS_NOT_EXIST')}","f");
				return false;
			}
			var status = pendingTableListWidget.selectedRows[0].STATUS;
			 if(status == 99){
			 	workbenchErrorBarWidget.showMessage("${getHtmlText('com.supcon.orchid.container.exceptions.PROCESS_IS_EXECUTED')}","f");
				return false;
			}
		}
  		return true;
	}
	
	ec.pending.pendingTransition.transitionSave=function(){
		var errorMessage=new Array();
   	  	var errorFields = new Array();
   	    var error=false;
   	    var form=CUI("#pending_Transition_Edit_Form");
   	    var errorBarWidget=pending_Transition_Edit_FormDialogErrorBarWidget;
   	    <#-- 
	    if(CUI.trim(CUI("#pending_Transition_Edit_Form #flowId").val())==''){
	       CUI("#pending_Transition_Edit_Form #flowId").css({'background':'#FCD6D6','border-color':'#db0000'});
           errorMessage.push("${getHtmlText('ec.workflow.pendingTransition.chooseFlowVersion')}");
	       return false;
	    }
   	    -->
	    if(CUI.trim(CUI("#pending_Transition_Edit_Form #staff_nameMultiIDs").val())==''){
	    	<#--
	        CUI("#pending_Transition_Edit_Form #staff_nameMultiIDsContainerDiv").css({'background':'#FCD6D6'});
            CUI("#pending_Transition_Edit_Form #staff_nameMultiIDsContainerDiv").focus();
            CUI("#pending_Transition_Edit_Form #staff_nameMneInput").css({'background':'#FCD6D6'});
            CUI("#pending_Transition_Edit_Form #staff_nameMneInput").focus();
	    	-->
            errorMessage.push("${getHtmlText('ec.workflow.pendingTransition.checkTrustee')}");
            errorFields.push(CUI("#pending_Transition_Edit_Form #staff_nameMultiIDsContainerDiv"));
            errorFields.push(CUI("#pending_Transition_Edit_Form #staff_nameMneInput"));
	        error=true;
	    }
	    if(CUI.trim(CUI("#pending_Transition_Edit_Form #staff_nameMultiIDs").val())!=''){
	    	<#--
	        CUI("#pending_Transition_Edit_Form #staff_nameMultiIDsContainerDiv").css({'background':'#FFFFFF'});
            CUI("#pending_Transition_Edit_Form #staff_nameMneInput").css({'background':'#FFFFFF'});
	    	-->
            removeErrorField(CUI("#pending_Transition_Edit_Form #staff_nameMultiIDsContainerDiv"));
            removeErrorField(CUI("#pending_Transition_Edit_Form #staff_nameMneInput"));
	    }
	    if(CUI.trim(CUI("#modifyReason",CUI("#pending_Transition_Edit_Form")).val())==''){
	    	<#--
	    	CUI("#modifyReason",CUI("#pending_Transition_Edit_Form")).css({'background':'#FCD6D6'});
	    	CUI("#modifyReason",CUI("#pending_Transition_Edit_Form")).focus();
	    	-->
	    	 errorMessage.push("${getHtmlText('ec.workflow.pendingTransition.modifyReasonNull')}");
	    	 errorFields.push(CUI("#modifyReason",CUI("#pending_Transition_Edit_Form")));
	    	 error=true;
	    }
	    if(CUI.trim(CUI("#activityId",CUI("#pending_Transition_Edit_Form")).val())==''){
	    	 errorMessage.push("${getHtmlText('ec.workflow.pendingTransition.newActitiyIsNull')}");
	    	 errorFields.push(CUI("*[id^='activeName__']",CUI("#pending_Transition_Edit_Form"))[0]);
	    	 error=true;
	    }
	    if(error){
	    	addError(form,errorBarWidget,null,errorMessage);
	    	CUI.each(errorFields,function(){
	    		showErrorField(CUI(this));
	    	})
	    	return false;
	    }
	    CUI("#pending_Transition_Edit_Form #staffMultiIDs").val(CUI("#pending_Transition_Edit_Form #staff_nameMultiIDs").val());
	    CUI("#pending_Transition_Edit_Form").submit();
	}
	ec.pending.pendingTransition.transitionSaveCallback=function(res){
	   if(res.dealSuccessFlag){
	     ec_pending_pendingTransition_transitionDialog_iframe.pending_Transition_Edit_FormDialogErrorBarWidget.show("${getHtmlText('ec.workflow.pendingTransition.transitionSuccess')}","s");
         setTimeout(function(){
			try{
			    ec.pending.pendingTransition.transitionDialog.close();
			}
			catch(e){
			
			}
	    },500);
	    if(ec.pending.pendingTransition.operateType=='quickQuery'){
	      ec.pending.pendingTransition.queryForm();
	    }
	    else{
	      ec.pending.pendingTransition.advQueryForm();
	    }
	   }
	}
	<#--
	ec.pending.pendingTransition.getActivity=function(){
	  var processDefinitionId=CUI("#pending_Transition_Edit_Form #flowId").val();
	  var url="/msService/ec/pending/pendingTransition/list-activity?processDefinitionId="+processDefinitionId;
	  if(processDefinitionId!=''){
	     CUI.ajax({
	       type:'POST',
	       url:url,
	       sync:true,
	       success:function(res){
	         CUI("#pending_Transition_Edit_Form #activityId").removeOption();
	         if(res!=null){
	           CUI("#pending_Transition_Edit_Form #activityId").addOption('','');
	           for(var i=0;i<res.length;i++){
	           	  var originalPId=CUI("#pending_Transition_Edit_Form #originalPId").val();
	           	  if(processDefinitionId==originalPId){
	           	  	if((CUI("#pending_Transition_Edit_Form #originalActivityName").val()).indexOf(res[i].taskName)==-1){
	                  CUI("#pending_Transition_Edit_Form #activityId").addOption(res[i].taskDescription,res[i].taskName);
	              	}
	           	  }
	              else{
	              	 CUI("#pending_Transition_Edit_Form #activityId").addOption(res[i].taskDescription,res[i].taskName);
	              }
	           }
	           if(res.length<=1){
                     CUI("#pending_Transition_Edit_Form #activityId").attr('readonly','readonly');
                     CUI("#pending_Transition_Edit_Form #activityId").css({'border-color':'#bcbcbc'})
                }
                else{
                    CUI("#pending_Transition_Edit_Form #activityId").removeAttr('readonly'); 
                    CUI("#pending_Transition_Edit_Form #activityId").css({'border-color':'#a2c7ed'});
                }
	         }
	       }
	     });
	  }
	}
	-->
	ec.pending.pendingTransition.modify = function(){
		removeErrorField(CUI("#modify_Owner_Edit_Form input[name='owner.staff.name']"));
		removeErrorField(CUI("#modify_Owner_Edit_Form #modifyReason"));
		if(ec.pending.pendingTransition.checkSelectedAny()){
			CUI.resetForm("modify_Owner_Edit_Form");
			CUI("#newPositionId",CUI("#modify_Owner_Edit_Form")).empty();
			CUI("#newPositionId",CUI("#modify_Owner_Edit_Form")).attr('disabled','disabled');
			CUI("#newPositionId",CUI("#modify_Owner_Edit_Form")).css({'border-color':'#bcbcbc'});
			var parameters='';
			var owner='';
			var flag=false;
			var rows=pendingTableListWidget.selectedRows;
			for (var i = 0; i < rows.length; i++) {
				parameters += (parameters == '' ? rows[i].ID : ',' + rows[i].ID);
				if (owner.indexOf(rows[i].OWNER) == -1) {
					owner += (owner == '' ? rows[i].OWNER : "," + rows[i].OWNER);
					if (i > 0) {
						flag=true;
					}
				}
			}
			CUI("#tableIds",CUI("#modify_Owner_Edit_Form")).val(parameters);
			CUI("#originalPerson",CUI("#modify_Owner_Edit_Form")).val(owner);
			if(flag){
				CUI.Dialog.confirm("${getHtmlText('ec.workflow.modifyOwner.modifyDifferentOwner')}", function(){ec.pending.pendingTransition.showEditDialog()});
			}else{
				ec.pending.pendingTransition.showEditDialog();
			}
		}
	}
	
	ec.pending.pendingTransition.ownerInfoCallback_edit=function(obj){
   		if(obj!=null && obj != undefined && obj[0].id != undefined && obj[0].id != null){
	   		$("#staffId",CUI("#modify_Owner_Edit_Form")).val(obj[0].id);
	   		$("#ownerId",CUI("#modify_Owner_Edit_Form")).val(obj[0].id);
	   		CUI.ajax({
	          type:"POST",
	          url:"/foundation/staff/getAllPosition?staffId="+obj[0].id,
	          sync:true,
	          success:function(res){
	             if(res != null){
	                CUI("#newPositionId",CUI("#modify_Owner_Edit_Form")).empty();
	                for(var i=0; i<res.length; i++){
	                   $("#newPositionId",CUI("#modify_Owner_Edit_Form")).append("<option value='"+res[i].id+"'>"+res[i].name+"</option>");
	                }
	                if(res.length < 1){
	                     CUI("#newPositionId",CUI("#modify_Owner_Edit_Form")).attr('disabled','disabled');
	                     CUI("#newPositionId",CUI("#modify_Owner_Edit_Form")).css({'border-color':'#bcbcbc'})
	                }else{
	                    CUI("#newPositionId",CUI("#modify_Owner_Edit_Form")).removeAttr('disabled'); 
	                    CUI("#newPositionId",CUI("#modify_Owner_Edit_Form")).css({'border-color':'#a2c7ed'});
	                }
	             }
	          }
	       });
   		}
	}
	
	 ec.pending.pendingTransition.save=function(){
	   	  var errorMessage = new Array();
	   	  var errorFields = new Array();
	   	  var error=false;
	   	  var form = CUI("#modify_Owner_Edit_Form");
	   	  var errorBarWidget = modify_Owner_Edit_FormDialogErrorBarWidget;
	      if(CUI.trim(CUI("#ownerId",CUI("#modify_Owner_Edit_Form")).val())==''){
	         errorFields.push(CUI("#modify_Owner_Edit_Form input[name='owner.staff.name']"));
	         errorMessage.push("${getHtmlText('ec.workflow.modifyOwner.new.notNull')}");
	         error=true;
	      }
	      if(CUI.trim(CUI("#ownerId",CUI("#modify_Owner_Edit_Form")).val())!=''){
	         removeErrorField(CUI("#modify_Owner_Edit_Form input[name='owner.staff.name']"));
	      }
	      if(CUI.trim(CUI("#modify_Owner_Edit_Form #modifyReason").val())==''){
	         errorFields.push(CUI("#modify_Owner_Edit_Form #modifyReason"));
	         errorMessage.push("${getHtmlText('ec.workflow.modifyOwner.modifyReason.isNull')}");
	         error=true;
	      }
	      if(CUI.trim(CUI("#modify_Owner_Edit_Form #modifyReason").val())!=''){
	         removeErrorField(CUI("#modify_Owner_Edit_Form #modifyReason"));
	      }
	      if(error){
	      		addError(form,errorBarWidget,null,errorMessage);
	      		CUI.each(errorFields,function(){
	      			showErrorField(CUI(this));
	      		})
	      		return false;
	      }
      	  CUI('#modify_Owner_Edit_Form').submit();
   	  }
	
	ec.pending.pendingTransition.showEditDialog=function(){
    	try{
			modify_Owner_Edit_FormDialogErrorBarWidget.close();
		}catch(e){}
		if( !ec.pending.pendingTransition.modifyDialog ){
	       ec.pending.pendingTransition.modifyDialog = new CUI.Dialog({
	          title:"<@s.text name='foundation.workflow.modifyOwner.edit'/>",
	          elementId: "ec_tableInfo_modifyOwner_edit_div",
	          formId: "modify_Owner_Edit_Form",
	          modal:true,
	          close:false,
		      type:3,
		      dragable:true,
		      buttons:[{name:"<@s.text name='common.button.save'/>",
		                handler:ec.pending.pendingTransition.save},
		               {name:"<@s.text name='common.button.cancel'/>",
		                handler:function(){this.hide()}
		              }]
		      
	       });
	      }
	      ec.pending.pendingTransition.modifyDialog.show();
		
   }
		
	ec.pending.pendingTransition.checkSelectedAny = function(){
    	if(pendingTableListWidget.selectedRows.length == 0){
        	workbenchErrorBarWidget.showMessage("${getHtmlText('ec.workflow.modifyOwner.checkselected')}","f");
			return false;
    	}
      return true;
    }
	
	ec.pending.pendingTransition.ownerInfoCallback = function(obj){
   		if(obj != null && obj != undefined && obj[0].id != undefined && obj[0].id != null){
   			$( '#ownerId', CUI("#pendingTransitionQueryQueryForm") ).val(obj[0].id);
   		}
    }
	ec.pending.pendingTransition.departmentInfoCallback = function(arr){
	   	if(arr != null && arr != undefined && arr[0].id != undefined && arr[0].id != null){
	       $( "#ownerDepartmentId", CUI("#pendingTransitionQueryQueryForm") ).val(arr[0].id);
	    }
    }
	ec.pending.pendingTransition.positionInfoCallback = function(arr){
		if(arr!=null && arr != undefined && arr[0].id != undefined && arr[0].id != null){
		   $("#ownerPositionId",CUI("#pendingTransitionQueryQueryForm")).val(arr[0].id);
		}
    }
    
   ec.pending.pendingTransition.ownerInfoCallback1=function(obj){
   		if(obj != null && obj != undefined && obj[0].id != undefined && obj[0].id != null){
   			$( '#ownerId', CUI("#pendingTransitionAdvQueryForm") ).val(obj[0].id);
   		} 
   }
   
   ec.pending.pendingTransition.departmentInfoCallback1=function(arr){
   	  if(arr != null && arr != undefined && arr[0].id != undefined && arr[0].id != null){
      		$("#ownerDepartmentId", CUI("#pendingTransitionAdvQueryForm")).val(arr[0].id);
      }
   }
   
   ec.pending.pendingTransition.positionInfoCallback1=function(arr){
	   if(arr != null && arr != undefined && arr[0].id != undefined && arr[0].id != null){
	       $("#ownerPositionId", CUI("#pendingTransitionAdvQueryForm")).val(arr[0].id);
	   }
   }
	
	/**
	 * @Description 负责人更改完成后的回调函数
	 * @method ec.pending.pendingTransition.callback
	 * @param {Object} res
	 * @public
	 */
   ec.pending.pendingTransition.callback = function(res){
      if(res.dealSuccessFlag){
         modify_Owner_Edit_FormDialogErrorBarWidget.show("${getHtmlText('ec.workflow.modifyOwner.saveandclosesuccessful')}","s");
         setTimeout(function(){
			try{
			     ec.pending.pendingTransition.modifyDialog.hide();
			}catch(e){}
	    },500);
	    if( ec.pending.pendingTransition.operateType=='quickQuery'){
	    	ec.pending.pendingTransition.queryForm();
	    }else{
	    	ec.pending.pendingTransition.advQueryForm();
	    }
      }
   }
   /**
   *待办作废
   *
   */
   ec.pending.pendingTransition.deleteMethod = function(){
		if(ec.pending.pendingTransition.checkSelectedAny()){
			CUI.Dialog.confirm("${getHtmlText('foundation.pending.deletepending')}", function(){
		 		var rows = pendingTableListWidget.selectedRows;
		 		var url = "/msService/ec/tableInfo/modifyOwner/invalid";
		 		var params = "ids=";
		 		for(var i = 0; i < rows.length; i++){
		 			if (rows[i].STATUS == 0) {
		 				workbenchErrorBarWidget.showMessage("${getHtmlText('foundation.pending.hasInvalid')}", "f");
		 				return;
		 			}
		 			params += rows[i].ID;
		 			if(i != rows.length -1){
		 				params += ",";
		 			}
		 		}
		 		url = url + "?" + params;
		 		CUI.ajax({
		   			type : "GET",
		   			url : url,
		   			success : function(res){
		   				if(res.dealFalg){
		   					if( ec.pending.pendingTransition.operateType=='quickQuery'){
	    						ec.pending.pendingTransition.queryForm();
	    					}else{
	    						ec.pending.pendingTransition.advQueryForm();
	    					}
		   				}
		   			}
		   		});
			});
		}
	}
	
	ec.pending.pendingTransition.addPending = function(){
		if ( ec.pending.pendingTransition.selectedAny() ) {
			var url = "/msService/ec/pending/pendingTransition/addPendingEdit";
			var row = pendingTableListWidget.selectedRows[0];
			if(row.STATUS == 0) {
				workbenchErrorBarWidget.showMessage("${getHtmlText('foundation.pending.invalidUnallowedAddPending')}", "f");
				return;
			}
			var tableInfoId = row.ID;
			var processKey = row.PKEY;
			var processVersion = row.VERSION;
			var taskCodes = row.ACTIVITYNAME == undefined ? '' : row.ACTIVITYNAME; // 活动code，多个用,分隔
			var activeName = row.CURRENTACTIVE == undefined ? '' : row.CURRENTACTIVE;
			
			ec.pending.pendingTransition.addPendingDialog = new CUI.Dialog({
				title    : "${getHtmlText('foundation.workflow.flow.addPending')}",
				formId   : "addPending_edit_form",
				url      : url += "?tableId=" + tableInfoId + "&processKey=" + processKey + "&processVersion=" + processVersion + "&originalActivityName=" + encodeURIComponent(taskCodes) + "&activeName=" + encodeURIComponent(activeName),
				modal    : true,
				type     : 3,
				dragable : true,
				buttons  : [{	name:"${getHtmlText('common.button.save')}",
								handler:function(){
									ec.pending.pendingTransition.addPendingSave();
								}
						 	},
						 	{	name:"${getHtmlText('common.button.cancel')}",
								handler:function(){this.close();}
						 	}]
			
			});
			ec.pending.pendingTransition.addPendingDialog.show();
		}
	}
	
	ec.pending.pendingTransition.addPendingSave = function() {
		var errorMsg = new Array();
   	  	var errorFields = new Array();
   	    var error = false;
	    if ( CUI.trim( $("#addPending_edit_form #staff_nameMultiIDs").val() ) == '' ) {
	    	errorMsg.push("${getHtmlText('ec.workflow.pendingTransition.checkTrustee')}");
        	errorFields.push(CUI("#addPending_edit_form #staff_nameMultiIDsContainerDiv"));
        	errorFields.push(CUI("#addPending_edit_form #staff_nameMneInput"));
        	error = true;
	    }
	    if ( CUI.trim( $("#activityId", CUI("#addPending_edit_form")).val() ) == '' ) {
	    	 errorMsg.push("${getHtmlText('ec.workflow.pendingTransition.newActitiyIsNull')}");
	    	 errorFields.push(CUI("*[id^='activeName__']", CUI("#addPending_edit_form"))[0]);
	    	 error = true;
	    }
	    if (error) {
	    	addError($("#addPending_edit_form"), addPending_edit_formDialogErrorBarWidget, errorFields, errorMsg);
	    	$.each(errorFields, function(i, item) {
	    		showErrorField(item);
	    	})
	    	return false;
	    }
	    CUI("#addPending_edit_form #staffMultiIDs").val(CUI("#addPending_edit_form #staff_nameMultiIDs").val());
	    CUI("#addPending_edit_form").submit();
	}
	
	ec.pending.pendingTransition.addPendingCallback = function(res) {
		if (res.dealSuccessFlag) {
			addPending_edit_formDialogErrorBarWidget.show("${getHtmlText('foundation.pending.newpendingsuccess')}", "s");
			setTimeout(function(){
				try {
					ec.pending.pendingTransition.addPendingDialog.close();
				} catch(e) {}
			}, 500);
			if (ec.pending.pendingTransition.operateType=='quickQuery') {
				ec.pending.pendingTransition.queryForm();
			} else{
				ec.pending.pendingTransition.advQueryForm();
			}
		}
	}
</script>
<#if (Parameters.openType)?default('page') != 'dialog'>
</body>
</html>
</#if>