<#macro helpTip id title="说明", helpInfo="" helpExample="范例", helpCode="", helpHint="", width=300>
<span id="${id}" class="baphelp-icon"></span>
<div id="${id}ref" style="display:none">
	<p class="baphelp-info">${helpInfo}</p>
	<p class="baphelp-example">${helpExample}</p>
<div class="baphelp-code">
<pre><code>${helpCode}
</code></pre>
</div>
<p class="baphelp-hint">${helpHint}</p>
</div>
<script type="text/javascript">
$('#${id}').helptip({refElm: "#${id}ref", html: true, isCustom: false, width: ${width}, title: "${title}"});
</script>
</#macro>