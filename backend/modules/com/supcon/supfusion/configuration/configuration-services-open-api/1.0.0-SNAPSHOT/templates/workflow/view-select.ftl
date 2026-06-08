<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
	<head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
        <title>${getText('ec.entity.viewselect')}</title>
		<@head />
	</head>
	<body class="ec-config-page">
	    <input type="hidden" id="callBackFuncName" name="callBackFuncName" />
		<@datatable id="ec_view_select" dblclick="viewSelect" dtPage="viewPage" dataUrl="/msService/ec/entity/view-list?entity.code=${(entity.code)!}" transMethod="post" hidekey="['code','type','title']" paginator=false>
			<@datacolumn key="titleInternational" label="${getHtmlText('ec.entity.viewselectName')}" width=100 />
			<@datacolumn key="displayNameInternational" label="${getHtmlText('ec.view.displayName')}" width=100 />
			<@datacolumn key="url" label="${getHtmlText('ec.entity.viewselectUrl')}" width=300 />
		</@datatable>
		<div align="right" style="margin-right:50px" >
			<button type="button" id="queryButton" class="cui-simplebtn" onMouseOver="changeBtnClass(this);" onMouseOut="changeBtnClass(this);" onclick="viewSelect()">${getText("common.button.choose")}</button>&#160;&#160;
			<button type="button" id="closeButton" class="cui-simplebtn" onMouseOver="changeBtnClass(this);" onMouseOut="changeBtnClass(this);" onclick="CUI.closeWindow()">${getText("common.button.cancel")}</button>
		</div>
		<script type="text/javascript">
			function viewSelect(){
				var arrObj = new Array();
	
				var	oRows = datatable_ec_view_select.selectedRows;
				if(oRows.length == 0){
					CUI.Dialog.alert("${getHtmlText('ec.view.checkselected')}");
					return false;
				}
				for(var i=0; i<oRows.length; i++){
					var obj = new Object();
					obj.viewName= oRows[i].displayNameInternational;
					obj.viewUrl= oRows[i].url;
					obj.viewType= oRows[i].type;
					obj.viewCode= oRows[i].code;
					arrObj.push(obj);
				}
				try{
					if(CUI("#callBackFuncName").val() != ""){
						eval("top.opener."+CUI("#callBackFuncName").val() + "(arrObj)");
					}else{
						top.opener.getViewInfo(arrObj);
					}
				}catch(e){
					CUI.Dialog.alert("${getHtmlText('ec.entity.viewselectAlert')}");
					if(CUI.Dialog) CUI.Dialog.toggleAllButton();
				}
				window.close();
			}
		</script> 
	</body>
</html>