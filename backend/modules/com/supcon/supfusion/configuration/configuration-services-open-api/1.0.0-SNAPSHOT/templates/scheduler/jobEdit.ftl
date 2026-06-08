<@errorbar id="SubmitschedulerJobFormDialogErrorBar"></@errorbar>
<@s.form id="SubmitschedulerJobForm" action="schedulerJobSave" namespace="/msService/ec/scheduler" validate="false" callback="ec.scheduler.job.callBack">
	<@s.hidden name="schedulerJob.moduleCode"/>
	<@s.hidden  name="schedulerJob.version" />
	<@s.hidden  name="schedulerJob.projFlag" />
	<@s.hidden  name="schedulerJob.code" />
	<@s.hidden  name="schedulerJob.inheritFlag" />
	<@s.hidden  name="schedulerJob.hasRunTimes" />
	<@s.hidden  name="artifact" value="${artifact!}"/>
	<table style="margin-top:8px" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%">
		<tr>
			<td class="la" style="width:20%;color:rgb(179, 3, 3);">${getHtmlText('ec.scheduler.datasource.code')}</td>
			<td class="co">
				<#if schedulerJob??&&schedulerJob.shortCode??>
					<div class="fix-input-readonly">
					<@s.textfield  name="schedulerJob.shortCode" cssClass="cui-noborder-input"  readOnly="true"/>
				<#else>
					<div class="fix-input">
					<@s.textfield  name="schedulerJob.shortCode" cssClass="cui-noborder-input"/>
				</#if>
				</div>
			</td>
			<td class="la" style="width:20%;color:rgb(179, 3, 3);">${getHtmlText('ec.scheduler.job.name')}</td>
			<td class="co">
				<#if schedulerJob??&&schedulerJob.name??>
					<@international name="schedulerJob.name" moduleCode=(artifact)! key=(schedulerJob.name)! isNew=true maxLength=80></@international>
				<#else>
					<@international name="schedulerJob.name" moduleCode=(artifact)! key=(schedulerJob.name)! isNew=true maxLength=80></@international>
				</#if>
			</td>
		</tr>
		<tr>
			<td class="la" style="width:20%;color:rgb(179, 3, 3);">${getHtmlText('ec.scheduler.job.isScheduleImm')}</td>
			<td class="co"><select class="edit-select edit-input-text" name="schedulerJob.isScheduleImm">
				<option value="true"
					<#if schedulerJob?? && schedulerJob.isScheduleImm>
						selected
					</#if>
				>${getText('ec.scheduler.datasource.isScheduleImm.true')}</option>
				<option value="false"
					<#if schedulerJob?? && !schedulerJob.isScheduleImm>
						selected
					</#if>
				>${getText('ec.scheduler.datasource.isScheduleImm.false')}</option>
			</select></td>
			<td class="la" name="job_jobtype_td">${getHtmlText('ec.scheduler.job.jobType')}</td>
			<td class="co" name="job_jobtype_td">
				<#if isProj?string('true', 'false')=="true">
					<select name="schedulerJob.jobType" class="edit-select edit-input-text" style="width:100%" onchange="ec.scheduler.job.jobType()">
						<option value="1"<#if  schedulerJob ?? &&  schedulerJob.jobType = 1>selected</#if>>${getText('ec.scheduler.job.jobtype1')}</option>
						<option value="2"<#if  schedulerJob ?? &&  schedulerJob.jobType = 2>selected</#if>>${getText('ec.scheduler.job.jobtype2')}</option>
					</select>
				<#else>
					<input type="hidden" name="schedulerJob.jobType" value="1">
					<input type="text" value="${getText('ec.scheduler.job.jobtype1')}" class="cui-readonly-field" readonly=true>
				</#if>
			</td>
		</tr>
		<tr>
		<tr id="job_url_tr">
			<td class="la" name="job_url_td" style="width:20%;color:rgb(179, 3, 3);">${getHtmlText('ec.scheduler.job.jobContent1')}
				<span id="jobContentHelpinfo" class="baphelp-icon"></span>
				<div id="jobContentHelpinforef" style="display:none">
				<p class="baphelp-info">url地址，任务调度需要执行的url地址 </p>
				<p class="baphelp-example">范例</p>
				<div class="baphelp-code">
				<pre><code><span>绝对路径：http://ip:port/foundation/user/add</br>相对路径：/foundation/user/add</span></code></pre>
				</div>
				</div>
				<script type="text/javascript">
					$('#jobContentHelpinfo').helptip({refElm: "#jobContentHelpinforef", html: true , isCustom :false, width: 460 , title :"说明"});
				</script>
			</td>
			<td class="co" style="width:16%" colspan="3">
				<#if schedulerJob??&&schedulerJob.jobContent??>
					<div class="fix-input-readonly">
					<@s.textarea  name="schedulerJob.jobContent"  cssClass="cui-noborder-input"  readOnly="true"/>
				<#else>
					<div class="fix-input">
					<@s.textarea  name="schedulerJob.jobContent"  cssClass="cui-noborder-input"/>
				</#if>
				</div>
			</td>
		</tr>
		<#if isProj?string('true', 'false')=="true">
		<tr id="job_datasource_tr" style="display:none;">
			<td class="la" name="job_datasource_td" style="width:20%;color:rgb(179, 3, 3);">${getHtmlText('ec.scheduler.job.datasource')}</td>
			<td class="co" name="job_datasource_td">
				<select <#if  schedulerJob ?? && schedulerJob.datasouceCode??>disabled</#if> class="edit-select edit-input-text" name="schedulerJob.datasouceCode" style="margin-top:5px;width:100%">
					<#list dataSourceList as ds>
						<option value="${ds.code}"
						<#if  schedulerJob ?? && schedulerJob.datasouceCode?? &&  schedulerJob.datasouceCode = ds.code>
							selected
						</#if>
						>
						${getText('${ds.name}')}
						</option>
					</#list>
				</select>
			</td>
			<td class="la" name="job_procedure_td" style="margin-top:5px;width:20%;color:rgb(179, 3, 3);">${getHtmlText('ec.scheduler.job.procedure')}</td>
			<td class="co" name="job_procedure_td">
				<#if schedulerJob??&&schedulerJob.procedureName??>
					<div class="fix-input-readonly">
					<@s.textfield  name="schedulerJob.procedureName"  cssClass="cui-noborder-input"  readOnly="true"/>
				<#else>
					<div class="fix-input">
					<@s.textfield  name="schedulerJob.procedureName"  cssClass="cui-noborder-input"/>
				</#if>
				</div>
			</td>
		</tr>
		</#if>
		<tr id="job_triggerType_tr">
			<td class="la" name="job_triggerType_td">${getHtmlText('ec.scheduler.job.triggerType')}</td>
			<td class="co" name="job_triggerType_td">
				<select name="schedulerJob.triggerType" class="edit-select edit-input-text" onchange="ec.scheduler.job.triggerType()">
					<option value="CRON"<#if  schedulerJob ?? &&  schedulerJob.triggerType = 'CRON'>selected</#if>>${getText('ec.scheduler.job.triggerType1')}</option>
					<option value="NOR"<#if  schedulerJob ?? &&  schedulerJob.triggerType = 'NOR'>selected</#if>>${getText('ec.scheduler.job.triggerType2')}</option>
				</select>
			</td>
			<td id ="job_cron_td" class="la" style="width:8%;color:#B30303">${getHtmlText('ec.scheduler.job.cron')}
				<span id="cronHelpinfo" class="baphelp-icon"></span>
				<div id="cronHelpinforef" style="display:none">
				<p class="baphelp-info">cron表达式，任务调度按照何时按照何种频次执行 </p>
				<p class="baphelp-example">范例</p>
				<div class="baphelp-code">
				<pre><code><span>1、每月最后一天00:00:00 ： 0 0 0 L * ? *</span></code></pre>
				<pre><code><span>2、每周五00:00:00 ： 0 0 0 6 * ? *</span></code></pre>
				<pre><code><span>3、每天晚上十二点 ： 0 0 0 * * ?</span></code></pre>
				<pre><code><span>4、每天凌晨1点 ： 0 0 1 * * ?</span></code></pre>
				<pre><code><span>5、每月1号凌晨1点 ： 0 0 1 1 * ?</span></code></pre>
				</div>
				</div>
				<script type="text/javascript">
					$('#cronHelpinfo').helptip({refElm: "#cronHelpinforef", html: true , isCustom :false, width: 460 , title :"说明"});
				</script>
			</td>
			<td id ="job_cronValue_td" style="width:16%" align="left">
				<div class="fix-input">
					<@s.textfield  name="schedulerJob.cron"  cssClass="cui-noborder-input"/>
				</div>
			</td>
		</tr>
		<tr id="job_time_tr" style="display:none;">
			<td class="la" style="width:20%;color:rgb(179, 3, 3);">${getHtmlText('ec.scheduler.job.startTime')}</td>
			<td class="co" style="width:30%">
					<#if schedulerJob??&&schedulerJob.startTime??>
						<@datepicker name="schedulerJob.startTime" id="starttime" type="dateTime" value="${schedulerJob.startTime!}"></@datepicker>
					<#else>
						<@datepicker name="schedulerJob.startTime" id="starttime" type="dateTime"></@datepicker>
					</#if>
			</td>
			<td class="la" style="width:20%;">${getHtmlText('ec.scheduler.job.endtime')}</td>
			<td class="co" style="width:30%">
					<#if schedulerJob??&&schedulerJob.endTime??>
						<@datepicker name="schedulerJob.endTime" id="endTime" type="dateTime" value="${schedulerJob.endTime!}"></@datepicker>
					<#else>
						<@datepicker name="schedulerJob.endTime" id="endTime" type="dateTime"></@datepicker>
					</#if>
			</td>
		</tr>
		<tr id="job_nor_tr" style="display:none;">
			<td class="la" style="width:20%;color:rgb(179, 3, 3);">${getHtmlText('ec.scheduler.job.repeatCount')}</td>
			<td class="co" style="width:30%">
				<div class="fix-input">
					<@s.textfield  name="schedulerJob.repeatCount"  cssClass="cui-noborder-input"/>
				</div>
			</td>
			<td class="la" style="width:20%;color:rgb(179, 3, 3);">${getHtmlText('ec.scheduler.job.intervalNum')}</td>
			<td class="co" style="width:30%">
				<div class="fix-input">
					<@s.textfield  name="schedulerJob.intervalNum"  cssClass="cui-noborder-input"/>
				</div>
			</td>
		</tr>
		<tr id="job_intervalUnit_tr" style="display:none;width:20%;">
			<td class="la">${getHtmlText('ec.scheduler.job.intervalUnit')}</td>
			<td class="co">
				<select name="schedulerJob.intervalUnit" class="edit-select edit-input-text" style="margin-top:5px;width:100%">
					<option value="SECOND"<#if  schedulerJob ?? &&  schedulerJob.intervalUnit = 'SECOND'>selected</#if>>${getText('ec.scheduler.job.intervalUnit.second')}</option>
					<option value="MINUTE"<#if  schedulerJob ?? &&  schedulerJob.intervalUnit = 'MINUTE'>selected</#if>>${getText('ec.scheduler.job.intervalUnit.minute')}</option>
					<option value="HOUR"<#if  schedulerJob ?? &&  schedulerJob.intervalUnit = 'HOUR'>selected</#if>>${getText('ec.scheduler.job.intervalUnit.hour')}</option>
					<option value="DAY"<#if  schedulerJob ?? &&  schedulerJob.intervalUnit = 'DAY'>selected</#if>>${getText('ec.scheduler.job.intervalUnit.day')}</option>
					<option value="WEEK"<#if  schedulerJob ?? &&  schedulerJob.intervalUnit = 'WEEK'>selected</#if>>${getText('ec.scheduler.job.intervalUnit.week')}</option>
					<option value="MONTH"<#if  schedulerJob ?? &&  schedulerJob.intervalUnit = 'MONTH'>selected</#if>>${getText('ec.scheduler.job.intervalUnit.month')}</option>
				</select>
				(建议间隔周期大于1分钟!)
			</td>
		</tr>
		<tr>
			<td class="la">${getHtmlText('ec.scheduler.job.description')}</td>
			<td class="co" colspan="3">
					<@s.textarea  name="schedulerJob.description"  cssClass="cui-edit-textarea"  cssStyle="margin-top:5px;width:405px;height:60px"/>
			</td>
		</tr>
	</table>
