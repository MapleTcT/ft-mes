//======================================================================================
//视图的事件，请不要在这里修改代码，修改的代码会被覆盖
//======================================================================================

/*==================================onload事件==================================*/
var Qualify_certificateType_certificateType_onload = function(){
	if(ReactAPI.getParamsInRequestUrl().id == undefined || ReactAPI.getParamsInRequestUrl().id == "") {
	//新增时，按左边树选中分类显示
	var selectedNode = parent.ReactAPI.getComponentAPI("NavTree").APIs("Qualify_1.0.0_certificateType_cerTypeLayOut_certificateType_nt").getSelectedTreeNode();
	if(selectedNode.id == -1) {
		ReactAPI.getComponentAPI("Input").APIs("extraCol1").setValue("");
	} else {
		ReactAPI.getComponentAPI("Input").APIs("extraCol1").setValue(selectedNode.title);
	}
} else {
	//查询上级名称
	$.ajax({
		url:"/msService/Qualify/certificateType/certificateType/getCertificateTypeParentName?certificateTypeId="+ReactAPI.getParamsInRequestUrl().id,
		async:false,
		success:function(res){
			res=JSON.parse(res);
			if(res.code == 200){
				ReactAPI.getComponentAPI("Input").APIs("extraCol1").setValue(res.data);
			}
		}
	});
}
}

