//======================================================================================
//视图的事件，请不要在这里修改代码，修改的代码会被覆盖
//======================================================================================

/*==================================onclick='proccess()'事件==================================*/
function proccess(){
	var selectedRows=ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_reminderPend_reminderPendList_reminderPend_sdg").getSelecteds();
	if(!(selectedRows.length>0)){
		//请选择要操作的行
		ReactAPI.showMessage("w", ReactAPI.international.getText("SupDatagrid.button.error"));
		return false;
	}
	var remindPendIds="";
	for(var i=0;i<selectedRows.length;i++){
		remindPendIds+=","+selectedRows[i].id;
	}
	$.ajax({
		url : "/msService/Qualify/reminderPend/reminderPend/processRemindPend?remindPendIds="+remindPendIds.substr(1),
		async: false,
		success:function(res){
			//处理成功
			ReactAPI.showMessage("s",ReactAPI.international.getText("EditView.notice.operate.success"));
			//刷新表体
			ReactAPI.getComponentAPI().SupDataGrid.APIs("Qualify_1.0.0_reminderPend_reminderPendList_reminderPend_sdg").refreshDataByRequst({
				type: "POST",
				url: "/msService/Qualify/reminderPend/reminderPend/reminderPendList-query",
				param: {}
			});
		}
	});
}

