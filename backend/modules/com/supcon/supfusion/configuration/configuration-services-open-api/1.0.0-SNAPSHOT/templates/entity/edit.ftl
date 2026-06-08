<@frame id="ec_model_manage_top" region="north" height=40 region="north">
	<div class="cui-main-title">
		<@loadpanel text="${getText('ec.configMenu.baseinfo')}" />
	</div>
</@frame>
<@errorbar id="ec_entity_edit_formDialogErrorBar" />
<form id="ec_entity_edit_form" name="ec_entity_edit_form"
 action="/msService/ec/entity/save" method="post" callback="ec.entity.addCallback"  onreset="clearErrorMessages(this);clearErrorLabels(this);"
>
	<input type="hidden" name="entity.version" <#if entity?? && entity.version??>value="${entity.version}" <#else>value="0" </#if> id="ec_entity_edit_form_entity_version"/>
    <input type="hidden" name="entity.code" <#if entity?? && entity.code??>value="${entity.code}" </#if> id="ec_entity_edit_form_entity_code"/>
    <input type="hidden" name="entity.module.code" <#if entity?? && entity.module.code??>value="${entity.module.code}" </#if> id="ec_entity_edit_form_entity_module_code" />
	<input type="hidden" name="entity.isControl" id="isControl" <#if entity?exists&&entity.isControl?exists&&entity.isControl== true>value="true"<#else>value="false"</#if>/>
	<input type="hidden" name="entity.payCloseAttention" id="payCloseAttention" <#if entity?exists&&entity.payCloseAttention?exists&&entity.payCloseAttention== true>value="true"<#else>value="false"</#if>/>
	<input type="hidden" name="entity.crossCompanyFlag" id="crossCompanyFlag" <#if entity?exists&&entity.crossCompanyFlag?exists&&entity.crossCompanyFlag== true>value="true"<#else>value="false"</#if>/>
	<input type="hidden" name="entity.mobile" id="mobile" <#if entity?exists&&entity.mobile?exists&&entity.mobile== true>value="true"<#else>value="false"</#if>/>
	<input type="hidden" name="entity.enableRest" id="enableREST" <#if entity?exists&&entity.enableRest?exists&&entity.enableRest== true>value="true"<#else>value="false"</#if>/>
	<input type="hidden" name="entity.enableWs" id="enableWS" <#if entity?exists&&entity.enableWs?exists&&entity.enableWs== true>value="true"<#else>value="false"</#if>/>
	<input type="hidden" name="entity.enableFieldsPermissionConf" id="enableFieldsPermissionConf" <#if entity?exists&&entity.enableFieldsPermissionConf?exists&&entity.enableFieldsPermissionConf== true>value="true"<#else>value="false"</#if>/>
	<table class="infoTable" id="supplant-entity-configinfo" cellpadding="0" cellspacing="0" border="0" align="center">
		<tr>
			<td class="la supplant-required-field" style="width:139px" align="right">${getHtmlText('ec.entity.entityCode')}</td>
			<td class="co" colspan="3">
			<#if isView?? && isView>
			    <input type="text" name="entity.entityName" maxlength="28" <#if entity?? && entity.entityName??>value="${entity.entityName}" readonly="true"</#if> id="ec_entity_edit_form_entity_entityName" class="cui-edit-field"/>
			<#else>
				<#if entity?? && (entity.code)??>
					<input type="text" name="entity.entityName" maxlength="28" <#if entity?? && entity.entityName??>value="${entity.entityName}" readonly="true"</#if> id="ec_entity_edit_form_entity_entityName" class="cui-edit-field"/>
				<#else>
					<input type="text" name="entity.entityName" maxlength="28" value="" id="ec_entity_edit_form_entity_entityName" class="cui-edit-field"/>
				</#if>
			</#if>
			</td>
		</tr>
		<tr>
			<td class="la supplant-required-field" style="" align="right">${getHtmlText('ec.entity.prefix')}</td>
			<td class="co" colspan="3">
				<#if isView?? && isView>
				<input type="text" name="entity.prefix" <#if entity?? && entity.prefix??>value="${entity.prefix}" readonly="true" </#if> id="ec_entity_edit_form_entity_prefix" class="cui-edit-field"/>
				<#else>
				<input type="text" name="entity.prefix" <#if entity?? && entity.prefix??>value="${entity.prefix}"  </#if> id="ec_entity_edit_form_entity_prefix" class="cui-edit-field"/>
				</#if>
				<div><span class="description">(${getHtmlText('ec.entityEdit.entityPrefix')})</span></div>
			</td>
		</tr>
		<tr>
			<td class="la supplant-required-field"  style="" align="right">${getHtmlText('ec.entity.name')}</td>
			<td class="co" width="30%">
			    <input type="text" name="entity.name" <#if entity?? && entity.name??>value="${entity.name}"</#if> disabled="disabled" id="ec_entity_edit_form_entity_name" class="cui-edit-field" style="display:none;"/>
				<#if isView?? && isView>
				<@international name="entity.name" moduleCode="${(module.artifact)!}" key="${entity.name!}" cssClass="cui-edit-field" cssClass="cui-edit-field"  view=true></@international>
				<#else>
				<#if !entity?exists>
					<@international name="entity.name" key=(entity.name)!'' moduleCode=module.artifact isNew=true maxLength=80></@international>
					<#--
					<@international name="entity.name" moduleCode="${(module.artifact)!}" key="" cssClass="cui-edit-field"></@international>
					-->
				<#else>
					<#if (responseMap.isRead)>
						<@international name="entity.name" moduleCode="${(module.artifact)!}" key="${entity.name!}" view=true cssClass="cui-readonly-field"></@international>
					<#else>
						<@international name="entity.name" key=(entity.name)! moduleCode=module.artifact isNew=true maxLength=80></@international>
						<#--
						<@international name="entity.name" moduleCode="${(module.artifact)!}" key="${entity.name!}" cssClass="cui-edit-field"></@international>
						-->
					</#if>
				</#if>
				</#if>
			</td>
			<td class="la supplant-required-field" align="right">${getHtmlText('ec.entity.type')}</td>
		    <td class="co">
		    	<#if entity?? && entity.code??>
		    		<input type="hidden" id="entity_isBase" name="entity.isBase" value="${(entity.isBase!false)?string('true','false')}"/>
		    		<input type="text" name="entity_isBase" width="100%" class="cui-edit-field" readonly="readonly" value="<#if entity.isBase==false>${getText('ec.entity.formtype')}<#else>${getText('ec.entity.isBase')}</#if>" />
		    	<#else>
			    	<select id="entity_isBase" name="entity.isBase" width="100%" onchange="ec.entity.isBaseCheckChoose(this)">
			    		<option></option>
			    		<option value="true" >${getText('ec.entity.isBase')}</option>
			    		<option value="false">${getText('ec.entity.formtype')}</option>
			    	</select>
		    	</#if>
		    </td>
		</tr>
		<#-- 增加实体类别：BAP、S2 -->
		<tr>
			<td class="la"  style="" align="right">${getHtmlText('ec.entity.entityType')}</td>
			<td>
				<#if bapS2integration!false>
				<div class="fix-input">
					<@systemcode name="entity.entityType.id" code="SYSTEM" value="${(entity.entityType.id)!'SYSTEM/BAP'}"></@systemcode>
				</div>
				<#else>
				<div class="fix-input-readonly">
				    <input type="text" name="" value="${getText((entity.entityType.value)!'BAP')}" id="entity_entityType" class="cui-noborder-input" readOnly="true"/>
					<input type="hidden" name="entity.entityType.id" value="${(entity.entityType.id)!'SYSTEM/BAP'}" id="ec_entity_edit_form_entity_entityType_id"/>
				</div>
				</#if>
			</td>
			<!-- 默认提供日志功能
			<td class="la"  style="" align="right">${getHtmlText('ec.entity.enableAudit')}</td>
			<td class="co">
				<input type="hidden" name="entity.enableAudit" value="<#if (entity.enableAudit)?? && entity.enableAudit>true<#else>false</#if>" />
				<input type="checkbox" onclick="ec.entity.setAudit()" id="entityAudit" <#if (entity.enableAudit)?? && entity.enableAudit>checked="checked"</#if>/>
			</td> -->
			<td class="la"  style="" align="right">${getHtmlText('ec.entity.groupEnabled')}</td>
			<td class="co">
			<input type="hidden" name="entity.groupEnabled" value="<#if (entity.groupEnabled)?? && entity.groupEnabled>true<#else>false</#if>" />
			<input type="checkbox" onclick="ec.entity.setGroupEnabled()" id="entityGroupEnabled" <#if (entity.groupEnabled)?? && entity.groupEnabled>checked="checked"</#if>/>

			</td>
		</tr>
		<tr class="tr-workflow" style="*height:20px;">
			<td class="la" style="" align="right" name="td_workflowEnabled">${getHtmlText('ec.entity.workflowEnabled')}</td>
			<td class="co" name="td_workflowEnabled">
				<input type="hidden" id="entity_workflowEnabled" name="entity.workflowEnabled" <#if entity?exists&&entity.workflowEnabled== true>value="true"</#if>/>
				<input type="checkbox" id="entity_workflowEnabled_ck" <#if entity?exists&&entity.workflowEnabled== true>checked="checked"</#if> onclick="javascript:$('#entity_workflowEnabled').val(this.checked);"/>
			</td>
		</tr>
		<tr style="*height:20px;">
			<td class="la td-workflow" style="" align="right">${getHtmlText('ec.entity.payCloseAttention')}</td>
			<td class="co td-workflow"><input type="checkbox" onclick="ec.entity.changePayCloseAttention(this)" id="payCloseAttention" <#if entity?exists&&entity.payCloseAttention?exists&&entity.payCloseAttention== true>checked="checked"</#if> value="true" /></td>

			<td class="la"  style="" align="right">${getHtmlText('ec.entity.crossCompanyFlag')}</td>
			<td class="co"><input type="checkbox" onclick="ec.entity.changeCrossCompanyFlag(this)" id="entityCrossCompanyFlag" <#if entity?exists&&entity.crossCompanyFlag?exists&&entity.crossCompanyFlag== true>checked="checked"</#if> value="true" /></td>
		</tr>
		<tr style="*height:20px;">
			<td class="la"  style="" align="right">${getHtmlText('ec.entity.mobile')}</td>
			<td class="co"><input type="checkbox" onclick="ec.entity.changeMobile(this)" id="entityMobile" <#if entity?exists&&entity.mobile?exists&&entity.mobile== true>checked="checked"</#if> value="true" /></td>
			<!--<td class="la" style="width:20%;padding-right:3px;text-align:right">${getHtmlText('ec.entity.enableAclRestrict')}</td>
			<td class="co">
				<input type="hidden" name="entity.enableAclRestrict" value="<#if (entity.enableAclRestrict)?? && entity.enableAclRestrict>true<#else>false</#if>" />
				<input type="checkbox" onclick="ec.entity.setAclRestrict()" id="entityAclRestrict" <#if (entity.enableAclRestrict)?? && entity.enableAclRestrict>checked="checked"</#if>/>
			</td>-->
		</tr>
		<tr style="*height:20px;">
			<td class="la " style="" align="right">${getHtmlText('ec.entity.enableREST')}</td>
			<td class="co"><input type="checkbox" onclick="ec.entity.changeEnableREST(this)" id="changeEnableREST" <#if entity?exists&&entity.enableRest?exists&&entity.enableRest== true>checked="checked"</#if> value="true" /></td>
			<td class="la"  style="" align="right">${getHtmlText('ec.entity.enableWS')}</td>
			<td class="co"><input type="checkbox" onclick="ec.entity.changeEnableWS(this)" id="changeEnableWS" <#if entity?exists&&entity.enableWs?exists&&entity.enableWs== true>checked="checked"</#if> value="true" /></td>
		</tr>
		<tr	style="*height:20px;">
			<td class="la " style="" align="right">${getHtmlText('ec.entity.enableFieldsPermissionConf')}</td>
 			<td class="co"><input type="checkbox" onclick="ec.entity.changeEnableFieldsPermissionConf(this)" id="changeEnableFieldsPermissionConf" <#if entity?exists&&entity.enableFieldsPermissionConf?exists&&entity.enableFieldsPermissionConf== true>checked="checked"</#if> value="true" /></td>
  		</tr>
		<tr>
			<td class="la" style="" align="right">${getHtmlText('ec.entity.description')}</td>
			<td class="co" colspan="3">
			<#if isView?? && isView>
			    <textarea name="entity.description"  readonly="true" cols="" rows="" id="ec_entity_edit_form_entity_description" class="cui-edit-textarea" style="">${entity.description!}</textarea>
			<#else>
			    <textarea name="entity.description" cols="" rows=""  id="ec_entity_edit_form_entity_description" class="cui-edit-textarea" style=""><#if entity?? && entity.description??>${entity.description!} </#if></textarea>
			</#if>
			</td>
		</tr>
	</table>
