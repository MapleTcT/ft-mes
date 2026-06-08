<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<meta http-equiv="X-UA-Compatible" content="IE=Edge">
	<title>${getText('${flowName!}')}</title>
	<@ec_flow_edit />
	<link rel="stylesheet" href="/bap/static/flowEditH5-release/js/layui/src/css/layui.css?t=201905302017">
    <link rel="stylesheet" href="/bap/static/flowEditH5-release/css/diagram-js.css?t=201905302017">
    <link rel="stylesheet" href="/bap/static/flowEditH5-release/css/bpmn.css?t=201905302017">
</head>
<body>
<div class="g_container">
    <!-- 隐藏属性-->
    <input type="hidden" id="lastModify">
    <input type="hidden" id="operatePowers">
    <input type="hidden" id="keyDescs">
    <input type="hidden" id="menuOperateStr">
    <input type="hidden" id="powerBefore">
    <input type="hidden" id="entryUrl">
    <input type="hidden" id="publishType">
    <input type="hidden" id="isEditFlag" value="0">
    <input type="hidden" id="language" value="${getCurrent('language')}">
    <!-- 工具栏开始 -->
    <div class="m_tool" id="menuTool">
      <div class="m_menu_item fl">
        <input type="button" class="release" title="${getText('js.ec.workflow.config.tool.publish')}"/>
        <input type="button" class="save" title="${getText('js.ec.workflow.config.tool.save')}"/>
        <input type="button" class="copy" title="${getText('js.ec.workflow.config.tool.copy')}"/>
        <input type="button" class="paste" title="${getText('js.ec.workflow.config.tool.paste')}"/>
        <input type="button" class="vertical" title="${getText('js.ec.workflow.config.tool.vertical')}"/>
        <input type="button" class="horizontal" title="${getText('js.ec.workflow.config.tool.horizontal')}"/>
        <input type="button" class="redo" title="${getText('js.ec.workflow.config.tool.redo')}"/>
        <input type="button" class="revoke" title="${getText('js.ec.workflow.config.tool.cancel')}"/>
        <input type="button" class="consult" title="${getText('js.ec.workflow.config.tool.reference')}"/>
        <input type="button" class="help" title="${getText('js.ec.workflow.config.tool.help')}"/>
      </div>
      <div class="m_switch_item fr">
          <input class="cur" type="button" value="${getText('js.ec.workflow.config.tool.design')}"/>
          <input type="button" value="${getText('js.ec.workflow.config.tool.source')}"/>
      </div>  
      <div class="viewScale fr">
        <input class="view_btn enlarge fl" type="button" data-id="1" title="${getText('js.ec.workflow.config.tool.enlarge')}"/>
        <input class="view_btn narrow fl" type="button" data-id="-1" title="${getText('js.ec.workflow.config.tool.narrow')}"/>
        <span class="scale" >${getText('js.ec.workflow.config.tool.scale')}<em id="showScale">100%</em></span> 
      </div> 
    </div>
    <!-- 工具栏结束 -->
    <!-- 画布开始 -->
    <div id="canvas"></div>
    <!-- 画布结束 -->
    <!-- 源码模式开始 -->
    <div class="m_codeSource">
      <textarea class="m_codeBox cut-part" id="showCodeSource" readonly >
    
      </textarea>
    </div>
    <!-- 源码模式结束 -->
    <!-- 控制台开始 -->
    <div class="m_consoleBox cut-part">
      <div class="m_ctrlWrap">
        <div class="m_ctrlHead">
           <span class="m_ctrlTit">${getText('js.ec.workflow.config.console')}</span>
           <button class="btn_Clear" id="consoleClear" title="${getText('js.ec.workflow.config.clearConsole')}"></button>
        </div>
        <div class="m_errorConsole" id="consoleTab">
        </div>
      </div>
    </div>
    <!-- 控制台结束 -->
    <@errorbar id="ec_wf_tableErrorBar" />
  </div>
<!--依赖脚本-->
<script type="text/javascript" src="/bap/static/flowEditH5-release/js/lib/sea.js?t=201905302017"></script>
<script type="text/javascript" src="/bap/static/flowEditH5-release/js/lib/template.js?t=201905302017"></script>
<script type="text/javascript" src="/bap/static/flowEditH5-release/js/layui/src/layui.js?t=201905302017"></script>
<script type="text/javascript" src="/bap/static/flowEditH5-release/js/lib/jquery-niceScroll.js?t=201905302017"></script>
<script type="text/javascript" src="/bap/static/flowEditH5-release/js/lib/history.min.js?t=201905302017"></script>
<script type="text/javascript" src="/bap/static/flowEditH5-release/js/${getCurrent('lang')}/workflowInternational.js?t=201905302017"></script>
<script type="text/javascript">
    seajs.config({
      base: "/bap/static/flowEditH5-release/js/",
    });   
    //IE8环境下提示版本过低
    var browser=navigator.appName;
	var b_version=navigator.appVersion;
	var version=b_version.split(";"); 
	var trim_Version=version[1]&&version[1].replace(/[ ]/g,""); 
	var isIE8=(browser=="Microsoft Internet Explorer" && trim_Version=="MSIE8.0")||($.browser.msie&&(Number($.browser.version)<= 8));//IE8兼容视图模式下浏览器判断逻辑补充
	if(isIE8){
	   var lowBrowser="<h3 class='browserNotice'>${getText('js.ec.workflow.config.alert.browserVersion')}<br>${getText('js.ec.workflow.config.alert.browserUpdate')}</h3>";
	   $(".g_container").html(lowBrowser);
	   closeSearch();//中断浏览器加载
	}
	// 页面加载完成后执行
	$(function(){
	   seajs.use("modeler.js?t=201905302017"); 
	});
	//中断浏览器加载
	function closeSearch() {
        var xmlhttp;
        if (window.XMLHttpRequest) {// code for IE7+, Firefox, Chrome, Opera, Safari
            xmlhttp = new XMLHttpRequest();
        }
        else {// code for IE6, IE5
            xmlhttp = new ActiveXObject("Microsoft.XMLHTTP");
        }
        xmlhttp.abort();
        winstop();
    }
    function winstop() {
        if (!!(window.attachEvent && !window.opera))
        { document.execCommand("stop"); }
        else
        { window.stop(); }
    }
