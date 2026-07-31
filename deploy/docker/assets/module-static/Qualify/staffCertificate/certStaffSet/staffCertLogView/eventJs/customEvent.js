//======================================================================================
//视图的事件，请不要在这里修改代码，修改的代码会被覆盖
//======================================================================================

/*==================================onload事件==================================*/
var Qualify_staffCertificate_certStaffSet_onload = function(){
	var selectStaff = parent.ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_staffCertificate_certStaffList_certStaff_sdg").getSelecteds()[0];
ReactAPI.getComponentAPI("Input").APIs("certStaffSet.staffId.name").setValue(selectStaff.name);//员工姓名
ReactAPI.getComponentAPI("Input").APIs("certStaffSet.staffId.code").setValue(selectStaff.code);//员工编码
}

