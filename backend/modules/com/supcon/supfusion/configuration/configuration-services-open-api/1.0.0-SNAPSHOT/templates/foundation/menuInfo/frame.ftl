<#if (Parameters.openType)?default('page') != 'dialog'>
<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>${getText('foundation.basic.menuManagement')}</title>
<@maincss/>
<@mainjs/>
</head>
<body>
<@loadpanel></@loadpanel>
<@errorbar id="workbenchErrorBar" offsetY=0 />
</#if>
<style type="text/css">
.ewc-dialog-el{height:100%;}
.datagrid-wrapper .paginatorbar-operatebar a{margin-left:1px;}
.datagrid-wrapper .paginatorbar-operatebar a:hover{margin-left:0px;}
#left_in{border-top:none;}
.menu-top td span{float:left;}
#murl{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;width:400px;}
#foundation_menuInfo_frame_OperateBar{background-color:#f3f3f3;}
#menuInfoListTableContentDiv{margin:0 10px;float:left;*float:none;height:22px;border:1px solid #baddf1;border-bottom:0px;background:url("/bap/static/datatable/assets/roletop.gif") repeat-x;}
</style>
<@frameset id="foundation_menuInfo_frame_OperateBar"><#-- id: module_entity_function -->
	 <@frame id="left_in" region="west" width=200 resize=true style="overflow-y:auto;overflow-x:hidden;">
     	<@tree id="menuInfoTree" dataUrl="/msService/ec/foundation/menuInfo/listChildren" rootName="${getText('foundation.menuinfo.list')}"
     	   callback="{onClick:function(event,treeId,node){
			   $('#base_menuInfoManager_addMenu').show();
			   $('#base_menuInfoManager_modifyMenu').show();
			   $('#base_menuInfoManager_deleteMenu').show();
			   $('#base_menuInfoManager_sortitem').show();
			   $('#base_menuInfoManager_move').show();
			   $('#base_menuInfoManager_copy').show();
				  				     	   
	     	   if(node.stFlag!=null){
	     	    	if(node.stFlag.id=='SYSTEM/PIMS'){
	     	    	    $('#base_menuInfoManager_addMenu').hide();
					    $('#base_menuInfoManager_modifyMenu').hide();
					    $('#base_menuInfoManager_deleteMenu').hide();
					    $('#base_menuInfoManager_sortitem').hide();
					    $('#base_menuInfoManager_move').hide();
	     	    	}
	     	   }
	     	   foundation_menuInfo_frame_OperateBarWidget.resize();
	     	   foundation.menuInfo.showMenuInfo(node);
	     	   CUI.setCookie('nodeCookie_' + treeId,node.layRec,'d1');
     	   }}"/>
     </@frame>
     
     <div style="position:absolute;bottom:0px;z-index:55;">
		<div id="opratebar" class="opratebar">
			<@operatebar operates="code:base_menuInfoManager_addMenu||name:${getHtmlText('foundation.basic.menuManagement.addMenu')}||iconcls:add||onclick:foundation.menuInfo.menuInfoMuluManage('add',true);
								   code:base_menuInfoManager_modifyMenu||name:${getHtmlText('foundation.basic.menuManagement.modifyMenu')}||iconcls:edit||onclick:foundation.menuInfo.menuInfoMuluManage('modify',true);
								   code:base_menuInfoManager_deleteMenu||name:${getHtmlText('foundation.basic.menuManagement.deleteMenu')}||iconcls:del||onclick:foundation.menuInfo.menuInfoMuluManage('delete',true); 
								   code:base_menuInfoManager_sortitem||name:${getHtmlText('foundation.basic.menuManagement.sortMenu')}||iconcls:sort||onclick:foundation.menuInfo.sortItem();
								   code:base_menuInfoManager_move||name:${getHtmlText('foundation.basic.menuManagement.move')}||iconcls:move||onclick:foundation.menuInfo.menuInfoManage('move'); 
								   code:base_menuInfoManager_copy||name:${getHtmlText('foundation.basic.menuManagement.copy')}||iconcls:copy||onclick:foundation.menuInfo.menuInfoManage('copy');"
								   operateType=""/>
		</div>
	 </div>
	 
      <@frame id="infoContent" region="center" offsetH=4>
      
       <div id="menuinfoQueryDiv">
      	<form id="QueryForm" onsubmit="return false;">
	       <@quickquery formId="QueryForm"  fieldcodes="querymenuName:foundation.menuinfo.menuname||queryopName:foundation.menuOperate.name||queryopCode:foundation.menuOperate.code" unique="LAST_QUERY_foundation_menuinfo_frame">
				       	<@queryfield formId="QueryForm" code="querymenuName" isCustomize=true >
				       	<input id="queryValue" class="cui-edit-field" name="queryValue">
				       	</@queryfield>
				       	<@queryfield formId="QueryForm" code="queryopName" isCustomize=true >
				       	<input id="queryValue" class="cui-edit-field" name="queryValue">
				       	</@queryfield>
				       	<@queryfield formId="QueryForm" code="queryopCode" isCustomize=true >
				       	<input id="queryValue" class="cui-edit-field" name="queryValue">
				       	</@queryfield>
				     	 <@querybutton formId="QueryForm" type="search" onclick="foundation.menuInfo.queryList()"/>
				 		 <@querybutton formId="QueryForm" type="clear"  />
			</@quickquery>
        </form>
	</div>
	<@datatable  formId="QueryForm" id="menuInfoListTable" transMethod="post" firstLoad=false dblclick="foundation.menuInfo.modifyMenuInfoFeatures" dtPage="pageMenuOperate" hidekey="['id','leaf','version','entityCode','menuInfo.id','menuInfo.stFlag.code']"   dataUrl="/foundation/menuInfo/queryList?a=-1" style="margin:0px 10px 4px;" >
	     <@operatebar operates="code:base_menuInfoManager_add||onclick:foundation.menuInfo.menuInfoFeatures('add');
					code:base_menuInfoManager_modify||onclick:foundation.menuInfo.menuInfoFeatures('modify');code:base_menuInfoManager_delete||onclick:foundation.menuInfo.menuInfoFeatures('delete');"resultType="json">
		</@operatebar>
		  <@datacolumn key="checkbox" type="checkbox" checkall="true" textalign="center" label="" width=60 />
	      <@datacolumn key="code" label="${getHtmlText('foundation.menuOperate.code')}"  width="200" />
		  <@datacolumn key="name" label="${getHtmlText('foundation.menuOperate.name')}"  width="200" />
		  <@datacolumn key="url" label="URL"  width="200" />
	</@datatable>
   </@frame>
