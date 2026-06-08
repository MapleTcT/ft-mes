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
		var fileName = $('#receiveFile').val();
		if(!fileName || fileName.length == 0) {
			CUI.Dialog.alert("${getHtmlText("ec.module.generate.upload.select")}");
			return false;
		}else if(!fileName.endsWith('.zip')){
			CUI.Dialog.alert("${getHtmlText("ec.module.upload.fileTypeError")}");
			return false;
		}
		if($('[id^="upload"]:checkbox').length > 0 && (!$('#upload_metadata').prop('checked') && !$('#upload_customcode').prop('checked'))) {
			CUI.Dialog.alert("${getHtmlText("ec.module.generate.upload.content.select")}");
			return false;
		}
		return true;
	}
	function changeuptype(obj){
		document.getElementById('changeFile').value= 'true';
		var val = obj.value;
		if(val.indexOf(".xml") > 0){
			document.getElementById("uptype").value = "xml";
		}
	}
	function uploadALL(obj){
		if(obj.checked){
			parent.$( '#dialog_btn_migrate', parent.ec.module.uploadWizardDialog.dialog._buttonbar ).eq(0).attr('canclick','false').addClass( 'cui-btn-gray' );
			parent.$( '#dialog_btn_upload', parent.ec.module.uploadWizardDialog.dialog._buttonbar ).eq(0).attr('canclick','true').removeClass( 'cui-btn-gray' );
		}else{
			parent.$( '#dialog_btn_migrate', parent.ec.module.uploadWizardDialog.dialog._buttonbar ).eq(0).attr('canclick','true').removeClass( 'cui-btn-gray' );
			parent.$( '#dialog_btn_upload', parent.ec.module.uploadWizardDialog.dialog._buttonbar ).eq(0).attr('canclick','false').addClass( 'cui-btn-gray' );
		}
	}
</script>
<div style="padding:15px 20px 0 20px;">
<form id="uploadForm" target="transfer" onSubmit="javascript:return beforesubmit();" action="/msService/ec/module/listEntity" method="post" enctype="multipart/form-data">
<input type="hidden" id="uptype" name="uptype" value="${uptype!'zip'}" />
<@s.hidden name="module.code" />
<@s.hidden name="changeFile" />
<input type="file" name="receiveFile" id="receiveFile" onchange="changeuptype(this)" />
<br/><br/>
<span id="allupload">
<input type="checkbox" value="true" name="allupload" id="allupload" onclick="uploadALL(this)" checked/><label for="allupload" style="font-size:12px;padding-right:15px;">${getHtmlText('ec.module.uploadAll')}</label>
</span>
<#if (module.code)?? && (module.code) != "undefined" && (module.code) != "-1">
<span id="upload_customcode_span">
<input type="checkbox" value="true" name="upload_customcode" id="upload_customcode" checked/><label for="upload_customcode" style="font-size:12px;padding-right:15px;">${getHtmlText('ec.upload.uploadCustomCode')}</label>
</span>
<span id="upload_metadata_span">
<input type="checkbox" value="true" checked="checked" name="upload_metadata" id="upload_metadata" onclick="if(this.checked){$('#upload_flow_span').show();$('#upload_filter_span').show();}else{$('#upload_flow_span').hide();$('#upload_filter_span').hide();}"/><label for="upload_metadata" style="font-size:12px;padding-right:15px;">${getHtmlText('ec.upload.uploadMeta')}</label>
</span>
<span id="upload_flow_span" >
<input type="checkbox" value="true" checked="checked" name="upload_flow" id="upload_flow" /><label for="upload_flow" style="font-size:12px;padding-right:15px;">${getHtmlText('ec.upload.importFlow')}</label>
</span>
<span id="upload_filter_span" >
<input type="checkbox" value="true" name="upload_filter" id="upload_filter" /><label for="upload_filter" style="font-size:12px;padding-right:15px;">${getHtmlText('ec.upload.filter')}</label>
</span>
<br/><br/>
<span id="upload_importTemplate_span" >
<input type="checkbox" value="true" checked="checked" name="upload_importTemplate" id="upload_importTemplate" /><label for="upload_importTemplate" style="font-size:12px;padding-right:15px;">${getHtmlText('ec.upload.importTemplateConfig')}</label>
</span>
<span id="upload_schedulerJob_span" >
<input type="checkbox" value="true" checked="checked" name="upload_schedulerJob" id="upload_schedulerJob" /><label for="upload_schedulerJob" style="font-size:12px;padding-right:15px;">${getHtmlText('ec.upload.schedulerJobConfig')}</label>
</span>
<br/><br/>
</#if>
</form>
</div>
<iframe style="display:none;" id="transfer" name="transfer" ></iframe>
</body>
</html>