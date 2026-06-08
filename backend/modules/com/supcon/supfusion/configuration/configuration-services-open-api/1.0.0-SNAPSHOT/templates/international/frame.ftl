<#if (Parameters.openType)?default('page') != 'dialog'>
<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>${getText('foundation.international.manage')}</title>
<@maincss/>
<@mainjs/>
</head>
<body>
<@loadpanel></@loadpanel>
<@errorbar id="workbenchErrorBar" offsetY=0 />
</#if>
<#-- 页面公用变量放在mainFrame,确定哪些公用变量 -->
<@frameset id="workbenchOperateBar"><#-- id: module_entity_function -->
	<#-- <@frame id="top_in" region="north" height=40>
     	<div class="cui-main-title">
	     	<@loadpanel paddingLeft="70" text="${getText('foundation.international.manage')}" />
			<div class="cui-main-title-r">
				<@operatebar operates="code:base_international_add||name:${getHtmlText('foundation.view.add')}||iconcls:add||onclick:foundation.international.add();
					code:base_international_modify||name:${getHtmlText('foundation.view.edit')}||iconcls:edit||onclick:foundation.international.modify();
					code:base_international_delete||name:${getHtmlText('foundation.view.del')}||iconcls:del||onclick:foundation.international.del();
					code:base_international_export||name:${getHtmlText('foundation.inter.export')}||iconcls:export||onclick:foundation.international.excelExport();
					code:base_international_language||name:${getHtmlText('foundation.inter.language')}||iconcls:del||onclick:foundation.international.language()" operateType="noPower">
				</@operatebar>
			</div>
		</div>
     </@frame>-->
     <@frame id="center_in" region="center">
     	<div>
			<form id="international_queryForm" onsubmit="return false;">
				<@quickquery formId="international_queryForm"  fieldcodes="internationalKey:foundation.international.key||internationalLangKey:foundation.userset.language||internationalenValue:foundation.userset.internationalvalue" unique="LAST_QUERY_foundation_international__frame">
			       	<@queryfield isCustomize=true formId="international_queryForm" code="internationalKey">
			       		<input type="text" id="internationalKey"  class="cui-edit-field" name="international.key"/>
			       	</@queryfield>
			       	<@queryfield isCustomize=true formId="international_queryForm" code="internationalLangKey">
			       		<select id="internationalLangKey" class="cui-edit-field" name="international.languageKey">
							<option value=""></option>
							<#list languages as l>
							<#if l.isUsed?? && l.isUsed?string=="true">
							<option value="${l.key}">${getText(l.internationalKey)}</option>
							</#if>
							</#list>
						</select>
			       	</@queryfield>
			       	<@queryfield isCustomize=true formId="international_queryForm" code="internationalenValue">
			       		<input type="text" id="internationalenValue" name="international.value" class="cui-edit-field"></input>
			       	</@queryfield>
			       	<@querybutton formId="international_queryForm" type="search" onclick="foundation.international.queryinternationals()" />
			 		<@querybutton formId="international_queryForm" type="clear"  />
			     </@quickquery>
			</form>
		</div>
		<@datatable dtPage="page" formId="international_queryForm" hidekey="['id']" dblclick="foundation.international.modify" style="margin:0px 10px;"  editable=false transMethod="post" id="internationalListTable" dataUrl="/msService/ec/foundation/international/queryList">
			<@operatebar operates="code:base_international_add||name:${getHtmlText('foundation.view.add')}||iconcls:add||onclick:foundation.international.add();code:base_international_modify||name:${getHtmlText('foundation.view.edit')}||iconcls:edit||onclick:foundation.international.modify();code:base_international_delete||name:${getHtmlText('foundation.view.del')}||iconcls:del||onclick:foundation.international.del();code:base_international_export||name:${getHtmlText('foundation.inter.export')}||iconcls:exportftl||onclick:foundation.international.excelExport();code:base_international_language||name:${getHtmlText('foundation.inter.language')}||iconcls:setting||onclick:foundation.international.language()" operateType="noPower"  resultType="json">
			</@operatebar>
			<@datacolumn key="key" label="${getHtmlText('foundation.international.key')}" width="300"/>
			<#list languages as l>
			<#if l.isUsed?? && l.isUsed?string=="true">
			<@datacolumn key="${l.key}" label="${getText(l.displayName)}" width="200"/>
			</#if>
			</#list>
		</@datatable>
     </@frame>
