<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>${getText('ec.entity.migrate')}</title>
<@maincss/>
<@mainjs/>
</head>
<body id="migrate_page">
<script type="text/javascript">
	function beforesubmit(){
		$('input[name="srcmodule.code"]').attr('disabled',true);
		if($('input[name="targetModule.name"]').val() == null || $('input[name="targetModule.name"]').val() == undefined ||  $('input[name="targetModule.name"]').val() == '') {
			CUI.Dialog.alert("${getHtmlText('ec.entity.migrate.content.select')}");
			return false;
		}
		return true;
	};
	
	function moduleSelects_selectEvent(type,url,title,refparam){
		_dialog = foundation.common.select({
			pageType : type,
			closePage : true,
			callBackFuncName : '_callback_moduleSelects',
			url : url,
			title : title,
			params : refparam,
		});
	}
	
	function _callback_moduleSelects(objs){		
		$('input[name="targetModule.name"]').val(objs[0].nameInternational);
		$('input[id="targetModule.code"]').val(objs[0].code);
		if(_dialog){
			_dialog.close();
		}
	};
	
	
	foundation.common._ec_module_ref2__callbackFunction = function(){
		if(datatable_migmoduleRef._DT.selectedRows.length == 0){
				ec_module_refBarWidget.show("${getHtmlText('ec.entity.migrate.selectRecord')}");
		}
		_callback_moduleSelects(datatable_migmoduleRef._DT.selectedRows);
	};
	
	
</script>

<div style="padding:15px 20px 0 20px;">
<form id="migrateForm" onSubmit="javascript:return beforesubmit();" action="/msService/ec/entity/migrate" method="post" enctype="multipart/form-data">
<#--  <@s.hidden name="entityCodes" />  -->
<input type="hidden" name="entityCodes"  <#if entityCodes?? >value="${entityCodes}"</#if> id="entityCodes">
<#--  <@s.hidden name="srcmodule.code" />  -->
<input type="hidden" name="srcmodule.code"  <#if srcmodule.code?? >value="${srcmodule.code}"</#if>  id="srcmodule_code">
<table class="infoTable"  cellpadding="0" cellspacing="0" border="0" align="center" width="90%" style="margin-top: 20px">
	<tr>
		<td class="la" style="width: 20%;padding-right: 5px;" align="right">${getHtmlText('ec.module.targetModule')}</td>
		<td>	<div class="fix-input">
	<div class="fix-search-click" id="migrateForm_base_module_codediv">
	<input type="text" readonly="true" mnetype="mnemonic" multiable="false" class="cui-noborder-input" name="targetModule.name" property_type="" isprecise="true" onpropertychange="CUI.iePreciseClear('targetModule','migrateForm')" formid="migrateForm" tabindex="" id="targetModule_name" value="" originalvalue="" style="" title=""  exp="" onblur="var that=this;setTimeout(function(){if(!window.mnePageBtnFlag){;cleanMneDiv(that,0,'migrateForm');}},200);" iscrosscompany="false" refviewcode="" currentviewcode="" autocomplete="off">
	<input type="hidden" reset="false" id="targetModule.code" name="targetModule.code" value="">
	<input id="migrateForm_base_click_button" formne="module_code" style="display:block;" type="button" devalue="" class="cui-search-click" value="" onclick="moduleSelects_selectEvent('other','/msService/ec/module/ref2?multiSelect=true<#if srcmodule?? && srcmodule.code??>&module.code=${(srcmodule.code)!}</#if>','${getText("ec.module.mbmk")}','')">
	</div>
</div>
</td>
	</tr>
</table>
</form>
</div>
</body>
</html>