<#if parameters.pattern ?? && parameters.pattern == 'vertical'><#assign ext = '_left' /><#else><#assign ext = '' /></#if>
class="etv-navset${ext}">
	<ul class="etv-nav${ext}">
	<#list parameters.tabs as tab>
		<li<#if tab.selected> class="selected"</#if><#if tab.requestUrl??> ajax="true" surl="${tab.requestUrl}"</#if><#if tab.click??> _click="${tab.click}"</#if>>${tab.label}</li>
	</#list>
	</ul>
	<div class="etv-content${ext}">
	<#list parameters.tabs as tab>
		<div<#if tab.id??> id="${tab.id}"</#if>>${tab.body}</div>
	</#list>
	</div>
</div>
