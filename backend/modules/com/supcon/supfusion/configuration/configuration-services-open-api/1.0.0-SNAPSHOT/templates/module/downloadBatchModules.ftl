<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<!-- <@maincss />
<@mainjs /> -->

<title>${getText('批量下载列表')}</title>
</head>
<body id="moduleList_page">
<script type="text/javascript">
	function beforesubmit(){
		
	}
	
</script>
<form id="downloadBatch" onSubmit="javascript:return beforesubmit();" action="/msService/ec/module/downloadModule-query?isMsService=${isMsService}" method="post" enctype="multipart/form-data">
<@datatable paginator=false withoutConfigTable=true firstLoad=true hidekey="['code','version']" id="ec_modules_download_datatable" style="margin:-35px 0px 0px 0px;" dataUrl="/msService/ec/module/downloadModule-query?isMsService=${isMsService}" pageInitMethod="ec_modules_download_datatablepageInitMethod" renderOverEvent="ec_modules_download_datatableRenderOverEvent" >
			<@datacolumn key="checkbox" type="checkbox" checkall="true" textalign="center" label="" width=30 />
			<@datacolumn rowMerge=true key="category" label="${getHtmlText('ec.module.category')}" width="120" />
			<@datacolumn key="code" label="${getHtmlText('ec.module.code')}"  width="120" />
			<@datacolumn key="nameInternational" label="${getHtmlText('ec.module.name')}"  width="150" />
</@datatable>
</form>
<script type="text/javascript">
	function ec_modules_download_datatableRenderOverEvent() {
		
	}
	function ec_modules_download_datatablepageInitMethod() {
		
	}
</script>
</body>
</html>