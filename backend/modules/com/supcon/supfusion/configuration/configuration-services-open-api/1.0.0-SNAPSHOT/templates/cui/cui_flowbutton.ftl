<#--
	工作流启动按钮
-->
<#macro flowButton  entityCode="" >
	<#assign urls = getFlowStartUrl(entityCode) >
	<#assign url = "">
	<#if urls?size gt 0>
	<#assign url = urls[0]>
	</#if>
	<#if url != "">
	</#if>
</#macro>