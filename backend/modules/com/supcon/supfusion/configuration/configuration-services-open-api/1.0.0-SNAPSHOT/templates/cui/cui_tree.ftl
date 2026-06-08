<#-- ==================================== new Tree (since version 2.5)  ==================================== -->
<#macro tree id,dataUrl,rootName,rootId="-1",movUrl="",callback="{}",nameCol="name",isSystemCode=false, isOnlySelectLeaf=false, customOncheckCallback=false, firstLoad=true,selectedMultiEnable=false,checkboxEnable=false,paramId="id",params="",autoExpand=true,isCopy=false,isMove=false,canMemory=true,resizeHeight=true>
	<#if params != ''>
	<#assign paramList = params?split(',')>
	</#if>
	<ul id="${id}" class="ztree"></ul>
	<script type="text/javascript">
		var ${id};
		var callback = ${callback};
		var ${id}_expandFlagOnce = true;
		//2016.12.24 by qin
		var _dataUrl = "${dataUrl}";
		_dataUrl = _dataUrl.substring(_dataUrl.indexOf("?"));
		if(_dataUrl.match(/systemCode.id=.*/)!=null){//系统编码类型
			var systemCodes = _dataUrl.match(/systemCode.id=.*/)[0];
			systemCodes = systemCodes.split(',');
			systemCodes.shift();
			systemCodes.pop();
		}
		callback.onAsyncSuccess=zTreeOnAsyncSuccess;
		callback.beforeDrop=zTreeBeforeDrop;
		<#if !customOncheckCallback >
			callback.onCheck=zTreeOnCheck;
		</#if>
		<#if isOnlySelectLeaf && isSystemCode>
			callback.onClick=onClickExpandNode;
		</#if>

		//isOnlySelectLeaf为true时，只能选择叶子节点，并且点击非叶子节点展开其子节点
		function onClickExpandNode(event,treeId,treeNode) {
			var zTree = $.fn.zTree.getZTreeObj(treeId);
			if (treeNode.isParent) {
				zTree.cancelSelectedNode();
			}
			if (treeNode.open) {
				zTree.expandNode(treeNode, false, false);
			} else {
				zTree.expandNode(treeNode, true, false);
			}
			event.stopPropagation();
		}
		//如果父节点是选中状态，异步加载成功后,将子节点设为选中状态
		function zTreeOnAsyncSuccess(event, treeId, treeNode, msg) {
			var zTree = $.fn.zTree.getZTreeObj(treeId);
			if(systemCodes){//系统编码
				init_check(systemCodes,zTree,treeNode);
				return;
			}
			if(treeNode.isParent && treeNode.checked){
				for(var i=0,c=treeNode.children.length; i<c; i++){
					treeNode.children[i].checked=true;
					if(treeNode.children[i].isParent){
						treeNode.children[i].check_Child_State = 2;
					}else{
						treeNode.children[i].check_Child_State = -1;
					}
					treeNode.check_Child_State = 2;
					zTree.updateNode(treeNode.children[i]);
					checkExpand(zTree,treeNode.children[i]);//再次验证子节点并进行异步展开
				}
			}

			if(${id}_expandFlagOnce){
				var expandFlag = ${autoExpand?string};
				var canMemory = ${canMemory?string};
				if(expandFlag == true || expandFlag == 'true'){
					if(canMemory == true || canMemory == 'true'){
						var curNodeLayRec = CUI.getCookie('nodeCookie_' + treeId);
					}else{
						var curNodeLayRec = "";
					}
					if(curNodeLayRec != undefined && curNodeLayRec !=null && curNodeLayRec != ""){
						var tempNode = zTree.getNodeByParam("id", ${rootId}, null);
						if(curNodeLayRec.indexOf("-")>0){
							var nodeIds = curNodeLayRec.split("-");
							var l = nodeIds.length - 1;
							for(var j=0;j<l;j++){
								var node = zTree.getNodeByParam("id", nodeIds[j], tempNode);
								zTree.expandNode(node,true);
								tempNode = node;
							}
							var lastNode = zTree.getNodeByParam("id", nodeIds[l], tempNode);

							if(lastNode != null){
								zTree.selectNode(lastNode);
								setTimeout(function(){$("#"+lastNode.tId+"_a").trigger('click');},800);
								${id}_expandFlagOnce = false;
							}
						}else{
							var root = zTree.getNodeByParam("id", curNodeLayRec, tempNode);
							if(root != null){
								zTree.selectNode(root);
								setTimeout(function(){$("#"+root.tId+"_a").trigger('click');},800);
								${id}_expandFlagOnce = false;
							}
						}
					}else{
						${id}_expandFlagOnce = false;
					}
					document.getElementById( '${id}' ).parentNode.scrollLeft = 0;
				}
			}
			if(zTree.setting.callback.customOnAsyncSuccessMethod) {
				zTree.setting.callback.customOnAsyncSuccessMethod(event, treeId, treeNode, msg);
			}
		}
		function zTreeBeforeDrop(treeId, treeNodes, targetNode, moveType) {
		    return !(targetNode == null || (moveType != "inner" && !targetNode.parentTId));
		};

		/*初始化选中节点状态*/
		function init_check(systemCodes,zTree,treeNode){
			var treeObj = zTree;
			if(systemCodes.length>0){
				for (var i=0;i<systemCodes.length;i++) {
					var nodes = zTree.getNodesByParam("id", systemCodes[i], null);
					if (nodes.length > 0) {
						var nodesParent = zTree.getNodesByParam("id", nodes[0].parentId, null);
						zTree.checkNode(nodes[0], true, true);
						zTree.expandNode(nodesParent[0], true, true, true);
						if (systemCodes.length == 1 && "${id}".match("singleCodeTree") != null) {
							zTree.selectNode(nodes[0]);
						}
					}
				}
			}
		}
		/*检测子节点与父节点的选中状态的联动*/
		function updateCheck(zTree,treeNode){
			for(var j=0;j<treeNode.children.length;j++){
					if(treeNode.children[j].checked==false){
						break;
					}
			}
			if(j==treeNode.children.length){
				zTree.updateNode(treeNode.children[0],true);//true可以让父子选中状态联动
			}
		}
		/*每次点击checkbox时做一次判断*/
		function zTreeOnCheck(event, treeId, treeNode) {
		 	 var zTree = $.fn.zTree.getZTreeObj(treeId);
			if(treeNode.level==0){
				var nodes = treeNode.children;
				if($(nodes).length>0){
					for(var i=0; i<nodes.length;i++){
						checkExpand(zTree,nodes[i]);
					}
				}
			}else{
				checkExpand(zTree,treeNode);
			}

		 /*	 var nodes = zTree.getCheckedNodes(true);
		 	 for (var i = 0; i < nodes.length; i++) {

	             if(nodes[i].isParent && nodes[i].getCheckStatus().half){
	             	nodes[i].check_Child_State = 1;
	             	nodes[i].getCheckStatus().half = false;
	             	nodes[i].checked = false;
	             	zTree.updateNode(nodes[i]);

             }

			 var parentNode = treeNode.getParentNode();
			 var checkChildState =parentNode.check_Child_State;
			 if(parentNode != null){
			 	if(checkChildState == 1){
			 		parentNode.getCheckStatus().half = false;
			 		parentNode.checked = false;
			 		zTree.updateNode(parentNode);
			 	}
			 }*/
		}
		/* 异步展开当前节点以及所有子节点 19.12.2016 by qin  */
		function checkExpand(zTree,treeNode){
			var Nodes = treeNode.children;
			if(treeNode.isParent&&$(Nodes).length==0){
				zTree.reAsyncChildNodes(treeNode, "refresh");
			}
		}
		(function($){
			var initHeight = function(){
		        $('.ztree').each(function(){
		        	if($(this).prev().hasClass("tree-companylist")){
		        		$(this).height($(this).parent().height() - 23 - 11);
					} else if ($(this).prev().hasClass("customcode_search_box")) {
						$(this).height($(this).parent().parent().parent().height() - 70 - 11);
					}else{
			            $(this).height($(this).parent().height() - 11);
					}

		        });
			};
			var key;
			<#if isSystemCode == true>
				key = {
					name: "value",
	            	children: "children2"
				};
			<#else>
				key = {
					name: "${nameCol}",
	            	children: "children"
				};
			</#if>
		    var setting = {
		    	view:{
		    		expandSpeed: "",
		    		selectedMulti: ${selectedMultiEnable?string},
		    		showLine: false
		    	},
	            data:{
	            	key: key
	            },
	            async:{
	            	enable: true,
	            	url: "${dataUrl}",
	            	autoParam: ["id=${paramId}"<#if paramList??><#list paramList as param>,"${param}"</#list></#if>]
	            },
	            movUrl:"${movUrl}",
	            callback:callback,
	            check:{
	            	chkboxType:{"Y":"ps","N":"ps"},
	            	chkStyle:"checkbox",
	            	enable: ${checkboxEnable?string}
	            },
	            edit: {
					enable: true,
					showRemoveBtn:false,
					showRenameBtn:false,
					drag: {
						autoExpandTrigger: false,
						isCopy: ${isCopy?string},
						isMove: ${isMove?string},
						prev: true,
						next: true,
						inner: true,
						minMoveSize: 5,
						borderMax: 10,
						borderMin: -5,
						maxShowNodeNum: 5,
						autoOpenTime: 500
					}
				}
	        };
		    var zTreeNodes = [{
	            id: ${rootId},
	            ${nameCol}: "${rootName?html}",
	            <#if paramList??><#list paramList as param><#if param != 'id' && param != 'name' && param != 'isParent' && param != 'companyFlag'>${param}:"-1",</#if></#list></#if>
	            <#if paramList??><#list paramList as param><#if param == 'companyFlag'>companyFlag:<#if rootId=="-1">false<#else>true</#if>,</#if></#list></#if>
	            isParent: true
	        }];
			$(function(){
				var tree = ${id} = $.fn.zTree.init($('#${id}'),setting,zTreeNodes);
		        $(window).resize(function(){
					var flag = ${resizeHeight?string};
					if(flag == true || flag == 'true') initHeight();
		        });
			<#if firstLoad>
		        var rootNode = tree.getNodeByParam("id", ${rootId});
		        tree.reAsyncChildNodes(rootNode, "refresh");
		    </#if>	
			
		        
			});
		})(jQuery);
	</script>
</#macro>


<#-- <@tree id="codeTypeTree" rootName="${getText('foundation.systemcode.typeTree')}" 
onclick="foundation.systemCode.showSystemClass" pageset="[]" defaultIcon="true"  border="1" jsonIncludes="*" jsonExcludes="" /> -->
<#macro tree1 id,rootName,pageset,showfield="",showimgname="",drag="",setImgfn="",defaultIcon="",rootName="",autoexptag="",border="",url="",condition="",showSetPath="",base="",width=0,showValuePath="",cssClass="",cssStyle="",treetype="",branchname="",onclick="",rootClick="",rootAttr="",toggleclick="">
	<div id="${id}" <#if cssClass != ""> class="${cssClass}"<#else> class="rootCss" </#if> <#if cssStyle!=""> style="${cssStyle}"</#if>>
	<#if rootName??><span id="${id}_virtual_root" class="virtual-root">${rootName}</span></#if>
	</div>
	<script type="text/javascript">
	 	var BASICPATH = '${base}';
	 	var tree_${id} = null;
	 	YAHOO.util.Event.onDOMReady(function() {
			tree_${id} = new CUI.treeview("${id}");
			tree_${id}.loadParam = {
				    <#if pageset??>DataSet: ${pageset},</#if>
				    TreeType      : "${treetype}",
				    SelectLeaf    : false,
				    BranchField   : "${branchname}",
				    NodeClick     : "${onclick}",
				    rootClick     : "${rootClick}",
				    <#if rootAttr != "">rootAttr      : ${rootAttr},</#if>
				    toggleClick   : "${toggleclick}",
				    ShowField     : "${showfield}",
				    ShowIcon	  : "${showimgname}",
	 				TreeDrag      : "${drag}",
	 				SetImgfn      : "${setImgfn}",
	 				DefaultIcon   : "${defaultIcon}",
	 				<#if rootName!="">rootName : "${rootName}",</#if>
	 				<#if autoexptag!="">autoExpTag : "${autoexptag}",</#if>
	 				Border        : "${border}",
	 				sUrl          : "${url}",
				    ClassCondition: "${condition}",
				    ShowSetPath   : "${showSetPath}",
				    width		  : ${width},
					ShowValuePath : "${showValuePath}"
		  	}
		  	tree_${id}.init();
		});
	</script>
</#macro>