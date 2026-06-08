<script type="text/javascript" charset="utf-8" src="/bap/static/js/FusionCharts.js"></script>

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
<@s.hidden  name="flowKey" id="flowKey" />
<@s.hidden  name="flowVersion" id="flowVersion" />
<@s.hidden  name="fvTableInfoId" id="fvTableInfoId" />
<@s.hidden  name="historyXML" id="historyXML" />
<@s.hidden name="flowXML" id="flowXML" />
<@s.hidden name="modelCode" id="modelCode" />
<@s.hidden name="fvTableInfoId" id="fvTableInfoId" />
<#if entityTableInfo??>
	<@s.hidden name="entityTableInfo.processKey" id="processName"/>
	<@s.hidden name="entityTableInfo.processVersion" id="processVersion" />
<#else>
	<@s.hidden name="deployment.processName" id="processName"/>
	<@s.hidden name="deployment.processVersion" id="processVersion" />
</#if>

<div class="flowchart-content" style="height:100%" id="workflowFlash">
</div>


<script type="text/javascript">

	var jsReady = false;
	function isReady() {
		return jsReady;
	}
	
	function showFlowView(){
		pageInit();
		var ec_flow_view_swf_url = "<@ec_flow_view_swf_url />";
		if($.browser.msie) { 
			var myChart = new FusionCharts(ec_flow_view_swf_url, "flowView",'100%','100%','#FFFFFF',null,null,null,'${getCurrent('language')}');
			myChart.render("workflowFlash");
		} else{
		 	var myChart = new FusionCharts(ec_flow_view_swf_url, "flowView",'100%','97%','#FFFFFF',null,null,null,'${getCurrent('language')}');
			myChart.render("workflowFlash");
		} 
	}
	function pageInit() {
	 jsReady = true;
	}	
	showFlowView();

	function jsGetFlowEntity(){
		
		var historyXML=$("#historyXML").val();
		var flowXML=$("#flowXML").val();
		var modelCode=$("#modelCode").val();
		var tableInfoId=$("#fvTableInfoId").val();
		var processName=$("#processName").val();
		var processVersion=$("#processVersion").val();
		try {
			infosoftglobal.FusionChartsUtil.getChartObject("flowView").getFlowEntity(flowXML,historyXML,modelCode,tableInfoId,processName,processVersion);
			
			setTimeout(function(){infosoftglobal.FusionChartsUtil.getChartObject("flowView").resizeCanvas(0);},10);	   
		}catch(exception){
		}
	}

</script>