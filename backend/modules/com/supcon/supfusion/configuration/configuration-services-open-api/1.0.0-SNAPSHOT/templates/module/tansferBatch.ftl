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
	var url = '/msService/ec/module/uploadBatchModules?filePath=${(filePath)!}';
	var view2 = parent.parent.ec.module.uploadWizardDialogBatch.getView( 'wizar_iframe_batch_step2' );
	view2.show();
	view2.load(url);
});
</script>
<@s.hidden name="errorMsg" />
<@s.hidden name="filePath" />
</body>
</html>