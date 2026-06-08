<script type="text/javascript" charset="utf-8" language="javascript">
var importDialog = null;
function _showImportDialog(){
	try{
		if(importDialog!=null&&importDialog.isShow==1){
			return false;
		}
		var url = "/foundation/workbench/init?serviceName=${parameters.serviceName}&maxSize=${parameters.maxSize?default(10485760)}";
		importDialog = new CUI.Dialog({
			title: "Excel${action.getText("common.button.import")}",
			url:url,
			modal:true,
			description:"${action.getText(parameters.description)}",
			height:'${parameters.height}',
			width: '${parameters.width}',
			dragable:true,
			buttons:[{name:"${action.getText("common.button.import")}",handler:function(){CUI("body").one("click", function(event){
    		if(CUI.Dialog) CUI.Dialog.toggleAllButton(CUI(event.target).parent().parent()[0]);
    	});foundation.importExcel.submitImportForm();}},{name:"${action.getText("common.button.cancel")}",id:"close",handler:function(){this.close();}}]
		});
		importDialog.show();
		
	}catch(e){}
}
</script>
<#if parameters.look?default('button') == 'button'>
	<input name="_importButton" id="_importButton"  type="button" value="${parameters.text}" onclick="javascript:_showImportDialog();" />
<#else>
	<span name="_importLabel" id="_importLabel"  style="cursor:pointer;border:none;" onclick="javascript:_showImportDialog();">${parameters.text}</span>
</#if>