//======================================================================================
//视图的事件，请不要在这里修改代码，修改的代码会被覆盖
//======================================================================================

/*==================================onload事件==================================*/
var Qualify_staffCertificate_certStaffSet_onload = function(){
	var selectStaff = parent.ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_staffCertificate_certStaffList_certStaff_sdg").getSelecteds()[0];
ReactAPI.getComponentAPI("Input").APIs("certStaffSet.staffId.name").setValue(selectStaff.name);//员工姓名
ReactAPI.getComponentAPI("Input").APIs("certStaffSet.staffId.code").setValue(selectStaff.code);//员工编码
}

/*==================================onsave事件==================================*/
var Qualify_staffCertificate_certStaffSet_onsave = function(){
	return false;
}

/*==================================onclick='openCertLevelRef()'事件==================================*/
function openCertLevelRef() {
	//获取人员id
	var staffId = parent.ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_staffCertificate_certStaffList_certStaff_sdg").getSelecteds()[0].id;
		
	ReactAPI.createDialog("certLevelDialog", {
		//title: "资质设置",
		title: ReactAPI.international.getText("Qualify.viewtitle.randon1571193702274"),
		url: "/msService/Qualify/certificate/certificateLevel/certLevelRef?refKey=certLvId.code&fromViewCode=Qualify_1.0.0_staffCertificate_staffCertEdit&type=staff&customConditionKey=type",
		size: 5,
		isRef: true, // 是否开启参照
		callback: function(data,event) {
			console.log(data);
			stafCertAdd(data,staffId);
			event.ReactAPI.showMessage("s", ReactAPI.international.getText("foundation.common.add.success"), null, true);
			//ReactAPI.destroyDialog("certLevelDialog");
		},
		onOk:function(data,event) {
			if(data.length == 0) {
				//ReactAPI.showMessage("w", "请选择一条记录！");
				event.ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.certificateLevel.chooseOneRecord"));
				return false;
            } else {
				stafCertAdd(data,staffId);
				event.ReactAPI.showMessage("s", ReactAPI.international.getText("foundation.common.add.success"), null, true);
				//ReactAPI.destroyDialog("certLevelDialog");
			}

		},
		//okText:"选择",
		okText: ReactAPI.international.getText("Button.text.select"),
		onCancel:function(){
			ReactAPI.destroyDialog("certLevelDialog");
		},
		cancelText: ReactAPI.international.getText("Button.text.close")
	});
}

/*==================================onchange='onIssueDateChange(value,rowIndex)'事件==================================*/
function onIssueDateChange(value, rowIndex){//(值，行索引)
	var dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_staffCertificate_staffCertEditdg1571377815400");
	//判断有效期是否为空
	var validDateValue = dataGrid.getValueByKey(rowIndex, 'validDate');
	if(validDateValue != null) {
		if(new Date(dataFormat(value)).getTime() > new Date(dataFormat(validDateValue)).getTime()) {
			dataGrid.setValueByKey(rowIndex, 'issueDate', null);
			//"发证日期"不能大于"有效期至"！
			ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.staffCertificate.certStaffSet.issueDateIsLargerThanValidDate"));
			return false;
		} else {
			//有效期提醒日期
			var vremindDateValue = dataGrid.getValueByKey(rowIndex, 'vremindDate');
			if(vremindDateValue != null) {
				if(new Date(dataFormat(value)).getTime() > new Date(dataFormat(vremindDateValue)).getTime()) {
					dataGrid.setValueByKey(rowIndex, 'issueDate', null);
					//"发证日期"不能大于"有效期提醒日期"！
					ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.staffCertificate.certStaffSet.issueDateIsLargerThanVremindDate"));
					return false;
				}
			}
		}
	}
	
	var reviewDateValue = dataGrid.getValueByKey(rowIndex, 'reviewDate');
	if(reviewDateValue != null) {
		//判断复审日期是否为空
		
		if(new Date(dataFormat(value)).getTime() > new Date(dataFormat(reviewDateValue)).getTime()) {
			dataGrid.setValueByKey(rowIndex, 'issueDate', null);
			//"发证日期"不能大于"复审日期"！
			ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.staffCertificate.certStaffSet.issueDateIsLargerThanReviewDate"));
			return false;
		} else {
			//复审提醒日期
			var rremindDateValue = dataGrid.getValueByKey(rowIndex, 'rremindDate');
			if(rremindDateValue != null) {
				if(new Date(dataFormat(value)).getTime() > new Date(dataFormat(rremindDateValue)).getTime()) {
					dataGrid.setValueByKey(rowIndex, 'issueDate', null);
					//"发证日期"不能大于"复审提醒日期"！
					ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.staffCertificate.certStaffSet.issueDateIsLargerThanRremindDate"));
					return false;
				}
			}
		}

	}
}

