<@errorbar id="createPendingsFormDialogErrorBar" />
<@s.form id="createPendingsForm" action="remind" namespace="/msService/ec/workflow" validate="true" callback="createPendingsFormCallBack">
	<@s.hidden name="tableInfoId" id="tableInfoId" />
		<div class="edit-panes edit-panes-w clearfix">
										<div class="cui-elements pd-top">
<div class="edit-panes-s" style="width:98%;margin-left:2px;">
	<table class="edit-table" style="">
		<tr style='border:none;height:0px; '><td style='height:0px;border:none;width:20%'></td><td style='height:0px;border:none;width:40%'></td><td style='height:0px;border:none;width:20%'></td><td style='height:0px;border:none;width:40%'></td></tr>
		<tr >
				<td  nullable=false class="edit-table-symbol"   align="right" style="text-align: right;  " >
							<label style="width:100%;;"  >${getText('ec.flowActive.activeName')}</label>
				</td>
				<td  nullable=false class="edit-table-content"   style="text-align: left;;" >
						<select name="activeName" id="activeName">
						<#if activityMap??>
						<#list activityMap as item>
						<option value="${item['ACTIVITY_NAME']}">${item['TASK_DESCRIPTION']}</option>
						</#list>
						</#if>
						</select>
				</td>
				<td class="edit-table-symbol">&nbsp;</td>
				
				<td class="edit-table-symbol">&nbsp;</td>
				
		</tr>
		<tr >
				<td  nullable=false class="edit-table-symbol"   align="right" style="text-align: right; color: rgb(179, 3, 3); " >
							<label style="width:100%;;"  >${getText('ec.createPending.pendingDealor')}</label>
				</td>
				<td  nullable=false class="edit-table-content"   colspan="3" style="text-align: left;;" >
				<@mneclient name="createPendingUserIds" url="other"  onkeyupfuncname="getcreatePendingUserIdsMultiInfo()" id="createPendingUserIds"  displayFieldName="staffname"  funcparam="unassignUserSupport=true&systemAdminFlag=true&multiSelect=true" classStyle="cui-noborder-input" type="User" cssStyle="width:97%;float:left;"  multiple=true clicked=true isEdit=true/>
			
				</td>
		</tr>
	</table>
		
</div></div></div></@s.form>
<script type="text/javascript" charset="utf-8" language="javascript">
CUI.ns("ec.waitWork");
$("#activeName").mSelect();

</script>