</script>
<script type="text/javascript">
	//localFlag：用于指定loadpanel的类型，默认为true
	var localLoadPanelWidget = null;
	var _config={bgColor: "#666666",head: "${getText('js.ec.workflow.config.wait')}",opacity: 50,show: true};
    if(window.createLoadPanel){createLoadPanel(false,null,_config);};
	function createLoadPanel(localFlag,containerEl,_config){
		try{
			if(localFlag == undefined) localFlag = true;
			if(localFlag == false){
				//containerEl = (containerEl == undefined) ? window : containerEl;
				if(!_config) {
					_config = {container:containerEl/*,opacity:50,bgColor:"#666666"*/};
				} else {
					_config.container = containerEl;
				}
				if(!window.containerLoadPanelWidget){
					window.containerLoadPanelWidget = new CUI.loading(_config);
			    }
			}else{
				if(localLoadPanelWidget == null) localLoadPanelWidget = new CUI.loading({local:localFlag,prevEl:'localLoadPanel',paddingLeft:0});
			}
		}catch(e){}
	}
	//关闭的时候，由于无法判断目前出现的loadpanel类型，因此local和container两者都去关闭一下，并将错误捕获
	function closeLoadPanel(){
		try{
			localLoadPanelWidget.close();
			localLoadPanelWidget = null;
		}catch(e){localLoadPanelWidget = null;}
		try{
			window.containerLoadPanelWidget.close();
			window.containerLoadPanelWidget = null;
		}catch(e){window.containerLoadPanelWidget = null;}
	}
	</script>
	<form id="workFlowForm" name="workFlowForm"
     action="/msService/ec/workflow/flowEditH5" method="post">
    <input type="hidden" name="flowId" value="${flowId}" id="flowId"/>
    <input type="hidden" name="deploymentId" value="${deploymentId}" id="deploymentId"/>
    <input type="hidden" name="flowName" value="${flowName}" id="flowName"/>
    <input type="hidden" name="namekey" value="${namekey}" id="namekey"/>
    <input type="hidden" name="requiredTime" value="${requiredTime}" id="requiredTime"/>
    <input type="hidden" name="mobileinitiate" value="${mobileinitiate}" id="mobileinitiate"/>
    <input type="hidden" name="mobileapprove" value="${mobileapprove}" id="mobileapprove"/>
    <input type="hidden" name="allowInvalid" value="${allowInvalid}" id="allowInvalid"/>
    <input type="hidden" name="graduallyReject" value="${graduallyReject}" id="graduallyReject"/>
    <input type="hidden" name="recallAble" value="${recallAble}" id="recallAble"/>
    <input type="hidden" name="recallRemainTime" value="${recallRemainTime}" id="recallRemainTime"/>
    <input type="hidden" name="mainViewViewCode" value="${mainViewViewCode}" id="mainViewViewCode"/>
    <input type="hidden" name="flowKey" value="${flowKey}" id="flowKey"/>
    <input type="hidden" name="des" value="${des}" id="des"/>
    <input type="hidden" name="menuId" value="${menuId}" id="menuId"/>
    <input type="hidden" name="version" value="${version}" id="version"/>
    <input type="hidden" name="processVersion" value="${processVersion}" id="processVersion"/>
    <input type="hidden" name="entityCode" value="${entityCode}" id="entityCode"/>
    <input type="hidden" name="moduleCode" value="${moduleCode}" id="moduleCode"/>
    <input type="hidden" name="i18nModuleCode" value="${i18nModuleCode}" id="i18nModuleCode"/>
    <input type="hidden" name="powerXml" value="${powerXml}" id="powerXml"/>
    <input type="hidden" name="activeArr" value="${activeArr}" id="activeArr"/>
    <input type="hidden" name="flowEditFlag" value="${flowEditFlag}" id="flowEditFlag"/>
    <input type="hidden" name="superviseNamesMultiIDs" value="${superviseNamesMultiIDs}" id="superviseNamesMultiIDs"/>
    <input type="hidden" name="selectStaffs" value="${selectStaffs}" id="selectStaffs"/>
    <input type="hidden" name="signature" value="${signature}" id="signature"/>
    <input type="hidden" name="groupRestrict" value="${((groupRestrict)?? && groupRestrict)?string('true', 'false')}" id="groupRestrict"/>
    </form>
<script type="text/javascript" >
//注册命名空间
CUI.ns("foundation.international");
</script>	
</body>
</html>
<!-- ****************************模板定义*********************** -->
<!-- 发布信息填写 -->
<script type="text/html" id="releaseTemp">
  <form class="layui-form" lay-filter="release">
      <div class="m-pop-form m-pop-content" style="height:200px;">
        <div class="m-form-line">
          <span class="m-form-name">${getText('js.ec.workflow.config.flowName')}</span>
          <div class="m-form-inp">
              <input class="text" type="text" name="name" disabled>
          </div>  
        </div>
        <div class="m-form-line">
          <span class="m-form-name">${getText('js.ec.workflow.config.publishType')}</span> 
          <%var disabled=getModifyAble();%>
          <div class="m-form-sel">
            <select name="type">
              <option value="0">${getText('js.ec.workflow.config.newRelease')}</option>
              <option value="1" <%if(disabled){%>disabled<%}%>>${getText('js.ec.workflow.config.modifyRelease')}</option>
            </select>
          </div>
        </div>
        <div class="m-form-line">
          <span class="m-form-name fl">${getText('js.ec.workflow.config.explain')}</span>
          <div class="m-form-textarea">
              <textarea name="des"></textarea>
          </div>
        </div>
      </div>
      <!-- 浮层按钮 -->
      {{include 'popButtonTemp'}}
    </form> 
</script>
<!-- 右侧属性表格模板 -->
<script type="text/html" id="attrTemp">
    <table class="m-attr-table">
      <thead>
        <tr>
          <th>${getText('js.ec.workflow.config.attribute')}</th>
          <th>${getText('js.ec.workflow.config.value')}</th>
        </tr>
      </thead>
      <tbody>
        <% var num=$data.length<5?5:$data.length;
        for(var i=0;i<num;i++){
          var item=$data[i];
          if(item){%>
          <tr>
            <td><%=item.key%></td>
            <td><%=item.value%></td>
          </tr>
        <%}else{%>
          <tr class="trEmpty">
            <td></td>
            <td></td>
          </tr>
        <%}}%>
      </tbody>
    </table>
</script>
<!-- 开始活动属性编辑面板模板 -->
<script type="text/html" id="editStartEventTemp">
  <form class="layui-form" lay-filter="StartEvent">
    <div class="m-pop-form">
      <div class="m-form-line">
        <span class="m-form-name">${getText('js.ec.workflow.config.name')}</span>
        <div class="m-form-inp" id="international">
            <input type="hidden" name="desc">
            <input type="hidden" name="internationalKey">
        </div> 
      </div>
      <div class="m-form-line">
        <span class="m-form-name">${getText('js.ec.workflow.config.view')}</span>
        <div class="m-form-inp m-view-select" id="viewSelect">
            <input class="text" type="text" disabled name="viewText">
            <input type="hidden" name="viewName">
            <input type="hidden" name="doURL">
            <input type="hidden" name="viewCode">
            <a class="search"></a>
        </div>  
      </div>
      <div class="m-form-line">
        <span class="m-form-name">${getText('js.ec.workflow.config.ignorePermission')}</span>
        <input type="checkbox" name="ignorePermission" lay-skin="primary" >
      </div>
      <!-- 流程启动者 -->
      <div class="m-form-line">
        {{include 'starterTemp' $data.starter}}
      </div>
    </div>
    <!-- 浮层按钮 -->
    {{include 'popButtonTemp'}}
  </form>  
