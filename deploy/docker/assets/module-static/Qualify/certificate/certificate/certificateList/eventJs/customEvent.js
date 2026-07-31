//======================================================================================
//视图的事件，请不要在这里修改代码，修改的代码会被覆盖
//======================================================================================

/*==================================onclick='addCertificate()'事件==================================*/
function addCertificate(){
	var treeNode = ReactAPI.getComponentAPI("NavTree").APIs("Qualify_1.0.0_certificate_certifcateLayOut_certificate_nt").getSelectedTreeNode();
	
	if(treeNode == null) {
		ReactAPI.showMessage("w", ReactAPI.international.getText("TreeList.NavTree.chooseNode"));
		return false;
	} else {
		var certTypeId = treeNode.id;

		if(!treeNode.leaf) {
			ReactAPI.showMessage("w", ReactAPI.international.getText("TreeList.NavTree.isleaf"));
			return false;
		}
	
		//获取新增按钮的powercode
		ReactAPI.getPowerCode("Qualify_1.0.0_certificate_certifcateLayOut_certificateList_addCertificate_add_Qualify_1.0.0_certificate_certificateList", function(res) {
			var powerCode = res["Qualify_1.0.0_certificate_certifcateLayOut_certificateList_addCertificate_add_Qualify_1.0.0_certificate_certificateList"];
			//打开资质编辑页面
			ReactAPI.createDialog("certificateEditDailog",{
				//title: "资质编辑",
				title: ReactAPI.international.getText("Qualify.viewtitle.randon1569416513467"),
				size: 5,
				url: "/msService/Qualify/certificate/certificate/certificateEdit?__pc__=" + powerCode +  "&viewCode=Qualify_1.0.0_certificate_certifcateLayOut&entityCode=Qualify_1.0.0_certificate&iscrosscompany=false&openType=dialog&viewType=edit&assModalName=cerType&cerType.id=" + certTypeId,
				buttons: [
					{
						//text: "保存",
						text: ReactAPI.international.getText("Button.text.save"),
						type: "primary",
						onClick: function(event) {
							//event.ReactAPI.submitFormData(); // 提交当前iframe页面
							//ReactAPI.submitFormData(); // 提交当前页面
							event.ReactAPI.submitFormData("save", function(res) {
								if(res.data != null) {
									if(res.data.dealSuccessFlag) {
										ReactAPI.destroyDialog("certificateEditDailog");     	
									}
								}
							});						
						}
					},
					{
						text: ReactAPI.international.getText("Button.text.cancel"),
						onClick: function() {
							ReactAPI.destroyDialog("certificateEditDailog");
						}
					}
				]
			});
		});	
	} 
}

