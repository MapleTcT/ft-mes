<@errorbar id="ec_property_edit_formDialogErrorBar" />
<style>
#enumerate_content_table td {
	vertical-align: middle;
	text-align: center;
	height : 20px;
}
#enumerate_content_table thead td {
	background: url('/bap/static/css/sprite_20120525.png') 0 -1340px repeat-x;
}
#code_config_span {
	display:none;
}
#code_config_span a{
	color:blue;
}
.cui-systemcode-btn-add {
	background:url(/bap/static/foundation/images/icon-add.gif);
	background-repeat:no-repeat;
	background-size:15px 15px;
}

.cui-systemcode-btn-edit {
	background:url(/bap/static/foundation/images/icon-edit.gif);
	background-repeat:no-repeat;
	background-size:15px 15px;
}

span#systemCode_add {
	background:url(/bap/static/css/edit_20150318.png) -1px -1156px no-repeat;
	_background:url("/bap/static/css/edit_20150318.gif") -1px -1156px no-repeat;
}


a#systemCode_edit{
	margin-left:10px;
	display: inline-block;
	text-decoration:none;
}


.buttonbar-button{
	position:relative;
	height:18px;
	display:inline-block;
	padding-left:18px;
	line-height:18px;
	color:black;
	cursor:pointer;
	float:left;
}


#ec_property_edit_table  .cui-edit-field  {
		line-height:26px;
}