/*==================================onchange='onValidDateChange(value,rowIndex)'事件==================================*/
function onValidDateChange(value, rowIndex){//(值，行索引)
	
	var dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_staffCertificate_staffCertEditdg1571377815400");
	//有效期至为空时，有效期提醒日期设置为空，且只读
	if(isNaN(value) || value == null) {
		dataGrid.setValueByKey(rowIndex, 'vremindDate', null);
		dataGrid.setDatagridCellAttr(rowIndex, "vremindDate", { readonly : true } );
		return false;
	} 

	//判断有效期至是否大于等于复审日期
	var reviewDateValue = dataGrid.getValueByKey(rowIndex, 'reviewDate');
	if(reviewDateValue != null) {
		if(new Date(dataFormat(value)).getTime() < new Date(dataFormat(reviewDateValue)).getTime()) {
			dataGrid.setValueByKey(rowIndex, "validDate", null);
			dataGrid.setValueByKey(rowIndex, 'vremindDate', null);
			dataGrid.setDatagridCellAttr(rowIndex, "vremindDate", { readonly : true } );
			//有效期至不能小于复审日期
			ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.staffCertificate.certStaffSet.validDateSetError"));
			return false;
		}
	}
	//判断有效期至是否大于等于发证日期
	var issueDateValue = dataGrid.getValueByKey(rowIndex, 'issueDate');
	if(issueDateValue != null) {
		if(new Date(dataFormat(value)).getTime() < new Date(dataFormat(issueDateValue)).getTime()) {
			dataGrid.setValueByKey(rowIndex, "validDate", null);
			dataGrid.setValueByKey(rowIndex, 'vremindDate', null);
			dataGrid.setDatagridCellAttr(rowIndex, "vremindDate", { readonly : true } );
			//有效期至不能小于发证日期
			ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.staffCertificate.certStaffSet.validDateIsLittleThanIssueDate"));
			return false;
		}
	}
		
	//获取资质提醒提前天数
	var validRemindDate = dataGrid.getValueByKey(rowIndex, 'certLvId.certId.validRemind');
	if(validRemindDate == null) {
		validRemindDate = 0;
	}
	var remindDateToMs = validRemindDate*24*60*60*1000;
	//提醒日期时间戳
	var vremDate = value - remindDateToMs;

	//设置有效期提醒日期
	var issueDateVal = dataGrid.getValueByKey(rowIndex, 'issueDate');
	if(issueDateVal != null) {
		if(new Date(dataFormat(vremDate)).getTime() < new Date(dataFormat(issueDateVal)).getTime()) {
			dataGrid.setValueByKey(rowIndex, 'vremindDate', null);
			//有效期提醒日期不能小于发证日期
			//ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.staffCertificate.certStaffSet.vremindDateIsLittleThanIssueDate"));
		} else {
			dataGrid.setValueByKey(rowIndex, 'vremindDate', vremDate);
		}
	} else {
		dataGrid.setValueByKey(rowIndex, 'vremindDate', vremDate);
	}
	dataGrid.setDatagridCellAttr(rowIndex, "vremindDate", { readonly : false } );
	
}

/*==================================onchange='onVremindDateChange(value,rowIndex)'事件==================================*/
function onVremindDateChange(value, rowIndex){//(值，行索引)
	var staffCertEditdg = ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_staffCertificate_staffCertEditdg1571377815400");
	
	if(isNaN(value) || value == null) {
		return false;
	} 
	
	var validDate = staffCertEditdg.getValueByKey(rowIndex, "validDate");
	if(new Date(dataFormat(value)).getTime() > new Date(dataFormat(validDate)).getTime()) {
		//有效期提醒日期不能大于有效期
		staffCertEditdg.setValueByKey(rowIndex, "vremindDate", null);
		ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.staffCertificate.certStaffSet.vremindDateSetError"));
		return false;
	}
	
	if(staffCertEditdg.getValueByKey(rowIndex, 'issueDate') != null) {
	var issueDateValue = staffCertEditdg.getValueByKey(rowIndex, 'issueDate');
		if(new Date(dataFormat(value)).getTime() < new Date(dataFormat(issueDateValue)).getTime()) {
			staffCertEditdg.setValueByKey(rowIndex, 'vremindDate', null);
			//有效期提醒日期不能小于发证日期
			ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.staffCertificate.certStaffSet.vremindDateIsLittleThanIssueDate"));
			return false;
		}
	}
}

