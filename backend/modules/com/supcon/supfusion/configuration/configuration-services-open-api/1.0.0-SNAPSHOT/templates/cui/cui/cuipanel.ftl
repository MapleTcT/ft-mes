<script type="text/javascript">
	YAHOO.util.Event.onDOMReady(function(){
		var panel_${parameters.id} = new CUI.Panel("${parameters.id}",{type:${parameters.type?default('null')}<#if parameters.width gt 0>,width:${parameters.width}</#if><#if parameters.height gt 0>,height:${parameters.height}</#if>});
	});
</script>
<div id="${parameters.id}" <#if parameters.title??> title="${parameters.title}"</#if>>