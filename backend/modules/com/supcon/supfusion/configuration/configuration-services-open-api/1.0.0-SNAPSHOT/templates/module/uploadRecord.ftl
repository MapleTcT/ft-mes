<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<@maincss />
<@mainjs />
</head>
<body id="uploadDetail">
<div style="display: block;">
	<@datatable paginator = false moreButtonResizeFlag=true firstLoad=true id="ec_module_upload2_datatable" dataUrl="/msService/ec/module/upload/list-query?uploadInfo.uploadInfoBatch.id=${uploadBatchId}"  renderOverEvent="upload_renderOverEvent" pageInitMethod="upload_PageInitMethod" withoutConfigTable=true  noCookie=false>
		<@datacolumn key="moduleCode" label="${getHtmlText('ec.module.mkbm')}" width=130  sortable=false />
		<@datacolumn key="moduleName" label="${getHtmlText('ec.module.modulename')}" width=70 sortable=false />
		<#assign uploadDate_displayDefaultType  = ''>
		<@datacolumn columnName="UPLOAD_DATE"    showFormat="YMD_HMS" defaultDisplay="${uploadDate_displayDefaultType!}"  key="uploadDate"   label="${getText('ec.module.uploadDate')}" textalign="center"  width=150   type="datetime" sortable=false />
		<@datacolumn key="uploadState" label="${getHtmlText('ec.module.uploadState')}" width=70 sortable=false />
		<@datacolumn key="oldVersion" label="${getHtmlText('ec.module.oldVersion')}" width=75 sortable=false />
		<@datacolumn key="uploadStaff.name" label="${getHtmlText('ec.module.uploadstaff')}" width=70 sortable=false />
		
	</@datatable>
</div>

<script type="text/javascript">
	
	/**
	 * 上载记录页面渲染
	 * @method uploadQuery
	 */
	function upload_renderOverEvent(){
		$.each($('#ec_module_upload2_datatable_tbody tr'),function(index,data){
		    var state = $(data).find("td[key=uploadState]").text();
		    if(null != state && undefined != state && "" != state && '失败'==state){
		        $(data).find("td[key=uploadState]").css('color','#ff0000');
		    }
		    }
		)
	}
	function upload_PageInitMethod(){
		
	}
</script>
</body>
</html>