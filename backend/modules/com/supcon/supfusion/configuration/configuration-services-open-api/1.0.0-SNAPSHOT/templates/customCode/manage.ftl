<link rel="stylesheet" href="/bap/static/codemirror/lib/codemirror.css">
<link rel="stylesheet" href="/bap/static/codemirror/theme/eclipse.css">
<link rel="stylesheet" href="/bap/static/codemirror/addon/dialog/dialog.css">
<script src="/bap/static/codemirror/lib/codemirror.js"></script>
<script src="/bap/static/codemirror/mode/javascript/javascript.js"></script>
<script src="/bap/static/codemirror/addon/edit/matchbrackets.js"></script>
<script src="/bap/static/codemirror/addon/fold/xml-fold.js"></script>
<script src="/bap/static/codemirror/addon/edit/matchtags.js"></script>
<script src="/bap/static/codemirror/addon/selection/active-line.js"></script>
<script src="/bap/static/codemirror/addon/dialog/dialog.js"></script>
<script src="/bap/static/codemirror/addon/search/searchcursor.js"></script>
<script src="/bap/static/codemirror/addon/search/search.js"></script>
<script src="/bap/static/codemirror/addon/search/jump-to-line.js"></script>
<script src="/bap/static/babel/babel-standalone.js?v=6.26.0"></script>

<style>
<!--
.CodeMirror {border: 1px solid #f5f5f5; font-size:13px;}
.ico_docu_edit{background-position:0px -6477px!important;}
-->
#left_in .etv-nav-em {
	display: inline-block;
}
.customcode_search_box {
	background-color: #fff;
    padding: 5px 0;
    position: absolute;
    width: 100%;
}

.customcode_search_input_wrap {
	width: 98%;
	height: 26px;
	position: relative;
}

.customcode_search_box.active .customcode_search_input_wrap{
	background-position: 0 -22px;
}

.customcode_search_input_wrap .icon {
	position: absolute;
	right: 4%;
	top: 7px;
	width: 13px;
	height: 13px;
	background-image: url( "/bap/static/new/img/search_code.png?t=20171208");
	background-position: -21px -5px;
}

.customcode_search_box.active .icon {
	background-position: -2px -5px;
}

.customcode_search_input {
	width: 96%;
	height: 26px;
	line-height: 26px;
	border: 1px solid #ddd;
	border-radius: 20px;
	-webkit-border-radius: 20px;
    -moz-border-radius: 20px;
    -ms-border-radius: 20px;
    -o-border-radius: 20px;
    outline: 0;
	background-color: transparent;
    box-sizing: border-box;
    margin-left: 11px;
    margin-top: 1px;
	padding-left: 15px;
	padding-right: 40px;
}

.customcode_search_result {
	position: absolute;
	top: 34px;
	background-color: #f5f5f5;
	width: 100%;
	left: 0;
	z-index: 21;
}

.customcode_search_list {
	border-bottom: 1px solid #d8d8d8;
	display: none;
}

.customcode_search_list li {
	padding: 6px 8px;
	color: #333;
	cursor: pointer;
}

.customcode_search_list li.active {
	background-color: #0f78bc;
	color:#fff;
}

