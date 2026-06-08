<#-- TabView -->
<#macro tabview id,pattern,ext,width=0,height=0,scrollable=false,removeable=false,tabs=[]>
	<script type="text/javascript">
		var ${id}Widget = null;
		$(function() {
			${id}Widget = new CUI.TabView('${id}',{
				<#if width gt 0>,width="${width}"</#if>
				<#if height gt 0>,height="${height}"</#if>
				<#if scrollable>,isscroll=true</#if>
				<#if removeable>,removeable=true</#if>	
			});
		});
	</script>
	<div id="${id}"
	<#if pattern ?? && pattern == 'vertical'><#assign ext = '_left' />
	<#else><#assign ext = '' /></#if> class="etv-navset${ext}">
		<ul class="etv-nav${ext}">
			<#assign tabs = tabs>
			<#list tabs as tab>
				<#if (tab.hide?? && !(tab.hide)) || !tab.hide??>
					<li<#if tab.selected?? && tab.selected> class="selected"</#if><#if tab.useid?? && tab.useid != ""> useid=${tab.useid}</#if><#if tab.requestUrl?? && tab.requestUrl != ""> ajax="true" surl="${tab.requestUrl}"</#if><#if tab.click?? && tab.click != ""> _click="${tab.click}"</#if>>${(tab.label)!}</li>
				</#if>
			</#list>
		</ul>
		<div class="etv-content${ext}">
			<#nested/>
		</div>
	</div>
</#macro>

<#--
tabivew的样例，多多参考 
demo：
<@tabview id='a' ext='' pattern='vertical' tabs=[{"label":"test1","requestUrl":"/test1?","hide":"这边传一个true或者false的表达式"},{"label":"test2","requestUrl":"test2"}",hide":"这边传一个true或者false的表达式"]>
中间写你content中的内容，有几个页签就写几个div
</@tabview>
-->
