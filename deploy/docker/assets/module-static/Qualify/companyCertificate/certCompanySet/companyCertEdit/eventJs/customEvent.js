//======================================================================================
//视图的事件，请不要在这里修改代码，修改的代码会被覆盖
//======================================================================================

/*==================================onload事件==================================*/
var Qualify_companyCertificate_certCompanySet_onload = function(){
	var selectCompany = parent.ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_companyCertificate_certCompanyList_certCompany_sdg").getSelecteds()[0];
ReactAPI.getComponentAPI("Input").APIs("certCompanySet.companyId.code").setValue(selectCompany.code);//公司编码
ReactAPI.getComponentAPI("Input").APIs("certCompanySet.companyId.name").setValue(selectCompany.name);//名称
}

/*==================================onsave事件==================================*/
var Qualify_companyCertificate_certCompanySet_onsave = function(){
	return false;
}

/*==================================onchange='onIssueDateChange(value,rowIndex)'事件==================================*/
function onIssueDateChange(value, rowIndex){//(值，行索引)
	debugger;
	var dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_companyCertificate_companyCertEditdg1575277481175");
	//判断有效期是否为空
	var validDateValue = dataGrid.getValueByKey(rowIndex, 'validDate');
	if(validDateValue != null) {
		if(new Date(dataFormat(value)).getTime() > new Date(dataFormat(validDateValue)).getTime()) {
			dataGrid.setValueByKey(rowIndex, 'issueDate', null);
			//"发证日期"不能大于"有效期至"！
			ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.companyCertificate.certCompanySet.issueDateIsLargerThanValidDate"));
			//return false;
		} else {
			//有效期提醒日期
			var vremindDateValue = dataGrid.getValueByKey(rowIndex, 'vremindDate');
			if(vremindDateValue != null) {
				if(new Date(dataFormat(value)).getTime() > new Date(dataFormat(vremindDateValue)).getTime()) {
					dataGrid.setValueByKey(rowIndex, 'issueDate', null);
					//"发证日期"不能大于"有效期提醒日期"！
					ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.companyCertificate.certCompanySet.issueDateIsLargerThanVremindDate"));
					//return false;
				}
			}
		}
	}

	if(dataGrid.getValueByKey(rowIndex, 'reviewDate') != null) {
		//判断复审日期是否为空
		var reviewDateValue = dataGrid.getValueByKey(rowIndex, 'reviewDate');
		if(new Date(dataFormat(value)).getTime() > new Date(dataFormat(reviewDateValue)).getTime()) {
			dataGrid.setValueByKey(rowIndex, 'issueDate', null);
			//"发证日期"不能大于"复审日期"！
			ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.companyCertificate.certCompanySet.issueDateIsLargerThanReviewDate"));
			//return false;
		} else {
			//复审提醒日期
			var rremindDateValue = dataGrid.getValueByKey(rowIndex, 'rremindDate');
			if(rremindDateValue != null) {
				if(new Date(dataFormat(value)).getTime() > new Date(dataFormat(rremindDateValue)).getTime()) {
					dataGrid.setValueByKey(rowIndex, 'issueDate', null);
					//"发证日期"不能大于"复审提醒日期"！
					ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.companyCertificate.certCompanySet.issueDateIsLargerThanRremindDate"));
					//return false;
				}
			}
		}

	}
	
}

/*==================================onchange='onValidDateChange(value,rowIndex)'事件==================================*/
function onValidDateChange(value, rowIndex){//(值，行索引)
	var dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_companyCertificate_companyCertEditdg1575277481175");
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
			//"有效期至"不能小于"复审日期"！
			ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.companyCertificate.certCompanySet.validDateSetError"));
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
			//"有效期至"不能小于"发证日期"！
			ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.companyCertificate.certCompanySet.validDateIsLittleThanIssueDate"));
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
			//"有效期提醒日期"不能小于"发证日期"！
			//ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.companyCertificate.certCompanySet.vremindDateIsLittleThanIssueDate"));
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
	if(isNaN(value) || value == null) {
		return false;
	}
	
	var staffCertEditdg = ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_companyCertificate_companyCertEditdg1575277481175");
	var validDate = staffCertEditdg.getValueByKey(rowIndex, "validDate");
	if(new Date(dataFormat(value)).getTime() > new Date(dataFormat(validDate)).getTime()) {
		staffCertEditdg.setValueByKey(rowIndex, "vremindDate", null);
		//"有效期提醒日期"不能大于"有效期至"！
		ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.companyCertificate.certCompanySet.vremindDateSetError"));
		return false;
	}
	var issueDate = staffCertEditdg.getValueByKey(rowIndex, 'issueDate');
	if(issueDate != null) {
		if(new Date(dataFormat(value)).getTime() < new Date(dataFormat(issueDate)).getTime()) {
			staffCertEditdg.setValueByKey(rowIndex, 'vremindDate', null);
			//"有效期提醒日期"不能小于"发证日期"！
			ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.companyCertificate.certCompanySet.vremindDateIsLittleThanIssueDate"));
			return false;
		}
	}
}

