<script type="text/javascript">
	var ${parameters.id}Widget = null;
	YAHOO.util.Event.onDOMReady(function() {
		${parameters.id}Widget = new CUI.Layout('${parameters.id}',{
			<#if parameters.border??>gutter:${parameters.border}</#if>
			<#if parameters.useBgImg !false>,useBgImg:${parameters.useBgImg}</#if>
		});
	});
</script>