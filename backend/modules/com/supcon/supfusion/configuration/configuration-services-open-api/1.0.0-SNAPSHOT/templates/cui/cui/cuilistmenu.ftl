<script type="text/javascript">
	YAHOO.util.Event.onDOMReady(function(){
		var ${parameters.id}Widget = new CUI.listMenu({
			id:"${parameters.id}",
			<#if parameters.listKey??>listKey:"${parameters.listKey}",</#if>
			<#if parameters.hideKey??>hideKey:"${parameters.hideKey}",</#if>
			<#if parameters.listName??>listName:"${parameters.listName}",</#if>
			<#if parameters.value??>value:"${parameters.value}",</#if>
			<#if parameters.onclick??>onclick:"${parameters.onclick}",</#if>
			list:${parameters.list}
			});
	});
</script>
<div id="${parameters.id}" <#if parameters.cssStyle??> style="${parameters.cssStyle}"</#if>></div>