</script>
<!-- 人工活动属性编辑面板 -->
<script type="text/html" id="editTaskEventTemp">
  <form class="layui-form" lay-filter="TaskEvent">
    <div class="m-pop-form">
      <div class="layui-inline m-pop-tab">
          <div class="layui-tab layui-tab-card">
            <ul class="layui-tab-title">
              <li class="layui-this">${getText('js.ec.workflow.config.basicAttr')}</li>
              <li>${getText('js.ec.workflow.config.actor')}</li>
              <li style="display:none">触发事件</li>
              <li>${getText('js.ec.workflow.config.noticeWarn')}</li>
              <li>${getText('js.ec.workflow.config.processEfficiency')}</li>
            </ul>
            <div class="layui-tab-content m-pop-content">
              <div class="layui-tab-item layui-show">
                  <!-- 基本属性 -->
                  <% var typeData={"type":"TaskEvent"};%>
                  {{include 'basicAttributeTemp' typeData}}
              </div>
              <div class="layui-tab-item">
                  <!-- 参与者 -->
                  {{include 'actorTemp' $data.actor}}     
              </div>
              <div class="layui-tab-item" style="display:none">
                  <!-- 触发事件 -->
                  {{include 'triggerTemp' $data.event}}
              </div>
              <div class="layui-tab-item">
                  <!-- 消息提醒 -->
                  {{include 'noticeTemp'}}  
              </div>
              <div class="layui-tab-item">
                  <!-- 流程效率 -->
                  {{include 'efficiencyTemp'}} 
              </div>
            </div>
          </div>
        </div>
    </div>
    <!-- 浮层按钮 -->
    {{include 'popButtonTemp'}}
  </form>
</script>
<!-- 会签活动属性编辑面板-->
<script type="text/html" id="editCountersignEventTemp">
  <form class="layui-form" lay-filter="CountersignEvent">
    <div class="m-pop-form">
      <div class="layui-inline m-pop-tab">
          <div class="layui-tab layui-tab-card">
            <ul class="layui-tab-title">
              <li class="layui-this">${getText('js.ec.workflow.config.basicAttr')}</li>
              <li>${getText('js.ec.workflow.config.actor')}</li>
              <li>${getText('js.ec.workflow.config.scope')}</li>
              <li style="display:none">触发事件</li>
              <li>${getText('js.ec.workflow.config.noticeWarn')}</li>
              <li>${getText('js.ec.workflow.config.processEfficiency')}</li>
            </ul>
            <div class="layui-tab-content m-pop-content">
              <div class="layui-tab-item layui-show">
                  <!-- 基本属性 -->
                  <% var typeData={"type":"CountersignEvent"};%>
                  {{include 'basicAttributeTemp' typeData}}
              </div>
              <div class="layui-tab-item">
                  <!-- 参与者 -->
                  {{include 'actorTemp' $data.actor}}  
              </div>
              <div class="layui-tab-item">
                  <!-- 选人范围 -->
                  {{include 'selectionScopeTemp' $data.selectionScope}}
              </div>
              <div class="layui-tab-item" style="display:none">
                  <!-- 触发事件 -->
                  {{include 'triggerTemp' $data.event}}
              </div>
              <div class="layui-tab-item">
                  <!-- 消息提醒 -->
                  {{include 'noticeTemp'}} 
              </div>
              <div class="layui-tab-item">
                  <!-- 流程效率 -->
                  {{include 'efficiencyTemp'}} 
              </div>
            </div>
          </div>
        </div>
    </div>
    <!-- 浮层按钮 -->
    {{include 'popButtonTemp'}}
  </form>
</script>
<!-- 自动活动属性编辑面板 -->
<script type="text/html" id="editAutoEventTemp">
  <form class="layui-form" lay-filter="AutoEvent">
    <div class="m-pop-form">
      <div class="layui-inline m-pop-tab">
          <div class="layui-tab layui-tab-card">
            <ul class="layui-tab-title">
              <li class="layui-this">${getText('js.ec.workflow.config.basicAttr')}</li>
              <li>${getText('js.ec.workflow.config.noticeWarn')}</li>
            </ul>
            <div class="layui-tab-content m-pop-content" style="height:240px">
              <div class="layui-tab-item layui-show">
                  <!-- 基本属性 -->
                  <div class="m-form-line">
                    <span class="m-form-name">${getText('js.ec.workflow.config.name')}</span>
                    <div class="m-form-inp" id="international">
				        <input type="hidden" name="desc">
				        <input type="hidden" name="internationalKey">
				    </div>
                  </div>
                  <div class="m-form-line">
                    <span class="m-form-name">${getText('js.ec.workflow.config.groovy')}</span>
                    <div class="m-form-inp" id="scriptSelect">
                        <input class="text" type="text" name="scriptName" disabled>
                        <input type="hidden" name="scriptCode">
                        <a class="search"></a>
                    </div>  
                  </div>
              </div>
              <div class="layui-tab-item">
                  <!-- 消息提醒 -->
                  {{include 'noticeTemp'}}  
              </div>
            </div>
          </div>
        </div>
    </div>
    <!-- 浮层按钮 -->
    {{include 'popButtonTemp'}}
  </form>
</script>
<!-- 结束活动属性编辑面板 -->
<script type="text/html" id="editEndEventTemp">
  <form class="layui-form" lay-filter="EndEvent">
    <div class="m-pop-form">
      <div class="layui-inline m-pop-tab">
          <div class="layui-tab layui-tab-card">
            <ul class="layui-tab-title">
              <li class="layui-this">${getText('js.ec.workflow.config.basicAttr')}</li>
              <li>${getText('js.ec.workflow.config.noticeWarn')}</li>
            </ul>
            <div class="layui-tab-content" style="height:160px">
              <div class="layui-tab-item layui-show">
                  <!-- 基本属性 -->
                  <div class="m-form-line">
                    <span class="m-form-name">${getText('js.ec.workflow.config.name')}</span>
                    <div class="m-form-inp" id="international">
				        <input type="hidden" name="desc">
				        <input type="hidden" name="internationalKey">
				    </div> 
                  </div>
                  <div class="m-form-line">
                    <input type="checkbox" name="effectFlag" lay-skin="primary" title="${getText('js.ec.workflow.config.effect')}"> 
                  </div>
              </div>
              <div class="layui-tab-item">
                  <!-- 消息提醒 -->
                  {{include 'noticeTemp'}}  
              </div>
            </div>
          </div>
        </div>
    </div>
    <!-- 浮层按钮 -->
    {{include 'popButtonTemp'}}
  </form>
