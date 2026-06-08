<div id="${parameters.id}">
<#list parameters.frames as frame>
	<div id="${frame.id}" region="${frame.region}" <#if frame.height gt 0> height="${frame.height}"</#if><#if frame.width gt 0> width="${frame.width}"</#if><#if frame.title??> header="${frame.title}"</#if><#if frame.collapse>  collapse="true"</#if><#if frame.resize> resize="true"</#if><#if frame.expand> expand="true"</#if><#if frame.animate> animate="true"</#if><#if frame.expandSize gt 0> expandSize="${frame.expandSize}"</#if><#if frame.collapseSize gt 0> collapseSize="${frame.collapseSize}"</#if><#if frame.offsetW gt 0> offsetW="${frame.offsetW}"</#if><#if frame.offsetH gt 0> offsetH="${frame.offsetH}"</#if> >
		${frame.body}
	</div>
</#list>
</div>