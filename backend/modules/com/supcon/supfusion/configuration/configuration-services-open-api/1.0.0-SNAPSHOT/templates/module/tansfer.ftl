<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>${getText('ec.module.upload')}</title>
<@maincss/>
<@mainjs/>
</head>
<body id="tansfer_Page">
<script type="text/javascript">
$(function(){
	if(errorMsg.value){
		CUI.Dialog.alert(errorMsg.value);
	}else{
		var url = '/msService/ec/module/showEntity?filePath=${(filePath)!}&module.code=';
		if(parent.$('input[name="module.code"]').val() != -1 || parent.$('input[name="module.code"]').val() != undefined || parent.$('input[name="module.code"]').val() != 'undefined'){
			url += parent.$('input[name="module.code"]').val();
		}
		if(parent.document.getElementById('upload_customcode')!=null && parent.document.getElementById('upload_customcode').checked){
			url += '&upload_customcode=true';
		}
		if(parent.document.getElementById('upload_metadata')!=null && parent.document.getElementById('upload_metadata').checked){
			url += '&upload_metadata=true';
		}
		if(parent.document.getElementById('upload_flow')!=null && parent.document.getElementById('upload_flow').checked){
			url += '&upload_flow=true';
		}
		if(parent.document.getElementById('upload_filter')!=null && parent.document.getElementById('upload_filter').checked){
			url += '&upload_filter=true';
		}
		var view2 = parent.parent.ec.module.uploadWizardDialog.getView( 'wizar_iframe_step2' );
		view2.show();
		view2.load(url);
	}
});
</script>
<@s.hidden name="errorMsg" />
<@s.hidden name="filePath" />
</body>
</html>