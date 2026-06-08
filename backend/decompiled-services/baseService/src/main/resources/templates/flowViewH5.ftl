<style type="text/css">
/*
  * 流程页面
  */     
.flowchart-container{
	overflow:hidden;
	height:100%;
}

.flowchart-title{
  border:1px solid #4691bb;
  border-bottom:none;
  height:23px;
  background:url("/bap/static/datatable/assets/datagrid-column.gif") repeat-x 0 -2px;
}
.flowchart-title strong{
  font-weight:100;
  padding-left:12px;
  float:left;
  margin-top:3px;
}
.flowchart-content iframe{
  min-height:430px;
}
.flowchart-content{
  /*border:1px solid #a0a0a0;
  border-top:none;*/
  background:#fff;
  padding:0px;
  display:block;
  overflow:hidden;
  height:100%;
}

</style>
<#if flowKey??>
<input type="hidden" name="flowKey" id="flowKey" value="${flowKey}"/>
</#if>
<#if flowVersion??>
<input type="hidden" name="flowVersion" id="flowVersion" value="${flowVersion}"/>
</#if>
<#if fvTableInfoId??>
<input type="hidden" name="fvTableInfoId" id="fvTableInfoId" value="${fvTableInfoId?c}"/>
</#if>
<input type="hidden" name="historyXML" id="historyXML"/>
<input type="hidden" name="flowXML" id="flowXML"/>
<#if modelCode??>
<input type="hidden" name="modelCode" id="modelCode" value="${modelCode}"/>
</#if>
<#if entityTableInfo??>
    <input type="hidden" name="entityTableInfo.processKey" id="processKey" value="${entityTableInfo.processKey}"/>
    <input type="hidden" name="entityTableInfo.processName" id="processName"/>
    <input type="hidden" name="entityTableInfo.processVersion" id="processVersion" value="${entityTableInfo.processVersion}"/>
<#else>
    <input type="hidden" name="deployment.processKey" id="processKey" value="${deployment.processKey}"/>
    <input type="hidden" name="deployment.name" id="processName" value="${getText('${deployment.name}')}"/>
    <input type="hidden" name="deployment.processVersion" id="processVersion" value="${deployment.processVersion}"/>
</#if>
<div class="flowchart-content" style="height:100%">
<iframe id="flowH5Frame" src="/msService/baseService/workflow/flowViewIframeH5" frameborder="0" width="100%" height="100%" allowfullscreen="true"></iframe>
</div>

<script type="text/javascript" src="/bap/static/jquery/jquery.js?t=201905302017"></script>
<script>
<#if historyXML??>
    var historyXML = "${historyXML?j_string}";
    $('#historyXML').val(historyXML);
</#if>
<#if flowXML??>
    var flowXML = "${flowXML?j_string}";
    $('#flowXML').val(flowXML);
</#if>
</script>