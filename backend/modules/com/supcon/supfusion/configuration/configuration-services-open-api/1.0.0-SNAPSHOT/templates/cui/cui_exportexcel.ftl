<#macro exportexcel action,getRequireDataAction="",settingtext="",settingUrl="",prepareParams="",formId="",look="button",width=250,height=170,text="",exportPageChoose=1,dtPage="page",prefix="",permissionCode="",modelCode="",viewCode="",importFlag="false">
<script type="text/javascript" charset="utf-8" language="javascript">
var exportDialog = null;
function ${prefix}_showExportDialog(formId){
	try{
		if(exportDialog!=null&&exportDialog.isShow==1){
			return false;
		}
		var url = "/foundation/workbench/export-init?dtPage=${dtPage}&exportPageChoose=${exportPageChoose}&modelCode=${modelCode}&viewCode=${viewCode}&permissionCode=${permissionCode}";
		var params = {};
		var actionName = '${action}';
		var getRequireDataActionName = '${getRequireDataAction}';
		params = actionName;
		params.getRequireDataAction = getRequireDataActionName;
		params.settingtext = '${settingtext}';
		params.settingUrl = '${settingUrl}';
		params.importFlag = '${importFlag}';
		<#if permissionCode?? && permissionCode?length gt 0>
		params.permissionCode = '${permissionCode}';
		</#if>
		var prepareParams = '${prepareParams}';
		var formParams = {};
		if(prepareParams != null && prepareParams != '') {
			eval('formParams = ' + prepareParams + '();');
			CUI.each(formParams,function(k,v){
				if(v!=null&&v!=undefined&&v!=''&&v!='null') {
					params[k] = v;
				}
			});
		} else {
			if(formId) {
				;
			} else {
				formId = '${formId}';
			}
			var formObj = null;
			if(formId == null || formId == undefined || formId == 'null' || formId == '') {
				formObj = CUI('form[action*="${action}"]').first();
			} else {
				formObj = CUI('#'+formId);
			}
			if(formObj!=null) {
				formObj.data("prefix", '${prefix}');
				if(formObj.attr('exportUrl') && formObj.attr('exportUrl')!='') {
					params = formObj.attr('exportUrl');
				}
				if(formObj.data()) {
					$.each(formObj.data(), function(key1, value1){
						params[key1]=value1;
					});
				}
				formObj.find('input,select').each(function(){
					//var name = CUI(this).attr('name');
					//if(name == null || name == '') {
				//		name = CUI(this).attr('id');
				//	}
			//		params[name]=CUI(this).val();
						 var name = CUI(this).attr('name');
						 if (name == null || name == '') {
							 name = CUI(this).attr('id');
						 }
						 var value = CUI(this).val();
						 if(name != undefined && name != null && value != undefined && value != null && value != "") {
							 if(name.endsWith('_start')) {
								 var dateType = CUI(this).attr('dateType');
								 if(dateType && dateType == 'date') {
									 value += " 00:00:00";
								 }
							 }
							 if(name.endsWith('_end')) {
								 var dateType = CUI(this).attr('dateType');
								 if(dateType && dateType == 'date') {
									 value += " 23:59:59";
								 }
							 }
							 if(CUI(this).attr('type')=='checkbox'||CUI(this).attr('type')=='radio') {
								 if(CUI(this).prop("checked")){
									 params[name]=value;
									 //postData += "&" + name + "=" + value;
								 }
							 }else{
								 params[name]=value;
								 //postData += "&" + name + "=" + value;
							 }
						 }

				});
			}
		}
		exportDialog = new CUI.Dialog({
			title: "${getHtmlText('common.button.export')}",
			url:url,
			modal:true,
			argument:params,
			width: 450,
			height: 243,
			dragable:true,
			buttons:[{name:"${getHtmlText('common.button.export')}",handler:function(){typeof submitExportForm  === 'function' && submitExportForm();}},{name:"${getHtmlText('common.button.cancel')}",handler:function(){this.close();}}]
		});
		exportDialog.show();
	}catch(e){}
}
</script>
<#if look == "button">
	<input name="_exportButton" id="_exportButton" type="button" value="${text}" onclick="javascript:${prefix}_showExportDialog();" />
<#else>
	<span name="_exportLabel" id="_exportLabel" style="cursor:pointer;border:none;" onclick="javascript:_showExportDialog();">${text}</span>
</#if>
</#macro>