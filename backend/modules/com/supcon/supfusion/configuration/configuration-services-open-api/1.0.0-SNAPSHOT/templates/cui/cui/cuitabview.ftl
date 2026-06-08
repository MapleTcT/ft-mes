<#if parameters.pattern ?? && parameters.pattern == 'vertical'><#assign position = 'left' /><#else><#assign position = 'top' />
</#if>
<script type="text/javascript">
var ${parameters.id?default('')}Widget;
  YAHOO.util.Event.onDOMReady(function() {
      ${parameters.id?default('')}Widget = new CUI.TabView("${parameters.id?default('')}",{tabposition:'${position}'<#if parameters.width gt 0>,width:${parameters.width}</#if><#if parameters.height gt 0>,height:'${parameters.height}px'</#if><#if parameters.scrollable>,isscroll:true</#if><#if parameters.removeable>,removeable:true</#if>});
  });
</script>
<div id="${parameters.id}" 