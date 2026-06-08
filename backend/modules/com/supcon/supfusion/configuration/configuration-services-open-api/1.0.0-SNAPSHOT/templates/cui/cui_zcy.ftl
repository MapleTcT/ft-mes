<#-- datepicker -->
<#macro datepicker id,tabindex,href,templateDir,title,templateDir,name="",readonly=false,disabled=false>
	<input type="text"<#rt/>
	 name="${name?html}"<#rt/>
	<#if disabled>
	 disabled="disabled"<#rt/>
	</#if>
	<#if readonly>
	 readonly="readonly"<#rt/>
	</#if>
	<#if tabindex??>
	 tabindex="${tabindex?html}"<#rt/>
	</#if>
	<#if href?if_exists != "">
	 href="${href}"<#rt/>
	</#if>
	<#if id??>
	 id="${id?html}"<#rt/>
	</#if>
	<#include "/${templateDir}/simple/css.ftl" />
	<#if title??>
	 title="${title?html}"<#rt/>
	</#if>
	<#include "/${templateDir}/simple/scripting-events.ftl" />
	<#include "/${templateDir}/simple/common-attributes.ftl" />
	<#include "/${templateDir}/simple/dynamic-attributes.ftl" />
	 style="padding-left:2px;width:90px"
	/>
	<script type="text/javascript">
		$(document).ready(function() {
			$("#${id}").datepicker({ picker: "<input type='button' class='cui-calpick'></input>"});    
	    });
	</script>
</#macro>

<#-- listmenu -->
<#macro datepicker id,listKey,hideKey,value,onclick,list,cssStyle>
	<script type="text/javascript">
		$(function() {
			var ${id}Widget = new CUI.listMenu({
				id:"${id}",
				<#if listKey??>listKey:"${listKey}",</#if>
				<#if hideKey??>hideKey:"${hideKey}",</#if>
				<#if listName??>listName:"${listName}",</#if>
				<#if value??>value:"${value}",</#if>
				<#if onclick??>onclick:"${onclick}",</#if>
				list:${list}
				});
		});
	</script>
	<div id="${id}" <#if cssStyle??> style="${cssStyle}"</#if>></div>
</#macro>


