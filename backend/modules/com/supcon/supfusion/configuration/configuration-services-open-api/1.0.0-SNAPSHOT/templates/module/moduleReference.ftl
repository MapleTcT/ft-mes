<@errorbar id="ec_moduleReference_refBar" />
<@datagrid id="moduleReference" style="margin-left:5px;margin-right:5px;" width=790 dblclick="sendBackModuleReferenceInfo" viewType="reference" route="0" formId="" noPermissionKeys="" modelCode="" dataUrl="/msService/ec/module/refQuery?isMsService=${isMsService}&page.pageSize=65536&module.code=${(module.code)!''}" postData="" dataGridName="moduleRefDataGrid" dtPage="page"  hidekeyPrefix="" hidekey="" transMethod="post" paginator=false editable=false buttons="" noPadding=true exportExcel=false tabViewIndex=0 pageInitMethod="">
	<@datacolumn  key="code" showFormat="TEXT" defaultValue="" defaultDisplay="" decimal=""  type="textfield" showType="textfield" textalign="center" hiddenCol=false viewUrl="" mneenable=false label="${getHtmlText('ec.module.artifact')}" width=100/>
	<@datacolumn  key="nameInternational" showFormat="TEXT" defaultValue="" defaultDisplay="" decimal=""  type="textfield" showType="textfield" textalign="center" hiddenCol=false viewUrl="" mneenable=false label="${getHtmlText('ec.module.name')}" width=300/>
</@datagrid>


<script type="text/javascript">
sendBackModuleReferenceInfo = function(event,oRow){
	var arrObj = new Array();
	var oRows = new Array();
	if(event == undefined){
		oRows = role_ListTableWidget.selectedRows;
	}else{
		oRows.push(oRow);
	}
	if(oRows.length == 0){
		//roleListFrameErrorbarWidget("${getText('foundation.role.checkselected')}");
		alert("未选择模块");
		return false;
	}
	
	if(oRows.length==1){
		oRows[0].rowIndex = CUI("#rowIndex").val();
		arrObj.push(oRows[0]);
		_callback_module_moduleReference(arrObj);
	}

}
</script>

