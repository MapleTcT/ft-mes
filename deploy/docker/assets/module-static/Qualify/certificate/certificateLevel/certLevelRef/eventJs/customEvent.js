//======================================================================================
//视图的事件，请不要在这里修改代码，修改的代码会被覆盖
//======================================================================================

/*==================================Qualify_1.0.0_certificate_certLevelRef_renderOver事件==================================*/
var result = ReactAPI.getSystemConfig({ moduleCode: "Qualify", key: "Qualify.setDefaultLevel" })["Qualify.setDefaultLevel"];
if (result) {
    var dgData = ReactAPI.getComponentAPI().SupDataGrid.APIs("Qualify_1.0.0_certificate_certLevelRef_certificateLevel_sdg").getDatagridData();
    if (dgData.length > 0) {
        for (var i = 0; i < dgData.length; i++) {
            var row = dgData[i];
            if (row.code.includes("defaultLevel")) {
                ReactAPI.getComponentAPI().SupDataGrid.APIs("Qualify_1.0.0_certificate_certLevelRef_certificateLevel_sdg").setCellValueByKey(row.rowIndex, "code", null);
                ReactAPI.getComponentAPI().SupDataGrid.APIs("Qualify_1.0.0_certificate_certLevelRef_certificateLevel_sdg").setCellValueByKey(row.rowIndex, "name", null);
            }
        }
    }
}

/*==================================Qualify_1.0.0_certificate_certLevelRef_ptPageInit事件==================================*/
ReactAPI.getComponentAPI('SearchPanel').APIs("Qualify_1.0.0_certificate_certLevelRef_certificateLevel_sp").updateSearch()