</@frameset>

<script type="text/javascript" charset="utf-8" language="javascript">
(function(){

   	changeBtnStyle();//按钮交互效果
    //注册命名空间
    CUI.ns("foundation.menuInfo");
    
    foundation.menuInfo.modifyMenuInfoFeatures=function(event,obj){
    	foundation.menuInfo.menuInfoFeatures('modify');
    }
    
    foundation.menuInfo.addmenuInfoTop =function(Code,module,url,company){
		if($("#menuInfoListTableContentDiv").length == 0){
			var Top ="<div id='menuInfoListTableContentDiv'>"
						+"<table cellpadding='0' cellspacing='0' border='0' class='detail-top'>"
						+"<tr><td style='padding-left:5px;'><span>${getHtmlText('foundation.menuinfo.code')} :</span>"
						+"<span id='mCode' readonly='true' style='padding-right:20px;'>"+Code+"</span>"
						+"<span>${getHtmlText('foundation.menuinfo.module')} :</span><span  id='mmodulecode' readonly='true'>"+module+"</span>"
						+"<span  style='padding-left:20px;'>${getHtmlText('foundation.menuinfo.company')} :</span><span id='mcompany' readonly='true' style='padding-right:20px;'>"+dealInnerHtml(company)+"</span>"
					  //+"<span>URL :</span><span id='murl' readonly='true'>"+url+"</span>"
						+"</td></tr></table></div>";
			$(Top).insertAfter($("#QueryForm").eq(0));
	    	$("#menuInfoListTableContentDiv").width($("#menuInfoListTable .paginatorbar").eq(0).width()-2);
		}
		else{
		  CUI("#mCode").html(Code);
		  CUI("#mmodulecode").html(module);
		//  CUI("#murl").html(url);
		  CUI("#mcompany").html(dealInnerHtml(company));
		}
    }
    //处理转义
    var dealInnerHtml=function(text){
    	while(text.indexOf("<")>=0){
			text=text.replace("<","&lt;");
		}
		while(text.indexOf(">")>=0){
			text=text.replace(">","&gt;");
		}
		//while(text.indexOf(" ")>=0){
		//	text=text.replace(" ","");
		//}
		return text;
    }
    //处理转义
    var dealInnerHtml=function(text){
    	while(text.indexOf("<")>=0){
			text=text.replace("<","&lt;");
		}
		while(text.indexOf(">")>=0){
			text=text.replace(">","&gt;");
		}
		//while(text.indexOf(" ")>=0){
		//	text=text.replace(" ","");
		//}
		return text;
    }
    //编辑菜单操作的回调函数 
    foundation.menuInfo.showMenuOperateInfo=function(res){
    	if(res.dealSuccessFlag){
    		if(res.operateType=='saveFeatures'){
    			foundation_menuInfoFeatures_edit_formDialogErrorBarWidget.show("保存成功！正在关闭窗口...","s");
    		}
    		else if(res.operateType=='deleteFeatures'){
    			workbenchErrorBarWidget.showMessage("删除成功！正在刷新...","s");
    		}
    		
    		setTimeout(function(){
    			try{foundation.menuInfo.menuOperateDialog.close();}catch(e){};
	    		var menuInfoId=''
		    	if(menuInfoTree.getSelectedNodes()[0]!=null){
		    		menuInfoId=menuInfoTree.getSelectedNodes()[0].id;
		    	}
		    	if(CUI.trim(menuInfoId)==''){
		    		foundation.menuInfo.queryList();
		    	} else{
		    		var url="queryListFeatures?a=-1";
		    		var dataPost='';
		    		dataPost+="&menuInfo.id="+menuInfoId;
		    		var pageSize=CUI('input[name="menuInfoListTable_PageLink_PageCount"]').val();
		    		if(pageSize!=null&&pageSize!="undefined") {
		     			dataPost += "&pageMenuOperate.pageSize="+encodeURIComponent(pageSize);
		        	} 
	        	 	setTimeout(function(){
	        	 		datatable_menuInfoListTable.setRequestDataUrl(url,dataPost);
			 	 		datatable_menuInfoListTable._initDomElements();
			 	 	},200);
		    	}
		    	
    		},500);
    	}
    	else{
    		CUI.showErrorInfos(res);
    	}
    	
    }
    //打开页面是展现
    foundation.menuInfo.showMenuInfo = function(node){
		var id ='';
		var leaf="";
		var dataPost;
		if(node != null && node != undefined){
			id = node.id;
			leaf = node.leaf;	
			code=node.code;
			module=node.moduleCode;
		}else if(menuInfoTree.getSelectedNodes()[0] != null){
			id = menuInfoTree.getSelectedNodes()[0].id;
			leaf = menuInfoTree.getSelectedNodes()[0].leaf;	
			code=menuInfoTree.getSelectedNodes()[0].code;
			module=menuInfoTree.getSelectedNodes()[0].moduleCode;
		}
		var url="queryListFeatures?a=-1";
		var pageSize=CUI('input[name="menuInfoListTable_PageLink_PageCount"]').val();
		dataPost="&menuInfo.id=" + id;
		if(pageSize!=null&&pageSize!="undefined") {
     		dataPost += "&pageMenuOperate.pageSize="+encodeURIComponent(pageSize);
        } 
		$('input[name="menuInfo.id"]','#QueryForm').val(id);
		if(typeof(datatable_menuInfoListTable) != undefined&&id!=''&&id!=-1){
			CUI.post("/msService/ec/foundation/menuInfo/getInfo?menuInfo.id="+id+"&menuInfo.version="+getOperateRecordVersion(),function(res){
				var modulecode='';
				var url='';
				var menuCode = '';
				var shortName = '';
				if(res.moduleCode!=null){
					modulecode = res.moduleCode;
				}
				if(res.url!=null){
					url = res.url;
				}
				if(res.code!=null){
					code = res.code;
				}
				if(null != res.company && res.company.shortName!=null){
					shortName = res.company.shortName;
				}
				foundation.menuInfo.addmenuInfoTop(code,modulecode,url,shortName);
			}, "json");
			  setTimeout(function(){datatable_menuInfoListTable.setRequestDataUrl(url,dataPost);
			   datatable_menuInfoListTable._initDomElements();},200);
		 	 
		} else if(id==-1){
		    //先将PT上菜单信息的展示条remove掉
			var obj=CUI("#menuInfoListTableContentDiv");
			if(obj!=undefined&&obj.length>0){
				obj.remove();
			}
			//再重新查询PT中的数据 
			var dataPost="&menuInfo.id=-1";
			if(pageSize!=null&&pageSize!="undefined") {
     			dataPost += "&pageMenuOperate.pageSize="+encodeURIComponent(pageSize);
        	} 
			setTimeout(function(){
				datatable_menuInfoListTable.setRequestDataUrl(url,dataPost);
		 	 	datatable_menuInfoListTable._initDomElements();
		 	 },200);
		}
	};
	
	foundation.menuInfo.showButton=function(leaf){
		if(leaf){
			CUI('#base_menuInfoManager_addFolder').hide();
			CUI('#base_menuInfoManager_modifyFolder').hide();
			CUI('#base_menuInfoManager_deleteFolder').hide();
			CUI('#base_menuInfoManager_sortitem').hide();
			CUI('#base_menuInfoManager_addMenu').show();
			CUI('#base_menuInfoManager_modifyMenu').show();
			CUI('#base_menuInfoManager_deleteMenu').show();
			CUI('#base_menuInfoManager_add').show();
			CUI('#base_menuInfoManager_modify').show();
			CUI('#base_menuInfoManager_delete').show();
		}else{
			CUI('#base_menuInfoManager_addFolder').show();
			CUI('#base_menuInfoManager_modifyFolder').show();
			CUI('#base_menuInfoManager_deleteFolder').show();
			CUI('#base_menuInfoManager_addMenu').show();
			CUI('#base_menuInfoManager_sortitem').show();
			CUI('#base_menuInfoManager_modifyMenu').hide();
			CUI('#base_menuInfoManager_deleteMenu').hide();
			CUI('#base_menuInfoManager_add').hide();
			CUI('#base_menuInfoManager_modify').hide();
			CUI('#base_menuInfoManager_delete').hide();
		}
	}
	
	foundation.menuInfo.showFeaturesButton=function(){
	        CUI('#base_menuInfoManager_addFolder').hide();
			CUI('#base_menuInfoManager_modifyFolder').hide();
			CUI('#base_menuInfoManager_deleteFolder').hide();
			CUI('#base_menuInfoManager_addMenu').show();
			CUI('#base_menuInfoManager_sortitem').show();
			CUI('#base_menuInfoManager_modifyMenu').show();
			CUI('#base_menuInfoManager_deleteMenu').show();
			CUI('#base_menuInfoManager_add').show();
			CUI('#base_menuInfoManager_modify').show();
			CUI('#base_menuInfoManager_delete').show();
	}
     /**
	 * 刷新树
	 * @method foundation.menuInfo.refreshTree
	 * @param {Object} parentRes
	 * @public
	 */
	foundation.menuInfo.refreshTree = function(parentRes){
		var rootNode = menuInfoTree.getNodeByParam("id", -1);
		if (menuInfoTree.getSelectedNodes().length && !menuInfoTree.isSelectedNode(rootNode) ){
			menuInfoTree.cancelSelectedNode();
		}
		menuInfoTree.reAsyncChildNodes(rootNode, "refresh");		
	};
     
     /**
	 * //菜单的排序 
	 * @method foundation.menuInfo.sortitem
	 * @public
	 */
	foundation.menuInfo.sortItem = function(){
		//父节点为空则是添加菜单目录 //修改的话则要知道当前的选中的目录
		var Id = getMenuInfoId();
		var leaf ="";
		if(menuInfoTree.getSelectedNodes()[0] != null && Id!=""){  
			
			  CUI(function(){
				foundation.menuInfo.dialog = new CUI.Dialog({
					title: "菜单排序",
					url:"/foundation/menuInfo/sortitem/menuInfoColOrder?menuInfo.id="+Id,
					modal:true,
					//type:3,
					width:460,
					height:330,
					dragable:true,
					onload: 'foundation.menuInfo.initDialog',
					buttons:[
							{	name:"${getHtmlText('common.button.save')}",
							    id:"saveButton",
								handler:function(){foundation.menuInfo.SaveColOrder()}
							},
							{	name:"${getHtmlText('common.button.cancel')}",
								handler:function(){this.close()}
							}]
				})
				foundation.menuInfo.dialog.show();
		     });	  
	
		}else{
		 	var msg={};
	    	msg.exceptionMsg="${getHtmlText('foundation.menuinfo.checkselected')}"
	    	CUI.showErrorInfos(msg);
		  	return false;
		}
	} 
    
    
     /**
	 * 菜单的保存 
	 * @method foundation.menuInfo.sortitem
	 * @public
	 */
	foundation.menuInfo.SaveColOrder = function(){
		var orderMenuinfo='';
		CUI('tr[id^="menuInfoColOrder_"]').each(function(index,item){
			var id=CUI(this).attr("colid");
			orderMenuinfo+=";"+id+","+index;
		});
		orderorderMenuinfo=orderMenuinfo.substr(1);
		CUI("#orderMenuInfoColID").val(orderorderMenuinfo);
		CUI.post("/foundation/menuInfo/sortitem/orderMenuInfoCol-save", 
		CUI('#SubmitmenuInfoColOrderForm').serialize(), function(res){
			
			if(res.dealSuccessFlag == true){
				menuInfoTree_expandFlagOnce = true;
				foundation.menuInfo.showMenuInfo();
				foundation.menuInfo.refreshTree();
				menuInfoSortDialogErrorBarWidget.showMessage("设置成功","s");
				try{setTimeout(function(){foundation.menuInfo.dialog.close();},1500);}catch(e){}
			}else{
				menuInfoSortDialogErrorBarWidget.showMessage("foundation.infoSet.orderfailure","s");
			}
		
		
		});
	} 
     //菜单的查询 
	foundation.menuInfo.queryList = function(){
		CUI('#menuInfoListTableContentDiv').remove();
	    menuInfoTree.cancelSelectedNode();//查询后解除树的选中状态
	    var url = "";
		var pageSize=CUI('input[name="menuInfoListTable_PageLink_PageCount"]').val();
		if(typeof(datatable_menuInfoListTable) != undefined && datatable_menuInfoListTable!=null){
		    var param=CUI('#QueryForm_queryParam').val();
		    var value=CUI('#queryValue').val();
		   	url="queryListFeatures?a=-1";
		    dataPost="&queryParam="+param+"&queryValue="+value;
		    dataPost+="&"+QueryForm_getCookieParam();
		     //"&menuInfoCode="+encodeURIComponent(CUI('#menuInfoCode').val());
		    //dataPost += "&menuInfoName="+encodeURIComponent(CUI('#menuInfoName').val());
		    
			if(pageSize!=null&&pageSize!=undefined) {
				dataPost += "&pageMenuOperate.pageSize="+encodeURIComponent(pageSize);
			}
			datatable_menuInfoListTable.setRequestDataUrl(url,dataPost);
			setTimeout(function(){
				datatable_menuInfoListTable._initDomElements();
		 	},200);
		}else{
			url += "/foundation/menuInfo/list?a=-1";
			if(pageSize!=null&&pageSize!=undefined) {
				url +="&page.pageSize="+pageSize;
			}
			var menuInfoCode=encodeURIComponent(CUI('#menuInfoCode').val());
			url +="&menuInfoCode="+menuInfoCode;
			var menuInfoName=encodeURIComponent(CUI('#menuInfoName').val());
			url +="&menuInfoName="+menuInfoName;
			CUI('#infoContent').load(url);
		}
	};
  
     //进入菜单目录    
     //信息添加、修改、删除页面.
	foundation.menuInfo.menuInfoMuluManage = function(strType,leaf){
		var url="";
		//菜单添加需要URL 
		var id=getMenuInfoId();
		 if(strType!="add"&&(id==null||id=="")){
	   	 	var msg={};
	    	msg.exceptionMsg="${getHtmlText('foundation.menuinfo.checkselected')}"
	    	CUI.showErrorInfos(msg);
	    	return false;
	     }
     	//得到选中的ID 
	     if(strType=='add'){
	    	 url="/foundation/menuInfo/add/default";
	    	 if(id!=""){
	    	 	url+="?parentId="+id + "&moduleArtifact=${moduleArtifact!}";
	    	 } else {
	    	 	url+="?moduleArtifact=${moduleArtifact!}"
	    	 }
		 }else if(strType=='modify'){
		 	if(id==-1){
		 		var msg={};
		    	msg.exceptionMsg="${getHtmlText('foundation.menuinfo.notModifyMenu')}"
		    	CUI.showErrorInfos(msg);
		    	return false;
		 	}
		 	url="/foundation/menuInfo/modify/default?menuInfo.id=" + id + "&menuInfo.leaf="+leaf;
		}else if(strType=='delete'){
			if(id==-1){
		 		var msg={};
		    	msg.exceptionMsg="${getHtmlText('foundation.menuinfo.notModifyMenu')}"
		    	CUI.showErrorInfos(msg);
		    	return false;
		 	}
			CUI.Dialog.confirm("${getHtmlText('foundation.menuinfo.checkdelete')}", function(){
				CUI.post("/foundation/menuInfo/delete/default?menuInfo.id="+id+"&menuInfo.version="+getOperateRecordVersion(),foundation.menuInfo.callBackInfo, "json");
				menuInfoTree.cancelSelectedNode();
			}); 
			return false;
	  	}
	  	CUI(function(){
			foundation.menuInfo.menuInfoDialog = new CUI.Dialog({
				title: "${getHtmlText('foundation.menuinfo.manage')}",
				url:url,
				modal:true,
				type:4,
				dragable:true,
				buttons:[
						{	name:"${getHtmlText('common.button.save')}",
							handler:function(){foundation.menuInfo.saveMenu()}
						},
						{	name:"${getHtmlText('common.button.cancel')}",
							handler:function(){this.close()}
						}]
			})
		foundation.menuInfo.menuInfoDialog.show();
	});
	}
     
     //添加菜单的方法
     foundation.menuInfo.menuInfoManage = function(strType,leaf){
	     CUI('#menuInfoUrl').show();
	     var id = getMenuInfoId();
	     var name=getMenuInfoName();
	     var url="";
	     if(strType!="add"&&(id==null||id=="")){
	   	 	var msg={};
	    	msg.exceptionMsg="${getHtmlText('foundation.menuinfo.checkselected')}"
	    	CUI.showErrorInfos(msg);
	    	return false;
	     }
	     
		 if(strType=='add'){
		 	var leaf=0;
			if(menuInfoTree.getSelectedNodes()[0] != null ){  
				leaf = menuInfoTree.getSelectedNodes()[0].leaf;
			}else if(typeof(datatable_menuInfoListTable) != 'undefined'  &&  datatable_menuInfoListTable!=null&&datatable_menuInfoListTable.selectedRows>0){		 
			 	leaf = datatable_menuInfoListTable.selectedRows[0].leaf;
			}
			if(leaf==1){
				var msg={};
				msg.exceptionMsg="${getHtmlText('foundation.menuinfo.notCreateByMenu')}"
	    		CUI.showErrorInfos(msg);
	    		return false;
			}
		 	url="/foundation/menuInfo/add/default?type=menuInfo";
		 	if(id!=""){
	    	 	url+="&parentId="+id;
	    	 }
	    	  
		 }else if(strType=='modify'){
		 	
	    	url="/foundation/menuInfo/modify/default?menuInfo.id=" + id + "&menuInfo.leaf="+leaf+ "&type=menuInfo";
		 }else if(strType=='delete'){
	      	var deleteUrl="/foundation/menuInfo/delete/default?menuInfo.id="+id;
	      	CUI.Dialog.confirm("${getHtmlText('foundation.menuinfo.checkdelete')}", function(){
	      		
			 CUI.post("/foundation/menuInfo/delete/default?menuInfo.id="+id+"&menuInfo.version="+getOperateRecordVersion(),foundation.menuInfo.callBackInfo, "json");
			}); 
			return false;
		 }else if(strType=='move'){
		 	if(id==-1){
		 		var msg={};
		    	msg.exceptionMsg="${getHtmlText('foundation.menuinfo.notMoveMenu')}"
		    	CUI.showErrorInfos(msg);
		    	return false;
		 	}
		   //菜单的移动 
			var open_url= "/foundation/menuInfo/move/menuInfoTreeFrame?menuInfo.id=" + id + "&actionMode=move&menuInfo.name="+encodeURIComponent(name)+"&closePage=true&callBackFuncName=foundation.menuInfo.moveResult";
			var handle = null;
			var window_height = 550;
			var window_width  = 280;
			ShowStyle = "width = " + window_width + ",height=" + window_height + ",scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
			handle = window.open(open_url,"",ShowStyle);
			handle = null;
			return false;
			
		}else if(strType=='copy'){
			var leaf=0;
			if(menuInfoTree.getSelectedNodes()[0] != null ){  
				leaf = menuInfoTree.getSelectedNodes()[0].leaf;
			}else if(typeof(datatable_menuInfoListTable) != 'undefined'  &&  datatable_menuInfoListTable!=null&&datatable_menuInfoListTable.selectedRows>0){		 
			 	leaf = datatable_menuInfoListTable.selectedRows[0].leaf;
			}

		 	if(id==-1||leaf==false){
		 		var msg={};
		    	msg.exceptionMsg="${getHtmlText('foundation.menuinfo.notCopyMenu')}"
		    	CUI.showErrorInfos(msg);
		    	return false;
		 	}
		   //菜单的复制
			var open_url= "/foundation/menuInfo/copy/menuInfoTreeFrame?menuInfo.id=" + id + "&actionMode=copy&menuInfo.name="+encodeURIComponent(name)+"&closePage=true&callBackFuncName=foundation.menuInfo.copyResult";
			var handle = null;
			var window_height = 550;
			var window_width  = 280;
			ShowStyle = "width = " + window_width + ",height=" + window_height + ",scrollbars=yes,resizable =yes,toolbar=no,menubar=no,location=no,status=yes";
			handle = window.open(open_url,"",ShowStyle);
			handle = null;
			return false;
			
		}
		
		CUI(function(){
			foundation.menuInfo.menuInfoDialog = new CUI.Dialog({
				title: "${getHtmlText('foundation.menuinfo.manage')}",
				url:url,
				modal:true,
				type:3,
				dragable:true,
				buttons:[
						{	name:"${getHtmlText('common.button.save')}",
							handler:function(){foundation.menuInfo.saveMenu()}
						},
						{	name:"${getHtmlText('common.button.cancel')}",
							handler:function(){this.close()}
						}]
			})
		foundation.menuInfo.menuInfoDialog.show();
	});
    }
    foundation.menuInfo.saveMenu=function(){
    	if($("#menuInfoedit").val()==''){
			foundation_menuInfo_edit_formDialogErrorBarWidget.show("${getHtmlText('common.menu.moduleCode.null')}");
			return false;
		}
		var name=$('[name="international_menuInfoname_showName"]').val();
		if(name.indexOf('{')>-1||name.indexOf('}')>-1){
			error = "${getHtmlText('common.menu.moduleName.error1')}";
			foundation_menuInfo_edit_formDialogErrorBarWidget.show(error);
			return false;
		}
    	var url = $('[name="menuInfo.url"]').val();
    	if(url != undefined){
    		url = url.trim();
    	}
		if(url != undefined && url != ""){
			if(url.indexOf('\\')>-1){
				error = "${getHtmlText('common.menu.urlError.backslash')}";
				foundation_menuInfo_edit_formDialogErrorBarWidget.showMessage(error);
				return false;
			}
			var arrNames = url.split('/');
			var isSlashOnly = true;
			$.each(arrNames,function(index,value,array){
				value = value.trim();
				if(value != ""){
					isSlashOnly = false;
				}
			});
			if(isSlashOnly){
				error = "${getHtmlText('common.menu.urlError.slashOnly')}";
				foundation_menuInfo_edit_formDialogErrorBarWidget.showMessage(error);
				return false;
			}
		}
    	CUI('#foundation_menuInfo_edit_form').submit();
    }
   //移动回调函数移动的保存的方法啊
   foundation.menuInfo.moveResult = function(oMenuInfo){
     //得到选中的ID 
	 var id = getMenuInfoId(); 
	 CUI.ajax({
		url: '/foundation/menuInfo/move/moveResult',
		type: 'post',
		async: false,
		data: "menuInfo.id=" + id + "&moveParentId=" + oMenuInfo.menuinfoID,
		success: function(response) {
		 	if(response.dealSuccessFlag == true){
		 		workbenchErrorBarWidget.showMessage("${getHtmlText('foundation.common.dealandrefreshsuccessful')}","s");
		 		setTimeout(function(){
		 			foundation.menuInfo.refreshTree(response);
		 		},800);
			}
		}
	});
   }
   
   //复制回调函数,复制的保存的方法
   foundation.menuInfo.copyResult = function(oMenuInfo){
     //得到选中的ID 
	 var id = getMenuInfoId(); 
	 CUI.ajax({
		url: '/foundation/menuInfo/copy/copyResult',
		type: 'post',
		async: false,
		data: "menuInfo.id=" + id + "&copyParentId=" + oMenuInfo.menuinfoID,
		success: function(response) {
		 	if(response.dealSuccessFlag == true){
		 		workbenchErrorBarWidget.showMessage("${getHtmlText('foundation.common.dealandrefreshsuccessful')}","s");
		 		setTimeout(function(){
		 			menuInfoTree_expandFlagOnce = true;
		 			foundation.menuInfo.refreshTree(response);
		 		},800);
			}
		}
	});
   }
	var foundation_menuInfo_edit_form_data = CUI('input,select,textarea', CUI('#foundation_menuInfo_edit_form')).serializeObject();
	/**
	 * 显示编辑对话框
	 * @method showEditDialog
	 * @param {String} url
	 * @private
	 */
	foundation.menuInfo.showMenuInfoEditDialog = function(url,addFlag) { 
		// 初始化当前form
	   CUI.initForm('foundation_menuInfo_edit_div','',foundation_menuInfo_edit_form_data,null,
	   addFlag?[{id:'editMenuInfoCode',readonly:false}]:['editMenuInfoCode']);	
	  
		CUI.post(url, function(res){
			CUI.fillValues('foundation_menuInfo_edit_div','menuInfo',res);	
			foundation.menuInfo.dialog = new CUI.Dialog({
				title: "${getHtmlText('foundation.menuinfo.manage')}",
				elementId: "foundation_menuInfo_edit_div",
				formId: "foundation_menuInfo_edit_form",
				modal:true,
				type:4,
				onload: 'foundation.menuInfo.initDialog',
				buttons:[{	name:"${getHtmlText('common.button.save')}",
							handler:function(){CUI('#foundation_menuInfo_edit_form').submit();}
						},
						{	name:"${getHtmlText('common.button.cancel')}",
							handler:function(){this.close()}
						}]
			});
			foundation.menuInfo.dialog.show();
		}, "json");
	};


     /**
	 * 添加，删除，修改，移动完成后的回调函数
	 * @method foundation.menuInfo.callBackInfo
	 * @param {Object} res
	 * @public
	 */
	foundation.menuInfo.callBackInfo = function(res){
		if(res.dealSuccessFlag){
			if(res.operateType == 'save'){
				 foundation_menuInfo_edit_formDialogErrorBarWidget.show("${getHtmlText('foundation.common.saveandclosesuccessful')}","s");
			} else if(res.operateType == 'delete' || res.operateType == 'deleteFeatures') {
				workbenchErrorBarWidget.showMessage("${getHtmlText('foundation.common.deleteandrefreshsuccessful')}","s");
			} else if(res.operateType == 'move') {
				workbenchErrorBarWidget.showMessage("${getHtmlText('foundation.common.deleteandrefreshsuccessful')}","s");
			}else if(res.operateType == 'saveFeatures'){
			  foundation_menuInfoFeatures_edit_formDialogErrorBarWidget.show("${getHtmlText('foundation.common.saveandclosesuccessful')}","s");
			}
			menuInfoTree_expandFlagOnce = true;
			if(res.operateType != 'saveFeatures' && res.operateType != 'deleteFeatures'){
				foundation.menuInfo.refreshTree(res);
			}
			//foundation.menuInfo.refreshTree(res);
			//datatable_menuInfoListTable.delAllRows();
			foundation.menuInfo.showMenuInfo();
			setTimeout(function(){
				try{foundation.menuInfo.menuInfoDialog.close();}catch(e){}
				try{foundation.menuInfo.menuOperateDialog.close();}catch(e){}				
			},1000);
		} else {
			CUI.showErrorInfos(res);
		}
	};

    //添加操作的方法
    foundation.menuInfo.menuInfoFeatures= function(strType){
      //alert(menuInfoFeaturesListTable);
		if($("[id=other_config_div]").length>0)  {
			$("#other_config_div").remove();
		}
      var menuInfoFeaturesRows=datatable_menuInfoListTable.selectedRows;
	  var id = getMenuInfoId();
	  var menuOperateId = getMenuInfoFeaturesId();
	  var entityCode=getEntityCode();
	  var url="";
	  if(strType=='add'&&id==''){
  		var msg={};
    	msg.exceptionMsg="${getHtmlText('foundation.menuinfo.checkselected')}"
    	CUI.showErrorInfos(msg);
    	return false;
	  }
	  else if(strType=='add'&&id==-1){
	  	var msg={};
    	msg.exceptionMsg="${getHtmlText('foundation.menuinfo.isRootNode')}"
    	CUI.showErrorInfos(msg);
    	return false;
	  }
	  if((strType=='modify'||strType=='delete')&&menuOperateId==''){
  		var msg={};
    	msg.exceptionMsg="${getHtmlText('foundation.menuOperate.checkselected')}"
    	CUI.showErrorInfos(msg);
    	return false;
	  }
	  if(strType=='add'){
	    
	  	url="/foundation/menuInfo/addFeatures/default?menuInfo.id="+id;
	   
	  }else if(strType=='modify'){
	    if(menuInfoFeaturesRows.length>1){
	       var msg={};
    	   msg.exceptionMsg="${getHtmlText('foundation.menuinfo.onlyModify')}"
    	   CUI.showErrorInfos(msg);
    	   return false;
	    }
	    url="/foundation/menuInfo/modifyFeatures/default?menuOperate.id=" + menuOperateId+"&menuInfo.id="+id;
	   }else if(strType=='delete'){
	      var menuInfoParams="";
	      if(menuInfoFeaturesRows.length==0){
	         CUI.Dialog.alert("${getHtmlText('foundation.menuinfo.checkselected')}");
			 return false;
	      }
	      for(var i=0;i<menuInfoFeaturesRows.length;i++){
	          menuInfoParams=(menuInfoParams=="")?menuInfoFeaturesRows[i].id+","+menuInfoFeaturesRows[i].version:menuInfoParams+";"+menuInfoFeaturesRows[i].id+","+menuInfoFeaturesRows[i].version;
	      }
	      //alert(menuInfoParams);
	      
	      CUI.Dialog.confirm("${getHtmlText('foundation.menuinfo.checkdelete')}", function(){
				CUI.post("/foundation/menuInfo/deleteFeatures/default?menuInfoParams="+menuInfoParams,foundation.menuInfo.showMenuOperateInfo, "json");
		  });
	      return false;
	   }
	   

		var type = (getMenuInfoStFlag() == 'S2' ? 5 : '');
		foundation.menuInfo.menuOperateDialog = new CUI.Dialog({
			title: "${getHtmlText('foundation.menuoperation.manage')}",
			url:url,
			modal:true,
			type:type,
			width: 600,
			height: 450,
			dragable:true,
			buttons:[
					{	name:"${getHtmlText('common.button.save')}",
						handler:function(){foundation.menuInfo.saveMenuOperate()}
					},
					{	name:"${getHtmlText('common.button.cancel')}",
						handler:function(){this.close()}
					}]
		})
		otherRestrictEditErrorBarWidget.close();
		foundation.menuInfo.menuOperateDialog.show();
	 
    }

	foundation.menuInfo.saveMenuOperate = function(){
		var flag1 = true;
		var flag2 = true;
		var result = "";
		var error = "";
		clearErrorLabels();
		if($("#editMenuOperateCode").attr('value')==""){
			flag1 = false;
			showErrorField($("#editMenuOperateCode"));
			error = "${getHtmlText('common.errorbar.codeError')}<br>";
			result += error;
		}
		if($('[name="menuOperate.name"]').attr("value")==""){
			flag1 = false;
			showErrorField($('[name="menuOperate.name"]'));
			error = "${getHtmlText('common.errorbar.nameError')}<br>";
			result += error;
		}
		/*if($("#menuOperateUrl").attr('value')==""){
			flag1 = false;
			showErrorField($("#menuOperateUrl"));
			error = "${getHtmlText('common.errorbar.urlError')}<br>";
			result += error;
		}*/
	
		<#if bapS2together?? && bapS2together>
		if ($("#menuoperateIscontainer").length > 0) {
			if($("#menuoperateIscontainer").is(':checked')){
				if($("#entryOperateGroup").attr('value') == ""){
					flag1 = false;
					showErrorField($("#entryOperateGroup"));
					error = "${getHtmlText('common.errorbar.error1')}<br>";
					result += error;
				}
				if($("#mainOperateGroup").attr('value') == ""){
					flag1 = false;
					showErrorField($("#mainOperateGroup"));
					error = "${getHtmlText('common.errorbar.error2')}<br>";
					result += error;
				}
			}
			var v = datatable_menuOperateBondModel.validateTable(true);
			if(v == ""){
				var json = datatable_menuOperateBondModel.parseEditedData();
				$('input[name="menuOperateBondModelListJson"]').remove();
				$('<input type="hidden" name="menuOperateBondModelListJson">').val(json).appendTo($('#foundation_menuInfoFeatures_edit_form'));
			}else{
				flag2 = false;
				result += v;
			}
		}
		</#if>
		if(result != ""){
			foundation_menuInfoFeatures_edit_formDialogErrorBarWidget.showMessage(result);
		}
		if(flag1 && flag2){
			CUI('#foundation_menuInfoFeatures_edit_form').submit();
		}
	}

	var foundation_menuInfoFeatures_edit_form_data = CUI('input,select,textarea', CUI('#foundation_menuInfoFeatures_edit_form')).serializeObject();
	/**
	 * 显示编辑对话框
	 * @method showEditDialog
	 * @param {String} url
	 * @private
	 */
	foundation.menuInfo.showFeaturesEditDialog = function(url,addFlag) { 
		// 初始化当前form
		CUI.initForm('foundation_menuInfoFeatures_edit_div','', foundation_menuInfoFeatures_edit_form_data,null,
	    addFlag?[{id:'editMenuOperateCode',readonly:false}]:['editMenuOperateCode']);	
	    
		CUI.post(url, function(res){

			CUI.fillValues('foundation_menuInfoFeatures_edit_div','menuOperate',res);
			var menuInfoId=res.menuInfo.id;
			CUI("#menuOperatemenuInfoId").val(menuInfoId);
			foundation.menuInfo.dialog = new CUI.Dialog({
				title: "${getHtmlText('foundation.menuOperate.manage')}",
				elementId: "foundation_menuInfoFeatures_edit_div",
				formId: "foundation_menuInfoFeatures_edit_form",
				modal:true,
				type:4,
				onload: 'foundation.menuInfo.initDialog',
				buttons:[{	name:"${getHtmlText('common.button.save')}",
							handler:function(){CUI('#foundation_menuInfoFeatures_edit_form').submit();}
						},
						{	name:"${getHtmlText('common.button.cancel')}",
							handler:function(){this.close()}
						}]
			});
			foundation.menuInfo.dialog.show();
		}, "json");
	};
 
	 /**
	 * 获取待的记录ID，从树或者列表上获取
	 * @method getOperateRecordId
	 * @return {string} 待操作的记录ID
	 * @private
	 */
	 var getMenuInfoId = function(){
		var Id;
		if(menuInfoTree.getSelectedNodes()[0] != null ){  
			Id = menuInfoTree.getSelectedNodes()[0].id;
		}else if( typeof(datatable_menuInfoListTable) != 'undefined' && datatable_menuInfoListTable!=null &&datatable_menuInfoListTable.selectedRows.length>0 && datatable_menuInfoListTable.selectedRows[0].menuInfo){		 
		 	Id = datatable_menuInfoListTable.selectedRows[0].menuInfo.id;
		}else{
		    return "";
		}
		return Id;
	};
	 /**
	 * 获取菜单的stFlag
	 * @method getOperateRecordId
	 * @return {string} 待操作的记录ID
	 * @private
	 */
	 var getMenuInfoStFlag = function(){
		var stFlag;
		if(menuInfoTree.getSelectedNodes()[0] != null && menuInfoTree.getSelectedNodes()[0].id != -1 && menuInfoTree.getSelectedNodes()[0].stFlag){  
			stFlag = menuInfoTree.getSelectedNodes()[0].stFlag.code;
		}else if( typeof(datatable_menuInfoListTable) != 'undefined' && datatable_menuInfoListTable!=null &&datatable_menuInfoListTable.selectedRows.length>0 && datatable_menuInfoListTable.selectedRows[0].menuInfo && datatable_menuInfoListTable.selectedRows[0].menuInfo.stFlag){		 
		 	stFlag = datatable_menuInfoListTable.selectedRows[0].menuInfo.stFlag.code;
		}else{
		    return "BAP";
		}
		return stFlag;
	};
	/**
	 * 获取待的记录name，从树或者列表上获取
	 * @method getOperateRecordId
	 * @return {string} 待操作的记录ID
	 * @private
	 */
	 var getMenuInfoName = function(){
		var name;
		if(menuInfoTree.getSelectedNodes()[0] != null ){  
			name = menuInfoTree.getSelectedNodes()[0].name;

		}else if( typeof(datatable_menuInfoListTable) != 'undefined' && datatable_menuInfoListTable!=null &&datatable_menuInfoListTable.selectedRows>0){		 

		 	name = datatable_menuInfoListTable.selectedRows[0].name;
		}else{
		    return "";
		}
		return name;
	};
	/**
	 * 获取待操作的记录ID，从树或者列表上获取
	 * @method getOperateRecordId
	 * @return {string} 待操作的记录ID
	 * @private
	 */
	 var getMenuInfoFeaturesId = function(){
		var menuInfoFeaturesId="";
		try{
			if(datatable_menuInfoListTable!=null&& typeof(datatable_menuInfoListTable) != undefined && datatable_menuInfoListTable.selectedRows.length>0){  
			 
			    menuInfoFeaturesId = datatable_menuInfoListTable.selectedRows[0].id;
			}else{
		    	return "";
			}
		}catch(e){}
	    
		return menuInfoFeaturesId;
	};
	 var getMenuInfoFeaturesVersion= function(){
		var menuInfoFeaturesVersion="";
		try{
			if(datatable_menuInfoFeaturesListTable!=null&& typeof(datatable_menuInfoFeaturesListTable) != undefined && datatable_menuInfoFeaturesListTable.selectedRows.length>0){  
			 
			    menuInfoFeaturesVersion = datatable_menuInfoFeaturesListTable.selectedRows[0].version;
			}else{
		    	return 0;
			}
		}catch(e){}
	    
		return menuInfoFeaturesVersion;
	};
	var getEntityCode=function(){
		var entityCode="";
		if(datatable_menuInfoListTable!=null&&typeof(datatable_menuInfoListTable) != undefined &&datatable_menuInfoListTable.selectedRows.length>0){
			entityCode=datatable_menuInfoListTable.selectedRows[0].entityCode;
		}
		return entityCode;
	}
	foundation.menuInfo.editMenuInfo=function(obj){
		/*if(datatable_menuInfoListTable.selectedRows.length>0){
			var leaf = datatable_menuInfoListTable.selectedRows[0].leaf;
			foundation.menuInfo.showButton(leaf);
		}(*/
	}
	
	var getOperateRecordVersion = function(){
		var version;
		if(menuInfoTree.getSelectedNodes()[0] != null){
			version = menuInfoTree.getSelectedNodes()[0].version;
		}else{
			version = datatable_menuInfoListTable.selectedRows[0].version;
		}
		return version;
	};



	$("#QueryForm input.cui-edit-field").bind("keydown",function(evt){
		if(evt.keyCode == 13){
			//按下回车
			setTimeout(function(){foundation.menuInfo.queryList();}, 200);
			
		}
	});
	 	 
})();	