/*==================================onchange='onReviewDateChange(value,rowIndex)'事件==================================*/
function onReviewDateChange(value, rowIndex){//(值，行索引)
	var dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_staffCertificate_staffCertEditdg1571377815400");
	//复审日期为空时，复审提醒日期设置为空，且只读
	if(value == null || isNaN(value)) {	
		dataGrid.setValueByKey(rowIndex, 'rremindDate', null);
		dataGrid.setDatagridCellAttr(rowIndex, "rremindDate", { readonly : true } );
		return false;
	} 
	
	//判断有效期至是否大于等于复审日期
	var validDateValue = dataGrid.getValueByKey(rowIndex, 'validDate');
	if(validDateValue != null) {
		if(new Date(dataFormat(value)).getTime() > new Date(dataFormat(validDateValue)).getTime()) {
			dataGrid.setValueByKey(rowIndex, "reviewDate", null);
			//复审日期不能大于有效期至
			dataGrid.setValueByKey(rowIndex, 'rremindDate', null);
			dataGrid.setDatagridCellAttr(rowIndex, "rremindDate", { readonly : true } );
			ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.staffCertificate.certStaffSet.reviewDateSetError"));
			return false;
		}
	}
	
	if(dataGrid.getValueByKey(rowIndex, 'issueDate') != null) {
		var issueDateValue = dataGrid.getValueByKey(rowIndex, 'issueDate'); 
		if(new Date(dataFormat(value)).getTime() < new Date(dataFormat(issueDateValue)).getTime()) {
			dataGrid.setValueByKey(rowIndex, "reviewDate", null);
			//复审日期不能小于发证日期
			dataGrid.setValueByKey(rowIndex, 'rremindDate', null);
			dataGrid.setDatagridCellAttr(rowIndex, "rremindDate", { readonly : true } );
			ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.staffCertificate.certStaffSet.reviewDateIsLittleThanIssueDate"));
			return false;
		}
	}
		
	//获取资质提醒提前天数
	var validRemindDate = dataGrid.getValueByKey(rowIndex,'certLvId.certId.validRemind');
	if(validRemindDate == null) {
		validRemindDate = 0;
	}
	var remindDateToMs = validRemindDate*24*60*60*1000;
	var vremDate = value - remindDateToMs;//复审提醒日期事件戳
		
	//设置复审提醒日期
	var issueDateVal = dataGrid.getValueByKey(rowIndex, 'issueDate');
	if(issueDateVal != null) {
		//复审提醒日期不能小于发证日期
		if(new Date(dataFormat(vremDate)).getTime() < new Date(dataFormat(issueDateVal)).getTime()) {
			dataGrid.setValueByKey(rowIndex, 'rremindDate', null);
		} else {
			dataGrid.setValueByKey(rowIndex,'rremindDate',vremDate);
		}
	} else {
		dataGrid.setValueByKey(rowIndex,'rremindDate',vremDate);
	}		
	dataGrid.setDatagridCellAttr(rowIndex, "rremindDate", { readonly : false } );
}

/*==================================onchange='onRremindDateChange(value,rowIndex)'事件==================================*/
function onRremindDateChange(value, rowIndex){//(值，行索引)
	var staffCertEditdg = ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_staffCertificate_staffCertEditdg1571377815400");
	
	if(isNaN(value) || value == null) {
		return false;
	} 
	
	var reviewDate = staffCertEditdg.getValueByKey(rowIndex, "reviewDate");
	if(new Date(dataFormat(value)).getTime() > new Date(dataFormat(reviewDate)).getTime()) {
		//复审提醒日期不能大于复审日期
		staffCertEditdg.setValueByKey(rowIndex, "rremindDate", null);
		ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.staffCertificate.certStaffSet.rremindDateSetError"));
		return false;
	}
	
	var issueDateValue = staffCertEditdg.getValueByKey(rowIndex, 'issueDate');
	if(issueDateValue != null) {
		if(new Date(dataFormat(value)).getTime() < new Date(dataFormat(issueDateValue)).getTime()) {
			//复审提醒日期不能小于发证日期
			staffCertEditdg.setValueByKey(rowIndex, 'rremindDate', null);
			ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.staffCertificate.certStaffSet.rremindDateIsLittleThanIssueDate"));
			return false;
		}
	}
}

/*==================================Qualify_1.0.0_staffCertificate_staffCertEditdg1571377815400_renderOver事件==================================*/
if (loadFlag == false) {
    var selectStaff = parent.ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_staffCertificate_certStaffList_certStaff_sdg").getSelecteds()[0];
    //获取人员id
    var staffId = selectStaff.id;
    ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_staffCertificate_staffCertEditdg1571377815400").refreshDataByRequst({
        type: "post",
        url: "/msService/Qualify/staffCertificate/staffCert/staffCertQuery",
        param: { staffId: staffId }
    });
    loadFlag = true;
}
var dg = ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_staffCertificate_staffCertEditdg1571377815400");
for (var i = 0; i < dg.getDatagridData().length; i++) {
    //有效日期
    var validDateValue = dg.getValueByKey(i, 'validDate');
    if (isNaN(validDateValue) || validDateValue == null) {
        dg.setDatagridCellAttr(i, "vremindDate", { readonly: true });
    }

    //复审日期
    var reviewDateValue = dg.getValueByKey(i, 'reviewDate');
    if (isNaN(reviewDateValue) || reviewDateValue == null) {
        dg.setDatagridCellAttr(i, "rremindDate", { readonly: true });
    }
}
var result = ReactAPI.getSystemConfig({ moduleCode: "Qualify", key: "Qualify.setDefaultLevel" })["Qualify.setDefaultLevel"];
if (result) {
    var dgData = dg.getDatagridData();
    if (dgData.length > 0) {
        for (var i = 0; i < dgData.length; i++) {
            var row = dgData[i];
            if (row.certLvId.code.includes("defaultLevel")) {
                dg.setCellValueByKey(row.rowIndex, "certLvId.code", null);
                dg.setCellValueByKey(row.rowIndex, "certLvId.name", null);
            }
        }
    }
}

