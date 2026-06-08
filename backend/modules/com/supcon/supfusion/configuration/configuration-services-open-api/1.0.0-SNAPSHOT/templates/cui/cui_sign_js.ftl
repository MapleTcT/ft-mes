<#macro signJs id,route="0",cabPath="",cabVersion="",width="100%",height="100%",divClass="",divStyle="padding-top:10px",propertyCode="">
<div id="signControl_${id}" style="${divStyle}" <#if divClass?? && divClass != "">class="${divClass}"</#if>></div>
<#if route == "0">
<script type="text/javascript">
var ${id}_SignControl;
(function($){	
	YAHOO.util.Event.onDOMReady(function() {
		${id}_SignControl = new CUI.SignControl({
			<#if id??>id : "${id}",</#if>
			cabPath : "<#if cabPath != "">${cabPath}<#else>/bap/static/office/</#if>",
			cabVersion : "<#if cabVersion != "">${cabVersion}<#else>4,0,0,0</#if>",
			<#if height??>height : "${height}",</#if>
			<#if propertyCode != "">propertyCode : "${propertyCode}",</#if>
			<#if width??>width : "${width}",</#if>
			currentUserName : "${(getCurrent('staffName'))!'bap'}"
		});
	});
})(jQuery);	
</script>
<#else>
<script type="text/javascript">
var ${id}_WebSignControl;
(function($){	
	YAHOO.util.Event.onDOMReady(function() {
		${id}_WebSignControl = new CUI.WebSignControl({
			<#if id??>id : "${id}",</#if>
			cabPath : "<#if cabPath != "">${cabPath}<#else>/bap/static/office/</#if>",
			cabVersion : "<#if cabVersion != "">${cabVersion}<#else>8,0,0,0</#if>",
			<#if height??>height : "${height}",</#if>
			<#if width??>width : "${width}",</#if>
			<#if propertyCode != "">propertyCode : "${propertyCode}",</#if>
			currentUserName : "${(getCurrent('userName'))!'bap'}"
		});
	});
})(jQuery);	
</script>
</#if>
</#macro>	