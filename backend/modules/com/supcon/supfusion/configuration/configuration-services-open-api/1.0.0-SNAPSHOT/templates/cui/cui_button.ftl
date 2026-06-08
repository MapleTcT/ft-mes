<#--
	type: search=查询按钮 clear=清空按钮 adv=高级查询按钮 pending=待办按钮
	formid
	onclick 查询
     如果用高级查询,则传入onadvancedclick，onclick
     customizeName 自定义按钮名称
-->
<#macro querybutton  isCustomize=false,formId="",type="search",onclick="",style="",onadvancedclick="",customizeName="">
  <#if isCustomize>
  	<a class="cui-btn-blue" onclick="${onclick}" style="margin-right:10px;<#if style !="">${style}</#if>"><span class="btn_r">${getHtmlText(customizeName)}</span></a>
  <#else>
		<#if type=="search">
		<a class="cui-btn-green" onclick="${onclick}" style="margin-right:10px;"><span class="btn_r">${getHtmlText('ec.common.search')}</span></a>
		<#elseif type=="adv">
			<a class="cui-btn-green cui-btn-left" onclick="${onclick}" style="margin-right:0;" >${getHtmlText('ec.common.search')}</a>
			<a class="cui-btn-right" onclick="${onadvancedclick}" title="${getText('common.button.advancedquery')}"  style="margin-right:5px;"></a>
		<#elseif type=="pending">
			<a class="cui-btn-blue" onclick="${onclick}" style="margin-right:10px;"><span class="btn_r">${getHtmlText('ec.common.onlypending')}</span></a>
		<#elseif type=="clear">
		<a class="cui-btn-blue" onclick="CUI.resetForm('${formId}')" style="margin-right:10px;<#if style !="">${style}</#if>"><span class="btn_r">${getHtmlText('ec.common.clear')}</span></a>
		<#elseif type=="form">
			<div <#if style !="">style="${style}"<#else>style="float:right"</#if>>
				<a class="cui-btn-blue" onclick="CUI.submitForm('${formId}')"><span class="btn_r">${getHtmlText('foundation.common.save')}</span></a>
				<a class="cui-btn-blue" onclick="CUI.resetForm('${formId}')"><span class="btn_r">${getHtmlText('calendar.common.cancal')}</span></a>
			</div>	
		</#if>
  </#if>
<#if !isCustomize && (type == "search" || type == "adv")>
<script type="text/javascript">
changeBtnStyle();//按钮交互效果
$(function(){
	$("#${formId} input[type='text'][mneType!='mnemonic']").unbind("keydown").bind("keydown",function(evt){
		if(evt.keyCode == 13){
			<#if onclick!="">
        		eval("${onclick}");
        	<#else>
        		$("#${formId}").submit();
        	</#if>
		}
	});	
})
 </script>
</#if>
</#macro>