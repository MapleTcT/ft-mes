<@errorbar id="ec_entity_edit_formDialogErrorBar" />
<form id="ec_entity_edit_form" action="save" namespace="/ec/entity" validate="true" callback="ec.entity.addCallback">
    <input type="hidden" name="entity.version" <#if entity?? && entity.version??>value="${entity.version}" <#else>value="0" </#if> id="ec_entity_edit_form_entity_version"/>
    <input type="hidden" name="entity.code" <#if entity?? && entity.code??>value="${entity.code}" </#if> id="ec_entity_edit_form_entity_code"/>
    <input type="hidden" name="entity.module.code" <#if entity?? && entity.module.code??>value="${entity.module.code}" </#if> id="ec_entity_edit_form_entity_module_code" />
	<input type="hidden" name="entity.isControl" id="isControl" <#if entity?exists&&entity.isControl?exists&&entity.isControl== true>value="true"<#else>value="false"</#if>/>
	<input type="hidden" name="entity.payCloseAttention" id="payCloseAttention" <#if entity?exists&&entity.payCloseAttention?exists&&entity.payCloseAttention== true>value="true"<#else>value="false"</#if>/>
	<input type="hidden" name="entity.crossCompanyFlag" id="crossCompanyFlag" <#if entity?exists&&entity.crossCompanyFlag?exists&&entity.crossCompanyFlag== true>value="true"<#else>value="false"</#if>/>
	<input type="hidden" name="entity.mobile" id="mobile" <#if entity?exists&&entity.mobile?exists&&entity.mobile== true>value="true"<#else>value="false"</#if>/>
	<table class="infoTable" id="supplant-entity-configinfo" cellpadding="0" cellspacing="0" border="0" align="center" width="90%" style="margin-top: 5px">
		<tr>
			<td class="la" style="width: 20%;padding-right: 3px;" align="right">${getHtmlText('ec.entity.entityCode')}</td>
			<td class="co" colspan="3">
				<input type="text" name="entity.entityName" maxlength="28" <#if entity?? && entity.entityName??>value="${entity.entityName}" readonly="true"</#if> id="ec_entity_edit_form_entity_entityName" class="cui-edit-field"/>
			</td>
		</tr>
		<tr>
			<td class="la" style="width: 20%;padding-right: 3px;" align="right">${getHtmlText('ec.entity.prefix')}</td>
			<td class="co" colspan="3">
			    <input type="text" name="entity.prefix" <#if entity?? && entity.prefix??>value="${entity.prefix}" </#if> id="ec_entity_edit_form_entity_prefix" class="cui-edit-field" readonly="true"/>
				<div><span class="description">(${getHtmlText('ec.entityEdit.entityPrefix')})</span></div>
			</td>
		</tr>
		<tr>
			<td class="la"  style="width: 20%;padding-right: 3px;" align="right">${getHtmlText('ec.entity.name')}</td>
			<td class="co" width="30%">
				<input type="text" name="entity.name" disabled="disabled" id="ec_entity_edit_form_entity_name" cssClass="cui-readonly-field" style="display:none;">
				<@international name="entity.name" moduleCode="${(module.artifact)!}" key="${entity.name!}" cssClass="cui-edit-field" cssClass="cui-edit-field"  view=true></@international>
			</td>
			<td class="la"  style="width: 20%;padding-right: 3px;" align="right">${getHtmlText('ec.entity.type')}</td>
		    <td class="co">
		    	<#if entity?? && entity.code??>
		    		<input type="hidden" id="entity_isBase" name="entity.isBase" value="${(entity.isBase!false)?string('true','false')}"/>
		    		<input type="text" name="entity_isBase" width="100%" class="cui-edit-field" readonly="readonly" value="<#if entity.isBase==false>${getText('ec.entity.formtype')}<#else>${getText('ec.entity.isBase')}</#if>" />
		    	<#else>
			    	<select id="entity_isBase" name="entity.isBase" width="100%" onchange="ec.entity.isBaseCheckChoose(this)">
			    		<option value="true" >${getText('ec.entity.isBase')}</option>
			    		<option value="false" <#if entity?? && entity.isBase==false>selected="selected"</#if>>${getText('ec.entity.formtype')}</option>
			    	</select>
		    	</#if>
		    </td>
		</tr>
		<#-- 增加实体类别：BAP、S2 -->
		<tr>
			<td class="la"  style="width: 20%;padding-right: 3px;" align="right">${getHtmlText('ec.entity.entityType')}</td>
			<td>
				<#if bapS2integration!false>
				<div class="fix-input">
					<@systemcode name="entity.entityType.id" code="SYSTEM" value="${(entity.entityType.id)!'SYSTEM/BAP'}"></@systemcode>
				</div>
				<#else>
				<div class="fix-input-readonly">
					<input type="text" id="entity_entityType" readOnly="true" class="cui-noborder-input" value="${getText((entity.entityType.value)!'BAP')}" />
					<input type="hidden" name="entity.entityType.id" value="${(entity.entityType.id)!'SYSTEM/BAP'}" />
				</div>
				</#if>
			</td>
			<!-- 默认提供日志功能
			<td class="la"  style="width: 20%;padding-right: 3px;" align="right">${getHtmlText('ec.entity.enableAudit')}</td>
			<td class="co">
				<input type="hidden" name="entity.enableAudit" value="<#if (entity.enableAudit)?? && entity.enableAudit>true<#else>false</#if>" />
				<input type="checkbox" onclick="ec.entity.setAudit()" id="entityAudit" <#if (entity.enableAudit)?? && entity.enableAudit>checked="checked"</#if>/>
			</td> -->
		</tr>
		<tr class="tr-workflow" style="*height:20px;">
			<td class="la"  style="width: 20%;padding-right: 3px;" align="right">${getHtmlText('ec.entity.groupEnabled')}</td>
			<td class="co"><input type="checkbox" name="entity.groupEnabled" <#if entity?exists&&entity.groupEnabled== true>checked="checked"</#if> value="true" /></td>
			<td class="la" style="width: 20%;padding-right: 3px;" align="right" name="td_workflowEnabled">${getHtmlText('ec.entity.workflowEnabled')}</td>
			<td class="co" name="td_workflowEnabled">
				<input type="hidden" id="entity_workflowEnabled" name="entity.workflowEnabled" <#if entity?exists&&entity.workflowEnabled== true>value="true"</#if>/>
				<input type="checkbox" id="entity_workflowEnabled_ck" <#if entity?exists&&entity.workflowEnabled== true>checked="checked"</#if> onclick="javascript:$('#entity_workflowEnabled').val(this.checked);"/>
			</td>
		</tr>
		<tr style="*height:20px;">
			<td class="la td-workflow" style="width: 20%;padding-right: 3px;" align="right">${getHtmlText('ec.entity.payCloseAttention')}</td>
			<td class="co td-workflow"><input type="checkbox" onclick="ec.entity.changePayCloseAttention(this)" id="payCloseAttention" <#if entity?exists&&entity.payCloseAttention?exists&&entity.payCloseAttention== true>checked="checked"</#if> value="true" /></td>

			<td class="la"  style="width: 20%;padding-right: 3px;" align="right">${getHtmlText('ec.entity.crossCompanyFlag')}</td>
			<td class="co"><input type="checkbox" onclick="ec.entity.changeCrossCompanyFlag(this)" id="entityCrossCompanyFlag" <#if entity?exists&&entity.crossCompanyFlag?exists&&entity.crossCompanyFlag== true>checked="checked"</#if> value="true" /></td>
		</tr>
		<tr style="*height:20px;">
			<td class="la"  style="width: 20%;padding-right: 3px;" align="right">${getHtmlText('ec.entity.mobile')}</td>
			<td class="co"><input type="checkbox" onclick="ec.entity.changeMobile(this)" id="entityMobile" <#if entity?exists&&entity.mobile?exists&&entity.mobile== true>checked="checked"</#if> value="true" /></td>
			<td class="la" style="width:20%;padding-right:3px;text-align:right">${getHtmlText('ec.entity.enableAclRestrict')}</td>
			<td class="co">
				<input type="hidden" name="entity.enableAclRestrict" value="<#if (entity.enableAclRestrict)?? && entity.enableAclRestrict>true<#else>false</#if>" />
				<input type="checkbox" onclick="ec.entity.setAclRestrict()" id="entityAclRestrict" <#if (entity.enableAclRestrict)?? && entity.enableAclRestrict>checked="checked"</#if>/>
			</td>
		</tr>
		<tr>
			<td class="la" style="width: 20%;padding-right: 3px;" align="right">${getHtmlText('ec.entity.description')}</td>
			<td class="co" colspan="3">
				<textarea name="entity.description"  readonly="true" cols="" rows="" id="ec_entity_edit_form_entity_description" class="cui-edit-textarea" style="width:95%;height:80px">${entity.description!}</textarea>
			</td>
		</tr>
	</table>
<form>
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
		$("#ec_entity_edit_form input[name='entity.workflowEnabled']").prop('disabled', true);
		$("#ec_entity_edit_form input[name='entity.isBase']").prop('disabled', true);
		$("#ec_entity_edit_form input[name='entity.groupEnabled']").prop('disabled', true);
		$("#ec_entity_edit_form input[name='entity.isInherentedBase']").prop('disabled', true);
		$("#entityIsControl").prop('disabled', true);
	}

	ec.entity.setAclRestrict = function(){
		if($('#entityAclRestrict').prop('checked')){
			$('input[name="entity.enableAclRestrict"]').val('true');
		}else{
			$('input[name="entity.enableAclRestrict"]').val('false');
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
	$('input,textarea').prop('disabled', true);
})();
</script>