</@frameset>

<script type="text/javascript" charset="UTF-8" language="javascript">
(function(){
	//注册命名空间
	CUI.ns("foundation.international");
	$("#internationalLangKey").mSelect();
	
	/**
	 * 国际化数据添加
	 * @method foundation.international.add
	 * @public
	 */
	foundation.international.add = function(){
		showEditDialog("/foundation/international/edit.action");
	};
	
	/**
	 * 国际化数据修改
	 * @method foundation.international.modify
	 * @public
	 */
	foundation.international.modify = function(){
		if(checkSelectedAny()) {
			showEditDialog("/foundation/international/edit.action?key=" + getOperateRecordKey());
		}	
	};
	
	/**
	 * 国际化数据删除
	 * @method foundation.international.del
	 */
	 foundation.international.del = function(){
		if(checkSelectedAny()) {
			CUI.Dialog.confirm("${getText('foundation.international.checkdelete')}", function(){
			CUI.post("/foundation/international/delete/default.action?key="+getOperateRecordKey(), foundation.international.callBackInfo, "json");
		});
		}	
	}
	
	

	/**
	 * 双击修改国际化数据
	 * @method foundation.international.modifyInternationalInfo
	 */
	foundation.international.modifyInternationalInfo = function(event,oRow){
		var url = "/foundation/international/modify/default.action?id=" + oRow.id;
		showEditDialog(url);
	};
	/**
	 * 保存
	 * @method foundation.international.SaveInfo
	 */
	foundation.international.SaveInfo = function(){
		var num = $('#listsize').val();
		var lang = "${lang}";
		var langType = $('input[langType="' + lang + '"]');
		var langTypeVal = langType.val().trim();
		if (langTypeVal == "" || langTypeVal == null || langTypeVal == undefined) {
			foundation_international_edit_formDialogErrorBarWidget.show("${getText('foundation.common.notnullcurrentlyselectedlang')}","f");
			return false;
		}
		for(var i=0;i<num;i++){
			var value = $('#index_'+i).val();
			var flag = $('#index_'+i).attr('flag');
			if(flag!=''&&value==''){
				foundation_international_edit_formDialogErrorBarWidget.show("${getText('foundation.common.notsaveinternational')}","f");
				showErrorField(CUI('#index_'+i));
				return false;
			}
		}
		CUI('#foundation_international_edit_form').submit();	
	};

	/**
	 * 保存完毕回调
	 * @method foundation.international.callBackInfo
	 */
	foundation.international.callBackInfo = function(res){
		if(res.dealSuccessFlag){
			if(res.operateType == 'save'){
				foundation_international_edit_formDialogErrorBarWidget.show("${getText('foundation.common.saveandclosesuccessful')}","s");
			} else if(res.operateType == 'delete') {
				workbenchErrorBarWidget.showMessage("${getText('foundation.common.deleteandrefreshsuccessful')}","s");
			} 
			setTimeout(function(){
				try{foundation.international.editDlg.close();}catch(e){}
				foundation.international.queryinternationals();
			},1500);
		} else {
			CUI.showErrorInfos(res, ((typeof (foundation_international_edit_formDialogErrorBarWidget)=='undefined')?null:foundation_international_edit_formDialogErrorBarWidget));
		}
	};
	
	/**
	 * 查询
	 * @method foundation.international.queryinternationals
	 */
	foundation.international.queryinternationals = function(){
		var dataPost="";
		var url = "/msService/ec/foundation/international/queryList?a=1";

		var internationalKey =encodeURIComponent(CUI.trim(CUI("#internationalKey").val())) ;
		var internationalLangKey =encodeURIComponent(CUI.trim(CUI('#internationalLangKey').val())) ;
		var internationalenValue =encodeURIComponent(CUI.trim(CUI('#internationalenValue').val())) ;
		if(internationalKey){
			 dataPost += "&international.key=" + internationalKey;
		}
		if(internationalLangKey){
			dataPost += "&international.languageKey=" + internationalLangKey;
		}
		if(internationalenValue){
			dataPost += "&international.value=" + internationalenValue;
		}  
		dataPost+="&"+international_queryForm_getCookieParam();
		var pageSize=CUI('input[name="internationalListTable_PageLink_PageCount"]').val();
		dataPost += "&pageSize="+encodeURIComponent(pageSize);
		var pageNo = CUI("#PageLink_PageSelect").val();
		dataPost += "&pageNo=1";
		dataPost += "&page.pageNo=1";
		
	    internationalListTableWidget.setRequestDataUrl(url,dataPost);

		return false;
		
	};
	
	/**
	 * 获取待操作的记录ID
	 * @method getOperateRecordId
	 * @private
	 */
	var getOperateRecordKey = function(){
		var id;
		id = internationalListTableWidget.selectedRows[0].key;
		return id;
	}; 
	
	/**
	 * 确认是否选中
	 * @method checkSelectedAny
	 * @private
	 */
	 
	 var checkSelectedAny = function(){
	 	if(internationalListTableWidget.selectedRows.length == 0){
			workbenchErrorBarWidget.showMessage("${getText('foundation.international.checkselected')}","f");
			return false;
		}
		return true;
	};
	
	/**
	 * 显示增加对话框
	 * @method showAddDialog
	 * @param {String} url
	 * @private
	 */
	var showEditDialog = function(url) {
		foundation.international.editDlg =	new CUI.Dialog({
				title: "${getText('foundation.international.manage')}",
				url :url,
				modal:true,
				type:3,
				buttons:[{	name:"${getText('common.button.save')}",
							handler:function(){
							if(url.indexOf("/foundation/international/edit")>-1){
							var currentKey = $("#foundation_international_edit_form_key").val();
								if(!/^(?!_)(?!.*?_$)[a-zA-Z0-9_.]+$/.test(currentKey)){
									foundation_international_edit_formDialogErrorBarWidget.showMessage("${getText('foundation.international.errormessage')}");
									return false;
								}
							}
							foundation.international.SaveInfo();}
						},
						{	name:"${getText('common.button.cancel')}",
							handler:function(){this.close()}
						}]
			});
			foundation.international.editDlg.show();
	};
	//定义一个全局变量来表示是否对页面进行整体刷新;yes表示刷新,其他表示不刷新
	foundation.international.isNeedRefresh="no";
	foundation.international.refresh=function(){
		if(foundation.international.isNeedRefresh=="yes"){
			var url="";
			var menuCode="base_inernationalManager";
			var parentCode="base_set";
			var pli = $("#main-menu li[code='" + menuCode +"']").data("pli");
			url="/foundation/international/frame.action";
			if (pli) {
				var dt = pli.data("dt");
				__addRootMenu(dt, $("#item-box"), menuCode);
				location.hash='{"url":"'+url+'","code":"' + menuCode + '","parentCode":"'+parentCode+'"}'
			}
		    loadPage({url:url});
		}
		if(foundation.international.languageDialog.isShow !==-1){
			foundation.international.languageDialog.close();
			foundation.international.isNeedRefresh="no";
		}
	}
	foundation.international.language = function(){
		foundation.international.languageDialog=new CUI.Dialog({
			title: "${getText('foundation.international.manage')}",
			url : "/foundation/international/language.action",
			modal:true,
			type:4,
			hideCloseBtn:true,
			buttons:[{name:"${getText('common.button.Confirm')}",
					  handler:foundation.international.refresh}
					]
		});
		foundation.international.isNeedRefresh="no";
		foundation.international.languageDialog.show();
	};
	foundation.international.excelExport = function() {
		new CUI.Dialog({
			title: "${getText('foundation.international.frame.export_setting')}",
			url : "/foundation/international/exportSet.action",
			modal:true,
			type:1,
			buttons:[
					{	name:"${getText('common.button.Confirm')}",
						handler:function(){foundation.international.exportOut()}
					},
					{	name:"${getText('common.button.cancel')}",
						handler:function(){this.close()}
					}]
		}).show();

		
		//var url = "/foundation/international/downloads.action";
		
		//var window_height = window.screen.availHeight-63;
		//var window_width  = window.screen.availWidth-20;
		//ShowStyle = "width=" + window_width + ",height=" + window_height + ",scrollbars=yes,resizable =yes,top=0,left=0,toolbar=no,menubar=no,location=no,status=no";
		//handle = window.open(url,"",ShowStyle);
		//handle = null;
		//window.open(url);
		//internationalListTableWidget.setRequestDataUrl(url,dataPost);
	}
	foundation.international.exportOut=function(){
		CUI("#foundation_international_export_form").submit();
	}
})();	
</script>
<#if (Parameters.openType)?default('page') != 'dialog'>
</body>
</html>
</#if>