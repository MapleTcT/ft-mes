<@errorbar id="EditDialogErrorBar" />
<@datatable dtPage="backupViewPage" dataUrl="/msService/ec/view/listBackupView?viewCode=${code}&isProj=${isProj?string}"  id="backupViewList" hidekey="['code','view.code','version']" style="margin:4px 10px;" height=290 transMethod="post" >
	<@datacolumn label="${getHtmlText('ec.view.createStaffId')}" key="publishStaff.name" width=100 />
	<@datacolumn showFormat="YMD_HMS" label="${getHtmlText('ec.view.createTime')}" key="publishDate" type="datetime" width=200 />
</@datatable>
<script type="text/javascript">
ec.view.selectHistory = function(){
	var selectedRows = backupViewListWidget.selectedRows;
	if(selectedRows.length == 0){
		return false;
	}
	return selectedRows[0];
};

ec.view.restore = function() {
	var selected = ec.view.selectHistory();
	if(selected){
		if(!confirm("${getText('ec.view.backup')}")){
			return;
		}
		CUI.ajax({
			type : 'GET',
			<#if isProj?? && isProj>
			data : {'backupView.code' : selected.code,'isProj' : true},
			<#else>
			data : {'backupView.code' : selected.code},
			</#if>
			url : '/msService/ec/view/restoreView',
			success : function(msg){
				if(msg && JSON.parse(msg).success == true){
					EditDialogErrorBarWidget.showMessage("${getHtmlText('ec.view.backupsuccess')}","s");
				}else{
					EditDialogErrorBarWidget.showMessage("${getHtmlText('ec.view.backupfail')}");
				}
			}
		});
		return ; 
	}else {
		EditDialogErrorBarWidget.showMessage("${getHtmlText('ec.view.choicebackupRecord')}");
	}
};

ec.view.preview = function() {
	var selected = ec.view.selectHistory();
	if(selected){
		window.open('/msService/ec/view/previewConfig?backupView.code=' + selected.code);
	}else {
		EditDialogErrorBarWidget.showMessage("${getHtmlText('ec.view.backupview')}");
	}
};

/**
 * 发布记录删除
 * @method ec.view.delBackupView
 * @public
 */
ec.view.delBackupView=function(){
	if(datatable_backupViewList.selectedRows.length==0){
			EditDialogErrorBarWidget.showMessage("${getHtmlText('ec.view.choicerecorddel')}");
		return;
	}
	if(confirm("${getText('common.button.suredelete')}")){
		$.ajax({
			data : {"code":datatable_backupViewList.selectedRows[0].code + "@" + datatable_backupViewList.selectedRows[0].version},
			url : "/msService/ec/view/delBackupView" ,
			success : function(msg){
				if(msg){
					EditDialogErrorBarWidget.showMessage("${getHtmlText('common.delete.success')}","s");
					datatable_backupViewList.setRequestDataUrl("/msService/ec/view/listBackupView?viewCode=${code}");
				}else{
					if(msg && msg.exceptionMsg){EditDialogErrorBarWidget.showMessage(msg.exceptionMsg);}
					else{EditDialogErrorBarWidget.showMessage("${getHtmlText('common.delete.failure')}");}
				}
			}	
		});
	}
}
</script>