<@head/>
<#assign assTreeMap = assTreeMap>
<#if assTreeMap?has_content && (assTreeMap.modelSelf)?has_content>
	<#list assTreeMap.modelSelf as modelSelf>
		<#assign modelDataType = modelSelf.dataType>
	</#list>
</#if>	
<title>${getText('ec.view.assTreeModel')}</title>
<@s.hidden name="closePage" id="closePage"></@s.hidden>
<@s.hidden name="modelCode" id="modelCode"></@s.hidden>
<@s.hidden name="callBackFuncName" id="callBackFuncName"></@s.hidden>
<style type="text/css">
	body {font-family: arial,helvetica,sans-serif;font-size: 12px;}
	body, div, dl, dt, dd, ul, ol, li, h1, h2, h3, h4, h5, h6, pre, code, form, legend, input, button, textarea, p, blockquote, th, td {margin: 0;padding: 0;}
	.grid-s4m0 .col-sub { margin-left:2px;margin-top:8px;width: 140px; float:left;background-color:white;border:1px solid #3C7FB1}			
	#form_select_elements{width:200px;background-color: #F2F2F2;margin:10px;}
	#form_select_elements h2 {margin:1;	padding:5px 0;text-align:center;font-size:13px;font-weight:normal;border:1px solid #0369A3;cursor:pointer;background-color: #9cc0d6;}
	#form_select_elements .accordion_pane {	border: 1px solid #3C7FB1;display: block; overflow: auto;padding:0px;font-size:12px;}
	#form_select_elements .accordion_pane li {font-size:12px;color:#000000;cursor:pointer;line-height:18px;z-index:100;margin-left:4px;list-style-type:none;}
	.tip-class {font-size:12px;font-weight:bold;padding-top:2px;padding-bottom:2px}	
</style>
<@frameset id="assmodelFrameset" border=0>
    <@frame id="assmodel_center" region="center">
    	<div id="form_select_elements">
		<h2 class="current">${getText('ec.view.assTreeModel')}</h2>
			<div class="accordion_pane" >
				<ul class="main_properties_container">
					<#if modelDataType == 2>
					<div class="tip-class">${getHtmlText('ec.view.assTreeSelf')}</div>
					<#if assTreeMap?has_content && (assTreeMap.modelSelf)?has_content>
						<#list assTreeMap.modelSelf as modelSelf>
						<li source="tree" showName="${getText('${(modelSelf.name)}')}" layRec="${(modelSelf.tableName)!},LAY_REC,${(modelSelf.tableName)!},LAY_REC-layRec" relativeRec="${(modelSelf.tableName)!},LAY_REC,${(modelSelf.tableName)!},LAY_REC" parentRelativeCode="${(modelSelf.modelName)?uncap_first}" relationCode="${(modelSelf.modelName)?uncap_first}.layRec">
							<span onclick="ec.view.sendBackAssTreeResult(this);">${getHtmlText('${(modelSelf.name)}')}</span>
						</li>
						</#list>
					</#if>
					</#if>
					<div class="tip-class">${getHtmlText('ec.view.assTreeName')}</div>
					<#if assTreeMap?has_content && (assTreeMap.treeModels)?has_content>
						<#list assTreeMap.treeModels as treeModel>
							<li source="tree" showName="${getText('${(treeModel.targetProperty.model.name)!}')}[${getText('${(treeModel.originalProperty.displayName)!}')}]" layRec="${(treeModel.targetProperty.model.tableName)!},${(treeModel.targetProperty.columnName)!},${(treeModel.originalProperty.model.tableName)!},${(treeModel.originalProperty.columnName)!}-layRec" relativeRec="${(treeModel.targetProperty.model.tableName)!},${(treeModel.targetProperty.columnName)!},${(treeModel.originalProperty.model.tableName)!},${(treeModel.originalProperty.columnName)!}" parentRelativeCode="${(treeModel.originalProperty.name)!}" relationCode="${(treeModel.originalProperty.name)!}.${(treeModel.targetProperty.name)!}">
								<img align="absmiddle" src="/bap/static/treeview/assets/ectree_colse.gif" flag="true" hasclick="false" onclick="ec.view.showAssModels(this,'${(treeModel.targetProperty.model.code)!}')">
								<span onclick="ec.view.sendBackAssTreeResult(this);">${getHtmlText('${(treeModel.targetProperty.model.name)!}')}[${getHtmlText('${(treeModel.originalProperty.displayName)!}')}]</span>
							</li>
						</#list>
					<#else>
						<li style="color:#ADADAD">${getHtmlText('ec.view.none')}</li>
					</#if>
					<div class="tip-class">${getHtmlText('ec.view.assModel')}</div>	
					<#if assTreeMap?has_content && (assTreeMap.notTreeModels)?has_content>
						<#list assTreeMap.notTreeModels as notTreeModel>
							<li style="color:#ADADAD" source="normal" showName="${getText('${(notTreeModel.targetProperty.model.name)!}')}[${getText('${(notTreeModel.originalProperty.displayName)!}')}]" layRec="${(notTreeModel.targetProperty.model.tableName)!},${(notTreeModel.targetProperty.columnName)!},${(notTreeModel.originalProperty.model.tableName)!},${(notTreeModel.originalProperty.columnName)!}-layRec" relativeRec="${(notTreeModel.targetProperty.model.tableName)!},${(notTreeModel.targetProperty.columnName)!},${(notTreeModel.originalProperty.model.tableName)!},${(notTreeModel.originalProperty.columnName)!}" parentRelativeCode="${(notTreeModel.originalProperty.name)!}" relationCode="${(notTreeModel.originalProperty.name)!}.${(notTreeModel.targetProperty.name)!}">
								<img align="absmiddle" src="/bap/static/treeview/assets/ectree_colse.gif" flag="true" hasclick="false" onclick="ec.view.showAssModels(this,'${(notTreeModel.targetProperty.model.code)!}')">
								<span>${getHtmlText('${(notTreeModel.targetProperty.model.name)!}')}[${getHtmlText('${(notTreeModel.originalProperty.displayName)!}')}]</span>
							</li>
						</#list>
					<#else>	
						<li style="color:#ADADAD">${getHtmlText('ec.view.none')}</li>
					</#if>
				</ul>
			</div>	
		</div>		
    </@frame>
</@frameset>
<script type="text/javascript" charset="utf-8" language="javascript">
(function(){
    //注册命名空间
    CUI.ns("ec.view");
	//点击菜单树，返回菜单信息
	$(function(){
		initSize();
        $(window).resize(function(){
            initSize();
        });
	});
	ec.view.sendBackInfo=function(oNode){
		var oMenuInfo = new Object();
		
		if(!confirm("${getText('ec.view.assTreeChoose')}")) return false;
		
		try{
			if(CUI("#closePage").val() != "false") top.opener.focus();
			if(CUI("#callBackFuncName").val() != ""){
				eval("top.opener." + CUI("#callBackFuncName").val() + "(treeModel)");
			}	
			if(CUI("#closePage").val() != "false") CUI.closeWindow();
		}catch(e){
			CUI.Dialog.alert("${getHtmlText('ec.view.assTreeChooseError')}");
		}
	}
	
	ec.view.showAssModels = function(obj,modelCode) {
		if(modelCode != null && modelCode != "" && modelCode != undefined) {
		
		
			if(CUI(obj).attr('hasclick')!='true'&&CUI(obj).attr('hasclick')!=true){
				// 第一次点击展开进入的时候，通过ajax查出属性的关联实体的属性
				try{
		    		var oResponse;
		    		CUI.ajax({
						url: "/msService/ec/view/findTreeModelsMapByModel",
		    			type: 'post',
		    			async: false,
		    			data: {"modelCode" : modelCode},
		    			success: function(jsonResponse) {
		    				oResponse = jsonResponse;
		    				if(window.closeLoadPanel){closeLoadPanel();}
		    			}
	    			});
	    			
	    			var objUl = $("<ul id=\"ass\" style=\"margin-left:25px;width:100%\"></ul>");
					$(obj.parentNode).append(objUl);
					var parentRelativeRec = $(obj.parentNode).attr('relativeRec');
					var parentShowName = $(obj.parentNode).attr('showName');
					var parentRelativeCode = $(obj.parentNode).attr('parentRelativeCode');
					var treeModels = oResponse.treeModels;
					var notTreeModels = oResponse.notTreeModels;
					var treeTipDiv = $("<div class=\"tip-class\" style=\"color:#5B8EC2\">|--${getText('ec.view.assTreeName')}</div>");
					objUl.append(treeTipDiv);
					if(treeModels != undefined && treeModels != null && treeModels.length > 0) {
						$.each(treeModels, function(i, item){
							var objLi = $("<li source=\"tree\" showName=\""+ parentShowName + "-" + item.targetProperty.model.nameInternational + "[" + item.originalProperty.displayNameInternational + "]\" layRec=\""+parentRelativeRec+"-"+item.targetProperty.model.tableName+","+item.targetProperty.columnName+","+item.originalProperty.model.tableName+","+item.originalProperty.columnName+"-layRec\" relativeRec=\""+parentRelativeRec+"-"+item.targetProperty.model.tableName+","+item.targetProperty.columnName+","+item.originalProperty.model.tableName+","+item.originalProperty.columnName+"\" parentRelativeCode=\"" + parentRelativeCode + "." + item.originalProperty.name + "\" relationCode=\""+parentRelativeCode + "." + item.originalProperty.name+"."+item.targetProperty.name+"\"><img align=\"absmiddle\" src=\"/bap/static/treeview/assets/ectree_colse.gif\" flag=\"true\" hasclick=\"false\"><span>"+ item.targetProperty.model.nameInternational + "[" + item.originalProperty.displayNameInternational + "]</span></li>");
							var objImg = $("img", objLi);
							objImg.bind('click', function(){
								ec.view.showAssModels(this,item.targetProperty.model.code);
							});
							var objSpan = $("span",objLi);
							objSpan.bind('click',function(){
								ec.view.sendBackAssTreeResult(this);
							});
							objUl.append(objLi);
						});
					} else {
						var objLi = $("<li style=\"color:#ADADAD\">${getHtmlText('ec.view.none')}</li>");
						objUl.append(objLi);
					}
					var notTreeTipDiv = $("<div class=\"tip-class\" style=\"color:#5B8EC2\">|--${getHtmlText('ec.view.assModel')}</div>");
					objUl.append(notTreeTipDiv);
					if(notTreeModels != undefined && notTreeModels != null && notTreeModels.length > 0) {
						$.each(notTreeModels, function(j, normalItem){
							var normalObjLi = $("<li style=\"color:#ADADAD\" source=\"normal\" showName=\"" + parentShowName + "-" + normalItem.targetProperty.model.nameInternational + "[" + normalItem.originalProperty.displayNameInternational + "]\" layRec=\""+parentRelativeRec+"-"+normalItem.targetProperty.model.tableName+","+normalItem.targetProperty.columnName+","+normalItem.originalProperty.model.tableName+","+normalItem.originalProperty.columnName+"-layRec\" relativeRec=\""+parentRelativeRec+"-"+normalItem.targetProperty.model.tableName+","+normalItem.targetProperty.columnName+","+normalItem.originalProperty.model.tableName+","+normalItem.originalProperty.columnName+"\" parentRelativeCode=\"" + parentRelativeCode + "." + normalItem.originalProperty.name + "\" relationCode=\""+ parentRelativeCode + "." + normalItem.originalProperty.name+"."+normalItem.targetProperty.name+"\"><img align=\"absmiddle\" src=\"/bap/static/treeview/assets/ectree_colse.gif\" flag=\"true\" hasclick=\"false\">"+ normalItem.targetProperty.model.nameInternational + "[" + normalItem.originalProperty.displayNameInternational + "]</li>");
							var normalObjImg = $("img", normalObjLi);
							normalObjImg.bind('click', function(){
								ec.view.showAssModels(this,normalItem.targetProperty.model.code);
							});
							objUl.append(normalObjLi);
						});
					} else {
						var normalObjLi = $("<li style=\"color:#ADADAD\">${getHtmlText('ec.view.none')}</li>");
						objUl.append(normalObjLi);
					}
		    		CUI(obj).attr('hasclick', true);
		    		obj.src="/bap/static/treeview/assets/ectree_expand.gif";
				}catch (e){
					return false;
				}
			}else{
				if(CUI(obj).attr('flag')=='true' || CUI(obj).attr('flag')==true){
					obj.src="/bap/static/treeview/assets/ectree_expand.gif";
					$(obj).next().next().show();
		    		CUI(obj).attr('flag', false);
				}else{
					obj.src="/bap/static/treeview/assets/ectree_colse.gif";
					$(obj).next().next().hide();
		    		CUI(obj).attr('flag', true);
				}
			}
		}	
	}
	
	ec.view.sendBackAssTreeResult = function(obj) {
		var assTreeResult = new Object();
		if(!confirm("${getText('ec.view.assTreeChoose')}")) return false;
		assTreeResult.layRec = $(obj.parentNode).attr('layRec');
		assTreeResult.relationCode = $(obj.parentNode).attr('parentrelativecode');
		assTreeResult.assTreePath = $(obj.parentNode).attr('showName');
		try{
			if(CUI("#closePage").val() != "false") top.opener.focus();
			if(CUI("#callBackFuncName").val() != ""){
				eval("top.opener." + CUI("#callBackFuncName").val() + "(assTreeResult)");
			}
			if(CUI("#closePage").val() != "false") CUI.closeWindow();
		}catch(e){
			CUI.Dialog.alert("${getHtmlText('foundation.inter.alert')}");
		}
	}
	
	function initSize(){
        $(".accordion_pane").height($(window).height() - 50);
    }
})();
</script>