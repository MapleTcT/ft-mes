//======================================================================================
//视图的事件，请不要在这里修改代码，修改的代码会被覆盖
//======================================================================================

/*==================================Qualify_1.0.0_staffCertificate_certStaffList_ptPageInit事件==================================*/
//设置按钮样式
ReactAPI.getComponentAPI().SupDataGrid.APIs("Qualify_1.0.0_staffCertificate_certStaffList_certStaff_sdg").setBtnImg("btn-staffCertSet","sup-btn-own-sz");
ReactAPI.getComponentAPI().SupDataGrid.APIs("Qualify_1.0.0_staffCertificate_certStaffList_certStaff_sdg").setBtnImg("btn-staffCertLogView","sup-btn-own-ckrz");

/*==================================onclick='viewStaffCertLog()'事件==================================*/
function viewStaffCertLog() {
	var selRows = ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_staffCertificate_certStaffList_certStaff_sdg").getSelecteds();
	
	if(selRows.length <= 0) {
		ReactAPI.showMessage("w", ReactAPI.international.getText("SupDatagrid.button.error"));
		return false;
	}
	
	if(selRows.length >= 2) {
		ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.staffCertificate.onlyChooseOneRecord"));
		return false;
	}
		
	ReactAPI.getPowerCode("certStaffList_staffCertLogView_modify_Qualify_1.0.0_staffCertificate_certStaffList", function(res) {
		var powerCode = res["certStaffList_staffCertLogView_modify_Qualify_1.0.0_staffCertificate_certStaffList"];
		//打开页面
		ReactAPI.createDialog("staffCertLogDialog", {
			title: ReactAPI.international.getText("Qualify.viewtitle.randon1571316913994"),
			//size: 4,
			url: "/msService/Qualify/staffCertificate/certStaffSet/staffCertLogView?__pc__=" + powerCode + "&staffId=" +selRows[0].id,
			isRef: false,
			buttons: [
				{
					text: ReactAPI.international.getText("Button.text.close"),
					onClick: event => {
						ReactAPI.destroyDialog("staffCertLogDialog");
					}
				}
			]
		});
	});
}

/*==================================onclick='setStaffCert()'事件==================================*/
function setStaffCert() {
	var selRows = ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_staffCertificate_certStaffList_certStaff_sdg").getSelecteds();
	
	if(selRows.length <= 0) {
		ReactAPI.showMessage("w", ReactAPI.international.getText("SupDatagrid.button.error"));
		return false;
	}
	
	if(selRows.length >= 2) {
		ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.operate.onlyChooseOneRecord"));
		return false;
	}
	
	//判断是否是当前登录人所属公司的数据
	var staffCId=selRows[0].cid;
	var currentPersonCID=ReactAPI.getUserInfo().company.id;
	if(staffCId != currentPersonCID){
		//无法操作非本公司数据
		ReactAPI.showMessage("w", ReactAPI.international.getText("ec.edit.button.uncurrent.company"));
		return false;
	}

	//获取人员资质设置按钮powercode
	ReactAPI.getPowerCode("certStaffList_staffCertSet_add_Qualify_1.0.0_staffCertificate_certStaffList", function(res) {
		var powerCode = res["certStaffList_staffCertSet_add_Qualify_1.0.0_staffCertificate_certStaffList"];
		
		//打开人员资质编辑页面
		ReactAPI.createDialog("staffCertEditDialog", {
			title: ReactAPI.international.getText("Qualify.viewtitle.randon1571316794961"),
			url: "/msService/Qualify/staffCertificate/certStaffSet/staffCertEdit?__pc__=" + powerCode + "&viewCode=Qualify_1.0.0_staffCertificate_staffCertList&entityCode=Qualify_1.0.0_staffCertificate&iscrosscompany=false&openType=dialog&viewType=edit",
			size: 5,
			onOk:function(event) {
				event.ReactAPI.submitFormData("save");
				
				console.log(event.ReactAPI.getSaveData());
				//删除的人员资质Ids
				var deletedStaffCertIds = event.ReactAPI.getSaveData().dgDeletedIds.dg1571377815400;
				//人员资质列表数据
				var staffCertList = event.ReactAPI.getSaveData().dgList.dg1571377815400;
				$.ajax({
					url: "/msService/Qualify/staffCertificate/staffCert/saveStaffCertPTData",
					type: "post",
					async: false,
					headers:{'Content-Type':'application/json;charset=utf8'},
					dataType:"json",
					//data: { deletedStaffCertIds:deletedStaffCertIds, staffCertList:staffCertList },//List<Map<String, Object>>
					data: JSON.stringify({ deletedStaffCertIds:deletedStaffCertIds, staffCertList:staffCertList }),
					success: function(res) {
						console.log(res);
						//if(res.data.dealSuccessFlag) {//res替换为res.data
						if(res.data.dealSuccessFlag) {
							ReactAPI.destroyDialog("staffCertEditDialog");
						}
					}
				});
				
			},
			//okText:"保存",    
			okText: ReactAPI.international.getText("Button.text.save"),    
			onCancel:function(){
				ReactAPI.destroyDialog("staffCertEditDialog");
			}
		});
	});
}

