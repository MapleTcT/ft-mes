<#-- errorbar -->
<#macro errorbar id,errorinfo="",offsetY=0>
<div id="${id}"></div>
<script type="text/javascript">
	var ${id}Widget = null;
	YAHOO.util.Event.onDOMReady(function() {
		${id}Widget = new CUI.ErrorBar('${id}',{
		
			<#if errorinfo!="">info:'${errorinfo}'</#if>
			<#if offsetY gt 0 && errorinfo!="">,</#if>
			<#if offsetY gt 0>offsetY:${offsetY}</#if>
		});
	});
</script>
</#macro>