</script>
<!-- 分发路由属性编辑面板 -->
<script type="text/html" id="editForkEventTemp">
  <form class="layui-form" lay-filter="ForkEvent">
    <div class="m-pop-form">
      <div class="m-form-line" style="padding:30px 0">
        <span class="m-form-name">${getText('js.ec.workflow.config.name')}</span>
        <div class="m-form-inp" id="international">
	        <input type="hidden" name="desc">
	        <input type="hidden" name="internationalKey">
	    </div> 
      </div>
    </div>
    <!-- 浮层按钮 -->
    {{include 'popButtonTemp'}}
  </form>
</script>
<!-- 选择路由属性编辑面板 -->
<script type="text/html" id="editDecisionEventTemp">
  <form class="layui-form" lay-filter="DecisionEvent">
    <div class="m-pop-form m-pop-content" style="height:300px;">
      <div class="m-form-line">
        <span class="m-form-name">${getText('js.ec.workflow.config.name')}</span>
        <div class="m-form-inp" id="international">
	        <input type="hidden" name="desc">
	        <input type="hidden" name="internationalKey">
	    </div>
      </div>
      <div class="m-form-line">
        <div class="m-form-name fl">
            <span>${getText('js.ec.workflow.config.expression')}</span>
	        <span id="exprHelpinfo" class="baphelp-icon"></span>
		    <div id="exprHelpinforef" style="display:none">
		       <p class="baphelp-info">${getText('js.ec.workflow.config.help.groovy')}</p>
			   <p class="baphelp-example">${getText('js.ec.workflow.config.help.example')}</p>
			   <div class="baphelp-code">
			      <pre><code><span>//${getText('js.ec.workflow.config.help.paramExplain')}<br>if(sum>100){<br>return "Link3968";<br>}</span></code></pre>
			   </div>
		    </div>
	    </div>
        <div class="m-form-textarea">
            <textarea class="textarea" name="expr"></textarea>
        </div> 
      </div>
    </div>
    <!-- 浮层按钮 -->
    {{include 'popButtonTemp'}}
  </form>
</script>
<!-- 消息活动属性编辑面板 -->
<script type="text/html" id="editInfoEventTemp">
  <form class="layui-form" lay-filter="InfoEvent">
    <div class="m-pop-form">
      <div class="layui-inline m-pop-tab">
          <div class="layui-tab layui-tab-card">
            <ul class="layui-tab-title">
              <li class="layui-this">${getText('js.ec.workflow.config.basicAttr')}</li>
            </ul>
            <div class="layui-tab-content m-pop-content" style="height:240px">
              <div class="layui-tab-item layui-show">
                  <!-- 基本属性 -->
                  <div class="m-form-line">
                    <span class="m-form-name">${getText('js.ec.workflow.config.name')}</span>
                    <div class="m-form-inp" id="international">
				        <input type="hidden" name="desc">
				        <input type="hidden" name="internationalKey">
				    </div>  
                  </div>
                  <div class="m-form-line">
                    <span class="m-form-name">${getText('js.ec.workflow.config.topic')}</span>
                    <div class="m-form-inp">
                        <input class="text" type="text" name="topic">
                    </div>  
                  </div>
              </div>
            </div>
          </div>
        </div>
    </div>
    <!-- 浮层按钮 -->
    {{include 'popButtonTemp'}}
  </form>
</script>
<!-- 聚合活动属性面板编辑 -->
<script type="text/html" id="editJoinEventTemp">
  <form class="layui-form" lay-filter="JoinEvent">
    <div class="m-pop-form m-pop-content" style="height:160px;">
      <div class="m-form-line">
        <span class="m-form-name">${getText('js.ec.workflow.config.name')}</span>
        <div class="m-form-inp" id="international">
	        <input type="hidden" name="desc">
	        <input type="hidden" name="internationalKey">
	    </div>  
      </div>
      <div class="m-form-line">
        <span class="m-form-name">${getText('js.ec.workflow.config.joinNum')}</span>
        <div class="m-form-inp">
            <input class="text" type="text" name="multiplicity" <%=getBindName()%>="this.value=testPositiveInteger(this.value,false)">
        </div>
      </div>
    </div>
    <!-- 浮层按钮 -->
    {{include 'popButtonTemp'}}
  </form>
</script>
<!-- 子流程属性面板编辑 -->
<script type="text/html" id="editSubProcessEventTemp">
  <form class="layui-form" lay-filter="SubProcessEvent">
    <div class="m-pop-form">
      <div class="m-form-line">
        <span class="m-form-name">名称</span>
        <div class="m-form-inp" id="international">
	        <input type="hidden" name="desc">
	        <input type="hidden" name="internationalKey">
	    </div> 
      </div>
      <div class="m-form-line">
        <span class="m-form-name">编码</span>
        <div class="m-form-inp">
            <input class="text" type="text" name="sub-process-key">
        </div>  
      </div>
      <div class="m-form-line">
        <span class="m-form-name">编辑子流程</span>
        <input class="btn-edit" id="editSubProcess" type="button" value="编辑">
      </div>
      <div class="m-form-line">
        {{include 'paramSetTemp' $data.paramSet}}
      </div>
    </div>
    <!-- 浮层按钮 -->
    {{include 'popButtonTemp'}}
  </form>  
