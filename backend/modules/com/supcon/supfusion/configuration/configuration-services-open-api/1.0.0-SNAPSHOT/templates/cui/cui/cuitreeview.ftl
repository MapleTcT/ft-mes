<script type="text/javascript">
 	var BASICPATH = '${base}';
 	var ${parameters.widget} = null;
 	YAHOO.util.Event.onDOMReady(function() {
		${parameters.widget} = new CUI.treeview("${parameters.id}");
		${parameters.widget}.loadParam = {
			    <#if parameters.pageset??>DataSet: ${parameters.pageset},</#if>
			    TreeType      : "${parameters.treetype}",
			    SelectLeaf      : false,
			    BranchField   : "${parameters.branchname}",
			    NodeClick     : "${parameters.onclick}",
			    rootClick     : "${parameters.rootClick}",
			    <#if parameters.rootAttr??>rootAttr      : ${parameters.rootAttr},</#if>
			    toggleClick   : "${parameters.toggleclick}",
			    ShowField     : "${parameters.showfield}",
			    ShowIcon	  : "${parameters.showimgname}",
 				TreeDrag      : "${parameters.drag}",
 				SetImgfn      : "${parameters.setImgfn}",
 				DefaultIcon   : "${parameters.defaultIcon}",
 				<#if parameters.rootName??>rootName : "${parameters.rootName}",</#if>
 				<#if parameters.autoexptag??>autoExpTag : "${parameters.autoexptag}",</#if>
 				Border        : "${parameters.border}",
 				sUrl          : "${parameters.url}",
			    ClassCondition: "${parameters.condition}",
			    ShowSetPath   : "${parameters.showSetPath}",
			    <#if parameters.width gt 0>width         : ${parameters.width},</#if>
				ShowValuePath : "${parameters.showValuePath}"
	  	}
	  	
	  	${parameters.widget}.init();
	});
</script>
<div id="${parameters.id}" <#if parameters.cssClass??> class="${parameters.cssClass}"<#else> class="rootCss" </#if> <#if parameters.cssStyle??> style="${parameters.cssStyle}"</#if>>
<#if parameters.rootName??><span id="${parameters.id}_virtual_root" class="virtual-root">${parameters.rootName}</span></#if>
</div>