</form>
<script type="text/javascript">
(function(){
	//注册命名空间
	CUI.ns("ec.entity");
	ec.entity.changeEntityIsControl=function(obj){
		if(CUI(obj).prop('checked')) {
			CUI("#isControl").val('true');
		}else{
			CUI("#isControl").val('false');
		}
	}
	ec.entity.changePayCloseAttention=function(obj){
		if(CUI(obj).prop('checked')) {
			CUI("#payCloseAttention").val('true');
		}else{
			CUI("#payCloseAttention").val('false');
		}
	}
	ec.entity.changeCrossCompanyFlag=function(obj){
		if(CUI(obj).prop('checked')) {
			CUI("#crossCompanyFlag").val('true');
		}else{
			CUI("#crossCompanyFlag").val('false');
		}
	}
	ec.entity.changeEnableREST=function(obj){
		if(CUI(obj).prop('checked')) {
			CUI("#enableREST").val('true');
		}else{
			CUI("#enableREST").val('false');
		}
	}
	ec.entity.changeEnableWS=function(obj){
		if(CUI(obj).prop('checked')) {
			CUI("#enableWS").val('true');
		}else{
			CUI("#enableWS").val('false');
		}
	}
	ec.entity.changeEnableFieldsPermissionConf=function(obj){
		if(CUI(obj).prop('checked')) {
 			CUI("#enableFieldsPermissionConf").val('true');
 		}else{
  			CUI("#enableFieldsPermissionConf").val('false');
 		}
 	}
	ec.entity.changeMobile=function(obj){
		if(CUI(obj).prop('checked')) {
			CUI("#mobile").val('true');
		}else{
			CUI("#mobile").val('false');
		}
	}
	 /* 编码规则验证
	 * @method ec_module_code_validate
	 * @public
	 */
	ec.entity.codeValidate=function(){
	 	var validate=/^[a-z]{1}[a-zA-Z_0-9]+$/;
	 	var obj = $("#ec_entity_edit_form input[name='entity.entityName']").val();
	 	$("#ec_entity_edit_form input[name='entity.entityName']").val(obj.trim());
		if (validate.test($("#ec_entity_edit_form input[name='entity.entityName']").val())){
			if($('input[name="entity.entityName"]').val()!='' && $('input[name="entity.prefix"]').val() == '') {
				$('input[name="entity.prefix"]').val($('input[name="entity.entityName"]').val());
			}
			return true;
		}else{
		    ec_entity_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.entity.formatmessage')}");
			return false;
		}
	}
	
	/*
	* @method ec_entity_isBaseValidate
	* @public
	*/
	ec.entity.isBaseValidate=function(){
		if($("[name='entity.isBase']").val()==''){
			ec_entity_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.entity.isBaseNotNull')}");
			return false;
		}else{
			return true;
		}
	}
	
	ec.entity.isBaseCheckChoose=function(obj){
		if(obj.value==='false'){
			$(".tr-workflow").show();
			$(".td-workflow").show();
			$("#ec_entity_edit_form #entity_workflowEnabled").val(true);
			$("#ec_entity_edit_form #entity_workflowEnabled_ck").prop('checked', true);
			$("#ec_entity_edit_form #entity_workflowEnabled_ck").prop('disabled', true);
		}else{
			$("#ec_entity_edit_form #entity_workflowEnabled").val(false);
			$("#ec_entity_edit_form #entity_workflowEnabled_ck").prop('checked', false);
			$("#ec_entity_edit_form #entity_workflowEnabled_ck").prop('disabled', false);
			$("#ec_entity_edit_form #payCloseAttention").prop('checked', false);
			$("#ec_entity_edit_form input[name='entity.groupEnabled']").prop('checked', false);
			$(".tr-workflow").hide();
			$(".td-workflow").hide();
		}
	}
	ec.entity.prefixShowOrHide=function(){
		if($('input[name="entity.isBase"]').prop('checked') || $('input[name="entity.isInherentedBase"]').prop('checked')) {
			$('*[name="entity.prefix"]').parents('tr').first().hide();
		} else {
			$('*[name="entity.prefix"]').parents('tr').first().show();
		}
	}
	ec.entity.isInherentedBaseCheckChoose=function(obj){
		if(CUI(obj).prop('checked')) {
			$("#ec_entity_edit_form input[name='entity.workflowEnabled']").prop('disabled', true);
			$("#ec_entity_edit_form input[name='entity.isBase']").prop('disabled', true);
			$("#ec_entity_edit_form input[name='entity.isBase']").prop('checked', true);
			$("#ec_entity_edit_form input[name='entity.groupEnabled']").prop('disabled', true);
		}else{
			$("#ec_entity_edit_form input[name='entity.workflowEnabled']").prop('disabled', false);
			$("#ec_entity_edit_form input[name='entity.isBase']").prop('disabled', false);
			$("#ec_entity_edit_form input[name='entity.groupEnabled']").prop('disabled', false);
		}
	}
	ec.entity.workflowCheckChoose=function(obj){
		if(CUI(obj).prop('checked')) {
			$("#ec_entity_edit_form input[name='entity.isBase']").prop('disabled', true);
			$("#ec_entity_edit_form input[name='entity.isInherentedBase']").prop('disabled', true);
		}else{
			$("#ec_entity_edit_form input[name='entity.isBase']").prop('disabled', false);
			$("#ec_entity_edit_form input[name='entity.isInherentedBase']").prop('disabled', false);
		}
	}
	
	ec.entity.load = function() {
		<#if (responseMap.isRead)>
			CUI('input,textarea', CUI('#ec_entity_edit_form')).prop("readonly", true);
			CUI('input,textarea', CUI('#ec_entity_edit_form')).addClass('cui-readonly-field');
			CUI('input[type="checkbox"]', CUI('#ec_entity_edit_form')).prop("disabled", true);
			
		</#if>
		
		<#if isView?? && isView>
			$("#ec_entity_edit_form input[name='entity.workflowEnabled']").prop('disabled', true);
			$("#ec_entity_edit_form input[name='entity.isBase']").prop('disabled', true);
			//$("#ec_entity_edit_form input[name='entity.groupEnabled']").prop('disabled', true);
			$("#ec_entity_edit_form input[name='entity.isInherentedBase']").prop('disabled', true);
			$("#entityIsControl").prop('disabled', true);
		<#else>
		<#if (entity.code)??>
			$("#ec_entity_edit_form input[name='entity.workflowEnabled']").prop('disabled', true);
			//$("#ec_entity_edit_form input[name='entity.groupEnabled']").prop('disabled', true);
			
		</#if>
		<#if (isProj)?exists && (isProj)>
			$("#entityIsControl").prop('disabled', true);
			<#if entity?exists && (entity.isControl)== true>
				$("#ec_entity_edit_form input[name='entity.entityName']").prop('readonly', true);
				$("#ec_entity_edit_form input[name='entity.entityName']").addClass('cui-readonly-field');
			</#if>
		</#if>
		</#if>
	}
	
	/*ec.entity.setAclRestrict = function(){
		if($('#entityAclRestrict').prop('checked')){
			$('input[name="entity.enableAclRestrict"]').val('true');
		}else{
			$('input[name="entity.enableAclRestrict"]').val('false');
		}
	}*/
	ec.entity.setGroupEnabled = function(){
		if($('#entityGroupEnabled').prop('checked')){
			$('input[name="entity.groupEnabled"]').val('true');
		}else{
			$('input[name="entity.groupEnabled"]').val('false');
		}
	}
	
	ec.entity.setAudit = function(){
		if($('#entityAudit').prop('checked')){
			$('input[name="entity.enableAudit"]').val('true');
		}else{
			$('input[name="entity.enableAudit"]').val('false');
		}
	}
	
	ec.entity.load();
	ec.entity.prefixShowOrHide();
	ec.entity.isBaseCheckChoose($('#entity_isBase')[0]);
	$('input[name="entity.isBase"],input[name="entity.isInherentedBase"],input[name="entity.workflowEnabled"]').click(function(){
		ec.entity.prefixShowOrHide();
	});
	$('input[name="entity.entityName"]').blur(function(){
		if($('input[name="entity.entityName"]').val()!='' && $('input[name="entity.prefix"]').val() == '') {
			$('input[name="entity.prefix"]').val($('input[name="entity.entityName"]').val());
		} 
	});
	<#if isView?? && isView>
	$('input,textarea').prop('disabled', true);
	</#if>
	<#if !entity?? || !entity.code??>
	$("#entity_isBase").mSelect();
	</#if>	
})();
CUI(function(){
	function submitBapForm(){//电子签名成功之后出现进度条并提交表单
		var ecFormFlag = false;
		var retrialFormFlag = false;
		if(ecFormFlag && ( $('#ec_entity_edit_form').parents('.ewc-dialog-blove').length > 0 || ( window != top && !window.frameElement.getAttribute( 'v3_tab_iframe' ) ) ) ) {
			// dialog不出进度条
			ecFormFlag = false;
		}
		ecFormFlag = (ecFormFlag || retrialFormFlag);
		
		//前台验证通过之后出进度条
		CUI.Dialog.toggleAllButton('ec_entity_edit_form',true,ecFormFlag, true);
	// TODO 延时600ms，让页面上已触发 未执行完成的事件处理完成，临时性解决办法
	setTimeout(function(){
		
			// 延迟保存数据, 解决onchange事件无法触发问题
			var formId = 'ec_entity_edit_form';
			var ecformflag = false;
			
			
		    $('input[type="text"]','#'+formId).each(function(){
		        var v=$.trim($(this).val());
		        $(this).val(v);
		    });
			var files = $('input[type="file"]', '#' + formId);
			if(ecformflag || (files!=null&&files.length>0)) {
				ajaxFileUpload(CUI('#'+formId).attr('action'),formId);
			} else {
			
			var postData = CUI('#'+formId).serialize();
			CUI.ajax({
				url : CUI('#'+formId).attr('action'),
				type : 'POST',
				dataType : 'json',
				data : postData,
				error : function(XMLHttpRequest, textStatus, errorThrown){
					//console.log("jqXHR=%o,textStatus=%o,errorThrown=%o", XMLHttpRequest, textStatus, errorThrown );
					if (XMLHttpRequest.status==401) {
						//showLoginDialog();
						return ;
					}
					var msg = CUI.parseJSON(XMLHttpRequest.responseText);
					var errorMsgs = "";
					CUI.each(msg.items,function(index,item){
						if(index.indexOf('.id') != -1 && index.indexOf('.id')+3 == index.length) {
							$("#ec_entity_edit_form *[name^='"+(index.substring(0,index.length -3))+"'][type!='hidden']").each(function(){
								if($(this).parents('td[nullable=false]').length > 0) {
									showErrorField($(this));
								}
							});
						} else {
							var field = CUI("#ec_entity_edit_form *[name='"+index+"']");
							if(field.prop('type') && field.prop('type').toUpperCase() == 'HIDDEN' && field.next() && field.next().length > 0) {
		                		showErrorField(field.next());
		                	} else {
		                		showErrorField(field);
		                	}
						}
						CUI("#ec_entity_edit_form *[name='"+index+"']").first().focus();
						for(var i = 0 ; i < item.length ; i++){
							errorMsgs += item[i] + '<br/>';
						}
					});
					CUI.each(msg.actionErrors,function(index,item){
						errorMsgs += item + '<br/>';
					});
					if(msg.exceptionMsg!=null&&msg.exceptionMsg!=""){
						errorMsgs += msg.exceptionMsg + '<br/>';
					}
					var oErrorWidget = null;
					oErrorWidget = ec_entity_edit_formDialogErrorBarWidget;
					if ( $(oErrorWidget.errorInfoBar).closest('.ewc-dialog-el').length>0 ) {
						oErrorWidget.show(errorMsgs);// 对话框内错误提示显示顶部横条
					}	else {
						oErrorWidget.showMessage(errorMsgs);// 编辑界面错误提示显示右上
					}					
					if(CUI.Dialog){
						CUI.Dialog.toggleAllButton('ec_entity_edit_form', true);
				    }
				},
				success : function(msg){
					window.onbeforeunload = null;
					if(window.containerLoadPanelWidget) {
						setTimeout(function(){closeLoadPanel();}, 500);
					}
					ec.entity.addCallback(msg,postData);
				}
			});
		}
	}, 600);
		return false;
	}

	CUI('#ec_entity_edit_form').unbind('submit.bapForm').bind('submit.bapForm',function(){
		//每次提交时先隐藏报错信息
		try{
		}catch(e){
		
		}
		// 清除错误标红
		try{clearErrorLabels();}catch(e){}
		var ecFormFlag = false;
		var retrialFormFlag = false;
		if(ecFormFlag && ( $(this).parents('.ewc-dialog-blove').length > 0 || ( window != top && !window.frameElement.getAttribute( 'v3_tab_iframe' ) ) ) ) {
			// dialog不出进度条
			ecFormFlag = false;
		}
		ecFormFlag = (ecFormFlag || retrialFormFlag);
		
		
		//禁用所有按钮
		//CUI("body").one("click", function(event){
		//if(CUI.Dialog) CUI.Dialog.toggleAllButton('ec_entity_edit_form',true,ecFormFlag);
		//});
			//if(!validateForm_ec_entity_edit_form())	return false;
		$('#ec_entity_edit_form').trigger('beforeSubmit');
		if($('#ec_entity_edit_form input[name="operateType"]').val() == "submit"){
			var deploymentId=$('#ec_entity_edit_form input[name="deploymentId"]');
			var buttonCode=$('#ec_entity_edit_form input[name="buttonCode"]');
			var namespace=$('#ec_entity_edit_form input[name="namespace"]');
			if( deploymentId.length > 0 && deploymentId.val() != undefined && deploymentId.val() != ''){
				var signatureInfo=signatureUtil.getSignatureInfo(true,'',deploymentId.val(),'','')//判断是否需要进行电子签名
				if(signatureInfo[0] != '') {
					var cancelItem = $('input[name="workFlowVarStatus"]');
					if(cancelItem.val() != "cancel") {
						signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.flow.submit','','ec_entity_edit_form');
						$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_entity_edit_form','ec.flow.submit',false)});
					}
					else {
						signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.edit.remove','','ec_entity_edit_form');
						$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_entity_edit_form','ec.edit.remove',false)});
					}
					
				}
				else {
					submitBapForm();
				}
			}
			else if( buttonCode.length > 0 && buttonCode.val() != undefined && buttonCode.val() != ''){
				var signatureInfo=signatureUtil.getSignatureInfo(false,buttonCode.val());
				if(signatureInfo[0] != '') {
					if(namespace.length > 0 && namespace.val() != undefined && namespace.val() != '') {
						parent.signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),namespace.val(),"ec_entity_edit_form",false,'');
						if(signatureInfo[0] == 'singleSign') {
							parent.$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk(namespace.val(),'ec_entity_edit_form',buttonCode.val(),false)});
						}
						else {
							setTimeout(function(){parent.$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,namespace.val(),'ec_entity_edit_form',buttonCode.val(),false)});},2000);
						}	
					}
					else {
						signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),'',"ec_entity_edit_form",false,'');
						if(signatureInfo[0] == 'singleSign') {
							$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_entity_edit_form',buttonCode.val(),false)});
						}
						else {
							setTimeout(function(){$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,'','ec_entity_edit_form',buttonCode.val(),false)});},2000);
						}
					}
				}
				else {
					submitBapForm();	
				}
			}
			else {
				submitBapForm();
			}
		}
		else {
			submitBapForm();
		}
		return false;
	});

});
</script>