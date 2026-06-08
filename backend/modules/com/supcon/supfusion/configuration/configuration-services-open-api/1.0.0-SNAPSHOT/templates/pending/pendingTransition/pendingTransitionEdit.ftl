<#if (Parameters.openType)?default('page') == 'frame'>
<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>${getText('ec.workflow.pendingTransition.edit')}</title>
<@maincss/>
<@mainjs/>
</head>
<body id="dialog_page">
<@loadpanel></@loadpanel>
<@errorbar id="workbenchErrorBar" offsetY=0 />
</#if>


<div id="ec_pending_pendingTransition_edit_div">
   <@errorbar id="pending_Transition_Edit_FormDialogErrorBar"></@errorbar>
    <@s.form id="pending_Transition_Edit_Form" action="save" namespace="/msService/ec/pending/pendingTransition"  callback="parent.ec.pending.pendingTransition.transitionSaveCallback">
	   <@s.hidden id="tableId" name="tableId" value="${tableId!}"/>
	   <@s.hidden id="originalPId" name="originalPId" value="${(deployments.processDefinitionId)!}"/>
	   <table class="cui-fd-infotable" id="editTable" width="90%">
		    <tr>
		           <td style="width: 20%;" class="lab">${getHtmlText('ec.workflow.pendingTransition.originalActivity')}</td>
		           <td style="width: 70%">
		            <div class="fix-input-readonly">
		              <input type="hidden" id="originalActivityName" name="originalActivityName" value="${originalActivityName!}"/>
		              
		              <input id="originalActivity"  name="originalActivity" class="cui-noborder-input" style="margin-left:0px;" type="text" value="${activeName!}" readonly/>
		              </div>
		           </td>
		        </tr>
		        <tr>
		           <td style="width: 20%; " class="lab cui-lmust">${getHtmlText('ec.workflow.pendingTransition.trustee')}</td>
		           <td style="width: 70%">
		             <input name="staffMultiIDs" id="staffMultiIDs" type="hidden"/>
                     <@mneclient iframe=true name="staff.name" id="staff_name" formId="pending_Transition_Edit_Form"  url="/msService/ec/foundation/staff/common/staffListFrameset" onkeyupfuncname="getstaff_nameMultiInfo()" conditionfunc="staff_name_querycustomFunc()" funcparam="multiSelect=true"  classStyle="cui-noborder-input" type="Staff" multiple=true clicked=true isEdit=true/>
		           </td>
		        </tr>
		        <tr>
		        <td style="width: 20%;" class="lab">${getHtmlText('ec.workflow.pendingTransition.flow')}</td>
		         <td style="width: 70%">
		         	<#-- 
		                <select id="flowId" name="processDefinitionId" style="width:100%;border-color:#a2c7ed" onchange="ec.pending.pendingTransition.getActivity()" class="edit-select edit-input-text">
		                <#list deployments.data as deployment>
		                  <option value="${deployment.processDefinitionId}" <#if deployment.processVersion==deployments.version>selected</#if>>${getText(deployment.name)}(${getText('ec.workflow.pendingTransition.flowVersion')}:${deployment.processVersion})</option>
		                </#list>
		                </select>
		             -->
		             <input type="hidden" name="processDefinitionId" value="${(deployments.processDefinitionId)!}" />
		             <input type="text" class="cui-noborder-input" style="margin-left:0px;" id="flowInfo" name="flowInfo" value="${flowName}(${getText('ec.workflow.pendingTransition.flowVersion')}:${processVersion})" readOnly="true" />
				</td>
		        </tr>
		        <tr>
		         <td style="width: 20%;" class="lab cui-lmust">${getHtmlText('ec.workflow.pendingTransition.newActivity')}</td>
		         <td style="width: 70%">
		                <select id="activityId" name="activeName" style="width:100%" class="edit-select edit-input-text">
		                <#if (deployments.activities)?? && deployments.activities?size gt 0>
			                <#list deployments.activities as act>
			                     <#if originalActivityName?index_of(act.taskName)==-1>
			                       	<option value="${act.taskName}">${getText(act.taskDescription)}</option>
			                     </#if>
			                </#list>
		                </#if>
		                </select>
				</td>
		        </tr>
		        <tr> 
				<td width="10%" class="lab cui-lmust v-align">${getHtmlText('ec.workflow.modifyOwner.modifyReason')}</td>
				<td width="20%" class="cui-vte">
				   <div class="fix-input">
				   		<@s.textarea id="modifyReason" name="modifyReason" cssClass="cui-noborder-textarea"></@s.textarea>
				   </div>
				</td>
             </tr>
		   </table>
     </@s.form>
</div>


<script type="text/javascript">
	ec_pending_pendingTransition_transitionSave=function(){
		var errorMessage=new Array();
   	  	var errorFields = new Array();
   	    var error=false;
   	    var form=CUI("#pending_Transition_Edit_Form");
   	    var errorBarWidget=pending_Transition_Edit_FormDialogErrorBarWidget;
	    if(CUI.trim(CUI("#pending_Transition_Edit_Form #staff_nameMultiIDs").val())==''){
            errorMessage.push("${getHtmlText('ec.workflow.pendingTransition.checkTrustee')}");
            errorFields.push(CUI("#pending_Transition_Edit_Form #staff_nameMultiIDsContainerDiv"));
            errorFields.push(CUI("#pending_Transition_Edit_Form #staff_nameMneInput"));
	        error=true;
	    }
	    if(CUI.trim(CUI("#pending_Transition_Edit_Form #staff_nameMultiIDs").val())!=''){
            removeErrorField(CUI("#pending_Transition_Edit_Form #staff_nameMultiIDsContainerDiv"));
            removeErrorField(CUI("#pending_Transition_Edit_Form #staff_nameMneInput"));
	    }
	    if(CUI.trim(CUI("#modifyReason",CUI("#pending_Transition_Edit_Form")).val())==''){
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
</script>

			
<#if (Parameters.openType)?default('page') != 'dialog'>
</body>
</html>
</#if>