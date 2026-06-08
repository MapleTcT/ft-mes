<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>${getText('ec.module.upload')}</title>
<@maincss/>
<@mainjs/>
</head>
<body id="upload_page">
<script type="text/javascript">
	function beforesubmit(){
		var fileName = document.getElementById('receiveFile').value;
		var uploadFilter = document.getElementsByName('uploadFilter');
		var checked_counts = 0;
		for(var i=0;i<uploadFilter.length;i++){
		    if(uploadFilter[i].checked){
		        checked_counts++;
		    }
		}

		if(!fileName || fileName.length == 0) {
			parent.CUI.Dialog.alert("${getHtmlText('ec.module.generate.upload.select')}");
			return false;
		}else if(!fileName.endsWith('.zip') ){
			parent.CUI.Dialog.alert("${getHtmlText('ec.module.upload.fileTypeError')}");
			return false;
		}else if(checked_counts==0){
		    parent.CUI.Dialog.alert("${getHtmlText('ec.module.upload.checkboxisnull')}");
        	return false;
		}
		return true;
	}
	function selectAllItem(obj){
		var resCheck=obj.checked;
		var checkboxs=document.getElementsByName("uploadFilter");
		for(var i=0;i<checkboxs.length;i++){
			checkboxs[i].checked=resCheck;
		}
	}

	function selectThisItem(obj){
		var resCheck=obj.checked;
		if(!resCheck){
			document.getElementById("allSelect").checked=false;
		}else{
			var checkboxs=document.getElementsByName("uploadFilter");
			for(var i=0;i<checkboxs.length;i++){
				if(!checkboxs[i].checked){
					return;
				}
			}
			document.getElementById("allSelect").checked=true;
		}
	}
</script>
<div style="padding:15px 20px 0 20px;">
<form id="uploadForm" target="transfer" onSubmit="javascript:return beforesubmit();" method="post" enctype="multipart/form-data">
<input type="hidden" id="uptype" name="uptype" value="${uptype!'zip'}" />
<input type="hidden" name="module.code" value="" id="module_code"/>
<input type="hidden" name="changeFile" value="" id="changeFile"/>
<input type="file" name="receiveFile" id="receiveFile"/>
<br/><br/>
<span id="upload_allSelect_span">
<input type="checkbox"  name="allSelect"  id="allSelect" onclick="selectAllItem(this)" checked /><label for="allSelect" style="font-size:12px;padding-right:15px;">${getHtmlText('foundation.common.allSelect')}</label>
</span>
<span id="upload_moduleinfo_span">
<input type="checkbox"  name="uploadFilter" onclick="selectThisItem(this)" value="ModuleInfo" id="upload_moduleinfo" checked /><label for="upload_moduleinfo" style="font-size:12px;padding-right:15px;">${getHtmlText('ec.engine.upload.moduleinfo')}</label>
</span>
<span id="upload_international_span">
<input type="checkbox"  name="uploadFilter" onclick="selectThisItem(this)" value="International" id="upload_international" checked /><label for="upload_international" style="font-size:12px;padding-right:15px;">${getHtmlText('ec.engine.upload.international')}</label>
</span>
<span id="upload_view_span">
<input type="checkbox"  name="uploadFilter" onclick="selectThisItem(this)" value="View" id="upload_view" checked/><label for="upload_view" style="font-size:12px;padding-right:15px;">${getHtmlText('ec.engine.upload.view')}</label>
</span>
<br/><br/>
<span id="upload_custfield_span">
<input type="checkbox"  name="uploadFilter" onclick="selectThisItem(this)" value="CustomProperty" id="upload_custfield" checked/><label for="upload_custfield" style="font-size:12px;padding-right:15px;">${getHtmlText('ec.engine.upload.custfield')}</label>
</span>
<#--<span id="upload_workflow_span">
<input type="checkbox" name="uploadFilter" onclick="selectThisItem(this)" value="WorkFlow" id="upload_workflow" checked/><label for="upload_workflow" style="font-size:12px;padding-right:15px;">${getHtmlText('ec.engine.upload.workflow')}</label>
</span>
<span id="upload_printtemplate_span">
<input type="checkbox"  name="uploadFilter" value="PrintTemplate" id="upload_printtemplate" checked/><label for="upload_printtemplate" style="font-size:12px;padding-right:15px;">${getHtmlText('ec.engine.upload.printtemplate')}</label>
</span>
<span id="upload_custproperty_span">
<input type="checkbox"  name="uploadFilter" onclick="selectThisItem(this)" value="CodeProperty" id="upload_custproperty" checked/><label for="upload_custproperty" style="font-size:12px;padding-right:15px;">${getHtmlText('ec.engine.upload.custproperty')}</label>
</span>
<span id="upload_logconfig_span">
<input type="checkbox" name="uploadFilter" onclick="selectThisItem(this)" value="AuditConfig" id="upload_logconfig" checked/><label for="upload_logconfig" style="font-size:12px;padding-right:15px;">${getHtmlText('ec.engine.upload.logconfig')}</label>
</span>
<br/><br/>
<span id="upload_importTemplate_span">
<input type="checkbox" name="uploadFilter" onclick="selectThisItem(this)" value="ImportTemplate" id="upload_importTemplate" checked/><label for="upload_importTemplate" style="font-size:12px;padding-right:15px;">${getHtmlText('ec.configMenu.importTemplateConfig')}</label>
</span>
<span id="upload_schedulerJob_span">
<input type="checkbox" name="uploadFilter" onclick="selectThisItem(this)" value="Scheduler" id="upload_schedulerJob" checked/><label for="upload_schedulerJob" style="font-size:12px;padding-right:15px;">${getHtmlText('ec.configMenu.schedulerConfig')}</label>
</span>-->
<br/><br/>
</form>
</div>
<iframe style="display:none;" id="transfer" name="transfer" ></iframe>
</body>
</html>