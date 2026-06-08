<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
	<head>
        <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
        <title>${getText('ec.view.selectScript')}</title>
		<@head />
		<@adpSkin />
		<style>
		#ec_script_select_datatable_wrap {
			width: 100%;
			height: calc(100% - 50px);
		}
		</style>
	</head>
	<body>
		
		<@errorbar id="scriptListFramePageErrorBar" />
		<input type="hidden" name="callBackFuncName" value="${callBackFuncName}" id="callBackFuncName"/>
		<div id="ec_script_select_datatable_wrap">
		<div>
		<@datatable id="ec_script_select_datatable" hidekey="['id']" dblclick="scriptSelect" editable=false transMethod="post" dataUrl="/msService/ec/scripts/list?entityCode="+entityCode>
			<@datacolumn key="scriptCode" label="${getHtmlText('js.ec.script.manager.code')}" width=150/>
			<@datacolumn key="name" label="${getHtmlText('js.ec.script.manager.name')}" width=150/>
			<@datacolumn key="description" label="${getHtmlText('js.ec.script.manager.descript')}" width=250/>
		</@datatable>
		</div>
		</div>
		<div align="right" style="margin-right:20px;" >
			<button type="button" id="queryButton" class="btn-primary" onclick="scriptSelect()">${getText("common.button.choose")}</button>&#160;&#160;
			<button type="button" id="closeButton" class="" onclick="CUI.closeWindow()">${getText("common.button.cancel")}</button>
		</div>
		<script type="text/javascript">
			function scriptSelect(event,oRow){
			
				var arrObj = new Array();
	
				var oRows = new Array();
				if(event == undefined){
					oRows = datatable_ec_script_select_datatable.selectedRows;
				}else{
					oRows.push(oRow);
				}	
				if(oRows.length == 0){
					CUI.Dialog.alert("${getHtmlText('foundation.common.selectNullData')}");
					return false;
				}
				
				for(var i=0; i<oRows.length; i++){
					oRows[i].rowIndex = CUI("#rowIndex").val();
					arrObj.push(oRows[i]);
				}
				try{
					if(CUI("#callBackFuncName").val() != ""){

						opener.setScript.call($(opener.document.getElementById('${callBackFuncName}')), arrObj);
						//eval("opener." + CUI("#callBackFuncName").val() + "(arrObj)");
						//opener.setScript(arrObj);
					}else{
						opener.setScript(arrObj);
					}
					/*if(CUI("#closePage").val() != "false") {
						top.opener.focus();
						CUI.closeWindow();
					}*/
					top.opener.focus();
					CUI.closeWindow();
					scriptListFramePageErrorBarWidget.showMessage("${getHtmlText('foundation.department.add.success')}","s");
				}catch(e){
					//console.log(e)
					scriptListFramePageErrorBarWidget.showMessage("${getHtmlText('foundation.department.add.failure')}","f");
					//alert("注意：父窗口回调出错！");
				}
			}
		</script> 
	</body>
</html>