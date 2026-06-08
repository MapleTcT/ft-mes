 <#if tableinfos?exists>
 	<#if tableinfos["handle"]?exists>
 	 <ul id="remindList" class="port-list">
		<#list tableinfos["handle"] as info>
		<li title="${info['CREATE_TIME']?string("yyyy-MM-dd")} ${info['SNAME']?default('')} ${info['NAME']?default('')}(${info['SUMMARY']?default(info['TABLE_NO']?default(''))}) ${info['STATUS']?default('')}" onclick="foundation.openDealinfoURL('${info['ID']}','${info['TARGET_ENTITY_CODE']}','${info['STA']}','${info['TARGET_TABLE_NAME']}')">
			<span style="width:20%;" >${info['CREATE_TIME']?string("yyyy-MM-dd")}</span>
			<span style="width:20%;" >${info['SNAME']?default('')}</span>
			<span style="width:42%;" >${info['NAME']?default('')}(${info['SUMMARY']?default(info['TABLE_NO']?default(''))})</span>
			<span style="width:18%;" >${info['STATUS']?default('')}</span>
		</li>
		
	    </#list>
	 </ul>
	 <#if tableinfos["handle"]?size gt 7>
     <a href="#" onclick="foundation.clickformore('handle')">${getHtmlText('foundation.more')}</a>
     </#if>
     <#else>
		<div align="" style="padding-top: 8px;padding-bottom: 4px; text-align: center;"> 
		${getHtmlText('foundation.workflow.nodata')}</div>
    </#if>
 </#if>