//按上级分类节点id新增资质分类
function openCertificateTypeAdd(parentId) {
	//获取新增按钮powercode
	ReactAPI.getPowerCode("Qualify_1.0.0_certificateType_cerTypeLayOut_cerTypeList_add_add_Qualify_1.0.0_certificateType_cerTypeList", function (res) {
		var powerCode = res["Qualify_1.0.0_certificateType_cerTypeLayOut_cerTypeList_add_add_Qualify_1.0.0_certificateType_cerTypeList"];

		//打开资质分类编辑页面
		ReactAPI.createDialog("cerTypeEditDialog", {
			//title: "资质分类",
			title: ReactAPI.international.getText("Qualify.viewtitle.randon1569401770550"),
			//width: 345,
			//height: 282,
			size: 2,
			url: "/msService/Qualify/certificateType/certificateType/cerTypeEdit?__pc__=" + powerCode + "&viewCode=Qualify_1.0.0_certificateType_cerTypeLayOut&entityCode=Qualify_1.0.0_certificateType&iscrosscompany=false&openType=dialog&viewType=edit&parentId=" + parentId,
			callback: function callback(data, event) {
				console.log(data, event);
			},
			onOk: function onOk(event) {
				event.ReactAPI.submitFormData("save", function (res) {
					ReactAPI.destroyDialog("cerTypeEditDialog");
				});
			},
			//okText:"保存",    
			okText: ReactAPI.international.getText("Button.text.save"),
			onCancel: function onCancel() {
				ReactAPI.destroyDialog("cerTypeEditDialog");
			}
		});
	});
}