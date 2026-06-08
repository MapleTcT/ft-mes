<style type="text/css">
.t{background:blue;cursor:pointer;}
.t tr{background:#FFF;}
.selected{background:#f2f200;}
.layoutselect{display:none;}
.cui-search-click {right:5px!important}
</style>
<@errorbar id="ec_view_inherit_formDialogErrorBar" />
<@s.form id="ec_scheduler_inherit_form" action="inheritSave" namespace="/msService/ec/scheduler" onsubmitMethod="ec.scheduler.job.inheritonsubmit()" validate="true" callback="ec.scheduler.job.extendJob.inheritcallBack">
	<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%">
		<tr>
			<td class="la" style="width:25%"></td>
			<td class="co" style="width:65%"></td>
			<td class="la" style="width:10%"></td>
		</tr>
		<tr>
			<td class="la">${getHtmlText('ec.scheduler.choosejob')}</td>
			<td class="co">
				<select id="job_code" name="schedulerJob.code"  style="width:100%">
				<#list jobPage.result as v>
					<option value="${v.code}">${getText(v.name)}</option>
				</#list>
				</select>
			</td>
		</tr>
	</table>
</@s.form>
<script type="text/javascript">
ec.scheduler.job.inheritonsubmit=function(){
	if($("#job_code").val()==""||$("#job_code").val()==null){
		ec_view_inherit_formDialogErrorBarWidget.show("${getText('ec.view.selectviewtoinhert')}");
		return false;
	}
	return true;
}
</script>
