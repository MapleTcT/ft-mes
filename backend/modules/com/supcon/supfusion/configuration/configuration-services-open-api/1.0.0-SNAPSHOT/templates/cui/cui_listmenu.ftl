<#-- listmenu -->
<#macro listmenu id,list=[],listKey="",hideKey="",listName="",value="",onclick="",cssStyle="",leftPos="">
<div id="${id}"></div>
<script type="text/javascript">
	YAHOO.util.Event.onDOMReady(function(){
		var ${id}Widget = new CUI.listMenu({
			<#if leftPos!="">leftPos:"${leftPos}",</#if>
			id:"${id}",
			<#if listKey!="">listKey:"${listKey}",</#if>
			<#if hideKey!="">hideKey:"${hideKey}",</#if>
			<#if listName!="">listName:"${listName}",</#if>
			<#if value!="">value:"${value}",</#if>
			<#if onclick!="">onclick:"${onclick}",</#if>
			 list:[<#list list as cs>
			 	<#if cs_index gt 0>,</#if>
			 	{"id":"${cs.id}","name":"${(cs.shortName)!}","type":"${cs.type!}"}
	  		</#list>]
		});
	});
</script>
</#macro>