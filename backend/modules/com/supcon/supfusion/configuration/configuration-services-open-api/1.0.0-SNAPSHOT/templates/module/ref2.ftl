<@errorbar id="ec_module_refBar" />
<@datagrid id="migmoduleRef" cannotAddNewRow=true dblclick="sendBackModuleInfo" viewType="reference" route="0" formId="" noPermissionKeys="" modelCode="" dataUrl="/msService/ec/module/migQuery?page.pageSize=65536&module.code=${(module.code)!''}" postData="" dataGridName="moduleRefDataGrid" dtPage="page"  hidekeyPrefix="" hidekey="" transMethod="post" paginator=false editable=false buttons="" noPadding=true exportExcel=false tabViewIndex=0 pageInitMethod="">
	<@datacolumn  key="code" showFormat="TEXT" defaultValue="" defaultDisplay="" decimal=""  type="textfield" showType="textfield" textalign="center" hiddenCol=false viewUrl="" mneenable=false label="${getHtmlText('ec.module.artifact')}" width=100/>
	<@datacolumn  key="nameInternational" showFormat="TEXT" defaultValue="" defaultDisplay="" decimal=""  type="textfield" showType="textfield" textalign="center" hiddenCol=false viewUrl="" mneenable=false label="${getHtmlText('ec.module.name')}" width=300/>
</@datagrid>


<script type="text/javascript">
sendBackModuleInfo = function(event,oRow){
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
		migwizar_iframe_step1._callback_moduleSelects(arrObj);
	}
}

foundation.common._ec_module_ref2__callbackFunction = function(){
	if(migmoduleRefWidget._DT.getSelectedRow().length == 0){
		ec_module_refBarWidget.show("${getHtmlText("请选择一条记录")}");
	}else if(migmoduleRefWidget._DT.getSelectedRow().length > 1){
		ec_module_refBarWidget.show("${getHtmlText("只允许选择一条记录")}");
	}
	migwizar_iframe_step1._callback_moduleSelects(migmoduleRefWidget._DT.getSelectedRow());
}
</script>

