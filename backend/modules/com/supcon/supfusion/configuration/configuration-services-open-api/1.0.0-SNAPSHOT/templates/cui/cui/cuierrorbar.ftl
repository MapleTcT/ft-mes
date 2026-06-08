<script type="text/javascript">
	var ${parameters.id}Widget = null;
	YAHOO.util.Event.onDOMReady(function() {
	
	
		${parameters.id}Widget = new CUI.ErrorBar('${parameters.id}',{
			<#if parameters.errorinfo??>info:'${parameters.errorinfo}'</#if><#if parameters.offsetY?? && parameters.errorinfo??>,</#if>
			<#if parameters.offsetY??>offsetY:${parameters.offsetY}</#if>
		});
	});
	
	
</script>
<div id="${parameters.id}"></div>