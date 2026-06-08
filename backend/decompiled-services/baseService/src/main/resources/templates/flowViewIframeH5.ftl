<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<meta http-equiv="X-UA-Compatible" content="IE=Edge">
	<link rel="stylesheet" href="/bap/static/flowEditH5-release/js/layui/src/css/layui.css?t=201905302017">
    <link rel="stylesheet" href="/bap/static/flowEditH5-release/css/diagramView.css?t=201905302017">
    <link rel="stylesheet" href="/bap/static/flowEditH5-release/css/bpmn.css?t=201905302017">
    <link rel="stylesheet" type="text/css" href="/bap/static/bap-fonts/${getUserFontProperty('font')}/font.css?t=201905302017">
</head>
<div class="g_container">
    <input id="resetFitView" type="hidden">
    <!-- 工具栏开始 -->
    <div class="m_tool" id="toolBox"></div>
    <!-- 工具栏结束 -->
    <!-- 画布开始 -->
    <div id="canvas"></div>
    <!-- 画布结束 -->
</div>
<body>
<!--依赖脚本-->
<script type="text/javascript" src="/bap/static/jquery/jquery.js?t=201905302017"></script>
<script type="text/javascript" src="/bap/static/flowEditH5-release/js/lib/sea.js?t=201905302017"></script>
<script type="text/javascript" src="/bap/static/flowEditH5-release/js/lib/template.js?t=201905302017"></script>
<script type="text/javascript" src="/bap/static/flowEditH5-release/js/layui/src/layui.js?t=201905302017"></script>
  <script>
    seajs.config({
      base: "/bap/static/flowEditH5-release/js/",
    });
    seajs.use("modelerView.js?t=201905302017");  
    //国际化编码对照关系
    var isEn_us=$("#language").val()=="en_us";//是否為英文版
    var commonInternationalKey ={
		'js.ec.workflow.view.fullScreen': "${getText("js.ec.workflow.view.fullScreen")}",
		'js.ec.workflow.view.exitFullScreen': "${getText("js.ec.workflow.view.exitFullScreen")}",
		'js.ec.workflow.view.performInfo': "${getText("js.ec.workflow.view.performInfo")}",
		'js.ec.workflow.view.todoPerson': "${getText("js.ec.workflow.view.todoPerson")}",
		'js.ec.workflow.config.bpmn.info.from':"${getText("js.ec.workflow.config.bpmn.info.from")}"+(isEn_us?" ":""),
	    'js.ec.workflow.config.bpmn.info.support':(isEn_us?" ":"")+"${getText("js.ec.workflow.config.bpmn.info.support")}",
	}
	//获取国际化编码值
	function getInternationalValByKey(key){
	    if(commonInternationalKey[key]){
	       return commonInternationalKey[key];
	    }else{
	      return key;
	    } 
	}
  </script>
<!-- 工具栏模板定义-->
<script type="text/html" id="toolTemp">
  <form class="layui-form" lay-filter="tool">
  <div class="m_tool_item">
    <span class="name">${getText('js.ec.workflow.view.flowName')}</span>
    <em id="flowName"><%=$data.name%></em>
  </div>
  <div class="m_tool_item">
    <span class="name">${getText('js.ec.workflow.view.version')}</span>
    <em><%=$data.version%></em>
  </div>
  <div class="m_tool_item">
    <input type="checkbox" name="hideDescribe" lay-skin="primary" title="${getText('js.ec.workflow.view.hideDescribe')}" lay-filter="hideDescribe">
  </div>
  <div class="m_tool_item">
    <input type="checkbox" name="hideReject" lay-skin="primary" title="${getText('js.ec.workflow.view.hideRejectline')}" lay-filter="hideReject">
  </div>
  <div class="m_tool_item">
    <span class="name">${getText('js.ec.workflow.view.scale')}</span>
    <div class="scaleSlider" id="scaleCtrl"></div>
  </div>
  <div class="m_tool_item">
      <a class="btn_fullScreen" id="fullScreen" data-id="0">${getText('js.ec.workflow.view.fullScreen')}</a>
  </div>
</form>
</script>