span#systemCode_edit{
	background:url(/bap/static/css/edit_20150318.png) -1px -1190px no-repeat;
	_background:url("/bap/static/css/edit_20150318.gif") -1px -1190px no-repeat;
}
</style>
<form id="ec_property_edit_form" name="ec_property_edit_form" action="/msService/ec/property/save" method="post" callback="ec.property.Callback" autocomplete="off" onsubmitMethod="ec.property.beforeSave();">
	<input type="hidden" name="property.version" <#if property?? && property.version??>value="${property.version}" <#else>value="0"</#if> id="ec_property_edit_form_property_version"/>
	<input type="hidden" name="property.sort" <#if property?? && property.sort??>value="${property.sort}" <#else>value="0"</#if> id="ec_property_edit_form_property_sort"/>
    <input type="hidden" name="property.model.code" <#if property?? && property.model??&& property.model.code??>value="${property.model.code}"</#if> id="ec_property_edit_form_property_model_code"/>
    <input type="hidden" name="model.enableDataAudit" <#if model?exists&&model.enableDataAudit?exists&&model.enableDataAudit== false>value="false"<#else>value=""</#if> id="ec_property_edit_form_model_enableDataAudit"/>
    <input type="hidden" name="property.entityCode" <#if property?? && property.entityCode??>value="${property.entityCode}"</#if> id="ec_property_edit_form_property_entityCode"/>
    <input type="hidden" name="property.moduleCode" <#if property?? && property.moduleCode??>value="${property.moduleCode}"</#if> id="ec_property_edit_form_property_moduleCode"/>
    <input type="hidden" name="property.code" <#if property?? && property.code??>value="${property.code}"</#if> id="ec_property_edit_form_property_code"/>
    <input type="hidden" name="property.defaultValue" <#if property?? && property.defaultValue??>value="${property.defaultValue}"</#if> id="property_defaultValue"/>
    <input type="hidden" name="property.fillcontent" <#if property?? && property.fillcontent??>value="${property.fillcontentEscapeHtml}"</#if> id="ec_property_edit_form_property_fillcontent"/>
    <input type="hidden" name="property.attributes" <#if property?? && property.attributes??>value="${property.attributesEscapeHtml}"</#if> id="ec_property_edit_form_property_attributes"/>
    <input type="hidden" name="property.isControl" <#if property?exists&&property.isControl?exists&&property.isControl== true>value="true"<#else>value="false"</#if> id="ec_property_edit_form_property_isControl"/>
    <input type="hidden" name="property.isUnique" <#if property?exists&&property.isUnique?exists&&property.isUnique== true>value="true"<#else>value="false"</#if> id="ec_property_edit_form_property_isUnique"/>
    <input type="hidden" name="property.isHidden" <#if property?exists&&property.isHidden?exists&&property.isHidden== true>value="true"<#else>value="false"</#if> id="ec_property_edit_form_property_isHidden"/>
	<input type="hidden" name="property.nullable" value="${(!((property.nullable)??) || property.nullable)?string('true', 'false')}">
	<input type="hidden" name="property.multable" value="${((property.multable)?? && property.multable)?string('true', 'false')}">
	<input type="hidden" name="property.seniorSystemCode" value="${((property.seniorSystemCode)?? && property.seniorSystemCode)?string('true', 'false')}">
	<input type="hidden" id="org_property_seniorSystemCode" value="${((property.seniorSystemCode)?? && property.seniorSystemCode)?string('true', 'false')}">
	<input type="hidden" name="property.sensitive" value="${((property.sensitive)?? && property.sensitive)?string('true', 'false')}" id="ec_property_edit_form_property_sensitive"/>
    <input type="hidden" name="property.stretch" value="${((property.stretch)?? && property.stretch)?string('true', 'false')}" id="ec_property_edit_form_property_stretch"/>
    <input type="hidden" name="property.isBussinessKey" value="${((property.isBussinessKey)?? && property.isBussinessKey)?string('true', 'false')}" id="ec_property_edit_form_property_isBussinessKey"/>
    <input type="hidden" name="property.isUsedMneCode" value="${((property.isUsedMneCode)?? && property.isUsedMneCode)?string('true', 'false')}" id="ec_property_edit_form_property_isUsedMneCode"/>
    <input type="hidden" name="property.isIndex" value="${((property.isIndex)?? && property.isIndex)?string('true', 'false')}" id="ec_property_edit_form_property_isIndex"/>
    <input type="hidden" name="property.systemcode.value" <#if property?? && property.systemcode?? && property.systemcode.value??>value="${property.systemcode.value}"</#if> id="ec_property_edit_form_property_systemcode_value"/>
    <input type="hidden" name="property.orgColumnName" <#if property?? && property.orgColumnName??>value="${property.orgColumnName}"</#if> id="ec_property_edit_form_property_orgColumnName"/>
    <input type="hidden" name="property.isGroupObject" <#if property?? && property.isGroupObject??>value="${property.isGroupObject}"</#if>  id="ec_property_edit_form_property_isGroupObject"/>
    <input type="hidden" name="property.onlyLeaf" <#if property?? && property.onlyLeaf??>value="${((property.onlyLeaf)?? && property.onlyLeaf)?string('true', 'false')}"</#if>  id="ec_property_edit_form_property_onlyLeaf"/>	<input type="hidden" id="currentCompany_type" name="currentCompany_type" value="UNIT" />
	<input type="hidden" name="property.isUsedForList" value="${(!((property.isUsedForList)??) || property.isUsedForList)?string('true', 'false')}">
	<input type="hidden" name="property.isCustom" value="${((property.isCustom)?? && property.isCustom)?string('true', 'false')}">
	<input type="hidden" name="property.isUsedForSearch" value="${((property.isUsedForSearch)?? && property.isUsedForSearch)?string('true', 'false')}">
	<input type="hidden" name="property.isIgnoreAudit" value="${((property.isIgnoreAudit)?? && property.isIgnoreAudit)?string('true', 'false')}">
	<input type="hidden" name="property.noAnalyzer" value="${((property.noAnalyzer)?? && property.noAnalyzer)?string('true', 'false')}">
	<input type="hidden" name="property.isMainAssociated" value="${((property.isMainAssociated)?? && property.isMainAssociated)?string('true', 'false')}">
	<input type="hidden" name="property.isMainDisplay" value="${((property.isMainDisplay)?? && property.isMainDisplay)?string('true', 'false')}" id="ec_property_edit_form_property_isMainDisplay"/>
    <input type="hidden" name="modelCode" <#if modelCode??>value="${modelCode}"</#if>  id="ec_property_edit_form_modelCode"/>
    <input type="hidden" name="systemcode_select" <#if systemcode_select??>value="${systemcode_select}"</#if> id="ec_property_edit_form_systemcode_select"/>
    <input type="hidden" name="property.counterRuleId" <#if property?? && property.counterRuleId??>value="${property.counterRuleId}"</#if> />
    <input type="hidden" name="property_associatedProperty_code" <#if property?? && property.associatedProperty??>value="${property.associatedProperty.code}"</#if> />
	<table class="infoTable" id="ec_property_edit_table" cellpadding="0" cellspacing="0" border="0" align="center" width="90%" style="margin-top: 5px">
		<tr style="height:0px">
			<td style="width:15%;height:0px;text-align:right" class="la"></td>
			<td class="co" style="width:35%;height:0px"></td>
			<td style="width:15%;height:0px;text-align:right" class="la"></td>
			<td class="co" style="width:35%;height:0px"></td>
		</tr>
		<tr>
			<td class="la" style="width:15%;;padding-right: 10px;color:rgb(179, 3, 3);" align="right">${getHtmlText('ec.property.name')}</td>
			<td class="co"  colspan="3" >
				<#if property?? && (property.name)?? && (property.name)!=''>
				    <input type="text" name="property.name" readonly="true" <#if property?? && property.name??>value="${property.name}" </#if>  id="ec_property_edit_form_property_name" class="cui-edit-field" style="width:98%"/>
				<#else>
					<input type="text" name="property.name" value="" id="ec_property_edit_form_property_name" class="cui-edit-field" style="width:98%"/>
				</#if>
				<br/><span class="description">${getHtmlText('ec.property.editname')}</span>
			</td>
		</tr>
		<tr>
			<td class="la" style="width:15%;;padding-right: 10px;" align="right">${getHtmlText('ec.property.columnName')}</td>
			<td class="co" colspan="3">
				<div class="fix-input" style="width:98%">							
					<div class="fix-search-click" style="background-color:#DCDCDC">
					<input type="text" name="property.columnName"  <#if property?? && property.columnName??>value="${property.columnName}" </#if>  readonly="readonly" id="ec_property_edit_form_property_columnName" class="cui-noborder-input" style="background-color:#DCDCDC"/>
					<input type="button" name="property.modifyColumnName" <#if (property.columnName)??&&(property.columnName)=='BAP_GROUP_ID'>style='display:none;'</#if> onclick="modifyColumnName();" title="点击可以修改列名" value="" class="cui-international-click" style='background:url("/bap/static/css/edit_20150318.png") -1px -1190px no-repeat transparent' >
					</div>
				</div>
				<span class="description">${getHtmlText('ec.property.columnName')}${getHtmlText('ec.property.columnnamedescription')}</span>
			</td>
		</tr>
		<tr>
			<td class="la" style="width:15%;;padding-right: 10px;color:rgb(179, 3, 3);" align="right">${getHtmlText('ec.property.displayName')}</td>
			<td class="co" colspan="3" >
				<#if property?? && property.isInherent?? && property.isInherent  >
					<@international readonly=true name="property.displayName" key=(property.displayName)!'' moduleCode=module.artifact isNew=true maxLength=80 view=true  cssStyle="width:98%"  ></@international>
				<#else >
					<@international name="property.displayName" key=(property.displayName)!'' moduleCode=module.artifact isNew=true maxLength=80 cssStyle="width:98%" ></@international>
				</#if>
				<#--
				<#if !property?exists>
					<@international name="property.displayName" moduleCode="${module.artifact!}" key="" cssClass="cui-edit-field"></@international>
				<#else>
					<@international name="property.displayName" moduleCode="${module.artifact!}" key="${property.displayName!}" cssClass="cui-edit-field"></@international>
				</#if>
				-->
				<span class="description">(${getHtmlText('ec.property.showname')})</span>
			</td>
		</tr>
		<tr>
			<td class="la" style="color:rgb(179, 3, 3);">${getHtmlText('ec.property.type')} </td>
			<td class="co">
			<#if (property.code)??>
			    <input type="hidden" name="property.type" value="${property.type}" id="property_type"/>
				<#list columnTypes?keys as columnkey>
					<#if  property?? &&  property.type = columnkey>
						${getHtmlText('${(columnTypes[columnkey])!}')}
					</#if>
				</#list>
			<#else>
			<select id="property_type" style="width: 80px;" name="property.type"  onchange="$('#property_defaultValue').val('');ec.property.typeChanged(this,'null');">
				<optgroup i18n="ec.property.basetype" label="${getText('ec.property.basetype')}">
				<#if !module?? || !(module.type)?? || (module.type) != 'Mis'>
					<#assign basetypeSize = 9>
					<#assign isMis = false>
				<#else>
					<#assign basetypeSize = 10>
					<#assign isMis = true>
				</#if>
				<#list columnTypes?keys as columnkey>
				<#if columnkey_index lte basetypeSize && columnkey!="BINARY" && (isMis || (columnkey != 'TIME'))>
				<option value="${columnkey}"
					<#if  property?? &&  property.type = columnkey>
						selected
					</#if>
				>${getText('${(columnTypes[columnkey])!}')}</option>
				</#if>
				</#list>
				</optgroup>
				<optgroup i18n="ec.property.supertype" label="${getText('ec.property.supertype')}">
				<#list columnTypes?keys as columnkey>
				<#if columnkey_index gt basetypeSize && columnkey!="ENUMERATE" && (isMis || (columnkey != 'LAYER'))>
				<option value="${columnkey}"
					<#if  property?? &&  property.type = columnkey>
						selected
					</#if>
				>${getText('${(columnTypes[columnkey])!}')}</option>
				</#if>
				</#list>
				</optgroup>
			</select>
			</#if>
			<span id="code_config_span"><a href="javascript:ec.property.config_fun();">${getHtmlText('ec.property.config')}</a></span>
			</td>
			
			<#-- 禁掉系统编码的多选-->
			<td id="baseproperty_enum_showFormat1" style="display:none;" class="la">${getText('ec.property.ismorcchoice')}</td>
			<td id="baseproperty_enum_showFormat2" style="display:none;" class="co">
				<input type="checkbox" name="property_multable" id="property_multable_cb" <#if ((property.multable)?? && property.multable)>checked="checked"</#if> value="true" <#if (property.code)?has_content>disabled<#else>onclick=clickMutableFunc()</#if> />
				<input type="checkbox" name="property_onlyLeaf" style="float:right;margin-right:30px;display:none" id="property_onlyLeaf_cb" <#if ((property.onlyLeaf)?? && property.onlyLeaf)>checked="checked"</#if> value="true" onclick=$("input[name='property.onlyLeaf']").val($(this).prop('checked')); />
				<lable id="property_onlyLeaf_lable" style="float:right;margin-right:10px;display:none">${getText('仅可选叶节点')}</label>
			</td>
			<td id="baseproperty_object_showFormat1" style="display:none;" class="la">${getText('组字段')}</td>
			<td id="baseproperty_object_showFormat2" style="display:none;" class="co">
				<input type="checkbox" name="property_groupobject" <#if (property.columnName)??&&property.columnName=='BAP_GROUP_ID'>checked="checked"</#if> onclick='ec.property.groupobject()' id="property_groupobject_cb" />
			</td>
		</tr>
		<tr id="propertyIgnore">
			<td class="la">${getHtmlText('ec.property.isIgnoreAudit')}</td>
			<td class="co"><input type="checkbox" name="property_isIgnoreAudit" onclick=$("input[name='property.isIgnoreAudit']").val($(this).prop('checked')) <#if property?? && (property.isIgnoreAudit)?? && (property.isIgnoreAudit)== true>checked="checked"</#if> value="true" />
			</td>			
		</tr>
		<tr id="seniorSystemCode" style="display:none;">
			<td class="la">${getHtmlText('ec.property.supertype')} </td>
			<td class="co">
				<input type="checkbox" name="property_seniorSystemCode" onclick="property_seniorSystemCodeClick(this)" id="property_seniorSystemCode" <#if (property?? && property.seniorSystemCode?? && property.seniorSystemCode)>checked="checked"</#if> value="true" />
			</td>
			<script type="text/javascript">
				function property_seniorSystemCodeClick(obj){
					$("input[name='property.seniorSystemCode']").val($(obj).prop('checked'));
					CUI('#property_defaultValue').val(CUI('#defaultValueSelect').val());
				}
			</script>
		</tr>
		<tr>
			<td id="filedType1_property" class="la">${getHtmlText('ec.view.cell.showType')}</td>
			<td id="filedType2_property" class="co">
				<select id="form_set_select_filedType" name="property.fieldType" style="width:80%"></select> 
			</td>
			<td id="showFormat1_property" class="la">${getHtmlText('ec.property.format')}</td>
			<td id="showFormat2_property" class="co">
				<select id="form_set_select_val_type" name="property.format" style="width:80%"></select> 
			</td>
		</tr>
		<tr id="baseproperty_enum_systemcode_1_1">
			<td class="la" width="20%" align="right">${getHtmlText('foundation.infoSetCol.systemCode')}&#160;&#160;&#160;</td>
			<td class="co" colspan="1">
				<select name="systemcode_select" id="systemcode_select" <#if property?? && (property.fillcontent)?? && property.fillcontent?has_content>disabled="disabled"</#if> onchange="_fillDefaultContent()" style="width:80%"></select>
			</td>
			<td class="la" colspan="2" style="padding-left:22px;text-align: left">
					<a  href="#"   id="systemCode_add">
						<span id="systemCode_add"  class="buttonbar-button"  href="#" onClick="foundation.systemCode.codeManage('add')"  >${getHtmlText('foundation.basic.extension.add')}</span>
					</a>
					<a  href="#"  id="systemCode_edit"  >
						<span id="systemCode_edit" class="buttonbar-button"  href="#" onClick="foundation.systemCode.codeManage('modify')" >${getHtmlText('foundation.basic.systemCode.modifyCode')}</span>
					</a>
					<a  href="#"  id="systemCode_edit"  >
						<span id="systemCode_edit" class="buttonbar-button"  href="#" onClick="foundation.systemCode.valueManage()"  >${getHtmlText('foundation.systemcode.codeValueManager')}</span>
					</a>
			</td>
		</tr>
		<tr id="baseproperty_enum_enumerate_1_1">
			<td class="la" width="20%" align="right" style="vertical-align:top;">${getHtmlText('ec.property.enum')}&#160;&#160;&#160;</td>
			<td class="co" colspan="3">
				<div style="max-height:170px;overflow:auto;">
					<table width="90%" cellspacing="1" bgcolor="#CCCCCC" id="enumerate_content_table">
						<thead>
							<tr bgcolor="#FFFFFF">
								<td width="40%" class="dg-hd-td-label">${getHtmlText('ec.property.value')}</td>
								<td width="40%" class="dg-hd-td-label">${getHtmlText('ec.property.content')}</td>
								<td width="10%" class="dg-hd-td-label">${getHtmlText('ec.property.dafult')}</td>
								<td width="10%" class="dg-hd-td-label">${getHtmlText('ec.property.delete')}</td>
							</tr>
						</thead>
						<tr bgcolor="#FFFFFF">
							<td><input type="text" class="cui-edit-field" style="width:95%" /></td>
							<td><input type="text" class="cui-edit-field" style="width:95%" /></td>
							<td><input type="radio" name="defaultValueRadio" /></td>
							<td><img src="/bap/static/foundation/images/icon-del.gif" style="cursor: pointer;" /></td>
						</tr>
					</table>
				</div>
			</td>
		</tr>
		<tr id="baseproperty_1_2">
			<td class="la" width="20%" align="right" id="default_label">${getHtmlText('ec.property.dafultValue')}&#160;&#160;&#160;</td>
			<td class="co" colspan="3" id="ec_property_default_value_td">
				<input id="defaultValueText" name="defaultValueText" <#if property?? && property.defaultValue??>value="${(property.defaultValue)!}"</#if>  class="cui-edit-field" onChange="_changeDefaultValue(this)" style="width:98%" />
				<div id="defaultValueDate" style="float: left;">
					<div style="float: left;"><input type="checkbox" name="defaultValueDate" onChange="_changeDefaultValue(this)" <#if (property.defaultValue)?? && property.defaultValue=='today'>checked="checked"</#if>  value="today" /></div><div style="float: left;">&nbsp;&nbsp;${getHtmlText('ec.view.today')}</div>
				</div>
				<div id="defaultValueTime" style="float: left;">
					<div style="float: left;"><input type="checkbox" name="defaultValueTime" onChange="_changeDefaultValue(this)"  value="currentTime" <#if (property.defaultValue)?? && property.defaultValue=='currentTime'>checked="checked"</#if> /></div><div style="float: left;">&nbsp;&nbsp;${getHtmlText('ec.property.default.currenttime')}</div>
				</div>
				<select id="defaultValueBoolean" name="defaultValueBoolean" onChange="_changeDefaultValue(this)"  style="width:33%" >
					<option value=""></option>
					<option value="true" <#if (property.defaultValue)?? && property.defaultValue=='true'>selected="selected"</#if> >${getText("common.radio.true")}</option>
					<option value="false" <#if (property.defaultValue)?? && property.defaultValue=='false'>selected="selected"</#if>>${getText("common.radio.false")}</option>
				</select>
				<select id="defaultValueSelect" name="defaultValueSelect" onChange="_changeDefaultValue(this)"  style="width:98%" ></select>
				<script type="text/javascript">
					function _changeDefaultValue(obj){
						if(obj.type=='checkbox' && obj.checked==false){
							if(obj.name=='defaultValueBoolean'){
								CUI('#property_defaultValue').val('false');
							}else{
								CUI('#property_defaultValue').val('');
							}
						}else{
							CUI('#property_defaultValue').val(CUI(obj).val());
						}
						
					}
					function _fillDefaultContent(){
						var type = CUI('#property_type').val();
						CUI('*[id^="defaultValue"]').hide();
						if(type==='BINARY'){
							$('#default_label').hide();
							$('#baseproperty_1_2').hide();
							return;
						}else{
							$('#default_label').show();
							$('#baseproperty_1_2').show();
						}
						if('DATE' == type || 'DATETIME'==type || 'BOOLEAN'==type || 'TIME'==type) {
							if('DATE' == type){
								CUI('#defaultValueDate').show();
								CUI('#defaultValueTime').hide();
								CUI('#defaultValueBoolean').hide();
							}else if('DATETIME'==type || 'TIME'==type){
								CUI('#defaultValueDate').hide();
								CUI('#defaultValueTime').show();
								CUI('#defaultValueBoolean').hide();
							}else if('BOOLEAN'==type){
								CUI('#defaultValueDate').hide();
								CUI('#defaultValueTime').hide();
								CUI('#defaultValueBoolean').show();
							}
							CUI('#defaultValueDate input').each(function(i){
								if(CUI(this).val() ==CUI('#property_defaultValue').val()) {
									CUI(this).prop('checked', true);
								} 
							});
						} else if('SYSTEMCODE' == type) {
							var objSelect = CUI('#defaultValueSelect');
							objSelect.show();
							objSelect.empty();
							CUI.ajax({
				    			url: "/msService/ec/systemCode/systemCodeJson",
				    			type: 'post',
				    			async: false,
				    			data: {systemEntityCode:CUI('#systemcode_select').val()},
				    			success: function(msg) {
				    				var objOption=document.createElement("option");
									CUI(objOption).appendTo(objSelect);
				    				CUI.each(msg, function(key,value){
										objOption=document.createElement("option");
										objOption.innerHTML = value;
										objOption.value = key;
										CUI(objOption).appendTo(objSelect);
									});
				    			}
				    		});
				    		var listType = systemCodeTypeArr[CUI('#systemcode_select').val()];
				    		if(listType!=undefined && listType == 'TREE'){
				    			//$('td[id^="baseproperty_enum_showFormat"]').hide();
				    		} else if(listType!=undefined && listType == 'LIST') {
				    			//$('td[id^="baseproperty_enum_showFormat"]').show();
				    		}
				    		<#if property?? && property.seniorSystemCode?? && property.seniorSystemCode>
				    			<#assign systemEntityCode = "">
				    			<#list property.fillcontentJson?keys as key>
									<#if key == 'fillContent'>
										<#assign systemEntityCode = property.fillcontentJson[key]>
									</#if>
								</#list>
							CUI('#defaultValueSelect').val('${systemEntityCode}/' + CUI('#property_defaultValue').val());
				    		<#else>
							CUI('#defaultValueSelect').val(CUI('#property_defaultValue').val());
				    		</#if>
							CUI('#defaultValueSelect').show();
							clickMutableFunc();
						} else if('ENUMERATE' == type) {
							if(CUI('#ec_property_edit_form_property_fillcontent').val() != '') {
								var fill = CUI.parseJSON(CUI('#ec_property_edit_form_property_fillcontent').val());
								var objSelect = CUI('#defaultValueSelect');
								objSelect.show();
								objSelect.empty();
								CUI.each(fill.fillContent, function(key,value){
									var objOption=document.createElement("option");
									objOption.innerHTML = value;
									objOption.value = key;
									CUI(objOption).appendTo(objSelect);
									CUI('#defaultValueSelect').val(CUI('#property_defaultValue').val());
								});
							}
							CUI('#defaultValueSelect').show();
							clickMutableFunc();
						} else {
							if('TEXT'===type || 'LONGTEXT'===type || 'PASSWORD'===type || 'BAPCODE'===type ||'SUMMARY'===type){
								<#if property?? && property.code?? && property.maxLength??>
									$('#ec_property_edit_form_property_maxLength').val(${property.maxLength!});
								<#else>
									//$('#ec_property_edit_form_property_maxLength').val(256);
									$('#ec_property_edit_form_property_maxLength').val('');
								</#if>
							}else{
								$('#ec_property_edit_form_property_maxLength').val('');
							}
							CUI('#defaultValueText').val(CUI('#property_defaultValue').val());
							CUI('#defaultValueText').show();
							if('PICTURE'===type){
							<#if property?? && property.code?? && property.picWidth??>
                                $('#ec_property_edit_form_property_picWidth').val(${property.picWidth!});
                            </#if>
                            <#if property?? && property.code?? && property.picHeight??>
                                $('#ec_property_edit_form_property_picHeight').val(${property.picHeight!});
                            </#if>
							}
						}
					}
					function clickMutableFunc(){
						$("input[name='property.multable']").val($("#property_multable_cb").prop('checked'));
						var listType=systemCodeTypeArr[CUI('#systemcode_select').val()];
						if((listType=="TREE"||listType=="tree")&&(!$("#property_multable_cb").prop('checked'))){
							CUI('#property_onlyLeaf_cb').show();
							CUI('#property_onlyLeaf_lable').show();
						}else{
							CUI('#property_onlyLeaf_cb').hide();
							CUI('#property_onlyLeaf_lable').hide();
						}
					}
				</script>
			</td>
		</tr>
		<tr id="baseproperty_3">
			<td class="la">${getHtmlText('ec.property.isUnique')} </td>
			<td class="co"><input type="checkbox" name="property_isUnique" onclick=$("input[name='property.isUnique']").val($(this).prop('checked')) <#if property?exists&&property.isUnique== true>checked="checked"</#if>  value="true"/></td>
			<td class="la">${getHtmlText('ec.property.isUsedForList')}</td>
			<td class="co"><input type="checkbox" name="property_isUsedForList" onclick=$("input[name='property.isUsedForList']").val($(this).prop('checked')) <#if property?exists&&property.isUsedForList== false><#else>checked="checked"</#if> value="true" cssClass="cui-edit-field" /></td>
		</tr>
		<tr id="indexNullable">
			<td class="la">${getHtmlText('ec.property.isIndex')} </td>
			<td class="co"><input type="checkbox" name="property_isIndex" onclick=$("input[name='property.isIndex']").val($(this).prop('checked')) <#if property?exists&&property.isIndex== true>checked="checked"</#if>   value="true"/></td>
			<td class="la">${getHtmlText('ec.property.nullable')} </td>
			<td class="co">
				<input type="checkbox" name="property_nullable" onclick=$("input[name='property.nullable']").val($(this).prop('checked')) id="property_nullable_cb" <#if !property?? || (property?? && property.nullable)>checked="checked"</#if> value="true" />
			</td>
		</tr>
		<tr id="mneControl">
			<td class="la" id="usedMneCodeTd">${getHtmlText('ec.property.isUsedMneCode')} </td>
			<td class="co" id="isUsedMneCodeTd"><input type="checkbox" name="property_isUsedMneCode" onclick="ec.property.checkUseMneCode()" <#if (property.isUsedMneCode)?? && property.isUsedMneCode>checked="checked"</#if> /></td>
		</tr>
		<tr id="baseproperty_hidden_base">
			<td class="la" id="hiddenTd">${getHtmlText('ec.property.isHidden')} </td>
			<td class="co" id="isHiddenTd"><input type="checkbox" name="property_isHidden" onclick=$("input[name='property.isHidden']").val($(this).prop('checked')) <#if (property.isHidden)?? && property.isHidden>checked="checked"</#if> <#if property?? && (property.name=="extraCol"||property.name=="id"||property.name=="version"||property.name=="groupId"||property.name=="positionLayRec"||property.name=="tableInfoId")>disabled="disabled"</#if> /><span class="description">(${getHtmlText('ec.property.isHiddenExplain')})</span></td>
		</tr>
		<tr id="baseproperty_base_sen">
			<td class="la">${getHtmlText('ec.property.sensitive')}</td>
			<td class="co"><input type="checkbox" name="property_sensitive" onclick=$("input[name='property.sensitive']").val($(this).prop('checked')) id="property_sensitive_cb" <#if property?? && property.sensitive??&& property.sensitive>checked="checked"</#if> value="true" /></td>
			<td class="la" style="display:none;" id="businessKey1">${getHtmlText('ec.property.businessKey')}</td>
			<td class="co" style="display:none;" id="businessKey2"><input type="checkbox" name="property_isBussinessKey" id="property_isBussinessKey_cb" <#if property?? && property.isBussinessKey>checked="checked"</#if> value="true" /></td>
		</tr>
		<tr id="baseproperty_base_5">
			<td class="la">${getHtmlText('ec.property.maxLength')}</td>
			<td class="co"><input type="text" name="property.maxLength"  <#if property?? && (property.maxLength)??>value="${property.maxLength}"</#if>  id="ec_property_edit_form_property_maxLength" class="cui-edit-field" onchange="ec.property.checkLengthIslarger(this)"/></td>
			<td class="la" style="color:rgb(179, 3, 3);">${getHtmlText('ec.property.decimalNum')}</td>
			<td class="co"><input type="text" name="property.decimalNum" <#if property?? && (property.decimalNum)??>value="${property.decimalNum}"</#if>  id="ec_property_edit_form_property_decimalNum" class="cui-edit-field"/></td>
			<td class="la">${getHtmlText('ec.property.isMainDisplay')} </td>
			<td class="co"><input type="checkbox" name="property_isMainDisplay" id="property_isMainDisplay_cb" <#if property?? && (property.isMainDisplay)?? && (property.isMainDisplay)== true>checked="checked"</#if>   value="true"/></td>
		</tr>
		<!-- 如果是微服务模块，禁用全文搜索 -->
		<#if !module?? || !(module.type)?? || (module.type) != 'Mis'>
			<tr id="baseproperty_base_7">
				<td class="la">${getHtmlText('ec.property.isUsedForSearch')}</td>
				<td class="co"><input type="checkbox" name="property_isUsedForSearch" onclick=$("input[name='property.isUsedForSearch']").val($(this).prop('checked')) <#if property?? && (property.isUsedForSearch)?? && (property.isUsedForSearch)== true>checked="checked"</#if> value="true" /></td>
				<td class="la analyzer-class">${getHtmlText('ec.property.noAnalyzer')}</td>
				<td class="co analyzer-class"><input type="checkbox" name="property_noAnalyzer" onclick=$("input[name='property.noAnalyzer']").val($(this).prop('checked')) <#if property?? && (property.noAnalyzer)?? && (property.noAnalyzer)== true>checked="checked"</#if> value="false" /></td>
			</tr>
		</#if>
		
		<tr id="associated_1" style="display:none;">
			<td class="la" width="20%" align="right">${getHtmlText('ec.property.choicefield')}</td>
			<td class="co" width="80%" colspan="3">
			<select id="ec_select_module" onchange="ec.ass.selectChange('entity',this,'module.code')" style="width:30%" />
			&#160;<select id="ec_select_entity" onchange="ec.ass.selectChange('model',this,'entity.code')" style="width:30%"></select>
			&#160;<select id="ec_select_model" onchange="ec.ass.selectChange('property',this,'model.code')" style="width:30%" name="ai.targetProperty.code"></select>
			&#160;<select name="property.associatedProperty.code" id="ec_select_property" style="width:30%;"></select>
			</td>
		</tr>
		<#--<tr id="default_associated_1" style="display:none;">
			<td class="la" width="20%" align="right">${getText('ec.property.dafult')}${getText('ec.property.value')}&#160;&#160;&#160;</td>
			<td class="co" colspan="3" id="ec_property_default_value_td_1">
				<div id="defaultValueUser" style="float: left;">
					<div style="float: left;"><input type="checkbox" name="defaultValueUser" onChange="_changeDefaultValue(this)"  value="currentUser" /></div><div style="float: left;">&nbsp;&nbsp;${getText('登录人')}</div>
				</div>
			</td>
		</tr>-->
		<tr id="associated_2" style="display:none;">
			<td class="la" width="20%" align="right">${getHtmlText('ec.property.associateType')}</td>
			<td class="co">
				<select name="property.associatedType" cssClass="cui-edit-field" cssStyle="width:100%">
					<option value="1" <#if property?? && property.associatedType?? && property.associatedType=1>selected</#if>>${getText('ec.property.one2one')}</option>
					<option value="2" <#if property?? && property.associatedType?? && property.associatedType=2>selected</#if>>${getText('ec.property.many2one')}</option>
					<#--
					<option value="3" <#if property?? && property.associatedType?? && property.associatedType=3>selected</#if>>${getText('ec.property.one2many')}</option>
					<option value="4" <#if property?? && property.associatedType?? && property.associatedType=4>selected</#if>>${getText('ec.property.many2many')}</option>
					-->
				</select>
			</td>
			<td class="la isMainAssociated" align="right">${getHtmlText('ec.property.isMainAssociated')}</td>
			<td class="co isMainAssociated">
				<input type="checkbox" name="property_isMainAssociated" onclick=$("input[name='property.isMainAssociated']").val($(this).prop('checked')) <#if property?? && (property.isMainAssociated)?? && property.isMainAssociated>checked="checked"</#if> />
			</td>
		</tr>
		<tr id="associated_3" style="display:none;">
			<td class="la" width="20%" align="right">${getHtmlText('ec.property.fetchMode')}</td>
			<td class="co">
				<select name="property.fetchMode" cssClass="cui-edit-field" cssStyle="width:100%">
					<option value=""></option>
					<option value="SELECT" <#if property?? && property.fetchMode?? && property.fetchMode='SELECT'>selected</#if>>${getText('ec.property.fetchMode.SELECT')}</option>
					<option value="JOIN" <#if property?? && property.fetchMode?? && property.fetchMode='JOIN'>selected</#if>>${getText('ec.property.fetchMode.JOIN')}</option>
					<#--<option value="SUBSELECT" <#if property?? && property.fetchMode?? && property.fetchMode='SUBSELECT'>selected</#if>>${getText('ec.property.fetchMode.SUBSELECT')}</option>-->
				</select>
				<span id="callBackHelpinfo" class="baphelp-icon"></span>
					<script type="text/javascript">
					$(function(){
								var cbElm = $('#callBackHelpinfo');
								var cbIns;
								cbElm.on('click', function(){
									cbIns.setContent( "${getHtmlText('ec.property.queryPrompt')}" );
								});				
								cbIns = cbElm.helptip({
									html: true , 
									isCustom :false, 
									width: 200 , 
									title :"说明"
								}).data('cui.tooltip');
							});	
						</script>	
			</td>
		</tr>
		<tr id="picSize_1" style="display:none;">
			<td class="la">${getHtmlText('ec.property.stretch')}</td>
			<td class="co"><input type="checkbox" name="property_stretch" onclick="ec.property.isStretch()" id="property_stretch" <#if property?? && property.stretch?? && property.stretch>checked="checked"</#if> value="true" /></td>
		</tr>
		<tr id="picSize" style="display:none;">
			<td class="la">${getHtmlText('ec.property.width')}</td>
			<td class="co"><input type="text" name="property.picWidth" style="width:80%" value="" id="ec_property_edit_form_property_picWidth" class="cui-edit-field"/></td>
			<td class="la">${getHtmlText('ec.property.height')}</td>
			<td class="co"><input type="text" name="property.picHeight" style="width:80%" value="" id="ec_property_edit_form_property_picHeight" class="cui-edit-field"/></td>
		</tr>
		<tr id="baseproperty_base_6">
			<td class="la">${getHtmlText('ec.property.description')}</td>
			<td class="co" colspan="3"><textarea name="property.description" cols="" rows="" id="ec_property_edit_form_property_description" class="editTextarea cui-edit-textarea" style="width:97%;height:60px"><#if property?? && property.description??>${property.description} </#if></textarea></td>
		</tr>
	</table>
