<@errorbar id="SubmitschedulerDatasourceFormDialogErrorBar"></@errorbar>
<@s.form  id="SubmitschedulerDatasourceForm" action="schedulerDatasourceSave" namespace="/msService/ec/scheduler" validate="false" callback="ec.scheduler.datasource.callBack">
	<@s.hidden name="schedulerDatasource.moduleCode"/>
	<@s.hidden name="schedulerDatasource.version"/>
	<@s.hidden  name="artifact" value="${artifact!}"/>
	<table style="margin-top:8px" class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%">
		<tr>
			<td class="la" style="width:20%;color:rgb(179, 3, 3);">${getHtmlText('ec.scheduler.datasource.code')}</td>
			<td class="co">
				<#if schedulerDatasource??&&schedulerDatasource.code??>
					<div class="fix-input-readonly">
					<@s.textfield  name="schedulerDatasource.code" cssClass="cui-noborder-input"  readOnly="true"/>
				<#else>
					<div class="fix-input">
					<@s.textfield  name="schedulerDatasource.code" cssClass="cui-noborder-input"/>
				</#if>
				</div>
			</td>
			<td class="la" style="width:20%;color:rgb(179, 3, 3);">${getHtmlText('ec.scheduler.datasource.name')}</td>
			<td class="co">
				<#if schedulerDatasource??&&schedulerDatasource.name??>
					<@international name="schedulerDatasource.name" key=(schedulerDatasource.name)! moduleCode=(artifact)! isNew=true maxLength=80></@international>
				<#else>
					<@international name="schedulerDatasource.name" key=(schedulerDatasource.name)! moduleCode=(artifact)! isNew=true maxLength=80></@international>
				</#if>
			</td>
		</tr>
		<tr>
			<td class="la" name="job_jobtype_td">${getHtmlText('ec.scheduler.datasource.datasourceType')}</td>
			<td class="co" name="job_jobtype_td">
				<select name="schedulerDatasource.datasourceType" class="edit-select edit-input-text" style="width:100%">
					<option value="ORACLE"<#if schedulerDatasource?? && schedulerDatasource.datasourceType = "ORACLE">selected</#if>>${getText('ec.scheduler.datasource.datasourceType.oracle')}</option>
					<option value="SQLSERVER"<#if schedulerDatasource?? && schedulerDatasource.datasourceType = "SQLSERVER">selected</#if>>${getText('ec.scheduler.datasource.datasourceType.sqlserver')}</option>
				</select>
			</td>
			<td class="la" style="width:20%;color:rgb(179, 3, 3);">${getHtmlText('ec.scheduler.datasource.datasourceAddress')}</td>
			<td class="co">
				<#if schedulerDatasource??&&schedulerDatasource.datasourceAddress??>
					<div class="fix-input">
					<@s.textfield  name="schedulerDatasource.datasourceAddress"  cssClass="cui-noborder-input"/>
				<#else>
					<div class="fix-input">
					<@s.textfield  name="schedulerDatasource.datasourceAddress"  cssClass="cui-noborder-input"/>
				</#if>
				</div>
			</td>
		</tr>
		<tr>
			<td class="la" style="width:8%;color:#B30303">${getHtmlText('ec.scheduler.datasource.datasourceName')}</td>
			<td class="co">
				<#if schedulerDatasource??&&schedulerDatasource.datasourceName??>
					<div class="fix-input">
					<@s.textfield  name="schedulerDatasource.datasourceName"  cssClass="cui-noborder-input"/>
				<#else>
					<div class="fix-input">
					<@s.textfield  name="schedulerDatasource.datasourceName"  cssClass="cui-noborder-input"/>
				</#if>
				</div>
			</td>
			<td class="la" style="width:8%;color:#B30303">${getHtmlText('ec.scheduler.datasource.port')}</td>
			<td class="co">
				<#if schedulerDatasource??&&schedulerDatasource.port??>
					<div class="fix-input">
					<@s.textfield  name="schedulerDatasource.port"  cssClass="cui-noborder-input"/>
				<#else>
					<div class="fix-input">
					<@s.textfield  name="schedulerDatasource.port"  cssClass="cui-noborder-input"/>
				</#if>
				</div>
			</td>
		</tr>
		<tr>
			<td class="la" style="width:8%;color:#B30303">${getHtmlText('ec.scheduler.datasource.username')}</td>
			<td class="co">
				<#if schedulerDatasource??&&schedulerDatasource.name??>
					<div class="fix-input">
					<@s.textfield  name="schedulerDatasource.username"  cssClass="cui-noborder-input"/>
				<#else>
					<div class="fix-input">
					<@s.textfield  name="schedulerDatasource.username"  cssClass="cui-noborder-input"/>
				</#if>
				</div>
			</td>
			<td class="lab" style="width:8%;color:#B30303">${getHtmlText('ec.scheduler.datasource.password')}</td>
			<td class="co">
				<#if schedulerDatasource??&&schedulerDatasource.password??>
					<div class="fix-input">
					<@s.password  name="schedulerDatasource.password"  cssClass="cui-noborder-input"/>
				<#else>
					<div class="fix-input">
					<@s.password  name="schedulerDatasource.password"  cssClass="cui-noborder-input"/>
				</#if>
				</div>
			</td>
		</tr>
	</table>