</script>
<!-- 迁移线属性面板编辑 -->
<script type="text/html" id="editSequenceFlowTemp">
  <form class="layui-form" lay-filter="SequenceFlow">
    <div class="m-pop-form">
      <div class="layui-inline m-pop-tab">
          <div class="layui-tab layui-tab-card">
            <ul class="layui-tab-title">
              <li class="layui-this">${getText('js.ec.workflow.config.basicAttr')}</li>
              <li>${getText('js.ec.workflow.config.scope')}</li>
            </ul>
            <div class="layui-tab-content m-pop-content">
              <div class="layui-tab-item layui-show">
                  <!-- 基本属性 -->
                  <div class="m-form-line">
                    <span class="m-form-name">${getText('js.ec.workflow.config.encode')}</span>
                    <div class="m-form-inp">
                        <input class="text" type="text" name="encode" disabled>
                    </div>  
                  </div>
                  <div class="m-form-line">
                    <span class="m-form-name">${getText('js.ec.workflow.config.name')}</span>
                    <div class="m-form-inp" id="international">
				        <input type="hidden" name="desc">
				        <input type="hidden" name="internationalKey">
				    </div>  
                  </div>
                  <div class="m-form-line">
                    <span class="m-form-name">${getText('js.ec.workflow.config.lineType')}</span> 
                    <div class="m-form-sel">
                        <select name="reject" id="lineType">
                          <option value="0">${getText('js.ec.workflow.config.commonLine')}</option>
                          <option value="1">${getText('js.ec.workflow.config.commonRejectLine')}</option>
                        </select>
                    </div>
                  </div>
                  <div class="m-form-line ckbAlign_right">
                    <span class="m-form-name">${getText('js.ec.workflow.config.ableSelectStaff')}</span> 
                    <input type="checkbox" name="ableSelectStaff" lay-skin="primary" lay-filter="ableSelectStaff"> 
                    <input type="checkbox" name="requiredStaff" lay-skin="primary" lay-filter="requiredStaff" title="${getText('js.ec.workflow.config.mustFill')}"> 
                    <input type="checkbox" name="defaultSelectStaff" lay-skin="primary" lay-filter="defaultSelectStaff" title="${getText('js.ec.workflow.config.defaultSelectStaff')}">
                  </div>
                  <div class="m-form-line">
                    <span class="m-form-name">${getText('js.ec.workflow.config.scope')}</span>
                    <input type="radio" name="selectStaff" value="1" title="${getText('js.ec.workflow.config.ourCompany')}" lay-filter="scope">
                    <input type="radio" name="selectStaff" value="2" title="${getText('js.ec.workflow.config.corssCompany')}" lay-filter="scope">
                    <input type="radio" name="selectStaff" value="3" title="${getText('js.ec.workflow.config.ourDepartment')}" lay-filter="scope">
                    <input type="radio" name="selectStaff" value="4" title="${getText('js.ec.workflow.config.departmentsAndSubordinates')}" lay-filter="scope">
                    <input type="radio" name="selectStaff" value="5" title="${getText('js.ec.workflow.config.custom')}" lay-filter="scope">
                  </div>
                  <div class="m-form-line">
                    <span class="m-form-name">${getText('js.ec.workflow.config.sequence')}</span>
                    <div class="m-form-inp">
                        <input class="text" type="text" name="sequence" <%=getBindName()%>="this.value=testPositiveInteger(this.value,true);">
                    </div>  
                  </div>
                  <div class="m-form-line">
                     <div class="m-form-name fl">
			            <span>${getText('js.ec.workflow.config.expression')}</span>
				        <span id="exprHelpinfo" class="baphelp-icon"></span>
					    <div id="exprHelpinforef" style="display:none">
					       <p class="baphelp-info">${getText('js.ec.workflow.config.help.expression')} </p>
						   <p class="baphelp-example">${getText('js.ec.workflow.config.help.example')}</p>
						   <div class="baphelp-code">
						      <pre><code><span>//${getText('js.ec.workflow.config.help.paramExplain')}<br>sum>100</span></code></pre>
						   </div>
					  </div>
				    </div>
                    <div class="m-form-textarea">
                        <textarea name="expr"></textarea>
                    </div>  
                  </div>
                  <div class="m-form-line">
                    <span class="m-form-name fl">${getText('js.ec.workflow.config.describe')}</span>
                    <div class="m-form-textarea">
                      <textarea name="description" ></textarea>
                    </div>
                  </div>
              </div>
              <div class="layui-tab-item">
                  <!-- 选人范围 -->
                  {{include 'selectionScopeTemp' $data.selectionScope}}  
              </div>
            </div>
          </div>
        </div>
    </div>
    <!-- 浮层按钮 -->
    {{include 'popButtonTemp'}}
  </form>
</script>
<!--*******************嵌套子模版定义******************* -->
<!-- 基本属性模板（人工活动、会签活动） -->
<script type="text/html" id="basicAttributeTemp">
  <div class="m-form-line">
      <span class="m-form-name">${getText('js.ec.workflow.config.name')}</span>
      <div class="m-form-inp" id="international">
	       <input type="hidden" name="desc">
	       <input type="hidden" name="internationalKey">
	  </div> 
    </div>
    <% if($data.type=="TaskEvent"){%>
    <div class="m-form-line">
      <span class="m-form-name">${getText('js.ec.workflow.config.taskType')}</span>
      <div class="m-form-sel">
        <select name="taskType">
          <option value="普通活动">${getText('js.ec.workflow.config.ordinaryActivity')}</option>
          <option value="通知活动">${getText('js.ec.workflow.config.notificationActivity')}</option>
        </select>
      </div>
    </div>
    <%}%>
    <div class="m-form-line">
      <span class="m-form-name">${getText('js.ec.workflow.config.callView')}</span>
      <div class="m-form-inp m-view-select" id="viewSelect">
        <input class="text" type="text" disabled name="viewText">
        <input type="hidden" name="viewName">
        <input type="hidden" name="viewUrl">
        <input type="hidden" name="viewCode">
        <input type="hidden" name="viewType">
        <a class="search"></a>
      </div>  
    </div>
    <div class="m-form-line" style="display:none">
      <span class="m-form-name">打开方式</span>
      <div class="m-form-sel">
        <select name="target">
          <option value="_blank">新窗口</option>
          <option value="_self">本窗口</option>
        </select>
      </div>
    </div>
    <div class="m-form-line">
      <span class="m-form-name">${getText('js.ec.workflow.config.candidate')}</span>
      <div class="m-form-inp">
        <input class="text" type="text" name="staffIds"> 
      </div> 
    </div>
    <div class="m-form-line">
      <span class="m-form-name">${getText('js.ec.workflow.config.sequence')}</span>
      <div class="m-form-inp">
        <input class="text" type="text" name="sequence" <%=getBindName()%>="this.value=testPositiveInteger(this.value,true);">  
      </div>
    </div>
    <div class="m-form-line">
      <span class="m-form-name">${getText('js.ec.workflow.config.customParam')}</span>
      <div class="m-form-inp">
       <input class="text" type="text" name="customParam">
      </div>
    </div>
    <% if($data.type=="CountersignEvent"){%>
    <div class="m-form-line">
      <span class="m-form-name">${getText('js.ec.workflow.config.loopSign')}</span>
      <input type="checkbox" name="loopSign" lay-skin="primary" lay-filter="loopSign"> 
      <span class="m-form-name">${getText('js.ec.workflow.config.scope')}</span>
      <input type="radio" name="loop" value="1" title="${getText('js.ec.workflow.config.ourCompany')}" lay-filter="scope">
      <input type="radio" name="loop" value="2" title="${getText('js.ec.workflow.config.corssCompany')}" lay-filter="scope">
      <input type="radio" name="loop" value="3" title="${getText('js.ec.workflow.config.ourDepartment')}" lay-filter="scope">
      <input type="radio" name="loop" value="4" title="${getText('js.ec.workflow.config.departmentsAndSubordinates')}" lay-filter="scope">
      <input type="radio" name="loop" value="5" title="${getText('js.ec.workflow.config.custom')}" lay-filter="scope">
    </div>
    <%}%>
    <div class="m-form-line">
      <span class="m-form-name">${getText('js.ec.workflow.config.bulkDealFlag')}</span>
      <input type="checkbox" name="bulkDealFlag" lay-skin="primary" > 
      <span class="m-form-name">${getText('js.ec.workflow.config.setHandlingOpinions')}</span>
      <div class="m-form-radio">
        <input type="radio" name="dealSet" value="0" title="${getText('js.ec.workflow.config.nullable')}">
        <input type="radio" name="dealSet" value="1" title="${getText('js.ec.workflow.config.mustFill')}">
        <input type="radio" name="dealSet" value="2" title="${getText('js.ec.workflow.config.noFill')}">
      </div>
    </div>
    <div class="m-form-line">
      <span class="m-form-name">${getText('js.ec.workflow.config.webSignetFalg')}</span>
      <input type="checkbox" name="webSignetFalg" lay-skin="primary" > 
      <span class="m-form-name">${getText('js.ec.workflow.config.recallAble')}</span>
      <input type="checkbox" name="recallAble" lay-skin="primary" >
      <span class="m-form-name">${getText('js.ec.workflow.config.showInSimpleDealInfo')}</span>
      <input type="checkbox" name="showInSimpleDealInfo" lay-skin="primary" checked> 
    </div>
    <div class="m-form-line">
      <span class="m-form-name">${getText('js.ec.workflow.config.mobileApprove')}</span>
      <input type="checkbox" name="mobileApprove" lay-skin="primary"> 
      <span class="m-form-name">${getText('js.ec.workflow.config.ignorePermission')}</span>
      <input type="checkbox" name="ignorePermission" lay-skin="primary"> 
      <span class="m-form-name">${getText('js.ec.workflow.config.isAllowProxy')}</span>
      <input type="checkbox" name="isAllowProxy" lay-skin="primary">
    </div>
    <!--<div class="m-form-line">
      <span class="m-form-name">是否双签</span>
      <input type="checkbox" name="doubleSign" lay-skin="primary"> 
    </div>-->
