<div style="overflow:auto;height:100%;_position:relative">
	<@errorbar id="foundation_international_edit_formDialogErrorBar" />
	<@s.form id="foundation_international_edit_form" action="save" namespace="/foundation/international" validate="true" callback="foundation.international.callBackInfo">
		<table class="cui-fd-infotable" id="editInfo" cellpadding="0" cellspacing="0" border="0" align="center" width="90%" style="margin-top: 5px">
			<#if !(key?? && key?has_content)>
			<tr>
				<td class="lab">${getHtmlText('foundation.menuinfo.module')}
				<span id="moduleHelpInfo" class="baphelp-icon helptip-mark"></span>
				</td>
				<td style="width: 70%">
					<select id="internationEdit" name="internation.edit">
						<option></option>
						<#list moduleMap?keys as key>
							<option value="${key}">${moduleMap[key]}</option>
						</#list>
					</select>			
				</td>
			</tr>
			</#if>
			<tr>
				<td class="lab" style="color:#B30303;">${getHtmlText('foundation.international.key')}</td>
				<td style="width: 70%">
					<div class="fix-input">
					<@s.textfield name="key" cssClass="cui-noborder-input"/>
					</div>
				</td>
			</tr>
			<@s.hidden id="listsize" value="${languages.size()}"/>
			<@s.hidden id="isNew" name="isNew" value="${isNew?string}"/>
			<#list languages as l>
			<#if l.isUsed?? && l.isUsed?string=="true">
				<tr>
				<#if lang?? && lang == l.locale?lower_case>
				<td  class="lab" style="color:#B30303;">${getHtmlText(l.displayName)}</td>
				<#else>
				<td  class="lab">${getHtmlText(l.displayName)}</td>
				</#if>
				<td>
					<div class="fix-input">
					<@s.hidden name="ids[${l_index}]" />
					<@s.hidden name="versions[${l_index}]" />
					<@s.textfield id="index_${l_index}" langType="${l.locale?lower_case}" flag="${(values[l_index])?default('')}" name="values[${l_index}]" cssClass="cui-noborder-input"/>
					</div>
				</td>
			</tr>
			</#if>
			</#list>
		</table>
	</@s.form>
	<script type="text/javascript">
	$(function(){

//        var currentKey = $("#foundation_international_edit_form_key").val();
//        if(currentKey){
//            $.simpleSelect.setSelectObjReadOnly('foundation_international_edit_form_key', true);
//         }

		$("#internationEdit").mSelect();
		
		$("#internationEdit").on("change",function(){  
			var moduleCode = $("#internationEdit").val();
			var currentKey = $("#foundation_international_edit_form_key").val();
			if(!(undefined == moduleCode || "" == moduleCode)){
				if(!(currentKey == moduleCode || "" == currentKey)){
					CUI.Dialog.confirm(  
					    "${getText('foundation.international.newinternationalkey')}",  // 提示消息  
					     function(){createNewKey(moduleCode);}, // 确定后事件  
					    function(){}, // 取消后事件  
					    "${getText('foundation.international.internationaltitle')}",  // 标题  
					    70,  // 高度, 可选, 默认70  
					    400  // 宽度, 可选, 默认400  
					); 
				} else {			
						createNewKey(moduleCode);
				}
			}
			
			
		});
		
		var createNewKey = function(moduleCode){
			$.ajax({
				type : "POST",
				url : "/foundation/international/createInternationKey.action",
				data : {"moduleCode":moduleCode},
				success : function(msg){
					if(null != msg.key && "" != msg.key){
						$("#foundation_international_edit_form_key").val(msg.key);
					}
				}
			});
		};
		$('#moduleHelpInfo').helptip({  
		    content: "${getText('foundation.international.helpinfo')}"  
		});
	});
	
</script>
<div>