</@s.form>
<script type="text/javascript" charset="UTF-8" language="javascript">
	//注册
	CUI.ns("ec.scheduler.datasource");
	
	/* 校验
	 * @method ec.scheduler.datasource.requiredVerification
	 * @public
	 */
	ec.scheduler.datasource.requiredVerification=function(){
		clearErrorLabels();
		var errorMsg='';
		if($("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.code']").val()==""){
			showErrorField($("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.code']"));
			errorMsg+="${getHtmlText('ec.scheduler.datasource.code.Required')}<br/>";
		}
		if($("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.name']").val()==""){
			showErrorField($("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.name']"));
			errorMsg+="${getHtmlText('ec.scheduler.datasource.name.Required')}<br/>";
		}
		if($("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.datasourceAddress']").val()==""){
			showErrorField($("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.datasourceAddress']"));
			errorMsg+="${getHtmlText('ec.scheduler.datasource.datasourceAddress.Required')}<br/>";
		}
		if($("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.datasourceName']").val()==""){
			showErrorField($("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.datasourceName']"));
			errorMsg+="${getHtmlText('ec.scheduler.datasource.datasourceName.Required')}<br/>";
		}
		if($("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.port']").val()==""){
			showErrorField($("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.port']"));
			errorMsg+="${getHtmlText('ec.scheduler.datasource.port.Required')}<br/>";
		}
		if($("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.username']").val()==""){
			showErrorField($("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.username']"));
			errorMsg+="${getHtmlText('ec.scheduler.datasource.username.Required')}<br/>";
		}
		if($("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.password']").val()==""){
			showErrorField($("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.password']"));
			errorMsg+="${getHtmlText('ec.scheduler.datasource.password.Required')}";
		}
		if(errorMsg != ''){
			SubmitschedulerDatasourceFormDialogErrorBarWidget.show(errorMsg);
			return false;
		}else{
			return true;
		}
	}
	
	 /* 编码规则验证
	 * @method ec.scheduler.datasource_validate
	 * @public
	 */
	ec.scheduler.datasource.datasourceValidate=function(){
		var errorMsg='';
		var codeValidate=/^[a-z]{1}[a-zA-Z0-9_]{2,19}$/;
	 	var obj = $("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.code']").val();
		if (!codeValidate.test(CUI.trim(obj))){
			showErrorField($("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.code']"));
			errorMsg+="${getHtmlText('ec.scheduler.datasource.code.formatmessage')}<br/>"
		}
		var schedulerDatasourcename = CUI('*[name="international_schedulerDatasourcename_showName"]');
		if(!checklong(schedulerDatasourcename.val(),150)){
			showErrorField(schedulerDatasourcename);
			errorMsg+="${getHtmlText('ec.scheduler.datasource.name.maxLength')}<br/>";
		}
		var ipValidate =/^(\d|[1-9]\d|1\d{2}|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d{2}|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d{2}|2[0-4]\d|25[0-5])\.(\d|[1-9]\d|1\d{2}|2[0-4]\d|25[0-5])$/;
		var obj = $("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.datasourceAddress']").val();
		if (!ipValidate.test(CUI.trim(obj))){
			showErrorField($("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.datasourceAddress']"));
			errorMsg+="${getHtmlText('ec.scheduler.datasource.datasourceAddress.formatmessage')}<br/>"
		}
		if(!checklong(CUI.trim(obj),150)){
			showErrorField($("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.datasourceAddress']"));
			errorMsg+="${getHtmlText('ec.scheduler.datasource.datasourceAddress.maxLength')}<br/>";
		}
	 	var datasourceName = $("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.datasourceName']").val();
		if (!checklong(datasourceName,150)){
			showErrorField($("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.datasourceName']"));
			errorMsg+="${getHtmlText('ec.scheduler.datasource.datasourceName.maxLength')}<br/>"
		}
		var portValidate =/^[0-9]*[1-9][0-9]*$/;
		var port = $("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.port']").val();
		if (!portValidate.test(CUI.trim(port))){
			showErrorField($("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.port']"));
			errorMsg+="${getHtmlText('ec.scheduler.datasource.port.formatmessage')}<br/>"
		}
		if (!checklong(port,150)){
			showErrorField($("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.port']"));
			errorMsg+="${getHtmlText('ec.scheduler.datasource.port.maxLength')}<br/>"
		}
		var username = $("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.username']").val();
		if (!checklong(username,150)){
			showErrorField($("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.username']"));
			errorMsg+="${getHtmlText('ec.scheduler.datasource.username.maxLength')}<br/>"
		}
		var password = $("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.password']").val();
		if (!checklong(password,150)){
			showErrorField($("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.password']"));
			errorMsg+="${getHtmlText('ec.scheduler.datasource.password.maxLength')}<br/>"
		}
		if(errorMsg != ''){
			SubmitschedulerDatasourceFormDialogErrorBarWidget.show(errorMsg);
			return false;
		}else{
			return true;
		}
	}
	
	function querySchedulerDatasourceCodeUnique(obj)
	{
		var flag = false;
		CUI.ajax({
			url: "/msService/ec/scheduler/querySchedulerDatasourceCodeUnique?schedulerDatasource.code=" + obj + "&isProj=true",
			type: 'post',
			async: false,
			success: function(subInfos){
				if(undefined != subInfos && null != subInfos && subInfos != "" && subInfos.flag == "false"){
				showErrorField($("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.code']"));
					SubmitschedulerDatasourceFormDialogErrorBarWidget.show('${getText("ec.scheduler.datasource.code.repeat")}');
					flag = false;
				}else{
					flag = true;
				}
			}
		});
		return flag;
	}
	
	function datasourceTestConnection(testFlag)
	{
		var loading = new CUI.loading({  
		"head": "数据处理中, 请稍等",  
		"opacity":50,  
		"bgColor":"#666666",  
		"show": true                  
		}); 
		var flag = false;
		CUI.ajax({
			data : {"schedulerDatasource.datasourceType":$("#SubmitschedulerDatasourceForm select[name='schedulerDatasource.datasourceType']").val(),
					"schedulerDatasource.datasourceAddress":$("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.datasourceAddress']").val(),
					"schedulerDatasource.datasourceName":$("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.datasourceName']").val(),
					"schedulerDatasource.port":$("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.port']").val(),
					"schedulerDatasource.username":$("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.username']").val(),
					"schedulerDatasource.password":$("#SubmitschedulerDatasourceForm input[name='schedulerDatasource.password']").val(),
					"isProj":true
					},
			url: "/msService/ec/scheduler/datasourceTestConnection",
			type: 'post',
			async: true,
			success : function(msg){
				//关闭遮罩  
				loading.close();
				if(msg && msg.success){
					if(testFlag){
						CUI.Dialog.alert("${getText('ec.scheduler.datasource.connectionsuccess')}");
					}else{
						flag = true;
					}
				}else{
					if(testFlag){
						CUI.Dialog.alert("${getText('ec.scheduler.datasource.connectionfailure')}");
					}
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

