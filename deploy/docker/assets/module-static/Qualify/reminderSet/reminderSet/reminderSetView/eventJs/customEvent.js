//======================================================================================
//视图的事件，请不要在这里修改代码，修改的代码会被覆盖
//======================================================================================

/*==================================onload事件==================================*/
var Qualify_reminderSet_reminderSet_onload = function(){
	//设置消息接收范围
var departmentId=ReactAPI.getParamsInRequestUrl().departmentId;
var departmentName=ReactAPI.getParamsInRequestUrl().departmentName;
var department={};
if(ReactAPI.getParamsInRequestUrl().id){
	var selectedRow=ReactAPI.getComponentAPI().SupDataGrid.APIs("Qualify_1.0.0_reminderSet_reminderSetLayout_reminderSet_sdg").getSelecteds()[0];
	department.id=selectedRow.department.id;
	department.name=selectedRow.department.name;
}else{
	department.id=departmentId;
	department.name=departmentName;
}
ReactAPI.getComponentAPI().Reference.APIs("reminderSet.department.name").setValue(department);
}

