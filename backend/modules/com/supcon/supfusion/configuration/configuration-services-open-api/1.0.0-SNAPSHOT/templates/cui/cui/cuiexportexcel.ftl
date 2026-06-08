<script type="text/javascript" charset="utf-8" language="javascript">
var exportDialog = null;
function _showExportDialog(){
	try{
		if(exportDialog!=null&&exportDialog.isShow==1){
			return false;
		}
		var url = "/foundation/workbench/export-init";
		var params = {};
		var actionName = '${parameters}';
		params = actionName;
		var settingtext = '${parameters.settingtext?default('')}';
		params.settingtext = settingtext;
		var settingUrl = '${parameters.settingUrl?default('')}';
		params.settingUrl = settingUrl;
		var prepareParams = '${parameters.prepareParams?default('')}';
		var formParams = {};
		if(prepareParams != null && prepareParams != '') {
			eval('formParams = ' + prepareParams + '();');
			CUI.each(formParams,function(k,v){
				if(v!=null&&v!=undefined&&v!=''&&v!='null') {
					params[k] = v;
				}
			});
		} else {
			var formId = '${parameters.formId?default('')}';
			var formObj = null;
			if(formId == null || formId == undefined || formId == 'null' || formId == '') {
				formObj = CUI('form[action*="${parameters}"]').first();
			} else {
				formObj = CUI('#'+formId);
			}
			if(formObj!=null) {
				formObj.find('input').each(function(){
					var name = CUI(this).attr('name');
					if(name == null || name == '') {
						name = CUI(this).attr('id');
					}
					params[name]=CUI(this).val();
				});
				formObj.find('select').each(function(){
					var name = CUI(this).attr('name');
					if(name == null || name == '') {
						name = CUI(this).attr('id');
					}
					params[name]=CUI(this).val();
				})
			}
		}
		exportDialog = new CUI.Dialog({
			title: "${action.getText("common.button.export")}",
			url:url,
			modal:true,
			argument:params,
			height:'${parameters.height}',
			width: '${parameters.width}',
			dragable:true,
			buttons:[{name:"${action.getText("common.button.export")}",handler:function(){submitExportForm();}},{name:"${action.getText("common.button.cancel")}",handler:function(){this.close();}}]
		});
		exportDialog.show();
	}catch(e){}
}
</script>
<#if parameters.look?default('button') == 'button'>
	<input name="_exportButton" id="_exportButton" type="button" value="${parameters.text}" onclick="javascript:_showExportDialog();" />
<#else>
	<span name="_exportLabel" id="_exportLabel" style="cursor:pointer;border:none;" onclick="javascript:_showExportDialog();">${parameters.text}</span>
</#if>