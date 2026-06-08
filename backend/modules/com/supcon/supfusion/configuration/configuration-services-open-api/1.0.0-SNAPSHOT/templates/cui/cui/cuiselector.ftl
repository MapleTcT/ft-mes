<#-- 编辑 -->
<#if !parameters.view>
<#-- 如果是系统编码 -->
<#if parameters.type == 'systemcode'>
<select name="${parameters.name}"<#rt/>
<#if parameters.cssStyle??>
 style="${parameters.cssStyle?html}"<#rt/>
<#else>
 style="width:100%;border: 1px solid #A5C7EF"<#rt/>
</#if>
<#include "/template/simple/dynamic-attributes.ftl" />
>
 <option value=""></option>
<#list parameters.dataSource?keys as key> 
 <option value="${key}"<#rt/>
<#if parameters.value?? && parameters.value == key>
 selected="true"<#rt/>
</#if>
>${parameters.dataSource[key]}</option>
</#list>
</select>
<#-- 如果不是系统编码 -->
<#else>
<input type="text" class="cui-edit-field"<#rt/>
 name="${parameters.name?default("")?html}"<#rt/>
<#if parameters.disabled?default(false)>
 disabled="disabled"<#rt/>
</#if>
<#if parameters.readOnly?default(false)>
 readonly="true"<#rt/>
</#if>
<#if parameters.tabindex??>
 tabindex="${parameters.tabindex?html}"<#rt/>
</#if>
<#if parameters.href?if_exists != "">
 href="${parameters.href}"<#rt/>
</#if>
<#if parameters.id??>
 id="${parameters.id?html}"<#rt/>
</#if>
<#if parameters.value??>
 value="${parameters.value?html}"<#rt/>
</#if>
<#if parameters.cssStyle??>
 style="${parameters.cssStyle?html}"<#rt/>
</#if>
<#if parameters.title??>
 title="${parameters.title?html}"<#rt/>
</#if>
<#if parameters.type == 'datepicker'>
 checkDate=true<#rt/>
</#if>
<#include "/template/simple/dynamic-attributes.ftl" />
 />
<#if parameters.type != 'datepicker'>
<input type="button" class="cui-search-click" value="&nbsp;" <#if parameters.onclick??> onclick="${parameters.onclick}(<#if parameters.funcparam??>${parameters.funcparam}</#if>)"</#if>></input>
</#if>
<#if parameters.type == 'datepicker'>
<script type="text/javascript">
	$(document).ready(function() {
		$("#${parameters.id}").datepicker({ picker: "<input type='button' value='&nbsp;' class='cui-calpick'></input>"});    
    });
</script>
</#if>

</#if>
<#-- 查看 -->
<#else>
<input name="${parameters.name}" class="cui-readonly-field" readonly="true" <#rt/>
<#if parameters.cssStyle??>
 style="${parameters.cssStyle?html}"<#rt/>
</#if>
<#if parameters.type == 'systemcode'>
<#list parameters.dataSource?keys as key> 
<#if parameters.value?? && parameters.value == key>
 value="${parameters.dataSource[key]}"<#rt/>
</#if>
</#list>
<#else>
<#if parameters.value??>
 value="${parameters.value?html}"<#rt/>
</#if>
</#if>
<#include "/template/simple/dynamic-attributes.ftl" />
/>
</#if>