</script>
<!-- 活动发起者 -->
<script type="text/html" id="starterTemp">
  <span class="m-form-name">${getText('js.ec.workflow.config.procesStarter')}</span>
  <input class="btn-add" id="addLine" type="button" value="${getText('js.ec.workflow.config.addLine')}">
  <input class="btn-delete" id="deleteLine" type="button" value="${getText('js.ec.workflow.config.deleteLine')}">
  <div class="m-form-tableHead">
    <table class="m-form-table avgCell3">
      <thead>
        <tr>
          <th>${getText('js.ec.workflow.config.type')}</th>
          <th>${getText('js.ec.workflow.config.name')}</th>
          <th>${getText('js.ec.workflow.config.choose')}</th>
        </tr>
      </thead>
    </table>
  </div>
  <div class="m-form-tableWrap">
    <table class="m-form-table avgCell3" id="starterTab">
      <tbody>
        <% var num=$data.length<6?6:$data.length;
          for(var i=0;i<num;i++){
            var item=$data[i];
            if(item){%>
              <tr>
                {{include 'starterTrTemp' item}}
              </tr>
            <%}else{%>
              <tr class="trEmpty">
                <td></td>
                <td></td>
                <td></td>
              </tr>
              <%}%>
          <%}%>
        </tbody>
      </table>
  </div>
</script>
<!-- 参与者 -->
<script type="text/html" id="actorTemp">
  <input class="btn-add" type="button" value="${getText('js.ec.workflow.config.addLine')}" id="addActor">
  <input class="btn-delete" type="button" value="${getText('js.ec.workflow.config.deleteLine')}" id="deleteActor">
  <div class="m-form-tableHead">
    <table class="m-form-table customCell">
      <thead>
        <tr>
          <th class="cell_10">${getText('js.ec.workflow.config.type')}</th>
          <th class="cell_20">${getText('js.ec.workflow.config.name')}</th>
          <th class="cell_10">${getText('js.ec.workflow.config.choose')}</th>
          <th class="cell_10">${getText('js.ec.workflow.config.positionPower')}</th>
          <th class="cell_20" >${getText('foundation.userpermission.groupRestrict')}</th>
          <th class="cell_10">${getText('js.ec.workflow.config.assignPositionPower')}</th>
          <th class="cell_10">${getText('js.ec.workflow.config.assignStaffPower')}</th>
          <th class="cell_5">${getText('js.ec.workflow.config.unLimitPower')}</th>
        </tr>
      </thead>
    </table>
  </div>
  <div class="m-form-tableWrap">
    <table class="m-form-table customCell" id="actorTab">
      <tbody>
        <% var num=$data.length<11?11:$data.length;
          for(var i=0;i<num;i++){
            var item=$data[i];
            if(item){%>
              <tr>
                {{include 'actorTrTemp' item}}
              </tr>
            <%}else{%>
              <tr class="trEmpty">
                  <td class="cell_10"></td>
                  <td class="cell_20"></td>
                  <td class="cell_10"></td>
                  <td class="cell_10"></td>
                  <td class="cell_20"></td>
                  <td class="cell_10"></td>
                  <td class="cell_10"></td>
                  <td class="cell_5"></td>
              </tr>
              <%}%>
          <%}%>
        </tbody>
      </table>
  </div>
  <input type="checkbox" name="inputorFlag" lay-skin="primary" title="${getText('js.ec.workflow.config.inputorFlag')}"> 
  <input type="checkbox" name="leaderFlag" lay-skin="primary" title="${getText('js.ec.workflow.config.leaderFlag')}"> 
  <input type="checkbox" name="bigLeaderFlag" lay-skin="primary" title="${getText('js.ec.workflow.config.bigLeaderFlag')}"> 
  <input type="checkbox" name="activityDealFlag" lay-skin="primary" title="${getText('js.ec.workflow.config.activityDealFlag')}"> 
  <input type="checkbox" name="flowDealFlag" lay-skin="primary" title="${getText('js.ec.workflow.config.flowDealFlag')}"> 
<#--  <input type="checkbox" name="attentFlag" lay-skin="primary" title="${getText('js.ec.workflow.config.attentFlag')}"> -->
</script>
<!-- 触发事件 -->
<script type="text/html" id="triggerTemp">
  <input class="btn-add" type="button" value="${getText('js.ec.workflow.config.addLine')}" id="addTrigger">
  <input class="btn-delete" type="button" value="${getText('js.ec.workflow.config.deleteLine')}" id="deleteTrigger">
  <div class="m-form-tableHead">
    <table class="m-form-table avgCell3">
      <thead>
        <tr>
          <th>名称</th>
          <th>触发点</th>
          <th>触发事件</th>
        </tr>
      </thead>
    </table>
  </div>
  <div class="m-form-tableWrap">
    <table class="m-form-table avgCell3" id="triggerTab">
      <tbody>
        <% var num=$data.length<11?11:$data.length;
          for(var i=0;i<num;i++){
            var item=$data[i];
            if(item){%>
              <tr>
                {{include 'triggerTrTemp' item}}
                  
              </tr>
            <%}else{%>
              <tr class="trEmpty">
                  <td></td>
                  <td></td>
                  <td></td>
              </tr>
              <%}%>
          <%}%>
        </tbody>
      </table>
  </div>