.customcode_search_list .no_result {
	display: block;
	padding: 6px 8px;
	background-color: #eee;
}
.etv-navsets, .dlg-etv-navset {
    position: relative;
    zoom: 1;
    margin: 0px auto;
    overflow-y: hidden;
    padding-top: 0;
}
.etv-navsets .etv-scrollbar, .dlg-etv-navset .etv-scrollbar {
    height: 34px;
    overflow: hidden;
    position: relative;
    margin-bottom: 1px;
}
.etv-navsets .etv-scrollbar, .dlg-etv-navset .etv-scrollbar {
    border-bottom: none;
}
.etv-navsets .etv-nav, .etv-scrollbar .etv-nav {
    border-bottom: 1px solid #f5f5f5;
    height: 33px;
    padding-left: 0;
    display: block;
}
.etv-navsets .etv-nav li.selected, .dlg-etv-navset .etv-nav li.selected {
    background: url(/bap/static/skin/images/icon_blue.png) center -791px no-repeat;
}
.etv-navsets .etv-nav li, .dlg-etv-navset .etv-nav li {
	width: 32%;
    float: left;
    list-style: none;
    cursor: pointer;
    position: relative;
    top: 4px;
    margin-right: 2px;
    z-index: 12;
	text-align: center;
}
.etv-navsets .etv-nav li.selected .etv-nav-span, .dlg-etv-navset .etv-nav li.selected .etv-nav-span {
    color: #0f78bc;
    font-weight: bold;
}
/* .cui-main-title {
    background: url(/bap/static/datatable/assets/datatable-top.gif) repeat-x scroll 0 0 transparent;
    border-bottom: 1px solid #BBDDF1;
    height: 23px;
    line-height: 23px;
    overflow: hidden;
    position: relative;
    text-align: left;
    padding-left: 30px;
    width: 100%;
} */
.etv-nav-span, .etv-nav-span-r {
    height: 29px;
    line-height: 14px;
    float: left;
    color: #adadad;
    font-weight: bold;
}
.etv-nav-span {
	width: 100%;
	text-align: center;
	padding: 0;
}
div#scrollbar {
	background: #f5f5f5;
    width: 100% !important;
    position: absolute;
    left: 0;
    top: 0;
}
div.save-div {
    display: inline-block;
    width: 100%;
    background-color: #f5f5f5;
	height: 33px;
	line-height: 33px;
}
a.cui-btn {
    border: 1px solid #f5f5f5;
}
span.file-path {
	display: inline-block;
	margin-left: 20px;
	overflow: hidden;
	text-overflow:ellipsis;
	white-space: nowrap;
}
.ztree{
	width: 100%;
    position: absolute;
    top: 36px;
	bottom: 0;
    overflow: auto;
}
.ztree li a.curSelectedNode {
	opacity: 1;
}
#allCustomCodeList, #allCustomCodeList_service, #allCustomCodeList_view {
	position: relative;
    width: 100%;
}
.etv-content {
	width: 100% !important;
    position: absolute;
    top: 34px;
    left: 0;
}
</style>
<input type="hidden" name="_editName" value="">
<input type="hidden" name="_path" value="">
<input type="hidden" name="_modelCode" value="">
<input type="hidden" name="_editable" value="0">
<@frameset id="ec_model_manage">
	<@frame id="ec_model_manage_top" region="north" height=39 region="north">
		<div class="cui-main-title">
	     	<@loadpanel text="${getText('ec.configMenu.customCode')}" />
	     	<span id="customCodeTreeHelpInfo" class="baphelp-icon helptip-mark" style="position: relative;left: -22px;top: 3px;"></span>
			<div id="customCodeTreeHelpInfoRef" style="display:none">
				<p class="baphelp-info">自定义代码左侧树中可以实现快速定位功能</p>
				<p class="baphelp-info" style="text-indent:2em;">"all"页签里面包含该模块下所有有效的文件。</p>
				<p class="baphelp-info" style="text-indent:2em;">"service"页签包含该模块下"service\src\main\java\com\supcon\orchid\模块名称"路径下面所有的文件。</p>
				<p class="baphelp-info" style="text-indent:2em;">"view"页签包含"service\src\main\resources\views\模块名称"路径下面所有的文件。</p>		
				<p class="baphelp-info" style="text-indent:2em;">all页签下面的搜索，搜索范围为整个模块的有效文件。serivce页签下面的搜索，搜索范围为"service\src\main\java\com\supcon\orchid\模块名称"路径下面的有效文件。view页签下面的搜索，搜索范围为"service\src\main\resources\views\模块名称"路径下面的有效文件。</p>
				<p class="baphelp-hint">注意：搜索关键字可以包含"*"。例："MES*.java" 搜索以"MES"开头，包含".java"名称的文件；*.java、*areaListRef*fastquery*datatable.ftl、*Basic*ServiceImpl.java。搜索时，搜索范围只针对文件的搜索，不搜索文件夹。搜索框输入关键字，自动进行搜索，键盘上下键可以再配合回车键选择指定的记录。</p>
			</div>
		</div>
	</@frame>
	<@frame id="left_in" class="etv-navsets" region="west" width=250 offsetH=6 resize=true style="overflow:hidden;z-index:100;position: relative;">
		    <ul class="etv-nav">  
		        <li>all</li>  
		        <li>service</li>  
		        <li>view</li>  
		    </ul>             
		    <div class="etv-content">  
		        <div id="allCustomCodeList">
				</div>  
		        <div id="allCustomCodeList_service">
				</div>  
		        <div id="allCustomCodeList_view">
		        </div>  
		    </div>  
		
	</@frame>
	<@frame id="ec_project_datamodel_container_main" region="center" offsetH=4>
	<div class="save-div">
		<span id="file-path" class="file-path"></span>
		<a class="cui-btn mr10 cui-btn-eighteen-dt-op-save" href="#" style="position:relative;top:3px;font-size:14px" onclick="ec.customcode.saveCustomCode(ec.customcode.editor);"><span i18n="ec.view.addCol">${getText('ec.customCode.save')}(<span style="text-decoration:underline;">S</span>)</span></a>
		<a id="customCodePublishBtn" class="cui-btn mr10 cui-btn-eighteen-dt-op-publish" href="#" style="position:relative;top:3px;font-size:14px;display:none" onclick="ec.customcode.saveCustomCode(ec.customcode.editor, true);"><span i18n="ec.view.addCol">${getText('ec.customCode.publish')}</span></a>
		<a id="customCodeHotDeploy" class="cui-btn mr10 cui-btn-eighteen-dt-op-publish" href="#" style="position:relative;top:3px;font-size:14px;display:none" onclick="ec.customcode.saveCustomCode(ec.customcode.editor, false, true);"><span i18n="ec.view.addCol">${getText('ec.customCode.publish')}</span></a>
		<span id="customCodeHotDeployHelpInfo" class="baphelp-icon helptip-mark helptip-mark-right" style="display:none;position: relative;top: 6px;"></span>
		<div id="customCodeHotDeployHelpInfoRef" style="display:none">
			<p class="baphelp-info">java代码的"保存并发布"实现自定义代码的保存及热更新</p>
			<p class="baphelp-info" style="text-indent:2em;">1.首先进行自定义代码的保存，保存成功之后，进行整个模块的代码编译。如果编译出错，将不再进行代码热更新处理。编译出错时，可以下载完整的编译日志，但是之前保存成功的自定义代码不会进行事物回滚。</p>
			<p class="baphelp-info" style="text-indent:2em;">2.编译成功之后，进行代码的热更新。如果改动了非方法区的代码，也会进行热更新，但是非方法区的代码改动之后不会生效。</p>
			<!-- p class="baphelp-info" style="text-indent:2em;">目前只有打开service目录下面的java文件可以看到"保存并发布"按钮。ps:打开静态资源文件和ftl看到的是另外一个"保存并发布"按钮。</p -->		
			<p class="baphelp-hint"><span style="color:red;">注意：目前热更新只支持java方法区里面的代码的热更新</span></p>
		</div>
	</div>
	<form>
		<textarea id="customcode" name="customcode"></textarea>
	</form>
	</@frame>
