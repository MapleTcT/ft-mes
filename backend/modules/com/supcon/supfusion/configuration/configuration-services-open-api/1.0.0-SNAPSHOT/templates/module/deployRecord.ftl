<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<@maincss />
<@mainjs />
</head>
<body id="deployDetail">
<div style="display: block;">
	<@datatable paginator = false moreButtonResizeFlag=true firstLoad=true id="ec_module_deploy_datatable" dataUrl="/msService/ec/module/deploy/list-query?deployInfo.deployInfoBatchId=${deployBatchId}"  renderOverEvent="deploy_renderOverEvent" noCookie=false >
		<@datacolumn key="moduleCode" label="${getHtmlText('ec.module.mkbm')}" width=150  sortable=false />
		<@datacolumn key="moduleName" label="${getHtmlText('ec.module.modulename')}" width=70 sortable=false/>
		<@datacolumn key="status" label="${getHtmlText('ec.module.status')}" width=60 sortable=false/>
		<@datacolumn key="tasks" label="${getHtmlText('ec.module.tasks')}" width=60 sortable=false/>
		<@datacolumn showFormat="YMD_HMS"  key="createTime"   label="${getText('ec.module.createtime')}" textalign="center"  width=150   type="datetime" sortable=false/>
		<@datacolumn key="deployUser" label="${getHtmlText('ec.module.deployuser')}" width=75 sortable=false/>
	</@datatable>
</div>

<script type="text/javascript">
	
	/**
	 * 上载记录页面渲染
	 * @method uploadQuery
	 */
	function deploy_renderOverEvent(){
		$.each($('#ec_module_deploy_datatable tr'),function(index,data){
		    var state = $(data).find("td[key=status]").text();
		    if(null != state && undefined != state && "" != state && '失败'==state){
		        $(data).find("td[key=status]").css('color','#ff0000');
		    }
		    }
		)
	}
	function upload_PageInitMethod(){
		
	}
</script>
</body>
</html>