</script>
<!-- 选人范围 -->
<script type="text/html" id="selectionScopeTemp">
  <input class="btn-add" type="button" value="${getText('js.ec.workflow.config.addLine')}" id="addScope">
  <input class="btn-delete" type="button" value="${getText('js.ec.workflow.config.deleteLine')}" id="deleteScope">
  <div class="m-form-tableHead">
    <table class="m-form-table avgCell7">
      <thead>
        <tr>
          <th>${getText('js.ec.workflow.config.type')}</th>
          <th>${getText('js.ec.workflow.config.name')}</th>
          <th>${getText('js.ec.workflow.config.choose')}</th>
          <th>${getText('js.ec.workflow.config.groupName')}</th>
          <th>${getText('js.ec.workflow.config.order')}</th>
          <th></th>
          <th></th>
        </tr>
      </thead>
    </table>
  </div>
  <div class="m-form-tableWrap">
    <table class="m-form-table avgCell7" id="selectionScopeTab">
      <tbody>
        <% var num=$data.length<11?11:$data.length;
          for(var i=0;i<num;i++){
            var item=$data[i];
            if(item){%>
              <tr>
                {{include 'selectionScopeTrTemp' item}}
              </tr>
            <%}else{%>
              <tr class="trEmpty">
                  <td></td>
                  <td></td>
                  <td></td>
                  <td></td>
                  <td></td>
                  <td></td>
                  <td></td>
              </tr>
              <%}%>
          <%}%>
        </tbody>
      </table>
  </div>
</script>
<!-- 参数设置 -->
<script type="text/html" id="paramSetTemp">
  <span class="m-form-name">参数设置</span>
  <div class="fr">
    <input class="btn-add" id="addParamSet" type="button" value="${getText('js.ec.workflow.config.addLine')}">
    <input class="btn-delete" id="deleteParamSet" type="button" value="${getText('js.ec.workflow.config.deleteLine')}">
  </div>
  <div class="m-form-tableHead">
    <table class="m-form-table avgCell3">
      <thead>
        <tr>
          <th>主流程参数名称</th>
          <th>子流程参数名称</th>
          <th>参数方向</th>
        </tr>
      </thead>
    </table>
  </div>
  <div class="m-form-tableWrap">
    <table class="m-form-table avgCell3" id="paramSetTab">
      <tbody>
        <% var num=$data.length<6?6:$data.length;
          for(var i=0;i<num;i++){
            var item=$data[i];
            if(item){%>
              <tr>
                {{include 'paramSetTrTemp' item}}
              </tr>
            <%}else{%>
              <tr class="trEmpty">
                <td></td>
                <td></td>
                <td></td>
              </tr>
              <%}%>
          <%}%>
        </tbody>
      </table>
  </div>
</script>
<!-- 消息提醒 -->
<script type="text/html" id="noticeTemp">
  <div class="m-form-line">
      <span class="m-form-name">${getText('js.ec.workflow.config.noticeType')}</span>
      <input type="checkbox" name="email" lay-skin="primary" title="${getText('js.ec.workflow.config.email')}"> 
      <input type="checkbox" name="jabber" lay-skin="primary" title="${getText('js.ec.workflow.config.jabber')}"> 
      <input type="checkbox" name="sms" lay-skin="primary" title="${getText('js.ec.workflow.config.sms')}"> 
      <input type="checkbox" name="app" lay-skin="primary" title="${getText('js.ec.workflow.config.app')}"> 
  </div>
</script>
<!-- 流程效率 -->
<script type="text/html" id="efficiencyTemp">
  <div class="m-form-line">
    <span class="m-form-name">${getText('js.ec.workflow.config.requiredTime')}</span>
    <div class="m-form-inp">
      <input class="text short" type="text" name="requiredTime" <%=getBindName()%>="this.value=this.value.replace(/[^\d.]/g,'')">${getText('js.ec.workflow.config.hour')}  
    </div>
  </div>
  <div class="m-form-line">
    <span class="m-form-name">${getText('js.ec.workflow.config.overdueReminders')}</span>
    <input type="checkbox" name="overdueReminders" lay-skin="primary"> 
  </div>
</script>
<!-- 浮层按钮 -->
<script type="text/html" id="popButtonTemp">
  <div class="m-form-button">
    <input class="btn-submit" id="submitForm" lay-submit lay-filter="*" type="button" value="${getText('js.ec.workflow.config.confirm')}">
    <input class="btn-cancel" id="cancelForm" type="button" value="${getText('js.ec.workflow.config.cancel')}">
  </div>
</script>
<!-- *********************表体行模板定义******************** -->
<!-- 表体行***活动发起者*** -->
<script type="text/html" id="starterTrTemp">
  <td>
  <% var disabled=$data.typeName&&$data.typeName!="";%>
    <select name="sel_userType" <%if(disabled){%>disabled<%}%>>
      <option value="USER" <%if($data.userType&&$data.userType=='USER'){%>selected<%}%>>${getText('js.ec.workflow.config.user')}</option>
      <option value="ROLE" <%if($data.userType&&$data.userType=='ROLE'){%>selected<%}%>>${getText('js.ec.workflow.config.role')}</option>
      <!-- <option value="POSITION" <%if($data.userType&&$data.userType=='POSITION'){%>selected<%}%>>${getText('js.ec.workflow.config.position')}</option> -->
      <!-- <option value="DEPTMENT" <%if($data.userType&&$data.userType=='DEPTMENT'){%>selected<%}%>>${getText('js.ec.workflow.config.department')}</option> -->
    </select>
  </td>
  <td><p class="nowrap" title="<%=$data.typeName%>"><%=$data.typeName?$data.typeName:""%></p></td>
  <td><input class="btn-choose" name="btn_userType" type="button" value="${getText('js.ec.workflow.config.choose')}" data-id="<%=$data.typeId%>" data-name="<%=$data.typeName%>"></td>
