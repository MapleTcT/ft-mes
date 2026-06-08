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
<div id="ec_pending_pendingTransition_addPending_div">
	<@s.form id="addPending_edit_form" action="addPending" namespace="/msService/ec/pending/pendingTransition" callback="ec.pending.pendingTransition.addPendingCallback">
		<@s.hidden id="tableInfoId" name="tableId" value="${tableId!}"/>
		<table class="cui-fd-infotable" id="editTable" style="margin-left:0px;width:90%;">
			<tr>
		        <td style="width: 20%;" class="lab">${getHtmlText('ec.workflow.pendingTransition.flow')}</td>
		        <td style="width: 70%">
		        	<div class="fix-input-readonly">
		        		<input type="hidden" name="processDefinitionId" value="${(processDefinitionId)!}" />
		         		<input type="text" class="cui-noborder-input" style="margin-left:0px;" id="flowInfo" name="flowInfo" value="${flowName}(${getText('ec.workflow.pendingTransition.flowVersion')}:${processVersion})" readonly />
		         	</div>
				</td>
	        </tr>
	        <#if originalActivityName?? && originalActivityName?has_content>
			<tr>
	            <td style="width: 20%;" class="lab">${getHtmlText('ec.workflow.pendingTransition.originalActivity')}</td>
	            <td style="width: 70%">
		            <div class="fix-input-readonly">
		            	<input type="hidden" id="originalActivityName" name="originalActivityName" value="${originalActivityName!}"/>
		            	<input id="originalActivity"  name="originalActivity" class="cui-noborder-input" style="margin-left:0px;" type="text" value="${activeName!}" readonly />
		            </div>
	            </td>
		    </tr>
		    </#if>
	        <tr>
	        	<td style="width: 20%; " class="lab cui-lmust">${getHtmlText('ec.workflow.pendingTransition.trustee')}</td>
	        	<td style="width: 70%">
			    	<input name="staffMultiIDs" id="staffMultiIDs" type="hidden"/>
               		<@mneclient name="staff.name" id="staff_name" formId="addPending_edit_form"  url="/msService/ec/foundation/staff/common/staffListFrameset" onkeyupfuncname="getstaff_nameMultiInfo()" funcparam="multiSelect=true"  classStyle="cui-noborder-input" type="Staff" multiple=true clicked=true isEdit=true/>
	        	</td>
	        </tr>
	        <tr>
		    	<td style="width: 20%;" class="lab cui-lmust">${getHtmlText('ec.workflow.pendingTransition.newActivity')}</td>
		        <td style="width: 70%">
	                <select id="activityId" name="activeName" style="width:100%" class="edit-select edit-input-text">
	                <#list activities as act>
	                	<option value="${act.taskName}">${getText(act.taskDescription)}</option>
	                </#list>
	                </select>
				</td>
		   	</tr>
		</table>
	</@s.form>
</div>
<#if (Parameters.openType)?default('page') != 'dialog'>
</body>
</html>
</#if>