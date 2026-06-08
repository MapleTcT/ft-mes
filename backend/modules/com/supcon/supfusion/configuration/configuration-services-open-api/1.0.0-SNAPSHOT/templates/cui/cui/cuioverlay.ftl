<script type="text/javascript">
	YAHOO.util.Event.onDOMReady(function(){
		YUE.on(YUD.get('${parameters.id}'),'click',function(){
			var ${parameters.id?default('')}Widget = new CUI.Overlay({
				id: "${parameters.id}",
				title: "${parameters.title}",
				text: "${parameters.text}"
				<#if parameters.width gt 0>,width: ${parameters.width}</#if>
				<#if parameters.height gt 0>,height: ${parameters.height}</#if>
			});
		},YUD.get('${parameters.id}'));
	});
</script>
<span id="${parameters.id}" class="overlay-help">&#160;&#160;&#160;&#160;&#160;</span>