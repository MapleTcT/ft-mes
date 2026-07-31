//======================================================================================
//视图的事件，请不要在这里修改代码，修改的代码会被覆盖
//======================================================================================

/*==================================Qualify_1.0.0_companyCertificate_certCompanyList_ptPageInit事件==================================*/
//设置按钮样式
ReactAPI.getComponentAPI().SupDataGrid.APIs("Qualify_1.0.0_companyCertificate_certCompanyList_certCompany_sdg").setBtnImg("btn-companyCertSet","sup-btn-own-sz");

/*==================================onclick='setCompanyCert()'事件==================================*/
function setCompanyCert() {
	
	var selRows = ReactAPI.getComponentAPI("SupDataGrid").APIs("Qualify_1.0.0_companyCertificate_certCompanyList_certCompany_sdg").getSelecteds();
		
	if(selRows.length <= 0) {
		ReactAPI.showMessage("w", ReactAPI.international.getText("SupDatagrid.button.error"));
		return false;
	}
	
	if(selRows.length >= 2) {
		ReactAPI.showMessage("w", ReactAPI.international.getText("Qualify.operate.onlyChooseOneRecord"));
		return false;
	}
	
	//判断是否是当前登录人所属公司的数据
	var companyId=selRows[0].id;
	var currentPersonCID=ReactAPI.getUserInfo().company.id;
	if(companyId != currentPersonCID){
		//无法操作非本公司数据
		ReactAPI.showMessage("w", ReactAPI.international.getText("ec.edit.button.uncurrent.company"));
		return false;
	}

	//获取企业资质设置按钮powercode
	ReactAPI.getPowerCode("certCompanyList_companyCertSet_add_Qualify_1.0.0_companyCertificate_certCompanyList", function(res) {
		var powerCode = res["certCompanyList_companyCertSet_add_Qualify_1.0.0_companyCertificate_certCompanyList"];
		
		//打开企业资质编辑页面
		ReactAPI.createDialog("companyCertEditDialog", {
			title: ReactAPI.international.getText("Qualify.viewtitle.randon1575276708573"),
			url: "/msService/Qualify/companyCertificate/certCompanySet/companyCertEdit?__pc__=" + powerCode + "&viewCode=Qualify_1.0.0_companyCertificate_companyCertList&entityCode=Qualify_1.0.0_companyCertificate&iscrosscompany=false&openType=dialog&viewType=edit",
			size: 5,
			onOk:function(event) {
				event.ReactAPI.submitFormData("save");
				console.log(event.ReactAPI.getSaveData());
				
				var saveDatas = event.ReactAPI.getSaveData();
				//删除的企业资质Ids
				var deletedCompanyCertIds = saveDatas.dgDeletedIds.dg1575277481175;
				//企业资质列表数据
				var companyCertList = saveDatas.dgList.dg1575277481175;
				$.ajax({
					url: "/msService/Qualify/companyCertificate/companyCert/saveCompanyCertPTData",
					type: "post",
					async: false,
					headers:{'Content-Type':'application/json;charset=utf8'},
					dataType:"json",
					//data: { deletedStaffCertIds:deletedStaffCertIds, staffCertList:staffCertList },//List<Map<String, Object>>
					data: JSON.stringify({ deletedCompanyCertIds:deletedCompanyCertIds, companyCertList:companyCertList }),
					success: function(res) {
						console.log(res);
						if(res.data.dealSuccessFlag) {
							ReactAPI.destroyDialog("companyCertEditDialog");
						}
					}
				});
			},
			//okText:"保存",    
			okText: ReactAPI.international.getText("Button.text.save"),
			onCancel:function(){
				ReactAPI.destroyDialog("companyCertEditDialog");
			}
		});
	});
}