$(function(){
	//首次进入页面时，默认加载info
	foundation.menuInfo.showMenuInfo();    //位置移动到domready里面，原先没移动的时候第一次执行menuInfo会报错
})
</script>


<div id="other_config_dialog" style="display:none;">
	<@errorbar id="otherRestrictEditErrorBar"></@errorbar>
	<table cellpadding="0"  cellspacing="0" border="0" align="left" width="98%" class="infoTable" style="margin-top: 8px;">
			<input type="hidden" id="otherConfigCode" />
			<tr>
				<td   style="padding-left:18px;width:27%;text-align: right;color:red">${getHtmlText('foundation.otherRestrict.title')}：</td>
				<td class="la" style="width:30%;text-align:left;padding-right:8px;"  colspan="1">
					<div class="fix-input"><input type="text" id="otherConfigTitle"  class="cui-noborder-inputTemp"/></div>
				</td>
				<td style="width:25%"></td>
			</tr>
			<tr>
				<td   style="padding-left:18px;width:27%;text-align: right">${getHtmlText('foundation.otherRestrict.memo')}：</td>
				<td class="la" style="width:30%;text-align:left;padding-right:8px;"  colspan="1">
					<div class="fix-input"><input type="text" id="otherConfigMemo"   class="cui-noborder-inputTemp"/></div>
				</td>
				<td style="width:25%"></td>
			</tr>
			<tr>
				<td class="la"  style="padding-left:48px;text-align:right">
					<input type="checkbox" id="isTransOtherConfig" />
				</td>
				<td class="co" style="width:25%;padding-right:0px">
					${getHtmlText('foundation.otherRestrict.handWriteCondition')}
					<div id="66387035_button_3" canClick="true" class="edit-btn btn-act" onclick="showTableDialogForOtherConfig()"  style="float:right;margin-right:0px"><a class="cui-btn-l">&nbsp;</a><a class="cui-btn-c">${getHtmlText('foundation.otherRestrict.conditionDefine')}</a><a class="cui-btn-r">&nbsp;</a></div>
				</td>
				<td style="width:25%"></td>
			</tr>
			<tr>
				<td class="co" colspan="3" style="padding-left:30px">
					<textarea readonly="true" id="otherConfigArea" class="cui-edit-textarea" style="width:90%;height:110px;border: 1px solid #C6d4e1;"></textarea>
				</td>
			</tr>
	</table>
</div>

<#if (Parameters.openType)?default('page') != 'dialog'>
</body>
</html>
</#if>