</form>
<div id="form_set_select_val" style="display:none;">
	<table class="infoTable" cellpadding="0" cellspacing="0" border="0" width="95%" align="left" style="margin-top: 10px">
		<tr>
			<td class="la" width="20%" align="right">${getHtmlText('ec.view.type')}&#160;&#160;&#160;</td>
			<td class="co" width="30%">
				<label id="form_set_select_type_name"></label>&#160;&#160;&#160;
			</td>
			<td class="la" width="20%" align="right">&#160;&#160;&#160;</td>
			<td class="co" width="30%"></td>
		</tr>
		<tr>
			<td class="la" align="right" id="form_set_select_val_sou_name"></td>
			<td class="co" colspan="3" id="form_set_select_val_sou"></td>
		</tr>
	</table>
</div>
<script type="text/javascript">
(function(){
	systemCodeTypeArr = new Object();
	//注册命名空间
	CUI.ns("ec.model","ec.module","ec.ass");
	/* 编码规则验证
	 * @method ec.property.nameValidate
	 * @public
	 */
	ec.property.nameValidate=function(){
	 	var validate=/^[a-z]{2}[a-zA-Z0-9]*$/; 
	 	var obj = $("#ec_property_edit_form input[name='property.name']").val();
	 	$("#ec_property_edit_form input[name='property.name']").val(obj.trim());
		if (validate.test($("#ec_property_edit_form input[name='property.name']").val())){
			return true;
		}else{
		    ec_property_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.property.formatmessage')}");
			return false
		}
	};
	
	ec.property.isStretch = function(){
		$("input[name='property.stretch']").val($("#property_stretch").prop('checked'));
		if($("#property_stretch").prop('checked')){
			//$('#picSize').hide();
			$("input[name='property.picWidth']").attr("disabled","disabled");
			$("input[name='property.picHeight']").attr("disabled","disabled");
		}else{
			$("input[name='property.picWidth']").removeAttr("disabled");
			$("input[name='property.picHeight']").removeAttr("disabled");
		}
	}
	
	/**
	 * hide与show
	 *
	 */
	ec.property.hideAndShow = function(hideFields, showFields){
	    if(hideFields) {
		    $.each(hideFields, function(i, item){
		    	$('input[name="' + item + '"]').val('');
		    	if($('input[name="' + item + '"]').attr('type') == 'hidden') {
					$('input[name="' + item.replace(/\./ig, '_', 'r') + '"]').parent().hide();
					$('input[name="' + item.replace(/\./ig, '_', 'r') + '"]').parent().prev().hide();
		    	} else {
					$('input[name="' + item + '"]').parent().hide();
					$('input[name="' + item + '"]').parent().prev().hide();
		    	}
		    });
	    }
	    if(showFields) {
		    $.each(showFields, function(i, item){
				if($('input[name="' + item + '"]').attr('type') == 'hidden') {
					$('input[name="' + item.replace(/\./ig, '_', 'r') + '"]').parent().show();
					$('input[name="' + item.replace(/\./ig, '_', 'r') + '"]').parent().prev().show();
		    	} else {
					$('input[name="' + item + '"]').parent().show();
					$('input[name="' + item + '"]').parent().prev().show();
		    	}
		    });
	    }
	};
	 /* 编码规则验证
	 * @method ec.property.nameValidate
	 * @public
	 */
	ec.property.typeChanged=function(obj,callback){
		_fillDefaultContent();
		
		ec.property.addSelectShowType($('#form_set_select_filedType').get(0),CUI(obj).val(),${fieldTypesJson});		
		ec.property.addFieldFormat($('#form_set_select_val_type').get(0),CUI(obj).val(),${formatsJson},$('#form_set_select_filedType').val());

		if(callback!=undefined && callback==='null'){
			$("input[name='property_isUnique']").prop('checked',false);
	        $("input[name='property.isUnique']").val(false);
	        $("input[name='property_isUnique']").prop('disabled',false);
	        $("input[name='property_isHidden']").prop('checked',false);
	        $("input[name='property.isHidden']").val(false);
	        $("input[name='property_isHidden']").prop('disabled',false);
	        $("input[name='property_nullable']").prop('checked',true);
	        $("input[name='property.nullable']").val(true);
	        $("input[name='property_nullable']").prop('disabled',false);
	        $("input[name='property_isIndex']").prop('checked',false);
	        $("input[name='property.isIndex']").val(false);
	        $("input[name='property_isIndex']").prop('disabled',false);
	        $("input[name='property_isBussinessKey']").prop('checked',false);
	        $("input[name='property.isBussinessKey']").val(false);
		}
		if('BAPCODE' == CUI(obj).val() || 'SUMMARY' == CUI(obj).val()) {
			$('#code_config_span').show();
		} else {
			$('#code_config_span').hide();
		}
		if('LONGTEXT' == CUI(obj).val()){
		    $("input[name='property_isUnique']").prop('disabled',true);
		    $("input[name='property_isUnique']").prop('checked',false);
		}else{
		    $("input[name='property_isUnique']").prop('disabled',false);
		}
	 	if(obj && CUI(obj).val()) {
	 		$('#seniorSystemCode').hide();
	 		<#if !(property.code)??>//用property.code来判断是否为新建字段
	 		$("input[name='property_isUsedForList']").prop('disabled',false);
	 		$("input[name='property_isUsedForList']").prop('checked',"checked");
	 		$("input[name='property_isUsedForList']").val("true");
	 		</#if>
	 		CUI('*[id="indexNullable"]').show();
	 		if('TEXT' == CUI(obj).val() || 'BAPCODE' == CUI(obj).val() || 'SUMMARY' == CUI(obj).val() || 'INTEGER' == CUI(obj).val() || 'LONG' == CUI(obj).val()) {
	 			CUI('*[id="usedMneCodeTd"]').show();
	 			CUI('*[id="isUsedMneCodeTd"]').show();
	 			$("#ec_property_edit_table input[name='property.isUsedMneCode']").attr('disabled', false);
	 			$('#businessKey1').show();
				$('#businessKey2').show();
	 		} else {
	 			CUI('*[id="usedMneCodeTd"]').hide();
	 			CUI('*[id="isUsedMneCodeTd"]').hide();
	 			$("#ec_property_edit_table input[name='property.isUsedMneCode']").attr('disabled', true);
	 			$('#businessKey1').hide();
				$('#businessKey2').hide();
	 		}
	 		if('OBJECT'==CUI(obj).val()) {
	 			CUI('*[id^="baseproperty_enum"]').hide();
				$.ajax({
					async : false,
					type : "POST",
					url : "/msService/ec/module/list-select?module.code=${module.code!}",
					success : function(msg){

			 			CUI('*[id^="baseproperty"]').hide();
			 			CUI('*[id^="baseproperty_hidden"]').show();
			 			CUI('*[id^="associated"]').show();
			 			<#if model?? && model.isMain == true>
			 			CUI('*[id^="associated"] .isMainAssociated').hide();
			 			</#if>
			 			<#if property?? && property.model?? && property.model.isMain == true>
			 			CUI('*[id^="associated"] .isMainAssociated').hide();
			 			</#if>
						$("#baseproperty_base_6").show();
						var se = $('#ec_select_module')[0];
						$('#ec_select_module').empty();
						se.options.add(new Option('--',''));
						for(var i = 0 ; i < msg.length ; i ++){
							//if(msg[i].code == 'sysbase_1.0' || msg[i].code == '${module.code!}') {
								se.options.add(new Option(msg[i].nameInternational,msg[i].code));
							//}
						}
						if (callback && callback != 'null') {
							CUI(obj).each(callback);
						}
					}
				});
				$('#picSize').hide();
				$('#picSize_1').hide();
				ec.property.hideAndShow(['property.decimalNum','property.maxLength'],[]);
				if(${entity.groupEnabled?c}){
	 				CUI('*[id^="baseproperty_object"]').show();
	 			}else{
	 				CUI('*[id^="baseproperty_object"]').hide();
	 			}
	 		} else if('INTEGER'==CUI(obj).val() || 'LONG'==CUI(obj).val() || 'BOOLEAN'==CUI(obj).val() || 'DATE'==CUI(obj).val() || 'TIME'==CUI(obj).val() || 'DATETIME'==CUI(obj).val() || 'BINARY'==CUI(obj).val()) {
	 			CUI('*[id^="associated"]').hide();
	 			CUI('*[id^="baseproperty"]').show();
	 			if('BINARY'===CUI(obj).val()){
	 				$('#baseproperty_1_2').hide();
	 			}
	 			CUI('*[id^="baseproperty_enum"]').hide();
	 			CUI('*[id^="baseproperty_object"]').hide();
				ec.property.hideAndShow(['property.decimalNum','property.maxLength','property.isMainDisplay'], []);
				$('#picSize').hide();
				$('#picSize_1').hide();
	 		} else if('DECIMAL'==CUI(obj).val() || 'MONEY'==CUI(obj).val()) {
	 			CUI('*[id^="associated"]').hide();
	 			CUI('*[id^="baseproperty"]').show();
	 			CUI('*[id^="baseproperty_enum"]').hide();
	 			CUI('*[id^="baseproperty_object"]').hide();
				ec.property.hideAndShow(['property.maxLength','property.isMainDisplay'], ['property.decimalNum']);
				$('#picSize').hide();
				$('#picSize_1').hide();
	 		} else if('BAPCODE'==CUI(obj).val() || 'TEXT'==CUI(obj).val() || 'LONGTEXT'==CUI(obj).val()) {
	 			CUI('*[id^="associated"]').hide();
	 			CUI('*[id^="baseproperty"]').show();
	 			CUI('*[id^="baseproperty_enum"]').hide();
	 			CUI('*[id^="baseproperty_object"]').hide();
				ec.property.hideAndShow(['property.decimalNum'], ['property.maxLength', 'property.isMainDisplay']);
				$('#picSize').hide();
				$('#picSize_1').hide();
	 		} else if('SUMMARY'==CUI(obj).val()) {
	 			CUI('*[id^="associated"]').hide();
	 			CUI('*[id^="baseproperty"]').show();
	 			CUI('*[id^="baseproperty_enum"]').hide();
	 			CUI('#baseproperty_base_7').hide();
	 			CUI('#baseproperty_base_sen').hide();
	 			CUI('*[id^="baseproperty_object"]').hide();
	 			CUI('#mneControl').hide();
				ec.property.hideAndShow(['property.decimalNum'], ['property.maxLength', 'property.isMainDisplay']);
				$('#picSize').hide();
				$('#picSize_1').hide();
	 		} else if('SYSTEMCODE'==CUI(obj).val() || 'ENUMERATE'==CUI(obj).val()){
	 			CUI('*[id^="associated"]').hide();
	 			CUI('*[id^="baseproperty_object"]').hide();
	 			CUI('*[id^="baseproperty"]').show();
	 			CUI('#baseproperty_base_7').hide();
	 			CUI('#baseproperty_base_sen').hide();
	 			CUI('#mneControl').hide();
	 			CUI('*[id^="baseproperty_base"]').hide();
	 			CUI('#property_type').val(CUI(obj).val());
	 			if('SYSTEMCODE'==CUI(obj).val()){
	 				CUI('*[id^="baseproperty_enum_enumerate"]').hide();
	 				CUI('*[id^="baseproperty_object"]').hide();
	 				ec.property.createSystemCodeSelect();
	 			} else {
	 				// ec.property.sortable();
	 				CUI('*[id^="baseproperty_object"]').hide();
	 				CUI('*[id^="baseproperty_enum_systemcode"]').hide();
	 				CUI('#baseproperty_1_2').hide();
	 			}

				ec.property.hideAndShow(['property.decimalNum','property.maxLength','property.isMainDisplay'], []);
				$('#picSize').hide();
				$('#picSize_1').hide();
				$('#seniorSystemCode').show();
			}else if("PROPERTYATTACHMENT"==CUI(obj).val()){
				CUI('*[id^="baseproperty"]').hide();
				CUI('*[id^="associated"]').hide();
	 			CUI('*[id^="baseproperty_enum"]').hide();
				ec.property.hideAndShow(['property.decimalNum'], ['property.maxLength', 'property.isMainDisplay']);
				CUI('[id="baseproperty_3"]').show();
				$("input[name='property_isUnique']").prop('disabled',true);
				$("input[name='property_isIndex']").prop('disabled',true);
				$("input[name='property_isControl']").prop('disabled',true);
				$('#picSize').hide();
				$('#picSize_1').hide();
	 		} else if("OFFICE"==CUI(obj).val() || "TAGNUMBER"==CUI(obj).val() || "COLOR"==CUI(obj).val()){
				CUI('*[id^="baseproperty"]').hide();
				CUI('*[id^="associated"]').hide();
	 			CUI('*[id^="baseproperty_enum"]').hide();
				CUI('*[id="indexNullable"]').hide();
	 			CUI('*[id="mneControl"]').hide();
	 			$('#picSize').hide();
	 			$('#picSize_1').hide();
	 			CUI('*[id^="baseproperty_object"]').hide();
	 		} else if("PICTURE"==CUI(obj).val()){
	 			CUI('*[id^="baseproperty_object"]').hide();
	 			CUI('*[id^="baseproperty"]').hide();
				CUI('*[id^="associated"]').hide();
	 			CUI('*[id^="baseproperty_enum"]').hide();
				ec.property.hideAndShow(['property.decimalNum'], ['property.maxLength', 'property.isMainDisplay']);
				CUI('[id="baseproperty_3"]').show();
				$("input[name='property_isUnique']").prop('disabled',true);
				$("input[name='property_isIndex']").prop('disabled',true);
				$("input[name='property_isControl']").prop('disabled',true);
				$("input[name='property_isUsedForList']").removeAttr("checked");
				$("input[name='property_isUsedForList']").prop('disabled',true);
				$("input[name='property_isUsedForList']").val(false);
				$("input[name='property_nullable']").prop('disabled',true);
				$('#picSize').show();
				$('#picSize_1').show();
				if($("#property_stretch").prop('checked')){
					$("input[name='property.picWidth']").attr("disabled","disabled");
					$("input[name='property.picHeight']").attr("disabled","disabled");
				}else{
					$("input[name='property.picWidth']").removeAttr("disabled");
					$("input[name='property.picHeight']").removeAttr("disabled");
				}
	 		} else if("LAYER"==CUI(obj).val()){
	 			CUI('*[id^="baseproperty_object"]').hide();
	 			CUI('*[id^="baseproperty"]').hide();
				CUI('*[id^="associated"]').hide();
	 			CUI('*[id^="baseproperty_enum"]').hide();
				ec.property.hideAndShow(['property.decimalNum'], ['property.maxLength', 'property.isMainDisplay']);
				CUI('[id="baseproperty_3"]').show();
				$("input[name='property_isUnique']").prop('disabled',true);
				$("input[name='property_isIndex']").prop('disabled',true);
				$("input[name='property_isControl']").prop('disabled',true);
				$("input[name='property_isUsedForList']").removeAttr("checked");
				$("input[name='property_isUsedForList']").prop('disabled',true);
				$("input[name='property_isUsedForList']").val(false);
	 		} else {
	 			CUI('*[id^="associated"]').hide();
	 			CUI('*[id^="baseproperty"]').show();
	 			CUI('*[id^="baseproperty_enum"]').hide();
	 			CUI('*[id^="baseproperty_object"]').hide();
	 			
				ec.property.hideAndShow(['property.decimalNum'], ['property.maxLength', 'property.isMainDisplay']);
				$('#picSize').hide();
				$('#picSize_1').hide();
	 		}
	 		if('TEXT'==CUI(obj).val()) {
 				CUI('.analyzer-class').show();
 			} else {
 				CUI('.analyzer-class').hide();
 			}
		}
	};
	 /* 提交前数据准备
	 * @method ec.property.beforeSave
	 * @public
	 */
	ec.property.beforeSave = function(){
		var description = $('textarea[name="property.description"]').val()
		if(description.length >= 255){
			ec_property_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.property.description.maxLength')}");
			return false;
		}
		// ec_property_edit_form_property_fillcontent
		// property_defaultValue
		var fill = '{"fillType":';
		var property_type = CUI('#property_type').val();
		
		if($('[name="property.displayName"]').val()==null || $('[name="property.displayName"]').val()==""){
			ec_property_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.property.displayname.isnotnull')}");
			return false;
		}
		
		if('TEXT' == property_type){
			if($('#property_isBussinessKey_cb').prop('checked')===true && $('#ec_property_edit_form_property_maxLength').val() == ''){
				ec_property_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.property.char.maxLength.notnull')}");
				return false;
			}
			if($('#ec_property_edit_form_property_maxLength').val() != '' && !isDecimal($('#ec_property_edit_form_property_maxLength').val())){
				ec_property_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.property.char.maxLength.valid')}");
				return false;
			}
			if($('#ec_property_edit_form_property_maxLength').val() > 2000){
				ec_property_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.property.char.maxLength')}");
				return false;
			}
		}
      /*if(property_type=='TEXT' || property_type=='LONGTEXT'){
			var maxL = $('#ec_property_edit_form_property_maxLength').val();
			if(maxL==null || maxL == ""){
				ec_property_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.property.maxLength.empty')}");
				return false;
			} else if(!isInteger(maxL) || parseInt(maxL)<=0){
				ec_property_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.property.maxLength.integer')}");
				return false;
			}
		}  */
		
		if(property_type == 'DECIMAL'||property_type == 'MONEY'){
			if(CUI('#property_defaultValue').val()!=null && CUI('#property_defaultValue').val()!='' && !isDecimal(CUI('#property_defaultValue').val())){
				CUI.Dialog.alert("${getHtmlText('The default value should be a Decimal!')}");
				return false;
			}
			var decValue = CUI('input[name="property.decimalNum"]').val();
			if(null == decValue || decValue==""){
				ec_property_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.property.decimal.empty')}");
				return false;
			}else if(!isInteger(decValue) || parseInt(decValue)<=0){
				ec_property_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.decimal.must.integer')}");
				return false;
			} else if(isInteger(decValue) && parseInt(decValue) > 6){
				ec_property_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.decimal.maxValue','6')}");
				return false;
			}
		}
		
		if(property_type == 'INTEGER'){
			if(CUI('#property_defaultValue').val()!=null && CUI('#property_defaultValue').val()!='' &&!isInteger(CUI('#property_defaultValue').val())){
				CUI.Dialog.alert("${getHtmlText('The default value should be a Integer!')}");
				return false;
			}
		}
		if('DECIMAL'!=property_type  &&  'MONEY'!=property_type) {
			ec.property.hideAndShow(['property.decimalNum'],[]);
		}
		if(property_type == 'SYSTEMCODE' || property_type == 'ENUMERATE') {
			if(property_type == 'SYSTEMCODE') {
				var orgType = $('#org_property_seniorSystemCode').val();
				var systemCodeType = $('input[name="property.seniorSystemCode"]').val();
				if(null != orgType && orgType!="" && systemCodeType != orgType ){
					var flag = false;
					if(confirm('系统编码使用方式已变更，需手动处理旧数据，是否继续？')){
						flag = true;
					}
					if(!flag){
						return false;
					}
				}
				fill += '"3","fillName":"${getText('foundation.infoSetCol.systemCode')}","fillContent":"' + CUI('#systemcode_select').val() + '"}';
			} else if(property_type == 'ENUMERATE') {
				fill += '"4","fillName":"${getText('ec.property.enum')}","fillContent":{';
				var fillOrder = "";
				var content = "";
				var key = "";
				var value = "";
				var inputBoxs = "";
				CUI('#property_defaultValue').val(',');
				$('tr:gt(0)', $('#enumerate_content_table')).each(function(){
					inputBoxs = CUI('input', this);
					key = $.trim(inputBoxs[0].value);
					value = inputBoxs[1].value;
					if(key == '') {
						return true;
					}
					if(content != '') {
						content += ",";
						fillOrder += ",";
					}
					content += ('"' + key + '":"' + value + '"');
					fillOrder += key;
					if(inputBoxs[2].checked) {
						CUI('#property_defaultValue').val(CUI('#property_defaultValue').val() + key + ',');
					}
				});
				fill += (content + '},"fillOrder":"' + fillOrder);
				fill += '"}';
			}
			CUI('#ec_property_edit_form_property_fillcontent').val(fill);
		}
	};
	/**
	 * 枚举类型回显
	 * 
	 * @returns
	 */
	ec.property.displayEnumerate = function() {
		var fill = CUI('#ec_property_edit_form_property_fillcontent').val();
		if(fill!=null&&fill!='null'&&fill!=undefined&&fill!='') {
			fill = jQuery.parseJSON(fill);
		} else {
			return;
		}
		var type = $('#property_type').val();
		var objValue = fill['fillContent'];
		if(objValue!=null&&objValue!='null'&&objValue!=undefined&&objValue!='') {
			CUI.each(objValue, function(key, value){
				var currRow = CUI('#enumerate_content_table tr').last();
				CUI('input:eq(0)', currRow).val(key);
				CUI('input:eq(1)', currRow).val(value);
				if(CUI('#property_defaultValue').val().indexOf(','+key+',') != -1) {
					CUI('input:eq(2)', currRow).prop('checked', true);
				}
				<#if (isProj)?exists && (isProj)>
					<#if property?exists && (property.isControl)== true>
						$('input:eq(0)', currRow).attr('disabled',true);
						$('input:eq(0)', currRow).addClass('cui-readonly-field');
						$('input:eq(1)', currRow).attr('disabled',true);
						$('input:eq(1)', currRow).addClass('cui-readonly-field');
						$('input:eq(2)', currRow).attr('disabled',true);
						$('img',currRow).unbind('click');
					</#if>
				</#if>
				// 新增加一行
				ec.property.addRow();
				ec.property.changeInputType(CUI('input[name="property.multable"]').get(0));
			});
		}
		$('#enumerate_content_table tr:last').remove();
	};
	 /* 创建系统编码下拉框
	 * @method ec.property.createSystemCodeSelect
	 * @public
	 */
	ec.property.createSystemCodeSelect = function(){
		var fill = {};
		var objValue =  CUI('#ec_property_edit_form_property_fillcontent').val();
		if(objValue!=null&&objValue!='null'&&objValue!=undefined&&objValue!='') {
			fill = jQuery.parseJSON(objValue);
			objValue = fill['fillContent'];
		}
		if(objValue==null||objValue=='null'||objValue==undefined) {
			objValue = '';
		}
		var objSelect = CUI('#systemcode_select');
		objSelect.empty();
		var codes = $('#form_set_select_type_name').data('systemCode');
		if (codes==null ||codes == 'null' && codes == undefined || codes=='') {
			CUI.ajax({
    			url: "/msService/ec/view/system-code-types?moduleCode=${module.code!}",
    			type: 'post',
    			async: false,
    			success: function(msg) {
    				$('#form_set_select_type_name').data('systemCode', msg);
    			}
    		});
    		codes = $('#form_set_select_type_name').data('systemCode');
		}
		if (codes!=null&&codes!='null'&&codes!=undefined&&codes!='') {
			$.each(codes, function(key, value) {
				
				var objOptionGroup = document.createElement("optgroup");
				if(key==='others'){
					objOptionGroup.setAttribute('label','${getText("ec.view.else")}');
				}else{
					objOptionGroup.setAttribute('label',key);
				}
				
				CUI(objOptionGroup).appendTo(objSelect);
				$.each(value,function(k,v){
					var objOption=document.createElement("option");
					objOption.innerHTML = v.name;
					objOption.value = k;
					objOption.id = v.id;
                    objOption.setAttribute('companytype', v.companyType );
					systemCodeTypeArr[k] = v.type;
					CUI(objOption).appendTo(objOptionGroup);
				});
				
			});
		}
		if(objValue!=null&&objValue!='null'&&objValue!=undefined) {
			objSelect.val(objValue);
		}
		_fillDefaultContent();
	}
	ec.ass.add_new_fk_property = function(){
		var p = CUI('#ec_entity_select_property').val();
		ec.ass.add_property_dialog = ec.property.edit_dialog("${getHtmlText('ec.property.addfield')}",'/msService/ec/property/add/add?entityCode=<s:property value="entityCode" />'+(p?'&like=' + p:'') , ec.property.save);
		ec.ass.add_property_dialog.show();
	};
	ec.ass.selectChange = function(type,obj,param,callback){
		if(obj && $(obj).val()){
			var data = {};
			data[param] = $(obj).val();
			data["modelCode"]=$("[name='modelCode']").val();
			$.ajax({
				type : "POST",
				url : "/msService/ec/" + type + "/list-select",
				data : data,
				success : function(msg){
					var nextSelects = $(obj).nextAll("select");
					nextSelects.each(function(){this.options.length = 0;});
					var n = $(obj).next();
					var se = n[0];
					se.options.add(new Option('--',''));
					for(var i = 0 ; i < msg.length ; i ++){
						if(type == 'module' && (msg[i].code != 'sysbase_1.0' || msg[i].code != '${module.code!}')) {
							continue;
						}
						if(msg[i].nameInternational) {
							se.options.add(new Option(msg[i].nameInternational,msg[i].code));
						} else {
							se.options.add(new Option(msg[i].displayNameInternational,msg[i].code));
						}
						if(se.id=='ec_select_property'){
							if(msg[i].name=='id'){
								se.options[se.options.length - 1].selected="selected";
							}
						}
					}
					if (callback) {
						CUI(obj).each(callback);
					}
				}
			});
		} else {
			var nextSelects = $(obj).nextAll("select");
			nextSelects.each(function(){this.options.length = 0;});
		}
	};
	ec.property.removeRow = function(obj){
		var tmpTr = CUI(obj).parent().parent();
		var flag = false;
		CUI('input[type="text"]', tmpTr).each(function(){
			if(CUI(this).val() != '') {
				flag = true;
			}
		});
		if(flag) {
			tmpTr.remove();
		} else {
			CUI.Dialog.alert("${getHtmlText('ec.property.deletealert')?js_string}");
		}
	};
	ec.property.changeInputType = function(obj){
		if(obj!=null && obj.checked) {
			CUI('input[name="defaultValueRadio"]').prop('type', 'checkbox');
		} else {
			CUI('input[name="defaultValueRadio"]').prop('type', 'radio');
		}
	}
	ec.property.addRow = function(){
		var str = '<tr bgcolor="#FFFFFF"><td><input type="text" class="cui-edit-field" style="width:95%" /></td><td><input type="text" class="cui-edit-field" style="width:95%" /></td><td><input type="radio" name="defaultValueRadio" /></td><td><img src="/bap/static/foundation/images/icon-del.gif" style="cursor: pointer;" /></td></tr>'
		CUI(str).appendTo('#enumerate_content_table');
		// ec.property.addRowEventBind();
	};
	ec.property.addRowEventBind = function(){
		CUI('#enumerate_content_table input[type="text"]').unbind('blur').bind('blur', function(){
			var lastTr = CUI('#enumerate_content_table tr').last();
			var flag = false;
			CUI('input[type="text"]', lastTr).each(function(){
				if(CUI(this).val() != '') {
					flag = true;
				}
			});
			if(flag) {
				ec.property.addRow();
				ec.property.changeInputType(CUI('input[name="property.multable"]').get(0));
			}
			CUI('#enumerate_content_table img').unbind('click').bind('click', function(){
				ec.property.removeRow(this);
			});
		});
	};
	ec.property.sortable = function() {
		$('#enumerate_content_table').sortable({
			items : $('tr:gt(0)', $('#enumerate_content_table')),
			cursor : "move"
		});
	};
	ec.property.checkLengthIslarger = function(obj) {
		if(obj.value!="" && (parseInt(obj.value) < parseInt($(obj).attr('bakvalue')))){
			ec_property_edit_formDialogErrorBarWidget.show("${getHtmlText('ec.property.maxLengthIslarger')}");
			return false;
		}
	};
	
	ec.property.addOptions=function(select,types,key){
		if(types[key]!=undefined && types[key]!=null && types[key]!=''){
			select.add(new Option(types[key],key));
		}
	};
	ec.property.config_fun = function(){
		var viewType = $('[name="property.type"]').val();
		if(viewType == 'BAPCODE') {
			ec.property.code_config_fun();
		} else if(viewType == 'SUMMARY') {
			ec.property.summary_config_fun();
		}
	};
	ec.property.summary_config_fun=function(){
		var configDialog = _dialog("${getHtmlText('ec.property.summary')}",'/msService/ec/property/summary_config?model.code=${(model.code)!property.model.code}', function(){
			if(ec.summary.config.saveConfig()) {
				configDialog.close();
			}
		});
		configDialog.show();
	};
	ec.property.code_config_fun=function(){
		var configDialog = _dialog("${getHtmlText('ec.property.bapcode')}",'/msService/ec/property/code_config?model.code=${(model.code)!property.model.code}', function(){
			if(ec.code.config.saveConfig()) {
				configDialog.close();
			}
		}, null, 700, 430);
		configDialog.show();
	};
	/**
	 * 添加显示类型
	 */
	ec.property.addSelectShowType=function(select,columnType,showTypes,isObj){
		$(select).empty();
		$('#filedType1_property').show();
		$('#filedType2_property').show();
		if(columnType==='TEXT'||columnType==='LONGTEXT'){
			ec.property.addOptions(select,showTypes,'TEXTFIELD');
			ec.property.addOptions(select,showTypes,'TEXTAREA');
			var propertyIsCustom = $('[name="property.isCustom"]').val();
			if(propertyIsCustom!='true'){
				 ec.property.addOptions(select,showTypes,'RICHTEXT');
			}
		}else if(columnType==='BAPCODE' || columnType==='SUMMARY'){
			ec.property.addOptions(select,showTypes,'TEXTFIELD');
		}else if(columnType==='INTEGER'||columnType==='LONG'||columnType==='DECIMAL' || columnType==='BINARY'){
			ec.property.addOptions(select,showTypes,'TEXTFIELD');
		}else if(columnType==='DATE'){
			ec.property.addOptions(select,showTypes,'DATE');
		}else if(columnType==='DATETIME'){
			ec.property.addOptions(select,showTypes,'DATETIME');
			ec.property.addOptions(select,showTypes,'DATE');
		}else if(columnType==='BOOLEAN'){
			ec.property.addOptions(select,showTypes,'SELECT');
			//ec.property.addOptions(select,showTypes,'CHECKBOX');
		}else if(columnType==='SYSTEMCODE'){
			this.addOptions(select,showTypes,'SELECTCOMP');
			this.addOptions(select,showTypes,'RADIO');
		}else if(columnType==='MONEY'){
			ec.property.addOptions(select,showTypes,'TEXTFIELD');
		}else if(columnType==='PASSWORD'){
			ec.property.addOptions(select,showTypes,'PASSWORDFIELD');
		}else if (columnType==='OBJECT'){
			ec.property.addOptions(select,showTypes,'SELECTCOMP');
		}else if (columnType==='ENUMERATE'){
			ec.property.addOptions(select,showTypes,'SELECT');
			ec.property.addOptions(select,showTypes,'RADIO');
		}else if(columnType==='PROPERTYATTACHMENT'){
			ec.property.addOptions(select,showTypes,'PROPERTYATTACHMENT');
		}else if(columnType==='OFFICE'){
			ec.property.addOptions(select,showTypes,'OFFICE');
		}else if(columnType==='PICTURE'){
			ec.property.addOptions(select,showTypes,'PICTURE');
		}else if(columnType==='TAGNUMBER'){
			ec.property.addOptions(select,showTypes,'SELECTTAGNUMBER');
		}else if(columnType==='LAYER'){
			$('#filedType1_property').hide();
			$('#filedType2_property').hide();
			ec.property.addOptions(select,showTypes,'SELECTCOMP');
		}else if(columnType==='TIME'){
			ec.property.addOptions(select,showTypes,'TIME');
		}else if(columnType==='COLOR'){
			ec.property.addOptions(select,showTypes,'COLOR');
		}else{
			ec.property.addOptions(select,showTypes,'LABEL');
		}
		
		<#if property?? && property.fieldType??>
			select.value = '${property.fieldType!}';
		</#if>
	};
	/**
	 * 根据字段类型与显示类型向select控件中添加显示格式
	 * @param select select控件
	 * @param columnType 字段类型
	 * @param {JSON} formats  显示格式 JSON
	 * @param showType 显示类型 可空
	 * @return
	 */
	ec.property.addFieldFormat=function(select,columnType,formats,showType){
		$(select).empty();
		if(showType==='LABEL' || showType==='PASSWORDFIELD'){
			ec.property.addOptions(select,formats,'TEXT');
		}else if(showType==='LABEL'){
			ec.property.addOptions(select,formats,'TEXT');
		}else if((showType==='TEXT'||showType==='TEXTFIELD'||showType==='TEXTAREA'||showType==='RICHTEXT')&&(columnType==='TEXT'||columnType==='LONGTEXT'||columnType===null)){
			ec.property.addOptions(select,formats,'TEXT');
			ec.property.addOptions(select,formats,'EMAIL');
			ec.property.addOptions(select,formats,'URL');
			ec.property.addOptions(select,formats,'IP');
		}else if((showType==='TEXT'||showType==='TEXTFIELD') && (columnType==='INTEGER'||columnType==='LONG' ||columnType==='DECIMAL'||columnType==='BINARY'||columnType===null)){
			ec.property.addOptions(select,formats,'TEXT');
			this.addOptions(select,formats,'PERCENT');
		}else if(showType==='DATE' && (columnType==='DATE' || columnType==='DATETIME' ||columnType===null)){
			ec.property.addOptions(select,formats,'YMD');
			ec.property.addOptions(select,formats,'YM');
			ec.property.addOptions(select,formats,'Y');
			select.value='YMD';
		}else if(showType==='DATETIME' && (columnType==='DATETIME' ||columnType===null)){
			ec.property.addOptions(select,formats,'YMD_HMS');
			ec.property.addOptions(select,formats,'YMD_HM');
			ec.property.addOptions(select,formats,'YMD_H');
			ec.property.addOptions(select,formats,'YMD');
			ec.property.addOptions(select,formats,'YM');
			ec.property.addOptions(select,formats,'Y');
			select.value='YMD_HMS';
		}else if(columnType==='BOOLEAN' ||columnType===null || columnType==='ENUMERATE'){
			if(showType==='CHECKBOX'){
				ec.property.addOptions(select,formats,'CHECKBOX');
			}else if(showType==='SELECT'){
				ec.property.addOptions(select,formats,'SELECT');
			}else if(showType==='RADIO'){
				ec.property.addOptions(select,formats,'RADIO');
			}
		}else if(columnType==='MONEY'){
			ec.property.addOptions(select,formats,'THOUSAND');
			ec.property.addOptions(select,formats,'TEN_THOUSAND');
		}else if(showType==='SELECTCOMP' && columnType != 'LAYER'){
			ec.property.addOptions(select,formats,'SELECTCOMP');
		}else if(showType==='PROPERTYATTACHMENT'){
			ec.property.addOptions(select,formats,'SELECTCOMP');
		}else if(showType==='OFFICE'){
			ec.property.addOptions(select,formats,'OFFICE');
		}else if(showType==='PICTURE'){
			ec.property.addOptions(select,formats,'PICTURE');
		}else if(showType==='RADIO'){
			ec.property.addOptions(select,formats,'RADIO');
		}else if(columnType==='TAGNUMBER'){
			ec.property.addOptions(select,formats,'SELECTTAGNUMBER');
		}else if(columnType==='LAYER'){
			ec.property.addOptions(select,formats,'LAYER');
		}else if(columnType==='TIME'){
			ec.property.addOptions(select,formats,'HMS');
			ec.property.addOptions(select,formats,'HM');
		}else if(columnType==='COLOR'){
			ec.property.addOptions(select,formats,'HEX');
			ec.property.addOptions(select,formats,'RGBA');
			ec.property.addOptions(select,formats,'HSLA');
		}else{
			ec.property.addOptions(select,formats,'TEXT');
		}
		
		<#if property?? && property.format??>
			select.value = '${property.format!}';
		</#if>
	};
	//修改
	<#if (property.type)?? && 'OBJECT'==property.type>
	CUI(function(){
		ec.property.typeChanged(CUI('#property_type')[0],function(){
			CUI('#ec_select_module').val('${(property.associatedProperty.model.entity.module.code)!}');
			ec.ass.selectChange('entity',CUI('#ec_select_module')[0],'module.code',function(){
				CUI('#ec_select_entity').val('${(property.associatedProperty.model.entity.code)!}');
				ec.ass.selectChange('model',CUI('#ec_select_entity')[0],'entity.code',function(){
					CUI('#ec_select_model').val('${(property.associatedProperty.model.code)!}');
					ec.ass.selectChange('property',CUI('#ec_select_model')[0],'model.code',function(){
						CUI('#ec_select_module').val('${(property.associatedProperty.model.entity.module.code)!}');
						CUI('#ec_select_entity').val('${(property.associatedProperty.model.entity.code)!}');
						CUI('#ec_select_model').val('${(property.associatedProperty.model.code)!}');
						CUI('#ec_select_property').val('${(property.associatedProperty.code)!}');
					});
				});
			});
		});
		if(CUI('input[name="model.enableDataAudit"]').val() == 'false'){
			$('#propertyIgnore').hide();
		}
		CUI('#ec_select_module').prop('disabled',true);
		CUI('#ec_select_entity').prop('disabled',true);
		CUI('#ec_select_model').prop('disabled',true);
		CUI('#ec_select_property').prop('disabled',true);
		CUI('#property_groupobject_cb').prop('disabled',true);
	});
	<#elseif (property.type)?? && ('ENUMERATE'==property.type || 'SYSTEMCODE'==property.type)>
	CUI(function(){
		if(CUI('input[name="model.enableDataAudit"]').val() == 'false'){
			$('#propertyIgnore').hide();
		}
		CUI('*[id^="associated"]').hide();
		CUI('*[id^="baseproperty"]').show();
		CUI('*[id^="baseproperty_base"]').hide();
		<#if 'SYSTEMCODE'==property.type>
		ec.property.createSystemCodeSelect();
	 	CUI('*[id^="baseproperty_enum_enumerate"]').hide();
		<#else>
		CUI('*[id^="baseproperty_enum_systemcode"]').hide();
		CUI('#baseproperty_1_2').hide();
		ec.property.displayEnumerate();
		</#if>
	});
	<#else>
	CUI(function(){
		if(CUI('input[name="model.enableDataAudit"]').val() == 'false'){
			$('#propertyIgnore').hide();
		}
		CUI('*[id^="associated"]').hide();
		CUI('*[id^="baseproperty"]').show();
		CUI('*[id^="baseproperty_enum"]').hide();
	});
	</#if>
	CUI(function(){
		<#if (isProj)?exists && (isProj)>
			$("#ec_property_edit_form input[name='property.isControl']").attr('disabled', true);
			<#if property?exists && (property.isControl)== true>
				$("#ec_property_edit_form input[name='property.name']").attr('readonly', true);
				$("#ec_property_edit_form input[name='property.name']").addClass('cui-readonly-field');
				$("#ec_property_edit_form input[name='property.decimalNum']").attr('readonly', true);
				$("#ec_property_edit_form input[name='property.decimalNum']").addClass('cui-readonly-field');
				$("#ec_property_edit_table input[name='property.multable']").attr('disabled', true);
				$("#ec_property_edit_table input[name='property.isUnique']").attr('disabled', true);
				if($("#ec_property_edit_table input[name='property.nullable']").prop('checked') == false){
					$("#ec_property_edit_table input[name='property.nullable']").attr('disabled', true);
				}
				<#if (property.type)?? && 'SYSTEMCODE'==property.type>
					$("#ec_property_edit_table select[name='systemcode_select']").attr('disabled', true);
				</#if>
			</#if>
		</#if>
	});
		
	CUI(function(){
		// ec.property.sortable();
		// ec.property.addRowEventBind();
		//CUI('#enumerate_content_table img').unbind('click').bind('click', function(){
		//	ec.property.removeRow(this);
		//});
		$('#enumerate_content_table img').hide();
		$('#enumerate_content_table input').prop('disabled', true);
		ec.property.typeChanged($('*[name="property.type"]')[0]);
		<#if property?? && property.isInherent?? && property.isInherent>
		$("#property_isBussinessKey_cb").attr("disabled",true);	//固有字段不能修改业务主键
		</#if>
	});
	
	CUI(function(){
	
		$('#form_set_select_filedType').unbind('change').bind('change',function(){
			ec.property.addFieldFormat(CUI('#form_set_select_val_type').get(0),CUI('#property_type').val(),${formatsJson},CUI(this).val());
		});
		
		if($('#property_isBussinessKey_cb').prop('checked')===true){
			$("input[name='property_nullable']").prop('disabled',true);
			$("input[name='property_isIndex']").prop('disabled',true);
		}
		if($("input[name='property.isCustom']").val() == "true"){
			$("input[name='property_nullable']").prop('disabled',true);
			$("input[name='property_nullable']").prop('checked',true);
		    $("input[name='property.nullable']").val(true);
		}
		$('#property_isBussinessKey_cb').unbind('click').bind('click',function(){
		    $("input[name='property.isBussinessKey']").val($(this).prop('checked'));
		    if($(this).prop('checked')===true){
		        $("input[name='property_nullable']").prop('checked',false);
		        $("input[name='property.nullable']").val(false);
		        $("input[name='property_nullable']").prop('disabled',true);
		        $("input[name='property_isIndex']").prop('checked',true);
		        $("input[name='property.isIndex']").val($(this).prop('checked'));
		        $("input[name='property_isIndex']").prop('disabled',true);
		    }else{
		    	 $("input[name='property_nullable']").prop('disabled',false);
		    	 //$("input[name='property_isUnique']").prop('disabled',false);
		    	 $("input[name='property_isIndex']").prop('disabled',false);
		    }
		});
		$('#property_isMainDisplay_cb').unbind('click').bind('click',function(){
		    $("input[name='property.isMainDisplay']").val($(this).prop('checked'));
		    if($(this).prop('checked')===true){
		        $("input[name='property_nullable']").prop('checked',false);
		        $("input[name='property.nullable']").val(false);
		    }
		});
	});
	
	ec.property.checkUseMneCode = function(){
		if($("input[name='property_isUsedMneCode']").prop('checked')){
			$("input[name='property.isUsedMneCode']").val(true);
		}else{
			$("input[name='property.isUsedMneCode']").val(false);
		}
	}

	CUI.ns('foundation.systemCode');
	
	
	
	
	// 系统编码管理----增加，修改，删除
	foundation.systemCode.codeManage =function(strType){
		var id = '-1';// 初始化为-1，防止id为空的情况下Action报错
		var moduleCode='-1';
		var entityCode='-1';
		var systemClass_company_type='';
		var version;
		if(strType == 'add' && ($('#systemcode_select').val()==null || $('#systemcode_select').val().length==0)){
			CUI.Dialog.alert("${getHtmlText('ec.module.choiceModule')}");
			return false;
		}
		entityCode = $('#systemcode_select').val();
		if(moduleCode!='')  {
			CUI.post("/msService/ec/systemCode/getModuleCode", {"entityCode":entityCode}, function(res){
					moduleCode='${(module.code)!}';;
					// 取得点击的数节点的公司的 TYPE
					if(strType != 'add'){
						id=$("#systemcode_select").find("option:selected").attr("id");
						entityCode=$('#systemcode_select').val();
						// 取得点击的页面的节点的公司的 TYPE
						systemClass_company_type=$("#systemcode_select").find("option:selected").attr("companyType");
					}
				
					if(strType == 'delete'){
						// 只能编辑本公司的系统编码
						if(CUI("#currentCompany_type").val()!=systemClass_company_type){
							ec_property_edit_formDialogErrorBarWidget.showMessage("${getHtmlText('foundation.systemClass.cantEdit')}");
							return false;
						}
						CUI.post("/msService/ec/systemCode/checkDeleteSystemCode", {"entityCode":entityCode}, function(res){
							if(res.checkDeleteFlag == false){
								ec_property_edit_formDialogErrorBarWidget.showMessage("${getHtmlText('foundation.systemCode.checkDeleteClass')}");
								return false;
							}else{
								version=systemEntityListWidget.selectedRows[0].version;
								CUI.Dialog.confirm("${getHtmlText('foundation.systemClass.checkdelete')}", function(){
									CUI.post("/msService/ec/systemCode/deleteCode/default", {"entityId":id,"entityVersion":version}, function(res){
										if(res.dealSuccessFlag == true){
											ec_property_edit_formDialogErrorBarWidget.showMessage("${getHtmlText('foundation.common.deleteandrefreshsuccessful')}","s");
											if(typeof ec_module_Tree != 'undefined' && ec_module_Tree.getSelectedNodes()[0]){
												foundation.systemCode.showSystemClass(ec_module_Tree.getSelectedNodes()[0]);
												foundation.systemCode.queryList();
											} else {
												foundation.systemCode.queryList();
											}
										}else{
											ec_property_edit_formDialogErrorBarWidget.showMessage("${getHtmlText('foundation.systemClass.dealfailure')}");
										}
									 });
								 }); 
						  	}
						});
						return false;
					}
					if(strType == 'add'){
						url="/msService/ec/systemCode/addCode/default?systemEntity.moduleCode="+moduleCode;
					}else if(strType=='modify'){
						if(systemClass_company_type!= CUI("#currentCompany_type").val()){
							ec_property_edit_formDialogErrorBarWidget.showMessage("${getHtmlText('foundation.systemClass.cantEdit')}");
							return false;
						}
						url="/msService/ec/systemCode/modifyCode/default?systemEntity.moduleCode="+moduleCode + "&entityId="+id;
					}
					CUI(function(){
						foundation.systemCode.editCodeDialog = new CUI.Dialog({
							title: "${getHtmlText('foundation.systemCode.systemcodemanager')}",
							url:url,
							modal:true,
							type:3,
							dragable:true,
							buttons:[{name:"${getHtmlText('common.button.save')}",handler:function(){foundation.systemCode.saveSysEntityInfo();}},{name:"${getText('common.button.cancel')}",id:"close",handler:function(){this.close()}}]
						})
						foundation.systemCode.editCodeDialog.show();
					});
			});
		}
		
	};
	
	// 系统编码保存
	foundation.systemCode.saveSysEntityInfo=function(){
		if(CUI("#SubmitSystemEntityForm_systemEntity_id").val()==""){
			if($("#SubmitSystemEntityForm input[name='systemEntity.code']").val()!=""){
				$("#SubmitSystemEntityForm select[name='systemEntity.moduleCode']").prop('disabled', false);
				$('input[name="property.systemcode.value"]').attr("value",CUI("#SubmitSystemEntityForm_systemEntity_code").val());
			}
		}
		CUI('#SubmitSystemEntityForm').submit();
	};
	
	
	// 系统编码保存完毕回调
	foundation.systemCode.systemEntityCallBackInfo=function(res){
		if(res.dealSuccessFlag == true){
			SubmitSystemEntityFormDialogErrorBarWidget.show("保存成功","s");
			setTimeout(function(){
				try{foundation.systemCode.editCodeDialog.close();}catch(e){}
				if(typeof ec_module_Tree != 'undefined' && ec_module_Tree.getSelectedNodes()[0]){
					foundation.systemCode.showSystemClass(ec_module_Tree.getSelectedNodes()[0]);
				} else if(!$("#systemcode_select").is(":disabled")){
					//回调刷新列表
					//$('#systemcode_select')
					var fill = {};
					var objValue =  $('input[name="property.systemcode.value"]').val();
					if(objValue==null||objValue=='null'||objValue==undefined) {
						objValue = '';
					}
					var objSelect = CUI('#systemcode_select');
					objSelect.empty();
					$('#form_set_select_type_name').data('systemCode', null);
					var codes = $('#form_set_select_type_name').data('systemCode');
					if (codes==null ||codes == 'null' && codes == undefined || codes=='') {
						CUI.ajax({
			    			url: "/msService/ec/view/system-code-types?moduleCode=${module.code!}",
			    			type: 'post',
			    			async: false,
			    			success: function(msg) {
			    				$('#form_set_select_type_name').data('systemCode', msg);
			    			}
			    		});
			    		codes = $('#form_set_select_type_name').data('systemCode');
					}
					if (codes!=null&&codes!='null'&&codes!=undefined&&codes!='') {
						$.each(codes, function(key, value) {
							
							var objOptionGroup = document.createElement("optgroup");
							if(key==='others'){
								objOptionGroup.setAttribute('label','${getText("ec.view.else")}');
							}else{
								objOptionGroup.setAttribute('label',key);
							}
							CUI(objOptionGroup).appendTo(objSelect);
							$.each(value,function(k,v){
								var objOption=document.createElement("option");
								objOption.innerHTML = v.name;
								objOption.value = k;
								objOption.id = v.id;
								objOption.setAttribute('companytype', v.companyType );
								systemCodeTypeArr[k] = v.type;
								CUI(objOption).appendTo(objOptionGroup);
							});
							
						});
					}
					if(objValue!=null&&objValue!='null'&&objValue!=undefined) {
						objSelect.val(objValue);
					}
					_fillDefaultContent();
					
					
				}
			},1500);	
		}else{
			alert("处理失败！","f");
		}
		
	};
	
	
	
	//系统编码值管理	
	foundation.systemCode.valueManage=function(){
		var objSelect = CUI('#systemcode_select');
		if(objSelect.val()==null||objSelect.val()=='null'&&objSelect.val()==undefined&&objSelect.val()==''){
			ec_property_edit_formDialogErrorBarWidget.showMessage("${getHtmlText('foundation.systemCode.checkselected')}");
			return false;
		}
		CUI.ajax({
			url: "/msService/ec/systemCode/getSystemEntityForProperty?entityCode="+objSelect.val(),
			type: 'post',
			async: false,
			success: function(msg) {
					var treeType=msg.listType;
					var dialogWidth=800;
					var dialogHeight=580;
					var url='/msService/ec/systemCode/codeValueManager/defaultForProperty?systemEntityCode='+objSelect.val();
					if(treeType=='tree'){
						url='/msService/ec/systemCode/codeValueManager/valueTreeManagerForProperty?systemEntityCode='+objSelect.val();
						dialogWidth=800;
						dialogHeight=730;
						CUI(function(){
						foundation.systemCode.valueCodeManageDialog= new CUI.Dialog({
							title: "${getHtmlText('foundation.systemcode.codeValueManager')}",
							url:url,
							modal:true,
							type:4,
							dragable:true,
							buttons:[
									{	name:"${getHtmlText('common.button.close')}",
										handler:function(){this.close()}
									}]
							})
							foundation.systemCode.valueCodeManageDialog.show();
						});
					}
					else{
						CUI(function(){
							foundation.systemCode.valueCodeManageDialog= new CUI.Dialog({
								title: "${getHtmlText('foundation.systemcode.codeValueManager')}",
								url:url,
								modal:true,
								height:dialogHeight,
								width: dialogWidth,
								dragable:true,
								buttons:[
										{	name:"${getHtmlText('common.button.close')}",
											handler:function(){this.close()}
										}]
							})
							foundation.systemCode.valueCodeManageDialog.show();
						});
					}
			},
			error:function()  {
				alert('error')
			}
		});
	};
	
	$("[name='property.name']").change(function(){
		var columnName = $("[name='property.name']").val().trim();
		var entityCode = "${entity.code}";
		if(null != columnName && "" != columnName ){
		if(!$("input[name='property_groupobject']").prop('checked')){
			$.ajax({
				type : "POST",
				url : "/msService/ec/property/formatColumnName",
				data : {"property.name":columnName,"entity.code":entityCode},
				success : function(msg){
					if(null != msg.columnName && "" != msg.columnName){
						$("[name='property.columnName']").val(msg.columnName);
						<#if !property??>
						var moduleCode = "${module.code}";
						var moduleName = moduleCode.substring(0,moduleCode.lastIndexOf("_"));
						var entityCode = "${entity.code}";
						var entityName = entityCode.substring(entityCode.lastIndexOf("_")+1);
						var modelCode = "${model.code}";
						var modelName = modelCode.substring(modelCode.lastIndexOf("_")+1);
						var propertyName = $("[name='property.name']").val();
						var internationKey = "key=" + moduleName + "." + entityName + "." + modelName + "." + propertyName;
						$("[name='property.displayName']").attr("key",internationKey);
						</#if>
					}
				}
			});
			}
		}
	});
	ec.property.groupobject  = function(){
		$("input[name='property.isGroupObject']").val($(this).prop('checked'));
		if($("input[name='property_groupobject']").prop('checked')){
		$("input[name='property.isGroupObject']").val(true);
			$("input[name='property_isGroupObject']").val(true);
			var entityCode = "${entity.code}";
			$("[name='property.columnName']").attr("readonly",true);
			$("[name='property.columnName']").parents(".fix-search-click").css({"background-color":"#DCDCDC"});
			$("[name='property.columnName']").css({"background-color":"#DCDCDC"});
			$.ajax({
				type : "POST",
				url : "/msService/ec/property/formatColumnName",
				data : {"property.name":'BAP_GROUP_ID',"entity.code":entityCode},
				success : function(msg){ 
					if(null != msg.columnName && "" != msg.columnName){
						$("[name='property.columnName']").val(msg.columnName);
					}
				}
			});
			$("[name='property.modifyColumnName']").hide();
		}else{
			$("input[name='property.isGroupObject']").val(false);
			var columnName = $("[name='property.name']").val().trim();
			var entityCode = "${entity.code}";
			$("[name='property.columnName']").attr("readonly",false);
			$("[name='property.columnName']").parents(".fix-search-click").css({"background-color":"#DCDCDC"});
			$("[name='property.columnName']").css({"background-color":"#DCDCDC"});
			$("[name='property.modifyColumnName']").show();
			$.ajax({
				type : "POST",
				url : "/msService/ec/property/formatColumnName",
				data : {"property.name":columnName,"entity.code":entityCode},
				success : function(msg){ 
					if(null != msg.columnName && "" != msg.columnName){
						$("[name='property.columnName']").val(msg.columnName);
					}else{
						$("[name='property.columnName']").val('');
					}
				}
			});
		}
	}
	//$("[name='property.associatedType']").change(function(){
	//	var associatedType = $("[name='property.associatedType']").val();
	//	if(associatedType == "1"){
	//		$("[name='property.fetchMode'] option[value='SUBSELECT']").remove();
	//	}else if(associatedType == "2" && $("[name='property.fetchMode'] option[value='SUBSELECT']").length == 0){
	//		$("[name='property.fetchMode']").append('<option value="SUBSELECT">${getText('ec.property.fetchMode.SUBSELECT')}</option>');
	//	}
	//});
	$("[name='property.associatedType']").trigger('change');
})();
function modifyColumnName(){
		$("[name='property.columnName']").removeAttr("readonly");
		$("[name='property.columnName']").parents(".fix-search-click").css({"background-color":"#FFFFFF"});
		$("[name='property.columnName']").css({"background-color":"#FFFFFF"});
	
}
CUI(function(){
	function submitBapForm(){//电子签名成功之后出现进度条并提交表单
		var ecFormFlag = false;
		var retrialFormFlag = false;
		if(ecFormFlag && ( $('#ec_property_edit_form').parents('.ewc-dialog-blove').length > 0 || ( window != top && !window.frameElement.getAttribute( 'v3_tab_iframe' ) ) ) ) {
			// dialog不出进度条
			ecFormFlag = false;
		}
		ecFormFlag = (ecFormFlag || retrialFormFlag);
		
		//前台验证通过之后出进度条
		CUI.Dialog.toggleAllButton('ec_property_edit_form',true,ecFormFlag, true);
	// TODO 延时600ms，让页面上已触发 未执行完成的事件处理完成，临时性解决办法
	setTimeout(function(){
		
			// 延迟保存数据, 解决onchange事件无法触发问题
			var formId = 'ec_property_edit_form';
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
			if(CUI('#ec_select_property').prop("disabled")==true){
			    postData=postData+'&property.associatedProperty.code='+$("#ec_select_property").val();
			}
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
							$("#ec_property_edit_form *[name^='"+(index.substring(0,index.length -3))+"'][type!='hidden']").each(function(){
								if($(this).parents('td[nullable=false]').length > 0) {
									showErrorField($(this));
								}
							});
						} else {
							var field = CUI("#ec_property_edit_form *[name='"+index+"']");
							if(field.prop('type') && field.prop('type').toUpperCase() == 'HIDDEN' && field.next() && field.next().length > 0) {
		                		showErrorField(field.next());
		                	} else {
		                		showErrorField(field);
		                	}
						}
						CUI("#ec_property_edit_form *[name='"+index+"']").first().focus();
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
					oErrorWidget = ec_property_edit_formDialogErrorBarWidget;
					if ( $(oErrorWidget.errorInfoBar).closest('.ewc-dialog-el').length>0 ) {
						oErrorWidget.show(errorMsgs);// 对话框内错误提示显示顶部横条
					}	else {
						oErrorWidget.showMessage(errorMsgs);// 编辑界面错误提示显示右上
					}					
					if(CUI.Dialog){
						CUI.Dialog.toggleAllButton('ec_property_edit_form', true);
				    }
				},
				success : function(msg){
					window.onbeforeunload = null;
					if(window.containerLoadPanelWidget) {
						setTimeout(function(){closeLoadPanel();}, 500);
					}
					ec.property.Callback(msg,postData);
				}
			});
		}
	}, 600);
		return false;
	}



	CUI('#ec_property_edit_form').unbind('submit.bapForm').bind('submit.bapForm',function(){
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
		//if(CUI.Dialog) CUI.Dialog.toggleAllButton('ec_property_edit_form',true,ecFormFlag);
		//});
		$('#ec_property_edit_form').trigger('beforeSubmit');
			var successFlag = ec.property.beforeSave();;
			if(successFlag!=null && !successFlag) {/*if(CUI.Dialog) CUI.Dialog.toggleAllButton('ec_property_edit_form', true);*/return false;}
		if($('#ec_property_edit_form input[name="operateType"]').val() == "submit"){
			var deploymentId=$('#ec_property_edit_form input[name="deploymentId"]');
			var buttonCode=$('#ec_property_edit_form input[name="buttonCode"]');
			var namespace=$('#ec_property_edit_form input[name="namespace"]');
			if( deploymentId.length > 0 && deploymentId.val() != undefined && deploymentId.val() != ''){
				var signatureInfo=signatureUtil.getSignatureInfo(true,'',deploymentId.val(),'','')//判断是否需要进行电子签名
				if(signatureInfo[0] != '') {
					var cancelItem = $('input[name="workFlowVarStatus"]');
					if(cancelItem.val() != "cancel") {
						signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.flow.submit','','ec_property_edit_form');
						$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_property_edit_form','ec.flow.submit',false)});
					}
					else {
						signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],'ec.edit.remove','','ec_property_edit_form');
						$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_property_edit_form','ec.edit.remove',false)});
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
						parent.signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),namespace.val(),"ec_property_edit_form",false,'');
						if(signatureInfo[0] == 'singleSign') {
							parent.$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk(namespace.val(),'ec_property_edit_form',buttonCode.val(),false)});
						}
						else {
							setTimeout(function(){parent.$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,namespace.val(),'ec_property_edit_form',buttonCode.val(),false)});},2000);
						}	
					}
					else {
						signatureUtil.showSignatureDialog(signatureInfo[1],signatureInfo[0],buttonCode.val(),'',"ec_property_edit_form",false,'');
						if(signatureInfo[0] == 'singleSign') {
							$('a[name="signature_signatureDialog_showDialog_0"]').unbind('click').bind('click',function(){singleSignOk('','ec_property_edit_form',buttonCode.val(),false)});
						}
						else {
							setTimeout(function(){$('#secondSignerOK').unbind('click').bind('click',function(){doubleSignOK(this,'','ec_property_edit_form',buttonCode.val(),false)});},2000);
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