</script>
<!-- 表体行***参与者*** -->
<script type="text/html" id="actorTrTemp">
  <td class="cell_10">
  <% var disabled=$data.typeName&&$data.typeName!="";
  var rand=getRand(7);
  var groupRestrict=getGroupRestrict();%>
    <select name="sel_userType" <%if(disabled){%>disabled<%}%>>
      <option value="USER" <%if($data.userType&&$data.userType=='USER'){%>selected<%}%>>${getText('js.ec.workflow.config.user')}</option>
      <option value="ROLE" <%if($data.userType&&$data.userType=='ROLE'){%>selected<%}%>>${getText('js.ec.workflow.config.role')}</option>
      <option value="POSITION" <%if($data.userType&&$data.userType=='POSITION'){%>selected<%}%>>${getText('js.ec.workflow.config.position')}</option>
      <option value="DEPTMENT" <%if($data.userType&&$data.userType=='DEPTMENT'){%>selected<%}%>>${getText('js.ec.workflow.config.department')}</option>
    </select>
  </td>
  <td class="cell_20"><p class="nowrap" title="<%=$data.typeName%>"><%=$data.typeName?$data.typeName:""%></p></td>
  <td class="cell_10"><input class='btn-choose btn60' name="btn_userType" type='button' data-id="<%=$data.typeId%>" data-name="<%=$data.typeName%>" value='${getText('js.ec.workflow.config.choose')}'></td>
  <td class="cell_10">
    <input lay-filter='limit' name='positionPower' type='checkbox' lay-skin='primary' <%if($data.positionPower){%>checked<%}%>>
  </td>
  <td class="cell_20" <%if(!groupRestrict){%>style='opacity:0.7'<%}%>>
      <input lay-filter='limit' name='groupPower' type='checkbox' <%if(!groupRestrict){%>disabled<%}%> lay-skin='primary'<%if($data.groupPower){%>checked<%}%>>
  </td>
  <td class="cell_10">
      <input lay-filter='limit' name='assignPositionPower' type='checkbox' lay-skin='primary' <%if($data.assignPositionPower){%>checked<%}%>>
      <input lay-filter='limit' class='btn-choose btn60' name="postOrder" type='button' value='${getText('js.ec.workflow.config.designate')}' data-val='<%=$data.assignPositions%>'>
  </td>
  <td class="cell_10">
      <input lay-filter='limit' name='assignStaffPower' type='checkbox' lay-skin='primary' <%if($data.assignStaffPower){%>checked<%}%> >
      <input lay-filter='limit' class='btn-choose btn60' name="personOrder" type='button' value='${getText('js.ec.workflow.config.designate')}' data-val='<%=$data.assignStaffs%>'>
  </td>
  <td class="cell_5">
      <input lay-filter='unlimit' name='unLimitPower' type='checkbox' lay-skin='primary' <%if($data.unLimitPower){%>checked<%}%>>
  </td>
</script>
<!-- 表体行***触发事件*** -->
<script type="text/html" id="triggerTrTemp">
  <td><input class="inp_edit" type="text" value='<%=$data.name?$data.name:""%>'></td>
  <td>
      <select>
        <option value="null" <%if($data.event&&$data.event=='null'){%>selected<%}%>>活动开始前</option>
        <option value="end" <%if($data.event&&$data.event=='end'){%>selected<%}%>>活动开始后</option>
      </select>
  </td>
  <td><input class="inp_edit" type="text" value='<%=$data.triggerName?$data.triggerName:""%>'></td>
</script>
<!-- 表体行***选人范围*** -->
<script type="text/html" id="selectionScopeTrTemp">
  <td>
     <% var disabled=$data.typeName&&$data.typeName!="";%>
    <select name="sel_userType" <%if(disabled){%>disabled<%}%>>
      <option value="USER" <%if($data.userType&&$data.userType=='USER'){%>selected<%}%>>${getText('js.ec.workflow.config.user')}</option>
      <option value="ROLE" <%if($data.userType&&$data.userType=='ROLE'){%>selected<%}%>>${getText('js.ec.workflow.config.role')}</option>
      <option value="POSITION" <%if($data.userType&&$data.userType=='POSITION'){%>selected<%}%>>${getText('js.ec.workflow.config.position')}</option>
      <option value="DEPTMENT" <%if($data.userType&&$data.userType=='DEPTMENT'){%>selected<%}%>>${getText('js.ec.workflow.config.department')}</option>
    </select>
  </td>
  <td><p class="nowrap" title="<%=$data.typeName%>"><%=$data.typeName?$data.typeName:""%></p></td>
  <td><input class='btn-choose btn60' name="btn_userType" type='button' value='${getText('js.ec.workflow.config.choose')}' data-id="<%=$data.typeId%>" data-name="<%=$data.typeName%>"></td>
  <td><input class="inp_edit" type="text" value='<%=$data.groupName?$data.groupName:""%>'></td>
  <td><input class="inp_edit" type="text" value='<%=$data.order?$data.order:""%>'></td>
  <td></td>
  <td></td>
</script>
<!-- 表体行***参数设置*** -->
<script type="text/html" id="paramSetTrTemp">
  <td><input class="inp_edit" type="text" value="<%=$data.var&&$data.var!="null"?$data.var:""%>"></td>
  <td><input class="inp_edit" type="text" value="<%=$data.subvar&&$data.subvar!="null"?$data.subvar:""%>"></td>
  <td>
      <select name="direction">
        <option value="in" <%if($data.type&&$data.type=='in'){%>selected<%}%>>输入参数</option>
        <option value="out" <%if($data.type&&$data.type=='out'){%>selected<%}%>>输出参数</option>
      </select>
  </td>
</script>
<!---国际化--->
<script type="text/html" id="internationTemp">
<div class="m_wrap">
<@international name="workflow" isNew=true isOldEdit=true moduleCode="${i18nModuleCode!}" key=""  cssClass="cui-edit-field"></@international>
</div>
</script>
<!---在线帮助--->
<script type="text/html" id="helpTemp">
 <div class="m_helpBox">
        <table class="m_helpTab">
            <tr>
                <th>${getText('js.ec.workflow.config.shortcuts')}</th>
                <th>${getText('js.ec.workflow.config.describe')}</th>
            </tr>
            <tr>
                <td>Delete</td>
                <td>${getText('js.ec.workflow.config.shortcuts.delete')}</td>
            </tr>
            <tr>
                <td>Shift</td>
                <td>${getText('js.ec.workflow.config.shortcuts.drag')}</td>
            </tr>
            <tr>
                <td rowspan="2">Ctrl</td>
                <td>${getText('js.ec.workflow.config.shortcuts.select')}</td>
            </tr>
            <tr>
                <td>${getText('js.ec.workflow.config.shortcuts.zoom')}</td>
            </tr>
            <tr>
                <td>Ctrl+ C</td>
                <td>${getText('js.ec.workflow.config.shortcuts.copy')}</td>
            </tr>
            <tr>
                <td>Ctrl+ V</td>
                <td>${getText('js.ec.workflow.config.shortcuts.paste')}</td>
            </tr>
            <tr>
                <td>Ctrl+ Z</td>
                <td>${getText('js.ec.workflow.config.tool.cancel')}</td>
            </tr>
            <tr>
                <td>Ctrl+ Y</td>
                <td>${getText('js.ec.workflow.config.tool.redo')}</td>
            </tr>
            <tr>
                <td>Ctrl+ -</td>
                <td>${getText('js.ec.workflow.config.shortcuts.viewNarrow')}</td>
            </tr>
            <tr>
                <td>Ctrl+ +</td>
                <td>${getText('js.ec.workflow.config.shortcuts.viewEnlarge')}</td>
            </tr>
            <tr>
                <td>Ctrl+ ↑ ↓ ← →</td>
                <td>${getText('js.ec.workflow.config.shortcuts.move')}</td>
            </tr>
            <tr>
                <td>Ctrl+ A</td>
                <td>${getText('js.ec.workflow.config.shortcuts.selectAll')}</td>
            </tr>
        </table>
    </div>
</script>