</@s.form>
<script type="text/javascript" src="/bap/static/ec/js/cron.js"></script>
<script type="text/javascript" charset="UTF-8" language="javascript">
	// 注册
	CUI.ns("ec.scheduler.job");
	/* 任务调度
	 * @method ec.scheduler.job.requiredVerification
	 * @public
	 */
	ec.scheduler.job.requiredVerification=function(){
		clearErrorLabels();
		var errorMsg='';
		if($("#SubmitschedulerJobForm input[name='schedulerJob.shortCode']").val()==""){
			showErrorField($("#SubmitschedulerJobForm input[name='schedulerJob.shortCode']"));
			errorMsg+="${getHtmlText('ec.scheduler.datasource.code.Required')}<br/>";
		}
		if($("#SubmitschedulerJobForm input[name='schedulerJob.name']").val()==""){
			showErrorField($("#SubmitschedulerJobForm input[name='schedulerJob.name']"));
			errorMsg+="${getHtmlText('ec.scheduler.job.name.Required')}<br/>";
		}
		if(CUI('*[name="schedulerJob.jobType"]').val() == "1"){
			if(CUI('*[name="schedulerJob.jobContent"]').val()==""){
				showErrorField(CUI('*[name="schedulerJob.jobContent"]'));
				errorMsg+="${getHtmlText('ec.scheduler.job.jobContent.Required')}<br/>";
			}else{
				var jobContent = CUI('*[name="schedulerJob.jobContent"]').val();
				//if(jobContent.indexOf("http://") == -1){
					//showErrorField(CUI('*[name="schedulerJob.jobContent"]'));
					//errorMsg+="${getHtmlText('ec.scheduler.job.jobContent.error')}<br/>";
				//}
			}
		}else{
			if($('[name="schedulerJob.datasouceCode"]').val()==null){
				showErrorField($("#SubmitschedulerJobForm [name='schedulerJob.datasouceCode']").parent());
				errorMsg+="${getHtmlText('ec.scheduler.job.datasouceCode.Required')}<br/>";
			}
			if($("#SubmitschedulerJobForm input[name='schedulerJob.procedureName']").val()==""){
				showErrorField($("#SubmitschedulerJobForm input[name='schedulerJob.procedureName']"));
				errorMsg+="${getHtmlText('ec.scheduler.job.procedureName.Required')}<br/>";
			}
		}
		var schedulerJobname = CUI('*[name="international_schedulerJobname_showName"]');
		if(schedulerJobname.val()!=""){
			if(!checklong(schedulerJobname.val(),150)){
				showErrorField(schedulerJobname);
				errorMsg+="${getHtmlText('ec.scheduler.job.name.maxLength')}<br/>";
			}
		}
		if(CUI('*[name="schedulerJob.triggerType"]').val() == "CRON"){
			if($("#SubmitschedulerJobForm input[name='schedulerJob.cron']").val()==""){
				showErrorField($("#SubmitschedulerJobForm input[name='schedulerJob.cron']"));
				errorMsg+="${getHtmlText('ec.scheduler.job.cron.Required')}<br/>";
			}else{
			        var cron = $("#SubmitschedulerJobForm input[name='schedulerJob.cron']").val();
			        var result = cronValidate(cron);
			        if(result != true){
			        	showErrorField($("#SubmitschedulerJobForm input[name='schedulerJob.cron']"));
			            errorMsg+="${getHtmlText('ec.scheduler.job.cron.error')}<br/>";  
			        }
			}
		}else{
			var isIntegerValidate =/^[0-9]*[1-9][0-9]*$/;//不包含0和负数
			var startTime = $("#SubmitschedulerJobForm input[name='schedulerJob.startTime']");
			if(startTime.val()==""){
				showErrorField(startTime);
				errorMsg+="${getHtmlText('ec.scheduler.job.startTime.Required')}<br/>";
			}
			if(startTime.val() != ""){
				if (!isDateTime(startTime.val())){
					errorMsg+="${getHtmlText('ec.scheduler.job.time.formaterror')}<br/>";
				}
				var tempStartTime = new Date(startTime.val().replace(/-/g, '/'));
				var curDate = new Date();
				//if(tempStartTime<curDate){
				//	showErrorField(startTime);
				//	errorMsg+="${getHtmlText('ec.scheduler.job.startTime.range')}<br/>";
				//}
				var endTime = $("#SubmitschedulerJobForm input[name='schedulerJob.endTime']");
				if(endTime.val() != ""){
					var tempEndTime = new Date(endTime.val().replace(/-/g, '/'));
					if (!isDateTime(endTime.val())){
						errorMsg+="${getHtmlText('ec.scheduler.job.time.formaterror')}<br/>";
					}
					if(tempEndTime<curDate){
						showErrorField(endTime);
						errorMsg+="${getHtmlText('ec.scheduler.job.endTime.range')}<br/>";
					}
					if(endTime.val() != "" && tempEndTime.getTime()<tempStartTime.getTime()){
						showErrorField(startTime);
						showErrorField(endTime);
						errorMsg+="${getHtmlText('ec.scheduler.job.time.range')}<br/>";
					}
				}
			}
			var repeatCount = $("#SubmitschedulerJobForm input[name='schedulerJob.repeatCount']");
			if(repeatCount.val()==""){
				showErrorField(repeatCount);
				errorMsg+="${getHtmlText('ec.scheduler.job.repeatCount.Required')}<br/>";
			}else{
				if(!isIntegerValidate.test(repeatCount.val())){
					showErrorField(repeatCount);
					errorMsg+="${getHtmlText('ec.scheduler.job.repeatCount.rang')}<br/>";
				}
				if(!checklong(repeatCount.val(),9)){
					showErrorField(repeatCount);
					errorMsg+="${getHtmlText('ec.scheduler.job.repeatCount.maxLength')}<br/>";
				}
			}
			if($(repeatCount).val() == "1"){
				if(startTime.val() != ""){
					var tempStartTime = new Date(startTime.val().replace(/-/g, '/'));
					var curDate = new Date();
					if(tempStartTime<curDate){
						showErrorField(startTime);
						errorMsg+="${getHtmlText('ec.scheduler.job.startTime.range')}<br/>";
					}
				}
			}
			var intervalNum = $("#SubmitschedulerJobForm input[name='schedulerJob.intervalNum']");
			if($(repeatCount).val() != "1"){
				if(intervalNum.val()==""){
					showErrorField($("#SubmitschedulerJobForm input[name='schedulerJob.intervalNum']"));
					errorMsg+="${getHtmlText('ec.scheduler.job.intervalNum.Required')}<br/>";
				}else{
					if(intervalNum.val()!=""){
						if(!isIntegerValidate.test(intervalNum.val())){
								showErrorField(intervalNum);
								errorMsg+="${getHtmlText('ec.scheduler.job.intervalNum.rang')}<br/>";
						}
						if(!checklong(intervalNum.val(),9)){
							showErrorField(intervalNum);
							errorMsg+="${getHtmlText('ec.scheduler.job.intervalNum.maxLength')}<br/>";
						}
					}
				}
			}
		}
		var description = CUI('*[name="schedulerJob.description"]');
		if(description.val()!=""){
			if(!checklong(description.val(),150)){
				showErrorField(description);
				errorMsg+="${getHtmlText('ec.scheduler.job.description.maxLength')}<br/>";
			}
		}
		if(errorMsg != ''){
			SubmitschedulerJobFormDialogErrorBarWidget.show(errorMsg);
			return false;
		}else{
			return true;
		}
	}
	
	/* 编码规则验证
	 * @method ec.scheduler.job_validate
	 * @public
	 */
	ec.scheduler.job.jobValidate=function(){
		var errorMsg='';
		var codeValidate=/^[a-z]{1}[a-zA-Z0-9_]{2,19}$/;
	 	var obj = $("#SubmitschedulerJobForm input[name='schedulerJob.shortCode']").val();
	 	$("#SubmitschedulerJobForm input[name='schedulerJob.shortCode']").val(CUI.trim(obj));
		if (!codeValidate.test($("#SubmitschedulerJobForm input[name='schedulerJob.shortCode']").val())){
			showErrorField($("#SubmitschedulerJobForm input[name='schedulerJob.shortCode']"));
			errorMsg+="${getHtmlText('ec.scheduler.datasource.code.formatmessage')}<br/>"
		}
		if(errorMsg != ''){
			SubmitschedulerJobFormDialogErrorBarWidget.show(errorMsg);
			return false;
		}else{
			return true;
		}
	}
	
	ec.scheduler.job.jobType = function(){
		if(CUI('*[name="schedulerJob.jobType"]').val() == "1"){
			$("#job_url_tr").show();
			$("#job_datasource_tr").hide();
		}else{
			$("#job_datasource_tr").show();
			$("#job_url_tr").hide();
		}
	}
	
	ec.scheduler.job.triggerType = function(){
		if(CUI('*[name="schedulerJob.triggerType"]').val() == "NOR"){
			$("#job_nor_tr").show();
			$("#job_time_tr").show();
			$("#job_intervalUnit_tr").show();
			$("#job_cron_td").hide();
			$("#job_cronValue_td").hide();
		}else{
			$("#job_cron_td").show();
			$("#job_cronValue_td").show();
			$("#job_nor_tr").hide();
			$("#job_intervalUnit_tr").hide();
			$("#job_time_tr").hide();
		}
	}
	ec.scheduler.job.jobType();
	ec.scheduler.job.triggerType();	
	function querySchedulerJobCodeUnique(shortCode,moduleCode)
	{
		var flag = false;
		CUI.ajax({
			url: "/msService/ec/scheduler/querySchedulerJobUnique?schedulerJob.shortCode=" + shortCode + "&schedulerJob.moduleCode=" + moduleCode + "&isProj=${isProj?string('true', 'false')}",
			type: 'post',
			async: false,
			success: function(subInfos){
				if(undefined != subInfos && null != subInfos && subInfos != "" && subInfos.flag == "false"){
					showErrorField($("#SubmitschedulerJobForm input[name='schedulerJob.shortCode']"));
					SubmitschedulerJobFormDialogErrorBarWidget.show('${getText("ec.scheduler.datasource.code.repeat")}');
					flag = false;
				}else{
					flag = true;
				}
			}
		});
		return flag;
	}
	
	function showDialog(){
		if($('#printTemplate_defined_sqlscript').attr('canClick')=="true"){
			dg.advQuery.showAdv();
		}
	}

