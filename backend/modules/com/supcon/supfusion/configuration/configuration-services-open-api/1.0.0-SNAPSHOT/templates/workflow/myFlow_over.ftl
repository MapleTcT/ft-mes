 <#if tableinfos?exists>
 	<#if tableinfos["over"]?exists>
 	<ul id="remindList" class="port-list">
		<#list tableinfos["over"] as info>
	   	<li title="${info['CREATE_TIME']?string("yyyy-MM-dd")} ${info['NAME']?default('')}(${info['SUMMARY']?default(info['TABLE_NO']?default(''))}) ${info['STATUS']?default('')}" onclick="foundation.opentableinfoURL('${info['ID']}','${info['TARGET_ENTITY_CODE']}','${info['TARGET_TABLE_NAME']}')">
			<span style="width:20%;"  >${info['CREATE_TIME']?string("yyyy-MM-dd")}</span>
			<span style="width:50%;" >${info['NAME']?default('')}(${info['SUMMARY']?default(info['TABLE_NO']?default(''))})</span>
			<span style="width:30%;"  >${info['STATUS']?default('')}</span>
		</li>
	    </#list>
	 </ul>
	  <#if tableinfos["over"]?size gt 7>
     <a href="#" onclick="foundation.clickformore('over')">${getHtmlText('foundation.more')}</a>
     </#if>
     <#else>
		<div align="" style="padding-top: 8px;padding-bottom: 4px; text-align: center;"> 
		${getHtmlText('foundation.workflow.nodata')}</div>
    </#if>
 </#if>