</@frameset>
<script>
CUI.ns("ec.customcode");
ec.customcode.node = null;
setTimeout(function() {
	ec.customcode.editor = CodeMirror.fromTextArea(document.getElementById("customcode"), {
		lineNumbers: true,
		theme: 'eclipse',
		styleActiveLine: true,
		matchBrackets: true,
		matchTags: {bothTags: true},
		extraKeys : {
			"Ctrl-S" : function(cm) {
				ec.customcode.saveCustomCode(cm);
			},
			"Alt-F": "findPersistent"
		}
	});
	ec.customcode.editor.on("change", function(cm, changes){
		if(changes.origin != 'setValue') {
			$('#' + ec.customcode.node.tId + "_ico").addClass("ico_docu_edit");
		}
	});
	_init_size();
	$('#customCodeHotDeployHelpInfo').helptip({refElm: "#customCodeHotDeployHelpInfoRef", html: true , isCustom :false, width: 460 , title :"说明"});
	var treeHelpInfo = $("#customCodeTreeHelpInfo");
	var loadPanel = $("#localLoadPanel");
	loadPanel.before(treeHelpInfo);
	$('#customCodeTreeHelpInfo').helptip({refElm: "#customCodeTreeHelpInfoRef", html: true , isCustom :false, width: 460 , title :"说明"});
}, 100);
//自动设置中间编辑区域的宽度
function _init_size(){
	$('.CodeMirror').height($(window).height() - 120);
	$('.CodeMirror-gutters').height($(window).height() - 120);
}
$(window).resize(function(){
	_init_size();				
});
ec.customcode.dealCustomCode = function(oNode) {
	if(oNode.path && !oNode.isDir) {
		if(null == ec.customcode.node || (ec.customcode.node != null && (ec.customcode.node.name != oNode.name || ec.customcode.node.path != oNode.path))) {
			if(ec.customcode.node && $('#' + ec.customcode.node.tId + "_ico").hasClass('ico_docu_edit')) {
				if(!confirm(ec.customcode.node.name + "${getText('ec.custom.save.update')}")){
					$('#' + oNode.tId + "_a").removeClass('curSelectedNode');
					$('#' + ec.customcode.node.tId + "_a").addClass('curSelectedNode');
					return false;
				}
			}
			$.ajax({
				url: '/msService/ec/customCode/editContent',
				type: 'post',
				async: false,
				data: {
					"path" : oNode.path
				},
				success: function(msg) {
					if(ec.customcode.node) {
						$('#' + ec.customcode.node.tId + "_ico").removeClass("ico_docu_edit");
						$('#' + ec.customcode.node.tId + "_a").removeClass('curSelectedNode');
					}
					ec.customcode.node = oNode;
					ec.customcode.editor.doc.setValue(msg);
					if(oNode.path && (oNode.path.endsWith(".ftl") || oNode.path.endsWith(".js") || oNode.path.endsWith(".css") || oNode.path.endsWith(".jsx") || oNode.path.endsWith(".html"))) {
						$('#customCodePublishBtn').show();
					} else {
						$('#customCodePublishBtn').hide();
					}
					if(oNode.path && oNode.path.endsWith(".java") && (oNode.path.indexOf("bap-workspace\\generate\\${entity.module.code}\\service") > -1 || oNode.path.indexOf("bap-workspace/generate/${entity.module.code}/service") > -1)){		 //只在services下面的java文件中显示新添加的保存并发布按钮
						$("#customCodeHotDeploy").show();
						$("#customCodeHotDeployHelpInfo").show();
					} else {
						$('#customCodeHotDeploy').hide();
						$("#customCodeHotDeployHelpInfo").hide();
					}
				}
			});
		} 
	}
	/*
	var showName = oNode.path;
	var prefix = "bap-workspace\\generate\\${entity.module.code}";
	if(undefined != showName && showName.indexOf(prefix) > -1){
		showName = showName.substring(prefix.length + 1);
	} else {
		showName = "";
	}
	$("#file-path").text(showName);
	$("#file-path").attr("title", showName);
	*/
}
/**
*	双击展现第一个子节点	
*/
ec.customcode.dblClickCustomCode = function(treeId,oNode) {
	var treeObj = $.fn.zTree.getZTreeObj(treeId); //获取ztree对象
	var selectNode = treeObj.getSelectedNodes();//获取当前节点
	if (selectNode[0].open) {
		treeObj.expandNode(selectNode[0], true, true, true);
	} else {
		treeObj.expandNode(selectNode[0], false ,true ,true);
	}
}
ec.customcode.saveCustomCode = function(cm, flag, hotDeploy) {
	if(ec.customcode.node == null) {
		CUI.Dialog.alert("${getText('ec.business.SelectRow')}");
		return false;
	}
	CUI.Dialog.confirm(  
	    '${getText("ec.custom.save.choise")}',  
	    function(){
	    	this.close();
	    	CUI.Dialog.toggleAllButton('design-button',true,true);
	    	
	    	var data = {
				"codeContent" : cm.getValue(),
				"path" : ec.customcode.node.path,
				"publishEnabled" : flag,
				"entityCode" : 'HierarchicalMod_1.0.0_tank',
				"hotDeploy" : hotDeploy
			};
			var customFiles = ['head.js', 'body.js'];
			if (data.codeContent) {
				$.each(customFiles, function(i, f){
					if (data.path.endsWith(f)){
						data.jses5 = Babel.t5(data.codeContent);
						return false;
					}
				});
			}
	    		    	
			$.ajax({
				url: '/msService/ec/customCode/saveContent',
				type: 'post',
				async: true,
				data: data,
				success: function(msg) {
					CUI.Dialog.toggleAllButton('design-button',true,true);
					if(msg && msg.success) {
						$('#' + ec.customcode.node.tId + "_ico").removeClass('ico_docu_edit');
						CUI.Dialog.alert("处理成功");
					} else {
						CUI.Dialog.alert("处理失败");
					}
				},
				error: function(msg){
					CUI.Dialog.toggleAllButton('design-button',true,true);
					var errMsg = "";
					if(msg && msg.responseText){
						errMsg = $.parseJSON(msg.responseText).exceptionMsg;
					}
					if(undefined != hotDeploy && hotDeploy){
						errMsg = errMsg+ '<a class="cui-link-dialog" style="color:#0f78bc" href="/msService/ec/customCode/down-log">下载详细日志</a>';
					}					
					CUI.Dialog.alert(errMsg);
				}
			});
		},
	    function(){},
	    '保存',
	    70, 
	    400  
	);
}
ec.customcode.findNode = function(filePath,treeObj,node){
	if(undefined != node && node.isParent){
		var path = node.path;
		if(undefined != filePath && "" != filePath && undefined != path && "" != path){
			if(filePath.indexOf(path + "\\") > -1 || "-1" == path){
				var childrenNodes = ec.customcode.expandNode(treeObj,node);
				for(var i=0;i<childrenNodes.length;i++){
					var childrenNode = childrenNodes[i];
					var returnFlag = ec.customcode.findNode(filePath,treeObj,childrenNode);
					if(returnFlag && !childrenNode.isDir){
						if(childrenNode.path == filePath){
							$('#' + childrenNode.tId + "_a").addClass('curSelectedNode');
							ec.customcode.dealCustomCode(childrenNode);
							return true;
						}						
					}
				}
			}
		}
		return false;
	}else{
		return true;
	}
}
//展开子节点
ec.customcode.expandNode = function(treeObj,node){
	if(!node.open){
		treeObj.expandNode(node,true);
	}	
	return node.children;
}

//遍历子节点
ec.customcode.traversalNode = function(treeObj,node){
	var childrenNodes = ec.customcode.expandNode(treeObj,node);
	for(var i=0;i<childrenNodes.length;i++){
		ec.customcode.traversalNode(treeObj,childrenNodes[i]);
	}
}

$(function(){  
     new CUI.TabView("left_in");
     $("#allCustomCodeList").load("/msService/ec/customCode/loadCustomCodeList?type=1&entityCode=${entityCode!}");
     $(".etv-nav li:eq(1)").one("click", function(event){
     	$("#allCustomCodeList_service").load("/msService/ec/customCode/loadCustomCodeList?type=2&entityCode=${entityCode!}");
     });
     $(".etv-nav li:eq(2)").one("click", function(event){
     	$("#allCustomCodeList_view").load("/msService/ec/customCode/loadCustomCodeList?type=3&entityCode=${entityCode!}");
     });     
     
});
</script>
