<#-- Frame Set -->
<#macro frameset id,border=0,useBgImg=false,style="">
<script type="text/javascript">
	var ${id}Widget = null;
	$(function() {
		${id}Widget = new CUI.Layout('${id}',{
			gutter:${border}
			<#if useBgImg>,useBgImg:${useBgImg?string}</#if>
		});
	});
</script>
<div id="${id}"
<#if style != "">
 style="${style}"
</#if>
>
	<#nested>
</div>
</#macro>
<#macro frame id,region="center",class="",width=0,height=0,title="",collapse=false,resize=false,expand=false,animate=false,expandSize=0,collapseSize=0,offsetW=0,offsetH=0,style="">
	<div 
		id="${id}" 
		region="${region}" 
		<#if height gt 0> height="${height}"</#if>
		<#if width gt 0> width="${width}"</#if>
		<#if title != ""> header="${title}"</#if>
		<#if collapse>  collapse="true"</#if>
		<#if resize> resize="true"</#if>
		<#if expand> expand="true"</#if>
		<#if animate> animate="true"</#if>
		<#if expandSize gt 0> expandSize="${expandSize}"</#if>
		<#if collapseSize gt 0> collapseSize="${collapseSize}"</#if>
		<#if offsetW gt 0> offsetW="${offsetW}"</#if>
		<#if offsetH gt 0> offsetH="${offsetH}"</#if>
		<#if style != ""> style="${style}"</#if>
		<#if class != ""> class="${class}"</#if>
	>
		<#nested>
	</div>
</#macro>