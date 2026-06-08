<div id="sqlPropertiesDlg">
	<div id = "ec_model_editSqlProperties" style="height:450px; overflow:auto;">
		<table id="ec_model_editSqlProperties_tableInfo" cellpadding="0" cellspacing="0"  align="center" width="98%"  style="margin-top: 5px">
			<thead>
				<tr>
					<th>${getHtmlText('ec.entity.wf.code')}</th>
					<th style="width:150px">${getHtmlText('ec.entity.wf.name')}</th>
					<th>${getHtmlText('ec.entity.type')}</th>
					<th style="width:200px">${getHtmlText('ec.model.format')}/${getHtmlText('ec.property.sysemcode')}</th>
					<th>${getHtmlText('ec.property.businessKey')}</th>
					<th>${getHtmlText('ec.property.isMainDisplay')}</th>
					<th>${getHtmlText('ec.property.isInherent')}</th>
				</tr>
			</thead>
			<tbody>
			<#if properties??>
			<#list properties as p>
			<tr>
				<td>
				<input type='hidden' name='property.name' value='${(p.name)!}'>
				<span title="${(p.name)!}">${(p.name)!}</span>
				</td>
				<td><@international name='property.displayName${p_index}' key=(p.displayName)! moduleCode=module.artifact isNew=true maxLength=80 view=false  cssStyle="border: none" ></@international></th>
				<td style="text-align:center;">
					<select id='property_type_sql' name='property.type' onchange='ec.model.typeChanged(this);' value='${(p.type)!}' <#if (p.isInherent == true) || ((p.code)??)>disabled=true</#if> >
					<#if ((p.code)??) || (p.isInherent == true)>
					<#if p.type == "TEXT">
						<option value='TEXT' selected>${getHtmlText('ec.property.char')}</option>
					<#elseif p.type == "BAPCODE">
						<option value='BAPCODE' selected>${getHtmlText('ec.property.bapcode')}</option>
					<#elseif p.type == "SYSTEMCODE">
						<option value='SYSTEMCODE' selected>${getHtmlText('ec.property.sysemcode')}</option>
					<#elseif p.type == "OBJECT">
						<option value='OBJECT' selected>${getHtmlText('ec.property.object')}</option>
					<#elseif p.type == "SUMMARY">
						<option value='SUMMARY' selected>${getHtmlText('ec.property.summary')}</option>
					<#elseif p.type == "INTEGER">
						<option value='INTEGER' selected>${getHtmlText('ec.property.integer')}</option>
					<#elseif p.type == "DECIMAL">
						<option value='DECIMAL' selected>${getHtmlText('ec.property.decimal')}</option>
					<#elseif p.type == "BOOLEAN">
						<option value='BOOLEAN' selected>${getHtmlText('ec.property.boolean')}</option>
					<#elseif p.type == "LONG">
						<option value='LONG' selected>${getHtmlText('ec.property.longInt')}</option>
					<#elseif p.type == "MONEY">
						<option value='MONEY' selected>${getHtmlText('ec.property.money')}</option>
					<#elseif p.type == "DATETIME">
						<option value='DATETIME' >${getHtmlText('ec.property.datetime')}</option>
					<#elseif p.type == "DATE">
						<option value='DATE' selected>${getHtmlText('ec.property.date')}</option>
					<#elseif p.type == "LONGTEXT">
						<option value='LONGTEXT' selected>${getHtmlText('ec.property.longText')}</option>
					</#if>
					<#else>
					<#if p.type == "TEXT">
                        <option value='TEXT' selected>${getHtmlText('ec.property.char')}</option>
                        <option value='BAPCODE'>${getHtmlText('ec.property.bapcode')}</option>
                        <option value='SYSTEMCODE'>${getHtmlText('ec.property.sysemcode')}</option>
                        <option value='OBJECT'>${getHtmlText('ec.property.object')}</option>
                        <option value='SUMMARY'>${getHtmlText('ec.property.summary')}</option>
                    <#elseif p.type == "DECIMAL">
                        <option value='DECIMAL'>${getHtmlText('ec.property.decimal')}</option>
                        <option value='LONG'>${getHtmlText('ec.property.longInt')}</option>
                        <option value='MONEY'>${getHtmlText('ec.property.money')}</option>
                        <option value='OBJECT'>${getHtmlText('ec.property.object')}</option>
                        <option value='INTEGER' selected>${getHtmlText('ec.property.integer')}</option>
                        <option value='BOOLEAN'>${getHtmlText('ec.property.boolean')}</option>
                    <#elseif p.type == "DATETIME">
                        <option value='DATE' selected>${getHtmlText('ec.property.date')}</option>
                        <option value='DATETIME'>${getHtmlText('ec.property.datetime')}</option>
                    <#elseif p.type == "BOOLEAN">
                        <option value='BOOLEAN' selected>${getHtmlText('ec.property.boolean')}</option>
					<#elseif p.type == "INTEGER">
						<option value='INTEGER' selected>${getHtmlText('ec.property.integer')}</option>
						<option value='BOOLEAN'>${getHtmlText('ec.property.boolean')}</option>
                    <#elseif p.type == "LONGTEXT">
                        <option value='LONGTEXT' selected>${getHtmlText('ec.property.longText')}</option>
                    </#if>
					</#if>
					</select>
				</td>
				<td>
					<div class='format_fix_input' style="<#if (p.isInherent == false) && (p.code)?? && p.type == "OBJECT">background-color:#FFFFFF;cursor:default;<#else>background-color:#eee;cursor:not-allowed;</#if>padding: 0px 0px;border: none;<#if (p.type)?? && p.type == "SYSTEMCODE">display:none<#else>display:block</#if>">
					<label judgeVal="0" moduleVal='${(p.associatedProperty.model.entity.module.code)!}' entityVal='${(p.associatedProperty.model.entity.code)!}' modelVal='${(p.associatedProperty.model.code)!}' associatedPropertyCode='${(p.associatedProperty.code)!}' associatedType='${(p.associatedType)!}' isMainAssociated='${p.isMainAssociated?c}' fetchMode='${(p.fetchMode)!}' id='format_lable${p_index}' <#if (p.isInherent == false) && (p.type == "OBJECT")>style="color:#484848"</#if> >
					<#if (p.type == "OBJECT")>
					${getHtmlText('${(p.associatedProperty.model.entity.module.name)!}')}-${getHtmlText('${(p.associatedProperty.model.entity.name)!}')}-${getHtmlText('${(p.associatedProperty.model.name)!}')}-${getHtmlText('${(p.associatedProperty.name)!}')}
					<#else>
					${getHtmlText('ec.property.choicefield')}
					</#if>
					</label>
					<input onclick='ec.model.formatDialog(this)' id='format_dialog${p_index}' type='button' class='cui-search-click' <#if (p.isInherent == true) || (p.type != "OBJECT")>style="cursor:not-allowed" disabled=true</#if> >
					</div>
					<select name="systemcode_select" id="systemcode_select${p_index}" value='${(p.fillcontent)!}' style="width:194px; <#if (p.type)?? && p.type == "SYSTEMCODE">display: block<#else>display: none</#if>" />
				</td>
				<td style="text-align:center;">
				<input type="checkbox" name="isBussinessKey" <#if (p.isBussinessKey == true)>checked="checked"</#if> onclick='ec.model.checkReceive(this)'>
				</td>
				<td style="text-align:center;">
				<input type="checkbox" name="isMainDisplay" <#if (p.isMainDisplay == true)>checked="checked"</#if> onclick='ec.model.checkReceive(this)'>
				</td>
				<td style="text-align:center;">
				<input type="checkbox" name="property.isInherent" <#if (p.isInherent == true)>checked="checked"</#if>disabled=true value='true' />
				</td>
				<td style="border: none">
				<input type='hidden' name='property.code' value='${(p.code)!}'>
				<input type='hidden' name='property.fieldType' value='${(p.fieldType)!}'>
				<input type='hidden' name='property.format' value='${(p.format)!}'>
				<input type='hidden' name='property.columnName' value='${(p.columnName)!}'>
				</td>
			</tr>
			</#list>
			</#if>
			</tbody>
		</table>
	</div>
	<div id="ec_model_format_dialog" style="display:none;">
		<@errorbar id="ec_model_format_dialogDialogErrorBar" />
		<table id="ec_model_format_dialog_table" class="infoTable" cellpadding="0" cellspacing="0"  align="center" style="width:96%;margin-top: 5px;"> 
			<tr>
				<td class="la">${getHtmlText('ec.property.module')}</td>
				<td><select id="ec_select_module_sql" class="cui-edit-field selectsql" onchange="ec.ass.selectChange('entity',this,'module.code')" style="width:70%" /></td>
			</tr>
			<tr>
				<td class="la">${getHtmlText('ec.property.entity')}</td>
				<td><select id="ec_select_entity_sql" class="cui-edit-field selectsql" onchange="ec.ass.selectChange('model',this,'entity.code')" style="width:70%"></select></td>
			</tr>
			<tr>
				<td class="la">${getHtmlText('ec.property.model')}</td>
				<td> <select id="ec_select_model_sql" class="cui-edit-field selectsql" onchange="ec.ass.selectChange('property',this,'model.code')" style="width:70%"></select></td>
			</tr>
			<tr>
				<td class="la">${getHtmlText('ec.property.field')}</td>
				<td><select name="associatedProperty.code" id="ec_select_property_sql" class="cui-edit-field selectsql" style="width:70%;"></select></td>
			</tr>
			<tr>
				<td class="la"><span>${getHtmlText('ec.property.associateType')}</span></td>
				<td>
					<select name="associatedType" cssClass="cui-edit-field" cssStyle="width:100%" style="width:70%;">
						<option value="1" <#if associatedType?? && associatedType=1>selected</#if>>${getText('ec.property.one2one')}</option>
						<option value="2" <#if associatedType?? && associatedType=2>selected</#if>>${getText('ec.property.many2one')}</option>
					</select>
				</td>
			</tr>
			<#if model?? && model.isMain == false>
			<tr>
				<td class="la"><span>${getHtmlText('ec.property.isMainAssociated')}</span></td>
				<td><input type="checkbox" name="isMainAssociated" checked="checked" value="true"></td>
			</tr>
			</#if>
			<tr>
				<td class="la" style="width:30%">
					${getHtmlText('ec.property.fetchMode')}
					<span id="callBackHelpinfo" class="baphelp-icon helptip-mark"></span>
					<script type="text/javascript">
						$(function(){
							var cbElm = $('#callBackHelpinfo');
							var cbIns;
							cbElm.on('click', function(){
								cbIns.setContent( "${getHtmlText('ec.property.queryPrompt')}" );
							});				
							cbIns = cbElm.helptip({
								html: true, 
								isCustom :false, 
								width: 200, 
								title :"${getText('ec.edit.button.memo')}"
							}).data('cui.tooltip');
						});	
					</script>
				</td>
				<td>
					<select name="fetchMode" cssClass="cui-edit-field" cssStyle="width:100%" style="width:70%;">
						<option value="SELECT" <#if fetchMode?? && fetchMode='SELECT'>selected</#if>>${getText('ec.property.fetchMode.SELECT')}</option>
						<option value="JOIN" <#if fetchMode?? && fetchMode='JOIN'>selected</#if>>${getText('ec.property.fetchMode.JOIN')}</option>
					</select>
				</td>
			</tr>
		</table>
	</div>
</div>