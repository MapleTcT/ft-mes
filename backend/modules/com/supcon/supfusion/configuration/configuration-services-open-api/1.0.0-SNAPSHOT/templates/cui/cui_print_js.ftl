<#macro printJs id,route="0",width="0",height="0",divClass="",divStyle="padding-top:10px", deploymentId="", tableInfoId="", isWorkflow=false,menuOperateCodes="">
<#assign needLoadControl = true >
<#if menuOperateCodes?? && menuOperateCodes?has_content>
	<#list menuOperateCodes?split(",") as opCode>
	<#if checkUserPermisition(opCode)>
		<#assign needLoadControl = true>
		<#break>
	</#if>
	</#list>
</#if>
<#if needLoadControl?? && needLoadControl>
<div id="printControl_${id}" style="${divStyle}" <#if divClass?? && divClass != "">class="${divClass}"</#if>></div>
<#if route == "0">
<script type="text/javascript">
$(function(){
	if(!window.${id}_PrintControl){
		window.${id}_PrintControl = new CUI.PrintControl({
			id : "${id}",
			height : "${height}",
			width : "${width}",
			deploymentId : "${deploymentId}",
			tableInfoId : "${tableInfoId}",
			isWorkflow : <#if isWorkflow>true<#else>false</#if>
		});
	}
	$(window).bind('beforeunload',function(){
	   ${id}_PrintControl.closeWin();
	})
});
/**
 * 跨浏览器控件事件注册调用方法 ,相对于控件中的OnSaveReport
 */
${id}_print_onSaveReport = function(templateId, template) {
	${id}_PrintControl.onSaveReport(templateId, template);
}

/**
 * 跨浏览器控件事件注册调用方法 ,相对于控件中的OnPrintReport
 */
${id}_print_onPrintReport = function(templateId) {
	${id}_PrintControl.onPrintReport(templateId);
}

/**
 * 跨浏览器控件事件注册调用方法 ,相对于控件中的OnBeforePrint
 */
${id}_print_onBeforePrint = function(templateId) {
	${id}_PrintControl.onBeforePrint(templateId);
}

/**
 * 跨浏览器控件事件注册调用方法 ,相对于控件中的SetPrintProperty
 */
${id}_print_setPrintProperty = function() {
	${id}_PrintControl.setPrintProperty();
}

</script>
<#else>
</#if>
</#if>
</#macro>	