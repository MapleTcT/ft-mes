
    
    <#list parameters.modulesets as module>
      <div id="${module.id}" <#if module.cssClass??>class="${module.cssClass} module"<#else >class="module"</#if><#if module.cssStyle?exists>style="${module.cssStyle}"</#if>>${module.body}</div>
   </#list>
</div>