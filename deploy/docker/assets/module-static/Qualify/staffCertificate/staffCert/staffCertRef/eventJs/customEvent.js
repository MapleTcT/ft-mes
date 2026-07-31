//======================================================================================
//视图的事件，请不要在这里修改代码，修改的代码会被覆盖
//======================================================================================

/*==================================Qualify_1.0.0_staffCertificate_staffCertRef_renderOver事件==================================*/
var dg = ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_staffCertificate_staffCertList_staffCert_sdg");
var rowLength = dg.getDatagridData().length;

for(var i=0; i<rowLength; i++) {
	//状态
	if(dg.getValueByKey(i, "certState") != null) {
		var certStateValue = dg.getValueByKey(i, "certState.value");
		//状态颜色
		if(certStateValue == ReactAPI.international.getText("Qualify.systemCodevalue.randon1570518178983")) {
			//过期
			dg.setDatagridCellAttr(i, "certState.value", { style: { color: "#b30303" } });
		} else if(certStateValue == ReactAPI.international.getText("Qualify.systemCodevalue.randon1570518205216")) {
			//失效
			dg.setDatagridCellAttr(i, "certState.value", { style: { color: "#b7b7b7" } });
		}
		
	}
	
	//提醒状态	
	if(dg.getValueByKey(i, "remindState") != null) {
		var remindStateValue = dg.getValueByKey(i, "remindState.value");
		//提醒状态颜色
		if(remindStateValue == ReactAPI.international.getText("Qualify.systemCodevalue.randon1570518307694")) {
			dg.setDatagridCellAttr(i, "remindState.value", { style: { backgroundColor: "#F0DAD2" } })
		} else if (remindStateValue == ReactAPI.international.getText("Qualify.systemCodevalue.randon1570518325619")) {
			dg.setDatagridCellAttr(i, "remindState.value", { style: { backgroundColor: "#D5F3F4" } })
		}

	}
}