/*==================================onchange='onReviewDateChange(value,rowIndex)'事件==================================*/
function onReviewDateChange(value, rowIndex){//(值，行索引)
	var dataGrid = ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_companyCertificate_companyCertEditdg1575277481175");
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
			//"复审日期"不能大于"有效期至"！
			dataGrid.setValueByKey(rowIndex, 'rremindDate', null);
			dataGrid.setDatagridCellAttr(rowIndex, "rremindDate", { readonly : true } );
			ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.companyCertificate.certCompanySet.reviewDateSetError"));
			return false;
		}
	}
	var issueDateValue = dataGrid.getValueByKey(rowIndex, 'issueDate');
	if(issueDateValue != null) {
		if(new Date(dataFormat(value)).getTime() < new Date(dataFormat(issueDateValue)).getTime()) {
			dataGrid.setValueByKey(rowIndex, "reviewDate", null);
			//"复审日期"不能小于"发证日期"！
			dataGrid.setValueByKey(rowIndex, 'rremindDate', null);
			dataGrid.setDatagridCellAttr(rowIndex, "rremindDate", { readonly : true } );
			ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.companyCertificate.certCompanySet.reviewDateIsLittleThanIssueDate"));
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
		//"复审提醒日期"不能小于"发证日期"！
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
	var staffCertEditdg = ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_companyCertificate_companyCertEditdg1575277481175");
	
	if(isNaN(value) || value == null) {
		return false;
	} 
	
	var reviewDate = staffCertEditdg.getValueByKey(rowIndex, "reviewDate");
	if(new Date(dataFormat(value)).getTime() > new Date(dataFormat(reviewDate)).getTime()) {
		//"复审提醒日期"不能大于"复审日期"！
		staffCertEditdg.setValueByKey(rowIndex, "rremindDate", null);
		ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.companyCertificate.certCompanySet.rremindDateSetError"));
		return false;
	}
	var issueDateValue = staffCertEditdg.getValueByKey(rowIndex, 'issueDate');
	if(issueDateValue != null) {
		if(new Date(dataFormat(value)).getTime() < new Date(dataFormat(issueDateValue)).getTime()) {
			//"复审提醒日期"不能小于"发证日期"！
			staffCertEditdg.setValueByKey(rowIndex, 'rremindDate', null);
			ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.companyCertificate.certCompanySet.rremindDateIsLittleThanIssueDate"));
			return false;
		}
	}
}

/*==================================Qualify_1.0.0_companyCertificate_companyCertEditdg1575277481175_renderOver事件==================================*/
if (loadFlag == false) {
    var selectCompany = parent.ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_companyCertificate_certCompanyList_certCompany_sdg").getSelecteds()[0];
    //获取企业id
    var companyId = selectCompany.id;
    ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_companyCertificate_companyCertEditdg1575277481175").refreshDataByRequst({
        type: "post",
        url: "/msService/Qualify/companyCertificate/companyCert/companyCertQuery",
        param: { companyId: companyId }
    });
    loadFlag = true;
}
var dg = ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_companyCertificate_companyCertEditdg1575277481175");
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

/*==================================onclick='openCompanyCertLevelRef()'事件==================================*/
function openCompanyCertLevelRef() {
    //获取公司id
    var companyId = parent.ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_companyCertificate_certCompanyList_certCompany_sdg").getSelecteds()[0].id;

    ReactAPI.createDialog("companyCertLevelDialog", {
        //title: "资质设置",
        title: ReactAPI.international.getText("Qualify.viewtitle.randon1571193702274"),
        url: "/msService/Qualify/certificate/certificateLevel/certLevelRef?refKey=certLvId.code&fromViewCode=Qualify_1.0.0_companyCertificate_companyCertEdit&type=company&customConditionKey=type&crossComapny=false",
        size: 5,
        isRef: true, // 是否开启参照
        callback: function (data, event) {
            console.log(data);
            companyCertAdd(data, companyId);
            event.ReactAPI.showMessage("s", ReactAPI.international.getText("foundation.common.add.success"), null, true);
            //ReactAPI.destroyDialog("companyCertLevelDialog");
        },
        onOk: function (data, event) {
            if (data.length == 0) {
                //ReactAPI.showMessage("w", "请选择一条记录！");
                event.ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.certificateLevel.chooseOneRecord"));
                return false;
            } else {
                companyCertAdd(data, companyId);
                event.ReactAPI.showMessage("s", ReactAPI.international.getText("foundation.common.add.success"), null, true);
                //ReactAPI.destroyDialog("companyCertLevelDialog");
            }

        },
        //okText:"选择",
        okText: ReactAPI.international.getText("Button.text.select"),
        onCancel: function () {
            ReactAPI.destroyDialog("companyCertLevelDialog");
        },
        cancelText: ReactAPI.international.getText("Button.text.close")
    });
}

