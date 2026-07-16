"use strict";

function editOrValueChange(valType, editType, operateField) {
  var table = ReactAPI.getComponentAPI("SupDataGrid").APIs(
    "PATROL_1.0.0_inputStandard_inputStanEditdg1575356531546"
  );

  table.deleteLine();
  if (editType === "PATROL_editType/input" || editType === "PATROL_editType/whether") {
    $("#btn-add").attr("style", "display: none;");
    $("#btn-delete").attr("style", "display: none;");
  } else {
    $("#btn-add").attr("style", "display: inline-block;");
    $("#btn-delete").attr("style", "display: inline-block;");
  }

  if (valType === "PATROL_valueType/char") {
    ReactAPI.getComponentAPI("InputNumber").APIs("inputStandard.decimalPlace").setReadonly(true);
    ReactAPI.getComponentAPI("Reference").APIs("inputStandard.unitID.name").setReadonly(true);
    if (editType === "PATROL_editType/whether" && table.getDatagridData().length === 0) {
      table.addLine([{ valueName: "是" }, { valueName: "否" }], true);
      table.setDatagridCellAttr(0, "valueName", { readonly: true });
      table.setDatagridCellAttr(1, "valueName", { readonly: true });
    }
  }

  if (valType === "PATROL_valueType/number") {
    ReactAPI.getComponentAPI("Reference").APIs("inputStandard.unitID.name").setReadonly(false);
    if (editType) {
      if (editType === "PATROL_editType/input") {
        ReactAPI.getComponentAPI("InputNumber").APIs("inputStandard.decimalPlace").setReadonly(false);
      } else {
        if (editType === "PATROL_editType/whether") {
          if (operateField === "valType") {
            ReactAPI.getComponentAPI("SystemCode").APIs("inputStandard.valType").setValue();
            ReactAPI.showMessage(
              "f",
              ReactAPI.international.getText("PATROL.custom.random1607587801943")
            );
          } else {
            ReactAPI.getComponentAPI("SystemCode").APIs("inputStandard.editType").setValue();
            ReactAPI.showMessage(
              "f",
              ReactAPI.international.getText("PATROL.custom.randon1575365823827")
            );
          }
          return false;
        }
        ReactAPI.getComponentAPI("InputNumber").APIs("inputStandard.decimalPlace").setReadonly(true);
      }
    }
  }

  return true;
}
