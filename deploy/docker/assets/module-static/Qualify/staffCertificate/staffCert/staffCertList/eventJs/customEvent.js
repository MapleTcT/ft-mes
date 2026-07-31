//======================================================================================
//视图的事件，请不要在这里修改代码，修改的代码会被覆盖
//======================================================================================

/*==================================Qualify_1.0.0_staffCertificate_staffCertList_renderOver事件==================================*/
var dg = ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_staffCertificate_staffCertList_staffCert_sdg").getDatagridData();
var rowLength = dg.length;
for(var i=0; i<rowLength; i++) {
	if(dg[i].remindState && dg[i].remindState.id){
		//复审即将到期
		if(dg[i].remindState.id=="Qualify_remindState/review"){
			ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_staffCertificate_staffCertList_staffCert_sdg").setDatagridCellAttr(i, "remindState.value", { style: { color: "#066592" } });
		}
		//有效期即将到期
		if(dg[i].remindState.id=="Qualify_remindState/valid"){
			ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_staffCertificate_staffCertList_staffCert_sdg").setDatagridCellAttr(i, "remindState.value", { style: { color: "#f55306" } });
		}
		//过期
		if(dg[i].remindState.id=="Qualify_remindState/expired"){
			ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_staffCertificate_staffCertList_staffCert_sdg").setDatagridCellAttr(i, "remindState.value", { style: { color: "#b30303" } });
		}
	}
	var row=dg[i];
	if(row.certLvId.code.includes("defaultLevel")){
		ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_staffCertificate_staffCertList_staffCert_sdg").setCellValueByKey(row.rowIndex,"certLvId.code",null);
		ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_staffCertificate_staffCertList_staffCert_sdg").setCellValueByKey(row.rowIndex,"certLvId.name",null);
	}
}