</script>
<style>
   .restrictMode{padding-left:5px;float:left;height:28px;line-height:28px;padding-top:2px;}
  .multi-mne-img-delete {
			cursor:pointer;
			vertical-align:middle;
			padding-bottom:3px;	
		}
	.multi-mne-img-edit {
		cursor:pointer;
		vertical-align:middle;
		padding-bottom:3px;	
	}
	#tab_other td{
		padding: 3px;
	}
	#other_config_table  td,th  {
		border:1px solid #c6d4e1;
		border-collapse:collapse;
	}
	.cui-noborder-inputTemp {
		border:0px;
		line-height: 20px;
		height: 20px;
		*line-height: 18px;
		*height: 18px;
		padding-right: 1px;
		padding-left: 2px;
		word-break: break-all;
	}
	.baphelp-icon {
	vertical-align: text-bottom;	
}

.baphelp-info {
	
}

.baphelp-example {
	background-color: #f2f2f2;
	margin-left: -12px;
	margin-right: -12px;
	padding: 5px 10px;
	margin-top: 5px;
	margin-bottom: 5px;
}


.baphelp-hint {
	margin-top: 5px;
	padding: 6px 5px 0 0;
	border-top: 1px dashed #e4e4e4;
	color: #b10000;
}

.baphelp-code code{
	font-family: arial;
}
</style>
