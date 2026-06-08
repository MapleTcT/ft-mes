<#macro importexcel serviceName="",maxSize=10485760,description="",look="button",width=250,height=150,text="">
<script type="text/javascript" charset="utf-8" language="javascript">
var importDialog = null;
function _showImportDialog(){
	try{
		if(importDialog!=null&&importDialog.isShow==1){
			return false;
		}
		var url = "/foundation/workbench/init?serviceName=${serviceName}&maxSize=${maxSize}";
		var impDescription = "${getText('${description}')}";
		importDialog = new CUI.Dialog({
			title: "${getHtmlText('common.button.import')}",
			url:url,
			modal:true,
			description:impDescription,
			height:'${height}',
			width: '${width}',
			dragable:true,
			buttons:[{name:"${getHtmlText('common.button.import')}",handler:function(){CUI("body").one("click", function(event){
    		if(CUI.Dialog) CUI.Dialog.toggleAllButton(CUI(event.target).parents( 'div.ewc-dialog-buttonbar' )[0]);
    	});foundation.importExcel.submitImportForm();$('#uploadprogressbarImport').show();}},
    	{name:"${getHtmlText('common.button.cancel')}",id:"close",handler:function(){this.close();}}]
		});
		importDialog.show();
	}catch(e){}
}
</script>
<#if look == 'button'>
	<input name="_importButton" id="_importButton"  type="button" value="${text}" onclick="javascript:_showImportDialog();" />
<#else>
	<span name="_importLabel" id="_importLabel"  style="cursor:pointer;border:none;" onclick="javascript:_showImportDialog();">${text}</span>
</#if>
</#macro>