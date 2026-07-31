var loadFlag = false;
//资质参照
function stafCertAdd(data, staffId) {
	var cerStaffDg = ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_staffCertificate_staffCertEditdg1571377815400");
	var pDataObjs = cerStaffDg.getDatagridData(); //编辑页面表体数据

	for (var i = 0; i < data.length; i++) {
		var flag = false;
		//过滤相同等级
		if (pDataObjs.length > 0) {
			for (var lRows = 0; lRows < pDataObjs.length; lRows++) {
				var levelId = cerStaffDg.getValueByKey(lRows, 'certLvId.id');
				if (levelId == data[i].id) {
					flag = true;
				}
			}
		}

		if (!flag) {
			var lastRow = cerStaffDg.addLine().rowIndex; //添加行，获取行索引
			//资质
			var certId = {
				code: data[i].certId.code,
				id: data[i].certId.id,
				name: data[i].certId.name,
				validRemind: data[i].certId.validRemind
				//资质等级
			};var certLvId = {
				id: data[i].id,
				code: data[i].code,
				name: data[i].name,
				certId: certId

				//默认资质状态（后续删除）
			};var certState = {
				id: 'Qualify_certState/valid',
				value: ReactAPI.international.getText("Qualify.systemCodevalue.randon1570518123000")
			};

			var date = new Date();
			//设置行数据
			cerStaffDg.setRowData(lastRow, {
				staffId: { id: staffId },
				certLvId: certLvId,
				//issueDate: date.getTime(),
				certState: certState,
				edited: true
			});
			//有效期提醒日期
			cerStaffDg.setDatagridCellAttr(lastRow, "vremindDate", { readonly: true });
			//复审提醒日期
			cerStaffDg.setDatagridCellAttr(lastRow, "rremindDate", { readonly: true });
		}
	}
	//ReactAPI.destroyDialog("certLevelDialog");
}

//时间戳转化为日期
function dataFormat(timestampValue) {
	var time = new Date(timestampValue);
	var y = time.getFullYear();
	var m = time.getMonth() + 1;
	var d = time.getDate();
	//var aa = y+'-'+dealDateFormat(m)+'-'+dealDateFormat(d);
	//console.log(aa.toString());
	return y + '-' + dealDateFormat(m) + '-' + dealDateFormat(d);
}

function dealDateFormat(m) {
	return m < 10 ? '0' + m : m;
}