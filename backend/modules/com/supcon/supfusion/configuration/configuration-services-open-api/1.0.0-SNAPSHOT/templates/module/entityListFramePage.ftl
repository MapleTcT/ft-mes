<!DOCTYPE html>
<html  xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="X-UA-Compatible" content="IE=edge" />
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<@maincss />
<@mainjs />

<title>${getText('ec.module.chooseEntity')}</title>
</head>
<body id="entityList_page">
<script type="text/javascript">
	function beforesubmit(){
		var allItem = ec_module_entity_datatableWidget.getAllData();
		var selecteditem = ec_module_entity_datatableWidget.getSelectedRow();
		if(selecteditem.length == allItem.length){
			$('#uploadFile').val(true);
		}
		
		if(selecteditem.length == 0){
			CUI.Dialog.alert('${getText('ec.entity.chooseUpload')}');
			return false;
		}
		var entities = "";
		for(var i=0;i<selecteditem.length;i++){
			entities += selecteditem[i].code+',';
		}
		$('#selectedEntities').val(entities);
		
		
		
		return true;
	}
	
</script>
<form id="uploadEntity" onSubmit="javascript:return beforesubmit();" action="/msService/ec/module/migrateupload" method="post" enctype="multipart/form-data">
<@s.hidden name="module.code" />
<@s.hidden name="upload_metadata"/>
<@s.hidden name="upload_customcode"/>
<@s.hidden name="upload_flow"/>
<@s.hidden name="upload_filter"/>
<@s.hidden name="selectedEntities"/>
<@s.hidden name="filePath"/>
<@s.hidden name="uploadFile"/>
<@datatable paginator=false withoutConfigTable=true firstLoad=true hidekey="['code','version']" id="ec_module_entity_datatable" style="margin:6px 12px;" dataUrl="/msService/ec/module/receive2?filePath=${(filePath)!}&module.code=${module.code}">
			<@datacolumn key="checkbox" type="checkbox" checkall="true" textalign="center" label="" width=30 />
			<@datacolumn key="entityName" label="${getHtmlText('ec.entity.entityCode')}" width=100 />
			<@datacolumn key="nameInternational" label="${getHtmlText('ec.entity.entityName')}" width=150 />
			<@datacolumn textalign="center" type="boolean" key="workflowEnabled" label="${getHtmlText('ec.entity.workflowEnabled')}" width=120 />
			<@datacolumn textalign="center" type="boolean" key="isBase" label="${getHtmlText('ec.entity.isBase')}" width = 120/>
			<@datacolumn textalign="center" type="boolean" key="crossCompanyFlag" label ="${getText('ec.entity.crossCompanyFlag')}" width=120 />
</@datatable>
</form>	
</body>
</html>