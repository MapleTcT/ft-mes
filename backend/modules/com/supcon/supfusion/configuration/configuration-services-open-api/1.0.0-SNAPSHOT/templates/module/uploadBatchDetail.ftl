
<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<@maincss/>
<@mainjs/>
</head>
<body>
<div >
	<form id="deploy_queryForm" name="deploy_queryForm" onsubmit="return false;">
		
	</form>
	<@datatable id="ec_module_upload_datatable" dataUrl="/msService/ec/module/uploadBatch/list-query" >
		<@datacolumn key="moduleCode" label="${getHtmlText('ec.module.mkbm')}" width=150  sortable=false />
		<@datacolumn columnName="UPLOAD_DATE"    showFormat="YMD_HMS"   key="uploadDate"   label="${getText('ec.module.uploadDate')}" textalign="center"  width=150   type="datetime" sortable=false />
		<@datacolumn key="uploadState" label="${getHtmlText('ec.module.uploadState')}" width=100 sortable=false />
		<@datacolumn key="oldVersion" label="${getHtmlText('ec.module.oldVersion')}" width=150 sortable=false />
		<@datacolumn key="uploadStaff.name" label="${getHtmlText('ec.module.uploadstaff')}" width=100 sortable=false />
		<@datacolumn key="totalTime" label="${getHtmlText('ec.module.uploadingtotalTime')}" width=100 sortable=false />
		<@datacolumn key="uploada" label="${getHtmlText('ec.module.xzmkhs')}" width=100 sortable=false />
		<@datacolumn key="uploadb" label="${getHtmlText('ec.module.jymkhs')}" width=100 sortable=false />
		<@datacolumn key="uploadc" label="${getHtmlText('ec.module.jxmwjhs')}" width=100 sortable=false />
		<@datacolumn key="uploadd" label="${getHtmlText('ec.module.drmbhs')}" width=100 sortable=false />
		<@datacolumn key="uploade" label="${getHtmlText('ec.module.kbmkhs')}" width=100 sortable=false />
		<@datacolumn key="uploadf" label="${getHtmlText('ec.module.drxtbmhs')}" width=100 sortable=false />
		<@datacolumn key="uploadg" label="${getHtmlText('ec.module.portleths')}" width=100 sortable=false />
		<@datacolumn key="uploadh" label="${getHtmlText('ec.module.dealinter')}" width=100 sortable=false />
		<@datacolumn key="uploadi" label="${getHtmlText('ec.module.drgjh')}" width=100 sortable=false />
		<@datacolumn key="uploadj" label="${getHtmlText('ec.module.jymkhs')}" width=100 sortable=false />
		<@datacolumn key="uploadk" label="${getHtmlText('ec.module.crjlsj')}" width=100 sortable=false />
	</@datatable>
</div>
</body>
</html>
